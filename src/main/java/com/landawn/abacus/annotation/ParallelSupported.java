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
 * Indicates that the annotated method or type supports parallel execution within an abacus
 * pipeline. In the {@code com.landawn.abacus.util.stream} APIs, an operation
 * marked {@code @ParallelSupported} can be applied to a parallelized pipeline (e.g., one created
 * via {@code Stream.parallel()}).
 *
 * <p>This is a pipeline-execution capability marker, not a general thread-safety annotation. It
 * does not promise that concurrent callers may invoke the method on the same object, nor does it
 * make user-supplied functions thread-safe. Callers must still obey each operation's requirements
 * for non-interference, associativity, ordering, and thread safety. This is the converse of
 * {@link SequentialOnly} in the abacus pipeline APIs.</p>
 *
 * <p>This annotation is particularly useful for:</p>
 * <ul>
 *   <li>Stream operations that can be parallelized.</li>
 *   <li>Algorithms that can be executed in parallel.</li>
 *   <li>Types whose documented pipeline operations support parallel execution.</li>
 * </ul>
 *
 * <p><b>Requirements for parallel-supported operations:</b></p>
 * <ul>
 *   <li>The implementation must preserve the operation's documented result when the pipeline is
 *       partitioned.</li>
 *   <li>User callbacks must meet the operation's documented non-interference and concurrency
 *       requirements.</li>
 *   <li>Order-sensitive operations must preserve the ordering guarantees stated by their API.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
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
