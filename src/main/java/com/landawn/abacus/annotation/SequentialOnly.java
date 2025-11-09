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
 * Indicates that the annotated method or type must be executed sequentially
 * and does not support parallel execution. This annotation documents that the
 * implementation is not thread-safe and requires sequential access to maintain correctness.
 *
 * <p>When applied to a method, it indicates that the method must not be called
 * from multiple threads simultaneously. When applied to a type, it indicates that
 * the type's operations must be executed sequentially.</p>
 *
 * <p>This annotation is used to mark:</p>
 * <ul>
 *   <li>Stream operations that cannot be parallelized</li>
 *   <li>Stateful operations that require sequential processing</li>
 *   <li>Algorithms that depend on processing order</li>
 *   <li>Non-thread-safe utility methods</li>
 * </ul>
 *
 * <p><b>Common reasons for sequential-only operations:</b></p>
 * <ul>
 *   <li>Maintaining mutable state across invocations</li>
 *   <li>Order-dependent processing requirements</li>
 *   <li>External resource constraints (file handles, connections)</li>
 *   <li>Complex side effects that must occur in a specific order</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 *
 * public class DataProcessor {
 *     private int counter = 0;
 *
 *     @SequentialOnly
 *     public int processWithCounter(String data) {
 *         // Not thread-safe due to mutable state
 *         counter++;
 *         return processData(data, counter);
 *     }
 *
 *     @SequentialOnly
 *     public void writeToFile(List<String> lines, Path outputFile) throws IOException {
 *         // Must maintain order when writing to file
 *         try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
 *             for (String line : lines) {
 *                 writer.write(line);
 *                 writer.newLine();
 *             }
 *         }
 *     }
 * }
 *
 * @SequentialOnly
 * public class OrderDependentProcessor {
 *     // All operations depend on sequential execution
 *     private List<String> history = new ArrayList<>();
 *
 *     public void addEntry(String entry) {
 *         // Order matters for historical tracking
 *         history.add(entry);
 *     }
 * }
 * }</pre>
 *
 * @see ParallelSupported
 * @see Stateful
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.METHOD, ElementType.TYPE })
public @interface SequentialOnly {

}
