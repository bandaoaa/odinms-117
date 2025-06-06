package client;

import java.io.Serializable;

public class SkillEntry implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    public int skillevel;
    public int masterlevel;
    public long expiration;
    public int teachId;
    public byte slot;
    public byte equipped;

    public SkillEntry(int skillevel, int masterlevel, long expiration) {

        this.skillevel = skillevel;
        this.masterlevel = masterlevel;
        this.expiration = expiration;
        this.teachId = 0;
        this.slot = -1;
        this.equipped = -1;
    }

    public SkillEntry(int skillevel, int masterlevel, long expiration, int teachId, byte slot, byte equipped) {

        this.skillevel = skillevel;
        this.masterlevel = masterlevel;
        this.expiration = expiration;
        this.teachId = teachId;
        this.slot = slot;
        this.equipped = equipped;
    }
}