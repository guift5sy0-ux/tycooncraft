package com.tycooncraft.machine;

import com.tycooncraft.ship.ShipEntity;
import com.tycooncraft.vehicle.DroneEntity;
import com.tycooncraft.vehicle.VehicleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 전자기 펄스: 반경 안의 모든 탈것(차·헬기·전투기·탱크·기관차·배)을 먹통으로, 드론은 추락 */
public final class EmpPulse {
    public static int hits;

    public static int pulse(ServerLevel sl, Vec3 c, double radius, int ticks) {
        int hit = 0;
        for (Entity e : sl.getEntities((Entity) null, new AABB(c, c).inflate(radius), e -> e.position().distanceTo(c) <= radius + e.getBbWidth() / 2)) {
            if (e instanceof VehicleEntity v) v.emp(ticks);
            else if (e instanceof DroneEntity d) d.emp();
            else if (e instanceof ShipEntity s) s.emp(ticks);
            else continue;
            hit++;
            for (Entity p : e.getPassengers())
                if (p instanceof ServerPlayer sp)
                    sp.displayClientMessage(Component.translatable("message.tycooncraft.emp_hit", ticks / 20).withStyle(ChatFormatting.AQUA), true);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 40,
                    e.getBbWidth() / 2, e.getBbHeight() / 2, e.getBbWidth() / 2, 0.3);
        }
        hits += hit;
        return hit;
    }

    private EmpPulse() {}
}
