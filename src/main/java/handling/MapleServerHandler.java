/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2012 Patrick Huy <patrick.huy@frz.cc> 
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
package handling;

import client.MapleClient;
import client.inventory.MaplePet;
import client.inventory.PetDataFactory;
import constants.GameConstants;
import constants.ServerConstants;
import handling.cashshop.CashShopServer;
import handling.cashshop.handler.CashShopOperation;
import handling.cashshop.handler.MTSOperation;
import handling.channel.ChannelServer;
import handling.channel.handler.*;
import handling.login.LoginServer;
import handling.login.handler.CharLoginHandler;
import handling.netty.MaplePacketDecoder;
import handling.netty.MapleSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.management.*;

import scripting.NPCScriptManager;
import server.MTSStorage;
import server.Randomizer;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.MapleAESOFB;
import tools.Pair;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;
import tools.packet.LoginPacket;
import tools.packet.MTSCSPacket;

public class MapleServerHandler extends ChannelInboundHandlerAdapter implements MapleServerHandlerMBean {

    public static boolean Log_Packets = false;
    private int channel = -1;
    private static int numDC = 0;
    private static long lastDC = System.currentTimeMillis();
    private boolean cs;
    private final List<String> BlockedIP = new ArrayList<>();
    private final Map<String, Pair<Long, Byte>> tracker = new ConcurrentHashMap<>();
    private static final EnumSet<RecvPacketOpcode> blocked = EnumSet.noneOf(RecvPacketOpcode.class), sBlocked = EnumSet.noneOf(RecvPacketOpcode.class);


    // <editor-fold defaultstate="collapsed" desc="Packet Log Implementation">
    private static final int Log_Size = 10000, Packet_Log_Size = 25;
    private static final ArrayList<LoggedPacket> Packet_Log = new ArrayList<>(Log_Size);
    private static final ReentrantReadWriteLock Packet_Log_Lock = new ReentrantReadWriteLock();
    private static String Packet_Log_Output = "/var/www/logs/";
    private static int Packet_Log_Index = 0;

    public static void log(String packet, String op, MapleClient c, ChannelHandlerContext ctx) {
        try {
            Packet_Log_Lock.writeLock().lock();
            LoggedPacket logged = null;
            if (Packet_Log.size() == Log_Size) {
                logged = Packet_Log.remove(0);
            }
            //This way, we don't create new LoggedPacket objects, we reuse them =]
            if (logged == null) {
                logged = new LoggedPacket(packet, op, ctx.channel().remoteAddress().toString(),
                        c == null ? -1 : c.getAccID(), FileoutputUtil.CurrentReadable_Time(),
                        c == null || c.getAccountName() == null ? "[Null]" : c.getAccountName(),
                        c == null || c.getPlayer() == null || c.getPlayer().getName() == null ? "[Null]" : c.getPlayer().getName(),
                        c == null || c.getPlayer() == null || c.getPlayer().getMap() == null ? "[Null]" : String.valueOf(c.getPlayer().getMapId()),
                        c == null || NPCScriptManager.getInstance().getCM(c) == null ? "[Null]" : String.valueOf(NPCScriptManager.getInstance().getCM(c).getNpc()));
            } else {
                logged.setInfo(packet, op, ctx.channel().remoteAddress().toString(),
                        c == null ? -1 : c.getAccID(), FileoutputUtil.CurrentReadable_Time(),
                        c == null || c.getAccountName() == null ? "[Null]" : c.getAccountName(),
                        c == null || c.getPlayer() == null || c.getPlayer().getName() == null ? "[Null]" : c.getPlayer().getName(),
                        c == null || c.getPlayer() == null || c.getPlayer().getMap() == null ? "[Null]" : String.valueOf(c.getPlayer().getMapId()),
                        c == null || NPCScriptManager.getInstance().getCM(c) == null ? "[Null]" : String.valueOf(NPCScriptManager.getInstance().getCM(c).getNpc()));
            }
            Packet_Log.add(logged);
        } finally {
            Packet_Log_Lock.writeLock().unlock();
        }
    }

    private static class LoggedPacket {

        private static final String nl = System.getProperty("line.separator");
        private String ip, accName, accId, chrName, packet, mapId, npcId, op, time;
        private long timestamp;

        public LoggedPacket(String p, String op, String ip, int id, String time, String accName, String chrName, String mapId, String npcId) {
            setInfo(p, op, ip, id, time, accName, chrName, mapId, npcId);
        }

        public final void setInfo(String p, String op, String ip, int id, String time, String accName, String chrName, String mapId, String npcId) {
            this.ip = ip;
            this.op = op;
            this.time = time;
            this.packet = p;
            this.accName = accName;
            this.chrName = chrName;
            this.mapId = mapId;
            this.npcId = npcId;
            timestamp = System.currentTimeMillis();
            this.accId = String.valueOf(id);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[IP: ").append(ip).append("] [").append(accId).append('|').append(accName).append('|').append(chrName).append("] [").append(npcId).append('|').append(mapId).append("] [Time: ").append(timestamp).append("] [").append(time).append(']');
            sb.append(nl);
            sb.append("[Op: ").append(op).append("] [").append(packet).append(']');
            return sb.toString();
        }
    }

    public static void registerMBean() {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            MapleServerHandler mbean = new MapleServerHandler();
            //The log is a static object, so we can just use this hacky method.
            mBeanServer.registerMBean(mbean, new ObjectName("handling:type=MapleServerHandler"));
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException |
                 NotCompliantMBeanException e) {
            System.out.println("Error registering PacketLog MBean");
        }
    }

    @Override
    public void writeLog() {
        writeLog(false);
    }

    public void writeLog(boolean crash) {
        Packet_Log_Lock.readLock().lock();
        try {
            try (FileWriter fw = new FileWriter(new File(Packet_Log_Output + Packet_Log_Index + (crash ? "_DC.txt" : ".txt")), true)) {
                String nl = System.getProperty("line.separator");
                for (LoggedPacket loggedPacket : Packet_Log) {
                    fw.write(loggedPacket.toString());
                    fw.write(nl);
                }
                final String logString = "Log has been written at " + lastDC + " [" + FileoutputUtil.CurrentReadable_Time() + "] - " + numDC + " have disconnected, within " + (System.currentTimeMillis() - lastDC) + " milliseconds. (" + System.currentTimeMillis() + ")";
                System.out.println(logString);
                fw.write(logString);
                fw.write(nl);
                fw.flush();
            }
            Packet_Log.clear();
            Packet_Log_Index++;
            if (Packet_Log_Index > Packet_Log_Size) {
                Packet_Log_Index = 0;
                Log_Packets = false;
            }
        } catch (IOException ex) {
            System.out.println("Error writing log to file.");
        } finally {
            Packet_Log_Lock.readLock().unlock();
        }

    }

    public static void initiate() {
        //RecvPacketOpcode[] block = new RecvPacketOpcode[]{RecvPacketOpcode.NPC_ACTION, RecvPacketOpcode.MOVE_PLAYER, RecvPacketOpcode.PONG, RecvPacketOpcode.MOVE_PET, RecvPacketOpcode.MOVE_SUMMON, RecvPacketOpcode.MOVE_DRAGON, RecvPacketOpcode.MOVE_LIFE, RecvPacketOpcode.MOVE_ANDROID, RecvPacketOpcode.HEAL_OVER_TIME, RecvPacketOpcode.AUTO_AGGRO, RecvPacketOpcode.CANCEL_DEBUFF, RecvPacketOpcode.MOVE_FAMILIAR};
        //    RecvPacketOpcode[] serverBlock = new RecvPacketOpcode[]{RecvPacketOpcode.CHANGE_KEYMAP, RecvPacketOpcode.ITEM_PICKUP, RecvPacketOpcode.PET_LOOT, RecvPacketOpcode.TAKE_DAMAGE, RecvPacketOpcode.FACE_EXPRESSION, RecvPacketOpcode.USE_ITEM, RecvPacketOpcode.CLOSE_RANGE_ATTACK, RecvPacketOpcode.MAGIC_ATTACK, RecvPacketOpcode.RANGED_ATTACK, RecvPacketOpcode.ARAN_COMBO, RecvPacketOpcode.SPECIAL_MOVE, RecvPacketOpcode.GENERAL_CHAT, RecvPacketOpcode.MONSTER_BOMB, RecvPacketOpcode.PASSIVE_ENERGY, RecvPacketOpcode.PET_AUTO_POT, RecvPacketOpcode.USE_CASH_ITEM, RecvPacketOpcode.PARTYCHAT, RecvPacketOpcode.CANCEL_BUFF, RecvPacketOpcode.SKILL_EFFECT, RecvPacketOpcode.CHAR_INFO_REQUEST, RecvPacketOpcode.ALLIANCE_OPERATION, RecvPacketOpcode.AUTO_ASSIGN_AP, RecvPacketOpcode.DISTRIBUTE_AP, RecvPacketOpcode.USE_MAGNIFY_GLASS, RecvPacketOpcode.SPAWN_PET, RecvPacketOpcode.SUMMON_ATTACK, RecvPacketOpcode.ITEM_MOVE, RecvPacketOpcode.PARTY_SEARCH_STOP};
        //   blocked.addAll(Arrays.asList(block));
        //    sBlocked.addAll(Arrays.asList(serverBlock));
        if (Log_Packets) {
            for (int i = 1; i <= Packet_Log_Size; i++) {
                if (!(new File(Packet_Log_Output + i + ".txt")).exists() && !(new File(Packet_Log_Output + i + "_DC.txt")).exists()) {
                    Packet_Log_Index = i;
                    break;
                }
            }
            // if (Packet_Log_Index <= 0) { //25+ files, do not log
            //     Log_Packets = false;
            //   }
        }

        registerMBean();
    }

    public MapleServerHandler() {
        //ONLY FOR THE MBEAN
    }
    // </editor-fold>

    public MapleServerHandler(final int channel, final boolean cs) {
        this.channel = channel;
        this.cs = cs;
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        /*
         * MapleClient client = (MapleClient)
         * session.getAttribute(MapleClient.CLIENT_KEY);
         * log.error(MapleClient.getLogMessage(client, cause.getMessage()),
         * cause);
         */
        // cause.printStackTrace();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        // Start of IP checking
        final String address = ctx.channel().remoteAddress().toString().split(":")[0];

        if (BlockedIP.contains(address)) {
            ctx.channel().close();
            return;
        }
        final Pair<Long, Byte> track = tracker.get(address);

        byte count;
        if (track == null) {
            count = 1;
        } else {
            count = track.right;

            final long difference = System.currentTimeMillis() - track.left;
            if (difference < 2000) { // Less than 2 sec
                count++;
            } else if (difference > 20000) { // Over 20 sec
                count = 1;
            }
            if (count >= 10) {
                BlockedIP.add(address);
                tracker.remove(address); // Cleanup
                ctx.channel().close();
                return;
            }
        }
        tracker.put(address, new Pair<>(System.currentTimeMillis(), count));
        // End of IP checking.
        String IP = address.substring(address.indexOf('/') + 1, address.length());
        if (channel > -1) {
            if (ChannelServer.getInstance(channel).isShutdown()) {
                ctx.channel().close();
                return;
            }
            if (!LoginServer.containsIPAuth(IP)) {
                ctx.channel().close();
                return;
            }

        } else if (cs) {
            if (CashShopServer.isShutdown()) {
                ctx.channel().close();
                return;
            }
        } else {
            if (LoginServer.isShutdown()) {
                ctx.channel().close();
                return;
            }
        }
        LoginServer.removeIPAuth(IP);
        final byte ivRecv[] = new byte[]{(byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255)};
        final byte ivSend[] = new byte[]{(byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255)};

        final MapleClient client = new MapleClient(
                new MapleAESOFB(ivSend, (short) (0xFFFF - ServerConstants.MAPLE_VERSION)), // Sent Cypher
                new MapleAESOFB(ivRecv, ServerConstants.MAPLE_VERSION), // Recv Cypher
                new MapleSession(ctx.channel()));
        client.setChannel(channel);

        MaplePacketDecoder.DecoderState decoderState = new MaplePacketDecoder.DecoderState();
        ctx.channel().attr(MaplePacketDecoder.DECODER_STATE_KEY).set(decoderState);


        ctx.writeAndFlush(LoginPacket.getHello(ServerConstants.MAPLE_VERSION, ivSend, ivRecv));

        ctx.channel().attr(MapleClient.CLIENT_KEY).set(client);

        StringBuilder sb = new StringBuilder();
        if (channel > -1) {
            sb.append(address).append("[Channel Server] Attempting to connect to the channel server.");
        } else if (cs) {
            sb.append(address).append("[Cash Server] Attempting to connect to Maple Valley Mall server.");
        } else {
            sb.append(address).append("[Login Server] Attempting to connect to the world server.");
        }
        System.out.println(sb.toString());
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        final MapleClient client = (MapleClient) ctx.channel().attr(MapleClient.CLIENT_KEY).get();

        if (client != null) {
            byte state = MapleClient.CHANGE_CHANNEL;
            if (Log_Packets && !LoginServer.isShutdown() && !cs && channel > -1) {
                state = client.getLoginState();
            }
            if (state != MapleClient.CHANGE_CHANNEL) {
                log("Data: " + numDC, "CLOSED", client, ctx);
                if (System.currentTimeMillis() - lastDC < 600000) { //within the minute
                    numDC++;
                    if (numDC >= 1) { //100+ people have dc'd in minute in channelserver
                        System.out.println("Writing log...");
                        writeLog();
                        numDC = 0;
                        lastDC = System.currentTimeMillis(); //intentionally place here
                    }
                } else {
                    numDC = 0;
                    lastDC = System.currentTimeMillis(); //intentionally place here
                }
            }
            try {
                client.disconnect(true, cs);
            } finally {
                ctx.channel().close();
                ctx.channel().attr(MapleClient.CLIENT_KEY).set(null);
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object status) throws Exception {
        final MapleClient client = (MapleClient) ctx.channel().attr(MapleClient.CLIENT_KEY).get();

        if (client != null) {
            client.sendPing();
        } else {
            ctx.channel().close();
            System.out.println("Netty detects heartbeat disconnection.");
        }
        super.userEventTriggered(ctx, status);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object message) {
        if (message == null || ctx == null) {
            return;
        }
        final LittleEndianAccessor slea = new LittleEndianAccessor(new ByteArrayByteStream((byte[]) message));
        if (slea.available() < 2) {
            return;
        }
        final MapleClient c = (MapleClient) ctx.channel().attr(MapleClient.CLIENT_KEY).get();
        if (c == null || !c.isReceiving()) {
            return;
        }
        final short header_num = slea.readShort();


        // final StringBuilder sb = new StringBuilder("Received data :\n");
        //  sb.append(HexTool.toString((byte[]) message)).append("\n").append(HexTool.toStringFromAscii((byte[]) message));

        //System.out.println(sb.toString());

        for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
            if (recv.getValue() == header_num) {
                if (recv.NeedsChecking()) {
                    if (!c.isLoggedIn()) {
                        return;
                    }
                }
                try {
                    //no login packets
                    if (Log_Packets && !blocked.contains(recv) && !sBlocked.contains(recv) && (cs || channel > -1)) {
                        log(slea.toString(), recv.toString(), c, ctx);
                    }
                    handlePacket(recv, slea, c, cs);
                    //Log after the packet is handle. You'll see why =]
                } catch (NegativeArraySizeException | ArrayIndexOutOfBoundsException e) {
                    //swallow, no one cares
                    if (!ServerConstants.Use_Fixed_IV) {
                        FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                        FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "Packet: " + header_num + "\n" + slea.toString(true));
                    }
                } catch (Exception e) {
                    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                    FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "Packet: " + header_num + "\n" + slea.toString(true));
                }

                return;
            }
        }
        //  final StringBuilder sbd = new StringBuilder("Received data : (Unhandled)\n");
        //sbd.append(HexTool.toString((byte[]) message)).append("\n").append(HexTool.toStringFromAscii((byte[]) message));
        //System.out.println(sbd.toString());
    }

    public static void handlePacket(final RecvPacketOpcode header, final LittleEndianAccessor slea, final MapleClient c, final boolean cs) throws Exception {
        switch (header) {
            case PONG:
                c.pongReceived();
                break;
            case LOGIN_PASSWORD:
                CharLoginHandler.login(slea, c);
                break;
            case CLIENT_START:
            case CLIENT_FAILED:
                // c.getSession().write(LoginPacket.getCustomEncryption());
                break;
            case VIEW_SERVERLIST:
                if (slea.readByte() == 0) {
                    CharLoginHandler.ServerListRequest(c);
                }
                break;
            case REDISPLAY_SERVERLIST:
            case SERVERLIST_REQUEST:
                CharLoginHandler.ServerListRequest(c);
                break;
            case CLIENT_HELLO:
                if (slea.readByte() != 8 || slea.readShort() != ServerConstants.MAPLE_VERSION || !String.valueOf(slea.readShort()).equals(ServerConstants.MAPLE_PATCH)) {
                    c.getSession().close();
                }
                break;
            case CHARLIST_REQUEST:
                CharLoginHandler.CharlistRequest(slea, c);
                break;
            case SERVERSTATUS_REQUEST:
                CharLoginHandler.ServerStatusRequest(c);
                break;
            case CHECK_CHAR_NAME:
                CharLoginHandler.CheckCharName(slea.readMapleAsciiString(), c);
                break;
            case CREATE_CHAR:
            case CREATE_SPECIAL_CHAR:
                CharLoginHandler.CreateChar(slea, c);
                break;
            case CREATE_ULTIMATE:
                CharLoginHandler.CreateUltimate(slea, c);
                break;
            case DELETE_CHAR:
                CharLoginHandler.DeleteChar(slea, c);
                break;
            case VIEW_ALL_CHAR:
                CharLoginHandler.ViewChar(slea, c);
                break;
            case PICK_ALL_CHAR:
                //System.out.println("PICK_ALL_CHAR");
                CharLoginHandler.Character_WithoutSecondPassword(slea, c, false, true);
                break;
            case CHAR_SELECT_NO_PIC:
                // System.out.println("CHAR_SELECT_NO_PIC");
                CharLoginHandler.Character_WithoutSecondPassword(slea, c, false, false);
                break;
            case VIEW_REGISTER_PIC:
                //  System.out.println("VIEW_REGISTER_PIC");
                CharLoginHandler.Character_WithoutSecondPassword(slea, c, true, true);
                break;
            case CHAR_SELECT:
                // System.out.println("CHAR_SELECT");
                CharLoginHandler.Character_WithoutSecondPassword(slea, c, true, false);
                break;
            case VIEW_SELECT_PIC:
                // System.out.println("VIEW_SELECT_PIC");
                CharLoginHandler.Character_WithSecondPassword(slea, c, true);
                break;
            case AUTH_SECOND_PASSWORD:
                // System.out.println("AUTH_SECOND_PASSWORD");
                CharLoginHandler.Character_WithSecondPassword(slea, c, false);
                break;
            case CHARACTER_CARD:
                CharLoginHandler.updateCCards(slea, c);
                break;
            case CLIENT_ERROR:
                if (slea.available() < 8) {
                    return;
                }
                short type = slea.readShort();
                String type_str = "Unknown?!";
                if (type == 0x01) {
                    type_str = "SendBackupPacket";
                } else if (type == 0x02) {
                    type_str = "Crash Report";
                } else if (type == 0x03) {
                    type_str = "Exception";
                }
                int errortype = slea.readInt(); // example error 38
                if (errortype == 0) { // i don't wanna log error code 0 stuffs, (usually some bounceback to login)
                    return;
                }
                short data_length = slea.readShort();
                slea.skip(4); // ?B3 86 01 00 00 00 FF 00 00 00 00 00 9E 05 C8 FF 02 00 CD 05 C9 FF 7D 00 00 00 3F 00 00 00 00 00 02 77 01 00 25 06 C9 FF 7D 00 00 00 40 00 00 00 00 00 02 C1 02
                short opcodeheader = slea.readShort();
                FileoutputUtil.log("ErrorCodes.txt", "Error Type: " + errortype + "\r\n" + "Data Length: " + data_length + "\r\n" + "Character: " + c.getPlayer().getName() + " Map: " + c.getPlayer().getMap().getId() + " - Account: " + c.getAccountName() + "\r\n" + SendPacketOpcode.getOpcodeName(opcodeheader) + " Opcode: " + opcodeheader + "\r\n" + HexTool.toString(slea.read((int) slea.available())) + "\r\n\r\n");
                break;
            case ENABLE_SPECIAL_CREATION:
                c.getSession().write(LoginPacket.enableSpecialCreation(c.getAccID(), true));
                break;
            // END OF LOGIN SERVER
            case CHANGE_CHANNEL:
            case CHANGE_ROOM_CHANNEL:
                InterServerHandler.ChangeChannel(slea, c, c.getPlayer(), header == RecvPacketOpcode.CHANGE_ROOM_CHANNEL);
                break;
            case PLAYER_LOGGEDIN:
                final int playerid = slea.readInt();
                if (cs) {
                    CashShopOperation.EnterCS(playerid, c);
                } else {
                    InterServerHandler.Loggedin(playerid, c);
                }
                break;
            case ENTER_PVP:
            case ENTER_PVP_PARTY:
                PlayersHandler.EnterPVP(slea, c);
                break;
            case PVP_RESPAWN:
                PlayersHandler.RespawnPVP(slea, c);
                break;
            case LEAVE_PVP:
                PlayersHandler.LeavePVP(slea, c);
                break;
            case PVP_ATTACK:
                PlayersHandler.AttackPVP(slea, c);
                break;
            case PVP_SUMMON:
                SummonHandler.SummonPVP(slea, c);
                break;
            case ENTER_CASH_SHOP:
                InterServerHandler.EnterCS(c, c.getPlayer(), false);
                break;
            case ENTER_MTS:
                InterServerHandler.EnterMTS(c, c.getPlayer());
                //  InterServerHandler.EnterCS(c, c.getPlayer(), true);
                break;
            case MOVE_PLAYER:
                PlayerHandler.MovePlayer(slea, c, c.getPlayer());
                break;
            case CHAR_INFO_REQUEST:
                slea.readInt();
                PlayerHandler.CharInfoRequest(slea.readInt(), c, c.getPlayer());
                break;
            case CLOSE_RANGE_ATTACK:
                PlayerHandler.closeRangeAttack(slea, c, c.getPlayer(), false);
                break;
            case PART_TIME_JOB:
                System.out.println("PTJ accessed");
                CharLoginHandler.PartTimeJob(slea, c);
                break;
            case MAGIC_WHEEL:
                InventoryHandler.UseMagicWheel(slea, c);
                break;
            case RESET_CORE_AURA:
            case SOLIDIFY_CORE_AURA:
                InventoryHandler.ResetCoreAura(slea.readInt(), c, c.getPlayer());
                break;
            case RANGED_ATTACK:
                PlayerHandler.rangedAttack(slea, c, c.getPlayer());
                break;
            case MAGIC_ATTACK:
                PlayerHandler.MagicDamage(slea, c, c.getPlayer());
                break;
            case SPECIAL_MOVE:
                PlayerHandler.SpecialMove(slea, c, c.getPlayer());
                break;
            case PASSIVE_ENERGY:
                PlayerHandler.closeRangeAttack(slea, c, c.getPlayer(), true);
                break;
            case GET_BOOK_INFO:
                PlayersHandler.MonsterBookInfoRequest(slea, c, c.getPlayer());
                break;
            case MONSTER_BOOK_DROPS:
                PlayersHandler.MonsterBookDropsRequest(slea, c, c.getPlayer());
                break;
            case YOUR_INFORMATION:
                PlayersHandler.loadInfo(slea, c, c.getPlayer());
                break;
            case FIND_FRIEND:
                PlayersHandler.findFriend(slea, c, c.getPlayer());
                break;
            case CHANGE_CODEX_SET:
                // 41 = honor level up
                PlayersHandler.ChangeSet(slea, c, c.getPlayer());
                break;
            case PROFESSION_INFO:
                ItemMakerHandler.ProfessionInfo(slea, c);
                break;
            case CRAFT_DONE:
                ItemMakerHandler.CraftComplete(slea, c, c.getPlayer());
                break;
            case CRAFT_MAKE:
                ItemMakerHandler.CraftMake(slea, c, c.getPlayer());
                break;
            case CRAFT_EFFECT:
                ItemMakerHandler.CraftEffect(slea, c, c.getPlayer());
                break;
            case START_HARVEST:
                ItemMakerHandler.StartHarvest(slea, c, c.getPlayer());
                break;
            case STOP_HARVEST:
                ItemMakerHandler.StopHarvest(slea, c, c.getPlayer());
                break;
            case MAKE_EXTRACTOR:
                ItemMakerHandler.MakeExtractor(slea, c, c.getPlayer());
                break;
            case USE_BAG:
                ItemMakerHandler.UseBag(slea, c, c.getPlayer());
                break;
            case USE_FAMILIAR:
                MobHandler.UseFamiliar(slea, c, c.getPlayer());
                break;
            case SPAWN_FAMILIAR:
                MobHandler.SpawnFamiliar(slea, c, c.getPlayer());
                break;
            case RENAME_FAMILIAR:
                MobHandler.RenameFamiliar(slea, c, c.getPlayer());
                break;
            case MOVE_FAMILIAR:
                MobHandler.MoveFamiliar(slea, c, c.getPlayer());
                break;
            case ATTACK_FAMILIAR:
                MobHandler.AttackFamiliar(slea, c, c.getPlayer());
                break;
            case TOUCH_FAMILIAR:
                MobHandler.TouchFamiliar(slea, c, c.getPlayer());
                break;
            case USE_RECIPE:
                ItemMakerHandler.UseRecipe(slea, c, c.getPlayer());
                break;
            case MOVE_ANDROID:
                PlayerHandler.MoveAndroid(slea, c, c.getPlayer());
                break;
            case FACE_EXPRESSION:
                PlayerHandler.ChangeEmotion(slea.readInt(), c.getPlayer());
                break;
            case FACE_ANDROID:
                PlayerHandler.ChangeAndroidEmotion(slea.readInt(), c.getPlayer());
                break;
            case TAKE_DAMAGE:
                PlayerHandler.TakeDamage(slea, c, c.getPlayer());
                break;
            case HEAL_OVER_TIME:
                PlayerHandler.Heal(slea, c.getPlayer());
                break;
            case CANCEL_BUFF:
                PlayerHandler.CancelBuffHandler(slea.readInt(), c.getPlayer());
                break;
            case MECH_CANCEL:
                PlayerHandler.CancelMech(slea, c.getPlayer());
                break;
            case CANCEL_ITEM_EFFECT:
                PlayerHandler.CancelItemEffect(slea.readInt(), c.getPlayer());
                break;
            case USE_TITLE:
                PlayerHandler.UseTitle(slea.readInt(), c, c.getPlayer());
                break;
            case USE_CHAIR:
                PlayerHandler.UseChair(slea.readInt(), c, c.getPlayer());
                break;
            case CANCEL_CHAIR:
                PlayerHandler.CancelChair(slea.readShort(), c, c.getPlayer());
                break;
            case WHEEL_OF_FORTUNE:
                break; //whatever
            case USE_ITEMEFFECT:
                PlayerHandler.UseItemEffect(slea.readInt(), c, c.getPlayer());
                break;
            case SKILL_EFFECT:
                PlayerHandler.SkillEffect(slea, c.getPlayer());
                break;
            case QUICK_SLOT:
                PlayerHandler.QuickSlot(slea, c.getPlayer());
                break;
            case MESO_DROP:
                slea.readInt();
                PlayerHandler.DropMeso(slea.readInt(), c.getPlayer());
                break;
            case CHANGE_KEYMAP:
                PlayerHandler.ChangeKeymap(slea, c.getPlayer());
                break;
            case UPDATE_ENV:
                // We handle this in MapleMap
                break;
            case CHANGE_MAP:
                if (cs) {
                    CashShopOperation.LeaveCS(slea, c, c.getPlayer());
                } else {
                    PlayerHandler.ChangeMap(slea, c, c.getPlayer());
                }
                break;
            case CHANGE_MAP_SPECIAL:
                slea.skip(1);
                PlayerHandler.ChangeMapSpecial(slea.readMapleAsciiString(), c, c.getPlayer());
                break;
            case USE_INNER_PORTAL:
                slea.skip(1);
                PlayerHandler.InnerPortal(slea, c, c.getPlayer());
                break;
            case TROCK_ADD_MAP:
                PlayerHandler.TrockAddMap(slea, c, c.getPlayer());
                break;
            case ARAN_COMBO:
                PlayerHandler.AranCombo(c, c.getPlayer(), 1);
                break;
            case SKILL_MACRO:
                PlayerHandler.ChangeSkillMacro(slea, c.getPlayer());
                break;
            case GIVE_FAME:
                PlayersHandler.GiveFame(slea, c, c.getPlayer());
                break;
            case TRANSFORM_PLAYER:
                PlayersHandler.TransformPlayer(slea, c, c.getPlayer());
                break;
            case NOTE_ACTION:
                PlayersHandler.Note(slea, c.getPlayer());
                break;
            case USE_DOOR:
                PlayersHandler.UseDoor(slea, c.getPlayer());
                break;
            case USE_MECH_DOOR:
                PlayersHandler.UseMechDoor(slea, c.getPlayer());
                break;
            case DAMAGE_REACTOR:
                PlayersHandler.HitReactor(slea, c);
                break;
            case CLICK_REACTOR:
            case TOUCH_REACTOR:
                PlayersHandler.TouchReactor(slea, c);
                break;
            case CLOSE_CHALKBOARD: //小黑板
                c.getPlayer().setChalkboard(null);
                break;
            case ITEM_SORT:
                InventoryHandler.ItemSort(slea, c);
                break;
            case ITEM_GATHER: //整理背包
                InventoryHandler.ItemGather(slea, c);
                break;
            case ITEM_MOVE:
                InventoryHandler.ItemMove(slea, c);
                break;
            case MOVE_BAG:
                InventoryHandler.MoveBag(slea, c);
                break;
            case SWITCH_BAG:
                InventoryHandler.SwitchBag(slea, c);
                break;
            case ITEM_MAKER:
                ItemMakerHandler.ItemMaker(slea, c);
                break;
            case ITEM_PICKUP:
                //  System.out.println("dddpickup");
                InventoryHandler.Pickup_Player(slea, c, c.getPlayer());
                break;
            case USE_CASH_ITEM:
                InventoryHandler.UseCashItem(slea, c);
                break;
            case USE_ITEM:
                InventoryHandler.UseItem(slea, c, c.getPlayer());
                break;
            case USE_COSMETIC:
                InventoryHandler.UseCosmetic(slea, c, c.getPlayer());
                break;
            case USE_MAGNIFY_GLASS:
                InventoryHandler.UseMagnify(slea, c);
                break;
            case USE_SCRIPTED_NPC_ITEM:
                InventoryHandler.UseScriptedNPCItem(slea, c, c.getPlayer());
                break;
            case USE_RETURN_SCROLL:
                InventoryHandler.UseReturnScroll(slea, c, c.getPlayer());
                break;
            case USE_NEBULITE:
                InventoryHandler.UseNebulite(slea, c);
                break;
            case USE_ALIEN_SOCKET:
                InventoryHandler.UseAlienSocket(slea, c);
                break;
            case USE_ALIEN_SOCKET_RESPONSE:
                slea.skip(4); // all 0
                c.getSession().write(MTSCSPacket.useAlienSocket(false));
                break;
            case VICIOUS_HAMMER:
                slea.skip(4); // 3F 00 00 00
                slea.skip(4); // all 0
                c.getSession().write(MTSCSPacket.ViciousHammer(false, 0));
                break;
            case USE_NEBULITE_FUSION:
                InventoryHandler.UseNebuliteFusion(slea, c);
                break;
            case USE_UPGRADE_SCROLL:
                slea.readInt();
                InventoryHandler.UseUpgradeScroll(slea.readShort(), slea.readShort(), slea.readShort(), c, c.getPlayer(), slea.readByte() > 0, false);
                break;
            case USE_FLAG_SCROLL: //装备保护
                slea.readInt();
                InventoryHandler.UseProtectShield(slea, c);
                break;
            case USE_POTENTIAL_SCROLL:
            case USE_EQUIP_SCROLL:
                slea.readInt();
                InventoryHandler.UseUpgradeScroll(slea.readShort(), slea.readShort(), (short) 0, c, c.getPlayer(), slea.readByte() > 0, false);
                break;
            case USE_SUMMON_BAG:
                InventoryHandler.UseSummonBag(slea, c, c.getPlayer());
                break;
            case USE_TREASUER_CHEST:
                InventoryHandler.UseTreasureChest(slea, c, c.getPlayer());
                break;
            case USE_REMOTE:
                CashShopOperation.UseGachapon(slea, c);
                break;
            case USE_SKILL_BOOK:
                slea.readInt();
                InventoryHandler.UseSkillBook((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case USE_CATCH_ITEM:
                InventoryHandler.UseCatchItem(slea, c, c.getPlayer());
                break;
            case USE_MOUNT_FOOD:
                InventoryHandler.UseMountFood(slea, c, c.getPlayer());
                break;
            case REWARD_ITEM:
                InventoryHandler.UseRewardItem((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case HYPNOTIZE_DMG:
                MobHandler.HypnotizeDmg(slea, c.getPlayer());
                break;
            case MOB_NODE:
                MobHandler.MobNode(slea, c.getPlayer());
                break;
            case DISPLAY_NODE:
                MobHandler.DisplayNode(slea, c.getPlayer());
                break;
            case MOVE_LIFE:
                // System.out.println("ddd");
                MobHandler.MoveMonster(slea, c, c.getPlayer());
                break;
            case AUTO_AGGRO:
                MobHandler.AutoAggro(slea.readInt(), c.getPlayer());
                break;
            case FRIENDLY_DAMAGE:
                MobHandler.FriendlyDamage(slea, c.getPlayer());
                break;
            case REISSUE_MEDAL:
                PlayerHandler.ReIssueMedal(slea, c, c.getPlayer());
                break;
            case MONSTER_BOMB:
                MobHandler.MonsterBomb(slea.readInt(), c.getPlayer());
                break;
            case MOB_BOMB:
                MobHandler.MobBomb(slea, c.getPlayer());
                break;
            case NPC_SHOP:
                NPCHandler.NPCShop(slea, c, c.getPlayer());
                break;
            case NPC_TALK:
                NPCHandler.NPCTalk(slea, c, c.getPlayer());
                break;
            case HIRED_REMOTE:
                InventoryHandler.useRemoteHiredMerchant(slea, c);
                break;
            case NPC_TALK_MORE:
                NPCHandler.NPCMoreTalk(slea, c);
                break;
            case NPC_ACTION:
                NPCHandler.NPCAnimation(slea, c);
                break;
            case QUEST_ACTION:
                NPCHandler.QuestAction(slea, c, c.getPlayer());
                break;
            case STORAGE:
                NPCHandler.Storage(slea, c, c.getPlayer());
                break;
            case GENERAL_CHAT:
                if (c.getPlayer() != null && c.getPlayer().getMap() != null) {
                    slea.readInt();
                    ChatHandler.GeneralChat(slea.readMapleAsciiString(), slea.readByte(), c, c.getPlayer());
                }
                break;
            case PARTYCHAT:
                slea.readInt();
                ChatHandler.Others(slea, c, c.getPlayer());
                break;
            case WHISPER:
                ChatHandler.Whisper_Find(slea, c);
                break;
            case MESSENGER:
                ChatHandler.Messenger(slea, c);
                break;
            case AUTO_ASSIGN_AP:
                StatsHandling.AutoAssignAP(slea, c, c.getPlayer());
                break;
            case DISTRIBUTE_AP:
                StatsHandling.DistributeAP(slea, c, c.getPlayer());
                break;
            case DISTRIBUTE_SP:
                slea.readInt();
                StatsHandling.DistributeSP(slea.readInt(), c, c.getPlayer());
                break;
            case PLAYER_INTERACTION:
                PlayerInteractionHandler.PlayerInteraction(slea, c, c.getPlayer());
                break;
            case GUILD_OPERATION:
                GuildHandler.Guild(slea, c);
                break;
            case DENY_GUILD_REQUEST:
                slea.skip(1);
                GuildHandler.DenyGuildRequest(slea.readMapleAsciiString(), c);
                break;
            case ALLIANCE_OPERATION:
                AllianceHandler.HandleAlliance(slea, c, false);
                break;
            case DENY_ALLIANCE_REQUEST:
                AllianceHandler.HandleAlliance(slea, c, true);
                break;
            case PUBLIC_NPC:
                NPCHandler.OpenPublicNpc(slea, c);
                break;
            case BBS_OPERATION:
                BBSHandler.BBSOperation(slea, c);
                break;
            case PARTY_OPERATION:
                PartyHandler.PartyOperation(slea, c);
                break;
            case DENY_PARTY_REQUEST:
                PartyHandler.DenyPartyRequest(slea, c);
                break;
            case ALLOW_PARTY_INVITE:
                PartyHandler.AllowPartyInvite(slea, c);
                break;
            case BUDDYLIST_MODIFY:
                BuddyListHandler.BuddyOperation(slea, c);
                break;
            case CYGNUS_SUMMON:
                UserInterfaceHandler.CygnusSummon_NPCRequest(c);
                break;
            case SHIP_OBJECT:
                UserInterfaceHandler.ShipObjectRequest(slea.readInt(), c);
                break;
            case BUY_CS_ITEM:
                CashShopOperation.BuyCashItem(slea, c, c.getPlayer());
                break;
            case COUPON_CODE:
                //FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "Coupon : \n" + slea.toString(true));
                //System.out.println(slea.toString());
                CashShopOperation.CouponCode(slea.readMapleAsciiString(), c);
                CashShopOperation.CouponCode(slea.readMapleAsciiString(), c);
                CashShopOperation.doCSPackets(c);
                break;
            case CS_UPDATE:
                CashShopOperation.CSUpdate(c);
                break;
            case TOUCHING_MTS:
                MTSOperation.MTSUpdate(MTSStorage.getInstance().getCart(c.getPlayer().getId()), c);
                break;
            case MTS_TAB:
                MTSOperation.MTSOperation(slea, c);
                break;
            case USE_POT:
                ItemMakerHandler.UsePot(slea, c);
                break;
            case CLEAR_POT:
                ItemMakerHandler.ClearPot(slea, c);
                break;
            case FEED_POT:
                ItemMakerHandler.FeedPot(slea, c);
                break;
            case CURE_POT:
                ItemMakerHandler.CurePot(slea, c);
                break;
            case REWARD_POT:
                ItemMakerHandler.RewardPot(slea, c);
                break;
            case DAMAGE_SUMMON:
                slea.skip(4);
                SummonHandler.DamageSummon(slea, c.getPlayer());
                break;
            case MOVE_SUMMON:
                SummonHandler.MoveSummon(slea, c.getPlayer());
                break;
            case SUMMON_ATTACK:
                //         c.getPlayer().dropMessage(5, "durr");
                SummonHandler.SummonAttack(slea, c, c.getPlayer());
                break;
            case MOVE_DRAGON:
                SummonHandler.MoveDragon(slea, c.getPlayer());
                break;
            case DRAGON_FLY:
                SummonHandler.DragonFly(slea, c.getPlayer());
                break;
            case SUB_SUMMON:
                SummonHandler.SubSummon(slea, c.getPlayer());
                break;
            case REMOVE_SUMMON:
                SummonHandler.RemoveSummon(slea, c);
                break;
            case SPAWN_PET:
                PetHandler.SpawnPet(slea, c, c.getPlayer());
                break;
            case MOVE_PET:
                PetHandler.MovePet(slea, c.getPlayer());
                break;
            case PET_CHAT:
                //System.out.println("Pet chat: " + slea.toString());
                if (slea.available() < 12) {
                    break;
                }
                final int petid = GameConstants.GMS ? c.getPlayer().getPetIndex((int) slea.readLong()) : slea.readInt();
                slea.readInt();
                PetHandler.PetChat(petid, slea.readShort(), slea.readMapleAsciiString(), c.getPlayer());
                break;
            case PET_COMMAND:
                MaplePet pet;
                if (GameConstants.GMS) {
                    pet = c.getPlayer().getPet(c.getPlayer().getPetIndex((int) slea.readLong()));
                } else {
                    pet = c.getPlayer().getPet((byte) slea.readInt());
                }
                slea.readByte(); //always 0?
                if (pet == null) {
                    return;
                }
                PetHandler.PetCommand(pet, PetDataFactory.getPetCommand(pet.getPetItemId(), slea.readByte()), c, c.getPlayer());
                break;
            case PET_FOOD:
                PetHandler.PetFood(slea, c, c.getPlayer());
                break;
            case PET_LOOT:
                InventoryHandler.Pickup_Pet(slea, c, c.getPlayer());
                break;
            case PET_AUTO_POT:
                PetHandler.Pet_AutoPotion(slea, c, c.getPlayer());
                break;
            case PET_AUTO_BUFF:
                PetHandler.Pet_AutoBuff(slea, c, c.getPlayer());
                break;
            case MONSTER_CARNIVAL:
                MonsterCarnivalHandler.MonsterCarnival(slea, c);
                break;
            case DUEY_ACTION:
                DueyHandler.DueyOperation(slea, c);
                break;
            case USE_HIRED_MERCHANT:
                HiredMerchantHandler.UseHiredMerchant(c, true);
                break;
            case MERCH_ITEM_STORE:
                HiredMerchantHandler.MerchantItemStore(slea, c);
                break;
            case CANCEL_DEBUFF:
                // Ignore for now
                break;
            case LEFT_KNOCK_BACK:
                PlayerHandler.leftKnockBack(slea, c);
                break;
            case SNOWBALL:
                PlayerHandler.snowBall(slea, c);
                break;
            case COCONUT:
                PlayersHandler.hitCoconut(slea, c);
                break;
            case REPAIR:
                NPCHandler.repair(slea, c);
                break;
            case REPAIR_ALL:
                NPCHandler.repairAll(c);
                break;
            case OWL:
                InventoryHandler.Owl(slea, c);
                break;
            case OWL_WARP:
                InventoryHandler.OwlWarp(slea, c);
                break;
            case USE_OWL_MINERVA:
                InventoryHandler.OwlMinerva(slea, c);
                break;
            case RPS_GAME:
                NPCHandler.RPSGame(slea, c);
                break;
            case UPDATE_QUEST:
                NPCHandler.UpdateQuest(slea, c);
                break;
            case USE_ITEM_QUEST:
                NPCHandler.UseItemQuest(slea, c);
                break;
            case QUEST_ITEM:
                NPCHandler.QuestItem(slea, c);
                break;
            case FOLLOW_REQUEST:
                PlayersHandler.FollowRequest(slea, c);
                break;
            case AUTO_FOLLOW_REPLY:
            case FOLLOW_REPLY:
                PlayersHandler.FollowReply(slea, c);
                break;
            case RING_ACTION:
                PlayersHandler.RingAction(slea, c);
                break;
            case REQUEST_FAMILY:
                FamilyHandler.RequestFamily(slea, c);
                break;
            case OPEN_FAMILY:
                FamilyHandler.OpenFamily(slea, c);
                break;
            case FAMILY_OPERATION:
                FamilyHandler.FamilyOperation(slea, c);
                break;
            case DELETE_JUNIOR:
                FamilyHandler.DeleteJunior(slea, c);
                break;
            case DELETE_SENIOR:
                FamilyHandler.DeleteSenior(slea, c);
                break;
            case USE_FAMILY:
                FamilyHandler.UseFamily(slea, c);
                break;
            case FAMILY_PRECEPT:
                FamilyHandler.FamilyPrecept(slea, c);
                break;
            case FAMILY_SUMMON:
                FamilyHandler.FamilySummon(slea, c);
                break;
            case ACCEPT_FAMILY:
                FamilyHandler.AcceptFamily(slea, c);
                break;
            case SOLOMON:
                PlayersHandler.Solomon(slea, c);
                break;
            case GACH_EXP:
                PlayersHandler.GachExp(slea, c);
                break;
            case PARTY_SEARCH_START:
                PartyHandler.MemberSearch(slea, c);
                break;
            case PARTY_SEARCH_STOP:
                PartyHandler.PartySearch(slea, c);
                break;
            case EXPEDITION_LISTING:
                PartyHandler.PartyListing(slea, c);
                break;
            case EXPEDITION_OPERATION:
                PartyHandler.Expedition(slea, c);
                break;
            case USE_TELE_ROCK:
                InventoryHandler.TeleRock(slea, c);
                break;
            case PAM_SONG:
                InventoryHandler.PamSong(slea, c);
                break;
            case XMAS_SURPRISE:
                CashShopOperation.CsSurprise(slea, c);
                break;
            case INNER_CIRCULATOR:
                InventoryHandler.useInnerCirculator(slea, c);
                break;
            case REPORT:
                PlayersHandler.Report(slea, c);
                break;
            case TEACH_SKILL: //連結技能
                PlayersHandler.TeachSkill(slea, c, c.getPlayer());
                break;
            case SET_GENDER:
                CharLoginHandler.SetGenderRequest(slea, c);
                break;
            case CANCEL_OUT_SWIPE:
                slea.readInt();
                break;
            case EQUIP_STOLEN_SKILL:
                PlayersHandler.UpdateEquippedSkills(slea, c, c.getPlayer());
                break;
            case UPDATE_STOLEN_SKILL:
                PlayersHandler.UpdateStolenSkills(slea, c, c.getPlayer());
                break;
            case SKILL_SWIPE_REQUEST:
                PlayersHandler.SkillSwipeRequest(slea, c, c.getPlayer());
                break;
            case UPDATE_FRIENDSHIP_POINTS: //楓之高校積分
                NPCHandler.UpdateFriendshipPoints(slea, c, c.getPlayer());
                break;
            case USE_PT_EXP_POTION:
                PlayersHandler.UsePtExpPotion(slea, c);
                break;
            default:
                System.out.println("[UNHANDLED] Recv [" + header.toString() + "] found");
                break;
        }
    }
}
