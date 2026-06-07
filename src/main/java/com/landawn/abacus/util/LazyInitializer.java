/*
 * Copyright (c) 2019, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.util.function.Supplier;

/**
 * A thread-safe lazy initialization wrapper that defers object creation until the first access.
 * This class implements the double-checked locking pattern to ensure thread-safe lazy initialization
 * while minimizing synchronization overhead after initialization.
 *
 * <p>The initialization is performed only once, even in multi-threaded environments. Once initialized,
 * subsequent calls to {@link #get()} return the cached value without synchronization.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * LazyInitializer<ExpensiveObject> lazy = LazyInitializer.of(() -> new ExpensiveObject());
 * // Object is not created yet
 * ExpensiveObject obj = lazy.get();    // creates the object here
 * ExpensiveObject same = lazy.get();   // returns the same cached instance
 * }</pre>
 *
 * @param <T> the type of the lazily initialized object
 */
final class LazyInitializer<T> implements com.landawn.abacus.util.function.Supplier<T> {
    private final Supplier<T> supplier;
    private volatile boolean initialized = false;
    private volatile T value = null; //NOSONAR

    /**
     * Constructs a new {@code LazyInitializer} with the specified supplier.
     *
     * @param supplier the supplier used to produce the value on first access; must not be {@code null}
     * @throws IllegalArgumentException if {@code supplier} is {@code null}
     */
    LazyInitializer(final Supplier<T> supplier) {
        N.checkArgNotNull(supplier, cs.supplier);

        this.supplier = supplier;
    }

    /**
     * Creates a new {@code LazyInitializer} instance with the specified supplier.
     * If the supplier is already a {@code LazyInitializer}, it is returned as-is to avoid double wrapping.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LazyInitializer<String> lazy = LazyInitializer.of(() -> "Hello World");
     * String value = lazy.get();   // "Hello World"
     * }</pre>
     *
     * @param <T> the type of the lazily initialized object
     * @param supplier the supplier that will provide the value when first requested; must not be {@code null}
     * @return a {@code LazyInitializer} wrapping the supplier, or the supplier itself cast to
     *         {@code LazyInitializer} if it is already an instance of {@code LazyInitializer}
     * @throws IllegalArgumentException if {@code supplier} is {@code null}
     */
    public static <T> LazyInitializer<T> of(final Supplier<T> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        if (supplier instanceof LazyInitializer) {
            return (LazyInitializer<T>) supplier;
        }

        return new LazyInitializer<>(supplier);
    }

    /**
     * Returns the lazily initialized value. On the first successful call the value is obtained from
     * the supplier and cached. Subsequent calls return the cached value without invoking the supplier again.
     *
     * <p>This method is thread-safe and uses double-checked locking to ensure that, in the absence of
     * exceptions, the supplier is invoked exactly once even under concurrent access.</p>
     *
     * <p><b>Exception handling:</b> If the supplier throws an unchecked exception during initialization,
     * the initialization state is <em>not</em> marked as completed and the exception is propagated to the
     * caller. Subsequent calls to {@code get()} will retry the initialization by invoking the supplier
     * again. If the supplier consistently throws, every call will throw; once it succeeds, the result
     * is cached and no further invocations occur.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LazyInitializer<Connection> dbConnection = LazyInitializer.of(() -> createConnection());
     * Connection conn = dbConnection.get();   // creates connection on first call
     * Connection same = dbConnection.get();   // returns cached connection
     * }</pre>
     *
     * @return the lazily initialized value (may be {@code null} if the supplier returns {@code null})
     * @throws RuntimeException if the supplier throws an unchecked exception during initialization
     */
    @Override
    public T get() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    value = supplier.get();
                    initialized = true;
                }
            }
        }

        return value;
    }
}
