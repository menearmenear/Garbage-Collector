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

public class ShopGui {
    private final GarbageCollectorPlugin plugin;

    public ShopGui(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public static String title() {
        return ChatColor.translateAlternateColorCodes('&', "&8Upgrade Shop");
    }

    public void open(Player p) {
        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, title());

        inv.setItem(10, tierItem(data));
        inv.setItem(12, luckItem(data));
        inv.setItem(14, speedItem(data));
        if (plugin.getConfig().getBoolean("shop.magnet.enabled", true)) {
            inv.setItem(16, magnetItem(data));
        }

        p.openInventory(inv);
        Sfx.play(plugin, p, "guiOpen", Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private ItemStack tierItem(PlayerData data) {
        int tier = data.getCollectorTier();
        int next = tier + 1;
        String nextPath = "garbage.collector.tier" + next;
        ItemStack item = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Upgrade Collector Tier");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current tier: " + ChatColor.GOLD + "T" + tier);
        if (plugin.getConfig().isConfigurationSection(nextPath)) {
            int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
            lore.add(ChatColor.GRAY + "Next tier: " + ChatColor.GOLD + "T" + next);
            lore.add(ChatColor.GRAY + "  + Value multiplier " + ChatColor.GREEN + "x"
                    + plugin.getConfig().getDouble(nextPath + ".valueMultiplier", 1));
            lore.add(ChatColor.GRAY + "  + Bag capacity " + ChatColor.GREEN
                    + plugin.getConfig().getInt(nextPath + ".bagCapacity", 20));
            lore.add("");
            if (cost < 0 || data.hasMoney(cost)) {
                lore.add(ChatColor.GREEN + "Cost: $" + cost);
            } else {
                lore.add(ChatColor.RED + "Cost: $" + cost);
            }
        } else {
            lore.add(ChatColor.GRAY + "Max tier reached!");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack luckItem(PlayerData data) {
        int costPer = plugin.getConfig().getInt("luck.upgradeCostPerPoint", 50);
        int perPurchase = plugin.getConfig().getInt("luck.upgradeMaxPerPurchase", 5);
        int max = plugin.getConfig().getInt("luck.maxLuck", 100);
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Upgrade Garbage Luck");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current luck: " + ChatColor.LIGHT_PURPLE + data.getGarbageLuck());
        lore.add(ChatColor.GRAY + "Buy " + ChatColor.LIGHT_PURPLE + "+" + perPurchase
                + ChatColor.GRAY + " luck (" + ChatColor.GREEN + "$" + (costPer * perPurchase) + ChatColor.GRAY + ")");
        lore.add(ChatColor.GRAY + "Higher luck = more money & drops");
        if (data.getGarbageLuck() >= max) lore.add(ChatColor.RED + "Max luck reached!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack speedItem(PlayerData data) {
        int lvl = data.getSpeedLevel();
        ItemStack item = new ItemStack(Material.SUGAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Upgrade Speed");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current speed lvl: " + ChatColor.AQUA + lvl);
        String nextPath = "shop.speed.levels." + (lvl + 1);
        if (plugin.getConfig().isConfigurationSection(nextPath)) {
            int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
            lore.add(ChatColor.GRAY + "Next: lvl " + ChatColor.AQUA + (lvl + 1)
                    + ChatColor.GRAY + " (walk speed " + plugin.getConfig().getDouble(nextPath + ".walkSpeed", 0.2) + ")");
            lore.add("");
            if (data.hasMoney(cost)) {
                lore.add(ChatColor.GREEN + "Cost: $" + cost);
            } else {
                lore.add(ChatColor.RED + "Cost: $" + cost);
            }
        } else {
            lore.add(ChatColor.GRAY + "Max speed reached!");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack magnetItem(PlayerData data) {
        int lvl = data.getMagnetLevel();
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "Magnet Aura");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Auto-collects nearby garbage");
        lore.add(ChatColor.GRAY + "Current lvl: " + ChatColor.DARK_PURPLE + lvl);
        String nextPath = "shop.magnet.levels." + (lvl + 1);
        if (plugin.getConfig().isConfigurationSection(nextPath)) {
            int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
            lore.add(ChatColor.GRAY + "Next: lvl " + ChatColor.DARK_PURPLE + (lvl + 1)
                    + ChatColor.GRAY + " (radius " + plugin.getConfig().getInt(nextPath + ".radius", 3) + ", pull every "
                    + plugin.getConfig().getInt(nextPath + ".pullIntervalSeconds", 3) + "s)");
            lore.add("");
            if (data.hasMoney(cost)) {
                lore.add(ChatColor.GREEN + "Cost: $" + cost);
            } else {
                lore.add(ChatColor.RED + "Cost: $" + cost);
            }
        } else {
            lore.add(ChatColor.GRAY + "Max magnet reached!");
        }
        meta.setLore(lore);
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

        if (clicked.getType() == Material.IRON_PICKAXE) {
            int next = data.getCollectorTier() + 1;
            String nextPath = "garbage.collector.tier" + next;
            if (!plugin.getConfig().isConfigurationSection(nextPath)) {
                p.sendMessage(ChatColor.RED + "Max tier reached!");
                return;
            }
            int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
            if (cost >= 0 && data.spend(cost)) {
                data.setCollectorTier(next);
                p.sendMessage(ChatColor.GREEN + "Collector upgraded to Tier " + next + " for $" + cost);
                Sfx.play(plugin, p, "shop", Sound.BLOCK_ANVIL_USE, 0.7f, 1.0f);
            } else if (cost < 0) {
                data.setCollectorTier(next);
                p.sendMessage(ChatColor.GREEN + "Collector upgraded to Tier " + next + "!");
                Sfx.play(plugin, p, "shop", Sound.BLOCK_ANVIL_USE, 0.7f, 1.0f);
            } else {
                p.sendMessage(ChatColor.RED + "Not enough money! You need $" + cost);
                Sfx.play(plugin, p, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            }
        } else if (clicked.getType() == Material.EXPERIENCE_BOTTLE) {
            int per = plugin.getConfig().getInt("luck.upgradeMaxPerPurchase", 5);
            int cost = plugin.getConfig().getInt("luck.upgradeCostPerPoint", 50) * per;
            int max = plugin.getConfig().getInt("luck.maxLuck", 100);
            if (data.getGarbageLuck() >= max) {
                p.sendMessage(ChatColor.RED + "Max luck reached!");
                Sfx.play(plugin, p, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
                return;
            }
            if (data.spend(cost)) {
                int toAdd = Math.min(per, max - data.getGarbageLuck());
                data.addGarbageLuck(toAdd);
                p.sendMessage(ChatColor.LIGHT_PURPLE + "Bought +" + toAdd + " luck for $" + cost);
                Sfx.play(plugin, p, "shop", Sound.BLOCK_ANVIL_USE, 0.7f, 1.0f);
            } else {
                p.sendMessage(ChatColor.RED + "Not enough money!");
                Sfx.play(plugin, p, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            }
        } else if (clicked.getType() == Material.SUGAR) {
            int lvl = data.getSpeedLevel();
            String nextPath = "shop.speed.levels." + (lvl + 1);
            if (!plugin.getConfig().isConfigurationSection(nextPath)) {
                p.sendMessage(ChatColor.RED + "Max speed reached!");
                Sfx.play(plugin, p, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
                return;
            }
            int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
            if (cost >= 0 && data.spend(cost)) {
                data.setSpeedLevel(lvl + 1);
                applySpeed(p, data);
                p.sendMessage(ChatColor.AQUA + "Speed upgraded to lvl " + (lvl + 1) + " for $" + cost);
                Sfx.play(plugin, p, "shop", Sound.BLOCK_ANVIL_USE, 0.7f, 1.0f);
            } else if (cost < 0) {
                data.setSpeedLevel(lvl + 1);
                applySpeed(p, data);
                p.sendMessage(ChatColor.AQUA + "Speed upgraded to lvl " + (lvl + 1) + "!");
            } else {
                p.sendMessage(ChatColor.RED + "Not enough money!");
                Sfx.play(plugin, p, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            }
        } else if (clicked.getType() == Material.COMPASS) {
            if (!plugin.getConfig().getBoolean("shop.magnet.enabled", true)) return;
            int lvl = data.getMagnetLevel();
            String nextPath = "shop.magnet.levels." + (lvl + 1);
            if (!plugin.getConfig().isConfigurationSection(nextPath)) {
                p.sendMessage(ChatColor.RED + "Max magnet level reached!");
                Sfx.play(plugin, p, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
                return;
            }
            int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
            if (cost >= 0 && data.spend(cost)) {
                data.setMagnetLevel(lvl + 1);
                p.sendMessage(ChatColor.DARK_PURPLE + "Magnet upgraded to lvl " + (lvl + 1) + " for $" + cost);
                Sfx.play(plugin, p, "shop", Sound.BLOCK_ANVIL_USE, 0.7f, 1.0f);
            } else if (cost < 0) {
                data.setMagnetLevel(lvl + 1);
                p.sendMessage(ChatColor.DARK_PURPLE + "Magnet upgraded to lvl " + (lvl + 1) + "!");
                Sfx.play(plugin, p, "shop", Sound.BLOCK_ANVIL_USE, 0.7f, 1.0f);
            } else {
                p.sendMessage(ChatColor.RED + "Not enough money!");
                Sfx.play(plugin, p, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            }
        }

        applySpeed(p, data);
        plugin.getScoreboardManager().updateForPlayer(p, data);
        plugin.getStatusBarManager().updateForPlayer(p, data);
        p.closeInventory();
    }

    public void applySpeed(Player p, PlayerData data) {
        String path = "shop.speed.levels." + data.getSpeedLevel();
        float walk = (float) plugin.getConfig().getDouble(path + ".walkSpeed", 0.2);
        p.setWalkSpeed(Math.min(1.0f, Math.max(0.0f, walk)));
    }
}