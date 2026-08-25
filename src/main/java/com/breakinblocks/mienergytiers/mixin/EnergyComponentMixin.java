package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.machines.blockentities.TransformerMachineBlockEntity;
import aztech.modern_industrialization.util.Simulation;
import com.breakinblocks.mienergytiers.energy.EnergyTransferContext;
import com.breakinblocks.mienergytiers.power.InstantaneousPowerTracker;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnergyComponent.class)
abstract class EnergyComponentMixin {
    @Shadow @Final private BlockEntity blockEntity;

    @Inject(method = "insertEu", at = @At("RETURN"))
    private void miEnergyTiers$recordFreshPower(long maximum, Simulation simulation,
            CallbackInfoReturnable<Long> cir) {
        if (maximum <= 0 || blockEntity.getLevel() == null) return;
        EnergyTransferContext context = EnergyTransferContext.current();
        if (context == null) return;
        // MI simulates every target before acting. Reserve source-backed throughput during that
        // simulation even when a full target accepts zero EU, avoiding a full-buffer deadlock.
        // The shared transfer context makes later simulation/acting calls see the reduced budget.
        long fresh = context.claimFresh(maximum);
        if (fresh == 0) return;
        InstantaneousPowerTracker.receive((EnergyComponent) (Object) this, blockEntity.getLevel().getGameTime(),
                fresh, context.tier());
    }

    @ModifyVariable(method = "consumeEu", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long miEnergyTiers$capTransformerOutput(long maximum) {
        if (!(blockEntity instanceof TransformerMachineBlockEntity) || blockEntity.getLevel() == null
                || EnergyTransferContext.current() == null) return maximum;
        return Math.min(maximum, InstantaneousPowerTracker.available(
                (EnergyComponent) (Object) this, blockEntity.getLevel().getGameTime()));
    }

    @Inject(method = "consumeEu", at = @At("RETURN"))
    private void miEnergyTiers$spendTransformerInputBudget(long maximum, Simulation simulation,
            CallbackInfoReturnable<Long> cir) {
        if (simulation != Simulation.ACT || cir.getReturnValue() <= 0
                || !(blockEntity instanceof TransformerMachineBlockEntity) || blockEntity.getLevel() == null
                || EnergyTransferContext.current() == null) return;
        if (!InstantaneousPowerTracker.spend((EnergyComponent) (Object) this,
                blockEntity.getLevel().getGameTime(), cir.getReturnValue())) {
            throw new IllegalStateException("Transformer fresh-input budget changed during extraction");
        }
    }
}
