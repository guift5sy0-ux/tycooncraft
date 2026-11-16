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
    private static final DustParticleOptions SOUL = new DustParticleOptions(new Vector3f(0.25f, 0.85f, 1f), 1.6f);
    private static final DustParticleOptions DARK = new DustParticleOptions(new Vector3f(0.12f, 0.05f, 0.18f), 2.4f);
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
        if (e instanceof WitherBoss w) buff(w, 600, 10, event.loadedFromDisk());
        else if (e instanceof EnderDragon d) buff(d, 600, 0, event.loadedFromDisk());
    }

    private static void buff(LivingEntity e, double hp, double armor, boolean loaded) {
        AttributeInstance mh = e.getAttribute(Attributes.MAX_HEALTH);
        if (mh != null && !mh.hasModifier(BUFF)) {
            mh.addPermanentModifier(new AttributeModifier(BUFF, hp, AttributeModifier.Operation.ADD_VALUE));
            if (!loaded) e.setHealth(e.getMaxHealth());
        }
        AttributeInstance ar = e.getAttribute(Attributes.ARMOR);
        if (armor > 0 && ar != null && !ar.hasModifier(BUFF)) ar.addPermanentModifier(new AttributeModifier(BUFF, armor, AttributeModifier.Operation.ADD_VALUE));
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel sl) || !e.isAlive()) return;
        if (e instanceof WitherBoss w) tickWither(sl, w, st(w));
        else if (e instanceof EnderDragon d) tickDragon(sl, d, st(d));
    }

    private static List<LivingEntity> foes(ServerLevel sl, Entity boss, double range) {
        List<LivingEntity> out = new ArrayList<>(sl.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(range),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator()));
        if (out.isEmpty())
            out.addAll(sl.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(range), x -> x.isAlive() && x.getTags().contains("tc_dummy")));
        return out;
    }

    private static void announce(ServerLevel sl, Entity boss, String key, ChatFormatting... style) {
        for (ServerPlayer p : sl.players())
            if (p.distanceTo(boss) < 128) p.displayClientMessage(Component.translatable(key).withStyle(style), true);
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
            sl.sendParticles(powered ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE, c.x + Math.cos(a) * ring, c.y + Math.sin(s.t * 0.1 + i) * 0.6, c.z + Math.sin(a) * ring, 1, 0, 0, 0, 0);
        }
        if (s.t % 3 == 0) sl.sendParticles(DARK, w.getX(), w.getY() + 0.5, w.getZ(), 2, 0.6, 0.4, 0.6, 0);
        if (w.getInvulnerableTicks() > 0) {
            // 소환 연출: 영혼 소용돌이 기둥
            int it = w.getInvulnerableTicks();
            for (int k = 0; k < 4; k++) {
                double a = it * 0.3 + k * Math.PI / 2, r = 1 + (220 - it) / 40.0;
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, w.getX() + Math.cos(a) * r, w.getY() + (220 - it) % 40 / 8.0, w.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
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
            announce(sl, w, "message.tycooncraft.wither.awaken", ChatFormatting.AQUA, ChatFormatting.BOLD);
            sl.playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 5f, 0.7f);
            for (int k = 0; k < 4; k++) bolt(sl, w.position().add(Math.cos(k * Math.PI / 2) * 5, 0, Math.sin(k * Math.PI / 2) * 5));
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 150, 0.5, 0.5, 0.5, 0.4);
            for (int k = 0; k < 3; k++) {
                WitherSkeleton g = EntityType.WITHER_SKELETON.create(sl);
                if (g == null) continue;
                double a = k * Math.PI * 2 / 3;
                double x = w.getX() + Math.cos(a) * 4, z = w.getZ() + Math.sin(a) * 4;
                g.moveTo(x, ground(sl, x, z), z, 0, 0);
                g.finalizeSpawn(sl, sl.getCurrentDifficultyAt(g.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                g.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
                g.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                g.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0);
                g.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD, 0);
                g.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 99999));
                g.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 99999, 1));
                sl.addFreshEntity(g);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, g.getY() + 1, z, 40, 0.3, 1, 0.3, 0.05);
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
                    sl.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 0, v.x, v.y, v.z, 1);
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
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + Math.cos(a) * r, c.y - 0.5, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
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
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 6, 0.6, 0.6, 0.6, 0.05);
                } else if (s.dash < 26) {
                    w.setDeltaMovement(s.dashDir.scale(1.5));
                    if (s.dash % 3 == 0) {
                        sl.explode(w, c.x, c.y - 1, c.z, 1.2f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 20, 0.6, 0.6, 0.6, 0.1);
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

    // ═════════════════════ 공허의 군주 엔더 드래곤 ═════════════════════
    private static void tickDragon(ServerLevel sl, EnderDragon d, State s) {
        s.t++;
        var phase = d.getPhaseManager().getCurrentPhase();
        if (phase.getPhase() == EnderDragonPhase.DYING) return;
        boolean powered = d.getHealth() <= d.getMaxHealth() * 0.5f;
        // 날개 끝 공허 궤적
        float yaw = d.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        for (int sd : new int[]{-1, 1}) {
            Vec3 tip = d.position().add(right.scale(sd * 6.5)).add(0, 2, 0);
            sl.sendParticles(powered ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL, tip.x, tip.y, tip.z, 3, 0.2, 0.2, 0.2, 0.02);
        }
        if (powered && s.t % 2 == 0) sl.sendParticles(VOID, d.getX(), d.getY() + 2, d.getZ(), 4, 2, 1, 2, 0);
        if (powered && !s.awakened) {
            s.awakened = true;
            STATS[6]++;
            announce(sl, d, "message.tycooncraft.dragon.awaken", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8f, 0.6f);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, d.getX(), d.getY() + 2, d.getZ(), 200, 3, 2, 3, 0.2);
        }
        List<LivingEntity> foes = foes(sl, d, 128);
        if (foes.isEmpty()) return;
        LivingEntity t = foes.stream().min(java.util.Comparator.comparingDouble(d::distanceToSqr)).orElse(foes.get(0));
        float speed = powered ? 1.5f : 1f;
        Vec3 head = d.head.position();

        // 1) 화염구 부채꼴 (비행 중)
        if (!phase.isSitting() && --s.fireCd <= 0) {
            STATS[7]++;
            Vec3 aim = t.getEyePosition().subtract(head).normalize();
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = aim.yRot(i * 0.14f);
                DragonFireball fb = new DragonFireball(sl, d, dir.scale(1.2));
                fb.setPos(head.add(dir.scale(2)));
                sl.addFreshEntity(fb);
            }
            sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 6f, 0.8f);
            s.fireCd = (int) (140 / speed);
        }
        // 2) 셜커 탄 군단: 모든 대상에게 유도탄
        if (--s.bulletCd <= 0) {
            STATS[8]++;
            for (int i = 0; i < Math.min(8, foes.size() * 3); i++) {
                LivingEntity e = foes.get(i % foes.size());
                ShulkerBullet b = new ShulkerBullet(sl, d, e, Direction.Axis.values()[i % 3]);
                b.setPos(d.getX() + sl.random.nextGaussian() * 2, d.getY() + 3, d.getZ() + sl.random.nextGaussian() * 2);
                sl.addFreshEntity(b);
            }
            sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 5f, 0.6f);
            s.bulletCd = (int) (260 / speed);
        }
        // 3) 착지 충격파: 앉아 있을 때 주기적으로 공허 고리
        if (phase.isSitting() && --s.shockCd <= 0) {
            STATS[4]++;
            s.shockCd = powered ? 40 : 60;
            Vec3 c = d.position();
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6f, 1.2f);
            for (int r = 2; r <= 12; r += 2)
                for (int i = 0; i < r * 6; i++) {
                    double a = i * Math.PI * 2 / (r * 6);
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0.02);
                }
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, d.getBoundingBox().inflate(12, 4, 12))) {
                if (e == d) continue;
                double dist = e.distanceTo(d);
                if (dist > 12) continue;
                e.hurt(d.damageSources().indirectMagic(d, d), 10f);
                Vec3 push = e.position().subtract(c).normalize();
                e.push(push.x * 2.2, 0.8, push.z * 2.2);
                e.hurtMarked = true;
            }
        }
        // 4) (각성 후) 공허 붕괴: 모든 대상 발밑에 소용돌이 → 공허 기둥 폭발
        if (powered) {
            if (s.collapse < 0 && --s.collapseCd <= 0) {
                s.collapse = 0;
                STATS[9]++;
                s.marks.clear();
                for (LivingEntity e : foes.subList(0, Math.min(6, foes.size()))) s.marks.add(e.position());
                sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 3f, 1.5f);
            }
            if (s.collapse >= 0) {
                s.collapse++;
                if (s.collapse < 40 && s.collapse % 2 == 0) {
                    for (Vec3 m : s.marks) {
                        double gy = ground(sl, m.x, m.z) + 0.1;
                        double r = 3.2 - s.collapse * 0.05;
                        for (int i = 0; i < 14; i++) {
                            double a = i * Math.PI / 7 + s.collapse * 0.25;
                            sl.sendParticles(VOID, m.x + Math.cos(a) * r, gy, m.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                        }
                    }
                }
                if (s.collapse == 40) {
                    for (Vec3 m : s.marks) {
                        double gy = ground(sl, m.x, m.z);
                        for (double y = 0; y < 14; y += 0.7)
                            sl.sendParticles(ParticleTypes.DRAGON_BREATH, m.x, gy + y, m.z, 4, 0.4, 0.2, 0.4, 0.02);
                        AreaEffectCloud cloud = new AreaEffectCloud(sl, m.x, gy, m.z);
                        cloud.setOwner(d);
                        cloud.setParticle(ParticleTypes.DRAGON_BREATH);
                        cloud.setRadius(2.8f);
                        cloud.setDuration(100);
                        cloud.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 1));
                        sl.addFreshEntity(cloud);
                        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(m.x - 2.8, gy - 1, m.z - 2.8, m.x + 2.8, gy + 6, m.z + 2.8))) {
                            if (e == d) continue;
                            e.hurt(d.damageSources().indirectMagic(d, d), 12f);
                            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 2), d);
                        }
                    }
                    sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8f, 0.5f);
                }
                if (s.collapse > 50) {
                    s.collapse = -1;
                    s.collapseCd = 240;
                }
            }
        }
    }

    // 추가 전리품
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide) return;
        List<ItemStack> extra = new ArrayList<>();
        if (e instanceof WitherBoss) {
            extra.add(new ItemStack(Items.NETHERITE_SCRAP, 3));
            extra.add(new ItemStack(ModItems.COIN_DIAMOND.get(), 2));
        } else if (e instanceof EnderDragon) {
            extra.add(new ItemStack(Items.NETHERITE_INGOT, 2));
            extra.add(new ItemStack(ModItems.COIN_DIAMOND.get(), 5));
            extra.add(new ItemStack(Items.ELYTRA));
        }
        for (ItemStack s : extra)
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(e.level(), e.getX(), e.getY() + 1, e.getZ(), s));
    }

    private VanillaBossBuffs() {}
}
