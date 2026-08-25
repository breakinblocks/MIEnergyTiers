package com.breakinblocks.mienergytiers.power;

import aztech.modern_industrialization.api.energy.CableTier;
import org.jspecify.annotations.Nullable;

/** A non-accumulating receipt ledger. Power expires if it was not received this or the previous game tick. */
public final class InstantaneousPowerBudget {
    private long receiptTick = Long.MIN_VALUE;
    private long available;
    private @Nullable CableTier inputTier;

    public void receive(long gameTick, long amount, @Nullable CableTier tier) {
        if (amount <= 0) return;
        if (receiptTick != gameTick) {
            receiptTick = gameTick;
            available = 0;
            inputTier = tier;
        }
        available = Math.addExact(available, amount);
        if (tier != null && (inputTier == null || tier.compareTo(inputTier) > 0)) inputTier = tier;
    }

    public long available(long gameTick) {
        return receiptTick == gameTick || receiptTick == gameTick - 1 ? available : 0;
    }

    public boolean spend(long gameTick, long amount) {
        if (amount < 0 || available(gameTick) < amount) return false;
        available -= amount;
        return true;
    }

    public @Nullable CableTier inputTier(long gameTick) {
        return available(gameTick) > 0 ? inputTier : null;
    }
}
