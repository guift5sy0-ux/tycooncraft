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
        PartDefinition p_hull = p_root.addOrReplaceChild("hull", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -3F, -18F, 10F, 7F, 30F).texOffs(0, 37).addBox(-6F, -5F, -15F, 12F, 2F, 26F).texOffs(0, 125).addBox(-4F, 4F, -16F, 8F, 1F, 10F).texOffs(0, 146).addBox(-4F, 4F, 4F, 8F, 1F, 7F).texOffs(32, 112).addBox(-7F, -4F, -22F, 14F, 8F, 4F).texOffs(0, 136).addBox(-6F, -3F, -26F, 12F, 6F, 4F).texOffs(24, 154).addBox(-4F, -2F, -29F, 8F, 4F, 3F).texOffs(70, 162).addBox(-7.5F, -1F, -21F, 1F, 3F, 3F).texOffs(78, 162).addBox(6.5F, -1F, -21F, 1F, 3F, 3F).texOffs(100, 169).addBox(-2F, -1F, -30F, 4F, 3F, 1F).texOffs(42, 178).addBox(-1F, 0F, -30.5F, 2F, 1F, 1F).texOffs(30, 146).addBox(-5F, 4F, -25F, 10F, 1F, 7F).texOffs(110, 169).addBox(-3F, -5F, -21F, 6F, 1F, 3F).texOffs(76, 37).addBox(-8F, -3F, -16F, 3F, 6F, 22F).texOffs(50, 65).addBox(-8F, 1F, -14F, 1F, 1F, 18F).texOffs(0, 169).addBox(-9F, -1F, -13F, 1F, 2F, 3F).texOffs(0, 174).addBox(-9F, -0.5F, -16F, 1F, 1F, 3F).texOffs(8, 169).addBox(-9F, -1F, -6F, 1F, 2F, 3F).texOffs(8, 174).addBox(-9F, -0.5F, -9F, 1F, 1F, 3F).texOffs(16, 169).addBox(-9F, -1F, 1F, 1F, 2F, 3F).texOffs(16, 174).addBox(-9F, -0.5F, -2F, 1F, 1F, 3F).texOffs(48, 178).addBox(-7F, -4F, -6F, 1F, 1F, 1F).texOffs(0, 65).addBox(5F, -3F, -16F, 3F, 6F, 22F).texOffs(88, 65).addBox(7F, 1F, -14F, 1F, 1F, 18F).texOffs(24, 169).addBox(8F, -1F, -13F, 1F, 2F, 3F).texOffs(24, 174).addBox(8F, -0.5F, -16F, 1F, 1F, 3F).texOffs(32, 169).addBox(8F, -1F, -6F, 1F, 2F, 3F).texOffs(32, 174).addBox(8F, -0.5F, -9F, 1F, 1F, 3F).texOffs(40, 169).addBox(8F, -1F, 1F, 1F, 2F, 3F).texOffs(40, 174).addBox(8F, -0.5F, -2F, 1F, 1F, 3F).texOffs(52, 178).addBox(6F, -4F, -6F, 1F, 1F, 1F).texOffs(68, 112).addBox(-4F, -9F, 2F, 8F, 4F, 8F).texOffs(42, 136).addBox(-3F, -12F, 3F, 6F, 3F, 6F).texOffs(86, 162).addBox(-2F, -14F, 4F, 4F, 2F, 4F).texOffs(96, 174).addBox(-4.2F, -8F, 1.8F, 8F, 2F, 1F).texOffs(56, 178).addBox(-3.2F, -11F, 2.8F, 6F, 1F, 1F).texOffs(64, 146).addBox(-4.2F, -8F, 3F, 1F, 2F, 6F).texOffs(78, 146).addBox(3.2F, -8F, 3F, 1F, 2F, 6F).texOffs(102, 162).addBox(-0.5F, -19F, 6F, 1F, 5F, 1F).texOffs(70, 178).addBox(-2.5F, -17F, 6F, 5F, 1F, 1F).texOffs(82, 178).addBox(-0.5F, -20F, 6F, 1F, 1F, 1F).texOffs(114, 174).addBox(1.5F, -16F, 4F, 2F, 2F, 1F).texOffs(48, 174).addBox(-6.5F, -7F, 5F, 2F, 2F, 2F).texOffs(56, 174).addBox(-6F, -6.5F, 2F, 1F, 1F, 3F).texOffs(64, 174).addBox(4.5F, -7F, 5F, 2F, 2F, 2F).texOffs(72, 174).addBox(5F, -6.5F, 2F, 1F, 1F, 3F).texOffs(32, 136).addBox(-0.5F, -11F, 10F, 1F, 6F, 4F).texOffs(36, 125).addBox(-6F, -4F, 10F, 12F, 8F, 3F).texOffs(120, 174).addBox(-5.5F, -3F, 12.5F, 2F, 2F, 1F).texOffs(0, 178).addBox(3.5F, -3F, 12.5F, 2F, 2F, 1F).texOffs(6, 178).addBox(-5.5F, 1F, 12.5F, 2F, 2F, 1F).texOffs(12, 178).addBox(3.5F, 1F, 12.5F, 2F, 2F, 1F).texOffs(46, 154).addBox(-11F, -1F, -2F, 3F, 2F, 5F).texOffs(62, 154).addBox(8F, -1F, -2F, 3F, 2F, 5F).texOffs(66, 125).addBox(-4F, 4F, -9F, 8F, 1F, 10F), PartPose.offsetAndRotation(0F, 12F, 0F, 0F, 0F, 0F));
        PartDefinition p_hull__turret0 = p_hull.addOrReplaceChild("turret0", CubeListBuilder.create().texOffs(92, 146).addBox(-3F, -2F, -3F, 6F, 2F, 6F).texOffs(48, 169).addBox(-2F, -3F, -2F, 4F, 1F, 4F).texOffs(78, 154).addBox(-1.5F, -1.5F, -9F, 1F, 1F, 6F).texOffs(92, 154).addBox(0.5F, -1.5F, -9F, 1F, 1F, 6F).texOffs(86, 178).addBox(-2F, -2F, -4F, 4F, 1F, 1F), PartPose.offsetAndRotation(0F, -5F, -12F, 0F, 0F, 0F));
        PartDefinition p_hull__turret1 = p_hull.addOrReplaceChild("turret1", CubeListBuilder.create().texOffs(0, 154).addBox(-3F, -2F, -3F, 6F, 2F, 6F).texOffs(64, 169).addBox(-2F, -3F, -2F, 4F, 1F, 4F).texOffs(106, 154).addBox(-1.5F, -1.5F, -9F, 1F, 1F, 6F).texOffs(0, 162).addBox(0.5F, -1.5F, -9F, 1F, 1F, 6F).texOffs(96, 178).addBox(-2F, -2F, -4F, 4F, 1F, 1F), PartPose.offsetAndRotation(0F, -5F, -4F, 0F, 0F, 0F));
        PartDefinition p_hull__pod0 = p_hull.addOrReplaceChild("pod0", CubeListBuilder.create().texOffs(0, 93).addBox(-2.5F, -2.5F, -7F, 5F, 5F, 14F).texOffs(14, 162).addBox(-3F, -3F, -7.5F, 6F, 6F, 1F).texOffs(28, 162).addBox(-3F, -3F, 5F, 6F, 6F, 1F).texOffs(66, 136).addBox(-0.5F, -3.5F, -4F, 1F, 1F, 8F).texOffs(18, 178).addBox(-1F, -3.2F, 0F, 2F, 1F, 2F), PartPose.offsetAndRotation(13.5F, 0F, 1F, 0F, 0F, 0F));
        PartDefinition p_hull__pod0__glow = p_hull__pod0.addOrReplaceChild("glow", CubeListBuilder.create().texOffs(80, 174).addBox(-1.5F, -1.5F, 6.6F, 3F, 3F, 1F).texOffs(106, 178).addBox(-0.5F, -0.5F, 7.2F, 1F, 1F, 1F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_hull__pod0__rotor = p_hull__pod0.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(110, 178).addBox(-2F, -0.5F, -0.5F, 4F, 1F, 1F).texOffs(80, 169).addBox(-0.5F, -2F, -0.5F, 1F, 4F, 1F).texOffs(120, 178).addBox(-0.5F, -0.5F, -0.8F, 1F, 1F, 1F), PartPose.offsetAndRotation(0F, 0F, -7.8F, 0F, 0F, 0F));
        PartDefinition p_hull__pod1 = p_hull.addOrReplaceChild("pod1", CubeListBuilder.create().texOffs(38, 93).addBox(-2.5F, -2.5F, -7F, 5F, 5F, 14F).texOffs(42, 162).addBox(-3F, -3F, -7.5F, 6F, 6F, 1F).texOffs(56, 162).addBox(-3F, -3F, 5F, 6F, 6F, 1F).texOffs(84, 136).addBox(-0.5F, -3.5F, -4F, 1F, 1F, 8F).texOffs(26, 178).addBox(-1F, -3.2F, 0F, 2F, 1F, 2F), PartPose.offsetAndRotation(-13.5F, 0F, 1F, 0F, 0F, 0F));
        PartDefinition p_hull__pod1__glow = p_hull__pod1.addOrReplaceChild("glow", CubeListBuilder.create().texOffs(88, 174).addBox(-1.5F, -1.5F, 6.6F, 3F, 3F, 1F).texOffs(124, 178).addBox(-0.5F, -0.5F, 7.2F, 1F, 1F, 1F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_hull__pod1__rotor = p_hull__pod1.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(0, 181).addBox(-2F, -0.5F, -0.5F, 4F, 1F, 1F).texOffs(84, 169).addBox(-0.5F, -2F, -0.5F, 1F, 4F, 1F).texOffs(10, 181).addBox(-0.5F, -0.5F, -0.8F, 1F, 1F, 1F), PartPose.offsetAndRotation(0F, 0F, -7.8F, 0F, 0F, 0F));
        PartDefinition p_hull__pod2 = p_hull.addOrReplaceChild("pod2", CubeListBuilder.create().texOffs(100, 112).addBox(-4F, -3.5F, -1F, 8F, 7F, 5F).texOffs(102, 136).addBox(-4.5F, -4F, 3.5F, 9F, 8F, 1F).texOffs(88, 169).addBox(-1F, -4.5F, -1F, 2F, 1F, 4F), PartPose.offsetAndRotation(0F, -1F, 13F, 0F, 0F, 0F));
        PartDefinition p_hull__pod2__glow = p_hull__pod2.addOrReplaceChild("glow", CubeListBuilder.create().texOffs(106, 162).addBox(-3F, -2.5F, 4.2F, 6F, 5F, 1F).texOffs(34, 178).addBox(-1.5F, -1F, 4.8F, 3F, 2F, 1F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_hull__pod2__rotor = p_hull__pod2.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(14, 181).addBox(-2.5F, -0.5F, 0F, 5F, 1F, 1F).texOffs(120, 162).addBox(-0.5F, -2.5F, 0F, 1F, 5F, 1F), PartPose.offsetAndRotation(0F, 0F, 5.4F, 0F, 0F, 0F));
        PartDefinition p_hull__door_l = p_hull.addOrReplaceChild("door_l", CubeListBuilder.create().texOffs(76, 93).addBox(0F, 0F, -6F, 4F, 1F, 12F), PartPose.offsetAndRotation(-4F, 4.5F, -4F, 0F, 0F, 0F));
        PartDefinition p_hull__door_r = p_hull.addOrReplaceChild("door_r", CubeListBuilder.create().texOffs(0, 112).addBox(-4F, 0F, -6F, 4F, 1F, 12F), PartPose.offsetAndRotation(4F, 4.5F, -4F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 256);
    }

    public static Map<String, ModelPart> bones(ModelPart root) {
        Map<String, ModelPart> m = new HashMap<>();
        m.put("hull", root.getChild("hull"));
        m.put("turret0", root.getChild("hull").getChild("turret0"));
        m.put("turret1", root.getChild("hull").getChild("turret1"));
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
