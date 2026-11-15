package com.tycooncraft;

import com.tycooncraft.event.TycoonCommands;
import com.tycooncraft.event.TycoonEvents;
import com.tycooncraft.registry.ModBlockEntities;
import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModComponents;
import com.tycooncraft.registry.ModCreativeTabs;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModMenus;
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

        modBus.addListener(this::registerCapabilities);

        NeoForge.EVENT_BUS.register(TycoonEvents.class);
        NeoForge.EVENT_BUS.register(TycoonCommands.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    /** 호퍼·다른 기계가 아이템을 넣고 뺄 수 있게 */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SELL_STATION.get(),
                (be, side) -> be.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MACHINE.get(),
                (be, side) -> be.getItemHandler());
    }
}
