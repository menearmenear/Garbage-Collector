package com.menear.garbagecollector.service;

import org.bukkit.entity.Entity;
import org.bukkit.location.Location;

public interface GarbageService {
    void startSpawning();
    void spawnAt(Location location);
    boolean isGarbage(Entity entity);
}
