package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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
            EntityType.WOLF,
            EntityType.IRON_GOLEM,
            EntityType.RAVAGER,
            EntityType.ENDERMAN,
            EntityType.EVOKER,
            EntityType.WITCH,
            EntityType.CAVE_SPIDER
    };

    private static final Map<UUID, BossBarTracker> ACTIVE_BOSS_BARS = new HashMap<>();

    public static boolean spawnEliteNear(ServerPlayer player) {

        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        EntityType<?> type = ELITE_CANDIDATES[RANDOM.nextInt(ELITE_CANDIDATES.length)];
        EliteTier tier = EliteTier.rollRandomTier(RANDOM);

        BlockPos spawnPos = findSafeSpawnPos(level, player.blockPosition());
        if (spawnPos == null) {
            EliteMobsMod.LOGGER.info("Could not find a safe spawn position for the elite, spawn cancelled.");
            return false;
        }

        if (!(type.create(level) instanceof PathfinderMob mob)) {
            return false;
        }

        mob.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(mob, 1.2D, true));
        mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(mob, Player.class, true));

        // ✅ FIX CORRECTO
        Player nearest = level.getNearestPlayer(mob, 64);
        if (nearest != null) {
            mob.setTarget(nearest);
        }

        mob.moveTo(
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                RANDOM.nextFloat() * 360.0F,
                0.0F
        );

        String eliteName = applyTier(level, mob, tier);

        level.addFreshEntity(mob);

        playSpawnSound(level, spawnPos, tier);

        if (tier.hasBossBar()) {
            registerBossBar(mob, tier, eliteName);
        }

        EliteMobsMod.LOGGER.info("Spawned a {} {} ('{}') near {} at {}",
                tier.getDisplayName(),
                type.getDescription().getString(),
                eliteName,
                player.getName().getString(),
                spawnPos
        );

        announceSpawn(player, eliteName, tier);

        return true;
    }

    private static void announceSpawn(ServerPlayer player, String eliteName, EliteTier tier) {
        Component message = Component.literal(
                "A " + eliteName + " has appeared nearby!"
        ).withStyle(tier.getColor());

        player.sendSystemMessage(message);
    }

    private static void playSpawnSound(ServerLevel level, BlockPos pos, EliteTier tier) {
        level.playSound(
                null,
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                tier.getSpawnSound(),
                SoundSource.HOSTILE,
                tier.getSpawnSoundVolume(),
                0.8F + RANDOM.nextFloat() * 0.3F
        );
    }

    private static String applyTier(ServerLevel level, PathfinderMob mob, EliteTier tier) {

        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() * tier.getHealthMultiplier());
        }

        AttributeInstance attackDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(attackDamage.getBaseValue() * tier.getDamageMultiplier());
        }

        mob.setHealth(mob.getMaxHealth());

        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));

        if (tier == EliteTier.BERSERKER) {
            AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * 1.25);
            }
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 0, false, false));
        }

        if (tier.hasGear()) {
            equipGear(level, mob, tier);
        }

        String baseName = mob.getType().getDescription().getString();
        String eliteName = EliteNameGenerator.generate(baseName);

        mob.setCustomName(Component.literal(eliteName)
                .withStyle(tier.getColor(), net.minecraft.ChatFormatting.BOLD));
        mob.setCustomNameVisible(true);

        mob.setPersistenceRequired();

        return eliteName;
    }

    private static void equipGear(ServerLevel level, PathfinderMob mob, EliteTier tier) {

        ItemStack helmet;
        ItemStack chestplate;
        ItemStack leggings;
        ItemStack boots;
        ItemStack weapon;

        switch (tier) {
            case ELITE -> {
                helmet = new ItemStack(Items.LEATHER_HELMET);
                chestplate = new ItemStack(Items.IRON_CHESTPLATE);
                leggings = new ItemStack(Items.LEATHER_LEGGINGS);
                boots = new ItemStack(Items.LEATHER_BOOTS);
                weapon = new ItemStack(Items.IRON_SWORD);
            }
            case CHAMPION -> {
                helmet = new ItemStack(Items.IRON_HELMET);
                chestplate = new ItemStack(Items.IRON_CHESTPLATE);
                leggings = new ItemStack(Items.IRON_LEGGINGS);
                boots = new ItemStack(Items.IRON_BOOTS);
                weapon = new ItemStack(Items.IRON_SWORD);
            }
            case BERSERKER -> {
                helmet = new ItemStack(Items.DIAMOND_HELMET);
                chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
                leggings = new ItemStack(Items.DIAMOND_LEGGINGS);
                boots = new ItemStack(Items.DIAMOND_BOOTS);
                weapon = new ItemStack(Items.DIAMOND_SWORD);
            }
            default -> {
                return;
            }
        }

        if (tier.hasEnchantedGear()) {
            enchantArmorPiece(level, helmet);
            enchantArmorPiece(level, chestplate);
            enchantArmorPiece(level, leggings);
            enchantArmorPiece(level, boots);
            enchantWeapon(level, weapon);
        }

        mob.setItemSlot(EquipmentSlot.HEAD, helmet);
        mob.setItemSlot(EquipmentSlot.CHEST, chestplate);
        mob.setItemSlot(EquipmentSlot.LEGS, leggings);
        mob.setItemSlot(EquipmentSlot.FEET, boots);
        mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);

        mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
        mob.setDropChance(EquipmentSlot.CHEST, 0.0F);
        mob.setDropChance(EquipmentSlot.LEGS, 0.0F);
        mob.setDropChance(EquipmentSlot.FEET, 0.0F);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private static Holder<Enchantment> resolveEnchantment(
            ServerLevel level, ResourceKey<Enchantment> key) {
        return level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
    }

    private static void enchantArmorPiece(ServerLevel level, ItemStack stack) {
        stack.enchant(resolveEnchantment(level, Enchantments.PROTECTION), 2 + RANDOM.nextInt(2));
        stack.enchant(resolveEnchantment(level, Enchantments.UNBREAKING), 2);
    }

    private static void enchantWeapon(ServerLevel level, ItemStack stack) {
        stack.enchant(resolveEnchantment(level, Enchantments.SHARPNESS), 3 + RANDOM.nextInt(2));
        stack.enchant(resolveEnchantment(level, Enchantments.UNBREAKING), 2);
        stack.enchant(resolveEnchantment(level, Enchantments.FIRE_ASPECT), 1);
    }

    private static void registerBossBar(PathfinderMob mob, EliteTier tier, String eliteName) {
        net.minecraft.server.level.ServerBossEvent bossEvent =
                new net.minecraft.server.level.ServerBossEvent(
                        Component.literal(eliteName).withStyle(tier.getColor()),
                        tier.getBossBarColor(),
                        BossEvent.BossBarOverlay.NOTCHED_10
                );

        bossEvent.setProgress(1.0F);

        ACTIVE_BOSS_BARS.put(mob.getUUID(), new BossBarTracker(mob, bossEvent));
    }

    public static void tickBossBars(ServerLevel level) {

        if (ACTIVE_BOSS_BARS.isEmpty()) return;

        var iterator = ACTIVE_BOSS_BARS.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            BossBarTracker tracker = entry.getValue();

            if (!tracker.mob.isAlive()) {
                tracker.bossEvent.removeAllPlayers();
                iterator.remove();
                continue;
            }

            float ratio = (float) (tracker.mob.getHealth() / tracker.mob.getMaxHealth());
            tracker.bossEvent.setProgress(Math.max(0.0F, Math.min(1.0F, ratio)));

            double rangeSq = 48.0 * 48.0;

            for (ServerPlayer player : level.players()) {
                boolean inRange = player.distanceToSqr(tracker.mob) <= rangeSq;
                boolean tracked = tracker.bossEvent.getPlayers().contains(player);

                if (inRange && !tracked) {
                    tracker.bossEvent.addPlayer(player);
                } else if (!inRange && tracked) {
                    tracker.bossEvent.removePlayer(player);
                }
            }
        }
    }

    private static class BossBarTracker {
        final PathfinderMob mob;
        final net.minecraft.server.level.ServerBossEvent bossEvent;

        BossBarTracker(PathfinderMob mob, net.minecraft.server.level.ServerBossEvent bossEvent) {
            this.mob = mob;
            this.bossEvent = bossEvent;
        }
    }

    private static BlockPos findSafeSpawnPos(ServerLevel level, BlockPos origin) {

        final int radius = 10;
        final int attempts = 12;

        for (int i = 0; i < attempts; i++) {
            int dx = RANDOM.nextInt(radius * 2) - radius;
            int dz = RANDOM.nextInt(radius * 2) - radius;

            BlockPos columnTop = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING,
                    origin.offset(dx, 0, dz)
            );

            BlockPos candidate = columnTop.below();

            if (level.getBlockState(candidate).isAir()) {
                continue;
            }

            BlockPos feetPos = candidate.above();
            if (level.getBlockState(feetPos).isAir()
                    && level.getBlockState(feetPos.above()).isAir()) {
                return feetPos;
            }
        }

        return null;
    }
}