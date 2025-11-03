/*
 * Copyright (C) 2020 HaiYang Li
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

package com.landawn.abacus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method or type uses lazy evaluation strategy.
 * Lazy evaluation defers computation until the result is actually needed,
 * which can improve performance by avoiding unnecessary calculations.
 *
 * <p>When applied to a method, it indicates that the method's computation
 * is deferred until the result is accessed. When applied to a type, it indicates
 * that the entire type or its operations follow lazy evaluation patterns.</p>
 *
 * <p>Lazy evaluation is commonly used in:</p>
 * <ul>
 *   <li>Stream operations that don't execute until a terminal operation</li>
 *   <li>Suppliers that compute values on demand</li>
 *   <li>Collections that generate elements as needed</li>
 *   <li>Properties that are calculated when first accessed</li>
 * </ul>
 *
 * <p><b>Benefits of lazy evaluation:</b></p>
 * <ul>
 *   <li>Improved performance by avoiding unnecessary computations</li>
 *   <li>Better memory efficiency by processing elements one at a time</li>
 *   <li>Support for infinite data structures</li>
 *   <li>Composition of operations without intermediate results</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * import java.nio.file.Files;
 * import java.nio.file.Path;
 * import java.util.List;
 * import java.util.function.Function;
 * import java.util.function.Supplier;
 * import java.util.stream.Stream;
 *
 * public class DataProcessor {
 *     @LazyEvaluation
 *     public Stream<String> processLargeFile(Path filePath) throws IOException {
 *         // Returns stream without reading file yet
 *         // File is read only when terminal operation is called
 *         return Files.lines(filePath).map(String::trim);
 *     }
 *
 *     @LazyEvaluation
 *     public Supplier<ExpensiveResult> computeResult() {
 *         // Computation deferred until get() is called
 *         return () -> performExpensiveCalculation();
 *     }
 * }
 *
 * @LazyEvaluation
 * public class LazyList<T> implements List<T> {
 *     // Elements generated on-demand when accessed
 *     private final Function<Integer, T> generator;
 *
 *     public T get(int index) {
 *         return generator.apply(index);  // Computed only when requested
 *     }
 * }
 * }</pre>
 *
 * @see IntermediateOp
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyEvaluation {

}
