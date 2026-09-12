/*
 * Copyright (c) 2026, Haiyang Li.
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.util.function.IntObjPredicate;
import com.landawn.abacus.util.function.Predicate;

/**
 * Utility class providing various Predicate implementations and factory methods.
 * This class contains methods for creating stateful, indexed, and specialized predicates.
 *
 * <p>This class is a top-level sibling of {@link Fn} (formerly nested as {@code Fn.Predicates}),
 * not a nested type. Use {@link Fn} for the general functional-interface factory and {@link Fnn}
 * for {@link Throwables} variants that can declare checked exceptions. For two-argument predicates
 * see {@link BiPredicates}; for three-argument predicates see {@link TriPredicates}.</p>
 *
 * @see Fn
 * @see Fnn
 * @see BiPredicates
 * @see TriPredicates
 * @see Consumers
 * @see Functions
 */
public final class Predicates {

    private Predicates() {
    }

    /**
     * Returns a stateful Predicate that tests elements based on their index position.
     * The predicate maintains an internal counter that increments with each test.
     * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Predicates.indexed((i, s) -> i < 5).test("hello");     // returns true (index 0 < 5)
     * }</pre>
     *
     * <p>The returned callback uses indices from zero through {@link Integer#MAX_VALUE}. Later calls
     * throw {@link ArithmeticException} without invoking user code. An invocation consumes its index
     * even when user code throws.</p>
     *
     * @param <T> the type of the input to the predicate
     * @param predicate the IntObjPredicate that accepts an index and element for testing
     * @return a stateful Predicate that applies the given IntObjPredicate with an incrementing index
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Predicate<T> indexed(final IntObjPredicate<T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        return new Predicate<>() {
            private long idx;

            /**
             * {@inheritDoc}
             * @throws ArithmeticException if the next zero-based invocation index exceeds {@link Integer#MAX_VALUE}
             */
            @Override
            public boolean test(final T t) throws ArithmeticException {
                // Keep exhaustion representable and reject before invoking user code.
                if (idx > Integer.MAX_VALUE) {
                    throw new ArithmeticException("Index exceeds Integer.MAX_VALUE");
                }

                return predicate.test((int) idx++, t);
            }
        };
    }

    /**
     * Returns a stateful Predicate that maintains a set of seen elements and returns {@code true} only for distinct elements.
     * The predicate uses a HashSet internally to track previously seen elements.
     * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Predicate<String> p = Predicates.distinct();
     * p.test("a");   // returns true (new)
     * p.test("a");   // returns false (seen)
     * p.test("b");   // returns true (new)
     * }</pre>
     *
     * @param <T> the type of the input to the predicate
     * @return a stateful Predicate that returns {@code true} for first occurrence of each distinct element
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Predicate<T> distinct() {
        return new Predicate<>() {
            private final Set<Object> set = N.newHashSet();

            @Override
            public boolean test(final T value) {
                return set.add(value);
            }
        };
    }

    /**
     * Returns a stateful Predicate that maintains distinct elements based on a key extracted by the mapper function.
     * The predicate returns {@code true} only for elements whose mapped keys haven't been seen before.
     * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Predicate<Person> p = Predicates.distinctBy(Person::getName);
     * p.test(new Person("Alice"));   // returns true
     * p.test(new Person("Alice"));   // returns false
     * }</pre>
     *
     * @param <T> the type of the input to the predicate
     * @param mapper the function to extract the key for distinctness comparison
     * @return a stateful Predicate that returns {@code true} for elements with distinct mapped keys
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Predicate<T> distinctBy(final java.util.function.Function<? super T, ?> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        return new Predicate<>() {
            private final Set<Object> set = N.newHashSet();

            @Override
            public boolean test(final T value) {
                return set.add(mapper.apply(value));
            }
        };
    }

    /**
     * Returns a stateful Predicate that maintains a concurrent set of seen elements and returns {@code true} only for distinct elements.
     * This predicate is thread-safe and can be used in parallel streams.
     * This method is marked as Beta and Stateful, indicating it should not be saved or cached for reuse.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Predicate<String> p = Predicates.concurrentDistinct();
     * p.test("a");   // returns true (thread-safe)
     * p.test("a");   // returns false
     * }</pre>
     *
     * @param <T> the type of the input to the predicate
     * @return a stateful thread-safe Predicate that returns {@code true} for first occurrence of each distinct element
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> concurrentDistinct() {
        return new Predicate<>() {
            private final Map<Object, Object> map = new ConcurrentHashMap<>();

            @Override
            public boolean test(final T value) {
                final Object key = value == null ? Fn.NONE : value;
                return map.putIfAbsent(key, Fn.NONE) == null;
            }
        };
    }

    /**
     * Returns a stateful Predicate that maintains distinct elements based on a key extracted by the mapper function.
     * This predicate is thread-safe and can be used in parallel streams.
     * This method is marked as Beta and Stateful, indicating it should not be saved or cached for reuse.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Predicates.concurrentDistinctBy(Person::getName).test(person);   // returns true (thread-safe)
     * }</pre>
     *
     * @param <T> the type of the input to the predicate
     * @param mapper the function to extract the key for distinctness comparison
     * @return a stateful thread-safe Predicate that returns {@code true} for elements with distinct mapped keys
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> concurrentDistinctBy(final java.util.function.Function<? super T, ?> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        return new Predicate<>() {
            private final Map<Object, Object> map = new ConcurrentHashMap<>();

            @Override
            public boolean test(final T value) {
                final Object key = mapper.apply(value);
                return map.putIfAbsent(key == null ? Fn.NONE : key, Fn.NONE) == null;
            }
        };
    }

    /**
     * Returns a stateful Predicate that removes continuous repeat elements.
     * The predicate returns {@code false} for elements that are equal to the immediately preceding element.
     * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Predicate<String> p = Predicates.skipRepeats();
     * p.test("a");   // returns true
     * p.test("a");   // returns false (repeated)
     * p.test("b");   // returns true (different)
     * }</pre>
     *
     * @param <T> the type of the input to the predicate
     * @return a stateful Predicate that returns {@code true} for elements different from their immediate predecessor
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Predicate<T> skipRepeats() {
        return new Predicate<>() {
            private T pre = (T) Fn.NONE;

            @Override
            public boolean test(final T value) {
                final boolean res = pre == Fn.NONE || !N.equals(value, pre);
                pre = value;
                return res;
            }
        };
    }
}
