package server;

import client.MapleTrait.MapleTraitType;
import client.*;
import client.inventory.EquipAdditions.RingSet;
import client.inventory.*;
import constants.GameConstants;

import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import scripting.EventManager;
import scripting.NPCScriptManager;
import server.maps.AramiaFireWorks;
import server.quest.MapleQuest;
import tools.StringUtil;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.MTSCSPacket;

public class MapleInventoryManipulator {

    public static void addRing(MapleCharacter chr, int itemId, int ringId, int sn, String partner) {
        CashItemInfo csi = CashItemFactory.getInstance().getItem(sn);
        if (csi == null) {
            return;
        }
        Item ring = chr.getCashInventory().toItem(csi, ringId);
        if (ring == null || ring.getUniqueId() != ringId || ring.getUniqueId() <= 0 || ring.getItemId() != itemId) {
            return;
        }
        chr.getCashInventory().addToInventory(ring);
        chr.getClient().getSession().write(MTSCSPacket.sendBoughtRings(GameConstants.isCrushRing(itemId), ring, sn, chr.getClient().getAccID(), partner));
    }

    public static boolean addbyItem(final MapleClient c, final Item item) {
        return addbyItem(c, item, false) >= 0;
    }

    public static short addbyItem(final MapleClient c, final Item item, final boolean fromcs) {
        final MapleInventoryType type = GameConstants.getInventoryType(item.getItemId());
        final short newSlot = c.getPlayer().getInventory(type).addItem(item);
        if (newSlot == -1) {
            if (!fromcs) {
                c.getSession().write(InventoryPacket.getInventoryFull());
                c.getSession().write(InventoryPacket.getShowInventoryFull());
            }
            return newSlot;
        }
        if (GameConstants.isHarvesting(item.getItemId())) {
            c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
        }
        c.getSession().write(InventoryPacket.addInventorySlot(type, item));
        c.getPlayer().havePartyQuest(item.getItemId());
        return newSlot;
    }

    public static int getUniqueId(int itemId, MaplePet pet) {
        int uniqueid = -1;
        if (GameConstants.isPet(itemId)) {
            if (pet != null) {
                uniqueid = pet.getUniqueId();
            } else {
                uniqueid = MapleInventoryIdentifier.getInstance();
            }
        } else if (GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH || MapleItemInformationProvider.getInstance().isCash(itemId)) { //less work to do
            uniqueid = MapleInventoryIdentifier.getInstance(); //shouldnt be generated yet, so put it here
        }
        return uniqueid;
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String gmLog) {
        return addById(c, itemId, quantity, null, null, 0, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, String gmLog) {
        return addById(c, itemId, quantity, owner, null, 0, gmLog);
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, String gmLog) {
        return addId(c, itemId, quantity, owner, null, 0, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, String gmLog) {
        return addById(c, itemId, quantity, owner, pet, 0, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, String gmLog) {
        return addId(c, itemId, quantity, owner, pet, period, gmLog) >= 0;
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, String gmLog) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        /*        if ((ii.isPickupRestricted(itemId) && c.getPlayer().haveItem(itemId, 1, true, false)) || (!ii.itemExists(itemId))) {
         c.getSession().write(InventoryPacket.getInventoryFull());
         c.getSession().write(InventoryPacket.showItemUnavailable());
         return -1;
         }
         * 
         */
        final MapleInventoryType type = GameConstants.getInventoryType(itemId);
        int uniqueid = getUniqueId(itemId, pet);
        short newSlot = -1;
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(itemId);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);
            if (!GameConstants.isRechargable(itemId)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = (Item) i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null) && eItem.getExpiration() == -1) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.getSession().write(InventoryPacket.updateInventorySlot(type, eItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }
                Item nItem;
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0, uniqueid);
                        newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1) {
                            c.getSession().write(InventoryPacket.getInventoryFull());
                            c.getSession().write(InventoryPacket.getShowInventoryFull());
                            return -1;
                        }
                        if (gmLog != null) {
                            nItem.setGMLog(gmLog);
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        if (period > 0) {
                            nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                        }
                        if (pet != null) {
                            nItem.setPet(pet);
                            pet.setInventoryPosition(newSlot);
                            c.getPlayer().addPet(pet);
                        }
                        c.getSession().write(InventoryPacket.addInventorySlot(type, nItem));
                        if (GameConstants.isRechargable(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        c.getPlayer().havePartyQuest(itemId);
                        c.getSession().write(CWvsContext.enableActions());
                        return (byte) newSlot;
                    }
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0, uniqueid);
                newSlot = c.getPlayer().getInventory(type).addItem(nItem);

                if (newSlot == -1) {
                    c.getSession().write(InventoryPacket.getInventoryFull());
                    c.getSession().write(InventoryPacket.getShowInventoryFull());
                    return -1;
                }
                if (period > 0) {
                    nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                if (gmLog != null) {
                    nItem.setGMLog(gmLog);
                }
                c.getSession().write(InventoryPacket.addInventorySlot(type, nItem));
                c.getSession().write(CWvsContext.enableActions());
            }
        } else {
            if (quantity == 1) {
                final Item nEquip = ii.getEquipById(itemId, uniqueid);
                if (owner != null) {
                    nEquip.setOwner(owner);
                }
                if (gmLog != null) {
                    nEquip.setGMLog(gmLog);
                }
                if (period > 0) {
                    nEquip.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    c.getSession().write(InventoryPacket.getInventoryFull());
                    c.getSession().write(InventoryPacket.getShowInventoryFull());
                    return -1;
                }
                c.getSession().write(InventoryPacket.addInventorySlot(type, nEquip));
                if (GameConstants.isHarvesting(itemId)) {
                    c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
                }
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        c.getPlayer().havePartyQuest(itemId);
        return (byte) newSlot;
    }

    public static Item addbyId_Gachapon(final MapleClient c, final int itemId, short quantity) {
        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.USE).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.ETC).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNextFreeSlot() == -1) {
            return null;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        /*        if ((ii.isPickupRestricted(itemId) && c.getPlayer().haveItem(itemId, 1, true, false)) || (!ii.itemExists(itemId))) {
         c.getSession().write(InventoryPacket.getInventoryFull());
         c.getSession().write(InventoryPacket.showItemUnavailable());
         return null;
         }
         * 
         */
        final MapleInventoryType type = GameConstants.getInventoryType(itemId);

        if (!type.equals(MapleInventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(itemId);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);

            if (!GameConstants.isRechargable(itemId)) {
                Item nItem = null;
                boolean recieved = false;

                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            nItem = (Item) i.next();
                            short oldQ = nItem.getQuantity();

                            if (oldQ < slotMax) {
                                recieved = true;

                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                nItem.setQuantity(newQ);
                                c.getSession().write(InventoryPacket.updateInventorySlot(type, nItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0);
                        final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1 && recieved) {
                            return nItem;
                        } else if (newSlot == -1) {
                            return null;
                        }
                        recieved = true;
                        c.getSession().write(InventoryPacket.addInventorySlot(type, nItem));
                        if (GameConstants.isRechargable(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (recieved) {
                    c.getPlayer().havePartyQuest(nItem.getItemId());
                    return nItem;
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0);
                final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);

                if (newSlot == -1) {
                    return null;
                }
                c.getSession().write(InventoryPacket.addInventorySlot(type, nItem));
                c.getPlayer().havePartyQuest(nItem.getItemId());
                return nItem;
            }
        } else {
            if (quantity == 1) {
                final Item item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                final short newSlot = c.getPlayer().getInventory(type).addItem(item);

                if (newSlot == -1) {
                    return null;
                }
                c.getSession().write(InventoryPacket.addInventorySlot(type, item, true));
                c.getPlayer().havePartyQuest(item.getItemId());
                return item;
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        return null;
    }

    /*
    道具拾取相關設定
    */
    public static boolean addFromDrop(final MapleClient c, final Item item, final boolean show) {
        return addFromDrop(c, item, show, false);
    }

    public static boolean addFromDrop(final MapleClient c, Item item, final boolean show, final boolean enhance) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        // 異次元通行證 月妙的年糕 氣泡 新進海賊的證明 中級海賊的證明 資深海賊的證明 第三片小碎片 怪物珠 
        //if (item.getItemId() == 2430115 || item.getItemId() == 2430287  || item.getItemId() == 2430364 || item.getItemId() == 2430373 || item.getItemId() == 2430374 || item.getItemId() == 2430375 || item.getItemId() == 2430383 || item.getItemId() == 2430492) {
        //    NPCScriptManager.getInstance().startItemScript(c, 9010000, "" + item.getItemId());
        //    c.getSession().write(CWvsContext.enableActions());
        //    return false;
        //}

        final EventManager LudiPQ = c.getChannelServer().getEventSM().getEventManager("LudiPQ"); //遺棄之塔101

        if (item.getItemId() == 2430115 && LudiPQ != null && c.getPlayer().getEventInstance().getProperty("drop") == null) {

            c.getPlayer().getEventInstance().setProperty("count", c.getPlayer().getEventInstance().getProperty("count") == null ? "1" : Integer.parseInt(c.getPlayer().getEventInstance().getProperty("count")) + 1 + "");

            c.getPlayer().getMap().broadcastMessage(tools.packet.CWvsContext.getTopMsg("You have collected " + c.getPlayer().getEventInstance().getProperty("count") + " Passes."));

            if (c.getPlayer().getEventInstance().getProperty("count").equals("20")) {

                c.getPlayer().getMap().broadcastMessage(tools.packet.CWvsContext.getTopMsg("A portal to the next stage has opened."));
                c.getPlayer().getMap().startMapEffect("All of the passes have been gathered. Proceed to the next stage by talking to the Red Balloon.", 5120018);

                c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("gate", 2));
                c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("quest/party/clear", 3));
                c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("Party1/Clear", 4));

                c.getPlayer().getEventInstance().setProperty("stage1", "1");
                c.getPlayer().getEventInstance().setProperty("drop", "1");
            }
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }

        final EventManager HenesysPQ = c.getChannelServer().getEventSM().getEventManager("HenesysPQ"); //月妙年糕

        if (item.getItemId() == 2430287 && HenesysPQ != null && c.getPlayer().getEventInstance().getProperty("drop") == null) {

            c.getPlayer().getEventInstance().setProperty("count", c.getPlayer().getEventInstance().getProperty("count") == null ? "1" : Integer.parseInt(c.getPlayer().getEventInstance().getProperty("count")) + 1 + "");

            c.getPlayer().getMap().broadcastMessage(tools.packet.CWvsContext.getTopMsg("Moon Bunny's Rice Cakes x" + c.getPlayer().getEventInstance().getProperty("count") + " have been obtained."));

            if (c.getPlayer().getEventInstance().getProperty("count").equals("10")) {

                c.getPlayer().getMap().startMapEffect("You've gathered 10 Moon Bunny's Rice Cakes for me. Yum, yum! So tasty. Come see me again so l can give you a reward.", 5120016);
                c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("quest/party/clear", 3));
                c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("Party1/Clear", 4));

                for (MapleCharacter ddd : c.getPlayer().getMap().getCharactersThreadsafe()) {
                    if (ddd.getQuestNAdd(MapleQuest.getInstance(1200)).getStatus() < 2) {
                        MapleQuest.getInstance(1200).forceComplete(ddd, 0);
                        ddd.getClient().getSession().write(tools.packet.CWvsContext.getShowQuestCompletion(1200));
                    }
                    ddd.gainExp(ddd.getLevel() * 500, true, true, true);
                }

                c.getPlayer().getMap().killAllMonsters(true);
                c.getPlayer().getMap().removeDrops();
                c.getPlayer().getMap().setSpawns(false);
                c.getPlayer().getEventInstance().setProperty("drop", "1");
                HenesysPQ.setProperty("state", "2");
            }
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }

        final EventManager Kenta = c.getChannelServer().getEventSM().getEventManager("Kenta"); //陷入危險的坎特

        if (item.getItemId() == 2430364 && Kenta != null && c.getPlayer().getEventInstance().getProperty("drop") == null) {

            c.getPlayer().getEventInstance().setProperty("count", c.getPlayer().getEventInstance().getProperty("count") == null ? "1" : Integer.parseInt(c.getPlayer().getEventInstance().getProperty("count")) + 1 + "");

            c.getPlayer().getMap().broadcastMessage(tools.packet.CWvsContext.getTopMsg("Air Bubble " + c.getPlayer().getEventInstance().getProperty("count") + " have been obtained."));

            if (c.getPlayer().getEventInstance().getProperty("count").equals("20")) {

                c.getPlayer().getMap().startMapEffect("I...can't hold...my breath much longer. Get me some Air Bubbles!", 5120052);
                c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("quest/party/clear", 3));
                c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("Party1/Clear", 4));

                c.getPlayer().getMap().killAllMonsters(true);
                c.getPlayer().getMap().resetFully(false);
                c.getPlayer().getMap().setSpawns(false);
                c.getPlayer().getEventInstance().setProperty("drop", "1");
            }
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }

        final EventManager Pirate = c.getChannelServer().getEventSM().getEventManager("Pirate"); //海盜船

        if ((item.getItemId() == 2430373 || item.getItemId() == 2430374 || item.getItemId() == 2430375) && Pirate != null && c.getPlayer().getEventInstance().getProperty("drop") == null) {

            if (item.getItemId() == 2430373) { //新進海賊的證明
                c.getPlayer().getEventInstance().setProperty("count0", c.getPlayer().getEventInstance().getProperty("count0") == null ? "1" : Integer.parseInt(c.getPlayer().getEventInstance().getProperty("count0")) + 1 + "");
                c.getPlayer().getMap().broadcastMessage(tools.packet.CWvsContext.getTopMsg("Rookie Pirate Mark " + c.getPlayer().getEventInstance().getProperty("count0") + " /10"));

                if (c.getPlayer().getEventInstance().getProperty("count0").equals("10")) {
                    c.getPlayer().getMap().startMapEffect("You've obtained all the Rookie Pirate Marks. Now you must find the Rising Pirate Marks.", 5120020);
                    c.getPlayer().getMap().killAllMonsters(true);
                    c.getPlayer().getEventInstance().setProperty("stage1a", "1");
                }
            }

            if (item.getItemId() == 2430374) { //中級海賊的證明
                c.getPlayer().getEventInstance().setProperty("count1", c.getPlayer().getEventInstance().getProperty("count1") == null ? "1" : Integer.parseInt(c.getPlayer().getEventInstance().getProperty("count1")) + 1 + "");
                c.getPlayer().getMap().broadcastMessage(tools.packet.CWvsContext.getTopMsg("Rising Pirate Mark " + c.getPlayer().getEventInstance().getProperty("count1") + " /10"));

                if (c.getPlayer().getEventInstance().getProperty("count1").equals("10")) {
                    c.getPlayer().getMap().startMapEffect("You've obtained all the Rising Pirate Marks. Now it's time the find the last marks.", 5120020);
                    c.getPlayer().getMap().killAllMonsters(true);
                    c.getPlayer().getEventInstance().setProperty("stage1a", "2");
                }
            }

            if (item.getItemId() == 2430375) { //資深海賊的證明
                c.getPlayer().getEventInstance().setProperty("count2", c.getPlayer().getEventInstance().getProperty("count2") == null ? "1" : Integer.parseInt(c.getPlayer().getEventInstance().getProperty("count2")) + 1 + "");
                c.getPlayer().getMap().broadcastMessage(tools.packet.CWvsContext.getTopMsg("Veteran Pirate Mark " + c.getPlayer().getEventInstance().getProperty("count2") + " /10"));

                if (c.getPlayer().getEventInstance().getProperty("count2").equals("10")) {
                    c.getPlayer().getMap().startMapEffect("You've obtained all the Veteran Pirate Marks. Now I will break the last seal. Please go through the portal.", 5120020);
                    c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("quest/party/clear", 3));
                    c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("Party1/Clear", 4));
                    c.getPlayer().getMap().killAllMonsters(true);
                    c.getPlayer().getEventInstance().setProperty("stage1", "1");
                    c.getPlayer().getEventInstance().setProperty("drop", "1");
                }
            }
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }

        final EventManager OrbisPQ = c.getChannelServer().getEventSM().getEventManager("OrbisPQ"); //雅典娜禁地

        if (item.getItemId() == 2430383 && OrbisPQ != null && c.getPlayer().getEventInstance().getProperty("drop") == null) {

            c.getPlayer().getEventInstance().setProperty("count", c.getPlayer().getEventInstance().getProperty("count") == null ? "1" : Integer.parseInt(c.getPlayer().getEventInstance().getProperty("count")) + 1 + "");

            c.getPlayer().getMap().broadcastMessage(tools.packet.CWvsContext.getTopMsg("4th Small Fragment " + c.getPlayer().getEventInstance().getProperty("count") + " / 30"));

            if (c.getPlayer().getEventInstance().getProperty("count").equals("30")) {

                c.getPlayer().getMap().startMapEffect("All of the small pieces have been obtained. Get the fourth piece from Chamberain Eak, and proceed to the center room.", 5120019);

                c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("quest/party/clear", 3));
                c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("Party1/Clear", 4));

                c.getPlayer().getEventInstance().setProperty("stage2a", "1");
                c.getPlayer().getEventInstance().setProperty("drop", "1");
            }
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }

        final EventManager Ellin = c.getChannelServer().getEventSM().getEventManager("Ellin"); //毒霧森林

        if (item.getItemId() == 2430492 && Ellin != null && c.getPlayer().getEventInstance().getProperty("drop") == null) {

            c.getPlayer().getEventInstance().setProperty("count", c.getPlayer().getEventInstance().getProperty("count") == null ? "1" : Integer.parseInt(c.getPlayer().getEventInstance().getProperty("count")) + 1 + "");

            c.getPlayer().getMap().broadcastMessage(tools.packet.CWvsContext.getTopMsg("Monster Marbles: " + c.getPlayer().getEventInstance().getProperty("count") + " / 20"));

            if (c.getPlayer().getEventInstance().getProperty("count").equals("20")) {

                c.getPlayer().getMap().startMapEffect("You have obtained all the Monster Marbles. Have your party leader talk to Ellin to move on to the next stage.", 5120023);
                c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("quest/party/clear", 3));
                c.getPlayer().getMap().broadcastMessage(tools.packet.CField.environmentChange("Party1/Clear", 4));

                c.getPlayer().getMap().killAllMonsters(true);
                c.getPlayer().getMap().removeDrops();
                c.getPlayer().getMap().setSpawns(false);
                c.getPlayer().getEventInstance().setProperty("drop", "1");
            }
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }

        /*        if (c.getPlayer() == null || (ii.isPickupRestricted(item.getItemId()) && c.getPlayer().haveItem(item.getItemId(), 1, true, false)) || (!ii.itemExists(item.getItemId()))) {
         c.getSession().write(InventoryPacket.getInventoryFull());
         c.getSession().write(InventoryPacket.showItemUnavailable());
         return false;
         }
         * 
         */
        final int before = c.getPlayer().itemQuantity(item.getItemId());
        short quantity = item.getQuantity();
        final MapleInventoryType type = GameConstants.getInventoryType(item.getItemId());

        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(item.getItemId());
            final List<Item> existing = c.getPlayer().getInventory(type).listById(item.getItemId());
            if (!GameConstants.isRechargable(item.getItemId())) {
                if (quantity <= 0) { //wth
                    c.getSession().write(InventoryPacket.getInventoryFull());
                    c.getSession().write(InventoryPacket.showItemUnavailable());
                    return false;
                }
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            final Item eItem = (Item) i.next();
                            final short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && item.getOwner().equals(eItem.getOwner()) && item.getExpiration() == eItem.getExpiration()) {
                                final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.getSession().write(InventoryPacket.updateInventorySlot(type, eItem, true));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    final short newQ = (short) Math.min(quantity, slotMax);
                    quantity -= newQ;
                    final Item nItem = new Item(item.getItemId(), (byte) 0, newQ, item.getFlag());
                    nItem.setExpiration(item.getExpiration());
                    nItem.setOwner(item.getOwner());
                    nItem.setPet(item.getPet());
                    nItem.setGMLog(item.getGMLog());
                    short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                    if (newSlot == -1) {
                        c.getSession().write(InventoryPacket.getInventoryFull());
                        c.getSession().write(InventoryPacket.getShowInventoryFull());
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    c.getSession().write(InventoryPacket.addInventorySlot(type, nItem, true));
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(item.getItemId(), (byte) 0, quantity, item.getFlag());
                nItem.setExpiration(item.getExpiration());
                nItem.setOwner(item.getOwner());
                nItem.setPet(item.getPet());
                nItem.setGMLog(item.getGMLog());
                final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.getSession().write(InventoryPacket.getInventoryFull());
                    c.getSession().write(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                c.getSession().write(InventoryPacket.addInventorySlot(type, nItem));
                c.getSession().write(CWvsContext.enableActions());
            }
        } else {
            if (quantity == 1) {
                if (enhance) {
                    item = checkEnhanced(item, c.getPlayer());
                }
                final short newSlot = c.getPlayer().getInventory(type).addItem(item);

                if (newSlot == -1) {
                    c.getSession().write(InventoryPacket.getInventoryFull());
                    c.getSession().write(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                c.getSession().write(InventoryPacket.addInventorySlot(type, item, true));
                if (GameConstants.isHarvesting(item.getItemId())) {
                    c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
                }
            } else {
                throw new RuntimeException("Trying to create equip with non-one quantity");
            }
        }
        if (before == 0) {
            switch (item.getItemId()) {
                case AramiaFireWorks.KEG_ID:
                    c.getPlayer().dropMessage(5, "You have gained a Powder Keg, you can give this in to Aramia of Henesys.");
                    break;
                case AramiaFireWorks.SUN_ID:
                    c.getPlayer().dropMessage(5, "You have gained a Warm Sun, you can give this in to Maple Tree Hill through @joyce.");
                    break;
                case AramiaFireWorks.DEC_ID:
                    c.getPlayer().dropMessage(5, "You have gained a Tree Decoration, you can give this in to White Christmas Hill through @joyce.");
                    break;
            }
        }
        c.getPlayer().havePartyQuest(item.getItemId());
        if (show) {
            c.getSession().write(InfoPacket.getShowItemGain(item.getItemId(), item.getQuantity()));
        }
        return true;
    }

    private static Item checkEnhanced(final Item before, final MapleCharacter chr) {
        if (before instanceof Equip) {
            final Equip eq = (Equip) before;
            if (eq.getState() == 0 && (eq.getUpgradeSlots() >= 1 || eq.getLevel() >= 1) && GameConstants.canScroll(eq.getItemId()) && Randomizer.nextInt(100) >= 80) { //20% chance of pot?
                eq.resetPotential();
            }
        }
        return before;
    }

    public static boolean checkSpace(final MapleClient c, final int itemid, int quantity, final String owner) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        /*        if (c.getPlayer() == null || (ii.isPickupRestricted(itemid) && c.getPlayer().haveItem(itemid, 1, true, false)) || (!ii.itemExists(itemid))) {
         c.getSession().write(CWvsContext.enableActions());
         return false;
         }
         * 
         */
        if (quantity <= 0 && !GameConstants.isRechargable(itemid)) {
            return false;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemid);
        if (c == null || c.getPlayer() == null || c.getPlayer().getInventory(type) == null) { //wtf is causing this?
            return false;
        }
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(itemid);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemid);
            if (!GameConstants.isRechargable(itemid)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    for (Item eItem : existing) {
                        final short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner != null && owner.equals(eItem.getOwner())) {
                            final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            // add new slots if there is still something left
            final int numSlotsNeeded;
            if (slotMax > 0 && !GameConstants.isRechargable(itemid)) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else {
                numSlotsNeeded = 1;
            }
            return !c.getPlayer().getInventory(type).isFull(numSlotsNeeded - 1);
        } else {
            return !c.getPlayer().getInventory(type).isFull();
        }
    }

    public static boolean removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot, final short quantity, final boolean fromDrop) {
        return removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static boolean removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot, short quantity, final boolean fromDrop, final boolean consume) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        final Item item = c.getPlayer().getInventory(type).getItem(slot);

        if (item != null) {
            final boolean allowZero = consume && GameConstants.isRechargable(item.getItemId());
            c.getPlayer().getInventory(type).removeItem(slot, quantity, allowZero);
            if (GameConstants.isHarvesting(item.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }

            if (item.getQuantity() == 0 && !allowZero) {
                c.getSession().write(InventoryPacket.clearInventoryItem(type, item.getPosition(), fromDrop));
            } else {
                c.getSession().write(InventoryPacket.updateInventorySlot(type, (Item) item, fromDrop));
            }
            return true;
        }
        return false;
    }

    public static boolean removeById(final MapleClient c, final MapleInventoryType type, final int itemId, final int quantity, final boolean fromDrop, final boolean consume) {
        int remremove = quantity;
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            int theQ = item.getQuantity();
            if (remremove <= theQ && removeFromSlot(c, type, item.getPosition(), (short) remremove, fromDrop, consume)) {
                remremove = 0;
                break;
            } else if (remremove > theQ && removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume)) {
                remremove -= theQ;
            }
        }
        return remremove <= 0;
    }

    public static boolean removeFromSlot_Lock(final MapleClient c, final MapleInventoryType type, final short slot, short quantity, final boolean fromDrop, final boolean consume) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        final Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item != null) {
            if (ItemFlag.LOCK.check(item.getFlag()) || ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                return false;
            }
            return removeFromSlot(c, type, slot, quantity, fromDrop, consume);
        }
        return false;
    }

    public static boolean removeById_Lock(final MapleClient c, final MapleInventoryType type, final int itemId) {
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            if (removeFromSlot_Lock(c, type, item.getPosition(), (short) 1, false, false)) {
                return true;
            }
        }
        return false;
    }

    public static void move(final MapleClient c, final MapleInventoryType type, final short src, final short dst) {
        if (src < 0 || dst < 0 || src == dst || type == MapleInventoryType.EQUIPPED) {
            return;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Item source = c.getPlayer().getInventory(type).getItem(src);
        final Item initialTarget = c.getPlayer().getInventory(type).getItem(dst);
        if (source == null) {
            return;
        }
        boolean bag = false, switchSrcDst = false, bothBag = false;
        short eqIndicator = -1;
        if (dst > c.getPlayer().getInventory(type).getSlotLimit()) {
            if (type == MapleInventoryType.ETC && dst > 100 && dst % 100 != 0) {
                final int eSlot = c.getPlayer().getExtendedSlot((dst / 100) - 1);
                if (eSlot > 0) {
                    final MapleStatEffect ee = ii.getItemEffect(eSlot);
                    if (dst % 100 > ee.getSlotCount() || ee.getType() != ii.getBagType(source.getItemId()) || ee.getType() <= 0) {
                        c.getPlayer().dropMessage(1, "You may not move that item to the bag.");
                        c.getSession().write(CWvsContext.enableActions());
                        return;
                    } else {
                        eqIndicator = 0;
                        bag = true;
                    }
                } else {
                    c.getPlayer().dropMessage(1, "You may not move it to that bag.");
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
            } else {
                c.getPlayer().dropMessage(1, "You may not move it there.");
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
        }
        if (src > c.getPlayer().getInventory(type).getSlotLimit() && type == MapleInventoryType.ETC && src > 100 && src % 100 != 0) {
            //source should be not null so not much checks are needed
            if (!bag) {
                switchSrcDst = true;
                eqIndicator = 0;
                bag = true;
            } else {
                bothBag = true;
            }
        }
        short olddstQ = -1;
        if (initialTarget != null) {
            olddstQ = initialTarget.getQuantity();
        }
        final short oldsrcQ = source.getQuantity();
        final short slotMax = ii.getSlotMax(source.getItemId());
        c.getPlayer().getInventory(type).move(src, dst, slotMax);
        if (GameConstants.isHarvesting(source.getItemId())) {
            c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
        }
        if (!type.equals(MapleInventoryType.EQUIP) && initialTarget != null
                && initialTarget.getItemId() == source.getItemId()
                && initialTarget.getOwner().equals(source.getOwner())
                && initialTarget.getExpiration() == source.getExpiration()
                && !GameConstants.isRechargable(source.getItemId())
                && !type.equals(MapleInventoryType.CASH)) {
            if (GameConstants.isHarvesting(initialTarget.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }
            if ((olddstQ + oldsrcQ) > slotMax) {
                c.getSession().write(InventoryPacket.moveAndMergeWithRestInventoryItem(type, src, dst, (short) ((olddstQ + oldsrcQ) - slotMax), slotMax, bag, switchSrcDst, bothBag));
            } else {
                c.getSession().write(InventoryPacket.moveAndMergeInventoryItem(type, src, dst, ((Item) c.getPlayer().getInventory(type).getItem(dst)).getQuantity(), bag, switchSrcDst, bothBag));
            }
        } else {
            if (c.getPlayer().isGM()) {
                c.getPlayer().dropMessage(6, "ITEM ID " + source.getItemId());
            }
            c.getSession().write(InventoryPacket.moveInventoryItem(type, switchSrcDst ? dst : src, switchSrcDst ? src : dst, eqIndicator, bag, bothBag));
        }
    }

    /*
    裝備道具相關設定
    */
    public static void equip(final MapleClient c, final short src, short dst) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleCharacter chr = c.getPlayer();
        if (chr == null || (GameConstants.GMS && dst == -55)) {
            return;
        }
        final PlayerStats statst = c.getPlayer().getStat();
        Equip source = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(src);
        Equip target;

        if (source == null || source.getDurability() == 0 || GameConstants.isHarvesting(source.getItemId())) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }

        final Map<String, Integer> stats = ii.getEquipStats(source.getItemId());

        if (stats == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (dst > -1200 && dst < -999 && !GameConstants.isEvanDragonItem(source.getItemId()) && !GameConstants.isMechanicItem(source.getItemId())) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        } else if (((dst <= -1200 && dst > -5000) || (dst >= -999 && dst < -99)) && !stats.containsKey("cash")) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        } else if ((dst <= -1300 && dst > -5000) && c.getPlayer().getAndroid() == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        //  if (!ii.canEquip(stats, source.getItemId(), chr.getLevel(), chr.getJob(), chr.getFame(), statst.getTotalStr(), statst.getTotalDex(), statst.getTotalLuk(), statst.getTotalInt())) {
        ////    c.getSession().write(CWvsContext.enableActions());
        //    return;
        // }
        if (GameConstants.isWeapon(source.getItemId()) && dst != -10 && dst != -11) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (dst == (GameConstants.GMS ? -18 : -23) && !GameConstants.isMountItemAvailable(source.getItemId(), c.getPlayer().getJob())) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (dst == (GameConstants.GMS ? -118 : -123) && source.getItemId() / 10000 != 190) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (dst == (GameConstants.GMS ? -59 : -55)) { //pendant
            MapleQuestStatus stat = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
            if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < System.currentTimeMillis()) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
        }
        if (GameConstants.isKatara(source.getItemId()) || source.getItemId() / 10000 == 135 || source.getItemId() == 1098000) {
            dst = (byte) -10; //shield slot
        }

        if (GameConstants.isEvanDragonItem(source.getItemId()) && (chr.getJob() < 2200 || chr.getJob() > 2218)) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }

        if (GameConstants.isMechanicItem(source.getItemId()) && (chr.getJob() < 3500 || chr.getJob() > 3512)) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }

        if (source.getItemId() / 1000 == 1112) { //ring
            for (RingSet s : RingSet.values()) {
                if (s.id.contains(Integer.valueOf(source.getItemId()))) {
                    List<Integer> theList = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).listIds();
                    for (Integer i : s.id) {
                        if (theList.contains(i)) {
                            c.getPlayer().dropMessage(1, "You may not equip this item because you already have a " + (StringUtil.makeEnumHumanReadable(s.name())) + " equipped.");
                            c.getSession().write(CWvsContext.enableActions());
                            return;
                        }
                    }
                }
            }
        }

        switch (dst) {
            case -6: { // Top
                final Item top = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -5);
                if (top != null && GameConstants.isOverall(top.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getSession().write(InventoryPacket.getInventoryFull());
                        c.getSession().write(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -5: {
                final Item top = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -5);
                final Item bottom = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -6);
                if (top != null && GameConstants.isOverall(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull(bottom != null && GameConstants.isOverall(source.getItemId()) ? 1 : 0)) {
                        c.getSession().write(InventoryPacket.getInventoryFull());
                        c.getSession().write(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                if (bottom != null && GameConstants.isOverall(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getSession().write(InventoryPacket.getInventoryFull());
                        c.getSession().write(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -6, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -10: { // Shield
                Item weapon = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                if (GameConstants.isKatara(source.getItemId())) {
                    if ((chr.getJob() != 900 && (chr.getJob() < 430 || chr.getJob() > 434)) || weapon == null || !GameConstants.isDagger(weapon.getItemId())) {
                        c.getSession().write(InventoryPacket.getInventoryFull());
                        c.getSession().write(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                } else if (weapon != null && GameConstants.isTwoHanded(weapon.getItemId()) && !GameConstants.isJett(chr.getJob())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getSession().write(InventoryPacket.getInventoryFull());
                        c.getSession().write(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -11, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -11: { // Weapon
                Item shield = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
                if (shield != null && GameConstants.isTwoHanded(source.getItemId()) && !GameConstants.isJett(chr.getJob())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getSession().write(InventoryPacket.getInventoryFull());
                        c.getSession().write(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -10, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
        }
        source = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(src); // Equip
        target = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst); // Currently equipping
        if (source == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        short flag = source.getFlag();
        if (stats.get("equipTradeBlock") != null || source.getItemId() / 10000 == 167) { // Block trade when equipped.
            if (!ItemFlag.UNTRADEABLE.check(flag)) {
                flag |= ItemFlag.UNTRADEABLE.getValue();
                source.setFlag(flag);
                c.getSession().write(InventoryPacket.updateSpecialItemUse_(source, MapleInventoryType.EQUIP.getType(), c.getPlayer()));
            }
        }
        if (source.getItemId() / 10000 == 166) { //佩戴机器人
            Equip target2 = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -54);
            if (target2 == null) {
                c.getPlayer().dropMessage(1, "The Android is not powered. Please insert a Mechanical Heart.");
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (source.getAndroid() == null) {
                final int uid = MapleInventoryIdentifier.getInstance();
                source.setUniqueId(uid);
                source.setAndroid(MapleAndroid.create(source.getItemId(), uid));
                flag |= ItemFlag.LOCK.getValue();
                flag |= ItemFlag.UNTRADEABLE.getValue();
                flag |= ItemFlag.ANDROID_ACTIVATED.getValue();
                source.setFlag(flag);
                c.getSession().write(InventoryPacket.updateSpecialItemUse_(source, MapleInventoryType.EQUIP.getType(), c.getPlayer()));
            }
            chr.removeAndroid();
            chr.setAndroid(source.getAndroid());
        } else if (dst <= -1300 && chr.getAndroid() != null) {
            chr.setAndroid(chr.getAndroid()); //respawn it
        }
        if (source.getCharmEXP() > 0 && !ItemFlag.CHARM_EQUIPPED.check(flag)) {
            chr.getTrait(MapleTraitType.charm).addExp(source.getCharmEXP(), chr);
            source.setCharmEXP((short) 0);
            flag |= ItemFlag.CHARM_EQUIPPED.getValue();
            source.setFlag(flag);
            c.getSession().write(InventoryPacket.updateSpecialItemUse_(source, GameConstants.getInventoryType(source.getItemId()).getType(), c.getPlayer()));
        }

        chr.getInventory(MapleInventoryType.EQUIP).removeSlot(src);
        if (target != null) {
            chr.getInventory(MapleInventoryType.EQUIPPED).removeSlot(dst);
        }
        source.setPosition(dst);
        chr.getInventory(MapleInventoryType.EQUIPPED).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            chr.getInventory(MapleInventoryType.EQUIP).addFromDB(target);
        }
        if (GameConstants.isWeapon(source.getItemId())) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.BOOSTER);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SPIRIT_CLAW);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SOULARROW);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.LIGHTNING_CHARGE);
        }
        if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
        } else if (source.getItemId() == 1122017) {
            chr.startFairySchedule(true, true);
        }
        if (source.getState() >= 17) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] potentials = {source.getPotential1(), source.getPotential2(), source.getPotential3(), source.getPotential4(), source.getPotential5()};
            for (int i : potentials) {
                if (i > 0) {
                    StructItemOption pot = ii.getPotentialInfo(i).get(ii.getReqLevel(source.getItemId()) / 10);
                    if (pot != null && pot.get("skillID") > 0) {
                        ss.put(SkillFactory.getSkill(PlayerStats.getSkillByJob(pot.get("skillID"), c.getPlayer().getJob())), new SkillEntry((byte) 1, (byte) 0, -1));
                    }
                }
            }
            c.getPlayer().changeSkillLevel_Skip(ss, true);
        }
        if (source.getSocketState() > 15) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] sockets = {source.getSocket1(), source.getSocket2(), source.getSocket3()};
            for (int i : sockets) {
                if (i > 0) {
                    StructItemOption soc = ii.getSocketInfo(i);
                    if (soc != null && soc.get("skillID") > 0) {
                        ss.put(SkillFactory.getSkill(PlayerStats.getSkillByJob(soc.get("skillID"), c.getPlayer().getJob())), new SkillEntry((byte) 1, (byte) 0, -1));
                    }
                }
            }
            c.getPlayer().changeSkillLevel_Skip(ss, true);
        }
        c.getSession().write(InventoryPacket.moveInventoryItem(MapleInventoryType.EQUIP, src, dst, (byte) 2, false, false));
        chr.equipChanged();
    }

    /*
    取下裝備設定
    */
    public static void unequip(final MapleClient c, final short src, final short dst) {
        Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(src);
        Equip target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);

        if (dst < 0 || source == null || (GameConstants.GMS && src == -55)) {
            return;
        }
        if (target != null && src <= 0) { // do not allow switching with equip
            c.getSession().write(InventoryPacket.getInventoryFull());
            return;
        }
        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeSlot(src);
        if (target != null) {
            c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot(dst);
        }
        source.setPosition(dst);
        c.getPlayer().getInventory(MapleInventoryType.EQUIP).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(target);
        }

        if (GameConstants.isWeapon(source.getItemId())) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.BOOSTER);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SPIRIT_CLAW);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SOULARROW);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.LIGHTNING_CHARGE);
        } else if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
        } else if (source.getItemId() / 10000 == 166) {
            c.getPlayer().removeAndroid();
        } else if (src <= -1300 && c.getPlayer().getAndroid() != null) {
            c.getPlayer().setAndroid(c.getPlayer().getAndroid());
        } else if (source.getItemId() == 1122017) {
            c.getPlayer().cancelFairySchedule(true);
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (source.getState() >= 17) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] potentials = {source.getPotential1(), source.getPotential2(), source.getPotential3(), source.getPotential4(), source.getPotential5()};
            for (int i : potentials) {
                if (i > 0) {
                    StructItemOption pot = ii.getPotentialInfo(i).get(ii.getReqLevel(source.getItemId()) / 10);
                    if (pot != null && pot.get("skillID") > 0) {
                        ss.put(SkillFactory.getSkill(PlayerStats.getSkillByJob(pot.get("skillID"), c.getPlayer().getJob())), new SkillEntry((byte) 0, (byte) 0, -1));
                    }
                }
            }
            c.getPlayer().changeSkillLevel_Skip(ss, true);
        }
        if (source.getSocketState() > 15) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] sockets = {source.getSocket1(), source.getSocket2(), source.getSocket3()};
            for (int i : sockets) {
                if (i > 0) {
                    StructItemOption soc = ii.getSocketInfo(i);
                    if (soc != null && soc.get("skillID") > 0) {
                        ss.put(SkillFactory.getSkill(PlayerStats.getSkillByJob(soc.get("skillID"), c.getPlayer().getJob())), new SkillEntry((byte) 1, (byte) 0, -1));
                    }
                }
            }
            c.getPlayer().changeSkillLevel_Skip(ss, true);
        }
        c.getSession().write(InventoryPacket.moveInventoryItem(MapleInventoryType.EQUIP, src, dst, (byte) 1, false, false));
        c.getPlayer().equipChanged();
    }

    public static boolean drop(final MapleClient c, MapleInventoryType type, final short src, final short quantity) {
        return drop(c, type, src, quantity, false);
    }

    public static boolean drop(final MapleClient c, MapleInventoryType type, final short src, short quantity, final boolean npcInduced) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (src < 0) {
            type = MapleInventoryType.EQUIPPED;
        }
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return false;
        }
        final Item source = c.getPlayer().getInventory(type).getItem(src);
        int[] blocked = {4000038, 4001116};
        for (int i = 0; i < blocked.length; i++) {
            if (source.getItemId() == blocked[i]) {
                c.getPlayer().dropMessage(1, "You may not drop this item! They can only be traded.");
                c.getSession().write(CWvsContext.enableActions());
                return false;
            }
        }
        if (quantity < 0 || source == null || (GameConstants.GMS && src == -55) || (!npcInduced && GameConstants.isPet(source.getItemId())) || (quantity == 0 && !GameConstants.isRechargable(source.getItemId())) || c.getPlayer().inPVP()) {
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }

        if (src > 100) {//防止從礦物背包內丟道具出來閃退
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }

        final short flag = source.getFlag();
        if (quantity > source.getQuantity() && !GameConstants.isRechargable(source.getItemId())) {
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }
        if (ItemFlag.LOCK.check(flag) || (quantity != 1 && type == MapleInventoryType.EQUIP)) { // hack
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }
        final Point dropPos = new Point(c.getPlayer().getPosition());
        //  c.getPlayer().getCheatTracker().checkDrop();
        if (quantity < source.getQuantity() && !GameConstants.isRechargable(source.getItemId())) {
            final Item target = source.copy();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            c.getSession().write(InventoryPacket.dropInventoryItemUpdate(type, source));

            if (ii.isAccountShared(target.getItemId())) {
                if (ItemFlag.KARMA_EQ.check(flag)) {
                    target.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                } else if (ItemFlag.KARMA_USE.check(flag)) {
                    target.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                } else {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                }
            } else {
                if (GameConstants.isPet(source.getItemId()) || ItemFlag.UNTRADEABLE.check(flag)) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                }
            }
        } else {
            c.getPlayer().getInventory(type).removeSlot(src);
            if (GameConstants.isHarvesting(source.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }
            c.getSession().write(InventoryPacket.dropInventoryItem((src < 0 ? MapleInventoryType.EQUIP : type), src));
            if (src < 0) {
                c.getPlayer().equipChanged();
            }
            if (ii.isAccountShared(source.getItemId())) {
                if (ItemFlag.KARMA_EQ.check(flag)) {
                    source.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                } else if (ItemFlag.KARMA_USE.check(flag)) {
                    source.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                } else {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                }
            } else {
                if (GameConstants.isPet(source.getItemId()) || ItemFlag.UNTRADEABLE.check(flag)) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                }
            }
        }
        return true;
    }
}
