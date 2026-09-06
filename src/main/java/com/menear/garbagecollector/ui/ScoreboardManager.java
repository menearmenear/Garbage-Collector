package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {
    private final GarbageCollectorPlugin plugin;
    private final Map<UUID, Objective> objectives = new HashMap<>();

    public ScoreboardManager(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public void addPlayer(Player p, PlayerData data) {
        if (p.getScoreboard() == Bukkit.getScoreboardManager().getMainScoreboard()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        Scoreboard sb = p.getScoreboard();
        Objective obj = sb.getObjective("gcstats");
        for (String entry : sb.getEntries()) {
            sb.resetScores(entry);
        }
        if (obj != null) obj.unregister();
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("scoreboard.title", "&6Garbage Collector"));
        obj = sb.registerNewObjective("gcstats", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        objectives.put(p.getUniqueId(), obj);
        updateForPlayer(p, data);
    }

    public void updateForPlayer(Player p, PlayerData data) {
        Objective obj = objectives.get(p.getUniqueId());
        if (obj == null || !p.isOnline()) return;
        // clear
        for (String entry : p.getScoreboard().getEntries()) {
            p.getScoreboard().resetScores(entry);
        }
        int tier = data.getCollectorTier();
        String tierName = plugin.getConfig().getString("garbage.collector.tier" + tier + ".valueMultiplier") != null
                ? "T" + tier : "T" + tier;
        int lineIndex = 0;
        for (String line : plugin.getConfig().getStringList("scoreboard.lines")) {
            String txt = ChatColor.translateAlternateColorCodes('&', line
                    .replace("%money%", String.valueOf(data.getMoney()))
                    .replace("%collected%", String.valueOf(data.getCollected()))
                    .replace("%luck%", String.valueOf(data.getGarbageLuck()))
                    .replace("%tier%", tierName));
            String colored = txt + ChatColor.RESET;
            Score score = obj.getScore(colored);
            score.setScore(10 - lineIndex);
            lineIndex++;
        }
    }

    public void removePlayer(Player p) {
        Objective obj = objectives.remove(p.getUniqueId());
        if (obj != null) {
            for (String entry : p.getScoreboard().getEntries()) {
                p.getScoreboard().resetScores(entry);
            }
            obj.unregister();
        }
        if (p.getScoreboard() != Bukkit.getScoreboardManager().getMainScoreboard()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public void removeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            removePlayer(p);
        }
        objectives.clear();
    }
}