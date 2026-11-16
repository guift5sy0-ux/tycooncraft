package com.tycooncraft.world.gen;

import com.tycooncraft.TycoonCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, TycoonCraft.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, TycoonCraft.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<TycoonStructure>> TYCOON =
            TYPES.register("tycoon", () -> () -> TycoonStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> TYCOON_PIECE =
            PIECES.register("tycoon_piece", () -> (StructurePieceType.ContextlessType) TycoonPiece::new);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        PIECES.register(bus);
    }

    private ModStructures() {}
}
