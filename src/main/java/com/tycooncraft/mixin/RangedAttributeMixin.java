package com.tycooncraft.mixin;

import com.tycooncraft.gear.ExtendedArmor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** v0.31: 방어력 상한 30 → 40 */
@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin {
    @Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
    private void tycooncraft$armorCap(double value, CallbackInfoReturnable<Double> cir) {
        if ("attribute.name.generic.armor".equals(((Attribute) (Object) this).getDescriptionId()))
            cir.setReturnValue(Double.isNaN(value) ? 0.0 : Mth.clamp(value, 0.0, ExtendedArmor.MAX_ARMOR));
    }
}
