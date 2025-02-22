importPackage(Packages.tools);

function start(ms) {
    var pq = ms.getPyramid();
    ms.getPlayer().resetEnteredScript();
    ms.getClient().sendPacket(MaplePacketCreator.getClock(pq.timer()));
}