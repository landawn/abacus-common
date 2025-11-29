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
 * Represents an operation that accepts a variable number of char-valued arguments and returns no result.
 * This is a functional interface designed to consume char arrays of any length.
 * Unlike most other functional interfaces, {@code CharNConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(char...)}.
 *
 * @see CharConsumer
 * @see java.util.function.Consumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharNConsumer {
    /**
     * Performs this operation on the given char array arguments.
     * The array can be of any length, including zero.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharNConsumer printer = chars -> {
     *     for (char c : chars) {
     *         System.out.print(c);
     *     }
     * };
     * printer.accept('H', 'e', 'l', 'l', 'o');  // Prints: Hello
     *
     * List<Character> charList = new ArrayList<>();
     * CharNConsumer collector = chars -> {
     *     for (char c : chars) {
     *         charList.add(c);
     *     }
     * };
     * collector.accept('a', 'b', 'c');
     * }</pre>
     *
     * @param args the char array input arguments. Can be empty but not {@code null}.
     */
    void accept(char... args);

    /**
     * Returns a composed {@code CharNConsumer} that performs, in sequence, this operation
     * followed by the {@code after} operation. If performing either operation throws an
     * exception, it is relayed to the caller of the composed operation.
     *
     * <p>The char array is passed to both consumers in the same order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharNConsumer printer = chars -> System.out.println(new String(chars));
     * CharNConsumer logger = chars -> System.out.println("Processed " + chars.length + " chars");
     * CharNConsumer combined = printer.andThen(logger);
     * combined.accept('t', 'e', 's', 't');
     * // Output:
     * // test
     * // Processed 4 chars
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code CharNConsumer} that performs in sequence this operation
     *         followed by the {@code after} operation
     */
    default CharNConsumer andThen(final CharNConsumer after) {
        return args -> {
            accept(args);
            after.accept(args);
        };
    }
}
