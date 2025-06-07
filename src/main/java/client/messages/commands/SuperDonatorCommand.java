/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.messages.commands;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import handling.channel.ChannelServer;
import handling.world.World;

import java.util.LinkedList;

import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.maps.MapleMap;
import tools.StringUtil;
import tools.packet.CWvsContext;

/**
 * @author Emilyx3
 */
public class SuperDonatorCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERDONATOR;
    }

    public static class itemi extends CommandExecute {

        static int[] blockedids = {5530103, 4000038, 4001551, 4001550, 4001549, 4001548, 4001547, 3010191, 3012015, 3012005, 2022166, 2100030, 2210022, 2210009, 1602000, 1602001, 1602002, 1602003, 1602004, 1602005, 1602006, 1602007, 1001076, 5530123, 2046025, 2046026, 2046119, 2046120, 2046251, 2046340, 2046341, 3994385, 4000313, 4001168, 4001168, 4000038, 1112400, 1442120, 1112920, 4001471, 4000519, 4000520, 1002959, 1532000, 1532001, 1532005, 1532006, 1532045, 1532046, 1532050, 1532051, 1532039, 1522022, 1522019, 1522017, 4000174, 1112309, 1112310, 1112311, 4001126, 4031830, 1002140, 1003274, 1062140, 1042223, 1003142, 1042003, 1062007, 1322013, 4001116};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);

            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            for (int i = 0; i < blockedids.length; i++) {
                if (itemId == blockedids[i]) {
                    c.getPlayer().dropMessage(5, "You can't make this item");
                    return 0;
                }
            }
            if (GameConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "Please purchase a pet from the cash shop instead.");
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " does not exist");
            } else {
                Item item;

                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                } else {
                    item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);
                }
                item.setOwner("SD " + c.getPlayer().getName());
                item.setGMLog(c.getPlayer().getName() + " used $itemi");


                MapleInventoryManipulator.addbyItem(c, item);
                //  } else {
                //        c.getPlayer().dropMessage(5, "can only make items for personal use..");
                //  }
            }
            return 1;
        }
    }

    public static class smega extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            String medal = "";
            Item medalItem = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -49);
            if (medalItem != null) {
                medal = "<" + ii.getName(medalItem.getItemId()) + "> ";
            }
            for (ChannelServer cservs : ChannelServer.getAllInstances()) {
                for (MapleCharacter online : cservs.getPlayerStorage().getAllCharacters()) {
                    online.getClient().getSession().write(CWvsContext.serverNotice(3, c.getChannel(), medal + player.getName() + " : " + StringUtil.joinStringFrom(splitted, 1)));
                }
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
                }
            }
            return 1;
        }
    }


    public static class chat extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append("Super Donor - ");
                sb.append(c.getPlayer().getName());
                sb.append("] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(c.getPlayer().isGM() ? 6 : 5, sb.toString()));
            } else {
                c.getPlayer().dropMessage(6, "Syntax: chat <message>");
                return 0;
            }
            return 1;
        }
    }

    public static class Move extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null && !victim.inPVP() && !c.getPlayer().inPVP() && victim.getGMLevel() < 4) {
                if (splitted.length == 2) {
                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getTruePosition()));
                }
            } else {
                try {
                    int ch = World.Find.findChannel(splitted[1]);
                    if (ch < 0) {
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                        if (target == null) {
                            c.getPlayer().dropMessage(6, "Map does not exist");
                            return 0;
                        }
                        MaplePortal targetPortal = null;
                        if (splitted.length > 2) {
                            try {
                                targetPortal = target.getPortal(Integer.parseInt(splitted[2]));
                            } catch (IndexOutOfBoundsException e) {
                                // noop, assume the gm didn't know how many portals there are
                                c.getPlayer().dropMessage(5, "Invalid portal selected.");
                            } catch (NumberFormatException a) {
                                // noop, assume that the gm is drunk
                            }
                        }
                        if (targetPortal == null) {
                            targetPortal = target.getPortal(0);
                        }
                        c.getPlayer().changeMap(target, targetPortal);
                    } else {
                        victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                        c.getPlayer().dropMessage(6, "Cross changing channel. Please wait.");
                        if (victim.getMapId() != c.getPlayer().getMapId() && victim.getGMLevel() < 4) {
                            final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(victim.getMapId());
                            c.getPlayer().changeMap(mapp, mapp.findClosestPortal(victim.getTruePosition()));
                        }
                        c.getPlayer().changeChannel(ch);
                    }
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "Something went wrong " + e.getMessage());
                    return 0;
                }
            }
            return 1;
        }
    }
}

