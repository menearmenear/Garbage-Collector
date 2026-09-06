package com.menear.garbagecollector.service;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Sfx;
import com.menear.garbagecollector.Stats;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Garbage zones. Zones give each player a curated set of garbage types,
 * rarity weights, monsters and effects. Spawning near a player uses their
 * active zone (locked until it is paid for and the level requirement met).
 */
public class ZoneService {
    private final GarbageCollectorPlugin plugin;

    public ZoneService(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> ids() {
        var section = Stats.section(plugin, "zones");
        return section == null ? new ArrayList<>() : new ArrayList<>(section.getKeys(false));
    }

    public boolean exists(String id) {
        return id != null && ids().contains(id);
    }

    public String defaultZone() {
        List<String> all = ids();
        return all.isEmpty() ? "" : all.get(0);
    }

    public String activeZone(PlayerData data) {
        String zone = data.getActiveZone();
        return exists(zone) ? zone : defaultZone();
    }

    public String displayName(String id) {
        return Stats.zoneDisplay(plugin, id);
    }

    public String icon(String id) {
        return Stats.zoneIcon(plugin, id);
    }

    public int slot(String id) {
        return plugin.getConfig().getInt("zones." + id + ".slot", 0);
    }

    public int cost(String id) {
        return plugin.getConfig().getInt("zones." + id + ".cost", 0);
    }

    public int requiredLevel(String id) {
        return plugin.getConfig().getInt("zones." + id + ".requiredLevel", 1);
    }

    public boolean danger(String id) {
        return plugin.getConfig().getBoolean("zones." + id + ".danger", false);
    }

    public List<String> types(String id) {
        return plugin.getConfig().getStringList("zones." + id + ".types");
    }

    public List<String> monsters(String id) {
        return plugin.getConfig().getStringList("zones." + id + ".monsters");
    }

    public List<String> descriptions(String id) {
        return plugin.getConfig().getStringList("zones." + id + ".description");
    }

    /** Rarity weight override for this zone, or -1 if the zone does not override it. */
    public double rarityWeight(String id, String rarity) {
        return plugin.getConfig().getDouble("zones." + id + ".rarities." + rarity, -1);
    }

    public List<String> effectDescriptors(String id) {
        List<String> out = new ArrayList<>();
        List<?> raw = plugin.getConfig().getList("zones." + id + ".effects");
        if (raw == null) return out;
        for (Object o : raw) {
            if (o instanceof Map<?, ?> m) {
                out.add(String.valueOf(m.get("effect")));
            }
        }
        return out;
    }

    public List<PotionEffectType> effects(String id) {
        List<PotionEffectType> out = new ArrayList<>();
        for (String name : effectDescriptors(id)) {
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(name.toLowerCase()));
            if (type != null) out.add(type);
        }
        return out;
    }

    public boolean unlocked(PlayerData data, String id) {
        return data.hasZone(id) || cost(id) <= 0;
    }

    public boolean meetsLevel(PlayerData data, String id) {
        return plugin.getLevelService().level(data) >= requiredLevel(id);
    }

    /** Pays for (if needed) and enters a zone: unlocks it and sets it active. */
    public boolean enter(Player player, PlayerData data, String id) {
        if (!exists(id)) return false;
        if (!meetsLevel(data, id)) {
            player.sendActionBar(ChatColor.RED + "Need Collector Level " + requiredLevel(id) + " for this zone!");
            Sfx.play(plugin, player, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            return false;
        }
        int price = cost(id);
        if (data.hasMoney(price)) {
            if (price > 0) data.spend(price);
            data.unlockZone(id);
            data.setActiveZone(id);
            player.sendMessage(ChatColor.GREEN + "Now collecting in: " + displayName(id).replace("&", "\u00A7"));
            Sfx.play(plugin, player, "shop", Sound.BLOCK_ANVIL_USE, 0.7f, 1.0f);
            plugin.getScoreboardManager().updateForPlayer(player, data);
            plugin.getStatusBarManager().updateForPlayer(player, data);
            return true;
        }
        player.sendActionBar(ChatColor.RED + "Not enough money! You need $" + price);
        Sfx.play(plugin, player, "shopFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
        return false;
    }
}