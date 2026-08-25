package com.breakinblocks.mienergytiers.energy;

import aztech.modern_industrialization.api.energy.CableTier;
import java.util.ArrayDeque;
import java.util.Deque;
import org.jspecify.annotations.Nullable;

public final class EnergyTransferContext {
    private static final ThreadLocal<Deque<EnergyTransferContext>> CURRENT =
            ThreadLocal.withInitial(ArrayDeque::new);

    private final CableTier tier;
    private final TransferEndpoint source;
    private final TransferEndpoint destination;
    private final long gameTick;
    // Direct transfers are fully fresh. Networks replace this with their actual extraction for the tick.
    private long freshAllowance = Long.MAX_VALUE;

    public EnergyTransferContext(CableTier tier, TransferEndpoint source, TransferEndpoint destination, long gameTick) {
        this.tier = tier;
        this.source = source;
        this.destination = destination;
        this.gameTick = gameTick;
    }

    public CableTier tier() { return tier; }
    public TransferEndpoint source() { return source; }
    public TransferEndpoint destination() { return destination; }
    public long gameTick() { return gameTick; }

    public synchronized void setFreshAllowance(long amount) {
        freshAllowance = Math.max(0, amount);
    }

    public synchronized long claimFresh(long received) {
        if (received <= 0) return 0;
        if (freshAllowance == Long.MAX_VALUE) return received;
        long claimed = Math.min(received, freshAllowance);
        freshAllowance -= claimed;
        return claimed;
    }

    public static Scope push(EnergyTransferContext context) {
        Deque<EnergyTransferContext> stack = CURRENT.get();
        stack.push(context);
        return new Scope(stack, context);
    }

    public static @Nullable EnergyTransferContext current() {
        return CURRENT.get().peek();
    }

    public static final class Scope implements AutoCloseable {
        private final Deque<EnergyTransferContext> stack;
        private final EnergyTransferContext expected;
        private boolean closed;

        private Scope(Deque<EnergyTransferContext> stack, EnergyTransferContext expected) {
            this.stack = stack;
            this.expected = expected;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (stack.peek() != expected) {
                stack.clear();
                CURRENT.remove();
                throw new IllegalStateException("Energy transfer context scopes closed out of order");
            }
            stack.pop();
            if (stack.isEmpty()) CURRENT.remove();
        }
    }
}
