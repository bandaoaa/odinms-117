package client;

import database.DatabaseConnection;
import server.Randomizer;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapleCoreAura implements Serializable {
    private static final long serialVersionUID = 7179541546283738569L;
    
    private int dataId, str, dex, int_, luk, watk, matk, maxlevel, rank;
    private long timestamp;
    private boolean changed = false, thirdParty = false, solid = false;
    
    private static final int RARE = 10;
    private static final int EPIC = 11;
    private static final int UNIQUE = 12;
    private static final int LEGENDARY = 13;

    public MapleCoreAura(int dataId, int str, int dex, int int_, int luk, int watk, int matk, int maxlevel, int rank, long timestamp, boolean solid) {
        this.dataId = dataId;
        this.str = str;
        this.dex = dex;
        this.int_ = int_;
        this.luk = luk;
        this.watk = watk;
        this.matk = matk;
        this.maxlevel = maxlevel;
        this.rank = rank;
        this.timestamp = timestamp;
        this.solid = solid;
    }

    // Getters
    public int getDataId() {
        return dataId;
    }

    public int getStr() {
        return str;
    }

    public int getDex() {
        return dex;
    }

    public int getInt() {
        return int_;
    }

    public int getLuk() {
        return luk;
    }

    public int getWatk() {
        return watk;
    }

    public int getMatk() {
        return matk;
    }

    public int getMaxlevel() {
        return maxlevel;
    }

    public int getRank() {
        return rank;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isSolid() {
        return solid;
    }

    public void setThirdParty(boolean third) {
        this.thirdParty = third;
    }

    public void changed() {
        this.changed = true;
    }

    public void addTimestamp(int itemId) {
        if (itemId == 5770000) {
            this.timestamp += 1296000000L;
        } else {
            this.timestamp += 604800000L;
            this.solid = true;
        }
        changed();
    }

    public void shiftRight() {
        int old_str = this.str;
        int old_dex = this.dex;
        int old_int = this.int_;
        int old_luk = this.luk;
        int old_watk = this.watk;
        int old_matk = this.matk;
        this.watk = old_str;
        this.dex = old_watk;
        this.luk = old_dex;
        this.matk = old_luk;
        this.int_ = old_matk;
        this.str = old_int;
        changed();
    }

    public void resetFluxField(int itemId, boolean changeRank) {
        int newRank = this.rank;
        if (changeRank) {
            if (this.rank == 10) {
                if (Randomizer.nextInt(100) < 10)
                    newRank++;
            } else if (this.rank == 13) {
                if (Randomizer.nextInt(100) < 5 && itemId != 5771000) {
                    newRank--;
                }
            } else if (Randomizer.nextInt(100) < 7) {
                newRank++;
            } else if ((Randomizer.nextInt(100) < 5) && (itemId != 5771000)) {
                newRank--;
            }
        }

        int maxLvl = newRank == 12 ? 28 : newRank == 11 ? 23 : newRank == 10 ? 20 : 35;
        int bonus = itemId == 5771001 ? 15 : (itemId == 5771000) || (itemId == 2940001) ? 10 : 0;
        int totalPts = (newRank == 12 ? 78 : newRank == 11 ? 48 : newRank == 10 ? 30 : 120) + bonus;
        int new_str = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
        int new_dex = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
        int new_int = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
        int new_luk = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
        int new_watk = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
        int new_matk = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
        while (new_str + new_dex + new_int + new_luk + new_watk + new_matk > totalPts) {
            int type = Randomizer.nextInt(6);
            switch (type) {
                case 0:
                    new_str = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
                    break;
                case 1:
                    new_dex = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
                    break;
                case 2:
                    new_int = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
                    break;
                case 3:
                    new_luk = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
                    break;
                case 4:
                    new_watk = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
                    break;
                case 5:
                    new_matk = Randomizer.rand(Randomizer.rand(3, 8), maxLvl);
            }
        }
        this.rank = newRank;
        this.maxlevel = maxLvl;
        this.str = new_str;
        this.dex = new_dex;
        this.int_ = new_int;
        this.luk = new_luk;
        this.watk = new_watk;
        this.matk = new_matk;
        this.timestamp = (System.currentTimeMillis() + 86400000L - 60000L);
        this.solid = false;
        changed();
    }
    
    public MapleCoreAura(Map<Byte, Integer> data, long timestamp) {
        for (Map.Entry<Byte, Integer> ii : data.entrySet()) {
            byte key = ii.getKey();
            int value = ii.getValue();
            
            switch (key) {
                case 0:
                    this.dataId = value;
                    break;
                case 1:
                    this.str = value;
                    break;
                case 2:
                    this.dex = value;
                    break;
                case 3:
                    this.int_ = value;
                    break;
                case 4:
                    this.luk = value;
                    break;
                case 5:
                    this.watk = value;
                    break;
                case 6:
                    this.matk = value;
                    break;
                case 7:
                    this.maxlevel = value;
                    break;
                case 8:
                    this.rank = value;
                    break;
                case 9:
                    this.thirdParty = (value > 0);
                    break;
                case 10:
                    this.solid = (value > 0);
                    break;
            }
        }
        this.timestamp = timestamp;
    }

    public Map<Byte, Integer> prepareTransfer() {
        Map<Byte, Integer> stats = new LinkedHashMap<>();
        stats.put((byte) 0, this.dataId);
        stats.put((byte) 1, this.str);
        stats.put((byte) 2, this.dex);
        stats.put((byte) 3, this.int_);
        stats.put((byte) 4, this.luk);
        stats.put((byte) 5, this.watk);
        stats.put((byte) 6, this.matk);
        stats.put((byte) 7, this.maxlevel);
        stats.put((byte) 8, this.rank);
        stats.put((byte) 9, this.thirdParty ? 1 : 0);
        stats.put((byte) 10, this.solid ? 1 : 0);
        return stats;
    }

    public void resetCoreAura() {
        this.str = 5;
        this.dex = 5;
        this.int_ = 5;
        this.luk = 5;
        this.watk = 0;
        this.matk = 0;
        this.timestamp = System.currentTimeMillis() + 86400000;
    }

    public static MapleCoreAura loadData(int charid, boolean others) throws SQLException {
        long currenttime = System.currentTimeMillis();
        MapleCoreAura mca = new MapleCoreAura(0, 3, 3, 3, 3, 3, 3, 5, 10, currenttime + 86400000L, false);
        
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            ps = con.prepareStatement("SELECT * FROM `skills_core_aura` WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                int core = rs.getInt("id");
                long timestp = rs.getLong("timestamp");
                
                if (currenttime > timestp) {
                    mca = new MapleCoreAura(core, 3, 3, 3, 3, 3, 3, 20, 10, currenttime + 86400000L, false);
                    mca.resetFluxField(0, true);
                } else {
                    mca = new MapleCoreAura(core, rs.getInt("str"), rs.getInt("dex"), rs.getInt("int"), rs.getInt("luk"), rs.getInt("watk"), rs.getInt("matk"), rs.getInt("maxlevel"), rs.getInt("rank"), timestp, rs.getByte("solid") > 0);
                }
            } else {
                mca.changed();
            }
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ignored) {}
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException ignored) {}
            }
        }
        
        mca.setThirdParty(others);
        return mca;
    }


    public final void saveData(int charid) throws SQLException {
        if (!this.changed || this.thirdParty) {
            return;
        }

        this.changed = false;
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("DELETE FROM skills_core_aura WHERE characterid = ?");
        ps.setInt(1, charid);
        ps.execute();
        ps.close();

        ps = con.prepareStatement("INSERT INTO skills_core_aura (characterid, str, dex,`int`, luk, watk, matk, maxlevel, rank, timestamp, solid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        ps.setInt(1, charid);
        ps.setInt(2, this.str);
        ps.setInt(3, this.dex);
        ps.setInt(4, this.int_);
        ps.setInt(5, this.luk);
        ps.setInt(6, this.watk);
        ps.setInt(7, this.matk);
        ps.setInt(8, this.maxlevel);
        ps.setInt(9, this.rank);
        ps.setLong(10, this.timestamp);
        ps.setByte(11, (byte) (this.solid ? 1 : 0));
        ps.execute();
        ps.close();
    }
}

