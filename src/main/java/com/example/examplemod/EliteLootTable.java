package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * RPG-style loot generator. The higher the DifficultyLevel + EliteTier,
 * the more overpowered, enchanted, and unique the drops.
 *
 * Loot tiers (cross-referencing EliteTier.lootLevel):
 *   0 = TOUGH   → basic iron/gold gear
 *   1 = ELITE   → diamond gear, random enchants
 *   2 = CHAMPION → diamond/netherite, strong enchants + bonus xp
 *   3 = BERSERKER → netherite, max enchants, rare bonus drops
 *   4 = INFERNAL (Lucifer) → Legendary infernal weapons/armor, max everything
 *
 * NOTE (1.20.5+): NBT-based ItemStack data (CompoundTag, getOrCreateTag(),
 * setHoverName(), raw "Enchantments" ListTag) was removed entirely and
 * replaced by Data Components. Everything below uses:
 *   - DataComponents.CUSTOM_NAME  for names
 *   - DataComponents.LORE         for lore
 *   - DataComponents.ENCHANTMENTS for enchantments (via the Enchantment registry)
 */
public class EliteLootTable {

    private static final Random RANDOM = new Random();

    // ──────────────────────────────────────────────────────────────────────
    // ENTRY POINT
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Generates and gives loot + xp to the player who killed the elite.
     */
    public static void grantLoot(ServerPlayer player, PathfinderMob mob,
                                 EliteTier tier, DifficultyLevel diffLevel) {

        ServerLevel level = player.serverLevel();
        List<ItemStack> drops = generateDrops(level, tier, diffLevel);

        for (ItemStack drop : drops) {
            if (!player.addItem(drop)) {
                // Inventario lleno — soltar a los pies del jugador
                player.drop(drop, false);
            }
        }

        int xpGrant = calculateXp(tier, diffLevel);
        player.giveExperiencePoints(xpGrant);

        sendKillMessage(player, tier, diffLevel, xpGrant);
    }

    // ──────────────────────────────────────────────────────────────────────
    // XP CALCULATION
    // ──────────────────────────────────────────────────────────────────────

    private static int calculateXp(EliteTier tier, DifficultyLevel diff) {
        int base = switch (tier) {
            case TOUGH     -> 15;
            case ELITE     -> 40;
            case CHAMPION  -> 100;
            case BERSERKER -> 250;
            case INFERNAL  -> 600;
        };
        double multiplier = 1.0 + diff.getIndex() * 0.5;
        return (int) (base * multiplier);
    }

    // ──────────────────────────────────────────────────────────────────────
    // DROP GENERATION
    // ──────────────────────────────────────────────────────────────────────

    private static List<ItemStack> generateDrops(ServerLevel level, EliteTier tier, DifficultyLevel diff) {
        List<ItemStack> result = new ArrayList<>();
        int lootLevel = tier.getLootLevel();
        int diffIdx = diff.getIndex();

        switch (lootLevel) {

            // ── TOUGH (loot 0) ──────────────────────────────────────────
            case 0 -> {
                if (RANDOM.nextFloat() < 0.70f) {
                    result.add(new ItemStack(RANDOM.nextBoolean() ? Items.IRON_SWORD : Items.IRON_CHESTPLATE));
                }
                result.add(new ItemStack(Items.COOKED_BEEF, 2 + RANDOM.nextInt(4)));
            }

            // ── ELITE (loot 1) ──────────────────────────────────────────
            case 1 -> {
                ItemStack weapon = new ItemStack(Items.DIAMOND_SWORD);
                enchantStack(level, weapon, true, 2, 3);
                result.add(weapon);

                if (RANDOM.nextFloat() < 0.4f) {
                    ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                    enchantStack(level, chest, false, 1, 2);
                    result.add(chest);
                }
                result.add(new ItemStack(Items.GOLD_INGOT, 2 + RANDOM.nextInt(6)));
                result.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 3 + RANDOM.nextInt(5)));
            }

            // ── CHAMPION (loot 2) ────────────────────────────────────────
            case 2 -> {
                ItemStack weapon = new ItemStack(
                        RANDOM.nextBoolean() ? Items.NETHERITE_SWORD : Items.DIAMOND_SWORD);
                enchantStack(level, weapon, true, 3, 5);
                setCustomName(weapon, "§b✦ Champion's Blade ✦", ChatFormatting.AQUA, ChatFormatting.BOLD);
                result.add(weapon);

                if (RANDOM.nextFloat() < 0.55f) {
                    ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
                    enchantStack(level, boots, false, 2, 4);
                    result.add(boots);
                }
                result.add(new ItemStack(Items.NETHERITE_SCRAP, 1 + RANDOM.nextInt(3)));
                result.add(new ItemStack(Items.DIAMOND, 3 + RANDOM.nextInt(6)));
                result.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 6 + RANDOM.nextInt(8)));
            }

            // ── BERSERKER (loot 3) ──────────────────────────────────────
            case 3 -> {
                ItemStack berserkSword = new ItemStack(Items.NETHERITE_SWORD);
                enchantStack(level, berserkSword, true, 5, 6);
                setCustomName(berserkSword, "§4⚔ Berserker's Wrath ⚔", ChatFormatting.DARK_RED, ChatFormatting.BOLD);
                result.add(berserkSword);

                if (RANDOM.nextFloat() < 0.30f) {
                    result.addAll(fullNetheriteArmor(level, 4, 5));
                } else {
                    List<ItemStack> armor = fullNetheriteArmor(level, 3, 4);
                    result.add(armor.get(RANDOM.nextInt(armor.size())));
                    result.add(armor.get(RANDOM.nextInt(armor.size())));
                }

                result.add(new ItemStack(Items.NETHERITE_INGOT, 1 + RANDOM.nextInt(3)));
                result.add(new ItemStack(Items.DIAMOND, 5 + RANDOM.nextInt(10)));
                result.add(new ItemStack(Items.GOLDEN_APPLE, 1 + RANDOM.nextInt(2)));
                result.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 10 + RANDOM.nextInt(12)));
            }

            // ── INFERNAL / LUCIFER (loot 4) ──────────────────────────────
            case 4 -> result.addAll(generateInfernalLoot(level, diff));
        }

        if (diffIdx >= 6 && RANDOM.nextFloat() < 0.25f) {
            result.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        }
        if (diffIdx >= 7 && RANDOM.nextFloat() < 0.15f) {
            result.add(new ItemStack(Items.TOTEM_OF_UNDYING));
        }

        return result;
    }

    // ──────────────────────────────────────────────────────────────────────
    // INFERNAL LOOT (special legendary items)
    // ──────────────────────────────────────────────────────────────────────

    private static List<ItemStack> generateInfernalLoot(ServerLevel level, DifficultyLevel diff) {
        List<ItemStack> drops = new ArrayList<>();
        int diffIdx = diff.getIndex();

        drops.add(buildSatanSword(level));

        float armorChance = 0.35f + diffIdx * 0.07f;
        if (RANDOM.nextFloat() < armorChance) {
            drops.addAll(buildInfernalArmor(level));
        } else {
            List<ItemStack> infArmor = buildInfernalArmor(level);
            drops.add(infArmor.get(RANDOM.nextInt(infArmor.size())));
        }

        if (RANDOM.nextFloat() < 0.5f) drops.add(new ItemStack(Items.TOTEM_OF_UNDYING));

        drops.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1 + RANDOM.nextInt(3)));
        drops.add(new ItemStack(Items.NETHERITE_INGOT, 2 + RANDOM.nextInt(4)));
        drops.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 15 + RANDOM.nextInt(20)));

        return drops;
    }

    /**
     * "Lucifer's Judgement" — espada netherite con encantamientos al máximo y nombre/lore custom.
     */
    private static ItemStack buildSatanSword(ServerLevel level) {
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
        setCustomName(sword, "§4§l☠ Lucifer's Judgement ☠", ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);

        addUnsafeEnchantByName(level, sword, "sharpness", 5);
        addUnsafeEnchantByName(level, sword, "fire_aspect", 2);
        addUnsafeEnchantByName(level, sword, "looting", 3);
        addUnsafeEnchantByName(level, sword, "knockback", 2);
        addUnsafeEnchantByName(level, sword, "sweeping_edge", 3);
        addUnsafeEnchantByName(level, sword, "unbreaking", 3);
        addUnsafeEnchantByName(level, sword, "mending", 1);

        addLore(sword,
                "§7Forged in the deepest fires of Hell.",
                "§7The last thing many adventurers have seen.",
                "§d§lLegendary — Infernal Tier"
        );

        return sword;
    }

    /**
     * Set completo de armadura Infernal Netherite con encantamientos al máximo.
     */
    private static List<ItemStack> buildInfernalArmor(ServerLevel level) {
        List<ItemStack> armor = new ArrayList<>();

        ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
        setCustomName(helmet, "§5§l☠ Hellfire Crown ☠", ChatFormatting.DARK_PURPLE);
        addUnsafeEnchantByName(level, helmet, "protection", 4);
        addUnsafeEnchantByName(level, helmet, "fire_protection", 4);
        addUnsafeEnchantByName(level, helmet, "blast_protection", 4);
        addUnsafeEnchantByName(level, helmet, "projectile_protection", 4);
        addUnsafeEnchantByName(level, helmet, "respiration", 3);
        addUnsafeEnchantByName(level, helmet, "aqua_affinity", 1);
        addUnsafeEnchantByName(level, helmet, "thorns", 3);
        addUnsafeEnchantByName(level, helmet, "unbreaking", 3);
        addUnsafeEnchantByName(level, helmet, "mending", 1);
        addLore(helmet, "§7Worn by fallen angels.", "§d§lLegendary — Infernal Tier");
        armor.add(helmet);

        ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
        setCustomName(chest, "§5§l☠ Breastplate of Damnation ☠", ChatFormatting.DARK_PURPLE);
        addUnsafeEnchantByName(level, chest, "protection", 4);
        addUnsafeEnchantByName(level, chest, "fire_protection", 4);
        addUnsafeEnchantByName(level, chest, "blast_protection", 4);
        addUnsafeEnchantByName(level, chest, "thorns", 3);
        addUnsafeEnchantByName(level, chest, "unbreaking", 3);
        addUnsafeEnchantByName(level, chest, "mending", 1);
        addLore(chest, "§7Heat cannot touch the damned.", "§d§lLegendary — Infernal Tier");
        armor.add(chest);

        ItemStack legs = new ItemStack(Items.NETHERITE_LEGGINGS);
        setCustomName(legs, "§5§l☠ Leggings of Perdition ☠", ChatFormatting.DARK_PURPLE);
        addUnsafeEnchantByName(level, legs, "protection", 4);
        addUnsafeEnchantByName(level, legs, "fire_protection", 4);
        addUnsafeEnchantByName(level, legs, "swift_sneak", 3);
        addUnsafeEnchantByName(level, legs, "unbreaking", 3);
        addUnsafeEnchantByName(level, legs, "mending", 1);
        addLore(legs, "§7They walked through the Ninth Circle.", "§d§lLegendary — Infernal Tier");
        armor.add(legs);

        ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
        setCustomName(boots, "§5§l☠ Boots of the Abyss ☠", ChatFormatting.DARK_PURPLE);
        addUnsafeEnchantByName(level, boots, "protection", 4);
        addUnsafeEnchantByName(level, boots, "fire_protection", 4);
        addUnsafeEnchantByName(level, boots, "feather_falling", 4);
        addUnsafeEnchantByName(level, boots, "depth_strider", 3);
        addUnsafeEnchantByName(level, boots, "soul_speed", 3);
        addUnsafeEnchantByName(level, boots, "frost_walker", 2);
        addUnsafeEnchantByName(level, boots, "thorns", 3);
        addUnsafeEnchantByName(level, boots, "unbreaking", 3);
        addUnsafeEnchantByName(level, boots, "mending", 1);
        addLore(boots, "§7Silence follows every step.", "§d§lLegendary — Infernal Tier");
        armor.add(boots);

        return armor;
    }

    // ──────────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────────

    private static List<ItemStack> fullNetheriteArmor(ServerLevel level, int minEnchants, int maxEnchants) {
        List<ItemStack> list = new ArrayList<>();
        list.add(enchantStack(level, new ItemStack(Items.NETHERITE_HELMET), false, minEnchants, maxEnchants));
        list.add(enchantStack(level, new ItemStack(Items.NETHERITE_CHESTPLATE), false, minEnchants, maxEnchants));
        list.add(enchantStack(level, new ItemStack(Items.NETHERITE_LEGGINGS), false, minEnchants, maxEnchants));
        list.add(enchantStack(level, new ItemStack(Items.NETHERITE_BOOTS), false, minEnchants, maxEnchants));
        return list;
    }

    /**
     * Aplica encantamientos aleatorios de una lista predefinida.
     */
    private static ItemStack enchantStack(ServerLevel level, ItemStack stack, boolean weapon,
                                          int minEnchants, int maxEnchants) {
        String[] weaponEnchs = {"sharpness", "fire_aspect", "looting", "knockback", "sweeping_edge",
                "unbreaking", "mending"};
        String[] armorEnchs = {"protection", "fire_protection", "blast_protection", "unbreaking",
                "mending", "thorns", "feather_falling"};
        String[] pool = weapon ? weaponEnchs : armorEnchs;

        int rolls = minEnchants + RANDOM.nextInt(Math.max(1, maxEnchants - minEnchants + 1));

        Set<String> used = new HashSet<>();
        for (int i = 0; i < rolls; i++) {
            String enchId = pool[RANDOM.nextInt(pool.length)];
            if (used.add(enchId)) {
                int lvl = 1 + RANDOM.nextInt(weapon ? 5 : 4);
                addUnsafeEnchantByName(level, stack, enchId, lvl);
            }
        }
        return stack;
    }

    /**
     * Añade un encantamiento por su ID vanilla (ej. "sharpness") usando el
     * Data Component ENCHANTMENTS. Reemplaza el nivel si ya existía.
     * "unsafe" en el sentido de que no respeta isPrimaryItemFor/canEnchant.
     */
    private static void addUnsafeEnchantByName(ServerLevel level, ItemStack stack, String enchId, int lvl) {
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

    private static void addLore(ItemStack stack, String... lines) {
        List<Component> loreLines = new ArrayList<>();
        for (String line : lines) {
            loreLines.add(Component.literal(line));
        }
        stack.set(DataComponents.LORE, new ItemLore(loreLines));
    }

    private static void setCustomName(ItemStack stack, String text, ChatFormatting... styles) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(text).withStyle(styles));
    }

    // ──────────────────────────────────────────────────────────────────────
    // KILL MESSAGE
    // ──────────────────────────────────────────────────────────────────────

    private static void sendKillMessage(ServerPlayer player, EliteTier tier, DifficultyLevel diff, int xp) {
        String tierName = tier.isInfernal() ? "§5§l☠ LUCIFER ☠" : "§" + colorCode(tier) + tier.getDisplayName();
        String msg = String.format("§7You slew a %s §7[%s§7] and gained §a+%d XP§7!",
                tierName, diff.getColor() + diff.getDisplayName(), xp);
        player.sendSystemMessage(Component.literal(msg));
    }

    private static char colorCode(EliteTier tier) {
        return switch (tier) {
            case TOUGH -> 'a';
            case ELITE -> '6';
            case CHAMPION -> 'b';
            case BERSERKER -> '4';
            case INFERNAL -> '5';
        };
    }
}