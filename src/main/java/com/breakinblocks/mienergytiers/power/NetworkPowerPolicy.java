package com.breakinblocks.mienergytiers.power;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import java.util.List;

public final class NetworkPowerPolicy {
    public static long nominalSourceThroughput(List<MIEnergyStorage> storages, CableTier tier) {
        long offered = 0;
        long remaining = tier.getMaxTransfer();
        for (MIEnergyStorage storage : storages) {
            if (remaining == 0) break;
            long endpointLimit = Math.min(tier.getEu(), remaining);
            long available = Math.max(0, storage.extract(endpointLimit, true));
            offered += Math.min(endpointLimit, available);
            remaining = Math.max(0, tier.getMaxTransfer() - offered);
        }
        return offered;
    }

    private NetworkPowerPolicy() {}
}
