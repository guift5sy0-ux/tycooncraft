package com.tycooncraft.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tycooncraft.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/** 배를 이루는 블록들을 그대로 그린다 (방향 회전 포함) */
public class ShipRenderer extends EntityRenderer<ShipEntity> {
    public ShipRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0;
    }

    @Override
    public void render(ShipEntity ship, float entityYaw, float pt, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        BlockRenderDispatcher br = Minecraft.getInstance().getBlockRenderer();
        float yaw = Mth.rotLerp(pt, ship.yawO, ship.yaw);
        Vec3 p = ship.getPosition(pt);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-(yaw - ship.yaw0())));
        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
        for (ShipEntity.Cell c : ship.cells()) {
            if (c.state().getRenderShape() == RenderShape.INVISIBLE) continue;
            Vec3 w = ship.rot(c.x() + 0.5, c.z() + 0.5, yaw).add(p);
            bp.set(Mth.floor(w.x), Mth.floor(p.y + c.y() + 0.5), Mth.floor(w.z));
            int light = LevelRenderer.getLightColor(ship.level(), bp);
            pose.pushPose();
            pose.translate(c.x() - ship.pivotX(), c.y(), c.z() - ship.pivotZ());
            br.renderSingleBlock(c.state(), pose, buffers, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
        }
        pose.popPose();
        super.render(ship, entityYaw, pt, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShipEntity ship) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
