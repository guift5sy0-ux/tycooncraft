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
        addSlot(new OutSlot(result, 0, RESULT_X, RESULT_Y));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) addSlot(new Slot(grid, c + r * 3, GRID_X + c * 18, GRID_Y + r * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, INV_X + c * 18, HOTBAR_Y));
    }

    public static final int GRID_X = 124, GRID_Y = 20, RESULT_X = 214, RESULT_Y = 38, INV_X = 110, INV_Y = 118, HOTBAR_Y = 178;

    /** 제작법을 고르면 보관함에서 재료를 꺼내 격자에 채운다 (max = 가능한 만큼 한꺼번에) */
    public boolean fill(Player p, int index, boolean max) {
        var all = GunCrafting.all();
        if (index < 0 || index >= all.size()) return false;
        GunCrafting.Recipe rc = all.get(index);
        var inv = p.getInventory();
        for (int i = 0; i < grid.getContainerSize(); i++) {   // 격자에 있던 것은 먼저 돌려줌
            ItemStack s = grid.removeItemNoUpdate(i);
            if (!s.isEmpty()) inv.placeItemBackInInventory(s);
        }
        java.util.Map<net.minecraft.world.item.Item, Integer> have = new java.util.HashMap<>();
        for (ItemStack s : inv.items) if (!s.isEmpty()) have.merge(GunCrafting.key(s), s.getCount(), Integer::sum);
        int sets = GunCrafting.sets(rc, have);
        if (sets <= 0) {
            slotsChanged(grid);
            return false;
        }
        if (!max) sets = 1;
        int slot = 0;
        for (var e : rc.needs().entrySet()) sets = Math.min(sets, new ItemStack(e.getKey()).getMaxStackSize());
        for (var e : rc.needs().entrySet()) {
            for (int k = 0; k < e.getValue(); k++) {
                ItemStack taken = take(inv, e.getKey(), sets);
                if (taken.isEmpty()) break;
                grid.setItem(slot++, taken);
            }
        }
        slotsChanged(grid);
        return true;
    }

    /** 같은 종류로 count 개를 꺼냄 (판자는 가장 많은 종류부터) */
    private static ItemStack take(Inventory inv, net.minecraft.world.item.Item key, int count) {
        net.minecraft.world.item.Item pick = key;
        if (key == net.minecraft.world.item.Items.OAK_PLANKS) {
            java.util.Map<net.minecraft.world.item.Item, Integer> m = new java.util.HashMap<>();
            for (ItemStack s : inv.items) if (s.is(net.minecraft.tags.ItemTags.PLANKS)) m.merge(s.getItem(), s.getCount(), Integer::sum);
            pick = m.entrySet().stream().filter(e -> e.getValue() >= count).map(java.util.Map.Entry::getKey).findFirst()
                    .orElse(m.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(key));
        }
        ItemStack out = ItemStack.EMPTY;
        for (int i = 0; i < inv.items.size() && out.getCount() < count; i++) {
            ItemStack s = inv.items.get(i);
            if (!s.is(pick)) continue;
            if (!out.isEmpty() && !ItemStack.isSameItemSameComponents(out, s)) continue;
            int n = Math.min(count - out.getCount(), s.getCount());
            if (out.isEmpty()) out = s.copyWithCount(n);
            else out.grow(n);
            s.shrink(n);
        }
        return out;
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
            if (!p.level().isClientSide)
                p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.3f);
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
