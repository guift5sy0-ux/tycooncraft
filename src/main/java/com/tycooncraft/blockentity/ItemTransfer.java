package com.tycooncraft.blockentity;

import com.tycooncraft.block.ConveyorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** 블록 앞쪽으로 아이템 내보내기 */
public final class ItemTransfer {

    /**
     * 앞 블록이 인벤토리면 넣고, 컨베이어면(또는 allowAir 이고 빈칸이면) 그 위에 놓는다.
     * @return 내보내지 못하고 남은 아이템
     */
    public static ItemStack output(Level level, BlockPos from, Direction dir, ItemStack stack, boolean allowAir) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        BlockPos front = from.relative(dir);

        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, front, dir.getOpposite());
        if (target != null) {
            return ItemHandlerHelper.insertItemStacked(target, stack, false);
        }
        BlockState state = level.getBlockState(front);
        boolean conveyor = state.getBlock() instanceof ConveyorBlock;
        boolean open = allowAir && state.getCollisionShape(level, front).isEmpty();
        if (conveyor || open) {
            spawnAt(level, front, dir, stack);
            return ItemStack.EMPTY;
        }
        return stack;
    }

    /** pos 블록 위(컨베이어 높이)에 아이템을 놓고 dir 방향으로 밀어 줌 */
    public static void spawnAt(Level level, BlockPos pos, Direction dir, ItemStack stack) {
        double x = pos.getX() + 0.5 - dir.getStepX() * 0.35;
        double z = pos.getZ() + 0.5 - dir.getStepZ() * 0.35;
        ItemEntity entity = new ItemEntity(level, x, pos.getY() + 0.3, z, stack.copy(),
                dir.getStepX() * 0.08, 0.0, dir.getStepZ() * 0.08);
        entity.setPickUpDelay(10);
        level.addFreshEntity(entity);
    }

    /** 이미 있는 아이템 엔티티를 앞쪽으로 옮김 (통과·리프트용) */
    public static boolean moveEntity(Level level, BlockPos from, Direction dir, ItemEntity entity, boolean allowAir) {
        ItemStack rest = output(level, from, dir, entity.getItem(), allowAir);
        if (rest.isEmpty()) {
            entity.discard();
            return true;
        }
        entity.setItem(rest);
        return false;
    }

    private ItemTransfer() {}
}
