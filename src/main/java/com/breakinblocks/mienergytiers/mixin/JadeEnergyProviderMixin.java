package com.breakinblocks.mienergytiers.mixin;

import com.breakinblocks.mienergytiers.power.HardPowerStateHolder;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ViewGroup;

@Pseudo
@Mixin(targets = "aztech.modern_industrialization.compat.jade.server.MachineComponentProvider$Energy", remap = false)
abstract class JadeEnergyProviderMixin {
    @Inject(method = "getGroups", at = @At("HEAD"), cancellable = true, remap = false)
    private void miEnergyTiers$hideCraftingBuffer(Accessor<?> accessor,
            CallbackInfoReturnable<List<ViewGroup<CompoundTag>>> cir) {
        if (accessor.getTarget() instanceof HardPowerStateHolder) {
            cir.setReturnValue(List.of());
        }
    }
}
