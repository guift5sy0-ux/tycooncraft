package com.tycooncraft.client;

import com.tycooncraft.gun.GunItem;
import com.tycooncraft.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * 3인칭 총 자세 (진짜 총을 쥔 것처럼):
 * 권총 - 두 손으로 감싸 쥔 이등변 자세 / 소총류 - 오른손은 손잡이, 왼손은 총열 덮개를 받침 /
 * 쌍권총 - 양팔을 나란히 / RPG - 어깨에 얹고 왼손으로 앞 손잡이. 달릴 때는 총을 가슴 앞에 비스듬히 내림.
 */
public final class GunPoses {
    public static final EnumProxy<HumanoidModel.ArmPose> PISTOL_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, true,
            (IArmPoseTransformer) (m, e, arm) -> pose(m, e, arm, -0.12f, 0.55f, 0f, 0f));
    public static final EnumProxy<HumanoidModel.ArmPose> RIFLE_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, true,
            (IArmPoseTransformer) (m, e, arm) -> pose(m, e, arm, -0.32f, 0.42f, 0.12f, -0.12f));
    public static final EnumProxy<HumanoidModel.ArmPose> DUAL_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, true,
            (IArmPoseTransformer) (m, e, arm) -> pose(m, e, arm, -0.06f, 0.06f, 0f, 0f));
    public static final EnumProxy<HumanoidModel.ArmPose> RPG_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, true,
            (IArmPoseTransformer) (m, e, arm) -> pose(m, e, arm, -0.1f, 0.3f, 0f, -0.18f));

    /** main: 총을 쥔 팔의 안쪽 각도, support: 받치는 팔의 안쪽 각도, mainDrop/supportDrop: 위아래 보정 */
    private static void pose(HumanoidModel<?> m, LivingEntity e, HumanoidArm arm, float main, float support, float mainDrop, float supportDrop) {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart g = right ? m.rightArm : m.leftArm;      // 총 쥔 팔
        ModelPart s = right ? m.leftArm : m.rightArm;      // 받치는 팔
        float side = right ? 1f : -1f;
        if (e.isSprinting() && !e.isUsingItem()) {
            // 달리기: 총구를 아래로, 가슴 앞에 비스듬히
            g.xRot = -0.85f;
            g.yRot = -0.45f * side;
            s.xRot = -1.15f;
            s.yRot = 0.75f * side;
            return;
        }
        float pitch = m.head.xRot, yaw = m.head.yRot;
        g.xRot = -Mth.HALF_PI + pitch + mainDrop;
        g.yRot = main * side + yaw;
        g.zRot = 0;
        s.xRot = -Mth.HALF_PI + pitch + supportDrop;
        s.yRot = support * side + yaw;
        s.zRot = 0;
    }

    public static HumanoidModel.ArmPose poseFor(ItemStack stack) {
        if (stack.is(ModItems.DUAL_PISTOLS.get())) return DUAL_PROXY.getValue();
        if (stack.is(ModItems.PISTOL.get()) || stack.is(ModItems.JUDGE.get())) return PISTOL_PROXY.getValue();
        if (stack.is(ModItems.RPG.get())) return RPG_PROXY.getValue();
        if (stack.getItem() instanceof GunItem) return RIFLE_PROXY.getValue();
        return HumanoidModel.ArmPose.ITEM;
    }

    private GunPoses() {}
}
