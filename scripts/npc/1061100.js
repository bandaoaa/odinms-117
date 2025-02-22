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
/**
 -- Odin JavaScript --------------------------------------------------------------------------------
 Hotel Receptionist - Sleepywood Hotel(105040400)
 -- By ---------------------------------------------------------------------------------------------
 Unknown
 -- Version Info -----------------------------------------------------------------------------------
 1.3 - More Cleanup by Moogra - 12/17/09
 1.2 - Cleanup and Statement fix by Moogra
 1.1 - Statement fix [Information]
 1.0 - First Version by Unknown
 ---------------------------------------------------------------------------------------------------
 **/

var status = 0;
var regcost = 499;
var vipcost = 999;
var iwantreg = 0;

function start() {
    cm.sendNext("您好，这里是林中之城旅馆。我们酒店会竭诚为您提供最优质\r\n的服务。如果您打猎累了的话，可以在我们酒店休息。");
}

function action(mode, type, selection) {
    if (mode == -1 || (mode == 0 && status == 1)) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 2) {
            cm.sendNext("我们还可以为你提供其他更多服务。欢迎下次光临！");
            cm.dispose();
            return;
        }
        status++;
        if (status == 1) {
            cm.sendSimple("我们旅馆有2种房间。请选择您想使用哪种服务。\r\n#b#L0#普通桑拿房(1次" + regcost + "金币)#l\r\n#L1#高级桑拿房(1次" + vipcost + "金币)#l");
            iwantreg = 1;
        } else if (status == 2) {
            if (selection == 0) {
                cm.sendYesNo("你选了普通桑拿房。你可以更快恢复生命值和魔法力，而且也\r\n可以购买多种物品。你要使用吗？");
            } else if (selection == 1) {
                cm.sendYesNo("你选了高级桑拿房。比普通桑拿房更快恢复生命值和魔法力，\r\n而且在里面可以购买特别的物品，你要使用吗？");
                iwantreg = 0;
            }
        } else if (status == 3) {
            if (iwantreg == 1) {
                if (cm.getMeso() >= regcost) {
                    cm.warp(105000011);
                    cm.gainMeso(-regcost);
                } else {
                    cm.sendNext("对不起！您好像没有" + regcost + "金币，你无法使用普通桑拿房。");
                }
            } else {
                if (cm.getMeso() >= vipcost) {
                    cm.warp(105000012);
                    cm.gainMeso(-vipcost);
                } else {
                    cm.sendNext("对不起！您好像没有" + vipcost + "金币，你无法使用高级桑拿房。");
                }
            }
            cm.dispose();
        }
    }
}
