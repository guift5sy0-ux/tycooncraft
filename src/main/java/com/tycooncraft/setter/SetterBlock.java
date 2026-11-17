package com.tycooncraft.setter;

import com.mojang.serialization.MapCodec;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 세터(자동 블록 설치기): 놓을 때 플레이어가 보는 방향을 바라봄(6방향).
 * 레드스톤 블록 4개 = 1회 충전, Shift+우클릭으로 설치할 블록 넣기, 우클릭으로 출발/정지.
 * 바라보는 방향 말고 다른 면에 부유체를 붙여야 뜬다. 우클릭한 사람이 보는 방향(축에 가까울 때만)으로 한 칸씩 가며 앞에 블록을 놓는다.
 */
public class SetterBlock extends BaseEntityBlock {
    public static final MapCodec<SetterBlock> CODEC = simpleCodec(SetterBlock::new);

    public SetterBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(DirectionalBlock.FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(DirectionalBlock.FACING);
    }

    /** 바라보는 방향을 바라보게 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(DirectionalBlock.FACING, ctx.getNearestLookingDirection());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SetterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SETTER.get(), SetterBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.REDSTONE_BLOCK)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof SetterBlockEntity be) {
            if (stack.getCount() < 4 && !player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.tycooncraft.setter.need_redstone").withStyle(ChatFormatting.RED), true);
            } else if (!be.charge()) {
                player.displayClientMessage(Component.translatable("message.tycooncraft.setter.full").withStyle(ChatFormatting.YELLOW), true);
            } else {
                if (!player.getAbilities().instabuild) stack.shrink(4);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.BLOCKS, 0.7f, 1.6f);
                be.report(player);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof SetterBlockEntity be) {
            if (player.isShiftKeyDown()) {
                if (player instanceof ServerPlayer sp) sp.openMenu(be);
            } else if (be.running()) be.stop(player, "message.tycooncraft.setter.stopped");
            else be.start(player, player.getLookAngle());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !moved && level.getBlockEntity(pos) instanceof SetterBlockEntity be && !be.relocating)
            net.minecraft.world.Containers.dropContents(level, pos, be.items());
        super.onRemove(state, level, pos, newState, moved);
    }
}
