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

import client.*;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
import client.status.MonsterStatus;
import constants.GameConstants;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import scripting.EventInstanceManager;
import scripting.EventManager;
import scripting.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Randomizer;
import server.events.MapleCoconut;
import server.events.MapleCoconut.MapleCoconuts;
import server.events.MapleEventType;
import server.life.MapleMonsterInformationProvider;
import server.life.MonsterDropEntry;
import server.life.MonsterGlobalDropEntry;
import server.maps.*;
import server.quest.MapleQuest;
import tools.AttackPair;
import tools.FileoutputUtil;
import tools.Pair;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;

public class PlayersHandler {

    public static void Note(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final byte type = slea.readByte();

        switch (type) {
            case 0:
                String name = slea.readMapleAsciiString();
                String msg = slea.readMapleAsciiString();
                boolean fame = slea.readByte() > 0;
                slea.readInt(); //0?
                Item itemz = chr.getCashInventory().findByCashId((int) slea.readLong());
                if (itemz == null || !itemz.getGiftFrom().equalsIgnoreCase(name) || !chr.getCashInventory().canSendNote(itemz.getUniqueId())) {
                    return;
                }
                try {
                    chr.sendNote(name, msg, fame ? 1 : 0);
                    chr.getCashInventory().sendedNote(itemz.getUniqueId());
                } catch (Exception e) {
                }
                break;
            case 1:
                short num = slea.readShort();
                if (num < 0) { // note overflow, shouldn't happen much unless > 32767 
                    num = 32767;
                }
                slea.skip(1); // first byte = wedding boolean? 
                for (int i = 0; i < num; i++) {
                    final int id = slea.readInt();
                    chr.deleteNote(id, slea.readByte() > 0 ? 1 : 0);
                }
                break;
            default:
                System.out.println("Unhandled note action, " + type + "");
        }
    }

    public static void GiveFame(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int who = slea.readInt();
        final int mode = slea.readByte();

        final int famechange = mode == 0 ? -1 : 1;
        final MapleCharacter target = chr.getMap().getCharacterById(who);

        if (target == null || target == chr) { // faming self
            c.getSession().write(CWvsContext.giveFameErrorResponse(1));
            return;
        } else if (chr.getLevel() < 15) {
            c.getSession().write(CWvsContext.giveFameErrorResponse(2));
            return;
        }
        switch (chr.canGiveFame(target)) {
            case OK:
                if (Math.abs(target.getFame() + famechange) <= 99999) {
                    target.addFame(famechange);
                    target.updateSingleStat(MapleStat.FAME, target.getFame());
                }
                if (!chr.isGM()) {
                    chr.hasGivenFame(target);
                }
                c.getSession().write(CWvsContext.OnFameResult(0, target.getName(), famechange == 1, target.getFame()));
                target.getClient().getSession().write(CWvsContext.OnFameResult(5, chr.getName(), famechange == 1, 0));
                break;
            case NOT_TODAY:
                c.getSession().write(CWvsContext.giveFameErrorResponse(3));
                break;
            case NOT_THIS_MONTH:
                c.getSession().write(CWvsContext.giveFameErrorResponse(4));
                break;
        }
    }

    public static void UseDoor(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int oid = slea.readInt();
        final boolean mode = slea.readByte() == 0; // specifies if backwarp or not, 1 town to target, 0 target to town

        for (MapleMapObject obj : chr.getMap().getAllDoorsThreadsafe()) {
            final MapleDoor door = (MapleDoor) obj;
            if (door.getOwnerId() == oid) {
                door.warp(chr, mode);
                break;
            }
        }
    }

    /*
    設定幻影已複製的技能
    */
    public static void UpdateEquippedSkills(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (!GameConstants.isPhantom(chr.getJob())) {
            return;
        }
        int skillid = slea.readInt();
        int equippedid = slea.readInt();
        int slot = 0;
        switch (skillid) {
            case 24001001: //盜亦有道 Ⅰ
                slot = 1;
                break;
            case 24101001: //盜亦有道 Ⅱ
                slot = 2;
                break;
            case 24111001: //盜亦有道 Ⅲ
                slot = 3;
                break;
            case 24121001: //盜亦有道 Ⅳ
                slot = 4;
        }
        if (slot <= 0) {
            return;
        }
        chr.unequipPhantomSkill(slot);

        if (equippedid > 0) {
            Skill base = SkillFactory.getSkill(skillid);
            int skillLevel = chr.getSkillLevel(base);

            Skill equip = SkillFactory.getSkill(equippedid);
            int skillLevelE = chr.getSkillLevel(equip);

            byte SkillSlot = chr.getSkillSlot(equip);

            if ((!chr.isStolenSkill(equip)) || (!GameConstants.canBeStolen(equippedid)) || (SkillSlot < 0) || (skillLevel <= 0) || (skillLevelE <= 0) || (chr.getSkillExpiry(equip) != -1L) || (chr.getSkillExpiry(base) != -1L)) {
                return;
            }

            chr.changeSingleSkillLevel(equip, skillLevelE, (byte) equip.getMasterLevel(), -1L, SkillSlot, (byte) slot);

        }
        c.getSession().write(CWvsContext.updateEquippedSkill(skillid, equippedid));

    }

    /*
    新增和删除幻影複製的技能
    */
    public static void UpdateStolenSkills(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        int skillid = slea.readInt();
        int targetid = slea.readInt();

        boolean remove = slea.readByte() > 0;
        MapleCharacter target = chr.getMap().getCharacterById(targetid);

        if ((skillid == 0) || (!GameConstants.isPhantom(chr.getJob())) || ((remove) && (targetid > 0)) || ((!remove) && ((targetid != chr.getStealTarget()) || (target == null) || ((target.isGM()) && (!chr.isGM())) || (!GameConstants.isAdventurer(target.getJob())) || (GameConstants.isCannon(target.getJob())) || (GameConstants.isJett(target.getJob()))))) {
            chr.setStealTarget(0);
            c.getSession().write(CWvsContext.updateStolenSkills(1));
            return;
        }
        Skill sx = SkillFactory.getSkill(skillid);
        byte newSlot = chr.getNextStolenSlot(skillid);
        int skillLevel = remove ? 1 : target.getSkillLevel(sx);
        if ((skillLevel <= 0) || ((!remove) && ((target.getSkillExpiry(sx) != -1L) || (newSlot < 0) || (!GameConstants.canBeStolen(skillid, target.getJob())) || (skillid / 10000 % 10 > chr.getJob() % 10)))) {
            chr.setStealTarget(0);
            c.getSession().write(CWvsContext.updateStolenSkills(2));
            return;
        }

        if (remove) {
            byte oldSlot = chr.getSkillSlot(sx);
            byte equipped = chr.getSkillEquipped(sx);

            chr.removeStolenSkill(sx);

            if (equipped > 0) {
                chr.changeSingleSkillLevel(sx, chr.getSkillLevel(skillid), chr.getSkillLevel(skillid), -1, (byte) 0, (byte) 0);
                c.getSession().write(CWvsContext.updateEquippedSkill(GameConstants.getPhantomBookSkill(equipped), 0));
                c.getSession().write(CWvsContext.updateStolenSkills(3, skillid, 0, oldSlot));

            } else {
                chr.changeSingleSkillLevel(sx, chr.getSkillLevel(skillid), chr.getSkillLevel(skillid), -1, (byte) -1, (byte) -1);
                c.getSession().write(CWvsContext.updateStolenSkills(3, skillid, 0, oldSlot));
            }
        } else {
            chr.changeSingleSkillLevel(sx, skillLevel, (byte) sx.getMasterLevel(), -1L, newSlot, (byte) 0);
            c.getSession().write(CWvsContext.updateStolenSkills(0, skillid, skillLevel, newSlot));
        }
        chr.setStealTarget(0);
    }

    /*
    點擊玩家打開可以複製的技能介面
    */
    public static void SkillSwipeRequest(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        MapleCharacter target = chr.getMap().getCharacterById(slea.readInt());
        if ((target == null) || ((target.isGM()) && (!chr.isGM())) || (!GameConstants.isAdventurer(target.getJob())) || (GameConstants.isCannon(target.getJob())) || (GameConstants.isJett(target.getJob())) || (!GameConstants.isPhantom(chr.getJob()))) {
            c.getSession().write(CWvsContext.updateStolenSkills(1));
            return;
        }
        chr.setStealTarget(target.getId());
        List<Integer> skills = new ArrayList();
        for (Map.Entry skill : target.getSkills().entrySet()) {
            int skillid = ((Skill) skill.getKey()).getId();
            if ((GameConstants.canBeStolen(skillid, target.getJob())) && (((SkillEntry) skill.getValue()).skillevel > 0) && (((SkillEntry) skill.getValue()).expiration == -1L)) {
                MapleStatEffect stat = ((Skill) skill.getKey()).getEffect(1);
                if ((stat != null)) {
                    skills.add(Integer.valueOf(skillid));
                }
            }
        }
        c.getSession().write(CWvsContext.showTargetSkills(target.getId(), target.getJob(), skills));
    }

    public static void UseMechDoor(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int oid = slea.readInt();
        final Point pos = slea.readPos();
        final int mode = slea.readByte(); // specifies if backwarp or not, 1 town to target, 0 target to town
        chr.getClient().getSession().write(CWvsContext.enableActions());
        for (MapleMapObject obj : chr.getMap().getAllMechDoorsThreadsafe()) {
            final MechDoor door = (MechDoor) obj;
            if (door.getOwnerId() == oid && door.getId() == mode) {
                chr.checkFollow();
                chr.getMap().movePlayer(chr, pos);
                break;
            }
        }
    }

    public static void TransformPlayer(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        // D9 A4 FD 00
        // 11 00
        // A0 C0 21 00
        // 07 00 64 66 62 64 66 62 64
        slea.readInt();
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final String target = slea.readMapleAsciiString();

        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        switch (itemId) {
            case 2212000:
                final MapleCharacter search_chr = chr.getMap().getCharacterByName(target);
                if (search_chr != null) {
                    MapleItemInformationProvider.getInstance().getItemEffect(2210023).applyTo(search_chr);
                    search_chr.dropMessage(6, chr.getName() + " has played a prank on you!");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                }
                break;
        }
    }

    public static void HitReactor(final LittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final int charPos = slea.readInt();
        final short stance = slea.readShort();
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);

        if (reactor == null || !reactor.isAlive()) {
            return;
        }
        reactor.hitReactor(charPos, stance, c);
    }

    public static void TouchReactor(final LittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final boolean touched = slea.available() == 0 || slea.readByte() > 0; //the byte is probably the state to set it to
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (!touched || reactor == null || !reactor.isAlive() || reactor.getTouch() == 0) {
            return;
        }
        if (reactor.getTouch() == 2) {
            ReactorScriptManager.getInstance().act(c, reactor); //not sure how touched boolean comes into play
        } else if (reactor.getTouch() == 1 && !reactor.isTimerActive()) {
            if (reactor.getReactorType() == 100) {
                final int itemid = GameConstants.getCustomReactItem(reactor.getReactorId(), reactor.getReactItem().getLeft(), reactor.getMap().getId());
                if (c.getPlayer().haveItem(itemid, reactor.getReactItem().getRight())) {
                    if (reactor.getArea().contains(c.getPlayer().getTruePosition())) {
                        MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemid), itemid, reactor.getReactItem().getRight(), true, false);
                        reactor.hitReactor(c);
                    } else {
                        c.getPlayer().dropMessage(5, "You are too far away.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "You don't have the item required.");
                }
            } else {
                //just hit it
                int id[] = {2001000, 2001001, 2006001, 2008000, 2008001, 2008004, 2008005, 2512001, 2519000, 2519001, 2519002, 2519003, 2618000, 2618001, 2618002, 3002000, 3009000, 9108000, 9108001, 9108002, 9108003, 9108004, 9108005, 9218000};
                int item[] = {4001053, 4001053, 4001055, 4001046, 4001049, 4001048, 4001045, 4031437, 4001117, 4001117, 4001117, 4001117, 4001132, 4001133, 4001133, 4001161, 4001162, 4001453, 4001453, 4001453, 4001453, 4001453, 4001453, 4001528};
                for (int i = 0; i < id.length; i++)
                    if (reactor.getReactorId() == id[i]) {
                        int y = i;
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, item[i], 1, true, false);//觸發品扣除
                    }
                reactor.hitReactor(c);
            }
        }
    }

    public static void hitCoconut(LittleEndianAccessor slea, MapleClient c) {
        /*
         * CB 00 A6 00 06 01 A6 00 = coconut id 06 01 = ?
         */
        int id = slea.readShort();
        String co = "coconut";
        MapleCoconut map = (MapleCoconut) c.getChannelServer().getEvent(MapleEventType.Coconut);
        if (map == null || !map.isRunning()) {
            map = (MapleCoconut) c.getChannelServer().getEvent(MapleEventType.CokePlay);
            co = "coke cap";
            if (map == null || !map.isRunning()) {
                return;
            }
        }
        //System.out.println("Coconut1");
        MapleCoconuts nut = map.getCoconut(id);
        if (nut == null || !nut.isHittable()) {
            return;
        }
        if (System.currentTimeMillis() < nut.getHitTime()) {
            return;
        }
        //System.out.println("Coconut2");
        if (nut.getHits() > 2 && Math.random() < 0.4 && !nut.isStopped()) {
            //System.out.println("Coconut3-1");
            nut.setHittable(false);
            if (Math.random() < 0.01 && map.getStopped() > 0) {
                nut.setStopped(true);
                map.stopCoconut();
                c.getPlayer().getMap().broadcastMessage(CField.hitCoconut(false, id, 1));
                return;
            }
            nut.resetHits(); // For next event (without restarts)
            //System.out.println("Coconut4");
            if (Math.random() < 0.05 && map.getBombings() > 0) {
                //System.out.println("Coconut5-1");
                c.getPlayer().getMap().broadcastMessage(CField.hitCoconut(false, id, 2));
                map.bombCoconut();
            } else if (map.getFalling() > 0) {
                //System.out.println("Coconut5-2");
                c.getPlayer().getMap().broadcastMessage(CField.hitCoconut(false, id, 3));
                map.fallCoconut();
                if (c.getPlayer().getTeam() == 0) {
                    map.addMapleScore();
                    //c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(5, c.getPlayer().getName() + " of Team Maple knocks down a " + co + "."));
                } else {
                    map.addStoryScore();
                    //c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(5, c.getPlayer().getName() + " of Team Story knocks down a " + co + "."));
                }
                c.getPlayer().getMap().broadcastMessage(CField.coconutScore(map.getCoconutScore()));
            }
        } else {
            //System.out.println("Coconut3-2");
            nut.hit();
            c.getPlayer().getMap().broadcastMessage(CField.hitCoconut(false, id, 1));
        }
    }

    public static void FollowRequest(final LittleEndianAccessor slea, final MapleClient c) {
        MapleCharacter tt = c.getPlayer().getMap().getCharacterById(slea.readInt());
        if (slea.readByte() > 0) {
            //1 when changing map
            tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
            if (tt != null && tt.getFollowId() == c.getPlayer().getId()) {
                tt.setFollowOn(true);
                c.getPlayer().setFollowOn(true);
            } else {
                c.getPlayer().checkFollow();
            }
            return;
        }
        if (slea.readByte() > 0) { //cancelling follow
            tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
            if (tt != null && tt.getFollowId() == c.getPlayer().getId() && c.getPlayer().isFollowOn()) {
                c.getPlayer().checkFollow();
            }
            return;
        }
        if (tt != null && tt.getPosition().distanceSq(c.getPlayer().getPosition()) < 10000 && tt.getFollowId() == 0 && c.getPlayer().getFollowId() == 0 && tt.getId() != c.getPlayer().getId()) { //estimate, should less
            tt.setFollowId(c.getPlayer().getId());
            tt.setFollowOn(false);
            tt.setFollowInitiator(false);
            c.getPlayer().setFollowOn(false);
            c.getPlayer().setFollowInitiator(false);
            tt.getClient().getSession().write(CWvsContext.followRequest(c.getPlayer().getId()));
        } else {
            c.getSession().write(CWvsContext.serverNotice(1, "You are too far away."));
        }
    }

    public static void FollowReply(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().getFollowId() > 0 && c.getPlayer().getFollowId() == slea.readInt()) {
            MapleCharacter tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
            if (tt != null && tt.getPosition().distanceSq(c.getPlayer().getPosition()) < 10000 && tt.getFollowId() == 0 && tt.getId() != c.getPlayer().getId()) { //estimate, should less
                boolean accepted = slea.readByte() > 0;
                if (accepted) {
                    tt.setFollowId(c.getPlayer().getId());
                    tt.setFollowOn(true);
                    tt.setFollowInitiator(false);
                    c.getPlayer().setFollowOn(true);
                    c.getPlayer().setFollowInitiator(true);
                    c.getPlayer().getMap().broadcastMessage(CField.followEffect(tt.getId(), c.getPlayer().getId(), null));
                } else {
                    c.getPlayer().setFollowId(0);
                    tt.setFollowId(0);
                    tt.getClient().getSession().write(CField.getFollowMsg(5));
                }
            } else {
                if (tt != null) {
                    tt.setFollowId(0);
                    c.getPlayer().setFollowId(0);
                }
                c.getSession().write(CWvsContext.serverNotice(1, "You are too far away."));
            }
        } else {
            c.getPlayer().setFollowId(0);
        }
    }

    public static void DoRing(final MapleClient c, final String name, final int itemid) {
        final int newItemId = itemid == 2240000 ? 1112803 : (itemid == 2240001 ? 1112806 : (itemid == 2240002 ? 1112807 : (itemid == 2240003 ? 1112809 : (1112300 + (itemid - 2240004)))));
        final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
        int errcode = 0;
        if (c.getPlayer().getMarriageId() > 0) {
            errcode = 0x17;
        } else if (chr == null) {
            errcode = 0x12;
        } else if (chr.getMapId() != c.getPlayer().getMapId()) {
            errcode = 0x13;
        } else if (!c.getPlayer().haveItem(itemid, 1) || itemid < 2240000 || itemid > 2240015) {
            errcode = 0x0D;
        } else if (chr.getMarriageId() > 0 || chr.getMarriageItemId() > 0) {
            errcode = 0x18;
        } else if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "")) {
            errcode = 0x14;
        } else if (!MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) {
            errcode = 0x15;
        }
        if (errcode > 0) {
            c.getSession().write(CWvsContext.sendEngagement((byte) errcode, 0, null, null));
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.getPlayer().setMarriageItemId(itemid);
        chr.getClient().getSession().write(CWvsContext.sendEngagementRequest(c.getPlayer().getName(), c.getPlayer().getId()));
    }

    public static void RingAction(final LittleEndianAccessor slea, final MapleClient c) {
        final byte mode = slea.readByte();
        if (mode == 0) {
            DoRing(c, slea.readMapleAsciiString(), slea.readInt());
            //1112300 + (itemid - 2240004)
        } else if (mode == 1) {
            c.getPlayer().setMarriageItemId(0);
        } else if (mode == 2) { //accept/deny proposal
            final boolean accepted = slea.readByte() > 0;
            final String name = slea.readMapleAsciiString();
            final int id = slea.readInt();
            final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (c.getPlayer().getMarriageId() > 0 || chr == null || chr.getId() != id || chr.getMarriageItemId() <= 0 || !chr.haveItem(chr.getMarriageItemId(), 1) || chr.getMarriageId() > 0 || !chr.isAlive() || chr.getEventInstance() != null || !c.getPlayer().isAlive() || c.getPlayer().getEventInstance() != null) {
                c.getSession().write(CWvsContext.sendEngagement((byte) 0x1D, 0, null, null));
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (accepted) {
                final int itemid = chr.getMarriageItemId();
                final int newItemId = itemid == 2240000 ? 1112803 : (itemid == 2240001 ? 1112806 : (itemid == 2240002 ? 1112807 : (itemid == 2240003 ? 1112809 : (1112300 + (itemid - 2240004)))));
                if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "") || !MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) {
                    c.getSession().write(CWvsContext.sendEngagement((byte) 0x15, 0, null, null));
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
                try {
                    final int[] ringID = MapleRing.makeRing(newItemId, c.getPlayer(), chr);
                    Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(newItemId, ringID[1]);
                    MapleRing ring = MapleRing.loadFromDb(ringID[1]);
                    if (ring != null) {
                        eq.setRing(ring);
                    }
                    MapleInventoryManipulator.addbyItem(c, eq);

                    eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(newItemId, ringID[0]);
                    ring = MapleRing.loadFromDb(ringID[0]);
                    if (ring != null) {
                        eq.setRing(ring);
                    }
                    MapleInventoryManipulator.addbyItem(chr.getClient(), eq);

                    MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.USE, chr.getMarriageItemId(), 1, false, false);

                    chr.getClient().getSession().write(CWvsContext.sendEngagement((byte) 0x10, newItemId, chr, c.getPlayer()));
                    chr.setMarriageId(c.getPlayer().getId());
                    c.getPlayer().setMarriageId(chr.getId());

                    chr.fakeRelog();
                    c.getPlayer().fakeRelog();
                } catch (Exception e) {
                    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                }

            } else {
                chr.getClient().getSession().write(CWvsContext.sendEngagement((byte) 0x1E, 0, null, null));
            }
            c.getSession().write(CWvsContext.enableActions());
            chr.setMarriageItemId(0);
        } else if (mode == 3) { //drop, only works for ETC
            final int itemId = slea.readInt();
            final MapleInventoryType type = GameConstants.getInventoryType(itemId);
            final Item item = c.getPlayer().getInventory(type).findById(itemId);
            if (item != null && type == MapleInventoryType.ETC && itemId / 10000 == 421) {
                MapleInventoryManipulator.drop(c, type, item.getPosition(), item.getQuantity());
            }
        }
    }

    public static void Solomon(final LittleEndianAccessor slea, final MapleClient c) {
        c.getSession().write(CWvsContext.enableActions());
        slea.readInt();
        Item item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slea.readShort());
        if (item == null || item.getItemId() != slea.readInt() || item.getQuantity() <= 0 || c.getPlayer().getGachExp() > 0 || c.getPlayer().getLevel() > 50 || MapleItemInformationProvider.getInstance().getItemEffect(item.getItemId()).getEXP() <= 0) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.getPlayer().setGachExp(c.getPlayer().getGachExp() + MapleItemInformationProvider.getInstance().getItemEffect(item.getItemId()).getEXP());
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, item.getPosition(), (short) 1, false);
        c.getPlayer().updateSingleStat(MapleStat.GACHAPONEXP, c.getPlayer().getGachExp());
    }

    public static void GachExp(final LittleEndianAccessor slea, final MapleClient c) {
        c.getSession().write(CWvsContext.enableActions());
        slea.readInt();
        if (c.getPlayer().getGachExp() <= 0) {
            return;
        }
        c.getPlayer().gainExp(c.getPlayer().getGachExp(), true, true, false);
        c.getPlayer().setGachExp(0);
        c.getPlayer().updateSingleStat(MapleStat.GACHAPONEXP, 0);
    }

    /*
    玩家舉報
    */
    public static void Report(final LittleEndianAccessor slea, final MapleClient c) {
        //0 = success 1 = unable to locate 2 = once a day 3 = you've been reported 4+ = unknown reason
        /*
         * MapleCharacter other; ReportType type; /* if (!GameConstants.GMS) {
         * other = c.getPlayer().getMap().getCharacterById(slea.readInt()); type
         * = ReportType.getById(slea.readByte()); } else { type =
         * ReportType.getById(slea.readByte()); other =
         * c.getPlayer().getMap().getCharacterByName(slea.readMapleAsciiString());
         * //th
         *
         */
        /*
         * if (other == null || type == null || other.isIntern()) {
         * c.getSession().write(CWvsContext.report(4)); return; } final
         * MapleQuestStatus stat =
         * c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.REPORT_QUEST));
         * if (stat.getCustomData() == null) { stat.setCustomData("0"); } final
         * long currentTime = System.currentTimeMillis(); final long theTime =
         * Long.parseLong(stat.getCustomData()); if (theTime + 7200000 >
         * currentTime && !c.getPlayer().isIntern()) {
         * c.getSession().write(CWvsContext.enableActions());
         * c.getPlayer().dropMessage(5, "You may only report every 2 hours."); }
         * else { stat.setCustomData(String.valueOf(currentTime));
         * other.addReport(type); c.getSession().write(CWvsContext.report(2));
         */
        // }
    }

    public static void MonsterBookInfoRequest(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        slea.readInt(); // tick
        final MapleCharacter player = c.getPlayer().getMap().getCharacterById(slea.readInt());
        c.getSession().write(CWvsContext.enableActions());
        if (player != null) {
            c.getSession().write(CWvsContext.getMonsterBookInfo(player));
        }
    }

    public static void MonsterBookDropsRequest(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        slea.readInt();
        final int cardid = slea.readInt();
        final int mobid = MapleItemInformationProvider.getInstance().getCardMobId(cardid);
        if (mobid <= 0 || !chr.getMonsterBook().hasCard(cardid)) {
            c.getSession().write(CWvsContext.getCardDrops(cardid, null));
            return;
        }
        final MapleMonsterInformationProvider ii = MapleMonsterInformationProvider.getInstance();
        final List<Integer> newDrops = new ArrayList<>();
        for (final MonsterDropEntry de : ii.retrieveDrop(mobid)) {
            if (de.itemId > 0 && de.questid <= 0 && !newDrops.contains(de.itemId)) {
                newDrops.add(de.itemId);
            }
        }
        for (final MonsterGlobalDropEntry de : ii.getGlobalDrop()) {
            if (de.itemId > 0 && de.questid <= 0 && !newDrops.contains(de.itemId)) {
                newDrops.add(de.itemId);
            }
        }
        c.getSession().write(CWvsContext.getCardDrops(cardid, newDrops));
    }

    public static void findFriend(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        byte boob = slea.readByte();
        if (boob == 5) {
            if (chr.getBIRTHDAY() == 0 && chr.getTODO() == 0 && chr.getLOCATION() == 0 && chr.getFOUND() == 0) {
                c.getSession().write(CWvsContext.myInfoResult(0));
            } else {
                c.getSession().write(CWvsContext.myInfoResult(1));
            }
        } else if (boob == 7) {
            List<MapleCharacter> frends = new LinkedList<>();
            for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                if (mch.getId() != c.getPlayer().getId()) {
                    frends.add(mch);
                }

            }
            c.getSession().write(CWvsContext.findFriendResult(frends));
        } else if (boob == 10) {
            int cid = slea.readInt();
            c.getSession().write(CWvsContext.friendCharacterInfo(c.getChannelServer().getPlayerStorage().getCharacterById(cid)));
        }
    }

    public static void loadInfo(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        int boob = slea.readByte();
        if (boob != 1) {
            int location = slea.readInt();
            int birthday = slea.readInt();
            int todo = slea.readInt();
            int found = slea.readInt();
            chr.setBIRTHDAY(birthday);
            chr.setTODO(todo);
            chr.setFOUND(found);
            chr.setLOCATION(location);
            c.getSession().write(CWvsContext.saveInformation(false));
        } else {
            c.getSession().write(CWvsContext.loadInformation(chr.getLOCATION(), chr.getTODO(), chr.getBIRTHDAY(), chr.getFOUND()));
        }
    }

    public static void ChangeSet(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        final int set = slea.readInt();
        chr.getMonsterBook().changeSet(set);
        chr.getMonsterBook().applyBook(chr, false);
        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.CURRENT_SET)).setCustomData(String.valueOf(set));
        c.getSession().write(CWvsContext.changeCardSet(set));

    }

    public static void EnterPVP(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null || c.getPlayer().getMapId() != 960000000) {
            c.getSession().write(CField.pvpBlocked(1));
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (c.getPlayer().getParty() != null) {
            c.getSession().write(CField.pvpBlocked(9));
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        slea.readInt();
        slea.skip(1);
        int type = slea.readByte(), lvl = slea.readByte(), playerCount = 0;
        boolean passed = false;
        switch (lvl) {
            case 0:
                passed = c.getPlayer().getLevel() >= 30 && c.getPlayer().getLevel() < 70;
                break;
            case 1:
                passed = c.getPlayer().getLevel() >= 70;
                break;
            case 2:
                passed = c.getPlayer().getLevel() >= 120;
                break;
            case 3:
                passed = c.getPlayer().getLevel() >= 180;
                break;
        }
        final EventManager em = c.getChannelServer().getEventSM().getEventManager("PVP");
        if (!passed || em == null) {
            c.getSession().write(CField.pvpBlocked(1));
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final List<Integer> maps = new ArrayList<>();
        switch (type) {
            case 0:
                maps.add(960010100);
                maps.add(960010101);
                maps.add(960010102);
                break;
            case 1:
                maps.add(960020100);
                maps.add(960020101);
                maps.add(960020102);
                maps.add(960020103);
                break;
            case 2:
                maps.add(960030100);
                break;
            case 3:
                maps.add(689000000);
                maps.add(689000010);
                break;
            default:
                passed = false;
                break;
        }
        if (!passed) {
            c.getSession().write(CField.pvpBlocked(1));
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.getPlayer().getStat().heal(c.getPlayer());
        c.getPlayer().cancelAllBuffs();
        c.getPlayer().dispelDebuffs();
        c.getPlayer().changeRemoval();
        c.getPlayer().clearAllCooldowns();
        c.getPlayer().unequipAllPets();
        final StringBuilder key = new StringBuilder().append(lvl).append(" ").append(type).append(" ");
        //check if any of the maps are available
        for (int i : maps) {
            final EventInstanceManager eim = em.getInstance(new StringBuilder("PVP").append(key.toString()).append(i).toString().replace(" ", "").replace(" ", ""));
            if (eim != null && (eim.getProperty("started").equals("0") || eim.getPlayerCount() < 10)) {
                eim.registerPlayer(c.getPlayer());
                return;
            }
        }
        //make one
        em.startInstance_Solo(key.append(maps.get(Randomizer.nextInt(maps.size()))).toString(), c.getPlayer());
    }

    public static void RespawnPVP(final LittleEndianAccessor slea, final MapleClient c) {
        final Lock ThreadLock = new ReentrantLock();
        if (c.getPlayer() == null || c.getPlayer().getMap() == null || !c.getPlayer().inPVP() || c.getPlayer().isAlive()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final int type = Integer.parseInt(c.getPlayer().getEventInstance().getProperty("type"));
        byte lvl = 0;
        c.getPlayer().getStat().heal_noUpdate(c.getPlayer());
        c.getPlayer().updateSingleStat(MapleStat.MP, c.getPlayer().getStat().getMp());
        ThreadLock.lock();
        try {
            c.getPlayer().getEventInstance().schedule("updateScoreboard", 500);
        } finally {
            ThreadLock.unlock();
        }
        c.getPlayer().changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().getPortal(type == 0 ? 0 : (type == 3 ? (c.getPlayer().getTeam() == 0 ? 3 : 1) : (c.getPlayer().getTeam() == 0 ? 2 : 3))));
        c.getSession().write(CField.getPVPScore(Integer.parseInt(c.getPlayer().getEventInstance().getProperty(String.valueOf(c.getPlayer().getId()))), false));

        if (c.getPlayer().getLevel() >= 30 && c.getPlayer().getLevel() < 70) {
            lvl = 0;
        } else if (c.getPlayer().getLevel() >= 70 && c.getPlayer().getLevel() < 120) {
            lvl = 1;
        } else if (c.getPlayer().getLevel() >= 120 && c.getPlayer().getLevel() < 180) {
            lvl = 2;
        } else if (c.getPlayer().getLevel() >= 180) {
            lvl = 3;
        }
        List<MapleCharacter> players = c.getPlayer().getEventInstance().getPlayers();
        List<Pair<Integer, String>> players1 = new LinkedList<>();
        for (int xx = 0; xx < players.size(); xx++) {
            players1.add(new Pair<>(players.get(xx).getId(), players.get(xx).getName()));
        }
        c.getSession().write(CField.getPVPType(type, players1, c.getPlayer().getTeam(), true, lvl));
        c.getSession().write(CField.enablePVP(true));
    }

    public static void LeavePVP(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null || !c.getPlayer().inPVP()) {
            c.getSession().write(CField.pvpBlocked(6));
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int x = Integer.parseInt(c.getPlayer().getEventInstance().getProperty(String.valueOf(c.getPlayer().getId())));
        final int lv = Integer.parseInt(c.getPlayer().getEventInstance().getProperty("lvl"));
        if (lv < 2 && c.getPlayer().getLevel() >= 120) { //gladiator, level 120+
            x /= 2;
        }
        c.getPlayer().setTotalBattleExp(c.getPlayer().getTotalBattleExp() + ((x / 10) * 3 / 2));
        c.getPlayer().setBattlePoints(c.getPlayer().getBattlePoints() + ((x / 10) * 3 / 2)); //PVP 1.5 EVENT!
        c.getPlayer().cancelAllBuffs();
        c.getPlayer().changeRemoval();
        c.getPlayer().dispelDebuffs();
        c.getPlayer().clearAllCooldowns();
        slea.readInt();
        c.getSession().write(CWvsContext.clearMidMsg());
        c.getPlayer().changeMap(c.getChannelServer().getMapFactory().getMap(960000000));
        c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
        c.getPlayer().getStat().heal(c.getPlayer());
    }

    public static final void AttackPVP(final LittleEndianAccessor slea, final MapleClient c) {
        final Lock ThreadLock = new ReentrantLock();
        final MapleCharacter chr = c.getPlayer();
        final int trueSkill = slea.readInt();
        int skillid = trueSkill;
        if (chr == null || chr.isHidden() || !chr.isAlive() || chr.hasBlockedInventory() || chr.getMap() == null || !chr.inPVP() || !chr.getEventInstance().getProperty("started").equals("1") || skillid >= 90000000) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final int lvl = Integer.parseInt(chr.getEventInstance().getProperty("lvl"));
        final int type = Integer.parseInt(chr.getEventInstance().getProperty("type"));
        final int ice = Integer.parseInt(chr.getEventInstance().getProperty("ice"));
        final int ourScore = Integer.parseInt(chr.getEventInstance().getProperty(String.valueOf(chr.getId())));
        int addedScore = 0, skillLevel = 0, trueSkillLevel = 0, animation = -1, attackCount = 1, mobCount = 1, fakeMastery = chr.getStat().passive_mastery(), ignoreDEF = chr.getStat().ignoreTargetDEF, critRate = chr.getStat().passive_sharpeye_rate(), skillDamage = 100;
        boolean magic = false, move = false, pull = false, push = false;

        double maxdamage = lvl == 3 ? chr.getStat().getCurrentMaxBasePVPDamageL() : chr.getStat().getCurrentMaxBasePVPDamage();
        MapleStatEffect effect = null;
        chr.checkFollow();
        Rectangle box = null;

        final Item weapon = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        final Item shield = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        final boolean katara = shield != null && shield.getItemId() / 10000 == 134;
        final boolean aran = weapon != null && weapon.getItemId() / 10000 == 144 && GameConstants.isAran(chr.getJob());
        slea.skip(1); //skill level
        int chargeTime = 0;
        if (GameConstants.isMagicChargeSkill(skillid)) {
            chargeTime = slea.readInt();
        } else {
            slea.skip(4);
        }
        boolean facingLeft = slea.readByte() > 0;
        if (skillid > 0) {
            if (skillid == 3211006 && chr.getTotalSkillLevel(3220010) > 0) { //終極四連箭
                skillid = 3220010;
            }
            final Skill skil = SkillFactory.getSkill(skillid);
            if (skil == null || skil.isPVPDisabled()) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            magic = skil.isMagic();
            move = skil.isMovement();
            push = skil.isPush();
            pull = skil.isPull();

            //PVP冰騎士相關
            if (chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0) {
                if (!GameConstants.isIceKnightSkill(skillid) && chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0) {
                    //c.getSession().close();
                    return;
                }
                if (GameConstants.isIceKnightSkill(skillid) && chr.getBuffSource(MapleBuffStat.MORPH) % 10000 != 1105) {
                    return;
                }
            }
            animation = skil.getAnimation();
            if (animation == -1 && !skil.isMagic()) {
                final String after = aran ? "aran" : (katara ? "katara" : (weapon == null ? "barehands" : MapleItemInformationProvider.getInstance().getAfterImage(weapon.getItemId())));
                if (after != null) {
                    final List<Triple<String, Point, Point>> p = MapleItemInformationProvider.getInstance().getAfterImage(after); //hack
                    if (p != null) {
                        ThreadLock.lock();
                        try {
                            while (animation == -1) {
                                final Triple<String, Point, Point> ep = p.get(Randomizer.nextInt(p.size()));
                                if (ep.left.contains("stab") && weapon != null && weapon.getItemId() / 10000 == 144) {
                                    continue;
                                }
                                if (SkillFactory.getDelay(ep.left) != null) {
                                    animation = SkillFactory.getDelay(ep.left);
                                }
                            }
                        } finally {
                            ThreadLock.unlock();
                        }
                    }
                }
            } else if (animation == -1 && skil.isMagic()) {
                animation = SkillFactory.getDelay(Randomizer.nextBoolean() ? "dash" : "dash2");
            }
            if (skil.isMagic()) {
                fakeMastery = 0; //whoosh still comes if you put this higher than 0
            }
            skillLevel = chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid));
            trueSkillLevel = chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(trueSkill));
            effect = skil.getPVPEffect(skillLevel);
            ignoreDEF += effect.getIgnoreMob();
            critRate += effect.getCr();

            skillDamage = (effect.getDamage() + chr.getStat().getDamageIncrease(skillid));
            box = effect.calculateBoundingBox(chr.getTruePosition(), facingLeft, chr.getStat().defRange);
            attackCount = Math.max(effect.getBulletCount(), effect.getAttackCount());
            mobCount = Math.max(1, effect.getMobCount());
            if (effect.getCooldown(chr) > 0 && !chr.isGM()) {
                if (chr.skillisCooling(skillid)) {
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
                if ((skillid != 35111004 && skillid != 35121013) || chr.getBuffSource(MapleBuffStat.MECH_CHANGE) != skillid) { // Battleship
                    c.getSession().write(CField.skillCooldown(skillid, effect.getCooldown(chr)));
                    chr.addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown(chr) * 1000);
                }
            }
            switch (chr.getJob()) {
                case 111:
                case 112:
                case 1111:
                case 1112:
                    if (PlayerHandler.isFinisher(skillid) > 0) { // finisher
                        if (chr.getBuffedValue(MapleBuffStat.COMBO) == null || chr.getBuffedValue(MapleBuffStat.COMBO) <= 2) {
                            return;
                        }
                        if (!GameConstants.GMS) {
                            skillDamage *= (chr.getBuffedValue(MapleBuffStat.COMBO) - 1) / 2;
                        }
                        chr.handleOrbconsume(PlayerHandler.isFinisher(skillid));
                    }
                    break;
            }
        } else {
            attackCount = (katara ? 2 : 1);
            Point lt = null, rb = null;
            final String after = aran ? "aran" : (katara ? "katara" : (weapon == null ? "barehands" : MapleItemInformationProvider.getInstance().getAfterImage(weapon.getItemId())));
            if (after != null) {
                final List<Triple<String, Point, Point>> p = MapleItemInformationProvider.getInstance().getAfterImage(after);
                if (p != null) {
                    ThreadLock.lock();
                    try {
                        while (animation == -1) {
                            final Triple<String, Point, Point> ep = p.get(Randomizer.nextInt(p.size()));
                            if (ep.left.contains("stab") && weapon != null && weapon.getItemId() / 10000 == 147) {
                                continue;
                            }
                            if (SkillFactory.getDelay(ep.left) != null) {
                                animation = SkillFactory.getDelay(ep.left);
                                lt = ep.mid;
                                rb = ep.right;
                            }
                        }
                    } finally {
                        ThreadLock.unlock();
                    }
                }
            }
            box = MapleStatEffect.calculateBoundingBox(chr.getTruePosition(), facingLeft, lt, rb, chr.getStat().defRange);
        }
        final MapleStatEffect shad = chr.getStatForBuff(MapleBuffStat.SHADOWPARTNER);
        final int originalAttackCount = attackCount;
        attackCount *= (shad != null ? 2 : 1);

        slea.skip(4); //?idk
        final int speed = slea.readByte();
        final int slot = slea.readShort();
        final int csstar = slea.readShort();
        int visProjectile = 0;
        if ((chr.getJob() >= 3500 && chr.getJob() <= 3512) || GameConstants.isJett(chr.getJob())) {
            visProjectile = 2333000;
        } else if (GameConstants.isCannon(chr.getJob())) {
            visProjectile = 2333001;
        } else if (!GameConstants.isMercedes(chr.getJob()) && chr.getBuffedValue(MapleBuffStat.SOULARROW) == null && slot > 0) {
            Item ipp = chr.getInventory(MapleInventoryType.USE).getItem((short) slot);
            if (ipp == null) {
                return;
            }
            if (csstar > 0) {
                ipp = chr.getInventory(MapleInventoryType.CASH).getItem((short) csstar);
                if (ipp == null) {
                    return;
                }
            }
            visProjectile = ipp.getItemId();
        }
        maxdamage *= skillDamage / 100.0;
        maxdamage *= chr.getStat().dam_r / 100.0;
        final List<AttackPair> ourAttacks = new ArrayList<AttackPair>(mobCount);
        final boolean area = inArea(chr);
        boolean didAttack = false, killed = false;
        if (!area) {
            List<Pair<Integer, Boolean>> attacks;
            for (MapleCharacter attacked : chr.getMap().getCharactersIntersect(box)) {
                if (attacked.getId() != chr.getId() && attacked.isAlive() && !attacked.isHidden() && (type == 0 || attacked.getTeam() != chr.getTeam())) {
                    double rawDamage = maxdamage / Math.max(1, ((magic ? attacked.getStat().mdef : attacked.getStat().wdef) * Math.max(1.0, 100.0 - ignoreDEF) / 100.0) * (type == 3 ? 0.2 : 0.5));
                    if (attacked.getBuffedValue(MapleBuffStat.INVINCIBILITY) != null || inArea(attacked)) {
                        rawDamage = 0;
                    }
                    rawDamage *= attacked.getStat().mesoGuard / 100.0;
                    rawDamage += (rawDamage * chr.getDamageIncrease(attacked.getId()) / 100.0);
                    rawDamage = attacked.modifyDamageTaken(rawDamage, attacked).left;
                    final double min = (rawDamage * chr.getStat().trueMastery / 100.0);
                    attacks = new ArrayList<Pair<Integer, Boolean>>(attackCount);
                    int totalMPLoss = 0, totalHPLoss = 0;
                    ThreadLock.lock();
                    try {
                        for (int i = 0; i < attackCount; i++) {
                            boolean critical_ = false;
                            int mploss = 0;
                            double ourDamage = Randomizer.nextInt((int) Math.abs(Math.round(rawDamage - min)) + 2) + min;
                            if (attacked.getStat().dodgeChance > 0 && Randomizer.nextInt(100) < attacked.getStat().dodgeChance) {
                                ourDamage = 0;
                            } else if (attacked.hasDisease(MapleDisease.DARKNESS) && Randomizer.nextInt(100) < 50) {
                                ourDamage = 0;
                                //i dont think level actually matters or it'd be too op
                                //} else if (attacked.getLevel() > chr.getLevel() && Randomizer.nextInt(100) < (attacked.getLevel() - chr.getLevel())) {
                                //	ourDamage = 0;
                            } else if (attacked.getJob() == 122 && attacked.getTotalSkillLevel(1220006) > 0 && attacked.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) != null) {
                                final MapleStatEffect eff = SkillFactory.getSkill(1220006).getEffect(attacked.getTotalSkillLevel(1220006)); //究極神盾
                                if (eff.makeChanceResult()) {
                                    ourDamage = 0;
                                }
                            } else if (attacked.getJob() == 412 && attacked.getTotalSkillLevel(4120002) > 0) { //瞬身迴避
                                final MapleStatEffect eff = SkillFactory.getSkill(4120002).getEffect(attacked.getTotalSkillLevel(4120002));
                                if (eff.makeChanceResult()) {
                                    ourDamage = 0;
                                }
                            } else if (attacked.getJob() == 422 && attacked.getTotalSkillLevel(4220002) > 0) { //瞬身迴避
                                final MapleStatEffect eff = SkillFactory.getSkill(4220002).getEffect(attacked.getTotalSkillLevel(4220002));
                                if (eff.makeChanceResult()) {
                                    ourDamage = 0;
                                }
                            } else if (shad != null && i >= originalAttackCount) {
                                ourDamage *= shad.getX() / 100.0;
                            }

                            // 暴風雪箭 楓幣炸彈 隱??鎖鏈地獄
                            if (ourDamage > 0 && skillid != 3211003 && skillid != 4211006 && (skillid == 4221001 || skillid == 4331006 || Randomizer.nextInt(100) < critRate)) {
                                ourDamage *= (100.0 + (Randomizer.nextInt(Math.max(2, chr.getStat().passive_sharpeye_percent() - chr.getStat().passive_sharpeye_min_percent())) + chr.getStat().passive_sharpeye_min_percent())) / 100.0;
                                critical_ = true;
                            }
                            if (attacked.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
                                mploss = (int) Math.min(attacked.getStat().getMp(), (ourDamage * attacked.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0));
                            }
                            ourDamage -= mploss;
                            if (attacked.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                                mploss = 0;
                            }
                            attacks.add(new Pair<Integer, Boolean>((int) Math.floor(ourDamage), critical_));

                            totalHPLoss += Math.floor(ourDamage);
                            totalMPLoss += mploss;
                        }
                    } finally {
                        ThreadLock.unlock();
                    }
                    if (GameConstants.isDemon(chr.getJob())) {
                        chr.handleForceGain(attacked.getObjectId(), skillid);
                    }
                    addedScore += Math.min(attacked.getStat().getHp() / 100, (totalHPLoss / 100) + (totalMPLoss / 100)); //ive NO idea
                    attacked.addMPHP(-totalHPLoss, -totalMPLoss);
                    ourAttacks.add(new AttackPair(attacked.getId(), attacked.getPosition(), attacks));
                    chr.onAttack(attacked.getStat().getCurrentMaxHp(), attacked.getStat().getCurrentMaxMp(attacked.getJob()), skillid, attacked.getObjectId(), totalHPLoss);

                    if (totalHPLoss > 0) {
                        didAttack = true;
                    }
                    if (attacked.getStat().getHPPercent() <= 20) {
                        SkillFactory.getSkill(attacked.getStat().getSkillByJob(93, attacked.getJob())).getEffect(1).applyTo(attacked);
                    }
                    if (effect != null) {
                        if (effect.getMonsterStati().size() > 0 && effect.makeChanceResult()) {
                            ThreadLock.lock();
                            try {
                                for (Map.Entry<MonsterStatus, Integer> z : effect.getMonsterStati().entrySet()) {
                                    MapleDisease d = MonsterStatus.getLinkedDisease(z.getKey());
                                    if (d != null) {
                                        attacked.giveDebuff(d, z.getValue(), effect.getDuration(), d.getDisease(), 1);
                                    }
                                }
                            } finally {
                                ThreadLock.unlock();
                            }
                        }
                        effect.handleExtraPVP(chr, attacked);
                    }
                    if (chr.getJob() == 121 || chr.getJob() == 122 || chr.getJob() == 2110 || chr.getJob() == 2111 || chr.getJob() == 2112) { // WHITEKNIGHT
                        if (chr.getBuffSource(MapleBuffStat.WK_CHARGE) == 1211006 || chr.getBuffSource(MapleBuffStat.WK_CHARGE) == 21101006) { //寒冰之劍
                            final MapleStatEffect eff = chr.getStatForBuff(MapleBuffStat.WK_CHARGE);
                            if (eff.makeChanceResult()) {
                                attacked.giveDebuff(MapleDisease.FREEZE, 1, eff.getDuration(), MapleDisease.FREEZE.getDisease(), 1);
                            }
                        }
                    } else if (chr.getBuffedValue(MapleBuffStat.SLOW) != null) {
                        final MapleStatEffect eff = chr.getStatForBuff(MapleBuffStat.SLOW);
                        if (eff != null && eff.makeChanceResult()) {
                            attacked.giveDebuff(MapleDisease.SLOW, 100 - Math.abs(eff.getX()), eff.getDuration(), MapleDisease.SLOW.getDisease(), 1);
                        }
                    } else if (chr.getJob() == 411 || chr.getJob() == 412 || chr.getJob() == 421 || chr.getJob() == 422 || chr.getJob() == 432 || chr.getJob() == 433 || chr.getJob() == 434 || chr.getJob() == 1411 || chr.getJob() == 1412) {

                        //飛毒殺
                        int[] skills = {4110011, 4120011, 4210010, 4220011, 4320005, 4340012, 14110004};
                        ThreadLock.lock();
                        try {
                            for (int i : skills) {
                                final Skill skill = SkillFactory.getSkill(i);
                                if (chr.getTotalSkillLevel(skill) > 0) {
                                    final MapleStatEffect venomEffect = skill.getEffect(chr.getTotalSkillLevel(skill));
                                    if (venomEffect.makeChanceResult()) {// THIS MIGHT ACTUALLY BE THE DOT
                                        attacked.giveDebuff(MapleDisease.POISON, 1, venomEffect.getDuration(), MapleDisease.POISON.getDisease(), 1);
                                    }
                                    break;
                                }
                            }
                        } finally {
                            ThreadLock.unlock();
                        }
                    }
                    if ((chr.getJob() / 100) % 10 == 2) {

                        //自然力變弱 魔法強化
                        int[] skills = {2000007, 12000006, 22000002, 32000012};
                        ThreadLock.lock();
                        try {
                            for (int i : skills) {
                                final Skill skill = SkillFactory.getSkill(i);
                                if (chr.getTotalSkillLevel(skill) > 0) {
                                    final MapleStatEffect venomEffect = skill.getEffect(chr.getTotalSkillLevel(skill));
                                    if (venomEffect.makeChanceResult()) {
                                        venomEffect.applyTo(attacked);
                                    }
                                    break;
                                }
                            }
                        } finally {
                            ThreadLock.unlock();
                        }
                    }
                    if (ice == attacked.getId()) {
                        chr.getClient().getSession().write(CField.getPVPIceHPBar(attacked.getStat().getHp(), attacked.getStat().getCurrentMaxHp()));
                    } else {
                        chr.getClient().getSession().write(CField.getPVPHPBar(attacked.getId(), attacked.getStat().getHp(), attacked.getStat().getCurrentMaxHp()));
                    }

                    if (!attacked.isAlive()) {
                        addedScore += 5; //i guess
                        killed = true;
                    }
                    if (ourAttacks.size() >= mobCount) {
                        break;
                    }
                }
            }
        } else if (type == 3) {
            if (Integer.parseInt(chr.getEventInstance().getProperty("redflag")) == chr.getId() && chr.getMap().getArea(1).contains(chr.getTruePosition())) {
                chr.getEventInstance().setProperty("redflag", "0");
                chr.getEventInstance().setProperty("blue", String.valueOf(Integer.parseInt(chr.getEventInstance().getProperty("blue")) + 1));
                chr.getEventInstance().broadcastPlayerMsg(-7, "Blue Team has scored a point!");
                chr.getMap().spawnAutoDrop(2910000, chr.getMap().getGuardians().get(0).left);
                chr.getEventInstance().broadcastPacket(CField.getCapturePosition(chr.getMap()));
                chr.getEventInstance().broadcastPacket(CField.resetCapture());
                chr.getEventInstance().schedule("updateScoreboard", 1000);
            } else if (Integer.parseInt(chr.getEventInstance().getProperty("blueflag")) == chr.getId() && chr.getMap().getArea(0).contains(chr.getTruePosition())) {
                chr.getEventInstance().setProperty("blueflag", "0");
                chr.getEventInstance().setProperty("red", String.valueOf(Integer.parseInt(chr.getEventInstance().getProperty("red")) + 1));
                chr.getEventInstance().broadcastPlayerMsg(-7, "Red Team has scored a point!");
                chr.getMap().spawnAutoDrop(2910001, chr.getMap().getGuardians().get(1).left);
                chr.getEventInstance().broadcastPacket(CField.getCapturePosition(chr.getMap()));
                chr.getEventInstance().broadcastPacket(CField.resetCapture());
                chr.getEventInstance().schedule("updateScoreboard", 1000);
            }
        }
        if (chr.getEventInstance() == null) { //if the PVP ends
            c.getSession().write(CWvsContext.enableActions());
            return;
        }

        if (killed || addedScore > 0) {
            chr.getEventInstance().addPVPScore(chr, addedScore);
            chr.getClient().getSession().write(CField.getPVPScore(ourScore + addedScore, killed));
        }
        if (didAttack) {
            chr.afterAttack(ourAttacks.size(), attackCount, skillid);
            PlayerHandler.AranCombo(c, chr, ourAttacks.size() * attackCount);
            if (skillid > 0 && (ourAttacks.size() > 0 || skillid != 4341002) && !GameConstants.isNoDelaySkill(skillid)) { //絕殺刃
                effect.applyTo(chr, chr.getTruePosition());
            } else {
                c.getSession().write(CWvsContext.enableActions());
            }
        } else {
            move = false;
            pull = false;
            push = false;
            c.getSession().write(CWvsContext.enableActions());
        }
        chr.getMap().broadcastMessage(CField.pvpAttack(chr.getId(), chr.getLevel(), trueSkill, trueSkillLevel, speed, fakeMastery, visProjectile, attackCount, chargeTime, animation, facingLeft ? 1 : 0, chr.getStat().defRange, skillid, skillLevel, move, push, pull, ourAttacks));
    }

    public static boolean inArea(MapleCharacter chr) {
        for (Rectangle rect : chr.getMap().getAreas()) {
            if (rect.contains(chr.getTruePosition())) {
                return true;
            }
        }
        for (MapleMist mist : chr.getMap().getAllMistsThreadsafe()) {
            if (mist.getOwnerId() == chr.getId() && mist.isPoisonMist() == 2 && mist.getBox().contains(chr.getTruePosition())) {
                return true;
            }
        }
        return false;
    }

    /*
    連結技能相關設定
    */
    public static void TeachSkill(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null) || (chr.hasBlockedInventory())) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (chr.getLevel() < 70) {
            chr.dropMessage(1, "After reaching level 70, you can choose a role in the same server to teach this skill.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int skillId = slea.readInt();
        if (chr.getSkillLevel(skillId) < 1) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int toChrId = slea.readInt();
        Pair toChrInfo = MapleCharacterUtil.getNameById(toChrId, 0);
        if (toChrInfo == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int toChrAccId = ((Integer) toChrInfo.getRight()).intValue();
        String toChrName = (String) toChrInfo.getLeft();
        MapleQuest quest = MapleQuest.getInstance(7783);
        if ((quest != null) && (chr.getAccountID() == toChrAccId)) {
            int toSkillId;
            if (GameConstants.isCannon(chr.getJob())) {
                toSkillId = 80000000;
            } else {
                if (GameConstants.isDemon(chr.getJob())) {
                    toSkillId = 80000001;
                } else {
                    if (GameConstants.isPhantom(chr.getJob())) {
                        toSkillId = 80000002;
                    } else {
                        if (GameConstants.isMercedes(chr.getJob())) {
                            toSkillId = 80001040;
                        } else {
                            if (GameConstants.isMihile(chr.getJob())) {
                                toSkillId = 80001140;
                            } else {
                                if (GameConstants.isJett(chr.getJob())) {
                                    toSkillId = 80001151;
                                } else {
                                    chr.dropMessage(1, "Failed to impart skills.");
                                    c.getSession().write(CWvsContext.enableActions());
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            if ((chr.teachSkill(toSkillId, toChrId) > 0) && (toSkillId >= 80000000)) {
                chr.changeTeachSkill(skillId, toChrId);
                quest.forceComplete(chr, 0);
                c.getSession().write(CWvsContext.teachMessage(skillId, toChrId, toChrName));
            } else {
                chr.dropMessage(1, new StringBuilder().append("Failed to impart skills, [").append(toChrName).append("] The character has already acquired this skill").toString());
            }
        } else {
            chr.dropMessage(1, "Failed to impart skills.");
        }
        c.getSession().write(CWvsContext.enableActions());
    }

    public static void UsePtExpPotion(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        final MapleCharacter user = c.getPlayer();
        short slot = slea.readShort();
        int itemId = slea.readInt();
        Item toUse = user.getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (toUse.getItemId() / 10000 != 223) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (toUse.getQuantity() < 1) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (toUse.getItemId() != itemId) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        while (user.getLevel() < 18) {
            user.levelUp();
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        c.getSession().write(CWvsContext.enableActions());
    }
}