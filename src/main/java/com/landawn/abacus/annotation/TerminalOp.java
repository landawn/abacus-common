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
 * Marks a method as a terminal operation in stream-like or pipeline processing.
 * Terminal operations consume the stream or pipeline and produce a final result,
 * triggering the execution of any pending intermediate operations.
 *
 * <p>This annotation is used for documentation purposes and to indicate methods that:</p>
 * <ul>
 *   <li>Consume the entire stream or pipeline</li>
 *   <li>Produce a final result or side effect</li>
 *   <li>Trigger execution of lazy intermediate operations</li>
 *   <li>Cannot be chained with other operations</li>
 * </ul>
 *
 * <p><b>Characteristics of terminal operations:</b></p>
 * <ul>
 *   <li><b>Eager evaluation:</b> Process all elements immediately</li>
 *   <li><b>Stream consumption:</b> Close and render the stream unusable after execution</li>
 *   <li><b>Result production:</b> Return a value, collection, or perform side effects</li>
 *   <li><b>Pipeline execution:</b> Trigger all pending intermediate operations</li>
 * </ul>
 *
 * <p><b>Common terminal operations include:</b></p>
 * <ul>
 *   <li>collect() - Accumulate elements into a collection or other container</li>
 *   <li>reduce() - Combine elements into a single result</li>
 *   <li>forEach() - Perform an action on each element</li>
 *   <li>findFirst(), findAny() - Retrieve an element</li>
 *   <li>count() - Count the number of elements</li>
 *   <li>anyMatch(), allMatch(), noneMatch() - Test elements against a predicate</li>
 *   <li>toArray() - Convert stream to an array</li>
 *   <li>min(), max() - Find extreme values</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 *
 * public class StreamProcessor<T> {
 *     @TerminalOp
 *     public List<T> collect() {
 *         // Consumes entire stream and returns result
 *         List<T> result = new ArrayList<>();
 *         this.forEach(result::add);
 *         return result;
 *     }
 *
 *     @TerminalOp
 *     public long count() {
 *         // Terminal operation that counts elements
 *         return this.reduce(0L, (count, element) -> count + 1, Long::sum);
 *     }
 *
 *     @TerminalOp
 *     public void forEach(Consumer<T> action) {
 *         // Side-effecting terminal operation
 *         for (T element : this) {
 *             action.accept(element);
 *         }
 *     }
 *
 *     @TerminalOp
 *     public Optional<T> findFirst() {
 *         // Short-circuiting terminal operation
 *         Iterator<T> iterator = this.iterator();
 *         return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
 *     }
 * }
 * }</pre>
 *
 * @see IntermediateOp
 * @see TerminalOpTriggered
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html">java.util.Stream</a>
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.METHOD })
public @interface TerminalOp {

}
