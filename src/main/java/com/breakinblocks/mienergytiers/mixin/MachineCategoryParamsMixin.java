package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.compat.rei.machines.MachineCategoryParams;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MachineCategoryParams.class, remap = false)
abstract class MachineCategoryParamsMixin {
    @Inject(method = "upgradeEuRequired", at = @At("HEAD"), cancellable = true)
    private void miEnergyTiers$voltageReplacesUpgradeRequirement(
            MachineRecipe recipe, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }
}
