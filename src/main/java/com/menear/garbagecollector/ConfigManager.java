package com.menear.garbagecollector;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final GarbageCollectorPlugin plugin;
    private final FileConfiguration cfg;

    public ConfigManager(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
        loadDefaults();
    }

    private void loadDefaults() {
        cfg.addDefault("spawn.intervalSeconds", 30);
        cfg.addDefault("spawn.world", "world");
        cfg.addDefault("garbage.types.paper.value", 1);
        cfg.addDefault("garbage.types.can.value", 3);
        cfg.addDefault("player.startingMoney", 0);
        cfg.addDefault("player.defaultBagCapacity", 20);
        cfg.addDefault("player.maxWater", 100);
        cfg.addDefault("water.drainPerMinute", 1);
        cfg.addDefault("water.refillItem", "WATER_BUCKET");
        cfg.addDefault("tool.material", "STICK");
        cfg.addDefault("tool.name", "Trash Grabber");
        cfg.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public int getSpawnIntervalSeconds() {
        return cfg.getInt("spawn.intervalSeconds", 30);
    }

    public String getSpawnWorld() {
        return cfg.getString("spawn.world", "world");
    }

    public int getDefaultBagCapacity() {
        return cfg.getInt("player.defaultBagCapacity", 20);
    }

    public int getMaxWater() {
        return cfg.getInt("player.maxWater", 100);
    }

    public int getWaterDrainPerMinute() {
        return cfg.getInt("water.drainPerMinute", 1);
    }
}
