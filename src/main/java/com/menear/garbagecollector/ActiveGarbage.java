package com.menear.garbagecollector;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActiveGarbage {
    public final UUID id;
    public final String typeName;
    public final int baseValue;
    public final String rarityName;
    public final int rarityXp;
    public final double rarityMult;
    public final String colorHex;
    public final DyeColor glowColor;
    public final long spawnedAt;
    public final boolean mob;
    public final Location location;

    private Entity clickEntity;   // Interaction entity (item garbage)
    private Entity visualEntity;  // ItemDisplay (item garbage) or mob entity
    private Entity hologramEntity;
    private boolean glowing;
    private int mobTier = 1;
    private String mobId;
    private String monsterName;
    private boolean miniBoss;
    private BossBar bossBar;
    private final Map<UUID, Double> bossDamage = new HashMap<>();
    private String zoneId;
    private double mobValueMult = 1.0;
    private int mobXp = 10;

    public ActiveGarbage(UUID id, String typeName, int baseValue, String rarityName, int rarityXp,
                         double rarityMult, String colorHex, DyeColor glowColor, Location location, boolean mob) {
        this.id = id;
        this.typeName = typeName;
        this.baseValue = baseValue;
        this.rarityName = rarityName;
        this.rarityXp = rarityXp;
        this.rarityMult = rarityMult;
        this.colorHex = colorHex;
        this.glowColor = glowColor;
        this.location = location;
        this.mob = mob;
        this.spawnedAt = System.currentTimeMillis();
    }

    public void setEntities(Entity click, Entity visual, Entity hologram) {
        this.clickEntity = click;
        this.visualEntity = visual;
        this.hologramEntity = hologram;
    }

    public Entity getClickEntity() { return clickEntity; }
    public Entity getVisualEntity() { return visualEntity; }

    public boolean isMob() { return mob; }
    public boolean isItem() { return !mob; }

    public void setMob(LivingEntity mob, int tier) { this.mobTier = Math.max(1, tier); }
    public int getMobTier() { return mobTier; }

    public void setMobInfo(String mobId, String monsterName, boolean miniBoss) {
        this.mobId = mobId;
        this.monsterName = monsterName;
        this.miniBoss = miniBoss;
    }
    public String getMobId() { return mobId; }
    public String getMonsterName() { return monsterName; }
    public boolean isMiniBoss() { return miniBoss; }

    public void setBossBar(BossBar bossBar) { this.bossBar = bossBar; }
    public BossBar getBossBar() { return bossBar; }

    public void addBossDamage(Player player, double damage) {
        bossDamage.merge(player.getUniqueId(), damage, Double::sum);
    }
    public UUID getTopDamager() {
        UUID best = null;
        double bestDmg = 0;
        for (Map.Entry<UUID, Double> e : bossDamage.entrySet()) {
            if (e.getValue() > bestDmg) { bestDmg = e.getValue(); best = e.getKey(); }
        }
        return best;
    }

    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public String getZoneId() { return zoneId; }

    public void setMobValueMult(double mult) { this.mobValueMult = Math.max(1.0, mult); }
    public double getMobValueMult() { return mobValueMult; }

    public void setMobXp(int xp) { this.mobXp = Math.max(1, xp); }
    public int getMobXp() { return mobXp; }

    public Entity raycastTarget() {
        return visualEntity != null ? visualEntity : clickEntity;
    }

    public boolean isGlowing() { return glowing; }

    public void setGlowing(boolean value) {
        if (this.glowing == value || visualEntity == null) return;
        this.glowing = value;
        if (visualEntity instanceof org.bukkit.entity.ItemDisplay display) {
            display.setGlowing(value);
            if (value) display.setGlowColorOverride(glowColor.getColor());
        } else if (visualEntity instanceof LivingEntity living) {
            living.setGlowing(value);
        }
    }

    public void remove() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        for (Entity e : new Entity[] { clickEntity, visualEntity, hologramEntity }) {
            if (e != null) e.remove();
        }
    }
}