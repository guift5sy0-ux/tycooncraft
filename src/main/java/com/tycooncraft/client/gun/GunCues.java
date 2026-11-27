package com.tycooncraft.client.gun;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * v0.32: 총 동작 소리 일정표.
 * 재장전·펌프·노리쇠 소리를 애니메이션과 같은 시각표로 만들어, 그 사람(엔티티) 위치에서 정확한 순간에 재생한다.
 * 자기 1인칭 동작과 다른 사람의 3인칭 동작이 같은 표를 쓰므로 소리와 손이 항상 맞는다.
 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class GunCues {
    public record Cue(float at, Supplier<SoundEvent> sound, float vol, float pitch) {}

    private record Pending(int entity, double at, Supplier<SoundEvent> sound, float vol, float pitch) {}

    private static final List<Pending> PENDING = new ArrayList<>();

    static Cue c(float at, Supplier<SoundEvent> s, float vol) {
        return new Cue(at, s, vol, 1f);
    }

    static Cue c(float at, Supplier<SoundEvent> s, float vol, float pitch) {
        return new Cue(at, s, vol, pitch);
    }

    /** 재장전 소리표 (초) — GunViewModel 의 동작 시각과 같은 비율을 씀 */
    public static List<Cue> reload(GunKind k, float dur, boolean wasEmpty, int shells) {
        List<Cue> l = new ArrayList<>();
        switch (k) {
            case PISTOL, JUDGE, SMG, RIFLE, VSS, SNIPER -> {
                float p = k == GunKind.SNIPER || k == GunKind.RIFLE ? 0.92f : k == GunKind.PISTOL || k == GunKind.JUDGE ? 1.06f : 1f;
                l.add(c(Plan.MAG_RELEASE * dur, ModSounds.MAG_OUT, 0.8f, p));
                l.add(c(Plan.MAG_RELEASE * dur + 0.5f, ModSounds.MAG_DROP, 0.55f, p));
                l.add(c(Plan.POUCH * dur, ModSounds.GUN_GRAB, 0.35f));
                l.add(c(Plan.SEAT * dur, ModSounds.MAG_IN, 0.95f, p));
                if (wasEmpty) switch (k) {
                    case PISTOL, JUDGE -> l.add(c(Plan.CH_SNAP * dur, ModSounds.SLIDE_RELEASE, 0.9f));
                    case SMG -> l.add(c(Plan.CH_SNAP * dur, ModSounds.HK_SLAP, 0.95f));
                    case RIFLE -> l.add(c(Plan.CH_SNAP * dur, ModSounds.CHARGE_RELEASE, 0.95f));   // 노리쇠 멈치
                    case VSS -> {
                        l.add(c(Plan.CH_GRAB * dur + 0.02f, ModSounds.CHARGE_BACK, 0.7f, 1.08f));
                        l.add(c(Plan.CH_SNAP * dur, ModSounds.CHARGE_RELEASE, 0.9f, 1.08f));
                    }
                    case SNIPER -> {
                        l.add(c(Plan.CH_GRAB * dur, ModSounds.BOLT_UP, 0.75f));
                        l.add(c(Plan.CH_PULL * dur, ModSounds.BOLT_BACK, 0.8f));
                        l.add(c(Plan.CH_SNAP * dur, ModSounds.BOLT_FWD, 0.85f));
                        l.add(c(Plan.CH_DONE * dur, ModSounds.BOLT_DOWN, 0.8f));
                    }
                    default -> {}
                }
            }
            case DUAL -> {
                l.add(c(0.08f * dur, ModSounds.MAG_OUT, 0.75f, 1.08f));
                l.add(c(0.1f * dur, ModSounds.MAG_OUT, 0.7f, 1.02f));
                l.add(c(0.08f * dur + 0.45f, ModSounds.MAG_DROP, 0.5f, 1.05f));
                l.add(c(0.1f * dur + 0.5f, ModSounds.MAG_DROP, 0.45f, 0.98f));
                l.add(c(0.3f * dur, ModSounds.GUN_GRAB, 0.35f));
                l.add(c(0.5f * dur, ModSounds.MAG_IN, 0.85f, 1.06f));
                l.add(c(0.53f * dur, ModSounds.MAG_IN, 0.8f, 1.0f));
                if (wasEmpty) {
                    l.add(c(0.78f * dur, ModSounds.SLIDE_RELEASE, 0.85f, 1.04f));
                    l.add(c(0.81f * dur, ModSounds.SLIDE_RELEASE, 0.8f, 0.98f));
                }
            }
            case SHOTGUN -> {
                int n = Math.max(1, shells);
                float a0 = Plan.SG_START, a1 = wasEmpty ? Plan.SG_END_EMPTY : Plan.SG_END;
                for (int i = 0; i < n; i++) {
                    float a = a0 + (a1 - a0) * i / n, b = a0 + (a1 - a0) * (i + 1) / n;
                    l.add(c((a + (b - a) * 0.2f) * dur, ModSounds.GUN_GRAB, 0.18f, 1.2f));
                    l.add(c((a + (b - a) * Plan.SG_PUSH) * dur, ModSounds.SHELL_INSERT, 0.85f, 0.96f + 0.08f * (i % 3) / 2f));
                }
                if (wasEmpty) {
                    l.add(c(0.885f * dur, ModSounds.PUMP_BACK, 0.9f));
                    l.add(c(0.935f * dur, ModSounds.PUMP_FWD, 0.95f));
                }
            }
            case RT20 -> {
                float s = Plan.rt20Scale(dur);
                l.add(c(0.8f * s, ModSounds.BOLT_UP, 0.9f, 0.8f));
                l.add(c(1.1f * s, ModSounds.BOLT_BACK, 0.95f, 0.78f));
                l.add(c(1.6f * s, ModSounds.GUN_GRAB, 0.4f, 0.85f));
                l.add(c(3.4f * s, ModSounds.ROUND_INSERT, 1f));
                l.add(c(3.85f * s, ModSounds.BOLT_FWD, 1f, 0.78f));
                l.add(c(4.15f * s, ModSounds.BOLT_DOWN, 1f, 0.8f));
            }
            case RPG -> {
                l.add(c(0.28f * dur, ModSounds.GUN_GRAB, 0.45f, 0.85f));
                l.add(c(0.68f * dur, ModSounds.ROCKET_LOAD, 0.95f));
                l.add(c(0.765f * dur, ModSounds.ROCKET_LOCK, 0.9f));
            }
        }
        return l;
    }

    /** 쏜 뒤 손으로 하는 동작 소리 (펌프·노리쇠) */
    public static List<Cue> afterShot(GunKind k) {
        return switch (k) {
            case SHOTGUN -> List.of(c(Plan.PUMP_BACK_AT, ModSounds.PUMP_BACK, 0.85f), c(Plan.PUMP_FWD_AT, ModSounds.PUMP_FWD, 0.9f));
            case SNIPER -> List.of(c(Plan.BOLT_UP_AT, ModSounds.BOLT_UP, 0.7f), c(Plan.BOLT_BACK_AT, ModSounds.BOLT_BACK, 0.8f),
                    c(Plan.BOLT_FWD_AT, ModSounds.BOLT_FWD, 0.85f), c(Plan.BOLT_DOWN_AT, ModSounds.BOLT_DOWN, 0.8f));
            default -> List.of();
        };
    }

    public static double now() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0;
        return (mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false)) / 20.0;
    }

    /** 엔티티 위치에서 delay 초 뒤 재생할 소리들 */
    public static void schedule(int entity, double start, List<Cue> cues) {
        for (Cue c : cues) PENDING.add(new Pending(entity, start + c.at(), c.sound(), c.vol(), c.pitch()));
    }

    public static void cancel(int entity) {
        PENDING.removeIf(p -> p.entity == entity);
    }

    /** 바로 재생 (그 엔티티 위치) */
    public static void play(int entity, Supplier<SoundEvent> s, float vol, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(entity);
        if (e == null) return;
        float pv = pitch * (0.97f + mc.level.random.nextFloat() * 0.06f);
        mc.level.playLocalSound(e.getX(), e.getEyeY() - 0.3, e.getZ(), s.get(), SoundSource.PLAYERS, vol, pv, false);
    }

    private static void process() {
        if (PENDING.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PENDING.clear();
            return;
        }
        double t = now();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (p.at > t) continue;
            it.remove();
            if (t - p.at < 0.5) play(p.entity, p.sound, p.vol, p.pitch);
        }
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre e) {
        process();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        process();
        com.tycooncraft.client.BulletFx.tickDelayed();
    }

    /** 동작 시각표 (재장전 비율 / 쏜 뒤 초) */
    public static final class Plan {
        // 탄창식: 기울이기 0~0.12, 탄창 빼기 0.12, 파우치 0.3, 새 탄창 꽂기 0.62, 장전 0.66~0.84, 복귀 ~1
        public static final float CANT_IN = 0.12f, MAG_RELEASE = 0.12f, DROP_END = 0.3f, HAND_OUT = 0.08f, POUCH = 0.29f,
                NEW_MAG = 0.37f, ENTRY = 0.53f, SEAT = 0.62f, CH_GRAB = 0.69f, CH_PULL = 0.745f, CH_SNAP = 0.785f, CH_DONE = 0.83f,
                HAND_BACK = 0.92f, CANT_OUT = 0.86f;
        // 샷건 한 발씩: 시작 0.12, 끝 0.94 (비었으면 0.84 + 펌프)
        public static final float SG_START = 0.12f, SG_END = 0.92f, SG_END_EMPTY = 0.84f, SG_PUSH = 0.78f;
        // 쏜 뒤 (초)
        public static final float PUMP_BACK_AT = 0.16f, PUMP_FWD_AT = 0.33f;
        public static final float BOLT_UP_AT = 0.33f, BOLT_BACK_AT = 0.45f, BOLT_FWD_AT = 0.68f, BOLT_DOWN_AT = 0.82f;

        /** RT-20: 동작은 최대 7초 안에서 하고 나머지는 대기 */
        public static float rt20Scale(float dur) {
            return Math.min(1f, dur / 7f);
        }
    }

    private GunCues() {}
}
