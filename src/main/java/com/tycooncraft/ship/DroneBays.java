package com.tycooncraft.ship;

import com.tycooncraft.mob.carrier.MiniDrone;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * v0.34: 운송기에 실린 드론 편대 격납고.
 * 셀 데이터의 Drones 만큼 드론을 띄워 두고(한 대씩 0.5초 간격으로 출격), 격추되면 Drones 가 줄어든다.
 * 격납고 위로 떨어뜨린 드론(아이템)은 하나씩 흡수해 새 드론으로 만든다 (최대 8).
 */
public final class DroneBays {
    private static final Map<ShipEntity, Map<Long, List<UUID>>> LIVE = new WeakHashMap<>();
    public static int launched, lost, rebuilt;

    static boolean isBay(ShipEntity.Cell c) {
        return c.state().getBlock() instanceof DroneBayBlock;
    }

    static void tick(ShipEntity ship, ServerLevel sl) {
        Map<Long, List<UUID>> bays = LIVE.computeIfAbsent(ship, k -> new HashMap<>());
        for (ShipEntity.Cell c : ship.cells) {
            if (!isBay(c) || c.be() == null) continue;
            CompoundTag be = c.be();
            long key = ShipEntity.key(c.x(), c.y(), c.z());
            List<UUID> live = bays.computeIfAbsent(key, k -> new ArrayList<>());
            live.removeIf(u -> !(sl.getEntity(u) instanceof MiniDrone d) || !d.isAlive());
            Vec3 ctr = ship.cellCenter(c);
            // 드론 아이템 흡수 → 새 드론
            if (ship.tickCount % 5 == 0) {
                for (ItemEntity it : sl.getEntitiesOfClass(ItemEntity.class, new AABB(ctr, ctr).inflate(1.2))) {
                    if (!it.getItem().is(ModItems.DRONE.get())) continue;
                    int have = be.contains("Drones") ? be.getInt("Drones") : DroneBayBlockEntity.START;
                    if (have >= DroneBayBlockEntity.MAX) break;
                    be.putInt("Drones", have + 1);
                    it.getItem().shrink(1);
                    if (it.getItem().isEmpty()) it.discard();
                    rebuilt++;
                    sl.playSound(null, ctr.x, ctr.y, ctr.z, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.3f);
                }
            }
            int want = be.contains("Drones") ? be.getInt("Drones") : DroneBayBlockEntity.START;
            if (live.size() < want && ship.tickCount % 10 == 0) {
                MiniDrone d = ModEntities.MINI_DRONE.get().create(sl);
                if (d == null) continue;
                d.moveTo(ctr.x, ctr.y + 0.9, ctr.z, ship.getYRot(), 0);
                d.finalizeSpawn(sl, sl.getCurrentDifficultyAt(d.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                d.setShip(ship, key, be.hasUUID("Owner") ? be.getUUID("Owner") : null, live.size());
                d.setDeltaMovement(0, 0.35, 0);
                sl.addFreshEntity(d);
                live.add(d.getUUID());
                launched++;
                sl.playSound(null, ctr.x, ctr.y, ctr.z, SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.BLOCKS, 0.7f, 1.7f);
            }
        }
    }

    /** 드론이 격추됨 → 그 격납고의 보유 수 -1 */
    public static void lost(Entity shipEntity, long key) {
        if (!(shipEntity instanceof ShipEntity ship)) return;
        for (ShipEntity.Cell c : ship.cells) {
            if (!isBay(c) || c.be() == null || ShipEntity.key(c.x(), c.y(), c.z()) != key) continue;
            int have = c.be().contains("Drones") ? c.be().getInt("Drones") : DroneBayBlockEntity.START;
            c.be().putInt("Drones", Math.max(0, have - 1));
            lost++;
        }
    }

    /** 테스트·HUD: 운송기의 격납고별 보유 수 합 */
    public static int stock(ShipEntity ship) {
        int n = 0;
        for (ShipEntity.Cell c : ship.cells)
            if (isBay(c) && c.be() != null) n += c.be().contains("Drones") ? c.be().getInt("Drones") : DroneBayBlockEntity.START;
        return n;
    }

    private DroneBays() {}
}
