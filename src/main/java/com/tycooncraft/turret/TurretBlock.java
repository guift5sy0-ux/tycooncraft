package com.tycooncraft.turret;

import com.mojang.serialization.MapCodec;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** 자동 포탑: 우클릭으로 탄약(중탄약·경탄약) 넣기. 24블록 안의 적대적 몹을 자동 사격 */
public class TurretBlock extends BaseEntityBlock {
    public static final MapCodec<TurretBlock> CODEC = simpleCodec(TurretBlock::new);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 14, 16);

    public TurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurretBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.TURRET.get(),
                level.isClientSide ? TurretBlockEntity::clientTick : TurretBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player p && level.getBlockEntity(pos) instanceof TurretBlockEntity t) t.setOwner(p.getUUID());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TurretBlockEntity t) {
            if (player.isShiftKeyDown() && (t.owner() == null || t.owner().equals(player.getUUID()))) {   // 주인만 모드 전환
                t.toggleIntruders();
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(t.intruders() ? "message.tycooncraft.turret.mode_intruders" : "message.tycooncraft.turret.mode_monsters")
                        .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
            } else player.openMenu(t);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TurretBlockEntity t) {
            Containers.dropContents(level, pos, t.ammo());
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
