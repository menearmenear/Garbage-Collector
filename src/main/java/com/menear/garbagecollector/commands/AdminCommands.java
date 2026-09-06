package com.menear.garbagecollector.commands;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.service.GarbageService;
import com.menear.garbagecollector.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sender.sendMessage("Garbage Collector v" + plugin.getDescription().getVersion());
            sender.sendMessage("/garbage give <player> [type]");
            sender.sendMessage("/garbage spawnburst <amount>");
            sender.sendMessage("/garbage stats <player>");
            sender.sendMessage("/garbage luck <player> <amount>");
            sender.sendMessage("/garbage reset <player>");
            sender.sendMessage("/garbage reload");
            return true;
        }

        String sub = args[0].toLowerCase();

        if ("reload".equals(sub)) {
            if (!hasAdmin(sender)) { sender.sendMessage("No permission"); return true; }
            plugin.reloadConfig();
            garbageService.reload();
            sender.sendMessage("Garbage config reloaded.");
            return true;
        }

        if ("give".equals(sub)) {
            if (!hasAdmin(sender)) { sender.sendMessage("No permission"); return true; }
            if (args.length < 2) {
                sender.sendMessage("Usage: /garbage give <player> [type] [tier] [luck]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage("Player not found"); return true; }
            String type = args.length > 2 ? args[2] : null;
            PlayerData data = playerService.getPlayerData(target.getUniqueId());
            // optional tier/luck set
            if (args.length > 3) {
                try { data.setCollectorTier(Integer.parseInt(args[3])); } catch (NumberFormatException ignored) {}
            }
            if (args.length > 4) {
                try { data.setGarbageLuck(Integer.parseInt(args[4])); } catch (NumberFormatException ignored) {}
            }
            garbageService.spawnAt(target.getLocation(), type);
            sender.sendMessage("Spawned garbage near " + target.getName()
                    + (type != null ? " (" + type + ")" : ""));
            return true;
        }

        if ("spawn".equals(sub)) {
            if (!hasAdmin(sender)) { sender.sendMessage("No permission"); return true; }
            int amount;
            try {
                amount = args.length > 1 ? Integer.parseInt(args[1]) : 1;
            } catch (NumberFormatException e) {
                sender.sendMessage("Usage: /garbage spawn <amount>");
                return true;
            }
            amount = Math.max(1, Math.min(100, amount));
            Location loc = sender instanceof Player p ? p.getLocation() : worldSpawnLocation();
            if (loc == null) { sender.sendMessage("No world to spawn in"); return true; }
            garbageService.spawnBurst(loc, amount);
            sender.sendMessage("Spawned " + amount + " garbage scattered around.");
            return true;
        }

        if ("spawnburst".equals(sub)) {
            if (!hasAdmin(sender)) { sender.sendMessage("No permission"); return true; }
            int amount;
            try {
                amount = args.length > 1 ? Integer.parseInt(args[1]) : 10;
            } catch (NumberFormatException e) {
                sender.sendMessage("Usage: /garbage spawnburst <amount>");
                return true;
            }
            amount = Math.max(1, Math.min(100, amount));
            Location loc = sender instanceof Player p ? p.getLocation() : worldSpawnLocation();
            if (loc == null) { sender.sendMessage("No world to spawn in"); return true; }
            garbageService.spawnBurst(loc, amount);
            sender.sendMessage("Spawned " + amount + " garbage nearby.");
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
            sender.sendMessage("Money: " + data.getMoney() + "  Total earned: " + data.getTotalEarned()
                    + "  Collected: " + data.getCollected());
            sender.sendMessage("Bag: " + data.totalGarbage() + " items worth $" + data.totalGarbageValue());
            sender.sendMessage("Tier: " + data.getCollectorTier() + "  Luck: " + data.getGarbageLuck()
                    + "  Speed lvl: " + data.getSpeedLevel() + "  Magnet lvl: " + data.getMagnetLevel());
            sender.sendMessage("Quick Hands lvl: " + data.getPickupLevel() + "  Cooldown lvl: " + data.getCooldownLevel());
            sender.sendMessage("Garbage counts: " + data.getGarbageCount());
            return true;
        }

        if ("luck".equals(sub)) {
            if (!hasAdmin(sender)) { sender.sendMessage("No permission"); return true; }
            if (args.length < 3) {
                sender.sendMessage("Usage: /garbage luck <player> <amount>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage("Player not found"); return true; }
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("Usage: /garbage luck <player> <amount>");
                return true;
            }
            playerService.getPlayerData(target.getUniqueId()).addGarbageLuck(amount);
            sender.sendMessage("Updated " + target.getName() + " luck to "
                    + playerService.getPlayerData(target.getUniqueId()).getGarbageLuck());
            return true;
        }

        if ("reset".equals(sub)) {
            if (!hasAdmin(sender)) { sender.sendMessage("No permission"); return true; }
            if (args.length < 2) {
                sender.sendMessage("Usage: /garbage reset <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage("Player not found"); return true; }
            playerService.reset(target.getUniqueId());
            plugin.getScoreboardManager().updateForPlayer(target, playerService.getPlayerData(target.getUniqueId()));
            sender.sendMessage("Reset " + target.getName() + " data.");
            return true;
        }

        return false;
    }

    private Location worldSpawnLocation() {
        if (!Bukkit.getWorlds().isEmpty()) {
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        return null;
    }

    private boolean hasAdmin(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return true;
        return sender.hasPermission(plugin.getConfig().getString("admin.permission", "garbage.admin"));
    }
}