package com.menear.garbagecollector.service;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Sfx;
import com.menear.garbagecollector.Stats;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Achievements. Purely config-driven progress checks that unlock once per
 * player and pay out config rewards (money, materials, xp, luck).
 */
public class AchievementService {
    private final GarbageCollectorPlugin plugin;

    public AchievementService(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> ids() {
        var section = Stats.section(plugin, "achievements");
        return section == null ? new ArrayList<>() : new ArrayList<>(section.getKeys(false));
    }

    public void checkAll(Player player, PlayerData data) {
        for (String id : ids()) {
            if (data.hasAchievement(id)) continue;
            if (reached(id, data)) grant(player, data, id);
        }
    }

    private boolean reached(String id, PlayerData data) {
        String p = "achievements." + id;
        String type = plugin.getConfig().getString(p + ".type", "collectTotal");
        int target = plugin.getConfig().getInt(p + ".target", 1);
        return switch (type) {
            case "collectTotal" -> data.getCollected() >= target;
            case "earnTotal" -> data.getTotalEarned() >= target;
            case "killTotal" -> data.mobKillsTotal() >= target;
            case "mythicFound" -> data.getMythicFound() >= target;
            case "collectType" -> data.getGarbage(plugin.getConfig().getString(p + ".targetType", "")) >= target;
            case "killMonster" -> data.getMobKillsById(plugin.getConfig().getString(p + ".monster", "")) >= target;
            default -> false;
        };
    }

    private void grant(Player player, PlayerData data, String id) {
        data.unlockAchievement(id);
        String p = "achievements." + id;
        int money = plugin.getConfig().getInt(p + ".rewardMoney", 0);
        int xp = plugin.getConfig().getInt(p + ".rewardXp", 0);
        int luck = plugin.getConfig().getInt(p + ".rewardLuck", 0);
        Map<String, Integer> materials = new HashMap<>();
        if (plugin.getConfig().isConfigurationSection(p + ".rewardMaterials")) {
            plugin.getConfig().getConfigurationSection(p + ".rewardMaterials").getKeys(false)
                    .forEach(k -> materials.put(k, plugin.getConfig().getInt(p + ".rewardMaterials." + k, 1)));
        }
        if (money > 0) data.addMoney(money);
        if (xp > 0) data.addCollectorXp(xp);
        if (luck > 0) data.addGarbageLuck(luck);
        materials.forEach(data::addRecycled);

        String name = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString(p + ".name", id));
        plugin.getServer().broadcastMessage(ChatColor.GOLD + player.getName() + ChatColor.LIGHT_PURPLE
                + " unlocked achievement " + ChatColor.WHITE + name + ChatColor.LIGHT_PURPLE + "!");
        Sfx.play(plugin, player, "achievement", Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.1f);
        plugin.getScoreboardManager().updateForPlayer(player, data);
        plugin.getStatusBarManager().updateForPlayer(player, data);
    }
}