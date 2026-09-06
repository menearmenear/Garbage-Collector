package com.menear.garbagecollector.service;

import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Stats;

import java.util.ArrayList;
import java.util.List;

/**
 * Recycling: converts bag garbage into recycled materials (keyed by the
 * garbage type, so tool purchases and recycle progress share the same key).
 */
public class RecycleService {
    private final GarbageCollectorPlugin plugin;

    public RecycleService(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> typeKeys() {
        var section = Stats.section(plugin, "recycling.recipes");
        return section == null ? new ArrayList<>() : new ArrayList<>(section.getKeys(false));
    }

    public boolean hasRecipe(String type) {
        return typeKeys().contains(type);
    }

    /** Material shown for the recycled material (icon). */
    public String icon(String type) {
        return plugin.getConfig().getString("recycling.recipes." + type + ".material", "PAPER");
    }

    public String displayName(String type) {
        return plugin.getConfig().getString("recycling.recipes." + type + ".display",
                capitalize(type));
    }

    /** How many garbage pieces of this type equal 1 recycled material. */
    public int amount(String type) {
        return Math.max(1, plugin.getConfig().getInt("recycling.recipes." + type + ".amount", 1));
    }

    /**
     * Recycles the player's bag garbage of this type. Consumes whole batches,
     * removes the matching bag value and returns the number of material units.
     * Returns 0 when the player doesn't have a full batch.
     */
    public int convert(PlayerData data, String type) {
        if (!hasRecipe(type)) return 0;
        int perBatch = amount(type);
        int have = data.getGarbage(type);
        int times = have / perBatch;
        if (times <= 0) return 0;
        int valuePer = data.getGarbageValue(type) / Math.max(1, have);
        data.addGarbage(type, -times * perBatch);
        data.addGarbageValue(type, -times * perBatch * valuePer);
        data.addRecycled(type, times);
        return times;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}