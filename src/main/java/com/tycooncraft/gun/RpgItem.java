package com.tycooncraft.gun;

import com.tycooncraft.entity.RocketEntity;
import com.tycooncraft.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** RPG: 로켓 1발 장전, 맞으면 폭발 (블록 파괴는 mobGriefing 규칙을 따름) */
public class RpgItem extends GunItem {

    public RpgItem(Properties properties, GunStats stats) {
        super(properties, stats);
    }

    @Override
    protected void fire(ServerLevel level, Player player, ItemStack stack, int silencerTier) {
        Vec3 look = player.getViewVector(1f);
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 pos = player.getEyePosition().add(look.scale(1.2)).add(right.scale(0.3)).add(0, -0.15, 0);
        float power = 3.5f * (1 + 0.1f * GunEnchants.level(stack, GunEnchants.FIREPOWER));
        level.addFreshEntity(RocketEntity.create(ModEntities.ROCKET.get(), level, player, pos, look.scale(1.6), RocketEntity.ROCKET, power));
        // 후폭풍
        Vec3 back = player.getEyePosition().subtract(look.scale(1.2)).add(right.scale(0.3));
        level.sendParticles(ParticleTypes.LARGE_SMOKE, back.x, back.y, back.z, 14, 0.3, 0.2, 0.3, 0.06);
        level.sendParticles(ParticleTypes.FLAME, back.x, back.y, back.z, 6, 0.15, 0.1, 0.15, 0.04);
    }
}
