package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Sfx;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ZoneGui {
    private final GarbageCollectorPlugin plugin;

    public ZoneGui(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public static String title() {
        return ChatColor.translateAlternateColorCodes('&', "&8Garbage Zones");
    }

    public void open(Player p) {
        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 45, title());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        for (String zone : plugin.getZoneService().ids()) {
            int slot = plugin.getZoneService().slot(zone);
            if (slot >= 0 && slot < 45) inv.setItem(slot, zoneItem(data, zone));
        }
        p.openInventory(inv);
        Sfx.play(plugin, p, "guiOpen", Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack zoneItem(PlayerData data, String zone) {
        String activeId = plugin.getZoneService().activeZone(data);
        boolean active = activeId.equals(zone);
        boolean unlocked = plugin.getZoneService().unlocked(data, zone);
        boolean levelOk = plugin.getZoneService().meetsLevel(data, zone);
        int cost = plugin.getZoneService().cost(zone);

        Material mat = safeMaterial(plugin.getZoneService().icon(zone), Material.BIRCH_PLANKS);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((active ? ChatColor.YELLOW + "» " : "")
                + ChatColor.translateAlternateColorCodes('&', plugin.getZoneService().displayName(zone))
                + (active ? ChatColor.YELLOW + " «" : ""));
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getZoneService().descriptions(zone)) {
            lore.add(line.replace("&", "\u00A7"));
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Requires Level: " + (levelOk ? ChatColor.GREEN : ChatColor.RED)
                + plugin.getZoneService().requiredLevel(zone));
        lore.add(cost > 0
                ? (data.hasMoney(cost) ? ChatColor.GREEN : ChatColor.RED) + "Cost: $" + cost
                : ChatColor.GREEN + "Free");
        lore.add("");
        if (active) {
            lore.add(ChatColor.GREEN + "Currently collecting here");
        } else if (unlocked) {
            lore.add(ChatColor.AQUA + "Unlocked - click to travel here");
        } else {
            lore.add((levelOk ? ChatColor.GOLD : ChatColor.RED) + "Locked - buy it to unlock");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!event.getView().getTitle().equals(title())) return;
        event.setCancelled(true);
        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
        String zone = zoneAt(event.getSlot());
        if (zone == null) return;
        if (plugin.getZoneService().activeZone(data).equals(zone)) {
            p.sendActionBar(ChatColor.GREEN + "You are already collecting there.");
            return;
        }
        plugin.getZoneService().enter(p, data, zone);
        open(p);
    }

    private String zoneAt(int slot) {
        for (String zone : plugin.getZoneService().ids()) {
            if (plugin.getZoneService().slot(zone) == slot) return zone;
        }
        return null;
    }

    private Material safeMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); } catch (Exception e) { return fallback; }
    }
}