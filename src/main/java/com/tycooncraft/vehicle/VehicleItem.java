package com.tycooncraft.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Supplier;

/** 탈것 설치 아이템: 땅(기관차는 레일)에 우클릭하면 바라보는 방향으로 놓인다 */
public class VehicleItem extends Item {
    private final Supplier<? extends EntityType<?>> type;
    private final boolean rail;
    private final String tip;

    public VehicleItem(Properties properties, Supplier<? extends EntityType<?>> type, boolean rail, String tip) {
        super(properties.stacksTo(1));
        this.type = type;
        this.rail = rail;
        this.tip = tip;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = ctx.getPlayer();
        Vec3 at;
        if (rail) {
            if (!state.is(BlockTags.RAILS) || !(state.getBlock() instanceof BaseRailBlock railBlock)) {
                if (player != null && !level.isClientSide) player.displayClientMessage(Component.translatable("message.tycooncraft.vehicle.need_rail"), true);
                return InteractionResult.FAIL;
            }
            RailShape shape = state.getValue(railBlock.getShapeProperty());
            at = new Vec3(pos.getX() + 0.5, pos.getY() + 0.0625 + (shape.isAscending() ? 0.5 : 0), pos.getZ() + 0.5);
        } else {
            BlockPos p = state.getCollisionShape(level, pos).isEmpty() ? pos : pos.relative(ctx.getClickedFace());
            at = Vec3.atBottomCenterOf(p);
        }
        if (!level.isClientSide) {
            Entity e = type.get().create(level);
            if (e == null) return InteractionResult.FAIL;
            float yaw = player != null ? player.getYRot() : 0;
            e.moveTo(at.x, at.y, at.z, yaw, 0);
            if (!rail && !level.noCollision(e, e.getBoundingBox().deflate(0.05))) {
                if (player != null) player.displayClientMessage(Component.translatable("message.tycooncraft.vehicle.no_space"), true);
                return InteractionResult.FAIL;
            }
            // 크리에이티브에서 놓으면 연료 가득
            if (player != null && player.getAbilities().instabuild) {
                if (e instanceof VehicleEntity ve) ve.setFuel(ve.maxFuel());
                else if (e instanceof LocomotiveEntity loco) loco.setFuel(loco.maxFuel());
            }
            level.addFreshEntity(e);
            ctx.getItemInHand().consume(1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(tip).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
