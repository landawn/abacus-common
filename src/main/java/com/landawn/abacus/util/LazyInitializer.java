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
 * ExpensiveObject obj = lazy.get(); // Object is created here
 * ExpensiveObject same = lazy.get(); // Returns the same cached instance
 * }</pre>
 * 
 * @param <T> the type of the lazily initialized object
 * @since 1.0
 */
final class LazyInitializer<T> implements com.landawn.abacus.util.function.Supplier<T> {
    private final Supplier<T> supplier;
    private volatile boolean initialized = false;
    private volatile T value = null; //NOSONAR

    /**
     * Constructs a new LazyInitializer with the specified supplier.
     * 
     * @param supplier the supplier that will provide the value when first requested; must not be null
     * @throws IllegalArgumentException if supplier is null
     */
    LazyInitializer(final Supplier<T> supplier) {
        N.checkArgNotNull(supplier, cs.supplier);

        this.supplier = supplier;
    }

    /**
     * Creates a new LazyInitializer instance with the specified supplier.
     * If the supplier is already a LazyInitializer, it is returned as-is to avoid double wrapping.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LazyInitializer<String> lazy = LazyInitializer.of(() -> "Hello World");
     * String value = lazy.get(); // "Hello World"
     * }</pre>
     * 
     * @param <T> the type of the lazily initialized object
     * @param supplier the supplier that will provide the value when first requested
     * @return a LazyInitializer wrapping the supplier, or the supplier itself if it's already a LazyInitializer
     * @throws IllegalArgumentException if supplier is null
     */
    public static <T> LazyInitializer<T> of(final Supplier<T> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        if (supplier instanceof LazyInitializer) {
            return (LazyInitializer<T>) supplier;
        }

        return new LazyInitializer<>(supplier);
    }

    /**
     * Returns the lazily initialized value. On first call, the value is obtained from the supplier
     * and cached. Subsequent calls return the cached value without invoking the supplier again.
     * 
     * <p>This method is thread-safe and uses double-checked locking to ensure the supplier is called
     * only once even in concurrent scenarios.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LazyInitializer<Connection> dbConnection = LazyInitializer.of(() -> createConnection());
     * Connection conn = dbConnection.get(); // Creates connection on first call
     * Connection same = dbConnection.get(); // Returns cached connection
     * }</pre>
     * 
     * @return the lazily initialized value
     * @throws RuntimeException if the supplier throws an exception during initialization
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