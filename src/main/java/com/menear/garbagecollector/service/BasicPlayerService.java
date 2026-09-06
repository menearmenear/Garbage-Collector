package com.menear.garbagecollector.service;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BasicPlayerService implements PlayerService {
    private final GarbageCollectorPlugin plugin;
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();

    public BasicPlayerService(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getOnlinePlayers().forEach(p -> load(p.getUniqueId()));
    }

    @Override
    public PlayerData getPlayerData(UUID uuid) {
        return players.computeIfAbsent(uuid, id -> load(id));
    }

    private PlayerData load(UUID uuid) {
        PlayerData data = PlayerData.load(plugin, uuid);
        players.put(uuid, data);
        return data;
    }

    @Override
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

    @Override
    public void saveAll() {
        players.keySet().forEach(this::save);
    }

    @Override
    public void startWaterDrainTask() {
        int drainPerMinute = plugin.getConfig().getInt("water.drainPerMinute", 1);
        long ticksPerDrain = 20L * 60 / Math.max(1, drainPerMinute);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : players.keySet()) {
                PlayerData data = players.get(id);
                if (data == null) continue;
                data.decreaseWater(1);
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    // Update UI via plugin-managed StatusBarManager later (example usage)
                    p.sendActionBar(data.getStatusBar());
                    if (data.getWater() <= 0) {
                        // apply slowness by adjusting walk speed or potion in future iterations
                        p.setWalkSpeed(0.1f);
                    } else {
                        // reset to default speed
                        p.setWalkSpeed(0.2f);
                    }
                }
            }
        }, 20L * 5, ticksPerDrain);
    }
}
