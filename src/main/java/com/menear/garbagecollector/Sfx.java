package com.menear.garbagecollector;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Config-driven sound helper.
 *
 * Each event can be overridden in config.yml under "sounds.events.<event>":
 *   sounds:
 *     enabled: true
 *     events:
 *       collect:
 *         sound: ENTITY_EXPERIENCE_ORB_PICKUP
 *         volume: 0.7
 *         pitch: 1.2
 */
public final class Sfx {
    private Sfx() {}

    public static void play(GarbageCollectorPlugin plugin, Location loc, String event,
                            Sound fallback, float fallbackVolume, float fallbackPitch) {
        if (loc == null || loc.getWorld() == null) return;
        if (!enabled(plugin)) return;
        Sound sound = soundOf(plugin, event, fallback);
        if (sound == null) return;
        loc.getWorld().playSound(loc, sound,
                volumeOf(plugin, event, fallbackVolume), pitchOf(plugin, event, fallbackPitch));
    }

    public static void play(GarbageCollectorPlugin plugin, Player p, String event,
                            Sound fallback, float fallbackVolume, float fallbackPitch) {
        if (p == null || !p.isOnline()) return;
        if (!enabled(plugin)) return;
        Sound sound = soundOf(plugin, event, fallback);
        if (sound == null) return;
        p.playSound(p.getLocation(), sound,
                volumeOf(plugin, event, fallbackVolume), pitchOf(plugin, event, fallbackPitch));
    }

    private static boolean enabled(GarbageCollectorPlugin plugin) {
        return plugin.getConfig().getBoolean("sounds.enabled",
                plugin.getConfig().getBoolean("effects.sounds", true));
    }

    private static Sound soundOf(GarbageCollectorPlugin plugin, String event, Sound fallback) {
        String cfg = plugin.getConfig().getString("sounds.events." + event + ".sound");
        if (cfg == null || cfg.isBlank()) return fallback;
        try {
            return Sound.valueOf(cfg.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException ignored) { }
        Sound resolved = Registry.SOUNDS.get(NamespacedKey.minecraft(
                cfg.trim().toLowerCase(Locale.ROOT).replace(' ', '_')));
        if (resolved == null) {
            plugin.getLogger().warning("Unknown sound in config for " + event + ": " + cfg);
            return fallback;
        }
        return resolved;
    }

    private static float volumeOf(GarbageCollectorPlugin plugin, String event, float fallback) {
        return (float) plugin.getConfig().getDouble("sounds.events." + event + ".volume", fallback);
    }

    private static float pitchOf(GarbageCollectorPlugin plugin, String event, float fallback) {
        return (float) plugin.getConfig().getDouble("sounds.events." + event + ".pitch", fallback);
    }
}