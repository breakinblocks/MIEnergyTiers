package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.api.energy.CableTier;
import com.breakinblocks.mienergytiers.config.HardEnergyConfig;
import com.breakinblocks.mienergytiers.energy.EnergyTransferContext;
import com.breakinblocks.mienergytiers.energy.TierAwareEndpoint;
import com.breakinblocks.mienergytiers.energy.ExternalEnergyPolicy;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "aztech.modern_industrialization.api.energy.EnergyApi$WrappedExternalStorage")
abstract class WrappedExternalStorageMixin {
    @Shadow @Final private ILongEnergyStorage externalStorage;

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void miEnergyTiers$rejectUntypedIngress(long maxExtract, boolean simulate,
            CallbackInfoReturnable<Long> cir) {
        EnergyTransferContext context = EnergyTransferContext.current();
        if (!ExternalEnergyPolicy.allowsInput(externalStorage, context)) {
            cir.setReturnValue(0L);
        }
    }
}
