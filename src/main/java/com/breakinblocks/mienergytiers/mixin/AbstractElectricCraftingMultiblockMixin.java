package com.breakinblocks.mienergytiers.mixin;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.machines.blockentities.multiblocks.AbstractElectricCraftingMultiblockBlockEntity;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.machines.components.UpgradeComponent;
import aztech.modern_industrialization.machines.multiblocks.ShapeMatcher;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.util.Simulation;
import com.breakinblocks.mienergytiers.energy.AmperagePolicy;
import com.breakinblocks.mienergytiers.energy.TierUtil;
import com.breakinblocks.mienergytiers.gui.HardPowerGuiComponent;
import com.breakinblocks.mienergytiers.power.EnergyFreeRecipes;
import com.breakinblocks.mienergytiers.power.HardPowerError;
import com.breakinblocks.mienergytiers.power.HardPowerState;
import com.breakinblocks.mienergytiers.power.HardPowerStateHolder;
import com.breakinblocks.mienergytiers.power.HatchDrawPlan;
import com.breakinblocks.mienergytiers.power.HatchTierCache;
import com.breakinblocks.mienergytiers.power.InstantaneousPowerTracker;
import com.breakinblocks.mienergytiers.power.RecipeVoltagePolicy;
import com.breakinblocks.mienergytiers.power.TieredEnergyInput;
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
    @Shadow
    protected List<EnergyComponent> energyInputs;

    @Unique
    private final HardPowerState miEnergyTiers$powerState = new HardPowerState();

    @Unique
    private final HatchTierCache miEnergyTiers$hatchCache = new HatchTierCache();

    @Override
    public HardPowerState miEnergyTiers$getHardPowerState() {
        return miEnergyTiers$powerState;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void miEnergyTiers$registerPowerGui(CallbackInfo ci) {
        // Multiblock status panels contain variable-width text, so keep the power lamp in the
        // otherwise unused top-right corner of the title bar instead of overlaying panel content.
        AbstractElectricCraftingMultiblockBlockEntity machine =
                (AbstractElectricCraftingMultiblockBlockEntity) (Object) this;
        ((MachineBlockEntityAccessor) this)
                .miEnergyTiers$registerGuiComponent(new HardPowerGuiComponent(
                        () -> miEnergyTiers$powerState,
                        machine.getCrafterComponent()::hasActiveRecipe,
                        () -> machine.getLevel() == null
                                ? 0
                                : InstantaneousPowerTracker.received(
                                        energyInputs, machine.getLevel().getGameTime()),
                        161,
                        4,
                        true));
    }

    @Inject(method = "onRematch", at = @At("TAIL"))
    private void miEnergyTiers$rebuildHatchTiers(ShapeMatcher matcher, CallbackInfo ci) {
        if (!matcher.isMatchSuccessful()) {
            miEnergyTiers$hatchCache.clear();
            return;
        }
        miEnergyTiers$hatchCache.rebuild(matcher.getMatchedHatches(), energyInputs);
    }

    @Override
    public long miEnergyTiers$maxRecipeEu() {
        miEnergyTiers$hatchCache.refresh();
        CableTier voltage = miEnergyTiers$hatchCache.voltage();
        if (voltage == null) return 0;
        long upgradeEu = ((AbstractElectricCraftingMultiblockBlockEntity) (Object) this)
                .components.mapOrDefault(UpgradeComponent.class, UpgradeComponent::getAddMaxEUPerTick, 0L);
        return Math.min(AmperagePolicy.maxRecipeEu(voltage, upgradeEu), miEnergyTiers$hatchCache.capacity());
    }

    @Override
    public boolean miEnergyTiers$hasStartPower(MachineRecipe recipe) {
        AbstractElectricCraftingMultiblockBlockEntity machine =
                (AbstractElectricCraftingMultiblockBlockEntity) (Object) this;
        if (EnergyFreeRecipes.isFree(recipe) || machine.getLevel() == null) return true;
        long fresh = InstantaneousPowerTracker.received(
                energyInputs, machine.getLevel().getGameTime());
        if (fresh >= recipe.eu) return true;
        miEnergyTiers$powerState.update(
                recipe.eu,
                fresh,
                TierUtil.forEu(recipe.eu),
                miEnergyTiers$highestTier(),
                HardPowerError.INSUFFICIENT_INSTANTANEOUS_POWER);
        return false;
    }

    @Override
    public boolean miEnergyTiers$isVoltageAllowed(MachineRecipe recipe) {
        if (EnergyFreeRecipes.isFree(recipe)) {
            return true;
        }
        CableTier required = TierUtil.forEu(recipe.eu);
        TierUtil.HatchRoute route = miEnergyTiers$route(required);
        if (route == null) {
            miEnergyTiers$powerState.update(
                    recipe.eu, 0, required, miEnergyTiers$highestTier(), HardPowerError.INVALID_HATCH_TIER);
            return false;
        }
        return true;
    }

    @Inject(method = "consumeEu", at = @At("HEAD"), cancellable = true)
    private void miEnergyTiers$atomicHatchDraw(
            long requested, Simulation simulation, CallbackInfoReturnable<Long> cir) {
        long gameTick = ((AbstractElectricCraftingMultiblockBlockEntity) (Object) this)
                .getLevel()
                .getGameTime();
        CableTier required = miEnergyTiers$recipeTier(requested);
        TierUtil.HatchRoute route = miEnergyTiers$route(required);
        if (route == null) {
            if (simulation == Simulation.ACT) {
                miEnergyTiers$powerState.update(
                        requested, 0, required, miEnergyTiers$highestTier(), HardPowerError.INVALID_HATCH_TIER);
            }
            cir.setReturnValue(0L);
            return;
        }

        long draw = Math.min(requested, miEnergyTiers$maxRecipeEu());
        if (draw <= 0) {
            cir.setReturnValue(0L);
            return;
        }

        HatchDrawPlan plan = HatchDrawPlan.plan(miEnergyTiers$routedInputs(route, gameTick), draw, gameTick);
        if (plan.total() != draw) {
            if (simulation == Simulation.ACT) {
                miEnergyTiers$powerState.update(
                        draw,
                        plan.total(),
                        required,
                        route.inputTier(),
                        HardPowerError.INSUFFICIENT_INSTANTANEOUS_POWER);
            }
            cir.setReturnValue(0L);
            return;
        }

        if (simulation == Simulation.ACT) {
            plan.commit();
            miEnergyTiers$powerState.clear(draw, draw, required, route.inputTier());
        }
        cir.setReturnValue(draw);
    }

    @Unique
    private CableTier miEnergyTiers$recipeTier(long requested) {
        CrafterComponent crafter =
                ((AbstractElectricCraftingMultiblockBlockEntity) (Object) this).getCrafterComponent();
        return TierUtil.forEu(crafter.hasActiveRecipe() ? crafter.getBaseRecipeEu() : requested);
    }

    @Unique
    private TierUtil.HatchRoute miEnergyTiers$route(CableTier required) {
        miEnergyTiers$hatchCache.refresh();
        return TierUtil.hatchRoute(miEnergyTiers$hatchCache.tiers(), required);
    }

    @Unique
    private List<TieredEnergyInput> miEnergyTiers$routedInputs(TierUtil.HatchRoute route, long gameTick) {
        List<TieredEnergyInput> routed = miEnergyTiers$hatchCache.inputs().stream()
                .filter(input -> input.tier() == route.inputTier())
                .sorted(Comparator.comparingLong((TieredEnergyInput input) ->
                                miEnergyTiers$availableFromHatch(input, Long.MAX_VALUE, gameTick))
                        .reversed())
                .toList();
        return routed.size() <= route.maxHatches() ? routed : routed.subList(0, route.maxHatches());
    }

    @Unique
    private long miEnergyTiers$availableFromHatch(TieredEnergyInput input, long requested, long gameTick) {
        long hatchLimit = TierUtil.maxHatchEuPerTick(input.tier());
        return Math.min(
                requested,
                Math.min(
                        hatchLimit,
                        Math.min(
                                input.energy().consumeEu(Math.min(requested, hatchLimit), Simulation.SIMULATE),
                                InstantaneousPowerTracker.available(input.energy(), gameTick))));
    }

    @Unique
    private CableTier miEnergyTiers$highestTier() {
        return miEnergyTiers$hatchCache.inputs().stream()
                .map(TieredEnergyInput::tier)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
