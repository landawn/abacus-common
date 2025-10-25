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
 * A functional interface that represents an operation that accepts an object-valued argument
 * and a long-valued argument, and returns no result. This is a specialization of BiConsumer
 * for the case where the second argument is a primitive long.
 *
 * <p>This interface extends both {@link Throwables.ObjLongConsumer} and 
 * {@link java.util.function.ObjLongConsumer}, providing compatibility with the standard
 * Java functional interfaces while adding exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, long)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @param <T> the type of the object argument to the operation
 * @see java.util.function.ObjLongConsumer
 * @see java.util.function.BiConsumer
 */
@FunctionalInterface
public interface ObjLongConsumer<T> extends Throwables.ObjLongConsumer<T, RuntimeException>, java.util.function.ObjLongConsumer<T> { //NOSONAR

    /**
     * Performs this operation on the given arguments.
     *
     * <p>This method consumes an object of type T and a long value, performing some
     * side-effect operation without returning any result. Common use cases include
     * timestamp operations, ID assignments, size or count updates, or any operation
     * where a long parameter modifies the state of an object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjLongConsumer<Event> setTimestamp = (event, timestamp) -> 
     *     event.setTimestamp(timestamp);
     * ObjLongConsumer<Account> adjustBalance = (account, amount) -> 
     *     account.addToBalance(amount);
     * 
     * setTimestamp.accept(myEvent, System.currentTimeMillis());
     * adjustBalance.accept(myAccount, 1000L);
     * }</pre>
     *
     * @param t the first input argument of type T
     * @param value the second input argument, a primitive long value if the operation cannot be completed
     */
    @Override
    void accept(T t, long value);

    /**
     * Returns a composed {@code ObjLongConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p>This method allows for chaining multiple consumers together, where each
     * consumer receives the same input arguments. This is useful for performing
     * multiple independent operations on the same data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjLongConsumer<Logger> logTimestamp = (logger, timestamp) -> 
     *     logger.log("Timestamp: " + timestamp);
     * ObjLongConsumer<Logger> logDate = (logger, timestamp) -> 
     *     logger.log("Date: " + new Date(timestamp));
     * 
     * ObjLongConsumer<Logger> logBoth = logTimestamp.andThen(logDate);
     * // This will log both the timestamp and its date representation
     * logBoth.accept(myLogger, System.currentTimeMillis());
     * }</pre>
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code ObjLongConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default ObjLongConsumer<T> andThen(final ObjLongConsumer<? super T> after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}