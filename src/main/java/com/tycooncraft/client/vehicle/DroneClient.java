package com.tycooncraft.client.vehicle;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.network.DronePayloads;
import com.tycooncraft.vehicle.DroneEntity;
import com.tycooncraft.vehicle.VehicleHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 드론 조종 (클라이언트): 카메라를 드론으로, 이동 키는 드론에게만, 클릭 = 폭파, Shift = 조종 끝.
 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class DroneClient {
    private static int pilotId = -1;
    private static int waitTicks;
    private static boolean seenDrone, seenPiloted, detonate, release;
    private static VehicleHooks.Input input = VehicleHooks.Input.NONE;

    public static void start(int entityId) {
        pilotId = entityId;
        waitTicks = 0;
        seenDrone = seenPiloted = detonate = release = false;
    }

    public static boolean piloting() {
        return pilotId >= 0;
    }

    private static DroneEntity drone(Minecraft mc) {
        if (mc.level == null) return null;
        Entity e = mc.level.getEntity(pilotId);
        return e instanceof DroneEntity d ? d : null;
    }

    private static void stop(Minecraft mc) {
        DroneEntity d = drone(mc);
        if (d != null) d.localPilot = false;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        pilotId = -1;
        input = VehicleHooks.Input.NONE;
    }

    /** 플레이어 몸은 가만히: 이동 입력을 가로채서 드론에게만 */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!piloting()) return;
        Input in = event.getInput();
        Minecraft mc = Minecraft.getInstance();
        input = new VehicleHooks.Input(in.forwardImpulse, in.leftImpulse, in.jumping, mc.options.keySprint.isDown() || VehicleKeyBinds.DESCEND.isDown());
        if (in.shiftKeyDown) release = true;
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
    }

    /** 조종 중 클릭 = 폭파 (공격·사용 막기) */
    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (!piloting()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.isAttack() || event.isUseItem()) detonate = true;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!piloting()) return;
        if (mc.player == null || mc.level == null || !mc.player.isAlive()) {
            pilotId = -1;
            return;
        }
        DroneEntity d = drone(mc);
        if (d == null || d.isRemoved()) {
            if (seenDrone || ++waitTicks > 60) stop(mc);
            return;
        }
        seenDrone = true;
        d.localPilot = true;
        if (mc.getCameraEntity() != d) mc.setCameraEntity(d);
        if (d.isPiloted()) seenPiloted = true;
        else if (seenPiloted) {          // 서버가 끝냄 (거리 초과 등)
            stop(mc);
            return;
        }
        byte action = 0;
        if (mc.screen == null) {
            boolean crash = d.flyLocal(input, mc.player.getYRot(), mc.player.getXRot());
            if (crash || detonate) action = 1;
            else if (release) action = 2;
        }
        PacketDistributor.sendToServer(new DronePayloads.Control(d.getId(), d.getX(), d.getY(), d.getZ(), d.getYRot(), d.getXRot(), action));
        if (action != 0) stop(mc);
    }

    /** 카메라 방향 = 내가 보는 방향 (마우스 움직임이 바로 반영되도록) + 기울기 */
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!piloting() || mc.player == null || !(mc.getCameraEntity() instanceof DroneEntity d)) return;
        float pt = (float) event.getPartialTick();
        event.setYaw(mc.player.getViewYRot(pt));
        event.setPitch(mc.player.getViewXRot(pt));
        event.setRoll(-Mth.lerp(pt, d.rollO, d.roll) * 0.4f);
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        if (piloting()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre event) {
        if (piloting() && (event.getName().equals(VanillaGuiLayers.CROSSHAIR) || event.getName().equals(VanillaGuiLayers.HOTBAR)
                || event.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR) || event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)
                || event.getName().equals(VanillaGuiLayers.FOOD_LEVEL) || event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL))) {
            event.setCanceled(true);
        }
    }

    /** 드론 카메라 화면: 모서리 표시, 조준점, 배터리, 거리, 고도, 신호, 조작법 */
    public static void renderHud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!piloting() || mc.player == null) return;
        DroneEntity d = drone(mc);
        if (d == null) return;
        Font font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        int c = 0xD0E8F0E8, red = 0xFFFF4030;
        // 화면 테두리 모서리
        int m = 14, L = 22;
        for (int[] k : new int[][]{{m, m, 1, 1}, {w - m, m, -1, 1}, {m, h - m, 1, -1}, {w - m, h - m, -1, -1}}) {
            g.fill(Math.min(k[0], k[0] + k[2] * L), k[1], Math.max(k[0], k[0] + k[2] * L), k[1] + k[3], c);
            g.fill(k[0], Math.min(k[1], k[1] + k[3] * L), k[0] + k[2], Math.max(k[1], k[1] + k[3] * L), c);
        }
        // 조준점
        int cx = w / 2, cy = h / 2;
        g.fill(cx - 12, cy, cx - 4, cy + 1, c);
        g.fill(cx + 5, cy, cx + 13, cy + 1, c);
        g.fill(cx, cy - 12, cx + 1, cy - 4, c);
        g.fill(cx, cy + 5, cx + 1, cy + 13, c);
        g.fill(cx, cy, cx + 1, cy + 1, red);
        // 녹화 표시
        boolean blink = (mc.level.getGameTime() / 10) % 2 == 0;
        if (blink) g.fill(m + 6, m + 8, m + 11, m + 13, red);
        g.drawString(font, d.isMk2() ? "REC  TC-DRONE MK2" : "REC  TC-DRONE", m + 15, m + 7, c, false);
        // 배터리
        float bat = d.batteryFraction();
        int bx = w - m - 70, by = m + 6;
        g.drawString(font, Component.translatable("hud.tycooncraft.drone.battery"), bx - 34, by + 1, c, false);
        g.fill(bx, by, bx + 60, by + 9, 0x80000000);
        g.fill(bx + 1, by + 1, bx + 1 + Math.round(58 * bat), by + 8, bat < 0.2f ? (blink ? red : 0xFF802010) : 0xFF60E070);
        g.fill(bx + 60, by + 2, bx + 62, by + 7, c);
        // 거리, 고도, 속도, 신호
        double dist = mc.player.distanceTo(d);
        double sp = Math.sqrt(Math.pow(d.getX() - d.xo, 2) + Math.pow(d.getY() - d.yo, 2) + Math.pow(d.getZ() - d.zo, 2)) * 20 * 3.6;
        double range = d.maxRange();
        int bars = dist < range * 0.4 ? 4 : dist < range * 0.65 ? 3 : dist < range * 0.85 ? 2 : 1;
        int y0 = h - m - 44;
        g.drawString(font, Component.translatable("hud.tycooncraft.drone.dist", (int) dist, (int) range), m + 8, y0, dist > range * 0.85 ? red : c, false);
        g.drawString(font, Component.translatable("hud.tycooncraft.drone.alt", (int) Math.floor(d.getY())), m + 8, y0 + 11, c, false);
        g.drawString(font, (int) sp + " km/h", m + 8, y0 + 22, c, false);
        for (int i = 0; i < 4; i++) {
            int bh = 3 + i * 3;
            g.fill(m + 90 + i * 5, y0 + 30 - bh, m + 93 + i * 5, y0 + 30, i < bars ? c : 0x40FFFFFF);
        }
        Component hint = Component.translatable("hud.tycooncraft.drone.hint");
        g.drawString(font, hint, (w - font.width(hint)) / 2, h - m - 12, c, true);
        if (bat < 0.2f && blink) {
            Component low = Component.translatable("hud.tycooncraft.drone.low");
            g.drawString(font, low, (w - font.width(low)) / 2, cy + 22, red, true);
        }
    }

    private DroneClient() {}
}
