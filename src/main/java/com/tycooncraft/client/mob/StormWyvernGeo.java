package com.tycooncraft.client.mob;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.HashMap;
import java.util.Map;

/** 자동 생성 (scripts/gen_mobs.py) — 직접 수정하지 말 것 */
public final class StormWyvernGeo {
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p_root = mesh.getRoot();
        PartDefinition p_body = p_root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -6F, -12F, 12F, 11F, 26F).texOffs(76, 0).addBox(-5F, 5F, -10F, 10F, 2F, 22F).texOffs(50, 71).addBox(-0.5F, -8F, -8F, 1F, 3F, 3F).texOffs(58, 71).addBox(-0.5F, -8F, -2F, 1F, 3F, 3F).texOffs(66, 71).addBox(-0.5F, -8F, 4F, 1F, 3F, 3F).texOffs(74, 71).addBox(-0.5F, -8F, 10F, 1F, 3F, 3F), PartPose.offsetAndRotation(0F, 10F, 0F, 0F, 0F, 0F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(76, 37).addBox(-3.5F, -3.5F, -11F, 7F, 7F, 11F), PartPose.offsetAndRotation(0F, -2F, -10F, -0.35F, 0F, 0F));
        PartDefinition p_head = p_neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(112, 37).addBox(-5F, -5F, -9F, 10F, 8F, 9F).texOffs(96, 56).addBox(-3.5F, -2F, -17F, 7F, 5F, 8F).texOffs(0, 71).addBox(-3F, -4F, -14F, 6F, 2F, 5F), PartPose.offsetAndRotation(0F, 0F, -11F, 0.35F, 0F, 0F));
        PartDefinition p_jaw = p_head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(126, 56).addBox(-3.5F, 0F, -9F, 7F, 2F, 9F), PartPose.offsetAndRotation(0F, 3F, -8F, 0F, 0F, 0F));
        PartDefinition p_crest_l = p_head.addOrReplaceChild("crest_l", CubeListBuilder.create().texOffs(158, 56).addBox(-0.5F, -1F, 0F, 1F, 2F, 9F), PartPose.offsetAndRotation(3F, -4F, -2F, 0.6F, 0.4F, 0F));
        PartDefinition p_crest_r = p_head.addOrReplaceChild("crest_r", CubeListBuilder.create().texOffs(178, 56).addBox(-0.5F, -1F, 0F, 1F, 2F, 9F), PartPose.offsetAndRotation(-3F, -4F, -2F, 0.6F, -0.4F, 0F));
        PartDefinition p_wing_l = p_body.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(82, 71).addBox(0F, -1.5F, -1.5F, 20F, 3F, 3F).texOffs(140, 0).addBox(0F, 0F, 1F, 20F, 1F, 18F), PartPose.offsetAndRotation(6F, -5F, -8F, 0F, 0F, 0F));
        PartDefinition p_wing_l_tip = p_wing_l.addOrReplaceChild("wing_l_tip", CubeListBuilder.create().texOffs(0, 78).addBox(0F, -1F, -1F, 20F, 2F, 2F).texOffs(182, 37).addBox(0F, 0F, 1F, 20F, 1F, 14F).texOffs(174, 71).addBox(19F, -1F, -4F, 1F, 1F, 4F), PartPose.offsetAndRotation(20F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_wing_r = p_body.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(128, 71).addBox(-20F, -1.5F, -1.5F, 20F, 3F, 3F).texOffs(0, 37).addBox(-20F, 0F, 1F, 20F, 1F, 18F), PartPose.offsetAndRotation(-6F, -5F, -8F, 0F, 0F, 0F));
        PartDefinition p_wing_r_tip = p_wing_r.addOrReplaceChild("wing_r_tip", CubeListBuilder.create().texOffs(44, 78).addBox(-20F, -1F, -1F, 20F, 2F, 2F).texOffs(0, 56).addBox(-20F, 0F, 1F, 20F, 1F, 14F).texOffs(184, 71).addBox(-20F, -1F, -4F, 1F, 1F, 4F), PartPose.offsetAndRotation(-20F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_leg_l = p_body.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(220, 56).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F).texOffs(194, 71).addBox(-2F, 6F, -3F, 4F, 1F, 4F), PartPose.offsetAndRotation(3.5F, 4F, 6F, 0.6F, 0F, 0F));
        PartDefinition p_leg_r = p_body.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(232, 56).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F).texOffs(210, 71).addBox(-2F, 6F, -3F, 4F, 1F, 4F), PartPose.offsetAndRotation(-3.5F, 4F, 6F, 0.6F, 0F, 0F));
        PartDefinition p_tail = p_body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(150, 37).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 11F), PartPose.offsetAndRotation(0F, -1F, 12F, 0F, 0F, 0F));
        PartDefinition p_tail2 = p_tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(68, 56).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 11F), PartPose.offsetAndRotation(0F, 0F, 11F, 0F, 0F, 0F));
        PartDefinition p_tail3 = p_tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(198, 56).addBox(-1F, -1F, 0F, 2F, 2F, 9F).texOffs(22, 71).addBox(-4F, -0.5F, 5F, 8F, 1F, 6F), PartPose.offsetAndRotation(0F, 0F, 11F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 256, 128);
    }

    public static Map<String, ModelPart> bones(ModelPart root) {
        Map<String, ModelPart> m = new HashMap<>();
        m.put("body", root.getChild("body"));
        m.put("neck", root.getChild("body").getChild("neck"));
        m.put("head", root.getChild("body").getChild("neck").getChild("head"));
        m.put("jaw", root.getChild("body").getChild("neck").getChild("head").getChild("jaw"));
        m.put("crest_l", root.getChild("body").getChild("neck").getChild("head").getChild("crest_l"));
        m.put("crest_r", root.getChild("body").getChild("neck").getChild("head").getChild("crest_r"));
        m.put("wing_l", root.getChild("body").getChild("wing_l"));
        m.put("wing_l_tip", root.getChild("body").getChild("wing_l").getChild("wing_l_tip"));
        m.put("wing_r", root.getChild("body").getChild("wing_r"));
        m.put("wing_r_tip", root.getChild("body").getChild("wing_r").getChild("wing_r_tip"));
        m.put("leg_l", root.getChild("body").getChild("leg_l"));
        m.put("leg_r", root.getChild("body").getChild("leg_r"));
        m.put("tail", root.getChild("body").getChild("tail"));
        m.put("tail2", root.getChild("body").getChild("tail").getChild("tail2"));
        m.put("tail3", root.getChild("body").getChild("tail").getChild("tail2").getChild("tail3"));
        return m;
    }

    private StormWyvernGeo() {}
}
