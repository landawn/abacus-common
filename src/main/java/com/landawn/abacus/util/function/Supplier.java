/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

/**
 * Represents a supplier of results.
 *
 * <p>This is an extended version of {@link java.util.function.Supplier} that implements both
 * the standard Java supplier interface and a throwable variant. There is no requirement that
 * a new or distinct result be returned each time the supplier is invoked.
 *
 * <p>This is a functional interface whose functional method is {@link #get()}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of results supplied by this supplier
 *
 * @see java.util.function.Supplier
 */
@FunctionalInterface
public interface Supplier<T> extends Throwables.Supplier<T, RuntimeException>, java.util.function.Supplier<T> { //NOSONAR
    /**
     * Gets a result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Supplier<String> helloSupplier = () -> "Hello, World!";
     * String greeting = helloSupplier.get(); // returns "Hello, World!"
     *
     * Supplier<LocalDateTime> timestampSupplier = LocalDateTime::now;
     * LocalDateTime now = timestampSupplier.get(); // returns current date-time
     *
     * Supplier<UUID> uuidSupplier = UUID::randomUUID;
     * UUID id = uuidSupplier.get(); // returns a random UUID
     *
     * // Lazy initialization pattern
     * Supplier<ExpensiveObject> lazyInit = () -> new ExpensiveObject();
     * ExpensiveObject obj = lazyInit.get(); // creates object only when needed
     * }</pre>
     *
     * @return a result
     */
    @Override
    T get();

    /**
     * Converts this supplier to a throwable supplier that can throw checked exceptions.
     * This method provides a way to use this supplier in contexts that require explicit exception handling.
     *
     * <p>The returned supplier will have the same behavior as this supplier but with the ability
     * to throw checked exceptions of the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Supplier<String> supplier = () -> "data";
     * Throwables.Supplier<String, Exception> throwableSupplier = supplier.toThrowable();
     * // Can now be used in contexts that handle exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned supplier may throw
     * @return a throwable supplier that wraps this supplier
     */
    default <E extends Throwable> Throwables.Supplier<T, E> toThrowable() {
        return (Throwables.Supplier<T, E>) this;
    }
}
