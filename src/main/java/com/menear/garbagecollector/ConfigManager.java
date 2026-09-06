package com.menear.garbagecollector;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {
    private final GarbageCollectorPlugin plugin;

    public ConfigManager(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Makes sure the on-disk config contains every option the current plugin
     * version ships with. Missing keys from the bundled default config are
     * merged in (keeping any values the server owner already tweaked), then
     * config-version is bumped to the default's version.
     */
    public void ensureDefaults() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
            plugin.reloadConfig();
            plugin.getLogger().info("Created default config.yml.");
            return;
        }

        FileConfiguration defaults = loadBundledDefaults();
        if (defaults == null) {
            plugin.getLogger().warning("Could not read the bundled config.yml - skipping config update.");
            return;
        }

        FileConfiguration current = plugin.getConfig();
        int currentVersion = current.getInt("config-version", 0);
        int defaultVersion = defaults.getInt("config-version", 1);
        if (currentVersion >= defaultVersion) return;

        merge(defaults, current, "");
        current.set("config-version", defaultVersion);
        try {
            current.save(configFile);
            plugin.reloadConfig();
            plugin.getLogger().info("Config updated to v" + defaultVersion
                    + " (new options merged in, your custom values were kept).");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save updated config.yml: " + e.getMessage());
        }
    }

    private FileConfiguration loadBundledDefaults() {
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) return null;
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            return null;
        }
    }

    /** Adds every key present in {@code defaults} that {@code target} is missing. */
    private void merge(ConfigurationSection defaults, ConfigurationSection target, String base) {
        for (String key : defaults.getKeys(false)) {
            String path = base.isEmpty() ? key : base + "." + key;
            if (defaults.isConfigurationSection(key)) {
                if (target.isConfigurationSection(path)) {
                    merge(defaults.getConfigurationSection(key), target, path);
                } else {
                    target.set(path, defaults.getConfigurationSection(key).getValues(true));
                }
            } else if (!target.contains(path)) {
                target.set(path, defaults.get(path));
            }
        }
    }
}