package com.tycooncraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tycooncraft.blockentity.MachineBlockEntity;
import com.tycooncraft.machine.MachineType;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 기계 블록 (7종 공용). 동력·연료 필요 없음.
 * 옆·위로 들어온 재료를 가공해 FACING(화살표) 방향으로 내보낸다. 우클릭하면 기계 창.
 */
public class MachineBlock extends BaseEntityBlock {
    public static final MapCodec<MachineBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            MachineType.CODEC.fieldOf("machine").forGetter(MachineBlock::type),
            propertiesCodec()
    ).apply(i, MachineBlock::new));
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final MachineType type;

    public MachineBlock(MachineType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public MachineType type() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        if (level.isClientSide) return null;
        return createTickerHelper(beType, ModBlockEntities.MACHINE.get(), (lvl, pos, st, be) -> be.serverTick());
    }

    /** 우클릭: 기계 창 열기 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            player.openMenu(machine, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            machine.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** v0.23.1: 동력 블록이 옆에 없으면 설치할 때 안내 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity who, net.minecraft.world.item.ItemStack st) {
        super.setPlacedBy(level, pos, state, who, st);
        if (level.isClientSide || !(who instanceof Player p)) return;
        for (Direction d : Direction.values())
            if (level.getBlockEntity(pos.relative(d)) instanceof com.tycooncraft.power.PowerNode n && !(n instanceof MachineBlockEntity)) return;
        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tycooncraft.tip.machine"), true);
    }
}
