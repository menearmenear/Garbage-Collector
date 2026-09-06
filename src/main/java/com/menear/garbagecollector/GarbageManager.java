package com.menear.garbagecollector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.Random;
import java.util.UUID;

public class GarbageManager {
    private final GarbageCollectorPlugin plugin;
    private final Random random = new Random();
    private final NamespacedKey garbageKey;

    public GarbageManager(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
        this.garbageKey = new NamespacedKey(plugin, "garbage-id");
    }

    public void startSpawning() {
        int interval = plugin.getConfigManager().getSpawnIntervalSeconds();
        Bukkit.getScheduler().runTaskTimer(plugin, this::spawnGarbage, 20L, interval * 20L);
    }

    private void spawnGarbage() {
        String worldName = plugin.getConfigManager().getSpawnWorld();
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        // Simple random spawn near spawn location
        Location spawn = world.getSpawnLocation().clone();
        double dx = (random.nextDouble() - 0.5) * 40;
        double dz = (random.nextDouble() - 0.5) * 40;
        spawn.add(dx, 1, dz);

        ArmorStand stand = (ArmorStand) world.spawnEntity(spawn, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setCustomName("Garbage");
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
        stand.setGravity(true);
        stand.getPersistentDataContainer().set(garbageKey, PersistentDataType.STRING, UUID.randomUUID().toString());

        // give it a random item as "trash"
        ItemStack item = new ItemStack(random.nextBoolean() ? Material.PAPER : Material.IRON_NUGGET);
        stand.getEquipment().setHelmet(item);
    }

    public boolean isGarbage(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof ArmorStand)) return false;
        ArmorStand stand = (ArmorStand) entity;
        return stand.getPersistentDataContainer().has(garbageKey, PersistentDataType.STRING);
    }
}
