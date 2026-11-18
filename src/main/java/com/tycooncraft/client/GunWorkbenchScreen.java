package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.gun.GunCrafting;
import com.tycooncraft.gun.GunWorkbenchMenu;
import com.tycooncraft.gun.GunWorkbenchPayload;
import com.tycooncraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 총기 제작대 화면.
 * 왼쪽: 분류 탭(총기·탄약·부품·파츠) + 제작법 목록 (재료가 다 있으면 밝게, 모자라면 붉게)
 * 제작법을 누르면 보관함에서 재료를 꺼내 격자를 채움 (Shift = 가능한 만큼), 격자가 비어 있으면 고른 제작법의 재료 배치를 흐리게 미리 보여 줌.
 */
public class GunWorkbenchScreen extends AbstractContainerScreen<GunWorkbenchMenu> {
    private static final ResourceLocation BG = TycoonCraft.id("textures/gui/gun_workbench.png");
    private static final int TEX_W = 512, TEX_H = 256;
    private static final int COLS = 5, ROWS = 6, CELL = 18, LIST_X = 9, LIST_Y = 44, TAB_Y = 18;
    private static final String[] TAB_KEYS = {"guns", "ammo", "components", "attachments"};
    public static int tab;
    public static int selected = -1;

    public GunWorkbenchScreen(GunWorkbenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 276;
        imageHeight = 200;
        inventoryLabelX = 110;
        inventoryLabelY = 107;
        titleLabelX = 8;
        titleLabelY = 6;
    }

    private static ItemStack tabIcon(int t) {
        return new ItemStack(switch (t) {
            case 0 -> ModItems.RIFLE.get();
            case 1 -> ModItems.LIGHT_AMMO.get();
            case 2 -> ModItems.GUN_RECEIVER.get();
            default -> ModItems.GUN_PART_ITEMS.get(com.tycooncraft.gun.GunPart.SCOPE).get();
        });
    }

    /** 현재 탭의 (전체 목록 기준 번호) 제작법들 */
    private static List<Integer> visible() {
        List<Integer> out = new ArrayList<>();
        var all = GunCrafting.all();
        for (int i = 0; i < all.size(); i++) if (GunCrafting.category(all.get(i)) == tab) out.add(i);
        return out;
    }

    /** 보관함 + 격자에 있는 재료 */
    private Map<Item, Integer> have() {
        Map<Item, Integer> m = new HashMap<>();
        if (minecraft == null || minecraft.player == null) return m;
        for (ItemStack s : minecraft.player.getInventory().items) if (!s.isEmpty()) m.merge(GunCrafting.key(s), s.getCount(), Integer::sum);
        for (int i = 0; i < menu.grid.getContainerSize(); i++) {
            ItemStack s = menu.grid.getItem(i);
            if (!s.isEmpty()) m.merge(GunCrafting.key(s), s.getCount(), Integer::sum);
        }
        return m;
    }

    private boolean gridEmpty() {
        for (int i = 0; i < menu.grid.getContainerSize(); i++) if (!menu.grid.getItem(i).isEmpty()) return false;
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.blit(BG, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        // 분류 탭
        for (int t = 0; t < 4; t++) {
            int tx = x + 7 + t * 24, ty = y + TAB_Y;
            g.blit(BG, tx, ty, t == tab ? 304 : 280, 0, 22, 22, TEX_W, TEX_H);
            g.renderItem(tabIcon(t), tx + 3, ty + 3);
        }
        // 제작법 목록
        Map<Item, Integer> have = have();
        List<Integer> list = visible();
        var all = GunCrafting.all();
        for (int k = 0; k < Math.min(list.size(), COLS * ROWS); k++) {
            int idx = list.get(k);
            int cx = x + LIST_X + (k % COLS) * CELL, cy = y + LIST_Y + (k / COLS) * CELL;
            boolean can = GunCrafting.sets(all.get(idx), have) > 0;
            g.blit(BG, cx, cy, idx == selected ? 300 : can ? 280 : 320, 24, 18, 18, TEX_W, TEX_H);
            g.renderItem(all.get(idx).result(), cx + 1, cy + 1);
            g.renderItemDecorations(font, all.get(idx).result(), cx + 1, cy + 1);
            if (!can) g.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0x88200A0C);
            else g.fill(cx + 14, cy + 1, cx + 16, cy + 3, 0xFF4FD86A);   // 만들 수 있음 표시
            if (mx >= cx && mx < cx + 18 && my >= cy && my < cy + 18) g.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0x30FFFFFF);
        }
        // 격자가 비어 있으면 고른 제작법 재료를 흐리게 미리 보기
        if (selected >= 0 && selected < all.size() && gridEmpty()) {
            int slot = 0;
            for (var e : all.get(selected).needs().entrySet()) {
                for (int n = 0; n < e.getValue() && slot < 9; n++, slot++) {
                    int sx = x + GunWorkbenchMenu.GRID_X + (slot % 3) * 18, sy = y + GunWorkbenchMenu.GRID_Y + (slot / 3) * 18;
                    g.renderItem(new ItemStack(e.getKey()), sx, sy);
                    g.fill(sx, sy, sx + 16, sy + 16, 0xA0424852);
                }
            }
            ItemStack out = all.get(selected).result();
            int rx = x + GunWorkbenchMenu.RESULT_X, ry = y + GunWorkbenchMenu.RESULT_Y;
            if (menu.getSlot(0).getItem().isEmpty()) {
                g.renderItem(out, rx, ry);
                g.fill(rx, ry, rx + 16, ry + 16, 0xA0424852);
            }
        }
        // 아래 정보칸
        int ix = x + 11, iy = y + 156;
        if (selected >= 0 && selected < all.size()) {
            GunCrafting.Recipe rc = all.get(selected);
            int sets = GunCrafting.sets(rc, have);
            g.drawString(font, font.plainSubstrByWidth(rc.result().getHoverName().getString(), 86), ix, iy, 0xFFF2C94C, false);
            g.drawString(font, sets > 0 ? Component.translatable("gui.tycooncraft.gun_workbench.can_make", sets) : Component.translatable("gui.tycooncraft.gun_workbench.missing"),
                    ix, iy + 11, sets > 0 ? 0xFF6FE38F : 0xFFFF7A6E, false);
            g.drawString(font, Component.translatable("gui.tycooncraft.gun_workbench.click_fill"), ix, iy + 23, 0xFF8A939E, false);
        } else {
            g.drawWordWrap(font, Component.translatable("gui.tycooncraft.gun_workbench.hint"), ix, iy, 86, 0xFF8A939E);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFE6E6E6, false);
        g.drawString(font, Component.translatable("gui.tycooncraft.gun_workbench.tab." + TAB_KEYS[tab]), 112, 9, 0xFFF2C94C, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB8C0CA, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        for (int t = 0; t < 4; t++) {
            int tx = leftPos + 7 + t * 24, ty = topPos + TAB_Y;
            if (mx >= tx && mx < tx + 22 && my >= ty && my < ty + 22)
                g.renderTooltip(font, Component.translatable("gui.tycooncraft.gun_workbench.tab." + TAB_KEYS[t]), mx, my);
        }
        int k = cellAt(mx, my);
        List<Integer> list = visible();
        if (k >= 0 && k < list.size()) g.renderComponentTooltip(font, lines(GunCrafting.all().get(list.get(k)), have()), mx, my);
    }

    private int cellAt(double mx, double my) {
        int lx = (int) Math.floor((mx - leftPos - LIST_X) / CELL), ly = (int) Math.floor((my - topPos - LIST_Y) / CELL);
        if (mx < leftPos + LIST_X || my < topPos + LIST_Y || lx < 0 || lx >= COLS || ly < 0 || ly >= ROWS) return -1;
        return ly * COLS + lx;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int t = 0; t < 4; t++) {
            int tx = leftPos + 7 + t * 24, ty = topPos + TAB_Y;
            if (mx >= tx && mx < tx + 22 && my >= ty && my < ty + 22) {
                tab = t;
                playClick();
                return true;
            }
        }
        int k = cellAt(mx, my);
        List<Integer> list = visible();
        if (k >= 0 && k < list.size()) {
            selected = list.get(k);
            PacketDistributor.sendToServer(new GunWorkbenchPayload(selected, Screen.hasShiftDown()));
            playClick();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void playClick() {
        if (minecraft != null)
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    public static List<Component> lines(GunCrafting.Recipe rc, Map<Item, Integer> have) {
        List<Component> l = new ArrayList<>();
        ItemStack out = rc.result();
        l.add(out.getHoverName().copy().withStyle(ChatFormatting.GOLD).append(Component.literal(out.getCount() > 1 ? " x" + out.getCount() : "").withStyle(ChatFormatting.GOLD)));
        l.add(Component.translatable("gui.tycooncraft.gun_workbench.needs").withStyle(ChatFormatting.GRAY));
        for (Map.Entry<Item, Integer> e : rc.needs().entrySet()) {
            int h = have.getOrDefault(e.getKey(), 0);
            Component name = e.getKey() == net.minecraft.world.item.Items.OAK_PLANKS
                    ? Component.translatable("gui.tycooncraft.gun_workbench.any_planks") : new ItemStack(e.getKey()).getHoverName();
            l.add(Component.literal(" • ").append(name).append(" " + Math.min(h, 999) + "/" + e.getValue())
                    .withStyle(h >= e.getValue() ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        l.add(Component.translatable("gui.tycooncraft.gun_workbench.click_fill_long").withStyle(ChatFormatting.DARK_GRAY));
        return l;
    }

    /** 자동 시험용 */
    public static void select(int t, int idx) {
        tab = t;
        selected = idx;
    }
}
