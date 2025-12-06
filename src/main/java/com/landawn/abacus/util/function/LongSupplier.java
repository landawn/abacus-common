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
 * Represents a supplier of long-valued results.
 *
 * <p>This is a functional interface whose functional method is {@link #getAsLong()}.
 *
 * <p>The interface extends both {@code Throwables.LongSupplier} with {@code RuntimeException}
 * and {@code java.util.function.LongSupplier}, providing compatibility with the Java standard library
 * while adding predefined supplier instances for common use cases.
 *
 * @see java.util.function.LongSupplier
 * @see java.util.function.Supplier
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface LongSupplier extends Throwables.LongSupplier<RuntimeException>, java.util.function.LongSupplier { //NOSONAR
    /**
     * A supplier that always returns zero (0L).
     *
     * <p>This is useful for providing a default or initial value in contexts requiring a long supplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongSupplier zeroSupplier = LongSupplier.ZERO;
     * long value = zeroSupplier.getAsLong();   // returns 0L
     * }</pre>
     */
    LongSupplier ZERO = () -> 0;
    /**
     * A supplier that returns random long values.
     *
     * <p>This supplier uses an internal random number generator to produce different long values
     * on each invocation. The values are uniformly distributed across the entire range of long values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongSupplier randomSupplier = LongSupplier.RANDOM;
     * long value1 = randomSupplier.getAsLong();   // returns a random long
     * long value2 = randomSupplier.getAsLong();   // returns another random long
     * }</pre>
     */
    LongSupplier RANDOM = Util.RAND_LONG::nextLong;

    /**
     * Gets a result as a long value.
     *
     * <p>This method should be implemented to provide the long value when called.
     * The implementation may return the same value or different values on subsequent calls,
     * depending on the specific supplier implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongSupplier timestamp = System::currentTimeMillis;
     * long time = timestamp.getAsLong();   // Returns current time in milliseconds
     * }</pre>
     *
     * @return a long value
     */
    @Override
    long getAsLong();
}
