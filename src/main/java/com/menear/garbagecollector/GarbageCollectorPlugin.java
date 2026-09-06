package com.menear.garbagecollector;

import com.menear.garbagecollector.commands.AdminCommands;
import com.menear.garbagecollector.commands.AdminTabCompleter;
import com.menear.garbagecollector.commands.ItemCommands;
import com.menear.garbagecollector.service.BasicGarbageService;
import com.menear.garbagecollector.service.BasicPlayerService;
import com.menear.garbagecollector.service.GarbageService;
import com.menear.garbagecollector.service.PlayerService;
import com.menear.garbagecollector.ui.GuiListener;
import com.menear.garbagecollector.ui.ScoreboardManager;
import com.menear.garbagecollector.ui.SellGui;
import com.menear.garbagecollector.ui.ShopGui;
import com.menear.garbagecollector.ui.StatusBarManager;
import com.menear.garbagecollector.ui.CollectionGui;
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

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Services and managers
        this.statusBarManager = new StatusBarManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.playerService = new BasicPlayerService(this);
        this.garbageService = new BasicGarbageService(this);
        this.sellGui = new SellGui(this);
        this.shopGui = new ShopGui(this);
        this.collectionGui = new CollectionGui(this);

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

        // Start background tasks
        garbageService.start();
        statusBarManager.startRefresh();

        getLogger().info("Garbage Collector enabled");
    }

    @Override
    public void onDisable() {
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
}