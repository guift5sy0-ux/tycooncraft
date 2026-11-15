package com.tycooncraft.gun;

import com.tycooncraft.registry.ModComponents;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 제작대: 총 + 소음기 → 소음기 단 총 (탄·인챈트 유지).
 * 소음기 단 총만 놓으면 → 소음기 없는 총 + 소음기 돌려받음.
 */
public class SilencerRecipe extends CustomRecipe {

    public SilencerRecipe(CraftingBookCategory category) {
        super(category);
    }

    private record Found(ItemStack gun, int gunSlot, int silencers, int others) {}

    private static Found scan(CraftingInput input) {
        ItemStack gun = ItemStack.EMPTY;
        int gunSlot = -1, sil = 0, others = 0, guns = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof GunItem) {
                gun = s;
                gunSlot = i;
                guns++;
            } else if (s.is(ModItems.SILENCER.get())) {
                sil++;
            } else {
                others++;
            }
        }
        return guns == 1 ? new Found(gun, gunSlot, sil, others) : new Found(ItemStack.EMPTY, -1, sil, others + guns);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        Found f = scan(input);
        if (f.gun().isEmpty() || f.others() > 0) return false;
        boolean silenced = GunItem.silenced(f.gun());
        return (f.silencers() == 1 && !silenced) || (f.silencers() == 0 && silenced);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Found f = scan(input);
        ItemStack out = f.gun().copyWithCount(1);
        out.set(ModComponents.SILENCED.get(), f.silencers() == 1);
        return out;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> rem = super.getRemainingItems(input);
        Found f = scan(input);
        if (f.silencers() == 0 && f.gunSlot() >= 0 && GunItem.silenced(f.gun())) {
            rem.set(f.gunSlot(), new ItemStack(ModItems.SILENCER.get()));
        }
        return rem;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SILENCER.get();
    }
}
