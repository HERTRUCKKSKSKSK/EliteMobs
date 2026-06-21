package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;

/**
 * Difficulty tiers for elite mobs, ordered from weakest to strongest.
 * Each tier defines stat multipliers, display color, spawn sound, and
 * whether the mob should be equipped with armor/weapons (and enchanted
 * gear for the top tiers). Champion and Berserker also get a boss bar.
 */
public enum EliteTier {

    TOUGH(
            "Tough",
            ChatFormatting.GREEN,
            1.8,   // health multiplier
            1.3,   // damage multiplier
            45,    // spawn weight (relative, out of 100 total)
            false, // gear?
            false, // enchanted gear?
            SoundEvents.WOLF_HOWL,
            1.0F,  // sound volume
            false, // boss bar?
            null
    ),
    ELITE(
            "Elite",
            ChatFormatting.GOLD,
            2.5,
            1.6,
            30,
            true,
            false,
            SoundEvents.ELDER_GUARDIAN_CURSE,
            1.0F,
            false,
            null
    ),
    CHAMPION(
            "Champion",
            ChatFormatting.AQUA,
            3.5,
            2.2,
            18,
            true,
            true,
            SoundEvents.WITHER_AMBIENT,
            1.2F,
            true,
            BossEvent.BossBarColor.BLUE
    ),
    BERSERKER(
            "Berserker",
            ChatFormatting.DARK_RED,
            5.0,
            3.0,
            7,
            true,
            true,
            SoundEvents.ENDER_DRAGON_GROWL,
            1.5F,
            true,
            BossEvent.BossBarColor.RED
    );

    private final String displayName;
    private final ChatFormatting color;
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final int spawnWeight;
    private final boolean hasGear;
    private final boolean enchantedGear;
    private final SoundEvent spawnSound;
    private final float spawnSoundVolume;
    private final boolean hasBossBar;
    private final BossEvent.BossBarColor bossBarColor;

    EliteTier(String displayName, ChatFormatting color, double healthMultiplier,
              double damageMultiplier, int spawnWeight, boolean hasGear, boolean enchantedGear,
              SoundEvent spawnSound, float spawnSoundVolume, boolean hasBossBar,
              BossEvent.BossBarColor bossBarColor) {
        this.displayName = displayName;
        this.color = color;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.spawnWeight = spawnWeight;
        this.hasGear = hasGear;
        this.enchantedGear = enchantedGear;
        this.spawnSound = spawnSound;
        this.spawnSoundVolume = spawnSoundVolume;
        this.hasBossBar = hasBossBar;
        this.bossBarColor = bossBarColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public int getSpawnWeight() {
        return spawnWeight;
    }

    public boolean hasGear() {
        return hasGear;
    }

    public boolean hasEnchantedGear() {
        return enchantedGear;
    }

    public SoundEvent getSpawnSound() {
        return spawnSound;
    }

    public float getSpawnSoundVolume() {
        return spawnSoundVolume;
    }

    /** Whether mobs of this tier display a boss-style health bar (like the Wither/Ender Dragon). */
    public boolean hasBossBar() {
        return hasBossBar;
    }

    public BossEvent.BossBarColor getBossBarColor() {
        return bossBarColor;
    }

    /**
     * Picks a random tier using weighted probabilities (see spawnWeight on each
     * constant). Berserker is rare, Tough is common.
     */
    public static EliteTier rollRandomTier(java.util.Random random) {
        int totalWeight = 0;
        for (EliteTier tier : values()) {
            totalWeight += tier.spawnWeight;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;

        for (EliteTier tier : values()) {
            cumulative += tier.spawnWeight;
            if (roll < cumulative) {
                return tier;
            }
        }

        // Should never happen, but fall back to the weakest tier just in case.
        return TOUGH;
    }
}