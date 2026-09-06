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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CollectionGui {
    private final GarbageCollectorPlugin plugin;

    public CollectionGui(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public static String title() {
        return ChatColor.translateAlternateColorCodes('&', "&8Your Collection");
    }

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public void open(Player p, String tab) {
        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 45, title());

        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        setTabButtons(inv, tab);
        placeItems(inv, data, tab);

        p.openInventory(inv);
        Sfx.play(plugin, p, "guiOpen", Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private void fillBorder(Inventory inv, Material mat) {
        ItemStack filler = new ItemStack(mat);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 4 || col == 0 || col == 8) {
                inv.setItem(i, filler);
            }
        }
        // clear inner slots of the border rows used for items
    }

    private void setTabButtons(Inventory inv, String current) {
        boolean garbage = "garbage".equals(current);
        inv.setItem(0, tabStack(garbage ? Material.PAPER : Material.WHITE_DYE,
                ChatColor.GOLD + "Garbage", garbage, "&8Click to view garbage"));
        inv.setItem(8, tabStack(garbage ? Material.WHITE_DYE : Material.IRON_SWORD,
                ChatColor.RED + "Mobs", !garbage, "&8Click to view mobs"));
    }

    private ItemStack tabStack(Material mat, String name, boolean selected, String sub) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((selected ? ChatColor.YELLOW + "» " : "") + name + (selected ? ChatColor.YELLOW + " «" : ""));
        List<String> lore = new ArrayList<>();
        lore.add(sub.replace("&", "\u00A7"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack coloredPane(String colorCode, String name) {
        Material mat = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', colorCode + name));
        item.setItemMeta(meta);
        return item;
    }

    private void placeItems(Inventory inv, PlayerData data, String tab) {
        List<ItemStack> items = "garbage".equals(tab) ? garbageItems(data) : mobItems(data);
        for (int i = 0; i < Math.min(items.size(), SLOTS.length); i++) {
            inv.setItem(SLOTS[i], items.get(i));
        }
    }

    private List<ItemStack> garbageItems(PlayerData data) {
        List<ItemStack> result = new ArrayList<>();
        List<String> types = new ArrayList<>(plugin.getConfig().getConfigurationSection("garbage.types").getKeys(false));
        types.sort(Comparator.comparingInt(t -> -plugin.getConfig().getInt("garbage.types." + t + ".value", 0)));
        for (String type : types) {
            Material mat = safeMaterial(plugin.getConfig().getString("garbage.types." + type + ".material", "PAPER"), Material.PAPER);
            int value = plugin.getConfig().getInt("garbage.types." + type + ".value", 1);
            int count = data.getCollectionGarbage().getOrDefault(type, 0);
            int current = data.getGarbage(type);
            ItemStack item = new ItemStack(mat, Math.min(64, Math.max(1, count)));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + capitalize(type));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Collection: " + ChatColor.GOLD + count);
            lore.add(ChatColor.GRAY + "Unlocked: " + (count > 0 ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
            lore.add(ChatColor.GRAY + "In bag: " + ChatColor.AQUA + current);
            lore.add(ChatColor.GRAY + "Sell value: " + ChatColor.GREEN + "$" + value);
            meta.setLore(lore);
            item.setItemMeta(meta);
            result.add(item);
        }
        return result;
    }

    private List<ItemStack> mobItems(PlayerData data) {
        List<ItemStack> result = new ArrayList<>();
        Map<String, Map<Integer, Integer>> mobs = data.getCollectionMobs();
        List<String> names = new ArrayList<>();
        if (plugin.getConfig().isConfigurationSection("mob.collectionName")) {
            names.addAll(plugin.getConfig().getConfigurationSection("mob.collectionName").getKeys(false));
        }
        if (names.isEmpty()) {
            String defaultName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("mob.name", "&cTrash Monster")));
            names.add(defaultName);
        }
        // always include any previously-collected mob types so data is never hidden
        for (String n : mobs.keySet()) {
            if (!names.contains(n)) names.add(n);
        }
        int maxTier = Math.max(1, plugin.getConfig().getInt("mob.maxTier", 5));
        for (String name : names) {
            Material mat = safeMaterial(plugin.getConfig().getString("mob.collectionName." + name + ".material", "IRON_SWORD"), Material.IRON_SWORD);
            int total = mobs.getOrDefault(name, Map.of()).values().stream().mapToInt(Integer::intValue).sum();
            ItemStack item = new ItemStack(mat, Math.min(64, Math.max(1, total)));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RED + name + " " + ChatColor.GRAY + "(" + total + ")");
            List<String> lore = new ArrayList<>();
            Map<Integer, Integer> tiers = mobs.get(name);
            if (tiers == null || tiers.isEmpty()) {
                tiers = new java.util.HashMap<>();
                for (int t = 1; t <= maxTier; t++) tiers.put(t, 0);
            }
            tiers.entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getKey))
                    .forEach(e -> lore.add(ChatColor.GRAY + "Tier " + romanNumeral(e.getKey())
                            + ": " + (e.getValue() > 0 ? ChatColor.GOLD + Integer.toString(e.getValue())
                            : ChatColor.DARK_GRAY + "not discovered")));
            lore.add(ChatColor.GRAY + "Total kills: " + ChatColor.GOLD + total);
            meta.setLore(lore);
            item.setItemMeta(meta);
            result.add(item);
        }
        return result;
    }

    private String romanNumeral(int n) {
        String[] symbols = {"X", "IX", "V", "IV", "I"};
        int[] values = {10, 9, 5, 4, 1};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (n >= values[i]) {
                sb.append(symbols[i]);
                n -= values[i];
            }
        }
        return sb.toString();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private Material safeMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase().replace(" ", "_")); } catch (Exception e) { return fallback; }
    }

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!event.getView().getTitle().equals(title())) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        int slot = event.getSlot();
        String itemName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (slot == 0 && itemName.contains("Garbage")) {
            Sfx.play(plugin, p, "guiClick", Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
            open(p, "garbage");
        } else if (slot == 8 && itemName.contains("Mobs")) {
            Sfx.play(plugin, p, "guiClick", Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
            open(p, "mobs");
        }
    }
}