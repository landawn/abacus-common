/*
 * Copyright (c) 2015, Haiyang Li.
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

import java.io.Serial;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.IntFunction;

import com.landawn.abacus.annotation.Beta;

/**
 * An abstract base class that provides a comprehensive framework for implementing lists of primitive data types
 * with high-performance operations and extensive functionality. This class serves as the foundation for all
 * primitive list implementations in the abacus-common framework, offering optimized storage and operations that avoid
 * the boxing overhead associated with standard Java collections containing primitive wrapper objects.
 *
 * <p>PrimitiveList extends the concept of traditional collections by providing specialized implementations
 * for primitive types (boolean, byte, char, short, int, long, float, double) with type-safe operations,
 * memory-efficient storage, and performance-optimized algorithms. The class design follows the template
 * method pattern, defining the contract and common functionality while allowing concrete implementations
 * to optimize type-specific operations.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Zero-Boxing Overhead:</b> Direct primitive storage without wrapper object allocation</li>
 *   <li><b>Memory Efficiency:</b> Compact array-based storage with intelligent capacity management</li>
 *   <li><b>Type Safety:</b> Compile-time type checking with generic type parameters</li>
 *   <li><b>Rich API:</b> Comprehensive set of operations including sorting, searching, and set operations</li>
 *   <li><b>Performance Optimization:</b> Specialized algorithms optimized for primitive data types</li>
 *   <li><b>Collection Integration:</b> Seamless interoperability with standard Java collections</li>
 *   <li><b>Serialization Support:</b> Built-in serialization capabilities for persistence and transmission</li>
 *   <li><b>Random Access:</b> O(1) element access by index through RandomAccess interface</li>
 * </ul>
 *
 * <p><b>Generic Type Parameters:</b>
 * <ul>
 *   <li><b>B:</b> The boxed wrapper type (e.g., Integer for int, Double for double, Boolean for boolean)</li>
 *   <li><b>A:</b> The primitive array type (e.g., int[] for int, double[] for double, boolean[] for boolean)</li>
 *   <li><b>L:</b> The concrete list type extending this class (enabling fluent method chaining)</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>High-Performance Computing:</b> Numerical computations requiring minimal memory overhead</li>
 *   <li><b>Large Dataset Processing:</b> Efficient storage and manipulation of primitive arrays</li>
 *   <li><b>Mathematical Operations:</b> Vector and matrix operations with primitive data</li>
 *   <li><b>Data Analysis:</b> Statistical computations on large numeric datasets</li>
 *   <li><b>Game Development:</b> Coordinate systems, physics calculations, and rendering data</li>
 *   <li><b>Financial Systems:</b> Price data, trading algorithms, and risk calculations</li>
 *   <li><b>Scientific Computing:</b> Sensor data, experimental results, and simulations</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Creating and populating primitive lists
 * IntList numbers = IntList.of(1, 2, 3, 4, 5);
 * DoubleList prices = DoubleList.create();
 * prices.addAll(new double[]{19.99, 29.99, 39.99});
 *
 * // High-performance operations without boxing
 * int sum = numbers.sum();
 * double average = prices.average().orElse(0.0);
 * int max = numbers.max().orElse(Integer.MIN_VALUE);
 *
 * // Set operations for data analysis
 * IntList set1 = IntList.of(1, 2, 3, 4);
 * IntList set2 = IntList.of(3, 4, 5, 6);
 * IntList intersection = set1.intersection(set2); // [3, 4]
 * IntList difference = set1.difference(set2);     // [1, 2]
 *
 * // Efficient sorting and searching
 * numbers.sort();
 * boolean found = numbers.contains(3);
 * int index = numbers.binarySearch(4);
 *
 * // Conversion to standard collections when needed
 * List<Integer> boxedList = numbers.boxed();
 * Set<Integer> uniqueValues = numbers.toSet();
 * int[] primitiveArray = numbers.toArray();
 * }</pre>
 *
 * <p><b>Core Operations Categories:</b>
 * <ul>
 *   <li><b>Basic Operations:</b> add, remove, get, set, size, isEmpty, clear</li>
 *   <li><b>Bulk Operations:</b> addAll, removeAll, retainAll, containsAll</li>
 *   <li><b>Search Operations:</b> contains, indexOf, lastIndexOf, binarySearch</li>
 *   <li><b>Sorting Operations:</b> sort, reverseSort, isSorted</li>
 *   <li><b>Set Operations:</b> intersection, difference, symmetricDifference, disjoint</li>
 *   <li><b>Transformation:</b> reverse, rotate, shuffle, swap</li>
 *   <li><b>Range Operations:</b> subList, deleteRange, replaceRange</li>
 *   <li><b>Conversion:</b> toArray, boxed, toSet, toCollection</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Access:</b> O(1) random access by index</li>
 *   <li><b>Insertion:</b> O(1) amortized for append, O(n) for middle insertion</li>
 *   <li><b>Deletion:</b> O(1) for last element, O(n) for arbitrary position</li>
 *   <li><b>Search:</b> O(n) linear search, O(log n) binary search on sorted data</li>
 *   <li><b>Sorting:</b> O(n log n) using optimized primitive-specific algorithms</li>
 *   <li><b>Set Operations:</b> O(n) to O(n²) depending on algorithm selection</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li><b>Dynamic Capacity:</b> Automatic resizing with 1.75x growth factor</li>
 *   <li><b>Memory Efficiency:</b> Primitive arrays avoid object header overhead</li>
 *   <li><b>Capacity Optimization:</b> Intelligent initial sizing and trimming support</li>
 *   <li><b>Maximum Size:</b> Limited by {@code MAX_ARRAY_SIZE} for platform compatibility</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Not Thread-Safe:</b> Implementations are not synchronized by default</li>
 *   <li><b>External Synchronization:</b> Use Collections.synchronizedList() or manual synchronization</li>
 *   <li><b>Concurrent Access:</b> Undefined behavior under concurrent modification</li>
 *   <li><b>Read-Only Access:</b> Multiple threads can safely read without synchronization</li>
 * </ul>
 *
 * <p><b>Serialization Support:</b>
 * <ul>
 *   <li><b>Serializable Interface:</b> Implements {@link java.io.Serializable} for persistence</li>
 *   <li><b>Version Compatibility:</b> Stable serialVersionUID for version compatibility</li>
 *   <li><b>Custom Serialization:</b> Subclasses may implement custom serialization logic</li>
 *   <li><b>Cross-Platform:</b> Serialized form is platform-independent</li>
 * </ul>
 *
 * <p><b>Integration with Java Collections Framework:</b>
 * <ul>
 *   <li><b>RandomAccess:</b> Indicates efficient random access capabilities</li>
 *   <li><b>Boxed Conversion:</b> Seamless conversion to standard List&lt;B&gt;</li>
 *   <li><b>Collection Compatibility:</b> Works with Collections utility methods</li>
 *   <li><b>Stream Integration:</b> Can be converted to streams for functional processing</li>
 * </ul>
 *
 * <p><b>Advanced Set Operations:</b>
 * <ul>
 *   <li><b>Mathematical Sets:</b> Union, intersection, difference, symmetric difference</li>
 *   <li><b>Algorithm Selection:</b> Automatic choice between linear and hash-based algorithms</li>
 *   <li><b>Disjoint Testing:</b> Efficient checking for common elements</li>
 *   <li><b>Duplicate Handling:</b> Detection and removal of duplicate elements</li>
 * </ul>
 *
 * <p><b>Range Operations:</b>
 * <ul>
 *   <li><b>Subrange Processing:</b> Operations on specified index ranges</li>
 *   <li><b>Range Deletion:</b> Efficient bulk removal of elements</li>
 *   <li><b>Range Replacement:</b> Bulk replacement with another list or array</li>
 *   <li><b>Range Movement:</b> Efficient reordering of list segments</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>IndexOutOfBoundsException:</b> For invalid index access</li>
 *   <li><b>NoSuchElementException:</b> For operations on empty lists</li>
 *   <li><b>IllegalArgumentException:</b> For invalid method arguments</li>
 *   <li><b>OutOfMemoryError:</b> For capacity exceeding available memory</li>
 * </ul>
 *
 * <p><b>Capacity Management:</b>
 * <ul>
 *   <li><b>Initial Capacity:</b> Default capacity of 10 elements</li>
 *   <li><b>Growth Strategy:</b> 1.75x expansion factor for balanced performance</li>
 *   <li><b>Maximum Capacity:</b> Platform-specific maximum array size</li>
 *   <li><b>Trimming:</b> Ability to reduce capacity to actual size</li>
 * </ul>
 *
 * <p><b>Comparison with Standard Collections:</b>
 * <ul>
 *   <li><b>Performance:</b> Significantly faster for primitive operations</li>
 *   <li><b>Memory Usage:</b> 2-4x less memory consumption than boxed collections</li>
 *   <li><b>Type Safety:</b> Compile-time prevention of type mixing</li>
 *   <li><b>Functionality:</b> Additional primitive-specific operations</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use primitive lists when working primarily with primitive data</li>
 *   <li>Consider initial capacity hints for known data sizes</li>
 *   <li>Convert to boxed collections only when necessary for API compatibility</li>
 *   <li>Use set operations instead of manual loops for better performance</li>
 *   <li>Leverage sorting for improved search performance</li>
 * </ul>
 *
 * <p><b>Extension Points:</b>
 * <ul>
 *   <li><b>Custom Suppliers:</b> Override create*Supplier() methods for custom collection types</li>
 *   <li><b>Algorithm Tuning:</b> Override needToSet() for custom algorithm selection heuristics</li>
 *   <li><b>Validation Logic:</b> Extend range checking and validation methods</li>
 *   <li><b>Serialization:</b> Implement custom serialization for specific requirements</li>
 * </ul>
 *
 * <p><b>Related Classes:</b>
 * <ul>
 *   <li><b>Concrete Implementations:</b> IntList, DoubleList, LongList, BooleanList, etc.</li>
 *   <li><b>Utility Classes:</b> Primitive arrays utilities and conversion helpers</li>
 *   <li><b>Collection Framework:</b> Integration with standard Java collections</li>
 * </ul>
 *
 * @param <B> the boxed wrapper type corresponding to the primitive type
 *            (e.g., Integer for int, Double for double, Boolean for boolean)
 * @param <A> the primitive array type for bulk operations
 *            (e.g., int[] for int, double[] for double, boolean[] for boolean)
 * @param <L> the concrete list type extending this class, enabling type-safe method chaining
 *            and fluent API patterns (e.g., IntList for int primitives)
 *
 * @see RandomAccess
 * @see java.io.Serializable
 * @see java.util.List
 * @see java.util.Collection
 */
public abstract class PrimitiveList<B, A, L extends PrimitiveList<B, A, L>> implements RandomAccess, java.io.Serializable { // Iterable<B>, // reference to notEmpty is ambiguous both methods notEmpty(java.lang.Iterable<?>)

    /**
     * Protected constructor for subclasses.
     */
    protected PrimitiveList() {
    }

    @Serial
    private static final long serialVersionUID = 1504784980113045443L;

    /**
     * Default initial capacity.
     */
    static final int DEFAULT_CAPACITY = 10;

    static final int MAX_ARRAY_SIZE = N.MAX_ARRAY_SIZE;

    /**
     * Returns the internal array backing this list without creating a copy.
     * This method provides direct access to the internal array for performance reasons.
     * 
     * <p><b>Warning:</b> The returned array should not be modified unless you understand
     * the implications. Modifications to the returned array will directly affect this list.
     * The array may be larger than the list size; only elements from index 0 to size()-1
     * are valid list elements.</p>
     *
     * @return the internal array backing this list
     * @deprecated should call {@code toArray()}
     */
    @Deprecated
    @Beta
    public abstract A array();

    /**
     * Appends all elements from the specified PrimitiveList to the end of this list,
     * in the order they appear in the specified list.
     * 
     * <p>This operation may cause the list to reallocate its internal array if the
     * current capacity is insufficient to accommodate all new elements.</p>
     *
     * @param c the PrimitiveList containing elements to be added to this list. 
     *          If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list changed as a result of the call (i.e., if c was not empty),
     *         {@code false} otherwise
     */
    public abstract boolean addAll(L c);

    /**
     * Inserts all elements from the specified PrimitiveList into this list at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to
     * the right (increases their indices). The new elements will appear in this list in the
     * order they appear in the specified list.
     * 
     * <p>This operation may cause the list to reallocate its internal array if the
     * current capacity is insufficient to accommodate all new elements.</p>
     *
     * @param index the index at which to insert the first element from the specified list.
     *              Must be between 0 and size() (inclusive).
     * @param c the PrimitiveList containing elements to be inserted into this list.
     *          If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list changed as a result of the call (i.e., if c was not empty),
     *         {@code false} otherwise
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    public abstract boolean addAll(int index, L c);

    /**
     * Appends all elements from the specified array to the end of this list,
     * in the order they appear in the array.
     * 
     * <p>This operation may cause the list to reallocate its internal array if the
     * current capacity is insufficient to accommodate all new elements.</p>
     *
     * @param a the array containing elements to be added to this list.
     *          If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list changed as a result of the call (i.e., if the array was not empty),
     *         {@code false} otherwise
     */
    public abstract boolean addAll(A a);

    /**
     * Inserts all elements from the specified array into this list at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to
     * the right (increases their indices). The new elements will appear in this list in the
     * order they appear in the array.
     * 
     * <p>This operation may cause the list to reallocate its internal array if the
     * current capacity is insufficient to accommodate all new elements.</p>
     *
     * @param index the index at which to insert the first element from the specified array.
     *              Must be between 0 and size() (inclusive).
     * @param a the array containing elements to be inserted into this list.
     *          If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list changed as a result of the call (i.e., if the array was not empty),
     *         {@code false} otherwise
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    public abstract boolean addAll(int index, A a);

    /**
     * Removes from this list all of its elements that are contained in the specified PrimitiveList.
     * After this call returns, this list will contain no elements in common with the specified list.
     * 
     * <p>For elements that appear multiple times in this list, all occurrences will be removed
     * if the element appears at least once in the specified list. The comparison is done by value.</p>
     *
     * @param c the PrimitiveList containing elements to be removed from this list.
     *          If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list was modified as a result of the call,
     *         {@code false} otherwise
     */
    public abstract boolean removeAll(L c);

    /**
     * Removes from this list all of its elements that are contained in the specified array.
     * After this call returns, this list will contain no elements in common with the specified array.
     * 
     * <p>For elements that appear multiple times in this list, all occurrences will be removed
     * if the element appears at least once in the specified array. The comparison is done by value.</p>
     *
     * @param a the array containing elements to be removed from this list.
     *          If {@code null} or empty, this list remains unchanged.
     * @return {@code true} if this list was modified as a result of the call,
     *         {@code false} otherwise
     */
    public abstract boolean removeAll(A a);

    /**
     * Removes duplicate elements from this list, keeping only the first occurrence of each value.
     * The relative order of retained elements is preserved.
     * 
     * <p>This method uses an optimized algorithm when the list is already sorted.
     * For sorted lists, it runs in O(n) time. For unsorted lists, it may use
     * additional memory to track seen elements.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 2, 3, 1, 4, 3);
     * list.removeDuplicates(); // list becomes [1, 2, 3, 4]
     * }</pre>
     *
     * @return {@code true} if any duplicates were removed from this list,
     *         {@code false} if the list already contained only unique elements
     */
    public abstract boolean removeDuplicates();

    /**
     * Retains only the elements in this list that are contained in the specified PrimitiveList.
     * In other words, removes from this list all of its elements that are not contained
     * in the specified list. After this call returns, this list will contain only elements
     * that also appear in the specified list.
     * 
     * <p>Elements are compared by value. The relative order of retained elements is preserved.</p>
     *
     * @param c the PrimitiveList containing elements to be retained in this list.
     *          If {@code null} or empty, this list will be cleared.
     * @return {@code true} if this list was modified as a result of the call,
     *         {@code false} otherwise
     */
    public abstract boolean retainAll(L c);

    /**
     * Retains only the elements in this list that are contained in the specified array.
     * In other words, removes from this list all of its elements that are not contained
     * in the specified array. After this call returns, this list will contain only elements
     * that also appear in the specified array.
     * 
     * <p>Elements are compared by value. The relative order of retained elements is preserved.</p>
     *
     * @param a the array containing elements to be retained in this list.
     *          If {@code null} or empty, this list will be cleared.
     * @return {@code true} if this list was modified as a result of the call,
     *         {@code false} otherwise
     */
    public abstract boolean retainAll(A a);

    /**
     * Removes the elements at the specified positions from this list.
     * The indices array is processed to handle duplicate indices and maintain
     * correct element positions during removal.
     * 
     * <p>This method efficiently removes multiple elements in a single operation,
     * which is more efficient than calling remove() multiple times. The indices
     * can be specified in any order and may contain duplicates (which are ignored).</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(10, 20, 30, 40, 50);
     * list.deleteAllByIndices(1, 3); // removes elements at indices 1 and 3
     * // list now contains [10, 30, 50]
     * }</pre>
     *
     * @param indices the indices of elements to be removed. Null or empty array results in no change.
     *                Invalid indices (negative or &gt;= size()) are ignored.
     */
    public abstract void deleteAllByIndices(int... indices);

    /**
     * Removes from this list all elements whose index is between fromIndex (inclusive)
     * and toIndex (exclusive). Shifts any succeeding elements to the left (reduces their index).
     * 
     * <p>This method is useful for removing a contiguous sequence of elements from the list.
     * If fromIndex equals toIndex, no elements are removed.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(10, 20, 30, 40, 50);
     * list.deleteRange(1, 4); // removes elements at indices 1, 2, and 3
     * // list now contains [10, 50]
     * }</pre>
     *
     * @param fromIndex the index of the first element to be removed (inclusive).
     *                  Must be non-negative.
     * @param toIndex the index after the last element to be removed (exclusive).
     *                Must be &gt;= fromIndex and &lt;= size().
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         ({@code fromIndex < 0 || toIndex > size() || fromIndex > toIndex})
     */
    public abstract void deleteRange(int fromIndex, int toIndex);

    /**
     * Moves a range of elements within this list to a new position.
     * The elements from fromIndex (inclusive) to toIndex (exclusive) are moved
     * so that the element originally at fromIndex will be at newPositionAfterMove.
     * Other elements are shifted as necessary to accommodate the move.
     * 
     * <p><b>Usage Examples:</b></p> 
     * <pre>{@code
     * IntList list = IntList.of(0, 1, 2, 3, 4, 5);
     * list.moveRange(1, 3, 3);  // Moves elements [1, 2] to position starting at index 3
     * // Result: [0, 3, 4, 1, 2, 5]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to be moved
     * @param toIndex the ending index (exclusive) of the range to be moved
     * @param newPositionAfterMove — the zero-based index where the first element of the range will be placed after the move; 
     *      must be between 0 and size() - lengthOfRange, inclusive.
     * @throws IndexOutOfBoundsException if any index is out of bounds or if
     *         newPositionAfterMove would cause elements to be moved outside the list
     */
    public abstract void moveRange(int fromIndex, int toIndex, int newPositionAfterMove);

    /**
     * Replaces each element in the specified range of this list with elements from
     * the replacement PrimitiveList. The range from fromIndex (inclusive) to toIndex (exclusive)
     * is removed and replaced with all elements from the replacement list.
     * 
     * <p>If the replacement list has a different size than the range being replaced,
     * the list will grow or shrink accordingly. Elements after the replaced range
     * are shifted as necessary.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4, 5);
     * IntList replacement = IntList.of(10, 20, 30);
     * list.replaceRange(1, 3, replacement); // replaces elements at indices 1 and 2
     * // list now contains [1, 10, 20, 30, 4, 5]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to replace.
     *                  Must be non-negative.
     * @param toIndex the ending index (exclusive) of the range to replace.
     *                Must be &gt;= fromIndex and &lt;= size().
     * @param replacement the PrimitiveList whose elements will replace the specified range.
     *                    If {@code null} or empty, the range is simply deleted.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         ({@code fromIndex < 0 || toIndex > size() || fromIndex > toIndex})
     */
    public abstract void replaceRange(int fromIndex, int toIndex, L replacement);

    /**
     * Replaces each element in the specified range of this list with elements from
     * the replacement array. The range from fromIndex (inclusive) to toIndex (exclusive)
     * is removed and replaced with all elements from the replacement array.
     * 
     * <p>If the replacement array has a different length than the range being replaced,
     * the list will grow or shrink accordingly. Elements after the replaced range
     * are shifted as necessary.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4, 5);
     * int[] replacement = {10, 20, 30};
     * list.replaceRange(1, 3, replacement); // replaces elements at indices 1 and 2
     * // list now contains [1, 10, 20, 30, 4, 5]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to replace.
     *                  Must be non-negative.
     * @param toIndex the ending index (exclusive) of the range to replace.
     *                Must be &gt;= fromIndex and &lt;= size().
     * @param replacement the array whose elements will replace the specified range.
     *                    If {@code null} or empty, the range is simply deleted.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         ({@code fromIndex < 0 || toIndex > size() || fromIndex > toIndex})
     */
    public abstract void replaceRange(int fromIndex, int toIndex, A replacement);

    /**
     * Returns {@code true} if this list contains any element that is also contained in the
     * specified PrimitiveList. This method returns {@code true} if the two lists share at least
     * one common element.
     * 
     * <p>This method uses an optimized algorithm when either list is large, potentially
     * converting to a Set for O(1) lookup performance.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list1 = IntList.of(1, 2, 3, 4);
     * IntList list2 = IntList.of(3, 5, 7);
     * boolean result = list1.containsAny(list2); // returns {@code true} (common element: 3)
     * }</pre>
     *
     * @param l the PrimitiveList to be checked for common elements with this list.
     *          If {@code null} or empty, returns {@code false}.
     * @return {@code true} if this list contains any element from the specified list,
     *         {@code false} otherwise
     */
    public abstract boolean containsAny(L l);

    /**
     * Returns {@code true} if this list contains any element that is also contained in the
     * specified array. This method returns {@code true} if this list and the array share
     * at least one common element.
     * 
     * <p>This method uses an optimized algorithm when either the list or array is large,
     * potentially converting to a Set for O(1) lookup performance.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4);
     * int[] array = {3, 5, 7};
     * boolean result = list.containsAny(array); // returns {@code true} (common element: 3)
     * }</pre>
     *
     * @param a the array to be checked for common elements with this list.
     *          If {@code null} or empty, returns {@code false}.
     * @return {@code true} if this list contains any element from the specified array,
     *         {@code false} otherwise
     */
    public abstract boolean containsAny(A a);

    /**
     * Returns {@code true} if this list contains all elements in the specified PrimitiveList.
     * This method returns {@code true} if the specified list is a subset of this list
     * (ignoring element order but considering duplicates).
     * 
     * <p>For elements that appear multiple times, this list must contain at least as many
     * occurrences as the specified list. An empty list is considered a subset of any list.</p>
     * 
     * <p>This method uses an optimized algorithm when the lists are large, potentially
     * converting to a Set for O(1) lookup performance.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list1 = IntList.of(1, 2, 2, 3, 4);
     * IntList list2 = IntList.of(2, 2, 3);
     * boolean result = list1.containsAll(list2); // returns true
     * }</pre>
     *
     * @param l the PrimitiveList to be checked for containment in this list.
     *          If {@code null} or empty, returns {@code true}.
     * @return {@code true} if this list contains all elements in the specified list,
     *         {@code false} otherwise
     */
    public abstract boolean containsAll(L l);

    /**
     * Returns {@code true} if this list contains all elements in the specified array.
     * This method returns {@code true} if all elements in the array are present in this list
     * (ignoring element order but considering duplicates).
     * 
     * <p>For elements that appear multiple times, this list must contain at least as many
     * occurrences as in the array. An empty array is considered a subset of any list.</p>
     * 
     * <p>This method uses an optimized algorithm when the list or array is large, potentially
     * converting to a Set for O(1) lookup performance.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 2, 3, 4);
     * int[] array = {2, 2, 3};
     * boolean result = list.containsAll(array); // returns true
     * }</pre>
     *
     * @param a the array to be checked for containment in this list.
     *          If {@code null} or empty, returns {@code true}.
     * @return {@code true} if this list contains all elements in the specified array,
     *         {@code false} otherwise
     */
    public abstract boolean containsAll(A a);

    /**
     * Returns {@code true} if this list has no elements in common with the specified PrimitiveList.
     * Two lists are disjoint if they share no common elements.
     * 
     * <p>This method uses an optimized algorithm when either list is large, potentially
     * converting to a Set for O(1) lookup performance.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list1 = IntList.of(1, 2, 3);
     * IntList list2 = IntList.of(4, 5, 6);
     * boolean result = list1.disjoint(list2); // returns true
     * 
     * IntList list3 = IntList.of(1, 2, 3);
     * IntList list4 = IntList.of(3, 4, 5);
     * boolean result2 = list3.disjoint(list4); // returns {@code false} (common element: 3)
     * }</pre>
     *
     * @param l the PrimitiveList to check for disjointness with this list.
     *          If {@code null} or empty, returns {@code true}.
     * @return {@code true} if the two lists have no elements in common,
     *         {@code false} otherwise
     */
    public abstract boolean disjoint(L l);

    /**
     * Returns {@code true} if this list has no elements in common with the specified array.
     * This list and the array are disjoint if they share no common elements.
     * 
     * <p>This method uses an optimized algorithm when either the list or array is large,
     * potentially converting to a Set for O(1) lookup performance.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3);
     * int[] array1 = {4, 5, 6};
     * boolean result1 = list.disjoint(array1); // returns true
     * 
     * int[] array2 = {3, 4, 5};
     * boolean result2 = list.disjoint(array2); // returns {@code false} (common element: 3)
     * }</pre>
     *
     * @param a the array to check for disjointness with this list.
     *          If {@code null} or empty, returns {@code true}.
     * @return {@code true} if this list and the array have no elements in common,
     *         {@code false} otherwise
     */
    public abstract boolean disjoint(A a);

    /**
     * Returns a new list containing elements that are present in both this list and the specified list.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences
     * present in both lists.
     *
     * <p>The returned list preserves the order of elements as they appear in this list.
     * This operation does not modify either list.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * IntList list2 = IntList.of(1, 2, 2, 4);
     * IntList result = list1.intersection(list2); // result will be [1, 2, 2]
     * // One occurrence of '1' (minimum count in both lists) and two occurrences of '2'
     *
     * IntList list3 = IntList.of(5, 5, 6);
     * IntList list4 = IntList.of(5, 7);
     * IntList result2 = list3.intersection(list4); // result will be [5]
     * // One occurrence of '5' (minimum count in both lists)
     * }</pre>
     *
     * @param b the list to find common elements with this list.
     *          If {@code null} or empty, returns an empty list.
     * @return a new PrimitiveList containing elements present in both this list and the specified list,
     *         considering the minimum number of occurrences in either list.
     * @see IntList#intersection(IntList)
     * @see N#intersection(int[], int[])
     */
    public abstract L intersection(final L b);

    /**
     * Returns a new list containing elements that are present in both this list and the specified array.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences
     * present in both sources.
     *
     * <p>The returned list preserves the order of elements as they appear in this list.
     * This operation does not modify the list or array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * int[] array = new int[] {1, 2, 2, 4};
     * IntList result = list1.intersection(array); // result will be [1, 2, 2]
     * // One occurrence of '1' (minimum count in both sources) and two occurrences of '2'
     *
     * IntList list2 = IntList.of(5, 5, 6);
     * int[] array2 = new int[] {5, 7};
     * IntList result2 = list2.intersection(array2); // result will be [5]
     * // One occurrence of '5' (minimum count in both sources)
     * }</pre>
     *
     * @param b the array to find common elements with this list.
     *          If {@code null} or empty, returns an empty list.
     * @return a new PrimitiveList containing elements present in both this list and the specified array,
     *         considering the minimum number of occurrences in either source.
     * @see IntList#intersection(int[])
     * @see N#intersection(int[], int[])
     */
    public abstract L intersection(final A b);

    /**
     * Returns a new list with the elements in this list but not in the specified list,
     * considering the number of occurrences of each element.
     *
     * <p>If an element appears multiple times in both lists, the difference will contain
     * the extra occurrences from this list. The returned list preserves the order of
     * elements as they appear in this list.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * IntList list2 = IntList.of(2, 5, 1);
     * IntList result = list1.difference(list2); // result will be [0, 2, 3]
     * // One '2' remains because list1 has two occurrences and list2 has one
     *
     * IntList list3 = IntList.of(5, 6);
     * IntList list4 = IntList.of(5, 5, 6);
     * IntList result2 = list3.difference(list4); // result will be [] (empty)
     * // No elements remain because list4 has at least as many occurrences of each value as list3
     * }</pre>
     *
     * @param b the list to compare against this list.
     *          If {@code null} or empty, returns a copy of this list.
     * @return a new PrimitiveList containing the elements that are present in this list but not in the specified list,
     *         considering the number of occurrences.
     * @see IntList#difference(IntList)
     * @see N#difference(int[], int[])
     */
    public abstract L difference(final L b);

    /**
     * Returns a new list with the elements in this list but not in the specified array,
     * considering the number of occurrences of each element.
     *
     * <p>If an element appears multiple times in both sources, the difference will contain
     * the extra occurrences from this list. The returned list preserves the order of
     * elements as they appear in this list.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * int[] array = new int[] {2, 5, 1};
     * IntList result = list1.difference(array); // result will be [0, 2, 3]
     * // One '2' remains because list1 has two occurrences and array has one
     *
     * IntList list2 = IntList.of(5, 6);
     * int[] array2 = new int[] {5, 5, 6};
     * IntList result2 = list2.difference(array2); // result will be [] (empty)
     * // No elements remain because array2 has at least as many occurrences of each value as list2
     * }</pre>
     *
     * @param a the array to compare against this list.
     *          If {@code null} or empty, returns a copy of this list.
     * @return a new PrimitiveList containing the elements that are present in this list but not in the specified array,
     *         considering the number of occurrences.
     * @see IntList#difference(int[])
     * @see N#difference(int[], int[])
     */
    public abstract L difference(final A a);

    /**
     * Returns a new list containing elements that are present in either this list or the specified list,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified list that aren't in this list.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list1 = IntList.of(1, 1, 2, 3);
     * IntList list2 = IntList.of(1, 2, 2, 4);
     * IntList result = list1.symmetricDifference(list2);
     * // result will contain: [1, 3, 2, 4]
     * // Elements explanation:
     * // - 1 appears twice in list1 and once in list2, so one occurrence remains
     * // - 3 appears only in list1, so it remains
     * // - 2 appears once in list1 and twice in list2, so one occurrence remains
     * // - 4 appears only in list2, so it remains
     * }</pre>
     *
     * @param b the list to compare with this list for symmetric difference.
     *          If {@code null} or empty, returns a copy of this list.
     * @return a new list containing elements that are present in either this list or the specified list,
     *         but not in both, considering the number of occurrences
     * @see IntList#symmetricDifference(IntList)
     * @see N#symmetricDifference(int[], int[])
     */
    public abstract L symmetricDifference(final L b);

    /**
     * Returns a new list containing elements that are present in either this list or the specified array,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list1 = IntList.of(1, 1, 2, 3);
     * int[] array = new int[] {1, 2, 2, 4};
     * IntList result = list1.symmetricDifference(array);
     * // result will contain: [1, 3, 2, 4]
     * // Elements explanation:
     * // - 1 appears twice in list1 and once in array, so one occurrence remains
     * // - 3 appears only in list1, so it remains
     * // - 2 appears once in list1 and twice in array, so one occurrence remains
     * // - 4 appears only in array, so it remains
     * }</pre>
     *
     * @param b the array to compare with this list for symmetric difference.
     *          If {@code null} or empty, returns a copy of this list.
     * @return a new list containing elements that are present in either this list or the specified array,
     *         but not in both, considering the number of occurrences
     * @see IntList#symmetricDifference(int[])
     * @see N#symmetricDifference(int[], int[])
     */
    public abstract L symmetricDifference(final A b);

    /**
     * Checks whether this list contains any duplicate elements.
     * An element is considered a duplicate if it appears more than once in the list.
     * 
     * <p>This method uses an efficient algorithm that may short-circuit as soon as
     * a duplicate is found.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list1 = IntList.of(1, 2, 3, 4);
     * boolean result1 = list1.hasDuplicates(); // returns false
     * 
     * IntList list2 = IntList.of(1, 2, 3, 2);
     * boolean result2 = list2.hasDuplicates(); // returns true
     * }</pre>
     *
     * @return {@code true} if the list contains at least one duplicate element,
     *         {@code false} otherwise
     */
    public abstract boolean hasDuplicates();

    /**
     * Returns a new list containing only the distinct elements from this list.
     * The order of elements is preserved, with the first occurrence of each
     * distinct value being retained.
     * 
     * <p>This method is equivalent to calling {@code distinct(0, size())}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 2, 3, 1, 4, 3);
     * IntList result = list.distinct(); // returns [1, 2, 3, 4]
     * }</pre>
     *
     * @return a new PrimitiveList with distinct elements
     */
    public L distinct() {
        return distinct(0, size());
    }

    /**
     * Returns a new list containing only the distinct elements from the specified range
     * of this list. The order of elements is preserved, with the first occurrence of each
     * distinct value being retained.
     * 
     * <p>This method uses an efficient algorithm to identify distinct elements without
     * modifying the original list.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 2, 3, 1, 4, 3);
     * IntList result = list.distinct(1, 6); // returns [2, 3, 1, 4]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to process.
     *                  Must be non-negative.
     * @param toIndex the ending index (exclusive) of the range to process.
     *                Must be &gt;= fromIndex and &lt;= size().
     * @return a new PrimitiveList with distinct elements from the specified range
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public abstract L distinct(final int fromIndex, final int toIndex);

    /**
     * Checks whether the elements in this list are sorted in ascending order.
     * An empty list or a list with a single element is considered sorted.
     * 
     * <p>For numeric types, ascending order means each element is less than or equal to
     * the next element. Equal consecutive values are allowed in a sorted list.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list1 = IntList.of(1, 2, 2, 3, 5);
     * boolean result1 = list1.isSorted(); // returns true
     * 
     * IntList list2 = IntList.of(1, 3, 2, 4);
     * boolean result2 = list2.isSorted(); // returns false
     * }</pre>
     *
     * @return {@code true} if all elements are in ascending order (allowing equal consecutive values),
     *         {@code false} otherwise
     */
    public abstract boolean isSorted();

    /**
     * Sorts all elements in this list in ascending order.
     * This method modifies the list in place using an efficient sorting algorithm.
     * 
     * <p>The sorting algorithm used is typically optimized for the primitive type,
     * offering O(n log n) performance on average. For already sorted or nearly sorted
     * lists, the algorithm may perform better.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(3, 1, 4, 1, 5, 9);
     * list.sort();
     * // list now contains [1, 1, 3, 4, 5, 9]
     * }</pre>
     */
    public abstract void sort();

    /**
     * Sorts all elements in this list in descending order.
     * This method first sorts the list in ascending order, then reverses it.
     * The list is modified in place.
     * 
     * <p>This is equivalent to calling {@code sort()} followed by {@code reverse()},
     * but may be optimized in specific implementations.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(3, 1, 4, 1, 5, 9);
     * list.reverseSort();
     * // list now contains [9, 5, 4, 3, 1, 1]
     * }</pre>
     */
    public abstract void reverseSort();

    /**
     * Reverses the order of all elements in this list.
     * After this method returns, the first element becomes the last,
     * the second element becomes the second to last, and so on.
     * This method modifies the list in place.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4, 5);
     * list.reverse();
     * // list now contains [5, 4, 3, 2, 1]
     * }</pre>
     */
    public abstract void reverse();

    /**
     * Reverses the order of elements in the specified range of this list.
     * After this method returns, the element at fromIndex becomes the element
     * at (toIndex - 1), and vice versa. Elements outside the specified range
     * are not affected. This method modifies the list in place.
     * 
     * <p>If fromIndex equals toIndex, the list is unchanged.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4, 5);
     * list.reverse(1, 4);
     * // list now contains [1, 4, 3, 2, 5]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to reverse.
     *                  Must be non-negative.
     * @param toIndex the ending index (exclusive) of the range to reverse.
     *                Must be &gt;= fromIndex and &lt;= size().
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public abstract void reverse(final int fromIndex, final int toIndex);

    /**
     * Rotates all elements in this list by the specified distance.
     * After calling rotate(distance), the element at index i will be moved to
     * index (i + distance) % size.
     * 
     * <p>Positive values of distance rotate elements towards higher indices (right rotation),
     * while negative values rotate towards lower indices (left rotation).
     * The list is modified in place.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4, 5);
     * list.rotate(2);
     * // list now contains [4, 5, 1, 2, 3]
     * 
     * IntList list2 = IntList.of(1, 2, 3, 4, 5);
     * list2.rotate(-2);
     * // list2 now contains [3, 4, 5, 1, 2]
     * }</pre>
     *
     * @param distance the distance to rotate the list. Positive values rotate right,
     *                 negative values rotate left. The distance can be larger than
     *                 the list size; it will be reduced modulo size.
     */
    public abstract void rotate(int distance);

    /**
     * Randomly shuffles all elements in this list.
     * After this method returns, the elements will be in a random order.
     * This method uses a default source of randomness and modifies the list in place.
     * 
     * <p>This implementation uses a high-quality shuffling algorithm (typically Fisher-Yates)
     * which guarantees that all permutations are equally likely, assuming a good
     * source of randomness.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4, 5);
     * list.shuffle();
     * // list now contains elements in random order, e.g., [3, 1, 5, 2, 4]
     * }</pre>
     */
    public abstract void shuffle();

    /**
     * Randomly shuffles all elements in this list using the specified source of randomness.
     * After this method returns, the elements will be in a random order determined by
     * the given Random object. This method modifies the list in place.
     * 
     * <p>This implementation uses a high-quality shuffling algorithm (typically Fisher-Yates)
     * which guarantees that all permutations are equally likely, assuming the Random
     * object produces uniformly distributed values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4, 5);
     * Random rnd = new Random(12345); // seed for reproducibility
     * list.shuffle(rnd);
     * // list now contains elements in a random order determined by the seed
     * }</pre>
     *
     * @param rnd the source of randomness to use for shuffling.
     *            Must not be {@code null}.
     */
    public abstract void shuffle(final Random rnd);

    /**
     * Swaps the elements at the specified positions in this list.
     * After this method returns, the element previously at position i will be
     * at position j, and vice versa. This method modifies the list in place.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4, 5);
     * list.swap(1, 3);
     * // list now contains [1, 4, 3, 2, 5]
     * }</pre>
     *
     * @param i the index of the first element to swap.
     *          Must be &gt;= 0 and &lt; size().
     * @param j the index of the second element to swap.
     *          Must be &gt;= 0 and &lt; size().
     * @throws IndexOutOfBoundsException if either i or j is out of range
     *         ({@code i < 0 || i >= size() || j < 0 || j >= size()})
     */
    public abstract void swap(int i, int j);

    /**
     * Returns a new PrimitiveList containing a copy of all elements in this list.
     * The returned list is independent of this list, so changes to the
     * returned list will not affect this list and vice versa.
     * 
     * <p>This method is equivalent to calling {@code copy(0, size())}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList original = IntList.of(1, 2, 3);
     * IntList copy = original.copy();
     * copy.add(4);
     * // original still contains [1, 2, 3]
     * // copy contains [1, 2, 3, 4]
     * }</pre>
     *
     * @return a new PrimitiveList containing all elements from this list
     */
    public abstract L copy();

    /**
     * Returns a new PrimitiveList containing a copy of elements in the specified range of this list.
     * The returned list is independent of this list, so changes to the
     * returned list will not affect this list and vice versa.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList original = IntList.of(1, 2, 3, 4, 5);
     * IntList copy = original.copy(1, 4);
     * // copy contains [2, 3, 4]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to copy.
     *                  Must be non-negative.
     * @param toIndex the ending index (exclusive) of the range to copy.
     *                Must be &gt;= fromIndex and &lt;= size().
     * @return a new PrimitiveList containing the elements in the specified range
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public abstract L copy(final int fromIndex, final int toIndex);

    /**
     * Returns a new PrimitiveList containing a copy of elements from the specified range of this list,
     * selecting only elements at intervals defined by the step parameter.
     * 
     * <p>For positive step values, elements are selected in forward direction starting from fromIndex.
     * For negative step values when fromIndex &gt; toIndex, elements are selected in reverse direction.
     * The absolute value of step determines the interval between selected elements.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
     * 
     * // Forward stepping
     * IntList copy1 = list.copy(0, 10, 2); // returns [0, 2, 4, 6, 8]
     * IntList copy2 = list.copy(1, 8, 3);  // returns [1, 4, 7]
     * 
     * // Reverse stepping
     * IntList copy3 = list.copy(8, 2, -2); // returns [8, 6, 4]
     * IntList copy4 = list.copy(9, -1, -3); // returns [9, 6, 3, 0]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to copy.
     *                  For forward stepping, must be &lt; toIndex.
     *                  For reverse stepping, must be &gt; toIndex (or toIndex can be -1 for start).
     * @param toIndex the ending index (exclusive) of the range to copy.
     *                Can be -1 when using negative step to indicate copying to the start.
     * @param step the interval between selected elements. Must not be zero.
     *             Positive values select elements in forward direction,
     *             negative values select elements in reverse direction.
     * @return a new PrimitiveList containing the selected elements
     * @throws IndexOutOfBoundsException if the range is invalid
     * @throws IllegalArgumentException if step is zero
     */
    public abstract L copy(final int fromIndex, final int toIndex, final int step);

    /**
     * Splits this list into consecutive chunks of the specified size and returns them as a List of PrimitiveLists.
     * Each chunk (except possibly the last) will have exactly chunkSize elements.
     * The last chunk may have fewer elements if the list size is not evenly divisible by chunkSize.
     * 
     * <p>This method is equivalent to calling {@code split(0, size(), chunkSize)}.</p>
     * 
     * <p>The returned chunks are independent copies, so modifications to them will not
     * affect this list or each other.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4, 5, 6, 7);
     * List<IntList> chunks = list.split(3);
     * // chunks contains: [[1, 2, 3], [4, 5, 6], [7]]
     * }</pre>
     *
     * @param chunkSize the desired size of each chunk. Must be greater than 0.
     * @return a List containing the PrimitiveList chunks
     * @throws IllegalArgumentException if chunkSize &lt;= 0
     */
    public List<L> split(final int chunkSize) {
        return split(0, size(), chunkSize);
    }

    /**
     * Splits the specified range of this list into consecutive chunks of the specified size
     * and returns them as a List of PrimitiveLists. Each chunk (except possibly the last)
     * will have exactly chunkSize elements. The last chunk may have fewer elements if the
     * range size is not evenly divisible by chunkSize.
     * 
     * <p>The returned chunks are independent copies, so modifications to them will not
     * affect this list or each other.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
     * List<IntList> chunks = list.split(2, 8, 3);
     * // chunks contains: [[3, 4, 5], [6, 7, 8]]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to split.
     *                  Must be non-negative.
     * @param toIndex the ending index (exclusive) of the range to split.
     *                Must be &gt;= fromIndex and &lt;= size().
     * @param chunkSize the desired size of each chunk. Must be greater than 0.
     * @return a List containing the PrimitiveList chunks
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     * @throws IllegalArgumentException if chunkSize &lt;= 0
     */
    public abstract List<L> split(final int fromIndex, final int toIndex, int chunkSize);

    /**
     * Trims the capacity of this PrimitiveList instance to be the list's current size.
     * This method can be used to minimize the storage of a PrimitiveList instance.
     * If the capacity is already equal to the size, this method does nothing.
     * 
     * <p>After this call, the capacity of the list will be equal to its size,
     * eliminating any unused capacity. This operation does not change the list's
     * contents or size, only its internal capacity.</p>
     * 
     * <p>This method returns the list itself to allow method chaining.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = new IntList(100); // capacity = 100
     * list.add(1);
     * list.add(2);
     * list.add(3); // size = 3, capacity = 100
     * list.trimToSize(); // size = 3, capacity = 3
     * }</pre>
     *
     * @return this PrimitiveList instance (for method chaining)
     */
    @Beta
    public abstract L trimToSize();

    /**
     * Removes all elements from this list.
     * The list will be empty after this call returns.
     * 
     * <p>The capacity of the list may not change, meaning the internal array
     * may still be allocated. To also minimize memory usage, call {@code trimToSize()}
     * after clearing.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3, 4, 5);
     * list.clear();
     * // list.size() returns 0
     * // list.isEmpty() returns true
     * }</pre>
     */
    public abstract void clear();

    /**
     * Returns {@code true} if this list contains no elements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = new IntList();
     * boolean empty1 = list.isEmpty(); // returns true
     * 
     * list.add(42);
     * boolean empty2 = list.isEmpty(); // returns false
     * 
     * list.clear();
     * boolean empty3 = list.isEmpty(); // returns true
     * }</pre>
     *
     * @return {@code true} if this list contains no elements, {@code false} otherwise
     */
    public abstract boolean isEmpty();

    /**
     * Returns the number of elements in this list.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = new IntList();
     * int size1 = list.size(); // returns 0
     * 
     * list.add(10);
     * list.add(20);
     * int size2 = list.size(); // returns 2
     * }</pre>
     *
     * @return the number of elements in this list
     */
    public abstract int size();

    /**
     * Returns a new array containing all elements in this list.
     * The returned array is independent of this list, so changes to the
     * returned array will not affect this list and vice versa.
     * 
     * <p>The returned array will have a length equal to the size of this list,
     * not the capacity. The elements are copied in the order they appear in the list.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3);
     * int[] array = list.toArray();
     * // array contains [1, 2, 3]
     * array[0] = 99;
     * // list still contains [1, 2, 3]
     * }</pre>
     *
     * @return a new array containing all elements from this list
     */
    public abstract A toArray();

    /**
     * Returns a List containing all elements in this list converted to their boxed type.
     * The returned list is a new ArrayList and is independent of this list.
     * 
     * <p>This method is equivalent to calling {@code boxed(0, size())}.</p>
     * 
     * <p>This method is useful when you need to work with APIs that require
     * List&lt;Integer&gt; rather than primitive arrays.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList primitiveList = IntList.of(1, 2, 3);
     * List<Integer> boxedList = primitiveList.boxed();
     * // boxedList contains [Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)]
     * }</pre>
     *
     * @return a new List containing all elements from this list as boxed objects
     */
    public List<B> boxed() {
        return boxed(0, size());
    }

    /**
     * Returns a List containing elements from the specified range of this list
     * converted to their boxed type. The returned list is a new ArrayList and
     * is independent of this list.
     * 
     * <p>This method is useful when you need to work with APIs that require
     * boxed types rather than primitives for a specific range of elements.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList primitiveList = IntList.of(1, 2, 3, 4, 5);
     * List<Integer> boxedList = primitiveList.boxed(1, 4);
     * // boxedList contains [Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(4)]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to box.
     *                  Must be non-negative.
     * @param toIndex the ending index (exclusive) of the range to box.
     *                Must be &gt;= fromIndex and &lt;= size().
     * @return a new List containing elements from the specified range as boxed objects
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public abstract List<B> boxed(final int fromIndex, final int toIndex);

    /**
     * Returns a List containing all elements in this list converted to their boxed type.
     * The returned list is a new ArrayList and is independent of this list.
     * 
     * <p>This method is equivalent to {@link #boxed()}.</p>
     *
     * @return a new List containing all elements from this list as boxed objects
     * @deprecated use {@link #boxed()} instead.
     */
    @Deprecated
    public List<B> toList() {
        return boxed();
    }

    /**
     * Returns a List containing elements from the specified range of this list
     * converted to their boxed type. The returned list is a new ArrayList and
     * is independent of this list.
     * 
     * <p>This method is equivalent to {@link #boxed(int, int)}.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to convert
     * @param toIndex the ending index (exclusive) of the range to convert
     * @return a new List containing elements from the specified range as boxed objects
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     * @deprecated use {@link #boxed(int, int)} instead.
     */
    @Deprecated
    public List<B> toList(final int fromIndex, final int toIndex) {
        return boxed(fromIndex, toIndex);
    }

    /**
     * Returns a Set containing all elements in this list converted to their boxed type.
     * Duplicate elements in the list will appear only once in the returned set.
     * The returned set is independent of this list.
     * 
     * <p>This method is equivalent to calling {@code toSet(0, size())}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 2, 3, 1);
     * Set<Integer> set = list.toSet();
     * // set contains [1, 2, 3] (order may vary depending on Set implementation)
     * }</pre>
     *
     * @return a new Set containing unique elements from this list as boxed objects
     */
    public Set<B> toSet() {
        return toSet(0, size());
    }

    /**
     * Returns a Set containing elements from the specified range of this list
     * converted to their boxed type. Duplicate elements in the range will appear
     * only once in the returned set. The returned set is independent of this list.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 2, 3, 1, 4);
     * Set<Integer> set = list.toSet(1, 5);
     * // set contains [2, 3, 1] (order may vary depending on Set implementation)
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to convert.
     *                  Must be non-negative.
     * @param toIndex the ending index (exclusive) of the range to convert.
     *                Must be &gt;= fromIndex and &lt;= size().
     * @return a new Set containing unique elements from the specified range as boxed objects
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public Set<B> toSet(final int fromIndex, final int toIndex) {
        return toCollection(fromIndex, toIndex, IntFunctions.ofSet());
    }

    /**
     * Returns a Collection containing all elements from this list converted to their boxed type.
     * The type of Collection returned is determined by the provided supplier function.
     * The returned collection is independent of this list.
     * 
     * <p>This method is equivalent to calling {@code toCollection(0, size(), supplier)}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntList.of(1, 2, 3);
     * LinkedList<Integer> linkedList = list.toCollection(LinkedList::new);
     * TreeSet<Integer> treeSet = list.toCollection(size -> new TreeSet<>());
     * }</pre>
     *
     * @param <C> the type of Collection to return
     * @param supplier a function that creates a new Collection instance with the given initial capacity.
     *                 The supplier receives the number of elements that will be added.
     * @return a Collection containing all elements from this list as boxed objects
     */
    public <C extends Collection<B>> C toCollection(final IntFunction<? extends C> supplier) {
        return toCollection(0, size(), supplier);
    }

    /**
     * Returns a Collection containing the elements from the specified range converted to their boxed type.
     * The type of Collection returned is determined by the provided supplier function.
     * The returned collection is independent of this list.
     * 
     * @param <C> the type of Collection to create, must extend Collection&lt;B&gt;
     * @param fromIndex the starting index (inclusive) of the range to convert
     * @param toIndex the ending index (exclusive) of the range to convert
     * @param supplier a function that creates a new Collection instance of the desired type with the given initial capacity
     * @return a Collection containing elements from the specified range in the same order
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     * @throws RuntimeException if the supplier throws an exception during Collection creation
     */
    public abstract <C extends Collection<B>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier);

    /**
     * Returns a Multiset containing all elements from this list converted to their boxed type.
     * A Multiset is a collection that allows duplicate elements and provides occurrence counting.
     * 
     * @return a Multiset containing all elements from this primitive list with their occurrence counts
     */
    public Multiset<B> toMultiset() {
        return toMultiset(0, size());
    }

    /**
     * Returns a Multiset containing all elements from specified range converted to their boxed type.
     * A Multiset is a collection that allows duplicate elements and provides occurrence counting.
     * 
     * @param fromIndex the starting index (inclusive) of the range to convert
     * @param toIndex the ending index (exclusive) of the range to convert
     * @return a Multiset containing elements from the specified range with their occurrence counts
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    public Multiset<B> toMultiset(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final IntFunction<Multiset<B>> supplier = createMultisetSupplier();

        return toMultiset(fromIndex, toIndex, supplier);
    }

    /**
     * Returns a Multiset containing all elements from this list converted to their boxed type.
     * The type of Multiset returned is determined by the provided supplier function.
     * A Multiset is a collection that allows duplicate elements and provides occurrence counting.
     * 
     * @param supplier a function that creates a new Multiset instance with the given initial capacity
     * @return a Multiset containing all elements from this primitive list with their occurrence counts
     * @throws RuntimeException if the supplier throws an exception during Multiset creation
     */
    public Multiset<B> toMultiset(final IntFunction<Multiset<B>> supplier) {
        return toMultiset(0, size(), supplier);
    }

    /**
     * Returns a Multiset containing all elements from specified range converted to their boxed type.
     * The type of Multiset returned is determined by the provided supplier function.
     * A Multiset is a collection that allows duplicate elements and provides occurrence counting.
     * 
     * @param fromIndex the starting index (inclusive) of the range to convert
     * @param toIndex the ending index (exclusive) of the range to convert
     * @param supplier a function that creates a new Multiset instance with the given initial capacity
     * @return a Multiset containing elements from the specified range with their occurrence counts
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     * @throws RuntimeException if the supplier throws an exception during Multiset creation
     */
    public abstract Multiset<B> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<B>> supplier);

    /**
     * Returns an iterator over the elements in this primitive list.
     * The iterator returns elements in the order they appear in the list, from index 0 to size()-1.
     * The returned iterator does not support the remove() operation by default.
     * 
     * @return an Iterator over the boxed elements of type B in this list
     */
    public abstract Iterator<B> iterator();

    /**
     * Validates that the specified range indices are within the bounds of this list.
     * This method checks that fromIndex is non-negative, toIndex does not exceed size(),
     * and fromIndex is not greater than toIndex.
     * 
     * @param fromIndex the starting index (inclusive) to validate
     * @param toIndex the ending index (exclusive) to validate
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; size(), or fromIndex &gt; toIndex
     */
    protected void checkFromToIndex(final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, size());
    }

    /**
     * Validates that the specified index is within the bounds of this list.
     * This method checks that index is non-negative and less than size().
     * 
     * @param index the index to validate
     * @throws IndexOutOfBoundsException if index &lt; 0 or index &gt;= size()
     */
    protected void rangeCheck(final int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
    }

    /**
     * Calculates a new capacity for the internal array when it needs to grow.
     * The new capacity is typically 1.75 times the current length, but is capped at MAX_ARRAY_SIZE.
     * If the calculated capacity is less than the minimum required capacity, the minimum is used.
     * 
     * @param minCapacity the minimum capacity required
     * @param curLen the current length of the internal array
     * @return the new capacity to use, which will be at least minCapacity and at most MAX_ARRAY_SIZE
     */
    protected int calNewCapacity(final int minCapacity, final int curLen) {
        int newCapacity = (int) (curLen * 1.75);

        if (newCapacity < 0 || newCapacity > MAX_ARRAY_SIZE) {
            newCapacity = MAX_ARRAY_SIZE;
        }

        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }

        return newCapacity;
    }

    /**
     * Creates a supplier function for List instances.
     * The returned function creates new List instances with the specified initial capacity.
     * This method is typically used internally for operations that need to create Lists.
     * 
     * @param <T> the element type of the List
     * @return an IntFunction that creates List instances with the given capacity
     */
    protected <T> IntFunction<List<T>> createListSupplier() {
        return IntFunctions.ofList();
    }

    /**
     * Creates a supplier function for Set instances.
     * The returned function creates new Set instances with the specified initial capacity.
     * This method is typically used internally for operations that need to create Sets.
     * 
     * @param <T> the element type of the Set
     * @return an IntFunction that creates Set instances with the given capacity
     */
    protected <T> IntFunction<Set<T>> createSetSupplier() {
        return IntFunctions.ofSet();
    }

    /**
     * Creates a supplier function for Map instances.
     * The returned function creates new Map instances with the specified initial capacity.
     * This method is typically used internally for operations that need to create Maps.
     * 
     * @param <K> the key type of the Map
     * @param <V> the value type of the Map
     * @return an IntFunction that creates Map instances with the given capacity
     */
    protected <K, V> IntFunction<Map<K, V>> createMapSupplier() {
        return IntFunctions.ofMap();
    }

    /**
     * Creates a supplier function for Multiset instances.
     * The returned function creates new Multiset instances with the specified initial capacity.
     * This method is typically used internally for operations that need to create Multisets.
     * 
     * @param <T> the element type of the Multiset
     * @return an IntFunction that creates Multiset instances with the given capacity
     */
    protected <T> IntFunction<Multiset<T>> createMultisetSupplier() {
        return IntFunctions.ofMultiset();
    }

    /**
     * Determines whether a set-based algorithm should be used for operations involving two collections.
     * This method uses heuristics based on the sizes of the collections to decide if using a Set
     * for lookups would be more efficient than linear searching.
     * Returns {@code true} if the minimum size is greater than 3 and the maximum size is greater than 9.
     * 
     * @param lenA the size of the first collection
     * @param lenB the size of the second collection
     * @return {@code true} if a set-based algorithm would be more efficient, {@code false} otherwise
     */
    protected boolean needToSet(final int lenA, final int lenB) {
        return Math.min(lenA, lenB) > 3 && Math.max(lenA, lenB) > 9;
    }

    /**
     * Throws a NoSuchElementException if this list is empty.
     * This method is typically called by operations that require at least one element to be present,
     * such as methods that retrieve the first or last element.
     * 
     * @throws NoSuchElementException if size() == 0, with a message indicating the list type is empty
     */
    protected void throwNoSuchElementExceptionIfEmpty() {
        if (size() == 0) {
            throw new NoSuchElementException(this.getClass().getSimpleName() + " is empty");
        }
    }
}
