package com.menear.garbagecollector.service;

import com.menear.garbagecollector.GarbageCollectorPlugin;
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

public class BasicGarbageService implements GarbageService {
    private final GarbageCollectorPlugin plugin;
    private final Random random = new Random();
    private final NamespacedKey garbageKey;

    public BasicGarbageService(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
        this.garbageKey = new NamespacedKey(plugin, "garbage-id");
    }

    @Override
    public void startSpawning() {
        int interval = plugin.getConfig().getInt("spawn.intervalSeconds", 30);
        Bukkit.getScheduler().runTaskTimer(plugin, this::spawnGarbage, 20L, Math.max(1L, interval) * 20L);
    }

    private void spawnGarbage() {
        String worldName = plugin.getConfig().getString("spawn.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        Location spawn = world.getSpawnLocation().clone();
        double radius = plugin.getConfig().getDouble("spawn.spawnRadius", 20.0);
        double dx = (random.nextDouble() - 0.5) * radius * 2;
        double dz = (random.nextDouble() - 0.5) * radius * 2;
        spawn.add(dx, 1, dz);

        spawnAt(spawn);
    }

    @Override
    public void spawnAt(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        ArmorStand stand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setCustomName("Garbage");
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
        stand.setGravity(true);
        stand.getPersistentDataContainer().set(garbageKey, PersistentDataType.STRING, UUID.randomUUID().toString());

        // choose a random type from config
        if (!plugin.getConfig().isConfigurationSection("garbage.types")) return;
        String[] keys = plugin.getConfig().getConfigurationSection("garbage.types").getKeys(false).toArray(new String[0]);
        if (keys.length == 0) return;
        String pick = keys[random.nextInt(keys.length)];
        String materialName = plugin.getConfig().getString("garbage.types." + pick + ".material", "PAPER");
        Material mat = Material.valueOf(materialName.toUpperCase());
        stand.getEquipment().setHelmet(new ItemStack(mat));
    }

    @Override
    public boolean isGarbage(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof ArmorStand)) return false;
        ArmorStand stand = (ArmorStand) entity;
        return stand.getPersistentDataContainer().has(garbageKey, PersistentDataType.STRING);
    }
}
