/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

// TODO: Auto-generated Javadoc
/**
 * The Class Synchronized.
 *
 * @author Haiyang Li
 * @param <T> the generic type
 * @since 0.8
 */
public final class Synchronized<T> {

    /** The mutex. */
    private final T mutex;

    /**
     * Instantiates a new synchronized.
     *
     * @param mutex the mutex
     */
    Synchronized(final T mutex) {
        N.checkArgNotNull(mutex);

        this.mutex = mutex;
    }

    /**
     * On.
     *
     * @param <T> the generic type
     * @param mutex the mutex
     * @return the synchronized
     */
    public static <T> Synchronized<T> on(final T mutex) {
        N.checkArgNotNull(mutex);

        return new Synchronized<>(mutex);
    }

    /**
     * Run.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param mutex to locked on.
     * @param cmd the cmd
     * @throws E the e
     */
    public static <T, E extends Exception> void run(final T mutex, final Try.Runnable<E> cmd) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            cmd.run();
        }
    }

    /**
     * Call.
     *
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param mutex to locked on.
     * @param cmd the cmd
     * @return the r
     * @throws E the e
     */
    public static <T, R, E extends Exception> R call(final T mutex, final Try.Callable<R, RuntimeException> cmd) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            return cmd.call();
        }
    }

    /**
     * Test.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param mutex to locked on.
     * @param predicate the predicate
     * @return true, if successful
     * @throws E the e
     */
    public static <T, E extends Exception> boolean test(final T mutex, final Try.Predicate<? super T, E> predicate) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex);
        }
    }

    /**
     * Test.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     * @param mutex to locked on.
     * @param u the u
     * @param predicate the predicate
     * @return true, if successful
     * @throws E the e
     */
    public static <T, U, E extends Exception> boolean test(final T mutex, final U u, final Try.BiPredicate<? super T, ? super U, E> predicate) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex, u);
        }
    }

    /**
     * Accept.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param mutex to locked on.
     * @param consumer the consumer
     * @throws E the e
     */
    public static <T, E extends Exception> void accept(final T mutex, final Try.Consumer<? super T, E> consumer) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex);
        }
    }

    /**
     * Accept.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     * @param mutex to locked on.
     * @param u the u
     * @param consumer the consumer
     * @throws E the e
     */
    public static <T, U, E extends Exception> void accept(final T mutex, final U u, final Try.BiConsumer<? super T, ? super U, E> consumer) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex, u);
        }
    }

    /**
     * Apply.
     *
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param mutex to locked on.
     * @param funciton the funciton
     * @return the r
     * @throws E the e
     */
    public static <T, R, E extends Exception> R apply(final T mutex, final Try.Function<? super T, R, E> funciton) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(funciton);

        synchronized (mutex) {
            return funciton.apply(mutex);
        }
    }

    /**
     * Apply.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param mutex to locked on.
     * @param u the u
     * @param funciton the funciton
     * @return the r
     * @throws E the e
     */
    public static <T, U, R, E extends Exception> R apply(final T mutex, final U u, final Try.BiFunction<? super T, ? super U, R, E> funciton) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(funciton);

        synchronized (mutex) {
            return funciton.apply(mutex, u);
        }
    }

    /**
     * Run.
     *
     * @param <E> the element type
     * @param cmd the cmd
     * @throws E the e
     */
    public <E extends Exception> void run(final Try.Runnable<E> cmd) throws E {
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            cmd.run();
        }
    }

    /**
     * Call.
     *
     * @param <R> the generic type
     * @param <E> the element type
     * @param cmd the cmd
     * @return the r
     * @throws E the e
     */
    public <R, E extends Exception> R call(final Try.Callable<R, E> cmd) throws E {
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            return cmd.call();
        }
    }

    /**
     * Test.
     *
     * @param <E> the element type
     * @param predicate the predicate
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean test(final Try.Predicate<? super T, E> predicate) throws E {
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex);
        }
    }

    /**
     * Accept.
     *
     * @param <E> the element type
     * @param consumer the consumer
     * @throws E the e
     */
    public <E extends Exception> void accept(final Try.Consumer<? super T, E> consumer) throws E {
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex);
        }
    }

    /**
     * Apply.
     *
     * @param <R> the generic type
     * @param <E> the element type
     * @param function the function
     * @return the r
     * @throws E the e
     */
    public <R, E extends Exception> R apply(final Try.Function<? super T, R, E> function) throws E {
        N.checkArgNotNull(function);

        synchronized (mutex) {
            return function.apply(mutex);
        }
    }
}
