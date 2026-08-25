package com.breakinblocks.mienergytiers.power;

import aztech.modern_industrialization.api.energy.CableTier;
import org.jspecify.annotations.Nullable;

/** A non-accumulating receipt ledger. Power expires if it was not received this or the previous game tick. */
public final class InstantaneousPowerBudget {
    private long receiptTick = Long.MIN_VALUE;
    private long received;
    private long available;
    private @Nullable CableTier inputTier;

    public void receive(long gameTick, long amount, @Nullable CableTier tier) {
        if (amount <= 0) return;
        if (receiptTick != gameTick) {
            receiptTick = gameTick;
            received = 0;
            available = 0;
            inputTier = tier;
        }
        received = Math.addExact(received, amount);
        available = Math.addExact(available, amount);
        if (tier != null && (inputTier == null || tier.compareTo(inputTier) > 0)) inputTier = tier;
    }

    public long available(long gameTick) {
        return receiptTick == gameTick || receiptTick == gameTick - 1 ? available : 0;
    }

    /** Total power accepted during the active receipt tick, including power already spent by a machine. */
    public long received(long gameTick) {
        return receiptTick == gameTick || receiptTick == gameTick - 1 ? received : 0;
    }

    public boolean spend(long gameTick, long amount) {
        if (amount < 0 || available(gameTick) < amount) return false;
        available -= amount;
        return true;
    }

    public @Nullable CableTier inputTier(long gameTick) {
        return received(gameTick) > 0 ? inputTier : null;
    }
}
