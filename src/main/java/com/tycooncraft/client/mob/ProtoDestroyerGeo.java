package com.tycooncraft.client.mob;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.HashMap;
import java.util.Map;

/** 자동 생성 (scripts/gen_mobs.py) — 직접 수정하지 말 것 */
public final class ProtoDestroyerGeo {
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p_root = mesh.getRoot();
        PartDefinition p_body = p_root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(128, 64).addBox(-8F, -6F, -5F, 16F, 8F, 11F).texOffs(0, 0).addBox(-15F, -26F, -9F, 30F, 21F, 18F).texOffs(0, 102).addBox(-13F, -28F, -7F, 20F, 3F, 13F).texOffs(50, 39).addBox(-16F, -22F, -6F, 3F, 14F, 10F).texOffs(182, 64).addBox(12F, -25F, -8F, 5F, 10F, 9F).texOffs(182, 132).addBox(-12F, -24F, -10F, 9F, 9F, 1F).texOffs(28, 132).addBox(5F, -22F, -10.5F, 8F, 10F, 1F).texOffs(168, 144).addBox(-4F, -16F, -10F, 7F, 7F, 1F).texOffs(202, 132).addBox(-4F, -17F, -11F, 1F, 9F, 1F).texOffs(206, 132).addBox(-1.5F, -17F, -11F, 1F, 9F, 1F).texOffs(210, 132).addBox(1F, -17F, -11F, 1F, 9F, 1F).texOffs(214, 132).addBox(3F, -17F, -11F, 1F, 9F, 1F).texOffs(106, 154).addBox(-6F, -9F, -11F, 11F, 2F, 2F).texOffs(178, 154).addBox(-10F, -7F, -9.5F, 20F, 2F, 1F).texOffs(66, 102).addBox(-10F, -34F, 9F, 4F, 12F, 4F).texOffs(218, 132).addBox(-11F, -40F, 10F, 3F, 7F, 3F).texOffs(198, 102).addBox(7F, -33F, 9F, 4F, 10F, 4F).texOffs(152, 39).addBox(-12F, -24F, 9F, 24F, 16F, 6F).texOffs(154, 118).addBox(-9F, -22F, 15F, 18F, 11F, 1F).texOffs(226, 64).addBox(-15F, -19F, 1F, 5F, 9F, 9F), PartPose.offsetAndRotation(0F, 3F, 0F, 0.28F, 0F, 0F));
        PartDefinition p_spike0 = p_body.addOrReplaceChild("spike0", CubeListBuilder.create().texOffs(230, 132).addBox(-1F, -8F, -1F, 2F, 8F, 2F), PartPose.offsetAndRotation(-11F, -26F, 6F, -0.5F, 0F, -0.24F));
        PartDefinition p_spike1 = p_body.addOrReplaceChild("spike1", CubeListBuilder.create().texOffs(88, 118).addBox(-1F, -11F, -1F, 2F, 11F, 2F), PartPose.offsetAndRotation(-6F, -26F, 3F, -0.38F, 0F, -0.12F));
        PartDefinition p_spike2 = p_body.addOrReplaceChild("spike2", CubeListBuilder.create().texOffs(46, 132).addBox(-1F, -9F, -1F, 2F, 9F, 2F), PartPose.offsetAndRotation(0F, -26F, 6F, -0.26F, 0F, 0F));
        PartDefinition p_spike3 = p_body.addOrReplaceChild("spike3", CubeListBuilder.create().texOffs(214, 102).addBox(-1F, -12F, -1F, 2F, 12F, 2F), PartPose.offsetAndRotation(6F, -26F, 3F, -0.5F, 0F, 0.12F));
        PartDefinition p_spike4 = p_body.addOrReplaceChild("spike4", CubeListBuilder.create().texOffs(114, 144).addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offsetAndRotation(11F, -26F, 6F, -0.38F, 0F, 0.24F));
        PartDefinition p_cockpit = p_body.addOrReplaceChild("cockpit", CubeListBuilder.create().texOffs(54, 132).addBox(-6F, -2F, -2F, 12F, 2F, 9F).texOffs(0, 144).addBox(-6F, -10F, 6F, 12F, 8F, 2F).texOffs(96, 118).addBox(-7F, -14F, -2F, 1F, 12F, 1F).texOffs(100, 118).addBox(-7F, -14F, 7F, 1F, 12F, 1F).texOffs(104, 118).addBox(6F, -14F, -2F, 1F, 12F, 1F).texOffs(108, 118).addBox(6F, -14F, 7F, 1F, 12F, 1F).texOffs(96, 132).addBox(-7F, -15F, -2F, 14F, 1F, 10F).texOffs(220, 154).addBox(-7F, -6F, -3F, 14F, 1F, 1F), PartPose.offsetAndRotation(0F, -26F, 2F, 0F, 0F, 0F));
        PartDefinition p_head = p_body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 64).addBox(-6F, -6F, -11F, 12F, 8F, 12F).texOffs(192, 118).addBox(-7F, -7F, -8F, 14F, 3F, 9F).texOffs(0, 161).addBox(-5F, -3F, -11.5F, 3F, 1F, 1F).texOffs(8, 161).addBox(2F, -3F, -11.5F, 3F, 1F, 1F).texOffs(16, 161).addBox(-4F, -4F, -11.5F, 2F, 1F, 1F).texOffs(22, 161).addBox(2F, -4F, -11.5F, 2F, 1F, 1F).texOffs(132, 154).addBox(-5F, 0F, -12F, 10F, 2F, 2F), PartPose.offsetAndRotation(0F, -27F, -7F, -0.2F, 0F, 0F));
        PartDefinition p_horn_r = p_head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(28, 144).addBox(-1.5F, -7F, -1.5F, 3F, 7F, 3F), PartPose.offsetAndRotation(-6F, -6F, -4F, -0.6F, 0F, -0.55F));
        PartDefinition p_horn_tip_r = p_horn_r.addOrReplaceChild("horn_tip_r", CubeListBuilder.create().texOffs(236, 144).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, -7F, 0F, -0.5F, 0F, -0.3F));
        PartDefinition p_horn_l = p_head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(40, 144).addBox(-1.5F, -7F, -1.5F, 3F, 7F, 3F), PartPose.offsetAndRotation(6F, -6F, -4F, -0.6F, 0F, 0.55F));
        PartDefinition p_horn_tip_l = p_horn_l.addOrReplaceChild("horn_tip_l", CubeListBuilder.create().texOffs(244, 144).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, -7F, 0F, -0.5F, 0F, 0.3F));
        PartDefinition p_jaw = p_head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(154, 102).addBox(-5.5F, 0F, -11F, 11F, 4F, 11F).texOffs(156, 154).addBox(-4.5F, -1F, -11.5F, 9F, 2F, 2F).texOffs(122, 144).addBox(-6F, 2F, -9F, 12F, 2F, 7F), PartPose.offsetAndRotation(0F, 1F, -1F, 0F, 0F, 0F));
        PartDefinition p_right_arm = p_body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(96, 0).addBox(-8F, -8F, -8F, 15F, 10F, 16F).texOffs(0, 132).addBox(-9F, -10F, -4F, 6F, 4F, 8F).texOffs(76, 39).addBox(-5F, -1F, -5F, 9F, 14F, 10F).texOffs(112, 118).addBox(-5.5F, 5F, -5.5F, 10F, 2F, 11F), PartPose.offsetAndRotation(-18F, -22F, 0F, 0F, 0F, 0F));
        PartDefinition p_arm_spike = p_right_arm.addOrReplaceChild("arm_spike", CubeListBuilder.create().texOffs(160, 144).addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offsetAndRotation(-6F, -9F, 0F, 0F, 0F, 0.5F));
        PartDefinition p_minigun = p_right_arm.addOrReplaceChild("minigun", CubeListBuilder.create().texOffs(0, 84).addBox(-5.5F, 0F, -5.5F, 11F, 7F, 11F).texOffs(0, 154).addBox(-7F, 2F, 3F, 3F, 4F, 3F), PartPose.offsetAndRotation(-0.5F, 13F, 0F, 0F, 0F, 0F));
        PartDefinition p_barrels = p_minigun.addOrReplaceChild("barrels", CubeListBuilder.create().texOffs(44, 84).addBox(1.6F, 0F, -0.2F, 2F, 15F, 2F).texOffs(210, 64).addBox(-1F, 0F, 1.7F, 2F, 17F, 2F).texOffs(52, 84).addBox(-3.6F, 0F, -0.1F, 2F, 15F, 2F).texOffs(218, 64).addBox(-2.6F, 0F, -3.2F, 2F, 17F, 2F).texOffs(60, 84).addBox(0.6F, 0F, -3.2F, 2F, 15F, 2F).texOffs(82, 102).addBox(-1F, 0F, -1F, 2F, 14F, 2F).texOffs(52, 144).addBox(-4F, 7F, -4F, 8F, 2F, 8F), PartPose.offsetAndRotation(0F, 7F, 0F, 0F, 0F, 0F));
        PartDefinition p_left_arm = p_body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(158, 0).addBox(-7F, -8F, -8F, 15F, 10F, 16F).texOffs(114, 39).addBox(-5F, -1F, -5F, 9F, 13F, 10F).texOffs(0, 39).addBox(-6F, 11F, -6.5F, 12F, 12F, 13F).texOffs(68, 84).addBox(-5.5F, 22F, -6F, 11F, 5F, 12F), PartPose.offsetAndRotation(18F, -22F, 0F, 0F, 0F, 0F));
        PartDefinition p_claw0 = p_left_arm.addOrReplaceChild("claw0", CubeListBuilder.create().texOffs(84, 144).addBox(-1F, 0F, -1F, 2F, 7F, 3F), PartPose.offsetAndRotation(-3.5F, 26F, -5F, -0.35F, 0F, 0F));
        PartDefinition p_claw0_tip = p_claw0.addOrReplaceChild("claw0_tip", CubeListBuilder.create().texOffs(28, 154).addBox(-0.5F, 0F, -1F, 1F, 4F, 2F), PartPose.offsetAndRotation(0F, 7F, 0F, -0.5F, 0F, 0F));
        PartDefinition p_claw1 = p_left_arm.addOrReplaceChild("claw1", CubeListBuilder.create().texOffs(94, 144).addBox(-1F, 0F, -1F, 2F, 7F, 3F), PartPose.offsetAndRotation(0F, 26F, -5F, -0.35F, 0F, 0F));
        PartDefinition p_claw1_tip = p_claw1.addOrReplaceChild("claw1_tip", CubeListBuilder.create().texOffs(34, 154).addBox(-0.5F, 0F, -1F, 1F, 4F, 2F), PartPose.offsetAndRotation(0F, 7F, 0F, -0.5F, 0F, 0F));
        PartDefinition p_claw2 = p_left_arm.addOrReplaceChild("claw2", CubeListBuilder.create().texOffs(104, 144).addBox(-1F, 0F, -1F, 2F, 7F, 3F), PartPose.offsetAndRotation(3.5F, 26F, -5F, -0.35F, 0F, 0F));
        PartDefinition p_claw2_tip = p_claw2.addOrReplaceChild("claw2_tip", CubeListBuilder.create().texOffs(40, 154).addBox(-0.5F, 0F, -1F, 1F, 4F, 2F), PartPose.offsetAndRotation(0F, 7F, 0F, -0.5F, 0F, 0F));
        PartDefinition p_pod = p_left_arm.addOrReplaceChild("pod", CubeListBuilder.create().texOffs(114, 84).addBox(-4F, -6F, -6F, 4F, 4F, 13F).texOffs(148, 84).addBox(0F, -7F, -7F, 4F, 4F, 13F).texOffs(182, 84).addBox(-2F, -6F, -2F, 4F, 4F, 13F).texOffs(144, 132).addBox(-5F, -3F, -4F, 10F, 2F, 9F), PartPose.offsetAndRotation(2F, -8F, 0F, 0F, 0F, 0F));
        PartDefinition p_left_leg = p_root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(48, 64).addBox(-5F, 0F, -5F, 10F, 10F, 10F).texOffs(90, 102).addBox(-4F, 9F, -4F, 8F, 8F, 8F).texOffs(184, 144).addBox(-5.5F, 3F, -6F, 11F, 6F, 2F).texOffs(0, 118).addBox(-6F, 16F, -5F, 12F, 4F, 10F).texOffs(46, 154).addBox(-5F, 17F, -8F, 2F, 3F, 3F).texOffs(56, 154).addBox(-1F, 17F, -8F, 2F, 3F, 3F).texOffs(66, 154).addBox(3F, 17F, -8F, 2F, 3F, 3F), PartPose.offsetAndRotation(7F, 4F, 1F, 0F, 0F, 0F));
        PartDefinition p_left_leg_spike = p_left_leg.addOrReplaceChild("left_leg_spike", CubeListBuilder.create().texOffs(12, 154).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, 4F, -6F, -1.1F, 0F, 0F));
        PartDefinition p_right_leg = p_root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(88, 64).addBox(-5F, 0F, -5F, 10F, 10F, 10F).texOffs(122, 102).addBox(-4F, 9F, -4F, 8F, 8F, 8F).texOffs(210, 144).addBox(-5.5F, 3F, -6F, 11F, 6F, 2F).texOffs(44, 118).addBox(-6F, 16F, -5F, 12F, 4F, 10F).texOffs(76, 154).addBox(-5F, 17F, -8F, 2F, 3F, 3F).texOffs(86, 154).addBox(-1F, 17F, -8F, 2F, 3F, 3F).texOffs(96, 154).addBox(3F, 17F, -8F, 2F, 3F, 3F), PartPose.offsetAndRotation(-7F, 4F, 1F, 0F, 0F, 0F));
        PartDefinition p_right_leg_spike = p_right_leg.addOrReplaceChild("right_leg_spike", CubeListBuilder.create().texOffs(20, 154).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, 4F, -6F, -1.1F, 0F, 0F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    public static Map<String, ModelPart> bones(ModelPart root) {
        Map<String, ModelPart> m = new HashMap<>();
        m.put("body", root.getChild("body"));
        m.put("spike0", root.getChild("body").getChild("spike0"));
        m.put("spike1", root.getChild("body").getChild("spike1"));
        m.put("spike2", root.getChild("body").getChild("spike2"));
        m.put("spike3", root.getChild("body").getChild("spike3"));
        m.put("spike4", root.getChild("body").getChild("spike4"));
        m.put("cockpit", root.getChild("body").getChild("cockpit"));
        m.put("head", root.getChild("body").getChild("head"));
        m.put("horn_r", root.getChild("body").getChild("head").getChild("horn_r"));
        m.put("horn_tip_r", root.getChild("body").getChild("head").getChild("horn_r").getChild("horn_tip_r"));
        m.put("horn_l", root.getChild("body").getChild("head").getChild("horn_l"));
        m.put("horn_tip_l", root.getChild("body").getChild("head").getChild("horn_l").getChild("horn_tip_l"));
        m.put("jaw", root.getChild("body").getChild("head").getChild("jaw"));
        m.put("right_arm", root.getChild("body").getChild("right_arm"));
        m.put("arm_spike", root.getChild("body").getChild("right_arm").getChild("arm_spike"));
        m.put("minigun", root.getChild("body").getChild("right_arm").getChild("minigun"));
        m.put("barrels", root.getChild("body").getChild("right_arm").getChild("minigun").getChild("barrels"));
        m.put("left_arm", root.getChild("body").getChild("left_arm"));
        m.put("claw0", root.getChild("body").getChild("left_arm").getChild("claw0"));
        m.put("claw0_tip", root.getChild("body").getChild("left_arm").getChild("claw0").getChild("claw0_tip"));
        m.put("claw1", root.getChild("body").getChild("left_arm").getChild("claw1"));
        m.put("claw1_tip", root.getChild("body").getChild("left_arm").getChild("claw1").getChild("claw1_tip"));
        m.put("claw2", root.getChild("body").getChild("left_arm").getChild("claw2"));
        m.put("claw2_tip", root.getChild("body").getChild("left_arm").getChild("claw2").getChild("claw2_tip"));
        m.put("pod", root.getChild("body").getChild("left_arm").getChild("pod"));
        m.put("left_leg", root.getChild("left_leg"));
        m.put("left_leg_spike", root.getChild("left_leg").getChild("left_leg_spike"));
        m.put("right_leg", root.getChild("right_leg"));
        m.put("right_leg_spike", root.getChild("right_leg").getChild("right_leg_spike"));
        return m;
    }

    private ProtoDestroyerGeo() {}
}
