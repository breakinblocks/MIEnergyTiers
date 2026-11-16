package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.machines.blockentities.hatches.EnergyHatch;
import com.breakinblocks.mienergytiers.energy.TierUtil;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EnergyHatch.class)
abstract class EnergyHatchMixin {
    @ModifyArg(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Laztech/modern_industrialization/machines/components/EnergyComponent;<init>(Lnet/minecraft/world/level/block/entity/BlockEntity;J)V"),
            index = 1)
    private long miEnergyTiers$passThroughCapacity(
            long capacity, @Local(argsOnly = true) boolean input, @Local(argsOnly = true) CableTier tier) {
        return input ? TierUtil.maxHatchEuPerTick(tier) : capacity;
    }
}
