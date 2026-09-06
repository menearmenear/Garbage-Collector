package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
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
        int cap = plugin.getConfig().getInt("garbage.collector.tier" + data.getCollectorTier() + ".bagCapacity",
                plugin.getConfig().getInt("garbage.collector.tier1.bagCapacity", 20));
        return ChatColor.GOLD + "Bag: " + ChatColor.WHITE + data.getCollected() + "/" + cap
                + ChatColor.GRAY + "  " + ChatColor.DARK_GRAY + "|" + ChatColor.GRAY + "  "
                + ChatColor.GREEN + "$" + data.getMoney();
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