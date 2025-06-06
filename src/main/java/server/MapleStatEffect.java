package server;

import client.MapleTrait.MapleTraitType;
import client.*;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

import provider.MapleData;
import provider.MapleDataTool;
import provider.MapleDataType;
import server.MapleCarnivalFactory.MCSkill;
import server.Timer.BuffTimer;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkillFactory;
import server.maps.*;
import server.quest.MapleQuest;
import tools.CaltechEval;
import tools.FileoutputUtil;
import tools.Pair;
import tools.Triple;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuffPacket;

public class MapleStatEffect implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private byte mastery, mhpR, mmpR, mobCount, attackCount, bulletCount, reqGuildLevel, period, expR, familiarTarget, iceGageCon, //1 = party 2 = nearby
            recipeUseCount, recipeValidDay, reqSkillLevel, slotCount, effectedOnAlly, effectedOnEnemy, type, preventslip, immortal, bs;
    private short hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, psdSpeed, psdJump, mpCon, hpCon, forceCon, bdR, damage, prop,
            ehp, emp, ewatk, ewdef, emdef, ignoreMob, dot, dotTime, criticaldamageMin, criticaldamageMax, pddR, mddR,
            asrR, terR, er, damR, padX, madX, mesoR, thaw, selfDestruction, PVPdamage, indiePad, indieMad, fatigueChange,
            str, dex, int_, luk, strX, dexX, intX, lukX, lifeId, imhp, immp, inflation, useLevel, mpConReduce,
            indieMhp, indieMmp, indieAllStat, indieSpeed, indieJump, indieAcc, indieEva, indiePdd, indieMdd, incPVPdamage, indieMhpR, indieMmpR,
            mobSkill, mobSkillLevel; //ar = accuracy rate
    private double hpR, mpR;
    private Map<MapleTraitType, Integer> traits;
    private int duration, subTime, sourceid, recipe, moveTo, t, u, v, w, x, y, z, cr, itemCon, itemConNo, bulletConsume, moneyCon,
            cooldown, morphId = 0, expinc, exp, consumeOnPickup, range, price, extendPrice, charColor, interval, rewardMeso, totalprob, cosmetic;
    private boolean overTime, skill, partyBuff = true;
    private EnumMap<MapleBuffStat, Integer> statups;
    private ArrayList<Pair<Integer, Integer>> availableMap;
    private EnumMap<MonsterStatus, Integer> monsterStatus;
    private Point lt, rb;
    private int expBuff, itemup, mesoup, cashup, berserk, illusion, booster, berserk2, cp, nuffSkill;
    private byte level;
    //    private List<Pair<Integer, Integer>> randomMorph;
    private List<MapleDisease> cureDebuffs;
    private List<Integer> petsCanConsume, familiars, randomPickup;
    private List<Triple<Integer, Integer, Integer>> rewardItem;

    public static MapleStatEffect loadSkillEffectFromData(final MapleData source, final int skillid, final boolean overtime, final int level, final String variables) {
        return loadFromData(source, skillid, true, overtime, level, variables);
    }

    public static MapleStatEffect loadItemEffectFromData(final MapleData source, final int itemid) {
        return loadFromData(source, itemid, false, false, 1, null);
    }

    private static void addBuffStatPairToListIfNotZero(final EnumMap<MapleBuffStat, Integer> list, final MapleBuffStat buffstat, final Integer val) {
        if (val.intValue() != 0) {
            list.put(buffstat, val);
        }
    }

    private static int parseEval(String path, MapleData source, int def, String variables, int level) {
        if (variables == null) {
            return MapleDataTool.getIntConvert(path, source, def);
        } else {
            final MapleData dd = source.getChildByPath(path);
            if (dd == null) {
                return def;
            }
            if (dd.getType() != MapleDataType.STRING) {
                return MapleDataTool.getIntConvert(path, source, def);
            }
            String dddd = MapleDataTool.getString(dd).replace(variables, String.valueOf(level));
            switch (dddd.substring(0, 1)) {
                case "-":
                    //-30+3*x
                    if (dddd.substring(1, 2).equals("u") || dddd.substring(1, 2).equals("d")) { //-u(x/2)
                        dddd = "n(" + dddd.substring(1, dddd.length()) + ")"; //n(u(x/2))
                    } else {
                        dddd = "n" + dddd.substring(1, dddd.length()); //n30+3*x
                    }
                    break;
                case "=":
                    //lol nexon and their mistakes
                    dddd = dddd.substring(1, dddd.length());
                    break;
            }
            return (int) (new CaltechEval(dddd).evaluate());
        }
    }

    private static MapleStatEffect loadFromData(final MapleData source, final int sourceid, final boolean skill, final boolean overTime, final int level, final String variables) {
        final MapleStatEffect ret = new MapleStatEffect();
        ret.sourceid = sourceid;
        ret.skill = skill;
        ret.level = (byte) level;
        if (source == null) {
            return ret;
        }
        ret.duration = parseEval("time", source, -1, variables, level);
        ret.subTime = parseEval("subTime", source, -1, variables, level); // used for debuff time..
        ret.hp = (short) parseEval("hp", source, 0, variables, level);
        ret.hpR = parseEval("hpR", source, 0, variables, level) / 100.0;
        ret.mp = (short) parseEval("mp", source, 0, variables, level);
        ret.mpR = parseEval("mpR", source, 0, variables, level) / 100.0;
        ret.mhpR = (byte) parseEval("mhpR", source, 0, variables, level);
        ret.mmpR = (byte) parseEval("mmpR", source, 0, variables, level);
        ret.pddR = (short) parseEval("pddR", source, 0, variables, level);
        ret.mddR = (short) parseEval("mddR", source, 0, variables, level);
        ret.ignoreMob = (short) parseEval("ignoreMobpdpR", source, 0, variables, level);
        ret.asrR = (short) parseEval("asrR", source, 0, variables, level);
        ret.terR = (short) parseEval("terR", source, 0, variables, level);
        ret.bdR = (short) parseEval("bdR", source, 0, variables, level);
        ret.damR = (short) parseEval("damR", source, 0, variables, level);
        ret.mesoR = (short) parseEval("mesoR", source, 0, variables, level);
        ret.thaw = (short) parseEval("thaw", source, 0, variables, level);
        ret.padX = (short) parseEval("padX", source, 0, variables, level);
        ret.madX = (short) parseEval("madX", source, 0, variables, level);
        ret.dot = (short) parseEval("dot", source, 0, variables, level);
        ret.dotTime = (short) parseEval("dotTime", source, 0, variables, level);
        ret.criticaldamageMin = (short) parseEval("criticaldamageMin", source, 0, variables, level);
        ret.criticaldamageMax = (short) parseEval("criticaldamageMax", source, 0, variables, level);
        ret.mpConReduce = (short) parseEval("mpConReduce", source, 0, variables, level);
        ret.forceCon = (short) parseEval("forceCon", source, 0, variables, level);
        ret.mpCon = (short) parseEval("mpCon", source, 0, variables, level);
        ret.hpCon = (short) parseEval("hpCon", source, 0, variables, level);
        ret.prop = (short) parseEval("prop", source, 100, variables, level);
        ret.cooldown = Math.max(0, parseEval("cooltime", source, 0, variables, level));
        ret.interval = parseEval("interval", source, 0, variables, level);
        ret.expinc = parseEval("expinc", source, 0, variables, level);
        ret.exp = parseEval("exp", source, 0, variables, level);
        ret.range = parseEval("range", source, 0, variables, level);
        ret.morphId = parseEval("morph", source, 0, variables, level);
        ret.cp = parseEval("cp", source, 0, variables, level);
        ret.cosmetic = parseEval("cosmetic", source, 0, variables, level);
        ret.er = (short) parseEval("er", source, 0, variables, level);
        ret.slotCount = (byte) parseEval("slotCount", source, 0, variables, level);
        ret.preventslip = (byte) parseEval("preventslip", source, 0, variables, level);
        ret.useLevel = (short) parseEval("useLevel", source, 0, variables, level);
        ret.nuffSkill = parseEval("nuffSkill", source, 0, variables, level);
        ret.familiarTarget = (byte) (parseEval("familiarPassiveSkillTarget", source, 0, variables, level) + 1);
        ret.mobCount = (byte) parseEval("mobCount", source, 1, variables, level);
        ret.immortal = (byte) parseEval("immortal", source, 0, variables, level);
        ret.iceGageCon = (byte) parseEval("iceGageCon", source, 0, variables, level);
        ret.expR = (byte) parseEval("expR", source, 0, variables, level);
        ret.reqGuildLevel = (byte) parseEval("reqGuildLevel", source, 0, variables, level);
        ret.period = (byte) parseEval("period", source, 0, variables, level);
        ret.type = (byte) parseEval("type", source, 0, variables, level);
        ret.bs = (byte) parseEval("bs", source, 0, variables, level);
        ret.attackCount = (byte) parseEval("attackCount", source, 1, variables, level);
        ret.bulletCount = (byte) parseEval("bulletCount", source, 1, variables, level);
        int priceUnit = parseEval("priceUnit", source, 0, variables, level);
        if (priceUnit > 0) {
            ret.price = parseEval("price", source, 0, variables, level) * priceUnit;
            ret.extendPrice = parseEval("extendPrice", source, 0, variables, level) * priceUnit;
        } else {
            ret.price = 0;
            ret.extendPrice = 0;
        }

        if (ret.skill) {
            switch (sourceid) {
                case 35111004: //合金盔甲: 重機槍
                case 35121005: //合金盔甲: 導彈罐
                case 35121013: //合金盔甲: 重機槍
                    ret.attackCount = 6;
                    ret.bulletCount = 6;
                    break;
            }
            if (GameConstants.isNoDelaySkill(sourceid)) {
                ret.mobCount = 6;
            }
        }

        //BUFF持續時間設定
        if (!ret.skill && ret.duration > -1) {
            ret.overTime = true;
        } else {
            ret.duration *= 1000; // items have their times stored in ms, of course
            ret.overTime = overTime || ret.isMorph() || ret.isPirateMorph() || ret.isAngel();
        }
        if (!ret.overTime && !ret.skill && sourceid / 10000 == 286 && ret.interval <= 0) { //怪物夥伴BUFF相關
            ret.overTime = true;
        }
        ret.statups = new EnumMap<>(MapleBuffStat.class);
        ret.mastery = (byte) parseEval("mastery", source, 0, variables, level);
        ret.watk = (short) parseEval("pad", source, 0, variables, level);
        ret.wdef = (short) parseEval("pdd", source, 0, variables, level);
        ret.matk = (short) parseEval("mad", source, 0, variables, level);
        ret.mdef = (short) parseEval("mdd", source, 0, variables, level);
        ret.ehp = (short) parseEval("emhp", source, 0, variables, level);
        ret.emp = (short) parseEval("emmp", source, 0, variables, level);
        ret.ewatk = (short) parseEval("epad", source, 0, variables, level);
        ret.ewdef = (short) parseEval("epdd", source, 0, variables, level);
        ret.emdef = (short) parseEval("emdd", source, 0, variables, level);
        ret.acc = (short) parseEval("acc", source, 0, variables, level);
        ret.avoid = (short) parseEval("eva", source, 0, variables, level);
        ret.speed = (short) parseEval("speed", source, 0, variables, level);
        ret.jump = (short) parseEval("jump", source, 0, variables, level);
        ret.psdSpeed = (short) parseEval("psdSpeed", source, 0, variables, level);
        ret.psdJump = (short) parseEval("psdJump", source, 0, variables, level);
        ret.indiePad = (short) parseEval("indiePad", source, 0, variables, level);
        ret.indieMad = (short) parseEval("indieMad", source, 0, variables, level);
        ret.indieMhp = (short) parseEval("indieMhp", source, 0, variables, level);
        ret.indieMmp = (short) parseEval("indieMmp", source, 0, variables, level);
        ret.indieSpeed = (short) parseEval("indieSpeed", source, 0, variables, level);
        ret.indieJump = (short) parseEval("indieJump", source, 0, variables, level);
        ret.indieAcc = (short) parseEval("indieAcc", source, 0, variables, level);
        ret.indieEva = (short) parseEval("indieEva", source, 0, variables, level);
        ret.indiePdd = (short) parseEval("indiePdd", source, 0, variables, level);
        ret.indieMdd = (short) parseEval("indieMdd", source, 0, variables, level);
        ret.indieAllStat = (short) parseEval("indieAllStat", source, 0, variables, level);
        ret.indieMhpR = (short) parseEval("indieMhpR", source, 0, variables, level);
        ret.indieMmpR = (short) parseEval("indieMmpR", source, 0, variables, level);
        ret.str = (short) parseEval("str", source, 0, variables, level);
        ret.dex = (short) parseEval("dex", source, 0, variables, level);
        ret.int_ = (short) parseEval("int", source, 0, variables, level);
        ret.luk = (short) parseEval("luk", source, 0, variables, level);
        ret.strX = (short) parseEval("strX", source, 0, variables, level);
        ret.dexX = (short) parseEval("dexX", source, 0, variables, level);
        ret.intX = (short) parseEval("intX", source, 0, variables, level);
        ret.lukX = (short) parseEval("lukX", source, 0, variables, level);
        ret.expBuff = parseEval("expBuff", source, 0, variables, level);
        ret.cashup = parseEval("cashBuff", source, 0, variables, level);
        ret.itemup = parseEval("itemupbyitem", source, 0, variables, level);
        ret.mesoup = parseEval("mesoupbyitem", source, 0, variables, level);
        ret.berserk = parseEval("berserk", source, 0, variables, level);
        ret.berserk2 = parseEval("berserk2", source, 0, variables, level);
        ret.booster = parseEval("booster", source, 0, variables, level);
        ret.lifeId = (short) parseEval("lifeId", source, 0, variables, level);
        ret.inflation = (short) parseEval("inflation", source, 0, variables, level);
        ret.imhp = (short) parseEval("imhp", source, 0, variables, level);
        ret.immp = (short) parseEval("immp", source, 0, variables, level);
        ret.illusion = parseEval("illusion", source, 0, variables, level);
        ret.consumeOnPickup = parseEval("consumeOnPickup", source, 0, variables, level);
        if (ret.consumeOnPickup == 1) {
            if (parseEval("party", source, 0, variables, level) > 0) {
                ret.consumeOnPickup = 2;
            }
        }
        ret.charColor = 0;
        String cColor = MapleDataTool.getString("charColor", source, null);
        if (cColor != null) {

            try {
                ret.charColor |= Integer.parseInt("0x" + cColor.substring(0, 2));
                ret.charColor |= Integer.parseInt("0x" + cColor.substring(2, 4) + "00");
                ret.charColor |= Integer.parseInt("0x" + cColor.substring(4, 6) + "0000");
                ret.charColor |= Integer.parseInt("0x" + cColor.substring(6, 8) + "000000");

            } catch (NumberFormatException e) {
                //  System.out.println("This is not a number");
                //System.out.println(e.getMessage());


            }
        }
        ret.traits = new EnumMap<>(MapleTraitType.class);
        for (MapleTraitType t : MapleTraitType.values()) {
            int expz = parseEval(t.name() + "EXP", source, 0, variables, level);
            if (expz != 0) {
                ret.traits.put(t, expz);
            }
        }

        ret.recipe = parseEval("recipe", source, 0, variables, level);
        ret.recipeUseCount = (byte) parseEval("recipeUseCount", source, 0, variables, level);
        ret.recipeValidDay = (byte) parseEval("recipeValidDay", source, 0, variables, level);
        ret.reqSkillLevel = (byte) parseEval("reqSkillLevel", source, 0, variables, level);

        ret.effectedOnAlly = (byte) parseEval("effectedOnAlly", source, 0, variables, level);
        ret.effectedOnEnemy = (byte) parseEval("effectedOnEnemy", source, 0, variables, level);

        List<MapleDisease> cure = new ArrayList<>(5);
        if (parseEval("poison", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.POISON);
        }
        if (parseEval("seal", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.SEAL);
        }
        if (parseEval("darkness", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.DARKNESS);
        }
        if (parseEval("weakness", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.WEAKEN);
        }
        if (parseEval("curse", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.CURSE);
        }
        ret.cureDebuffs = cure;

        ret.petsCanConsume = new ArrayList<>();
        for (int i = 0; true; i++) {
            final int dd = parseEval(String.valueOf(i), source, 0, variables, level);
            if (dd > 0) {
                ret.petsCanConsume.add(dd);
            } else {
                break;
            }
        }
        final MapleData mdd = source.getChildByPath("0");
        if (mdd != null && mdd.getChildren().size() > 0) {
            ret.mobSkill = (short) parseEval("mobSkill", mdd, 0, variables, level);
            ret.mobSkillLevel = (short) parseEval("level", mdd, 0, variables, level);
        } else {
            ret.mobSkill = 0;
            ret.mobSkillLevel = 0;
        }
        final MapleData pd = source.getChildByPath("randomPickup");
        if (pd != null) {
            ret.randomPickup = new ArrayList<>();
            for (MapleData p : pd) {
                ret.randomPickup.add(MapleDataTool.getInt(p));
            }
        }
        final MapleData ltd = source.getChildByPath("lt");
        if (ltd != null) {
            ret.lt = (Point) ltd.getData();
            ret.rb = (Point) source.getChildByPath("rb").getData();
        }

        final MapleData ltc = source.getChildByPath("con");
        if (ltc != null) {
            ret.availableMap = new ArrayList<>();
            for (MapleData ltb : ltc) {
                ret.availableMap.add(new Pair<>(MapleDataTool.getInt("sMap", ltb, 0), MapleDataTool.getInt("eMap", ltb, 999999999)));
            }
        }
        final MapleData ltb = source.getChildByPath("familiar");
        if (ltb != null) {
            ret.fatigueChange = (short) (parseEval("incFatigue", ltb, 0, variables, level) - parseEval("decFatigue", ltb, 0, variables, level));
            ret.familiarTarget = (byte) parseEval("target", ltb, 0, variables, level);
            final MapleData lta = ltb.getChildByPath("targetList");
            if (lta != null) {
                ret.familiars = new ArrayList<>();
                for (MapleData ltz : lta) {
                    ret.familiars.add(MapleDataTool.getInt(ltz, 0));
                }
            }
        } else {
            ret.fatigueChange = 0;
        }
        int totalprob = 0;
        final MapleData lta = source.getChildByPath("reward");
        if (lta != null) {
            ret.rewardMeso = parseEval("meso", lta, 0, variables, level);
            final MapleData ltz = lta.getChildByPath("case");
            if (ltz != null) {
                ret.rewardItem = new ArrayList<>();
                for (MapleData lty : ltz) {
                    ret.rewardItem.add(new Triple<>(MapleDataTool.getInt("id", lty, 0), MapleDataTool.getInt("count", lty, 0), MapleDataTool.getInt("prop", lty, 0)));
                    totalprob += MapleDataTool.getInt("prob", lty, 0);
                }
            }
        } else {
            ret.rewardMeso = 0;
        }
        ret.totalprob = totalprob;
        ret.cr = parseEval("cr", source, 0, variables, level);
        ret.t = parseEval("t", source, 0, variables, level);
        ret.u = parseEval("u", source, 0, variables, level);
        ret.v = parseEval("v", source, 0, variables, level);
        ret.w = parseEval("w", source, 0, variables, level);
        ret.x = parseEval("x", source, 0, variables, level);
        ret.y = parseEval("y", source, 0, variables, level);
        ret.z = parseEval("z", source, 0, variables, level);
        ret.damage = (short) parseEval("damage", source, 100, variables, level);
        ret.PVPdamage = (short) parseEval("PVPdamage", source, 0, variables, level);
        ret.incPVPdamage = (short) parseEval("incPVPDamage", source, 0, variables, level);
        ret.selfDestruction = (short) parseEval("selfDestruction", source, 0, variables, level);
        ret.bulletConsume = parseEval("bulletConsume", source, 0, variables, level);
        ret.moneyCon = parseEval("moneyCon", source, 0, variables, level);

        ret.itemCon = parseEval("itemCon", source, 0, variables, level);
        ret.itemConNo = parseEval("itemConNo", source, 0, variables, level);
        ret.moveTo = parseEval("moveTo", source, -1, variables, level);
        ret.monsterStatus = new EnumMap<>(MonsterStatus.class);
        if (ret.overTime && ret.getSummonMovementType() == null && !ret.isEnergyCharge()) {
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.WATK, Integer.valueOf(ret.watk));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.WDEF, Integer.valueOf(ret.wdef));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MATK, Integer.valueOf(ret.matk));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MDEF, Integer.valueOf(ret.mdef));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ACC, Integer.valueOf(ret.acc));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.AVOID, Integer.valueOf(ret.avoid));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.SPEED, Integer.valueOf(ret.speed));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.JUMP, Integer.valueOf(ret.jump));
            if (sourceid != 22131001) {//守護之力是被動新增HP上限
                addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MAXHP, (int) ret.mhpR);
            }
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MAXMP, (int) ret.mmpR);
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.BOOSTER, Integer.valueOf(ret.booster));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.HP_LOSS_GUARD, Integer.valueOf(ret.thaw));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.EXPRATE, Integer.valueOf(ret.expBuff)); // EXP
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ACASH_RATE, Integer.valueOf(ret.cashup)); // custom
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.DROP_RATE, Integer.valueOf(GameConstants.getModifier(ret.sourceid, ret.itemup))); // defaults to 2x
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MESO_RATE, Integer.valueOf(GameConstants.getModifier(ret.sourceid, ret.mesoup))); // defaults to 2x
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.BERSERK_FURY, Integer.valueOf(ret.berserk2));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ILLUSION, Integer.valueOf(ret.illusion));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.PYRAMID_PQ, Integer.valueOf(ret.berserk));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ENHANCED_MAXHP, Integer.valueOf(ret.ehp));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ENHANCED_MAXMP, Integer.valueOf(ret.emp));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ENHANCED_WATK, Integer.valueOf(ret.ewatk));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ENHANCED_WDEF, Integer.valueOf(ret.ewdef));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ENHANCED_MDEF, Integer.valueOf(ret.emdef));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.GIANT_POTION, Integer.valueOf(ret.inflation));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.STR, Integer.valueOf(ret.str));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.DEX, Integer.valueOf(ret.dex));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.INT, Integer.valueOf(ret.int_));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.LUK, Integer.valueOf(ret.luk));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ANGEL_ATK, Integer.valueOf(ret.indiePad));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ANGEL_MATK, Integer.valueOf(ret.indieMad));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.HP_BOOST, Integer.valueOf(ret.imhp));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MP_BOOST, Integer.valueOf(ret.immp)); //same one? lol
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.HP_BOOST_PERCENT, Integer.valueOf(ret.indieMhpR));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MP_BOOST_PERCENT, Integer.valueOf(ret.indieMmpR));
            addBuffStatPairToListIfNotZero(ret.statups, sourceid == 32111012 ? MapleBuffStat.ENHANCED_MAXHP : MapleBuffStat.HP_BOOST, Integer.valueOf(ret.indieMhp));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MP_BOOST, Integer.valueOf(ret.indieMmp));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.WDEF, Integer.valueOf(ret.indiePdd)); //普通藍色繩索相關
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MDEF, Integer.valueOf(ret.indieMdd)); //普通藍色繩索相關
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.PVP_DAMAGE, Integer.valueOf(ret.incPVPdamage));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ANGEL_JUMP, Integer.valueOf(ret.indieJump));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ANGEL_SPEED, Integer.valueOf(ret.indieSpeed));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ANGEL_ACC, Integer.valueOf(ret.indieAcc));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ANGEL_AVOID, Integer.valueOf(ret.indieEva));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ANGEL_STAT, Integer.valueOf(ret.indieAllStat));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.PVP_ATTACK, Integer.valueOf(ret.PVPdamage));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.INVINCIBILITY, Integer.valueOf(ret.immortal));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.NO_SLIP, Integer.valueOf(ret.preventslip));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.FAMILIAR_SHADOW, Integer.valueOf(ret.charColor > 0 ? 1 : 0));
            if (ret.isPirateMorph()) { //HACK: add stance :D and also this buffstat has to be the first one..
                ret.statups.put(MapleBuffStat.STANCE, 100); //100% :D:D:D
            }
        }
        if (ret.overTime && !ret.skill && sourceid / 10000 == 286 && ret.interval <= 0) { //怪物夥伴BUFF相關
            if (ret.statups.isEmpty()) {
                ret.overTime = false;
            } else {
                ret.duration = (Integer.MAX_VALUE);
                //ret.info.put(MapleStatInfo.time, Integer.MAX_VALUE);
            }
        }
        if (ret.skill) { // hack because we can't get from the datafile...
            switch (sourceid) {
                case 1101004: //極速武器
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 1101007: //反射之盾
                    ret.statups.put(MapleBuffStat.POWERGUARD, ret.x);
                    break;
                case 1111002: //鬥氣集中
                    ret.statups.put(MapleBuffStat.COMBO, 1);
                    break;
                case 1111003: //黑暗之劍
                    ret.monsterStatus.put(MonsterStatus.DARKNESS, ret.x);
                    break;
                case 1111005: //昏迷之劍
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 1111007: //防禦消除
                    ret.monsterStatus.put(MonsterStatus.MAGIC_CRASH, Integer.valueOf(1));
                    break;
                case 1111008: //虎咆哮
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 1121000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 1121001: //絕對引力
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 1121002: //格擋
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    break;
                case 1121010: // 鬥氣爆發
                    ret.statups.put(MapleBuffStat.ENRAGE, ret.x * 100 + ret.mobCount);
                    ret.statups.put(MapleBuffStat.CRITICAL_INC, ret.z);
                    break;
                case 1201004: //極速武器
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 1201006: //降魔咒
                    ret.monsterStatus.put(MonsterStatus.WATK, ret.x);
                    ret.monsterStatus.put(MonsterStatus.WDEF, ret.x);
                    ret.monsterStatus.put(MonsterStatus.DARKNESS, ret.z);
                    break;
                case 1201007: //反射之盾
                    ret.statups.put(MapleBuffStat.POWERGUARD, ret.x);
                    break;
                case 1211004: //烈焰之劍
                    ret.statups.put(MapleBuffStat.WK_CHARGE, ret.x);
                    break;
                case 1211006: //寒冰之劍
                    ret.statups.put(MapleBuffStat.WK_CHARGE, ret.x);
                    break;
                case 1211008: //雷鳴之劍
                    ret.statups.put(MapleBuffStat.WK_CHARGE, ret.x);
                    break;
                case 1211009: //魔防消除
                    ret.monsterStatus.put(MonsterStatus.MAGIC_CRASH, Integer.valueOf(1));
                    break;
                case 1211010: //復原
                    ret.hpR = ret.x / 100.0;
                    break;
                case 1211011: //戰鬥命令
                    ret.statups.put(MapleBuffStat.COMBAT_ORDERS, ret.x);
                    break;
                case 1220013: //祝福護甲
                    ret.statups.put(MapleBuffStat.DIVINE_SHIELD, ret.x + 1);
                    break;
                case 1221000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 1221002: //格擋
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    break;
                case 1221004: //聖靈之劍
                    ret.statups.put(MapleBuffStat.WK_CHARGE, ret.x);
                    break;
                case 1301004: //極速武器
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 1301007: //神聖之火
                    ret.statups.put(MapleBuffStat.MAXHP, ret.x);
                    ret.statups.put(MapleBuffStat.MAXMP, ret.x);
                    break;
                case 1311005: //龍之獻祭
                    ret.hpR = -ret.x / 100.0;
                    break;
                case 1311006: //龍咆哮
                    ret.hpR = -ret.x / 100.0;
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 1311007: //魔防消除
                    ret.monsterStatus.put(MonsterStatus.MAGIC_CRASH, Integer.valueOf(1));
                    break;
                case 1321000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 1321001: //絕對引力
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 1321002: //格擋
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    break;
                case 1321007: //暗之靈魂
                    ret.statups.put(MapleBuffStat.BEHOLDER, (int) ret.level);
                    break;
                case 2000007: //自然力變弱
                    ret.statups.put(MapleBuffStat.ELEMENT_WEAKEN, ret.x);
                    break;
                case 2001002: //魔心防禦
                    ret.statups.put(MapleBuffStat.MAGIC_GUARD, ret.x);
                    break;
                case 2101003: //緩速術
                    ret.monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    break;
                case 2111004: //魔法封印
                    ret.monsterStatus.put(MonsterStatus.SEAL, 1);
                    break;
                case 2111005: //極速詠唱
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 2111007: //瞬間移動精通
                    ret.mpCon = (short) ret.y;
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.TELEPORT_MASTERY, ret.x);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 2111008: //自然力重置
                    ret.statups.put(MapleBuffStat.ELEMENT_RESET, ret.x);
                    break;
                case 2120010: //神秘狙擊
                    ret.duration = 5000;
                    ret.statups.put(MapleBuffStat.ARCANE_AIM, ret.x);
                    break;
                case 2121000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 2121004: //魔力無限
                    ret.hpR = ret.y / 100.0;
                    ret.mpR = ret.y / 100.0;
                    ret.statups.put(MapleBuffStat.INFINITY, ret.x);
                    if (GameConstants.GMS) {
                        ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    }
                    break;
                case 2121005: //召喚火魔
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 2121006: //梅杜莎之眼
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 2121009: //大師魔法
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.BUFF_MASTERY, ret.x);
                    break;
                case 2201003: //緩速術
                    ret.monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    break;
                case 2201004: //冰錐術
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 2211002: //冰風暴
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 2211003: //落雷凝聚
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 2211004: //魔法封印
                    ret.monsterStatus.put(MonsterStatus.SEAL, 1);
                    break;
                case 2211005: //極速詠唱
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 2211006: //寒冰地獄
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 2211007: //瞬間移動精通
                    ret.mpCon = (short) ret.y;
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.TELEPORT_MASTERY, ret.x);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 2211008: //自然力重置
                    ret.statups.put(MapleBuffStat.ELEMENT_RESET, ret.x);
                    break;
                case 2220010: //神秘狙擊
                    ret.duration = 5000;
                    ret.statups.put(MapleBuffStat.ARCANE_AIM, ret.x);
                    break;
                case 2221000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 2221001: //核爆術
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 2221003: //崩潰之星
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 2221004: //魔力無限
                    ret.hpR = ret.y / 100.0;
                    ret.mpR = ret.y / 100.0;
                    ret.statups.put(MapleBuffStat.INFINITY, ret.x);
                    if (GameConstants.GMS) { //TODO JUMP
                        ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    }
                    break;
                case 2221005: //召喚冰魔
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.FREEZE, Integer.valueOf(1));
                    break;
                case 2221006: //閃電連擊
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 2221007: //暴風雪
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 2221009: //大師魔法
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.BUFF_MASTERY, ret.x);
                    break;
                case 2301003: //神聖之光
                    ret.statups.put(MapleBuffStat.INVINCIBLE, ret.x);
                    break;
                case 2301004: //天使祝福
                    ret.statups.put(MapleBuffStat.BLESS, (int) ret.level);
                    break;
                case 2311003: //神聖祈禱
                    ret.statups.put(MapleBuffStat.HOLY_SYMBOL, ret.x);
                    break;
                case 2311004: //聖光
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 2311005: //喚化術
                    ret.monsterStatus.put(MonsterStatus.DOOM, 1);
                    break;
                case 2311007: //瞬間移動精通
                    ret.mpCon = (short) ret.y;
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.TELEPORT_MASTERY, ret.x);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 2311009: //聖十字魔法盾
                    ret.statups.put(MapleBuffStat.HOLY_MAGIC_SHELL, ret.x);
                    ret.cooldown = ret.y;
                    ret.hpR = ret.z / 100.0;
                    break;
                case 2320011: //神秘狙擊
                    ret.duration = 5000;
                    ret.statups.put(MapleBuffStat.ARCANE_AIM, ret.x);
                    break;
                case 2321000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 2321003: //聖龍精通
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 2321004: //魔力無限
                    ret.hpR = ret.y / 100.0;
                    ret.mpR = ret.y / 100.0;
                    ret.statups.put(MapleBuffStat.INFINITY, ret.x);
                    if (GameConstants.GMS) { //TODO JUMP
                        ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    }
                    break;
                case 2321005: //進階祝福
                    ret.statups.put(MapleBuffStat.HOLY_SHIELD, (int) ret.level);
                    break;
                case 2321010: //大師魔法
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.BUFF_MASTERY, ret.x);
                    break;
                case 2311002: //時空門
                    ret.statups.put(MapleBuffStat.SOULARROW, ret.x);
                    break;
                case 2311006: //極速詠唱
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 3101002: //快速之弓
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 3101004: //無形之箭
                    ret.statups.put(MapleBuffStat.SOULARROW, ret.x);
                    break;
                case 3101005: //炸彈箭
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 3101007: //銀鷹召喚
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 3111000: //集中
                    ret.statups.put(MapleBuffStat.CONCENTRATE, ret.x);
                    break;
                case 3111002: //替身術
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 3111004: //箭雨
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 3111005: //召喚鳳凰
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 3120006: //鳳凰附體
                    ret.statups.put(MapleBuffStat.SPIRIT_LINK, 1);
                    break;
                case 3120010: //終極火焰衝擊
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 3120012: //精銳替身術
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 3121000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 3121002: //會心之眼
                    ret.statups.put(MapleBuffStat.SHARP_EYES, (ret.x << 8) + ret.criticaldamageMax);
                    break;
                case 3121007: //爆發
                    ret.statups.put(MapleBuffStat.BLIND, (int) ret.dex); //提升敏捷
                    ret.monsterStatus.put(MonsterStatus.ACC, ret.x); //减少怪物命中率
                    break;
                case 3201002: //快速之弩
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 3201004://無形之箭
                    ret.statups.put(MapleBuffStat.SOULARROW, ret.x);
                    break;
                case 3201007: //金鷹召喚
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 3211000: //集中
                    ret.statups.put(MapleBuffStat.CONCENTRATE, ret.x);
                    break;
                case 3211002: //替身術
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 3211003: //暴風雪箭
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 3211004: //升龍弩
                    ret.monsterStatus.put(MonsterStatus.SEAL, 1);
                    break;
                case 3211005: //召喚銀隼
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 3220005: //銀隼附體
                    ret.statups.put(MapleBuffStat.SPIRIT_LINK, 1);
                    break;
                case 3220012: //精銳替身術
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 3221000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 3221002: //會心之眼
                    ret.statups.put(MapleBuffStat.SHARP_EYES, (ret.x << 8) + ret.criticaldamageMax);
                    break;
                case 3221006: //爆發
                    ret.statups.put(MapleBuffStat.BLIND, (int) ret.dex);
                    ret.monsterStatus.put(MonsterStatus.ACC, ret.x);
                    break;
                case 4001003: //隱身術
                    ret.statups.put(MapleBuffStat.DARKSIGHT, ret.x);
                    break;
                case 4101003: //極速暗殺
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 4111002: //影分身
                    ret.statups.put(MapleBuffStat.SHADOWPARTNER, ret.x);
                    break;
                case 4111003: //影網術
                    ret.monsterStatus.put(MonsterStatus.SHADOW_WEB, 1);
                    break;
                case 4111007: //黑暗殺
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 4111009: //無形鏢
                    ret.statups.put(MapleBuffStat.SPIRIT_CLAW, Integer.valueOf(0));
                    break;
                case 4121000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 4121003: //挑釁
                    ret.monsterStatus.put(MonsterStatus.SHOWDOWN, ret.x);
                    ret.monsterStatus.put(MonsterStatus.MDEF, ret.x);
                    ret.monsterStatus.put(MonsterStatus.WDEF, ret.x);
                    break;
                case 4121015: //絕對領域
                    ret.monsterStatus.put(MonsterStatus.SPEED, ret.y);
                    ret.monsterStatus.put(MonsterStatus.WATK, ret.w);
                    ret.monsterStatus.put(MonsterStatus.WDEF, ret.w);
                    break;
                case 4201002: //快速之刀
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 4201011: //楓幣護盾
                    ret.statups.put(MapleBuffStat.MESOGUARD, ret.x);
                    break;
                case 4211003: //勇者掠奪術
                    ret.statups.put(MapleBuffStat.PICKPOCKET, ret.x);
                    break;
                case 4211007: //黑暗殺
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 4211008: //影分身
                    ret.statups.put(MapleBuffStat.SHADOWPARTNER, (int) ret.level);
                    break;
                case 4221000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 4221007: //瞬步連擊
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 4221013: //暗殺本能
                    ret.statups.put(MapleBuffStat.ANGEL_ATK, ret.x);
                    break;
                case 4311009: //神速雙刀
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 4321002: //閃光彈
                    ret.monsterStatus.put(MonsterStatus.DARKNESS, ret.x);
                    break;
                case 4321006: //翔空落葉斬
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 4330001: //進階隱身術
                    ret.statups.put(MapleBuffStat.DARKSIGHT, (int) ret.level);
                    break;
                case 4331002: //替身術
                    ret.statups.put(MapleBuffStat.SHADOWPARTNER, ret.x);
                    break;
                case 4341000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 4341002: //絕殺刃
                    ret.duration = 60 * 1000;
                    ret.hpR = -ret.x / 100.0;
                    ret.statups.put(MapleBuffStat.FINAL_CUT, ret.y);
                    break;
                case 4341006: //幻影替身
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 4341007: //荊棘特效
                    ret.statups.put(MapleBuffStat.ANGEL_ATK, (int) ret.x);
                    break;
                case 5001005: //衝鋒
                    ret.statups.put(MapleBuffStat.DASH_SPEED, ret.x);
                    ret.statups.put(MapleBuffStat.DASH_JUMP, ret.y);
                    break;
                case 5101006: //致命快打
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 5110001: //蓄能激發
                    ret.statups.put(MapleBuffStat.ENERGY_CHARGE, 0);
                    break;
                case 5111002: //能量暴擊
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 5111007: //幸運骰子
                    ret.statups.put(MapleBuffStat.DICE_ROLL, 0);
                    break;
                case 5111010: //雲體風身
                    ret.statups.put(MapleBuffStat.WATER_SHIELD, ret.x);
                    break;
                case 5120011: //反擊姿態
                    ret.cooldown = ret.x;
                    ret.statups.put(MapleBuffStat.PIRATES_REVENGE, (int) ret.damR);
                    break;
                case 5120012: //雙倍幸運骰子
                    ret.statups.put(MapleBuffStat.DICE_ROLL, 0);
                    break;
                case 5121000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 5121009: //最終極速
                    ret.statups.put(MapleBuffStat.SPEED_INFUSION, ret.x);
                    break;
                case 5121015: //拳霸大師
                    ret.statups.put(MapleBuffStat.PIRATES_REVENGE, ret.x);
                    break;
                case 5201003: //迅雷再起
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 5201008: //無形彈藥
                    ret.statups.put(MapleBuffStat.SPIRIT_CLAW, Integer.valueOf(0));
                    break;
                case 5211007: //幸運骰子
                    ret.statups.put(MapleBuffStat.DICE_ROLL, 0);
                    break;
                case 5211009: //魔法彈丸
                    ret.statups.put(MapleBuffStat.ANGEL_ATK, ret.y);
                    break;
                case 5211011: //召喚船員
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 5211014: //砲台章魚王
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 5211015: //召喚船員
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 5211016: //召喚船員
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 5220012: //反擊
                    ret.cooldown = ret.x;
                    ret.statups.put(MapleBuffStat.PIRATES_REVENGE, (int) ret.damR);
                    break;
                case 5220014: //雙倍幸運骰子
                    ret.statups.put(MapleBuffStat.DICE_ROLL, 0);
                    break;
                case 5220019: //船員指令
                    ret.statups.clear();
                    ret.statups.put(MapleBuffStat.SPIRIT_LINK, 10);
                    break;
                case 5221000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 5221015: //無盡追擊
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.HOMING_BEACON, ret.x);
                    break;
                case 5221018://海盜風采
                    ret.statups.put(MapleBuffStat.Relentless, (int) ret.damR);
                    ret.statups.put(MapleBuffStat.ABNORMAL_STATUS_R, ret.x);
                    ret.statups.put(MapleBuffStat.ELEMENTAL_STATUS_R, ret.x);
                    ret.statups.put(MapleBuffStat.STANCE, ret.z);
                    break;
                case 5011002: //迴避砲擊
                    ret.monsterStatus.put(MonsterStatus.SPEED, ret.z);
                    break;
                case 5301002: //加農砲推進器
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 5310008:
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 5311001: //猴子的強力炸彈
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 5311002: //猴子的衝擊波
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 5311004://幸運木桶
                    ret.statups.put(MapleBuffStat.BARREL_ROLL, 0);
                    break;
                case 5311005: //幸運骰子
                    ret.statups.put(MapleBuffStat.DICE_ROLL, 0);
                    break;
                case 5320007: //雙倍幸運骰子
                    ret.statups.put(MapleBuffStat.DICE_ROLL, 0);
                    break;
                case 5321003: //磁錨
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 5321004: //雙胞胎猴子
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 5321005: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 5321010: //百烈精神
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.x);
                    break;
                case 5701005: //迅雷再起
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x); //提升攻擊速度
                    break;
                case 5711001: //破城砲
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 5711011:  //幸運骰子
                    ret.statups.put(MapleBuffStat.DICE_ROLL, 0);
                    break;
                case 5720005: //雙倍幸運骰子
                    ret.statups.put(MapleBuffStat.DICE_ROLL, 0);
                    break;
                case 5720012: //反擊
                    ret.cooldown = ret.x;
                    ret.statups.put(MapleBuffStat.PIRATES_REVENGE, (int) ret.damR);
                    break;
                case 5721000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 5721003: //精準砲擊
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.HOMING_BEACON, ret.x);
                    break;
                case 5721009: //海盜風采
                    ret.statups.put(MapleBuffStat.Relentless, (int) ret.damR);
                    ret.statups.put(MapleBuffStat.ABNORMAL_STATUS_R, ret.x);
                    ret.statups.put(MapleBuffStat.ELEMENTAL_STATUS_R, ret.x);
                    ret.statups.put(MapleBuffStat.STANCE, ret.z);
                    break;
                case 10001075: //女皇的祈禱
                    ret.statups.put(MapleBuffStat.ECHO_OF_HERO, ret.x);
                    break;
                case 11001004: //光之精靈
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 11101001: //快速之劍
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 11101002: //終極攻擊
                    ret.statups.put(MapleBuffStat.FINALATTACK, ret.x);
                    break;
                case 11101006: //伤害反射
                    ret.statups.put(MapleBuffStat.POWERGUARD, ret.x);
                    break;
                case 11111001: //鬥氣集中
                    ret.statups.put(MapleBuffStat.COMBO, 1);
                    break;
                case 11111002: //黑暗之劍
                    ret.monsterStatus.put(MonsterStatus.DARKNESS, ret.x);
                    break;
                case 11111003: //昏迷之劍
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 11111007: //閃耀激發
                    ret.statups.put(MapleBuffStat.WK_CHARGE, ret.x);
                    break;
                case 11111008: //魔防消除
                    ret.monsterStatus.put(MonsterStatus.MAGIC_CRASH, Integer.valueOf(1));
                    break;
                case 12000006: //魔法強化
                    ret.statups.put(MapleBuffStat.ELEMENT_WEAKEN, ret.x);
                    break;
                case 12001001: //魔心防禦
                    ret.statups.put(MapleBuffStat.MAGIC_GUARD, ret.x);
                    break;
                case 12001004: //火之精靈
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 12101001: //緩速術
                    ret.monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    break;
                case 12101004: //極速詠唱
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 12101005: //自然力重置
                    ret.statups.put(MapleBuffStat.ELEMENT_RESET, ret.x);
                    break;
                case 12111002: //魔法封印
                    ret.monsterStatus.put(MonsterStatus.SEAL, 1);
                    break;
                case 12111004: //召喚火魔
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 12111007: //瞬間移動精通
                    ret.mpCon = (short) ret.y;
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.TELEPORT_MASTERY, ret.x);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 13001004: //風之精靈
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 13101001: //快速之箭
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 13101002: //終極攻擊
                    ret.statups.put(MapleBuffStat.FINALATTACK1, (int) ret.prop);
                    break;
                case 13101003: //無形之箭
                    ret.statups.put(MapleBuffStat.SOULARROW, ret.x);
                    break;
                case 13101006: //風影漫步
                    ret.statups.put(MapleBuffStat.WIND_WALK, ret.x);
                    break;
                case 13111000: //箭雨
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 13111001: //集中
                    ret.statups.put(MapleBuffStat.CONCENTRATE, ret.x);
                    break;
                case 13111004: //替身術
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 14001005: //暗之精靈
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 14101002: //極速暗殺
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 14001003: //隱身術
                    ret.statups.put(MapleBuffStat.DARKSIGHT, ret.x);
                    break;
                case 14111000: //影分身
                    ret.statups.put(MapleBuffStat.SHADOWPARTNER, ret.x);
                    break;
                case 14111001: //影網術
                    ret.monsterStatus.put(MonsterStatus.SHADOW_WEB, 1);
                    break;
                case 14111007: //無形鏢
                    ret.statups.put(MapleBuffStat.SPIRIT_CLAW, Integer.valueOf(0));
                    break;
                case 14111010: //黑暗殺
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 15001003: //衝鋒
                    ret.statups.put(MapleBuffStat.DASH_SPEED, ret.x);
                    ret.statups.put(MapleBuffStat.DASH_JUMP, ret.y);
                    break;
                case 15001004: //雷之精靈
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 15100004: //蓄能激發
                    ret.statups.put(MapleBuffStat.ENERGY_CHARGE, 0);
                    break;
                case 15101002: //致命快打
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 15101005: //能量暴擊
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 15111005: //最終極速
                    ret.statups.put(MapleBuffStat.SPEED_INFUSION, ret.x);
                    break;
                case 15111006: //閃光擊
                    ret.statups.put(MapleBuffStat.SPARK, ret.x);
                    break;
                case 15111011: //幸運骰子
                    ret.statups.put(MapleBuffStat.DICE_ROLL, 0);
                    break;
                case 21000000: //矛之鬥氣
                    ret.statups.put(MapleBuffStat.ARAN_COMBO, 100);
                    break;
                case 21001003: //神速之矛
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.y); //攻擊速度提升
                    break;
                case 21100002: //突刺之矛
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 21100005: //連環吸血
                    ret.statups.put(MapleBuffStat.COMBO_DRAIN, ret.x);
                    break;
                case 21101003: //強化連擊
                    ret.statups.put(MapleBuffStat.BODY_PRESSURE, ret.x);
                    ret.monsterStatus.put(MonsterStatus.NEUTRALISE, ret.x);
                    break;
                case 21101006: //寒冰屬性
                    ret.statups.put(MapleBuffStat.WK_CHARGE, ret.x);
                    break;
                case 21111001: //釘錘
                    //ret.statups.put(MapleBuffStat.SMART_KNOCKBACK, (int) ret.level);
                    final ArrayList<Pair<MapleBuffStat, Integer>> statups = new ArrayList<>();
                    statups.add(new Pair<>(MapleBuffStat.SMART_KNOCKBACK, ret.x));
                    break;
                case 21110006: //旋風斬
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 21111009: //鬥氣填充
                    ret.hpR = -ret.x / 100.0;
                    break;
                case 22121000: //雪嵐陣
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1); //結冰
                    break;
                case 21120006: //極冰暴風
                    ret.duration = 12000;
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 21120007: //宙斯之盾
                    ret.statups.put(MapleBuffStat.COMBO_BARRIER, ret.x);
                    break;
                case 21121000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 21121003: //靈魂戰鬥
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop); //受到攻擊時不會退縮
                    break;
                case 22000002: //自然力變弱 (PVP)
                    ret.statups.put(MapleBuffStat.ELEMENT_WEAKEN, ret.x);
                    break;
                case 22111001: //魔心防禦
                    ret.statups.put(MapleBuffStat.MAGIC_GUARD, ret.x);
                    break;
                case 22131000: //魔光殺
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 22131001: //守護之力
                    ret.statups.put(MapleBuffStat.MAGIC_SHIELD, ret.x);
                    break;
                case 22131002: //自然力重置
                    ret.statups.put(MapleBuffStat.ELEMENT_RESET, ret.x);
                    break;
                case 22141002: //極速詠唱
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x); //攻擊速度提升
                    break;
                case 22151001: //火焰殺
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 22151002: //襲殺翼
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.HOMING_BEACON, ret.x);
                    break;
                case 22161002: //鬼神詛咒
                    ret.monsterStatus.put(MonsterStatus.IMPRINT, ret.x);
                    break;
                case 22151003: //魔法抵抗．改
                    ret.statups.put(MapleBuffStat.MAGIC_RESISTANCE, ret.x);
                    break;
                case 22161004: //龍神的庇護
                    ret.statups.put(MapleBuffStat.ONYX_SHROUD, ret.x);
                    break;
                case 22161005: //瞬間移動精通
                    ret.mpCon = (short) ret.y;
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.TELEPORT_MASTERY, ret.x);
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 22171000: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 22181000: //玛瑙的祝福
                    ret.statups.put(MapleBuffStat.MDEF, (int) ret.mdef);
                    break;
                case 22181001: //烈焰爆擊
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 22181003: //靈魂之石
                    ret.statups.put(MapleBuffStat.SOUL_STONE, 1);
                    break;
                case 22181004: //歐尼斯的意志
                    ret.statups.put(MapleBuffStat.ONYX_WILL, ret.x);
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    break;
                case 20021110: //精靈的祝福
                    ret.moveTo = ret.x;
                    break;
                case 23101002: //雙弩槍推進器
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x); //攻擊速度提升
                    break;
                case 23101003: //靈魂灌注
                    ret.statups.put(MapleBuffStat.SPIRIT_SURGE, ret.x);
                    ret.statups.put(MapleBuffStat.CRITICAL_INC, ret.x); //一擊機率
                    break;
                case 23111000: //光速雙擊
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 23111002: //獨角獸射擊
                    ret.monsterStatus.put(MonsterStatus.IMPRINT, ret.x);
                    break;
                case 23111005: //水之盾
                    ret.statups.put(MapleBuffStat.ABNORMAL_STATUS_R, (int) ret.terR);
                    ret.statups.put(MapleBuffStat.ELEMENTAL_STATUS_R, (int) ret.terR);
                    ret.statups.put(MapleBuffStat.WATER_SHIELD, ret.x);
                    break;
                case 23111008: //冰元素騎士
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 23111009: //火元素騎士
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 23111010: //暗元素騎士
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 23121002: //傳說之槍
                    ret.monsterStatus.put(MonsterStatus.WDEF, -ret.x);
                    break;
                case 23121004: //遠古意志
                    ret.statups.put(MapleBuffStat.PIRATES_REVENGE, (int) ret.damR);
                    break;
                case 23121005: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 20031203: //水晶花園傳送
                    ret.moveTo = ret.x;
                    break;
                case 20031205: //幻影斗蓬
                    ret.statups.put(MapleBuffStat.SHROUD_WALK, 999);
                    break;
                case 24101005: //極速手杖
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 24111002: //幸運幻影
                    ret.statups.put(MapleBuffStat.FINAL_FEINT, ret.x);
                    break;
                case 24111003://幸運卡牌守護
                    ret.statups.put(MapleBuffStat.BadLuckWard, (int) ret.level);
                    break;
                case 24121004: //艾麗亞祝禱
                    ret.statups.put(MapleBuffStat.PIRATES_REVENGE, (int) ret.level);
                    //ret.statups.put(MapleBuffStat.PriereAria, (int) ret.level);
                    break;
                case 24121007: //靈魂竊取
                    ret.statups.put(MapleBuffStat.WATER_SHIELD, ret.x);
                    ret.statups.put(MapleBuffStat.WATK, (int) ret.x);
                    ret.statups.put(MapleBuffStat.DIVINE_BODY, 1);
                    ret.statups.put(MapleBuffStat.POWERGUARD, ret.y); //傷害返還
                    break;
                case 24121008: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 31001001: //惡魔推進器
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x); //攻擊速度提升
                    break;
                case 31101002: //惡魔追擊
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 31101003: //黑暗復仇
                    ret.statups.put(MapleBuffStat.PERFECT_ARMOR, ret.y);
                    ret.statups.put(MapleBuffStat.POWERGUARD, ret.x); //傷害返還
                    break;
                case 31111001: //死亡之握
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 31111004: //黑暗耐力				
                    ret.statups.put(MapleBuffStat.ABNORMAL_STATUS_R, ret.y); //异常抗性
                    ret.statups.put(MapleBuffStat.ELEMENTAL_STATUS_R, ret.z); //属性抗性
                    ret.statups.put(MapleBuffStat.DEFENCE_BOOST_R, ret.x);
                    break;
                case 31121002: //吸血鬼之觸
                    ret.statups.put(MapleBuffStat.LeechAura, ret.x);
                    break;
                case 31121003: //魔力吶喊
                    ret.monsterStatus.put(MonsterStatus.SHOWDOWN, ret.w); //挑釁
                    ret.monsterStatus.put(MonsterStatus.MDEF, ret.x); //魔防
                    ret.monsterStatus.put(MonsterStatus.WDEF, ret.x); //物防
                    ret.monsterStatus.put(MonsterStatus.MATK, ret.x); //魔攻
                    ret.monsterStatus.put(MonsterStatus.WATK, ret.x); //物攻
                    ret.monsterStatus.put(MonsterStatus.ACC, ret.x); //减少怪物命中率
                    break;
                case 31121004: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 31121005: //變形
                    ret.statups.put(MapleBuffStat.PIRATES_REVENGE, (int) ret.damR);
                    ret.statups.put(MapleBuffStat.DARK_METAMORPHOSIS, 6);
                    break;
                case 31121007: //無限力量
                    ret.statups.put(MapleBuffStat.BOUNDLESS_RAGE, 1);
                    break;
                case 32000012:
                    ret.statups.put(MapleBuffStat.ELEMENT_WEAKEN, ret.x);
                    break;
                case 32001003: //黑色繩索
                    ret.duration = (sourceid == 32110007 ? 60000 : 2100000000);
                    ret.statups.put(MapleBuffStat.AURA, (int) ret.level);
                    ret.statups.put(MapleBuffStat.DARK_AURA, ret.x);
                    break;
                case 32101001: //神聖黑色鎖鏈
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 32101003: //黃色繩索
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.AURA, (int) ret.level);
                    ret.statups.put(MapleBuffStat.YELLOW_AURA, (int) ret.level);
                    break;
                case 32101004: //紅色吸血術
                    ret.statups.put(MapleBuffStat.COMBO_DRAIN, ret.x);
                    break;
                case 32101005: //長杖極速
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x); //提升攻擊速度
                    break;
                case 32110000: //進階藍色繩索
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.AURA, (int) ret.level);
                    ret.statups.put(MapleBuffStat.BLUE_AURA, (int) ret.level);
                    break;
                case 32110007: //黑色繩索
                    ret.duration = 60000;
                    ret.statups.put(MapleBuffStat.AURA, (int) ret.level);
                    ret.statups.put(MapleBuffStat.DARK_AURA, ret.x);
                    break;
                case 32110008: //黃色繩索
                    ret.duration = 60000;
                    ret.statups.put(MapleBuffStat.AURA, (int) ret.level);
                    ret.statups.put(MapleBuffStat.YELLOW_AURA, (int) ret.level);
                    break;
                case 32110009: //藍色繩索
                    ret.duration = 60000;
                    ret.statups.put(MapleBuffStat.AURA, (int) ret.level);
                    ret.statups.put(MapleBuffStat.BLUE_AURA, (int) ret.level);
                    break;
                case 32111004: //轉換
                    ret.statups.put(MapleBuffStat.CONVERSION, ret.x);
                    break;
                case 32111005: //超級體
                    ret.duration = 60000;
                    ret.statups.put(MapleBuffStat.BODY_BOOST, (int) ret.level);
                    break;
                case 32111006: //甦醒
                    ret.statups.put(MapleBuffStat.REAPER, 1);
                    break;
                case 32111010: //瞬間移動精通
                    ret.mpCon = (short) ret.y;
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.TELEPORT_MASTERY, ret.x);
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 32111012: //藍色繩索
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.AURA, (int) ret.level);
                    ret.statups.put(MapleBuffStat.BLUE_AURA, (int) ret.level);
                    break;
                case 32111014: //防禦姿態
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    break;
                case 32120000: //進階黑色繩索
                    ret.dot = ret.damage;
                    ret.dotTime = 3;
                    break;
                case 32120001: //進階黃色繩索
                    ret.monsterStatus.put(MonsterStatus.SPEED, (int) ret.speed);
                    break;
                case 32121003: //颶風
                    ret.statups.put(MapleBuffStat.TORNADO, ret.x);
                    break;
                case 32121004: //黑暗世紀
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 32121007: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 33001003: //快速之弩
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x); //攻擊速度提升
                    break;
                case 33101001: //炸彈之箭
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 33101002: //美洲豹咆哮
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 33101003: //無形之箭
                    ret.statups.put(MapleBuffStat.SOULARROW, ret.x);
                    break;
                case 33101004: //地雷
                    ret.statups.put(MapleBuffStat.RAINING_MINES, ret.x);
                    break;
                case 33101008: //地雷(hidden 自動爆炸)
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 33111002: //歧路
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 33111003: //瘋狂陷阱
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 33111004: //黑暗狙擊
                    ret.statups.put(MapleBuffStat.BLIND, ret.x); //致盲
                    ret.monsterStatus.put(MonsterStatus.ACC, ret.x); //减少怪物命中率
                    break;
                case 33111005: //銀鷹召喚
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 33111007: //狂獸附體
                    ret.statups.put(MapleBuffStat.SPEED, ret.y); //移動速度
                    ret.statups.put(MapleBuffStat.ATTACK_BUFF, ret.z); //提升攻擊力百分比
                    ret.statups.put(MapleBuffStat.FELINE_BERSERK, ret.x);
                    break;
                case 33121002: //音爆
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 33121004: //銳利之眼
                    ret.statups.put(MapleBuffStat.SHARP_EYES, (ret.x << 8) + ret.criticaldamageMax);
                    break;
                case 33121005: //化學彈丸
                    ret.monsterStatus.put(MonsterStatus.SHOWDOWN, ret.x);
                    ret.monsterStatus.put(MonsterStatus.MDEF, ret.x);
                    ret.monsterStatus.put(MonsterStatus.WDEF, ret.x);
                    break;
                case 33121007: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 35001001: //火焰發射
                    ret.duration = 1000;
                    ret.statups.put(MapleBuffStat.MECH_CHANGE, (int) level);
                    break;
                case 35001002: //合金盔甲: 原型
                    ret.duration = 2100000000;
                    break;
                case 35101003: //自動錘
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 35101005: //開放通道 : GX-9
                    ret.statups.put(MapleBuffStat.SOULARROW, ret.x);
                    break;
                case 35101006: //機甲戰神極速
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x); //攻擊速度提升
                    break;
                case 35101007: //全備型盔甲 (Perfect Armor)
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.PERFECT_ARMOR, ret.x);
                    break;
                case 35101009: //強化的火焰發射
                    ret.duration = 1000;
                    ret.statups.put(MapleBuffStat.MECH_CHANGE, (int) level);
                    break;
                case 35111001: //賽特拉特
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 35111002: //磁場
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1)); //眩暈
                    break;
                case 35111004: //合金盔甲: 重機槍
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.MECH_CHANGE, (int) level);
                    break;
                case 35111005: //加速器 : EX-7
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    ret.monsterStatus.put(MonsterStatus.WDEF, ret.y);
                    break;
                case 35111009:
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 35111010:
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 35111011: //治療機器人 : H-LX
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 35111013: //幸運骰子
                    ret.statups.put(MapleBuffStat.DICE_ROLL, 0);
                    break;
                case 35111015: //火箭衝擊
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 35120000: //合金盔甲終極
                    ret.duration = 2100000000;
                    break;
                case 35121003: //戰鬥機器 : 巨人錘
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 35121005: //合金盔甲: 導彈罐
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.MECH_CHANGE, (int) level);
                    break;
                case 35121006: //終極賽特拉特
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.SATELLITESAFE_PROC, ret.x);
                    ret.statups.put(MapleBuffStat.SATELLITESAFE_ABSORB, ret.y);
                    break;
                case 35121007: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 35121009: //機器人工廠 : RM1
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 35121010: //擴音器 : AF-11
                    ret.duration = 60000;
                    ret.statups.put(MapleBuffStat.DAMAGE_BUFF, ret.x); //組員傷害提升
                    break;
                case 35121011: //機器人工廠 : 機器人
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 35121013: //合金盔甲: 重機槍
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.MECH_CHANGE, (int) level);
                    break;
                case 50001075: //女皇的祈禱
                    ret.statups.put(MapleBuffStat.ECHO_OF_HERO, ret.x);
                    break;
                case 50001214: //光之守護
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    break;
                case 51101003: //快速之劍
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x); //攻擊速度提升
                    break;
                case 51101004: //激勵
                    ret.statups.put(MapleBuffStat.ENHANCED_WATK, 20);
                    break;
                case 51111003: //閃耀激發
                    ret.statups.put(MapleBuffStat.DAMAGE_BUFF, ret.z);
                    break;
                case 51111004: //靈魂抗性
                    ret.statups.put(MapleBuffStat.DEFENCE_BOOST_R, ret.x);
                    ret.statups.put(MapleBuffStat.ELEMENTAL_STATUS_R, ret.y);
                    ret.statups.put(MapleBuffStat.ABNORMAL_STATUS_R, ret.z);
                    break;
                case 51111005: //魔防消除
                    ret.monsterStatus.put(MonsterStatus.MAGIC_CRASH, Integer.valueOf(1));
                    break;
                case 51111007: //閃耀連擊
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 51121004: //格檔
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    break;
                case 51121005: //楓葉祝福
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 51121006: //靈魂之怒
                    ret.statups.put(MapleBuffStat.ENRAGE, (int) ret.mobCount);
                    ret.statups.put(MapleBuffStat.DAMAGE_BUFF, ret.x);
                    ret.statups.put(MapleBuffStat.CRITICAL_RATE_BUFF, ret.y);
                    ret.statups.put(MapleBuffStat.CRITICAL_RATE_BUFF, ret.z);
                    break;
                case 51121007: //靈魂突擊
                    ret.monsterStatus.put(MonsterStatus.DARKNESS, ret.x); //黑暗
                    break;
                case 51121008: //聖光爆發
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1); //眩暈
                    break;
                case 80001034:
                    ret.statups.put(MapleBuffStat.VIRTUE_EFFECT, 1);
                    break;
                case 80001035:
                    ret.statups.put(MapleBuffStat.VIRTUE_EFFECT, 1);
                    break;
                case 80001036:
                    ret.statups.put(MapleBuffStat.VIRTUE_EFFECT, 1);
                    break;
                case 80001040:
                    ret.moveTo = ret.x;
                    break;
                case 80001089:
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.SOARING, ret.x);
                    break;
                case 80001140: //光之守護
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    break;
                case 90001002:
                    ret.monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    break;
                case 90001003:
                    ret.monsterStatus.put(MonsterStatus.POISON, 1);
                    break;
                case 90001004:
                    ret.monsterStatus.put(MonsterStatus.DARKNESS, ret.x);
                    break;
                case 90001005:
                    ret.monsterStatus.put(MonsterStatus.SEAL, 1);
                    break;
                case 90001006:
                    ret.duration = 1000;
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case 9001002:
                    ret.statups.put(MapleBuffStat.HOLY_SYMBOL, ret.x);
                    break;
                case 9001003:
                    ret.statups.put(MapleBuffStat.BLESS, (int) ret.level);
                    break;
                case 9001008:
                    ret.statups.put(MapleBuffStat.MAXHP, ret.x);
                    ret.statups.put(MapleBuffStat.MAXMP, ret.x);
                    break;
                case 9001020:
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 9101002:
                    ret.statups.put(MapleBuffStat.DARKSIGHT, 1);
                    break;
                case 9101003:
                    ret.statups.put(MapleBuffStat.BLESS, (int) ret.level);
                    break;
                case 9101008:
                    ret.statups.put(MapleBuffStat.MAXHP, ret.x);
                    ret.statups.put(MapleBuffStat.MAXMP, ret.x);
                    break;
                case 9101020:
                    ret.duration = 2000;
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;

                default:
                    break;
            }
            if (GameConstants.isBeginnerJob(sourceid / 10000)) {
                switch (sourceid % 10000) {
                    //angelic blessing: HACK, we're actually supposed to use the passives for atk/matk buff
                    case 1087:
                        ret.duration = 2100000000;
                        ret.statups.put(MapleBuffStat.ANGEL_ATK, 10);
                        ret.statups.put(MapleBuffStat.ANGEL_MATK, 10);
                        break;
                    case 1085:
                    case 1090:
                        ret.duration = 2100000000;
                        ret.statups.put(MapleBuffStat.ANGEL_ATK, 5);
                        ret.statups.put(MapleBuffStat.ANGEL_MATK, 5);
                        break;
                    case 1179:
                        ret.duration = 2100000000;
                        ret.statups.put(MapleBuffStat.ANGEL_ATK, 12);
                        ret.statups.put(MapleBuffStat.ANGEL_MATK, 12);
                        break;
                    case 1105:
                        ret.statups.put(MapleBuffStat.ICE_SKILL, 1);
                        ret.duration = 2100000000;
                        break;
                    case 93:
                        ret.statups.put(MapleBuffStat.HIDDEN_POTENTIAL, 1);
                        break;
                    case 8001:
                        ret.statups.put(MapleBuffStat.SOULARROW, ret.x);
                        break;
                    case 1005: // Echo of Hero
                        ret.statups.put(MapleBuffStat.ECHO_OF_HERO, ret.x);
                        break;
                    case 1011: // Berserk fury
                        ret.statups.put(MapleBuffStat.BERSERK_FURY, ret.x);
                        break;
                    case 1010:
                        ret.statups.put(MapleBuffStat.DIVINE_BODY, 1);
                        break;
                    case 1001:
                        if (sourceid / 10000 == 3001 || sourceid / 10000 == 3000) { //resistance is diff
                            ret.statups.put(MapleBuffStat.INFILTRATE, ret.x);
                        } else {
                            ret.statups.put(MapleBuffStat.RECOVERY, ret.x);
                        }
                        break;
                    case 8003:
                        ret.statups.put(MapleBuffStat.MAXHP, ret.x);
                        ret.statups.put(MapleBuffStat.MAXMP, ret.x);
                        break;
                    case 8004:
                        ret.statups.put(MapleBuffStat.COMBAT_ORDERS, ret.x);
                        break;
                    case 8005:
                        ret.statups.put(MapleBuffStat.HOLY_SHIELD, 1);
                        break;
                    case 8006:
                        ret.statups.put(MapleBuffStat.SPEED_INFUSION, ret.x);
                        break;
                    case 103:
                        ret.monsterStatus.put(MonsterStatus.STUN, 1);
                        break;
                    case 99:
                    case 104:
                        ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                        ret.duration *= 2; // freezing skills are a little strange
                        break;
                    case 8002:
                        ret.statups.put(MapleBuffStat.SHARP_EYES, (ret.x << 8) + ret.criticaldamageMax);
                        break;
                    case 1026: // 飛翔技能
                    case 1142: // Soaring
                        ret.duration = 2100000000;
                        ret.statups.put(MapleBuffStat.SOARING, 1);
                        break;
                }
            }
        } else {
            switch (sourceid) {
                case 2022746: //天使祝福
                case 2022747: //黑天使祝福
                case 2022823: //白色精靈祝福
                case 2022764:
                    //ret.statups.clear(); //no atk/matk
                    ret.statups.put(MapleBuffStat.PYRAMID_PQ, 1); //ITEM_EFFECT buff
                    break;
                case 2003547:
                    ret.statups.put(MapleBuffStat.ABNORMAL_STATUS_R, (int) ret.terR); //狀態異常抗性
                    break;
                case 2003551:
                    ret.statups.put(MapleBuffStat.DROP_RATE, (int) 110); //獲得財物秘藥
                    ret.statups.put(MapleBuffStat.MESO_RATE, (int) 110);
                    break;
                case 2003549:
                    ret.statups.put(MapleBuffStat.HOLY_MAGIC_SHELL, 100); //無敵秘藥
                    break;
            }
        }
        if (ret.isPoison()) {
            ret.monsterStatus.put(MonsterStatus.POISON, 1);
        }
        if (ret.isMorph() || ret.isPirateMorph()) {
            ret.statups.put(MapleBuffStat.MORPH, ret.getMorph());
        }

        return ret;
    }

    /**
     * @param applyto
     * @param obj
     * @param attack  damage done by the skill
     */
    public final void applyPassive(final MapleCharacter applyto, final MapleMapObject obj) {
        if (makeChanceResult() && !GameConstants.isDemon(applyto.getJob())) { // demon can't heal mp
            switch (sourceid) {
                case 2100000: //魔力吸收
                case 2200000: //魔力吸收
                case 2300000: //魔力吸收
                    if (obj == null || obj.getType() != MapleMapObjectType.MONSTER) {
                        return;
                    }
                    final MapleMonster mob = (MapleMonster) obj; // x is absorb percentage
                    if (!mob.getStats().isBoss()) {
                        final int absorbMp = Math.min((int) (mob.getMobMaxMp() * (getX() / 100.0)), mob.getMp());
                        if (absorbMp > 0) {
                            mob.setMp(mob.getMp() - absorbMp);
                            applyto.getStat().setMp(applyto.getStat().getMp() + absorbMp, applyto);
                            applyto.getClient().getSession().write(EffectPacket.showOwnBuffEffect(sourceid, 1, applyto.getLevel(), level));
                            applyto.getMap().broadcastMessage(applyto, EffectPacket.showBuffeffect(applyto.getId(), sourceid, 1, applyto.getLevel(), level), false);
                        }
                    }
                    break;
            }
        }
    }

    public final boolean applyTo(MapleCharacter chr) {
        return applyTo(chr, chr, true, null, duration);
    }

    public final boolean applyTo(MapleCharacter chr, Point pos) {
        return applyTo(chr, chr, true, pos, duration);
    }

    /*
    消耗類道具設定
    */
    public final boolean applyTo(final MapleCharacter applyfrom, final MapleCharacter applyto, final boolean primary, final Point pos, int newDuration) {
        if (sourceid == 2022198) { //勒塞倫藥丸
            applyfrom.giveDebuff(MapleDisease.POISON, MobSkillFactory.getMobSkill(125, 5)); //go left
        }

        if (isHeal() && (applyfrom.getMapId() == 749040100 || applyto.getMapId() == 749040100)) {
            applyfrom.getClient().getSession().write(CWvsContext.enableActions());
            return false; //z
        } else if ((isSoaring_Mount() && applyfrom.getBuffedValue(MapleBuffStat.MONSTER_RIDING) == null) || (isSoaring_Normal() && !applyfrom.getMap().canSoar())) {
            applyfrom.getClient().getSession().write(CWvsContext.enableActions());
            return false;
        } else if (sourceid == 4341006 && applyfrom.getBuffedValue(MapleBuffStat.SHADOWPARTNER) == null) { //幻影替身
            applyfrom.getClient().getSession().write(CWvsContext.enableActions());
            return false;
        } else if (sourceid == 33101008 && (applyfrom.getBuffedValue(MapleBuffStat.RAINING_MINES) == null || applyfrom.getBuffedValue(MapleBuffStat.SUMMON) != null || !applyfrom.canSummon())) { //地雷(hidden 自動爆炸)
            applyfrom.getClient().getSession().write(CWvsContext.enableActions());
            return false;
        } else if (isShadow() && applyfrom.getJob() != 411 && applyfrom.getJob() != 412 && applyfrom.getJob() != 421 && applyfrom.getJob() != 422 && applyfrom.getJob() != 1411 && applyfrom.getJob() != 1412) { //pirate/shadow = dc
            applyfrom.getClient().getSession().write(CWvsContext.enableActions());
            return false;
        } else if (isMI() && applyfrom.getJob() != 433 && applyfrom.getJob() != 434) { //pirate/shadow = dc
            applyfrom.getClient().getSession().write(CWvsContext.enableActions());
            return false;
        } else if (sourceid == 33101004 && applyfrom.getMap().isTown()) { //地雷
            applyfrom.dropMessage(5, "You may not use this skill in towns.");
            applyfrom.getClient().getSession().write(CWvsContext.enableActions());
            return false;
        }
        int hpchange = calcHPChange(applyfrom, primary);
        int mpchange = calcMPChange(applyfrom, primary);

        final PlayerStats stat = applyto.getStat();
        if (primary) {
            if (itemConNo != 0 && !applyto.inPVP()) {
                if (!applyto.haveItem(itemCon, itemConNo, false, true)) {
                    applyto.getClient().getSession().write(CWvsContext.enableActions());
                    return false;
                }
                MapleInventoryManipulator.removeById(applyto.getClient(), GameConstants.getInventoryType(itemCon), itemCon, itemConNo, false, true);
            }
        } else if (!primary && isResurrection()) {
            hpchange = stat.getMaxHp();
            applyto.setStance(0); //TODO fix death bug, player doesnt spawn on other screen
        }
        if (isDispel() && makeChanceResult()) {
            applyto.dispelDebuffs();
        } else if (isHeroWill() || sourceid == 2003548) { //覺醒的秘藥
            applyto.dispelDebuffs();
        } else if (cureDebuffs.size() > 0) {
            for (final MapleDisease debuff : cureDebuffs) {
                applyfrom.dispelDebuff(debuff);
            }
        }
        final Map<MapleStat, Integer> hpmpupdate = new EnumMap<>(MapleStat.class);
        if (hpchange != 0) {
            if (hpchange < 0 && (-hpchange) > stat.getHp() && !applyto.hasDisease(MapleDisease.ZOMBIFY)) {
                applyto.getClient().getSession().write(CWvsContext.enableActions());
                return false;
            }
            stat.setHp(stat.getHp() + hpchange, applyto);
        }
        if (mpchange != 0) {
            if (mpchange < 0 && (-mpchange) > stat.getMp()) {
                applyto.getClient().getSession().write(CWvsContext.enableActions());
                return false;
            }
            //short converting needs math.min cuz of overflow
            if ((mpchange < 0 && GameConstants.isDemon(applyto.getJob())) || !GameConstants.isDemon(applyto.getJob())) { // heal
                stat.setMp(stat.getMp() + mpchange, applyto);
            }
            hpmpupdate.put(MapleStat.MP, Integer.valueOf(stat.getMp()));
        }
        hpmpupdate.put(MapleStat.HP, Integer.valueOf(stat.getHp()));

        applyto.getClient().getSession().write(CWvsContext.updatePlayerStats(hpmpupdate, true, applyto));
        if (expinc != 0) {
            applyto.gainExp(expinc, true, true, false);
            applyto.getClient().getSession().write(EffectPacket.showForeignEffect(20));
        } else if (sourceid / 10000 == 238) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final int mobid = ii.getCardMobId(sourceid);
            if (mobid > 0) {
                final boolean done = applyto.getMonsterBook().monsterCaught(applyto.getClient(), mobid, MapleLifeFactory.getMonsterStats(mobid).getName());
                applyto.getClient().getSession().write(CWvsContext.getCard(done ? sourceid : 0, 1));
            }
        } else if (isReturnScroll()) {
            applyReturnScroll(applyto);
        } else if (useLevel > 0 && !skill) {
            applyto.setExtractor(new MapleExtractor(applyto, sourceid, useLevel * 50, 1440)); //no clue about time left
            applyto.getMap().spawnExtractor(applyto.getExtractor());
        } else if (isMistEruption()) {
            int i = y;
            for (MapleMist m : applyto.getMap().getAllMistsThreadsafe()) {
                if (m.getOwnerId() == applyto.getId() && m.getSourceSkill().getId() == 2111003) { //致命毒霧
                    if (m.getSchedule() != null) {
                        m.getSchedule().cancel(false);
                        m.setSchedule(null);
                    }
                    if (m.getPoisonSchedule() != null) {
                        m.getPoisonSchedule().cancel(false);
                        m.setPoisonSchedule(null);
                    }
                    applyto.getMap().broadcastMessage(CField.removeMist(m.getObjectId(), true));
                    applyto.getMap().removeMapObject(m);

                    i--;
                    if (i <= 0) {
                        break;
                    }
                }
            }
        } else if (cosmetic > 0) {
            if (cosmetic >= 30000) {
                applyto.setHair(cosmetic);
                applyto.updateSingleStat(MapleStat.HAIR, cosmetic);
            } else if (cosmetic >= 20000) {
                applyto.setFace(cosmetic);
                applyto.updateSingleStat(MapleStat.FACE, cosmetic);
            } else if (cosmetic < 100) {
                applyto.setSkinColor((byte) cosmetic);
                applyto.updateSingleStat(MapleStat.SKIN, cosmetic);
            }
            applyto.equipChanged();
        } else if (bs > 0) {
            if (!applyto.inPVP()) {
                return false;
            }
            final int xd = Integer.parseInt(applyto.getEventInstance().getProperty(String.valueOf(applyto.getId())));
            applyto.getEventInstance().setProperty(String.valueOf(applyto.getId()), String.valueOf(xd + bs));
            applyto.getClient().getSession().write(CField.getPVPScore(xd + bs, false));
        } else if (iceGageCon > 0) {
            if (!applyto.inPVP()) {
                return false;
            }
            final int fdf = Integer.parseInt(applyto.getEventInstance().getProperty("icegage"));
            if (fdf < iceGageCon) {
                return false;
            }
            applyto.getEventInstance().setProperty("icegage", String.valueOf(fdf - iceGageCon));
            applyto.getClient().getSession().write(CField.getPVPIceGage(fdf - iceGageCon));
            applyto.applyIceGage(fdf - iceGageCon);
        } else if (recipe > 0) {
            if (applyto.getSkillLevel(recipe) > 0 || applyto.getProfessionLevel((recipe / 10000) * 10000) < reqSkillLevel) {
                return false;
            }
            applyto.changeSingleSkillLevel(SkillFactory.getCraft(recipe), Integer.MAX_VALUE, recipeUseCount, (long) (recipeValidDay > 0 ? (System.currentTimeMillis() + recipeValidDay * 24L * 60 * 60 * 1000) : -1L));
        } else if (isComboRecharge()) {
            applyto.setCombo((short) Math.min(30000, applyto.getCombo() + y));
            applyto.setLastCombo(System.currentTimeMillis());
            applyto.getClient().getSession().write(CField.rechargeCombo(applyto.getCombo()));
            SkillFactory.getSkill(21000000).getEffect(10).applyComboBuff(applyto, applyto.getCombo()); //矛之鬥氣
        } else if (isDragonBlink()) {
            final MaplePortal portal = applyto.getMap().getPortal(Randomizer.nextInt(applyto.getMap().getPortals().size()));
            if (portal != null) {
                applyto.getClient().getSession().write(CField.dragonBlink(portal.getId()));
                applyto.getMap().movePlayer(applyto, portal.getPosition());
                applyto.checkFollow();
            }
        } else if (isSpiritClaw()) {
            MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
            boolean itemz = false;
            for (int i = 0; i < use.getSlotLimit(); i++) { // impose order...
                Item item = use.getItem((byte) i);
                if (item != null) {
                    if (GameConstants.isThrowingStar(item.getItemId()) && item.getQuantity() >= 100 || GameConstants.isBullet(item.getItemId()) && item.getQuantity() >= 100) {
                        MapleInventoryManipulator.removeFromSlot(applyto.getClient(), MapleInventoryType.USE, (short) i, (short) 100, false, true);
                        itemz = true;
                        break;
                    }
                }
            }
            if (!itemz) {
                return false;
            }
        } else if (cp != 0 && applyto.getCarnivalParty() != null) {
            applyto.getCarnivalParty().addCP(applyto, cp);
            applyto.CPUpdate(false, applyto.getAvailableCP(), applyto.getTotalCP(), 0);
            if (applyto.getMapId() == 980031100 || applyto.getMapId() == 980032100 || applyto.getMapId() == 980033100) {
                applyto.dropMessage(-1, applyto.getName() + " has earned " + cp + " points for Team [" + (applyto.getCarnivalParty().getTeam() == 0 ? "Maple Red" : "Maple Blue") + "]!");
            }
            // @eric: this should fix CPQ giving the CP for all of the teams, gMS BB now has ONE board and does not have 2 team digits, it's client-sided.
            for (MaplePartyCharacter mpc : applyto.getParty().getMembers()) {
                if (mpc.getId() != applyto.getId() && mpc.getChannel() == applyto.getClient().getChannel() && mpc.getMapid() == applyto.getMapId() && mpc.isOnline()) {
                    MapleCharacter mc = applyto.getMap().getCharacterById(mpc.getId());
                    if (mc != null) {
                        if (mc.getMapId() == 980031100 || mc.getMapId() == 980032100 || mc.getMapId() == 980033100) {
                            mc.dropMessage(-1, applyto.getName() + " has earned " + cp + " points for Team [" + (applyto.getCarnivalParty().getTeam() == 0 ? "Maple Red" : "Maple Blue") + "]!");
                        } else {
                            mc.CPUpdate(true, applyto.getCarnivalParty().getAvailableCP(), applyto.getCarnivalParty().getTotalCP(), applyto.getCarnivalParty().getTeam());
                        }
                    }
                }
            }
        } else if (nuffSkill != 0 && applyto.getParty() != null) {
            final MCSkill skil = MapleCarnivalFactory.getInstance().getSkill(nuffSkill);
            if (skil != null) {
                final MapleDisease dis = skil.getDisease();
                for (MapleCharacter chr : applyto.getMap().getCharactersThreadsafe()) {
                    if (applyto.getParty() == null || chr.getParty() == null || (chr.getParty().getId() != applyto.getParty().getId())) {
                        if (skil.targetsAll || Randomizer.nextBoolean()) {
                            if (dis == null) {
                                chr.dispel();
                            } else if (skil.getSkill() == null) {
                                chr.giveDebuff(dis, 1, 30000, dis.getDisease(), 1);
                            } else {
                                chr.giveDebuff(dis, skil.getSkill());
                            }
                            if (!skil.targetsAll) {
                                break;
                            }
                        }
                    }
                }
            }
        } else if ((effectedOnEnemy > 0 || effectedOnAlly > 0) && primary && applyto.inPVP()) {
            final int typeee = Integer.parseInt(applyto.getEventInstance().getProperty("type"));
            if (typeee > 0 || effectedOnEnemy > 0) {
                for (MapleCharacter chr : applyto.getMap().getCharactersThreadsafe()) {
                    if (chr.getId() != applyto.getId() && (effectedOnAlly > 0 ? (chr.getTeam() == applyto.getTeam()) : (chr.getTeam() != applyto.getTeam() || typeee == 0))) {
                        applyTo(applyto, chr, false, pos, newDuration);
                    }
                }
            }
        } else if (mobSkill > 0 && mobSkillLevel > 0 && primary && applyto.inPVP()) {
            if (effectedOnEnemy > 0) {
                final int ffsa = Integer.parseInt(applyto.getEventInstance().getProperty("type"));
                for (MapleCharacter chr : applyto.getMap().getCharactersThreadsafe()) {
                    if (chr.getId() != applyto.getId() && (chr.getTeam() != applyto.getTeam() || ffsa == 0)) {
                        chr.disease(mobSkill, mobSkillLevel);
                    }
                }
            } else {
                if (sourceid == 2910000 || sourceid == 2910001) { //red flag
                    applyto.getClient().getSession().write(EffectPacket.showOwnBuffEffect(sourceid, 13, applyto.getLevel(), level));
                    applyto.getMap().broadcastMessage(applyto, EffectPacket.showBuffeffect(applyto.getId(), sourceid, 13, applyto.getLevel(), level), false);

                    applyto.getClient().getSession().write(EffectPacket.showOwnCraftingEffect("UI/UIWindow2.img/CTF/Effect", 0, 0));
                    applyto.getMap().broadcastMessage(applyto, EffectPacket.showCraftingEffect(applyto.getId(), "UI/UIWindow2.img/CTF/Effect", 0, 0), false);
                    if (applyto.getTeam() == (sourceid - 2910000)) { //restore duh flag
                        if (sourceid == 2910000) {
                            applyto.getEventInstance().broadcastPlayerMsg(-7, "The Red Team's flag has been restored.");
                        } else {
                            applyto.getEventInstance().broadcastPlayerMsg(-7, "The Blue Team's flag has been restored.");
                        }
                        applyto.getMap().spawnAutoDrop(sourceid, applyto.getMap().getGuardians().get(sourceid - 2910000).left);
                    } else {
                        applyto.disease(mobSkill, mobSkillLevel);
                        if (sourceid == 2910000) {
                            applyto.getEventInstance().setProperty("redflag", String.valueOf(applyto.getId()));
                            applyto.getEventInstance().broadcastPlayerMsg(-7, "The Red Team's flag has been captured!");
                            applyto.getClient().getSession().write(EffectPacket.showOwnCraftingEffect("UI/UIWindow2.img/CTF/Tail/Red", 600000, 0));
                            applyto.getMap().broadcastMessage(applyto, EffectPacket.showCraftingEffect(applyto.getId(), "UI/UIWindow2.img/CTF/Tail/Red", 600000, 0), false);
                        } else {
                            applyto.getEventInstance().setProperty("blueflag", String.valueOf(applyto.getId()));
                            applyto.getEventInstance().broadcastPlayerMsg(-7, "The Blue Team's flag has been captured!");
                            applyto.getClient().getSession().write(EffectPacket.showOwnCraftingEffect("UI/UIWindow2.img/CTF/Tail/Blue", 600000, 0));
                            applyto.getMap().broadcastMessage(applyto, EffectPacket.showCraftingEffect(applyto.getId(), "UI/UIWindow2.img/CTF/Tail/Blue", 600000, 0), false);
                        }
                    }
                } else {
                    applyto.disease(mobSkill, mobSkillLevel);
                }
            }
        } else if (randomPickup != null && randomPickup.size() > 0) {
            MapleItemInformationProvider.getInstance().getItemEffect(randomPickup.get(Randomizer.nextInt(randomPickup.size()))).applyTo(applyto);
        }
        for (Entry<MapleTraitType, Integer> tt : traits.entrySet()) {
            applyto.getTrait(tt.getKey()).addExp(tt.getValue(), applyto);
        }

        final SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null && (sourceid != 32111006 || (applyfrom.getBuffedValue(MapleBuffStat.REAPER) != null && !primary))) {
            int summId = sourceid;
            if (sourceid == 3111002) { //替身術
                final Skill elite = SkillFactory.getSkill(3120012); //精銳替身術
                if (applyfrom.getTotalSkillLevel(elite) > 0) {
                    return elite.getEffect(applyfrom.getTotalSkillLevel(elite)).applyTo(applyfrom, applyto, primary, pos, newDuration);
                }
            } else if (sourceid == 3211002) { //替身術
                final Skill elite = SkillFactory.getSkill(3220012); //精銳替身術
                if (applyfrom.getTotalSkillLevel(elite) > 0) {
                    return elite.getEffect(applyfrom.getTotalSkillLevel(elite)).applyTo(applyfrom, applyto, primary, pos, newDuration);
                }
            }
            final MapleSummon tosummon = new MapleSummon(applyfrom, summId, getLevel(), new Point(pos == null ? applyfrom.getTruePosition() : pos), summonMovementType);
            applyfrom.cancelEffect(this, true, -1, statups);
            applyfrom.getMap().spawnSummon(tosummon);
            applyfrom.addSummon(tosummon);
            tosummon.addHP((short) x);
            if (isBeholder()) {
                tosummon.addHP((short) 1);
            } else if (sourceid == 4341006) { //幻影替身
                applyfrom.cancelEffectFromBuffStat(MapleBuffStat.SHADOWPARTNER);
            } else if (sourceid == 32111006) { //甦醒
                applyfrom.cancelEffectFromBuffStat(MapleBuffStat.REAPER);
                //return true; //no buff
            } else if (sourceid == 35111002) { //磁場
                List<Integer> count = new ArrayList<>();
                final List<MapleSummon> ss = applyfrom.getSummonsReadLock();
                try {
                    for (MapleSummon s : ss) {
                        if (s.getSkill() == sourceid) {
                            count.add(s.getObjectId());
                        }
                    }
                } finally {
                    applyfrom.unlockSummonsReadLock();
                }
                if (count.size() != 3) {
                    return true; //no buff until 3
                }
                applyfrom.getClient().getSession().write(CField.skillCooldown(sourceid, getCooldown(applyfrom)));
                applyfrom.addCooldown(sourceid, System.currentTimeMillis(), getCooldown(applyfrom) * 1000);
                applyfrom.getMap().broadcastMessage(CField.teslaTriangle(applyfrom.getId(), count.get(0), count.get(1), count.get(2)));
            } else if (sourceid == 35121003) { //戰鬥機器 : 巨人錘
                applyfrom.getClient().getSession().write(CWvsContext.enableActions()); //doubt we need this at all
            }
        } else if (isMechDoor()) {
            int newId = 0;
            boolean applyBuff = false;
            if (applyto.getMechDoors().size() >= 2) {
                final MechDoor remove = applyto.getMechDoors().remove(0);
                newId = remove.getId();
                applyto.getMap().broadcastMessage(CField.removeMechDoor(remove, true));
                applyto.getMap().removeMapObject(remove);
            } else {
                for (MechDoor d : applyto.getMechDoors()) {
                    if (d.getId() == newId) {
                        applyBuff = true;
                        newId = 1;
                        break;
                    }
                }
            }
            final MechDoor door = new MechDoor(applyto, new Point(pos == null ? applyto.getTruePosition() : pos), newId);
            applyto.getMap().spawnMechDoor(door);
            applyto.addMechDoor(door);
            applyto.getClient().getSession().write(CWvsContext.mechPortal(door.getTruePosition()));
            if (!applyBuff) {
                return true; //do not apply buff until 2 doors spawned
            }
        }
        if (primary && availableMap != null) {
            for (Pair<Integer, Integer> e : availableMap) {
                if (applyto.getMapId() < e.left || applyto.getMapId() > e.right) {
                    applyto.getClient().getSession().write(CWvsContext.enableActions());
                    return true;
                }
            }
        }
        if (overTime && !isEnergyCharge()) {
            applyBuffEffect(applyfrom, applyto, primary, newDuration);
        }
        if (skill) {
            removeMonsterBuff(applyfrom);
        }
        if (primary) {
            if ((overTime || isHeal()) && !isEnergyCharge()) {
                applyBuff(applyfrom, newDuration);
            }
            if (isMonsterBuff()) {
                applyMonsterBuff(applyfrom);
            }
        }
        if (isMagicDoor()) { // Magic Door
            MapleDoor door = new MapleDoor(applyto, new Point(pos == null ? applyto.getTruePosition() : pos), sourceid); // Current Map door
            if (door.getTownPortal() != null) {

                applyto.getMap().spawnDoor(door);
                applyto.addDoor(door);

                MapleDoor townDoor = new MapleDoor(door); // Town door
                applyto.addDoor(townDoor);
                door.getTown().spawnDoor(townDoor);

                if (applyto.getParty() != null) { // update town doors
                    applyto.silentPartyUpdate();
                }
            } else {
                applyto.dropMessage(5, "You may not spawn a door because all doors in the town are taken.");
            }
        } else if (isMist()) {
            final Rectangle bounds = calculateBoundingBox(pos != null ? pos : applyfrom.getPosition(), applyfrom.isFacingLeft());
            final MapleMist mist = new MapleMist(bounds, applyfrom, this);
            applyfrom.getMap().spawnMist(mist, getDuration(), false);

        } else if (isTimeLeap()) { // Time Leap
            for (MapleCoolDownValueHolder i : applyto.getCooldowns()) {
                if (i.skillId != 5121010) { //時間置換
                    applyto.removeCooldown(i.skillId);
                    applyto.getClient().getSession().write(CField.skillCooldown(i.skillId, 0));
                }
            }
        } else {
        }
        if (fatigueChange != 0 && applyto.getSummonedFamiliar() != null && (familiars == null || familiars.contains(applyto.getSummonedFamiliar().getFamiliar()))) {
            applyto.getSummonedFamiliar().addFatigue(applyto, fatigueChange);
        }
        if (rewardMeso != 0) {
            applyto.gainMeso(rewardMeso, false);
        }
        if (rewardItem != null && totalprob > 0) {
            for (Triple<Integer, Integer, Integer> reward : rewardItem) {
                if (MapleInventoryManipulator.checkSpace(applyto.getClient(), reward.left, reward.mid, "") && reward.right > 0 && Randomizer.nextInt(totalprob) < reward.right) { // Total prob
                    if (GameConstants.getInventoryType(reward.left) == MapleInventoryType.EQUIP) {
                        final Item item = MapleItemInformationProvider.getInstance().getEquipById(reward.left);
                        item.setGMLog("Reward item (effect): " + sourceid + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.addbyItem(applyto.getClient(), item);
                    } else {
                        MapleInventoryManipulator.addById(applyto.getClient(), reward.left, reward.mid.shortValue(), "Reward item (effect): " + sourceid + " on " + FileoutputUtil.CurrentReadable_Date());
                    }
                }
            }
        }
        if (familiarTarget == 2 && applyfrom.getParty() != null && primary) { //to party
            for (MaplePartyCharacter mpc : applyfrom.getParty().getMembers()) {
                if (mpc.getId() != applyfrom.getId() && mpc.getChannel() == applyfrom.getClient().getChannel() && mpc.getMapid() == applyfrom.getMapId() && mpc.isOnline()) {
                    MapleCharacter mc = applyfrom.getMap().getCharacterById(mpc.getId());
                    if (mc != null) {
                        applyTo(applyfrom, mc, false, null, newDuration);
                    }
                }
            }
        } else if (familiarTarget == 3 && primary) {
            for (MapleCharacter mc : applyfrom.getMap().getCharactersThreadsafe()) {
                if (mc.getId() != applyfrom.getId()) {
                    applyTo(applyfrom, mc, false, null, newDuration);
                }
            }
        }
        return true;
    }

    /*
    回城卷軸相關設定
    */
    public final boolean applyReturnScroll(final MapleCharacter applyto) {
        if (moveTo != -1) {
            MapleMap target;
            if (moveTo == 999999999) { //回城卷軸
                target = applyto.getMap().getReturnMap();
            } else {
                target = ChannelServer.getInstance(applyto.getClient().getChannel()).getMapFactory().getMap(moveTo);
            }
            if (sourceid == 20031203) { //水晶花園傳送
                applyto.getQuestNAdd(MapleQuest.getInstance(25010)).setCustomData("" + applyto.getMapId());
            }
            applyto.changeMap(target, target.getPortal(0));
            return true;
        }
        return false;
    }

    private boolean isSoulStone() {
        return skill && sourceid == 22181003; //靈魂之石
    }

    private void applyBuff(final MapleCharacter applyfrom, int newDuration) {
        if (isSoulStone()) {
            if (applyfrom.getParty() != null) {
                int membrs = 0;
                for (MapleCharacter chr : applyfrom.getMap().getCharactersThreadsafe()) {
                    if (chr.getParty() != null && chr.getParty().getId() == applyfrom.getParty().getId() && chr.isAlive()) {
                        membrs++;
                    }
                }
                List<MapleCharacter> awarded = new ArrayList<>();
                while (awarded.size() < Math.min(membrs, y)) {
                    for (MapleCharacter chr : applyfrom.getMap().getCharactersThreadsafe()) {
                        if (chr != null && chr.isAlive() && chr.getParty() != null && chr.getParty().getId() == applyfrom.getParty().getId() && !awarded.contains(chr) && Randomizer.nextInt(y) == 0) {
                            awarded.add(chr);
                        }
                    }
                }
                for (MapleCharacter chr : awarded) {
                    applyTo(applyfrom, chr, false, null, newDuration);
                    chr.getClient().getSession().write(EffectPacket.showOwnBuffEffect(sourceid, 2, applyfrom.getLevel(), level));
                    chr.getMap().broadcastMessage(chr, EffectPacket.showBuffeffect(chr.getId(), sourceid, 2, applyfrom.getLevel(), level), false);
                }
            }
        } else if (isPartyBuff() && (applyfrom.getParty() != null || applyfrom.inPVP())) {
            final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
            final List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(MapleMapObjectType.PLAYER));

            for (final MapleMapObject affectedmo : affecteds) {
                final MapleCharacter affected = (MapleCharacter) affectedmo;

                if (affected.getId() != applyfrom.getId() && ((applyfrom.inPVP() && affected.getTeam() == applyfrom.getTeam() && Integer.parseInt(applyfrom.getEventInstance().getProperty("type")) != 0) || (applyfrom.getParty() != null && affected.getParty() != null && applyfrom.getParty().getId() == affected.getParty().getId()))) {
                    if ((isResurrection() && !affected.isAlive()) || (!isResurrection() && affected.isAlive())) {
                        applyTo(applyfrom, affected, false, null, newDuration);
                        affected.getClient().getSession().write(EffectPacket.showOwnBuffEffect(sourceid, 2, applyfrom.getLevel(), level));
                        affected.getMap().broadcastMessage(affected, EffectPacket.showBuffeffect(affected.getId(), sourceid, 2, applyfrom.getLevel(), level), false);
                    }
                    if (isTimeLeap()) {
                        for (MapleCoolDownValueHolder i : affected.getCooldowns()) {
                            if (i.skillId != 5121010) { //時間置換
                                affected.removeCooldown(i.skillId);
                                affected.getClient().getSession().write(CField.skillCooldown(i.skillId, 0));
                            }
                        }
                    }
                }
            }
        }
    }

    /*
    消除怪物BUFF相關內容
    */
    private void removeMonsterBuff(final MapleCharacter applyfrom) {
        List<MonsterStatus> cancel = new ArrayList<>();
        switch (sourceid) {
            case 1111007: //防禦消除
            case 1211009: //魔防消除
            case 1311007: //魔防消除
            case 11111008: //魔防消除
            case 51111005: //魔防消除
                cancel.add(MonsterStatus.WEAPON_DEFENSE_UP);
                cancel.add(MonsterStatus.MAGIC_DEFENSE_UP);
                cancel.add(MonsterStatus.WEAPON_ATTACK_UP);
                cancel.add(MonsterStatus.MAGIC_ATTACK_UP);
                break;
            default:
                return;
        }
        final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
        final List<MapleMapObject> affected = applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(MapleMapObjectType.MONSTER));
        int i = 0;

        for (final MapleMapObject mo : affected) {
            if (makeChanceResult()) {
                for (MonsterStatus stat : cancel) {
                    ((MapleMonster) mo).cancelStatus(stat);
                }
            }
            i++;
            if (i >= mobCount) {
                break;
            }
        }
    }

    /*
    給與怪物BUFF狀態
    */
    public final void applyMonsterBuff(final MapleCharacter applyfrom) {
        final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
        final boolean pvp = applyfrom.inPVP();
        final MapleMapObjectType typefe = pvp ? MapleMapObjectType.PLAYER : MapleMapObjectType.MONSTER;
        final List<MapleMapObject> affected = sourceid == 35111005 ? applyfrom.getMap().getMapObjectsInRange(applyfrom.getTruePosition(), Double.POSITIVE_INFINITY, Arrays.asList(typefe)) : applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(typefe));
        int i = 0;

        for (final MapleMapObject mo : affected) {
            if (makeChanceResult()) {
                for (Map.Entry<MonsterStatus, Integer> stat : getMonsterStati().entrySet()) {
                    if (pvp) {
                        MapleCharacter chr = (MapleCharacter) mo;
                        MapleDisease d = MonsterStatus.getLinkedDisease(stat.getKey());
                        if (d != null) {
                            chr.giveDebuff(d, stat.getValue(), getDuration(), d.getDisease(), 1);
                        }
                    } else {
                        MapleMonster mons = (MapleMonster) mo;
                        if (sourceid == 35111005 && mons.getStats().isBoss()) { //加速器 : EX-7
                            break;
                        }
                        mons.applyStatus(applyfrom, new MonsterStatusEffect(stat.getKey(), stat.getValue(), sourceid, null, false), isPoison(), isSubTime(sourceid) ? getSubTime() : getDuration(), true, this);
                    }
                }
                if (pvp && skill) {
                    MapleCharacter chr = (MapleCharacter) mo;
                    handleExtraPVP(applyfrom, chr);
                }
            }
            i++;
            if (i >= mobCount && sourceid != 35111005) { //加速器 : EX-7
                break;
            }
        }
    }

    public final boolean isSubTime(final int source) {
        switch (source) {
            case 1201006: //降魔咒
            case 23111008: //元素騎士
            case 23111009: //元素騎士
            case 23111010: //元素騎士
            case 31101003: //黑暗復仇
            case 31121003: //魔力吶喊
            case 31121005: //變形
                return true;
        }
        return false;
    }

    public final void handleExtraPVP(MapleCharacter applyfrom, MapleCharacter chr) {

        //喚化術
        if (sourceid == 2311005 || sourceid == 1201006 || (GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 104)) { //doom, threaten, snatch
            final long starttime = System.currentTimeMillis();

            final int localsourceid = sourceid;
            final Map<MapleBuffStat, Integer> localstatups = new EnumMap<>(MapleBuffStat.class);

            if (sourceid == 2311005) {  //喚化術
                localstatups.put(MapleBuffStat.MORPH, 7);

            } else if (sourceid == 1201006) { //降魔咒
                localstatups.put(MapleBuffStat.THREATEN_PVP, (int) level);

            } else {
                localstatups.put(MapleBuffStat.MORPH, x);
            }
            chr.getClient().getSession().write(BuffPacket.giveBuff(localsourceid, getDuration(), localstatups, this));
            chr.registerEffect(this, starttime, BuffTimer.getInstance().schedule(new CancelEffectAction(chr, this, starttime, localstatups), isSubTime(sourceid) ? getSubTime() : getDuration()), localstatups, false, getDuration(), applyfrom.getId());
        }
    }

    public final Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft) {
        return calculateBoundingBox(posFrom, facingLeft, lt, rb, range);
    }

    public final Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft, int addedRange) {
        return calculateBoundingBox(posFrom, facingLeft, lt, rb, range + addedRange);
    }

    public static Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft, final Point lt, final Point rb, final int range) {
        if (lt == null || rb == null) {
            return new Rectangle((facingLeft ? (-200 - range) : 0) + posFrom.x, (-100 - range) + posFrom.y, 200 + range, 100 + range);
        }
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x - range, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(lt.x * -1 + posFrom.x + range, rb.y + posFrom.y);
            mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
        }
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }

    public final double getMaxDistanceSq() { //lt = infront of you, rb = behind you; not gonna distanceSq the two points since this is in relative to player position which is (0,0) and not both directions, just one
        final int maxX = Math.max(Math.abs(lt == null ? 0 : lt.x), Math.abs(rb == null ? 0 : rb.x));
        final int maxY = Math.max(Math.abs(lt == null ? 0 : lt.y), Math.abs(rb == null ? 0 : rb.y));
        return (maxX * maxX) + (maxY * maxY);
    }

    public final void setDuration(int d) {
        this.duration = d;
    }

    public final void silentApplyBuff(final MapleCharacter chr, final long starttime, final int localDuration, final Map<MapleBuffStat, Integer> statup, final int cid) {
        chr.registerEffect(this, starttime, BuffTimer.getInstance().schedule(new CancelEffectAction(chr, this, starttime, statup),
                ((starttime + localDuration) - System.currentTimeMillis())), statup, true, localDuration, cid);

        final SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null) {
            final MapleSummon tosummon = new MapleSummon(chr, this, chr.getTruePosition(), summonMovementType);
            if (!tosummon.isPuppet()) {
                chr.getMap().spawnSummon(tosummon);
                chr.addSummon(tosummon);
                tosummon.addHP((short) x);
                if (isBeholder()) {
                    tosummon.addHP((short) 1);
                }
            }
        }
    }

    public final void applyComboBuff(final MapleCharacter applyto, short combo) {
        final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
        stat.put(MapleBuffStat.ARAN_COMBO, (int) combo);
        applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid, 99999, stat, this)); // Hackish timing, todo find out

        final long starttime = System.currentTimeMillis();
//	final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
//	final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime + 99999) - System.currentTimeMillis()));
        applyto.registerEffect(this, starttime, null, applyto.getId());
    }

    public final void applyEnergyBuff(final MapleCharacter applyto, final boolean infinity) {
        final long starttime = System.currentTimeMillis();
        if (infinity) {
            applyto.getClient().getSession().write(BuffPacket.giveEnergyChargeTest(0, duration / 1000));
            applyto.registerEffect(this, starttime, null, applyto.getId());
        } else {
            final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
            stat.put(MapleBuffStat.ENERGY_CHARGE, 10000);
            applyto.cancelEffect(this, true, -1, stat);
            applyto.getMap().broadcastMessage(applyto, BuffPacket.giveEnergyChargeTest(applyto.getId(), 10000, duration / 1000), false);
            final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime, stat);
            final ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, ((starttime + duration) - System.currentTimeMillis()));
            applyto.registerEffect(this, starttime, schedule, stat, false, duration, applyto.getId());

        }
    }

    /*
    角色加載BUFF相關設定
    */
    private void applyBuffEffect(final MapleCharacter applyfrom, final MapleCharacter applyto, final boolean primary, final int newDuration) {
        int localDuration = newDuration;
        if (primary) {
            localDuration = Math.max(newDuration, alchemistModifyVal(applyfrom, localDuration, false));
        }
        Map<MapleBuffStat, Integer> localstatups = statups, maskedStatups = null;
        boolean normal = true, showEffect = primary;
        int maskedDuration = 0;
        switch (sourceid) {
            case 8006:
            case 5001005: //衝鋒
            case 5121009: //最終極速
            case 10008006:
            case 15001003: //衝鋒
            case 15111005: //最終極速
            case 20008006:
            case 20018006:
            case 20028006:
            case 30008006:
            case 30018006: {
                applyto.getClient().getSession().write(BuffPacket.givePirate(statups, localDuration / 1000, sourceid));
                if (!applyto.isHidden()) {
                    applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignPirate(statups, localDuration / 1000, applyto.getId(), sourceid), false);
                }
                normal = false;
                break;
            }
            case 1111002: //鬥氣集中
            case 11111001: {
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.COMBO, 0);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 1121010: { //鬥氣爆發
                applyto.handleOrbconsume(10);
                break;
            }
            case 1211006: //寒冰之劍
            case 1211004: //烈焰之劍
            case 1221004: //聖靈之劍
            case 11111007: {//閃耀激發
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.WK_CHARGE, 1);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 1220013: { //祝福護甲
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.DIVINE_SHIELD, 1);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 2022746:
            case 2022747: {
                if (applyto.isHidden()) {
                    break;
                }
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), maskedStatups == null ? localstatups : maskedStatups, this), false);
                break;
            }
            case 2120010: //神秘狙擊
            case 2220010: //神秘狙擊
            case 2320011: {
                if (applyto.getFirstLinkMid() > 0) {
                    applyto.getClient().getSession().write(BuffPacket.giveArcane(applyto.getAllLinkMid(), localDuration));
                } else {
                    return;
                }
                break;
            }
            case 2121004: //魔力無限
            case 2221004: //魔力無限
            case 2321004: {
                maskedDuration = alchemistModifyVal(applyfrom, 4000, false);
                break;
            }
            case 2321005: { //進階祝福
                applyto.cancelEffectFromBuffStat(MapleBuffStat.BLESS);
                break;
            }
            case 3101004: //無形之箭
            case 3201004: //無形之箭
            case 13101003: {
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.SOULARROW, 0);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 3111005: { //召喚鳳凰
                if (applyfrom.getTotalSkillLevel(3120006) > 0) { //鳳凰附體
                    SkillFactory.getSkill(3120006).getEffect(applyfrom.getTotalSkillLevel(3120006)).applyBuffEffect(applyfrom, applyto, primary, newDuration);
                }
                break;
            }
            case 3120006: //鳳凰附體
            case 3220005: { //銀隼附體
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.SPIRIT_LINK, 0);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 3211005: { //召喚銀隼
                if (applyfrom.getTotalSkillLevel(3220005) > 0) { //銀隼附體
                    SkillFactory.getSkill(3220005).getEffect(applyfrom.getTotalSkillLevel(3220005)).applyBuffEffect(applyfrom, applyto, primary, newDuration);
                }
                break;
            }
            case 4001003: { //隱身術 進階隱身術
                if (applyfrom.getTotalSkillLevel(4330001) > 0 && ((applyfrom.getJob() >= 430 && applyfrom.getJob() <= 434) || (applyfrom.getJob() == 400 && applyfrom.getSubcategory() == 1))) {
                    SkillFactory.getSkill(4330001).getEffect(applyfrom.getTotalSkillLevel(4330001)).applyBuffEffect(applyfrom, applyto, primary, newDuration);
                    return;
                }
            }
            case 4111002: //影分身
            case 4211008: //影分身
            case 4331002: //替身術
            case 14111000: {
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.SHADOWPARTNER, x);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 4330001: //進階隱身術
            case 14001003: { //隱身術
                if (applyto.isHidden()) {
                    return;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.DARKSIGHT, 0);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 4341002: { //絕殺刃
                localstatups = new EnumMap<>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.FINAL_CUT, y);
                applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid, localDuration, localstatups, this));
                normal = false;
                break;
            }
            case 5111007: //幸運骰子
            case 5211007: //幸運骰子
            case 5311005: //幸運骰子
            case 5711011: //幸運骰子
            case 15111011:
            case 35111013: {
                final int zz = Randomizer.nextInt(6) + 1;
                applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto.getId(), sourceid, zz, -1, level), false);
                applyto.getClient().getSession().write(EffectPacket.showOwnDiceEffect(sourceid, zz, -1, level));
                if (zz <= 1) {
                    return;
                }
                localstatups = new EnumMap<>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.DICE_ROLL, zz);
                applyto.getClient().getSession().write(BuffPacket.giveDice(zz, sourceid, localDuration, localstatups));
                normal = false;
                showEffect = false;
                break;
            }
            case 5211011: //召喚船員
            case 5211015: //召喚船員
            case 5211016: {
                if (applyfrom.getTotalSkillLevel(5220019) > 0) { //船員指令
                    SkillFactory.getSkill(5220019).getEffect(applyfrom.getTotalSkillLevel(5220019)).applyBuffEffect(applyfrom, applyto, primary, newDuration);
                }
                break;
            }
            case 5120012: //雙倍幸運骰子
            case 5220014: //雙倍幸運骰子
            case 5320007: //雙倍幸運骰子
            case 5720005: {
                final int zz = Randomizer.nextInt(6) + 1;
                final int zz2 = makeChanceResult() ? (Randomizer.nextInt(6) + 1) : 0;
                applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto.getId(), sourceid, zz, zz2 > 0 ? -1 : 0, level), false);
                applyto.getClient().getSession().write(EffectPacket.showOwnDiceEffect(sourceid, zz, zz2 > 0 ? -1 : 0, level));
                if (zz <= 1 && zz2 <= 1) {
                    return;
                }
                final int buffid = zz == zz2 ? (zz * 100) : (zz <= 1 ? zz2 : (zz2 <= 1 ? zz : (zz * 10 + zz2)));
                if (buffid >= 100) {
                    applyto.dropMessage(-6, "[Double Lucky Dice] You have rolled a Double Down! (" + (buffid / 100) + ")");
                } else if (buffid >= 10) {
                    applyto.dropMessage(-6, "[Double Lucky Dice] You have rolled two dice. (" + (buffid / 10) + " and " + (buffid % 10) + ")");
                }
                localstatups = new EnumMap<>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.DICE_ROLL, buffid);
                applyto.getClient().getSession().write(BuffPacket.giveDice(zz, sourceid, localDuration, localstatups));
                normal = false;
                showEffect = false;
                break;
            }
            case 5221015: //無盡追擊
            case 5721003: //精準砲擊
            case 22151002: { //襲殺翼
                if (applyto.getFirstLinkMid() > 0) {
                    applyto.getClient().getSession().write(BuffPacket.cancelHoming());
                    applyto.getClient().getSession().write(BuffPacket.giveHoming(sourceid, applyto.getFirstLinkMid(), 1));
                } else {
                    return;
                }
                normal = false;
                break;
            }
            case 5311004: { //幸運木桶
                final int zz = Randomizer.nextInt(4) + 1;
                applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto.getId(), sourceid, zz, -1, (int) level), false);
                applyto.getClient().getSession().write(EffectPacket.showOwnDiceEffect(sourceid, zz, -1, (int) level));
                localstatups = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.BARREL_ROLL, zz);
                break;
            }
            case 13101006: { //風影漫步
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.DARKSIGHT, 1);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 15111006: { //閃光擊
                localstatups = new EnumMap<>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.SPARK, x);
                applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid, localDuration, localstatups, this));
                normal = false;
                break;
            }
            case 20031209: //卡牌審判
            case 20031210: {
                int zz = Randomizer.nextInt(this.sourceid == 20031209 ? 2 : 5) + 1;
                int skillid = 24100003;
                if (applyto.getSkillLevel(24120002) > 0) { //死神卡牌
                    skillid = 24120002;
                }
                applyto.setCardStack(0);
                applyto.addRunningStack(skillid == 24100003 ? 5 : 10);
                applyto.getMap().broadcastMessage(applyto, CField.gainCardStack(applyto.getId(), applyto.getRunningStack(), skillid == 24120002 ? 2 : 1, skillid, 0, skillid == 24100003 ? 5 : 10), true);
                applyto.getMap().broadcastMessage(applyto, CField.EffectPacket.showDiceEffect(applyto.getId(), this.sourceid, zz, -1, this.level), false);
                applyto.getClient().getSession().write(CField.EffectPacket.showOwnDiceEffect(this.sourceid, zz, -1, this.level));
                localstatups = new EnumMap<>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.JUDGMENT_DRAW, Integer.valueOf(zz));
                if (zz == 5) {
                    localstatups.put(MapleBuffStat.ABSORB_DAMAGE_HP, z);
                }
                applyto.getClient().getSession().write(CWvsContext.BuffPacket.giveCard(this.sourceid, localDuration, localstatups, this));
                normal = false;
                showEffect = false;
                break;
            }
            case 22131001: { //守護之力
                final List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MAGIC_SHIELD, x));
                //applyto.getMap().broadcastMessage(applyto, CField.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 23101003: { //靈魂灌注
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.SPIRIT_SURGE, x);
                stat.put(MapleBuffStat.CRITICAL_INC, x);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 23111005: { //水之盾
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.WATER_SHIELD, x);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 30001001:
            case 30011001: {
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.INFILTRATE, 0);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 31121005: { //變形
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.DARK_METAMORPHOSIS, 6);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 32001003: { //黑色繩索
                if (applyfrom.getTotalSkillLevel(32120000) > 0) { //進階黑色繩索
                    SkillFactory.getSkill(32120000).getEffect(applyfrom.getTotalSkillLevel(32120000)).applyBuffEffect(applyfrom, applyto, primary, newDuration);
                    return;
                }
                final EnumMap<MapleBuffStat, Integer> statt = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                statt.put(MapleBuffStat.DARK_AURA, x);
                applyto.getClient().getSession().write(BuffPacket.giveBuff(32001003, localDuration, statt, this));
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), statt, this), false);
                break;
            }
            case 32101003: { //黃色繩索
                if (applyfrom.getTotalSkillLevel(32120001) > 0) { //進階黃色繩索
                    SkillFactory.getSkill(32120001).getEffect(applyfrom.getTotalSkillLevel(32120001)).applyBuffEffect(applyfrom, applyto, primary, newDuration);
                    return;
                }
                final EnumMap<MapleBuffStat, Integer> statt = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                statt.put(MapleBuffStat.YELLOW_AURA, x);
                applyto.getClient().getSession().write(BuffPacket.giveBuff(32101003, localDuration, statt, this));
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), statt, this), false);
                break;
            }
            case 32110000: { //進階藍色繩索
                if (sourceid == 32111012) {
                    localDuration = 2100000000;
                }
                applyto.cancelEffectFromBuffStat(MapleBuffStat.BLUE_AURA);

                applyto.cancelEffectFromBuffStat(MapleBuffStat.BODY_BOOST);
                final EnumMap<MapleBuffStat, Integer> statt = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                statt.put(sourceid == 32110009 ? MapleBuffStat.BODY_BOOST : MapleBuffStat.AURA, (int) (sourceid == 32110000 ? applyfrom.getTotalSkillLevel(32111012) : level));
                applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid == 32110000 ? 32111012 : sourceid, localDuration, statt, this));
                statt.clear();
                statt.put(MapleBuffStat.BLUE_AURA, (int) level);
                applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid, localDuration, statt, this));
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), statt, this), false);
                normal = false;
                break;
            }
            case 32110007:
            case 32120000: { //進階黑色繩索
                applyto.cancelEffectFromBuffStat(MapleBuffStat.DARK_AURA);

                applyto.cancelEffectFromBuffStat(MapleBuffStat.BODY_BOOST);
                final EnumMap<MapleBuffStat, Integer> statt = new EnumMap<>(MapleBuffStat.class);
                statt.put(sourceid == 32110007 ? MapleBuffStat.BODY_BOOST : MapleBuffStat.AURA, (int) (sourceid == 32120000 ? applyfrom.getTotalSkillLevel(32001003) : level));
                applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid == 32120000 ? 32001003 : sourceid, localDuration, statt, this));
                statt.clear();
                statt.put(MapleBuffStat.DARK_AURA, x);
                applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid, localDuration, statt, this));
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), statt, this), false);
                normal = false;
                break;
            }
            //case 32110008: {
            //    localDuration = 10000;
            //}
            case 32110009:
            case 32120001: { // 進階黃色繩索
                applyto.cancelEffectFromBuffStat(MapleBuffStat.YELLOW_AURA);

                applyto.cancelEffectFromBuffStat(MapleBuffStat.BODY_BOOST);
                final EnumMap<MapleBuffStat, Integer> statt = new EnumMap<>(MapleBuffStat.class);
                statt.put(sourceid == 32110008 ? MapleBuffStat.BODY_BOOST : MapleBuffStat.AURA, (int) (sourceid == 32120001 ? applyfrom.getTotalSkillLevel(32101003) : level));
                applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid == 32120001 ? 32101003 : sourceid, localDuration, statt, this));
                statt.clear();
                statt.put(MapleBuffStat.YELLOW_AURA, (int) level);
                applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid, localDuration, statt, this));
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), statt, this), false);
                normal = false;
                break;
            }
            case 32111005: { //超級體
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.BODY_BOOST);
                List<Triple<MapleBuffStat, Integer, Integer>> statt = new ArrayList<>();
                //int sourcez = 0;
                if (applyfrom.getStatForBuff(MapleBuffStat.DARK_AURA) != null) {
                    statt.add(new Triple<>(MapleBuffStat.DARK_AURA, (int) (level + 10 + applyto.getTotalSkillLevel(32001003)), 32001003)); //i think
                }
                if (applyfrom.getStatForBuff(MapleBuffStat.YELLOW_AURA) != null) {
                    statt.add(new Triple<>(MapleBuffStat.YELLOW_AURA, (int) applyto.getTotalSkillLevel(32101003), 32101003)); //i think
                }
                if (applyfrom.getStatForBuff(MapleBuffStat.BLUE_AURA) != null) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.BLUE_AURA);
                    int sourcez = 32111012;
                    int ilocalDuration = 5000 + (int) Math.floor(250 * level); //5+d(x/4)
                    final EnumMap<MapleBuffStat, Integer> istat = new EnumMap<>(MapleBuffStat.class);
                    istat.put(MapleBuffStat.DIVINE_BODY, 100);
                    applyto.getClient().getSession().write(BuffPacket.giveBuff(sourcez, ilocalDuration, istat, this));
                    final long starttime = System.currentTimeMillis();
                    final Runnable icancelAction = new Runnable() {
                        @Override
                        public void run() {
                            if (applyto != null) {
                                applyto.cancelBuffStats(MapleBuffStat.DIVINE_BODY);
                                SkillFactory.getSkill(32111012).getEffect(applyto.getTotalSkillLevel(32111012)).applyTo(applyto);
                            }
                        }
                    };
                    BuffTimer.getInstance().schedule(icancelAction, ilocalDuration);
                }
                localstatups = new EnumMap<>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.BODY_BOOST, (int) level);
                //localstatups.put(MapleBuffStat.DIVINE_BODY, 100);
                applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid, localDuration, localstatups, this));
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                for (Triple<MapleBuffStat, Integer, Integer> s : statt) {
                    localstatups.put(s.left, s.mid);
                    stat.put(s.left, s.mid);
                    applyto.getClient().getSession().write(BuffPacket.giveBuff(s.right, localDuration, stat, this));
                    stat.clear();
                }
                normal = false;
                break;
            }
            case 32111012: { // 藍色繩索
                if (applyfrom.getTotalSkillLevel(32110000) > 0) { //進階藍色繩索
                    SkillFactory.getSkill(32110000).getEffect(applyfrom.getTotalSkillLevel(32110000)).applyBuffEffect(applyfrom, applyto, primary, newDuration);
                    return;
                }
                final EnumMap<MapleBuffStat, Integer> statt = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);

                statt.put(MapleBuffStat.BLUE_AURA, x);
                applyto.getClient().getSession().write(BuffPacket.giveBuff(32111012, localDuration, statt, this));
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), statt, this), false);
                break;
            }
            case 32121003: { //颶風
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.TORNADO, x);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 33101006: {
                applyto.clearLinkMid();
                MapleBuffStat theBuff = null;
                int theStat = y;
                switch (Randomizer.nextInt(6)) {
                    case 0:
                        theBuff = MapleBuffStat.CRITICAL_RATE_BUFF;
                        break;
                    case 1:
                        theBuff = MapleBuffStat.MP_BUFF;
                        break;
                    case 2:
                        theBuff = MapleBuffStat.DAMAGE_TAKEN_BUFF;
                        theStat = x;
                        break;
                    case 3:
                        theBuff = MapleBuffStat.DODGE_CHANGE_BUFF;
                        theStat = x;
                        break;
                    case 4:
                        theBuff = MapleBuffStat.DAMAGE_BUFF;
                        break;
                    case 5:
                        theBuff = MapleBuffStat.ATTACK_BUFF;
                        break;
                }
                localstatups = new EnumMap<>(MapleBuffStat.class);
                localstatups.put(theBuff, theStat);
                applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid, localDuration, localstatups, this));
                normal = false;
                break;
            }
            case 35001001: //火焰發射
            case 35101009: //強化的火焰發射
            case 35121013: //合金盔甲: 重機槍
            case 35121005: { //合金盔甲: 導彈罐
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.MECH_CHANGE, 1);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 35111004: { //合金盔甲: 重機槍
                if (applyto.getBuffedValue(MapleBuffStat.MECH_CHANGE) != null && applyto.getBuffSource(MapleBuffStat.MECH_CHANGE) == 35121005) {  //合金盔甲: 導彈罐
                    SkillFactory.getSkill(35121013).getEffect(level).applyBuffEffect(applyfrom, applyto, primary, newDuration);
                    return;
                }
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.MECH_CHANGE, 1);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 35001002: { //合金盔甲: 原型
                if (applyfrom.getTotalSkillLevel(35120000) > 0) { //合金盔甲終極
                    SkillFactory.getSkill(35120000).getEffect(applyfrom.getTotalSkillLevel(35120000)).applyBuffEffect(applyfrom, applyto, primary, newDuration);
                    return;
                }
            }

            //fallthrough intended
            default:
                if (isPirateMorph()) { //變身属性新增
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.MORPH, getMorph(applyto));
                    applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                    applyto.getClient().getSession().write(BuffPacket.giveBuff(sourceid, localDuration, stat, this));
                    maskedStatups = new EnumMap<>(localstatups);
                    maskedStatups.remove(MapleBuffStat.MORPH);
                    normal = maskedStatups.size() > 0;
                } else if (isMorph()) {
                    if (applyto.isHidden()) {
                        break;
                    }
                    if (isIceKnight()) {
                        //odd
                        final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                        stat.put(MapleBuffStat.ICE_KNIGHT, 2);
                        applyto.getClient().getSession().write(BuffPacket.giveBuff(0, localDuration, stat, this));
                    }
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.MORPH, getMorph(applyto));
                    applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                } else if (isInflation()) {
                    if (applyto.isHidden()) {
                        break;
                    }
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.GIANT_POTION, (int) inflation);
                    applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                } else if (charColor > 0) {
                    if (applyto.isHidden()) {
                        break;
                    }
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.FAMILIAR_SHADOW, 1);
                    applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                } else if (isMonsterRiding()) {
                    localDuration = 2100000000;
                    localstatups = new EnumMap<>(statups);
                    localstatups.put(MapleBuffStat.MONSTER_RIDING, 1);
                    final int mountid = parseMountInfo(applyto, sourceid);
                    final int mountid2 = parseMountInfo_Pure(applyto, sourceid);
                    if (mountid != 0 && mountid2 != 0) {
                        final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                        stat.put(MapleBuffStat.MONSTER_RIDING, 0);
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.POWERGUARD);
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.MANA_REFLECTION);
                        applyto.getClient().getSession().write(BuffPacket.giveMount(mountid2, sourceid, stat));
                        applyto.getMap().broadcastMessage(applyto, BuffPacket.showMonsterRiding(applyto.getId(), stat, mountid, sourceid), false);
                    } else {
                        return;
                    }
                    maskedStatups = new EnumMap<MapleBuffStat, Integer>(localstatups);
                    maskedStatups.remove(MapleBuffStat.MONSTER_RIDING);
                    normal = maskedStatups.size() > 0;
                    //normal = false;
                } else if (isSoaring()) {
                    if (applyto.isHidden()) {
                        break;
                    }
                    //  applyto.dropMessage(5, "Disabled..");
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.SOARING, 0);
                    applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                } else if (berserk > 0) {
                    if (applyto.isHidden()) {
                        break;
                    }
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.PYRAMID_PQ, 0);
                    applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                } else if (isBerserkFury() || berserk2 > 0) {
                    if (applyto.isHidden()) {
                        break;
                    }
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.BERSERK_FURY, 1);
                    applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                } else if (isDivineBody()) {
                    if (applyto.isHidden()) {
                        break;
                    }
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.DIVINE_BODY, 1);
                    applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                }
                break;
        }
        if (showEffect && !applyto.isHidden()) {
            applyto.getMap().broadcastMessage(applyto, EffectPacket.showBuffeffect(applyto.getId(), sourceid, 1, applyto.getLevel(), level), false);
        }
        if (isMechPassive()) {
            applyto.getClient().getSession().write(EffectPacket.showOwnBuffEffect(sourceid - 1000, 1, applyto.getLevel(), level, (byte) 1));
        }
        if (!isMonsterRiding() && !isMechDoor()) {
            applyto.cancelEffect(this, true, -1, localstatups);
        }
        // Broadcast effect to self
        if (normal && localstatups.size() > 0) {
            applyto.getClient().getSession().write(BuffPacket.giveBuff((skill ? sourceid : -sourceid), localDuration, maskedStatups == null ? localstatups : maskedStatups, this));
        }
        final long starttime = System.currentTimeMillis();
        final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime, localstatups);
        final ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, maskedDuration > 0 ? maskedDuration : localDuration);
        applyto.registerEffect(this, starttime, schedule, localstatups, false, localDuration, applyfrom.getId());
    }

    public static int parseMountInfo(final MapleCharacter player, final int skillid) {
        switch (skillid) {
            case 80001000:
            case 1004: // Monster riding
            case 11004: // Monster riding
            case 10001004:
            case 20001004:
            case 20011004:
            case 20021004:
                if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -118) != null && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -119) != null) {
                    return player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -118).getItemId();
                }
                return parseMountInfo_Pure(player, skillid);
            default:
                return GameConstants.getMountItem(skillid, player);
        }
    }

    public static int parseMountInfo_Pure(final MapleCharacter player, final int skillid) {
        switch (skillid) {
            case 80001000:
            case 1004: // Monster riding
            case 11004: // Monster riding
            case 10001004:
            case 20001004:
            case 20011004:
            case 20021004:
                if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18) != null && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -19) != null) {
                    return player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18).getItemId();
                }
                return 0;
            default:
                return GameConstants.getMountItem(skillid, player);
        }
    }

    private int calcHPChange(final MapleCharacter applyfrom, final boolean primary) {
        int hpchange = 0;
        if (hp != 0) {
            if (!skill) {
                if (primary) {
                    hpchange += alchemistModifyVal(applyfrom, hp, true);
                } else {
                    hpchange += hp;
                }
                if (applyfrom.hasDisease(MapleDisease.ZOMBIFY)) {
                    hpchange /= 2;
                }
            } else { // assumption: this is heal
                hpchange += makeHealHP(hp / 100.0, applyfrom.getStat().getTotalMagic(), 3, 5);
                if (applyfrom.hasDisease(MapleDisease.ZOMBIFY)) {
                    hpchange = -hpchange;
                }
            }
        }
        if (hpR != 0) {
            hpchange += (int) (applyfrom.getStat().getCurrentMaxHp() * hpR) / (applyfrom.hasDisease(MapleDisease.ZOMBIFY) ? 2 : 1);
        }
        // actually receivers probably never get any hp when it's not heal but whatever
        if (primary) {
            if (hpCon != 0) {
                hpchange -= hpCon;
            }
        }
        return hpchange;
    }

    private static int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
        return (int) ((Math.random() * ((int) (stat * upperfactor * rate) - (int) (stat * lowerfactor * rate) + 1)) + (int) (stat * lowerfactor * rate));
    }

    private int calcMPChange(final MapleCharacter applyfrom, final boolean primary) {
        int mpchange = 0;
        if (mp != 0) {
            if (primary) {
                mpchange += alchemistModifyVal(applyfrom, mp, false); // recovery up doesn't apply for mp
            } else {
                mpchange += mp;
            }
        }
        if (mpR != 0) {
            mpchange += (int) (applyfrom.getStat().getCurrentMaxMp(applyfrom.getJob()) * mpR);
        }
        if (GameConstants.isDemon(applyfrom.getJob())) {
            mpchange = 0;
        }
        if (primary) {
            if (mpCon != 0 && !GameConstants.isDemon(applyfrom.getJob())) {
                if (applyfrom.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                    mpchange = 0;
                } else {
                    mpchange -= (mpCon - (mpCon * applyfrom.getStat().mpconReduce / 100)) * (applyfrom.getStat().mpconPercent / 100.0);
                }
            } else if (forceCon != 0 && GameConstants.isDemon(applyfrom.getJob())) {
                if (applyfrom.getBuffedValue(MapleBuffStat.BOUNDLESS_RAGE) != null) {
                    mpchange = 0;
                } else {
                    mpchange -= forceCon;
                }
            }
        }

        return mpchange;
    }

    public final int alchemistModifyVal(final MapleCharacter chr, final int val, final boolean withX) {
        if (!skill) { // RecoveryUP only used for hp items and skills
            return (val * (100 + (withX ? chr.getStat().RecoveryUP : chr.getStat().BuffUP)) / 100);
        }
        return (val * (100 + (withX ? chr.getStat().RecoveryUP : (chr.getStat().BuffUP_Skill + (getSummonMovementType() == null ? 0 : chr.getStat().BuffUP_Summon)))) / 100);
    }

    public final void setSourceId(final int newid) {
        sourceid = newid;
    }

    public final boolean isGmBuff() {
        switch (sourceid) {
            case 10001075: //女皇的祈禱
            case 9001000: // GM dispel
            case 9001001: // GM haste
            case 9001002: // GM Holy Symbol
            case 9001003: // GM Bless
            case 9001005: // GM resurrection
            case 9001008: // GM Hyper body

            case 9101000:
            case 9101001:
            case 9101002:
            case 9101004:
            case 9101003:
            case 9101005:
            case 9101008:
                return true;
            default:
                return GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1005;
        }
    }

    public final boolean isInflation() {
        return inflation > 0;
    }

    public final int getInflation() {
        return inflation;
    }

    public final boolean isEnergyCharge() {
        return skill && (sourceid == 5110001 || sourceid == 15100004); //蓄能激發
    }

    /*
    給與怪物BUFF狀態相關技能
    */
    private boolean isMonsterBuff() {
        switch (sourceid) {
            case 1111007: //防禦消除
            case 1201006: //降魔咒
            case 1211009: //魔防消除
            case 1311007: //魔防消除
            case 2101003: //緩速術
            case 2111004: //魔法封印
            case 2201003: //緩速術
            case 2211004: //魔法封印
            case 2311005: //喚化術
            case 4111003: //影網術
            case 4121015: //絕對領域
            case 4321002: //閃光彈
            case 5011002: //迴避砲擊
            case 11111008: //魔防消除
            case 12101001: //緩速術
            case 12111002: //魔法封印
            case 14111001: //影網術
            case 21101003: //強化連擊
            case 22161002: //鬼神詛咒
            case 32120000: //進階黑色繩索
            case 32120001: //進階黃色繩索
            case 35111005: //加速器 : EX-7
            case 51111005: //魔防消除
            case 90001002:
            case 90001003:
            case 90001004:
            case 90001005:
            case 90001006:
                return skill;
        }
        return false;
    }

    public final void setPartyBuff(boolean pb) {
        this.partyBuff = pb;
    }

    private boolean isPartyBuff() {
        if (lt == null || rb == null || !partyBuff) {
            return isSoulStone();
        }
        switch (sourceid) {
            case 1211004: //烈焰之劍
            case 1211006: //寒冰之劍
            case 1211008: //雷鳴之劍
            case 1221004: //聖靈之劍
            case 4341002: //絕殺刃
            case 11111007: //閃耀激發
            case 35121005: //合金盔甲: 導彈罐
                return false;
        }
        if (GameConstants.isNoDelaySkill(sourceid)) {
            return false;
        }
        return true;
    }

    public final boolean isHeal() { //群體治癒
        return skill && (sourceid == 2301002 || sourceid == 9101000 || sourceid == 9001000);
    }

    public final boolean isResurrection() { //復甦之光
        return skill && (sourceid == 9001005 || sourceid == 9101005 || sourceid == 2321006);
    }

    public final boolean isTimeLeap() {
        return skill && sourceid == 5121010; //時間置換
    }

    public final short getHp() {
        return hp;
    }

    public final short getMp() {
        return mp;
    }

    public final double getHpR() {
        return hpR;
    }

    public final double getMpR() {
        return mpR;
    }

    public final byte getMastery() {
        return mastery;
    }

    public final short getWatk() {
        return watk;
    }

    public final short getMatk() {
        return matk;
    }

    public final short getWdef() {
        return wdef;
    }

    public final short getMdef() {
        return mdef;
    }

    public final short getAcc() {
        return acc;
    }

    public final short getAvoid() {
        return avoid;
    }

    public final short getHands() {
        return hands;
    }

    public final short getSpeed() {
        return speed;
    }

    public final short getJump() {
        return jump;
    }

    public final short getPassiveSpeed() {
        return psdSpeed;
    }

    public final short getPassiveJump() {
        return psdJump;
    }

    public final int getDuration() {
        return duration;
    }

    public final int getSubTime() {
        return subTime;
    }

    public final boolean isOverTime() {
        return overTime;
    }

    public final Map<MapleBuffStat, Integer> getStatups() {
        return statups;
    }

    public final boolean sameSource(final MapleStatEffect effect) {
        boolean sameSrc = this.sourceid == effect.sourceid;
        switch (this.sourceid) { // All these are passive skills, will have to cast the normal ones.
            case 32120000: //進階黑色繩索
                sameSrc = effect.sourceid == 32001003; //黑色繩索
                break;
            case 32110000: //進階藍色繩索
                sameSrc = effect.sourceid == 32111012; //藍色繩索
                break;
            case 32120001: //進階黃色繩索
                sameSrc = effect.sourceid == 32101003; //黃色繩索
                break;
            case 35120000: //合金盔甲終極
                sameSrc = effect.sourceid == 35001002; //合金盔甲: 原型
                break;
            //case 35121013: //合金盔甲: 重機槍
            //    sameSrc = effect.sourceid == 35111004;
            //    break;
        }
        return effect != null && sameSrc && this.skill == effect.skill;
    }

    public final int getCr() {
        return cr;
    }

    public final int getT() {
        return t;
    }

    public final int getU() {
        return u;
    }

    public final int getV() {
        return v;
    }

    public final int getW() {
        return w;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getZ() {
        return z;
    }

    public final short getDamage() {
        return damage;
    }

    public final short getPVPDamage() {
        return PVPdamage;
    }

    public final byte getAttackCount() {
        return attackCount;
    }

    public final byte getBulletCount() {
        return bulletCount;
    }

    public final int getBulletConsume() {
        return bulletConsume;
    }

    public final byte getMobCount() {
        return mobCount;
    }

    public final int getMoneyCon() {
        return moneyCon;
    }

    public final int getCooldown(final MapleCharacter chra) {
        return Math.max(0, (cooldown - chra.getStat().reduceCooltime));
    }

    public final Map<MonsterStatus, Integer> getMonsterStati() {
        return monsterStatus;
    }

    public final int getBerserk() {
        return berserk;
    }

    public final boolean isHide() {
        return skill && (sourceid == 9101002);
    }

    public final boolean isRecovery() { //合金盔甲: 導彈罐
        return skill && (sourceid == 1001 || sourceid == 11001 || sourceid == 10001001 || sourceid == 20001001 || sourceid == 20011001 || sourceid == 20021001 || sourceid == 35121005);
    }

    public final boolean isBerserk() { //黑暗力量
        return skill && sourceid == 1320006;
    }

    public final boolean isBeholder() { //暗之靈魂
        return skill && sourceid == 1321007;
    }

    public final boolean isInfinity() { //魔力無限
        return skill && (sourceid == 2121004 || sourceid == 2221004 || sourceid == 2321004);
    }

    public final boolean isMonsterRiding_() {
        return skill && (sourceid == 1004 || sourceid == 11004 || sourceid == 10001004 || sourceid == 20001004 || sourceid == 20011004 || sourceid == 20021004 || sourceid == 80001000);
    }

    public final boolean isMonsterRiding() {
        return skill && (isMonsterRiding_() || GameConstants.getMountItem(sourceid, null) != 0);
    }

    public final boolean isMagicDoor() { //時空門
        return skill && (sourceid == 2311002 || sourceid % 10000 == 8001);
    }

    public final boolean isMesoGuard() { //楓幣護盾
        return skill && sourceid == 4201011;
    }

    public final boolean isMechDoor() { //開放通道 : GX-9
        return skill && sourceid == 35101005;
    }

    public final boolean isComboRecharge() { //鬥氣填充
        return skill && sourceid == 21111009;
    }

    public final boolean isDragonBlink() { //龍之氣息
        return skill && sourceid == 22141004;
    }

    public final boolean isCharge() {
        switch (sourceid) {
            case 1211008: //雷鳴之劍
            case 11111007: //閃耀激發
                return skill;
        }
        return false;
    }

    public final boolean isPoison() {
        return dot > 0 && dotTime > 0;
    }

    private boolean isMist() {
        switch (sourceid) {
            case 1076: //奧茲的火牢術屏障
            case 2111003: //致命毒霧
            case 4221006: //煙幕彈
            case 12111005: //火牢術屏障
            case 14111006: //毒炸彈
            case 22161003: //聖療之光
            case 32121006: //魔法屏障
                return true;
        }
        return false;
    }

    private boolean isSpiritClaw() {
        return sourceid == 4111009 || sourceid == 14111007 || sourceid == 5201008; //無形鏢 無形鏢 無形彈藥
    }

    private boolean isDispel() { //淨化
        return skill && (sourceid == 2311001 || sourceid == 9001000 || sourceid == 9101000);
    }

    private boolean isHeroWill() { //楓葉淨化
        switch (sourceid) {
            case 1121011:
            case 1221012:
            case 1321010:
            case 2121008:
            case 2221008:
            case 2321009:
            case 3121009:
            case 3221008:
            case 4121009:
            case 4221008:
            case 4341008:
            case 5121008:
            case 5221010:
            case 5321006:
            case 21121008:
            case 22171004: //楓葉淨化
            case 23121008:
            case 24121009:
            case 32121008:
            case 33121008:
            case 35121008:
                return skill;
        }
        return false;
    }

    public final boolean isAranCombo() {
        return sourceid == 21000000; //矛之鬥氣
    }

    public final boolean isCombo() {
        switch (sourceid) {
            case 1111002: //鬥氣集中
            case 11111001: //鬥氣集中
                return skill;
        }
        return false;
    }

    public final boolean isPirateMorph() {
        switch (sourceid) {
            case 13111005: //阿爾法
                return skill;
        }
        return false;
    }

    public final boolean isMorph() {
        return morphId > 0;
    }

    public final int getMorph() {
        switch (sourceid) {
            case 13111005: //阿爾法
                return 1003;
        }
        return morphId;
    }

    public final boolean isDivineBody() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1010;
    }

    public final boolean isDivineShield() {
        switch (sourceid) {
            case 1220013: //祝福護甲
                return skill;
        }
        return false;
    }

    public final boolean isBerserkFury() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1011;
    }

    public final int getMorph(final MapleCharacter chr) {
        final int morph = getMorph();
        switch (morph) {
            case 1000:
            case 1001:
            case 1003:
                return morph + (chr.getGender() == 1 ? 100 : 0);
        }
        return morph;
    }

    public final byte getLevel() {
        return level;
    }

    public final SummonMovementType getSummonMovementType() {
        if (!skill) {
            return null;
        }
        switch (sourceid) {
            case 3111002: //替身術
            case 3120012: //精銳替身術
            case 3211002: //替身術
            case 3220012: //精銳替身術
            case 4111007: //黑暗殺
            case 4211007: //黑暗殺
            case 4341006: //幻影替身
            case 5211014: //砲台章魚王
            case 5321003: //磁錨
            case 5711001: //破城砲
            case 13111004: //替身術
            case 14111010: //黑暗殺
            case 33101008: //地雷(hidden 自動爆炸)
            case 33111003: //瘋狂陷阱
            case 35111002: //磁場
            case 35111005: //加速器 : EX-7
            case 35111011: //治療機器人 : H-LX
            case 35121003: //戰鬥機器 : 巨人錘
            case 35121009: //機器人工廠 : RM1
            case 35121010: //擴音器 : AF-11
            case 35121011: //機器人工廠 : 機器人
                return SummonMovementType.STATIONARY;
            case 3101007: //銀鷹召喚
            case 3111005: //召喚鳳凰
            case 3201007: //金鷹召喚
            case 3211005: //召喚銀隼
            case 23111008: //元素騎士
            case 23111009: //元素騎士
            case 23111010: //元素騎士
            case 33111005: //銀鷹召喚
                return SummonMovementType.CIRCLE_FOLLOW; //跟隨並且隨機移動打怪
            case 5211002: //海鷗突擊隊(已經過時的技能)
                return SummonMovementType.CIRCLE_STATIONARY;
            case 5211011: //召喚船員
            case 5211015: //召喚船員
            case 5211016: //召喚船員
            case 32111006: //甦醒
                return SummonMovementType.WALK_STATIONARY;
            case 1321007: //暗之靈魂
            case 2121005: //召喚火魔
            case 2221005: //召喚冰魔
            case 2321003: //聖龍精通
            case 5321004: //雙胞胎猴子
            case 11001004: //光之精靈
            case 12001004: //火之精靈
            case 12111004: //召喚火魔
            case 13001004: //風之精靈
            case 14001005: //暗之精靈
            case 15001004: //雷之精靈
            case 35111001: //賽特拉特
            case 35111009: //賽特拉特1
            case 35111010: //賽特拉特2
                return SummonMovementType.FOLLOW; //跟隨移動
        }
        if (isAngel()) {
            return SummonMovementType.FOLLOW;
        }
        return null;
    }

    public final boolean isAngel() {
        return GameConstants.isAngel(sourceid);
    }

    public final boolean isSkill() {
        return skill;
    }

    public final int getSourceId() {
        return sourceid;
    }

    public final boolean isIceKnight() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1105;
    }

    public final boolean isSoaring() {
        return isSoaring_Normal() || isSoaring_Mount();
    }

    public final boolean isSoaring_Normal() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1026;
    }

    public final boolean isSoaring_Mount() {
        return skill && ((GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1142) || sourceid == 80001089);
    }

    public final boolean isMistEruption() {
        switch (sourceid) {
            case 2121003: //地獄爆發
                return skill;
        }
        return false;
    }

    public final boolean isShadow() {
        switch (sourceid) {
            case 4111002: //影分身
            case 4211008: //影分身
            case 14111000: //影分身
                return skill;
        }
        return false;
    }

    public final boolean isMI() {
        switch (sourceid) {
            case 4331002: //替身術
                return skill;
        }
        return false;
    }

    public final boolean isMechPassive() {
        switch (sourceid) {
            //case 35121005: //合金盔甲: 導彈罐
            case 35121013: //合金盔甲: 重機槍
                return true;
        }
        return false;
    }

    /**
     * @return true if the effect should happen based on it's probablity, false otherwise
     */
    public final boolean makeChanceResult() {
        return prop >= 100 || Randomizer.nextInt(100) < prop;
    }

    public final short getProb() {
        return prop;
    }

    public final short getIgnoreMob() {
        return ignoreMob;
    }

    public final int getEnhancedHP() {
        return ehp;
    }

    public final int getEnhancedMP() {
        return emp;
    }

    public final int getEnhancedWatk() {
        return ewatk;
    }

    public final int getEnhancedWdef() {
        return ewdef;
    }

    public final int getEnhancedMdef() {
        return emdef;
    }

    public final short getDOT() {
        return dot;
    }

    public final short getDOTTime() {
        return dotTime;
    }

    public final short getCriticalMax() {
        return criticaldamageMax;
    }

    public final short getCriticalMin() {
        return criticaldamageMin;
    }

    public final short getASRRate() {
        return asrR;
    }

    public final short getTERRate() {
        return terR;
    }

    public final short getDAMRate() {
        return damR;
    }

    public final short getMesoRate() {
        return mesoR;
    }

    public final int getEXP() {
        return exp;
    }

    public final short getAttackX() {
        return padX;
    }

    public final short getMagicX() {
        return madX;
    }

    public final int getPercentHP() {
        return mhpR;
    }

    public final int getPercentMP() {
        return mmpR;
    }

    public final int getConsume() {
        return consumeOnPickup;
    }

    public final int getSelfDestruction() {
        return selfDestruction;
    }

    public final int getCharColor() {
        return charColor;
    }

    public final List<Integer> getPetsCanConsume() {
        return petsCanConsume;
    }

    public final boolean isReturnScroll() {
        return skill && (sourceid == 80001040 || sourceid == 20021110 || sourceid == 20031203); //精靈的祝福, 水晶花園傳送
    }

    public final boolean isMechChange() {
        switch (sourceid) {
            case 35001001: //火焰發射
            case 35101009: //強化的火焰發射
            case 35111004: //合金盔甲: 重機槍
            case 35121005: //合金盔甲: 導彈罐
            case 35121013: //合金盔甲: 重機槍
                return skill;
        }
        return false;
    }

    public final int getRange() {
        return range;
    }

    public final short getER() {
        return er;
    }

    public final int getPrice() {
        return price;
    }

    public final int getExtendPrice() {
        return extendPrice;
    }

    public final byte getPeriod() {
        return period;
    }

    public final byte getReqGuildLevel() {
        return reqGuildLevel;
    }

    public final byte getEXPRate() {
        return expR;
    }

    public final short getLifeID() {
        return lifeId;
    }

    public final short getUseLevel() {
        return useLevel;
    }

    public final byte getSlotCount() {
        return slotCount;
    }

    public final short getStr() {
        return str;
    }

    public final short getStrX() {
        return strX;
    }

    public final short getDex() {
        return dex;
    }

    public final short getDexX() {
        return dexX;
    }

    public final short getInt() {
        return int_;
    }

    public final short getIntX() {
        return intX;
    }

    public final short getLuk() {
        return luk;
    }

    public final short getLukX() {
        return lukX;
    }

    public final short getMPConReduce() {
        return mpConReduce;
    }

    public final short getIndieMHp() {
        return indieMhp;
    }

    public final short getIndieMMp() {
        return indieMmp;
    }

    public final short getIndieAllStat() {
        return indieAllStat;
    }

    public final byte getType() {
        return type;
    }

    public int getBossDamage() {
        return bdR;
    }

    public int getInterval() {
        return interval;
    }

    public ArrayList<Pair<Integer, Integer>> getAvailableMaps() {
        return availableMap;
    }

    public short getWDEFRate() {
        return pddR;
    }

    public short getMDEFRate() {
        return mddR;
    }

    public static class CancelEffectAction implements Runnable {

        private final MapleStatEffect effect;
        private final WeakReference<MapleCharacter> target;
        private final long startTime;
        private final Map<MapleBuffStat, Integer> statup;

        public CancelEffectAction(final MapleCharacter target, final MapleStatEffect effect, final long startTime, final Map<MapleBuffStat, Integer> statup) {
            this.effect = effect;
            this.target = new WeakReference<>(target);
            this.startTime = startTime;
            this.statup = statup;
        }

        @Override
        public void run() {
            final MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.cancelEffect(effect, false, startTime, statup);
            }
        }
    }
}
