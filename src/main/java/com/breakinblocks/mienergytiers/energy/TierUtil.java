package com.breakinblocks.mienergytiers.energy;

import aztech.modern_industrialization.api.energy.CableTier;
import java.util.Collection;
import java.util.Comparator;
import org.jspecify.annotations.Nullable;

public final class TierUtil {
    public static CableTier forEu(long euPerTick) {
        return CableTier.allTiers().stream()
                .filter(tier -> tier.getEu() >= euPerTick)
                .findFirst()
                .orElse(CableTier.allTiers().getLast());
    }

    public static boolean canSupply(CableTier available, long euPerTick) {
        return available.compareTo(forEu(euPerTick)) >= 0;
    }

    public static @Nullable CableTier routeTier(Collection<CableTier> inputs, CableTier required) {
        return inputs.stream().filter(tier -> tier.compareTo(required) >= 0)
                .min(Comparator.naturalOrder()).orElse(null);
    }

    private TierUtil() {}
}
