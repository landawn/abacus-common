/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Core utility classes for Abacus: collections, strings, I/O, beans, CSV/JSON helpers, sequences,
 * functional factories, and value types.
 *
 * <h2>Where to start</h2>
 * <ul>
 *   <li>{@link N} &mdash; the primary facade (null-safe arrays, collections, objects, math, and more).</li>
 *   <li>Plural static-utility holders: {@link Strings}, {@link Numbers}, {@link Dates}, {@link Array},
 *       {@link Maps}, {@link Iterables}, {@link Iterators}, {@link Comparators}, {@link ClassUtil},
 *       {@link IOUtil}, {@link Beans}.</li>
 *   <li>Primitive lists: {@link BooleanList}, {@link CharList}, {@link ByteList}, {@link ShortList},
 *       {@link IntList}, {@link LongList}, {@link FloatList}, {@link DoubleList}.</li>
 *   <li>Collection types: {@link Multiset}, {@link Multimap}, {@link ListMultimap}, {@link SetMultimap},
 *       {@link BiMap}, and the {@code Immutable*} family ({@link ImmutableList}, {@link ImmutableSet},
 *       {@link ImmutableMap}, …).</li>
 *   <li>Tabular data: {@link Dataset}, {@link Sheet} (copy-producing {@code transposed()} vs in-place
 *       {@code sortBy*}).</li>
 *   <li>Lazy sequences: {@link Seq} (checked-exception pipelines). Object and primitive streams live in
 *       {@link com.landawn.abacus.util.stream}.</li>
 *   <li>Text splitting/joining and CSV: {@link Splitter} ({@code split} allocates; {@code splitInto}
 *       writes into a caller-supplied destination), {@link Joiner}, {@link CsvParser}
 *       ({@code parseLine}/{@code parseLineToArray} allocate; {@code parseLineInto} fills an array or
 *       {@code Collection}), {@link CsvUtil}.</li>
 *   <li>Functional factories: {@link Fn}, {@link Fnn} (throwable callbacks), plus {@link Consumers},
 *       {@link Predicates}, {@link Functions}, {@link Suppliers}, {@link BiConsumers},
 *       {@link BiPredicates}, {@link BiFunctions}, {@link UnaryOperators}, {@link BinaryOperators},
 *       {@link TriConsumers}, {@link TriPredicates}, {@link TriFunctions}. The interfaces themselves
 *       are in {@link com.landawn.abacus.util.function}; throwable variants are nested in
 *       {@link Throwables}.</li>
 *   <li>Value types: {@link Pair}, {@link Triple}, {@link Tuple}, {@link Range}, {@link Duration}
 *       (millisecond granularity), {@link Holder}, {@link Indexed}, {@link Timed}, {@link Fraction}.
 *       Optional/nullable wrappers are nested in {@link u} ({@code u.Optional}, {@code u.Nullable},
 *       primitive optionals).</li>
 *   <li>Concurrency: {@link AsyncExecutor}, {@link ContinuableFuture}, {@link Futures}, {@link Retry}.</li>
 *   <li>Fluent wrappers: {@link Builder}. Object pooling for temporary buffers: {@link Objectory}.</li>
 *   <li>Types: {@link TypeReference} captures generic {@link com.landawn.abacus.type.Type} arguments.</li>
 * </ul>
 *
 * <p>The terse facades {@link N}, {@link u}, {@link cs}, {@link Fn}, and {@link Fnn} are intentional
 * and frozen. New public types get descriptive names.</p>
 *
 * <h2>API naming conventions</h2>
 *
 * <p>This package (and the library as a whole) follows a small set of naming
 * conventions for new APIs. Legacy names that predate these rules remain for source and binary
 * compatibility, so existing methods do not always follow them. They are summarized here so new
 * public API stays predictable; the
 * complete contributor guide lives in {@code docs/naming-conventions.md}.</p>
 *
 * <h3>Guiding principles</h3>
 * <ul>
 *   <li><b>Family parallelism</b> &mdash; when an operation exists for a family of
 *       types or arities (e.g. the primitive list family {@code IntList} &hellip;
 *       {@code BooleanList}, or the functional-interface family), it carries the
 *       <i>same</i> name and parameter order across every new sibling for which it is
 *       meaningful.</li>
 *   <li><b>One verb, one meaning</b> &mdash; new verb prefixes should map to one
 *       semantic. Deprecated or otherwise retained legacy names may be exceptions.</li>
 * </ul>
 *
 * <h3>Verb / prefix dictionary</h3>
 * <ul>
 *   <li>{@code isXxx} / {@code hasXxx} / {@code canXxx} &mdash; side-effect-free
 *       boolean query. The only prefixes that return a bare boolean answer.</li>
 *   <li>{@code checkXxx} &mdash; validate a precondition and <b>throw</b> on
 *       failure (returns {@code void} or the validated argument). A
 *       {@code check*} method never returns a plain boolean answer.</li>
 *   <li>{@code toXxx} &mdash; convert to a new representation; the suffix encodes the
 *       result type ({@code toInt} &rarr; {@code int}, {@code toInteger} &rarr;
 *       {@code Integer}).</li>
 *   <li>{@code parseXxx} &mdash; parse text to a primitive/value.</li>
 *   <li>{@code valueOf} &mdash; parse/convert a single value to {@code T}
 *       (JDK-aligned).</li>
 *   <li>{@code of(...)} &mdash; factory from explicit elements or a literal.</li>
 *   <li>{@code from...} / {@code fromXxx(...)} &mdash; factory by adapting or
 *       deserializing another representation ({@code fromJson}, {@code fromCollection}).</li>
 *   <li>{@code newXxx(...)} &mdash; construct a fresh, empty/sized, mutable
 *       instance.</li>
 *   <li>{@code asXxx(...)} &mdash; build-from-elements, or a lightweight view/wrapper.</li>
 *   <li>{@code getXxx} / {@code setXxx} &mdash; property accessor / mutator.</li>
 *   <li>{@code withXxx} &mdash; copy-with-change on an immutable type (returns a new instance).</li>
 *   <li>{@code xxxInto} &mdash; write into a caller-supplied destination and return {@code void}
 *       ({@link Splitter#splitInto}, {@link CsvParser#parseLineInto},
 *       {@link com.landawn.abacus.parser.JsonParser#parseInto}). The allocating counterpart keeps
 *       the original name ({@code split}, {@code parse}, {@code parseLine}/{@code parseLineToArray}).</li>
 * </ul>
 *
 * <p><b>Restricted:</b> {@code createXxx} is reserved for existing API only
 * (object-pool borrow in {@code Objectory}, and Apache-Commons-compatible factories
 * in {@code Numbers}); new factory methods should use {@code of} / {@code from} /
 * {@code new} / {@code valueOf} instead.</p>
 *
 * <h3>Booleans &amp; comparisons</h3>
 * <ul>
 *   <li>Predicates use {@code is}/{@code has}/{@code can}; collective predicates may
 *       use {@code all*} / {@code any*} / {@code none*}.</li>
 *   <li>Comparison predicates prefer the spelled-out names {@code lessThan},
 *       {@code lessThanOrEqual}, {@code greaterThan}, {@code greaterThanOrEqual}
 *       (rather than {@code lt}/{@code le}/{@code gt}/{@code ge}).</li>
 * </ul>
 *
 * <h3>Copy versus mutate</h3>
 * <p>A mutating verb changes the receiver ({@code sort}, {@code reverse}, {@code Sheet.sortBy*}).
 * A copy-producing counterpart uses a past-participle or {@code *ed} name and returns a new value
 * ({@code sorted}, {@code reversed}, {@link Sheet#transposed()}).</p>
 *
 * <h3>Anti-patterns</h3>
 * <ul>
 *   <li>Never disambiguate overloads by letter case or letter count
 *       (e.g. {@code flatMap} vs. {@code flatmap} vs. {@code flattMap}); use distinct
 *       words. The existing {@code Stream}/{@code Seq} {@code flatMap} family is an intentional
 *       exception, documented on {@link com.landawn.abacus.util.stream.Stream}.</li>
 *   <li>Never use a prefix against its meaning (e.g. a {@code check*} that returns a
 *       boolean).</li>
 *   <li>Use camelCase {@code To} for conversions, not the digit {@code 2}
 *       ({@code collectionToArray}, not {@code collection2Array}).</li>
 * </ul>
 *
 * <h3>Stability</h3>
 * <p>This is a published library: public names are a compatibility contract. Names
 * are evolved by adding the new name and marking the old one {@code @Deprecated},
 * never by renaming or removing public API.</p>
 *
 * @see N
 * @see Seq
 * @see com.landawn.abacus.util.function
 * @see com.landawn.abacus.util.stream
 */
package com.landawn.abacus.util;
