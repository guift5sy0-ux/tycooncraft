package com.tycooncraft.gun;

import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModMenus;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

/** 총기 제작대 3x3 격자 (작업대와 같은 배치) */
public class GunWorkbenchMenu extends AbstractContainerMenu {
    public final CraftingContainer grid = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    public GunWorkbenchMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public GunWorkbenchMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModMenus.GUN_WORKBENCH.get(), id);
        this.access = access;
        this.player = inv.player;
        addSlot(new OutSlot(result, 0, 124, 35));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) addSlot(new Slot(grid, c + r * 3, 30 + c * 18, 17 + r * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
    }

    private void update() {
        GunCrafting.Recipe rc = GunCrafting.match(grid);
        ItemStack out = rc == null ? ItemStack.EMPTY : rc.result().copy();
        result.setItem(0, out);
        setRemoteSlot(0, out);
        if (player instanceof ServerPlayer sp) sp.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, out));
    }

    @Override
    public void slotsChanged(Container c) {
        if (c == grid) access.execute((level, pos) -> update());
    }

    /** 결과를 가져가면 칸마다 재료 1개씩 소모 */
    class OutSlot extends Slot {
        OutSlot(Container c, int i, int x, int y) {
            super(c, i, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player p, ItemStack stack) {
            stack.onCraftedBy(p.level(), p, stack.getCount());
            for (int i = 0; i < grid.getContainerSize(); i++) {
                ItemStack in = grid.getItem(i);
                if (in.isEmpty()) continue;
                ItemStack rem = in.getCraftingRemainingItem();
                grid.removeItem(i, 1);
                if (!rem.isEmpty()) {
                    if (grid.getItem(i).isEmpty()) grid.setItem(i, rem);
                    else if (!p.getInventory().add(rem)) p.drop(rem, false);
                }
            }
            slotsChanged(grid);
            super.onTake(p, stack);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack s = slot.getItem();
            ret = s.copy();
            if (index == 0) {
                if (!moveItemStackTo(s, 10, 46, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(s, ret);
            } else if (index >= 10) {
                if (!moveItemStackTo(s, 1, 10, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(s, 10, 46, false)) return ItemStack.EMPTY;
            if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (s.getCount() == ret.getCount()) return ItemStack.EMPTY;
            slot.onTake(p, s);
            if (index == 0) p.drop(s, false);
        }
        return ret;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        access.execute((level, pos) -> clearContainer(p, grid));
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, ModBlocks.GUN_WORKBENCH.get());
    }
}
