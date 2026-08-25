package com.breakinblocks.mienergytiers.power;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import java.util.Map;
import java.util.WeakHashMap;
import org.jspecify.annotations.Nullable;

public final class InstantaneousPowerTracker {
    private static final Map<EnergyComponent, InstantaneousPowerBudget> BUDGETS = new WeakHashMap<>();

    public static synchronized void receive(EnergyComponent component, long tick, long amount, @Nullable CableTier tier) {
        budget(component).receive(tick, amount, tier);
    }

    public static synchronized long available(EnergyComponent component, long tick) {
        return budget(component).available(tick);
    }

    public static synchronized long received(EnergyComponent component, long tick) {
        return budget(component).received(tick);
    }

    public static synchronized long received(Iterable<EnergyComponent> components, long tick) {
        long total = 0;
        for (EnergyComponent component : components) {
            long received = budget(component).received(tick);
            total = received > Long.MAX_VALUE - total ? Long.MAX_VALUE : total + received;
        }
        return total;
    }

    public static synchronized @Nullable CableTier inputTier(EnergyComponent component, long tick) {
        return budget(component).inputTier(tick);
    }

    public static synchronized boolean spend(EnergyComponent component, long tick, long amount) {
        return budget(component).spend(tick, amount);
    }

    private static InstantaneousPowerBudget budget(EnergyComponent component) {
        return BUDGETS.computeIfAbsent(component, ignored -> new InstantaneousPowerBudget());
    }

    private InstantaneousPowerTracker() {}
}
