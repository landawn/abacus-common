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
 * This class provides a way to perform synchronized operations on a given object.
 * It provides static methods to perform operations like run, call, test, accept, and apply on a given object in a synchronized manner.
 * It also provides instance methods to perform these operations on the object provided at the time of creation of the Synchronized object.
 *
 * @param <T> The type of the object on which the synchronized operations are to be performed.
 * @param <T>
 */
@SuppressWarnings("java:S2445")
public final class Synchronized<T> {

    private final T mutex;

    Synchronized(final T mutex) {
        N.checkArgNotNull(mutex);

        this.mutex = mutex;
    }

    /**
     * Creates a new Synchronized object on the provided {@code mutex}.
     *
     * This method is useful when you want to perform synchronized operations on a given object.
     * The returned Synchronized object provides methods to perform operations like run, call, test, accept, and apply on the {@code mutex} in a synchronized manner.
     *
     * @param <T> The type of the mutex.
     * @param mutex The object on which the synchronized operations are to be performed. Must not be {@code null}.
     * @return A new Synchronized object on the provided {@code mutex}.
     * @throws IllegalArgumentException if the provided {@code mutex} is {@code null}.
     */
    public static <T> Synchronized<T> on(final T mutex) throws IllegalArgumentException {
        N.checkArgNotNull(mutex);

        return new Synchronized<>(mutex);
    }

    /**
     * Executes the provided {@code cmd} in a synchronized manner on the provided {@code mutex}.
     *
     * This method is useful when you want to run a piece of code that might throw an exception and you need to ensure that the execution is synchronized.
     * It allows you to handle exceptions in a specific way by providing a runnable task that can throw an exception.
     *
     * @param <T> The type of the mutex.
     * @param <E> The type of the exception that the {@code cmd} might throw.
     * @param mutex The object on which the synchronized operations are to be performed. Must not be {@code null}.
     * @param cmd The runnable task that might throw an exception. Must not be {@code null}.
     * @throws IllegalArgumentException if the provided {@code mutex} or {@code cmd} is {@code null}.
     * @throws E if an exception occurs during the execution of the {@code cmd}.
     */
    public static <T, E extends Throwable> void run(final T mutex, final Throwables.Runnable<E> cmd) throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            cmd.run();
        }
    }

    /**
     * Executes the provided {@code cmd} in a synchronized manner on the provided {@code mutex} and returns the result.
     *
     * This method is useful when you want to run a piece of code that might throw an exception, you need the result of that code, and you need to ensure that the execution is synchronized.
     * It allows you to handle exceptions in a specific way by providing a callable task that can throw an exception and returns a result.
     *
     * @param <T> The type of the mutex.
     * @param <R> The type of the result.
     * @param <E> The type of the exception that the {@code cmd} might throw.
     * @param mutex The object on which the synchronized operations are to be performed. Must not be {@code null}.
     * @param cmd The callable task that might throw an exception and returns a result. Must not be {@code null}.
     * @return The result of the {@code cmd}.
     * @throws IllegalArgumentException if the provided {@code mutex} or {@code cmd} is {@code null}.
     * @throws E if an exception occurs during the execution of the {@code cmd}.
     */
    public static <T, R, E extends Throwable> R call(final T mutex, final Throwables.Callable<R, E> cmd) throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            return cmd.call();
        }
    }

    /**
     * Tests the provided {@code predicate} in a synchronized manner on the provided {@code mutex} and returns the result.
     *
     * This method is useful when you want to test a condition that might throw an exception and you need to ensure that the execution is synchronized.
     * It allows you to handle exceptions in a specific way by providing a predicate that can throw an exception and returns a boolean result.
     *
     * @param <T> The type of the mutex.
     * @param <E> The type of the exception that the {@code predicate} might throw.
     * @param mutex The object on which the synchronized operations are to be performed. Must not be {@code null}.
     * @param predicate The predicate that might throw an exception and returns a boolean result. Must not be {@code null}.
     * @return The result of the {@code predicate}.
     * @throws IllegalArgumentException if the provided {@code mutex} or {@code predicate} is {@code null}.
     * @throws E if an exception occurs during the execution of the {@code predicate}.
     */
    public static <T, E extends Throwable> boolean test(final T mutex, final Throwables.Predicate<? super T, E> predicate) throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex);
        }
    }

    /**
     * Tests the provided {@code predicate} in a synchronized manner on the provided {@code mutex} and returns the result.
     *
     * This method is useful when you want to test a condition that might throw an exception and you need to ensure that the execution is synchronized.
     * It allows you to handle exceptions in a specific way by providing a bi-predicate that can throw an exception and returns a boolean result.
     *
     * @param <T> The type of the mutex.
     * @param <U> The type of the second argument to the predicate.
     * @param <E> The type of the exception that the {@code predicate} might throw.
     * @param mutex The object on which the synchronized operations are to be performed. Must not be {@code null}.
     * @param u The second argument to the predicate. This argument should be of type U.
     * @param predicate The bi-predicate that might throw an exception and returns a boolean result. Must not be {@code null}.
     * @return The result of the {@code predicate}.
     * @throws IllegalArgumentException if the provided {@code mutex} or {@code predicate} is {@code null}.
     * @throws E if an exception occurs during the execution of the {@code predicate}.
     */
    public static <T, U, E extends Throwable> boolean test(final T mutex, final U u, final Throwables.BiPredicate<? super T, ? super U, E> predicate)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex, u);
        }
    }

    /**
     * Executes the provided {@code consumer} in a synchronized manner on the provided {@code mutex}.
     *
     * This method is useful when you want to perform an operation that might throw an exception and you need to ensure that the execution is synchronized.
     * It allows you to handle exceptions in a specific way by providing a consumer that can throw an exception.
     *
     * @param <T> The type of the mutex.
     * @param <E> The type of the exception that the {@code consumer} might throw.
     * @param mutex The object on which the synchronized operations are to be performed. Must not be {@code null}.
     * @param consumer The consumer that might throw an exception. Must not be {@code null}.
     * @throws IllegalArgumentException if the provided {@code mutex} or {@code consumer} is {@code null}.
     * @throws E if an exception occurs during the execution of the {@code consumer}.
     */
    public static <T, E extends Throwable> void accept(final T mutex, final Throwables.Consumer<? super T, E> consumer) throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex);
        }
    }

    /**
     * Executes the provided {@code consumer} in a synchronized manner on the provided {@code mutex} and {@code u}.
     *
     * This method is useful when you want to perform an operation that might throw an exception and you need to ensure that the execution is synchronized.
     * It allows you to handle exceptions in a specific way by providing a bi-consumer that can throw an exception.
     *
     * @param <T> The type of the mutex.
     * @param <U> The type of the second argument to the consumer.
     * @param <E> The type of the exception that the {@code consumer} might throw.
     * @param mutex The object on which the synchronized operations are to be performed. Must not be {@code null}.
     * @param u The second argument to the consumer. This argument should be of type U.
     * @param consumer The bi-consumer that might throw an exception. Must not be {@code null}.
     * @throws IllegalArgumentException if the provided {@code mutex} or {@code consumer} is {@code null}.
     * @throws E if an exception occurs during the execution of the {@code consumer}.
     */
    public static <T, U, E extends Throwable> void accept(final T mutex, final U u, final Throwables.BiConsumer<? super T, ? super U, E> consumer)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex, u);
        }
    }

    /**
     * Executes the provided {@code function} in a synchronized manner on the provided {@code mutex} and returns the result.
     *
     * This method is useful when you want to run a piece of code that might throw an exception, you need the result of that code, and you need to ensure that the execution is synchronized.
     * It allows you to handle exceptions in a specific way by providing a function that can throw an exception and returns a result.
     *
     * @param <T> The type of the mutex.
     * @param <R> The type of the result.
     * @param <E> The type of the exception that the {@code function} might throw.
     * @param mutex The object on which the synchronized operations are to be performed. Must not be {@code null}.
     * @param function The function that might throw an exception and returns a result. Must not be {@code null}.
     * @return The result of the {@code function}.
     * @throws IllegalArgumentException if the provided {@code mutex} or {@code function} is {@code null}.
     * @throws E if an exception occurs during the execution of the {@code function}.
     */
    public static <T, R, E extends Throwable> R apply(final T mutex, final Throwables.Function<? super T, ? extends R, E> funciton)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(funciton);

        synchronized (mutex) {
            return funciton.apply(mutex);
        }
    }

    /**
     * Executes the provided {@code function} in a synchronized manner on the provided {@code mutex} and {@code u}, and returns the result.
     *
     * This method is useful when you want to run a piece of code that might throw an exception, you need the result of that code, and you need to ensure that the execution is synchronized.
     * It allows you to handle exceptions in a specific way by providing a bi-function that can throw an exception and returns a result.
     *
     * @param <T> The type of the mutex.
     * @param <U> The type of the second argument to the function.
     * @param <R> The type of the result.
     * @param <E> The type of the exception that the {@code function} might throw.
     * @param mutex The object on which the synchronized operations are to be performed. Must not be {@code null}.
     * @param u The second argument to the function. This argument should be of type U.
     * @param function The bi-function that might throw an exception and returns a result. Must not be {@code null}.
     * @return The result of the {@code function}.
     * @throws IllegalArgumentException if the provided {@code mutex} or {@code function} is {@code null}.
     * @throws E if an exception occurs during the execution of the {@code function}.
     */
    public static <T, U, R, E extends Throwable> R apply(final T mutex, final U u, final Throwables.BiFunction<? super T, ? super U, ? extends R, E> funciton)
            throws E {
        N.checkArgNotNull(mutex);
        N.checkArgNotNull(funciton);

        synchronized (mutex) {
            return funciton.apply(mutex, u);
        }
    }

    /**
     *
     *
     * @param <E>
     * @param cmd
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <E extends Throwable> void run(final Throwables.Runnable<E> cmd) throws IllegalArgumentException, E {
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            cmd.run();
        }
    }

    /**
     *
     *
     * @param <R>
     * @param <E>
     * @param cmd
     * @return
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <R, E extends Throwable> R call(final Throwables.Callable<R, E> cmd) throws IllegalArgumentException, E {
        N.checkArgNotNull(cmd);

        synchronized (mutex) {
            return cmd.call();
        }
    }

    /**
     *
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <E extends Throwable> boolean test(final Throwables.Predicate<? super T, E> predicate) throws IllegalArgumentException, E {
        N.checkArgNotNull(predicate);

        synchronized (mutex) {
            return predicate.test(mutex);
        }
    }

    /**
     *
     *
     * @param <E>
     * @param consumer
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <E extends Throwable> void accept(final Throwables.Consumer<? super T, E> consumer) throws IllegalArgumentException, E {
        N.checkArgNotNull(consumer);

        synchronized (mutex) {
            consumer.accept(mutex);
        }
    }

    /**
     *
     *
     * @param <R>
     * @param <E>
     * @param function
     * @return
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <R, E extends Throwable> R apply(final Throwables.Function<? super T, ? extends R, E> function) throws IllegalArgumentException, E {
        N.checkArgNotNull(function);

        synchronized (mutex) {
            return function.apply(mutex);
        }
    }
}
