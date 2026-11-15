package com.tycooncraft.blockentity;

import com.tycooncraft.economy.Coins;
import com.tycooncraft.economy.MarketData;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;

public class SellStationBlockEntity extends BlockEntity {
    private static final int ABSORB_INTERVAL = 10;

    /** 판매 대금 (소수점 이하는 다음 판매에 누적) */
    private double balance;

    private final IItemHandler itemHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return MarketData.canSell(stack); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!MarketData.canSell(stack)) return stack;
            if (!simulate) accept(stack.copy());
            return ItemStack.EMPTY;
        }
    };

    public SellStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SELL_STATION.get(), pos, state);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    private double accept(ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) return 0;
        double earned = MarketData.get(serverLevel.getServer()).sell(stack);
        if (earned < 0) return 0;
        balance += earned;
        setChanged();
        return earned;
    }

    public void serverTick() {
        if (level == null || level.getGameTime() % ABSORB_INTERVAL != 0) return;
        AABB area = new AABB(worldPosition).inflate(0.3);
        boolean sold = false;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && MarketData.canSell(e.getItem()))) {
            accept(item.getItem());
            item.discard();
            sold = true;
        }
        if (sold) level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.3f, 1.4f);
    }

    public void sellFromHand(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!MarketData.canSell(stack)) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.not_sellable", stack.getHoverName()), true);
            return;
        }
        ItemStack selling = stack.copy();
        double earned = accept(selling);
        player.setItemInHand(hand, ItemStack.EMPTY);
        player.displayClientMessage(Component.translatable("message.tycooncraft.sold_station",
                selling.getHoverName(), selling.getCount(), Coins.format(earned)), true);
        level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5f, 1.2f);
    }

    public void withdraw(Player player) {
        long whole = (long) Math.floor(balance + 1e-9);
        if (whole <= 0) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.empty"), true);
            return;
        }
        balance -= whole;
        setChanged();
        Coins.give(player, whole);
        player.displayClientMessage(Component.translatable("message.tycooncraft.withdraw", Coins.format(whole)), true);
        level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);
    }

    public void showBalance(Player player) {
        player.displayClientMessage(Component.translatable("message.tycooncraft.balance",
                Coins.format((long) Math.floor(balance + 1e-9))), true);
    }

    public void dropBalance() {
        if (level == null) return;
        for (ItemStack stack : Coins.toStacks((long) Math.floor(balance + 1e-9))) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        }
        balance = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("balance", balance);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        balance = tag.getDouble("balance");
    }
}
