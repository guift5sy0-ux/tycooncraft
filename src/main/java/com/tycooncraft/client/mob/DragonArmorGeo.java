package com.tycooncraft.client.mob;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.HashMap;
import java.util.Map;

/** 자동 생성 (scripts/gen_mobs.py) — 직접 수정하지 말 것 */
public final class DragonArmorGeo {
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p_root = mesh.getRoot();
        PartDefinition p_head = p_root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(156, 209).addBox(-8.5F, -9F, -10.5F, 17F, 2F, 7F).texOffs(124, 221).addBox(-1.5F, -6.5F, -10.9F, 3F, 3F, 1F).texOffs(114, 192).addBox(-0.5F, -2.5F, -23.5F, 1F, 2F, 13F).texOffs(248, 209).addBox(-1F, -13F, -6F, 2F, 5F, 2F).texOffs(240, 209).addBox(-1F, -14F, -2F, 2F, 6F, 2F).texOffs(60, 221).addBox(-1F, -13F, 2F, 2F, 5F, 2F).texOffs(60, 209).addBox(7F, -4F, -8F, 2F, 2F, 10F).texOffs(84, 209).addBox(-9F, -4F, -8F, 2F, 2F, 10F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_dhorn_l = p_head.addOrReplaceChild("dhorn_l", CubeListBuilder.create().texOffs(216, 170).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 12F), PartPose.offsetAndRotation(6F, -7F, 3F, 0.18F, 0.38F, 0F));
        PartDefinition p_dhorn_l_1 = p_dhorn_l.addOrReplaceChild("dhorn_l_1", CubeListBuilder.create().texOffs(162, 192).addBox(-2F, -2F, 0F, 4F, 4F, 10F), PartPose.offsetAndRotation(0F, 0F, 11.5F, 0.18F, 0F, 0F));
        PartDefinition p_dhorn_l_2 = p_dhorn_l_1.addOrReplaceChild("dhorn_l_2", CubeListBuilder.create().texOffs(108, 209).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 9F), PartPose.offsetAndRotation(0F, 0F, 9.5F, 0.22F, 0F, 0F));
        PartDefinition p_dhorn_l_3 = p_dhorn_l_2.addOrReplaceChild("dhorn_l_3", CubeListBuilder.create().texOffs(204, 209).addBox(-1F, -1F, 0F, 2F, 2F, 7F), PartPose.offsetAndRotation(0F, 0F, 8.5F, 0.28F, 0F, 0F));
        PartDefinition p_dhorn_l_4 = p_dhorn_l_3.addOrReplaceChild("dhorn_l_4", CubeListBuilder.create().texOffs(68, 221).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 5F), PartPose.offsetAndRotation(0F, 0F, 6.5F, 0.32F, 0F, 0F));
        PartDefinition p_dhorn_r = p_head.addOrReplaceChild("dhorn_r", CubeListBuilder.create().texOffs(60, 192).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 12F), PartPose.offsetAndRotation(-6F, -7F, 3F, 0.18F, -0.38F, 0F));
        PartDefinition p_dhorn_r_1 = p_dhorn_r.addOrReplaceChild("dhorn_r_1", CubeListBuilder.create().texOffs(190, 192).addBox(-2F, -2F, 0F, 4F, 4F, 10F), PartPose.offsetAndRotation(0F, 0F, 11.5F, 0.18F, 0F, 0F));
        PartDefinition p_dhorn_r_2 = p_dhorn_r_1.addOrReplaceChild("dhorn_r_2", CubeListBuilder.create().texOffs(132, 209).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 9F), PartPose.offsetAndRotation(0F, 0F, 9.5F, 0.22F, 0F, 0F));
        PartDefinition p_dhorn_r_3 = p_dhorn_r_2.addOrReplaceChild("dhorn_r_3", CubeListBuilder.create().texOffs(222, 209).addBox(-1F, -1F, 0F, 2F, 2F, 7F), PartPose.offsetAndRotation(0F, 0F, 8.5F, 0.28F, 0F, 0F));
        PartDefinition p_dhorn_r_4 = p_dhorn_r_3.addOrReplaceChild("dhorn_r_4", CubeListBuilder.create().texOffs(80, 221).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 5F), PartPose.offsetAndRotation(0F, 0F, 6.5F, 0.32F, 0F, 0F));
        PartDefinition p_jaw = p_root.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(116, 221).addBox(-5F, 4F, -14F, 1F, 4F, 1F).texOffs(132, 221).addBox(-5F, 4F, -9F, 1F, 3F, 1F).texOffs(120, 221).addBox(4F, 4F, -14F, 1F, 4F, 1F).texOffs(136, 221).addBox(4F, 4F, -9F, 1F, 3F, 1F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_body = p_root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(142, 192).addBox(-1.5F, -14F, -12F, 3F, 8F, 7F).texOffs(100, 170).addBox(-1.5F, -18F, -1F, 3F, 12F, 7F).texOffs(60, 170).addBox(-1.5F, -21F, 10F, 3F, 15F, 7F).texOffs(80, 170).addBox(-1.5F, -19F, 21F, 3F, 13F, 7F).texOffs(94, 192).addBox(-1.5F, -16F, 32F, 3F, 10F, 7F).texOffs(218, 192).addBox(-1.5F, -13F, 42F, 3F, 7F, 7F).texOffs(120, 170).addBox(12F, -1.5F, -15F, 8F, 3F, 16F).texOffs(92, 221).addBox(15F, -5F, -12F, 1F, 4F, 2F).texOffs(98, 221).addBox(15F, -5F, -5F, 1F, 4F, 2F).texOffs(168, 170).addBox(-20F, -1.5F, -15F, 8F, 3F, 16F).texOffs(104, 221).addBox(-16F, -5F, -12F, 1F, 4F, 2F).texOffs(110, 221).addBox(-16F, -5F, -5F, 1F, 4F, 2F).texOffs(140, 221).addBox(-1F, 3F, -16.6F, 2F, 3F, 1F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    public static Map<String, ModelPart> bones(ModelPart root) {
        Map<String, ModelPart> m = new HashMap<>();
        m.put("head", root.getChild("head"));
        m.put("dhorn_l", root.getChild("head").getChild("dhorn_l"));
        m.put("dhorn_l_1", root.getChild("head").getChild("dhorn_l").getChild("dhorn_l_1"));
        m.put("dhorn_l_2", root.getChild("head").getChild("dhorn_l").getChild("dhorn_l_1").getChild("dhorn_l_2"));
        m.put("dhorn_l_3", root.getChild("head").getChild("dhorn_l").getChild("dhorn_l_1").getChild("dhorn_l_2").getChild("dhorn_l_3"));
        m.put("dhorn_l_4", root.getChild("head").getChild("dhorn_l").getChild("dhorn_l_1").getChild("dhorn_l_2").getChild("dhorn_l_3").getChild("dhorn_l_4"));
        m.put("dhorn_r", root.getChild("head").getChild("dhorn_r"));
        m.put("dhorn_r_1", root.getChild("head").getChild("dhorn_r").getChild("dhorn_r_1"));
        m.put("dhorn_r_2", root.getChild("head").getChild("dhorn_r").getChild("dhorn_r_1").getChild("dhorn_r_2"));
        m.put("dhorn_r_3", root.getChild("head").getChild("dhorn_r").getChild("dhorn_r_1").getChild("dhorn_r_2").getChild("dhorn_r_3"));
        m.put("dhorn_r_4", root.getChild("head").getChild("dhorn_r").getChild("dhorn_r_1").getChild("dhorn_r_2").getChild("dhorn_r_3").getChild("dhorn_r_4"));
        m.put("jaw", root.getChild("jaw"));
        m.put("body", root.getChild("body"));
        return m;
    }

    private DragonArmorGeo() {}
}
