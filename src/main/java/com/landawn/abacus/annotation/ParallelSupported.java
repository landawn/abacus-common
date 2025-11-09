/*
 * Copyright (C) 2018, 2019 HaiYang Li
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method or type supports parallel execution.
 * This annotation documents that the implementation is thread-safe and can be
 * executed concurrently without synchronization issues.
 *
 * <p>When applied to a method, it indicates that the method can be safely called
 * from multiple threads simultaneously. When applied to a type, it indicates that
 * the entire type and its operations support parallel execution.</p>
 *
 * <p>This annotation is particularly useful for:</p>
 * <ul>
 *   <li>Stream operations that can be parallelized</li>
 *   <li>Collection operations that support concurrent access</li>
 *   <li>Algorithms that can be executed in parallel</li>
 *   <li>Thread-safe utility methods</li>
 * </ul>
 *
 * <p><b>Requirements for parallel-supported operations:</b></p>
 * <ul>
 *   <li>No shared mutable state or proper synchronization</li>
 *   <li>Stateless operations or thread-local state</li>
 *   <li>Associative and non-interfering operations</li>
 *   <li>Thread-safe access to shared resources</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 *
 * public class MathUtils {
 *     @ParallelSupported
 *     public static int square(int n) {
 *         // Stateless operation, safe for parallel execution
 *         return n * n;
 *     }
 *
 *     @ParallelSupported
 *     public static double sum(List<Double> numbers) {
 *         // Can be safely executed in parallel
 *         return numbers.parallelStream()
 *                       .mapToDouble(Double::doubleValue)
 *                       .sum();
 *     }
 * }
 *
 * @ParallelSupported
 * public class ThreadSafeProcessor<T> {
 *     // All methods can be safely called from multiple threads
 *     private final ConcurrentMap<String, T> cache = new ConcurrentHashMap<>();
 *
 *     public T process(String key, Function<String, T> processor) {
 *         return cache.computeIfAbsent(key, processor);
 *     }
 * }
 * }</pre>
 *
 * @see SequentialOnly
 * @see Stateful
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.METHOD, ElementType.TYPE })
public @interface ParallelSupported {

}
