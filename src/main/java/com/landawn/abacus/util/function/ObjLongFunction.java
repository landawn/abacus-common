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
 * A functional interface that represents a function that accepts an object-valued argument
 * and a long-valued argument, and produces a result. This is a specialization of BiFunction
 * for the case where the second argument is a primitive long.
 *
 * <p>This interface is typically used for operations that need to compute a value based on
 * an object and a long integer, such as timestamp-based calculations, ID lookups,
 * size/offset operations, or transformations that involve large numeric parameters.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(Object, long)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the function
 * @param <R> the type of the result of the function
 * @see java.util.function.BiFunction
 * @see ToLongFunction
 */
@FunctionalInterface
public interface ObjLongFunction<T, R> extends Throwables.ObjLongFunction<T, R, RuntimeException> { // NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p>This method takes an object of type T and a long value as input and
     * produces a result of type R. The function should be deterministic, meaning
     * that for the same inputs, it should always produce the same output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjLongFunction<String, String> formatTimestamp = (format, timestamp) ->
     *     String.format(format, timestamp);
     * ObjLongFunction<Map<Long, String>, String> getById = (map, id) ->
     *     map.getOrDefault(id, "Not Found");
     *
     * String formatted = formatTimestamp.apply("Timestamp: %d", System.currentTimeMillis());
     * Map<Long, String> dataMap = Map.of(12345L, "Value1", 67890L, "Value2");
     * String value = getById.apply(dataMap, 12345L); // Returns "Value1"
     * }</pre>
     *
     * @param t the first function argument of type T
     * @param u the second function argument, a primitive long value
     * @return the function result of type R
     */
    @Override
    R apply(T t, long u);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result. If evaluation of
     * either function throws an exception, it is relayed to the caller of the
     * composed function.
     *
     * <p>This method enables function composition, allowing you to chain multiple
     * transformations together. The output of this function becomes the input to
     * the {@code after} function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjLongFunction<TimeZone, Date> createDate = (timezone, millis) -> {
     *     Calendar cal = Calendar.getInstance(timezone);
     *     cal.setTimeInMillis(millis);
     *     return cal.getTime();
     * };
     * Function<Date, String> formatDate = date ->
     *     new SimpleDateFormat("yyyy-MM-dd").format(date);
     *
     * ObjLongFunction<TimeZone, String> timestampToString =
     *     createDate.andThen(formatDate);
     *
     * String result = timestampToString.apply(TimeZone.getDefault(),
     *     System.currentTimeMillis());
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    default <V> ObjLongFunction<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (t, u) -> after.apply(apply(t, u));
    }

    /**
     * Converts this {@code ObjLongFunction} to a {@code Throwables.ObjLongFunction} that can throw a checked exception.
     * This method provides a way to use this function in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjLongFunction function = (...) -> { ... };
     * var throwableFunction = function.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned function can throw
     * @return a {@code Throwables.ObjLongFunction} view of this function that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.ObjLongFunction<T, R, E> toThrowable() {
        return (Throwables.ObjLongFunction<T, R, E>) this;
    }

}
