/*
     名字：費歐娜
     地图：亡者峽谷
     描述：610010004
 */

var status;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    switch (mode) {
        case -1:
            cm.dispose();
            return;
        case 0:
            if (status < 3) {
                cm.dispose();
                return;
            }
            status--;
            break;
        case 1:
            status++;
            break;
    }
    switch (status) {
        case 0:
            if (cm.getPlayer().getQuestNAdd(Packages.server.quest.MapleQuest.getInstance(8225)).getStatus() < 2) {
                cm.sendOk("讓開，旅行者，這裡不是你湊熱鬧的地方。");
                cm.dispose();
                return;
            }
            var chat = "嘿，搭檔！如果你有合適的物品，我可以把它做成很好的。#b"
            var options = new Array("武器锻造", "武器升级");
            for (var i = 0; i < options.length; i++)
                chat += "\r\n#L" + i + "#" + options[i] + "#l";
            cm.sendSimple(chat);
            break;
        case 1:
            if (selection < 1) {
                var chat = "好的，你想制作那一种武器？#b";
                items = [2070018, 1382060, 1442068, 1452060];
                for (var i = 0; i < items.length; i++)
                    chat += "\r\n#L" + i + "##z" + items[i] + "##l";
            }
            if (selection > 0) {
                var chat = "好的，你想升级那一种武器？#b";
                items = [1472074, 1472073, 1472075, 1332079, 1332078, 1332080, 1462054, 1462053, 1462055, 1402050, 1402049, 1402051];
                for (var i = 0; i < items.length; i++)
                    chat += "\r\n#L" + i + "##z" + items[i] + "##l";
            }
            select = selection;
            cm.sendSimple(chat);
            break;
        case 2:
            selectItem = selection;

            if (select < 1) {
                var matSet = [[4032015, 4032016, 4032017, 4021008, 4032005], [4032016, 4032017, 4032004, 4032005, 4032012, 4005001], [4032015, 4032017, 4032004, 4032005, 4032012, 4005000], [4032015, 4032016, 4032004, 4032005, 4032012, 4005002]];
                var matSetQty = [[1, 1, 1, 100, 30], [1, 1, 400, 10, 30, 4], [1, 1, 500, 40, 20, 4], [1, 1, 300, 75, 10, 4]];
                var costSet = [70000, 70000, 70000, 70000];
            }
            if (select > 0) {
                var matSet = [[4032017, 4005001, 4021008], [4032015, 4005002, 4021008], [4032016, 4005000, 4021008], [4032017, 4005001, 4021008], [4032015, 4005002, 4021008], [4032016, 4005000, 4021008], [4032017, 4005001, 4021008], [4032015, 4005002, 4021008], [4032016, 4005000, 4021008], [4032017, 4005001, 4021008], [4032015, 4005002, 4021008], [4032016, 4005000, 4021008]];
                var matSetQty = [[1, 10, 20], [1, 10, 30], [1, 5, 20], [1, 10, 20], [1, 10, 30], [1, 5, 20], [1, 10, 20], [1, 10, 30], [1, 5, 20], [1, 10, 20], [1, 10, 30], [1, 5, 20]];
                var costSet = [75000, 50000, 50000, 75000, 50000, 50000, 75000, 50000, 50000, 75000, 50000, 50000];
            }
            item = items[selectItem];
            mat = matSet[selectItem];
            matQty = matSetQty[selectItem];
            cost = costSet[selectItem];

            var chat = "你想製作";
            chat += "#t" + item + "#？";
            chat += "我需要你提供足夠的材料才能完成。#b";
            for (var i = 0; i < mat.length; i++)
                chat += "\r\n#v" + mat[i] + "#" + (matQty[i] * 1) + "#t" + mat[i] + "#";
            chat += "\r\n#v4031138#" + (cost * 1) + "楓幣";
            cm.sendYesNo(chat);
            break;
        case 3:
            for (var i = 0; i < mat.length; i++)
                if (cm.getPlayer().itemQuantity(mat[i]) < matQty[i] * 1) {
                    cm.sendOk("很抱歉，你所提供的材料不能滿足製作要求。");
                    cm.dispose();
                    return;
                }
            if (cm.getPlayer().getMeso() < (cost * 1)) {
                cm.sendOk("很抱歉，请确定一下您有#b" + cost + "#k楓幣嗎？");
                cm.dispose();
                return;
            }
            if (cm.getPlayer().getInventory(Packages.client.inventory.MapleInventoryType.EQUIP).getNumFreeSlot() < 1) {
                cm.getClient().sendPacket(Packages.tools.packet.MaplePacketCreator.serverNotice(1, "裝備道具視窗的欄位不足"));
                cm.dispose();
                return;
            }
            for (var i = 0; i < mat.length; i++)
                cm.gainItem(mat[i], -matQty[i] * 1);
            cm.gainMeso(-cost * 1);
            cm.gainItem(item, 1);
            cm.sendOk("不錯吧，有需要請再來找我哦~");
            cm.dispose();
    }
}
