package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Sfx;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class RecycleGui {
    private final GarbageCollectorPlugin plugin;

    public RecycleGui(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public static String title() {
        return ChatColor.translateAlternateColorCodes('&', "&8Recycle");
    }

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    public void open(Player p) {
        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 45, title());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        int i = 0;
        for (String type : plugin.getRecycleService().typeKeys()) {
            if (i >= SLOTS.length) break;
            inv.setItem(SLOTS[i++], recipeItem(data, type));
        }
        p.openInventory(inv);
        Sfx.play(plugin, p, "guiOpen", Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack recipeItem(PlayerData data, String type) {
        Material mat = safeMaterial(plugin.getRecycleService().icon(type), Material.PAPER);
        int inBag = data.getGarbage(type);
        int amount = plugin.getRecycleService().amount(type);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + plugin.getRecycleService().displayName(type));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "In bag: " + ChatColor.AQUA + inBag + ChatColor.GRAY + "  (per batch: " + amount + ")");
        lore.add(ChatColor.GRAY + "You have " + ChatColor.WHITE + data.getRecycled(type)
                + ChatColor.GRAY + " " + plugin.getRecycleService().displayName(type) + " materials");
        lore.add("");
        lore.add(ChatColor.GRAY + "Click to convert into materials");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "recycle-type"), PersistentDataType.STRING, type);
        item.setItemMeta(meta);
        return item;
    }

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!event.getView().getTitle().equals(title())) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
        String type = recycleTypeFor(clicked);
        if (type == null) return;

        int times = plugin.getRecycleService().convert(data, type);
        if (times > 0) {
            p.sendMessage(ChatColor.GREEN + "Recycled +" + times + " " + plugin.getRecycleService().displayName(type)
                    + ChatColor.GRAY + " (now " + data.getRecycled(type) + ")");
            Sfx.play(plugin, p, "recycle", Sound.BLOCK_ANVIL_USE, 0.7f, 0.8f);
            plugin.getMissionService().progress(data, "recycle", type);
        } else {
            p.sendMessage(ChatColor.RED + "You don't have a full batch of "
                    + plugin.getRecycleService().displayName(type) + " yet!");
            Sfx.play(plugin, p, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
        }
        plugin.getStatusBarManager().updateForPlayer(p, data);
        plugin.getScoreboardManager().updateForPlayer(p, data);
        open(p);
    }

    private String recycleTypeFor(ItemStack clicked) {
        if (clicked.hasItemMeta()) {
            String tagged = clicked.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "recycle-type"), PersistentDataType.STRING);
            if (tagged != null) return tagged;
        }
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase();
        for (String type : plugin.getRecycleService().typeKeys()) {
            if (plugin.getRecycleService().displayName(type).toLowerCase().equals(name)
                    || plugin.getRecycleService().displayName(type).replace(" ", "_").toLowerCase().equals(name)) {
                return type;
            }
        }
        return null;
    }

    private Material safeMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        Material m = Material.matchMaterial(name.toUpperCase().replace(" ", "_"));
        return m != null ? m : fallback;
    }
}