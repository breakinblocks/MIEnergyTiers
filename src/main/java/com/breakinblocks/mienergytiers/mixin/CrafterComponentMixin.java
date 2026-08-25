package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import com.breakinblocks.mienergytiers.power.RecipeVoltagePolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CrafterComponent.class)
abstract class CrafterComponentMixin {
    @Redirect(method = "updateActiveRecipe", at = @At(value = "INVOKE",
            target = "Laztech/modern_industrialization/machines/components/CrafterComponent$Behavior;banRecipe(Laztech/modern_industrialization/machines/recipe/MachineRecipe;)Z"))
    private boolean miEnergyTiers$preserveBansAndCheckVoltage(CrafterComponent.Behavior behavior, MachineRecipe recipe) {
        if (behavior.banRecipe(recipe)) return true;
        return behavior instanceof RecipeVoltagePolicy policy && !policy.miEnergyTiers$isVoltageAllowed(recipe);
    }
}
