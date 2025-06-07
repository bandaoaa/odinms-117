package server;

import client.SkillFactory;
import client.inventory.MapleInventoryIdentifier;
import constants.QuickMove;
import database.DatabaseConnection;
import handling.MapleServerHandler;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.login.LoginInformationProvider;
import handling.login.LoginServer;
import static handling.login.LoginServer.TIMEZONE;
import handling.world.World;
import handling.world.family.MapleFamily;
import handling.world.guild.MapleGuild;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import server.Timer.BuffTimer;
import server.Timer.EtcTimer;
import server.Timer.EventTimer;
import server.Timer.MapTimer;
import server.Timer.PingTimer;
import server.Timer.PokeTimer;
import server.Timer.WorldTimer;
import server.events.MapleOxQuizFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MobSkillFactory;
import server.life.PlayerNPC;
import server.quest.MapleQuest;

public class Start {

    public static long startTime = System.currentTimeMillis();
    public static final Start instance = new Start();
    private static final int LOADING_THREADS = Runtime.getRuntime().availableProcessors(); //建立線程，線程數=CPU核心數

    public void run() throws InterruptedException {
        System.setProperty("net.sf.odinms.wzpath", "wz"); // 支援Java17
        System.setProperty("polyglot.js.nashorn-compat", "true");

        System.out.println("[Database] Clearing loggedin status...");
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0")) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[Database Error] Could not clear loggedin status: " + ex.getMessage());
        }

        System.out.println("[World] Initializing World...");
        World.init();

        System.out.println("[Timers] Starting timers...");
        WorldTimer.getInstance().start();
        PokeTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        PingTimer.getInstance().start();
        System.out.println("[Timers] Timers started.");

        System.out.println("[Loading Phase 1] Starting parallel data loading with " + LOADING_THREADS + " threads...");
        ExecutorService loadingExecutor = Executors.newFixedThreadPool(LOADING_THREADS);

        // 任務並行載入 使用lambda表達式
        CompletableFuture<Void> phase1Tasks = CompletableFuture.allOf(Stream.of((Runnable)
                        () -> MapleGuildRanking.getInstance().load(),
                        () -> MapleGuild.loadAll(),
                        () -> MapleFamily.loadAll(),
                        () -> MapleLifeFactory.loadQuestCounts(),
                        () -> MapleQuest.initQuests(),
                        () -> MapleItemInformationProvider.getInstance().runEtc(),
                        () -> MapleMonsterInformationProvider.getInstance().load(),
                        () -> MapleItemInformationProvider.getInstance().runItems(),
                        () -> SkillFactory.load(),
                        () -> LoginInformationProvider.getInstance(),
                        () -> RandomRewards.load(),
                        () -> MapleOxQuizFactory.getInstance(),
                        () -> MapleCarnivalFactory.getInstance(),
                        () -> CharacterCardFactory.getInstance().initialize(),
                        () -> MobSkillFactory.getInstance(),
                        () -> SpeedRunner.loadSpeedRuns(),
                        () -> MTSStorage.load(),
                        () -> MapleInventoryIdentifier.getInstance(),
                        () -> CashItemFactory.getInstance().initialize()
                ).map(r -> CompletableFuture.runAsync(r, loadingExecutor)).toArray(CompletableFuture[]::new)
        );


        try {
            System.out.println("[Loading Phase 1] Waiting for all tasks to complete...");
            phase1Tasks.join();  // 等待所有任務完成
            System.out.println("[Loading Phase 1] All parallel tasks finished.");
        } catch (Exception e) {
            System.err.println("[Loading Error Phase 1] Parallel loading interrupted!" + e);
            e.printStackTrace();
            return;
        }

        // 網路服務啟動任務
        System.out.println("[Network] Starting network services...");
        CompletableFuture<Void> networkTasks = CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> {
                    try {
                        MapleServerHandler.initiate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }),
                CompletableFuture.runAsync(() -> {
                    try {
                        LoginServer.run_startup_configurations();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }),
                CompletableFuture.runAsync(() -> {
                    try {
                        ChannelServer.startChannel_Main();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }),
                CompletableFuture.runAsync(() -> {
                    try {
                        CashShopServer.run_startup_configurations();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
        );

        try {
            networkTasks.join(); // 等待網路服務啟動完成
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        System.out.println("[Network] Network services started.");

        System.out.println("[Loading Phase 2] Starting remaining data loading...");
        CompletableFuture<Void> phase2Tasks = CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> {
                    World.registerRespawn();
                }, loadingExecutor),
                CompletableFuture.runAsync(() -> {
                    PlayerNPC.loadAll();
                }, loadingExecutor)
        );

        try {
            phase2Tasks.join(); // 等待任務完成
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        QuickMove.QuickMoveLoad();
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE));

        System.out.println("[Login] Setting Login Server ON...");
        LoginServer.setOn();

        loadingExecutor.shutdown(); //銷毀線程

        System.out.println("[Server] Fully Initialized in " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");
    }

    public static void main(final String args[]) throws InterruptedException {
        instance.run();
    }
}
