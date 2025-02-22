/* 流浪炼金术士 */

var status = 0;
var menu = "";
var set;
var makeitem;
var access = true;
var reqitem = new Array();
var cost = 4000;
var makeditem = new Array(4006000, 4006001);
var reqset = new Array(
    [[[4000046, 20], [4000027, 20], [4021001, 1]],
        [[4000025, 20], [4000049, 20], [4021006, 1]],
        [[4000129, 15], [4000130, 15], [4021002, 1]],
        [[4000074, 15], [4000057, 15], [4021005, 1]],
        [[4000054, 7], [4000053, 7], [4021003, 1]],
        [[4000238, 15], [4000241, 15], [4021000, 1]]],

    [[[4000028, 20], [4000027, 20], [4011001, 1]],
        [[4000014, 20], [4000056, 20], [4011003, 1]],
        [[4000132, 15], [4000128, 15], [4011005, 1]],
        [[4000074, 15], [4000069, 15], [4011002, 1]],
        [[4000080, 7], [4000079, 7], [4011004, 1]],
        [[4000226, 15], [4000237, 15], [4011001, 1]]]);

function start() {
    action(-1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 0 && (status == 1 || status == 2)) {
        cm.dispose();
        return;
    }
    if (mode == 0) {
        cm.sendNext("还没得到需要的材料吧？但以后随时有材料就给我吧。打猎,购买等等，得到道具的办法有多种。");
        cm.dispose();
    }
    if (mode == 1) {
        status++;
    }
    if (status == 0) {
        cm.sendNext("把拔鼠的舌头和猫头鹰的嘴按照1：1的比例混合........对了！我\r\n忘记放那些散发微弱光芒的粉末了~差点出大事......哎呦？你站\r\n在那多久了啊？啊......看来是我工作得太全神贯注了。嘿嘿~");
    } else if (status == 1) {
        cm.sendSimple("你也看见了，我是一名流浪炼金术士。虽然我还处于修行阶段，但说不定也能为你制作几样需要的东西呢。你先随便看看吧！\r\n\r\n#b#L0#制作#t4006000##l\r\n#b#L1#制作#t4006001##l");
    } else if (status == 2) {
        set = selection;
        makeitem = makeditem[set];
        for (i = 0; i < reqset[set].length; i++) {
            menu += "\r\n#L" + i + "##b用#t" + reqset[set][i][0][0] + "#和#t" + reqset[set][i][1][0] + "#制作#l";
        }
        cm.sendSimple("呵呵......#b#t" + makeitem + "##k是只有我能制作的神秘的石头.很多冒险家在使\r\n用非常强的技能时好像需要用到。制作#t" + makeitem + "#的办法一共有六\r\n种. 你想用哪种方法来制作呢?" + menu);
    } else if (status == 3) {
        //cm.playerMessage("当前选择 status: " + status + " selection: " + selection +"  "+reqset.length);
        if (selection < 0) { // || selection >= reqset.length
            //cm.playerMessage("出现错误关闭");
            cm.dispose();
            return;
        }
        set = reqset[set][selection];
        reqitem[0] = new Array(set[0][0], set[0][1]);
        reqitem[1] = new Array(set[1][0], set[1][1]);
        reqitem[2] = new Array(set[2][0], set[2][1]);
        menu = "";
        for (i = 0; i < reqitem.length; i++) {
            menu += "\r\n#v" + reqitem[i][0] + "# #t" + reqitem[i][0] + "# #b" + reqitem[i][1] + "个";
        }
        menu += "\r\n#i4031138# #b" + cost + " 金币#k";
        cm.sendYesNo("为做#b5个#t" + makeitem + "##k需要如下的材料，大多是打猎怪物就会得到\r\n的。所以你努力下去，不那么难得到。怎么样? 你真想制作吗?\r\n" + menu);
    } else if (status == 4) {
        for (i = 0; i < reqitem.length; i++) {
            if (!cm.haveItem(reqitem[i][0], reqitem[i][1]))
                access = false;
        }
        if (access == false || !cm.canHold(makeitem) || cm.getMeso() < cost) {
            cm.sendNext("请你再确认都有需要的道具或背包的其他窗有没有空间.");
        } else {
            cm.sendOk("在这里，采取5件f #b#t" + makeitem + "##k. 即使我不得不承认，这是一个杰作。好吧，如果你需要我的帮助下的道路，通过一切手段回来和我说话!");
            cm.gainItem(reqitem[0][0], -reqitem[0][1]);
            cm.gainItem(reqitem[1][0], -reqitem[1][1]);
            cm.gainItem(reqitem[2][0], -reqitem[2][1]);
            cm.gainMeso(-cost);
            cm.gainItem(makeitem, 5);
        }
        cm.dispose();
    }
}
