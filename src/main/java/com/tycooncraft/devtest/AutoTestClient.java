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

    private static int phase, phaseT;

    private static void next(int p) {
        phase = p;
        phaseT = 0;
        System.out.println("[AUTOTEST] phase " + p + " at t=" + t);
    }

    /** 서버 시계와 어긋나도 되도록, 손에 든 물건·조종 상태를 보고 진행 */
    private static void phases(Minecraft mc) {
        var o = mc.options;
        phaseT++;
        var held = mc.player.getMainHandItem();
        switch (phase) {
            case 0 -> { if (held.getItem() instanceof com.tycooncraft.gun.GunItem) next(1); }
            case 1 -> {
                if (phaseT == 15) shot(mc, "13_silenced_pistol");
                if (phaseT == 20) KeyMapping.click(o.keyUse.getKey());
                if (phaseT == 22) shot(mc, "14_silenced_fire");
                if (held.getItem() instanceof com.tycooncraft.vehicle.DroneItem) next(2);
            }
            case 2 -> {
                if (phaseT == 15) KeyMapping.click(o.keyUse.getKey());
                if (com.tycooncraft.client.vehicle.DroneClient.piloting()
                        && mc.getCameraEntity() instanceof com.tycooncraft.vehicle.DroneEntity) next(3);
                if (phaseT > 200) next(9);
            }
            case 3 -> {
                if (phaseT == 2) {
                    mc.player.setXRot(-6);
                    key(o.keyUp, true);
                    key(o.keyJump, true);
                }
                if (phaseT == 22) key(o.keyJump, false);
                if (phaseT == 30) {
                    shot(mc, "15_drone_cam");
                    System.out.println("[AUTOTEST] camera=" + mc.getCameraEntity().getType().toShortString()
                            + String.format(" pos=%.1f,%.1f,%.1f", mc.getCameraEntity().getX(), mc.getCameraEntity().getY(), mc.getCameraEntity().getZ()));
                }
                if (phaseT == 34) mc.player.setXRot(40);
                if (phaseT == 46) shot(mc, "16_drone_dive");
                if (phaseT == 50) KeyMapping.click(o.keyAttack.getKey());
                if (phaseT == 52) key(o.keyUp, false);
                if (phaseT == 64) {
                    o.setCameraType(CameraType.THIRD_PERSON_BACK);
                    System.out.println("[AUTOTEST] camera after=" + mc.getCameraEntity().getType().toShortString());
                }
                if (phaseT == 72) {
                    shot(mc, "17_after_boom");
                    next(9);
                }
            }
            case 9 -> {
                if (phaseT == 10) next(10);
            }
            case 10 -> later(mc);
            case 11 -> {
                if (phaseT == 10) {
                    System.out.println("[AUTOTEST] finished");
                    mc.stop();
                }
            }
            default -> {
            }
        }
    }

    /** 통합 서버 스레드에서 플레이어를 직접 다룸 (클라이언트 setPos는 먼 거리에서 무시될 수 있음) */
    private static void serverDo(Minecraft mc, java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> f) {
        var srv = mc.getSingleplayerServer();
        var id = mc.player.getUUID();
        if (srv != null) srv.execute(() -> {
            var sp = srv.getPlayerList().getPlayer(id);
            if (sp != null) {
                sp.stopRiding();
                f.accept(sp);
            }
        });
    }

    private static void lookAt(Minecraft mc, double x, double y, double z) {
        var p = mc.player;
        double dx = x - p.getX(), dy = y - p.getEyeY(), dz = z - p.getZ();
        p.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        p.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    private static void ride(Minecraft mc) {
        var v = mc.player.getVehicle();
        System.out.println("[AUTOTEST] client t=" + t + " ride=" + (v == null ? "none" : v.getType().toShortString())
                + String.format(" pos=%.1f,%.1f,%.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
    }

    /** 신규 콘텐츠: 몹·포탑, 낙하산, 전차, 전투기, 쌍권총, VSS, RPG */
    private static void later(Minecraft mc) {
        var o = mc.options;
        int s = phaseT;
        double g = -60;
        switch (s) {
            case 1 -> {
                o.setCameraType(CameraType.FIRST_PERSON);
                mc.player.setPos(32, g + 3, -32);
            }
            case 5 -> lookAt(mc, 45, g + 1, -45);
            case 40 -> shot(mc, "18_gunners_turret");
            case 50 -> {
                mc.player.setPos(20, g + 2, -44);
                lookAt(mc, 21, g + 2, -60);
            }
            case 90 -> shot(mc, "19_boss_spider");
            case 100 -> {
                o.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.player.setPos(0, g + 45, 0);
                mc.player.setDeltaMovement(0, 0, 0);
                mc.player.getAbilities().flying = false;
                mc.player.setXRot(20);
            }
            case 150 -> shot(mc, "20_parachute");
            case 175 -> {
                mc.player.setPos(10, g, -14.6);
                o.setCameraType(CameraType.FIRST_PERSON);
            }
            case 178 -> lookAt(mc, 10, g + 1.2, -20);
            case 182 -> KeyMapping.click(o.keyUse.getKey());
            case 195 -> {
                ride(mc);
                o.setCameraType(CameraType.THIRD_PERSON_BACK);
                key(o.keyUp, true);
                mc.player.setYRot(mc.player.getYRot() + 30);
            }
            case 240 -> shot(mc, "21_tank");
            case 245 -> KeyMapping.click(o.keyAttack.getKey());
            case 249 -> shot(mc, "22_tank_fire");
            case 265 -> key(o.keyUp, false);
            case 270 -> key(o.keyShift, true);
            case 274 -> key(o.keyShift, false);
            case 285 -> {
                o.setCameraType(CameraType.FIRST_PERSON);
                mc.player.setPos(-26.5, g, 32);
            }
            case 288 -> lookAt(mc, -30, g + 1.0, 30);
            case 292 -> KeyMapping.click(o.keyUse.getKey());
            case 305 -> {
                ride(mc);
                o.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.player.setYRot(180);
                mc.player.setXRot(0);
                key(o.keyUp, true);
            }
            case 400 -> mc.player.setXRot(-12);
            case 440 -> {
                shot(mc, "23_jet_takeoff");
                ride(mc);
            }
            case 470 -> mc.player.setXRot(-4);
            case 480 -> KeyMapping.click(o.keySwapOffhand.getKey());
            case 487 -> shot(mc, "24_jet_roll");
            case 510 -> {
                shot(mc, "24b_jet_flying");
                ride(mc);
            }
            case 515 -> KeyMapping.click(o.keyUse.getKey());
            case 519 -> shot(mc, "25_jet_missile");
            case 525 -> key(o.keyUp, false);
            case 530 -> key(o.keyShift, true);
            case 534 -> {
                key(o.keyShift, false);
                ride(mc);
            }
            case 555 -> {
                o.setCameraType(CameraType.FIRST_PERSON);
                mc.player.getInventory().selected = 2;
                mc.player.setXRot(5);
            }
            case 580 -> shot(mc, "26_dual_pistols");
            case 585 -> key(o.keyUse, true);
            case 590 -> shot(mc, "26b_dual_fire_a");
            case 591 -> shot(mc, "26c_dual_fire_b");
            case 600 -> key(o.keyUse, false);
            case 610 -> mc.player.getInventory().selected = 1;
            case 622 -> key(o.keyUse, true);
            case 644 -> shot(mc, "27_vss_scope");
            case 646 -> key(o.keyUse, false);
            case 655 -> KeyMapping.click(o.keySwapOffhand.getKey());
            case 665 -> key(o.keyUse, true);
            case 672 -> {
                shot(mc, "27b_vss_auto");
                System.out.println("[AUTOTEST] vss mode=" + com.tycooncraft.gun.GunItem.fireMode(mc.player.getMainHandItem()));
            }
            case 680 -> key(o.keyUse, false);
            case 690 -> mc.player.getInventory().selected = 3;
            case 705 -> shot(mc, "28_rpg");
            case 708 -> KeyMapping.click(o.keyUse.getKey());
            case 713 -> shot(mc, "29_rpg_fire");
            // ── v0.12: 자동 굴착기, 연막탄, 섬광탄 (통합 서버에서 직접 순간이동·지급) ──
            case 740 -> serverDo(mc, sp -> {
                var m = AutoTest.miner;
                double mx = m != null ? m.getX() : -12, mz = m != null ? m.getZ() : 22.5;
                System.out.println("[AUTOTEST] miner final" + (m == null ? " none" : String.format(" x=%.1f running=%s mined=%d", m.getX(), m.isRunning(), m.minedBlocks)));
                sp.teleportTo(sp.serverLevel(), mx - 3.5, g + 3.2, mz - 5.5, -30f, 22f);
            })
            case 775 -> shot(mc, "30_miner");
            case 780 -> serverDo(mc, sp -> {
                sp.getInventory().setItem(4, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.SMOKE_GRENADE.get(), 4));
                sp.getInventory().setItem(5, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.FLASH_GRENADE.get(), 4));
                sp.getInventory().selected = 4;
                sp.teleportTo(sp.serverLevel(), 0.5, g, 40.5, 0f, -10f);
            })
            case 790 -> mc.player.getInventory().selected = 4;
            case 805 -> shot(mc, "30b_grenade_held");
            case 808 -> KeyMapping.click(o.keyUse.getKey());
            case 880 -> {
                mc.player.setXRot(5);
                shot(mc, "31_smoke");
            }
            case 884 -> serverDo(mc, sp -> {
                sp.getInventory().selected = 5;
                sp.teleportTo(sp.serverLevel(), 0.5, g, 70.5, 0f, 20f);
            })
            case 892 -> mc.player.getInventory().selected = 5;
            case 900 -> KeyMapping.click(o.keyUse.getKey());
            case 953 -> shot(mc, "32_flash");
            case 958 -> System.out.println("[AUTOTEST] client flashed=" + mc.player.hasEffect(com.tycooncraft.registry.ModEffects.FLASHED));
            case 975 -> next(11);
            default -> {
            }
        }
    }

    private static void step(Minecraft mc) {
        var o = mc.options;
        if (t >= 1080) phases(mc);
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
            default -> {
            }
        }
    }

    private AutoTestClient() {}
}
