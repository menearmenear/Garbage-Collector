package com.menear.garbagecollector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private int powerLevel;
    private int efficiencyLevel;
    private int backpackLevel = 1;
    private int toolLevel = 1;
    private int collectorXp;
    private int mythicFound;
    private String activeZone = "";
    private long lastMissionDay;
    private final Map<String, Integer> garbageCount = new HashMap<>();
    private final Map<String, Integer> garbageValue = new HashMap<>();
    private final Map<String, Integer> collectionGarbage = new HashMap<>();
    private final Map<String, Map<Integer, Integer>> collectionMobs = new HashMap<>();
    private final Map<String, Integer> mobKillsById = new HashMap<>();
    private final Set<String> unlockedZones = new HashSet<>();
    private final Set<String> achievements = new HashSet<>();
    private final Set<String> missionClaimed = new HashSet<>();
    private final Map<Integer, String> missions = new HashMap<>();
    private final Map<Integer, Integer> missionProgress = new HashMap<>();
    private final Map<String, Integer> recycled = new HashMap<>();
    private final Map<String, Long> activePowerups = new HashMap<>();
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
            data.powerLevel = Math.max(0, cfg.getInt("powerLevel", 0));
            data.efficiencyLevel = Math.max(0, cfg.getInt("efficiencyLevel", 0));
            data.backpackLevel = Math.max(1, cfg.getInt("backpackLevel", 1));
            data.toolLevel = Math.max(1, cfg.getInt("toolLevel", 1));
            data.collectorXp = Math.max(0, cfg.getInt("collectorXp", 0));
            data.mythicFound = Math.max(0, cfg.getInt("mythicFound", 0));
            data.activeZone = cfg.getString("activeZone", "");
            data.lastMissionDay = cfg.getLong("lastMissionDay", 0);

            loadStringIntMap(cfg, "garbageCount", data.garbageCount);
            loadStringIntMap(cfg, "garbageValue", data.garbageValue);
            loadStringIntMap(cfg, "recycled", data.recycled);
            loadStringIntMap(cfg, "mobKillsById", data.mobKillsById);

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

            data.unlockedZones.addAll(cfg.getStringList("unlockedZones"));
            data.achievements.addAll(cfg.getStringList("achievements"));
            data.missionClaimed.addAll(cfg.getStringList("missionClaimed"));

            if (cfg.isConfigurationSection("missions")) {
                cfg.getConfigurationSection("missions").getKeys(false).forEach(k ->
                        data.missions.put(Integer.parseInt(k), cfg.getString("missions." + k, "")));
            }
            if (cfg.isConfigurationSection("missionProgress")) {
                cfg.getConfigurationSection("missionProgress").getKeys(false).forEach(k ->
                        data.missionProgress.put(Integer.parseInt(k), cfg.getInt("missionProgress." + k, 0)));
            }
            if (cfg.isConfigurationSection("activePowerups")) {
                cfg.getConfigurationSection("activePowerups").getKeys(false).forEach(k ->
                        data.activePowerups.put(k, cfg.getLong("activePowerups." + k, 0)));
            }
        }
        return data;
    }

    private static void loadStringIntMap(FileConfiguration cfg, String path, Map<String, Integer> into) {
        if (cfg.isConfigurationSection(path)) {
            cfg.getConfigurationSection(path).getKeys(false).forEach(k ->
                    into.put(k, cfg.getInt(path + "." + k, 0)));
        }
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
        cfg.set("powerLevel", powerLevel);
        cfg.set("efficiencyLevel", efficiencyLevel);
        cfg.set("backpackLevel", backpackLevel);
        cfg.set("toolLevel", toolLevel);
        cfg.set("collectorXp", collectorXp);
        cfg.set("mythicFound", mythicFound);
        cfg.set("activeZone", activeZone);
        cfg.set("lastMissionDay", lastMissionDay);

        cfg.set("garbageCount.data", null);
        saveStringIntMap(cfg, "garbageCount", garbageCount);
        cfg.set("garbageValue", null);
        saveStringIntMap(cfg, "garbageValue", garbageValue);
        cfg.set("recycled", null);
        saveStringIntMap(cfg, "recycled", recycled);
        cfg.set("mobKillsById", null);
        saveStringIntMap(cfg, "mobKillsById", mobKillsById);

        cfg.set("collection.garbage", null);
        collectionGarbage.forEach((k, v) -> cfg.set("collection.garbage." + k, v));
        cfg.set("collection.mobs", null);
        collectionMobs.forEach((mob, tiers) ->
                tiers.forEach((tier, count) -> cfg.set("collection.mobs." + mob + "." + tier, count)));
        cfg.set("collection.mobKillsTotal", mobKillsTotal);

        cfg.set("unlockedZones", null);
        cfg.set("unlockedZones", new java.util.ArrayList<>(unlockedZones));
        cfg.set("achievements", null);
        cfg.set("achievements", new java.util.ArrayList<>(achievements));
        cfg.set("missionClaimed", null);
        cfg.set("missionClaimed", new java.util.ArrayList<>(missionClaimed));
        cfg.set("missions", null);
        missions.forEach((k, v) -> cfg.set("missions." + k, v));
        cfg.set("missionProgress", null);
        missionProgress.forEach((k, v) -> cfg.set("missionProgress." + k, v));
        cfg.set("activePowerups", null);
        activePowerups.forEach((k, v) -> cfg.set("activePowerups." + k, v));

        cfg.save(f);
    }

    private static void saveStringIntMap(FileConfiguration cfg, String path, Map<String, Integer> map) {
        map.forEach((k, v) -> cfg.set(path + "." + k, v));
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

    public int getPowerLevel() { return powerLevel; }
    public void setPowerLevel(int powerLevel) { this.powerLevel = Math.max(0, powerLevel); }

    public int getEfficiencyLevel() { return efficiencyLevel; }
    public void setEfficiencyLevel(int efficiencyLevel) { this.efficiencyLevel = Math.max(0, efficiencyLevel); }

    public int getBackpackLevel() { return backpackLevel; }
    public void setBackpackLevel(int backpackLevel) { this.backpackLevel = Math.max(1, backpackLevel); }

    public int getToolLevel() { return toolLevel; }
    public void setToolLevel(int toolLevel) { this.toolLevel = Math.max(1, toolLevel); }

    public int getCollectorXp() { return collectorXp; }
    public void addCollectorXp(int amount) { this.collectorXp += Math.max(0, amount); }

    public int getMythicFound() { return mythicFound; }
    public void addMythicFound(int amount) { this.mythicFound += Math.max(0, amount); }

    public String getActiveZone() { return activeZone == null ? "" : activeZone; }
    public void setActiveZone(String activeZone) { this.activeZone = activeZone == null ? "" : activeZone; }

    public boolean hasZone(String zone) { return unlockedZones.contains(zone); }
    public Set<String> getUnlockedZones() { return new java.util.HashSet<>(unlockedZones); }
    public boolean unlockZone(String zone) {
        if (unlockedZones.contains(zone)) return false;
        unlockedZones.add(zone);
        return true;
    }

    public boolean hasAchievement(String id) { return achievements.contains(id); }
    public Set<String> getAchievements() { return new java.util.HashSet<>(achievements); }
    public void unlockAchievement(String id) { achievements.add(id); }

    // ---- Bag (garbage count + value per type) ----
    public void addGarbage(String type, int n) { garbageCount.merge(type, n, Integer::sum); }

    public void addGarbageValue(String type, int value) { garbageValue.merge(type, value, Integer::sum); }

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

    // ---- Recycled materials ----
    public int getRecycled(String material) { return recycled.getOrDefault(material, 0); }
    public Map<String, Integer> getRecycledAll() { return new HashMap<>(recycled); }
    public void addRecycled(String material, int amount) { recycled.merge(material, Math.max(0, amount), Integer::sum); }
    public boolean hasRecycled(String material, int amount) { return getRecycled(material) >= amount; }
    public boolean spendRecycled(String material, int amount) {
        if (!hasRecycled(material, amount)) return false;
        recycled.merge(material, -amount, Integer::sum);
        if (recycled.get(material) <= 0) recycled.remove(material);
        return true;
    }
    public int totalRecycled() { return recycled.values().stream().mapToInt(Integer::intValue).sum(); }

    // ---- Powerups (id -> expiry millis) ----
    public boolean hasPowerup(String id) {
        Long expiry = activePowerups.get(id);
        return expiry != null && expiry > System.currentTimeMillis();
    }
    public long getPowerupExpiry(String id) { return activePowerups.getOrDefault(id, 0L); }
    public void setPowerup(String id, long expiryMillis) { activePowerups.put(id, expiryMillis); }
    public void removePowerup(String id) { activePowerups.remove(id); }
    public Map<String, Long> getActivePowerups() { return new HashMap<>(activePowerups); }

    // ---- Missions (daily contracts) ----
    public String getMissionId(int index) { return missions.get(index); }
    public void setMission(int index, String id) { missions.put(index, id); missionProgress.putIfAbsent(index, 0); }
    public int getMissionProgress(int index) { return missionProgress.getOrDefault(index, 0); }
    public void addMissionProgress(int index, int amount) { missionProgress.merge(index, amount, Integer::sum); }
    public boolean isMissionClaimed(String id) { return missionClaimed.contains(id); }
    public void claimMission(String id) { missionClaimed.add(id); }
    public void clearMissions() { missions.clear(); missionProgress.clear(); missionClaimed.clear(); }
    public long getLastMissionDay() { return lastMissionDay; }
    public void setLastMissionDay(long day) { this.lastMissionDay = day; }

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
    public void addMobKillById(String id) { mobKillsById.merge(id, 1, Integer::sum); }
    public int getMobKillsById(String id) { return mobKillsById.getOrDefault(id, 0); }
    public Map<String, Integer> getMobKillsById() { return new HashMap<>(mobKillsById); }
    public Map<String, Map<Integer, Integer>> getCollectionMobs() {
        Map<String, Map<Integer, Integer>> copy = new HashMap<>();
        collectionMobs.forEach((mob, tiers) -> copy.put(mob, new HashMap<>(tiers)));
        return copy;
    }
    public int mobKillsTotal() { return mobKillsTotal; }
}