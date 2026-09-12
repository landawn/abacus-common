/*
 * Copyright (C) 2019 HaiYang Li
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

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

/**
 * A comprehensive utility class providing an extensive collection of static methods for Map operations,
 * transformations, manipulations, and analysis. This class serves as the primary map utility facade
 * in the Abacus library, offering null-safe, performance-optimized operations for all types of Map
 * implementations with a focus on functional programming patterns and Optional-based return types.
 *
 * <p>The {@code Maps} class is designed as a final utility class that provides a complete toolkit
 * for map processing including creation, transformation, filtering, comparison, and restructuring
 * operations. All methods are static and designed to handle common edge cases (such as {@code null}
 * or empty maps) gracefully while maintaining good performance for large-scale map operations.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Comprehensive Map Operations:</b> Broad set of operations for all Map types and implementations</li>
 *   <li><b>Optional/Nullable Returns:</b> Value-retrieval methods return {@link Optional}, {@link Nullable},
 *       or primitive-Optional types for null-safe value handling</li>
 *   <li><b>Null-Tolerant Design:</b> {@code null} or empty maps are handled gracefully by the
 *       <em>non-mutating</em> methods; the mutating methods require a non-{@code null} map. Some methods
 *       also validate non-{@code null} arguments such as default values and predicates</li>
 *   <li><b>Functional Programming:</b> Support for filter, invert, and other functional patterns</li>
 *   <li><b>Type Safety:</b> Generic methods with compile-time type checking</li>
 *   <li><b>Performance Optimized:</b> Efficient algorithms with minimal object allocation</li>
 *   <li><b>Restructuring Support:</b> Key replacement, inversion, flattening, and unflattening operations</li>
 * </ul>
 *
 * <p><b>Core Design Principles:</b>
 * <ul>
 *   <li><b>Present vs. Absent:</b> For methods whose names use {@code IfPresent}/{@code IfAbsent},
 *       "present" means a key is found with a non-{@code null} value; "absent" means the key is missing
 *       or maps to {@code null}. {@code IfExists} methods use a different contract and preserve
 *       {@code null} values.</li>
 *   <li><b>Value Equality:</b> every method that compares map <em>values</em> - {@code containsEntry},
 *       {@code removeEntry}, {@code removeEntries}, {@code replace(map, key, oldValue, newValue)},
 *       {@code intersection}, {@code difference} and {@code symmetricDifference} - uses
 *       {@link N#deepEquals(Object, Object)}, so array-valued entries are matched by content. This diverges
 *       deliberately from {@link Map#remove(Object, Object)} and {@link Map#replace(Object, Object, Object)},
 *       which use {@link java.util.Objects#equals(Object, Object)} and compare arrays by identity.</li>
 *   <li><b>Empty Text Is Absent (numeric accessors only):</b> for the <em>numeric</em> type-converting
 *       accessors, a value that is an empty {@link CharSequence} carries no value and is treated exactly
 *       like a {@code null} value: {@code getAsByte}/{@code Short}/{@code Int}/{@code Long}/{@code Float}/
 *       {@code Double} and their {@code OrDefaultIfAbsent} forms return an empty optional or the supplied
 *       default rather than {@code 0}, as do the path accessors that convert to a number -
 *       {@link #getByPathAsInt(Map, String)}, {@link #getByPathAsIntOrDefaultIfAbsent(Map, String, int)} and
 *       {@link #getByPathAs(Map, String, Class)} with a numeric target type.
 *       {@link #getAs(Map, Object, Class)} follows the same rule for a numeric {@code targetType},
 *       because it returns {@link Optional#empty()}
 *       whenever {@link N#convert(Object, Class)} answers {@code null}, which it does for {@code ""} to
 *       every numeric wrapper type. A blank-but-non-empty value such as {@code " "} is malformed rather
 *       than absent and still throws {@link NumberFormatException}.
 *       <br>The <em>non-numeric</em> accessors deliberately treat {@code ""} as a value that is present:
 *       {@link #getAsChar(Map, Object)} maps it to the null character {@code U+0000},
 *       {@link #getAsBoolean(Map, Object)} maps it to {@code false} (as does
 *       {@code getAs(map, key, Boolean.class)}), and {@link #getAsString(Map, Object)} and
 *       {@link #getByPathAsString(Map, String)} return {@code Optional.of("")}.</li>
 *   <li><b>What the numeric accessors accept:</b> {@code getAsByte}/{@code Short}/{@code Int}/{@code Long}/
 *       {@code Float}/{@code Double} convert through the {@link Numbers} parsers, which understand a
 *       {@link Number} and the <em>text</em> of a number. A value of any other type is read from its string
 *       form, so a {@link Character} value such as {@code 'A'} raises a {@link NumberFormatException} rather
 *       than yielding its code unit 65. {@link #getAs(Map, Object, Class)} is a different converter
 *       ({@link N#convert(Object, Class)}) and does <em>not</em> always agree on such a value: it answers
 *       {@code 65} for {@code getAs(map, key, Integer.class)} while throwing for every other numeric target
 *       type. Where the two are documented to agree - a {@code null} value, an empty {@link CharSequence},
 *       and a {@code null} conversion result - they do; elsewhere pick the converter you actually want.</li>
 *   <li><b>Exception Minimization:</b> Methods avoid throwing unnecessary exceptions when contracts
 *       are not violated, preferring sensible defaults for edge cases</li>
 *   <li><b>Empty Over Null:</b> Methods prefer returning empty maps over {@code null} values when possible</li>
 *   <li><b>Null Safety First:</b> Comprehensive {@code null} input handling throughout the API</li>
 * </ul>
 *
 * <p><b>{@code Maps} vs. related map APIs:</b> {@code Maps} provides <em>static utility methods</em> that operate
 * on existing {@link Map} instances. When you instead need a specialized map-like <em>data structure</em>, reach
 * for one of the dedicated types:</p>
 * <table border="1">
 *   <caption>When to use Maps versus related map utilities and data structures</caption>
 *   <tr>
 *     <th>API</th>
 *     <th>What it is</th>
 *     <th>Use it when</th>
 *   </tr>
 *   <tr>
 *     <td>{@code Maps}</td>
 *     <td>static helpers over {@link Map}</td>
 *     <td>you have a plain {@code Map} and need {@code get*}/{@code zip}/{@code invert}/{@code filter}/{@code flatten} operations</td>
 *   </tr>
 *   <tr>
 *     <td>{@link N}</td>
 *     <td>general-purpose facade (also has a few map helpers)</td>
 *     <td>doing broadly-applicable operations; {@code Maps} is the focused home for map-specific logic</td>
 *   </tr>
 *   <tr>
 *     <td>{@link Multimap} / {@link ListMultimap} / {@link SetMultimap}</td>
 *     <td>data structure: one key maps to many values</td>
 *     <td>a key must hold a collection of values</td>
 *   </tr>
 *   <tr>
 *     <td>{@link BiMap}</td>
 *     <td>data structure: bidirectional one-to-one map</td>
 *     <td>you must look up by value as well as by key</td>
 *   </tr>
 *   <tr>
 *     <td>{@link ImmutableMap}</td>
 *     <td>data structure: unmodifiable map</td>
 *     <td>you need a read-only map</td>
 *   </tr>
 *   <tr>
 *     <td>{@link Multiset}</td>
 *     <td>data structure: element maps to an occurrence count</td>
 *     <td>you are counting occurrences</td>
 *   </tr>
 * </table>
 *
 * <p><b>Value Retrieval Method Groups:</b>
 * <br>The {@code get*} methods are organized into five families, each with distinct null-handling and return-type contracts:
 * <ul>
 *   <li><b>Existence-aware ({@code getIfExists}, {@code getByPathIfExists}):</b>
 *       Returns {@link Nullable} - distinguishes between "key/path exists with {@code null}"
 *       ({@code Nullable.of(null)}) and "key/path is missing" ({@code Nullable.empty()}).
 *       Use when you need to tell whether a key or path is present with a {@code null} value.</li>
 *   <li><b>Null-as-absent ({@code getOrDefaultIfAbsent}, {@code getByPathAsOrDefaultIfAbsent},
 *       {@code getAs*OrDefaultIfAbsent}, {@code getValuesOrDefaultIfAbsent}, {@code getOrEmpty*IfAbsent}):</b> Returns a raw/default value
 *       - treats a {@code null} value the same as a missing key and falls back to the supplied default.
 *       Use when {@code null} has no special meaning in your domain.</li>
 *   <li><b>Get-or-create ({@code getOrPut*IfAbsent}):</b> Returns a raw value - like {@code Map.computeIfAbsent}
 *       but treats {@code null} as absent. Inserts the default into the map when the key is absent.
 *       Use for multimap-style "get or create a new collection" patterns.</li>
 *   <li><b>Type-converting ({@code getAs*}, {@code getByPathAs*}, {@code getAs*OrDefaultIfAbsent}):</b> Returns
 *       {@link Optional}, primitive optional variants, or a converted/default value - retrieves and converts the value to
 *       a target type (e.g.&nbsp;{@code int}, {@code String}, {@code LocalDate}). The {@code OrDefaultIfAbsent}
 *       forms treat {@code null} values as absent. Use when map values are heterogeneous (e.g.&nbsp;{@code Map<String, Object>}).</li>
 *   <li><b>Path-based ({@code getByPath*}):</b> Returns raw, {@link Optional}, {@link Nullable}, primitive optional,
 *       or converted/default values -
 *       traverses nested map/collection structures via a dot-separated path with optional {@code [index]} segments.
 *       {@code getByPathIfExists} preserves a resolved {@code null} value, while
 *       {@code getByPathAsOrDefaultIfAbsent} treats a resolved {@code null} value as absent and returns the default.
 *       Use for deep navigation into configuration-style nested maps.</li>
 * </ul>
 *
 * <p><b>Terminology - {@code Exists} vs. {@code Present} vs. {@code Absent}:</b>
 * <br>The suffixes {@code IfExists}, {@code IfPresent}, and {@code IfAbsent} are <em>not</em> interchangeable
 * spellings of one idea; each encodes a distinct null-handling contract:
 * <table border="1">
 *   <caption>Meaning of the IfExists / IfPresent / IfAbsent suffixes</caption>
 *   <tr>
 *     <th>Term</th>
 *     <th>A key/path "counts" when&hellip;</th>
 *     <th>Example methods</th>
 *   </tr>
 *   <tr>
 *     <td>{@code Exists}</td>
 *     <td>the key is contained in the map, or the path can be resolved - a {@code null} value still
 *         "exists"; only a missing key/path (or a {@code null}/empty map) is non-existent. This is
 *         the contract that can tell "present with {@code null} value" apart from "missing".</td>
 *     <td>{@code getIfExists}, {@code getByPathIfExists}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code Present}</td>
 *     <td>the key is contained in the map <em>and</em> its value is non-{@code null}; a key mapped to
 *         {@code null} is <em>not</em> "present".</td>
 *     <td>{@code getValuesIfPresent}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code Absent}</td>
 *     <td>the logical negation of "present" - the key is missing <em>or</em> its value is {@code null}. These
 *         methods fall back to (and, for the {@code getOrPut*} family, insert) a default.</td>
 *     <td>{@code getOrDefaultIfAbsent}, {@code getByPathAsOrDefaultIfAbsent}, {@code getAs*OrDefaultIfAbsent},
 *         {@code getValuesOrDefaultIfAbsent}, {@code getOrEmpty*IfAbsent}, {@code getOrPut*IfAbsent}</td>
 *   </tr>
 * </table>
 *
 * <p><b>Comparison of the flagship value-retrieval methods:</b>
 * <table border="1">
 *   <caption>How each retrieval method treats a missing key/path vs. a {@code null} value</caption>
 *   <tr>
 *     <th>Method</th>
 *     <th>Keys</th>
 *     <th>Returns</th>
 *     <th>Key/path missing</th>
 *     <th>Value is {@code null}</th>
 *     <th>Mutates map?</th>
 *   </tr>
 *   <tr>
 *     <td>{@code getIfExists(map, key)}</td>
 *     <td>one</td>
 *     <td>{@code Nullable<V>}</td>
 *     <td>{@code Nullable.empty()}</td>
 *     <td>{@code Nullable.of(null)} (still "exists")</td>
 *     <td>no</td>
 *   </tr>
 *   <tr>
 *     <td>{@code getOrDefaultIfAbsent(map, key, default)}</td>
 *     <td>one</td>
 *     <td>{@code V}</td>
 *     <td>{@code default}</td>
 *     <td>{@code default} ({@code null} = absent)</td>
 *     <td>no</td>
 *   </tr>
 *   <tr>
 *     <td>{@code getAsOrDefaultIfAbsent(map, key, default, type)}</td>
 *     <td>one</td>
 *     <td>{@code T}</td>
 *     <td>{@code default}</td>
 *     <td>{@code default} ({@code null} = absent)</td>
 *     <td>no</td>
 *   </tr>
 *   <tr>
 *     <td>{@code getByPathAsOrDefaultIfAbsent(map, path, default, type)}</td>
 *     <td>path</td>
 *     <td>{@code T}</td>
 *     <td>{@code default}</td>
 *     <td>{@code default} ({@code null} = absent)</td>
 *     <td>no</td>
 *   </tr>
 *   <tr>
 *     <td>{@code getOrPutIfAbsent(map, key, supplier)}</td>
 *     <td>one</td>
 *     <td>{@code V}</td>
 *     <td>supplied value (also stored)</td>
 *     <td>supplied value (also stored)</td>
 *     <td>yes</td>
 *   </tr>
 *   <tr>
 *     <td>{@code getValuesIfPresent(map, keys)}</td>
 *     <td>many</td>
 *     <td>{@code List<V>}</td>
 *     <td>skipped (not added)</td>
 *     <td>skipped (not added)</td>
 *     <td>no</td>
 *   </tr>
 * </table>
 *
 * <p>In short: {@code getIfExists} and the path {@code *IfExists} methods observe a {@code null}
 * value as distinct from a missing key/path; the null-as-absent/default families collapse
 * {@code null} and missing into "absent"; and {@code getValuesIfPresent} keeps only the keys that
 * resolve to a non-{@code null} value.</p>
 *
 * <p><b>Cross-class accessor naming:</b> the existence-aware lookups here that return a {@link Nullable}
 * rather than throwing — {@code getIfExists} and {@code getByPathIfExists} — carry the {@code *IfExists}
 * suffix. The sibling utility classes spell the same "look up a possibly-absent value without throwing"
 * idea with different verbs: {@link Iterables} uses {@code find*} (e.g.&nbsp;{@code findFirstOrLast}) and
 * {@link Beans} uses {@code *IfPresent} (e.g.&nbsp;{@code getPropValueIfPresent}), while plain {@code get*}
 * methods return a default or throw.</p>
 *
 * <p><b>Core Functional Categories:</b>
 * <ul>
 *   <li><b>Map Creation:</b> {@code zip} for combining separate key/value collections</li>
 *   <li><b>Access Operations:</b> {@code getIfExists}, {@code getOrDefaultIfAbsent}, {@code getByPath*},
 *       and {@code getAs*} retrieval methods with raw, optional, {@code nullable}, and default-returning forms</li>
 *   <li><b>Containment Operations:</b> {@code containsEntry} for key-value pair membership tests</li>
 *   <li><b>Transformation Operations:</b> {@code filter}, {@code invert}, {@code flatInvert}, {@code replaceKeys}</li>
 *   <li><b>Comparison Operations:</b> {@code difference}, {@code symmetricDifference}, {@code intersection}</li>
 *   <li><b>Mutation Operations:</b> {@code putIfAbsent}, {@code putAllIf}, {@code removeIf}, {@code replace}, {@code replaceAll}</li>
 *   <li><b>Conversion Operations:</b> {@code keySet}, {@code values}, {@code entrySet}</li>
 *   <li><b>Restructuring Operations:</b> {@code flatten}, {@code unflatten} for nested map manipulation; {@code transpose} for column-to-row conversion</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Map creation with JDK collection types
 * Map<String, Integer> map = new HashMap<>();
 * map.put("key", 42);
 * Map<String, Integer> linkedMap = new LinkedHashMap<>();
 * Map<String, Integer> sortedMap = new TreeMap<>();
 *
 * // Zip operations for map creation
 * List<String> keys = Arrays.asList("a", "b", "c");
 * List<Integer> nums = Arrays.asList(1, 2, 3);
 * Map<String, Integer> zipped = Maps.zip(keys, nums);           // contains a=1, b=2, and c=3 (iteration order is unspecified)
 *
 * // Existence-aware access (distinguishes null-value from missing-key)
 * Nullable<Integer> nullable = Maps.getIfExists(map, "key");   // returns Nullable.of(42)
 *
 * // Null-as-absent access (treats null same as missing)
 * Integer defaultValue = Maps.getOrDefaultIfAbsent(map, "missing", 0); // returns 0
 *
 * // Type-converting access
 * OptionalInt intVal = Maps.getAsInt(map, "key");               // returns OptionalInt.of(42)
 *
 * // Null-safe operations
 * Nullable<Integer> fromNull = Maps.getIfExists(null, "key");   // returns Nullable.empty()
 *
 * // Transformation operations
 * Map<String, Integer> filtered = Maps.filter(map, (k, v) -> v > 0);
 * Map<Integer, String> inverted = Maps.invert(map);   // returns {42=key}
 *
 * // Conversion operations
 * Set<String> keySet = Maps.keySet(map);
 * Collection<Integer> mapValues = Maps.values(map);
 * }</pre>
 *
 * <p><b>Map Creation Utilities:</b>
 * <ul>
 *   <li><b>Zip Operations:</b> {@code zip()} for combining separate key/value collections,
 *       with optional merge functions, default values, and a custom map supplier</li>
 *   <li><b>Get-or-create:</b> {@code getOrPutIfAbsent()}, {@code getOrPutListIfAbsent()},
 *       {@code getOrPutSetIfAbsent()}, {@code getOrPutMapIfAbsent()} for multimap-style construction</li>
 * </ul>
 *
 * <p><b>Value Retrieval Access:</b>
 * <ul>
 *   <li><b>Existence-aware:</b> {@code getIfExists()} returning {@link Nullable} - preserves {@code null} values</li>
 *   <li><b>Null-as-absent:</b> {@code getOrDefaultIfAbsent()}, {@code getOrEmpty*IfAbsent()} with fallback values</li>
 *   <li><b>Get-or-create:</b> {@code getOrPut*IfAbsent()} - inserts default into map when absent</li>
 *   <li><b>Type-converting:</b> {@code getAs*()}, {@code getAs*OrDefaultIfAbsent()} with automatic type conversion</li>
 *   <li><b>Path-based:</b> {@code getByPath*()}, {@code getByPathIfExists()} for nested map navigation</li>
 * </ul>
 *
 * <p><b>Functional Transformations:</b>
 * <ul>
 *   <li><b>Key Replacement:</b> {@code replaceKeys()}</li>
 *   <li><b>Value Replacement:</b> {@code replaceAll()}</li>
 *   <li><b>Filtering:</b> {@code filter()}, {@code filterByKey()}, {@code filterByValue()}</li>
 *   <li><b>Difference:</b> {@code difference()}, {@code symmetricDifference()}, {@code intersection()}</li>
 *   <li><b>Inversion:</b> {@code invert()}, {@code flatInvert()} for key-value swapping</li>
 *   <li><b>Flattening:</b> {@code flatten()}, {@code unflatten()}; <b>Transposition:</b> {@code transpose()}</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Memory Efficient:</b> Minimal object allocation and copying in operations</li>
 *   <li><b>Eager Evaluation:</b> Operations are performed immediately and return concrete results</li>
 *   <li><b>Result Sizing:</b> Result maps are pre-sized from a <em>capacity hint</em> derived from the
 *       input size. A hint is never a promise about the result size, and is an over-estimate whenever
 *       distinct inputs collapse onto one key: {@code zip} with duplicate keys, {@code invert} and
 *       {@code flatInvert} with duplicate values, and {@code replaceKeys} with a many-to-one converter
 *       all produce fewer entries than the hint. It is an upper bound for the filtering operations
 *       ({@code filter}, {@code filterByKey}, {@code filterByValue}), and only a heuristic for the
 *       comparison operations ({@code difference}, {@code intersection}, {@code symmetricDifference}),
 *       whose result size is not known in advance. {@code zip} derives its hint from
 *       {@link Collection#size()}, so an {@link Iterable} that is not a {@code Collection} contributes a
 *       hint of {@code 0} however many elements it yields</li>
 *   <li><b>Type Preservation:</b> Transformation methods attempt to return a map of the same type
 *       as the input (falling back to {@link java.util.HashMap}/{@link java.util.LinkedHashMap})</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Static Design:</b> All operations are exposed as static methods. The only shared mutable
 *       state is an internal concurrent cache of map classes that cannot be reflectively constructed.</li>
 *   <li><b>Mutating vs. Non-mutating:</b> Many methods (e.g.&nbsp;{@code zip}, {@code filter},
 *       {@code invert}, {@code difference}) return a new map and do not modify their inputs;
 *       others (e.g.&nbsp;{@code putIfAbsent}, {@code putAllIf}, {@code removeIf}, {@code replace},
 *       {@code replaceAll}, {@code replaceKeys}, {@code getOrPut*IfAbsent}) modify the supplied
 *       map in place. Consult each method's documentation</li>
 *   <li><b>Caller Responsibility:</b> Thread safety of an individual call depends on the thread
 *       safety of the supplied map. Multi-step helpers are not made atomic merely because the map
 *       is concurrent, so compound access must be coordinated by the caller.</li>
 * </ul>
 *
 * <p><b>Integration with Java Maps:</b>
 * <ul>
 *   <li><b>Standard Map Interface:</b> Full compatibility with java.util.Map implementations</li>
 *   <li><b>Specialized Maps:</b> Support for SortedMap, NavigableMap, ConcurrentMap</li>
 *   <li><b>Stream Compatibility:</b> Integration with Java 8+ Stream operations</li>
 *   <li><b>Collection Framework:</b> Seamless integration with Java Collections</li>
 * </ul>
 *
 * <p><b>Null Handling Strategy:</b>
 * <ul>
 *   <li><b>Present/Absent Model:</b> Clear distinction between missing keys and {@code null} values</li>
 *   <li><b>Graceful Degradation:</b> The non-mutating methods handle a {@code null} map gracefully,
 *       without exceptions. The methods that would <em>insert</em> ({@code putIfAbsent},
 *       {@code putAllIf}, {@code merge}, {@code getOrPut*IfAbsent})
 *       instead reject a {@code null} map with {@link IllegalArgumentException}, because silently
 *       discarding a write is worse than failing. The methods that only ever <em>change or remove an
 *       existing entry</em> are the in-between case: they mutate, but a {@code null} map cannot contain
 *       the entry they would touch, so they are a no-op - {@code replace} (both overloads),
 *       {@code removeEntry}, {@code removeEntries}, {@code removeKeys}, {@code removeIf*},
 *       {@code replaceAll} and {@code replaceKeys*}</li>
 *   <li><b>Optional Returns:</b> Use of Optional types to avoid {@code null} return values</li>
 *   <li><b>Null Value Support:</b> Proper handling of {@code null} values within map operations</li>
 * </ul>
 *
 * <p><b>Error Handling Strategy:</b>
 * <ul>
 *   <li><b>Contract Preservation:</b> Exceptions thrown only when method contracts are violated</li>
 *   <li><b>Edge Case Handling:</b> Graceful handling of empty maps, {@code null} inputs, missing keys</li>
 *   <li><b>Clear Documentation:</b> Well-defined behavior for all edge cases</li>
 *   <li><b>Consistent API:</b> Uniform error handling patterns across all methods</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use Optional-returning methods to avoid {@code null} pointer exceptions</li>
 *   <li>Leverage the present/absent model for clear {@code null} value handling</li>
 *   <li>Prefer map transformation utilities over manual iteration</li>
 *   <li>Use appropriate map types for specific use cases (HashMap, LinkedHashMap, TreeMap)</li>
 *   <li>Take advantage of the null-safe design for robust code</li>
 *   <li>Use functional operations for complex map processing pipelines</li>
 * </ul>
 *
 * <p><b>Performance Tips:</b>
 * <ul>
 *   <li>Choose appropriate Map implementations based on access patterns</li>
 *   <li>Use bulk operations for better performance with large maps</li>
 *   <li>Consider memory implications when transforming large maps</li>
 *   <li>Use primitive-specific methods when working with numeric values</li>
 * </ul>
 *
 * <p><b>Common Patterns:</b>
 * <ul>
 *   <li><b>Safe Access:</b> {@code Nullable<V> value = Maps.getIfExists(map, key);}</li>
 *   <li><b>Filtering:</b> {@code Map<K, V> filtered = Maps.filter(map, predicate);}</li>
 *   <li><b>Inversion:</b> {@code Map<V, K> inverted = Maps.invert(map);}</li>
 *   <li><b>Zipping:</b> {@code Map<K, V> zipped = Maps.zip(keys, values);}</li>
 * </ul>
 *
 * <p><b>Related Utility Classes:</b>
 * <ul>
 *   <li><b>{@link com.landawn.abacus.util.N}:</b> General utility class with map operations</li>
 *   <li><b>{@link com.landawn.abacus.util.Iterables}:</b> Iterable utilities for map processing</li>
 *   <li><b>{@link com.landawn.abacus.util.Iterators}:</b> Iterator utilities for map entries</li>
 *   <li><b>{@link com.landawn.abacus.util.Strings}:</b> String utilities for map keys/values</li>
 *   <li><b>{@link com.landawn.abacus.util.Beans}:</b> Bean utilities for object-map conversion</li>
 *   <li><b>{@link com.landawn.abacus.util.stream.Stream}:</b> Stream operations for maps</li>
 *   <li><b>{@link java.util.Map}:</b> Core Java map interface</li>
 *   <li><b>{@link java.util.Collections}:</b> Core Java collection utilities</li>
 * </ul>
 *
 * <p><b>Usage Examples: Data Processing Pipeline</b></p>
 * <pre>{@code
 * // Build a map by zipping parallel key/value collections
 * List<String> quarters = Arrays.asList("Q1", "Q2", "Q3", "Q4");
 * List<Double> sales = Arrays.asList(1200.50, 1450.75, 980.25, 1350.00);
 * Map<String, Double> salesData = Maps.zip(quarters, sales);
 *
 * // Filtering
 * Map<String, Double> highPerformance = Maps.filter(salesData,
 *     (quarter, amount) -> amount > 1200.0);   // contains Q1=1200.5, Q2=1450.75, and Q4=1350.0
 *
 * // Filtering by value only
 * Map<String, Double> belowTarget = Maps.filterByValue(salesData,
 *     amount -> amount < 1000.0);              // contains Q3=980.25
 *
 * // Inversion (swap keys and values)
 * Map<Double, String> byAmount = Maps.invert(salesData);
 *
 * // Batch retrieval of values for a set of keys
 * List<Double> firstHalf = Maps.getValuesIfPresent(salesData, Arrays.asList("Q1", "Q2"));
 * }</pre>
 *
 * <p><b>Usage Examples: Configuration Management</b></p>
 * <pre>{@code
 * // Configuration map processing
 * Map<String, String> config = N.asMap("timeout", "45", "environment", "prod", "debug", "true",
 *     "prod.timeout", "60", "empty", " ");
 *
 * // Safe access with defaults
 * int timeout = Maps.getAsInt(config, "timeout").orElse(30);
 * String environment = Maps.getAsString(config, "environment").orElse("development");
 * boolean debugMode = Maps.getAsBoolean(config, "debug").orElse(false);
 *
 * // Validation and filtering
 * // Filter invalid entries
 * Map<String, String> validConfig = Maps.filter(config,
 *     (key, value) -> value != null && !value.trim().isEmpty());
 *
 * // Key-based filtering for timeout-related entries
 * // Extract timeout-related configuration
 * Map<String, String> timeoutConfigs = Maps.filterByKey(validConfig,
 *     key -> key.endsWith(".timeout"));
 *
 * // Environment-specific filtering
 * // Environment-specific configuration
 * String envPrefix = environment + ".";
 * Map<String, String> envConfig = Maps.filterByKey(validConfig,
 *     key -> key.startsWith(envPrefix));
 *
 * // Renaming keys in place (strip the environment prefix)
 * Maps.replaceKeys(envConfig, key -> key.substring(envPrefix.length()));   // envConfig is rekeyed without the environment prefix
 *
 * // Flattening nested configuration
 * // Flatten nested configuration
 * Map<String, Object> nested = N.asMap("db", N.asMap("host", "localhost"));
 * Map<String, Object> flatConfig = Maps.flatten(nested);                   // returns {db.host=localhost}
 * }</pre>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang, Google Guava, and other open source
 * projects under the Apache License 2.0. Methods from these libraries may have been modified for
 * consistency, performance optimization, and enhanced null-safety within the Abacus framework.</p>
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Beans
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.stream.Stream
 * @see com.landawn.abacus.util.u.Optional
 * @see com.landawn.abacus.util.u.Nullable
 * @see java.util.Map
 * @see java.util.HashMap
 * @see java.util.LinkedHashMap
 * @see java.util.TreeMap
 * @see java.util.Collections
 */
public final class Maps {

    private static final Object NONE = ClassUtil.newNullSentinel();

    /** Seed capacity for the key list the {@code removeIf*} methods collect matches into. */
    private static final int REMOVAL_KEYS_INIT_CAPACITY = 7;

    private Maps() {
        // Utility class.
    }

    /**
     * Creates a new entry (key-value pair) with the provided key and value.
     *
     * <p>This method generates a new entry using the provided key and value.
     * The created entry is mutable, meaning that its value can be changed after creation
     * via {@link java.util.Map.Entry#setValue(Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map.Entry<String, Integer> entry = Maps.newEntry("key", 1);
     * // entry.getKey() returns "key"
     * // entry.getValue() returns 1
     * }</pre>
     *
     * @param <K> the type of the key.
     * @param <V> the type of the value.
     * @param key the key of the new entry.
     * @param value the value of the new entry.
     * @return a new Entry with the provided key and value.
     * @deprecated replaced by {@link N#newEntry(Object, Object)}.
     */
    @Deprecated
    public static <K, V> Map.Entry<K, V> newEntry(final K key, final V value) {
        return N.newEntry(key, value);
    }

    /**
     * Creates a new immutable entry with the provided key and value.
     *
     * <p>This method generates a new immutable entry (key-value pair) using the provided key and value.
     * The created entry is immutable, meaning that its key and value cannot be changed after creation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableEntry<String, Integer> entry = Maps.newImmutableEntry("key", 1);
     * // entry.getKey() returns "key"
     * // entry.getValue() returns 1
     * // entry.setValue(2) throws UnsupportedOperationException
     * }</pre>
     *
     * @param <K> the type of the key.
     * @param <V> the type of the value.
     * @param key the key of the new entry.
     * @param value the value of the new entry.
     * @return a new ImmutableEntry with the provided key and value.
     * @deprecated replaced by {@link N#newImmutableEntry(Object, Object)}.
     */
    @Deprecated
    public static <K, V> ImmutableEntry<K, V> newImmutableEntry(final K key, final V value) {
        return N.newImmutableEntry(key, value);
    }

    /**
     * Names of map classes that {@link N#newMap(Class, int)} could not instantiate, so later calls can skip
     * the failing attempt. Names rather than {@code Class} objects: a {@code static} set of {@code Class}
     * objects holds their defining {@code ClassLoader} alive for the life of the JVM, which leaks a
     * classloader per redeploy in a container.
     *
     * <p>The trade-off is that names collide across classloaders: if {@code com.app.MyMap} from one
     * application cannot be constructed, a different {@code com.app.MyMap} loaded elsewhere is skipped too.
     * The only cost of a false skip is a {@code HashMap} result instead of a same-type one, so the
     * collision is preferred to the leak.</p>
     */
    private static final Set<String> UNABLE_CREATED_MAP_CLASS_NAMES = N.newConcurrentHashSet();

    /**
     * Creates an empty map of the same kind as {@code m}, sized for {@code m}'s current entry count.
     * Equivalent to {@code newTargetMap(m, m == null ? 0 : m.size())}.
     *
     * @param m the template map whose runtime type should be mirrored, may be {@code null}
     * @return a new, empty map of the same kind as {@code m}, or a new {@code HashMap} if {@code m} is
     *         {@code null} or its type cannot be instantiated
     */
    @SuppressWarnings("rawtypes")
    static Map newTargetMap(final Map<?, ?> m) {
        return newTargetMap(m, m == null ? 0 : m.size());
    }

    /**
     * Creates an empty map that mirrors the runtime type of {@code m}, so transformation methods can
     * return a result of the same kind as their input.
     *
     * <p>A {@code null} template yields a {@code HashMap}. A {@link SortedMap} template yields a
     * {@link TreeMap} using the template's comparator (the {@code size} hint does not apply to it).
     * Otherwise the template's own class is instantiated with the given expected size; if that class
     * cannot be constructed reflectively, a {@code HashMap} is returned instead and the class is
     * remembered so later calls skip the failing attempt.</p>
     *
     * @param m the template map whose runtime type should be mirrored, may be {@code null}
     * @param size the expected number of entries, used as the initial capacity hint
     * @return a new, empty map of the same kind as {@code m}, or a new {@code HashMap} if {@code m} is
     *         {@code null} or its type cannot be instantiated
     */
    @SuppressWarnings("rawtypes")
    static Map newTargetMap(final Map<?, ?> m, final int size) {
        // N.newHashMap, not new HashMap<>(size): `size` is an expected entry count, and N.newHashMap
        // converts it to a capacity (size / 0.75 + 1). Passing it straight to the HashMap constructor -
        // as the fallbacks here used to - made the reflective and fallback paths of this one method
        // size differently and forced an extra rehash on every fallback.
        if (m == null) {
            return size == 0 ? new HashMap<>() : N.newHashMap(size);
        }

        if (m instanceof SortedMap) {
            return new TreeMap<>(((SortedMap) m).comparator());
        }

        final Class<? extends Map> cls = m.getClass();

        if (UNABLE_CREATED_MAP_CLASS_NAMES.contains(cls.getName())) {
            return N.newHashMap(size);
        }

        try {
            return N.newMap(cls, size);
        } catch (final Exception e) {
            // The size-1 retry distinguishes "this class can never be constructed reflectively" from "this
            // class rejected THIS size". Only the former is remembered: a class whose constructor validates
            // its capacity argument must stay type-preserved at the sizes it does accept, so blacklisting it
            // on the first failure would permanently downgrade every later call to a HashMap. The cost of
            // keeping that distinction is one throwaway instance per fallback for a class that is
            // constructible but refused this size - rare, and cheaper than losing type preservation.
            try {
                N.newMap(cls, 1);
            } catch (final Exception e1) {
                UNABLE_CREATED_MAP_CLASS_NAMES.add(cls.getName());
            }

            return N.newHashMap(size);
        }
    }

    /**
     * Creates an empty map for results whose keys come from {@code m}'s <i>values</i> (such as
     * {@link #invert(Map)} and {@link #flatInvert(Map)}).
     *
     * <p>The result mirrors the template's own class, so it preserves encounter order exactly as far as
     * the template does - a {@code HashMap} template yields an unordered {@code HashMap}. It differs from
     * {@link #newTargetMap(Map, int)} for the two templates whose key equivalence cannot be carried over:
     * a {@link SortedMap} template yields a {@link LinkedHashMap} rather than a {@code TreeMap}, because
     * the template's comparator applies to its keys, and an {@link IdentityHashMap} template yields a
     * {@code LinkedHashMap} too, because its reference equivalence likewise applies to its keys. In both
     * cases the template's encounter order is carried over instead. A {@code null} template yields a
     * {@code HashMap}. If the template's class cannot be constructed reflectively, a {@code LinkedHashMap}
     * is returned instead and the class is remembered so later calls skip the failing attempt.</p>
     *
     * @param m the template map whose runtime type should be mirrored, may be {@code null}
     * @return a new, empty map suitable for holding entries rekeyed from {@code m}'s values
     */
    @SuppressWarnings("rawtypes")
    static Map newOrderingMap(final Map<?, ?> m) {
        if (m == null) {
            return new HashMap<>();
        }

        if (m instanceof SortedMap) {
            return new LinkedHashMap<>();
        }

        // Same reason as the SortedMap branch, one step further: an IdentityHashMap's reference equivalence
        // applies to the TEMPLATE's keys and cannot be reused for the new keys, which are the template's
        // values. Mirroring it stopped equal values from collapsing (invert(..) then reported two entries
        // where its javadoc promises one, and result.get(value) missed an entry the map plainly held), and
        // it silenced invert(Map, BiFunction)'s merge function entirely.
        if (m instanceof IdentityHashMap) {
            return new LinkedHashMap<>();
        }

        final int size = m.size();
        final Class<? extends Map> cls = m.getClass();

        // See newTargetMap(Map, int): `size` is an expected entry count, so it must go through
        // N.newLinkedHashMap rather than straight into the LinkedHashMap capacity constructor.
        if (UNABLE_CREATED_MAP_CLASS_NAMES.contains(cls.getName())) {
            return N.newLinkedHashMap(size);
        }

        try {
            return N.newMap(cls, size);
        } catch (final Exception e) {
            // See newTargetMap(Map, int): the size-1 retry keeps a class that merely rejected this size out
            // of the permanent blacklist.
            try {
                N.newMap(cls, 1);
            } catch (final Exception e1) {
                UNABLE_CREATED_MAP_CLASS_NAMES.add(cls.getName());
            }

            return N.newLinkedHashMap(size);
        }
    }

    /**
     * Returns the key set of the specified map if it is not {@code null} or empty. Otherwise, an empty immutable set is returned.
     * This is a convenience method that avoids {@code null} checks and provides a guaranteed {@code non-null} Set result.
     *
     * <p><b>Note:</b> The returned set is always <em>unmodifiable</em>; mutation attempts throw
     * {@link UnsupportedOperationException}. For a non-empty map it is a live read-only view over
     * {@link Map#keySet()} (later changes to the underlying map are reflected through it); for a
     * {@code null} or empty map a shared immutable empty set is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("one", 1);
     * map.put("two", 2);
     * // keys contains: ["one", "two"]
     * Set<String> keys = Maps.keySet(map);
     *
     * Map<String, Integer> emptyMap = null;
     * Set<String> emptyKeys = Maps.keySet(emptyMap);
     * // emptyKeys is an empty immutable set
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map whose keys are to be returned, may be {@code null}.
     * @return an unmodifiable view of the map's key set if non-empty, otherwise an empty immutable set.
     * @see N#nullToEmpty(Map)
     */
    @Beta
    public static <K> Set<K> keySet(final Map<? extends K, ?> map) {
        return N.isEmpty(map) ? ImmutableSet.empty() : ImmutableSet.wrap((Set<K>) map.keySet());
    }

    /**
     * Returns the collection of values from the specified map if it is not {@code null} or empty.
     * Otherwise, an empty immutable list is returned.
     * This is a convenience method that avoids {@code null} checks and provides a guaranteed {@code non-null} Collection result.
     *
     * <p><b>Note:</b> The returned collection is always <em>unmodifiable</em>; mutation attempts
     * throw {@link UnsupportedOperationException}. For a non-empty map it is a live read-only view
     * over {@link Map#values()} (later changes to the underlying map are reflected through it); for
     * a {@code null} or empty map a shared immutable empty list is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("one", 1);
     * map.put("two", 2);
     * // values contains: [1, 2]
     * Collection<Integer> values = Maps.values(map);
     *
     * Map<String, Integer> emptyMap = null;
     * Collection<Integer> emptyValues = Maps.values(emptyMap);
     * // emptyValues is an empty immutable list
     * }</pre>
     *
     * @param <V> the type of values in the map.
     * @param map the map whose values are to be returned, may be {@code null}.
     * @return an unmodifiable view of the map's values if non-empty, otherwise an empty immutable list.
     * @see N#nullToEmpty(Map)
     */
    @Beta
    public static <V> Collection<V> values(final Map<?, ? extends V> map) {
        return N.isEmpty(map) ? ImmutableList.empty() : ImmutableCollection.wrap((Collection<V>) map.values());
    }

    /**
     * Returns the entry set of the specified map if it is not {@code null} or empty.
     * Otherwise, an empty immutable set is returned.
     * This is a convenience method that avoids {@code null} checks and provides a guaranteed {@code non-null} Set result.
     *
     * <p><b>Note:</b> The returned set is always <em>structurally unmodifiable</em>; structural mutation attempts
     * (add/remove/clear) throw {@link UnsupportedOperationException}. For a non-empty map it is a
     * live view over {@link Map#entrySet()} (later changes to the underlying map are reflected
     * through it); for a {@code null} or empty map a shared immutable empty set is returned.
     * Entries obtained from a non-empty view are the backing map's entries, so
     * {@link Map.Entry#setValue(Object)} may still update that map.</p>
     * <p>The key and value types must match the backing map. To view entries under wider types,
     * copy them into entries with the desired types instead of widening this mutable entry view.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("one", 1);
     * map.put("two", 2);
     * // entries contains the key-value pairs: ["one"=1, "two"=2]
     * Set<Map.Entry<String, Integer>> entries = Maps.entrySet(map);
     *
     * Map<String, Integer> emptyMap = null;
     * Set<Map.Entry<String, Integer>> emptyEntries = Maps.entrySet(emptyMap);
     * // emptyEntries is an empty immutable set
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param <V> the type of values in the map.
     * @param map the map whose entry set is to be returned, may be {@code null}.
     * @return the entry set of the map if non-empty, otherwise an empty immutable set.
     * @see N#nullToEmpty(Map)
     */
    @Beta
    public static <K, V> Set<Map.Entry<K, V>> entrySet(final Map<K, V> map) {
        return N.isEmpty(map) ? ImmutableSet.empty() : ImmutableSet.wrap(map.entrySet());
    }

    /**
     * Creates a Map by zipping together two Iterables, one containing keys and the other containing values.
     * Pairing stops when either Iterable is exhausted. Duplicate keys may make the resulting Map smaller
     * than the number of pairs; the last paired value for each key is retained.
     * The keys and values are associated in the order in which they are provided (i.e., the first key is associated with the first value, and so on).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Same length iterables
     * List<String> keys = Arrays.asList("name", "age", "city");
     * List<String> values = Arrays.asList("John", "25", "New York");
     * // result: {name=John, age=25, city=New York}
     * Map<String, String> result = Maps.zip(keys, values);
     * // Example 2: Different length iterables (keys shorter)
     * List<Integer> ids = Arrays.asList(1, 2);
     * List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
     * Map<Integer, String> userMap = Maps.zip(ids, names);
     * // userMap: {1=Alice, 2=Bob}
     * }</pre>
     *
     * @param <K> the type of keys in the resulting Map.
     * @param <V> the type of values in the resulting Map.
     * @param keys an Iterable of keys for the resulting Map; may be {@code null} or empty.
     * @param values an Iterable of values for the resulting Map; may be {@code null} or empty.
     * @return a new {@code HashMap} where each key from {@code keys} is associated with the
     *         corresponding value from {@code values}; an empty map if either input is {@code null} or empty.
     * @see N#zip(Iterable, Iterable, BiFunction)
     * @see Iterators#zip(Iterator, Iterator, BiFunction)
     */
    public static <K, V> Map<K, V> zip(final Iterable<? extends K> keys, final Iterable<? extends V> values) {
        return zip(keys, values, IntFunctions.ofMap());
    }

    /**
     * Creates a Map by zipping together two Iterables with a custom map supplier.
     * Pairing stops when either Iterable is exhausted. Duplicate keys may make the resulting Map smaller
     * than the number of pairs; the last paired value for each key is retained.
     * The keys and values are associated in the order in which they are provided.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> keys = Arrays.asList("a", "b", "c");
     * List<Integer> values = Arrays.asList(1, 2, 3);
     * LinkedHashMap<String, Integer> result = Maps.zip(keys, values, LinkedHashMap::new);
     * // result: {a=1, b=2, c=3} (maintains insertion order)
     * }</pre>
     *
     * @param <K> the type of keys in the resulting Map.
     * @param <V> the type of values in the resulting Map.
     * @param <M> the type of the resulting Map.
     * @param keys an Iterable of keys for the resulting Map; may be {@code null} or empty.
     * @param values an Iterable of values for the resulting Map; may be {@code null} or empty.
     * @param mapSupplier a function that creates a new Map instance given an expected size
     * @return a Map where each key from {@code keys} is associated with the corresponding value from {@code values};
     *         an empty map (from {@code mapSupplier.apply(0)}) if either input is {@code null} or empty.
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null} or returns {@code null}.
     * @see N#zip(Iterable, Iterable, BiFunction)
     * @see Iterators#zip(Iterator, Iterator, BiFunction)
     */
    public static <K, V, M extends Map<K, V>> M zip(final Iterable<? extends K> keys, final Iterable<? extends V> values,
            final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        if (N.isEmptyCollection(keys) || N.isEmptyCollection(values)) {
            return N.checkArgNotNull(mapSupplier.apply(0), "mapSupplier returned null");
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int keysSize = keys instanceof Collection ? ((Collection<K>) keys).size() : 0;
        final int valuesSize = values instanceof Collection ? ((Collection<V>) values).size() : 0;
        final int minLen = N.min(keysSize, valuesSize);
        final M result = N.checkArgNotNull(mapSupplier.apply(minLen), "mapSupplier returned null");

        while (keyIter.hasNext() && valueIter.hasNext()) {
            result.put(keyIter.next(), valueIter.next());
        }

        return result;
    }

    /**
     * Creates a Map by zipping together two Iterables with a merge function to handle duplicate keys.
     * Pairing stops when either Iterable is exhausted. Duplicate keys and removals caused by a
     * {@code null} merge result may make the resulting Map smaller than the number of pairs.
     * If duplicate keys are encountered, the merge function is used to resolve the conflict.
     *
     * <p><b>Note:</b> Entries are inserted via {@link Map#merge(Object, Object, BiFunction)}, which does not
     * accept {@code null} values - a {@code null} element in {@code values} causes a {@link NullPointerException}.
     * The non-merging {@link #zip(Iterable, Iterable)} overload accepts {@code null} values.</p>
     *
     * <p><b>Note:</b> Also per {@code Map.merge}, a {@code mergeFunction} that returns {@code null}
     * <em>removes</em> the mapping the key currently has instead of storing {@code null}. That removal is
     * <b>not final</b>: the entries are merged one at a time, so a later element with the same key finds it
     * absent and is stored directly, <em>without</em> {@code mergeFunction} being called for it. A key is
     * therefore absent from the result only when {@code mergeFunction} was called for its <em>last</em>
     * occurrence and returned {@code null} -
     * {@code zip(["a","a"], [1,2], (v1,v2) -> null)} yields {@code {}}, but
     * {@code zip(["a","a","a"], [1,2,3], (v1,v2) -> null)} yields {@code {a=3}}. Use a merge function that
     * returns a sentinel, and strip it afterwards, when a dropped key must stay dropped.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> keys = Arrays.asList("a", "b", "a");
     * List<Integer> values = Arrays.asList(1, 2, 3);
     * Map<String, Integer> result = Maps.zip(keys, values,
     *     (v1, v2) -> v1 + v2, HashMap::new);
     * // result: {a=4, b=2} (values for duplicate key "a" are summed: 1 + 3 = 4)
     *
     * // A null merge result removes the key
     * Map<String, Integer> dropped = Maps.zip(Arrays.asList("a", "a"), Arrays.asList(1, 2),
     *     (v1, v2) -> null, HashMap::new);
     * // dropped: {} ("a" was removed, not stored as null)
     *
     * // ...but the removal is not final: a later element re-creates the key
     * Map<String, Integer> back = Maps.zip(Arrays.asList("a", "a", "a"), Arrays.asList(1, 2, 3),
     *     (v1, v2) -> null, HashMap::new);
     * // back: {a=3} (the 3rd element found "a" absent and was stored without calling the merge function)
     * }</pre>
     *
     * @param <K> the type of keys in the resulting Map.
     * @param <V> the type of values in the resulting Map.
     * @param <M> the type of the resulting Map.
     * @param keys an Iterable of keys for the resulting Map; may be {@code null} or empty.
     * @param values an Iterable of values for the resulting Map; may be {@code null} or empty.
     * @param mergeFunction a function used to resolve conflicts when duplicate keys are encountered
     * @param mapSupplier a function that creates a new Map instance given an expected size
     * @return a Map where each key from {@code keys} is associated with the corresponding value from {@code values};
     *         an empty map (from {@code mapSupplier.apply(0)}) if either input is {@code null} or empty.
     * @throws IllegalArgumentException if {@code mergeFunction} or {@code mapSupplier} is {@code null}, or if
     *         {@code mapSupplier} returns {@code null}.
     * @see N#zip(Iterable, Iterable, BiFunction)
     * @see Iterators#zip(Iterator, Iterator, BiFunction)
     */
    public static <K, V, M extends Map<K, V>> M zip(final Iterable<? extends K> keys, final Iterable<? extends V> values,
            final BiFunction<? super V, ? super V, ? extends V> mergeFunction, final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mergeFunction, cs.mergeFunction);
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        if (N.isEmptyCollection(keys) || N.isEmptyCollection(values)) {
            return N.checkArgNotNull(mapSupplier.apply(0), "mapSupplier returned null");
        }

        final Iterator<? extends K> keyIter = keys.iterator();
        final Iterator<? extends V> valueIter = values.iterator();

        final int keysSize = keys instanceof Collection ? ((Collection<K>) keys).size() : 0;
        final int valuesSize = values instanceof Collection ? ((Collection<V>) values).size() : 0;
        final int minLen = N.min(keysSize, valuesSize);
        final M result = N.checkArgNotNull(mapSupplier.apply(minLen), "mapSupplier returned null");

        while (keyIter.hasNext() && valueIter.hasNext()) {
            result.merge(keyIter.next(), valueIter.next(), mergeFunction);
        }

        return result;
    }

    /**
     * Retrieves the value associated with the specified key, wrapped in a {@link Nullable}.
     * <p>This method distinguishes between a key that maps to {@code null} and a key that is missing:
     * <ul>
     * <li>If the key exists and maps to a value, returns {@code Nullable.of(value)}.</li>
     * <li>If the key exists and maps to {@code null}, returns {@code Nullable.of(null)}.</li>
     * <li>If the key does not exist or the map is {@code null}, returns {@code Nullable.empty()}.</li>
     * </ul>
     *
     * <p><b>Contrast with {@link #getOrDefaultIfAbsent(Map, Object, Object)}:</b><br>
     * This method <em>preserves</em> {@code null} values - a key mapped to {@code null} is considered present.
     * {@code getOrDefaultIfAbsent} treats {@code null} the same as a missing key and returns the default instead.
     * Choose this method when you need to distinguish "key present with {@code null}" from "key absent".</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     * map.put("key1", "value1");
     * map.put("key2", null);
     * Maps.getIfExists(map, "key1");   // returns Nullable.of("value1")
     * Maps.getIfExists(map, "key2");   // returns Nullable.of(null)
     * Maps.getIfExists(map, "key3");   // returns Nullable.empty()
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param map the map from which to retrieve the value
     * @param key the key whose associated value is to be returned
     * @return a {@code Nullable} wrapping the value if the key exists, otherwise an empty {@code Nullable}
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see #getIfExists(Map, Object, Object)
     */
    // @ai-ignore getIfExists variants - intentional overloads for type-safe nested map access (1/2/3 level deep). Do not suggest consolidation.
    public static <K, V> Nullable<V> getIfExists(final Map<K, ? extends V> map, final K key) {
        if (N.isEmpty(map)) {
            return Nullable.empty();
        }

        final V val = map.get(key);

        if (val != null || map.containsKey(key)) {
            return Nullable.of(val);
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Retrieves a value from a double-nested map structure (Map-in-Map) using two keys.
     *
     * <p>This is a safe-navigation shorthand for {@code map.get(key).get(k2)}. It returns
     * {@code Nullable.empty()} if any of the following conditions are met:</p>
     * <ul>
     *   <li>The outer map is {@code null}, empty, or does not contain {@code key}.</li>
     *   <li>The inner map (retrieved via {@code key}) is {@code null}, empty, or does not contain {@code k2}.</li>
     * </ul>
     *
     * <p><b>Key presence vs. value {@code null}:</b><br>
     * This method distinguishes between "key not present" and "key present with a {@code null} value".
     * If both keys exist in their respective maps, the result is {@code Nullable.of(value)},
     * even when the value is {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a double-nested map: country -> state -> population
     * Map<String, Map<String, Integer>> populationMap = new HashMap<>();
     * Map<String, Integer> california = new HashMap<>();
     * california.put("Los Angeles", 4000000);
     * california.put("San Francisco", 870000);
     * populationMap.put("California", california);
     * // Successful retrieval
     * Nullable<Integer> pop1 = Maps.getIfExists(populationMap, "California", "Los Angeles");
     * // returns: Nullable.of(4000000)
     * // Missing inner key
     * Nullable<Integer> pop2 = Maps.getIfExists(populationMap, "California", "San Diego");
     * // returns: Nullable.empty()
     * // Missing outer key
     * Nullable<Integer> pop3 = Maps.getIfExists(populationMap, "Texas", "Houston");
     * // returns: Nullable.empty()
     *
     * // Null map
     * Nullable<Integer> pop4 = Maps.getIfExists(null, "California", "Los Angeles");
     * // returns: Nullable.empty()
     * }</pre>
     *
     * @param <K>  the type of keys in the outer map
     * @param <K2> the type of keys in the inner map
     * @param <V2> the type of values in the inner map
     * @param map  the outer map containing nested inner maps, may be {@code null}
     * @param key  the key used to retrieve the inner map from the outer map
     * @param k2   the key used to retrieve the value from the inner map
     * @return a {@code Nullable<V2>} containing the value if both keys are present
     *         (even if the value is {@code null}); otherwise {@code Nullable.empty()}
     *
     * @see #getIfExists(Map, Object)
     * @see #getIfExists(Map, Object, Object, Object)
     */
    public static <K, K2, V2> Nullable<V2> getIfExists(final Map<K, ? extends Map<? extends K2, ? extends V2>> map, final K key, final K2 k2) {
        if (N.isEmpty(map)) {
            return Nullable.empty();
        }

        final Map<? extends K2, ? extends V2> m2 = map.get(key);

        if (N.notEmpty(m2)) {
            final V2 v2 = m2.get(k2);

            if (v2 != null || m2.containsKey(k2)) {
                return Nullable.of(v2);
            }
        }

        return Nullable.empty();
    }

    /**
     * Retrieves a value from a triple-nested map structure (Map-in-Map-in-Map) using three keys.
     *
     * <p>This is a safe-navigation shorthand for {@code map.get(key).get(k2).get(k3)}. It returns
     * {@code Nullable.empty()} if any of the following conditions are met:</p>
     * <ul>
     *   <li>The outermost map is {@code null}, empty, or does not contain {@code key}.</li>
     *   <li>The middle map (retrieved via {@code key}) is {@code null}, empty, or does not contain {@code k2}.</li>
     *   <li>The innermost map (retrieved via {@code k2}) is {@code null}, empty, or does not contain {@code k3}.</li>
     * </ul>
     *
     * <p><b>Key presence vs. value {@code null}:</b><br>
     * This method distinguishes between "key not present" and "key present with a {@code null} value".
     * If all three keys exist in their respective maps, the result is {@code Nullable.of(value)},
     * even when the value is {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a triple-nested map structure: country -> state -> city -> population
     * Map<String, Map<String, Map<String, Integer>>> populationMap = new HashMap<>();
     * Map<String, Map<String, Integer>> usaStates = new HashMap<>();
     * Map<String, Integer> californiaCities = new HashMap<>();
     * californiaCities.put("Los Angeles", 4000000);
     * californiaCities.put("San Francisco", 870000);
     * usaStates.put("California", californiaCities);
     * populationMap.put("USA", usaStates);
     * // Successful retrieval
     * Nullable<Integer> pop1 = Maps.getIfExists(populationMap, "USA", "California", "Los Angeles");
     * // returns: Nullable.of(4000000)
     * // Missing innermost key
     * Nullable<Integer> pop2 = Maps.getIfExists(populationMap, "USA", "California", "San Diego");
     * // returns: Nullable.empty()
     * // Missing middle key
     * Nullable<Integer> pop3 = Maps.getIfExists(populationMap, "USA", "Texas", "Houston");
     * // returns: Nullable.empty()
     * // Missing outermost key
     * Nullable<Integer> pop4 = Maps.getIfExists(populationMap, "Canada", "Ontario", "Toronto");
     * // returns: Nullable.empty()
     *
     * // Null map
     * Nullable<Integer> pop5 = Maps.getIfExists(null, "USA", "California", "Los Angeles");
     * // returns: Nullable.empty()
     * }</pre>
     *
     * @param <K>  the type of keys in the outermost map
     * @param <K2> the type of keys in the middle map
     * @param <K3> the type of keys in the innermost map
     * @param <V3> the type of values in the innermost map
     * @param map  the outermost map containing nested maps, may be {@code null}
     * @param key  the key used to retrieve the middle map from the outermost map
     * @param k2   the key used to retrieve the innermost map from the middle map
     * @param k3   the key used to retrieve the value from the innermost map
     * @return a {@code Nullable<V3>} containing the value if all three keys are present
     *         (even if the value is {@code null}); otherwise {@code Nullable.empty()}
     * @see #getIfExists(Map, Object)
     * @see #getIfExists(Map, Object, Object)
     */
    public static <K, K2, K3, V3> Nullable<V3> getIfExists(final Map<K, ? extends Map<? extends K2, ? extends Map<? extends K3, ? extends V3>>> map,
            final K key, final K2 k2, final K3 k3) {
        if (N.isEmpty(map)) {
            return Nullable.empty();
        }

        final Map<? extends K2, ? extends Map<? extends K3, ? extends V3>> m2 = map.get(key);

        if (N.notEmpty(m2)) {
            final Map<? extends K3, ? extends V3> m3 = m2.get(k2);

            if (N.notEmpty(m3)) {
                final V3 v3 = m3.get(k3);

                if (v3 != null || m3.containsKey(k3)) {
                    return Nullable.of(v3);
                }
            }
        }

        return Nullable.empty();
    }

    /**
     * Resolves a value from a nested map/collection structure using a dot-separated path.
     *
     * <p>The path supports dot-separated keys and optional {@code [index]} segments to access
     * {@link Collection} elements (for example: {@code "user.addresses[0].city"}). Java arrays
     * are not supported by the index syntax. If the path cannot
     * be resolved (missing key, index out of bounds, or root map is {@code null}/empty),
     * this method returns {@code null}.</p>
     *
     * <p>If the path resolves to a {@code null} value, this method also returns {@code null}.
     * Use {@link #getByPathIfExists(Map, String)} when you need to distinguish "path exists
     * with {@code null}" from "path cannot be resolved".</p>
     *
     * <p><b>Path syntax details</b> (shared by every {@code getByPath*} method):</p>
     * <ul>
     *   <li>A {@code null} or empty {@code path} is <em>not</em> a traversal: it is looked up directly as a
     *       single key, so {@code getByPath(map, null)} returns the value stored under the {@code null} key
     *       and {@code getByPath(map, "")} the value stored under {@code ""}.</li>
     *   <li>Empty segments are ignored, so leading, trailing and repeated separators are equivalent to a
     *       single one: {@code ".user.name"}, {@code "user..name"} and {@code "user.name."} all resolve
     *       {@code "user.name"}.</li>
     *   <li>A path that is <em>nothing but</em> separators ({@code "."}, {@code ".."}) therefore has no segments
     *       at all and is unresolvable. It is <b>not</b> reduced to the empty path: {@code getByPath(map, "")}
     *       reads the {@code ""} key, while {@code getByPath(map, ".")} answers {@code null} (and
     *       {@code getByPathIfExists(map, ".")} answers {@code Nullable.empty()}) even when that key is
     *       present.</li>
     *   <li>Consequently a map key that is the empty string is <b>not addressable</b> as an ordinary segment.
     *       Given {@code {"": {"a": 1}, "a": 99}}, the path {@code ".a"} resolves the top-level {@code "a"} (99),
     *       not the nested one - the leading empty segment is dropped rather than matched. {@link #flatten(Map)}
     *       <em>does</em> keep such a segment ({@code {"": {"a": 1}}} flattens to {@code {".a": 1}}), so a
     *       flattened key containing an empty segment cannot be read back with {@code getByPath}. Use
     *       {@link #getIfExists(Map, Object)} on the nested maps directly for those keys.
     *       <br>The one exception is the <em>prefix of an index segment</em>, which is whatever precedes the
     *       first {@code '['} and is allowed to be empty: given {@code {"": ["x", "y"]}}, the path
     *       {@code "[0]"} resolves {@code "x"}. That is a consequence of the index grammar below, not a way
     *       to address an empty key in general - {@code ""} still cannot name an intermediate map.</li>
     *   <li>A segment is read as an index segment when it contains a {@code '['} <em>and</em> ends with a
     *       {@code ']'}. Its bracketed suffix must then match exactly {@code ( '[' [0-9]+ ']' )+} - one or
     *       more groups of plain ASCII digits, running to the end of the segment. Anything else makes the
     *       whole path unresolvable rather than resolving to some other element: an empty or blank index
     *       ({@code "a[]"}, {@code "a[ ]"}), a signed, hexadecimal or otherwise non-decimal index
     *       ({@code "a[+1]"}, {@code "a[0x1]"}, {@code "a[-1]"}), text after the last group
     *       ({@code "a[0]junk]"}), and an index too large for an {@code int} are all rejected.</li>
     *   <li>Because {@code [...]} is reserved syntax, a map key that both contains a {@code '['} and ends
     *       with a {@code ']'} - such as {@code "a[b]"} - is <b>not addressable</b> through a path, even
     *       when the map plainly contains it. A key that merely ends with {@code ']'} and has no
     *       {@code '['}, such as {@code "a]"}, is an ordinary key and resolves normally. Use
     *       {@link #getIfExists(Map, Object)} for keys of the first kind.</li>
     *   <li>Indexing a non-{@link java.util.List} {@code Collection} is supported but walks the iterator, so
     *       it costs O(n) in the element position.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("name", "John", "age", null, "tags", Arrays.asList("a", "b")));
     *
     * String name = Maps.getByPath(map, "user.name");          // returns "John"
     * String firstTag = Maps.getByPath(map, "user.tags[0]");   // returns "a"
     * Integer age = Maps.getByPath(map, "user.age");           // returns null (path exists)
     * String missing = Maps.getByPath(map, "user.height");     // returns null (path missing)
     * }</pre>
     *
     * @param <T> the type of the value to be returned
     * @param map the map to query, may be {@code null}
     * @param path the dot-separated path to the value
     * @return the value at the specified path, or {@code null} if the path cannot be resolved
     * @see #getByPathAsOrDefaultIfAbsent(Map, String, Object, Class)
     * @see #getByPathIfExists(Map, String)
     */
    // @ai-ignore getByPath variants - dot-separated path navigation into nested map/collection structures. Each variant serves a distinct return-type contract (raw, typed, Optional, Nullable). Do not suggest consolidation.
    @MayReturnNull
    public static <T> T getByPath(final Map<String, ?> map, final String path) {
        final Object val = resolveByPath(map, path, null);

        if (val == NONE) {
            return null;
        }

        return (T) val;
    }

    /**
     * Retrieves a value from a nested map/collection structure using a dot-separated path
     * and returns it wrapped in a {@link Nullable} if the path exists.
     *
     * <p>The path syntax is the same as for {@link #getByPath(Map, String)}. If the path
     * can be resolved, the resulting value (which may be {@code null}) is wrapped in
     * {@code Nullable.of(...)}. If the path cannot be resolved (for example a key is
     * missing, an index is out of bounds, or the root map is {@code null}/empty),
     * {@code Nullable.empty()} is returned.</p>
     *
     * <p><b>Null vs. missing path:</b><br>
     * This method distinguishes between "path exists but value is {@code null}" and
     * "path does not exist":</p>
     *
     * <ul>
     *   <li>Path exists - {@code Nullable.of(value)} (even when {@code value == null})</li>
     *   <li>Path missing - {@code Nullable.empty()}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("name", "John", "age", null));
     *
     * // name.isPresent() = true, name.get() = "John"
     * Nullable<String> name = Maps.getByPathIfExists(map, "user.name");
     *
     * // age.isPresent() = true, age.get() = null
     * Nullable<Integer> age = Maps.getByPathIfExists(map, "user.age");
     *
     * Nullable<String> email = Maps.getByPathIfExists(map, "user.email");
     * // email.isPresent() = false
     * }</pre>
     *
     * @param <T>  the expected type of the value
     * @param map  the root map to traverse, may be {@code null}
     * @param path the dot-separated path with optional {@code [index]} segments
     * @return a {@code Nullable<T>} containing the value if the path exists (even if the
     *         value is {@code null}); otherwise {@code Nullable.empty()}
     * @see #getByPath(Map, String)
     */
    public static <T> Nullable<T> getByPathIfExists(final Map<String, ?> map, final String path) {
        final Object val = resolveByPath(map, path, null);

        if (val == NONE) {
            return Nullable.empty();
        }

        return Nullable.of((T) val);
    }

    /**
     * Retrieves a value from a nested map/collection structure using a dot-separated path
     * and converts it to an {@code int} if present.
     *
     * <p>The path syntax is the same as for {@link #getByPath(Map, String)}. If the path
     * cannot be resolved, if it resolves to {@code null}, or if it resolves to an empty
     * {@link CharSequence}, this method returns {@link OptionalInt#empty()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("age", "25", "score", ""));
     *
     * OptionalInt age = Maps.getByPathAsInt(map, "user.age");         // OptionalInt.of(25)
     * OptionalInt blank = Maps.getByPathAsInt(map, "user.score");     // OptionalInt.empty() ("" carries no value)
     * OptionalInt missing = Maps.getByPathAsInt(map, "user.rank");    // OptionalInt.empty()
     * }</pre>
     *
     * @param map  the root map to traverse, may be {@code null}
     * @param path the dot-separated path with optional {@code [index]} segments
     * @return an {@code OptionalInt} containing the resolved integer value, or empty if the
     *         path cannot be resolved, resolves to {@code null}, or resolves to an empty {@code CharSequence}
     * @throws NumberFormatException if the value is a non-empty {@code String} that does not spell an integer.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is
     *         malformed, and a fractional spelling such as {@code "25.9"} is rejected here even though the
     *         {@code Number} {@code 25.9} would be truncated - the same split the {@code getAs*} accessors have.
     * @throws ArithmeticException if the value is a {@code Number}, or a numeric {@code String}, whose value lies outside the {@code int} range.
     * @see #getAsInt(Map, Object)
     * @see #getByPathAsIntOrDefaultIfAbsent(Map, String, int)
     * @see #getByPathAsString(Map, String)
     * @see #getByPathAs(Map, String, Class)
     */
    public static OptionalInt getByPathAsInt(final Map<String, ?> map, final String path) throws NumberFormatException, ArithmeticException {
        final Object val = resolveByPath(map, path, null);

        if (val == NONE || val == null || isEmptyText(val)) {
            return OptionalInt.empty();
        } else {
            // One range-checked conversion for both Number and String values. The Number branch used
            // Number.intValue(), which narrows out-of-range values silently, while the String branch
            // already range-checked - so the same logical value behaved differently by storage type.
            return OptionalInt.of(Numbers.toInt(val));
        }
    }

    /**
     * Retrieves a value from a nested map/collection structure using a dot-separated path,
     * converts it to an {@code int} if present, or returns {@code defaultValue} if the
     * path is absent, resolves to {@code null}, or resolves to an empty {@link CharSequence}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("age", "25", "score", ""));
     *
     * int age = Maps.getByPathAsIntOrDefaultIfAbsent(map, "user.age", 0);          // 25
     * int blank = Maps.getByPathAsIntOrDefaultIfAbsent(map, "user.score", -1);     // -1 ("" carries no value)
     * int missing = Maps.getByPathAsIntOrDefaultIfAbsent(map, "user.rank", -1);    // -1
     * }</pre>
     *
     * @param map          the root map to traverse, may be {@code null}
     * @param path         the dot-separated path with optional {@code [index]} segments
     * @param defaultValue the value to return if the path cannot be resolved, resolves to {@code null},
     *                     or resolves to an empty {@code CharSequence}
     * @return the resolved integer value, or {@code defaultValue} if the path cannot be
     *         resolved, resolves to {@code null}, or resolves to an empty {@code CharSequence}
     * @throws NumberFormatException if the value is a non-empty {@code String} that does not spell an integer.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is
     *         malformed, and a fractional spelling such as {@code "25.9"} is rejected here even though the
     *         {@code Number} {@code 25.9} would be truncated - the same split the {@code getAs*} accessors have.
     * @throws ArithmeticException if the value is a {@code Number}, or a numeric {@code String}, whose value lies outside the {@code int} range.
     * @see #getAsIntOrDefaultIfAbsent(Map, Object, int)
     * @see #getByPathAsInt(Map, String)
     */
    public static int getByPathAsIntOrDefaultIfAbsent(final Map<String, ?> map, final String path, final int defaultValue)
            throws NumberFormatException, ArithmeticException {
        final Object val = resolveByPath(map, path, null);

        if (val == NONE || val == null || isEmptyText(val)) {
            return defaultValue;
        } else {
            // See getByPathAsInt(Map, String): range-checked for Number values too.
            return Numbers.toInt(val);
        }
    }

    /**
     * Retrieves a value from a nested map/collection structure using a dot-separated path
     * and converts it to {@code String} if present.
     *
     * <p>The path syntax is the same as for {@link #getByPath(Map, String)}. If the path
     * cannot be resolved, or if it resolves to {@code null}, this method returns
     * {@link Optional#empty()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("name", "John", "age", 25));
     *
     * Optional<String> name = Maps.getByPathAsString(map, "user.name");   // Optional.of("John")
     * Optional<String> age = Maps.getByPathAsString(map, "user.age");     // Optional.of("25")
     * }</pre>
     *
     * @param map  the root map to traverse, may be {@code null}
     * @param path the dot-separated path with optional {@code [index]} segments
     * @return an {@code Optional<String>} containing the resolved string value, converted if
     *         necessary; otherwise empty if the path cannot be resolved or resolves to {@code null}
     * @see #getAsString(Map, Object)
     * @see #getByPathAsStringOrDefaultIfAbsent(Map, String, String)
     * @see #getByPathAsInt(Map, String)
     * @see #getByPathAs(Map, String, Class)
     */
    public static Optional<String> getByPathAsString(final Map<String, ?> map, final String path) {
        final Object val = resolveByPath(map, path, null);

        if (val == NONE || val == null) {
            return Optional.empty();
        } else if (val instanceof String) {
            return Optional.of((String) val);
        } else {
            return Optional.of(N.stringOf(val));
        }
    }

    /**
     * Retrieves a value from a nested map/collection structure using a dot-separated path,
     * converts it to {@code String} if present, or returns {@code defaultValue} if the
     * path is absent or resolves to {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("name", "John", "age", 25));
     *
     * String name = Maps.getByPathAsStringOrDefaultIfAbsent(map, "user.name", "Unknown");       // "John"
     * String missing = Maps.getByPathAsStringOrDefaultIfAbsent(map, "user.email", "Unknown");   // "Unknown"
     * }</pre>
     *
     * @param map          the root map to traverse, may be {@code null}
     * @param path         the dot-separated path with optional {@code [index]} segments
     * @param defaultValue the default value to return if the path cannot be resolved or resolves to {@code null}; must not be {@code null}
     * @return the resolved string value, or {@code defaultValue} if the path cannot be
     *         resolved or resolves to {@code null}
     * @throws IllegalArgumentException if {@code defaultValue} is {@code null}.
     * @see #getAsStringOrDefaultIfAbsent(Map, Object, String)
     * @see #getByPathAsString(Map, String)
     */
    public static String getByPathAsStringOrDefaultIfAbsent(final Map<String, ?> map, final String path, final String defaultValue)
            throws IllegalArgumentException {
        N.checkArgNotNull(defaultValue, cs.defaultValue); // NOSONAR

        final Object val = resolveByPath(map, path, null);

        if (val == NONE || val == null) {
            return defaultValue;
        } else if (val instanceof String) {
            return (String) val;
        } else {
            return N.stringOf(val);
        }
    }

    /**
     * Retrieves a value from a nested map/collection structure using a dot-separated path
     * and converts it to {@code targetType} if present.
     *
     * <p>The path syntax is the same as for {@link #getByPath(Map, String)}. If the path
     * cannot be resolved, if it resolves to {@code null}, or if the conversion answers {@code null},
     * this method returns {@link Optional#empty()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("age", "25", "score", ""));
     *
     * Optional<Integer> age = Maps.getByPathAs(map, "user.age", Integer.class);     // Optional.of(25)
     * Optional<Integer> blank = Maps.getByPathAs(map, "user.score", Integer.class); // Optional.empty()
     * Optional<Long> missing = Maps.getByPathAs(map, "user.id", Long.class);        // Optional.empty()
     * }</pre>
     *
     * @param <T>        the target type
     * @param map        the root map to traverse, may be {@code null}
     * @param path       the dot-separated path with optional {@code [index]} segments
     * @param targetType the target type to convert the value to; must not be {@code null}
     * @return an {@code Optional<T>} containing the resolved and converted value, or empty if the path
     *         cannot be resolved, resolves to {@code null}, or the conversion itself answers {@code null}
     *         (as {@link N#convert(Object, Class)} does for {@code ""} to any numeric wrapper)
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @see #getAs(Map, Object, Class)
     * @see #getByPathAsOrDefaultIfAbsent(Map, String, Object, Class)
     * @see #getByPathAsInt(Map, String)
     * @see #getByPathAsString(Map, String)
     */
    public static <T> Optional<T> getByPathAs(final Map<String, ?> map, final String path, final Class<? extends T> targetType)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType); // NOSONAR

        final Object val = resolveByPath(map, path, targetType);

        if (val == NONE || val == null) {
            return Optional.empty();
        } else {
            return Optional.of((T) val);
        }
    }

    /**
     * Resolves a value from a nested map/collection structure using a dot-separated path,
     * converts it to the specified target type if necessary, and returns {@code defaultValue}
     * if the path cannot be resolved or resolves to {@code null}.
     *
     * <p>This method uses the same null-as-absent rule as
     * {@link #getOrDefaultIfAbsent(Map, Object, Object)}: a resolved {@code null} value is
     * treated the same as a missing path. If the path resolves to a non-{@code null} value that
     * is not assignable to {@code targetType}, the value is converted using
     * {@link N#convert(Object, Class)}.</p>
     *
     * <p><b>Note:</b> {@code defaultValue} is returned as-is when the path is absent or resolves
     * to {@code null}; it is not used to determine the conversion target and must not be {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user", N.asMap("age", "25", "email", null));
     *
     * int age = Maps.getByPathAsOrDefaultIfAbsent(map, "user.age", 0, Integer.class);            // returns 25 (converted from String)
     * int missing = Maps.getByPathAsOrDefaultIfAbsent(map, "user.height", 180, Integer.class);   // returns 180
     * String email = Maps.getByPathAsOrDefaultIfAbsent(map, "user.email", "unknown", String.class);
     * // returns "unknown" (null treated as absent)
     * }</pre>
     *
     * @param <T>          the type of the value to be returned
     * @param map          the map to query, may be {@code null}
     * @param path         the dot-separated path to the value
     * @param defaultValue the default value to return if the path cannot be resolved or resolves to {@code null}; must not be {@code null}
     * @param targetType   the class of the type to which a found, non-{@code null} value should be converted; must not be {@code null}
     * @return the resolved value converted to {@code targetType} if necessary; otherwise {@code defaultValue}
     *         if the root map is {@code null}/empty, the path cannot be resolved, the resolved value is
     *         {@code null}, or the conversion itself answers {@code null}
     * @throws IllegalArgumentException if {@code defaultValue} or {@code targetType} is {@code null}.
     * @see #getByPath(Map, String)
     * @see #getByPathIfExists(Map, String)
     * @see #getByPathAs(Map, String, Class)
     * @see #getAsOrDefaultIfAbsent(Map, Object, Object, Class)
     */
    public static <T> T getByPathAsOrDefaultIfAbsent(final Map<String, ?> map, final String path, final T defaultValue, final Class<? extends T> targetType)
            throws IllegalArgumentException {
        N.checkArgNotNull(defaultValue, cs.defaultValue); // NOSONAR
        N.checkArgNotNull(targetType, cs.targetType); // NOSONAR

        final Object val = resolveByPath(map, path, targetType);

        if (val == NONE || val == null) {
            return defaultValue;
        } else {
            return (T) val;
        }
    }

    /**
     * Shared resolver behind every {@code getByPath*} method.
     *
     * <p>Returns the private {@link #NONE} sentinel when the path cannot be resolved, and the resolved
     * value - possibly {@code null}, converted to {@code targetType} when one is given - otherwise. The
     * distinction matters: {@code getByPathIfExists} must observe a resolved {@code null} as "present",
     * while the null-as-absent callers collapse both into their own default.</p>
     *
     * <p>{@code NONE} rather than the caller's own default value: a caller default is an ordinary object
     * that a map is free to actually contain, so using it as the not-found marker forced every return
     * site to re-check {@code containsKey} to tell "found it, and it happens to be that instance" from
     * "not found". {@code NONE} is private and unreachable from outside, so the check is unnecessary.</p>
     */
    @SuppressWarnings("rawtypes")
    private static Object resolveByPath(final Map<String, ?> map, final String path, final Class<?> targetType) {
        if (N.isEmpty(map)) {
            return NONE;
        } else if (N.isEmpty(path)) {
            // A null/empty path is not a traversal - it is looked up directly as a single key.
            final Object ret = getOrDefaultByPresence(map, path);

            return ret == NONE ? NONE : convertIfNecessary(ret, targetType);
        }

        final String[] keys = Strings.split(path, '.');
        Map intermediateMap = map;
        String key = null;

        for (int i = 0, len = keys.length; i < len; i++) {
            key = keys[i];

            if (N.isEmpty(intermediateMap)) {
                return NONE;
            }

            // A segment is an index segment only when it has BOTH brackets. A trailing ']' alone used to be
            // enough, which made an ordinary key that happens to end in ']' unresolvable: getByPath(map, "a]")
            // answered null for a map that plainly contained "a]". Only "[...]" is reserved syntax.
            final int idx = key.indexOf('[');

            if (idx >= 0 && key.charAt(key.length() - 1) == ']') {
                final int[] indexes = parseIndexes(key, idx);

                if (indexes == null) {
                    return NONE;
                }

                final Object next = intermediateMap.get(key.substring(0, idx));

                if (!(next instanceof Collection)) {
                    return NONE;
                }

                Collection intermediateColl = (Collection) next;

                for (int j = 0, idxLen = indexes.length; j < idxLen; j++) {
                    // No `indexes[j] < 0` test: parseIndexes accepts ASCII digits only, so an index is
                    // always non-negative by construction.
                    if (N.isEmpty(intermediateColl) || intermediateColl.size() <= indexes[j]) {
                        return NONE;
                    } else {
                        if (j == idxLen - 1) {
                            if (i == len - 1) {
                                final Object ret = N.getElement(intermediateColl, indexes[j]);

                                return convertIfNecessary(ret, targetType);
                            } else {
                                final Object nextMap = N.getElement(intermediateColl, indexes[j]);

                                if (!(nextMap instanceof Map)) {
                                    return NONE;
                                }

                                intermediateMap = (Map) nextMap;
                            }
                        } else {
                            final Object nextColl = N.getElement(intermediateColl, indexes[j]);

                            if (!(nextColl instanceof Collection)) {
                                return NONE;
                            }

                            intermediateColl = (Collection) nextColl;
                        }
                    }
                }
            } else {
                if (i == len - 1) {
                    final Object ret = getOrDefaultByPresence(intermediateMap, key);

                    return ret == NONE ? NONE : convertIfNecessary(ret, targetType);
                } else {
                    final Object nextMap = intermediateMap.get(key);

                    if (!(nextMap instanceof Map)) {
                        return NONE;
                    }

                    intermediateMap = (Map) nextMap;
                }
            }
        }

        return NONE;
    }

    /**
     * Parses the {@code [i]}, {@code [i][j]}... indexes out of the suffix of one path segment, or returns
     * {@code null} if that suffix is malformed.
     *
     * <p>The accepted grammar is exactly {@code ( '[' [0-9]+ ']' )+}, applied to <em>everything</em> from
     * {@code fromIndex} (the first {@code '['}) to the end of the segment. Two earlier bugs both came from
     * validating less than the whole suffix:</p>
     * <ul>
     *   <li>The indexes used to be pulled out with {@code Strings.substringsBetween} and checked
     *       individually, so any text <em>between</em> or <em>after</em> the bracket groups was simply not
     *       looked at: {@code "a[0]junk]"} parsed as {@code a[0]} and silently resolved the first element
     *       of {@code a} instead of failing.</li>
     *   <li>Each index was parsed with {@link Numbers#toInt(String)}, a general numeric parser, so an index
     *       typo was accepted as a different valid index rather than rejected - {@code "a[0x1]"} and
     *       {@code "a[+1]"} both resolved element 1. Requiring plain ASCII digits makes an index mean one
     *       thing only. It also makes a negative index unrepresentable, which is why the caller no longer
     *       range-checks for one.</li>
     * </ul>
     *
     * @param key the whole path segment, for example {@code "tags[0][1]"}
     * @param fromIndex the offset of the first {@code '['} in {@code key}
     * @return the parsed indexes, or {@code null} if the suffix does not match the grammar (including an
     *         index with more digits than an {@code int} can hold)
     */
    private static int[] parseIndexes(final String key, final int fromIndex) {
        final int len = key.length();
        int count = 0;

        // First pass validates the entire suffix and counts the groups, so the array is allocated once and
        // only for a segment that is well-formed all the way to its last character.
        for (int i = fromIndex; i < len;) {
            if (key.charAt(i) != '[') {
                return null;
            }

            final int close = key.indexOf(']', i + 1);

            if (close < 0 || close == i + 1) { // no closing bracket, or an empty "[]"
                return null;
            }

            for (int d = i + 1; d < close; d++) {
                final char c = key.charAt(d);

                if (c < '0' || c > '9') {
                    return null;
                }
            }

            count++;
            i = close + 1;
        }

        if (count == 0) {
            return null;
        }

        final int[] indexes = new int[count];

        for (int i = fromIndex, j = 0; j < count; j++) {
            final int close = key.indexOf(']', i + 1);

            try {
                indexes[j] = Integer.parseInt(key, i + 1, close, 10);
            } catch (final NumberFormatException e) {
                // Digits only, so the sole way to get here is an index too large for an int.
                return null;
            }

            i = close + 1;
        }

        return indexes;
    }

    /**
     * Returns the value {@code key} maps to, or {@link #NONE} if the map does not contain {@code key}.
     * A key present with a {@code null} value answers {@code null}, not {@code NONE}.
     */
    private static Object getOrDefaultByPresence(final Map<?, ?> map, final Object key) {
        final Object val = map.get(key);

        return val == null && !map.containsKey(key) ? NONE : val;
    }

    private static Object convertIfNecessary(final Object val, final Class<?> targetType) {
        if (val == null || targetType == null || targetType.isAssignableFrom(val.getClass())) {
            return val;
        } else {
            return N.convert(val, targetType);
        }
    }

    /**
     * Answers whether a non-{@code null} map value is an empty {@link CharSequence}, which the
     * type-converting accessors treat as carrying no value at all.
     *
     * <p>{@link Numbers#toInt(Object)} and its siblings fall back to their {@code defaultValue} (zero, as
     * these accessors call them) for {@code null} or empty input. The {@code getAs*} methods have already
     * excluded {@code null} by the time they convert, so that fallback could only ever fire for an empty
     * {@code CharSequence} - silently reporting {@code ""} as a parsed {@code 0} while {@code " "} threw
     * {@code NumberFormatException}. Treating {@code ""} as absent instead agrees with
     * {@link N#convert(Object, Class)}, which answers {@code null} for {@code ""} to every numeric wrapper
     * type, and so keeps {@code getAsInt} and {@code getAs(map, key, Integer.class)} consistent.</p>
     */
    private static boolean isEmptyText(final Object val) {
        return val instanceof CharSequence && ((CharSequence) val).isEmpty();
    }

    /**
     * Returns the value associated with the specified key, or {@code defaultValue} if the key is
     * considered <em>absent</em>.
     *
     * <p>A key is treated as absent if:
     * <ul>
     *   <li>the map is {@code null} or empty, or</li>
     *   <li>the map does not contain the key, or</li>
     *   <li>the value mapped to the key is {@code null}</li>
     * </ul>
     *
     * <p>This method therefore differs from {@link Map#getOrDefault(Object, Object)} in that
     * a {@code null} value is treated the same as a missing key.</p>
     *
     * <p><b>Contrast with {@link #getIfExists(Map, Object)}:</b><br>
     * This method treats a {@code null} value the same as a missing key (null-as-absent semantics).
     * {@code getIfExists} <em>preserves</em> {@code null} - a key mapped to {@code null} is considered present
     * and returns {@code Nullable.of(null)}.
     * Choose this method when {@code null} has no special meaning and you just need a fallback value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     * map.put("key1", "value1");
     * map.put("key2", null);
     *
     * Maps.getOrDefaultIfAbsent(map, "key1", "default");   // returns "value1"
     * Maps.getOrDefaultIfAbsent(map, "key2", "default");   // returns "default" (null treated as absent)
     * Maps.getOrDefaultIfAbsent(map, "key3", "default");   // returns "default"
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map from which to retrieve the value; may be {@code null} or empty
     * @param key the key whose associated value is to be returned
     * @param defaultValue the value to return if the key is absent; must not be {@code null}
     * @return the value mapped to {@code key}, or {@code defaultValue} if the key is absent
     * @throws IllegalArgumentException if {@code defaultValue} is {@code null}.
     * @see #getIfExists(Map, Object)
     * @see #getAsOrDefaultIfAbsent(Map, Object, Object, Class)
     */
    // @ai-ignore getOrDefaultIfAbsent variants - null-treating-as-absent retrieval with default values/suppliers and nested map support. Do not suggest consolidation.
    public static <K, V> V getOrDefaultIfAbsent(final Map<K, ? extends V> map, final K key, final V defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(defaultValue, cs.defaultValue);

        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final V val = map.get(key);

        // if (val != null || map.containsKey(key)) {
        if (val == null) {
            return defaultValue;
        } else {
            return val;
        }
    }

    /**
     * Returns the value from a two-level nested map structure, or {@code defaultValue} if the value
     * is considered <em>absent</em>.
     *
     * <p>The lookup proceeds as follows:
     * <ol>
     *   <li>Retrieve the inner map associated with {@code key}</li>
     *   <li>Retrieve the value associated with {@code k2} from the inner map</li>
     * </ol>
     *
     * <p>The value is considered absent if:
     * <ul>
     *   <li>the outer map is {@code null} or empty, or</li>
     *   <li>no inner map is associated with {@code key}, or</li>
     *   <li>the inner map does not contain {@code k2}, or</li>
     *   <li>the mapped value is {@code null}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Map<String, Integer>> nested = new HashMap<>();
     * nested.put("data", Collections.singletonMap("count", 5));
     *
     * Maps.getOrDefaultIfAbsent(nested, "data", "count", 0);      // returns 5
     * Maps.getOrDefaultIfAbsent(nested, "data", "missing", 0);    // returns 0
     * Maps.getOrDefaultIfAbsent(nested, "missing", "count", 0);   // returns 0
     * }</pre>
     *
     * @param <K> the outer map key type
     * @param <K2> the inner map key type
     * @param <V2> the value type
     * @param map the outer map; may be {@code null} or empty
     * @param key the key used to retrieve the inner map
     * @param k2 the key used to retrieve the value from the inner map
     * @param defaultValue the value to return if the lookup result is absent; must not be {@code null}
     * @return the resolved value, or {@code defaultValue} if absent
     * @throws IllegalArgumentException if {@code defaultValue} is {@code null}.
     * @see #getIfExists(Map, Object, Object)
     */
    public static <K, K2, V2> V2 getOrDefaultIfAbsent(final Map<K, ? extends Map<? extends K2, ? extends V2>> map, final K key, final K2 k2,
            final V2 defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(defaultValue, cs.defaultValue);

        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Map<? extends K2, ? extends V2> m2 = map.get(key);

        if (N.notEmpty(m2)) {
            final V2 v2 = m2.get(k2);

            if (v2 != null) {
                return v2;
            }
        }

        return defaultValue;
    }

    /**
     * Returns the value associated with the specified key, or a value supplied by
     * {@code defaultValueSupplier} if the key is considered <em>absent</em>.
     *
     * <p>A key is treated as absent if the map is {@code null} or empty, the key is not present,
     * or the associated value is {@code null}.</p>
     *
     * <p>The supplier is invoked <strong>only</strong> when the key is absent and its result
     * must not be {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     * map.put("key1", "value1");
     * map.put("key2", null);
     *
     * Maps.getOrDefaultIfAbsent(map, "key1", () -> "default");                          // returns "value1"
     * Maps.getOrDefaultIfAbsent(map, "key2", () -> "default");                          // returns "default" (null treated as absent)
     * Maps.getOrDefaultIfAbsent(map, "key3", () -> "default");                          // returns "default" (key missing)
     * Maps.getOrDefaultIfAbsent((Map<String, String>) null, "key1", () -> "default");   // returns "default" (null map)
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map from which to retrieve the value; may be {@code null} or empty
     * @param key the key whose associated value is to be returned
     * @param defaultValueSupplier supplies the value to return if the key is absent
     * @return the mapped value, or the value supplied by {@code defaultValueSupplier} if absent
     * @throws IllegalArgumentException if {@code defaultValueSupplier} is {@code null} or returns {@code null}.
     */
    public static <K, V> V getOrDefaultIfAbsent(final Map<K, ? extends V> map, final K key, final Supplier<? extends V> defaultValueSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(defaultValueSupplier, cs.defaultValueSupplier);

        if (N.isEmpty(map)) {
            return N.checkArgNotNull(defaultValueSupplier.get(), "defaultValueSupplier returned null");
        }

        final V val = map.get(key);

        if (val == null) {
            return N.checkArgNotNull(defaultValueSupplier.get(), "defaultValueSupplier returned null");
        } else {
            return val;
        }
    }

    /**
     * Returns the List value to which the specified key is mapped, or an empty <b>immutable</b> List if the key is absent.
     * A key is considered absent if the map is empty, contains no mapping for the key, or the mapped value is {@code null}.
     *
     * <p><b>Important:</b> The empty List returned when the key is absent is <em>immutable</em> (via {@link N#emptyList()}).
     * Any attempt to modify it (e.g.&nbsp;{@code add}, {@code remove}) will throw {@link UnsupportedOperationException}.
     * If you need a mutable list, use {@link #getOrPutListIfAbsent(Map, Object)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<String>> map = new HashMap<>();
     * map.put("fruits", Arrays.asList("apple", "banana"));
     * map.put("empty", null);
     *
     * List<String> result1 = Maps.getOrEmptyListIfAbsent(map, "fruits");    // returns [apple, banana]
     * List<String> result2 = Maps.getOrEmptyListIfAbsent(map, "empty");     // returns immutable empty list
     * List<String> result3 = Maps.getOrEmptyListIfAbsent(map, "missing");   // returns immutable empty list
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <E> the type of elements in the list.
     * @param map the map from which to retrieve the value; may be {@code null} or empty.
     * @param key the key whose associated value is to be returned.
     * @return the List value mapped by the key, or an empty <b>immutable</b> List if the key is absent.
     * @see N#emptyList()
     * @see #getOrPutListIfAbsent(Map, Object)
     */
    // @ai-ignore getOrEmpty*IfAbsent variants - return empty immutable collection (List/Set/Map) when key is absent. Do not suggest consolidation.
    public static <K, E> List<E> getOrEmptyListIfAbsent(final Map<K, ? extends List<E>> map, final K key) {
        if (N.isEmpty(map)) {
            return N.emptyList();
        }

        final List<E> val = map.get(key);

        if (val == null) {
            return N.emptyList();
        }

        return val;
    }

    /**
     * Returns the Set value to which the specified key is mapped, or an empty <b>immutable</b> Set if the key is absent.
     * A key is considered absent if the map is empty, contains no mapping for the key, or the mapped value is {@code null}.
     *
     * <p><b>Important:</b> The empty Set returned when the key is absent is <em>immutable</em> (via {@link N#emptySet()}).
     * Any attempt to modify it (e.g.&nbsp;{@code add}, {@code remove}) will throw {@link UnsupportedOperationException}.
     * If you need a mutable set, use {@link #getOrPutSetIfAbsent(Map, Object)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Set<Integer>> map = new HashMap<>();
     * map.put("primes", new LinkedHashSet<>(Arrays.asList(2, 3, 5, 7)));
     * map.put("empty", null);
     *
     * Set<Integer> result1 = Maps.getOrEmptySetIfAbsent(map, "primes");    // returns [2, 3, 5, 7]
     * Set<Integer> result2 = Maps.getOrEmptySetIfAbsent(map, "empty");     // returns immutable empty set
     * Set<Integer> result3 = Maps.getOrEmptySetIfAbsent(map, "missing");   // returns immutable empty set
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <E> the type of elements in the set.
     * @param map the map from which to retrieve the value; may be {@code null} or empty.
     * @param key the key whose associated value is to be returned.
     * @return the Set value mapped by the key, or an empty <b>immutable</b> Set if the key is absent.
     * @see N#emptySet()
     * @see #getOrPutSetIfAbsent(Map, Object)
     */
    public static <K, E> Set<E> getOrEmptySetIfAbsent(final Map<K, ? extends Set<E>> map, final K key) {
        if (N.isEmpty(map)) {
            return N.emptySet();
        }

        final Set<E> val = map.get(key);

        if (val == null) {
            return N.emptySet();
        }

        return val;
    }

    /**
     * Returns the Map value to which the specified key is mapped, or an empty <b>immutable</b> Map if the key is absent.
     * A key is considered absent if the map is empty, contains no mapping for the key, or the mapped value is {@code null}.
     *
     * <p><b>Important:</b> The empty Map returned when the key is absent is <em>immutable</em> (via {@link N#emptyMap()}).
     * Any attempt to modify it (e.g.&nbsp;{@code put}, {@code remove}) will throw {@link UnsupportedOperationException}.
     * If you need a mutable map, use {@link #getOrPutMapIfAbsent(Map, Object)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Map<String, Integer>> map = new HashMap<>();
     * Map<String, Integer> innerMap = new HashMap<>();
     * innerMap.put("a", 1);
     * innerMap.put("b", 2);
     * map.put("data", innerMap);
     * map.put("empty", null);
     *
     * Map<String, Integer> result1 = Maps.getOrEmptyMapIfAbsent(map, "data");      // returns {a=1, b=2}
     * Map<String, Integer> result2 = Maps.getOrEmptyMapIfAbsent(map, "empty");     // returns immutable empty map
     * Map<String, Integer> result3 = Maps.getOrEmptyMapIfAbsent(map, "missing");   // returns immutable empty map
     * }</pre>
     *
     * @param <K> the type of keys maintained by the outer map.
     * @param <KK> the type of keys maintained by the inner map.
     * @param <VV> the type of values maintained by the inner map.
     * @param map the map from which to retrieve the value; may be {@code null} or empty.
     * @param key the key whose associated value is to be returned.
     * @return the Map value mapped by the key, or an empty <b>immutable</b> Map if the key is absent.
     * @see N#emptyMap()
     * @see #getOrPutMapIfAbsent(Map, Object)
     */
    public static <K, KK, VV> Map<KK, VV> getOrEmptyMapIfAbsent(final Map<K, ? extends Map<KK, VV>> map, final K key) {
        if (N.isEmpty(map)) {
            return N.emptyMap();
        }

        final Map<KK, VV> val = map.get(key);

        if (val == null) {
            return N.emptyMap();
        }

        return val;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new value obtained from {@code defaultValueSupplier} and returns it.
     *
     * <p>Here, absent means the key is missing from the specified map or maps to {@code null}.
     * The supplier is invoked only when the key is absent, and exactly once.
     *
     * <p><b>Note:</b> The supplier must return a non-{@code null} value. If it returns {@code null},
     * an {@link IllegalArgumentException} is thrown and the map is left unchanged - consistent with
     * {@link #getOrDefaultIfAbsent(Map, Object, Supplier)}. This guarantees that both the value
     * stored in the map and the value returned are never {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<String>> map = new HashMap<>();
     *
     * List<String> list1 = Maps.getOrPutIfAbsent(map, "key1", () -> new ArrayList<>());
     * list1.add("value1");
     * // map now contains: {"key1"=["value1"]}
     *
     * // list2 is the same instance as list1, supplier not called
     * List<String> list2 = Maps.getOrPutIfAbsent(map, "key1", () -> new ArrayList<>());
     *
     * map.put("key2", null);
     * List<String> list3 = Maps.getOrPutIfAbsent(map, "key2", () -> new ArrayList<>());
     * // New list created because value was null
     * }</pre>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param map the map to check and possibly update; must not be {@code null}.
     * @param key the key to check for, may be {@code null}.
     * @param defaultValueSupplier the supplier to provide a value when the key is absent; must not be
     *        {@code null} and must not return {@code null}.
     * @return the existing non-{@code null} value associated with the specified key, or the
     *         non-{@code null} value newly created by {@code defaultValueSupplier} (now stored in the
     *         map) if the key was absent.
     * @throws IllegalArgumentException if {@code map} or {@code defaultValueSupplier} is {@code null}, or if
     *         {@code defaultValueSupplier} returns {@code null}.
     */
    // @ai-ignore getOrPut*IfAbsent variants - get-or-create pattern that inserts a new collection (List/Set/LinkedHashSet/Map/LinkedHashMap) when key is absent. Do not suggest consolidation.
    public static <K, V> V getOrPutIfAbsent(final Map<K, V> map, final K key, final Supplier<? extends V> defaultValueSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);
        N.checkArgNotNull(defaultValueSupplier, cs.defaultValueSupplier);

        V val = map.get(key);

        if (val == null) {
            val = N.checkArgNotNull(defaultValueSupplier.get(), "defaultValueSupplier returned null");
            map.put(key, val);
        }

        return val;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code List} and returns it.
     *
     * <p>Here, absent means the key is missing from the specified map or maps to {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<Integer>> map = new HashMap<>();
     *
     * List<Integer> list1 = Maps.getOrPutListIfAbsent(map, "numbers");
     * list1.add(1);
     * list1.add(2);
     * // map now contains: {"numbers"=[1, 2]}
     *
     * List<Integer> list2 = Maps.getOrPutListIfAbsent(map, "numbers");
     * // list2 is the same instance as list1
     * // list2 contains [1, 2]
     * }</pre>
     *
     * @param <K> the key type.
     * @param <E> the element type of the list.
     * @param map the map to check and possibly update; must not be {@code null}.
     * @param key the key to check for, may be {@code null}.
     * @return the value associated with the specified key, or a new {@code ArrayList} if the key is absent.
     * @throws IllegalArgumentException if {@code map} is {@code null}.
     */
    public static <K, E> List<E> getOrPutListIfAbsent(final Map<K, List<E>> map, final K key) throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);

        List<E> v = map.get(key);

        if (v == null) {
            v = new ArrayList<>();
            map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code Set} and returns it.
     *
     * <p>Here, absent means the key is missing from the specified map or maps to {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Set<String>> map = new HashMap<>();
     *
     * Set<String> set1 = Maps.getOrPutSetIfAbsent(map, "tags");
     * set1.add("java");
     * set1.add("spring");
     * // map now contains: {"tags"=["java", "spring"]}
     *
     * Set<String> set2 = Maps.getOrPutSetIfAbsent(map, "tags");
     * // set2 is the same instance as set1
     * }</pre>
     *
     * @param <K> the key type.
     * @param <E> the element type of the set.
     * @param map the map to check and possibly update; must not be {@code null}.
     * @param key the key to check for, may be {@code null}.
     * @return the value associated with the specified key, or a new {@code HashSet} if the key is absent.
     * @throws IllegalArgumentException if {@code map} is {@code null}.
     */
    public static <K, E> Set<E> getOrPutSetIfAbsent(final Map<K, Set<E>> map, final K key) throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);

        Set<E> v = map.get(key);

        if (v == null) {
            v = new HashSet<>();
            map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code LinkedHashSet} and returns it.
     *
     * <p>Here, absent means the key is missing from the specified map or maps to {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Set<String>> map = new HashMap<>();
     *
     * Set<String> set = Maps.getOrPutLinkedHashSetIfAbsent(map, "orderedTags");
     * set.add("first");
     * set.add("second");
     * set.add("third");
     * // map now contains: {"orderedTags"=["first", "second", "third"]} (order preserved)
     * }</pre>
     *
     * @param <K> the key type.
     * @param <E> the element type of the set.
     * @param map the map to check and possibly update; must not be {@code null}.
     * @param key the key to check for, may be {@code null}.
     * @return the value associated with the specified key, or a new {@code LinkedHashSet} if the key is absent.
     * @throws IllegalArgumentException if {@code map} is {@code null}.
     */
    public static <K, E> Set<E> getOrPutLinkedHashSetIfAbsent(final Map<K, Set<E>> map, final K key) throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);

        Set<E> v = map.get(key);

        if (v == null) {
            v = new LinkedHashSet<>();
            map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code Map} and returns it.
     *
     * <p>Here, absent means the key is missing from the specified map or maps to {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Map<String, Integer>> map = new HashMap<>();
     *
     * Map<String, Integer> innerMap = Maps.getOrPutMapIfAbsent(map, "scores");
     * innerMap.put("math", 95);
     * innerMap.put("english", 88);
     * // map now contains: {"scores"={"math"=95, "english"=88}}
     *
     * Map<String, Integer> sameMap = Maps.getOrPutMapIfAbsent(map, "scores");
     * // sameMap is the same instance as innerMap
     * }</pre>
     *
     * @param <K> the key type.
     * @param <KK> the key type of the value map.
     * @param <VV> the value type of the value map.
     * @param map the map to check and possibly update; must not be {@code null}.
     * @param key the key to check for, may be {@code null}.
     * @return the value associated with the specified key, or a new {@code HashMap} if the key is absent.
     * @throws IllegalArgumentException if {@code map} is {@code null}.
     */
    public static <K, KK, VV> Map<KK, VV> getOrPutMapIfAbsent(final Map<K, Map<KK, VV>> map, final K key) throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);

        Map<KK, VV> v = map.get(key);

        if (v == null) {
            v = new HashMap<>();
            map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the value associated with the specified {@code key} if it exists and is not {@code null} in the specified {@code map},
     * otherwise puts a new {@code LinkedHashMap} and returns it.
     *
     * <p>Here, absent means the key is missing from the specified map or maps to {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Map<String, String>> map = new HashMap<>();
     *
     * Map<String, String> innerMap = Maps.getOrPutLinkedHashMapIfAbsent(map, "config");
     * innerMap.put("first", "1");
     * innerMap.put("second", "2");
     * innerMap.put("third", "3");
     * // map now contains: {"config"={"first"="1", "second"="2", "third"="3"}} (order preserved)
     * }</pre>
     *
     * @param <K> the key type.
     * @param <KK> the key type of the value map.
     * @param <VV> the value type of the value map.
     * @param map the map to check and possibly update; must not be {@code null}.
     * @param key the key to check for, may be {@code null}.
     * @return the value associated with the specified key, or a new {@code LinkedHashMap} if the key is absent.
     * @throws IllegalArgumentException if {@code map} is {@code null}.
     */
    public static <K, KK, VV> Map<KK, VV> getOrPutLinkedHashMapIfAbsent(final Map<K, Map<KK, VV>> map, final K key) throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);

        Map<KK, VV> v = map.get(key);

        if (v == null) {
            v = new LinkedHashMap<>();
            map.put(key, v);
        }

        return v;
    }

    /**
     * Returns the mapped boolean value wrapped in {@code OptionalBoolean}.
     * Returns {@code OptionalBoolean.empty()} if the map is {@code null}/empty, the key is absent,
     * or the mapped value is {@code null}. Non-{@code null} values that are not {@link Boolean}
     * are converted with {@link N#convert(Object, Class)}, the same conversion used by
     * {@link #getAs(Map, Object, Class)}: a {@link Number} is {@code true} when it is non-zero, and
     * anything else is read from its string form.
     *
     * <p><b>Exactly which strings are {@code true}:</b> only {@code "true"} (any case), the single
     * character {@code "Y"} or {@code "y"}, and {@code "1"}, each optionally surrounded by padding
     * (Unicode whitespace or any character {@code <= ' '}), which is stripped before the comparison;
     * interior padding is not stripped, so {@code "tr ue"} is {@code false}. <em>Every</em> other string
     * is {@code false} - including spellings a reader may expect to be true, such as {@code "T"},
     * {@code "yes"} and {@code "on"}, and including {@code ""}, a string consisting only of padding, and
     * the string form of an arbitrary object.</p>
     *
     * <p><b>A {@link Character} value is treated as a one-character string</b>, so it follows exactly the rule
     * above: {@code 'Y'}, {@code 'y'} and {@code '1'} are {@code true} and every other character is
     * {@code false}.</p>
     *
     * <p><b>Note:</b> unlike the numeric accessors, this method never throws for an unconvertible value,
     * and there is no accessor in this class that can tell "present but not a boolean" from
     * "present and false": {@link #getAs(Map, Object, Class)} with {@code Boolean.class} performs the
     * very same conversion and answers {@code Optional.of(false)} for {@code "junk"} just as this method
     * answers {@code OptionalBoolean.of(false)}. To make that distinction, read the raw value with
     * {@link #getIfExists(Map, Object)} and test its type yourself.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("flag1", true);
     * map.put("flag2", "false");
     * map.put("flag3", null);
     *
     * OptionalBoolean result1 = Maps.getAsBoolean(map, "flag1");   // returns OptionalBoolean.of(true)
     * OptionalBoolean result2 = Maps.getAsBoolean(map, "flag2");   // returns OptionalBoolean.of(false)
     * OptionalBoolean result3 = Maps.getAsBoolean(map, "flag3");   // returns OptionalBoolean.empty()
     * OptionalBoolean result4 = Maps.getAsBoolean(map, "flag4");   // returns OptionalBoolean.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code OptionalBoolean} containing the boolean value, or empty if the map is
     *         {@code null}/empty, the key is absent, or the value is {@code null}.
     */
    // @ai-ignore getAs* type-conversion variants - retrieve map values with automatic type conversion for each primitive type (boolean/char/byte/short/int/long/float/double/String) plus generic typed access. Each pair provides Optional and OrDefaultIfAbsent forms. Do not suggest consolidation.
    public static <K> OptionalBoolean getAsBoolean(final Map<K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return OptionalBoolean.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalBoolean.empty();
        } else if (val instanceof Boolean) {
            return OptionalBoolean.of((Boolean) val);
        } else {
            // Route non-Boolean values through the same conversion as getAs(map, key, Boolean.class).
            // Boolean.parseBoolean answers false for 1, "1" and 'Y', which contradicted the typed accessor
            // for the very values (numeric/String flags) this method exists to read.
            return OptionalBoolean.of(N.convert(val, Boolean.class));
        }
    }

    /**
     * Returns the mapped boolean value, or {@code defaultValue} if the map is {@code null}/empty,
     * the key is absent, or the mapped value is {@code null}.
     * Non-{@code null} values that are not {@link Boolean} are converted with {@link N#convert(Object, Class)},
     * the same conversion used by {@link #getAs(Map, Object, Class)}: a {@link Number} is {@code true} when it
     * is non-zero, and a {@link String} is parsed leniently.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("enabled", true);
     * map.put("disabled", "false");
     *
     * boolean result1 = Maps.getAsBooleanOrDefaultIfAbsent(map, "enabled", false);   // returns true
     * boolean result2 = Maps.getAsBooleanOrDefaultIfAbsent(map, "disabled", true);   // returns false
     * boolean result3 = Maps.getAsBooleanOrDefaultIfAbsent(map, "missing", true);    // returns true (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the value to return if the map is {@code null}/empty, the key is absent, or the value is {@code null}
     * @return the mapped boolean value, or {@code defaultValue} if the key is absent or mapped to {@code null}
     */
    public static <K> boolean getAsBooleanOrDefaultIfAbsent(final Map<K, ?> map, final K key, final boolean defaultValue) {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (val instanceof Boolean) {
            return (Boolean) val;
        } else {
            // See getAsBoolean(Map, Object): same conversion as getAs(map, key, Boolean.class).
            return N.convert(val, Boolean.class);
        }
    }

    /**
     * Returns the mapped character value wrapped in {@code OptionalChar}.
     * Returns {@code OptionalChar.empty()} if the map is {@code null}/empty, the key is absent,
     * or the mapped value is {@code null}.
     *
     * <p>A non-{@code null} value that is not a {@link Character} is converted by kind:</p>
     * <ul>
     *   <li>a {@link Number} is its UTF-16 code unit; a fractional value is first truncated toward zero,
     *       matching {@link #getAsInt(Map, Object)} and the other numeric accessors;</li>
     *   <li>anything else is converted from its string form: an empty string yields the null character
     *       {@code U+0000}, a single-character string yields that character (so {@code " "} is a space,
     *       not a parse failure), and a longer string is parsed as a numeric UTF-16 code-unit value with
     *       {@link Numbers#toLong(String)} - the same grammar the other {@code getAs*} accessors use, so
     *       {@code "0x41"} reads as {@code 'A'}.</li>
     * </ul>
     *
     * <p><b>Note:</b> the {@link Number} rule is why {@code 65} reads as {@code 'A'} rather than as the
     * digit {@code '6'} - a number names a code unit, not the text of one. The empty-string rule is a
     * deliberate exception to this class's "empty text is absent" convention, which the numeric accessors
     * follow.</p>
     *
     * <p><b>Deliberately more lenient than {@code getAs(map, key, Character.class)}</b>, which converts with
     * {@link N#convert(Object, Class)} and accepts only a single character or a plain decimal code unit. This
     * method additionally accepts any grammar {@link Numbers#toLong(String)} understands - {@code "0x41"} is
     * {@code 'A'} here and a {@link NumberFormatException} there - and truncates a fractional {@link Number},
     * so {@code 65.9} is {@code 'A'} here and throws there. The leniency runs the other way for an empty
     * string: {@code ""} is {@code U+0000} here but {@link Optional#empty()} there.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("letter", 'A');
     * map.put("digit", "5");
     * map.put("empty", null);
     *
     * OptionalChar result1 = Maps.getAsChar(map, "letter");    // returns OptionalChar.of('A')
     * OptionalChar result2 = Maps.getAsChar(map, "digit");     // returns OptionalChar.of('5')
     * OptionalChar result3 = Maps.getAsChar(map, "empty");     // returns OptionalChar.empty()
     * OptionalChar result4 = Maps.getAsChar(map, "missing");   // returns OptionalChar.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code OptionalChar} containing the character value, or empty if the map is
     *         {@code null}/empty, the key is absent, or the value is {@code null}.
     * @throws IllegalArgumentException if the value resolves to a code-unit value outside {@code [0, 65535]} -
     *         for a {@code Number} and for a multi-character {@code String} alike.
     * @throws NumberFormatException if the value is a multi-character {@code String} that cannot be parsed as a
     *         number at all. Note that a fractional spelling such as {@code "65.9"} is rejected here, while the
     *         {@code Number} {@code 65.9} is truncated - the same split the integral accessors have. The
     *         message names the value and this accessor's own contract; the parser's own message is kept as
     *         the {@linkplain Throwable#getCause() cause}.
     * @throws ArithmeticException if the value has no exact {@code long} value at all ({@code NaN}, an infinity,
     *         or a magnitude beyond {@code long}), whether it arrives as a {@code Number} or as a
     *         multi-character {@code String}.
     */
    public static <K> OptionalChar getAsChar(final Map<K, ?> map, final K key) throws IllegalArgumentException, NumberFormatException, ArithmeticException {
        if (N.isEmpty(map)) {
            return OptionalChar.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return OptionalChar.empty();
        } else if (val instanceof Character) {
            return OptionalChar.of(((Character) val));
        } else if (val instanceof Number) {
            // A Number names the character's UTF-16 code unit, not the text of one. Routing numbers through
            // N.toString() made the mapping discontinuous, because parseChar() branches on string length:
            // 5 -> '5' (U+0035, since "5" is one char) and 9 -> '9', but 10 -> U+000A and 65 -> 'A'. It also
            // left 65.0 unparseable while 65 worked.
            return OptionalChar.of(toCodeUnit((Number) val));
        } else {
            return OptionalChar.of(parseChar(N.toString(val)));
        }
    }

    /**
     * Returns the mapped character value, or {@code defaultValue} if the map is {@code null}/empty,
     * the key is absent, or the mapped value is {@code null}.
     *
     * <p>A non-{@code null} value that is not a {@link Character} is converted exactly as described by
     * {@link #getAsChar(Map, Object)}: a {@link Number} is its UTF-16 code unit (a fractional value first
     * truncated toward zero), and anything else is converted from its string form.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("grade", 'A');
     * map.put("initial", "J");
     *
     * char result1 = Maps.getAsCharOrDefaultIfAbsent(map, "grade", 'F');     // returns 'A'
     * char result2 = Maps.getAsCharOrDefaultIfAbsent(map, "initial", 'X');   // returns 'J'
     * char result3 = Maps.getAsCharOrDefaultIfAbsent(map, "missing", 'N');   // returns 'N' (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the value to return if the map is {@code null}/empty, the key is absent, or the value is {@code null}
     * @return the mapped character value, or {@code defaultValue} if the key is absent or mapped to {@code null}
     * @throws IllegalArgumentException if the value resolves to a code-unit value outside {@code [0, 65535]} -
     *         for a {@code Number} and for a multi-character {@code String} alike.
     * @throws NumberFormatException if the value is a multi-character {@code String} that cannot be parsed as a
     *         number at all. Note that a fractional spelling such as {@code "65.9"} is rejected here, while the
     *         {@code Number} {@code 65.9} is truncated - the same split the integral accessors have. The
     *         message names the value and this accessor's own contract; the parser's own message is kept as
     *         the {@linkplain Throwable#getCause() cause}.
     * @throws ArithmeticException if the value has no exact {@code long} value at all ({@code NaN}, an infinity,
     *         or a magnitude beyond {@code long}), whether it arrives as a {@code Number} or as a
     *         multi-character {@code String}.
     */
    public static <K> char getAsCharOrDefaultIfAbsent(final Map<K, ?> map, final K key, final char defaultValue)
            throws IllegalArgumentException, NumberFormatException, ArithmeticException {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (val instanceof Character) {
            return (Character) val;
        } else if (val instanceof Number) {
            // See getAsChar(Map, Object): a Number is a code unit, not the text of one.
            return toCodeUnit((Number) val);
        } else {
            return parseChar(N.toString(val));
        }
    }

    /**
     * Parses {@code str} as a {@code char}: empty/{@code null} yields {@code '\0'}; a single-character
     * string yields that character; a longer string is parsed as a numeric UTF-16 code-unit value.
     */
    private static char parseChar(final String str) {
        if (Strings.isEmpty(str)) {
            return 0;
        } else if (str.length() == 1) {
            return str.charAt(0);
        } else {
            // Numbers.toLong, not Integer.parseInt: every other getAs* accessor in this class parses through
            // the Numbers family, so Integer.parseInt left this one method with its own, narrower grammar
            // ("0x41" threw here while Numbers.toInt("0x41") is 65) and made it report an out-of-range value
            // as a NumberFormatException, while the very same value arriving as a Number produced an
            // IllegalArgumentException from toCodeUnit. One parser now serves both paths.
            try {
                return toCodeUnit(Numbers.toLong(str));
            } catch (final NumberFormatException e) {
                // Numbers.toLong names `long` in its message, which is an implementation detail of this
                // accessor: a caller of getAsChar asked about a char and was told "is not a valid Long".
                // Re-state the failure in the accessor's own terms, keeping the original as the cause.
                final NumberFormatException nfe = new NumberFormatException("'" + str + "' is neither a single character nor a numeric UTF-16 code unit");
                nfe.initCause(e);
                throw nfe;
            }
        }
    }

    /**
     * Converts a {@code Number} to the {@code char} with that UTF-16 code unit, truncating a fractional
     * value toward zero the way {@link Numbers#toInt(Object)} does and rejecting a value outside
     * {@code [0, 65535]}.
     */
    private static char toCodeUnit(final Number num) {
        return toCodeUnit(Numbers.toLong(num));
    }

    /**
     * Narrows an already-parsed code-unit value to {@code char}, rejecting anything outside
     * {@code [0, 65535]}. Shared by the {@code Number} and the multi-character {@code String} paths of
     * {@link #getAsChar(Map, Object)} so both reject an out-of-range value the same way.
     * @throws IllegalArgumentException if {@code value} is outside {@code [0, 65535]}
     */
    private static char toCodeUnit(final long value) throws IllegalArgumentException {
        if (value < Character.MIN_VALUE || value > Character.MAX_VALUE) {
            throw new IllegalArgumentException("Integer value out of char range: " + value);
        }

        return (char) value;
    }

    /**
     * Returns the mapped byte value wrapped in {@code OptionalByte}.
     * Returns {@code OptionalByte.empty()} if the map is {@code null}/empty, the key is absent,
     * the mapped value is {@code null}, or the mapped value is an empty {@link CharSequence}. Non-{@code null} values - {@link Number} and {@link String}
     * alike - are converted with {@link Numbers#toByte(Object)}, and a value outside the target range is
     * rejected rather than silently narrowed. A fractional {@code Number} is truncated toward zero; a
     * fractional {@code String} such as {@code "65.9"} is <em>not</em> - it is rejected with a
     * {@link NumberFormatException}, because it does not spell an integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("small", (byte) 10);
     * map.put("medium", 127);
     * map.put("text", "25");
     *
     * OptionalByte result1 = Maps.getAsByte(map, "small");     // returns OptionalByte.of(10)
     * OptionalByte result2 = Maps.getAsByte(map, "medium");    // returns OptionalByte.of(127)
     * OptionalByte result3 = Maps.getAsByte(map, "text");      // returns OptionalByte.of(25)
     * OptionalByte result4 = Maps.getAsByte(map, "missing");   // returns OptionalByte.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code OptionalByte} containing the byte value, or empty if the map is
     *         {@code null}/empty, the key is absent, the value is {@code null}, or the value is an empty
     *         {@code CharSequence}.
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     * @throws ArithmeticException if the value is a {@code Number}, or a numeric {@code String}, whose value lies outside the {@code byte} range.
     */
    public static <K> OptionalByte getAsByte(final Map<K, ?> map, final K key) throws NumberFormatException, ArithmeticException {
        if (N.isEmpty(map)) {
            return OptionalByte.empty();
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return OptionalByte.empty();
        } else {
            // One range-checked conversion for both Number and String values. The Number branch used
            // Number.byteValue(), which narrows out-of-range values silently, while the String branch
            // already range-checked - so the same logical value behaved differently by storage type.
            return OptionalByte.of(Numbers.toByte(val));
        }
    }

    /**
     * Returns the mapped byte value, or {@code defaultValue} if the map is {@code null}/empty,
     * the key is absent, the mapped value is {@code null}, or the mapped value is an empty
     * {@link CharSequence}.
     * Non-{@code null} values - {@link Number} and {@link String} alike - are converted with
     * {@link Numbers#toByte(Object)}, and a value outside the target range is rejected rather than
     * silently narrowed. A fractional {@code Number} is truncated toward zero; a fractional
     * {@code String} such as {@code "65.9"} is <em>not</em> - it is rejected with a
     * {@link NumberFormatException}, because it does not spell an integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("id", (byte) 5);
     * map.put("count", "10");
     *
     * byte result1 = Maps.getAsByteOrDefaultIfAbsent(map, "id", (byte) 0);         // returns 5
     * byte result2 = Maps.getAsByteOrDefaultIfAbsent(map, "count", (byte) 0);      // returns 10
     * byte result3 = Maps.getAsByteOrDefaultIfAbsent(map, "missing", (byte) -1);   // returns -1 (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the value to return if the map is {@code null}/empty, the key is absent, the value is
     *        {@code null}, or the value is an empty {@code CharSequence}
     * @return the mapped byte value, or {@code defaultValue} if the key is absent, mapped to {@code null},
     *         or mapped to an empty {@code CharSequence}
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     * @throws ArithmeticException if the value is a {@code Number}, or a numeric {@code String}, whose value lies outside the {@code byte} range.
     */
    public static <K> byte getAsByteOrDefaultIfAbsent(final Map<K, ?> map, final K key, final byte defaultValue)
            throws NumberFormatException, ArithmeticException {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return defaultValue;
        } else {
            // See getAsByte(Map, Object): range-checked for Number values too.
            return Numbers.toByte(val);
        }
    }

    /**
     * Returns the mapped short value wrapped in {@code OptionalShort}.
     * Returns {@code OptionalShort.empty()} if the map is {@code null}/empty, the key is absent,
     * the mapped value is {@code null}, or the mapped value is an empty {@link CharSequence}. Non-{@code null} values - {@link Number} and {@link String}
     * alike - are converted with {@link Numbers#toShort(Object)}, and a value outside the target range is
     * rejected rather than silently narrowed. A fractional {@code Number} is truncated toward zero; a
     * fractional {@code String} such as {@code "65.9"} is <em>not</em> - it is rejected with a
     * {@link NumberFormatException}, because it does not spell an integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("year", (short) 2023);
     * map.put("count", 1000);
     * map.put("text", "500");
     *
     * OptionalShort result1 = Maps.getAsShort(map, "year");      // returns OptionalShort.of(2023)
     * OptionalShort result2 = Maps.getAsShort(map, "count");     // returns OptionalShort.of(1000)
     * OptionalShort result3 = Maps.getAsShort(map, "text");      // returns OptionalShort.of(500)
     * OptionalShort result4 = Maps.getAsShort(map, "missing");   // returns OptionalShort.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code OptionalShort} containing the short value, or empty if the map is
     *         {@code null}/empty, the key is absent, the value is {@code null}, or the value is an empty
     *         {@code CharSequence}.
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     * @throws ArithmeticException if the value is a {@code Number}, or a numeric {@code String}, whose value lies outside the {@code short} range.
     */
    public static <K> OptionalShort getAsShort(final Map<K, ?> map, final K key) throws NumberFormatException, ArithmeticException {
        if (N.isEmpty(map)) {
            return OptionalShort.empty();
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return OptionalShort.empty();
        } else {
            // One range-checked conversion for both Number and String values. The Number branch used
            // Number.shortValue(), which narrows out-of-range values silently, while the String branch
            // already range-checked - so the same logical value behaved differently by storage type.
            return OptionalShort.of(Numbers.toShort(val));
        }
    }

    /**
     * Returns the mapped short value, or {@code defaultValue} if the map is {@code null}/empty,
     * the key is absent, the mapped value is {@code null}, or the mapped value is an empty
     * {@link CharSequence}.
     * Non-{@code null} values - {@link Number} and {@link String} alike - are converted with
     * {@link Numbers#toShort(Object)}, and a value outside the target range is rejected rather than
     * silently narrowed. A fractional {@code Number} is truncated toward zero; a fractional
     * {@code String} such as {@code "65.9"} is <em>not</em> - it is rejected with a
     * {@link NumberFormatException}, because it does not spell an integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("port", (short) 8080);
     * map.put("timeout", "3000");
     *
     * short result1 = Maps.getAsShortOrDefaultIfAbsent(map, "port", (short) 80);      // returns 8080
     * short result2 = Maps.getAsShortOrDefaultIfAbsent(map, "timeout", (short) 0);    // returns 3000
     * short result3 = Maps.getAsShortOrDefaultIfAbsent(map, "missing", (short) -1);   // returns -1 (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the value to return if the map is {@code null}/empty, the key is absent, the value is
     *        {@code null}, or the value is an empty {@code CharSequence}
     * @return the mapped short value, or {@code defaultValue} if the key is absent, mapped to {@code null},
     *         or mapped to an empty {@code CharSequence}
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     * @throws ArithmeticException if the value is a {@code Number}, or a numeric {@code String}, whose value lies outside the {@code short} range.
     */
    public static <K> short getAsShortOrDefaultIfAbsent(final Map<K, ?> map, final K key, final short defaultValue)
            throws NumberFormatException, ArithmeticException {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return defaultValue;
        } else {
            // See getAsShort(Map, Object): range-checked for Number values too.
            return Numbers.toShort(val);
        }
    }

    /**
     * Returns the mapped integer value wrapped in {@code OptionalInt}.
     * Returns {@code OptionalInt.empty()} if the map is {@code null}/empty, the key is absent,
     * the mapped value is {@code null}, or the mapped value is an empty {@link CharSequence}. Non-{@code null} values - {@link Number} and {@link String}
     * alike - are converted with {@link Numbers#toInt(Object)}, and a value outside the target range is
     * rejected rather than silently narrowed. A fractional {@code Number} is truncated toward zero; a
     * fractional {@code String} such as {@code "65.9"} is <em>not</em> - it is rejected with a
     * {@link NumberFormatException}, because it does not spell an integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", 42);
     * map.put("total", "100");
     * map.put("null", null);
     *
     * OptionalInt result1 = Maps.getAsInt(map, "count");     // returns OptionalInt.of(42)
     * OptionalInt result2 = Maps.getAsInt(map, "total");     // returns OptionalInt.of(100)
     * OptionalInt result3 = Maps.getAsInt(map, "null");      // returns OptionalInt.empty()
     * OptionalInt result4 = Maps.getAsInt(map, "missing");   // returns OptionalInt.empty()
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code OptionalInt} containing the integer value, or empty if the map is
     *         {@code null}/empty, the key is absent, the value is {@code null}, or the value is an empty
     *         {@code CharSequence}.
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     * @throws ArithmeticException if the value is a {@code Number}, or a numeric {@code String}, whose value lies outside the {@code int} range.
     */
    public static <K> OptionalInt getAsInt(final Map<K, ?> map, final K key) throws NumberFormatException, ArithmeticException {
        if (N.isEmpty(map)) {
            return OptionalInt.empty();
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return OptionalInt.empty();
        } else {
            // One range-checked conversion for both Number and String values. The Number branch used
            // Number.intValue(), which narrows out-of-range values silently, while the String branch
            // already range-checked - so the same logical value behaved differently by storage type.
            return OptionalInt.of(Numbers.toInt(val));
        }
    }

    /**
     * Returns the mapped integer value, or {@code defaultValue} if the map is {@code null}/empty,
     * the key is absent, the mapped value is {@code null}, or the mapped value is an empty
     * {@link CharSequence}.
     * Non-{@code null} values - {@link Number} and {@link String} alike - are converted with
     * {@link Numbers#toInt(Object)}, and a value outside the target range is rejected rather than
     * silently narrowed. A fractional {@code Number} is truncated toward zero; a fractional
     * {@code String} such as {@code "65.9"} is <em>not</em> - it is rejected with a
     * {@link NumberFormatException}, because it does not spell an integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("age", 25);
     * map.put("score", "98");
     *
     * int result1 = Maps.getAsIntOrDefaultIfAbsent(map, "age", 0);        // returns 25
     * int result2 = Maps.getAsIntOrDefaultIfAbsent(map, "score", 0);      // returns 98
     * int result3 = Maps.getAsIntOrDefaultIfAbsent(map, "missing", -1);   // returns -1 (default)
     * }</pre>
     *
     * @param <K> the type of keys in the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the value to return if the map is {@code null}/empty, the key is absent, the value is
     *        {@code null}, or the value is an empty {@code CharSequence}
     * @return the mapped integer value, or {@code defaultValue} if the key is absent, mapped to {@code null},
     *         or mapped to an empty {@code CharSequence}
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     * @throws ArithmeticException if the value is a {@code Number}, or a numeric {@code String}, whose value lies outside the {@code int} range.
     */
    public static <K> int getAsIntOrDefaultIfAbsent(final Map<K, ?> map, final K key, final int defaultValue)
            throws NumberFormatException, ArithmeticException {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return defaultValue;
        } else {
            // See getAsInt(Map, Object): range-checked for Number values too.
            return Numbers.toInt(val);
        }
    }

    /**
     * Returns the mapped long value wrapped in {@code OptionalLong}.
     * Returns {@code OptionalLong.empty()} if the map is {@code null}/empty, the key is absent,
     * the mapped value is {@code null}, or the mapped value is an empty {@link CharSequence}. Non-{@code null} values - {@link Number} and {@link String}
     * alike - are converted with {@link Numbers#toLong(Object)}, and a value outside the target range is
     * rejected rather than silently narrowed. A fractional {@code Number} is truncated toward zero; a
     * fractional {@code String} such as {@code "65.9"} is <em>not</em> - it is rejected with a
     * {@link NumberFormatException}, because it does not spell an integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", 42L);
     * map.put("score", "100");
     *
     * // count.isPresent() = true, count.getAsLong() = 42
     * OptionalLong count = Maps.getAsLong(map, "count");
     *
     * // score.isPresent() = true, score.getAsLong() = 100 (converted from String)
     * OptionalLong score = Maps.getAsLong(map, "score");
     *
     * OptionalLong missing = Maps.getAsLong(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code OptionalLong} containing the long value, or empty if the map is
     *         {@code null}/empty, the key is absent, the value is {@code null}, or the value is an empty
     *         {@code CharSequence}.
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     * @throws ArithmeticException if the value is a {@code Number}, or a numeric {@code String}, whose value lies outside the {@code long} range.
     */
    public static <K> OptionalLong getAsLong(final Map<K, ?> map, final K key) throws NumberFormatException, ArithmeticException {
        if (N.isEmpty(map)) {
            return OptionalLong.empty();
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return OptionalLong.empty();
        } else {
            // One range-checked conversion for both Number and String values. The Number branch used
            // Number.longValue(), which narrows out-of-range values silently, while the String branch
            // already range-checked - so the same logical value behaved differently by storage type.
            return OptionalLong.of(Numbers.toLong(val));
        }
    }

    /**
     * Returns the mapped long value, or {@code defaultValue} if the map is {@code null}/empty,
     * the key is absent, the mapped value is {@code null}, or the mapped value is an empty
     * {@link CharSequence}.
     * Non-{@code null} values - {@link Number} and {@link String} alike - are converted with
     * {@link Numbers#toLong(Object)}, and a value outside the target range is rejected rather than
     * silently narrowed. A fractional {@code Number} is truncated toward zero; a fractional
     * {@code String} such as {@code "65.9"} is <em>not</em> - it is rejected with a
     * {@link NumberFormatException}, because it does not spell an integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", 42L);
     * map.put("score", "100");
     *
     * // count = 42
     * long count = Maps.getAsLongOrDefaultIfAbsent(map, "count", -1L);
     *
     * // score = 100 (converted from String)
     * long score = Maps.getAsLongOrDefaultIfAbsent(map, "score", -1L);
     *
     * long missing = Maps.getAsLongOrDefaultIfAbsent(map, "missing", -1L);
     * // missing = -1
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the value to return if the map is {@code null}/empty, the key is absent, the value is
     *        {@code null}, or the value is an empty {@code CharSequence}
     * @return the mapped long value, or {@code defaultValue} if the key is absent, mapped to {@code null},
     *         or mapped to an empty {@code CharSequence}
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     * @throws ArithmeticException if the value is a {@code Number}, or a numeric {@code String}, whose value lies outside the {@code long} range.
     */
    public static <K> long getAsLongOrDefaultIfAbsent(final Map<K, ?> map, final K key, final long defaultValue)
            throws NumberFormatException, ArithmeticException {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return defaultValue;
        } else {
            // See getAsLong(Map, Object): range-checked for Number values too.
            return Numbers.toLong(val);
        }
    }

    /**
     * Returns the mapped float value wrapped in {@code OptionalFloat}.
     * Returns {@code OptionalFloat.empty()} if the map is {@code null}/empty, the key is absent,
     * the mapped value is {@code null}, or the mapped value is an empty {@link CharSequence}. Non-{@code null} values are converted with
     * {@link Numbers#toFloat(Object)} when necessary.
     *
     * <p><b>No range check, unlike the integral accessors:</b> a magnitude beyond {@code float} saturates to
     * {@code Float.POSITIVE_INFINITY} or {@code Float.NEGATIVE_INFINITY}, and a {@code NaN} stays {@code NaN}.
     * This follows IEEE-754 and {@link Numbers#toFloat(Object)}, which never throws for an out-of-range
     * {@code Number}; {@link #getAsInt(Map, Object)} and the other integral accessors do throw.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("price", 19.99f);
     * map.put("discount", "0.15");
     *
     * // price.isPresent() = true, price.getAsFloat() = 19.99
     * OptionalFloat price = Maps.getAsFloat(map, "price");
     *
     * // discount.isPresent() = true, discount.getAsFloat() = 0.15 (converted from String)
     * OptionalFloat discount = Maps.getAsFloat(map, "discount");
     *
     * OptionalFloat missing = Maps.getAsFloat(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code OptionalFloat} containing the float value, or empty if the map is
     *         {@code null}/empty, the key is absent, the value is {@code null}, or the value is an empty
     *         {@code CharSequence}.
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     */
    public static <K> OptionalFloat getAsFloat(final Map<K, ?> map, final K key) throws NumberFormatException {
        if (N.isEmpty(map)) {
            return OptionalFloat.empty();
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return OptionalFloat.empty();
        } else {
            return OptionalFloat.of(Numbers.toFloat(val));
        }
    }

    /**
     * Returns the mapped float value, or {@code defaultValue} if the map is {@code null}/empty,
     * the key is absent, the mapped value is {@code null}, or the mapped value is an empty
     * {@link CharSequence}.
     * Non-{@code null} values are converted with {@link Numbers#toFloat(Object)} when necessary.
     *
     * <p><b>No range check, unlike the integral accessors:</b> a magnitude beyond {@code float} saturates to
     * {@code Float.POSITIVE_INFINITY} or {@code Float.NEGATIVE_INFINITY}, and a {@code NaN} stays {@code NaN}.
     * This follows IEEE-754 and {@link Numbers#toFloat(Object)}, which never throws for an out-of-range
     * {@code Number}; {@link #getAsInt(Map, Object)} and the other integral accessors do throw.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("price", 19.99f);
     * map.put("discount", "0.15");
     *
     * // price = 19.99
     * float price = Maps.getAsFloatOrDefaultIfAbsent(map, "price", 0.0f);
     *
     * // discount = 0.15 (converted from String)
     * float discount = Maps.getAsFloatOrDefaultIfAbsent(map, "discount", 0.0f);
     *
     * float missing = Maps.getAsFloatOrDefaultIfAbsent(map, "missing", 0.0f);
     * // missing = 0.0
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the value to return if the map is {@code null}/empty, the key is absent, the value is
     *        {@code null}, or the value is an empty {@code CharSequence}
     * @return the mapped float value, or {@code defaultValue} if the key is absent, mapped to {@code null},
     *         or mapped to an empty {@code CharSequence}
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     */
    public static <K> float getAsFloatOrDefaultIfAbsent(final Map<K, ?> map, final K key, final float defaultValue) throws NumberFormatException {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return defaultValue;
        } else {
            return Numbers.toFloat(val);
        }
    }

    /**
     * Returns the mapped double value wrapped in {@code OptionalDouble}.
     * Returns {@code OptionalDouble.empty()} if the map is {@code null}/empty, the key is absent,
     * the mapped value is {@code null}, or the mapped value is an empty {@link CharSequence}. Non-{@code null} values are converted with
     * {@link Numbers#toDouble(Object)} when necessary.
     *
     * <p><b>No range check, unlike the integral accessors:</b> a magnitude beyond {@code double} saturates to
     * {@code Double.POSITIVE_INFINITY} or {@code Double.NEGATIVE_INFINITY}, and a {@code NaN} stays {@code NaN}.
     * This follows IEEE-754 and {@link Numbers#toDouble(Object)}, which never throws for an out-of-range
     * {@code Number}; {@link #getAsInt(Map, Object)} and the other integral accessors do throw.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("temperature", 98.6);
     * map.put("pi", "3.14159");
     *
     * // temp.isPresent() = true, temp.getAsDouble() = 98.6
     * OptionalDouble temp = Maps.getAsDouble(map, "temperature");
     *
     * // pi.isPresent() = true, pi.getAsDouble() = 3.14159 (converted from String)
     * OptionalDouble pi = Maps.getAsDouble(map, "pi");
     *
     * OptionalDouble missing = Maps.getAsDouble(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code OptionalDouble} containing the double value, or empty if the map is
     *         {@code null}/empty, the key is absent, the value is {@code null}, or the value is an empty
     *         {@code CharSequence}.
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     */
    public static <K> OptionalDouble getAsDouble(final Map<K, ?> map, final K key) throws NumberFormatException {
        if (N.isEmpty(map)) {
            return OptionalDouble.empty();
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(Numbers.toDouble(val));
        }
    }

    /**
     * Returns the mapped double value, or {@code defaultValue} if the map is {@code null}/empty,
     * the key is absent, the mapped value is {@code null}, or the mapped value is an empty
     * {@link CharSequence}.
     * Non-{@code null} values are converted with {@link Numbers#toDouble(Object)} when necessary.
     *
     * <p><b>No range check, unlike the integral accessors:</b> a magnitude beyond {@code double} saturates to
     * {@code Double.POSITIVE_INFINITY} or {@code Double.NEGATIVE_INFINITY}, and a {@code NaN} stays {@code NaN}.
     * This follows IEEE-754 and {@link Numbers#toDouble(Object)}, which never throws for an out-of-range
     * {@code Number}; {@link #getAsInt(Map, Object)} and the other integral accessors do throw.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("temperature", 98.6);
     * map.put("pi", "3.14159");
     *
     * // temp = 98.6
     * double temp = Maps.getAsDoubleOrDefaultIfAbsent(map, "temperature", 0.0);
     *
     * // pi = 3.14159 (converted from String)
     * double pi = Maps.getAsDoubleOrDefaultIfAbsent(map, "pi", 0.0);
     *
     * double missing = Maps.getAsDoubleOrDefaultIfAbsent(map, "missing", 0.0);
     * // missing = 0.0
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the value to return if the map is {@code null}/empty, the key is absent, the value is
     *        {@code null}, or the value is an empty {@code CharSequence}
     * @return the mapped double value, or {@code defaultValue} if the key is absent, mapped to {@code null},
     *         or mapped to an empty {@code CharSequence}
     * @throws NumberFormatException if the value is a non-empty {@code String} that cannot be parsed as a number.
     *         Note that an empty {@code String} is treated as absent, whereas a blank one such as {@code " "} is malformed.
     */
    public static <K> double getAsDoubleOrDefaultIfAbsent(final Map<K, ?> map, final K key, final double defaultValue) throws NumberFormatException {
        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        // isEmptyText: "" carries no value, so it is absent rather than a parsed 0. See isEmptyText(Object).
        if (val == null || isEmptyText(val)) {
            return defaultValue;
        } else {
            return Numbers.toDouble(val);
        }
    }

    /**
     * Returns the mapped string value wrapped in {@code Optional<String>}.
     * Returns {@code Optional.empty()} if the map is {@code null}/empty, the key is absent, or the
     * mapped value is {@code null}. Non-{@code null} values that are not {@link String} are converted
     * with {@link N#stringOf(Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "John");
     * map.put("age", 25);
     *
     * // name.isPresent() = true, name.get() = "John"
     * Optional<String> name = Maps.getAsString(map, "name");
     *
     * // age.isPresent() = true, age.get() = "25" (converted from Integer)
     * Optional<String> age = Maps.getAsString(map, "age");
     *
     * Optional<String> missing = Maps.getAsString(map, "missing");
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @return an {@code Optional<String>} containing the mapped string value, converted if necessary;
     *         otherwise {@code Optional.empty()} if the map is {@code null}/empty, the key is absent,
     *         or the value is {@code null}.
     */
    public static <K> Optional<String> getAsString(final Map<K, ?> map, final K key) {
        if (N.isEmpty(map)) {
            return Optional.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return Optional.empty();
        } else if (val instanceof String) {
            return Optional.of((String) val);
        } else {
            return Optional.of(N.stringOf(val));
        }
    }

    /**
     * Returns the mapped string value, or {@code defaultValue} if the map is {@code null}/empty,
     * the key is absent, or the mapped value is {@code null}.
     * Non-{@code null} values that are not {@link String} are converted with {@link N#stringOf(Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "John");
     * map.put("age", 25);
     *
     * // name = "John"
     * String name = Maps.getAsStringOrDefaultIfAbsent(map, "name", "Unknown");
     *
     * // age = "25" (converted from Integer)
     * String age = Maps.getAsStringOrDefaultIfAbsent(map, "age", "Unknown");
     *
     * String missing = Maps.getAsStringOrDefaultIfAbsent(map, "missing", "Unknown");
     * // missing = "Unknown"
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the map is {@code null}/empty, the key is absent, or the value
     *        is {@code null}; must not be {@code null}.
     * @return the value mapped by the specified key (converted to {@code String} via {@code N.stringOf} if
     *         necessary), or {@code defaultValue} if the map is {@code null}/empty, the key is absent, or the value is {@code null}.
     * @throws IllegalArgumentException if {@code defaultValue} is {@code null}.
     */
    public static <K> String getAsStringOrDefaultIfAbsent(final Map<K, ?> map, final K key, final String defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(defaultValue, cs.defaultValue); // NOSONAR

        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (val instanceof String) {
            return (String) val;
        } else {
            return N.stringOf(val);
        }
    }

    /**
     * Returns the mapped value wrapped in {@code Optional<T>}, converting it to {@code targetType}
     * when necessary. Returns {@code Optional.empty()} if the map is {@code null}/empty, the key is
     * absent, the mapped value is {@code null}, or the conversion answers {@code null}.
     *
     * <p><b>A {@code null} conversion result is absent.</b> {@link N#convert(Object, Class)} answers
     * {@code null} when the value carries nothing convertible - notably for an empty {@code String} to any
     * numeric wrapper type - so that result is reported the same way a {@code null} value is, rather than
     * being wrapped. This keeps {@code getAs(map, key, Integer.class)} consistent with
     * {@link #getAsInt(Map, Object)} and with {@link #getByPathAs(Map, String, Class)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("date", "2023-12-25");
     * map.put("count", "100");
     *
     * // date.isPresent() = true, date.get() = LocalDate.of(2023, 12, 25)
     * Optional<LocalDate> date = Maps.getAs(map, "date", LocalDate.class);
     *
     * // count.isPresent() = true, count.get() = 100
     * Optional<Integer> count = Maps.getAs(map, "count", Integer.class);
     *
     * Optional<BigDecimal> missing = Maps.getAs(map, "missing", BigDecimal.class);
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <T> the type of the value.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param targetType the target type to which the value should be converted; must not be {@code null}.
     * @return an {@code Optional<T>} with the value mapped by the specified key (converted to {@code targetType}
     *         if necessary), or an empty {@code Optional<T>} if the map is {@code null}/empty, the key is absent,
     *         the value is {@code null}, or the conversion answers {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> Optional<T> getAs(final Map<K, ?> map, final K key, final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (N.isEmpty(map)) {
            return Optional.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return Optional.empty();
        } else if (targetType.isAssignableFrom(val.getClass())) {
            return Optional.of((T) val);
        } else {
            // ofNullable, not of: N.convert legitimately answers null when the value carries nothing
            // convertible ("" to any numeric wrapper, for one), and Optional.of(null) would raise a
            // message-less NPE out of an accessor whose whole point is to avoid one.
            return Optional.ofNullable((T) N.convert(val, targetType));
        }
    }

    /**
     * Returns the mapped value wrapped in {@code Optional<T>}, converting it to {@code targetType}
     * when necessary. Returns {@code Optional.empty()} if the map is {@code null}/empty, the key is
     * absent, the mapped value is {@code null}, or the conversion answers {@code null}.
     *
     * <p><b>A {@code null} conversion result is absent.</b> {@link N#convert(Object, Class)} answers
     * {@code null} when the value carries nothing convertible - notably for an empty {@code String} to any
     * numeric wrapper type - so that result is reported the same way a {@code null} value is, rather than
     * being wrapped. This keeps {@code getAs(map, key, Integer.class)} consistent with
     * {@link #getAsInt(Map, Object)} and with {@link #getByPathAs(Map, String, Class)}.</p>
     *
     * <p><b>A parameterized descriptor is honored only as far as {@link N#convert(Object, Type)} honors it.</b>
     * A stored value is returned as-is only when {@code targetType} carries no type arguments; anything else is
     * routed through {@code N.convert}, which normalizes the elements, keys and values of a collection, map or
     * array target. The generic internals of other parameterized handlers - {@code Optional<T>},
     * {@code Pair<K, V>}, a generic bean - remain opaque there, so a value of the right raw class is still
     * returned unconverted even when its type arguments differ.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("items", Arrays.asList("A", "B", "C"));
     *
     * // items.isPresent() = true, items.get() = ["A", "B", "C"]
     * Optional<List<String>> items = Maps.getAs(map, "items", new TypeReference<List<String>>() {}.type());
     *
     * Optional<Set<Integer>> missing = Maps.getAs(map, "missing", new TypeReference<Set<Integer>>() {}.type());
     * // missing.isPresent() = false
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <T> the type of the value.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param targetType the target type to which the value should be converted; must not be {@code null}.
     * @return an {@code Optional<T>} with the value mapped by the specified key (converted to {@code targetType}
     *         if necessary), or an empty {@code Optional<T>} if the map is {@code null}/empty, the key is absent,
     *         the value is {@code null}, or the conversion answers {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> Optional<T> getAs(final Map<K, ?> map, final K key, final Type<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (N.isEmpty(map)) {
            return Optional.empty();
        }

        final Object val = map.get(key);

        if (val == null) {
            return Optional.empty();
        } else if (targetType.parameterTypes().isEmpty() && targetType.javaType().isAssignableFrom(val.getClass())) {
            // The identity fast path is taken only for a descriptor that is no more informative than its
            // erasure: javaType() ERASES the Type's parameters, so a Type<List<String>> answers
            // isAssignableFrom(ArrayList.class) for a stored List<Integer> and the caller got that list back
            // unconverted - a ClassCastException at first read. Asking parameterTypes() rather than
            // enumerating collection/map/array keeps every other parameterized handler (Optional<T>,
            // Pair<K, V>, Timed<T>, a generic bean, ...) off this path too. N.convert keeps the same fast
            // path for a container whose element/key/value types are unconstrained, so nothing is lost by
            // routing a parameterized target through it.
            return Optional.of((T) val);
        } else {
            // See getAs(Map, Object, Class): a null conversion result is absent, not an NPE.
            return Optional.ofNullable((T) N.convert(val, targetType));
        }
    }

    /**
     * Returns the mapped value converted to {@code targetType}, or {@code defaultValue}
     * if the map is {@code null}/empty, the key is absent, or the mapped value is {@code null}.
     *
     * <p><b>Note:</b> Like {@link #getByPathAsOrDefaultIfAbsent(Map, String, Object, Class)} and the rest of the
     * {@code getAs*OrDefaultIfAbsent} family, a present {@code null} value is replaced by the default
     * (null-as-absent semantics). {@code defaultValue} is returned as-is and must not be {@code null}; it is
     * not used to determine the conversion target.</p>
     *
     * <p><b>Note:</b> A conversion that answers {@code null} - {@link N#convert(Object, Class)} does so for an
     * empty {@code String} to any numeric wrapper type - also falls back to {@code defaultValue}, so this
     * method never returns {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("count", "100");
     * map.put("active", "true");
     *
     * // count = 100 (converted from String)
     * Integer count = Maps.getAsOrDefaultIfAbsent(map, "count", 0, Integer.class);
     *
     * // active = true (converted from String)
     * Boolean active = Maps.getAsOrDefaultIfAbsent(map, "active", false, Boolean.class);
     *
     * Double missing = Maps.getAsOrDefaultIfAbsent(map, "missing", 0.0, Double.class);
     * // missing = 0.0
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <T> the type of the value.
     * @param map the map from which to retrieve the value.
     * @param key the key whose associated value is to be returned.
     * @param defaultValue the default value to return if the map is {@code null}/empty, the key is absent, or the value
     *        is {@code null}; returned as-is and must not be {@code null}.
     * @param targetType the class of the type to which a found, non-{@code null} value should be converted; must not be {@code null}.
     * @return the value to which the specified key is mapped (converted to {@code targetType} if necessary),
     *         or {@code defaultValue} if the map is {@code null}/empty, the key is absent, the value is
     *         {@code null}, or the conversion answers {@code null}. Never {@code null}.
     * @throws IllegalArgumentException if {@code defaultValue} or {@code targetType} is {@code null}.
     * @see #getOrDefaultIfAbsent(Map, Object, Object)
     * @see #getByPathAsOrDefaultIfAbsent(Map, String, Object, Class)
     * @see N#convert(Object, Class)
     * @see N#convert(Object, Type)
     */
    public static <K, T> T getAsOrDefaultIfAbsent(final Map<K, ?> map, final K key, final T defaultValue, final Class<? extends T> targetType)
            throws IllegalArgumentException {
        N.checkArgNotNull(defaultValue, cs.defaultValue); // NOSONAR
        N.checkArgNotNull(targetType, cs.targetType); // NOSONAR

        if (N.isEmpty(map)) {
            return defaultValue;
        }

        final Object val = map.get(key);

        if (val == null) {
            return defaultValue;
        } else if (targetType.isAssignableFrom(val.getClass())) {
            return (T) val;
        }

        // A null conversion result falls back to defaultValue rather than escaping as null, which would
        // break this method's non-null return guarantee. See getAs(Map, Object, Class).
        final T converted = N.convert(val, targetType);

        return converted == null ? defaultValue : converted;
    }

    /**
     * Returns the non-{@code null} values mapped by the specified keys.
     * Keys that are absent or mapped to {@code null} are skipped and do not contribute an element
     * to the returned list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", null);
     *
     * List<String> keys = Arrays.asList("a", "b", "c", "d");
     * List<Integer> values = Maps.getValuesIfPresent(map, keys);
     * // values = [1, 2] (null value for "c" and missing "d" are not included)
     * }</pre>
     *
     * @param <V> the value type.
     * @param map the map to read from; may be {@code null} or empty
     * @param keys the keys to read, in result order; may be {@code null} or empty
     * @return a list containing one value for each key that maps to a non-{@code null} value; returns
     *         an empty list if {@code map} or {@code keys} is {@code null} or empty
     */
    // @ai-ignore getValues* variants - batch retrieval of values by a collection of keys, with present-only or default-value semantics. Do not suggest consolidation.
    public static <V> List<V> getValuesIfPresent(final Map<?, ? extends V> map, final Collection<?> keys) {
        if (N.isEmpty(map) || N.isEmpty(keys)) {
            return new ArrayList<>();
        }

        final List<V> result = new ArrayList<>(keys.size());
        V val = null;

        for (final Object key : keys) {
            //noinspection SuspiciousMethodCalls
            val = map.get(key);

            if (val != null) {
                result.add(val);
            }
        }

        return result;
    }

    /**
     * Returns one value for each requested key, preserving the order of {@code keys}.
     * If a key is absent or maps to {@code null}, {@code defaultValue} is used for that position.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", null);
     *
     * List<String> keys = Arrays.asList("a", "b", "c", "d");
     * List<Integer> values = Maps.getValuesOrDefaultIfAbsent(map, keys, -1);
     * // values = [1, 2, -1, -1] ("c" has null value, "d" is missing)
     * }</pre>
     *
     * @param <V> the value type.
     * @param map the map to read from; may be {@code null} or empty
     * @param keys the keys to read, in result order; may be {@code null} or empty
     * @param defaultValue the default value to use when a key is absent or mapped to {@code null}.
     * @return a list with the same size and order as {@code keys}, using {@code defaultValue} when a key
     *         is absent or mapped to {@code null}; returns an empty list
     *         if {@code keys} is {@code null} or empty. If {@code map} is {@code null} or empty, the returned list
     *         contains {@code defaultValue} repeated {@code keys.size()} times.
     * @throws IllegalArgumentException if {@code defaultValue} is {@code null}.
     */
    public static <V> List<V> getValuesOrDefaultIfAbsent(final Map<?, ? extends V> map, final Collection<?> keys, final V defaultValue)
            throws IllegalArgumentException {
        N.checkArgNotNull(defaultValue, cs.defaultValue); // NOSONAR

        if (N.isEmpty(keys)) {
            return new ArrayList<>();
        } else if (N.isEmpty(map)) {
            return N.repeat(defaultValue, keys.size());
        }

        final List<V> result = new ArrayList<>(keys.size());
        V val = null;

        for (final Object key : keys) {
            //noinspection SuspiciousMethodCalls
            val = map.get(key);

            if (val == null) {
                result.add(defaultValue);
            } else {
                result.add(val);
            }
        }

        return result;
    }

    /**
     * Returns a new map containing entries that are present in both input maps.
     * The intersection contains entries whose keys are present in both maps with equal values.
     * The returned map's key-value pairs are taken from the first input map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = new HashMap<>();
     * map1.put("a", 1);
     * map1.put("b", 2);
     * map1.put("c", 3);
     *
     * Map<String, Integer> map2 = new HashMap<>();
     * map2.put("b", 2);
     * map2.put("c", 4);
     * map2.put("d", 5);
     *
     * // Only "b" is included because it has the same value in both maps
     * Map<String, Integer> result = Maps.intersection(map1, map2);   // result is {b=2}
     *
     * Map<String, String> map3 = new HashMap<>();
     * map3.put("x", "foo");
     * map3.put("y", "bar");
     *
     * Map<String, String> map4 = new HashMap<>();
     * map4.put("x", "foo");
     * map4.put("z", "baz");
     *
     * // Only "x" is included because it has the same value in both maps
     * Map<String, String> result2 = Maps.intersection(map3, map4);   // result2 is {x=foo}
     *
     * }</pre>
     *
     * <p>Values are compared with {@link N#deepEquals(Object, Object)}, so array-valued entries are
     * matched by content (including nested arrays) rather than by reference. This is the same value
     * equality used by {@link #difference(Map, Map)} and {@link #symmetricDifference(Map, Map)}.
     *
     * <p>A {@code null} or empty second map yields an empty result, since no entry can be common to it.
     * Like {@code difference} and {@code symmetricDifference}, the result mirrors the first map's runtime
     * type - and therefore its key semantics - where that type can be instantiated.
     *
     * @param <K> the type of keys in the map.
     * @param <V> the type of values in the map.
     * @param map the first input map; may be {@code null}.
     * @param map2 the second input map to find common entries with; may be {@code null}.
     * @return a new map containing entries present in both maps with equal values.
     *         If either map is {@code null} or empty, an empty map is returned.
     * @see N#intersection(int[], int[])
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Iterables#intersection(Set, Set)
     */
    public static <K, V> Map<K, V> intersection(final Map<K, V> map, final Map<? extends K, ? extends V> map2) {
        if (map == null) {
            return new HashMap<>();
        }

        if (N.isEmpty(map2)) {
            return newTargetMap(map, 0);
        }

        final Map<K, V> result = Maps.newTargetMap(map, N.size(map) / 2);
        Object val = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            val = map2.get(entry.getKey());

            if ((val != null && N.deepEquals(val, entry.getValue())) || (val == null && entry.getValue() == null && map2.containsKey(entry.getKey()))) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Calculates the difference between two maps.
     * The result is a map containing only those keys from the first map whose mapped values
     * differ from (or are absent in) the second map. For each included key the value is a
     * {@link Pair} where the left element is the value from the first map and the right element
     * is a {@link Nullable} holding the value from the second map (empty {@code Nullable} when
     * the key is not present in the second map).
     * <p>Entries whose key-value pair is equal in both maps are <i>not</i> included in the result.
     * <p>Values are compared with {@link N#deepEquals(Object, Object)}, so array-valued entries are
     * matched by content (including nested arrays) rather than by reference; two entries holding
     * equal-content arrays are therefore treated as unchanged and excluded from the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = N.asMap("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> map2 = N.asMap("a", 1, "b", 20, "d", 4);
     *
     * Map<String, Pair<Integer, Nullable<Integer>>> diff = Maps.difference(map1, map2);
     * // diff contains:
     * // "b" -> Pair.of(2, Nullable.of(20))    // different values
     * // "c" -> Pair.of(3, Nullable.empty())   // key only in map1
     * // "a" is NOT included because it has the same value in both maps
     * }</pre>
     *
     * <p>Note that this method only returns keys from the first map. Keys that exist only in the second map
     * are not included in the result. If you need to identify keys that are unique to each map,
     * use {@link #symmetricDifference(Map, Map)} instead.
     *
     * <p>If the first map is {@code null}, an empty map is returned. If the second map is {@code null},
     * all values from the first map will be paired with empty {@code Nullable} objects.
     *
     * @param <K> the type of keys in the maps.
     * @param <V> the type of values in the maps.
     * @param map the first map to compare.
     * @param map2 the second map to compare.
     * @return a map representing the difference between the two input maps.
     * @see #symmetricDifference(Map, Map)
     * @see Difference.MapDifference#of(Map, Map)
     * @see N#difference(Collection, Collection)
     * @see Iterables#difference(Set, Set)
     * @see #intersection(Map, Map)
     */
    public static <K, V> Map<K, Pair<V, Nullable<V>>> difference(final Map<K, V> map, final Map<? extends K, ? extends V> map2) {
        if (map == null) {
            return new HashMap<>();
        }

        // An empty map2 means every entry of map is a difference, so that branch knows its result size
        // exactly; only the comparing branch has to guess (the same /2 heuristic intersection uses).
        final Map<K, Pair<V, Nullable<V>>> result = newTargetMap(map, N.isEmpty(map2) ? N.size(map) : N.size(map) / 2);

        if (N.isEmpty(map2)) {
            for (final Map.Entry<K, V> entry : map.entrySet()) {
                result.put(entry.getKey(), Pair.of(entry.getValue(), Nullable.empty()));
            }
        } else {
            V val = null;

            for (final Map.Entry<K, V> entry : map.entrySet()) {
                val = map2.get(entry.getKey());

                if (val == null && !map2.containsKey(entry.getKey())) {
                    result.put(entry.getKey(), Pair.of(entry.getValue(), Nullable.empty()));
                } else if (!N.deepEquals(val, entry.getValue())) {
                    result.put(entry.getKey(), Pair.of(entry.getValue(), Nullable.of(val)));
                }
            }
        }

        return result;
    }

    /**
     * Returns a new map containing the symmetric difference between two maps.
     * The symmetric difference includes entries whose keys are present in only one of the maps
     * or entries with the same key but different values in both maps.
     *
     * <p>For each key in the result map, the value is a pair where:
     * <ul>
     * <li>If the key exists only in the first map, the pair contains the value from the first map and an empty Nullable</li>
     * <li>If the key exists only in the second map, the pair contains an empty {@code Nullable} and the value from the second map</li>
     * <li>If the key exists in both maps with different values, the pair contains both values</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = N.asMap("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> map2 = N.asMap("b", 2, "c", 4, "d", 5);
     *
     * Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(map1, map2);
     * // result contains:
     * // "a" -> Pair.of(Nullable.of(1), Nullable.empty())   // key only in map1
     * // "c" -> Pair.of(Nullable.of(3), Nullable.of(4))     // different values
     * // "d" -> Pair.of(Nullable.empty(), Nullable.of(5))   // key only in map2
     * // Note: "b" is not included because it has identical values in both maps
     * }</pre>
     *
     * <p>If either input map is {@code null}, it is treated as an empty map.
     *
     * <p>The result mirrors the first map's runtime type - and therefore its key semantics - where that type
     * can be instantiated, falling back to the second map's type when the first is {@code null}. This matches
     * {@link #difference(Map, Map)} and {@link #intersection(Map, Map)}.
     *
     * <p>Values are compared with {@link N#deepEquals(Object, Object)}, so array-valued entries are
     * matched by content (including nested arrays) rather than by reference; two entries holding
     * equal-content arrays are therefore treated as unchanged and excluded from the result.
     *
     * @param <K> the type of keys in the maps.
     * @param <V> the type of values in the maps.
     * @param map the first input map.
     * @param map2 the second input map.
     * @return a new map containing the symmetric difference between the two input maps.
     * @see #difference(Map, Map)
     * @see N#symmetricDifference(int[], int[])
     * @see N#symmetricDifference(Collection, Collection)
     * @see Iterables#symmetricDifference(Set, Set)
     * @see #intersection(Map, Map)
     */
    public static <K, V> Map<K, Pair<Nullable<V>, Nullable<V>>> symmetricDifference(final Map<K, V> map, final Map<? extends K, ? extends V> map2) {
        // The result's key semantics must come from whichever map supplies its keys, and that is `map`
        // whenever it exists: map2 only ever contributes keys that `map.containsKey` already rejected under
        // map's own equality. The old rule - use an IdentityHashMap if *either* input was one - could
        // therefore drop equals-based keys copied out of a HashMap into an identity-keyed result, where
        // result.get(key) then missed entries the map visibly contained. newTargetMap already mirrors an
        // IdentityHashMap first argument, exactly as difference(Map, Map) and intersection(Map, Map) do.
        final Map<?, ?> template = map == null ? map2 : map;
        final int expectedSize = map == null ? N.size(map2) : Math.max(N.size(map), N.size(map2));
        final Map<K, Pair<Nullable<V>, Nullable<V>>> result = Maps.newTargetMap(template, expectedSize);

        if (N.notEmpty(map)) {
            if (N.isEmpty(map2)) {
                for (final Map.Entry<K, V> entry : map.entrySet()) {
                    result.put(entry.getKey(), Pair.of(Nullable.of(entry.getValue()), Nullable.empty()));
                }
            } else {
                K key = null;
                V val2 = null;

                for (final Map.Entry<K, V> entry : map.entrySet()) {
                    key = entry.getKey();
                    val2 = map2.get(key);

                    if (val2 == null && !map2.containsKey(key)) {
                        result.put(key, Pair.of(Nullable.of(entry.getValue()), Nullable.empty()));
                    } else if (!N.deepEquals(val2, entry.getValue())) {
                        result.put(key, Pair.of(Nullable.of(entry.getValue()), Nullable.of(val2)));
                    }
                }
            }
        }

        if (N.notEmpty(map2)) {
            if (N.isEmpty(map)) {
                for (final Map.Entry<? extends K, ? extends V> entry : map2.entrySet()) {
                    result.put(entry.getKey(), Pair.of(Nullable.empty(), Nullable.of(entry.getValue())));
                }
            } else {
                for (final Map.Entry<? extends K, ? extends V> entry : map2.entrySet()) {
                    if (!map.containsKey(entry.getKey())) {
                        result.put(entry.getKey(), Pair.of(Nullable.empty(), Nullable.of(entry.getValue())));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Checks if the specified map contains the specified entry.
     *
     * <p><b>Note:</b> A {@code null} entry is tolerated here ({@code false} is returned), matching
     * the companion {@link #removeEntry(Map, Map.Entry)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     *
     * Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("a", 1);
     * Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("a", 2);
     *
     * // contains1 = true
     * boolean contains1 = Maps.containsEntry(map, entry1);
     *
     * boolean contains2 = Maps.containsEntry(map, entry2);
     * // contains2 = false (value doesn't match)
     * }</pre>
     *
     * @param map the map to check, may be {@code null}.
     * @param entry the entry to check for, may be {@code null}.
     * @return {@code true} if the map contains the specified entry; {@code false} otherwise,
     *         including when {@code map} or {@code entry} is {@code null}.
     * @see #removeEntry(Map, Map.Entry)
     */
    public static boolean containsEntry(final Map<?, ?> map, final Map.Entry<?, ?> entry) {
        if (entry == null) {
            return false;
        }

        return containsEntry(map, entry.getKey(), entry.getValue());
    }

    /**
     * Checks if the specified map contains the specified key-value pair.
     *
     * <p><b>Value equality:</b> values are compared with {@link N#deepEquals(Object, Object)}, so
     * array-valued entries are matched by content (including nested arrays). This is the same value
     * equality used by {@link #intersection(Map, Map)}, {@link #difference(Map, Map)} and
     * {@link #symmetricDifference(Map, Map)}, and it is a deliberate divergence from
     * {@link Map#remove(Object, Object)}/{@link Map#replace(Object, Object, Object)}, which use
     * {@link java.util.Objects#equals(Object, Object)} and therefore compare arrays by identity.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", null);
     *
     * // contains1 = true
     * boolean contains1 = Maps.containsEntry(map, "a", 1);
     *
     * // contains2 = false
     * boolean contains2 = Maps.containsEntry(map, "a", 2);
     *
     * // contains3 = true
     * boolean contains3 = Maps.containsEntry(map, "b", null);
     *
     * boolean contains4 = Maps.containsEntry(map, "c", null);
     * // contains4 = false (key not present)
     * }</pre>
     *
     * @param map the map to be checked.
     * @param key the key whose presence in the map is to be tested.
     * @param value the value whose presence in the map is to be tested.
     * @return {@code true} if the map contains the specified key-value pair, {@code false} otherwise.
     * @see N#deepEquals(Object, Object)
     */
    public static boolean containsEntry(final Map<?, ?> map, final Object key, final Object value) {
        if (N.isEmpty(map)) {
            return false;
        }

        final Object val = map.get(key);

        // deepEquals, not equals: this must agree with intersection/difference/symmetricDifference, which
        // compare array-valued entries by content. N.equals delegates to Objects.equals, under which two
        // equal-content arrays are unequal - so the query and the set operations used to contradict each other.
        return val == null ? value == null && map.containsKey(key) : N.deepEquals(val, value);
    }

    /**
     * Puts if the specified key is not already associated with a value (or is mapped to {@code null}).
     *
     * <p>Here, absent means the key is missing from the specified map or maps to {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = new HashMap<>();
     * map.put("key1", "value1");
     * map.put("key2", null);
     *
     * // result1 = "value1" (key1 already has a non-null value, not changed)
     * // map = {key1=value1, key2=null}
     * String result1 = Maps.putIfAbsent(map, "key1", "newValue");
     *
     * // result2 = null (key2 was null, now set to value2)
     * // map = {key1=value1, key2=value2}
     * String result2 = Maps.putIfAbsent(map, "key2", "value2");
     *
     * String result3 = Maps.putIfAbsent(map, "key3", "value3");
     * // result3 = null (key3 was absent, now set to value3)
     * // map = {key1=value1, key2=value2, key3=value3}
     * }</pre>
     *
     * <p><b>A {@code null} {@code value} is stored, not rejected</b> - by this method, that is - exactly as
     * {@link Map#putIfAbsent(Object, Object)} stores one. Whether the store then succeeds is the supplied
     * map's decision: a map that forbids {@code null} values, such as {@link java.util.concurrent.ConcurrentHashMap}
     * or {@link java.util.Hashtable}, throws {@link NullPointerException} from its own {@code put}.
     * On a map that does permit them, the key then exists but is still <em>absent</em>
     * under this class's present/absent model (see the class documentation), so a later call with the same
     * key writes again rather than treating the mapping as established. Use
     * {@link #getOrPutIfAbsent(Map, Object, Supplier)} when the stored value must be non-{@code null}.</p>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param map the map to put the value in; must not be {@code null}.
     * @param key the key to associate the value with.
     * @param value the value to put if the key is absent; may be {@code null}, in which case {@code null} is
     *        stored - provided the supplied map permits {@code null} values.
     * @return the existing {@code non-null} value associated with the specified key, or {@code null} if the key was absent or mapped to {@code null} (in which case the new value is put).
     * @throws IllegalArgumentException if {@code map} is {@code null}.
     * @see Map#putIfAbsent(Object, Object)
     * @see #putIfAbsent(Map, Object, Supplier)
     */
    @MayReturnNull
    public static <K, V> V putIfAbsent(final Map<K, V> map, final K key, final V value) throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);

        V v = map.get(key);

        if (v == null) {
            v = map.put(key, value);
        }

        return v;
    }

    /**
     * Puts a supplied value if the specified key is not already associated with a value (or is mapped to
     * {@code null}).
     *
     * <p>Here, absent means the key is missing from the specified map or maps to {@code null}.
     * The {@code supplier} is invoked only when the key is absent, and exactly once. This behaves
     * uniformly for every map type (including a {@link java.util.concurrent.ConcurrentMap}): it
     * always returns the <em>previous</em> value, never the newly created one.
     *
     * <p>Pass the supplier as a typed {@code Supplier} local variable or wrap it with
     * {@code Fn.s(...)}. A bare lambda or inexact method reference such as {@code ArrayList::new}
     * can be ambiguous: {@code putIfAbsent(map, key, () -> v)} is an
     * ambiguous method call against {@link #putIfAbsent(Map, Object, Object)}. On a
     * {@code Map<K, Object>} a {@code Supplier<Object>} is itself a valid value, so even a typed
     * variable stays ambiguous; use a narrower map value type or perform the lookup and insertion explicitly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<String>> map = new HashMap<>();
     * map.put("key1", Arrays.asList("a", "b"));
     * // Declare the supplier explicitly to avoid overload ambiguity with putIfAbsent(map, key, value)
     * Supplier<List<String>> supplier = ArrayList::new;
     *
     * // Supplier is only called when the key is absent
     * // result1 = [a, b] (key1 already has a non-null value, supplier not called)
     * // map = {key1=[a, b]}
     * List<String> result1 = Maps.putIfAbsent(map, "key1", supplier);
     *
     * List<String> result2 = Maps.putIfAbsent(map, "key2", supplier);
     * // result2 = null (key2 was absent, supplier called and value set)
     * // map = {key1=[a, b], key2=[]}
     * }</pre>
     *
     * <p><b>Note:</b> Like {@link Map#putIfAbsent(Object, Object)}, this returns the <em>previous</em>
     * value, so it returns {@code null} whenever the key was absent or mapped to {@code null}. If you
     * instead want the value that ends up associated with the key (the existing one, or the one just
     * created by the supplier), use {@link #getOrPutIfAbsent(Map, Object, Supplier)}.</p>
     *
     * <p><b>A {@code null} from the supplier is stored, not rejected</b> by this method, matching
     * {@link #putIfAbsent(Map, Object, Object)} with a {@code null} value and
     * {@link Map#putIfAbsent(Object, Object)}. As there, a map that forbids {@code null} values throws
     * {@link NullPointerException} from its own {@code put}. On a map that permits them, the key then
     * exists but is still <em>absent</em> under this
     * class's present/absent model, so a later call runs the supplier again and overwrites. Use
     * {@link #getOrPutIfAbsent(Map, Object, Supplier)}, which rejects a {@code null} supplier result with
     * {@link IllegalArgumentException}, when the stored value must be non-{@code null}.</p>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param map the map to put the value in; must not be {@code null}.
     * @param key the key to associate the value with.
     * @param supplier the supplier to get the value from if the key is absent; must not be {@code null}, but
     *        may itself return {@code null}, in which case {@code null} is stored - provided the supplied
     *        map permits {@code null} values
     * @return the existing {@code non-null} value associated with the specified key, or {@code null} if the
     *         key was absent or mapped to {@code null} (in which case the supplier is invoked and its
     *         value is put).
     * @throws IllegalArgumentException if {@code map} or {@code supplier} is {@code null}.
     * @see Map#putIfAbsent(Object, Object)
     * @see #putIfAbsent(Map, Object, Object)
     * @see #getOrPutIfAbsent(Map, Object, Supplier)
     */
    @MayReturnNull
    public static <K, V> V putIfAbsent(final Map<K, V> map, final K key, final Supplier<V> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);
        N.checkArgNotNull(supplier, cs.supplier);

        V v = map.get(key);

        if (v == null) {
            v = map.put(key, supplier.get());
        }

        return v;
    }

    /**
     * Puts all entries from the source map into the target map, but only if the key passes the specified filter predicate.
     * This method iterates through all entries in the source map and adds them to the target map if the key satisfies the filter condition.
     * The target map is modified in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> target = new HashMap<>();
     * target.put("a", 1);
     *
     * Map<String, Integer> source = new HashMap<>();
     * source.put("b", 2);
     * source.put("c", 3);
     * source.put("abc", 4);
     *
     * boolean changed = Maps.putAllIf(target, source, key -> key.length() > 1);
     * // changed: true
     * // target: {a=1, abc=4}
     * }</pre>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param targetMap the target map to which entries will be added; must not be {@code null}.
     * @param sourceMap the source map from which entries will be taken.
     * @param keyFilter a predicate that filters keys to be added to the target map
     * @return {@code true} if any source entry passed the filter and was put into the target map,
     *         {@code false} otherwise. A {@code true} result does not necessarily mean that the
     *         target map's contents changed (an equal existing mapping may have been replaced).
     * @throws IllegalArgumentException if {@code targetMap} or {@code keyFilter} is {@code null}.
     */
    @Beta
    public static <K, V> boolean putAllIf(final Map<K, V> targetMap, final Map<? extends K, ? extends V> sourceMap, final Predicate<? super K> keyFilter)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetMap, cs.targetMap);
        N.checkArgNotNull(keyFilter, cs.keyFilter);

        if (N.isEmpty(sourceMap)) {
            return false;
        }

        // `anyPut`, not `changed`: a put that overwrites an equal existing mapping leaves the target's
        // contents identical, and the documented @return is "an entry passed the filter and was put".
        boolean anyPut = false;

        for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
            if (keyFilter.test(entry.getKey())) {
                targetMap.put(entry.getKey(), entry.getValue());
                anyPut = true;
            }
        }

        return anyPut;
    }

    /**
     * Puts all entries from the source map into the target map, but only if the key and value pass the specified filter predicate.
     * This method iterates through all entries in the source map and adds them to the target map if both the key and value satisfy the filter condition.
     * The target map is modified in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> target = new HashMap<>();
     * target.put("a", 1);
     *
     * Map<String, Integer> source = new HashMap<>();
     * source.put("b", 2);
     * source.put("c", 3);
     * source.put("d", 10);
     *
     * boolean changed = Maps.putAllIf(target, source, (key, value) -> value > 2);
     * // changed: true
     * // target: {a=1, c=3, d=10}
     * }</pre>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     * @param targetMap the target map to which entries will be added; must not be {@code null}.
     * @param sourceMap the source map from which entries will be taken.
     * @param entryFilter a predicate that filters keys and values to be added to the target map
     * @return {@code true} if any source entry passed the filter and was put into the target map,
     *         {@code false} otherwise. A {@code true} result does not necessarily mean that the
     *         target map's contents changed (an equal existing mapping may have been replaced).
     * @throws IllegalArgumentException if {@code targetMap} or {@code entryFilter} is {@code null}.
     */
    @Beta
    public static <K, V> boolean putAllIf(final Map<K, V> targetMap, final Map<? extends K, ? extends V> sourceMap,
            final BiPredicate<? super K, ? super V> entryFilter) throws IllegalArgumentException {
        N.checkArgNotNull(targetMap, cs.targetMap);
        N.checkArgNotNull(entryFilter, cs.entryFilter);

        if (N.isEmpty(sourceMap)) {
            return false;
        }

        // See putAllIf(Map, Map, Predicate) for why this is not called `changed`.
        boolean anyPut = false;

        for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
            if (entryFilter.test(entry.getKey(), entry.getValue())) {
                targetMap.put(entry.getKey(), entry.getValue());
                anyPut = true;
            }
        }

        return anyPut;
    }

    /**
     * Merges the given non-{@code null} value into the map for the specified key, mirroring
     * {@link Map#merge(Object, Object, BiFunction)} as a static helper that behaves uniformly across map types.
     *
     * <p>If the key is absent (missing or mapped to {@code null}), it is associated with {@code value} and the
     * {@code mergeFunction} is not invoked. Otherwise the {@code mergeFunction} is applied to the existing value
     * (first argument) and {@code value} (second argument); if it returns a non-{@code null} result the key is
     * re-associated with it, and if it returns {@code null} the key is removed. The resulting value (or
     * {@code null} when the key was removed) is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> counts = new HashMap<>();
     *
     * // Absent key -> stored directly, mergeFunction not called
     * Maps.merge(counts, "a", 1, Integer::sum);   // returns 1; counts = {a=1}
     *
     * // Present key -> existing value combined with the new value
     * Maps.merge(counts, "a", 5, Integer::sum);   // returns 6; counts = {a=6}
     *
     * // mergeFunction returning null removes the entry
     * Maps.merge(counts, "a", 6, (oldV, v) -> oldV.equals(v) ? null : oldV + v);
     * // returns null; counts = {} ("a" removed)
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map in which the value is to be merged; must not be {@code null}.
     * @param key the key with which the resulting value is to be associated.
     * @param value the non-{@code null} value to merge with the existing value, or to store directly if the key
     *        is absent.
     * @param mergeFunction the function combining the existing value and {@code value} when the key is present;
     * A {@code null} result removes the key.
     * @return the new value associated with the key, or {@code null} if the key was removed (because
     *         {@code mergeFunction} returned {@code null}).
     * @throws IllegalArgumentException if {@code map} or {@code value} is {@code null}, or if {@code mergeFunction}
     *         is {@code null}.
     * @see Map#merge(Object, Object, BiFunction)
     */
    @MayReturnNull
    public static <K, V> V merge(final Map<K, V> map, final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> mergeFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(map, cs.map);
        N.checkArgNotNull(value, cs.value);
        N.checkArgNotNull(mergeFunction, cs.mergeFunction);

        final V oldValue = map.get(key);
        final V newValue = oldValue == null ? value : mergeFunction.apply(oldValue, value);

        if (newValue == null) {
            map.remove(key);
        } else {
            map.put(key, newValue);
        }

        return newValue;
    }

    /**
     * Removes the specified entry from the map.
     *
     * <p><b>Note:</b> A {@code null} entry is tolerated here ({@code false} is returned), matching
     * the companion {@link #containsEntry(Map, Map.Entry)}. This keeps the query/mutate idiom
     * {@code if (containsEntry(m, e)) removeEntry(m, e)} safe for a {@code null} entry.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * Map.Entry<String, Integer> entry = N.newEntry("a", 1);
     * boolean removed = Maps.removeEntry(map, entry);   // returns true; map is {b=2}
     * }</pre>
     *
     * @param map the map from which the entry is to be removed.
     * @param entry the entry to be removed from the map, may be {@code null}.
     * @return {@code true} if the entry was removed, {@code false} otherwise, including when
     *         {@code map} or {@code entry} is {@code null}.
     * @see Map#remove(Object, Object)
     */
    public static boolean removeEntry(final Map<?, ?> map, final Map.Entry<?, ?> entry) {
        if (entry == null) {
            return false;
        }

        return removeEntry(map, entry.getKey(), entry.getValue());
    }

    /**
     * Removes the specified key-value pair from the map.
     * This method removes an entry from the map only if the key is mapped to the specified value.
     * If the key is not present in the map or is mapped to a different value, the map remains unchanged.
     *
     * <p><b>Value equality:</b> values are compared with {@link N#deepEquals(Object, Object)}, so
     * array-valued entries are matched by content (including nested arrays). This is the same value
     * equality used by {@link #intersection(Map, Map)}, {@link #difference(Map, Map)} and
     * {@link #symmetricDifference(Map, Map)}, and it is a deliberate divergence from
     * {@link Map#remove(Object, Object)}/{@link Map#replace(Object, Object, Object)}, which use
     * {@link java.util.Objects#equals(Object, Object)} and therefore compare arrays by identity.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     *
     * boolean removed1 = Maps.removeEntry(map, "a", 1);   // returns true; map is {b=2}
     * boolean removed2 = Maps.removeEntry(map, "b", 3);   // returns false; value does not match
     * }</pre>
     *
     * @param map the map from which the entry is to be removed.
     * @param key the key whose associated value is to be removed.
     * @param value the value to be removed.
     * @return {@code true} if the entry was removed, {@code false} otherwise.
     * @see Map#remove(Object, Object)
     */
    public static boolean removeEntry(final Map<?, ?> map, final Object key, final Object value) {
        if (N.isEmpty(map)) {
            return false;
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        final Object curValue = map.get(key);

        // deepEquals: see containsEntry(Map, Object, Object) - the query and the removal must agree.
        //noinspection SuspiciousMethodCalls
        if (!N.deepEquals(curValue, value) || (curValue == null && !map.containsKey(key))) {
            return false;
        }

        //noinspection SuspiciousMethodCalls
        map.remove(key);
        return true;
    }

    /**
     * Removes the specified entries from the map.
     * This method removes all entries from the map that have matching key-value pairs in the entriesToRemove map.
     * An entry is removed only if both the key and value match exactly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     *
     * Map<String, Integer> entriesToRemove = new HashMap<>();
     * entriesToRemove.put("a", 1);
     * entriesToRemove.put("b", 5);
     *
     * boolean changed = Maps.removeEntries(map, entriesToRemove);
     * // changed: true
     * // map: {b=2, c=3}  // Only "a"=1 was removed
     * }</pre>
     *
     * <p>{@code entriesToRemove} may be {@code map} itself or another view backed by it.</p>
     *
     * <p><b>Value equality:</b> values are compared with {@link N#deepEquals(Object, Object)}, so
     * array-valued entries are matched by content (including nested arrays). This is the same value
     * equality used by {@link #intersection(Map, Map)}, {@link #difference(Map, Map)} and
     * {@link #symmetricDifference(Map, Map)}, and it is a deliberate divergence from
     * {@link Map#remove(Object, Object)}/{@link Map#replace(Object, Object, Object)}, which use
     * {@link java.util.Objects#equals(Object, Object)} and therefore compare arrays by identity.</p>
     *
     * @param map the map from which the entries are to be removed.
     * @param entriesToRemove the map containing the entries to be removed.
     * @return {@code true} if any entries were removed, {@code false} otherwise.
     */
    public static boolean removeEntries(final Map<?, ?> map, final Map<?, ?> entriesToRemove) {
        if (N.isEmpty(map) || N.isEmpty(entriesToRemove)) {
            return false;
        }

        final int originalSize = map.size();

        final List<Map.Entry<?, ?>> entries = new ArrayList<>(entriesToRemove.size());

        for (final Map.Entry<?, ?> entry : entriesToRemove.entrySet()) {
            entries.add(N.newImmutableEntry(entry.getKey(), entry.getValue()));
        }

        for (final Map.Entry<?, ?> entry : entries) {
            final Object curValue = map.get(entry.getKey());
            // deepEquals: see containsEntry(Map, Object, Object).
            if (N.deepEquals(curValue, entry.getValue()) && (curValue != null || map.containsKey(entry.getKey()))) {
                map.remove(entry.getKey());
            }
        }

        return map.size() < originalSize;
    }

    /**
     * Removes the specified keys from the map.
     * This method removes all entries from the map whose keys are contained in the provided collection.
     * If any of the keys in the collection are not present in the map, they are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     *
     * List<String> keysToRemove = Arrays.asList("a", "c", "d");
     * boolean changed = Maps.removeKeys(map, keysToRemove);
     * // changed: true
     * // map: {b=2}
     * }</pre>
     *
     * <p>{@code keysToRemove} may be a live view backed by {@code map}, such as {@link Map#keySet()}.</p>
     *
     * @param map the map from which the keys are to be removed.
     * @param keysToRemove the collection of keys to be removed from the map.
     * @return {@code true} if any keys were removed, {@code false} otherwise.
     */
    public static boolean removeKeys(final Map<?, ?> map, final Collection<?> keysToRemove) {
        if (N.isEmpty(map) || N.isEmpty(keysToRemove)) {
            return false;
        }

        final int originalSize = map.size();

        for (final Object key : new ArrayList<>(keysToRemove)) {
            map.remove(key);
        }

        return map.size() < originalSize;
    }

    /**
     * Removes entries from the specified map that match the given filter predicate.
     * This method iterates through all entries in the map and removes those that satisfy the filter condition.
     * The map is modified in place.
     *
     * <p>For a non-mutating snapshot containing the matching entries, use
     * {@link #filter(Map, Predicate)}.</p>
     *
     * <p><b>All matches are decided before any entry is removed</b>, and this is a contract, not an
     * implementation detail: the whole map is scanned first and the matching keys are removed only after the
     * scan completes. Three consequences follow, all of which differ from delegating to
     * {@code map.entrySet().removeIf(..)}:</p>
     * <ul>
     *   <li>If {@code filter} throws, the map is left <b>completely unchanged</b> - earlier matches are not
     *       removed. The view-based idiom removes as it iterates and would leave the map half-modified.</li>
     *   <li>A map that rejects removal is only rejected when there is something to remove: on an
     *       unmodifiable map a filter that matches nothing returns {@code false} rather than throwing
     *       {@link UnsupportedOperationException}, because {@code Map.remove} is never reached.
     *       {@code map.entrySet().removeIf(..)} throws unconditionally.</li>
     *   <li>Removal is by key, so on a concurrent map an entry whose value changed after the scan is still
     *       removed - the decision is the one the filter made, not a re-test at removal time. Removal is
     *       therefore not atomic with the test; coordinate externally if that matters.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     *
     * boolean changed = Maps.removeIf(map, entry -> entry.getValue() > 1);
     * // changed: true
     * // map: {a=1}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map from which entries are to be removed.
     * @param filter the predicate used to determine which entries to remove.
     * @return {@code true} if one or more entries were removed, {@code false} otherwise.
     * @throws IllegalArgumentException if {@code filter} is {@code null}.
     */
    public static <K, V> boolean removeIf(final Map<K, V> map, final Predicate<? super Map.Entry<K, V>> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        if (N.isEmpty(map)) {
            return false;
        }

        List<K> keysToRemove = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry)) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(REMOVAL_KEYS_INIT_CAPACITY);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes entries from the specified map that match the given filter predicate based on key and value.
     * This method iterates through all entries in the map and removes those whose key and value satisfy the filter condition.
     * The map is modified in place.
     *
     * <p>For a non-mutating snapshot containing the matching entries, use
     * {@link #filter(Map, BiPredicate)}.</p>
     *
     * <p><b>All matches are decided before any entry is removed</b>, and this is a contract, not an
     * implementation detail: the whole map is scanned first and the matching keys are removed only after the
     * scan completes. Three consequences follow, all of which differ from delegating to
     * {@code map.entrySet().removeIf(..)}:</p>
     * <ul>
     *   <li>If {@code filter} throws, the map is left <b>completely unchanged</b> - earlier matches are not
     *       removed. The view-based idiom removes as it iterates and would leave the map half-modified.</li>
     *   <li>A map that rejects removal is only rejected when there is something to remove: on an
     *       unmodifiable map a filter that matches nothing returns {@code false} rather than throwing
     *       {@link UnsupportedOperationException}, because {@code Map.remove} is never reached.
     *       {@code map.entrySet().removeIf(..)} throws unconditionally.</li>
     *   <li>Removal is by key, so on a concurrent map an entry whose value changed after the scan is still
     *       removed - the decision is the one the filter made, not a re-test at removal time. Removal is
     *       therefore not atomic with the test; coordinate externally if that matters.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("apple", 1);
     * map.put("banana", 2);
     * map.put("cherry", 3);
     *
     * boolean changed = Maps.removeIf(map, (key, value) -> key.length() > 5 && value > 1);
     * // changed: true
     * // map: {apple=1}  // "banana" and "cherry" were removed (both have length > 5 and value > 1)
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map from which entries are to be removed.
     * @param filter the predicate used to determine which entries to remove.
     * @return {@code true} if one or more entries were removed, {@code false} otherwise.
     * @throws IllegalArgumentException if {@code filter} is {@code null}.
     */
    public static <K, V> boolean removeIf(final Map<K, V> map, final BiPredicate<? super K, ? super V> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        if (N.isEmpty(map)) {
            return false;
        }

        // Collect-then-remove - see removeIf(Map, Predicate) for why this is not a view removeIf.
        List<K> keysToRemove = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (filter.test(entry.getKey(), entry.getValue())) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(REMOVAL_KEYS_INIT_CAPACITY);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes entries from the specified map that match the given key filter predicate.
     * This method iterates through all entries in the map and removes those whose keys satisfy the filter condition.
     * The map is modified in place.
     *
     * <p>For a non-mutating snapshot containing entries with matching keys, use
     * {@link #filterByKey(Map, Predicate)}.</p>
     *
     * <p><b>All matches are decided before any entry is removed</b>, and this is a contract, not an
     * implementation detail: the whole map is scanned first and the matching keys are removed only after the
     * scan completes. Three consequences follow, all of which differ from delegating to
     * {@code map.keySet().removeIf(..)}:</p>
     * <ul>
     *   <li>If {@code filter} throws, the map is left <b>completely unchanged</b> - earlier matches are not
     *       removed. The view-based idiom removes as it iterates and would leave the map half-modified.</li>
     *   <li>A map that rejects removal is only rejected when there is something to remove: on an
     *       unmodifiable map a filter that matches nothing returns {@code false} rather than throwing
     *       {@link UnsupportedOperationException}, because {@code Map.remove} is never reached.
     *       {@code map.keySet().removeIf(..)} throws unconditionally.</li>
     *   <li>Removal is by key, so on a concurrent map an entry whose value changed after the scan is still
     *       removed - the decision is the one the filter made, not a re-test at removal time. Removal is
     *       therefore not atomic with the test; coordinate externally if that matters.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("apple", 1);
     * map.put("banana", 2);
     * map.put("cherry", 3);
     *
     * boolean changed = Maps.removeIfKey(map, key -> key.startsWith("b"));
     * // changed: true
     * // map: {apple=1, cherry=3}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param map the map from which entries are to be removed.
     * @param filter the predicate used to determine which keys to remove.
     * @return {@code true} if one or more entries were removed, {@code false} otherwise.
     * @throws IllegalArgumentException if {@code filter} is {@code null}.
     * @see #filterByKey(Map, Predicate)
     */
    public static <K> boolean removeIfKey(final Map<K, ?> map, final Predicate<? super K> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        if (N.isEmpty(map)) {
            return false;
        }

        // Collect-then-remove - see removeIf(Map, Predicate) for why this is not a view removeIf.
        List<K> keysToRemove = null;

        for (final Map.Entry<K, ?> entry : map.entrySet()) {
            if (filter.test(entry.getKey())) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(REMOVAL_KEYS_INIT_CAPACITY);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final K key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Removes entries from the specified map that match the given value filter predicate.
     * This method iterates through all entries in the map and removes those whose values satisfy the filter condition.
     * The map is modified in place.
     *
     * <p>For a non-mutating snapshot containing entries with matching values, use
     * {@link #filterByValue(Map, Predicate)}.</p>
     *
     * <p><b>All matches are decided before any entry is removed</b>, and this is a contract, not an
     * implementation detail: the whole map is scanned first and the matching keys are removed only after the
     * scan completes. Three consequences follow, all of which differ from delegating to
     * {@code map.values().removeIf(..)}:</p>
     * <ul>
     *   <li>If {@code filter} throws, the map is left <b>completely unchanged</b> - earlier matches are not
     *       removed. The view-based idiom removes as it iterates and would leave the map half-modified.</li>
     *   <li>A map that rejects removal is only rejected when there is something to remove: on an
     *       unmodifiable map a filter that matches nothing returns {@code false} rather than throwing
     *       {@link UnsupportedOperationException}, because {@code Map.remove} is never reached.
     *       {@code map.values().removeIf(..)} throws unconditionally.</li>
     *   <li>Removal is by key, so on a concurrent map an entry whose value changed after the scan is still
     *       removed - the decision is the one the filter made, not a re-test at removal time. Removal is
     *       therefore not atomic with the test; coordinate externally if that matters.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     * map.put("d", 2);
     *
     * boolean changed = Maps.removeIfValue(map, value -> value == 2);
     * // changed: true
     * // map: {a=1, c=3}
     * }</pre>
     *
     * @param <V> the type of mapped values.
     * @param map the map from which entries are to be removed.
     * @param filter the predicate used to determine which values to remove.
     * @return {@code true} if one or more entries were removed, {@code false} otherwise.
     * @throws IllegalArgumentException if {@code filter} is {@code null}.
     * @see #filterByValue(Map, Predicate)
     */
    public static <V> boolean removeIfValue(final Map<?, V> map, final Predicate<? super V> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        if (N.isEmpty(map)) {
            return false;
        }

        // Collect-then-remove - see removeIf(Map, Predicate) for why this is not a view removeIf.
        List<Object> keysToRemove = null;

        for (final Map.Entry<?, V> entry : map.entrySet()) {
            if (filter.test(entry.getValue())) {
                if (keysToRemove == null) {
                    keysToRemove = new ArrayList<>(REMOVAL_KEYS_INIT_CAPACITY);
                }

                keysToRemove.add(entry.getKey());
            }
        }

        if (N.notEmpty(keysToRemove)) {
            for (final Object key : keysToRemove) {
                map.remove(key);
            }

            return true;
        }

        return false;
    }

    /**
     * Replaces the entry for the specified key with the new value if the key is present in the map.
     * This method updates the value for a key only if the key exists in the map.
     * If the key is not present, the map remains unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     *
     * Integer oldValue1 = Maps.replace(map, "a", 10);   // returns 1
     * Integer oldValue2 = Maps.replace(map, "c", 30);   // returns null
     * // map: {a=10, b=2}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map in which the entry is to be replaced.
     * @param key the key with which the specified value is associated.
     * @param newValue the new value to be associated with the specified key.
     * @return the previous value associated with the specified key, or {@code null} if the map is
     *         {@code null}/empty, there was no mapping for the key, or the key was previously mapped to {@code null}.
     */
    @MayReturnNull
    public static <K, V> V replace(final Map<K, V> map, final K key, final V newValue) {
        if (N.isEmpty(map)) {
            return null;
        }

        V curValue = null;

        if (((curValue = map.get(key)) != null) || map.containsKey(key)) {
            curValue = map.put(key, newValue);
        }

        return curValue;
    }

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     * This method updates the value for a key only if the current value matches the oldValue parameter.
     * If the key is not present or the current value doesn't match oldValue, the map remains unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     *
     * boolean replaced1 = Maps.replace(map, "a", 1, 10);   // returns true; map is {a=10, b=2}
     * boolean replaced2 = Maps.replace(map, "b", 3, 20);   // returns false; old value does not match
     * }</pre>
     *
     * <p><b>Value equality:</b> values are compared with {@link N#deepEquals(Object, Object)}, so
     * array-valued entries are matched by content (including nested arrays). This is the same value
     * equality used by {@link #intersection(Map, Map)}, {@link #difference(Map, Map)} and
     * {@link #symmetricDifference(Map, Map)}, and it is a deliberate divergence from
     * {@link Map#remove(Object, Object)}/{@link Map#replace(Object, Object, Object)}, which use
     * {@link java.util.Objects#equals(Object, Object)} and therefore compare arrays by identity.</p>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map in which the entry is to be replaced.
     * @param key the key with which the specified value is associated.
     * @param oldValue the expected current value associated with the specified key.
     * @param newValue the new value to be associated with the specified key.
     * @return {@code true} if the value was replaced, {@code false} otherwise.
     * @see N#deepEquals(Object, Object)
     * @see Map#replace(Object, Object, Object)
     */
    public static <K, V> boolean replace(final Map<K, V> map, final K key, final V oldValue, final V newValue) {
        if (N.isEmpty(map)) {
            return false;
        }

        final Object curValue = map.get(key);

        // deepEquals: see containsEntry(Map, Object, Object).
        if (!N.deepEquals(curValue, oldValue) || (curValue == null && !map.containsKey(key))) {
            return false;
        }

        map.put(key, newValue);
        return true;
    }

    /**
     * Replaces each entry's value with the result of applying the given function to that entry.
     * This method applies the provided function to each key-value pair in the map and updates the value with the function's result.
     * The function receives both the key and the current value as parameters.
     *
     * <p>This is a {@code null}-tolerant delegate to {@link Map#replaceAll(BiFunction)}: a {@code null} or empty
     * map is a no-op, and everything else - iteration order, fail-fast behavior, atomicity and any
     * implementation-specific override - is exactly that of the supplied map.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     *
     * Maps.replaceAll(map, (key, value) -> value * 10);
     * // map: {a=10, b=20, c=30}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map in which the entries are to be replaced.
     * @param function the function to apply to each entry to compute a new value.
     * @throws IllegalArgumentException if {@code function} is {@code null}.
     * @throws UnsupportedOperationException if the supplied map does not support {@code put} / {@code setValue}.
     * @throws ConcurrentModificationException if the supplied map detects a structural modification during the
     *         replacement (most JDK maps compare their modification count and fail fast).
     * @see Map#replaceAll(BiFunction)
     */
    public static <K, V> void replaceAll(final Map<K, V> map, final BiFunction<? super K, ? super V, ? extends V> function)
            throws IllegalArgumentException, UnsupportedOperationException, ConcurrentModificationException {
        N.checkArgNotNull(function, cs.function);

        if (N.isEmpty(map)) {
            return;
        }

        // Delegate rather than hand-rolling an entrySet/setValue loop: Map.replaceAll carries each
        // implementation's own fail-fast (HashMap and TreeMap compare modCount and throw
        // ConcurrentModificationException) plus any ConcurrentMap-specific override. The previous loop
        // detected concurrent removal only via an IllegalStateException from Entry.setValue, which the
        // JDK's own map entries never throw.
        map.replaceAll(function);
    }

    // Replaced with N.forEach(Map....)

    /**
     * Filters the entries of the specified map based on the given predicate.
     * This method creates a new map containing only the entries that satisfy the predicate condition.
     * The predicate is tested against each Map.Entry in the original map.
     * The returned map is of the same type as the input map if possible.
     *
     * <p>This method leaves the input map unchanged. To remove matching entries from the supplied
     * map in place, use {@link #removeIf(Map, Predicate)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 1);
     * map.put("b", 2);
     * map.put("c", 3);
     *
     * Map<String, Integer> filtered = Maps.filter(map, entry -> entry.getValue() > 1);
     * // filtered: {b=2, c=3}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map to be filtered.
     * @param predicate the predicate used to filter the entries.
     * @return a new map containing only the entries that match the predicate.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see N#filter(Iterable, Predicate)
     * @see Iterators#filter(Iterator, Predicate)
     */
    public static <K, V> Map<K, V> filter(final Map<K, V> map, final Predicate<? super Map.Entry<K, V>> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, map.size());

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the entries of the specified map based on the given predicate applied to key-value pairs.
     * This method creates a new map containing only the entries whose key and value satisfy the predicate condition.
     * The predicate receives both the key and value as separate parameters.
     * The returned map is of the same type as the input map if possible.
     *
     * <p>This method leaves the input map unchanged. To remove matching entries from the supplied
     * map in place, use {@link #removeIf(Map, BiPredicate)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("apple", 5);
     * map.put("banana", 2);
     * map.put("cherry", 8);
     *
     * Map<String, Integer> filtered = Maps.filter(map, (key, value) -> key.length() > 5 || value > 4);
     * // filtered: {apple=5, banana=2, cherry=8}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map to be filtered.
     * @param predicate the predicate used to filter the entries.
     * @return a new map containing only the entries that match the predicate.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see N#filter(Iterable, Predicate)
     * @see Iterators#filter(Iterator, Predicate)
     */
    public static <K, V> Map<K, V> filter(final Map<K, V> map, final BiPredicate<? super K, ? super V> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, map.size());

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the entries of the specified map based on the given predicate applied to key-value pairs,
     * collecting the matching entries into a map created by the provided supplier.
     * The predicate receives both the key and value as separate parameters.
     *
     * <p>Unlike {@link #filter(Map, BiPredicate)}, which reflectively mirrors the input map's type, this
     * overload lets the caller control the result map type via {@code mapSupplier} (the size-aware
     * {@link IntFunction} convention used by {@link #zip(Iterable, Iterable, IntFunction)}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("apple", 5);
     * map.put("banana", 2);
     * map.put("cherry", 8);
     *
     * // Collect matches into a LinkedHashMap
     * LinkedHashMap<String, Integer> filtered = Maps.filter(map, (key, value) -> value > 4, LinkedHashMap::new);
     * // filtered: {apple=5, cherry=8}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param <M> the type of the resulting map.
     * @param map the map to be filtered.
     * @param predicate the predicate used to filter the entries
     * @param mapSupplier a function that creates a new Map instance given an expected size
     * @return a new map (created by {@code mapSupplier}) containing only the entries that match the predicate;
     *         an empty map (from {@code mapSupplier.apply(0)}) if {@code map} is {@code null}.
     * @throws IllegalArgumentException if {@code predicate} or {@code mapSupplier} is {@code null}, or if
     *         {@code mapSupplier} returns {@code null}.
     * @see #filter(Map, BiPredicate)
     */
    public static <K, V, M extends Map<K, V>> M filter(final Map<K, V> map, final BiPredicate<? super K, ? super V> predicate,
            final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        if (map == null) {
            return N.checkArgNotNull(mapSupplier.apply(0), "mapSupplier returned null");
        }

        final M result = N.checkArgNotNull(mapSupplier.apply(map.size()), "mapSupplier returned null");

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the entries of the specified map based on the given key predicate.
     * This method creates a new map containing only the entries whose keys satisfy the predicate condition.
     * The returned map is of the same type as the input map if possible.
     *
     * <p>This method leaves the input map unchanged. To remove entries with matching keys from the
     * supplied map in place, use {@link #removeIfKey(Map, Predicate)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("apple", 1);
     * map.put("banana", 2);
     * map.put("apricot", 3);
     *
     * Map<String, Integer> filtered = Maps.filterByKey(map, key -> key.startsWith("ap"));
     * // filtered: {apple=1, apricot=3}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map to be filtered.
     * @param predicate the predicate used to filter the keys.
     * @return a new map containing only the entries with keys that match the predicate.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #removeIfKey(Map, Predicate)
     * @see N#filter(Iterable, Predicate)
     * @see Iterators#filter(Iterator, Predicate)
     */
    public static <K, V> Map<K, V> filterByKey(final Map<K, V> map, final Predicate<? super K> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, map.size());

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Filters the entries of the specified map based on the given value predicate.
     * This method creates a new map containing only the entries whose values satisfy the predicate condition.
     * The returned map is of the same type as the input map if possible.
     *
     * <p>This method leaves the input map unchanged. To remove entries with matching values from the
     * supplied map in place, use {@link #removeIfValue(Map, Predicate)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a", 10);
     * map.put("b", 20);
     * map.put("c", 30);
     *
     * Map<String, Integer> filtered = Maps.filterByValue(map, value -> value >= 20);
     * // filtered: {b=20, c=30}
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map.
     * @param <V> the type of mapped values.
     * @param map the map to be filtered.
     * @param predicate the predicate used to filter the values.
     * @return a new map containing only the entries with values that match the predicate.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see #removeIfValue(Map, Predicate)
     * @see N#filter(Iterable, Predicate)
     * @see Iterators#filter(Iterator, Predicate)
     */
    public static <K, V> Map<K, V> filterByValue(final Map<K, V> map, final Predicate<? super V> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        if (map == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = newTargetMap(map, map.size());

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Inverts the given map by swapping its keys with its values.
     * The resulting map's keys are the input map's values and its values are the input map's keys.
     * Note: This method does not check for duplicate values in the input map. If there are duplicate values,
     * some information may be lost in the inversion process as each value in the resulting map must be unique.
     * Which entry survives is decided by {@code map}'s iteration order (the last one encountered wins), so for
     * an unordered map such as {@link java.util.HashMap} it is unspecified.
     *
     * <p>The verb <i>invert</i> denotes a copy-producing transformation: the returned map is
     * independent of the input map. In contrast, {@link BiMap#inverse()} returns a live view backed
     * by the same mappings.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = new HashMap<>();
     * map.put("Alice", 1);
     * map.put("Bob", 2);
     * map.put("Charlie", 3);
     *
     * // inverted = {1=Alice, 2=Bob, 3=Charlie}
     * Map<Integer, String> inverted = Maps.invert(map);
     * // Example with duplicate values: the LAST value encountered wins. Use a LinkedHashMap when that
     * // matters - "last" is only meaningful for a map with a defined iteration order.
     * Map<String, String> map2 = new LinkedHashMap<>();
     * map2.put("key1", "valueA");
     * map2.put("key2", "valueA");
     *
     * Map<String, String> inverted2 = Maps.invert(map2);
     * // inverted2 = {valueA=key2} (key1 was overwritten)
     * }</pre>
     *
     * @param <K> the key type of the input map and the value type of the resulting map.
     * @param <V> the value type of the input map and the key type of the resulting map.
     * @param map the map to be inverted.
     * @return a new map which is the inverted version of the input map.
     */
    public static <K, V> Map<V, K> invert(final Map<K, V> map) {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, K> result = newOrderingMap(map);

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }

        return result;
    }

    /**
     * Inverts the given map by swapping its keys with its values.
     * The resulting map's keys are the input map's values and its values are the input map's keys.
     * If there are duplicate values in the input map, the merging operation specified by mergeFunction is applied.
     *
     * <p>Like {@link #invert(Map)}, this is a copy-producing transformation; the returned map is
     * independent of the input. It is not an {@link BiMap#inverse() inverse view}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // A LinkedHashMap, so that "first" and "last" below are actually defined: over a HashMap the
     * // encounter order - and therefore which key the merge function sees first - is unspecified.
     * Map<String, String> map = new LinkedHashMap<>();
     * map.put("key1", "valueA");
     * map.put("key2", "valueA");
     * map.put("key3", "valueB");
     * // Use the first key when there are duplicates
     * // inverted1 = {valueA=key1, valueB=key3}
     * Map<String, String> inverted1 = Maps.invert(map, (oldKey, newKey) -> oldKey);
     * // Use the last key when there are duplicates
     * // inverted2 = {valueA=key2, valueB=key3}
     * Map<String, String> inverted2 = Maps.invert(map, (oldKey, newKey) -> newKey);
     * // Concatenate keys when there are duplicates
     * Map<String, String> inverted3 = Maps.invert(map, (oldKey, newKey) -> oldKey + "," + newKey);
     * // inverted3 = {valueA=key1,key2, valueB=key3}
     * }</pre>
     *
     * @param <K> the key type of the input map and the value type of the resulting map.
     * @param <V> the value type of the input map and the key type of the resulting map.
     * @param map the map to be inverted.
     * @param mergeFunction the merging operation to be applied if there are duplicate values in the input map.
     * @return a new map which is the inverted version of the input map.
     * @throws IllegalArgumentException if {@code mergeFunction} is {@code null}.
     */
    public static <K, V> Map<V, K> invert(final Map<K, V> map, final BiFunction<? super K, ? super K, ? extends K> mergeFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(mergeFunction, cs.mergeFunction);

        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, K> result = newOrderingMap(map);
        K oldVal = null;

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            oldVal = result.get(entry.getValue());

            if (oldVal != null || result.containsKey(entry.getValue())) {
                result.put(entry.getValue(), mergeFunction.apply(oldVal, entry.getKey()));
            } else {
                result.put(entry.getValue(), entry.getKey());
            }
        }

        return result;
    }

    /**
     * Inverts the given map by mapping each value in the Collection to the corresponding key.
     * The resulting map's keys are the values in the Collections of the input map, and its values are Lists of the corresponding keys from the input map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<Integer>> map = new LinkedHashMap<>();
     * map.put("Alice", Arrays.asList(1, 2, 3));
     * map.put("Bob", Arrays.asList(2, 4));
     * map.put("Charlie", Arrays.asList(3, 5));
     *
     * // inverted = {1=[Alice], 2=[Alice, Bob], 3=[Alice, Charlie], 4=[Bob], 5=[Charlie]}
     * Map<Integer, List<String>> inverted = Maps.flatInvert(map);
     * // Each value from the collections becomes a key, mapping to all original keys that contained it
     * }</pre>
     *
     * <p>A {@code null} or empty {@code Collection} value contributes nothing to the result, so its key
     * simply disappears. A {@code null} <em>element</em> inside a collection is not skipped: it becomes a
     * {@code null} key in the result, which throws {@link NullPointerException} if the result map's type
     * rejects {@code null} keys.</p>
     *
     * @param <K> the key type of the input map and the element type of the List values in the resulting map.
     * @param <V> the element type of the Collection values in the input map and the key type of the resulting map.
     * @param map the map to be inverted; may be {@code null}, which yields an empty map.
     * @return a new map which is the inverted version of the input map.
     */
    public static <K, V> Map<V, List<K>> flatInvert(final Map<K, ? extends Collection<? extends V>> map) {
        if (map == null) {
            return new HashMap<>();
        }

        final Map<V, List<K>> result = newOrderingMap(map);

        for (final Map.Entry<K, ? extends Collection<? extends V>> entry : map.entrySet()) {
            final Collection<? extends V> c = entry.getValue();

            if (N.notEmpty(c)) {
                for (final V v : c) {
                    List<K> list = result.computeIfAbsent(v, k -> new ArrayList<>());

                    list.add(entry.getKey());
                }
            }
        }

        return result;
    }

    /**
     * Transforms a map of collections into a list of maps.
     * Each resulting map is a "flat" representation of the original map's entries, where each key in the original map
     * is associated with one element from its corresponding collection.
     * The transformation is done in a way that the first map in the resulting list contains the first elements of all collections,
     * the second map contains the second elements, and so on.
     * If the collections in the original map are of different sizes, the resulting list's size is equal to the size of the largest collection.
     *
     * <p><b>Note:</b> The result is a {@code List} of maps, not a single map - this method
     * <em>transposes</em> a map of "columns" into a list of "rows". Contrast with
     * {@link #flatten(Map)}, which does return a single flattened map.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<Integer>> map = new HashMap<>();
     * map.put("a", Arrays.asList(1, 2, 3));
     * map.put("b", Arrays.asList(4, 5, 6));
     * map.put("c", Arrays.asList(7, 8));
     *
     * List<Map<String, Integer>> result = Maps.transpose(map);
     * // result contains:
     * // [{a=1, b=4, c=7}, {a=2, b=5, c=8}, {a=3, b=6}]
     * }</pre>
     *
     * @param <K> the type of keys in the input map and the resulting maps.
     * @param <V> the type of values in the collections of the input map and the values in the resulting maps.
     * @param map the input map, where each key is associated with a collection of values.
     * @return a list of maps, where each map represents a "flat" version of the original map's entries.
     */
    public static <K, V> List<Map<K, V>> transpose(final Map<K, ? extends Collection<? extends V>> map) {
        if (map == null) {
            return new ArrayList<>();
        }

        int maxValueSize = 0;

        for (final Collection<? extends V> v : map.values()) {
            maxValueSize = N.max(maxValueSize, N.size(v));
        }

        final List<Map<K, V>> result = new ArrayList<>(maxValueSize);

        for (int i = 0; i < maxValueSize; i++) {
            result.add(newTargetMap(map));
        }

        K key = null;
        Iterator<? extends V> iter = null;

        for (final Map.Entry<K, ? extends Collection<? extends V>> entry : map.entrySet()) {
            if (N.isEmpty(entry.getValue())) {
                continue;
            }

            key = entry.getKey();
            iter = entry.getValue().iterator();

            for (int i = 0; iter.hasNext(); i++) {
                result.get(i).put(key, iter.next());
            }
        }

        return result;
    }

    /**
     * Flattens the given map.
     * This method takes a map where some values may be other maps and returns a new map where all nested maps are flattened into the top-level map.
     * The keys of the flattened map are the keys of the original map and the keys of any nested maps, concatenated with a dot.
     * Note: This method does not modify the original map.
     * Empty nested maps do not produce an entry. For a reversible flatten/unflatten round trip,
     * choose a delimiter that does not occur in any input key; delimiters are not escaped.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new LinkedHashMap<>();
     * map.put("name", "John");
     * Map<String, Object> address = new LinkedHashMap<>();
     * address.put("city", "New York");
     * address.put("zip", "10001");
     * map.put("address", address);
     *
     * Map<String, Object> flattened = Maps.flatten(map);
     * // flattened contains {name=John, address.city=New York, address.zip=10001}
     * // (entry order may vary - the returned map is a HashMap)
     * }</pre>
     *
     * @param map the map to be flattened; may be {@code null}, which yields an empty result.
     * @return a new map which is the flattened version of the input map.
     * @throws IllegalArgumentException if a key (at any level) is {@code null}, a cyclic map structure is
     *         encountered, or two input paths produce the same flattened key.
     * @throws ClassCastException if a nested map's key is not a {@code String} (possible only when a raw or
     *         unchecked map has been stored as a value).
     */
    public static Map<String, Object> flatten(final Map<String, Object> map) throws IllegalArgumentException, ClassCastException {
        return flatten(map, IntFunctions.ofMap());
    }

    /**
     * Flattens the given map using a provided map supplier.
     * This method takes a map where some values may be other maps and returns a new map where all nested maps are flattened into the top-level map.
     * The keys of the flattened map are the keys of the original map and the keys of any nested maps, concatenated with a dot.
     * Note: This method does not modify the original map.
     * Empty nested maps do not produce an entry. For a reversible flatten/unflatten round trip,
     * choose a delimiter that does not occur in any input key; delimiters are not escaped.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new LinkedHashMap<>();
     * map.put("name", "John");
     * Map<String, Object> address = new LinkedHashMap<>();
     * address.put("city", "New York");
     * address.put("zip", "10001");
     * map.put("address", address);
     * // Use a LinkedHashMap to preserve insertion order
     * LinkedHashMap<String, Object> flattened = Maps.flatten(map, LinkedHashMap::new);
     * // flattened = {name=John, address.city=New York, address.zip=10001}
     * }</pre>
     *
     * @param <M> the type of the map to be returned. It extends the Map with String keys and Object values.
     * @param map the map to be flattened; may be {@code null}, which yields an empty result.
     * @param mapSupplier a function that creates a new Map instance given an expected size. It is invoked
     *        exactly once - the whole result is flat, so no nested maps are created.
     * @return a new map which is the flattened version of the input map.
     * @throws IllegalArgumentException if {@code mapSupplier} returns {@code null}, or if a key (at any level) is
     *         {@code null}, a cyclic map structure is encountered, or two input paths produce the same flattened
     *         key.
     * @throws ClassCastException if a nested map's key is not a {@code String} (possible only when a raw or
     *         unchecked map has been stored as a value).
     */
    public static <M extends Map<String, Object>> M flatten(final Map<String, Object> map, final IntFunction<? extends M> mapSupplier)
            throws IllegalArgumentException, ClassCastException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        return flatten(map, ".", mapSupplier);
    }

    /**
     * Flattens the given map using a provided map supplier and a delimiter.
     * This method takes a map where some values may be other maps and returns a new map where all nested maps are flattened into the top-level map.
     * The keys of the flattened map are the keys of the original map and the keys of any nested maps, concatenated with a provided delimiter.
     * Note: This method does not modify the original map.
     * Empty nested maps do not produce an entry. For a reversible flatten/unflatten round trip,
     * {@code delimiter} must not occur in any input key; delimiters are not escaped.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new LinkedHashMap<>();
     * map.put("name", "John");
     * Map<String, Object> address = new LinkedHashMap<>();
     * address.put("city", "New York");
     * address.put("zip", "10001");
     * map.put("address", address);
     * // Use underscore as delimiter instead of dot
     * Map<String, Object> flattened = Maps.flatten(map, "_", LinkedHashMap::new);
     * // flattened = {name=John, address_city=New York, address_zip=10001}
     * }</pre>
     *
     * @param <M> the type of the map to be returned. It extends the Map with String keys and Object values.
     * @param map the map to be flattened; may be {@code null}, which yields an empty result.
     * @param delimiter the non-empty delimiter to be used when concatenating keys.
     * @param mapSupplier a function that creates a new Map instance given an expected size. It is invoked
     *        exactly once - the whole result is flat, so no nested maps are created.
     * @return a new map which is the flattened version of the input map.
     * @throws IllegalArgumentException if {@code mapSupplier} returns {@code null}, or if {@code delimiter} is
     *         {@code null} or empty, a key (at any level) is {@code null}, a cyclic map structure is encountered, or
     *         two input paths produce the same flattened key.
     * @throws ClassCastException if a nested map's key is not a {@code String} (possible only when a raw or
     *         unchecked map has been stored as a value).
     */
    public static <M extends Map<String, Object>> M flatten(final Map<String, Object> map, final String delimiter, final IntFunction<? extends M> mapSupplier)
            throws IllegalArgumentException, ClassCastException {
        N.checkArgNotEmpty(delimiter, cs.delimiter);
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        final M result = N.checkArgNotNull(mapSupplier.apply(N.size(map)), "mapSupplier returned null");

        if (result == map) {
            throw new IllegalArgumentException("mapSupplier must create a new map");
        }

        flatten(map, null, delimiter, result, new IdentityHashMap<>());

        return result;
    }

    /**
     * @throws IllegalArgumentException if the map contains a cyclic nested map, a null key, or duplicate flattened keys.
     */
    private static void flatten(final Map<String, Object> map, final String prefix, final String delimiter, final Map<String, Object> output,
            final IdentityHashMap<Map<?, ?>, Boolean> ancestors) throws IllegalArgumentException {
        if (N.isEmpty(map)) {
            return;
        }

        if (ancestors.put(map, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Cyclic map structure cannot be flattened");
        }

        try {
            // null is the root sentinel. An empty string is a valid nested key and must retain its
            // path segment (for example, {"": {"a": 1}} flattens to {".a": 1}).
            if (prefix == null) {
                for (final Map.Entry<String, Object> entry : map.entrySet()) {
                    if (entry.getKey() == null) {
                        throw new IllegalArgumentException("Map keys must not be null");
                    }

                    if (entry.getValue() instanceof Map) {
                        flatten((Map<String, Object>) entry.getValue(), entry.getKey(), delimiter, output, ancestors);
                    } else {
                        putFlattened(output, entry.getKey(), entry.getValue());
                    }
                }
            } else {
                for (final Map.Entry<String, Object> entry : map.entrySet()) {
                    if (entry.getKey() == null) {
                        throw new IllegalArgumentException("Map keys must not be null");
                    }

                    if (entry.getValue() instanceof Map) {
                        flatten((Map<String, Object>) entry.getValue(), prefix + delimiter + entry.getKey(), delimiter, output, ancestors);
                    } else {
                        putFlattened(output, prefix + delimiter + entry.getKey(), entry.getValue());
                    }
                }
            }
        } finally {
            ancestors.remove(map);
        }
    }

    /**
     * @throws IllegalArgumentException if {@code output} already contains the flattened key.
     */
    private static void putFlattened(final Map<String, Object> output, final String key, final Object value) throws IllegalArgumentException {
        if (output.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate flattened key: " + key);
        }

        output.put(key, value);
    }

    /**
     * Unflattens the given map.
     * This method takes a flattened map where keys are concatenated with a dot and returns a new map where all keys are nested as per their original structure.
     * Note: This method does not modify the original map.
     * Delimiters in keys are always interpreted as path separators; there is no escape syntax.
     * Map-valued input entries remain leaf values and are not merged with entries whose keys extend
     * the same path; such a leaf/prefix conflict is rejected regardless of input order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> flattened = new LinkedHashMap<>();
     * flattened.put("name", "John");
     * flattened.put("address.city", "New York");
     * flattened.put("address.zip", "10001");
     *
     * Map<String, Object> unflattened = Maps.unflatten(flattened);
     * // unflattened contains {name=John, address={city=New York, zip=10001}}
     * // (entry order may vary - the returned/nested maps are HashMaps)
     * }</pre>
     *
     * @param map the flattened map to be unflattened; may be {@code null}, which yields an empty result.
     * @return a new map which is the unflattened version of the input map.
     * @throws IllegalArgumentException if a key is {@code null} or flat keys conflict (for example, both {@code "a"}
     *         and {@code "a.b"} are present).
     */
    public static Map<String, Object> unflatten(final Map<String, Object> map) throws IllegalArgumentException {
        return unflatten(map, IntFunctions.ofMap());
    }

    /**
     * Unflattens the given map using a provided map supplier.
     * This method takes a flattened map where keys are concatenated with a delimiter and returns a new map where all keys are nested as per their original structure.
     * Note: This method does not modify the original map.
     * Delimiters in keys are always interpreted as path separators; there is no escape syntax.
     * Map-valued input entries remain leaf values and are not merged with entries whose keys extend
     * the same path; such a leaf/prefix conflict is rejected regardless of input order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> flattened = new LinkedHashMap<>();
     * flattened.put("name", "John");
     * flattened.put("address.city", "New York");
     * flattened.put("address.zip", "10001");
     * // Use a LinkedHashMap to preserve insertion order
     * LinkedHashMap<String, Object> unflattened = Maps.unflatten(flattened, LinkedHashMap::new);
     * // unflattened = {name=John, address={city=New York, zip=10001}}
     * }</pre>
     *
     * @param <M> the type of the map to be returned. It extends the Map with String keys and Object values.
     * @param map the flattened map to be unflattened; may be {@code null}, which yields an empty result.
     * @param mapSupplier a function that creates a new Map instance given an expected size;
     *        it must return a distinct map on every invocation.
     * @return a new map which is the unflattened version of the input map.
     * @throws IllegalArgumentException if {@code mapSupplier} returns {@code null}, a key is {@code null}, or flat
     *         keys conflict (for example, both {@code "a"} and {@code "a.b"} are present).
     */
    public static <M extends Map<String, Object>> M unflatten(final Map<String, Object> map, final IntFunction<? extends M> mapSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        return unflatten(map, ".", mapSupplier);
    }

    /**
     * Unflattens the given map using a provided map supplier and a delimiter.
     * This method takes a flattened map where keys are concatenated with a specified delimiter and returns a new map where all keys are nested as per their original structure.
     * Note: This method does not modify the original map.
     * Delimiters in keys are always interpreted as path separators; there is no escape syntax.
     * Map-valued input entries remain leaf values and are not merged with entries whose keys extend
     * the same path; such a leaf/prefix conflict is rejected regardless of input order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> flattened = new LinkedHashMap<>();
     * flattened.put("name", "John");
     * flattened.put("address_city", "New York");
     * flattened.put("address_zip", "10001");
     * // Use underscore as delimiter
     * Map<String, Object> unflattened = Maps.unflatten(flattened, "_", LinkedHashMap::new);
     * // unflattened = {name=John, address={city=New York, zip=10001}}
     * }</pre>
     *
     * @param <M> the type of the map to be returned. It extends the Map with String keys and Object values.
     * @param map the flattened map to be unflattened; may be {@code null}, which yields an empty result.
     * @param delimiter the non-empty delimiter that was used in the flattening process to concatenate keys.
     * @param mapSupplier a function that creates a new Map instance given an expected size;
     *        it must return a distinct map on every invocation.
     * @return a new map which is the unflattened version of the input map. Keys without the delimiter
     *         are copied as-is; no error is raised when the delimiter is absent.
     * @throws IllegalArgumentException if {@code mapSupplier} returns {@code null}, {@code delimiter} is
     *         {@code null} or empty, a key is {@code null}, or flat keys conflict (for example, both {@code "a"} and
     *         {@code "a.b"} are present).
     */
    public static <M extends Map<String, Object>> M unflatten(final Map<String, Object> map, final String delimiter, final IntFunction<? extends M> mapSupplier)
            throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, cs.delimiter);
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        final IdentityHashMap<Map<String, Object>, Boolean> suppliedMaps = new IdentityHashMap<>();

        if (map != null) {
            suppliedMaps.put(map, Boolean.TRUE);
        }

        final M result = newUnflattenMap(mapSupplier, N.size(map), suppliedMaps);

        final Splitter keySplitter = Splitter.with(delimiter);

        if (N.notEmpty(map)) {
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                final String flatKey = entry.getKey();

                if (flatKey == null) {
                    throw new IllegalArgumentException("Map keys must not be null");
                }

                if (flatKey.contains(delimiter)) {
                    final String[] keys = keySplitter.splitToArray(flatKey);
                    Map<String, Object> lastMap = result;

                    for (int i = 0, to = keys.length - 1; i < to; i++) {
                        final Object existingValue = lastMap.get(keys[i]);
                        Map<String, Object> tmp;

                        if (existingValue == null && !lastMap.containsKey(keys[i])) {
                            tmp = newUnflattenMap(mapSupplier, 0, suppliedMaps);
                            lastMap.put(keys[i], tmp);
                        } else if (existingValue instanceof Map && existingValue != map && suppliedMaps.containsKey(existingValue)) {
                            // Only maps created for path prefixes are branches; a map-valued input leaf still conflicts.
                            tmp = (Map<String, Object>) existingValue;
                        } else {
                            throw new IllegalArgumentException("Conflicting flat key prefix: " + keys[i]);
                        }

                        lastMap = tmp;
                    }

                    final String lastKey = keys[keys.length - 1];

                    if (lastMap.get(lastKey) instanceof Map) {
                        throw new IllegalArgumentException("Conflicting flat key: " + flatKey);
                    }

                    lastMap.put(lastKey, entry.getValue());
                } else {
                    if (result.get(flatKey) instanceof Map) {
                        throw new IllegalArgumentException("Conflicting flat key: " + flatKey);
                    }

                    result.put(flatKey, entry.getValue());
                }
            }
        }

        return result;
    }

    /**
     * @throws IllegalArgumentException if the supplier returns null, a nonempty map, an already supplied map, or the input map.
     */
    private static <M extends Map<String, Object>> M newUnflattenMap(final IntFunction<? extends M> mapSupplier, final int expectedSize,
            final IdentityHashMap<Map<String, Object>, Boolean> suppliedMaps) throws IllegalArgumentException {
        final M newMap = N.checkArgNotNull(mapSupplier.apply(expectedSize), "mapSupplier returned null");

        if (suppliedMaps.put(newMap, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("mapSupplier must return a distinct new map on every invocation and must not return the input map");
        }

        return newMap;
    }

    /**
     * Replaces (renames) the keys in the specified map by applying the given converter to each existing key.
     *
     * <p>This method operates in two phases:
     * <ol>
     *   <li><b>Validation phase:</b> Computes the converted key for every current key and validates that no duplicates
     *       would be produced. If duplicates are detected, throws {@link IllegalStateException} before modifying the map.</li>
     *   <li><b>Replacement phase:</b> Builds the fully rekeyed map first, then applies it to {@code map} with a single
     *       {@link Map#clear()} followed by {@link Map#putAll(Map)}. Entries are re-inserted in the original
     *       {@link Map#keySet()} order, so an insertion-ordered map such as {@link LinkedHashMap} keeps its original
     *       order; a map that imposes its own order re-derives it from the new keys (a {@link TreeMap} comes back
     *       sorted by the converted keys, and a {@link HashMap}'s order is unspecified as always).</li>
     * </ol>
     *
     * <p><b>Not atomic, even on a concurrent map.</b> The rekeyed copy is built from a snapshot and then
     * applied as two separate operations, so on a map shared with other threads a reader can observe the map
     * empty or only partially refilled in between, and a concurrent write gets one of three unrelated fates
     * depending purely on when it lands: before the {@code clear()} it is wiped; after it, it survives unless
     * the {@code putAll()} happens to overwrite the same key; and either way it is absent from the rekeyed
     * copy, which was snapshotted before any of this began. Rekey a map no other thread is touching, or guard
     * the call externally.</p>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *   <li>If a key converts to itself (i.e., {@code keyConverter.apply(key).equals(key)}), the entry is still
     *       removed and reinserted, which may affect iteration order in some map implementations.</li>
     *   <li>If {@code keyConverter} returns {@code null}, it is treated as a valid key. However, if {@code null}
     *       is returned for multiple keys, an {@link IllegalStateException} is thrown due to duplicate keys.
     *       Additionally, the final {@code map.put(null, value)} behavior depends on whether the map implementation
     *       supports {@code null} keys (e.g., {@link java.util.HashMap} allows {@code null}, but
     *       {@link java.util.concurrent.ConcurrentHashMap} does not).</li>
     *   <li>If the map is empty or {@code null}, this method returns immediately without any action.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert keys to uppercase
     * Map<String, Integer> map = new LinkedHashMap<>();
     * map.put("name", 1);
     * map.put("age", 2);
     * map.put("city", 3);
     *
     * Maps.replaceKeys(map, String::toUpperCase);
     * // map now contains: {NAME=1, AGE=2, CITY=3}
     * // Add prefix to keys
     * Map<String, String> data = new HashMap<>();
     * data.put("id", "123");
     * data.put("type", "user");
     *
     * Maps.replaceKeys(data, k -> "prefix_" + k);
     * // data now contains: {prefix_id=123, prefix_type=user}
     * // This will throw IllegalStateException (duplicate keys):
     * // Maps.replaceKeys(map, k -> "constant"); // All keys convert to same value
     * }</pre>
     *
     * @param <K> the key type
     * @param map the map whose keys are to be replaced; modified in-place. If {@code null} or empty, no action is taken.
     * @param keyConverter the function applied once to each processed key to produce the new key; processing stops
     *        if conversion fails or produces a duplicate
     * @throws IllegalArgumentException if {@code keyConverter} is {@code null}.
     * @throws IllegalStateException if the converted keys contain duplicates (including multiple {@code null} values);
     *         the message identifies both original source keys without invoking the converter again
     * @throws UnsupportedOperationException if {@code map} does not support {@link Map#clear()} or
     *         {@link Map#putAll(Map)}. Failures while applying the validated copy can leave the map empty or partially
     *         populated; only failures during conversion and duplicate detection leave it untouched.
     */
    public static <K> void replaceKeys(final Map<K, ?> map, final Function<? super K, ? extends K> keyConverter)
            throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
        N.checkArgNotNull(keyConverter, cs.keyConverter);

        if (N.isEmpty(map)) {
            return;
        }

        final Map<K, Object> mapToUse = (Map<K, Object>) map;

        // One entrySet() pass, not keySet() + a get() per key: the second lookup doubled the cost for no
        // benefit. The key and the value are copied into separate lists rather than kept as Map.Entry
        // objects, because the entries belong to `map` and are invalidated by the clear() below.
        final List<K> keys = new ArrayList<>(mapToUse.size());
        final List<Object> values = new ArrayList<>(mapToUse.size());

        for (final Map.Entry<K, Object> entry : mapToUse.entrySet()) {
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }

        // Everything below is bounded by what the iteration actually yielded, never by a separately read
        // size(): the two can disagree for a concurrently modified map, and trusting size() would index
        // past the end of `keys`.
        final int size = keys.size();

        // Rebuild into a same-kind temporary map first. Besides validating converted keys before
        // the original map is mutated, using the target map itself for duplicate detection honors
        // comparator-based and identity-based key equivalence rather than assuming equals semantics.
        final Map<K, Object> newMap = newTargetMap(map, size);
        final List<K> convertedKeys = new ArrayList<>(size);
        K newKey = null;

        for (int i = 0; i < size; i++) {
            final K oldKey = keys.get(i);
            newKey = keyConverter.apply(oldKey);

            if (newMap.containsKey(newKey)) {
                throw new IllegalStateException("Duplicate new key: " + newKey + " - produced by both '"
                        + findEarlierSourceKey(newMap, keys, values, convertedKeys, newKey) + "' and '" + oldKey + "'");
            }

            newMap.put(newKey, values.get(i));
            convertedKeys.add(newKey);
        }

        mapToUse.clear();
        mapToUse.putAll(newMap);
    }

    /**
     * Locates a duplicate's earlier source using cached conversions. Rebuilding only the private map
     * preserves its actual key equivalence and value restrictions; the converter is never invoked again.
     */
    private static <K> Object findEarlierSourceKey(final Map<K, Object> newMap, final List<K> keys, final List<Object> values, final List<K> convertedKeys,
            final K duplicateKey) {
        newMap.clear();

        for (int i = 0, size = convertedKeys.size(); i < size; i++) {
            newMap.put(convertedKeys.get(i), values.get(i));

            if (newMap.containsKey(duplicateKey)) {
                return keys.get(i);
            }
        }

        return "?";
    }

    /**
     * Replaces (renames) keys in the specified map by applying the given converter, merging values when
     * multiple original keys map to the same converted key.
     *
     * <p>The method takes a snapshot of the current entries and rebuilds them in the original
     * {@link Map#keySet()} encounter order. For each entry, its converted key is inserted into a
     * compatible temporary map. If that map already contains the converted key (according to the
     * map implementation's own key semantics), {@code mergeFunction} combines the existing value
     * (first argument) and incoming value (second argument). Because every converted key is computed
     * from the original snapshot, chained renames (e.g. {@code "a" -> "b"} while {@code "b" -> "c"})
     * do not cascade.</p>
     *
     * <p><b>Difference from {@link #replaceKeys(Map, Function)}:</b> This method allows duplicate converted
     * keys by merging their values, whereas the single-argument version throws {@link IllegalStateException}
     * on duplicates.</p>
     *
     * <p><b>Not atomic, even on a concurrent map</b> - the rekeyed copy is applied with a {@link Map#clear()}
     * followed by a {@link Map#putAll(Map)}; see {@link #replaceKeys(Map, Function)} for what a concurrent
     * reader or writer can observe in between.</p>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *   <li>The conversion order follows the iteration order of {@code map.keySet()} at the time this method
     *       is called. When multiple keys convert to the same new key, merges occur in that iteration order.</li>
     *   <li>If {@code keyConverter} returns {@code null} for any key, behavior depends on the map implementation.
     *       Maps that allow {@code null} keys (e.g., {@link java.util.HashMap}) will accept it; others
     *       (e.g., {@link java.util.concurrent.ConcurrentHashMap}) will throw {@link NullPointerException}.</li>
     *   <li>If {@code mergeFunction} returns {@code null}, the entry accumulated so far under that converted key
     *       is removed - the same rule {@link Map#merge(Object, Object, BiFunction)} and
     *       {@link #zip(Iterable, Iterable, BiFunction, IntFunction)} follow. The removal is <b>not final</b>:
     *       entries are folded one at a time, so a later entry that converts to the same key finds it absent
     *       and is stored directly, <em>without</em> {@code mergeFunction} being called for it. A key is
     *       therefore missing from the result only when its <em>last</em> collision resolved to {@code null}.
     *       For {@code {a1=1, a2=2, a3=3}} all converting to {@code "a"}, a merger that answers {@code null}
     *       for {@code (1, 2)} leaves {@code {a=3}}, not {@code {}}. Return a sentinel value and strip it
     *       afterwards when a dropped key must stay dropped.</li>
     *   <li>Unlike {@link Map#merge(Object, Object, BiFunction)}, {@code mergeFunction} is invoked based on key
     *       <em>presence</em>: it is called even when the existing value under the new key, or the value
     *       being moved, is {@code null} (the corresponding argument is then {@code null}).</li>
     *   <li>If the map is empty or {@code null}, this method returns immediately without any action.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge values when keys collide (sum integers)
     * Map<String, Integer> map = new HashMap<>();
     * map.put("a1", 10);
     * map.put("a2", 20);
     * map.put("b1", 30);
     *
     * // Explanation: "a1" (10) and "a2" (20) both map to "a", merged via sum -> 30
     * Maps.replaceKeys(map, k -> k.substring(0, 1), Integer::sum);
     * // map now contains: {a=30, b=30}
     * // Concatenate strings on collision
     * Map<String, String> data = new LinkedHashMap<>();
     * data.put("user_1", "John");
     * data.put("user_2", "Jane");
     * data.put("admin_1", "Bob");
     *
     * Maps.replaceKeys(data, k -> k.split("_")[0], (v1, v2) -> v1 + ", " + v2);
     * // data now contains: {user="John, Jane", admin="Bob"}
     * // Use (existing, incoming) -> existing to keep the first value on a collision.
     * // Use (existing, incoming) -> incoming to keep the last value on a collision.
     * // Returning null removes what has accumulated under the key so far, but a still-later entry
     * // converting to that key re-creates it without consulting the merger:
     * Map<String, Integer> three = new LinkedHashMap<>();
     * three.put("a1", 1);
     * three.put("a2", 2);
     * three.put("a3", 3);
     * Maps.replaceKeys(three, k -> "a", (existing, incoming) -> existing == 1 ? null : existing + incoming);
     * // three now contains: {a=3}   (not {})
     * }</pre>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map whose keys are to be replaced; modified in-place. If {@code null} or empty, no action is taken.
     * @param keyConverter converts each existing key to its replacement key
     * @param mergeFunction merges values when multiple entries map to the same converted key. The function receives
     *        {@code (existingValue, incomingValue)} and returns the merged value. If it returns {@code null},
     *        the entry is removed.
     * @throws IllegalArgumentException if any of {@code keyConverter}, {@code mergeFunction} is {@code null}.
     * @throws UnsupportedOperationException if {@code map} does not support {@link Map#clear()} or
     *         {@link Map#putAll(Map)}. Failures while applying the validated copy can leave the map empty or
     *         partially populated - the copy is built first, so only failures during conversion and merging
     *         leave the map untouched.
     * @see #replaceKeys(Map, Function)
     */
    public static <K, V> void replaceKeys(final Map<K, V> map, final Function<? super K, ? extends K> keyConverter,
            final BiFunction<? super V, ? super V, ? extends V> mergeFunction) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(keyConverter, cs.keyConverter);
        N.checkArgNotNull(mergeFunction, cs.mergeFunction);

        if (N.isEmpty(map)) {
            return;
        }

        // One entrySet() pass - see replaceKeys(Map, Function). Copied into separate lists rather than kept
        // as Map.Entry objects, which belong to `map` and are invalidated by the clear() below.
        final List<K> keys = new ArrayList<>(map.size());
        final List<V> values = new ArrayList<>(map.size());

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }

        // Rebuilding in encounter order is important: an unchanged target must not be seeded ahead
        // of an earlier entry that converts to that target. The temporary map also applies the same
        // comparator/identity key semantics as the original map and validates keys before mutation.
        final Map<K, V> newMap = newTargetMap(map, map.size());
        K newKey = null;

        for (int i = 0, size = keys.size(); i < size; i++) {
            newKey = keyConverter.apply(keys.get(i));
            final V value = values.get(i);

            if (newMap.containsKey(newKey)) {
                final V merged = mergeFunction.apply(newMap.get(newKey), value);

                if (merged == null) {
                    newMap.remove(newKey);
                } else {
                    newMap.put(newKey, merged);
                }
            } else {
                newMap.put(newKey, value);
            }
        }

        map.clear();
        map.putAll(newMap);
    }

    /**
     * Converts the keys in the provided map from snake_case (or other delimited formats) to camelCase.
     * This method modifies the map in-place by delegating to {@link #replaceKeys(Map, Function)}
     * with {@link Fn#toCamelCase()}.
     *
     * <p>The conversion splits keys on underscore, hyphen, and whitespace delimiters (and on
     * case boundaries, e.g. {@code "fooBar"}), then joins them in camelCase format where the first word
     * is lowercase and subsequent words start with an uppercase letter.</p>
     *
     * <p><b>Conversion Examples:</b></p>
     * <ul>
     *   <li>{@code "user_name"} -&gt; {@code "userName"}</li>
     *   <li>{@code "first_name"} -&gt; {@code "firstName"}</li>
     *   <li>{@code "USER_NAME"} -&gt; {@code "userName"}</li>
     *   <li>{@code "user-name"} -&gt; {@code "userName"}</li>
     *   <li>{@code "userName"} -&gt; {@code "userName"} (already camelCase, unchanged)</li>
     *   <li>{@code "XMLParser"} -&gt; {@code "xmlParser"} (split at the case boundary)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert snake_case keys to camelCase
     * Map<String, Object> map = new LinkedHashMap<>();
     * map.put("user_name", "John");
     * map.put("first_name", "Jane");
     * map.put("email_address", "john@example.com");
     *
     * Maps.replaceKeysWithCamelCase(map);
     * // map now contains: {userName=John, firstName=Jane, emailAddress=john@example.com}
     * // Useful when converting database column names to Java property names
     * Map<String, Object> dbRow = queryRow("SELECT user_id, created_at FROM users");
     * Maps.replaceKeysWithCamelCase(dbRow); // dbRow contains {userId=..., createdAt=...}
     * }</pre>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *   <li>If the map is {@code null} or empty, this method returns immediately without any action.</li>
     *   <li>Keys that are already in camelCase are left unchanged. A delimiter-free key is not automatically
     *       left alone, though: it is still split at case boundaries and lowercased, so
     *       {@code "XMLParser"} becomes {@code "xmlParser"} and {@code "UserName"} becomes {@code "userName"}.</li>
     *   <li>If multiple keys convert to the same camelCase key, an {@link IllegalStateException} is thrown
     *       before any modifications are made (e.g., {@code "user_name"} and {@code "USER_NAME"} both
     *       convert to {@code "userName"}).</li>
     * </ul>
     *
     * @param props the map whose keys are to be converted to camelCase; modified in-place.
     *              If {@code null} or empty, no action is taken.
     * @throws IllegalStateException if the converted keys contain duplicates
     * @see #replaceKeys(Map, Function)
     * @see Fn#toCamelCase()
     * @see Strings#toCamelCase(String)
     * @see #replaceKeysWithSnakeCase(Map)
     * @see #replaceKeysWithScreamingSnakeCase(Map)
     */
    public static void replaceKeysWithCamelCase(final Map<String, ?> props) throws IllegalStateException {
        replaceKeys(props, Fn.toCamelCase());
    }

    /**
     * Converts the keys in the provided map from camelCase to snake_case (lowercase with underscores).
     * This method modifies the map in-place by delegating to {@link #replaceKeys(Map, Function)}
     * with {@link Fn#toSnakeCase()}.
     *
     * <p>The conversion inserts an underscore before each uppercase letter (when preceded by a lowercase
     * letter or followed by a lowercase letter) and converts the entire string to lowercase.</p>
     *
     * <p><b>Conversion Examples:</b></p>
     * <ul>
     *   <li>{@code "userName"} -&gt; {@code "user_name"}</li>
     *   <li>{@code "firstName"} -&gt; {@code "first_name"}</li>
     *   <li>{@code "emailAddress"} -&gt; {@code "email_address"}</li>
     *   <li>{@code "userID"} -&gt; {@code "user_id"}</li>
     *   <li>{@code "XMLParser"} -&gt; {@code "xml_parser"}</li>
     *   <li>{@code "user_name"} -&gt; {@code "user_name"} (already snake_case, unchanged)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert camelCase keys to snake_case
     * Map<String, Object> map = new LinkedHashMap<>();
     * map.put("userName", "John");
     * map.put("firstName", "Jane");
     * map.put("emailAddress", "john@example.com");
     *
     * Maps.replaceKeysWithSnakeCase(map);
     * // map now contains: {user_name=John, first_name=Jane, email_address=john@example.com}
     * // Useful when converting Java bean properties to database column names
     * Map<String, Object> beanProps = Beans.beanToMap(user);
     * Maps.replaceKeysWithSnakeCase(beanProps); // beanProps is ready for database insertion (snake_case keys)
     * }</pre>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *   <li>If the map is {@code null} or empty, this method returns immediately without any action.</li>
     *   <li>Keys that are already in snake_case remain unchanged (no double underscores are introduced).</li>
     *   <li>Consecutive uppercase letters are handled intelligently: {@code "userID"} becomes {@code "user_id"},
     *       not {@code "user_i_d"}.</li>
     *   <li>If multiple keys convert to the same snake_case key, an {@link IllegalStateException} is thrown
     *       before any modifications are made (e.g., {@code "userName"} and {@code "UserName"} both
     *       convert to {@code "user_name"}).</li>
     * </ul>
     *
     * @param props the map whose keys are to be converted to snake_case; modified in-place.
     *              If {@code null} or empty, no action is taken.
     * @throws IllegalStateException if the converted keys contain duplicates
     * @see #replaceKeys(Map, Function)
     * @see Fn#toSnakeCase()
     * @see Strings#toSnakeCase(String)
     * @see #replaceKeysWithCamelCase(Map)
     * @see #replaceKeysWithScreamingSnakeCase(Map)
     */
    public static void replaceKeysWithSnakeCase(final Map<String, ?> props) throws IllegalStateException {
        replaceKeys(props, Fn.toSnakeCase());
    }

    /**
     * Converts the keys in the provided map from camelCase to SCREAMING_SNAKE_CASE (uppercase with underscores).
     * This method modifies the map in-place by delegating to {@link #replaceKeys(Map, Function)}
     * with {@link Fn#toScreamingSnakeCase()}.
     *
     * <p>The conversion inserts an underscore before each uppercase letter (when preceded by a lowercase
     * letter or followed by a lowercase letter) and converts the entire string to uppercase. This naming
     * convention is commonly used for constants in Java and environment variables.</p>
     *
     * <p><b>Conversion Examples:</b></p>
     * <ul>
     *   <li>{@code "userName"} -&gt; {@code "USER_NAME"}</li>
     *   <li>{@code "firstName"} -&gt; {@code "FIRST_NAME"}</li>
     *   <li>{@code "emailAddress"} -&gt; {@code "EMAIL_ADDRESS"}</li>
     *   <li>{@code "userID"} -&gt; {@code "USER_ID"}</li>
     *   <li>{@code "XMLParser"} -&gt; {@code "XML_PARSER"}</li>
     *   <li>{@code "maxRetryCount"} -&gt; {@code "MAX_RETRY_COUNT"}</li>
     *   <li>{@code "USER_NAME"} -&gt; {@code "USER_NAME"} (already SCREAMING_SNAKE_CASE, unchanged)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert camelCase keys to SCREAMING_SNAKE_CASE
     * Map<String, Object> map = new HashMap<>();
     * map.put("userName", "John");
     * map.put("firstName", "Jane");
     * map.put("maxRetryCount", 3);
     *
     * Maps.replaceKeysWithScreamingSnakeCase(map);
     * // map now contains: {USER_NAME=John, FIRST_NAME=Jane, MAX_RETRY_COUNT=3}
     * // Useful when converting configuration properties to environment variable format
     * Map<String, Object> config = loadConfig();
     * Maps.replaceKeysWithScreamingSnakeCase(config); // keys are SCREAMING_SNAKE_CASE for env-variable naming
     * }</pre>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *   <li>If the map is {@code null} or empty, this method returns immediately without any action.</li>
     *   <li>Keys that are already in SCREAMING_SNAKE_CASE remain unchanged.</li>
     *   <li>Consecutive uppercase letters are handled intelligently: {@code "userID"} becomes {@code "USER_ID"},
     *       not {@code "USER_I_D"}.</li>
     *   <li>If multiple keys convert to the same SCREAMING_SNAKE_CASE key, an {@link IllegalStateException}
     *       is thrown before any modifications are made (e.g., {@code "userName"} and {@code "UserName"}
     *       both convert to {@code "USER_NAME"}).</li>
     * </ul>
     *
     * @param props the map whose keys are to be converted to SCREAMING_SNAKE_CASE; modified in-place.
     *              If {@code null} or empty, no action is taken.
     * @throws IllegalStateException if the converted keys contain duplicates
     * @see #replaceKeys(Map, Function)
     * @see Fn#toScreamingSnakeCase()
     * @see Strings#toScreamingSnakeCase(String)
     * @see #replaceKeysWithCamelCase(Map)
     * @see #replaceKeysWithSnakeCase(Map)
     */
    public static void replaceKeysWithScreamingSnakeCase(final Map<String, ?> props) throws IllegalStateException {
        replaceKeys(props, Fn.toScreamingSnakeCase());
    }
}
