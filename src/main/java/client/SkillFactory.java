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
package client;

import client.status.MonsterStatus;
import constants.GameConstants;

import java.awt.Point;
import java.io.File;
import java.util.*;

import provider.*;
import server.Randomizer;
import tools.StringUtil;
import tools.Triple;

public class SkillFactory {

    private static final Map<Integer, Skill> skills = new HashMap<>();
    private static final Map<String, Integer> delays = new HashMap<>();
    private static final Map<Integer, CraftingEntry> crafts = new HashMap<>();
    private static final Map<Integer, FamiliarEntry> familiars = new HashMap<>();
    private static final Map<Integer, List<Integer>> skillsByJob = new HashMap<>();
    private static final Map<Integer, SummonSkillEntry> SummonSkillInformation = new HashMap<>();

    public static void load() {
        final MapleData delayData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Character.wz")).getData("00002000.img");
        final MapleData stringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")).getData("Skill.img");
        final MapleDataProvider datasource = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Skill.wz"));
        final MapleDataDirectoryEntry root = datasource.getRoot();
        int del = 0; //buster is 67 but its the 57th one!
        for (MapleData delay : delayData) {
            if (!delay.getName().equals("info")) {
                delays.put(delay.getName(), del);
                del++;
            }
        }

        int skillid;
        MapleData summon_data;
        SummonSkillEntry sse;

        for (MapleDataFileEntry topDir : root.getFiles()) { // Loop thru jobs
            if (topDir.getName().length() <= 8) {
                for (MapleData data : datasource.getData(topDir.getName())) { // Loop thru each jobs
                    if (data.getName().equals("skill")) {
                        for (MapleData data2 : data) { // Loop thru each jobs
                            if (data2 != null) {
                                skillid = Integer.parseInt(data2.getName());
                                Skill skil = Skill.loadFromData(skillid, data2, delayData);
                                List<Integer> job = skillsByJob.get(skillid / 10000);
                                if (job == null) {
                                    job = new ArrayList<>();
                                    skillsByJob.put(skillid / 10000, job);
                                }
                                job.add(skillid);
                                skil.setName(getName(skillid, stringData));
                                skills.put(skillid, skil);

                                summon_data = data2.getChildByPath("summon/attack1/info");
                                if (summon_data != null) { //地雷(hidden 自動爆炸)
                                    sse = new SummonSkillEntry();
                                    sse.type = (byte) MapleDataTool.getInt("type", summon_data, 0);
                                    sse.mobCount = (byte) (skillid == 33101008 ? 3 : MapleDataTool.getInt("mobCount", summon_data, 1));
                                    sse.attackCount = (byte) MapleDataTool.getInt("attackCount", summon_data, 1);
                                    if (summon_data.getChildByPath("range/lt") != null) {
                                        final MapleData ltd = summon_data.getChildByPath("range/lt");
                                        sse.lt = (Point) ltd.getData();
                                        sse.rb = (Point) summon_data.getChildByPath("range/rb").getData();
                                    } else {
                                        sse.lt = new Point(-100, -100);
                                        sse.rb = new Point(100, 100);
                                    }
                                    //sse.range = (short) MapleDataTool.getInt("range/r", summon_data, 0);
                                    sse.delay = MapleDataTool.getInt("effectAfter", summon_data, 0) + MapleDataTool.getInt("attackAfter", summon_data, 0);
                                    for (MapleData effect : summon_data) {
                                        if (effect.getChildren().size() > 0) {
                                            for (final MapleData effectEntry : effect) {
                                                sse.delay += MapleDataTool.getIntConvert("delay", effectEntry, 0);
                                            }
                                        }
                                    }
                                    for (MapleData effect : data2.getChildByPath("summon/attack1")) {
                                        sse.delay += MapleDataTool.getIntConvert("delay", effect, 0);
                                    }
                                    SummonSkillInformation.put(skillid, sse);
                                }
                            }
                        }
                    }
                }
            } else if (topDir.getName().startsWith("Familiar")) {
                for (MapleData data : datasource.getData(topDir.getName())) {
                    skillid = Integer.parseInt(data.getName());
                    FamiliarEntry skil = new FamiliarEntry();
                    skil.prop = (byte) MapleDataTool.getInt("prop", data, 0);
                    skil.time = (byte) MapleDataTool.getInt("time", data, 0);
                    skil.attackCount = (byte) MapleDataTool.getInt("attackCount", data, 1);
                    skil.targetCount = (byte) MapleDataTool.getInt("targetCount", data, 1);
                    skil.speed = (byte) MapleDataTool.getInt("speed", data, 1);
                    skil.knockback = MapleDataTool.getInt("knockback", data, 0) > 0 || MapleDataTool.getInt("attract", data, 0) > 0;
                    if (data.getChildByPath("lt") != null) {
                        skil.lt = (Point) data.getChildByPath("lt").getData();
                        skil.rb = (Point) data.getChildByPath("rb").getData();
                    }
                    if (MapleDataTool.getInt("stun", data, 0) > 0) {
                        skil.status.add(MonsterStatus.STUN);
                    }
                    //if (MapleDataTool.getInt("poison", data, 0) > 0) {
                    //	status.add(MonsterStatus.POISON);
                    //}
                    if (MapleDataTool.getInt("slow", data, 0) > 0) {
                        skil.status.add(MonsterStatus.SPEED);
                    }
                    familiars.put(skillid, skil);
                }
            } else if (topDir.getName().startsWith("Recipe")) {
                for (MapleData data : datasource.getData(topDir.getName())) {
                    skillid = Integer.parseInt(data.getName());
                    CraftingEntry skil = new CraftingEntry(skillid, (byte) MapleDataTool.getInt("incFatigability", data, 0), (byte) MapleDataTool.getInt("reqSkillLevel", data, 0), (byte) MapleDataTool.getInt("incSkillProficiency", data, 0), MapleDataTool.getInt("needOpenItem", data, 0) > 0, MapleDataTool.getInt("period", data, 0));
                    for (MapleData d : data.getChildByPath("target")) {
                        skil.targetItems.add(new Triple<>(MapleDataTool.getInt("item", d, 0), MapleDataTool.getInt("count", d, 0), MapleDataTool.getInt("probWeight", d, 0)));
                    }
                    for (MapleData d : data.getChildByPath("recipe")) {
                        skil.reqItems.put(MapleDataTool.getInt("item", d, 0), MapleDataTool.getInt("count", d, 0));
                    }
                    crafts.put(skillid, skil);
                }
            }
        }
    }

    public static List<Integer> getSkillsByJob(final int jobId) {
        return skillsByJob.get(jobId);
    }

    public static String getSkillName(final int id) {
        Skill skil = getSkill(id);
        if (skil != null) {
            return skil.getName();
        }
        return null;
    }

    public static Integer getDelay(final String id) {
        if (Delay.fromString(id) != null) {
            return Delay.fromString(id).i;
        }
        return delays.get(id);
    }

    private static String getName(final int id, final MapleData stringData) {
        String strId = Integer.toString(id);
        strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
        MapleData skillroot = stringData.getChildByPath(strId);
        if (skillroot != null) {
            return MapleDataTool.getString(skillroot.getChildByPath("name"), "");
        }
        return "";
    }

    public static SummonSkillEntry getSummonData(final int skillid) {
        return SummonSkillInformation.get(skillid);
    }

    public static Collection<Skill> getAllSkills() {
        return skills.values();
    }

    public static Skill getSkill(final int id) {
        if (!skills.isEmpty()) {
            if (id >= 92000000 && crafts.containsKey(Integer.valueOf(id))) { //92000000
                return crafts.get(Integer.valueOf(id));
            }
            return skills.get(Integer.valueOf(id));
        }

        return null;
    }

    public static long getDefaultSExpiry(final Skill skill) {
        if (skill == null) {
            return -1;
        }
        return (skill.isTimeLimited() ? (System.currentTimeMillis() + (long) (30L * 24L * 60L * 60L * 1000L)) : -1);
    }

    public static CraftingEntry getCraft(final int id) {
        if (!crafts.isEmpty()) {
            return crafts.get(Integer.valueOf(id));
        }

        return null;
    }

    public static FamiliarEntry getFamiliar(final int id) {
        if (!familiars.isEmpty()) {
            return familiars.get(Integer.valueOf(id));
        }

        return null;
    }

    public static class CraftingEntry extends Skill {
        //reqSkillProficiency -> always seems to be 0

        public boolean needOpenItem;
        public int period;
        public byte incFatigability, reqSkillLevel, incSkillProficiency;
        public List<Triple<Integer, Integer, Integer>> targetItems = new ArrayList<>(); // itemId / amount / probability
        public Map<Integer, Integer> reqItems = new HashMap<>(); // itemId / amount

        public CraftingEntry(int id, byte incFatigability, byte reqSkillLevel, byte incSkillProficiency, boolean needOpenItem, int period) {
            super(id);
            this.incFatigability = incFatigability;
            this.reqSkillLevel = reqSkillLevel;
            this.incSkillProficiency = incSkillProficiency;
            this.needOpenItem = needOpenItem;
            this.period = period;
        }
    }

    public static class FamiliarEntry {

        public byte prop, time, attackCount, targetCount, speed;
        public Point lt, rb;
        public boolean knockback;
        public EnumSet<MonsterStatus> status = EnumSet.noneOf(MonsterStatus.class);

        public final boolean makeChanceResult() {
            return prop >= 100 || Randomizer.nextInt(100) < prop;
        }
    }

    public static enum Delay {

        walk1(0),
        walk2(1),
        stand1(2),
        stand2(3),
        alert(4),
        swingO1(5),
        swingO2(6),
        swingO3(7),
        swingOF(8),
        swingT1(9),
        swingT2(10),
        swingT3(11),
        swingTF(12),
        swingP1(13),
        swingP2(14),
        swingPF(15),
        bluntSmash(16),
        stabO1(16),
        stabO2(17),
        stabOF(18),
        stabT1(19),
        stabT2(20),
        stabTF(21),
        swingD1(22),
        swingD2(23),
        stabD1(24),
        swingDb1(25),
        swingDb2(26),
        swingC1(27),
        swingC2(28),
        tripleBlow(29),
        quadBlow(30),
        deathBlow(31),
        finishBlow(32),
        finishAttack(33),
        finishAttack_link(34),
        finishAttack_link2(34),
        shoot1(35),
        shoot2(36),
        shootF(37),
        shootDb2(40),
        dualVulcanPrep(40),
        shotC1(41),
        dash(43),
        dash2(44),
        proneStab(47),
        prone(48),
        heal(49),
        fly(50),
        jump(51),
        sit(52),
        rope(53),
        dead(54),
        ladder(55),
        rain(56),
        alert0(62),
        alert1(63),
        alert2(64),
        alert3(66),
        alert4(67),
        alert5(68),
        alert6(69),
        alert7(70),
        ladder2(71),
        rope2(72),
        shoot6(73),
        magic1(74),
        magic2(75),
        magic3(76),
        magic5(77),
        magic6(78),
        explosion(78),
        burster1(79),
        burster2(80),
        savage(81),
        avenger(82),
        assaulter(83),
        prone2(84),
        assassination(85),
        assassinationS(86),
        tornadoDash(89),
        tornadoDashStop(89),
        tornadoRush(89),
        rush(90),
        rush2(91),
        brandish1(92),
        brandish2(93),
        braveSlash(94),
        braveslash1(94),
        braveslash2(95),
        braveslash3(96),
        braveslash4(97),
        darkImpale(98),
        sanctuary(99),
        meteor(100),
        paralyze(101),
        blizzard(102),
        genesis(103),
        blast(106),
        smokeshell(107),
        showdown(108),
        ninjastorm(109),
        chainlightning(110),
        holyshield(111),
        resurrection(112),
        somersault(113),
        straight(114),
        eburster(115),
        backspin(116),
        eorb(117),
        screw(118),
        doubleupper(119),
        dragonstrike(120),
        doublefire(121),
        triplefire(122),
        fake(123),
        airstrike(124),
        edrain(125),
        octopus(126),
        backstep(127),
        shot(128),
        fireburner(130),
        coolingeffect(131),
        fist(133),
        rapidfire(GameConstants.GMS ? 0x7F : 0x6E),
        timeleap(134),
        homing(135),
        ghostwalk(136),
        ghoststand(137),
        ghostjump(138),
        ghostproneStab(139),
        ghostladder(140),
        ghostrope(141),
        ghostfly(142),
        ghostsit(143),
        cannon(144),
        torpedo(145),
        darksight(146),
        bamboo(147),
        pyramid(148),
        wave(149),
        blade(150),
        souldriver(151),
        firestrike(152),
        flamegear(153),
        stormbreak(154),
        vampire(155),
        swingT2PoleArm(157),
        swingP1PoleArm(158),
        swingP2PoleArm(159),
        doubleSwing(160),
        tripleSwing(161),
        fullSwingDouble(162),
        fullSwingTriple(163),
        overSwingDouble(164),
        overSwingTriple(165),
        rollingSpin(166),
        comboSmash(167),
        comboFenrir(168),
        comboTempest(169),
        finalCharge(170),
        finalBlow(172),
        finalToss(173),
        magicmissile(174),
        lightningBolt(175),
        lightingBolt(175),
        dragonBreathe(176),
        breathe_prepare(177),
        dragonIceBreathe(178),
        icebreathe_prepare(179),
        blaze(180),
        fireCircle(181),
        illusion(182),
        magicFlare(183),
        elementalReset(184),
        magicRegistance(185),
        magicBooster(186),
        magicShield(187),
        recoveryAura(188),
        flameWheel(189),
        killingWing(190),
        OnixBlessing(191),
        Earthquake(192),
        soulStone(193),
        dragonThrust(194),
        ghostLettering(195),
        darkFog(196),
        slow(197),
        mapleHero(198),
        Awakening(199),
        flyingAssaulter(200),
        tripleStab(201),
        fatalBlow(202),
        slashStorm1(203),
        slashStorm2(204),
        bloodyStorm(205),
        flashBang(206),
        upperStab(207),
        bladeFury(208),
        chainPull(210),
        chainAttack(210),
        owlDead(211),
        monsterBombPrepare(213),
        monsterBombThrow(213),
        finalCut(214),
        finalCutPrepare(214),
        fly2(217),
        fly2Move(218),
        fly2Skill(219),
        knockback(220),
        rbooster_pre(224),
        rbooster(224),
        rbooster_after(224),
        crossRoad(227),
        nemesis(228),
        tank(235),
        tank_laser(239),
        siege_pre(241),
        tank_siegepre(242),
        sonicBoom(244),
        darkLightning(246),
        darkChain(247),
        cyclone_pre(0),
        cyclone(0),
        glacialchain(248),
        flamethrower(252),
        flamethrower_pre(252),
        flamethrower2(253),
        flamethrower_pre2(253),
        gatlingshot(258),
        gatlingshot2(259),
        drillrush(260),
        earthslug(261),
        rpunch(262),
        clawCut(263),
        swallow(266),
        swallow_attack(266),
        swallow_loop(266),
        flashRain(274),
        OnixProtection(285),
        OnixWill(286),
        phantomBlow(287),
        comboJudgement(288),
        arrowRain(289),
        arrowEruption(290),
        iceStrike(291),
        swingT2Giant(294),
        cannonJump(296),
        swiftShot(297),
        giganticBackstep(299),
        mistEruption(300),
        cannonSmash(301),
        cannonSlam(302),
        flamesplash(303),
        rushBoom(305),
        noiseWave(306),
        superCannon(310),
        jShot(312),
        demonSlasher(313),
        bombExplosion(314),
        cannonSpike(315),
        speedDualShot(316),
        strikeDual(317),
        crossPiercing(319),
        piercing(321),
        elfTornado(322),
        immolation(323),
        multiSniping(325),
        edgeSpiral(326),
        windEffect(327),
        elfrush(328),
        elfrush2(329),
        elfrush_final(330),
        demolitionElf(332),
        dealingRush(333),
        deathDraw(324),
        healingAttack(325),
        maxForce0(335),
        maxForce1(336),
        maxForce2(337),
        maxForce3(338),
        darkThrust(341),
        demonTrace(342),
        darkSpin(347),
        bluntSmashLoop(353),
        demonicBreathe(356),
        reverseGravity(369),
        reverseGravity2(370),
        devilishPower(363),
        demonImpact(364),
        demonbind(365),
        provoc(366),
        rollingElf(351),
        shurikenBurst(375),
        tripleThrow(376),
        quadrupleThrow(377),
        windTalisman(378),
        suddenRaid(379),
        shadeSplit(380),
        edgeCarnival(381),
        HTultimateDrive(383),
        HTtempest(386),
        doubleBarrel(388),
        bulletSmash(390),
        tornadoUpper(391),
        HTtwilightFinish(394),
        HTdoublePiercing(395),
        HTcallOfFate(396),
        HTknightEngaging(397),
        HTcoatOfArms(398),
        fusillade(400),
        headShot(401),
        HTphantomCharge(402),
        HTphantomCharge2(403),
        fistEnrage(405),
        doubleSpiral(407),
        iceAttack1(410),
        iceAttack2(411),
        iceSmash(412),
        iceTempest(413),
        iceChop(414),
        icePanic(415),
        iceDoubleJump(416),
        shockwave(427),
        demolition(428),
        snatch(429),
        windspear(430),
        windshot(431),
        bulletSmash2(451);

        public int i;

        private Delay(int i) {
            this.i = i;
        }

        public static Delay fromString(String s) {
            for (Delay b : Delay.values()) {
                if (b.name().equalsIgnoreCase(s)) {
                    return b;
                }
            }
            return null;
        }
    }
}
