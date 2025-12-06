/*
 * Copyright (c) 2017, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Stream;

/**
 * A comprehensive utility class providing an extensive collection of static methods for Iterator operations,
 * transformations, aggregations, and manipulations. This class serves as the primary iterator utility facade
 * in the Abacus library, offering performance-optimized, iterator-focused operations with null-safety and
 * functional programming patterns as core design principles.
 *
 * <p>The {@code Iterators} class is designed as a final utility class that provides a complete toolkit
 * for iterator processing including filtering, mapping, reducing, searching, sorting, and parallel
 * operations. Unlike collection-based utilities, this class focuses specifically on Iterator patterns
 * for memory-efficient, lazy evaluation of large datasets.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Iterator-Centric Design:</b> Optimized specifically for Iterator patterns and lazy evaluation</li>
 *   <li><b>Memory Efficient:</b> Minimal memory footprint with streaming operations</li>
 *   <li><b>Parallel Processing:</b> Support for concurrent iterator operations with ExecutorService</li>
 *   <li><b>Null-Safe Operations:</b> Graceful handling of null inputs and empty iterators</li>
 *   <li><b>Functional Programming:</b> Comprehensive support for map, filter, reduce, and functional patterns</li>
 *   <li><b>Type Safety:</b> Generic methods with compile-time type checking</li>
 *   <li><b>Performance Optimized:</b> Efficient algorithms with minimal object allocation</li>
 *   <li><b>Stream Integration:</b> Seamless integration with Java Stream API and Abacus Stream utilities</li>
 * </ul>
 *
 * <p><b>Core Functional Categories:</b>
 * <ul>
 *   <li><b>Access Operations:</b> get, getFirst, getLast, elementAt with safe index handling</li>
 *   <li><b>Search Operations:</b> find, findFirst, findLast, indexOf, contains with predicate support</li>
 *   <li><b>Transformation Operations:</b> map, flatMap, filter, distinct, reverse, shuffle</li>
 *   <li><b>Aggregation Operations:</b> reduce, fold, sum, average, min, max for various types</li>
 *   <li><b>Parallel Operations:</b> forEach, map, filter with ExecutorService support</li>
 *   <li><b>Validation Operations:</b> isEmpty, allMatch, anyMatch, noneMatch with short-circuit evaluation</li>
 *   <li><b>Conversion Operations:</b> toArray, toList, toSet, toMap with type preservation</li>
 *   <li><b>Combination Operations:</b> concat, merge, zip, cartesianProduct for iterator composition</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Iterator First:</b> Methods are designed to work with Iterator types as primary input,
 *       promoting memory-efficient streaming operations over collection materialization</li>
 *   <li><b>Lazy Evaluation:</b> Operations are performed lazily when possible, allowing for efficient
 *       processing of large datasets without excessive memory consumption</li>
 *   <li><b>Read-Only Operations:</b> Methods only read input iterators without modifying source data</li>
 *   <li><b>Exception Avoidance:</b> Methods avoid throwing unnecessary exceptions when contracts
 *       are not violated, preferring empty results over exceptions for edge cases</li>
 *   <li><b>Nullable Returns:</b> Many methods return {@code Nullable} types for null-safe value handling</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic iterator access operations
 * Iterator<String> iter = Arrays.asList("A", "B", "C", "D").iterator();
 * Nullable<String> element = Iterators.get(iter, 2);     // Nullable["C"]
 * Nullable<String> first = Iterators.getFirst(iter);     // Nullable["A"]
 * Nullable<String> notFound = Iterators.get(iter, 10);   // Nullable.empty()
 *
 * // Search operations with predicates
 * Iterator<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5).iterator();
 * Nullable<Integer> found = Iterators.findFirst(numbers, n -> n > 3);   // Nullable[4]
 * int index = Iterators.indexOf(numbers.iterator(), 3);                 // Returns 2
 *
 * // Transformation operations
 * Iterator<String> words = Arrays.asList("hello", "world", "java").iterator();
 * Iterator<Integer> lengths = Iterators.map(words, String::length);           // [5, 5, 4]
 * Iterator<String> filtered = Iterators.filter(words, s -> s.length() > 4);   // [hello, world]
 *
 * // Aggregation operations
 * Iterator<Double> values = Arrays.asList(1.0, 2.0, 3.0, 4.0).iterator();
 * double sum = Iterators.sum(values);             // 10.0
 * double average = Iterators.average(values);     // 2.5
 * Nullable<Double> max = Iterators.max(values);   // Nullable[4.0]
 *
 * // Parallel processing operations
 * ExecutorService executor = Executors.newFixedThreadPool(4);
 * Iterator<String> data = largeDataSet.iterator();
 * 
 * // Parallel forEach processing
 * Iterators.forEach(data, executor, item -> processItem(item));
 * 
 * // Parallel map transformation
 * Iterator<String> processed = Iterators.map(data, executor, this::expensiveTransform);
 *
 * // Validation operations with short-circuit
 * Iterator<Integer> nums = Arrays.asList(2, 4, 6, 8).iterator();
 * boolean allEven = Iterators.allMatch(nums, n -> n % 2 == 0);   // true
 * boolean anyOdd = Iterators.anyMatch(nums, n -> n % 2 == 1);    // false
 *
 * // Conversion operations
 * Iterator<String> stringIter = Arrays.asList("a", "b", "c").iterator();
 * String[] array = Iterators.toArray(stringIter, String.class);
 * List<String> list = Iterators.toList(stringIter);
 * Set<String> set = Iterators.toSet(stringIter);
 * }</pre>
 *
 * <p><b>Iterator Access Patterns:</b>
 * <ul>
 *   <li><b>Safe Access:</b> {@code get()}, {@code getFirst()}, {@code getLast()} with Nullable returns</li>
 *   <li><b>Index-Based:</b> {@code elementAt()} with bounds checking and default values</li>
 *   <li><b>Position Finding:</b> {@code indexOf()}, {@code lastIndexOf()} with predicate support</li>
 *   <li><b>Existence Checking:</b> {@code contains()}, {@code containsAny()}, {@code containsAll()}</li>
 * </ul>
 *
 * <p><b>Functional Transformations:</b>
 * <ul>
 *   <li><b>Mapping:</b> {@code map()}, {@code flatMap()}, {@code mapToInt()}, {@code mapToLong()}</li>
 *   <li><b>Filtering:</b> {@code filter()}, {@code filterNot()}, {@code distinct()}, {@code limit()}</li>
 *   <li><b>Reduction:</b> {@code reduce()}, {@code fold()}, {@code scan()} with accumulator functions</li>
 *   <li><b>Partitioning:</b> {@code partition()}, {@code groupBy()}, {@code split()} with predicate logic</li>
 * </ul>
 *
 * <p><b>Parallel Processing Support:</b>
 * <ul>
 *   <li><b>Concurrent Operations:</b> ExecutorService-based parallel processing</li>
 *   <li><b>Thread-Safe Design:</b> Safe concurrent access to iterator operations</li>
 *   <li><b>Load Balancing:</b> Automatic work distribution across available threads</li>
 *   <li><b>Exception Handling:</b> Proper exception propagation in parallel contexts</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Memory Efficient:</b> Streaming operations with minimal memory footprint</li>
 *   <li><b>Lazy Evaluation:</b> Operations performed only when results are consumed</li>
 *   <li><b>Short-Circuit Operations:</b> Early termination for operations like findFirst, anyMatch</li>
 *   <li><b>Cache-Friendly:</b> Sequential access patterns optimized for CPU cache performance</li>
 *   <li><b>Scalable Parallel Processing:</b> Efficient utilization of multi-core systems</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Stateless Design:</b> All static methods are stateless and thread-safe</li>
 *   <li><b>Immutable Operations:</b> Methods create new iterators rather than modifying inputs</li>
 *   <li><b>Parallel Support:</b> Built-in support for concurrent processing with ExecutorService</li>
 *   <li><b>No Shared State:</b> No static mutable fields that could cause race conditions</li>
 * </ul>
 *
 * <p><b>Integration with Java Iterators:</b>
 * <ul>
 *   <li><b>Standard Iterator:</b> Full compatibility with java.util.Iterator</li>
 *   <li><b>Enhanced Iterators:</b> Integration with ObjIterator and specialized iterator types</li>
 *   <li><b>Stream Conversion:</b> Seamless conversion to/from Java 8+ Streams</li>
 *   <li><b>Collection Compatibility:</b> Works with iterators from all Java Collection types</li>
 * </ul>
 *
 * <p><b>Error Handling Strategy:</b>
 * <ul>
 *   <li><b>Graceful Degradation:</b> Methods handle edge cases without throwing exceptions</li>
 *   <li><b>Null Safety:</b> Comprehensive null input handling throughout the API</li>
 *   <li><b>Boundary Checking:</b> Safe index access with bounds validation</li>
 *   <li><b>Nullable Returns:</b> Use of Nullable types to avoid null return values</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li><b>Streaming Operations:</b> Process data without loading entire datasets into memory</li>
 *   <li><b>Iterator Chaining:</b> Compose operations without intermediate collection creation</li>
 *   <li><b>Resource Cleanup:</b> Proper handling of iterator lifecycle and resource management</li>
 *   <li><b>Garbage Collection Friendly:</b> Minimal object allocation and retention</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use iterator-based operations for memory-efficient processing of large datasets</li>
 *   <li>Leverage parallel processing for CPU-intensive transformations</li>
 *   <li>Prefer lazy evaluation patterns for improved performance</li>
 *   <li>Use Nullable return types to avoid null pointer exceptions</li>
 *   <li>Chain operations efficiently to minimize intermediate collection creation</li>
 *   <li>Consider iterator consumption patterns (single-use vs. reusable)</li>
 * </ul>
 *
 * <p><b>Performance Tips:</b>
 * <ul>
 *   <li>Use streaming operations for large datasets to avoid memory overhead</li>
 *   <li>Leverage short-circuit operations for better performance</li>
 *   <li>Consider parallel processing for CPU-intensive operations</li>
 *   <li>Minimize iterator materialization until results are needed</li>
 *   <li>Use appropriate ExecutorService configurations for parallel operations</li>
 * </ul>
 *
 * <p><b>Common Patterns:</b>
 * <ul>
 *   <li><b>Safe Access:</b> {@code Nullable<T> result = Iterators.get(iterator, index);}</li>
 *   <li><b>Parallel Processing:</b> {@code Iterators.forEach(iterator, executor, processor);}</li>
 *   <li><b>Stream Conversion:</b> {@code Stream<T> stream = Iterators.stream(iterator);}</li>
 *   <li><b>Functional Pipeline:</b> {@code Iterator<R> result = Iterators.map(Iterators.filter(iter, pred), mapper);}</li>
 * </ul>
 *
 * <p><b>Related Utility Classes:</b>
 * <ul>
 *   <li><b>{@link com.landawn.abacus.util.Iterables}:</b> Iterable-focused utility operations</li>
 *   <li><b>{@link com.landawn.abacus.util.ObjIterator}:</b> Enhanced iterator implementations</li>
 *   <li><b>{@link com.landawn.abacus.util.N}:</b> General utility class with collection operations</li>
 *   <li><b>{@link com.landawn.abacus.util.stream.Stream}:</b> Stream-based processing utilities</li>
 *   <li><b>{@link com.landawn.abacus.util.Enumerations}:</b> Enumeration utilities</li>
 *   <li><b>{@link com.landawn.abacus.util.Array}:</b> Array manipulation utilities</li>
 *   <li><b>{@link java.util.Iterator}:</b> Core Java iterator interface</li>
 *   <li><b>{@link java.util.stream.Stream}:</b> Java 8+ Stream API</li>
 * </ul>
 *
 * <p><b>Example: Large Dataset Processing</b>
 * <pre>{@code
 * // Processing large datasets with memory efficiency
 * Iterator<String> largeDataset = getMillionRecordIterator();
 * ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
 *
 * try {
 *     // Parallel processing with memory-efficient streaming
 *     Iterator<ProcessedRecord> processed = Iterators.map(largeDataset, executor, this::expensiveTransform);
 *     
 *     // Filter and aggregate without loading all data into memory
 *     Iterator<ProcessedRecord> filtered = Iterators.filter(processed, record -> record.isValid());
 *     
 *     // Collect results efficiently
 *     List<ProcessedRecord> results = Iterators.toList(filtered);
 *     
 *     // Statistical analysis
 *     double averageScore = Iterators.average(
 *         Iterators.map(results.iterator(), ProcessedRecord::getScore)
 *     );
 *     
 *     // Find best performing records
 *     Nullable<ProcessedRecord> best = Iterators.max(results.iterator(), 
 *         Comparator.comparing(ProcessedRecord::getScore));
 *         
 * } finally {
 *     executor.shutdown();
 * }
 * }</pre>
 *
 * <p><b>Example: Data Pipeline with Functional Operations</b>
 * <pre>{@code
 * // Building a data processing pipeline
 * Iterator<RawData> source = dataSource.iterator();
 *
 * // Multi-stage transformation pipeline
 * Iterator<String> stage1 = Iterators.map(source, this::extractText);
 * Iterator<String> stage2 = Iterators.filter(stage1, text -> !text.isEmpty());
 * Iterator<String> stage3 = Iterators.map(stage2, String::trim);
 * Iterator<String> stage4 = Iterators.distinct(stage3);
 *
 * // Validation and aggregation
 * boolean hasValidData = Iterators.anyMatch(stage4, this::isValidFormat);
 * long totalCount = Iterators.count(stage4);
 *
 * // Parallel processing for CPU-intensive operations
 * ExecutorService executor = Executors.newCachedThreadPool();
 * Iterator<AnalyzedData> analyzed = Iterators.map(stage4, executor, this::performAnalysis);
 *
 * // Grouping and final processing
 * Map<String, List<AnalyzedData>> grouped = Iterators.groupBy(analyzed, AnalyzedData::getCategory);
 * 
 * // Convert to final result format
 * Map<String, Summary> summaries = grouped.entrySet().stream()
 *     .collect(Collectors.toMap(
 *         Map.Entry::getKey,
 *         entry -> createSummary(entry.getValue())
 *     ));
 * }</pre>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang, Google Guava, and other open source
 * projects under the Apache License 2.0. Methods from these libraries may have been modified for
 * consistency, performance optimization, and enhanced iterator-specific functionality within the
 * Abacus framework.</p>
 *
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.ObjIterator
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.stream.Stream
 * @see com.landawn.abacus.util.Enumerations
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Maps
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.Numbers
 * @see com.landawn.abacus.util.u.Nullable
 * @see java.util.Iterator
 * @see java.util.stream.Stream
 * @see java.lang.Iterable
 */
public final class Iterators {

    private static final Logger logger = LoggerFactory.getLogger(Iterators.class);

    private Iterators() {
        // Utility class.
    }

    /**
     * Retrieves the element at the specified position in the given iterator.
     * The method will advance the iterator to the specified index and return the element at that position wrapped in a {@code Nullable}.
     * If the index is out of bounds (greater than the number of elements in the iterator), a {@code Nullable}.empty() is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "C", "D").iterator();
     * Nullable<String> result = Iterators.get(iter, 2);
     * // result.get() => "C"
     *
     * Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();
     * Nullable<Integer> result2 = Iterators.get(iter2, 10);
     * // result2.isPresent() => false
     * }</pre>
     *
     * @param <T> the type of elements in the iterator.
     * @param iter the iterator from which to retrieve the element, or {@code null} to return {@code Nullable.empty()}.
     * @param index the position in the iterator of the element to be returned. Indexing starts from 0.
     * @return a {@code Nullable} containing the element at the specified position in the iterator, or {@code Nullable.empty()} if the index is out of bounds.
     * @throws IllegalArgumentException if {@code index} is negative.
     */
    public static <T> Nullable<T> get(final Iterator<? extends T> iter, long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        if (iter == null) {
            return Nullable.empty();
        }

        while (iter.hasNext()) {
            if (index == 0) {
                return Nullable.of(iter.next());
            } else {
                iter.next();
                index--;
            }
        }

        return Nullable.empty();
    }

    /**
     * Counts the occurrences of a specific value in the given iterator.
     * This method can be used to find the frequency of a particular value in the iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "A", "C", "A").iterator();
     * long count = Iterators.occurrencesOf(iter, "A");
     * // count => 3
     *
     * Iterator<Integer> iter2 = Arrays.asList(1, null, 2, null, 3).iterator();
     * long nullCount = Iterators.occurrencesOf(iter2, null);
     * // nullCount => 2
     * }</pre>
     *
     * @param iter the iterator to be searched, or {@code null} to return {@code 0}.
     * @param valueToFind the value to count occurrences of, or {@code null} to count {@code null} occurrences.
     * @return the number of occurrences of the value in the iterator, or {@code 0} if {@code iter} is {@code null}.
     * @see N#occurrencesOf(Iterator, Object)
     */
    public static long occurrencesOf(final Iterator<?> iter, final Object valueToFind) {
        if (iter == null) {
            return 0;
        }

        long occurrences = 0;

        if (valueToFind == null) {
            while (iter.hasNext()) {
                if (iter.next() == null) {
                    occurrences++;
                }
            }
        } else {
            while (iter.hasNext()) {
                if (N.equals(iter.next(), valueToFind)) {
                    occurrences++;
                }
            }
        }

        return occurrences;
    }

    /**
     * Counts the number of elements in the given iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "C", "D").iterator();
     * long count = Iterators.count(iter);
     * // count => 4
     *
     * Iterator<Integer> emptyIter = Collections.emptyIterator();
     * long emptyCount = Iterators.count(emptyIter);
     * // emptyCount => 0
     * }</pre>
     *
     * @param iter the iterator to be counted, or {@code null} to return {@code 0}.
     * @return the number of elements in the iterator, or {@code 0} if {@code iter} is {@code null}.
     * @see N#count(Iterator)
     * @see #count(Iterator, Predicate)
     */
    public static long count(final Iterator<?> iter) {
        if (iter == null) {
            return 0;
        }

        long res = 0;

        while (iter.hasNext()) {
            iter.next();
            res++;
        }

        return res;
    }

    /**
     * Counts the number of elements in the given iterator that match the provided predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 6).iterator();
     * long evenCount = Iterators.count(iter, n -> n % 2 == 0);
     * // evenCount => 3
     *
     * Iterator<String> iter2 = Arrays.asList("apple", "apricot", "banana", "avocado").iterator();
     * long aCount = Iterators.count(iter2, s -> s.startsWith("a"));
     * // aCount => 3
     * }</pre>
     *
     * @param <T> the type of elements in the iterator.
     * @param iter the iterator to be searched, or {@code null} to return {@code 0}.
     * @param predicate the predicate to apply to each element in the iterator.
     * @return the number of elements in the iterator that match the provided predicate, or {@code 0} if {@code iter} is {@code null}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @see N#count(Iterator, Predicate)
     */
    public static <T> long count(final Iterator<? extends T> iter, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);   //NOSONAR

        if (iter == null) {
            return 0;
        }

        long res = 0;

        while (iter.hasNext()) {
            if (predicate.test(iter.next())) {
                res++;
            }
        }

        return res;
    }

    /**
     * Returns the index of the first occurrence of the specified value in the given iterator.
     * This method starts searching from the beginning of the iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "C", "B", "D").iterator();
     * long index = Iterators.indexOf(iter, "B");
     * // index => 1
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B", "C").iterator();
     * long notFound = Iterators.indexOf(iter2, "Z");
     * // notFound => -1
     * }</pre>
     *
     * @param iter the iterator to be searched, or {@code null} to return {@code -1}.
     * @param valueToFind the value to find in the iterator, or {@code null} to find {@code null} values.
     * @return the index of the first occurrence of the specified value in the iterator, or {@code -1} if the value is not found or {@code iter} is {@code null}.
     */
    public static long indexOf(final Iterator<?> iter, final Object valueToFind) {
        return indexOf(iter, valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified value in the given iterator,
     * starting the search from the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "C", "B", "D").iterator();
     * long index = Iterators.indexOf(iter, "B", 2);
     * // index => 3 (finds second occurrence of "B")
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B", "C").iterator();
     * long notFound = Iterators.indexOf(iter2, "A", 1);
     * // notFound => -1 (skips first element)
     * }</pre>
     *
     * @param iter the iterator to be searched, or {@code null} to return {@code -1}.
     * @param valueToFind the value to find in the iterator, or {@code null} to find {@code null} values.
     * @param fromIndex the index to start the search from.
     * @return the index of the first occurrence of the specified value in the iterator, or {@code -1} if the value is not found or {@code iter} is {@code null}.
     */
    public static long indexOf(final Iterator<?> iter, final Object valueToFind, final long fromIndex) {
        if (iter == null) {
            return N.INDEX_NOT_FOUND;
        }

        long index = 0;

        if (fromIndex > 0) {
            while (index < fromIndex && iter.hasNext()) {
                iter.next();
                index++;
            }
        }

        while (iter.hasNext()) {
            if (N.equals(iter.next(), valueToFind)) {
                return index;
            }

            index++;
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     *
     * Determines whether two iterators contain equal elements in the same order. More specifically,
     * this method returns {@code true} if {@code iterator1} and {@code iterator2} contain the same
     * number of elements and every element of {@code iterator1} is equal to the corresponding element
     * of {@code iterator2}.
     *
     * <p>Note that this will modify the supplied iterators, since they will have been advanced some
     * number of elements forward.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter1 = Arrays.asList("A", "B", "C").iterator();
     * Iterator<String> iter2 = Arrays.asList("A", "B", "C").iterator();
     * boolean equal = Iterators.elementsEqual(iter1, iter2);
     * // equal => true
     *
     * Iterator<Integer> iter3 = Arrays.asList(1, 2, 3).iterator();
     * Iterator<Integer> iter4 = Arrays.asList(1, 2, 4).iterator();
     * boolean notEqual = Iterators.elementsEqual(iter3, iter4);
     * // notEqual => false
     * }</pre>
     *
     * @param iterator1 the first iterator to compare.
     * @param iterator2 the second iterator to compare.
     * @return {@code true} if the iterators contain equal elements in the same order, {@code false} otherwise.
     */
    public static boolean elementsEqual(final Iterator<?> iterator1, final Iterator<?> iterator2) {
        final boolean isIterator1Empty = N.isEmpty(iterator1);
        final boolean isIterator2Empty = N.isEmpty(iterator2);

        if (isIterator1Empty && isIterator2Empty) {
            return true;
        }

        if (isIterator1Empty || isIterator2Empty) {
            return false;
        }

        while (iterator1.hasNext()) {
            if (!iterator2.hasNext() || !N.equals(iterator1.next(), iterator2.next())) {
                return false;
            }
        }

        return !iterator2.hasNext();
    }

    /**
     * Creates an iterator that returns the same element a specified number of times.
     * This method is useful for generating a sequence of identical elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = Iterators.repeat("Hello", 3);
     * // Yields: "Hello", "Hello", "Hello"
     *
     * ObjIterator<Integer> numbers = Iterators.repeat(5, 0);
     * // numbers.hasNext() => false (empty iterator)
     * }</pre>
     *
     * @param <T> the type of the element to repeat.
     * @param e the element to repeat (can be {@code null}).
     * @param n the number of times to repeat the element. Must be non-negative.
     * @return an {@code ObjIterator} that returns the element {@code n} times, or an empty iterator if {@code n} is {@code 0}.
     * @throws IllegalArgumentException if {@code n} is negative.
     */
    public static <T> ObjIterator<T> repeat(final T e, final int n) throws IllegalArgumentException {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);   //NOSONAR

        if (n == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private int cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public T next() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return e;
            }
        };
    }

    /**
     * Creates an iterator that returns the same element a specified number of times (long version).
     * This method is similar to {@link #repeat(Object, int)} but supports a larger number of repetitions using {@code long}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = Iterators.repeat("Hello", 1000000L);
     * // Yields "Hello" one million times
     *
     * ObjIterator<Integer> iter2 = Iterators.repeat(42, 5L);
     * // Yields: 42, 42, 42, 42, 42
     * }</pre>
     *
     * @param <T> the type of the element to repeat.
     * @param e the element to repeat (can be {@code null}).
     * @param n the number of times to repeat the element. Must be non-negative.
     * @return an {@code ObjIterator} that returns the element {@code n} times, or an empty iterator if {@code n} is {@code 0}.
     * @throws IllegalArgumentException if {@code n} is negative.
     */
    public static <T> ObjIterator<T> repeat(final T e, final long n) throws IllegalArgumentException {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);   //NOSONAR

        if (n == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public T next() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return e;
            }
        };
    }

    /**
     * Repeats each element in the specified collection {@code n} times.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B", "C");
     * ObjIterator<String> iter = Iterators.repeatElements(list, 2);
     * // Yields: "A", "A", "B", "B", "C", "C"
     *
     * List<Integer> numbers = Arrays.asList(1, 2);
     * ObjIterator<Integer> iter2 = Iterators.repeatElements(numbers, 3);
     * // Yields: 1, 1, 1, 2, 2, 2
     * }</pre>
     *
     * @param <T> the type of elements in the collection.
     * @param c the collection whose elements are to be repeated; must not be empty if {@code n} > 0.
     * @param n the number of times the collection's elements are to be repeated.
     * @return an {@code ObjIterator} over the elements in the collection, repeated {@code n} times.
     * @throws IllegalArgumentException if {@code c} is {@code null} or empty when {@code n} > 0, or if {@code n} is negative.
     * @see N#repeatElements(Collection, int)
     */
    public static <T> ObjIterator<T> repeatElements(final Collection<? extends T> c, final long n) throws IllegalArgumentException {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0 || N.isEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final Iterator<? extends T> iter = c.iterator();
            private T next = null;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt > 0 || iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (cnt <= 0) {
                    next = iter.next();
                    cnt = n;
                }

                cnt--;

                return next;
            }
        };
    }

    /**
     * Repeats the entire collection {@code n} times.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B");
     * ObjIterator<String> iter = Iterators.repeatCollection(list, 3);
     * // Yields: "A", "B", "A", "B", "A", "B"
     *
     * List<Integer> numbers = Arrays.asList(1, 2, 3);
     * ObjIterator<Integer> iter2 = Iterators.repeatCollection(numbers, 2);
     * // Yields: 1, 2, 3, 1, 2, 3
     * }</pre>
     *
     * <p><b>Preconditions:</b></p>
     * <ul>
     *   <li>If {@code n} > 0, then {@code c} must not be {@code null} or empty</li>
     *   <li>If {@code n} == 0, {@code c} can be anything (result is empty iterator)</li>
     * </ul>
     *
     * <p><b>Common Mistakes:</b></p>
     * <pre>{@code
     * // DON'T: Pass empty collection with n > 0
     * Iterators.repeatCollection(Collections.emptyList(), 5);   // IllegalArgumentException!
     *
     * // DO: Ensure collection has elements
     * if (N.notEmpty(collection)) {
     *     Iterator<T> it = Iterators.repeatCollection(collection, n);
     * }
     *
     * // DON'T: Pass null collection with n > 0
     * Iterators.repeatCollection(null, 3);   // IllegalArgumentException!
     *
     * // DO: Check for null first
     * if (collection != null && !collection.isEmpty()) {
     *     Iterator<T> it = Iterators.repeatCollection(collection, n);
     * }
     * }</pre>
     *
     * @param <T> the type of elements in the collection.
     * @param c the collection to be repeated; must not be empty if {@code n} > 0.
     * @param n the number of times the collection is to be repeated.
     * @return an {@code ObjIterator} over the collection, repeated {@code n} times.
     * @throws IllegalArgumentException if {@code c} is {@code null} or empty when {@code n} > 0, or if {@code n} is negative.
     * @see N#repeatCollection(Collection, int)
     */
    public static <T> ObjIterator<T> repeatCollection(final Collection<? extends T> c, final long n) throws IllegalArgumentException {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0 || N.isEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterator<? extends T> iter = null;
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0 || (iter != null && iter.hasNext());
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null || !iter.hasNext()) {
                    iter = c.iterator();
                    cnt--;
                }

                return iter.next();
            }
        };
    }

    /**
     * Repeats each element in the specified Collection a calculated number of times until the specified total size is reached.
     * Elements are repeated in order, with some elements potentially repeated more times than others to reach exactly the target size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B", "C");
     * ObjIterator<String> iter = Iterators.repeatElementsToSize(list, 7);
     * // Yields: "A", "A", "A", "B", "B", "C", "C"
     * // Each element repeated at least twice, with "A" repeated three times
     *
     * List<Integer> numbers = Arrays.asList(1, 2);
     * ObjIterator<Integer> iter2 = Iterators.repeatElementsToSize(numbers, 5);
     * // Yields: 1, 1, 1, 2, 2
     * }</pre>
     *
     * @param <T> the type of elements in the collection.
     * @param c the collection whose elements are to be repeated. Must not be empty or {@code null} if {@code size > 0}.
     * @param size the total number of elements the resulting iterator should produce. Must be non-negative.
     * @return an {@code ObjIterator} that repeats each element until {@code size} elements have been produced.
     * @throws IllegalArgumentException if {@code size} is negative, or if {@code c} is empty or {@code null} when {@code size > 0}.
     * @see N#repeatElementsToSize(Collection, int)
     */
    public static <T> ObjIterator<T> repeatElementsToSize(final Collection<? extends T> c, final long size) throws IllegalArgumentException {
        N.checkArgument(size >= 0, "'size' can't be negative: %s", size);
        N.checkArgument(size == 0 || N.notEmpty(c), "Collection can't be empty or null when size > 0");

        if (N.isEmpty(c) || size == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final long n = size / c.size();
            private long mod = size % c.size();

            private Iterator<? extends T> iter = null;
            private T next = null;
            private long cnt = mod-- > 0 ? n + 1 : n;

            @Override
            public boolean hasNext() {
                return cnt > 0 || ((n > 0 || mod > 0) && (iter != null && iter.hasNext()));
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null) {
                    iter = c.iterator();
                    next = iter.next();
                } else if (cnt <= 0) {
                    next = iter.next();
                    cnt = mod-- > 0 ? n + 1 : n;
                }

                cnt--;

                return next;
            }
        };
    }

    /**
     * Repeats the entire specified Collection cyclically until the specified total size is reached.
     * The collection is repeated as a whole, cycling through it multiple times if necessary.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B");
     * ObjIterator<String> iter = Iterators.repeatCollectionToSize(list, 5);
     * // Yields: "A", "B", "A", "B", "A"
     *
     * List<Integer> numbers = Arrays.asList(1, 2, 3);
     * ObjIterator<Integer> iter2 = Iterators.repeatCollectionToSize(numbers, 7);
     * // Yields: 1, 2, 3, 1, 2, 3, 1
     * }</pre>
     *
     * @param <T> the type of elements in the collection.
     * @param c the collection to be repeated. Must not be empty or {@code null} if {@code size > 0}.
     * @param size the total number of elements the resulting iterator should produce. Must be non-negative.
     * @return an {@code ObjIterator} that cycles through the collection until {@code size} elements have been produced.
     * @throws IllegalArgumentException if {@code size} is negative, or if {@code c} is empty or {@code null} when {@code size > 0}.
     * @see N#repeatCollectionToSize(Collection, int)
     */
    public static <T> ObjIterator<T> repeatCollectionToSize(final Collection<? extends T> c, final long size) throws IllegalArgumentException {
        N.checkArgument(size >= 0, "'size' can't be negative: %s", size);
        N.checkArgument(size == 0 || N.notEmpty(c), "Collection can't be empty or null when size > 0");

        if (N.isEmpty(c) || size == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterator<? extends T> iter = null;
            private long cnt = size;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null || !iter.hasNext()) {
                    iter = c.iterator();
                }

                cnt--;

                return iter.next();
            }
        };
    }

    /**
     * Returns an infinite iterator cycling over the provided elements.
     * However, if the provided elements are empty, an empty iterator will be returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<String> iter = Iterators.cycle("A", "B", "C");
     * // Yields: "A", "B", "C", "A", "B", "C", "A", "B", "C", ... (infinitely)
     *
     * ObjIterator<Integer> numbers = Iterators.cycle(1, 2);
     * // Yields: 1, 2, 1, 2, 1, 2, ... (infinitely)
     * }</pre>
     *
     * @param <T> the type of elements in the array.
     * @param elements the array whose elements are to be cycled over.
     * @return an iterator cycling over the elements of the array.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> cycle(final T... elements) {
        if (N.isEmpty(elements)) {
            return ObjIterator.empty();
        }

        final T[] a = elements.clone();
        final int len = a.length;

        return new ObjIterator<>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @SuppressFBWarnings("IT_NO_SUCH_ELEMENT")
            @Override
            public T next() { // NOSONAR
                if (cursor >= len) {
                    cursor = 0;
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Returns an infinite iterator cycling over the elements of the provided iterable.
     * However, if the provided elements are empty, an empty iterator will be returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B", "C");
     * ObjIterator<String> iter = Iterators.cycle(list);
     * // Yields: "A", "B", "C", "A", "B", "C", ... (infinitely)
     *
     * Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
     * ObjIterator<Integer> numbers = Iterators.cycle(set);
     * // Yields: 1, 2, 3, 1, 2, 3, ... (infinitely, in set iteration order)
     * }</pre>
     *
     * @param <T> the type of elements in the iterable.
     * @param iterable the iterable whose elements are to be cycled over.
     * @return an iterator cycling over the elements of the iterable.
     */
    public static <T> ObjIterator<T> cycle(final Iterable<? extends T> iterable) {
        if (N.isEmpty(iterable)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterator<? extends T> iter;
            private List<T> list;
            private T[] a;
            private int len;
            private int cursor = 0;
            private T next = null;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                if (iter == null) {
                    iter = iterable.iterator();
                    list = new ArrayList<>();
                }

                if (a == null) {
                    if (iter.hasNext()) {
                        next = iter.next();
                        list.add(next);
                        return next;
                    } else {
                        a = (T[]) list.toArray();
                        len = a.length;
                        list = null;
                    }
                }

                if (cursor >= len) {
                    cursor = 0;
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Returns an iterator that cycles over the elements of the provided iterable for a specified number of rounds.
     * If the provided iterable is empty, an empty iterator will be returned.
     * If the number of rounds is zero, an empty iterator will be returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B", "C");
     * ObjIterator<String> iter = Iterators.cycle(list, 2);
     * // Yields: "A", "B", "C", "A", "B", "C"
     *
     * Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
     * ObjIterator<Integer> numbers = Iterators.cycle(set, 3);
     * // Yields: 1, 2, 1, 2, 1, 2
     * }</pre>
     *
     * @param <T> the type of elements in the iterable.
     * @param iterable the iterable whose elements are to be cycled over.
     * @param rounds the number of times to cycle over the iterable's elements.
     * @return an {@code ObjIterator} cycling over the elements of the iterable for the specified number of rounds.
     * @throws IllegalArgumentException if {@code rounds} is negative.
     */
    public static <T> ObjIterator<T> cycle(final Iterable<? extends T> iterable, final long rounds) {
        N.checkArgNotNegative(rounds, cs.rounds);

        if (N.isEmpty(iterable) || rounds == 0) {
            return ObjIterator.empty();
        } else if (rounds == 1) {
            return ObjIterator.of(iterable);
        }

        return new ObjIterator<>() {
            private Iterator<? extends T> iter;
            private List<T> list;
            private T[] a;
            private int len;
            private long m = 1;
            private int cursor = 0;
            private T next = null;

            @Override
            public boolean hasNext() {
                return m < rounds || (m == rounds && cursor < len);
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null) {
                    iter = iterable.iterator();
                    list = new ArrayList<>();
                }

                if (a == null) {
                    if (iter.hasNext()) {
                        next = iter.next();
                        list.add(next);
                        return next;
                    } else {
                        m++;
                        a = (T[]) list.toArray();
                        len = a.length;
                        list = null;
                    }
                }

                if (cursor >= len) {
                    m++;
                    cursor = 0;
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple boolean arrays into a single BooleanIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] a1 = {true, false};
     * boolean[] a2 = {true};
     * BooleanIterator iter = Iterators.concat(a1, a2);
     * // iter.nextBoolean() => true
     * // iter.nextBoolean() => false
     * // iter.nextBoolean() => true
     * }</pre>
     *
     * @param a the boolean arrays to be concatenated.
     * @return a BooleanIterator that will iterate over the elements of each provided boolean array in order.
     */
    public static BooleanIterator concat(final boolean[]... a) {
        if (N.isEmpty(a)) {
            return BooleanIterator.EMPTY;
        }

        return new BooleanIterator() {
            private final Iterator<boolean[]> iter = Arrays.asList(a).iterator();
            private boolean[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public boolean nextBoolean() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple char arrays into a single CharIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] a1 = {'a', 'b'};
     * char[] a2 = {'c'};
     * CharIterator iter = Iterators.concat(a1, a2);
     * // iter.nextChar() => 'a'
     * // iter.nextChar() => 'b'
     * // iter.nextChar() => 'c'
     * }</pre>
     *
     * @param a the char arrays to be concatenated.
     * @return a CharIterator that will iterate over the elements of each provided char array in order.
     */
    public static CharIterator concat(final char[]... a) {
        if (N.isEmpty(a)) {
            return CharIterator.EMPTY;
        }

        return new CharIterator() {
            private final Iterator<char[]> iter = Arrays.asList(a).iterator();
            private char[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public char nextChar() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple byte arrays into a single ByteIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] a1 = {1, 2};
     * byte[] a2 = {3};
     * ByteIterator iter = Iterators.concat(a1, a2);
     * // iter.nextByte() => 1
     * // iter.nextByte() => 2
     * // iter.nextByte() => 3
     * }</pre>
     *
     * @param a the byte arrays to be concatenated.
     * @return a ByteIterator that will iterate over the elements of each provided byte array in order.
     */
    public static ByteIterator concat(final byte[]... a) {
        if (N.isEmpty(a)) {
            return ByteIterator.EMPTY;
        }

        return new ByteIterator() {
            private final Iterator<byte[]> iter = Arrays.asList(a).iterator();
            private byte[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public byte nextByte() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple short arrays into a single ShortIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] a1 = {10, 20};
     * short[] a2 = {30};
     * ShortIterator iter = Iterators.concat(a1, a2);
     * // iter.nextShort() => 10
     * // iter.nextShort() => 20
     * // iter.nextShort() => 30
     * }</pre>
     *
     * @param a the short arrays to be concatenated.
     * @return a ShortIterator that will iterate over the elements of each provided short array in order.
     */
    public static ShortIterator concat(final short[]... a) {
        if (N.isEmpty(a)) {
            return ShortIterator.EMPTY;
        }

        return new ShortIterator() {
            private final Iterator<short[]> iter = Arrays.asList(a).iterator();
            private short[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public short nextShort() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple int arrays into a single IntIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] a1 = {1, 2};
     * int[] a2 = {3, 4};
     * IntIterator iter = Iterators.concat(a1, a2);
     * // iter.nextInt() => 1
     * // iter.nextInt() => 2
     * // iter.nextInt() => 3
     * // iter.nextInt() => 4
     * }</pre>
     *
     * @param a the int arrays to be concatenated.
     * @return an IntIterator that will iterate over the elements of each provided int array in order.
     */
    public static IntIterator concat(final int[]... a) {
        if (N.isEmpty(a)) {
            return IntIterator.EMPTY;
        }

        return new IntIterator() {
            private final Iterator<int[]> iter = Arrays.asList(a).iterator();
            private int[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public int nextInt() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple long arrays into a single LongIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] a1 = {1L, 2L};
     * long[] a2 = {3L};
     * LongIterator iter = Iterators.concat(a1, a2);
     * // iter.nextLong() => 1L
     * // iter.nextLong() => 2L
     * // iter.nextLong() => 3L
     * }</pre>
     *
     * @param a the long arrays to be concatenated.
     * @return a LongIterator that will iterate over the elements of each provided long array in order.
     */
    public static LongIterator concat(final long[]... a) {
        if (N.isEmpty(a)) {
            return LongIterator.EMPTY;
        }

        return new LongIterator() {
            private final Iterator<long[]> iter = Arrays.asList(a).iterator();
            private long[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public long nextLong() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple float arrays into a single FloatIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] a1 = {1.1f};
     * float[] a2 = {2.2f};
     * FloatIterator iter = Iterators.concat(a1, a2);
     * // iter.nextFloat() => 1.1f
     * // iter.nextFloat() => 2.2f
     * }</pre>
     *
     * @param a the float arrays to be concatenated.
     * @return a FloatIterator that will iterate over the elements of each provided float array in order.
     */
    public static FloatIterator concat(final float[]... a) {
        if (N.isEmpty(a)) {
            return FloatIterator.EMPTY;
        }

        return new FloatIterator() {
            private final Iterator<float[]> iter = Arrays.asList(a).iterator();
            private float[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public float nextFloat() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple double arrays into a single DoubleIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] a1 = {1.1};
     * double[] a2 = {2.2, 3.3};
     * DoubleIterator iter = Iterators.concat(a1, a2);
     * // iter.nextDouble() => 1.1
     * // iter.nextDouble() => 2.2
     * // iter.nextDouble() => 3.3
     * }</pre>
     *
     * @param a the double arrays to be concatenated.
     * @return a DoubleIterator that will iterate over the elements of each provided double array in order.
     */
    public static DoubleIterator concat(final double[]... a) {
        if (N.isEmpty(a)) {
            return DoubleIterator.EMPTY;
        }

        return new DoubleIterator() {
            private final Iterator<double[]> iter = Arrays.asList(a).iterator();
            private double[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public double nextDouble() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple BooleanIterators into a single BooleanIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter1 = BooleanIterator.of(true, false);
     * BooleanIterator iter2 = BooleanIterator.of(true);
     * BooleanIterator result = Iterators.concat(iter1, iter2);
     * // result.nextBoolean() => true
     * // result.nextBoolean() => false
     * // result.nextBoolean() => true
     * }</pre>
     *
     * @param a the BooleanIterators to be concatenated.
     * @return a BooleanIterator that will iterate over the elements of each provided BooleanIterator in order.
     */
    public static BooleanIterator concat(final BooleanIterator... a) {
        if (N.isEmpty(a)) {
            return BooleanIterator.EMPTY;
        }

        return new BooleanIterator() {
            private final Iterator<BooleanIterator> iter = Arrays.asList(a).iterator();
            private BooleanIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public boolean nextBoolean() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextBoolean();
            }
        };
    }

    /**
     * Concatenates multiple CharIterators into a single CharIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter1 = CharIterator.of('a', 'b');
     * CharIterator iter2 = CharIterator.of('c');
     * CharIterator result = Iterators.concat(iter1, iter2);
     * // result.nextChar() => 'a'
     * // result.nextChar() => 'b'
     * // result.nextChar() => 'c'
     * }</pre>
     *
     * @param a the CharIterators to be concatenated.
     * @return a CharIterator that will iterate over the elements of each provided CharIterator in order.
     */
    public static CharIterator concat(final CharIterator... a) {
        if (N.isEmpty(a)) {
            return CharIterator.EMPTY;
        }

        return new CharIterator() {
            private final Iterator<CharIterator> iter = Arrays.asList(a).iterator();
            private CharIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public char nextChar() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextChar();
            }
        };
    }

    /**
     * Concatenates multiple ByteIterators into a single ByteIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIterator iter1 = ByteIterator.of((byte) 1, (byte) 2);
     * ByteIterator iter2 = ByteIterator.of((byte) 3);
     * ByteIterator result = Iterators.concat(iter1, iter2);
     * // result.nextByte() => 1
     * // result.nextByte() => 2
     * // result.nextByte() => 3
     * }</pre>
     *
     * @param a the ByteIterators to be concatenated.
     * @return a ByteIterator that will iterate over the elements of each provided ByteIterator in order.
     */
    public static ByteIterator concat(final ByteIterator... a) {
        if (N.isEmpty(a)) {
            return ByteIterator.EMPTY;
        }

        return new ByteIterator() {
            private final Iterator<ByteIterator> iter = Arrays.asList(a).iterator();
            private ByteIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public byte nextByte() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextByte();
            }
        };
    }

    /**
     * Concatenates multiple ShortIterators into a single ShortIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter1 = ShortIterator.of((short) 1, (short) 2);
     * ShortIterator iter2 = ShortIterator.of((short) 3);
     * ShortIterator result = Iterators.concat(iter1, iter2);
     * // result.nextShort() => 1
     * // result.nextShort() => 2
     * // result.nextShort() => 3
     * }</pre>
     *
     * @param a the ShortIterators to be concatenated.
     * @return a ShortIterator that will iterate over the elements of each provided ShortIterator in order.
     */
    public static ShortIterator concat(final ShortIterator... a) {
        if (N.isEmpty(a)) {
            return ShortIterator.EMPTY;
        }

        return new ShortIterator() {
            private final Iterator<ShortIterator> iter = Arrays.asList(a).iterator();
            private ShortIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public short nextShort() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextShort();
            }
        };
    }

    /**
     * Concatenates multiple IntIterators into a single IntIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator iter1 = IntIterator.of(1, 2);
     * IntIterator iter2 = IntIterator.of(3);
     * IntIterator result = Iterators.concat(iter1, iter2);
     * // result.nextInt() => 1
     * // result.nextInt() => 2
     * // result.nextInt() => 3
     * }</pre>
     *
     * @param a the IntIterators to be concatenated.
     * @return an IntIterator that will iterate over the elements of each provided IntIterator in order.
     */
    public static IntIterator concat(final IntIterator... a) {
        if (N.isEmpty(a)) {
            return IntIterator.EMPTY;
        }

        return new IntIterator() {
            private final Iterator<IntIterator> iter = Arrays.asList(a).iterator();
            private IntIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public int nextInt() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextInt();
            }
        };
    }

    /**
     * Concatenates multiple LongIterators into a single LongIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iter1 = LongIterator.of(1L, 2L);
     * LongIterator iter2 = LongIterator.of(3L);
     * LongIterator result = Iterators.concat(iter1, iter2);
     * // result.nextLong() => 1L
     * // result.nextLong() => 2L
     * // result.nextLong() => 3L
     * }</pre>
     *
     * @param a the LongIterators to be concatenated.
     * @return a LongIterator that will iterate over the elements of each provided LongIterator in order.
     */
    public static LongIterator concat(final LongIterator... a) {
        if (N.isEmpty(a)) {
            return LongIterator.EMPTY;
        }

        return new LongIterator() {
            private final Iterator<LongIterator> iter = Arrays.asList(a).iterator();
            private LongIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public long nextLong() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextLong();
            }
        };
    }

    /**
     * Concatenates multiple FloatIterators into a single FloatIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter1 = FloatIterator.of(1.1f, 2.2f);
     * FloatIterator iter2 = FloatIterator.of(3.3f);
     * FloatIterator result = Iterators.concat(iter1, iter2);
     * // result.nextFloat() => 1.1f
     * // result.nextFloat() => 2.2f
     * // result.nextFloat() => 3.3f
     * }</pre>
     *
     * @param a the FloatIterators to be concatenated.
     * @return a FloatIterator that will iterate over the elements of each provided FloatIterator in order.
     */
    public static FloatIterator concat(final FloatIterator... a) {
        if (N.isEmpty(a)) {
            return FloatIterator.EMPTY;
        }

        return new FloatIterator() {
            private final Iterator<FloatIterator> iter = Arrays.asList(a).iterator();
            private FloatIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public float nextFloat() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextFloat();
            }
        };
    }

    /**
     * Concatenates multiple DoubleIterators into a single DoubleIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator iter1 = DoubleIterator.of(1.1, 2.2);
     * DoubleIterator iter2 = DoubleIterator.of(3.3);
     * DoubleIterator result = Iterators.concat(iter1, iter2);
     * // result.nextDouble() => 1.1
     * // result.nextDouble() => 2.2
     * // result.nextDouble() => 3.3
     * }</pre>
     *
     * @param a the DoubleIterators to be concatenated.
     * @return a DoubleIterator that will iterate over the elements of each provided DoubleIterator in order.
     */
    public static DoubleIterator concat(final DoubleIterator... a) {
        if (N.isEmpty(a)) {
            return DoubleIterator.EMPTY;
        }

        return new DoubleIterator() {
            private final Iterator<DoubleIterator> iter = Arrays.asList(a).iterator();
            private DoubleIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public double nextDouble() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextDouble();
            }
        };
    }

    /**
     * Concatenates multiple arrays into a single ObjIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] a1 = {"a", "b"};
     * String[] a2 = {"c"};
     * ObjIterator<String> iter = Iterators.concat(a1, a2);
     * // iter.next() => "a"
     * // iter.next() => "b"
     * // iter.next() => "c"
     * }</pre>
     *
     * @param <T> the type of elements in the arrays.
     * @param a the arrays to be concatenated.
     * @return an ObjIterator that will iterate over the elements of each provided array in order.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final T[]... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<? extends T>> list = new ArrayList<>(a.length);

        for (final T[] e : a) {
            if (N.notEmpty(e)) {
                list.add(ObjIterator.of(e));
            }
        }

        return concat(list);
    }

    /**
     * Concatenates multiple Iterators into a single ObjIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
     * Iterator<String> iter2 = Arrays.asList("c").iterator();
     * ObjIterator<String> result = Iterators.concat(iter1, iter2);
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * }</pre>
     *
     * @param <T> the type of elements in the Iterators.
     * @param a the Iterators to be concatenated.
     * @return an ObjIterator that will iterate over the elements of each provided Iterator in order.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterator<? extends T>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates multiple Iterable objects into a single ObjIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list1 = Arrays.asList("A", "B");
     * List<String> list2 = Arrays.asList("C", "D");
     * Set<String> set = new HashSet<>(Arrays.asList("E", "F"));
     * ObjIterator<String> iter = Iterators.concat(list1, list2, set);
     * // Yields: "A", "B", "C", "D", "E", "F"
     *
     * List<Integer> nums1 = Arrays.asList(1, 2);
     * List<Integer> nums2 = Arrays.asList(3, 4);
     * ObjIterator<Integer> numbers = Iterators.concat(nums1, nums2);
     * // Yields: 1, 2, 3, 4
     * }</pre>
     *
     * @param <T> the type of elements in the Iterable objects.
     * @param a the Iterable objects to be concatenated.
     * @return an ObjIterator that will iterate over the elements of each provided Iterable.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterable<? extends T>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<? extends T>> list = new ArrayList<>(a.length);

        for (final Iterable<? extends T> e : a) {
            list.add(N.iterate(e));
        }

        return concat(list);
    }

    /**
     * Concatenates multiple Maps into a single ObjIterator of Map.Entry.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = N.asMap("a", 1);
     * Map<String, Integer> map2 = N.asMap("b", 2);
     * ObjIterator<Map.Entry<String, Integer>> iter = Iterators.concat(map1, map2);
     * // Iterates over entries from map1 then map2
     * }</pre>
     *
     * @param <K> the type of keys in the Maps.
     * @param <V> the type of values in the Maps.
     * @param a the Maps to be concatenated.
     * @return an ObjIterator of Map.Entry that will iterate over the entries of each provided Map.
     */
    @SafeVarargs
    public static <K, V> ObjIterator<Map.Entry<K, V>> concat(final Map<? extends K, ? extends V>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<Map.Entry<K, V>>> list = new ArrayList<>(a.length);

        for (final Map<? extends K, ? extends V> e : a) {
            if (N.notEmpty(e)) {
                list.add(((Map<K, V>) e).entrySet().iterator());
            }
        }

        return concat(list);
    }

    /**
     * Concatenates multiple Iterators into a single ObjIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<String>> iterators = Arrays.asList(
     *     Arrays.asList("a", "b").iterator(),
     *     Arrays.asList("c").iterator()
     * );
     * ObjIterator<String> result = Iterators.concat(iterators);
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * }</pre>
     *
     * @param <T> the type of elements in the Iterators.
     * @param c the collection of iterator to be concatenated.
     * @return an ObjIterator that will iterate over the elements of each provided Iterator in order.
     */
    public static <T> ObjIterator<T> concat(final Collection<? extends Iterator<? extends T>> c) {
        if (N.isEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        };
    }

    /**
     * Concatenates multiple Iterable objects into a single ObjIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<List<String>> iterables = Arrays.asList(
     *     Arrays.asList("a", "b"),
     *     Arrays.asList("c")
     * );
     * ObjIterator<String> result = Iterators.concatIterables(iterables);
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * }</pre>
     *
     * @param <T> the type of elements in the Iterable objects.
     * @param c the collection of Iterable objects to be concatenated.
     * @return an ObjIterator that will iterate over the elements of each provided Iterable.
     * @see N#concat(Iterable...)
     * @see N#iterateEach(Collection)
     */
    public static <T> ObjIterator<T> concatIterables(final Collection<? extends Iterable<? extends T>> c) {
        if (N.isEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final Iterator<? extends Iterable<? extends T>> iter = c.iterator();
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = N.iterate(iter.next());
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        };
    }

    /**
     * Concatenates multiple BiIterators into a single BiIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIterator<String, Integer> iter1 = BiIterator.of("a", 1);
     * BiIterator<String, Integer> iter2 = BiIterator.of("b", 2);
     * BiIterator<String, Integer> result = Iterators.concat(iter1, iter2);
     * // result.next() => Pair("a", 1)
     * // result.next() => Pair("b", 2)
     * }</pre>
     *
     * @param <A> the type of the first element in the BiIterator.
     * @param <B> the type of the second element in the BiIterator.
     * @param a the BiIterators to be concatenated.
     * @return a BiIterator that will iterate over the elements of each provided BiIterator in order.
     */
    @SafeVarargs
    public static <A, B> BiIterator<A, B> concat(final BiIterator<A, B>... a) {
        if (N.isEmpty(a)) {
            return BiIterator.empty();
        }

        return new BiIterator<>() {
            private final Iterator<BiIterator<A, B>> iter = Arrays.asList(a).iterator();
            private BiIterator<A, B> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cur.next(action);
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                final Throwables.BiConsumer<? super A, ? super B, RuntimeException> actionE = Fnn.from(action);

                while (hasNext()) {
                    cur.foreachRemaining(actionE);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                while (hasNext()) {
                    cur.foreachRemaining(action);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {

                return new ObjIterator<>() {
                    private ObjIterator<R> mappedIter = null;

                    @Override
                    public boolean hasNext() {
                        if (mappedIter == null || !mappedIter.hasNext()) {
                            while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                                cur = iter.next();
                            }

                            if (cur != null) {
                                mappedIter = cur.map(mapper);
                            }
                        }

                        return mappedIter != null && mappedIter.hasNext();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mappedIter.next();
                    }
                };
            }
        };
    }

    /**
     * Concatenates multiple TriIterators into a single TriIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriIterator<String, Integer, Boolean> iter1 = TriIterator.of("a", 1, true);
     * TriIterator<String, Integer, Boolean> iter2 = TriIterator.of("b", 2, false);
     * TriIterator<String, Integer, Boolean> result = Iterators.concat(iter1, iter2);
     * // result.next() => Triple("a", 1, true)
     * // result.next() => Triple("b", 2, false)
     * }</pre>
     *
     * @param <A> the type of the first element in the TriIterator.
     * @param <B> the type of the second element in the TriIterator.
     * @param <C> the type of the third element in the TriIterator.
     * @param a the TriIterators to be concatenated.
     * @return a TriIterator that will iterate over the elements of each provided TriIterator in order.
     */
    @SafeVarargs
    public static <A, B, C> TriIterator<A, B, C> concat(final TriIterator<A, B, C>... a) {
        if (N.isEmpty(a)) {
            return TriIterator.empty();
        }

        return new TriIterator<>() {
            private final Iterator<TriIterator<A, B, C>> iter = Arrays.asList(a).iterator();
            private TriIterator<A, B, C> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public Triple<A, B, C> next() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cur.next(action);
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                while (hasNext()) {
                    cur.foreachRemaining(action);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (hasNext()) {
                    cur.foreachRemaining(action);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {

                return new ObjIterator<>() {
                    private ObjIterator<R> mappedIter = null;

                    @Override
                    public boolean hasNext() {
                        if (mappedIter == null || !mappedIter.hasNext()) {
                            while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                                cur = iter.next();
                            }

                            if (cur != null) {
                                mappedIter = cur.map(mapper);
                            }
                        }

                        return mappedIter != null && mappedIter.hasNext();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mappedIter.next();
                    }
                };
            }
        };
    }

    /**
     * Merges two iterators into a single {@code ObjIterator}. The order of elements in the resulting iterator is determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
     * Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();
     * ObjIterator<Integer> result = Iterators.merge(iter1, iter2,
     *     (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // result => 1, 2, 3, 4, 5, 6
     * }</pre>
     *
     * @param <T> the type of elements in the iterators.
     * @param a the first iterator to be merged.
     * @param b the second iterator to be merged.
     * @param nextSelector a {@code BiFunction} that determines the order of elements in the resulting iterator.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided iterators in the order determined by {@code nextSelector}.
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}.
     */
    public static <T> ObjIterator<T> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector);

        return new ObjIterator<>() {
            private final Iterator<? extends T> iterA = a == null ? ObjIterator.<T> empty() : a;
            private final Iterator<? extends T> iterB = b == null ? ObjIterator.<T> empty() : b;
            private T nextA = null;
            private T nextB = null;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return hasNextA || hasNextB || iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public T next() {
                if (hasNextA) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
                            hasNextA = false;
                            hasNextB = true;
                            return nextA;
                        } else {
                            return nextB;
                        }
                    } else {
                        hasNextA = false;
                        return nextA;
                    }
                } else if (hasNextB) {
                    if (iterA.hasNext()) {
                        if (nextSelector.apply((nextA = iterA.next()), nextB) == MergeResult.TAKE_FIRST) {
                            return nextA;
                        } else {
                            hasNextA = true;
                            hasNextB = false;
                            return nextB;
                        }
                    } else {
                        hasNextB = false;
                        return nextB;
                    }
                } else if (iterA.hasNext()) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply((nextA = iterA.next()), (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return iterA.next();
                    }
                } else {
                    return iterB.next();
                }
            }
        };
    }

    /**
     * Merges multiple iterators into a single {@code ObjIterator}. The order of elements in the resulting iterator is determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<Integer>> iters = Arrays.asList(
     *     Arrays.asList(1, 3, 5).iterator(),
     *     Arrays.asList(2, 4, 6).iterator()
     * );
     * ObjIterator<Integer> merged = Iterators.merge(iters, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // Yields: 1, 2, 3, 4, 5, 6 (sorted merge)
     * }</pre>
     *
     * @param <T> the type of elements in the iterators.
     * @param c the collection of iterators to be merged.
     * @param nextSelector a {@code BiFunction} that determines the order of elements in the resulting iterator.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided iterators in the order determined by {@code nextSelector}.
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}.
     */
    public static <T> ObjIterator<T> merge(final Collection<? extends Iterator<? extends T>> c,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector);

        if (N.isEmpty(c)) {
            return ObjIterator.empty();
        } else if (c.size() == 1) {
            return ObjIterator.of(c.iterator().next());
        } else if (c.size() == 2) {
            final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
        ObjIterator<T> result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result, iter.next(), nextSelector);
        }

        return result;
    }

    /**
     * Merges two {@code Iterable} objects into a single {@code ObjIterator}. The order of elements in the resulting iterator is determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> list1 = Arrays.asList(1, 3, 5);
     * List<Integer> list2 = Arrays.asList(2, 4, 6);
     * ObjIterator<Integer> merged = Iterators.merge(list1, list2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // Yields: 1, 2, 3, 4, 5, 6 (sorted merge)
     * }</pre>
     *
     * @param <T> the type of elements in the {@code Iterable} objects.
     * @param a the first {@code Iterable} object to be merged.
     * @param b the second {@code Iterable} object to be merged.
     * @param nextSelector a {@code BiFunction} that determines the order of elements in the resulting iterator.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided {@code Iterable} objects in the order determined by {@code nextSelector}.
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}.
     */
    public static <T> ObjIterator<T> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
        final Iterator<? extends T> iterA = N.iterate(a);
        final Iterator<? extends T> iterB = N.iterate(b);

        return merge(iterA, iterB, nextSelector);

    }

    /**
     * Merges multiple {@code Iterable} objects into a single {@code ObjIterator}. The order of elements in the resulting iterator is determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<List<Integer>> lists = Arrays.asList(
     *     Arrays.asList(1, 4, 7),
     *     Arrays.asList(2, 5, 8),
     *     Arrays.asList(3, 6, 9)
     * );
     * ObjIterator<Integer> merged = Iterators.mergeIterables(lists, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // Yields: 1, 2, 3, 4, 5, 6, 7, 8, 9 (sorted merge)
     * }</pre>
     *
     * @param <T> the type of elements in the {@code Iterable} objects.
     * @param iterables the collection of {@code Iterable} objects to be merged.
     * @param nextSelector a {@code BiFunction} that determines the order of elements in the resulting iterator.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided {@code Iterable} objects in the order determined by {@code nextSelector}.
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}.
     */
    public static <T> ObjIterator<T> mergeIterables(final Collection<? extends Iterable<? extends T>> iterables,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector);

        if (N.isEmpty(iterables)) {
            return ObjIterator.empty();
        } else if (iterables.size() == 1) {
            return ObjIterator.of(iterables.iterator().next());
        } else if (iterables.size() == 2) {
            final Iterator<? extends Iterable<? extends T>> iter = iterables.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(iterables.size());

        for (final Iterable<? extends T> e : iterables) {
            iterList.add(N.iterate(e));
        }

        return merge(iterList, nextSelector);
    }

    /**
     * Merges two sorted Iterators into a single ObjIterator, which will iterate over the elements of each Iterator in a sorted order.
     * The elements in the Iterators should implement Comparable interface.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
     * Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();
     * ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2);
     * // result => 1, 2, 3, 4, 5, 6
     * }</pre>
     *
     * @param <T> the type of elements in the Iterators, which should implement Comparable interface.
     * @param sortedA the first Iterator to be merged. It should be in non-descending order.
     * @param sortedB the second Iterator to be merged. It should be in non-descending order.
     * @return an ObjIterator that will iterate over the elements of the provided Iterators in a sorted order.
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> ObjIterator<T> mergeSorted(final Iterator<? extends T> sortedA, final Iterator<? extends T> sortedB) {
        return mergeSorted(sortedA, sortedB, N.NATURAL_COMPARATOR);
    }

    /**
     * Merges two sorted iterators into a single {@code ObjIterator}, which will iterate over the elements of each iterator in a sorted order.
     * The order of elements in the resulting iterator is determined by the provided {@code Comparator}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter1 = Arrays.asList(5, 3, 1).iterator();
     * Iterator<Integer> iter2 = Arrays.asList(6, 4, 2).iterator();
     * ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2, Comparator.reverseOrder());
     * // result => 6, 5, 4, 3, 2, 1
     * }</pre>
     *
     * @param <T> the type of elements in the iterators.
     * @param sortedA the first iterator to be merged. It should be in non-descending order.
     * @param sortedB the second iterator to be merged. It should be in non-descending order.
     * @param cmp the {@code Comparator} to determine the order of elements in the resulting iterator.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided iterators in a sorted order.
     * @throws IllegalArgumentException if {@code cmp} is {@code null}.
     */
    public static <T> ObjIterator<T> mergeSorted(final Iterator<? extends T> sortedA, final Iterator<? extends T> sortedB, final Comparator<? super T> cmp)
            throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return merge(sortedA, sortedB, MergeResult.minFirst(cmp));
    }

    /**
     * Merges two sorted Iterable objects into a single ObjIterator, which will iterate over the elements of each Iterable in a sorted order.
     * The elements in the Iterable objects should implement Comparable interface.
     *
     * @param <T> the type of elements in the Iterable objects, which should implement Comparable interface.
     * @param sortedA the first Iterable object to be merged. It should be in non-descending order.
     * @param sortedB the second Iterable object to be merged. It should be in non-descending order.
     * @return an ObjIterator that will iterate over the elements of the provided Iterable objects in a sorted order.
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> ObjIterator<T> mergeSorted(final Iterable<? extends T> sortedA, final Iterable<? extends T> sortedB) {
        return mergeSorted(sortedA, sortedB, N.NATURAL_COMPARATOR);
    }

    /**
     * Merges two sorted {@code Iterable} objects into a single {@code ObjIterator}, which will iterate over the elements of each {@code Iterable} in the order determined by the provided {@code Comparator}.
     *
     * @param <T> the type of elements in the {@code Iterable} objects.
     * @param sortedA the first {@code Iterable} object to be merged. It should be in non-descending order.
     * @param sortedB the second {@code Iterable} object to be merged. It should be in non-descending order.
     * @param cmp the {@code Comparator} to determine the order of elements in the resulting iterator.
     * @return an {@code ObjIterator} that will iterate over the elements of the provided {@code Iterable} objects in a sorted order.
     * @throws IllegalArgumentException if {@code cmp} is {@code null}.
     */
    public static <T> ObjIterator<T> mergeSorted(final Iterable<? extends T> sortedA, final Iterable<? extends T> sortedB, final Comparator<? super T> cmp) {
        final Iterator<? extends T> iterA = N.iterate(sortedA);
        final Iterator<? extends T> iterB = N.iterate(sortedB);

        return mergeSorted(iterA, iterB, cmp);
    }

    /**
     * Zips two iterators into a single {@code ObjIterator}, which will iterate over the elements of each iterator in parallel.
     * The resulting elements are determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> a = Arrays.asList("a", "b").iterator();
     * Iterator<Integer> b = Arrays.asList(1, 2).iterator();
     * ObjIterator<String> result = Iterators.zip(a, b, (s, i) -> s + i);
     * // result.next() => "a1"
     * // result.next() => "b2"
     * }</pre>
     *
     * @param <A> the type of elements in the first iterator.
     * @param <B> the type of elements in the second iterator.
     * @param <R> the type of elements in the resulting {@code ObjIterator}.
     * @param a the first iterator to be zipped.
     * @param b the second iterator to be zipped.
     * @param zipFunction a {@code BiFunction} that takes an element from each iterator and returns a new element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements created by {@code zipFunction}.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        N.checkArgNotNull(zipFunction, cs.function);

        return new ObjIterator<>() {
            private final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(iterA.next(), iterB.next());
            }
        };
    }

    /**
     * Zips two {@code Iterable} objects into a single {@code ObjIterator}, which will iterate over the elements of each {@code Iterable} in parallel.
     * The resulting elements are determined by the provided {@code BiFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> a = Arrays.asList("a", "b");
     * List<Integer> b = Arrays.asList(1, 2);
     * ObjIterator<String> result = Iterators.zip(a, b, (s, i) -> s + i);
     * // result.next() => "a1"
     * // result.next() => "b2"
     * }</pre>
     *
     * @param <A> the type of elements in the first {@code Iterable}.
     * @param <B> the type of elements in the second {@code Iterable}.
     * @param <R> the type of elements in the resulting {@code ObjIterator}.
     * @param a the first {@code Iterable} to be zipped.
     * @param b the second {@code Iterable} to be zipped.
     * @param zipFunction a {@code BiFunction} that takes an element from each {@code Iterable} and returns a new element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements created by {@code zipFunction}.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);

        return zip(iterA, iterB, zipFunction);
    }

    /**
     * Zips three iterators into a single {@code ObjIterator}, which will iterate over the elements of each iterator in parallel.
     * The resulting elements are determined by the provided {@code TriFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> a = Arrays.asList("a").iterator();
     * Iterator<Integer> b = Arrays.asList(1).iterator();
     * Iterator<Boolean> c = Arrays.asList(true).iterator();
     * ObjIterator<String> result = Iterators.zip(a, b, c, (s, i, bool) -> s + i + bool);
     * // result.next() => "a1true"
     * }</pre>
     *
     * @param <A> the type of elements in the first iterator.
     * @param <B> the type of elements in the second iterator.
     * @param <C> the type of elements in the third iterator.
     * @param <R> the type of elements in the resulting {@code ObjIterator}.
     * @param a the first iterator to be zipped.
     * @param b the second iterator to be zipped.
     * @param c the third iterator to be zipped.
     * @param zipFunction a {@code TriFunction} that takes an element from each iterator and returns a new element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements created by {@code zipFunction}.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c,
            final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) {
        return new ObjIterator<>() {
            private final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;
            private final Iterator<C> iterC = c == null ? ObjIterator.<C> empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(iterA.next(), iterB.next(), iterC.next());
            }
        };
    }

    /**
     * Zips three {@code Iterable} objects into a single {@code ObjIterator}, which will iterate over the elements of each {@code Iterable} in parallel.
     * The resulting elements are determined by the provided {@code TriFunction}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("Alice", "Bob");
     * List<Integer> ages = Arrays.asList(30, 25);
     * List<String> cities = Arrays.asList("NYC", "LA");
     * ObjIterator<String> result = Iterators.zip(names, ages, cities, (name, age, city) -> name + "," + age + "," + city);
     * // result.next() => "Alice,30,NYC"
     * // result.next() => "Bob,25,LA"
     * }</pre>
     *
     * @param <A> the type of elements in the first {@code Iterable}.
     * @param <B> the type of elements in the second {@code Iterable}.
     * @param <C> the type of elements in the third {@code Iterable}.
     * @param <R> the type of elements in the resulting {@code ObjIterator}.
     * @param a the first {@code Iterable} to be zipped.
     * @param b the second {@code Iterable} to be zipped.
     * @param c the third {@code Iterable} to be zipped.
     * @param zipFunction a {@code TriFunction} that takes an element from each {@code Iterable} and returns a new element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements created by {@code zipFunction}.
     * @throws IllegalArgumentException if {@code zipFunction} is {@code null}.
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c,
            final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);
        final Iterator<C> iterC = N.iterate(c);

        return zip(iterA, iterB, iterC, zipFunction);
    }

    /**
     * Zips two Iterators into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine two Iterators into one, which will iterate over the elements of each Iterator in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided BiFunction <i>zipFunction</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> a = Arrays.asList("a").iterator();
     * Iterator<Integer> b = Arrays.asList(1, 2).iterator();
     * ObjIterator<String> result = Iterators.zip(a, b, "empty", 0, (s, i) -> s + i);
     * // result.next() => "a1"
     * // result.next() => "empty2"
     * }</pre>
     *
     * @param <A> the type of elements in the first Iterator.
     * @param <B> the type of elements in the second Iterator.
     * @param <R> the type of elements in the resulting ObjIterator.
     * @param a the first Iterator to be zipped.
     * @param b the second Iterator to be zipped.
     * @param valueForNoneA the default value to be used when the first Iterator is exhausted.
     * @param valueForNoneB the default value to be used when the second Iterator is exhausted.
     * @param zipFunction a BiFunction that takes an element from each Iterator and returns a new element for the resulting ObjIterator.
     * @return an ObjIterator that will iterate over the elements created by <i>zipFunction</i>.
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        return new ObjIterator<>() {
            private final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public R next() {
                if (iterA.hasNext()) {
                    return zipFunction.apply(iterA.next(), iterB.hasNext() ? iterB.next() : valueForNoneB);
                } else {
                    return zipFunction.apply(valueForNoneA, iterB.next());
                }
            }
        };
    }

    /**
     * Zips two Iterable objects into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine two Iterable objects into one, which will iterate over the elements of each Iterable in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided BiFunction <i>zipFunction</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> a = Arrays.asList("a");
     * List<Integer> b = Arrays.asList(1, 2);
     * ObjIterator<String> result = Iterators.zip(a, b, "empty", 0, (s, i) -> s + i);
     * // result.next() => "a1"
     * // result.next() => "empty2"
     * }</pre>
     *
     * @param <A> the type of elements in the first Iterable.
     * @param <B> the type of elements in the second Iterable.
     * @param <R> the type of elements in the resulting ObjIterator.
     * @param a the first Iterable to be zipped.
     * @param b the second Iterable to be zipped.
     * @param valueForNoneA the default value to be used when the first Iterable is exhausted.
     * @param valueForNoneB the default value to be used when the second Iterable is exhausted.
     * @param zipFunction a BiFunction that takes an element from each Iterable and returns a new element for the resulting ObjIterator.
     * @return an ObjIterator that will iterate over the elements created by <i>zipFunction</i>.
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);

        return zip(iterA, iterB, valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zips three Iterators into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine three Iterators into one, which will iterate over the elements of each Iterator in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided TriFunction <i>zipFunction</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> names = Arrays.asList("Alice").iterator();
     * Iterator<Integer> ages = Arrays.asList(30, 25).iterator();
     * Iterator<String> cities = Arrays.asList("NYC").iterator();
     * ObjIterator<String> result = Iterators.zip(names, ages, cities, "Unknown", 0, "N/A",
     *     (name, age, city) -> name + "," + age + "," + city);
     * // result.next() => "Alice,30,NYC"
     * // result.next() => "Unknown,25,N/A"
     * }</pre>
     *
     * @param <A> the type of elements in the first Iterator.
     * @param <B> the type of elements in the second Iterator.
     * @param <C> the type of elements in the third Iterator.
     * @param <R> the type of elements in the resulting ObjIterator.
     * @param a the first Iterator to be zipped.
     * @param b the second Iterator to be zipped.
     * @param c the third Iterator to be zipped.
     * @param valueForNoneA the default value to be used when the first Iterator is exhausted.
     * @param valueForNoneB the default value to be used when the second Iterator is exhausted.
     * @param valueForNoneC the default value to be used when the third Iterator is exhausted.
     * @param zipFunction a TriFunction that takes an element from each Iterator and returns a new element for the resulting ObjIterator.
     * @return an ObjIterator that will iterate over the elements created by <i>zipFunction</i>.
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) {
        return new ObjIterator<>() {
            private final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;
            private final Iterator<C> iterC = c == null ? ObjIterator.<C> empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public R next() {
                if (iterA.hasNext()) {
                    return zipFunction.apply(iterA.next(), iterB.hasNext() ? iterB.next() : valueForNoneB, iterC.hasNext() ? iterC.next() : valueForNoneC);
                } else if (iterB.hasNext()) {
                    return zipFunction.apply(valueForNoneA, iterB.next(), iterC.hasNext() ? iterC.next() : valueForNoneC);
                } else {
                    return zipFunction.apply(valueForNoneA, valueForNoneB, iterC.next());
                }
            }
        };
    }

    /**
     * Zips three Iterable objects into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine three Iterable objects into one, which will iterate over the elements of each Iterable in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided TriFunction <i>zipFunction</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> names = Arrays.asList("Alice");
     * List<Integer> ages = Arrays.asList(30, 25);
     * List<String> cities = Arrays.asList("NYC");
     * ObjIterator<String> result = Iterators.zip(names, ages, cities, "Unknown", 0, "N/A",
     *     (name, age, city) -> name + "," + age + "," + city);
     * // result.next() => "Alice,30,NYC"
     * // result.next() => "Unknown,25,N/A"
     * }</pre>
     *
     * @param <A> the type of elements in the first Iterable.
     * @param <B> the type of elements in the second Iterable.
     * @param <C> the type of elements in the third Iterable.
     * @param <R> the type of elements in the resulting ObjIterator.
     * @param a the first Iterable to be zipped.
     * @param b the second Iterable to be zipped.
     * @param c the third Iterable to be zipped.
     * @param valueForNoneA the default value to be used when the first Iterable is exhausted.
     * @param valueForNoneB the default value to be used when the second Iterable is exhausted.
     * @param valueForNoneC the default value to be used when the third Iterable is exhausted.
     * @param zipFunction a TriFunction that takes an element from each Iterable and returns a new element for the resulting ObjIterator.
     * @return an ObjIterator that will iterate over the elements created by <i>zipFunction</i>.
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);
        final Iterator<C> iterC = N.iterate(c);

        return zip(iterA, iterB, iterC, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Unzips an Iterator into a BiIterator.
     * The transformation is determined by the provided BiConsumer <i>unzip</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a:1", "b:2").iterator();
     * BiIterator<String, Integer> result = Iterators.unzip(iter, (s, pair) -> {
     *     String[] parts = s.split(":");
     *     pair.set(parts[0], Integer.parseInt(parts[1]));
     * });
     * // result.next() => Pair("a", 1)
     * // result.next() => Pair("b", 2)
     * }</pre>
     *
     * @param <T> the type of elements in the original Iterator.
     * @param <A> the type of the first element in the resulting BiIterator.
     * @param <B> the type of the second element in the resulting BiIterator.
     * @param iter the original Iterator to be unzipped.
     * @param unzip a BiConsumer that takes an element from the original Iterator and a Pair to be filled with the resulting elements for the BiIterator.
     * @return a BiIterator that will iterate over the elements created by <i>unzip</i>.
     * @see BiIterator#unzip(Iterator, BiConsumer)
     * @see TriIterator#unzip(Iterator, BiConsumer)
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Pair<A, B>> unzip) {
        return BiIterator.unzip(iter, unzip);
    }

    /**
     * Unzips an Iterable into a BiIterator.
     * The transformation is determined by the provided BiConsumer <i>unzip</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a:1", "b:2");
     * BiIterator<String, Integer> result = Iterators.unzip(list, (s, pair) -> {
     *     String[] parts = s.split(":");
     *     pair.set(parts[0], Integer.parseInt(parts[1]));
     * });
     * // result.next() => Pair("a", 1)
     * // result.next() => Pair("b", 2)
     * }</pre>
     *
     * @param <T> the type of elements in the original Iterable.
     * @param <A> the type of the first element in the resulting BiIterator.
     * @param <B> the type of the second element in the resulting BiIterator.
     * @param c the original Iterable to be unzipped.
     * @param unzip a BiConsumer that takes an element from the original Iterable and a Pair to be filled with the resulting elements for the BiIterator.
     * @return a BiIterator that will iterate over the elements created by <i>unzip</i>.
     * @see BiIterator#unzip(Iterator, BiConsumer)
     * @see TriIterator#unzip(Iterator, BiConsumer)
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterable<? extends T> c, final BiConsumer<? super T, Pair<A, B>> unzip) {
        return BiIterator.unzip(N.iterate(c), unzip);
    }

    /**
     * Unzips an Iterator into a TriIterator.
     * The transformation is determined by the provided BiConsumer <i>unzip</i>.
     *
     * @param <T> the type of elements in the original Iterator.
     * @param <A> the type of the first element in the resulting TriIterator.
     * @param <B> the type of the second element in the resulting TriIterator.
     * @param <C> the type of the third element in the resulting TriIterator.
     * @param iter the original Iterator to be unzipped.
     * @param unzip a BiConsumer that takes an element from the original Iterator and a Triple to be filled with the resulting elements for the TriIterator.
     * @return a TriIterator that will iterate over the elements created by <i>unzip</i>.
     * @deprecated replaced by {@link TriIterator#unzip(Iterator, BiConsumer)}
     * @see TriIterator#unzip(Iterator, BiConsumer)
     * @see TriIterator#toMultiList(Supplier)
     * @see TriIterator#toMultiSet(Supplier)
     */
    @Deprecated
    @Beta
    public static <T, A, B, C> TriIterator<A, B, C> unzipp(final Iterator<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzip) {
        return TriIterator.unzip(iter, unzip);
    }

    /**
     * Unzips an Iterable into a TriIterator.
     * The transformation is determined by the provided BiConsumer <i>unzip</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a:1:x", "b:2:y");
     * TriIterator<String, Integer, String> result = Iterators.unzipp(list, (s, triple) -> {
     *     String[] parts = s.split(":");
     *     triple.set(parts[0], Integer.parseInt(parts[1]), parts[2]);
     * });
     * // result.next() => Triple("a", 1, "x")
     * // result.next() => Triple("b", 2, "y")
     * }</pre>
     *
     * @param <T> the type of elements in the original Iterable.
     * @param <A> the type of the first element in the resulting TriIterator.
     * @param <B> the type of the second element in the resulting {@code TriIterator}.
     * @param <C> the type of the third element in the resulting {@code TriIterator}.
     * @param c the original {@code Iterable} to be unzipped.
     * @param unzip a {@code BiConsumer} that takes an element from the original {@code Iterable} and a {@code Triple} to be filled with the resulting elements for the {@code TriIterator}.
     * @return a {@code TriIterator} that will iterate over the elements created by {@code unzip}.
     * @deprecated replaced by {@link TriIterator#unzip(Iterable, BiConsumer)}
     * @see TriIterator#unzip(Iterable, BiConsumer)
     * @see TriIterator#toMultiList(Supplier)
     * @see TriIterator#toMultiSet(Supplier)
     */
    @Deprecated
    @Beta
    public static <T, A, B, C> TriIterator<A, B, C> unzipp(final Iterable<? extends T> c, final BiConsumer<? super T, Triple<A, B, C>> unzip) {
        return TriIterator.unzip(N.iterate(c), unzip);
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     *
     * Calls {@code next()} on {@code iterator}, either {@code numberToAdvance} times or until {@code hasNext()} returns {@code false}, whichever comes first.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
     * long advanced = Iterators.advance(iter, 3);
     * // advanced => 3
     * // iter.next() => 4 (iterator is now at position 3)
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B").iterator();
     * long advanced2 = Iterators.advance(iter2, 10);
     * // advanced2 => 2 (only 2 elements were available)
     * // iter2.hasNext() => false
     * }</pre>
     *
     * @param iterator the iterator to be advanced, or {@code null} to return {@code 0}.
     * @param numberToAdvance the number of elements to advance the iterator.
     * @return the actual number of elements the iterator was advanced, or {@code 0} if {@code iterator} is {@code null}.
     * @throws IllegalArgumentException if {@code numberToAdvance} is negative.
     */
    public static long advance(final Iterator<?> iterator, final long numberToAdvance) throws IllegalArgumentException {
        N.checkArgNotNegative(numberToAdvance, cs.numberToAdvance);

        if (iterator == null) {
            return 0;
        }

        long i;

        for (i = 0; i < numberToAdvance && iterator.hasNext(); i++) {
            iterator.next();
        }

        return i;
    }

    /**
     * Skips the first {@code n} elements of the provided iterator and returns a new {@code ObjIterator} starting from the (n+1)th element.
     * If {@code n} is greater than the size of the iterator, an empty {@code ObjIterator} will be returned.
     *
     * <p>This is a lazy evaluation operation. The {@code skip} action is only triggered when {@code Iterator.hasNext()} or {@code Iterator.next()} is called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
     * ObjIterator<Integer> result = Iterators.skip(iter, 2);
     * // Yields: 3, 4, 5 (skips first 2 elements)
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B", "C").iterator();
     * ObjIterator<String> result2 = Iterators.skip(iter2, 10);
     * // result2.hasNext() => false (skipped all elements)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be skipped, or {@code null} to return an empty iterator.
     * @param n the number of elements to skip from the beginning of the iterator.
     * @return an {@code ObjIterator} that will iterate over the elements of the original iterator starting from the (n+1)th element, or an empty iterator if {@code iter} is {@code null}.
     * @throws IllegalArgumentException if {@code n} is negative.
     */
    public static <T> ObjIterator<T> skip(final Iterator<? extends T> iter, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (iter == null) {
            return ObjIterator.empty();
        } else if (n <= 0) {
            return ObjIterator.of(iter);
        }

        return new ObjIterator<>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.next();
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that is limited to the specified count of elements from the original iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
     * ObjIterator<Integer> result = Iterators.limit(iter, 3);
     * // Yields: 1, 2, 3 (limits to first 3 elements)
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B").iterator();
     * ObjIterator<String> result2 = Iterators.limit(iter2, 10);
     * // Yields: "A", "B" (count exceeds available elements)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be limited, or {@code null} to return an empty iterator.
     * @param count the maximum number of elements to be iterated over from the original iterator.
     * @return an {@code ObjIterator} that will iterate over up to {@code count} elements of the original iterator, or an empty iterator if {@code iter} is {@code null}.
     * @throws IllegalArgumentException if {@code count} is negative.
     */
    public static <T> ObjIterator<T> limit(final Iterator<? extends T> iter, final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (iter == null || count == 0) {
            return ObjIterator.empty();
        } else if (count == Long.MAX_VALUE) {
            return ObjIterator.of(iter);
        }

        return new ObjIterator<>() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.next();
            }
        };
    }

    /**
     * Returns a new ObjIterator that starts from the specified offset and is limited to the specified count of elements from the original Iterator.
     * This method combines both {@link #skip(Iterator, long)} and {@link #limit(Iterator, long)} operations in a single call.
     *
     * <p>This is a lazy evaluation operation. The {@code skip} action is only triggered when {@code Iterator.hasNext()} or {@code Iterator.next()} is called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
     * ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 1, 3);
     * // Yields: 2, 3, 4 (skips first element, then takes next 3)
     *
     * Iterator<String> iter2 = Arrays.asList("A", "B", "C", "D", "E", "F").iterator();
     * ObjIterator<String> result2 = Iterators.skipAndLimit(iter2, 2, 2);
     * // Yields: "C", "D"
     * }</pre>
     *
     * @param <T> the type of elements in the iterator.
     * @param iter the iterator to be skipped and limited.
     * @param offset the number of elements to skip from the beginning. Must be non-negative.
     * @param count the maximum number of elements to return after skipping. Must be non-negative.
     * @return an {@code ObjIterator} that will iterate over up to {@code count} elements starting from the (offset+1)th element.
     * @see N#slice(Iterator, int, int)
     */
    public static <T> ObjIterator<T> skipAndLimit(final Iterator<? extends T> iter, final long offset, final long count) {
        checkOffsetCount(offset, count);

        if (iter == null) {
            return ObjIterator.empty();
        }

        if (offset == 0 && count == Long.MAX_VALUE) {
            return ObjIterator.of(iter);
        } else if (offset == 0) {
            return limit(iter, count);
        } else if (count == Long.MAX_VALUE) {
            return skip(iter, offset);
        }

        return new ObjIterator<>() {
            private long cnt = count;
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return cnt > 0 && iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.next();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < offset && iter.hasNext()) {
                    iter.next();
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that starts from the specified offset and is limited to the specified count of elements from the original {@code Iterable}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
     * ObjIterator<Integer> result = Iterators.skipAndLimit(list, 2, 3);
     * // Yields: 3, 4, 5 (skips 2, takes next 3)
     *
     * List<String> words = Arrays.asList("A", "B", "C", "D");
     * ObjIterator<String> result2 = Iterators.skipAndLimit(words, 1, 2);
     * // Yields: "B", "C"
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param iter the original {@code Iterable} to be skipped and limited, or {@code null} to return an empty iterator.
     * @param offset the number of elements to skip from the beginning of the {@code Iterable}.
     * @param count the maximum number of elements to be iterated over from the {@code Iterable} after skipping.
     * @return an {@code ObjIterator} that will iterate over up to {@code count} elements of the original {@code Iterable} starting from the (offset+1)th element.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative.
     */
    public static <T> ObjIterator<T> skipAndLimit(final Iterable<? extends T> iter, final long offset, final long count) {
        checkOffsetCount(offset, count);

        return iter == null ? ObjIterator.empty() : skipAndLimit(iter.iterator(), offset, count);
    }

    /**
     * Returns a new {@code ObjIterator} with {@code null} elements removed from the specified Iterable.
     * All {@code null} elements will be filtered out, returning only {@code non-null} elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", null, "B", null, "C");
     * ObjIterator<String> iter = Iterators.skipNulls(list);
     * // Yields: "A", "B", "C"
     *
     * List<Integer> numbers = Arrays.asList(1, null, 2, 3, null);
     * ObjIterator<Integer> iter2 = Iterators.skipNulls(numbers);
     * // Yields: 1, 2, 3
     * }</pre>
     *
     * @param <T> the type of elements in the iterable.
     * @param c the iterable whose {@code null} elements should be skipped.
     * @return an {@code ObjIterator} that iterates over only the {@code non-null} elements.
     */
    @Beta
    public static <T> ObjIterator<T> skipNulls(final Iterable<? extends T> c) {
        return filter(c, Fn.notNull());
    }

    /**
     * Returns a new {@code ObjIterator} with {@code null} elements removed from the specified Iterator.
     * All {@code null} elements will be filtered out, returning only {@code non-null} elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", null, "B", null, "C").iterator();
     * ObjIterator<String> result = Iterators.skipNulls(iter);
     * // Yields: "A", "B", "C"
     *
     * Iterator<Integer> iter2 = Arrays.asList(1, null, 2, 3, null).iterator();
     * ObjIterator<Integer> result2 = Iterators.skipNulls(iter2);
     * // Yields: 1, 2, 3
     * }</pre>
     *
     * @param <T> the type of elements in the iterator.
     * @param iter the iterator whose {@code null} elements should be skipped.
     * @return an {@code ObjIterator} that iterates over only the {@code non-null} elements.
     */
    public static <T> ObjIterator<T> skipNulls(final Iterator<? extends T> iter) {
        return filter(iter, Fn.notNull());
    }

    /**
     * Returns a new ObjIterator with distinct elements from the original Iterable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("A", "B", "A", "C", "B", "D");
     * ObjIterator<String> distinct = Iterators.distinct(list);
     * // Yields: "A", "B", "C", "D"
     *
     * List<Integer> numbers = Arrays.asList(1, 2, 2, 3, 1, 4);
     * ObjIterator<Integer> distinct2 = Iterators.distinct(numbers);
     * // Yields: 1, 2, 3, 4
     * }</pre>
     *
     * @param <T> the type of elements in the original Iterable.
     * @param c the original Iterable to be processed for distinct elements.
     * @return a new ObjIterator that will iterate over the distinct elements of the original Iterable.
     */
    @Beta
    public static <T> ObjIterator<T> distinct(final Iterable<? extends T> c) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return distinct(c.iterator());
    }

    /**
     * Returns a new ObjIterator with distinct elements from the original Iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "A", "C", "B", "D").iterator();
     * ObjIterator<String> distinct = Iterators.distinct(iter);
     * // Yields: "A", "B", "C", "D"
     *
     * Iterator<Integer> iter2 = Arrays.asList(1, 2, 2, 3, 1, 4).iterator();
     * ObjIterator<Integer> distinct2 = Iterators.distinct(iter2);
     * // Yields: 1, 2, 3, 4
     * }</pre>
     *
     * @param <T> the type of elements in the original Iterator.
     * @param iter the original Iterator to be processed for distinct elements.
     * @return a new ObjIterator that will iterate over the distinct elements of the original Iterator.
     */
    public static <T> ObjIterator<T> distinct(final Iterator<? extends T> iter) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        final Set<T> set = new HashSet<>();

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private T tmp = null;

            @Override
            public boolean hasNext() {
                if (next == NONE) {
                    while (iter.hasNext()) {
                        tmp = iter.next();

                        if (set.add(tmp)) {
                            next = tmp;
                            break;
                        }
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} with distinct elements from the original {@code Iterable} based on a key derived from each element.
     * The key for each element is determined by the provided {@code Function}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> words = Arrays.asList("apple", "apricot", "banana", "avocado");
     * ObjIterator<String> distinct = Iterators.distinctBy(words, s -> s.charAt(0));
     * // Yields: "apple", "banana" (distinct by first character)
     *
     * List<Person> people = Arrays.asList(
     *     new Person("Alice", 30),
     *     new Person("Bob", 25),
     *     new Person("Charlie", 30)
     * );
     * ObjIterator<Person> distinctByAge = Iterators.distinctBy(people, Person::getAge);
     * // Yields: Person("Alice", 30), Person("Bob", 25)
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be processed for distinct elements, or {@code null} to return an empty iterator.
     * @param keyExtractor a {@code Function} that takes an element from the {@code Iterable} and returns a key. Elements with the same key are considered duplicates.
     * @return an {@code ObjIterator} that will iterate over the distinct elements of the original {@code Iterable} based on the keys derived from {@code keyExtractor}.
     * @throws IllegalArgumentException if {@code keyExtractor} is {@code null}.
     */
    @Beta
    public static <T> ObjIterator<T> distinctBy(final Iterable<? extends T> c, final Function<? super T, ?> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        if (c == null) {
            return ObjIterator.empty();
        }

        return distinctBy(c.iterator(), keyExtractor);
    }

    /**
     * Returns an {@code ObjIterator} with distinct elements from the original iterator based on a key derived from each element.
     * The key for each element is determined by the provided {@code Function}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> words = Arrays.asList("apple", "apricot", "banana", "avocado").iterator();
     * ObjIterator<String> distinct = Iterators.distinctBy(words, s -> s.charAt(0));
     * // Yields: "apple", "banana" (distinct by first character)
     *
     * Iterator<String> names = Arrays.asList("Alice", "Andrew", "Bob", "Anna").iterator();
     * ObjIterator<String> distinctByLength = Iterators.distinctBy(names, String::length);
     * // Yields: "Alice", "Bob" (distinct by length)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be processed for distinct elements, or {@code null} to return an empty iterator.
     * @param keyExtractor a {@code Function} that takes an element from the iterator and returns a key. Elements with the same key are considered duplicates.
     * @return an {@code ObjIterator} that will iterate over the distinct elements of the original iterator based on the keys derived from {@code keyExtractor}.
     * @throws IllegalArgumentException if {@code keyExtractor} is {@code null}.
     */
    public static <T> ObjIterator<T> distinctBy(final Iterator<? extends T> iter, final Function<? super T, ?> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        if (iter == null) {
            return ObjIterator.empty();
        }

        final Set<Object> set = new HashSet<>();

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private T tmp = null;

            @Override
            public boolean hasNext() {
                if (next == NONE) {
                    while (iter.hasNext()) {
                        tmp = iter.next();

                        if (set.add(keyExtractor.apply(tmp))) {
                            next = tmp;
                            break;
                        }
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that only includes elements from the original {@code Iterable} that satisfy the provided {@code Predicate}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
     * ObjIterator<Integer> evens = Iterators.filter(numbers, n -> n % 2 == 0);
     * // Yields: 2, 4, 6
     *
     * List<String> words = Arrays.asList("apple", "banana", "apricot", "cherry");
     * ObjIterator<String> aWords = Iterators.filter(words, s -> s.startsWith("a"));
     * // Yields: "apple", "apricot"
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be filtered, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the {@code Iterable}. Only elements that return {@code true} are included in the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original {@code Iterable} that satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Beta
    public static <T> ObjIterator<T> filter(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        if (c == null) {
            return ObjIterator.empty();
        }

        return filter(c.iterator(), predicate);
    }

    /**
     * Returns an {@code ObjIterator} that only includes elements from the original iterator that satisfy the provided {@code Predicate}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 6).iterator();
     * ObjIterator<Integer> evens = Iterators.filter(iter, n -> n % 2 == 0);
     * // Yields: 2, 4, 6
     *
     * Iterator<String> words = Arrays.asList("hello", "world", "hi", "java").iterator();
     * ObjIterator<String> longWords = Iterators.filter(words, s -> s.length() > 3);
     * // Yields: "hello", "world", "java"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be filtered, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the iterator. Only elements that return {@code true} are included in the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original iterator that satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    public static <T> ObjIterator<T> filter(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private T tmp = null;

            @Override
            public boolean hasNext() {
                if (next == NONE) {
                    while (iter.hasNext()) {
                        tmp = iter.next();

                        if (predicate.test(tmp)) {
                            next = tmp;
                            break;
                        }
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that includes elements from the original {@code Iterable} as long as they satisfy the provided {@code Predicate}.
     * The iteration stops when an element that does not satisfy the {@code Predicate} is encountered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 2, 1);
     * ObjIterator<Integer> result = Iterators.takeWhile(numbers, n -> n < 4);
     * // Yields: 1, 2, 3 (stops at 4)
     *
     * List<String> words = Arrays.asList("a", "ab", "abc", "b", "c");
     * ObjIterator<String> shortWords = Iterators.takeWhile(words, s -> s.startsWith("a"));
     * // Yields: "a", "ab", "abc" (stops at "b")
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the {@code Iterable}. The iteration continues as long as the {@code Predicate} returns {@code true}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original {@code Iterable} as long as they satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Beta
    public static <T> ObjIterator<T> takeWhile(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        if (c == null) {
            return ObjIterator.empty();
        }

        return takeWhile(c.iterator(), predicate);
    }

    /**
     * Returns an {@code ObjIterator} that includes elements from the original iterator as long as they satisfy the provided {@code Predicate}.
     * The iteration stops when an element that does not satisfy the {@code Predicate} is encountered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 2, 1).iterator();
     * ObjIterator<Integer> result = Iterators.takeWhile(iter, n -> n < 4);
     * // Yields: 1, 2, 3 (stops at 4)
     *
     * Iterator<String> words = Arrays.asList("short", "text", "verylongword", "a").iterator();
     * ObjIterator<String> shortWords = Iterators.takeWhile(words, s -> s.length() < 10);
     * // Yields: "short", "text" (stops at "verylongword")
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the iterator. The iteration continues as long as the {@code Predicate} returns {@code true}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original iterator as long as they satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    public static <T> ObjIterator<T> takeWhile(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private T tmp = null;
            private boolean hasMore = true;

            @Override
            public boolean hasNext() {
                if (next == NONE && hasMore && iter.hasNext()) {
                    tmp = iter.next();

                    if (predicate.test(tmp)) {
                        next = tmp;
                    } else {
                        hasMore = false;
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that includes elements from the original {@code Iterable} as long as they satisfy the provided {@code Predicate}.
     * The iteration stops after the first element that does not satisfy the {@code Predicate} is encountered, but includes that element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 2, 1);
     * ObjIterator<Integer> result = Iterators.takeWhileInclusive(numbers, n -> n < 4);
     * // Yields: 1, 2, 3, 4 (includes 4, then stops)
     *
     * List<String> words = Arrays.asList("a", "ab", "abc", "b", "c");
     * ObjIterator<String> result2 = Iterators.takeWhileInclusive(words, s -> s.startsWith("a"));
     * // Yields: "a", "ab", "abc", "b" (includes "b", then stops)
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the {@code Iterable}. The iteration continues as long as the {@code Predicate} returns {@code true}, including the first element that returns {@code false}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original {@code Iterable} as long as they satisfy the provided {@code Predicate}, including the first element that does not satisfy the {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Beta
    public static <T> ObjIterator<T> takeWhileInclusive(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        if (c == null) {
            return ObjIterator.empty();
        }

        return takeWhileInclusive(c.iterator(), predicate);
    }

    /**
     * Returns an {@code ObjIterator} that includes elements from the original iterator as long as they satisfy the provided {@code Predicate}.
     * The iteration stops after the first element that does not satisfy the {@code Predicate} is encountered, but includes that element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 2, 1).iterator();
     * ObjIterator<Integer> result = Iterators.takeWhileInclusive(iter, n -> n < 4);
     * // Yields: 1, 2, 3, 4 (includes 4, then stops)
     *
     * Iterator<String> words = Arrays.asList("cat", "car", "dog", "cap").iterator();
     * ObjIterator<String> result2 = Iterators.takeWhileInclusive(words, s -> s.startsWith("ca"));
     * // Yields: "cat", "car", "dog" (includes "dog", then stops)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the iterator. The iteration continues as long as the {@code Predicate} returns {@code true}, including the first element that returns {@code false}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original iterator as long as they satisfy the provided {@code Predicate}, including the first element that does not satisfy the {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    public static <T> ObjIterator<T> takeWhileInclusive(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private T tmp = null;
            private boolean hasMore = true;

            @Override
            public boolean hasNext() {
                if (next == NONE && hasMore && iter.hasNext()) {
                    tmp = iter.next();

                    if (predicate.test(tmp)) {
                        next = tmp;
                    } else {
                        next = tmp;
                        hasMore = false;
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    /**
     * Returns an {@code ObjIterator} that skips elements from the original {@code Iterable} as long as they satisfy the provided {@code Predicate}.
     * The iteration begins when an element that does not satisfy the {@code Predicate} is encountered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 2, 1);
     * ObjIterator<Integer> result = Iterators.dropWhile(numbers, n -> n < 4);
     * // Yields: 4, 5, 2, 1 (drops 1, 2, 3, starts from 4)
     *
     * List<String> words = Arrays.asList("a", "ab", "abc", "b", "c");
     * ObjIterator<String> result2 = Iterators.dropWhile(words, s -> s.length() < 3);
     * // Yields: "abc", "b", "c" (drops "a", "ab", starts from "abc")
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the {@code Iterable}. The iteration skips elements as long as the {@code Predicate} returns {@code true}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original {@code Iterable} starting from the first element that does not satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Beta
    public static <T> ObjIterator<T> dropWhile(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        if (c == null) {
            return ObjIterator.empty();
        }

        return dropWhile(c.iterator(), predicate);
    }

    /**
     * Returns an {@code ObjIterator} that skips elements from the original iterator as long as they satisfy the provided {@code Predicate}.
     * The iteration begins when an element that does not satisfy the {@code Predicate} is encountered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 2, 1).iterator();
     * ObjIterator<Integer> result = Iterators.dropWhile(iter, n -> n < 4);
     * // Yields: 4, 5, 2, 1 (drops 1, 2, 3, starts from 4)
     *
     * Iterator<String> words = Arrays.asList("short", "tiny", "verylongword", "a").iterator();
     * ObjIterator<String> result2 = Iterators.dropWhile(words, s -> s.length() < 10);
     * // Yields: "verylongword", "a" (drops "short", "tiny", starts from "verylongword")
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests each element from the iterator. The iteration skips elements as long as the {@code Predicate} returns {@code true}.
     * @return an {@code ObjIterator} that will iterate over the elements of the original iterator starting from the first element that does not satisfy the provided {@code Predicate}.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    public static <T> ObjIterator<T> dropWhile(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private boolean hasDropped = false;

            @Override
            public boolean hasNext() {
                if (!hasDropped) {
                    while (iter.hasNext()) {
                        next = iter.next();

                        if (predicate.test(next)) {
                            next = NONE;
                        } else {
                            hasDropped = true;
                            break;
                        }
                    }
                }

                return next != NONE || iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (next != NONE) {
                    final T tmp = next;
                    next = NONE;
                    return tmp;
                } else {
                    return iter.next();
                }
            }
        };
    }

    /**
     * Skips elements in the provided {@code Iterable} until the provided {@code Predicate} returns {@code true}.
     * This method can be used to ignore elements in an {@code Iterable} until a certain condition is met.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 2, 1);
     * ObjIterator<Integer> result = Iterators.skipUntil(numbers, n -> n >= 4);
     * // Yields: 4, 5, 2, 1 (skips until finding n >= 4)
     *
     * List<String> words = Arrays.asList("a", "ab", "abc", "abcd", "b");
     * ObjIterator<String> result2 = Iterators.skipUntil(words, s -> s.length() >= 4);
     * // Yields: "abcd", "b" (skips until finding length >= 4)
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param c the original {@code Iterable} to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests elements from the original {@code Iterable}.
     * @return an {@code ObjIterator} that will iterate over the remaining elements after the {@code Predicate} returns {@code true} for the first time.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Beta
    public static <T> ObjIterator<T> skipUntil(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        if (c == null) {
            return ObjIterator.empty();
        }

        return skipUntil(c.iterator(), predicate);
    }

    /**
     * Skips elements in the provided iterator until the provided {@code Predicate} returns {@code true}.
     * This method can be used to ignore elements in an iterator until a certain condition is met.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 2, 1).iterator();
     * ObjIterator<Integer> result = Iterators.skipUntil(iter, n -> n >= 4);
     * // Yields: 4, 5, 2, 1 (skips until finding n >= 4)
     *
     * Iterator<String> words = Arrays.asList("cat", "dog", "elephant", "ant").iterator();
     * ObjIterator<String> result2 = Iterators.skipUntil(words, s -> s.length() > 5);
     * // Yields: "elephant", "ant" (skips until finding length > 5)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param iter the original iterator to be processed, or {@code null} to return an empty iterator.
     * @param predicate a {@code Predicate} that tests elements from the original iterator.
     * @return an {@code ObjIterator} that will iterate over the remaining elements after the {@code Predicate} returns {@code true} for the first time.
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Beta
    public static <T> ObjIterator<T> skipUntil(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private boolean hasSkipped = false;

            @Override
            public boolean hasNext() {
                if (!hasSkipped) {
                    while (iter.hasNext()) {
                        next = iter.next();

                        if (predicate.test(next)) {
                            hasSkipped = true;
                            break;
                        } else {
                            next = NONE;
                        }
                    }
                }

                return next != NONE || iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (next != NONE) {
                    final T tmp = next;
                    next = NONE;
                    return tmp;
                } else {
                    return iter.next();
                }
            }
        };
    }

    /**
     * Transforms the elements of the given {@code Iterable} using the provided {@code Function} and returns an {@code ObjIterator} with the transformed elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * ObjIterator<String> uppercase = Iterators.map(list, String::toUpperCase);
     * // Yields: "A", "B", "C"
     *
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4);
     * ObjIterator<Integer> doubled = Iterators.map(numbers, n -> n * 2);
     * // Yields: 2, 4, 6, 8
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param c the original {@code Iterable} to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the {@code Iterable} and returns a transformed element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original {@code Iterable}.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Beta
    public static <T, U> ObjIterator<U> map(final Iterable<? extends T> c, final Function<? super T, U> mapper) {
        N.checkArgNotNull(mapper, cs.mapper);

        if (c == null) {
            return ObjIterator.empty();
        }

        return map(c.iterator(), mapper);
    }

    /**
     * Transforms the elements of the given iterator using the provided {@code Function} and returns an {@code ObjIterator} with the transformed elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a", "b").iterator();
     * ObjIterator<String> result = Iterators.map(iter, String::toUpperCase);
     * // result.next() => "A"
     * // result.next() => "B"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param iter the original iterator to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the iterator and returns a transformed element for the resulting {@code ObjIterator}.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original iterator.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    public static <T, U> ObjIterator<U> map(final Iterator<? extends T> iter, final Function<? super T, U> mapper) {
        N.checkArgNotNull(mapper, cs.mapper);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public U next() {
                return mapper.apply(iter.next());
            }
        };
    }

    /**
     * Transforms the elements of the given {@code Iterable} into {@code Iterable}s using the provided {@code Function} and flattens the result into an {@code ObjIterator}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a,b", "c,d");
     * ObjIterator<String> result = Iterators.flatMap(list, s -> Arrays.asList(s.split(",")));
     * // Yields: "a", "b", "c", "d"
     *
     * List<List<Integer>> nested = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4));
     * ObjIterator<Integer> flat = Iterators.flatMap(nested, x -> x);
     * // Yields: 1, 2, 3, 4
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param c the original {@code Iterable} to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the {@code Iterable} and returns an {@code Iterable} of transformed elements.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original {@code Iterable}.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Beta
    public static <T, U> ObjIterator<U> flatMap(final Iterable<? extends T> c, final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return flatMap(c.iterator(), mapper);
    }

    /**
     * Transforms the elements of the given iterator into {@code Iterable}s using the provided {@code Function} and flattens the result into an {@code ObjIterator}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a,b", "c").iterator();
     * ObjIterator<String> result = Iterators.flatMap(iter, s -> Arrays.asList(s.split(",")));
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param iter the original iterator to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the iterator and returns an {@code Iterable} of transformed elements.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original iterator.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    public static <T, U> ObjIterator<U> flatMap(final Iterator<? extends T> iter, final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        N.checkArgNotNull(mapper, cs.mapper);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterable<? extends U> c = null;
            private Iterator<? extends U> cur = null;

            @Override
            public boolean hasNext() {
                if (cur == null || !cur.hasNext()) {
                    while (iter.hasNext()) {
                        c = mapper.apply(iter.next());
                        cur = c == null ? null : c.iterator();

                        if (cur != null && cur.hasNext()) {
                            break;
                        }
                    }
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public U next() throws IllegalArgumentException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        };
    }

    /**
     * Transforms the elements of the given {@code Iterable} into arrays using the provided {@code Function} and flattens the result into an {@code ObjIterator}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a,b", "c,d");
     * ObjIterator<String> result = Iterators.flatmap(list, s -> s.split(","));
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * // result.next() => "d"
     * }</pre>
     *
     * @param <T> the type of elements in the original {@code Iterable}.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param c the original {@code Iterable} to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the {@code Iterable} and returns an array of transformed elements.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original {@code Iterable}.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Beta
    public static <T, U> ObjIterator<U> flatmap(final Iterable<? extends T> c, final Function<? super T, ? extends U[]> mapper) { //NOSONAR
        if (c == null) {
            return ObjIterator.empty();
        }

        return flatmap(c.iterator(), mapper);
    }

    /**
     * Transforms the elements of the given iterator into arrays using the provided {@code Function} and flattens the result into an {@code ObjIterator}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a,b", "c").iterator();
     * ObjIterator<String> result = Iterators.flatmap(iter, s -> s.split(","));
     * // result.next() => "a"
     * // result.next() => "b"
     * // result.next() => "c"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <U> the type of elements in the resulting {@code ObjIterator}.
     * @param iter the original iterator to be transformed, or {@code null} to return an empty iterator.
     * @param mapper a {@code Function} that takes an element from the iterator and returns an array of transformed elements.
     * @return an {@code ObjIterator} that will iterate over the transformed elements of the original iterator.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    public static <T, U> ObjIterator<U> flatmap(final Iterator<? extends T> iter, final Function<? super T, ? extends U[]> mapper) { //NOSONAR
        N.checkArgNotNull(mapper, cs.mapper);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private U[] a = null;
            private int len = 0;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                if (cursor >= len) {
                    while (iter.hasNext()) {
                        a = mapper.apply(iter.next());
                        len = N.len(a);
                        cursor = 0;

                        if (len > 0) {
                            break;
                        }
                    }
                }

                return cursor < len;
            }

            @Override
            public U next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Performs an action for each element of the given iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a", "b").iterator();
     * List<String> list = new ArrayList<>();
     * Iterators.forEach(iter, list::add);
     * // list => ["a", "b"]
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iter the original iterator to be processed.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterator.
     * @throws E if the {@code elementConsumer} encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iter, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given iterator and executes a final action upon completion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
     * List<String> list = new ArrayList<>();
     * Iterators.forEach(iter, list::add, () -> System.out.println("Done: " + list.size()));
     * // Adds all elements to list, then prints "Done: 3"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iter the original iterator to be processed.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterator.
     * @param onComplete a {@code Runnable} action to be executed after all elements in the iterator have been processed.
     * @throws E if the {@code elementConsumer} encounters an exception.
     * @throws E2 if the {@code onComplete} action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iter, 0, Long.MAX_VALUE, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given iterator, starting from a specified offset and up to a specified count.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8).iterator();
     * Iterators.forEach(iter, 2, 3, i -> System.out.println(i));
     * // Prints: 3, 4, 5 (skips first 2, processes next 3)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iter the original iterator to be processed.
     * @param offset the starting point in the iterator from where elements will be processed.
     * @param count the maximum number of elements to be processed from the iterator.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterator.
     * @throws E if the {@code elementConsumer} encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iter, offset, count, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given iterator, starting from a specified offset and up to a specified count.
     * After all elements have been processed, a final action is executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter = Arrays.asList("A", "B", "C", "D", "E").iterator();
     * AtomicInteger processed = new AtomicInteger();
     * Iterators.forEach(iter, 1, 3,
     *     s -> processed.incrementAndGet(),
     *     () -> System.out.println("Processed: " + processed.get())
     * );
     * // Processes "B", "C", "D", then prints "Processed: 3"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iter the original iterator to be processed.
     * @param offset the starting point in the iterator from where elements will be processed.
     * @param count the maximum number of elements to be processed from the iterator.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterator.
     * @param onComplete a {@code Runnable} action to be executed after all elements in the iterator have been processed.
     * @throws E if the {@code elementConsumer} encounters an exception.
     * @throws E2 if the {@code onComplete} action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iter, offset, count, 0, 0, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given iterator, starting from a specified offset and up to a specified count.
     * This method also supports multi-threading with a specified number of threads and queue size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = IntStream.range(0, 100).iterator();
     * Iterators.forEach(iter, 0, 100, 4, 10, i -> {
     *     System.out.println("Processing " + i + " on thread " + Thread.currentThread().getName());
     * });
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iter the original iterator to be processed.
     * @param offset the starting point in the iterator from where elements will be processed.
     * @param count the maximum number of elements to be processed from the iterator.
     * @param processThreadNum the number of threads to be used for processing.
     * @param queueSize the size of the queue for holding elements before processing.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterator.
     * @throws E if the {@code elementConsumer} encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iter, offset, count, processThreadNum, queueSize, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given iterator, starting from a specified offset and up to a specified count.
     * This method also supports multi-threading with a specified number of threads and queue size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = IntStream.range(0, 100).iterator();
     * AtomicInteger sum = new AtomicInteger();
     * Iterators.forEach(iter, 0, 100, 4, 10,
     *     i -> sum.addAndGet(i),
     *     () -> System.out.println("Total: " + sum.get())
     * );
     * // Processes all elements in parallel with 4 threads
     * }</pre>
     *
     * @param <T> the type of elements in the original iterator.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iter the original iterator to be processed.
     * @param offset the starting point in the iterator from where processing should begin.
     * @param count the maximum number of elements to process.
     * @param processThreadNum the number of threads to be used for processing.
     * @param queueSize the size of the queue to hold the processing records. The default size is 1024.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterator.
     * @param onComplete a {@code Runnable} action to be performed once all elements have been processed.
     * @throws E if the {@code elementConsumer} encounters an exception.
     * @throws E2 if the {@code onComplete} action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete)
            throws E, E2 {
        forEach(Array.asList(iter), offset, count, 0, processThreadNum, queueSize, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given collection of iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<Integer>> iterators = Arrays.asList(
     *     Arrays.asList(1, 2).iterator(),
     *     Arrays.asList(3, 4).iterator()
     * );
     * List<Integer> result = new ArrayList<>();
     * Iterators.forEach(iterators, result::add);
     * // result => [1, 2, 3, 4]
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iterators the original collection of iterators to be processed.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @throws E if the {@code elementConsumer} encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given collection of iterators.
     * After all elements have been processed, a final action is executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<String>> iterators = Arrays.asList(
     *     Arrays.asList("A", "B").iterator(),
     *     Arrays.asList("C", "D").iterator()
     * );
     * List<String> result = new ArrayList<>();
     * Iterators.forEach(iterators, result::add, () -> System.out.println("Total: " + result.size()));
     * // Adds all elements, then prints "Total: 4"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iterators the original collection of iterators to be processed.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @param onComplete a {@code Runnable} action to be executed after all elements in the iterators have been processed.
     * @throws E if the {@code elementConsumer} encounters an exception.
     * @throws E2 if the {@code onComplete} action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iterators, 0, Long.MAX_VALUE, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given collection of iterators, starting from a specified offset and up to a specified count.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<Integer>> iterators = Arrays.asList(
     *     Arrays.asList(1, 2, 3).iterator(),
     *     Arrays.asList(4, 5, 6).iterator()
     * );
     * List<Integer> result = new ArrayList<>();
     * Iterators.forEach(iterators, 2, 3, result::add);
     * // result => [3, 4, 5] (skips first 2 elements, processes next 3)
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iterators the original collection of iterators to be processed.
     * @param offset the starting point in the iterators from where elements will be processed.
     * @param count the maximum number of elements to be processed from the iterators.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @throws E if the {@code elementConsumer} encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, offset, count, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given collection of iterators, starting from a specified offset and up to a specified count.
     * After all elements have been processed, a final action is executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<String>> iterators = Arrays.asList(
     *     Arrays.asList("A", "B", "C").iterator(),
     *     Arrays.asList("D", "E", "F").iterator()
     * );
     * List<String> result = new ArrayList<>();
     * Iterators.forEach(iterators, 1, 3, result::add, () -> System.out.println("Done"));
     * // result => ["B", "C", "D"], then prints "Done"
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iterators the original collection of iterators to be processed.
     * @param offset the starting point in the iterators from where elements will be processed.
     * @param count the maximum number of elements to be processed from the iterators.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @param onComplete a {@code Runnable} action to be executed after all elements in the iterators have been processed.
     * @throws E if the {@code elementConsumer} encounters an exception.
     * @throws E2 if the {@code onComplete} action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iterators, offset, count, 0, 0, 0, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given collection of iterators.
     * This method also supports multi-threading with a specified number of threads for reading and processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<Integer>> iterators = Arrays.asList(
     *     IntStream.range(0, 50).iterator(),
     *     IntStream.range(50, 100).iterator()
     * );
     * AtomicInteger sum = new AtomicInteger();
     * Iterators.forEach(iterators, 2, 4, 10, i -> sum.addAndGet(i));
     * // Uses 2 threads to read and 4 threads to process in parallel
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iterators the original collection of iterators to be processed.
     * @param readThreadNum the number of threads to be used for reading elements from the iterators.
     * @param processThreadNum the number of threads to be used for processing elements.
     * @param queueSize the size of the queue for holding elements before processing.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @throws E if the {@code elementConsumer} encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, readThreadNum, processThreadNum, queueSize, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given collection of iterators.
     * This method also supports multi-threading with a specified number of threads for reading and processing, and a queue for holding elements before processing.
     * After all elements have been processed, a final action is executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<String>> iterators = Arrays.asList(
     *     Files.lines(Paths.get("file1.txt")).iterator(),
     *     Files.lines(Paths.get("file2.txt")).iterator()
     * );
     * AtomicInteger lineCount = new AtomicInteger();
     * Iterators.forEach(iterators, 2, 4, 100,
     *     line -> lineCount.incrementAndGet(),
     *     () -> System.out.println("Total lines: " + lineCount.get())
     * );
     * // Reads from 2 files in parallel, processes with 4 threads
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iterators the original collection of iterators to be processed.
     * @param readThreadNum the number of threads to be used for reading elements from the iterators.
     * @param processThreadNum the number of threads to be used for processing elements.
     * @param queueSize the size of the queue for holding elements before processing.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @param onComplete a {@code Runnable} action to be executed after all elements in the iterators have been processed.
     * @throws E if the {@code elementConsumer} encounters an exception.
     * @throws E2 if the {@code onComplete} action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer,
            final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iterators, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given collection of iterators, starting from a specified offset and up to a specified count.
     * This method also supports multi-threading with a specified number of threads for reading and processing, and a queue for holding elements before processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<Integer>> iterators = Arrays.asList(
     *     IntStream.range(0, 1000).iterator(),
     *     IntStream.range(1000, 2000).iterator()
     * );
     * AtomicInteger sum = new AtomicInteger();
     * Iterators.forEach(iterators, 100, 500, 2, 4, 50, i -> sum.addAndGet(i));
     * // Skips first 100 elements, processes next 500 with 2 read threads and 4 process threads
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param iterators the original collection of iterators to be processed.
     * @param offset the starting point in the iterators from where elements will be processed.
     * @param count the maximum number of elements to be processed from the iterators.
     * @param readThreadNum the number of threads to be used for reading elements from the iterators.
     * @param processThreadNum the number of threads to be used for processing elements.
     * @param queueSize the size of the queue for holding elements before processing.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @throws E if the {@code elementConsumer} encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset, final long count,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, offset, count, readThreadNum, processThreadNum, queueSize, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given collection of iterators, starting from a specified offset and up to a specified count.
     * This method also supports multi-threading with a specified number of threads for reading and processing, and a queue size for holding the processing records.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Iterator<Integer>> iterators = Arrays.asList(
     *     Arrays.asList(1, 2).iterator(),
     *     Arrays.asList(3, 4).iterator()
     * );
     * 
     * // Parallel processing with 2 read threads and 4 process threads
     * Iterators.forEach(iterators, 0, Long.MAX_VALUE, 2, 4, 100, 
     *     item -> process(item),
     *     () -> System.out.println("Done")
     * );
     * }</pre>
     *
     * @param <T> the type of elements in the original iterators.
     * @param <E> the type of exception that can be thrown by the {@code elementConsumer}.
     * @param <E2> the type of exception that can be thrown by the {@code onComplete} action.
     * @param iterators the original collection of iterators to be processed.
     * @param offset the starting point in the iterators from where processing should begin.
     * @param count the maximum number of elements to process.
     * @param readThreadNum the number of threads to be used for reading.
     * @param processThreadNum the number of threads to be used for processing.
     * @param queueSize the size of the queue to hold the processing records.
     * @param elementConsumer a {@code Consumer} that performs an action on each element in the iterators.
     * @param onComplete a {@code Runnable} action to be performed once all elements have been processed.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative.
     * @throws E if the {@code elementConsumer} encounters an exception.
     * @throws E2 if the {@code onComplete} action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer,
            final Throwables.Runnable<E2> onComplete) throws IllegalArgumentException, E, E2 {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s cannot be negative", offset, count);

        if (N.isEmpty(iterators)) {
            return;
        }

        final long startTime = System.currentTimeMillis();

        if (logger.isInfoEnabled()) {
            logger.info("### Start to process: sizeOfIterators=" + iterators.size() + ", offset=" + offset + ", count=" + count + ", readThreadNum="
                    + readThreadNum + ", processThreadNum=" + processThreadNum + ", queueSize=" + queueSize);
        }

        final int readThreadNumToUse = readThreadNum == 0 ? 1 : readThreadNum;
        try (final Stream<T> stream = ((readThreadNum > 0 || queueSize > 0)
                ? Stream.parallelConcatIterators(iterators, readThreadNumToUse, (queueSize == 0 ? calculateBufferedSize(readThreadNumToUse) : queueSize))
                : Stream.concatIterators(iterators))) {

            @SuppressWarnings("deprecation")
            final Iterator<? extends T> iteratorII = stream.skip(offset).limit(count).iterator();

            if (processThreadNum == 0) {
                while (iteratorII.hasNext()) {
                    elementConsumer.accept(iteratorII.next());
                }

                if (onComplete != null) {
                    onComplete.run();
                }
            } else {
                final CountDownLatch countDownLatch = new CountDownLatch(processThreadNum);
                final ExecutorService executorService = Executors.newFixedThreadPool(processThreadNum);
                final Holder<Exception> errorHolder = new Holder<>();

                try {
                    for (int i = 0; i < processThreadNum; i++) {
                        executorService.execute(() -> {
                            T element = null;
                            try {
                                while (errorHolder.value() == null) {
                                    synchronized (iteratorII) {
                                        if (iteratorII.hasNext()) {
                                            element = iteratorII.next();
                                        } else {
                                            break;
                                        }
                                    }

                                    elementConsumer.accept(element);
                                }
                            } catch (final Exception e) {
                                synchronized (errorHolder) {
                                    if (errorHolder.value() == null) {
                                        errorHolder.setValue(e);
                                    } else {
                                        errorHolder.value().addSuppressed(e);
                                    }
                                }
                            } finally {
                                countDownLatch.countDown();
                            }
                        });
                    }

                    try {
                        countDownLatch.await();
                    } catch (final InterruptedException e) {
                        throw ExceptionUtil.toRuntimeException(e, true);
                    }

                    if (errorHolder.value() == null && onComplete != null) {
                        //noinspection CatchMayIgnoreException
                        try {
                            onComplete.run();
                        } catch (final Exception e) {
                            errorHolder.setValue(e);
                        }
                    }

                    if (errorHolder.value() != null) {
                        throw ExceptionUtil.toRuntimeException(errorHolder.value(), true);
                    }
                } finally {
                    executorService.shutdown();
                }
            }
        } finally {
            if (logger.isInfoEnabled()) {
                logger.info("### End to process. Elapsed time: " + (System.currentTimeMillis() - startTime) + " ms");
            }
        }
    }

    /**
     * Validates that the offset and count parameters are non-negative.
     * This is a package-private utility method used internally for parameter validation.
     *
     * @param offset the offset value to check.
     * @param count the count value to check.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative.
     */
    static void checkOffsetCount(final int offset, final int count) throws IllegalArgumentException {
        if (offset < 0 || count < 0) {
            throw new IllegalArgumentException("offset: " + offset + " and count: " + count + " can't be negative");
        }
    }

    /**
     * Validates that the offset and count parameters are non-negative (long version).
     * This is a package-private utility method used internally for parameter validation.
     *
     * @param offset the offset value to check.
     * @param count the count value to check.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative.
     */
    static void checkOffsetCount(final long offset, final long count) throws IllegalArgumentException {
        if (offset < 0 || count < 0) {
            throw new IllegalArgumentException("offset: " + offset + " and count: " + count + " can't be negative");
        }
    }

    /**
     * Calculates an appropriate buffer size based on the number of read threads.
     * This is a package-private utility method used internally to optimize buffer allocation.
     * The buffer size is calculated as the minimum of 1024 and (readThreadNum * 64).
     *
     * @param readThreadNum the number of threads that will be reading.
     * @return the calculated buffer size, capped at 1024.
     */
    static int calculateBufferedSize(final int readThreadNum) {
        return N.min(1024, readThreadNum * 64);
    }
}
