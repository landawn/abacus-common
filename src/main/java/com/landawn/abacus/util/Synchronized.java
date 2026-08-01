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

package com.landawn.abacus.util;

import com.landawn.abacus.annotation.Beta;

/**
 * A utility class that provides thread-safe synchronized operations on objects.
 * This class offers both static methods for one-time synchronized operations and instance methods
 * for repeated synchronized operations on the same object (mutex).
 *
 * <p>The class is designed to simplify synchronized code blocks by providing a fluent API that handles
 * the synchronization mechanism internally. It supports various operation types including:
 * <ul>
 *   <li>Runnable operations (no return value)</li>
 *   <li>Callable operations (with return value)</li>
 *   <li>Predicate operations (boolean tests)</li>
 *   <li>Consumer operations (accepting the mutex as input)</li>
 *   <li>Function operations (transforming the mutex)</li>
 * </ul>
 *
 * <p>All operations synchronize on the provided mutex object. This provides mutual exclusion only
 * with code that locks the exact same object; it does not make the mutex or returned values
 * intrinsically thread-safe. Because the mutex is externally accessible, callers should also avoid
 * lock-order inversions with other monitors.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Static usage
 * List<String> list = new ArrayList<>();
 * Synchronized.run(list, () -> list.add("item"));
 *
 * // Instance usage
 * Synchronized<List<String>> syncList = Synchronized.on(list);
 * syncList.run(() -> list.add("item"));
 * String result = syncList.call(() -> list.get(0));
 * }</pre>
 *
 * @param <T> the type of the object on which the synchronized operations are to be performed
 */
@Beta
@SuppressWarnings("java:S2445")
public final class Synchronized<T> {

    private final T mutex;

    /**
     * Constructs a new {@code Synchronized} instance wrapping the given mutex.
     *
     * @param mutex the object to synchronize on; must not be {@code null}
     * @throws IllegalArgumentException if {@code mutex} is {@code null}
     */
    Synchronized(final T mutex) {
        N.checkArgNotNull(mutex);

        this.mutex = mutex;
    }

    /**
     * Creates a new Synchronized instance for the provided mutex object.
     * This factory method is the primary way to create a Synchronized instance for repeated
     * synchronized operations on the same object.
     *
     * <p>The returned Synchronized instance provides instance methods to perform various
     * synchronized operations on the mutex object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * Synchronized<Map<String, Integer>> syncMap = Synchronized.on(map);
     * syncMap.run(() -> map.put("key", 1));
     * Integer value = syncMap.call(() -> map.get("key"));
     * }</pre>
     *
     * @param <T> the type of the mutex object.
     * @param mutex the object on which synchronized operations will be performed.
     * @return a new Synchronized instance for the provided mutex.
     * @throws IllegalArgumentException if the provided mutex is {@code null}.
     * @see #run(Object, Throwables.Runnable)
     * @see #call(Object, Throwables.Callable)
     */
    public static <T> Synchronized<T> on(final T mutex) throws IllegalArgumentException {
        N.checkArgNotNull(mutex);

        return new Synchronized<>(mutex);
    }

    /**
     * Executes the provided runnable command in a synchronized block on the specified mutex.
     * This method is useful for one-time synchronized operations where creating
     * a Synchronized instance would be unnecessary overhead.
     *
     * <p>The command is executed while holding the monitor lock on the mutex object,
     * providing mutual exclusion with other code that locks the same object. Any exception thrown by the command is propagated
     * to the caller.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = new ArrayList<>();
     * Synchronized.run(list, () -> {
     *     list.add("item1");
     *     list.add("item2");
     * });
     * }</pre>
     *
     * @param <T> the type of the mutex.
     * @param <E> the type of exception that the command might throw.
     * @param mutex the object to synchronize on.
     * @param cmd the runnable command to execute.
     * @throws IllegalArgumentException if the provided mutex is {@code null}.
     * @throws E if the command throws an exception of type E.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     */
    public static <T, E extends Throwable> void run(final T mutex, final Throwables.Runnable<E> cmd) throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(cmd, cs.cmd);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (mutex) {
            cmd.run();
        }
    }

    /**
     * Executes the provided callable command in a synchronized block on the specified mutex and returns its result.
     * This method is useful for one-time synchronized operations that need to return a value.
     *
     * <p>The callable is executed while holding the monitor lock on the mutex object,
     * providing mutual exclusion with other code that locks the same object. The result of the callable is returned to the caller,
     * and any exception thrown by the callable is propagated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("count", 0);
     * Integer result = Synchronized.call(map, () -> {
     *     int current = map.get("count");
     *     map.put("count", current + 1);
     *     return current;
     * });
     * }</pre>
     *
     * @param <T> the type of the mutex.
     * @param <R> the type of the result returned by the callable.
     * @param <E> the type of exception that the callable might throw.
     * @param mutex the object to synchronize on.
     * @param cmd the callable command to execute.
     * @return the result of the callable command.
     * @throws IllegalArgumentException if the provided mutex is {@code null}.
     * @throws E if the callable throws an exception of type E.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     */
    public static <T, R, E extends Throwable> R call(final T mutex, final Throwables.Callable<? extends R, E> cmd) throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(cmd, cs.cmd);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (mutex) {
            return cmd.call();
        }
    }

    /**
     * Tests the provided predicate in a synchronized block on the specified mutex.
     * The predicate receives the mutex as its argument and returns a boolean result.
     *
     * <p>This method is useful for performing synchronized conditional checks where the
     * predicate needs access to the mutex object itself.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = new ArrayList<>();
     * list.add("item");
     * boolean hasItems = Synchronized.test(list, l -> !l.isEmpty());
     * }</pre>
     *
     * @param <T> the type of the mutex.
     * @param <E> the type of exception that the predicate might throw.
     * @param mutex the object to synchronize on and pass to the predicate.
     * @param predicate the predicate to test.
     * @return the boolean result of the predicate.
     * @throws IllegalArgumentException if the provided mutex is {@code null}.
     * @throws E if the predicate throws an exception of type E.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    public static <T, E extends Throwable> boolean test(final T mutex, final Throwables.Predicate<? super T, E> predicate) throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(predicate, cs.predicate);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (mutex) {
            return predicate.test(mutex);
        }
    }

    /**
     * Tests the provided bi-predicate in a synchronized block on the specified mutex.
     * The bi-predicate receives both the mutex and an additional argument.
     *
     * <p>This method is useful for performing synchronized conditional checks where the
     * predicate needs access to both the mutex object and an additional parameter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("count", 5);
     * boolean isGreater = Synchronized.test(map, 3,
     *     (m, threshold) -> m.get("count") > threshold);
     * }</pre>
     *
     * @param <T> the type of the mutex.
     * @param <U> the type of the additional argument.
     * @param <E> the type of exception that the predicate might throw.
     * @param mutex the object to synchronize on and pass as first argument to the predicate.
     * @param u the additional argument to pass as second argument to the predicate.
     * @param predicate the bi-predicate to test.
     * @return the boolean result of the bi-predicate.
     * @throws IllegalArgumentException if the provided mutex is {@code null}.
     * @throws E if the predicate throws an exception of type E.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    public static <T, U, E extends Throwable> boolean test(final T mutex, final U u, final Throwables.BiPredicate<? super T, ? super U, E> predicate)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(predicate, cs.predicate);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (mutex) {
            return predicate.test(mutex, u);
        }
    }

    /**
     * Executes the provided consumer in a synchronized block on the specified mutex.
     * The consumer receives the mutex as its argument.
     *
     * <p>This method is useful for performing synchronized operations where the
     * consumer needs access to the mutex object itself.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * Synchronized.accept(sb, s -> s.append("Hello").append(" World"));
     * }</pre>
     *
     * @param <T> the type of the mutex.
     * @param <E> the type of exception that the consumer might throw.
     * @param mutex the object to synchronize on and pass to the consumer.
     * @param consumer the consumer to execute.
     * @throws IllegalArgumentException if the provided mutex is {@code null}.
     * @throws E if the consumer throws an exception of type E.
     * @throws IllegalArgumentException if {@code consumer} is {@code null}.
     */
    public static <T, E extends Throwable> void accept(final T mutex, final Throwables.Consumer<? super T, E> consumer) throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(consumer, cs.consumer);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (mutex) {
            consumer.accept(mutex);
        }
    }

    /**
     * Executes the provided bi-consumer in a synchronized block on the specified mutex.
     * The bi-consumer receives both the mutex and an additional argument.
     *
     * <p>This method is useful for performing synchronized operations where the
     * consumer needs access to both the mutex object and an additional parameter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     * Synchronized.accept(map, "key", (m, k) -> m.put(k, "value"));
     * }</pre>
     *
     * @param <T> the type of the mutex.
     * @param <U> the type of the additional argument.
     * @param <E> the type of exception that the consumer might throw.
     * @param mutex the object to synchronize on and pass as first argument to the consumer.
     * @param u the additional argument to pass as second argument to the consumer.
     * @param consumer the bi-consumer to execute.
     * @throws IllegalArgumentException if the provided mutex is {@code null}.
     * @throws E if the consumer throws an exception of type E.
     * @throws IllegalArgumentException if {@code consumer} is {@code null}.
     */
    public static <T, U, E extends Throwable> void accept(final T mutex, final U u, final Throwables.BiConsumer<? super T, ? super U, E> consumer)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(consumer, cs.consumer);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (mutex) {
            consumer.accept(mutex, u);
        }
    }

    /**
     * Applies the provided function in a synchronized block on the specified mutex and returns its result.
     * The function receives the mutex as its argument and transforms it to a result.
     *
     * <p>This method is useful for performing synchronized transformations where the
     * function needs access to the mutex object itself.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * Integer size = Synchronized.apply(list, l -> l.size());
     * }</pre>
     *
     * @param <T> the type of the mutex.
     * @param <R> the type of the result.
     * @param <E> the type of exception that the function might throw.
     * @param mutex the object to synchronize on and pass to the function.
     * @param function the function to apply.
     * @return the result of applying the function.
     * @throws IllegalArgumentException if the provided mutex is {@code null}.
     * @throws E if the function throws an exception of type E.
     * @throws IllegalArgumentException if {@code function} is {@code null}.
     */
    public static <T, R, E extends Throwable> R apply(final T mutex, final Throwables.Function<? super T, ? extends R, E> function)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(function, cs.function);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (mutex) {
            return function.apply(mutex);
        }
    }

    /**
     * Applies the provided bi-function in a synchronized block on the specified mutex and returns its result.
     * The bi-function receives both the mutex and an additional argument.
     *
     * <p>This method is useful for performing synchronized transformations where the
     * function needs access to both the mutex object and an additional parameter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("count", 10);
     * Integer result = Synchronized.apply(map, 5,
     *     (m, increment) -> m.merge("count", increment, Integer::sum));
     * }</pre>
     *
     * @param <T> the type of the mutex.
     * @param <U> the type of the additional argument.
     * @param <R> the type of the result.
     * @param <E> the type of exception that the function might throw.
     * @param mutex the object to synchronize on and pass as first argument to the function.
     * @param u the additional argument to pass as second argument to the function.
     * @param function the bi-function to apply.
     * @return the result of applying the bi-function.
     * @throws IllegalArgumentException if the provided mutex is {@code null}.
     * @throws E if the function throws an exception of type E.
     * @throws IllegalArgumentException if {@code function} is {@code null}.
     */
    public static <T, U, R, E extends Throwable> R apply(final T mutex, final U u, final Throwables.BiFunction<? super T, ? super U, ? extends R, E> function)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(function, cs.function);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (mutex) {
            return function.apply(mutex, u);
        }
    }

    /**
     * Executes the provided runnable command in a synchronized block on this instance's mutex.
     * This method is useful for repeated synchronized operations on the same object.
     *
     * <p>The command is executed while holding the monitor lock on the mutex object,
     * providing mutual exclusion with other code that locks the same object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = new ArrayList<>();
     * Synchronized<List<String>> syncList = Synchronized.on(list);
     * syncList.run(() -> {
     *     list.add("item1");
     *     list.add("item2");
     * });
     * }</pre>
     *
     * @param <E> the type of exception that the command might throw.
     * @param cmd the runnable command to execute.
     * @throws E if the command throws an exception of type E.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     */
    public <E extends Throwable> void run(final Throwables.Runnable<E> cmd) throws E, IllegalArgumentException {
        N.checkArgNotNull(cmd, cs.cmd);

        synchronized (mutex) {
            cmd.run();
        }
    }

    /**
     * Executes the provided callable command in a synchronized block on this instance's mutex and returns its result.
     * This method is useful for repeated synchronized operations on the same object that return values.
     *
     * <p>The callable is executed while holding the monitor lock on the mutex object,
     * providing mutual exclusion with other code that locks the same object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * Synchronized<Map<String, Integer>> syncMap = Synchronized.on(map);
     * Integer result = syncMap.call(() -> map.computeIfAbsent("key", k -> 42));
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param <E> the type of exception that the callable might throw.
     * @param cmd the callable command to execute.
     * @return the result of the callable command.
     * @throws E if the callable throws an exception of type E.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     */
    public <R, E extends Throwable> R call(final Throwables.Callable<? extends R, E> cmd) throws E, IllegalArgumentException {
        N.checkArgNotNull(cmd, cs.cmd);

        synchronized (mutex) {
            return cmd.call();
        }
    }

    /**
     * Tests the provided predicate in a synchronized block on this instance's mutex.
     * The predicate receives the mutex as its argument and returns a boolean result.
     *
     * <p>This method is useful for repeated synchronized conditional checks
     * on the same object where the predicate needs access to the mutex.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = new ArrayList<>();
     * Synchronized<List<String>> syncList = Synchronized.on(list);
     * boolean isEmpty = syncList.test(l -> l.isEmpty());
     * }</pre>
     *
     * @param <E> the type of exception that the predicate might throw.
     * @param predicate the predicate to test with the mutex as argument.
     * @return the boolean result of the predicate.
     * @throws E if the predicate throws an exception of type E.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    public <E extends Throwable> boolean test(final Throwables.Predicate<? super T, E> predicate) throws E, IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        synchronized (mutex) {
            return predicate.test(mutex);
        }
    }

    /**
     * Executes the provided consumer in a synchronized block on this instance's mutex.
     * The consumer receives the mutex as its argument.
     *
     * <p>This method is useful for repeated synchronized operations on the same object
     * where the consumer needs access to the mutex.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * Synchronized<StringBuilder> syncSb = Synchronized.on(sb);
     * syncSb.accept(s -> s.append("Hello").append(" World"));
     * }</pre>
     *
     * @param <E> the type of exception that the consumer might throw.
     * @param consumer the consumer to execute with the mutex as argument.
     * @throws E if the consumer throws an exception of type E.
     * @throws IllegalArgumentException if {@code consumer} is {@code null}.
     */
    public <E extends Throwable> void accept(final Throwables.Consumer<? super T, E> consumer) throws E, IllegalArgumentException {
        N.checkArgNotNull(consumer, cs.consumer);

        synchronized (mutex) {
            consumer.accept(mutex);
        }
    }

    /**
     * Applies the provided function in a synchronized block on this instance's mutex and returns its result.
     * The function receives the mutex as its argument and transforms it to a result.
     *
     * <p>This method is useful for repeated synchronized transformations on the same object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("count", 10);
     * Synchronized<Map<String, Integer>> syncMap = Synchronized.on(map);
     * Integer doubled = syncMap.apply(m -> m.get("count") * 2);
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param <E> the type of exception that the function might throw.
     * @param function the function to apply with the mutex as argument.
     * @return the result of applying the function.
     * @throws E if the function throws an exception of type E.
     * @throws IllegalArgumentException if {@code function} is {@code null}.
     */
    public <R, E extends Throwable> R apply(final Throwables.Function<? super T, ? extends R, E> function) throws E, IllegalArgumentException {
        N.checkArgNotNull(function, cs.function);

        synchronized (mutex) {
            return function.apply(mutex);
        }
    }
}
