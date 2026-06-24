package com.example.examplemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player progression state:
 *  - Active difficulty level (chosen by the player via the GUI)
 *  - Which armor tiers have ever been worn (persists even after removing the armor)
 *  - Highest XP level ever reached (for unlock checks that persist)
 *
 * Data is stored in a static in-memory map and also serialised to player NBT
 * via the AttachmentType system (NeoForge 1.21.x).
 *
 * NBT layout (under key "EliteMobsData"):
 *   activeLevel   : int
 *   armorUnlocks  : int bitmask (bit 0=LEATHER,1=IRON,2=DIAMOND,3=NETHERITE)
 *   highestXpSeen : int
 */
public class PlayerProgressionData {

    private static final String NBT_KEY = "EliteMobsData";
    private static final ConcurrentHashMap<UUID, PlayerProgressionData> CACHE = new ConcurrentHashMap<>();

    // ──────────────────────────────────────────────────
    // Fields
    // ──────────────────────────────────────────────────

    /** The difficulty circle the player has *activated*. */
    private DifficultyLevel activeLevel;

    /**
     * Bitmask of armor tiers ever equipped.
     * Bit 0 = LEATHER, 1 = IRON, 2 = DIAMOND, 3 = NETHERITE.
     */
    private int armorUnlockBits;

    /** The highest XP level this player has ever reached. */
    private int highestXpSeen;

    // ──────────────────────────────────────────────────
    // Construction
    // ──────────────────────────────────────────────────

    public PlayerProgressionData() {
        this.activeLevel = DifficultyLevel.LIMBO;
        this.armorUnlockBits = 0;
        this.highestXpSeen = 0;
    }

    // ──────────────────────────────────────────────────
    // Static access
    // ──────────────────────────────────────────────────

    public static PlayerProgressionData get(Player player) {
        return CACHE.computeIfAbsent(player.getUUID(), id -> {
            PlayerProgressionData data = new PlayerProgressionData();
            data.loadFromPlayer(player);
            return data;
        });
    }

    public static void invalidate(UUID uuid) {
        CACHE.remove(uuid);
    }

    // ──────────────────────────────────────────────────
    // NBT persistence
    // ──────────────────────────────────────────────────

    public void saveToPlayer(Player player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag tag = new CompoundTag();
        tag.putInt("activeLevel", activeLevel.ordinal());
        tag.putInt("armorUnlocks", armorUnlockBits);
        tag.putInt("highestXpSeen", highestXpSeen);
        root.put(NBT_KEY, tag);
    }

    public void loadFromPlayer(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(NBT_KEY)) return;
        CompoundTag tag = root.getCompound(NBT_KEY);
        int ordinal = tag.getInt("activeLevel");
        DifficultyLevel[] levels = DifficultyLevel.values();
        this.activeLevel = (ordinal >= 0 && ordinal < levels.length) ? levels[ordinal] : DifficultyLevel.LIMBO;
        this.armorUnlockBits = tag.getInt("armorUnlocks");
        this.highestXpSeen = tag.getInt("highestXpSeen");
    }

    // ──────────────────────────────────────────────────
    // Armor unlock tracking
    // ──────────────────────────────────────────────────

    /**
     * Called every tick (or on equipment change) to check if the player has
     * equipped a new armor tier that should be permanently unlocked.
     */
    public void checkAndRecordArmor(Player player) {
        boolean changed = false;

        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.isEmpty()) continue;

            if (DifficultyLevel.ArmorTierRequirement.NETHERITE.isSatisfiedBy(stack)) {
                int bit = 1 << 3;
                if ((armorUnlockBits & bit) == 0) { armorUnlockBits |= bit; changed = true; }
            } else if (DifficultyLevel.ArmorTierRequirement.DIAMOND.isSatisfiedBy(stack)) {
                int bit = 1 << 2;
                if ((armorUnlockBits & bit) == 0) { armorUnlockBits |= bit; changed = true; }
            } else if (DifficultyLevel.ArmorTierRequirement.IRON.isSatisfiedBy(stack)) {
                int bit = 1 << 1;
                if ((armorUnlockBits & bit) == 0) { armorUnlockBits |= bit; changed = true; }
            } else if (DifficultyLevel.ArmorTierRequirement.LEATHER.isSatisfiedBy(stack)) {
                int bit = 1;
                if ((armorUnlockBits & bit) == 0) { armorUnlockBits |= bit; changed = true; }
            }
        }

        // Track XP
        if (player.experienceLevel > highestXpSeen) {
            highestXpSeen = player.experienceLevel;
            changed = true;
        }

        if (changed) saveToPlayer(player);
    }

    /**
     * Whether this player has permanently unlocked the given armor tier requirement.
     */
    public boolean hasArmorUnlock(DifficultyLevel.ArmorTierRequirement req) {
        return switch (req) {
            case NONE -> true;
            case LEATHER -> (armorUnlockBits & 1) != 0;
            case IRON -> (armorUnlockBits & (1 << 1)) != 0;
            case DIAMOND -> (armorUnlockBits & (1 << 2)) != 0;
            case NETHERITE -> (armorUnlockBits & (1 << 3)) != 0;
        };
    }

    // ──────────────────────────────────────────────────
    // Unlock check for a DifficultyLevel
    // ──────────────────────────────────────────────────

    /**
     * Returns true if the player has met the permanent unlock requirements
     * for the given difficulty level.
     */
    public boolean hasUnlocked(DifficultyLevel level) {
        if (!hasArmorUnlock(level.getArmorRequirement())) return false;
        return highestXpSeen >= level.getXpLevelRequirement();
    }

    // ──────────────────────────────────────────────────
    // Getters / Setters
    // ──────────────────────────────────────────────────

    public DifficultyLevel getActiveLevel() { return activeLevel; }

    public void setActiveLevel(DifficultyLevel level) {
        this.activeLevel = level;
    }

    public int getHighestXpSeen() { return highestXpSeen; }
}
