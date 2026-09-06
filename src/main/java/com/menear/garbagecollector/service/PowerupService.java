package com.menear.garbagecollector.service;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Sfx;
import com.menear.garbagecollector.Stats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Temporary shop powerups. Bought in /shop -> Powerups, they apply while
 * active and expire automatically (expired effects are cleaned by tick()).
 */
public class PowerupService {
    private final GarbageCollectorPlugin plugin;

    public PowerupService(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> ids() {
        var section = Stats.section(plugin, "powerups");
        return section == null ? new ArrayList<>() : new ArrayList<>(section.getKeys(false));
    }

    public boolean exists(String id) {
        return ids().contains(id);
    }

    public String displayName(String id) {
        return plugin.getConfig().getString("powerups." + id + ".displayName", capitalize(id));
    }

    public String icon(String id) {
        return plugin.getConfig().getString("powerups." + id + ".icon", "SUGAR");
    }

    public int cost(String id) {
        return plugin.getConfig().getInt("powerups." + id + ".cost", 100);
    }

    public int durationSeconds(String id) {
        return Math.max(5, plugin.getConfig().getInt("powerups." + id + ".durationSeconds", 60));
    }

    public boolean isActive(PlayerData data, String id) {
        return data.hasPowerup(id);
    }

    public boolean buyAndApply(Player player, PlayerData data, String id) {
        if (!exists(id)) return false;
        int price = cost(id);
        if (!data.hasMoney(price)) {
            player.sendActionBar(ChatColor.RED + "Not enough money! You need $" + price);
            Sfx.play(plugin, player, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            return false;
        }
        data.spend(price);
        data.setPowerup(id, System.currentTimeMillis() + durationSeconds(id) * 1000L);
        player.sendMessage(ChatColor.GREEN + "Activated " + displayName(id).replace("&", "\u00A7")
                + ChatColor.GRAY + " for " + durationSeconds(id) + "s (cost $" + price + ")");
        Sfx.play(plugin, player, "shop", Sound.BLOCK_ANVIL_USE, 0.7f, 1.0f);
        if ("speed".equals(id)) plugin.getShopGui().applySpeed(player, data);
        plugin.getScoreboardManager().updateForPlayer(player, data);
        plugin.getStatusBarManager().updateForPlayer(player, data);
        return true;
    }

    /** Cleans expired powerups and re-applies the base walk speed when Speed Boost ends. */
    public void tick() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
            for (Map.Entry<String, Long> e : data.getActivePowerups().entrySet()) {
                if (e.getValue() > 0 && e.getValue() <= now) {
                    data.removePowerup(e.getKey());
                    if ("speed".equals(e.getKey())) plugin.getShopGui().applySpeed(p, data);
                }
            }
        }
    }

    // ---- Stat hooks used by the collector machine ----
    public double radiusMult(PlayerData data) {
        return isActive(data, "magnet")
                ? plugin.getConfig().getDouble("powerups.magnet.radiusMult", 3.0) : 1.0;
    }

    public int luckBonus(PlayerData data) {
        return isActive(data, "luck")
                ? plugin.getConfig().getInt("powerups.luck.luckBonus", 25) : 0;
    }

    public double valueMult(PlayerData data) {
        return isActive(data, "golden")
                ? plugin.getConfig().getDouble("powerups.golden.valueMult", 2.0) : 1.0;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}