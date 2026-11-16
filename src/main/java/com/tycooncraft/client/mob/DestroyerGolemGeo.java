package com.tycooncraft.client.mob;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.HashMap;
import java.util.Map;

/** 자동 생성 (scripts/gen_mobs.py) — 직접 수정하지 말 것 */
public final class DestroyerGolemGeo {
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p_root = mesh.getRoot();
        PartDefinition p_body = p_root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(120, 60).addBox(-8F, -6F, -5F, 16F, 8F, 10F).texOffs(0, 0).addBox(-14F, -26F, -8F, 28F, 20F, 16F).texOffs(172, 81).addBox(-11F, -24F, -9F, 22F, 12F, 1F).texOffs(126, 98).addBox(-4F, -12F, -9.5F, 8F, 3F, 1F).texOffs(146, 36).addBox(-10F, -24F, 8F, 20F, 16F, 6F).texOffs(218, 81).addBox(-8F, -22F, 14F, 16F, 12F, 1F).texOffs(48, 81).addBox(-9F, -36F, 10F, 4F, 12F, 4F).texOffs(64, 81).addBox(5F, -36F, 10F, 4F, 12F, 4F).texOffs(48, 60).addBox(-15F, -20F, 2F, 6F, 10F, 10F), PartPose.offsetAndRotation(0F, 4F, 0F, 0F, 0F, 0F));
        PartDefinition p_head = p_body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(80, 60).addBox(-5F, -8F, -6F, 10F, 9F, 10F).texOffs(160, 98).addBox(-4F, -5F, -6.5F, 8F, 2F, 1F).texOffs(80, 98).addBox(-6F, -3F, -7F, 12F, 4F, 3F).texOffs(122, 98).addBox(3F, -12F, 1F, 1F, 5F, 1F), PartPose.offsetAndRotation(0F, -26F, -3F, 0F, 0F, 0F));
        PartDefinition p_right_arm = p_body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(38, 36).addBox(-7F, -7F, -7F, 13F, 9F, 14F).texOffs(206, 0).addBox(-5F, -1F, -5F, 9F, 14F, 10F).texOffs(144, 98).addBox(-4F, 4F, -5.5F, 7F, 3F, 1F), PartPose.offsetAndRotation(-17F, -22F, 0F, 0F, 0F, 0F));
        PartDefinition p_minigun = p_right_arm.addOrReplaceChild("minigun", CubeListBuilder.create().texOffs(0, 81).addBox(-5F, 0F, -5F, 10F, 7F, 10F).texOffs(110, 98).addBox(-6F, 2F, 4F, 3F, 4F, 3F), PartPose.offsetAndRotation(-0.5F, 13F, 0F, 0F, 0F, 0F));
        PartDefinition p_barrels = p_minigun.addOrReplaceChild("barrels", CubeListBuilder.create().texOffs(172, 60).addBox(1.6F, 0F, -1F, 2F, 16F, 2F).texOffs(180, 60).addBox(0.3F, 0F, 1.3F, 2F, 16F, 2F).texOffs(188, 60).addBox(-2.3F, 0F, 1.3F, 2F, 16F, 2F).texOffs(196, 60).addBox(-3.6F, 0F, -1F, 2F, 16F, 2F).texOffs(204, 60).addBox(-2.3F, 0F, -3.3F, 2F, 16F, 2F).texOffs(212, 60).addBox(0.3F, 0F, -3.3F, 2F, 16F, 2F).texOffs(40, 81).addBox(-1F, 0F, -1F, 2F, 15F, 2F).texOffs(0, 98).addBox(-4F, 13F, -4F, 8F, 2F, 8F), PartPose.offsetAndRotation(0F, 7F, 0F, 0F, 0F, 0F));
        PartDefinition p_left_arm = p_body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(92, 36).addBox(-6F, -7F, -7F, 13F, 9F, 14F).texOffs(0, 36).addBox(-4F, -1F, -5F, 9F, 14F, 10F).texOffs(88, 0).addBox(-5F, 12F, -6F, 11F, 14F, 12F).texOffs(198, 36).addBox(-5.5F, 25F, -6.5F, 12F, 8F, 13F), PartPose.offsetAndRotation(17F, -22F, 0F, 0F, 0F, 0F));
        PartDefinition p_pod = p_left_arm.addOrReplaceChild("pod", CubeListBuilder.create().texOffs(0, 60).addBox(-5F, -9F, -7F, 11F, 8F, 13F), PartPose.offsetAndRotation(1F, -7F, 0F, 0F, 0F, 0F));
        PartDefinition p_left_leg = p_root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(134, 0).addBox(-4.5F, 0F, -4.5F, 9F, 16F, 9F).texOffs(32, 98).addBox(-5F, 4F, -5.5F, 10F, 6F, 2F).texOffs(80, 81).addBox(-5.5F, 16F, -6.5F, 11F, 4F, 12F), PartPose.offsetAndRotation(6F, 4F, 0F, 0F, 0F, 0F));
        PartDefinition p_right_leg = p_root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(170, 0).addBox(-4.5F, 0F, -4.5F, 9F, 16F, 9F).texOffs(56, 98).addBox(-5F, 4F, -5.5F, 10F, 6F, 2F).texOffs(126, 81).addBox(-5.5F, 16F, -6.5F, 11F, 4F, 12F), PartPose.offsetAndRotation(-6F, 4F, 0F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 256, 128);
    }

    public static Map<String, ModelPart> bones(ModelPart root) {
        Map<String, ModelPart> m = new HashMap<>();
        m.put("body", root.getChild("body"));
        m.put("head", root.getChild("body").getChild("head"));
        m.put("right_arm", root.getChild("body").getChild("right_arm"));
        m.put("minigun", root.getChild("body").getChild("right_arm").getChild("minigun"));
        m.put("barrels", root.getChild("body").getChild("right_arm").getChild("minigun").getChild("barrels"));
        m.put("left_arm", root.getChild("body").getChild("left_arm"));
        m.put("pod", root.getChild("body").getChild("left_arm").getChild("pod"));
        m.put("left_leg", root.getChild("left_leg"));
        m.put("right_leg", root.getChild("right_leg"));
        return m;
    }

    private DestroyerGolemGeo() {}
}
