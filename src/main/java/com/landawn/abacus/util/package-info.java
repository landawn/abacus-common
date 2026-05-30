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
 * Core utility classes for Abacus.
 *
 * <h2>API naming conventions</h2>
 *
 * <p>This package (and the library as a whole) follows a small set of naming
 * conventions. They are summarized here so the public API stays predictable; the
 * complete contributor guide lives in {@code docs/naming-conventions.md}.</p>
 *
 * <h3>Guiding principles</h3>
 * <ul>
 *   <li><b>Family parallelism</b> &mdash; when an operation exists for a family of
 *       types or arities (e.g. the primitive list family {@code IntList} &hellip;
 *       {@code BooleanList}, or the functional-interface family), it carries the
 *       <i>same</i> name and parameter order across every sibling for which it is
 *       meaningful.</li>
 *   <li><b>One verb, one meaning</b> &mdash; each verb prefix maps to exactly one
 *       semantic everywhere it appears.</li>
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
 * <h3>Anti-patterns</h3>
 * <ul>
 *   <li>Never disambiguate overloads by letter case or letter count
 *       (e.g. {@code flatMap} vs. {@code flatmap} vs. {@code flattMap}); use distinct
 *       words.</li>
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
 */
package com.landawn.abacus.util;
