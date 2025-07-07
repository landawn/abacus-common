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

/**
 * Represents an operation that accepts a variable number of arguments and returns no result.
 * This is a variable-arity (varargs) generalization of {@code Consumer}.
 * Unlike most other functional interfaces, {@code NConsumer} is expected to operate via side-effects.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(Object[])}.
 * 
 * <p>The 'N' in NConsumer stands for 'N-ary', indicating that this consumer can accept
 * any number of arguments of the same type.
 * 
 * <p>Example usage:
 * <pre>{@code
 * NConsumer<String> printAll = args -> {
 *     for (String s : args) {
 *         System.out.println(s);
 *     }
 * };
 * printAll.accept("Hello", "World", "!");
 * 
 * NConsumer<Integer> sumAndStore = args -> {
 *     int sum = 0;
 *     for (Integer n : args) {
 *         sum += n;
 *     }
 *     database.store("sum", sum);
 * };
 * sumAndStore.accept(1, 2, 3, 4, 5);
 * }</pre>
 * 
 * @param <T> the type of the input to the operation
 * 
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 */
@FunctionalInterface
public interface NConsumer<T> {

    /**
     * Performs this operation on the given arguments.
     * 
     * <p>The varargs parameter allows this method to accept any number of arguments
     * of type T, including zero arguments (empty array).
     * 
     * <p>Common use cases include:
     * <ul>
     *   <li>Processing multiple values in batch operations</li>
     *   <li>Logging or printing multiple values</li>
     *   <li>Aggregating variable numbers of inputs</li>
     *   <li>Storing collections of values</li>
     * </ul>
     * 
     * <p>Note: The {@code @SuppressWarnings("unchecked")} annotation is used because
     * varargs with generics can generate unchecked warnings at the call site.
     *
     * @param args the input arguments as a varargs array
     */
    @SuppressWarnings("unchecked")
    void accept(T... args);

    /**
     * Returns a composed {@code NConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation.
     * 
     * <p>If performing either operation throws an exception, it is relayed to the
     * caller of the composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     * 
     * <p>This method is useful for chaining multiple operations that need to process
     * the same variable number of arguments in sequence.
     * 
     * <p>Example usage:
     * <pre>{@code
     * NConsumer<String> logger = args -> {
     *     System.out.println("Processing " + args.length + " items");
     * };
     * NConsumer<String> processor = args -> {
     *     for (String s : args) {
     *         // process each string
     *         database.store(s);
     *     }
     * };
     * 
     * NConsumer<String> combined = logger.andThen(processor);
     * combined.accept("item1", "item2", "item3");
     * // First logs: "Processing 3 items"
     * // Then stores each item in the database
     * }</pre>
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code NConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default NConsumer<T> andThen(final NConsumer<? super T> after) {
        return args -> {
            accept(args);
            after.accept(args);
        };
    }
}