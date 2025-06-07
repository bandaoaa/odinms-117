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

import client.*;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import client.status.MonsterStatus;

import java.awt.Point;
import java.util.*;

import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Randomizer;
import server.maps.MapleMapObjectType;
import tools.FileoutputUtil;
import tools.packet.CField;

public class GameConstants {

    public static boolean GMS = true; //true = GMS
    public static List<MapleMapObjectType> rangedMapobjectTypes = Collections.unmodifiableList(Arrays.asList(
            MapleMapObjectType.ITEM,
            MapleMapObjectType.MONSTER,
            MapleMapObjectType.DOOR,
            MapleMapObjectType.REACTOR,
            MapleMapObjectType.SUMMON,
            MapleMapObjectType.NPC,
            MapleMapObjectType.MIST,
            MapleMapObjectType.FAMILIAR,
            MapleMapObjectType.EXTRACTOR));
    private static int[] exp = {
            0, 15, 35, 57, 92, 135, 372, 560, 840, 1242, 1242,
            1242, 1242, 1242, 1242, 1490, 1788, 2146, 2575, 3090, 3708,
            4450, 5340, 6408, 7690, 9228, 11074, 13289, 15947, 19136, 19136,
            19136, 19136, 19136, 19136, 22963, 27556, 33067, 39681, 47616, 51425,
            55539, 59982, 64781, 69963, 75560, 81605, 88133, 95184, 102799, 111023,
            119905, 129497, 139857, 151046, 163129, 176180, 190274, 205496, 221936, 239691,
            258866, 279575, 301941, 326097, 352184, 380359, 410788, 443651, 479143, 479143,
            479143, 479143, 479143, 479143, 512683, 548571, 586971, 628059, 672024, 719065,
            769400, 823258, 880886, 942548, 1008526, 1079123, 1154662, 1235488, 1321972, 1414511,
            1513526, 1619473, 1732836, 1854135, 1983924, 2122799, 2271395, 2430393, 2600520, 2782557,
            2977336, 3185749, 3408752, 3647365, 3902680, 4175868, 4468179, 4780951, 5115618, 5473711,
            5856871, 6266852, 6705531, 7174919, 7677163, 8214565, 8789584, 9404855, 10063195, 10063195,
            10063195, 10063195, 10063195, 10063195, 10767619, 11521352, 12327847, 13190796, 14114152, 15102142,
            16159292, 17290443, 18500774, 19795828, 21181536, 22664244, 24250741, 25948292, 27764673, 29708200,
            31787774, 34012918, 36393823, 38941390, 41667288, 44583998, 47704878, 51044219, 54617315, 58440527,
            62531364, 66908559, 71592158, 76603609, 81965862, 87703472, 93842715, 100411706, 107440525, 113895024,
            120728726, 127972450, 135650797, 143789844, 152417235, 161562269, 171256005, 181531366, 192423248, 203968643,
            216206761, 229179167, 242929917, 257505712, 272956055, 289333418, 306693423, 325095029, 344600730, 365276774,
            387193381, 410424983, 435050483, 461153512, 488822722, 518152086, 549241211, 582195683, 617127424, 654155070,
            693404374, 735008637, 779109155, 825855704, 875407047, 927931469, 983607358, 1042623799, 1105181227, 1171492101};
    private static int[] closeness = {0, 1, 3, 6, 14, 31, 60, 108, 181, 287, 434, 632, 891, 1224, 1642, 2161, 2793,
            3557, 4467, 5542, 6801, 8263, 9950, 11882, 14084, 16578, 19391, 22547, 26074,
            30000};
    private static int[] setScore = {0, 10, 100, 300, 600, 1000, 2000, 4000, 7000, 10000};
    public static final int[] rankC = {70000000, 70000001, 70000002, 70000003, 70000004, 70000005, 70000006, 70000007, 70000008, 70000009, 70000010, 70000011, 70000012, 70000013};
    public static final int[] rankB = {70000014, 70000015, 70000016, 70000017, 70000018, 70000021, 70000022, 70000023, 70000024, 70000025, 70000026};
    public static final int[] rankA = {70000027, 70000028, 70000029, 70000030, 70000031, 70000032, 70000033, 70000034, 70000035, 70000036, 70000039, 70000040, 70000041, 70000042};
    public static final int[] rankS = {70000043, 70000044, 70000045, 70000047, 70000048, 70000049, 70000050, 70000051, 70000052, 70000053, 70000054, 70000055, 70000056, 70000057, 70000058, 70000059, 70000060, 70000061, 70000062};
    public static final int[] circulators = {2700000, 2700100, 2700200, 2700300, 2700400, 2700500, 2700600, 2700700, 2700800, 2700900, 2701000};
    private static int[] cumulativeTraitExp = {0, 20, 46, 80, 124, 181, 255, 351, 476, 639, 851, 1084,
            1340, 1622, 1932, 2273, 2648, 3061, 3515, 4014, 4563, 5128,
            5710, 6309, 6926, 7562, 8217, 8892, 9587, 10303, 11040, 11788,
            12547, 13307, 14089, 14883, 15689, 16507, 17337, 18179, 19034, 19902,
            20783, 21677, 22584, 23505, 24440, 25399, 26362, 27339, 28331, 29338,
            30360, 31397, 32450, 33519, 34604, 35705, 36823, 37958, 39110, 40279,
            41466, 32671, 43894, 45135, 46395, 47674, 48972, 50289, 51626, 52967,
            54312, 55661, 57014, 58371, 59732, 61097, 62466, 63839, 65216, 66597,
            67982, 69371, 70764, 72161, 73562, 74967, 76376, 77789, 79206, 80627,
            82052, 83481, 84914, 86351, 87792, 89237, 90686, 92139, 93596, 96000};
    private static int[] mobHpVal = {0, 15, 20, 25, 35, 50, 65, 80, 95, 110, 125, 150, 175, 200, 225, 250, 275, 300, 325, 350,
            375, 405, 435, 465, 495, 525, 580, 650, 720, 790, 900, 990, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800,
            1900, 2000, 2100, 2200, 2300, 2400, 2520, 2640, 2760, 2880, 3000, 3200, 3400, 3600, 3800, 4000, 4300, 4600, 4900, 5200,
            5500, 5900, 6300, 6700, 7100, 7500, 8000, 8500, 9000, 9500, 10000, 11000, 12000, 13000, 14000, 15000, 17000, 19000, 21000, 23000,
            25000, 27000, 29000, 31000, 33000, 35000, 37000, 39000, 41000, 43000, 45000, 47000, 49000, 51000, 53000, 55000, 57000, 59000, 61000, 63000,
            65000, 67000, 69000, 71000, 73000, 75000, 77000, 79000, 81000, 83000, 85000, 89000, 91000, 93000, 95000, 97000, 99000, 101000, 103000,
            105000, 107000, 109000, 111000, 113000, 115000, 118000, 120000, 125000, 130000, 135000, 140000, 145000, 150000, 155000, 160000, 165000, 170000, 175000, 180000,
            185000, 190000, 195000, 200000, 205000, 210000, 215000, 220000, 225000, 230000, 235000, 240000, 250000, 260000, 270000, 280000, 290000, 300000, 310000, 320000,
            330000, 340000, 350000, 360000, 370000, 380000, 390000, 400000, 410000, 420000, 430000, 440000, 450000, 460000, 470000, 480000, 490000, 500000, 510000, 520000,
            530000, 550000, 570000, 590000, 610000, 630000, 650000, 670000, 690000, 710000, 730000, 750000, 770000, 790000, 810000, 830000, 850000, 870000, 890000, 910000};
    private static int[] pvpExp = {0, 3000, 6000, 12000, 24000, 48000, 960000, 192000, 384000, 768000};
    private static int[] guildexp = {0, 20000, 160000, 540000, 1280000, 2500000, 4320000, 6860000, 10240000, 14580000};
    private static int[] mountexp = {0, 6, 25, 50, 105, 134, 196, 254, 263, 315, 367, 430, 543, 587, 679, 725, 897, 1146, 1394, 1701, 2247,
            2543, 2898, 3156, 3313, 3584, 3923, 4150, 4305, 4550};
    //public static int[] itemBlock = {4001168, 5220013, 3993003, 2340000, 2049100, 4001129, 2040037, 2040006, 2040007, 2040303, 2040403, 2040506, 2040507, 2040603, 2040709, 2040710, 2040711, 2040806, 2040903, 2041024, 2041025, 2043003, 2043103, 2043203, 2043303, 2043703, 2043803, 2044003, 2044103, 2044203, 2044303, 2044403, 2044503, 2044603, 2044908, 2044815, 2044019, 2044703};
    public static int[] cashBlock = {5220084, 5220092}; //miracle cube and stuff
    public static int JAIL = 910310300, MAX_BUFFSTAT = 8;
    public static String[] RESERVED = {"Rental", "Donor", "MapleNews"};
    public static String[] stats = {"tuc", "reqLevel", "reqJob", "reqSTR", "reqDEX", "reqINT", "reqLUK", "reqPOP", "cash", "cursed", "success", "setItemID", "equipTradeBlock", "durability", "randOption", "randStat", "masterLevel", "reqSkillLevel", "elemDefault", "incRMAS", "incRMAF", "incRMAI", "incRMAL", "canLevel", "skill", "charmEXP"};
    public static final int[] hyperTele = {10000, 20000, 30000, 40000, 50000, 1000000, 1010000, 1020000, 2000000, //Maple Island
            104000000, 104010000, 104010100, 104010200, 104020000, 103010100, 103010000, 103000000, 103050000, 103020000, 103020020, 103020100, 103020200, 103020300, 103020310, 103020320, 103020400, 103020410, 103020420, 103030000, 103030100, 103030200, 103030300, 103030400, 102000000, 102010000, 102010100, 102020000, 102020100, 102020200, 102020300, 102020400, 102020500, 102040000, 102040100, 102040200, 102040300, 102040400, 102040500, 102040600, 102030000, 102030100, 102030200, 102030300, 102030400, 101000000, 101010000, 101010100, 101020000, 101020100, 101020200, 101020300, 101030000, 101030100, 101030200, 101030300, 101030400, 101030500, 101030101, 101030201, 101040000, 101040100, 101040200, 101040300, 101040310, 101040320, 101050000, 101050400, 100000000, 100010000, 100010100, 100020000, 100020100, 100020200, 100020300, 100020400, 100020500, 100020401, 100020301, 100040000, 100040100, 100040200, 100040300, 100040400, 100020101, 106020000, 120010100, 120010000, 120000000, 120020000, 120020100, 120020200, 120020300, 120020400, 120020500, 120020600, 120020700, 120030000, 120030100, 120030200, 120030300, 120030400, 120030500, //Victoria Island
            105000000, 105010000, 105010100, 105020000, 105020100, 105020200, 105020300, 105020400, 105020500, 105030000, 105030100, 105030200, 105030300, 105030400, 105030500, 105100000, 105100100, //Sleepy Wood
            120000100, 120000101, 120000102, 120000103, 120000104, 120000201, 120000202, 120000301, //Nautilus
            103040000, 103040100, 103040101, 103040102, 103040103, 103040200, 103040201, 103040202, 103040203, 103040300, 103040301, 103040302, 103040303, 103040400, //Kerning Square
            200000000, 200010000, 200010100, 200010110, 200010120, 200010130, 200010111, 200010121, 200010131, 200010200, 200010300, 200010301, 200010302, 200020000, 200030000, 200040000, 200050000, 200060000, 200070000, 200080000, 200000100, 200000200, 200000300, 200100000, 200080100, 200080200, 200081500, 200082200, 200082300, 211000000, 211000100, 211000200, 211010000, 211020000, 211030000, 211040000, 211050000, 211040100, 211040200, 921120000, //Orbis
            211040300, 211040400, 211040500, 211040600, 211040700, 211040800, 211040900, 211041000, 211041100, 211041200, 211041300, 211041400, 211041500, 211041600, 211041700, 211041800, 211041900, 211042000, 211042100, 211042200, 211042300, 211042400, 280030000, 211060000, //Dead Mine
            211060010, 211060100, 211060200, 211060201, 211060300, 211060400, 211060401, 211060410, 211060500, 211060600, 211060601, 211060610, 211060620, 211060700, 211060800, 211060801, 211060810, 211060820, 211060830, 211060900, 211061000, 211061001, 211070000, //Lion King's Castle
            220000000, 220000100, 220000300, 220000400, 220000500, 220010000, 220010100, 220010200, 220010300, 220010400, 220010500, 220010600, 220010700, 220010800, 220010900, 220011000, 220020000, 220020100, 220020200, 220020300, 220020400, 220020500, 220020600, 220030100, 220030200, 220030300, 220030400, 220030000, 220040000, 220040100, 220040200, 220040300, 220040400, 220050000, 220050100, 220050200, 221023200, 221022300, 221022200, 221021700, 221021600, 221021100, 221020000, 221000000, 221030000, 221030100, 221030200, 221030300, 221030400, 221030500, 221030600, 221040000, 221040100, 221040200, 221040300, 221040400, 222000000, 222010000, 222010001, 222010002, 222010100, 222010101, 222010102, 222010200, 222010201, 222010300, 222010400, 222020300, 222020200, 222020100, 222020000, //Ludas Lake
            220050300, 220060000, 220060100, 220060200, 220060300, 220060400, 220070000, 220070100, 220070200, 220070300, 220070400, 220080000, 220080001, //Clock Tower Lower Floor
            300000100, 300000000, 300010000, 300010100, 300010200, 300010400, 300020000, 300020100, 300020200, 300030000, 300030100, 300010410, 300020210, 300030200, 300030300, 300030310, //Ellin Forest
            230010000, 230010100, 230010200, 230010201, 230010300, 230010400, 230020000, 230020100, 230020200, 230020201, 230020300, 230030000, 230030100, 230030101, 230030200, 230040000, 230040100, 230040200, 230040300, 230040400, 230040410, 230040420, 230000000, //Aqua Road
            250000000, 250000100, 250010000, 250010100, 250010200, 250010300, 250010301, 250010302, 250010303, 250010304, 250010400, 250010500, 250010501, 250010502, 250010503, 250010600, 250010700, 250020000, 250020100, 250020200, 250020300, 251000000, 251000100, 251010000, 251010200, 251010300, 251010400, 251010401, 251010402, 251010403, 251010500, //Mu Lung Garden
            240010100, 240010200, 240010300, 240010400, 240010500, 240010600, 240010700, 240010800, 240010900, 240011000, 240020000, 240020100, 240020101, 240020200, 240020300, 240020400, 240020401, 240020500, 240030000, 240030100, 240030101, 240030102, 240030200, 240030300, 240040000, 240040100, 240040200, 240040300, 240040400, 240040500, 240040510, 240040511, 240040520, 240040521, 240040600, 240040700, 240050000, 240010000, 240000000, //Minar Forest
            240070000, 240070010, 240070100, 240070200, 240070300, 240070400, 240070500, 240070600, //Neo City
            260010000, 260010100, 260010200, 260010300, 260010400, 260010500, 260010600, 260010700, 260020000, 260020100, 260020200, 260020300, 260020400, 260020500, 260020600, 260020610, 260020620, 260020700, 261000000, 260000000, 926010000, 261010000, 261010001, 261010002, 261010003, 261010100, 261010101, 261010102, 261010103, 261020000, 261020100, 261020200, 261020300, 261020400, 261020500, 261020600, 261020700, //Nihal Desert
            270000000, 270000100, 270010000, 270010100, 270010110, 270010111, 270010200, 270010210, 270010300, 270010310, 270010400, 270010500, 270020000, 270020100, 270020200, 270020210, 270020211, 270020300, 270020310, 270020400, 270020410, 270020500, 270030000, 270030100, 270030110, 270030200, 270030210, 270030300, 270030310, 270030400, 270030410, 270030411, 270030500, 270040000, 270050000, //Temple of Time
            271000000, 271000100, 271000200, 271000210, 271000300, 271020000, 271020100, 271010000, 271010100, 271010200, 271010300, 271010301, 271010400, 271010500, 271030000, 271030100, 271030101, 271030102, 271030200, 271030201, 271030300, 271030310, 271030320, 271030400, 271030410, 271030500, 271030510, 271030520, 271030530, 271030540, 271030600, 271040000, 271040100, //Gate of Future
            130000000, 130000100, 130000110, 130000120, 130000200, 130000210, 130010000, 130010010, 130010020, 130010100, 130010110, 130010120, 130010200, 130010210, 130010220, 130020000, 130030005, 130030006, 130030000, //Ereve
            140000000, 140010000, 140010100, 140010200, 140020000, 140020100, 140020200, 140030000, 140090000, 140020300, //Rien
            310000000, 310000010, 310020000, 310020100, 310020200, 310030000, 310030100, 310030110, 310030200, 310030300, 310030310, 310040000, 310040100, 310040110, 310040200, 310040300, 310040400, 310050000, 310050100, 310050200, 310050300, 310050400, 310050500, 310050510, 310050520, 310050600, 310050700, 310050800, 310060000, 310060100, 310060110, 310060120, 310060200, 310060210, 310060220, 310060300, 310010000//Edelstein
    };

    public static int getExpNeededForLevel(int level) {
        if (level < 0 || level >= exp.length) {
            return Integer.MAX_VALUE;
        }
        return exp[level];
    }

    public static int getSkillLevel(int level) {
        if ((level >= 70) && (level < 120))
            return 2;
        if ((level >= 120) && (level < 200))
            return 3;
        if (level == 200) {
            return 4;
        }
        return 1;
    }

    public static int getGuildExpNeededForLevel(int level) {
        if (level < 0 || level >= guildexp.length) {
            return Integer.MAX_VALUE;
        }
        return guildexp[level];
    }

    public static boolean canBeStolen(int a1) {
        return canBeStolen(a1, 0);
    }

    public static boolean canBeStolen(int a1, int targetJob) {
        if (a1 >= 90000000) {
            return false;
        }
        switch (a1) {
            case 1101004: //極速武器
            case 1121000: //楓葉祝福
            case 1121010: //鬥氣爆發
            case 1121011: //楓葉淨化

            case 1201004: //極速武器
            case 1211002: //屬性攻擊
            case 1211004: //烈焰之劍
            case 1211006: //寒冰之劍
            case 1221000: //楓葉祝福
            case 1221004: //聖靈之劍
            case 1221012: //楓葉淨化

            case 1301004: //極速武器
            case 1321000: //楓葉祝福
            case 1321010: //楓葉淨化

            case 2111005: //極速詠唱
            case 2121000: //楓葉祝福
            case 2121004: //魔力無限
            case 2121008: //楓葉淨化

            case 2221004: //魔力無限
            case 2221008: //楓葉淨化

            case 2311006: //極速詠唱
            case 2321000: //楓葉祝福
            case 2321004: //魔力無限
            case 2321009: //楓葉淨化

            case 3101002: //快速之弓
            case 3101004: //無形之箭
            case 3121000: //楓葉祝福
            case 3121007: //爆發
            case 3121009: //楓葉淨化

            case 3201004: //無形之箭
            case 3221006: //爆發
            case 3221008: //楓葉淨化

            case 4111002: //影分身
            case 4111009: //無形鏢
            case 4121009: //楓葉淨化

            case 4201002: //快速之刀
            case 4221008: //楓葉淨化

            case 4341008: //楓葉淨化

            case 5001005: //衝鋒
            case 5111002: //能量暴擊
            case 5121008:

            case 5211011: //召喚船員
            case 5211015: //召喚船員
            case 5221010: //楓葉淨化

            case 5921003:
            case 5921010:
                return false;
        }
        int job = a1 / 10000;
        if (((targetJob > 0) && (!isJobFamily(job, targetJob))) || (!isAdventurer(job)) || (isCannon(job)) || (isJett(job))) {
            return false;
        }
        int v1 = a1 / 1000 % 10;
        if ((v1 <= 0) || (v1 == 9) || (job / 1000 > 0) || (a1 / 10000 / 100 == 24) || (a1 / 10000 / 1000 > 0) || (a1 / 10000 == 2003) || (a1 % 1000 <= 0) || (a1 == 2001) || (a1 == 2002) || (a1 == 3001) || (a1 == 2003) || (a1 / 10 == 43) || (a1 / 10 == 53) || (a1 == 501) || (a1 % 1000 / 100 == 9) || (!isAdventurer(a1 / 10000))) {
            return false;
        }
        return isApplicableSkill(a1);
    }

    /*
    幻影複製技能相關
    */
    public static int getPhantomBookSlot(int i) {
        switch (i) {
            case 1:
            case 2:
                return 4;
            case 3:
                return 3;
            case 4:
                return 2;
        }
        return 0;
    }

    /*
    幻影複製技能相關
    */
    public static int getPhantomBookSkill(int slot) {
        switch (slot) {
            case 1:
                return 24001001; //盜亦有道 Ⅰ
            case 2:
                return 24101001; //盜亦有道 Ⅱ
            case 3:
                return 24111001; //盜亦有道 Ⅲ
            case 4:
                return 24121001; //盜亦有道 Ⅳ
        }
        return 0;
    }

    /*
    幻影複製技能相關
    */
    public static int getPhantomBook(int skillid) {
        int jobid = skillid / 10000;
        if (jobid % 100 == 0) {
            return 1;
        }
        if (jobid % 10 == 2) {
            return 4;
        }
        if (jobid % 10 == 1) {
            return 3;
        }
        if (jobid % 10 == 0) {
            return 2;
        }
        return 0;
    }

    public static int getPVPExpNeededForLevel(int level) {
        if (level < 0 || level >= pvpExp.length) {
            return Integer.MAX_VALUE;
        }
        return pvpExp[level];
    }

    public static int getClosenessNeededForLevel(int level) {
        return closeness[level - 1];
    }

    public static int getMountExpNeededForLevel(int level) {
        return mountexp[level - 1];
    }

    public static int getTraitExpNeededForLevel(int level) {
        if (level < 0 || level >= cumulativeTraitExp.length) {
            return Integer.MAX_VALUE;
        }
        return cumulativeTraitExp[level];
    }

    public static int getSetExpNeededForLevel(int level) {
        if (level < 0 || level >= setScore.length) {
            return Integer.MAX_VALUE;
        }
        return setScore[level];
    }

    public static int getMonsterHP(int level) {
        if (level < 0 || level >= mobHpVal.length) {
            return Integer.MAX_VALUE;
        }
        return mobHpVal[level];
    }

    public static int getBookLevel(int level) {
        return (int) ((5 * level) * (level + 1));
    }

    public static int getTimelessRequiredEXP(int level) {
        return 70 + (level * 10);
    }

    public static int getReverseRequiredEXP(int level) {
        return 60 + (level * 5);
    }

    public static int getProfessionEXP(int level) {
        return ((100 * level * level) + (level * 400)) / 2;
    }

    public static boolean isHarvesting(int itemId) {
        return itemId >= 1500000 && itemId < 1520000;
    }

    public static int maxViewRangeSq() {
        return 1000000; // 1024 * 768
    }

    public static int maxViewRangeSq_Half() {
        return 500000; // 800 * 800
    }

    public static boolean isJobFamily(int baseJob, int currentJob) {
        return currentJob >= baseJob && currentJob / 100 == baseJob / 100;
    }

    public static boolean isKOC(int job) {
        return job >= 1000 && job < 2000;
    }

    public static boolean isEvan(int job) {
        return job == 2001 || (job >= 2200 && job <= 2218);
    }

    public static boolean isMercedes(int job) {
        return job == 2002 || (job >= 2300 && job <= 2312);
    }

    public static boolean isDemon(int job) {
        return job == 3001 || (job >= 3100 && job <= 3112);
    }

    public static boolean isAran(int job) {
        return job >= 2000 && job <= 2112 && job != 2001 && job != 2002;
    }

    public static boolean isResist(int job) {
        return job >= 3000 && job <= 3512;
    }

    public static boolean isAdventurer(int job) {
        return job >= 0 && job < 1000;
    }

    public static boolean isCannon(int job) {
        return (job == 501) || (job / 10 == 53);
    }

    public static boolean isJett(int job) {
        return (job == 508) || (job / 10 == 57);
    }

    public static boolean isPhantom(int job) {
        return (job == 2003) || (job / 100 == 24);
    }

    public static boolean isMihile(int job) {
        return (job == 5000) || (job / 100 == 51);
    }

    public static int getBuffDelay(int skill) {
        switch (skill) {
            case 24111002: //幸運幻影
            case 24111003: //幸運卡牌守護
            case 24111005: //月光賜福
            case 24121004: //艾麗亞祝禱
            case 24121008: //楓葉祝福
                return 1000;
        }
        return 0;
    }

    public static int getJudgmentStat(int buffid, int stat) {
        switch (stat) {
            case 1:
                return buffid == 20031209 ? 5 : 10; //卡牌審判
            case 2:
                return buffid == 20031209 ? 10 : 20;
            case 3:
                return 2020;
            case 4:
                return 100;
        }
        return 0;
    }

    /*
    恢復類技能檢測
    */
    public static boolean isRecoveryIncSkill(int id) {
        switch (id) {
            case 1110000: //強化恢復
            case 5100013: //耐久
            case 5700005: //血脈通暢
            case 11110000: //魔力恢復
            case 20020109: //精靈的回復
            case 31110009: //強化惡魔之力
            case 51110000: //魔力恢復
                return true;
        }
        return false;
    }

    public static boolean isLinkedAranSkill(int id) {
        return getLinkedAranSkill(id) != id;
    }

    /*
    連續技能
    */
    public static int getLinkedAranSkill(int id) {
        switch (id) {
            case 5201005: //緩降術
                return 5201011; //飛翼
            case 5211015: //召喚船員
            case 5211016: //召喚船員
                return 5211011; //召喚船員
            case 5300007:
                return 5301001; //火藥桶破壞
            case 5320011:
                return 5321004; //雙胞胎猴子
            case 5710012:
                return 5711002; //猛虎衝
            case 21110007:
            case 21110008:
                return 21110002; //伺機攻擊
            case 21120009:
            case 21120010:
                return 21120002; //終極攻擊
            case 23101007:
                return 23101001; //昇龍刺擊
            case 23111009:
            case 23111010:
                return 23111008; //元素騎士
            case 24111008:
                return 24111006; //國王突刺
            case 24121010:
                return 24121003; //最終的夕陽
            case 30010183:
            case 30010184:
            case 30010186:
                return 30010110;
            case 31001006:
            case 31001007:
            case 31001008:
                return 31000004; //惡魔狂斬
            case 32001007:
            case 32001008:
            case 32001009:
            case 32001010:
            case 32001011:
                return 32001001; //最終攻擊
            case 33101006:
            case 33101007:
                return 33101005; //咆哮
            case 33101008: //地雷(hidden 自動爆炸)
                return 33101004; //地雷
            case 35101009: //強化的火焰發射
            case 35101010: //強化的加格林 Shot
                return 35100008; //重裝武器精通
            case 35111009:
            case 35111010:
                return 35111001; //賽特拉特
            case 35121013:
                return 35111004; //合金盔甲: 重機槍
            case 35121011: //機器人工廠 : 機器人
                return 35121009; //機器人工廠 : RM1
            case 20010022: //龍飛行
                return 80001000;
        }
        return id;
    }

    public static boolean isForceIncrease(int skillid) {
        switch (skillid) {
            case 31000004: //惡魔狂斬
            case 31001006:
            case 31001007:
            case 31001008:

            case 30010166:
            case 30011167:
            case 30011168:
            case 30011169:
            case 30011170:
                return true;
        }
        return false;
    }

    public static int getBOF_ForJob(int job) {
        return PlayerStats.getSkillByJob(12, job);
    }

    public static int getEmpress_ForJob(int job) {
        return PlayerStats.getSkillByJob(73, job);
    }

    public static boolean isElementAmp_Skill(int skill) {
        switch (skill) {
            case 2110001: //魔力激發
            case 2210001: //魔力激發
            case 12110001: //魔力激發
            case 22150000: //魔力激發
                return true;
        }
        return false;
    }

    public static int getMPEaterForJob(int job) {
        switch (job) {
            case 210:
            case 211:
            case 212:
                return 2100000; //魔力吸收
            case 220:
            case 221:
            case 222:
                return 2200000; //魔力吸收
            case 230:
            case 231:
            case 232:
                return 2300000; //魔力吸收
        }
        return 2100000; //魔力吸收
    }

    public static int getJobShortValue(int job) {
        if (job >= 1000) {
            job -= (job / 1000) * 1000;
        }
        job /= 100;
        if (job == 4) { // For some reason dagger/ claw is 8.. IDK
            job *= 2;
        } else if (job == 3) {
            job += 1;
        } else if (job == 5) {
            job += 11; // 16
        }
        return job;
    }

    public static boolean isPyramidSkill(int skill) {
        return isBeginnerJob(skill / 10000) && skill % 10000 == 1020;
    }

    //巨大藥水相關技能
    public static boolean isInflationSkill(final int skill) {
        return isBeginnerJob(skill / 10000) && (skill % 10000 == 1092 || skill % 10000 == 1094 || skill % 10000 == 1095);
    }

    //道場相關技能
    public static boolean isMulungSkill(int skill) {
        return isBeginnerJob(skill / 10000) && (skill % 10000 == 1009 || skill % 10000 == 1010 || skill % 10000 == 1011);
    }

    //冰騎士相關技能
    public static boolean isIceKnightSkill(int skill) {
        return isBeginnerJob(skill / 10000) && (skill % 10000 == 1098 || skill % 10000 == 97 || skill % 10000 == 99 || skill % 10000 == 100 || skill % 10000 == 103 || skill % 10000 == 104 || skill % 10000 == 1105);
    }

    public static boolean isThrowingStar(int itemId) {
        return itemId / 10000 == 207;
    }

    public static boolean isBullet(int itemId) {
        return itemId / 10000 == 233;
    }

    public static boolean isRechargable(int itemId) {
        return isThrowingStar(itemId) || isBullet(itemId);
    }

    public static boolean isOverall(int itemId) {
        return itemId / 10000 == 105;
    }

    public static boolean isPet(int itemId) {
        return itemId / 10000 == 500;
    }

    public static boolean exItemGather(int itemId) { //小背包
        return itemId >= 4330000 && itemId <= 4330019;
    }

    public static boolean isArrowForCrossBow(int itemId) {
        return itemId >= 2061000 && itemId < 2062000;
    }

    public static boolean isArrowForBow(int itemId) {
        return itemId >= 2060000 && itemId < 2061000;
    }

    public static boolean isMagicWeapon(int itemId) {
        int s = itemId / 10000;
        return s == 137 || s == 138;
    }

    public static boolean isWeapon(int itemId) {
        return itemId >= 1300000 && itemId < 1600000;
    }

    public static MapleInventoryType getInventoryType(int itemId) {
        byte type = (byte) (itemId / 1000000);
        if (type < 1 || type > 5) {
            return MapleInventoryType.UNDEFINED;
        }
        return MapleInventoryType.getByType(type);
    }

    public static boolean isInBag(int slot, byte type) {
        return ((slot >= 101 && slot <= 512) && type == MapleInventoryType.ETC.getType());
    }

    public static MapleWeaponType getWeaponType(int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        switch (cat) { // 39, 50, 51 ??
            case 30:
                return MapleWeaponType.SWORD1H;
            case 31:
                return MapleWeaponType.AXE1H;
            case 32:
                return MapleWeaponType.BLUNT1H;
            case 33:
                return MapleWeaponType.DAGGER;
            case 34:
                return MapleWeaponType.KATARA;
            case 35:
                return MapleWeaponType.MAGIC_ARROW; // can be magic arrow or cards
            case 36:
                return MapleWeaponType.CANE;
            case 37:
                return MapleWeaponType.WAND;
            case 38:
                return MapleWeaponType.STAFF;
            case 40:
                return MapleWeaponType.SWORD2H;
            case 41:
                return MapleWeaponType.AXE2H;
            case 42:
                return MapleWeaponType.BLUNT2H;
            case 43:
                return MapleWeaponType.SPEAR;
            case 44:
                return MapleWeaponType.POLE_ARM;
            case 45:
                return MapleWeaponType.BOW;
            case 46:
                return MapleWeaponType.CROSSBOW;
            case 47:
                return MapleWeaponType.CLAW;
            case 48:
                return MapleWeaponType.KNUCKLE;
            case 49:
                return MapleWeaponType.GUN;
            case 52:
                return MapleWeaponType.DUAL_BOW;
            case 53:
                return MapleWeaponType.CANNON;
        }
        return MapleWeaponType.NOT_A_WEAPON;
    }

    public static boolean isShield(int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        return cat == 9;
    }

    public static boolean isEquip(int itemId) {
        return itemId / 1000000 == 1;
    }

    public static boolean isCleanSlate(int itemId) {
        return itemId / 100 == 20490;
    }

    public static boolean isAccessoryScroll(int itemId) {
        return itemId / 100 == 20492;
    }

    public static boolean isInnocence(int itemId) {
        return itemId == 2049600 || itemId == 2049601 || itemId == 2049604;
    }

    public static boolean isChaosScroll(int itemId) {
        if (itemId >= 2049105 && itemId <= 2049110) {
            return false;
        }
        return itemId / 100 == 20491 || itemId == 2040126;
    }

    public static int getChaosNumber(int itemId) {
        return itemId == 2049116 ? 10 : 5;
    }

    public static boolean isEquipScroll(int scrollId) {
        return scrollId / 100 == 20493;
    }

    public static boolean isPotentialScroll(int scrollId) {
        return scrollId / 100 == 20494 || scrollId / 100 == 20497 || scrollId == 5534000;
    }

    public static boolean isSpecialScroll(int scrollId) {
        switch (scrollId) {
            case 2040727: //鞋子防滑卷軸10%
            case 2041058: //披風防寒卷軸10%

            case 2049600: //回真卷軸 70%
            case 2049601: //回真卷軸 20%
            case 2049604: //回真卷軸 60%

            case 2530000: //幸運日卷軸
            case 2530001: //開心DAY 幸運卷軸
            case 2530002: //幸運日卷軸

            case 2531000: //保護卷軸

            case 2532000: //安全卷軸

            case 5063000: //幸運鑰匙
            case 5063100: //幸運保護券

            case 5064000: //裝備保護卷軸
            case 5064002: //星光裝備保護卷軸

            case 5064100: //安全盾牌卷軸
            case 5064101: //星光安全盾牌卷軸

            case 5064200: //完美回真卡
            case 5064201: //星光回真卷軸

            case 5064300: //卷軸保護卡
            case 5064301: //星光卷軸保護卡
                return true;
        }
        return false;
    }

    public static boolean isTwoHanded(int itemId) {
        switch (getWeaponType(itemId)) {
            case AXE2H:
            case GUN:
            case KNUCKLE:
            case BLUNT2H:
            case BOW:
            case CLAW:
            case CROSSBOW:
            case POLE_ARM:
            case SPEAR:
            case SWORD2H:
            case CANNON:
                //case DUAL_BOW: //magic arrow
                return true;
            default:
                return false;
        }
    }

    public static boolean isTownScroll(int id) {
        return id >= 2030000 && id < 2040000;
    }

    public static boolean isUpgradeScroll(int id) {
        return id >= 2040000 && id < 2050000;
    }

    public static boolean isGun(int id) {
        return id >= 1492000 && id < 1500000;
    }

    public static boolean isUse(int id) {
        return id >= 2000000 && id < 3000000;
    }

    public static boolean isSummonSack(int id) {
        return id / 10000 == 210;
    }

    public static boolean isMonsterCard(int id) {
        return id / 10000 == 238;
    }

    public static boolean isSpecialCard(int id) {
        return id / 1000 >= 2388;
    }

    public static int getCardShortId(int id) {
        return id % 10000;
    }

    public static boolean isGem(int id) {
        return id >= 4250000 && id <= 4251402;
    }

    public static boolean isOtherGem(int id) {
        switch (id) {
            case 4001174:
            case 4001175:
            case 4001176:
            case 4001177:
            case 4001178:
            case 4001179:
            case 4001180:
            case 4001181:
            case 4001182:
            case 4001183:
            case 4001184:
            case 4001185:
            case 4001186:
            case 4031980:
            case 2041058:
            case 2040727:
            case 1032062:
            case 4032334:
            case 4032312:
            case 1142156:
            case 1142157:
                return true; //mostly quest items
        }
        return false;
    }

    public static boolean isCustomQuest(int id) {
        return id > 99999;
    }

    public static int getTaxAmount(int meso) {
        if (meso >= 100000000) {
            return (int) Math.round(0.06 * meso);
        } else if (meso >= 25000000) {
            return (int) Math.round(0.05 * meso);
        } else if (meso >= 10000000) {
            return (int) Math.round(0.04 * meso);
        } else if (meso >= 5000000) {
            return (int) Math.round(0.03 * meso);
        } else if (meso >= 1000000) {
            return (int) Math.round(0.018 * meso);
        } else if (meso >= 100000) {
            return (int) Math.round(0.008 * meso);
        }
        return 0;
    }

    public static int EntrustedStoreTax(int meso) {
        if (meso >= 100000000) {
            return (int) Math.round(0.03 * meso);
        } else if (meso >= 25000000) {
            return (int) Math.round(0.025 * meso);
        } else if (meso >= 10000000) {
            return (int) Math.round(0.02 * meso);
        } else if (meso >= 5000000) {
            return (int) Math.round(0.015 * meso);
        } else if (meso >= 1000000) {
            return (int) Math.round(0.009 * meso);
        } else if (meso >= 100000) {
            return (int) Math.round(0.004 * meso);
        }
        return 0;
    }

    public static byte gachaponRareItem(int id) {
        switch (id) {
            case 2340000: // White Scroll
            case 2049100: // Chaos Scroll
            case 2049000: // Reverse Scroll
            case 2049001: // Reverse Scroll
            case 2049002: // Reverse Scroll
            case 2040006: // Miracle
            case 2040007: // Miracle
            case 2040303: // Miracle
            case 2040403: // Miracle
            case 2040506: // Miracle
            case 2040507: // Miracle
            case 2040603: // Miracle
            case 2040709: // Miracle
            case 2040710: // Miracle
            case 2040711: // Miracle
            case 2040806: // Miracle
            case 2040903: // Miracle
            case 2041024: // Miracle
            case 2041025: // Miracle
            case 2043003: // Miracle
            case 2043103: // Miracle
            case 2043203: // Miracle
            case 2043303: // Miracle
            case 2043703: // Miracle
            case 2043803: // Miracle
            case 2044003: // Miracle
            case 2044103: // Miracle
            case 2044203: // Miracle
            case 2044303: // Miracle
            case 2044403: // Miracle
            case 2044503: // Miracle
            case 2044603: // Miracle
            case 2044908: // Miracle
            case 2044815: // Miracle
            case 2044019: // Miracle
            case 2044703: // Miracle
                return 2;
            //1 = wedding msg o.o
        }
        return 0;
    }

    public static int[] tenPercent = {
            //10% scrolls
            2040002,
            2040005,
            2040026,
            2040031,
            2040100,
            2040105,
            2040200,
            2040205,
            2040612,
            2040619,
            2040622,
            2040627,
            2040702,
            2040705,
            2040708,
            2040727,
            2040802,
            2040805,
            2040816,
            2040825,
            2040902,
            2040915,
            2040920,
            2040925,
            2040928,
            2040933,
            2044602,
            2044702,
            2044802,
            2044809,
            2044902,
            2045302,
            2048002,
            2048005
    };
    public static int[] fishingReward = {
            0, 100, // Meso
            1, 100, // EXP
            2022179, 1, // Onyx Apple
            1302021, 5, // Pico Pico Hammer
            1072238, 1, // Voilet Snowshoe
            1072239, 1, // Yellow Snowshoe
            2049100, 2, // Chaos Scroll
            2430144, 1,
            2290285, 1,
            2028062, 1,
            2028061, 1,
            2049301, 1, // Equip Enhancer Scroll
            2049401, 1, // Potential Scroll
            1302000, 3, // Sword
            1442011, 1, // Surfboard
            4000517, 8, // Golden Fish
            4000518, 10, // Golden Fish Egg
            4031627, 2, // White Bait (3cm)
            4031628, 1, // Sailfish (120cm)
            4031630, 1, // Carp (30cm)
            4031631, 1, // Salmon(150cm)
            4031632, 1, // Shovel
            4031633, 2, // Whitebait (3.6cm)
            4031634, 1, // Whitebait (5cm)
            4031635, 1, // Whitebait (6.5cm)
            4031636, 1, // Whitebait (10cm)
            4031637, 2, // Carp (53cm)
            4031638, 2, // Carp (60cm)
            4031639, 1, // Carp (100cm)
            4031640, 1, // Carp (113cm)
            4031641, 2, // Sailfish (128cm)
            4031642, 2, // Sailfish (131cm)
            4031643, 1, // Sailfish (140cm)
            4031644, 1, // Sailfish (148cm)
            4031645, 2, // Salmon (166cm)
            4031646, 2, // Salmon (183cm)
            4031647, 1, // Salmon (227cm)
            4031648, 1, // Salmon (288cm)
            4001187, 20,
            4001188, 20,
            4001189, 20,
            4031629, 1 // Pot
    };

    public static boolean isReverseItem(int itemId) {
        switch (itemId) {
            case 1002790:
            case 1002791:
            case 1002792:
            case 1002793:
            case 1002794:
            case 1082239:
            case 1082240:
            case 1082241:
            case 1082242:
            case 1082243:
            case 1052160:
            case 1052161:
            case 1052162:
            case 1052163:
            case 1052164:
            case 1072361:
            case 1072362:
            case 1072363:
            case 1072364:
            case 1072365:

            case 1302086:
            case 1312038:
            case 1322061:
            case 1332075:
            case 1332076:
            case 1372045:
            case 1382059:
            case 1402047:
            case 1412034:
            case 1422038:
            case 1432049:
            case 1442067:
            case 1452059:
            case 1462051:
            case 1472071:
            case 1482024:
            case 1492025:

            case 1342012:
            case 1942002:
            case 1952002:
            case 1962002:
            case 1972002:
            case 1532016:
            case 1522017:
                return true;
            default:
                return false;
        }
    }

    public static boolean isTimelessItem(int itemId) {
        switch (itemId) {
            case 1032031: //shield earring, but technically
            case 1102172:
            case 1002776:
            case 1002777:
            case 1002778:
            case 1002779:
            case 1002780:
            case 1082234:
            case 1082235:
            case 1082236:
            case 1082237:
            case 1082238:
            case 1052155:
            case 1052156:
            case 1052157:
            case 1052158:
            case 1052159:
            case 1072355:
            case 1072356:
            case 1072357:
            case 1072358:
            case 1072359:
            case 1092057:
            case 1092058:
            case 1092059:

            case 1122011:
            case 1122012:

            case 1302081:
            case 1312037:
            case 1322060:
            case 1332073:
            case 1332074:
            case 1372044:
            case 1382057:
            case 1402046:
            case 1412033:
            case 1422037:
            case 1432047:
            case 1442063:
            case 1452057:
            case 1462050:
            case 1472068:
            case 1482023:
            case 1492023:
            case 1342011:
            case 1532015:
            case 1522016:
                //raven.
                return true;
            default:
                return false;
        }
    }

    public static boolean isRing(int itemId) {
        return itemId >= 1112000 && itemId < 1113000;
    }// 112xxxx - pendants, 113xxxx - belts

    //if only there was a way to find in wz files -.-
    public static boolean isEffectRing(int itemid) {
        return isFriendshipRing(itemid) || isCrushRing(itemid) || isMarriageRing(itemid);
    }

    public static boolean isMarriageRing(int itemId) {
        switch (itemId) {
            case 1112803:
            case 1112806:
            case 1112807:
            case 1112809:
                return true;
        }
        return false;
    }

    public static boolean isFriendshipRing(int itemId) {
        switch (itemId) {
            case 1112800:
            case 1112801:
            case 1112802:
            case 1112810: //new
            case 1112811: //new, doesnt work in friendship?
            case 1112812: //new, im ASSUMING it's friendship cuz of itemID, not sure.
            case 1112816: //new, i'm also assuming
            case 1112817:

                // case 1049000:
                return true;
        }
        return false;
    }

    public static boolean isCrushRing(int itemId) {
        switch (itemId) {
            case 1112001:
            case 1112002:
            case 1112003:
            case 1112005: //new
            case 1112006: //new
            case 1112007:
            case 1112012:
            case 1112015: //new
                return true;
        }
        return false;
    }

    public static int[] Equipments_Bonus = {1122017};

    public static int Equipment_Bonus_EXP(int itemid) { // TODO : Add Time for more exp increase
        switch (itemid) {
            case 1122017:
                return 10;
        }
        return 0;
    }

    public static int getExpForLevel(int i, int itemId) {
        if (isReverseItem(itemId)) {
            return getReverseRequiredEXP(i);
        } else if (getMaxLevel(itemId) > 0) {
            return getTimelessRequiredEXP(i);
        }
        return 0;
    }

    public static int getMaxLevel(int itemId) {
        Map<Integer, Map<String, Integer>> inc = MapleItemInformationProvider.getInstance().getEquipIncrements(itemId);
        return inc != null ? (inc.size()) : 0;
    }

    public static int getStatChance() {
        return 25;
    }

    //錯誤的內容
    public static MonsterStatus getStatFromWeapon(int itemid) {
        switch (itemid) {
            case 1302109:
            case 1312041:
            case 1322067:
            case 1332083:
            case 1372048:
            case 1382064:
            case 1402055:
            case 1412037:
            case 1422041:
            case 1432052:
            case 1442073:
            case 1452064:
            case 1462058:
            case 1472079:
            case 1482035:
                return MonsterStatus.DARKNESS;
            case 1302108:
            case 1312040:
            case 1322066:
            case 1332082:
            case 1372047:
            case 1382063:
            case 1402054:
            case 1412036:
            case 1422040:
            case 1432051:
            case 1442072:
            case 1452063:
            case 1462057:
            case 1472078:
            case 1482036:
                return MonsterStatus.SPEED;
        }
        return null;
    }

    public static int getXForStat(MonsterStatus stat) {
        switch (stat) {
            case DARKNESS:
                return -70;
            case SPEED:
                return -50;
        }
        return 0;
    }

    //錯誤的內容
    public static int getSkillForStat(MonsterStatus stat) {
        switch (stat) {
            case DARKNESS:
                return 1111003; //黑暗之劍
        }
        return 0;
    }

    /*
    神奇服飾箱隨機道具設定
    */
    public final static int[] cashSurpriseRewards = {
            50200004, 3,
            50200069, 3,
            50200117, 3,
            50100008, 3,
            50000047, 3,
            10002819, 3,
            50100010, 1,
            50200001, 3,
            60000073, 3
    };

    public static int[] normalDrops = {
            4001009, //real
            4001010,
            4001011,
            4001012,
            4001013,
            4001014, //real
            4000019,
            4000000,
            4000306,
            4032181,
            4006001,
            4006000,
            2050004,
            3994102,
            3994103,
            3994104,
            3994105,
            2430007}; //end
    public static int[] rareDrops = {
            2022179,
            2049100,
            2049100,
            2430144,
            2028062,
            2028061,
            2290285,
            2049301,
            2049401,
            2022326,
            2022193,
            2049000,
            2049001,
            2049002};
    public static int[] superDrops = {
            2040804,
            2049400,
            2028062,
            2028061,
            2430144,
            2430144,
            2430144,
            2430144,
            2290285,
            2049100,
            2049100,
            2049100,
            2049100};

    /*
    技能點設定相關
    */
    public static int getSkillBook(int job) {
        if (job >= 2210 && job <= 2218) {
            return job - 2209;
        }
        switch (job) {
            case 570:
            case 2310:
            case 2410:
            case 3110:
            case 3210:
            case 3310:
            case 3510:
            case 5110:
                return 1;
            case 571:
            case 2311:
            case 2411:
            case 3111:
            case 3211:
            case 3311:
            case 3511:
            case 5111:
                return 2;
            case 572:
            case 2312:
            case 2412:
            case 3112:
            case 3212:
            case 3312:
            case 3512:
            case 5112:
                return 3;
        }
        return 0;
    }

    /*
    技能點設定相關
    */
    public static int getSkillBook(int job, int level) {
        if (job >= 2210 && job <= 2218) {
            return job - 2209;
        }
        switch (job) {
            case 508:
            case 570:
            case 571:
            case 572:

            case 2300:
            case 2310:
            case 2311:
            case 2312:

            case 2400:
            case 2410:
            case 2411:
            case 2412:

            case 3100:
            case 3110:
            case 3111:
            case 3112:

            case 3200:
            case 3210:
            case 3211:
            case 3212:

            case 3300:
            case 3310:
            case 3311:
            case 3312:

            case 3500:
            case 3510:
            case 3511:
            case 3512:

            case 5100:
            case 5110:
            case 5111:
            case 5112:
                return (level <= 30 ? 0 : (level >= 31 && level <= 70 ? 1 : (level >= 71 && level <= 120 ? 2 : (level >= 120 ? 3 : 0))));
        }
        return 0;
    }

    public static int getSkillBookForSkill(int skillid) {
        return getSkillBook(skillid / 10000);
    }

    /*
    騎獸相關內容
    */
    public static int getMountItem(int sourceid, MapleCharacter chr) {
        switch (sourceid) {
            case 33001001: //美洲豹騎乘
                if (chr == null) {
                    return 1932015;
                }
                switch (chr.getIntNoRecord(JAGUAR)) {
                    case 20:
                        return 1932030;
                    case 30:
                        return 1932031;
                    case 40:
                        return 1932032;
                    case 50:
                        return 1932033;
                    case 60:
                        return 1932036;
                }
                return 1932015;
            case 1203: //鬥神降世
                return 1932103;
            case 1204: //海盜船
                return 1932102;
            case 20021160: //西皮迪亞
                return 1932086;
            case 20021161: //西皮迪亞
                return 1932087;
            case 20031160: //布羅朗斯
                return 1932106;
            case 20031161: //斯基德普拉特尼
                return 1932107;
            case 30011109: //魔族之翼
                return 1932051;
            case 30011159: //特化魔族之翼
                return 1932085;
            case 35001002: //合金盔甲: 原型
            case 35120000: //合金盔甲終極
                return 1932016;
        }
        if (!isBeginnerJob(sourceid / 10000)) {
            if (sourceid / 10000 == 8000 && sourceid != 80001000) { //todoo clean up
                Skill skil = SkillFactory.getSkill(sourceid);
                if (skil != null && skil.getTamingMob() > 0) {
                    return skil.getTamingMob();
                }
            }
        }
        return 0;
    }

    public static boolean isTotem(int itemId) {
        return itemId / 10000 == 122;
    }

    public static boolean isKatara(int itemId) {
        return itemId / 10000 == 134;
    }

    public static boolean isSoulShield(int itemId) {
        return itemId / 10000 == 109;
    }

    public static boolean isDagger(int itemId) {
        return itemId / 10000 == 133;
    }

    public static boolean isApplicableSkill(int skil) {
        return (skil < 60000000 && (skil % 10000 < 8000 || skil % 10000 > 8006) && !isAngel(skil)) || skil >= 92000000 || (skil >= 80000000 && skil < 80010000); //no additional/decent skills
    }

    public static boolean isApplicableSkill_(int skil) { //not applicable to saving but is more of temporary
        for (int i : PlayerStats.pvpSkills) {
            if (skil == i) {
                return true;
            }
        }
        return (skil >= 90000000 && skil < 92000000) || (skil % 10000 >= 8000 && skil % 10000 <= 8003) || isAngel(skil);
    }

    public static boolean isTablet(int itemId) {
        return itemId / 1000 == 2047;
    }

    public static boolean isGeneralScroll(int itemId) {
        return itemId / 1000 == 2046;
    }

    public static int getSuccessTablet(int scrollId, int level) {
        if (scrollId % 1000 / 100 == 2) { //2047_2_00 = armor, 2047_3_00 = accessory
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 55;
                case 2:
                    return 43;
                case 3:
                    return 33;
                case 4:
                    return 26;
                case 5:
                    return 20;
                case 6:
                    return 16;
                case 7:
                    return 12;
                case 8:
                    return 10;
                default:
                    return 7;
            }
        } else if (scrollId % 1000 / 100 == 3) {
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 35;
                case 2:
                    return 18;
                case 3:
                    return 12;
                default:
                    return 7;
            }
        } else {
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 50; //-20
                case 2:
                    return 36; //-14
                case 3:
                    return 26; //-10
                case 4:
                    return 19; //-7
                case 5:
                    return 14; //-5
                case 6:
                    return 10; //-4
                default:
                    return 7;  //-3
            }
        }
    }

    public static int getCurseTablet(int scrollId, int level) {
        if (scrollId % 1000 / 100 == 2) { //2047_2_00 = armor, 2047_3_00 = accessory
            switch (level) {
                case 0:
                    return 10;
                case 1:
                    return 12;
                case 2:
                    return 16;
                case 3:
                    return 20;
                case 4:
                    return 26;
                case 5:
                    return 33;
                case 6:
                    return 43;
                case 7:
                    return 55;
                case 8:
                    return 70;
                default:
                    return 100;
            }
        } else if (scrollId % 1000 / 100 == 3) {
            switch (level) {
                case 0:
                    return 12;
                case 1:
                    return 18;
                case 2:
                    return 35;
                case 3:
                    return 70;
                default:
                    return 100;
            }
        } else {
            switch (level) {
                case 0:
                    return 10;
                case 1:
                    return 14; //+4
                case 2:
                    return 19; //+5
                case 3:
                    return 26; //+7
                case 4:
                    return 36; //+10
                case 5:
                    return 50; //+14
                case 6:
                    return 70; //+20
                default:
                    return 100;  //+30
            }
        }
    }

    public static boolean isAccessory(int itemId) {
        return (itemId >= 1010000 && itemId < 1040000) || (itemId >= 1122000 && itemId < 1153000) || (itemId >= 1112000 && itemId < 1113000);
    }

    /*
    潜能出現機率的設定
    */
    public static boolean potentialIDFits(int potentialID, int newstate, int i) {
        //first line is always the best
        //but, sometimes it is possible to get second/third line as well
        //may seem like big chance, but it's not as it grabs random potential ID anyway
        if (newstate == 20) {
            return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID > 40000 && potentialID < 60000 : potentialID > 30040 && potentialID < 31005 && potentialID < 40000);
        } else if (newstate == 19) {
            return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID > 30000 && potentialID < 31005 && potentialID < 40000 : potentialID > 20040 && potentialID < 20407 && potentialID < 30000);
        } else if (newstate == 18) {
            return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 20000 && potentialID < 20407 && potentialID < 30000 : potentialID >= 10000 && potentialID < 20000);
        } else if (newstate == 17) {
            return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 10000 && potentialID < 20000 : potentialID < 10000);
        } else {
            return false;
        }
    }

    public static boolean optionTypeFits(int optionType, int itemId) {
        switch (optionType) {
            case 10: // weapons
                return isWeapon(itemId);
            case 11: // all equipment except weapons
                return !isWeapon(itemId);
            case 20: // all armors
                return !isAccessory(itemId) && !isWeapon(itemId);
            case 40: // accessories
                return isAccessory(itemId);
            case 51: // hat
                return itemId / 10000 == 100;
            case 52: // top and overall
                return itemId / 10000 == 104 || itemId / 10000 == 105;
            case 53: // bottom and overall
                return itemId / 10000 == 106 || itemId / 10000 == 105;
            case 54: // glove
                return itemId / 10000 == 108;
            case 55: // shoe
                return itemId / 10000 == 107;
            default:
                return true;
        }
    }

    public static int getNebuliteGrade(int id) {
        if (id / 10000 != 306) {
            return -1;
        }
        if (id >= 3060000 && id < 3061000) {
            return 0;
        } else if (id >= 3061000 && id < 3062000) {
            return 1;
        } else if (id >= 3062000 && id < 3063000) {
            return 2;
        } else if (id >= 3063000 && id < 3064000) {
            return 3;
        }
        return 4;
    }

    /*
    騎獸相關設定
    */
    public static boolean isMountItemAvailable(int mountid, int jobid) {
        if (jobid != 900 && mountid / 10000 == 190) {
            switch (mountid) {
                case 1902000:
                case 1902001:
                case 1902002:
                    return isAdventurer(jobid);
                case 1902005:
                case 1902006:
                case 1902007:
                    return isKOC(jobid) || isMihile(jobid);
                case 1902015:
                case 1902016:
                case 1902017:
                case 1902018:
                    return isAran(jobid);
                case 1902040:
                case 1902041:
                case 1902042:
                    return isEvan(jobid);
            }

            if (isResist(jobid)) {
                return false; //none lolol
            }
        }
        if (mountid / 10000 != 190) {
            return false;
        }
        return true;
    }

    public static boolean isMechanicItem(int itemId) {
        return itemId >= 1610000 && itemId < 1660000;
    }

    public static boolean isEvanDragonItem(int itemId) {
        return itemId >= 1940000 && itemId < 1980000; //194 = mask, 195 = pendant, 196 = wings, 197 = tail
    }

    public static boolean canScroll(int itemId) {
        return itemId / 100000 != 19 && itemId / 100000 != 16; //no mech/taming/dragon
    }

    public static boolean canHammer(int itemId) {
        switch (itemId) {
            case 1122000:
            case 1122076: //ht, chaos ht
                return false;
        }
        if (!canScroll(itemId)) {
            return false;
        }
        return true;
    }

    public static int[] owlItems = new int[]{
            1082002, // work gloves
            2070005,
            2070006,
            1022047,
            1102041,
            2044705,
            2340000, // white scroll
            2040017,
            1092030,
            2040804};

    public static int getMasterySkill(int job) {
        if (job >= 1410 && job <= 1412) {
            return 14100000; //精準暗器
        } else if (job >= 410 && job <= 412) {
            return 4100000; //精準暗器
        } else if (job >= 520 && job <= 522) {
            return 5200000; //精通槍法
        }
        return 0;
    }

    public static String getCashBlockedMsg(int id) {
        switch (id) {
            //case 5220083:
            case 5220084:
            case 5220092:
                //cube
                return "This item is blocked from the Cash Shop.";
        }
        return "This item is blocked from the Cash Shop.";
    }

    /*
    設定反應堆觸發道具
    */
    public static int getCustomReactItem(int rid, int original, int map) {
        if (rid == 2008006) { //orbis pq LOL
            return (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 4001055);
            //4001056 = sunday. 4001062 = saturday
        } else if (rid == 2408002) {
            return map == 240050101 ? 4001088 : map == 240050102 ? 4001089 : map == 240050103 ? 4001090 : 4001091;
        } else {
            return original;
        }
    }

    public static int getJobNumber(int jobz) {
        int job = (jobz % 1000);
        if (job / 100 == 0 || isBeginnerJob(jobz)) {
            return 0; //beginner
        } else if ((job / 10) % 10 == 0 || job == 501) {
            return 1;
        } else {
            return 2 + (job % 10);
        }
    }

    /*
    新手職業設定
    */
    public static boolean isBeginnerJob(int job) {
        return job == 0 || job == 1 || job == 1000 || job == 2000 || job == 2001 || job == 2002 || job == 2003 || job == 3000 || job == 3001 || job == 5000;
    }

    public static boolean isForceRespawn(int mapid) {
        switch (mapid) {
            case 103000800: //kerning PQ crocs
            case 925100100: //crocs and stuff
                return true;
            default:
                return mapid / 100000 == 9800 && (mapid % 10 == 1 || mapid % 1000 == 100);
        }
    }

    public static int getFishingTime(boolean vip, boolean gm) {
        return gm ? 1000 : (vip ? 30000 : 60000);
    }

    public static int getCustomSpawnID(int summoner, int def) {
        switch (summoner) {
            case 9400589:
            case 9400748: //MV
                return 9400706; //jr
            default:
                return def;
        }
    }

    public static boolean canForfeit(int questid) {
        switch (questid) {
            case 20000:
            case 20010:
            case 20015: //cygnus quests
            case 20020:
                return false;
            default:
                return true;
        }
    }

    public static int getLowestPrice(int itemId) {
        switch (itemId) {
            case 2340000: //ws
            case 2531000:
            case 2530000:
                return 50000000;
        }
        return -1;
    }

    public static boolean isNoDelaySkill(int skillId) { //限制部分技能，否者會導致錯誤發生

        // 變形 颶風 地雷 合金盔甲: 導彈罐 戰鬥機器 : 巨人錘
        return skillId == 31121005 || skillId == 33101004 || skillId == 32121003 || skillId == 35121005 || skillId == 35111004 || skillId == 35121013 || skillId == 35121003;
    }

    public static boolean isNoSpawn(int mapID) {
        return mapID == 809040100 || mapID == 925020010 || mapID == 925020011 || mapID == 925020012 || mapID == 925020013 || mapID == 925020014 || mapID == 980010000 || mapID == 980010100 || mapID == 980010200 || mapID == 980010300 || mapID == 980010020;
    }

    public static int getExpRate(int job, int def) {
        return def;
    }

    public static int getModifier(int itemId, int up) {
        if (up <= 0) {
            return 0;
        }
        switch (itemId) {
            case 2022459:
            case 2860179:
            case 2860193:
            case 2860207:
                return 130;
            case 2022460:
            case 2022462:
            case 2022730:
                return 150;
            case 2860181:
            case 2860195:
            case 2860209:
                return 200;
        }
        if (itemId / 10000 == 286) { //familiars
            return 150;
        }
        return 200;
    }

    public static short getSlotMax(int itemId) {
        switch (itemId) {
            case 4030003:
            case 4030004:
            case 4030005:
                return 1;
            case 4001168:
            case 4031306:
            case 4031307:
            case 3993000:
            case 3993002:
            case 3993003:
                return 100;
            case 5220010:
            case 5220013:
                return 1000;
            case 5220020:
                return 2000;
        }
        return 0;
    }

    public static short getStat(int itemId, int def) {
        switch (itemId) {
            case 1002419:
                return 5;
            case 1002959:
                return 25;
            case 1142002:
                return 10;
            case 1122121:
                return 7;
        }
        return (short) def;
    }

    public static short getHpMp(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 500;
            case 1142002:
            case 1002959:
                return 1000;
        }
        return (short) def;
    }

    public static short getATK(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 3;
            case 1002959:
                return 4;
            case 1142002:
                return 9;
        }
        return (short) def;
    }

    public static short getDEF(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 250;
            case 1002959:
                return 500;
        }
        return (short) def;
    }

    public static boolean isDojo(int mapId) {
        return mapId >= 925020100 && mapId <= 925023814;
    }

    public static int getPartyPlay(int mapId, int def) {
        return def / 2;
    }

    public static boolean isHyperTeleMap(int mapId) {
        for (int i : hyperTele) {
            if (i == mapId) {
                return true;
            }
        }
        return false;
    }

    public static int getCurrentDate() {
        String time = FileoutputUtil.CurrentReadable_Time();
        return Integer.parseInt(new StringBuilder(time.substring(0, 4)).append(time.substring(5, 7)).append(time.substring(8, 10)).append(time.substring(11, 13)).toString());
    }

    public static int getCurrentDate_NoTime() {
        String time = FileoutputUtil.CurrentReadable_Time();
        return Integer.parseInt(new StringBuilder(time.substring(0, 4)).append(time.substring(5, 7)).append(time.substring(8, 10)).toString());
    }

    public static void achievementRatio(MapleClient c) {
        //PQs not affected: Amoria, MV, CWK, English, Zakum, Horntail(?), Carnival, Ghost, Guild, LudiMaze, Elnath(?) 
        switch (c.getPlayer().getMapId()) {
            case 240080600:
            case 920010000:
            case 930000000:
            case 930000100:
            case 910010000:
            case 922010100:
            case 910340100:
            case 925100000:
            case 926100000:
            case 926110000:
            case 921120005:
            case 932000100:
            case 923040100:
            case 921160100:
                c.getSession().write(CField.achievementRatio(0));
                break;
            case 930000200:
            case 922010200:
            case 922010300:
            case 922010400:
            case 922010401:
            case 922010402:
            case 922010403:
            case 922010404:
            case 922010405:
            case 925100100:
            case 926100001:
            case 926110001:
            case 921160200:
                c.getSession().write(CField.achievementRatio(10));
                break;
            case 930000300:
            case 910340200:
            case 922010500:
            case 922010600:
            case 925100200:
            case 925100201:
            case 925100202:
            case 926100100:
            case 926110100:
            case 921120100:
            case 932000200:
            case 923040200:
            case 921160300:
            case 921160310:
            case 921160320:
            case 921160330:
            case 921160340:
            case 921160350:
                c.getSession().write(CField.achievementRatio(25));
                break;
            case 930000400:
            case 926100200:
            case 926110200:
            case 926100201:
            case 926110201:
            case 926100202:
            case 926110202:
            case 921160400:
                c.getSession().write(CField.achievementRatio(35));
                break;
            case 910340300:
            case 922010700:
            case 930000500:
            case 925100300:
            case 925100301:
            case 925100302:
            case 926100203:
            case 926110203:
            case 921120200:
            case 932000300:
            case 240080700:
            case 240080800:
            case 923040300:
            case 921160500:
                c.getSession().write(CField.achievementRatio(50));
                break;
            case 910340400:
            case 922010800:
            case 930000600:
            case 925100400:
            case 926100300:
            case 926110300:
            case 926100301:
            case 926110301:
            case 926100302:
            case 926110302:
            case 926100303:
            case 926110303:
            case 926100304:
            case 926110304:
            case 921120300:
            case 932000400:
            case 923040400:
            case 921160600:
                c.getSession().write(CField.achievementRatio(70));
                break;
            case 910340500:
            case 922010900:
            case 930000700:
            case 920010800:
            case 925100500:
            case 926100400:
            case 926110400:
            case 926100401:
            case 926110401:
            case 921120400:
            case 921160700:
                c.getSession().write(CField.achievementRatio(85));
                break;
            case 922011000:
            case 922011100:
            case 930000800:
            case 920011000:
            case 920011100:
            case 920011200:
            case 920011300:
            case 925100600:
            case 926100500:
            case 926110500:
            case 926100600:
            case 926110600:
            case 921120500:
            case 921120600:
                c.getSession().write(CField.achievementRatio(100));
                break;
        }
    }

    public static boolean isAngel(int sourceid) {
        return isBeginnerJob(sourceid / 10000) && (sourceid % 10000 == 1085 || sourceid % 10000 == 1087 || sourceid % 10000 == 1090 || sourceid % 10000 == 1179);
    }

    public static int getAngelicSkill(int equipId) {
        switch (equipId) {
            case 1112585:
                return 1085;
            case 1112586:
                return 1087;
            case 1112594:
                return 1090;
            case 1112663:
                return 1179;
        }
        return 0;
    }

    public static int getAngelicBuff(int skill) {
        switch (skill % 10000) {
            case 1085:
                return 2022746;
            case 1087:
                return 2022747;
            case 1090:
                return 2022764;//no idea lol
            case 1179:
                return 2022823;
        }
        return 0;
    }


    public static boolean isFishingMap(int mapid) {
        return mapid == 749050500 || mapid == 749050501 || mapid == 749050502 || mapid == 970020000 || mapid == 970020005;
    }

    public static int getRewardPot(int itemid, int closeness) {
        switch (itemid) {
            case 2440000:
                switch (closeness / 10) {
                    case 0:
                    case 1:
                    case 2:
                        return 2028041 + (closeness / 10);
                    case 3:
                    case 4:
                    case 5:
                        return 2028046 + (closeness / 10);
                    case 6:
                    case 7:
                    case 8:
                        return 2028049 + (closeness / 10);
                }
                return 2028057;
            case 2440001:
                switch (closeness / 10) {
                    case 0:
                    case 1:
                    case 2:
                        return 2028044 + (closeness / 10);
                    case 3:
                    case 4:
                    case 5:
                        return 2028049 + (closeness / 10);
                    case 6:
                    case 7:
                    case 8:
                        return 2028052 + (closeness / 10);
                }
                return 2028060;
            case 2440002:
                return 2028069;
            case 2440003:
                return 2430278;
            case 2440004:
                return 2430381;
            case 2440005:
                return 2430393;
        }
        return 0;
    }

    public static boolean isEventMap(int mapid) {
        return (mapid >= 109010000 && mapid < 109050000) || (mapid > 109050001 && mapid < 109090000) || (mapid >= 809040000 && mapid <= 809040100);
    }

    public static boolean isMagicChargeSkill(int skillid) {
        switch (skillid) {
            case 2121001: //核爆術
            case 2221001: //核爆術
            case 2321001: //核爆術
                return true;
        }
        return false;
    }

    public static boolean isTeamMap(int mapid) {
        return mapid == 960010104 || mapid == 109080000 || mapid == 109080001 || mapid == 109080002 || mapid == 109080003 || mapid == 109080010 || mapid == 109080011 || mapid == 109080012 || mapid == 109090300 || mapid == 109090301 || mapid == 109090302 || mapid == 109090303 || mapid == 109090304 || mapid == 910040100 || mapid == 960020100 || mapid == 960020101 || mapid == 960020102 || mapid == 960020103 || mapid == 960030100 || mapid == 689000000 || mapid == 689000010;
    }

    public static int getStatDice(int stat) {
        switch (stat) {
            case 2:
                return 30;
            case 3:
                return 20;
            case 4:
                return 15;
            case 5:
                return 20;
            case 6:
                return 30;
        }
        return 0;
    }

    public static int getDiceStat(int buffid, int stat) {
        if (buffid == stat || buffid % 10 == stat || buffid / 10 == stat) {
            return getStatDice(stat);
        } else if (buffid == (stat * 100)) {
            return getStatDice(stat) + 10;
        }
        return 0;
    }

    //questID; FAMILY USES 19000x, MARRIAGE USES 16000x, EXPED USES 16010x
    //dojo = 150000, bpq = 150001, master monster portals: 122600
    //compensate evan = 170000, compensate sp = 170001
    public static int OMOK_SCORE = 122200;
    public static int MATCH_SCORE = 122210;
    public static int HP_ITEM = 122221;
    public static int MP_ITEM = 122223;
    public static int CURE_ITEM = 122224;
    public static int JAIL_TIME = 123455;
    public static int JAIL_QUEST = 123456;
    public static int REPORT_QUEST = 123457;
    public static int ULT_EXPLORER = 111111;
    //codex = -55 slot
    //crafting/gathering are designated as skills(short exp then byte 0 then byte level), same with recipes(integer.max_value skill level)
    public static int ENERGY_DRINK = 122500;
    public static int HARVEST_TIME = 122501;
    public static int PENDANT_SLOT = 122700;
    public static int CURRENT_SET = 122800;
    public static int BOSS_PQ = 150001;
    public static int JAGUAR = 111112;

    public static int PARTY_REQUEST = 122900;
    public static int PARTY_INVITE = 122901;
    public static int QUICK_SLOT = 123000;
    public static int ITEM_TITLE = 124000;

    public static int[] getInnerSkillbyRank(int rank) {
        if (rank == 0) {
            return rankC;
        } else if (rank == 1) {
            return rankB;
        } else if (rank == 2) {
            return rankA;
        } else if (rank == 3) {
            return rankS;
        } else {
            return null;
        }
    }
}


