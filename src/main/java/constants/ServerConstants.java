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
package constants;

import server.ServerProperties;

public class ServerConstants {

    public static boolean DEBUG = false; // true
    public static final byte[] NEXON_IP = new byte[]{(byte) 8, (byte) 31, (byte) 98, (byte) 53};
    public static boolean AUTOMATIC_REGISTER; //账号注册
    //public static final byte[] Gateway_IP = new byte[]{(byte) 127, (byte) 0, (byte) 0, (byte) 1};

    /*
     * Specifics which job gives an additional EXP to party
     * returns the percentage of EXP to increase
     */
    public static byte Class_Bonus_EXP(final int job) {
        switch (job) {
            case 501:
            case 530:
            case 531:
            case 532:
            case 2300:
            case 2310:
            case 2311:
            case 2312:
            case 3100:
            case 3110:
            case 3111:
            case 3112:
            case 800:
            case 900:
            case 910:
                return 10;
        }
        return 0;
    }

    // End of Poll
    public static final short MAPLE_VERSION = (short) 117;
    public static final String MAPLE_PATCH = "2";
    public static boolean Use_Fixed_IV = false; // true = disable sniffing, false = server can connect to itself
    public static boolean Use_Localhost = false; // true = packets are logged, false = others can connect to server
    public static final int MIN_MTS = 100; //lowest amount an item can be, GMS = 110
    public static final int MTS_BASE = 0; //+amount to everything, GMS = 500, MSEA = 1000
    public static final int MTS_TAX = 5; //+% to everything, GMS = 10
    public static final int MTS_MESO = 10000; //mesos needed, GMS = 5000

    //master login is only used in GMS: fake account for localhost only
    //master and master2 is to bypass all accounts passwords only if you are under the IPs below

    public static enum PlayerGMRank {

        NORMAL('@', 0),
        SRB('%', 1),
        DONATOR('#', 2),
        SUPERDONATOR('$', 3),
        INTERN('!', 4),
        GM('!', 5),
        SUPERGM('!', 6),
        ADMIN('!', 7);

        private char commandPrefix;
        private int level;

        PlayerGMRank(char ch, int level) {
            commandPrefix = ch;
            this.level = level;
        }

        public char getCommandPrefix() {
            return commandPrefix;
        }

        public int getLevel() {
            return level;
        }
    }

    public static enum CommandType {

        NORMAL(0),
        TRADE(1);
        private int level;

        CommandType(int level) {
            this.level = level;
        }

        public int getType() {
            return level;
        }
    }

    static {
        loadSetting();
    }

    public static void loadSetting() {
        AUTOMATIC_REGISTER = Boolean.parseBoolean(ServerProperties.getProperty("net.sf.odinms.login.AUTOMATIC_REGISTER", "false")); //账号注册
    }
}