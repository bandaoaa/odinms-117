package client.messages.commands;

import client.MapleBuffStat;
import client.MapleClient;
import client.MapleStat;
import constants.ServerConstants.PlayerGMRank;
import handling.world.World;

import java.util.Arrays;

import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CWvsContext;

/**
 * @author Emilyx3
 */
public class DonatorCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.DONATOR;
    }


    public static class healme extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().setHp(c.getPlayer().getStat().getMaxHp(), c.getPlayer());
            c.getPlayer().updateSingleStat(MapleStat.HP, c.getPlayer().getStat().getMaxHp());
            c.getPlayer().getStat().setMp(c.getPlayer().getStat().getMaxMp(), c.getPlayer());
            c.getPlayer().updateSingleStat(MapleStat.MP, c.getPlayer().getStat().getMaxMp());
            return 1;
        }
    }

    public static class chatt extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append("Donor - ");
                sb.append(c.getPlayer().getName());
                sb.append("] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(c.getPlayer().isGM() ? 6 : 5, sb.toString()));
            } else {
                c.getPlayer().dropMessage(6, "Syntax: #chatt <message>");
                return 0;
            }
            return 1;
        }
    }

    public static class rbs extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null || c.getPlayer().getBuffedValue(MapleBuffStat.WATER_SHIELD) != null) {
                c.getPlayer().dropMessage(5, "Please disable Shadow Partner/Mirror Image/Water Shield(the dragon thing) before rebirthing or changing job.");
            } else {
                if ((c.getPlayer().getLevel() >= 150)) {
                    c.getPlayer().doEXPRB();
                    c.getPlayer().dropMessage(6, "Done! You have rebirthed! Use @job to change your job");
                } else {
                    c.getPlayer().dropMessage(6, "Sorry, you must be level 150 to rebirth");

                }

            }
            return 1;
        }
    }

    public static class cleardrop extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            java.util.List<MapleMapObject> items = map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.ITEM));
            for (MapleMapObject itemmo : items) {
                map.removeMapObject(itemmo);
                map.broadcastMessage(CField.removeItemFromMap(itemmo.getObjectId(), 0, c.getPlayer().getId()));
            }
            c.getPlayer().dropMessage(5, "You have destroyed " + items.size() + " items on the ground.");
            return 1;

        }
    }
}


