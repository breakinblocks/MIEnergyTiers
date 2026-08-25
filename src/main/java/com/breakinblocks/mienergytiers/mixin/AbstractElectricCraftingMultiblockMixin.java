package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.CableTierHolder;
import aztech.modern_industrialization.api.machine.holder.EnergyComponentHolder;
import aztech.modern_industrialization.machines.blockentities.multiblocks.AbstractElectricCraftingMultiblockBlockEntity;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.ShapeMatcher;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.util.Simulation;
import com.breakinblocks.mienergytiers.energy.TierUtil;
import com.breakinblocks.mienergytiers.power.HardPowerError;
import com.breakinblocks.mienergytiers.power.HardPowerState;
import com.breakinblocks.mienergytiers.power.HardPowerStateHolder;
import com.breakinblocks.mienergytiers.power.RecipeVoltagePolicy;
import com.breakinblocks.mienergytiers.power.TieredEnergyInput;
import com.breakinblocks.mienergytiers.power.InstantaneousPowerTracker;
import com.breakinblocks.mienergytiers.gui.HardPowerGuiComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractElectricCraftingMultiblockBlockEntity.class)
abstract class AbstractElectricCraftingMultiblockMixin implements HardPowerStateHolder, RecipeVoltagePolicy {
    @Shadow protected List<EnergyComponent> energyInputs;

    @Unique private final HardPowerState miEnergyTiers$powerState = new HardPowerState();
    @Unique private final List<TieredEnergyInput> miEnergyTiers$tieredInputs = new ArrayList<>();
    @Unique private CableTier miEnergyTiers$requiredTier = CableTier.LV;

    @Override
    public HardPowerState miEnergyTiers$getHardPowerState() {
        return miEnergyTiers$powerState;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void miEnergyTiers$registerPowerGui(CallbackInfo ci) {
        ((MachineBlockEntityAccessor) this).miEnergyTiers$registerGuiComponent(
                new HardPowerGuiComponent(() -> miEnergyTiers$powerState));
    }

    @Inject(method = "onRematch", at = @At("TAIL"))
    private void miEnergyTiers$rebuildHatchTiers(ShapeMatcher matcher, CallbackInfo ci) {
        miEnergyTiers$tieredInputs.clear();
        if (!matcher.isMatchSuccessful()) return;
        for (HatchBlockEntity hatch : matcher.getMatchedHatches()) {
            if (hatch instanceof EnergyComponentHolder energyHolder && hatch instanceof CableTierHolder tierHolder
                    && energyHolder.getEnergyComponent() instanceof EnergyComponent component) {
                if (energyInputs.contains(component)) {
                    miEnergyTiers$tieredInputs.add(new TieredEnergyInput(component, tierHolder.getCableTier()));
                }
            }
        }
        miEnergyTiers$tieredInputs.sort(Comparator.comparing(TieredEnergyInput::tier));
    }

    @Override
    public boolean miEnergyTiers$isVoltageAllowed(MachineRecipe recipe) {
        CableTier required = TierUtil.forEu(recipe.eu);
        miEnergyTiers$requiredTier = required;
        CableTier route = miEnergyTiers$routeTier(required);
        if (route == null) {
            miEnergyTiers$powerState.update(recipe.eu, 0, required, miEnergyTiers$highestTier(),
                    HardPowerError.INVALID_HATCH_TIER);
            return false;
        }
        return true;
    }

    @Inject(method = "consumeEu", at = @At("HEAD"), cancellable = true)
    private void miEnergyTiers$atomicHatchDraw(long requested, Simulation simulation,
            CallbackInfoReturnable<Long> cir) {
        long gameTick = ((AbstractElectricCraftingMultiblockBlockEntity) (Object) this).getLevel().getGameTime();
        CableTier required = TierUtil.forEu(requested);
        if (required.compareTo(miEnergyTiers$requiredTier) > 0) miEnergyTiers$requiredTier = required;
        CableTier route = miEnergyTiers$routeTier(miEnergyTiers$requiredTier);
        if (route == null) {
            if (simulation == Simulation.ACT) {
                miEnergyTiers$powerState.update(requested, 0, miEnergyTiers$requiredTier,
                        miEnergyTiers$highestTier(), HardPowerError.INVALID_HATCH_TIER);
            }
            cir.setReturnValue(0L);
            return;
        }

        long available = 0;
        for (TieredEnergyInput input : miEnergyTiers$tieredInputs) {
            if (input.tier() == route) {
                long componentAvailable = Math.min(
                        input.energy().consumeEu(requested - available, Simulation.SIMULATE),
                        InstantaneousPowerTracker.available(input.energy(), gameTick));
                available += Math.min(requested - available, componentAvailable);
                if (available == requested) break;
            }
        }
        if (available != requested) {
            if (simulation == Simulation.ACT) {
                miEnergyTiers$powerState.update(requested, available, miEnergyTiers$requiredTier, route,
                        HardPowerError.INSUFFICIENT_INSTANTANEOUS_POWER);
            }
            cir.setReturnValue(0L);
            return;
        }

        if (simulation == Simulation.ACT) {
            long consumed = 0;
            for (TieredEnergyInput input : miEnergyTiers$tieredInputs) {
                if (input.tier() == route) {
                    long componentDraw = Math.min(requested - consumed,
                            Math.min(input.energy().consumeEu(requested - consumed, Simulation.SIMULATE),
                                    InstantaneousPowerTracker.available(input.energy(), gameTick)));
                    if (componentDraw > 0 && !InstantaneousPowerTracker.spend(input.energy(), gameTick, componentDraw)) {
                        throw new IllegalStateException("Instantaneous MI hatch budget changed between simulation and commit");
                    }
                    consumed += input.energy().consumeEu(componentDraw, Simulation.ACT);
                    if (consumed == requested) break;
                }
            }
            if (consumed != requested) {
                throw new IllegalStateException("MI hatch energy changed between atomic simulation and commit");
            }
            miEnergyTiers$powerState.clear(requested, requested, miEnergyTiers$requiredTier, route);
        }
        cir.setReturnValue(requested);
    }

    @Unique
    private CableTier miEnergyTiers$routeTier(CableTier required) {
        return TierUtil.routeTier(miEnergyTiers$tieredInputs.stream().map(TieredEnergyInput::tier).toList(), required);
    }

    @Unique
    private CableTier miEnergyTiers$highestTier() {
        return miEnergyTiers$tieredInputs.stream().map(TieredEnergyInput::tier)
                .max(Comparator.naturalOrder()).orElse(null);
    }

}
