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

package com.landawn.abacus.util;

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
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBiPredicate;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * This is the main class for the Sheet data structure.
 * A Sheet is a two-dimensional table that can store data in cells, each identified by a row key and a column key.
 * The row keys are of type {@code R}, the column keys are of type {@code C}, and the values stored in the cells are of type {@code V}.
 * This class implements the Cloneable interface, meaning it can create and return a copy of itself.
 * <li>
 * {@code R} = Row, {@code C} = Column, {@code H} = Horizontal, {@code V} = Vertical.
 * </li>
 *
 * @param <R> the type of the row keys
 * @param <C> the type of the column keys
 * @param <V> the type of the values stored in the cells
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
     * Default constructor for the Sheet class.
     * Initializes an empty Sheet with no row keys and no column keys.
     */
    public Sheet() {
        this(N.emptyList(), N.emptyList());
    }

    /**
     * Constructs a new Sheet with the specified row keys and column keys.
     * The row keys and column keys are used to identify the cells in the Sheet.
     * The Sheet is initially empty, meaning it contains no values.
     *
     * @param rowKeySet the collection of row keys for the Sheet
     * @param columnKeySet the collection of column keys for the Sheet
     * @throws IllegalArgumentException if any of the row keys or column keys are null
     */
    public Sheet(final Collection<R> rowKeySet, final Collection<C> columnKeySet) throws IllegalArgumentException {
        N.checkArgument(!N.anyNull(rowKeySet), "Row key can't be null");
        N.checkArgument(!N.anyNull(columnKeySet), "Column key can't be null");

        _rowKeySet = N.newLinkedHashSet(rowKeySet);
        _columnKeySet = N.newLinkedHashSet(columnKeySet);
    }

    /**
     * Constructs a new Sheet with the specified row keys, column keys, and initial data.
     * The row keys and column keys are used to identify the cells in the Sheet.
     * The initial data is provided as a two-dimensional array where each inner array represents a row of data.
     *
     * @param rowKeySet the collection of row keys for the Sheet
     * @param columnKeySet the collection of column keys for the Sheet
     * @param rows the initial data for the Sheet, where each inner array represents a row of data
     * @throws IllegalArgumentException if any of the row keys or column keys are {@code null}, or if the dimensions of the initial data do not match the provided row keys and column keys
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
     * Returns an empty immutable {@code Sheet}.
     * This method is useful when you need an empty Sheet for initialization purposes.
     * The returned Sheet is immutable, meaning no modifications (such as adding or removing rows/columns, or changing values) are allowed.
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @return an empty immutable {@code Sheet}
     */
    public static <R, C, V> Sheet<R, C, V> empty() {
        return EMPTY_SHEET;
    }

    /**
     * Constructs a new Sheet with the specified row keys, column keys, and initial data.
     * The row keys and column keys are used to identify the cells in the Sheet.
     * The initial data is provided as a two-dimensional array where each inner array represents a row of data.
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet
     * @param columnKeySet the collection of column keys for the Sheet
     * @param rows the initial data for the Sheet, where each inner array represents a row of data
     * @return a new Sheet with the specified row keys, column keys, and initial data
     * @throws IllegalArgumentException if any of the row keys or column keys are {@code null}, or if the dimensions of the initial data do not match the provided row keys and column keys
     */
    public static <R, C, V> Sheet<R, C, V> rows(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final Object[][] rows)
            throws IllegalArgumentException {
        return new Sheet<>(rowKeySet, columnKeySet, rows);
    }

    /**
     * Constructs a new Sheet with the specified row keys, column keys, and initial data.
     * The row keys and column keys are used to identify the cells in the Sheet.
     * The initial data is provided as a collection of collections where each inner collection represents a row of data.
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet
     * @param columnKeySet the collection of column keys for the Sheet
     * @param rows the initial data for the Sheet, where each inner collection represents a row of data
     * @return a new Sheet with the specified row keys, column keys, and initial data
     * @throws IllegalArgumentException if any of the row keys or column keys are {@code null}, or if the dimensions of the initial data do not match the provided row keys and column keys
     */
    public static <R, C, V> Sheet<R, C, V> rows(final Collection<R> rowKeySet, final Collection<C> columnKeySet,
            final Collection<? extends Collection<? extends V>> rows) throws IllegalArgumentException {
        final Sheet<R, C, V> instance = new Sheet<>(rowKeySet, columnKeySet);

        final int rowLength = instance.rowLength();
        final int columnLength = instance.columnLength();

        if (N.notEmpty(rows)) {
            N.checkArgument(rows.size() == rowLength, "The size of collection is not equal to size of row/column key set"); //NOSONAR

            for (final Collection<? extends V> e : rows) {
                N.checkArgument(e.size() == columnLength, "The size of collection is not equal to size of row/column key set");
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
     * Constructs a new Sheet with the specified row keys, column keys, and initial data.
     * The row keys and column keys are used to identify the cells in the Sheet.
     * The initial data is provided as a two-dimensional array where each inner array represents a column of data.
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet
     * @param columnKeySet the collection of column keys for the Sheet
     * @param columns the initial data for the Sheet, where each inner array represents a column of data
     * @return a new Sheet with the specified row keys, column keys, and initial data
     * @throws IllegalArgumentException if any of the row keys or column keys are {@code null}, or if the dimensions of the initial data do not match the provided row keys and column keys
     */
    public static <R, C, V> Sheet<R, C, V> columns(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final Object[][] columns)
            throws IllegalArgumentException {
        final Sheet<R, C, V> instance = new Sheet<>(rowKeySet, columnKeySet);

        final int rowLength = instance.rowLength();
        final int columnLength = instance.columnLength();

        if (N.notEmpty(columns)) {
            N.checkArgument(columns.length == columnLength, "The length of array is not equal to size of row/column key set");

            for (final Object[] e : columns) {
                N.checkArgument(e.length == rowLength, "The length of array is not equal to size of row/column key set");
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
     * Constructs a new Sheet with the specified row keys, column keys, and initial data.
     * The row keys and column keys are used to identify the cells in the Sheet.
     * The initial data is provided as a collection of collections where each inner collection represents a column of data.
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet
     * @param columnKeySet the collection of column keys for the Sheet
     * @param columns the initial data for the Sheet, where each inner collection represents a column of data
     * @return a new Sheet with the specified row keys, column keys, and initial data
     * @throws IllegalArgumentException if any of the row keys or column keys are {@code null}, or if the dimensions of the initial data do not match the provided row keys and column keys
     */
    public static <R, C, V> Sheet<R, C, V> columns(final Collection<R> rowKeySet, final Collection<C> columnKeySet,
            final Collection<? extends Collection<? extends V>> columns) throws IllegalArgumentException {
        final Sheet<R, C, V> instance = new Sheet<>(rowKeySet, columnKeySet);

        final int rowLength = instance.rowLength();
        final int columnLength = instance.columnLength();

        if (N.notEmpty(columns)) {
            N.checkArgument(columns.size() == columnLength, "The size of collection is not equal to size of row/column key set");

            for (final Collection<? extends V> e : columns) {
                N.checkArgument(e.size() == rowLength, "The size of collection is not equal to size of row/column key set");
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
     * Returns the immutable set of row keys in the Sheet.
     * The row keys are used to identify the rows in the Sheet.
     *
     * @return an ImmutableSet of row keys
     */
    public ImmutableSet<R> rowKeySet() {
        return ImmutableSet.wrap(_rowKeySet);
    }

    /**
     * Returns the immutable set of column keys in the Sheet.
     * The column keys are used to identify the columns in the Sheet.
     *
     * @return a ImmutableSet of column keys
     */
    public ImmutableSet<C> columnKeySet() {
        return ImmutableSet.wrap(_columnKeySet);
    }

    /**
     * Retrieves the value stored in the cell identified by the specified row key and column key.
     *
     * @param rowKey the row key of the cell
     * @param columnKey the column key of the cell
     * @return the value stored in the cell, or {@code null} if the cell does not exist
     * @throws IllegalArgumentException if the specified rowKey or columnKey does not exist in the Sheet
     */
    @MayReturnNull
    public V get(final R rowKey, final C columnKey) throws IllegalArgumentException {
        if (_isInitialized) {
            final int rowIndex = getRowIndex(rowKey);
            final int columnIndex = getColumnIndex(columnKey);

            return get(rowIndex, columnIndex);
        } else {
            checkRowKey(rowKey);
            checkColumnKey(columnKey);

            return null;
        }
    }

    /**
     * Retrieves the value stored in the cell identified by the specified row index and column index.
     *
     * @param rowIndex the index of the row
     * @param columnIndex the index of the column
     * @return the value stored in the cell, or {@code null} if the cell does not exist
     * @throws IndexOutOfBoundsException if the specified indices are out of bounds
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
     * Retrieves the value stored in the cell identified by the specified Point point.
     * The Point point represents the row index and column index of the cell.
     *
     * @param point the Point point of the cell
     * @return the value stored in the cell, or {@code null} if the cell does not exist
     * @throws IndexOutOfBoundsException if the specified indices are out of bounds
     */
    @Beta
    public V get(final Point point) throws IndexOutOfBoundsException {
        return get(point.rowIndex, point.columnIndex);
    }

    /**
     * Inserts or updates a value in the cell identified by the specified row key and column key.
     * If the cell already contains a value, the existing value is replaced with the new value.
     * If the cell does not exist, a new cell is created with the specified keys and value.
     *
     * @param rowKey the row key of the cell
     * @param columnKey the column key of the cell
     * @param value the new value to be stored in the cell
     * @return the previous value stored in the cell
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the specified rowKey or columnKey does not exist in the Sheet
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
     * Inserts or updates a value in the cell identified by the specified row index and column index.
     * If the cell already contains a value, the existing value is replaced with the new value.
     * If the cell does not exist, a new cell is created with the specified indices and value.
     *
     * @param rowIndex the index of the row
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

        final Object previousValue = _columnList.get(columnIndex).get(rowIndex);
        _columnList.get(columnIndex).set(rowIndex, value);

        return (V) previousValue;
    }

    /**
     * Inserts or updates a value in the cell identified by the specified Point point.
     * The Point point represents the row index and column index of the cell.
     * If the cell already contains a value, the existing value is replaced with the new value.
     * If the cell does not exist, a new cell is created at the specified point with the provided value.
     *
     * @param point the Point point of the cell
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
     * Inserts or updates all values from the specified source Sheet into this Sheet.
     * The source Sheet must have the same or a subset of the row keys and column keys as this Sheet.
     * If a cell in this Sheet already contains a value, the existing value is replaced with the new value from the source Sheet.
     * If a cell does not exist, a new cell is created with the keys and value from the source Sheet.
     *
     * @param source the source Sheet from which to get the new values
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the source Sheet contains row keys or column keys that are not present in this Sheet
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

        for (final R r : tmp.rowKeySet()) {
            for (final C c : tmp.columnKeySet()) {
                // this.put(r, c, tmp.get(r, c));

                put(getRowIndex(r), getColumnIndex(c), tmp.get(r, c));
            }
        }
    }

    /**
     * Removes the value stored in the cell identified by the specified row key and column key.
     *
     * @param rowKey the row key of the cell
     * @param columnKey the column key of the cell
     * @return the value that was stored in the cell
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the specified rowKey or columnKey does not exist in the Sheet
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
     * Removes the value stored in the cell identified by the specified row index and column index.
     *
     * @param rowIndex the index of the row
     * @param columnIndex the index of the column
     * @return the value that was stored in the cell
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IndexOutOfBoundsException if the specified indices are out of bounds
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
     * Removes the value stored in the cell identified by the specified Point point.
     * The Point point represents the row index and column index of the cell.
     *
     * @param point the Point point of the cell
     * @return the value that was stored in the cell
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IndexOutOfBoundsException if the specified indices are out of bounds
     */
    @Beta
    public V remove(final Point point) throws IllegalStateException, IndexOutOfBoundsException {
        return remove(point.rowIndex, point.columnIndex);
    }

    /**
     * Checks if the Sheet contains a cell identified by the specified row key and column key.
     * If the cell exists, this method returns {@code true}. Otherwise, it returns {@code false}.
     *
     * @param rowKey the row key of the cell
     * @param columnKey the column key of the cell
     * @return {@code true} if the cell exists, {@code false} otherwise
     */
    public boolean contains(final R rowKey, final C columnKey) {
        return _rowKeySet.contains(rowKey) && _columnKeySet.contains(columnKey);
    }

    /**
     * Checks if the Sheet contains a cell identified by the specified row key, column key, and value.
     * If the cell exists and contains the specified value, this method returns {@code true}. Otherwise, it returns {@code false}.
     *
     * @param rowKey the row key of the cell
     * @param columnKey the column key of the cell
     * @param value the value to check in the cell
     * @return {@code true} if the cell exists and contains the specified value, {@code false} otherwise
     */
    public boolean contains(final R rowKey, final C columnKey, final Object value) {
        return N.equals(get(rowKey, columnKey), value);
    }

    /**
     * Checks if the Sheet contains a cell with the specified value.
     * If a cell with the specified value exists, this method returns {@code true}. Otherwise, it returns {@code false}.
     *
     * @param value the value to check in the cells
     * @return {@code true} if a cell with the specified value exists, {@code false} otherwise
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
     *
     * @param rowKey the row key of the row
     * @return an ImmutableList of values in the row
     * @throws IllegalArgumentException if the row key does not exist in the Sheet
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
     * The row is identified by the provided row key and the values are provided as a collection.
     * The order of the values in the collection should match the order of the column keys in the Sheet.
     *
     * @param rowKey the key of the row to be set
     * @param row the collection of values to be set in the row
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist in the Sheet or the provided collection is not empty and its size does not match the number of columns in the Sheet
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
     * Adds a new row to the Sheet.
     * The row is identified by the provided row key and the values are provided as a collection.
     * The order of the values in the collection should match the order of the column keys in the Sheet.
     *
     * @param rowKey the key of the row to be added
     * @param row the collection of values to be added in the row
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key already exists in the Sheet or the provided collection is not empty and its size does not match the number of columns in the Sheet
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
     * The row is identified by the provided row key and the values are provided as a collection.
     * The order of the values in the collection should match the order of the column keys in the Sheet.
     *
     * @param rowIndex the index at which the row should be inserted
     * @param rowKey the key of the row to be added
     * @param row the collection of values to be added in the row
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is less than zero or bigger than row size
     * @throws IllegalArgumentException if the row key already exists in the Sheet or the provided collection is not empty and its size does not match the number of columns in the Sheet
     */
    public void addRow(final int rowIndex, final R rowKey, final Collection<? extends V> row)
            throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();

        final int rowLength = rowLength();
        final int columnLength = columnLength();

        if (rowIndex == rowLength) {
            addRow(rowKey, row);
            return;
        }

        if (rowIndex < 0 || rowIndex > rowLength) {
            throw new IndexOutOfBoundsException("The new row index " + rowIndex + " is out-of-bounds for row size " + (rowLength + 1));
        }

        if (_rowKeySet.contains(rowKey)) {
            throw new IllegalArgumentException("Row '" + rowKey + "' already existed");
        }

        if (N.notEmpty(row) && row.size() != columnLength) {
            throw new IllegalArgumentException("The size of specified row: " + row.size() + " doesn't match the size of column key set: " + columnLength);
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
     * The function takes each value in the row as input and returns the updated value.
     *
     * @param rowKey the key of the row to be updated
     * @param func the function to apply to each value in the row
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist in the Sheet
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
                // removed last row.
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
     * The new position is specified by the new row index.
     *
     * @param rowKey the key of the row to be moved
     * @param newRowIndex the new index at which the row should be positioned
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist in the Sheet or the new index is out of bounds
     * @throws IndexOutOfBoundsException if the new row index is out of bounds
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
     * The rows are identified by the provided row keys.
     *
     * @param rowKeyA the key of the first row to be swapped
     * @param rowKeyB the key of the second row to be swapped
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the row keys do not exist in the Sheet
     */
    public void swapRows(final R rowKeyA, final R rowKeyB) throws IllegalStateException, IllegalArgumentException {
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
     * The row to be renamed is identified by the provided old row key and the new name is provided as the new row key.
     *
     * @param rowKey the old key of the row to be renamed
     * @param newRowKey the new key for the row
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the old row key does not exist in the Sheet or the new row key is already in use
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
     * If the row exists, this method returns {@code true}. Otherwise, it returns {@code false}.
     *
     * @param rowKey the row key to check
     * @return {@code true} if the row exists, {@code false} otherwise
     */
    public boolean containsRow(final R rowKey) {
        return _rowKeySet.contains(rowKey);
    }

    /**
     * Retrieves a map representing a row in the Sheet.
     * The row is identified by the provided row key.
     * The map's keys are the column keys and the values are the values stored in the cells of the row.
     *
     * @param rowKey the row key of the row
     * @return a Map where the keys are the column keys and the values are the values stored in the cells of the row
     * @throws IllegalArgumentException if the row key does not exist in the Sheet
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
     * Each entry in the map corresponds to a row in the Sheet.
     * The map's keys are the row keys and the values are maps where the keys are the column keys and the values are the values stored in the cells of the row.
     * An empty map is returned if the Sheet is empty.
     *
     * @return a Map where the keys are the row keys and the values are maps representing the rows. In these maps, the keys are the column keys and the values are the values stored in the cells of the row.
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
     *
     * @param columnKey the column key of the column
     * @return an ImmutableList of values in the column
     * @throws IllegalArgumentException if the column key does not exist in the Sheet
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
     * The column is identified by the provided column key and the values are provided as a collection.
     * The order of the values in the collection should match the order of the row keys in the Sheet.
     *
     * @param columnKey the key of the column to be set
     * @param column the collection of values to be set in the column
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist in the Sheet or the provided collection is not empty and its size does not match the number of rows in the Sheet
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
     * Adds a new column to the Sheet.
     * The column is identified by the provided column key and the values are provided as a collection.
     * The order of the values in the collection should match the order of the row keys in the Sheet.
     *
     * @param columnKey the key of the column to be added
     * @param column the collection of values to be added in the column
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the column key already exists in the Sheet or the provided collection is not empty and its size does not match the number of rows in the Sheet
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
     * The column is identified by the provided column key and the values are provided as a collection.
     * The order of the values in the collection should match the order of the row keys in the Sheet.
     *
     * @param columnIndex the index at which the column should be inserted
     * @param columnKey the key of the column to be added
     * @param column the collection of values to be added in the column
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IndexOutOfBoundsException if the specified {@code columnIndex} is less than zero or bigger than column size
     * @throws IllegalArgumentException if the column key already exists in the Sheet or the provided collection is not empty and its size does not match the number of rows in the Sheet
     */
    public void addColumn(final int columnIndex, final C columnKey, final Collection<? extends V> column)
            throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();

        final int rowLength = rowLength();
        final int columnLength = columnLength();

        if (columnIndex == columnLength) {
            addColumn(columnKey, column);
            return;
        }

        if (columnIndex < 0 || columnIndex > columnLength) {
            throw new IndexOutOfBoundsException("The new column index " + columnIndex + " is out-of-bounds for column size " + (columnLength + 1));
        }

        if (_columnKeySet.contains(columnKey)) {
            throw new IllegalArgumentException("Column '" + columnKey + "' already existed");
        }

        if (N.notEmpty(column) && column.size() != rowLength) {
            throw new IllegalArgumentException("The size of specified column: " + column.size() + " doesn't match the size of row key set: " + rowLength);
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
     * The function takes each value in the column as input and returns the updated value.
     *
     * @param columnKey the key of the column to be updated
     * @param func the function to apply to each value in the column
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist in the Sheet
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
     *
     * @param columnKey the key of the column to be removed
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist in the Sheet
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
     * The new position is specified by the new column index.
     *
     * @param columnKey the key of the column to be moved
     * @param newColumnIndex the new index at which the column should be positioned
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist
     * @throws IndexOutOfBoundsException if the new column index is out of bounds
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
     * Swaps the positions of two columns in the Sheet.
     * The columns are identified by the provided column keys.
     *
     * @param columnKeyA the key of the first column to be swapped
     * @param columnKeyB the key of the second column to be swapped
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the column keys do not exist in the Sheet
     */
    public void swapColumns(final C columnKeyA, final C columnKeyB) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndexA = getColumnIndex(columnKeyA);
        final int columnIndexB = getColumnIndex(columnKeyB);

        final List<C> tmp = new ArrayList<>(rowLength());
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
     * The column to be renamed is identified by the provided old column key and the new name is provided as the new column key.
     *
     * @param columnKey the old key of the column to be renamed
     * @param newColumnKey the new key for the column
     * @throws IllegalStateException if the Sheet is frozen
     * @throws IllegalArgumentException if the old column key does not exist in the Sheet or the new column key is already in use
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
     * If the column exists, this method returns {@code true}. Otherwise, it returns {@code false}.
     *
     * @param columnKey the column key to check
     * @return {@code true} if the column exists, {@code false} otherwise
     */
    public boolean containsColumn(final C columnKey) {
        return _columnKeySet.contains(columnKey);
    }

    /**
     * Retrieves a map representing a column in the Sheet.
     * The column is identified by the provided column key.
     * The map's keys are the row keys and the values are the values stored in the cells of the column.
     *
     * @param columnKey the column key of the column
     * @return a Map where the keys are the row keys and the values are the values stored in the cells of the column
     * @throws IllegalArgumentException if the column key does not exist in the Sheet
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
     * Each entry in the map corresponds to a column in the Sheet.
     * The map's keys are the column keys and the values are maps where the keys are the row keys and the values are the values stored in the cells of the column.
     * An empty map is returned if the Sheet is empty.
     *
     * @return a Map where the keys are the column keys and the values are maps representing the columns. In these maps, the keys are the row keys and the values are the values stored in the cells of the column.
     */
    public Map<C, Map<R, V>> columnMap() {
        final Map<C, Map<R, V>> result = N.newLinkedHashMap(this.columnKeySet().size());

        for (final C columnKey : this.columnKeySet()) {
            result.put(columnKey, column(columnKey));
        }

        return result;
    }

    /**
     * Retrieves the number of rows in the Sheet.
     *
     * @return the number of rows in the Sheet
     */
    public int rowLength() {
        return _rowKeySet.size();
    }

    /**
     * Retrieves the number of columns in the Sheet.
     *
     * @return the number of columns in the Sheet
     */
    public int columnLength() {
        return _columnKeySet.size();
    }

    // TODO should the method name be "replaceAll"? If change the method name to replaceAll, what about updateColumn/updateRow?
    /**
     * Updates all values in the sheet using the provided function.
     * The function takes each value in the sheet as input and returns the updated value.
     *
     * @param func The function to apply to each value in the sheet. It takes a value from the sheet as input and returns the updated value.
     * @throws IllegalStateException if the sheet is frozen (read-only).
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
     * Updates all values in the sheet using the provided function.
     * The function takes two integers as input, representing the row and column indices of each value in the sheet, and returns the updated value.
     *
     * @param func The function to apply to each value in the sheet. It takes the row and column indices of a value from the sheet as input and returns the updated value.
     * @throws IllegalStateException if the sheet is frozen (read-only).
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
     * Updates all values in the sheet using the provided function.
     * The function takes three inputs: the row key, the column key, and the current value at that position in the sheet.
     * It returns the updated value which is then set at the corresponding position in the sheet.
     *
     * @param func The function to apply to each value in the sheet. It takes the row key, column key, and a value from the sheet as input and returns the updated value.
     * @throws IllegalStateException if the sheet is frozen (read-only).
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
     * Replaces all values in the sheet that satisfy the provided predicate with the new value.
     * The predicate takes each value in the sheet as input and returns a boolean indicating whether the value should be replaced.
     *
     * @param predicate The predicate to test each value in the sheet. It takes a value from the sheet as input and returns a boolean indicating whether the value should be replaced.
     * @param newValue The new value to replace the old values that satisfy the predicate.
     * @throws IllegalStateException if the sheet is frozen (read-only).
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
     * Replaces all values in the sheet that satisfy the provided predicate with the new value.
     * The predicate takes two integers as input, representing the row and column indices of each value in the sheet, and returns a boolean indicating whether the value should be replaced.
     *
     * @param predicate The predicate to test each value in the sheet. It takes the row and column indices of a value from the sheet as input and returns a boolean indicating whether the value should be replaced.
     * @param newValue The new value to replace the old values that satisfy the predicate.
     * @throws IllegalStateException if the sheet is frozen (read-only).
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
     * Replaces all values in the sheet that satisfy the provided predicate with the new value.
     * The predicate takes three inputs: the row key, the column key, and the current value at that position in the sheet.
     * It returns a boolean indicating whether the value should be replaced.
     *
     * @param predicate The predicate to test each value in the sheet. It takes the row key, column key, and a value from the sheet as input and returns a boolean indicating whether the value should be replaced.
     * @param newValue The new value to replace the old values that satisfy the predicate.
     * @throws IllegalStateException if the sheet is frozen (read-only).
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
     * Sorts the rows in the sheet based on the natural ordering of the row keys.
     * The natural ordering is the ordering imposed by the objects' own compareTo method.
     *
     * @throws ClassCastException if the row keys' class does not implement Comparable, or if comparing two row keys throws a ClassCastException.
     * @throws IllegalStateException if the sheet is frozen (read-only).
     */
    public void sortByRowKey() throws IllegalStateException {
        sortByRowKey((Comparator<R>) Comparator.naturalOrder());
    }

    /**
     * Sorts the rows in the sheet based on the row keys using the provided comparator.
     * The comparator takes two row keys as input and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     *
     * @param cmp The comparator to determine the order of the row keys. It takes two row keys as input and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     * @throws IllegalStateException if the sheet is frozen (read-only).
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

        final boolean indexedMapIntialized = N.notEmpty(_rowKeyIndexMap);
        _rowKeySet.clear();

        for (int i = 0; i < rowLength; i++) {
            _rowKeySet.add(arrayOfPair[i].value());

            if (indexedMapIntialized) {
                _rowKeyIndexMap.forcePut(arrayOfPair[i].value(), i);
            }
        }
    }

    /**
     * Sorts the rows in the sheet based on the values in the specified row using the provided comparator.
     * The comparator takes two values from the row as input and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     *
     * @param rowKey The key of the row based on whose values the rows will be sorted.
     * @param cmp The comparator to determine the order of the values in the specified row. It takes two values from the row as input and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     * @throws IllegalStateException if the sheet is frozen (read-only).
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

        final boolean indexedMapIntialized = N.notEmpty(_columnKeyIndexMap);
        final Object[] columnKeys = _columnKeySet.toArray(new Object[columnLength]);
        C columnKey = null;
        _columnKeySet.clear();

        for (int i = 0; i < columnLength; i++) {
            columnKey = (C) columnKeys[arrayOfPair[i].index()];
            _columnKeySet.add(columnKey);

            if (indexedMapIntialized) {
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
     * Sorts the rows in the sheet based on the values in the specified rows using the provided comparator.
     * The comparator takes two arrays of objects as input, each array representing a row in the sheet, and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     *
     * @param rowKeysToSort The keys of the rows based on whose values the rows will be sorted.
     * @param cmp The comparator to determine the order of the rows. It takes two arrays of objects, each representing a row in the sheet, as input and returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     * @throws IllegalStateException if the sheet is frozen (read-only).
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

        final boolean indexedMapIntialized = N.notEmpty(_columnKeyIndexMap);
        final Object[] columnKeys = _columnKeySet.toArray(new Object[columnLength]);
        C columnKey = null;
        _columnKeySet.clear();

        for (int i = 0; i < columnLength; i++) {
            columnKey = (C) columnKeys[arrayOfPair[i].index()];
            _columnKeySet.add(columnKey);

            if (indexedMapIntialized) {
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

        final boolean indexedMapIntialized = N.notEmpty(_columnKeyIndexMap);
        _columnKeySet.clear();

        for (int i = 0; i < columnLength; i++) {
            _columnKeySet.add(arrayOfPair[i].value());

            if (indexedMapIntialized) {
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

        final boolean indexedMapIntialized = N.notEmpty(_rowKeyIndexMap);
        final Object[] rowkeys = _rowKeySet.toArray(new Object[rowLength]);
        R rowKey = null;
        _rowKeySet.clear();

        for (int i = 0; i < rowLength; i++) {
            rowKey = (R) rowkeys[arrayOfPair[i].index()];
            _rowKeySet.add(rowKey);

            if (indexedMapIntialized) {
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

        final boolean indexedMapIntialized = N.notEmpty(_rowKeyIndexMap);
        final Object[] rowkeys = _rowKeySet.toArray(new Object[rowLength]);
        R rowKey = null;
        _rowKeySet.clear();

        for (int i = 0; i < rowLength; i++) {
            rowKey = (R) rowkeys[arrayOfPair[i].index()];
            _rowKeySet.add(rowKey);

            if (indexedMapIntialized) {
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
            throw new RuntimeException("Kryo is required");
        }

        final Sheet<R, C, V> copy = kryoParser.clone(this);

        copy._isFrozen = freeze;

        return copy;
    }

    /**
     * Merges the current Sheet with another Sheet.
     * This method creates a new Sheet object and initializes it with the row keys and column keys from both Sheets.
     * The values in the new Sheet are determined by applying the provided merge function to each pair of values that share the same row key and column key in the two original Sheets.
     * If a key is present in one Sheet but not the other, the merge function is applied with a {@code null} value for the missing Sheet.
     * The merge function takes two values as input (one from each Sheet) and returns the merged value.
     *
     * @param <U> The type of the values in the other Sheet.
     * @param <X> The type of the values in the new merged Sheet.
     * @param b The other Sheet to be merged with the current Sheet.
     * @param mergeFunction The function to apply to each pair of values in the two Sheets. It takes two values as input (one from each Sheet) and returns the merged value.
     * @return A new Sheet object that is the result of merging the current Sheet with the other Sheet.
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
        }

        idx = 0;

        for (final C columnKey : newColumnKeySet) {
            columnIndexes1[idx] = this.containsColumn(columnKey) ? this.getColumnIndex(columnKey) : -1;
            columnIndexes2[idx] = sheetB.containsColumn(columnKey) ? sheetB.getColumnIndex(columnKey) : -1;
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
     * Transposes the current Sheet object.
     * This method creates a new Sheet object with the row keys and column keys of the current Sheet swapped.
     * The values in the new Sheet are the same as the current Sheet but with the row and column indices swapped.
     * Changes to the transposed Sheet will not affect the current Sheet, and vice versa.
     *
     * @return A new Sheet object that is the transpose of the current Sheet.
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
     * Sets the Sheet to a frozen (read-only) state.
     * Once a Sheet is frozen, no modifications (such as adding or removing rows/columns, or changing values) are allowed.
     */
    public void freeze() {
        _isFrozen = true;
    }

    /**
     * Checks if the Sheet is frozen (read-only).
     *
     * @return {@code true} if the Sheet is frozen, {@code false} otherwise.
     */
    public boolean isFrozen() {
        return _isFrozen;
    }

    /**
     * Clears the content of the Sheet.
     * This method removes all values from the Sheet and trims the capacity of each column to its current size.
     * This operation is not allowed if the Sheet is frozen (read-only).
     *
     * @throws IllegalStateException if the Sheet is frozen (read-only).
     */
    public void clear() throws IllegalStateException {
        checkFrozen();

        if (_isInitialized && _columnList.size() > 0) {
            for (final List<V> column : _columnList) {
                N.fill(column, 0, column.size(), null);
            }
        }
    }

    /**
     * Trims the capacity of each column in the Sheet to be the column's current size.
     * This method can be used to minimize the storage of a Sheet instance.
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
     * Counts and returns the number of non-null values in the Sheet.
     * This method iterates over all the values in the Sheet and increments a counter for each non-null value.
     * The count of non-null values is then returned.
     *
     * @return A long value representing the count of non-null values in the Sheet.
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
     * Checks if the Sheet is empty.
     * A Sheet is considered empty if its row key set or column key set is empty.
     *
     * @return {@code true} if the Sheet is empty, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return _rowKeySet.isEmpty() || _columnKeySet.isEmpty();
    }

    // This should not be a public method. It's implementation detail.
    //    /**
    //     * Checks if the Sheet has been initialized.
    //     * A Sheet is considered initialized if it has been populated with data.
    //     *
    //     * @return true if the Sheet is initialized, false otherwise.
    //     */
    //    public boolean isInitialized() {
    //        return _isInitialized;
    //    }

    /**
     * Performs the given action for each cell in the Sheet.
     * The action is a TriConsumer that takes three inputs: the row key, the column key, and the value at that position in the Sheet.
     * The action is performed in the order of row by row in the Sheet.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param action The action to be performed for each cell in the Sheet. It takes the row key, column key, and a value from the Sheet as input.
     * @throws E if the action throws an exception.
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
     * Performs the given action for each cell in the Sheet in a vertical manner.
     * The action is a TriConsumer that takes three inputs: the row key, the column key, and the value at that position in the Sheet.
     * The action is performed in the order of column by column in the Sheet.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param action The action to be performed for each cell in the Sheet. It takes the row key, column key, and a value from the Sheet as input.
     * @throws E if the action throws an exception.
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
     * Performs the given action for each non-null cell in the Sheet in a horizontal manner.
     * The action is a TriConsumer that takes three inputs: the row key, the column key, and the non-null value at that position in the Sheet.
     * The action is performed in the order of row by row in the Sheet.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param action The action to be performed for each non-null cell in the Sheet. It takes the row key, column key, and a non-null value from the Sheet as input.
     * @throws E if the action throws an exception.
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
     * Performs the given action for each non-null cell in the Sheet in a vertical manner.
     * The action is a TriConsumer that takes three inputs: the row key, the column key, and the non-null value at that position in the Sheet.
     * The action is performed in the order of column by column in the Sheet.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param action The action to be performed for each non-null cell in the Sheet. It takes the row key, column key, and a non-null value from the Sheet as input.
     * @throws E if the action throws an exception.
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
     * Returns a stream of cells in the Sheet in a horizontal manner.
     * The cells are ordered by rows, meaning the cells in the first row are followed by the cells in the second row, and so on.
     *
     * @return A Stream of Cell objects representing the cells in the Sheet, ordered by rows.
     */
    public Stream<Sheet.Cell<R, C, V>> cellsH() {
        return cellsH(0, rowLength());
    }

    /**
     * Returns a stream of cells in the specified row in the Sheet.
     *
     * @param rowIndex The index of the row from which the stream should start.
     * @return A stream of cells in the specified row in the Sheet.
     * @throws IndexOutOfBoundsException if the rowIndex is out of the range of the Sheet's row indices.
     */
    public Stream<Sheet.Cell<R, C, V>> cellsH(final int rowIndex) throws IndexOutOfBoundsException {
        return cellsH(rowIndex, rowIndex + 1);
    }

    /**
     * Returns a stream of cells in the Sheet in a horizontal manner starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     * The cells are ordered by rows, meaning the cells in the specified row are followed by the cells in the subsequent rows up to the end row index.
     *
     * @param fromRowIndex The index of the row from which the stream should start.
     * @param toRowIndex The index of the row at which the stream should end.
     * @return A Stream of Cell objects representing the cells in the Sheet, ordered by rows starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the range of the Sheet's row indices.
     */
    public Stream<Sheet.Cell<R, C, V>> cellsH(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        final int columnLength = columnLength();

        initIndexMap();

        return Stream.of(new ObjIteratorEx<Sheet.Cell<R, C, V>>() {
            private final long toIndex = toRowIndex * columnLength * 1L;
            private long cursor = fromRowIndex * columnLength * 1L;

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
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; //NOSONAR
            }
        });
    }

    /**
     * Returns a stream of cells in the Sheet in a vertical manner.
     * The cells are ordered by columns, meaning the cells in the first column are followed by the cells in the second column, and so on.
     *
     * @return A Stream of Cell objects representing the cells in the Sheet, ordered by columns.
     */
    public Stream<Sheet.Cell<R, C, V>> cellsV() {
        return cellsV(0, columnLength());
    }

    /**
     * Returns a stream of cells in the specified column in the Sheet.
     *
     * @param columnIndex The index of the column from which the stream should start.
     * @return A stream of cells in the specified column in the Sheet.
     * @throws IndexOutOfBoundsException if the columnIndex is out of the range of the Sheet's column indices.
     */
    public Stream<Sheet.Cell<R, C, V>> cellsV(final int columnIndex) throws IndexOutOfBoundsException {
        return cellsV(columnIndex, columnIndex + 1);
    }

    /**
     * Returns a stream of cells in the Sheet in a vertical manner starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     * The cells are ordered by columns, meaning the cells in the specified column are followed by the cells in the subsequent columns up to the end column index.
     *
     * @param fromColumnIndex The index of the column from which the stream should start.
     * @param toColumnIndex The index of the column at which the stream should end.
     * @return A Stream of Cell objects representing the cells in the Sheet, ordered by columns starting starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     * @throws IndexOutOfBoundsException if the fromColumnIndex or toColumnIndex is out of the range of the Sheet's column indices.
     */
    public Stream<Sheet.Cell<R, C, V>> cellsV(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        final int rowLength = rowLength();

        initIndexMap();

        return Stream.of(new ObjIteratorEx<Sheet.Cell<R, C, V>>() {
            private final long toIndex = toColumnIndex * rowLength * 1L;
            private long cursor = fromColumnIndex * rowLength * 1L;

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
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; //NOSONAR
            }
        });
    }

    /**
     * Returns a stream of streams of cells in the Sheet in a horizontal manner.
     * Each inner stream represents a row in the Sheet, and the outer stream represents the sequence of these rows.
     * The cells in each row are ordered by columns, meaning the cells in the first column are followed by the cells in the second column, and so on.
     *
     * @return A Stream of Stream of Cell objects representing the cells in the Sheet, ordered by rows.
     */
    public Stream<Stream<Cell<R, C, V>>> cellsR() {
        return cellsR(0, rowLength());
    }

    /**
     * Returns a stream of streams of cells in the Sheet in a horizontal manner starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     * Each inner stream represents a row in the Sheet, and the outer stream represents the sequence of these rows.
     * The cells in each row are ordered by columns, meaning the cells in the first column are followed by the cells in the subsequent columns up to the end row index.
     *
     * @param fromRowIndex The index of the row from which the stream should start.
     * @param toRowIndex The index of the row at which the stream should end.
     * @return A Stream of Stream of Cell objects representing the cells in the Sheet, ordered by rows starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the range of the Sheet's row indices.
     */
    public Stream<Stream<Cell<R, C, V>>> cellsR(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        final int columnLength = columnLength();

        return Stream.of(new ObjIteratorEx<Stream<Cell<R, C, V>>>() {
            private volatile int rowIndex = fromRowIndex;

            @Override
            public boolean hasNext() {
                return rowIndex < toRowIndex;
            }

            @Override
            public Stream<Cell<R, C, V>> next() {
                if (rowIndex >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Stream.of(new ObjIteratorEx<Cell<R, C, V>>() {
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
                    public void advance(final long n) throws IllegalArgumentException {
                        N.checkArgNotNegative(n, cs.n);

                        columnIndex = n < columnLength - columnIndex ? columnIndex + (int) n : columnLength;
                    }

                    @Override
                    public long count() {
                        return columnLength - columnIndex; //NOSONAR
                    }
                });
            }

            @Override
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                rowIndex = n < toRowIndex - rowIndex ? rowIndex + (int) n : toRowIndex;
            }

            @Override
            public long count() {
                return toRowIndex - rowIndex; //NOSONAR
            }
        });
    }

    /**
     * Returns a stream of streams of cells in the Sheet in a vertical manner.
     * Each inner stream represents a column in the Sheet, and the outer stream represents the sequence of these columns.
     * The cells in each column are ordered by rows, meaning the cells in the first row are followed by the cells in the subsequent rows.
     *
     * @return A Stream of Stream of Cell objects representing the cells in the Sheet, ordered by columns.
     */
    public Stream<Stream<Cell<R, C, V>>> cellsC() {
        return cellsC(0, columnLength());
    }

    /**
     * Returns a stream of streams of cells in the Sheet in a vertical manner starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     * Each inner stream represents a column in the Sheet, and the outer stream represents the sequence of these columns.
     * The cells in each column are ordered by rows, meaning the cells in the first row are followed by the cells in the subsequent rows up to the end column index.
     *
     * @param fromColumnIndex The index of the column from which the stream should start.
     * @param toColumnIndex The index of the column at which the stream should end.
     * @return A Stream of Stream of Cell objects representing the cells in the Sheet, ordered by columns starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     * @throws IndexOutOfBoundsException if the fromColumnIndex or toColumnIndex is out of the range of the Sheet's column indices.
     */
    public Stream<Stream<Cell<R, C, V>>> cellsC(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        final int rowLength = rowLength();

        return Stream.of(new ObjIteratorEx<Stream<Cell<R, C, V>>>() {
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

                    return IntStream.range(0, rowLength).mapToObj(rowIndex -> Cell.of(_rowKeyIndexMap.getByValue(rowIndex), c, column.get(rowIndex)));
                } else {
                    return IntStream.range(0, rowLength).mapToObj(rowIndex -> Cell.of(_rowKeyIndexMap.getByValue(rowIndex), c, null));
                }

            }

            @Override
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                columnIndex = n < toColumnIndex - columnIndex ? columnIndex + (int) n : toColumnIndex;
            }

            @Override
            public long count() {
                return toColumnIndex - columnIndex; //NOSONAR
            }

        });
    }

    /**
     * Returns a stream of Point objects representing the points in the Sheet in a horizontal manner.
     * Each Point represents a point in the Sheet with its row and column indices.
     * The points are ordered by rows, meaning the points in the first row are followed by the points in the second row, and so on.
     *
     * @return A Stream of Point objects representing the points in the Sheet, ordered by rows.
     */
    public Stream<Point> pointsH() {
        return pointsH(0, rowLength());
    }

    /**
     * Returns a stream of Point objects representing the points in the specified row in the Sheet.
     * Each Point represents a point in the Sheet with its row and column indices.
     * The points are ordered by columns, meaning the points in the first column are followed by the points in the subsequent columns.
     *
     * @param rowIndex The index of the row from which the stream should start.
     * @return A Stream of Point objects representing the points in the specified row in the Sheet.
     * @throws IndexOutOfBoundsException if the rowIndex is out of the range of the Sheet's row indices
     */
    public Stream<Point> pointsH(final int rowIndex) throws IndexOutOfBoundsException {
        return pointsH(rowIndex, rowIndex + 1);
    }

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

    /**
     * Returns a stream of Point objects representing the points in the specified column in the Sheet.
     * Each Point represents a point in the Sheet with its row and column indices.
     * The points are ordered by rows, meaning the points in the first row are followed by the points in the subsequent rows.
     *
     * @param columnIndex The index of the column from which the stream should start.
     * @return A Stream of Point objects representing the points in the specified column in the Sheet.
     * @throws IndexOutOfBoundsException if the columnIndex is out of the range of the Sheet's column indices.
     */
    public Stream<Point> pointsV(final int columnIndex) throws IndexOutOfBoundsException {
        return pointsV(columnIndex, columnIndex + 1);
    }

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

        return IntStream.range(fromColumnIndex, toColumnIndex)
                .mapToObj(columnIndex -> IntStream.range(0, rowLength).mapToObj(rowIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of values in the Sheet in a horizontal manner.
     * The values are ordered by rows, meaning the values in the first row are followed by the values in the second row, and so on.
     *
     * @return A Stream of values representing the values in the Sheet, ordered by rows.
     */
    public Stream<V> streamH() {
        return streamH(0, rowLength());
    }

    /**
     * Returns a stream of values in the specified row in the Sheet.
     * The values are ordered by columns, meaning the values in the first column are followed by the values in the subsequent columns.
     *
     * @param rowIndex The index of the row from which the stream should start.
     * @return A Stream of values representing the values in the specified row in the Sheet.
     * @throws IndexOutOfBoundsException if the rowIndex is out of the range of the Sheet's row indices
     */
    public Stream<V> streamH(final int rowIndex) throws IndexOutOfBoundsException {
        return streamH(rowIndex, rowIndex + 1);
    }

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

        return Stream.of(new ObjIteratorEx<V>() {
            private final int columnLength = columnLength();
            private final long toIndex = toRowIndex * columnLength * 1L;
            private long cursor = fromRowIndex * columnLength * 1L;

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
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; //NOSONAR
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

    /**
     * Returns a stream of values in the specified column in the Sheet.
     * The values are ordered by rows, meaning the values in the first row are followed by the values in the subsequent rows.
     *
     * @param columnIndex The index of the column from which the stream should start.
     * @return A Stream of values representing the values in the specified column in the Sheet.
     * @throws IndexOutOfBoundsException if the columnIndex is out of the range of the Sheet's column indices.
     */
    public Stream<V> streamV(final int columnIndex) throws IndexOutOfBoundsException {
        return streamV(columnIndex, columnIndex + 1);
    }

    /**
     * Returns a stream of values in the Sheet in a vertical manner.
     * The values are ordered by columns, starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     * The values in each column are ordered by rows, meaning the values in the first row are followed by the values in the subsequent rows.
     *
     * @param fromColumnIndex The index of the column from which the stream should start.
     * @param toColumnIndex The index of the column at which the stream should end.
     * @return A Stream of values representing the values in the Sheet, ordered by columns starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     * @throws IndexOutOfBoundsException if the fromColumnIndex or toColumnIndex is out of the range of the Sheet's column indices.
     */
    public Stream<V> streamV(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<V>() {
            private final int rowLength = rowLength();
            private final long toIndex = toColumnIndex * rowLength * 1L;
            private long cursor = fromColumnIndex * rowLength * 1L;

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
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; //NOSONAR
            }
        });
    }

    /**
     * Returns a stream of streams of values in the Sheet in a horizontal manner.
     * Each inner stream represents a row in the Sheet, and the outer stream represents the sequence of these rows.
     * The values in each row are ordered by columns, meaning the values in the first column are followed by the values in the subsequent columns.
     *
     * @return A Stream of Stream of values representing the values in the Sheet, ordered by rows.
     */
    public Stream<Stream<V>> streamR() {
        return streamR(0, rowLength());
    }

    /**
     * Returns a stream of streams of values in the Sheet in a horizontal manner.
     * Each inner stream represents a row in the Sheet, and the outer stream represents the sequence of these rows.
     * The values in each row are ordered by columns, meaning the values in the first column are followed by the values in the subsequent columns.
     * The values are ordered by rows, starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     *
     * @param fromRowIndex The index of the row from which the stream should start.
     * @param toRowIndex The index of the row at which the stream should end.
     * @return A Stream of Stream of values representing the values in the Sheet, ordered by rows starting from the specified {@code fromRowIndex} and ending at the specified {@code toRowIndex}.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the range of the Sheet's row indices.
     */
    public Stream<Stream<V>> streamR(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<Stream<V>>() {
            private final int toIndex = toRowIndex;
            private volatile int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<V> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Stream.of(new ObjIteratorEx<V>() {
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
                    public void advance(final long n) throws IllegalArgumentException {
                        N.checkArgNotNegative(n, cs.n);

                        cursor2 = n < toIndex2 - cursor2 ? cursor2 + (int) n : toIndex2;
                    }

                    @Override
                    public long count() {
                        return toIndex2 - cursor2; //NOSONAR
                    }
                });
            }

            @Override
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; //NOSONAR
            }
        });
    }

    /**
     * Returns a stream of streams of values in the Sheet in a vertical manner.
     * Each inner stream represents a column in the Sheet, and the outer stream represents the sequence of these columns.
     * The values in each column are ordered by rows, meaning the values in the first row are followed by the values in the subsequent rows.
     *
     * @return A Stream of Stream of values representing the values in the Sheet, ordered by columns.
     */
    public Stream<Stream<V>> streamC() {
        return streamC(0, columnLength());
    }

    /**
     * Returns a stream of streams of values in the Sheet in a vertical manner.
     * Each inner stream represents a column in the Sheet, and the outer stream represents the sequence of these columns.
     * The values in each column are ordered by rows, meaning the values in the first row are followed by the values in the subsequent rows.
     * The values are ordered by columns, starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     *
     * @param fromColumnIndex The index of the column from which the stream should start.
     * @param toColumnIndex The index of the column at which the stream should end.
     * @return A Stream of Stream of values representing the values in the Sheet, ordered by columns starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     * @throws IndexOutOfBoundsException if the fromColumnIndex or toColumnIndex is out of the range of the Sheet's column indices.
     */
    public Stream<Stream<V>> streamC(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<Stream<V>>() {
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
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; //NOSONAR
            }
        });
    }

    /**
     * Returns a stream of pairs in the Sheet, where each pair consists of a row key and a stream of values in that row.
     * The pairs are ordered by rows, meaning the pair for the first row is followed by the pair for the second row, and so on.
     *
     * @return A Stream of Pair objects, where each Pair consists of a row key and a Stream of values in that row.
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

        return Stream.of(new ObjIteratorEx<Pair<R, Stream<V>>>() {
            private final int toIndex = toRowIndex;
            private volatile int cursor = fromRowIndex;

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

                final Stream<V> row = Stream.of(new ObjIteratorEx<V>() {
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
                    public void advance(final long n) throws IllegalArgumentException {
                        N.checkArgNotNegative(n, cs.n);

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
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; //NOSONAR
            }
        });
    }

    /**
     * Returns a stream of pairs in the Sheet, where each pair consists of a column key and a stream of values in that column.
     * The pairs are ordered by columns, meaning the pair for the first column is followed by the pair for the second column, and so on.
     *
     * @return A Stream of Pair objects, where each Pair consists of a column key and a Stream of values in that column.
     */
    public Stream<Pair<C, Stream<V>>> columns() {
        return columns(0, columnLength());
    }

    /**
     * Returns a stream of pairs in the Sheet, where each pair consists of a column key and a stream of values in that column.
     * Each pair in the stream represents a column in the Sheet, with the column key and a stream of values in that column.
     * The pairs are ordered by columns, starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     *
     * @param fromColumnIndex The index of the column from which the stream should start.
     * @param toColumnIndex The index of the column at which the stream should end.
     * @return A Stream of Pair objects, where each Pair consists of a column key and a Stream of values in that column, ordered by columns starting from the specified {@code fromColumnIndex} and ending at the specified {@code toColumnIndex}.
     * @throws IndexOutOfBoundsException if the fromColumnIndex or toColumnIndex is out of the range of the Sheet's column indices.
     */
    public Stream<Pair<C, Stream<V>>> columns(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnLength());

        if (rowLength() == 0 || columnLength() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<Pair<C, Stream<V>>>() {
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
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; //NOSONAR
            }
        });
    }

    /**
     * Converts the Sheet into a DataSet in a horizontal manner.
     * Each row in the Sheet becomes a row in the DataSet, with the column keys serving as the column names in the DataSet.
     * The DataSet is ordered by rows, meaning the first row in the Sheet becomes the first row in the DataSet, and so on.
     *
     * @return A DataSet object representing the data in the Sheet, ordered by rows.
     */
    public DataSet toDataSetH() {
        final int rowLength = rowLength();
        final int columnLength = columnLength();
        final List<String> dataSetColumnNameList = new ArrayList<>(columnLength);

        for (final C columnKey : _columnKeySet) {
            dataSetColumnNameList.add(N.toString(columnKey));
        }

        final List<List<Object>> dataSetColumnList = new ArrayList<>(columnLength);

        if (_isInitialized) {
            for (final List<V> column : _columnList) {
                dataSetColumnList.add(new ArrayList<>(column));
            }
        } else {
            for (int i = 0; i < columnLength; i++) {
                final List<Object> column = new ArrayList<>(rowLength);
                N.fill(column, 0, rowLength, null);
                dataSetColumnList.add(column);
            }
        }

        return new RowDataSet(dataSetColumnNameList, dataSetColumnList);
    }

    /**
     * Converts the Sheet into a DataSet in a vertical manner.
     * Each column in the Sheet becomes a row in the DataSet, with the row keys serving as the column names in the DataSet.
     * The DataSet is ordered by columns, meaning the first column in the Sheet becomes the first row in the DataSet, and so on.
     *
     * @return A DataSet object representing the data in the Sheet, ordered by columns.
     */
    public DataSet toDataSetV() {
        final int rowLength = rowLength();
        final int columnLength = columnLength();
        final List<String> dataSetColumnNameList = new ArrayList<>(rowLength);

        for (final R rowKey : _rowKeySet) {
            dataSetColumnNameList.add(N.toString(rowKey));
        }

        final List<List<Object>> dataSetColumnList = new ArrayList<>(rowLength);

        if (_isInitialized) {
            for (int i = 0; i < rowLength; i++) {
                final List<Object> column = new ArrayList<>(columnLength);

                for (int j = 0; j < columnLength; j++) {
                    column.add(_columnList.get(j).get(i));
                }

                dataSetColumnList.add(column);
            }
        } else {
            for (int i = 0; i < rowLength; i++) {
                final List<Object> column = new ArrayList<>(columnLength);
                N.fill(column, 0, columnLength, null);
                dataSetColumnList.add(column);
            }
        }

        return new RowDataSet(dataSetColumnNameList, dataSetColumnList);
    }

    /**
     * Converts the Sheet into a two-dimensional array in a horizontal manner.
     * Each row in the Sheet becomes a row in the array.
     * The array is ordered by rows, meaning the first row in the Sheet becomes the first row in the array, and so on.
     *
     * @return A two-dimensional array representing the data in the Sheet, ordered by rows.
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
     * Converts the Sheet into a two-dimensional array in a horizontal manner with a specified type.
     * Each row in the Sheet becomes a row in the array.
     * The array is ordered by rows, meaning the first row in the Sheet becomes the first row in the array, and so on.
     *
     * @param <T> The type of the elements in the array.
     * @param cls The Class object representing the type of the elements in the array.
     * @return A two-dimensional array of type T representing the data in the Sheet, ordered by rows.
     */
    public <T> T[][] toArrayH(final Class<T> cls) {
        final int rowLength = rowLength();
        final int columnLength = columnLength();
        final T[][] copy = N.newArray(N.newArray(cls, 0).getClass(), rowLength);

        for (int i = 0; i < rowLength; i++) {
            copy[i] = N.newArray(cls, columnLength);
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
     * Converts the Sheet into a two-dimensional array in a vertical manner.
     * Each column in the Sheet becomes a row in the array.
     * The array is ordered by columns, meaning the first column in the Sheet becomes the first row in the array, and so on.
     *
     * @return A two-dimensional array representing the data in the Sheet, ordered by columns.
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
     * Converts the Sheet into a two-dimensional array in a vertical manner with a specified type.
     * Each column in the Sheet becomes a row in the array.
     * The array is ordered by columns, meaning the first column in the Sheet becomes the first row in the array, and so on.
     *
     * @param <T> The type of the elements in the array.
     * @param cls The Class object representing the type of the elements in the array.
     * @return A two-dimensional array of type T representing the data in the Sheet, ordered by columns.
     */
    public <T> T[][] toArrayV(final Class<T> cls) {
        final int rowLength = rowLength();
        final int columnLength = columnLength();
        final T[][] copy = N.newArray(N.newArray(cls, 0).getClass(), columnLength);

        for (int i = 0; i < columnLength; i++) {
            copy[i] = N.newArray(cls, rowLength);
        }

        if (_isInitialized) {
            for (int i = 0; i < columnLength; i++) {
                _columnList.get(i).toArray(copy[i]);
            }
        }

        return copy;
    }

    /**
     * Applies the provided function to this Sheet and returns the result.
     * This method is useful for performing complex operations on the Sheet that are not covered by its built-in methods.
     *
     * @param <T> The type of the result produced by the function.
     * @param <E> The type of the exception that the function may throw.
     * @param func The function to apply to this Sheet. This function takes a Sheet as input and produces a result of type T.
     * @return The result produced by applying the function to this Sheet.
     * @throws E if the function throws an exception.
     */
    public <T, E extends Exception> T apply(final Throwables.Function<? super Sheet<R, C, V>, T, E> func) throws E {
        return func.apply(this);
    }

    /**
     * Applies the provided function to this Sheet if it is not empty and returns the result wrapped in an Optional.
     * This method is useful for performing complex operations on the Sheet that are not covered by its built-in methods.
     * If the Sheet is empty, it returns an empty Optional.
     *
     * @param <T> The type of the result produced by the function.
     * @param <E> The type of the exception that the function may throw.
     * @param func The function to apply to this Sheet. This function takes a Sheet as input and produces a result of type T.
     * @return An Optional containing the result produced by applying the function to this Sheet if it is not empty, or an empty Optional if the Sheet is empty.
     * @throws E if the function throws an exception.
     */
    public <T, E extends Exception> Optional<T> applyIfNotEmpty(final Throwables.Function<? super Sheet<R, C, V>, T, E> func) throws E {
        if (!isEmpty()) {
            return Optional.ofNullable(func.apply(this));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Accepts a consumer that operates on this Sheet.
     * This method is useful for performing complex operations on the Sheet that are not covered by its built-in methods.
     *
     * @param <E> The type of the exception that the consumer may throw.
     * @param action The consumer to apply to this Sheet. This consumer takes a Sheet as input.
     * @throws E if the consumer throws an exception.
     */
    public <E extends Exception> void accept(final Throwables.Consumer<? super Sheet<R, C, V>, E> action) throws E {
        action.accept(this);
    }

    /**
     * Accepts a consumer that operates on this Sheet if it is not empty.
     * This method is useful for performing complex operations on the Sheet that are not covered by its built-in methods.
     * If the Sheet is empty, the consumer is not applied and the method returns {@link OrElse#FALSE}.
     *
     * @param <E> The type of the exception that the consumer may throw.
     * @param action The consumer to apply to this Sheet. This consumer takes a Sheet as input.
     * @return {@link OrElse#TRUE} if the consumer was applied, or {@link OrElse#FALSE} if the Sheet is empty.
     * @throws E if the consumer throws an exception.
     */
    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super Sheet<R, C, V>, E> action) throws E {
        if (!isEmpty()) {
            action.accept(this);

            return OrElse.TRUE;
        }

        return OrElse.FALSE;
    }

    /**
     * Prints the content of the Sheet to the standard output.
     * The content is printed in a tabular format with row keys and column keys.
     * This method is useful for quickly inspecting the content of the Sheet during debugging.
     *
     * @throws UncheckedIOException if an I/O error occurs while printing the content of the Sheet.
     */
    public void println() throws UncheckedIOException {
        println(_rowKeySet, _columnKeySet);
    }

    /**
     * Prints the content of the Sheet to the standard output.
     * The content is printed in a tabular format with specified row keys and column keys.
     * This method is useful for quickly inspecting the content of the Sheet during debugging.
     *
     * @param rowKeySet The collection of row keys to be included in the output.
     * @param columnKeySet The collection of column keys to be included in the output.
     * @throws UncheckedIOException if an I/O error occurs while printing the content of the Sheet.
     */
    public void println(final Collection<R> rowKeySet, final Collection<C> columnKeySet) throws UncheckedIOException {
        println(rowKeySet, columnKeySet, IOUtil.newOutputStreamWriter(System.out)); // NOSONAR
    }

    /**
     * Prints the content of the Sheet to the provided Writer output.
     * The content is printed in a tabular format with row keys and column keys.
     * This method is useful for quickly inspecting the content of the Sheet during debugging.
     *
     * @param output The Writer to which the content of the Sheet will be printed.
     * @throws UncheckedIOException if an I/O error occurs while printing the content of the Sheet.
     */
    public void println(final Writer output) throws UncheckedIOException {
        println(_rowKeySet, _columnKeySet, output);
    }

    /**
     * Prints the content of the Sheet to the provided Writer output.
     * The content is printed in a tabular format with specified row keys and column keys.
     * This method is useful for quickly inspecting the content of the Sheet during debugging.
     *
     * @param rowKeySet The collection of row keys to be included in the output.
     * @param columnKeySet The collection of column keys to be included in the output.
     * @param output The Writer to which the content of the Sheet will be printed.
     * @throws IllegalArgumentException if the provided row keys or column keys are not included in this sheet.
     * @throws UncheckedIOException if an I/O error occurs while printing the content of the Sheet.
     */
    public void println(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final Writer output)
            throws IllegalArgumentException, UncheckedIOException {
        if (N.notEmpty(rowKeySet) && !_rowKeySet.containsAll(rowKeySet)) {
            throw new IllegalArgumentException("Row keys: " + N.difference(rowKeySet, _rowKeySet) + " are not included in this sheet row keys: " + _rowKeySet);
        }

        if (N.notEmpty(columnKeySet) && !_columnKeySet.containsAll(columnKeySet)) {
            throw new IllegalArgumentException(
                    "Column keys: " + N.difference(columnKeySet, _columnKeySet) + " are not included in this sheet Column keys: " + _columnKeySet);
        }

        N.checkArgNotNull(output, cs.output);

        final boolean isBufferedWriter = output instanceof BufferedWriter || output instanceof java.io.BufferedWriter;
        final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output);

        try {
            if (N.isEmpty(rowKeySet) && N.isEmpty(columnKeySet)) {
                bw.write("+---+");
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write("|   |");
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write("+---+");
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
                        bw.write(Strings.repeat(' ', maxColumnLens[i] + hchDelta + 1));
                    } else {
                        bw.write('+');

                        bw.write(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                    }
                }

                bw.write('+');

                bw.write(IOUtil.LINE_SEPARATOR);

                for (int i = 0; i < columnLen; i++) {
                    if (i == 0) {
                        bw.write("  ");
                    } else {
                        bw.write(" | ");
                    }

                    bw.write(Strings.padEnd(columnNameList.get(i), maxColumnLens[i]));
                }

                bw.write(" |");

                bw.write(IOUtil.LINE_SEPARATOR);

                for (int i = 0; i < columnLen; i++) {
                    bw.write('+');

                    if (i == 1 && N.isEmpty(columnKeySet)) {
                        bw.write(Strings.repeat(' ', maxColumnLens[i] + hchDelta));
                    } else {
                        bw.write(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                    }
                }

                bw.write('+');

                for (int j = 0; j < rowLen; j++) {
                    bw.write(IOUtil.LINE_SEPARATOR);

                    for (int i = 0; i < columnLen; i++) {
                        if (i == 0) {
                            bw.write("| ");
                        } else {
                            bw.write(" | ");
                        }

                        bw.write(Strings.padEnd(strColumnList.get(i).get(j), maxColumnLens[i]));
                    }

                    bw.write(" |");
                }

                if (rowLen == 0) {
                    bw.write(IOUtil.LINE_SEPARATOR);

                    for (int i = 0; i < columnLen; i++) {
                        if (i == 0) {
                            bw.write("| ");
                            bw.write(Strings.padEnd("", maxColumnLens[i]));
                        } else {
                            bw.write(Strings.padEnd("", maxColumnLens[i] + 3));
                        }
                    }

                    bw.write(" |");
                }

                bw.write(IOUtil.LINE_SEPARATOR);

                for (int i = 0; i < columnLen; i++) {
                    bw.write('+');

                    bw.write(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                }

                bw.write('+');
            }

            bw.write(IOUtil.LINE_SEPARATOR);

            bw.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle((BufferedWriter) bw);
            }
        }
    }

    /**
     *
     *
     * @return
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
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Sheet) {
            final Sheet<R, C, V> other = (Sheet<R, C, V>) obj;

            return N.equals(other._rowKeySet, _rowKeySet) && N.equals(other._columnKeySet, _columnKeySet) && N.deepEquals(other._columnList, _columnList);
        }

        return false;
    }

    /**
     *
     *
     * @return
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

    /**
     * Inits the.
     */
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

    /**
     * Inits the index map.
     */
    private void initIndexMap() {
        if (_rowKeyIndexMap == null || _columnKeyIndexMap == null) {
            final int rowLength = rowLength();
            _rowKeyIndexMap = N.newBiMap(rowLength);
            int index = 0;
            for (final R rowKey : _rowKeySet) {
                _rowKeyIndexMap.put(rowKey, index++);
            }

            final int columnLength = columnLength();
            _columnKeyIndexMap = N.newBiMap(columnLength);
            index = 0;
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
            throw new IllegalStateException("This DataSet is frozen, can't modify it.");
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
    public static record Cell<R, C, V>(R rowKey, C columnKey, V value) {

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
    public static record Point(int rowIndex, int columnIndex) {

        public static final Point ZERO = new Point(0, 0);

        private static final int MAX_CACHE_SIZE = 128;
        private static final Point[][] CACHE = new Point[MAX_CACHE_SIZE][MAX_CACHE_SIZE];

        static {
            for (int i = 0; i < MAX_CACHE_SIZE; i++) {
                for (int j = 0; j < MAX_CACHE_SIZE; j++) {
                    CACHE[i][j] = new Point(i, j);
                }
            }
        }

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
