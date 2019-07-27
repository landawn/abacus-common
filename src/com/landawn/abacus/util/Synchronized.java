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
 * @since 0.8
 * 
 * @author Haiyang Li 
 */
public final class Synchronized<T> {
    private final T mutex;

    Synchronized(final T mutex) {
        N.checkArgNotNull(mutex);

        this.mutex = mutex;
    }

    public static <T> Synchronized<T> on(final T mutex) {
        N.checkArgNotNull(mutex);

        return new Synchronized<>(mutex);
    }

    /**
     * 
     * @param mutex to locked on.
     * @param cmd
     * @return
     */
    public static <T, E extends Exception> void run(final T mutex, final Try.Runnable<E> cmd) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            cmd.run();
        }
    }

    /**
     * 
     * @param mutex to locked on.
     * @param cmd
     * @return
     */
    public static <T, R, E extends Exception> R call(final T mutex, final Try.Callable<R, RuntimeException> cmd) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            return cmd.call();
        }
    }

    /**
     * 
     * @param mutex to locked on.
     * @param predicate
     * @return
     */
    public static <T, E extends Exception> boolean test(final T mutex, final Try.Predicate<? super T, E> predicate) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex);
        }
    }

    /**
     * 
     * @param mutex to locked on.
     * @param predicate
     * @return
     */
    public static <T, U, E extends Exception> boolean test(final T mutex, final U u, final Try.BiPredicate<? super T, ? super U, E> predicate) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex, u);
        }
    }

    /**
     * 
     * @param mutex to locked on.
     * @param consumer
     * @return
     */
    public static <T, E extends Exception> void accept(final T mutex, final Try.Consumer<? super T, E> consumer) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex);
        }
    }

    /**
     * 
     * @param mutex to locked on.
     * @param consumer
     * @return
     */
    public static <T, U, E extends Exception> void accept(final T mutex, final U u, final Try.BiConsumer<? super T, ? super U, E> consumer) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex, u);
        }
    }

    /**
     * 
     * @param mutex to locked on.
     * @param funciton
     * @return
     */
    public static <T, R, E extends Exception> R apply(final T mutex, final Try.Function<? super T, R, E> funciton) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(funciton);

        synchronized (mutex) {
            return funciton.apply(mutex);
        }
    }

    /**
     * 
     * @param mutex to locked on.
     * @param funciton
     * @return
     */
    public static <T, U, R, E extends Exception> R apply(final T mutex, final U u, final Try.BiFunction<? super T, ? super U, R, E> funciton) throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(funciton);

        synchronized (mutex) {
            return funciton.apply(mutex, u);
        }
    }

    public <E extends Exception> void run(final Try.Runnable<E> cmd) throws E {
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            cmd.run();
        }
    }

    public <R, E extends Exception> R call(final Try.Callable<R, E> cmd) throws E {
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            return cmd.call();
        }
    }

    public <E extends Exception> boolean test(final Try.Predicate<? super T, E> predicate) throws E {
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex);
        }
    }

    public <E extends Exception> void accept(final Try.Consumer<? super T, E> consumer) throws E {
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex);
        }
    }

    public <R, E extends Exception> R apply(final Try.Function<? super T, R, E> function) throws E {
        N.checkArgNotNull(function);

        synchronized (mutex) {
            return function.apply(mutex);
        }
    }
}
