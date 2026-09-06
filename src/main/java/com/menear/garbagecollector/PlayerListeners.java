package com.menear.garbagecollector;

import com.menear.garbagecollector.service.GarbageService;
import com.menear.garbagecollector.service.PlayerService;
import com.menear.garbagecollector.ui.StatusBarManager;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.block.Action;

public class PlayerListeners implements Listener {
    private final GarbageCollectorPlugin plugin;
    private final NamespacedKey garbageKey;
    private final GarbageService garbageService;
    private final PlayerService playerService;
    private final StatusBarManager statusBarManager;

    public PlayerListeners(GarbageCollectorPlugin plugin, GarbageService garbageService, PlayerService playerService, StatusBarManager statusBarManager) {
        this.plugin = plugin;
        this.garbageService = garbageService;
        this.playerService = playerService;
        this.statusBarManager = statusBarManager;
        this.garbageKey = new NamespacedKey(plugin, "garbage-id");
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        // Only handle main hand to avoid double-call
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity ent = event.getRightClicked();
        Player p = event.getPlayer();
        if (!garbageService.isGarbage(ent)) return;
        ArmorStand stand = (ArmorStand) ent;

        PlayerData data = playerService.getPlayerData(p.getUniqueId());

        // Tool requirement
        boolean requireTool = plugin.getConfig().getBoolean("tool.requireTool", true);
        if (requireTool) {
            ItemStack inHand = p.getInventory().getItemInMainHand();
            String required = plugin.getConfig().getString("tool.material", "STICK");
            if (inHand == null || inHand.getType() != Material.valueOf(required.toUpperCase())) {
                p.sendActionBar("\u00A7cYou need the Trash Grabber to collect garbage!");
                return;
            }
        }

        if (!data.canCollect()) {
            p.sendActionBar("\u00A7cBag full!");
            return;
        }

        // Determine value by matching the helmet material to config
        int value = 1;
        ItemStack helmet = stand.getEquipment().getHelmet();
        if (helmet != null) {
            String matName = helmet.getType().name();
            if (plugin.getConfig().isConfigurationSection("garbage.types")) {
                for (String key : plugin.getConfig().getConfigurationSection("garbage.types").getKeys(false)) {
                    String cfgMat = plugin.getConfig().getString("garbage.types." + key + ".material", "PAPER");
                    if (matName.equalsIgnoreCase(cfgMat)) {
                        value = plugin.getConfig().getInt("garbage.types." + key + ".value", 1);
                        break;
                    }
                }
            }
        }

        data.addMoney(value);
        data.increaseCollected(1);
        stand.remove();

        statusBarManager.updateForPlayer(p, data);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        String refillItem = plugin.getConfig().getString("water.refillItem", "WATER_BUCKET");
        if (!item.getType().name().equalsIgnoreCase(refillItem)) return;

        PlayerData data = playerService.getPlayerData(p.getUniqueId());
        int refillAmount = plugin.getConfig().getInt("water.refillAmount", plugin.getConfig().getInt("player.maxWater", 100));
        data.refillWater(refillAmount);

        // consume one if not in creative
        if (!p.getGameMode().isCreative()) {
            int amount = item.getAmount();
            if (amount <= 1) {
                p.getInventory().remove(item);
            } else {
                item.setAmount(amount - 1);
            }
        }

        // remove slowness effect if present
        p.removePotionEffect(PotionEffectType.SLOW);
        statusBarManager.updateForPlayer(p, data);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerService.getPlayerData(event.getPlayer().getUniqueId());
        // give basic tool if not present
        ItemStack tool = new ItemStack(Material.valueOf(plugin.getConfig().getString("tool.material", "STICK")));
        event.getPlayer().getInventory().addItem(tool);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerService.save(event.getPlayer().getUniqueId());
    }
}
