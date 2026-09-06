package com.menear.garbagecollector.service;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Top-player leaderboard. Reads all player files and sorts by the chosen
 * lifetime stat. No live tracking storage needed - files are authoritative.
 */
public class LeaderboardService {
    private final GarbageCollectorPlugin plugin;

    public LeaderboardService(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public static final List<String> SORTS = List.of("money", "collected", "monsters", "luck", "recycled");

    public List<Entry> top(String sort, int limit) {
        List<Entry> list = new ArrayList<>();
        File dir = new File(plugin.getDataFolder(), "players");
        if (!dir.isDirectory()) return list;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return list;
        for (File f : files) {
            String base = f.getName().replace(".yml", "");
            UUID uuid;
            try {
                uuid = UUID.fromString(base);
            } catch (Exception e) {
                continue;
            }
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            int value = 0;
            switch (sort == null ? "money" : sort) {
                case "collected" -> value = cfg.getInt("collected", 0);
                case "money" -> value = cfg.getInt("totalEarned", 0);
                case "monsters" -> value = cfg.getInt("collection.mobKillsTotal", 0);
                case "luck" -> value = cfg.getInt("garbageLuck", 0);
                case "recycled" -> {
                    if (cfg.isConfigurationSection("recycled")) {
                        for (String k : cfg.getConfigurationSection("recycled").getKeys(false)) {
                            value += cfg.getInt("recycled." + k, 0);
                        }
                    }
                }
                default -> value = cfg.getInt("totalEarned", 0);
            }
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null || name.isEmpty()) name = base.substring(0, Math.min(8, base.length()));
            list.add(new Entry(name, value));
        }
        list.sort((a, b) -> Integer.compare(b.value, a.value));
        return limit > 0 && list.size() > limit ? list.subList(0, limit) : list;
    }

    public record Entry(String name, int value) {}
}