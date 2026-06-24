package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;

/**
 * Difficulty tiers for elite mobs, ordered from weakest to strongest.
 * TOUGH → ELITE → CHAMPION → BERSERKER → INFERNAL (Lucifer)
 *
 * Spawn probabilities are set per-DifficultyLevel, not here. These
 * values are only used for the "/elite <tier>" forced-spawn command.
 */
public enum EliteTier {

    TOUGH(
            "Tough",
            ChatFormatting.GREEN,
            1.8,   // health
            1.3,   // damage
            false, // gear
            false, // enchanted gear
            SoundEvents.WOLF_HOWL,
            1.0F,
            false,
            null,
            0      // loot level 0
    ),
    ELITE(
            "Elite",
            ChatFormatting.GOLD,
            2.8,
            1.7,
            true,
            false,
            SoundEvents.ELDER_GUARDIAN_CURSE,
            1.2F,
            false,
            null,
            1
    ),
    CHAMPION(
            "Champion",
            ChatFormatting.AQUA,
            4.5,
            2.5,
            true,
            true,
            SoundEvents.WITHER_AMBIENT,
            1.4F,
            true,
            BossEvent.BossBarColor.BLUE,
            2
    ),
    BERSERKER(
            "Berserker",
            ChatFormatting.DARK_RED,
            7.0,
            3.5,
            true,
            true,
            SoundEvents.ENDER_DRAGON_GROWL,
            1.6F,
            true,
            BossEvent.BossBarColor.RED,
            3
    ),
    INFERNAL(
            "Lucifer",
            ChatFormatting.DARK_PURPLE,
            12.0,
            5.0,
            true,
            true,
            // Combinación infernal: usamos WITHER_DEATH (se tocará junto con ENDER_DRAGON_GROWL desde el manager)
            SoundEvents.WITHER_DEATH,
            2.0F,
            true,
            BossEvent.BossBarColor.PURPLE,
            4
    );

    private final String displayName;
    private final ChatFormatting color;
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final boolean hasGear;
    private final boolean enchantedGear;
    private final SoundEvent spawnSound;
    private final float spawnSoundVolume;
    private final boolean hasBossBar;
    private final BossEvent.BossBarColor bossBarColor;
    private final int lootLevel; // 0=Tough, 1=Elite, 2=Champion, 3=Berserker, 4=Infernal

    EliteTier(String displayName, ChatFormatting color,
              double healthMultiplier, double damageMultiplier,
              boolean hasGear, boolean enchantedGear,
              SoundEvent spawnSound, float spawnSoundVolume,
              boolean hasBossBar, BossEvent.BossBarColor bossBarColor,
              int lootLevel) {
        this.displayName = displayName;
        this.color = color;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.hasGear = hasGear;
        this.enchantedGear = enchantedGear;
        this.spawnSound = spawnSound;
        this.spawnSoundVolume = spawnSoundVolume;
        this.hasBossBar = hasBossBar;
        this.bossBarColor = bossBarColor;
        this.lootLevel = lootLevel;
    }

    public String getDisplayName() { return displayName; }
    public ChatFormatting getColor() { return color; }
    public double getHealthMultiplier() { return healthMultiplier; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public boolean hasGear() { return hasGear; }
    public boolean hasEnchantedGear() { return enchantedGear; }
    public SoundEvent getSpawnSound() { return spawnSound; }
    public float getSpawnSoundVolume() { return spawnSoundVolume; }
    public boolean hasBossBar() { return hasBossBar; }
    public BossEvent.BossBarColor getBossBarColor() { return bossBarColor; }
    public int getLootLevel() { return lootLevel; }

    /** Returns true if this tier is the Infernal/Lucifer tier. */
    public boolean isInfernal() { return this == INFERNAL; }
}
