package com.menear.garbagecollector;

import com.menear.garbagecollector.commands.AdminCommands;
import com.menear.garbagecollector.commands.AdminTabCompleter;
import com.menear.garbagecollector.commands.ItemCommands;
import com.menear.garbagecollector.service.AchievementService;
import com.menear.garbagecollector.service.BasicGarbageService;
import com.menear.garbagecollector.service.BasicPlayerService;
import com.menear.garbagecollector.service.EventService;
import com.menear.garbagecollector.service.GarbageService;
import com.menear.garbagecollector.service.LeaderboardService;
import com.menear.garbagecollector.service.LevelService;
import com.menear.garbagecollector.service.MissionService;
import com.menear.garbagecollector.service.PlayerService;
import com.menear.garbagecollector.service.PowerupService;
import com.menear.garbagecollector.service.RecycleService;
import com.menear.garbagecollector.service.ZoneService;
import com.menear.garbagecollector.ui.AchievementGui;
import com.menear.garbagecollector.ui.CollectionGui;
import com.menear.garbagecollector.ui.GuiListener;
import com.menear.garbagecollector.ui.LeaderboardGui;
import com.menear.garbagecollector.ui.MissionGui;
import com.menear.garbagecollector.ui.RecycleGui;
import com.menear.garbagecollector.ui.ScoreboardManager;
import com.menear.garbagecollector.ui.SellGui;
import com.menear.garbagecollector.ui.ShopGui;
import com.menear.garbagecollector.ui.StatusBarManager;
import com.menear.garbagecollector.ui.ZoneGui;
import org.bukkit.plugin.java.JavaPlugin;

public final class GarbageCollectorPlugin extends JavaPlugin {

    private static GarbageCollectorPlugin instance;
    private PlayerService playerService;
    private GarbageService garbageService;
    private StatusBarManager statusBarManager;
    private ScoreboardManager scoreboardManager;
    private SellGui sellGui;
    private ShopGui shopGui;
    private CollectionGui collectionGui;
    private RecycleGui recycleGui;
    private ZoneGui zoneGui;
    private MissionGui missionGui;
    private AchievementGui achievementGui;
    private LeaderboardGui leaderboardGui;
    private LevelService levelService;
    private ZoneService zoneService;
    private RecycleService recycleService;
    private PowerupService powerupService;
    private MissionService missionService;
    private AchievementService achievementService;
    private LeaderboardService leaderboardService;
    private EventService eventService;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        new ConfigManager(this).ensureDefaults();

        // Services and managers
        this.statusBarManager = new StatusBarManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.playerService = new BasicPlayerService(this);
        this.garbageService = new BasicGarbageService(this);
        this.levelService = new LevelService(this);
        this.zoneService = new ZoneService(this);
        this.recycleService = new RecycleService(this);
        this.powerupService = new PowerupService(this);
        this.missionService = new MissionService(this);
        this.achievementService = new AchievementService(this);
        this.leaderboardService = new LeaderboardService(this);
        this.eventService = new EventService(this);

        this.sellGui = new SellGui(this);
        this.shopGui = new ShopGui(this);
        this.collectionGui = new CollectionGui(this);
        this.recycleGui = new RecycleGui(this);
        this.zoneGui = new ZoneGui(this);
        this.missionGui = new MissionGui(this);
        this.achievementGui = new AchievementGui(this);
        this.leaderboardGui = new LeaderboardGui(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(
                new PlayerListeners(this, garbageService, playerService),
                this
        );
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new MobDamageListener(this, garbageService), this);

        // Commands
        getCommand("garbage").setExecutor(new AdminCommands(this, garbageService, playerService));
        getCommand("garbage").setTabCompleter(new AdminTabCompleter(this));
        ItemCommands itemCommands = new ItemCommands(this);
        getCommand("garbageitem").setExecutor(itemCommands);
        getCommand("sell").setExecutor(itemCommands);
        getCommand("shop").setExecutor(itemCommands);
        getCommand("collection").setExecutor(itemCommands);
        getCommand("recycle").setExecutor(itemCommands);
        getCommand("zone").setExecutor(itemCommands);
        getCommand("missions").setExecutor(itemCommands);
        getCommand("achievements").setExecutor(itemCommands);
        getCommand("leaderboard").setExecutor(itemCommands);

        // Start background tasks
        garbageService.start();
        statusBarManager.startRefresh();
        getServer().getScheduler().runTaskTimer(this, () -> powerupService.tick(), 20L, 20L);
        eventService.start();

        getLogger().info("Garbage Collector enabled");
    }

    @Override
    public void onDisable() {
        eventService.stop();
        playerService.saveAll();
        scoreboardManager.removeAll();
        getLogger().info("Garbage Collector disabled");
    }

    public static GarbageCollectorPlugin getInstance() {
        return instance;
    }

    public PlayerService getPlayerService() { return playerService; }
    public GarbageService getGarbageService() { return garbageService; }
    public StatusBarManager getStatusBarManager() { return statusBarManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public SellGui getSellGui() { return sellGui; }
    public ShopGui getShopGui() { return shopGui; }
    public CollectionGui getCollectionGui() { return collectionGui; }
    public RecycleGui getRecycleGui() { return recycleGui; }
    public ZoneGui getZoneGui() { return zoneGui; }
    public MissionGui getMissionGui() { return missionGui; }
    public AchievementGui getAchievementGui() { return achievementGui; }
    public LeaderboardGui getLeaderboardGui() { return leaderboardGui; }
    public LevelService getLevelService() { return levelService; }
    public ZoneService getZoneService() { return zoneService; }
    public RecycleService getRecycleService() { return recycleService; }
    public PowerupService getPowerupService() { return powerupService; }
    public MissionService getMissionService() { return missionService; }
    public AchievementService getAchievementService() { return achievementService; }
    public LeaderboardService getLeaderboardService() { return leaderboardService; }
    public EventService getEventService() { return eventService; }
}