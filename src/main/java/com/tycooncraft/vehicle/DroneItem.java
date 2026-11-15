package com.tycooncraft.vehicle;

import com.tycooncraft.network.DronePayloads;
import com.tycooncraft.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** 자폭 드론: 우클릭하면 앞에 띄우고 바로 드론 카메라로 조종 */
public class DroneItem extends Item {

    private final int tier;

    public DroneItem(Properties properties, int tier) {
        super(properties.stacksTo(8));
        this.tier = tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            DroneEntity drone = ModEntities.DRONE.get().create(level);
            if (drone == null) return InteractionResultHolder.fail(stack);
            Vec3 look = Vec3.directionFromRotation(0, player.getYRot());
            Vec3 at = player.getEyePosition().add(look.scale(1.2)).add(0, -0.3, 0);
            drone.moveTo(at.x, at.y, at.z, player.getYRot(), 0);
            drone.setOwner(player);
            drone.setTier(tier);
            if (!level.noCollision(drone, drone.getBoundingBox())) {
                drone.moveTo(player.getX(), player.getY() + 2.1, player.getZ(), player.getYRot(), 0);
            }
            level.addFreshEntity(drone);
            stack.consume(1, player);
            startPiloting(sp, drone);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void startPiloting(ServerPlayer player, DroneEntity drone) {
        drone.setPiloted(true);
        drone.armSound();
        PacketDistributor.sendToPlayer(player, new DronePayloads.Pilot(drone.getId()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        if (tier >= 2) lines.add(Component.translatable("tooltip.tycooncraft.drone_mk2").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.tycooncraft.drone").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.tycooncraft.drone2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
