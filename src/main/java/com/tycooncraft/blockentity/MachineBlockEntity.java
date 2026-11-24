package com.tycooncraft.blockentity;

import com.tycooncraft.block.MachineBlock;
import com.tycooncraft.machine.MachineMenu;
import com.tycooncraft.machine.MachineRecipe;
import com.tycooncraft.machine.MachineType;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 기계 공통 로직 (재료 칸 6 + 결과 칸 1).
 * - 옆·위에 닿은 재료 아이템을 흡수, 이 기계가 안 쓰는 아이템은 앞으로 통과
 * - 레시피 재료가 모이면 가공 → 결과 칸
 * - 앞이 컨베이어·인벤토리면 결과물을 자동으로 내보냄 (아니면 결과 칸에 쌓임)
 */
public class MachineBlockEntity extends BlockEntity implements com.tycooncraft.power.PowerNode, MenuProvider {
    public static final int INPUT_SLOTS = 6;
    private static final int ABSORB_INTERVAL = 5;

    private final ItemStackHandler inputs = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return type().accepts(stack.getItem());
        }
    };

    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int progress;
    /** v0.21.0 동력: 일할 거리가 있는지(수요), 받은 동력 비율을 쌓아 진행도로 바꿈 */
    private boolean wantsPower;
    private float powerAcc;
    private int powerPct;

    /** GUI 동기화용: 0 = 진행도, 1 = 필요 시간 */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> type().processTime();
                case 2 -> powerPct;
                default -> type().power();
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 2) powerPct = value;
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    /** 외부(호퍼·다른 기계) 연결: 0~5 재료 넣기, 6 결과 빼기 */
    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return INPUT_SLOTS + 1; }
        @Override public ItemStack getStackInSlot(int slot) { return slot < INPUT_SLOTS ? inputs.getStackInSlot(slot) : output.getStackInSlot(0); }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot < INPUT_SLOTS && inputs.isItemValid(slot, stack); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= INPUT_SLOTS) return stack;
            return inputs.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < INPUT_SLOTS) return ItemStack.EMPTY;
            return output.extractItem(0, amount, simulate);
        }
    };

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE.get(), pos, state);
    }

    public MachineType type() {
        return ((MachineBlock) getBlockState().getBlock()).type();
    }

    public IItemHandler getItemHandler() {
        return automation;
    }

    public ItemStackHandler inputs() {
        return inputs;
    }

    public ItemStackHandler output() {
        return output;
    }

    public ContainerData data() {
        return data;
    }

    private Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    private int count(Item item) {
        int n = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack s = inputs.getStackInSlot(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    private void consume(Item item, int n) {
        for (int i = 0; i < INPUT_SLOTS && n > 0; i++) {
            ItemStack s = inputs.getStackInSlot(i);
            if (s.is(item)) {
                int take = Math.min(n, s.getCount());
                inputs.extractItem(i, take, false);
                n -= take;
            }
        }
    }

    private MachineRecipe findRecipe() {
        for (MachineRecipe r : type().recipes()) {
            boolean ok = true;
            for (Map.Entry<Item, Integer> e : r.inputs().entrySet()) {
                if (count(e.getKey()) < e.getValue()) {
                    ok = false;
                    break;
                }
            }
            if (ok && output.insertItem(0, r.result(), true).isEmpty()) return r;
        }
        return null;
    }

    @Override
    public boolean consumer() {
        return true;
    }

    @Override
    public int demand() {
        return wantsPower ? type().power() : 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) com.tycooncraft.power.PowerGrid.add(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) com.tycooncraft.power.PowerGrid.remove(level, worldPosition);
    }

    public void serverTick() {
        if (level == null) return;
        if (level.getGameTime() % ABSORB_INTERVAL == 0) absorb();

        // 결과물 자동 배출 (앞이 컨베이어·인벤토리일 때만)
        ItemStack out = output.getStackInSlot(0);
        if (!out.isEmpty()) {
            ItemStack rest = ItemTransfer.output(level, worldPosition, facing(), out.copy(), false);
            if (rest.getCount() != out.getCount()) output.setStackInSlot(0, rest);
        }

        MachineRecipe recipe = findRecipe();
        float ratio = com.tycooncraft.power.PowerGrid.ratio(level, worldPosition);
        powerPct = Math.round(ratio * 100);
        wantsPower = recipe != null;
        if (recipe == null) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        // 동력이 없으면 멈춤, 모자라면 그만큼 느리게
        powerAcc += ratio;
        if (powerAcc < 1f) return;
        powerAcc -= 1f;
        progress++;
        if (progress >= type().processTime()) {
            progress = 0;
            for (Map.Entry<Item, Integer> e : recipe.inputs().entrySet()) consume(e.getKey(), e.getValue());
            output.insertItem(0, recipe.result(), false);
            level.playSound(null, worldPosition, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.25f, 1.2f);
        }
        setChanged();
    }

    /** 주변 아이템 흡수 / 통과 */
    private void absorb() {
        AABB area = new AABB(worldPosition).inflate(0.25);
        Direction out = facing();
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive);
        for (ItemEntity entity : items) {
            double dx = entity.getX() - (worldPosition.getX() + 0.5);
            double dz = entity.getZ() - (worldPosition.getZ() + 0.5);
            if (dx * out.getStepX() + dz * out.getStepZ() > 0.45) continue; // 배출구 쪽은 무시

            ItemStack stack = entity.getItem();
            if (type().accepts(stack.getItem())) {
                ItemStack rest = ItemHandlerHelper.insertItemStacked(inputs, stack.copy(), false);
                if (rest.isEmpty()) entity.discard();
                else entity.setItem(rest);
            } else {
                ItemTransfer.moveEntity(level, worldPosition, out, entity, false);
            }
        }
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inputs.getStackInSlot(i));
        }
        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), output.getStackInSlot(0));
    }

    // ── GUI ──

    @Override
    public Component getDisplayName() {
        return Component.translatable(type().translationKey());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMenu(id, inventory, this);
    }

    // ── 저장 ──

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inputs", inputs.serializeNBT(registries));
        tag.put("output", output.serializeNBT(registries));
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inputs")) inputs.deserializeNBT(registries, tag.getCompound("inputs"));
        if (tag.contains("output")) output.deserializeNBT(registries, tag.getCompound("output"));
        progress = tag.getInt("progress");
    }
}
