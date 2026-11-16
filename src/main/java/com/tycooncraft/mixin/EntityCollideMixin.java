package com.tycooncraft.mixin;

import com.tycooncraft.ship.ShipCollisions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** 배(블록으로 만든 움직이는 구조물)의 갑판·선체를 모든 엔티티의 이동 충돌에 추가한다 → 갑판 위를 걸어 다닐 수 있다 */
@Mixin(Entity.class)
public abstract class EntityCollideMixin {
    @Redirect(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<VoxelShape> tycooncraft$shipCollisions(Level level, Entity self, AABB box) {
        return ShipCollisions.append(level, self, box, level.getEntityCollisions(self, box));
    }
}
