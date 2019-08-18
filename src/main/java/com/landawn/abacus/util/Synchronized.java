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
 * @param <T>
 * @since 0.8
 */
public final class Synchronized<T> {

    /** The mutex. */
    private final T mutex;

    /**
     * Instantiates a new synchronized.
     *
     * @param mutex
     */
    Synchronized(final T mutex) {
        N.checkArgNotNull(mutex);

        this.mutex = mutex;
    }

    /**
     * On.
     *
     * @param <T>
     * @param mutex
     * @return
     */
    public static <T> Synchronized<T> on(final T mutex) {
        N.checkArgNotNull(mutex);

        return new Synchronized<>(mutex);
    }

    /**
     * Run.
     *
     * @param <T>
     * @param <E>
     * @param mutex to locked on.
     * @param cmd
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
     * @param <T>
     * @param <R>
     * @param <E>
     * @param mutex to locked on.
     * @param cmd
     * @return
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
     * @param <T>
     * @param <E>
     * @param mutex to locked on.
     * @param predicate
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
     * @param <T>
     * @param <U>
     * @param <E>
     * @param mutex to locked on.
     * @param u
     * @param predicate
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
     * @param <T>
     * @param <E>
     * @param mutex to locked on.
     * @param consumer
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
     * @param <T>
     * @param <U>
     * @param <E>
     * @param mutex to locked on.
     * @param u
     * @param consumer
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
     * @param <T>
     * @param <R>
     * @param <E>
     * @param mutex to locked on.
     * @param funciton
     * @return
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
     * @param <T>
     * @param <U>
     * @param <R>
     * @param <E>
     * @param mutex to locked on.
     * @param u
     * @param funciton
     * @return
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
     * @param <E>
     * @param cmd
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
     * @param <R>
     * @param <E>
     * @param cmd
     * @return
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
     * @param <E>
     * @param predicate
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
     * @param <E>
     * @param consumer
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
     * @param <R>
     * @param <E>
     * @param function
     * @return
     * @throws E the e
     */
    public <R, E extends Exception> R apply(final Try.Function<? super T, R, E> function) throws E {
        N.checkArgNotNull(function);

        synchronized (mutex) {
            return function.apply(mutex);
        }
    }
}
