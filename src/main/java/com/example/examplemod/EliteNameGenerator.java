package com.example.examplemod;

import java.util.Random;

/**
 * Generates thematic names for elite mobs, scaled to DifficultyLevel and EliteTier.
 *
 * Pattern: "[Adjective] [MobName] of the [Place]"
 * For INFERNAL tier at TREACHERY level, special Dante-themed names are chosen.
 */
public class EliteNameGenerator {

    private static final Random RANDOM = new Random();

    // ──────────────────────────────────────────────────────────────────────
    // Name pools (expand per level)
    // ──────────────────────────────────────────────────────────────────────

    private static final String[] LIMBO_ADJECTIVES = {
            "Restless", "Wandering", "Lost", "Pale", "Hollow"
    };
    private static final String[] LUST_ADJECTIVES = {
            "Burning", "Frenzied", "Craving", "Enraged", "Obsessed"
    };
    private static final String[] GLUTTONY_ADJECTIVES = {
            "Devouring", "Gorging", "Bloated", "Insatiable", "Ravenous"
    };
    private static final String[] GREED_ADJECTIVES = {
            "Hoarding", "Corrupt", "Gold-Stained", "Grasping", "Avaricious"
    };
    private static final String[] ANGER_ADJECTIVES = {
            "Wrathful", "Seething", "Furious", "Bloodthirsty", "Raging"
    };
    private static final String[] HERESY_ADJECTIVES = {
            "Blasphemous", "Forsaken", "Heretical", "Cursed", "Apostate"
    };
    private static final String[] VIOLENCE_ADJECTIVES = {
            "Savage", "Merciless", "Gore-Soaked", "Brutal", "Slaughtering"
    };
    private static final String[] FRAUD_ADJECTIVES = {
            "Treacherous", "Deceiving", "Shadow-Born", "Insidious", "Masked"
    };
    private static final String[] TREACHERY_ADJECTIVES = {
            "Damned", "Doomed", "Eternal", "Accursed", "Satanic",
            "Hellbound", "Abyssal", "Infernal", "Soul-Devouring", "Annihilating"
    };

    // General fallback
    private static final String[] GENERAL_ADJECTIVES = {
            "Heavy", "Backbreaking", "Withered", "Savage", "Ruthless", "Grim",
            "Cursed", "Wretched", "Vile", "Merciless", "Feral",
            "Ancient", "Forsaken", "Dreadful", "Venomous", "Corrupted",
            "Frenzied", "Brutal", "Sinister", "Wicked", "Howling",
            "Smoldering", "Restless", "Unholy", "Gnarled", "Soulless"
    };

    // Places — scaled by tier
    private static final String[] LIMBO_PLACES = {
            "Void", "Mist", "Between", "Pale Fields", "Forgotten Gate"
    };
    private static final String[] INFERNAL_PLACES = {
            "Ninth Circle", "Eternal Darkness", "Satan's Throne", "Abyss of Souls",
            "Lake of Ice", "Judecca", "Ptolomaea", "Antenora", "Caina"
    };
    private static final String[] GENERAL_PLACES = {
            "Cave", "Nightmares", "Swamp", "Abyss", "Wastes", "Void",
            "Deep", "Crypt", "Shadows", "Ruins", "Wilds", "Mist",
            "Frozen Peaks", "Forgotten Lands", "Catacombs", "Underworld",
            "Burning Plains", "Dark Forest", "Endless Night", "Hollow Earth",
            "Lost Realm", "Blood Moon", "Ashen Fields", "Silent Hills",
            "Ninth Circle", "Hellfire", "Scorched Throne", "Eternal Damnation"
    };

    // Special names for INFERNAL (Lucifer) — these override the adjective+mob+place format
    private static final String[] LUCIFER_NAMES = {
            "Lucifer, Lord of Treachery",
            "Satan, Prince of Hell",
            "Beelzebub, The Fallen Morning Star",
            "Mephistopheles, Devourer of Souls",
            "Belial, The Worthless One",
            "Asmodeus, The Destroyer",
            "Mammon, The Eternal Corruptor",
            "Belphegor, Duke of the Abyss"
    };

    // ──────────────────────────────────────────────────────────────────────
    // GENERATION
    // ──────────────────────────────────────────────────────────────────────

    /** Legacy overload for backwards compatibility with EliteCommand. */
    public static String generate(String baseMobName) {
        return generate(baseMobName, EliteTier.TOUGH, DifficultyLevel.LIMBO);
    }

    /**
     * Generates a contextual name based on mob type, tier, and difficulty level.
     */
    public static String generate(String baseMobName, EliteTier tier, DifficultyLevel diff) {
        // INFERNAL tier gets legendary names
        if (tier == EliteTier.INFERNAL) {
            return LUCIFER_NAMES[RANDOM.nextInt(LUCIFER_NAMES.length)];
        }

        String adjective = getAdjective(diff);
        String place     = getPlace(diff);

        return adjective + " " + baseMobName + " of the " + place;
    }

    private static String getAdjective(DifficultyLevel diff) {
        String[] pool = switch (diff) {
            case LIMBO     -> LIMBO_ADJECTIVES;
            case LUST      -> LUST_ADJECTIVES;
            case GLUTTONY  -> GLUTTONY_ADJECTIVES;
            case GREED     -> GREED_ADJECTIVES;
            case ANGER     -> ANGER_ADJECTIVES;
            case HERESY    -> HERESY_ADJECTIVES;
            case VIOLENCE  -> VIOLENCE_ADJECTIVES;
            case FRAUD     -> FRAUD_ADJECTIVES;
            case TREACHERY -> TREACHERY_ADJECTIVES;
        };
        return pool[RANDOM.nextInt(pool.length)];
    }

    private static String getPlace(DifficultyLevel diff) {
        if (diff == DifficultyLevel.LIMBO) {
            return LIMBO_PLACES[RANDOM.nextInt(LIMBO_PLACES.length)];
        }
        if (diff == DifficultyLevel.TREACHERY) {
            return INFERNAL_PLACES[RANDOM.nextInt(INFERNAL_PLACES.length)];
        }
        return GENERAL_PLACES[RANDOM.nextInt(GENERAL_PLACES.length)];
    }
}
