package com.menear.garbagecollector.service;

import com.menear.garbagecollector.ActiveGarbage;
import com.menear.garbagecollector.CollectionResult;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface GarbageService {
    void start();
    void spawnAt(Location location);
    void spawnAt(Location location, String forcedType);
    void spawnBurst(Location location, int amount);
    void spawnZoneBurst(Player forPlayer, Location location, int amount);
    void spawnMiniBoss(Player target);
    void setToxicActive(long until);
    boolean isToxicActive();
    void setGoldenActive(long until);
    boolean isGoldenActive();
    boolean isGarbage(Entity entity);
    ActiveGarbage findGarbage(Entity entity);
    CollectionResult collect(Player player, ActiveGarbage garbage);
    void interact(Player player, ActiveGarbage garbage);
    void handleMobKilled(Player killer, ActiveGarbage garbage);
    void updateGlow();
    void despawnExpired();
    void updateMobName(org.bukkit.entity.LivingEntity mob, ActiveGarbage garbage, int tier, int maxTiers);
    void reload();
}