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
 * Marks a method as an intermediate operation in stream-like or pipeline processing.
 * Intermediate operations are typically lazy and return a new stream or processing pipeline
 * without consuming the source data until a terminal operation is called.
 *
 * <p>Within abacus this annotation is applied extensively across the pipeline APIs —
 * {@code com.landawn.abacus.util.Seq}, {@code Stream}, {@code IntStream},
 * {@code LongStream}, {@code DoubleStream}, {@code EntryStream}, and friends — to mark methods
 * such as {@code filter}, {@code map}, {@code flatMap}, {@code distinct}, {@code skip},
 * {@code limit}, {@code peek}, {@code sorted}, {@code take/dropWhile}, and similar. It is a
 * documentation marker (retention {@code CLASS}); the runtime does not branch on it, but IDEs and
 * static-analysis tools can use it to identify pipeline-shape methods.</p>
 *
 * <p><b>Characteristics of intermediate operations:</b></p>
 * <ul>
 *   <li><b>Lazy evaluation:</b> Operations are not executed until a terminal operation is invoked.</li>
 *   <li><b>Stateless or stateful:</b> May maintain state between elements (e.g., {@code distinct}, {@code sorted}).</li>
 *   <li><b>Non-consuming:</b> Do not process the stream source directly.</li>
 *   <li><b>Pipeline formation:</b> Enable fluent API design through method chaining.</li>
 * </ul>
 *
 * <p><b>Common intermediate operations include:</b></p>
 * <ul>
 *   <li>{@code filter()} — Selects elements matching a predicate.</li>
 *   <li>{@code map()} — Transforms each element.</li>
 *   <li>{@code flatMap()} — Transforms and flattens nested structures.</li>
 *   <li>{@code distinct()} — Removes duplicate elements.</li>
 *   <li>{@code sorted()} — Orders elements (note: typically marked also with
 *       {@link TerminalOpTriggered} when implemented by buffering).</li>
 *   <li>{@code peek()} — Performs an action on each element without consuming.</li>
 *   <li>{@code limit()} / {@code skip()} — Truncate or trim the stream.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * public class StreamProcessor<T> {
 *     @IntermediateOp
 *     public StreamProcessor<T> filter(Predicate<T> predicate) {
 *         // Returns a new stage without consuming elements.
 *         return new FilteredStream<>(this, predicate);
 *     }
 *
 *     @IntermediateOp
 *     public <R> StreamProcessor<R> map(Function<T, R> mapper) {
 *         // Lazy transformation — runs only when a terminal op is invoked.
 *         return new MappedStream<>(this, mapper);
 *     }
 * }
 *
 * // Real example from the abacus Seq API:
 * //     Seq.of(users)
 * //        .filter(User::isActive)   // @IntermediateOp
 * //        .map(User::email)         // @IntermediateOp
 * //        .toList();                // @TerminalOp
 * }</pre>
 *
 * @see TerminalOp
 * @see TerminalOpTriggered
 * @see LazyEvaluation
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html">java.util.Stream</a>
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.METHOD })
public @interface IntermediateOp {

}
