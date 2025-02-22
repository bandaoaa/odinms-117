/*
 *	������ - �������ѩ��
 */

var status = -1;
var minLevel = 40;
var maxCount = 5;
var minPartySize = 1;
var maxPartySize = 4;

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (status >= 0 && mode == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }
        if (status == 0) {
            cm.sendSimple("#e<�������ѩ��>#n\r\n�A�@�N�h�����̵M�r�ަb�������a�Ϫ��ƩԪ��l�ҶܡH#b\r\n\r\n\r\n#L1#�����ƩԪ��l�ҡC(40�ťH�W�C�Ѿl�J������: ��)#l\r\n#L0#������ԧƩ�(120�ťH�W)#l");
        } else if (status == 1) {
            if (selection == 0) {
                if (cm.getPlayer().getLevel() >= 120) {
                    cm.sendNext("�{�b�A�N��F�ƩԤ���J�f�A�аȥ������Ʃԧa�C");
                } else {
                    cm.sendOk("�H�A�{�b����O�A��ԧƩԦ��ǫj�j�C�����F��120�ťH�W�~��i��D�ԡC");
                    cm.dispose();
                }
            } else {
                if (cm.getPlayer().getParty() == null) {
                    cm.sendOk("�����ն��J���C");
                } else if (!cm.isLeader()) {
                    cm.sendOk("�A���O�����ڡH�������өM�ڻ��ܡC");
                } else {
                    var party = cm.getPlayer().getParty().getMembers();
                    var mapId = cm.getPlayer().getMapId();
                    var next = 0;
                    var levelValid = 0;
                    var inMap = 0;
                    var it = party.iterator();
                    while (it.hasNext()) {
                        var cPlayer = it.next();
                        var ccPlayer = cm.getPlayer().getMap().getCharacterById(cPlayer.getId());
                        if (ccPlayer == null) {
                            next = 1;
                        }
                        if (ccPlayer != null && ccPlayer.getLevel() >= minLevel) {
                            levelValid += 1;
                        } else {
                            next = 2;
                        }
                        if (ccPlayer != null && ccPlayer.getMapId() == mapId) {
                            inMap += 1;
                        }
                        //if (ccPlayer != null && ccPlayer.getBossLog("������") >= maxCount) {
                        //  next = 4;
                        //}
                    }
                    if (party.size() > maxPartySize || inMap < minPartySize) {
                        next = 3;
                    }
                    if (next == 1) {
                        cm.sendOk("��������a���b���a�ϡC");
                    } else if (next == 2) {
                        cm.sendOk("��������a�����Ť��ŦX�C���� " + minLevel + "�ťH�W�������A�~��i�h�C");
                    } else if (next == 3) {
                        cm.sendOk("��������" + minPartySize + "�H�C�ܤ֥�����" + minPartySize + "��" + minLevel + "�ťH�W�������A�~��i�h�C");
                    } else if (next == 4) {
                        cm.sendOk("��������a���J�����Ƥw�g�Χ��C");
                    } else if (next == 0) {
                        cm.getPlayer().dropMessage(6, "test");
                        var em = cm.getEventManager("Aswan");
                        if (em == null) {
                            cm.sendOk("��e�A�Ⱦ����}�Ҧ��\��A�еy��b��...");
                        } else {
                            var prop = em.getProperty("state");
                            if (prop.equals("0") || prop == null) {
                                //         cm.setPartyBossLog("������");
                                em.startInstance(cm.getPlayer().getParty(), cm.getPlayer().getMap(), 200);
                            } else {
                                cm.sendOk("��e�W�D�w�����a�b�i����Ȥ��A�еy��b�աC");
                            }
                        }
                    } else {
                        cm.sendOk("��������" + minPartySize + "�H�C�o�̫D�`�M�I�C�ܤ֥�����" + minPartySize + "��" + minLevel + "�ťH�W�������A�~��i�h�C");
                    }
                }
                cm.dispose();
            }
        } else if (status == 2) {
            cm.warp(262030000, 0); //�ƩԤ��� - �ƩԤ���J�f
            cm.dispose();
        }
    }
}