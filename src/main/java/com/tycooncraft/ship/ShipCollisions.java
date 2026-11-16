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

    private ShipCollisions() {}
}
