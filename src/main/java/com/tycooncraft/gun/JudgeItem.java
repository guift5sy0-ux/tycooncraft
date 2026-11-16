package com.tycooncraft.gun;

import com.tycooncraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * 날카로운 반자동 "이계의 심판자".
 * - 관통 3명, 헤드샷 2.2배, 보라빛 공허 탄도
 * - 맞은 대상에 "심판의 표식"(발광 5초). 표식이 있는 대상을 다시 맞히면 심판: 번개 + 방어 무시 피해 14
 * - 총열 아래 날: 근접 공격 피해 12 (네더라이트 검보다 강함)
 */
public class JudgeItem extends GunItem {
    private static final String MARK = "tycooncraft_judge_mark";
    private static final DustParticleOptions VOID = new DustParticleOptions(new Vector3f(0.65f, 0.25f, 1f), 1.2f);
    public static int judgments;

    public JudgeItem(Properties p, GunStats stats) {
        super(p, stats);
    }

    @Override
    protected void fire(ServerLevel level, Player player, ItemStack stack, int silencerTier) {
        Vec3 dir = player.getViewVector(1f);
        Vec3 from = player.getEyePosition();
        int hunter = GunEnchants.level(stack, GunEnchants.HEADHUNTER);
        Vec3 hit = Ballistics.shoot(level, player, from, dir, stats().range(), damage(stack),
                3 + GunEnchants.level(stack, GunEnchants.PIERCING), 2.2f + 0.5f * hunter,
                GunEnchants.level(stack, GunEnchants.INCENDIARY) > 0, false, level.damageSources().playerAttack(player),
                e -> !player.isPassengerOfSameVehicle(e) && e != player.getVehicle(), e -> judge(level, player, e));
        // 공허 탄도
        double dist = Math.min(hit.distanceTo(from), stats().range());
        for (double t = 1.5; t < dist; t += 0.9) {
            Vec3 q = from.add(dir.scale(t)).add(0, -0.12, 0);
            level.sendParticles(VOID, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
            if (((int) (t * 10)) % 3 == 0) level.sendParticles(ParticleTypes.REVERSE_PORTAL, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.01);
        }
        level.sendParticles(ParticleTypes.DRAGON_BREATH, hit.x, hit.y, hit.z, 8, 0.15, 0.15, 0.15, 0.02);
    }

    private static void judge(ServerLevel level, Player player, Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive()) return;
        long now = level.getGameTime();
        long marked = le.getPersistentData().getLong(MARK);
        if (marked > 0 && now - marked <= 100 && now - marked > 0) {
            le.getPersistentData().remove(MARK);
            le.removeEffect(MobEffects.GLOWING);
            judgments++;
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(le.getX(), le.getY(), le.getZ());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            le.invulnerableTime = 0;
            le.hurt(level.damageSources().indirectMagic(player, player), 14f);
            level.sendParticles(ParticleTypes.END_ROD, le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(), 30, 0.4, 0.8, 0.4, 0.15);
            level.playSound(null, le.getX(), le.getY(), le.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 2.5f, 1.2f);
            player.displayClientMessage(Component.translatable("message.tycooncraft.judge.judgment").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else if (marked == 0 || now - marked > 100) {
            le.getPersistentData().putLong(MARK, now);
            le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
            level.sendParticles(ParticleTypes.WITCH, le.getX(), le.getY() + le.getBbHeight() + 0.3, le.getZ(), 8, 0.2, 0.1, 0.2, 0);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 1, 0, 0, 0, 0);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.3f);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tycooncraft.judge.lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.tycooncraft.judge.mark").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.tycooncraft.judge.blade").withStyle(ChatFormatting.GRAY));
    }
}
