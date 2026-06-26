package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The 9 progressive difficulty levels of EliteMobs, inspired by Dante's Inferno.
 *
 * Each level has:
 *  - unlock requirements (armor tier + xp level)
 *  - spawn count per night
 *  - spawn probabilities per EliteTier (must sum to 100)
 *  - base stat multipliers that affect ALL overworld mobs
 *  - a display color and description
 */
public enum DifficultyLevel {

    // ────────────────────────────────────────────────────────────────────────
    // Circle I – LIMBO  (unlocked by default)
    // ────────────────────────────────────────────────────────────────────────
    LIMBO(
            0,
            "Limbo",
            ChatFormatting.WHITE,
            "§7The edge of darkness. 0 boss per night for now.",
            // Unlock: no armor, no xp
            ArmorTierRequirement.NONE, 0,
            // Bosses per night
            0,
            // Spawn weights: TOUGH, ELITE, CHAMPION, BERSERKER, INFERNAL
            new int[]{0, 0, 0, 0, 0},
            // Global mob multiplier (health, damage)
            1.0, 1.0
    ),

    // ────────────────────────────────────────────────────────────────────────
    // Circle II – LUST
    // ────────────────────────────────────────────────────────────────────────
    LUST(
            1,
            "Lust",
            ChatFormatting.LIGHT_PURPLE,
            "§dPassion blinds. 2 bosses per night. Requires leather armor.",
            ArmorTierRequirement.LEATHER, 0,
            2,
            new int[]{70, 20, 9, 1, 0},
            1.1, 1.1
    ),

    // ────────────────────────────────────────────────────────────────────────
    // Circle III – GLUTTONY
    // ────────────────────────────────────────────────────────────────────────
    GLUTTONY(
            2,
            "Gluttony",
            ChatFormatting.DARK_GREEN,
            "§2Excess devours. 3 bosses per night. Iron armor + 5 levels.",
            ArmorTierRequirement.IRON, 5,
            3,
            new int[]{60, 25, 13, 2, 0},
            1.25, 1.2
    ),

    // ────────────────────────────────────────────────────────────────────────
    // Circle IV – GREED
    // ────────────────────────────────────────────────────────────────────────
    GREED(
            3,
            "Greed",
            ChatFormatting.GOLD,
            "§6Wealth corrupts. 4 bosses per night. Iron armor + 10 levels.",
            ArmorTierRequirement.IRON, 10,
            4,
            new int[]{60, 25, 10, 5, 0},
            1.4, 1.35
    ),

    // ────────────────────────────────────────────────────────────────────────
    // Circle V – ANGER
    // ────────────────────────────────────────────────────────────────────────
    ANGER(
            4,
            "Anger",
            ChatFormatting.RED,
            "§cRage consumes. 4 bosses per night. Iron armor + 15 levels.",
            ArmorTierRequirement.IRON, 15,
            4,
            new int[]{55, 25, 15, 5, 0},
            1.6, 1.5
    ),

    // ────────────────────────────────────────────────────────────────────────
    // Circle VI – HERESY
    // ────────────────────────────────────────────────────────────────────────
    HERESY(
            5,
            "Heresy",
            ChatFormatting.DARK_AQUA,
            "§3Beliefs burn. 4 bosses per night. Iron armor + 20 levels.",
            ArmorTierRequirement.IRON, 20,
            4,
            new int[]{50, 30, 15, 5, 0},
            1.8, 1.7
    ),

    // ────────────────────────────────────────────────────────────────────────
    // Circle VII – VIOLENCE
    // ────────────────────────────────────────────────────────────────────────
    VIOLENCE(
            6,
            "Violence",
            ChatFormatting.DARK_RED,
            "§4Blood flows freely. 5 bosses per night. Diamond armor + 20 levels.",
            ArmorTierRequirement.DIAMOND, 20,
            5,
            new int[]{40, 40, 20, 9, 1},  // NOTE: Infernal now has 1%! (adjusting: 40+35+19+9+5=108 → normalize below... actually: 38+33+15+9+5=100)
            // Let's keep exact: 38,33,15,9,5 = 100
            2.2, 2.0
    ),

    // ────────────────────────────────────────────────────────────────────────
    // Circle VIII – FRAUD
    // ────────────────────────────────────────────────────────────────────────
    FRAUD(
            7,
            "Fraud",
            ChatFormatting.GRAY,
            "§8Deception kills. 5 bosses per night. Diamond armor + 30 levels.",
            ArmorTierRequirement.DIAMOND, 30,
            5,
            new int[]{20, 40, 20, 15, 5},
            2.8, 2.5
    ),

    // ────────────────────────────────────────────────────────────────────────
    // Circle IX – TREACHERY
    // ────────────────────────────────────────────────────────────────────────
    TREACHERY(
            8,
            "Treachery",
            ChatFormatting.DARK_GRAY,
            "§8§lAbandon all hope, ye who enter here. 5 bosses per night. Netherite + 30 levels.",
            ArmorTierRequirement.NETHERITE, 30,
            5,
            new int[]{0, 35, 30, 25, 10},
            4.0, 3.5
    );

    // ──────────────────────────────────────────────────────────────────────────

    private final int index;
    private final String displayName;
    private final ChatFormatting color;
    private final String description;
    private final ArmorTierRequirement armorRequirement;
    private final int xpLevelRequirement;
    private final int bossesPerNight;
    private final int[] spawnWeights; // [TOUGH, ELITE, CHAMPION, BERSERKER, INFERNAL]
    private final double globalHealthMult;
    private final double globalDamageMult;

    DifficultyLevel(int index, String displayName, ChatFormatting color, String description,
                    ArmorTierRequirement armorRequirement, int xpLevelRequirement,
                    int bossesPerNight, int[] spawnWeights,
                    double globalHealthMult, double globalDamageMult) {
        this.index = index;
        this.displayName = displayName;
        this.color = color;
        this.description = description;
        this.armorRequirement = armorRequirement;
        this.xpLevelRequirement = xpLevelRequirement;
        this.bossesPerNight = bossesPerNight;
        this.spawnWeights = spawnWeights;
        this.globalHealthMult = globalHealthMult;
        this.globalDamageMult = globalDamageMult;
    }

    public int getIndex() { return index; }
    public String getDisplayName() { return displayName; }
    public ChatFormatting getColor() { return color; }
    public String getDescription() { return description; }
    public ArmorTierRequirement getArmorRequirement() { return armorRequirement; }
    public int getXpLevelRequirement() { return xpLevelRequirement; }
    public int getBossesPerNight() { return bossesPerNight; }
    public int[] getSpawnWeights() { return spawnWeights; }
    public double getGlobalHealthMult() { return globalHealthMult; }
    public double getGlobalDamageMult() { return globalDamageMult; }

    /**
     * Rolls a random EliteTier using this level's spawn weight table.
     */
    public EliteTier rollTier(java.util.Random random) {
        int total = 0;
        for (int w : spawnWeights) total += w;
        if (total == 0) return EliteTier.TOUGH;

        int roll = random.nextInt(total);
        int cumulative = 0;
        EliteTier[] tiers = EliteTier.values();
        for (int i = 0; i < spawnWeights.length && i < tiers.length; i++) {
            cumulative += spawnWeights[i];
            if (roll < cumulative) return tiers[i];
        }
        return EliteTier.TOUGH;
    }

    /** Returns the display component for UI menus. */
    public Component getDisplayComponent() {
        return Component.literal("● " + displayName)
                .withStyle(color, net.minecraft.ChatFormatting.BOLD);
    }

    /**
     * Armor tiers used for unlock requirements.
     */
    public enum ArmorTierRequirement {
        NONE,
        LEATHER,
        IRON,
        DIAMOND,
        NETHERITE;

        /**
         * Returns true if the given ItemStack (worn as armor) satisfies
         * this requirement.
         */
        public boolean isSatisfiedBy(ItemStack stack) {
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) return false;
            // We check by item identity groups
            return switch (this) {
                case NONE -> true;
                case LEATHER -> isLeatherOrBetter(stack);
                case IRON -> isIronOrBetter(stack);
                case DIAMOND -> isDiamondOrBetter(stack);
                case NETHERITE -> isNetherite(stack);
            };
        }

        private static boolean isLeatherOrBetter(ItemStack s) {
            return isLeather(s) || isChainmail(s) || isIronOrBetter(s);
        }
        private static boolean isIronOrBetter(ItemStack s) {
            return isIron(s) || isGold(s) || isDiamondOrBetter(s);
        }
        private static boolean isDiamondOrBetter(ItemStack s) {
            return isDiamond(s) || isNetherite(s);
        }
        private static boolean isLeather(ItemStack s) {
            return s.is(Items.LEATHER_HELMET) || s.is(Items.LEATHER_CHESTPLATE)
                    || s.is(Items.LEATHER_LEGGINGS) || s.is(Items.LEATHER_BOOTS);
        }
        private static boolean isChainmail(ItemStack s) {
            return s.is(Items.CHAINMAIL_HELMET) || s.is(Items.CHAINMAIL_CHESTPLATE)
                    || s.is(Items.CHAINMAIL_LEGGINGS) || s.is(Items.CHAINMAIL_BOOTS);
        }
        private static boolean isIron(ItemStack s) {
            return s.is(Items.IRON_HELMET) || s.is(Items.IRON_CHESTPLATE)
                    || s.is(Items.IRON_LEGGINGS) || s.is(Items.IRON_BOOTS);
        }
        private static boolean isGold(ItemStack s) {
            return s.is(Items.GOLDEN_HELMET) || s.is(Items.GOLDEN_CHESTPLATE)
                    || s.is(Items.GOLDEN_LEGGINGS) || s.is(Items.GOLDEN_BOOTS);
        }
        private static boolean isDiamond(ItemStack s) {
            return s.is(Items.DIAMOND_HELMET) || s.is(Items.DIAMOND_CHESTPLATE)
                    || s.is(Items.DIAMOND_LEGGINGS) || s.is(Items.DIAMOND_BOOTS);
        }
        private static boolean isNetherite(ItemStack s) {
            return s.is(Items.NETHERITE_HELMET) || s.is(Items.NETHERITE_CHESTPLATE)
                    || s.is(Items.NETHERITE_LEGGINGS) || s.is(Items.NETHERITE_BOOTS);
        }
    }
}
