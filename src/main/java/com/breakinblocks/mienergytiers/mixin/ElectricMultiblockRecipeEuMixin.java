package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.machines.blockentities.multiblocks.DistillationTowerBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.ElectricBlastFurnaceBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.ElectricCraftingMultiblockBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.FusionReactorBlockEntity;
import com.breakinblocks.mienergytiers.power.RecipeVoltagePolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ElectricCraftingMultiblockBlockEntity.class, ElectricBlastFurnaceBlockEntity.class,
        DistillationTowerBlockEntity.class, FusionReactorBlockEntity.class})
abstract class ElectricMultiblockRecipeEuMixin {
    @Inject(method = "getMaxRecipeEu", at = @At("HEAD"), cancellable = true)
    private void miEnergyTiers$applyVoltageCap(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(((RecipeVoltagePolicy) this).miEnergyTiers$maxRecipeEu());
    }
}
