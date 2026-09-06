package com.menear.garbagecollector.commands;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdminTabCompleter implements TabCompleter {
    private final GarbageCollectorPlugin plugin;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "give", "spawnburst", "stats", "luck", "reset", "reload", "help");

    public AdminTabCompleter(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String typed = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(typed))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2 && (sub.equals("give") || sub.equals("stats") || sub.equals("luck") || sub.equals("reset"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(typed))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && sub.equals("give") && plugin.getConfig().isConfigurationSection("garbage.types")) {
            return plugin.getConfig().getConfigurationSection("garbage.types").getKeys(false).stream()
                    .filter(k -> k.toLowerCase().startsWith(typed))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}