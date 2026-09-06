package com.menear.garbagecollector.service;

import com.menear.garbagecollector.ActiveGarbage;
import com.menear.garbagecollector.CollectionResult;
import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Sfx;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BasicGarbageService implements GarbageService {
    private final GarbageCollectorPlugin plugin;
    private final Random random = new Random();
    private final NamespacedKey garbageKey;
    private final Map<UUID, ActiveGarbage> garbages = new ConcurrentHashMap<>();
    private final List<Rarity> rarities = new ArrayList<>();

    private static final class Rarity {
        final String name; final int weight; final String colorHex;
        final DyeColor glow; final int xp; final double multiplier;
        Rarity(String name, int weight, String colorHex, DyeColor glow, int xp, double multiplier) {
            this.name = name; this.weight = weight; this.colorHex = colorHex;
            this.glow = glow; this.xp = xp; this.multiplier = multiplier;
        }
    }

    public BasicGarbageService(GarbageCollectorPlugin plugin) {
        this.plugin = plugin;
        this.garbageKey = new NamespacedKey(plugin, "gc");
        reloadRarities();
    }

    // -------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------
    private void reloadRarities() {
        rarities.clear();
        if (plugin.getConfig().isConfigurationSection("garbage.rarities")) {
            for (String name : plugin.getConfig().getConfigurationSection("garbage.rarities").getKeys(false)) {
                String p = "garbage.rarities." + name;
                try {
                    rarities.add(new Rarity(
                            name,
                            plugin.getConfig().getInt(p + ".weight", 10),
                            plugin.getConfig().getString(p + ".color", "#FFFFFF"),
                            safeDye(plugin.getConfig().getString(p + ".glow", "WHITE")),
                            plugin.getConfig().getInt(p + ".xp", 1),
                            plugin.getConfig().getDouble(p + ".valueMultiplier", 1.0)
                    ));
                } catch (Exception e) {
                    plugin.getLogger().warning("Skipping bad rarity config: " + name);
                }
            }
        }
        if (rarities.isEmpty()) {
            rarities.add(new Rarity("common", 70, "#FFFFFF", DyeColor.WHITE, 1, 1.0));
        }
    }

    private DyeColor safeDye(String name) {
        try { return DyeColor.valueOf(name.toUpperCase()); } catch (Exception e) { return DyeColor.WHITE; }
    }

    @Override
    public void start() {
        reloadRarities();
        int interval = Math.max(1, plugin.getConfig().getInt("spawn.intervalSeconds", 10));
        Bukkit.getScheduler().runTaskTimer(plugin, this::spawnTick, 60L, interval * 20L);
        int glowTick = 4;
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateGlow, 20L, glowTick);
        Bukkit.getScheduler().runTaskTimer(plugin, this::despawnExpired, 60L, 40L);
    }

    // -------------------------------------------------------------
    // Spawning
    // -------------------------------------------------------------
    private void spawnTick() {
        String mode = plugin.getConfig().getString("spawn.mode", "mixed");
        boolean nearPlayer = "players".equalsIgnoreCase(mode);
        if ("mixed".equalsIgnoreCase(mode)) {
            nearPlayer = random.nextDouble() * 100 < plugin.getConfig().getDouble("spawn.playerChance", 60);
        }

        Location loc;
        if (nearPlayer) {
            Player target = randomPlayer();
            if (target == null) loc = worldSpawnLocation();
            else {
                double r = plugin.getConfig().getDouble("spawn.playerRadius", 12);
                double a = random.nextDouble() * Math.PI * 2;
                double d = random.nextDouble() * r;
                loc = target.getLocation().clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
            }
        } else {
            loc = worldSpawnLocation();
        }

        double mobChance = plugin.getConfig().getDouble("spawn.mobChance", 15);
        if (random.nextDouble() * 100 < mobChance) {
            spawnMob(ground(loc));
        } else {
            spawnItem(ground(loc), null);
        }
    }

    private Player randomPlayer() {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return null;
        return online.get(random.nextInt(online.size()));
    }

    private Location worldSpawnLocation() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("spawn.world", "world"));
        if (world == null && !Bukkit.getWorlds().isEmpty()) world = Bukkit.getWorlds().get(0);
        if (world == null) return null;
        Location spawn = world.getSpawnLocation().clone();
        double radius = plugin.getConfig().getDouble("spawn.spawnRadius", 30);
        double a = random.nextDouble() * Math.PI * 2;
        double d = random.nextDouble() * radius;
        spawn.add(Math.cos(a) * d, 0, Math.sin(a) * d);
        return spawn;
    }

    private Location ground(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        int y = loc.getWorld().getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
        return new Location(loc.getWorld(), loc.getBlockX() + 0.5, y + 1.0, loc.getBlockZ() + 0.5);
    }

    private String randomTypeName() {
        List<String> types = new ArrayList<>(plugin.getConfig().getConfigurationSection("garbage.types").getKeys(false));
        return types.isEmpty() ? "paper" : types.get(random.nextInt(types.size()));
    }

    private Rarity rollRarity() {
        int total = rarities.stream().mapToInt(r -> r.weight).sum();
        if (total <= 0) return rarities.get(0);
        int roll = random.nextInt(total);
        for (Rarity r : rarities) {
            roll -= r.weight;
            if (roll < 0) return r;
        }
        return rarities.get(0);
    }

    private Material safeMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); } catch (Exception e) { return fallback; }
    }

    // -------------------------------------------------------------
    // Item garbage (Interaction + ItemDisplay + TextDisplay)
    // -------------------------------------------------------------
    private void spawnItem(Location loc, String forcedType) {
        spawnItem(loc, forcedType, 1.0);
    }

    private void spawnItem(Location loc, String forcedType, double valueMult) {
        if (loc == null || loc.getWorld() == null) return;
        Rarity r = rollRarity();
        String typeName = forcedType != null && plugin.getConfig().isConfigurationSection("garbage.types." + forcedType)
                ? forcedType : randomTypeName();
        int baseValue = plugin.getConfig().getInt("garbage.types." + typeName + ".value", 1);
        Material mat = safeMaterial(plugin.getConfig().getString("garbage.types." + typeName + ".material", "PAPER"), Material.PAPER);

        UUID id = UUID.randomUUID();
        ActiveGarbage g = new ActiveGarbage(id, typeName, baseValue, r.name, r.xp, r.multiplier * valueMult,
                r.colorHex, r.glow, loc, false);

        World world = loc.getWorld();
        Interaction inter = world.spawn(loc, Interaction.class);
        inter.setInteractionWidth(0.6f);
        inter.setInteractionHeight(0.6f);
        inter.setPersistent(true);
        inter.getPersistentDataContainer().set(garbageKey, PersistentDataType.STRING, id.toString());

        ItemDisplay display = world.spawn(loc.clone().add(0, 0.5, 0), ItemDisplay.class);
        display.setItemStack(new ItemStack(mat));
        display.setTransformation(new Transformation(new Vector3f(0, -0.42f, 0), new Quaternionf(),
                new Vector3f(0.9f), new Quaternionf()));
        display.setInterpolationDelay(-1);
        display.setViewRange(16f);
        display.setPersistent(true);

        int shown = Math.max(1, (int) Math.round(baseValue * r.multiplier * valueMult));
        TextDisplay text = world.spawn(loc.clone().add(0, 1.35, 0), TextDisplay.class);
        text.setText(colorize(typeName + " $" + shown, r.colorHex));
        text.setBillboard(Display.Billboard.CENTER);
        text.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                new Vector3f(0.5f), new Quaternionf()));
        text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        text.setSeeThrough(false);
        text.setShadowed(false);
        text.setViewRange(16f);
        text.setInterpolationDelay(-1);
        text.setPersistent(true);

        g.setEntities(inter, display, text);
        garbages.put(id, g);
        spawnEffect(loc, r, false);
    }

    // -------------------------------------------------------------
    // Trash Monster mob
    // -------------------------------------------------------------
    private void spawnMob(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        Rarity r = rollRarity();
        String typeName = randomTypeName();
        int baseValue = plugin.getConfig().getInt("garbage.types." + typeName + ".value", 1);
        Material mat = safeMaterial(plugin.getConfig().getString("garbage.types." + typeName + ".material", "PAPER"), Material.PAPER);

        int tier = randomMobTier();
        int maxTiers = mobTierCount();
        double hpMult = mobHealthMult(tier);
        double valueMultMob = mobValueMult(tier);

        String mobType = plugin.getConfig().getString("mob.type", "ZOMBIE");
        EntityType et;
        try { et = EntityType.valueOf(mobType.toUpperCase()); } catch (Exception e) { et = EntityType.ZOMBIE; }

        LivingEntity mob = (LivingEntity) loc.getWorld().spawnEntity(loc, et);
        mob.setPersistent(true);
        mob.setCanPickupItems(false);
        mob.getEquipment().setHelmet(new ItemStack(mat));

        double maxHealth = Math.max(1.0, 20.0 * hpMult);
        mob.setMaxHealth(maxHealth);
        mob.setHealth(maxHealth);

        UUID id = UUID.randomUUID();
        ActiveGarbage g = new ActiveGarbage(id, typeName, baseValue, r.name, r.xp, r.multiplier,
                r.colorHex, r.glow, loc, true);
        g.setMob(mob, tier);
        mob.getPersistentDataContainer().set(garbageKey, PersistentDataType.STRING, id.toString());
        g.setEntities(null, mob, null);
        garbages.put(id, g);
        updateMobName(mob, g, tier, maxTiers);
        Sfx.play(plugin, loc, "mobSpawn", Sound.ENTITY_ZOMBIE_AMBIENT, 0.5f, 1.0f);
        spawnEffect(loc, r, false);
    }

    private int mobTierCount() {
        return Math.max(1, plugin.getConfig().getInt("mob.maxTier", 5));
    }

    private int randomMobTier() {
        int max = mobTierCount();
        // weighted toward lower tiers
        return 1 + (int) (random.nextDouble() * random.nextDouble() * max);
    }

    private double mobHealthMult(int tier) {
        return plugin.getConfig().getDouble("mob.tier" + tier + ".healthMult",
                plugin.getConfig().getDouble("mob.healthPerTier", 1.5) * (tier - 1) + 1);
    }

    private double mobValueMult(int tier) {
        return plugin.getConfig().getDouble("mob.tier" + tier + ".valueMult",
                plugin.getConfig().getDouble("mob.valuePerTier", 1.5) * (tier - 1) + 1);
    }

    @Override
    public void updateMobName(org.bukkit.entity.LivingEntity mob, ActiveGarbage g, int tier, int maxTiers) {
        if (mob == null || g == null) return;
        double health = Math.max(0, mob.getHealth());
        double max = Math.max(1, mob.getMaxHealth());
        int heartsCount = (int) Math.ceil(health / 2.0);
        int maxHeartsCount = (int) Math.ceil(max / 2.0);
        String baseName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("mob.name", "&cTrash Monster"));
        String tierName = ChatColor.YELLOW + " " + romanNumeral(tier);
        String heartsStr = "§c" + "♥".repeat(heartsCount) + "§7" + "♥".repeat(Math.max(0, maxHeartsCount - heartsCount));
        String hp = ChatColor.GRAY + " " + heartsStr + " §f" + heartsCount + "/" + maxHeartsCount;
        mob.setCustomName(baseName + tierName + hp);
        mob.setCustomNameVisible(true);
    }

    private String romanNumeral(int n) {
        String[] symbols = {"X", "IX", "V", "IV", "I"};
        int[] values = {10, 9, 5, 4, 1};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (n >= values[i]) {
                sb.append(symbols[i]);
                n -= values[i];
            }
        }
        return sb.toString();
    }

    // -------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------
    @Override
    public boolean isGarbage(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(garbageKey, PersistentDataType.STRING);
    }

    @Override
    public ActiveGarbage findGarbage(Entity entity) {
        for (ActiveGarbage g : garbages.values()) {
            if ((g.getClickEntity() != null && g.getClickEntity() == entity)
                    || (g.getVisualEntity() != null && g.getVisualEntity() == entity)) {
                return g;
            }
        }
        return null;
    }

    // -------------------------------------------------------------
    // Collection
    // -------------------------------------------------------------
    private boolean hasTool(Player p) {
        if (!plugin.getConfig().getBoolean("tool.requireTool", true)) return true;
        String required = plugin.getConfig().getString("tool.material", "STICK");
        return p.getInventory().getItemInMainHand().getType() == safeMaterial(required, Material.STICK);
    }

    @Override
    public void spawnAt(Location location) {
        spawnItem(ground(location), null);
    }

    @Override
    public void spawnAt(Location location, String forcedType) {
        spawnItem(ground(location), forcedType);
    }

    @Override
    public void spawnBurst(Location location, int amount) {
        if (location == null || location.getWorld() == null) return;
        double spreadRadius = Math.max(8, Math.min(60, plugin.getConfig().getInt("spawn.burstSpread", 20)));
        // keeps existing spam protection: hard cap
        for (int i = 0; i < amount; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double d = Math.sqrt(random.nextDouble()) * spreadRadius;
            Location loc = location.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
            spawnItem(ground(loc), null);
        }
    }

    public double collectorValueMultiplier(PlayerData data) {
        String tier = "tier" + data.getCollectorTier();
        return plugin.getConfig().getDouble("garbage.collector." + tier + ".valueMultiplier",
                plugin.getConfig().getDouble("garbage.collector.tier1.valueMultiplier", 1.0));
    }

    public int collectorBagCapacity(PlayerData data) {
        String tier = "tier" + data.getCollectorTier();
        return plugin.getConfig().getInt("garbage.collector." + tier + ".bagCapacity",
                plugin.getConfig().getInt("garbage.collector.tier1.bagCapacity", 20));
    }

    @Override
    public CollectionResult collect(Player player, ActiveGarbage garbage) {
        if (player == null || !player.isOnline() || garbage == null) return CollectionResult.fail("");
        PlayerData data = plugin.getPlayerService().getPlayerData(player.getUniqueId());

        if (!hasTool(player)) {
            String name = plugin.getConfig().getString("tool.name", "&6Trash Grabber").replace("&", "\u00A7");
            player.sendActionBar(ChatColor.RED + "You need the " + name + "!");
            Sfx.play(plugin, player, "collectFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            return CollectionResult.fail("no tool");
        }
        if (data.getCollected() >= collectorBagCapacity(data)) {
            player.sendActionBar(ChatColor.RED + "Bag full! Sell your haul with /sell");
            Sfx.play(plugin, player, "collectFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            return CollectionResult.fail("bag full");
        }

        // luck bonus
        int luck = data.getGarbageLuck();
        double tierMult = collectorValueMultiplier(data);
        double moneyMult = plugin.getConfig().getBoolean("luck.enabled", true)
                ? 1 + luck * plugin.getConfig().getDouble("luck.moneyBonusPerPoint", 0.02)
                : 1.0;

        int value = Math.max(1, (int) Math.round(garbage.baseValue * garbage.rarityMult * tierMult * moneyMult));
        data.addMoney(value);
        data.addTotalEarned(value);
        data.addGarbage(garbage.typeName, 1);
        data.addCollectionGarbage(garbage.typeName, 1);
        data.addCollected(1);

        int xp = Math.max(1, (int) Math.round(garbage.rarityXp * (1 + luck * plugin.getConfig().getDouble("luck.xpBonusPerPoint", 0.03))));
        player.giveExpLevels(xp);

        boolean luckGained = false;
        if (plugin.getConfig().getBoolean("luck.enabled", true)
                && random.nextDouble() < plugin.getConfig().getDouble("luck.gainOnCollectChance", 0.05)
                && luck < plugin.getConfig().getInt("luck.maxLuck", 100)) {
            data.addGarbageLuck(plugin.getConfig().getInt("luck.gainAmount", 1));
            luckGained = true;
        }

        garbage.remove();
        garbages.remove(garbage.id);
        collectEffect(garbage.location, garbage);

        StringBuilder msg = new StringBuilder();
        msg.append(colorize("+" + value + "$ " + garbage.typeName, garbage.colorHex));
        msg.append(ChatColor.GRAY).append("  |  ").append(ChatColor.GREEN).append("Collected: ").append(data.getCollected())
                .append('/').append(collectorBagCapacity(data));
        msg.append(ChatColor.GRAY).append(" | ").append(ChatColor.YELLOW).append("Luck: ").append(luckGained ? data.getGarbageLuck() + " (+1)" : Integer.toString(data.getGarbageLuck()));
        player.sendActionBar(msg.toString());
        if (luckGained) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Your Garbage Luck grew! +1 (now " + data.getGarbageLuck() + ")");
            Sfx.play(plugin, player, "luckUp", Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.6f);
        }

        plugin.getStatusBarManager().updateForPlayer(player, data);
        plugin.getScoreboardManager().updateForPlayer(player, data);
        return new CollectionResult(true, value, garbage.typeName, value, luckGained, msg.toString());
    }

    @Override
    public void handleMobKilled(Player killer, ActiveGarbage garbage) {
        garbages.remove(garbage.id);
        if (killer == null) {
            bigBurst(garbage.location, null);
            return;
        }

        PlayerData kdata = plugin.getPlayerService().getPlayerData(killer.getUniqueId());
        int tier = garbage.getMobTier();
        String mobName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("mob.name", "Trash Monster")));
        kdata.addMobKill(mobName, tier);

        // spawn the garbage to collect, scaled by mob tier value
        spawnItem(ground(garbage.location), garbage.typeName, mobValueMult(tier));

        // bonus loot scaled by luck
        int luck = kdata.getGarbageLuck();
        double luckBonus = plugin.getConfig().getDouble("mob.luckDropChanceBonusPerPoint", 0.01);
        boolean anyDrop = false;
        if (plugin.getConfig().isConfigurationSection("mob.drops")) {
            for (String key : plugin.getConfig().getConfigurationSection("mob.drops").getKeys(false)) {
                String p = "mob.drops." + key;
                double chance = plugin.getConfig().getDouble(p + ".chance", 0);
                if (random.nextDouble() < Math.min(1.0, chance + luck * luckBonus)) {
                    Material mat = safeMaterial(plugin.getConfig().getString(p + ".material", "BONE"), Material.BONE);
                    int[] minmax = parseAmount(plugin.getConfig().getString(p + ".amount", "1-1"));
                    int amount = minmax[1] <= minmax[0] ? minmax[0] : minmax[0] + random.nextInt(minmax[1] - minmax[0] + 1);
                    garbage.location.getWorld().dropItem(garbage.location.clone().add(0, 0.5, 0), new ItemStack(mat, Math.max(1, amount)));
                    anyDrop = true;
                }
            }
        }

        bigBurst(garbage.location, garbage.rarityName);
        Sfx.play(plugin, garbage.location, "mobDeath", Sound.ENTITY_ZOMBIE_DEATH, 0.8f, 0.9f);
        Sfx.play(plugin, killer, "mobDeath", Sound.ENTITY_ZOMBIE_DEATH, 0.8f, 0.9f);
        if (anyDrop) Sfx.play(plugin, garbage.location, "mobLoot", Sound.ENTITY_ITEM_PICKUP, 0.4f, 1.3f);
        killer.sendActionBar(ChatColor.RED + "Trash monster slain - loot dropped!");
        plugin.getStatusBarManager().updateForPlayer(killer, kdata);
        plugin.getScoreboardManager().updateForPlayer(killer, kdata);
    }

    private int[] parseAmount(String s) {
        try {
            String[] parts = s.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;
            return new int[] { Math.max(0, min), Math.max(1, max) };
        } catch (Exception e) {
            return new int[] { 1, 1 };
        }
    }

    // -------------------------------------------------------------
    // Periodic tasks
    // -------------------------------------------------------------
    @Override
    public void updateGlow() {
        double lookRange = 6.0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead() || p.isSleeping()) continue;
            var eye = p.getEyeLocation();
            var dir = eye.getDirection();
            for (ActiveGarbage g : garbages.values()) {
                Entity target = g.raycastTarget();
                if (target == null || !target.isValid()) continue;
                if (target.getLocation().getWorld() != p.getWorld()) {
                    g.setGlowing(false);
                    continue;
                }
                boolean hit = target.getLocation().distanceSquared(p.getLocation()) <= lookRange * lookRange
                        && (target.getBoundingBox().rayTrace(eye.toVector(), dir, lookRange) != null);
                g.setGlowing(hit);
            }
        }
    }

    @Override
    public void despawnExpired() {
        int ttl = plugin.getConfig().getInt("garbage.ttlSeconds", 120);
        if (ttl <= 0) return;
        long expire = ttl * 1000L;
        long now = System.currentTimeMillis();
        for (ActiveGarbage g : new ArrayList<>(garbages.values())) {
            if (now - g.spawnedAt > expire) {
                garbages.remove(g.id);
                g.remove();
            }
        }
    }

    // -------------------------------------------------------------
    // Effects
    // -------------------------------------------------------------
    private void spawnEffect(Location loc, Rarity r, boolean mob) {
        if (loc == null || loc.getWorld() == null) return;
        if (plugin.getConfig().getBoolean("effects.particles", true)) {
            Color c = parseColor(r.colorHex);
            int count = Math.max(4, r.weight > 20 ? 6 : 12);
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.7, 0), count, 0.3, 0.3, 0.3, new Particle.DustOptions(c, r.weight <= 20 ? 2.0f : 1.2f));
        }
        Sfx.play(plugin, loc, "spawn", Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.4f, 1.3f);
    }

    private void collectEffect(Location loc, ActiveGarbage g) {
        if (loc == null || loc.getWorld() == null) return;
        if (plugin.getConfig().getBoolean("effects.particles", true)) {
            Color c = parseColor(g.colorHex);
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.8, 0), 14, 0.3, 0.3, 0.3, new Particle.DustOptions(c, 1.5f));
        }
        Sfx.play(plugin, loc, "collect", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
        // extra sparkle for non-common rarities
        if (!"common".equalsIgnoreCase(g.rarityName)) {
            Sfx.play(plugin, loc, "collectRare", Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.6f);
        }
    }

    private void bigBurst(Location loc, String rarityName) {
        if (loc == null || loc.getWorld() == null) return;
        if (plugin.getConfig().getBoolean("effects.particles", true)) {
            loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.3, 0), 20, 0.4, 0.2, 0.4, 0.05);
        }
    }

    private Color parseColor(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            int rgb = Integer.parseInt(h, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    // hex "#RRGGBB" -> legacy "§x§R§R§G§G§B§B" section codes
    private String legacyHex(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() != 6) return "";
        StringBuilder sb = new StringBuilder("§x");
        for (char c : h.toCharArray()) sb.append('§').append(c);
        return sb.toString();
    }

    private String colorize(String text, String hex) {
        return legacyHex(hex) + text + ChatColor.RESET;
    }
}