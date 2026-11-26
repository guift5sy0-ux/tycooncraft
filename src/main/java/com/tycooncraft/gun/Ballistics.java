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
    public static int BLOCKED;
    /** v0.31.2: 지금 들어가는 피해가 총알인지 (서버 스레드) → 방어력 60% 무시 */
    public static boolean BULLET_HIT;
    public static final float BULLET_ARMOR_IGNORE = 0.6f;
    /** 방탄조끼 등급별 몸통 총알 피해 감소 */
    private static final float[] VEST_CUT = {0f, 0.20f, 0.25f, 0.30f, 0.35f};
    /** 방탄모 등급별 헤드샷 '추가' 피해 감소 + 머리 피해 자체 감소 */
    private static final float[] HELMET_BONUS_CUT = {0f, 0.30f, 0.38f, 0.46f, 0.54f};
    private static final float[] HELMET_CUT = {0f, 0.06f, 0.08f, 0.10f, 0.12f};

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

    /** v0.31.1: 총알 보호 인챈트 - 4부위 레벨 합 × 0.6% (최대 IV×4 = 9.6%) */
    public static float bulletProtection(LivingEntity le) {
        int lv = 0;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            lv += Math.min(4, GunEnchants.level(le.getItemBySlot(s), GunEnchants.BULLET_PROTECTION));
        return 1f - 0.006f * lv;
    }

    /** 방어구를 반영한 최종 총알 피해 */
    public static float damage(Entity target, float base, boolean head, float headMult) {
        if (!(target instanceof LivingEntity le)) return base;
        float bp = bulletProtection(le);
        if (head) {
            int h = helmetTier(le);
            float mult = 1 + (headMult - 1) * (1 - HELMET_BONUS_CUT[h]);
            return base * mult * (1 - HELMET_CUT[h]) * bp;
        }
        ItemStack lg = le.getItemBySlot(EquipmentSlot.LEGS), ft = le.getItemBySlot(EquipmentSlot.FEET);
        float legs = lg.is(ModItems.JUGGERNAUT_LEGGINGS.get()) ? 0.93f : lg.is(ModItems.PANTS_3.get()) ? 0.94f : lg.is(ModItems.PANTS_2.get()) ? 0.95f : lg.is(ModItems.PANTS_1.get()) ? 0.96f : 1f;
        float boots = ft.is(ModItems.COMBAT_BOOTS.get()) ? 0.95f : ft.is(ModItems.BOOTS_3.get()) ? 0.96f : ft.is(ModItems.BOOTS_2.get()) ? 0.97f : ft.is(ModItems.BOOTS_1.get()) ? 0.98f : 1f;
        return base * (1 - VEST_CUT[vestTier(le)]) * legs * boots * bp;
    }

    /** 다음 한 발의 총알 모양 (서버 스레드 전용): 종류 0 일반 1 화염 2 심판자 3 기관포, 속도(블록/틱), 소음기 여부 */
    public static int nextKind = -1;
    public static float nextSpeed;
    public static boolean nextSilenced;
    /** RT-20: 전차·디스트로이어(프로토 포함)에 1.1배 */
    public static boolean nextAntiMaterial;
    public static final float ANTI_MATERIAL_MULT = 1.1f;

    public static boolean armoredTarget(Entity e) {
        return e instanceof com.tycooncraft.vehicle.TankEntity || e instanceof com.tycooncraft.mob.DestroyerGolem || e instanceof com.tycooncraft.mech.ProtoDestroyer;
    }

    /** 플레이어가 쏘면 총알이 눈이 아니라 손에 든 총의 총구에서 나가 보이게 */
    static Vec3 visualMuzzle(Entity shooter, Vec3 from, Vec3 dir) {
        // v0.20.0: 총을 든 사람형 몹(갱단 등)은 눈이 아니라 오른손 총구에서 섬광·총알이 나가 보이게
        if (shooter instanceof com.tycooncraft.mob.GunnerEntity gm && from.distanceToSqr(gm.getEyePosition()) < 1.5) {
            Vec3 r = dir.cross(new Vec3(0, 1, 0));
            r = r.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : r.normalize();
            return gm.getEyePosition().add(dir.scale(0.75)).add(r.scale(0.32)).add(0, -0.42, 0);
        }
        if (!(shooter instanceof net.minecraft.world.entity.player.Player p) || from.distanceToSqr(p.getEyePosition()) > 2.5) return from;
        Vec3 right = dir.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        double side = p.getPersistentData().contains("tycooncraft_dual_left") && p.getMainHandItem().getItem() instanceof GunItem g && g.isDual()
                ? (p.getPersistentData().getBoolean("tycooncraft_dual_left") ? -0.3 : 0.3) : 0.24;
        return p.getEyePosition().add(dir.scale(0.85)).add(right.scale(side)).add(0, -0.17, 0);
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
        var shipHit = com.tycooncraft.ship.ShipCollisions.clip(level, from, end);
        if (shipHit != null) {   // 운송기 선체에 막힘 (블록 판정으로 취급)
            end = shipHit.point();
            blockHit = new BlockHitResult(end, net.minecraft.core.Direction.UP, net.minecraft.core.BlockPos.containing(end), false);
        }
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
            if (nextAntiMaterial && armoredTarget(target)) d *= ANTI_MATERIAL_MULT;
            target.invulnerableTime = 0;
            // 엔더 드래곤 같은 다중 부위 몹: 실제 체력은 본체에 있으므로 본체의 무적 시간도 풀어 준다
            if (target instanceof net.neoforged.neoforge.entity.PartEntity<?> part) part.getParent().invulnerableTime = 0;
            BULLET_HIT = true;
            boolean landed;
            try { landed = target.hurt(src, d); } finally { BULLET_HIT = false; }
            if (landed) {
                if (onHit != null) onHit.accept(target);
                if (fire) target.igniteForSeconds(4);
                if (head) {
                    if (shooter instanceof Player p) {
                        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5f, 1.6f);
                        {} /* v0.18.9: 전투 안내 문구 제거 */
                    }
                }
            }
            if (fire) level.sendParticles(ParticleTypes.SMALL_FLAME, impact.x, impact.y, impact.z, 3, 0.08, 0.08, 0.08, 0.02);   // v0.20.0: 탄착 효과는 클라이언트(BulletFx)가 그림
            n++;
            if (target instanceof net.minecraft.world.entity.LivingEntity le && le.isBlocking() && le.isDamageSourceBlocked(src)) {   // 방패에 막힘 → 관통 없음
                BLOCKED++;
                level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.3f);
                break;
            }
        }
        // v0.19.4: 총알이 포탑에 맞음 → 포탑 체력 감소 (포탑끼리는 서로 쏘지 않음)
        if (n <= pierce && shooter != null && blockHit.getType() == HitResult.Type.BLOCK
                && level.getBlockEntity(blockHit.getBlockPos()) instanceof com.tycooncraft.turret.TurretBlockEntity tb) tb.hurt(dmg, shooter);
        Vec3 first = n > 0 ? hits.get(0).getLocation() : impact;
        // 눈에 보이는 총알 (탄착 파편·소리는 총알이 도착할 때 클라이언트가 만든다)
        Vec3 stop = n > pierce ? hits.get(pierce).getLocation() : impact;
        byte kind = nextKind >= 0 ? (byte) nextKind : fire ? (byte) 1 : (byte) 0;
        byte imp = n > pierce ? (byte) 2 : blockHit.getType() == HitResult.Type.BLOCK ? (byte) 1 : (byte) 0;
        Vec3 vis = visualMuzzle(shooter, from, dir);
        float speed = nextSpeed > 0 ? nextSpeed : range >= 150 ? 30f : range >= 70 ? 22f : 16f;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level, null, vis.x, vis.y, vis.z, 160,
                new com.tycooncraft.network.TracerPayload(vis, stop, kind, imp, shooter == null ? -1 : shooter.getId(), speed, tracer && !nextSilenced));
        nextKind = -1;
        nextSpeed = 0;
        nextSilenced = false;
        nextAntiMaterial = false;
        return first;
    }

    private Ballistics() {}
}
