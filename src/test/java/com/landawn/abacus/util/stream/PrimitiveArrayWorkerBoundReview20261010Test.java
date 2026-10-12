package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.Executor;
import java.util.function.ToDoubleFunction;

import org.junit.jupiter.api.Tag;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
// It inherits every case of PrimitiveArrayShortSliceReview20261010Test, which checks the results in the regular suite.
/** Regression coverage for redundant workers on short primitive array slices. */
@Tag("slow-test")
class PrimitiveArrayWorkerBoundReview20261010Test extends PrimitiveArrayShortSliceReview20261010Test {
    @Override
    void assertWorkers(final int expectedWorkers, final double expectedSum, final ToDoubleFunction<Executor> action) {
        assertEquals(expectedWorkers, runInline(expectedSum, action), "source workers must be bounded by the array slice length");
    }
}
