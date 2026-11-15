package com.tycooncraft.gear;

import com.tycooncraft.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 낙하산 (다리 칸에 착용): 4칸 넘게 빠르게 떨어지면 자동으로 펼쳐져 천천히 내려온다.
 * 펼쳐진 동안 W 로 앞으로 활공, 낙하 피해 없음. 땅·물에 닿으면 접힘.
 */
public final class Parachute {
    public static final String KEY = "tycooncraft_chute";

    public static boolean wearing(Player p) {
        return p.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PARACHUTE.get());
    }

    public static boolean open(Player p) {
        return p.getPersistentData().getBoolean(KEY);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        boolean open = open(p);
        if (!wearing(p) || p.onGround() || p.isInWater() || p.getAbilities().flying || p.isPassenger() || p.isFallFlying() || p.isSpectator()) {
            if (open) p.getPersistentData().putBoolean(KEY, false);
            return;
        }
        Vec3 v = p.getDeltaMovement();
        if (!open) {
            boolean falling = p.level().isClientSide ? v.y < -0.55 : p.fallDistance > 4.5f;
            if (p.fallDistance > 4.0f && falling) {
                open = true;
                p.getPersistentData().putBoolean(KEY, true);
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 1.2f, 0.7f);
            }
        }
        if (!open) return;
        p.fallDistance = 0;
        if (p.level().isClientSide) {
            Vec3 look = Vec3.directionFromRotation(0, p.getYRot());
            double glide = p.zza > 0 ? 0.035 : 0.012;
            double vx = v.x * 0.94 + look.x * glide, vz = v.z * 0.94 + look.z * glide;
            p.setDeltaMovement(vx, Math.max(v.y, -0.16), vz);
        }
    }

    private Parachute() {}
}
