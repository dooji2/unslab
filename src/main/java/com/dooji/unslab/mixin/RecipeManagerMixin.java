package com.dooji.unslab.mixin;

import com.dooji.unslab.UnslabMapping;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.registry.Registries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At("HEAD"))
    private void injectCustomRecipes(Map<Identifier, JsonElement> recipeMap, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
        UnslabMapping.getSlabToBlockMap().forEach((slab, fullBlock) -> {
            if (isValidBlock(slab) && isValidBlock(fullBlock)) {
                String fullBlockPath = getItemIdentifier(fullBlock).replace(':', '_');
                Identifier craftingId = Identifier.of("unslab", "crafting/" + fullBlockPath);

                recipeMap.put(craftingId, createCraftingRecipe(slab, fullBlock));
            }
        });
    }

    @Unique
    private JsonObject createCraftingRecipe(Block slab, Block fullBlock) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");

        JsonArray pattern = new JsonArray();
        pattern.add("S");
        pattern.add("S");
        recipe.add("pattern", pattern);

        JsonObject key = new JsonObject();

        JsonObject slabJson = new JsonObject();
        slabJson.addProperty("item", getItemIdentifier(slab));
        key.add("S", slabJson);
        recipe.add("key", key);

        JsonObject result = new JsonObject();
        result.addProperty("item", getItemIdentifier(fullBlock));
        result.addProperty("Count", 1);
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