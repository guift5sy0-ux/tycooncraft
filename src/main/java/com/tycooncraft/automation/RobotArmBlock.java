package com.tycooncraft.automation;

import com.mojang.serialization.MapCodec;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 로봇 팔: 설치할 때 바라보는 방향이 '내려놓는 쪽', 등 뒤가 '집는 쪽'. 아이템 들고 우클릭 = 필터, 웅크리고 빈손 = 필터 해제 */
public class RobotArmBlock extends BaseEntityBlock {
    public static final MapCodec<RobotArmBlock> CODEC = simpleCodec(RobotArmBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 10, 15);

    public RobotArmBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack st, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RobotArmBlockEntity a)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            a.setFilter(st);
            player.displayClientMessage(Component.translatable("message.tycooncraft.filter.set", st.getHoverName()), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RobotArmBlockEntity a) {
            if (player.isShiftKeyDown()) {
                a.setFilter(ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("message.tycooncraft.filter.clear"), true);
            } else {
                Component f = a.filter().isEmpty() ? Component.translatable("message.tycooncraft.filter.any") : a.filter().getHoverName();
                float r = com.tycooncraft.power.PowerGrid.ratio(level, pos, com.tycooncraft.power.Layer.ELEC);
                player.displayClientMessage(Component.translatable("message.tycooncraft.robot_arm.status", f, Math.round(r * 100)), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity who, ItemStack st) {
        super.setPlacedBy(level, pos, state, who, st);
        if (!level.isClientSide && who instanceof Player p) p.displayClientMessage(Component.translatable("message.tycooncraft.tip.robot_arm"), true);
    }

    /** 부수면 집게에 든 아이템은 떨어뜨림 */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState ns, boolean moved) {
        if (!state.is(ns.getBlock()) && level.getBlockEntity(pos) instanceof RobotArmBlockEntity a && !a.held().isEmpty())
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, a.held().copy());
        super.onRemove(state, level, pos, ns, moved);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RobotArmBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ROBOT_ARM.get(), (l, p, s, be) -> be.serverTick());
    }
}
