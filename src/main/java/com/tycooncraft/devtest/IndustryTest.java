package com.tycooncraft.devtest;

import com.tycooncraft.blockentity.MachineBlockEntity;
import com.tycooncraft.economy.PriceTable;
import com.tycooncraft.machine.MachineRecipe;
import com.tycooncraft.machine.MachineType;
import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

import java.util.*;

/** v0.16 산업 체인 테스트: 레시피 가로채기 검사 + 신규 기계 실제 가공 */
public final class IndustryTest {
    record Job(MachineType type, BlockPos pos, Object[] inputs) {}
    static final List<Job> JOBS = new ArrayList<>();

    static void tick(ServerLevel level, int t, int gy) {
        if (t == 880) {
            shadowCheck();
            coverage();
            JOBS.clear();
            int x = 146;
            job(level, MachineType.ELECTROLYZER, new BlockPos(x, gy, 100), ModItems.SALT.get(), 2, Items.GLASS_BOTTLE, 1);
            job(level, MachineType.CHEMICAL_REACTOR, new BlockPos(x + 2, gy, 100), ModItems.BAUXITE_DUST.get(), 2, ModItems.CAUSTIC_SODA.get(), 1);
            job(level, MachineType.ELECTROLYZER, new BlockPos(x + 4, gy, 100), ModItems.ALUMINA.get(), 2, ModItems.COKE.get(), 1);
            job(level, MachineType.ALLOY_FURNACE, new BlockPos(x + 6, gy, 100), Items.IRON_INGOT, 2, ModItems.COKE.get(), 1, ModItems.QUICKLIME.get(), 1);
            job(level, MachineType.CHEMICAL_REACTOR, new BlockPos(x + 8, gy, 100), ModItems.SILICON_WAFER.get(), 1, ModItems.SULFURIC_ACID.get(), 1, ModItems.GOLD_WIRE.get(), 2);
            job(level, MachineType.ASSEMBLER, new BlockPos(x + 10, gy, 100), ModItems.ELECTRIC_MOTOR.get(), 3, ModItems.GEARBOX.get(), 2,
                    ModItems.CONTROL_BOARD.get(), 1, ModItems.HYDRAULIC_CYLINDER.get(), 2, ModItems.ALUMINUM_PLATE.get(), 4, ModItems.BOLT.get(), 8);
            job(level, MachineType.LATHE, new BlockPos(x + 12, gy, 100), ModItems.SILICON.get(), 1);
            job(level, MachineType.PRESS, new BlockPos(x + 14, gy, 100), Items.JUNGLE_LOG, 1);
        }
        if (t == 1000) {
            StringBuilder sb = new StringBuilder("[AUTOTEST] v16 industry");
            for (Job j : JOBS) {
                ItemStack out = level.getBlockEntity(j.pos) instanceof MachineBlockEntity be ? be.output().getStackInSlot(0) : ItemStack.EMPTY;
                sb.append(' ').append(j.type.getSerializedName()).append("->").append(out.isEmpty() ? "EMPTY" : out.getCount() + "x" + out.getItem().builtInRegistryHolder().key().location().getPath());
            }
            System.out.println(sb);
        }
    }

    static void job(ServerLevel level, MachineType type, BlockPos pos, Object... in) {
        level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, ModBlocks.machine(type).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be)
            for (int i = 0; i < in.length; i += 2) be.inputs().insertItem(i / 2, new ItemStack((Item) in[i], (Integer) in[i + 1]), false);
        JOBS.add(new Job(type, pos, in));
    }

    /** 각 레시피의 재료만 정확히 넣었을 때 다른 레시피가 먼저 잡히면 안 됨 */
    static void shadowCheck() {
        int total = 0, bad = 0;
        for (MachineType type : MachineType.values()) {
            List<MachineRecipe> rs = type.recipes();
            for (MachineRecipe target : rs) {
                total++;
                for (MachineRecipe r : rs) {
                    boolean ok = r.inputs().entrySet().stream().allMatch(e -> target.inputs().getOrDefault(e.getKey(), 0) >= e.getValue());
                    if (!ok) continue;
                    if (r != target && r.output() != target.output()) {
                        bad++;
                        System.out.println("[AUTOTEST] v16 SHADOW " + type.getSerializedName() + ": " + r.output() + " steals " + target.output());
                    }
                    break;
                }
            }
        }
        System.out.println("[AUTOTEST] v16 recipes total=" + total + " shadowed=" + bad);
    }

    /** 신규 50종: 판매가 있음 + 만드는 방법 있음 + (완제품 제외) 어딘가에 쓰임 */
    static void coverage() {
        List<Item> items = new ArrayList<>();
        boolean on = false;
        for (var h : ModItems.ITEMS.getEntries()) {
            String id = h.getId().getPath();
            if (id.equals("raw_bauxite")) on = true;
            if (on && id.endsWith("_ore")) break;
            if (on) items.add(h.get());
        }
        Set<Item> made = new HashSet<>(), used = new HashSet<>();
        for (MachineType type : MachineType.values())
            for (MachineRecipe r : type.recipes()) { made.add(r.output()); used.addAll(r.inputs().keySet()); }
        made.add(ModItems.RAW_BAUXITE.get());
        made.add(ModItems.ROCK_SALT.get());
        Set<String> finals = Set.of("industrial_robot_arm", "transformer", "solar_panel", "battery_cell", "h_beam", "precast_panel");
        List<String> noPrice = new ArrayList<>(), noMake = new ArrayList<>(), unused = new ArrayList<>();
        for (Item it : items) {
            String id = it.builtInRegistryHolder().key().location().getPath();
            if (PriceTable.basePrice(it) <= 0) noPrice.add(id);
            if (!made.contains(it)) noMake.add(id);
            if (!used.contains(it) && !finals.contains(id)) unused.add(id);
        }
        System.out.println("[AUTOTEST] v16 items=" + items.size() + " noPrice=" + noPrice + " noRecipe=" + noMake + " unused=" + unused);
    }

    private IndustryTest() {}
}
