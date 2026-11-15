package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.gun.SilencerRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 특수 제작대 레시피: 총 + 소음기 장착/분리 */
public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TycoonCraft.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<SilencerRecipe>> SILENCER =
            SERIALIZERS.register("silencer", () -> new SimpleCraftingRecipeSerializer<>(SilencerRecipe::new));

    private ModRecipes() {}
}
