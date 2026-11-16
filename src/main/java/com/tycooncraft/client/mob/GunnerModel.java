package com.tycooncraft.client.mob;

import com.tycooncraft.mob.GunnerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/** 총잡이: 적을 노리면 두 팔로 총을 겨눔 */
public class GunnerModel<T extends GunnerEntity> extends HumanoidModel<T> {

    public GunnerModel(ModelPart root) {
        super(root);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        if (entity.isAggressive()) {
            rightArmPose = entity.getMainHandItem().getItem() instanceof com.tycooncraft.gun.GunItem
                    ? com.tycooncraft.client.GunPoses.poseFor(entity.getMainHandItem()) : ArmPose.BOW_AND_ARROW;
            leftArmPose = ArmPose.EMPTY;
        } else {
            rightArmPose = entity.getMainHandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
            leftArmPose = ArmPose.EMPTY;
        }
    }
}
