package com.menear.garbagecollector.ui;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Sfx;
import com.menear.garbagecollector.service.MissionService.Contract;
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

public class MissionGui {
    private final GarbageCollectorPlugin plugin;
    private final int[] SLOTS = {10, 11, 12};

    public MissionGui(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public static String title() {
        return ChatColor.translateAlternateColorCodes('&', "&8Daily Contracts");
    }

    public void open(Player p) {
        PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
        plugin.getMissionService().refresh(data);
        Inventory inv = Bukkit.createInventory(null, 45, title());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        for (int i = 0; i < SLOTS.length; i++) {
            String id = data.getMissionId(i);
            if (id == null) continue;
            Contract c = plugin.getMissionService().contract(id);
            if (c == null) continue;
            inv.setItem(SLOTS[i], missionItem(data, i, c));
        }
        ItemStack claim = new ItemStack(Material.EMERALD);
        ItemMeta cm = claim.getItemMeta();
        cm.setDisplayName(ChatColor.GREEN + "Claim rewards");
        cm.setLore(List.of(ChatColor.GRAY + "Click a mission to claim its reward when done."));
        claim.setItemMeta(cm);
        inv.setItem(31, claim);
        p.openInventory(inv);
        Sfx.play(plugin, p, "guiOpen", Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack missionItem(PlayerData data, int index, Contract c) {
        int progress = data.getMissionProgress(index);
        boolean done = progress >= c.target;
        boolean claimed = data.isMissionClaimed(c.id);
        ItemStack item = new ItemStack(done ? Material.EMERALD : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Contract #" + (index + 1));
        List<String> lore = new ArrayList<>();
        if (c.type.equals("findRare") || c.type.equals("kill") || c.type.equals("recycle")) {
            lore.add(ChatColor.WHITE + plugin.getMissionService().description(c));
        } else {
            lore.add(ChatColor.WHITE + plugin.getMissionService().description(c));
        }
        lore.add(ChatColor.GRAY + "Progress: " + (done ? ChatColor.GREEN : ChatColor.YELLOW)
                + Math.min(progress, c.target) + "/" + c.target);
        lore.add("");
        StringBuilder reward = new StringBuilder("Rewards:");
        if (c.rewardMoney > 0) reward.append(" $" + c.rewardMoney);
        if (!c.rewardMaterials.isEmpty()) reward.append(" materials");
        if (c.rewardXp > 0) reward.append(" +" + c.rewardXp + " xp");
        if (c.rewardLuck > 0) reward.append(" +" + c.rewardLuck + " luck");
        lore.add(ChatColor.GREEN + reward.toString());
        lore.add("");
        if (claimed) {
            lore.add(ChatColor.DARK_GRAY + "Reward claimed");
        } else if (done) {
            lore.add(ChatColor.GOLD + "Click to claim!");
        } else {
            lore.add(ChatColor.GRAY + "Not complete yet");
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
        for (int i = 0; i < SLOTS.length; i++) {
            if (event.getSlot() == SLOTS[i]) {
                plugin.getMissionService().claim(p, data, i);
                open(p);
                return;
            }
        }
    }
}