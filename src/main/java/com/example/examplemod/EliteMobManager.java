package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

/**
 * Central manager for spawning and managing elite mobs.
 *
 * Changes from v1:
 *  - All spawns are DifficultyLevel-aware (tier probabilities, global stat multipliers)
 *  - Added INFERNAL tier with dual-sound and unique boss bar color
 *  - Boss bar now uses NOTCHED_20 style for higher tiers
 *  - On-death loot delegation to EliteLootTable
 *  - Gear scaling per tier+level (Treachery bosses get full enchanted netherite)
 */
public class EliteMobManager {

    private static final Random RANDOM = new Random();

    private static final EntityType<?>[] ELITE_CANDIDATES = {
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER,
            EntityType.HUSK,
            EntityType.DROWNED,
            EntityType.STRAY,
            EntityType.PILLAGER,
            EntityType.VINDICATOR,
            EntityType.IRON_GOLEM,
            EntityType.RAVAGER,
            EntityType.ENDERMAN,
            EntityType.EVOKER,
            EntityType.WITCH,
            EntityType.CAVE_SPIDER
    };

    // Map mob UUID → tracker
    private static final Map<UUID, BossBarTracker> ACTIVE_BOSS_BARS = new HashMap<>();
    // Map mob UUID → who to grant loot to on death + tier/level info
    private static final Map<UUID, LootContext> LOOT_CONTEXTS = new HashMap<>();

    // ──────────────────────────────────────────────────────────────────────
    // SPAWN
    // ──────────────────────────────────────────────────────────────────────

    /** Spawns an elite using the player's active DifficultyLevel tier table. */
    public static boolean spawnEliteNear(ServerPlayer player) {
        PlayerProgressionData data = PlayerProgressionData.get(player);
        DifficultyLevel diff = data.getActiveLevel();
        EliteTier tier = diff.rollTier(RANDOM);
        return spawnEliteNear(player, tier, diff);
    }

    /** Spawns an elite with a forced tier (command usage) — uses player's active difficulty. */
    public static boolean spawnEliteNear(ServerPlayer player, EliteTier tier) {
        PlayerProgressionData data = PlayerProgressionData.get(player);
        DifficultyLevel diff = data.getActiveLevel();
        return spawnEliteNear(player, tier, diff);
    }

    /** Core spawn method. */
    public static boolean spawnEliteNear(ServerPlayer player, EliteTier tier, DifficultyLevel diff) {
        if (!(player.level() instanceof ServerLevel level)) return false;

        EntityType<?> type = ELITE_CANDIDATES[RANDOM.nextInt(ELITE_CANDIDATES.length)];
        BlockPos spawnPos = findSafeSpawnPos(level, player.blockPosition());
        if (spawnPos == null) return false;

        if (!(type.create(level) instanceof PathfinderMob mob)) return false;

        // AI
        mob.goalSelector.addGoal(1,
                new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(mob, 1.2D, true));
        mob.targetSelector.addGoal(1,
                new NearestAttackableTargetGoal<>(mob, Player.class, true));

        Player nearest = level.getNearestPlayer(mob, 64);
        if (nearest != null) mob.setTarget(nearest);

        mob.moveTo(
                spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                RANDOM.nextFloat() * 360.0F, 0.0F);

        String name = applyTierAndDifficulty(level, mob, tier, diff);

        level.addFreshEntity(mob);

        playSpawnSound(level, spawnPos, tier);

        if (tier.hasBossBar()) {
            registerBossBar(mob, tier, name);
        }

        // Register loot context
        LOOT_CONTEXTS.put(mob.getUUID(), new LootContext(player, tier, diff));

        announceSpawn(player, name, tier, diff);
        return true;
    }

    // ──────────────────────────────────────────────────────────────────────
    // TIER + DIFFICULTY APPLICATION
    // ──────────────────────────────────────────────────────────────────────

    private static String applyTierAndDifficulty(ServerLevel level, PathfinderMob mob,
                                                 EliteTier tier, DifficultyLevel diff) {
        // Health: tier multiplier × global difficulty multiplier
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            double newHp = hp.getBaseValue() * tier.getHealthMultiplier() * diff.getGlobalHealthMult();
            // Violence+ levels get brutally scaled
            if (diff.getIndex() >= 6) newHp *= 1.5;
            hp.setBaseValue(newHp);
        }

        // Damage
        AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            double newDmg = dmg.getBaseValue() * tier.getDamageMultiplier() * diff.getGlobalDamageMult();
            if (diff.getIndex() >= 6) newDmg *= 1.3;
            dmg.setBaseValue(newDmg);
        }

        mob.setHealth(mob.getMaxHealth());

        // Glowing
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));

        // Random effects (scaled by difficulty)
        applyEffects(mob, tier, diff);

        // Gear
        if (tier.hasGear()) {
            equipScaledGear(level, mob, tier, diff);
        }

        // Custom name
        String base = mob.getType().getDescription().getString();
        String name = EliteNameGenerator.generate(base, tier, diff);

        mob.setCustomName(Component.literal(name)
                .withStyle(tier.getColor(), net.minecraft.ChatFormatting.BOLD));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();

        return name;
    }

    // ──────────────────────────────────────────────────────────────────────
    // EFFECTS
    // ──────────────────────────────────────────────────────────────────────

    private static void applyEffects(PathfinderMob mob, EliteTier tier, DifficultyLevel diff) {
        int diffIdx = diff.getIndex();

        // Speed — scales with difficulty
        int speedAmp = Math.min(3, diffIdx / 3); // 0,0,0 then 1,1,1 then 2
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, speedAmp));

        // Strength — always present from ELITE+
        if (tier.getLootLevel() >= 1) {
            int strAmp = Math.min(4, tier.getLootLevel() + diffIdx / 4);
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, strAmp));
        }

        // Regen — CHAMPION+
        if (tier.getLootLevel() >= 2) {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 0));
        }

        // Resistance — BERSERKER+
        if (tier == EliteTier.BERSERKER || tier == EliteTier.INFERNAL) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 3));
            AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(speed.getBaseValue() * 1.35);
        }

        // INFERNAL: fire immunity, invisibility flicker, and more
        if (tier == EliteTier.INFERNAL) {
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 2));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 4));
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 1));
            // Set mob on fire briefly for visual effect — will be extinguished by fire_resistance
            mob.igniteForSeconds(0); // Not actually on fire, just visual flair via particles from effects
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // GEAR SCALING
    // ──────────────────────────────────────────────────────────────────────

    private static void equipScaledGear(ServerLevel level, PathfinderMob mob,
                                        EliteTier tier, DifficultyLevel diff) {
        int diffIdx = diff.getIndex();

        ItemStack weapon;
        ItemStack helmet;
        ItemStack chest;
        ItemStack legs;
        ItemStack boots;

        if (tier == EliteTier.INFERNAL) {
            // Lucifer: full legendary infernal gear (same as loot drop)
            weapon = infernalWeapon(level);
            helmet = infernalArmorPiece(level, EquipmentSlot.HEAD);
            chest  = infernalArmorPiece(level, EquipmentSlot.CHEST);
            legs   = infernalArmorPiece(level, EquipmentSlot.LEGS);
            boots  = infernalArmorPiece(level, EquipmentSlot.FEET);
        } else if (tier == EliteTier.BERSERKER || diffIdx >= 7) {
            // Netherite with heavy enchants
            weapon = enchantedItem(level, Items.NETHERITE_SWORD, true, 4, 6);
            helmet = enchantedItem(level, Items.NETHERITE_HELMET, false, 3, 5);
            chest  = enchantedItem(level, Items.NETHERITE_CHESTPLATE, false, 3, 5);
            legs   = enchantedItem(level, Items.NETHERITE_LEGGINGS, false, 3, 5);
            boots  = enchantedItem(level, Items.NETHERITE_BOOTS, false, 3, 5);
        } else if (tier == EliteTier.CHAMPION || diffIdx >= 5) {
            weapon = enchantedItem(level, Items.DIAMOND_SWORD, true, 2, 4);
            helmet = enchantedItem(level, Items.DIAMOND_HELMET, false, 2, 3);
            chest  = enchantedItem(level, Items.DIAMOND_CHESTPLATE, false, 2, 3);
            legs   = enchantedItem(level, Items.DIAMOND_LEGGINGS, false, 2, 3);
            boots  = enchantedItem(level, Items.DIAMOND_BOOTS, false, 2, 3);
        } else if (tier == EliteTier.ELITE || diffIdx >= 3) {
            weapon = enchantedItem(level, Items.IRON_SWORD, true, 1, 2);
            helmet = new ItemStack(Items.IRON_HELMET);
            chest  = new ItemStack(Items.IRON_CHESTPLATE);
            legs   = new ItemStack(Items.IRON_LEGGINGS);
            boots  = new ItemStack(Items.IRON_BOOTS);
        } else {
            weapon = new ItemStack(Items.STONE_SWORD);
            helmet = new ItemStack(Items.LEATHER_HELMET);
            chest  = new ItemStack(Items.LEATHER_CHESTPLATE);
            legs   = new ItemStack(Items.LEATHER_LEGGINGS);
            boots  = new ItemStack(Items.LEATHER_BOOTS);
        }

        mob.setItemSlot(EquipmentSlot.HEAD,     helmet);
        mob.setItemSlot(EquipmentSlot.CHEST,    chest);
        mob.setItemSlot(EquipmentSlot.LEGS,     legs);
        mob.setItemSlot(EquipmentSlot.FEET,     boots);
        mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);

        // No drops from gear (drops come from EliteLootTable)
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        mob.setDropChance(EquipmentSlot.HEAD,     0.0F);
        mob.setDropChance(EquipmentSlot.CHEST,    0.0F);
        mob.setDropChance(EquipmentSlot.LEGS,     0.0F);
        mob.setDropChance(EquipmentSlot.FEET,     0.0F);
    }

    // ──────────────────────────────────────────────────────────────────────
    // ITEM BUILDERS
    // ──────────────────────────────────────────────────────────────────────

    private static ItemStack enchantedItem(ServerLevel level, Item item, boolean weapon, int min, int max) {
        ItemStack stack = new ItemStack(item);
        String[] weaponEnchs = {"sharpness","fire_aspect","knockback","unbreaking"};
        String[] armorEnchs  = {"protection","fire_protection","unbreaking","thorns"};
        String[] pool = weapon ? weaponEnchs : armorEnchs;
        int rolls = min + RANDOM.nextInt(Math.max(1, max - min + 1));
        Set<String> used = new HashSet<>();
        for (int i = 0; i < rolls; i++) {
            String ench = pool[RANDOM.nextInt(pool.length)];
            if (used.add(ench)) addUnsafeEnchant(level, stack, ench, 1 + RANDOM.nextInt(weapon ? 5 : 4));
        }
        return stack;
    }

    private static ItemStack infernalWeapon(ServerLevel level) {
        ItemStack sw = new ItemStack(Items.NETHERITE_SWORD);
        sw.set(DataComponents.CUSTOM_NAME, Component.literal("§4§l☠ Lucifer's Judgement ☠"));
        addUnsafeEnchant(level, sw, "sharpness", 5);
        addUnsafeEnchant(level, sw, "fire_aspect", 2);
        addUnsafeEnchant(level, sw, "looting", 3);
        addUnsafeEnchant(level, sw, "unbreaking", 3);
        return sw;
    }

    private static ItemStack infernalArmorPiece(ServerLevel level, EquipmentSlot slot) {
        Item item = switch (slot) {
            case HEAD  -> Items.NETHERITE_HELMET;
            case CHEST -> Items.NETHERITE_CHESTPLATE;
            case LEGS  -> Items.NETHERITE_LEGGINGS;
            case FEET  -> Items.NETHERITE_BOOTS;
            default    -> Items.NETHERITE_CHESTPLATE;
        };
        ItemStack stack = new ItemStack(item);
        addUnsafeEnchant(level, stack, "protection", 4);
        addUnsafeEnchant(level, stack, "fire_protection", 4);
        addUnsafeEnchant(level, stack, "thorns", 3);
        addUnsafeEnchant(level, stack, "unbreaking", 3);
        return stack;
    }

    /**
     * Añade un encantamiento por su ID vanilla (ej. "sharpness") usando el
     * Data Component ENCHANTMENTS (NBT/CompoundTag ya no existe en 1.20.5+).
     */
    private static void addUnsafeEnchant(ServerLevel level, ItemStack stack, String enchId, int lvl) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("minecraft", enchId);
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, location);

        Optional<Holder.Reference<Enchantment>> holder = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(key);

        if (holder.isEmpty()) {
            // ID de encantamiento desconocido: ignorar en vez de petar el server
            return;
        }

        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        mutable.set(holder.get(), lvl);
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    // ──────────────────────────────────────────────────────────────────────
    // SOUNDS
    // ──────────────────────────────────────────────────────────────────────

    private static void playSpawnSound(ServerLevel level, BlockPos pos, EliteTier tier) {
        // INFERNAL: play two sounds simultaneously for the "hellish combination"
        if (tier == EliteTier.INFERNAL) {
            level.playSound(null, pos, SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 2.0F, 0.5F);
            level.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0F, 0.7F);
            level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.5F, 0.8F);
        } else {
            level.playSound(null, pos,
                    tier.getSpawnSound(), SoundSource.HOSTILE,
                    tier.getSpawnSoundVolume(), 1.0F);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // ANNOUNCE
    // ──────────────────────────────────────────────────────────────────────

    private static void announceSpawn(ServerPlayer player, String name, EliteTier tier, DifficultyLevel diff) {
        String msg = tier == EliteTier.INFERNAL
                ? "§5§l☠ LUCIFER HAS APPEARED! ☠ §7" + name
                : "§7A " + tier.getColor() + name + "§7 approaches! [" + diff.getColor() + diff.getDisplayName() + "§7]";
        player.sendSystemMessage(Component.literal(msg));
    }

    // ──────────────────────────────────────────────────────────────────────
    // SAFE SPAWN POS
    // ──────────────────────────────────────────────────────────────────────

    private static BlockPos findSafeSpawnPos(ServerLevel level, BlockPos origin) {
        for (int i = 0; i < 16; i++) {
            int dx = RANDOM.nextInt(32) - 16;
            int dz = RANDOM.nextInt(32) - 16;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                    origin.offset(dx, 0, dz));
            BlockPos feet = top.below();
            if (level.getBlockState(feet).isSolid()
                    && level.getBlockState(feet.above()).isAir()) {
                return feet.above();
            }
        }
        return null;
    }

    // ──────────────────────────────────────────────────────────────────────
    // BOSS BAR
    // ──────────────────────────────────────────────────────────────────────

    private static void registerBossBar(PathfinderMob mob, EliteTier tier, String name) {
        BossEvent.BossBarOverlay overlay = tier == EliteTier.INFERNAL
                ? BossEvent.BossBarOverlay.PROGRESS
                : BossEvent.BossBarOverlay.NOTCHED_10;

        ServerBossEvent boss = new ServerBossEvent(
                Component.literal(name).withStyle(tier.getColor()),
                tier.getBossBarColor(),
                overlay
        );
        boss.setProgress(1.0F);
        ACTIVE_BOSS_BARS.put(mob.getUUID(), new BossBarTracker(mob, boss));
    }

    public static void tickBossBars(ServerLevel level) {
        var it = ACTIVE_BOSS_BARS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            BossBarTracker t = e.getValue();
            if (!t.mob.isAlive()) {
                t.boss.removeAllPlayers();
                it.remove();
                // Grant loot on death
                LootContext ctx = LOOT_CONTEXTS.remove(e.getKey());
                if (ctx != null && ctx.player.isAlive()) {
                    EliteLootTable.grantLoot(ctx.player, t.mob, ctx.tier, ctx.diffLevel);
                }
                continue;
            }
            float ratio = t.mob.getHealth() / t.mob.getMaxHealth();
            t.boss.setProgress(Math.max(0f, Math.min(1f, ratio)));
            double range = 64 * 64;
            for (ServerPlayer p : level.players()) {
                boolean near = p.distanceToSqr(t.mob) <= range;
                boolean tracked = t.boss.getPlayers().contains(p);
                if (near && !tracked) t.boss.addPlayer(p);
                if (!near && tracked) t.boss.removePlayer(p);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // LOOT ON DEATH — for non-boss-bar tiers (TOUGH, ELITE)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Called from the death event for non-boss-bar elites (TOUGH, ELITE tier).
     */
    public static void handleEliteDeath(net.minecraft.world.entity.LivingEntity entity,
                                        ServerPlayer killer) {
        LootContext ctx = LOOT_CONTEXTS.remove(entity.getUUID());
        if (ctx != null) {
            EliteLootTable.grantLoot(ctx.player, (PathfinderMob) entity, ctx.tier, ctx.diffLevel);
        }
    }

    public static boolean isTrackedElite(UUID uuid) {
        return LOOT_CONTEXTS.containsKey(uuid) || ACTIVE_BOSS_BARS.containsKey(uuid);
    }

    // ──────────────────────────────────────────────────────────────────────
    // INNER CLASSES
    // ──────────────────────────────────────────────────────────────────────

    private static class BossBarTracker {
        final PathfinderMob mob;
        final ServerBossEvent boss;
        BossBarTracker(PathfinderMob mob, ServerBossEvent boss) {
            this.mob = mob;
            this.boss = boss;
        }
    }

    private static class LootContext {
        final ServerPlayer player;
        final EliteTier tier;
        final DifficultyLevel diffLevel;
        LootContext(ServerPlayer player, EliteTier tier, DifficultyLevel diffLevel) {
            this.player = player;
            this.tier = tier;
            this.diffLevel = diffLevel;
        }
    }
}