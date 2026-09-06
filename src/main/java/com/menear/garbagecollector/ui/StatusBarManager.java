package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StatusBarManager {
    private final GarbageCollectorPlugin plugin;
    private final String displayMode;
    private BossBar bossBar;
    private final ConcurrentMap<UUID, Boolean> addedToBoss = new ConcurrentHashMap<>();

    public StatusBarManager(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
        this.displayMode = plugin.getConfig().getString("status.display", "actionbar");
        if (isBossBar()) initBossBar();
    }

    private boolean isBossBar() {
        return "bossbar".equalsIgnoreCase(displayMode);
    }

    private void initBossBar() {
        String colorName = plugin.getConfig().getString("status.bossbar.color", "BLUE");
        String styleName = plugin.getConfig().getString("status.bossbar.style", "SOLID");
        BarColor color = BarColor.valueOf(colorName.toUpperCase());
        BarStyle style = BarStyle.valueOf(styleName.toUpperCase());
        this.bossBar = Bukkit.createBossBar("Garbage Status", color, style);
    }

    public void updateForPlayer(Player player, PlayerData data) {
        String status = data.getStatusBar();
        if (isBossBar()) {
            if (!addedToBoss.getOrDefault(player.getUniqueId(), false)) {
                bossBar.addPlayer(player);
                addedToBoss.put(player.getUniqueId(), true);
            }
            bossBar.setTitle(status);
            float progress = Math.max(0f, Math.min(1f, data.getWater() / (float) data.getMaxWater()));
            bossBar.setProgress(progress);
        } else {
            // action bar
            player.sendActionBar(status);
        }
    }

    public void removeAll() {
        if (bossBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                bossBar.removePlayer(p);
            }
        }
        addedToBoss.clear();
    }
}
