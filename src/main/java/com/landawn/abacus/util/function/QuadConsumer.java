/*
 * Copyright 2017 Haiyang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;

/**
 * A functional interface that represents an operation that accepts four input arguments
 * and returns no result. This is the four-arity specialization of {@link java.util.function.Consumer}.
 * Unlike most other functional interfaces, {@code QuadConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, Object, Object, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <A> the type of the first argument to the operation
 * @param <B> the type of the second argument to the operation
 * @param <C> the type of the third argument to the operation
 * @param <D> the type of the fourth argument to the operation
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 * @see TriConsumer
 */
@FunctionalInterface
public interface QuadConsumer<A, B, C, D> extends Throwables.QuadConsumer<A, B, C, D, RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * QuadConsumer<String, Integer, Boolean, Date> logger =
     *     (message, level, isError, timestamp) -> {
     *         System.out.printf("[%s] Level %d %s: %s%n",
     *             timestamp, level, isError ? "ERROR" : "INFO", message);
     *     };
     *
     * QuadConsumer<User, Product, Integer, Double> processOrder =
     *     (user, product, quantity, discount) -> {
     *         Order order = new Order(user, product, quantity);
     *         order.applyDiscount(discount);
     *         orderService.save(order);
     *     };
     * }</pre>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     * @param d the fourth input argument
     */
    @Override
    void accept(A a, B b, C c, D d);

    /**
     * Returns a composed {@code QuadConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * QuadConsumer<Order, Customer, Product, Integer> saveOrder =
     *     (order, customer, product, quantity) ->
     *         orderRepository.save(order);
     *
     * QuadConsumer<Order, Customer, Product, Integer> sendNotification =
     *     (order, customer, product, quantity) ->
     *         emailService.sendOrderConfirmation(customer, order);
     *
     * QuadConsumer<Order, Customer, Product, Integer> processAndNotify =
     *     saveOrder.andThen(sendNotification);
     *
     * // This will both save the order and send notification
     * processAndNotify.accept(newOrder, customer, product, 5);
     * }</pre>
     *
     * @param after the operation to perform after this operation.
     * @return a composed {@code QuadConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     * @throws IllegalArgumentException if {@code after} is {@code null}.
     */
    default QuadConsumer<A, B, C, D> andThen(final QuadConsumer<? super A, ? super B, ? super C, ? super D> after) throws IllegalArgumentException {
        N.checkArgNotNull(after, cs.after);

        return (a, b, c, d) -> {
            accept(a, b, c, d);
            after.accept(a, b, c, d);
        };
    }

    /**
     * Returns this consumer as a {@link Throwables.QuadConsumer} view.
     * This method does not translate exceptions or make the original implementation capable of
     * throwing new checked exceptions; the exception type parameter is for target-type compatibility.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * QuadConsumer<String, Integer, Boolean, Long> consumer = (s, i, b, id) -> { };
     * Throwables.QuadConsumer<String, Integer, Boolean, Long, RuntimeException> throwableConsumer = consumer.toThrowable();
     * }</pre>
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.QuadConsumer}
     * @return a {@code Throwables.QuadConsumer} view of this consumer
     */
    default <E extends Throwable> Throwables.QuadConsumer<A, B, C, D, E> toThrowable() {
        return (Throwables.QuadConsumer<A, B, C, D, E>) this;
    }
}
