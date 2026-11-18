package com.tycooncraft.devtest;

import com.tycooncraft.gun.GunCrafting;
import com.tycooncraft.gun.GunWorkbenchMenu;
import com.tycooncraft.machine.MachineType;
import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/** v0.18: 총기·탄약은 총기 제작대에서만 */
public final class GunWorkbenchTest {
    static void fill(GunWorkbenchMenu m, Item... items) {
        for (int i = 0; i < 9; i++) m.grid.setItem(i, i < items.length ? new ItemStack(items[i]) : ItemStack.EMPTY);
    }

    static int count(ServerPlayer sp, Item item) {
        int n = 0;
        for (ItemStack s : sp.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    static void run(ServerPlayer sp) {
        var sl = sp.serverLevel();
        sp.getInventory().clearContent();
        BlockPos pos = sp.blockPosition().relative(sp.getDirection(), 2);
        sl.setBlockAndUpdate(pos, ModBlocks.GUN_WORKBENCH.get().defaultBlockState());
        sp.openMenu(ModBlocks.GUN_WORKBENCH.get().defaultBlockState().getMenuProvider(sl, pos));
        if (!(sp.containerMenu instanceof GunWorkbenchMenu m)) {
            System.out.println("[AUTOTEST] v18 workbench menu NOT open");
            return;
        }
        Item rec = ModItems.GUN_RECEIVER.get(), bar = ModItems.GUN_BARREL.get(), stock = ModItems.GUN_STOCK.get();
        fill(m, Items.SPYGLASS, rec, bar, stock, bar, ModItems.ADVANCED_CIRCUIT.get(), Items.GLASS);
        boolean sniperShown = m.getSlot(0).getItem().is(ModItems.SNIPER.get());
        m.clicked(0, 0, ClickType.QUICK_MOVE, sp);
        boolean gridEmpty = true;
        for (int i = 0; i < 9; i++) gridEmpty &= m.grid.getItem(i).isEmpty();
        // 탄약 4번 연속 제작 (쉬프트 클릭)
        m.grid.setItem(0, new ItemStack(ModItems.COPPER_PLATE.get(), 4));
        m.grid.setItem(4, new ItemStack(Items.GUNPOWDER, 4));
        m.grid.setItem(8, new ItemStack(ModItems.IRON_ROD.get(), 4));
        m.clicked(0, 0, ClickType.QUICK_MOVE, sp);
        // 틀린 조합
        fill(m, rec, bar);
        boolean wrongEmpty = m.getSlot(0).getItem().isEmpty();
        System.out.println("[AUTOTEST] v18 workbench sniperShown=" + sniperShown + " sniper=" + count(sp, ModItems.SNIPER.get()) + " gridEmpty=" + gridEmpty
                + " lightAmmo=" + count(sp, ModItems.LIGHT_AMMO.get()) + " wrongEmpty=" + wrongEmpty + " recipes=" + GunCrafting.all().size());

        // 작업대·조립기로는 안 됨
        var rm = sl.getRecipeManager();
        boolean vanillaPistol = rm.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 1, List.of(new ItemStack(rec), new ItemStack(bar), new ItemStack(ModItems.IRON_PLATE.get()))), sl).isPresent();
        boolean vanillaAmmo = rm.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 1, List.of(new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.GUNPOWDER), new ItemStack(Items.IRON_NUGGET))), sl).isPresent();
        int machineGuns = 0;
        for (MachineType t : MachineType.values())
            for (var r : t.recipes()) if (GunCrafting.isGunOrAmmo(r.output())) machineGuns++;
        var wb = rm.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, List.of(
                new ItemStack(ModItems.IRON_PLATE.get()), new ItemStack(ModItems.IRON_PLATE.get()), new ItemStack(ModItems.IRON_PLATE.get()),
                new ItemStack(ModItems.IRON_GEAR.get()), new ItemStack(Items.CRAFTING_TABLE), new ItemStack(ModItems.IRON_GEAR.get()),
                new ItemStack(Items.OAK_LOG), new ItemStack(Items.SPRUCE_LOG), new ItemStack(Items.OAK_LOG))), sl);
        System.out.println("[AUTOTEST] v18 workbench-only vanillaPistol=" + vanillaPistol + " vanillaAmmo=" + vanillaAmmo + " machineGunRecipes=" + machineGuns
                + " benchRecipe=" + wb.map(h -> h.value().getResultItem(sl.registryAccess()).getItem() == ModItems.GUN_WORKBENCH.get().asItem()).orElse(false));
        // 부품도 제작대에서 (판자는 아무 나무나)
        fill(m, Items.BIRCH_PLANKS, Items.LEATHER, Items.SPRUCE_PLANKS, Items.OAK_PLANKS);
        boolean stockOk = m.getSlot(0).getItem().is(stock);
        fill(m, ModItems.STEEL_ROD.get());
        boolean barrelOk = m.getSlot(0).getItem().is(bar);
        fill(m, ModItems.STEEL_PLATE.get(), ModItems.MECHANISM.get(), ModItems.STEEL_PLATE.get(), ModItems.IRON_GEAR.get());
        boolean recOk = m.getSlot(0).getItem().is(rec);
        fill(m, ModItems.STEEL_PLATE.get(), ModItems.STEEL_ROD.get(), Items.WHITE_WOOL);
        boolean silOk = m.getSlot(0).getItem().is(ModItems.SILENCER.get());
        fill(m, ModItems.ALUMINUM_PLATE.get(), Items.GLASS_PANE, ModItems.STEEL_ROD.get(), Items.GLASS_PANE, ModItems.CIRCUIT.get(), ModItems.ALUMINUM_PLATE.get());
        boolean scopeOk = m.getSlot(0).getItem().is(ModItems.GUN_PART_ITEMS.get(com.tycooncraft.gun.GunPart.SCOPE).get());
        int assemblerParts = 0;
        for (var r : MachineType.ASSEMBLER.recipes()) if (r.output() instanceof com.tycooncraft.gun.GunPartItem || r.output() == ModItems.SILENCER_ADVANCED.get()) assemblerParts++;
        System.out.println("[AUTOTEST] v18 workbench gunParts scope=" + scopeOk + " assemblerParts=" + assemblerParts);
        long total = rm.getRecipes().stream().filter(h -> h.id().getNamespace().equals("tycooncraft")).count();
        long known = rm.getRecipes().stream().filter(h -> h.id().getNamespace().equals("tycooncraft") && sp.getRecipeBook().contains(h)).count();
        System.out.println("[AUTOTEST] v18 workbench parts stock=" + stockOk + " barrel=" + barrelOk + " receiver=" + recOk + " silencer=" + silOk
                + " recipeBook=" + known + "/" + total);
        // 화면용: 소총 재료
        fill(m, rec, bar, bar, stock, ModItems.ADVANCED_CIRCUIT.get());
    }

    private GunWorkbenchTest() {}
}
