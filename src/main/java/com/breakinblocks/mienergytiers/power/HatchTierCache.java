package com.breakinblocks.mienergytiers.power;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.CableTierHolder;
import aztech.modern_industrialization.api.machine.holder.EnergyComponentHolder;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import com.breakinblocks.mienergytiers.energy.TierUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class HatchTierCache {
    private record Entry(HatchBlockEntity hatch, EnergyComponent energy, CableTier tier) {}

    private final List<Entry> entries = new ArrayList<>();
    private final List<TieredEnergyInput> inputs = new ArrayList<>();
    private @Nullable CableTier voltage;
    private long capacity;

    public void clear() {
        entries.clear();
        recompute();
    }

    public void rebuild(Iterable<HatchBlockEntity> hatches, Collection<EnergyComponent> energyInputs) {
        entries.clear();
        for (HatchBlockEntity hatch : hatches) {
            Entry entry = entryFor(hatch);
            if (entry != null && energyInputs.contains(entry.energy())) entries.add(entry);
        }
        recompute();
    }

    public void refresh() {
        boolean changed = false;
        for (int index = entries.size() - 1; index >= 0; index--) {
            Entry cached = entries.get(index);
            Entry current = cached.hatch().isRemoved() ? null : entryFor(cached.hatch());
            if (current == null) {
                entries.remove(index);
                changed = true;
            } else if (current.tier() != cached.tier() || current.energy() != cached.energy()) {
                entries.set(index, current);
                changed = true;
            }
        }
        if (changed) recompute();
    }

    public List<TieredEnergyInput> inputs() {
        return inputs;
    }

    public List<CableTier> tiers() {
        return inputs.stream().map(TieredEnergyInput::tier).toList();
    }

    public @Nullable CableTier voltage() {
        return voltage;
    }

    public long capacity() {
        return capacity;
    }

    private static @Nullable Entry entryFor(HatchBlockEntity hatch) {
        if (hatch instanceof EnergyComponentHolder energyHolder
                && hatch instanceof CableTierHolder tierHolder
                && energyHolder.getEnergyComponent() instanceof EnergyComponent component) {
            return new Entry(hatch, component, tierHolder.getCableTier());
        }
        return null;
    }

    private void recompute() {
        inputs.clear();
        for (Entry entry : entries) inputs.add(new TieredEnergyInput(entry.energy(), entry.tier()));
        inputs.sort(Comparator.comparing(TieredEnergyInput::tier));
        List<CableTier> tiers = tiers();
        voltage = TierUtil.effectiveVoltage(tiers);
        TierUtil.HatchRoute route = voltage == null ? null : TierUtil.hatchRoute(tiers, voltage);
        capacity = route == null ? 0 : TierUtil.hatchCapacity(tiers, route);
    }
}
