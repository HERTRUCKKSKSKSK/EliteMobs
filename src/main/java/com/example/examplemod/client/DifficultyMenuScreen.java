package com.example.examplemod.client;

import com.example.examplemod.DifficultyLevel;
import com.example.examplemod.EliteMobsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * The "Circle Selection" screen, opened with Ctrl+P (default keybind).
 *
 * Layout:
 *  ┌─────────────────────────────────────────┐
 *  │  ☠ SELECT YOUR CIRCLE ☠                │
 *  │  ════════════════════════════           │
 *  │  [● LIMBO]     unlocked                 │
 *  │  [● LUST]      unlocked / LOCKED        │
 *  │  ...                                    │
 *  │  [Close]                                │
 *  └─────────────────────────────────────────┘
 *
 * Buttons are greyed out for locked levels. Clicking an unlocked level
 * sends a packet to the server to set it as the active difficulty.
 */
public class DifficultyMenuScreen extends Screen {

    private static final int PANEL_W = 280;
    private static final int PANEL_H = 320;
    private static final int BTN_W   = 240;
    private static final int BTN_H   = 22;
    private static final int PADDING = 6;

    // Mirrors what the server told us about unlocked levels
    private final boolean[] unlockedLevels;
    private final int activeLevelOrdinal;

    private final List<Button> levelButtons = new ArrayList<>();

    public DifficultyMenuScreen(boolean[] unlockedLevels, int activeLevelOrdinal) {
        super(Component.literal("☠ SELECT YOUR CIRCLE ☠"));
        this.unlockedLevels = unlockedLevels;
        this.activeLevelOrdinal = activeLevelOrdinal;
    }

    @Override
    protected void init() {
        levelButtons.clear();

        int panelX = (width - PANEL_W) / 2;
        int panelY = (height - PANEL_H) / 2;
        int startY = panelY + 50; // below the title

        DifficultyLevel[] levels = DifficultyLevel.values();

        for (int i = 0; i < levels.length; i++) {
            DifficultyLevel lvl = levels[i];
            boolean unlocked = i < unlockedLevels.length && unlockedLevels[i];
            boolean active   = (i == activeLevelOrdinal);

            int btnX = panelX + (PANEL_W - BTN_W) / 2;
            int btnY = startY + i * (BTN_H + PADDING);

            String label = buildLabel(lvl, unlocked, active, i);
            final int finalI = i;

            Button btn = Button.builder(Component.literal(label), b -> {
                if (unlocked) {
                    // Send packet to server
                    ClientPacketHandler.sendSetDifficulty(finalI);
                    onClose();
                }
            }).bounds(btnX, btnY, BTN_W, BTN_H).build();

            btn.active = unlocked;
            this.addRenderableWidget(btn);
            levelButtons.add(btn);
        }

        // Close button
        int closeBtnY = startY + levels.length * (BTN_H + PADDING) + 4;
        this.addRenderableWidget(Button.builder(
                Component.literal("§7[ Close ]"),
                b -> onClose()
        ).bounds(panelX + (PANEL_W - 100) / 2, closeBtnY, 100, 20).build());
    }

    private static String buildLabel(DifficultyLevel lvl, boolean unlocked, boolean active, int i) {
        String circle = "Circle " + toRoman(i + 1) + " — ";
        if (!unlocked) {
            return "§8🔒 " + circle + lvl.getDisplayName()
                    + " §8[" + requirementHint(lvl) + "]";
        }
        String prefix = active ? "§l▶ " : "   ";
        return prefix + lvl.getColor() + "● " + circle + lvl.getDisplayName()
                + (active ? " §e§l[ACTIVE]" : "");
    }

    private static String requirementHint(DifficultyLevel lvl) {
        String armor = switch (lvl.getArmorRequirement()) {
            case NONE      -> "";
            case LEATHER   -> "Leather";
            case IRON      -> "Iron";
            case DIAMOND   -> "Diamond";
            case NETHERITE -> "Netherite";
        };
        String xp = lvl.getXpLevelRequirement() > 0
                ? "Lvl " + lvl.getXpLevelRequirement()
                : "";
        if (armor.isEmpty() && xp.isEmpty()) return "Always unlocked";
        if (armor.isEmpty()) return xp;
        if (xp.isEmpty()) return armor + " armor";
        return armor + " armor + " + xp;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background overlay
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int panelX = (width - PANEL_W) / 2;
        int panelY = (height - PANEL_H) / 2;

        // Dark panel background
        graphics.fill(panelX - 2, panelY - 2, panelX + PANEL_W + 2, panelY + PANEL_H + 2,
                0xFF111111);
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H,
                0xCC220000);

        // Border lines (dark red)
        graphics.hLine(panelX, panelX + PANEL_W, panelY,      0xFF880000);
        graphics.hLine(panelX, panelX + PANEL_W, panelY + PANEL_H, 0xFF880000);
        graphics.vLine(panelX,           panelY, panelY + PANEL_H, 0xFF880000);
        graphics.vLine(panelX + PANEL_W, panelY, panelY + PANEL_H, 0xFF880000);

        // Title
        String title = "☠  SELECT YOUR CIRCLE  ☠";
        int titleWidth = this.font.width(title);
        graphics.drawString(this.font, "§4§l" + title,
                (width - titleWidth) / 2, panelY + 10, 0xFFFFFFFF, false);

        // Subtitle
        String sub = "Dante's Inferno Progression";
        int subWidth = this.font.width(sub);
        graphics.drawString(this.font, "§8" + sub,
                (width - subWidth) / 2, panelY + 24, 0xFFAAAAAA, false);

        // Separator
        graphics.hLine(panelX + 10, panelX + PANEL_W - 10, panelY + 35, 0xFF550000);

        // Render all widgets (buttons etc.)
        super.render(graphics, mouseX, mouseY, partialTick);

        // Tooltip on hover for locked buttons
        DifficultyLevel[] levels = DifficultyLevel.values();
        for (int i = 0; i < levelButtons.size() && i < levels.length; i++) {
            Button btn = levelButtons.get(i);
            if (!btn.active && btn.isHovered()) {
                DifficultyLevel lvl = levels[i];
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("§cLocked — Requirements:"));
                tooltip.add(Component.literal("§7• " + requirementHint(lvl)));
                tooltip.add(Component.literal(lvl.getDescription()));
                graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
            if (btn.active && btn.isHovered()) {
                DifficultyLevel lvl = levels[i];
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal(lvl.getDescription()));
                tooltip.add(Component.literal("§7Bosses per night: §a" + lvl.getBossesPerNight()));
                tooltip.add(buildWeightTooltip(lvl));
                graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
        }
    }

    private Component buildWeightTooltip(DifficultyLevel lvl) {
        int[] w = lvl.getSpawnWeights();
        return Component.literal(String.format(
                "§7Tough §a%d%% §7Elite §6%d%% §7Champ §b%d%% §7Bsrk §c%d%% §7Inf §5%d%%",
                w[0], w[1], w[2], w[3], w.length > 4 ? w[4] : 0));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // Roman numeral helper (I–IX)
    private static String toRoman(int n) {
        return new String[]{"I","II","III","IV","V","VI","VII","VIII","IX"}[Math.max(0, Math.min(8, n-1))];
    }
}
