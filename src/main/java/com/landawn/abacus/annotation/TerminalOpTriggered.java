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
 * resulting in the closure of the current stream and the creation of a new stream.
 * This annotation is used to document stream operations that break the typical lazy evaluation pattern.
 * 
 * <p>In normal stream processing, intermediate operations are lazy and don't consume the stream
 * until a terminal operation is explicitly called. However, some operations need to trigger
 * terminal operations internally to complete their processing, which closes the current stream.</p>
 * 
 * <p><b>When this happens:</b></p>
 * <ul>
 *   <li>The original stream becomes closed and unusable</li>
 *   <li>A new stream is returned for further processing</li>
 *   <li>The intermediate operation consumes elements to completion</li>
 *   <li>Subsequent operations work on the new stream</li>
 * </ul>
 * 
 * <p><b>Common scenarios that trigger terminal operations:</b></p>
 * <ul>
 *   <li><b>Sorting operations:</b> Must collect all elements before sorting</li>
 *   <li><b>Reverse operations:</b> Must collect all elements before reversing</li>
 *   <li><b>Grouping operations:</b> Require full stream consumption for grouping</li>
 *   <li><b>Statistical operations:</b> Need complete data set for calculations</li>
 *   <li><b>Buffering operations:</b> Must collect elements before releasing them</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * public class StreamProcessor&lt;T&gt; {
 *     {@literal @}TerminalOpTriggered
 *     public StreamProcessor&lt;T&gt; sorted(Comparator&lt;T&gt; comparator) {
 *         // This internally triggers terminal operation (collect)
 *         List&lt;T&gt; sortedList = this.collect(Collectors.toList());
 *         sortedList.sort(comparator);
 *         // Returns new stream from sorted data
 *         return new StreamProcessor&lt;&gt;(sortedList.stream());
 *     }
 * }
 * }</pre>
 * 
 * <p><b>Performance implications:</b></p>
 * <ul>
 *   <li>Higher memory usage due to intermediate data collection</li>
 *   <li>Loss of streaming efficiency for large datasets</li>
 *   <li>Eager evaluation instead of lazy evaluation</li>
 *   <li>Potential for OutOfMemoryError with very large streams</li>
 * </ul>
 * 
 * <p><b>Design considerations:</b></p>
 * <ul>
 *   <li>Document the stream closure behavior clearly</li>
 *   <li>Consider providing alternative lazy implementations when possible</li>
 *   <li>Be aware of the performance trade-offs</li>
 *   <li>Test with large datasets to ensure acceptable memory usage</li>
 * </ul>
 * 
 * @since 2018
 * @see IntermediateOp
 * @see TerminalOp
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html">java.util.Stream</a>
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.METHOD })
public @interface TerminalOpTriggered {

}
