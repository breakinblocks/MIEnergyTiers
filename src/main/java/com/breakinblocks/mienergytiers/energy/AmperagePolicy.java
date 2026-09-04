package com.breakinblocks.mienergytiers.energy;

import aztech.modern_industrialization.api.energy.CableTier;

public final class AmperagePolicy {
    public static long maxAmps(CableTier tier) {
        return tier.getEu() <= 0 ? 1 : Math.max(1, tier.getMaxTransfer() / tier.getEu());
    }

    public static long amps(CableTier tier, long upgradeEu) {
        if (tier.getEu() <= 0 || upgradeEu <= 0) return 1;
        return Math.min(maxAmps(tier), 1 + upgradeEu / tier.getEu());
    }

    public static long ampsPerUpgrade(CableTier tier, long upgradeEu) {
        return tier.getEu() <= 0 || upgradeEu <= 0 ? 0 : Math.min(maxAmps(tier) - 1, upgradeEu / tier.getEu());
    }

    public static long upgradesPerAmp(CableTier tier, long upgradeEu) {
        if (upgradeEu <= 0) return Long.MAX_VALUE;
        return Math.max(1, (tier.getEu() + upgradeEu - 1) / upgradeEu);
    }

    public static long maxRecipeEu(CableTier tier, long upgradeEu) {
        long amps = amps(tier, upgradeEu);
        return tier.getEu() > Long.MAX_VALUE / amps ? Long.MAX_VALUE : tier.getEu() * amps;
    }

    private AmperagePolicy() {}
}
