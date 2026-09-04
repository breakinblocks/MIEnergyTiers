package com.breakinblocks.mienergytiers.power;

import aztech.modern_industrialization.api.energy.MIEnergyStorage;

/** An energy source that may deliver more than one nominal packet of its tier per tick. */
public interface AmperageSource {
    long amperage();

    static long amperageOf(MIEnergyStorage storage) {
        return storage instanceof AmperageSource source ? Math.max(1, source.amperage()) : 1;
    }
}
