package com.breakinblocks.mienergytiers.power;

import aztech.modern_industrialization.api.energy.CableTier;
import org.jspecify.annotations.Nullable;

public final class HardPowerState {
    private long requestedEuPerTick;
    private long availableEuPerTick;
    private @Nullable CableTier recipeTier;
    private @Nullable CableTier inputTier;
    private HardPowerError error = HardPowerError.NONE;

    public void update(long requested, long available, @Nullable CableTier recipe,
            @Nullable CableTier input, HardPowerError newError) {
        requestedEuPerTick = requested;
        availableEuPerTick = available;
        recipeTier = recipe;
        inputTier = input;
        error = newError;
    }

    public void clear(long requested, long available, @Nullable CableTier recipe, @Nullable CableTier input) {
        update(requested, available, recipe, input, HardPowerError.NONE);
    }

    public long requestedEuPerTick() { return requestedEuPerTick; }
    public long availableEuPerTick() { return availableEuPerTick; }
    public @Nullable CableTier recipeTier() { return recipeTier; }
    public @Nullable CableTier inputTier() { return inputTier; }
    public HardPowerError error() { return error; }
}
