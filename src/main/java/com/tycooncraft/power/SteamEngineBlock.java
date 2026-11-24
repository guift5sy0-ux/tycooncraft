package com.tycooncraft.power;

import com.mojang.serialization.MapCodec;
import com.tycooncraft.registry.ModBlockEntities;
import com.tycooncraft.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
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

/** 증기기관: 붙어 있는 보일러의 증기로 피스톤을 움직여 플라이휠을 돌림 → 동력 32 P/t. 모든 면으로 축과 이어짐 */
public class SteamEngineBlock extends BaseEntityBlock {
    public static final MapCodec<SteamEngineBlock> CODEC = simpleCodec(SteamEngineBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SteamEngineBlock(Properties p) {
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
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!(level.getBlockEntity(pos) instanceof SteamEngineBlockEntity e) || !e.spinning()) return;
        if (r.nextInt(3) == 0)   // 배기 증기 (하얗게 금방 사라짐)
            level.addParticle(ModParticles.SMOKE.get(), pos.getX() + 0.5 + r.nextGaussian() * 0.1, pos.getY() + 1.05, pos.getZ() + 0.5 + r.nextGaussian() * 0.1, 0, 0.05, 0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamEngineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.STEAM_ENGINE.get(), (l, p, s, be) -> {
            if (l.isClientSide) be.clientTick();
            else be.serverTick();
        });
    }
}
