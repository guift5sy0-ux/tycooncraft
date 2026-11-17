package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.economy.CoinItem;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.vehicle.VehicleItem;
import com.tycooncraft.gun.RpgItem;
import com.tycooncraft.gear.GearItem;
import com.tycooncraft.mob.FlareItem;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import com.tycooncraft.vehicle.DroneItem;
import com.tycooncraft.gun.GunStats;
import com.tycooncraft.machine.MachineType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TycoonCraft.MODID);

    // ── 화폐 (₩) ──
    public static final DeferredItem<CoinItem> COIN_COPPER = coin("coin_copper", 1);
    public static final DeferredItem<CoinItem> COIN_IRON = coin("coin_iron", 10);
    public static final DeferredItem<CoinItem> COIN_GOLD = coin("coin_gold", 100);
    public static final DeferredItem<CoinItem> COIN_EMERALD = coin("coin_emerald", 1_000);
    public static final DeferredItem<CoinItem> COIN_DIAMOND = coin("coin_diamond", 10_000);

    // ── 1차: 가루 ──
    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");
    public static final DeferredItem<Item> COAL_DUST = ITEMS.registerSimpleItem("coal_dust");
    public static final DeferredItem<Item> FLOUR = ITEMS.registerSimpleItem("flour");
    // ── 2차: 합금 ──
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    // ── 3차: 판 / 선 / 봉 ──
    public static final DeferredItem<Item> IRON_PLATE = ITEMS.registerSimpleItem("iron_plate");
    public static final DeferredItem<Item> COPPER_PLATE = ITEMS.registerSimpleItem("copper_plate");
    public static final DeferredItem<Item> GOLD_PLATE = ITEMS.registerSimpleItem("gold_plate");
    public static final DeferredItem<Item> STEEL_PLATE = ITEMS.registerSimpleItem("steel_plate");
    public static final DeferredItem<Item> COPPER_WIRE = ITEMS.registerSimpleItem("copper_wire");
    public static final DeferredItem<Item> GOLD_WIRE = ITEMS.registerSimpleItem("gold_wire");
    public static final DeferredItem<Item> IRON_ROD = ITEMS.registerSimpleItem("iron_rod");
    public static final DeferredItem<Item> STEEL_ROD = ITEMS.registerSimpleItem("steel_rod");
    // ── 4차: 기어 ──
    public static final DeferredItem<Item> IRON_GEAR = ITEMS.registerSimpleItem("iron_gear");
    public static final DeferredItem<Item> STEEL_GEAR = ITEMS.registerSimpleItem("steel_gear");
    // ── 5차: 조립품 ──
    public static final DeferredItem<Item> CIRCUIT = ITEMS.registerSimpleItem("circuit");
    public static final DeferredItem<Item> ADVANCED_CIRCUIT = ITEMS.registerSimpleItem("advanced_circuit");
    public static final DeferredItem<Item> MECHANISM = ITEMS.registerSimpleItem("mechanism");
    public static final DeferredItem<Item> ENGINE = ITEMS.registerSimpleItem("engine");
    public static final DeferredItem<Item> WOODEN_CRATE = ITEMS.registerSimpleItem("wooden_crate");

    // ── 총기 부품 & 탄약 ──
    public static final DeferredItem<Item> GUN_BARREL = ITEMS.registerSimpleItem("gun_barrel");
    public static final DeferredItem<Item> GUN_RECEIVER = ITEMS.registerSimpleItem("gun_receiver");
    public static final DeferredItem<Item> GUN_STOCK = ITEMS.registerSimpleItem("gun_stock");
    public static final DeferredItem<Item> LIGHT_AMMO = ITEMS.registerSimpleItem("light_ammo");
    public static final DeferredItem<Item> HEAVY_AMMO = ITEMS.registerSimpleItem("heavy_ammo");
    public static final DeferredItem<Item> SHOTGUN_SHELL = ITEMS.registerSimpleItem("shotgun_shell");

    // ── 총 ── (피해, 탄 수, 퍼짐, 사거리, 탄창, 발사 간격, 재장전, 연사, 조준, 반동, 탄약, 발사음, 재장전음)
    public static final DeferredItem<GunItem> PISTOL = ITEMS.register("pistol", () -> new GunItem(new Item.Properties(),
            new GunStats(7f, 1, 0.012f, 48, 12, 6, 30, false, false, 2.5f, LIGHT_AMMO, ModSounds.PISTOL_FIRE, ModSounds.PISTOL_RELOAD)));
    public static final DeferredItem<GunItem> SMG = ITEMS.register("smg", () -> new GunItem(new Item.Properties(),
            new GunStats(5f, 1, 0.03f, 40, 30, 3, 40, true, false, 1.2f, LIGHT_AMMO, ModSounds.SMG_FIRE, ModSounds.SMG_RELOAD)));
    public static final DeferredItem<GunItem> RIFLE = ITEMS.register("rifle", () -> new GunItem(new Item.Properties(),
            new GunStats(8f, 1, 0.015f, 80, 30, 4, 50, true, false, 1.8f, HEAVY_AMMO, ModSounds.RIFLE_FIRE, ModSounds.RIFLE_RELOAD)));
    public static final DeferredItem<GunItem> SHOTGUN = ITEMS.register("shotgun", () -> new GunItem(new Item.Properties(),
            new GunStats(4f, 8, 0.09f, 20, 6, 18, 50, false, false, 6f, SHOTGUN_SHELL, ModSounds.SHOTGUN_FIRE, ModSounds.SHOTGUN_RELOAD)));
    public static final DeferredItem<GunItem> SNIPER = ITEMS.register("sniper", () -> new GunItem(new Item.Properties(),
            new GunStats(26f, 1, 0.05f, 160, 5, 30, 60, false, true, 8f, HEAVY_AMMO, ModSounds.SNIPER_FIRE, ModSounds.SNIPER_RELOAD)));

    // ── 총기 부착물 & 드론 ──
    public static final DeferredItem<Item> SILENCER = ITEMS.register("silencer", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<DroneItem> DRONE = ITEMS.register("drone", () -> new DroneItem(new Item.Properties(), 1));
    public static final DeferredItem<DroneItem> DRONE_MK2 = ITEMS.register("drone_mk2", () -> new DroneItem(new Item.Properties(), 2));
    public static final DeferredItem<Item> SILENCER_ADVANCED = ITEMS.register("silencer_advanced", () -> new Item(new Item.Properties().stacksTo(16)));

    // ── 신형 총기 ──
    public static final DeferredItem<GunItem> VSS = ITEMS.register("vss", () -> new GunItem(new Item.Properties(),
            new GunStats(8f, 1, 0.008f, 100, 10, 6, 40, false, true, 2.0f, LIGHT_AMMO, ModSounds.SILENCED_FIRE, ModSounds.RIFLE_RELOAD)));
    public static final DeferredItem<GunItem> DUAL_PISTOLS = ITEMS.register("dual_pistols", () -> new GunItem(new Item.Properties(),
            new GunStats(5.5f, 1, 0.032f, 40, 24, 2, 22, true, false, 1.3f, LIGHT_AMMO, ModSounds.PISTOL_FIRE, ModSounds.PISTOL_RELOAD)));
    public static final DeferredItem<RpgItem> RPG = ITEMS.register("rpg", () -> new RpgItem(new Item.Properties(),
            new GunStats(0f, 1, 0f, 0, 1, 20, 50, false, false, 5f, ModItems.ROCKET, ModSounds.RPG_FIRE, ModSounds.RIFLE_RELOAD)));
    /** 날카로운 반자동 "이계의 심판자": 네더라이트 5 + 권총 + 드래곤 알 + 신호기 2 */
    public static final DeferredItem<com.tycooncraft.gun.JudgeItem> JUDGE = ITEMS.register("judge", () -> new com.tycooncraft.gun.JudgeItem(
            new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC).durability(com.tycooncraft.gun.JudgeItem.DURABILITY)
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                    Item.BASE_ATTACK_DAMAGE_ID, 11, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                    Item.BASE_ATTACK_SPEED_ID, -2.2, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build()),
            new GunStats(11f, 1, 0.005f, 90, 9, 5, 34, false, false, 3.2f, HEAVY_AMMO, ModSounds.JUDGE_FIRE, ModSounds.PISTOL_RELOAD)));
    public static final DeferredItem<Item> ROCKET = ITEMS.register("rocket", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> TANK_SHELL = ITEMS.register("tank_shell", () -> new Item(new Item.Properties().stacksTo(16)));

    // ── 방탄 장비 & 낙하산 ──
    public static final DeferredItem<GearItem> VEST_1 = gear("vest_1", ModArmorMaterials.TACTICAL_LIGHT, ArmorItem.Type.CHESTPLATE, 15, "tooltip.tycooncraft.vest", 15);
    public static final DeferredItem<GearItem> VEST_2 = gear("vest_2", ModArmorMaterials.TACTICAL_MEDIUM, ArmorItem.Type.CHESTPLATE, 25, "tooltip.tycooncraft.vest", 25);
    public static final DeferredItem<GearItem> VEST_3 = gear("vest_3", ModArmorMaterials.TACTICAL_HEAVY, ArmorItem.Type.CHESTPLATE, 35, "tooltip.tycooncraft.vest", 35);
    public static final DeferredItem<GearItem> HELMET_1 = gear("helmet_1", ModArmorMaterials.TACTICAL_LIGHT, ArmorItem.Type.HELMET, 15, "tooltip.tycooncraft.helmet", 20);
    public static final DeferredItem<GearItem> HELMET_2 = gear("helmet_2", ModArmorMaterials.TACTICAL_MEDIUM, ArmorItem.Type.HELMET, 25, "tooltip.tycooncraft.helmet", 35);
    public static final DeferredItem<GearItem> HELMET_3 = gear("helmet_3", ModArmorMaterials.TACTICAL_HEAVY, ArmorItem.Type.HELMET, 35, "tooltip.tycooncraft.helmet", 50);
    // 중장갑 세트 (4종)
    public static final DeferredItem<GearItem> HELMET_4 = gear("helmet_4", ModArmorMaterials.JUGGERNAUT, ArmorItem.Type.HELMET, 45, "tooltip.tycooncraft.helmet", 65);
    public static final DeferredItem<GearItem> VEST_4 = gear("vest_4", ModArmorMaterials.JUGGERNAUT, ArmorItem.Type.CHESTPLATE, 45, "tooltip.tycooncraft.vest", 45);
    public static final DeferredItem<GearItem> JUGGERNAUT_LEGGINGS = gear("juggernaut_leggings", ModArmorMaterials.JUGGERNAUT, ArmorItem.Type.LEGGINGS, 45, "tooltip.tycooncraft.jugg_legs", 8);
    public static final DeferredItem<GearItem> COMBAT_BOOTS = gear("combat_boots", ModArmorMaterials.JUGGERNAUT, ArmorItem.Type.BOOTS, 45, "tooltip.tycooncraft.jugg_boots", 5);
    public static final DeferredItem<GearItem> PARACHUTE = ITEMS.register("parachute", () -> new GearItem(ModArmorMaterials.PARACHUTE, ArmorItem.Type.LEGGINGS,
            new Item.Properties().stacksTo(1), "tooltip.tycooncraft.parachute"));

    // ── 보스 소환 & 스폰알 ──
    // v0.12: 유황, 수류탄, 자동 굴착기
    public static final DeferredItem<Item> SULFUR = ITEMS.registerSimpleItem("sulfur");
    public static final DeferredItem<BlockItem> SULFUR_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.SULFUR_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_SULFUR_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_SULFUR_ORE);
    public static final DeferredItem<com.tycooncraft.gun.GrenadeItem> FRAG_GRENADE = ITEMS.register("frag_grenade",
            () -> new com.tycooncraft.gun.GrenadeItem(new Item.Properties(), "tooltip.tycooncraft.frag_grenade"));
    public static final DeferredItem<com.tycooncraft.gun.GrenadeItem> FLASH_GRENADE = ITEMS.register("flash_grenade",
            () -> new com.tycooncraft.gun.GrenadeItem(new Item.Properties(), "tooltip.tycooncraft.flash_grenade"));
    public static final DeferredItem<com.tycooncraft.gun.GrenadeItem> SMOKE_GRENADE = ITEMS.register("smoke_grenade",
            () -> new com.tycooncraft.gun.GrenadeItem(new Item.Properties(), "tooltip.tycooncraft.smoke_grenade"));
    public static final DeferredItem<Item> VENOM_SPIT = ITEMS.registerSimpleItem("venom_spit");
    public static final DeferredItem<Item> DRILL_HEAD = ITEMS.registerSimpleItem("drill_head");
    public static final DeferredItem<Item> WHEEL = ITEMS.registerSimpleItem("wheel");
    public static final DeferredItem<com.tycooncraft.miner.AutoMinerItem> AUTO_MINER = ITEMS.register("auto_miner",
            () -> new com.tycooncraft.miner.AutoMinerItem(new Item.Properties()));

    public static final DeferredItem<FlareItem> CARTEL_FLARE = ITEMS.register("cartel_flare", () -> new FlareItem(new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GANGSTER_EGG = ITEMS.register("gangster_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GANGSTER, 0x1C1C20, 0xB01E22, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> MERCENARY_EGG = ITEMS.register("mercenary_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MERCENARY, 0x56603E, 0x2A2C26, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> OUTLAW_EGG = ITEMS.register("outlaw_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OUTLAW, 0xA0221E, 0x6E4A28, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SPIDER_EGG = ITEMS.register("mutant_spider_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MUTANT_SPIDER, 0x361214, 0x5ADC46, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BOSS_EGG = ITEMS.register("cartel_boss_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CARTEL_BOSS, 0xE8E6DE, 0xAA1A1E, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> GANG_TRUCK_EGG = ITEMS.register("gang_truck_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GANG_TRUCK, 0x1E1D1C, 0xA81414, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOLEM_EGG = ITEMS.register("destroyer_golem_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.DESTROYER_GOLEM, 0x565C64, 0xE0B020, new Item.Properties()));
    // 보스 전리품 (판매용)
    public static final DeferredItem<Item> DESTROYER_CORE = ITEMS.register("destroyer_core", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // ── 탈것 & 연료 ──
    // ── v0.16 산업 아이템 50종 ──
    public static final DeferredItem<Item> RAW_BAUXITE = ITEMS.registerSimpleItem("raw_bauxite");
    public static final DeferredItem<Item> ROCK_SALT = ITEMS.registerSimpleItem("rock_salt");
    public static final DeferredItem<Item> BAUXITE_DUST = ITEMS.registerSimpleItem("bauxite_dust");
    public static final DeferredItem<Item> SALT = ITEMS.registerSimpleItem("salt");
    public static final DeferredItem<Item> LIMESTONE_DUST = ITEMS.registerSimpleItem("limestone_dust");
    public static final DeferredItem<Item> QUICKLIME = ITEMS.registerSimpleItem("quicklime");
    public static final DeferredItem<Item> SILICA_DUST = ITEMS.registerSimpleItem("silica_dust");
    public static final DeferredItem<Item> COKE = ITEMS.registerSimpleItem("coke");
    public static final DeferredItem<Item> GRAPHITE = ITEMS.registerSimpleItem("graphite");
    public static final DeferredItem<Item> SULFURIC_ACID = ITEMS.registerSimpleItem("sulfuric_acid");
    public static final DeferredItem<Item> CAUSTIC_SODA = ITEMS.registerSimpleItem("caustic_soda");
    public static final DeferredItem<Item> GAS_CYLINDER = ITEMS.registerSimpleItem("gas_cylinder");
    public static final DeferredItem<Item> CHLORINE_CYLINDER = ITEMS.registerSimpleItem("chlorine_cylinder");
    public static final DeferredItem<Item> PLASTIC_PELLET = ITEMS.registerSimpleItem("plastic_pellet");
    public static final DeferredItem<Item> EPOXY_RESIN = ITEMS.registerSimpleItem("epoxy_resin");
    public static final DeferredItem<Item> ALUMINA = ITEMS.registerSimpleItem("alumina");
    public static final DeferredItem<Item> ALUMINUM_INGOT = ITEMS.registerSimpleItem("aluminum_ingot");
    public static final DeferredItem<Item> ALUMINUM_PLATE = ITEMS.registerSimpleItem("aluminum_plate");
    public static final DeferredItem<Item> ALUMINUM_FOIL = ITEMS.registerSimpleItem("aluminum_foil");
    public static final DeferredItem<Item> COPPER_FOIL = ITEMS.registerSimpleItem("copper_foil");
    public static final DeferredItem<Item> LATEX = ITEMS.registerSimpleItem("latex");
    public static final DeferredItem<Item> RUBBER = ITEMS.registerSimpleItem("rubber");
    public static final DeferredItem<Item> GASKET = ITEMS.registerSimpleItem("gasket");
    public static final DeferredItem<Item> FIBERGLASS = ITEMS.registerSimpleItem("fiberglass");
    public static final DeferredItem<Item> CEMENT = ITEMS.registerSimpleItem("cement");
    public static final DeferredItem<Item> REBAR = ITEMS.registerSimpleItem("rebar");
    public static final DeferredItem<Item> H_BEAM = ITEMS.registerSimpleItem("h_beam");
    public static final DeferredItem<Item> PRECAST_PANEL = ITEMS.registerSimpleItem("precast_panel");
    public static final DeferredItem<Item> BOLT = ITEMS.registerSimpleItem("bolt");
    public static final DeferredItem<Item> BALL_BEARING = ITEMS.registerSimpleItem("ball_bearing");
    public static final DeferredItem<Item> DRIVE_SHAFT = ITEMS.registerSimpleItem("drive_shaft");
    public static final DeferredItem<Item> STEEL_PIPE = ITEMS.registerSimpleItem("steel_pipe");
    public static final DeferredItem<Item> VALVE = ITEMS.registerSimpleItem("valve");
    public static final DeferredItem<Item> PISTON = ITEMS.registerSimpleItem("piston");
    public static final DeferredItem<Item> HYDRAULIC_CYLINDER = ITEMS.registerSimpleItem("hydraulic_cylinder");
    public static final DeferredItem<Item> COPPER_COIL = ITEMS.registerSimpleItem("copper_coil");
    public static final DeferredItem<Item> ELECTRIC_MOTOR = ITEMS.registerSimpleItem("electric_motor");
    public static final DeferredItem<Item> GEARBOX = ITEMS.registerSimpleItem("gearbox");
    public static final DeferredItem<Item> SILICON = ITEMS.registerSimpleItem("silicon");
    public static final DeferredItem<Item> SILICON_WAFER = ITEMS.registerSimpleItem("silicon_wafer");
    public static final DeferredItem<Item> MICROCHIP = ITEMS.registerSimpleItem("microchip");
    public static final DeferredItem<Item> PCB = ITEMS.registerSimpleItem("pcb");
    public static final DeferredItem<Item> RESISTOR = ITEMS.registerSimpleItem("resistor");
    public static final DeferredItem<Item> CAPACITOR = ITEMS.registerSimpleItem("capacitor");
    public static final DeferredItem<Item> CONTROL_BOARD = ITEMS.registerSimpleItem("control_board");
    public static final DeferredItem<Item> INSULATED_CABLE = ITEMS.registerSimpleItem("insulated_cable");
    public static final DeferredItem<Item> BATTERY_CELL = ITEMS.registerSimpleItem("battery_cell");
    public static final DeferredItem<Item> SOLAR_PANEL = ITEMS.registerSimpleItem("solar_panel");
    public static final DeferredItem<Item> TRANSFORMER = ITEMS.registerSimpleItem("transformer");
    public static final DeferredItem<Item> INDUSTRIAL_ROBOT_ARM = ITEMS.registerSimpleItem("industrial_robot_arm");
    public static final DeferredItem<BlockItem> BAUXITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.BAUXITE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_BAUXITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_BAUXITE_ORE);
    public static final DeferredItem<BlockItem> ROCK_SALT_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.ROCK_SALT_ORE);

    public static final DeferredItem<Item> FUEL_CANISTER = ITEMS.registerSimpleItem("fuel_canister");
    public static final DeferredItem<VehicleItem> CAR = ITEMS.register("car",
            () -> new VehicleItem(new Item.Properties(), ModEntities.CAR, false, "tooltip.tycooncraft.car"));
    public static final DeferredItem<VehicleItem> LOCOMOTIVE = ITEMS.register("locomotive",
            () -> new VehicleItem(new Item.Properties(), ModEntities.LOCOMOTIVE, true, "tooltip.tycooncraft.locomotive"));
    public static final DeferredItem<VehicleItem> TANK = ITEMS.register("tank",
            () -> new VehicleItem(new Item.Properties(), ModEntities.TANK, false, "tooltip.tycooncraft.tank"));
    public static final DeferredItem<VehicleItem> FIGHTER_JET = ITEMS.register("fighter_jet",
            () -> new VehicleItem(new Item.Properties(), ModEntities.FIGHTER_JET, false, "tooltip.tycooncraft.fighter_jet"));
    public static final DeferredItem<VehicleItem> HELICOPTER = ITEMS.register("helicopter",
            () -> new VehicleItem(new Item.Properties(), ModEntities.HELICOPTER, false, "tooltip.tycooncraft.helicopter"));

    // ── 블록 아이템 ──
    public static final DeferredItem<BlockItem> SELL_STATION = ITEMS.registerSimpleBlockItem(ModBlocks.SELL_STATION);
    public static final DeferredItem<BlockItem> EXCHANGE = ITEMS.registerSimpleBlockItem(ModBlocks.EXCHANGE);
    public static final DeferredItem<BlockItem> CONVEYOR = ITEMS.registerSimpleBlockItem(ModBlocks.CONVEYOR);
    public static final DeferredItem<BlockItem> TRANSMISSION = ITEMS.registerSimpleBlockItem(ModBlocks.TRANSMISSION);
    public static final DeferredItem<BlockItem> SHIP_MOTOR = ITEMS.registerSimpleBlockItem(ModBlocks.SHIP_MOTOR);
    public static final DeferredItem<BlockItem> SHIP_HELM = ITEMS.registerSimpleBlockItem(ModBlocks.SHIP_HELM);
    public static final DeferredItem<BlockItem> SHIP_FLOAT = ITEMS.registerSimpleBlockItem(ModBlocks.SHIP_FLOAT);
    public static final DeferredItem<BlockItem> EMP_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.EMP_GENERATOR);
    public static final DeferredItem<BlockItem> LIFT_UP = ITEMS.registerSimpleBlockItem(ModBlocks.LIFT_UP);
    public static final DeferredItem<BlockItem> LIFT_DOWN = ITEMS.registerSimpleBlockItem(ModBlocks.LIFT_DOWN);
    public static final DeferredItem<BlockItem> BROKEN_MACHINE = ITEMS.registerSimpleBlockItem(ModBlocks.BROKEN_MACHINE);
    public static final DeferredItem<BlockItem> TURRET = ITEMS.registerSimpleBlockItem(ModBlocks.TURRET);
    public static final DeferredItem<BlockItem> CIWS = ITEMS.registerSimpleBlockItem(ModBlocks.CIWS);
    public static final List<DeferredItem<BlockItem>> MACHINE_ITEMS = new ArrayList<>();
    static {
        for (MachineType type : MachineType.values()) {
            MACHINE_ITEMS.add(ITEMS.registerSimpleBlockItem(ModBlocks.MACHINES.get(type)));
        }
    }

    public static Item machineItem(MachineType type) {
        return ModBlocks.machine(type).asItem();
    }

    private static DeferredItem<GearItem> gear(String name, net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> mat, ArmorItem.Type type,
                                               int durability, String tip, int pct) {
        return ITEMS.register(name, () -> new GearItem(mat, type, new Item.Properties().durability(type.getDurability(durability)), tip, pct));
    }

    private static DeferredItem<CoinItem> coin(String name, long value) {
        return ITEMS.register(name, () -> new CoinItem(new Item.Properties(), value));
    }

    private ModItems() {}
}
