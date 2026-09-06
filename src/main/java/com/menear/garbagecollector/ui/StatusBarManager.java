package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Stats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class StatusBarManager {
    private final GarbageCollectorPlugin plugin;

    public StatusBarManager(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateForPlayer(Player p, PlayerData data) {
        if (p == null || !p.isOnline()) return;
        p.sendActionBar(build(data));
    }

    public String build(PlayerData data) {
        int cap = Stats.capacity(plugin, data);
        return ChatColor.GOLD + "Bag: " + ChatColor.WHITE + data.totalGarbage() + "/" + cap
                + ChatColor.GRAY + "  " + ChatColor.DARK_GRAY + "|" + ChatColor.GRAY + "  "
                + ChatColor.GREEN + "$" + data.getMoney()
                + ChatColor.GRAY + "  " + ChatColor.DARK_GRAY + "|" + ChatColor.GRAY + "  "
                + ChatColor.AQUA + Stats.zoneDisplay(plugin, plugin.getZoneService().activeZone(data));
    }

    public void startRefresh() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOnline()) {
                    PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
                    p.sendActionBar(build(data));
                }
            }
        }, 20L, 20L);
    }

    public void removeAll() {
    }
}