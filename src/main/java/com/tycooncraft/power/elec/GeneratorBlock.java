package com.tycooncraft.power.elec;

import com.mojang.serialization.MapCodec;
import com.tycooncraft.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** 발전기(동력 → 전기) / 전동기(전기 → 동력) — 모양은 같고 하는 일이 반대 */
public class GeneratorBlock extends ElecBlock {
    public static final MapCodec<GeneratorBlock> CODEC = simpleCodec(p -> new GeneratorBlock(p, false));
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private final boolean motor;

    public GeneratorBlock(Properties p, boolean motor) {
        super(p);
        this.motor = motor;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return motor ? new MotorBlockEntity(pos, state) : new GeneratorBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (!s.getValue(ACTIVE) || r.nextInt(3) != 0) return;
        double x = pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.7, z = pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.7;
        level.addParticle(ModParticles.SPARK.get(), x, pos.getY() + 1.02, z, (r.nextDouble() - 0.5) * 0.04, 0.03, (r.nextDouble() - 0.5) * 0.04);
    }
}
