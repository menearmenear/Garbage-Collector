package com.menear.garbagecollector;

import com.menear.garbagecollector.service.GarbageService;
import com.menear.garbagecollector.service.PlayerService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

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

    // Consumes a right-clicked Luck Token and grants a permanent +1 luck.
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.NAUTILUS_SHELL) return;
        if (item.getItemMeta() == null
                || !item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "luck-token"), PersistentDataType.BYTE)) {
            return;
        }
        event.setCancelled(true);
        Player p = event.getPlayer();
        PlayerData data = playerService.getPlayerData(p.getUniqueId());
        data.addGarbageLuck(1);
        plugin.getScoreboardManager().updateForPlayer(p, data);
        plugin.getStatusBarManager().updateForPlayer(p, data);
        item.setAmount(item.getAmount() - 1);
        p.sendMessage(ChatColor.LIGHT_PURPLE + "You gain +1 Garbage Luck! (now " + data.getGarbageLuck() + ")");
        Sfx.play(plugin, p, "token", Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!garbageService.isGarbage(event.getEntity())) return;
        ActiveGarbage garbage = garbageService.findGarbage(event.getEntity());
        if (garbage == null || !garbage.isMob()) return;

        Player killer = event.getEntity().getKiller();
        event.getDrops().clear();
        event.setDroppedExp(0);

        boolean requiresTool = plugin.getConfig().getBoolean("tool.requireTool", true);
        boolean hasTool = killer != null && isGrabber(killer.getInventory().getItemInMainHand());

        if (killer != null && requiresTool && !hasTool) {
            killer.sendActionBar("\u00A7cYou need the "
                    + plugin.getConfig().getString("tool.name", "&6Trash Grabber").replace("&", "\u00A7")
                    + " to loot trash monsters!");
        }
        garbageService.handleMobKilled(!requiresTool || hasTool ? killer : null, garbage);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        PlayerData data = playerService.getPlayerData(p.getUniqueId());
        // daily missions refresh each server day
        plugin.getMissionService().refresh(data);
        // give the collector tool once (first join)
        if (!hasAnyTool(p)) {
            ItemStack tool = CollectorItem.build(plugin, data);
            p.getInventory().addItem(tool).values().forEach(left ->
                    p.getWorld().dropItemNaturally(p.getLocation(), left));
        }

        plugin.getScoreboardManager().addPlayer(p, data);
        plugin.getScoreboardManager().updateForPlayer(p, data);
        plugin.getStatusBarManager().updateForPlayer(p, data);
        plugin.getShopGui().applySpeed(p, data);
    }

    private boolean hasAnyTool(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == toolMaterial()) {
                return true;
            }
        }
        return false;
    }

    private boolean isGrabber(ItemStack item) {
        return item != null && item.getType() == toolMaterial();
    }

    private Material toolMaterial() {
        Material m = Material.matchMaterial(plugin.getConfig().getString("tool.material", "STICK"));
        return m != null ? m : Material.STICK;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerService.save(event.getPlayer().getUniqueId());
        plugin.getScoreboardManager().removePlayer(event.getPlayer());
    }
}