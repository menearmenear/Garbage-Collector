package com.menear.garbagecollector;

import com.menear.garbagecollector.service.GarbageService;
import com.menear.garbagecollector.service.PlayerService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerListeners implements Listener {
    private final GarbageCollectorPlugin plugin;
    private final GarbageService garbageService;
    private final PlayerService playerService;

    public PlayerListeners(GarbageCollectorPlugin plugin, GarbageService garbageService, PlayerService playerService) {
        this.plugin = plugin;
        this.garbageService = garbageService;
        this.playerService = playerService;
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player p = event.getPlayer();
        ActiveGarbage garbage = garbageService.findGarbage(event.getRightClicked());
        if (garbage == null) return;
        garbageService.interact(p, garbage);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!garbageService.isGarbage(event.getEntity())) return;
        ActiveGarbage garbage = garbageService.findGarbage(event.getEntity());
        if (garbage == null || !garbage.isMob()) return;

        Player killer = event.getEntity().getKiller();
        event.getDrops().clear();
        event.setDroppedExp(0);

        String required = plugin.getConfig().getString("tool.material", "STICK");
        boolean hasTool = killer != null
                && killer.getInventory().getItemInMainHand().getType() == Material.valueOf(required.toUpperCase());

        if (killer != null && !hasTool) {
            killer.sendActionBar("\u00A7cYou need the "
                    + plugin.getConfig().getString("tool.name", "&6Trash Grabber").replace("&", "\u00A7")
                    + " to loot trash monsters!");
        }
        garbageService.handleMobKilled(hasTool ? killer : null, garbage);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        PlayerData data = playerService.getPlayerData(p.getUniqueId());
        // give the collector tool once (first join)
        if (!hasAnyTool(p)) {
            p.getInventory().addItem(CollectorItem.build(plugin, data));
        }
        plugin.getScoreboardManager().addPlayer(p, data);
        plugin.getScoreboardManager().updateForPlayer(p, data);
        plugin.getStatusBarManager().updateForPlayer(p, data);
        plugin.getShopGui().applySpeed(p, data);
    }

    private boolean hasAnyTool(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == Material.valueOf(
                    plugin.getConfig().getString("tool.material", "STICK"))) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerService.save(event.getPlayer().getUniqueId());
        plugin.getScoreboardManager().removePlayer(event.getPlayer());
    }
}