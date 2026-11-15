package com.tycooncraft.gun;

import com.tycooncraft.registry.ModComponents;
import com.tycooncraft.registry.ModSounds;
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

    public static boolean silenced(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SILENCED.get(), false);
    }

    // ── 인챈트 반영 수치 ──
    public int magazine(ItemStack stack) {
        return Math.round(stats.magazine() * (1 + 0.25f * GunEnchants.level(stack, GunEnchants.EXTENDED_MAG)));
    }

    public int fireDelay(ItemStack stack) {
        return Math.max(1, Math.round(stats.fireDelay() * (1 - 0.15f * GunEnchants.level(stack, GunEnchants.RAPID_FIRE))));
    }

    public int reloadTicks(ItemStack stack) {
        return Math.max(5, Math.round(stats.reloadTicks() * (1 - 0.2f * GunEnchants.level(stack, GunEnchants.QUICK_RELOAD))));
    }

    public float damage(ItemStack stack) {
        return stats.damage() * (1 + 0.15f * GunEnchants.level(stack, GunEnchants.FIREPOWER)) * (silenced(stack) ? 0.9f : 1f);
    }

    private float precision(ItemStack stack) {
        return 1 - 0.25f * GunEnchants.level(stack, GunEnchants.PRECISION);
    }

    // ── 인챈트 가능 ──
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 14;
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
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DRY_FIRE.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
                player.getCooldowns().addCooldown(this, 6);
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
        player.getCooldowns().addCooldown(this, fireDelay(stack));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!stats.auto() || !(entity instanceof Player player)) return;
        int elapsed = USE_DURATION - remaining;
        if (elapsed > 0 && elapsed % fireDelay(stack) == 0) {
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
            player.getCooldowns().addCooldown(this, fireDelay(stack));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    // ── 발사 ──

    private void shoot(Level level, Player player, ItemStack stack) {
        float recoil = stats.recoil() * (1 - 0.2f * GunEnchants.level(stack, GunEnchants.PRECISION)) * (silenced(stack) ? 0.85f : 1f);
        if (level.isClientSide) {
            // 반동: 화면이 살짝 위로
            player.setXRot(player.getXRot() - recoil);
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * recoil * 0.5f);
            return;
        }
        ServerLevel server = (ServerLevel) level;
        int saver = GunEnchants.level(stack, GunEnchants.AMMO_SAVER);
        if (!player.getAbilities().instabuild && player.getRandom().nextFloat() >= 0.15f * saver) setAmmo(stack, ammo(stack) - 1);
        boolean quiet = silenced(stack);
        for (int i = 0; i < stats.pellets(); i++) fireRay(server, player, stack, quiet);
        float pitch = 0.96f + player.getRandom().nextFloat() * 0.08f;
        if (quiet) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SILENCED_FIRE.get(), SoundSource.PLAYERS, 1.0f, pitch);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), stats.fireSound().get(), SoundSource.PLAYERS, 4.0f, pitch);
        }
        // 총구 화염 + 연기 (소음기면 화염 없음, 연기 조금)
        Vec3 look = player.getViewVector(1f);
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 muzzle = player.getEyePosition().add(look.scale(quiet ? 1.7 : 1.3)).add(right.scale(0.25)).add(0, -0.18, 0);
        if (!quiet) server.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.02, 0.02, 0.02, 0.005);
        server.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, quiet ? 1 : 4, 0.04, 0.04, 0.04, 0.012);
    }

    private void fireRay(ServerLevel level, Player player, ItemStack stack, boolean quiet) {
        RandomSource r = player.getRandom();
        Vec3 eye = player.getEyePosition();
        float spread = stats.spread() * precision(stack) * (player.isUsingItem() && stats.scoped() ? 0.1f : 1f);
        Vec3 dir = player.getViewVector(1f)
                .add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread).normalize();
        Vec3 end = eye.add(dir.scale(stats.range()));

        BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();

        // 탄 경로에 걸린 대상들 (가까운 순). 관통탄 레벨만큼 더 뚫고 지나감
        final Vec3 rayEnd = end;
        AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        List<EntityHitResult> hits = new java.util.ArrayList<>();
        for (Entity e : level.getEntities(player, box, e -> e.isAlive() && e.isPickable() && !e.isSpectator()
                && !player.isPassengerOfSameVehicle(e) && e != player.getVehicle())) {
            e.getBoundingBox().inflate(e.getPickRadius() + 0.1).clip(eye, rayEnd).ifPresent(p -> hits.add(new EntityHitResult(e, p)));
        }
        hits.sort(java.util.Comparator.comparingDouble(h -> h.getLocation().distanceToSqr(eye)));
        int maxHits = 1 + GunEnchants.level(stack, GunEnchants.PIERCING);
        float dmg = damage(stack);
        int fire = GunEnchants.level(stack, GunEnchants.INCENDIARY);
        int hunter = GunEnchants.level(stack, GunEnchants.HEADHUNTER);

        Vec3 impact = end;
        int n = 0;
        for (EntityHitResult hit : hits) {
            if (n >= maxHits) break;
            Entity target = hit.getEntity();
            impact = hit.getLocation();
            float d = dmg * (n == 0 ? 1f : 0.7f);
            boolean head = target instanceof LivingEntity le && le.getBbHeight() > 0.9f && impact.y >= le.getEyeY() - 0.28;
            if (head) d *= 1.5f + 0.5f * hunter;
            target.invulnerableTime = 0; // 연사가 무적 시간에 막히지 않게
            if (target.hurt(level.damageSources().playerAttack(player), d)) {
                if (fire > 0) target.igniteForSeconds(4);
                if (head) {
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT, impact.x, impact.y, impact.z, 10, 0.15, 0.15, 0.15, 0.3);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5f, 1.6f);
                    player.displayClientMessage(Component.translatable("message.tycooncraft.gun.headshot").withStyle(ChatFormatting.GOLD), true);
                }
            }
            level.sendParticles(fire > 0 ? ParticleTypes.FLAME : ParticleTypes.CRIT, impact.x, impact.y, impact.z, 6, 0.1, 0.1, 0.1, 0.2);
            n++;
        }
        if (n == 0 && blockHit.getType() == HitResult.Type.BLOCK) {
            var state = level.getBlockState(blockHit.getBlockPos());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), impact.x, impact.y, impact.z, 6, 0.05, 0.05, 0.05, 0.1);
        }
        if (n >= maxHits) impact = hits.get(maxHits - 1).getLocation();

        // 탄도 궤적 (소음기면 안 보이게)
        if (quiet) return;
        double dist = impact.distanceTo(eye);
        for (double d = 2; d < dist; d += 2.5) {
            Vec3 p = eye.add(dir.scale(d));
            level.sendParticles(fire > 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.ELECTRIC_SPARK, p.x, p.y - 0.1, p.z, 1, 0, 0, 0, 0);
        }
    }

    // ── 재장전 ──

    private void reload(Level level, Player player, ItemStack stack) {
        int current = ammo(stack);
        int need = magazine(stack) - current;
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
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DRY_FIRE.get(), SoundSource.PLAYERS, 0.8f, 0.9f);
            return;
        }
        setAmmo(stack, current + loaded);
        player.getCooldowns().addCooldown(this, reloadTicks(stack));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), stats.reloadSound().get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable("message.tycooncraft.gun.reloaded", current + loaded, magazine(stack)), true);
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
        return Math.round(13f * ammo(stack) / magazine(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = (float) ammo(stack) / magazine(stack);
        return Mth.hsvToRgb(f / 3f, 1f, 1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tycooncraft.gun.ammo", ammo(stack), magazine(stack)).withStyle(ChatFormatting.YELLOW));
        String one = String.format(java.util.Locale.ROOT, "%.1f", damage(stack)).replace(".0", "");
        String dmg = stats.pellets() > 1 ? one + " x" + stats.pellets() : one;
        tooltip.add(Component.translatable("tooltip.tycooncraft.gun.stats", dmg, (int) stats.range()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tycooncraft.gun.uses", stats.ammo().get().getDescription()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(stats.scoped() ? "tooltip.tycooncraft.gun.how_scoped"
                : stats.auto() ? "tooltip.tycooncraft.gun.how_auto" : "tooltip.tycooncraft.gun.how_semi").withStyle(ChatFormatting.DARK_GRAY));
        if (silenced(stack)) tooltip.add(Component.translatable("tooltip.tycooncraft.gun.silenced").withStyle(ChatFormatting.AQUA));
    }
}
