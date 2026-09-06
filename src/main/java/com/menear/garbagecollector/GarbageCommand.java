package com.menear.garbagecollector;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class GarbageCommand implements CommandExecutor {
    private final GarbageCollectorPlugin plugin;

    public GarbageCommand(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Garbage Collector plugin v" + plugin.getDescription().getVersion());
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("garbage.admin")) {
                sender.sendMessage("No permission");
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage("Config reloaded");
            return true;
        }
        return false;
    }
}
