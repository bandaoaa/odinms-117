package client.messages.commands;

import client.*;
import client.inventory.*;
import client.messages.CommandProcessorUtil;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import constants.GameConstants;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.world.World;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import scripting.NPCScriptManager;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleShopFactory;
import server.life.*;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.packet.CWvsContext;

/**
 * @author Emilyx3
 */
public class AdminCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.ADMIN;
    }

    public static class maxSkill extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            HashMap<Skill, SkillEntry> sa = new HashMap<>();
            for (Skill skil : SkillFactory.getAllSkills()) {
                if (GameConstants.isApplicableSkill(skil.getId()) && skil.canBeLearnedBy(c.getPlayer().getJob())) { //no db/additionals/resistance skills
                    sa.put(skil, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
                }
            }
            c.getPlayer().changeSkillsLevel(sa);
            return 1;
        }
    }

    public static class annoyehe extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            ChannelServer cserv = ChannelServer.getInstance(c.getPlayer().getClient().getChannel());
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            for (int i = 1; i <= 10; i++) {
                MapleMap target = cserv.getMapFactory().getMap(200000000);
                MaplePortal targetPortal = target.getPortal(0);
                victim.changeMap(target, targetPortal);
                MapleMap target1 = cserv.getMapFactory().getMap(102000000);
                MaplePortal targetPortal1 = target.getPortal(0);
                victim.changeMap(target1, targetPortal1);
                MapleMap target2 = cserv.getMapFactory().getMap(103000000);
                MaplePortal targetPortal2 = target.getPortal(0);
                victim.changeMap(target2, targetPortal2);
                MapleMap target3 = cserv.getMapFactory().getMap(100000000);
                MaplePortal targetPortal3 = target.getPortal(0);
                victim.changeMap(target3, targetPortal3);
                MapleMap target4 = cserv.getMapFactory().getMap(200000000);
                MaplePortal targetPortal4 = target.getPortal(0);
                victim.changeMap(target4, targetPortal4);
                MapleMap target5 = cserv.getMapFactory().getMap(211000000);
                MaplePortal targetPortal5 = target.getPortal(0);
                victim.changeMap(target5, targetPortal5);
                MapleMap target6 = cserv.getMapFactory().getMap(230000000);
                MaplePortal targetPortal6 = target.getPortal(0);
                victim.changeMap(target6, targetPortal6);
                MapleMap target7 = cserv.getMapFactory().getMap(222000000);
                MaplePortal targetPortal7 = target.getPortal(0);
                victim.changeMap(target7, targetPortal7);
                MapleMap target8 = cserv.getMapFactory().getMap(251000000);
                MaplePortal targetPortal8 = target.getPortal(0);
                victim.changeMap(target8, targetPortal8);
                MapleMap target9 = cserv.getMapFactory().getMap(220000000);
                MaplePortal targetPortal9 = target.getPortal(0);
                victim.changeMap(target9, targetPortal9);
                MapleMap target10 = cserv.getMapFactory().getMap(221000000);
                MaplePortal targetPortal10 = target.getPortal(0);
                victim.changeMap(target10, targetPortal10);
                MapleMap target11 = cserv.getMapFactory().getMap(240000000);
                MaplePortal targetPortal11 = target.getPortal(0);
                victim.changeMap(target11, targetPortal11);
                MapleMap target12 = cserv.getMapFactory().getMap(600000000);
                MaplePortal targetPortal12 = target.getPortal(0);
                victim.changeMap(target12, targetPortal12);
                MapleMap target13 = cserv.getMapFactory().getMap(800000000);
                MaplePortal targetPortal13 = target.getPortal(0);
                victim.changeMap(target13, targetPortal13);
                MapleMap target14 = cserv.getMapFactory().getMap(680000000);
                MaplePortal targetPortal14 = target.getPortal(0);
                victim.changeMap(target14, targetPortal14);
                MapleMap target15 = cserv.getMapFactory().getMap(105040300);
                MaplePortal targetPortal15 = target.getPortal(0);
                victim.changeMap(target15, targetPortal15);
                MapleMap target16 = cserv.getMapFactory().getMap(990000000);
                MaplePortal targetPortal16 = target.getPortal(0);
                victim.changeMap(target16, targetPortal16);
                MapleMap target17 = cserv.getMapFactory().getMap(100000001);
                MaplePortal targetPortal17 = target.getPortal(0);
                victim.changeMap(target17, targetPortal17);
            }
            victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(
                    c.getPlayer().getPosition()));
            return 1;
        }
    }

    public static class huhuhu extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            ChannelServer cserv = ChannelServer.getInstance(c.getPlayer().getClient().getChannel());
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            for (int i = 1; i <= 100; i++) {
                MapleMap target = cserv.getMapFactory().getMap(100000000);
                MaplePortal targetPortal = target.getPortal(0);
                victim.changeMap(target, targetPortal);
            }
            victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(
                    c.getPlayer().getPosition()));
            return 1;
        }
    }

    public static class warpallhere extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                if (mch.getMapId() != c.getPlayer().getMapId()) {
                    mch.changeMap(c.getPlayer().getMap(), c.getPlayer().getPosition());
                }

            }
            return 1;
        }
    }

    public static class derpitem extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getAccountID() == 1) {
                final int itemId = Integer.parseInt(splitted[1]);
                final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);

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
                } else {
                    Item item;

                    if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                        item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                    } else {
                        item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);

                    }
                    if (c.getPlayer().getGMLevel() < 7 && c.getPlayer().getDGM() == 0) {
                        item.setOwner("GM " + c.getPlayer().getName());
                        item.setGMLog(c.getPlayer().getName() + " used !item");
                    } else if (c.getPlayer().getDGM() == 1) {
                        item.setOwner("DGM " + c.getPlayer().getName());
                        item.setGMLog(c.getPlayer().getName() + " used !item");
                    } else if (c.getPlayer().getGMLevel() == 7) {
                        item.setOwner("SV " + c.getPlayer().getName());
                        item.setGMLog(c.getPlayer().getName() + " used !item");
                    } else if (c.getPlayer().getGMLevel() == 99) {
                        item.setOwner(c.getPlayer().getName());
                        item.setGMLog(c.getPlayer().getName() + " used !item");
                    }
                    MapleInventoryManipulator.addbyItem(c, item);
                }
            }
            return 1;
        }
    }

    public static class clearLife extends CommandExecute {

        @Override
        public int execute(final MapleClient c, String[] splitted) {
            Connection con = (Connection) DatabaseConnection.getConnection();
            if (!MapleMapFactory.isMapCleared(c.getPlayer().getMapId())) {
                try (PreparedStatement ps = (PreparedStatement) con.prepareStatement("INSERT INTO wz_clearedlife(mapid) VALUES (?)")) {
                    ps.setInt(1, c.getPlayer().getMapId());
                    ps.executeUpdate();
                } catch (SQLException ex) {
                }
                MapleMapFactory.addClearedMap(c.getPlayer().getMapId());
                MapleMap oldMap = c.getPlayer().getMap();
                MapleMap newMap = c.getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
                for (MapleCharacter ch : oldMap.getCharacters()) {
                    ch.changeMap(newMap);
                }
                c.getPlayer().dropMessage(6, "This map's life has been cleared.");
            } else {
                c.getPlayer().dropMessage(6, "This map's life is already cleared.");
            }
            return 1;
        }
    }

    public static class savetest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getAccID() == 1) {
                if (splitted.length < 2) {
                    c.getPlayer().dropMessage(6, "Enter a number plz");
                    return 0;
                }
                long shit = System.currentTimeMillis();
                int i;
                for (i = 0; i < Integer.parseInt(splitted[1]); i++) {
                    c.getPlayer().saveToDB(false, false);
                    c.getPlayer().dropMessage(5, "Saved..");
                }
                System.out.println("Saving " + i + " times took " + ((System.currentTimeMillis() - shit) / 1000) + " seconds");
            } else {
                c.getPlayer().dropMessage(5, "Not authorized to use this command..");
            }
            return 1;
        }
    }

    public static class CharInfo extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final StringBuilder builder = new StringBuilder();
            final MapleCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (other == null) {
                builder.append("...does not exist");
                c.getPlayer().dropMessage(6, builder.toString());
                return 0;
            }
            if (other.getClient().getLastPing() <= 0) {
                other.getClient().sendPing();
            }
            builder.append(MapleClient.getLogMessage(other, ""));
            builder.append(" at ").append(other.getPosition().x);
            builder.append(" /").append(other.getPosition().y);

            builder.append(" || HP : ");
            builder.append(other.getStat().getHp());
            builder.append(" /");
            builder.append(other.getStat().getCurrentMaxHp());

            builder.append(" || MP : ");
            builder.append(other.getStat().getMp());
            builder.append(" /");
            builder.append(other.getStat().getCurrentMaxMp(other.getJob()));

            builder.append(" || BattleshipHP : ");
            builder.append(other.currentBattleshipHP());

            builder.append(" || WATK : ");
            builder.append(other.getStat().getTotalWatk());
            builder.append(" || MATK : ");
            builder.append(other.getStat().getTotalMagic());
            builder.append(" || MAXDAMAGE : ");
            builder.append(other.getStat().getCurrentMaxBaseDamage());
            builder.append(" || DAMAGE% : ");
            builder.append(other.getStat().dam_r);
            builder.append(" || BOSSDAMAGE% : ");
            builder.append(other.getStat().bossdam_r);
            builder.append(" || CRIT CHANCE : ");
            builder.append(other.getStat().passive_sharpeye_rate());
            builder.append(" || CRIT DAMAGE : ");
            builder.append(other.getStat().passive_sharpeye_percent());

            builder.append(" || STR : ");
            builder.append(other.getStat().getStr());
            builder.append(" || DEX : ");
            builder.append(other.getStat().getDex());
            builder.append(" || INT : ");
            builder.append(other.getStat().getInt());
            builder.append(" || LUK : ");
            builder.append(other.getStat().getLuk());

            builder.append(" || Total STR : ");
            builder.append(other.getStat().getTotalStr());
            builder.append(" || Total DEX : ");
            builder.append(other.getStat().getTotalDex());
            builder.append(" || Total INT : ");
            builder.append(other.getStat().getTotalInt());
            builder.append(" || Total LUK : ");
            builder.append(other.getStat().getTotalLuk());

            builder.append(" || EXP : ");
            builder.append(other.getExp());
            builder.append(" || MESO : ");
            builder.append(other.getMeso());

            builder.append(" || party : ");
            builder.append(other.getParty() == null ? -1 : other.getParty().getId());

            builder.append(" || hasTrade: ");
            builder.append(other.getTrade() != null);
            builder.append(" || Latency: ");
            builder.append(other.getClient().getLatency());
            builder.append(" || PING: ");
            builder.append(other.getClient().getLastPing());
            builder.append(" || PONG: ");
            builder.append(other.getClient().getLastPong());
            builder.append(" || remoteAddress: ");

            other.getClient().DebugMessage(builder);

            c.getPlayer().dropMessage(6, builder.toString());
            return 1;
        }
    }

    /*  public static class Fame extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleCharacter player = c.getPlayer();
     if (splitted.length < 2) {
     c.getPlayer().dropMessage(6, "Syntax: !fame <player> <amount>");
     return 0;
     }
     MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
     int fame;
     try {
     fame = Integer.parseInt(splitted[2]);
     } catch (NumberFormatException nfe) {
     c.getPlayer().dropMessage(6, "Invalid Number...");
     return 0;
     }
     if (victim != null && player.allowedToTarget(victim)) {
     victim.addFame(fame);
     victim.updateSingleStat(MapleStat.FAME, victim.getFame());
     }
     return 1;
     }
     }
     * 
     */
    public static class ReloadOps extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            SendPacketOpcode.reloadValues();
            RecvPacketOpcode.reloadValues();
            c.getPlayer().dropMessage(5, "Reloaded...");
            return 1;
        }
    }

    public static class ReloadDrops extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            ReactorScriptManager.getInstance().clearDrops();
            return 1;
        }
    }

    public static class ReloadPortal extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            PortalScriptManager.getInstance().clearScripts();
            return 1;
        }
    }

    public static class npc extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9900002, null);
            return 1;
        }
    }

    public static class Hide extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            SkillFactory.getSkill(GameConstants.GMS ? 9101002 : 9001004).getEffect(1).applyTo(c.getPlayer());
            return 0;
        }
    }

    public static class ReloadShops extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory.getInstance().clear();
            return 1;
        }
    }

    public static class ReloadEvents extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer instance : ChannelServer.getAllInstances()) {
                instance.reloadEvents();
            }
            return 1;
        }
    }

    public static class ResetMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetFully();
            return 1;
        }
    }
    /*
     public static class ResetQuest extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleQuest.getInstance(Integer.parseInt(splitted[1])).forfeit(c.getPlayer());
     return 1;
     }
     }

     public static class StartQuest extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleQuest.getInstance(Integer.parseInt(splitted[1])).start(c.getPlayer(), Integer.parseInt(splitted[2]));
     return 1;
     }
     }

     public static class CompleteQuest extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleQuest.getInstance(Integer.parseInt(splitted[1])).complete(c.getPlayer(), Integer.parseInt(splitted[2]), Integer.parseInt(splitted[3]));
     return 1;
     }
     }

     public static class FStartQuest extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceStart(c.getPlayer(), Integer.parseInt(splitted[2]), splitted.length >= 4 ? splitted[3] : null);
     return 1;
     }
     }

     public static class FCompleteQuest extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceComplete(c.getPlayer(), Integer.parseInt(splitted[2]));
     return 1;
     }
     }

     public static class HReactor extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
     return 1;
     }
     }

     public static class FHReactor extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).forceHitReactor(Byte.parseByte(splitted[2]));
     return 1;
     }
     }

     public static class DReactor extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleMap map = c.getPlayer().getMap();
     List<MapleMapObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.REACTOR));
     if (splitted[1].equals("all")) {
     for (MapleMapObject reactorL : reactors) {
     MapleReactor reactor2l = (MapleReactor) reactorL;
     c.getPlayer().getMap().destroyReactor(reactor2l.getObjectId());
     }
     } else {
     c.getPlayer().getMap().destroyReactor(Integer.parseInt(splitted[1]));
     }
     return 1;
     }
     }

     public static class SetReactor extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     c.getPlayer().getMap().setReactorState(Byte.parseByte(splitted[1]));
     return 1;
     }
     }

     public static class ResetReactor extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     c.getPlayer().getMap().resetReactors();
     return 1;
     }
     }

     public static class SendAllNote extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {

     if (splitted.length >= 1) {
     String text = StringUtil.joinStringFrom(splitted, 1);
     for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
     c.getPlayer().sendNote(mch.getName(), text);
     }
     } else {
     c.getPlayer().dropMessage(6, "Use it like this, !sendallnote <text>");
     return 0;
     }
     return 1;
     }
     }

     public static class BuffSkill extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     SkillFactory.getSkill(Integer.parseInt(splitted[1])).getEffect(Integer.parseInt(splitted[2])).applyTo(c.getPlayer());
     return 0;
     }
     }

     public static class BuffItem extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleItemInformationProvider.getInstance().getItemEffect(Integer.parseInt(splitted[1])).applyTo(c.getPlayer());
     return 0;
     }
     }

     public static class BuffItemEX extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleItemInformationProvider.getInstance().getItemEffectEX(Integer.parseInt(splitted[1])).applyTo(c.getPlayer());
     return 0;
     }
     }

     public static class ItemSize extends CommandExecute { //test

     @Override
     public int execute(MapleClient c, String[] splitted) {
     c.getPlayer().dropMessage(6, "Number of items: " + MapleItemInformationProvider.getInstance().getAllItems().size());
     return 0;
     }
     }*/

    public static class COSAY extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getGMLevel() <= 7) {
                return 0;
            }
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append("Co-Owner - ");

                sb.append(c.getPlayer().getName());
                sb.append("] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(c.getPlayer().isGM() ? 6 : 5, sb.toString()));
            } else {
                c.getPlayer().dropMessage(6, "Syntax: cosay <message>");
                return 0;
            }
            return 1;
        }
    }

    public static class SSay extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append("Supervisor - ");

                sb.append(c.getPlayer().getName());
                sb.append("] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(c.getPlayer().isGM() ? 6 : 5, sb.toString()));
            } else {
                c.getPlayer().dropMessage(6, "Syntax: ssay <message>");
                return 0;
            }
            return 1;
        }
    }

    public static class OSay extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getAccountID() == 1) {
                if (splitted.length > 1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    sb.append("Owner - ");

                    sb.append(c.getPlayer().getName());
                    sb.append("] ");
                    sb.append(StringUtil.joinStringFrom(splitted, 1));
                    World.Broadcast.broadcastMessage(CWvsContext.serverNotice(c.getPlayer().isGM() ? 6 : 5, sb.toString()));
                } else {
                    c.getPlayer().dropMessage(6, "Syntax: osay <message>");
                    return 0;
                }
            }
            return 1;
        }
    }

    /*
     public static class GiveSkill extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
     Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[2]));
     byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);
     byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 4, 1);

     if (level > skill.getMaxLevel()) {
     level = (byte) skill.getMaxLevel();
     }
     if (masterlevel > skill.getMaxLevel()) {
     masterlevel = (byte) skill.getMaxLevel();
     }
     victim.changeSingleSkillLevel(skill, level, masterlevel);
     return 1;
     }
     }

     public static class UnlockInv extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     java.util.Map<Item, MapleInventoryType> eqs = new HashMap<>();
     boolean add = false;
     if (splitted.length < 2 || splitted[1].equals("all")) {
     for (MapleInventoryType type : MapleInventoryType.values()) {
     for (Item item : c.getPlayer().getInventory(type)) {
     if (ItemFlag.LOCK.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (add) {
     eqs.put(item, type);
     }
     add = false;
     }
     }
     } else if (splitted[1].equals("eqp")) {
     for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).newList()) {
     if (ItemFlag.LOCK.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (add) {
     eqs.put(item, MapleInventoryType.EQUIP);
     }
     add = false;
     }
     } else if (splitted[1].equals("eq")) {
     for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
     if (ItemFlag.LOCK.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (add) {
     eqs.put(item, MapleInventoryType.EQUIP);
     }
     add = false;
     }
     } else if (splitted[1].equals("u")) {
     for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
     if (ItemFlag.LOCK.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (add) {
     eqs.put(item, MapleInventoryType.USE);
     }
     add = false;
     }
     } else if (splitted[1].equals("s")) {
     for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
     if (ItemFlag.LOCK.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (add) {
     eqs.put(item, MapleInventoryType.SETUP);
     }
     add = false;
     }
     } else if (splitted[1].equals("e")) {
     for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
     if (ItemFlag.LOCK.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (add) {
     eqs.put(item, MapleInventoryType.ETC);
     }
     add = false;
     }
     } else if (splitted[1].equals("c")) {
     for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
     if (ItemFlag.LOCK.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
     item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
     add = true;
     //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
     }
     if (add) {
     eqs.put(item, MapleInventoryType.CASH);
     }
     add = false;
     }
     } else {
     c.getPlayer().dropMessage(6, "[all/eqp/eq/u/s/e/c]");
     }

     for (Map.Entry<Item, MapleInventoryType> eq : eqs.entrySet()) {
     c.getPlayer().forceReAddItem_NoUpdate(eq.getKey().copy(), eq.getValue());
     }
     return 1;
     }
     }*/

    /*
     public static class Marry extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (splitted.length < 3) {
     c.getPlayer().dropMessage(6, "Need <name> <itemid>");
     return 0;
     }
     int itemId = Integer.parseInt(splitted[2]);
     if (!GameConstants.isEffectRing(itemId)) {
     c.getPlayer().dropMessage(6, "Invalid itemID.");
     } else {
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
     return 1;
     }
     }
     * 
     */
    /*
     public static class Vac extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (!c.getPlayer().isHidden()) {
     c.getPlayer().dropMessage(6, "You can only vac monsters while in hide.");
     return 0;
     } else {
     for (final MapleMapObject mmo : c.getPlayer().getMap().getAllMonstersThreadsafe()) {
     final MapleMonster monster = (MapleMonster) mmo;
     c.getPlayer().getMap().broadcastMessage(MobPacket.moveMonster(false, -1, 0, monster.getObjectId(), monster.getTruePosition(), c.getPlayer().getLastRes()));
     monster.setPosition(c.getPlayer().getPosition());
     }
     }
     return 1;
     }
     }

     public static class GivePoint extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (c.getPlayer().getAccountID() == 1) {
     if (splitted.length < 3) {
     c.getPlayer().dropMessage(6, "Need playername and amount.");
     return 0;
     }
     MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
     if (chrs == null) {
     c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
     } else {
     chrs.setPoints(chrs.getPoints() + Integer.parseInt(splitted[2]));
     c.getPlayer().dropMessage(6, splitted[1] + " has " + chrs.getPoints() + " points, after giving " + splitted[2] + ".");
     }

     }
     return 1;
     }
     }

     public static class GiveVPoint extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (splitted.length < 3) {
     c.getPlayer().dropMessage(6, "Need playername and amount.");
     return 0;
     }
     MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
     if (chrs == null) {
     c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
     } else {
     chrs.setVPoints(chrs.getVPoints() + Integer.parseInt(splitted[2]));
     c.getPlayer().dropMessage(6, splitted[1] + " has " + chrs.getVPoints() + " vpoints, after giving " + splitted[2] + ".");
     }
     return 1;
     }
     }

     public static class SpeakMap extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     for (MapleCharacter victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
     if (victim.getId() != c.getPlayer().getId()) {
     victim.getMap().broadcastMessage(CField.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
     }
     }
     return 1;
     }
     }

     public static class SpeakChn extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     for (MapleCharacter victim : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
     if (victim.getId() != c.getPlayer().getId()) {
     victim.getMap().broadcastMessage(CField.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
     }
     }
     return 1;
     }
     }

     public static class SpeakWorld extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     for (ChannelServer cserv : ChannelServer.getAllInstances()) {
     for (MapleCharacter victim : cserv.getPlayerStorage().getAllCharacters()) {
     if (victim.getId() != c.getPlayer().getId()) {
     victim.getMap().broadcastMessage(CField.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
     }
     }
     }
     return 1;
     }
     }

     public static class NPC extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     int npcId = Integer.parseInt(splitted[1]);
     MapleNPC npc = MapleLifeFactory.getNPC(npcId);
     if (npc != null && !npc.getName().equals("MISSINGNO")) {
     npc.setPosition(c.getPlayer().getPosition());
     npc.setCy(c.getPlayer().getPosition().y);
     npc.setRx0(c.getPlayer().getPosition().x + 50);
     npc.setRx1(c.getPlayer().getPosition().x - 50);
     npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
     npc.setCustom(true);
     c.getPlayer().getMap().addMapObject(npc);
     c.getPlayer().getMap().broadcastMessage(CField.NPCPacket.spawnNPC(npc, true));
     } else {
     c.getPlayer().dropMessage(6, "You have entered an invalid Npc-Id");
     return 0;
     }
     return 1;
     }
     }

   
     public static class DestroyPNPC extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     try {
     c.getPlayer().dropMessage(6, "Destroying playerNPC...");
     final MapleNPC npc = c.getPlayer().getMap().getNPCByOid(Integer.parseInt(splitted[1]));
     if (npc instanceof PlayerNPC) {
     ((PlayerNPC) npc).destroy(true);
     c.getPlayer().dropMessage(6, "Done");
     } else {
     c.getPlayer().dropMessage(6, "!destroypnpc [objectid]");
     }
     } catch (Exception e) {
     c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
     }
     return 1;
     }
     }

     public static class PS extends CommandExecute {

     protected static StringBuilder builder = new StringBuilder();

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (builder.length() > 1) {
     c.getSession().write(CField.getPacketFromHexString(builder.toString()));
     builder = new StringBuilder();
     } else {
     c.getPlayer().dropMessage(6, "Please enter packet data!");
     }
     return 1;
     }
     }

     public static class APS extends PS {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (splitted.length > 1) {
     builder.append(StringUtil.joinStringFrom(splitted, 1));
     c.getPlayer().dropMessage(6, "String is now: " + builder.toString());
     } else {
     c.getPlayer().dropMessage(6, "Please enter packet data!");
     }
     return 1;
     }
     }

     public static class CPS extends PS {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     builder = new StringBuilder();
     return 1;
     }
     }

     public static class P extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (splitted.length > 1) {
     c.getSession().write(CField.getPacketFromHexString(StringUtil.joinStringFrom(splitted, 1)));
     } else {
     c.getPlayer().dropMessage(6, "Please enter packet data!");
     }
     return 1;
     }
     }

     public static class Packet extends P {
     }

     public static class PTS extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (splitted.length > 1) {
     try {
     c.getSession().getHandler().messageReceived(c.getSession(), (Object) CField.getPacketFromHexString(StringUtil.joinStringFrom(splitted, 1)));
     } catch (Exception e) {
     c.getPlayer().dropMessage(6, "Error: " + e);
     }
     } else {
     c.getPlayer().dropMessage(6, "Please enter packet data!");
     }
     return 1;
     }
     }
     * 
     */

    /*   public static class ReloadMap extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     final int mapId = Integer.parseInt(splitted[1]);
     for (ChannelServer cserv : ChannelServer.getAllInstances()) {
     if (cserv.getMapFactory().isMapLoaded(mapId) && cserv.getMapFactory().getMap(mapId).getCharactersSize() > 0) {
     c.getPlayer().dropMessage(5, "There exists characters on channel " + cserv.getChannel());
     return 0;
     }
     }
     for (ChannelServer cserv : ChannelServer.getAllInstances()) {
     if (cserv.getMapFactory().isMapLoaded(mapId)) {
     cserv.getMapFactory().removeMap(mapId);
     }
     }
     return 1;
     }
     }

     public static class Crash extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
     if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
     victim.getClient().getSession().write(HexTool.getByteArrayFromHexString("1A 00")); //give_buff with no data :D
     return 1;
     } else {
     c.getPlayer().dropMessage(6, "The victim does not exist.");
     return 0;
     }
     }
     }

     public static class GainP extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (splitted.length < 2) {
     c.getPlayer().dropMessage(5, "Need amount.");
     return 0;
     }
     c.getPlayer().setPoints(c.getPlayer().getPoints() + Integer.parseInt(splitted[1]));
     return 1;
     }
     }

     public static class GainVP extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (splitted.length < 2) {
     c.getPlayer().dropMessage(5, "Need amount.");
     return 0;
     }
     c.getPlayer().setVPoints(c.getPlayer().getVPoints() + Integer.parseInt(splitted[1]));
     return 1;
     }
     }
     * 
     */
    public static class MakePNPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getGMLevel() == 99) {
                try {
                    c.getPlayer().dropMessage(6, "Making playerNPC...");
                    MapleCharacter chhr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (chhr == null) {
                        c.getPlayer().dropMessage(6, splitted[1] + " is not online");
                        return 0;
                    }
                    PlayerNPC npc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(), c.getPlayer());
                    npc.addToServer();
                    c.getPlayer().dropMessage(6, "Done");
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
                }
            }
            return 1;
        }
    }

    public static class UnB extends UnBan {
    }

    public static class UnBan extends CommandExecute {

        private String getCommand() {
            return "UnBan";
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[Syntax] !" + getCommand() + " <IGN>");
                return 0;
            }

            byte ret;
            ret = MapleClient.unban(splitted[1]);
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] SQL error.");
                return 0;
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] The character does not exist.");
                return 0;
            } else {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] Successfully unbanned!");

            }
            byte ret_ = MapleClient.unbanIPMacs(splitted[1]);
            if (ret_ == -2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] SQL error.");
            } else if (ret_ == -1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] The character does not exist.");
            } else if (ret_ == 0) {
                c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
            } else if (ret_ == 1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
            } else if (ret_ == 2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
            }
            return ret_ > 0 ? 1 : 0;
        }
    }

    public static class uacc extends CommandExecute {

        private String getCommand() {
            return "UnBanAcc";
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[Syntax] !" + getCommand() + " <name>");
                return 0;
            }
            if (c.getPlayer().getGMLevel() == 6) {
                return 0;
            }
            byte ret;
            ret = MapleClient.unbanaccs(splitted[1]);
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] SQL error.");
                return 0;
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] The character does not exist.");
                return 0;
            } else {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] Successfully unbanned!");

            }
            byte ret_ = MapleClient.unbanbyAccId(splitted[1]);
            if (ret_ == -2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] SQL error.");
            } else if (ret_ == -1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] The character does not exist.");
            } else if (ret_ == 0) {
                c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
            } else if (ret_ == 1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
            } else if (ret_ == 2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
            }
            return ret_ > 0 ? 1 : 0;
        }
    }

    public static class UnbanIP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[Syntax] !unbanip <IGN>");
                return 0;
            }
            byte ret = MapleClient.unbanIPMacs(splitted[1]);
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] SQL error.");
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] The character does not exist.");
            } else if (ret == 0) {
                c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
            } else if (ret == 1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
            } else if (ret == 2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
            }
            if (ret > 0) {
                return 1;
            }
            return 0;
        }
    }

    /* public static class GiveNX extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
     if (splitted.length < 3) {
     c.getPlayer().dropMessage(5, "Need amount.");
     return 0;
     }
     victim.modifyCSPoints(1, Integer.parseInt(splitted[2]), true);
     return 1;
     }
     }

     */
    public static class reloadnpc extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (c.getAccID() == 1) {
                NPCScriptManager.getInstance().reloadScripts();
                c.getPlayer().dropMessage(5, "Done.");
            } else {
                //c.getPlayer().ban("Trying to use owner only command on a player", true, true);
            }
            return 1;
        }
    }

    public static class SaveAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (c.getAccID() == 1) {
                for (ChannelServer chan : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : chan.getPlayerStorage().getAllCharacters()) {
                        chr.saveToDB(false, false);
                        // chr.dropMessage("You've been saved, the server is going to reboot!");
                    }
                }
                c.getPlayer().dropMessage(5, "Done.");
            } else {
                //c.getPlayer().ban("Trying to use owner only command on a player", true, true);
            }
            return 1;
        }
    }

    public static class SaveAllSr extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (c.getAccID() == 1) {
                for (ChannelServer chan : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : chan.getPlayerStorage().getAllCharacters()) {
                        chr.saveToDB(false, false);
                    }
                }
                for (ChannelServer cs : ChannelServer.getAllInstances()) {

                    cs.setShutdown();
                    cs.closeAllMerchant();
                }
                System.exit(0);
            } else {
                // c.getPlayer().ban("Trying to use owner only command on a player", true, true);
            }
            return 1;
        }
    }

    public static class strip extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getAccountID() != 1) {
                c.getPlayer().dropMessage(1, "Only the admin can use this command.");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);

            MapleInventory equipped = victim.getInventory(MapleInventoryType.EQUIPPED);
            MapleInventory equip = victim.getInventory(MapleInventoryType.EQUIP);
            List<Short> ids = new ArrayList<>();
            for (Item item : equipped.newList()) {
                ids.add(item.getPosition());
            }
            for (short id : ids) {
                MapleInventoryManipulator.unequip(victim.getClient(), id, equip.getNextFreeSlot());
            }


            return 1;
        }
    }

    public static class StripEveryone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            ChannelServer cs = c.getChannelServer();
            if (c.getPlayer().getAccountID() != 1) {
                c.getPlayer().dropMessage(1, "Only the admin can use this command.");
                return 0;
            }
            for (MapleCharacter mchr : cs.getPlayerStorage().getAllCharacters()) {
                if (mchr.isGM()) {
                    continue;
                }
                MapleInventory equipped = mchr.getInventory(MapleInventoryType.EQUIPPED);
                MapleInventory equip = mchr.getInventory(MapleInventoryType.EQUIP);
                List<Short> ids = new ArrayList<>();
                for (Item item : equipped.newList()) {
                    ids.add(item.getPosition());
                }
                for (short id : ids) {
                    MapleInventoryManipulator.unequip(mchr.getClient(), id, equip.getNextFreeSlot());
                }
            }

            return 1;
        }
    }

    public static class removegm extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getAccountID() != 1) {
                c.getPlayer().dropMessage(1, "Only the admin can use this command.");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim.getAccountID() != 1 || victim.getGMLevel() != 98 || victim.getGMLevel() < 7) {
                victim.setgmLevel((byte) 0);
                victim.setDGM(0);
                victim.clearInvGM();
                victim.setJob(0);
                victim.getStat().setStr((short) 4, victim);
                victim.getStat().setLuk((short) 4, victim);
                victim.getStat().setInt((short) 4, victim);
                victim.getStat().setDex((short) 4, victim);
                victim.setRemainingAp(0);
                victim.setMSIPoints2(0);
                victim.modifyCSPoints(1, -victim.getCSPoints(1));
                victim.dropMessage(5, "Your GM status has been removed by " + c.getPlayer().getName());
                c.getPlayer().dropMessage(5, "You have removed " + victim.getName() + "'s GM status!");
                victim.saveToDB(false, false);
            } else {
                // c.getPlayer().ban("Trying to DE-GM an admin", true, true);
            }
            return 1;
        }
    }

    public static class SuperSecretThing extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (c.getPlayer().getGMLevel() >= 8) {
                MapleInventory equipped = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED);
                MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
                for (Item ite : equipped.newList()) {
                    Equip item = (Equip) ite;
                    item.setPotential1(60002);
                    item.setPotential2(60002);
                    item.setPotential3(60002);
                }
                c.getPlayer().fakeRelog();
            }


            return 1;
        }
    }

    public static class giveDGM extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getAccID() == 1) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                victim.setgmLevel((byte) 6);
                victim.setDGM(1);
                victim.dropMessage(5, "You're now a DGM!");
                c.getPlayer().dropMessage(5, "You have made " + victim.getName() + " a DGM!");
                victim.saveToDB(false, false);

            } else {
                //c.getPlayer().ban("Trying to use owner only command on a player", true, true);
            }
            return 1;
        }
    }

    public static class giveGM extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getAccID() == 1) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                victim.setgmLevel((byte) 6);
                victim.dropMessage(5, "You're now a GM!");
                c.getPlayer().dropMessage(5, "You have made " + victim.getName() + " a GM!");
                victim.saveToDB(false, false);

            } else {
                //    c.getPlayer().ban("Trying to use owner only command on a player", true, true);
            }
            return 1;
        }
    }

    public static class giveSDONOR extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getAccID() == 1) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                victim.setgmLevel((byte) 3);
                victim.dropMessage(5, "You're now a Super Donor!");
                c.getPlayer().dropMessage(5, "You have made " + victim.getName() + " a Super Donor!");
                victim.saveToDB(false, false);

            } else {
                // c.getPlayer().ban("Trying to use owner only command on a player", true, true);
            }
            return 1;
        }
    }

    public static class giveDONOR extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getAccID() == 1) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                victim.setgmLevel((byte) 2);
                victim.dropMessage(5, "You're now a Donor!");
                c.getPlayer().dropMessage(5, "You have made " + victim.getName() + " a Donor!");
                victim.saveToDB(false, false);

            } else {
                //    c.getPlayer().ban("Trying to use owner only command on a player", true, true);
            }
            return 1;
        }
    }

    public static class GiveMSI extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getAccID() == 1) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                victim.setMSIPoints(Integer.parseInt(splitted[2]));
                victim.dropMessage(5, "You have gained " + splitted[2] + " MSI points");
                c.getPlayer().dropMessage(5, "You have given " + splitted[2] + " MSI points to " + victim.getName());
                victim.saveToDB(false, false);

            } else {
                //      c.getPlayer().ban("Trying to use owner only command on a player", true, true);
            }
            return 1;
        }
    }

    public static class givebucks extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getAccID() == 1) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                victim.setPoints2(Integer.parseInt(splitted[2]));
                victim.dropMessage(5, "You have gained " + splitted[2] + " Donator bucks");
                c.getPlayer().dropMessage(5, "You have given " + splitted[2] + " Donator bucks to " + victim.getName());
                victim.saveToDB(false, false);

            } else {
                //      c.getPlayer().ban("Trying to use owner only command on a player", true, true);
            }
            return 1;
        }
    }

    public static class GainMeso extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().gainMeso(Integer.MAX_VALUE - c.getPlayer().getMeso(), true);
            return 1;
        }
    }
}
/*
 public static class MesoEveryone extends CommandExecute {

 @Override
 public int execute(MapleClient c, String[] splitted) {
 for (ChannelServer cserv : ChannelServer.getAllInstances()) {
 for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
 mch.gainMeso(Integer.parseInt(splitted[1]), true);
 }
 }
 return 1;
 }
 }

 public static class ExpRate extends CommandExecute {

 @Override
 public int execute(MapleClient c, String[] splitted) {
 if (splitted.length > 1) {
 final int rate = Integer.parseInt(splitted[1]);
 if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
 for (ChannelServer cserv : ChannelServer.getAllInstances()) {
 cserv.setExpRate(rate);
 }
 } else {
 c.getChannelServer().setExpRate(rate);
 }
 c.getPlayer().dropMessage(6, "Exprate has been changed to " + rate + "x");
 } else {
 c.getPlayer().dropMessage(6, "Syntax: !exprate <number> [all]");
 }
 return 1;
 }
 }

 public static class MesoRate extends CommandExecute {

 @Override
 public int execute(MapleClient c, String[] splitted) {
 if (splitted.length > 1) {
 final int rate = Integer.parseInt(splitted[1]);
 if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
 for (ChannelServer cserv : ChannelServer.getAllInstances()) {
 cserv.setMesoRate(rate);
 }
 } else {
 c.getChannelServer().setMesoRate(rate);
 }
 c.getPlayer().dropMessage(6, "Meso Rate has been changed to " + rate + "x");
 } else {
 c.getPlayer().dropMessage(6, "Syntax: !mesorate <number> [all]");
 }
 return 1;
 }
 }

 public static class DCAll extends CommandExecute {

 @Override
 public int execute(MapleClient c, String[] splitted) {
 int range = -1;
 switch (splitted[1]) {
 case "m":
 range = 0;
 break;
 case "c":
 range = 1;
 break;
 case "w":
 range = 2;
 break;
 }
 if (range == -1) {
 range = 1;
 }
 if (range == 0) {
 c.getPlayer().getMap().disconnectAll();
 } else if (range == 1) {
 c.getChannelServer().getPlayerStorage().disconnectAll(true);
 } else if (range == 2) {
 for (ChannelServer cserv : ChannelServer.getAllInstances()) {
 cserv.getPlayerStorage().disconnectAll(true);
 }
 }
 return 1;
 }
 }

 public static class Shutdown extends CommandExecute {

 protected static Thread t = null;

 @Override
 public int execute(MapleClient c, String[] splitted) {
 c.getPlayer().dropMessage(6, "Shutting down...");
 if (t == null || !t.isAlive()) {
 t = new Thread(ShutdownServer.getInstance());
 ShutdownServer.getInstance().shutdown();
 t.start();
 } else {
 c.getPlayer().dropMessage(6, "A shutdown thread is already in progress or shutdown has not been done. Please wait.");
 }
 return 1;
 }
 }

 public static class ShutdownTime extends Shutdown {

 private static ScheduledFuture<?> ts = null;
 private int minutesLeft = 0;

 @Override
 public int execute(MapleClient c, String[] splitted) {
 minutesLeft = Integer.parseInt(splitted[1]);
 c.getPlayer().dropMessage(6, "Shutting down... in " + minutesLeft + " minutes");
 if (ts == null && (t == null || !t.isAlive())) {
 t = new Thread(ShutdownServer.getInstance());
 ts = EventTimer.getInstance().register(new Runnable() {

 @Override
 public void run() {
 if (minutesLeft == 0) {
 ShutdownServer.getInstance().shutdown();
 t.start();
 ts.cancel(false);
 return;
 }
 World.Broadcast.broadcastMessage(CWvsContext.serverNotice(0, "The server will shutdown in " + minutesLeft + " minutes. Please log off safely."));
 minutesLeft--;
 }
 }, 60000);
 } else {
 c.getPlayer().dropMessage(6, "A shutdown thread is already in progress or shutdown has not been done. Please wait.");
 }
 return 1;
 }
 }

 public static class StartProfiling extends CommandExecute {

 @Override
 public int execute(MapleClient c, String[] splitted) {
 CPUSampler sampler = CPUSampler.getInstance();
 sampler.addIncluded("client");
 sampler.addIncluded("constants"); //or should we do Packages.constants etc.?
 sampler.addIncluded("database");
 sampler.addIncluded("handling");
 sampler.addIncluded("provider");
 sampler.addIncluded("scripting");
 sampler.addIncluded("server");
 sampler.addIncluded("tools");
 sampler.start();
 return 1;
 }
 }

 public static class StopProfiling extends CommandExecute {

 @Override
 public int execute(MapleClient c, String[] splitted) {
 CPUSampler sampler = CPUSampler.getInstance();
 try {
 String filename = "odinprofile.txt";
 if (splitted.length > 1) {
 filename = splitted[1];
 }
 File file = new File(filename);
 if (file.exists()) {
 c.getPlayer().dropMessage(6, "The entered filename already exists, choose a different one");
 return 0;
 }
 sampler.stop();
 try (FileWriter fw = new FileWriter(file)) {
 sampler.save(fw, 1, 10);
 }
 } catch (IOException e) {
 System.err.println("Error saving profile" + e);
 }
 sampler.reset();
 return 1;
 } * 
 */
