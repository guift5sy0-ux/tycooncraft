package com.tycooncraft.ship;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/** 월드에 떠 있는 배 목록(클라이언트/서버 따로)과, 이동 충돌에 배 선체 상자를 끼워 넣는 도우미 */
public final class ShipCollisions {
    private static final Set<ShipEntity> SERVER = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<ShipEntity> CLIENT = Collections.newSetFromMap(new WeakHashMap<>());

    static void add(ShipEntity s) {
        set(s.level().isClientSide).add(s);
    }

    static void remove(ShipEntity s) {
        set(s.level().isClientSide).remove(s);
    }

    private static Set<ShipEntity> set(boolean client) {
        return client ? CLIENT : SERVER;
    }

    /** 같은 레벨의 배들 (복사본) */
    public static List<ShipEntity> ships(Level level) {
        Set<ShipEntity> set = set(level.isClientSide);
        if (set.isEmpty()) return List.of();
        List<ShipEntity> out = new ArrayList<>(set.size());
        for (ShipEntity s : set) if (s.level() == level && !s.isRemoved()) out.add(s);
        return out;
    }

    public static List<VoxelShape> append(Level level, Entity self, AABB box, List<VoxelShape> base) {
        Set<ShipEntity> set = set(level.isClientSide);
        if (set.isEmpty() || self instanceof ShipEntity) return base;
        List<VoxelShape> out = null;
        for (ShipEntity s : set) {
            if (s.level() != level || s.isRemoved() || self.getVehicle() == s) continue;
            if (!s.worldBounds().intersects(box)) continue;
            for (AABB b : s.boxes()) {
                if (b != null && b.intersects(box)) {
                    if (out == null) out = new ArrayList<>(base);
                    out.add(Shapes.create(b));
                }
            }
        }
        return out == null ? base : out;
    }

    /** 이번 사격에서 무시할 배 (자기 배의 포탑이 쏠 때) */
    public static final ThreadLocal<ShipEntity> IGNORE = new ThreadLocal<>();

    /** 선분이 배 선체 상자에 처음 닿는 곳 (없으면 null) */
    @org.jetbrains.annotations.Nullable
    public static ShipHit clip(Level level, net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to) {
        Set<ShipEntity> set = set(level.isClientSide);
        if (set.isEmpty()) return null;
        ShipEntity ignore = IGNORE.get();
        AABB seg = new AABB(from, to);
        ShipHit best = null;
        double bd = Double.MAX_VALUE;
        for (ShipEntity s : set) {
            if (s == ignore || s.level() != level || s.isRemoved() || !s.worldBounds().intersects(seg)) continue;
            for (AABB b : s.boxes()) {
                if (b == null) continue;
                var hit = b.clip(from, to);
                if (hit.isPresent() && hit.get().distanceToSqr(from) < bd) {
                    bd = hit.get().distanceToSqr(from);
                    best = new ShipHit(s, hit.get());
                }
            }
        }
        return best;
    }

    public record ShipHit(ShipEntity ship, net.minecraft.world.phys.Vec3 point) {}

    /** 폭발이 근처 배에 피해 (블록을 부수지 않는 '몹 사망 폭발' 류는 제외) */
    public static void onDetonate(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
        var ex = event.getExplosion();
        boolean projectile = ex.getDirectSourceEntity() instanceof net.minecraft.world.entity.projectile.Projectile;
        if (ex.getBlockInteraction() == net.minecraft.world.level.Explosion.BlockInteraction.KEEP && !projectile) return;
        for (ShipEntity s : ships(event.getLevel())) s.blast(ex);
    }

    private ShipCollisions() {}
}
