package com.menear.garbagecollector.service;

import com.menear.garbagecollector.PlayerData;

import java.util.UUID;

public interface PlayerService {
    PlayerData getPlayerData(UUID uuid);
    void save(UUID uuid);
    void saveAll();
    void reset(UUID uuid);
}