package com.tycooncraft.blockentity;

import com.tycooncraft.block.LiftBlock;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** 리프트 기둥의 각 블록이 자기 옆에 닿은 아이템을 기둥 끝으로 보낸다 */
public class LiftBlockEntity extends BlockEntity {
    private static final int INTERVAL = 4;
    private static final int MAX_HEIGHT = 64;

    public LiftBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIFT.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.getGameTime() % INTERVAL != 0) return;
        AABB area = new AABB(worldPosition).inflate(0.3, 0.0, 0.3).expandTowards(0, 0.3, 0);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive);
        if (items.isEmpty()) return;

        Block block = getBlockState().getBlock();
        if (!(block instanceof LiftBlock lift)) return;
        int step = lift.isUp() ? 1 : -1;
        BlockPos end = worldPosition;
        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos next = end.above(step);
            if (!level.getBlockState(next).is(block)) break;
            end = next;
        }
        var dir = level.getBlockState(end).getValue(LiftBlock.FACING);

        // 출구 쪽 면에 있는 아이템은 방금 나온 것이므로 다시 빨아들이지 않음
        // (그래서 리프트 입구는 출구 방향이 아닌 다른 면으로 연결)
        for (ItemEntity entity : items) {
            double dx = entity.getX() - (worldPosition.getX() + 0.5);
            double dz = entity.getZ() - (worldPosition.getZ() + 0.5);
            if (dx * dir.getStepX() + dz * dir.getStepZ() > 0.45) continue;
            ItemTransfer.moveEntity(level, end, dir, entity);
        }
    }
}
