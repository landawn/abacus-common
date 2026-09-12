package com.landawn.abacus.util;

import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.TestBase;

public abstract class FnTestSupport extends TestBase {

    public static class MyCloseable implements AutoCloseable {
        protected final AtomicInteger closeCount = new AtomicInteger(0);

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }

        public int getCloseCount() {
            return closeCount.get();
        }
    }
}
