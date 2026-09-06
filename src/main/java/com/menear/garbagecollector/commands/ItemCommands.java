package com.menear.garbagecollector.commands;

import com.menear.garbagecollector.CollectorItem;
import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ItemCommands implements CommandExecutor {
    private final GarbageCollectorPlugin plugin;

    public ItemCommands(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        String cmd = command.getName().toLowerCase();
        if ("sell".equals(cmd)) {
            plugin.getSellGui().open(p);
            return true;
        }
        if ("shop".equals(cmd)) {
            plugin.getShopGui().open(p);
            return true;
        }
        if ("garbageitem".equals(cmd)) {
            PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
            p.getInventory().addItem(CollectorItem.build(plugin, data));
            p.sendMessage(ChatColor.GREEN + "Here is your Garbage Collector!");
            return true;
        }
        if ("collection".equals(cmd)) {
            plugin.getCollectionGui().open(p, "garbage");
            return true;
        }
        if ("recycle".equals(cmd)) {
            plugin.getRecycleGui().open(p);
            return true;
        }
        if ("zone".equals(cmd)) {
            plugin.getZoneGui().open(p);
            return true;
        }
        if ("missions".equals(cmd)) {
            plugin.getMissionGui().open(p);
            return true;
        }
        if ("achievements".equals(cmd)) {
            plugin.getAchievementGui().open(p);
            return true;
        }
        if ("leaderboard".equals(cmd)) {
            plugin.getLeaderboardGui().open(p);
            return true;
        }
        return false;
    }
}