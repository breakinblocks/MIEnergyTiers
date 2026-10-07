package com.breakinblocks.mienergytiers.power;

import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.util.Simulation;
import com.breakinblocks.mienergytiers.energy.TierUtil;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class HatchDrawPlan {
    private final List<TieredEnergyInput> inputs;
    private final long[] draws;
    private final long total;
    private final long gameTick;

    private HatchDrawPlan(List<TieredEnergyInput> inputs, long[] draws, long total, long gameTick) {
        this.inputs = inputs;
        this.draws = draws;
        this.total = total;
        this.gameTick = gameTick;
    }

    public static HatchDrawPlan plan(List<TieredEnergyInput> inputs, long target, long gameTick) {
        long[] draws = new long[inputs.size()];
        Map<InstantaneousPowerBudget, Long> reserved = new IdentityHashMap<>();
        long total = 0;
        for (int index = 0; index < inputs.size() && total < target; index++) {
            TieredEnergyInput input = inputs.get(index);
            InstantaneousPowerBudget budget = InstantaneousPowerTracker.budget(input.energy());
            long fresh = budget.available(gameTick) - reserved.getOrDefault(budget, 0L);
            long request = Math.min(target - total, Math.min(TierUtil.maxHatchEuPerTick(input.tier()), fresh));
            if (request <= 0) continue;
            long draw = input.energy().consumeEu(request, Simulation.SIMULATE);
            if (draw <= 0) continue;
            reserved.merge(budget, draw, Long::sum);
            draws[index] = draw;
            total += draw;
        }
        return new HatchDrawPlan(inputs, draws, total, gameTick);
    }

    public long total() {
        return total;
    }

    public void commit() {
        for (int index = 0; index < draws.length; index++) {
            long draw = draws[index];
            if (draw == 0) continue;
            EnergyComponent energy = inputs.get(index).energy();
            if (!InstantaneousPowerTracker.spend(energy, gameTick, draw)) {
                throw new IllegalStateException("Instantaneous MI hatch budget changed between simulation and commit");
            }
            if (energy.consumeEu(draw, Simulation.ACT) != draw) {
                throw new IllegalStateException("MI hatch energy changed between atomic simulation and commit");
            }
        }
    }
}
