package com.breakinblocks.mienergytiers.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.IElement;

@Pseudo
@Mixin(targets = "aztech.modern_industrialization.client.compat.jade.PipeComponentProvider", remap = false)
abstract class PipeComponentProviderMixin {
    @WrapWithCondition(
            method =
                    "appendTooltip(Lsnownee/jade/api/ITooltip;Lsnownee/jade/api/BlockAccessor;Lsnownee/jade/api/config/IPluginConfig;)V",
            slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=maxEu")),
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lsnownee/jade/api/ITooltip;add(Lsnownee/jade/api/ui/IElement;)V",
                            ordinal = 0),
            remap = false)
    private boolean miEnergyTiers$hideCableBuffer(ITooltip tooltip, IElement storedEnergy) {
        return false;
    }
}
