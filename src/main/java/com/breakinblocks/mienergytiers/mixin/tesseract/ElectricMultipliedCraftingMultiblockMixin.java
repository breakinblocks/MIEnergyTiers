package com.breakinblocks.mienergytiers.mixin.tesseract;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.machines.components.UpgradeComponent;
import aztech.modern_industrialization.machines.multiblocks.ShapeMatcher;
import aztech.modern_industrialization.util.Simulation;
import com.breakinblocks.mienergytiers.energy.AmperagePolicy;
import com.breakinblocks.mienergytiers.energy.TierUtil;
import com.breakinblocks.mienergytiers.gui.HardPowerGuiComponent;
import com.breakinblocks.mienergytiers.mixin.MachineBlockEntityAccessor;
import com.breakinblocks.mienergytiers.power.HardPowerError;
import com.breakinblocks.mienergytiers.power.HardPowerState;
import com.breakinblocks.mienergytiers.power.HardPowerStateHolder;
import com.breakinblocks.mienergytiers.power.HatchDrawPlan;
import com.breakinblocks.mienergytiers.power.HatchTierCache;
import com.breakinblocks.mienergytiers.power.InstantaneousPowerTracker;
import com.breakinblocks.mienergytiers.power.TieredEnergyInput;
import java.util.Comparator;
import java.util.List;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.MultipliedCrafterComponent;
import net.swedz.tesseract.neoforge.compat.mi.machine.blockentity.multiblock.multiplied.AbstractElectricMultipliedCraftingMultiblockBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tesseract's multiplied ("batch") multiblocks do not extend MI's electric crafting multiblock, so
 * none of the enforcement aimed at {@code ElectricCraftingMultiblockBlockEntity} reaches them. They
 * do expose the same shape of hooks through {@code ModularCrafterAccessBehavior}, which is what this
 * uses: the voltage cap comes back through {@code getMaxRecipeEu}, a recipe above the installed tier
 * is refused through {@code isRecipeBanned}, and the draw itself goes through the same atomic hatch
 * path the MI multiblocks use.
 */
@Mixin(AbstractElectricMultipliedCraftingMultiblockBlockEntity.class)
abstract class ElectricMultipliedCraftingMultiblockMixin implements HardPowerStateHolder {
    @Shadow(remap = false)
    protected List<EnergyComponent> energyInputs;

    @Shadow(remap = false)
    protected UpgradeComponent upgrades;

    @Unique
    private final HardPowerState miEnergyTiers$powerState = new HardPowerState();

    @Unique
    private final HatchTierCache miEnergyTiers$hatchCache = new HatchTierCache();

    @Override
    public HardPowerState miEnergyTiers$getHardPowerState() {
        return miEnergyTiers$powerState;
    }

    @Inject(method = "onRematch", at = @At("TAIL"), remap = false)
    private void miEnergyTiers$rebuildHatchTiers(ShapeMatcher matcher, CallbackInfo ci) {
        if (!matcher.isMatchSuccessful()) {
            miEnergyTiers$hatchCache.clear();
            return;
        }
        miEnergyTiers$hatchCache.rebuild(matcher.getMatchedHatches(), energyInputs);
    }

    /**
     * Overrides the {@code ModularCrafterAccessBehavior} default. The bonus from upgrades is folded
     * in here rather than added on afterwards, so an upgraded machine still cannot outrun its hatches.
     */
    @Unique
    public long getMaxRecipeEu() {
        return miEnergyTiers$cap();
    }

    @Inject(method = "getBaseMaxRecipeEu", at = @At("HEAD"), cancellable = true, remap = false)
    private void miEnergyTiers$capBaseRecipeEu(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(miEnergyTiers$cap());
    }

    /**
     * Overrides the {@code ModularCrafterAccessBehavior} default so a recipe whose tier the installed
     * hatches cannot route is refused outright rather than run slowly.
     */
    @Unique
    public boolean isRecipeBanned(long recipeEu) {
        CableTier required = TierUtil.forEu(recipeEu);
        if (miEnergyTiers$route(required) == null) {
            miEnergyTiers$powerState.update(
                    recipeEu, 0, required, miEnergyTiers$highestTier(), HardPowerError.INVALID_HATCH_TIER);
            return true;
        }
        return false;
    }

    @Inject(method = "consumeEu", at = @At("HEAD"), cancellable = true, remap = false)
    private void miEnergyTiers$atomicHatchDraw(
            long requested, Simulation simulation, CallbackInfoReturnable<Long> cir) {
        AbstractElectricMultipliedCraftingMultiblockBlockEntity machine =
                (AbstractElectricMultipliedCraftingMultiblockBlockEntity) (Object) this;
        if (machine.getLevel() == null) {
            cir.setReturnValue(0L);
            return;
        }
        long gameTick = machine.getLevel().getGameTime();
        CableTier required = TierUtil.forEu(machine.getBaseRecipeEu() > 0 ? machine.getBaseRecipeEu() : requested);
        TierUtil.HatchRoute route = miEnergyTiers$route(required);
        if (route == null) {
            if (simulation == Simulation.ACT) {
                miEnergyTiers$powerState.update(
                        requested, 0, required, miEnergyTiers$highestTier(), HardPowerError.INVALID_HATCH_TIER);
            }
            cir.setReturnValue(0L);
            return;
        }

        long draw = Math.min(requested, miEnergyTiers$cap());
        if (draw <= 0) {
            cir.setReturnValue(0L);
            return;
        }

        List<TieredEnergyInput> routed = miEnergyTiers$routedInputs(route, gameTick);
        HatchDrawPlan plan = HatchDrawPlan.plan(routed, draw, gameTick);
        if (plan.total() != draw) {
            long unit = miEnergyTiers$batchUnitEu();
            long partial = unit > 0 && unit < draw ? plan.total() / unit * unit : 0;
            if (partial > 0) {
                HatchDrawPlan partialPlan = HatchDrawPlan.plan(routed, partial, gameTick);
                if (partialPlan.total() == partial) {
                    if (simulation == Simulation.ACT) {
                        partialPlan.commit();
                        miEnergyTiers$powerState.clear(draw, partial, required, route.inputTier());
                    }
                    cir.setReturnValue(partial);
                    return;
                }
            }
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
    private long miEnergyTiers$batchUnitEu() {
        AbstractElectricMultipliedCraftingMultiblockBlockEntity machine =
                (AbstractElectricMultipliedCraftingMultiblockBlockEntity) (Object) this;
        if (!(machine.getCrafterComponent() instanceof MultipliedCrafterComponent crafter)
                || !crafter.hasActiveRecipe()) {
            return 0;
        }
        int multiplier = crafter.getRecipeMultiplier();
        if (multiplier <= 1) return 0;
        long unitEu = Math.max(machine.getBaseRecipeEu(), crafter.getBaseRecipeEu());
        return Math.max(1, crafter.transformEuCost(unitEu, 0) / multiplier);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void miEnergyTiers$registerPowerGui(CallbackInfo ci) {
        AbstractElectricMultipliedCraftingMultiblockBlockEntity machine =
                (AbstractElectricMultipliedCraftingMultiblockBlockEntity) (Object) this;
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

    @Unique
    private long miEnergyTiers$cap() {
        miEnergyTiers$hatchCache.refresh();
        CableTier voltage = miEnergyTiers$hatchCache.voltage();
        if (voltage == null) return 0;
        long upgradeEu = upgrades == null ? 0L : upgrades.getAddMaxEUPerTick();
        return Math.min(AmperagePolicy.maxRecipeEu(voltage, upgradeEu), miEnergyTiers$hatchCache.capacity());
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
