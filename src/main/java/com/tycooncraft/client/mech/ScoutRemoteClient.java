package com.tycooncraft.client.mech;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.mech.ProtoScout;
import com.tycooncraft.mech.ScoutRemote;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * v0.28.1 프로토-정찰 원격 조종 (클라이언트): 카메라가 기계 뒤에서 따라감, 내 몸은 제자리.
 * WASD 이동 · 마우스 방향 · 좌클릭(누르기) 베기 · 우클릭 덮치기 · 스페이스 점프 · A/D 두 번 대시 · Shift 끝
 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class ScoutRemoteClient {
    private static int id = -1, wait, leftTap, rightTap;
    private static boolean seen, leftWas, rightWas, stopSent;
    private static float fwd, strafe;
    private static boolean jump, sneak;
    private static byte pendingAct;
    private static CameraType prevCam;
    public static int framesLinked;

    public static void link(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (entityId < 0) {
            end(mc);
            return;
        }
        id = entityId;
        wait = 0;
        seen = stopSent = false;
        pendingAct = 0;
        if (prevCam == null) prevCam = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    public static boolean active() {
        return id >= 0;
    }

    static ProtoScout mech(Minecraft mc) {
        if (mc.level == null || id < 0) return null;
        Entity e = mc.level.getEntity(id);
        return e instanceof ProtoScout p ? p : null;
    }

    static void end(Minecraft mc) {
        id = -1;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        if (prevCam != null) mc.options.setCameraType(prevCam);
        prevCam = null;
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent e) {
        if (!active()) return;
        Input in = e.getInput();
        fwd = in.forwardImpulse;
        strafe = in.leftImpulse;
        jump = in.jumping;
        sneak = in.shiftKeyDown;
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        if (!active() || e.isPickBlock()) return;
        e.setCanceled(true);
        e.setSwingHand(false);
        if (e.isUseItem()) pendingAct = ProtoScout.ACT_POUNCE;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (!active()) return;
        if (mc.player == null || mc.level == null || !mc.player.isAlive()) {
            end(mc);
            return;
        }
        ProtoScout m = mech(mc);
        if (m == null || m.isRemoved()) {
            // 잠깐 안 보일 수 있음(청크 넘어갈 때) → 3초까지 기다렸다가 포기
            if (++wait > 60) {
                if (!stopSent) PacketDistributor.sendToServer(new ScoutRemote.Input(id, 0, 0, false, 0, 0, false, ScoutRemote.Input.STOP));
                end(mc);
            }
            return;
        }
        wait = 0;
        seen = true;
        framesLinked++;
        if (mc.getCameraEntity() != m) mc.setCameraEntity(m);
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        boolean screen = mc.screen != null;
        byte act = pendingAct;
        pendingAct = 0;
        if (sneak && !screen) {
            if (!stopSent) PacketDistributor.sendToServer(new ScoutRemote.Input(m.getId(), 0, 0, false, 0, 0, false, ScoutRemote.Input.STOP));
            stopSent = true;
            end(mc);
            return;
        }
        // A/D 두 번 빠르게 → 옆 대시
        boolean l = !screen && mc.options.keyLeft.isDown(), r = !screen && mc.options.keyRight.isDown();
        if (leftTap > 0) leftTap--;
        if (rightTap > 0) rightTap--;
        if (l && !leftWas) {
            if (leftTap > 0) act = ProtoScout.ACT_DASH;
            leftTap = 6;
        }
        if (r && !rightWas) {
            if (rightTap > 0) act = ProtoScout.ACT_DASH + 10;
            rightTap = 6;
        }
        leftWas = l;
        rightWas = r;
        boolean slash = !screen && mc.options.keyAttack.isDown();
        PacketDistributor.sendToServer(new ScoutRemote.Input(m.getId(), screen ? 0 : fwd, screen ? 0 : strafe, !screen && jump,
                mc.player.getYRot(), mc.player.getXRot(), slash, act));
    }

    /** 카메라 방향 = 내가 보는 방향 */
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.player == null || !(mc.getCameraEntity() instanceof ProtoScout)) return;
        float pt = (float) e.getPartialTick();
        e.setYaw(mc.player.getViewYRot(pt));
        e.setPitch(mc.player.getViewXRot(pt));
    }

    @SubscribeEvent
    public static void onCamDist(CalculateDetachedCameraDistanceEvent e) {
        if (active()) e.setDistance(3.2f);
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent e) {
        if (active()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre e) {
        if (active() && (e.getName().equals(VanillaGuiLayers.HOTBAR) || e.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || e.getName().equals(VanillaGuiLayers.FOOD_LEVEL) || e.getName().equals(VanillaGuiLayers.ARMOR_LEVEL))) e.setCanceled(true);
    }

    /** 원격 조종 화면: 모서리 · 기체 체력 · 덮치기 · 신호 · 조작법 */
    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ProtoScout m = mech(mc);
        if (!active() || m == null || mc.options.hideGui || mc.player == null) return;
        GuiGraphics g = e.getGuiGraphics();
        Font font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight(), c = 0xD0F0E0A0, red = 0xFFFF4030;
        int mm = 12, L = 20;
        for (int[] k : new int[][]{{mm, mm, 1, 1}, {w - mm, mm, -1, 1}, {mm, h - mm, 1, -1}, {w - mm, h - mm, -1, -1}}) {
            g.fill(Math.min(k[0], k[0] + k[2] * L), k[1], Math.max(k[0], k[0] + k[2] * L), k[1] + k[3], c);
            g.fill(k[0], Math.min(k[1], k[1] + k[3] * L), k[0] + k[2], Math.max(k[1], k[1] + k[3] * L), c);
        }
        boolean blink = (mc.level.getGameTime() / 10) % 2 == 0;
        if (blink) g.fill(mm + 6, mm + 8, mm + 11, mm + 13, red);
        g.drawString(font, Component.translatable("hud.tycooncraft.proto_scout.remote"), mm + 15, mm + 7, c, false);
        int x = mm + 8, y = mm + 22;
        ProtoClient.bar(g, x, y, m.getHealth() / m.getMaxHealth(), 0xFFD03A2A, Component.translatable("hud.tycooncraft.proto.hp", Math.round(m.getHealth())), font);
        String pc = m.pounceCd() > 0 ? String.format("%.1fs", m.pounceCd() / 20f) : "READY";
        g.drawString(font, Component.translatable("hud.tycooncraft.proto_scout.pounce", pc), x, y + 12, m.pounceCd() > 0 ? 0xFF909090 : 0xFF80FF80, false);
        double dist = mc.player.distanceTo(m);
        int bars = dist < ProtoScout.RANGE * 0.4 ? 4 : dist < ProtoScout.RANGE * 0.65 ? 3 : dist < ProtoScout.RANGE * 0.85 ? 2 : 1;
        int sx = w - mm - 90, sy = mm + 8;
        g.drawString(font, (int) dist + " / " + (int) ProtoScout.RANGE + "m", sx, sy, dist > ProtoScout.RANGE * 0.85 ? red : c, false);
        for (int i = 0; i < 4; i++) {
            int bh = 3 + i * 3;
            g.fill(sx + 62 + i * 5, sy + 9 - bh, sx + 65 + i * 5, sy + 9, i < bars ? c : 0x40FFFFFF);
        }
        Component hint = Component.translatable("hud.tycooncraft.proto_scout.keys");
        g.drawString(font, hint, (w - font.width(hint)) / 2, h - mm - 12, c, true);
    }

    private ScoutRemoteClient() {}
}
