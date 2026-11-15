package com.tycooncraft.mob;

import com.tycooncraft.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** 거대 크리처 "변이 거미 여왕": 3배 크기, 벽 타기, 강한 물기·도약. 드물게 밤에 출몰 */
public class MutantSpider extends Spider {

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
