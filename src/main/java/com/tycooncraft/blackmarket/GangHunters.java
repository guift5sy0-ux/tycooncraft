package com.tycooncraft.blackmarket;

import com.tycooncraft.mob.GunnerEntity;
import com.tycooncraft.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * v0.26.0 갱단 추격:
 * - 추적 V: 30초마다 30% 확률로 갱단원 1~2명이 근처에 나타남 (평소 출현의 약 3배)
 * - 추적 V 또는 현상금이 걸린 사람: 3분마다 40% 확률로 갱단 무리(4~5명 또는 갱단 트럭)가 쫓아옴
 */
public final class GangHunters {
    public static final int SMALL_EVERY = 600, SQUAD_EVERY = 3600;
    public static final float SMALL_CHANCE = 0.3f, SQUAD_CHANCE = 0.4f;
    private static final Map<UUID, Long> NEXT_SMALL = new HashMap<>(), NEXT_SQUAD = new HashMap<>();
    public static int smallSpawns, squads;

    static void tick(MinecraftServer s) {
        if (s.getTickCount() % 20 != 0) return;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive() || BlackMarket.isBM(p.level())) continue;
            ServerLevel l = p.serverLevel();
            if (l.getDifficulty() == Difficulty.PEACEFUL) continue;
            long now = l.getGameTime();
            boolean tracked = BlackMarket.tracked(p);
            boolean bounty = BountyData.get(s).totalOn(p.getUUID()) > 0;
            if (tracked && now >= NEXT_SMALL.getOrDefault(p.getUUID(), 0L)) {
                NEXT_SMALL.put(p.getUUID(), now + SMALL_EVERY);
                if (p.getRandom().nextFloat() < SMALL_CHANCE) {
                    int n = 1 + p.getRandom().nextInt(2);
                    for (int i = 0; i < n; i++) spawnHunter(l, p, ModEntities.GANGSTER.get(), 16, 26);
                    smallSpawns++;
                }
            }
            if ((tracked || bounty) && now >= NEXT_SQUAD.getOrDefault(p.getUUID(), now + SQUAD_EVERY / 2)) {
                NEXT_SQUAD.put(p.getUUID(), now + SQUAD_EVERY);
                if (p.getRandom().nextFloat() < SQUAD_CHANCE) squad(l, p);
            } else if (!NEXT_SQUAD.containsKey(p.getUUID()) && (tracked || bounty)) NEXT_SQUAD.put(p.getUUID(), now + SQUAD_EVERY / 2);
        }
    }

    /** 갱단 무리: 절반은 트럭을 타고, 절반은 걸어서 */
    public static int squad(ServerLevel l, ServerPlayer p) {
        int made = 0;
        if (p.getRandom().nextBoolean()) {
            BlockPos at = findSpot(l, p, 28, 36);
            if (at != null) {
                var truck = ModEntities.GANG_TRUCK.get().create(l);
                if (truck != null) {
                    truck.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getRandom().nextFloat() * 360, 0);
                    truck.finalizeSpawn(l, l.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
                    truck.setTarget(p);
                    for (var e : truck.getPassengers()) if (e instanceof Mob m) m.setTarget(p);
                    l.addFreshEntity(truck);
                    made += 1 + truck.getPassengers().size();
                }
            }
        }
        if (made == 0) {
            List<EntityType<GunnerEntity>> crew = List.of(ModEntities.GANGSTER.get(), ModEntities.GANGSTER.get(), ModEntities.MERCENARY.get(),
                    ModEntities.OUTLAW.get(), ModEntities.MERCENARY.get());
            int n = 4 + p.getRandom().nextInt(2);
            for (int i = 0; i < n; i++) if (spawnHunter(l, p, crew.get(i), 18, 28)) made++;
        }
        if (made > 0) {
            squads++;
            p.displayClientMessage(Component.translatable("message.tycooncraft.bm.squad").withStyle(ChatFormatting.DARK_RED), true);
            p.playNotifySound(net.minecraft.sounds.SoundEvents.RAID_HORN.value(), net.minecraft.sounds.SoundSource.HOSTILE, 0.5f, 0.8f);
        }
        return made;
    }

    static boolean spawnHunter(ServerLevel l, ServerPlayer p, EntityType<GunnerEntity> type, int min, int max) {
        BlockPos at = findSpot(l, p, min, max);
        if (at == null) return false;
        GunnerEntity g = type.create(l);
        if (g == null) return false;
        g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getRandom().nextFloat() * 360, 0);
        g.finalizeSpawn(l, l.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        g.setTarget(p);
        g.getPersistentData().putBoolean("tycooncraft_hunter", true);
        return l.addFreshEntity(g);
    }

    /** 플레이어 주변 원 위에서, 발 디딜 땅이 있고 머리 위 2칸이 빈 곳 */
    static BlockPos findSpot(ServerLevel l, ServerPlayer p, int min, int max) {
        var r = p.getRandom();
        for (int tries = 0; tries < 16; tries++) {
            double a = r.nextDouble() * Math.PI * 2, d = min + r.nextDouble() * (max - min);
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            if (!l.isLoaded(new BlockPos(x, (int) p.getY(), z))) continue;
            for (int dy = 8; dy >= -8; dy--) {
                BlockPos b = new BlockPos(x, (int) p.getY() + dy, z);
                if (l.getBlockState(b.below()).isSolidRender(l, b.below()) && l.getBlockState(b).isAir() && l.getBlockState(b.above()).isAir()
                        && l.getFluidState(b).isEmpty()) return b;
            }
        }
        return null;
    }

    private GangHunters() {}
}
