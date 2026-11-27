package com.tycooncraft.ship;

import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** v0.34 드론 편대 격납고: 주인 + 보유 드론 수 (0~8). 운송기에 실리면 이 데이터가 셀에 그대로 옮겨감 */
public class DroneBayBlockEntity extends BlockEntity {
    public static final int MAX = 8, START = 6;
    private int drones = START;
    private UUID owner;

    public DroneBayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_BAY.get(), pos, state);
    }

    public int drones() {
        return drones;
    }

    public void setDrones(int n) {
        drones = Math.max(0, Math.min(MAX, n));
        setChanged();
    }

    public void setOwner(UUID u) {
        owner = u;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.saveAdditional(t, reg);
        t.putInt("Drones", drones);
        if (owner != null) t.putUUID("Owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.loadAdditional(t, reg);
        drones = t.contains("Drones") ? t.getInt("Drones") : START;
        owner = t.hasUUID("Owner") ? t.getUUID("Owner") : null;
    }

    /** 부수면 드론 수가 아이템에 남음 (다시 놓아도 공짜로 채워지지 않게) */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder b) {
        super.collectImplicitComponents(b);
        CompoundTag t = new CompoundTag();
        t.putInt("Drones", drones);
        b.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput in) {
        super.applyImplicitComponents(in);
        CustomData d = in.get(DataComponents.CUSTOM_DATA);
        if (d != null && d.contains("Drones")) drones = d.copyTag().getInt("Drones");
    }
}
