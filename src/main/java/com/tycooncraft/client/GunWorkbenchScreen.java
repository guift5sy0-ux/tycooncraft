package com.tycooncraft.client;

import com.tycooncraft.gun.GunCrafting;
import com.tycooncraft.gun.GunWorkbenchMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 총기 제작대 화면: 작업대 격자 + 왼쪽에 제작법 목록 (마우스를 올리면 재료 표시) */
public class GunWorkbenchScreen extends AbstractContainerScreen<GunWorkbenchMenu> {
    private static final ResourceLocation BG = ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private static final int PANEL_W = 96, COLS = 4;

    public GunWorkbenchScreen(GunWorkbenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth + PANEL_W) / 2;
        titleLabelX = 29;
    }

    private int panelX() {
        return leftPos - PANEL_W - 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int px = panelX(), py = topPos;
        g.fill(px, py, px + PANEL_W, py + imageHeight, 0xFF8B8B8B);
        g.fill(px + 1, py + 1, px + PANEL_W - 1, py + imageHeight - 1, 0xFFC6C6C6);
        g.drawString(font, Component.translatable("gui.tycooncraft.gun_workbench.recipes"), px + 6, py + 6, 0x404040, false);
        List<GunCrafting.Recipe> all = GunCrafting.all();
        for (int i = 0; i < all.size(); i++) {
            int x = px + 6 + (i % COLS) * 21, y = py + 18 + (i / COLS) * 21;
            g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
            g.renderItem(all.get(i).result(), x + 1, y + 1);
            g.renderItemDecorations(font, all.get(i).result(), x + 1, y + 1);
        }
        g.drawWordWrap(font, Component.translatable("gui.tycooncraft.gun_workbench.hint"), px + 6, py + 18 + ((all.size() + COLS - 1) / COLS) * 21 + 4, PANEL_W - 10, 0x555555);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        List<GunCrafting.Recipe> all = GunCrafting.all();
        int px = panelX(), py = topPos;
        for (int i = 0; i < all.size(); i++) {
            int x = px + 6 + (i % COLS) * 21, y = py + 18 + (i / COLS) * 21;
            if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                g.renderComponentTooltip(font, lines(all.get(i)), mx, my);
                break;
            }
        }
    }

    public static List<Component> lines(GunCrafting.Recipe rc) {
        List<Component> l = new ArrayList<>();
        ItemStack out = rc.result();
        l.add(out.getHoverName().copy().withStyle(ChatFormatting.GOLD).append(Component.literal(out.getCount() > 1 ? " x" + out.getCount() : "").withStyle(ChatFormatting.GOLD)));
        l.add(Component.translatable("gui.tycooncraft.gun_workbench.needs").withStyle(ChatFormatting.GRAY));
        for (Map.Entry<Item, Integer> e : rc.needs().entrySet())
            l.add(Component.literal(" • ").append(new ItemStack(e.getKey()).getHoverName()).append(" x" + e.getValue()).withStyle(ChatFormatting.WHITE));
        return l;
    }
}
