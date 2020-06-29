/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.util.concurrent.atomic.AtomicReference;

import com.landawn.abacus.util.function.Supplier;

/**
 * Copied from Apache Commons Lang.
 * <br />
 * 
 * <p>
 * A specialized {@code ConcurrentInitializer} implementation which is similar
 * to AtomicInitializer, but ensures that the {@link #initialize()}
 * method is called only once.
 * </p>
 * <p>
 * As AtomicInitializer this class is based on atomic variables, so it
 * can create an object under concurrent access without synchronization.
 * However, it implements an additional check to guarantee that the
 * {@link #initialize()} method which actually creates the object cannot be
 * called multiple times.
 * </p>
 * <p>
 * Because of this additional check this implementation is slightly less
 * efficient than AtomicInitializer, but if the object creation in the
 * {@code initialize()} method is expensive or if multiple invocations of
 * {@code initialize()} are problematic, it is the better alternative.
 * </p>
 * <p>
 * From its semantics this class has the same properties as
 * LazyInitializer. It is a &quot;save&quot; implementation of the lazy
 * initializer pattern. Comparing both classes in terms of efficiency is
 * difficult because which one is faster depends on multiple factors. Because
 * {@code AtomicSafeInitializer} does not use synchronization at all it probably
 * outruns LazyInitializer, at least under low or moderate concurrent
 * access. Developers should run their own benchmarks on the expected target
 * platform to decide which implementation is suitable for their specific use
 * case.
 * </p>
 *
 * @param <T> the type of the object managed by this initializer class
 * @since 3.0
 */
public final class SafeInitializer<T> implements Supplier<T> {
    /** A guard which ensures that initialize() is called only once. */
    private final AtomicReference<SafeInitializer<T>> factory = new AtomicReference<>();

    /** Holds the reference to the managed object. */
    private final AtomicReference<T> reference = new AtomicReference<>();

    private final Supplier<T> supplier;

    SafeInitializer(final Supplier<T> supplier) {
        N.checkArgNotNull(supplier, "supplier");

        this.supplier = supplier;
    }

    /**
     * Get (and initialize, if not initialized yet) the required object.
     *
     * @return lazily initialized object
     * exception
     */
    @Override
    public final T get() {
        T result = reference.get();

        if (result == null) {
            while ((result = reference.get()) == null) {
                if (factory.compareAndSet(null, this)) {
                    reference.set(supplier.get());
                }
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param supplier
     * @return
     */
    public static <T> SafeInitializer<T> of(final Supplier<T> supplier) {
        N.checkArgNotNull(supplier);

        if (supplier instanceof SafeInitializer) {
            return (SafeInitializer<T>) supplier;
        }

        return new SafeInitializer<>(supplier);
    }
}
