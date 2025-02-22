/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation version 3 as published by
the Free Software Foundation. You may not use, modify or distribute
this program under any other version of the GNU Affero General Public
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/* Author: Xterminator
NPC Name: Mr. Goldstein
Map(s): Victoria Road : Lith Harbour (104000000)
Description:Extends Buddy List
 */
var status = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (status == 0 && mode == 0) {
            cm.sendNext("是吗......看来我的预感是错的，你好像没什么朋友啊？哈哈哈\r\n~ 开玩笑，玩笑~ 如果你改变了注意，可以再来找我。等朋友\r\n多一点的时候......呵呵......");
            cm.dispose();
            return;
        } else if (status >= 1 && mode == 0) {
            cm.sendNext("是吗......看来我的预感是错的，你好像没什么朋友啊？或者身上没有24万金币？如果你改变了注意，可以再来找我。等你有了钱的时候......呵呵......");
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }
        if (status == 0) {
            cm.sendYesNo("希望今天客人能多一点...... 啊！等一下！你想增加好友目录吗\r\n？我一看你，就觉得你有很多朋友。怎么样...... 只要花一点钱\r\n，我就可以为你增加好友目录。但是不会应用于相同账号的其\r\n他角色，所以一定要慎重。你想增加吗？");
        } else if (status == 1) {
            cm.sendYesNo("好的！明智的决定。价格不贵，因为我下定决心，给你打了个\r\n#r打折扣#k。#b好友目录添加 5 名一共是24万金币#k。当然，绝不零售\r\n。只要购买一次，目录就可以永久增加。对好友目录不足的人\r\n来说，这个买卖应该不坏。怎么样？你愿意支付24万金币吗？");
        } else if (status == 2) {
            var capacity = cm.getPlayer().getBuddylist().getCapacity();
            if (capacity >= 50 || cm.getMeso() < 240000) {
                cm.sendNext("你......确定自己有#b24万金币#k吗？如果有的话，请你确认一下好\r\n友目录是否已经增加到最大了。即使钱再多，好友目录的人\r\n数也无法增加到#b100个以上#k。");
                cm.dispose();
            } else {
                var newcapacity = capacity + 5;
                cm.gainMeso(-240000);
                cm.getPlayer().setBuddyCapacity(newcapacity)
                cm.sendOk("好的！你的好友目录已经增加了5个。你可以确认一下。如果好友目录还是不够的话，可以随时来找我。我可以随时帮你增加，不管多少次都行。当然不是免费的......那么再见~");
                cm.dispose();
            }
        }
    }
}
