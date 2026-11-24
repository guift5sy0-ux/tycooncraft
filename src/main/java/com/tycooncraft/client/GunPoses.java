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
    // 조준 자세 {main, support, mainDrop, supportDrop} + 들고 다니는 자세 {총 쥔 팔 x,y, 받치는 팔 x,y (받치는 팔 x 가 0 이면 자연스럽게 둠)} + 반동 세기
    public static final EnumProxy<HumanoidModel.ArmPose> PISTOL_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, true,
            (IArmPoseTransformer) (m, e, arm) -> pose(m, e, arm, -0.12f, 0.55f, 0f, 0f, -0.55f, -0.08f, 0f, 0f, 0.35f));
    public static final EnumProxy<HumanoidModel.ArmPose> RIFLE_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, true,
            (IArmPoseTransformer) (m, e, arm) -> pose(m, e, arm, -0.32f, 0.42f, 0.12f, -0.12f, -0.88f, -0.22f, -1.05f, 0.78f, 0.25f));
    public static final EnumProxy<HumanoidModel.ArmPose> DUAL_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, true,
            (IArmPoseTransformer) (m, e, arm) -> pose(m, e, arm, -0.06f, 0.06f, 0f, 0f, -0.3f, 0.05f, -0.3f, -0.05f, 0.35f));
    public static final EnumProxy<HumanoidModel.ArmPose> RPG_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, true,
            (IArmPoseTransformer) (m, e, arm) -> pose(m, e, arm, -0.1f, 0.3f, 0f, -0.18f, -0.82f, -0.25f, -1.0f, 0.68f, 0.45f));
    /** RT-20: 무거운 대물 저격총 — 들고 다닐 땐 더 낮게, 쏘면 크게 튐 */
    public static final EnumProxy<HumanoidModel.ArmPose> HEAVY_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, true,
            (IArmPoseTransformer) (m, e, arm) -> pose(m, e, arm, -0.3f, 0.45f, 0.14f, -0.1f, -0.8f, -0.2f, -0.98f, 0.76f, 0.7f));

    /** main: 총을 쥔 팔의 안쪽 각도, support: 받치는 팔의 안쪽 각도, mainDrop/supportDrop: 위아래 보정, c*: 들고 다니는 자세, kickAmt: 반동 */
    private static void pose(HumanoidModel<?> m, LivingEntity e, HumanoidArm arm, float main, float support, float mainDrop, float supportDrop,
                             float cgx, float cgy, float csx, float csy, float kickAmt) {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart g = right ? m.rightArm : m.leftArm;      // 총 쥔 팔
        ModelPart s = right ? m.leftArm : m.rightArm;      // 받치는 팔
        float side = right ? 1f : -1f;
        float a = GunAnim.aim(e);
        if (e.isSprinting() && !e.isUsingItem() && a < 0.5f) {
            // 달리기: 총구를 아래로, 가슴 앞에 비스듬히
            g.xRot = -0.85f;
            g.yRot = -0.45f * side;
            s.xRot = -1.15f;
            s.yRot = 0.75f * side;
            return;
        }
        float pitch = m.head.xRot, yaw = m.head.yRot;
        // 조준 자세: 두 팔을 시선 방향으로 뻗어 어깨에 견착
        float agx = -Mth.HALF_PI + pitch + mainDrop, agy = main * side + yaw;
        float asx = -Mth.HALF_PI + pitch + supportDrop, asy = support * side + yaw;
        // 들고 다니는 자세(로우 레디): 총구를 앞 아래로 내리고 몸 앞에 비스듬히, 시선은 조금만 따라감
        float bgx = cgx + pitch * 0.25f, bgy = cgy * side + yaw * 0.35f;
        float bsx = csx == 0f ? s.xRot : csx + pitch * 0.25f, bsy = csx == 0f ? s.yRot : csy * side + yaw * 0.35f;
        float k = GunAnim.kick(e) * kickAmt;
        g.xRot = Mth.lerp(a, bgx, agx) - k;       // 반동: 총구가 위로 튐
        g.yRot = Mth.lerp(a, bgy, agy);
        g.zRot = 0;
        s.xRot = Mth.lerp(a, bsx, asx) - k * 0.8f;
        s.yRot = Mth.lerp(a, bsy, asy);
        s.zRot = csx == 0f ? Mth.lerp(a, s.zRot, 0f) : 0;
    }

    public static HumanoidModel.ArmPose poseFor(ItemStack stack) {
        if (stack.is(ModItems.DUAL_PISTOLS.get())) return DUAL_PROXY.getValue();
        if (stack.is(ModItems.PISTOL.get()) || stack.is(ModItems.JUDGE.get())) return PISTOL_PROXY.getValue();
        if (stack.is(ModItems.RPG.get())) return RPG_PROXY.getValue();
        if (stack.is(ModItems.RT20.get())) return HEAVY_PROXY.getValue();
        if (stack.getItem() instanceof GunItem) return RIFLE_PROXY.getValue();
        return HumanoidModel.ArmPose.ITEM;
    }

    private GunPoses() {}
}
