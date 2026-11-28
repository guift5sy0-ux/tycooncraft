package com.tycooncraft.blackmarket;

import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * v0.26.0 암상인 (0 무기 밀수꾼 · 1 희귀품 브로커 · 2 장물아비).
 * 거래는 마을 상인처럼, 다만 암시장에서 싸우는 자는 총으로 처리한다. 죽으면 다음 날 다시 나옴.
 */
public class BlackMarketDealer extends AbstractVillager implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(BlackMarketDealer.class, EntityDataSerializers.INT);
    public static final String[] KEYS = {"dealer.tycooncraft.arms", "dealer.tycooncraft.broker", "dealer.tycooncraft.fence"};
    private long stockDay = -1;

    public BlackMarketDealer(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, 8).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(KIND, 0);
    }

    public int kind() {
        return entityData.get(KIND);
    }

    public void setKind(int k) {
        entityData.set(KIND, k);
        setCustomName(Component.translatable(KEYS[k]));
        setCustomNameVisible(true);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(k == 0 ? ModItems.SMG.get() : k == 1 ? ModItems.SHOTGUN.get() : ModItems.PISTOL.get()));
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0f);
        offers = null;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 0.0, 8, 12, 40f));   // 노점에서 움직이지 않고 쏨
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, BlackMarketDealer.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 5, false, false, p -> p instanceof Player pl && BlackMarket.hostile(pl)));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (getTarget() instanceof Player p && !BlackMarket.hostile(p) && getLastHurtByMob() != p) setTarget(null);
        setAggressive(getTarget() != null);
        // 매일 새 진열
        long day = level().getServer().overworld().getDayTime() / 24000L;
        if (day != stockDay) {
            stockDay = day;
            offers = null;
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isTrading() || isBaby()) return super.mobInteract(player, hand);
        if (BlackMarket.hostile(player) || getTarget() == player) {
            if (!level().isClientSide) player.displayClientMessage(Component.translatable("message.tycooncraft.bm.refuse"), true);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide && !getOffers().isEmpty()) {
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void updateTrades() {
        offers = new MerchantOffers();
        offers.addAll(DealerTrades.stock(kind(), random));
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {}

    /** v0.28: 거래액 ₩5,000 당 평판 1 */
    @Override
    public void notifyTrade(MerchantOffer offer) {
        super.notifyTrade(offer);
        if (getTradingPlayer() instanceof ServerPlayer sp) {
            long won = com.tycooncraft.economy.Coins.valueOf(offer.getCostA()) + com.tycooncraft.economy.Coins.valueOf(offer.getCostB());
            // v0.28 평판 할인: 동전이 큰 단위라 값을 깎을 수 없어서, 결제 직후 할인분을 현금으로 돌려줌
            long rebate = Math.round(won * Reputation.DISCOUNT[Reputation.tier(sp)]);
            if (rebate > 0) {
                com.tycooncraft.economy.Coins.give(sp, rebate);
                sp.displayClientMessage(Component.translatable("message.tycooncraft.rep.rebate", com.tycooncraft.economy.Coins.format(rebate)), true);
            }
            repAcc += won;
            int rep = (int) (repAcc / 5000);
            repAcc %= 5000;
            if (rep > 0) Reputation.add(sp, rep);
        }
    }

    private long repAcc;

    @Override
    public boolean showProgressBar() {
        return false;
    }

    private final com.tycooncraft.gun.MobMag mag = new com.tycooncraft.gun.MobMag();

    @Override
    public void performRangedAttack(LivingEntity target, float pull) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (mag.busy(this)) return;
        mag.fired(this);
        Vec3 from = getEyePosition();
        Vec3 base = target.getBoundingBox().getCenter().subtract(from).normalize();
        int pellets = kind() == 1 ? 6 : 1;
        float spread = kind() == 1 ? 0.07f : 0.03f;
        float dmg = kind() == 0 ? 6f : kind() == 1 ? 4f : 8f;
        for (int i = 0; i < pellets; i++) {
            Vec3 dir = base.add(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread).normalize();
            Ballistics.shoot(sl, this, from.add(dir.scale(0.6)), dir, 40, dmg, 0, 1.5f, false, true, damageSources().mobAttack(this),
                    e -> !(e instanceof BlackMarketDealer) && (!(e instanceof Player p) || BlackMarket.hostile(p) || p == getLastHurtByMob()));
        }
        sl.playSound(null, getX(), getY(), getZ(), kind() == 1 ? ModSounds.SHOTGUN_FIRE.get() : kind() == 0 ? ModSounds.RIFLE_FIRE.get() : ModSounds.PISTOL_FIRE.get(),
                SoundSource.HOSTILE, 3f, 1f);
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel sl) {
            BlackMarketHub.dealerDied(sl, kind());
            if (src.getEntity() instanceof ServerPlayer p) BlackMarket.onDealerKilled(p);
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("DealerKind", kind());
        t.putLong("StockDay", stockDay);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        entityData.set(KIND, t.getInt("DealerKind"));
        stockDay = t.getLong("StockDay");
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob m) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isTrading() ? SoundEvents.WANDERING_TRADER_TRADE : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.WANDERING_TRADER_YES;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean ok) {
        return ok ? SoundEvents.WANDERING_TRADER_YES : SoundEvents.WANDERING_TRADER_NO;
    }
}
