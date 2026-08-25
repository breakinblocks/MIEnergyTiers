package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import com.breakinblocks.mienergytiers.config.HardEnergyConfig;
import com.breakinblocks.mienergytiers.power.HardPowerError;
import com.breakinblocks.mienergytiers.power.HardPowerStateHolder;
import com.breakinblocks.mienergytiers.power.RecipeVoltagePolicy;
import com.breakinblocks.mienergytiers.power.UnderpowerPolicy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrafterComponent.class)
abstract class CrafterComponentMixin {
    @Shadow private CrafterComponent.Behavior behavior;
    @Shadow private RecipeHolder<MachineRecipe> activeRecipe;
    @Shadow private ResourceLocation delayedActiveRecipe;
    @Shadow private boolean matchesMultipleRecipes;
    @Shadow private long usedEnergy;
    @Shadow private long recipeEnergy;
    @Shadow private long recipeMaxEu;
    @Shadow private int efficiencyTicks;
    @Shadow private int maxEfficiencyTicks;
    @Shadow private long previousBaseEu;
    @Shadow private long previousMaxEu;
    @Shadow protected abstract void clearLocks();

    @Redirect(method = "updateActiveRecipe", at = @At(value = "INVOKE",
            target = "Laztech/modern_industrialization/machines/components/CrafterComponent$Behavior;banRecipe(Laztech/modern_industrialization/machines/recipe/MachineRecipe;)Z"))
    private boolean miEnergyTiers$preserveBansAndCheckVoltage(CrafterComponent.Behavior behavior, MachineRecipe recipe) {
        if (behavior.banRecipe(recipe)) return true;
        return behavior instanceof RecipeVoltagePolicy policy && !policy.miEnergyTiers$isVoltageAllowed(recipe);
    }

    @Inject(method = "tickRecipe", at = @At("TAIL"), cancellable = true)
    private void miEnergyTiers$decayUnderpoweredRecipe(CallbackInfoReturnable<Boolean> cir) {
        UnderpowerPolicy policy = HardEnergyConfig.UNDERPOWER_POLICY.get();
        if (policy == UnderpowerPolicy.PRESERVE_PROGRESS
                || activeRecipe == null
                || !(behavior instanceof HardPowerStateHolder holder)
                || holder.miEnergyTiers$getHardPowerState().error()
                        != HardPowerError.INSUFFICIENT_INSTANTANEOUS_POWER) {
            return;
        }

        long floor = policy == UnderpowerPolicy.DECAY_ONLY ? 1 : 0;
        usedEnergy = Math.max(floor, usedEnergy - Math.max(1, recipeMaxEu));
        if (usedEnergy == 0 && policy == UnderpowerPolicy.DECAY_AND_WASTE_INPUTS) {
            // Inputs were consumed by MI when the recipe started. Clearing the active recipe here
            // intentionally refunds nothing and emits no outputs.
            activeRecipe = null;
            delayedActiveRecipe = null;
            matchesMultipleRecipes = false;
            recipeEnergy = 0;
            recipeMaxEu = 0;
            efficiencyTicks = 0;
            maxEfficiencyTicks = 0;
            previousBaseEu = -1;
            previousMaxEu = -1;
            clearLocks();
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "getProgress", at = @At("HEAD"), cancellable = true)
    private void miEnergyTiers$displayDecayFloorAsZero(CallbackInfoReturnable<Float> cir) {
        // MI treats an internal usedEnergy value of zero as permission to find a new recipe.
        // Keep one internal EU as a sentinel so the consumed inputs remain associated with this
        // active craft, while exposing the policy's logical floor as zero progress.
        if (HardEnergyConfig.UNDERPOWER_POLICY.get() == UnderpowerPolicy.DECAY_ONLY
                && activeRecipe != null && usedEnergy == 1) {
            cir.setReturnValue(0.0F);
        }
    }
}
