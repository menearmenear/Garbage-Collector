package com.menear.garbagecollector;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CollectorItem {
    public static ItemStack build(GarbageCollectorPlugin plugin, PlayerData data) {
        ItemStack item = new ItemStack(Material.valueOf(plugin.getConfig().getString("tool.material", "STICK")));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        String name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("tool.name", "&6Trash Grabber"));
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("tool.lore")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Tier: " + ChatColor.GOLD + data.getCollectorTier());
        lore.add(ChatColor.GRAY + "Luck: " + ChatColor.LIGHT_PURPLE + data.getGarbageLuck());
        lore.add(ChatColor.GRAY + "Speed Lvl: " + ChatColor.AQUA + data.getSpeedLevel());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}