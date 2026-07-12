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
 * Marks methods where intermediate operations trigger terminal operations internally,
 * which consumes the current stream and creates a new stream. The upstream stream may be closed as a result.
 * This annotation is used to document stream operations that break the typical lazy evaluation pattern.
 *
 * <p>Within abacus this is applied across {@code com.landawn.abacus.util.Seq} and the
 * {@code *Stream} families on operations such as {@code sorted}, {@code reversed},
 * {@code shuffled}, {@code groupBy}, and similar — places where the entire upstream must be
 * materialized before any downstream stage can be wired up.</p>
 *
 * <p>In normal pipeline processing, intermediate operations are lazy and do not consume the
 * source until a terminal operation is invoked. Methods marked with {@code @TerminalOpTriggered}
 * break that pattern: the upstream pipeline is consumed (and may be closed), a buffer is built, and a
 * fresh pipeline is returned over that buffer. Callers may keep chaining as if it were a normal
 * intermediate operation, but the lazy-evaluation guarantee no longer holds for everything
 * upstream of this call.</p>
 *
 * <p><b>What happens internally:</b></p>
 * <ul>
 *   <li>The original (upstream) stream is fully consumed and may then be closed.</li>
 *   <li>Elements are buffered, reordered, grouped, or otherwise post-processed in memory.</li>
 *   <li>A new pipeline is returned over the buffered data and continues lazily from there.</li>
 *   <li>Subsequent operations work on the new pipeline, not the original one.</li>
 * </ul>
 *
 * <p><b>Common scenarios that trigger terminal operations:</b></p>
 * <ul>
 *   <li><b>Sorting operations:</b> Must collect all elements before sorting.</li>
 *   <li><b>Reverse operations:</b> Must collect all elements before reversing.</li>
 *   <li><b>Grouping operations:</b> Require full stream consumption for grouping.</li>
 *   <li><b>Statistical operations:</b> Need complete data set for calculations.</li>
 *   <li><b>Buffering operations:</b> Must collect elements before releasing them.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * public class StreamProcessor<T> {
 *     @TerminalOpTriggered
 *     public StreamProcessor<T> sorted(Comparator<T> comparator) {
 *         // This internally triggers terminal operation (collect)
 *         List<T> sortedList = this.collect(Collectors.toList());
 *         sortedList.sort(comparator);
 *         // Returns new stream from sorted data
 *         return new StreamProcessor<>(sortedList.stream());
 *     }
 * }
 * }</pre>
 *
 * <p><b>Performance implications:</b></p>
 * <ul>
 *   <li>Higher memory usage due to intermediate data collection.</li>
 *   <li>Loss of streaming efficiency for large datasets.</li>
 *   <li>Eager evaluation instead of lazy evaluation.</li>
 *   <li>Potential for {@code OutOfMemoryError} with very large streams.</li>
 * </ul>
 *
 * <p><b>Design considerations:</b></p>
 * <ul>
 *   <li>Document the stream closure behavior clearly.</li>
 *   <li>Consider providing alternative lazy implementations when possible.</li>
 *   <li>Be aware of the performance trade-offs.</li>
 *   <li>Test with large datasets to ensure acceptable memory usage.</li>
 * </ul>
 *
 * @see IntermediateOp
 * @see TerminalOp
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html">java.util.stream.Stream</a>
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.METHOD })
public @interface TerminalOpTriggered {

}
