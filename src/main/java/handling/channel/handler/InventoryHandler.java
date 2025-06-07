/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import client.InnerAbility;
import client.InnerSkillValueHolder;
import client.MagicWheel;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleDisease;
import client.MapleQuestStatus;
import client.MapleStat;
import client.MapleTrait.MapleTraitType;
import client.MonsterFamiliar;
import client.PlayerStats;
import client.Skill;
import client.SkillEntry;
import client.SkillFactory;

import client.inventory.Equip;
import client.inventory.Equip.ScrollResult;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MapleMount;
import client.inventory.MaplePet;
import client.inventory.MaplePet.PetFlag;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import scripting.NPCScriptManager;
import server.*;
import server.Timer;
import server.Timer.MapTimer;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.*;
import server.quest.MapleQuest;
import server.shops.HiredMerchant;
import server.shops.IMaplePlayerShop;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.data.LittleEndianAccessor;
import tools.packet.CField.EffectPacket;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.*;

public class InventoryHandler {

    public static void ItemMove(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        slea.readInt();
        MapleInventoryType type = MapleInventoryType.getByType(slea.readByte()); //04
        short src = slea.readShort();                                            //01 00
        short dst = slea.readShort();
        long checkq = slea.readShort();
        short quantity = (short) (int) checkq;                                      //53 01

        if (src < 0 && dst > 0) {
            MapleInventoryManipulator.unequip(c, src, dst);
        } else if (dst < 0) {
            MapleInventoryManipulator.equip(c, src, dst);
        } else if (dst == 0) {
            if (checkq < 1 || c.getPlayer().getInventory(type).getItem(src) == null) {
                c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
                //     World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, c.getPlayer().getName() + " --- Possibly attempting drop dupe! Go investigate"));
                return;
            }
            MapleInventoryManipulator.drop(c, type, src, quantity);
        } else {
            MapleInventoryManipulator.move(c, type, src, dst);
        }
        c.getPlayer().saveToDB(false, false);
    }

    public static void SwitchBag(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        slea.readInt();
        short src = (short) slea.readInt();                                       //01 00
        short dst = (short) slea.readInt();                                            //00 00
        if (src < 100 || dst < 100) {
            return;
        }
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, src, dst);
    }

    /*
    小背包移動道具設定
    */
    public static final void MoveBag(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        slea.readInt();
        if (slea.available() < 11L) { //防止擁有小背包時，按兩下道具卡屏
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final boolean srcFirst = slea.readInt() > 0;
        short dst = (short) slea.readInt();
        if (slea.readByte() != 4) { //must be etc) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        short src = slea.readShort();
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, srcFirst ? dst : src, srcFirst ? src : dst);
    }

    public static void ItemSort(LittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        MapleInventoryType pInvType = MapleInventoryType.getByType(slea.readByte());
        if (pInvType == MapleInventoryType.UNDEFINED || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        MapleInventory pInv = c.getPlayer().getInventory(pInvType); //Mode should correspond with MapleInventoryType
        boolean sorted = false;

        while (!sorted) {
            byte freeSlot = (byte) pInv.getNextFreeSlot();
            if (freeSlot != -1) {
                byte itemSlot = -1;
                for (byte i = (byte) (freeSlot + 1); i <= pInv.getSlotLimit(); i++) {
                    if (pInv.getItem(i) != null) {
                        itemSlot = i;
                        break;
                    }
                }
                if (itemSlot > 0) {
                    MapleInventoryManipulator.move(c, pInvType, itemSlot, freeSlot);
                } else {
                    sorted = true;
                }
            } else {
                sorted = true;
            }
        }
        c.getSession().write(CWvsContext.finishedSort(pInvType.getType()));
        c.getSession().write(CWvsContext.enableActions());
    }

    /*
    自動整理背包道具設定
    */
    public static final void ItemGather(final LittleEndianAccessor slea, final MapleClient c) {
        // [41 00] [E5 1D 55 00] [01]
        // [32 00] [01] [01] // Sent after

        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        if (c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final byte mode = slea.readByte();
        final MapleInventoryType invType = MapleInventoryType.getByType(mode);
        MapleInventory Inv = c.getPlayer().getInventory(invType);

        final List<Item> itemMap = new LinkedList<Item>();
        for (Item item : Inv.list()) {
            if (!GameConstants.isPet(item.getItemId()) && !GameConstants.exItemGather(item.getItemId()) && item.getPosition() < 97) { //防止啟用寵物和擁有小背包時，排序錯誤
                itemMap.add(item.copy());
            }
        }
        for (Item itemStats : itemMap) {
            MapleInventoryManipulator.removeFromSlot(c, invType, itemStats.getPosition(), itemStats.getQuantity(), true, false);
        }

        final List<Item> sortedItems = sortItems(itemMap, c, invType);
        for (Item item : sortedItems) {
            MapleInventoryManipulator.addFromDrop(c, item, false);
        }
        c.getSession().write(CWvsContext.finishedGather(mode));
        c.getSession().write(CWvsContext.enableActions());
        itemMap.clear();
        sortedItems.clear();
    }

    /*
    自動整理背包道具排序
    */
    private static List<Item> sortItems(List<Item> items, MapleClient c, MapleInventoryType pInvType) {
        List<Item> sortedItems = new ArrayList<>(items);
        // Logic for sorting can be implemented here, currently just returning the original list
        // Collections.sort(sortedItems, (a, b) -> Integer.compare(a.getItemId(), b.getItemId())); // Example sorting logic

        // Organize items in the inventory
        MapleInventory pInv = c.getPlayer().getInventory(pInvType);
        boolean sorted = false;

        while (!sorted) {
            final byte freeSlot = (byte) pInv.getNextFreeSlot();
            if (freeSlot != -1) {
                byte itemSlot = -1;
                for (byte i = (byte) (freeSlot + 1); i <= pInv.getSlotLimit(); i++) {
                    if (pInv.getItem(i) != null) {
                        itemSlot = i;
                        break;
                    }
                }
                if (itemSlot > 0) {
                    MapleInventoryManipulator.move(c, pInvType, itemSlot, freeSlot);
                } else {
                    sorted = true;
                }
            } else {
                sorted = true;
            }
        }
        return sortedItems;
    }

    public static boolean UseRewardItem(byte slot, int itemId, MapleClient c, MapleCharacter chr) {
        Item toUse = c.getPlayer().getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);
        c.getSession().write(CWvsContext.enableActions());
        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && !chr.hasBlockedInventory()) {
            if (chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.USE).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.SETUP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.ETC).getNextFreeSlot() > -1) {
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                Pair<Integer, List<StructRewardItem>> rewards = ii.getRewardItem(itemId);

                if (rewards != null && rewards.getLeft() > 0) {
                    while (true) {
                        for (StructRewardItem reward : rewards.getRight()) {
                            if (reward.prob > 0 && Randomizer.nextInt(rewards.getLeft()) < reward.prob) { // Total prob
                                if (GameConstants.getInventoryType(reward.itemid) == MapleInventoryType.EQUIP) {
                                    Item item = MapleItemInformationProvider.getEquipById(reward.itemid);
                                    if (reward.period > 0) {
                                        item.setExpiration(System.currentTimeMillis() + (reward.period * 60 * 60 * 10));
                                    }
                                    item.setGMLog("Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                                    MapleInventoryManipulator.addbyItem(c, item);
                                } else {
                                    MapleInventoryManipulator.addById(c, reward.itemid, reward.quantity, "Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                                }
                                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemId), itemId, 1, false, false);

                                c.getSession().write(EffectPacket.showRewardItemAnimation(reward.itemid, reward.effect));
                                chr.getMap().broadcastMessage(chr, EffectPacket.showRewardItemAnimation(reward.itemid, reward.effect, chr.getId()), false);
                                return true;
                            }
                        }
                    }
                } else {
                    chr.dropMessage(6, "Unknown error.");
                }
            } else {
                chr.dropMessage(6, "Insufficient inventory slot.");
            }
        }
        return false;
    }

    public static void UseItem(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMapId() == 749040100 || chr.getMap() == null || chr.hasDisease(MapleDisease.POTION) || chr.hasBlockedInventory() || chr.inPVP()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        long time = System.currentTimeMillis();
        if (chr.getNextConsume() > time) {
            chr.dropMessage(5, "You may not use this item yet.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) { //cwk quick hack
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                if (chr.getMap().getConsumeItemCoolTime() > 0) {
                    chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
                }
            }

        } else {
            c.getSession().write(CWvsContext.enableActions());
        }
    }

    public static void UseCosmetic(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null || chr.hasBlockedInventory() || chr.inPVP()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 254 || (itemId / 1000) % 10 != chr.getGender()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
    }

    public static void UseReturnScroll(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (!chr.isAlive() || chr.getMapId() == 749040100 || chr.hasBlockedInventory() || chr.isInBlockedMap() || chr.inPVP()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) {
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyReturnScroll(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            } else {
                c.getSession().write(CWvsContext.enableActions());
            }
        } else {
            c.getSession().write(CWvsContext.enableActions());
        }
    }

    /*
    使用外星電轉機
    */
    public static void UseAlienSocket(final LittleEndianAccessor slea, final MapleClient c) {
        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        final Item alienSocket = c.getPlayer().getInventory(MapleInventoryType.USE).getItem((byte) slea.readShort());
        final int alienSocketId = slea.readInt();
        final Item toMount = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readShort());
        if (alienSocket == null || alienSocketId != alienSocket.getItemId() || toMount == null || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            return;
        }
        // Can only use once-> 2nd and 3rd must use NPC.
        final Equip eqq = (Equip) toMount;
        if (eqq.getSocketState() != 0) { // Used before
            c.getPlayer().dropMessage(1, "This item already has a socket.");
        } else {
            eqq.setSocket1(0); // First socket, GMS removed the other 2
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, alienSocket.getPosition(), (short) 1, false);
            Timer.MapTimer.getInstance().schedule(new Runnable() {
                public void run() {

                    c.getPlayer().forceReAddItem(toMount, MapleInventoryType.EQUIP);
                }
            }, 3000);//延遲3秒，防止報錯
        }
        c.getSession().write(MTSCSPacket.useAlienSocket(true));
    }

    public static void UseNebulite(LittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        Item nebulite = c.getPlayer().getInventory(MapleInventoryType.SETUP).getItem((byte) slea.readShort());
        int nebuliteId = slea.readInt();
        Item toMount = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readShort());
        if (nebulite == null || nebuliteId != nebulite.getItemId() || toMount == null || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            return;
        }
        Equip eqq = (Equip) toMount;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        boolean success = false;
        if (eqq.getSocket1() == 0 || eqq.getSocket2() == 0 || eqq.getSocket3() == 0) { // GMS removed 2nd and 3rd sockets, we can put into npc.
            StructItemOption pot = ii.getSocketInfo(nebuliteId);
            if (pot != null && GameConstants.optionTypeFits(pot.optionType, eqq.getItemId())) {
                if (eqq.getSocket1() == 0) { // priority comes first
                    eqq.setSocket1(pot.opID);
                } else if (eqq.getSocket2() == 0) {
                    eqq.setSocket2(pot.opID);
                } else if (eqq.getSocket3() == 0) {
                    eqq.setSocket3(pot.opID);
                }
                if (nebulite.getOwner() != null) {
                    eqq.setOwner(nebulite.getOwner());
                }
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, nebulite.getPosition(), (short) 1, false);
                c.getPlayer().forceReAddItem(toMount, MapleInventoryType.EQUIP);
                success = true;
            }
        }
        c.getPlayer().getMap().broadcastMessage(CField.showNebuliteEffect(c.getPlayer().getId(), success));
        c.getSession().write(CWvsContext.enableActions());
    }

    public static void UseNebuliteFusion(LittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        int nebuliteId1 = slea.readInt();
        Item nebulite1 = c.getPlayer().getInventory(MapleInventoryType.SETUP).getItem((byte) slea.readShort());
        int nebuliteId2 = slea.readInt();
        Item nebulite2 = c.getPlayer().getInventory(MapleInventoryType.SETUP).getItem((byte) slea.readShort());
        int mesos = slea.readInt();
        int premiumQuantity = slea.readInt();
        if (nebulite1 == null || nebulite2 == null || nebuliteId1 != nebulite1.getItemId() || nebuliteId2 != nebulite2.getItemId() || (mesos == 0 && premiumQuantity == 0) || (mesos != 0 && premiumQuantity != 0) || mesos < 0 || premiumQuantity < 0 || c.getPlayer().hasBlockedInventory()) {
            c.getPlayer().dropMessage(1, "Failed to fuse Nebulite.");
            c.getSession().write(InventoryPacket.getInventoryFull());
            return;
        }
        int grade1 = GameConstants.getNebuliteGrade(nebuliteId1);
        int grade2 = GameConstants.getNebuliteGrade(nebuliteId2);
        int highestRank = grade1 > grade2 ? grade1 : grade2;
        if (grade1 == -1 || grade2 == -1 || (highestRank == 3 && premiumQuantity != 2) || (highestRank == 2 && premiumQuantity != 1)
                || (highestRank == 1 && mesos != 5000) || (highestRank == 0 && mesos != 3000) || (mesos > 0 && c.getPlayer().getMeso() < mesos)
                || (premiumQuantity > 0 && c.getPlayer().getItemQuantity(4420000, false) < premiumQuantity) || grade1 >= 4 || grade2 >= 4
                || (c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 1)) { // 4000 + = S, 3000 + = A, 2000 + = B, 1000 + = C, else = D
            c.getSession().write(CField.useNebuliteFusion(c.getPlayer().getId(), 0, false));
            return; // Most of them were done in client, so we just send the unsuccessfull packet, as it is only here when they packet edit.
        }
        int avg = (grade1 + grade2) / 2; // have to revise more about grades.
        int rank = Randomizer.nextInt(100) < 4 ? (Randomizer.nextInt(100) < 70 ? (avg != 3 ? (avg + 1) : avg) : (avg != 0 ? (avg - 1) : 0)) : avg;
        // 4 % chance to up/down 1 grade, (70% to up, 30% to down), cannot up to S grade. =)
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<StructItemOption> pots = new LinkedList<>(ii.getAllSocketInfo(rank).values());
        int newId = 0;
        while (newId == 0) {
            StructItemOption pot = pots.get(Randomizer.nextInt(pots.size()));
            if (pot != null) {
                newId = pot.opID;
            }
        }
        if (mesos > 0) {
            c.getPlayer().gainMeso(-mesos, true);
        } else if (premiumQuantity > 0) {
            MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4420000, premiumQuantity, false, false);
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, nebulite1.getPosition(), (short) 1, false);
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, nebulite2.getPosition(), (short) 1, false);
        MapleInventoryManipulator.addById(c, newId, (short) 1, "Fused from " + nebuliteId1 + " and " + nebuliteId2 + " on " + FileoutputUtil.CurrentReadable_Date());
        c.getSession().write(CField.useNebuliteFusion(c.getPlayer().getId(), newId, true));
    }

    public static void UseMagnify(LittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        byte src = (byte) slea.readShort();
        boolean insight = src == 127 && c.getPlayer().getTrait(MapleTraitType.sense).getLevel() >= 30;
        Item magnify = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(src);
        byte dst = (byte) slea.readShort();
        Item toReveal = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        //  if (toReveal == null) {
        //    toReveal = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        // }
        if ((magnify == null && !insight) || toReveal == null || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            return;
        }
        Equip eqq = (Equip) toReveal;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int reqLevel = ii.getReqLevel(eqq.getItemId()) / 10;
        int lockline = eqq.getLine();
        int lockpot = eqq.getLockPot();

        if (eqq.getState() == 1 && (insight || magnify.getItemId() == 2460003 || (magnify.getItemId() == 2460002 && reqLevel <= 12) || (magnify.getItemId() == 2460001 && reqLevel <= 7) || (magnify.getItemId() == 2460000 && reqLevel <= 3))) {
            List<List<StructItemOption>> pots = new LinkedList<>(ii.getAllPotentialInfo().values());
            int new_state = Math.abs(eqq.getPotential1());
            if (new_state > 20 || new_state < 17) { // incase overflow
                new_state = 17;
            }
            int lines = 2; // default
            if (eqq.getPotential2() != 0) {
                lines++;
            }
            if (eqq.getPotential3() != 0) {
                lines++;
            }
            if (eqq.getPotential4() != 0) {
                lines++;
            }
            while (eqq.getState() != new_state) {
                //31001 = haste, 31002 = door, 31003 = se, 31004 = hb, 41005 = combat orders, 41006 = advanced blessing, 41007 = speed infusion
                for (int i = 0; i < lines; i++) { // minimum 2 lines, max 5
                    boolean rewarded = false;
                    while (!rewarded) {
                        StructItemOption pot = pots.get(Randomizer.nextInt(pots.size())).get(reqLevel);
                        if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eqq.getItemId()) && GameConstants.potentialIDFits(pot.opID, new_state, i)) { //optionType
                            //have to research optionType before making this truely official-like
                            if (i == 0) {
                                if (lockline == 1) { //使用潛能膠囊鎖
                                    eqq.setPotential1(lockpot);
                                } else {
                                    eqq.setPotential1(pot.opID);
                                }
                            } else if (i == 1) {
                                if (lockline == 2) {
                                    eqq.setPotential2(lockpot);
                                } else {
                                    eqq.setPotential2(pot.opID);
                                }
                            } else if (i == 2) {
                                if (lockline == 3) {
                                    eqq.setPotential3(lockpot);
                                } else {
                                    eqq.setPotential3(pot.opID);
                                }
                            } else if (i == 3) {
                                if (lockline == 4) {
                                    eqq.setPotential4(lockpot);
                                } else {
                                    eqq.setPotential4(pot.opID);
                                }
                            } else if (i == 4) {
                                if (lockline == 5) {
                                    eqq.setPotential5(lockpot);
                                } else {
                                    eqq.setPotential5(pot.opID);
                                }
                            }
                            rewarded = true;
                        }
                    }
                }
            }
            c.getPlayer().getTrait(MapleTraitType.insight).addExp((insight ? 10 : ((magnify.getItemId() + 2) - 2460000)) * 2, c.getPlayer());
            c.getPlayer().getMap().broadcastMessage(CField.showMagnifyingEffect(c.getPlayer().getId(), eqq.getPosition()));
            if (!insight) {
                c.getSession().write(InventoryPacket.scrolledItem(magnify, toReveal, false, true));
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, magnify.getPosition(), (short) 1, false);
            } else {
                c.getPlayer().forceReAddItem(toReveal, MapleInventoryType.EQUIP);
            }
            c.getSession().write(CWvsContext.enableActions());
        } else {
            c.getSession().write(InventoryPacket.getInventoryFull());
        }
    }

    public static final boolean UseUpgradeScroll(final short slot, final short dst, final short ws, final MapleClient c, final MapleCharacter chr, final boolean legendarySpirit, final boolean cash) {
        return UseUpgradeScroll(slot, dst, ws, c, chr, 0, legendarySpirit, cash);
    }

    public static final boolean UseUpgradeScroll(final short slot, final short dst, final short ws, final MapleClient c, final MapleCharacter chr, final int vegas, final boolean legendarySpirit, final boolean cash) {
        boolean whiteScroll = false; // white scroll being used?
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        chr.setScrolledPosition((short) 0);
        if ((ws & 2) == 2) {
            whiteScroll = true;
        }
        Equip toScroll = null;
        if (dst < 0) {
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else if (legendarySpirit) {
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        if (toScroll == null || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("test1");
            return false;
        }
        final byte oldLevel = toScroll.getLevel();
        final byte oldEnhance = toScroll.getEnhance();
        final byte oldState = toScroll.getState();
        final short oldFlag = toScroll.getFlag();
        final byte oldSlots = toScroll.getUpgradeSlots();
        boolean SLOTS_PROTECT = false;
        boolean SCROLL_PROTECT = false;
        Item scroll = cash ? chr.getInventory(MapleInventoryType.CASH).getItem(slot) : chr.getInventory(MapleInventoryType.USE).getItem(slot);
        if (scroll == null) {
            scroll = cash ? chr.getInventory(MapleInventoryType.USE).getItem(slot) : chr.getInventory(MapleInventoryType.CASH).getItem(slot);
            if (scroll == null) {
                c.getSession().write(InventoryPacket.getInventoryFull());
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("test2");
                return false;
            }
        }

        //完美回真卡 星光回真卷軸 回真卷軸 70% 回真卷軸 20% 回真卷軸 60%
        if (scroll.getItemId() == 5064200 || scroll.getItemId() == 5064201 || scroll.getItemId() == 2049600 || scroll.getItemId() == 2049601 || scroll.getItemId() == 2049604) {
            int success = ii.getScrollSuccess(scroll.getItemId());

            if (Randomizer.nextInt(100) < success || success == 0) { //現金道具 success == 0
                Equip template = (Equip) ii.getEquipById(toScroll.getItemId());
                toScroll.setStr(template.getStr());
                toScroll.setDex(template.getDex());
                toScroll.setInt(template.getInt());
                toScroll.setLuk(template.getLuk());
                toScroll.setAcc(template.getAcc());
                toScroll.setAvoid(template.getAvoid());
                toScroll.setSpeed(template.getSpeed());
                toScroll.setJump(template.getJump());
                toScroll.setEnhance(template.getEnhance());
                toScroll.setItemEXP(template.getItemEXP());
                toScroll.setHp(template.getHp());
                toScroll.setMp(template.getMp());
                toScroll.setLevel(template.getLevel());
                toScroll.setWatk(template.getWatk());
                toScroll.setMatk(template.getMatk());
                toScroll.setWdef(template.getWdef());
                toScroll.setMdef(template.getMdef());
                toScroll.setUpgradeSlots(template.getUpgradeSlots());
                toScroll.setViciousHammer(template.getViciousHammer());
                toScroll.setIncSkill(template.getIncSkill());

                if (ItemFlag.SHIELD_WARD.check(oldFlag)) { //裝備保護
                    toScroll.setFlag((short) (toScroll.getFlag() - ItemFlag.SHIELD_WARD.getValue()));
                }

                if (ItemFlag.SCROLL_PROTECT.check(oldFlag)) { //卷軸保護
                    toScroll.setFlag((short) (toScroll.getFlag() - ItemFlag.SCROLL_PROTECT.getValue()));
                }

                MapleInventoryManipulator.removeFromSlot(c, success == 0 ? MapleInventoryType.CASH : MapleInventoryType.USE, scroll.getPosition(), (short) 1, false, false);

                c.getSession().write(InventoryPacket.scrolledItem(scroll, toScroll, false, false));
                chr.getMap().broadcastMessage(chr, CField.getScrollEffect(c.getPlayer().getId(), ScrollResult.SUCCESS, legendarySpirit ? true : false, false), true); //回真成功

                c.getSession().write(CWvsContext.enableActions());
                return false;
            }
        }
        if (!GameConstants.isSpecialScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId())) {
            if (toScroll.getUpgradeSlots() < 1) {
                if (legendarySpirit) {
                    c.getPlayer().getMap().broadcastMessage(CField.getScrollEffect(c.getPlayer().getId(), Equip.ScrollResult.FAIL, legendarySpirit, false));
                }
                c.getSession().write(InventoryPacket.getInventoryFull());
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("test3");
                return false;
            }
        } else if (GameConstants.isEquipScroll(scroll.getItemId())) {
            if (toScroll.getUpgradeSlots() >= 1 || toScroll.getEnhance() >= 100 || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                c.getSession().write(InventoryPacket.getInventoryFull());
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("test4");
                return false;
            }
        } else if (GameConstants.isPotentialScroll(scroll.getItemId())) {
            final boolean isEpic = scroll.getItemId() / 100 == 20497;
            if ((!isEpic && toScroll.getState() >= 1) || (isEpic && toScroll.getState() >= 18) || (toScroll.getLevel() == 0 && toScroll.getUpgradeSlots() == 0 && toScroll.getItemId() / 10000 != 135 && !isEpic) || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                c.getSession().write(InventoryPacket.getInventoryFull());
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("test5");
                return false;
            }
        } else if (GameConstants.isSpecialScroll(scroll.getItemId())) {
            if (ii.isCash(toScroll.getItemId()) || toScroll.getEnhance() >= 8) {
                c.getSession().write(InventoryPacket.getInventoryFull());
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("test6");
                return false;
            }
        }
        if (!GameConstants.canScroll(toScroll.getItemId()) && !GameConstants.isChaosScroll(toScroll.getItemId())) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("test7");
            return false;
        }
        if ((GameConstants.isCleanSlate(scroll.getItemId()) || GameConstants.isTablet(scroll.getItemId()) || GameConstants.isGeneralScroll(scroll.getItemId()) || GameConstants.isChaosScroll(scroll.getItemId())) && (vegas > 0 || ii.isCash(toScroll.getItemId()))) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("test8");
            return false;
        }
        if (GameConstants.isTablet(scroll.getItemId()) && toScroll.getDurability() < 0) { //not a durability item
            c.getSession().write(InventoryPacket.getInventoryFull());
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("test9");
            return false;
        } else if ((!GameConstants.isTablet(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isSpecialScroll(scroll.getItemId()) && !GameConstants.isChaosScroll(scroll.getItemId())) && toScroll.getDurability() >= 0) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("test10");
            return false;
        }
        Item wscroll = null;

        // Anti cheat and validation
        List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
        if (scrollReqs != null && scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("test11");
            return false;
        }

        if (whiteScroll) {
            wscroll = chr.getInventory(MapleInventoryType.USE).findById(2340000);
            if (wscroll == null) {
                whiteScroll = false;
            }
        }
        if ((GameConstants.isTablet(scroll.getItemId()) || GameConstants.isGeneralScroll(scroll.getItemId())) && !(toScroll.getItemId() / 10000 == 166)) {
            switch (scroll.getItemId() % 1000 / 100) {
                case 0: //1h
                    if (GameConstants.isTwoHanded(toScroll.getItemId()) || !GameConstants.isWeapon(toScroll.getItemId())) {
                        c.getSession().write(CWvsContext.enableActions());
                        System.out.println("test12");
                        return false;
                    }
                    break;
                case 1: //2h
                    if (!GameConstants.isTwoHanded(toScroll.getItemId()) || !GameConstants.isWeapon(toScroll.getItemId())) {
                        c.getSession().write(CWvsContext.enableActions());
                        System.out.println("test13");
                        return false;
                    }
                    break;
                case 2: //armor
                    if (GameConstants.isAccessory(toScroll.getItemId()) || GameConstants.isWeapon(toScroll.getItemId())) {
                        c.getSession().write(CWvsContext.enableActions());
                        System.out.println("test14");
                        return false;
                    }
                    break;
                case 3: //accessory
                    if (!GameConstants.isAccessory(toScroll.getItemId()) || GameConstants.isWeapon(toScroll.getItemId())) {
                        c.getSession().write(CWvsContext.enableActions());
                        System.out.println("test15");
                        return false;
                    }
                    break;
            }
        } else if (!GameConstants.isAccessoryScroll(scroll.getItemId()) && !GameConstants.isChaosScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId()) && !GameConstants.isSpecialScroll(scroll.getItemId())) {
            if (!ii.canScroll(scroll.getItemId(), toScroll.getItemId())) {
                if (toScroll.getAndroid2() == false) {
                    c.getSession().write(CWvsContext.enableActions());
                    System.out.println("test16");
                    return false;
                }
            }
        }
        if (GameConstants.isAccessoryScroll(scroll.getItemId()) && !GameConstants.isAccessory(toScroll.getItemId())) {
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("test17");
            return false;
        }
        if (scroll.getQuantity() <= 0) {
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("test18");
            return false;
        }

        if (legendarySpirit && vegas == 0) {
            if (chr.getSkillLevel(SkillFactory.getSkill(chr.getStat().getSkillByJob(1003, chr.getJob()))) <= 0) {
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("test19");
                return false;
            }
        }
        // Scroll Success/ Failure/ Curse
        Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll, whiteScroll, chr, vegas);
        ScrollResult scrollSuccess;
        if (scrolled == null) {
            scrollSuccess = Equip.ScrollResult.CURSE;
            if (ItemFlag.SCROLL_PROTECT.check(toScroll.getFlag())) {//卷軸保護
                SCROLL_PROTECT = true;
            }
        } else if (scrolled.getLevel() > oldLevel || scrolled.getEnhance() > oldEnhance || scrolled.getState() > oldState || scrolled.getFlag() > oldFlag) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        } else if ((GameConstants.isCleanSlate(scroll.getItemId()) && scrolled.getUpgradeSlots() > oldSlots)) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        } else {
            scrollSuccess = Equip.ScrollResult.FAIL;
            if (ItemFlag.SCROLL_PROTECT.check(toScroll.getFlag())) {
                SCROLL_PROTECT = true;
            }
        }
        // Update
        if (SCROLL_PROTECT) {
            chr.dropMessage(5, "Due to the protective effect of the scroll, the scroll" + ii.getName(scroll.getItemId()) + "No damage.");
        } else {
            chr.getInventory(GameConstants.getInventoryType(scroll.getItemId())).removeItem(scroll.getPosition(), (short) 1, false);
        }
        if (whiteScroll) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, wscroll.getPosition(), (short) 1, false, false);
        } else if (scrollSuccess == Equip.ScrollResult.FAIL && scrolled.getUpgradeSlots() < oldSlots && c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5640000) != null) {
            chr.setScrolledPosition(scrolled.getPosition());
            if (vegas == 0) {
                c.getSession().write(CWvsContext.pamSongUI());
            }
        }
        if (ItemFlag.SHIELD_WARD.check(oldFlag)) {
            toScroll.setFlag((short) (toScroll.getFlag() - ItemFlag.SHIELD_WARD.getValue()));
        }
        if (ItemFlag.SLOTS_PROTECT.check(oldFlag)) {
            toScroll.setFlag((short) (toScroll.getFlag() - ItemFlag.SLOTS_PROTECT.getValue()));
        }
        if (ItemFlag.SCROLL_PROTECT.check(oldFlag)) {
            toScroll.setFlag((short) (toScroll.getFlag() - ItemFlag.SCROLL_PROTECT.getValue()));
        }
        if (scrollSuccess == Equip.ScrollResult.CURSE) {
            c.getSession().write(InventoryPacket.scrolledItem(scroll, toScroll, true, false));
            if (dst < 0) {
                chr.getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                chr.getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else if (vegas == 0) {
            c.getSession().write(InventoryPacket.scrolledItem(scroll, scrolled, false, false));
        }

        chr.getMap().broadcastMessage(chr, CField.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit, whiteScroll), vegas == 0);

        // equipped item was scrolled and changed
        if (dst < 0 && (scrollSuccess == Equip.ScrollResult.SUCCESS || scrollSuccess == Equip.ScrollResult.CURSE) && vegas == 0) {
            chr.equipChanged();
        }
        return true;
    }

    /*
    使用裝備保護捲軸設定
    */
    public static void UseProtectShield(LittleEndianAccessor slea, MapleClient c) {
        byte slot = (byte) slea.readShort();
        byte dst = (byte) slea.readShort();
        slea.skip(1);
        boolean use = false;
        boolean legendarySpirit = false;
        Equip toScroll;
        Equip.ScrollResult scrollSuccess = Equip.ScrollResult.SUCCESS;
        if (dst < 0) {
            toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else {
            legendarySpirit = true;
            toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        Item scroll = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot);
        if (scroll == null || !GameConstants.isSpecialScroll(scroll.getItemId())) {
            scroll = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
            use = true;
        }
        if (!use) {
            if (scroll.getItemId() == 5064000 || scroll.getItemId() == 5064002) { //裝備保護卷軸 星光裝備保護卷軸
                short flag = toScroll.getFlag();
                flag |= ItemFlag.SHIELD_WARD.getValue();
                toScroll.setFlag(flag);
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 5064100 || scroll.getItemId() == 5064101) { //安全盾牌卷軸 星光安全盾牌卷軸
                short flag = toScroll.getFlag();
                flag |= ItemFlag.SLOTS_PROTECT.getValue();
                toScroll.setFlag(flag);
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 5064300 || scroll.getItemId() == 5064301) { //卷軸保護卡 星光卷軸保護卡
                short flag = toScroll.getFlag();
                flag |= ItemFlag.SCROLL_PROTECT.getValue();
                toScroll.setFlag(flag);
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 5063000) { //幸運鑰匙
                short flag = toScroll.getFlag();
                flag |= ItemFlag.LUCKS_KEY.getValue();
                toScroll.setFlag(flag);
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 5063100) { //幸運保護券
                short flag = toScroll.getFlag();
                if (!ItemFlag.LUCKS_KEY.check(flag) && !ItemFlag.SHIELD_WARD.check(flag)) {
                    flag |= ItemFlag.LUCKS_KEY.getValue();
                    flag |= ItemFlag.SHIELD_WARD.getValue();
                    toScroll.setFlag(flag);
                    c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
                } else {
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
            }
            c.getPlayer().getInventory(MapleInventoryType.CASH).removeItem(scroll.getPosition(), (short) 1, false);
        } else {
            if (scroll.getItemId() == 2531000) { //保護卷軸
                short flag = toScroll.getFlag();
                flag |= ItemFlag.SHIELD_WARD.getValue();
                toScroll.setFlag(flag);
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 2532000) { //安全卷軸
                short flag = toScroll.getFlag();
                flag |= ItemFlag.SLOTS_PROTECT.getValue();
                toScroll.setFlag(flag);
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 2530000 || scroll.getItemId() == 2530001 || scroll.getItemId() == 2530002) { //幸運日卷軸
                short flag = toScroll.getFlag();
                flag |= ItemFlag.LUCKS_KEY.getValue();
                toScroll.setFlag(flag);
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            }
            c.getPlayer().getInventory(MapleInventoryType.USE).removeItem(scroll.getPosition(), (short) 1, false);
        }
        c.getSession().write(InventoryPacket.scrolledItem(scroll, toScroll, false, false));
        c.getPlayer().getMap().broadcastMessage(CField.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit, false));
        c.getSession().write(CWvsContext.enableActions());
    }

    /*
    魔法轉盤
    */
    public static void UseMagicWheel(LittleEndianAccessor slea, MapleClient c) {
        byte action = slea.readByte();
        if (action == 0x02) {
            int ivtype = slea.readInt();
            byte slot = (byte) slea.readInt();
            int itemid = slea.readInt();
            int type = itemid == 4400001 ? 1 : itemid == 4400002 ? 2 : 0;
            if (itemid == 4400000 || itemid == 4400001 || itemid == 4400002) {//支援道具ID：4400000
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, (byte) slot, (short) 1, false);
                if (c.getPlayer().getInventory(MapleInventoryType.USE).getNextFreeSlot() < 1
                        || c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() < 1
                        || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNextFreeSlot() < 1
                        || c.getPlayer().getInventory(MapleInventoryType.CASH).getNextFreeSlot() < 1
                        || c.getPlayer().getInventory(MapleInventoryType.ETC).getNextFreeSlot() < 1) {
                    c.getSession().write(CWvsContext.magicWheelMessage((byte) 7));
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
                List<Integer> items = new ArrayList<Integer>();

                Connection con = null;
                PreparedStatement ps = null;
                ResultSet rs = null;
                try {
                    con = DatabaseConnection.getConnection();
                    ps = con.prepareStatement("SELECT * FROM `wheeldata` WHERE `type` = ? ORDER BY RAND() LIMIT 10");
                    ps.setInt(1, type);
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        items.add(rs.getInt("itemid"));
                    }

                } catch (SQLException ex) {
                    ex.printStackTrace();
                } finally {
                    if (ps != null) {
                        try {
                            ps.close();
                        } catch (Exception e) {
                        }
                    }
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (Exception e) {
                        }
                    }
                }
                MagicWheel mw = new MagicWheel(items);
                c.getPlayer().setMagicWheel(mw);
                c.getSession().write(CWvsContext.magicWheelStart(items, mw.getUniqueId(), mw.getRandom()));
            } else {
                c.getSession().write(CWvsContext.magicWheelMessage((byte) 7));
                c.getSession().write(CWvsContext.enableActions());
            }
        } else if (action == 0x04) {
            String uniqueid = slea.readMapleAsciiString();
            MagicWheel mw = c.getPlayer().getMagicWheel();
            MapleInventoryManipulator.addById(c, mw.getItemId(mw.getRandom()), (short) 1, "");
            c.getSession().write(CWvsContext.magicWheelMessage((byte) 5));
            c.getPlayer().setMagicWheel(null);
            // c.getPlayer().setMagicWheel(mw);
        }
    }

    public static boolean UseSkillBook(byte slot, int itemId, MapleClient c, MapleCharacter chr) {
        Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || chr.hasBlockedInventory()) {
            return false;
        }
        Map<String, Integer> skilldata = MapleItemInformationProvider.getInstance().getEquipStats(toUse.getItemId());
        if (skilldata == null) { // Hacking or used an unknown item
            return false;
        }
        boolean canuse = false, success = false;
        int skill = 0, maxlevel = 0;

        Integer SuccessRate = skilldata.get("success");
        Integer ReqSkillLevel = skilldata.get("reqSkillLevel");
        Integer MasterLevel = skilldata.get("masterLevel");

        byte i = 0;
        Integer CurrentLoopedSkillId;
        while (true) {
            CurrentLoopedSkillId = skilldata.get("skillid" + i);
            i++;
            if (CurrentLoopedSkillId == null || MasterLevel == null) {
                break; // End of data
            }
            Skill CurrSkillData = SkillFactory.getSkill(CurrentLoopedSkillId);
            if (CurrSkillData != null && CurrSkillData.canBeLearnedBy(chr.getJob()) && (ReqSkillLevel == null || chr.getSkillLevel(CurrSkillData) >= ReqSkillLevel) && chr.getMasterLevel(CurrSkillData) < MasterLevel) {
                canuse = true;
                if (SuccessRate == null || Randomizer.nextInt(100) <= SuccessRate) {
                    success = true;
                    chr.changeSingleSkillLevel(CurrSkillData, chr.getSkillLevel(CurrSkillData), (byte) (int) MasterLevel);
                } else {
                    success = false;
                }
                MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(itemId), slot, (short) 1, false);
                break;
            }
        }
        c.getPlayer().getMap().broadcastMessage(CWvsContext.useSkillBook(chr, skill, maxlevel, canuse, success));
        c.getSession().write(CWvsContext.enableActions());
        return canuse;
    }

    /*
    使用捕捉怪物道具
    */
    public static void UseCatchItem(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt();
        final MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        final MapleMap map = chr.getMap();

        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mob != null && !chr.hasBlockedInventory() && itemid / 10000 == 227 && MapleItemInformationProvider.getInstance().getCardMobId(itemid) == mob.getId()) {
            if (!MapleItemInformationProvider.getInstance().isMobHP(itemid) || mob.getHp() <= mob.getMobMaxHp() / 2) {
                map.broadcastMessage(MobPacket.catchMonster(mob.getObjectId(), itemid, (byte) 1));
                map.killMonster(mob, chr, true, false, (byte) 1);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false, false);
                if (MapleItemInformationProvider.getInstance().getCreateId(itemid) > 0) {
                    MapleInventoryManipulator.addById(c, MapleItemInformationProvider.getInstance().getCreateId(itemid), (short) 1, "Catch item " + itemid + " on " + FileoutputUtil.CurrentReadable_Date());
                }
            } else {
                //map.broadcastMessage(MobPacket.catchMonster(mob.getObjectId(), itemid, (byte) 0));//注釋掉 版本不支援0 特效
                map.broadcastMessage(MobPacket.getMobCoolEffect(mob.getObjectId(), itemid));//添加COOL 特效
                c.getSession().write(CWvsContext.catchMob(mob.getId(), itemid, (byte) 0));
            }
        }
        c.getSession().write(CWvsContext.enableActions());
    }

    public static void UseMountFood(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemid = slea.readInt(); //2260000 usually
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        MapleMount mount = chr.getMount();

        if (itemid / 10000 == 226 && toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mount != null && !c.getPlayer().hasBlockedInventory()) {
            int fatigue = mount.getFatigue();

            boolean levelup = false;
            mount.setFatigue((byte) -30);

            if (fatigue > 0) {
                mount.increaseExp();
                int level = mount.getLevel();
                if (level < 30 && mount.getExp() >= GameConstants.getMountExpNeededForLevel(level + 1)) {
                    mount.setLevel((byte) (level + 1));
                    levelup = true;
                }
            }
            chr.getMap().broadcastMessage(CWvsContext.updateMount(chr, levelup));
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().write(CWvsContext.enableActions());
    }

    /*
    雇傭商人遙控器
    */
    public static void useRemoteHiredMerchant(LittleEndianAccessor slea, MapleClient c) {
        short slot = slea.readShort();
        Item item = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null) {
            c.getSession().close(); //hack
            return;
        }
        if (item.getItemId() != 5470000 || item.getQuantity() <= 0) {
            c.getSession().close(); //hack
            return;
        }
        boolean use = false;

        HiredMerchant merchant = c.getChannelServer().findAndGetMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());

        if (merchant == null) {
            c.getPlayer().dropMessage(1, "There is no store set up on the current channel.");
            return;
        }

        if (FieldLimitType.ChannelSwitch.check(c.getPlayer().getMap().getFieldLimit())) {
            c.getPlayer().dropMessage(1, "This map cannot be used.");
            return;
        }

        MapleCharacter chr = c.getPlayer();

        if (merchant.isOwner(chr) && merchant.isOpen() && merchant.isAvailable()) {
            merchant.setOpen(false);
            merchant.removeAllVisitors((byte) 17, (byte) 1);
            chr.setPlayerShop(merchant);
            c.getSession().write(PlayerShopPacket.getHiredMerch(chr, merchant, false));
            use = true;
        }
    }

    public static void UseScriptedNPCItem(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);
        long expiration_days = 0;
        int mountid = 0;

        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && !chr.hasBlockedInventory() && !chr.inPVP()) {
            switch (toUse.getItemId()) {
                case 2430692: //星岩箱子
                    if (c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430692) >= 1) {
                            int rank = Randomizer.nextInt(100) < 30 ? (Randomizer.nextInt(100) < 4 ? 2 : 1) : 0;
                            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                            List<StructItemOption> pots = new LinkedList<>(ii.getAllSocketInfo(rank).values());
                            int newId = 0;
                            while (newId == 0) {
                                StructItemOption pot = pots.get(Randomizer.nextInt(pots.size()));
                                if (pot != null) {
                                    newId = pot.opID;
                                }
                            }
                            if (MapleInventoryManipulator.checkSpace(c, newId, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, newId, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getSession().write(InfoPacket.getShowItemGain(newId, (short) 1, true));
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "You do not have a Nebulite Box.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Please make some space.");
                    }
                    break;
                case 2430144: //秘密的技能書
                    int itemid = Randomizer.nextInt(373) + 2290000;
                    if (MapleItemInformationProvider.getInstance().itemExists(itemid) && !MapleItemInformationProvider.getInstance().getName(itemid).contains("Special") && !MapleItemInformationProvider.getInstance().getName(itemid).contains("Event")) {
                        MapleInventoryManipulator.addById(c, itemid, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    }
                    break;
                case 2430370: //秘密配方
                    if (MapleInventoryManipulator.checkSpace(c, 2028062, (short) 1, "")) {
                        MapleInventoryManipulator.addById(c, 2028062, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    }
                    break;
                default:
                    NPCScriptManager.getInstance().startItemScript(c, 9010000, "" + itemId); //maple admin as default npc
                    break;
            }
        }
        c.getSession().write(CWvsContext.enableActions());
    }

    public static void UseSummonBag(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (!chr.isAlive() || chr.hasBlockedInventory() || chr.inPVP()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.lastsackcompare = System.currentTimeMillis() - c.lastsack;

        if (c.lastsackcompare > 300000) {
            c.lastsack = System.currentTimeMillis();
            slea.readInt();
            byte slot = (byte) slea.readShort();
            int itemId = slea.readInt();
            Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

            if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && (c.getPlayer().getMapId() < 910000000 || c.getPlayer().getMapId() > 910000022)) {
                Map<String, Integer> toSpawn = MapleItemInformationProvider.getInstance().getEquipStats(itemId);

                if (toSpawn == null) {
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
                MapleMonster ht = null;
                int type = 0;
                for (Entry<String, Integer> i : toSpawn.entrySet()) {
                    if (i.getKey().startsWith("mob") && Randomizer.nextInt(99) <= i.getValue()) {
                        ht = MapleLifeFactory.getMonster(Integer.parseInt(i.getKey().substring(3)));
                        chr.getMap().spawnMonster_sSack(ht, chr.getPosition(), type);
                    }
                }
                if (ht == null) {
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }

                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            }
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CWvsContext.serverNotice(5, chr.getName() + " used a summoning bag with ID: " + itemId), false);
            c.getSession().write(CWvsContext.enableActions());

        } else {
            c.getPlayer().dropMessage(5, "You can use sack once per 300 secs.");
            c.getSession().write(CWvsContext.enableActions());
        }
    }

    public static void UseTreasureChest(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        short slot = slea.readShort();
        int itemid = slea.readInt();

        Item toUse = chr.getInventory(MapleInventoryType.ETC).getItem((byte) slot);
        if (toUse == null || toUse.getQuantity() <= 0 || toUse.getItemId() != itemid || chr.hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int reward;
        int keyIDforRemoval;
        String box;

        switch (toUse.getItemId()) {
            case 4280000: // Gold box
                reward = RandomRewards.getGoldBoxReward();
                keyIDforRemoval = 5490000;
                box = "Gold";
                break;
            case 4280001: // Silver box
                reward = RandomRewards.getSilverBoxReward();
                keyIDforRemoval = 5490001;
                box = "Silver";
                break;
            default: // Up to no good
                return;
        }

        // Get the quantity
        int amount = 1;
        switch (reward) {
            case 2000004:
                amount = 200; // Elixir
                break;
            case 2000005:
                amount = 100; // Power Elixir
                break;
        }
        if (chr.getInventory(MapleInventoryType.CASH).countById(keyIDforRemoval) > 0) {
            Item item = MapleInventoryManipulator.addbyId_Gachapon(c, reward, (short) amount);

            if (item == null) {
                chr.dropMessage(5, "Please check your item inventory and see if you have a Master Key, or if the inventory is full.");
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, (byte) slot, (short) 1, true);
            MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, keyIDforRemoval, 1, true, false);
            c.getSession().write(InfoPacket.getShowItemGain(reward, (short) amount, true));

            if (GameConstants.gachaponRareItem(item.getItemId()) > 0) {
                World.Broadcast.broadcastSmega(CWvsContext.getGachaponMega(c.getPlayer().getName(), " : got a(n)", item, (byte) 2, "[" + box + " Chest]"));
            }
        } else {
            chr.dropMessage(5, "Please check your item inventory and see if you have a Master Key, or if the inventory is full.");
            c.getSession().write(CWvsContext.enableActions());
        }
    }

    public static void UseCashItem(LittleEndianAccessor slea, MapleClient c) {
        c.lastsmegacomparee = System.currentTimeMillis() - c.lastsmegaa;
        if (c.lastsmegacomparee > 1000) {
            c.lastsmegaa = System.currentTimeMillis();

            if (c.getPlayer().getMap().getId() == GameConstants.JAIL) {
                c.getPlayer().dropMessage(5, "You're in jail, herp derp.");
                c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
                return;
            }
            if (c.getPlayer().isMuted() || (c.getPlayer().getMap().getMuted() && !c.getPlayer().isGM())) {
                c.getPlayer().dropMessage(5, c.getPlayer().isMuted() ? "You are Muted, therefore you are unable to talk. " : "The map is Muted, therefore you are unable to talk.");
                c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
                return;
            }
            if (c.getPlayer() == null || c.getPlayer().getMap() == null || c.getPlayer().inPVP()) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            slea.readInt();
            c.getPlayer().setScrolledPosition((short) 0);
            byte slot = (byte) slea.readShort();
            int itemId = slea.readInt();
            Item toUse = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot);
            if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1 || c.getPlayer().hasBlockedInventory()) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }

            boolean used = false, cc = false;

            switch (itemId) {
                case 2320000: //瞬移之石
                case 5040000: //瞬移之石
                case 5040001://可樂翅膀
                case 5040003: //新瞬移之石
                case 5040004: //超能瞬移之石

                case 5041000: //高級瞬移之石
                case 5041001: { //高級超能瞬移之石(沒有)
                    used = UseTeleRock(slea, c, itemId);
                    break;
                }
                case 5450005: { //[30天]移動倉庫王老闆
                    c.getPlayer().setConversation(4);
                    c.getPlayer().getStorage().sendStorage(c, 1022005);
                    break;
                }
                case 5050000: { //能力點數重配捲軸
                    Map<MapleStat, Integer> statupdate = new EnumMap<>(MapleStat.class);
                    int apto = GameConstants.GMS ? (int) slea.readLong() : slea.readInt();
                    int apfrom = GameConstants.GMS ? (int) slea.readLong() : slea.readInt();

                    if (apto == apfrom) {
                        break; // Hack
                    }
                    int job = c.getPlayer().getJob();
                    PlayerStats playerst = c.getPlayer().getStat();
                    used = true;

                    switch (apto) { // AP to
                        case 64: // str
                            if (playerst.getStr() >= 999) {
                                used = false;
                            }
                            break;
                        case 128: // dex
                            if (playerst.getDex() >= 999) {
                                used = false;
                            }
                            break;
                        case 256: // int
                            if (playerst.getInt() >= 999) {
                                used = false;
                            }
                            break;
                        case 512: // luk
                            if (playerst.getLuk() >= 999) {
                                used = false;
                            }
                            break;
                        case 2048: // hp
                            if (playerst.getMaxHp() >= 99999) {
                                used = false;
                            }
                            break;
                        case 8192: // mp
                            if (playerst.getMaxMp() >= 99999) {
                                used = false;
                            }
                            break;
                    }
                    switch (apfrom) { // AP to
                        case 64: // str
                            if (playerst.getStr() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 1 && playerst.getStr() <= 35)) {
                                used = false;
                            }
                            break;
                        case 128: // dex
                            if (playerst.getDex() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 3 && playerst.getDex() <= 25) || (c.getPlayer().getJob() % 1000 / 100 == 4 && playerst.getDex() <= 25) || (c.getPlayer().getJob() % 1000 / 100 == 5 && playerst.getDex() <= 20)) {
                                used = false;
                            }
                            break;
                        case 256: // int
                            if (playerst.getInt() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 2 && playerst.getInt() <= 20)) {
                                used = false;
                            }
                            break;
                        case 512: // luk
                            if (playerst.getLuk() <= 4) {
                                used = false;
                            }
                            break;
                        case 2048: // hp
                            if (/*
                             * playerst.getMaxMp() <
                             * ((c.getPlayer().getLevel() * 14) + 134) ||
                             */c.getPlayer().getHpApUsed() <= 0 || c.getPlayer().getHpApUsed() >= 10000) {
                                used = false;
                                c.getPlayer().dropMessage(1, "You need points in HP or MP in order to take points out.");
                            }
                            break;
                        case 8192: // mp
                            if (/*
                             * playerst.getMaxMp() <
                             * ((c.getPlayer().getLevel() * 14) + 134) ||
                             */c.getPlayer().getHpApUsed() <= 0 || c.getPlayer().getHpApUsed() >= 10000) {
                                used = false;
                                c.getPlayer().dropMessage(1, "You need points in HP or MP in order to take points out.");
                            }
                            break;
                    }
                    if (used) {
                        switch (apto) { // AP to
                            case 64: { // str
                                int toSet = playerst.getStr() + 1;
                                playerst.setStr((short) toSet, c.getPlayer());
                                statupdate.put(MapleStat.STR, toSet);
                                break;
                            }
                            case 128: { // dex
                                int toSet = playerst.getDex() + 1;
                                playerst.setDex((short) toSet, c.getPlayer());
                                statupdate.put(MapleStat.DEX, toSet);
                                break;
                            }
                            case 256: { // int
                                int toSet = playerst.getInt() + 1;
                                playerst.setInt((short) toSet, c.getPlayer());
                                statupdate.put(MapleStat.INT, toSet);
                                break;
                            }
                            case 512: { // luk
                                int toSet = playerst.getLuk() + 1;
                                playerst.setLuk((short) toSet, c.getPlayer());
                                statupdate.put(MapleStat.LUK, toSet);
                                break;
                            }
                            case 2048: // hp
                                int maxhp = playerst.getMaxHp();
                                if (GameConstants.isBeginnerJob(job)) { // Beginner
                                    maxhp += Randomizer.rand(4, 8);
                                } else if ((job >= 100 && job <= 132) || (job >= 3200 && job <= 3212) || (job >= 1100 && job <= 1112) || (job >= 3100 && job <= 3112)) { // Warrior
                                    maxhp += Randomizer.rand(36, 42);
                                } else if ((job >= 200 && job <= 232) || (GameConstants.isEvan(job)) || (job >= 1200 && job <= 1212)) { // Magician
                                    maxhp += Randomizer.rand(10, 12);
                                } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman
                                    maxhp += Randomizer.rand(14, 18);
                                } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) {
                                    maxhp += Randomizer.rand(24, 28);
                                } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
                                    maxhp += Randomizer.rand(16, 20);
                                } else if (job >= 2000 && job <= 2112) { // Aran
                                    maxhp += Randomizer.rand(34, 38);
                                } else { // GameMaster
                                    maxhp += Randomizer.rand(50, 100);
                                }
                                maxhp = Math.min(99999, Math.abs(maxhp));
                                c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() + 1));
                                playerst.setMaxHp(maxhp, c.getPlayer());
                                statupdate.put(MapleStat.MAXHP, (int) maxhp);
                                break;

                            case 8192: // mp
                                int maxmp = playerst.getMaxMp();

                                if (GameConstants.isBeginnerJob(job)) { // Beginner
                                    maxmp += Randomizer.rand(6, 8);
                                } else if (job >= 3100 && job <= 3112) {
                                    break;
                                } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1112) || (job >= 2000 && job <= 2112)) { // Warrior
                                    maxmp += Randomizer.rand(4, 9);
                                } else if ((job >= 200 && job <= 232) || (GameConstants.isEvan(job)) || (job >= 3200 && job <= 3212) || (job >= 1200 && job <= 1212)) { // Magician
                                    maxmp += Randomizer.rand(32, 36);
                                } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 500 && job <= 532) || (job >= 3200 && job <= 3212) || (job >= 3500 && job <= 3512) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 1500 && job <= 1512) || (job >= 2300 && job <= 2312)) { // Bowman
                                    maxmp += Randomizer.rand(8, 10);
                                } else { // GameMaster
                                    maxmp += Randomizer.rand(50, 100);
                                }
                                maxmp = Math.min(99999, Math.abs(maxmp));
                                c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() + 1));
                                playerst.setMaxMp(maxmp, c.getPlayer());
                                statupdate.put(MapleStat.MAXMP, (int) maxmp);
                                break;
                        }
                        switch (apfrom) { // AP from
                            case 64: { // str
                                int toSet = playerst.getStr() - 1;
                                playerst.setStr((short) toSet, c.getPlayer());
                                statupdate.put(MapleStat.STR, toSet);
                                break;
                            }
                            case 128: { // dex
                                int toSet = playerst.getDex() - 1;
                                playerst.setDex((short) toSet, c.getPlayer());
                                statupdate.put(MapleStat.DEX, toSet);
                                break;
                            }
                            case 256: { // int
                                int toSet = playerst.getInt() - 1;
                                playerst.setInt((short) toSet, c.getPlayer());
                                statupdate.put(MapleStat.INT, toSet);
                                break;
                            }
                            case 512: { // luk
                                int toSet = playerst.getLuk() - 1;
                                playerst.setLuk((short) toSet, c.getPlayer());
                                statupdate.put(MapleStat.LUK, toSet);
                                break;
                            }
                            case 2048: // HP
                                int maxhp = playerst.getMaxHp();
                                if (GameConstants.isBeginnerJob(job)) { // Beginner
                                    maxhp -= 12;
                                } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1212)) { // Magician
                                    maxhp -= 10;
                                } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 3300 && job <= 3312) || (job >= 3500 && job <= 3512) || (job >= 2300 && job <= 2312)) { // Bowman, Thief
                                    maxhp -= 15;
                                } else if ((job >= 500 && job <= 532) || (job >= 1500 && job <= 1512)) { // Pirate
                                    maxhp -= 22;
                                } else if (((job >= 100 && job <= 132) || job >= 1100 && job <= 1112) || (job >= 3100 && job <= 3112)) { // Soul Master
                                    maxhp -= 32;
                                } else if ((job >= 2000 && job <= 2112) || (job >= 3200 && job <= 3212)) { // Aran
                                    maxhp -= 40;
                                } else { // GameMaster
                                    maxhp -= 20;
                                }
                                c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() - 1));
                                playerst.setMaxHp(maxhp, c.getPlayer());
                                statupdate.put(MapleStat.MAXHP, (int) maxhp);
                                break;
                            case 8192: // MP
                                int maxmp = playerst.getMaxMp();
                                if (GameConstants.isBeginnerJob(job)) { // Beginner
                                    maxmp -= 8;
                                } else if (job >= 3100 && job <= 3112) {
                                    break;
                                } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1112)) { // Warrior
                                    maxmp -= 4;
                                } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1212)) { // Magician
                                    maxmp -= 30;
                                } else if ((job >= 500 && job <= 532) || (job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 1500 && job <= 1512) || (job >= 3300 && job <= 3312) || (job >= 3500 && job <= 3512) || (job >= 2300 && job <= 2312)) { // Pirate, Bowman. Thief
                                    maxmp -= 10;
                                } else if (job >= 2000 && job <= 2112) { // Aran
                                    maxmp -= 5;
                                } else { // GameMaster
                                    maxmp -= 20;
                                }
                                c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() - 1));
                                playerst.setMaxMp(maxmp, c.getPlayer());
                                statupdate.put(MapleStat.MAXMP, (int) maxmp);
                                break;
                        }
                        c.getSession().write(CWvsContext.updatePlayerStats(statupdate, true, c.getPlayer()));
                    }
                    break;
                }
                case 5220083: {//怪物夥伴
                    used = true;
                    for (Entry<Integer, StructFamiliar> f : MapleItemInformationProvider.getInstance().getFamiliars().entrySet()) {
                        if (f.getValue().itemid == 2870055 || f.getValue().itemid == 2871002 || f.getValue().itemid == 2870235 || f.getValue().itemid == 2870019) {
                            MonsterFamiliar mf = c.getPlayer().getFamiliars().get(f.getKey());
                            if (mf != null) {
                                if (mf.getVitality() >= 3) {
                                    mf.setExpiry((long) Math.min(System.currentTimeMillis() + 90 * 24 * 60 * 60000L, mf.getExpiry() + 30 * 24 * 60 * 60000L));
                                } else {
                                    mf.setVitality(mf.getVitality() + 1);
                                    mf.setExpiry((long) (mf.getExpiry() + 30 * 24 * 60 * 60000L));
                                }
                            } else {
                                mf = new MonsterFamiliar(c.getPlayer().getId(), f.getKey(), (long) (System.currentTimeMillis() + 30 * 24 * 60 * 60000L));
                                c.getPlayer().getFamiliars().put(f.getKey(), mf);
                            }
                            c.getSession().write(CField.registerFamiliar(mf));
                        }
                    }
                    break;
                }
                case 5220084: {//怪物夥伴
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 3) {
                        c.getPlayer().dropMessage(5, "Make 3 USE space.");
                        break;
                    }
                    used = true;
                    int[] familiars = new int[3];
                    while (true) {
                        for (int i = 0; i < familiars.length; i++) {
                            if (familiars[i] > 0) {
                                continue;
                            }
                            for (Map.Entry<Integer, StructFamiliar> f : MapleItemInformationProvider.getInstance().getFamiliars().entrySet()) {
                                if (Randomizer.nextInt(500) == 0 && ((i < 2 && f.getValue().grade == 0 || (i == 2 && f.getValue().grade != 0)))) {
                                    MapleInventoryManipulator.addById(c, f.getValue().itemid, (short) 1, "Booster Pack");
                                    c.getSession().write(CField.getBoosterFamiliar(c.getPlayer().getId(), f.getKey(), 0));
                                    familiars[i] = f.getValue().itemid;
                                    break;
                                }
                            }
                        }
                        if (familiars[0] > 0 && familiars[1] > 0 && familiars[2] > 0) {
                            break;
                        }
                    }
                    c.getSession().write(MTSCSPacket.getBoosterPack(familiars[0], familiars[1], familiars[2]));
                    c.getSession().write(MTSCSPacket.getBoosterPackClick());
                    c.getSession().write(MTSCSPacket.getBoosterPackReveal());
                    break;
                }
                case 5050001: //一轉技能點數重配捲軸
                case 5050002: //二轉技能點數重配捲軸
                case 5050003: //三轉技能點數重配捲軸
                case 5050004: //4轉技能點數重配卷軸
                case 5050005: //龍魔導士1，2轉技能點數重配
                case 5050006: //龍魔導士3，4轉技能點數重配
                case 5050007: //龍魔導士5，6轉技能點數重配
                case 5050008: //龍魔導士7，8轉技能點數重配
                case 5050009: { //龍魔導士9，10轉技能點數重配
                    if (itemId >= 5050005 && !GameConstants.isEvan(c.getPlayer().getJob())) {
                        c.getPlayer().dropMessage(1, "This reset is only for Evans.");
                        break;
                    } //well i dont really care other than this o.o
                    if (itemId < 5050005 && GameConstants.isEvan(c.getPlayer().getJob())) {
                        c.getPlayer().dropMessage(1, "This reset is only for non-Evans.");
                        break;
                    } //well i dont really care other than this o.o
                    int skill1 = slea.readInt();
                    int skill2 = slea.readInt();
                    /*
                     * for (int i : GameConstants.blockedSkills) { if (skill1 == i)
                     * { c.getPlayer().dropMessage(1, "You may not add this
                     * skill."); return; } }
                     *
                     */

                    Skill skillSPTo = SkillFactory.getSkill(skill1);
                    Skill skillSPFrom = SkillFactory.getSkill(skill2);

                    if (skillSPTo.isBeginnerSkill() || skillSPFrom.isBeginnerSkill()) {
                        c.getPlayer().dropMessage(1, "You may not add beginner skills.");
                        break;
                    }
                    if (GameConstants.getSkillBookForSkill(skill1) != GameConstants.getSkillBookForSkill(skill2)) { //resistance evan
                        c.getPlayer().dropMessage(1, "You may not add different job skills.");
                        break;
                    }
                    //if (GameConstants.getJobNumber(skill1 / 10000) > GameConstants.getJobNumber(skill2 / 10000)) { //putting 3rd job skillpoints into 4th job for example
                    //    c.getPlayer().dropMessage(1, "You may not add skillpoints to a higher job.");
                    //    break;
                    //}
                    if ((c.getPlayer().getSkillLevel(skillSPTo) + 1 <= skillSPTo.getMaxLevel()) && c.getPlayer().getSkillLevel(skillSPFrom) > 0 && skillSPTo.canBeLearnedBy(c.getPlayer().getJob())) {
                        if (skillSPTo.isFourthJob() && (c.getPlayer().getSkillLevel(skillSPTo) + 1 > c.getPlayer().getMasterLevel(skillSPTo))) {
                            c.getPlayer().dropMessage(1, "You will exceed the master level.");
                            break;
                        }
                        if (itemId >= 5050005) {
                            if (GameConstants.getSkillBookForSkill(skill1) != (itemId - 5050005) * 2 && GameConstants.getSkillBookForSkill(skill1) != (itemId - 5050005) * 2 + 1) {
                                c.getPlayer().dropMessage(1, "You may not add this job SP using this reset.");
                                break;
                            }
                        } else {
                            int theJob = GameConstants.getJobNumber(skill2 / 10000);
                            switch (skill2 / 10000) {
                                case 430:
                                    theJob = 1;
                                    break;
                                case 432:
                                case 431:
                                    theJob = 2;
                                    break;
                                case 433:
                                    theJob = 3;
                                    break;
                                case 434:
                                    theJob = 4;
                                    break;
                            }
                            if (theJob != itemId - 5050000) { //you may only subtract from the skill if the ID matches Sp reset
                                c.getPlayer().dropMessage(1, "You may not subtract from this skill. Use the appropriate SP reset.");
                                break;
                            }
                        }
                        Map<Skill, SkillEntry> sa = new HashMap<>();
                        sa.put(skillSPFrom, new SkillEntry((byte) (c.getPlayer().getSkillLevel(skillSPFrom) - 1), c.getPlayer().getMasterLevel(skillSPFrom), SkillFactory.getDefaultSExpiry(skillSPFrom)));
                        sa.put(skillSPTo, new SkillEntry((byte) (c.getPlayer().getSkillLevel(skillSPTo) + 1), c.getPlayer().getMasterLevel(skillSPTo), SkillFactory.getDefaultSExpiry(skillSPTo)));
                        c.getPlayer().changeSkillsLevel(sa);
                        used = true;
                    }
                    break;
                }
                case 5500000: { //[1天]魔法沙漏
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    int days = 1;
                    if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                        boolean change = true;
                        for (String z : GameConstants.RESERVED) {
                            if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                                change = false;
                            }
                        }
                        if (change) {
                            item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                            c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                            used = true;
                        } else {
                            c.getPlayer().dropMessage(1, "It may not be used on this item.");
                        }
                    }
                    break;
                }
                case 5500001: { //[7天]魔法沙漏
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    int days = 7;
                    if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                        boolean change = true;
                        for (String z : GameConstants.RESERVED) {
                            if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                                change = false;
                            }
                        }
                        if (change) {
                            item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                            c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                            used = true;
                        } else {
                            c.getPlayer().dropMessage(1, "It may not be used on this item.");
                        }
                    }
                    break;
                }
                case 5500002: { //[20天]魔法沙漏
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    int days = 20;
                    if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                        boolean change = true;
                        for (String z : GameConstants.RESERVED) {
                            if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                                change = false;
                            }
                        }
                        if (change) {
                            item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                            c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                            used = true;
                        } else {
                            c.getPlayer().dropMessage(1, "It may not be used on this item.");
                        }
                    }
                    break;
                }
                case 5500005: { //[50天]魔法沙漏
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    int days = 50;
                    if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                        boolean change = true;
                        for (String z : GameConstants.RESERVED) {
                            if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                                change = false;
                            }
                        }
                        if (change) {
                            item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000L));//載入時間超過24天需要添加L
                            c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                            used = true;
                        } else {
                            c.getPlayer().dropMessage(1, "It may not be used on this item.");
                        }
                    }
                    break;
                }
                case 5500006: { //[99天]魔法沙漏
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    int days = 99;
                    if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                        boolean change = true;
                        for (String z : GameConstants.RESERVED) {
                            if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                                change = false;
                            }
                        }
                        if (change) {
                            item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000L));//載入時間超過24天需要添加L
                            c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                            used = true;
                        } else {
                            c.getPlayer().dropMessage(1, "It may not be used on this item.");
                        }
                    }
                    break;
                }
                case 5060000: { //刻名道具
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());

                    if (item != null && item.getOwner().equals("")) {
                        boolean change = true;
                        for (String z : GameConstants.RESERVED) {
                            if (c.getPlayer().getName().indexOf(z) != -1) {
                                change = false;
                            }
                        }
                        if (change) {
                            item.setOwner(c.getPlayer().getName());
                            c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                            used = true;
                        }
                    }
                    break;
                }
                case 5534000: { //商城潛能道具
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                    if (item != null) {
                        Equip eq = (Equip) item;
                        if (eq.getState() == 0) {
                            eq.resetPotential();
                            c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(false, c.getPlayer().getId(), true, itemId));
                            c.getSession().write(InventoryPacket.scrolledItem(toUse, item, false, true));
                            c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                            used = true;
                        } else {
                            c.getPlayer().dropMessage(5, "This item's Potential cannot be reset.");
                        }
                    } else {
                        c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(false, c.getPlayer().getId(), false, itemId));
                    }
                    break;
                }
                case 5062000: { //奇幻方塊
                    if (c.getPlayer().getLevel() < 50) {
                        c.getPlayer().dropMessage(1, "You may not use this until level 50.");
                    } else {
                        final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                        if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                            boolean potLock = c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5067000) != null;
                            int line = potLock && slea.available() > 0 ? slea.readInt() : 0;
                            int toLock = potLock && slea.available() > 0 ? slea.readUShort() : 0;
                            final Equip eq = (Equip) item;

                            if (eq.getState() >= 17 && eq.getState() != 20) {

                                eq.renewPotential(0);
                                c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(false, c.getPlayer().getId(), true, itemId));
                                c.getSession().write(InventoryPacket.scrolledItem(toUse, item, false, true));
                                c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                                MapleInventoryManipulator.addById(c, 2430112, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                                if (potLock) {
                                    eq.setLine(line);
                                    eq.setLockPot(toLock);
                                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5067000).getPosition(), (short) 1, false);
                                }
                                used = true;
                            } else {
                                c.getPlayer().dropMessage(5, "This item's Potential cannot be reset.");
                            }
                        } else {
                            c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(false, c.getPlayer().getId(), false, itemId));
                        }
                    }
                    break;
                }
                case 5062001: //超級奇幻方塊
                case 5062100: { //楓葉奇幻方塊(没有)
                    if (c.getPlayer().getLevel() < 70) {
                        c.getPlayer().dropMessage(1, "You may not use this until level 70.");
                    } else {
                        Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                        if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                            Equip eq = (Equip) item;
                            if (eq.getState() >= 17 && eq.getState() != 20) {
                                eq.renewPotential(1);
                                c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(false, c.getPlayer().getId(), true, itemId));
                                c.getSession().write(InventoryPacket.scrolledItem(toUse, item, false, true));
                                c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                                MapleInventoryManipulator.addById(c, 2430112, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                                used = true;
                            } else {
                                c.getPlayer().dropMessage(5, "This item's Potential cannot be reset.");
                            }
                        } else {
                            c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(false, c.getPlayer().getId(), false, itemId));
                        }
                    }
                    break;
                }
                case 5062002: //傳說方塊
                case 5062003:
                case 5062005: { //驚奇方塊
                    if (c.getPlayer().getLevel() < 100) {
                        c.getPlayer().dropMessage(1, "You may not use this until level 100.");
                    } else {
                        Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                        if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                            Equip eq = (Equip) item;
                            if (eq.getState() >= 17) {
                                eq.renewPotential(3);
                                c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(false, c.getPlayer().getId(), true, itemId));
                                c.getSession().write(InventoryPacket.scrolledItem(toUse, item, false, true));
                                c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                                MapleInventoryManipulator.addById(c, 2430481, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                                used = true;
                            } else {
                                c.getPlayer().dropMessage(5, "This item's Potential cannot be reset.");
                            }
                        } else {
                            c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(false, c.getPlayer().getId(), false, itemId));
                        }
                    }
                    break;
                }
                case 5750000: { //星岩方塊
                    if (c.getPlayer().getLevel() < 10) {
                        c.getPlayer().dropMessage(1, "You may not use this until level 10.");
                    } else {
                        Item item = c.getPlayer().getInventory(MapleInventoryType.SETUP).getItem((byte) slea.readInt());
                        if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() >= 1) {
                            int grade = GameConstants.getNebuliteGrade(item.getItemId());
                            if (grade != -1 && grade < 4) {
                                int rank = Randomizer.nextInt(100) < 7 ? (Randomizer.nextInt(100) < 2 ? (grade + 1) : (grade != 3 ? (grade + 1) : grade)) : grade;
                                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                                List<StructItemOption> pots = new LinkedList<>(ii.getAllSocketInfo(rank).values());
                                int newId = 0;
                                while (newId == 0) {
                                    StructItemOption pot = pots.get(Randomizer.nextInt(pots.size()));
                                    if (pot != null) {
                                        newId = pot.opID;
                                    }
                                }
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, item.getPosition(), (short) 1, false);
                                MapleInventoryManipulator.addById(c, newId, (short) 1, "Upgraded from alien cube on " + FileoutputUtil.CurrentReadable_Date());
                                MapleInventoryManipulator.addById(c, 2430691, (short) 1, "Alien Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                                used = true;
                            } else {
                                c.getPlayer().dropMessage(1, "Grade S Nebulite cannot be added.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "You do not have sufficient inventory slot.");
                        }
                    }
                    break;
                }
                case 5750001: { //星岩電轉機
                    if (c.getPlayer().getLevel() < 10) {
                        c.getPlayer().dropMessage(1, "You may not use this until level 10.");
                    } else {
                        Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                        if (item != null) {
                            Equip eq = (Equip) item;
                            if (eq.getSocket1() > 0 || eq.getSocket3() > 0 || eq.getSocket3() > 0) { // first slot only.
                                eq.setSocket1(0);
                                //     eq.setSocket2(0);
                                //   eq.setSocket3(0);
                                c.getSession().write(InventoryPacket.scrolledItem(toUse, item, false, true));
                                c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                                used = true;
                            } else {
                                c.getPlayer().dropMessage(5, "This item do not have 3 sockets");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "This item's nebulite cannot be removed.");
                        }
                    }
                    break;
                }
                case 5521000: { //與自己分享名牌
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());

                    if (item != null && !ItemFlag.KARMA_ACC.check(item.getFlag()) && !ItemFlag.KARMA_ACC_USE.check(item.getFlag())) {
                        if (MapleItemInformationProvider.getInstance().isShareTagEnabled(item.getItemId())) {
                            short flag = item.getFlag();
                            if (ItemFlag.UNTRADEABLE.check(flag)) {
                                flag -= ItemFlag.UNTRADEABLE.getValue();
                            } else if (type == MapleInventoryType.EQUIP) {
                                flag |= ItemFlag.KARMA_ACC.getValue();
                            } else {
                                flag |= ItemFlag.KARMA_ACC_USE.getValue();
                            }
                            item.setFlag(flag);
                            c.getPlayer().forceReAddItem_NoUpdate(item, type);
                            c.getSession().write(InventoryPacket.updateSpecialItemUse(item, type.getType(), item.getPosition(), true, c.getPlayer()));
                            used = true;
                        }
                    }
                    break;
                }
                case 5520000: //神奇剪刀
                case 5520001: { //白金神奇剪刀
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());

                    if (item != null && !ItemFlag.KARMA_EQ.check(item.getFlag()) && !ItemFlag.KARMA_USE.check(item.getFlag())) {
                        if ((itemId == 5520000 && MapleItemInformationProvider.getInstance().isKarmaEnabled(item.getItemId())) || (itemId == 5520001 && MapleItemInformationProvider.getInstance().isPKarmaEnabled(item.getItemId()))) {
                            short flag = item.getFlag();
                            if (ItemFlag.UNTRADEABLE.check(flag)) {
                                flag -= ItemFlag.UNTRADEABLE.getValue();
                            } else if (type == MapleInventoryType.EQUIP) {
                                flag |= ItemFlag.KARMA_EQ.getValue();
                            } else {
                                flag |= ItemFlag.KARMA_USE.getValue();
                            }
                            item.setFlag(flag);
                            c.getPlayer().forceReAddItem_NoUpdate(item, type);
                            c.getSession().write(InventoryPacket.updateSpecialItemUse(item, type.getType(), item.getPosition(), true, c.getPlayer()));
                            used = true;
                        }
                    }
                    break;
                }
                case 5570000: { //黃金鐵鎚
                    slea.readInt(); // Inventory type, Hammered eq is always EQ.
                    Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                    // another int here, D3 49 DC 00
                    if (item != null) {
                        if (GameConstants.canHammer(item.getItemId()) && MapleItemInformationProvider.getInstance().getSlots(item.getItemId()) > 0 && item.getViciousHammer() < 2) {
                            item.setViciousHammer((byte) (item.getViciousHammer() + 1));
                            item.setUpgradeSlots((byte) (item.getUpgradeSlots() + 1));
                            c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIP);
                            c.getSession().write(MTSCSPacket.ViciousHammer(true, (byte) item.getViciousHammer()));
                            used = true;
                        } else {
                            c.getPlayer().dropMessage(5, "You may not use it on this item.");
                            c.getSession().write(MTSCSPacket.ViciousHammer(true, (byte) 0));
                        }
                    }
                    break;
                }
                case 5610000: //卷軸成功提升卡(10%卷軸專用)
                case 5610001: { //卷軸成功提升卡(60%卷軸專用)
                    slea.readInt(); // Inventory type, always eq
                    final byte dst = (byte) slea.readInt();
                    slea.readInt(); // Inventory type, always use
                    final byte src = (byte) slea.readInt();
                    used = UseUpgradeScroll(src, dst, (byte) 1, c, c.getPlayer(), itemId, false, true);
                    c.getSession().write(MTSCSPacket.VegasScroll(73));//67 is working ON
                    Item scrollItem = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(src);
                    if (scrollItem != null) {
                        scrollItem.setQuantity((short) (scrollItem.getQuantity()));
                        c.getSession().write(InventoryPacket.updateInventorySlot(MapleInventoryType.USE, scrollItem, false));
                    }
                    MapTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
                            c.getSession().write(InventoryPacket.updateInventorySlot(MapleInventoryType.EQUIP, item, false));
                            c.getSession().write(InventoryPacket.scrolledItem(item, item, false, false));
                            c.getSession().write(MTSCSPacket.VegasScroll(76));
                            //c.enableActions();

                        }
                    }, 2000);
                    used = true;
                    break;
                }
                case 5060001: { //封印之鎖
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                    // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                    if (item != null && item.getExpiration() == -1) {
                        short flag = item.getFlag();
                        flag |= ItemFlag.LOCK.getValue();
                        item.setFlag(flag);

                        c.getPlayer().forceReAddItem_Flag(item, type);
                        used = true;
                    }
                    break;
                }
                case 5061000: { //封印之鎖 : 7日
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                    // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                    if (item != null && item.getExpiration() == -1) {
                        short flag = item.getFlag();
                        flag |= ItemFlag.LOCK.getValue();
                        item.setFlag(flag);
                        item.setExpiration(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));

                        c.getPlayer().forceReAddItem_Flag(item, type);
                        used = true;
                    }
                    break;
                }
                case 5061001: { //封印之鎖 : 30日
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                    // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                    if (item != null && item.getExpiration() == -1) {
                        short flag = item.getFlag();
                        flag |= ItemFlag.LOCK.getValue();
                        item.setFlag(flag);

                        item.setExpiration(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)); //載入時間超過24天需要添加L

                        c.getPlayer().forceReAddItem_Flag(item, type);
                        used = true;
                    }
                    break;
                }
                case 5061002: { //封印之鎖 : 90日
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                    // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                    if (item != null && item.getExpiration() == -1) {
                        short flag = item.getFlag();
                        flag |= ItemFlag.LOCK.getValue();
                        item.setFlag(flag);

                        item.setExpiration(System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)); //載入時間超過24天需要添加L

                        c.getPlayer().forceReAddItem_Flag(item, type);
                        used = true;
                    }
                    break;
                }
                case 5061003: { //封印之鎖 : 365日
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                    // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                    if (item != null && item.getExpiration() == -1) {
                        short flag = item.getFlag();
                        flag |= ItemFlag.LOCK.getValue();
                        item.setFlag(flag);

                        item.setExpiration(System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)); //載入時間超過24天需要添加L

                        c.getPlayer().forceReAddItem_Flag(item, type);
                        used = true;
                    }
                    break;
                }
                case 5063000: //幸運鑰匙
                case 5063100: {//幸運保護券
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) slea.readShort());
                    if (item != null && item.getType() == 1) { //equip
                        short flag = item.getFlag();
                        flag |= ItemFlag.LUCKS_KEY.getValue();
                        item.setFlag(flag);

                        c.announce(CWvsContext.InventoryPacket.scrolledItem(toUse, item, false, false));
                        used = true;
                    }
                    break;
                }
                case 5064000: //裝備保護卷軸
                case 5064002: { //星光裝備保護卷軸
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) slea.readShort());
                    if (item != null && item.getType() == 1) { //equip
                        if (((Equip) item).getEnhance() >= 12) {
                            break; //cannot be used
                        }
                        short flag = item.getFlag();
                        flag |= ItemFlag.SHIELD_WARD.getValue();
                        item.setFlag(flag);

                        c.announce(CWvsContext.InventoryPacket.scrolledItem(toUse, item, false, false));
                        used = true;
                    }
                    break;
                }
                case 5064100: //安全盾牌卷軸
                case 5064101: { //星光安全盾牌卷軸
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) slea.readShort());
                    if (item != null && item.getType() == 1) { //equip
                        short flag = item.getFlag();
                        flag |= ItemFlag.SLOTS_PROTECT.getValue();
                        item.setFlag(flag);

                        c.announce(CWvsContext.InventoryPacket.scrolledItem(toUse, item, false, false));
                        used = true;
                    }
                    break;
                }
                case 5064200: { // 完美回真卡
                    slea.readInt();
                    final byte dst = (byte) slea.readShort();
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                    slea.readByte();
                    short src = (short) c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5064200);
                    if (item != null && item.getType() == 1) {
                        used = UseUpgradeScroll(src, dst, (byte) 0, c, c.getPlayer(), false, true);
                    }
                    used = true;
                    break;
                }
                case 5064201: { //星光回真卷軸
                    slea.readInt();
                    final byte dst = (byte) slea.readShort();
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                    slea.readByte();
                    short src = (short) c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5064201);
                    if (item != null && item.getType() == 1) {
                        used = UseUpgradeScroll(src, dst, (byte) 0, c, c.getPlayer(), false, true);
                    }
                    used = true;
                    break;
                }
                case 5064300: //卷軸保護卡
                case 5064301: { //星光卷軸保護卡
                    Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) slea.readShort());
                    if (item != null && item.getType() == 1) { //equip
                        short flag = item.getFlag();
                        flag |= ItemFlag.SCROLL_PROTECT.getValue();
                        item.setFlag(flag);

                        c.announce(CWvsContext.InventoryPacket.scrolledItem(toUse, item, false, false));
                        used = true;
                    }
                    break;
                }
                case 5152100: //日拋隱形眼鏡(黑色)
                case 5152101: //日拋隱形眼鏡(藍色)
                case 5152102: //日拋隱形眼鏡(紅色)
                case 5152103: //日拋隱形眼鏡(綠色)
                case 5152104: //日拋隱形眼鏡(褐色)
                case 5152105: //日拋隱形眼鏡(藍綠色)
                case 5152106: //日拋隱形眼鏡(紫色)
                case 5152107: //日拋隱形眼鏡(亮紫色)
                case 5152108: {//日拋隱形眼鏡(白色)
                    int teye = (itemId - 5152100) * 100;
                    if (teye >= 0) {
                        c.getPlayer().setFace(c.getPlayer().getGender() == 0 ? teye + 20000 : teye + 21000);
                        c.getPlayer().updateSingleStat(MapleStat.FACE, c.getPlayer().getFace());
                        c.getPlayer().equipChanged();
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "Error occurred while using contact lenses.");
                    }
                    break;
                }
                case 5071000: { //紅喇叭
                    if (c.getPlayer().getLevel() < 10) {
                        c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                        break;
                    }
                    if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                        c.getPlayer().dropMessage(5, "Cannot be used here.");
                        break;
                    }
                    if (!c.getChannelServer().getMegaphoneMuteState()) {
                        String message = slea.readMapleAsciiString();

                        if (message.length() > 65) {
                            break;
                        }
                        StringBuilder sb = new StringBuilder();
                        addMedalString(c.getPlayer(), sb);
                        sb.append(c.getPlayer().getName());
                        sb.append(" : ");
                        sb.append(message);

                        c.getChannelServer().broadcastSmegaPacket(CWvsContext.serverNotice(2, sb.toString()));
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                    }
                    break;
                }
                case 5077000: { //三行喇叭
                    if (c.getPlayer().getLevel() < 10) {
                        c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                        break;
                    }
                    if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                        c.getPlayer().dropMessage(5, "Cannot be used here.");
                        break;
                    }
                    if (!c.getChannelServer().getMegaphoneMuteState()) {
                        byte numLines = slea.readByte();
                        if (numLines > 3) {
                            return;
                        }
                        List<String> messages = new LinkedList<>();
                        String message;

                        for (int i = 0; i < numLines; i++) {
                            message = slea.readMapleAsciiString();

                            if (message.length() > 65) {
                                break;
                            }
                            messages.add(c.getPlayer().getName() + " : " + message);
                        }
                        boolean ear = slea.readByte() > 0;

                        World.Broadcast.broadcastSmega(CWvsContext.tripleSmega(messages, ear, c.getChannel()));
                        used = true;

                    } else {
                        c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                    }
                    break;
                }
                case 5079004: { // Heart Megaphone
                    if (c.getPlayer().getLevel() < 10) {
                        c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                        break;
                    }
                    if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                        c.getPlayer().dropMessage(5, "Cannot be used here.");
                        break;
                    }
                    if (!c.getChannelServer().getMegaphoneMuteState()) {
                        String message = slea.readMapleAsciiString();

                        if (message.length() > 65) {
                            break;
                        }
                        World.Broadcast.broadcastSmega(CWvsContext.echoMegaphone(c.getPlayer().getName(), message));
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                    }
                    break;
                }
                case 5074000: { //骷髏喇叭
                    if (c.getPlayer().getLevel() < 10) {
                        c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                        break;
                    }
                    if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                        c.getPlayer().dropMessage(5, "Cannot be used here.");
                        break;
                    }
                    if (!c.getChannelServer().getMegaphoneMuteState()) {
                        String message = slea.readMapleAsciiString();

                        if (message.length() > 65) {
                            break;
                        }
                        StringBuilder sb = new StringBuilder();
                        addMedalString(c.getPlayer(), sb);
                        sb.append(c.getPlayer().getName());
                        sb.append(" : ");
                        sb.append(message);

                        boolean ear = slea.readByte() != 0;

                        World.Broadcast.broadcastSmega(CWvsContext.serverNotice(22, c.getChannel(), sb.toString(), ear));
                        used = true;

                    } else {
                        c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                    }
                    break;
                }
                case 5072000: { //高效能喇叭
                    if (c.getPlayer().getLevel() < 10) {
                        c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                        break;
                    }
                    if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                        c.getPlayer().dropMessage(5, "Cannot be used here.");
                        break;
                    }
                    if (!c.getChannelServer().getMegaphoneMuteState()) {
                        String message = slea.readMapleAsciiString();

                        if (message.length() > 65) {
                            break;
                        }
                        StringBuilder sb = new StringBuilder();
                        addMedalString(c.getPlayer(), sb);
                        sb.append(c.getPlayer().getName());
                        sb.append(" : ");
                        sb.append(message);

                        boolean ear = slea.readByte() != 0;

                        World.Broadcast.broadcastSmega(CWvsContext.serverNotice(3, c.getChannel(), sb.toString(), ear));
                        used = true;

                    } else {
                        c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                    }
                    break;
                }
                case 5076000: { //道具喇叭
                    if (c.getPlayer().getLevel() < 10) {
                        c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                        break;
                    }
                    if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                        c.getPlayer().dropMessage(5, "Cannot be used here.");
                        break;
                    }
                    if (!c.getChannelServer().getMegaphoneMuteState()) {
                        String message = slea.readMapleAsciiString();

                        if (message.length() > 65) {
                            break;
                        }
                        StringBuilder sb = new StringBuilder();
                        addMedalString(c.getPlayer(), sb);
                        sb.append(c.getPlayer().getName());
                        sb.append(" : ");
                        sb.append(message);

                        boolean ear = slea.readByte() > 0;

                        Item item = null;
                        if (slea.readByte() == 1) { //item
                            byte invType = (byte) slea.readInt();
                            byte pos = (byte) slea.readInt();
                            if (pos <= 0) {
                                invType = -1;
                            }
                            item = c.getPlayer().getInventory(MapleInventoryType.getByType(invType)).getItem(pos);
                        }
                        World.Broadcast.broadcastSmega(CWvsContext.itemMegaphone(sb.toString(), ear, c.getChannel(), item));
                        used = true;

                    } else {
                        c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                    }
                    break;
                }
                case 5075000: //楓之谷TV消息
                case 5075001: //閃星楓之谷TV消息
                case 5075002: //愛心楓之谷TV消息
                case 5075003: //廣播消息
                case 5075004: //閃星廣播消息
                case 5075005: { //愛心廣播消息
                    final int tvType = itemId % 10;
                    boolean megassenger = false;
                    boolean ear = false;
                    MapleCharacter victim = null;
                    if (tvType != 1) {
                        if (tvType >= 3) {
                            megassenger = true;
                            if (tvType == 3) {
                                slea.readByte();
                            }
                            ear = (1 == slea.readByte());
                        } else if (tvType != 2) {
                            slea.readByte();
                        }
                        if (tvType != 4) {
                            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
                        }
                    }
                    final List<String> tvMessages = (List<String>) new LinkedList();
                    final StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < 5; i++) {
                        final String message = slea.readMapleAsciiString();
                        if (megassenger) {
                            builder.append(" ").append(message);
                        }
                        tvMessages.add(message);
                    }
                    if (megassenger) {
                        World.Broadcast.broadcastSmega(CWvsContext.serverNotice(3, c.getChannel(), new StringBuilder().append(c.getPlayer().getName()).append(" : ").append(builder.toString()).toString(), ear));
                    }
                    if (!MapleTVEffect.isActive()) {
                        final MapleTVEffect mapleTVEffect = new MapleTVEffect(c.getPlayer(), victim, tvMessages, tvType);
                        mapleTVEffect.stratMapleTV();
                        used = true;
                        break;
                    }
                    c.getPlayer().dropMessage(1, "MapleTV is currently in use");
                    break;
                }
                case 5390000: //炎熱喇叭
                case 5390001: //愛莉絲喇叭
                case 5390002: //愛心朵朵喇叭
                case 5390005: //小帥虎喇叭
                case 5390006: //怒吼老虎喇叭
                case 5390007: //達陣喇叭
                case 5390008: //我是冠軍喇叭
                case 5390009: {
                    if (c.getPlayer().getLevel() < 10) {
                        c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                        break;
                    }
                    if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                        c.getPlayer().dropMessage(5, "Cannot be used here.");
                        break;
                    }
                    if (!c.getChannelServer().getMegaphoneMuteState()) {
                        List<String> lines = new LinkedList<>();
                        for (int i = 0; i < 4; i++) {
                            String text;
                            if (itemId == 5390009) {
                                text = "Looking for friends!! Add me <3333";
                            } else {
                                text = slea.readMapleAsciiString();
                            }
                            if (text.length() > 55) {
                                continue;
                            }
                            lines.add(text);
                        }
                        boolean ear = slea.readByte() != 0;
                        World.Broadcast.broadcastSmega(CWvsContext.getAvatarMega(c.getPlayer(), c.getChannel(), itemId, lines, ear));
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                    }
                    break;
                }
                case 5090000: { //訊息
                    String sendTo = slea.readMapleAsciiString();
                    String msg = slea.readMapleAsciiString();
                    if (MapleCharacterUtil.canCreateChar(sendTo, false)) { // Name does not exist
                        c.getSession().write(MTSCSPacket.OnMemoResult((byte) 5, (byte) 1));
                    } else {
                        int ch = World.Find.findChannel(sendTo);
                        if (ch <= 0) { // offline
                            c.getPlayer().sendNote(sendTo, msg);
                            c.getSession().write(MTSCSPacket.OnMemoResult((byte) 4, (byte) 0));
                            used = true;
                        } else {
                            c.getSession().write(MTSCSPacket.OnMemoResult((byte) 5, (byte) 0));
                        }
                    }
                    break;
                }
                case 5100000: { //賀曲
                    c.getPlayer().getMap().broadcastMessage(CField.musicChange("Jukebox/Congratulation"));
                    used = true;
                    break;
                }
                case 5190001: //自動服用HP藥水技能
                case 5190002:
                case 5190003:
                case 5190004:
                case 5190005:
                case 5190006:
                case 5190007:
                case 5190008:
                case 5190000: { // Pet Flags
                    int uniqueid = (int) slea.readLong();
                    MaplePet pet = c.getPlayer().getPet(0);
                    int slo = 0;

                    if (pet == null) {
                        break;
                    }
                    if (pet.getUniqueId() != uniqueid) {
                        pet = c.getPlayer().getPet(1);
                        slo = 1;
                        if (pet != null) {
                            if (pet.getUniqueId() != uniqueid) {
                                pet = c.getPlayer().getPet(2);
                                slo = 2;
                                if (pet != null) {
                                    if (pet.getUniqueId() != uniqueid) {
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                        } else {
                            break;
                        }
                    }
                    PetFlag zz = PetFlag.getByAddId(itemId);
                    if (zz != null && !zz.check(pet.getFlags())) {
                        pet.setFlags(pet.getFlags() | zz.getValue());
                        c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                        c.getSession().write(CWvsContext.enableActions());
                        c.getSession().write(MTSCSPacket.changePetFlag(uniqueid, true, zz.getValue()));
                        used = true;
                    }
                    break;
                }
                case 5191001: //取消自動服用藥水功能
                case 5191002:
                case 5191003:
                case 5191004:
                case 5191000: { // Pet Flags
                    int uniqueid = (int) slea.readLong();
                    MaplePet pet = c.getPlayer().getPet(0);
                    int slo = 0;

                    if (pet == null) {
                        break;
                    }
                    if (pet.getUniqueId() != uniqueid) {
                        pet = c.getPlayer().getPet(1);
                        slo = 1;
                        if (pet != null) {
                            if (pet.getUniqueId() != uniqueid) {
                                pet = c.getPlayer().getPet(2);
                                slo = 2;
                                if (pet != null) {
                                    if (pet.getUniqueId() != uniqueid) {
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                        } else {
                            break;
                        }
                    }
                    PetFlag zz = PetFlag.getByDelId(itemId);
                    if (zz != null && zz.check(pet.getFlags())) {
                        pet.setFlags(pet.getFlags() - zz.getValue());
                        c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                        c.getSession().write(CWvsContext.enableActions());
                        c.getSession().write(MTSCSPacket.changePetFlag(uniqueid, false, zz.getValue()));
                        used = true;
                    }
                    break;
                }
                case 5501001: //[30天]騎乘延長線圈
                case 5501002: { //[60天]騎乘延長線圈
                    Skill skil = SkillFactory.getSkill(slea.readInt());
                    if (skil == null || skil.getId() / 10000 != 8000 || c.getPlayer().getSkillLevel(skil) <= 0 || !skil.isTimeLimited() || GameConstants.getMountItem(skil.getId(), c.getPlayer()) <= 0) {
                        break;
                    }
                    long toAdd = (itemId == 5501001 ? 30 : 60) * 24 * 60 * 60 * 1000L;
                    long expire = c.getPlayer().getSkillExpiry(skil);
                    if (expire < System.currentTimeMillis() || (long) (expire + toAdd) >= System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L)) {
                        break;
                    }
                    c.getPlayer().changeSingleSkillLevel(skil, c.getPlayer().getSkillLevel(skil), c.getPlayer().getMasterLevel(skil), (long) (expire + toAdd));
                    used = true;
                    break;
                }
                case 5170000: { //取寵物名

                    MaplePet pet = c.getPlayer().getPet(0);
                    int slo = 0;

                    if (pet == null) {
                        break;
                    }

                    String nName = slea.readMapleAsciiString();

                    pet.setName(nName);
                    pet.saveToDb();
                    c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                    c.getSession().write(CWvsContext.enableActions());
                    c.getPlayer().getMap().broadcastMessage(MTSCSPacket.changePetName(c.getPlayer(), nName, slo));
                    used = true;

                    break;
                }
                case 5700000: { //機器人取名券
                    slea.skip(8);
                    if (c.getPlayer().getAndroid() == null) {
                        break;
                    }
                    String nName = slea.readMapleAsciiString();
                    c.getPlayer().getAndroid().setName(nName);
                    c.getPlayer().getAndroid().saveToDb();
                    c.getPlayer().setAndroid(c.getPlayer().getAndroid()); //respawn it
                    used = true;
                    break;
                }
                case 5155000: { //卡勒塔的珍珠
                    c.getPlayer().changeElf();
                    used = true;
                    break;
                }
                case 5240000: //新鮮香蕉
                case 5240001: //電池
                case 5240002:
                case 5240003:
                case 5240004:
                case 5240005:
                case 5240006:
                case 5240007:
                case 5240008:
                case 5240009:
                case 5240010:
                case 5240011:
                case 5240012:
                case 5240013:
                case 5240014:
                case 5240015:
                case 5240016:
                case 5240017:
                case 5240018:
                case 5240019:
                case 5240020:
                case 5240021:
                case 5240022:
                case 5240023:
                case 5240024:
                case 5240025:
                case 5240026:
                case 5240027:
                case 5240028:
                case 5240029:
                case 5240030:
                case 5240031:
                case 5240032:
                case 5240033:
                case 5240034:
                case 5240035:
                case 5240036:
                case 5240037:
                case 5240038:
                case 5240039:
                case 5240040:
                case 5240041:
                case 5240042:
                case 5240043:
                case 5240044:
                case 5240045:
                case 5240047:
                case 5240048:
                case 5240050:
                case 5240051:
                case 5240052:
                case 5240053:
                case 5240054:
                case 5240055:
                case 5240056:
                case 5240066: { // Pet food
                    MaplePet pet = c.getPlayer().getPet(0);

                    if (pet == null) {
                        break;
                    }
                    if (!pet.canConsume(itemId)) {
                        pet = c.getPlayer().getPet(1);
                        if (pet != null) {
                            if (!pet.canConsume(itemId)) {
                                pet = c.getPlayer().getPet(2);
                                if (pet != null) {
                                    if (!pet.canConsume(itemId)) {
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                        } else {
                            break;
                        }
                    }
                    byte petindex = c.getPlayer().getPetIndex(pet);
                    pet.setFullness(100);
                    if (pet.getCloseness() < 30000) {
                        if (pet.getCloseness() + (100 * c.getChannelServer().getTraitRate()) > 30000) {
                            pet.setCloseness(30000);
                        } else {
                            pet.setCloseness(pet.getCloseness() + (100 * c.getChannelServer().getTraitRate()));
                        }
                        if (pet.getCloseness() >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                            pet.setLevel(pet.getLevel() + 1);
                            c.getSession().write(EffectPacket.showOwnPetLevelUp(c.getPlayer().getPetIndex(pet)));
                            c.getPlayer().getMap().broadcastMessage(PetPacket.showPetLevelUp(c.getPlayer(), petindex));
                        }
                    }
                    c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition()), true));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PetPacket.commandResponse(c.getPlayer().getId(), (byte) 1, petindex, true, true), true);
                    used = true;
                    break;
                }
                case 5230000: //智慧貓頭鷹
                case 5230001: {//新手的智慧貓頭鷹
                    int itemSearch = slea.readInt();
                    List<HiredMerchant> hms = c.getChannelServer().searchMerchant(itemSearch);
                    if (hms.size() > 0) {
                        c.getSession().write(CWvsContext.getOwlSearched(itemSearch, hms));
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "Unable to find the item.");
                    }
                    break;
                }
                case 5281000: //毒屁
                case 5281001: { //花香
                    Rectangle bounds = new Rectangle((int) c.getPlayer().getPosition().getX(), (int) c.getPlayer().getPosition().getY(), 1, 1);
                    MapleMist mist = new MapleMist(bounds, c.getPlayer());
                    c.getPlayer().getMap().spawnMist(mist, 10000, true);
                    c.getSession().write(CWvsContext.enableActions());
                    used = true;
                    break;
                }
                case 5370000: //黑板 7天
                case 5370001: //黑板 1天
                case 5370002: { // 黑板：3日券
                    for (MapleEventType t : MapleEventType.values()) {
                        MapleEvent e = ChannelServer.getInstance(c.getChannel()).getEvent(t);
                        if (e.isRunning()) {
                            for (int i : e.getType().mapids) {
                                if (c.getPlayer().getMapId() == i) {
                                    c.getPlayer().dropMessage(5, "You may not use that here.");
                                    c.getSession().write(CWvsContext.enableActions());
                                    return;
                                }
                            }
                        }
                    }
                    String message = slea.readMapleAsciiString();

                    c.getPlayer().setChalkboard(message);
                    break;
                }
                case 5450000: //地攤商人妙妙
                case 5450003: { // Mu Mu the Travelling Merchant
                    if (c.getPlayer().getLevel() < 10) {
                        c.getPlayer().dropMessage(5, "You must be over level 10 to use this command.");
                    } else if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000) {
                        c.getPlayer().dropMessage(5, "You may not use this command here.");
                    } else if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
                        c.getPlayer().dropMessage(5, "You may not use this command here.");
                    } else {
                        MapleShopFactory.getInstance().getShop(9090000).sendShop(c);
                    }
                    used = true;
                    break;
                }
                case 5300000: //菇菇寶貝的雕像
                case 5300001: //緞帶肥肥的雕像
                case 5300002: { //葛雷的雕像
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    ii.getItemEffect(itemId).applyTo(c.getPlayer());
                    used = true;
                    break;
                }
                default:
                    if (itemId / 10000 == 512) {
                        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        String msg = ii.getMsg(itemId);
                        String ourMsg = slea.readMapleAsciiString();
                        if (!msg.contains("%s")) {
                            msg = ourMsg;
                        } else {
                            msg = msg.replaceFirst("%s", c.getPlayer().getName());
                            if (!msg.contains("%s")) {
                                msg = ii.getMsg(itemId).replaceFirst("%s", ourMsg);
                            } else {
                                try {
                                    msg = msg.replaceFirst("%s", ourMsg);
                                } catch (Exception e) {
                                    msg = ii.getMsg(itemId).replaceFirst("%s", ourMsg);
                                }
                            }
                        }
                        c.getPlayer().getMap().startMapEffect(msg, itemId);

                        int buff = ii.getStateChangeItem(itemId);
                        if (buff != 0) {
                            for (MapleCharacter mChar : c.getPlayer().getMap().getCharactersThreadsafe()) {
                                ii.getItemEffect(buff).applyTo(mChar);
                            }
                        }
                        used = true;
                    } else if (itemId / 10000 == 510) {
                        c.getPlayer().getMap().startJukebox(c.getPlayer().getName(), itemId);
                        used = true;
                    } else if (itemId / 10000 == 520) {
                        int mesars = MapleItemInformationProvider.getInstance().getMeso(itemId);
                        if (mesars > 0 && c.getPlayer().getMeso() < (Integer.MAX_VALUE - mesars)) {
                            used = true;
                            if (Math.random() > 0.1) {
                                int gainmes = Randomizer.nextInt(mesars);
                                c.getPlayer().gainMeso(gainmes, false);
                                c.getSession().write(MTSCSPacket.sendMesobagSuccess(gainmes));
                            } else {
                                c.getSession().write(MTSCSPacket.sendMesobagFailed(false)); // not random
                            }
                        }
                    } else if (itemId / 10000 == 562) {
                        if (UseSkillBook(slot, itemId, c, c.getPlayer())) {
                            c.getPlayer().gainSP(1);
                        } //this should handle removing
                    } else if (itemId / 10000 == 553) {
                        UseRewardItem(slot, itemId, c, c.getPlayer());// this too*/
                    } else if (itemId / 10000 != 519) {
                        System.out.println("Unhandled CS item : " + itemId);
                        System.out.println(slea.toString(true));
                    }
                    break;
            }

            if (used) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false, true);
            }
            c.getSession().write(CWvsContext.enableActions());
            if (cc) {
                if (!c.getPlayer().isAlive() || c.getPlayer().getEventInstance() != null || FieldLimitType.ChannelSwitch.check(c.getPlayer().getMap().getFieldLimit())) {
                    c.getPlayer().dropMessage(1, "Auto relog failed.");
                    return;
                }
                c.getPlayer().dropMessage(5, "Auto relogging. Please wait.");
                c.getPlayer().fakeRelog();
                if (c.getPlayer().getScrolledPosition() != 0) {
                    c.getSession().write(CWvsContext.pamSongUI());
                }
            }
        } else {
            c.getPlayer().dropMessage(5, "Don't spam.");
            c.getSession().write(CWvsContext.enableActions());
        }
    }

    public static void Pickup_Player(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        slea.skip(1); // or is this before tick?
        Point Client_Reportedpos = slea.readPos();
        if (chr == null || chr.getMap() == null) {
            return;
        }
        MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        MapleMapItem mapitem = (MapleMapItem) ob;
        Lock lock = mapitem.getLock();
        lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (mapitem.getQuest() > 0 && chr.getQuestStatus(mapitem.getQuest()) != 1) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId() && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (mapitem.getMeso() > 0) {
                if (chr.getParty() != null && mapitem.getOwner() != chr.getId()) {
                    List<MapleCharacter> toGive = new LinkedList<>();
                    int splitMeso = mapitem.getMeso() * 40 / 100;
                    for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                        MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                        if (m != null && m.getId() != chr.getId()) {
                            toGive.add(m);
                        }
                    }
                    for (MapleCharacter m : toGive) {
                        int mesos = splitMeso / toGive.size() + (m.getStat().hasPartyBonus ? (int) (mapitem.getMeso() / 20.0) : 0);
                        if (mapitem.getDropper() instanceof MapleMonster && m.getStat().incMesoProp > 0) {
                            mesos += Math.floor((m.getStat().incMesoProp * mesos) / 100.0f);
                        }
                        m.gainMeso(mesos, true);
                    }
                    int mesos = mapitem.getMeso() - splitMeso;
                    if (mapitem.getDropper() instanceof MapleMonster && chr.getStat().incMesoProp > 0) {
                        mesos += Math.floor((chr.getStat().incMesoProp * mesos) / 100.0f);
                    }
                    chr.gainMeso(mesos, true);
                } else {
                    int mesos = mapitem.getMeso();
                    if (mapitem.getDropper() instanceof MapleMonster && chr.getStat().incMesoProp > 0) {
                        mesos += Math.floor((chr.getStat().incMesoProp * mesos) / 100.0f);
                    }
                    chr.gainMeso(mesos, true);
                }
                removeItem(chr, mapitem, ob);
            } else {
                /*
                 * if
                 * (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId()))
                 * { c.getSession().write(CWvsContext.enableActions());
                 * c.getPlayer().dropMessage(5, "This item cannot be picked
                 * up."); } else
                 */
                if (c.getPlayer().inPVP() && Integer.parseInt(c.getPlayer().getEventInstance().getProperty("ice")) == c.getPlayer().getId()) {
                    c.getSession().write(InventoryPacket.getInventoryFull());
                    c.getSession().write(InventoryPacket.getShowInventoryFull());
                    c.getSession().write(CWvsContext.enableActions());
                } else if (useItem(c, mapitem.getItemId())) {
                    removeItem(c.getPlayer(), mapitem, ob);
                    //another hack
                    if (mapitem.getItemId() / 10000 == 291) {
                        c.getPlayer().getMap().broadcastMessage(CField.getCapturePosition(c.getPlayer().getMap()));
                        c.getPlayer().getMap().broadcastMessage(CField.resetCapture());
                    }
                } else if (mapitem.getItemId() / 10000 != 291 && MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                    MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster);
                    removeItem(chr, mapitem, ob);
                } else {
                    c.getSession().write(InventoryPacket.getInventoryFull());
                    c.getSession().write(InventoryPacket.getShowInventoryFull());
                    c.getSession().write(CWvsContext.enableActions());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static void Pickup_Pet(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        if (c.getPlayer().hasBlockedInventory() || c.getPlayer().inPVP()) { //hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        byte petz = (byte) (GameConstants.GMS ? (c.getPlayer().getPetIndex((int) slea.readLong())) : slea.readInt());
        MaplePet pet = chr.getPet(petz);
        slea.skip(1); // [4] Zero, [4] Seems to be tickcount, [1] Always zero
        slea.readInt();
        Point Client_Reportedpos = slea.readPos();
        MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null || pet == null) {
            return;
        }
        MapleMapItem mapitem = (MapleMapItem) ob;
        Lock lock = mapitem.getLock();
        lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.getSession().write(InventoryPacket.getInventoryFull());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && mapitem.isPlayerDrop()) {
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId() && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (mapitem.getMeso() > 0) {
                /*
                 * if (chr.getParty() != null && mapitem.getOwner() !=
                 * chr.getId()) { List<MapleCharacter> toGive = new
                 * LinkedList<>(); int splitMeso = mapitem.getMeso() * 40 / 100;
                 * for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                 * MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                 * if (m != null && m.getId() != chr.getId()) { toGive.add(m); }
                 * } for (MapleCharacter m : toGive) { m.gainMeso(splitMeso /
                 * toGive.size() + (m.getStat().hasPartyBonus ? (int)
                 * (mapitem.getMeso() / 20.0) : 0), true); }
                 * chr.gainMeso(mapitem.getMeso() - splitMeso, true);
                 *
                 */
                // } else {
                chr.gainMeso(mapitem.getMeso(), true);
                //  }
                removeItem_Pet(chr, mapitem, petz);
            } else {
                if (mapitem.getItemId() / 10000 == 291) {
                    c.getSession().write(CWvsContext.enableActions());
                } else if (useItem(c, mapitem.getItemId())) {
                    removeItem_Pet(chr, mapitem, petz);
                } else if (MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                    MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster);
                    removeItem_Pet(chr, mapitem, petz);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static boolean useItem(MapleClient c, int id) {
        if (GameConstants.isUse(id)) { // TO prevent caching of everything, waste of mem
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MapleStatEffect eff = ii.getItemEffect(id);
            if (eff == null) {
                return false;
            }
            //must hack here for ctf
            if (id / 10000 == 291) {
                boolean area = false;
                for (Rectangle rect : c.getPlayer().getMap().getAreas()) {
                    if (rect.contains(c.getPlayer().getTruePosition())) {
                        area = true;
                        break;
                    }
                }
                if (!c.getPlayer().inPVP() || (c.getPlayer().getTeam() == (id - 2910000) && area)) {
                    return false; //dont apply the consume
                }
            }
            int consumeval = eff.getConsume();

            if (consumeval > 0) {
                consumeItem(c, eff);
                consumeItem(c, ii.getItemEffectEX(id));
                c.getSession().write(InfoPacket.getShowItemGain(id, (byte) 1));
                return true;
            }
        }
        return false;
    }

    public static void consumeItem(MapleClient c, MapleStatEffect eff) {
        if (eff == null) {
            return;
        }
        if (eff.getConsume() == 2) {
            if (c.getPlayer().getParty() != null && c.getPlayer().isAlive()) {
                for (MaplePartyCharacter pc : c.getPlayer().getParty().getMembers()) {
                    MapleCharacter chr = c.getPlayer().getMap().getCharacterById(pc.getId());
                    if (chr != null && chr.isAlive()) {
                        eff.applyTo(chr);
                    }
                }
            } else {
                eff.applyTo(c.getPlayer());
            }
        } else if (c.getPlayer().isAlive()) {
            eff.applyTo(c.getPlayer());
        }
    }

    public static void removeItem_Pet(MapleCharacter chr, MapleMapItem mapitem, int pet) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(CField.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), pet));
        chr.getMap().removeMapObject(mapitem);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    private static void removeItem(MapleCharacter chr, MapleMapItem mapitem, MapleMapObject ob) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(CField.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
        chr.getMap().removeMapObject(ob);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    private static void addMedalString(MapleCharacter c, StringBuilder sb) {
        Item medal = c.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -46);
        if (medal != null) { // Medal
            sb.append("<");
            if (medal.getItemId() == 1142257 && GameConstants.isAdventurer(c.getJob())) {
                MapleQuestStatus stat = c.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER));
                if (stat != null && stat.getCustomData() != null) {
                    sb.append(stat.getCustomData());
                    sb.append("'s Successor");
                } else {
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                }
            } else {
                sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
            }
            sb.append("> ");
        }
    }

    private static boolean getIncubatedItems(MapleClient c, int itemId) {
        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 2 || c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 2 || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 2) {
            c.getPlayer().dropMessage(5, "Please make room in your inventory.");
            return false;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int id1 = RandomRewards.getPeanutReward(), id2 = RandomRewards.getPeanutReward();
        while (!ii.itemExists(id1)) {
            id1 = RandomRewards.getPeanutReward();
        }
        while (!ii.itemExists(id2)) {
            id2 = RandomRewards.getPeanutReward();
        }
        c.getSession().write(CWvsContext.getPeanutResult(id1, (short) 1, id2, (short) 1, itemId));
        MapleInventoryManipulator.addById(c, id1, (short) 1, ii.getName(itemId) + " on " + FileoutputUtil.CurrentReadable_Date());
        MapleInventoryManipulator.addById(c, id2, (short) 1, ii.getName(itemId) + " on " + FileoutputUtil.CurrentReadable_Date());
        return true;
    }

    public static void OwlMinerva(LittleEndianAccessor slea, MapleClient c) {
        byte slot = (byte) slea.readShort();
        int itemid = slea.readInt();
        Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && itemid == 2310000 && !c.getPlayer().hasBlockedInventory()) {
            int itemSearch = slea.readInt();
            List<HiredMerchant> hms = c.getChannelServer().searchMerchant(itemSearch);
            if (hms.size() > 0) {
                c.getSession().write(CWvsContext.getOwlSearched(itemSearch, hms));
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, true, false);
            } else {
                c.getPlayer().dropMessage(1, "Unable to find the item.");
            }
        }
        c.getSession().write(CWvsContext.enableActions());
    }

    public static void Owl(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().haveItem(5230000, 1, true, false) || c.getPlayer().haveItem(2310000, 1, true, false)) {
            if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022) {
                c.getSession().write(CWvsContext.getOwlOpen());
            } else {
                c.getPlayer().dropMessage(5, "This can only be used inside the Free Market.");
                c.getSession().write(CWvsContext.enableActions());
            }
        }
    }

    public static int OWL_ID = 2; //don't change. 0 = owner ID, 1 = store ID, 2 = object ID

    public static void OwlWarp(LittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(CWvsContext.getOwlMessage(4));
            return;
        } else if (c.getPlayer().getTrade() != null) {
            c.getSession().write(CWvsContext.getOwlMessage(7));
            return;
        }
        if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022 && !c.getPlayer().hasBlockedInventory()) {
            int id = slea.readInt();
            int map = slea.readInt();
            if (map >= 910000001 && map <= 910000022) {
                c.getSession().write(CWvsContext.getOwlMessage(0));
                MapleMap mapp = c.getChannelServer().getMapFactory().getMap(map);
                c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                HiredMerchant merchant = null;
                List<MapleMapObject> objects;
                switch (OWL_ID) {
                    case 0:
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop) {
                                IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if (ips instanceof HiredMerchant) {
                                    HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getOwnerId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    case 1:
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop) {
                                IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if (ips instanceof HiredMerchant) {
                                    HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getStoreId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        MapleMapObject ob = mapp.getMapObject(id, MapleMapObjectType.HIRED_MERCHANT);
                        if (ob instanceof IMaplePlayerShop) {
                            IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                            if (ips instanceof HiredMerchant) {
                                merchant = (HiredMerchant) ips;
                            }
                        }
                        break;
                }
                if (merchant != null) {
                    if (merchant.isOwner(c.getPlayer())) {
                        merchant.setOpen(false);
                        merchant.removeAllVisitors((byte) 16, (byte) 0);
                        c.getPlayer().setPlayerShop(merchant);
                        c.getSession().write(PlayerShopPacket.getHiredMerch(c.getPlayer(), merchant, false));
                    } else {
                        if (!merchant.isOpen() || !merchant.isAvailable()) {
                            c.getPlayer().dropMessage(1, "The owner of the store is currently undergoing store maintenance. Please try again in a bit.");
                        } else {
                            if (merchant.getFreeSlot() == -1) {
                                c.getPlayer().dropMessage(1, "You can't enter the room due to full capacity.");
                            } else if (merchant.isInBlackList(c.getPlayer().getName())) {
                                c.getPlayer().dropMessage(1, "You may not enter this store.");
                            } else {
                                c.getPlayer().setPlayerShop(merchant);
                                merchant.addVisitor(c.getPlayer());
                                c.getSession().write(PlayerShopPacket.getHiredMerch(c.getPlayer(), merchant, false));
                            }
                        }
                    }
                } else {
                    c.getPlayer().dropMessage(1, "The room is already closed.");
                }
            } else {
                c.getSession().write(CWvsContext.getOwlMessage(23));
            }
        } else {
            c.getSession().write(CWvsContext.getOwlMessage(23));
        }
    }

    public static void PamSong(LittleEndianAccessor slea, MapleClient c) {
        Item pam = c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5640000);
        if (slea.readByte() > 0 && c.getPlayer().getScrolledPosition() != 0 && pam != null && pam.getQuantity() > 0) {
            MapleInventoryType inv = c.getPlayer().getScrolledPosition() < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP;
            Item item = c.getPlayer().getInventory(inv).getItem(c.getPlayer().getScrolledPosition());
            c.getPlayer().setScrolledPosition((short) 0);
            if (item != null) {
                Equip eq = (Equip) item;
                eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() + 1));
                c.getPlayer().forceReAddItem_Flag(eq, inv);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, pam.getPosition(), (short) 1, true, false);
                c.getPlayer().getMap().broadcastMessage(CField.pamsSongEffect(c.getPlayer().getId()));
            }
        } else {
            c.getPlayer().setScrolledPosition((short) 0);
        }
    }

    public static void TeleRock(LittleEndianAccessor slea, MapleClient c) {
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 232 || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        boolean used = UseTeleRock(slea, c, itemId);
        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().write(CWvsContext.enableActions());
    }

    /*
    傳送石相關設定
    */
    public static final boolean UseTeleRock(LittleEndianAccessor slea, MapleClient c, int itemId) {
        boolean used = false;
        if (itemId == 5041001 || itemId == 5040004) {
            slea.readByte(); //useless
        }
        if (slea.readByte() == 0) { // Rocktype
            final MapleMap target = c.getChannelServer().getMapFactory().getMap(slea.readInt());
            if ((itemId == 5041000 && c.getPlayer().isRockMap(target.getId())) || (itemId != 5041000 && c.getPlayer().isRegRockMap(target.getId())) || ((itemId == 5040004 || itemId == 5041001) && (c.getPlayer().isHyperRockMap(target.getId()) || GameConstants.isHyperTeleMap(target.getId())))) {
                if (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(target.getFieldLimit()) && !c.getPlayer().isInBlockedMap()) { //Makes sure this map doesn't have a forced return map
                    c.getPlayer().changeMap(target, target.getPortal(0));
                    used = true;
                }
            }
        } else {
            final MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
            if (victim != null && !victim.isIntern() && c.getPlayer().getEventInstance() == null && victim.getEventInstance() == null) {
                if (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(c.getChannelServer().getMapFactory().getMap(victim.getMapId()).getFieldLimit()) && !victim.isInBlockedMap() && !c.getPlayer().isInBlockedMap()) {
                    if (itemId == 5041000 || itemId == 5040004 || itemId == 5041001 || (victim.getMapId() / 100000000) == (c.getPlayer().getMapId() / 100000000)) { // Viprock or same continent
                        c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "Unable to move across continents.");
                    }
                } else {
                    c.getPlayer().dropMessage(1, "Due to the obstruction of the Earth's atmosphere, it is impossible to move to the character map.");
                }
            } else {
                c.getPlayer().dropMessage(1, "The player's position cannot be found now and cannot be moved.");
            }
        }
        return used && itemId != 5041001 && itemId != 5040004;
    }

    public static final void useInnerCirculator(LittleEndianAccessor slea, MapleClient c) {
        int itemid = slea.readInt();
        short slot = (short) slea.readInt();
        Item item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (item.getItemId() == itemid) {
            List<InnerSkillValueHolder> newValues = new LinkedList<>();
            int i = 0;
            for (InnerSkillValueHolder isvh : c.getPlayer().getInnerSkills()) {
                if (i == 0 && c.getPlayer().getInnerSkills().size() > 1 && itemid == 2701000) { //Ultimate Circulator
                    newValues.add(InnerAbility.getInstance().renewSkill(isvh.getRank(), itemid, true));
                } else {
                    newValues.add(InnerAbility.getInstance().renewSkill(isvh.getRank(), itemid, false));
                }
                //c.getPlayer().changeSkillLevel(SkillFactory.getSkill(isvh.getSkillId()), (byte) 0, (byte) 0);
                i++;
            }
            c.getPlayer().getInnerSkills().clear();
            for (InnerSkillValueHolder isvh : newValues) {
                c.getPlayer().getInnerSkills().add(isvh);
                //c.getPlayer().changeSkillLevel(SkillFactory.getSkill(isvh.getSkillId()), isvh.getSkillLevel(), isvh.getSkillLevel());
            }
            c.getPlayer().getInventory(MapleInventoryType.USE).removeItem(slot, (short) 1, false);

            /* I don't have packet for inner abiliy update */
            c.getSession().write(CField.getCharInfo(c.getPlayer()));
            MapleMap currentMap = c.getPlayer().getMap();
            currentMap.removePlayer(c.getPlayer());
            currentMap.addPlayer(c.getPlayer());
            // c.getSession().write(CField.updateInnerPotential());
            //c.getSession().write(CField.innerResetMessage());

            c.getPlayer().dropMessage(5, "Inner Potential has been reconfigured.");
        }
    }

    /*
    寶盒的護佑相關設定
    */
    public static void ResetCoreAura(int slot, MapleClient c, MapleCharacter chr) {
        Item starDust = chr.getInventory(MapleInventoryType.USE).getItem((short) slot);
        if (starDust == null || c.getPlayer().hasBlockedInventory() || chr.getLevel() < 10 || !GameConstants.isJett(chr.getJob()) || starDust.getPosition() != slot || (starDust.getItemId() != 2940000 && starDust.getItemId() != 2940001 && starDust.getItemId() != 2943000)) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        boolean used = false;
        if (starDust.getItemId() == 2943000) {
            if (chr.getCoreAura().getTimestamp() - System.currentTimeMillis() < 0L) {
                chr.getCoreAura().resetFluxField(0, true);
                chr.getStat().recalcLocalStats(chr);
                c.getSession().write(CWvsContext.updateCoreAura(chr, 1));
                c.getSession().write(CWvsContext.updateCoreAura(chr, 2));
                chr.dropMessage(5, "The Core Aura has already expired, please try again later.");
            } else if (!chr.getCoreAura().isSolid()) {
                chr.getCoreAura().addTimestamp(2943000);
                c.getSession().write(CWvsContext.updateCoreAura(chr, 2));
                used = true;
            } else {
                chr.dropMessage(5, "You have already used this item.");
            }
        } else {
            chr.getCoreAura().resetFluxField(starDust.getItemId(), chr.getLevel() >= 120);
            chr.getStat().recalcLocalStats(chr);
            c.getSession().write(CWvsContext.updateCoreAura(chr, 1));
            c.getSession().write(CWvsContext.updateCoreAura(chr, 2));
            used = true;
        }

        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, starDust.getPosition(), (short) 1, false, false);
        }
    }
}
