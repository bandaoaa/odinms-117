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
package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import constants.GameConstants;

import java.awt.Point;

import server.MapleStatEffect;
import tools.packet.CField.SummonPacket;

public class MapleSummon extends AnimatedMapleMapObject {

    private final int ownerid, skillLevel, ownerLevel, skill;
    private MapleMap map; //required for instanceMaps
    private short hp;
    private boolean changedMap = false;
    private SummonMovementType movementType;
    // Since player can have more than 1 summon [Pirate] 
    // Let's put it here instead of cheat tracker
    private int lastSummonTickCount;
    private byte Summon_tickResetCount;
    private long Server_ClientSummonTickDiff;
    private long lastAttackTime;
    private long SpawnTime;

    public MapleSummon(final MapleCharacter owner, final MapleStatEffect skill, final Point pos, final SummonMovementType movementType) {
        this(owner, skill.getSourceId(), skill.getLevel(), pos, movementType);
    }

    public MapleSummon(final MapleCharacter owner, final int sourceid, final int level, final Point pos, final SummonMovementType movementType) {
        super();
        this.ownerid = owner.getId();
        this.ownerLevel = owner.getLevel();
        this.skill = sourceid;
        this.map = owner.getMap();
        this.skillLevel = level;
        this.movementType = movementType;
        setPosition(pos);

        if (!isPuppet()) { // Safe up 12 bytes of data, since puppet doesn't attack.
            lastSummonTickCount = 0;
            Summon_tickResetCount = 0;
            Server_ClientSummonTickDiff = 0;
            lastAttackTime = 0;
        }
    }

    @Override
    public final void sendSpawnData(final MapleClient client) {
    }

    @Override
    public final void sendDestroyData(final MapleClient client) {
        client.getSession().write(SummonPacket.removeSummon(this, false));
    }

    public final void updateMap(final MapleMap map) {
        this.map = map;
    }

    public final MapleCharacter getOwner() {
        return map.getCharacterById(ownerid);
    }

    public final int getOwnerId() {
        return ownerid;
    }

    public final int getOwnerLevel() {
        return ownerLevel;
    }

    public final int getSkill() {
        return skill;
    }

    public final short getHP() {
        return hp;
    }

    public final void addHP(final short delta) {
        this.hp += delta;
    }

    public final SummonMovementType getMovementType() {
        return movementType;
    }

    public long getSpawnTime() {
        return SpawnTime;
    }

    public void setSpawnTime(long now) {
        SpawnTime = now;
    }

    public final boolean isPuppet() {
        switch (skill) {
            case 3111002: //替身術
            case 3120012: //精銳替身術
            case 3211002: //替身術
            case 3220012: //精銳替身術
            case 4341006: //幻影替身
            case 13111004: //替身術
            case 33111003: //瘋狂陷阱
                return true;
        }
        return isAngel();
    }

    public final boolean isAngel() {
        return GameConstants.isAngel(skill);
    }

    public final boolean isMultiAttack() { //賽特拉特 磁場 地雷(hidden 自動爆炸) 戰鬥機器 : 巨人錘
        if (skill != 35111002 && skill != 35121003 && (skill == 33101008 || skill >= 35000000) && skill != 35111009 && skill != 35111010 && skill != 35111001) {
            return false;
        }
        return true;
    }

    public final boolean isBeholder() {
        return skill == 1321007; //暗之靈魂
    }

    public final boolean isReaper() { //甦醒
        return skill == 32111006;
    }

    public final boolean isMultiSummon() {
        return skill == 5211014 || skill == 33101008; //砲台章魚王 地雷(hidden 自動爆炸)
    }

    public final int getSkillLevel() {
        return skillLevel;
    }

    public final int getSummonType() {
        if (isAngel()) {
            return 2;

            // 精銳替身術(弓) 精銳替身術(弩)  瘋狂陷阱 地雷(hidden 自動爆炸)  磁場
        } else if ((skill != 3120012 && skill != 3220012 && skill != 33111003 && isPuppet()) || skill == 33101008 || skill == 35111002) {
            return 0;
        }
        switch (skill) {
            case 1321007: //暗之靈魂
                return 2; //buffs and stuff
            case 35111001: //賽特拉特
            case 35111009:
            case 35111010:
                return 3; //attacks what you attack
            case 35121009: //機器人工廠 : RM1
                return 5; //sub summons
            case 35121003: //戰鬥機器 : 巨人錘
                return 6; //charge
            case 4111007: //黑暗殺
            case 4211007: //黑暗殺
                return 7; //attacks what you get hit by
        }
        return 1;
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.SUMMON;
    }

    public final void CheckSummonAttackFrequency(final MapleCharacter chr, final int tickcount) {
        final int tickdifference = (tickcount - lastSummonTickCount);
        final long STime_TC = System.currentTimeMillis() - tickcount;
        final long S_C_Difference = Server_ClientSummonTickDiff - STime_TC;
        Summon_tickResetCount++;
        if (Summon_tickResetCount > 4) {
            Summon_tickResetCount = 0;
            Server_ClientSummonTickDiff = STime_TC;
        }
        lastSummonTickCount = tickcount;
    }

    public final void CheckPVPSummonAttackFrequency(final MapleCharacter chr) {
        final long tickdifference = (System.currentTimeMillis() - lastAttackTime);
        lastAttackTime = System.currentTimeMillis();
    }

    public final boolean isChangedMap() {
        return changedMap;
    }

    public final void setChangedMap(boolean cm) {
        this.changedMap = cm;
    }
}