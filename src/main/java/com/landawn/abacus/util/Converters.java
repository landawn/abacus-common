/*
 * Copyright (c) 2025, Haiyang Li.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import com.landawn.abacus.annotation.NotNull;

/**
 * A small, process-global registry of custom type converters consulted by
 * {@link N#convert(Object, Class)} / {@link N#convert(Object, com.landawn.abacus.type.Type)}.
 *
 * <p>A converter is a {@link BiFunction} that takes a source object and the requested target class and
 * returns an instance of (or assignable to) the target type. Register a converter for a source class to
 * teach {@code N.convert(...)} how to convert instances of that class.
 *
 * <p>This is the canonical home for the converter registry; {@link N#registerConverter(Class, BiFunction)}
 * is retained as a thin delegating facade.
 *
 * @see N#convert(Object, Class)
 * @see N#registerConverter(Class, BiFunction)
 */
final class Converters {

    private Converters() {
        // singleton for utility class
    }

    private static final Map<Class<?>, BiFunction<Object, Class<?>, Object>> converterMap = new ConcurrentHashMap<>();

    /**
     * Registers a converter for a specific source class. The converter is a function that takes an object of the source class
     * and a target class, and converts the source object into an instance of the target class.
     *
     * <p>A converter is only registered if no converter is already registered for the specified source class
     * (existing registrations are never overwritten).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Converters.register(StringBuilder.class, (value, targetType) -> value.toString()); // returns true if not already registered
     * Converters.register(String.class, (value, targetType) -> value);                   // throws IllegalArgumentException
     * }</pre>
     *
     * @param srcClass the source class that the converter can convert from. This must not be a built-in class.
     * @param converter the converter function that takes a source object and a target class, and returns an instance of the target class.
     * @return {@code true} if there is no {@code converter} registered with specified {@code srcClass} yet before this call.
     * @throws IllegalArgumentException if the specified {@code srcClass} is a built-in class or if either {@code srcClass} or {@code converter} is {@code null}.
     */
    @SuppressWarnings("rawtypes")
    public static boolean register(@NotNull final Class<?> srcClass, final BiFunction<?, Class<?>, ?> converter) throws IllegalArgumentException {
        N.checkArgNotNull(srcClass, cs.srcClass);
        N.checkArgNotNull(converter, cs.converter);

        if (N.isBuiltinClass(srcClass)) {
            throw new IllegalArgumentException("Can't register converter with built-in class: " + ClassUtil.getCanonicalClassName(srcClass));
        }

        synchronized (converterMap) {
            if (converterMap.containsKey(srcClass)) {
                return false;
            }

            converterMap.put(srcClass, (BiFunction) converter);

            return true;
        }
    }

    /**
     * Returns the converter registered for the specified source class, or {@code null} if none is registered.
     *
     * @param srcClass the source class to look up
     * @return the registered converter, or {@code null} if there is none
     */
    static BiFunction<Object, Class<?>, Object> getConverter(final Class<?> srcClass) {
        return converterMap.get(srcClass);
    }
}
