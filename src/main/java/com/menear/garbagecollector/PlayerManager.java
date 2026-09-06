package com.menear.garbagecollector;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private final GarbageCollectorPlugin plugin;
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();

    public PlayerManager(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
        // load online players
        Bukkit.getOnlinePlayers().forEach(p -> load(p.getUniqueId()));
    }

    public PlayerData get(UUID uuid) {
        return players.computeIfAbsent(uuid, id -> load(id));
    }

    private PlayerData load(UUID uuid) {
        PlayerData data = PlayerData.load(plugin, uuid);
        players.put(uuid, data);
        return data;
    }

    public void save(UUID uuid) {
        PlayerData data = players.get(uuid);
        if (data != null) {
            try {
                data.save(plugin);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
            }
        }
    }

    public void saveAll() {
        players.keySet().forEach(this::save);
    }

    public void startWaterDrainTask() {
        int drainPerMinute = plugin.getConfigManager().getWaterDrainPerMinute();
        long ticksPerDrain = 20L * 60 / Math.max(1, drainPerMinute);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : players.keySet()) {
                PlayerData data = players.get(id);
                if (data == null) continue;
                data.decreaseWater(1);
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.sendActionBar(data.getStatusBar());
                    if (data.getWater() <= 0) {
                        p.setWalkSpeed(0.1f); // slow
                    }
                }
            }
        }, 20L * 5, ticksPerDrain);
    }
}
