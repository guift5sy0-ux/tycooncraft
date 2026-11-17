package com.tycooncraft.ship;

import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/** 운송기 엔진의 연료 탱크. 호퍼·손으로 넣은 연료는 바로 연료 단위로 바뀐다 */
public class ShipMotorBlockEntity extends BlockEntity {
    private int fuel;

    private final IItemHandler handler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return ShipFuel.value(stack) > 0; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int v = ShipFuel.value(stack);
            if (v <= 0) return stack;
            int n = Math.min(stack.getCount(), (ShipFuel.MOTOR_CAPACITY - fuel) / v);
            if (n <= 0) return stack;
            if (!simulate) addFuel(n * v);
            return stack.copyWithCount(stack.getCount() - n);
        }
    };

    public ShipMotorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHIP_MOTOR.get(), pos, state);
    }

    public IItemHandler getItemHandler() {
        return handler;
    }

    public int fuel() {
        return fuel;
    }

    public void setFuel(int f) {
        fuel = Math.max(0, Math.min(ShipFuel.MOTOR_CAPACITY, f));
        setChanged();
    }

    public void addFuel(int f) {
        setFuel(fuel + f);
    }

    /** 손에 든 연료를 최대한 넣는다. 넣은 개수 */
    public int insertFrom(ItemStack hand) {
        ItemStack rest = handler.insertItem(0, hand.copy(), false);
        int used = hand.getCount() - rest.getCount();
        hand.shrink(used);
        return used;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putInt("Fuel", fuel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        fuel = tag.getInt("Fuel");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        return saveWithoutMetadata(reg);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
