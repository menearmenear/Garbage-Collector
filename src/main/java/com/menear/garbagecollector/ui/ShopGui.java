package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Sfx;
import com.menear.garbagecollector.Stats;
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

    private static String upgradesTitle() {
        return ChatColor.translateAlternateColorCodes('&', "&8Shop - Upgrades");
    }

    private static String backpacksTitle() {
        return ChatColor.translateAlternateColorCodes('&', "&8Shop - Backpacks");
    }

    private static String toolsTitle() {
        return ChatColor.translateAlternateColorCodes('&', "&8Shop - Grabbers");
    }

    private static String powerupsTitle() {
        return ChatColor.translateAlternateColorCodes('&', "&8Shop - Powerups");
    }

    public static boolean isShopView(String title) {
        return title.equals(title()) || title.equals(upgradesTitle()) || title.equals(backpacksTitle())
                || title.equals(toolsTitle()) || title.equals(powerupsTitle());
    }

    public void open(Player p) {
        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 45, title());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }

        inv.setItem(10, navItem(Material.MAP, ChatColor.GOLD + "Garbage Zones",
                "Choose where to collect with", "better loot in later zones"));
        inv.setItem(11, navItem(Material.DIAMOND, ChatColor.WHITE + "Upgrades",
                "Collector tier, power, luck, efficiency,", "magnet, quick hands, cooldown, speed"));
        inv.setItem(12, navItem(Material.ENDER_CHEST, ChatColor.AQUA + "Backpacks",
                "Bigger bags from your Collector Tier", "and backpack upgrades"));
        inv.setItem(13, navItem(Material.GOLDEN_HOE, ChatColor.YELLOW + "Grabbers",
                "Better collector tools with", "power, efficiency, luck & magnet"));
        inv.setItem(14, navItem(Material.BLAZE_POWDER, ChatColor.RED + "Powerups",
                "Temporary boosts (speed, luck, magnet,", "golden trash, freeze, infinite bag)"));
        inv.setItem(15, navItem(Material.ANVIL, ChatColor.GREEN + "Recycling",
                "Convert collected garbage", "into recycled materials"));

        p.openInventory(inv);
        Sfx.play(plugin, p, "guiOpen", Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private void openUpgrades(Player p, PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 45, upgradesTitle());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        inv.setItem(10, tierItem(data));
        inv.setItem(11, powerItem(data));
        inv.setItem(12, luckItem(data));
        inv.setItem(13, efficiencyItem(data));
        inv.setItem(14, magnetItem(data));
        inv.setItem(15, quickHandsItem(data));
        inv.setItem(16, cooldownItem(data));
        inv.setItem(22, speedItem(data));
        inv.setItem(36, backItem());
        p.openInventory(inv);
        Sfx.play(plugin, p, "guiOpen", Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private void openBackpacks(Player p, PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 45, backpacksTitle());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        int slot = 10;
        for (int lvl = 1; lvl <= 5 && slot <= 16; lvl++, slot++) {
            inv.setItem(slot, backpackItem(data, lvl));
        }
        inv.setItem(36, backItem());
        p.openInventory(inv);
        Sfx.play(plugin, p, "guiOpen", Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private void openTools(Player p, PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 45, toolsTitle());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        int slot = 10;
        for (String key : plugin.getConfig().getStringList("tools.order")) {
            if (slot > 16) break;
            inv.setItem(slot++, toolItem(data, key));
        }
        inv.setItem(36, backItem());
        p.openInventory(inv);
        Sfx.play(plugin, p, "guiOpen", Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private void openPowerups(Player p, PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 45, powerupsTitle());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        int slot = 10;
        for (String id : plugin.getPowerupService().ids()) {
            if (slot > 16) break;
            inv.setItem(slot++, powerupItem(data, id));
        }
        inv.setItem(36, backItem());
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

    private ItemStack navItem(Material mat, String name, String l1, String l2) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(ChatColor.GRAY + l1, ChatColor.GRAY + l2));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back to shop");
        item.setItemMeta(meta);
        return item;
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
            lore.add((cost < 0 || data.hasMoney(cost) ? ChatColor.GREEN : ChatColor.RED) + "Cost: $" + cost);
        } else {
            lore.add(ChatColor.GRAY + "Max tier reached!");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack powerItem(PlayerData data) {
        int lvl = data.getPowerLevel();
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Upgrade Power");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current level: " + ChatColor.RED + lvl);
        lore.add(ChatColor.GRAY + "Required to collect heavier garbage");
        String nextPath = "shop.power.levels." + (lvl + 1);
        if (plugin.getConfig().isConfigurationSection(nextPath)) {
            int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
            lore.add(ChatColor.GRAY + "Next lvl: " + ChatColor.RED + (lvl + 1));
            lore.add("");
            lore.add((cost < 0 || data.hasMoney(cost) ? ChatColor.GREEN : ChatColor.RED) + "Cost: $" + cost);
        } else {
            lore.add(ChatColor.GRAY + "Max level reached!");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack efficiencyItem(PlayerData data) {
        int lvl = data.getEfficiencyLevel();
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "Upgrade Efficiency");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current level: " + ChatColor.BLUE + lvl);
        lore.add(ChatColor.GRAY + "Reduces hold time & collection cooldown");
        String nextPath = "shop.efficiency.levels." + (lvl + 1);
        if (plugin.getConfig().isConfigurationSection(nextPath)) {
            int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
            lore.add(ChatColor.GRAY + "Next lvl: " + ChatColor.BLUE + (lvl + 1));
            lore.add("");
            lore.add((cost < 0 || data.hasMoney(cost) ? ChatColor.GREEN : ChatColor.RED) + "Cost: $" + cost);
        } else {
            lore.add(ChatColor.GRAY + "Max level reached!");
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
            lore.add(ChatColor.GRAY + "Auto-collects every "
                    + plugin.getConfig().getInt(nextPath + ".cooldownSeconds", 60) + "s (radius "
                    + plugin.getConfig().getInt(nextPath + ".radius", 3) + ")");
            lore.add(ChatColor.GRAY + "Next lvl: " + ChatColor.DARK_PURPLE + (lvl + 1));
            lore.add("");
            lore.add((cost < 0 || data.hasMoney(cost) ? ChatColor.GREEN : ChatColor.RED) + "Cost: $" + cost);
        } else {
            lore.add(ChatColor.GRAY + "Max magnet reached!");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack quickHandsItem(PlayerData data) {
        int lvl = data.getPickupLevel();
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Upgrade Quick Hands");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current lvl: " + ChatColor.YELLOW + lvl);
        lore.add(ChatColor.GRAY + "Reduces the time you must hold to collect");
        String nextPath = "shop.quickHands.levels." + (lvl + 1);
        if (plugin.getConfig().isConfigurationSection(nextPath)) {
            int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
            lore.add(ChatColor.GRAY + "Next: lvl " + ChatColor.YELLOW + (lvl + 1));
            lore.add("");
            lore.add((cost < 0 || data.hasMoney(cost) ? ChatColor.GREEN : ChatColor.RED) + "Cost: $" + cost);
        } else {
            lore.add(ChatColor.GRAY + "Max level reached!");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack cooldownItem(PlayerData data) {
        int lvl = data.getCooldownLevel();
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "Upgrade Cooldown");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current lvl: " + ChatColor.BLUE + lvl);
        lore.add(ChatColor.GRAY + "Reduces time between collections");
        String nextPath = "shop.cooldown.levels." + (lvl + 1);
        if (plugin.getConfig().isConfigurationSection(nextPath)) {
            int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
            lore.add(ChatColor.GRAY + "Next: lvl " + ChatColor.BLUE + (lvl + 1));
            lore.add("");
            lore.add((cost < 0 || data.hasMoney(cost) ? ChatColor.GREEN : ChatColor.RED) + "Cost: $" + cost);
        } else {
            lore.add(ChatColor.GRAY + "Max level reached!");
        }
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
            lore.add(ChatColor.GRAY + "Next: lvl " + ChatColor.AQUA + (lvl + 1));
            lore.add("");
            lore.add((cost < 0 || data.hasMoney(cost) ? ChatColor.GREEN : ChatColor.RED) + "Cost: $" + cost);
        } else {
            lore.add(ChatColor.GRAY + "Max speed reached!");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backpackItem(PlayerData data, int lvl) {
        String p = "backpacks.levels." + lvl;
        int current = data.getBackpackLevel();
        boolean owned = lvl <= current;
        boolean next = lvl == current + 1;
        ItemStack item = new ItemStack(safeMaterial(plugin.getConfig().getString(p + ".icon", "SHULKER_BOX"), Material.SHULKER_BOX));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((owned ? ChatColor.GREEN : ChatColor.GRAY)
                + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(p + ".displayName", "Backpack " + lvl))
                + (owned ? " " + ChatColor.GOLD + "(owned)" : ""));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Bag capacity multiplier: " + ChatColor.GREEN + "x"
                + plugin.getConfig().getDouble(p + ".capacityMult", 1.0));
        int base = plugin.getConfig().getInt("garbage.collector.tier" + data.getCollectorTier() + ".bagCapacity", 20);
        int capacity = (int) (base * plugin.getConfig().getDouble(p + ".capacityMult", 1.0));
        lore.add(ChatColor.GRAY + "Brings bag capacity to: " + ChatColor.AQUA + capacity);
        int cost = plugin.getConfig().getInt(p + ".cost", -1);
        if (next) {
            lore.add("");
            lore.add((cost < 0 || data.hasMoney(cost) ? ChatColor.GREEN : ChatColor.RED) + "Cost: $" + cost);
            lore.add(ChatColor.YELLOW + "Click to buy and equip");
        } else if (owned) {
            lore.add(ChatColor.GRAY + "Equipped");
        } else {
            lore.add(ChatColor.RED + "Buy the previous backpack first");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack toolItem(PlayerData data, String key) {
        String p = "tools." + key;
        List<String> order = Stats.toolOrder(plugin);
        int idx = order.indexOf(key);
        String ownedKey = Stats.toolId(plugin, data.getToolLevel());
        boolean isOwned = key.equals(ownedKey);
        boolean isNext = idx == order.indexOf(ownedKey) + 1;

        ItemStack item = new ItemStack(safeMaterial(plugin.getConfig().getString(p + ".icon", "STICK"), Material.STICK));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((isOwned ? ChatColor.GREEN : ChatColor.GRAY)
                + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(p + ".displayName", key))
                + (isOwned ? " " + ChatColor.GOLD + "(equipped)" : ""));
        List<String> lore = new ArrayList<>();
        int power = plugin.getConfig().getInt(p + ".power", 0);
        int efficiency = plugin.getConfig().getInt(p + ".efficiency", 0);
        int luck = plugin.getConfig().getInt(p + ".luck", 0);
        int magnet = plugin.getConfig().getInt(p + ".magnet", 0);
        if (power > 0) lore.add(ChatColor.RED + "+" + power + " Power");
        if (efficiency > 0) lore.add(ChatColor.BLUE + "+" + efficiency + " Efficiency");
        if (luck > 0) lore.add(ChatColor.LIGHT_PURPLE + "+" + luck + " Luck");
        if (magnet > 0) lore.add(ChatColor.DARK_PURPLE + "+" + magnet + " Magnet");
        int cost = plugin.getConfig().getInt(p + ".cost", -1);
        if (isNext) {
            lore.add("");
            lore.add((cost < 0 || data.hasMoney(cost) ? ChatColor.GREEN : ChatColor.RED) + "Cost: $" + cost);
            if (plugin.getConfig().isConfigurationSection(p + ".materials")) {
                for (String mat : plugin.getConfig().getConfigurationSection(p + ".materials").getKeys(false)) {
                    int need = plugin.getConfig().getInt(p + ".materials." + mat, 1);
                    boolean have = data.getRecycled(mat) >= need;
                    lore.add((have ? ChatColor.GREEN : ChatColor.RED) + "  "
                            + plugin.getRecycleService().displayName(mat) + " x" + need);
                }
            }
            lore.add(ChatColor.YELLOW + "Click to buy and equip");
        } else if (isOwned) {
            lore.add(ChatColor.GRAY + "Equipped");
        } else {
            lore.add(ChatColor.RED + "Buy the previous grabber first");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack powerupItem(PlayerData data, String id) {
        boolean active = plugin.getPowerupService().isActive(data, id);
        ItemStack item = new ItemStack(safeMaterial(plugin.getPowerupService().icon(id), Material.BLAZE_POWDER));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getPowerupService().displayName(id))
                + (active ? " " + ChatColor.GREEN + "(active)" : ""));
        int cost = plugin.getPowerupService().cost(id);
        int seconds = plugin.getPowerupService().durationSeconds(id);
        meta.setLore(List.of(
                ChatColor.GRAY + "Duration: " + ChatColor.WHITE + seconds + "s",
                (data.hasMoney(cost) ? ChatColor.GREEN : ChatColor.RED) + "Cost: $" + cost,
                ChatColor.YELLOW + "Click to activate"));
        item.setItemMeta(meta);
        return item;
    }

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        String viewTitle = event.getView().getTitle();
        if (!viewTitle.equals(title()) && !viewTitle.equals(upgradesTitle())
                && !viewTitle.equals(backpacksTitle()) && !viewTitle.equals(toolsTitle())
                && !viewTitle.equals(powerupsTitle())) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());

        if (viewTitle.equals(title())) {
            switch (clicked.getType()) {
                case MAP -> plugin.getZoneGui().open(p);
                case DIAMOND -> openUpgrades(p, data);
                case ENDER_CHEST -> openBackpacks(p, data);
                case GOLDEN_HOE -> openTools(p, data);
                case BLAZE_POWDER -> openPowerups(p, data);
                case ANVIL -> plugin.getRecycleGui().open(p);
                default -> { }
            }
            return;
        }
        if (viewTitle.equals(upgradesTitle())) {
            handleUpgradeClick(p, data, clicked);
        } else if (viewTitle.equals(backpacksTitle())) {
            handleBackpackClick(p, data, clicked);
        } else if (viewTitle.equals(toolsTitle())) {
            handleToolClick(p, data, clicked);
        } else if (viewTitle.equals(powerupsTitle())) {
            handlePowerupClick(p, data, clicked);
        }
        applySpeed(p, data);
        plugin.getScoreboardManager().updateForPlayer(p, data);
        plugin.getStatusBarManager().updateForPlayer(p, data);
    }

    private void handleUpgradeClick(Player p, PlayerData data, ItemStack clicked) {
        switch (clicked.getType()) {
            case ARROW -> open(p);
            case IRON_PICKAXE -> {
                int next = data.getCollectorTier() + 1;
                String nextPath = "garbage.collector.tier" + next;
                if (!plugin.getConfig().isConfigurationSection(nextPath)) {
                    p.sendMessage(ChatColor.RED + "Max tier reached!");
                    return;
                }
                int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
                if (cost >= 0 && !data.spend(cost)) {
                    p.sendMessage(ChatColor.RED + "Not enough money! You need $" + cost);
                    fail(p);
                    return;
                }
                data.setCollectorTier(next);
                p.sendMessage(ChatColor.GREEN + "Collector upgraded to Tier " + next + "!"
                        + (cost >= 0 ? " Cost: $" + cost : ""));
                success(p);
            }
            case BEACON -> upgradeLevel(p, data, "power", "Power", ChatColor.RED);
            case DIAMOND_PICKAXE -> upgradeLevel(p, data, "efficiency", "Efficiency", ChatColor.BLUE);
            case EXPERIENCE_BOTTLE -> {
                int per = plugin.getConfig().getInt("luck.upgradeMaxPerPurchase", 5);
                int cost = plugin.getConfig().getInt("luck.upgradeCostPerPoint", 50) * per;
                int max = plugin.getConfig().getInt("luck.maxLuck", 100);
                if (data.getGarbageLuck() >= max) {
                    p.sendMessage(ChatColor.RED + "Max luck reached!");
                    fail(p);
                    return;
                }
                if (data.spend(cost)) {
                    int toAdd = Math.min(per, max - data.getGarbageLuck());
                    data.addGarbageLuck(toAdd);
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "Bought +" + toAdd + " luck for $" + cost);
                    success(p);
                } else {
                    p.sendMessage(ChatColor.RED + "Not enough money!");
                    fail(p);
                }
            }
            case COMPASS -> upgradeLevel(p, data, "magnet", "Magnet", ChatColor.DARK_PURPLE);
            case FEATHER -> upgradeLevel(p, data, "quickHands", "Quick Hands", ChatColor.YELLOW);
            case CLOCK -> upgradeLevel(p, data, "cooldown", "Cooldown", ChatColor.BLUE);
            case SUGAR -> upgradeLevel(p, data, "speed", "Speed", ChatColor.AQUA);
            default -> { }
        }
    }

    private void upgradeLevel(Player p, PlayerData data, String path, String label, ChatColor color) {
        if (!plugin.getConfig().getBoolean("shop." + path + ".enabled", true)) return;
        String base = "shop." + path + ".levels.";
        int lvl = switch (path) {
            case "power" -> data.getPowerLevel();
            case "efficiency" -> data.getEfficiencyLevel();
            case "magnet" -> data.getMagnetLevel();
            case "quickHands" -> data.getPickupLevel();
            case "cooldown" -> data.getCooldownLevel();
            default -> data.getSpeedLevel();
        };
        String nextPath = base + (lvl + 1);
        if (!plugin.getConfig().isConfigurationSection(nextPath)) {
            p.sendMessage(ChatColor.RED + "Max " + label + " level reached!");
            fail(p);
            return;
        }
        int cost = plugin.getConfig().getInt(nextPath + ".cost", -1);
        if (cost >= 0 && !data.spend(cost)) {
            p.sendMessage(ChatColor.RED + "Not enough money! You need $" + cost);
            fail(p);
            return;
        }
        switch (path) {
            case "power" -> data.setPowerLevel(lvl + 1);
            case "efficiency" -> data.setEfficiencyLevel(lvl + 1);
            case "magnet" -> data.setMagnetLevel(lvl + 1);
            case "quickHands" -> data.setPickupLevel(lvl + 1);
            case "cooldown" -> data.setCooldownLevel(lvl + 1);
            default -> data.setSpeedLevel(lvl + 1);
        }
        p.sendMessage(color + label + " upgraded to lvl " + (lvl + 1)
                + (cost >= 0 ? " for $" + cost : ""));
        success(p);
    }

    private void handleBackpackClick(Player p, PlayerData data, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            open(p);
            return;
        }
        int next = data.getBackpackLevel() + 1;
        String p2 = "backpacks.levels." + next;
        if (!plugin.getConfig().isConfigurationSection(p2)) {
            p.sendMessage(ChatColor.RED + "Max backpack reached!");
            return;
        }
        int cost = plugin.getConfig().getInt(p2 + ".cost", -1);
        if (cost >= 0 && !data.spend(cost)) {
            p.sendMessage(ChatColor.RED + "Not enough money! You need $" + cost);
            fail(p);
            return;
        }
        data.setBackpackLevel(next);
        p.sendMessage(ChatColor.GREEN + "Backpack upgraded: "
                + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(p2 + ".displayName", "Pack " + next)));
        success(p);
    }

    private void handleToolClick(Player p, PlayerData data, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            open(p);
            return;
        }
        List<String> order = Stats.toolOrder(plugin);
        String ownedKey = Stats.toolId(plugin, data.getToolLevel());
        int ownedIdx = Math.max(0, order.indexOf(ownedKey));
        String nextKey = ownedIdx + 1 < order.size() ? order.get(ownedIdx + 1) : null;
        if (nextKey == null) {
            p.sendMessage(ChatColor.RED + "Best grabber already equipped!");
            fail(p);
            return;
        }
        String p2 = "tools." + nextKey;
        int cost = plugin.getConfig().getInt(p2 + ".cost", -1);
        if (cost >= 0 && !data.spend(cost)) {
            p.sendMessage(ChatColor.RED + "Not enough money! You need $" + cost);
            fail(p);
            return;
        }
        if (plugin.getConfig().isConfigurationSection(p2 + ".materials")) {
            for (String mat : plugin.getConfig().getConfigurationSection(p2 + ".materials").getKeys(false)) {
                int need = plugin.getConfig().getInt(p2 + ".materials." + mat, 1);
                if (data.getRecycled(mat) < need) {
                    if (cost >= 0) data.addMoney(cost); // refund the money part
                    p.sendMessage(ChatColor.RED + "Not enough " + plugin.getRecycleService().displayName(mat)
                            + " materials (need " + need + ")!");
                    fail(p);
                    return;
                }
            }
            plugin.getConfig().getConfigurationSection(p2 + ".materials").getKeys(false)
                    .forEach(mat -> data.addRecycled(mat, -plugin.getConfig().getInt(p2 + ".materials." + mat, 1)));
        }
        data.setToolLevel(order.indexOf(nextKey) + 1);
        p.sendMessage(ChatColor.GREEN + "Equipped "
                + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(p2 + ".displayName", nextKey)));
        success(p);
    }

    private void handlePowerupClick(Player p, PlayerData data, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            open(p);
            return;
        }
        String id = powerupIdFor(clicked);
        if (id == null) return;
        if (plugin.getPowerupService().buyAndApply(p, data, id)) {
            openPowerups(p, data);
        }
    }

    private String powerupIdFor(ItemStack clicked) {
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (name.endsWith(" (active)")) name = name.substring(0, name.length() - 9);
        for (String id : plugin.getPowerupService().ids()) {
            if (ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                    plugin.getPowerupService().displayName(id))).equals(name)) {
                return id;
            }
        }
        return null;
    }

    private void success(Player p) {
        p.closeInventory();
        Sfx.play(plugin, p, "shop", Sound.BLOCK_ANVIL_USE, 0.7f, 1.0f);
    }

    private void fail(Player p) {
        p.closeInventory();
        Sfx.play(plugin, p, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
    }

    public void applySpeed(Player p, PlayerData data) {
        String path = "shop.speed.levels." + data.getSpeedLevel();
        float walk = (float) plugin.getConfig().getDouble(path + ".walkSpeed", 0.2);
        p.setWalkSpeed(Math.min(1.0f, Math.max(0.0f, walk)));
    }

    private Material safeMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); } catch (Exception e) { return fallback; }
    }
}