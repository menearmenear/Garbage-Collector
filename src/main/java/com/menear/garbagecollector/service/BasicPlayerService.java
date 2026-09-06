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
        Bukkit.getOnlinePlayers().forEach(p -> getPlayerData(p.getUniqueId()));
    }

    @Override
    public PlayerData getPlayerData(UUID uuid) {
        return players.computeIfAbsent(uuid, id -> PlayerData.load(plugin, id));
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
    public void reset(UUID uuid) {
        PlayerData data = players.remove(uuid);
        if (data != null) data.delete(plugin);
        PlayerData fresh = PlayerData.load(plugin, uuid);
        players.put(uuid, fresh);
    }
}
