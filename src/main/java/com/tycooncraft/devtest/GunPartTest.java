package com.tycooncraft.devtest;

import com.tycooncraft.gun.GunEvents;
import com.tycooncraft.gun.GunEnchants;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.gun.GunPart;
import com.tycooncraft.gun.GunParts;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/** v0.17: 총기 파츠 — 수치, 제작대 장착/떼기, 인챈트 → 파츠 자동 변환 */
public final class GunPartTest {
    static void tick(ServerLevel level, int t) {
        if (t != 2300) return;
        GunItem rifle = (GunItem) ModItems.RIFLE.get();
        ItemStack g = new ItemStack(rifle);
        String before = String.format("mag=%d dmg=%.1f reload=%d delay=%d scoped=%s", rifle.magazine(g), rifle.damage(g), rifle.reloadTicks(g), rifle.fireDelay(g), rifle.isScoped(g));
        for (GunPart p : List.of(GunPart.HEAVY_BARREL, GunPart.SCOPE, GunPart.EXTENDED_MAG, GunPart.RAPID_TRIGGER, GunPart.VERTICAL_GRIP)) GunParts.install(g, p);
        String after = String.format("mag=%d dmg=%.1f reload=%d delay=%d scoped=%s spread=%.2f recoil=%.2f", rifle.magazine(g), rifle.damage(g), rifle.reloadTicks(g), rifle.fireDelay(g),
                rifle.isScoped(g), GunParts.spreadMult(g), GunParts.recoilMult(g));
        GunPart swapped = GunParts.install(g, GunPart.MAG_COUPLER);    // 같은 탄창 부위 → 확장 탄창이 빠져야 함
        System.out.println("[AUTOTEST] v17 parts before{" + before + "} after{" + after + "} swappedOut=" + swapped + " installed=" + GunParts.installed(g));

        // 제작대
        var rm = level.getRecipeManager();
        ItemStack pistol = new ItemStack(ModItems.PISTOL.get());
        ItemStack dot = new ItemStack(ModItems.GUN_PART_ITEMS.get(GunPart.RED_DOT).get());
        CraftingInput in = CraftingInput.of(2, 1, List.of(pistol, dot));
        var r = rm.getRecipeFor(RecipeType.CRAFTING, in, level);
        ItemStack out = r.map(h -> h.value().assemble(in, level.registryAccess())).orElse(ItemStack.EMPTY);
        boolean attached = GunParts.has(out, GunPart.RED_DOT);
        CraftingInput in2 = CraftingInput.of(1, 1, List.of(out));
        var r2 = rm.getRecipeFor(RecipeType.CRAFTING, in2, level);
        ItemStack out2 = r2.map(h -> h.value().assemble(in2, level.registryAccess())).orElse(ItemStack.EMPTY);
        NonNullList<ItemStack> rem = r2.map(h -> h.value().getRemainingItems(in2)).orElse(NonNullList.create());
        boolean detached = !out2.isEmpty() && !GunParts.has(out2, GunPart.RED_DOT) && !rem.isEmpty() && rem.get(0).is(ModItems.GUN_PART_ITEMS.get(GunPart.RED_DOT).get());
        // 맞지 않는 파츠(권총 + 조준경) 거부
        CraftingInput bad = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.PISTOL.get()), new ItemStack(ModItems.GUN_PART_ITEMS.get(GunPart.SCOPE).get())));
        boolean refused = rm.getRecipeFor(RecipeType.CRAFTING, bad, level).isEmpty();
        System.out.println("[AUTOTEST] v17 crafting attached=" + attached + " detached=" + detached + " pistolScopeRefused=" + refused);

        // 인챈트 → 파츠
        var reg = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ItemStack old = new ItemStack(rifle);
        old.enchant(reg.getHolderOrThrow(GunEnchants.FIREPOWER), 3);
        old.enchant(reg.getHolderOrThrow(GunEnchants.EXTENDED_MAG), 2);
        old.enchant(reg.getHolderOrThrow(GunEnchants.QUICK_RELOAD), 1);
        var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        fp.getInventory().clearContent();
        GunEvents.migrate(fp, old);
        int extra = 0;
        for (int i = 0; i < fp.getInventory().getContainerSize(); i++) if (fp.getInventory().getItem(i).getItem() instanceof com.tycooncraft.gun.GunPartItem) extra++;
        System.out.println("[AUTOTEST] v17 migrate installed=" + GunParts.installed(old) + " enchantsLeft=" + old.getEnchantments().size() + " extraPartsInBag=" + extra);
    }

    private GunPartTest() {}
}
