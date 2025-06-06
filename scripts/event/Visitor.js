function init() {
}

function monsterValue(eim, mobId) {
    return 1;
}

function setup(mapid) {

    var eim = em.newInstance("Visitor" + mapid);
    for (var i = 0; i < 16; i++) {
        var map = eim.setInstanceMap(502030000 + (parseInt(mapid) - 1) + i);
        map.resetFully();
        if (i == 0) { //dimensional cube
            for (var x = 0; x < 3; x++) {
                map.spawnMonsterOnGroundBelow(em.getMonster(9420024 + (parseInt(mapid) * 6)), new java.awt.Point(94, 0));
                map.spawnMonsterOnGroundBelow(em.getMonster(9420024 + (parseInt(mapid) * 6)), new java.awt.Point(-67, -140));
                map.spawnMonsterOnGroundBelow(em.getMonster(9420027 + (parseInt(mapid) * 6)), new java.awt.Point(-67, -140));
                map.spawnMonsterOnGroundBelow(em.getMonster(9420027 + (parseInt(mapid) * 6)), new java.awt.Point(-287, 50));
            }
        } else if (i == 1) {
            for (var x = 0; x < 3; x++) {
                map.spawnMonsterOnGroundBelow(em.getMonster(9420029 + (parseInt(mapid) * 6)), new java.awt.Point(177, 110));
                map.spawnMonsterOnGroundBelow(em.getMonster(9420029 + (parseInt(mapid) * 6)), new java.awt.Point(177, -160));
                map.spawnMonsterOnGroundBelow(em.getMonster(9420029 + (parseInt(mapid) * 6)), new java.awt.Point(-237, -20));
            }
        } else if (i == 2) {
            for (var x = 0; x < 3; x++) {
                map.spawnMonsterOnGroundBelow(em.getMonster(9420025 + (parseInt(mapid) * 6)), new java.awt.Point(160, 50));
                map.spawnMonsterOnGroundBelow(em.getMonster(9420026 + (parseInt(mapid) * 6)), new java.awt.Point(0, 50));
                map.spawnMonsterOnGroundBelow(em.getMonster(9420028 + (parseInt(mapid) * 6)), new java.awt.Point(-160, 50));
            }
        } else if (i == 3) {
            for (var x = 0; x < 6; x++) {
                map.spawnMonsterOnGroundBelow(em.getMonster(9420042 + parseInt(mapid)), new java.awt.Point(-50, -160));
                map.spawnMonsterOnGroundBelow(em.getMonster(9420045 + parseInt(mapid)), new java.awt.Point(-50, 100));
                map.spawnMonsterOnGroundBelow(em.getMonster(9420048 + parseInt(mapid)), new java.awt.Point(-50, 320));
            }

        } else if (i == 4) { //choice
            map.spawnReactorOnGroundBelow(em.getReactor(5029000), new java.awt.Point(-140, 70));
            map.spawnReactorOnGroundBelow(em.getReactor(5029000), new java.awt.Point(0, 70));
            map.spawnReactorOnGroundBelow(em.getReactor(5029000), new java.awt.Point(140, 70));
            eim.setProperty("stage5", (java.lang.Math.floor(java.lang.Math.random() * 3) + 1) + "");
        } else if (i == 5) {
            for (var x = 0; x < 6; x++) {
                map.spawnMonsterOnGroundBelow(em.getMonster(9420051 + (parseInt(mapid))), new java.awt.Point(0, -50));
                map.spawnMonsterOnGroundBelow(em.getMonster(9420051 + (parseInt(mapid))), new java.awt.Point(0, 320));
            }
        } else if (i == 6) {
            for (var x = 0; x < 6; x++) {
                map.spawnMonsterOnGroundBelow(em.getMonster(9420054 + (parseInt(mapid))), new java.awt.Point(0, 0));
            }
        } else if (i == 7) { //control cube: npc!
            map.spawnNpc(9250139, new java.awt.Point(-5, -150));
            eim.setProperty("stage8", "1");
        } else if (i == 8) {
            map.spawnMonsterOnGroundBelow(em.getMonster(9420057 + (parseInt(mapid))), new java.awt.Point(0, 0));
        } else if (i == 9) {
            map.spawnMonsterOnGroundBelow(em.getMonster(9420060 + (parseInt(mapid))), new java.awt.Point(0, 0));
        } else if (i == 10) {
            map.spawnMonsterOnGroundBelow(em.getMonster(9420065 + (parseInt(mapid))), new java.awt.Point(0, 0));
        }
    }
    eim.setProperty("entryTimestamp", "" + java.lang.System.currentTimeMillis());
    eim.setProperty("current_instance", "0");
    eim.setProperty("mode", mapid);
    eim.startEventTimer(3600000); //60 mins lol

    return eim;
}

function playerEntry(eim, player) {
    var map = eim.getMapInstance(0);
    player.changeMap(map, map.getPortal(0));
}

function playerDead(eim, player) {
}

function changedMap(eim, player, mapid) {
    switch (mapid) {
        case 502030000:
        case 502030001:
        case 502030002:
        case 502030003:
        case 502030004:
        case 502030005:
        case 502030006:
        case 502030007:
        case 502030008:
        case 502030009:
        case 502030010:
        case 502030032:
        case 502030033:
        case 502030034:
        case 502030035:
        case 502030036:
        case 502030037:
        case 502030038:
        case 502030039:
        case 502030040:
        case 502030041:
        case 502030042:
        case 502030043:
        case 502030044:
        case 502030045:
        case 502030046:
        case 502030047:
            return;
    }
    if (mapid < 502030000 || mapid >= 502030100) {
        eim.unregisterPlayer(player);
        eim.disposeIfPlayerBelow(2, 502029000);
    } else {
        return;
        // eim.setProperty("current_instance", "" + mapid);
    }
}

function playerRevive(eim, player) {
}

function playerDisconnected(eim, player) {
    return -2;
}

function leftParty(eim, player) {
    // If only 2 players are left, uncompletable
    if (!eim.disposeIfPlayerBelow(2, 502029000)) {
        playerExit(eim, player);
    }
}

function disbandParty(eim) {
    // Boot whole party and end
    eim.disposeIfPlayerBelow(100, 502029000);
}


function scheduledTimeout(eim) {
    clearPQ(eim);
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);

    eim.disposeIfPlayerBelow(2, 502029000);
}

function clearPQ(eim) {
    // KPQ does nothing special with winners
    eim.disposeIfPlayerBelow(100, 502029000);
}

function allMonstersDead(eim) {
}

function cancelSchedule() {
}