package com.tycooncraft.client.mech;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.mech.ProtoDestroyer;
import com.tycooncraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * v0.28.3 프로토 디스트로이어 조종석 1인칭.
 * 창틀(위 프레임·기울어진 기둥) · 유리 반사/주사선 · 아래 계기판(기체·과열 게이지, 탄약·로켓 램프, 짓밟기 램프)
 * · 위쪽 방위 띠 · 가운데 개틀링 조준원+거리 · 손상/과열 경고등. 걸음마다 계기판이 쿵 하고 흔들림.
 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class CockpitHud {
    static final int METAL = 0xFF2A2D33, METAL_HI = 0xFF4A4F58, METAL_LO = 0xFF15171B, AMBER = 0xFFFFB040, AMBER_DIM = 0xFF7A5420,
            GREEN = 0xFF60FF80, RED = 0xFFFF3A2A;
    public static int frames;

    static ProtoDestroyer cockpit() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.options.getCameraType().isFirstPerson() && mc.player.getVehicle() instanceof ProtoDestroyer m
                && m.getControllingPassenger() == mc.player ? m : null;
    }

    @SubscribeEvent
    public static void hideVanilla(RenderGuiLayerEvent.Pre e) {
        if (cockpit() == null) return;
        var n = e.getName();
        if (n.equals(VanillaGuiLayers.HOTBAR) || n.equals(VanillaGuiLayers.CROSSHAIR) || n.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || n.equals(VanillaGuiLayers.FOOD_LEVEL) || n.equals(VanillaGuiLayers.ARMOR_LEVEL) || n.equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || n.equals(VanillaGuiLayers.EXPERIENCE_LEVEL) || n.equals(VanillaGuiLayers.VEHICLE_HEALTH) || n.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)
                || n.equals(VanillaGuiLayers.AIR_LEVEL)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void hideHand(RenderHandEvent e) {
        if (cockpit() != null) e.setCanceled(true);
    }

    /** 걸음 충격 (0~1) */
    static float stomp(ProtoDestroyer m, float pt) {
        float limb = m.walkAnimation.position(pt) * ProtoDestroyer.GAIT, amt = Math.min(1f, m.walkAnimation.speed(pt) * 1.6f);
        float land = m.attack() == ProtoDestroyer.SLAM ? Math.max(0f, 1f - (m.animT + pt) / 10f) : 0f;
        return (float) Math.pow(1f - Math.abs(Mth.sin(limb)), 6) * amt + land;
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post e) {
        ProtoDestroyer m = cockpit();
        Minecraft mc = Minecraft.getInstance();
        if (m == null || mc.options.hideGui) return;
        frames++;
        GuiGraphics g = e.getGuiGraphics();
        Font font = mc.font;
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(true);
        int w = g.guiWidth(), h = g.guiHeight();
        int bob = Math.round(stomp(m, pt) * 4f);
        long t = mc.level.getGameTime();
        float hp = m.getHealth() / m.getMaxHealth();
        boolean blink = (t / 6) % 2 == 0;

        // ── 유리: 살짝 호박색 반사 + 주사선 + 가장자리 어둡게 ──
        for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, 0x0A000000);
        for (int i = 0; i < 24; i++) {
            int a = (int) (0x50 * (1 - i / 24f));
            g.fill(0, i, w, i + 1, a << 24);
            g.fill(i, 0, i + 1, h, a << 24);
            g.fill(w - 1 - i, 0, w - i, h, a << 24);
        }
        g.fill(w / 2 + 40, 30, w / 2 + 44, h / 2 - 10, 0x10FFFFFF);   // 유리 반사 줄
        g.fill(w / 2 + 50, 30, w / 2 + 51, h / 2 - 30, 0x10FFFFFF);

        // ── 창틀: 위 프레임 + 양쪽 기울어진 기둥 ──
        int top = 16 + bob / 2;
        g.fill(0, 0, w, top, METAL);
        g.fill(0, top, w, top + 2, METAL_LO);
        g.fill(0, top - 3, w, top - 2, METAL_HI);
        for (int x = 10; x < w; x += 28) g.fill(x, 5, x + 2, 7, METAL_HI);   // 리벳
        int dash = (int) (h * 0.78f) + bob;
        for (int y = top; y < dash; y++) {
            float k = (float) (y - top) / (dash - top);
            int pw = (int) (10 + 30 * k * k);                 // 아래로 갈수록 두꺼워지는 기둥
            int off = (int) (6 + 26 * (1 - k));
            g.fill(off, y, off + pw, y + 1, METAL);
            g.fill(off + pw, y, off + pw + 1, y + 1, METAL_LO);
            g.fill(w - off - pw, y, w - off, y + 1, METAL);
            g.fill(w - off - pw - 1, y, w - off - pw, y + 1, METAL_LO);
            g.fill(0, y, off, y + 1, METAL_LO);
            g.fill(w - off, y, w, y + 1, METAL_LO);
        }

        // ── 위쪽 방위 띠 ──
        float yaw = Mth.wrapDegrees(mc.player.getViewYRot(pt));
        int dashY = (int) (h * 0.78f) + bob;
        int cx = w / 2, ty = dashY - 14;   // 방위 띠는 계기판 바로 위 (보스바와 안 겹치게)
        g.fill(cx - 90, ty - 1, cx + 90, ty + 10, 0xC0101214);
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        for (int d = -180; d <= 540; d += 15) {
            float rel = Mth.wrapDegrees(d - yaw);
            int x = cx + Math.round(rel * 1.2f);
            if (x < cx - 88 || x > cx + 88) continue;
            if (d % 45 == 0) {
                String s = dirs[Math.floorMod(d / 45, 8)];
                g.drawCenteredString(font, s, x, ty + 1, s.equals("N") ? RED : AMBER);
            } else g.fill(x, ty + 6, x + 1, ty + 9, AMBER_DIM);
        }
        g.fill(cx, ty + 9, cx + 1, ty + 12, AMBER);

        // ── 조준원 (개틀링) + 거리 ──
        int cy = h / 2;
        int col = m.overheated() ? (blink ? RED : AMBER_DIM) : 0xD0FFB040;
        ring(g, cx, cy, 12, col);
        g.fill(cx - 22, cy, cx - 15, cy + 1, col);
        g.fill(cx + 16, cy, cx + 23, cy + 1, col);
        g.fill(cx, cy - 22, cx + 1, cy - 15, col);
        g.fill(cx, cy + 16, cx + 1, cy + 23, col);
        g.fill(cx, cy, cx + 1, cy + 1, col);
        HitResult hr = mc.player.pick(80, pt, false);
        if (hr.getType() != HitResult.Type.MISS) {
            String r = String.format("%dm", (int) hr.getLocation().distanceTo(mc.player.getEyePosition(pt)));
            g.drawString(font, r, cx + 26, cy - 4, col, false);
        }
        if (mc.crosshairPickEntity != null && mc.crosshairPickEntity != m) {   // 표적 고정 표시
            g.renderOutline(cx - 28, cy - 28, 57, 57, blink ? RED : 0x80FF3A2A);
            g.drawCenteredString(font, Component.translatable("hud.tycooncraft.cockpit.lock"), cx, cy + 30, RED);
        }

        // ── 계기판 ──
        g.fill(0, dash, w, h, METAL);
        g.fill(0, dash, w, dash + 2, METAL_HI);
        g.fill(0, dash + 2, w, dash + 3, METAL_LO);
        for (int x = 14; x < w; x += 40) {
            g.fill(x, dash + 6, x + 2, dash + 8, METAL_HI);
            g.fill(x, h - 6, x + 2, h - 4, METAL_HI);
        }
        int py = dash + 10;
        // 왼쪽: 기체 · 과열 세로 게이지
        gauge(g, font, 20, py, h - py - 10, hp, hp < 0.3f ? (blink ? RED : 0xFF801810) : 0xFFD03A2A, "HULL", Math.round(m.getHealth()) + "");
        float heat = m.heat() / (float) ProtoDestroyer.MAX_HEAT;
        gauge(g, font, 54, py, h - py - 10, heat, m.overheated() ? (blink ? RED : AMBER_DIM) : AMBER, "HEAT", Math.round(heat * 100) + "%");
        // 가운데: 탄약 숫자판
        boolean cr = mc.player.getAbilities().instabuild;
        int ammo = ProtoDestroyer.count(mc.player, ModItems.HEAVY_AMMO.get());
        int rockets = ProtoDestroyer.count(mc.player, ModItems.ROCKET.get());
        int px = cx - 60;
        g.fill(px, py, px + 120, py + 22, 0xFF0A0C0E);
        g.renderOutline(px, py, 120, 22, METAL_HI);
        g.drawString(font, "AMMO", px + 4, py + 2, AMBER_DIM, false);
        String am = cr ? "INF" : String.format("%04d", Math.min(9999, ammo));
        var ps = g.pose();
        ps.pushPose();
        ps.translate(px + 40, py + 4, 0);
        ps.scale(2f, 2f, 1f);
        g.drawString(font, am, 0, 0, cr || ammo > 0 ? GREEN : RED, false);
        ps.popPose();
        // 로켓 램프 4개 + 짓밟기 램프
        int ly = py + 28;
        g.drawString(font, "RKT", px, ly + 1, AMBER_DIM, false);
        boolean rReady = m.rocketCd() <= 0 && (cr || rockets > 0);
        int lit = rReady ? 4 : Math.round(4f * (1f - m.rocketCd() / (float) ProtoDestroyer.ROCKET_CD));
        for (int i = 0; i < 4; i++) lamp(g, px + 24 + i * 12, ly, i < lit ? (rReady ? GREEN : AMBER) : 0xFF303030);
        g.drawString(font, "STMP", px + 76, ly + 1, AMBER_DIM, false);
        lamp(g, px + 104, ly, m.slamCd() > 0 ? 0xFF303030 : GREEN);
        // 오른쪽: 속도·고도 + 경고등
        double spd = m.getDeltaMovement().horizontalDistance() * 20 * 3.6;
        int rx = w - 110;
        g.drawString(font, String.format("SPD %4.0f km/h", spd), rx, py + 2, AMBER, false);
        g.drawString(font, String.format("ALT %4d", m.getBlockY()), rx, py + 14, AMBER, false);
        boolean dmg = hp < 0.3f, ovh = m.overheated();
        lampText(g, font, rx, py + 28, "DMG", dmg && blink ? RED : 0xFF302020);
        lampText(g, font, rx + 34, py + 28, "OVH", ovh && blink ? RED : 0xFF302020);
        lampText(g, font, rx + 68, py + 28, "AMO", !cr && ammo == 0 && blink ? AMBER : 0xFF302818);
        // 손상 시 화면 테두리 경고
        if (dmg && blink) {
            g.renderOutline(1, top + 2, w - 2, dash - top - 3, 0x90FF2010);
            g.drawCenteredString(font, Component.translatable("hud.tycooncraft.cockpit.damage"), cx, top + 8, RED);
        }
    }

    static void ring(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int a = 0; a < 360; a += 6) {
            int x = cx + Math.round(Mth.cos(a * Mth.DEG_TO_RAD) * r), y = cy + Math.round(Mth.sin(a * Mth.DEG_TO_RAD) * r);
            g.fill(x, y, x + 1, y + 1, col);
        }
    }

    static void gauge(GuiGraphics g, Font font, int x, int y, int hgt, float f, int col, String label, String val) {
        int gh = hgt - 10;
        g.fill(x, y, x + 22, y + gh, 0xFF0A0C0E);
        g.renderOutline(x, y, 22, gh, METAL_HI);
        int fh = Math.round((gh - 4) * Mth.clamp(f, 0, 1));
        for (int yy = y + gh - 2 - fh; yy < y + gh - 2; yy += 2) g.fill(x + 3, yy, x + 19, yy + 1, col);
        g.drawCenteredString(font, label, x + 11, y + gh + 1, AMBER_DIM);
        g.drawCenteredString(font, val, x + 11, y + 2, 0xFFE0E0E0);
    }

    static void lamp(GuiGraphics g, int x, int y, int col) {
        g.fill(x, y, x + 9, y + 9, 0xFF101010);
        g.fill(x + 1, y + 1, x + 8, y + 8, col);
        g.fill(x + 2, y + 2, x + 4, y + 4, 0x60FFFFFF);
    }

    static void lampText(GuiGraphics g, Font font, int x, int y, String s, int col) {
        g.fill(x, y, x + 30, y + 11, col);
        g.renderOutline(x, y, 30, 11, 0xFF101010);
        g.drawCenteredString(font, s, x + 15, y + 2, 0xFFFFFFFF);
    }

    private CockpitHud() {}
}
