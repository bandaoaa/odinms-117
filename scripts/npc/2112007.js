/*
     名字：調查結果
     地图：可疑的研究室
     描述：926100000
 */

var x = [-1638, -1386, -1286, -1252, -1178, -884, -678, -584, -438, -578, -318, -200, 300, 560, 618, 778, 894, 1048, 1234, 1142, 1348, -1366, -1496, -1040, -908, 784, 132];

function start() {
    eim = cm.getPlayer().getEventInstance();
    if (eim.getProperty("1ch") == null || eim.getProperty("2ch") == null) {
        eim.setProperty("1ch", Math.floor(Math.random() * 26));
        eim.setProperty("2ch", Math.floor(Math.random() * 26));
    }
    for (var i = 0; i < x.length; i++)
        if (cm.getNpcob().getPosition().x == x[i]) {
            y = i;
        }
    var progress = eim.getProperty("3ch");
    if ((progress >> y) % 2 == 0) {
        if (eim.getProperty("1ch") == y) {
            cm.gainItem(4001131, 1);
            cm.sendNext("你發現了一封信，好像是封寫給愛人的信。");
        }
        if (eim.getProperty("2ch") == y) {
            cm.getPlayer().getMap().getReactorByName("d00").forceHitReactor(1);
            cm.getClient().sendPacket(Packages.tools.packet.MaplePacketCreator.getTopMsg("你打開了進入實驗室的通道"));
            cm.getPlayer().getMap().broadcastMessage(Packages.tools.packet.EtcPacket.environmentChange("quest/party/clear", 3));
            cm.getPlayer().getMap().broadcastMessage(Packages.tools.packet.EtcPacket.environmentChange("Party1/Clear", 4));
        }
        progress |= (1 << y);
        cm.gainMeso(500 + (Math.random() * 600));
        eim.setProperty("3ch", progress);
    }
    cm.dispose();
}
