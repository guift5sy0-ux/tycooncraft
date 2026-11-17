package com.tycooncraft.gun;

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
 * 제작대: 총 + 파츠 1개 → 장착 (같은 부위에 있던 파츠는 제작대에 남음).
 * 총만 넣으면 → 마지막에 단 파츠 하나를 떼어 제작대에 남김.
 */
public class GunPartRecipe extends CustomRecipe {
    public GunPartRecipe(CraftingBookCategory category) {
        super(category);
    }

    private record Found(ItemStack gun, int gunSlot, GunPart part, int partSlot, int others) {}

    private static Found scan(CraftingInput in) {
        ItemStack gun = ItemStack.EMPTY;
        int gs = -1, ps = -1, others = 0;
        GunPart part = null;
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof GunItem && gun.isEmpty()) {
                gun = s;
                gs = i;
            } else if (s.getItem() instanceof GunPartItem pi && part == null) {
                part = pi.part;
                ps = i;
            } else others++;
        }
        return new Found(gun, gs, part, ps, others);
    }

    @Override
    public boolean matches(CraftingInput in, Level level) {
        Found f = scan(in);
        if (f.gun.isEmpty() || f.others > 0) return false;
        if (f.part != null) return GunParts.supported(f.gun).contains(f.part) && !GunParts.has(f.gun, f.part);
        return !GunParts.installed(f.gun).isEmpty() && !GunItem.silenced(f.gun);   // 소음기부터 떼고(소음기 레시피) 나머지 파츠
    }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider reg) {
        Found f = scan(in);
        ItemStack out = f.gun.copyWithCount(1);
        if (f.part != null) GunParts.install(out, f.part);
        else GunParts.removeLast(out);
        clampAmmo(out);
        return out;
    }

    /** 확장 탄창을 떼면 넘치는 탄은 버리지 않고 줄여만 둠 (재장전 때 돌아오지 않음 — 단순화) */
    private static void clampAmmo(ItemStack gun) {
        if (gun.getItem() instanceof GunItem g && GunItem.ammo(gun) > g.magazine(gun)) GunItem.setAmmo(gun, g.magazine(gun));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput in) {
        NonNullList<ItemStack> rem = super.getRemainingItems(in);
        Found f = scan(in);
        if (f.gun.isEmpty()) return rem;
        ItemStack copy = f.gun.copy();
        if (f.part != null) {
            GunPart old = GunParts.install(copy, f.part);
            if (old != null) rem.set(f.partSlot, new ItemStack(GunParts.item(old)));
        } else {
            GunPart removed = GunParts.removeLast(copy);
            if (removed != null) rem.set(f.gunSlot, new ItemStack(GunParts.item(removed)));
        }
        return rem;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GUN_PART.get();
    }
}
