package com.tycooncraft.machine;

import com.tycooncraft.blockentity.MachineBlockEntity;
import com.tycooncraft.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** 기계 GUI: 재료 6칸 + 결과 1칸 + 플레이어 인벤토리 */
public class MachineMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 30, INPUT_Y = 26, OUTPUT_X = 124, OUTPUT_Y = 35;
    private static final int MACHINE_SLOTS = MachineBlockEntity.INPUT_SLOTS + 1;

    private final MachineBlockEntity machine;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** 클라이언트 쪽 (서버가 보낸 블록 위치로 기계를 찾음) */
    public MachineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (MachineBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public MachineMenu(int id, Inventory inventory, MachineBlockEntity machine) {
        super(ModMenus.MACHINE.get(), id);
        this.machine = machine;
        this.data = machine.data();
        this.access = ContainerLevelAccess.create(inventory.player.level(), machine.getBlockPos());

        for (int i = 0; i < MachineBlockEntity.INPUT_SLOTS; i++) {
            addSlot(new SlotItemHandler(machine.inputs(), i, INPUT_X + 18 * (i % 3), INPUT_Y + 18 * (i / 3)));
        }
        addSlot(new SlotItemHandler(machine.output(), 0, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));

        addDataSlots(data);
    }

    public MachineType type() {
        return machine.type();
    }

    public BlockPos pos() {
        return machine.getBlockPos();
    }

    /** 진행 막대 길이 (0 ~ width) */
    /** 받는 동력 비율 % */
    public int powerPct() {
        return data.get(2);
    }

    /** 필요한 동력 P/t */
    public int powerNeed() {
        return data.get(3);
    }

    public int progressScaled(int width) {
        int max = data.get(1);
        return max <= 0 ? 0 : data.get(0) * width / max;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int playerStart = MACHINE_SLOTS, playerEnd = slots.size();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            if (!machine.type().accepts(stack.getItem())) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, 0, MachineBlockEntity.INPUT_SLOTS, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, machine.getBlockState().getBlock());
    }
}
