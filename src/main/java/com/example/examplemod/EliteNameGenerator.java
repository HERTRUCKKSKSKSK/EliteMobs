package com.example.examplemod;

import java.util.Random;

/**
 * Generates flavorful random names for elite mobs, following the pattern:
 * "[Adjective] [Mob Name] of the [Place/Theme]"
 *
 * Examples: "Heavy Zombie of the Cave", "Backbreaking Skeleton of the Nightmares",
 * "Withered Witch of the Swamp".
 */
public class EliteNameGenerator {

    private static final Random RANDOM = new Random();

    private static final String[] ADJECTIVES = {
            "Heavy", "Backbreaking", "Withered", "Savage", "Ruthless", "Grim",
            "Cursed", "Wretched", "Vile", "Bloodthirsty", "Merciless", "Feral",
            "Ancient", "Forsaken", "Hollow", "Dreadful", "Venomous", "Rotten",
            "Frenzied", "Brutal", "Sinister", "Wicked", "Shattered", "Howling",
            "Frostbitten", "Smoldering", "Corrupted", "Restless", "Unholy", "Gnarled"
    };

    private static final String[] PLACES = {
            "Cave", "Nightmares", "Swamp", "Abyss", "Wastes", "Void",
            "Deep", "Crypt", "Shadows", "Ruins", "Wilds", "Mist",
            "Frozen Peaks", "Forgotten Lands", "Catacombs", "Underworld",
            "Burning Plains", "Dark Forest", "Endless Night", "Hollow Earth",
            "Lost Realm", "Blood Moon", "Ashen Fields", "Silent Hills"
    };

    /**
     * Builds a random flavor name for the given mob's base name, e.g.
     * given "Zombie" might return "Heavy Zombie of the Cave".
     */
    public static String generate(String baseMobName) {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String place = PLACES[RANDOM.nextInt(PLACES.length)];
        return adjective + " " + baseMobName + " of the " + place;
    }
}