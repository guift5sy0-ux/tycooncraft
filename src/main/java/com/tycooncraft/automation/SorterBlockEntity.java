package com.tycooncraft.automation;

import com.tycooncraft.blockentity.ItemTransfer;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * v0.23.0 분류기: 컨베이어에서 올라온 아이템 중 필터와 같은 것은 왼쪽으로, 나머지는 앞으로 보낸다.
 * 동력 필요 없음. 여러 개를 이어 붙이면 여러 갈래로 나눌 수 있음.
 */
public class SorterBlockEntity extends BlockEntity {
    public static int sortedLeft, passed;
    private ItemStack filter = ItemStack.EMPTY;
    /** 클라이언트: 가림판 애니메이션 */
    public long flapAt = -100;
    public boolean flapLeft;

    public SorterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SORTER.get(), pos, state);
    }

    public ItemStack filter() {
        return filter;
    }

    public void setFilter(ItemStack st) {
        filter = st.isEmpty() ? ItemStack.EMPTY : st.copyWithCount(1);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public Direction out(ItemStack s) {
        Direction f = getBlockState().getValue(SorterBlock.FACING);
        return !filter.isEmpty() && ItemStack.isSameItemSameComponents(s, filter) ? f.getCounterClockWise() : f;
    }

    /** 상자·로봇 팔이 넣을 때도 바로 갈래로 보냄 */
    public final IItemHandler handler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (level == null || stack.isEmpty()) return stack;
            if (simulate) return ItemStack.EMPTY;
            route(stack.copy());
            return ItemStack.EMPTY;
        }
    };

    private ItemStack route(ItemStack s) {
        Direction d = out(s);
        ItemStack rest = ItemTransfer.output(level, worldPosition, d, s, true);
        if (rest.getCount() < s.getCount()) {
            boolean left = d != getBlockState().getValue(SorterBlock.FACING);
            if (left) sortedLeft += s.getCount() - rest.getCount();
            else passed += s.getCount() - rest.getCount();
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, left ? 1 : 0);
        }
        return rest;
    }

    public void serverTick() {
        if (level == null || level.getGameTime() % 2 != 0) return;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition).expandTowards(0, 0.4, 0))) {
            if (!e.isAlive()) continue;
            ItemStack rest = route(e.getItem());
            if (rest.isEmpty()) e.discard();
            else e.setItem(rest);
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            if (level != null && level.isClientSide) {
                flapAt = level.getGameTime();
                flapLeft = param == 1;
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        if (!filter.isEmpty()) tag.put("Filter", filter.save(reg));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        filter = tag.contains("Filter") ? ItemStack.parseOptional(reg, tag.getCompound("Filter")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        CompoundTag t = new CompoundTag();
        saveAdditional(t, reg);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
