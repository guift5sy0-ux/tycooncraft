package com.tycooncraft.gun;

import com.tycooncraft.registry.ModItems;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * 총알 판정 공통: 플레이어 총, 총잡이 몹, 자동 포탑, 전투기 기관포.
 * 방탄조끼(몸통 피해 감소)·방탄모(헤드샷 추가 피해 감소)를 여기서 반영한다.
 */
public final class Ballistics {
    /** 방탄조끼 등급별 몸통 총알 피해 감소 */
    private static final float[] VEST_CUT = {0f, 0.15f, 0.25f, 0.35f, 0.45f};
    /** 방탄모 등급별 헤드샷 '추가' 피해 감소 + 머리 피해 자체 감소 */
    private static final float[] HELMET_BONUS_CUT = {0f, 0.2f, 0.35f, 0.5f, 0.65f};
    private static final float[] HELMET_CUT = {0f, 0.05f, 0.08f, 0.1f, 0.15f};

    public static int vestTier(LivingEntity e) {
        ItemStack s = e.getItemBySlot(EquipmentSlot.CHEST);
        if (s.is(ModItems.VEST_4.get())) return 4;
        if (s.is(ModItems.VEST_3.get())) return 3;
        if (s.is(ModItems.VEST_2.get())) return 2;
        if (s.is(ModItems.VEST_1.get())) return 1;
        return 0;
    }

    public static int helmetTier(LivingEntity e) {
        ItemStack s = e.getItemBySlot(EquipmentSlot.HEAD);
        if (s.is(ModItems.HELMET_4.get())) return 4;
        if (s.is(ModItems.HELMET_3.get())) return 3;
        if (s.is(ModItems.HELMET_2.get())) return 2;
        if (s.is(ModItems.HELMET_1.get())) return 1;
        return 0;
    }

    public static boolean isHead(Entity target, Vec3 hit) {
        return target instanceof LivingEntity le && le.getBbHeight() > 0.9f && hit.y >= le.getEyeY() - 0.28;
    }

    /** 방어구를 반영한 최종 총알 피해 */
    public static float damage(Entity target, float base, boolean head, float headMult) {
        if (!(target instanceof LivingEntity le)) return base;
        if (head) {
            int h = helmetTier(le);
            float mult = 1 + (headMult - 1) * (1 - HELMET_BONUS_CUT[h]);
            return base * mult * (1 - HELMET_CUT[h]);
        }
        float legs = le.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.JUGGERNAUT_LEGGINGS.get()) ? 0.92f : 1f;
        float boots = le.getItemBySlot(EquipmentSlot.FEET).is(ModItems.COMBAT_BOOTS.get()) ? 0.95f : 1f;
        return base * (1 - VEST_CUT[vestTier(le)]) * legs * boots;
    }

    /** 한 발: 경로상 대상(가까운 순)을 pierce+1 명까지 맞힘. 맞힌 첫 지점 반환 */
    public static Vec3 shoot(ServerLevel level, Entity shooter, Vec3 from, Vec3 dir, double range, float dmg, int pierce,
                             float headMult, boolean fire, boolean tracer, DamageSource src, Predicate<Entity> extraFilter) {
        return shoot(level, shooter, from, dir, range, dmg, pierce, headMult, fire, tracer, src, extraFilter, null);
    }

    /** onHit: 실제로 피해를 입힌 대상마다 호출 */
    public static Vec3 shoot(ServerLevel level, Entity shooter, Vec3 from, Vec3 dir, double range, float dmg, int pierce,
                             float headMult, boolean fire, boolean tracer, DamageSource src, Predicate<Entity> extraFilter,
                             java.util.function.Consumer<Entity> onHit) {
        Vec3 end = from.add(dir.scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                shooter == null ? net.minecraft.world.phys.shapes.CollisionContext.empty() : net.minecraft.world.phys.shapes.CollisionContext.of(shooter)));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();
        final Vec3 rayEnd = end;
        AABB box = new AABB(from, end).inflate(1.0);
        List<EntityHitResult> hits = new ArrayList<>();
        for (Entity e : level.getEntities(shooter, box, e -> e.isAlive() && e.isPickable() && !e.isSpectator() && extraFilter.test(e))) {
            e.getBoundingBox().inflate(e.getPickRadius() + 0.1).clip(from, rayEnd).ifPresent(p -> hits.add(new EntityHitResult(e, p)));
        }
        hits.sort(Comparator.comparingDouble(h -> h.getLocation().distanceToSqr(from)));
        Vec3 impact = end;
        int n = 0;
        for (EntityHitResult hit : hits) {
            if (n > pierce) break;
            Entity target = hit.getEntity();
            impact = hit.getLocation();
            boolean head = isHead(target, impact);
            float d = damage(target, dmg * (n == 0 ? 1f : 0.7f), head, headMult);
            target.invulnerableTime = 0;
            // 엔더 드래곤 같은 다중 부위 몹: 실제 체력은 본체에 있으므로 본체의 무적 시간도 풀어 준다
            if (target instanceof net.neoforged.neoforge.entity.PartEntity<?> part) part.getParent().invulnerableTime = 0;
            if (target.hurt(src, d)) {
                if (onHit != null) onHit.accept(target);
                if (fire) target.igniteForSeconds(4);
                if (head) {
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT, impact.x, impact.y, impact.z, 10, 0.15, 0.15, 0.15, 0.3);
                    if (shooter instanceof Player p) {
                        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5f, 1.6f);
                        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tycooncraft.gun.headshot")
                                .withStyle(net.minecraft.ChatFormatting.GOLD), true);
                    }
                }
            }
            level.sendParticles(fire ? ParticleTypes.FLAME : ParticleTypes.CRIT, impact.x, impact.y, impact.z, 6, 0.1, 0.1, 0.1, 0.2);
            n++;
        }
        if (n == 0 && blockHit.getType() == HitResult.Type.BLOCK) {
            var state = level.getBlockState(blockHit.getBlockPos());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), impact.x, impact.y, impact.z, 6, 0.05, 0.05, 0.05, 0.1);
        }
        Vec3 first = n > 0 ? hits.get(0).getLocation() : impact;
        if (tracer) {
            double dist = (n > pierce ? hits.get(pierce).getLocation() : impact).distanceTo(from);
            for (double t = 2; t < dist; t += 2.5) {
                Vec3 p = from.add(dir.scale(t));
                level.sendParticles(fire ? ParticleTypes.SMALL_FLAME : ParticleTypes.ELECTRIC_SPARK, p.x, p.y - 0.1, p.z, 1, 0, 0, 0, 0);
            }
        }
        return first;
    }

    private Ballistics() {}
}
