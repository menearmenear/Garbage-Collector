package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
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
import java.util.Map;
import java.util.Random;

public class SellGui {
    private final GarbageCollectorPlugin plugin;
    private final Random random = new Random();

    public SellGui(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public static String title() {
        return ChatColor.translateAlternateColorCodes('&', "&8Sell Garbage");
    }

    public void open(Player p) {
        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
        Map<String, Integer> counts = data.getGarbageCount();
        Inventory inv = Bukkit.createInventory(null, 27, title());

        int slot = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() <= 0) continue;
            String type = e.getKey();
            Material mat = safeMaterial(plugin.getConfig().getString("garbage.types." + type + ".material", "PAPER"), Material.PAPER);
            int value = plugin.getConfig().getInt("garbage.types." + type + ".value", 1);
            ItemStack item = new ItemStack(mat, Math.min(64, e.getValue()));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + capitalize(type));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "You have: " + ChatColor.GOLD + e.getValue());
            lore.add(ChatColor.GRAY + "Sell value: " + ChatColor.GREEN + "$" + value + " each");
            lore.add(ChatColor.GRAY + "Click to sell all of this type");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        ItemStack sellAll = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = sellAll.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Sell ALL");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Sell every garbage type");
        lore.add(ChatColor.GRAY + "Total value: " + ChatColor.GREEN + "$" + computeTotal(data));
        meta.setLore(lore);
        sellAll.setItemMeta(meta);
        inv.setItem(26, sellAll);

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private int computeTotal(PlayerData data) {
        int total = 0;
        for (Map.Entry<String, Integer> e : data.getGarbageCount().entrySet()) {
            total += e.getValue() * plugin.getConfig().getInt("garbage.types." + e.getKey() + ".value", 1);
        }
        return total;
    }

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!event.getView().getTitle().equals(title())) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());

        if (clicked.getType() == Material.GOLD_INGOT) {
            // sell all
            int total = computeTotal(data);
            if (total > 0) {
                data.addMoney(total);
                data.addTotalEarned(total);
                int count = data.totalGarbage();
                data.clearGarbage();
                p.sendMessage(ChatColor.GREEN + "Sold all garbage for $" + total + " (" + count + " items)");
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.0f);
                rollBonusDrop(p, data);
            } else {
                p.sendMessage(ChatColor.RED + "You have no garbage to sell!");
            }
            p.closeInventory();
            plugin.getScoreboardManager().updateForPlayer(p, data);
            plugin.getStatusBarManager().updateForPlayer(p, data);
            return;
        }

        // individual type: find matching item
        for (Map.Entry<String, Integer> e : data.getGarbageCount().entrySet()) {
            if (e.getValue() <= 0) continue;
            Material mat = safeMaterial(plugin.getConfig().getString("garbage.types." + e.getKey() + ".material", "PAPER"), Material.PAPER);
            if (mat == clicked.getType()) {
                int valuePer = plugin.getConfig().getInt("garbage.types." + e.getKey() + ".value", 1);
                int earn = e.getValue() * valuePer;
                data.addMoney(earn);
                data.addTotalEarned(earn);
                data.setGarbage(e.getKey(), 0);
                p.sendMessage(ChatColor.GREEN + "Sold " + e.getValue() + " " + e.getKey() + " for $" + earn);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.0f);
                rollBonusDrop(p, data);
                p.closeInventory();
                plugin.getScoreboardManager().updateForPlayer(p, data);
                plugin.getStatusBarManager().updateForPlayer(p, data);
                return;
            }
        }
    }

    private void rollBonusDrop(Player p, PlayerData data) {
        List<Map<?, ?>> list = new ArrayList<>();
        Object raw = plugin.getConfig().get("sell.bonusLoot");
        if (raw instanceof List<?> l) {
            for (Object o : l) {
                if (o instanceof Map<?, ?> m) list.add(m);
            }
        }
        double luckBonus = data.getGarbageLuck() * plugin.getConfig().getDouble("mob.luckDropChanceBonusPerPoint", 0.01);
        for (Map<?, ?> entry : list) {
            double chance = ((Number) entry.get("chance")).doubleValue();
            if (random.nextDouble() < Math.min(1.0, chance + luckBonus)) {
                Material mat = safeMaterial(entry.get("material").toString(), Material.DIAMOND);
                int[] minmax = parseAmount(entry.get("amount").toString());
                int amount = minmax[1] <= minmax[0] ? minmax[0] : minmax[0] + random.nextInt(minmax[1] - minmax[0] + 1);
                p.getInventory().addItem(new ItemStack(mat, Math.max(1, amount)));
                p.sendMessage(ChatColor.LIGHT_PURPLE + "Bonus loot! You found " + mat.name());
                return;
            }
        }
    }

    private int[] parseAmount(String s) {
        try {
            String[] parts = s.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;
            return new int[] { Math.max(0, min), Math.max(1, max) };
        } catch (Exception e) {
            return new int[] { 1, 1 };
        }
    }

    private Material safeMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase().replace(" ", "_")); } catch (Exception e) { return fallback; }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String stripColor(String s) {
        return ChatColor.stripColor(s);
    }
}