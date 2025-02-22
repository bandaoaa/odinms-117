importPackage(Packages.tools);
var player;

function start(ms) {
    player = ms.getPlayer();
    player.resetEnteredScript();
    ms.getClient().sendPacket(MaplePacketCreator.showEffect("event/space/start"));
    player.startMapEffect("Please rescue Gaga within the time limit.", 5120027);
    var map = player.getMap();
    if (map.getTimeLeft() > 0) {
        ms.getClient().sendPacket(MaplePacketCreator.getClock(map.getTimeLeft()));
    } else {
        map.addMapTimer(180);
    }
    ms.useItem(2360002);
}  