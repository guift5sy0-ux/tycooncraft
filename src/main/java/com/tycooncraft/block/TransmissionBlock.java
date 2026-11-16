package com.tycooncraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * 변속기: 컨베이어 줄 옆(위·아래 포함)에 붙이면 이어진 컨베이어 전체의 속도를 정한다.
 * 우클릭으로 1단(보통) → 2단(빠름) → 3단(매우 빠름). 부수면 1단으로 돌아간다.
 */
public class TransmissionBlock extends Block {
    public static final MapCodec<TransmissionBlock> CODEC = simpleCodec(TransmissionBlock::new);
    public static final IntegerProperty SPEED = IntegerProperty.create("speed", 1, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    private static final int MAX_CHAIN = 1024;

    public TransmissionBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(SPEED, 1));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(SPEED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int next = state.getValue(SPEED) % 3 + 1;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(SPEED, next), 3);
            int n = apply(level, pos, next);
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6f, 0.8f + next * 0.25f);
            player.displayClientMessage(Component.translatable("message.tycooncraft.transmission", next, n)
                    .withStyle(next == 1 ? ChatFormatting.GREEN : next == 2 ? ChatFormatting.YELLOW : ChatFormatting.RED), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide && !old.is(this)) apply(level, pos, state.getValue(SPEED));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moving) {
        if (!level.isClientSide && !now.is(this)) apply(level, pos, 1);
        super.onRemove(state, level, pos, now, moving);
    }

    /** pos 에 붙은 컨베이어들과 거기에 이어진 모든 컨베이어의 속도를 바꾼다. 바뀐 개수 반환 */
    public static int apply(Level level, BlockPos pos, int speed) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        seen.add(pos);
        for (Direction d : Direction.values()) q.add(pos.relative(d));
        int n = 0;
        while (!q.isEmpty() && seen.size() < MAX_CHAIN) {
            BlockPos p = q.poll();
            if (!seen.add(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof ConveyorBlock)) continue;
            if (s.getValue(ConveyorBlock.SPEED_LEVEL) != speed) level.setBlock(p, s.setValue(ConveyorBlock.SPEED_LEVEL, speed), 2);
            n++;
            for (Direction d : Direction.values()) q.add(p.relative(d));
        }
        return n;
    }
}
