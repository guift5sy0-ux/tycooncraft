package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.machine.MachineMenu;
import com.tycooncraft.machine.MachineRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** 기계 창: 재료 6칸 → 진행 화살표 → 결과 칸. 화살표에 마우스를 올리면 레시피 목록 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final ResourceLocation BG = TycoonCraft.id("textures/gui/machine.png");
    private static final int ARROW_X = 90, ARROW_Y = 36, ARROW_W = 26, ARROW_H = 16;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int w = menu.progressScaled(ARROW_W);
        if (w > 0) {
            int x = leftPos + ARROW_X, y = topPos + ARROW_Y;
            g.fill(x, y + 5, x + Math.min(w, 18), y + 11, 0xFF4CAF50);
            if (w > 18) {
                // 화살표 머리 (삼각형을 세로 줄로 채움)
                for (int i = 0; i < w - 18; i++) {
                    int half = 7 - i;
                    if (half <= 0) break;
                    g.fill(x + 18 + i, y + 8 - half, x + 19 + i, y + 8 + half, 0xFF4CAF50);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (isHovering(ARROW_X, ARROW_Y, ARROW_W, ARROW_H, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.tycooncraft.recipes").withStyle(ChatFormatting.GOLD));
            for (MachineRecipe r : menu.type().recipes()) lines.add(r.describe().copy().withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
}
