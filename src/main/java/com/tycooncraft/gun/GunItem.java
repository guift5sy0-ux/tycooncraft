package com.tycooncraft.gun;

import com.tycooncraft.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 총. 우클릭 발사 (연사형은 누르고 있기, 저격형은 꾹 눌러 조준 후 떼서 발사).
 * 웅크리고 우클릭 = 재장전 (인벤토리의 탄약 사용).
 */
public class GunItem extends Item {
    private static final int USE_DURATION = 72000;
    private final GunStats stats;

    public GunItem(Properties properties, GunStats stats) {
        super(properties.stacksTo(1));
        this.stats = stats;
    }

    public GunStats stats() {
        return stats;
    }

    public static int ammo(ItemStack stack) {
        return stack.getOrDefault(ModComponents.AMMO.get(), 0);
    }

    private static void setAmmo(ItemStack stack, int n) {
        stack.set(ModComponents.AMMO.get(), n);
    }

    // ── 사용 ──

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        if (player.isShiftKeyDown()) {
            reload(level, player, stack);
            return InteractionResultHolder.consume(stack);
        }
        if (ammo(stack) <= 0) {
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.6f);
                player.displayClientMessage(Component.translatable("message.tycooncraft.gun.empty").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (stats.auto() || stats.scoped()) {
            player.startUsingItem(hand);
            if (stats.auto()) shoot(level, player, stack); // 첫 발은 바로
            return InteractionResultHolder.consume(stack);
        }
        shoot(level, player, stack);
        player.getCooldowns().addCooldown(this, stats.fireDelay());
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!stats.auto() || !(entity instanceof Player player)) return;
        int elapsed = USE_DURATION - remaining;
        if (elapsed > 0 && elapsed % stats.fireDelay() == 0) {
            if (ammo(stack) <= 0) {
                player.stopUsingItem();
                return;
            }
            shoot(level, player, stack);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!stats.scoped() || !(entity instanceof Player player)) return;
        if (USE_DURATION - timeLeft >= 6 && ammo(stack) > 0) {
            shoot(level, player, stack);
            player.getCooldowns().addCooldown(this, stats.fireDelay());
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return stats.scoped() ? UseAnim.BOW : UseAnim.NONE;
    }

    // ── 발사 ──

    private void shoot(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) {
            // 반동: 화면이 살짝 위로
            player.setXRot(player.getXRot() - stats.recoil());
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * stats.recoil() * 0.5f);
            return;
        }
        ServerLevel server = (ServerLevel) level;
        if (!player.getAbilities().instabuild) setAmmo(stack, ammo(stack) - 1);
        for (int i = 0; i < stats.pellets(); i++) fireRay(server, player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), stats.sound(), SoundSource.PLAYERS, 1.0f, stats.pitch());
        Vec3 muzzle = player.getEyePosition().add(player.getViewVector(1f).scale(1.2));
        server.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y - 0.1, muzzle.z, 3, 0.03, 0.03, 0.03, 0.01);
    }

    private void fireRay(ServerLevel level, Player player) {
        RandomSource r = player.getRandom();
        Vec3 eye = player.getEyePosition();
        float spread = stats.spread() * (player.isUsingItem() && stats.scoped() ? 0.1f : 1f);
        Vec3 dir = player.getViewVector(1f)
                .add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread).normalize();
        Vec3 end = eye.add(dir.scale(stats.range()));

        BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();

        AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, end, box,
                e -> e.isAlive() && e.isPickable() && !e.isSpectator() && e != player);

        Vec3 impact = end;
        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            impact = entityHit.getLocation();
            target.invulnerableTime = 0; // 연사가 무적 시간에 막히지 않게
            target.hurt(level.damageSources().playerAttack(player), stats.damage());
            level.sendParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z, 6, 0.1, 0.1, 0.1, 0.2);
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            var state = level.getBlockState(blockHit.getBlockPos());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), impact.x, impact.y, impact.z, 6, 0.05, 0.05, 0.05, 0.1);
        }

        // 탄도 궤적
        double dist = impact.distanceTo(eye);
        for (double d = 2; d < dist; d += 2.5) {
            Vec3 p = eye.add(dir.scale(d));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y - 0.1, p.z, 1, 0, 0, 0, 0);
        }
    }

    // ── 재장전 ──

    private void reload(Level level, Player player, ItemStack stack) {
        int current = ammo(stack);
        int need = stats.magazine() - current;
        if (need <= 0) return;
        if (level.isClientSide) return;

        int loaded;
        if (player.getAbilities().instabuild) {
            loaded = need;
        } else {
            loaded = takeAmmo(player.getInventory(), stats.ammo().get(), need);
        }
        if (loaded <= 0) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.gun.no_ammo",
                    stats.ammo().get().getDescription()).withStyle(ChatFormatting.RED), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.2f);
            return;
        }
        setAmmo(stack, current + loaded);
        player.getCooldowns().addCooldown(this, stats.reloadTicks());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.8f, 1.8f);
        player.displayClientMessage(Component.translatable("message.tycooncraft.gun.reloaded", current + loaded, stats.magazine()), true);
    }

    private static int takeAmmo(Inventory inv, Item ammo, int need) {
        int taken = 0;
        for (int i = 0; i < inv.getContainerSize() && taken < need; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ammo)) {
                int t = Math.min(need - taken, s.getCount());
                s.shrink(t);
                taken += t;
            }
        }
        return taken;
    }

    // ── 표시 ──

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * ammo(stack) / stats.magazine());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = (float) ammo(stack) / stats.magazine();
        return Mth.hsvToRgb(f / 3f, 1f, 1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tycooncraft.gun.ammo", ammo(stack), stats.magazine()).withStyle(ChatFormatting.YELLOW));
        String dmg = stats.pellets() > 1 ? (int) stats.damage() + " x" + stats.pellets() : String.valueOf((int) stats.damage());
        tooltip.add(Component.translatable("tooltip.tycooncraft.gun.stats", dmg, (int) stats.range()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tycooncraft.gun.uses", stats.ammo().get().getDescription()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(stats.scoped() ? "tooltip.tycooncraft.gun.how_scoped"
                : stats.auto() ? "tooltip.tycooncraft.gun.how_auto" : "tooltip.tycooncraft.gun.how_semi").withStyle(ChatFormatting.DARK_GRAY));
    }
}
