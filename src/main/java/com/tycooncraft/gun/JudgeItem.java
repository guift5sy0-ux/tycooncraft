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
 * - 관통 없음, 헤드샷 1.8배, 보라빛 공허 탄도
 * - 맞은 대상에 "심판의 표식"(발광 3초). 표식이 있는 대상을 다시 맞히면 심판: 번개 + 방어 무시 피해 6 (대상마다 재사용 6초)
 * - 내구도 500: 한 발마다 1 소모, 다 닳으면 총이 터짐. 모루에서 네더라이트 주괴로 수리, 수선 인챈트 가능
 * - 총열 아래 날: 근접 공격 피해 12
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
                0, 1.8f + 0.4f * hunter,
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
        wear(level, player, stack);
    }

    public static final int DURABILITY = 500;

    /** 한 발마다 내구도 1. 0이 되면 총이 터진다 */
    private static void wear(ServerLevel level, Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) return;
        int left = stack.getMaxDamage() - stack.getDamageValue() - 1;
        if (left <= 0) {
            stack.shrink(1);
            level.explode(player, player.getX(), player.getEyeY() - 0.4, player.getZ(), 1.6f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getEyeY(), player.getZ(), 80, 0.5, 0.5, 0.5, 0.3);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.5f, 0.6f);
            player.displayClientMessage(Component.translatable("message.tycooncraft.judge.broke").withStyle(ChatFormatting.DARK_RED), true);
            return;
        }
        stack.setDamageValue(stack.getDamageValue() + 1);
        if (left == 50 || left == 20 || left <= 5)
            player.displayClientMessage(Component.translatable("message.tycooncraft.judge.wear", left).withStyle(ChatFormatting.RED), true);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.NETHERITE_INGOT);
    }

    private static void judge(ServerLevel level, Player player, Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive()) return;
        long now = level.getGameTime();
        long marked = le.getPersistentData().getLong(MARK);
        long lastJudge = le.getPersistentData().getLong(MARK + "_cd");
        if (marked > 0 && now - marked <= 60 && now - marked > 0 && now - lastJudge > 120) {
            le.getPersistentData().remove(MARK);
            le.getPersistentData().putLong(MARK + "_cd", now);
            le.removeEffect(MobEffects.GLOWING);
            judgments++;
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(le.getX(), le.getY(), le.getZ());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
                // 번개 연출만: 혹시 생긴 불은 바로 끔 (기지·지형 보호)
                var center = le.blockPosition();
                for (int d = 2; d <= 14; d += 4) {
                    level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount() + d, () -> {
                        for (var bp : net.minecraft.core.BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 3, 2)))
                            if (level.getBlockState(bp).is(net.minecraft.world.level.block.Blocks.FIRE)) level.removeBlock(bp, false);
                    }));
                }
            }
            le.invulnerableTime = 0;
            le.hurt(level.damageSources().indirectMagic(player, player), 6f);
            level.sendParticles(ParticleTypes.END_ROD, le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(), 30, 0.4, 0.8, 0.4, 0.15);
            level.playSound(null, le.getX(), le.getY(), le.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 2.5f, 1.2f);
            player.displayClientMessage(Component.translatable("message.tycooncraft.judge.judgment").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else if ((marked == 0 || now - marked > 60) && now - lastJudge > 120) {
            le.getPersistentData().putLong(MARK, now);
            le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
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
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("tooltip.tycooncraft.judge.durability", left, stack.getMaxDamage())
                .withStyle(left < 50 ? ChatFormatting.RED : ChatFormatting.AQUA));
    }
}
