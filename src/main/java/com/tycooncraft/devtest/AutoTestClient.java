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

    private static net.minecraft.world.entity.LivingEntity judgeTarget;
    private static net.minecraft.world.entity.Entity display;

    /** 대상이 화면 가운데 오도록 거리 dist, 높이 up 만큼 떨어진 곳으로 이동 */
    private static void view(net.minecraft.server.level.ServerPlayer sp, net.minecraft.world.entity.LivingEntity e, double dist, double up, double g) {
        sp.removeAllEffects();
        if (e == null || !e.isAlive()) {
            System.out.println("[AUTOTEST] view target missing");
            return;
        }
        double ang = Math.toRadians(40);
        double x = e.getX() + Math.cos(ang) * dist, z = e.getZ() + Math.sin(ang) * dist;
        double y = Math.max(g, e.getY() - 2) + up;
        double dx = e.getX() - x, dz = e.getZ() - z, dy = (e.getY() + e.getBbHeight() * 0.5) - (y + 1.62);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        sp.getAbilities().flying = true;
        sp.onUpdateAbilities();
        sp.teleportTo(sp.serverLevel(), x, y, z, yaw, pitch);
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
            // 1인칭 조종석 시점: 수평 / 기수 올림 / 기수 내림
            case 520 -> {
                o.setCameraType(CameraType.FIRST_PERSON);
                mc.player.setXRot(0);
            }
            case 523 -> {
                shot(mc, "25b_jet_fp_level");
                var v = mc.player.getVehicle();
                if (v != null) System.out.printf("[AUTOTEST] jet fp eyeRel=%.2f,%.2f,%.2f xRot=%.1f%n", mc.player.getEyePosition().x - v.getX(),
                        mc.player.getEyePosition().y - v.getY(), mc.player.getEyePosition().z - v.getZ(), v.getXRot());
            }
            case 524 -> mc.player.setXRot(-25);
            case 534 -> {
                shot(mc, "25c_jet_fp_up");
                var v = mc.player.getVehicle();
                if (v != null) System.out.printf("[AUTOTEST] jet fp up eyeY=%.2f xRot=%.1f%n", mc.player.getEyePosition().y - v.getY(), v.getXRot());
            }
            case 535 -> mc.player.setXRot(20);
            case 545 -> {
                shot(mc, "25d_jet_fp_down");
                o.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 548 -> key(o.keyUp, false);
            case 549 -> key(o.keyShift, true);
            case 552 -> {
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
                sp.teleportTo(sp.serverLevel(), mx + 3, g + 3.0, mz - 5, 31f, 26f);
            });
            case 775 -> shot(mc, "30_miner");
            case 780 -> serverDo(mc, sp -> {
                sp.getInventory().setItem(4, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.SMOKE_GRENADE.get(), 4));
                sp.getInventory().setItem(5, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.FLASH_GRENADE.get(), 4));
                sp.getInventory().selected = 4;
                sp.teleportTo(sp.serverLevel(), 0.5, g, 40.5, 0f, -10f);
            });
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
            });
            case 892 -> mc.player.getInventory().selected = 5;
            case 900 -> KeyMapping.click(o.keyUse.getKey());
            case 953 -> shot(mc, "32_flash");
            case 958 -> System.out.println("[AUTOTEST] client flashed=" + mc.player.hasEffect(com.tycooncraft.registry.ModEffects.FLASHED));
            // ── 이계의 심판자: 고정된 허스크에 두 발 → 표식 → 심판 ──
            case 965 -> serverDo(mc, sp -> {
                sp.removeAllEffects();
                var judge = new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.JUDGE.get());
                judge.set(com.tycooncraft.registry.ModComponents.AMMO.get(), 9);
                sp.getInventory().setItem(6, judge);
                sp.getInventory().selected = 6;
                sp.teleportTo(sp.serverLevel(), 0.5, g, 100.5, 0f, 0f);
                var h = net.minecraft.world.entity.EntityType.HUSK.create(sp.serverLevel());
                h.moveTo(0.5, g, 106.5, 180, 0);
                h.setNoAi(true);
                h.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
                h.setHealth(200);
                sp.serverLevel().addFreshEntity(h);
                judgeTarget = h;
            });
            case 975 -> mc.player.getInventory().selected = 6;
            case 990 -> {
                mc.player.setXRot(4);
                shot(mc, "33_judge");
            }
            case 992 -> KeyMapping.click(o.keyUse.getKey());
            case 994 -> shot(mc, "33b_judge_fire");
            case 1002 -> KeyMapping.click(o.keyUse.getKey());
            case 1004 -> shot(mc, "33c_judgment");
            case 1012 -> serverDo(mc, sp -> System.out.println("[AUTOTEST] judge husk hp=" + (judgeTarget == null ? -1 : judgeTarget.getHealth())
                    + " judgments=" + com.tycooncraft.gun.JudgeItem.judgments));
            // ── v0.13: 보스 3종 관찰 ──
            case 1022 -> serverDo(mc, sp -> view(sp, AutoTest.golem, 9, 4, g));
            case 1060 -> shot(mc, "34_golem");
            // ── 총 쥐는 자세: 1인칭 팔 + 3인칭 ──
            case 1146 -> serverDo(mc, sp -> {
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
                sp.teleportTo(sp.serverLevel(), 0.5, g, -60.5, 180f, 0f);
                sp.getInventory().setItem(7, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.RIFLE.get()));
                sp.getInventory().setItem(8, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.PISTOL.get()));
            });
            case 1160 -> {
                mc.player.getInventory().selected = 7;
                o.setCameraType(CameraType.FIRST_PERSON);
            }
            case 1180 -> shot(mc, "37_fp_rifle");
            case 1182 -> o.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 1192 -> shot(mc, "37b_tp_rifle");
            case 1194 -> {
                o.setCameraType(CameraType.FIRST_PERSON);
                mc.player.getInventory().selected = 8;
            }
            case 1214 -> shot(mc, "38_fp_pistol");
            case 1216 -> o.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 1226 -> shot(mc, "38b_tp_pistol");
            case 1228 -> {
                o.setCameraType(CameraType.FIRST_PERSON);
                mc.player.getInventory().selected = 1;
            }
            case 1245 -> shot(mc, "39_fp_vss");
            case 1247 -> o.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 1257 -> shot(mc, "39b_tp_vss");
            case 1259 -> o.setCameraType(CameraType.FIRST_PERSON);
            case 1264 -> key(o.keyUse, true);
            case 1268 -> shot(mc, "39c_fp_vss_fire");
            case 1274 -> key(o.keyUse, false);
            case 1282 -> serverDo(mc, sp -> view(sp, AutoTest.wither, 12, 3, g));
            case 1315 -> shot(mc, "40_wither");
            case 1318 -> serverDo(mc, sp -> view(sp, AutoTest.dragon, 30, 6, g));
            case 1350 -> shot(mc, "41_dragon");
            case 1356 -> serverDo(mc, sp -> view(sp, AutoTest.wither, 6, 0.5, g));
            case 1385 -> shot(mc, "40b_wither_close");
            case 1388 -> serverDo(mc, sp -> view(sp, AutoTest.dragon, 18, 3, g));
            case 1420 -> shot(mc, "41b_dragon_close");
            // ── 외형 확인용 정지 개체 촬영 ──
            case 1428 -> serverDo(mc, sp -> {
                var sl = sp.serverLevel();
                var w = net.minecraft.world.entity.EntityType.WITHER.create(sl);
                w.moveTo(0.5, g + 2.5, 92.5, 0, 0);
                w.setYHeadRot(0);
                w.setNoAi(true);
                sl.addFreshEntity(w);
                display = w;
                sp.removeAllEffects();
                sp.teleportTo(sl, 0.5, g + 3.2, 100.5, 180f, 4f);
            });
            case 1452 -> shot(mc, "42_wither_portrait");
            case 1454 -> serverDo(mc, sp -> sp.teleportTo(sp.serverLevel(), 6.5, g + 4.5, 98.5, 140f, 12f));
            case 1472 -> shot(mc, "42b_wither_angle");
            case 1474 -> serverDo(mc, sp -> {
                var sl = sp.serverLevel();
                if (display != null) display.discard();
                var d = net.minecraft.world.entity.EntityType.ENDER_DRAGON.create(sl);
                d.moveTo(0.5, g + 7, 70.5, 0, 0);
                d.setNoAi(true);
                sl.addFreshEntity(d);
                display = d;
                sp.teleportTo(sl, 14.5, g + 10, 88.5, 142f, 14f);
            });
            case 1500 -> shot(mc, "43_dragon_portrait");
            case 1502 -> serverDo(mc, sp -> sp.teleportTo(sp.serverLevel(), -12.5, g + 12, 62.5, -125f, 18f));
            case 1522 -> shot(mc, "43b_dragon_side");
            // ── v0.14.2 드래곤 신규 패턴 촬영 ──
            case 1526 -> serverDo(mc, sp -> {
                var sl = sp.serverLevel();
                if (display != null) display.discard();
                sp.getAbilities().invulnerable = true;
                var d = net.minecraft.world.entity.EntityType.ENDER_DRAGON.create(sl);
                d.moveTo(0.5, g + 18, 64.5, 0, 0);
                sl.addFreshEntity(d);
                display = d;
                AutoTest.dummyVillagerAt(sl, 0.5, 44.5);
                sp.teleportTo(sl, 26.5, g + 9, 48.5, 110f, 8f);
            });
            case 1532 -> serverDo(mc, sp -> com.tycooncraft.mob.VanillaBossBuffs.forceMove((net.minecraft.world.entity.boss.enderdragon.EnderDragon) display, 1));
            case 1550 -> shot(mc, "44_dragon_rush_charge");
            case 1562 -> shot(mc, "44b_dragon_rush");
            case 1590 -> serverDo(mc, sp -> com.tycooncraft.mob.VanillaBossBuffs.forceMove((net.minecraft.world.entity.boss.enderdragon.EnderDragon) display, 2));
            case 1618 -> shot(mc, "45_dragon_dive_rise");
            case 1631 -> shot(mc, "45b_dragon_dive_impact");
            case 1650 -> serverDo(mc, sp -> com.tycooncraft.mob.VanillaBossBuffs.forceMove((net.minecraft.world.entity.boss.enderdragon.EnderDragon) display, 3));
            case 1680 -> shot(mc, "46_dragon_sweep");
            case 1712 -> serverDo(mc, sp -> ((net.minecraft.world.entity.LivingEntity) display).setHealth(((net.minecraft.world.entity.LivingEntity) display).getMaxHealth() * 0.45f));
            case 1750 -> shot(mc, "47_dragon_awaken_charge");
            case 1773 -> shot(mc, "47b_dragon_awaken_blast");
            case 1790 -> shot(mc, "47c_dragon_red_lightning");
            case 1800 -> serverDo(mc, sp -> com.tycooncraft.mob.VanillaBossBuffs.forceMove((net.minecraft.world.entity.boss.enderdragon.EnderDragon) display, 1));
            case 1836 -> shot(mc, "48_dragon_p2_rush");
            case 1860 -> {
                serverDo(mc, sp -> System.out.println("[AUTOTEST] dragon moves dstats=" + java.util.Arrays.toString(com.tycooncraft.mob.VanillaBossBuffs.DSTATS)));
                if (display != null) serverDo(mc, sp -> display.discard());
            }
            // ── v0.15: 공장(높인 컨베이어·투입구) 촬영 ──
            case 1866 -> serverDo(mc, sp -> sp.teleportTo(sp.serverLevel(), 101.5, g + 3.2, 144.5, 200f, 30f));
            case 1890 -> shot(mc, "50_factory");
            case 1892 -> serverDo(mc, sp -> sp.teleportTo(sp.serverLevel(), 107.5, g + 1.6, 141.2, 155f, 25f));
            case 1910 -> shot(mc, "50b_factory_close");
            // ── 배: 갑판 위에 서서 출항 ──
            case 1912 -> serverDo(mc, sp -> {
                var sl = sp.serverLevel();
                // 수조 안 새 배 (서버 테스트가 끝난 뒤 다시 지어서 조립)
                for (int x = 100; x <= 124; x++) for (int z = 100; z <= 124; z++) for (int y = -1; y <= 3; y++) {
                    var p = new net.minecraft.core.BlockPos(x, (int) g + y, z);
                    if (y == -1) sl.setBlock(p, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 2);
                    else if (!sl.getBlockState(p).isAir() && !(sl.getBlockState(p).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) sl.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
                V15Test.buildShipAt(sl, 0);
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
                sp.teleportTo(sl, 111.5, g + 0.2, 109.5, 0f, 10f);
            });
            case 1925 -> serverDo(mc, sp -> V15Test.assembleClientShip(sp.serverLevel()));
            case 1935 -> V15Test.logClient(mc, "start");
            case 1940 -> serverDo(mc, sp -> V15Test.clientShipInput(1, 0));
            case 1975 -> shot(mc, "51_ship_deck");
            case 1990 -> V15Test.logClient(mc, "sailing");
            case 1995 -> serverDo(mc, sp -> V15Test.clientShipInput(0.7f, 1));
            case 2030 -> {
                V15Test.logClient(mc, "turning");
                o.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 2045 -> shot(mc, "52_ship_turn_tp");
            case 2047 -> o.setCameraType(CameraType.FIRST_PERSON);
            case 2050 -> serverDo(mc, sp -> V15Test.clientShipInput(0, 0));
            case 2054 -> serverDo(mc, sp -> {
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
                var cs = V15Test.clientShip;
                if (cs != null) sp.teleportTo(sp.serverLevel(), cs.getX() + 9, g + 6, cs.getZ() - 7, 50f, 25f);
            });
            case 2075 -> shot(mc, "53_ship_outside");
            case 2110 -> serverDo(mc, sp -> V15Test.dockClientShip(sp));
            case 2125 -> shot(mc, "54_ship_docked");
            // ── v0.15.2: 눈에 보이는 총알 + 로켓 방향 ──
            case 2130 -> serverDo(mc, sp -> {
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
                sp.teleportTo(sp.serverLevel(), 60.5, g + 3, 60.5, 90f, 0f);   // 서쪽(-X)을 봄
                sp.getInventory().selected = 2;
            });
            case 2140 -> serverDo(mc, sp -> {
                var sl = sp.serverLevel();
                // 옆에서 보이는 사격선: 동쪽으로 날아가는 로켓/포탄 + 총알 연사
                for (int i = 0; i < 3; i++) {
                    var r = com.tycooncraft.entity.RocketEntity.create(com.tycooncraft.registry.ModEntities.ROCKET.get(), sl, null,
                            new net.minecraft.world.phys.Vec3(48 + i * 2, g + 2.5 + i * 0.8, 52), new net.minecraft.world.phys.Vec3(0.9, 0.05 * i, 0), i, 0.5f);
                    sl.addFreshEntity(r);
                }
                var from = new net.minecraft.world.phys.Vec3(46, g + 1.6, 66);
                for (int i = 0; i < 6; i++)
                    com.tycooncraft.gun.Ballistics.shoot(sl, null, from.add(0, i * 0.15, 0), new net.minecraft.world.phys.Vec3(1, 0.01 * i, 0), 60, 0, 0, 1f, false, true,
                            sl.damageSources().generic(), e -> false);
            });
            case 2141 -> shot(mc, "55_bullets_rockets");
            case 2143 -> V15Test.logClientFx();
            // ── v0.16: 갱단 무장 트럭 ──
            case 2150 -> serverDo(mc, sp -> {
                var sl = sp.serverLevel();
                V16Test.clientTruck = com.tycooncraft.registry.ModEntities.GANG_TRUCK.get().spawn(sl, new net.minecraft.core.BlockPos(-20, (int) g, 22), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED);
                V16Test.clientTarget = AutoTest.dummyAt(sl, -20.5, 50.5);
                sp.teleportTo(sl, -38.5, g + 5, 36.5, -108f, 16f);
            });
            case 2165 -> shot(mc, "56_gang_truck");
            case 2166, 2180, 2200, 2220 -> serverDo(mc, sp -> {
                if (V16Test.clientTruck != null && V16Test.clientTruck.isAlive()) V16Test.clientTruck.setTarget(V16Test.clientTarget);
            });
            case 2205 -> shot(mc, "57_gang_truck_chase");
            case 2170, 2230 -> serverDo(mc, sp -> {
                if (V16Test.clientTruck != null) { V16Test.crew0.clear(); V16Test.diag("client", sp.serverLevel(), V16Test.clientTruck); }
            });
            case 2235 -> shot(mc, "58_gang_truck_firing");
            case 2240 -> serverDo(mc, sp -> {
                if (V16Test.clientTruck != null) V16Test.clientTruck.discard();
            });
            case 2245 -> serverDo(mc, sp -> com.tycooncraft.world.StructureBuilder.hideout(sp.serverLevel(), new net.minecraft.core.BlockPos(-70, (int) g, -10)));
            case 2250 -> serverDo(mc, sp -> sp.teleportTo(sp.serverLevel(), -27.5, g + 17, 33.5, 135f, 28f));
            case 2290 -> shot(mc, "59_gang_hideout");
            case 2292 -> serverDo(mc, sp -> sp.teleportTo(sp.serverLevel(), -54.5, g + 6, 33.5, 180f, 14f));
            case 2325 -> shot(mc, "59b_gang_hideout_gate");
            // ── v0.16: 산업 (신규 기계·광석 + 아이템 액자) ──
            case 2330 -> serverDo(mc, sp -> {
                var sl = sp.serverLevel();
                int y = (int) g, z = -40;
                net.minecraft.world.level.block.state.BlockState[] row = {
                        com.tycooncraft.registry.ModBlocks.machine(com.tycooncraft.machine.MachineType.CHEMICAL_REACTOR).defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH),
                        com.tycooncraft.registry.ModBlocks.machine(com.tycooncraft.machine.MachineType.ELECTROLYZER).defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH),
                        com.tycooncraft.registry.ModBlocks.BAUXITE_ORE.get().defaultBlockState(),
                        com.tycooncraft.registry.ModBlocks.DEEPSLATE_BAUXITE_ORE.get().defaultBlockState(),
                        com.tycooncraft.registry.ModBlocks.ROCK_SALT_ORE.get().defaultBlockState()};
                for (int i = 0; i < row.length; i++) sl.setBlockAndUpdate(new net.minecraft.core.BlockPos(-24 + i * 2, y, z), row[i]);
                // 아이템 액자 벽 (10×3)
                net.minecraft.world.item.Item[] show = {
                        com.tycooncraft.registry.ModItems.RAW_BAUXITE.get(), com.tycooncraft.registry.ModItems.ALUMINA.get(), com.tycooncraft.registry.ModItems.ALUMINUM_INGOT.get(),
                        com.tycooncraft.registry.ModItems.ALUMINUM_FOIL.get(), com.tycooncraft.registry.ModItems.COKE.get(), com.tycooncraft.registry.ModItems.GRAPHITE.get(),
                        com.tycooncraft.registry.ModItems.SULFURIC_ACID.get(), com.tycooncraft.registry.ModItems.CHLORINE_CYLINDER.get(), com.tycooncraft.registry.ModItems.EPOXY_RESIN.get(),
                        com.tycooncraft.registry.ModItems.RUBBER.get(), com.tycooncraft.registry.ModItems.BALL_BEARING.get(), com.tycooncraft.registry.ModItems.VALVE.get(),
                        com.tycooncraft.registry.ModItems.HYDRAULIC_CYLINDER.get(), com.tycooncraft.registry.ModItems.COPPER_COIL.get(), com.tycooncraft.registry.ModItems.ELECTRIC_MOTOR.get(),
                        com.tycooncraft.registry.ModItems.GEARBOX.get(), com.tycooncraft.registry.ModItems.SILICON_WAFER.get(), com.tycooncraft.registry.ModItems.MICROCHIP.get(),
                        com.tycooncraft.registry.ModItems.PCB.get(), com.tycooncraft.registry.ModItems.CONTROL_BOARD.get(), com.tycooncraft.registry.ModItems.BATTERY_CELL.get(),
                        com.tycooncraft.registry.ModItems.SOLAR_PANEL.get(), com.tycooncraft.registry.ModItems.TRANSFORMER.get(), com.tycooncraft.registry.ModItems.INDUSTRIAL_ROBOT_ARM.get(),
                        com.tycooncraft.registry.ModItems.H_BEAM.get(), com.tycooncraft.registry.ModItems.PRECAST_PANEL.get(), com.tycooncraft.registry.ModItems.CEMENT.get(),
                        com.tycooncraft.registry.ModItems.STEEL_PIPE.get(), com.tycooncraft.registry.ModItems.INSULATED_CABLE.get(), com.tycooncraft.registry.ModItems.GAS_CYLINDER.get()};
                for (int i = 0; i < show.length; i++) {
                    var wall = new net.minecraft.core.BlockPos(-24 + i % 10, y + 3 - i / 10, z - 3);
                    sl.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                    var frame = new net.minecraft.world.entity.decoration.ItemFrame(sl, wall.south(), net.minecraft.core.Direction.SOUTH);
                    frame.setItem(new net.minecraft.world.item.ItemStack(show[i]), false);
                    sl.addFreshEntity(frame);
                }
                sp.teleportTo(sl, -19.5, g + 2.2, z + 5.5, 180f, 12f);
            });
            case 2350 -> shot(mc, "60_industry_items");
            case 2352 -> serverDo(mc, sp -> sp.teleportTo(sp.serverLevel(), -19.5, g + 1.6, -36.5, 180f, 10f));
            case 2370 -> shot(mc, "61_industry_machines");
            // ── v0.16: 부유체 운송기 (땅 위에서 떠오름) ──
            case 2375 -> serverDo(mc, sp -> {
                AirshipTest.clientBuild(sp.serverLevel(), (int) g);
                sp.teleportTo(sp.serverLevel(), 31.5, g + 4, -52.5, 150f, 8f);
            });
            case 2378, 2390, 2400, 2410, 2420, 2430 -> serverDo(mc, sp -> AirshipTest.clientInput(0, 1));
            case 2380 -> shot(mc, "62_airship_ground");
            case 2440 -> serverDo(mc, sp -> AirshipTest.clientInput(0, 0));
            case 2441 -> serverDo(mc, sp -> sp.teleportTo(sp.serverLevel(), 33.5, g + 3, -50.5, 148f, -12f));
            case 2445 -> shot(mc, "63_airship_flying");
            case 2450 -> serverDo(mc, sp -> {
                if (AirshipTest.clientShip != null) System.out.printf("[AUTOTEST] v16 clientair y=%.2f fuel=%d%n", AirshipTest.clientShip.getY() - g, AirshipTest.clientShip.fuel());
            });
            // ── v0.16: 방공 요격 포대 ──
            case 2455 -> serverDo(mc, sp -> {
                CiwsTest.clientSetup(sp.serverLevel(), (int) g);
                sp.teleportTo(sp.serverLevel(), 7.5, g + 2.5, -80.5, 124f, -8f);
            });
            case 2462, 2470, 2478, 2486 -> serverDo(mc, sp -> CiwsTest.clientRocket(sp.serverLevel(), (int) g));
            case 2468 -> shot(mc, "64_ciws_tracking");
            case 2476 -> shot(mc, "65_ciws_intercept");
            case 2500 -> serverDo(mc, sp -> System.out.println("[AUTOTEST] v16 clientciws kills=" + com.tycooncraft.turret.Interceptor.KILLS + " shots=" + com.tycooncraft.turret.Interceptor.SHOTS));
            // ── v0.16.7: 조종석 1인칭 — 타륜을 잡은 손, 왼쪽으로 감기 ──
            case 2505 -> serverDo(mc, sp -> AirshipTest.clientSeat(sp, (int) g));
            case 2515 -> {
                mc.player.setYRot(0f);
                mc.player.setXRot(18f);
                mc.options.keyLeft.setDown(true);
            }
            case 2540 -> shot(mc, "66_helm_first_person_left");
            case 2542 -> {
                mc.options.keyLeft.setDown(false);
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            }
            case 2560 -> shot(mc, "67_helm_seat_third_person");
            case 2562 -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                serverDo(mc, sp -> sp.stopRiding());
            }
            case 2569 -> serverDo(mc, sp -> System.out.printf("[AUTOTEST] v16 dismount onDeck=%s y=%.2f shipY=%.2f%n",
                    AirshipTest.seatShip != null && AirshipTest.seatShip.standingOn(sp, 0.3), sp.getY(), AirshipTest.seatShip == null ? 0 : AirshipTest.seatShip.getY()));
            // ── v0.16.9: 왼손 총 금지 ──
            case 2571 -> serverDo(mc, sp -> sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.RPG.get())));
            case 2580 -> serverDo(mc, sp -> System.out.println("[AUTOTEST] v16 offhand rpgStillInOffhand=" + sp.getOffhandItem().is(com.tycooncraft.registry.ModItems.RPG.get())
                    + " rpgInBag=" + sp.getInventory().contains(new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.RPG.get())) + " blocked=" + com.tycooncraft.gun.GunEvents.OFFHAND_BLOCKED));
            // ── v0.17: 총기 파츠 모습 + 바지·전투화 ──
            case 2585 -> serverDo(mc, sp -> {
                var sl = sp.serverLevel();
                int y = (int) g, z = -52;
                Object[][] guns = {{com.tycooncraft.registry.ModItems.RIFLE.get(), new com.tycooncraft.gun.GunPart[]{com.tycooncraft.gun.GunPart.SCOPE, com.tycooncraft.gun.GunPart.VERTICAL_GRIP, com.tycooncraft.gun.GunPart.EXTENDED_MAG, com.tycooncraft.gun.GunPart.HEAVY_BARREL, com.tycooncraft.gun.GunPart.BRASS_CATCHER}},
                        {com.tycooncraft.registry.ModItems.SMG.get(), new com.tycooncraft.gun.GunPart[]{com.tycooncraft.gun.GunPart.RED_DOT, com.tycooncraft.gun.GunPart.VERTICAL_GRIP, com.tycooncraft.gun.GunPart.MAG_COUPLER}},
                        {com.tycooncraft.registry.ModItems.PISTOL.get(), new com.tycooncraft.gun.GunPart[]{com.tycooncraft.gun.GunPart.RED_DOT, com.tycooncraft.gun.GunPart.EXTENDED_MAG, com.tycooncraft.gun.GunPart.RAPID_TRIGGER}},
                        {com.tycooncraft.registry.ModItems.SHOTGUN.get(), new com.tycooncraft.gun.GunPart[]{com.tycooncraft.gun.GunPart.RED_DOT, com.tycooncraft.gun.GunPart.EXTENDED_MAG, com.tycooncraft.gun.GunPart.HEAVY_BARREL}},
                        {com.tycooncraft.registry.ModItems.SNIPER.get(), new com.tycooncraft.gun.GunPart[]{com.tycooncraft.gun.GunPart.VERTICAL_GRIP, com.tycooncraft.gun.GunPart.EXTENDED_MAG, com.tycooncraft.gun.GunPart.BRASS_CATCHER}},
                        {com.tycooncraft.registry.ModItems.RIFLE.get(), new com.tycooncraft.gun.GunPart[]{}}};
                for (int i = 0; i < guns.length; i++) {
                    var wall = new net.minecraft.core.BlockPos(-24 + (i % 3) * 2, y + 2 - (i / 3) * 2, z);
                    sl.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                    var st = new net.minecraft.world.item.ItemStack((net.minecraft.world.item.Item) guns[i][0]);
                    for (var p : (com.tycooncraft.gun.GunPart[]) guns[i][1]) com.tycooncraft.gun.GunParts.install(st, p);
                    var frame = new net.minecraft.world.entity.decoration.ItemFrame(sl, wall.south(), net.minecraft.core.Direction.SOUTH);
                    frame.setItem(st, false);
                    sl.addFreshEntity(frame);
                }
                sp.teleportTo(sl, -21.5, g + 1.8, z + 3.6, 180f, 8f);
            });
            case 2605 -> shot(mc, "68_gun_parts_wall");
            case 2607 -> serverDo(mc, sp -> {
                var st = new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.RIFLE.get());
                for (var p : new com.tycooncraft.gun.GunPart[]{com.tycooncraft.gun.GunPart.SCOPE, com.tycooncraft.gun.GunPart.VERTICAL_GRIP, com.tycooncraft.gun.GunPart.EXTENDED_MAG, com.tycooncraft.gun.GunPart.HEAVY_BARREL})
                    com.tycooncraft.gun.GunParts.install(st, p);
                sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, st);
                sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.HELMET_3.get()));
                sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.VEST_3.get()));
                sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.PANTS_3.get()));
                sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.BOOTS_3.get()));
                sp.teleportTo(sp.serverLevel(), -21.5, g + 1, -45.5, 0f, 10f);
            });
            case 2620 -> shot(mc, "69_parts_first_person");
            case 2622 -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            case 2640 -> shot(mc, "70_armor_set3_third_person");
            case 2642 -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                serverDo(mc, sp -> {
                    sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.PANTS_1.get()));
                    sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.BOOTS_2.get()));
                    sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.VEST_1.get()));
                    sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(com.tycooncraft.registry.ModItems.HELMET_1.get()));
                });
            }
            case 2645 -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            case 2665 -> shot(mc, "71_armor_set1_third_person");
            case 2667 -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                serverDo(mc, sp -> {
                    for (var slot : net.minecraft.world.entity.EquipmentSlot.values())
                        if (slot.isArmor()) sp.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
                    sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                });
            }
            // ── v0.18: 은행 (저축·송금·경매) ──
            case 2670 -> serverDo(mc, BankTest::run);
            case 2674 -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            case 2682 -> mc.getToasts().clear();
            case 2684 -> shot(mc, "72_inventory_bank_button");
            case 2686 -> com.tycooncraft.client.BankClient.open();
            case 2690 -> { if (mc.screen instanceof com.tycooncraft.client.BankScreen b) b.demo(0, 0); }
            case 2698 -> shot(mc, "73_bank_account");
            case 2700 -> { if (mc.screen instanceof com.tycooncraft.client.BankScreen b) b.demo(1, 0); }
            case 2706 -> shot(mc, "74_bank_transfer");
            case 2708 -> { if (mc.screen instanceof com.tycooncraft.client.BankScreen b) b.demo(2, 0); }
            case 2716 -> shot(mc, "75_bank_auction");
            case 2718 -> { if (mc.screen instanceof com.tycooncraft.client.BankScreen b) b.demo(2, 1); }
            case 2726 -> shot(mc, "76_bank_auction_sell");
            case 2728 -> {
                System.out.println("[AUTOTEST] v18 screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName())
                        + " synced=" + (com.tycooncraft.client.BankClient.last != null) + " auctions=" + (com.tycooncraft.client.BankClient.last == null ? -1 : com.tycooncraft.client.BankClient.last.auctions().size()));
                mc.setScreen(null);
            }
            // ── v0.18: 폐공장 지하실 + 골렘, 갱단 드롭, 스코프 전환 ──
            case 2732 -> serverDo(mc, V18Test::build);
            case 2748 -> mc.getToasts().clear();
            case 2750 -> shot(mc, "77_ruin_basement_golem");
            case 2752 -> serverDo(mc, V18Test::kill);
            case 2760 -> serverDo(mc, V18Test::check);
            // ── v0.18: 총기 제작대 ──
            case 2764 -> serverDo(mc, GunWorkbenchTest::run);
            case 2770 -> mc.getToasts().clear();
            case 2766 -> {
                int rifle = 0;
                for (int i = 0; i < com.tycooncraft.gun.GunCrafting.all().size(); i++)
                    if (com.tycooncraft.gun.GunCrafting.all().get(i).result().is(com.tycooncraft.registry.ModItems.RIFLE.get())) rifle = i;
                com.tycooncraft.client.GunWorkbenchScreen.select(0, rifle);
            }
            case 2774 -> shot(mc, "78_gun_workbench");
            case 2775 -> com.tycooncraft.client.GunWorkbenchScreen.select(3, -1);
            case 2777 -> shot(mc, "78b_gun_workbench_parts");
            case 2778 -> { if (mc.player != null) mc.player.closeContainer(); }
            case 2779 -> serverDo(mc, GunWorkbenchTest::showcase);
            case 2790 -> mc.getToasts().clear();
            case 2794 -> shot(mc, "80_gun_workbench_block");
            case 2795 -> serverDo(mc, GunWorkbenchTest::showItems);
            case 2799 -> shot(mc, "81_new_item_textures");
            case 2801 -> { if (mc.player != null) mc.player.closeContainer(); }
            // ── v0.18.3: 트럭 운전수, 쌍권총, 경찰 방패 ──
            case 2803 -> serverDo(mc, V183Test::setup);
            case 2880 -> serverDo(mc, V183Test::check);
            case 2888 -> mc.getToasts().clear();
            case 2892 -> shot(mc, "79_police_shield");
            case 2894 -> serverDo(mc, V183Test::cleanup);
            case 2898 -> next(11);
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
