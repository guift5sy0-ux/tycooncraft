package com.tycooncraft.blackmarket;

import com.tycooncraft.economy.Coins;
import com.tycooncraft.registry.ModBlockEntities;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** v0.29 갱단 금고 상자: 27칸. 잠김 상태 + 마지막으로 연 시각. 하루(24000틱) 뒤 다시 채워지고 잠김 */
public class LockedChestBlockEntity extends BaseContainerBlockEntity {
    public static final long RESTOCK = 24_000;
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    boolean locked = true;
    long openedAt = -1;
    int tier;   // 0 보통, 1 변종 은신처(더 좋음)

    public LockedChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOCKED_CHEST.get(), pos, state);
    }

    /** 전리품 채우기 */
    public void restock(RandomSource r, int tier) {
        this.tier = tier;
        Collections.fill(items, ItemStack.EMPTY);
        List<ItemStack> loot = new ArrayList<>();
        loot.addAll(Coins.toStacks((tier == 1 ? 12_000 : 4_000) + r.nextInt(tier == 1 ? 30_000 : 12_000)));
        Item[] ammo = {ModItems.LIGHT_AMMO.get(), ModItems.HEAVY_AMMO.get(), ModItems.SHOTGUN_SHELL.get()};
        loot.add(new ItemStack(ammo[r.nextInt(3)], 8 + r.nextInt(17)));
        if (r.nextFloat() < 0.6f) loot.add(new ItemStack(Items.GUNPOWDER, 4 + r.nextInt(12)));
        if (r.nextFloat() < 0.5f) loot.add(new ItemStack(ModItems.STEEL_INGOT.get(), 2 + r.nextInt(6)));
        if (r.nextFloat() < 0.4f) loot.add(new ItemStack(Items.GOLD_INGOT, 2 + r.nextInt(5)));
        if (r.nextFloat() < (tier == 1 ? 0.35f : 0.12f)) loot.add(new ItemStack(Items.DIAMOND, 1 + r.nextInt(3)));
        if (r.nextFloat() < (tier == 1 ? 0.25f : 0.08f)) loot.add(new ItemStack(ModItems.ROCKET.get(), 1 + r.nextInt(2)));
        if (r.nextFloat() < (tier == 1 ? 0.3f : 0.1f)) loot.add(new ItemStack(ModItems.UNDERCITY_KEY.get()));
        if (r.nextFloat() < (tier == 1 ? 0.12f : 0.03f)) loot.add(new ItemStack(r.nextBoolean() ? ModItems.RIFLE.get() : ModItems.SHOTGUN.get()));
        if (r.nextFloat() < 0.15f) loot.add(new ItemStack(ModItems.BLACK_MARKET_PASS.get()));
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 27; i++) slots.add(i);
        Collections.shuffle(slots, new java.util.Random(r.nextLong()));
        for (int i = 0; i < loot.size() && i < 27; i++) items.set(slots.get(i), loot.get(i));
        locked = true;
        openedAt = -1;
        setChanged();
    }

    public boolean locked() {
        return locked;
    }

    public int filled() {
        int n = 0;
        for (ItemStack s : items) if (!s.isEmpty()) n++;
        return n;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.tycooncraft.locked_chest");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        items = list;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return ChestMenu.threeRows(id, inv, this);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.loadAdditional(t, r);
        items = NonNullList.withSize(27, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(t, items, r);
        locked = !t.contains("Locked") || t.getBoolean("Locked");
        openedAt = t.getLong("OpenedAt");
        tier = t.getInt("Tier");
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r);
        ContainerHelper.saveAllItems(t, items, r);
        t.putBoolean("Locked", locked);
        t.putLong("OpenedAt", openedAt);
        t.putInt("Tier", tier);
    }
}
