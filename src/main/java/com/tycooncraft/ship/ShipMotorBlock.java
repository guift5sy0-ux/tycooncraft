package com.tycooncraft.ship;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** 배 전용 모터: 배 블록 아무 곳에나 붙이면 추진력. 많을수록 빠르고, 배가 클수록 느리다. 프로펠러 = 설치할 때 바라본 쪽 */
public class ShipMotorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ShipMotorBlock> CODEC = simpleCodec(ShipMotorBlock::new);

    public ShipMotorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShipMotorBlockEntity(pos, state);
    }

    /** 연료를 들고 우클릭: 주입 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (ShipFuel.value(stack) <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof ShipMotorBlockEntity be) {
            int n = be.insertFrom(player.getAbilities().instabuild ? stack.copy() : stack);
            if (n > 0) level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.8f, 1.2f);
            report(sl, pos, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 빈손 우클릭: 이 엔진과 같은 구조물(운송기)에 붙은 엔진 전체의 연료 합계 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) report(sl, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void report(ServerLevel level, BlockPos pos, Player player) {
        int[] s = ShipAssembler.structureFuel(level, pos);   // {engines, fuel, capacity, floats, blocks}
        player.displayClientMessage(Component.translatable("message.tycooncraft.ship.fuel_status", s[0], ShipEntity.fuelText(s[1]),
                ShipEntity.fuelText(s[2]), s[3], s[4]).withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }
}
