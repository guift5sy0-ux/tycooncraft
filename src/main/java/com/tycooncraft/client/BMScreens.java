package com.tycooncraft.client;

import com.tycooncraft.blackmarket.BMPayloads;
import com.tycooncraft.blackmarket.Casino;
import com.tycooncraft.blackmarket.IllegalMods;
import com.tycooncraft.economy.Coins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** v0.28 블랙마켓 화면: 의뢰 게시판 · 슬롯머신 · 룰렛 · 불법 개조점 */
public final class BMScreens {
    public static void open(BMPayloads.Open p) {
        Minecraft mc = Minecraft.getInstance();
        switch (p.screen()) {
            case BMPayloads.MISSIONS -> mc.setScreen(new MissionScreen(p));
            case BMPayloads.SLOTS, BMPayloads.ROULETTE -> {
                if (mc.screen instanceof CasinoScreen cs && cs.game == p.screen()) cs.update(p);
                else mc.setScreen(new CasinoScreen(p));
            }
            case BMPayloads.MODS -> mc.setScreen(new ModScreen(p));
            default -> {}
        }
    }

    static void send(int screen, int action, long arg, int arg2) {
        PacketDistributor.sendToServer(new BMPayloads.Act(screen, action, arg, arg2));
    }

    static void panel(GuiGraphics g, int x0, int y0, int x1, int y1, int border) {
        g.fill(x0, y0, x1, y1, 0xE0100A0C);
        g.renderOutline(x0, y0, x1 - x0, y1 - y0, border);
        g.renderOutline(x0 + 2, y0 + 2, x1 - x0 - 4, y1 - y0 - 4, (border & 0x00FFFFFF) | 0x50000000);
    }

    static final String[] TIER_KEYS = {"rep.tycooncraft.tier0", "rep.tycooncraft.tier1", "rep.tycooncraft.tier2", "rep.tycooncraft.tier3", "rep.tycooncraft.tier4"};
    static final int[] TIER_COL = {0xFFAAAAAA, 0xFF55FF55, 0xFF55FFFF, 0xFFFF55FF, 0xFFFFAA00};

    // ═════════ 의뢰 게시판 ═════════
    static class MissionScreen extends Screen {
        final BMPayloads.Open d;
        final boolean active;
        final int offers;

        MissionScreen(BMPayloads.Open d) {
            super(Component.translatable("gui.tycooncraft.mission.title"));
            this.d = d;
            active = d.nums().get(3) == 1;
            offers = d.nums().get(4).intValue();
        }

        int top() {
            return height / 2 - 105;
        }

        @Override
        protected void init() {
            int cx = width / 2, y = top() + 46;
            if (active) {
                addRenderableWidget(Button.builder(Component.translatable("gui.tycooncraft.mission.turnin"), b -> send(BMPayloads.MISSIONS, 1, 0, 0)).bounds(cx + 60, y + 26, 70, 18).build());
                addRenderableWidget(Button.builder(Component.translatable("gui.tycooncraft.mission.abandon"), b -> send(BMPayloads.MISSIONS, 2, 0, 0)).bounds(cx + 134, y + 26, 46, 18).build());
            }
            int oy = top() + 106;
            for (int i = 0; i < offers; i++) {
                final int k = i;
                Button b = Button.builder(Component.translatable("gui.tycooncraft.mission.accept"), x -> send(BMPayloads.MISSIONS, 0, k, 0)).bounds(cx + 124, oy + i * 34 + 6, 56, 18).build();
                b.active = !active;
                addRenderableWidget(b);
            }
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            int cx = width / 2, top = top();
            panel(g, cx - 190, top - 8, cx + 190, top + 222, 0xFFAA7722);
            super.render(g, mx, my, pt);
            g.drawCenteredString(font, title, cx, top, 0xFFFFCC55);
            // 평판
            int pts = d.nums().get(0).intValue(), tier = d.nums().get(1).intValue();
            long next = d.nums().get(2);
            g.drawString(font, Component.translatable("gui.tycooncraft.rep").append(": ").append(Component.translatable(TIER_KEYS[tier])), cx - 180, top + 16, TIER_COL[tier]);
            int bx = cx - 30, bw = 210;
            g.fill(bx, top + 16, bx + bw, top + 24, 0xFF222222);
            float f = next < 0 ? 1f : (float) pts / next;
            g.fill(bx + 1, top + 17, bx + 1 + Math.round((bw - 2) * Mth.clamp(f, 0, 1)), top + 23, TIER_COL[tier]);
            String ps = next < 0 ? pts + " (MAX)" : pts + " / " + next;
            g.drawCenteredString(font, ps, bx + bw / 2, top + 16, 0xFFFFFFFF);
            // 진행 중
            int y = top + 34;
            g.drawString(font, Component.translatable("gui.tycooncraft.mission.active"), cx - 180, y, 0xFFFFAA44);
            y += 12;
            g.fill(cx - 182, y - 2, cx + 182, y + 46, 0x40FFAA44);
            if (active) wrap(g, d.lines().get(0), cx - 178, y + 2, 230, 0xFFFFFFFF);
            else g.drawString(font, Component.translatable("gui.tycooncraft.mission.none"), cx - 178, y + 2, 0xFF888888);
            // 오늘의 의뢰
            y = top + 96;
            g.drawString(font, Component.translatable("gui.tycooncraft.mission.offers"), cx - 180, y, 0xFFFFAA44);
            y += 10;
            int base = active ? 1 : 0;
            for (int i = 0; i < offers; i++) {
                g.fill(cx - 182, y + i * 34, cx + 182, y + i * 34 + 30, 0x30FFFFFF);
                wrap(g, d.lines().get(base + i), cx - 178, y + i * 34 + 3, 296, 0xFFE0E0E0);
            }
            if (offers == 0) g.drawString(font, Component.translatable("gui.tycooncraft.mission.no_offers"), cx - 178, y + 4, 0xFF888888);
            if (!d.msg().isEmpty()) g.drawCenteredString(font, d.msg(), cx, top + 210, 0xFFFFCC66);
        }

        void wrap(GuiGraphics g, String s, int x, int y, int w, int col) {
            var lines = font.split(Component.literal(s), w);
            for (int i = 0; i < Math.min(3, lines.size()); i++) g.drawString(font, lines.get(i), x, y + i * 9, col);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    // ═════════ 카지노: 슬롯머신 · 룰렛 ═════════
    public static class CasinoScreen extends Screen {
        static final String[] SYM = {"♥", "●", "♪", "♦", "7", "☠"};
        static final int[] SYM_COL = {0xFFFF3355, 0xFFFFEE44, 0xFFFFAA22, 0xFF44EEFF, 0xFFFF2222, 0xFFBBBBBB};
        static final String[] SYM_NAME = {"gui.tycooncraft.casino.cherry", "gui.tycooncraft.casino.lemon", "gui.tycooncraft.casino.bell",
                "gui.tycooncraft.casino.diamond", "gui.tycooncraft.casino.seven", "gui.tycooncraft.casino.skull"};
        final int game;
        BMPayloads.Open d;
        EditBox bet;
        int spinT = -1, choice = Casino.RED;
        long[] res;
        String pendingMsg = "";
        long shownFunds;
        public static int results;

        CasinoScreen(BMPayloads.Open d) {
            super(Component.translatable(d.screen() == BMPayloads.SLOTS ? "gui.tycooncraft.casino.slots" : "gui.tycooncraft.casino.roulette"));
            this.game = d.screen();
            this.d = d;
            shownFunds = d.nums().get(0);
        }

        void update(BMPayloads.Open p) {
            d = p;
            if (p.nums().size() > 7 && p.nums().get(7) == 1) {
                res = new long[]{p.nums().get(3), p.nums().get(4), p.nums().get(5), p.nums().get(6)};
                spinT = 0;
                pendingMsg = p.msg();
                results++;
            } else {
                pendingMsg = p.msg();
                shownFunds = p.nums().get(0);
            }
        }

        int top() {
            return height / 2 - 90;
        }

        @Override
        protected void init() {
            int cx = width / 2, y = top() + 134;
            bet = new EditBox(font, cx - 60, y, 70, 18, Component.literal("bet"));
            bet.setFilter(s -> s.matches("\\d{0,9}"));
            bet.setValue("1000");
            addRenderableWidget(bet);
            addRenderableWidget(Button.builder(Component.literal("+1만"), b -> add(10_000)).bounds(cx + 14, y, 34, 18).build());
            addRenderableWidget(Button.builder(Component.literal("×2"), b -> mul()).bounds(cx + 50, y, 26, 18).build());
            addRenderableWidget(Button.builder(Component.literal("MAX"), b -> bet.setValue(String.valueOf(d.nums().get(1)))).bounds(cx + 78, y, 34, 18).build());
            if (game == BMPayloads.ROULETTE) {
                int by = top() + 108;
                addRenderableWidget(Button.builder(Component.translatable("gui.tycooncraft.casino.red"), b -> choice = Casino.RED).bounds(cx - 120, by, 76, 18).build());
                addRenderableWidget(Button.builder(Component.translatable("gui.tycooncraft.casino.black"), b -> choice = Casino.BLACK).bounds(cx - 38, by, 76, 18).build());
                addRenderableWidget(Button.builder(Component.translatable("gui.tycooncraft.casino.green"), b -> choice = Casino.GREEN).bounds(cx + 44, by, 76, 18).build());
            }
            addRenderableWidget(Button.builder(Component.translatable(game == BMPayloads.SLOTS ? "gui.tycooncraft.casino.spin" : "gui.tycooncraft.casino.roll"), b -> {
                if (spinning()) return;
                long v = bet.getValue().isEmpty() ? 0 : Long.parseLong(bet.getValue());
                if (v > 0) send(game, 0, v, choice);
            }).bounds(cx - 60, y + 24, 120, 20).build());
        }

        void add(long v) {
            long cur = bet.getValue().isEmpty() ? 0 : Long.parseLong(bet.getValue());
            bet.setValue(String.valueOf(Math.min(d.nums().get(1), cur + v)));
        }

        void mul() {
            long cur = bet.getValue().isEmpty() ? 0 : Long.parseLong(bet.getValue());
            bet.setValue(String.valueOf(Math.min(d.nums().get(1), Math.max(1000, cur * 2))));
        }

        boolean spinning() {
            return spinT >= 0 && spinT < 34;
        }

        @Override
        public void tick() {
            super.tick();
            if (spinT >= 0 && spinT < 34) {
                spinT++;
                var mc = Minecraft.getInstance();
                if (spinT % 2 == 0 && spinT < 30) mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.25f, 1.6f + spinT * 0.01f);
                if (spinT == 34) {
                    shownFunds = d.nums().get(0);
                    if (res[3] > 0) mc.player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.6f, 1.4f);
                }
            }
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            int cx = width / 2, top = top();
            panel(g, cx - 140, top - 8, cx + 140, top + 196, 0xFFCC2255);
            super.render(g, mx, my, pt);
            g.drawCenteredString(font, title, cx, top, 0xFFFF77AA);
            g.drawString(font, Component.translatable("gui.tycooncraft.casino.funds", Coins.format(shownFunds)), cx - 128, top + 14, 0xFF88FF88);
            String lim = Component.translatable("gui.tycooncraft.casino.max", Coins.format(d.nums().get(1))).getString() + " · " + d.lines().get(0);
            g.drawString(font, lim, cx + 128 - font.width(lim), top + 14, 0xFFBBBBBB);
            if (game == BMPayloads.SLOTS) renderSlots(g, cx, top + 30, pt);
            else renderRoulette(g, cx, top + 30, pt);
            if (!spinning() && !pendingMsg.isEmpty()) g.drawCenteredString(font, pendingMsg, cx, top + 182, 0xFFFFDD66);
        }

        void renderSlots(GuiGraphics g, int cx, int y, float pt) {
            var rnd = new java.util.Random((long) (spinT * 7919L));
            for (int i = 0; i < 3; i++) {
                int x = cx - 90 + i * 62;
                g.fill(x, y, x + 56, y + 56, 0xFF1A1014);
                g.renderOutline(x, y, 56, 56, 0xFFFFCC44);
                int stop = 14 + i * 8;
                int sym = res == null ? i : (spinT >= 0 && spinT < stop) ? rnd.nextInt(6) : (int) res[i];
                var ps = g.pose();
                ps.pushPose();
                ps.translate(x + 28, y + 14, 0);
                ps.scale(3f, 3f, 1f);
                g.drawCenteredString(font, SYM[sym], 0, 0, SYM_COL[sym]);
                ps.popPose();
                g.drawCenteredString(font, Component.translatable(SYM_NAME[sym]), x + 28, y + 44, 0xFFAAAAAA);
            }
            g.drawCenteredString(font, Component.translatable("gui.tycooncraft.casino.slots_pay"), cx, y + 64, 0xFF999999);
            g.drawCenteredString(font, Component.translatable("gui.tycooncraft.casino.slots_pay2"), cx, y + 74, 0xFF999999);
        }

        void renderRoulette(GuiGraphics g, int cx, int y, float pt) {
            int n = res == null ? 0 : spinning() ? (int) ((spinT * 5 + res[0]) % 37) : (int) res[0];
            int c = Casino.color(n);
            int col = c == Casino.RED ? 0xFFCC2222 : c == Casino.GREEN ? 0xFF22AA44 : 0xFF222222;
            g.fill(cx - 30, y + 2, cx + 30, y + 50, col);
            g.renderOutline(cx - 30, y + 2, 60, 48, 0xFFFFCC44);
            var ps = g.pose();
            ps.pushPose();
            ps.translate(cx, y + 14, 0);
            ps.scale(3f, 3f, 1f);
            g.drawCenteredString(font, res == null ? "?" : String.valueOf(n), 0, 0, 0xFFFFFFFF);
            ps.popPose();
            String[] k = {"gui.tycooncraft.casino.red", "gui.tycooncraft.casino.black", "gui.tycooncraft.casino.green"};
            g.drawCenteredString(font, Component.translatable("gui.tycooncraft.casino.choice", Component.translatable(k[choice])), cx, y + 56, 0xFFFFFFFF);
            g.drawCenteredString(font, Component.translatable("gui.tycooncraft.casino.roulette_pay"), cx, y + 67, 0xFF999999);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    // ═════════ 불법 개조점 ═════════
    static class ModScreen extends Screen {
        final BMPayloads.Open d;

        ModScreen(BMPayloads.Open d) {
            super(Component.translatable("gui.tycooncraft.mods.title"));
            this.d = d;
        }

        int top() {
            return height / 2 - 95;
        }

        @Override
        protected void init() {
            int cx = width / 2, y = top() + 40;
            boolean gun = d.nums().get(1) == 1;
            int tier = d.nums().get(0).intValue(), mask = d.nums().get(2).intValue();
            for (int i = 0; i < IllegalMods.IDS.length; i++) {
                final int k = i;
                Button b = Button.builder(Component.translatable("gui.tycooncraft.mods.install"), x -> send(BMPayloads.MODS, 0, k, 0))
                        .bounds(cx + 112, y + i * 22, 52, 18).build();
                b.active = gun && tier >= IllegalMods.TIER[i] && (mask & (1 << i)) == 0;
                addRenderableWidget(b);
            }
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            int cx = width / 2, top = top();
            panel(g, cx - 175, top - 8, cx + 175, top + 186, 0xFF883322);
            super.render(g, mx, my, pt);
            g.drawCenteredString(font, title, cx, top, 0xFFFF6644);
            boolean gun = d.nums().get(1) == 1;
            int tier = d.nums().get(0).intValue(), mask = d.nums().get(2).intValue();
            g.drawString(font, gun ? Component.translatable("gui.tycooncraft.mods.gun", d.lines().get(0)) : Component.translatable("gui.tycooncraft.mods.hold_gun"),
                    cx - 165, top + 14, gun ? 0xFFFFFFFF : 0xFFFF5555);
            g.drawString(font, Component.translatable("gui.tycooncraft.rep").getString() + ": " + d.lines().get(1), cx - 165, top + 25, 0xFFBBBBBB);
            int y = top + 40;
            for (int i = 0; i < IllegalMods.IDS.length; i++) {
                boolean has = (mask & (1 << i)) != 0, ok = tier >= IllegalMods.TIER[i];
                g.fill(cx - 167, y + i * 22 - 2, cx + 167, y + i * 22 + 19, has ? 0x4044FF44 : 0x30FFFFFF);
                g.drawString(font, Component.translatable("mod.tycooncraft." + IllegalMods.IDS[i]), cx - 162, y + i * 22, has ? 0xFF66FF66 : 0xFFFFFFFF);
                g.drawString(font, Component.translatable("mod.tycooncraft." + IllegalMods.IDS[i] + ".desc"), cx - 162, y + i * 22 + 9, 0xFFAAAAAA);
                String pr = has ? Component.translatable("gui.tycooncraft.mods.installed").getString() : "₩" + Coins.format(d.nums().get(3 + i));
                g.drawString(font, pr, cx + 104 - font.width(pr), y + i * 22, has ? 0xFF66FF66 : 0xFFFFD040);
                if (!ok && !has) {
                    var need = Component.translatable(TIER_KEYS[IllegalMods.TIER[i]]);
                    g.drawString(font, need, cx + 104 - font.width(need), y + i * 22 + 9, 0xFFFF5555);
                }
            }
            if (!d.msg().isEmpty()) g.drawCenteredString(font, d.msg(), cx, top + 176, 0xFFFFCC66);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private BMScreens() {}
}
