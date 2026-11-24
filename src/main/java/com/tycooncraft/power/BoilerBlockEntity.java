package com.tycooncraft.power;

import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class BoilerBlockEntity extends BlockEntity {
    public static final int TANK = 4000, MAX_STEAM = 400, STEAM_PER_TICK = 2;
    public int water = TANK, steam, burn, burnMax;
    public static int coalBurned;

    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isFuel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER.get(), pos, state);
    }

    public static boolean isFuel(ItemStack st) {
        return !st.isEmpty() && st.getBurnTime(RecipeType.SMELTING) > 0 && !st.is(net.minecraft.world.item.Items.LAVA_BUCKET);
    }

    public IItemHandler fuel() {
        return fuel;
    }

    /** 증기기관이 증기를 가져감 */
    public boolean takeSteam(int n) {
        if (steam < n) return false;
        steam -= n;
        return true;
    }

    public void serverTick() {
        if (level == null) return;
        // 물 원천에 닿아 있으면 계속 보충
        if (water < TANK && level.getGameTime() % 5 == 0) {
            for (Direction d : Direction.values()) {
                if (d == Direction.UP) continue;
                var fs = level.getFluidState(worldPosition.relative(d));
                if (fs.is(FluidTags.WATER) && fs.isSource()) {
                    water = Math.min(TANK, water + 100);
                    break;
                }
            }
        }
        boolean lit = burn > 0;
        if (burn > 0) {
            burn--;
            if (water > 0 && steam < MAX_STEAM) {
                steam = Math.min(MAX_STEAM, steam + STEAM_PER_TICK);
                water--;
            }
        } else if (steam < MAX_STEAM / 2 && water > 0) {
            ItemStack f = fuel.getStackInSlot(0);
            if (!f.isEmpty()) {
                // 증기기관용으로 연료가 화로보다 빨리 탐 (석탄 1개 = 800틱)
                burnMax = burn = Math.max(20, f.getBurnTime(RecipeType.SMELTING) / 2);
                ItemStack rem = f.getCraftingRemainingItem();
                fuel.extractItem(0, 1, false);
                if (!rem.isEmpty() && fuel.getStackInSlot(0).isEmpty()) fuel.setStackInSlot(0, rem);
                coalBurned++;
            }
        }
        boolean nowLit = burn > 0;
        if (nowLit != getBlockState().getValue(BoilerBlock.LIT)) level.setBlock(worldPosition, getBlockState().setValue(BoilerBlock.LIT, nowLit), 3);
        if (lit || nowLit) setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.put("Fuel", fuel.serializeNBT(reg));
        tag.putInt("Water", water);
        tag.putInt("Steam", steam);
        tag.putInt("Burn", burn);
        tag.putInt("BurnMax", burnMax);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        fuel.deserializeNBT(reg, tag.getCompound("Fuel"));
        water = tag.contains("Water") ? tag.getInt("Water") : TANK;
        steam = tag.getInt("Steam");
        burn = tag.getInt("Burn");
        burnMax = tag.getInt("BurnMax");
    }
}
