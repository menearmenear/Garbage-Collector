package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.Sfx;
import com.menear.garbagecollector.service.LeaderboardService;
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

public class LeaderboardGui {
    private final GarbageCollectorPlugin plugin;

    public LeaderboardGui(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public static String title() {
        return ChatColor.translateAlternateColorCodes('&', "&8Top Garbage Collectors");
    }

    private static final int[] TAB_SLOTS = {10, 11, 12, 13, 14};
    private static final int[] ROW_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30};

    public void open(Player p) {
        open(p, "money");
    }

    public void open(Player p, String sort) {
        Inventory inv = Bukkit.createInventory(null, 45, title());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        List<String> sorts = LeaderboardService.SORTS;
        for (int i = 0; i < sorts.size() && i < TAB_SLOTS.length; i++) {
            inv.setItem(TAB_SLOTS[i], tabItem(sorts.get(i), sort));
        }
        List<LeaderboardService.Entry> entries = plugin.getLeaderboardService().top(sort, 10);
        for (int i = 0; i < entries.size() && i < ROW_SLOTS.length; i++) {
            inv.setItem(ROW_SLOTS[i], entryItem(i, entries.get(i), sort));
        }
        p.openInventory(inv);
        Sfx.play(plugin, p, "guiOpen", Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack tabItem(String sort, String activeSort) {
        ItemStack item = new ItemStack(sort.equals(activeSort) ? Material.EMERALD : Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + capitalize(sort)
                + (sort.equals(activeSort) ? ChatColor.GREEN + " (viewing)" : ""));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack entryItem(int index, LeaderboardService.Entry entry, String sort) {
        ItemStack item = new ItemStack(index == 0 ? Material.GOLD_BLOCK
                : index == 1 ? Material.IRON_BLOCK
                : index == 2 ? Material.COAL_BLOCK : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((index + 1) + ". " + ChatColor.AQUA + entry.name());
        meta.setLore(List.of(ChatColor.GRAY + statsLabel(sort) + ": " + ChatColor.YELLOW + entry.value()));
        item.setItemMeta(meta);
        return item;
    }

    private String statsLabel(String sort) {
        return switch (sort) {
            case "collected" -> "Garbage collected";
            case "monsters" -> "Monsters slain";
            case "luck" -> "Garbage luck";
            case "recycled" -> "Materials recycled";
            default -> "Lifetime earnings";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!event.getView().getTitle().equals(title())) return;
        event.setCancelled(true);
        String current = "money";
        for (int i = 0; i < TAB_SLOTS.length; i++) {
            if (event.getSlot() == TAB_SLOTS[i] && i < LeaderboardService.SORTS.size()) {
                current = LeaderboardService.SORTS.get(i);
                break;
            }
        }
        // toggle-view deterministically: if the clicked tab is already on, refresh anyway
        open(p, current);
    }
}