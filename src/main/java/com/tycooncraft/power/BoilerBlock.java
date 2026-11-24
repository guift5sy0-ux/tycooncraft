package com.tycooncraft.power;

import com.mojang.serialization.MapCodec;
import com.tycooncraft.registry.ModBlockEntities;
import com.tycooncraft.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 보일러: 석탄 등 연료를 태워 물을 끓여 증기를 만든다. 붙어 있는 증기기관이 증기를 가져가 동력을 만듦.
 * 물: 옆(아래 포함)에 물 원천이 있으면 계속 채워지고, 물 양동이로 우클릭해도 됨. 연료: 들고 우클릭하거나 컨베이어·호퍼로 넣기.
 */
public class BoilerBlock extends BaseEntityBlock {
    public static final MapCodec<BoilerBlock> CODEC = simpleCodec(BoilerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public BoilerBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LIT);
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
    protected ItemInteractionResult useItemOn(ItemStack st, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BoilerBlockEntity b)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (st.is(Items.WATER_BUCKET)) {
            if (b.water > BoilerBlockEntity.TANK - 1000) return ItemInteractionResult.CONSUME;
            if (!level.isClientSide) {
                b.water += 1000;
                b.setChanged();
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1f, 1f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (BoilerBlockEntity.isFuel(st)) {
            if (!level.isClientSide) {
                ItemStack rest = b.fuel().insertItem(0, st.copy(), false);
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, rest);
                level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.6f, 0.8f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 빈손 우클릭: 상태 (물·연료·증기) */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BoilerBlockEntity b) {
            ItemStack f = b.fuel().getStackInSlot(0);
            player.displayClientMessage(Component.translatable("message.tycooncraft.boiler.status", b.water, BoilerBlockEntity.TANK,
                    f.isEmpty() ? Component.translatable("message.tycooncraft.boiler.no_fuel") : Component.literal(f.getCount() + "× ").append(f.getHoverName()),
                    b.steam, BoilerBlockEntity.MAX_STEAM), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BoilerBlockEntity b)
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, b.fuel().getStackInSlot(0));
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) return;
        // 굴뚝 연기 (뒤쪽 위) + 화실 불빛
        Direction f = state.getValue(FACING);
        double cx = pos.getX() + 0.5 - f.getStepX() * 0.28, cz = pos.getZ() + 0.5 - f.getStepZ() * 0.28;
        if (r.nextInt(2) == 0) level.addParticle(ModParticles.SMOKE_LARGE.get(), cx, pos.getY() + 1.55, cz, r.nextGaussian() * 0.005, 0.06, r.nextGaussian() * 0.005);
        if (r.nextInt(6) == 0) {
            double fx = pos.getX() + 0.5 + f.getStepX() * 0.52, fz = pos.getZ() + 0.5 + f.getStepZ() * 0.52;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, fx, pos.getY() + 0.22, fz, 0, 0.005, 0);
            if (r.nextInt(4) == 0) level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8f, 0.9f, false);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoilerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.BOILER.get(), (l, p, s, be) -> be.serverTick());
    }
}
