package com.tycooncraft.turret;

import com.tycooncraft.entity.GrenadeEntity;
import com.tycooncraft.entity.RocketEntity;
import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.registry.ModSounds;
import com.tycooncraft.ship.ShipCollisions;
import com.tycooncraft.vehicle.DroneEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 방공 요격 포대(CIWS) 공용 두뇌: 날아오는 로켓·포탄·미사일·수류탄·화염구·자폭 드론을 찾아
 * 개틀링으로 쏴서 공중에서 터뜨린다. 블록 포대와 운송기에 실린 포대가 함께 쓴다.
 * 주인(설치한 사람)·같은 팀이 쏜 것은 건드리지 않는다.
 */
public final class Interceptor {
    public static final double RANGE = 40;
    public static int SHOTS, KILLS;

    public static final class State {
        public int target = -1, cooldown, search, rounds;
    }

    public static boolean isThreat(Entity e) {
        return e.isAlive() && (e instanceof RocketEntity || e instanceof GrenadeEntity || e instanceof AbstractHurtingProjectile || e instanceof DroneEntity);
    }

    @Nullable
    static UUID shooterOf(Entity e) {
        if (e instanceof DroneEntity d) return d.ownerId();
        if (e instanceof Projectile p && p.getOwner() != null) return p.getOwner().getUUID();
        return null;
    }

    static boolean friendly(ServerLevel sl, Entity e, @Nullable UUID owner) {
        UUID s = shooterOf(e);
        if (s == null || owner == null) return false;
        return com.tycooncraft.alliance.Alliances.friendly(sl.getServer(), owner, s);   // 본인·동맹·팀
    }

    /** 이쪽으로 오는 중인가 (지나쳐 가는 것·멀어지는 것은 무시). 드론은 범위 안이면 위협 */
    static boolean incoming(Entity e, Vec3 at) {
        if (e instanceof DroneEntity) return true;
        Vec3 v = e.getDeltaMovement();
        Vec3 to = at.subtract(e.position());
        if (v.lengthSqr() < 1e-4 || v.dot(to) <= 0) return false;
        // 지금 경로대로 가장 가까이 오는 거리
        double t = v.dot(to) / v.lengthSqr();
        return e.position().add(v.scale(t)).distanceTo(at) < 18;
    }

    static boolean sees(ServerLevel sl, Vec3 from, Entity e) {
        Vec3 p = e.getBoundingBox().getCenter();
        Vec3 st = from.add(p.subtract(from).normalize().scale(0.8));
        return sl.clip(new ClipContext(st, p, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    static boolean valid(ServerLevel sl, Vec3 m, Entity e, @Nullable UUID owner) {
        return isThreat(e) && e.distanceToSqr(m) < RANGE * RANGE && !friendly(sl, e, owner) && incoming(e, m) && sees(sl, m, e);
    }

    /**
     * 한 틱. ammo: 탄약 1개를 꺼내 true(중탄)/false(경탄), 없으면 null.
     * @return 목표가 바뀌었으면 true (클라이언트 동기화용)
     */
    public static boolean tick(ServerLevel sl, Vec3 m, State st, @Nullable UUID owner, Supplier<Boolean> ammo, @Nullable Object ignoreShip) {
        if (st.cooldown > 0) st.cooldown--;
        Entity target = st.target >= 0 ? sl.getEntity(st.target) : null;
        int before = st.target;
        if (target == null || !valid(sl, m, target, owner)) {
            target = null;
            if (--st.search <= 0) {
                st.search = 2;   // 빠른 탄을 놓치지 않게 자주 찾는다
                double bd = Double.MAX_VALUE;
                for (Entity e : sl.getEntities((Entity) null, new AABB(m, m).inflate(RANGE), Interceptor::isThreat)) {
                    double d = e.distanceToSqr(m);
                    if (d < bd && valid(sl, m, e, owner)) {
                        bd = d;
                        target = e;
                    }
                }
            }
            st.target = target == null ? -1 : target.getId();
        }
        if (target == null || st.cooldown > 0) return before != st.target;
        if (st.rounds <= 0) {
            Boolean heavy = ammo.get();
            if (heavy == null) return before != st.target;
            st.rounds += heavy ? 4 : 2;
        }
        st.rounds--;
        st.cooldown = 0;   // 매 틱 사격 (분당 1200발)
        SHOTS++;
        Vec3 aim = target.getBoundingBox().getCenter().add(target.getDeltaMovement());
        double dist = aim.distanceTo(m);
        Vec3 dir = aim.subtract(m).normalize();
        // 보이는 예광탄 (피해 없음)
        if (ignoreShip instanceof com.tycooncraft.ship.ShipEntity s) ShipCollisions.IGNORE.set(s);
        try {
            Ballistics.shoot(sl, null, m.add(dir.scale(1.0)), dir, dist, 0f, 0, 1f, false, true, sl.damageSources().generic(), e -> false);
        } finally {
            ShipCollisions.IGNORE.remove();
        }
        if (SHOTS % 3 == 0) sl.playSound(null, m.x, m.y, m.z, ModSounds.RIFLE_FIRE.get(), SoundSource.BLOCKS, 1.4f, 1.9f);
        // 명중 확률: 가까울수록 높음
        double p = Math.max(0.15, 0.40 - dist * 0.007);
        if (sl.random.nextDouble() < p) {
            kill(sl, target);
            st.target = -1;
        }
        return before != st.target;
    }

    static void kill(ServerLevel sl, Entity e) {
        KILLS++;
        Vec3 p = e.position();
        if (e instanceof RocketEntity r) r.intercept();
        else if (e instanceof DroneEntity d) d.detonate();
        else {
            e.discard();
            sl.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            sl.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.8f, 1.5f);
        }
        sl.sendParticles(com.tycooncraft.registry.ModParticles.SMOKE_LARGE.get(), p.x, p.y, p.z, 6, 0.3, 0.3, 0.3, 0.02);
    }

    private Interceptor() {}
}
