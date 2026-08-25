package com.breakinblocks.mienergytiers.energy;

import aztech.modern_industrialization.api.energy.CableTier;

/** Implement on an explicit FE/GrandPower adapter to declare its voltage. */
public interface TierAwareEndpoint {
    CableTier miEnergyTier();

    default boolean acceptsTier(CableTier offeredTier) {
        return offeredTier.compareTo(miEnergyTier()) <= 0;
    }
}
