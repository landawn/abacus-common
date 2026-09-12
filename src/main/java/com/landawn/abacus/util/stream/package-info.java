/*
 * Copyright (C) 2015 HaiYang Li
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

/**
 * Sequential and parallel streams for object and primitive values, plus entry streams and collectors.
 *
 * <p>{@link Stream} is the object stream. Primitive specializations avoid boxing:
 * {@link ByteStream}, {@link CharStream}, {@link ShortStream}, {@link IntStream}, {@link LongStream},
 * {@link FloatStream}, and {@link DoubleStream}. {@link EntryStream} is a key/value pipeline over
 * {@link java.util.Map.Entry}. All of them implement {@link BaseStream} ({@link AutoCloseable}):
 * intermediate operations are lazy; a terminal operation consumes the pipeline once and closes it.</p>
 *
 * <p>{@link Collectors} (and nested {@code MoreCollectors}) accumulate streams into lists, sets, maps,
 * multimaps, primitive lists, immutable collections, and statistical summaries. Extended iterators
 * ({@link IteratorEx}, {@link ObjIteratorEx}, and the primitive {@code *IteratorEx} types) back the
 * stream implementations.</p>
 *
 * <h2>{@code Stream} versus {@link com.landawn.abacus.util.Seq Seq}</h2>
 * <ul>
 *   <li>Prefer {@link Stream} for in-memory work that does not throw checked exceptions, for
 *       parallelism, and for primitive or {@link EntryStream} pipelines.</li>
 *   <li>Prefer {@code Seq} when per-element work throws checked exceptions (I/O, JDBC, parsing) or
 *       when the source is a resource that should close after the terminal operation.</li>
 * </ul>
 * <p>The two interoperate: {@code seq.stream()} yields a {@link Stream}. Pipelines share operation
 * names; the canonical glossary &mdash; including the intentional
 * {@code flatMap}/{@code flatmap}/{@code flattMap}/{@code flatMapArray} casing, first/last/find*
 * terminals, and boolean match terminals such as {@code hasMatchCountBetween} &mdash; is the
 * <i>Shared pipeline naming</i> section on {@link Stream}
 * ({@code Stream.html#shared-pipeline-naming}).</p>
 *
 * @see Stream
 * @see EntryStream
 * @see Collectors
 * @see BaseStream
 * @see com.landawn.abacus.util.Seq
 */
package com.landawn.abacus.util.stream;
