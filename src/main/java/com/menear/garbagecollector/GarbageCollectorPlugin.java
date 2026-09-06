package com.menear.garbagecollector;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class GarbageCollectorPlugin extends JavaPlugin {

    private static GarbageCollectorPlugin instance;
    private PlayerManager playerManager;
    private GarbageManager garbageManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.playerManager = new PlayerManager(this);
        this.garbageManager = new GarbageManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListeners(this), this);

        // Commands
        getCommand("garbage").setExecutor(new GarbageCommand(this));

        // Start background tasks
        garbageManager.startSpawning();
        playerManager.startWaterDrainTask();

        getLogger().info("Garbage Collector enabled");
    }

    @Override
    public void onDisable() {
        // Save player data
        playerManager.saveAll();
        getLogger().info("Garbage Collector disabled");
    }

    public static GarbageCollectorPlugin getInstance() {
        return instance;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public GarbageManager getGarbageManager() {
        return garbageManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
