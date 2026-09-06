package com.menear.garbagecollector;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

public class PlayerListeners implements Listener {
    private final GarbageCollectorPlugin plugin;
    private final NamespacedKey garbageKey;

    public PlayerListeners(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
        this.garbageKey = new NamespacedKey(plugin, "garbage-id");
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity ent = event.getRightClicked();
        Player p = event.getPlayer();
        if (!(ent instanceof ArmorStand)) return;
        ArmorStand stand = (ArmorStand) ent;
        if (!stand.getPersistentDataContainer().has(garbageKey, PersistentDataType.STRING)) return;

        PlayerData data = plugin.getPlayerManager().get(p.getUniqueId());
        if (!data.canCollect()) {
            p.sendActionBar("\u00A7cBag full!");
            return;
        }

        // collect
        data.addMoney(1);
        data.increaseCollected(1);
        stand.remove();
        p.sendActionBar("\u00A7aCollected garbage! Money: " + data.getStatusBar());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerManager().get(event.getPlayer().getUniqueId());
        // give basic tool if not present
        ItemStack tool = new ItemStack(Material.STICK);
        event.getPlayer().getInventory().addItem(tool);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerManager().save(event.getPlayer().getUniqueId());
    }
}
