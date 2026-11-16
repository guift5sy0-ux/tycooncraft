package com.tycooncraft.client.mob;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.HashMap;
import java.util.Map;

/** 자동 생성 (scripts/gen_mobs.py) — 직접 수정하지 말 것 */
public final class BehemothGeo {
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p_root = mesh.getRoot();
        PartDefinition p_body = p_root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-11F, -12F, -16F, 22F, 16F, 32F).texOffs(52, 75).addBox(-12F, -16F, -16F, 24F, 8F, 14F).texOffs(0, 48).addBox(-8F, 4F, -12F, 16F, 3F, 24F).texOffs(26, 112).addBox(-1.5F, -23F, -13F, 3F, 7F, 5F).texOffs(210, 75).addBox(-1.5F, -25F, -6F, 3F, 9F, 5F).texOffs(160, 98).addBox(-1.5F, -24F, 1F, 3F, 8F, 5F).texOffs(70, 112).addBox(-1.5F, -22F, 8F, 3F, 6F, 5F).texOffs(108, 0).addBox(-12F, -10F, -10F, 3F, 9F, 20F).texOffs(154, 0).addBox(9F, -10F, -10F, 3F, 9F, 20F), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 0F, 0F));
        PartDefinition p_head = p_body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 75).addBox(-7F, -7F, -12F, 14F, 11F, 12F).texOffs(176, 98).addBox(-5F, -3F, -19F, 10F, 6F, 7F).texOffs(122, 112).addBox(-5F, 3F, -18F, 10F, 1F, 6F), PartPose.offsetAndRotation(0F, -5F, -16F, 0F, 0F, 0F));
        PartDefinition p_jaw = p_head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(166, 75).addBox(-5F, 0F, -12F, 10F, 3F, 12F), PartPose.offsetAndRotation(0F, 4F, -6F, 0F, 0F, 0F));
        PartDefinition p_horn_l = p_head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(210, 98).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 10F), PartPose.offsetAndRotation(6F, -5F, -6F, 0.5F, 0.35F, 0F));
        PartDefinition p_horn_l_tip = p_horn_l.addOrReplaceChild("horn_l_tip", CubeListBuilder.create().texOffs(86, 112).addBox(-1F, -1F, 0F, 2F, 2F, 7F), PartPose.offsetAndRotation(0F, 0F, 10F, -0.9F, 0F, 0F));
        PartDefinition p_horn_r = p_head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(0, 112).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 10F), PartPose.offsetAndRotation(-6F, -5F, -6F, 0.5F, -0.35F, 0F));
        PartDefinition p_horn_r_tip = p_horn_r.addOrReplaceChild("horn_r_tip", CubeListBuilder.create().texOffs(104, 112).addBox(-1F, -1F, 0F, 2F, 2F, 7F), PartPose.offsetAndRotation(0F, 0F, 10F, -0.9F, 0F, 0F));
        PartDefinition p_tail = p_body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(128, 75).addBox(-3.5F, -3F, 0F, 7F, 6F, 12F), PartPose.offsetAndRotation(0F, -6F, 16F, -0.3F, 0F, 0F));
        PartDefinition p_tail2 = p_tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(226, 75).addBox(-2.5F, -2F, 0F, 5F, 4F, 10F).texOffs(42, 112).addBox(-4F, -3F, 8F, 8F, 6F, 6F), PartPose.offsetAndRotation(0F, 0F, 12F, 0.25F, 0F, 0F));
        PartDefinition p_leg_fl = p_root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(80, 48).addBox(-4F, -2F, -4F, 8F, 16F, 8F).texOffs(0, 98).addBox(-5F, 14F, -5F, 10F, 4F, 10F).texOffs(154, 112).addBox(-5F, 16F, -6F, 10F, 2F, 2F), PartPose.offsetAndRotation(7F, 6F, -11F, 0F, 0F, 0F));
        PartDefinition p_leg_fr = p_root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(112, 48).addBox(-4F, -2F, -4F, 8F, 16F, 8F).texOffs(40, 98).addBox(-5F, 14F, -5F, 10F, 4F, 10F).texOffs(178, 112).addBox(-5F, 16F, -6F, 10F, 2F, 2F), PartPose.offsetAndRotation(-7F, 6F, -11F, 0F, 0F, 0F));
        PartDefinition p_leg_bl = p_root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(144, 48).addBox(-4F, -2F, -4F, 8F, 16F, 8F).texOffs(80, 98).addBox(-5F, 14F, -5F, 10F, 4F, 10F).texOffs(202, 112).addBox(-5F, 16F, -6F, 10F, 2F, 2F), PartPose.offsetAndRotation(7F, 6F, 11F, 0F, 0F, 0F));
        PartDefinition p_leg_br = p_root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(176, 48).addBox(-4F, -2F, -4F, 8F, 16F, 8F).texOffs(120, 98).addBox(-5F, 14F, -5F, 10F, 4F, 10F).texOffs(226, 112).addBox(-5F, 16F, -6F, 10F, 2F, 2F), PartPose.offsetAndRotation(-7F, 6F, 11F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 256, 128);
    }

    public static Map<String, ModelPart> bones(ModelPart root) {
        Map<String, ModelPart> m = new HashMap<>();
        m.put("body", root.getChild("body"));
        m.put("head", root.getChild("body").getChild("head"));
        m.put("jaw", root.getChild("body").getChild("head").getChild("jaw"));
        m.put("horn_l", root.getChild("body").getChild("head").getChild("horn_l"));
        m.put("horn_l_tip", root.getChild("body").getChild("head").getChild("horn_l").getChild("horn_l_tip"));
        m.put("horn_r", root.getChild("body").getChild("head").getChild("horn_r"));
        m.put("horn_r_tip", root.getChild("body").getChild("head").getChild("horn_r").getChild("horn_r_tip"));
        m.put("tail", root.getChild("body").getChild("tail"));
        m.put("tail2", root.getChild("body").getChild("tail").getChild("tail2"));
        m.put("leg_fl", root.getChild("leg_fl"));
        m.put("leg_fr", root.getChild("leg_fr"));
        m.put("leg_bl", root.getChild("leg_bl"));
        m.put("leg_br", root.getChild("leg_br"));
        return m;
    }

    private BehemothGeo() {}
}
