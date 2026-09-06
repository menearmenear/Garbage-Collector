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
        lore.add(ChatColor.GOLD + "Collector Level " + plugin.getLevelService().level(data));
        lore.add(ChatColor.GRAY + "Tier: " + ChatColor.GOLD + data.getCollectorTier());
        lore.add(ChatColor.RED + "Power: " + Stats.power(plugin, data));
        lore.add(ChatColor.LIGHT_PURPLE + "Luck: " + Stats.luck(plugin, data));
        lore.add(ChatColor.BLUE + "Efficiency: " + Stats.efficiency(plugin, data));
        lore.add(ChatColor.DARK_PURPLE + "Magnet: " + Stats.magnet(plugin, data));
        lore.add(ChatColor.AQUA + "Bag: " + Stats.capacity(plugin, data));
        lore.add(ChatColor.GRAY + "Speed Lvl: " + ChatColor.AQUA + data.getSpeedLevel());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}