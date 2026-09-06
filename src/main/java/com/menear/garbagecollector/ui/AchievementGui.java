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

public class AchievementGui {
    private final GarbageCollectorPlugin plugin;

    public AchievementGui(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public static String title() {
        return ChatColor.translateAlternateColorCodes('&', "&8Achievements");
    }

    private final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public void open(Player p) {
        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 45, title());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        List<String> ids = plugin.getAchievementService().ids();
        for (int i = 0; i < ids.size() && i < SLOTS.length; i++) {
            inv.setItem(SLOTS[i], item(data, ids.get(i)));
        }
        p.openInventory(inv);
        Sfx.play(plugin, p, "guiOpen", Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack item(PlayerData data, String id) {
        String p = "achievements." + id;
        boolean unlocked = data.hasAchievement(id);
        ItemStack item = new ItemStack(unlocked ? Material.EMERALD : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((unlocked ? ChatColor.GREEN : ChatColor.GRAY)
                + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(p + ".name", id)));
        List<String> lore = new ArrayList<>();
        lore.add(unlocked
                ? ChatColor.GREEN + "Unlocked"
                : ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(p + ".desc", "?") + " " + plugin.getConfig().getInt(p + ".target", 1)));
        lore.add("");
        StringBuilder reward = new StringBuilder("Rewards:");
        int money = plugin.getConfig().getInt(p + ".rewardMoney", 0);
        int xp = plugin.getConfig().getInt(p + ".rewardXp", 0);
        int luck = plugin.getConfig().getInt(p + ".rewardLuck", 0);
        int mats = plugin.getConfig().isConfigurationSection(p + ".rewardMaterials")
                ? plugin.getConfig().getConfigurationSection(p + ".rewardMaterials").getKeys(false).size() : 0;
        if (money > 0) reward.append(" $" + money);
        if (xp > 0) reward.append(" +" + xp + " xp");
        if (luck > 0) reward.append(" +" + luck + " luck");
        if (mats > 0) reward.append(" materials");
        lore.add(ChatColor.GREEN + reward.toString());
        if (!unlocked) {
            String type = plugin.getConfig().getString(p + ".type", "collectTotal");
            int target = plugin.getConfig().getInt(p + ".target", 1);
            int progress = switch (type) {
                case "collectTotal" -> data.getCollected();
                case "earnTotal" -> data.getTotalEarned();
                case "killTotal" -> data.mobKillsTotal();
                case "mythicFound" -> data.getMythicFound();
                case "collectType" -> data.getGarbage(plugin.getConfig().getString(p + ".targetType", ""));
                case "killMonster" -> data.getMobKillsById(plugin.getConfig().getString(p + ".monster", ""));
                default -> 0;
            };
            lore.add(ChatColor.GRAY + "Progress: " + ChatColor.YELLOW + Math.min(progress, target) + "/" + target);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(title())) return;
        event.setCancelled(true);
    }
}