# Garbage Collector

A immersive Paper plugin that spawns collectible **garbage** around your world. Grab the loot, sell your haul, upgrade your collector, hunt Trash Monsters, and track everything in a Hypixel-style collection menu.

Built for **Paper 1.20.4+** (API 26.2) running on **Java 25**.

---

## Features

- **Collect garbage** — right-click anything you find with your *Trash Grabber* to collect it. Each piece has a **value** and a **rarity**:
  - `Common` (White) · `Rare` (Cyan) · `Epic` (Magenta) · `Legendary` (Gold)
  - Rarer garbage is worth much more and gives bonus XP.
- **Garbage spawns naturally** — items and mobs spawn every few seconds either near players or around world spawn (mode configurable).
- **Trash Monster mobs** — zombies wearing garbage as a helmet. They spawn with a **tier (I–V)** that scales their health and loot, show a **live health bar of hearts** above their head (♥♥♥❤❤ 18/20), and drop bonus loot scaled by your Luck.
- **Scaled value** — your **Collector tier** raises how much each piece is worth and your **bag capacity**.
- **Luck stat** — every purchase/collect can raise your Luck, which boosts money, XP, mob drops, and sell bonus-loot rolls.
- **Shops & menus** (Hypixel-style, with sounds):
  - `/sell` — sell each garbage type or everything at once, with a **bonus loot roll** on every sale.
  - `/shop` — upgrade your **Collector tier**, buy **Luck**, or level up **Speed**.
  - `/collection` — two-tab menu (Garbage + Mobs) tracking every type/tier you've ever collected, with per-tier kill counts.
- **Immersive sound design** — configurable sounds for spawning, collecting, rare finds, luck gains, mob spawns/hurts/deaths, loot drops, selling, bonus loot, shop purchases/denials, and GUI clicks.
- **Live status** — always-on **action bar** shows `Bag: 12/40 | $1250`, and a side **scoreboard** shows money, collected, luck, and tier.
- **Player data persistence** — everything is saved per player to `plugins/Garbage-Collector/players/<uuid>.yml`.

---

## Installation

1. Download the latest `Garbage-Collector.jar` from the releases (or build it yourself, see below).
2. Drop it into your server's `plugins/` folder.
3. Restart the server (or use PlugMan-style reload).
4. Tweak `plugins/Garbage-Collector/config.yml` if desired, then run `/garbage reload`.

> Requires Paper 1.20.4+ and Java 25.

---

## Commands

### Player commands

| Command             | Description                                    |
|---------------------|------------------------------------------------|
| `/garbageitem`       | Gives you the *Trash Grabber* collector item.  |
| `/sell`              | Opens the sell GUI.                            |
| `/shop`              | Opens the upgrade shop.                        |
| `/collection`        | Opens your collection (Garbage + Mobs tabs).   |

### Admin commands (`garbage.admin`)

| Command                          | Description                                        |
|----------------------------------|----------------------------------------------------|
| `/garbage` / `/garbage help`     | Shows help.                                        |
| `/garbage give <player> [type] [tier] [luck]` | Spawns garbage (optionally a specific type) near a player and sets their tier/luck. |
| `/garbage spawn <amount>`        | Spawns `amount` garbage scattered randomly around you (or world spawn from console). |
| `/garbage spawnburst <amount>`   | Alias for a quick burst of `amount` garbage.       |
| `/garbage stats <player>`        | Shows a player's money, tier, luck, speed, and counts. |
| `/garbage luck <player> <amount>`| Sets a player's luck.                               |
| `/garbage reset <player>`        | Wipes a player's data (money, upgrades, collections). |
| `/garbage reload`                | Reloads `config.yml`.                               |

---

## How it works

### Rarities
Every spawned piece rolls a rarity by weight. Rarity multiplies both the display value and collection value/XP:

| Rarity       | Weight | Value multiplier | XP |
|--------------|--------|------------------|----|
| Common       | 70     | 1.0×             | 1  |
| Rare         | 20     | 3.0×             | 3  |
| Epic         | 8      | 8.0×             | 8  |
| Legendary    | 2      | 25.0×            | 20 |

### Garbage types
| Type    | Material        | Base value |
|---------|-----------------|------------|
| paper   | Paper           | $1         |
| can     | Iron Nugget     | $3         |
| bottle  | Glass Bottle    | $2         |
| sock    | Leather         | $1         |
| diamond | Diamond         | $10        |

### Mobs
Trash Monsters live on `mob.type` (default Zombie), wear a random garbage type as a helmet, and spawn at a **tier** weighted toward lower tiers:

- **Tier I–V** — every tier adds **50% health** and **50% loot value** (`mob.healthPerTier`, `mob.valuePerTier`).
- Their name tag shows the tier + live hearts, e.g. `Trash Monster III ♥♥♥♥♥♥❤❤❤❤ 13/20`.
- On death they drop the garbage to collect (value scaled by tier) plus a luck-scaled **bonus loot table** (`mob.drops`).

### Collector tiers
| Tier | Value multiplier | Bag capacity | Cost  |
|------|------------------|--------------|-------|
| I    | 1.0×             | 20           | free  |
| II   | 1.5×             | 40           | $500  |
| III  | 2.5×             | 80           | $2000 |
| IV   | 4.0×             | 150          | $8000 |

---

## Configuration

Everything is in `plugins/Garbage-Collector/config.yml`. Key sections:

- `spawn` — interval, world, radius, spawn mode (`spawn` / `players` / `mixed`), mob chance, burst spread.
- `garbage.collector` — collector tiers (value multiplier, bag capacity, cost).
- `garbage.rarities` — weights, colors, glow, XP, value multiplier.
- `garbage.types` — name, material, base value.
- `luck` — money/XP bonuses per point, gain-on-collect chance, max, upgrade cost.
- `shop.speed` — walk-speed levels and costs.
- `sell` — `alwaysBonusChance` + the `bonusLoot` table (material, chance, amount range).
- `tool` — the collector item's material/name/lore.
- `mob` — mob type/name, max tier, per-tier health/value scaling, drop table.
- `effects` — particles/sounds master toggles.
- `sounds.events` — per-event sound, volume, and pitch (e.g. `collect`, `mobDeath`, `bonusLoot`). Set `sounds.enabled: false` to mute everything.
- `scoreboard` — title + line templates (`%money%`, `%collected%`, `%luck%`, `%tier%`).

Run `/garbage reload` after making changes (interval/spawn-rate changes need a restart).

---

## Building from source

Requires Java 25 and Gradle (the project uses the Gradle wrapper).

```bash
./build.sh          # builds + deploys to server/plugins/ (dev helper)
gradle build        # or: plain Gradle build
```

The build script prints a clean-step summary and copies the jar into `server/plugins/` automatically. `start.sh` launches the bundled Paper server with the plugin pre-loaded (great for local testing).

---

## Permissions

| Permission      | Effect                       |
|-----------------|------------------------------|
| `garbage.admin` | Access to all `/garbage` admin commands. |

---

## Roadmap

- [ ] Boss-bar health for high-tier mobs
- [ ] Rarity-bar / drop-roll animations
- [ ] Claimable bonus chests
- [ ] Daily garbage quests

---

## License

MIT