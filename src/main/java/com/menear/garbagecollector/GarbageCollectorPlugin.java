package com.menear.garbagecollector;

import com.menear.garbagecollector.service.BasicGarbageService;
import com.menear.garbagecollector.service.BasicPlayerService;
import com.menear.garbagecollector.service.GarbageService;
import com.menear.garbagecollector.service.PlayerService;
import com.menear.garbagecollector.ui.StatusBarManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class GarbageCollectorPlugin extends JavaPlugin {

    private static GarbageCollectorPlugin instance;
    private PlayerService playerService;
    private GarbageService garbageService;
    private ConfigManager configManager;
    private StatusBarManager statusBarManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        // Services and managers
        this.statusBarManager = new StatusBarManager(this);
        this.playerService = new BasicPlayerService(this);
        this.garbageService = new BasicGarbageService(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(
                new PlayerListeners(this, garbageService, playerService, statusBarManager),
                this
        );

        // Commands
        getCommand("garbage").setExecutor(new com.menear.garbagecollector.commands.AdminCommands(this, garbageService, playerService));

        // Start background tasks
        garbageService.startSpawning();
        playerService.startWaterDrainTask();

        getLogger().info("Garbage Collector enabled");
    }

    @Override
    public void onDisable() {
        // Save player data
        playerService.saveAll();
        statusBarManager.removeAll();
        getLogger().info("Garbage Collector disabled");
    }

    public static GarbageCollectorPlugin getInstance() {
        return instance;
    }

    public PlayerService getPlayerService() {
        return playerService;
    }

    public GarbageService getGarbageService() {
        return garbageService;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StatusBarManager getStatusBarManager() { return statusBarManager; }
}
