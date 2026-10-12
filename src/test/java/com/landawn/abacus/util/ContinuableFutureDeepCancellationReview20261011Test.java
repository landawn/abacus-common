package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ContinuableFutureDeepCancellationReview20261011Test extends TestBase {
    private static final Executor HOLD_TASKS = task -> {
    };

    @Test
    public void cancellationTraversesDeepAsyncChainsWithoutOverflow() {
        final List<ContinuableFuture<Integer>> stages = pendingChain(20_000);
        assertTrue(assertDoesNotThrow(() -> stages.getLast().cancelAll(false)));
        assertTrue(stages.stream().allMatch(ContinuableFuture::isCancelled));
    }

    @Test
    public void statusTraversesDeepAsyncChainsWithoutOverflow() {
        final List<ContinuableFuture<Integer>> stages = pendingChain(20_000);
        stages.forEach(stage -> stage.cancel(false));
        assertTrue(assertDoesNotThrow(() -> stages.getLast().isAllCancelled()));
    }

    @Test
    public void cancellationStillVisitsEveryBranchAndHonorsSubclassOverrides() {
        final AtomicInteger cancellations = new AtomicInteger();
        final AtomicInteger statuses = new AtomicInteger();
        final ContinuableFuture<Integer> custom = new ContinuableFuture<>(new FutureTask<>(() -> 1), null, HOLD_TASKS) {
            @Override
            public boolean cancelAll(final boolean interrupt) {
                cancellations.incrementAndGet();
                return super.cancelAll(interrupt);
            }

            @Override
            public boolean isAllCancelled() {
                statuses.incrementAndGet();
                return super.isAllCancelled();
            }
        };
        final ContinuableFuture<Integer> completed = ContinuableFuture.completed(1);
        final ContinuableFuture<Integer> left = new ContinuableFuture<>(new FutureTask<>(() -> 2), List.of(completed, custom), HOLD_TASKS);
        final ContinuableFuture<Integer> right = new ContinuableFuture<>(new FutureTask<>(() -> 3), List.of(custom), HOLD_TASKS);
        final ContinuableFuture<Integer> joined = new ContinuableFuture<>(new FutureTask<>(() -> 4), List.of(left, right), HOLD_TASKS);

        assertFalse(joined.cancelAll(false));
        assertTrue(custom.isCancelled());
        assertTrue(right.isCancelled());
        assertEquals(1, cancellations.get());
        assertTrue(right.isAllCancelled());
        assertEquals(1, statuses.get());
        assertFalse(joined.isAllCancelled());
        assertEquals(1, statuses.get(), "status checks must retain upstream-first short-circuiting");
    }

    private static List<ContinuableFuture<Integer>> pendingChain(final int count) {
        final List<ContinuableFuture<Integer>> stages = new ArrayList<>(count);
        ContinuableFuture<Integer> current = ContinuableFuture.call(() -> 0, HOLD_TASKS);
        stages.add(current);
        for (int i = 1; i < count; i++) {
            current = current.thenCallAsync(value -> value + 1);
            stages.add(current);
        }
        return stages;
    }
}
