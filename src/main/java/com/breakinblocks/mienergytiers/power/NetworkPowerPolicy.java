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
            long endpointLimit = Math.min(nominalPacket(tier, storage), remaining);
            long available = Math.max(0, storage.extract(endpointLimit, true));
            offered += Math.min(endpointLimit, available);
            remaining = Math.max(0, tier.getMaxTransfer() - offered);
        }
        return offered;
    }

    public static long demand(List<MIEnergyStorage> storages, CableTier tier) {
        long limit = tier.getMaxTransfer();
        long demand = 0;
        for (MIEnergyStorage storage : storages) {
            if (!storage.canReceive()) continue;
            long accepted = Math.max(0, storage.receive(limit, true));
            demand = accepted > limit - demand ? limit : demand + accepted;
        }
        return demand;
    }

    public static long nominalPacket(CableTier tier, MIEnergyStorage source) {
        long amps = AmperageSource.amperageOf(source);
        long eu = tier.getEu();
        if (eu > 0 && amps > Long.MAX_VALUE / eu) return Long.MAX_VALUE;
        return eu * amps;
    }

    private NetworkPowerPolicy() {}
}
