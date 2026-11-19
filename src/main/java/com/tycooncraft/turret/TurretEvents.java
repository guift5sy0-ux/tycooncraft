package com.tycooncraft.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** v0.19.4: 폭발은 포탑을 바로 부수지 않고 거리에 따라 체력을 깎는다 */
public final class TurretEvents {
    @SubscribeEvent
    public static void onExplode(ExplosionEvent.Detonate e) {
        if (e.getLevel().isClientSide) return;
        Vec3 c = e.getExplosion().center();
        float r = e.getExplosion().radius();
        double reach = r * 2;
        for (BlockPos p : TurretBlockEntity.all(e.getLevel()).toArray(BlockPos[]::new)) {
            double d = p.getCenter().distanceTo(c);
            if (d > reach || !(e.getLevel().getBlockEntity(p) instanceof TurretBlockEntity t)) continue;
            e.getAffectedBlocks().remove(p);
            t.hurt((float) (9 * r * (1 - d / reach)), e.getExplosion().getIndirectSourceEntity());
        }
    }

    private TurretEvents() {}
}
