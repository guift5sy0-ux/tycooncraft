package com.tycooncraft.devtest;

import com.tycooncraft.TycoonCraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** 개발용 자동 시험 – 클라이언트: 키 입력 흉내, 카메라 전환, 스크린샷, 끝나면 종료 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class AutoTestClient {
    private static int t = -1;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!AutoTest.ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.player == null || mc.level == null) return;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        t++;
        try {
            step(mc);
        } catch (Exception e) {
            System.out.println("[AUTOTEST] client step " + t + " failed: " + e);
            e.printStackTrace();
        }
    }

    private static void key(KeyMapping k, boolean down) {
        KeyMapping.set(k.getKey(), down);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {});
        Entity v = mc.player.getVehicle();
        System.out.println("[AUTOTEST] screenshot " + name + " vehicle=" + (v == null ? "none" : v.getType().toShortString()));
    }

    private static void step(Minecraft mc) {
        var o = mc.options;
        Entity v = mc.player.getVehicle();
        switch (t) {
            case 1 -> o.setCameraType(CameraType.FIRST_PERSON);
            case 115 -> shot(mc, "01_overview");
            case 195 -> shot(mc, "02_car_front");
            case 275 -> shot(mc, "03_helicopter");
            case 355 -> shot(mc, "04_locomotive");
            // ── 자동차 ──
            case 400 -> {
                if (v != null) mc.player.setYRot(v.getYRot());
                mc.player.setXRot(12);
            }
            case 420 -> shot(mc, "05_car_cockpit");
            case 425 -> {
                key(o.keyUp, true);
                o.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 470 -> key(o.keyLeft, true);
            case 490 -> key(o.keyLeft, false);
            case 500 -> {
                key(o.keyJump, true);
                shot(mc, "06_car_driving");
            }
            case 503 -> key(o.keyJump, false);
            case 560 -> key(o.keyUp, false);
            case 600 -> shot(mc, "07_car_stopped");
            // ── 헬기 ──
            case 640 -> {
                o.setCameraType(CameraType.FIRST_PERSON);
                if (v != null) mc.player.setYRot(v.getYRot());
                mc.player.setXRot(8);
            }
            case 700 -> shot(mc, "08_heli_cockpit");
            case 735 -> {
                key(o.keyJump, true);
                o.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 785 -> {
                key(o.keyJump, false);
                shot(mc, "09_heli_hover");
                key(o.keyUp, true);
            }
            case 840 -> shot(mc, "10_heli_forward");
            case 870 -> key(o.keyUp, false);
            // ── 기관차 ──
            case 930 -> {
                o.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.player.setYRot(-90);
                mc.player.setXRot(15);
                key(o.keyUp, true);
            }
            case 1000 -> {
                key(o.keyJump, true);
                shot(mc, "11_loco_moving");
            }
            case 1003 -> key(o.keyJump, false);
            case 1010 -> {
                key(o.keyUp, false);
                o.setCameraType(CameraType.FIRST_PERSON);
                mc.player.setYRot(-90);
                mc.player.setXRot(10);
            }
            case 1030 -> shot(mc, "12_loco_cab");
            // ── 소음기 총 & 드론 ──
            case 1075 -> o.setCameraType(CameraType.FIRST_PERSON);
            case 1085 -> shot(mc, "13_silenced_pistol");
            case 1090 -> KeyMapping.click(o.keyUse.getKey());
            case 1110 -> KeyMapping.click(o.keyUse.getKey());
            case 1125 -> {
                mc.player.setXRot(-8);
                key(o.keyUp, true);
                key(o.keyJump, true);
            }
            case 1150 -> key(o.keyJump, false);
            case 1175 -> {
                shot(mc, "14_drone_cam");
                System.out.println("[AUTOTEST] camera=" + mc.getCameraEntity().getType().toShortString());
            }
            case 1185 -> {
                key(o.keyUp, false);
                mc.player.setXRot(35);
                key(o.keyUp, true);
            }
            case 1200 -> shot(mc, "15_drone_dive");
            case 1205 -> KeyMapping.click(o.keyAttack.getKey());
            case 1208 -> key(o.keyUp, false);
            case 1212 -> {
                shot(mc, "16_after_boom");
                System.out.println("[AUTOTEST] camera after=" + mc.getCameraEntity().getType().toShortString());
            }
            case 1240 -> {
                System.out.println("[AUTOTEST] finished");
                mc.stop();
            }
            default -> {
            }
        }
    }

    private AutoTestClient() {}
}
