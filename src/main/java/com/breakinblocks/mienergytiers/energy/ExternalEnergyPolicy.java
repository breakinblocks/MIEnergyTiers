package com.breakinblocks.mienergytiers.energy;

import com.breakinblocks.mienergytiers.config.HardEnergyConfig;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import org.jspecify.annotations.Nullable;

public final class ExternalEnergyPolicy {
    public static boolean allowsInput(ILongEnergyStorage storage, @Nullable EnergyTransferContext context) {
        if (!HardEnergyConfig.REJECT_UNTYPED_EXTERNAL_INPUT.get()) return true;
        return context != null && storage instanceof TierAwareEndpoint endpoint
                && endpoint.miEnergyTier() == context.tier();
    }

    private ExternalEnergyPolicy() {}
}
