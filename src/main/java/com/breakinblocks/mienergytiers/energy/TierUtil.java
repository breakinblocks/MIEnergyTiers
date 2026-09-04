package com.breakinblocks.mienergytiers.energy;

import aztech.modern_industrialization.api.energy.CableTier;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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

    /**
     * Find a multiblock hatch route. A native route is preferred. Otherwise, at least two hatches
     * from the tier immediately below the requested voltage can promote to that voltage, with up
     * to four hatches participating in the aggregate draw.
     */
    public static @Nullable HatchRoute hatchRoute(Collection<CableTier> inputs, CableTier required) {
        CableTier nativeTier = routeTier(inputs, required);
        if (nativeTier != null) return new HatchRoute(nativeTier, nativeTier, Integer.MAX_VALUE);

        List<CableTier> tiers = CableTier.allTiers();
        int requiredIndex = tiers.indexOf(required);
        if (requiredIndex <= 0) return null;
        CableTier lowerTier = tiers.get(requiredIndex - 1);
        long lowerHatches = inputs.stream().filter(tier -> tier == lowerTier).count();
        return lowerHatches >= 2 ? new HatchRoute(lowerTier, required, 4) : null;
    }

    public static @Nullable CableTier effectiveVoltage(Collection<CableTier> inputs) {
        List<CableTier> tiers = CableTier.allTiers();
        for (int index = tiers.size() - 1; index >= 0; index--) {
            if (hatchRoute(inputs, tiers.get(index)) != null) return tiers.get(index);
        }
        return null;
    }

    public static long hatchCapacity(Collection<CableTier> inputs, HatchRoute route) {
        long hatches = Math.min(inputs.stream().filter(tier -> tier == route.inputTier()).count(), route.maxHatches());
        if (hatches <= 0) return 0;
        long perHatch = maxHatchEuPerTick(route.inputTier());
        return perHatch > Long.MAX_VALUE / hatches ? Long.MAX_VALUE : perHatch * hatches;
    }

    public static long maxHatchEuPerTick(CableTier tier) {
        return tier.getEu() > Long.MAX_VALUE / 2 ? Long.MAX_VALUE : tier.getEu() * 2;
    }

    public record HatchRoute(CableTier inputTier, CableTier effectiveTier, int maxHatches) {
        public boolean isPromoted() {
            return inputTier != effectiveTier;
        }
    }

    private TierUtil() {}
}
