package com.tycooncraft.client.mob;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.HashMap;
import java.util.Map;

/** 자동 생성 (scripts/gen_mobs.py) — 직접 수정하지 말 것 */
public final class AbyssalWitherGeo {
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p_root = mesh.getRoot();
        PartDefinition p_shoulders = p_root.addOrReplaceChild("shoulders", CubeListBuilder.create().texOffs(0, 35).addBox(-10F, 3.9F, -0.5F, 20F, 3F, 3F).texOffs(40, 16).addBox(-11F, 2.9F, -1.5F, 4F, 5F, 5F).texOffs(106, 35).addBox(-10.5F, -0.5F, 0F, 1F, 3F, 1F).texOffs(110, 35).addBox(-8.5F, -0.5F, 0F, 1F, 3F, 1F).texOffs(58, 16).addBox(7F, 2.9F, -1.5F, 4F, 5F, 5F).texOffs(114, 35).addBox(7.5F, -0.5F, 0F, 1F, 3F, 1F).texOffs(118, 35).addBox(9.5F, -0.5F, 0F, 1F, 3F, 1F).texOffs(0, 45).addBox(-5F, 1.4F, 0.5F, 1F, 2F, 1F).texOffs(4, 45).addBox(-1F, 1.4F, 0.5F, 1F, 2F, 1F).texOffs(8, 45).addBox(3F, 1.4F, 0.5F, 1F, 2F, 1F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_ribcage = p_root.addOrReplaceChild("ribcage", CubeListBuilder.create().texOffs(32, 0).addBox(0F, 0F, 0F, 3F, 10F, 3F).texOffs(0, 41).addBox(-4F, 1.5F, 0.5F, 11F, 2F, 2F).texOffs(26, 41).addBox(-4F, 4F, 0.5F, 11F, 2F, 2F).texOffs(52, 41).addBox(-4F, 6.5F, 0.5F, 11F, 2F, 2F).texOffs(46, 35).addBox(0.5F, 3.5F, -1.8F, 2F, 3F, 2F).texOffs(78, 41).addBox(0.5F, 0.5F, 3F, 2F, 2F, 2F).texOffs(12, 45).addBox(1F, 0F, 5F, 1F, 1F, 2F).texOffs(86, 41).addBox(0.5F, 3.5F, 3F, 2F, 2F, 2F).texOffs(18, 45).addBox(1F, 3F, 5F, 1F, 1F, 2F).texOffs(94, 41).addBox(0.5F, 6.5F, 3F, 2F, 2F, 2F).texOffs(24, 45).addBox(1F, 6F, 5F, 1F, 1F, 2F), PartPose.offsetAndRotation(-2F, 6.9F, -0.5F, 0.2042F, 0F, 0F));
        PartDefinition p_tail = p_root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(76, 16).addBox(0F, 0F, 0F, 3F, 6F, 3F).texOffs(30, 45).addBox(0.5F, 1F, 3F, 2F, 1F, 2F).texOffs(38, 45).addBox(1F, 4F, 3F, 1F, 1F, 2F).texOffs(102, 41).addBox(0.5F, 5.5F, 0.5F, 2F, 2F, 2F), PartPose.offsetAndRotation(-2F, 16.6922F, 1.5279F, 0.8325F, 0F, 0F));
        PartDefinition p_center_head = p_root.addOrReplaceChild("center_head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -4F, -4F, 8F, 8F, 8F).texOffs(0, 16).addBox(-5F, -5F, -5F, 10F, 1F, 10F).texOffs(44, 45).addBox(-1F, -6.5F, -5.6F, 2F, 2F, 1F).texOffs(56, 27).addBox(-0.5F, -12F, -4.8F, 1F, 6F, 1F).texOffs(54, 35).addBox(-3.5F, -9F, -4.8F, 1F, 4F, 1F).texOffs(58, 35).addBox(2.5F, -9F, -4.8F, 1F, 4F, 1F).texOffs(62, 35).addBox(-0.5F, -8F, 0F, 1F, 4F, 1F).texOffs(66, 35).addBox(-0.5F, -8F, 2.5F, 1F, 4F, 1F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_whorn_l = p_center_head.addOrReplaceChild("whorn_l", CubeListBuilder.create().texOffs(88, 16).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 6F), PartPose.offsetAndRotation(4.5F, -4F, 0F, -1.2708F, 0F, 0.9F));
        PartDefinition p_whorn_l_1 = p_whorn_l.addOrReplaceChild("whorn_l_1", CubeListBuilder.create().texOffs(60, 27).addBox(-1.2F, -1.2F, 0F, 2F, 2F, 5F), PartPose.offsetAndRotation(0F, 0F, 5.5F, 0F, 0F, -0.45F));
        PartDefinition p_whorn_l_2 = p_whorn_l_1.addOrReplaceChild("whorn_l_2", CubeListBuilder.create().texOffs(70, 35).addBox(-0.8F, -0.8F, 0F, 1F, 1F, 4F), PartPose.offsetAndRotation(0F, 0F, 4.5F, 0.15F, 0F, -0.45F));
        PartDefinition p_whorn_l_3 = p_whorn_l_2.addOrReplaceChild("whorn_l_3", CubeListBuilder.create().texOffs(110, 41).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, 0F, 3.5F, 0.2F, 0F, -0.35F));
        PartDefinition p_whorn_r = p_center_head.addOrReplaceChild("whorn_r", CubeListBuilder.create().texOffs(106, 16).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 6F), PartPose.offsetAndRotation(-4.5F, -4F, 0F, -1.2708F, 0F, -0.9F));
        PartDefinition p_whorn_r_1 = p_whorn_r.addOrReplaceChild("whorn_r_1", CubeListBuilder.create().texOffs(74, 27).addBox(-1.2F, -1.2F, 0F, 2F, 2F, 5F), PartPose.offsetAndRotation(0F, 0F, 5.5F, 0F, 0F, 0.45F));
        PartDefinition p_whorn_r_2 = p_whorn_r_1.addOrReplaceChild("whorn_r_2", CubeListBuilder.create().texOffs(80, 35).addBox(-0.8F, -0.8F, 0F, 1F, 1F, 4F), PartPose.offsetAndRotation(0F, 0F, 4.5F, 0.15F, 0F, 0.45F));
        PartDefinition p_whorn_r_3 = p_whorn_r_2.addOrReplaceChild("whorn_r_3", CubeListBuilder.create().texOffs(118, 41).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, 0F, 3.5F, 0.2F, 0F, 0.35F));
        PartDefinition p_right_head = p_root.addOrReplaceChild("right_head", CubeListBuilder.create().texOffs(44, 0).addBox(-4F, -4F, -4F, 6F, 6F, 6F).texOffs(0, 27).addBox(-4.5F, -4.8F, -4.5F, 7F, 1F, 7F).texOffs(50, 45).addBox(-1.5F, -5.6F, -4.8F, 1F, 1F, 1F), PartPose.offsetAndRotation(-8F, 4F, 0F, 0F, 0F, 0F));
        PartDefinition p_right_h-4 = p_right_head.addOrReplaceChild("right_h-4", CubeListBuilder.create().texOffs(90, 35).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F), PartPose.offsetAndRotation(-3.5F, -4.8F, -1F, 0F, 0F, 0.4F));
        PartDefinition p_right_h1 = p_right_head.addOrReplaceChild("right_h1", CubeListBuilder.create().texOffs(94, 35).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F), PartPose.offsetAndRotation(1.5F, -4.8F, -1F, 0F, 0F, -0.4F));
        PartDefinition p_left_head = p_root.addOrReplaceChild("left_head", CubeListBuilder.create().texOffs(68, 0).addBox(-4F, -4F, -4F, 6F, 6F, 6F).texOffs(28, 27).addBox(-4.5F, -4.8F, -4.5F, 7F, 1F, 7F).texOffs(54, 45).addBox(-1.5F, -5.6F, -4.8F, 1F, 1F, 1F), PartPose.offsetAndRotation(10F, 4F, 0F, 0F, 0F, 0F));
        PartDefinition p_left_h-4 = p_left_head.addOrReplaceChild("left_h-4", CubeListBuilder.create().texOffs(98, 35).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F), PartPose.offsetAndRotation(-3.5F, -4.8F, -1F, 0F, 0F, 0.4F));
        PartDefinition p_left_h1 = p_left_head.addOrReplaceChild("left_h1", CubeListBuilder.create().texOffs(102, 35).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F), PartPose.offsetAndRotation(1.5F, -4.8F, -1F, 0F, 0F, -0.4F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static Map<String, ModelPart> bones(ModelPart root) {
        Map<String, ModelPart> m = new HashMap<>();
        m.put("shoulders", root.getChild("shoulders"));
        m.put("ribcage", root.getChild("ribcage"));
        m.put("tail", root.getChild("tail"));
        m.put("center_head", root.getChild("center_head"));
        m.put("whorn_l", root.getChild("center_head").getChild("whorn_l"));
        m.put("whorn_l_1", root.getChild("center_head").getChild("whorn_l").getChild("whorn_l_1"));
        m.put("whorn_l_2", root.getChild("center_head").getChild("whorn_l").getChild("whorn_l_1").getChild("whorn_l_2"));
        m.put("whorn_l_3", root.getChild("center_head").getChild("whorn_l").getChild("whorn_l_1").getChild("whorn_l_2").getChild("whorn_l_3"));
        m.put("whorn_r", root.getChild("center_head").getChild("whorn_r"));
        m.put("whorn_r_1", root.getChild("center_head").getChild("whorn_r").getChild("whorn_r_1"));
        m.put("whorn_r_2", root.getChild("center_head").getChild("whorn_r").getChild("whorn_r_1").getChild("whorn_r_2"));
        m.put("whorn_r_3", root.getChild("center_head").getChild("whorn_r").getChild("whorn_r_1").getChild("whorn_r_2").getChild("whorn_r_3"));
        m.put("right_head", root.getChild("right_head"));
        m.put("right_h-4", root.getChild("right_head").getChild("right_h-4"));
        m.put("right_h1", root.getChild("right_head").getChild("right_h1"));
        m.put("left_head", root.getChild("left_head"));
        m.put("left_h-4", root.getChild("left_head").getChild("left_h-4"));
        m.put("left_h1", root.getChild("left_head").getChild("left_h1"));
        return m;
    }

    private AbyssalWitherGeo() {}
}
