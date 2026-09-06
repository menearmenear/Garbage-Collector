package com.menear.garbagecollector;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Central place for computing the "collector machine" stats shared by the
 * service, the GUIs and the item lore. All numbers come from config.
 */
public final class Stats {
    private Stats() {}

    public static List<String> toolOrder(GarbageCollectorPlugin plugin) {
        List<String> order = new ArrayList<>();
        for (String s : plugin.getConfig().getStringList("tools.order")) {
            if (plugin.getConfig().isConfigurationSection("tools." + s)) order.add(s);
        }
        if (order.isEmpty() && plugin.getConfig().isConfigurationSection("tools")) {
            order.addAll(plugin.getConfig().getConfigurationSection("tools").getKeys(false));
        }
        return order;
    }

    public static String toolId(GarbageCollectorPlugin plugin, int level) {
        List<String> order = toolOrder(plugin);
        if (level < 1) level = 1;
        return level <= order.size() ? order.get(level - 1) : order.get(order.size() - 1);
    }

    public static int toolBonus(GarbageCollectorPlugin plugin, int toolLevel, String key) {
        if (!plugin.getConfig().isConfigurationSection("tools." + toolId(plugin, toolLevel))) return 0;
        return plugin.getConfig().getInt("tools." + toolId(plugin, toolLevel) + "." + key, 0);
    }

    /** Total Power = Power upgrades + current grabber bonus. */
    public static int power(GarbageCollectorPlugin plugin, PlayerData data) {
        return data.getPowerLevel() + toolBonus(plugin, data.getToolLevel(), "power");
    }

    /** Total Efficiency = Efficiency upgrades + legacy Quick Hands/Cooldown + grabber bonus. */
    public static int efficiency(GarbageCollectorPlugin plugin, PlayerData data) {
        return data.getEfficiencyLevel() + data.getPickupLevel() + data.getCooldownLevel()
                + toolBonus(plugin, data.getToolLevel(), "efficiency");
    }

    /** Total Luck = upgrades + grabber bonus (active Lucky Hour powerup adds on top when rolling). */
    public static int luck(GarbageCollectorPlugin plugin, PlayerData data) {
        return data.getGarbageLuck() + toolBonus(plugin, data.getToolLevel(), "luck");
    }

    /** Total Magnet level = upgrades + grabber bonus. */
    public static int magnet(GarbageCollectorPlugin plugin, PlayerData data) {
        return data.getMagnetLevel() + toolBonus(plugin, data.getToolLevel(), "magnet");
    }

    public static double backpackMult(GarbageCollectorPlugin plugin, int backpackLevel) {
        return Math.max(0.1, plugin.getConfig().getDouble("backpacks.levels." + backpackLevel + ".capacityMult", 1.0));
    }

    public static double tierValueMultiplier(GarbageCollectorPlugin plugin, PlayerData data) {
        String path = "garbage.collector.tier" + data.getCollectorTier() + ".valueMultiplier";
        return plugin.getConfig().getDouble(path,
                plugin.getConfig().getDouble("garbage.collector.tier1.valueMultiplier", 1.0));
    }

    public static int tierBagCapacity(GarbageCollectorPlugin plugin, PlayerData data) {
        String path = "garbage.collector.tier" + data.getCollectorTier() + ".bagCapacity";
        return plugin.getConfig().getInt(path,
                plugin.getConfig().getInt("garbage.collector.tier1.bagCapacity", 20));
    }

    /** Total bag capacity = tier capacity x backpack multiplier (infinite while Infinite Bag is active). */
    public static int capacity(GarbageCollectorPlugin plugin, PlayerData data) {
        if (data.hasPowerup("infinitebag")) return Integer.MAX_VALUE;
        int base = tierBagCapacity(plugin, data);
        return (int) Math.max(1, base * backpackMult(plugin, data.getBackpackLevel()));
    }

    public static long requiredHoldMillis(GarbageCollectorPlugin plugin, PlayerData data) {
        long base = plugin.getConfig().getLong("collect.baseHoldMillis", 3000);
        long min = plugin.getConfig().getLong("collect.minHoldMillis", 400);
        long red = plugin.getConfig().getLong("collect.holdReductionPerLevel", 400);
        return Math.max(min, base - efficiency(plugin, data) * red);
    }

    public static long effectiveCooldownMillis(GarbageCollectorPlugin plugin, PlayerData data) {
        long base = plugin.getConfig().getLong("collect.baseCooldownMillis", 2000);
        long min = plugin.getConfig().getLong("collect.minCooldownMillis", 300);
        long red = plugin.getConfig().getLong("collect.cooldownReductionPerLevel", 350);
        return Math.max(min, base - efficiency(plugin, data) * red);
    }

    /** Is the given garbage type reachable with this player's Power? */
    public static boolean hasPowerFor(GarbageCollectorPlugin plugin, PlayerData data, String type) {
        int require = plugin.getConfig().getInt("garbage.types." + type + ".requiresPower", 0);
        return require <= 0 || power(plugin, data) >= require;
    }

    public static boolean collectible(GarbageCollectorPlugin plugin, String type) {
        return plugin.getConfig().getBoolean("garbage.types." + type + ".collectible", false);
    }

    // ---- Rarity helpers ----
    public static boolean isMythic(GarbageCollectorPlugin plugin, String rarityName) {
        return plugin.getConfig().getBoolean("garbage.rarities." + (rarityName == null ? "" : rarityName) + ".mythic", false);
    }

    public static String zoneIcon(GarbageCollectorPlugin plugin, String zoneId) {
        return plugin.getConfig().getString("zones." + zoneId + ".icon", "BIRCH_PLANKS");
    }

    public static String zoneDisplay(GarbageCollectorPlugin plugin, String zoneId) {
        return plugin.getConfig().getString("zones." + zoneId + ".displayName",
                "&7" + (zoneId == null ? "Unknown" : zoneId));
    }

    public static ConfigurationSection section(GarbageCollectorPlugin plugin, String path) {
        return plugin.getConfig().isConfigurationSection(path)
                ? plugin.getConfig().getConfigurationSection(path) : null;
    }
}