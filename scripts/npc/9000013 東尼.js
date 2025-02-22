/*
     名字：東尼
     地图：玩具城
     描述：220000000
 */

var Text = [["[上樓~上樓]\r\n\r\n是一個爬梯子遊戲，參加者需要不斷的爬梯子選擇道路，轉移地圖，直至到達目的地。 \r\n\r\n遊戲由三個關卡組成，時間限制為#b6分鐘#k，在整個遊戲過程中，你不能跳躍，瞬移，加速，或者使用一些增益藥劑，路途中也會有一些陷阱，將你帶入歧途，所以請小心。"],
    ["[向高地出發]\r\n\r\n是一場穿越障礙物的比賽，很像忍耐之林。通過你的智慧，在規定時間內到達最終目的地。\r\n\r\n遊戲由四個關卡組成，時間限制為#b15分鐘#k，在整個遊戲過程中, 你不能瞬移，加速，或者使用一些增益藥劑。"],
    ["[滾雪球大賽]\r\n\r\n是由兩支組隊參加的競技比賽，在規定的時間內把雪球滾得越遠越大的一方獲勝。 \r\n\r\n在整個遊戲過程中, 只有#b普通攻擊#k可以使用，用#b「Ctrl」#k鍵攻擊，可以卷起雪花，在滾雪球的過程中，玩家不能用身體接觸到雪球，否則會被強制送回起點。這是一個精心策劃的戰略活動，只有團隊合作，才能獲得最終勝利。"],
    ["[打椰子比賽]\r\n\r\n是由兩支組隊參加的競技比賽，在規定的時間內椰子數量最多的一方獲勝。\r\n\r\n遊戲時間限制為#b5分鐘#k，如果遊戲常規時間結束時，兩隊比分相同，那麼將會在追加的2分鐘延時賽中。決定最終結果，如果依然保持平局，那麼比賽將以平局結束。\r\n\r\n只有#b普通攻擊#k可以使用，如果你沒有近距離攻擊的武器，可以通過地圖上的npc購買武器。無論是角色的等級、武器或技能，所有的傷害值都將是相同的。\r\n\r\n請小心地圖中的障礙和陷阱，如果角色在遊戲中死亡，那麼角色將會從遊戲中消除。"],
    ["[OX答題]\r\n\r\n答題遊戲是MapleStory考驗智益的活動。一旦你加入活動，請按#b「M」鍵#k，打開小地圖，就可以看到 X 或 O 。\r\n\r\n這個活動很簡單，簡單來說就是螢幕中間出現問題之後，依照你的判斷，站在答案相應的地圖位置裡面。"],
    ["[尋寶]\r\n\r\n這是一場尋找隱藏在地圖上物品的活動，你的目標是找到#b藏寶卷軸#k。\r\n\r\n遊戲時間限制為#b10分鐘#k，尋找地圖中隱藏起來神秘寶箱，一旦你將它們找到，使用#b「Ctrl」#k鍵攻擊進行，打破寶箱獲取裡面的物品。"]];

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
            cm.sendNext("嘿，我是#b#p9000001##k，我在等我的弟弟#b#p9000000##k，他現在應該到這裡了...");
            break;
        case 1:
            cm.sendNextPrev("我們打算一起邀約去參加附近的活動，有很多人都趕著去報名...");
            break;
        case 2:
            cm.sendSimple("嘿...你想不想和我一起去看看？我想我弟弟應該和其他人在活動地圖等我們過去。\r\n#L0##e1.#n#b為什麼要舉辦活動#k#l\r\n#L1##e2.#n#b有一些什麼樣的活動#k#l\r\n#L2##e3.#n#b讓我們出發吧#k#l");
            break;
        case 3:
            if (selection == 0) {
                cm.sendNext("整個這個月，MapleStory都在慶祝它的周年紀念日，GM's將舉行驚喜事件，所以請你保持留意，確保不要錯過這次活動。");
                cm.dispose();
            }
            if (selection == 1) {
                cm.sendSimple("這次比賽有很多項目，所以在你參加活動之前，最好能瞭解這些活動得規則，這樣會對你有很大的幫助，請選擇一個你感興趣的項目。#b\r\n#L0# 上樓~上樓#l\r\n#L1# 向高地出發#l\r\n#L2# 滾雪球大賽#l\r\n#L3# 打椰子比賽#l\r\n#L4# OX答題#l\r\n#L5# 尋寶#l#k");
            }
            if (selection == 2) {
                if (cm.getEvent() != null && cm.getEvent().getLimit() > 0) {
                    cm.getPlayer().saveLocation("EVENT");
                    if (cm.getEvent().getMapId() == 109080000 || cm.getEvent().getMapId() == 109060001)
                        cm.divideTeams();
                    cm.getEvent().minusLimit();
                    cm.getPlayer().changeMap(cm.getMap(cm.getEvent().getMapId()), cm.getMap(cm.getEvent().getMapId()).getPortal(0));
                    cm.dispose();
                    return;
                }
                cm.sendNext("活動事件尚未開啟，或者你今天已經參加過活動，請耐心等待。");
                cm.dispose();
            }
            break;
        case 4:
            cm.sendOk(Text[selection]);
            cm.dispose();
    }
}
