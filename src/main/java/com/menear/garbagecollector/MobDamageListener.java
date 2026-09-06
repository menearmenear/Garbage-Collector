package com.menear.garbagecollector;

import com.menear.garbagecollector.service.GarbageService;
import org.bukkit.Sound;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
        Sfx.play(plugin, event.getEntity().getLocation(), "mobHurt", Sound.ENTITY_ZOMBIE_HURT, 0.5f, 1.0f);
    }

    // Mini-boss damage tracking: records the damage each player dealt for the
    // top-damager reward and keeps the boss bar in sync.
    @EventHandler
    public void onPlayerDamageBoss(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player p) || p.isDead()) return;
        ActiveGarbage garbage = garbageService.findGarbage(event.getEntity());
        if (garbage == null || !garbage.isMiniBoss()) return;
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        garbage.addBossDamage(p, event.getFinalDamage());
        BossBar bar = garbage.getBossBar();
        if (bar == null) return;
        bar.setProgress(Math.max(0.0, Math.min(1.0, mob.getHealth() / Math.max(1.0, mob.getMaxHealth()))));
        if (p.getLocation().distanceSquared(mob.getLocation()) <= 32 * 32 && !bar.getPlayers().contains(p)) {
            bar.addPlayer(p);
        }
    }
}