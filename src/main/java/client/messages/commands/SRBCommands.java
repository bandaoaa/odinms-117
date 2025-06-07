package client.messages.commands;

import client.MapleClient;
import constants.ServerConstants.PlayerGMRank;
import handling.world.World;
import tools.StringUtil;
import tools.packet.CWvsContext;

/**
 * @author Emilyx3
 */
public class SRBCommands {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SRB;
    }

    public static class spam extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append("SRB - ");
                sb.append(c.getPlayer().getName());
                sb.append("] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(5, sb.toString()));
            } else {
                c.getPlayer().dropMessage(6, "Syntax: chat <message>");
                return 0;
            }
            return 1;
        }
    }


}
