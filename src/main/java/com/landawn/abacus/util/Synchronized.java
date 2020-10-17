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

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public final class Synchronized<T> {

    private final T mutex;

    Synchronized(final T mutex) {
        N.checkArgNotNull(mutex);

        this.mutex = mutex;
    }

    /**
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
     *
     * @param <T>
     * @param <E>
     * @param mutex to locked on.
     * @param cmd
     * @throws E the e
     */
    public static <T, E extends Throwable> void run(final T mutex, final Throwables.Runnable<E> cmd) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            cmd.run();
        }
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param mutex to locked on.
     * @param cmd
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Throwable> R call(final T mutex, final Throwables.Callable<R, E> cmd) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            return cmd.call();
        }
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param mutex to locked on.
     * @param predicate
     * @return true, if successful
     * @throws E the e
     */
    public static <T, E extends Throwable> boolean test(final T mutex, final Throwables.Predicate<? super T, E> predicate) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex);
        }
    }

    /**
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
    public static <T, U, E extends Throwable> boolean test(final T mutex, final U u, final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex, u);
        }
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param mutex to locked on.
     * @param consumer
     * @throws E the e
     */
    public static <T, E extends Throwable> void accept(final T mutex, final Throwables.Consumer<? super T, E> consumer) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex);
        }
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <E>
     * @param mutex to locked on.
     * @param u
     * @param consumer
     * @throws E the e
     */
    public static <T, U, E extends Throwable> void accept(final T mutex, final U u, final Throwables.BiConsumer<? super T, ? super U, E> consumer) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex, u);
        }
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param mutex to locked on.
     * @param funciton
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Throwable> R apply(final T mutex, final Throwables.Function<? super T, R, E> funciton) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(funciton);

        synchronized (mutex) {
            return funciton.apply(mutex);
        }
    }

    /**
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
    public static <T, U, R, E extends Throwable> R apply(final T mutex, final U u, final Throwables.BiFunction<? super T, ? super U, R, E> funciton) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(funciton);

        synchronized (mutex) {
            return funciton.apply(mutex, u);
        }
    }

    /**
     *
     * @param <E>
     * @param cmd
     * @throws E the e
     */
    public <E extends Throwable> void run(final Throwables.Runnable<E> cmd) throws E {
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            cmd.run();
        }
    }

    /**
     *
     * @param <R>
     * @param <E>
     * @param cmd
     * @return
     * @throws E the e
     */
    public <R, E extends Throwable> R call(final Throwables.Callable<R, E> cmd) throws E {
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            return cmd.call();
        }
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Throwable> boolean test(final Throwables.Predicate<? super T, E> predicate) throws E {
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex);
        }
    }

    /**
     *
     * @param <E>
     * @param consumer
     * @throws E the e
     */
    public <E extends Throwable> void accept(final Throwables.Consumer<? super T, E> consumer) throws E {
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex);
        }
    }

    /**
     *
     * @param <R>
     * @param <E>
     * @param function
     * @return
     * @throws E the e
     */
    public <R, E extends Throwable> R apply(final Throwables.Function<? super T, R, E> function) throws E {
        N.checkArgNotNull(function);

        synchronized (mutex) {
            return function.apply(mutex);
        }
    }
}
