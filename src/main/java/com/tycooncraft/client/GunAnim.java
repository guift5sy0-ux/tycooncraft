package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * v0.20.1: 총을 '들고 다닐 때'와 '쏠 때(조준)' 자세를 나눔.
 * 쏘거나(총알이 나가면 서버가 알려 줌), 공격 키를 누르거나, 조준경을 보거나, 몹이 전투 중이면 조준 자세로 빠르게 올리고,
 * 1.5초 동안 안 쏘면 천천히 내려 들고 다니는 자세(로우 레디)로 돌아감.
 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class GunAnim {
    private static final class S {
        float v, vO;
        long lastShot = -1000;
        double reloadStart = -99, reloadDur;
    }

    private static final Map<Integer, S> STATE = new HashMap<>();
    public static final int HOLD_TICKS = 30;

    private static S st(Entity e) {
        return STATE.computeIfAbsent(e.getId(), k -> new S());
    }

    /** 총알이 나감 (TracerPayload) */
    public static void shot(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        S s = STATE.computeIfAbsent(id, k -> new S());
        s.lastShot = mc.level.getGameTime();
    }

    /** v0.32: 재장전 (3인칭 자세용) */
    public static void startReload(int id, int ticks) {
        S s = STATE.computeIfAbsent(id, k -> new S());
        s.reloadStart = com.tycooncraft.client.gun.GunCues.now();
        s.reloadDur = ticks / 20.0;
    }

    public static void stopReload(int id) {
        S s = STATE.get(id);
        if (s != null) s.reloadStart = -99;
    }

    /** 재장전 진행도 0~1, 안 하면 -1 */
    public static float reloadProgress(Entity e) {
        S s = STATE.get(e.getId());
        if (s == null || s.reloadStart < 0) return -1f;
        double u = (com.tycooncraft.client.gun.GunCues.now() - s.reloadStart) / Math.max(0.05, s.reloadDur);
        if (u > 1 || u < 0) return -1f;
        return (float) u;
    }

    private static boolean wantsAim(Entity e, S s, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (now - s.lastShot < HOLD_TICKS) return true;
        if (s.reloadStart >= 0 && com.tycooncraft.client.gun.GunCues.now() - s.reloadStart < s.reloadDur) return true;
        if (e instanceof LivingEntity le && le.isUsingItem()) return true;
        if (e == mc.player && mc.screen == null && (mc.options.keyAttack.isDown() || GunClient.isScoping())) return true;
        return e instanceof Mob m && m.isAggressive();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            STATE.clear();
            return;
        }
        long now = mc.level.getGameTime();
        STATE.entrySet().removeIf(en -> mc.level.getEntity(en.getKey()) == null && now - en.getValue().lastShot > 200 && reloadProgress(en.getKey()) < 0);
        for (var en : STATE.entrySet()) {
            Entity e = mc.level.getEntity(en.getKey());
            S s = en.getValue();
            s.vO = s.v;
            boolean up = e != null && wantsAim(e, s, now);
            s.v += ((up ? 1f : 0f) - s.v) * (up ? 0.5f : 0.12f);   // 올릴 땐 빠르게, 내릴 땐 천천히
        }
    }

    private static float reloadProgress(int id) {
        S s = STATE.get(id);
        if (s == null || s.reloadStart < 0) return -1f;
        double u = (com.tycooncraft.client.gun.GunCues.now() - s.reloadStart) / Math.max(0.05, s.reloadDur);
        return u > 1 || u < 0 ? -1f : (float) u;
    }

    /** 0 = 들고 다니는 자세, 1 = 조준 자세 */
    public static float aim(Entity e) {
        S s = st(e);
        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        return Mth.clamp(Mth.lerp(pt, s.vO, s.v), 0f, 1f);
    }

    /** 방금 쏜 반동 (1 → 0, 약 4틱) */
    public static float kick(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0;
        S s = STATE.get(e.getId());
        if (s == null) return 0;
        float since = mc.level.getGameTime() - s.lastShot + mc.getTimer().getGameTimeDeltaPartialTick(false);
        return since < 0 || since > 4 ? 0 : 1 - since / 4f;
    }

    private GunAnim() {}
}
