package com.menear.garbagecollector.service;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;

/**
 * Collector Level progression. XP is earned from collecting garbage,
 * killing monsters and completing missions. Higher levels unlock zones
 * and more valuable garbage.
 */
public class LevelService {
    private final GarbageCollectorPlugin plugin;

    public LevelService(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public int maxLevel() {
        return Math.max(1, plugin.getConfig().getInt("collectorLevels.maxLevel", 50));
    }

    public int perLevelBase() {
        return plugin.getConfig().getInt("collectorLevels.perLevelBase", 100);
    }

    public int perLevelGrowth() {
        return plugin.getConfig().getInt("collectorLevels.perLevelGrowth", 50);
    }

    /** Cumulative XP required to REACH the given level (level 1 starts at 0). */
    public int xpForLevel(int level) {
        if (level <= 1) return 0;
        int xp = 0;
        for (int l = 2; l <= level; l++) {
            xp += perLevelBase() + (l - 2) * perLevelGrowth();
        }
        return xp;
    }

    public int levelForXp(int xp) {
        int level = 1;
        while (level < maxLevel() && xp >= xpForLevel(level + 1)) level++;
        return level;
    }

    public int level(PlayerData data) {
        return levelForXp(data.getCollectorXp());
    }

    public int xpIntoLevel(PlayerData data) {
        return Math.max(0, data.getCollectorXp() - xpForLevel(level(data)));
    }

    public int xpNeededForNext(PlayerData data) {
        int current = level(data);
        if (current >= maxLevel()) return 0;
        return xpForLevel(current + 1) - xpForLevel(current);
    }

    public double progressRatio(PlayerData data) {
        int need = xpNeededForNext(data);
        if (need <= 0) return 1.0;
        return Math.min(1.0, (double) xpIntoLevel(data) / need);
    }
}