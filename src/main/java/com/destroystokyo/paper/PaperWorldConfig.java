package com.destroystokyo.paper;

import java.util.Arrays;
import java.util.List;

import com.destroystokyo.paper.antixray.ChunkPacketBlockControllerAntiXray.ChunkEdgeMode;
import com.destroystokyo.paper.antixray.ChunkPacketBlockControllerAntiXray.EngineMode;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spigotmc.SpigotWorldConfig;

import static com.destroystokyo.paper.PaperConfig.log;
import static com.destroystokyo.paper.PaperConfig.logError;

public class PaperWorldConfig {

    private final String worldName;
    private final SpigotWorldConfig spigotConfig;
    private final YamlConfiguration config;
    private boolean verbose;

    public PaperWorldConfig(String worldName, SpigotWorldConfig spigotConfig) {
        this.worldName = worldName;
        this.spigotConfig = spigotConfig;
        this.config = PaperConfig.config;
        init();
    }

    public void init() {
        log("-------- World Settings For [" + worldName + "] --------");
        PaperConfig.readConfig(PaperWorldConfig.class, this);
    }

    private void set(String path, Object val) {
        config.set("world-settings.default." + path, val);
        if (config.get("world-settings." + worldName + "." + path) != null) {
            config.set("world-settings." + worldName + "." + path, val);
        }
    }

    private boolean getBoolean(String path, boolean def) {
        config.addDefault("world-settings.default." + path, def);
        return config.getBoolean("world-settings." + worldName + "." + path, config.getBoolean("world-settings.default." + path));
    }

    private double getDouble(String path, double def) {
        config.addDefault("world-settings.default." + path, def);
        return config.getDouble("world-settings." + worldName + "." + path, config.getDouble("world-settings.default." + path));
    }

    private int getInt(String path, int def) {
        config.addDefault("world-settings.default." + path, def);
        return config.getInt("world-settings." + worldName + "." + path, config.getInt("world-settings.default." + path));
    }

    private float getFloat(String path, float def) {
        // TODO: Figure out why getFloat() always returns the default value.
        return (float) getDouble(path, (double) def);
    }

    private <T> List<T> getList(String path, List<T> def) {
        config.addDefault("world-settings.default." + path, def);
        return (List<T>) config.getList("world-settings." + worldName + "." + path, config.getList("world-settings.default." + path));
    }

    private String getString(String path, String def) {
        config.addDefault("world-settings.default." + path, def);
        return config.getString("world-settings." + worldName + "." + path, config.getString("world-settings.default." + path));
    }

    public int cactusMaxHeight;
    public int reedMaxHeight;
    private void blockGrowthHeight() {
        cactusMaxHeight = getInt("max-growth-height.cactus", 3);
        reedMaxHeight = getInt("max-growth-height.reeds", 3);
        log("Max height for cactus growth " + cactusMaxHeight + ". Max height for reed growth " + reedMaxHeight);

    }

    public double babyZombieMovementSpeed;
    private void babyZombieMovementSpeed() {
        babyZombieMovementSpeed = getDouble("baby-zombie-movement-speed", 0.5D); // Player moves at 0.1F, for reference
        log("Baby zombies will move at the speed of " + babyZombieMovementSpeed);
    }

    public int fishingMinTicks;
    public int fishingMaxTicks;
    private void fishingTickRange() {
        fishingMinTicks = getInt("fishing-time-range.MinimumTicks", 100);
        fishingMaxTicks = getInt("fishing-time-range.MaximumTicks", 600);
        log("Fishing time ranges are between " + fishingMinTicks +" and " + fishingMaxTicks + " ticks");
    }

    public boolean nerfedMobsShouldJump;
    private void nerfedMobsShouldJump() {
        nerfedMobsShouldJump = getBoolean("spawner-nerfed-mobs-should-jump", false);
    }

    public int softDespawnDistance;
    public int hardDespawnDistance;
    private void despawnDistances() {
        softDespawnDistance = getInt("despawn-ranges.soft", 32); // 32^2 = 1024, Minecraft Default
        hardDespawnDistance = getInt("despawn-ranges.hard", 128); // 128^2 = 16384, Minecraft Default

        if (softDespawnDistance > hardDespawnDistance) {
            softDespawnDistance = hardDespawnDistance;
        }

        log("Living Entity Despawn Ranges:  Soft: " + softDespawnDistance + " Hard: " + hardDespawnDistance);

        softDespawnDistance = softDespawnDistance*softDespawnDistance;
        hardDespawnDistance = hardDespawnDistance*hardDespawnDistance;
    }

    public boolean keepSpawnInMemory;
    private void keepSpawnInMemory() {
        keepSpawnInMemory = getBoolean("keep-spawn-loaded", true);
        log("Keep spawn chunk loaded: " + keepSpawnInMemory);
    }

    public int fallingBlockHeightNerf;
    public int entityTNTHeightNerf;
    private void heightNerfs() {
        fallingBlockHeightNerf = getInt("falling-block-height-nerf", 0);
        entityTNTHeightNerf = getInt("tnt-entity-height-nerf", 0);

        if (fallingBlockHeightNerf != 0) log("Falling Block Height Limit set to Y: " + fallingBlockHeightNerf);
        if (entityTNTHeightNerf != 0) log("TNT Entity Height Limit set to Y: " + entityTNTHeightNerf);
    }

    public boolean netherVoidTopDamage;
    private void netherVoidTopDamage() {
        netherVoidTopDamage = getBoolean( "nether-ceiling-void-damage", false );
        log("Top of the nether void damage: " + netherVoidTopDamage);
    }

    public boolean queueLightUpdates;
    private void queueLightUpdates() {
        queueLightUpdates = getBoolean("queue-light-updates", false);
        log("Lighting Queue enabled: " + queueLightUpdates);
        log("Warning: This feature may help reduce TPS loss from light, but comes at the cost of buggy light data");
        log("We are working to improve this feature.");
    }

    public boolean disableEndCredits;
    private void disableEndCredits() {
        disableEndCredits = getBoolean("game-mechanics.disable-end-credits", false);
        log("End credits disabled: " + disableEndCredits);
    }

    public boolean optimizeExplosions;
    private void optimizeExplosions() {
        optimizeExplosions = getBoolean("optimize-explosions", false);
        log("Optimize explosions: " + optimizeExplosions);
    }

    public boolean disableExplosionKnockback;
    private void disableExplosionKnockback(){
        disableExplosionKnockback = getBoolean("disable-explosion-knockback", false);
    }

    public boolean disableThunder;
    private void disableThunder() {
        disableThunder = getBoolean("disable-thunder", false);
    }

    public boolean disableIceAndSnow;
    private void disableIceAndSnow(){
        disableIceAndSnow = getBoolean("disable-ice-and-snow", false);
    }

    public int mobSpawnerTickRate;
    private void mobSpawnerTickRate() {
        mobSpawnerTickRate = getInt("mob-spawner-tick-rate", 1);
    }

    public int containerUpdateTickRate;
    private void containerUpdateTickRate() {
        containerUpdateTickRate = getInt("container-update-tick-rate", 1);
    }

    public boolean disableChestCatDetection;
    private void disableChestCatDetection() {
        disableChestCatDetection = getBoolean("game-mechanics.disable-chest-cat-detection", false);
    }

    public boolean disablePlayerCrits;
    private void disablePlayerCrits() {
        disablePlayerCrits = getBoolean("game-mechanics.disable-player-crits", false);
    }

    public boolean allChunksAreSlimeChunks;
    private void allChunksAreSlimeChunks() {
        allChunksAreSlimeChunks = getBoolean("all-chunks-are-slime-chunks", false);
    }

    public int portalSearchRadius;
    private void portalSearchRadius() {
        portalSearchRadius = getInt("portal-search-radius", 128);
    }

    public boolean disableTeleportationSuffocationCheck;
    private void disableTeleportationSuffocationCheck() {
        disableTeleportationSuffocationCheck = getBoolean("disable-teleportation-suffocation-check", false);
    }

    public boolean nonPlayerEntitiesOnScoreboards = false;
    private void nonPlayerEntitiesOnScoreboards() {
        nonPlayerEntitiesOnScoreboards = getBoolean("allow-non-player-entities-on-scoreboards", false);
    }

    public boolean allowLeashingUndeadHorse = false;
    private void allowLeashingUndeadHorse() {
        allowLeashingUndeadHorse = getBoolean("allow-leashing-undead-horse", false);
    }

    public int nonPlayerArrowDespawnRate = -1;
    public int creativeArrowDespawnRate = -1;
    private void nonPlayerArrowDespawnRate() {
        nonPlayerArrowDespawnRate = getInt("non-player-arrow-despawn-rate", -1);
        if (nonPlayerArrowDespawnRate == -1) {
            nonPlayerArrowDespawnRate = spigotConfig.arrowDespawnRate;
        }
        creativeArrowDespawnRate = getInt("creative-arrow-despawn-rate", -1);
        if (creativeArrowDespawnRate == -1) {
            creativeArrowDespawnRate = spigotConfig.arrowDespawnRate;
        }
        log("Non Player Arrow Despawn Rate: " + nonPlayerArrowDespawnRate);
        log("Creative Arrow Despawn Rate: " + creativeArrowDespawnRate);
    }

    public double skeleHorseSpawnChance;
    private void skeleHorseSpawnChance() {
        skeleHorseSpawnChance = getDouble("skeleton-horse-thunder-spawn-chance", 0.01D);
        if (skeleHorseSpawnChance < 0) {
            skeleHorseSpawnChance = 0.01D; // Vanilla value
        }
    }
    
    public double sqrMaxThunderDistance;
    public double sqrMaxLightningImpactSoundDistance;
    public double maxLightningFlashDistance;
    private void lightningStrikeDistanceLimit() {
        sqrMaxThunderDistance = getInt("lightning-strike-distance-limit.sound", -1);
        if (sqrMaxThunderDistance > 0) {
            sqrMaxThunderDistance *= sqrMaxThunderDistance;
        }

        sqrMaxLightningImpactSoundDistance = getInt("lightning-strike-distance-limit.impact-sound", -1);
        if (sqrMaxLightningImpactSoundDistance < 0) {
            sqrMaxLightningImpactSoundDistance = 32 * 32; //Vanilla value
        } else {
            sqrMaxLightningImpactSoundDistance *= sqrMaxLightningImpactSoundDistance;
        }

        maxLightningFlashDistance = getInt("lightning-strike-distance-limit.flash", -1);
        if (maxLightningFlashDistance < 0) {
            maxLightningFlashDistance = 512; // Vanilla value
        }
    }

    public boolean firePhysicsEventForRedstone = false;
    private void firePhysicsEventForRedstone() {
        firePhysicsEventForRedstone = getBoolean("fire-physics-event-for-redstone", firePhysicsEventForRedstone);
    }

    public int fixedInhabitedTime;
    private void fixedInhabitedTime() {
        if (PaperConfig.version < 16) {
            if (!config.getBoolean("world-settings.default.use-chunk-inhabited-timer", true)) config.set("world-settings.default.fixed-chunk-inhabited-time", 0);
            if (!config.getBoolean("world-settings." + worldName + ".use-chunk-inhabited-timer", true)) config.set("world-settings." + worldName + ".fixed-chunk-inhabited-time", 0);
            set("use-chunk-inhabited-timer", null);
        }
        fixedInhabitedTime = getInt("fixed-chunk-inhabited-time", -1);
    }

    public int grassUpdateRate = 1;
    private void grassUpdateRate() {
        grassUpdateRate = Math.max(0, getInt("grass-spread-tick-rate", grassUpdateRate));
        log("Grass Spread Tick Rate: " + grassUpdateRate);
    }

    public short keepLoadedRange;
    private void keepLoadedRange() {
        keepLoadedRange = (short) (getInt("keep-spawn-loaded-range", Math.min(spigotConfig.viewDistance, 8)) * 16);
        log( "Keep Spawn Loaded Range: " + (keepLoadedRange/16));
    }

    public boolean useVanillaScoreboardColoring;
    private void useVanillaScoreboardColoring() {
        useVanillaScoreboardColoring = getBoolean("use-vanilla-world-scoreboard-name-coloring", false);
    }

    public boolean frostedIceEnabled = true;
    public int frostedIceDelayMin = 20;
    public int frostedIceDelayMax = 40;
    private void frostedIce() {
        this.frostedIceEnabled = this.getBoolean("frosted-ice.enabled", this.frostedIceEnabled);
        this.frostedIceDelayMin = this.getInt("frosted-ice.delay.min", this.frostedIceDelayMin);
        this.frostedIceDelayMax = this.getInt("frosted-ice.delay.max", this.frostedIceDelayMax);
        log("Frosted Ice: " + (this.frostedIceEnabled ? "enabled" : "disabled") + " / delay: min=" + this.frostedIceDelayMin + ", max=" + this.frostedIceDelayMax);
    }

    public boolean autoReplenishLootables;
    public boolean restrictPlayerReloot;
    public boolean changeLootTableSeedOnFill;
    public int maxLootableRefills;
    public int lootableRegenMin;
    public int lootableRegenMax;
    private void enhancedLootables() {
        autoReplenishLootables = getBoolean("lootables.auto-replenish", false);
        restrictPlayerReloot = getBoolean("lootables.restrict-player-reloot", true);
        changeLootTableSeedOnFill = getBoolean("lootables.reset-seed-on-fill", true);
        maxLootableRefills = getInt("lootables.max-refills", -1);
        lootableRegenMin = PaperConfig.getSeconds(getString("lootables.refresh-min", "12h"));
        lootableRegenMax = PaperConfig.getSeconds(getString("lootables.refresh-max", "2d"));
        if (autoReplenishLootables) {
            log("Lootables: Replenishing every " +
                PaperConfig.timeSummary(lootableRegenMin) + " to " +
                PaperConfig.timeSummary(lootableRegenMax) +
                (restrictPlayerReloot ? " (restricting reloot)" : "")
            );
        }
    }

    public boolean preventTntFromMovingInWater;
    private void preventTntFromMovingInWater() {
        if (PaperConfig.version < 13) {
            boolean oldVal = getBoolean("enable-old-tnt-cannon-behaviors", false);
            set("prevent-tnt-from-moving-in-water", oldVal);
        }
        preventTntFromMovingInWater = getBoolean("prevent-tnt-from-moving-in-water", false);
        log("Prevent TNT from moving in water: " + preventTntFromMovingInWater);
    }

    public long delayChunkUnloadsBy;
    private void delayChunkUnloadsBy() {
        delayChunkUnloadsBy = PaperConfig.getSeconds(getString("delay-chunk-unloads-by", "10s"));
        if (delayChunkUnloadsBy > 0) {
            log("Delaying chunk unloads by " + delayChunkUnloadsBy + " seconds");
            delayChunkUnloadsBy *= 1000;
        }
    }

    public boolean skipEntityTickingInChunksScheduledForUnload = true;
    private void skipEntityTickingInChunksScheduledForUnload() {
        skipEntityTickingInChunksScheduledForUnload = getBoolean("skip-entity-ticking-in-chunks-scheduled-for-unload", skipEntityTickingInChunksScheduledForUnload);
    }

    public int autoSavePeriod = -1;
    private void autoSavePeriod() {
        autoSavePeriod = getInt("auto-save-interval", -1);
        if (autoSavePeriod > 0) {
            log("Auto Save Interval: " +autoSavePeriod + " (" + (autoSavePeriod / 20) + "s)");
        } else if (autoSavePeriod < 0) {
            autoSavePeriod = MinecraftServer.getServer().autosavePeriod;
        }
    }

    public int maxAutoSaveChunksPerTick = 24;
    private void maxAutoSaveChunksPerTick() {
        maxAutoSaveChunksPerTick = getInt("max-auto-save-chunks-per-tick", 24);
    }

    public int queueSizeAutoSaveThreshold = 50;
    private void queueSizeAutoSaveThreshold() {
        queueSizeAutoSaveThreshold = getInt("save-queue-limit-for-auto-save", 50);
    }

    public boolean removeCorruptTEs = false;
    private void removeCorruptTEs() {
        removeCorruptTEs = getBoolean("remove-corrupt-tile-entities", false);
    }

    public boolean filterNBTFromSpawnEgg = true;
    private void fitlerNBTFromSpawnEgg() {
        filterNBTFromSpawnEgg = getBoolean("filter-nbt-data-from-spawn-eggs-and-related", true);
        if (!filterNBTFromSpawnEgg) {
            Bukkit.getLogger().warning("Spawn Egg and Armor Stand NBT filtering disabled, this is a potential security risk");
        }
    }

    public boolean enableTreasureMaps = true;
    public boolean treasureMapsAlreadyDiscovered = false;
    private void treasureMapsAlreadyDiscovered() {
        enableTreasureMaps = getBoolean("enable-treasure-maps", true);
        treasureMapsAlreadyDiscovered = getBoolean("treasure-maps-return-already-discovered", false);
        if (treasureMapsAlreadyDiscovered) {
            log("Treasure Maps will return already discovered locations");
        }
    }

    public boolean armorStandEntityLookups = true;
    private void armorStandEntityLookups() {
        armorStandEntityLookups = getBoolean("armor-stands-do-collision-entity-lookups", true);
    }

    public int maxCollisionsPerEntity;
    private void maxEntityCollision() {
        maxCollisionsPerEntity = getInt( "max-entity-collisions", this.spigotConfig.getInt("max-entity-collisions", 8) );
        log( "Max Entity Collisions: " + maxCollisionsPerEntity );
    }

    public boolean parrotsHangOnBetter;
    private void parrotsHangOnBetter() {
        parrotsHangOnBetter = getBoolean("parrots-are-unaffected-by-player-movement", false);
        log("Parrots are unaffected by player movement: " + parrotsHangOnBetter);
    }

    public boolean disableCreeperLingeringEffect;
    private void setDisableCreeperLingeringEffect() {
        disableCreeperLingeringEffect = getBoolean("disable-creeper-lingering-effect", false);
        log("Creeper lingering effect: " + disableCreeperLingeringEffect);
    }

    public int expMergeMaxValue;
    private void expMergeMaxValue() {
        expMergeMaxValue = getInt("experience-merge-max-value", -1);
        log("Experience Merge Max Value: " + expMergeMaxValue);
    }

    public int maxChunkSendsPerTick = 81;
    private void maxChunkSendsPerTick() {
        maxChunkSendsPerTick = getInt("max-chunk-sends-per-tick", maxChunkSendsPerTick);
        if (maxChunkSendsPerTick <= 0) {
            maxChunkSendsPerTick = 81;
        }
        log("Max Chunk Sends Per Tick: " + maxChunkSendsPerTick);
    }

    public int maxChunkGensPerTick = 10;
    private void maxChunkGensPerTick() {
        maxChunkGensPerTick = getInt("max-chunk-gens-per-tick", maxChunkGensPerTick);
        if (maxChunkGensPerTick <= 0) {
            maxChunkGensPerTick = Integer.MAX_VALUE;
            log("Max Chunk Gens Per Tick: Unlimited (NOT RECOMMENDED)");
        } else {
            log("Max Chunk Gens Per Tick: " + maxChunkGensPerTick);
        }
    }

    public double squidMaxSpawnHeight;
    private void squidMaxSpawnHeight() {
        squidMaxSpawnHeight = getDouble("squid-spawn-height.maximum", 0.0D);
    }

    public boolean cooldownHopperWhenFull = true;
    public boolean disableHopperMoveEvents = false;
    private void hopperOptimizations() {
        cooldownHopperWhenFull = getBoolean("hopper.cooldown-when-full", cooldownHopperWhenFull);
        log("Cooldown Hoppers when Full: " + (cooldownHopperWhenFull ? "enabled" : "disabled"));
        disableHopperMoveEvents = getBoolean("hopper.disable-move-event", disableHopperMoveEvents);
        log("Hopper Move Item Events: " + (disableHopperMoveEvents ? "disabled" : "enabled"));
    }

    public boolean disableSprintInterruptionOnAttack;
    private void disableSprintInterruptionOnAttack() {
        disableSprintInterruptionOnAttack = getBoolean("game-mechanics.disable-sprint-interruption-on-attack", false);
    }

    public boolean disableEnderpearlExploit = true;
    private void disableEnderpearlExploit() {
        disableEnderpearlExploit = getBoolean("game-mechanics.disable-unloaded-chunk-enderpearl-exploit", disableEnderpearlExploit);
        log("Disable Unloaded Chunk Enderpearl Exploit: " + (disableEnderpearlExploit ? "enabled" : "disabled"));
    }

    public boolean villagesLoadChunks = false;
    private void villagesLoadChunks() {
        villagesLoadChunks = getBoolean("game-mechanics.villages-load-chunks", false);
        if (villagesLoadChunks) {
            log("Villages can load chunks - Warning this can cause intense TPS loss. Strongly consider disabling this.");
        }
    }

    public int shieldBlockingDelay = 5;
    private void shieldBlockingDelay() {
        shieldBlockingDelay = getInt("game-mechanics.shield-blocking-delay", 5);
    }

    public boolean scanForLegacyEnderDragon = true;
    private void scanForLegacyEnderDragon() {
        scanForLegacyEnderDragon = getBoolean("game-mechanics.scan-for-legacy-ender-dragon", true);
    }

    public int bedSearchRadius = 1;
    private void bedSearchRadius() {
        bedSearchRadius = getInt("bed-search-radius", 1);
        if (bedSearchRadius < 1) {
            bedSearchRadius = 1;
        }
        if (bedSearchRadius > 1) {
            log("Bed Search Radius: " + bedSearchRadius);
        }
    }

    public int waterOverLavaFlowSpeed;
    private void waterOverLavaFlowSpeed() {
        waterOverLavaFlowSpeed = getInt("water-over-lava-flow-speed", 5);
        log("Water over lava flow speed: " + waterOverLavaFlowSpeed);
    }

    public enum DuplicateUUIDMode {
        SAFE_REGEN, DELETE, NOTHING, WARN
    }
    public DuplicateUUIDMode duplicateUUIDMode = DuplicateUUIDMode.SAFE_REGEN;
    public int duplicateUUIDDeleteRange = 32;
    private void repairDuplicateUUID() {
        String desiredMode = getString("duplicate-uuid-resolver", "saferegen").toLowerCase().trim();
        duplicateUUIDDeleteRange = getInt("duplicate-uuid-saferegen-delete-range", duplicateUUIDDeleteRange);
        switch (desiredMode.toLowerCase()) {
            case "regen":
            case "regenerate":
            case "saferegen":
            case "saferegenerate":
                duplicateUUIDMode = DuplicateUUIDMode.SAFE_REGEN;
                log("Duplicate UUID Resolve: Regenerate New UUID if distant (Delete likely duplicates within " + duplicateUUIDDeleteRange + " blocks)");
                break;
            case "remove":
            case "delete":
                duplicateUUIDMode = DuplicateUUIDMode.DELETE;
                log("Duplicate UUID Resolve: Delete Entity");
                break;
            case "silent":
            case "nothing":
                duplicateUUIDMode = DuplicateUUIDMode.NOTHING;
                logError("Duplicate UUID Resolve: Do Nothing (no logs) - Warning, may lose indication of bad things happening");
                break;
            case "log":
            case "warn":
                duplicateUUIDMode = DuplicateUUIDMode.WARN;
                log("Duplicate UUID Resolve: Warn (do nothing but log it happened, may be spammy)");
                break;
            default:
                duplicateUUIDMode = DuplicateUUIDMode.WARN;
                logError("Warning: Invalid duplicate-uuid-resolver config " + desiredMode + " - must be one of: regen, delete, nothing, warn");
                log("Duplicate UUID Resolve: Warn (do nothing but log it happened, may be spammy)");
                break;
        }
    }

    public boolean armorStandTick = true;
    private void armorStandTick() {
        this.armorStandTick = this.getBoolean("armor-stands-tick", this.armorStandTick);
        log("ArmorStand ticking is " + (this.armorStandTick ? "enabled" : "disabled") + " by default");
    }

    public boolean optimizeLight = true;
    private void optimizeLight() {
        this.optimizeLight = getBoolean("optimize-light", optimizeLight);
    }

    public boolean antiXray;
    public boolean asynchronous;
    public EngineMode engineMode;
    public ChunkEdgeMode chunkEdgeMode;
    public int maxChunkSectionIndex;
    public int updateRadius;
    public List<String> hiddenBlocks;
    public List<String> replacementBlocks;
    private void antiXray() {
        antiXray = getBoolean("anti-xray.enabled", false);
        asynchronous = true;
        engineMode = EngineMode.getById(getInt("anti-xray.engine-mode", EngineMode.HIDE.getId()));
        engineMode = engineMode == null ? EngineMode.HIDE : engineMode;
        chunkEdgeMode = ChunkEdgeMode.getById(getInt("anti-xray.chunk-edge-mode", ChunkEdgeMode.WAIT.getId()));
        chunkEdgeMode = chunkEdgeMode == null ? ChunkEdgeMode.DEFAULT : chunkEdgeMode;

        if (chunkEdgeMode != ChunkEdgeMode.WAIT) {
            log("Migrating anti-xray chunk edge mode to " + ChunkEdgeMode.WAIT + " (" + ChunkEdgeMode.WAIT.getId() + ")");
            chunkEdgeMode = ChunkEdgeMode.WAIT;
            set("anti-xray.chunk-edge-mode", ChunkEdgeMode.WAIT.getId());
        }

        maxChunkSectionIndex = getInt("anti-xray.max-chunk-section-index", 3);
        maxChunkSectionIndex = maxChunkSectionIndex > 15 ? 15 : maxChunkSectionIndex;
        updateRadius = getInt("anti-xray.update-radius", 2);
        hiddenBlocks = getList("anti-xray.hidden-blocks", Arrays.asList("gold_ore", "iron_ore", "coal_ore", "lapis_ore", "mossy_cobblestone", "obsidian", "chest", "diamond_ore", "redstone_ore", "lit_redstone_ore", "clay", "emerald_ore", "ender_chest"));
        replacementBlocks = getList("anti-xray.replacement-blocks", Arrays.asList("stone", "planks"));
        log("Anti-Xray: " + (antiXray ? "enabled" : "disabled") + " / Engine Mode: " + engineMode.getDescription() + " / Chunk Edge Mode: " + chunkEdgeMode.getDescription() + " / Up to " + ((maxChunkSectionIndex + 1) * 16) + " blocks / Update Radius: " + updateRadius);
    }

    public boolean preventMovingIntoUnloadedChunks = false;
    private void preventMovingIntoUnloadedChunks() {
        preventMovingIntoUnloadedChunks = getBoolean("prevent-moving-into-unloaded-chunks", false);
    }

    public boolean useEigencraftRedstone = false;
    private void useEigencraftRedstone() {
        useEigencraftRedstone = this.getBoolean("use-faster-eigencraft-redstone", false);
        if (useEigencraftRedstone) {
            log("Using Eigencraft redstone algorithm by theosib.");
        } else {
            log("Using vanilla redstone algorithm.");
        }
    }

    public boolean countAllMobsForSpawning = false;
    private void countAllMobsForSpawning() {
        countAllMobsForSpawning = getBoolean("count-all-mobs-for-spawning", false);
        if (countAllMobsForSpawning) {
            log("Counting all mobs for spawning. Mob farms may reduce natural spawns elsewhere in world.");
        } else {
            log("Using improved mob spawn limits (Only Natural Spawns impact spawn limits for more natural spawns)");
        }
    }
}
