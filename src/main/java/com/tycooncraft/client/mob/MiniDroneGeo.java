package com.tycooncraft.client.mob;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.HashMap;
import java.util.Map;

/** 자동 생성 (scripts/gen_destroyers.py) — 직접 수정하지 말 것 */
public final class MiniDroneGeo {
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p_root = mesh.getRoot();
        PartDefinition p_body = p_root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -1.5F, -3F, 5F, 3F, 6F).texOffs(0, 9).addBox(-1.5F, -2.5F, -2F, 3F, 1F, 4F).texOffs(16, 23).addBox(-2F, -1F, -4F, 4F, 2F, 1F).texOffs(0, 27).addBox(-1F, 1.5F, -1F, 2F, 1F, 2F), PartPose.offsetAndRotation(0F, 20F, 0F, 0F, 0F, 0F));
        PartDefinition p_body__gun = p_body.addOrReplaceChild("gun", CubeListBuilder.create().texOffs(0, 19).addBox(-1F, 0F, -1F, 2F, 2F, 2F).texOffs(8, 19).addBox(-0.5F, 0.5F, -4F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, 1.5F, -2F, 0F, 0F, 0F));
        PartDefinition p_body__arm0 = p_body.addOrReplaceChild("arm0", CubeListBuilder.create().texOffs(14, 9).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 4F).texOffs(16, 19).addBox(-1F, -1F, 3.5F, 2F, 2F, 2F).texOffs(8, 27).addBox(-0.5F, 0.5F, 4F, 1F, 1F, 1F), PartPose.offsetAndRotation(2F, -0.5F, -2F, 0F, 2.3562F, 0F));
        PartDefinition p_body__arm0__rotor = p_body__arm0.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(12, 27).addBox(-3F, -0.25F, -0.5F, 6F, 1F, 1F), PartPose.offsetAndRotation(0F, -1.2F, 4.5F, 0F, 0F, 0F));
        PartDefinition p_body__arm1 = p_body.addOrReplaceChild("arm1", CubeListBuilder.create().texOffs(0, 14).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 4F).texOffs(24, 19).addBox(-1F, -1F, 3.5F, 2F, 2F, 2F).texOffs(26, 27).addBox(-0.5F, 0.5F, 4F, 1F, 1F, 1F), PartPose.offsetAndRotation(-2F, -0.5F, -2F, 0F, -2.3562F, 0F));
        PartDefinition p_body__arm1__rotor = p_body__arm1.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(0, 30).addBox(-3F, -0.25F, -0.5F, 6F, 1F, 1F), PartPose.offsetAndRotation(0F, -1.2F, 4.5F, 0F, 0F, 0F));
        PartDefinition p_body__arm2 = p_body.addOrReplaceChild("arm2", CubeListBuilder.create().texOffs(10, 14).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 4F).texOffs(0, 23).addBox(-1F, -1F, 3.5F, 2F, 2F, 2F).texOffs(14, 30).addBox(-0.5F, 0.5F, 4F, 1F, 1F, 1F), PartPose.offsetAndRotation(2F, -0.5F, 2F, 0F, 0.7854F, 0F));
        PartDefinition p_body__arm2__rotor = p_body__arm2.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(18, 30).addBox(-3F, -0.25F, -0.5F, 6F, 1F, 1F), PartPose.offsetAndRotation(0F, -1.2F, 4.5F, 0F, 0F, 0F));
        PartDefinition p_body__arm3 = p_body.addOrReplaceChild("arm3", CubeListBuilder.create().texOffs(20, 14).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 4F).texOffs(8, 23).addBox(-1F, -1F, 3.5F, 2F, 2F, 2F).texOffs(0, 32).addBox(-0.5F, 0.5F, 4F, 1F, 1F, 1F), PartPose.offsetAndRotation(-2F, -0.5F, 2F, 0F, -0.7854F, 0F));
        PartDefinition p_body__arm3__rotor = p_body__arm3.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(4, 32).addBox(-3F, -0.25F, -0.5F, 6F, 1F, 1F), PartPose.offsetAndRotation(0F, -1.2F, 4.5F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 32, 64);
    }

    public static Map<String, ModelPart> bones(ModelPart root) {
        Map<String, ModelPart> m = new HashMap<>();
        m.put("body", root.getChild("body"));
        m.put("gun", root.getChild("body").getChild("gun"));
        m.put("arm0", root.getChild("body").getChild("arm0"));
        m.put("rotor", root.getChild("body").getChild("arm3").getChild("rotor"));
        m.put("arm1", root.getChild("body").getChild("arm1"));
        m.put("arm2", root.getChild("body").getChild("arm2"));
        m.put("arm3", root.getChild("body").getChild("arm3"));
        return m;
    }

    private MiniDroneGeo() {}
}
