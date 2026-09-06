package com.menear.garbagecollector.service;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.Sfx;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Random world events (garbage truck, toxic spill, golden trash, trash king).
 * Fires a weighted random event every minInterval-maxInterval minutes.
 */
public class EventService {
    private final GarbageCollectorPlugin plugin;
    private final Random random = new Random();
    private BukkitTask task;
    private long nextAt = -1;

    public EventService(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!enabled()) return;
        scheduleNext();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("events.enabled", true);
    }

    private void scheduleNext() {
        int min = plugin.getConfig().getInt("events.minIntervalMinutes", 10);
        int max = plugin.getConfig().getInt("events.maxIntervalMinutes", 30);
        int minutes = min + random.nextInt(Math.max(1, max - min + 1));
        nextAt = System.currentTimeMillis() + minutes * 60_000L;
    }

    private void tick() {
        if (nextAt <= 0 || System.currentTimeMillis() < nextAt) return;
        fire(pickEvent());
        scheduleNext();
    }

    private String pickEvent() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("events");
        if (section == null) return null;
        List<String> ids = section.getKeys(false).stream()
                .filter(section::isConfigurationSection).toList();
        if (ids.isEmpty()) return null;
        Map<String, Integer> weights = new HashMap<>();
        int total = 0;
        for (String id : ids) {
            int w = Math.max(0, section.getInt(id + ".weight", 0));
            weights.put(id, w);
            total += w;
        }
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (String id : ids) {
            roll -= weights.get(id);
            if (roll < 0) return id;
        }
        return null;
    }

    private void fire(String id) {
        if (id == null) return;
        GarbageService service = plugin.getGarbageService();
        long durationMs = plugin.getConfig().getLong("events." + id + ".durationSeconds", 60) * 1000L;
        switch (id) {
            case "garbage_truck" -> {
                Player target = randomPlayer();
                if (target == null) return;
                int amount = plugin.getConfig().getInt("events." + id + ".amount", 30);
                broadcast(ChatColor.GOLD + "GARBAGE TRUCK", ChatColor.GRAY + "A garbage truck dumped its load near " + target.getName() + "!");
                Sfx.play(plugin, target.getLocation(), "event", Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.8f);
                service.spawnZoneBurst(target, target.getLocation(), amount);
            }
            case "toxic_spill" -> {
                service.setToxicActive(System.currentTimeMillis() + durationMs);
                broadcast(ChatColor.GREEN + "TOXIC SPILL", ChatColor.DARK_GREEN + "Toxic garbage is everywhere for the next " + (durationMs / 1000) + "s!");
                playEventSound();
            }
            case "golden_trash" -> {
                service.setGoldenActive(System.currentTimeMillis() + durationMs);
                broadcast(ChatColor.GOLD + "GOLDEN TRASH", ChatColor.YELLOW + "All garbage is worth 2x for the next " + (durationMs / 1000) + "s!");
                playEventSound();
            }
            case "trash_king" -> {
                Player target = randomPlayer();
                if (target == null) return;
                broadcast(ChatColor.RED + "THE LANDFILL KING", ChatColor.DARK_RED + target.getName() + " has drawn the Landfill King!");
                Sfx.play(plugin, target.getLocation(), "event", Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.8f);
                service.spawnMiniBoss(target);
            }
        }
    }

    private void broadcast(String title, String subtitle) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, 10, 60, 10);
        }
    }

    private void playEventSound() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Sfx.play(plugin, p.getLocation(), "event", Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.8f);
        }
    }

    private Player randomPlayer() {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return null;
        return online.get(random.nextInt(online.size()));
    }
}