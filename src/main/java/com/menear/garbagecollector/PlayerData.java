package com.menear.garbagecollector;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerData {
    private final UUID uuid;
    private int money;
    private int collected;
    private int bagCapacity;
    private int water;

    public PlayerData(UUID uuid, int money, int collected, int bagCapacity, int water) {
        this.uuid = uuid;
        this.money = money;
        this.collected = collected;
        this.bagCapacity = bagCapacity;
        this.water = water;
    }

    public static PlayerData.load(GarbageCollectorPlugin plugin, UUID uuid) {
        File dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, uuid.toString() + ".yml");
        if (!f.exists()) {
            int startingMoney = plugin.getConfig().getInt("player.startingMoney", 0);
            int capacity = plugin.getConfigManager().getDefaultBagCapacity();
            int maxWater = plugin.getConfigManager().getMaxWater();
            return new PlayerData(uuid, startingMoney, 0, capacity, maxWater);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        int money = cfg.getInt("money", 0);
        int collected = cfg.getInt("collected", 0);
        int capacity = cfg.getInt("bagCapacity", plugin.getConfigManager().getDefaultBagCapacity());
        int water = cfg.getInt("water", plugin.getConfigManager().getMaxWater());
        return new PlayerData(uuid, money, collected, capacity, water);
    }

    public void save(GarbageCollectorPlugin plugin) throws IOException {
        File dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, uuid.toString() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        cfg.set("money", money);
        cfg.set("collected", collected);
        cfg.set("bagCapacity", bagCapacity);
        cfg.set("water", water);
        cfg.save(f);
    }

    public void addMoney(int amount) { this.money += amount; }
    public void addCollected(int amount) { this.collected += amount; }
    public boolean canCollect() { return collected < bagCapacity; }
    public void increaseCollected(int amount) { this.collected += amount; }
    public void decreaseWater(int amount) { this.water = Math.max(0, this.water - amount); }
    public void refillWater(int amount) { this.water = Math.min(this.getMaxWater(), this.water + amount); }
    public int getWater() { return water; }
    public int getMaxWater() { return pluginDefaultMaxWater(); }

    private int pluginDefaultMaxWater() {
        return 100; // fallback
    }

    public String getStatusBar() {
        // Simple action bar: bag / capacity | water%
        int waterPct = Math.round((water / (float)getMaxWater()) * 100);
        return ChatColor.GREEN + "Bag: " + collected + "/" + bagCapacity + ChatColor.GRAY + " | " + ChatColor.AQUA + "Water: " + waterPct + "%";
    }
}
