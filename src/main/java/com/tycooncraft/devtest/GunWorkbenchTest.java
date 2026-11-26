package com.tycooncraft.devtest;

import com.tycooncraft.gun.GunCrafting;
import com.tycooncraft.gun.GunWorkbenchMenu;
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
        int machineGuns = 0;   // v0.30: 기계 삭제
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
        fill(m, ModItems.IRON_PLATE.get(), Items.GLASS_PANE, ModItems.STEEL_ROD.get(), Items.GLASS_PANE, ModItems.CIRCUIT.get(), ModItems.IRON_PLATE.get());
        boolean scopeOk = m.getSlot(0).getItem().is(ModItems.GUN_PART_ITEMS.get(com.tycooncraft.gun.GunPart.SCOPE).get());
        int assemblerParts = 0;
        System.out.println("[AUTOTEST] v18 workbench gunParts scope=" + scopeOk + " assemblerParts=" + assemblerParts);
        long total = rm.getRecipes().stream().filter(h -> h.id().getNamespace().equals("tycooncraft")).count();
        long known = rm.getRecipes().stream().filter(h -> h.id().getNamespace().equals("tycooncraft") && sp.getRecipeBook().contains(h)).count();
        System.out.println("[AUTOTEST] v18 workbench parts stock=" + stockOk + " barrel=" + barrelOk + " receiver=" + recOk + " silencer=" + silOk
                + " recipeBook=" + known + "/" + total);
        // 제작법 클릭 → 재료 자동 채우기 (Shift = 최대)
        fill(m);
        sp.getInventory().clearContent();
        sp.getInventory().add(new ItemStack(rec, 2)); sp.getInventory().add(new ItemStack(bar, 5)); sp.getInventory().add(new ItemStack(stock, 3));
        sp.getInventory().add(new ItemStack(ModItems.ADVANCED_CIRCUIT.get(), 2)); sp.getInventory().add(new ItemStack(Items.GLASS, 4)); sp.getInventory().add(new ItemStack(Items.SPYGLASS, 2));
        int sniperIdx = -1;
        for (int i = 0; i < GunCrafting.all().size(); i++) if (GunCrafting.all().get(i).result().is(ModItems.SNIPER.get())) sniperIdx = i;
        boolean one = m.fill(sp, sniperIdx, false);
        boolean oneOk = m.getSlot(0).getItem().is(ModItems.SNIPER.get()) && m.grid.getItem(0).getCount() == 1;
        boolean maxed = m.fill(sp, sniperIdx, true);
        int perSlot = m.grid.getItem(0).getCount();
        m.clicked(0, 0, ClickType.QUICK_MOVE, sp);
        boolean refuse = !m.fill(sp, sniperIdx, false);
        System.out.println("[AUTOTEST] v184 fill one=" + one + "/" + oneOk + " max=" + maxed + " perSlot=" + perSlot + " snipers=" + count(sp, ModItems.SNIPER.get())
                + " noMaterialsRefused=" + refuse + " barrelsLeft=" + count(sp, bar));
        // 화면용: 재료 조금 (만들 수 있는 것 / 없는 것 섞이게)
        fill(m);
        sp.getInventory().add(new ItemStack(rec, 1)); sp.getInventory().add(new ItemStack(bar, 2)); sp.getInventory().add(new ItemStack(stock, 1));
        sp.getInventory().add(new ItemStack(ModItems.ADVANCED_CIRCUIT.get(), 1)); sp.getInventory().add(new ItemStack(ModItems.IRON_PLATE.get(), 3));
        sp.getInventory().add(new ItemStack(ModItems.CIRCUIT.get(), 1)); sp.getInventory().add(new ItemStack(ModItems.STEEL_PLATE.get(), 6));
    }

    /** 제작대 3대 + 공장 아이템 진열 (방향별) */
    static void showcase(ServerPlayer sp) {
        var sl = sp.serverLevel();
        BlockPos base = BlockPos.containing(-30, sp.getY(), -60);
        for (int i = 0; i < 3; i++) {
            var dir = new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST}[i];
            sl.setBlockAndUpdate(base.offset(i * 2 - 2, 0, 0), ModBlocks.GUN_WORKBENCH.get().defaultBlockState().setValue(com.tycooncraft.gun.GunWorkbenchBlock.FACING, dir));
        }
        sp.teleportTo(sl, base.getX() + 0.5, base.getY() + 0.2, base.getZ() - 2.6, 0f, 32f);
    }

    /** 새 아이템 그림 한눈에: 상자(54칸) + 보관함(36칸) */
    static void showItems(ServerPlayer sp) {
        String[] ids = ("steel_ingot aluminum_ingot iron_plate steel_plate copper_plate gold_plate aluminum_plate iron_rod steel_rod rebar iron_gear steel_gear "
                + "iron_dust copper_dust gold_dust coal_dust silica_dust bauxite_dust raw_bauxite rock_salt sulfur graphite coke silicon "
                + "copper_wire gold_wire insulated_cable copper_coil bolt ball_bearing gasket valve piston drive_shaft steel_pipe h_beam "
                + "circuit advanced_circuit control_board pcb microchip capacitor resistor battery_cell silicon_wafer solar_panel transformer electric_motor "
                + "engine gearbox mechanism hydraulic_cylinder industrial_robot_arm drill_head wheel "
                + "coin_copper coin_iron coin_gold coin_silver coin_emerald coin_diamond bill_5k bill_50k check_100k gold_bar_1m "
                + "light_ammo heavy_ammo shotgun_shell frag_grenade flash_grenade smoke_grenade gun_receiver gun_barrel gun_stock silencer "
                + "part_scope part_red_dot part_extended_mag part_vertical_grip vest_1 vest_2 vest_3 vest_4 helmet_4 pants_2 boots_3 combat_boots "
                + "sulfuric_acid gas_cylinder fuel_canister wooden_crate").split(" ");
        var chest = new net.minecraft.world.SimpleContainer(54);
        sp.getInventory().clearContent();
        int i = 0;
        for (String id : ids) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(com.tycooncraft.TycoonCraft.id(id));
            if (item == Items.AIR) continue;
            if (i < 54) chest.setItem(i, new ItemStack(item));
            else if (i - 54 < 36) sp.getInventory().setItem((i - 54 + 9) % 36, new ItemStack(item));
            i++;
        }
        sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> net.minecraft.world.inventory.ChestMenu.sixRows(id, inv, chest),
                net.minecraft.network.chat.Component.literal("TycoonCraft")));
    }

    private GunWorkbenchTest() {}
}
