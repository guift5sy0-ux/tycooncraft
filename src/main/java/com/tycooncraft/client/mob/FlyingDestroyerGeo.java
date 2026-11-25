package com.tycooncraft.client.mob;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.HashMap;
import java.util.Map;

/** 자동 생성 (scripts/gen_destroyers.py) — 직접 수정하지 말 것 */
public final class FlyingDestroyerGeo {
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p_root = mesh.getRoot();
        PartDefinition p_hull = p_root.addOrReplaceChild("hull", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -4F, -12F, 12F, 7F, 24F).texOffs(38, 31).addBox(-5F, -5F, -10F, 10F, 1F, 19F).texOffs(72, 0).addBox(-7F, -2F, -9F, 1F, 4F, 18F).texOffs(0, 31).addBox(6F, -2F, -9F, 1F, 4F, 18F).texOffs(0, 53).addBox(-7.2F, 1F, -8F, 1F, 1F, 16F).texOffs(34, 53).addBox(6.2F, 1F, -8F, 1F, 1F, 16F).texOffs(0, 123).addBox(-5F, 3F, -11F, 10F, 1F, 7F).texOffs(32, 139).addBox(-5F, 3F, 6F, 10F, 1F, 6F).texOffs(40, 112).addBox(-5F, -3F, -15F, 10F, 6F, 3F).texOffs(16, 147).addBox(-4F, -2F, -17F, 8F, 4F, 2F).texOffs(10, 165).addBox(-2.5F, -1F, -19F, 5F, 2F, 2F).texOffs(14, 170).addBox(-1.5F, -0.5F, -19.3F, 3F, 1F, 1F).texOffs(30, 154).addBox(-3F, -4F, -14F, 6F, 1F, 4F).texOffs(36, 147).addBox(-2F, 3F, -15F, 4F, 2F, 4F).texOffs(64, 139).addBox(-0.5F, 3.5F, -21F, 1F, 1F, 6F).texOffs(50, 154).addBox(-2F, 3.5F, -19F, 1F, 1F, 4F).texOffs(60, 154).addBox(1F, 3.5F, -19F, 1F, 1F, 4F).texOffs(34, 99).addBox(-3F, -9F, -3F, 6F, 4F, 8F).texOffs(78, 139).addBox(-2.5F, -10F, -2F, 5F, 1F, 6F).texOffs(56, 165).addBox(-3.2F, -8F, -3.2F, 6F, 2F, 1F).texOffs(34, 123).addBox(-3.2F, -8F, -2F, 1F, 2F, 6F).texOffs(48, 123).addBox(2.2F, -8F, -2F, 1F, 2F, 6F).texOffs(70, 154).addBox(-1F, -14F, 2F, 1F, 4F, 1F).texOffs(24, 165).addBox(1F, -13F, 3F, 1F, 3F, 1F).texOffs(22, 170).addBox(-1F, -15F, 2F, 1F, 1F, 1F).texOffs(70, 165).addBox(-2F, -11F, 4F, 4F, 1F, 2F).texOffs(0, 86).addBox(-13F, -1F, -5F, 26F, 2F, 11F).texOffs(62, 123).addBox(-11F, -2F, -3F, 22F, 1F, 7F).texOffs(62, 99).addBox(-15F, -1F, -4F, 2F, 2F, 9F).texOffs(84, 99).addBox(13F, -1F, -4F, 2F, 2F, 9F).texOffs(26, 170).addBox(-16F, -1F, -2F, 1F, 1F, 1F).texOffs(30, 170).addBox(15F, -1F, -2F, 1F, 1F, 1F).texOffs(100, 139).addBox(-10F, 0F, -10F, 3F, 3F, 4F).texOffs(74, 154).addBox(-9F, 1F, -14F, 1F, 1F, 4F).texOffs(114, 139).addBox(7F, 0F, -10F, 3F, 3F, 4F).texOffs(84, 154).addBox(8F, 1F, -14F, 1F, 1F, 4F).texOffs(106, 99).addBox(-0.5F, -10F, 7F, 1F, 6F, 5F).texOffs(0, 147).addBox(-5F, -6F, 9F, 1F, 4F, 3F).texOffs(8, 147).addBox(4F, -6F, 9F, 1F, 4F, 3F).texOffs(52, 147).addBox(-4F, -3F, 12F, 8F, 5F, 1F).texOffs(28, 165).addBox(-3F, -2F, 12.5F, 2F, 3F, 1F).texOffs(34, 165).addBox(1F, -2F, 12.5F, 2F, 3F, 1F).texOffs(40, 165).addBox(-4F, 4F, -9F, 2F, 2F, 2F).texOffs(82, 165).addBox(-4F, 6F, -9F, 2F, 1F, 2F).texOffs(48, 165).addBox(2F, 4F, -9F, 2F, 2F, 2F).texOffs(90, 165).addBox(2F, 6F, -9F, 2F, 1F, 2F).texOffs(0, 112).addBox(-5F, 2.5F, -4F, 10F, 1F, 10F), PartPose.offsetAndRotation(0F, 12F, 0F, 0F, 0F, 0F));
        PartDefinition p_hull__pod0 = p_hull.addOrReplaceChild("pod0", CubeListBuilder.create().texOffs(68, 53).addBox(-3F, -3F, -5F, 6F, 6F, 10F).texOffs(94, 154).addBox(-3.5F, -2F, -5.5F, 7F, 4F, 1F).texOffs(0, 131).addBox(-2F, -3.5F, -5.5F, 4F, 7F, 1F).texOffs(110, 154).addBox(-3.5F, -2F, 4.5F, 7F, 4F, 1F).texOffs(10, 131).addBox(-2F, -3.5F, 4.5F, 4F, 7F, 1F).texOffs(0, 160).addBox(-1F, -4F, -2F, 2F, 1F, 4F).texOffs(70, 147).addBox(-3.5F, -1F, -2F, 1F, 2F, 4F).texOffs(34, 170).addBox(-4.5F, -4F, -4.5F, 9F, 1F, 1F).texOffs(54, 170).addBox(-4.5F, -4F, 3.5F, 9F, 1F, 1F).texOffs(20, 131).addBox(-4.5F, -4F, -3.5F, 1F, 1F, 7F).texOffs(36, 131).addBox(3.5F, -4F, -3.5F, 1F, 1F, 7F), PartPose.offsetAndRotation(13.5F, 0F, 1F, 0F, 0F, 0F));
        PartDefinition p_hull__pod0__glow = p_hull__pod0.addOrReplaceChild("glow", CubeListBuilder.create().texOffs(12, 160).addBox(-2F, -2F, 5.2F, 4F, 4F, 1F).texOffs(98, 165).addBox(-1F, -1F, 5.6F, 2F, 2F, 1F).texOffs(80, 147).addBox(3F, -1F, -2F, 1F, 2F, 4F).texOffs(90, 147).addBox(-4F, -1F, -2F, 1F, 2F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_hull__pod0__rotor = p_hull__pod0.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(74, 170).addBox(-4F, -0.5F, -0.5F, 8F, 1F, 1F).texOffs(66, 112).addBox(-0.5F, -0.5F, -4F, 1F, 1F, 8F).texOffs(104, 165).addBox(-1F, -1F, -1F, 2F, 1F, 2F), PartPose.offsetAndRotation(0F, -3.8F, 0F, 0F, 0F, 0F));
        PartDefinition p_hull__pod1 = p_hull.addOrReplaceChild("pod1", CubeListBuilder.create().texOffs(0, 70).addBox(-3F, -3F, -5F, 6F, 6F, 10F).texOffs(22, 160).addBox(-3.5F, -2F, -5.5F, 7F, 4F, 1F).texOffs(52, 131).addBox(-2F, -3.5F, -5.5F, 4F, 7F, 1F).texOffs(38, 160).addBox(-3.5F, -2F, 4.5F, 7F, 4F, 1F).texOffs(62, 131).addBox(-2F, -3.5F, 4.5F, 4F, 7F, 1F).texOffs(54, 160).addBox(-1F, -4F, -2F, 2F, 1F, 4F).texOffs(100, 147).addBox(2.5F, -1F, -2F, 1F, 2F, 4F).texOffs(92, 170).addBox(-4.5F, -4F, -4.5F, 9F, 1F, 1F).texOffs(0, 173).addBox(-4.5F, -4F, 3.5F, 9F, 1F, 1F).texOffs(72, 131).addBox(-4.5F, -4F, -3.5F, 1F, 1F, 7F).texOffs(88, 131).addBox(3.5F, -4F, -3.5F, 1F, 1F, 7F), PartPose.offsetAndRotation(-13.5F, 0F, 1F, 0F, 0F, 0F));
        PartDefinition p_hull__pod1__glow = p_hull__pod1.addOrReplaceChild("glow", CubeListBuilder.create().texOffs(66, 160).addBox(-2F, -2F, 5.2F, 4F, 4F, 1F).texOffs(112, 165).addBox(-1F, -1F, 5.6F, 2F, 2F, 1F).texOffs(110, 147).addBox(3F, -1F, -2F, 1F, 2F, 4F).texOffs(0, 154).addBox(-4F, -1F, -2F, 1F, 2F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_hull__pod1__rotor = p_hull__pod1.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(20, 173).addBox(-4F, -0.5F, -0.5F, 8F, 1F, 1F).texOffs(84, 112).addBox(-0.5F, -0.5F, -4F, 1F, 1F, 8F).texOffs(118, 165).addBox(-1F, -1F, -1F, 2F, 1F, 2F), PartPose.offsetAndRotation(0F, -3.8F, 0F, 0F, 0F, 0F));
        PartDefinition p_hull__pod2 = p_hull.addOrReplaceChild("pod2", CubeListBuilder.create().texOffs(32, 70).addBox(-3F, -3F, -5F, 6F, 6F, 10F).texOffs(76, 160).addBox(-3.5F, -2F, -5.5F, 7F, 4F, 1F).texOffs(104, 131).addBox(-2F, -3.5F, -5.5F, 4F, 7F, 1F).texOffs(92, 160).addBox(-3.5F, -2F, 4.5F, 7F, 4F, 1F).texOffs(114, 131).addBox(-2F, -3.5F, 4.5F, 4F, 7F, 1F).texOffs(108, 160).addBox(-1F, -4F, -2F, 2F, 1F, 4F).texOffs(38, 173).addBox(-4.5F, -4F, -4.5F, 9F, 1F, 1F).texOffs(58, 173).addBox(-4.5F, -4F, 3.5F, 9F, 1F, 1F).texOffs(0, 139).addBox(-4.5F, -4F, -3.5F, 1F, 1F, 7F).texOffs(16, 139).addBox(3.5F, -4F, -3.5F, 1F, 1F, 7F), PartPose.offsetAndRotation(0F, -1F, 13F, 0F, 0F, 0F));
        PartDefinition p_hull__pod2__glow = p_hull__pod2.addOrReplaceChild("glow", CubeListBuilder.create().texOffs(0, 165).addBox(-2F, -2F, 5.2F, 4F, 4F, 1F).texOffs(0, 170).addBox(-1F, -1F, 5.6F, 2F, 2F, 1F).texOffs(10, 154).addBox(3F, -1F, -2F, 1F, 2F, 4F).texOffs(20, 154).addBox(-4F, -1F, -2F, 1F, 2F, 4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_hull__pod2__rotor = p_hull__pod2.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(78, 173).addBox(-4F, -0.5F, -0.5F, 8F, 1F, 1F).texOffs(102, 112).addBox(-0.5F, -0.5F, -4F, 1F, 1F, 8F).texOffs(6, 170).addBox(-1F, -1F, -1F, 2F, 1F, 2F), PartPose.offsetAndRotation(0F, -3.8F, 0F, 0F, 0F, 0F));
        PartDefinition p_hull__door_l = p_hull.addOrReplaceChild("door_l", CubeListBuilder.create().texOffs(74, 86).addBox(0F, 0F, -5F, 5F, 1F, 12F), PartPose.offsetAndRotation(-5F, 3F, -1F, 0F, 0F, 0F));
        PartDefinition p_hull__door_r = p_hull.addOrReplaceChild("door_r", CubeListBuilder.create().texOffs(0, 99).addBox(-5F, 0F, -5F, 5F, 1F, 12F), PartPose.offsetAndRotation(5F, 3F, -1F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 256);
    }

    public static Map<String, ModelPart> bones(ModelPart root) {
        Map<String, ModelPart> m = new HashMap<>();
        m.put("hull", root.getChild("hull"));
        m.put("pod0", root.getChild("hull").getChild("pod0"));
        m.put("glow", root.getChild("hull").getChild("pod2").getChild("glow"));
        m.put("rotor", root.getChild("hull").getChild("pod2").getChild("rotor"));
        m.put("pod1", root.getChild("hull").getChild("pod1"));
        m.put("pod2", root.getChild("hull").getChild("pod2"));
        m.put("door_l", root.getChild("hull").getChild("door_l"));
        m.put("door_r", root.getChild("hull").getChild("door_r"));
        return m;
    }

    private FlyingDestroyerGeo() {}
}
