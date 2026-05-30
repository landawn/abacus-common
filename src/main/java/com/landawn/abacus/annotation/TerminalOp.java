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
 * <p>Within abacus this annotation is applied across the pipeline APIs —
 * {@code com.landawn.abacus.util.Seq}, the various {@code *Stream} classes,
 * {@code EntryStream}, and friends — on methods such as {@code toList}, {@code toSet},
 * {@code toMap}, {@code collect}, {@code count}, {@code reduce}, {@code forEach},
 * {@code findFirst}/{@code findAny}, {@code anyMatch}/{@code allMatch}/{@code noneMatch},
 * {@code min}/{@code max}, and {@code toArray}. The annotation is a documentation marker
 * (retention {@code CLASS}); it does not affect runtime behavior but lets readers, IDEs, and
 * static analyzers identify the closing call of a pipeline at a glance.</p>
 *
 * <p>This annotation indicates methods that:</p>
 * <ul>
 *   <li>Consume the entire stream or pipeline.</li>
 *   <li>Produce a final result or side effect.</li>
 *   <li>Trigger execution of all pending lazy intermediate operations.</li>
 *   <li>Cannot be chained with further stream operations.</li>
 * </ul>
 *
 * <p><b>Characteristics of terminal operations:</b></p>
 * <ul>
 *   <li><b>Eager evaluation:</b> Process all elements immediately upon invocation.</li>
 *   <li><b>Stream consumption:</b> Close and render the stream unusable after execution.</li>
 *   <li><b>Result production:</b> Return a value or collection, or perform side effects.</li>
 *   <li><b>Pipeline execution:</b> Trigger all pending intermediate operations in the pipeline.</li>
 * </ul>
 *
 * <p><b>Common terminal operations include:</b></p>
 * <ul>
 *   <li>{@code collect()} - Accumulates elements into a collection or other container.</li>
 *   <li>{@code reduce()} - Combines elements into a single result.</li>
 *   <li>{@code forEach()} - Performs an action on each element.</li>
 *   <li>{@code findFirst()}, {@code findAny()} - Retrieves the first or any matching element.</li>
 *   <li>{@code count()} - Counts the number of elements.</li>
 *   <li>{@code anyMatch()}, {@code allMatch()}, {@code noneMatch()} - Tests elements against a predicate.</li>
 *   <li>{@code toArray()} - Converts the stream to an array.</li>
 *   <li>{@code min()}, {@code max()} - Finds the minimum or maximum element.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
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
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html">java.util.stream.Stream</a>
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.METHOD })
public @interface TerminalOp {

}
