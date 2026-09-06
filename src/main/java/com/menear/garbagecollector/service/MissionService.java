package com.menear.garbagecollector.service;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Sfx;
import com.menear.garbagecollector.Stats;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily contracts/missions. Each in-game day 3 contracts are picked from
 * config; killing/collecting/recycling garbage progresses them and the
 * rewards can be claimed in the Missions GUI.
 */
public class MissionService {
    private final GarbageCollectorPlugin plugin;

    public MissionService(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("missions.enabled", true);
    }

    public int dailyCount() {
        return Math.max(1, plugin.getConfig().getInt("missions.dailyCount", 3));
    }

    public void refresh(PlayerData data) {
        long day = LocalDate.now().toEpochDay();
        if (data.getLastMissionDay() != day) {
            data.clearMissions();
            data.setLastMissionDay(day);
            List<String> pool = new ArrayList<>(contractIds());
            Collections.shuffle(pool);
            int count = Math.min(dailyCount(), pool.size());
            for (int i = 0; i < count; i++) data.setMission(i, pool.get(i));
        }
    }

    public List<String> contractIds() {
        var section = Stats.section(plugin, "missions.contracts");
        return section == null ? new ArrayList<>() : new ArrayList<>(section.getKeys(false));
    }

    public Contract contract(String id) {
        String p = "missions.contracts." + id;
        if (!plugin.getConfig().isConfigurationSection(p)) return null;
        int rewardMoney = plugin.getConfig().getInt(p + ".rewardMoney", 0);
        int rewardXp = plugin.getConfig().getInt(p + ".rewardXp", 0);
        int rewardLuck = plugin.getConfig().getInt(p + ".rewardLuck", 0);
        Map<String, Integer> materials = new HashMap<>();
        if (plugin.getConfig().isConfigurationSection(p + ".rewardMaterials")) {
            plugin.getConfig().getConfigurationSection(p + ".rewardMaterials").getKeys(false)
                    .forEach(k -> materials.put(k, plugin.getConfig().getInt(p + ".rewardMaterials." + k, 1)));
        }
        return new Contract(
                id,
                plugin.getConfig().getString(p + ".type", "collect"),
                plugin.getConfig().getString(p + ".targetType", null),
                plugin.getConfig().getString(p + ".monster", null),
                Math.max(1, plugin.getConfig().getInt(p + ".target", 1)),
                rewardMoney, rewardXp, rewardLuck, materials);
    }

    /**
     * Advances all open missions that match the event. kind is
     * collect/recycle/kill/findRare, match is the garbage type, monster id
     * or rarity name.
     */
    public void progress(PlayerData data, String kind, String match) {
        if (!enabled()) return;
        for (int i = 0; i < 10; i++) {
            String id = data.getMissionId(i);
            if (id == null) continue;
            Contract c = contract(id);
            if (c == null) continue;
            boolean hit;
            switch (kind) {
                case "collect" -> hit = "collect".equals(c.type) && matches(c.targetType, match);
                case "recycle" -> hit = "recycle".equals(c.type) && matches(c.targetType, match);
                case "kill" -> hit = "kill".equals(c.type)
                        && (c.monster == null || "any".equalsIgnoreCase(c.monster) || c.monster.equalsIgnoreCase(match));
                case "findRare" -> hit = "findRare".equals(c.type)
                        && (match == null || !"common".equalsIgnoreCase(match));
                default -> hit = false;
            }
            if (hit) data.addMissionProgress(i, 1);
        }
    }

    private boolean matches(String target, String match) {
        return target == null || target.isEmpty() || target.equals(match);
    }

    public String description(Contract c) {
        String target = c.targetType != null ? capitalize(c.targetType)
                : (c.monster != null ? capitalize(c.monster) : "any garbage");
        return switch (c.type) {
            case "collect" -> "Collect " + c.target + " of " + target;
            case "recycle" -> "Recycle " + c.target + " of " + target;
            case "kill" -> "Kill " + c.target + " " + (c.monster != null ? capitalize(c.monster).replace("_", " ") : "trash monsters");
            case "findRare" -> "Find " + c.target + " Rare+ garbage";
            default -> c.id;
        };
    }

    public boolean claim(Player player, PlayerData data, int index) {
        String id = data.getMissionId(index);
        if (id == null) return false;
        Contract c = contract(id);
        if (c == null) return false;
        if (data.getMissionProgress(index) < c.target) {
            player.sendActionBar(ChatColor.RED + "Mission not complete (" + data.getMissionProgress(index) + "/" + c.target + ")");
            Sfx.play(plugin, player, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            return false;
        }
        if (data.isMissionClaimed(id)) {
            player.sendActionBar(ChatColor.RED + "Reward already claimed!");
            return false;
        }
        data.claimMission(id);
        StringBuilder reward = new StringBuilder();
        if (c.rewardMoney > 0) {
            data.addMoney(c.rewardMoney);
            reward.append(" +$").append(c.rewardMoney);
        }
        c.rewardMaterials.forEach((k, v) -> {
            data.addRecycled(k, v);
            reward.append(" +").append(v).append(" ").append(k);
        });
        if (c.rewardXp > 0) {
            data.addCollectorXp(c.rewardXp);
            reward.append(" +").append(c.rewardXp).append(" xp");
        }
        if (c.rewardLuck > 0) {
            data.addGarbageLuck(c.rewardLuck);
            reward.append(" +").append(c.rewardLuck).append(" luck");
        }
        player.sendMessage(ChatColor.GREEN + "Mission complete! Rewards:" + reward + ChatColor.GREEN + " claimed!");
        Sfx.play(plugin, player, "mission", Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.3f);
        plugin.getScoreboardManager().updateForPlayer(player, data);
        plugin.getStatusBarManager().updateForPlayer(player, data);
        return true;
    }

    public String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static final class Contract {
        public final String id;
        public final String type;
        public final String targetType;
        public final String monster;
        public final int target;
        public final int rewardMoney;
        public final int rewardXp;
        public final int rewardLuck;
        public final Map<String, Integer> rewardMaterials;

        Contract(String id, String type, String targetType, String monster, int target,
                 int rewardMoney, int rewardXp, int rewardLuck, Map<String, Integer> rewardMaterials) {
            this.id = id;
            this.type = type;
            this.targetType = targetType;
            this.monster = monster;
            this.target = target;
            this.rewardMoney = rewardMoney;
            this.rewardXp = rewardXp;
            this.rewardLuck = rewardLuck;
            this.rewardMaterials = rewardMaterials;
        }
    }
}