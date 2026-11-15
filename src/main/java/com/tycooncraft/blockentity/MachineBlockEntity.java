package com.tycooncraft.blockentity;

import com.tycooncraft.block.MachineBlock;
import com.tycooncraft.machine.MachineRecipe;
import com.tycooncraft.machine.MachineType;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 기계 공통 로직.
 * - 주변(옆·위)에 닿은 재료 아이템을 흡수해 보관함에 넣음
 * - 이 기계가 쓰지 않는 아이템은 그대로 앞으로 통과시킴 (섞인 라인 지원)
 * - 레시피 재료가 다 모이면 가공 → 앞으로 배출 (앞이 막히면 대기)
 */
public class MachineBlockEntity extends BlockEntity {
    private static final int ABSORB_INTERVAL = 5;
    private static final int ITEM_LIMIT = 128;
    private static final int TOTAL_LIMIT = 512;

    private final Map<Item, Integer> buffer = new LinkedHashMap<>();
    private ItemStack pendingOutput = ItemStack.EMPTY;
    private int progress;

    /** 슬롯 0: 넣기 = 재료 보관함, 빼기 = 대기 중인 완성품 */
    private final IItemHandler itemHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return pendingOutput; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return type().accepts(stack.getItem()); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !type().accepts(stack.getItem())) return stack;
            int n = room(stack.getItem(), stack.getCount());
            if (n <= 0) return stack;
            if (!simulate) add(stack.getItem(), n);
            return stack.copyWithCount(stack.getCount() - n);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (pendingOutput.isEmpty()) return ItemStack.EMPTY;
            ItemStack taken = pendingOutput.copyWithCount(Math.min(amount, pendingOutput.getCount()));
            if (!simulate) {
                pendingOutput.shrink(taken.getCount());
                setChanged();
            }
            return taken;
        }
    };

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE.get(), pos, state);
    }

    public MachineType type() {
        return ((MachineBlock) getBlockState().getBlock()).type();
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    private Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    private int total() {
        int t = 0;
        for (int v : buffer.values()) t += v;
        return t;
    }

    /** 이 아이템을 몇 개까지 더 넣을 수 있는지 */
    private int room(Item item, int wanted) {
        int free = Math.min(TOTAL_LIMIT - total(), ITEM_LIMIT - buffer.getOrDefault(item, 0));
        return Math.max(0, Math.min(free, wanted));
    }

    private void add(Item item, int n) {
        buffer.merge(item, n, Integer::sum);
        setChanged();
    }

    private MachineRecipe findRecipe() {
        for (MachineRecipe r : type().recipes()) {
            boolean ok = true;
            for (Map.Entry<Item, Integer> e : r.inputs().entrySet()) {
                if (buffer.getOrDefault(e.getKey(), 0) < e.getValue()) {
                    ok = false;
                    break;
                }
            }
            if (ok) return r;
        }
        return null;
    }

    public void serverTick() {
        if (level == null) return;

        if (level.getGameTime() % ABSORB_INTERVAL == 0) absorb();

        // 앞이 막혀 대기 중인 완성품 먼저 내보내기
        if (!pendingOutput.isEmpty()) {
            pendingOutput = ItemTransfer.output(level, worldPosition, facing(), pendingOutput);
            setChanged();
            if (!pendingOutput.isEmpty()) return;
        }

        MachineRecipe recipe = findRecipe();
        if (recipe == null) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        progress++;
        if (progress >= type().processTime()) {
            progress = 0;
            for (Map.Entry<Item, Integer> e : recipe.inputs().entrySet()) {
                int left = buffer.getOrDefault(e.getKey(), 0) - e.getValue();
                if (left <= 0) buffer.remove(e.getKey());
                else buffer.put(e.getKey(), left);
            }
            pendingOutput = ItemTransfer.output(level, worldPosition, facing(), recipe.result());
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
            // 방금 배출한 완성품(앞쪽에 있는 것)은 건드리지 않음
            double dx = entity.getX() - (worldPosition.getX() + 0.5);
            double dz = entity.getZ() - (worldPosition.getZ() + 0.5);
            if (dx * out.getStepX() + dz * out.getStepZ() > 0.45) continue;

            ItemStack stack = entity.getItem();
            if (type().accepts(stack.getItem())) {
                int n = room(stack.getItem(), stack.getCount());
                if (n <= 0) continue;
                add(stack.getItem(), n);
                if (n >= stack.getCount()) entity.discard();
                else entity.setItem(stack.copyWithCount(stack.getCount() - n));
            } else {
                ItemTransfer.moveEntity(level, worldPosition, out, entity);
            }
        }
    }

    public void insertFromHand(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Component machineName = Component.translatable(type().translationKey());
        if (!type().accepts(stack.getItem())) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.machine.not_accepted",
                    stack.getHoverName(), machineName), true);
            return;
        }
        int n = room(stack.getItem(), stack.getCount());
        if (n <= 0) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.machine.full", machineName), true);
            return;
        }
        add(stack.getItem(), n);
        player.displayClientMessage(Component.translatable("message.tycooncraft.machine.inserted",
                machineName, stack.getHoverName(), n), true);
        stack.shrink(n);
    }

    public void showInfo(Player player) {
        MutableComponent contents = Component.empty();
        if (buffer.isEmpty()) {
            contents.append(Component.translatable("message.tycooncraft.machine.empty"));
        } else {
            boolean first = true;
            for (Map.Entry<Item, Integer> e : buffer.entrySet()) {
                if (!first) contents.append(", ");
                contents.append(e.getKey().getDescription()).append(" x" + e.getValue());
                first = false;
            }
        }
        player.sendSystemMessage(Component.literal("[").append(Component.translatable(type().translationKey()))
                .append("] ").append(Component.translatable("message.tycooncraft.machine.contents", contents))
                .withStyle(ChatFormatting.GOLD));
        List<MachineRecipe> recipes = type().recipes();
        for (int i = 0; i < Math.min(6, recipes.size()); i++) {
            player.sendSystemMessage(Component.literal(" · ").append(recipes.get(i).describe()).withStyle(ChatFormatting.GRAY));
        }
        if (recipes.size() > 6) {
            player.sendSystemMessage(Component.translatable("message.tycooncraft.machine.more", recipes.size() - 6)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public void dropContents() {
        if (level == null) return;
        for (Map.Entry<Item, Integer> e : buffer.entrySet()) {
            int n = e.getValue();
            while (n > 0) {
                int take = Math.min(n, e.getKey().getDefaultMaxStackSize());
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), new ItemStack(e.getKey(), take));
                n -= take;
            }
        }
        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), pendingOutput);
        buffer.clear();
        pendingOutput = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        buffer.forEach((item, count) -> {
            CompoundTag e = new CompoundTag();
            e.putString("id", BuiltInRegistries.ITEM.getKey(item).toString());
            e.putInt("count", count);
            list.add(e);
        });
        tag.put("buffer", list);
        if (!pendingOutput.isEmpty()) tag.put("pending", pendingOutput.save(registries));
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.clear();
        ListTag list = tag.getList("buffer", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(e.getString("id"));
            if (id == null) continue;
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) buffer.put(item, e.getInt("count"));
        }
        pendingOutput = ItemStack.parseOptional(registries, tag.getCompound("pending"));
        progress = tag.getInt("progress");
    }
}
