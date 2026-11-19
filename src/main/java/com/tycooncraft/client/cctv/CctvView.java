package com.tycooncraft.client.cctv;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.cctv.CctvBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

import java.util.List;

/**
 * 스크린으로 CCTV 보기: 카메라 위치에 임시 시점 엔티티를 두고 화면을 그쪽으로 돌린다.
 * 숫자키 1~5 전환, 마우스로 ±60° 둘러보기, 웅크리기·ESC·피격·스크린에서 멀어지면 종료.
 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class CctvView {
    private static List<BlockPos> cams = List.of();
    private static BlockPos screen;
    private static int index = -1;
    private static Marker eye;
    private static float yaw0, pitch0, baseYaw;
    private static boolean signal;
    private static int switchFlash, ticks;

    public static boolean active() {
        return screen != null;
    }

    public static int index() {
        return index;
    }

    public static boolean signal() {
        return signal;
    }

    public static void open(BlockPos screenPos, List<BlockPos> links) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (links.isEmpty()) {
            mc.player.displayClientMessage(Component.translatable("message.tycooncraft.cctv.no_links").withStyle(net.minecraft.ChatFormatting.YELLOW), true);
            return;
        }
        cams = links;
        screen = screenPos;
        yaw0 = mc.player.getYRot();
        pitch0 = mc.player.getXRot();
        eye = new Marker(EntityType.MARKER, mc.level);
        ticks = 0;
        select(0);
    }

    public static void select(int i) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || i < 0 || i >= cams.size() || eye == null) return;
        index = i;
        switchFlash = 6;
        BlockPos p = cams.get(i);
        BlockState s = mc.level.isLoaded(p) ? mc.level.getBlockState(p) : null;
        signal = s != null && s.getBlock() instanceof CctvBlock;
        if (!signal) {
            mc.setCameraEntity(mc.player);
            return;
        }
        Direction f = s.getValue(CctvBlock.FACING);
        baseYaw = f.toYRot();
        double x = p.getX() + 0.5 + f.getStepX() * 0.45, y = p.getY() + 0.55, z = p.getZ() + 0.5 + f.getStepZ() * 0.45;
        eye.moveTo(x, y, z, baseYaw, 20f);
        eye.xo = x;
        eye.yo = y;
        eye.zo = z;
        eye.yRotO = baseYaw;
        eye.xRotO = 20f;
        mc.setCameraEntity(eye);
    }

    private static float sentYaw = 999, sentPitch = 999;

    /** 보는 방향대로 CCTV 머리를 돌림: 내 화면은 즉시, 서버(다른 사람)에는 조금씩 묶어서 전송 */
    private static void aimHead(BlockPos p, float yaw, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level.getBlockEntity(p) instanceof com.tycooncraft.cctv.CctvBlockEntity be) be.aim(yaw, pitch);
        if ((Math.abs(yaw - sentYaw) > 1.5f || Math.abs(pitch - sentPitch) > 1.5f) && ticks % 3 == 0) {
            sentYaw = yaw;
            sentPitch = pitch;
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.tycooncraft.cctv.CctvAimPayload(p, yaw, pitch));
        }
    }

    public static void close() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.player.setYRot(yaw0);
            mc.player.setXRot(pitch0);
        }
        screen = null;
        index = -1;
        eye = null;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre e) {
        if (!active()) return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        if (switchFlash > 0) switchFlash--;
        if (mc.player == null || mc.level == null || !mc.player.isAlive() || mc.player.hurtTime > 0
                || mc.player.distanceToSqr(screen.getX() + 0.5, screen.getY() + 0.5, screen.getZ() + 0.5) > 8 * 8) {
            close();
            return;
        }
        if (mc.screen instanceof PauseScreen) {
            mc.setScreen(null);
            close();
            return;
        }
        if (mc.options.keyShift.isDown() && ticks > 5) {
            close();
            return;
        }
        for (int i = 0; i < 5; i++)
            while (mc.options.keyHotbarSlots[i].consumeClick()) select(i);
        // 마우스로 둘러보기 (기본 방향 기준 좌우 60°, 위아래 30°)
        if (signal && eye != null) {
            float dy = Mth.clamp(Mth.wrapDegrees(mc.player.getYRot() - yaw0), -60, 60);
            float dp = Mth.clamp(mc.player.getXRot() - pitch0, -30, 30);
            eye.yRotO = eye.getYRot();
            eye.xRotO = eye.getXRot();
            eye.setYRot(baseYaw + dy);
            eye.setXRot(20f + dp);
            aimHead(cams.get(index), dy, 20f + dp);
            BlockPos p = cams.get(index);
            if (!(mc.level.getBlockState(p).getBlock() instanceof CctvBlock)) select(index);   // 카메라가 부서짐 → 신호 없음
        }
    }

    /** 보는 동안 움직이지 않음 */
    @SubscribeEvent
    public static void onMove(MovementInputUpdateEvent e) {
        if (!active()) return;
        var in = e.getInput();
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = in.jumping = false;
        in.shiftKeyDown = false;
    }

    /** 보는 동안 공격·사용 막기 */
    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered e) {
        if (!active()) return;
        e.setCanceled(true);
        e.setSwingHand(false);
    }

    /** 보는 동안 핫바·체력 같은 화면 요소 숨김 */
    @SubscribeEvent
    public static void onHand(net.neoforged.neoforge.client.event.RenderHandEvent e) {
        if (active()) e.setCanceled(true);   // 카메라 화면에 손/들고 있는 아이템이 보이지 않게
    }

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre e) {
        if (active()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post e) {
        if (!active()) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = e.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        var font = mc.font;
        if (!signal) {   // 신호 없음: 노이즈
            g.fill(0, 0, w, h, 0xFF101214);
            var r = new java.util.Random(ticks * 31L);
            for (int i = 0; i < 1400; i++) {
                int x = r.nextInt(w), y = r.nextInt(h), c = 40 + r.nextInt(160);
                g.fill(x, y, x + 2, y + 1, 0xFF000000 | c << 16 | c << 8 | c);
            }
            g.drawCenteredString(font, Component.translatable("gui.tycooncraft.cctv.no_signal"), w / 2, h / 2 - 4, 0xFFFF5555);
        } else {
            for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, 0x14000000);   // 주사선
            g.fill(0, 0, w, 14, 0x60000000);
            g.fill(0, h - 14, w, h, 0x60000000);
            int c = 0xC0FFFFFF;
            g.fill(8, 8, 28, 9, c); g.fill(8, 8, 9, 28, c);
            g.fill(w - 28, 8, w - 8, 9, c); g.fill(w - 9, 8, w - 8, 28, c);
            g.fill(8, h - 9, 28, h - 8, c); g.fill(8, h - 28, 9, h - 8, c);
            g.fill(w - 28, h - 9, w - 8, h - 8, c); g.fill(w - 9, h - 28, w - 8, h - 8, c);
        }
        if (switchFlash > 0) g.fill(0, 0, w, h, (switchFlash * 30) << 24 | 0xFFFFFF);
        if ((ticks / 10) % 2 == 0) g.fill(14, 3, 19, 8, 0xFFE03030);
        g.drawString(font, "REC", 22, 3, 0xFFFFFFFF, false);
        BlockPos p = cams.get(Math.max(0, index));
        g.drawString(font, Component.translatable("gui.tycooncraft.cctv.cam", index + 1, cams.size(), p.getX(), p.getY(), p.getZ()), 48, 3, 0xFFFFFFFF, false);
        long t = mc.level == null ? 0 : (mc.level.getDayTime() + 6000) % 24000;
        String clock = String.format("%02d:%02d", t / 1000, (t % 1000) * 60 / 1000);
        g.drawString(font, clock, w - 8 - font.width(clock), 3, 0xFFFFFFFF, false);
        StringBuilder chans = new StringBuilder();
        for (int i = 0; i < cams.size(); i++) chans.append(i == index ? "[" + (i + 1) + "] " : " " + (i + 1) + "  ");
        g.drawString(font, chans.toString(), 8, h - 11, 0xFFB0E0FF, false);
        Component hint = Component.translatable("gui.tycooncraft.cctv.hint");
        g.drawString(font, hint, w - 8 - font.width(hint), h - 11, 0xFFCCCCCC, false);
    }

    private CctvView() {}
}
