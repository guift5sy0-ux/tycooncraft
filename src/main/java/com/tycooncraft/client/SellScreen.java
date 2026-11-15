package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.economy.Coins;
import com.tycooncraft.economy.SellMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 판매소 창: 왼쪽 판매 칸, 오른쪽 금액 표시판, [판매] [인출] 버튼 */
public class SellScreen extends AbstractContainerScreen<SellMenu> {
    private static final ResourceLocation BG = TycoonCraft.id("textures/gui/sell_station.png");
    private static final int LCD_GREEN = 0xFF5CFF8C, LCD_DIM = 0xFF3CA060;

    public SellScreen(SellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.tycooncraft.sell.sell"), b -> click(SellMenu.BUTTON_SELL))
                .bounds(leftPos + 72, topPos + 52, 46, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tycooncraft.sell.withdraw"), b -> click(SellMenu.BUTTON_WITHDRAW))
                .bounds(leftPos + 122, topPos + 52, 46, 18).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, Component.translatable("gui.tycooncraft.sell.balance"), 76, 20, LCD_DIM, false);
        String bal = "₩" + Coins.format(menu.balance());
        g.drawString(font, bal, 164 - font.width(bal), 20, LCD_GREEN, false);
        g.drawString(font, Component.translatable("gui.tycooncraft.sell.pending"), 76, 35, LCD_DIM, false);
        String pend = "₩" + Coins.format(menu.pending());
        g.drawString(font, pend, 164 - font.width(pend), 35, LCD_GREEN, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
