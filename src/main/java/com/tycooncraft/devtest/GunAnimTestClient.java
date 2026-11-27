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
            case 206 -> liveDone = true;
            default -> {}
        }
    }

    static int countCasings(Minecraft mc) {
        return GunViewModel.ejected;
    }

    static void key(KeyMapping k, boolean down) {
        KeyMapping.set(k.getKey(), down);
    }
}
