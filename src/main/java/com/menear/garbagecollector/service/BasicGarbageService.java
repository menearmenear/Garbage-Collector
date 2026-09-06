package com.menear.garbagecollector.service;

import com.menear.garbagecollector.ActiveGarbage;
import com.menear.garbagecollector.CollectionResult;
import com.menear.garbagecollector.GarbageCollectorPlugin;
import com.menear.garbagecollector.PlayerData;
import com.menear.garbagecollector.Sfx;
import com.menear.garbagecollector.Stats;
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
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final Map<UUID, Long> collectCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> magnetCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Hold> holds = new ConcurrentHashMap<>();
    private final List<Rarity> rarities = new ArrayList<>();
    private volatile long toxicUntil;
    private volatile long goldenUntil;

    public static final class Hold {
        final ActiveGarbage g;
        long lastEvent;
        long progress;
        Hold(ActiveGarbage g, long now) {
            this.g = g;
            this.lastEvent = now;
            this.progress = 0;
        }
    }

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
    public void reload() {
        reloadRarities();
        plugin.getLogger().info("Garbage service reloaded (rarities re-read from config).");
    }

    @Override
    public void start() {
        reloadRarities();
        int interval = Math.max(1, plugin.getConfig().getInt("spawn.intervalSeconds", 10));
        Bukkit.getScheduler().runTaskTimer(plugin, this::spawnTick, 60L, interval * 20L);
        int glowTick = 4;
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateGlow, 20L, glowTick);
        Bukkit.getScheduler().runTaskTimer(plugin, this::despawnExpired, 60L, 40L);
        if (plugin.getConfig().getBoolean("shop.magnet.enabled", true)) {
            Bukkit.getScheduler().runTaskTimer(plugin, this::magnetTick, 20L, 10L);
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateHoldBars, 20L, 4L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::zoneTick, 30L, 20L);
    }

    // -------------------------------------------------------------
    // World event flags
    // -------------------------------------------------------------
    @Override
    public void setToxicActive(long until) { this.toxicUntil = until; }

    @Override
    public boolean isToxicActive() { return System.currentTimeMillis() < toxicUntil; }

    @Override
    public void setGoldenActive(long until) { this.goldenUntil = until; }

    @Override
    public boolean isGoldenActive() { return System.currentTimeMillis() < goldenUntil; }

    // Danger-zone potion effects applied while a player's zone is active.
    private void zoneTick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
            String zone = plugin.getZoneService().activeZone(data);
            if (!plugin.getZoneService().danger(zone)) continue;
            for (PotionEffectType type : plugin.getZoneService().effects(zone)) {
                if (type == null) continue;
                var current = p.getPotionEffect(type);
                if (current == null || current.getDuration() < 40) {
                    p.addPotionEffect(new PotionEffect(type, 100, 0, false, false, true));
                }
            }
        }
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

        Player target = null;
        Location loc;
        if (nearPlayer) {
            target = randomPlayer();
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

        String zoneId = target != null
                ? plugin.getZoneService().activeZone(plugin.getPlayerService().getPlayerData(target.getUniqueId()))
                : plugin.getZoneService().defaultZone();

        double mobChance = plugin.getConfig().getDouble("spawn.mobChance", 15);
        if (mobChance > 0 && random.nextDouble() * 100 < mobChance) {
            boolean boss = random.nextDouble() * 100 < plugin.getConfig().getDouble("spawn.miniBossChance", 0.5);
            spawnMob(ground(loc), zoneId, boss);
        } else {
            spawnItem(ground(loc), null, 1.0, zoneId);
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

    private boolean isType(String type) {
        return type != null && plugin.getConfig().isConfigurationSection("garbage.types." + type);
    }

    private String randomTypeName(String zoneId) {
        List<String> zoneTypes = plugin.getZoneService().exists(zoneId) ? plugin.getZoneService().types(zoneId) : new ArrayList<>();
        List<String> types = zoneTypes.isEmpty()
                ? new ArrayList<>(plugin.getConfig().getConfigurationSection("garbage.types").getKeys(false))
                : zoneTypes;
        return types.isEmpty() ? "paper" : types.get(random.nextInt(types.size()));
    }

    private Rarity rollRarity(String zoneId) {
        if (plugin.getZoneService().exists(zoneId)) {
            int total = 0;
            for (Rarity r : rarities) {
                double w = plugin.getZoneService().rarityWeight(zoneId, r.name);
                if (w > 0) total += (int) w;
            }
            if (total > 0) {
                int roll = random.nextInt(total);
                for (Rarity r : rarities) {
                    double w = plugin.getZoneService().rarityWeight(zoneId, r.name);
                    if (w <= 0) continue;
                    roll -= (int) w;
                    if (roll < 0) return r;
                }
            }
        }
        return rollGlobalRarity();
    }

    private Rarity rollGlobalRarity() {
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
        spawnItem(loc, forcedType, 1.0, plugin.getZoneService().defaultZone());
    }

    private void spawnItem(Location loc, String forcedType, double valueMult) {
        spawnItem(loc, forcedType, valueMult, plugin.getZoneService().defaultZone());
    }

    private void spawnItem(Location loc, String forcedType, double valueMult, String zoneId) {
        if (loc == null || loc.getWorld() == null) return;
        if (isToxicActive() && forcedType == null && random.nextDouble() < 0.3) {
            List<String> toxic = plugin.getConfig().getStringList("events.toxic_spill.types");
            if (!toxic.isEmpty()) forcedType = toxic.get(random.nextInt(toxic.size()));
        }
        Rarity r = rollRarity(zoneId);
        String typeName = isType(forcedType) ? forcedType : randomTypeName(zoneId);
        int baseValue = plugin.getConfig().getInt("garbage.types." + typeName + ".value", 1);
        Material mat = safeMaterial(plugin.getConfig().getString("garbage.types." + typeName + ".material", "PAPER"), Material.PAPER);

        UUID id = UUID.randomUUID();
        ActiveGarbage g = new ActiveGarbage(id, typeName, baseValue, r.name, r.xp, r.multiplier * valueMult,
                r.colorHex, r.glow, loc, false);
        g.setZoneId(zoneId);

        World world = loc.getWorld();
        Interaction inter = world.spawn(loc, Interaction.class);
        inter.setInteractionWidth(0.6f);
        inter.setInteractionHeight(0.6f);
        inter.setResponsive(true);
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
    private void spawnMob(Location loc, String zoneId, boolean forceMiniBoss) {
        if (loc == null || loc.getWorld() == null) return;
        String mobId = pickMobId(zoneId, forceMiniBoss);
        if (mobId == null) {
            spawnItem(loc, null, 1.0, zoneId);
            return;
        }
        spawnMobById(loc, mobId, zoneId, forceMiniBoss);
    }

    @Override
    public void spawnMiniBoss(Player target) {
        if (target == null || !target.isOnline()) return;
        PlayerData data = plugin.getPlayerService().getPlayerData(target.getUniqueId());
        spawnMob(ground(target.getLocation()), plugin.getZoneService().activeZone(data), true);
    }

    private String pickMobId(String zoneId, boolean forceMiniBoss) {
        List<String> zoneMonsters = plugin.getZoneService().exists(zoneId) ? plugin.getZoneService().monsters(zoneId) : new ArrayList<>();
        List<String> defined = new ArrayList<>(plugin.getConfig().getConfigurationSection("mob.types").getKeys(false));
        List<String> pool = zoneMonsters.isEmpty() ? defined : zoneMonsters;
        pool.retainAll(defined);
        if (pool.isEmpty()) return null;
        if (forceMiniBoss) {
            for (String id : pool) {
                if (plugin.getConfig().getBoolean("mob.types." + id + ".miniBoss", false)) return id;
            }
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private void spawnMobById(Location loc, String mobId, String zoneId, boolean forceMiniBoss) {
        if (loc == null || loc.getWorld() == null) return;
        World world = loc.getWorld();
        String defPath = "mob.types." + mobId;
        boolean defined = plugin.getConfig().isConfigurationSection(defPath);

        Rarity r = rollRarity(zoneId);
        String typeName = randomTypeName(zoneId);
        int baseValue = plugin.getConfig().getInt("garbage.types." + typeName + ".value", 1);
        Material mat = safeMaterial(plugin.getConfig().getString("garbage.types." + typeName + ".material", "PAPER"), Material.PAPER);

        String rawName = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString(defined ? defPath + ".displayName" : "mob.name", "&cTrash Monster"));
        int maxTiers = mobTierCount();
        int tier = forceMiniBoss ? maxTiers : randomMobTier();
        boolean miniBoss = forceMiniBoss || plugin.getConfig().getBoolean(defPath + ".miniBoss", false);

        double hpMult = (defined ? plugin.getConfig().getDouble(defPath + ".hpMult", 1.0) : 1.0) * mobHealthMult(tier);
        double dropValueMult = (defined ? plugin.getConfig().getDouble(defPath + ".valueMult", 1.0) : 1.0) * mobValueMult(tier);
        int mobXp = defined ? plugin.getConfig().getInt(defPath + ".xp", 10) : 10;
        double scale = defined ? plugin.getConfig().getDouble(defPath + ".scale", 1.0) : 1.0;
        double speed = defined ? plugin.getConfig().getDouble(defPath + ".speed", 1.0) : 1.0;
        String typeNameCfg = defined ? plugin.getConfig().getString(defPath + ".entityType", "ZOMBIE")
                : plugin.getConfig().getString("mob.type", "ZOMBIE");

        EntityType et;
        try { et = EntityType.valueOf(typeNameCfg.toUpperCase()); } catch (Exception e) { et = EntityType.ZOMBIE; }
        if (et == null || !et.isSpawnable() || et == EntityType.PLAYER) et = EntityType.ZOMBIE;

        LivingEntity mob = (LivingEntity) world.spawnEntity(loc, et);
        mob.setPersistent(true);
        mob.setCanPickupItems(false);
        mob.getEquipment().setHelmet(new ItemStack(mat));
        var scaleAttr = mob.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(Math.max(0.05, scale));
        var speedAttr = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(Math.max(0.05, 0.23 * speed));

        double maxHealth = Math.max(1.0, 20.0 * hpMult);
        mob.setMaxHealth(maxHealth);
        mob.setHealth(maxHealth);

        BossBar bar = null;
        if (miniBoss) {
            BarColor color = BarColor.RED;
            try {
                color = BarColor.valueOf(plugin.getConfig().getString(defPath + ".bossBarColor", "RED").toUpperCase());
            } catch (Exception ignored) { }
            bar = Bukkit.createBossBar(rawName + ChatColor.YELLOW + " " + romanNumeral(tier), color, BarStyle.SOLID);
            bar.setProgress(1.0);
            for (Player near : world.getNearbyPlayers(loc, 32)) bar.addPlayer(near);
            Sfx.play(plugin, loc, "bossBar", Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.7f);
        }

        UUID id = UUID.randomUUID();
        ActiveGarbage g = new ActiveGarbage(id, typeName, baseValue, r.name, r.xp, r.multiplier,
                r.colorHex, r.glow, loc, true);
        g.setMob(mob, tier);
        g.setMobInfo(mobId, rawName, miniBoss);
        g.setMobValueMult(dropValueMult);
        g.setMobXp(mobXp);
        g.setZoneId(zoneId);
        if (bar != null) g.setBossBar(bar);
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
        String baseName = g.getMonsterName() != null
                ? g.getMonsterName()
                : ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("mob.name", "&cTrash Monster"));
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

    @Override
    public void spawnZoneBurst(Player forPlayer, Location location, int amount) {
        if (location == null || location.getWorld() == null) return;
        String zoneId = forPlayer != null && forPlayer.isOnline()
                ? plugin.getZoneService().activeZone(plugin.getPlayerService().getPlayerData(forPlayer.getUniqueId()))
                : plugin.getZoneService().defaultZone();
        double spreadRadius = Math.max(8, Math.min(60, plugin.getConfig().getInt("spawn.burstSpread", 20)));
        for (int i = 0; i < amount; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double d = Math.sqrt(random.nextDouble()) * spreadRadius;
            Location loc = location.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
            spawnItem(ground(loc), null, 1.0, zoneId);
        }
    }

    public double collectorValueMultiplier(PlayerData data) {
        return Stats.tierValueMultiplier(plugin, data);
    }

    public int collectorBagCapacity(PlayerData data) {
        return Stats.capacity(plugin, data);
    }

    @Override
    public CollectionResult collect(Player player, ActiveGarbage garbage) {
        return doCollect(player, garbage, true, true);
    }

    /**
     * Hold-to-collect entry point. Repeated interactions with the same
     * garbage accumulate "hold" time (real elapsed time between events).
     * When the required hold duration is reached the garbage is collected.
     */
    @Override
    public void interact(Player player, ActiveGarbage garbage) {
        if (player == null || !player.isOnline() || garbage == null) return;
        PlayerData data = plugin.getPlayerService().getPlayerData(player.getUniqueId());

        // fail fast so the player never wastes a full hold on a useless attempt
        if (!hasTool(player)) {
            player.sendActionBar(ChatColor.RED + "You need the "
                    + plugin.getConfig().getString("tool.name", "&6Trash Grabber").replace("&", "\u00A7") + "!");
            Sfx.play(plugin, player, "collectFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            return;
        }
        if (!garbage.isMob() && !Stats.hasPowerFor(plugin, data, garbage.typeName)) {
            int need = plugin.getConfig().getInt("garbage.types." + garbage.typeName + ".requiresPower", 0);
            player.sendActionBar(ChatColor.RED + "Need Power " + need + " to lift this! Buy it in /shop");
            Sfx.play(plugin, player, "collectFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            return;
        }
        if (data.getCollected() >= Stats.capacity(plugin, data)) {
            player.sendActionBar(ChatColor.RED + "Bag full! Sell your haul with /sell");
            Sfx.play(plugin, player, "collectFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            return;
        }
        long manualCd = Stats.effectiveCooldownMillis(plugin, data);
        if (manualCd > 0) {
            Long last = collectCooldowns.get(player.getUniqueId());
            if (last != null) {
                long elapsed = System.currentTimeMillis() - last;
                if (elapsed < manualCd) {
                    int leftSec = (int) Math.ceil((manualCd - elapsed) / 1000.0);
                    player.sendActionBar(ChatColor.GRAY + "Cooldown... " + ChatColor.YELLOW + leftSec + "s");
                    return;
                }
            }
        }

        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Hold hold = holds.get(uid);
        if (hold == null || hold.g != garbage) {
            holds.put(uid, new Hold(garbage, now));
            hold = holds.get(uid);
        }
        long gap = now - hold.lastEvent;
        long maxGap = plugin.getConfig().getLong("collect.holdMaxGapMillis", 1500);
        if (gap > maxGap) {
            hold.progress = 0;
        }
        hold.lastEvent = now;
        if (gap <= maxGap) {
            hold.progress += gap;
        }

        long required = requiredHoldMillis(data);
        if (hold.progress >= required) {
            holds.remove(uid);
            doCollect(player, garbage, true, true);
            return;
        }
        sendHoldBar(player, data, hold, required);
    }

    private long requiredHoldMillis(PlayerData data) {
        return Stats.requiredHoldMillis(plugin, data);
    }

    private long effectiveCollectCooldown(PlayerData data) {
        return Stats.effectiveCooldownMillis(plugin, data);
    }

    private void sendHoldBar(Player player, PlayerData data, Hold hold, long required) {
        long progress = Math.min(required, hold.progress);
        int pct = (int) Math.round(progress * 100.0 / required);
        int filled = (int) Math.round(pct / 10.0);
        int remainSec = (int) Math.ceil(Math.max(0, required - progress) / 1000.0);
        String bar = ChatColor.GREEN + "█".repeat(filled) + ChatColor.GRAY + "░".repeat(10 - filled);
        player.sendActionBar(ChatColor.GOLD + "Collecting... " + bar + ChatColor.GRAY + " " + pct + "%  "
                + ChatColor.YELLOW + remainSec + "s");
    }

    private void updateHoldBars() {
        long now = System.currentTimeMillis();
        long maxGap = plugin.getConfig().getLong("collect.holdMaxGapMillis", 1500);
        for (Map.Entry<UUID, Hold> e : new ArrayList<>(holds.entrySet())) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null || !p.isOnline()) {
                holds.remove(e.getKey());
                continue;
            }
            Hold hold = e.getValue();
            if (now - hold.lastEvent > maxGap) {
                holds.remove(e.getKey());
                continue;
            }
            PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
            sendHoldBar(p, data, hold, requiredHoldMillis(data));
        }
    }

    private CollectionResult doCollect(Player player, ActiveGarbage garbage, boolean requireTool, boolean checkManualCooldown) {
        if (player == null || !player.isOnline() || garbage == null) return CollectionResult.fail("");
        PlayerData data = plugin.getPlayerService().getPlayerData(player.getUniqueId());

        if (requireTool && !hasTool(player)) {
            String name = plugin.getConfig().getString("tool.name", "&6Trash Grabber").replace("&", "\u00A7");
            player.sendActionBar(ChatColor.RED + "You need the " + name + "!");
            Sfx.play(plugin, player, "collectFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            return CollectionResult.fail("no tool");
        }
        if (!garbage.isMob() && !Stats.hasPowerFor(plugin, data, garbage.typeName)) {
            int need = plugin.getConfig().getInt("garbage.types." + garbage.typeName + ".requiresPower", 0);
            player.sendActionBar(ChatColor.RED + "Need Power " + need + " to lift this! Buy it in /shop");
            Sfx.play(plugin, player, "collectFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            return CollectionResult.fail("no power");
        }
        if (data.getCollected() >= Stats.capacity(plugin, data)) {
            player.sendActionBar(ChatColor.RED + "Bag full! Sell your haul with /sell");
            Sfx.play(plugin, player, "collectFail", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            return CollectionResult.fail("bag full");
        }
        if (checkManualCooldown) {
            long cooldownMs = Stats.effectiveCooldownMillis(plugin, data);
            if (cooldownMs > 0) {
                Long last = collectCooldowns.get(player.getUniqueId());
                if (last != null && System.currentTimeMillis() - last < cooldownMs) {
                    return CollectionResult.fail("cooldown");
                }
            }
        }

        // luck bonus
        int luck = data.getGarbageLuck();
        double tierMult = collectorValueMultiplier(data);
        double moneyMult = plugin.getConfig().getBoolean("luck.enabled", true)
                ? 1 + luck * plugin.getConfig().getDouble("luck.moneyBonusPerPoint", 0.02)
                : 1.0;
        double goldenMult = isGoldenActive() ? 2.0 : plugin.getPowerupService().valueMult(data);

        int value = Math.max(1, (int) Math.round(garbage.baseValue * garbage.rarityMult * tierMult * moneyMult * goldenMult));
        data.addGarbageValue(garbage.typeName, value);
        data.addGarbage(garbage.typeName, 1);
        data.addCollectionGarbage(garbage.typeName, 1);
        data.addCollected(1);
        if (checkManualCooldown) {
            collectCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }

        int xp = Math.max(1, (int) Math.round(garbage.rarityXp * (1 + luck * plugin.getConfig().getDouble("luck.xpBonusPerPoint", 0.03))));
        player.giveExpLevels(xp);
        data.addCollectorXp(xp);

        boolean luckGained = false;
        if (plugin.getConfig().getBoolean("luck.enabled", true)
                && random.nextDouble() < plugin.getConfig().getDouble("luck.gainOnCollectChance", 0.05)
                && luck < plugin.getConfig().getInt("luck.maxLuck", 100)) {
            data.addGarbageLuck(plugin.getConfig().getInt("luck.gainAmount", 1));
            luckGained = true;
        }

        // Mythic find: broadcast + permanent luck reward + mission/achievement triggers
        boolean mythic = Stats.isMythic(plugin, garbage.rarityName);
        if (mythic) {
            data.addMythicFound(1);
            int reward = plugin.getConfig().getInt("garbage.mythicLuckReward", 1);
            if (reward > 0) data.addGarbageLuck(reward);
            plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + player.getName()
                    + " found a MYTHIC " + ChatColor.WHITE + garbage.typeName.replace("_", " ")
                    + ChatColor.LIGHT_PURPLE + "!");
            Sfx.play(plugin, player, "mythic", Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f);
        }

        // daily contracts + achievements
        plugin.getMissionService().progress(data, "collect", garbage.typeName);
        if (!"common".equalsIgnoreCase(garbage.rarityName)) {
            plugin.getMissionService().progress(data, "findRare", garbage.rarityName);
        }
        plugin.getAchievementService().checkAll(player, data);

        garbage.remove();
        garbages.remove(garbage.id);
        collectEffect(garbage.location, garbage);

        StringBuilder msg = new StringBuilder();
        msg.append(colorize("+" + value + "$ " + garbage.typeName, garbage.colorHex));
        msg.append(ChatColor.GRAY).append("  |  ").append(ChatColor.GREEN).append("Bag: ").append(data.getCollected())
                .append('/').append(Stats.capacity(plugin, data));
        msg.append(ChatColor.GRAY).append(" | ").append(ChatColor.YELLOW).append("Luck: ").append(luckGained ? data.getGarbageLuck() + " (+1)" : Integer.toString(data.getGarbageLuck()));
        if (mythic) msg.append(ChatColor.LIGHT_PURPLE).append(" | MYTHIC!");
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
        if (garbage == null) return;
        garbages.remove(garbage.id);
        Player reward = killer != null ? killer : null;
        if (reward == null && garbage.isMiniBoss() && garbage.getTopDamager() != null) {
            Player top = Bukkit.getPlayer(garbage.getTopDamager());
            if (top != null && top.isOnline()) reward = top;
        }
        if (reward == null) {
            bigBurst(garbage.location, garbage.rarityName);
            garbage.remove();
            return;
        }

        PlayerData kdata = plugin.getPlayerService().getPlayerData(reward.getUniqueId());
        int tier = garbage.getMobTier();
        String mobName = ChatColor.stripColor(
                garbage.getMonsterName() != null ? garbage.getMonsterName()
                : ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("mob.name", "&cTrash Monster")));
        kdata.addMobKill(mobName, tier);
        if (garbage.getMobId() != null) kdata.addMobKillById(garbage.getMobId());
        kdata.addCollectorXp(Math.max(1, garbage.getMobXp() * tier));
        plugin.getMissionService().progress(kdata, "kill", garbage.getMobId() != null ? garbage.getMobId() : "any");

        // spawn the garbage to collect, scaled by mob tier/type value
        spawnItem(ground(garbage.location), garbage.typeName, garbage.getMobValueMult(), garbage.getZoneId());

        // bonus loot scaled by luck (mob.types drop override if present)
        int luck = kdata.getGarbageLuck();
        double luckBonus = plugin.getConfig().getDouble("mob.luckDropChanceBonusPerPoint", 0.01);
        String dropsPath = garbage.getMobId() != null && plugin.getConfig().isConfigurationSection("mob.types." + garbage.getMobId() + ".drops")
                ? "mob.types." + garbage.getMobId() + ".drops" : "mob.drops";
        boolean anyDrop = false;
        if (plugin.getConfig().isConfigurationSection(dropsPath)) {
            for (String key : plugin.getConfig().getConfigurationSection(dropsPath).getKeys(false)) {
                String p = dropsPath + "." + key;
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

        // mini-boss bonus: luck token (also announces the kill)
        if (garbage.isMiniBoss()) {
            boolean token = random.nextDouble() < plugin.getConfig().getDouble("mob.miniBossTokenChance", 0.5);
            if (token) dropLuckToken(garbage.location);
            plugin.getServer().broadcastMessage(ChatColor.GOLD + reward.getName() + ChatColor.YELLOW
                    + " has slain " + ChatColor.GOLD + mobName + "!" + (token ? ChatColor.LIGHT_PURPLE + " (+1 Luck Token)" : ""));
        }

        bigBurst(garbage.location, garbage.rarityName);
        Sfx.play(plugin, garbage.location, "mobDeath", Sound.ENTITY_ZOMBIE_DEATH, 0.8f, 0.9f);
        Sfx.play(plugin, reward, "mobDeath", Sound.ENTITY_ZOMBIE_DEATH, 0.8f, 0.9f);
        if (anyDrop) Sfx.play(plugin, garbage.location, "mobLoot", Sound.ENTITY_ITEM_PICKUP, 0.4f, 1.3f);
        reward.sendActionBar(ChatColor.RED + "Trash monster slain - loot dropped!");
        garbage.remove();
        plugin.getAchievementService().checkAll(reward, kdata);
        plugin.getStatusBarManager().updateForPlayer(reward, kdata);
        plugin.getScoreboardManager().updateForPlayer(reward, kdata);
    }

    private void dropLuckToken(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        ItemStack token = new ItemStack(Material.NAUTILUS_SHELL);
        ItemMeta meta = token.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Luck Token");
        meta.setLore(List.of(ChatColor.GRAY + "Right-click to permanently gain +1 Garbage Luck!"));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "luck-token"), PersistentDataType.BYTE, (byte) 1);
        token.setItemMeta(meta);
        loc.getWorld().dropItemNaturally(loc.clone().add(0, 0.5, 0), token);
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
    // Magnet (auto-collect aura)
    // -------------------------------------------------------------
    private void magnetTick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead() || !p.isOnline()) continue;
            PlayerData data = plugin.getPlayerService().getPlayerData(p.getUniqueId());
            int level = Stats.magnet(plugin, data);
            if (level <= 0) continue;
            String lvlPath = "shop.magnet.levels." + level;
            double radius = plugin.getConfig().getDouble(lvlPath + ".radius", 3)
                    * plugin.getPowerupService().radiusMult(data);
            int intervalMs = Math.max(1, plugin.getConfig().getInt(lvlPath + ".cooldownSeconds", 60)) * 1000;

            Long last = magnetCooldowns.get(p.getUniqueId());
            if (last != null && System.currentTimeMillis() - last < intervalMs) continue;

            ActiveGarbage nearest = nearestItem(p, radius);
            if (nearest == null) continue;

            CollectionResult result = doCollect(p, nearest, false, false);
            if (result == null || !result.success) continue;
            magnetCooldowns.put(p.getUniqueId(), System.currentTimeMillis());
            Sfx.play(plugin, p, "magnet", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.4f);
            if (plugin.getConfig().getBoolean("effects.particles", true)) {
                p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().clone().add(0, 1, 0),
                        6, 0.3, 0.5, 0.3, 0.02);
            }
            p.sendMessage(ChatColor.LIGHT_PURPLE + "[Magnet] Auto-collected "
                    + colorize(result.garbageType, nearest.colorHex) + " (+$" + result.value + " bag)");
        }
    }

    private ActiveGarbage nearestItem(Player p, double radius) {
        ActiveGarbage best = null;
        double bestDist = Double.MAX_VALUE;
        double r2 = radius * radius;
        for (ActiveGarbage g : garbages.values()) {
            if (!g.isItem()) continue;
            Entity target = g.raycastTarget();
            if (target == null || !target.isValid()) continue;
            if (target.getLocation().getWorld() != p.getWorld()) continue;
            double d = target.getLocation().distanceSquared(p.getLocation());
            if (d <= r2 && d < bestDist) {
                best = g;
                bestDist = d;
            }
        }
        return best;
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