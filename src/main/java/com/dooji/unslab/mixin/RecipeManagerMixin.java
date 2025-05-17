package com.dooji.unslab.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.JsonOps;
import net.minecraft.block.Block;
import net.minecraft.recipe.PreparedRecipes;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.unslab.Unslab;
import com.dooji.unslab.UnslabMapping;

import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.SortedMap;

@Mixin(ServerRecipeManager.class)
public class RecipeManagerMixin {
    @Inject(
            method = "prepare(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)Lnet/minecraft/recipe/PreparedRecipes;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/JsonDataLoader;load(Lnet/minecraft/resource/ResourceManager;Ljava/lang/String;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V", shift = At.Shift.AFTER)
    )
    private void injectCustomRecipes(ResourceManager resourceManager, Profiler profiler, CallbackInfoReturnable<PreparedRecipes> cir, @Local SortedMap<Identifier, Recipe<?>> sortedMap) {
        Unslab.LOGGER.info("[Unslab] Adding Unslab recipes to the recipe manager...");
        ServerRecipeManagerAccessor accessor = (ServerRecipeManagerAccessor)this;

        int count = 0;
        for (Map.Entry<Block, Block> entry : UnslabMapping.getSlabToBlockMap().entrySet()) {
            Block slab = entry.getKey();
            Block fullBlock = entry.getValue();

            if (isValidBlock(slab) && isValidBlock(fullBlock)) {
                String slabPath = Registries.BLOCK.getId(slab).getPath();
                Identifier recipeId = Identifier.of(Unslab.MOD_ID, "slab_to_" + slabPath);

                try {
                    JsonObject recipeJson = createShapedRecipeJson(slab, fullBlock);
                    Recipe<?> recipe = Recipe.CODEC.parse(accessor.getRegistries().getOps(JsonOps.INSTANCE), recipeJson).getOrThrow();

                    sortedMap.put(recipeId, recipe);
                    count++;
                } catch (Exception e) {
                    Unslab.LOGGER.error("[Unslab] Failed to create recipe for {} -> {}: {}", slab, fullBlock, e.getMessage());
                }
            }
        }

        Unslab.LOGGER.info("[Unslab] Successfully added {} Unslab recipes", count);
    }

    @Unique
    private JsonObject createShapedRecipeJson(Block slab, Block fullBlock) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");

        JsonArray pattern = new JsonArray();
        pattern.add("S");
        pattern.add("S");
        recipe.add("pattern", pattern);

        JsonObject key = new JsonObject();
        key.addProperty("S", getItemIdentifier(slab));
        recipe.add("key", key);

        recipe.addProperty("category", "building");

        JsonObject result = new JsonObject();
        result.addProperty("id", getItemIdentifier(fullBlock));
        result.addProperty("count", 1);
        recipe.add("result", result);

        return recipe;
    }

    @Unique
    private String getItemIdentifier(Block block) {
        Identifier itemId = Registries.ITEM.getId(block.asItem());
        return itemId.toString();
    }

    @Unique
    private boolean isValidBlock(Block block) {
        if (block == null || block.asItem() == null) {
            return false;
        }

        Identifier blockId = Registries.BLOCK.getId(block);
        return !blockId.getPath().equals("air");
    }
}
