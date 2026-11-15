package com.tycooncraft.mob;

import com.tycooncraft.registry.ModItems;
import com.tycooncraft.entity.SpiderShot;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 거대 크리처 "변이 거미 여왕": 3배 크기, 벽 타기, 강한 물기·도약. 드물게 밤에 출몰.
 * 패턴 1 거미줄 뱉기: 멀리 있는 대상에게 거미줄 덩어리 (맞은 자리에 거미줄 + 강한 둔화)
 * 패턴 2 맹독 침: 잠깐 몸을 웅크려 예고한 뒤 독침 5발을 부채꼴로 발사 (독 II + 위더 + 멀미)
 */
public class MutantSpider extends Spider {
    private int webCd = 60, venomCd = 120, venomCharge;
    public static int webShots, venomShots;

    public MutantSpider(EntityType<? extends Spider> type, Level level) {
        super(type, level);
        this.xpReward = 60;
    }

    public static AttributeSupplier.Builder attributes() {
        return Spider.createAttributes()
                .add(Attributes.MAX_HEALTH, 180)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (webCd > 0) webCd--;
        if (venomCd > 0) venomCd--;
        LivingEntity t = getTarget();
        if (venomCharge > 0) {
            venomCharge--;
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30, 30);
            if (level() instanceof ServerLevel sl && venomCharge % 2 == 0)
                sl.sendParticles(new DustParticleOptions(new Vector3f(0.35f, 0.9f, 0.2f), 1.6f), getX(), getY() + getBbHeight() * 0.6, getZ(), 6, 0.9, 0.4, 0.9, 0);
            if (venomCharge == 0 && t != null && t.isAlive()) venomVolley(t);
            return;
        }
        if (t == null || !t.isAlive() || !hasLineOfSight(t)) return;
        double d = distanceTo(t);
        if (venomCd == 0 && d < 20) {
            venomCharge = 18;
            venomCd = 150 + random.nextInt(60);
            playSound(SoundEvents.SPIDER_AMBIENT, 3f, 0.3f);
        } else if (webCd == 0 && d > 3.5 && d < 26) {
            webCd = 70 + random.nextInt(50);
            shoot(t, false, 0, 1.5f);
            playSound(ModSounds.SPIDER_WEB.get(), 2.5f, 0.9f + random.nextFloat() * 0.2f);
            webShots++;
        }
    }

    private void venomVolley(LivingEntity t) {
        playSound(ModSounds.SPIDER_VENOM.get(), 3f, 1f);
        for (int i = -2; i <= 2; i++) shoot(t, true, i * 9f, 1.9f);
        venomShots++;
    }

    private void shoot(LivingEntity t, boolean venom, float yawOff, float speed) {
        SpiderShot s = new SpiderShot(ModEntities.SPIDER_SHOT.get(), this, level(), venom);
        Vec3 from = new Vec3(getX(), getY() + getBbHeight() * 0.55, getZ());
        Vec3 look = Vec3.directionFromRotation(0, yBodyRot);
        from = from.add(look.scale(getBbWidth() * 0.55));
        s.setPos(from);
        Vec3 aim = t.getEyePosition().add(t.getDeltaMovement().scale(distanceTo(t) / speed)).subtract(from);
        double horiz = aim.horizontalDistance();
        double grav = venom ? 0.01 : 0.03;
        aim = aim.add(0, horiz * horiz / (speed * speed) * grav * 0.5 * 1.1, 0);
        aim = aim.yRot(yawOff * Mth.DEG_TO_RAD);
        s.shoot(aim.x, aim.y, aim.z, speed, venom ? 1.5f : 2.5f);
        level().addFreshEntity(s);
    }

    @Override
    public float getVoicePitch() {
        return 0.45f;
    }

    @Override
    protected float getSoundVolume() {
        return 2.5f;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.COIN_GOLD.get(), 2 + random.nextInt(4)));
        spawnAtLocation(new ItemStack(Items.STRING, 8 + random.nextInt(8)));
        spawnAtLocation(new ItemStack(Items.SPIDER_EYE, 2));
    }
}
