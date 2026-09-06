package com.menear.garbagecollector.commands;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.service.GarbageService;
import com.menear.garbagecollector.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class AdminCommands implements CommandExecutor {
    private final GarbageCollectorPlugin plugin;
    private final GarbageService garbageService;
    private final PlayerService playerService;

    public AdminCommands(GarbageCollectorPlugin plugin, GarbageService garbageService, PlayerService playerService) {
        this.plugin = plugin;
        this.garbageService = garbageService;
        this.playerService = playerService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Garbage plugin v" + plugin.getDescription().getVersion());
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("reload".equals(sub)) {
            if (!hasAdmin(sender)) { sender.sendMessage("No permission"); return true; }
            plugin.reloadConfig();
            sender.sendMessage("Garbage config reloaded.");
            return true;
        }
        if ("give".equals(sub)) {
            if (!hasAdmin(sender)) { sender.sendMessage("No permission"); return true; }
            if (args.length < 3) {
                sender.sendMessage("Usage: /garbage give <player> <type>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage("Player not found"); return true; }
            String type = args[2];
            // spawn one garbage at target location
            garbageService.spawnAt(target.getLocation());
            sender.sendMessage("Spawned garbage near " + target.getName());
            return true;
        }
        if ("stats".equals(sub)) {
            if (!hasAdmin(sender)) { sender.sendMessage("No permission"); return true; }
            if (args.length < 2) {
                sender.sendMessage("Usage: /garbage stats <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage("Player not found"); return true; }
            PlayerData data = playerService.getPlayerData(target.getUniqueId());
            sender.sendMessage("Player: " + target.getName());
            sender.sendMessage("Money: " + data.getMoney() + " Collected: " + data.getCollected() + " Bag: " + data.getBagCapacity() + " Water: " + data.getWater());
            return true;
        }

        return false;
    }

    private boolean hasAdmin(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return true;
        return sender.hasPermission(plugin.getConfig().getString("admin.permission", "garbage.admin"));
    }
}
