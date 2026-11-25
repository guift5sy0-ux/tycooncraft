package com.tycooncraft.mob.carrier;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** v0.28 디스트로이어 계열 공통: 디스트로이어끼리는 싸우지 않고, 그 밖의 모든 생명체를 공격 */
public final class Destroyers {
    public static boolean isDestroyer(Entity e) {
        return e instanceof com.tycooncraft.mob.DestroyerGolem || e instanceof ScoutDestroyer || e instanceof AerialDestroyer
                || (e instanceof MiniDrone d && !d.ally());
    }

    /** 노릴 수 있는 대상: 디스트로이어가 아니고, 살아 있고, 크리에이티브·관전 플레이어가 아님 */
    public static boolean prey(LivingEntity e) {
        if (e == null || !e.isAlive() || isDestroyer(e)) return false;
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return !(e instanceof net.minecraft.world.entity.decoration.ArmorStand);
    }

    /** 모든 몹 대상 (플레이어·주민 우선순위는 따로) */
    public static <T extends Mob> net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<Mob> anyMob(T self, int chance) {
        return new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(self, Mob.class, chance, true, false, Destroyers::prey);
    }

    private Destroyers() {}
}
