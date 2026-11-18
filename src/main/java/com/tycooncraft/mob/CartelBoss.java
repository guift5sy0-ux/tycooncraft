package com.tycooncraft.mob;

import com.tycooncraft.entity.RocketEntity;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 보스 "카르텔 두목": 소총 연사 + 6초마다 RPG, 체력 2/3·1/3 에서 부하 소환. 보스 체력바 표시.
 * 처치하면 다이아 동전, 마법이 붙은 총, 로켓.
 */
public class CartelBoss extends GunnerEntity {
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.tycooncraft.cartel_boss"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private int rocketCooldown = 80;
    private int phase;

    public CartelBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level, Kind.BOSS);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        LivingEntity target = getTarget();
        if (target != null && --rocketCooldown <= 0 && hasLineOfSight(target)) {
            rocketCooldown = 120;
            Vec3 from = getEyePosition();
            Vec3 dir = target.getBoundingBox().getCenter().subtract(from).normalize();
            level().addFreshEntity(RocketEntity.create(ModEntities.ROCKET.get(), level(), this, from.add(dir.scale(1.2)), dir.scale(1.3), RocketEntity.ROCKET, 2.6f));
            level().playSound(null, getX(), getY(), getZ(), ModSounds.RPG_FIRE.get(), SoundSource.HOSTILE, 3f, 0.9f);
        }
        float hp = getHealth() / getMaxHealth();
        if ((phase == 0 && hp < 0.66f) || (phase == 1 && hp < 0.33f)) {
            phase++;
            summonMinions();
        }
    }

    private void summonMinions() {
        if (!(level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < 3; i++) {
            GunnerEntity g = ModEntities.GANGSTER.get().create(sl);
            if (g == null) continue;
            g.moveTo(getX() + random.nextGaussian() * 3, getY(), getZ() + random.nextGaussian() * 3, random.nextFloat() * 360, 0);
            g.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            g.setTarget(getTarget());
            sl.addFreshEntity(g);
        }
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 1, getZ(), 30, 2, 0.5, 2, 0.02);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.COIN_EMERALD.get(), 1 + random.nextInt(2)));
        spawnAtLocation(new ItemStack(ModItems.ROCKET.get(), 4));
        ItemStack loot = new ItemStack(random.nextBoolean() ? ModItems.VSS.get() : ModItems.RPG.get());
        spawnAtLocation(loot);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
    }
}
