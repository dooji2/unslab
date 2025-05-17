package com.dooji.unslab.mixin;

import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerRecipeManager.class)
public interface ServerRecipeManagerAccessor {
    @Accessor
    WrapperLookup getRegistries();
}
