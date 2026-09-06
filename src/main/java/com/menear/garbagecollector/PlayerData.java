package com.menear.garbagecollector;

import org.bukkit.ChatColor;

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
    private final int maxWater;

    public PlayerData(UUID uuid, int money, int collected, int bagCapacity, int water, int maxWater) {
        this.uuid = uuid;
        this.money = money;
        this.collected = collected;
        this.bagCapacity = bagCapacity;
        this.water = water;
        this.maxWater = maxWater;
    }

    public static PlayerData load(GarbageCollectorPlugin plugin, UUID uuid) {
        File dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, uuid.toString() + ".yml");
        int maxWaterConfig = plugin.getConfig().getInt("player.maxWater", 100);

        if (!f.exists()) {
            int startingMoney = plugin.getConfig().getInt("player.startingMoney", 0);
            int capacity = plugin.getConfig().getInt("player.defaultBagCapacity", 20);
            return new PlayerData(uuid, startingMoney, 0, capacity, maxWaterConfig, maxWaterConfig);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        int money = cfg.getInt("money", 0);
        int collected = cfg.getInt("collected", 0);
        int capacity = cfg.getInt("bagCapacity", plugin.getConfig().getInt("player.defaultBagCapacity", 20));
        int water = cfg.getInt("water", maxWaterConfig);
        return new PlayerData(uuid, money, collected, capacity, water, maxWaterConfig);
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

    // getters / setters and utilities

    public void addMoney(int amount) { this.money += amount; }
    public void addCollected(int amount) { this.collected += amount; }
    public boolean canCollect() { return collected < bagCapacity; }
    public void increaseCollected(int amount) { this.collected += amount; }
    public void decreaseWater(int amount) { this.water = Math.max(0, this.water - amount); }
    public void refillWater(int amount) { this.water = Math.min(this.getMaxWater(), this.water + amount); }
    public int getWater() { return water; }
    public int getMaxWater() { return maxWater; }

    public int getMoney() { return money; }
    public int getCollected() { return collected; }
    public int getBagCapacity() { return bagCapacity; }

    public String getStatusBar() {
        int waterPct = Math.round((water / (float)getMaxWater()) * 100);
        return ChatColor.GREEN + "Bag: " + collected + "/" + bagCapacity + ChatColor.GRAY + " | " + ChatColor.AQUA + "Water: " + waterPct + "%";
    }
}
