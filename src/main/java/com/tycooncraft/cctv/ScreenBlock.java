package com.tycooncraft.cctv;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * CCTV 스크린. 우클릭: 연결된 CCTV 보기 (숫자키 1~5 전환, 웅크리기/ESC 나가기).
 * 웅크리고 우클릭: 선택해 둔 CCTV 연결/해제 (최대 5대, 128블록 안).
 */
public class ScreenBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final com.mojang.serialization.MapCodec<ScreenBlock> CODEC = simpleCodec(ScreenBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 2, 16, 15, 14);
    public static final int MAX_LINKS = 5, MAX_RANGE = 128;

    public ScreenBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScreenBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState s, Rotation r) {
        return s.setValue(FACING, r.rotate(s.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState s, Mirror m) {
        return s.rotate(m.getRotation(s.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ScreenBlockEntity be)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) link(level, pos, be, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) com.tycooncraft.client.cctv.CctvView.open(pos, be.links());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void link(Level level, BlockPos pos, ScreenBlockEntity be, Player player) {
        var tag = player.getPersistentData();
        var cam = NbtUtils.readBlockPos(tag, CctvBlock.PENDING).orElse(null);
        if (cam == null || !(level.getBlockState(cam).getBlock() instanceof CctvBlock)) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.cctv.select_first").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (be.links().contains(cam)) {
            be.remove(cam);
            player.displayClientMessage(Component.translatable("message.tycooncraft.cctv.unlinked", be.links().size(), MAX_LINKS).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (be.links().size() >= MAX_LINKS) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.cctv.full", MAX_LINKS).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!cam.closerThan(pos, MAX_RANGE)) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.cctv.too_far", MAX_RANGE).withStyle(ChatFormatting.RED), true);
            return;
        }
        be.add(cam);
        tag.remove(CctvBlock.PENDING);
        player.displayClientMessage(Component.translatable("message.tycooncraft.cctv.linked", be.links().size(), MAX_LINKS).withStyle(ChatFormatting.GREEN), true);
    }
}
