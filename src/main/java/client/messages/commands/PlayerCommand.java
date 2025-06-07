package client.messages.commands;

import client.*;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.MapleRing;

import client.messages.CommandProcessorUtil;
import client.messages.commands.CommandExecute.TradeExecute;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import constants.GameConstants;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.channel.handler.HiredMerchantHandler;
import handling.world.World;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import scripting.NPCScriptManager;
import server.*;
import server.RankingWorker.RankingInformation;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.Pair;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CWvsContext;

/**
 * @author Emilyx3
 */
public class PlayerCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.NORMAL;
    }

    public static class STR extends DistributeStatCommands {

        public STR() {
            stat = MapleStat.STR;
        }
    }

    public static class DEX extends DistributeStatCommands {

        public DEX() {
            stat = MapleStat.DEX;
        }
    }

    public static class INT extends DistributeStatCommands {

        public INT() {
            stat = MapleStat.INT;
        }
    }


    public static class Connected extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            java.util.Map<Integer, Integer> connected = World.getConnected();
            StringBuilder conStr = new StringBuilder("Connected Clients: ");
            boolean first = true;
            for (int i : connected.keySet()) {
                if (!first) {
                    conStr.append(", ");
                } else {
                    first = false;
                }
                if (i == 0) {
                    conStr.append("Total: ");
                    conStr.append(connected.get(i));
                } else {
                    conStr.append("Channel");
                    conStr.append(i);
                    conStr.append(": ");
                    conStr.append(connected.get(i));
                }
            }
            c.getPlayer().dropMessage(6, conStr.toString());
            return 1;
        }
    }


    public static class hireditems extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setConversation(3);
            HiredMerchantHandler.displayMerch(c);
            c.getPlayer().fakeRelog();
            return 1;
        }
    }


    public static class meso2nek extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMeso() >= 1000000000) {
                if (c.getPlayer().getInventory(MapleInventoryType.ETC).isFull()) {
                    c.getPlayer().dropMessage(1, "ETC inventory is full, can't make NEK");
                } else {
                    c.getPlayer().gainMeso(-1000000000, true);
                    MapleInventoryManipulator.addById(c.getPlayer().getClient(), 4001116, (short) 1, "M " + c.getPlayer().getName());
                    c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
                }
            }
            return 1;
        }
    }


    public static class nek2meso extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().haveItem(4001116, 1) && c.getPlayer().getMeso() <= 1100000000) {
                MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4001116, 1, true, true);
                c.getPlayer().gainMeso(1000000000, true);
            } else {
                c.getPlayer().dropMessage(5, "Too much mesos! Please have only 1100000000 mesos in your inventory, or less.");
            }
            return 1;
        }
    }


    private static ResultSet GMlist() {
        try {
            Connection con = (Connection) DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = (PreparedStatement) con.prepareStatement("SELECT gm, name FROM characters WHERE gm >= 6 AND dgm = 0 AND noacc = 0 ORDER BY gm DESC");
            return ps.executeQuery();
        } catch (SQLException ex) {
            return null;
        }
    }

    public static class onlinegms extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            int i = 0;
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                if (cs.getPlayerStorage().getAllCharacters().size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    player.dropMessage(5, "Channel " + cs.getChannel());
                    for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters()) {
                        if (chr.isGM() && chr.getDGM() == 0 && chr.gethiddenGM() == 0) {
                            i++;
                            if (sb.length() > 150) {
                                player.dropMessage(5, sb.toString());
                                sb = new StringBuilder();
                            }
                            sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName() + "   "));
                        }
                    }
                    player.dropMessage(5, sb.toString());
                }
            }
            return 1;
        }
    }

    public static class onlinedgms extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            int i = 0;
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                if (cs.getPlayerStorage().getAllCharacters().size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    player.dropMessage(5, "Channel " + cs.getChannel());
                    for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters()) {
                        if (chr.isGM() && chr.getDGM() == 1 && chr.gethiddenGM() == 0) {
                            i++;
                            if (sb.length() > 150) {
                                player.dropMessage(5, sb.toString());
                                sb = new StringBuilder();
                            }
                            sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName() + "   "));
                        }
                    }
                    player.dropMessage(5, sb.toString());
                }
            }
            return 1;
        }
    }


    public static class autonek extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getGML() == 0) {
                c.getPlayer().setGML(1);
                c.getPlayer().dropMessage(5, "Auto NEK is now activate! When you gain over 1b mesos they will be converted to GML!");
            } else {
                c.getPlayer().setGML(0);
                c.getPlayer().dropMessage(5, "Auto NEK is now disabled!");
            }
            return 1;
        }
    }

    public static class boop extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getClient().getSession().write(CField.getGameMessage("BOOP", false)); //pink
            return 1;
        }
    }

    public static class Marry extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMeso() >= 100000000) {
                if (splitted.length < 3) {
                    c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                    return 0;
                }
                int itemId = Integer.parseInt(splitted[2]);
                if (!GameConstants.isEffectRing(itemId)) {
                    c.getPlayer().dropMessage(6, "Invalid itemID.");
                } else {
                    c.getPlayer().gainMeso(-100000000, true);
                    MapleCharacter fff = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (fff == null) {
                        c.getPlayer().dropMessage(6, "Player must be online");
                    } else {
                        int[] ringID = {MapleInventoryIdentifier.getInstance(), MapleInventoryIdentifier.getInstance()};
                        try {
                            MapleCharacter[] chrz = {fff, c.getPlayer()};
                            for (int i = 0; i < chrz.length; i++) {
                                Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId, ringID[i]);
                                if (eq == null) {
                                    c.getPlayer().dropMessage(6, "Invalid itemID.");
                                    return 0;
                                }
                                MapleInventoryManipulator.addbyItem(chrz[i].getClient(), eq.copy());
                                chrz[i].dropMessage(6, "Successfully married with " + chrz[i == 0 ? 1 : 0].getName());
                            }
                            MapleRing.addToDB(itemId, c.getPlayer(), fff.getName(), fff.getId(), ringID);
                        } catch (SQLException e) {
                        }
                    }
                }

            } else {
                c.getPlayer().dropMessage(5, "A ring costs 100m! it must be a crush, friend or a marriage ring!");
            }
            return 1;
        }
    }

    public static class LUK extends DistributeStatCommands {

        public LUK() {
            stat = MapleStat.LUK;
        }
    }

    public static class male extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMap().getId() == 100000000) {
                c.getPlayer().setGender((byte) 0);
                c.getPlayer().saveToDB(false, false);
                c.getPlayer().reloadC();
                c.getPlayer().dropMessage(5, "Done! now you can wear male equips!");
            } else {
                c.getPlayer().dropMessage(5, "You must be in HENESYS to use this command!");
            }
            return 1;
        }
    }

    public static class female extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMap().getId() == 100000000) {
                c.getPlayer().setGender((byte) 1);
                c.getPlayer().saveToDB(false, false);
                c.getPlayer().reloadC();
                c.getPlayer().dropMessage(5, "Done! now you can wear female equips!");
            } else {
                c.getPlayer().dropMessage(5, "You must be in HENESYS to use this command!");
            }
            return 1;
        }
    }

    public static class unisx extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMap().getId() == 100000000) {
                c.getPlayer().setGender((byte) 2);
                c.getPlayer().saveToDB(false, false);
                c.getPlayer().reloadC();
                c.getPlayer().dropMessage(5, "Done! now you can wear all equips!");
            } else {
                c.getPlayer().dropMessage(5, "You must be in HENESYS to use this command!");
            }
            return 1;
        }
    }

    public static class buycube extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 2) {
                c.getPlayer().dropMessage(5, "Format: @buycube QUANTITY");
                return 0;
            }
            if (c.getPlayer().isGM()) {
                return 0;
            }
            final int itemId = 5062002;
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1);
            if (quantity < 0 || quantity > 50) {
                c.getPlayer().dropMessage(5, "Quantity must be greater than 0. Quantity must be 50 or less than 50.");
                return 0;
            }
            Item item;
            if (GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH && c.getPlayer().getMeso() >= 25000000 * quantity && !c.getPlayer().getInventory(MapleInventoryType.CASH).isFull()) {
                c.getPlayer().gainMeso(-25000000 * quantity, true);
                item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);
                item.setOwner("CUBE " + c.getPlayer().getName());
                item.setGMLog(c.getPlayer().getName() + " used @buycube");
                MapleInventoryManipulator.addbyItem(c, item);
                c.getPlayer().dropMessage(6, "You've lost " + 25000000 * quantity + " mesos! and have gained " + quantity + " cubes!");
            } else {
                c.getPlayer().dropMessage(5, "Not enough mesos");
                return 0;

            }
            return 1;
        }
    }

    public static class helpme extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Usage: @helpme message");
                return 0;
            }
            if (c.getPlayer().getGMLevel() > 3) {
                c.getPlayer().dropMessage(6, "(D)GMs can't use @helpme");
                return 0;
            }
            World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, c.getPlayer().getName() + ": " + StringUtil.joinStringFrom(splitted, 1)));
            c.getPlayer().dropMessage(6, "You've successfully sent your message, a GM will reply to your shortly.");
            //   c.getChannelServer().getWorldInterface().broadcastGMMessage(null, MaplePacketCreator.serverNotice(6, "Channel: " + c.getChannel() + "  " + player.getName() + ": " + StringUtil.joinStringFrom(splitted, 1)).getBytes());
            return 1;
        }
    }

    public static class gms extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "List of AxedMS GMs");
            String status;
            ResultSet rs = GMlist();
            int i = 1;
            try {
                while (rs.next()) {
                    if (rs.getInt("gm") == 6) {
                        status = "GM";
                    } else if (rs.getInt("gm") == 7) {
                        status = "Supervisor";
                    } else if (rs.getInt("gm") == 8) {
                        status = "Co-Owner";
                    } else if (rs.getInt("gm") == 98) {
                        status = "The king";
                    } else if (rs.getInt("gm") == 99) {
                        status = "Administrator/Owner";
                    } else {
                        status = "";
                    }
                    c.getPlayer().dropMessage(6, i + ". " + rs.getString("name") + " - " + status);
                    i++;
                }
            } catch (SQLException se) {
            }

            return 1;
        }
    }

    public static class online extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            int i = 0;
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                if (cs.getPlayerStorage().getAllCharacters().size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    player.dropMessage(5, "Channel " + cs.getChannel());
                    for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters()) {
                        i++;
                        if (sb.length() > 150) {
                            player.dropMessage(5, sb.toString());
                            sb = new StringBuilder();
                        }
                        sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName() + "   "));
                    }
                    player.dropMessage(5, sb.toString());
                    player.dropMessage(5, "Total online: " + cs.getPlayerStorage().getAllCharacters().size());
                }
            }
            return 1;
        }
    }

    private static ResultSet ranking(boolean gm) {
        try {
            Connection con = (Connection) DatabaseConnection.getConnection();
            java.sql.PreparedStatement ps;
            if (!gm) {
                ps = con.prepareStatement("SELECT reborns, name FROM characters WHERE gm < 4 ORDER BY reborns desc LIMIT 30");
            } else {
                ps = con.prepareStatement("SELECT name, gm FROM characters WHERE gm >= 4");
            }
            return ps.executeQuery();
        } catch (SQLException ex) {
            return null;
        }
    }

    public static class top30 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                ResultSet rs = ranking(false);
                c.getPlayer().dropMessage(6, "Here are the top 30 players");
                int i = 1;
                while (rs.next()) {
                    c.getPlayer().dropMessage(6, i + ". " + rs.getString("name") + " || Rebirths: " + rs.getInt("reborns"));
                    i++;
                }
            } catch (SQLException ex) {
                Logger.getLogger(PlayerCommand.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 1;
        }
    }

    public static class eventitem extends CommandExecute {

        int[] blockedids = {4000038, 5530103, 4001551, 4001550, 4001549, 4001548, 4001547, 1602000, 1602001, 1602002, 1602003, 1602004, 1602005, 1602006, 1602007, 1001076, 5530123, 2046025, 2046026, 2046119, 2046120, 2046251, 2046340, 2046341, 3994385, 4000313, 4001168, 4000038, 1112400, 1442120, 1112920, 2043203, 2043303, 2043003, 2043103, 2043703, 2043803, 2041024, 2041025, 2040903, 2040913, 2040303, 2040037, 2040006, 2040007, 2040709, 2040710, 2040711, 2040806, 2040603, 2040507, 2040506, 2040403, 2044103, 2044003, 2044019, 2044303, 2044403, 2044203, 2044603, 2044503, 2044815, 2044908, 2044703, 2040807, 1002959, 1532000, 1532001, 1532005, 1532006, 1532045, 1532046, 1532050, 1532051, 1532039, 1522022, 1522019, 1522017, 4000174, 1112309, 1112310, 1112311, 4001126, 4031830, 1002140, 1003274, 1062140, 1042223, 1003142, 1042003, 1062007, 1322013, 4001116};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 2) {
                c.getPlayer().dropMessage(5, "@eventitem itemid");
                return 0;
            }
            if (c.getPlayer().isGM()) {
                return 0;
            }
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            if (quantity < 0) {
                c.getPlayer().dropMessage(5, "Making items with negative quantity is for fags.");
                return 0;
            }
            if (c.getPlayer().haveItem(4000038, 1 * quantity)) {
                final int itemId = Integer.parseInt(splitted[1]);
                for (int i = 0; i < blockedids.length; i++) {
                    if (itemId == blockedids[i]) {
                        c.getPlayer().dropMessage(6, "You can't make this item");
                        return 0;
                    }
                }

                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (GameConstants.isPet(itemId)) {
                    c.getPlayer().dropMessage(5, "Please purchase a pet from the cash shop instead.");
                } else if (!ii.itemExists(itemId)) {
                    c.getPlayer().dropMessage(5, itemId + " does not exist");
                } else {
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000038, 1 * quantity, true, true);
                    Item item;

                    if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                        item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                    } else {
                        item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);

                    }
                    item.setOwner("EVENT " + c.getPlayer().getName());
                    item.setGMLog(c.getPlayer().getName() + " used @eventitem");


                    MapleInventoryManipulator.addbyItem(c, item);
                }

            } else {
                c.getPlayer().dropMessage(5, "You need " + 1 * quantity + " trophies to get what you're trying to get.");
            }
            return 1;
        }
    }

    public static class voteitem extends CommandExecute {

        int[] blockedids = {4000038, 5530103, 4001551, 4001550, 4001549, 4001548, 4001547, 3012015, 3010191, 3012005, 1602000, 1602001, 1602002, 1602003, 1602004, 1602005, 1602006, 1602007, 1001076, 5530123, 2046025, 2046026, 2046119, 2046120, 2046251, 2046340, 2046341, 3994385, 4000313, 4001168, 4001168, 4000038, 1112400, 1442120, 1112920, 2043203, 2043303, 2043003, 2043103, 2043703, 2043803, 2041024, 2041025, 2040903, 2040913, 2040303, 2040037, 2040006, 2040007, 2040709, 2040710, 2040711, 2040806, 2040603, 2040507, 2040506, 2040403, 2044103, 2044003, 2044019, 2044303, 2044403, 2044203, 2044603, 2044503, 2044815, 2044908, 2044703, 2040807, 1002959, 1532000, 1532001, 1532005, 1532006, 1532045, 1532046, 1532050, 1532051, 1532039, 1522022, 1522019, 1522017, 4000174, 1112309, 1112310, 1112311, 4001126, 4031830, 1002140, 1003274, 1062140, 1042223, 1003142, 1042003, 1062007, 1322013, 4001116};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 2) {
                c.getPlayer().dropMessage(5, "@eventitem itemid");
                return 0;
            }
            if (c.getPlayer().getVPoints() >= 10) {
                final int itemId = Integer.parseInt(splitted[1]);
                for (int i = 0; i < blockedids.length; i++) {
                    if (itemId == blockedids[i]) {
                        c.getPlayer().dropMessage(6, "You can't make this item");
                        return 0;
                    }
                }

                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (!ii.itemExists(itemId)) {
                    c.getPlayer().dropMessage(5, itemId + " does not exist");
                } else {
                    Item item;

                    if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                        c.getPlayer().setVPoints(c.getPlayer().getVPoints() - 10);
                        item = ii.voteitem((Equip) MapleItemInformationProvider.getEquipById(itemId));

                        item.setOwner("VOTE " + c.getPlayer().getName());
                        item.setGMLog(c.getPlayer().getName() + " used @voteitem");


                        MapleInventoryManipulator.addbyItem(c, item);
                    }
                }

            } else {
                c.getPlayer().dropMessage(5, "You need 10 votepoints to get what you're trying to get.");
            }
            return 1;
        }
    }

    public abstract static class DistributeStatCommands extends CommandExecute {

        protected MapleStat stat = null;
        private static int statLim = 32767;

        private void setStat(MapleCharacter player, int amount) {
            switch (stat) {
                case STR:
                    player.getStat().setStr((short) amount, player);
                    player.updateSingleStat(MapleStat.STR, player.getStat().getStr());
                    break;
                case DEX:
                    player.getStat().setDex((short) amount, player);
                    player.updateSingleStat(MapleStat.DEX, player.getStat().getDex());
                    break;
                case INT:
                    player.getStat().setInt((short) amount, player);
                    player.updateSingleStat(MapleStat.INT, player.getStat().getInt());
                    break;
                case LUK:
                    player.getStat().setLuk((short) amount, player);
                    player.updateSingleStat(MapleStat.LUK, player.getStat().getLuk());
                    break;
            }
        }

        private int getStat(MapleCharacter player) {
            switch (stat) {
                case STR:
                    return player.getStat().getStr();
                case DEX:
                    return player.getStat().getDex();
                case INT:
                    return player.getStat().getInt();
                case LUK:
                    return player.getStat().getLuk();
                default:
                    throw new RuntimeException(); //Will never happen.
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "Invalid number entered.");
                return 0;
            }
            int change;
            try {
                change = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(5, "Invalid number entered.");
                return 0;
            }
            if (change <= 0) {
                c.getPlayer().dropMessage(5, "You must enter a number greater than 0.");
                return 0;
            }
            if (c.getPlayer().getRemainingAp() < change) {
                c.getPlayer().dropMessage(5, "You don't have enough AP for that.");
                return 0;
            }
            if (getStat(c.getPlayer()) + change > statLim) {
                c.getPlayer().dropMessage(5, "The stat limit is " + statLim + ".");
                return 0;
            }
            setStat(c.getPlayer(), getStat(c.getPlayer()) + change);
            c.getPlayer().setRemainingAp((c.getPlayer().getRemainingAp() - change));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
            c.getPlayer().dropMessage(5, StringUtil.makeEnumHumanReadable(stat.name()) + " has been raised by " + change + ".");
            return 1;
        }
    }

    public static class Mob extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonster mob = null;
            for (final MapleMapObject monstermo : c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 100000, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.isAlive()) {
                    c.getPlayer().dropMessage(6, "Monster " + mob.toString());
                    break; //only one
                }
            }
            if (mob == null) {
                c.getPlayer().dropMessage(6, "No monster was found.");
            }
            return 1;
        }
    }

    public static class resetap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().resetStats(4, 4, 4, 4);
            c.getPlayer().dropMessage(6, "Done, you now have" + c.getPlayer().getRemainingAp() + " AP");
            return 1;
        }
    }

    public static class checkap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "You currently have: " + c.getPlayer().getRemainingAp() + " AP");
            return 1;
        }
    }

    public static class GotoEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (!c.getPlayer().isInBlockedMap()) {
                if (c.getPlayer().getClient().getChannelServer().eventOn == true && c.getPlayer().getClient().getChannelServer().eventClosed == false) {
                    c.getPlayer().changeMap2(c.getPlayer().getClient().getChannelServer().eventMap, 0);
                } else {
                    c.getPlayer().dropMessage(5, "Sorry, no events going on at the moment.");
                }
            }
            return 1;
        }
    }

    public static class checkrb extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "You currently have: " + c.getPlayer().getReborns() + " Rebirths!");
            return 1;
        }
    }

    public abstract static class OpenNPCCommand extends CommandExecute {

        protected int npc = -1;
        private static int[] npcs = { //Ish yur job to make sure these are in order and correct ;(
                9270035,
                9900002,
                9000000,
                9000030,
                9010000,
                9000085,
                9000018,
                9900000,
                9270034,
                9270036};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, npcs[npc], null);
            return 1;
        }
    }

    public static class Npc extends OpenNPCCommand {

        public Npc() {
            npc = 0;
        }
    }

    public static class DCash extends OpenNPCCommand {

        public DCash() {
            npc = 1;
        }
    }

    public static class Event extends OpenNPCCommand {

        public Event() {
            npc = 2;
        }
    }

    public static class CheckDrop extends OpenNPCCommand {

        public CheckDrop() {
            npc = 4;
        }
    }

    public static class fdsfds extends OpenNPCCommand {

        public fdsfds() {
            npc = 5;
        }
    }

    public static class fdsfs extends OpenNPCCommand {

        public fdsfs() {
            npc = 6;
        }
    }

    public static class changelook extends OpenNPCCommand {

        public changelook() {
            npc = 7;
        }
    }

    public static class shop extends OpenNPCCommand {

        public shop() {
            npc = 8;
        }
    }

    public static class job extends OpenNPCCommand {

        public job() {
            npc = 9;
        }
    }

    /*
     * public static class ClearSlot extends CommandExecute {
     *
     * private static MapleInventoryType[] invs = { MapleInventoryType.EQUIP,
     * MapleInventoryType.USE, MapleInventoryType.SETUP, MapleInventoryType.ETC,
     * MapleInventoryType.CASH,};
     *
     * @Override public int execute(MapleClient c, String[] splitted) {
     * MapleCharacter player = c.getPlayer(); if (splitted.length < 2 ||
     * player.hasBlockedInventory()) { c.getPlayer().dropMessage(5, "@clearslot
     * <eq/use/setup/etc/cash/all>"); return 0; } else { MapleInventoryType
     * type; if (splitted[1].equalsIgnoreCase("eq")) { type =
     * MapleInventoryType.EQUIP; } else if (splitted[1].equalsIgnoreCase("use"))
     * { type = MapleInventoryType.USE; } else if
     * (splitted[1].equalsIgnoreCase("setup")) { type =
     * MapleInventoryType.SETUP; } else if (splitted[1].equalsIgnoreCase("etc"))
     * { type = MapleInventoryType.ETC; } else if
     * (splitted[1].equalsIgnoreCase("cash")) { type = MapleInventoryType.CASH;
     * } else if (splitted[1].equalsIgnoreCase("all")) { type = null; } else {
     * c.getPlayer().dropMessage(5, "Invalid. @clearslot
     * <eq/use/setup/etc/cash/all>"); return 0; } if (type == null) { //All, a
     * bit hacky, but it's okay for (MapleInventoryType t : invs) { type = t;
     * MapleInventory inv = c.getPlayer().getInventory(type); byte start = -1;
     * for (byte i = 0; i < inv.getSlotLimit(); i++) { if (inv.getItem(i) !=
     * null) { start = i; break; } } if (start == -1) {
     * c.getPlayer().dropMessage(5, "There are no items in that inventory.");
     * return 0; } int end = 0; for (byte i = start; i < inv.getSlotLimit();
     * i++) { if (inv.getItem(i) != null) {
     * MapleInventoryManipulator.removeFromSlot(c, type, i,
     * inv.getItem(i).getQuantity(), true); } else { end = i; break;//Break at
     * first empty space. } } c.getPlayer().dropMessage(5, "Cleared slots " +
     * start + " to " + end + "."); } } else { MapleInventory inv =
     * c.getPlayer().getInventory(type); byte start = -1; for (byte i = 0; i <
     * inv.getSlotLimit(); i++) { if (inv.getItem(i) != null) { start = i;
     * break; } } if (start == -1) { c.getPlayer().dropMessage(5, "There are no
     * items in that inventory."); return 0; } byte end = 0; for (byte i =
     * start; i < inv.getSlotLimit(); i++) { if (inv.getItem(i) != null) {
     * MapleInventoryManipulator.removeFromSlot(c, type, i,
     * inv.getItem(i).getQuantity(), true); } else { end = i; break;//Break at
     * first empty space. } } c.getPlayer().dropMessage(5, "Cleared slots " +
     * start + " to " + end + "."); } return 1; } }
    }
     */
    public static class Go extends CommandExecute {

        private static final HashMap<String, Integer> gotomaps = new HashMap<>();

        static {
            gotomaps.put("southperry", 2000000);
            gotomaps.put("amherst", 1010000);
            gotomaps.put("henesys", 100000000);
            gotomaps.put("ellinia", 101000000);
            gotomaps.put("perion", 102000000);
            gotomaps.put("kerning", 103000000);
            gotomaps.put("harbor", 104000000);
            gotomaps.put("sleepywood", 105000000);
            gotomaps.put("florina", 120000300);
            gotomaps.put("orbis", 200000000);
            gotomaps.put("happyville", 209000000);
            gotomaps.put("elnath", 211000000);
            gotomaps.put("ludibrium", 220000000);
            gotomaps.put("aquaroad", 230000000);
            gotomaps.put("leafre", 240000000);
            gotomaps.put("mulung", 250000000);
            gotomaps.put("herbtown", 251000000);
            gotomaps.put("omegasector", 221000000);
            gotomaps.put("koreanfolktown", 222000000);
            gotomaps.put("newleafcity", 600000000);
            gotomaps.put("sharenian", 990000000);
            gotomaps.put("pianus", 230040420);
            gotomaps.put("horntail", 240050400);
            gotomaps.put("griffey", 240020101);
            gotomaps.put("manon", 240020401);
            gotomaps.put("zakum", 211042300);
            gotomaps.put("czakum", 211042301);
            gotomaps.put("papulatus", 220080001);
            gotomaps.put("showatown", 801000000);
            gotomaps.put("zipangu", 800000000);
            gotomaps.put("ariant", 260000100);
            gotomaps.put("nautilus", 120000000);
            gotomaps.put("boatquay", 541000000);
            gotomaps.put("malaysia", 550000000);
            gotomaps.put("erev", 130000000);
            gotomaps.put("ellin", 300000000);
            gotomaps.put("kampung", 551000000);
            gotomaps.put("singapore", 540000000);
            gotomaps.put("amoria", 680000000);
            gotomaps.put("timetemple", 270000000);
            gotomaps.put("pinkbean", 270050100);
            gotomaps.put("fm", 910000000);
            gotomaps.put("freemarket", 910000000);
            //      gotomaps.put("oxquiz", 109020001);
            //     gotomaps.put("ola", 109030101);
            //   gotomaps.put("fitness", 109040000);
            gotomaps.put("cygnus", 271030200);
            gotomaps.put("golden", 950100000);
            gotomaps.put("phantom", 610010000);
            gotomaps.put("cwk", 610030000);
            gotomaps.put("rien", 140000000);
            gotomaps.put("edel", 310000000);
            gotomaps.put("ardent", 910001000);
            gotomaps.put("craft", 910001000);
            gotomaps.put("pvp", 960000000);
            gotomaps.put("future", 271000000);


        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            //  if (!c.getPlayer().inBlockedMap()) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Syntax: @go <mapname>");
            } else {
                if (gotomaps.containsKey(splitted[1])) {
                    MapleMap target = c.getChannelServer().getMapFactory().getMap(gotomaps.get(splitted[1]));
                    if (target == null) {
                        c.getPlayer().dropMessage(6, "Map does not exist || Check maps with @go maps");
                        return 0;
                    }
                    MaplePortal targetPortal = target.getPortal(0);
                    c.getPlayer().changeMap(target, targetPortal);
                } else {
                    if (splitted[1].equals("maps")) {
                        c.getPlayer().dropMessage(6, "Use @go <map>. Locations are as follows:");
                        StringBuilder sb = new StringBuilder();
                        for (String s : gotomaps.keySet()) {
                            sb.append(s).append(", ");
                        }
                        c.getPlayer().dropMessage(6, sb.substring(0, sb.length() - 2));
                    } else {
                        c.getPlayer().dropMessage(6, "Invalid command syntax - Use @go <map>. For a list of locations, use @go maps.");
                    }
                }
            }
            //  }
            return 1;
        }
    }

    public static class dispose extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(CWvsContext.enableActions());
            c.getPlayer().dropMessage(6, "Disposed!");
            return 1;
        }
    }

    public static class TSmega extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setSmega();
            return 1;
        }
    }

    public static class Ranking extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) { //job start end
                c.getPlayer().dropMessage(5, "Use @ranking [job] [start number] [end number] where start and end are ranks of the players");
                final StringBuilder builder = new StringBuilder("JOBS: ");
                for (String b : RankingWorker.getJobCommands().keySet()) {
                    builder.append(b);
                    builder.append(" ");
                }
                c.getPlayer().dropMessage(5, builder.toString());
            } else {
                int start = 1, end = 20;
                try {
                    start = Integer.parseInt(splitted[2]);
                    end = Integer.parseInt(splitted[3]);
                } catch (NumberFormatException e) {
                    c.getPlayer().dropMessage(5, "You didn't specify start and end number correctly, the default values of 1 and 20 will be used.");
                }
                if (end < start || end - start > 20) {
                    c.getPlayer().dropMessage(5, "End number must be greater, and end number must be within a range of 20 from the start number.");
                } else {
                    final Integer job = RankingWorker.getJobCommand(splitted[1]);
                    if (job == null) {
                        c.getPlayer().dropMessage(5, "Please use @ranking to check the job names.");
                    } else {
                        final List<RankingInformation> ranks = RankingWorker.getRankingInfo(job.intValue());
                        if (ranks == null || ranks.size() <= 0) {
                            c.getPlayer().dropMessage(5, "Please try again later.");
                        } else {
                            int num = 0;
                            for (RankingInformation rank : ranks) {
                                if (rank.rank >= start && rank.rank <= end) {
                                    if (num == 0) {
                                        c.getPlayer().dropMessage(6, "Rankings for " + splitted[1] + " - from " + start + " to " + end);
                                        c.getPlayer().dropMessage(6, "--------------------------------------");
                                    }
                                    c.getPlayer().dropMessage(6, rank.toString());
                                    num++;
                                }
                            }
                            if (num == 0) {
                                c.getPlayer().dropMessage(5, "No ranking was returned.");
                            }
                        }
                    }
                }
            }
            return 1;
        }
    }

    public static class spy extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Syntax: @spy <playername>");
            } else {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    c.getPlayer().dropMessage(5, "Players stats are:");
                    c.getPlayer().dropMessage(5, "Level: " + victim.getLevel() + "  ||  Rebirths: " + victim.getReborns());
                    c.getPlayer().dropMessage(5, "Fame: " + victim.getFame());
                    c.getPlayer().dropMessage(5, "Str: " + victim.getStat().getStr() + "  ||  Dex: " + victim.getStat().getDex() + "  ||  Int: " + victim.getStat().getInt() + "  ||  Luk: " + victim.getStat().getLuk());
                    c.getPlayer().dropMessage(5, "Player has " + victim.getMeso() + " mesos.");
                    c.getPlayer().dropMessage(5, "Hp: " + victim.getStat().getHp() + "/" + victim.getStat().getCurrentMaxHp() + "  ||  Mp: " + victim.getStat().getMp() + "/" + victim.getStat().getCurrentMaxMp(victim.getJob()));
                    if (victim.gethiddenGM() == 0) {
                        c.getPlayer().dropMessage(5, "NX Cash: " + victim.getCSPoints(1) + "  ||  AP: " + victim.getRemainingAp() + " || MSI points: " + victim.getMSIPoints());
                    } else {
                        c.getPlayer().dropMessage(5, "NX Cash: " + victim.getCSPoints(1) + "  ||  AP: " + victim.getRemainingAp() + " || MSI points: 0");
                    }

                    c.getPlayer().dropMessage(5, "Votepoints: " + victim.getVPoints());
                    victim.dropMessage(5, c.getPlayer().getName() + " has used @spy on you!");
                    if (victim.getGMLevel() == 2 && victim.gethiddenGM() == 0) {
                        c.getPlayer().dropMessage(5, "This player is a donator.");
                    } else if (victim.getDGM() == 1 && victim.gethiddenGM() == 0) {
                        c.getPlayer().dropMessage(5, "This player is a DGM.");
                    } else if (victim.getGMLevel() == 3 && victim.gethiddenGM() == 0) {
                        c.getPlayer().dropMessage(5, "This player is a super donator.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "Player not found.");
                }
            }
            return 1;
        }
    }

    public static class Help extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "@str, @dex, @int, @luk <amount to add>");
            c.getPlayer().dropMessage(5, "@mob < Information on the closest monster >");
            c.getPlayer().dropMessage(5, "@go < Warps you to places, use @go maps to see the available maps >");
            /*
             * c.getPlayer().dropMessage(5, "@changesecondpass - Change second
             * password, @changesecondpass <current Password> <new password>
             * <Confirm new password> ");
             */
            c.getPlayer().dropMessage(5, "@shop < A-I-O Shop >");

            //c.getPlayer().dropMessage(5, "@jaira < Spawn jaira with 1000b hp >");
            c.getPlayer().dropMessage(5, "@meso2nek < 1b meso = 1 neck >");
            c.getPlayer().dropMessage(5, "@nek2meso < 1 neck = 1b >");
            c.getPlayer().dropMessage(5, "@autonek < 1 neck = 1b >");
            c.getPlayer().dropMessage(5, "@job < Job advancer >");
            c.getPlayer().dropMessage(5, "@dcash < Universal Cash Item Dropper >");
            c.getPlayer().dropMessage(5, "@tsmega < Toggle super megaphone on/off >");
            c.getPlayer().dropMessage(5, "@dispose < If you are unable to attack or talk to NPC >");
            /*
             * c.getPlayer().dropMessage(5, "@clearslot < Cleanup that trash in
             * your inventory >");
             */
            c.getPlayer().dropMessage(5, "@skills < Maxes your skills >");
            c.getPlayer().dropMessage(5, "@ranking < Use @ranking for more details >");
            c.getPlayer().dropMessage(5, "@checkdrop < Use @checkdrop for more details >");
            c.getPlayer().dropMessage(5, "@rb < rebirth >");
            c.getPlayer().dropMessage(5, "@resetap < Resets your AP >");
            c.getPlayer().dropMessage(5, "@checkap < Shows your available AP (to be spent) >");
            c.getPlayer().dropMessage(5, "@clearinv < Clear your inventory >");
            c.getPlayer().dropMessage(5, "@msi < itemid > < makes you a msi >");
            c.getPlayer().dropMessage(5, "@gtop100 < claim vote >");
            c.getPlayer().dropMessage(5, "@marry < make a ring >");
            c.getPlayer().dropMessage(5, "@ultimateps < claim vote >");
            c.getPlayer().dropMessage(5, "@vote4nx < trades votepoints for nx >");
            c.getPlayer().dropMessage(5, "@changelook < opens the hair/face/skin changer >");
            c.getPlayer().dropMessage(5, "@emo < kill yourself >");
            c.getPlayer().dropMessage(5, "@checkrb < shows how many rebirths >");
            c.getPlayer().dropMessage(5, "@save < saves your progress >");
            c.getPlayer().dropMessage(5, "@spy playername < Shows info >");
            c.getPlayer().dropMessage(5, "@search < find ids, etc >");
            c.getPlayer().dropMessage(5, "@buyscroll < Trade NX for a scroll >");
            c.getPlayer().dropMessage(5, "@vp2scroll < Trade votepoints for a scroll >");
            c.getPlayer().dropMessage(5, "@online < See who's online >");
            c.getPlayer().dropMessage(5, "@eventitem < 1 trophy = 1 item, use @search to find item>");
            c.getPlayer().dropMessage(5, "@gmroar < 500 reborns = gm roar >");
            c.getPlayer().dropMessage(5, "@vote4cube < 1 Votepoint = 15 super cube >");
            c.getPlayer().dropMessage(5, "@vote4nek < 6 Votepoints = 1 nek >");
            c.getPlayer().dropMessage(5, "@srb < get a 3k-20k stat ring, random stats >");

            c.getPlayer().dropMessage(5, "@srb2 < get a randomized ring with watk and matk 20k-32k stats >");
            c.getPlayer().dropMessage(5, "@top30 < shows top 30 players using rebirths >");
            c.getPlayer().dropMessage(5, "@changename < changes your name, it will DC you. >");
            c.getPlayer().dropMessage(5, "@voteitem < 10 points = item >");
            c.getPlayer().dropMessage(5, "@gms < Shows all REAL GMs >");
            c.getPlayer().dropMessage(5, "@unisx < Wear male AND female equips >");
            c.getPlayer().dropMessage(5, "@female < Wear female equips >");
            c.getPlayer().dropMessage(5, "@male < Wear male equips >");
            c.getPlayer().dropMessage(5, "@helpme < Send a notice to all the online GMs, misusing this command can get you jailed and or banned >");
            c.getPlayer().dropMessage(5, "@onlinegms < Shows online GMs, if you need help or want to harass them. >");
            c.getPlayer().dropMessage(5, "@onlinedgms < Shows online DGMs, if you want to harass them. >");
            c.getPlayer().dropMessage(5, "@freepet < creates a free pet for you >");
            return 1;
        }
    }

    public static class freepet extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int itemId = Integer.parseInt(splitted[1]);

            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {

                int uniqueid = MapleInventoryIdentifier.getInstance();


                Item item = new Item(itemId, (byte) 0, (short) 1, (byte) 0, uniqueid);

                item.setExpiration(2475606994921L);


                final MaplePet pet = MaplePet.createPet(itemId, uniqueid);
                item.setPet(pet);


                MapleInventoryManipulator.addbyItem(c, item);
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " does not exist");
            }

            return 1;
        }
    }

    public static class gmroar extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getReborns() >= 500 && c.getPlayer().getLevel() >= 200) {
                //  c.getPlayer().setReborns(c.getPlayer().getReborns() - 500);
                c.getPlayer().changeJob(900);
                //  c.getPlayer().dropMessage(5, "Done! You've lost 500 reborns");
                c.getPlayer().dropMessage(5, "Please take GM Roar and reborn, else you won't be able to log in to your character.");
            } else {
                c.getPlayer().dropMessage(5, "You need above 500 reborns to get GM roar! (you also have to be level 200)");
            }
            return 1;
        }
    }

    public static class save extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.lastsmegacompare = System.currentTimeMillis() - c.lastsmega;
            if (c.lastsmegacompare > 5000) {
                c.lastsmega = System.currentTimeMillis();
                //  c.getPlayer().saveToDB(false, false);
                c.getPlayer().dropMessage(5, "Your progress has been saved! (Please don't spam this command)");
            } else {
                c.getPlayer().dropMessage(5, "Please don't spam this command");
            }
            return 1;
        }
    }

    public static class vp2scroll extends CommandExecute {

        static int[] blockedids = {4000038, 5530103, 4001551, 4001550, 4001549, 4001548, 4001547, 3012015, 3010191, 3012005, 1602000, 1602001, 1602002, 1602003, 1602004, 1602005, 1602006, 1602007, 1001076, 5530123, 2046025, 2046026, 2046119, 2046120, 2046251, 2046340, 2046341, 3994385, 4000313, 4001168, 4001168, 4000038, 1112400, 1442120, 1112920, 2043203, 2043303, 2043003, 2043103, 2043703, 2043803, 2041024, 2041025, 2040903, 2040913, 2040303, 2040037, 2040006, 2040007, 2040709, 2040710, 2040711, 2040806, 2040603, 2040507, 2040506, 2040403, 2044103, 2044003, 2044019, 2044303, 2044403, 2044203, 2044603, 2044503, 2044815, 2044908, 2044703, 2040807, 4001116};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 3) {
                c.getPlayer().dropMessage(5, "Format: @vp2scroll SCROLLID QUANTITY");
                return 0;
            }
            if (c.getPlayer().isGM()) {
                return 0;
            }
            final int itemId = Integer.parseInt(splitted[1]);
            short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            if (quantity < 0) {
                c.getPlayer().dropMessage(5, "Quantity must be greater than 0.");
                return 0;
            }
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            for (int i = 0; i < blockedids.length; i++) {
                if (itemId == blockedids[i]) {
                    c.getPlayer().dropMessage(5, "You can't make this item");
                    return 0;
                }
            }

            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " does not exist");
            } else {
                Item item;

                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.USE && c.getPlayer().getVPoints() >= 1 * quantity && !c.getPlayer().getInventory(MapleInventoryType.USE).isFull()) {
                    c.getPlayer().setVPoints(c.getPlayer().getVPoints() - quantity);
                    quantity = (short) (quantity * 10);
                    item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);
                    item.setOwner("SCROLL " + c.getPlayer().getName());
                    item.setGMLog(c.getPlayer().getName() + " used @vp2scroll");
                    MapleInventoryManipulator.addbyItem(c, item);
                    c.getPlayer().dropMessage(6, "You've lost " + 1 * (short) (quantity / 10) + " vote points! and have gained " + quantity + " scrolls!");
                } else {
                    c.getPlayer().dropMessage(5, "You can only make USE items! (scrolls) || Make sure you have enough CASH!");
                    return 0;

                }
            }
            return 1;
        }
    }


    public static class msi extends CommandExecute {

        int[] blockedids = {4000038, 5530103, 4001551, 4001550, 4001549, 4001548, 4001547, 3010191, 3012005, 1602000, 1602001, 1602002, 1602003, 1602004, 1602005, 1602006, 1602007, 1001076, 5530123, 2046025, 2046026, 2046119, 2046120, 2046251, 2046340, 2046341, 3994385, 4000313, 4001168, 4001168, 4000038, 1112400, 1442120, 1112920, 1002959, 1532000, 1532001, 1532005, 1532006, 1532045, 1532046, 1532050, 1532051, 1532039, 1522022, 1522019, 1522017, 4000174, 1112309, 1112310, 1112311, 4001126, 4031830, 1002140, 1003274, 1062140, 1042223, 1003142, 1042003, 1062007, 1322013, 3012015, 4001116};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMSIPoints() > 0) {
                if (splitted.length == 2) {
                    int itemid;
                    short multiply;
                    try {
                        itemid = Integer.parseInt(splitted[1]);
                        multiply = (short) 32767;

                        for (int i = 0; i < blockedids.length; i++) {
                            if (itemid == blockedids[i]) {
                                c.getPlayer().dropMessage(6, "You can't make this item");
                                return 0;
                            }
                        }


                    } catch (NumberFormatException asd) {
                        return 0;
                    }
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    Item item = MapleItemInformationProvider.getEquipById(itemid);
                    MapleInventoryType type = GameConstants.getInventoryType(itemid);
                    if (type.equals(MapleInventoryType.EQUIP) && !c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getPlayer().setMSIPoints2(c.getPlayer().getMSIPoints() - 1);
                        MapleInventoryManipulator.addFromDrop(c, ii.MSI((Equip) item, multiply), true);
                        c.getPlayer().dropMessage(5, "You have " + c.getPlayer().getMSIPoints() + " MSI Points left!");
                        c.getPlayer().saveToDB(false, false);
                    } else {
                        c.getPlayer().dropMessage(6, "Make sure it's an equippable item.");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "Invalid syntax.(@msi (Item ID) Example: @msi 1002140");
                }
            } else {
                c.getPlayer().dropMessage(6, "You need MSI points for this! (donate)");

            }
            return 1;
        }
    }

    public static class changename extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getDGM() == 0) {
                c.getPlayer().dropMessage(6, "Can't change name.");
                return 0;
            }
            if (c.getPlayer().getPoints() >= 10) {
                if (MapleCharacterUtil.canCreateChar(splitted[1], false)) {

                    if (c.getPlayer().getClient().getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]) != null) {

                        c.getPlayer().getClient().getChannelServer().getPlayerStorage().NameDereg(splitted[1]);

                    } else {
                        //  System.out.println("name not taken");
                    }
                    c.getPlayer().setPoints(c.getPlayer().getPoints() - 10);
                    c.getPlayer().setName(splitted[1]);
                    c.getPlayer().getClient().getSession().close();


                } else {
                    c.getPlayer().dropMessage(5, "Name not eligible");
                }
            } else {
                c.getPlayer().dropMessage(5, "Donate to change your name!!");
            }
            return 1;
        }
    }

    public static class srb extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().isGM()) {
                return 0;
            }
            if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                c.getPlayer().dropMessage(5, "You're an idiot, you have no inventory space.");
            } else {
                int totAp = c.getPlayer().getStat().getStr() + c.getPlayer().getStat().getDex() + c.getPlayer().getStat().getInt() + c.getPlayer().getStat().getLuk();
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                Item item = MapleItemInformationProvider.getEquipById(1112400);
                if (totAp > 130000) {
                    MapleInventoryManipulator.addFromDrop(c, ii.SRB2((Equip) item), true, true);
                    c.getPlayer().resetSRB();
                } else {
                    c.getPlayer().dropMessage(6, "You need at least 130k stats in total (str+luk+dex+int = total)");
                }
            }
            return 1;
        }
    }

    public static class srb2 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().isGM()) {
                return 0;
            }
            if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                c.getPlayer().dropMessage(5, "You're an idiot, you have no inventory space.");
            } else {
                //	int totAp = c.getPlayer().getStat().getStr() + c.getPlayer().getStat().getDex() + c.getPlayer().getStat().getInt() + c.getPlayer().getStat().getLuk();
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                Item item = MapleItemInformationProvider.getEquipById(1112920);
                if (c.getPlayer().getReborns() >= 1000) {
                    MapleInventoryManipulator.addFromDrop(c, ii.SRB3((Equip) item), true, true);
                    /// c.getPlayer().resetSRB();
                    c.getPlayer().setReborns(c.getPlayer().getReborns() - 1000);
                } else {
                    c.getPlayer().dropMessage(6, "You need at least 1000 rebirths for this");
                }
            }
            return 1;
        }
    }

    public static class ClearInv extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 2) {
                c.getPlayer().dropMessage(6, "@clearinv all/equip/use/setup/etc/cash");
                return 0;
            }
            java.util.Map<Pair<Short, Short>, MapleInventoryType> eqs = new HashMap<>();
            switch (splitted[1]) {
                case "all":
                    for (MapleInventoryType type : MapleInventoryType.values()) {
                        for (Item item : c.getPlayer().getInventory(type)) {
                            eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), type);
                        }
                    }
                    break;
                case "equip":
                    for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                        eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), MapleInventoryType.EQUIP);
                    }
                    break;
                case "use":
                    for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                        eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), MapleInventoryType.USE);
                    }
                    break;
                case "setup":
                    for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                        eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), MapleInventoryType.SETUP);
                    }
                    break;
                case "etc":
                    for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                        eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), MapleInventoryType.ETC);
                    }
                    break;
                case "cash":
                    for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                        eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), MapleInventoryType.CASH);
                    }
                    break;
                default:
                    c.getPlayer().dropMessage(6, "@clearinv all/equip/use/setup/etc/cash");
                    break;
            }
            for (Map.Entry<Pair<Short, Short>, MapleInventoryType> eq : eqs.entrySet()) {
                MapleInventoryManipulator.removeFromSlot(c, eq.getValue(), eq.getKey().left, eq.getKey().right, false, false);
            }
            return 1;
        }
    }

    public static class Search extends lul {
    }

    public static class lul extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (splitted.length == 1) {
                c.getPlayer().dropMessage(6, "Provide something to search.");
            } else {
                String search = StringUtil.joinStringFrom(splitted, 1);
                c.getPlayer().dropMessage(6, "Search: " + search + ">>");

                List<String> retItems = new ArrayList<>();
                for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                    if (itemPair != null && itemPair.name != null && itemPair.name.toLowerCase().contains(search.toLowerCase())) {
                        retItems.add(itemPair.itemId + " - " + itemPair.name);
                    }
                }
                if (retItems != null && retItems.size() > 0) {
                    for (String singleRetItem : retItems) {
                        c.getPlayer().dropMessage(6, singleRetItem);
                    }
                } else {
                    c.getPlayer().dropMessage(6, "No Items Found");
                }


            }
            return 0;
        }
    }

    public static class emo extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().setHp(0, c.getPlayer());
            c.getPlayer().updateSingleStat(MapleStat.HP, 0);
            return 1;
        }
    }

    public static class buyscroll extends CommandExecute {

        static int[] blockedids = {5530103, 4000038, 4001551, 4001550, 4001549, 4001548, 4001547, 3012015, 3010191, 3012015, 3012005, 2100030, 2210022, 2210009, 2022166, 1602000, 1602001, 1602002, 1602003, 1602004, 1602005, 1602006, 1602007, 1001076, 5530123, 2046025, 2046026, 2046119, 2046120, 2046251, 2046340, 2046341, 3994385, 4000313, 4001168, 4001168, 4000038, 1112400, 1442120, 1112920, 2043203, 2043303, 2043003, 2043103, 2043703, 2043803, 2041024, 2041025, 2040903, 2040913, 2040303, 2040037, 2040006, 2040007, 2040709, 2040710, 2040711, 2040806, 2040603, 2040507, 2040506, 2040403, 2044103, 2044003, 2044019, 2044303, 2044403, 2044203, 2044603, 2044503, 2044815, 2044908, 2044703, 2040807, 4001116};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 3) {
                c.getPlayer().dropMessage(5, "Format: @buyscroll SCROLLID QUANTITY");
                return 0;
            }
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            if (quantity < 0) {
                c.getPlayer().dropMessage(5, "Quantity must be greater than 0.");
                return 0;
            }
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            for (int i = 0; i < blockedids.length; i++) {
                if (itemId == blockedids[i]) {
                    c.getPlayer().dropMessage(5, "You can't make this item");
                    return 0;
                }
            }

            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " does not exist");
            } else {
                Item item;

                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.USE && c.getPlayer().getCSPoints(1) >= 5000 * quantity && !c.getPlayer().getInventory(MapleInventoryType.USE).isFull()) {
                    c.getPlayer().modifyCSPoints(1, -5000 * quantity);
                    item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);
                    item.setOwner("SCROLL " + c.getPlayer().getName());
                    item.setGMLog(c.getPlayer().getName() + " used @buyscroll");
                    MapleInventoryManipulator.addbyItem(c, item);
                    c.getPlayer().dropMessage(6, "You've lost " + 5000 * quantity + " cash! and have gained " + quantity + " scrolls!");
                } else {
                    c.getPlayer().dropMessage(5, "You can only make USE items! (scrolls) || Make sure you have enough CASH!");
                    return 0;

                }
            }
            return 1;
        }
    }

    public static class TradeHelp extends TradeExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(-2, "[System] : <@offerequip, @offeruse, @offersetup, @offeretc, @offercash> <quantity> <name of the item>");
            return 1;
        }
    }

    public static class gtop100 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                java.sql.Connection con = DatabaseConnection.getConnection();
                try (PreparedStatement ps = (PreparedStatement) con.prepareStatement("SELECT * FROM voterewardsgtop WHERE name = ? LIMIT 1")) {
                    ps.setString(1, c.getAccountName());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            c.getPlayer().setVPoints(c.getPlayer().getVPoints() + 3);
                            c.getPlayer().dropMessage(5, "You have gained 3 votepoints");
                            c.getPlayer().dropMessage(5, "You now have " + c.getPlayer().getVPoints() + " votepoints left");
                        } else {
                            c.getPlayer().dropMessage(5, "You dont seem to have any claims left");
                        }
                    }
                }
                try (PreparedStatement pse = (PreparedStatement) con.prepareStatement("DELETE FROM voterewardsgtop WHERE name = ? LIMIT 1")) {
                    pse.setString(1, c.getAccountName());
                    pse.executeUpdate();
                }
                c.getPlayer().saveToDB(false, false);
            } catch (SQLException ex) {
                Logger.getLogger(PlayerCommand.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 1;
        }
    }


    public static class vote4fame extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getVPoints() >= 1) {
                c.getPlayer().setVPoints(c.getPlayer().getVPoints() - 1);
                c.getPlayer().setFame(c.getPlayer().getFame() + 300);
                c.getPlayer().dropMessage(5, "You've gained 300 fame! Enjoy! You have " + c.getPlayer().getVPoints() + " votepoints left");
            } else {
                c.getPlayer().dropMessage(5, "You must have at least 1 votepoint..");
            }
            return 1;
        }
    }

    public static class ultimateps extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                java.sql.Connection con = DatabaseConnection.getConnection();
                try (PreparedStatement ps = (PreparedStatement) con.prepareStatement("SELECT * FROM voterewards WHERE name = ? LIMIT 1")) {
                    ps.setString(1, c.getAccountName());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            c.getPlayer().setVPoints(c.getPlayer().getVPoints() + 3);
                            c.getPlayer().dropMessage(5, "You have gained 3 votepoints");
                            c.getPlayer().dropMessage(5, "You now have " + c.getPlayer().getVPoints() + " votepoints left");
                        } else {
                            c.getPlayer().dropMessage(5, "You dont seem to have any claims left");
                        }
                    }
                }
                try (PreparedStatement pse = (PreparedStatement) con.prepareStatement("DELETE FROM voterewards WHERE name = ? LIMIT 1")) {
                    pse.setString(1, c.getAccountName());
                    pse.executeUpdate();
                }
                c.getPlayer().saveToDB(false, false);
            } catch (SQLException ex) {
                Logger.getLogger(PlayerCommand.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 1;
        }
    }

    public static class vote4nek extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getVPoints() >= 6) {
                c.getPlayer().setVPoints(c.getPlayer().getVPoints() - 6);
                MapleInventoryManipulator.addById(c, 4001116, (short) 1, "vote4nek " + c.getPlayer().getName());
                c.getPlayer().dropMessage(5, "You've gained 1 NEK! Enjoy! You have " + c.getPlayer().getVPoints() + " votepoints left");
            } else {
                c.getPlayer().dropMessage(5, "You must have at least 6 votepoints..");
            }
            return 1;
        }
    }

    public static class vote4cube extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getVPoints() >= 1) {
                c.getPlayer().setVPoints(c.getPlayer().getVPoints() - 1);
                MapleInventoryManipulator.addById(c, 5062002, (short) 15, "vote4cube");
                c.getPlayer().dropMessage(5, "You've gained 15 super cubes! Enjoy! You have " + c.getPlayer().getVPoints() + " votepoints left");
            } else {
                c.getPlayer().dropMessage(5, "You must have at least 1 votepoint..");
            }
            return 1;
        }
    }

    public static class vote4nx extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getVPoints() >= 4) {
                c.getPlayer().setVPoints(c.getPlayer().getVPoints() - 4);
                c.getPlayer().modifyCSPoints(1, 100000);
                c.getPlayer().dropMessage(5, "You've gained 100000 NX! Enjoy! You have " + c.getPlayer().getVPoints() + " votepoints left");
            } else {
                c.getPlayer().dropMessage(5, "You must have at least 4 votepoints..");
            }
            return 1;
        }
    }


    public static class rb extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 2) {
                c.getPlayer().dropMessage(6, "Sorry, you must be level 200 to rebirth.");
                c.getPlayer().dropMessage(6, "Evan - @rb evan");
                c.getPlayer().dropMessage(6, "Aran - @rb aran");
                c.getPlayer().dropMessage(6, "Dual blade - @rb db");
                c.getPlayer().dropMessage(6, "Explorer - @rb exp");
                c.getPlayer().dropMessage(6, "Mechanic - @rb mech");
                c.getPlayer().dropMessage(6, "Wild Hunter - @rb wh");
                c.getPlayer().dropMessage(6, "Battle Mage - @rb bam");
                c.getPlayer().dropMessage(6, "Demon Slayer - @rb ds");
                c.getPlayer().dropMessage(6, "Cannon Master - @rb cs");
                c.getPlayer().dropMessage(6, "Mercedes - @rb mr");
                c.getPlayer().dropMessage(6, "Phantom - @rb ph");
                c.getPlayer().dropMessage(6, "Jett - @rb jt");
                c.getPlayer().dropMessage(6, "Mihile - @rb mi");
                return 0;
            }

            if (c.getPlayer().getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null || c.getPlayer().getBuffedValue(MapleBuffStat.WATER_SHIELD) != null) {
                c.getPlayer().dropMessage(5, "Please disable Shadow Partner/Mirror Image/Water Shield before rebirthing or changing job.");
            } else {
                if (splitted[1].equalsIgnoreCase("evan") && (c.getPlayer().getLevel() >= 200)) {
                    c.getPlayer().doERB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Evan]");
                } else if (splitted[1].equalsIgnoreCase("aran") && (c.getPlayer().getLevel() >= 200)) {
                    c.getPlayer().doARB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Aran]");
                } else if (splitted[1].equalsIgnoreCase("db") && c.getPlayer().getLevel() >= 200) {
                    if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getPlayer().dropMessage(5, "Please make space in your inventory before rebirthing into a dual blade!");
                        return 0;
                    }
                    c.getPlayer().unequipEverything();
                    c.getPlayer().doDBRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Dual blade]");
                } else if (splitted[1].equalsIgnoreCase("exp") && c.getPlayer().getLevel() >= 200) {
                    c.getPlayer().doEXPRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Explorer]");
                } else if (splitted[1].equalsIgnoreCase("mech") && c.getPlayer().getLevel() >= 200) {
                    c.getPlayer().doMRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Mechanic]");
                } else if (splitted[1].equalsIgnoreCase("wh") && c.getPlayer().getLevel() >= 200) {
                    c.getPlayer().doWHRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Wild Hunter]");
                } else if (splitted[1].equalsIgnoreCase("bam") && c.getPlayer().getLevel() >= 200) {
                    c.getPlayer().doBAMRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Battle Mage]");
                } else if (splitted[1].equalsIgnoreCase("ds") && c.getPlayer().getLevel() >= 200) {
                    c.getPlayer().doDSRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Demon Slayer]");
                } else if (splitted[1].equalsIgnoreCase("cs") && c.getPlayer().getLevel() >= 200) {
                    c.getPlayer().doCANNONRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Cannonneer]");
                } else if (splitted[1].equalsIgnoreCase("mr") && c.getPlayer().getLevel() >= 200) {
                    c.getPlayer().doMERCRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Mercedes]");
                } else if (splitted[1].equalsIgnoreCase("ph") && c.getPlayer().getLevel() >= 200) {
                    c.getPlayer().doPHANTOMRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Phantom]");
                } else if (splitted[1].equalsIgnoreCase("jt") && c.getPlayer().getLevel() >= 200) {
                    c.getPlayer().doJETTRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Jett]");
                } else if (splitted[1].equalsIgnoreCase("mi") && c.getPlayer().getLevel() >= 200) {
                    c.getPlayer().doMIRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed to [Mihile]");
                } else if (splitted[1].equalsIgnoreCase("help")) {
                    c.getPlayer().dropMessage(6, "Sorry, you must be level 200 to rebirth.");
                    c.getPlayer().dropMessage(6, "Evan - @rb evan");
                    c.getPlayer().dropMessage(6, "Aran - @rb aran");
                    c.getPlayer().dropMessage(6, "Dual blade - @rb db");
                    c.getPlayer().dropMessage(6, "Explorer - @rb exp");
                    c.getPlayer().dropMessage(6, "Mechanic - @rb mech");
                    c.getPlayer().dropMessage(6, "Wild Hunter - @rb wh");
                    c.getPlayer().dropMessage(6, "Battle Mage - @rb bam");
                    c.getPlayer().dropMessage(6, "Demon Slayer - @rb ds");
                    c.getPlayer().dropMessage(6, "Cannon Master - @rb cs");
                    c.getPlayer().dropMessage(6, "Mercedes - @rb mr");
                    c.getPlayer().dropMessage(6, "Phantom - @rb ph");
                    c.getPlayer().dropMessage(6, "Jett - @rb jt");
                    c.getPlayer().dropMessage(6, "Mihile - @rb mi");
                } else {
                    c.getPlayer().dropMessage(6, "Sorry, you must be level 200 to rebirth.");
                    c.getPlayer().dropMessage(6, "Evan - @rb evan");
                    c.getPlayer().dropMessage(6, "Aran - @rb aran");
                    c.getPlayer().dropMessage(6, "Dual blade - @rb db");
                    c.getPlayer().dropMessage(6, "Explorer - @rb exp");
                    c.getPlayer().dropMessage(6, "Mechanic - @rb mech");
                    c.getPlayer().dropMessage(6, "Wild Hunter - @rb wh");
                    c.getPlayer().dropMessage(6, "Battle Mage - @rb bam");
                    c.getPlayer().dropMessage(6, "Demon Slayer - @rb ds");
                    c.getPlayer().dropMessage(6, "Cannon Master - @rb cs");
                    c.getPlayer().dropMessage(6, "Mercedes - @rb mr");
                    c.getPlayer().dropMessage(6, "Phantom - @rb ph");
                    c.getPlayer().dropMessage(6, "Jett - @rb jt");
                    c.getPlayer().dropMessage(6, "Mihile - @rb mi");
                }

            }
            return 1;
        }
    }

    public static class skills extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().maxAAllSkills();
            c.getPlayer().dropMessage(6, "Your skills have been maxed!");
            return 1;
        }
    }

    public abstract static class OfferCommand extends TradeExecute {

        protected int invType = -1;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-2, "[Error] : <quantity> <name of item>");
            } else if (c.getPlayer().getLevel() < 70) {
                c.getPlayer().dropMessage(-2, "[Error] : Only level 70+ may use this command");
            } else {
                int quantity = 1;
                try {
                    quantity = Integer.parseInt(splitted[1]);
                } catch (Exception e) { //swallow and just use 1
                }
                String search = StringUtil.joinStringFrom(splitted, 2).toLowerCase();
                Item found = null;
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                for (Item inv : c.getPlayer().getInventory(MapleInventoryType.getByType((byte) invType))) {
                    if (ii.getName(inv.getItemId()) != null && ii.getName(inv.getItemId()).toLowerCase().contains(search)) {
                        found = inv;
                        break;
                    }
                }
                if (found == null) {
                    c.getPlayer().dropMessage(-2, "[Error] : No such item was found (" + search + ")");
                    return 0;
                }
                if (GameConstants.isPet(found.getItemId()) || GameConstants.isRechargable(found.getItemId())) {
                    c.getPlayer().dropMessage(-2, "[Error] : You may not trade this item using this command");
                    return 0;
                }
                if (quantity > found.getQuantity() || quantity <= 0 || quantity > ii.getSlotMax(found.getItemId())) {
                    c.getPlayer().dropMessage(-2, "[Error] : Invalid quantity");
                    return 0;
                }
                if (!c.getPlayer().getTrade().setItems(c, found, (byte) -1, quantity)) {
                    c.getPlayer().dropMessage(-2, "[Error] : This item could not be placed");
                    return 0;
                } else {
                    c.getPlayer().getTrade().chatAuto("[System] : " + c.getPlayer().getName() + " offered " + ii.getName(found.getItemId()) + " x " + quantity);
                }
            }
            return 1;
        }
    }

    public static class OfferEquip extends OfferCommand {

        public OfferEquip() {
            invType = 1;
        }
    }

    public static class OfferUse extends OfferCommand {

        public OfferUse() {
            invType = 2;
        }
    }

    public static class OfferSetup extends OfferCommand {

        public OfferSetup() {
            invType = 3;
        }
    }

    public static class OfferEtc extends OfferCommand {

        public OfferEtc() {
            invType = 4;
        }
    }

    public static class OfferCash extends OfferCommand {

        public OfferCash() {
            invType = 5;
        }
    }
}
