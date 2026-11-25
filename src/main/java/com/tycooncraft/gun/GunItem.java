package com.tycooncraft.gun;

import com.tycooncraft.registry.ModComponents;
import com.tycooncraft.registry.ModItems;
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
    /** 전체 총기 피해 배율 (v0.15.6: 하향 평준화 — 기본값 자체를 낮춤) */
    public static final float DAMAGE_SCALE = 1.0f;

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

    public static void setAmmo(ItemStack stack, int n) {
        stack.set(ModComponents.AMMO.get(), n);
    }

    /** 소음기 등급: 0 없음, 1 소음기, 2 상위 소음기 (VSS 는 일체형이라 항상 2) */
    public static int silencerTier(ItemStack stack) {
        if (stack.is(ModItems.VSS.get())) return 2;
        int t = stack.getOrDefault(ModComponents.SILENCER_TIER.get(), 0);
        if (t == 0 && stack.getOrDefault(ModComponents.SILENCED.get(), false)) t = 1;
        return t;
    }

    public static boolean silenced(ItemStack stack) {
        return silencerTier(stack) > 0;
    }

    /** 소음기를 달 수 있는 총 (단발 총열) */
    public static boolean canTakeSilencer(ItemStack stack) {
        return stack.is(ModItems.PISTOL.get()) || stack.is(ModItems.SMG.get()) || stack.is(ModItems.RIFLE.get())
                || stack.is(ModItems.SHOTGUN.get()) || stack.is(ModItems.SNIPER.get());
    }

    // ── 인챈트 반영 수치 ──
    public int magazine(ItemStack stack) {
        return Math.round(stats.magazine() * (1 + 0.25f * GunEnchants.level(stack, GunEnchants.EXTENDED_MAG)));
    }

    public int fireDelay(ItemStack stack) {
        if (stack.is(ModItems.VSS.get()) && fireMode(stack) == 1) return 3;
        return Math.max(1, Math.round(stats.fireDelay() * (1 - 0.15f * GunEnchants.level(stack, GunEnchants.RAPID_FIRE))));
    }

    public static final int RT20_RELOAD = 280;   // 14초 고정 (인챈트·파츠 무시)
    public static final float RT20_RECOIL_DMG = 4f;

    public int reloadTicks(ItemStack stack) {
        if (stack.is(ModItems.RT20.get())) return RT20_RELOAD;
        return Math.max(5, Math.round(stats.reloadTicks() * (1 - 0.2f * GunEnchants.level(stack, GunEnchants.QUICK_RELOAD))));
    }

    public float damage(ItemStack stack) {
        float sil = silencerTier(stack) == 1 && !stack.is(ModItems.VSS.get()) ? 0.93f : 1f;
        return stats.damage() * DAMAGE_SCALE * (1 + 0.15f * GunEnchants.level(stack, GunEnchants.FIREPOWER)) * sil;
    }

    private float precision(ItemStack stack) {
        return (1 - 0.25f * GunEnchants.level(stack, GunEnchants.PRECISION)) * GunParts.spreadMult(stack);
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
        if (hand == InteractionHand.OFF_HAND) return InteractionResultHolder.fail(stack);   // 왼손 사격 금지
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (isDual() && !player.getOffhandItem().isEmpty()) {   // 쌍권총은 왼손이 비어 있어야
            if (!level.isClientSide) {} /* v0.18.9: 전투 안내 문구 제거 */
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown()) {
            reload(level, player, stack);
            return InteractionResultHolder.consume(stack);
        }
        if (ammo(stack) <= 0) {
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DRY_FIRE.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
                player.getCooldowns().addCooldown(this, 6);
                {} /* v0.18.9: 전투 안내 문구 제거 */
            }
            return InteractionResultHolder.fail(stack);
        }
        if (isAuto(stack) || isScoped(stack)) {
            player.startUsingItem(hand);
            if (isAuto(stack)) shoot(level, player, stack); // 첫 발은 바로
            return InteractionResultHolder.consume(stack);
        }
        shoot(level, player, stack);
        player.getCooldowns().addCooldown(this, fireDelay(stack));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!isAuto(stack) || !(entity instanceof Player player)) return;
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
        if (!isScoped(stack) || !(entity instanceof Player player)) return;
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
        if (isDual() && !player.getOffhandItem().isEmpty()) return;
        int tier = silencerTier(stack);
        float recoil = stats.recoil() * (1 - 0.2f * GunEnchants.level(stack, GunEnchants.PRECISION)) * (tier == 2 ? 0.75f : tier == 1 ? 0.85f : 1f) * GunParts.recoilMult(stack);
        if (level.isClientSide) {
            // 반동: 화면이 살짝 위로
            player.setXRot(player.getXRot() - recoil);
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * recoil * 0.5f);
            // 쏜 사람은 서버 왕복 없이 바로 총소리를 듣는다 (다른 사람에겐 서버가 들려줌)
            float p0 = 0.96f + player.getRandom().nextFloat() * 0.08f;
            if (tier == 2) level.playSound(player, player.getX(), player.getY(), player.getZ(), ModSounds.SILENCED_FIRE.get(), SoundSource.PLAYERS, 0.75f, p0);
            else if (tier == 1) level.playSound(player, player.getX(), player.getY(), player.getZ(), ModSounds.SILENCED_FIRE.get(), SoundSource.PLAYERS, 1.0f, p0 * 0.92f);
            else level.playSound(player, player.getX(), player.getY(), player.getZ(), stats.fireSound().get(), SoundSource.PLAYERS, 1.0f, p0);
            return;
        }
        ServerLevel server = (ServerLevel) level;
        // v0.26.0 밀수품: 5% 확률로 탄 걸림 (발사 안 됨, 잠깐 멈춤)
        if (com.tycooncraft.blackmarket.DealerTrades.smuggled(stack) && player.getRandom().nextFloat() < 0.05f) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DRY_FIRE.get(), SoundSource.PLAYERS, 1.0f, 0.7f);
            player.getCooldowns().addCooldown(this, 16);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tycooncraft.jam"), true);
            return;
        }
        int saver = GunEnchants.level(stack, GunEnchants.AMMO_SAVER);
        int before = ammo(stack);
        if (!player.getAbilities().instabuild && player.getRandom().nextFloat() >= 0.15f * saver) setAmmo(stack, before - 1);
        fire(server, player, stack, tier);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) com.tycooncraft.village.GuardAlert.onGunfire(sp);
        if (stack.is(ModItems.RT20.get())) rt20Kick(server, player);
        // 쌍권총: 다음 발은 반대쪽 총
        if (isDual()) player.getPersistentData().putBoolean("tycooncraft_dual_left", !player.getPersistentData().getBoolean("tycooncraft_dual_left"));
        float pitch = 0.96f + player.getRandom().nextFloat() * 0.08f;
        if (tier == 2) {
            level.playSound(player, player.getX(), player.getY(), player.getZ(), ModSounds.SILENCED_FIRE.get(), SoundSource.PLAYERS, 0.75f, pitch);
        } else if (tier == 1) {
            // 일반 소음기: 생각보다 크게 들림 (약 32블록)
            level.playSound(player, player.getX(), player.getY(), player.getZ(), ModSounds.SILENCED_FIRE.get(), SoundSource.PLAYERS, 2.0f, pitch * 0.92f);
        } else {
            level.playSound(player, player.getX(), player.getY(), player.getZ(), stats.fireSound().get(), SoundSource.PLAYERS, 4.0f, pitch);
        }
        // 총구 화염 + 연기 (소음기면 화염 없음). 쌍권총은 좌우 번갈아
        Vec3 look = player.getViewVector(1f);
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        double side = isDual() ? (player.getPersistentData().getBoolean("tycooncraft_dual_left") ? 0.32 : -0.32) : 0.25;
        Vec3 muzzle = player.getEyePosition().add(look.scale(tier > 0 ? 1.7 : 1.3)).add(right.scale(side)).add(0, -0.18, 0);
        // v0.20.0: 총구 연기·섬광·탄피는 클라이언트(BulletFx)가 현실적으로 그림
    }

    /** VSS: F 로 바꾸는 사격 모드 (0 = 단발·조준, 1 = 연사·조준 없음) */
    public static int fireMode(ItemStack stack) {
        return stack.getOrDefault(ModComponents.FIRE_MODE.get(), 0);
    }

    public boolean isAuto(ItemStack stack) {
        if (stack.is(ModItems.VSS.get())) return fireMode(stack) == 1;
        return stats.auto();
    }

    public boolean isScoped(ItemStack stack) {
        return hasScope(stack) && fireMode(stack) == 0;
    }

    /** 스코프가 달린 총 (F 로 조준 사격 ↔ 일반 사격 전환) */
    public boolean hasScope(ItemStack stack) {
        return stack.is(ModItems.VSS.get()) || stats.scoped() || GunParts.has(stack, GunPart.SCOPE);
    }

    public boolean isDual() {
        return this == ModItems.DUAL_PISTOLS.get();
    }

    /** 실제 발사 (RPG 는 로켓으로 덮어씀) */
    protected void fire(ServerLevel level, Player player, ItemStack stack, int silencerTier) {
        for (int i = 0; i < stats.pellets(); i++) fireRay(level, player, stack, silencerTier > 0);
    }

    private void fireRay(ServerLevel level, Player player, ItemStack stack, boolean quiet) {
        RandomSource r = player.getRandom();
        float spread = stats.spread() * precision(stack) * (player.isUsingItem() && isScoped(stack) ? 0.1f : 1f);
        Vec3 dir = player.getViewVector(1f)
                .add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread).normalize();
        int hunter = GunEnchants.level(stack, GunEnchants.HEADHUNTER);
        Vec3 origin = player.getEyePosition();
        if (isDual()) {
            // 오른쪽·왼쪽 총구에서 번갈아 (탄도 궤적도 그 총에서 나감)
            boolean left = player.getPersistentData().getBoolean("tycooncraft_dual_left");
            Vec3 right = player.getViewVector(1f).cross(new Vec3(0, 1, 0)).normalize();
            origin = origin.add(right.scale(left ? -0.32 : 0.32)).add(0, -0.2, 0).add(player.getViewVector(1f).scale(0.6));
        }
        boolean rt20 = stack.is(ModItems.RT20.get());
        Ballistics.nextAntiMaterial = rt20;
        Ballistics.shoot(level, player, origin, dir, stats.range(), damage(stack),
                GunEnchants.level(stack, GunEnchants.PIERCING) + (rt20 ? 1 : 0), 1.5f + 0.5f * hunter,
                GunEnchants.level(stack, GunEnchants.INCENDIARY) > 0, !quiet, level.damageSources().playerAttack(player),
                e -> !player.isPassengerOfSameVehicle(e) && e != player.getVehicle());
    }

    /** RT-20 반동: 고정 피해 4 (방어구·저항 무시) + 뒤로 2칸 (웅크리면 1칸) */
    public static void rt20Kick(ServerLevel level, Player player) {
        var type = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, com.tycooncraft.TycoonCraft.id("recoil")));
        player.invulnerableTime = 0;
        player.hurt(new net.minecraft.world.damagesource.DamageSource(type), RT20_RECOIL_DMG);
        Vec3 back = player.getLookAngle().multiply(1, 0, 1);
        if (back.lengthSqr() < 1e-4) back = Vec3.directionFromRotation(0, player.getYRot());
        double v = player.isShiftKeyDown() ? 0.46 : 0.9;   // 지면 마찰(0.546) 기준 약 1칸 / 2칸
        back = back.normalize().scale(-v);
        player.setDeltaMovement(back.x, player.getDeltaMovement().y, back.z);
        player.hurtMarked = true;
        RT20_KICKS++;
    }

    public static int RT20_KICKS;

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
            {} /* v0.18.9: 전투 안내 문구 제거 */
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DRY_FIRE.get(), SoundSource.PLAYERS, 0.8f, 0.9f);
            return;
        }
        setAmmo(stack, current + loaded);
        player.getCooldowns().addCooldown(this, com.tycooncraft.blackmarket.BlackMarket.reloadTicks(player, reloadTicks(stack)));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), stats.reloadSound().get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        {} /* v0.18.9: 전투 안내 문구 제거 */
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
        int st = silencerTier(stack);
        if (stack.is(ModItems.RT20.get())) {
            tooltip.add(Component.translatable("tooltip.tycooncraft.rt20").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.tycooncraft.rt20_2").withStyle(ChatFormatting.GOLD));
        }
        if (stack.is(ModItems.VSS.get())) {
            tooltip.add(Component.translatable("tooltip.tycooncraft.gun.integral").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable(fireMode(stack) == 1 ? "tooltip.tycooncraft.gun.mode_auto" : "tooltip.tycooncraft.gun.mode_semi").withStyle(ChatFormatting.YELLOW));
        }
        else if (hasScope(stack))
            tooltip.add(Component.translatable(fireMode(stack) == 1 ? "tooltip.tycooncraft.gun.mode_hip" : "tooltip.tycooncraft.gun.mode_scope").withStyle(ChatFormatting.YELLOW));
        else if (st > 0) tooltip.add(Component.translatable(st == 2 ? "tooltip.tycooncraft.gun.silenced2" : "tooltip.tycooncraft.gun.silenced").withStyle(ChatFormatting.AQUA));
        for (GunPart p : GunParts.installed(stack))
            tooltip.add(Component.literal("⚙ ").append(Component.translatable("item.tycooncraft.part_" + p.id)).withStyle(ChatFormatting.GREEN));
        if (!GunParts.supported(stack).isEmpty()) tooltip.add(Component.translatable("tooltip.tycooncraft.gun.parts_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
