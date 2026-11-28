package com.tycooncraft.devtest;

import com.tycooncraft.client.gun.GunKind;
import com.tycooncraft.client.gun.GunViewModel;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.registry.ModItems;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * v0.32: 1인칭 총 동작 확인. 총마다 꺼내기·정지·쏜 직후·재장전 단계별 화면을 찍고,
 * 실제로 쏘고 재장전해서 탄피·소리 일정이 도는지 확인한다.
 */
public final class GunAnimTestClient {
    record Step(String label, float reloadU, float shotAgo, boolean empty) {}

    static final GunKind[] KINDS = System.getenv("TC_AXISTEST") != null ? new GunKind[]{GunKind.PISTOL, GunKind.RIFLE} : new GunKind[]{GunKind.PISTOL, GunKind.SMG, GunKind.RIFLE, GunKind.SHOTGUN, GunKind.SNIPER, GunKind.VSS, GunKind.RT20, GunKind.RPG, GunKind.DUAL, GunKind.JUDGE};

    static Item item(GunKind k) {
        return switch (k) {
            case PISTOL -> ModItems.PISTOL.get();
            case JUDGE -> ModItems.JUDGE.get();
            case SMG -> ModItems.SMG.get();
            case RIFLE -> ModItems.RIFLE.get();
            case SHOTGUN -> ModItems.SHOTGUN.get();
            case SNIPER -> ModItems.SNIPER.get();
            case VSS -> ModItems.VSS.get();
            case RT20 -> ModItems.RT20.get();
            case RPG -> ModItems.RPG.get();
            case DUAL -> ModItems.DUAL_PISTOLS.get();
        };
    }

    static Step[] steps(GunKind k) {
        java.util.List<Step> l = new java.util.ArrayList<>();
        if (System.getenv("TC_AXISTEST") != null) {
            l.add(new Step("base", -1, 9, false));
            l.add(new Step("yawP", -1, 9, false));
            l.add(new Step("pitchP", -1, 9, false));
            l.add(new Step("rollP", -1, 9, false));
            l.add(new Step("txP", -1, 9, false));
            return l.toArray(new Step[0]);
        }
        l.add(new Step("idle", -1, 9, false));
        switch (k) {
            case PISTOL, JUDGE, DUAL -> l.add(new Step("slide", -1, 0.02f, false));
            case SHOTGUN -> l.add(new Step("pump", -1, 0.26f, false));
            case SNIPER -> {
                l.add(new Step("bolt_up", -1, 0.4f, false));
                l.add(new Step("bolt_back", -1, 0.6f, false));
            }
            case RIFLE -> l.add(new Step("shot", -1, 0.01f, false));
            default -> {}
        }
        float[] us = k == GunKind.SHOTGUN ? new float[]{0.1f, 0.25f, 0.36f, 0.45f, 0.89f}
                : k == GunKind.RT20 ? new float[]{0.06f, 0.09f, 0.16f, 0.22f, 0.25f, 0.29f}
                : k == GunKind.RPG ? new float[]{0.12f, 0.3f, 0.5f, 0.64f, 0.74f}
                : k == GunKind.DUAL ? new float[]{0.12f, 0.2f, 0.32f, 0.6f, 0.79f}
                : new float[]{0.1f, 0.2f, 0.3f, 0.46f, 0.6f, 0.72f, 0.77f};
        for (float u : us) l.add(new Step(String.format("r%02d", Math.round(u * 100)), u, 9, true));
        return l.toArray(new Step[0]);
    }

    static int gi, si, wait;
    static boolean started;

    /** true 를 돌려주면 끝 */
    public static boolean tick(Minecraft mc, int phaseT) {
        var o = mc.options;
        if (!started) {
            started = true;
            gi = 0;
            si = -2;
            wait = 0;
            o.setCameraType(CameraType.FIRST_PERSON);
            o.hideGui = false;
            AutoTestClient.serverKeepPub(mc, sp -> {
                var sl = sp.serverLevel();
                sl.setDayTime(6000);
                sp.stopRiding();
                int y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 260, 260);
                sp.teleportTo(sl, 260.5, y, 260.5, 30f, 4f);
                sp.getAbilities().flying = false;
            });
            return false;
        }
        if (wait-- > 0) return false;
        if (gi >= KINDS.length) {
            live(mc, phaseT);
            return liveDone;
        }
        GunKind k = KINDS[gi];
        Step[] st = steps(k);
        if (si == -2) {
            GunViewModel.DEBUG_RELOAD_U = -1;
            GunViewModel.DEBUG_SHOT_AGO = -1;
            GunViewModel.DEBUG_EMPTY = false;
            Item it = item(k);
            AutoTestClient.serverKeepPub(mc, sp -> {
                ItemStack s = new ItemStack(it);
                if (it instanceof GunItem g) GunItem.setAmmo(s, g.magazine(s));
                sp.stopRiding();
                sp.getInventory().selected = 0;
                sp.getInventory().setItem(0, s);
                sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                sp.setXRot(4f);
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
            });
            si = -1;
            wait = 3;
            return false;
        }
        if (si == -1) {   // 꺼내는 중 한 장
            AutoTestClient.shotPub(mc, "anim_" + k.name().toLowerCase() + "_0equip");
            si = 0;
            wait = 18;
            return false;
        }
        if (si < st.length) {
            Step s = st[si];
            if (!pending) {
                GunViewModel.DEBUG_RELOAD_U = s.reloadU;
                GunViewModel.DEBUG_SHOT_AGO = s.shotAgo;
                GunViewModel.DEBUG_EMPTY = s.empty;
                java.util.Arrays.fill(GunViewModel.DEBUG_EXTRA, 0);
                switch (s.label) {
                    case "yawP" -> GunViewModel.DEBUG_EXTRA[4] = 30;
                    case "pitchP" -> GunViewModel.DEBUG_EXTRA[3] = 30;
                    case "rollP" -> GunViewModel.DEBUG_EXTRA[5] = 30;
                    case "txP" -> GunViewModel.DEBUG_EXTRA[0] = 0.15f;
                    default -> {}
                }
                pending = true;
                wait = 2;
                return false;
            }
            AutoTestClient.shotPub(mc, "anim_" + k.name().toLowerCase() + "_" + (si + 1) + s.label);
            pending = false;
            si++;
            return false;
        }
        GunViewModel.DEBUG_RELOAD_U = -1;
        GunViewModel.DEBUG_SHOT_AGO = -1;
        GunViewModel.DEBUG_EMPTY = false;
        gi++;
        si = -2;
        return false;
    }

    static boolean pending, liveDone;
    static int liveT;

    /** 실제로 쏘고 재장전 (탄피·소리 일정·서버 동기화 경로) */
    static void live(Minecraft mc, int phaseT) {
        var o = mc.options;
        liveT++;
        if (liveT > 316 && liveT < 376) AutoTestClient.serverKeepPub(mc, sp -> helmDrive());
        switch (liveT) {
            case 1 -> AutoTestClient.serverKeepPub(mc, sp -> {
                ItemStack s = new ItemStack(ModItems.PISTOL.get());
                GunItem.setAmmo(s, 2);
                sp.getInventory().setItem(sp.getInventory().selected, s);
                sp.getInventory().add(new ItemStack(ModItems.LIGHT_AMMO.get(), 32));
                sp.stopRiding();
                sp.teleportTo(sp.serverLevel(), sp.getX(), sp.getY() + 30, sp.getZ(), sp.getYRot(), -10f);
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
            });
            case 25 -> KeyMapping.click(o.keyUse.getKey());
            case 26 -> AutoTestClient.shotPub(mc, "anim_live_1shot");
            case 27 -> AutoTestClient.shotPub(mc, "anim_live_2casing");
            case 28 -> AutoTestClient.shotPub(mc, "anim_live_2casingb");
            case 34 -> {
                for (int i = 0; i < 5; i++)
                    GunViewModel.spawnCasing(GunKind.values()[i], false, new org.joml.Quaternionf(mc.gameRenderer.getMainCamera().rotation()).transform(new org.joml.Vector3f(-0.3f + i * 0.15f, 0.05f, -0.7f)));
            }
            case 37 -> AutoTestClient.shotPub(mc, "anim_live_2casingc");
            case 39 -> {
                AutoTestClient.shotPub(mc, "anim_live_2casingd");
                System.out.println("[AUTOTEST] v320 particles=" + mc.particleEngine.countParticles() + " casingRenders=" + com.tycooncraft.client.gun.CasingParticle.renders);
            }
            case 42 -> {
                GunViewModel.DEBUG_LOG = true;
                KeyMapping.click(o.keyUse.getKey());
            }
            case 43 -> AutoTestClient.shotPub(mc, "anim_live_9e1");
            case 44 -> AutoTestClient.shotPub(mc, "anim_live_9e2");
            case 45 -> AutoTestClient.shotPub(mc, "anim_live_9e3");
            case 46 -> AutoTestClient.shotPub(mc, "anim_live_9e4");
            case 48 -> AutoTestClient.shotPub(mc, "anim_live_9e5");
            case 56 -> key(o.keyShift, true);
            case 60 -> KeyMapping.click(o.keyUse.getKey());
            case 64 -> key(o.keyShift, false);
            case 74 -> AutoTestClient.shotPub(mc, "anim_live_3reload");
            case 76 -> AutoTestClient.shotPub(mc, "anim_live_4reload");
            case 100 -> {
                System.out.println("[AUTOTEST] v320 anim rendered=" + GunViewModel.rendered + " ejected=" + GunViewModel.ejected
                        + " ammo=" + GunItem.ammo(mc.player.getMainHandItem()) + " casings=" + countCasings(mc));
                // 쌍권총 + 방패: 방패가 가방으로 옮겨져야 함
                AutoTestClient.serverKeepPub(mc, sp -> {
                    sp.getInventory().setItem(sp.getInventory().selected, new ItemStack(ModItems.DUAL_PISTOLS.get()));
                    sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(net.minecraft.world.item.Items.SHIELD));
                });
            }
            case 110 -> AutoTestClient.serverKeepPub(mc, sp -> System.out.println("[AUTOTEST] v320 dualShield offhand=" + sp.getOffhandItem().getItem()
                    + " blocked=" + com.tycooncraft.gun.GunEvents.DUAL_SHIELD_BLOCKED
                    + " shieldInBag=" + sp.getInventory().contains(new ItemStack(net.minecraft.world.item.Items.SHIELD))));
            case 116 -> AutoTestClient.serverKeepPub(mc, sp -> {
                ItemStack r = new ItemStack(ModItems.RIFLE.get());
                GunItem.setAmmo(r, 3);
                sp.getInventory().setItem(sp.getInventory().selected, r);
                sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                sp.getInventory().add(new ItemStack(ModItems.HEAVY_AMMO.get(), 64));
            });
            case 124 -> o.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 128 -> key(o.keyShift, true);
            case 132 -> KeyMapping.click(o.keyUse.getKey());
            case 134 -> key(o.keyShift, false);
            case 150 -> AutoTestClient.shotPub(mc, "anim_live_5tp_reload");
            case 168 -> AutoTestClient.shotPub(mc, "anim_live_6tp_reload");
            case 180 -> o.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 184 -> KeyMapping.click(o.keyUse.getKey());
            case 186 -> AutoTestClient.shotPub(mc, "anim_live_7tp_casing");
            case 188 -> AutoTestClient.shotPub(mc, "anim_live_8tp_casing");
            case 200 -> o.setCameraType(CameraType.FIRST_PERSON);
            case 206 -> AutoTestClient.serverKeepPub(mc, sp -> hdxSetup(sp));
            case 310 -> AutoTestClient.serverKeepPub(mc, sp -> hdxCheck(sp));
            case 316 -> AutoTestClient.serverKeepPub(mc, sp -> helmSetup(sp));
            case 330 -> AutoTestClient.shotPub(mc, "helm_adv");
            case 376 -> AutoTestClient.serverKeepPub(mc, sp -> helmCheck(sp));
            case 380 -> AutoTestClient.serverKeepPub(mc, sp -> mobAimSetup(sp));
            case 384 -> mc.options.hideGui = true;
            case 514 -> mc.options.hideGui = false;
            case 392 -> AutoTestClient.shotPub(mc, "mob_aim_0");
            case 400 -> AutoTestClient.serverKeepPub(mc, sp -> {
                for (var m : aimMobs) if (!(m instanceof net.minecraft.world.entity.monster.Zombie)) m.getNavigation().stop();
            });
            case 404 -> AutoTestClient.shotPub(mc, "mob_aim_0b");
            case 420 -> AutoTestClient.shotPub(mc, "mob_aim_1");
            case 432 -> AutoTestClient.shotPub(mc, "mob_aim_2");
            case 436 -> {
                AutoTestClient.serverKeepPub(mc, sp -> mobAimCheck(sp));
            }
            case 440 -> AutoTestClient.serverKeepPub(mc, sp -> {   // 플레이어를 노리는 갱단 (정면)
                var sl = sp.serverLevel();
                sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                sp.setInvulnerable(true);
                var base = sp.blockPosition();
                aimMobs.clear();
                for (int i = 0; i < 3; i++) {
                    var g = (i == 0 ? com.tycooncraft.registry.ModEntities.GANGSTER.get() : i == 1 ? com.tycooncraft.registry.ModEntities.MARKSMAN.get() : com.tycooncraft.registry.ModEntities.ENFORCER.get()).create(sl);
                    g.moveTo(base.getX() + 0.5 + (i - 1) * 3, base.getY(), base.getZ() - 7.5, 0, 0);
                    g.finalizeSpawn(sl, sl.getCurrentDifficultyAt(g.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
                    sl.addFreshEntity(g);
                    g.setTarget(sp);
                    aimMobs.add(g);
                }
                sp.teleportTo(sl, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 180f, 5f);
            });
            case 470 -> AutoTestClient.shotPub(mc, "mob_aim_p1");
            case 478 -> {
                AutoTestClient.shotPub(mc, "mob_aim_p2");
                logArms("live");
            }
            case 486 -> AutoTestClient.shotPub(mc, "mob_aim_p3");
            case 490 -> AutoTestClient.serverKeepPub(mc, sp -> {
                System.out.println("[AUTOTEST] v351 mobAimPlayer clientAim=" + clientAimInfo() + " agg=" + aimMobs.stream().map(m -> m.isAggressive() + "").toList());
                for (var m : aimMobs) m.discard();
                sp.setInvulnerable(false);
            });
            case 494 -> AutoTestClient.serverKeepPub(mc, sp -> {   // 가까이서 고정 (정면 4칸)
                var sl = sp.serverLevel();
                var base = sp.blockPosition();
                aimMobs.clear();
                net.minecraft.world.entity.EntityType<?>[] types = {com.tycooncraft.registry.ModEntities.GANGSTER.get(), com.tycooncraft.registry.ModEntities.MARKSMAN.get(),
                        com.tycooncraft.registry.ModEntities.ENFORCER.get(), com.tycooncraft.registry.ModEntities.VILLAGE_GUARD.get()};
                for (int i = 0; i < types.length; i++) {
                    var g = (net.minecraft.world.entity.Mob) types[i].create(sl);
                    g.moveTo(base.getX() + 0.5 + (i - 1.5) * 1.6, base.getY(), base.getZ() - 4.5, 0, 0);
                    g.finalizeSpawn(sl, sl.getCurrentDifficultyAt(g.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
                    g.setNoAi(true);
                    g.setAggressive(true);
                    if (i == 1) g.setLeftHanded(true);   // 왼손잡이도 총을 쥔 손으로 겨누는지
                    g.setYRot(0);
                    g.setYHeadRot(0);
                    g.yBodyRot = 0;
                    sl.addFreshEntity(g);
                    aimMobs.add(g);
                }
                sp.teleportTo(sl, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 180f, 8f);
            });
            case 502 -> {   // 오른쪽 경비병: 재장전 동작 중 모습
                if (aimMobs.size() == 4) com.tycooncraft.client.GunAnim.startReload(aimMobs.get(3).getId(), 40);
            }
            case 510 -> {
                AutoTestClient.shotPub(mc, "mob_aim_close");
                logArms("close");
            }
            case 512 -> AutoTestClient.serverKeepPub(mc, sp -> {
                for (var m : aimMobs) m.discard();
                sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            });
            case 516 -> {
                o.setCameraType(CameraType.THIRD_PERSON_FRONT);
                key(o.keyAttack, true);
            }
            case 530 -> {
                AutoTestClient.shotPub(mc, "player_aim_front");
                if (mc.getEntityRenderDispatcher().getRenderer(mc.player) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr) {
                    var hm = pr.getModel();
                    System.out.println(String.format("[AUTOTEST] v351 playerArms R %.2f %.2f %.2f | L %.2f %.2f %.2f | head %.2f %.2f", hm.rightArm.xRot, hm.rightArm.yRot, hm.rightArm.zRot,
                            hm.leftArm.xRot, hm.leftArm.yRot, hm.leftArm.zRot, hm.head.xRot, hm.head.yRot));
                }
            }
            case 532 -> {
                key(o.keyAttack, false);
                o.setCameraType(CameraType.FIRST_PERSON);
            }
            case 536 -> liveDone = true;
            default -> {}
        }
    }

    // v0.33: HDX 위력 비교 — 돌 정육면체 두 개 속에 TNT / HDX 를 넣고 터뜨린 뒤 파인 블록 수
    static net.minecraft.core.BlockPos tntC, hdxC;

    static void hdxSetup(net.minecraft.server.level.ServerPlayer sp) {
        var sl = sp.serverLevel();
        var base = sp.blockPosition().offset(0, 40, 60);
        tntC = base;
        hdxC = base.offset(40, 0, 0);
        for (var c : new net.minecraft.core.BlockPos[]{tntC, hdxC})
            for (int x = -7; x <= 7; x++) for (int y = -7; y <= 7; y++) for (int z = -7; z <= 7; z++)
                sl.setBlock(c.offset(x, y, z), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 2);
        sl.setBlock(tntC, net.minecraft.world.level.block.Blocks.TNT.defaultBlockState(), 3);
        sl.setBlock(hdxC, ModBlocks_HDX(), 3);
        net.minecraft.world.level.block.Blocks.TNT.defaultBlockState().onCaughtFire(sl, tntC, null, null);
        sl.removeBlock(tntC, false);
        ModBlocks_HDX().onCaughtFire(sl, hdxC, null, null);
        sl.removeBlock(hdxC, false);
    }

    static net.minecraft.world.level.block.state.BlockState ModBlocks_HDX() {
        return com.tycooncraft.registry.ModBlocks.HDX.get().defaultBlockState();
    }

    static void hdxCheck(net.minecraft.server.level.ServerPlayer sp) {
        var sl = sp.serverLevel();
        int[] n = new int[2];
        int i = 0;
        for (var c : new net.minecraft.core.BlockPos[]{tntC, hdxC}) {
            for (int x = -7; x <= 7; x++) for (int y = -7; y <= 7; y++) for (int z = -7; z <= 7; z++)
                if (sl.getBlockState(c.offset(x, y, z)).isAir()) n[i]++;
            i++;
        }
        System.out.println("[AUTOTEST] v330 hdx tntHole=" + n[0] + " hdxHole=" + n[1] + " ratio=" + String.format("%.2f", n[1] / (float) Math.max(1, n[0]))
                + " hdxExploded=" + com.tycooncraft.explosive.HdxBlock.PrimedHdx.EXPLODED);
    }

    // v0.35: 상위 조종간 — 같은 배 두 척에 같은 조작(약하게 전진 + 오른쪽 끝까지)을 2초간 → 선회 각도 비교
    static com.tycooncraft.ship.ShipEntity normalShip, advShip;
    static float yaw0N, yaw0A;

    static void helmSetup(net.minecraft.server.level.ServerPlayer sp) {
        var sl = sp.serverLevel();
        var b = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp.blockPosition().offset(0, 0, -50));
        net.minecraft.core.BlockPos hN = AirshipTest.build(sl, b.getX(), b.getY() + 1, b.getZ(), 2);
        net.minecraft.core.BlockPos hA = AirshipTest.build(sl, b.getX() + 14, b.getY() + 1, b.getZ(), 2);
        sl.setBlockAndUpdate(hA, com.tycooncraft.registry.ModBlocks.ADVANCED_HELM.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH));
        normalShip = com.tycooncraft.ship.ShipAssembler.assemble(sl, hN, null);
        advShip = com.tycooncraft.ship.ShipAssembler.assemble(sl, hA, null);
        for (var s : new com.tycooncraft.ship.ShipEntity[]{normalShip, advShip}) {
            if (s == null) continue;
            s.autopilot = true;
            s.setFuel(5000);
        }
        if (normalShip != null) yaw0N = normalShip.yaw;
        if (advShip != null) yaw0A = advShip.yaw;
        sp.teleportTo(sl, b.getX() + 7, b.getY() + 6, b.getZ() - 8, 0f, 30f);
    }

    static int helmT;

    static void helmDrive() {
        for (var s : new com.tycooncraft.ship.ShipEntity[]{normalShip, advShip}) if (s != null) s.testInput(0.3f, 1f, 0f);
    }

    static void helmCheck(net.minecraft.server.level.ServerPlayer sp) {
        float dN = normalShip == null ? -1 : Math.abs(net.minecraft.util.Mth.wrapDegrees(normalShip.yaw - yaw0N));
        float dA = advShip == null ? -1 : Math.abs(net.minecraft.util.Mth.wrapDegrees(advShip.yaw - yaw0A));
        System.out.println(String.format("[AUTOTEST] v350 helm normalTurn=%.1f advTurn=%.1f ratio=%.2f advFlag=%s recipe=%s", dN, dA, dA / Math.max(0.1f, dN),
                advShip != null && advShip.advancedHelm(), sp.serverLevel().getRecipeManager().byKey(com.tycooncraft.TycoonCraft.id("advanced_helm")).isPresent()));
        for (var s : new com.tycooncraft.ship.ShipEntity[]{normalShip, advShip}) if (s != null) s.discard();
    }

    // v0.35.1: 총 든 몹이 적을 노리면 조준 자세 + 쏠 때 반동
    static void logArms(String tag) {
        Minecraft mc = Minecraft.getInstance();
                StringBuilder b = new StringBuilder();
                for (var m : aimMobs) {
                    var ce = mc.level.getEntity(m.getId());
                    if (ce == null) continue;
                    var r = mc.getEntityRenderDispatcher().getRenderer(ce);
                    if (r instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> lr && lr.getModel() instanceof net.minecraft.client.model.HumanoidModel<?> hm)
                        b.append(String.format("%s[R %.2f %.2f %.2f | L %.2f %.2f %.2f | head %.2f %.2f | pose %s aim %.2f agg %s main %s] ", ce.getType().toShortString(),
                                hm.rightArm.xRot, hm.rightArm.yRot, hm.rightArm.zRot, hm.leftArm.xRot, hm.leftArm.yRot, hm.leftArm.zRot, hm.head.xRot, hm.head.yRot,
                                hm.rightArmPose, com.tycooncraft.client.GunAnim.aim(ce), ((net.minecraft.world.entity.Mob) ce).isAggressive(), ((net.minecraft.world.entity.Mob) ce).getMainArm()));
                }
        System.out.println("[AUTOTEST] v351 mobArms " + tag + " " + b);
    }

    static java.util.List<net.minecraft.world.entity.Mob> aimMobs = new java.util.ArrayList<>();

    static void mobAimSetup(net.minecraft.server.level.ServerPlayer sp) {
        var sl = sp.serverLevel();
        var base = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp.blockPosition().offset(-40, 0, 0));
        sp.stopRiding();
        sp.teleportTo(sl, base.getX() - 8.5, base.getY() + 1, base.getZ() - 1.5, 160f, 20f);
        sp.getAbilities().flying = false;
        aimMobs.clear();
        com.tycooncraft.gun.MobMag.testMag = 2;   // 두 발마다 재장전 (재장전 동작 확인용)
        net.minecraft.world.entity.EntityType<?>[] types = {com.tycooncraft.registry.ModEntities.GANGSTER.get(), com.tycooncraft.registry.ModEntities.ENFORCER.get(),
                com.tycooncraft.registry.ModEntities.MARKSMAN.get(), com.tycooncraft.registry.ModEntities.MERCENARY.get(), com.tycooncraft.registry.ModEntities.VILLAGE_GUARD.get()};
        for (int i = 0; i < types.length; i++) {
            var e = types[i].create(sl);
            if (!(e instanceof net.minecraft.world.entity.Mob m)) continue;
            m.moveTo(base.getX() - 6.5, base.getY(), base.getZ() - 4.5 - i * 3.0, -90f, 0);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
            m.setPersistenceRequired();
            sl.addFreshEntity(m);
            var z = net.minecraft.world.entity.EntityType.ZOMBIE.create(sl);
            z.moveTo(base.getX() - 20.5, base.getY(), base.getZ() - 4.5 - i * 3.0, 90f, 0);
            z.setNoAi(true);
            z.setInvulnerable(true);
            z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
            sl.addFreshEntity(z);
            m.setTarget(z);
            aimMobs.add(m);
            aimMobs.add(z);
        }
    }

    static void mobAimCheck(net.minecraft.server.level.ServerPlayer sp) {
        StringBuilder b = new StringBuilder();
        for (var m : aimMobs)
            if (!(m instanceof net.minecraft.world.entity.monster.Zombie))
                b.append(m.getType().toShortString()).append(":agg=").append(m.isAggressive()).append(",tgt=").append(m.getTarget() != null).append(' ');
        System.out.println("[AUTOTEST] v351 mobAim " + b + " clientAim=" + clientAimInfo() + " mobReloads=" + com.tycooncraft.gun.MobMag.reloads);
        for (var m : aimMobs) m.discard();
        com.tycooncraft.gun.MobMag.testMag = 0;
    }

    static String clientAimInfo() {
        Minecraft mc = Minecraft.getInstance();
        StringBuilder b = new StringBuilder();
        for (var m : aimMobs) {
            var ce = mc.level.getEntity(m.getId());
            if (ce instanceof net.minecraft.world.entity.Mob cm && !(cm instanceof net.minecraft.world.entity.monster.Zombie))
                b.append(String.format("%.2f", com.tycooncraft.client.GunAnim.aim(cm))).append(cm.isAggressive() ? "A" : "-").append(',');
        }
        return b.toString();
    }

    static int countCasings(Minecraft mc) {
        return GunViewModel.ejected;
    }

    static void key(KeyMapping k, boolean down) {
        KeyMapping.set(k.getKey(), down);
    }
}
