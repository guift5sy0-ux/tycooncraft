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
            if (c.state().getBlock() instanceof com.tycooncraft.ship.ShipHelmBlock) {
                renderHelm(ship, c, pose, buffers, light, pt);
                pose.popPose();
                continue;
            }
            br.renderSingleBlock(c.state(), pose, buffers, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            boolean ciws = c.state().getBlock() instanceof com.tycooncraft.turret.CiwsBlock;
            ShipEntity.Turret t = c.state().getBlock() instanceof com.tycooncraft.turret.TurretBlock || ciws ? ship.turretAt(c) : null;
            if (t != null) {   // 실린 포탑: 월드 기준 방향 → 운송기 회전만큼 되돌려 그림
                var vc = com.tycooncraft.client.vehicle.VehicleModels.cutout(buffers);
                float ty = Mth.rotLerp(pt, t.yawO * Mth.RAD_TO_DEG, t.yaw * Mth.RAD_TO_DEG) + (yaw - ship.yaw0());
                float tp = Mth.lerp(pt, t.pitchO, t.pitch) * Mth.RAD_TO_DEG;
                pose.pushPose();
                pose.translate(0.5, 14 / 32.0, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(ty));
                com.tycooncraft.client.vehicle.VehicleModels.part(pose, vc, ciws ? com.tycooncraft.client.vehicle.VehicleModels.CIWS_HEAD : com.tycooncraft.client.vehicle.VehicleModels.TURRET_HEAD, 1f, light);
                pose.translate(0, 6 / 32.0, 0);
                pose.mulPose(Axis.XP.rotationDegrees(tp));
                com.tycooncraft.client.vehicle.VehicleModels.part(pose, vc, ciws ? com.tycooncraft.client.vehicle.VehicleModels.CIWS_GUN : com.tycooncraft.client.vehicle.VehicleModels.TURRET_GUN, 1f, light);
                pose.popPose();
            }
            pose.popPose();
        }
        pose.popPose();
        super.render(ship, entityYaw, pt, pose, buffers, packedLight);
    }

    /** 타륜 허브 위치(px, 남쪽 바라보는 기본 방향) */
    private static final float HUB_X = 8, HUB_Y = 15f, HUB_Z = 13.2f;

    /** 조종석: 의자·기둥 + 조향에 따라 도는 타륜, 1인칭 조종사는 두 손으로 타륜을 잡은 팔까지 */
    private void renderHelm(ShipEntity ship, ShipEntity.Cell c, PoseStack pose, MultiBufferSource buffers, int light, float pt) {
        Minecraft mc = Minecraft.getInstance();
        var mr = mc.getBlockRenderer().getModelRenderer();
        var vc = buffers.getBuffer(net.minecraft.client.renderer.Sheets.cutoutBlockSheet());
        net.minecraft.core.Direction f = c.state().hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
                ? c.state().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) : net.minecraft.core.Direction.SOUTH;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));    // 블록스테이트 y 회전과 같게
        pose.translate(-0.5, 0, -0.5);
        mr.renderModel(pose.last(), vc, null, mc.getModelManager().getModel(com.tycooncraft.client.vehicle.VehicleModels.HELM_BODY), 1, 1, 1, light, OverlayTexture.NO_OVERLAY);
        float wa = Mth.lerp(pt, ship.wheelAngleO, ship.wheelAngle);
        pose.pushPose();
        pose.translate(HUB_X / 16f, HUB_Y / 16f, HUB_Z / 16f);
        pose.mulPose(Axis.ZP.rotationDegrees(wa));
        pose.translate(-0.5, -0.5, -0.5);
        mr.renderModel(pose.last(), vc, null, mc.getModelManager().getModel(com.tycooncraft.client.vehicle.VehicleModels.HELM_WHEEL), 1, 1, 1, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        // 1인칭 조종사: 양손이 타륜 테(9시·3시)를 잡고 함께 돈다
        if (mc.player != null && ship.getControllingPassengerForRender() == mc.player && mc.options.getCameraType().isFirstPerson()) {
            var renderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr) {
                var model = pr.getModel();
                var skin = mc.player.getSkin().texture();
                var avc = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skin));
                for (int side = -1; side <= 1; side += 2) {      // -1: 오른손(서쪽 -x), +1: 왼손
                    double a = Math.toRadians((side < 0 ? 180 : 0) + wa);
                    float gx = HUB_X + (float) (Math.cos(a) * 3.8), gy = HUB_Y + (float) (Math.sin(a) * 3.8), gz = HUB_Z - 0.6f;
                    float sx = 8 + side * 6.0f, sy = 12.0f, sz = 2.5f;   // 어깨: 눈보다 아래·약간 뒤 → 화면 아래쪽에서 올라옴
                    org.joml.Vector3f dir = new org.joml.Vector3f(gx - sx, gy - sy, gz - sz);
                    var arm = side < 0 ? model.rightArm : model.leftArm;
                    var sleeve = side < 0 ? model.rightSleeve : model.leftSleeve;
                    var saved = arm.storePose();
                    var savedS = sleeve.storePose();
                    arm.setPos(0, 0, 0);
                    arm.setRotation(0, 0, 0);
                    sleeve.setPos(0, 0, 0);
                    sleeve.setRotation(0, 0, 0);
                    pose.pushPose();
                    pose.translate(sx / 16f, sy / 16f, sz / 16f);
                    pose.mulPose(new org.joml.Quaternionf().rotationTo(0, 1, 0, dir.x, dir.y, dir.z));
                    // 팔 상자는 어깨(위 2px)에서 손(아래 10px)으로: 손끝이 테에 닿게 길이를 맞춤
                    float len = dir.length();
                    pose.translate(0, (len - 10f) / 16f, 0);   // ModelPart 는 이미 1/16 단위로 그림
                    pose.scale(0.6f, 1f, 0.6f);
                    arm.render(pose, avc, light, OverlayTexture.NO_OVERLAY);
                    sleeve.render(pose, avc, light, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                    arm.loadPose(saved);
                    sleeve.loadPose(savedS);
                }
            }
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ShipEntity ship) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
