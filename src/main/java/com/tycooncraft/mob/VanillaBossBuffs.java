package com.tycooncraft.mob;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 바닐라 보스 강화 — 모델은 그대로, 연출·패턴·능력치를 대폭 강화.
 * 심연의 위더 (체력 900): 영혼불 오라 · 해골 폭풍(나선 연사) · 영혼 폭발(빨려드는 예고 후 폭발 고리) · 암흑 운석(낙하 지점 예고)
 *   체력 절반에서 각성 — 번개와 함께 영혼 기사단 소환, 영혼 돌진 추가, 오라가 푸른 불꽃으로 변함
 * 공허의 군주 엔더 드래곤 (체력 800): 공허 날개 궤적 · 화염구 부채꼴 · 셜커 탄 군단 · 착지 충격파
 *   체력 절반에서 각성 — 공허 붕괴(모든 플레이어 발밑 예고 → 공허 기둥 폭발) 추가
 */
public final class VanillaBossBuffs {
    private static final ResourceLocation BUFF = TycoonCraft.id("boss_buff");
    private static final DustParticleOptions SOUL = new DustParticleOptions(new Vector3f(1f, 0.45f, 0.08f), 1.6f);
    private static final DustParticleOptions DARK = new DustParticleOptions(new Vector3f(0.22f, 0.04f, 0.03f), 2.4f);
    private static final DustParticleOptions VOID = new DustParticleOptions(new Vector3f(0.7f, 0.2f, 1f), 2.0f);

    /** 개체별 패턴 상태 (메모리에만) */
    static final class State {
        int t, skullCd = 120, novaCd = 260, meteorCd = 200, dashCd = 160, fireCd = 100, bulletCd = 220, collapseCd = 260, shockCd = 40;
        int nova = -1, meteor = -1, dash = -1, collapse = -1;
        boolean awakened;
        Vec3 dashDir = Vec3.ZERO;
        final List<Vec3> marks = new ArrayList<>();
    }

    private static final Map<Entity, State> STATES = new WeakHashMap<>();
    public static final int[] STATS = new int[10];     // 테스트 통계

    private static State st(Entity e) {
        return STATES.computeIfAbsent(e, k -> new State());
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        Entity e = event.getEntity();
        // v0.15.6: 체력은 바닐라로 원복 (위더 300, 드래곤 200), 위더 방어력 +10 만 유지
        if (e instanceof WitherBoss w) buff(w, 0, 10, event.loadedFromDisk());
        else if (e instanceof EnderDragon d) buff(d, 0, 0, event.loadedFromDisk());
    }

    private static void buff(LivingEntity e, double hp, double armor, boolean loaded) {
        AttributeInstance mh = e.getAttribute(Attributes.MAX_HEALTH);
        // 예전 버전에서 체력 강화(+600)를 받은 채 저장된 보스는 강화를 떼고 체력을 비율대로 맞춘다
        if (mh != null && hp == 0 && mh.hasModifier(BUFF)) {
            float ratio = e.getHealth() / e.getMaxHealth();
            mh.removeModifier(BUFF);
            e.setHealth(Math.max(1f, e.getMaxHealth() * ratio));
        }
        if (mh != null && hp > 0 && !mh.hasModifier(BUFF)) {
            mh.addPermanentModifier(new AttributeModifier(BUFF, hp, AttributeModifier.Operation.ADD_VALUE));
            if (!loaded) e.setHealth(e.getMaxHealth());
        }
        AttributeInstance ar = e.getAttribute(Attributes.ARMOR);
        if (armor > 0 && ar != null && !ar.hasModifier(BUFF)) ar.addPermanentModifier(new AttributeModifier(BUFF, armor, AttributeModifier.Operation.ADD_VALUE));
    }

    /** 보스가 주는 피해 (보스 본체·투사체·폭발·패턴 모두) */
    public static final float BOSS_DAMAGE = 0.6f;

    public static boolean isBoss(Entity e) {
        return e instanceof WitherBoss || e instanceof EnderDragon
                || e instanceof com.tycooncraft.mob.DestroyerGolem
                || e instanceof com.tycooncraft.mob.MutantSpider;
    }

    @SubscribeEvent
    public static void onBossHit(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        var src = event.getSource();
        if (isBoss(event.getEntity())) return;
        if ((src.getEntity() != null && isBoss(src.getEntity())) || (src.getDirectEntity() != null && isBoss(src.getDirectEntity())))
            event.setAmount(event.getAmount() * BOSS_DAMAGE);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel sl) || !e.isAlive()) return;
        if (e instanceof net.minecraft.world.entity.Mob m && m.isNoAi()) return;
        if (e instanceof WitherBoss w) tickWither(sl, w, st(w));
        else if (e instanceof EnderDragon d) {
            tickDragon(sl, d, st(d));
            calmWings(sl, d);
        }
        if (e.tickCount % 20 == 0) huntDestroyer(sl, e);
    }

    /** 바닐라 드래곤 날개 밀쳐내기(4블록/틱)도 줄임: 근처 플레이어의 갑작스런 수평 속도를 0.7로 제한 (겉날개 비행 중 제외) */
    public static final double DRAGON_MAX_PUSH = 0.7;

    private static void calmWings(ServerLevel sl, EnderDragon d) {
        for (Player p : sl.getEntitiesOfClass(Player.class, d.getBoundingBox().inflate(8))) {
            if (p.isFallFlying() || p.isSpectator()) continue;
            Vec3 v = p.getDeltaMovement();
            double h = Math.sqrt(v.x * v.x + v.z * v.z);
            if (h > DRAGON_MAX_PUSH) {
                double k = DRAGON_MAX_PUSH / h;
                p.setDeltaMovement(v.x * k, Math.min(v.y, 0.5), v.z * k);
                p.hurtMarked = true;
            }
        }
    }

    /** 보스들(위더·워든·엘더 가디언)은 디스트로이어와 적대: 플레이어를 노리고 있지 않으면 디스트로이어를 노림 */
    private static void huntDestroyer(ServerLevel sl, Entity e) {
        if (!(e instanceof WitherBoss || e instanceof net.minecraft.world.entity.monster.warden.Warden || e instanceof net.minecraft.world.entity.monster.ElderGuardian)) return;
        var m = (net.minecraft.world.entity.Mob) e;
        if (m.getTarget() instanceof Player p && p.isAlive()) return;
        double r = e instanceof net.minecraft.world.entity.monster.ElderGuardian ? 16 : 32;
        DestroyerGolem best = null;
        double bd = Double.MAX_VALUE;
        for (DestroyerGolem g : sl.getEntitiesOfClass(DestroyerGolem.class, e.getBoundingBox().inflate(r), LivingEntity::isAlive)) {
            double d = g.distanceToSqr(e);
            if (d < bd) { bd = d; best = g; }
        }
        if (best == null) return;
        if (e instanceof net.minecraft.world.entity.monster.warden.Warden w) w.increaseAngerAt(best, 100, false);
        else m.setTarget(best);
    }

    private static List<LivingEntity> foes(ServerLevel sl, Entity boss, double range) {
        List<LivingEntity> out = new ArrayList<>(sl.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(range),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator()));
        out.addAll(sl.getEntitiesOfClass(DestroyerGolem.class, boss.getBoundingBox().inflate(range), LivingEntity::isAlive));   // 디스트로이어도 적
        if (out.isEmpty())
            out.addAll(sl.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(range), x -> x.isAlive() && x.getTags().contains("tc_dummy")));
        return out;
    }

    /** v0.18.9: 보스 전투 안내 문구는 띄우지 않음 (화면을 가려 불편) */
    private static void announce(ServerLevel sl, Entity boss, String key, ChatFormatting... style) {
    }

    private static void bolt(ServerLevel sl, Vec3 at) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(sl);
        if (b == null) return;
        b.moveTo(at.x, at.y, at.z);
        b.setVisualOnly(true);
        sl.addFreshEntity(b);
    }

    private static double ground(ServerLevel sl, double x, double z) {
        return sl.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
    }

    // ═════════════════════ 심연의 위더 ═════════════════════
    private static void tickWither(ServerLevel sl, WitherBoss w, State s) {
        s.t++;
        boolean powered = w.getHealth() <= w.getMaxHealth() * 0.5f;
        Vec3 c = w.position().add(0, 1.6, 0);
        // 오라: 몸 주위를 도는 영혼불 고리 (각성 후 더 크고 푸르게)
        double ring = powered ? 2.6 : 1.9;
        for (int i = 0; i < 3; i++) {
            double a = s.t * 0.25 + i * Math.PI * 2 / 3;
            sl.sendParticles(powered ? ParticleTypes.FLAME : ParticleTypes.SMOKE, c.x + Math.cos(a) * ring, c.y + Math.sin(s.t * 0.1 + i) * 0.6, c.z + Math.sin(a) * ring, 1, 0, 0, 0, 0);
        }
        if (s.t % 3 == 0) sl.sendParticles(DARK, w.getX(), w.getY() + 0.5, w.getZ(), 2, 0.6, 0.4, 0.6, 0);
        if (w.getInvulnerableTicks() > 0) {
            // 소환 연출: 영혼 소용돌이 기둥
            int it = w.getInvulnerableTicks();
            for (int k = 0; k < 4; k++) {
                double a = it * 0.3 + k * Math.PI / 2, r = 1 + (220 - it) / 40.0;
                sl.sendParticles(ParticleTypes.FLAME, w.getX() + Math.cos(a) * r, w.getY() + (220 - it) % 40 / 8.0, w.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            if (it == 200) {
                announce(sl, w, "message.tycooncraft.wither.rise", ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
                for (int k = 0; k < 3; k++) bolt(sl, w.position().add(Math.cos(k * 2.1) * 6, 0, Math.sin(k * 2.1) * 6));
            }
            return;
        }
        // 각성 (체력 절반)
        if (powered && !s.awakened) {
            s.awakened = true;
            STATS[5]++;
            announce(sl, w, "message.tycooncraft.wither.awaken", ChatFormatting.GOLD, ChatFormatting.BOLD);
            sl.playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 5f, 0.7f);
            for (int k = 0; k < 4; k++) bolt(sl, w.position().add(Math.cos(k * Math.PI / 2) * 5, 0, Math.sin(k * Math.PI / 2) * 5));
            sl.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 150, 0.5, 0.5, 0.5, 0.4);
            for (int k = 0; k < 3; k++) {
                WitherSkeleton g = EntityType.WITHER_SKELETON.create(sl);
                if (g == null) continue;
                double a = k * Math.PI * 2 / 3;
                double x = w.getX() + Math.cos(a) * 4, z = w.getZ() + Math.sin(a) * 4;
                g.moveTo(x, ground(sl, x, z), z, 0, 0);
                g.finalizeSpawn(sl, sl.getCurrentDifficultyAt(g.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                g.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));   // v0.19.9: 네더라이트 검 → 철 검
                g.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                g.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0);
                g.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD, 0);
                g.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 99999));
                g.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 99999, 1));
                sl.addFreshEntity(g);
                sl.sendParticles(ParticleTypes.FLAME, x, g.getY() + 1, z, 40, 0.3, 1, 0.3, 0.05);
            }
        }
        List<LivingEntity> foes = foes(sl, w, 64);
        if (foes.isEmpty()) return;
        LivingEntity t = w.getTarget() instanceof LivingEntity le && foes.contains(le) ? le : foes.get(sl.random.nextInt(foes.size()));
        float speed = powered ? 1.4f : 1f;

        // 1) 해골 폭풍: 몸 주위로 나선을 그리며 해골 24발
        if (--s.skullCd <= 0) {
            int k = -s.skullCd;
            if (k == 0) {
                STATS[0]++;
                sl.playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 4f, 0.5f);
            }
            if (k % 2 == 0) {
                double a = k * 0.55;
                for (int arm = 0; arm < 2; arm++) {
                    double aa = a + arm * Math.PI;
                    Vec3 dir = new Vec3(Math.cos(aa), -0.25, Math.sin(aa)).normalize();
                    WitherSkull sk = new WitherSkull(sl, w, dir.scale(0.9));
                    sk.setPos(c.add(dir.scale(1.5)));
                    sk.setDangerous(arm == 0 && k % 6 == 0);
                    sl.addFreshEntity(sk);
                }
                sl.playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.5f, 1.4f);
            }
            if (k >= 24) s.skullCd = (int) (220 / speed);
        }
        // 2) 영혼 폭발: 1.5초 동안 영혼이 빨려 들어온 뒤 폭발 고리
        if (s.nova < 0 && --s.novaCd <= 0 && w.distanceTo(t) < 14) {
            s.nova = 0;
            STATS[1]++;
            sl.playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 4f, 0.5f);
        }
        if (s.nova >= 0) {
            s.nova++;
            if (s.nova < 30) {
                for (int i = 0; i < 6; i++) {
                    double a = sl.random.nextDouble() * Math.PI * 2, r = 7 - s.nova * 0.2;
                    Vec3 p = c.add(Math.cos(a) * r, sl.random.nextGaussian(), Math.sin(a) * r);
                    Vec3 v = c.subtract(p).scale(0.15);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 0, v.x, v.y, v.z, 1);
                }
                w.setDeltaMovement(w.getDeltaMovement().scale(0.5));
            } else if (s.nova == 30) {
                sl.playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 5f, 0.5f);
                sl.playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 4f, 0.6f);
                sl.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, w.getBoundingBox().inflate(10))) {
                    if (e == w || e instanceof WitherSkeleton) continue;
                    if (e.distanceTo(w) > 10) continue;
                    e.hurt(w.damageSources().indirectMagic(w, w), powered ? 16f : 12f);
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1), w);
                    Vec3 push = e.position().subtract(w.position()).normalize();
                    e.push(push.x * 1.8, 0.7, push.z * 1.8);
                    e.hurtMarked = true;
                }
            } else if (s.nova <= 42) {
                double r = (s.nova - 30) * 0.9;
                int n = (int) (r * 8) + 8;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n;
                    sl.sendParticles(ParticleTypes.FLAME, c.x + Math.cos(a) * r, c.y - 0.5, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
                }
            } else {
                s.nova = -1;
                s.novaCd = (int) (300 / speed);
            }
        }
        // 3) 암흑 운석: 대상들 주변에 검은 고리 → 하늘에서 푸른 해골 낙하
        if (s.meteor < 0 && --s.meteorCd <= 0) {
            s.meteor = 0;
            STATS[2]++;
            s.marks.clear();
            for (LivingEntity e : foes.subList(0, Math.min(6, foes.size()))) {
                s.marks.add(e.position());
                s.marks.add(e.position().add(sl.random.nextGaussian() * 3, 0, sl.random.nextGaussian() * 3));
            }
        }
        if (s.meteor >= 0) {
            s.meteor++;
            if (s.meteor < 30 && s.meteor % 2 == 0) {
                for (Vec3 m : s.marks) {
                    double gy = ground(sl, m.x, m.z) + 0.1;
                    for (int i = 0; i < 12; i++) {
                        double a = i * Math.PI / 6 + s.meteor * 0.15;
                        sl.sendParticles(DARK, m.x + Math.cos(a) * 2.2, gy, m.z + Math.sin(a) * 2.2, 1, 0, 0, 0, 0);
                    }
                }
            }
            if (s.meteor == 30) {
                for (Vec3 m : s.marks) {
                    double gy = ground(sl, m.x, m.z);
                    WitherSkull sk = new WitherSkull(sl, w, new Vec3(0, -1.6, 0));
                    sk.setPos(m.x, gy + 18, m.z);
                    sk.setDangerous(true);
                    sl.addFreshEntity(sk);
                }
                sl.playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 4f, 0.5f);
            }
            if (s.meteor > 40) {
                s.meteor = -1;
                s.meteorCd = (int) (260 / speed);
            }
        }
        // 4) (각성 후) 영혼 돌진: 대상을 향해 돌진하며 영혼불 폭발 궤적
        if (powered) {
            if (s.dash < 0 && --s.dashCd <= 0) {
                s.dash = 0;
                STATS[3]++;
                s.dashDir = t.getEyePosition().subtract(c).normalize();
                sl.playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 4f, 0.5f);
            }
            if (s.dash >= 0) {
                s.dash++;
                if (s.dash < 10) {
                    w.setDeltaMovement(Vec3.ZERO);
                    sl.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 6, 0.6, 0.6, 0.6, 0.05);
                } else if (s.dash < 26) {
                    w.setDeltaMovement(s.dashDir.scale(1.5));
                    if (s.dash % 3 == 0) {
                        sl.explode(w, c.x, c.y - 1, c.z, 1.2f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                        sl.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 20, 0.6, 0.6, 0.6, 0.1);
                    }
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, w.getBoundingBox().inflate(1.2))) {
                        if (e == w || e instanceof WitherSkeleton) continue;
                        e.hurt(w.damageSources().mobAttack(w), 14f);
                        e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2), w);
                    }
                } else {
                    s.dash = -1;
                    s.dashCd = 200;
                }
            }
        }
    }

    // ═════════════════════ 공허의 군주 엔더 드래곤 (폭룡 베일 모티프) ═════════════════════
    // 공중 기술: 돌진(박치기) · 급강하 내려찍기 · 화염 휩쓸기 / 착지 기술: 꼬리 휩쓸기 · 엄니 찌르기 · 포효
    // 전역 기술: 붉은 낙뢰 · 폭염 분출(2페이즈) / 50% 이하: 각성 폭발 후 더 공격적으로 (붉은 번개 날개)
    public static final int[] DSTATS = new int[10];   // rush, dive, sweep, lightning, erupt, tail, stab, roar, transition, hits
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1f, 0.12f, 0.08f), 2.2f);
    private static final DustParticleOptions EMBER = new DustParticleOptions(new Vector3f(1f, 0.55f, 0.1f), 1.8f);
    static final int M_NONE = 0, M_RUSH = 1, M_DIVE = 2, M_SWEEP = 3;

    static final class DState {
        int move, mt, stab, chain, moveCd = 100, groundCd = 50, boltCd = 160, eruptCd = 120, fireCd = 140, transition = -1;
        int rest, lastMove, airTicks;   // v0.18.8: 기술 뒤 숨 고르기(딜 타임), 오래 날면 착지
        Vec3 restAt = Vec3.ZERO;
        Vec3 anchor = Vec3.ZERO, dir = Vec3.ZERO, mark = Vec3.ZERO;
        float sweepYaw;
        final java.util.Set<Entity> hit = new java.util.HashSet<>();
        final List<double[]> strikes = new ArrayList<>();   // x, z, 남은 틱, 종류(0 낙뢰 1 폭염)
    }

    private static final Map<Entity, DState> DSTATES = new WeakHashMap<>();
    public static int REST_COUNT, LAND_COUNT;
    /** 큰 기술(돌진·급강하·휩쓸기)을 쓴 뒤 땅 가까이 내려와 숨을 고르는 시간 (공격할 틈) */
    public static final int DRAGON_REST = 100;
    /** 이만큼 날아다니면 강제로 착지 (착지 중에는 근접 공격 가능) */
    public static final int DRAGON_LAND_AFTER = 600;

    /** 드래곤 패턴 밀쳐내기 배율 (v0.18.3: 너무 세서 40%로) */
    public static final double DRAGON_KB = 0.2;

    /** 테스트용: 기술 강제 시작 */
    public static void forceMove(EnderDragon d, int move) {
        DState s = DSTATES.computeIfAbsent(d, k -> new DState());
        s.move = move;
        s.mt = 0;
        s.chain = 0;
        s.hit.clear();
        s.anchor = d.position();
    }

    private static boolean airborne(EnderDragon d) {
        var p = d.getPhaseManager().getCurrentPhase().getPhase();
        return p == EnderDragonPhase.HOLDING_PATTERN || p == EnderDragonPhase.STRAFE_PLAYER
                || p == EnderDragonPhase.CHARGING_PLAYER || p == EnderDragonPhase.HOVERING;
    }

    /** 드래곤의 '앞'은 일반 몹과 반대: (sin yaw, -cos yaw) */
    private static void face(EnderDragon d, Vec3 dir) {
        if (dir.horizontalDistanceSqr() < 1e-4) return;
        d.setYRot((float) Math.toDegrees(Math.atan2(dir.x, -dir.z)));
    }

    private static Vec3 fwd(EnderDragon d) {
        float y = d.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(Mth.sin(y), 0, -Mth.cos(y));
    }

    private static void hold(EnderDragon d, Vec3 p) {
        d.setPos(p.x, p.y, p.z);
        d.setDeltaMovement(Vec3.ZERO);
    }

    private static void smash(ServerLevel sl, EnderDragon d, Vec3 c, double r, float dmg, double kb) {
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, r * 0.6 + 2, r))) {
            if (e == d) continue;
            double dist = e.position().distanceTo(c);
            if (dist > r) continue;
            float f = (float) (1 - 0.5 * dist / r);
            if (e.hurt(d.damageSources().mobAttack(d), dmg * f)) DSTATS[9]++;
            Vec3 push = e.position().subtract(c).multiply(1, 0, 1);
            push = push.lengthSqr() < 1e-3 ? new Vec3(1, 0, 0) : push.normalize();
            e.push(push.x * kb * f * DRAGON_KB, 0.3 + 0.15 * kb * f, push.z * kb * f * DRAGON_KB);
            e.hurtMarked = true;
        }
    }

    private static void redBolt(ServerLevel sl, EnderDragon d, double x, double z, float dmg) {
        double gy = ground(sl, x, z);
        bolt(sl, new Vec3(x, gy, z));
        for (double y = 0; y < 16; y += 0.8)
            sl.sendParticles(RED, x + sl.random.nextGaussian() * 0.15, gy + y, z + sl.random.nextGaussian() * 0.15, 2, 0.05, 0.1, 0.05, 0);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, gy + 0.3, z, 30, 1.2, 0.3, 1.2, 0.4);
        sl.sendParticles(ParticleTypes.FLAME, x, gy + 0.3, z, 20, 1.0, 0.2, 1.0, 0.08);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - 2.6, gy - 1, z - 2.6, x + 2.6, gy + 5, z + 2.6))) {
            if (e == d) continue;
            if (e.hurt(d.damageSources().indirectMagic(d, d), dmg)) DSTATS[9]++;
            e.igniteForSeconds(3);
        }
    }

    private static void queue(DState s, double x, double z, int delay, int kind) {
        s.strikes.add(new double[]{x, z, delay, kind});
    }

    private static void tickDragon(ServerLevel sl, EnderDragon d, State s0) {
        DState s = DSTATES.computeIfAbsent(d, k -> new DState());
        s0.t++;
        var phase = d.getPhaseManager().getCurrentPhase();
        if (phase.getPhase() == EnderDragonPhase.DYING) return;
        boolean powered = d.getHealth() <= d.getMaxHealth() * 0.5f;
        boolean sitting = phase.isSitting();
        float yaw = d.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        // 날개 끝: 1페이즈 공허 궤적 / 2페이즈 붉은 번개 날개
        for (int sd : new int[]{-1, 1}) {
            Vec3 tip = d.position().add(right.scale(sd * 6.5)).add(0, 2, 0);
            if (powered) {
                sl.sendParticles(RED, tip.x, tip.y, tip.z, 3, 0.6, 0.3, 0.6, 0);
                if (s0.t % 3 == 0) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, tip.x, tip.y, tip.z, 4, 0.8, 0.4, 0.8, 0.3);
            } else sl.sendParticles(ParticleTypes.PORTAL, tip.x, tip.y, tip.z, 3, 0.2, 0.2, 0.2, 0.02);
        }
        // 대기 중인 낙뢰·폭염 처리 (항상)
        for (var it = s.strikes.iterator(); it.hasNext(); ) {
            double[] k = it.next();
            double gy = ground(sl, k[0], k[1]) + 0.15;
            if (--k[2] > 0) {
                if ((int) k[2] % 2 == 0) {
                    double r = k[3] == 0 ? 2.6 : 2.2;
                    for (int i = 0; i < 12; i++) {
                        double a = i * Math.PI / 6 + k[2] * 0.2;
                        sl.sendParticles(k[3] == 0 ? RED : EMBER, k[0] + Math.cos(a) * r, gy, k[1] + Math.sin(a) * r, 1, 0, 0, 0, 0);
                    }
                    if (k[3] == 1) sl.sendParticles(ParticleTypes.LAVA, k[0], gy, k[1], 1, 0.5, 0, 0.5, 0);
                }
                continue;
            }
            if (k[3] == 0) redBolt(sl, d, k[0], k[1], powered ? 15f : 12f);
            else {
                sl.explode(d, k[0], gy, k[1], powered ? 2.6f : 2.2f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                for (double y = 0; y < 7; y += 0.5) sl.sendParticles(ParticleTypes.FLAME, k[0], gy + y, k[1], 3, 0.3, 0.1, 0.3, 0.03);
            }
            it.remove();
        }

        List<LivingEntity> foes = foes(sl, d, 128);
        // ── 각성(2페이즈) 전환: 멈춰서 화염·번개를 모았다가 대폭발 ──
        if (powered && !s0.awakened) {
            s0.awakened = true;
            s.transition = 0;
            s.move = M_NONE;
            s.anchor = d.position();
            DSTATS[8]++;
            STATS[6]++;
            announce(sl, d, "message.tycooncraft.dragon.awaken", ChatFormatting.RED, ChatFormatting.BOLD);
            sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8f, 0.5f);
        }
        if (s.transition >= 0) {
            int t = ++s.transition;
            if (airborne(d)) hold(d, s.anchor);
            Vec3 c = d.position().add(0, 2, 0);
            for (int i = 0; i < 6; i++) {
                double a = t * 0.3 + i * Math.PI / 3, r = 14 - t * 0.2;
                sl.sendParticles(i % 2 == 0 ? RED : EMBER, c.x + Math.cos(a) * r, c.y + Math.sin(t * 0.2 + i) * 3, c.z + Math.sin(a) * r, 2, 0.1, 0.1, 0.1, 0);
            }
            if (t % 10 == 0) sl.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 4f, 0.5f + t * 0.01f);
            if (t == 60) {
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 8f, 0.6f);
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 8f, 0.7f);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 3, 2, 1, 2, 0);
                sl.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 300, 3, 2, 3, 0.6);
                sl.sendParticles(RED, c.x, c.y, c.z, 200, 6, 3, 6, 0);
                smash(sl, d, new Vec3(c.x, ground(sl, c.x, c.z), c.z), 18, 18f, 2.8);
                smash(sl, d, c, 14, 10f, 2.0);
                for (int i = 0; i < 14; i++) {
                    double a = sl.random.nextDouble() * Math.PI * 2, r = 5 + sl.random.nextDouble() * 15;
                    queue(s, c.x + Math.cos(a) * r, c.z + Math.sin(a) * r, 4 + i * 3, 0);
                }
            }
            if (t >= 80) s.transition = -1;
            return;
        }
        if (foes.isEmpty()) {
            s.move = M_NONE;
            return;
        }
        LivingEntity tgt = foes.stream().min(java.util.Comparator.comparingDouble(d::distanceToSqr)).orElse(foes.get(0));
        float speed = powered ? 1.5f : 1f;

        // ── 전역: 붉은 낙뢰 (대상 발밑 + 주변) ──
        if (--s.boltCd <= 0) {
            DSTATS[3]++;
            sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6f, 1.3f);
            int waves = powered ? 3 : 1;
            for (int w = 0; w < waves; w++)
                for (LivingEntity e : foes.subList(0, Math.min(6, foes.size()))) {
                    Vec3 lead = e.position().add(e.getDeltaMovement().multiply(12, 0, 12));
                    queue(s, lead.x + sl.random.nextGaussian() * w * 1.5, lead.z + sl.random.nextGaussian() * w * 1.5, 30 + w * 14, 0);
                }
            for (int i = 0; i < (powered ? 6 : 3); i++) {
                double a = sl.random.nextDouble() * Math.PI * 2, r = 4 + sl.random.nextDouble() * 10;
                queue(s, tgt.getX() + Math.cos(a) * r, tgt.getZ() + Math.sin(a) * r, 30 + i * 4, 0);
            }
            s.boltCd = (int) (240 / speed);
        }
        // ── 2페이즈 전역: 폭염 분출 (드래곤 주변 땅이 터져 오름) ──
        if (powered && --s.eruptCd <= 0) {
            DSTATS[4]++;
            Vec3 c = sitting ? d.position() : tgt.position();
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.LAVA_AMBIENT, SoundSource.HOSTILE, 6f, 0.5f);
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI * 2 / 10 + sl.random.nextDouble() * 0.4, r = 3 + sl.random.nextDouble() * 9;
                queue(s, c.x + Math.cos(a) * r, c.z + Math.sin(a) * r, 22 + i * 2, 1);
            }
            s.eruptCd = 200;
        }

        // ── 착지 중: 꼬리 휩쓸기 · 엄니 찌르기 · 포효 ──
        if (sitting) {
            s.move = M_NONE;
            s.lastMove = M_NONE;
            s.airTicks = 0;
            if (s.stab > 0) {                                   // 엄니 찌르기 예고 진행 중
                s.stab--;
                Vec3 h = d.head.position();
                sl.sendParticles(powered ? RED : EMBER, h.x, h.y, h.z, 4, 0.5, 0.5, 0.5, 0);
                if (s.stab == 0) {
                    Vec3 f = s.dir;
                    sl.playSound(null, h.x, h.y, h.z, SoundEvents.ENDER_DRAGON_HURT, SoundSource.HOSTILE, 6f, 0.5f);
                    for (double k = 0; k < 11; k += 0.7) {
                        Vec3 p = h.add(f.scale(k));
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y - 1, p.z, 1, 0, 0, 0, 0);
                        if (powered) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y - 1, p.z, 3, 0.3, 0.3, 0.3, 0.2);
                    }
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, d.getBoundingBox().inflate(14, 5, 14))) {
                        if (e == d) continue;
                        Vec3 rel = e.position().subtract(h).multiply(1, 0, 1);
                        double along = rel.dot(f), side = rel.subtract(f.scale(along)).length();
                        if (along > -1 && along < 12 && side < 2.8) {
                            if (e.hurt(d.damageSources().mobAttack(d), powered ? 24f : 20f)) DSTATS[9]++;
                            e.push(f.x * 2.6 * DRAGON_KB, 0.4, f.z * 2.6 * DRAGON_KB);
                            e.hurtMarked = true;
                        }
                    }
                    if (powered) for (int i = 2; i <= 10; i += 4) queue(s, h.x + f.x * i, h.z + f.z * i, 10 + i, 0);
                }
                return;
            }
            if (--s.groundCd <= 0) {
                Vec3 f = fwd(d);
                Vec3 rel = tgt.position().subtract(d.position()).multiply(1, 0, 1);
                double dist = rel.length();
                double dot = dist > 0.1 ? rel.normalize().dot(f) : 1;
                if (dot < -0.2 && dist < 16) {                    // 꼬리 휩쓸기 (뒤쪽)
                    DSTATS[5]++;
                    Vec3 back = d.position().subtract(f.scale(6));
                    sl.playSound(null, back.x, back.y, back.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 6f, 0.5f);
                    for (int i = -6; i <= 6; i++) {
                        Vec3 p = d.position().add(f.scale(-7).yRot(i * 0.18f)).add(0, 1, 0);
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                        sl.sendParticles(ParticleTypes.CLOUD, p.x, p.y - 0.8, p.z, 3, 0.3, 0.1, 0.3, 0.05);
                    }
                    smash(sl, d, back, 9, powered ? 18f : 14f, 3.0);
                    s.groundCd = (int) (75 / speed);
                } else if (dot > 0.3 && dist < 18) {               // 엄니 찌르기 (정면) — 예고 후 발동
                    DSTATS[6]++;
                    s.dir = rel.normalize();
                    s.stab = powered ? 12 : 18;
                    sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5f, 1.4f);
                    s.groundCd = (int) (85 / speed);
                } else {                                         // 포효: 밀쳐내고 둔화
                    DSTATS[7]++;
                    Vec3 c = d.position();
                    sl.playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8f, 0.6f);
                    for (int r = 2; r <= 12; r += 2)
                        for (int i = 0; i < r * 5; i++) {
                            double a = i * Math.PI * 2 / (r * 5);
                            sl.sendParticles(powered ? RED : ParticleTypes.REVERSE_PORTAL, c.x + Math.cos(a) * r, c.y + 0.4, c.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0.02);
                        }
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, d.getBoundingBox().inflate(12, 4, 12))) {
                        if (e == d || e.distanceTo(d) > 12) continue;
                        e.hurt(d.damageSources().mobAttack(d), 6f);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), d);
                        Vec3 p = e.position().subtract(c).normalize();
                        e.push(p.x * 2.4 * DRAGON_KB, 0.35, p.z * 2.4 * DRAGON_KB);
                        e.hurtMarked = true;
                    }
                    s.groundCd = (int) (95 / speed);
                }
            }
            return;
        }

        // ── 비행 중 ──
        if (s.move != M_NONE && !airborne(d)) s.move = M_NONE;
        if (s.lastMove != M_NONE && s.move == M_NONE) {   // 큰 기술이 끝남(또는 끊김) → 지쳐서 내려옴
            s.rest = DRAGON_REST;
            s.restAt = new Vec3(d.getX(), ground(sl, d.getX(), d.getZ()) + 3.5, d.getZ());
            REST_COUNT++;
        }
        s.lastMove = s.move;
        if (s.rest > 0 && s.move == M_NONE) {
            s.rest--;
            hold(d, d.position().lerp(s.restAt, 0.12));
            face(d, tgt.position().subtract(d.position()));
            if (s0.t % 4 == 0) {
                Vec3 h = d.head.position();
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, h.x, h.y, h.z, 3, 0.4, 0.3, 0.4, 0.02);
                sl.sendParticles(ParticleTypes.CLOUD, d.getX(), d.getY(), d.getZ(), 4, 2.5, 0.3, 2.5, 0.01);
            }
            if (s.rest == DRAGON_REST - 1) announce(sl, d, "message.tycooncraft.dragon.tired", ChatFormatting.YELLOW);
            return;
        }
        if (airborne(d) && s.move == M_NONE && ++s.airTicks > DRAGON_LAND_AFTER) {   // 너무 오래 날면 착지하러 감
            s.airTicks = 0;
            d.getPhaseManager().setPhase(EnderDragonPhase.LANDING_APPROACH);
            LAND_COUNT++;
            return;
        }
        if (s.move == M_NONE && airborne(d)) {
            if (--s.moveCd <= 0 && d.distanceTo(tgt) < 90) {
                int r = sl.random.nextInt(7);
                forceMove(d, r < 3 ? M_RUSH : r < 5 ? M_DIVE : M_SWEEP);
                s.moveCd = (int) (110 / speed);
            } else if (--s.fireCd <= 0) {                      // 화염구 부채꼴 (틈틈이)
                Vec3 head = d.head.position();
                Vec3 aim = tgt.getEyePosition().subtract(head).normalize();
                for (int i = -2; i <= 2; i++) {
                    Vec3 dir = aim.yRot(i * 0.14f);
                    DragonFireball fb = new DragonFireball(sl, d, dir.scale(1.2));
                    fb.setPos(head.add(dir.scale(2)));
                    sl.addFreshEntity(fb);
                }
                sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 6f, 0.8f);
                s.fireCd = (int) (170 / speed);
            }
        }
        switch (s.move) {
            case M_RUSH -> rush(sl, d, s, tgt, powered);
            case M_DIVE -> dive(sl, d, s, tgt, powered);
            case M_SWEEP -> sweep(sl, d, s, tgt, powered);
            default -> {
            }
        }
    }

    /** 돌진: 포효하며 힘을 모은 뒤 대상에게 일직선으로 들이받는다 (2페이즈 2연속) */
    private static void rush(ServerLevel sl, EnderDragon d, DState s, LivingEntity tgt, boolean powered) {
        int tele = s.chain == 0 ? 26 : 12;
        int t = ++s.mt;
        if (t == 1) {
            DSTATS[0]++;
            sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8f, 0.8f);
        }
        if (t <= tele) {
            Vec3 to = tgt.position().add(0, 1.5, 0).subtract(s.anchor);
            face(d, to);
            hold(d, s.anchor.add(fwd(d).scale(-0.08 * t)));   // 뒤로 살짝 빠지며 힘을 모음
            Vec3 h = d.head.position();
            sl.sendParticles(powered ? RED : EMBER, h.x, h.y, h.z, 6, 0.8, 0.8, 0.8, 0);
            sl.sendParticles(ParticleTypes.FLAME, h.x, h.y, h.z, 3, 0.5, 0.5, 0.5, 0.02);
            if (t == tele) {
                s.dir = to.normalize();
                s.anchor = d.position();
                s.hit.clear();
                sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 8f, 0.5f);
            }
            return;
        }
        int dt = t - tele;
        double v = powered ? 3.0 : 2.4;
        Vec3 p = d.position().add(s.dir.scale(v));
        double gy = ground(sl, p.x, p.z);
        if (p.y < gy + 2.5) p = new Vec3(p.x, gy + 2.5, p.z);
        hold(d, p);
        face(d, s.dir);
        sl.sendParticles(ParticleTypes.FLAME, p.x, p.y + 1, p.z, 12, 2.5, 1, 2.5, 0.05);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 1, p.z, 4, 2, 1, 2, 0.02);
        if (powered) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 1, p.z, 8, 2, 1, 2, 0.3);
        if (powered && dt % 3 == 0) queue(s, p.x, p.z, 16 + dt, 0);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, d.getBoundingBox().inflate(2.5))) {
            if (e == d || !s.hit.add(e)) continue;
            if (e.hurt(d.damageSources().mobAttack(d), powered ? 22f : 17f)) DSTATS[9]++;
            e.push(s.dir.x * 3 * DRAGON_KB, 0.5, s.dir.z * 3 * DRAGON_KB);
            e.hurtMarked = true;
            sl.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3f, 1.2f);
        }
        boolean passed = p.subtract(s.anchor).dot(s.dir) > tgt.position().subtract(s.anchor).dot(s.dir) + 10;
        if (dt >= 30 || passed) {
            sl.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 6, 2, 1, 2, 0);
            if (powered && s.chain == 0) {         // 2페이즈: 곧바로 한 번 더
                s.chain = 1;
                s.mt = 0;
                s.anchor = d.position();
            } else s.move = M_NONE;
        }
    }

    /** 급강하 내려찍기: 대상 위로 솟구쳤다가 표시된 지점에 수직 낙하 → 폭발·화염 고리 (2페이즈: 번개 고리) */
    private static void dive(ServerLevel sl, EnderDragon d, DState s, LivingEntity tgt, boolean powered) {
        int t = ++s.mt;
        if (t == 1) {
            DSTATS[1]++;
            sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 8f, 0.6f);
        }
        if (t <= 32) {
            s.mark = new Vec3(tgt.getX(), ground(sl, tgt.getX(), tgt.getZ()), tgt.getZ());
            Vec3 up = s.mark.add(0, 22, 0);
            Vec3 p = d.position().add(up.subtract(d.position()).scale(0.12));
            hold(d, p);
            face(d, s.mark.subtract(p));
            if (t % 2 == 0)
                for (int i = 0; i < 20; i++) {
                    double a = i * Math.PI / 10 + t * 0.1;
                    sl.sendParticles(powered ? RED : EMBER, s.mark.x + Math.cos(a) * 6, s.mark.y + 0.2, s.mark.z + Math.sin(a) * 6, 1, 0, 0, 0, 0);
                }
            if (t == 32) sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8f, 0.7f);
            return;
        }
        if (t <= 34) {           // 정점에서 잠깐 멈춤
            hold(d, d.position());
            return;
        }
        Vec3 p = d.position();
        Vec3 to = s.mark.add(0, 1.5, 0).subtract(p);
        double step = Math.min(3.2, to.length());
        p = to.lengthSqr() > 1e-3 ? p.add(to.normalize().scale(step)) : p;
        hold(d, p);
        sl.sendParticles(ParticleTypes.FLAME, p.x, p.y + 1, p.z, 10, 1.5, 1.5, 1.5, 0.05);
        if (step < 3.2 || t > 60) {
            Vec3 c = s.mark;
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 8f, 0.5f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 2, 1, 0.5, 1, 0);
            for (int r = 2; r <= 10; r += 2)
                for (int i = 0; i < r * 6; i++) {
                    double a = i * Math.PI * 2 / (r * 6);
                    sl.sendParticles(ParticleTypes.FLAME, c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 1, 0, 0.2, 0, 0.05);
                }
            smash(sl, d, c, 10, powered ? 24f : 20f, 2.6);
            if (powered)
                for (int ring = 0; ring < 3; ring++)
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4 + ring * 0.4, r = 5 + ring * 4;
                        queue(s, c.x + Math.cos(a) * r, c.z + Math.sin(a) * r, 8 + ring * 8, 0);
                    }
            else
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3, r = 7;
                    queue(s, c.x + Math.cos(a) * r, c.z + Math.sin(a) * r, 14, 1);
                }
            s.move = M_NONE;
        }
    }

    /** 화염 휩쓸기: 공중에 멈춰 대상 쪽을 좌→우로 훑는 화염 숨결 (2페이즈: 화염+번개) */
    private static void sweep(ServerLevel sl, EnderDragon d, DState s, LivingEntity tgt, boolean powered) {
        int t = ++s.mt;
        int len = 44;
        if (t == 1) {
            DSTATS[2]++;
            Vec3 to = tgt.position().subtract(d.position());
            s.sweepYaw = (float) Math.toDegrees(Math.atan2(to.x, -to.z));
            sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 7f, 1.0f);
        }
        hold(d, s.anchor);
        if (t <= 12) {
            d.setYRot(s.sweepYaw - 55);
            Vec3 h = d.head.position();
            sl.sendParticles(ParticleTypes.FLAME, h.x, h.y, h.z, 4, 0.4, 0.4, 0.4, 0.02);
            return;
        }
        float k = (t - 12) / (float) len;
        float yaw = s.sweepYaw - 55 + 110 * k;
        d.setYRot(yaw);
        Vec3 h = d.head.position();
        Vec3 flat = new Vec3(Mth.sin(yaw * Mth.DEG_TO_RAD), 0, -Mth.cos(yaw * Mth.DEG_TO_RAD));
        double drop = Math.max(0, h.y - tgt.getY() - 1);
        double reach = 26;
        Vec3 dir = flat.scale(reach).add(0, -drop, 0).normalize();
        for (double q = 1; q < reach; q += 1.4) {
            Vec3 p = h.add(dir.scale(q));
            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.25 + q * 0.04, 0.25, 0.25 + q * 0.04, 0.02);
            if (powered && sl.random.nextInt(3) == 0) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.3, 0.3, 0.3, 0.2);
        }
        if (t % 4 == 0)
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(h, h).inflate(reach))) {
                if (e == d) continue;
                Vec3 rel = e.position().add(0, 1, 0).subtract(h);
                double along = rel.dot(dir);
                if (along < 0 || along > reach) continue;
                if (rel.subtract(dir.scale(along)).length() < 1.6 + along * 0.08) {
                    if (e.hurt(d.damageSources().indirectMagic(d, d), powered ? 7f : 5f)) DSTATS[9]++;
                    e.igniteForSeconds(5);
                }
            }
        if (powered && t % 8 == 0) {
            Vec3 end = h.add(dir.scale(reach));
            queue(s, end.x, end.z, 12, 0);
        }
        if (t >= 12 + len) s.move = M_NONE;
    }


    // 추가 전리품
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide) return;
        List<ItemStack> extra = new ArrayList<>();
        if (e instanceof WitherBoss) {
            extra.add(new ItemStack(Items.NETHERITE_SCRAP, 3));
            extra.add(new ItemStack(ModItems.COIN_EMERALD.get(), 3));
        } else if (e instanceof EnderDragon) {
            extra.add(new ItemStack(Items.NETHERITE_INGOT, 2));
            extra.add(new ItemStack(ModItems.COIN_DIAMOND.get(), 1));
            extra.add(new ItemStack(Items.ELYTRA));
        }
        for (ItemStack s : extra)
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(e.level(), e.getX(), e.getY() + 1, e.getZ(), s));
    }

    private VanillaBossBuffs() {}
}
