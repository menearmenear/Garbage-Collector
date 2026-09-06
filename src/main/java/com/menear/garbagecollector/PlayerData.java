package com.menear.garbagecollector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerData {
    private final UUID uuid;
    private int money;
    private int totalEarned;
    private int collected;
    private int collectorTier = 1;
    private int garbageLuck;
    private int speedLevel = 1;
    private int magnetLevel;
    private int pickupLevel;
    private int cooldownLevel;
    private final Map<String, Integer> garbageCount = new HashMap<>();
    private final Map<String, Integer> garbageValue = new HashMap<>();
    private final Map<String, Integer> collectionGarbage = new HashMap<>();
    private final Map<String, Map<Integer, Integer>> collectionMobs = new HashMap<>();
    private int mobKillsTotal;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public static PlayerData load(GarbageCollectorPlugin plugin, UUID uuid) {
        File dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, uuid.toString() + ".yml");
        PlayerData data = new PlayerData(uuid);
        if (f.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            data.money = cfg.getInt("money", 0);
            data.totalEarned = cfg.getInt("totalEarned", 0);
            data.collected = cfg.getInt("collected", 0);
            data.collectorTier = Math.max(1, cfg.getInt("collectorTier", 1));
            data.garbageLuck = cfg.getInt("garbageLuck", 0);
            data.speedLevel = Math.max(1, cfg.getInt("speedLevel", 1));
            data.magnetLevel = Math.max(0, cfg.getInt("magnetLevel", 0));
            data.pickupLevel = Math.max(0, cfg.getInt("pickupLevel", 0));
            data.cooldownLevel = Math.max(0, cfg.getInt("cooldownLevel", 0));
            if (cfg.isConfigurationSection("garbageCount")) {
                cfg.getConfigurationSection("garbageCount").getKeys(false).forEach(k ->
                        data.garbageCount.put(k, cfg.getInt("garbageCount." + k, 0)));
            }
            if (cfg.isConfigurationSection("garbageValue")) {
                cfg.getConfigurationSection("garbageValue").getKeys(false).forEach(k ->
                        data.garbageValue.put(k, cfg.getInt("garbageValue." + k, 0)));
            }
            if (cfg.isConfigurationSection("collection.garbage")) {
                cfg.getConfigurationSection("collection.garbage").getKeys(false).forEach(k ->
                        data.collectionGarbage.put(k, cfg.getInt("collection.garbage." + k, 0)));
            }
            if (cfg.isConfigurationSection("collection.mobs")) {
                cfg.getConfigurationSection("collection.mobs").getKeys(false).forEach(mob ->
                        cfg.getConfigurationSection("collection.mobs." + mob).getKeys(false).forEach(tier ->
                                data.collectionMobs.computeIfAbsent(mob, x -> new HashMap<>())
                                        .put(Integer.parseInt(tier), cfg.getInt("collection.mobs." + mob + "." + tier, 0))));
            }
            data.mobKillsTotal = cfg.getInt("collection.mobKillsTotal", 0);
        }
        return data;
    }

    public void save(GarbageCollectorPlugin plugin) throws IOException {
        File dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, uuid.toString() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        cfg.set("money", money);
        cfg.set("totalEarned", totalEarned);
        cfg.set("collected", collected);
        cfg.set("collectorTier", collectorTier);
        cfg.set("garbageLuck", garbageLuck);
        cfg.set("speedLevel", speedLevel);
        cfg.set("magnetLevel", magnetLevel);
        cfg.set("pickupLevel", pickupLevel);
        cfg.set("cooldownLevel", cooldownLevel);
        cfg.set("garbageCount.data", null);
        String base = "garbageCount.";
        garbageCount.forEach((k, v) -> cfg.set(base + k, v));
        cfg.set("garbageValue", null);
        garbageValue.forEach((k, v) -> cfg.set("garbageValue." + k, v));
        cfg.set("collection.garbage", null);
        collectionGarbage.forEach((k, v) -> cfg.set("collection.garbage." + k, v));
        cfg.set("collection.mobs", null);
        collectionMobs.forEach((mob, tiers) ->
                tiers.forEach((tier, count) -> cfg.set("collection.mobs." + mob + "." + tier, count)));
        cfg.set("collection.mobKillsTotal", mobKillsTotal);
        cfg.save(f);
    }

    public void delete(GarbageCollectorPlugin plugin) {
        File f = new File(new File(plugin.getDataFolder(), "players"), uuid.toString() + ".yml");
        if (f.exists()) f.delete();
    }

    // Getters / setters
    public UUID getUuid() { return uuid; }
    public int getMoney() { return money; }
    public void addMoney(int amount) { this.money += amount; }
    public boolean hasMoney(int amount) { return money >= amount; }
    public boolean spend(int amount) {
        if (money < amount) return false;
        money -= amount;
        return true;
    }

    public int getTotalEarned() { return totalEarned; }
    public void addTotalEarned(int amount) { this.totalEarned += amount; }

    public int getCollected() { return collected; }
    public void addCollected(int amount) { this.collected += amount; }

    public int getCollectorTier() { return collectorTier; }
    public void setCollectorTier(int collectorTier) { this.collectorTier = Math.max(1, collectorTier); }

    public int getGarbageLuck() { return garbageLuck; }
    public void setGarbageLuck(int garbageLuck) { this.garbageLuck = Math.max(0, garbageLuck); }
    public void addGarbageLuck(int amount) { this.garbageLuck = Math.max(0, this.garbageLuck + amount); }

    public int getSpeedLevel() { return speedLevel; }
    public void setSpeedLevel(int speedLevel) { this.speedLevel = Math.max(1, speedLevel); }

    public int getMagnetLevel() { return magnetLevel; }
    public void setMagnetLevel(int magnetLevel) { this.magnetLevel = Math.max(0, magnetLevel); }

    public int getPickupLevel() { return pickupLevel; }
    public void setPickupLevel(int pickupLevel) { this.pickupLevel = Math.max(0, pickupLevel); }

    public int getCooldownLevel() { return cooldownLevel; }
    public void setCooldownLevel(int cooldownLevel) { this.cooldownLevel = Math.max(0, cooldownLevel); }

    public void addGarbage(String type, int n) {
        garbageCount.merge(type, n, Integer::sum);
    }

    public void addGarbageValue(String type, int value) {
        garbageValue.merge(type, value, Integer::sum);
    }

    public int getGarbageValue(String type) { return garbageValue.getOrDefault(type, 0); }

    public Map<String, Integer> getGarbageValueAll() { return new HashMap<>(garbageValue); }

    public int totalGarbageValue() {
        return garbageValue.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getGarbage(String type) { return garbageCount.getOrDefault(type, 0); }

    public Map<String, Integer> getGarbageCount() { return new HashMap<>(garbageCount); }

    public int totalGarbage() { return garbageCount.values().stream().mapToInt(Integer::intValue).sum(); }

    public void clearGarbage() {
        garbageCount.clear();
        garbageValue.clear();
    }

    public void clearGarbageType(String type) {
        garbageCount.put(type, 0);
        garbageValue.put(type, 0);
    }

    // ---- Collection (lifetime) tracking ----
    public void addCollectionGarbage(String type, int n) {
        collectionGarbage.merge(type, n, Integer::sum);
    }
    public Map<String, Integer> getCollectionGarbage() { return new HashMap<>(collectionGarbage); }
    public int collectionGarbageTotal() {
        return collectionGarbage.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void addMobKill(String mobType, int tier) {
        collectionMobs.computeIfAbsent(mobType, x -> new HashMap<>())
                .merge(tier, 1, Integer::sum);
        mobKillsTotal++;
    }
    public Map<String, Map<Integer, Integer>> getCollectionMobs() {
        Map<String, Map<Integer, Integer>> copy = new HashMap<>();
        collectionMobs.forEach((mob, tiers) -> copy.put(mob, new HashMap<>(tiers)));
        return copy;
    }
    public int mobKillsTotal() { return mobKillsTotal; }
}