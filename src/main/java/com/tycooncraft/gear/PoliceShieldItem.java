package com.tycooncraft.gear;

import com.tycooncraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

import java.util.List;

/**
 * 경찰 방패(진압 방패): 일반 방패보다 크고 튼튼함(내구도 1200).
 * 들고 막는 동안 앞에서 오는 총알·근접·폭발을 막고, 밀려나지 않으며, 막은 총알은 뒤로 관통하지 않는다.
 */
public class PoliceShieldItem extends ShieldItem {
    public PoliceShieldItem(Properties props) {
        super(props);
    }

    public static boolean blockingWith(LivingEntity e) {
        return e.isBlocking() && e.getUseItem().getItem() instanceof PoliceShieldItem;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModItems.STEEL_PLATE.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tycooncraft.police_shield").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tycooncraft.police_shield2").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.tycooncraft.police_shield3").withStyle(ChatFormatting.GRAY));
    }

    /** 막는 중에는 밀쳐내기 무시 */
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (blockingWith(event.getEntity())) event.setCanceled(true);
    }

    public static boolean isPlayer(LivingEntity e) {
        return e instanceof Player;
    }

    // ── 방패 공격 ──
    /** 방패로 때리면 피해 6 고정 (맨손 1 + 5) */
    public static net.minecraft.world.item.component.ItemAttributeModifiers attributes() {
        return net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.0,
                                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static final int BASH_TICKS = 8, BASH_COOLDOWN = 80, STUN_TICKS = 10;
    public static final float BASH_DAMAGE = 6f;
    private static final java.util.Map<java.util.UUID, int[]> DASH = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, java.util.Set<Integer>> HIT = new java.util.HashMap<>();
    /** 기절: 엔티티 → 풀리는 게임 시각 */
    private static final java.util.Map<java.util.UUID, Long> STUN = new java.util.HashMap<>();
    public static int BASH_HITS;

    public static boolean stunned(net.minecraft.world.entity.Entity e) {
        Long t = STUN.get(e.getUUID());
        return t != null && e.level().getGameTime() < t;
    }

    /** 오른손에 든 채 Q: 보는 방향으로 돌진 */
    public static void bash(net.minecraft.server.level.ServerPlayer p) {
        ItemStack st = p.getMainHandItem();
        if (!(st.getItem() instanceof PoliceShieldItem) || p.getCooldowns().isOnCooldown(st.getItem()) || stunned(p)) return;
        net.minecraft.world.phys.Vec3 look = p.getLookAngle();
        net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(look.x, 0, look.z).normalize();
        p.setDeltaMovement(dir.x * 1.35, Math.max(p.getDeltaMovement().y, 0.12), dir.z * 1.35);
        p.hurtMarked = true;
        p.getCooldowns().addCooldown(st.getItem(), BASH_COOLDOWN);
        DASH.put(p.getUUID(), new int[]{BASH_TICKS});
        HIT.put(p.getUUID(), new java.util.HashSet<>());
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.6f);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)) return;
        int[] left = DASH.get(p.getUUID());
        if (left == null) return;
        if (--left[0] < 0) {
            DASH.remove(p.getUUID());
            HIT.remove(p.getUUID());
            return;
        }
        var sl = p.serverLevel();
        net.minecraft.world.phys.Vec3 look = p.getLookAngle();
        net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(look.x, 0, look.z).normalize();
        if (left[0] > BASH_TICKS / 2) {   // 앞쪽 절반 동안 계속 밀어 줌
            p.setDeltaMovement(dir.x * 1.2, p.getDeltaMovement().y, dir.z * 1.2);
            p.hurtMarked = true;
        }
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
        var box = p.getBoundingBox().expandTowards(dir.scale(1.2)).inflate(0.4, 0.1, 0.4);
        var hit = HIT.get(p.getUUID());
        boolean any = false;
        for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !hit.contains(e.getId()))) {
            if (t instanceof Player tp && com.tycooncraft.alliance.Alliances.friendly(sl.getServer(), p.getUUID(), tp.getUUID())) continue;
            if (t instanceof net.minecraft.world.entity.decoration.ArmorStand) continue;
            hit.add(t.getId());
            any = true;
            BASH_HITS++;
            t.hurt(sl.damageSources().playerAttack(p), BASH_DAMAGE);
            // 플레이어와 비슷한 크기면 날아감
            if (t.getBbWidth() <= 1.0f && t.getBbHeight() <= 2.4f) {
                t.setDeltaMovement(dir.x * 1.7, 0.55, dir.z * 1.7);
                t.hurtMarked = true;
            }
            STUN.put(t.getUUID(), sl.getGameTime() + STUN_TICKS);
            t.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, STUN_TICKS, 6, false, false));
            if (t instanceof net.minecraft.world.entity.Mob m) m.getNavigation().stop();
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.7, t.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
            sl.playSound(null, t.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.7f);
        }
        if (any) {   // 부딪히면 멈춤
            p.setDeltaMovement(p.getDeltaMovement().multiply(0.15, 1, 0.15));
            p.hurtMarked = true;
            left[0] = Math.min(left[0], 1);
        }
    }

    /** 기절한 동안은 공격 못 함 */
    @SubscribeEvent
    public static void onStunnedAttack(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        var src = event.getSource().getEntity();
        if (src != null && src == event.getSource().getDirectEntity() && stunned(src)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onStunnedSwing(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (stunned(event.getEntity())) event.setCanceled(true);
    }
}
