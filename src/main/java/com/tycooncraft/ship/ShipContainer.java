package com.tycooncraft.ship;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 운송기에 실린 상자·통·호퍼의 내용물 창: 셀의 블록엔티티 태그("Items")를 직접 읽고 쓴다 */
public class ShipContainer extends SimpleContainer {
    final ShipEntity ship;
    final long key;
    final CompoundTag tag;

    ShipContainer(ShipEntity ship, long key, CompoundTag tag, int size) {
        super(size);
        this.ship = ship;
        this.key = key;
        this.tag = tag;
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, ship.registryAccess());
        for (int i = 0; i < size; i++) setItem(i, items.get(i));
    }

    @Override
    public void setChanged() {
        super.setChanged();
        NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < getContainerSize(); i++) items.set(i, getItem(i));
        tag.remove("Items");
        ContainerHelper.saveAllItems(tag, items, ship.registryAccess());
    }

    @Override
    public boolean stillValid(Player player) {
        return ship.isAlive() && ship.hasCell(key) && player.distanceToSqr(ship.cellCenterByKey(key)) < 64;
    }

    private int viewers;

    @Override
    public void startOpen(Player player) {
        super.startOpen(player);
        viewers++;
    }

    @Override
    public void stopOpen(Player player) {
        super.stopOpen(player);
        if (--viewers <= 0) ship.containerClosed(this);
    }
}
