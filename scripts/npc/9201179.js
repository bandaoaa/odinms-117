/*
     名字：一堆利耶尼礦石
     地图：外星基地走廊
     描述：610040010
 */

function start() {
    if (cm.getPlayer().getQuestNAdd(Packages.server.quest.MapleQuest.getInstance(28751)).getStatus() != 1) {
        cm.dispose();
        return;
    }
    if (cm.getPlayer().getPosition().x < 270 || cm.getPlayer().getPosition().x > 470 || cm.getPlayer().getPosition().y != 47) {
        cm.getClient().sendPacket(Packages.tools.packet.MaplePacketCreator.getTopMsg("距離太遠了……需要靠近一些"));
        cm.dispose();
        return;
    }
    if (cm.getPlayer().getQuestNAdd(Packages.server.quest.MapleQuest.getInstance(28751)).getCustomData() == null) {
        cm.getPlayer().getQuestNAdd(Packages.server.quest.MapleQuest.getInstance(28751)).setCustomData("00000");
    }
    var progress = cm.getPlayer().getQuestNAdd(Packages.server.quest.MapleQuest.getInstance(28751)).getCustomData();
    var slot = cm.getNpc() - 9201177;
    var ch = progress[slot];
    if (ch == '0') {
        var nextProgress = progress.substr(0, slot) + '3' + progress.substr(slot + 1);
        cm.gainItem(4033193, 1);
        Packages.server.quest.MapleQuest.getInstance(28776).forceStart(cm.getPlayer(), 0, null);
        cm.getClient().sendPacket(Packages.tools.packet.MaplePacketCreator.serverNotice(6, "獲得一塊礦石燃料"));
        cm.getPlayer().getQuestNAdd(Packages.server.quest.MapleQuest.getInstance(28751)).setCustomData(nextProgress);
        cm.getPlayer().updateQuest(cm.getPlayer().getQuestNAdd(Packages.server.quest.MapleQuest.getInstance(28751)), true);
    }
    cm.dispose();
}
