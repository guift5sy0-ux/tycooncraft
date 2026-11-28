package com.tycooncraft.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** v0.36: 헬파이어 바이크 탑승 자세 (손잡이·발판을 짚음) */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void tycooncraft$bikePose(LivingEntity e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch, CallbackInfo ci) {
        com.tycooncraft.client.vehicle.HellBikePose.apply((HumanoidModel<?>) (Object) this, e);
    }
}
