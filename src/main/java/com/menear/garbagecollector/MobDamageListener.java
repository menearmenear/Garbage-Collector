package com.menear.garbagecollector;

import com.menear.garbagecollector.service.GarbageService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class MobDamageListener implements Listener {
    private final GarbageCollectorPlugin plugin;
    private final GarbageService garbageService;

    public MobDamageListener(GarbageCollectorPlugin plugin, GarbageService garbageService) {
        this.plugin = plugin;
        this.garbageService = garbageService;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!garbageService.isGarbage(event.getEntity())) return;
        var garbage = garbageService.findGarbage(event.getEntity());
        if (garbage == null || !garbage.isMob() || !(garbage.getVisualEntity() instanceof LivingEntity mob)) return;
        int maxTiers = Math.max(1, plugin.getConfig().getInt("mob.maxTier", 5));
        garbageService.updateMobName(mob, garbage, garbage.getMobTier(), maxTiers);
    }
}