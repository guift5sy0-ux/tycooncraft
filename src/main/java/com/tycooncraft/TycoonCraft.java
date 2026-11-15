package com.tycooncraft;

import com.tycooncraft.devtest.AutoTest;
import com.tycooncraft.event.TycoonCommands;
import com.tycooncraft.event.TycoonEvents;
import com.tycooncraft.registry.ModBlockEntities;
import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModComponents;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.mob.GunnerEntity;
import com.tycooncraft.registry.ModRecipes;
import com.tycooncraft.network.HornPayload;
import com.tycooncraft.registry.ModCreativeTabs;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModMenus;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * TycoonCraft - 친구들과 공장을 짓고 가공품을 팔아 돈을 버는 경제·공장 모드.
 */
@Mod(TycoonCraft.MODID)
public class TycoonCraft {
    public static final String MODID = "tycooncraft";

    public TycoonCraft(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        com.tycooncraft.registry.ModArmorMaterials.MATERIALS.register(modBus);
        com.tycooncraft.registry.ModEffects.EFFECTS.register(modBus);

        modBus.addListener(this::registerCapabilities);
        modBus.addListener(HornPayload::register);
        modBus.addListener(this::attributes);
        modBus.addListener(this::spawnPlacements);

        NeoForge.EVENT_BUS.register(TycoonEvents.class);
        NeoForge.EVENT_BUS.register(TycoonCommands.class);
        NeoForge.EVENT_BUS.register(com.tycooncraft.gear.Parachute.class);
        NeoForge.EVENT_BUS.register(com.tycooncraft.gun.GunEvents.class);
        if (AutoTest.ENABLED) NeoForge.EVENT_BUS.register(AutoTest.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ModEntities.GANGSTER.get(), GunnerEntity.attributes(GunnerEntity.Kind.GANGSTER).build());
        event.put(ModEntities.MERCENARY.get(), GunnerEntity.attributes(GunnerEntity.Kind.MERCENARY).build());
        event.put(ModEntities.OUTLAW.get(), GunnerEntity.attributes(GunnerEntity.Kind.OUTLAW).build());
        event.put(ModEntities.CARTEL_BOSS.get(), GunnerEntity.attributes(GunnerEntity.Kind.BOSS).build());
        event.put(ModEntities.MUTANT_SPIDER.get(), com.tycooncraft.mob.MutantSpider.attributes().build());
    }

    private void spawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        for (var type : java.util.List.of(ModEntities.GANGSTER.get(), ModEntities.MERCENARY.get(), ModEntities.OUTLAW.get())) {
            event.register(type, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        event.register(ModEntities.MUTANT_SPIDER.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** 호퍼·다른 기계가 아이템을 넣고 뺄 수 있게 */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SELL_STATION.get(),
                (be, side) -> be.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MACHINE.get(),
                (be, side) -> be.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.TURRET.get(),
                (be, side) -> be.getItemHandler());
    }
}
