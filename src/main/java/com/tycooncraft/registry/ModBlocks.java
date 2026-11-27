package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.block.ExchangeBlock;
import com.tycooncraft.block.LiftBlock;
import com.tycooncraft.block.SellStationBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TycoonCraft.MODID);

    // v0.33: 강화 폭약
    public static final DeferredBlock<com.tycooncraft.explosive.HdxBlock> HDX = BLOCKS.register("hdx",
            () -> new com.tycooncraft.explosive.HdxBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.TNT)));

    // ── 판매 & 화폐 ──
    public static final DeferredBlock<SellStationBlock> SELL_STATION = BLOCKS.register("sell_station",
            () -> new SellStationBlock(machineProps().strength(3.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredBlock<ExchangeBlock> EXCHANGE = BLOCKS.register("exchange",
            () -> new ExchangeBlock(machineProps().strength(3.0f, 6.0f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<com.tycooncraft.gun.GunWorkbenchBlock> GUN_WORKBENCH = BLOCKS.register("gun_workbench",
            () -> new com.tycooncraft.gun.GunWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .strength(2.5f).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion()));

    // ── v0.26.0 블랙마켓 ──
    public static final DeferredBlock<com.tycooncraft.blackmarket.ManholeBlock> MANHOLE = BLOCKS.register("manhole",
            () -> new com.tycooncraft.blackmarket.ManholeBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .strength(-1.0f, 3_600_000f).sound(net.minecraft.world.level.block.SoundType.NETHERITE_BLOCK).noLootTable()));
    public static final DeferredBlock<com.tycooncraft.blackmarket.BountyBoardBlock> BOUNTY_BOARD = BLOCKS.register("bounty_board",
            () -> new com.tycooncraft.blackmarket.BountyBoardBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000f).sound(net.minecraft.world.level.block.SoundType.WOOD).noLootTable()));
    /** v0.29 언더시티 갱단 금고 상자: 열쇠 1개로 한 번 열림, 하루 뒤 다시 채워지고 잠김 */
    public static final DeferredBlock<com.tycooncraft.blackmarket.LockedChestBlock> LOCKED_CHEST = BLOCKS.register("locked_chest",
            () -> new com.tycooncraft.blackmarket.LockedChestBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000f).sound(net.minecraft.world.level.block.SoundType.WOOD).noLootTable()));
    public static final DeferredBlock<com.tycooncraft.blackmarket.BMBlock> MISSION_BOARD = BLOCKS.register("mission_board",
            () -> new com.tycooncraft.blackmarket.BMBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000f).sound(net.minecraft.world.level.block.SoundType.WOOD).noLootTable(), 0));
    public static final DeferredBlock<com.tycooncraft.blackmarket.BMBlock> SLOT_MACHINE = BLOCKS.register("slot_machine",
            () -> new com.tycooncraft.blackmarket.BMBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000f).sound(net.minecraft.world.level.block.SoundType.METAL).lightLevel(s -> 9).noLootTable(), 1));
    public static final DeferredBlock<com.tycooncraft.blackmarket.BMBlock> ROULETTE_TABLE = BLOCKS.register("roulette_table",
            () -> new com.tycooncraft.blackmarket.BMBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000f).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion().noLootTable(), 2));
    public static final DeferredBlock<com.tycooncraft.blackmarket.BMBlock> MOD_BENCH = BLOCKS.register("mod_bench",
            () -> new com.tycooncraft.blackmarket.BMBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000f).sound(net.minecraft.world.level.block.SoundType.ANVIL).noLootTable(), 3));
    public static final DeferredBlock<com.tycooncraft.blackmarket.VaultBlock> BM_VAULT = BLOCKS.register("bm_vault",
            () -> new com.tycooncraft.blackmarket.VaultBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000f).sound(net.minecraft.world.level.block.SoundType.NETHERITE_BLOCK).noLootTable()));
    public static final DeferredBlock<com.tycooncraft.cctv.CctvBlock> CCTV = BLOCKS.register("cctv",
            () -> new com.tycooncraft.cctv.CctvBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .strength(1.5f).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<com.tycooncraft.cctv.ScreenBlock> CCTV_SCREEN = BLOCKS.register("cctv_screen",
            () -> new com.tycooncraft.cctv.ScreenBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK)
                    .strength(1.5f).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion().lightLevel(s -> 7)));

    // ── 배 ──
    public static final DeferredBlock<com.tycooncraft.ship.ShipMotorBlock> SHIP_MOTOR = BLOCKS.register("ship_motor",
            () -> new com.tycooncraft.ship.ShipMotorBlock(machineProps().strength(2.0f, 6.0f)));
    /** v0.27.1 비행 코어 엔진: 운송기 엔진의 상위호환 (엔진 6개분 추진 + 부유체 40개분 부양 + 연료 40% 절약) */
    public static final DeferredBlock<com.tycooncraft.ship.ShipMotorBlock> SHIP_CORE_ENGINE = BLOCKS.register("ship_core_engine",
            () -> new com.tycooncraft.ship.ShipMotorBlock(machineProps().strength(3.0f, 12.0f).lightLevel(s -> 10)));
    public static final DeferredBlock<com.tycooncraft.ship.ShipHelmBlock> SHIP_HELM = BLOCKS.register("ship_helm",
            () -> new com.tycooncraft.ship.ShipHelmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1.5f).noOcclusion()));
    public static final DeferredBlock<com.tycooncraft.setter.SetterBlock> SETTER = BLOCKS.register("setter",
            () -> new com.tycooncraft.setter.SetterBlock(machineProps().strength(3.0f, 6.0f)));
    public static final DeferredBlock<com.tycooncraft.ship.ShipBoosterBlock> SHIP_BOOSTER = BLOCKS.register("ship_booster",
            () -> new com.tycooncraft.ship.ShipBoosterBlock(machineProps().strength(2.5f, 6.0f)));
    public static final DeferredBlock<com.tycooncraft.ship.ShipFloatBlock> SHIP_FLOAT = BLOCKS.register("ship_float",
            () -> new com.tycooncraft.ship.ShipFloatBlock(machineProps().strength(1.5f, 4.0f)));
    public static final DeferredBlock<com.tycooncraft.machine.EmpGeneratorBlock> EMP_GENERATOR = BLOCKS.register("emp_generator",
            () -> new com.tycooncraft.machine.EmpGeneratorBlock(machineProps().strength(4.0f, 12.0f).requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(s -> s.getValue(com.tycooncraft.machine.EmpGeneratorBlock.ACTIVE) ? 12 : 0)));
    public static final DeferredBlock<LiftBlock> LIFT_UP = BLOCKS.register("lift_up",
            () -> new LiftBlock(machineProps().strength(1.5f), true));
    public static final DeferredBlock<LiftBlock> LIFT_DOWN = BLOCKS.register("lift_down",
            () -> new LiftBlock(machineProps().strength(1.5f), false));

    // ── 폐공장 장식 ──
    public static final DeferredBlock<com.tycooncraft.turret.CiwsBlock> CIWS = BLOCKS.register("ciws",
            () -> new com.tycooncraft.turret.CiwsBlock(machineProps().strength(5.0f, 12.0f).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredBlock<com.tycooncraft.turret.TurretBlock> TURRET = BLOCKS.register("turret",
            () -> new com.tycooncraft.turret.TurretBlock(machineProps().strength(4.0f, 12.0f).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredBlock<Block> BROKEN_MACHINE = BLOCKS.registerSimpleBlock("broken_machine",
            machineProps().strength(3.0f, 6.0f).requiresCorrectToolForDrops());


    private static BlockBehaviour.Properties machineProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL);
    }

    private ModBlocks() {}
}
