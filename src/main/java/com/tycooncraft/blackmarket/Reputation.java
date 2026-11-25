package com.tycooncraft.blackmarket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.28 암시장 평판 5단계: 낯선 자 → 단골 → 동업자 → 간부 → 대부.
 * 얻는 법: 암상인 거래(₩5,000당 1), 의뢰 완료, 카지노(₩20,000 걸 때마다 1)
 * 잃는 법: 암시장 소란 -30, 암상인 살해 -200, 의뢰 실패·포기 -15
 * 혜택: 암상인 할인 · 의뢰 보상 증가 · 카지노 최대 베팅 · 불법 개조 해금
 */
public final class Reputation extends SavedData {
    public static final int[] NEED = {0, 100, 300, 700, 1500};
    public static final double[] DISCOUNT = {0, 0.03, 0.06, 0.10, 0.15};
    public static final long[] MAX_BET = {10_000, 50_000, 100_000, 250_000, 500_000};
    public static final int FIGHT = -30, DEALER_KILL = -200, FAIL = -15;

    private final Map<UUID, Integer> points = new HashMap<>();

    public static Reputation get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new Factory<>(Reputation::new, Reputation::load), "tycooncraft_bm_rep");
    }

    static Reputation load(CompoundTag t, HolderLookup.Provider r) {
        Reputation d = new Reputation();
        CompoundTag p = t.getCompound("p");
        for (String k : p.getAllKeys()) d.points.put(UUID.fromString(k), p.getInt(k));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
        CompoundTag p = new CompoundTag();
        points.forEach((k, v) -> p.putInt(k.toString(), v));
        t.put("p", p);
        return t;
    }

    public int points(UUID u) {
        return points.getOrDefault(u, 0);
    }

    public static int points(ServerPlayer p) {
        return get(p.server).points(p.getUUID());
    }

    public static int tier(int pts) {
        int t = 0;
        for (int i = 0; i < NEED.length; i++) if (pts >= NEED[i]) t = i;
        return t;
    }

    public static int tier(ServerPlayer p) {
        return tier(points(p));
    }

    public static Component tierName(int t) {
        ChatFormatting[] c = {ChatFormatting.GRAY, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD};
        return Component.translatable("rep.tycooncraft.tier" + t).withStyle(c[t]);
    }

    /** 평판 증감. 단계가 바뀌면 알림 */
    public static int add(ServerPlayer p, int delta) {
        Reputation d = get(p.server);
        int before = d.points(p.getUUID());
        int after = Math.max(0, before + delta);
        d.points.put(p.getUUID(), after);
        d.setDirty();
        int t0 = tier(before), t1 = tier(after);
        if (t1 > t0) {
            p.sendSystemMessage(Component.translatable("message.tycooncraft.rep.up", tierName(t1)).withStyle(ChatFormatting.GOLD));
            p.playNotifySound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.7f);
        } else if (t1 < t0) {
            p.sendSystemMessage(Component.translatable("message.tycooncraft.rep.down", tierName(t1)).withStyle(ChatFormatting.RED));
        }
        return after;
    }

    public static void set(ServerPlayer p, int v) {
        Reputation d = get(p.server);
        d.points.put(p.getUUID(), Math.max(0, v));
        d.setDirty();
    }

    /** "단골 (120 / 300)" */
    public static Component describe(ServerPlayer p) {
        int pts = points(p), t = tier(pts);
        String next = t + 1 < NEED.length ? " (" + pts + " / " + NEED[t + 1] + ")" : " (" + pts + ")";
        return Component.empty().append(tierName(t)).append(Component.literal(next).withStyle(ChatFormatting.GRAY));
    }

    private Reputation() {}
}
