package com.tycooncraft.turret;

import com.tycooncraft.gun.Ballistics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * v0.19.4: 갱단·디스트로이어·거미 여왕 등은 근처 포탑을 먼저 노린다.
 * 원거리형은 시야가 닿는 거리까지 가서 총을 쏘고(총알이 포탑에 맞으면 포탑 체력이 깎임), 근접형은 붙어서 때린다.
 */
public class TurretHuntGoal extends Goal {
    public static final double NOTICE = 24;
    private final PathfinderMob mob;
    private final float rangedDmg, meleeDmg;
    private final double range;
    private final int interval;
    private final Supplier<SoundEvent> sound;
    private final BooleanSupplier enabled;
    private BlockPos turret;
    private int cd, repath;
    public static int RANGED_SHOTS, MELEE_HITS;

    public TurretHuntGoal(PathfinderMob mob, float rangedDmg, double range, int interval, Supplier<SoundEvent> sound, float meleeDmg, BooleanSupplier enabled) {
        this.mob = mob;
        this.rangedDmg = rangedDmg;
        this.range = range;
        this.interval = interval;
        this.sound = sound;
        this.meleeDmg = meleeDmg;
        this.enabled = enabled;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static TurretHuntGoal ranged(PathfinderMob mob, float dmg, double range, int interval, Supplier<SoundEvent> sound) {
        return new TurretHuntGoal(mob, dmg, range, interval, sound, 0, () -> true);
    }

    public static TurretHuntGoal melee(PathfinderMob mob, float dmg, int interval) {
        return new TurretHuntGoal(mob, 0, 0, interval, null, dmg, () -> true);
    }

    @Override
    public boolean canUse() {
        if ((mob.tickCount + mob.getId()) % 10 != 0 || !enabled.getAsBoolean() || !(mob.level() instanceof ServerLevel)) return false;
        turret = TurretBlockEntity.nearest(mob.level(), mob.position(), NOTICE);
        return turret != null;
    }

    @Override
    public boolean canContinueToUse() {
        return turret != null && enabled.getAsBoolean() && mob.level().getBlockEntity(turret) instanceof TurretBlockEntity
                && turret.getCenter().distanceToSqr(mob.position()) < (NOTICE + 8) * (NOTICE + 8);
    }

    @Override
    public void start() {
        mob.setTarget(null);   // 포탑부터 처리
        cd = 10;
        repath = 0;
    }

    @Override
    public void stop() {
        turret = null;
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean sees(Vec3 eye, Vec3 c) {
        BlockHitResult h = mob.level().clip(new ClipContext(eye, c, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        return h.getType() == HitResult.Type.MISS || h.getBlockPos().equals(turret);
    }

    @Override
    public void tick() {
        if (turret == null) return;
        if (mob.getTarget() != null) mob.setTarget(null);
        Vec3 c = Vec3.atCenterOf(turret);
        Vec3 eye = mob.getEyePosition();
        mob.getLookControl().setLookAt(c.x, c.y, c.z, 30f, 30f);
        boolean ranged = rangedDmg > 0;
        double d = mob.position().distanceTo(c);
        double reach = mob.getBbWidth() * 0.5 + 1.8;
        boolean inRange = ranged ? d <= range && sees(eye, c) : d <= reach;
        if (!inRange) {
            if (--repath <= 0) {
                repath = 10;
                mob.getNavigation().moveTo(c.x, turret.getY(), c.z, 1.0);
            }
        } else mob.getNavigation().stop();
        if (--cd > 0 || !inRange) return;
        cd = interval;
        ServerLevel sl = (ServerLevel) mob.level();
        if (ranged) {
            Vec3 dir = c.subtract(eye).normalize();
            dir = dir.add(mob.getRandom().nextGaussian() * 0.03, mob.getRandom().nextGaussian() * 0.03, mob.getRandom().nextGaussian() * 0.03).normalize();
            Ballistics.shoot(sl, mob, eye.add(dir.scale(0.6)), dir, range + 6, rangedDmg, 0, 1f, false, true, mob.damageSources().mobAttack(mob), e -> e != mob);
            if (sound != null) sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(), sound.get(), SoundSource.HOSTILE, 3f, 0.95f + mob.getRandom().nextFloat() * 0.1f);
            RANGED_SHOTS++;
        } else {
            mob.swing(InteractionHand.MAIN_HAND);
            if (sl.getBlockEntity(turret) instanceof TurretBlockEntity t) t.hurt(meleeDmg, mob);
            MELEE_HITS++;
        }
    }
}
