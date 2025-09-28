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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBiPredicate;
import com.landawn.abacus.util.function.IntObjFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * A two-dimensional table data structure that stores data in cells identified by row and column keys.
 * <p>
 * This class provides a flexible way to work with tabular data where each cell is uniquely identified
 * by a row key and column key combination. The Sheet supports various operations including:
 * </p>
 * <ul>
 *   <li>Cell-level operations: get, put, remove values</li>
 *   <li>Row/Column operations: add, remove, rename, sort rows and columns</li>
 *   <li>Data transformation: transpose, merge, update values</li>
 *   <li>Export capabilities: to arrays, datasets, and formatted output</li>
 *   <li>Stream operations for functional-style data processing</li>
 * </ul>
 * 
 * <p>The Sheet can be frozen to prevent modifications, making it immutable.</p>
 * 
 * <p><b>Note:</b> This implementation is not thread-safe. If multiple threads access a sheet concurrently, and at least one of the threads modifies the sheet, it must be synchronized externally.</p>
 * 
 * <p>Notation: {@code R} = Row, {@code C} = Column, {@code H} = Horizontal, {@code V} = Vertical</p>
 *
 * <pre>{@code
 * // Create a sheet with integer values
 * Sheet<String, String, Integer> sheet = Sheet.rows(
 *     List.of("row1", "row2"),
 *     List.of("col1", "col2", "col3"),
 *     new Integer[][] {
 *         {1, 2, 3},
 *         {4, 5, 6}
 *     }
 * );
 * 
 * // Access and modify values
 * Integer value = sheet.get("row1", "col2"); // returns 2
 * sheet.put("row2", "col3", 10);
 * }</pre>
 *
 * @param <R> the type of the row keys
 * @param <C> the type of the column keys
 * @param <V> the type of the values stored in the cells
 *
 * @see com.landawn.abacus.util.Dataset
 */
public final class Sheet<R, C, V> implements Cloneable {

    static final KryoParser kryoParser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : null;

    private final Set<R> _rowKeySet; //NOSONAR

    private final Set<C> _columnKeySet; //NOSONAR

    private BiMap<R, Integer> _rowKeyIndexMap; //NOSONAR

    private BiMap<C, Integer> _columnKeyIndexMap; //NOSONAR

    private List<List<V>> _columnList; //NOSONAR

    private boolean _isInitialized = false; //NOSONAR

    private boolean _isFrozen = false; //NOSONAR

    /**
     * Creates an empty Sheet with no row keys and no column keys.
     * <p>
     * The Sheet will be initialized with empty row and column key sets.
     * Values can be added later by first adding rows and columns.
     * </p>
     * 
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>();
     * sheet.addRow("row1", List.of()); // Add empty row
     * sheet.addColumn("col1", List.of(1)); // Add column with value
     * }</pre>
     */
    public Sheet() {
        this(N.emptyList(), N.emptyList());
    }

    /**
     * Creates a new Sheet with the specified row keys and column keys.
     * <p>
     * The Sheet is initialized with the given row and column keys but contains no values initially.
     * All cells will return {@code null} until values are explicitly set. The order of keys is preserved
     * as provided in the collections.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3")
     * );
     * // All cells initially contain null
     * sheet.put("row1", "col1", 42); // Set a value
     * }</pre>
     *
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @throws IllegalArgumentException if any of the row keys or column keys are {@code null} or duplicated
     */
    public Sheet(final Collection<R> rowKeySet, final Collection<C> columnKeySet) throws IllegalArgumentException {
        N.checkArgument(!N.anyNull(rowKeySet), "Row key can't be null");
        N.checkArgument(!N.anyNull(columnKeySet), "Column key can't be null");
        N.checkArgument(!N.hasDuplicates(rowKeySet), "Duplicate row keys are not allowed");
        N.checkArgument(!N.hasDuplicates(columnKeySet), "Duplicate column keys are not allowed");

        _rowKeySet = N.newLinkedHashSet(rowKeySet);
        _columnKeySet = N.newLinkedHashSet(columnKeySet);
    }

    /**
     * Creates a new Sheet with the specified row keys, column keys, and initial data.
     * <p>
     * The data is provided as a two-dimensional array where each inner array represents a row.
     * The dimensions of the array must match the sizes of the row and column key sets exactly.
     * The array can contain {@code null} values which will be stored as null cells in the Sheet.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {1, 2, 3},     // row1: col1=1, col2=2, col3=3
     *         {4, null, 6}   // row2: col1=4, col2=null, col3=6
     *     }
     * );
     * Integer val = sheet.get("row2", "col2"); // returns null
     * }</pre>
     *
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @param rows the initial data as a 2D array where rows[i][j] is the value at row i, column j;
     *             must have length equal to rowKeySet size, and each inner array must have length equal to columnKeySet size
     * @throws IllegalArgumentException if any row/column keys are {@code null} or duplicated, or if array dimensions don't match the key sets
     */
    public Sheet(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final Object[][] rows) throws IllegalArgumentException {
        this(rowKeySet, columnKeySet);

        final int rowLength = this.rowLength();
        final int columnLength = this.columnLength();

        if (N.notEmpty(rows)) {
            N.checkArgument(rows.length == rowLength, "The length of array is not equal to size of row/column key set"); //NOSONAR

            for (final Object[] e : rows) {
                N.checkArgument(e.length == columnLength, "The length of array is not equal to size of row/column key set");
            }

            initIndexMap();

            _columnList = new ArrayList<>(columnLength);

            for (int i = 0; i < columnLength; i++) {
                final List<V> column = new ArrayList<>(rowLength);

                for (int j = 0; j < rowLength; j++) {
                    column.add((V) rows[j][i]);
                }

                _columnList.add(column);
            }

            _isInitialized = true;
        }
    }

    @SuppressWarnings("rawtypes")
    private static final Sheet EMPTY_SHEET = new Sheet<>(N.emptyList(), N.emptyList(), new Object[0][0]);

    static {
        EMPTY_SHEET.freeze();
    }

    /**
     * Returns an empty, immutable Sheet instance.
     * <p>
     * This method returns a singleton empty Sheet that is frozen (immutable).
     * Useful for initialization or when an empty Sheet is needed without creating a new instance.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> emptySheet = Sheet.empty();
     * // emptySheet.put("row", "col", 1); // throws IllegalStateException
     * }</pre>
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @return an empty, immutable Sheet instance
     */
    @SuppressWarnings({ "cast" })
    public static <R, C, V> Sheet<R, C, V> empty() {
        return (Sheet<R, C, V>) EMPTY_SHEET;
    }

    /**
     * Creates a new Sheet from row-oriented data.
     * <p>
     * This is a convenience factory method equivalent to calling the constructor with the same parameters.
     * Each inner array in the rows parameter represents a complete row of data.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {1, 2, 3},  // row1 values
     *         {4, 5, 6}   // row2 values
     *     }
     * );
     * }</pre>
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @param rows the data as a 2D array where each inner array represents a row
     * @return a new Sheet with the specified keys and data
     * @throws IllegalArgumentException if any keys are {@code null} or duplicated, or dimensions don't match
     */
    public static <R, C, V> Sheet<R, C, V> rows(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final Object[][] rows)
            throws IllegalArgumentException {
        return new Sheet<>(rowKeySet, columnKeySet, rows);
    }

    /**
     * Creates a new Sheet from row-oriented collection data.
     * <p>
     * Each inner collection represents a complete row of data. The order of values
     * in each inner collection must correspond to the order of column keys.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     List.of(
     *         List.of(1, 2, 3),  // row1 values
     *         List.of(4, 5, 6)   // row2 values
     *     )
     * );
     * }</pre>
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @param rows the data as a collection of collections where each inner collection represents a row
     * @return a new Sheet with the specified keys and data
     * @throws IllegalArgumentException if any keys are {@code null} or duplicated, or dimensions don't match
     */
    public static <R, C, V> Sheet<R, C, V> rows(final Collection<R> rowKeySet, final Collection<C> columnKeySet,
            final Collection<? extends Collection<? extends V>> rows) throws IllegalArgumentException {
        final Sheet<R, C, V> instance = new Sheet<>(rowKeySet, columnKeySet);

        final int rowLength = instance.rowLength();
        final int columnLength = instance.columnLength();

        if (N.notEmpty(rows)) {
            N.checkArgument(rows.size() == rowLength, "The size of row collection is not equal to size row key set"); //NOSONAR

            for (final Collection<? extends V> e : rows) {
                N.checkArgument(e.size() == columnLength, "The size of row is not equal to size of column key set");
            }

            instance.initIndexMap();

            instance._columnList = new ArrayList<>(columnLength);

            for (int i = 0; i < columnLength; i++) {
                instance._columnList.add(new ArrayList<>(rowLength));
            }

            for (final Collection<? extends V> row : rows) {
                final Iterator<? extends V> iter = row.iterator();

                for (int i = 0; i < columnLength; i++) {
                    instance._columnList.get(i).add(iter.next());
                }
            }

            instance._isInitialized = true;
        }

        return instance;

    }

    /**
     * Creates a new Sheet from column-oriented data.
     * <p>
     * Each inner array represents a complete column of data. This is useful when
     * your data is naturally organized by columns rather than rows.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.columns(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {
     *         {1, 2, 3},  // col1 values
     *         {4, 5, 6}   // col2 values
     *     }
     * );
     * }</pre>
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columns the data as a 2D array where each inner array represents a column
     * @return a new Sheet with the specified keys and data
     * @throws IllegalArgumentException if any keys are {@code null} or duplicated, or dimensions don't match
     */
    public static <R, C, V> Sheet<R, C, V> columns(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final Object[][] columns)
            throws IllegalArgumentException {
        final Sheet<R, C, V> instance = new Sheet<>(rowKeySet, columnKeySet);

        final int rowLength = instance.rowLength();
        final int columnLength = instance.columnLength();

        if (N.notEmpty(columns)) {
            N.checkArgument(columns.length == columnLength, "The length of column array is not equal to size of column key set");

            for (final Object[] e : columns) {
                N.checkArgument(e.length == rowLength, "The length of column is not equal to size of row key set");
            }

            instance.initIndexMap();

            instance._columnList = new ArrayList<>(columnLength);

            for (final Object[] column : columns) {
                instance._columnList.add(new ArrayList<>((List<V>) Arrays.asList(column)));
            }

            instance._isInitialized = true;
        }

        return instance;
    }

    /**
     * Creates a new Sheet from column-oriented collection data.
     * <p>
     * Each inner collection represents a complete column of data. The order of values
     * in each inner collection must correspond to the order of row keys.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.columns(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     List.of(
     *         List.of(1, 2, 3),  // col1 values
     *         List.of(4, 5, 6)   // col2 values
     *     )
     * );
     * }</pre>
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columns the data as a collection of collections where each inner collection represents a column
     * @return a new Sheet with the specified keys and data
     * @throws IllegalArgumentException if any keys are {@code null} or duplicated, or dimensions don't match
     */
    public static <R, C, V> Sheet<R, C, V> columns(final Collection<R> rowKeySet, final Collection<C> columnKeySet,
            final Collection<? extends Collection<? extends V>> columns) throws IllegalArgumentException {
        final Sheet<R, C, V> instance = new Sheet<>(rowKeySet, columnKeySet);

        final int rowLength = instance.rowLength();
        final int columnLength = instance.columnLength();

        if (N.notEmpty(columns)) {
            N.checkArgument(columns.size() == columnLength, "The size of column collection is not equal to size of column key set");

            for (final Collection<? extends V> e : columns) {
                N.checkArgument(e.size() == rowLength, "The size of column is not equal to size of row key set");
            }

            instance.initIndexMap();

            instance._columnList = new ArrayList<>(columnLength);

            for (final Collection<? extends V> column : columns) {
                instance._columnList.add(new ArrayList<>(column));
            }

            instance._isInitialized = true;
        }

        return instance;
    }

    /**
     * Returns an immutable set of all row keys in this Sheet.
     * <p>
     * The returned set maintains the insertion order of row keys. Modifications to the
     * returned set are not allowed and will throw {@code UnsupportedOperationException}.
     * This is useful for iterating over rows or checking row existence.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * ImmutableSet<String> rowKeys = sheet.rowKeySet(); // ["row1", "row2"]
     * 
     * // Iterate over rows
     * for (String rowKey : sheet.rowKeySet()) {
     *     List<Integer> rowValues = sheet.getRow(rowKey);
     * }
     * }</pre>
     *
     * @return an immutable set containing all row keys in insertion order
     * @see #columnKeySet()
     * @see #containsRow(Object)
     */
    public ImmutableSet<R> rowKeySet() {
        return ImmutableSet.wrap(_rowKeySet);
    }

    /**
     * Returns an immutable set of all column keys in this Sheet.
     * <p>
     * The returned set maintains the insertion order of column keys. Modifications to the
     * returned set are not allowed and will throw {@code UnsupportedOperationException}.
     * This is useful for iterating over columns or checking column existence.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * ImmutableSet<String> colKeys = sheet.columnKeySet(); // ["col1", "col2", "col3"]
     * 
     * // Iterate over columns
     * for (String colKey : sheet.columnKeySet()) {
     *     List<Integer> colValues = sheet.getColumn(colKey);
     * }
     * }</pre>
     *
     * @return an immutable set containing all column keys in insertion order
     * @see #rowKeySet()
     * @see #containsColumn(Object)
     */
    public ImmutableSet<C> columnKeySet() {
        return ImmutableSet.wrap(_columnKeySet);
    }

    /**
     * Checks whether the cell at the specified row and column contains a {@code null} value.
     * <p>
     * Returns {@code true} if the cell is {@code null} or if the Sheet has not been initialized
     * with data yet. Returns {@code false} if the cell contains a non-null value.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, null}, {3, 4}}
     * );
     * boolean isNull = sheet.isNull("row1", "col2"); // true
     * }</pre>
     *
     * @param rowKey the key identifying the row
     * @param columnKey the key identifying the column
     * @return {@code true} if the cell contains {@code null}, {@code false} otherwise
     * @throws IllegalArgumentException if the row key or column key doesn't exist in the Sheet
     * @see #isNull(int, int)
     * @see #get(Object, Object)
     */
    public boolean isNull(final R rowKey, final C columnKey) throws IllegalArgumentException {
        if (_isInitialized) {
            final int rowIndex = getRowIndex(rowKey);
            final int columnIndex = getColumnIndex(columnKey);

            return _columnList.get(columnIndex).get(rowIndex) == null;
        } else {
            checkRowKey(rowKey);
            checkColumnKey(columnKey);

            return true;
        }
    }

    /**
     * Checks whether the cell at the specified row and column indices contains a {@code null} value.
     * <p>
     * This method provides index-based access for checking null values. Indices are zero-based.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, null}, {3, 4}}
     * );
     * boolean isNull = sheet.isNull(0, 1); // true (row1, col2)
     * }</pre>
     *
     * @param rowIndex the zero-based index of the row
     * @param columnIndex the zero-based index of the column
     * @return {@code true} if the cell contains {@code null}, {@code false} otherwise
     * @throws IndexOutOfBoundsException if the indices are out of bounds
     * @see #isNull(Object, Object)
     */
    public boolean isNull(final int rowIndex, final int columnIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);
        checkColumnIndex(columnIndex);

        if (_isInitialized) {
            return _columnList.get(columnIndex).get(rowIndex) == null;
        } else {
            return true;
        }
    }

    /**
     * Checks whether the cell at the specified point contains a {@code null} value.
     * <p>
     * The Point object encapsulates both row and column indices for convenient access.
     * </p>
     *
     * <pre>{@code
     * Point point = Point.of(0, 1);
     * boolean isNull = sheet.isNull(point); // checks cell at row 0, column 1
     * }</pre>
     *
     * @param point the Point containing row and column indices
     * @return {@code true} if the cell contains {@code null}, {@code false} otherwise
     * @throws IndexOutOfBoundsException if the point's indices are out of bounds
     * @see #isNull(int, int)
     */
    public boolean isNull(final Point point) throws IndexOutOfBoundsException {
        return isNull(point.rowIndex, point.columnIndex);
    }

    /**
     * Retrieves the value stored in the cell identified by the specified row and column keys.
     * <p>
     * Returns {@code null} if the cell has not been initialized or explicitly contains {@code null}.
     * This method provides key-based access to cell values.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * Integer value = sheet.get("row1", "col2"); // returns 2
     * }</pre>
     *
     * @param rowKey the key identifying the row
     * @param columnKey the key identifying the column
     * @return the value in the cell, or {@code null} if the cell is empty
     * @throws IllegalArgumentException if the row key or column key doesn't exist
     * @see #get(int, int)
     * @see #put(Object, Object, Object)
     */
    @MayReturnNull
    public V get(final R rowKey, final C columnKey) throws IllegalArgumentException {
        if (_isInitialized) {
            final int rowIndex = getRowIndex(rowKey);
            final int columnIndex = getColumnIndex(columnKey);

            return _columnList.get(columnIndex).get(rowIndex);
        } else {
            checkRowKey(rowKey);
            checkColumnKey(columnKey);

            return null;
        }
    }

    /**
     * Retrieves the value stored in the cell at the specified row and column indices.
     * <p>
     * This method provides index-based access to cell values. Indices are zero-based.
     * Returns {@code null} if the cell has not been initialized or contains {@code null}.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * Integer value = sheet.get(0, 1); // returns 2 (row1, col2)
     * }</pre>
     *
     * @param rowIndex the zero-based index of the row
     * @param columnIndex the zero-based index of the column
     * @return the value in the cell, or {@code null} if the cell is empty
     * @throws IndexOutOfBoundsException if the indices are out of bounds
     * @see #get(Object, Object)
     */
    @MayReturnNull
    public V get(final int rowIndex, final int columnIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);
        checkColumnIndex(columnIndex);

        if (_isInitialized) {
            return _columnList.get(columnIndex).get(rowIndex);
        } else {
            return null;
        }
    }

    /**
     * Retrieves the value stored in the cell at the specified point.
     * <p>
     * The Point object encapsulates both row and column indices for convenient access.
     * This is a convenience method equivalent to calling {@code get(point.rowIndex, point.columnIndex)}.
     * </p>
     *
     * <pre>{@code
     * Point point = Point.of(0, 1);
     * Integer value = sheet.get(point); // gets value at row 0, column 1
     * }</pre>
     *
     * @param point the Point containing row and column indices
     * @return the value in the cell, or {@code null} if the cell is empty
     * @throws IndexOutOfBoundsException if the point's indices are out of bounds
     * @see #get(int, int)
     */
    @Beta
    public V get(final Point point) throws IndexOutOfBoundsException {
        return get(point.rowIndex, point.columnIndex);
    }

    /**
     * Sets or updates the value in the cell identified by the specified row and column keys.
     * <p>
     * If the cell already contains a value, it is replaced with the new value.
     * The Sheet must not be frozen for this operation to succeed.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * Integer oldValue = sheet.put("row1", "col2", 10); // returns 2, sets to 10
     * }</pre>
     *
     * @param rowKey the key identifying the row
     * @param columnKey the key identifying the column
     * @param value the new value to store in the cell (can be {@code null})
     * @return the previous value in the cell, or {@code null} if it was empty
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key or column key doesn't exist
     * @see #put(int, int, Object)
     * @see #get(Object, Object)
     */
    public V put(final R rowKey, final C columnKey, final V value) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();
        final int rowIndex = getRowIndex(rowKey);
        final int columnIndex = getColumnIndex(columnKey);

        init();

        //    if (!containsRow(rowKey)) {
        //        addRow(rowKey, null);
        //    }
        //
        //    if (!containsColumn(columnKey)) {
        //        addColumn(columnKey, null);
        //    }

        return put(rowIndex, columnIndex, value);
    }

    /**
     * Sets or updates the value in the cell at the specified row and column indices.
     * <p>
     * This method provides index-based access for setting cell values. Indices are zero-based.
     * The Sheet must not be frozen for this operation to succeed.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * Integer oldValue = sheet.put(0, 1, 10); // returns 2, sets cell[0][1] to 10
     * }</pre>
     *
     * @param rowIndex the zero-based index of the row
     * @param columnIndex the index of the column
     * @param value the new value to be stored in the cell
     * @return the previous value stored in the cell
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IndexOutOfBoundsException if the specified indices are out of bounds
     */
    public V put(final int rowIndex, final int columnIndex, final V value) throws IllegalStateException, IndexOutOfBoundsException {
        checkFrozen();
        checkRowIndex(rowIndex);
        checkColumnIndex(columnIndex);

        init();

        final V previousValue = _columnList.get(columnIndex).get(rowIndex);
        _columnList.get(columnIndex).set(rowIndex, value);

        return previousValue;
    }

    /**
     * Inserts or updates a value in the cell identified by the specified Point.
     * The Point represents the row index and column index of the cell.
     * If the cell already contains a value, the existing value is replaced with the new value.
     * If the cell does not exist, a new cell is created at the specified point with the provided value.
     *
     * @param point the Point of the cell
     * @param value the new value to be stored in the cell
     * @return the previous value stored in the cell
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IndexOutOfBoundsException if the specified indices are out of bounds
     */
    @Beta
    public V put(final Point point, final V value) throws IllegalStateException, IndexOutOfBoundsException {
        return put(point.rowIndex, point.columnIndex, value);
    }

    /**
     * <p>Copies all values from the source Sheet into this Sheet.</p>
     *
     * <p>This method transfers data from the source Sheet into this Sheet. The source Sheet must have row keys
     * and column keys that are contained within this Sheet. Values from the source Sheet will replace any
     * existing values in the corresponding cells of this Sheet.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create two sheets
     * Sheet&lt;String, String, Integer&gt; targetSheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {1, 2, 3},
     *         {4, 5, 6}
     *     }
     * );
     *
     * Sheet&lt;String, String, Integer&gt; sourceSheet = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1", "col3"),
     *     new Integer[][] {
     *         {10, 30}
     *     }
     * );
     *
     * // Copy values from source to target
     * targetSheet.putAll(sourceSheet);
     *
     * // Now targetSheet contains:
     * // [10, 2, 30]
     * // [1, 2, 3]
     * </pre>
     *
     * @param source the source Sheet from which to get the values
     * @throws IllegalStateException if this Sheet is frozen and cannot be modified
     * @throws IllegalArgumentException if the source Sheet contains row keys or column keys that are not present in this Sheet
     * @see #putAll(Sheet, BiFunction)
     * @see #put(Object, Object, Object)
     * @see #merge(Sheet, BiFunction)
     */
    public void putAll(final Sheet<? extends R, ? extends C, ? extends V> source) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        if (!this.rowKeySet().containsAll(source.rowKeySet())) {
            throw new IllegalArgumentException(source.rowKeySet() + " are not all included in this sheet with row key set: " + this.rowKeySet());
        }

        if (!this.columnKeySet().containsAll(source.columnKeySet())) {
            throw new IllegalArgumentException(source.columnKeySet() + " are not all included in this sheet with column key set: " + this.columnKeySet());
        }

        final Sheet<R, C, ? extends V> tmp = (Sheet<R, C, ? extends V>) source;
        int rowIndex = 0;
        int columnIndex = 0;

        for (final R r : tmp.rowKeySet()) {
            rowIndex = getRowIndex(r);
            for (final C c : tmp.columnKeySet()) {
                // this.put(r, c, tmp.get(r, c));
                columnIndex = getColumnIndex(c);

                put(rowIndex, columnIndex, tmp.get(r, c));
            }
        }
    }

    /**
     * <p>Merges all values from the source Sheet into this Sheet using the provided merge function to resolve conflicts.</p>
     *
     * <p>This method combines data from the source Sheet into this Sheet. When a cell in both sheets contains a value,
     * the provided merge function determines how to combine them, taking the current value and source value as parameters.
     * The source Sheet must have row keys and column keys that are contained within this Sheet.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create two sheets with overlapping data
     * Sheet&lt;String, String, Integer&gt; targetSheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {
     *         {1, 2},
     *         {3, 4}
     *     }
     * );
     *
     * Sheet&lt;String, String, Integer&gt; sourceSheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {
     *         {5, 6},
     *         {7, 8}
     *     }
     * );
     *
     * // Merge source into target, adding values together where they overlap
     * targetSheet.putAll(sourceSheet, (target, source) -> target + source);
     *
     * // Now targetSheet contains:
     * // col1: [6, 10]
     * // col2: [8, 12]
     * </pre>
     *
     * @param source the source Sheet from which to get the values
     * @param mergeFunction the function to resolve conflicts when both sheets have a value for the same cell;
     *        takes the current value from this sheet and the value from the source sheet as parameters
     * @throws IllegalStateException if this Sheet is frozen and cannot be modified
     * @throws IllegalArgumentException if the source Sheet contains row keys or column keys that are not present in this Sheet
     * @see #putAll(Sheet)
     * @see #put(Object, Object, Object)
     * @see #merge(Sheet, BiFunction)
     */
    public void putAll(final Sheet<? extends R, ? extends C, ? extends V> source, final BiFunction<? super V, ? super V, ? extends V> mergeFunction)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        if (!this.rowKeySet().containsAll(source.rowKeySet())) {
            throw new IllegalArgumentException(source.rowKeySet() + " are not all included in this sheet with row key set: " + this.rowKeySet());
        }

        if (!this.columnKeySet().containsAll(source.columnKeySet())) {
            throw new IllegalArgumentException(source.columnKeySet() + " are not all included in this sheet with column key set: " + this.columnKeySet());
        }

        final Sheet<R, C, ? extends V> tmp = (Sheet<R, C, ? extends V>) source;
        int rowIndex = 0;
        int columnIndex = 0;

        for (final R r : tmp.rowKeySet()) {
            rowIndex = getRowIndex(r);
            for (final C c : tmp.columnKeySet()) {
                // this.put(r, c, tmp.get(r, c));
                columnIndex = getColumnIndex(c);

                put(rowIndex, columnIndex, mergeFunction.apply(get(rowIndex, columnIndex), tmp.get(r, c)));
            }
        }
    }

    /**
     * Removes the value stored in the cell identified by the specified row key and column key.
     * <p>
     * Sets the cell value to {@code null} and returns the previous value. This operation
     * requires the Sheet to be mutable (not frozen) and the cell position to exist.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * Integer removed = sheet.remove("row1", "col2"); // returns 2
     * Integer nowNull = sheet.get("row1", "col2"); // returns null
     * }</pre>
     *
     * @param rowKey the row key of the cell to clear
     * @param columnKey the column key of the cell to clear
     * @return the value that was previously stored in the cell, or {@code null} if the cell was empty
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key or column key doesn't exist
     * @see #remove(int, int)
     * @see #put(Object, Object, Object)
     */
    @MayReturnNull
    public V remove(final R rowKey, final C columnKey) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        if (_isInitialized) {
            final int rowIndex = getRowIndex(rowKey);
            final int columnIndex = getColumnIndex(columnKey);

            return remove(rowIndex, columnIndex);
        } else {
            checkRowKey(rowKey);
            checkColumnKey(columnKey);

            return null;
        }
    }

    /**
     * Removes the value stored in the cell at the specified row and column indices.
     * <p>
     * Sets the cell value to {@code null} and returns the previous value. Uses zero-based
     * indexing. This operation requires the Sheet to be mutable (not frozen).
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * Integer removed = sheet.remove(0, 1); // removes value at row1, col2; returns 2
     * Integer nowNull = sheet.get(0, 1); // returns null
     * }</pre>
     *
     * @param rowIndex the zero-based index of the row
     * @param columnIndex the zero-based index of the column
     * @return the value that was previously stored in the cell, or {@code null} if the cell was empty
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IndexOutOfBoundsException if rowIndex < 0 or rowIndex >= rowLength() or columnIndex < 0 or columnIndex >= columnLength()
     * @see #remove(Object, Object)
     * @see #put(int, int, Object)
     */
    @MayReturnNull
    public V remove(final int rowIndex, final int columnIndex) throws IllegalStateException, IndexOutOfBoundsException {
        checkFrozen();
        checkRowIndex(rowIndex);
        checkColumnIndex(columnIndex);

        if (_isInitialized) {
            return _columnList.get(columnIndex).set(rowIndex, null);
        } else {
            return null;
        }
    }

    /**
     * Removes the value stored in the cell at the specified Point.
     * <p>
     * Sets the cell value to {@code null} and returns the previous value. The Point
     * encapsulates both row and column indices for convenient access.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * Point point = Point.of(1, 0); // row2, col1
     * Integer removed = sheet.remove(point); // returns 3
     * }</pre>
     *
     * @param point the Point containing the row and column indices of the cell
     * @return the value that was previously stored in the cell, or {@code null} if the cell was empty
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IndexOutOfBoundsException if the point indices are out of bounds
     * @see #remove(int, int)
     * @see #put(Point, Object)
     */
    @Beta
    public V remove(final Point point) throws IllegalStateException, IndexOutOfBoundsException {
        return remove(point.rowIndex, point.columnIndex);
    }

    /**
     * Checks if the Sheet contains a cell identified by the specified row key and column key.
     * <p>
     * This method verifies if the combination of row and column keys exists in the Sheet structure.
     * It returns {@code true} if both keys are present, regardless of the cell's value (including null).
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2")
     * );
     * 
     * boolean exists = sheet.contains("row1", "col1"); // true
     * boolean missing = sheet.contains("row3", "col1"); // false
     * }</pre>
     *
     * @param rowKey the row key to check
     * @param columnKey the column key to check
     * @return {@code true} if the cell position exists in the Sheet structure, {@code false} otherwise
     * @see #contains(Object, Object, Object)
     * @see #containsRow(Object)
     * @see #containsColumn(Object)
     */
    public boolean contains(final R rowKey, final C columnKey) {
        return _rowKeySet.contains(rowKey) && _columnKeySet.contains(columnKey);
    }

    /**
     * Checks if the Sheet contains a cell with the specified row key, column key, and value.
     * <p>
     * This method verifies both the existence of the cell position and that it contains
     * the specified value. Uses {@code Objects.equals()} for value comparison, so it
     * correctly handles {@code null} values.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}}
     * );
     * 
     * boolean hasValue = sheet.contains("row1", "col1", 1); // true
     * boolean hasNull = sheet.contains("row2", "col2", null); // true
     * boolean wrong = sheet.contains("row1", "col1", 5); // false
     * }</pre>
     *
     * @param rowKey the row key of the cell to check
     * @param columnKey the column key of the cell to check
     * @param value the value to check for equality in the cell
     * @return {@code true} if the cell exists and contains the specified value, {@code false} otherwise
     * @see #contains(Object, Object)
     * @see #containsValue(Object)
     * @see #get(Object, Object)
     */
    public boolean contains(final R rowKey, final C columnKey, final Object value) {
        return N.equals(get(rowKey, columnKey), value);
    }

    /**
     * Checks if the Sheet contains any cell with the specified value.
     * <p>
     * Searches through all cells in the Sheet to find one containing the specified value.
     * Uses {@code Objects.equals()} for comparison, so it correctly handles {@code null} values.
     * For uninitialized Sheets, only returns {@code true} if searching for {@code null}.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}}
     * );
     * 
     * boolean hasOne = sheet.containsValue(1); // true
     * boolean hasNull = sheet.containsValue(null); // true
     * boolean hasFive = sheet.containsValue(5); // false
     * }</pre>
     *
     * @param value the value to search for in all cells
     * @return {@code true} if any cell contains the specified value, {@code false} otherwise
     * @see #contains(Object, Object, Object)
     * @see #countOfNonNullValue()
     */
    public boolean containsValue(final Object value) {
        //        if (value == null) {
        //            for (R r : rowKeySet()) {
        //                for (C c : columnKeySet()) {
        //                    if (this.get(r, c) == null) {
        //                        return true;
        //                    }
        //                }
        //            }
        //        } else {
        //            for (R r : rowKeySet()) {
        //                for (C c : columnKeySet()) {
        //                    if (value.equals(this.get(r, c))) {
        //                        return true;
        //                    }
        //                }
        //            }
        //        }
        //
        //        return false;

        if (_isInitialized) {
            for (final List<V> column : _columnList) {
                //noinspection SuspiciousMethodCalls
                if (column.contains(value)) {
                    return true;
                }
            }

            return false;
        } else {
            return value == null;
        }
    }

    /**
     * Retrieves all the values in the row identified by the specified row key.
     * <p>
     * Returns an immutable list containing all values in the specified row, in the order
     * corresponding to the column keys. The list may contain {@code null} values if cells
     * in the row are empty.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {1, 2, 3},
     *         {4, null, 6}
     *     }
     * );
     * List<Integer> row1 = sheet.getRow("row1"); // [1, 2, 3]
     * List<Integer> row2 = sheet.getRow("row2"); // [4, null, 6]
     * }</pre>
     *
     * @param rowKey the row key identifying the row to retrieve
     * @return an immutable list of values in the row, in column order
     * @throws IllegalArgumentException if the row key does not exist in the Sheet
     * @see #getColumn(Object)
     * @see #setRow(Object, Collection)
     */
    public ImmutableList<V> getRow(final R rowKey) throws IllegalArgumentException {
        final int columnLength = columnLength();
        final List<V> row = new ArrayList<>(columnLength);

        if (_isInitialized) {
            final int rowIndex = getRowIndex(rowKey);

            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                row.add(_columnList.get(columnIndex).get(rowIndex));
            }
        } else {
            checkRowKey(rowKey);

            N.fill(row, 0, columnLength, null);
        }

        return ImmutableList.wrap(row);
    }

    /**
     * Sets the values for a specific row in the Sheet.
     * <p>
     * Replaces all existing values in the specified row with the values from the provided collection.
     * The values must be in the same order as the column keys. If the collection is empty, all cells
     * in the row will be set to {@code null}.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * 
     * // Replace entire row
     * sheet.setRow("row1", List.of(7, 8, 9));
     * // row1 now contains: [7, 8, 9]
     * 
     * // Clear row (set all to null)
     * sheet.setRow("row2", List.of());
     * }</pre>
     *
     * @param rowKey the key of the row to be set
     * @param row the collection of values to set in the row; must match the number of columns or be empty
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist or the collection size doesn't match column count (unless empty)
     * @see #getRow(Object)
     * @see #updateRow(Object, Function)
     */
    public void setRow(final R rowKey, final Collection<? extends V> row) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int rowIndex = getRowIndex(rowKey);
        final int columnLength = columnLength();

        if (N.notEmpty(row) && row.size() != columnLength) {
            throw new IllegalArgumentException("The size of specified row: " + row.size() + " doesn't match the size of column key set: " + columnLength); //NOSONAR
        }

        init();

        if (N.isEmpty(row)) {
            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).set(rowIndex, null);
            }
        } else {
            final Iterator<? extends V> iter = row.iterator();

            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).set(rowIndex, iter.next());
            }
        }
    }

    /**
     * Adds a new row to the Sheet at the end.
     * <p>
     * Creates a new row with the specified key and values. The row is appended after all existing rows.
     * The values must be provided in the same order as the column keys. If an empty collection is provided,
     * the new row will contain all {@code null} values.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}}
     * );
     * 
     * // Add new row with values
     * sheet.addRow("row2", List.of(4, 5, 6));
     * 
     * // Add empty row (all nulls)
     * sheet.addRow("row3", List.of());
     * }</pre>
     *
     * @param rowKey the unique key for the new row; must not already exist in the Sheet
     * @param row the collection of values for the new row; must match the number of columns or be empty
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key already exists or the collection size doesn't match column count (unless empty)
     * @see #addRow(int, Object, Collection)
     * @see #removeRow(Object)
     */
    public void addRow(final R rowKey, final Collection<? extends V> row) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        if (_rowKeySet.contains(rowKey)) {
            throw new IllegalArgumentException("Row '" + rowKey + "' already existed"); //NOSONAR
        }

        final int rowLength = rowLength();
        final int columnLength = columnLength();

        if (N.notEmpty(row) && row.size() != columnLength) {
            throw new IllegalArgumentException("The size of specified row: " + row.size() + " doesn't match the size of column key set: " + columnLength);
        }

        init();

        _rowKeySet.add(rowKey);
        _rowKeyIndexMap.put(rowKey, rowLength);

        if (N.isEmpty(row)) {
            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).add(null);
            }
        } else {
            final Iterator<? extends V> iter = row.iterator();

            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).add(iter.next());
            }
        }
    }

    /**
     * Inserts a new row at the specified index in the Sheet.
     * <p>
     * Creates a new row with the specified key and values at the given position. Existing rows
     * at and after the specified index are shifted down. The index must be between 0 (insert at beginning)
     * and rowLength() (append at end).
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {5, 6}}
     * );
     * 
     * // Insert row at index 1 (between row1 and row3)
     * sheet.addRow(1, "row2", List.of(3, 4));
     * // Sheet now contains rows: ["row1", "row2", "row3"]
     * }</pre>
     *
     * @param rowIndex the zero-based index where the row should be inserted; must be >= 0 and <= rowLength()
     * @param rowKey the unique key for the new row; must not already exist in the Sheet
     * @param row the collection of values for the new row; must match the number of columns or be empty
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IndexOutOfBoundsException if rowIndex < 0 or rowIndex > rowLength()
     * @throws IllegalArgumentException if the row key already exists or the collection size doesn't match column count (unless empty)
     * @see #addRow(Object, Collection)
     * @see #moveRow(Object, int)
     */
    public void addRow(final int rowIndex, final R rowKey, final Collection<? extends V> row)
            throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();

        final int rowLength = rowLength();
        final int columnLength = columnLength();

        N.checkPositionIndex(rowIndex, rowLength);

        if (_rowKeySet.contains(rowKey)) {
            throw new IllegalArgumentException("Row '" + rowKey + "' already existed");
        }

        if (N.notEmpty(row) && row.size() != columnLength) {
            throw new IllegalArgumentException("The size of specified row: " + row.size() + " doesn't match the size of column key set: " + columnLength);
        }

        if (rowIndex == rowLength) {
            addRow(rowKey, row);
            return;
        }

        init();

        final List<R> tmp = new ArrayList<>(rowLength + 1);
        tmp.addAll(_rowKeySet);
        tmp.add(rowIndex, rowKey);

        _rowKeySet.clear();
        _rowKeySet.addAll(tmp);

        for (int i = _rowKeyIndexMap.size() - 1; i >= rowIndex; i--) {
            _rowKeyIndexMap.put(_rowKeyIndexMap.getByValue(i), i + 1);
        }

        _rowKeyIndexMap.put(rowKey, rowIndex);

        if (N.isEmpty(row)) {
            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).add(rowIndex, null);
            }
        } else {
            final Iterator<? extends V> iter = row.iterator();

            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).add(rowIndex, iter.next());
            }
        }

    }

    /**
     * Updates the values in the row identified by the provided row key using the provided function.
     * <p>
     * Applies the given function to each value in the specified row, replacing the original value
     * with the result. The function is called once for each cell in the row, including {@code null} values.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * 
     * // Double all values in row1
     * sheet.updateRow("row1", v -> v == null ? null : v * 2);
     * // row1 now contains: [2, 4, 6]
     * 
     * // Convert to negative values
     * sheet.updateRow("row2", v -> v == null ? 0 : -v);
     * }</pre>
     *
     * @param rowKey the key of the row to be updated
     * @param func the function to apply to each value in the row; receives current value (may be null) and returns new value
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist in the Sheet
     * @see #updateColumn(Object, Function)
     * @see #updateAll(Function)
     */
    public void updateRow(final R rowKey, final Function<? super V, ? extends V> func) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int rowIndex = this.getRowIndex(rowKey);

        if (columnLength() > 0) {
            this.init();

            for (final List<V> column : _columnList) {
                column.set(rowIndex, func.apply(column.get(rowIndex)));
            }
        }
    }

    /**
     * Removes the row identified by the provided row key from the Sheet.
     * <p>
     * Deletes the entire row and shifts all subsequent rows up. The row key is removed from
     * the Sheet and cannot be reused unless re-added. All values in the removed row are lost.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     * 
     * sheet.removeRow("row2");
     * // Sheet now contains only row1 and row3
     * // sheet.containsRow("row2") returns false
     * }</pre>
     *
     * @param rowKey the key of the row to be removed
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist in the Sheet
     */
    public void removeRow(final R rowKey) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        checkRowKey(rowKey);

        _rowKeySet.remove(rowKey);

        if (_rowKeyIndexMap != null) {
            final int columnLength = columnLength();
            final int newRowSize = rowLength();
            final int removedRowIndex = _rowKeyIndexMap.remove(rowKey);

            if (removedRowIndex == newRowSize) {
                // removed the last row.
            } else {
                for (int i = removedRowIndex; i < newRowSize; i++) {
                    _rowKeyIndexMap.put(_rowKeyIndexMap.getByValue(i + 1), i);
                }
            }

            if (_isInitialized) {
                for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                    _columnList.get(columnIndex).remove(removedRowIndex); //NOSONAR
                }
            }
        }
    }

    /**
     * Moves the row identified by the provided row key to a new position in the Sheet.
     * <p>
     * Repositions a row to a different index while maintaining all its data. Other rows are
     * shifted accordingly. The row key remains associated with the same row data.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     * 
     * // Move row2 to the beginning (index 0)
     * sheet.moveRow("row2", 0);
     * // Row order is now: ["row2", "row1", "row3"]
     * }</pre>
     *
     * @param rowKey the key of the row to be moved
     * @param newRowIndex the new zero-based index where the row should be positioned
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist in the Sheet
     * @throws IndexOutOfBoundsException if newRowIndex < 0 or newRowIndex >= rowLength()
     * @see #swapRowPosition(Object, Object)
     */
    public void moveRow(final R rowKey, final int newRowIndex) throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        checkFrozen();

        this.checkRowIndex(newRowIndex);

        final int rowIndex = this.getRowIndex(rowKey);
        final List<R> tmp = new ArrayList<>(rowLength());
        tmp.addAll(_rowKeySet);
        tmp.add(newRowIndex, tmp.remove(rowIndex));

        _rowKeySet.clear();
        _rowKeySet.addAll(tmp);

        _rowKeyIndexMap = null;

        if (_isInitialized && _columnList.size() > 0) {
            for (final List<V> column : _columnList) {
                column.add(newRowIndex, column.remove(rowIndex));
            }
        }
    }

    /**
     * Swaps the positions of two rows in the Sheet.
     * <p>
     * Exchanges the positions of two rows while maintaining their keys and data associations.
     * This is more efficient than using multiple move operations for a simple swap.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     * 
     * // Swap row1 and row3
     * sheet.swapRowPosition("row1", "row3");
     * // Row order is now: ["row3", "row2", "row1"]
     * // Data follows the rows: {{5, 6}, {3, 4}, {1, 2}}
     * }</pre>
     *
     * @param rowKeyA the key of the first row to swap
     * @param rowKeyB the key of the second row to swap
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if either row key does not exist in the Sheet
     * @see #moveRow(Object, int)
     * @see #swapColumnPosition(Object, Object)
     */
    public void swapRowPosition(final R rowKeyA, final R rowKeyB) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int rowIndexA = this.getRowIndex(rowKeyA);
        final int rowIndexB = this.getRowIndex(rowKeyB);

        final List<R> tmp = new ArrayList<>(rowLength());
        tmp.addAll(_rowKeySet);
        final R tmpRowKeyA = tmp.get(rowIndexA);
        tmp.set(rowIndexA, tmp.get(rowIndexB));
        tmp.set(rowIndexB, tmpRowKeyA);

        _rowKeySet.clear();
        _rowKeySet.addAll(tmp);

        _rowKeyIndexMap.forcePut(tmp.get(rowIndexA), rowIndexA);
        _rowKeyIndexMap.forcePut(tmp.get(rowIndexB), rowIndexB);

        if (_isInitialized && _columnList.size() > 0) {
            V tmpVal = null;

            for (final List<V> column : _columnList) {
                tmpVal = column.get(rowIndexA);
                column.set(rowIndexA, column.get(rowIndexB));
                column.set(rowIndexB, tmpVal);
            }
        }
    }

    /**
     * Renames a row in the Sheet.
     * <p>
     * Changes the key associated with a row while maintaining its position and data.
     * The new key must not already exist in the Sheet.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * // Rename row1 to rowA
     * sheet.renameRow("row1", "rowA");
     * // sheet.containsRow("row1") returns false
     * // sheet.containsRow("rowA") returns true
     * // Data remains unchanged: sheet.get("rowA", "col1") returns 1
     * }</pre>
     *
     * @param rowKey the current key of the row to rename
     * @param newRowKey the new key for the row; must not already exist
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if rowKey doesn't exist or newRowKey already exists
     * @see #renameColumn(Object, Object)
     */
    public void renameRow(final R rowKey, final R newRowKey) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();
        checkRowKey(rowKey);

        if (_rowKeySet.contains(newRowKey)) {
            throw new IllegalArgumentException("Invalid new row key: " + N.toString(newRowKey) + ". It's already in the row key set.");
        }

        final int rowIndex = this.getRowIndex(rowKey);
        final List<R> tmp = new ArrayList<>(_rowKeySet);
        tmp.set(rowIndex, newRowKey);

        _rowKeySet.clear();
        _rowKeySet.addAll(tmp);

        if (N.notEmpty(_rowKeyIndexMap)) {
            _rowKeyIndexMap.put(newRowKey, _rowKeyIndexMap.remove(rowKey));
        }
    }

    /**
     * Checks if the Sheet contains a row identified by the specified row key.
     * <p>
     * Tests for the existence of a row with the given key. This method is useful
     * before performing row operations to avoid exceptions.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * boolean exists = sheet.containsRow("row1"); // true
     * boolean missing = sheet.containsRow("row3"); // false
     * }</pre>
     *
     * @param rowKey the row key to check
     * @return {@code true} if the row exists, {@code false} otherwise
     * @see #containsColumn(Object)
     * @see #contains(Object, Object)
     */
    public boolean containsRow(final R rowKey) {
        return _rowKeySet.contains(rowKey);
    }

    /**
     * Retrieves a map representing a row in the Sheet.
     * <p>
     * Returns a map where keys are column keys and values are the cell values for the specified row.
     * The map maintains the column order as defined in the Sheet. Values may be {@code null}.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, null, 6}}
     * );
     * 
     * Map<String, Integer> row1Map = sheet.row("row1");
     * // Returns: {"col1"=1, "col2"=2, "col3"=3}
     * 
     * Map<String, Integer> row2Map = sheet.row("row2");
     * // Returns: {"col1"=4, "col2"=null, "col3"=6}
     * }</pre>
     *
     * @param rowKey the key identifying the row
     * @return a map of column keys to cell values for the specified row
     * @throws IllegalArgumentException if the row key does not exist in the Sheet
     * @see #column(Object)
     * @see #rowMap()
     */
    public Map<C, V> row(final R rowKey) throws IllegalArgumentException {
        final int columnLength = columnLength();
        final Map<C, V> rowMap = N.newLinkedHashMap(columnLength);

        if (_isInitialized) {
            final int rowIndex = getRowIndex(rowKey);
            int columnIndex = 0;

            for (final C columnKey : this.columnKeySet()) {
                rowMap.put(columnKey, _columnList.get(columnIndex++).get(rowIndex));
            }
        } else {
            checkRowKey(rowKey);

            for (final C columnKey : this.columnKeySet()) {
                rowMap.put(columnKey, null);
            }
        }

        return rowMap;
    }

    /**
     * Retrieves a map representing all rows in the Sheet.
     * <p>
     * Returns a nested map structure where the outer map's keys are row keys and values are
     * maps representing each row. The inner maps have column keys as keys and cell values as values.
     * This provides a complete view of the Sheet's data organized by rows.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * Map<String, Map<String, Integer>> allRows = sheet.rowMap();
     * // Returns:
     * // {
     * //   "row1" = {"col1"=1, "col2"=2},
     * //   "row2" = {"col1"=3, "col2"=4}
     * // }
     * }</pre>
     *
     * @return a map of row keys to row maps, where each row map contains column keys to cell values
     * @see #columnMap()
     * @see #row(Object)
     */
    public Map<R, Map<C, V>> rowMap() {
        final Map<R, Map<C, V>> result = N.newLinkedHashMap(this.rowKeySet().size());

        for (final R rowKey : this.rowKeySet()) {
            result.put(rowKey, row(rowKey));
        }

        return result;
    }

    /**
     * Retrieves all the values in the column identified by the provided column key.
     * <p>
     * Returns an immutable list containing all values in the specified column, in the order
     * corresponding to the row keys. The list may contain {@code null} values if cells
     * in the column are empty.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {
     *         {1, 2},
     *         {3, null},
     *         {5, 6}
     *     }
     * );
     * List<Integer> col1 = sheet.getColumn("col1"); // [1, 3, 5]
     * List<Integer> col2 = sheet.getColumn("col2"); // [2, null, 6]
     * }</pre>
     *
     * @param columnKey the key identifying the column to retrieve
     * @return an immutable list of values in the column, in row order
     * @throws IllegalArgumentException if the column key does not exist in the Sheet
     * @see #getRow(Object)
     * @see #setColumn(Object, Collection)
     */
    public ImmutableList<V> getColumn(final C columnKey) throws IllegalArgumentException {

        //    if (_initialized) {
        //        column = _columnList.get(getColumnIndex(columnKey));
        //    } else {
        //        final int rowLength = rowLength();
        //        checkColumnKey(columnKey);
        //        column = new ArrayList<>(rowLength);
        //        N.fill(column, 0, rowLength, null);
        //    }

        if (!_isInitialized) {
            init();
        }

        final List<V> column = _columnList.get(getColumnIndex(columnKey));

        return ImmutableList.wrap(column);
    }

    /**
     * Sets the values for a specific column in the Sheet.
     * <p>
     * Replaces all existing values in the specified column with the values from the provided collection.
     * The values must be in the same order as the row keys. If the collection is empty, all cells
     * in the column will be set to {@code null}.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     * 
     * // Replace entire column
     * sheet.setColumn("col1", List.of(7, 8, 9));
     * // col1 now contains: [7, 8, 9]
     * 
     * // Clear column (set all to null)
     * sheet.setColumn("col2", List.of());
     * }</pre>
     *
     * @param columnKey the key of the column to be set
     * @param column the collection of values to set in the column; must match the number of rows or be empty
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the column key doesn't exist or collection size doesn't match row count (unless empty)
     * @see #getColumn(Object)
     * @see #updateColumn(Object, Function)
     */
    public void setColumn(final C columnKey, final Collection<? extends V> column) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndex = getColumnIndex(columnKey);

        final int rowLength = rowLength();

        if (N.notEmpty(column) && column.size() != rowLength) {
            throw new IllegalArgumentException("The size of specified column: " + column.size() + " doesn't match the size of row key set: " + rowLength); //NOSONAR
        }

        init();

        if (N.isEmpty(column)) {
            N.fill(_columnList.get(columnIndex), 0, rowLength, null);
        } else {
            _columnList.set(columnIndex, new ArrayList<>(column));
        }
    }

    /**
     * Adds a new column to the Sheet at the end.
     * <p>
     * Creates a new column with the specified key and values. The column is appended after all existing columns.
     * The values must be provided in the same order as the row keys. If an empty collection is provided,
     * the new column will contain all {@code null} values.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1"),
     *     new Integer[][] {{1}, {2}, {3}}
     * );
     * 
     * // Add new column with values
     * sheet.addColumn("col2", List.of(4, 5, 6));
     * 
     * // Add empty column (all nulls)
     * sheet.addColumn("col3", List.of());
     * }</pre>
     *
     * @param columnKey the unique key for the new column; must not already exist in the Sheet
     * @param column the collection of values for the new column; must match the number of rows or be empty
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the column key already exists or collection size doesn't match row count (unless empty)
     * @see #addColumn(int, Object, Collection)
     * @see #removeColumn(Object)
     */
    public void addColumn(final C columnKey, final Collection<? extends V> column) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        if (_columnKeySet.contains(columnKey)) {
            throw new IllegalArgumentException("Column '" + columnKey + "' already existed");
        }

        final int rowLength = rowLength();
        final int columnLength = columnLength();

        if (N.notEmpty(column) && column.size() != rowLength) {
            throw new IllegalArgumentException("The size of specified column: " + column.size() + " doesn't match the size of row key set: " + rowLength);
        }

        init();

        _columnKeySet.add(columnKey);
        _columnKeyIndexMap.put(columnKey, columnLength);

        if (N.isEmpty(column)) {
            final List<V> newColumn = new ArrayList<>();
            N.fill(newColumn, 0, rowLength, null);
            _columnList.add(newColumn);
        } else {
            _columnList.add(new ArrayList<>(column));
        }
    }

    /**
     * Inserts a new column at the specified index in the Sheet.
     * <p>
     * Creates a new column with the specified key and values at the given position. Existing columns
     * at and after the specified index are shifted right. The index must be between 0 (insert at beginning)
     * and columnLength() (append at end).
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col3"),
     *     new Integer[][] {{1, 5}, {2, 6}, {3, 7}}
     * );
     * 
     * // Insert column at index 1 (between col1 and col3)
     * sheet.addColumn(1, "col2", List.of(10, 20, 30));
     * // Sheet now contains columns: ["col1", "col2", "col3"]
     * }</pre>
     *
     * @param columnIndex the zero-based index where the column should be inserted; must be >= 0 and <= columnLength()
     * @param columnKey the unique key for the new column; must not already exist in the Sheet
     * @param column the collection of values for the new column; must match the number of rows or be empty
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IndexOutOfBoundsException if columnIndex < 0 or columnIndex > columnLength()
     * @throws IllegalArgumentException if the column key already exists or collection size doesn't match row count (unless empty)
     * @see #addColumn(Object, Collection)
     * @see #moveColumn(Object, int)
     */
    public void addColumn(final int columnIndex, final C columnKey, final Collection<? extends V> column)
            throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();

        final int rowLength = rowLength();
        final int columnLength = columnLength();

        N.checkPositionIndex(columnIndex, columnLength);

        if (_columnKeySet.contains(columnKey)) {
            throw new IllegalArgumentException("Column '" + columnKey + "' already existed");
        }

        if (N.notEmpty(column) && column.size() != rowLength) {
            throw new IllegalArgumentException("The size of specified column: " + column.size() + " doesn't match the size of row key set: " + rowLength);
        }

        if (columnIndex == columnLength) {
            addColumn(columnKey, column);
            return;
        }

        init();

        final List<C> tmp = new ArrayList<>(columnLength + 1);
        tmp.addAll(_columnKeySet);
        tmp.add(columnIndex, columnKey);

        _columnKeySet.clear();
        _columnKeySet.addAll(tmp);

        for (int i = _columnKeyIndexMap.size() - 1; i >= columnIndex; i--) {
            _columnKeyIndexMap.put(_columnKeyIndexMap.getByValue(i), i + 1);
        }

        _columnKeyIndexMap.put(columnKey, columnIndex);

        if (N.isEmpty(column)) {
            final List<V> newColumn = new ArrayList<>();
            N.fill(newColumn, 0, rowLength, null);
            _columnList.add(columnIndex, newColumn);
        } else {
            _columnList.add(columnIndex, new ArrayList<>(column));
        }
    }

    /**
     * Updates the values in the column identified by the provided column key using the provided function.
     * <p>
     * Applies the given function to each value in the specified column, replacing the original value
     * with the result. The function is called once for each cell in the column, including {@code null} values.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 10}, {2, 20}, {3, 30}}
     * );
     * 
     * // Double all values in col1
     * sheet.updateColumn("col1", v -> v == null ? null : v * 2);
     * // col1 now contains: [2, 4, 6]
     * 
     * // Add 5 to all values in col2
     * sheet.updateColumn("col2", v -> v == null ? 0 : v + 5);
     * }</pre>
     *
     * @param columnKey the key of the column to be updated
     * @param func the function to apply to each value in the column; receives current value (may be null) and returns new value
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist in the Sheet
     * @see #updateRow(Object, Function)
     * @see #updateAll(Function)
     */
    public void updateColumn(final C columnKey, final Function<? super V, ? extends V> func) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndex = this.getColumnIndex(columnKey);

        if (rowLength() > 0) {
            this.init();

            final int rowLength = rowLength();
            final List<V> column = _columnList.get(columnIndex);

            for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                column.set(rowIndex, func.apply(column.get(rowIndex)));
            }
        }
    }

    /**
     * Removes the column identified by the provided column key from the Sheet.
     * <p>
     * Deletes the entire column and shifts all subsequent columns left. The column key is removed from
     * the Sheet and cannot be reused unless re-added. All values in the removed column are lost.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * 
     * sheet.removeColumn("col2");
     * // Sheet now contains only col1 and col3
     * // sheet.containsColumn("col2") returns false
     * }</pre>
     *
     * @param columnKey the key of the column to be removed
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist in the Sheet
     * @see #addColumn(Object, Collection)
     * @see #removeRow(Object)
     */
    public void removeColumn(final C columnKey) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        checkColumnKey(columnKey);

        _columnKeySet.remove(columnKey);

        if (_columnKeyIndexMap != null) {
            final int newColumnLength = columnLength();
            final int removedColumnIndex = _columnKeyIndexMap.remove(columnKey);

            if (removedColumnIndex == newColumnLength) {
                // removed the last column
            } else {
                for (int i = removedColumnIndex; i < newColumnLength; i++) {
                    _columnKeyIndexMap.put(_columnKeyIndexMap.getByValue(i + 1), i);
                }
            }

            if (_isInitialized) {
                _columnList.remove(removedColumnIndex);
            }
        }
    }

    /**
     * Moves the column identified by the provided column key to a new position in the Sheet.
     * <p>
     * Repositions a column to a different index while maintaining all its data. Other columns are
     * shifted accordingly. The column key remains associated with the same column data.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * 
     * // Move col3 to the beginning (index 0)
     * sheet.moveColumn("col3", 0);
     * // Column order is now: ["col3", "col1", "col2"]
     * }</pre>
     *
     * @param columnKey the key of the column to be moved
     * @param newColumnIndex the new zero-based index where the column should be positioned
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist in the Sheet
     * @throws IndexOutOfBoundsException if newColumnIndex < 0 or newColumnIndex >= columnLength()
     * @see #swapColumnPosition(Object, Object)
     */
    public void moveColumn(final C columnKey, final int newColumnIndex) throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        checkFrozen();

        final int columnIndex = this.getColumnIndex(columnKey);
        this.checkColumnIndex(newColumnIndex);

        final List<C> tmp = new ArrayList<>(columnLength());
        tmp.addAll(_columnKeySet);
        tmp.add(newColumnIndex, tmp.remove(columnIndex));

        _columnKeySet.clear();
        _columnKeySet.addAll(tmp);

        _columnKeyIndexMap = null;

        if (_isInitialized && _columnList.size() > 0) {
            _columnList.add(newColumnIndex, _columnList.remove(columnIndex));
        }
    }

    /**
     * <p>Swaps the positions of two columns in the Sheet.</p>
     *
     * <p>This method exchanges the positions of two columns identified by their column keys while maintaining
     * all the data in those columns. The column keys remain attached to their respective column data,
     * but their positions in the sheet's column ordering are exchanged.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a sheet with three columns
     * Sheet&lt;String, String, Integer&gt; sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {1, 2, 3},
     *         {4, 5, 6},
     *         {7, 8, 9}
     *     }
     * );
     *
     * // Swap columns "col1" and "col3"
     * sheet.swapColumnPosition("col1", "col3");
     *
     * // Now the sheet's columns are ordered: col3, col2, col1
     * // with their respective data:
     * // row1: [3, 2, 1]
     * // row2: [6, 5, 4]
     * // row3: [9, 8, 7]
     * </pre>
     *
     * @param columnKeyA the key of the first column to be swapped
     * @param columnKeyB the key of the second column to be swapped
     * @throws IllegalStateException if the Sheet is frozen and can't be modified
     * @throws IllegalArgumentException if either of the column keys doesn't exist in the Sheet
     * @see #moveColumn(Object, int)
     * @see #swapRowPosition(Object, Object)
     */
    public void swapColumnPosition(final C columnKeyA, final C columnKeyB) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndexA = getColumnIndex(columnKeyA);
        final int columnIndexB = getColumnIndex(columnKeyB);

        final List<C> tmp = new ArrayList<>(columnLength());
        tmp.addAll(_columnKeySet);
        final C tmpColumnKeyA = tmp.get(columnIndexA);
        tmp.set(columnIndexA, tmp.get(columnIndexB));
        tmp.set(columnIndexB, tmpColumnKeyA);

        _columnKeySet.clear();
        _columnKeySet.addAll(tmp);

        _columnKeyIndexMap.forcePut(tmp.get(columnIndexA), columnIndexA);
        _columnKeyIndexMap.forcePut(tmp.get(columnIndexB), columnIndexB);

        if (_isInitialized && _columnList.size() > 0) {
            final List<V> tmpColumnA = _columnList.get(columnIndexA);

            _columnList.set(columnIndexA, _columnList.get(columnIndexB));
            _columnList.set(columnIndexB, tmpColumnA);
        }
    }

    /**
     * Renames a column in the Sheet.
     * <p>
     * Changes the key associated with a column while maintaining its position and data.
     * The new key must not already exist in the Sheet.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * // Rename col1 to colA
     * sheet.renameColumn("col1", "colA");
     * // sheet.containsColumn("col1") returns false
     * // sheet.containsColumn("colA") returns true
     * // Data remains unchanged: sheet.get("row1", "colA") returns 1
     * }</pre>
     *
     * @param columnKey the current key of the column to rename
     * @param newColumnKey the new key for the column; must not already exist
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if columnKey doesn't exist or newColumnKey already exists
     * @see #renameRow(Object, Object)
     */
    public void renameColumn(final C columnKey, final C newColumnKey) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        this.checkColumnKey(columnKey);

        if (_columnKeySet.contains(newColumnKey)) {
            throw new IllegalArgumentException("Invalid new column key: " + N.toString(newColumnKey) + ". It's already in the column key set.");
        }

        final int columnIndex = this.getColumnIndex(columnKey);
        final List<C> tmp = new ArrayList<>(_columnKeySet);
        tmp.set(columnIndex, newColumnKey);

        _columnKeySet.clear();
        _columnKeySet.addAll(tmp);

        if (N.notEmpty(_columnKeyIndexMap)) {
            _columnKeyIndexMap.put(newColumnKey, _columnKeyIndexMap.remove(columnKey));
        }
    }

    /**
     * Checks if the Sheet contains a column identified by the specified column key.
     * <p>
     * Tests for the existence of a column with the given key. This method is useful
     * before performing column operations to avoid exceptions.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * boolean exists = sheet.containsColumn("col1"); // true
     * boolean missing = sheet.containsColumn("col3"); // false
     * }</pre>
     *
     * @param columnKey the column key to check
     * @return {@code true} if the column exists, {@code false} otherwise
     * @see #containsRow(Object)
     * @see #contains(Object, Object)
     */
    public boolean containsColumn(final C columnKey) {
        return _columnKeySet.contains(columnKey);
    }

    /**
     * Retrieves a map representing a column in the Sheet.
     * <p>
     * Returns a map where keys are row keys and values are the cell values for the specified column.
     * The map maintains the row order as defined in the Sheet. Values may be {@code null}.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, null}}
     * );
     * 
     * Map<String, Integer> col1Map = sheet.column("col1");
     * // Returns: {"row1"=1, "row2"=3, "row3"=5}
     * 
     * Map<String, Integer> col2Map = sheet.column("col2");
     * // Returns: {"row1"=2, "row2"=4, "row3"=null}
     * }</pre>
     *
     * @param columnKey the key identifying the column
     * @return a map of row keys to cell values for the specified column
     * @throws IllegalArgumentException if the column key does not exist in the Sheet
     * @see #row(Object)
     * @see #columnMap()
     */
    public Map<R, V> column(final C columnKey) throws IllegalArgumentException {
        final int rowLength = rowLength();
        final Map<R, V> columnMap = N.newLinkedHashMap(rowLength);

        if (_isInitialized) {
            final int columnIndex = getColumnIndex(columnKey);
            final List<V> column = _columnList.get(columnIndex);
            int rowIndex = 0;

            for (final R rowKey : this.rowKeySet()) {
                columnMap.put(rowKey, column.get(rowIndex++));
            }
        } else {
            checkColumnKey(columnKey);

            for (final R rowKey : this.rowKeySet()) {
                columnMap.put(rowKey, null);
            }
        }

        return columnMap;
    }

    /**
     * Retrieves a map representing all columns in the Sheet.
     * <p>
     * Returns a nested map structure where the outer map's keys are column keys and values are
     * maps representing each column. The inner maps have row keys as keys and cell values as values.
     * This provides a complete view of the Sheet's data organized by columns.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * Map<String, Map<String, Integer>> allCols = sheet.columnMap();
     * // Returns:
     * // {
     * //   "col1" = {"row1"=1, "row2"=3},
     * //   "col2" = {"row1"=2, "row2"=4}
     * // }
     * }</pre>
     *
     * @return a map of column keys to column maps, where each column map contains row keys to cell values
     * @see #rowMap()
     * @see #column(Object)
     */
    public Map<C, Map<R, V>> columnMap() {
        final Map<C, Map<R, V>> result = N.newLinkedHashMap(this.columnKeySet().size());

        for (final C columnKey : this.columnKeySet()) {
            result.put(columnKey, column(columnKey));
        }

        return result;
    }

    /**
     * Returns the number of rows in the Sheet.
     * <p>
     * The row length represents the total count of row keys in the Sheet,
     * regardless of whether the cells contain values or are null.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     * int rows = sheet.rowLength(); // returns 3
     * }</pre>
     *
     * @return the number of rows in the Sheet
     * @see #columnLength()
     * @see #isEmpty()
     */
    public int rowLength() {
        return _rowKeySet.size();
    }

    /**
     * Returns the number of columns in the Sheet.
     * <p>
     * The column length represents the total count of column keys in the Sheet,
     * regardless of whether the cells contain values or are null.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * int cols = sheet.columnLength(); // returns 3
     * }</pre>
     *
     * @return the number of columns in the Sheet
     * @see #rowLength()
     * @see #isEmpty()
     */
    public int columnLength() {
        return _columnKeySet.size();
    }

    // TODO should the method name be "replaceAll"? If change the method name to replaceAll, what about updateColumn/updateRow?

    /**
     * Updates all values in the Sheet using the provided function.
     * <p>
     * Applies the given function to every cell in the Sheet, replacing each value with
     * the result of the function. The function is called for each cell including those
     * containing {@code null} values.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}}
     * );
     * 
     * // Double all non-null values
     * sheet.updateAll(v -> v == null ? null : v * 2);
     * // Sheet now contains: {{2, 4}, {6, null}}
     * }</pre>
     *
     * @param func the function to apply to each value; receives current value (may be null) and returns new value
     * @throws IllegalStateException if the Sheet is frozen
     * @see #updateAll(IntBiFunction)
     * @see #updateAll(TriFunction)
     * @see #replaceIf(Predicate, Object)
     */
    public void updateAll(final Function<? super V, ? extends V> func) throws IllegalStateException {
        checkFrozen();

        if (rowLength() > 0 && columnLength() > 0) {
            this.init();

            final int rowLength = rowLength();

            for (final List<V> column : _columnList) {
                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    column.set(rowIndex, func.apply(column.get(rowIndex)));
                }
            }
        }
    }

    /**
     * Updates all values in the Sheet using the provided index-based function.
     * <p>
     * Applies the given function to every cell in the Sheet, using the cell's row and column
     * indices as input. This is useful when the new value depends on the cell's position.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{0, 0, 0}, {0, 0, 0}}
     * );
     * 
     * // Set each cell to row index + column index
     * sheet.updateAll((rowIdx, colIdx) -> rowIdx + colIdx);
     * // Sheet now contains: {{0, 1, 2}, {1, 2, 3}}
     * }</pre>
     *
     * @param func the function to apply; receives row and column indices (zero-based) and returns new value
     * @throws IllegalStateException if the Sheet is frozen
     * @see #updateAll(Function)
     * @see #updateAll(TriFunction)
     */
    public void updateAll(final IntBiFunction<? extends V> func) throws IllegalStateException {
        checkFrozen();

        if (rowLength() > 0 && columnLength() > 0) {
            this.init();

            final int rowLength = rowLength();
            int columnIndex = 0;

            for (final List<V> column : _columnList) {
                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    column.set(rowIndex, func.apply(rowIndex, columnIndex));
                }

                columnIndex++;
            }
        }
    }

    /**
     * Updates all values in the Sheet using the provided key-based function.
     * <p>
     * Applies the given function to every cell in the Sheet, using the cell's row key,
     * column key, and current value as input. This provides maximum flexibility for
     * value updates based on both position and current value.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, String> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new String[][] {{"A", "B"}, {"C", "D"}}
     * );
     * 
     * // Combine keys and value
     * sheet.updateAll((r, c, v) -> r + "-" + c + "-" + v);
     * // Sheet now contains:
     * // {{"row1-col1-A", "row1-col2-B"},
     * //  {"row2-col1-C", "row2-col2-D"}}
     * }</pre>
     *
     * @param func the function to apply; receives row key, column key, and current value (may be null), returns new value
     * @throws IllegalStateException if the Sheet is frozen
     * @see #updateAll(Function)
     * @see #updateAll(IntBiFunction)
     */
    public void updateAll(final TriFunction<? super R, ? super C, ? super V, ? extends V> func) throws IllegalStateException {
        checkFrozen();

        if (rowLength() > 0 && columnLength() > 0) {
            this.init();

            final int rowLength = rowLength();
            int columnIndex = 0;
            C columnKey = null;

            for (final List<V> column : _columnList) {
                columnKey = _columnKeyIndexMap.getByValue(columnIndex);

                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    column.set(rowIndex, func.apply(_rowKeyIndexMap.getByValue(rowIndex), columnKey, column.get(rowIndex)));
                }

                columnIndex++;
            }
        }
    }

    /**
     * Replaces all values in the Sheet that satisfy the provided predicate with the new value.
     * <p>
     * Tests each cell value with the predicate and replaces it with the new value if
     * the predicate returns {@code true}. This is useful for bulk replacements based on conditions.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * 
     * // Replace all values greater than 3 with 0
     * sheet.replaceIf(v -> v != null && v > 3, 0);
     * // Sheet now contains: {{1, 2, 3}, {0, 0, 0}}
     * 
     * // Replace all null values with -1
     * sheet.replaceIf(Objects::isNull, -1);
     * }</pre>
     *
     * @param predicate the predicate to test each value; receives current value (may be null)
     * @param newValue the value to replace matching cells with
     * @throws IllegalStateException if the Sheet is frozen
     * @see #replaceIf(IntBiPredicate, Object)
     * @see #replaceIf(TriPredicate, Object)
     * @see #updateAll(Function)
     */
    public void replaceIf(final Predicate<? super V> predicate, final V newValue) throws IllegalStateException {
        checkFrozen();

        if (rowLength() > 0 && columnLength() > 0) {
            this.init();

            final int rowLength = rowLength();

            for (final List<V> column : _columnList) {
                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    if (predicate.test(column.get(rowIndex))) {
                        column.set(rowIndex, newValue);
                    }
                }
            }
        }
    }

    /**
     * Replaces all values in the Sheet that satisfy the provided index-based predicate with the new value.
     * <p>
     * Tests each cell using its row and column indices and replaces it with the new value if
     * the predicate returns {@code true}. Useful for position-based replacements.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     * 
     * // Replace values in the first row with 0
     * sheet.replaceIf((r, c) -> r == 0, 0);
     * // Sheet now contains: {{0, 0}, {3, 4}, {5, 6}}
     * 
     * // Replace diagonal values with -1
     * sheet.replaceIf((r, c) -> r == c, -1);
     * }</pre>
     *
     * @param predicate the predicate to test; receives row and column indices (zero-based)
     * @param newValue the value to replace matching cells with
     * @throws IllegalStateException if the Sheet is frozen
     * @see #replaceIf(Predicate, Object)
     * @see #replaceIf(TriPredicate, Object)
     */
    public void replaceIf(final IntBiPredicate predicate, final V newValue) throws IllegalStateException {
        checkFrozen();

        if (rowLength() > 0 && columnLength() > 0) {
            this.init();

            final int rowLength = rowLength();
            int columnIndex = 0;

            for (final List<V> column : _columnList) {
                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    if (predicate.test(rowIndex, columnIndex)) {
                        column.set(rowIndex, newValue);
                    }
                }

                columnIndex++;
            }
        }
    }

    /**
     * Replaces all values in the Sheet that satisfy the provided key-based predicate with the new value.
     * <p>
     * Tests each cell using its row key, column key, and current value, replacing it with the new value if
     * the predicate returns {@code true}. This provides maximum flexibility for conditional replacements.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * // Replace values in col1 that are odd
     * sheet.replaceIf((r, c, v) -> "col1".equals(c) && v != null && v % 2 == 1, 0);
     * // Sheet now contains: {{0, 2}, {0, 4}}
     * }</pre>
     *
     * @param predicate the predicate to test; receives row key, column key, and current value (may be null)
     * @param newValue the value to replace matching cells with
     * @throws IllegalStateException if the Sheet is frozen
     * @see #replaceIf(Predicate, Object)
     * @see #replaceIf(IntBiPredicate, Object)
     */
    public void replaceIf(final TriPredicate<? super R, ? super C, ? super V> predicate, final V newValue) throws IllegalStateException {
        checkFrozen();

        if (rowLength() > 0 && columnLength() > 0) {
            this.init();

            final int rowLength = rowLength();
            int columnIndex = 0;
            R rowKey = null;
            C columnKey = null;
            V val = null;

            for (final List<V> column : _columnList) {
                columnKey = _columnKeyIndexMap.getByValue(columnIndex);

                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    rowKey = _rowKeyIndexMap.getByValue(rowIndex);
                    val = column.get(rowIndex);

                    if (predicate.test(rowKey, columnKey, val)) {
                        column.set(rowIndex, newValue);
                    }
                }

                columnIndex++;
            }
        }
    }

    /**
     * Sorts the rows in the Sheet based on the natural ordering of the row keys.
     * <p>
     * Reorders the rows according to the natural ordering of their keys, as implemented by
     * the {@code Comparable} interface. All row data moves with their respective keys.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("charlie", "alice", "bob"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{3, 4}, {1, 2}, {5, 6}}
     * );
     * 
     * sheet.sortByRowKey();
     * // Rows are now ordered: ["alice", "bob", "charlie"]
     * // With data: {{1, 2}, {5, 6}, {3, 4}}
     * }</pre>
     *
     * @throws ClassCastException if the row keys don't implement {@code Comparable}
     * @throws IllegalStateException if the Sheet is frozen
     * @see #sortByRowKey(Comparator)
     * @see #sortByColumnKey()
     */
    public void sortByRowKey() throws IllegalStateException {
        sortByRowKey((Comparator<R>) Comparator.naturalOrder());
    }

    /**
     * Sorts the rows in the Sheet based on the row keys using the provided comparator.
     * <p>
     * Reorders the rows according to the specified comparator applied to their keys.
     * All row data moves with their respective keys to maintain data integrity.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("long_name", "a", "medium"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     * 
     * // Sort by string length
     * sheet.sortByRowKey(Comparator.comparing(String::length));
     * // Rows are now ordered: ["a", "medium", "long_name"]
     * }</pre>
     *
     * @param cmp the comparator to determine row key ordering; must not be null
     * @throws IllegalStateException if the Sheet is frozen
     * @see #sortByRowKey()
     * @see #sortByColumnKey(Comparator)
     */
    public void sortByRowKey(final Comparator<? super R> cmp) throws IllegalStateException {
        checkFrozen();

        final int rowLength = rowLength();
        final Indexed<R>[] arrayOfPair = new Indexed[rowLength];
        final Iterator<R> iter = _rowKeySet.iterator();

        for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
            arrayOfPair[rowIndex] = Indexed.of(iter.next(), rowIndex);
        }

        final Comparator<Indexed<R>> pairCmp = createComparatorForIndexedObject(cmp);

        N.sort(arrayOfPair, pairCmp);

        if (_isInitialized) {
            final int columnCount = _columnKeySet.size();
            final Set<Integer> ordered = N.newHashSet(rowLength);
            final V[] tempRow = (V[]) new Object[columnCount];
            List<V> tmpColumn = null;

            for (int i = 0, index = 0; i < rowLength; i++) {
                index = arrayOfPair[i].index();

                if ((index != i) && !ordered.contains(i)) {
                    for (int j = 0; j < columnCount; j++) {
                        tempRow[j] = _columnList.get(j).get(i);
                    }

                    int previous = i;
                    int next = index;

                    do {
                        for (int j = 0; j < columnCount; j++) {
                            tmpColumn = _columnList.get(j);
                            tmpColumn.set(previous, tmpColumn.get(next));
                        }

                        ordered.add(next);

                        previous = next;
                        next = arrayOfPair[next].index();
                    } while (next != i);

                    for (int j = 0; j < columnCount; j++) {
                        _columnList.get(j).set(previous, tempRow[j]);
                    }

                    ordered.add(i);
                }
            }
        }

        final boolean indexedMapInitialized = N.notEmpty(_rowKeyIndexMap);
        _rowKeySet.clear();

        for (int i = 0; i < rowLength; i++) {
            _rowKeySet.add(arrayOfPair[i].value());

            if (indexedMapInitialized) {
                _rowKeyIndexMap.forcePut(arrayOfPair[i].value(), i);
            }
        }
    }

    /**
     * Sorts the columns in the Sheet based on the values in the specified row.
     * <p>
     * Reorders the columns according to the values in the specified row using the provided comparator.
     * This effectively sorts the "vertical" arrangement of data based on a "horizontal" slice.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {3, 1, 2},  // Values to sort by
     *         {6, 4, 5}
     *     }
     * );
     * 
     * // Sort columns by values in row1 (ascending)
     * sheet.sortByRow("row1", Integer::compareTo);
     * // Columns are now ordered: ["col2", "col3", "col1"]
     * // Data becomes: {{1, 2, 3}, {4, 5, 6}}
     * }</pre>
     *
     * @param rowKey the key of the row whose values will determine the column ordering
     * @param cmp the comparator to apply to values in the specified row
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key doesn't exist
     * @see #sortByColumn(Object, Comparator)
     * @see #sortByRows(Collection, Comparator)
     */
    public void sortByRow(final R rowKey, final Comparator<? super V> cmp) throws IllegalStateException {
        checkFrozen();

        if (!_isInitialized) {
            return;
        }

        final int rowIndex = getRowIndex(rowKey);
        final int columnLength = columnLength();
        final Indexed<V>[] arrayOfPair = new Indexed[columnLength];

        for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
            arrayOfPair[columnIndex] = Indexed.of(_columnList.get(columnIndex).get(rowIndex), columnIndex);
        }

        if (N.allMatch(arrayOfPair, it -> it.value() == null)) { // All null values in the specified row.
            return;
        }

        final Comparator<Indexed<V>> pairCmp = createComparatorForIndexedObject(cmp);

        N.sort(arrayOfPair, pairCmp);

        final Set<Integer> ordered = N.newHashSet(columnLength);
        List<V> tempColumn = null;

        for (int i = 0, index = 0; i < columnLength; i++) {
            index = arrayOfPair[i].index();

            if ((index != i) && !ordered.contains(i)) {
                tempColumn = _columnList.get(i);

                int previous = i;
                int next = index;

                do {
                    _columnList.set(previous, _columnList.get(next));

                    ordered.add(next);

                    previous = next;
                    next = arrayOfPair[next].index();
                } while (next != i);

                _columnList.set(previous, tempColumn);

                ordered.add(i);
            }
        }

        final boolean indexedMapInitialized = N.notEmpty(_columnKeyIndexMap);
        final Object[] columnKeys = _columnKeySet.toArray(new Object[columnLength]);
        C columnKey = null;
        _columnKeySet.clear();

        for (int i = 0; i < columnLength; i++) {
            columnKey = (C) columnKeys[arrayOfPair[i].index()];
            _columnKeySet.add(columnKey);

            if (indexedMapInitialized) {
                _columnKeyIndexMap.forcePut(columnKey, i);
            }
        }
    }

    //    /**
    //     *
    //     *
    //     * @param rowKeysToSort
    //     * @param cmp
    //     * @deprecated Use {@link #sortByRows(Collection<R>,Comparator<? super Object[]>)} instead
    //     */
    //    public void sortByRow(Collection<R> rowKeysToSort, Comparator<? super Object[]> cmp) {
    //        sortByRows(rowKeysToSort, cmp);
    //    }

    /**
     * Sorts the columns in the Sheet based on the values in the specified rows.
     * <p>
     * Reorders the columns according to the combined values from multiple rows using the provided comparator.
     * Each column is represented as an array of values from the specified rows for comparison.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("priority", "secondary", "data"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {2, 1, 1},     // priority row
     *         {5, 3, 4},     // secondary row  
     *         {10, 20, 30}   // data row
     *     }
     * );
     * 
     * // Sort columns by priority first, then secondary
     * sheet.sortByRows(List.of("priority", "secondary"), 
     *     (a, b) -> {
     *         int result = Integer.compare((Integer)a[0], (Integer)b[0]);
     *         return result != 0 ? result : Integer.compare((Integer)a[1], (Integer)b[1]);
     *     });
     * }</pre>
     *
     * @param rowKeysToSort the keys of rows whose values will determine column ordering
     * @param cmp the comparator applied to arrays of values from the specified rows
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if any row key doesn't exist
     * @see #sortByRow(Object, Comparator)
     * @see #sortByColumns(Collection, Comparator)
     */
    public void sortByRows(final Collection<R> rowKeysToSort, final Comparator<? super Object[]> cmp) throws IllegalStateException {
        checkFrozen();

        if (!_isInitialized) {
            return;
        }

        final int sortRowSize = rowKeysToSort.size();
        final int[] rowIndexes = new int[sortRowSize];
        int idx = 0;

        for (final R rowKey : rowKeysToSort) {
            rowIndexes[idx++] = getRowIndex(rowKey);
        }

        final int columnLength = columnLength();
        final Indexed<Object[]>[] arrayOfPair = new Indexed[columnLength];

        for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
            final Object[] values = new Object[sortRowSize];

            for (int i = 0; i < sortRowSize; i++) {
                values[i] = _columnList.get(columnIndex).get(rowIndexes[i]);
            }

            arrayOfPair[columnIndex] = Indexed.of(values, columnIndex);
        }

        if (N.allMatch(arrayOfPair, it -> N.allNull(it.value()))) { // All null values in the specified row.
            return;
        }

        final Comparator<Indexed<Object[]>> pairCmp = createComparatorForIndexedObject(cmp);

        N.sort(arrayOfPair, pairCmp);

        final Set<Integer> ordered = N.newHashSet(columnLength);
        List<V> tempColumn = null;

        for (int i = 0, index = 0; i < columnLength; i++) {
            index = arrayOfPair[i].index();

            if ((index != i) && !ordered.contains(i)) {
                tempColumn = _columnList.get(i);

                int previous = i;
                int next = index;

                do {
                    _columnList.set(previous, _columnList.get(next));

                    ordered.add(next);

                    previous = next;
                    next = arrayOfPair[next].index();
                } while (next != i);

                _columnList.set(previous, tempColumn);

                ordered.add(i);
            }
        }

        final boolean indexedMapInitialized = N.notEmpty(_columnKeyIndexMap);
        final Object[] columnKeys = _columnKeySet.toArray(new Object[columnLength]);
        C columnKey = null;
        _columnKeySet.clear();

        for (int i = 0; i < columnLength; i++) {
            columnKey = (C) columnKeys[arrayOfPair[i].index()];
            _columnKeySet.add(columnKey);

            if (indexedMapInitialized) {
                _columnKeyIndexMap.forcePut(columnKey, i);
            }
        }
    }

    /**
     * Sorts the columns in the sheet based on the natural ordering of the column keys.
     * The natural ordering is the ordering imposed by the objects' own compareTo method.
     *
     * @throws ClassCastException if the column keys' class does not implement Comparable, or if comparing two column keys throws a ClassCastException.
     * @throws IllegalStateException if the sheet is frozen (read-only).
     */
    public void sortByColumnKey() throws IllegalStateException {
        sortByColumnKey((Comparator<C>) Comparator.naturalOrder());
    }

    /**
     * Sorts the columns in the sheet based on the column keys using the provided comparator.
     * The comparator takes two column keys as input and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     *
     * @param cmp The comparator to determine the order of the column keys. It takes two column keys as input and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     * @throws IllegalStateException if the sheet is frozen (read-only).
     */
    public void sortByColumnKey(final Comparator<? super C> cmp) throws IllegalStateException {
        checkFrozen();

        final int columnLength = _columnKeySet.size();
        final Indexed<C>[] arrayOfPair = new Indexed[columnLength];
        final Iterator<C> iter = _columnKeySet.iterator();

        for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
            arrayOfPair[columnIndex] = Indexed.of(iter.next(), columnIndex);
        }

        final Comparator<Indexed<C>> pairCmp = createComparatorForIndexedObject(cmp);

        N.sort(arrayOfPair, pairCmp);

        if (_isInitialized) {
            final Set<Integer> ordered = N.newHashSet(columnLength);
            List<V> tempColumn = null;

            for (int i = 0, index = 0; i < columnLength; i++) {
                index = arrayOfPair[i].index();

                if ((index != i) && !ordered.contains(i)) {
                    tempColumn = _columnList.get(i);

                    int previous = i;
                    int next = index;

                    do {
                        _columnList.set(previous, _columnList.get(next));

                        ordered.add(next);

                        previous = next;
                        next = arrayOfPair[next].index();
                    } while (next != i);

                    _columnList.set(previous, tempColumn);

                    ordered.add(i);
                }
            }
        }

        final boolean indexedMapInitialized = N.notEmpty(_columnKeyIndexMap);
        _columnKeySet.clear();

        for (int i = 0; i < columnLength; i++) {
            _columnKeySet.add(arrayOfPair[i].value());

            if (indexedMapInitialized) {
                _columnKeyIndexMap.forcePut(arrayOfPair[i].value(), i);
            }
        }
    }

    /**
     * Sorts the columns in the sheet based on the values in the specified column using the provided comparator.
     * The comparator takes two values from the column as input and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     *
     * @param columnKey The key of the column based on whose values the columns will be sorted.
     * @param cmp The comparator to determine the order of the values in the specified column. It takes two values from the column as input and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     * @throws IllegalStateException if the sheet is frozen (read-only).
     */
    public void sortByColumn(final C columnKey, final Comparator<? super V> cmp) throws IllegalStateException {
        checkFrozen();

        if (!_isInitialized) {
            return;
        }

        final int columnIndex = getColumnIndex(columnKey);
        final int rowLength = rowLength();
        final Indexed<V>[] arrayOfPair = new Indexed[rowLength];
        final List<V> column = _columnList.get(columnIndex);

        for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
            arrayOfPair[rowIndex] = Indexed.of(column.get(rowIndex), rowIndex);
        }

        if (N.allMatch(arrayOfPair, it -> it.value() == null)) { // All null values in the specified row.
            return;
        }

        final Comparator<Indexed<V>> pairCmp = createComparatorForIndexedObject(cmp);

        N.sort(arrayOfPair, pairCmp);

        final int columnCount = _columnKeySet.size();
        final Set<Integer> ordered = N.newHashSet(rowLength);
        final V[] tempRow = (V[]) new Object[columnCount];
        List<V> tmpColumn = null;

        for (int i = 0, index = 0; i < rowLength; i++) {
            index = arrayOfPair[i].index();

            if ((index != i) && !ordered.contains(i)) {
                for (int j = 0; j < columnCount; j++) {
                    tempRow[j] = _columnList.get(j).get(i);
                }

                int previous = i;
                int next = index;

                do {
                    for (int j = 0; j < columnCount; j++) {
                        tmpColumn = _columnList.get(j);
                        tmpColumn.set(previous, tmpColumn.get(next));
                    }

                    ordered.add(next);

                    previous = next;
                    next = arrayOfPair[next].index();
                } while (next != i);

                for (int j = 0; j < columnCount; j++) {
                    _columnList.get(j).set(previous, tempRow[j]);
                }

                ordered.add(i);
            }
        }

        final boolean indexedMapInitialized = N.notEmpty(_rowKeyIndexMap);
        final Object[] rowKeys = _rowKeySet.toArray(new Object[rowLength]);
        R rowKey = null;
        _rowKeySet.clear();

        for (int i = 0; i < rowLength; i++) {
            rowKey = (R) rowKeys[arrayOfPair[i].index()];
            _rowKeySet.add(rowKey);

            if (indexedMapInitialized) {
                _rowKeyIndexMap.forcePut(rowKey, i);
            }
        }
    }

    //    /**
    //     *
    //     *
    //     * @param columnKeysToSort
    //     * @param cmp
    //     * @deprecated Use {@link #sortByColumns(Collection<C>,Comparator<? super Object[]>)} instead
    //     */
    //    public void sortByColumn(Collection<C> columnKeysToSort, Comparator<? super Object[]> cmp) {
    //        sortByColumns(columnKeysToSort, cmp);
    //    }

    /**
     * Sorts the columns in the sheet based on the values in the specified columns using the provided comparator.
     * The comparator takes two arrays of objects as input, each array representing a column in the sheet, and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     *
     * @param columnKeysToSort The keys of the columns based on whose values the columns will be sorted.
     * @param cmp The comparator to determine the order of the columns. It takes two arrays of objects, each representing a column in the sheet, as input and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     * @throws IllegalStateException if the sheet is frozen (read-only).
     */
    public void sortByColumns(final Collection<C> columnKeysToSort, final Comparator<? super Object[]> cmp) throws IllegalStateException {
        checkFrozen();

        if (!_isInitialized) {
            return;
        }

        final int sortColumnSize = columnKeysToSort.size();
        final int[] columnIndexes = new int[sortColumnSize];
        int idx = 0;

        for (final C columnKey : columnKeysToSort) {
            columnIndexes[idx++] = getColumnIndex(columnKey);
        }

        final int rowLength = rowLength();
        final Indexed<Object[]>[] arrayOfPair = new Indexed[rowLength];

        for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
            final Object[] values = new Object[sortColumnSize];

            for (int i = 0; i < sortColumnSize; i++) {
                values[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            arrayOfPair[rowIndex] = Indexed.of(values, rowIndex);
        }

        if (N.allMatch(arrayOfPair, it -> N.allNull(it.value()))) { // All null values in the specified row.
            return;
        }

        final Comparator<Indexed<Object[]>> pairCmp = createComparatorForIndexedObject(cmp);

        N.sort(arrayOfPair, pairCmp);

        final int columnCount = _columnKeySet.size();
        final Set<Integer> ordered = N.newHashSet(rowLength);
        final V[] tempRow = (V[]) new Object[columnCount];
        List<V> tmpColumn = null;

        for (int i = 0, index = 0; i < rowLength; i++) {
            index = arrayOfPair[i].index();

            if ((index != i) && !ordered.contains(i)) {
                for (int j = 0; j < columnCount; j++) {
                    tempRow[j] = _columnList.get(j).get(i);
                }

                int previous = i;
                int next = index;

                do {
                    for (int j = 0; j < columnCount; j++) {
                        tmpColumn = _columnList.get(j);
                        tmpColumn.set(previous, tmpColumn.get(next));
                    }

                    ordered.add(next);

                    previous = next;
                    next = arrayOfPair[next].index();
                } while (next != i);

                for (int j = 0; j < columnCount; j++) {
                    _columnList.get(j).set(previous, tempRow[j]);
                }

                ordered.add(i);
            }
        }

        final boolean indexedMapInitialized = N.notEmpty(_rowKeyIndexMap);
        final Object[] rowKeys = _rowKeySet.toArray(new Object[rowLength]);
        R rowKey = null;
        _rowKeySet.clear();

        for (int i = 0; i < rowLength; i++) {
            rowKey = (R) rowKeys[arrayOfPair[i].index()];
            _rowKeySet.add(rowKey);

            if (indexedMapInitialized) {
                _rowKeyIndexMap.forcePut(rowKey, i);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private <T> Comparator<Indexed<T>> createComparatorForIndexedObject(final Comparator<? super T> cmp) {
        Comparator<Indexed<T>> pairCmp = null;

        if (cmp != null) {
            pairCmp = (a, b) -> cmp.compare(a.value(), b.value());
        } else {
            final Comparator<Indexed<Comparable>> tmp = (a, b) -> N.compare(a.value(), b.value());
            pairCmp = (Comparator) tmp;
        }

        return pairCmp;
    }

    /**
     * Creates a copy of the current Sheet object.
     * This method creates a new Sheet object and initializes it with the same row keys, column keys, and values as the current Sheet.
     * Changes to the copy will not affect the current Sheet, and vice versa.
     *
     * @return A new Sheet object that is a copy of the current Sheet.
     */
    public Sheet<R, C, V> copy() {
        final Sheet<R, C, V> copy = new Sheet<>(_rowKeySet, _columnKeySet);

        if (_isInitialized) {
            copy.initIndexMap();

            copy._columnList = new ArrayList<>(_columnList.size());

            for (final List<V> column : _columnList) {
                copy._columnList.add(new ArrayList<>(column));
            }

            copy._isInitialized = true;
        }

        return copy;
    }

    /**
     * Creates a copy of the current Sheet object with the specified row keys and column keys.
     * This method creates a new Sheet object and initializes it with the same values as the current Sheet for the specified row keys and column keys.
     * Changes to the copy will not affect the current Sheet, and vice versa.
     *
     * @param rowKeySet The collection of row keys to be included in the copied Sheet.
     * @param columnKeySet The collection of column keys to be included in the copied Sheet.
     * @return A new Sheet object that is a copy of the current Sheet with the specified row keys and column keys.
     * @throws IllegalArgumentException if any of the specified row keys or column keys are not present in the current Sheet.
     */
    public Sheet<R, C, V> copy(final Collection<R> rowKeySet, final Collection<C> columnKeySet) {
        if (!_rowKeySet.containsAll(rowKeySet)) {
            throw new IllegalArgumentException("Row keys: " + N.difference(rowKeySet, _rowKeySet) + " are not included in this sheet row keys: " + _rowKeySet);
        }

        if (!_columnKeySet.containsAll(columnKeySet)) {
            throw new IllegalArgumentException(
                    "Column keys: " + N.difference(columnKeySet, _columnKeySet) + " are not included in this sheet Column keys: " + _columnKeySet);
        }

        final Sheet<R, C, V> copy = new Sheet<>(rowKeySet, columnKeySet);

        if (_isInitialized) {
            copy.initIndexMap();

            copy._columnList = new ArrayList<>(copy.columnLength());

            final int[] rowKeyIndices = new int[copy.rowLength()];
            int idx = 0;

            for (final R rowKey : copy._rowKeySet) {
                rowKeyIndices[idx++] = this.getRowIndex(rowKey);
            }

            for (final C columnKey : copy._columnKeySet) {
                final List<V> column = _columnList.get(this.getColumnIndex(columnKey));
                final List<V> newColumn = new ArrayList<>(rowKeyIndices.length);

                for (final int rowIndex : rowKeyIndices) {
                    newColumn.add(column.get(rowIndex));
                }

                copy._columnList.add(newColumn);
            }

            copy._isInitialized = true;
        }

        return copy;
    }

    /**
     * Creates a deep copy of the current Sheet object by Serialization/Deserialization.
     * This method creates a new Sheet object and initializes it with the same row keys, column keys, and values as the current Sheet.
     * Changes to the copy will not affect the current Sheet, and vice versa.
     * The copy will maintain the same frozen state as the current Sheet.
     *
     * @return A new Sheet object that is a deep copy of the current Sheet.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Beta
    @Override
    public Sheet<R, C, V> clone() { //NOSONAR
        return clone(_isFrozen);
    }

    /**
     * Creates a deep copy of the current Sheet object by Serialization/Deserialization.
     * This method creates a new Sheet object and initializes it with the same row keys, column keys, and values as the current Sheet.
     * Changes to the copy will not affect the current Sheet, and vice versa.
     *
     * @param freeze A boolean value that determines whether the copied Sheet should be frozen (read-only).
     * @return A new Sheet object that is a deep copy of the current Sheet.
     */
    public Sheet<R, C, V> clone(final boolean freeze) {
        if (kryoParser == null) {
            throw new UnsupportedOperationException("Kryo library is required for deep cloning. Please add Kryo to your classpath or use copy() instead.");
        }

        final Sheet<R, C, V> copy = kryoParser.clone(this);

        copy._isFrozen = freeze;

        return copy;
    }

    /**
     * Merges this Sheet with another Sheet using a merge function.
     * <p>
     * Creates a new Sheet containing the union of all row and column keys from both Sheets.
     * For each cell position, applies the merge function with values from both Sheets.
     * Missing values are represented as {@code null} in the merge function.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet1 = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * sheet1.println();
     * 
     * #        +------+------+
     * #        | col1 | col2 |
     * # +------+------+------+
     * # | row1 | 1    | 2    |
     * # | row2 | 3    | 4    |
     * # +------+------+------+
     * 
     * Sheet<String, String, Integer> sheet2 = Sheet.rows(
     *     List.of("row2", "row3"),
     *     List.of("col2", "col3"),
     *     new Integer[][] {{10, 20}, {30, 40}}
     * );
     * sheet2.println();
     * 
     * #        +------+------+
     * #        | col2 | col3 |
     * # +------+------+------+
     * # | row2 | 10   | 20   |
     * # | row3 | 30   | 40   |
     * # +------+------+------+
     * 
     * // Add values where both exist, use non-null value otherwise
     * Sheet<String, String, String> merged = sheet1.merge(sheet2, (a, b) -> a + "#" + b);
     * merged.println();
     * 
     * #        +-----------+---------+-----------+
     * #        | col1      | col2    | col3      |
     * # +------+-----------+---------+-----------+
     * # | row1 | 1#null    | 2#null  | null#null |
     * # | row2 | 3#null    | 4#10    | null#20   |
     * # | row3 | null#null | null#30 | null#40   |
     * # +------+-----------+---------+-----------+
     * }</pre>
     *
     * @param <U> the type of values in the other Sheet
     * @param <X> the type of values in the resulting merged Sheet
     * @param b the other Sheet to merge with this one
     * @param mergeFunction function to combine values; receives value from this Sheet and other Sheet (either may be null)
     * @return a new Sheet containing the merged result
     * @see #putAll(Sheet, BiFunction)
     */
    public <U, X> Sheet<R, C, X> merge(final Sheet<? extends R, ? extends C, ? extends U> b,
            final BiFunction<? super V, ? super U, ? extends X> mergeFunction) {
        final Sheet<R, C, U> sheetB = (Sheet<R, C, U>) b;

        final Set<R> newRowKeySet = N.newLinkedHashSet(this.rowKeySet());
        newRowKeySet.addAll(sheetB.rowKeySet());

        final Set<C> newColumnKeySet = N.newLinkedHashSet(this.columnKeySet());
        newColumnKeySet.addAll(sheetB.columnKeySet());

        final Sheet<R, C, X> result = new Sheet<>(newRowKeySet, newColumnKeySet);
        final int[] rowIndexes1 = new int[newRowKeySet.size()], rowIndexes2 = new int[newRowKeySet.size()];
        final int[] columnIndexes1 = new int[newColumnKeySet.size()], columnIndexes2 = new int[newColumnKeySet.size()];

        int idx = 0;
        for (final R rowKey : newRowKeySet) {
            rowIndexes1[idx] = this.containsRow(rowKey) ? this.getRowIndex(rowKey) : -1;
            rowIndexes2[idx] = sheetB.containsRow(rowKey) ? sheetB.getRowIndex(rowKey) : -1;
            idx++;
        }

        idx = 0;

        for (final C columnKey : newColumnKeySet) {
            columnIndexes1[idx] = this.containsColumn(columnKey) ? this.getColumnIndex(columnKey) : -1;
            columnIndexes2[idx] = sheetB.containsColumn(columnKey) ? sheetB.getColumnIndex(columnKey) : -1;
            idx++;
        }

        V e1 = null;
        U e2 = null;

        for (int rowIndex = 0, rowLen = newRowKeySet.size(); rowIndex < rowLen; rowIndex++) {
            for (int columnIndex = 0, columnLen = newColumnKeySet.size(); columnIndex < columnLen; columnIndex++) {
                e1 = rowIndexes1[rowIndex] > -1 && columnIndexes1[columnIndex] > -1 ? this.get(rowIndexes1[rowIndex], columnIndexes1[columnIndex]) : null;
                e2 = rowIndexes2[rowIndex] > -1 && columnIndexes2[columnIndex] > -1 ? sheetB.get(rowIndexes2[rowIndex], columnIndexes2[columnIndex]) : null;
                result.put(rowIndex, columnIndex, mergeFunction.apply(e1, e2));
            }
        }

        return result;
    }

    /**
     * Creates a transposed copy of this Sheet.
     * <p>
     * Returns a new Sheet where rows become columns and columns become rows.
     * Row keys become column keys and vice versa. The data is reorganized accordingly.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> original = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * 
     * //        +------+------+------+
     * //        | col1 | col2 | col3 |
     * // +------+------+------+------+
     * // | row1 | 1    | 2    | 3    |
     * // | row2 | 4    | 5    | 6    |
     * // +------+------+------+------+
     * 
     * Sheet<String, String, Integer> transposed = original.transpose();
     * //        +------+------+
     * //        | row1 | row2 |
     * // +------+------+------+
     * // | col1 | 1    | 4    |
     * // | col2 | 2    | 5    |
     * // | col3 | 3    | 6    |
     * // +------+------+------+
     * }</pre>
     *
     * @return a new transposed Sheet where row and column keys are swapped
     * @see #copy()
     */
    public Sheet<C, R, V> transpose() {
        final Sheet<C, R, V> copy = new Sheet<>(_columnKeySet, _rowKeySet);

        if (_isInitialized) {
            copy.initIndexMap();

            final int rowLength = copy.rowLength();
            final int columnLength = copy.columnLength();

            copy._columnList = new ArrayList<>(columnLength);

            for (int i = 0; i < columnLength; i++) {
                final List<V> column = new ArrayList<>(rowLength);

                for (int j = 0; j < rowLength; j++) {
                    column.add(_columnList.get(j).get(i));
                }

                copy._columnList.add(column);
            }

            copy._isInitialized = true;
        }

        return copy;
    }

    /**
     * Makes this Sheet immutable by freezing it.
     * <p>
     * Once frozen, all modification operations will throw {@code IllegalStateException}.
     * This includes adding/removing rows/columns, changing values, sorting, etc.
     * The frozen state cannot be reversed.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1"),
     *     new Integer[][] {{1}}
     * );
     * 
     * sheet.freeze();
     * // sheet.put("row1", "col1", 2); // throws IllegalStateException
     * }</pre>
     *
     * @see #isFrozen()
     * @see #clone(boolean)
     */
    public void freeze() {
        _isFrozen = true;
    }

    /**
     * Checks if this Sheet is frozen (immutable).
     * <p>
     * A frozen Sheet cannot be modified and will throw {@code IllegalStateException}
     * for any modification attempts.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.empty();
     * boolean frozen1 = sheet.isFrozen(); // true (empty sheet is frozen)
     * 
     * Sheet<String, String, Integer> mutable = new Sheet<>(List.of("r"), List.of("c"));
     * boolean frozen2 = mutable.isFrozen(); // false
     * }</pre>
     *
     * @return {@code true} if the Sheet is frozen, {@code false} otherwise
     * @see #freeze()
     */
    public boolean isFrozen() {
        return _isFrozen;
    }

    /**
     * Clears all values in the Sheet, setting them to {@code null}.
     * <p>
     * Removes all values from cells while preserving the Sheet structure (row and column keys).
     * The Sheet dimensions remain unchanged.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * sheet.clear();
     * // All values are now null, but structure is preserved
     * Integer val = sheet.get("row1", "col1"); // returns null
     * }</pre>
     *
     * @throws IllegalStateException if the Sheet is frozen
     * @see #isEmpty()
     */
    public void clear() throws IllegalStateException {
        checkFrozen();

        if (_isInitialized && _columnList.size() > 0) {
            for (final List<V> column : _columnList) {
                // column.clear();
                N.fill(column, 0, column.size(), null);
            }
        }
    }

    /**
     * Optimizes the memory usage by trimming internal storage capacity.
     * <p>
     * Reduces the capacity of internal lists to match their current size,
     * potentially freeing unused memory. This is useful after removing many rows.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>(rowKeys, colKeys);
     * // ... add and remove many rows ...
     * sheet.trimToSize(); // optimize memory usage
     * }</pre>
     *
     * @see #clear()
     */
    public void trimToSize() {
        if (_isInitialized && _columnList.size() > 0) {
            for (final List<V> column : _columnList) {
                if (column instanceof ArrayList) {
                    ((ArrayList<?>) column).trimToSize();
                }
            }
        }
    }

    /**
     * Counts the number of non-null values in the Sheet.
     * <p>
     * Iterates through all cells and counts those containing non-null values.
     * Empty or uninitialized cells are counted as null.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, null, 3}, {4, 5, null}}
     * );
     * 
     * long count = sheet.countOfNonNullValue(); // returns 4
     * }</pre>
     *
     * @return the number of non-null values in the Sheet
     * @see #isEmpty()
     */
    public long countOfNonNullValue() {
        if (_isInitialized) {
            long count = 0;

            for (final List<V> col : _columnList) {
                for (final V e : col) {
                    if (e != null) {
                        count++;
                    }
                }
            }

            return count;
        } else {
            return 0;
        }
    }

    /**
     * Checks if the Sheet has no rows or no columns.
     * <p>
     * A Sheet is considered empty if it has zero rows or zero columns.
     * An empty Sheet cannot contain any data.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> empty1 = new Sheet<>(List.of(), List.of("col1"));
     * boolean isEmpty1 = empty1.isEmpty(); // true (no rows)
     * 
     * Sheet<String, String, Integer> empty2 = new Sheet<>(List.of("row1"), List.of());
     * boolean isEmpty2 = empty2.isEmpty(); // true (no columns)
     * 
     * Sheet<String, String, Integer> notEmpty = new Sheet<>(List.of("row1"), List.of("col1"));
     * boolean isEmpty3 = notEmpty.isEmpty(); // false
     * }</pre>
     *
     * @return {@code true} if the Sheet has no rows or no columns, {@code false} otherwise
     * @see #rowLength()
     * @see #columnLength()
     */
    public boolean isEmpty() {
        return _rowKeySet.isEmpty() || _columnKeySet.isEmpty();
    }

    // This should not be a public method. It's an implementation detail.
    //    /**
    //     * Checks if the Sheet has been initialized.
    //     * A Sheet is considered initialized if it has been populated with data.
    //     *
    //     * @return {@code true} if the Sheet is initialized, {@code false} otherwise.
    //     */
    //    public boolean isInitialized() {
    //        return _isInitialized;
    //    }

    /**
     * Performs the given action for each cell in the Sheet in horizontal order (row by row).
     * <p>
     * Iterates through cells row by row, calling the action for each cell including null values.
     * The action receives the row key, column key, and value for each cell.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * sheet.forEachH((r, c, v) -> System.out.println(r + "," + c + "=" + v));
     * // Prints: row1,col1=1  row1,col2=2  row2,col1=3  row2,col2=4
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each cell; receives row key, column key, and value
     * @throws E if the action throws an exception
     * @see #forEachV(Throwables.TriConsumer)
     * @see #forEachNonNullH(Throwables.TriConsumer)
     */
    public <E extends Exception> void forEachH(final Throwables.TriConsumer<? super R, ? super C, ? super V, E> action) throws E {
        if (_isInitialized) {
            for (final R rowKey : _rowKeySet) {
                for (final C columnKey : _columnKeySet) {
                    action.accept(rowKey, columnKey, get(rowKey, columnKey));
                }
            }
        } else {
            for (final R rowKey : _rowKeySet) {
                for (final C columnKey : _columnKeySet) {
                    action.accept(rowKey, columnKey, null);
                }
            }
        }
    }

    /**
     * Performs the given action for each cell in the Sheet in vertical order (column by column).
     * <p>
     * Iterates through cells column by column, calling the action for each cell including null values.
     * The action receives the row key, column key, and value for each cell.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * sheet.forEachV((r, c, v) -> System.out.println(r + "," + c + "=" + v));
     * // Prints: row1,col1=1  row2,col1=3  row1,col2=2  row2,col2=4
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each cell; receives row key, column key, and value
     * @throws E if the action throws an exception
     * @see #forEachH(Throwables.TriConsumer)
     * @see #forEachNonNullV(Throwables.TriConsumer)
     */
    public <E extends Exception> void forEachV(final Throwables.TriConsumer<? super R, ? super C, ? super V, E> action) throws E {
        if (_isInitialized) {
            for (final C columnKey : _columnKeySet) {
                for (final R rowKey : _rowKeySet) {
                    action.accept(rowKey, columnKey, get(rowKey, columnKey));
                }
            }
        } else {
            for (final C columnKey : _columnKeySet) {
                for (final R rowKey : _rowKeySet) {
                    action.accept(rowKey, columnKey, null);
                }
            }
        }
    }

    /**
     * Performs the given action for each non-null cell in the Sheet in horizontal order (row by row).
     * <p>
     * Iterates through cells row by row, calling the action only for cells containing non-null values.
     * Skips null and uninitialized cells. The action is guaranteed to receive non-null values.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, null}, {3, 4}}
     * );
     * 
     * sheet.forEachNonNullH((r, c, v) -> System.out.println(r + "," + c + "=" + v));
     * // Prints: row1,col1=1  row2,col1=3  row2,col2=4 (skips null)
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each non-null cell; receives row key, column key, and non-null value
     * @throws E if the action throws an exception
     * @see #forEachH(Throwables.TriConsumer)
     * @see #forEachNonNullV(Throwables.TriConsumer)
     */
    public <E extends Exception> void forEachNonNullH(final Throwables.TriConsumer<? super R, ? super C, ? super V, E> action) throws E {
        if (_isInitialized) {
            V value = null;

            for (final R rowKey : _rowKeySet) {
                for (final C columnKey : _columnKeySet) {
                    if ((value = get(rowKey, columnKey)) != null) {
                        action.accept(rowKey, columnKey, value);
                    }
                }
            }
        } else {
            // ...
        }
    }

    /**
     * Performs the given action for each non-null cell in the Sheet in vertical order (column by column).
     * <p>
     * Iterates through cells column by column, calling the action only for cells containing non-null values.
     * Skips null and uninitialized cells. The action is guaranteed to receive non-null values.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, null}, {3, 4}}
     * );
     * 
     * sheet.forEachNonNullV((r, c, v) -> System.out.println(r + "," + c + "=" + v));
     * // Prints: row1,col1=1  row2,col1=3  row2,col2=4 (skips null)
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each non-null cell; receives row key, column key, and non-null value
     * @throws E if the action throws an exception
     * @see #forEachV(Throwables.TriConsumer)
     * @see #forEachNonNullH(Throwables.TriConsumer)
     */
    public <E extends Exception> void forEachNonNullV(final Throwables.TriConsumer<? super R, ? super C, ? super V, E> action) throws E {
        if (_isInitialized) {
            V value = null;

            for (final C columnKey : _columnKeySet) {
                for (final R rowKey : _rowKeySet) {
                    if ((value = get(rowKey, columnKey)) != null) {
                        action.accept(rowKey, columnKey, value);
                    }
                }
            }
        } else {
            // ...
        }
    }

    /**
     * Returns a stream of all cells in the Sheet in horizontal order (row by row).
     * <p>
     * Creates a stream that iterates through all cells row by row. Each cell is represented
     * as a Cell object containing the row key, column key, and value.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * sheet.cellsH().forEach(cell -> 
     *     System.out.println(cell.rowKey() + "," + cell.columnKey() + "=" + cell.value()));
     * }</pre>
     *
     * @return a Stream of Cell objects representing all cells, ordered by rows
     * @see #cellsH(int, int)
     * @see #cellsV()
     */
    public Stream<Sheet.Cell<R, C, V>> cellsH() {
        return cellsH(0, rowLength());
    }

    //    /**
    //     * Returns a stream of cells from a specific row in the Sheet.
    //     * <p>
    //     * Creates a stream containing all cells from the specified row, ordered by columns.
    //     * </p>
    //     *
    //     * <pre>{@code
    //     * Sheet<String, String, Integer> sheet = Sheet.rows(
    //     *     List.of("row1", "row2"),
    //     *     List.of("col1", "col2", "col3"),
    //     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
    //     * );
    //     * 
    //     * sheet.cellsH(0).forEach(cell -> System.out.println(cell.value()));
    //     * // Prints: 1, 2, 3 (values from row1)
    //     * }</pre>
    //     *
    //     * @param rowIndex the zero-based index of the row
    //     * @return a Stream of Cell objects from the specified row
    //     * @throws IndexOutOfBoundsException if rowIndex < 0 or rowIndex >= rowLength()
    //     * @see #cellsH(int, int)
    //     */
    //    public Stream<Sheet.Cell<R, C, V>> cellsH(final int rowIndex) throws IndexOutOfBoundsException {
    //        return cellsH(rowIndex, rowIndex + 1);
    //    }

    /**
     * Returns a stream of cells from a range of rows in horizontal order.
     * <p>
     * Creates a stream containing cells from the specified row range [fromRowIndex, toRowIndex),
     * ordered row by row. The toRowIndex is exclusive.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     * 
     * // Get cells from rows 0 and 1 (excluding row 2)
     * sheet.cellsH(0, 2).forEach(cell -> System.out.println(cell.value()));
     * // Prints: 1, 2, 3, 4
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of Cell objects from the specified row range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex > toRowIndex
     * @see #cellsH()
     * @see #cellsV(int, int)
     */
    public Stream<Sheet.Cell<R, C, V>> cellsH(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        final int columnLength = columnLength();

        initIndexMap();

        return Stream.of(new ObjIteratorEx<>() {
            private final long toIndex = (long) toRowIndex * columnLength;
            private long cursor = (long) fromRowIndex * columnLength;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Sheet.Cell<R, C, V> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int rowIndex = (int) (cursor / columnLength);
                final int columnIndex = (int) (cursor++ % columnLength);

                return Cell.of(_rowKeyIndexMap.getByValue(rowIndex), _columnKeyIndexMap.getByValue(columnIndex),
                        _isInitialized ? _columnList.get(columnIndex).get(rowIndex) : null);
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of all cells in the Sheet in vertical order (column by column).
     * <p>
     * Creates a stream that iterates through all cells column by column. Each cell is represented
     * as a Cell object containing the row key, column key, and value.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * sheet.cellsV().forEach(cell -> 
     *     System.out.println(cell.rowKey() + "," + cell.columnKey() + "=" + cell.value()));
     * // Prints: row1,col1=1  row2,col1=3  row1,col2=2  row2,col2=4
     * }</pre>
     *
     * @return a Stream of Cell objects representing all cells, ordered by columns
     * @see #cellsV(int, int)
     * @see #cellsH()
     */
    public Stream<Sheet.Cell<R, C, V>> cellsV() {
        return cellsV(0, columnLength());
    }

    //    /**
    //     * Returns a stream of cells from a specific column in the Sheet.
    //     * <p>
    //     * Creates a stream containing all cells from the specified column, ordered by rows.
    //     * </p>
    //     *
    //     * <pre>{@code
    //     * Sheet<String, String, Integer> sheet = Sheet.rows(
    //     *     List.of("row1", "row2", "row3"),
    //     *     List.of("col1", "col2"),
    //     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
    //     * );
    //     * 
    //     * sheet.cellsV(0).forEach(cell -> System.out.println(cell.value()));
    //     * // Prints: 1, 3, 5 (values from col1)
    //     * }</pre>
    //     *
    //     * @param columnIndex the zero-based index of the column
    //     * @return a Stream of Cell objects from the specified column
    //     * @throws IndexOutOfBoundsException if columnIndex < 0 or columnIndex >= columnLength()
    //     * @see #cellsV(int, int)
    //     */
    //    public Stream<Sheet.Cell<R, C, V>> cellsV(final int columnIndex) throws IndexOutOfBoundsException {
    //        return cellsV(columnIndex, columnIndex + 1);
    //    }

    /**
     * Returns a stream of cells from a range of columns in vertical order.
     * <p>
     * Creates a stream containing cells from the specified column range [fromColumnIndex, toColumnIndex),
     * ordered column by column. The toColumnIndex is exclusive.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * 
     * // Get cells from columns 0 and 1 (excluding column 2)
     * sheet.cellsV(0, 2).forEach(cell -> System.out.println(cell.value()));
     * // Prints: 1, 4, 2, 5
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of Cell objects from the specified column range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex > toColumnIndex
     * @see #cellsV()
     * @see #cellsH(int, int)
     */
    public Stream<Sheet.Cell<R, C, V>> cellsV(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        final int rowLength = rowLength();

        initIndexMap();

        return Stream.of(new ObjIteratorEx<>() {
            private final long toIndex = (long) toColumnIndex * rowLength;
            private long cursor = (long) fromColumnIndex * rowLength;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Sheet.Cell<R, C, V> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int rowIndex = (int) (cursor % rowLength);
                final int columnIndex = (int) (cursor++ / rowLength);

                return Cell.of(_rowKeyIndexMap.getByValue(rowIndex), _columnKeyIndexMap.getByValue(columnIndex),
                        _isInitialized ? _columnList.get(columnIndex).get(rowIndex) : null);
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of row streams, where each inner stream contains the cells of one row.
     * <p>
     * Creates a nested stream structure where the outer stream yields rows and each inner stream
     * contains the cells of that row ordered by columns.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * sheet.cellsR().forEach(rowStream -> {
     *     rowStream.forEach(cell -> System.out.print(cell.value() + " "));
     *     System.out.println();
     * });
     * // Prints: 1 2 \n 3 4
     * }</pre>
     *
     * @return a Stream of Streams where each inner stream represents a row's cells
     * @see #cellsR(int, int)
     * @see #cellsC()
     */
    public Stream<Stream<Cell<R, C, V>>> cellsR() {
        return cellsR(0, rowLength());
    }

    /**
     * Returns a stream of row streams for a range of rows.
     * <p>
     * Creates a nested stream structure for the specified row range [fromRowIndex, toRowIndex).
     * Each inner stream contains the cells of one row ordered by columns.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     * 
     * // Process rows 0 and 1 (excluding row 2)
     * sheet.cellsR(0, 2).forEach(rowStream -> {
     *     List<Integer> rowValues = rowStream.map(Cell::value).toList();
     *     System.out.println(rowValues);
     * });
     * // Prints: [1, 2] \n [3, 4]
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of Streams for the specified row range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex > toRowIndex
     * @see #cellsR()
     * @see #cellsC(int, int)
     */
    public Stream<Stream<Cell<R, C, V>>> cellsR(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        final int columnLength = columnLength();

        return Stream.of(new ObjIteratorEx<>() {
            private int rowIndex = fromRowIndex;

            @Override
            public boolean hasNext() {
                return rowIndex < toRowIndex;
            }

            @Override
            public Stream<Cell<R, C, V>> next() {
                if (rowIndex >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Stream.of(new ObjIteratorEx<>() {
                    private final int curRowIndex = rowIndex++;
                    private final R r = _rowKeyIndexMap.getByValue(curRowIndex);
                    private int columnIndex = 0;

                    @Override
                    public boolean hasNext() {
                        return columnIndex < columnLength;
                    }

                    @Override
                    public Cell<R, C, V> next() {
                        if (columnIndex >= columnLength) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final int curColumnIndex = columnIndex++;

                        return Cell.of(r, _columnKeyIndexMap.getByValue(curColumnIndex),
                                _isInitialized ? _columnList.get(curColumnIndex).get(curRowIndex) : null);
                    }

                    @Override
                    public void advance(final long n) {
                        if (n <= 0) {
                            return;
                        }

                        columnIndex = n < columnLength - columnIndex ? columnIndex + (int) n : columnLength;
                    }

                    @Override
                    public long count() {
                        return columnLength - columnIndex; //NOSONAR
                    }
                });
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                rowIndex = n < toRowIndex - rowIndex ? rowIndex + (int) n : toRowIndex;
            }

            @Override
            public long count() {
                return toRowIndex - rowIndex; //NOSONAR
            }
        });
    }

    /**
     * Returns a stream of column streams, where each inner stream contains the cells of one column.
     * <p>
     * Creates a nested stream structure where the outer stream yields columns and each inner stream
     * contains the cells of that column ordered by rows.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * sheet.cellsC().forEach(colStream -> {
     *     colStream.forEach(cell -> System.out.print(cell.value() + " "));
     *     System.out.println();
     * });
     * // Prints: 1 3 \n 2 4
     * }</pre>
     *
     * @return a Stream of Streams where each inner stream represents a column's cells
     * @see #cellsC(int, int)
     * @see #cellsR()
     */
    public Stream<Stream<Cell<R, C, V>>> cellsC() {
        return cellsC(0, columnLength());
    }

    /**
     * Returns a stream of column streams for a range of columns.
     * <p>
     * Creates a nested stream structure for the specified column range [fromColumnIndex, toColumnIndex).
     * Each inner stream contains the cells of one column ordered by rows.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * 
     * // Process columns 0 and 1 (excluding column 2)
     * sheet.cellsC(0, 2).forEach(colStream -> {
     *     List<Integer> colValues = colStream.map(Cell::value).toList();
     *     System.out.println(colValues);
     * });
     * // Prints: [1, 4] \n [2, 5]
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of Streams for the specified column range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex > toColumnIndex
     * @see #cellsC()
     * @see #cellsR(int, int)
     */
    public Stream<Stream<Cell<R, C, V>>> cellsC(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        final int rowLength = rowLength();

        return Stream.of(new ObjIteratorEx<>() {
            private int columnIndex = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return columnIndex < toColumnIndex;
            }

            @Override
            public Stream<Cell<R, C, V>> next() {
                if (columnIndex >= toColumnIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int curColumnIndex = columnIndex++;
                final C c = _columnKeyIndexMap.getByValue(curColumnIndex);

                if (_isInitialized) {
                    final List<V> column = _columnList.get(curColumnIndex);

                    //noinspection resource
                    return IntStream.range(0, rowLength).mapToObj(rowIndex -> Cell.of(_rowKeyIndexMap.getByValue(rowIndex), c, column.get(rowIndex)));
                } else {
                    //noinspection resource
                    return IntStream.range(0, rowLength).mapToObj(rowIndex -> Cell.of(_rowKeyIndexMap.getByValue(rowIndex), c, null));
                }

            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                columnIndex = n < toColumnIndex - columnIndex ? columnIndex + (int) n : toColumnIndex;
            }

            @Override
            public long count() {
                return toColumnIndex - columnIndex; //NOSONAR
            }

        });
    }

    /**
     * Returns a stream of all coordinate points in the Sheet in horizontal order (row by row).
     * <p>
     * Creates a stream of Point objects representing cell coordinates. Each Point contains
     * zero-based row and column indices. Points are ordered row by row.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2")
     * );
     * 
     * sheet.pointsH().forEach(point -> 
     *     System.out.println("(" + point.rowIndex() + "," + point.columnIndex() + ")"));
     * // Prints: (0,0) (0,1) (1,0) (1,1)
     * }</pre>
     *
     * @return a Stream of Point objects representing all cell coordinates, ordered by rows
     * @see #pointsH(int, int)
     * @see #pointsV()
     */
    public Stream<Point> pointsH() {
        return pointsH(0, rowLength());
    }

    //    /**
    //     * Returns a stream of coordinate points from a specific row.
    //     * <p>
    //     * Creates a stream of Point objects for all column positions in the specified row.
    //     * Points are ordered by column index.
    //     * </p>
    //     *
    //     * <pre>{@code
    //     * Sheet<String, String, Integer> sheet = new Sheet<>(
    //     *     List.of("row1", "row2"),
    //     *     List.of("col1", "col2", "col3")
    //     * );
    //     * 
    //     * sheet.pointsH(0).forEach(point -> 
    //     *     System.out.println("(" + point.rowIndex() + "," + point.columnIndex() + ")"));
    //     * // Prints: (0,0) (0,1) (0,2)
    //     * }</pre>
    //     *
    //     * @param rowIndex the zero-based index of the row
    //     * @return a Stream of Point objects from the specified row
    //     * @throws IndexOutOfBoundsException if rowIndex < 0 or rowIndex >= rowLength()
    //     * @see #pointsH(int, int)
    //     */
    //    public Stream<Point> pointsH(final int rowIndex) throws IndexOutOfBoundsException {
    //        return pointsH(rowIndex, rowIndex + 1);
    //    }

    /**
     * Returns a stream of Point objects representing the points in the Sheet in a horizontal manner.
     * Each Point represents a point in the Sheet with its row and column indices.
     * The points are ordered by rows, starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     *
     * @param fromRowIndex The index of the row from which the stream should start.
     * @param toRowIndex The index of the row at which the stream should end.
     * @return A Stream of Point objects representing the points in the Sheet, ordered by rows starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the range of the Sheet's row indices.
     */
    public Stream<Point> pointsH(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowLength());

        final int columnLength = columnLength();

        //noinspection resource
        return IntStream.range(fromRowIndex, toRowIndex)
                .flatMapToObj(rowIndex -> IntStream.range(0, columnLength).mapToObj(columnIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of Point objects representing the points in the Sheet in a vertical manner.
     * Each Point represents a point in the Sheet with its row and column indices.
     * The points are ordered by columns, meaning the points in the first column are followed by the points in the second column, and so on.
     *
     * @return A Stream of Point objects representing the points in the Sheet, ordered by columns.
     */
    public Stream<Point> pointsV() {
        return pointsV(0, columnLength());
    }

    //    /**
    //     * Returns a stream of Point objects representing the points in the specified column in the Sheet.
    //     * Each Point represents a point in the Sheet with its row and column indices.
    //     * The points are ordered by rows, meaning the points in the first row are followed by the points in the subsequent rows.
    //     *
    //     * @param columnIndex The index of the column from which the stream should start.
    //     * @return A Stream of Point objects representing the points in the specified column in the Sheet.
    //     * @throws IndexOutOfBoundsException if the columnIndex is out of the range of the Sheet's column indices.
    //     */
    //    public Stream<Point> pointsV(final int columnIndex) throws IndexOutOfBoundsException {
    //        return pointsV(columnIndex, columnIndex + 1);
    //    }

    /**
     * Returns a stream of Point objects representing the points in the Sheet in a vertical manner.
     * Each Point represents a point in the Sheet with its row and column indices.
     * The points are ordered by columns, starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     *
     * @param fromColumnIndex The index of the column from which the stream should start.
     * @param toColumnIndex The index of the column at which the stream should end.
     * @return A Stream of Point objects representing the points in the Sheet, ordered by columns starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     * @throws IndexOutOfBoundsException if the fromColumnIndex or toColumnIndex is out of the range of the Sheet's column indices.
     */
    public Stream<Point> pointsV(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        final int rowLength = rowLength();

        //noinspection resource
        return IntStream.range(fromColumnIndex, toColumnIndex)
                .flatMapToObj(columnIndex -> IntStream.range(0, rowLength).mapToObj(rowIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of streams of Point objects representing the points in the Sheet in a horizontal manner.
     * Each inner stream represents a row in the Sheet, and the outer stream represents the sequence of these rows.
     * Each Point in the inner stream represents a point in the Sheet with its row and column indices.
     * The points in each row are ordered by columns, meaning the points in the first column are followed by the points in the second column, and so on.
     *
     * @return A Stream of Stream of Point objects representing the points in the Sheet, ordered by rows.
     */
    public Stream<Stream<Point>> pointsR() {
        return pointsR(0, rowLength());
    }

    /**
     * Returns a stream of streams of Point objects representing the points in the Sheet in a horizontal manner.
     * Each inner stream represents a row in the Sheet, and the outer stream represents the sequence of these rows.
     * Each Point in the inner stream represents a point in the Sheet with its row and column indices.
     * The points in each row are ordered by columns, meaning the points in the first column are followed by the points in the second column, and so on.
     * The points are ordered by rows, starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     *
     * @param fromRowIndex The index of the row from which the stream should start.
     * @param toRowIndex The index of the row at which the stream should end.
     * @return A Stream of Stream of Point objects representing the points in the Sheet, ordered by rows starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the range of the Sheet's row indices.
     */
    public Stream<Stream<Point>> pointsR(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowLength());

        final int columnLength = columnLength();

        //noinspection resource
        return IntStream.range(fromRowIndex, toRowIndex)
                .mapToObj(rowIndex -> IntStream.range(0, columnLength).mapToObj(columnIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of streams of Point objects representing the points in the Sheet in a vertical manner.
     * Each inner stream represents a column in the Sheet, and the outer stream represents the sequence of these columns.
     * Each Point in the inner stream represents a point in the Sheet with its row and column indices.
     * The points in each column are ordered by rows, meaning the points in the first row are followed by the points in the subsequent rows.
     *
     * @return A Stream of Stream of Point objects representing the points in the Sheet, ordered by columns.
     */
    public Stream<Stream<Point>> pointsC() {
        return pointsR(0, columnLength());
    }

    /**
     * Returns a stream of streams of Point objects representing the points in the Sheet in a vertical manner.
     * Each inner stream represents a column in the Sheet, and the outer stream represents the sequence of these columns.
     * Each Point in the inner stream represents a point in the Sheet with its row and column indices.
     * The points in each column are ordered by rows, meaning the points in the first row are followed by the points in the subsequent rows.
     * The points are ordered by columns, starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     *
     * @param fromColumnIndex The index of the column from which the stream should start.
     * @param toColumnIndex The index of the column at which the stream should end.
     * @return A Stream of Stream of Point objects representing the points in the Sheet, ordered by columns starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     * @throws IndexOutOfBoundsException if the fromColumnIndex or toColumnIndex is out of the range of the Sheet's column indices.
     */
    public Stream<Stream<Point>> pointsC(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        final int rowLength = rowLength();

        //noinspection resource
        return IntStream.range(fromColumnIndex, toColumnIndex)
                .mapToObj(columnIndex -> IntStream.range(0, rowLength).mapToObj(rowIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of all values in the Sheet in horizontal order (row by row).
     * <p>
     * Creates a stream containing all cell values, ordered row by row. Includes null values
     * from empty or uninitialized cells.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}}
     * );
     * 
     * sheet.streamH().forEach(System.out::println);
     * // Prints: 1, 2, 3, null
     * }</pre>
     *
     * @return a Stream of values from all cells, ordered by rows
     * @see #streamH(int, int)
     * @see #streamV()
     */
    public Stream<V> streamH() {
        return streamH(0, rowLength());
    }

    //    /**
    //     * Returns a stream of values from a specific row.
    //     * <p>
    //     * Creates a stream containing all values from the specified row, ordered by columns.
    //     * Includes null values from empty cells.
    //     * </p>
    //     *
    //     * <pre>{@code
    //     * Sheet<String, String, Integer> sheet = Sheet.rows(
    //     *     List.of("row1", "row2"),
    //     *     List.of("col1", "col2", "col3"),
    //     *     new Integer[][] {{1, 2, 3}, {4, null, 6}}
    //     * );
    //     * 
    //     * sheet.streamH(1).forEach(System.out::println);
    //     * // Prints: 4, null, 6 (values from row2)
    //     * }</pre>
    //     *
    //     * @param rowIndex the zero-based index of the row
    //     * @return a Stream of values from the specified row
    //     * @throws IndexOutOfBoundsException if rowIndex < 0 or rowIndex >= rowLength()
    //     * @see #streamH(int, int)
    //     */
    //    public Stream<V> streamH(final int rowIndex) throws IndexOutOfBoundsException {
    //        return streamH(rowIndex, rowIndex + 1);
    //    }

    /**
     * Returns a stream of values in the Sheet in a horizontal manner.
     * The values are ordered by rows, starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     * The values in each row are ordered by columns, meaning the values in the first column are followed by the values in the subsequent columns.
     *
     * @param fromRowIndex The index of the row from which the stream should start.
     * @param toRowIndex The index of the row at which the stream should end.
     * @return A Stream of values representing the values in the Sheet, ordered by rows starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the range of the Sheet's row indices.
     */
    public Stream<V> streamH(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int columnLength = columnLength();
            private final long toIndex = (long) toRowIndex * columnLength;
            private long cursor = (long) fromRowIndex * columnLength;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public V next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (_isInitialized) {
                    return _columnList.get((int) (cursor % columnLength)).get((int) (cursor++ / columnLength));
                } else {
                    cursor++;
                    return null;
                }
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of values in the Sheet in a vertical manner.
     * The values are ordered by columns, meaning the values in the first column are followed by the values in the second column, and so on.
     *
     * @return A Stream of values representing the values in the Sheet, ordered by columns.
     */
    public Stream<V> streamV() {
        return streamV(0, columnLength());
    }

    //    /**
    //     * Returns a stream of values in the specified column in the Sheet.
    //     * The values are ordered by rows, meaning the values in the first row are followed by the values in the subsequent rows.
    //     *
    //     * @param columnIndex The index of the column from which the stream should start.
    //     * @return A Stream of values representing the values in the specified column in the Sheet.
    //     * @throws IndexOutOfBoundsException if the columnIndex is out of the range of the Sheet's column indices.
    //     */
    //    public Stream<V> streamV(final int columnIndex) throws IndexOutOfBoundsException {
    //        return streamV(columnIndex, columnIndex + 1);
    //    }

    /**
     * Returns a stream of values from a range of columns in vertical order.
     * <p>
     * Creates a stream containing values from the specified column range [fromColumnIndex, toColumnIndex),
     * ordered column by column. The toColumnIndex is exclusive. Includes null values from empty cells.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, null, 6}}
     * );
     * 
     * // Get values from columns 0 and 1 (excluding column 2)
     * sheet.streamV(0, 2).forEach(System.out::println);
     * // Prints: 1, 4, 2, null
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of values from the specified column range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex > toColumnIndex
     * @see #streamV()
     * @see #streamH(int, int)
     */
    public Stream<V> streamV(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int rowLength = rowLength();
            private final long toIndex = (long) toColumnIndex * rowLength;
            private long cursor = (long) fromColumnIndex * rowLength;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public V next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (_isInitialized) {
                    return _columnList.get((int) (cursor / rowLength)).get((int) (cursor++ % rowLength));
                } else {
                    cursor++;
                    return null;
                }
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of row streams, where each inner stream contains the values of one row.
     * <p>
     * Creates a nested stream structure where the outer stream yields rows and each inner stream
     * contains the values of that row ordered by columns. Includes null values.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}}
     * );
     * 
     * sheet.streamR().forEach(rowStream -> {
     *     rowStream.forEach(value -> System.out.print(value + " "));
     *     System.out.println();
     * });
     * // Prints: 1 2 \n 3 null
     * }</pre>
     *
     * @return a Stream of Streams where each inner stream represents a row's values
     * @see #streamR(int, int)
     * @see #streamC()
     */
    public Stream<Stream<V>> streamR() {
        return streamR(0, rowLength());
    }

    /**
     * Returns a stream of row streams for a range of rows.
     * <p>
     * Creates a nested stream structure for the specified row range [fromRowIndex, toRowIndex).
     * Each inner stream contains the values of one row ordered by columns. Includes null values.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}, {5, 6}}
     * );
     * 
     * // Process rows 0 and 1 (excluding row 2)
     * sheet.streamR(0, 2).forEach(rowStream -> {
     *     List<Integer> rowValues = rowStream.toList();
     *     System.out.println(rowValues);
     * });
     * // Prints: [1, 2] \n [3, null]
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of Streams for the specified row range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex > toRowIndex
     * @see #streamR()
     * @see #streamC(int, int)
     */
    public Stream<Stream<V>> streamR(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<V> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Stream.of(new ObjIteratorEx<>() {
                    private final int rowIndex = cursor++;
                    private final int toIndex2 = columnLength();
                    private int cursor2 = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor2 < toIndex2;
                    }

                    @Override
                    public V next() {
                        if (cursor2 >= toIndex2) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        if (_isInitialized) {
                            return _columnList.get(cursor2++).get(rowIndex);
                        } else {
                            cursor2++;
                            return null;
                        }
                    }

                    @Override
                    public void advance(final long n) {
                        if (n <= 0) {
                            return;
                        }

                        cursor2 = n < toIndex2 - cursor2 ? cursor2 + (int) n : toIndex2;
                    }

                    @Override
                    public long count() {
                        return toIndex2 - cursor2; //NOSONAR
                    }
                });
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of column value streams.
     * <p>
     * Creates a nested stream structure where the outer stream yields columns and each inner stream
     * contains the values of that column ordered by rows. Useful for column-wise processing.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * sheet.streamC().forEach(colStream -> {
     *     List<Integer> colValues = colStream.toList();
     *     System.out.println(colValues);
     * });
     * // Prints: [1, 3] \n [2, 4]
     * }</pre>
     *
     * @return a Stream of Streams where each inner stream represents a column's values
     * @see #streamC(int, int)
     * @see #streamR()
     */
    public Stream<Stream<V>> streamC() {
        return streamC(0, columnLength());
    }

    /**
     * Returns a stream of column value streams for a range of columns.
     * <p>
     * Creates a nested stream structure for the specified column range [fromColumnIndex, toColumnIndex).
     * Each inner stream contains the values of one column ordered by rows.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * 
     * // Process only first two columns
     * sheet.streamC(0, 2).forEach(colStream -> {
     *     List<Integer> colValues = colStream.toList();
     *     System.out.println(colValues);
     * });
     * // Prints: [1, 4] \n [2, 5]
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of Streams for the specified column range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex > toColumnIndex
     * @see #streamC()
     * @see #streamR(int, int)
     */
    public Stream<Stream<V>> streamC(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toColumnIndex;
            private int cursor = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<V> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (_isInitialized) {
                    return Stream.of(_columnList.get(cursor++));
                } else {
                    cursor++;
                    return Stream.repeat(null, rowLength());
                }
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of key-value pairs representing all rows in the Sheet.
     * <p>
     * Each pair contains a row key and a stream of all values in that row (ordered by columns).
     * This provides a convenient way to process rows with their identifiers.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * sheet.rows().forEach(pair -> {
     *     String rowKey = pair.left;
     *     Stream<Integer> values = pair.right;
     *     System.out.println(rowKey + ": " + values.toList());
     * });
     * // Prints: row1: [1, 2]  row2: [3, 4]
     * }</pre>
     *
     * @return a Stream of Pair objects where each pair contains a row key and its value stream
     * @see #rows(int, int)
     * @see #columns()
     */
    public Stream<Pair<R, Stream<V>>> rows() {
        return rows(0, rowLength());
    }

    /**
     * Returns a stream of pairs in the Sheet, where each pair consists of a row key and a stream of values in that row.
     * Each pair in the stream represents a row in the Sheet, with the row key and a stream of values in that row.
     * The pairs are ordered by rows, starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     *
     * @param fromRowIndex The index of the row from which the stream should start.
     * @param toRowIndex The index of the row at which the stream should end.
     * @return A Stream of Pair objects, where each Pair consists of a row key and a Stream of values in that row, ordered by rows starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the range of the Sheet's row indices.
     */
    public Stream<Pair<R, Stream<V>>> rows(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Pair<R, Stream<V>> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final R rowKey = _rowKeyIndexMap.getByValue(cursor);

                final Stream<V> row = Stream.of(new ObjIteratorEx<>() {
                    private final int rowIndex = cursor++;
                    private final int toIndex2 = columnLength();
                    private int cursor2 = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor2 < toIndex2;
                    }

                    @Override
                    public V next() {
                        if (cursor2 >= toIndex2) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        if (_isInitialized) {
                            return _columnList.get(cursor2++).get(rowIndex);
                        } else {
                            cursor2++;
                            return null;
                        }
                    }

                    @Override
                    public void advance(final long n) {
                        if (n <= 0) {
                            return;
                        }

                        cursor2 = n < toIndex2 - cursor2 ? cursor2 + (int) n : toIndex2;
                    }

                    @Override
                    public long count() {
                        return toIndex2 - cursor2; //NOSONAR
                    }
                });

                return Pair.of(rowKey, row);
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of key-value pairs representing all rows in the Sheet, where each pair consists of a row key and a mapped value.
     * <p>
     * Each pair in the stream represents a row in the Sheet, with the row key and a value obtained by applying the provided {@code rowMapper} function to the row's values.
     * The pairs are ordered by rows.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * // Map each row to its sum
     * sheet.rows((rowIndex, row) -> row.join("-"))
     *      .forEach(pair -> System.out.println(pair.left() + ": " + pair.right()));
     * // Prints: 
     * // row1: 1-2  
     * // row2: 3-4
     * }</pre>
     *
     * @param <T> The type of the mapped value for each row.
     * @param rowMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the Dataset, and the DisposableObjArray represents the row itself.
     * @return A Stream of Pair objects, where each Pair consists of a row key and a mapped value obtained by applying the {@code rowMapper} function to the row's values, ordered by rows.
     */
    public <T> Stream<Pair<R, T>> rows(final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) {
        return rows(0, rowLength(), rowMapper);
    }

    /**
     * Returns a stream of key-value pairs for a range of rows in the Sheet, where each pair consists of a row key and a mapped value.
     * <p>
     * Each pair in the stream represents a row in the specified range [fromRowIndex, toRowIndex), with the row key and a value obtained by applying the provided {@code rowMapper} function to the row's values.
     * The pairs are ordered by rows.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     * 
     * // Map each row to its sum for rows 0 and 1 (excluding row 2)
     * sheet.rows(0, 2, (rowIndex, row) -> row.join("-"))
     *      .forEach(pair -> System.out.println(pair.left() + ": " + pair.right()));
     * // Prints: 
     * // row1: 1-2  
     * // row2: 3-4
     * }</pre>
     *
     * @param <T> The type of the mapped value for each row.
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @param rowMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the Dataset, and the DisposableObjArray represents the row itself.
     * @return A Stream of Pair objects for the specified row range, where each Pair consists of a row key and a mapped value obtained by applying the {@code rowMapper} function to the row's values, ordered by rows.
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex > toRowIndex
     */
    public <T> Stream<Pair<R, T>> rows(final int fromRowIndex, final int toRowIndex, final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int columnLength = columnLength();
            private final Object[] rowData = new Object[columnLength];
            private final DisposableObjArray rowArray = DisposableObjArray.wrap(rowData);
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Pair<R, T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final R rowKey = _rowKeyIndexMap.getByValue(cursor);

                if (_isInitialized) {
                    for (int i = 0; i < columnLength; i++) {
                        rowData[i] = _columnList.get(i).get(cursor);
                    }
                }

                return Pair.of(rowKey, rowMapper.apply(cursor++, rowArray));
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of key-value pairs representing all columns in the Sheet.
     * <p>
     * Each pair contains a column key and a stream of all values in that column (ordered by rows).
     * This provides a convenient way to process columns with their identifiers.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * sheet.columns().forEach(pair -> {
     *     String colKey = pair.left;
     *     Stream<Integer> values = pair.right;
     *     System.out.println(colKey + ": " + values.toList());
     * });
     * // Prints: col1: [1, 3]  col2: [2, 4]
     * }</pre>
     *
     * @return a Stream of Pair objects where each pair contains a column key and its value stream
     * @see #columns(int, int)
     * @see #rows()
     */
    public Stream<Pair<C, Stream<V>>> columns() {
        return columns(0, columnLength());
    }

    /**
     * Returns a stream of key-value pairs for a range of columns in the Sheet.
     * <p>
     * Each pair contains a column key and a stream of all values in that column (ordered by rows).
     * Only columns in the specified range [fromColumnIndex, toColumnIndex) are included.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * 
     * // Process only first two columns
     * sheet.columns(0, 2).forEach(pair -> {
     *     String colKey = pair.left;
     *     List<Integer> values = pair.right.toList();
     *     System.out.println(colKey + ": " + values);
     * });
     * // Prints: col1: [1, 4]  col2: [2, 5]
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of Pair objects for the specified column range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex > toColumnIndex
     * @see #columns()
     * @see #rows(int, int)
     */
    public Stream<Pair<C, Stream<V>>> columns(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toColumnIndex;
            private int cursor = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Pair<C, Stream<V>> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final C columnKey = _columnKeyIndexMap.getByValue(cursor);

                if (_isInitialized) {
                    return Pair.of(columnKey, Stream.of(_columnList.get(cursor++)));
                } else {
                    cursor++;
                    return Pair.of(columnKey, Stream.repeat(null, rowLength()));
                }
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of key-value pairs representing all columns in the Sheet, where each pair consists of a column key and a mapped value.
     * <p>
     * Each pair in the stream represents a column in the Sheet, with the column key and a value obtained by applying the provided {@code columnMapper} function to the column's values.
     * The pairs are ordered by columns.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * // Map each column to its sum
     * sheet.columns((colIndex, col) -> col.join("-"))
     *      .forEach(pair -> System.out.println(pair.left() + ": " + pair.right()));
     * // Prints: 
     * // col1: 1-3  
     * // col2: 2-4
     * }</pre>
     *
     * @param <T> The type of the mapped value for each column.
     * @param columnMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                     The integer represents the index of the column in the Dataset, and the DisposableObjArray represents the column itself.
     * @return A Stream of Pair objects, where each Pair consists of a column key and a mapped value obtained by applying the {@code columnMapper} function to the column's values, ordered by columns.
     */
    public <T> Stream<Pair<C, T>> columns(final IntObjFunction<? super DisposableObjArray, ? extends T> columnMapper) {
        return columns(0, columnLength(), columnMapper);
    }

    /**
     * Returns a stream of key-value pairs for a range of columns in the Sheet, where each pair consists of a column key and a mapped value.
     * <p>
     * Each pair in the stream represents a column in the specified range [fromColumnIndex, toColumnIndex), with the column key and a value obtained by applying the provided {@code columnMapper} function to the column's values.
     * The pairs are ordered by columns.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}
     * );
     * 
     * // Map each column to its sum for columns 0 and 1 (excluding column 2)
     * sheet.columns(0, 2, (colIndex, col) -> col.join("-"))
     *      .forEach(pair -> System.out.println(pair.left() + ": " + pair.right()));
     * // Prints: 
     * // col1: 1-4-7  
     * // col2: 2-5-8
     * }</pre>
     *
     * @param <T> The type of the mapped value for each column.
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @param columnMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                     The integer represents the index of the column in the Dataset, and the DisposableObjArray represents the column itself.
     * @return A Stream of Pair objects for the specified column range, where each Pair consists of a column key and a mapped value obtained by applying the {@code columnMapper} function to the column's values, ordered by columns.
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex > toColumnIndex
     */
    public <T> Stream<Pair<C, T>> columns(final int fromColumnIndex, final int toColumnIndex,
            final IntObjFunction<? super DisposableObjArray, ? extends T> columnMapper) {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int rowLength = rowLength();
            private final Object[] columnData = new Object[rowLength];
            private final DisposableObjArray columnArray = DisposableObjArray.wrap(columnData);
            private final int toIndex = toColumnIndex;
            private int cursor = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Pair<C, T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final C columnKey = _columnKeyIndexMap.getByValue(cursor);

                if (_isInitialized) {
                    _columnList.toArray(columnData);
                }

                return Pair.of(columnKey, columnMapper.apply(cursor++, columnArray));
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Converts the Sheet into a Dataset in a horizontal manner.
     * Each row in the Sheet becomes a row in the Dataset, with the column keys serving as the column names in the Dataset.
     * The Dataset is ordered by rows, meaning the first row in the Sheet becomes the first row in the Dataset, and so on.
     *
     * @return A Dataset object representing the data in the Sheet, ordered by rows.
     */
    public Dataset toDatasetH() {
        final int rowLength = rowLength();
        final int columnLength = columnLength();
        final List<String> datasetColumnNameList = new ArrayList<>(columnLength);

        for (final C columnKey : _columnKeySet) {
            datasetColumnNameList.add(N.toString(columnKey));
        }

        final List<List<Object>> datasetColumnList = new ArrayList<>(columnLength);

        if (_isInitialized) {
            for (final List<V> column : _columnList) {
                datasetColumnList.add(new ArrayList<>(column));
            }
        } else {
            for (int i = 0; i < columnLength; i++) {
                final List<Object> column = new ArrayList<>(rowLength);
                N.fill(column, 0, rowLength, null);
                datasetColumnList.add(column);
            }
        }

        return new RowDataset(datasetColumnNameList, datasetColumnList);
    }

    /**
     * Converts the Sheet into a Dataset in a vertical manner.
     * Each column in the Sheet becomes a row in the Dataset, with the row keys serving as the column names in the Dataset.
     * The Dataset is ordered by columns, meaning the first column in the Sheet becomes the first row in the Dataset, and so on.
     *
     * @return A Dataset object representing the data in the Sheet, ordered by columns.
     */
    public Dataset toDatasetV() {
        final int rowLength = rowLength();
        final int columnLength = columnLength();
        final List<String> datasetColumnNameList = new ArrayList<>(rowLength);

        for (final R rowKey : _rowKeySet) {
            datasetColumnNameList.add(N.toString(rowKey));
        }

        final List<List<Object>> datasetColumnList = new ArrayList<>(rowLength);

        if (_isInitialized) {
            for (int i = 0; i < rowLength; i++) {
                final List<Object> column = new ArrayList<>(columnLength);

                for (int j = 0; j < columnLength; j++) {
                    column.add(_columnList.get(j).get(i));
                }

                datasetColumnList.add(column);
            }
        } else {
            for (int i = 0; i < rowLength; i++) {
                final List<Object> column = new ArrayList<>(columnLength);
                N.fill(column, 0, columnLength, null);
                datasetColumnList.add(column);
            }
        }

        return new RowDataset(datasetColumnNameList, datasetColumnList);
    }

    /**
     * Converts the Sheet into a two-dimensional array with row-major ordering.
     * <p>
     * Returns an {@code Object[][]} where {@code array[i][j]} corresponds to the value at
     * row {@code i} and column {@code j} in the Sheet. The array dimensions match the Sheet dimensions.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * Object[][] array = sheet.toArrayH();
     * // array[0] = [1, 2] (row1)
     * // array[1] = [3, 4] (row2)
     * }</pre>
     *
     * @return a two-dimensional Object array with row-major ordering
     * @see #toArrayH(Class)
     * @see #toArrayV()
     */
    public Object[][] toArrayH() {
        final int rowLength = rowLength();
        final int columnLength = columnLength();
        final Object[][] copy = new Object[rowLength][columnLength];

        if (_isInitialized) {
            for (int i = 0; i < columnLength; i++) {
                final List<V> column = _columnList.get(i);

                for (int j = 0; j < rowLength; j++) {
                    copy[j][i] = column.get(j);
                }
            }
        }

        return copy;
    }

    /**
     * Converts the Sheet into a typed two-dimensional array with row-major ordering.
     * <p>
     * Returns a {@code T[][]} where {@code array[i][j]} corresponds to the value at
     * row {@code i} and column {@code j} in the Sheet. Values are cast to the specified type.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * Integer[][] array = sheet.toArrayH(Integer.class);
     * // array[0] = [1, 2] (row1)
     * // array[1] = [3, 4] (row2)
     * }</pre>
     *
     * @param <T> the type of the elements in the array
     * @param componentType the Class object representing the element type
     * @return a two-dimensional typed array with row-major ordering
     * @throws ClassCastException if any value cannot be cast to the specified type
     * @see #toArrayH()
     * @see #toArrayV(Class)
     */
    public <T> T[][] toArrayH(final Class<T> componentType) {
        final int rowLength = rowLength();
        final int columnLength = columnLength();
        final T[][] copy = N.newArray(N.newArray(componentType, 0).getClass(), rowLength);

        for (int i = 0; i < rowLength; i++) {
            copy[i] = N.newArray(componentType, columnLength);
        }

        if (_isInitialized) {
            for (int i = 0; i < columnLength; i++) {
                final List<V> column = _columnList.get(i);

                for (int j = 0; j < rowLength; j++) {
                    copy[j][i] = (T) column.get(j);
                }
            }
        }

        return copy;
    }

    /**
     * Converts the Sheet into a two-dimensional array with column-major ordering.
     * <p>
     * Returns an {@code Object[][]} where {@code array[i][j]} corresponds to the value at
     * column {@code i} and row {@code j} in the Sheet. This transposes the data compared to {@link #toArrayH()}.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * Object[][] array = sheet.toArrayV();
     * // array[0] = [1, 3] (col1)
     * // array[1] = [2, 4] (col2)
     * }</pre>
     *
     * @return a two-dimensional Object array with column-major ordering
     * @see #toArrayV(Class)
     * @see #toArrayH()
     */
    public Object[][] toArrayV() {
        final int rowLength = rowLength();
        final int columnLength = columnLength();
        final Object[][] copy = new Object[columnLength][rowLength];

        if (_isInitialized) {
            for (int i = 0; i < columnLength; i++) {
                _columnList.get(i).toArray(copy[i]);
            }
        }

        return copy;
    }

    /**
     * Converts the Sheet into a typed two-dimensional array with column-major ordering.
     * <p>
     * Returns a {@code T[][]} where {@code array[i][j]} corresponds to the value at
     * column {@code i} and row {@code j} in the Sheet. Values are cast to the specified type.
     * This transposes the data compared to {@link #toArrayH(Class)}.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * Integer[][] array = sheet.toArrayV(Integer.class);
     * // array[0] = [1, 3] (col1)
     * // array[1] = [2, 4] (col2)
     * }</pre>
     *
     * @param <T> the type of the elements in the array
     * @param componentType the Class object representing the element type
     * @return a two-dimensional typed array with column-major ordering
     * @throws ClassCastException if any value cannot be cast to the specified type
     * @see #toArrayV()
     * @see #toArrayH(Class)
     */
    public <T> T[][] toArrayV(final Class<T> componentType) {
        final int rowLength = rowLength();
        final int columnLength = columnLength();
        final T[][] copy = N.newArray(N.newArray(componentType, 0).getClass(), columnLength);

        for (int i = 0; i < columnLength; i++) {
            copy[i] = N.newArray(componentType, rowLength);
        }

        if (_isInitialized) {
            for (int i = 0; i < columnLength; i++) {
                _columnList.get(i).toArray(copy[i]);
            }
        }

        return copy;
    }

    /**
     * Applies a transformation function to this Sheet and returns the result.
     * <p>
     * This method enables functional-style operations on the Sheet by applying
     * a function that takes the Sheet as input and produces any desired result.
     * Useful for custom processing, aggregations, or transformations.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * // Calculate sum of all values
     * Integer sum = sheet.apply(s -> 
     *     s.streamH().filter(Objects::nonNull).mapToInt(Integer::intValue).sum());
     * }</pre>
     *
     * @param <T> the type of the result produced by the function
     * @param <E> the type of exception the function may throw
     * @param func the function to apply to this Sheet
     * @return the result produced by applying the function to this Sheet
     * @throws E if the function throws an exception
     * @see #applyIfNotEmpty(Throwables.Function)
     */
    public <T, E extends Exception> T apply(final Throwables.Function<? super Sheet<R, C, V>, T, E> func) throws E {
        return func.apply(this);
    }

    /**
     * Applies a transformation function to this Sheet if it's not empty, returning an Optional result.
     * <p>
     * This method provides safe functional-style operations by only applying the function
     * when the Sheet contains data. Returns an empty Optional if the Sheet is empty.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1"),
     *     new Integer[][] {{42}}
     * );
     * 
     * Optional<Integer> result = sheet.applyIfNotEmpty(s -> 
     *     s.streamH().mapToInt(Integer::intValue).max().orElse(0));
     * // result.isPresent() = true, result.get() = 42
     * 
     * Sheet<String, String, Integer> empty = new Sheet<>();
     * Optional<Integer> emptyResult = empty.applyIfNotEmpty(s -> 100);
     * // emptyResult.isEmpty() = true
     * }</pre>
     *
     * @param <T> the type of the result produced by the function
     * @param <E> the type of exception the function may throw
     * @param func the function to apply to this Sheet if not empty
     * @return an Optional containing the result if Sheet is not empty, empty Optional otherwise
     * @throws E if the function throws an exception
     * @see #apply(Throwables.Function)
     * @see #isEmpty()
     */
    public <T, E extends Exception> Optional<T> applyIfNotEmpty(final Throwables.Function<? super Sheet<R, C, V>, T, E> func) throws E {
        if (!isEmpty()) {
            return Optional.ofNullable(func.apply(this));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Executes an action on this Sheet without returning a value.
     * <p>
     * This method enables functional-style side effects on the Sheet, such as logging,
     * validation, or other operations that don't produce a return value.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * // Log sheet statistics
     * sheet.accept(s -> {
     *     System.out.println("Rows: " + s.rowLength());
     *     System.out.println("Columns: " + s.columnLength());
     *     System.out.println("Non-null values: " + s.countOfNonNullValue());
     * });
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on this Sheet
     * @throws E if the action throws an exception
     * @see #acceptIfNotEmpty(Throwables.Consumer)
     */
    public <E extends Exception> void accept(final Throwables.Consumer<? super Sheet<R, C, V>, E> action) throws E {
        action.accept(this);
    }

    /**
     * Executes an action on this Sheet if it's not empty, returning whether the action was performed.
     * <p>
     * This method provides conditional execution of side effects based on whether the Sheet contains data.
     * Returns {@link OrElse#TRUE} if the action was executed, {@link OrElse#FALSE} if the Sheet was empty.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1"),
     *     new Integer[][] {{42}}
     * );
     * 
     * OrElse result = sheet.acceptIfNotEmpty(s -> {
     *     System.out.println("Processing non-empty sheet...");
     * });
     * // result == OrElse.TRUE
     * 
     * Sheet<String, String, Integer> empty = new Sheet<>();
     * OrElse emptyResult = empty.acceptIfNotEmpty(s -> {
     *     System.out.println("This won't be printed");
     * });
     * // emptyResult == OrElse.FALSE
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on this Sheet if not empty
     * @return OrElse.TRUE if the action was executed, OrElse.FALSE if the Sheet was empty
     * @throws E if the action throws an exception
     * @see #accept(Throwables.Consumer)
     * @see #isEmpty()
     */
    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super Sheet<R, C, V>, E> action) throws E {
        if (!isEmpty()) {
            action.accept(this);

            return OrElse.TRUE;
        }

        return OrElse.FALSE;
    }

    /**
     * Prints the entire Sheet to standard output in a formatted table.
     * <p>
     * Outputs a nicely formatted ASCII table showing all rows and columns with their keys.
     * The table includes borders and proper alignment for readability.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("R1", "R2"),
     *     List.of("C1", "C2", "C3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * 
     * sheet.println();
     * // Output:
     * //      +----+----+----+
     * //      | C1 | C2 | C3 |
     * // +----+----+----+----+
     * // | R1 | 1  | 2  | 3  |
     * // | R2 | 4  | 5  | 6  |
     * // +----+----+----+----+
     * }</pre>
     *
     * @throws UncheckedIOException if an I/O error occurs while printing
     * @see #println(String)
     * @see #println(Appendable)
     */
    public void println() throws UncheckedIOException {
        println(_rowKeySet, _columnKeySet);
    }

    /**
     * Prints the entire Sheet to standard output with a prefix on each line.
     * <p>
     * Outputs a formatted ASCII table with the specified prefix prepended to every line.
     * Useful for logging or when integrating Sheet output into larger formatted output.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("R1", "R2"),
     *     List.of("C1", "C2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * sheet.println("## ");
     * // Output:
     * // ##      +----+----+
     * // ##      | C1 | C2 |
     * // ## +----+----+----+
     * // ## | R1 | 1  | 2  |
     * // ## | R2 | 3  | 4  |
     * // ## +----+----+----+
     * }</pre>
     *
     * @param prefix the string to prepend to each line of output
     * @throws UncheckedIOException if an I/O error occurs while printing
     * @see #println()
     * @see #println(Collection, Collection, String, Appendable)
     */
    public void println(String prefix) throws UncheckedIOException {
        println(_rowKeySet, _columnKeySet, prefix, System.out); // NOSONAR);
    }

    /**
     * Prints a subset of the Sheet to standard output showing only specified rows and columns.
     * <p>
     * Outputs a formatted ASCII table containing only the rows and columns specified by the key sets.
     * This allows for focused printing of relevant data portions.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("R1", "R2", "R3"),
     *     List.of("C1", "C2", "C3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}
     * );
     * 
     * // Print only specific rows and columns
     * sheet.println(List.of("R1", "R3"), List.of("C1", "C3"));
     * // Shows only intersection of R1,R3 with C1,C3
     * }</pre>
     *
     * @param rowKeySet the row keys to include in the output
     * @param columnKeySet the column keys to include in the output
     * @throws UncheckedIOException if an I/O error occurs while printing
     * @throws IllegalArgumentException if any specified keys don't exist in the Sheet
     * @see #println()
     * @see #println(Collection, Collection, Appendable)
     */
    public void println(final Collection<R> rowKeySet, final Collection<C> columnKeySet) throws UncheckedIOException {
        println(rowKeySet, columnKeySet, System.out); // NOSONAR
    }

    /**
     * Prints the entire Sheet to the specified output destination.
     * <p>
     * Outputs a formatted ASCII table to any Appendable (Writer, StringBuilder, etc.).
     * Useful for capturing Sheet output to files, strings, or other destinations.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("R1", "R2"),
     *     List.of("C1", "C2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * // Print to a file
     * try (FileWriter writer = new FileWriter("sheet.txt")) {
     *     sheet.println(writer);
     * }
     * 
     * // Print to a StringBuilder
     * StringBuilder sb = new StringBuilder();
     * sheet.println(sb);
     * String result = sb.toString();
     * }</pre>
     *
     * @param output the destination for the formatted output
     * @throws UncheckedIOException if an I/O error occurs while writing
     * @see #println()
     * @see #println(Collection, Collection, Appendable)
     */
    public void println(final Appendable output) throws UncheckedIOException {
        println(_rowKeySet, _columnKeySet, output);
    }

    /**
     * Prints the content of the Sheet to the provided Writer output.
     * The content is printed in a tabular format with specified row keys and column keys.
     *
     * @param rowKeySet The collection of row keys to be included in the output.
     * @param columnKeySet The collection of column keys to be included in the output.
     * @param output The appendable to which the content of the Sheet will be printed.
     * @throws IllegalArgumentException if the provided row keys or column keys are not included in this sheet.
     * @throws UncheckedIOException if an I/O error occurs while printing the content of the Sheet.
     * @see #println()
     * @see #println(String)
     */
    public void println(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final Appendable output)
            throws IllegalArgumentException, UncheckedIOException {
        println(rowKeySet, columnKeySet, null, output);
    }

    /**
     * Prints the content of the Sheet to the provided Writer output.
     * The content is printed in a tabular format with specified row keys and column keys.
     *
     * @param rowKeySet The collection of row keys to be included in the output.
     * @param columnKeySet The collection of column keys to be included in the output.
     * @param prefix The prefix string to be printed before each line of the Sheet content.
     * @param output The appendable to which the content of the Sheet will be printed.
     * @throws IllegalArgumentException if the provided row keys or column keys are not included in this sheet.
     * @throws UncheckedIOException if an I/O error occurs while printing the content of the Sheet.
     * @see #println()
     * @see #println(String)
     */
    public void println(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final String prefix, final Appendable output)
            throws IllegalArgumentException, UncheckedIOException {
        if (N.notEmpty(rowKeySet) && !_rowKeySet.containsAll(rowKeySet)) {
            throw new IllegalArgumentException("Row keys: " + N.difference(rowKeySet, _rowKeySet) + " are not included in this sheet row keys: " + _rowKeySet);
        }

        if (N.notEmpty(columnKeySet) && !_columnKeySet.containsAll(columnKeySet)) {
            throw new IllegalArgumentException(
                    "Column keys: " + N.difference(columnKeySet, _columnKeySet) + " are not included in this sheet Column keys: " + _columnKeySet);
        }

        N.checkArgNotNull(output, cs.output);

        final boolean isBufferedWriter = output instanceof Writer writer && IOUtil.isBufferedWriter(writer);
        final Writer bw = isBufferedWriter ? (Writer) output : (output instanceof Writer writer ? Objectory.createBufferedWriter((writer)) : null);
        final Appendable appendable = bw != null ? bw : output;
        final String lineSeparator = Strings.isEmpty(prefix) ? IOUtil.LINE_SEPARATOR : (IOUtil.LINE_SEPARATOR + prefix);

        try {
            if (N.notEmpty(prefix)) {
                appendable.append(prefix);
            }

            if (N.isEmpty(rowKeySet) && N.isEmpty(columnKeySet)) {
                appendable.append("+---+");
                appendable.append(lineSeparator);
                appendable.append("|   |");
                appendable.append(lineSeparator);
                appendable.append("+---+");
            } else {
                final int rowLen = rowKeySet.size();
                final int columnLen = N.max(2, columnKeySet.size() + 1);

                final int[] rowIndices = new int[rowLen];
                int idx = 0;

                for (final R rowKey : rowKeySet) {
                    rowIndices[idx++] = getRowIndex(rowKey);
                }

                final int[] columnIndices = new int[columnLen];
                idx = 0;
                columnIndices[idx++] = -1; // rowKey Column

                if (N.isEmpty(columnKeySet)) {
                    columnIndices[idx] = -1;
                } else {
                    for (final C columnKey : columnKeySet) {
                        columnIndices[idx++] = getColumnIndex(columnKey);
                    }
                }

                final List<String> columnNameList = new ArrayList<>(columnLen);
                columnNameList.add(" "); // add for row key column

                if (N.isEmpty(columnKeySet)) {
                    columnNameList.add(" "); // add for row key column
                } else {
                    for (final C ck : columnKeySet) {
                        columnNameList.add(N.toString(ck));
                    }
                }

                final List<List<String>> strColumnList = new ArrayList<>(columnLen);
                final int[] maxColumnLens = new int[columnLen];

                for (int i = 0; i < columnLen; i++) {
                    final List<String> strColumn = new ArrayList<>(rowLen);
                    int maxLen = N.len(columnNameList.get(i));
                    String str = null;

                    if (i == 0) {
                        for (final R rk : rowKeySet) {
                            str = N.toString(rk);
                            maxLen = N.max(maxLen, N.len(str));
                            strColumn.add(str);
                        }
                    } else if (columnIndices[i] < 0) {
                        maxLen = N.max(maxLen, 1);
                        N.fill(strColumn, 0, rowLen, " ");
                    } else if (!_isInitialized) {
                        maxLen = N.max(maxLen, 4);
                        N.fill(strColumn, 0, rowLen, "null");
                    } else {
                        for (final int rowIndex : rowIndices) {
                            str = N.toString(_columnList.get(columnIndices[i]).get(rowIndex));
                            maxLen = N.max(maxLen, N.len(str));
                            strColumn.add(str);
                        }
                    }

                    maxColumnLens[i] = maxLen;
                    strColumnList.add(strColumn);
                }

                final char hch = '-';
                final char hchDelta = 2;
                for (int i = 0; i < columnLen; i++) {
                    if (i == 0) {
                        appendable.append(Strings.repeat(' ', maxColumnLens[i] + hchDelta + 1));
                    } else {
                        appendable.append('+');

                        appendable.append(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                    }
                }

                appendable.append('+');
                appendable.append(lineSeparator);

                for (int i = 0; i < columnLen; i++) {
                    if (i == 0) {
                        appendable.append("  ");
                    } else {
                        appendable.append(" | ");
                    }

                    appendable.append(Strings.padEnd(columnNameList.get(i), maxColumnLens[i]));
                }

                appendable.append(" |");
                appendable.append(lineSeparator);

                for (int i = 0; i < columnLen; i++) {
                    appendable.append('+');

                    if (i == 1 && N.isEmpty(columnKeySet)) {
                        appendable.append(Strings.repeat(' ', maxColumnLens[i] + hchDelta));
                    } else {
                        appendable.append(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                    }
                }

                appendable.append('+');

                for (int j = 0; j < rowLen; j++) {
                    appendable.append(lineSeparator);

                    for (int i = 0; i < columnLen; i++) {
                        if (i == 0) {
                            appendable.append("| ");
                        } else {
                            appendable.append(" | ");
                        }

                        appendable.append(Strings.padEnd(strColumnList.get(i).get(j), maxColumnLens[i]));
                    }

                    appendable.append(" |");
                }

                if (rowLen == 0) {
                    appendable.append(lineSeparator);

                    for (int i = 0; i < columnLen; i++) {
                        if (i == 0) {
                            appendable.append("| ");
                            appendable.append(Strings.padEnd("", maxColumnLens[i]));
                        } else {
                            appendable.append(Strings.padEnd("", maxColumnLens[i] + 3));
                        }
                    }

                    appendable.append(" |");
                }

                appendable.append(lineSeparator);

                for (int i = 0; i < columnLen; i++) {
                    appendable.append('+');

                    appendable.append(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                }

                appendable.append('+');
            }

            appendable.append(IOUtil.LINE_SEPARATOR);

            if (bw != null) {
                bw.flush();
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (bw != null && !isBufferedWriter) {
                Objectory.recycle((BufferedWriter) bw);
            }
        }
    }

    /**
     * Returns a hash code value for this Sheet.
     * <p>
     * The hash code is computed based on the row keys, column keys, and values.
     * Two Sheets that are equal according to {@link #equals(Object)} will have the same hash code.
     * </p>
     *
     * @return a hash code value for this Sheet
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_rowKeySet == null) ? 0 : _rowKeySet.hashCode());
        result = prime * result + ((_columnKeySet == null) ? 0 : _columnKeySet.hashCode());
        return prime * result + (_isInitialized ? _columnList.hashCode() : 0);
    }

    /**
     * Indicates whether some other object is "equal to" this Sheet.
     * <p>
     * Two Sheets are considered equal if they have the same row keys, column keys,
     * and identical values at corresponding positions. The comparison uses deep equality
     * for values and considers the order of keys.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet1 = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1"),
     *     new Integer[][] {{42}}
     * );
     * 
     * Sheet<String, String, Integer> sheet2 = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1"),
     *     new Integer[][] {{42}}
     * );
     * 
     * boolean isEqual = sheet1.equals(sheet2); // true
     * }</pre>
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this Sheet is equal to the obj argument; {@code false} otherwise
     * @see #hashCode()
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof Sheet)) {
            return false;
        }

        final Sheet<R, C, V> other = (Sheet<R, C, V>) obj;

        return N.equals(other._rowKeySet, _rowKeySet) && N.equals(other._columnKeySet, _columnKeySet) && N.equals(other._columnList, _columnList);
    }

    /**
     * Returns a string representation of this Sheet.
     * <p>
     * The string contains the row keys, column keys, and all column data in a structured format.
     * This is primarily useful for debugging and logging purposes.
     * </p>
     *
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * 
     * String repr = sheet.toString();
     * // Returns: {rowKeySet=[row1, row2], columnKeySet=[col1, col2], columns={col1=[1, 3], col2=[2, 4]}}
     * }</pre>
     *
     * @return a string representation of this Sheet
     * @see #println()
     */
    @Override
    public String toString() {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append("{rowKeySet=");
            sb.append(_rowKeySet);
            sb.append(", columnKeySet=");
            sb.append(_columnKeySet);
            sb.append(", columns={");

            if (_isInitialized) {
                final Iterator<C> iter = _columnKeySet.iterator();

                for (int i = 0, columnLength = columnLength(); i < columnLength; i++) {
                    if (i > 0) {
                        sb.append(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    sb.append(iter.next()).append("=").append(N.toString(_columnList.get(i)));
                }
            }

            sb.append("}}");

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    private void init() {
        if (!_isInitialized) {
            initIndexMap();

            final int rowLength = rowLength();
            final int columnLength = columnLength();
            _columnList = new ArrayList<>(columnLength);

            for (int i = 0; i < columnLength; i++) {
                final List<V> column = new ArrayList<>(rowLength);
                N.fill(column, 0, rowLength, null);
                _columnList.add(column);
            }

            _isInitialized = true;
        }
    }

    private void initIndexMap() {
        if (_rowKeyIndexMap == null) {
            final int rowLength = rowLength();
            _rowKeyIndexMap = N.newBiMap(rowLength);
            int index = 0;
            for (final R rowKey : _rowKeySet) {
                _rowKeyIndexMap.put(rowKey, index++);
            }
        }

        if (_columnKeyIndexMap == null) {
            final int columnLength = columnLength();
            _columnKeyIndexMap = N.newBiMap(columnLength);
            int index = 0;
            for (final C columnKey : _columnKeySet) {
                _columnKeyIndexMap.put(columnKey, index++);
            }
        }
    }

    /**
     * Checks if the provided rowKey is a valid key for a row in the Sheet.
     * The rowKey is valid if it exists in the Sheet's row keys.
     *
     * @param rowKey the key of the row to be checked
     * @throws IllegalArgumentException if the rowKey does not exist in the Sheet
     */
    private void checkRowKey(final R rowKey) throws IllegalArgumentException {
        if (!_rowKeySet.contains(rowKey)) {
            throw new IllegalArgumentException("No row found by key: " + rowKey);
        }
    }

    /**
     * Checks if the provided columnKey is a valid key for a column in the Sheet.
     * The columnKey is valid if it exists in the Sheet's column keys.
     *
     * @param columnKey the key of the column to be checked
     * @throws IllegalArgumentException if the columnKey does not exist in the Sheet
     */
    private void checkColumnKey(final C columnKey) throws IllegalArgumentException {
        if (!_columnKeySet.contains(columnKey)) {
            throw new IllegalArgumentException("No column found by key: " + columnKey);
        }
    }

    /**
     * Checks if the provided rowIndex is a valid index for a row in the Sheet.
     * The rowIndex is valid if it is greater than or equal to 0 and less than the number of rows in the Sheet.
     *
     * @param rowIndex the index of the row to be checked
     * @throws IndexOutOfBoundsException if the rowIndex is not a valid index for a row in the Sheet
     */
    private void checkRowIndex(final int rowIndex) throws IndexOutOfBoundsException {
        if (rowIndex < 0 || rowIndex >= rowLength()) {
            throw new IndexOutOfBoundsException("Row index " + rowIndex + " is out-of-bounds for row size " + rowLength());
        }
    }

    /**
     * Checks if the provided fromRowIndex and toRowIndex are valid indices for rows in the Sheet.
     * The fromRowIndex and toRowIndex are valid if they are greater than or equal to 0,
     * fromRowIndex is less than or equal to toRowIndex, and toRowIndex is less than the specified length.
     *
     * @param fromRowIndex the starting index of the row range to be checked
     * @param toRowIndex the ending index of the row range to be checked
     * @param len the total length of the row range
     * @throws IndexOutOfBoundsException if the fromRowIndex and toRowIndex are not valid indices for rows in the Sheet
     */
    private void checkRowFromToIndex(final int fromRowIndex, final int toRowIndex, final int len) throws IndexOutOfBoundsException {
        if (fromRowIndex < 0 || fromRowIndex > toRowIndex || toRowIndex > len) {
            throw new IndexOutOfBoundsException("Row index range [" + fromRowIndex + ", " + toRowIndex + "] is out-of-bounds for row size" + len);
        }
    }

    /**
     * Checks if the provided columnIndex is a valid index for a column in the Sheet.
     * The columnIndex is valid if it is greater than or equal to 0 and less than the number of columns in the Sheet.
     *
     * @param columnIndex the index of the column to be checked
     * @throws IndexOutOfBoundsException if the columnIndex is not a valid index for a column in the Sheet
     */
    private void checkColumnIndex(final int columnIndex) throws IndexOutOfBoundsException {
        if (columnIndex < 0 || columnIndex >= columnLength()) {
            throw new IndexOutOfBoundsException("Column index " + columnIndex + " is out-of-bounds for column size " + columnLength());
        }
    }

    /**
     * Checks if the provided fromColumnIndex and toColumnIndex are valid indices for columns in the Sheet.
     * The fromColumnIndex and toColumnIndex are valid if they are greater than or equal to 0,
     * fromColumnIndex is less than or equal to toColumnIndex, and toColumnIndex is less than the specified length.
     *
     * @param fromColumnIndex the starting index of the column range to be checked
     * @param toColumnIndex the ending index of the column range to be checked
     * @param len the total length of the column range
     * @throws IndexOutOfBoundsException if the fromColumnIndex and toColumnIndex are not valid indices for columns in the Sheet
     */
    private void checkColumnFromToIndex(final int fromColumnIndex, final int toColumnIndex, final int len) throws IndexOutOfBoundsException {
        if (fromColumnIndex < 0 || fromColumnIndex > toColumnIndex || toColumnIndex > len) {
            throw new IndexOutOfBoundsException("Column index range [" + fromColumnIndex + ", " + toColumnIndex + "] is out-of-bounds for column size" + len);
        }
    }

    /**
     * Retrieves the index of a row in the Sheet.
     * The row is identified by the provided row key.
     *
     * @param rowKey the key of the row
     * @return the index of the row
     * @throws IllegalArgumentException if the row key does not exist in the Sheet
     */
    private int getRowIndex(final R rowKey) throws IllegalArgumentException {
        if (_rowKeyIndexMap == null) {
            this.initIndexMap();
        }

        final Integer index = _rowKeyIndexMap.get(rowKey);

        if (index == null) {
            throw new IllegalArgumentException("No row found by key: " + rowKey);
        }

        return index;
    }

    /**
     * Retrieves the index of a column in the Sheet.
     * The column is identified by the provided column key.
     *
     * @param columnKey the key of the column
     * @return the index of the column
     * @throws IllegalArgumentException if the column key does not exist in the Sheet
     */
    private int getColumnIndex(final C columnKey) throws IllegalArgumentException {
        if (_columnKeyIndexMap == null) {
            this.initIndexMap();
        }

        final Integer index = _columnKeyIndexMap.get(columnKey);

        if (index == null) {
            throw new IllegalArgumentException("No column found by key: " + columnKey);
        }

        return index;
    }

    /**
     * Checks if the Sheet is frozen.
     * If the Sheet is frozen, an IllegalStateException is thrown.
     * A Sheet is considered frozen if it has been marked as unmodifiable.
     *
     * @throws IllegalStateException if the Sheet is frozen
     */
    private void checkFrozen() throws IllegalStateException {
        if (_isFrozen) {
            throw new IllegalStateException("This Sheet is frozen, can't modify it.");
        }
    }

    /**
     * A record representing a cell in the Sheet.
     * A cell is identified by a row key of type {@code R}, a column key of type {@code C}, and contains a value of type {@code V}.
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKey the key of the row
     * @param columnKey the key of the column
     * @param value the value stored in the cell
     */
    public record Cell<R, C, V>(R rowKey, C columnKey, V value) {

        /**
         * Creates a new Cell with the specified row key, column key, and value.
         *
         * @param rowKey the key of the row
         * @param columnKey the key of the column
         * @param value the value stored in the cell
         * @return a new Cell with the specified row key, column key, and value
         */
        public static <R, C, V> Cell<R, C, V> of(final R rowKey, final C columnKey, final V value) {
            return new Cell<>(rowKey, columnKey, value);
        }
    }

    /**
     * A record representing a point in a two-dimensional space, such as a cell in a Sheet.
     * A point is identified by a rowIndex and a columnIndex.
     *
     * @param rowIndex the index of the row
     * @param columnIndex the index of the column
     */
    public record Point(int rowIndex, int columnIndex) {

        private static final int MAX_CACHE_SIZE = 128;
        private static final Point[][] CACHE = new Point[MAX_CACHE_SIZE][MAX_CACHE_SIZE];

        static {
            for (int i = 0; i < MAX_CACHE_SIZE; i++) {
                for (int j = 0; j < MAX_CACHE_SIZE; j++) {
                    CACHE[i][j] = new Point(i, j);
                }
            }
        }

        public static final Point ZERO = CACHE[0][0];

        /**
         * Creates a new Point with the specified row index and column index.
         *
         * @param rowIndex the index of the row
         * @param columnIndex the index of the column
         * @return a new Point with the specified row index and column index
         */
        public static Point of(final int rowIndex, final int columnIndex) {
            if (rowIndex < MAX_CACHE_SIZE && columnIndex < MAX_CACHE_SIZE) {
                return CACHE[rowIndex][columnIndex];
            }

            return new Point(rowIndex, columnIndex);
        }
    }
}
