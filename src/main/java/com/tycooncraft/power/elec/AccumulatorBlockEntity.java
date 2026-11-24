package com.tycooncraft.power.elec;

import com.tycooncraft.power.PowerStorage;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** 축전지: 남는 전기를 200,000 ⚡까지 모아 두고 모자랄 때 틱당 128까지 내보낸다 */
public class AccumulatorBlockEntity extends GridBlockEntity implements PowerStorage {
    public static final int CAPACITY = 200_000, RATE = 128;
    public int stored;

    public AccumulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ACCUMULATOR.get(), pos, state);
    }

    @Override
    public int chargeRoom() {
        return Math.min(RATE, CAPACITY - stored);
    }

    @Override
    public int dischargeable() {
        return Math.min(RATE, stored);
    }

    @Override
    public long stored() {
        return stored;
    }

    @Override
    public void charge(int amount) {
        stored += amount;
        setChanged();
    }

    @Override
    public void discharge(int amount) {
        stored -= amount;
        setChanged();
    }

    @Override
    public void serverTick() {
        if (level == null || level.getGameTime() % 10 != 0) return;
        int lv = stored <= 0 ? 0 : 1 + (int) Math.min(3, stored * 4L / CAPACITY);
        BlockState s = getBlockState();
        if (s.hasProperty(AccumulatorBlock.LEVEL) && s.getValue(AccumulatorBlock.LEVEL) != lv)
            level.setBlock(worldPosition, s.setValue(AccumulatorBlock.LEVEL, lv), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putInt("Stored", stored);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        stored = tag.getInt("Stored");
    }
}
