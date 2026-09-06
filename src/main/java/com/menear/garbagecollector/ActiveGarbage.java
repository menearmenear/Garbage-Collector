package com.menear.garbagecollector;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

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
        for (Entity e : new Entity[] { clickEntity, visualEntity, hologramEntity }) {
            if (e != null) e.remove();
        }
    }
}