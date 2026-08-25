package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.machines.blockentities.ElectricCraftingMachineBlockEntity;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.machines.guicomponents.EnergyBar;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.util.Simulation;
import com.breakinblocks.mienergytiers.energy.TierUtil;
import com.breakinblocks.mienergytiers.power.HardPowerError;
import com.breakinblocks.mienergytiers.power.HardPowerState;
import com.breakinblocks.mienergytiers.power.HardPowerStateHolder;
import com.breakinblocks.mienergytiers.power.RecipeVoltagePolicy;
import com.breakinblocks.mienergytiers.power.InstantaneousPowerTracker;
import com.breakinblocks.mienergytiers.gui.HardPowerGuiComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElectricCraftingMachineBlockEntity.class)
abstract class ElectricCraftingMachineMixin implements HardPowerStateHolder, RecipeVoltagePolicy {
    @Shadow @Final private EnergyComponent energy;
    @Shadow public abstract CableTier getCableTier();

    @Unique private final HardPowerState miEnergyTiers$powerState = new HardPowerState();

    @Override
    public HardPowerState miEnergyTiers$getHardPowerState() {
        return miEnergyTiers$powerState;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void miEnergyTiers$registerPowerGui(CallbackInfo ci) {
        ElectricCraftingMachineBlockEntity machine = (ElectricCraftingMachineBlockEntity) (Object) this;
        EnergyBar energyBar = machine.guiComponents.getNullable(EnergyBar.class);
        int renderX = energyBar == null ? 39 : energyBar.params.renderX();
        int renderY = energyBar == null ? 65 : energyBar.params.renderY();
        if (energyBar != null) machine.guiComponents.unregister(energyBar);
        ((MachineBlockEntityAccessor) this).miEnergyTiers$registerGuiComponent(
                new HardPowerGuiComponent(() -> miEnergyTiers$powerState,
                        machine.getCrafterComponent()::hasActiveRecipe, renderX, renderY, false));
    }

    @Override
    public boolean miEnergyTiers$isVoltageAllowed(MachineRecipe recipe) {
        CableTier required = TierUtil.forEu(recipe.eu);
        CableTier installed = getCableTier();
        boolean allowed = installed.compareTo(required) >= 0;
        if (!allowed) {
            miEnergyTiers$powerState.update(recipe.eu, 0, required, installed, HardPowerError.WRONG_RECIPE_TIER);
        }
        return allowed;
    }

    @Inject(method = "consumeEu", at = @At("HEAD"), cancellable = true)
    private void miEnergyTiers$atomicDraw(long requested, Simulation simulation,
            CallbackInfoReturnable<Long> cir) {
        CableTier installed = getCableTier();
        CableTier required = TierUtil.forEu(requested);
        long gameTick = ((ElectricCraftingMachineBlockEntity) (Object) this).getLevel().getGameTime();
        CableTier inputTier = InstantaneousPowerTracker.inputTier(energy, gameTick);
        if (requested > installed.getEu()) {
            if (simulation == Simulation.ACT) {
                miEnergyTiers$powerState.update(requested, 0, required, installed, HardPowerError.WRONG_RECIPE_TIER);
            }
            cir.setReturnValue(0L);
            return;
        }

        long available = Math.min(energy.consumeEu(requested, Simulation.SIMULATE),
                InstantaneousPowerTracker.available(energy, gameTick));
        if (available != requested) {
            if (simulation == Simulation.ACT) {
                miEnergyTiers$powerState.update(requested, available, required, inputTier,
                        HardPowerError.INSUFFICIENT_INSTANTANEOUS_POWER);
            }
            cir.setReturnValue(0L);
            return;
        }

        if (simulation == Simulation.ACT) {
            if (!InstantaneousPowerTracker.spend(energy, gameTick, requested)) {
                throw new IllegalStateException("Instantaneous MI power budget changed between simulation and commit");
            }
            long consumed = energy.consumeEu(requested, Simulation.ACT);
            if (consumed != requested) {
                throw new IllegalStateException("MI energy storage changed between atomic simulation and commit");
            }
            miEnergyTiers$powerState.clear(requested, requested, required, inputTier);
        }
        cir.setReturnValue(requested);
    }
}
