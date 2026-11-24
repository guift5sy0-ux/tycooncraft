package com.tycooncraft.automation;

import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/** v0.23.0 창고: 한 블록에 54칸 (큰 상자 크기). 로봇 팔·컨베이어·호퍼로 넣고 뺄 수 있다 */
public class WarehouseBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SIZE = 54;
    public final SimpleContainer inv = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            WarehouseBlockEntity.this.setChanged();
        }
    };
    public final IItemHandler handler = new InvWrapper(inv);

    public WarehouseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAREHOUSE.get(), pos, state);
    }

    public int total() {
        int n = 0;
        for (int i = 0; i < SIZE; i++) n += inv.getItem(i).getCount();
        return n;
    }

    public int usedSlots() {
        int n = 0;
        for (int i = 0; i < SIZE; i++) if (!inv.getItem(i).isEmpty()) n++;
        return n;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tycooncraft.warehouse");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory pi, Player p) {
        return ChestMenu.sixRows(id, pi, inv);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        ContainerHelper.saveAllItems(tag, inv.getItems(), reg);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        inv.clearContent();
        ContainerHelper.loadAllItems(tag, inv.getItems(), reg);
    }
}
