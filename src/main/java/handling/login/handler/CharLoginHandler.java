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
package handling.login.handler;

import client.*;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.login.LoginInformationProvider;
import handling.login.LoginInformationProvider.JobType;
import handling.login.LoginServer;
import handling.login.LoginWorker;
import handling.world.World;

import java.util.*;
import java.util.Map.Entry;

import server.MapleItemInformationProvider;
import server.quest.MapleQuest;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.LoginPacket;
import tools.packet.PacketHelper;

public class CharLoginHandler {

    private static boolean loginFailCount(final MapleClient c) {
        c.loginAttempt++;
        if (c.loginAttempt > 5) {
            return true;
        }
        return false;
    }

    /*
    性別設定相關
    */
    public static final void SetGenderRequest(final LittleEndianAccessor slea, final MapleClient c) {
        byte type = slea.readByte(); //?
        if (type == 0x01 && c.getGender() == 10) { //Packet shouldn't come if Gender isn't 10.
            c.setGender(slea.readByte());

            c.getSession().write(LoginPacket.getAuthSuccessRequest(c));
            c.getSession().write(LoginPacket.getGenderChanged(c)); //選擇性別回饋
            //c.getSession().write(LoginPacket.getLoginFailed(23)); //楓之谷協議
        }
        if (c.getGender() == 10) {
            c.updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN, c.getSessionIPAddress()); //防止卡帳號
        }
    }

    public static final void updateCCards(LittleEndianAccessor slea, MapleClient c) {
        if ((slea.available() != 24) || (!c.isLoggedIn())) {
            c.getSession().close();
            return;
        }
        Map<Integer, Integer> cids = new LinkedHashMap();
        for (int i = 1; i <= 6; i++) {
            int charId = slea.readInt();
            if (((!c.login_Auth(charId)) && (charId != 0)) || (ChannelServer.getInstance(c.getChannel()) == null) || (c.getWorld() != 0)) {
                c.getSession().close();
                return;
            }
            cids.put(Integer.valueOf(i), Integer.valueOf(charId));
        }
        c.updateCharacterCards(cids);
    }

    /*
    登入遊戲相關
    */
    public static final void login(final LittleEndianAccessor slea, final MapleClient c) {
        String login = slea.readMapleAsciiString();
        String pwd = slea.readMapleAsciiString();
//        int checkId = AutoRegister.checkAccount(c, login, pwd);
//        if (checkId == 0) { //생성 가능한 아이디일때
//            AutoRegister.registerAccount(c, login, pwd);
//            c.getSession().write(CWvsContext.serverNotice(1, "The account has been successfully created."));
//            c.getSession().write(LoginPacket.getLoginFailed(20));
//            return;
//        } else if (checkId == 1) { //계정 찾기 실패
//            c.getSession().write(CWvsContext.serverNotice(1, "Account registration failed, automatic registration function not enabled. Please go to the website to register an account."));
//            c.getSession().write(LoginPacket.getLoginFailed(20));
//            return;
//        } else if (checkId == 2) { //php오류
//            c.getSession().write(CWvsContext.serverNotice(1, "An unknown error has occurred."));
//            c.getSession().write(LoginPacket.getLoginFailed(20));
//            return;
//        } else if (checkId == 3) { //레벨
//            c.getSession().write(CWvsContext.serverNotice(1, "The site level is incorrect. Please use it after obtaining an account rating."));
//            c.getSession().write(LoginPacket.getLoginFailed(20));
//            return;
//        } else if (checkId == 6) { //ㅇㅇ
//            c.getSession().write(CWvsContext.serverNotice(1, "Each IP can only create two new accounts."));
//            c.getSession().write(LoginPacket.getLoginFailed(20));
//            return;
//        }
        final boolean ipBan = c.hasBannedIP();
        final boolean macBan = c.hasBannedMac();
        int loginok = c.login(login, pwd, ipBan || macBan);
        final Calendar tempbannedTill = c.getTempBanCalendar();
        if (loginok == 0 && (ipBan || macBan) && !c.isGm()) {
            loginok = 3;
            if (macBan) {
                MapleCharacter.ban(c.getSession().getRemoteAddress().toString().split(":")[0], "Enforcing account ban, account " + login, false);
            }
        }
        if (loginok != 0) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().write(LoginPacket.getLoginFailed(loginok));
            } else {
                c.getSession().close();
            }
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().write(LoginPacket.getTempBan(PacketHelper.getTime(tempbannedTill.getTimeInMillis()), c.getBanReason()));
            } else {
                c.getSession().close();
            }
        } else {
            c.loginAttempt = 0;
            LoginWorker.registerClient(c);
        }
    }

    /*
    登入遊戲相關
    */
    public static void ServerListRequest(final MapleClient c) {
        c.getSession().write(LoginPacket.getLoginWelcome());
        c.getSession().write(LoginPacket.getServerList(0, LoginServer.getLoad()));
        //c.getSession().write(CField.getServerList(1, "Scania", LoginServer.getInstance().getChannels(), 1200));
        //c.getSession().write(CField.getServerList(2, "Scania", LoginServer.getInstance().getChannels(), 1200));
        //c.getSession().write(CField.getServerList(3, "Scania", LoginServer.getInstance().getChannels(), 1200));
        c.getSession().write(LoginPacket.getEndOfServerList());
        c.getSession().write(LoginPacket.enableRecommended());
        c.getSession().write(LoginPacket.sendRecommended(0, LoginServer.getEventMessage()));
    }

    public static void ServerStatusRequest(final MapleClient c) {
        // 0 = Select world normally
        // 1 = "Since there are many users, you may encounter some..."
        // 2 = "The concurrent users in this world have reached the max"
        final int numPlayer = LoginServer.getUsersOn();
        final int userLimit = LoginServer.getUserLimit();
        if (numPlayer >= userLimit) {
            c.getSession().write(LoginPacket.getServerStatus(2));
        } else if (numPlayer * 2 >= userLimit) {
            c.getSession().write(LoginPacket.getServerStatus(1));
        } else {
            c.getSession().write(LoginPacket.getServerStatus(0));
        }
    }

    public static void CharlistRequest(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isLoggedIn()) {
            c.getSession().close();
            return;
        }
        if (GameConstants.GMS) {
            slea.readByte(); //2?
        }
        final int server = slea.readByte();
        final int channel = slea.readByte() + 1;
        if (!World.isChannelAvailable(channel) || server != 0) { //TODOO: MULTI WORLDS
            c.getSession().write(LoginPacket.getLoginFailed(10)); //cannot process so many
            return;
        }

        //System.out.println("Client " + c.getSession().getRemoteAddress().toString().split(":")[0] + " is connecting to server " + server + " channel " + channel + "");

        final List<MapleCharacter> chars = c.loadCharacters(server);
        if (chars != null && ChannelServer.getInstance(channel) != null) {
            c.setWorld(server);
            c.setChannel(channel);
            c.getSession().write(LoginPacket.getSecondAuthSuccess(c));
            c.getSession().write(LoginPacket.getCharList(c.getSecondPassword(), chars, 15));
        } else {
            c.getSession().close();
        }
    }

    public static void CheckCharName(final String name, final MapleClient c) {
        c.getSession().write(LoginPacket.charNameResponse(name, !(MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm()))));
    }

    /*
    新建角色設定
    */
    public static void CreateChar(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isLoggedIn()) {
            c.getSession().close();
            return;
        }
        final String name = slea.readMapleAsciiString();
        final JobType jobType = JobType.getByType(slea.readInt());
        final short db = slea.readShort();
        final byte gender = slea.readByte();
        byte skinColor = slea.readByte();
        int hairColor = 0;
        int weapon3 = 0;

        final byte unk2 = slea.readByte(); // 08
        final boolean mercedes = (jobType == JobType.Mercedes);
        final boolean demon = (jobType == JobType.Demon);
        final boolean jett = (jobType == JobType.Jett);
        final boolean phantom = (jobType == JobType.Phantom);

        final int face = slea.readInt();
        final int hair = slea.readInt();
        if (!mercedes && !demon && !phantom && !jett) {
            hairColor = slea.readInt();
            skinColor = (byte) slea.readInt();
        }
        final int demonMark = demon ? slea.readInt() : 0;
        final int top = slea.readInt();
        final int bottom = (mercedes || demon || jett) ? 0 : slea.readInt();

        final int shoes = slea.readInt();
        final int weapon = slea.readInt();

        if (jett || phantom) {
            weapon3 = slea.readInt();
        }
        int shield = phantom ? 1352100 : mercedes ? 1352000 : demon ? slea.readInt() : 0; //副手武器

        MapleCharacter newchar = MapleCharacter.getDefault(c, jobType);
        newchar.setWorld((byte) c.getWorld());
        newchar.setFace(face);
        newchar.setHair(jobType == JobType.Mihile ? 36033 : hair + hairColor); //米哈逸髮色
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor(skinColor);
        newchar.setDemonMarking(demonMark);

        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);
        Item item;

        item = li.getEquipById(top);
        item.setPosition((byte) -5);
        equip.addFromDB(item);

        if (bottom > 0) { //resistance have overall
            item = li.getEquipById(bottom);
            item.setPosition((byte) (phantom ? -5 : -6));
            equip.addFromDB(item);
        }

        item = li.getEquipById(shoes);
        item.setPosition((byte) (jett || phantom ? -9 : -7));
        equip.addFromDB(item);

        item = li.getEquipById(weapon);
        item.setPosition((byte) (jett || phantom ? -7 : -11));
        equip.addFromDB(item);

        if (weapon3 > 0) {
            item = li.getEquipById(weapon3);
            item.setPosition((byte) (-11));
            equip.addFromDB(item);
        }

        if (shield > 0) {
            item = li.getEquipById(shield);
            item.setPosition((byte) -10);
            equip.addFromDB(item);
        }

        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000013, (byte) 0, (short) 100, (byte) 0));
        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000014, (byte) 0, (short) 100, (byte) 0));

        //blue/red pots
        switch (jobType) {
            case Resistance: // Resistance
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (byte) 0, (short) 1, (byte) 0));
                break;
            case Adventurer: // Adventurer
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (byte) 0, (short) 1, (byte) 0));
                break;
            case Cygnus: // Cygnus
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161047, (byte) 0, (short) 1, (byte) 0));
                break;
            case Aran: // Aran
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161048, (byte) 0, (short) 1, (byte) 0));
                break;
            case Evan: //Evan
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161052, (byte) 0, (short) 1, (byte) 0));
                break;
            case Mercedes: // Mercedes
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161079, (byte) 0, (short) 1, (byte) 0));
                break;
            case Demon: //Demon
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161054, (byte) 0, (short) 1, (byte) 0));
                break;
        }

        if (MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm()) && (c.isGm() || c.canMakeCharacter(c.getWorld()))) {
            MapleCharacter.saveNewCharToDB(newchar, jobType, db);
            c.getSession().write(LoginPacket.addNewCharEntry(newchar, true));
            c.createdChar(newchar.getId());
        } else {
            c.getSession().write(LoginPacket.addNewCharEntry(newchar, false));
        }
    }

    public static void CreateUltimate(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isLoggedIn() || c.getPlayer() == null || c.getPlayer().getLevel() < 120 || c.getPlayer().getMapId() != 130000000 || c.getPlayer().getQuestStatus(20734) != 0 || c.getPlayer().getQuestStatus(20616) != 2 || !GameConstants.isKOC(c.getPlayer().getJob()) || !c.canMakeCharacter(c.getPlayer().getWorld())) {
            c.getPlayer().dropMessage(1, "You have no character slots.");
            c.getSession().write(CField.createUltimate(0));
            return;
        }
        System.out.println(slea.toString());
        final String name = slea.readMapleAsciiString();
        final int job = slea.readInt(); //job ID
        if (job < 110 || job > 520 || job % 10 > 0 || (job % 100 != 10 && job % 100 != 20 && job % 100 != 30) || job == 430) {
            c.getPlayer().dropMessage(1, "An error has occurred.");
            c.getSession().write(CField.createUltimate(0));
            return;
        }
        final int face = slea.readInt();
        final int hair = slea.readInt();

        final int hat = slea.readInt();
        final int top = slea.readInt();
        final int glove = slea.readInt();
        final int shoes = slea.readInt();
        final int weapon = slea.readInt();

        final byte gender = c.getPlayer().getGender();
        JobType jobType;
        //if (!LoginInformationProvider.getInstance().isEligibleItem(gender, 0, jobType.type, face) || !LoginInformationProvider.getInstance().isEligibleItem(gender, 1, jobType.type, hair)) {
        //    c.getPlayer().dropMessage(1, "An error occurred.");
        //    c.getSession().write(CField.createUltimate(0));
        //    return;
        //}

        jobType = JobType.UltimateAdventurer;
        if (!LoginInformationProvider.getInstance().isEligibleItem(-1, job, jobType.type, hat) || !LoginInformationProvider.getInstance().isEligibleItem(-1, job, jobType.type, top)
                || !LoginInformationProvider.getInstance().isEligibleItem(-1, job, jobType.type, glove) || !LoginInformationProvider.getInstance().isEligibleItem(-1, job, jobType.type, shoes)
                || !LoginInformationProvider.getInstance().isEligibleItem(-1, job, jobType.type, weapon)) {
            c.getPlayer().dropMessage(1, "An error occured.");
            c.getSession().write(CField.createUltimate(0));
            return;
        }

        MapleCharacter newchar = MapleCharacter.getDefault(c, jobType);
        newchar.setJob(job);
        newchar.setWorld((byte) c.getPlayer().getWorld());
        newchar.setFace(face);
        newchar.setHair(hair);
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor((byte) 3); //troll
        newchar.setLevel((short) 51);
        newchar.getStat().str = (short) 4;
        newchar.getStat().dex = (short) 4;
        newchar.getStat().int_ = (short) 4;
        newchar.getStat().luk = (short) 4;
        newchar.setRemainingAp(254); //49*5 + 25 - 16
        newchar.setRemainingSp(job / 100 == 2 ? 128 : 122); //2 from job advancements. 120 from leveling. (mages get +6)
        newchar.getStat().maxhp += 150; //Beginner 10 levels
        newchar.getStat().maxmp += 125;
        switch (job) {
            case 110:
            case 120:
            case 130:
                newchar.getStat().maxhp += 600; //Job Advancement
                newchar.getStat().maxhp += 2000; //Levelup 40 times
                newchar.getStat().maxmp += 200;
                break;
            case 210:
            case 220:
            case 230:
                newchar.getStat().maxmp += 600;
                newchar.getStat().maxhp += 500; //Levelup 40 times
                newchar.getStat().maxmp += 2000;
                break;
            case 310:
            case 320:
            case 410:
            case 420:
            case 520:
                newchar.getStat().maxhp += 500;
                newchar.getStat().maxmp += 250;
                newchar.getStat().maxhp += 900; //Levelup 40 times
                newchar.getStat().maxmp += 600;
                break;
            case 510:
                newchar.getStat().maxhp += 500;
                newchar.getStat().maxmp += 250;
                newchar.getStat().maxhp += 450; //Levelup 20 times
                newchar.getStat().maxmp += 300;
                newchar.getStat().maxhp += 800; //Levelup 20 times
                newchar.getStat().maxmp += 400;
                break;
            default:
                return;
        }

        newchar.setQuestAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER), (byte) 0, c.getPlayer().getName());

        final Map<Skill, SkillEntry> ss = new HashMap<>();
        ss.put(SkillFactory.getSkill(1074 + (job / 100)), new SkillEntry((byte) 5, (byte) 5, -1));
        ss.put(SkillFactory.getSkill(80), new SkillEntry((byte) 1, (byte) 1, -1));
        newchar.changeSkillLevel_Skip(ss, false);
        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();

        int[] items = new int[]{1142257, hat, top, shoes, glove, weapon, hat + 1, top + 1, shoes + 1, glove + 1, weapon + 1}; //brilliant = fine+1
        for (byte i = 0; i < items.length; i++) {
            Item item = li.getEquipById(items[i]);
            item.setPosition((byte) (i + 1));
            newchar.getInventory(MapleInventoryType.EQUIP).addFromDB(item);
        }
        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000004, (byte) 0, (short) 100, (byte) 0));
        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000004, (byte) 0, (short) 100, (byte) 0));
        c.getPlayer().fakeRelog();
        if (MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm())) {
            MapleCharacter.saveNewCharToDB(newchar, jobType, (short) 0);
            MapleQuest.getInstance(20734).forceComplete(c.getPlayer(), 1101000);
            c.getSession().write(CField.createUltimate(1));
        } else {
            c.getSession().write(CField.createUltimate(0));
        }
    }

    /*
    删除角色相關
    */
    public static void DeleteChar(final LittleEndianAccessor slea, final MapleClient c) {
        String Secondpw_Client = GameConstants.GMS ? slea.readMapleAsciiString() : null;
        if (Secondpw_Client == null) {
            if (slea.readByte() > 0) { // Specific if user have second password or not
                Secondpw_Client = slea.readMapleAsciiString();
            }
            slea.readMapleAsciiString();
        }

        final int Character_ID = slea.readInt();

        if (!c.login_Auth(Character_ID) || !c.isLoggedIn() || loginFailCount(c)) {
            c.getSession().close();
            return; // Attempting to delete other character
        }
        byte state = 0;

        if (c.getSecondPassword() != null) { // On the server, there's a second password
            if (Secondpw_Client == null) { // Client's hacking
                c.getSession().close();
                return;
            } else {
                if (!c.CheckSecondPassword(Secondpw_Client)) { // Wrong Password
                    state = 20;
                }
            }
        }

        if (state == 0) {
            state = (byte) c.deleteCharacter(Character_ID);
        }
        c.getSession().write(LoginPacket.deleteCharResponse(Character_ID, state));
    }

    public static void Character_WithoutSecondPassword(final LittleEndianAccessor slea, final MapleClient c, final boolean haspic, final boolean view) {
        slea.readByte(); // 1?
        slea.readByte(); // 1?
        final int charId = slea.readInt();
        if (view) {
            c.setChannel(1);
            c.setWorld(slea.readInt());
        }
        final String currentpw = c.getSecondPassword();
        if (!c.isLoggedIn() || loginFailCount(c) || (currentpw != null && (!currentpw.equals("") || haspic)) || !c.login_Auth(charId) || ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0) { // TODOO: MULTI WORLDS
            c.getSession().close();
            return;
        }
        c.updateMacs(slea.readMapleAsciiString());
        if (c.hasBannedIP() || c.hasBannedMac() || c.hasProxyBan()) {
            c.getSession().close();
            return;
        }
        slea.readMapleAsciiString();
        if (slea.available() != 0) {
            final String setpassword = slea.readMapleAsciiString();

            if (setpassword.length() >= 6 && setpassword.length() <= 16) {
                c.setSecondPassword(setpassword);
                c.updateSecondPassword();
            } else {
                c.getSession().write(LoginPacket.secondPwError((byte) 0x14));
                return;
            }
        } else if (haspic) {
            return;
        }

        if (c.getIdleTask() != null) {
            c.getIdleTask().cancel(true);
        }
        final String s = c.getSessionIPAddress();
        LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP());
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
        c.getSession().write(CField.getServerIP(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
    }

    public static void Character_WithSecondPassword(final LittleEndianAccessor slea, final MapleClient c, final boolean view) {
        final String password = slea.readMapleAsciiString();
        final int charId = slea.readInt();
        if (view) {
            c.setChannel(1);
            c.setWorld(slea.readInt());
        }

        if (!c.isLoggedIn() || loginFailCount(c) || c.getSecondPassword() == null || !c.login_Auth(charId) || ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0) { // TODOO: MULTI WORLDS
            c.getSession().close();
            return;
        }

        c.updateMacs(slea.readMapleAsciiString());
        if (c.hasBannedIP() || c.hasBannedMac() || c.hasProxyBan()) {
            c.getSession().close();
            return;
        }
        if (c.CheckSecondPassword(password) && password.length() >= 6 && password.length() <= 16) {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            final String s = c.getSessionIPAddress();
            LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP());
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
            c.getSession().write(CField.getServerIP(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
        } else {
            c.getSession().write(LoginPacket.secondPwError((byte) 0x14));
        }
    }

    /*     */
    public static void PartTimeJob(LittleEndianAccessor slea, MapleClient c) {
        /* 234 */
        boolean complete = slea.readByte() == 2;
        /* 235 */
        int charId = slea.readInt();
        /* 236 */
        int type = slea.readByte();

        /* 241 */
        Pair info = c.getPartTimeJob(charId);
        /* 242 */
        if (complete) {
            /* 243 */
            if ((((Byte) info.getLeft()).byteValue() <= 0) || (((Long) info.getRight()).longValue() <= -2)) {
                System.out.println("7");
                /* 244 */
                c.getSession().write(LoginPacket.partTimeJobRequest(charId, 3, 0, 0, false, false));
                /* 245 */
                return;
                /*     */
            }
            /* 247 */
            int hoursFromLogin = Math.min((int) ((System.currentTimeMillis() - ((Long) info.getRight()).longValue()) / 3600000L), 6);
            /* 248 */
            boolean insert = c.updatePartTimeJob(charId, (byte) (hoursFromLogin > 0 ? -((Byte) info.getLeft()).byteValue() : 0), hoursFromLogin > 0 ? -hoursFromLogin - 10 : -2);
            /* 249 */
            if (insert) {
                System.out.println("6");
                c.getSession().write(LoginPacket.partTimeJobRequest(charId, 0, 0, ((Long) info.getRight()).longValue(), hoursFromLogin != 0, hoursFromLogin == 6));
            } /*     */ else {
                System.out.println("5");
                c.getSession().write(LoginPacket.partTimeJobRequest(charId, 2, 0, 0, false, false));
            }
            /*     */
        } /*     */ else {
            /* 255 */
            if ((((Byte) info.getLeft()).byteValue() > 0) || (((Long) info.getRight()).longValue() > 0L) || (!c.canMakePartTimeJob())) {
                System.out.println("1");
                /* 256 */
                c.getSession().write(LoginPacket.partTimeJobRequest(charId, 3, 0, 0, false, false));
                /* 257 */
                return;
                /*     */
            }
            /* 259 */
            if (((Byte) info.getLeft()).byteValue() < 0) {
                System.out.println("2");
                /* 260 */
                c.getSession().write(LoginPacket.partTimeJobRequest(charId, 1, 0, 0, false, false));
                /* 261 */
                return;
                /*     */
            }
            /* 263 */
            long start = System.currentTimeMillis();
            /* 264 */
            boolean insert = c.updatePartTimeJob(charId, (byte) type, start);
            /* 265 */
            if (insert) {
                System.out.println("3");
                c.getSession().write(LoginPacket.partTimeJobRequest(charId, 0, type, start, false, false));
            } /*     */ else {
                System.out.println("4");
                c.getSession().write(LoginPacket.partTimeJobRequest(charId, 2, 0, 0, false, false));
            }
            /*     */
        }
        /*     */
    }

    public static void ViewChar(LittleEndianAccessor slea, MapleClient c) {
        Map<Byte, ArrayList<MapleCharacter>> worlds = new HashMap<>();
        List<MapleCharacter> chars = c.loadCharacters(0); //TODO multi world
        c.getSession().write(LoginPacket.showAllCharacter(chars.size()));
        for (MapleCharacter chr : chars) {
            if (chr != null) {
                ArrayList<MapleCharacter> chrr;
                if (!worlds.containsKey(chr.getWorld())) {
                    chrr = new ArrayList<>();
                    worlds.put(chr.getWorld(), chrr);
                } else {
                    chrr = worlds.get(chr.getWorld());
                }
                chrr.add(chr);
            }
        }
        for (Entry<Byte, ArrayList<MapleCharacter>> w : worlds.entrySet()) {
            c.getSession().write(LoginPacket.showAllCharacterInfo(w.getKey(), w.getValue(), c.getSecondPassword()));
        }
    }
}
