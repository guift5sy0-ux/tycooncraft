package com.tycooncraft.client.mob;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.HashMap;
import java.util.Map;

/** 자동 생성 (scripts/gen_destroyers.py) — 직접 수정하지 말 것 */
public final class ScoutDestroyerGeo {
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p_root = mesh.getRoot();
        PartDefinition p_body = p_root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -3F, -6F, 8F, 5F, 7F).texOffs(12, 37).addBox(-3.5F, -4F, -5F, 7F, 1F, 5F).texOffs(0, 30).addBox(-3F, 2F, -5F, 6F, 1F, 6F).texOffs(24, 44).addBox(-0.5F, -7F, -4.5F, 1F, 3F, 2F).texOffs(30, 44).addBox(-0.5F, -6.4F, -2F, 1F, 3F, 2F).texOffs(22, 55).addBox(-0.5F, -5.8F, 0.5F, 1F, 2F, 2F).texOffs(24, 30).addBox(-5F, -2F, -4F, 1F, 3F, 4F).texOffs(34, 30).addBox(4F, -2F, -4F, 1F, 3F, 4F), PartPose.offsetAndRotation(0F, 14.2185F, 0F, 0F, 0F, 0F));
        PartDefinition p_body__abdomen = p_body.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(30, 0).addBox(-3.5F, -2.5F, 0F, 7F, 5F, 7F).texOffs(36, 37).addBox(-2.5F, -3.5F, 1F, 5F, 1F, 5F).texOffs(28, 55).addBox(-1.5F, -2F, 6.5F, 3F, 3F, 1F).texOffs(44, 30).addBox(-4F, 0F, 1F, 1F, 2F, 5F).texOffs(0, 37).addBox(3F, 0F, 1F, 1F, 2F, 5F).texOffs(36, 55).addBox(-0.5F, -5.5F, 1.5F, 1F, 2F, 2F).texOffs(42, 55).addBox(-0.5F, -5.5F, 3.5F, 1F, 2F, 2F), PartPose.offsetAndRotation(0F, -1F, 1F, -0.22F, 0F, 0F));
        PartDefinition p_body__head = p_body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36, 12).addBox(-3F, -2F, -5F, 6F, 4F, 5F).texOffs(36, 44).addBox(-2.5F, -3F, -4F, 5F, 1F, 4F).texOffs(0, 50).addBox(-2F, 1.5F, -6F, 4F, 1F, 4F).texOffs(16, 68).addBox(-2.7F, -4.5F, -1F, 1F, 2F, 1F).texOffs(24, 68).addBox(-2.7F, -5F, -1F, 1F, 1F, 1F).texOffs(20, 68).addBox(1.7F, -4.5F, -1F, 1F, 2F, 1F).texOffs(28, 68).addBox(1.7F, -5F, -1F, 1F, 1F, 1F), PartPose.offsetAndRotation(0F, -0.5F, -6F, 0F, 0F, 0F));
        PartDefinition p_body__head__mandible_l = p_body__head.addOrReplaceChild("mandible_l", CubeListBuilder.create().texOffs(48, 55).addBox(-0.5F, 0F, -3F, 1F, 1F, 3F).texOffs(32, 68).addBox(-1F, 0F, -3.5F, 1F, 1F, 1F), PartPose.offsetAndRotation(-2F, 1.5F, -5F, 0F, -0.3F, 0F));
        PartDefinition p_body__head__mandible_r = p_body__head.addOrReplaceChild("mandible_r", CubeListBuilder.create().texOffs(56, 55).addBox(-0.5F, 0F, -3F, 1F, 1F, 3F).texOffs(36, 68).addBox(0F, 0F, -3.5F, 1F, 1F, 1F), PartPose.offsetAndRotation(2F, 1.5F, -5F, 0F, 0.3F, 0F));
        PartDefinition p_body__leg_l0 = p_body.addOrReplaceChild("leg_l0", CubeListBuilder.create().texOffs(0, 22).addBox(-1F, -1F, -1F, 2F, 6F, 2F).texOffs(16, 50).addBox(-1.5F, -1.5F, -1.5F, 3F, 2F, 3F), PartPose.offsetAndRotation(-4F, 0F, -4F, -0.35F, 0F, 0.85F));
        PartDefinition p_body__leg_l0__low = p_body__leg_l0.addOrReplaceChild("low", CubeListBuilder.create().texOffs(8, 22).addBox(-0.75F, 0F, -0.75F, 1F, 7F, 1F).texOffs(0, 60).addBox(-1F, -0.5F, -1F, 2F, 2F, 2F).texOffs(8, 60).addBox(-1F, 1F, -1.25F, 2F, 3F, 1F).texOffs(14, 60).addBox(-0.5F, 6.5F, -1.5F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, 5.5F, 0F, 0F, 0F, -1.55F));
        PartDefinition p_body__leg_l1 = p_body.addOrReplaceChild("leg_l1", CubeListBuilder.create().texOffs(12, 22).addBox(-1F, -1F, -1F, 2F, 6F, 2F).texOffs(28, 50).addBox(-1.5F, -1.5F, -1.5F, 3F, 2F, 3F), PartPose.offsetAndRotation(-4F, 0F, 3F, 0.35F, 0F, 0.85F));
        PartDefinition p_body__leg_l1__low = p_body__leg_l1.addOrReplaceChild("low", CubeListBuilder.create().texOffs(20, 22).addBox(-0.75F, 0F, -0.75F, 1F, 7F, 1F).texOffs(22, 60).addBox(-1F, -0.5F, -1F, 2F, 2F, 2F).texOffs(30, 60).addBox(-1F, 1F, -1.25F, 2F, 3F, 1F).texOffs(36, 60).addBox(-0.5F, 6.5F, -1.5F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, 5.5F, 0F, 0F, 0F, -1.55F));
        PartDefinition p_body__blade_l = p_body.addOrReplaceChild("blade_l", CubeListBuilder.create().texOffs(44, 60).addBox(-1F, -1F, -1F, 2F, 2F, 2F).texOffs(0, 44).addBox(-0.5F, -0.5F, -5F, 1F, 1F, 5F), PartPose.offsetAndRotation(-3.2F, -2.5F, -4.5F, -0.9F, -0.15F, 0F));
        PartDefinition p_body__blade_l__fore = p_body__blade_l.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(52, 60).addBox(-1F, -1F, -1F, 2F, 2F, 2F).texOffs(40, 50).addBox(-0.5F, -0.5F, -4.5F, 1F, 1F, 4F), PartPose.offsetAndRotation(0F, 0F, -5F, 1.45F, 0F, 0F));
        PartDefinition p_body__blade_l__fore__edge = p_body__blade_l__fore.addOrReplaceChild("edge", CubeListBuilder.create().texOffs(0, 12).addBox(-0.5F, -0.5F, -8F, 1F, 2F, 8F).texOffs(0, 64).addBox(-0.5F, 0.5F, -10F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, 0F, -4.5F, 0.55F, 0F, 0F));
        PartDefinition p_body__leg_r0 = p_body.addOrReplaceChild("leg_r0", CubeListBuilder.create().texOffs(24, 22).addBox(-1F, -1F, -1F, 2F, 6F, 2F).texOffs(50, 50).addBox(-1.5F, -1.5F, -1.5F, 3F, 2F, 3F), PartPose.offsetAndRotation(4F, 0F, -4F, -0.35F, 0F, -0.85F));
        PartDefinition p_body__leg_r0__low = p_body__leg_r0.addOrReplaceChild("low", CubeListBuilder.create().texOffs(32, 22).addBox(-0.75F, 0F, -0.75F, 1F, 7F, 1F).texOffs(8, 64).addBox(-1F, -0.5F, -1F, 2F, 2F, 2F).texOffs(16, 64).addBox(-1F, 1F, -1.25F, 2F, 3F, 1F).texOffs(22, 64).addBox(-0.5F, 6.5F, -1.5F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, 5.5F, 0F, 0F, 0F, 1.55F));
        PartDefinition p_body__leg_r1 = p_body.addOrReplaceChild("leg_r1", CubeListBuilder.create().texOffs(36, 22).addBox(-1F, -1F, -1F, 2F, 6F, 2F).texOffs(0, 55).addBox(-1.5F, -1.5F, -1.5F, 3F, 2F, 3F), PartPose.offsetAndRotation(4F, 0F, 3F, 0.35F, 0F, -0.85F));
        PartDefinition p_body__leg_r1__low = p_body__leg_r1.addOrReplaceChild("low", CubeListBuilder.create().texOffs(44, 22).addBox(-0.75F, 0F, -0.75F, 1F, 7F, 1F).texOffs(30, 64).addBox(-1F, -0.5F, -1F, 2F, 2F, 2F).texOffs(38, 64).addBox(-1F, 1F, -1.25F, 2F, 3F, 1F).texOffs(44, 64).addBox(-0.5F, 6.5F, -1.5F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, 5.5F, 0F, 0F, 0F, 1.55F));
        PartDefinition p_body__blade_r = p_body.addOrReplaceChild("blade_r", CubeListBuilder.create().texOffs(52, 64).addBox(-1F, -1F, -1F, 2F, 2F, 2F).texOffs(12, 44).addBox(-0.5F, -0.5F, -5F, 1F, 1F, 5F), PartPose.offsetAndRotation(3.2F, -2.5F, -4.5F, -0.9F, 0.15F, 0F));
        PartDefinition p_body__blade_r__fore = p_body__blade_r.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(0, 68).addBox(-1F, -1F, -1F, 2F, 2F, 2F).texOffs(12, 55).addBox(-0.5F, -0.5F, -4.5F, 1F, 1F, 4F), PartPose.offsetAndRotation(0F, 0F, -5F, 1.45F, 0F, 0F));
        PartDefinition p_body__blade_r__fore__edge = p_body__blade_r__fore.addOrReplaceChild("edge", CubeListBuilder.create().texOffs(18, 12).addBox(-0.5F, -0.5F, -8F, 1F, 2F, 8F).texOffs(8, 68).addBox(-0.5F, 0.5F, -10F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, 0F, -4.5F, 0.55F, 0F, 0F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    public static Map<String, ModelPart> bones(ModelPart root) {
        Map<String, ModelPart> m = new HashMap<>();
        m.put("body", root.getChild("body"));
        m.put("abdomen", root.getChild("body").getChild("abdomen"));
        m.put("head", root.getChild("body").getChild("head"));
        m.put("mandible_l", root.getChild("body").getChild("head").getChild("mandible_l"));
        m.put("mandible_r", root.getChild("body").getChild("head").getChild("mandible_r"));
        m.put("leg_l0", root.getChild("body").getChild("leg_l0"));
        m.put("low", root.getChild("body").getChild("leg_r1").getChild("low"));
        m.put("leg_l1", root.getChild("body").getChild("leg_l1"));
        m.put("blade_l", root.getChild("body").getChild("blade_l"));
        m.put("fore", root.getChild("body").getChild("blade_r").getChild("fore"));
        m.put("edge", root.getChild("body").getChild("blade_r").getChild("fore").getChild("edge"));
        m.put("leg_r0", root.getChild("body").getChild("leg_r0"));
        m.put("leg_r1", root.getChild("body").getChild("leg_r1"));
        m.put("blade_r", root.getChild("body").getChild("blade_r"));
        return m;
    }

    private ScoutDestroyerGeo() {}
}
