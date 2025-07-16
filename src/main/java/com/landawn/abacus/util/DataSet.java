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

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collector;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntObjFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

/**
 * The DataSet interface represents a data structure that holds a collection of data in a tabular format.
 * It provides a variety of methods for manipulating and accessing the data, such as sorting, filtering, joining, and grouping.
 * It also supports operations like union, intersection, and difference between two DataSets.
 * The data in a DataSet is organized into rows and columns, similar to a table in a relational database.
 * Each column in a DataSet has a name(case-sensitive), and the data within a column is of a specific type.
 * <br />
 * @see com.landawn.abacus.util.Builder.DataSetBuilder
 * @see com.landawn.abacus.util.Sheet
 * @see com.landawn.abacus.util.CSVUtil
 * @see com.landawn.abacus.util.IntFunctions
 * @see com.landawn.abacus.util.Clazz
 * @see com.landawn.abacus.util.N#newEmptyDataSet()
 * @see com.landawn.abacus.util.N#newEmptyDataSet(Collection)
 * @see com.landawn.abacus.util.N#newDataSet(Map)
 * @see com.landawn.abacus.util.N#newDataSet(Collection)
 * @see com.landawn.abacus.util.N#newDataSet(Collection, Collection)
 * @see com.landawn.abacus.util.N#newDataSet(String, String, Map)
 */
public sealed interface DataSet permits RowDataSet {

    /**
     * Returns an immutable empty {@code DataSet}.
     * This method can be used when you need an empty DataSet for initialization or comparison purposes.
     *
     * @return an immutable empty {@code DataSet}
     */
    static DataSet empty() {
        return RowDataSet.EMPTY_DATA_SET;
    }

    /**
     * Creates a new DataSet with the specified column names and rows.
     *
     * The DataSet is a data structure that stores data in a tabular format, similar to a table in a database.
     * Each item in the <i>columnNames</i> collection represents a column in the DataSet.
     * The <i>rows</i> parameter is a 2D array where each subarray represents a row in the DataSet.
     * The order of elements in each row should correspond to the order of column names.
     *
     * @param columnNames A collection of strings representing the names of the columns in the DataSet.
     * @param rows A 2D array representing the data in the DataSet. Each subarray is a row.
     * @return A new DataSet with the specified column names and rows.
     * @throws IllegalArgumentException If the provided columnNames and rows do not align properly.
     * @see N#newDataSet(Collection, Object[][])
     */
    static DataSet rows(final Collection<String> columnNames, final Object[][] rows) throws IllegalArgumentException {
        return N.newDataSet(columnNames, rows);
    }

    /**
     * Creates a new DataSet with the specified column names and rows.
     *
     * The DataSet is a data structure that stores data in a tabular format, similar to a table in a database.
     * Each item in the <i>columnNames</i> collection represents a column in the DataSet.
     * The <i>rows</i> parameter is a collection of collections where each sub-collection represents a row in the DataSet.
     * The order of elements in each row should correspond to the order of column names.
     *
     * @param columnNames A collection of strings representing the names of the columns in the DataSet.
     * @param rows A collection of collections representing the data in the DataSet. Each sub-collection is a row which can be: Map/Bean/Array/List.
     * @return A new DataSet with the specified column names and rows.
     * @throws IllegalArgumentException If the provided columnNames and rows do not align properly.
     * @see N#newDataSet(Collection, Collection)
     */
    static DataSet rows(final Collection<String> columnNames, final Collection<? extends Collection<?>> rows) throws IllegalArgumentException {
        return N.newDataSet(columnNames, rows);
    }

    /**
     * Creates a new DataSet with the specified column names and columns.
     *
     * The DataSet is a data structure that stores data in a tabular format, similar to a table in a database.
     * Each item in the <i>columnNames</i> collection represents a column in the DataSet.
     * The <i>columns</i> parameter is a 2D array where each subarray represents a column in the DataSet.
     * The order of elements in each column should correspond to the order of column names.
     *
     * @param columnNames A collection of strings representing the names of the columns in the DataSet.
     * @param columns A 2D array representing the data in the DataSet. Each subarray is a column.
     * @return A new DataSet with the specified column names and columns.
     * @throws IllegalArgumentException If the length of <i>columnNames</i> is not equal to the length of <i>columns</i> or the size of the sub-collection in <i>columns</i> is not equal.
     */
    static DataSet columns(final Collection<String> columnNames, final Object[][] columns) throws IllegalArgumentException {
        if (N.size(columnNames) != N.len(columns)) {
            throw new IllegalArgumentException("The length of 'columnNames' is not equal to the length of the sub-collections in 'columns'.");
        }

        final int columnCount = N.size(columnNames);
        final int rowCount = N.len(N.firstOrNullIfEmpty(columns));

        final List<String> columnNameList = N.newArrayList(columnNames);
        final List<List<Object>> columnList = new ArrayList<>(columnCount);

        for (final Object[] column : columns) {
            if (N.len(column) != rowCount) {
                throw new IllegalArgumentException("The size of the sub-collection in 'columns' is not equal.");
            }

            columnList.add(Array.asList(column));
        }

        return new RowDataSet(columnNameList, columnList);
    }

    /**
     * Creates a new DataSet with the specified column names and columns.
     *
     * The DataSet is a data structure that stores data in a tabular format, similar to a table in a database.
     * Each item in the <i>columnNames</i> collection represents a column in the DataSet.
     * The <i>columns</i> parameter is a collection of collections where each sub-collection represents a column in the DataSet.
     *
     * @param columnNames A collection of strings representing the names of the columns in the DataSet.
     * @param columns A collection of collections representing the data in the DataSet. Each sub-collection is a column.
     * @return A new DataSet with the specified column names and columns.
     * @throws IllegalArgumentException If the length of <i>columnNames</i> is not equal to the length of <i>columns</i> or the size of the sub-collection in <i>columns</i> is not equal.
     */
    static DataSet columns(final Collection<String> columnNames, final Collection<? extends Collection<?>> columns) throws IllegalArgumentException {
        if (N.size(columnNames) != N.size(columns)) {
            throw new IllegalArgumentException("The length of 'columnNames' is not equal to the length of the sub-collections in 'columns'.");
        }

        final int columnCount = N.size(columnNames);
        final int rowCount = N.size(N.firstOrNullIfEmpty(columns));

        final List<String> columnNameList = N.newArrayList(columnNames);
        final List<List<Object>> columnList = new ArrayList<>(columnCount);

        for (final Collection<?> column : columns) {
            if (N.size(column) != rowCount) {
                throw new IllegalArgumentException("The size of the sub-collection in 'columns' is not equal.");
            }

            columnList.add(N.newArrayList(column));
        }

        return new RowDataSet(columnNameList, columnList);
    }

    //    /**
    //     * Creates a new DataSet with a single column.
    //     *
    //     * The DataSet is a data structure that stores data in a tabular format, similar to a table in a database.
    //     * The 'columnName' parameter represents the name of the column in the DataSet.
    //     * The 'column' parameter is a collection where each item represents a row in the DataSet.
    //     *
    //     * @param columnName A string representing the name of the column in the DataSet.
    //     * @param column A collection of objects representing the data in the DataSet. Each object is a row.
    //     * @return A new DataSet with the specified column name and column data.
    //     * @throws IllegalArgumentException If the provided columnName is empty.
    //     * @see #columns(Collection, Collection)
    //     * @see N#newDataSet(String, Collection)
    //     */
    //    static DataSet singleColumn(final String columnName, final Collection<?> column) throws IllegalArgumentException {
    //        return N.newDataSet(columnName, column);
    //    }

    //    /**
    //     * Returns the bean name associated with the query.
    //     *
    //     * @return
    //     */
    //    String beanName();
    //
    //    /**
    //     * Returns the target bean class associated with the query.
    //     *
    //     * @return
    //     */
    //    <T> Class<T> rowType();

    /**
     * Returns an immutable list of column names in this DataSet.
     * The order of the column names in the list reflects the order of the columns in the DataSet.
     *
     * @return an ImmutableList of column names
     */
    ImmutableList<String> columnNameList();

    /**
     * Returns the number of columns in this DataSet.
     *
     * @return the count of columns
     */
    int columnCount();

    /**
     * Returns the column name for the specified column index.
     *
     * @param columnIndex the index of the column.
     * @return the name of the column at the specified index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds
     */
    String getColumnName(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Returns the index of the specified column in the DataSet.
     *
     * @param columnName the name(case-sensitive) of the column for which the index is required.
     * @return the index of the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     */
    int getColumnIndex(String columnName) throws IllegalArgumentException;

    /**
     * Returns an array of column indexes corresponding to the provided column names.
     *
     * @param columnNames the collection of column names(case-sensitive) for which indexes are required.
     * @return an array of integers representing the indexes of the specified columns.
     * @throws IllegalArgumentException if any of the provided column names does not exist in the DataSet.
     */
    int[] getColumnIndexes(Collection<String> columnNames) throws IllegalArgumentException;

    /**
     * Checks if the specified column name exists in this DataSet.
     *
     * @param columnName the name(case-sensitive) of the column to check.
     * @return {@code true} if the column exists, {@code false} otherwise.
     */
    boolean containsColumn(String columnName);

    /**
     * Check if this {@code DataSet} contains all the specified columns.
     *
     * @param columnNames the collection of column names(case-sensitive) to check.
     * @return {@code true} if all the specified columns are included in the this {@code DataSet}
     */
    boolean containsAllColumns(Collection<String> columnNames);

    /**
     * Renames a column in the DataSet.
     *
     * @param columnName the current name of the column.
     * @param newColumnName the new name for the column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet or the new column name exists in the DataSet.
     */
    void renameColumn(String columnName, String newColumnName) throws IllegalArgumentException;

    /**
     * Renames multiple columns in the DataSet.
     *
     * @param oldNewNames a map where the key is the current name of the column and the value is the new name for the column.
     * @throws IllegalArgumentException if any of the specified old column names does not exist in the DataSet or any of the new column names already exists in the DataSet.
     */
    void renameColumns(Map<String, String> oldNewNames) throws IllegalArgumentException;

    //    /**
    //     *
    //     * @param columnName
    //     * @param func
    //     */
    //    void renameColumn(String columnName, Function<? super String, String > func)  ;

    /**
     * Renames multiple columns in the DataSet using a function to determine the new names.
     *
     * @param columnNames the collection of current column names to be renamed.
     * @param func a function that takes the current column name as input and returns the new column name.
     * @throws IllegalArgumentException if any of the specified old column names does not exist in the DataSet or any of the new column names already exists in the DataSet.
     */
    void renameColumns(Collection<String> columnNames, Function<? super String, String> func) throws IllegalArgumentException;

    /**
     * Renames all columns in the DataSet using a function to determine the new names.
     *
     * @param func a function that takes the current column name as input and returns the new column name.
     * @throws IllegalArgumentException if any of the new column names already exists in the DataSet.
     */
    void renameColumns(Function<? super String, String> func) throws IllegalArgumentException;

    /**
     * Moves a column in the DataSet to a new position.
     *
     * @param columnName the name of the column to be moved.
     * @param newPosition the new position for the column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet
     * @throws IndexOutOfBoundsException if the new position is out of bounds.
     */
    void moveColumn(String columnName, int newPosition) throws IllegalArgumentException, IndexOutOfBoundsException;

    /**
     * Moves multiple columns in the DataSet to new positions.
     *
     * @param columnNameNewPositionMap a map where the key is the current name of the column and the value is the new position for the column.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or any of the new positions are out of bounds.
     */
    void moveColumns(Map<String, Integer> columnNameNewPositionMap) throws IllegalArgumentException;

    /**
     * Swaps the positions of two columns in the DataSet.
     *
     * @param columnNameA the name of the first column to be swapped.
     * @param columnNameB the name of the second column to be swapped.
     * @throws IllegalArgumentException if either of the specified column names does not exist in the DataSet.
     */
    void swapColumnPosition(String columnNameA, String columnNameB) throws IllegalArgumentException;

    /**
     * Moves a row in the DataSet to a new position.
     *
     * @param rowIndex the index of the row to be moved.
     * @param newRowIndex the new position for the row.
     * @throws IndexOutOfBoundsException if the specified row index is out of bounds or the new position is out of bounds.
     */
    void moveRow(int rowIndex, int newRowIndex) throws IllegalArgumentException;

    /**
     * Swaps the positions of two rows in the DataSet.
     *
     * @param rowIndexA the index of the first row to be swapped.
     * @param rowIndexB the index of the second row to be swapped.
     * @throws IndexOutOfBoundsException if either of the specified row indexes is out of bounds.
     */
    void swapRowPosition(int rowIndexA, int rowIndexB) throws IllegalArgumentException;

    /**
     * Retrieves the value at the specified row and column index in the DataSet.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T> the type of the value to be returned.
     * @param rowIndex the index of the row.
     * @param columnIndex the index of the column.
     * @return the value at the specified row and column index.
     * @throws IndexOutOfBoundsException if the specified row or column index is out of bounds.
     */
    <T> T get(int rowIndex, int columnIndex) throws IndexOutOfBoundsException;

    //    /**
    //     * There is NO underline auto-conversion from column value to target type: {@code T}.
    //     * So the column values must be the type which is assignable to target type.
    //     *
    //     * @param rowIndex
    //     * @param columnIndex
    //     * @param targetType
    //     *
    //     * @param <T>
    //     * @return
    //     * @throws UnsupportedOperationException
    //     * @deprecated may be misused because it implies there is an underline auto-conversion from column values to target return type but actually there is not.
    //     */
    //    @SuppressWarnings("unused")
    //    @Deprecated
    //    default <T> T get(int rowIndex, int columnIndex, Class<? extends T> targetType) throws UnsupportedOperationException {
    //        throw new UnsupportedOperationException();
    //    }

    /**
     * Sets the value at the specified row and column index in the DataSet.
     *
     * @param rowIndex the index of the row.
     * @param columnIndex the index of the column.
     * @param element the new value to be set at the specified row and column index.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified row or column index is out of bounds.
     */
    void set(int rowIndex, int columnIndex, Object element) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Checks if the value at the specified row and column index in the DataSet is {@code null}.
     *
     * @param rowIndex the index of the row.
     * @param columnIndex the index of the column.
     * @return {@code true} if the value at the specified row and column index is {@code null}, {@code false} otherwise.
     * @throws IndexOutOfBoundsException if the specified row or column index is out of bounds.
     */
    boolean isNull(int rowIndex, int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the value at the specified column index in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T> the type of the value to be returned.
     * @param columnIndex the index of the column.
     * @return the value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    <T> T get(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the value at the specified column in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     * <br />
     * Using {@code get(int)} for better performance.
     *
     * @param <T> the type of the value to be returned.
     * @param columnName the name of the column.
     * @return the value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     * @see #get(int)
     */
    <T> T get(String columnName);

    //    /**
    //     * There is NO underline auto-conversion from column value to target type: {@code T}.
    //     * So the column values must be the type which is assignable to target type.
    //     *
    //     *
    //     * @param <T>
    //     * @return
    //     * @throws UnsupportedOperationException
    //     * @deprecated may be misused because it implies there is an underline auto-conversion from column values to target return type but actually there is not.
    //     */
    //    @SuppressWarnings("unused")
    //    @Deprecated
    //    default <T> T get(int columnIndex, Class<? extends T> targetType) throws UnsupportedOperationException {
    //        throw new UnsupportedOperationException();
    //    }
    //
    //    /**
    //     *
    //     * There is NO underline auto-conversion from column value to target type: {@code T}.
    //     * So the column values must be the type which is assignable to target type.
    //     *
    //     * @param columnName
    //     * @param targetType
    //     *
    //     * @param <T>
    //     * @return
    //     * @throws UnsupportedOperationException
    //     * @deprecated may be misused because it implies there is an underline auto-conversion from column values to target return type but actually there is not.
    //     */
    //    @SuppressWarnings("unused")
    //    @Deprecated
    //    default <T> T get(String columnName, Class<? extends T> targetType) throws UnsupportedOperationException {
    //        throw new UnsupportedOperationException();
    //    }
    //
    //    /**
    //     * Returns the value from the current row and specified column if the specified {@code columnIndex} is equal or bigger than zero,
    //     * or the specified {@code defaultValue} otherwise.
    //     * <br />
    //     * There is NO underline auto-conversion from column value to target type: {@code T}.
    //     * So the column values must be the type which is assignable to target type.
    //     *
    //     * @param <T>
    //     * @param columnIndex
    //     * @param defaultValue
    //     * @return
    //     * @throws UnsupportedOperationException
    //     * @deprecated
    //     */
    //    @SuppressWarnings("unused")
    //    @Deprecated
    //    default <T> T getOrDefault(int columnIndex, T defaultValue) throws UnsupportedOperationException {
    //        throw new UnsupportedOperationException();
    //    }
    //
    //    /**
    //     * Returns the value from the current row and specified column if the specified {@code columnName} exists,
    //     * or the specified {@code defaultValue} otherwise.
    //     * <br />
    //     * There is NO underline auto-conversion from column value to target type: {@code T}.
    //     * So the column values must be the type which is assignable to target type.
    //     *
    //     * @param <T>
    //     * @param columnName
    //     * @param defaultValue
    //     * @return
    //     * @throws UnsupportedOperationException
    //     * @deprecated
    //     */
    //    @SuppressWarnings("unused")
    //    @Deprecated
    //    default <T> T getOrDefault(String columnName, T defaultValue) throws UnsupportedOperationException {
    //        throw new UnsupportedOperationException();
    //    }

    /**
     * Retrieves the boolean value at the specified column index in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Boolean}.
     * So the column values must be the type which is assignable to target type.
     * <br />
     * Returns default value (false) if the property is {@code null}.
     *
     * @param columnIndex the index of the column.
     * @return the boolean value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    boolean getBoolean(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the boolean value at the specified column in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Boolean}.
     * So the column values must be the type which is assignable to target type.
     * <br />
     * Returns default value (false) if the property is {@code null}.
     * <br />
     * Using {@code getBoolean(int)} for better performance.
     *
     * @param columnName the name of the column.
     * @return the boolean value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     * @see #getBoolean(int)
     */
    boolean getBoolean(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the char value at the specified column index in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Character}.
     * So the column values must be the type which is assignable to target type.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     *
     * @param columnIndex the index of the column.
     * @return the char value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    char getChar(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the char value at the specified column in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Character}.
     * So the column values must be the type which is assignable to target type.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     * <br />
     * Using {@code getChar(int)} for better performance.
     *
     * @param columnName the name of the column.
     * @return the char value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     * @see #getChar(int)
     */
    char getChar(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the byte value at the specified column index in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Byte}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     *
     * @param columnIndex the index of the column.
     * @return the byte value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    byte getByte(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the byte value at the specified column in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Byte}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     * <br />
     * Using {@code getByte(int)} for better performance.
     *
     * @param columnName the name of the column.
     * @return the byte value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     * @see #getByte(int)
     */
    byte getByte(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the short value at the specified column index in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Short}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     *
     * @param columnIndex the index of the column.
     * @return the short value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    short getShort(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the short value at the specified column in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Short}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     * <br />
     * Using {@code getShort(int)} for better performance.
     *
     * @param columnName the name of the column.
     * @return the short value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     * @see #getShort(int)
     */
    short getShort(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the integer value at the specified column index in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Integer}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     *
     * @param columnIndex the index of the column.
     * @return the integer value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    int getInt(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the integer value at the specified column in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Integer}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     * <br />
     * Using {@code getInt(int)} for better performance.
     *
     *
     * @param columnName the name of the column.
     * @return the integer value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     * @see #getInt(int)
     */
    int getInt(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the long value at the specified column index in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Long}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     *
     * @param columnIndex the index of the column.
     * @return the long value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    long getLong(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the long value at the specified column in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Long}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     * <br />
     * Using {@code getLong(int)} for better performance.
     *
     * @param columnName the name of the column.
     * @return the long value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     * @see #getLong(int)
     */
    long getLong(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the float value at the specified column index in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Float}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0f) if the property is {@code null}.
     *
     * @param columnIndex the index of the column.
     * @return the float value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    float getFloat(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the float value at the specified column in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Float}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0f) if the property is {@code null}.
     * <br />
     * Using {@code getFloat(int)} for better performance.
     *
     * @param columnName the name of the column.
     * @return the float value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     * @see #getFloat(int)
     */
    float getFloat(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the double value at the specified column index in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Double}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0d) if the property is {@code null}.
     *
     * @param columnIndex the index of the column.
     * @return the double value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    double getDouble(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the double value at the specified column in the DataSet for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Double}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0d) if the property is {@code null}.
     * <br />
     * Using {@code getDouble(int)} for better performance.
     *
     * @param columnName the name of the column.
     * @return the double value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     * @see #getDouble(int)
     */
    double getDouble(String columnName) throws IllegalArgumentException;

    /**
     * Checks if the value at the specified column index in the DataSet for the current row is {@code null}.
     * <br />
     * This method can be used to validate the data before performing operations that do not support {@code null} values.
     *
     * @param columnIndex the index of the column.
     * @return {@code true} if the value at the specified column index is {@code null}, {@code false} otherwise.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    boolean isNull(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Checks if the value at the specified column in the DataSet for the current row is {@code null}.
     * <br />
     * This method can be used to validate the data before performing operations that do not support {@code null} values.
     * <br />
     * Using {@code isNull(int)} for better performance.
     *
     * @param columnName the name of the column.
     * @return {@code true} if the value at the specified column is {@code null}, {@code false} otherwise.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     * @see #isNull(int)
     */
    boolean isNull(String columnName) throws IllegalArgumentException;

    /**
     * Sets the value at the specified column index in the DataSet for the current row.
     *
     * @param columnIndex the index of the column.
     * @param value the new value to be set at the specified column index.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    void set(int columnIndex, Object value) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Sets the value at the specified column in the DataSet for the current row.
     * <br />
     * Using {@code set(int, Object)} for better performance.
     *
     * @param columnName the name of the column.
     * @param value the new value to be set at the specified column.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     * @see #set(int, Object)
     */
    void set(String columnName, Object value) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Retrieves the values of the specified column index in the DataSet as an ImmutableList.
     * <br />
     * The returned list is immutable and any attempt to modify it will result in an UnsupportedOperationException.
     * <br />
     * The type of the values in the list will be the same as the type of the column.
     * <br />
     * The order of the values in the list reflects the order of the rows in the DataSet.
     *
     * @param <T> the type of the values to be returned.
     * @param columnIndex the index of the column.
     * @return an ImmutableList of values at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    <T> ImmutableList<T> getColumn(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the values of the specified column in the DataSet as an ImmutableList.
     * <br />
     * The returned list is immutable and any attempt to modify it will result in an UnsupportedOperationException.
     * <br />
     * The type of the values in the list will be the same as the type of the column.
     * <br />
     * The order of the values in the list reflects the order of the rows in the DataSet.
     *
     * @param <T> the type of the values to be returned.
     * @param columnName the name of the column.
     * @return an ImmutableList of values at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     */
    <T> ImmutableList<T> getColumn(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the values of the specified column in the DataSet as a List.
     * <br />
     * The returned list is a copy and modifications to it will not affect the original DataSet.
     * <br />
     * The type of the values in the list will be the same as the type of the column.
     *
     * @param <T> the type of the values to be returned.
     * @param columnName the name of the column.
     * @return a List of values at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     */
    <T> List<T> copyColumn(String columnName) throws IllegalArgumentException;

    /**
     * Adds a new column to the DataSet.
     * <br />
     * The new column is added at the end of the existing columns.
     * The size of this list should match the number of rows in the DataSet.
     *
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the DataSet.
     * @param column The data for the new column. It should be a list where each element represents a row in the column.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the new column name already exists in the DataSet or the provided collection is not empty and its size does not match the number of rows in the DataSet.
     */
    void addColumn(String newColumnName, Collection<?> column) throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the DataSet at the specified position.
     * <br />
     * The new column is added at the position specified by newColumnPosition. Existing columns at and after this position are shifted to the right.
     * The size of the list provided should match the number of rows in the DataSet.
     *
     * @param newColumnPosition The position at which the new column should be added. It should be a valid index within the current column range.
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the DataSet.
     * @param column The data for the new column. It should be a collection where each element represents a row in the column.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified {@code newColumnPosition} is less than zero or bigger than column size or the new column name already exists in the DataSet,
     *                               or if the provided collection is not empty and its size does not match the number of rows in the DataSet, or the newColumnPosition is out of bounds.
     */
    void addColumn(int newColumnPosition, String newColumnName, Collection<?> column) throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the DataSet.
     * <br />
     * The new column is generated by applying a function to an existing column. The function takes the value of the existing column for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the end of the existing columns.
     *
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the DataSet.
     * @param fromColumnName The name of the existing column to be used as input for the function.
     * @param func The function to generate the values for the new column. It takes the value of the existing column for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the new column name already exists in the DataSet or the existing column name does not exist in the DataSet.
     */
    void addColumn(String newColumnName, String fromColumnName, Function<?, ?> func) throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the DataSet at the specified position.
     * <br />
     * The new column is generated by applying a function to an existing column. The function takes the value of the existing column for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the position specified by newColumnPosition. Existing columns at and after this position are shifted to the right.
     *
     * @param newColumnPosition The position at which the new column should be added. It should be a valid index within the current column range.
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the DataSet.
     * @param fromColumnName The name of the existing column to be used as input for the function.
     * @param func The function to generate the values for the new column. It takes the value of the existing column for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newColumnPosition} is less than zero or bigger than column size or the new column name already exists in the DataSet, or if the existing column name does not exist in the DataSet.
     */
    void addColumn(int newColumnPosition, String newColumnName, String fromColumnName, Function<?, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the DataSet.
     * <br />
     * The new column is generated by applying a function to multiple existing columns. The function takes the values of the existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the end of the existing columns.
     *
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the DataSet.
     * @param fromColumnNames The names of the existing columns to be used as input for the function.
     * @param func The function to generate the values for the new column. It takes the values of the existing columns for each row and returns the value for the new column for that row. The input to the function is a DisposableObjArray containing the values of the existing columns for a particular row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the new column name already exists in the DataSet or any of the existing column names does not exist in the DataSet.
     */
    void addColumn(String newColumnName, Collection<String> fromColumnNames, Function<? super DisposableObjArray, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the DataSet at the specified position.
     * <br />
     * The new column is generated by applying a function to multiple existing columns. The function takes the values of the existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the position specified by newColumnPosition. Existing columns at and after this position are shifted to the right.
     *
     * @param newColumnPosition The position at which the new column should be added. It should be a valid index within the current column range.
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the DataSet.
     * @param fromColumnNames The names of the existing columns to be used as input for the function.
     * @param func The function to generate the values for the new column. It takes the values of the existing columns for each row and returns the value for the new column for that row. The input to the function is a DisposableObjArray containing the values of the existing columns for a particular row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newColumnPosition} is less than zero or bigger than column size or the new column name already exists in the DataSet, or if any of the existing column names does not exist in the DataSet.
     */
    void addColumn(int newColumnPosition, String newColumnName, Collection<String> fromColumnNames, Function<? super DisposableObjArray, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the DataSet.
     * <br />
     * The new column is generated by applying a BiFunction to two existing columns. The BiFunction takes the values of the existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the end of the existing columns.
     *
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the DataSet.
     * @param fromColumnNames A Tuple2 containing the names of the two existing columns to be used as input for the BiFunction.
     * @param func The BiFunction to generate the values for the new column. It takes the values of the two existing columns for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the new column name already exists in the DataSet or any of the existing column names does not exist in the DataSet.
     */
    void addColumn(String newColumnName, Tuple2<String, String> fromColumnNames, BiFunction<?, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the DataSet at the specified position.
     * <br />
     * The new column is generated by applying a BiFunction to two existing columns. The BiFunction takes the values of the two existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the position specified by newColumnPosition. Existing columns at and after this position are shifted to the right.
     *
     * @param newColumnPosition The position at which the new column should be added. It should be a valid index within the current column range.
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the DataSet.
     * @param fromColumnNames A Tuple2 containing the names of the two existing columns to be used as input for the BiFunction.
     * @param func The BiFunction to generate the values for the new column. It takes the values of the two existing columns for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newColumnPosition} is less than zero or bigger than column size or the new column name already exists in the DataSet, or if any of the existing column names does not exist in the DataSet.
     */
    void addColumn(int newColumnPosition, String newColumnName, Tuple2<String, String> fromColumnNames, BiFunction<?, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the DataSet.
     * <br />
     * The new column is generated by applying a TriFunction to three existing columns. The TriFunction takes the values of the three existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the end of the existing columns.
     *
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the DataSet.
     * @param fromColumnNames A Tuple3 containing the names of the three existing columns to be used as input for the TriFunction.
     * @param func The TriFunction to generate the values for the new column. It takes the values of the three existing columns for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the new column name already exists in the DataSet or any of the existing column names does not exist in the DataSet.
     */
    void addColumn(String newColumnName, Tuple3<String, String, String> fromColumnNames, TriFunction<?, ?, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the DataSet at the specified position.
     * <br />
     * The new column is generated by applying a TriFunction to three existing columns. The TriFunction takes the values of the three existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the position specified by newColumnPosition. Existing columns at and after this position are shifted to the right.
     *
     * @param newColumnPosition The position at which the new column should be added. It should be a valid index within the current column range.
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the DataSet.
     * @param fromColumnNames A Tuple3 containing the names of the three existing columns to be used as input for the TriFunction.
     * @param func The TriFunction to generate the values for the new column. It takes the values of the three existing columns for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newColumnPosition} is less than zero or bigger than column size or the new column name already exists in the DataSet, or if any of the existing column names does not exist in the DataSet.
     */
    void addColumn(int newColumnPosition, String newColumnName, Tuple3<String, String, String> fromColumnNames, TriFunction<?, ?, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Removes a column from the DataSet.
     * <br />
     * The column is identified by its name. All data in the column is removed and returned as a List.
     * <br />
     * The order of the values in the returned list reflects the order of the rows in the DataSet.
     *
     * @param <T> The type of the values in the column.
     * @param columnName The name of the column to be removed. It should be a name that exists in the DataSet.
     * @return A List containing the values of the removed column.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     */
    <T> List<T> removeColumn(String columnName) throws IllegalStateException, IllegalArgumentException;

    /**
     * Removes multiple columns from the DataSet.
     * <br />
     * The columns are identified by their names provided in the collection. All data in these columns are removed.
     *
     * @param columnNames A collection containing the names of the columns to be removed. These should be names that exist in the DataSet.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     */
    void removeColumns(Collection<String> columnNames) throws IllegalStateException, IllegalArgumentException;

    /**
     * Removes multiple columns from the DataSet.
     * <br />
     * The columns to be removed are identified by a Predicate function. The function is applied to each column name, and if it returns {@code true}, the column is removed.
     *
     * @param filter A Predicate function to determine which columns should be removed. It should return {@code true} for column names that should be removed, and {@code false} for those that should be kept.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     */
    void removeColumns(Predicate<? super String> filter) throws IllegalStateException;

    //    /**
    //     * Remove the column(s) whose name matches the specified {@code filter}.
    //     *
    //     * @param filter column name filter
    //     * @deprecated replaced by {@code removeColumns}.
    //     */
    //    @Deprecated
    //    void removeColumnsIf(Predicate<? super String> filter);

    /**
     * Updates the values in a specified column of the DataSet.
     * <br />
     * The update is performed by applying a function to each value in the column. The function takes the current value and returns the new value.
     *
     * @param columnName The name of the column to be updated. It should be a name that exists in the DataSet.
     * @param func The function to be applied to each value in the column. It takes the current value and returns the new value.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     */
    void updateColumn(String columnName, Function<?, ?> func) throws IllegalStateException, IllegalArgumentException;

    /**
     * Updates the values in multiple specified columns of the DataSet.
     * <br />
     * The update is performed by applying a function to each value in the specified columns. The function takes the current value and returns the new value.
     *
     * @param columnNames A collection containing the names of the columns to be updated. These should be names that exist in the DataSet.
     * @param func The function to be applied to each value in the columns. It takes the current value and returns the new value.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     */
    void updateColumns(Collection<String> columnNames, Function<?, ?> func) throws IllegalStateException, IllegalArgumentException;

    /**
     * Converts the values in a specified column of the DataSet to a specified target type.
     *
     * @param columnName The name of the column to be converted. It should be a name that exists in the DataSet.
     * @param targetType The Class object representing the target type to which the column values should be converted.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet or a value cannot be cast to the target type.
     * @throws NumberFormatException if string value of the column cannot be parsed to the target(Number) type.
     * @throws RuntimeException if any other error occurs during the conversion.
     * @see N#convert(Object, Class)
     */
    void convertColumn(String columnName, Class<?> targetType) throws IllegalStateException, IllegalArgumentException, NumberFormatException, RuntimeException;

    /**
     * Converts the values in multiple specified columns of the DataSet to their respective target types.
     *
     * @param columnTargetTypes A map where the key is the column name and the value is the Class object representing the target type to which the column values should be converted. The column names should exist in the DataSet.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnTargetTypes} is empty or a value cannot be cast to the target type.
     * @throws NumberFormatException if string value of the column cannot be parsed to the target(Number) type.
     * @throws RuntimeException if any other error occurs during the conversion.
     * @see N#convert(Object, Class)
     */
    void convertColumns(Map<String, Class<?>> columnTargetTypes)
            throws IllegalStateException, IllegalArgumentException, NumberFormatException, RuntimeException;

    //
    //    /**
    //     * convert the specified columns to target types.
    //     *
    //     * @param targetColumnTypes fill the element with {@code null} if don't wan to convert the target column.
    //     */
    //    void convertColumn(Class<?>[] targetColumnTypes);

    /**
     * Combines multiple columns into a new column in the DataSet.
     * <br />
     * The new column is created by combining the values of the specified columns for each row. The type of the new column is specified by the newColumnType parameter.
     *
     * @param columnNames A collection containing the names of the columns to be combined. These should be names that exist in the DataSet.
     * @param newColumnName The name of the new column to be created. It should not be a name that already exists in the DataSet.
     * @param newColumnType The Class object representing the type of the new column.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty or the new column name already exists in the DataSet.
     */
    void combineColumns(Collection<String> columnNames, String newColumnName, Class<?> newColumnType) throws IllegalStateException, IllegalArgumentException;

    /**
     * Combines multiple columns into a new column in the DataSet.
     * <br />
     * The new column is created by applying a function to the values of the specified columns for each row. The function takes a DisposableObjArray of the values in the existing columns for a particular row and returns the value for the new column for that row.
     *
     * @param columnNames A collection containing the names of the columns to be combined. These should be names that exist in the DataSet.
     * @param newColumnName The name of the new column to be created. It should not be a name that already exists in the DataSet.
     * @param combineFunc The function to generate the values for the new column. It takes a DisposableObjArray of the values in the existing columns for a particular row and returns the value for the new column for that row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty or the new column name already exists in the DataSet.
     */
    void combineColumns(Collection<String> columnNames, String newColumnName, Function<? super DisposableObjArray, ?> combineFunc)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Combines two columns into a new column in the DataSet.
     * <br />
     * The new column is created by applying a BiFunction to the values of the specified columns for each row.
     * The BiFunction takes the values of the two existing columns for a particular row and returns the value for the new column for that row.
     *
     * @param columnNames A Tuple2 containing the names of the two columns to be combined. These should be names that exist in the DataSet.
     * @param newColumnName The name of the new column to be created. It should not be a name that already exists in the DataSet.
     * @param combineFunc The BiFunction to generate the values for the new column. It takes the values of the two existing columns for a particular row and returns the value for the new column for that row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or the new column name already exists in the DataSet.
     */
    void combineColumns(Tuple2<String, String> columnNames, String newColumnName, BiFunction<?, ?, ?> combineFunc)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Combines three columns into a new column in the DataSet.
     * <br />
     * The new column is created by applying a TriFunction to the values of the specified columns for each row.
     * The TriFunction takes the values of the three existing columns for a particular row and returns the value for the new column for that row.
     *
     * @param columnNames A Tuple3 containing the names of the three columns to be combined. These should be names that exist in the DataSet.
     * @param newColumnName The name of the new column to be created. It should not be a name that already exists in the DataSet.
     * @param combineFunc The TriFunction to generate the values for the new column. It takes the values of the three existing columns for a particular row and returns the value for the new column for that row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet is empty or the new column name already exists in the DataSet.
     */
    void combineColumns(Tuple3<String, String, String> columnNames, String newColumnName, TriFunction<?, ?, ?, ?> combineFunc)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Combines multiple columns into a new column in the DataSet based on a column name filter.
     * <br />
     * The new column is created by combining the values of the specified columns for each row. The type of the new column is specified by the newColumnType parameter.
     * <br />
     * The columns to be combined are determined by the columnNameFilter. If the columnNameFilter returns {@code true} for a column name, that column is included in the combination.
     *
     * @param columnNameFilter A Predicate function to determine which columns should be combined. It should return {@code true} for column names that should be combined, and {@code false} for those that should be kept.
     * @param newColumnName The name of the new column to be created. It should not be a name that already exists in the DataSet.
     * @param newColumnType The Class object representing the type of the new column.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if none of column name matches the specified {@code columnNameFilter} or the new column name already exists in the DataSet.
     */
    void combineColumns(Predicate<? super String> columnNameFilter, String newColumnName, Class<?> newColumnType)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Combines multiple columns into a new column in the DataSet based on a column name filter.
     * <br />
     * The new column is created by applying a function to the values of the specified columns for each row. The function takes a DisposableObjArray of the values in the existing columns for a particular row and returns the value for the new column for that row.
     * <br />
     * The columns to be combined are determined by the columnNameFilter. If the columnNameFilter returns {@code true} for a column name, that column is included in the combination.
     *
     * @param columnNameFilter A Predicate function to determine which columns should be combined. It should return {@code true} for column names that should be combined, and {@code false} for those that should be kept.
     * @param newColumnName The name of the new column to be created. It should not be a name that already exists in the DataSet.
     * @param combineFunc The function to generate the values for the new column. It takes a DisposableObjArray of the values in the existing columns for a particular row and returns the value for the new column for that row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if none of column name matches the specified {@code columnNameFilter} or the new column name already exists in the DataSet.
     */
    void combineColumns(Predicate<? super String> columnNameFilter, String newColumnName, Function<? super DisposableObjArray, ?> combineFunc)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Divides a column into multiple new columns in the DataSet.
     * <br />
     * The division is performed by applying a function to each value in the specified column. The function takes the current value and returns a List of new values, each of which will be a value in one of the new columns.
     * <br />
     * The new columns are added at the end of the existing columns.
     *
     * @param columnName The name of the column to be divided. It should be a name that exists in the DataSet.
     * @param newColumnNames A collection containing the names of the new columns to be created. These should not be names that already exist in the DataSet. The size of this collection should match the size of the Lists returned by the divideFunc.
     * @param divideFunc The function to be applied to each value in the column. It takes the current value and returns a List of new values. The size of this List should match the size of the newColumnNames collection.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet, any of the new column names already exist in the DataSet, or {@code newColumnNames} is empty, or the size of the Lists returned by the divideFunc does not match the size of the newColumnNames collection.
     */
    void divideColumn(String columnName, Collection<String> newColumnNames, Function<?, ? extends List<?>> divideFunc)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Divides a column into multiple new columns in the DataSet.
     * <br />
     * The division is performed by applying a BiConsumer to each value in the specified column. The BiConsumer takes the current value and an Object array, and it should populate the array with the new values for the new columns.
     * <br />
     * The new columns are added at the end of the existing columns.
     *
     * @param columnName The name of the column to be divided. It should be a name that exists in the DataSet.
     * @param newColumnNames A collection containing the names of the new columns to be created. These should not be names that already exist in the DataSet. The size of this collection should match the size of the Object array populated by the output BiConsumer.
     * @param output The BiConsumer to be applied to each value in the column. It takes the current value and an Object array, and it should populate the array with the new values for the new columns.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet, any of the new column names already exist in the DataSet, or {@code newColumnNames} is empty.
     */
    void divideColumn(String columnName, Collection<String> newColumnNames, BiConsumer<?, Object[]> output)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Divides a column into two new columns in the DataSet.
     * <br />
     * The division is performed by applying a BiConsumer to each value in the specified column. The BiConsumer takes the current value and a Pair object, and it should populate the Pair with the new values for the new columns.
     * <br />
     * The new columns are added at the end of the existing columns.
     *
     * @param columnName The name of the column to be divided. It should be a name that exists in the DataSet.
     * @param newColumnNames A Tuple2 containing the names of the two new columns to be created. These should not be names that already exist in the DataSet.
     * @param output The BiConsumer to be applied to each value in the column. It takes the current value and a Pair object, and it should populate the Pair with the new values for the new columns.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet, any of the new column names already exist in the DataSet.
     */
    void divideColumn(String columnName, Tuple2<String, String> newColumnNames, BiConsumer<?, Pair<Object, Object>> output)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Divides a column into three new columns in the DataSet.
     * <br />
     * The division is performed by applying a BiConsumer to each value in the specified column. The BiConsumer takes the current value and a Triple object, and it should populate the Triple with the new values for the new columns.
     * <br />
     * The new columns are added at the end of the existing columns.
     *
     * @param columnName The name of the column to be divided. It should be a name that exists in the DataSet.
     * @param newColumnNames A Tuple3 containing the names of the three new columns to be created. These should not be names that already exist in the DataSet.
     * @param output The BiConsumer to be applied to each value in the column. It takes the current value and a Triple object, and it should populate the Triple with the new values for the new columns.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet, any of the new column names already exist in the DataSet.
     */
    void divideColumn(String columnName, Tuple3<String, String, String> newColumnNames, BiConsumer<?, Triple<Object, Object, Object>> output)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new row to the DataSet.
     * <br />
     * The row can be represented in various formats such as an Object array, List, Map, or a Bean with getter/setter methods.
     *
     * @param row The new row to be added to the DataSet. It can be an Object array, List, Map, or a Bean with getter/setter methods.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the structure of the row does not match the required type - Object array, List, Map, or Bean.
     */
    void addRow(Object row) throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new row to the DataSet at the specified position.
     * <br />
     * The row can be represented in various formats such as an Object array, List, Map, or a Bean with getter/setter methods.
     * Existing rows at and after the new row position are shifted down.
     *
     * @param newRowPosition The position at which the new row should be added. It should be a valid index within the current row range.
     * @param row The new row to be added to the DataSet. It can be an Object array, List, Map, or a Bean with getter/setter methods.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newRowPosition} is less than zero or bigger than row size, or the structure of the row does not match the required type - Object array, List, Map, or Bean.
     */
    void addRow(int newRowPosition, Object row) throws IllegalStateException, IllegalArgumentException;

    /**
     * Removes a row from the DataSet.
     * <br />
     * The row is identified by its index. All data in the row is removed.
     *
     * @param rowIndex The index of the row to be removed. It should be a valid index within the current row range.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     */
    void removeRow(int rowIndex) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Removes multiple rows from the DataSet.
     * <br />
     * The rows are identified by their indices. All data in these rows are removed.
     *
     * @param indices The indices of the rows to be removed. Each index should be a valid index within the current row range.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IndexOutOfBoundsException if any of the specified indices is out of bounds.
     */
    void removeRows(int... indices) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Removes a range of rows from the DataSet.
     * <br />
     * The range is specified by an inclusive start index and an exclusive end index. All rows within this range are removed.
     *
     * @param inclusiveFromRowIndex The start index of the range. It should be a valid index within the current row range. The row at this index is included in the removal.
     * @param exclusiveToRowIndex The end index of the range. It should be a valid index within the current row range. The row at this index is not included in the removal.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified {@code inclusiveFromRowIndex} is less than zero, or the specified {@code exclusiveToRowIndex} is bigger than row size, or {@code inclusiveFromRowIndex} is bigger than {@code exclusiveToRowIndex}.
     */
    void removeRowRange(int inclusiveFromRowIndex, int exclusiveToRowIndex) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Updates a specific row in the DataSet.
     * <br />
     * The update is performed by applying a function to each value in the specified row. The function takes the current value and returns the new value.
     *
     * @param rowIndex The index of the row to be updated. It should be a valid index within the current row range.
     * @param func The function to be applied to each value in the row. It takes the current value and returns the new value.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     */
    void updateRow(int rowIndex, Function<?, ?> func) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Updates the values in the specified rows of the DataSet.
     * <br />
     * The update is performed by applying a function to each value in the specified rows. The function takes the current value and returns the new value.
     *
     * @param indices An array of integers representing the indices of the rows to be updated. Each index should be a valid index within the current row range.
     * @param func The function to be applied to each value in the rows. It takes the current value and returns the new value.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IndexOutOfBoundsException if any of the specified indices is out of bounds.
     */
    void updateRows(int[] indices, Function<?, ?> func) throws IllegalStateException, IndexOutOfBoundsException;

    // TODO should the method name be "replaceAll"? If change the method name to replaceAll, what about updateColumn/updateRow?

    /**
     * Updates all the values in the DataSet.
     * <br />
     * The update is performed by applying a function to each value in the DataSet. The function takes the current value and returns the new value.
     *
     * @param func The function to be applied to each value in the DataSet. It takes the current value and returns the new value.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     */
    void updateAll(Function<?, ?> func) throws IllegalStateException;

    /**
     * Replaces values in the DataSet that satisfy a specified condition with a new value.
     * <br />
     * The predicate takes each value in the DataSet as input and returns a boolean indicating whether the value should be replaced.
     *
     * @param predicate The predicate to test each value in the sheet. It takes a value from the DataSet as input and returns a boolean indicating whether the value should be replaced.
     * @param newValue The new value to replace the values that satisfy the condition.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     */
    void replaceIf(Predicate<?> predicate, Object newValue) throws IllegalStateException;

    /**
     * Prepends the provided DataSet to the current DataSet.
     * <br />
     * The operation is performed by adding all rows from the provided DataSet to the beginning of the current DataSet.
     * The structure (columns and their types) of the provided DataSet should match the structure of the current DataSet.
     *
     * @param other The DataSet to be prepended to the current DataSet. It should have the same structure as the current DataSet.
     * @throws IllegalStateException if the current DataSet is frozen (read-only).
     * @throws IllegalArgumentException if this DataSet and the provided DataSet don't have the same column names.
     * @see #merge(DataSet)
     */
    void prepend(DataSet other) throws IllegalStateException, IllegalArgumentException;

    /**
     * Appends the provided DataSet to the current DataSet.
     * <br />
     * The operation is performed by adding all rows from the provided DataSet to the end of the current DataSet.
     * The structure (columns and their types) of the provided DataSet should match the structure of the current DataSet.
     *
     * @param other The DataSet to be appended to the current DataSet. It should have the same structure as the current DataSet.
     * @throws IllegalStateException if the current DataSet is frozen (read-only).
     * @throws IllegalArgumentException if this DataSet and the provided DataSet don't have the same column names.
     * @see #merge(DataSet)
     */
    void append(DataSet other) throws IllegalStateException, IllegalArgumentException;

    /**
     * Retrieves the current row number in the DataSet.
     * This method is typically used when iterating over the rows in the DataSet.
     *
     * @return The current row number as an integer. The first row is number 0.
     */
    int currentRowNum();

    /**
     * Moves the cursor to the row in this DataSet object specified by the given index.
     * This method is typically used when navigating through the rows in the DataSet.
     *
     * @param rowIndex The index of the row to move to. The first row is 0, the second is 1, and so on.
     * @return The DataSet object itself with the cursor moved to the specified row.
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     */
    DataSet absolute(int rowIndex);

    /**
     * Retrieves a row from the DataSet as an array of Objects.
     * <br />
     * This method is typically used when accessing the data in a specific row.
     *
     * @param rowIndex The index of the row to retrieve. The first row is 0, the second is 1, and so on.
     * @return An array of Objects representing the data in the specified row.
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     */
    Object[] getRow(int rowIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves a row from the DataSet and converts it to a specific type.
     * <br />
     * This method is typically used when accessing the data in a specific row and converting it to a specific type.
     *
     * @param rowIndex The index of the row to retrieve. The first row is 0, the second is 1, and so on.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @param <T> The type to which the row data should be converted.
     * @return An instance of the specified type representing the data in the specified row.
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> T getRow(int rowIndex, Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Retrieves a row from the DataSet as an instance of the specified type.
     * <br />
     * This method is typically used when accessing the data in a specific row and converting it to a specific type.
     * The type can be an array, List, Set, Map, or a Bean.
     *
     * @param <T> The target type of the row.
     * @param rowIndex The index of the row to retrieve. The first row is 0, the second is 1, and so on.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return An instance of the specified type representing the data in the specified row.
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> T getRow(int rowIndex, Collection<String> columnNames, Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Retrieves a row from the DataSet as an instance of the specified type.
     * <br />
     * This method is typically used when accessing the data in a specific row and converting it to a specific type.
     * The type is determined by the provided IntFunction. It must be Object[], Collection, Map, or Bean class.
     *
     * @param <T> The target type of the row.
     * @param rowIndex The index of the row to retrieve. The first row is 0, the second is 1, and so on.
     * @param rowSupplier The IntFunction that generates an instance of the target type. It takes an integer as input, which is the number of columns in the row, and returns an instance of the target type.
     * @return An instance of the specified type representing the data in the specified row.
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> T getRow(int rowIndex, IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Retrieves a row from the DataSet as an instance of the specified type.
     * <br />
     * This method is typically used when accessing the data in a specific row and converting it to a specific type.
     * The type is determined by the provided IntFunction. It must be Object[], Collection, Map, or Bean class.
     * Only the columns specified in the {@code columnNames} collection will be included in the returned row.
     *
     * @param <T> The target type of the row.
     * @param rowIndex The index of the row to retrieve. The first row is 0, the second is 1, and so on.
     * @param columnNames The collection of column names to be included in the returned row.
     * @param rowSupplier The IntFunction that generates an instance of the target type. It takes an integer as input, which is the number of columns in the row, and returns an instance of the target type.
     * @return An instance of the specified type representing the data in the specified row.
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class, or if any of the specified {@code columnNames} does not exist in the DataSet.
     */
    <T> T getRow(int rowIndex, Collection<String> columnNames, IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Retrieves the first row from the DataSet as an Optional array of Objects.
     * <br />
     * This method is typically used when you need to access the first row of data in the DataSet.
     * If the DataSet is empty, the returned Optional will be empty.
     *
     * @return An Optional array of Objects representing the data in the first row. If the DataSet is empty, the Optional will be empty.
     */
    Optional<Object[]> firstRow();

    /**
     * Retrieves the first row from the DataSet as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the first row of data in the DataSet and convert it to a specific type.
     * The type can be an Object[], Collection, Map, or a Bean class.
     * If the DataSet is empty, the returned Optional will be empty.
     *
     * @param <T> The target type of the row.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return An Optional instance of the specified type representing the data in the first row. If the DataSet is empty, the Optional will be empty.
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> firstRow(Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Retrieves the first row from the DataSet as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the first row of data in the DataSet and convert it to a specific type.
     * The type is determined by the provided Class object. It can be an Object[], Collection, Map, or a Bean class.
     * Only the columns specified in the {@code columnNames} collection will be included in the returned row.
     * If the DataSet is empty, the returned Optional will be empty.
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the returned row.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return An Optional instance of the specified type representing the data in the first row. If the DataSet is empty, the Optional will be empty.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> firstRow(Collection<String> columnNames, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Retrieves the first row from the DataSet as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the first row of data in the DataSet and convert it to a specific type.
     * The type is determined by the provided IntFunction. It can be an Object[], Collection, Map, or a Bean class.
     * If the DataSet is empty, the returned Optional will be empty.
     *
     * @param <T> The target type of the row.
     * @param rowSupplier The IntFunction that generates an instance of the target type. It takes an integer as input, which is the number of columns in the row, and returns an instance of the target type.
     * @return An Optional instance of the specified type representing the data in the first row. If the DataSet is empty, the Optional will be empty.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> firstRow(IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Retrieves the first row from the DataSet as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the first row of data in the DataSet and convert it to a specific type.
     * The type is determined by the provided IntFunction. It can be an Object[], Collection, Map, or a Bean class.
     * Only the columns specified in the {@code columnNames} collection will be included in the returned row.
     * If the DataSet is empty, the returned Optional will be empty.
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the returned row.
     * @param rowSupplier The IntFunction that generates an instance of the target type. It takes an integer as input, which is the number of columns in the row, and returns an instance of the target type.
     * @return An Optional instance of the specified type representing the data in the first row. If the DataSet is empty, the Optional will be empty.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> firstRow(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Retrieves the last row from the DataSet as an Optional array of Objects.
     * <br />
     * This method is typically used when you need to access the last row of data in the DataSet.
     * If the DataSet is empty, the returned Optional will be empty.
     *
     * @return An Optional array of Objects representing the data in the last row. If the DataSet is empty, the Optional will be empty.
     */
    Optional<Object[]> lastRow();

    /**
     * Retrieves the last row from the DataSet as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the last row of data in the DataSet and convert it to a specific type.
     * The type can be an Object[], Collection, Map, or a Bean class.
     * If the DataSet is empty, the returned Optional will be empty.
     *
     * @param <T> The target type of the row.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return An Optional instance of the specified type representing the data in the last row. If the DataSet is empty, the Optional will be empty.
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> lastRow(Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Retrieves the last row from the DataSet as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the last row of data in the DataSet and convert it to a specific type.
     * The type can be an Object[], Collection, Map, or a Bean class.
     * Only the columns specified in the {@code columnNames} collection will be included in the returned row.
     * If the DataSet is empty, the returned Optional will be empty.
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the returned row.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return An Optional instance of the specified type representing the data in the last row. If the DataSet is empty, the Optional will be empty.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> lastRow(Collection<String> columnNames, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Retrieves the last row from the DataSet as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the last row of data in the DataSet and convert it to a specific type.
     * The type is determined by the provided IntFunction. It can be an Object[], Collection, Map, or a Bean class.
     * If the DataSet is empty, the returned Optional will be empty.
     *
     * @param <T> The target type of the row.
     * @param rowSupplier The IntFunction that generates an instance of the target type. It takes an integer as input, which is the number of columns in the row, and returns an instance of the target type.
     * @return An Optional instance of the specified type representing the data in the last row. If the DataSet is empty, the Optional will be empty.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> lastRow(IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Retrieves the last row from the DataSet as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the last row of data in the DataSet and convert it to a specific type.
     * The type is determined by the provided IntFunction. It can be an Object[], Collection, Map, or a Bean class.
     * Only the columns specified in the {@code columnNames} collection will be included in the returned row.
     * If the DataSet is empty, the returned Optional will be empty.
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the returned row.
     * @param rowSupplier The IntFunction that generates an instance of the target type. It takes an integer as input, which is the number of columns in the row, and returns an instance of the target type.
     * @return An Optional instance of the specified type representing the data in the last row. If the DataSet is empty, the Optional will be empty.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> lastRow(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Performs the given action for each row of the DataSet until all rows.
     * <br />
     * This method is typically used when you need to perform an operation on each row in the DataSet.
     * The action is a Consumer function that takes a DisposableObjArray as input, which represents a row in the DataSet.
     * The action is applied to each row in the DataSet in the order they appear.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param action The action to be performed on each row. It takes a DisposableObjArray as input, which represents a row in the DataSet. The action should not cache or update the input DisposableObjArray or its values(Array).
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(Throwables.Consumer<? super DisposableObjArray, E> action) throws E;

    /**
     * Performs the given action for each row of the DataSet until all rows.
     * <br />
     * This method is typically used when you need to perform an operation on each row in the DataSet.
     * The action is a Consumer function that takes a DisposableObjArray as input, which represents a row in the DataSet.
     * The action is applied to each row in the DataSet in the order they appear.
     * Only the columns specified in the {@code columnNames} collection will be included in the DisposableObjArray.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param columnNames The collection of column names to be included in the DisposableObjArray.
     * @param action The action to be performed on each row. It takes a DisposableObjArray as input, which represents a row in the DataSet. The action should not cache or update the input DisposableObjArray or its values(Array).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(Collection<String> columnNames, Throwables.Consumer<? super DisposableObjArray, E> action)
            throws IllegalArgumentException, E;

    /**
     * Performs the given action for each row of the DataSet within the specified range until all rows.
     * <br />
     * This method is typically used when you need to perform an operation on a specific range of rows in the DataSet.
     * The action is a Consumer function that takes a DisposableObjArray as input, which represents a row in the DataSet.
     * The action is applied to each row in the DataSet in the order they appear, starting from the row at the index specified by {@code fromRowIndex} and ending at the row before the index specified by {@code toRowIndex}.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param fromRowIndex The starting index of the range of rows to be processed. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be processed. This index is exclusive, meaning the row at this index will not be processed.
     * @param action The action to be performed on each row. It takes a DisposableObjArray as input, which represents a row in the DataSet. The action should not cache or update the input DisposableObjArray or its values(Array).
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Throwables.Consumer<? super DisposableObjArray, E> action)
            throws IndexOutOfBoundsException, E;

    /**
     * Performs the given action for each row of the DataSet within the specified range.
     * <br />
     * This method is typically used when you need to perform an operation on a specific range of rows in the DataSet.
     * The action is a Consumer function that takes a DisposableObjArray as input, which represents a row in the DataSet.
     * The action is applied to each row in the DataSet in the order they appear, starting from the row at the index specified by {@code fromRowIndex} and ending at the row before the index specified by {@code toRowIndex}.
     * Only the columns specified in the {@code columnNames} collection will be included in the DisposableObjArray.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param fromRowIndex The starting index of the range of rows to be processed. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be processed. This index is exclusive, meaning the row at this index will not be processed.
     * @param columnNames The collection of column names to be included in the DisposableObjArray.
     * @param action The action to be performed on each row. It takes a DisposableObjArray as input, which represents a row in the DataSet. The action should not cache or update the input DisposableObjArray or its values(Array).
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Collection<String> columnNames,
            Throwables.Consumer<? super DisposableObjArray, E> action) throws IndexOutOfBoundsException, IllegalArgumentException, E;

    /**
     * Performs the given action for each row of the DataSet.
     * <br />
     * This method is typically used when you need to perform an operation on each row in the DataSet.
     * The action is a BiConsumer function that takes two inputs, which represent the values of the two columns specified in the Tuple {@code columnNames}.
     * The action is applied to each row in the DataSet in the order they appear.
     * Only the columns specified in the Tuple {@code columnNames} will be included in the action.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param columnNames A Tuple2 representing the names of the two columns to be included in the action.
     * @param action The action to be performed on each row. It takes two inputs, which represent the values of the two columns specified in the Tuple {@code columnNames}. The action should not cache or update the input values.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(Tuple2<String, String> columnNames, Throwables.BiConsumer<?, ?, E> action) throws IllegalArgumentException, E;

    /**
     * Performs the given action for each row of the DataSet within the specified range.
     * <br />
     * This method is typically used when you need to perform an operation on a specific range of rows in the DataSet.
     * The action is a BiConsumer function that takes two inputs, which represent the values of the two columns specified in the Tuple {@code columnNames}.
     * The action is applied to each row in the DataSet in the order they appear, starting from the row at the index specified by {@code fromRowIndex} and ending at the row before the index specified by {@code toRowIndex}.
     * Only the columns specified in the Tuple {@code columnNames} will be included in the action.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param fromRowIndex The starting index of the range of rows to be processed. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be processed. This index is exclusive, meaning the row at this index will not be processed.
     * @param columnNames A Tuple2 representing the names of the two columns to be included in the action.
     * @param action The action to be performed on each row. It takes two inputs, which represent the values of the two columns specified in the Tuple {@code columnNames}. The action should not cache or update the input values.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, Throwables.BiConsumer<?, ?, E> action)
            throws IndexOutOfBoundsException, IllegalArgumentException, E;

    /**
     * Performs the given action for each row of the DataSet.
     * <br />
     * This method is typically used when you need to perform an operation on each row in the DataSet.
     * The action is a TriConsumer function that takes three inputs, which represent the values of the three columns specified in the Tuple {@code columnNames}.
     * The action is applied to each row in the DataSet in the order they appear.
     * Only the columns specified in the Tuple {@code columnNames} will be included in the action.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param columnNames A Tuple3 representing the names of the three columns to be included in the action.
     * @param action The action to be performed on each row. It takes three inputs, which represent the values of the three columns specified in the Tuple {@code columnNames}. The action should not cache or update the input values.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(Tuple3<String, String, String> columnNames, Throwables.TriConsumer<?, ?, ?, E> action)
            throws IllegalArgumentException, E;

    /**
     * Performs the given action for each row of the DataSet within the specified range.
     * <br />
     * This method is typically used when you need to perform an operation on a specific range of rows in the DataSet.
     * The action is a TriConsumer function that takes three inputs, which represent the values of the three columns specified in the Tuple {@code columnNames}.
     * The action is applied to each row in the DataSet in the order they appear, starting from the row at the index specified by {@code fromRowIndex} and ending at the row before the index specified by {@code toRowIndex}.
     * Only the columns specified in the Tuple {@code columnNames} will be included in the action.
     *
     * @param <E> The type of the exception that the action can throw.
     * @param fromRowIndex The starting index of the range of rows to be processed. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be processed. This index is exclusive, meaning the row at this index will not be processed.
     * @param columnNames A Tuple3 representing the names of the three columns to be included in the action.
     * @param action The action to be performed on each row. It takes three inputs, which represent the values of the three columns specified in the Tuple {@code columnNames}. The action should not cache or update the input values.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, Throwables.TriConsumer<?, ?, ?, E> action)
            throws IndexOutOfBoundsException, IllegalArgumentException, E;

    /**
     * Converts the entire DataSet into a list of Object arrays.
     * <br />
     * This method is typically used when you need to export the data in the DataSet to a different format or system.
     * Each row in the DataSet is converted into an Object array, where each element in the array corresponds to a column in the row.
     * The order of the elements in the array matches the order of the columns in the DataSet.
     * The resulting list of Object arrays is in the same order as the rows in the DataSet.
     *
     * @return A List of Object arrays representing the data in the DataSet. Each Object array is a row in the DataSet.
     */
    List<Object[]> toList();

    /**
     * Converts a specified range of the DataSet into a list of Object arrays.
     * <br />
     * This method is typically used when you need to export a specific range of data in the DataSet to a different format or system.
     * Each row in the specified range of the DataSet is converted into an Object array, where each element in the array corresponds to a column in the row.
     * The order of the elements in the array matches the order of the columns in the DataSet.
     * The resulting list of Object arrays is in the same order as the rows in the DataSet.
     *
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @return A List of Object arrays representing the data in the specified range of the DataSet. Each Object array is a row in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     */
    List<Object[]> toList(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type.
     * <br />
     * This method is typically used when you need to export the data in the DataSet to a specific type of objects.
     * Each row in the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a row in the DataSet.
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Converts a specified range of the DataSet into a list of instances of the specified type.
     * <br />
     * This method is typically used when you need to export a specific range of data in the DataSet to a specific type of objects.
     * Each row in the specified range of the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return A List of instances of the specified type representing the data in the specified range of the DataSet. Each instance is a row in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type, including only the specified columns.
     * <br />
     * This method is typically used when you need to export specific columns of data in the DataSet to a specific type of objects.
     * Each row in the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns specified in the {@code columnNames} collection will be included in the instance.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the instance.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a row in the DataSet.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty, or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(Collection<String> columnNames, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Converts a specified range of the DataSet into a list of instances of the specified type, including only the specified columns.
     * <br />
     * This method is typically used when you need to export a specific range of data and specific columns of data in the DataSet to a specific type of objects.
     * Each row in the specified range of the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns specified in the {@code columnNames} collection will be included in the instance.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param columnNames The collection of column names to be included in the instance.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return A List of instances of the specified type representing the data in the specified range of the DataSet. Each instance is a row in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty, or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Class<? extends T> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export the data in the DataSet to a specific type of objects.
     * Each row in the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param rowSupplier The function to create a new instance of the target type. It takes an integer as input, which represents the number of columns in the DataSet.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a row in the DataSet.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Converts a specified range of the DataSet into a list of instances of the specified type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export a specific range of data in the DataSet to a specific type of objects.
     * Each row in the specified range of the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param rowSupplier The function to create a new instance of the target type. It takes an integer as input, which represents the number of columns in the DataSet.
     * @return A List of instances of the specified type representing the data in the specified range of the DataSet. Each instance is a row in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the specified columns.
     * <br />
     * This method is typically used when you need to export specific columns of data in the DataSet to a specific type of objects.
     * Each row in the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns specified in the {@code columnNames} collection will be included in the instance.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the instance.
     * @param rowSupplier The function to create a new instance of the target type. It takes an integer as input, which represents the number of columns in the DataSet.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a row in the DataSet.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Converts a specified range of the DataSet into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the specified columns.
     * <br />
     * This method is typically used when you need to export a specific range of data and specific columns of data in the DataSet to a specific type of objects.
     * Each row in the specified range of the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns specified in the {@code columnNames} collection will be included in the instance.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param columnNames The collection of column names to be included in the instance.
     * @param rowSupplier The function to create a new instance of the target type. It takes an integer as input, which represents the number of columns in the DataSet.
     * @return A List of instances of the specified type representing the data in the specified range of the DataSet. Each instance is a row in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Collection<String> columnNames, IntFunction<? extends T> rowSupplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the columns that pass the specified filter.
     * <br />
     * This method is typically used when you need to export specific columns of data in the DataSet to a specific type of objects.
     * Each row in the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns that pass the {@code columnNameFilter} will be included in the instance.
     * The names of the properties in the instance are determined by the {@code columnNameConverter}.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param columnNameFilter The predicate to filter the column names. Only the columns that pass this filter will be included in the instance.
     * @param columnNameConverter The function to convert the column names into property names in the instance.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a row in the DataSet.
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter, Class<? extends T> rowType)
            throws IllegalArgumentException;

    /**
     * Converts a specified range of the DataSet into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the columns that pass the specified filter.
     * <br />
     * This method is typically used when you need to export a specific range of data and specific columns of data in the DataSet to a specific type of objects.
     * Each row in the specified range of the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns that pass the {@code columnNameFilter} will be included in the instance.
     * The names of the properties in the instance are determined by the {@code columnNameConverter}.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param columnNameFilter The predicate to filter the column names. Only the columns that pass this filter will be included in the instance.
     * @param columnNameConverter The function to convert the column names into property names in the instance.
     * @return A List of instances of the specified type representing the data in the specified range of the DataSet. Each instance is a row in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter,
            Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the columns that pass the specified filter.
     * <br />
     * This method is typically used when you need to export specific columns of data in the DataSet to a specific type of objects.
     * Each row in the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns that pass the {@code columnNameFilter} will be included in the instance.
     * The names of the properties in the instance are determined by the {@code columnNameConverter}.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param columnNameFilter The predicate to filter the column names. Only the columns that pass this filter will be included in the instance.
     * @param columnNameConverter The function to convert the column names into property names in the instance.
     * @param rowSupplier The function to create a new instance of the target type. It takes an integer as input, which represents the number of columns in the DataSet.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a row in the DataSet.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter, IntFunction<? extends T> rowSupplier);

    /**
     * Converts a specified range of the DataSet into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the columns that pass the specified filter.
     * <br />
     * This method is typically used when you need to export a specific range of data and specific columns of data in the DataSet to a specific type of objects.
     * Each row in the specified range of the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns that pass the {@code columnNameFilter} will be included in the instance.
     * The names of the properties in the instance are determined by the {@code columnNameConverter}.
     * The order of the properties in the instance matches the order of the columns in the DataSet.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param columnNameFilter The predicate to filter the column names. Only the columns that pass this filter will be included in the instance.
     * @param columnNameConverter The function to convert the column names into property names in the instance.
     * @return A List of instances of the specified type representing the data in the specified range of the DataSet. Each instance is a row in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter,
            IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type - Bean class, mapping column names to field names based on the provided map.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a specific type of objects (entities), and the column names in the DataSet do not directly match the field names in the entity class.
     * Each row in the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The mapping between column names and field names is determined by the {@code prefixAndFieldNameMap}.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param prefixAndFieldNameMap The map that defines the mapping between column names and field names. The key is the column name prefix, and the value is the corresponding field name.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a row in the DataSet.
     * @throws IllegalArgumentException if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toEntities(Map<String, String> prefixAndFieldNameMap, Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts a specified range of the DataSet into a list of instances of the specified type - Bean class, mapping column names to field names based on the provided map.
     * <br />
     * This method is typically used when you need to export a specific range of data in the DataSet to a specific type of objects (entities), and the column names in the DataSet do not directly match the field names in the entity class.
     * Each row in the specified range of the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The mapping between column names and field names is determined by the {@code prefixAndFieldNameMap}.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param prefixAndFieldNameMap The map that defines the mapping between column names and field names. The key is the column name prefix, and the value is the corresponding field name.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the specified range of the DataSet. Each instance is a row in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toEntities(int fromRowIndex, int toRowIndex, Map<String, String> prefixAndFieldNameMap, Class<? extends T> beanClass)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type - Bean class, mapping column names to field names based on the provided map.
     * <br />
     * This method is typically used when you need to export specific columns of data in the DataSet to a specific type of objects (entities), and the column names in the DataSet do not directly match the field names in the entity class.
     * Each row in the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The mapping between column names and field names is determined by the {@code prefixAndFieldNameMap}.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the instance.
     * @param prefixAndFieldNameMap The map that defines the mapping between column names and field names. The key is the column name prefix, and the value is the corresponding field name.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a row in the DataSet.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty, or if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toEntities(Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap, Class<? extends T> beanClass)
            throws IllegalArgumentException;

    /**
     * Converts a specified range of the DataSet into a list of instances of the specified type - Bean class, including only the specified columns, mapping column names to field names based on the provided map.
     * <br />
     * This method is typically used when you need to export a specific range of data and specific columns of data in the DataSet to a specific type of objects (entities), and the column names in the DataSet do not directly match the field names in the entity class.
     * Each row in the specified range of the DataSet is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns specified in the {@code columnNames} collection will be included in the instance.
     * The mapping between column names and field names is determined by the {@code prefixAndFieldNameMap}.
     * The resulting list of instances is in the same order as the rows in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param columnNames The collection of column names to be included in the instance.
     * @param prefixAndFieldNameMap The map that defines the mapping between column names and field names. The key is the column name prefix, and the value is the corresponding field name.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the specified range of the DataSet. Each instance is a row in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty, or if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toEntities(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> beanClass) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type - Bean class, merging rows with the same ID into a single instance.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a specific type of objects (entities), and the rows in the DataSet have duplicate IDs.
     * Each unique ID in the DataSet corresponds to an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Rows with the same ID are merged into a single instance, with the properties of the instance being the union of the properties of the rows.
     * The resulting list of instances is in the same order as the unique IDs in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a merged entity in the DataSet.
     * @throws IllegalArgumentException if the specified {@code beanClass} is not a supported type - Bean class or no id defined in {@code beanClass}.
     */
    <T> List<T> toMergedEntities(Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type - Bean class, merging rows with the same ID into a single instance.
     * <br />
     * This method is typically used when you need to export specific columns of data in the DataSet to a specific type of objects (entities), and the rows in the DataSet have duplicate IDs.
     * Each unique ID in the DataSet corresponds to an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Rows with the same ID are merged into a single instance, with the properties of the instance being the union of the properties of the rows.
     * The resulting list of instances is in the same order as the unique IDs in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param selectPropNames The collection of property names to be included in the instance.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a merged entity in the DataSet.
     * @throws IllegalArgumentException if any of the specified property names does not exist in the DataSet or {@code selectPropNames} is empty, or if the specified {@code beanClass} is not a supported type - Bean class or no id defined in {@code beanClass}.
     */
    <T> List<T> toMergedEntities(Collection<String> selectPropNames, Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type - Bean class, merging rows with the same ID into a single instance.
     * <br />
     * This method is typically used when you need to export specific columns of data in the DataSet to a specific type of objects (entities), and the rows in the DataSet have duplicate IDs.
     * Each unique ID in the DataSet corresponds to an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Rows with the same ID are merged into a single instance, with the properties of the instance being the union of the properties of the rows.
     * The resulting list of instances is in the same order as the unique IDs in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param idPropName The property name that is used as the ID for merging rows. Rows with the same ID will be merged into a single instance.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a merged entity in the DataSet.
     * @throws IllegalArgumentException if the specified {@code idPropName} does not exist in the DataSet or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toMergedEntities(String idPropName, Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type - Bean class, merging rows with the same ID into a single instance.
     * <br />
     * This method is typically used when you need to export specific columns of data in the DataSet to a specific type of objects (entities), and the rows in the DataSet have duplicate IDs.
     * Each unique ID in the DataSet corresponds to an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Rows with the same ID are merged into a single instance, with the properties of the instance being the union of the properties of the rows.
     * The resulting list of instances is in the same order as the unique IDs in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param idPropName The property name that is used as the ID for merging rows. Rows with the same ID will be merged into a single instance.
     * @param selectPropNames The collection of property names to be included in the instance.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a merged entity in the DataSet.
     * @throws IllegalArgumentException if the specified {@code idPropName} does not exist in the DataSet or {@code idPropNames} is empty, or if any of the specified property names does not exist in the DataSet or {@code selectPropNames} is empty, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toMergedEntities(String idPropName, Collection<String> selectPropNames, Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified type - Bean class, merging rows with the same IDs into a single instance.
     * <br />
     * This method is typically used when you need to export specific columns of data in the DataSet to a specific type of objects (entities), and the rows in the DataSet have duplicate IDs.
     * Each unique ID in the DataSet corresponds to an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Rows with the same IDs are merged into a single instance, with the properties of the instance being the union of the properties of the rows.
     * The resulting list of instances is in the same order as the unique IDs in the DataSet.
     *
     * @param <T> The target type of the row.
     * @param idPropNames The collection of property names that are used as the IDs for merging rows. Rows with the same IDs will be merged into a single instance.
     * @param selectPropNames The collection of property names to be included in the instance.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the DataSet. Each instance is a merged entity in the DataSet.
     * @throws IllegalArgumentException if any of the specified ID property names does not exist in the DataSet or {@code idPropNames} is empty, or if any of the specified property names does not exist in the DataSet or {@code selectPropNames} is empty, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toMergedEntities(Collection<String> idPropNames, Collection<String> selectPropNames, Class<? extends T> beanClass)
            throws IllegalArgumentException;

    /**
     * Converts the entire DataSet into a list of instances of the specified bean class,
     * merging rows that share the same ID properties into a single entity.
     * <p>
     * This method is commonly used when exporting selected columns from a DataSet into a list of typed objects (entities),
     * especially when the DataSet contains multiple rows with the same ID values.
     * <p>
     * Each unique combination of ID property values results in one merged instance of the target type. 
     * The properties of each instance represent a union of the properties from all matching rows.
     * The order of the resulting list corresponds to the order of unique IDs in the DataSet.
     *
     * @param <T> the target bean type.
     * @param idPropNames the collection of property names used to identify and group rows. 
     *                    Rows with matching values for these properties will be merged into one instance.
     * @param selectPropNames the collection of property names to include in the resulting instances.
     * @param prefixAndFieldNameMap an optional mapping of column name prefixes to field names in the bean. 
     *                               This supports column headers that are prefixed.
     * @param beanClass the class representing the bean type. Must be a valid JavaBean.
     * @return a list of merged entities of the specified type, based on the DataSet content.
     * @throws IllegalArgumentException if {@code idPropNames} is null or empty, if any specified ID or selected property
     *                                  name does not exist in the DataSet, if the {@code prefixAndFieldNameMap} is invalid,
     *                                  or if {@code beanClass} is not a supported JavaBean class.
     */
    <T> List<T> toMergedEntities(Collection<String> idPropNames, Collection<String> selectPropNames, Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts the entire DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The value of each entry is the value of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the DataSet that will be used as the values in the resulting map.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the DataSet.
     */
    <K, V> Map<K, V> toMap(String keyColumnName, String valueColumnName) throws IllegalArgumentException;

    /**
     * Converts the entire DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The value of each entry is the value of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the DataSet that will be used as the values in the resulting map.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the DataSet.
     */
    <K, V, M extends Map<K, V>> M toMap(String keyColumnName, String valueColumnName, IntFunction<? extends M> supplier) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The value of each entry is the value of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the DataSet that will be used as the values in the resulting map.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the DataSet.
     */
    <K, V> Map<K, V> toMap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The value of each entry is the value of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the DataSet that will be used as the values in the resulting map.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the DataSet.
     */
    <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName, IntFunction<? extends M> supplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the DataSet that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowType The Class object representing the type of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V> Map<K, V> toMap(String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType) throws IllegalArgumentException;

    /**
     * Converts the entire DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the DataSet that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowType The Class object representing the type of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V, M extends Map<K, V>> M toMap(String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType,
            IntFunction<? extends M> supplier) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the DataSet that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowType The Class object representing the type of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V> Map<K, V> toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the DataSet that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowType The Class object representing the type of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType,
            IntFunction<? extends M> supplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the DataSet that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V> Map<K, V> toMap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends V> rowSupplier) throws IllegalArgumentException;

    /**
     * Converts the entire DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the DataSet that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V, M extends Map<K, V>> M toMap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends V> rowSupplier,
            IntFunction<? extends M> supplier) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the DataSet that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V> Map<K, V> toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends V> rowSupplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a Map, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a Map, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting map does not preserve the order of the rows in the DataSet.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the DataSet that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends V> rowSupplier, IntFunction<? extends M> supplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a ListMultimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a ListMultimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting ListMultimap does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the DataSet that will be used as the values in the resulting map.
     * @return A ListMultimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the DataSet.
     */
    <K, T> ListMultimap<K, T> toMultimap(String keyColumnName, String valueColumnName) throws IllegalArgumentException;

    /**
     * Converts the entire DataSet into a Multimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a Multimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting Multimap does not preserve the order of the rows in the DataSet.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the DataSet that will be used as the values in the resulting map.
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the DataSet.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(String keyColumnName, String valueColumnName, IntFunction<? extends M> supplier)
            throws IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a ListMultimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a ListMultimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting ListMultimap does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the DataSet that will be used as the values in the resulting map.
     * @return A ListMultimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the DataSet.
     */
    <K, T> ListMultimap<K, T> toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a Multimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a Multimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting Multimap does not preserve the order of the rows in the DataSet.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the DataSet that will be used as the values in the resulting map.
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName,
            IntFunction<? extends M> supplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a ListMultimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a ListMultimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting ListMultimap does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the DataSet that will be used as the values in the resulting map.
     * @param rowType The class of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @return A ListMultimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T> ListMultimap<K, T> toMultimap(String keyColumnName, Collection<String> valueColumnNames, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Converts the entire DataSet into a Multimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export data in the DataSet to a Multimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting Multimap does not preserve the order of the rows in the DataSet.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the DataSet that will be used as the values in the resulting map.
     * @param rowType The class of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(String keyColumnName, Collection<String> valueColumnNames,
            Class<? extends T> rowType, IntFunction<? extends M> supplier) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a ListMultimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a ListMultimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting ListMultimap does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the DataSet that will be used as the values in the resulting map.
     * @param rowType The class of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @return A ListMultimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T> ListMultimap<K, T> toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a Multimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a Multimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting Multimap does not preserve the order of the rows in the DataSet.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the DataSet that will be used as the values in the resulting map.
     * @param rowType The class of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName,
            Collection<String> valueColumnNames, Class<? extends T> rowType, IntFunction<? extends M> supplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a ListMultimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a ListMultimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting ListMultimap does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the DataSet that will be used as the values in the resulting map.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity. The return value created by specified {@code rowSupplier} must be an Object[], Collection, Map, or Bean class
     * @return A ListMultimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T> ListMultimap<K, T> toMultimap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends T> rowSupplier)
            throws IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a Multimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a Multimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting Multimap does not preserve the order of the rows in the DataSet.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the DataSet that will be used as the values in the resulting map.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity. The return value created by specified {@code rowSupplier} must be an Object[], Collection, Map, or Bean class
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends T> rowSupplier, IntFunction<? extends M> supplier) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a ListMultimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a ListMultimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting ListMultimap does not preserve the order of the rows in the DataSet.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the DataSet that will be used as the values in the resulting map.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity. The return value created by specified {@code rowSupplier} must be an Object[], Collection, Map, or Bean class
     * @return A ListMultimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T> ListMultimap<K, T> toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into a Multimap, where each entry in the map corresponds to a row in the DataSet.
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the DataSet to a Multimap, where each key-value pair in the map corresponds to a row in the DataSet.
     * The resulting Multimap does not preserve the order of the rows in the DataSet.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the DataSet that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the DataSet that will be used as the values in the resulting map.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity. The return value created by specified {@code rowSupplier} must be an Object[], Collection, Map, or Bean class
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the DataSet. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the DataSet, or if any of the specified value column names does not exist in the DataSet or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName,
            Collection<String> valueColumnNames, IntFunction<? extends T> rowSupplier, IntFunction<? extends M> supplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a JSON string.
     * <br />
     * This method is typically used when you need to export the data in the DataSet to a JSON format.
     * The resulting JSON string represents the entire DataSet, including all rows and columns.
     * The order of the rows in the JSON string is the same as the order of the rows in the DataSet.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @return A JSON string representing the current DataSet.
     * @see #toJson(int, int, Collection)
     */
    String toJson();

    /**
     * Converts a range of rows in the DataSet into a JSON string.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to a JSON format.
     * The resulting JSON string represents the specified range of rows in the DataSet.
     * The order of the rows in the JSON string is the same as the order of the rows in the DataSet.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the JSON string.
     * @param toRowIndex The ending index of the row range to be included in the JSON string.
     * @return A JSON string representing the specified range of rows in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     */
    String toJson(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Converts a range of rows in the DataSet into a JSON string, including only the specified columns.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to a JSON format.
     * The resulting JSON string represents the specified range of rows and columns in the DataSet.
     * The order of the rows in the JSON string is the same as the order of the rows in the DataSet.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the JSON string.
     * @param toRowIndex The ending index of the row range to be included in the JSON string.
     * @param columnNames The names of the columns in the DataSet to be included in the JSON string.
     * @return A JSON string representing the specified range of rows and columns in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     */
    String toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a JSON string and writes it to the provided File.
     *
     * @param output
     * @throws UncheckedIOException if an I/O error occurs while writing to the File.
     * @see #toJson(int, int, Collection, File)
     */
    void toJson(File output) throws UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into a JSON string and writes it to the provided File.
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws UncheckedIOException if an I/O error occurs while writing to the File.
     * @see #toJson(int, int, Collection, File)
     */
    void toJson(int fromRowIndex, int toRowIndex, File output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into a JSON string, including only the specified columns, and writes it to the provided File.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to a JSON format and write it directly to a File.
     * The resulting JSON string represents the specified range of rows and columns in the DataSet.
     * The order of the rows in the JSON string is the same as the order of the rows in the DataSet.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the JSON string.
     * @param toRowIndex The ending index of the row range to be included in the JSON string.
     * @param columnNames The names of the columns in the DataSet to be included in the JSON string.
     * @param output The File where the JSON string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs while writing to the File.
     */
    void toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames, File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire DataSet into a JSON string and writes it to the provided OutputStream.
     *
     * @param output
     * @throws UncheckedIOException if an I/O error occurs while writing to the OutputStream.
     * @see #toJson(int, int, Collection, OutputStream)
     */
    void toJson(OutputStream output) throws UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into a JSON string and writes it to the provided OutputStream.
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws UncheckedIOException if an I/O error occurs while writing to the OutputStream.
     * @see #toJson(int, int, Collection, OutputStream
     */
    void toJson(int fromRowIndex, int toRowIndex, OutputStream output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into a JSON string, including only the specified columns, and writes it to the provided OutputStream.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to a JSON format and write it directly to an OutputStream, such as a FileOutputStream for writing to a file.
     * The resulting JSON string represents the specified range of rows and columns in the DataSet.
     * The order of the rows in the JSON string is the same as the order of the rows in the DataSet.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the JSON string.
     * @param toRowIndex The ending index of the row range to be included in the JSON string.
     * @param columnNames The names of the columns in the DataSet to be included in the JSON string.
     * @param output The OutputStream where the JSON string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs while writing to the OutputStream.
     */
    void toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames, OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire DataSet into a JSON string and writes it to the provided Writer.
     *
     * @param output
     * @throws UncheckedIOException
     * @see #toJson(int, int, Collection, Writer)
     */
    void toJson(Writer output) throws UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into a JSON string and writes it to the provided Writer.
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     * @throws IndexOutOfBoundsException
     * @throws UncheckedIOException
     * @see #toJson(int, int, Collection, Writer)
     */
    void toJson(int fromRowIndex, int toRowIndex, Writer output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into a JSON string, including only the specified columns, and writes it to the provided Writer.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to a JSON format and write it directly to a Writer, such as a FileWriter for writing to a file.
     * The resulting JSON string represents the specified range of rows and columns in the DataSet.
     * The order of the rows in the JSON string is the same as the order of the rows in the DataSet.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the JSON string.
     * @param toRowIndex The ending index of the row range to be included in the JSON string.
     * @param columnNames The names of the columns in the DataSet to be included in the JSON string.
     * @param output The Writer where the JSON string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs while writing to the Writer.
     */
    void toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire DataSet into an XML string, with each row represented as an XML element with the specified name, and returns it as a String.
     *
     * @return A String containing the XML representation of the DataSet.
     * @see #toXml(int, int, Collection, String)
     */
    String toXml();

    /**
     * Converts the entire DataSet into an XML string, with each row represented as an XML element with the specified name, and returns it as a String.
     * <br />
     * This method is typically used when you need to export the data in the DataSet to an XML format.
     * The resulting XML string represents the entire DataSet.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @return A String containing the XML representation of the DataSet.
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @see #toXml(int, int, Collection, String)
     */
    String toXml(String rowElementName) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into an XML string and returns it as a String.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows in the DataSet.
     * Each row in the DataSet is represented as an XML element with a default name.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     * <br />
     * Note: The method uses the default settings for XML serialization. If you need more control over the XML output (e.g., custom element names, namespaces, etc.), consider using the overloaded method with more parameters.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @return A String containing the XML representation of the specified range of rows in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @see #toXml(int, int, Collection, String)
     */
    String toXml(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Converts a range of rows in the DataSet into an XML string, with each row represented as an XML element with the specified name, and returns it as a String.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows in the DataSet.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @return A String containing the XML representation of the specified range of rows in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @see #toXml(int, int, Collection, String)
     */
    String toXml(int fromRowIndex, int toRowIndex, String rowElementName) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into an XML string, with each row represented as an XML element with a default name, and returns it as a String.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows in the DataSet.
     * Each row in the DataSet is represented as an XML element with a default name.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The collection of column names to be included in the XML string.
     * @return A String containing the XML representation of the specified range of rows in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty
     * @see #toXml(int, int, Collection, String)
     */
    String toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the DataSet into an XML string, including only the specified columns, and returns it as a String.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows and columns in the DataSet.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The names of the columns in the DataSet to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @return A String containing the XML representation of the specified range of rows and columns in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty, or if {@code rowElementName} is empty.
     */
    String toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Writes the entire DataSet as an XML string to the specified File.
     * <br />
     * This method is typically used when you need to export the entire data in the DataSet to an XML format.
     * The resulting XML string represents all the rows in the DataSet.
     * Each row in the DataSet is represented as an XML element with a default name.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param output The File where the XML string will be written.
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     * @see #toXml(int, int, Collection, String, File)
     */
    void toXml(File output) throws UncheckedIOException;

    /**
     * Writes the entire DataSet as an XML string to the specified File, with each row represented as an XML element with the specified name.
     * <br />
     * This method is typically used when you need to export the entire data in the DataSet to an XML format.
     * The resulting XML string represents all the rows in the DataSet.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @param output The File where the XML string will be written.
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     * @see #toXml(int, int, Collection, String, File)
     */
    void toXml(String rowElementName, File output) throws IllegalArgumentException, UncheckedIOException;

    /**
     * Writes a range of rows in the DataSet as an XML string to the specified File.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows in the DataSet.
     * Each row in the DataSet is represented as an XML element with a default name.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param output The File where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     * @see #toXml(int, int, Collection, String, File)
     */
    void toXml(int fromRowIndex, int toRowIndex, File output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Writes a range of rows in the DataSet as an XML string to the specified File, with each row represented as an XML element with the specified name.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows in the DataSet.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @param output The File where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     * @see #toXml(int, int, Collection, String, File)
     */
    void toXml(int fromRowIndex, int toRowIndex, String rowElementName, File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into an XML string, including only the specified columns, and writes it to the specified File.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The names of the columns in the DataSet to be included in the XML string.
     * @param output The File where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     * @see #toXml(int, int, Collection, String, File)
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into an XML string, including only the specified columns, and writes it to the specified File.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows and columns in the DataSet.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The names of the columns in the DataSet to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @param output The File where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty, or if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName, File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire DataSet into an XML string and writes it to the specified OutputStream.
     * Each row in the DataSet is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export the entire data in the DataSet to an XML format.
     * The resulting XML string represents the entire DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param output The OutputStream where the XML string will be written.
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     * @see #toXml(int, int, Collection, String, OutputStream)
     */
    void toXml(OutputStream output) throws UncheckedIOException;

    /**
     * Converts the entire DataSet into an XML string, using the specified row element name, and writes it to the specified OutputStream.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export the data in the DataSet to an XML format.
     * The resulting XML string represents the entire DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @param output The OutputStream where the XML string will be written.
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     * @see #toXml(int, int, Collection, String, OutputStream)
     */
    void toXml(String rowElementName, OutputStream output) throws UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into an XML string and writes it to the specified OutputStream.
     * Each row in the DataSet is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows in the DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param output The OutputStream where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     * @see #toXml(int, int, Collection, String, OutputStream)
     */
    void toXml(int fromRowIndex, int toRowIndex, OutputStream output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into an XML string, using the specified row element name, and writes it to the specified OutputStream.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows in the DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @param output The OutputStream where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     * @see #toXml(int, int, Collection, String, OutputStream)
     */
    void toXml(int fromRowIndex, int toRowIndex, String rowElementName, OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into an XML string, including only the specified columns, and writes it to the specified OutputStream.
     * Each row in the DataSet is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows in the DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the specified columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The collection of column names to be included in the XML string.
     * @param output The OutputStream where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     * @see #toXml(int, int, Collection, String, OutputStream)
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into an XML string, including only the specified columns, and writes it to the specified OutputStream.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows and columns in the DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The names of the columns in the DataSet to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @param output The OutputStream where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty, or if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName, OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Writes the entire DataSet as an XML string to the specified Writer.
     * Each row in the DataSet is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export the entire data in the DataSet to an XML format.
     * The resulting XML string represents all the rows in the DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param output The Writer where the XML string will be written.
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     * @see #toXml(int, int, Collection, String, Writer)
     */
    void toXml(Writer output) throws UncheckedIOException;

    /**
     * Writes the entire DataSet as an XML string to the specified Writer.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export the entire data in the DataSet to an XML format.
     * The resulting XML string represents all the rows in the DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @param output The Writer where the XML string will be written.
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     * @see #toXml(int, int, Collection, String, Writer)
     */
    void toXml(String rowElementName, Writer output) throws IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into an XML string and writes it to the specified Writer.
     * Each row in the DataSet is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows in the DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param output The Writer where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     * @see #toXml(int, int, Collection, String, Writer)
     */
    void toXml(int fromRowIndex, int toRowIndex, Writer output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into an XML string and writes it to the specified Writer.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows in the DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @param output The Writer where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     * @see #toXml(int, int, Collection, String, Writer)
     */
    void toXml(int fromRowIndex, int toRowIndex, String rowElementName, Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into an XML string, including only the specified columns, and writes it to the specified Writer.
     * Each row in the DataSet is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows in the DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the specified columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The collection of column names to be included in the XML string.
     * @param output The Writer where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     * @see #toXml(int, int, Collection, String, Writer)
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into an XML string, including only the specified columns, and writes it to the specified Writer.
     * Each row in the DataSet is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to an XML format.
     * The resulting XML string represents the specified range of rows and columns in the DataSet.
     * The order of the rows in the XML string is the same as the order of the rows in the DataSet.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The names of the columns in the DataSet to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the DataSet.
     * @param output The Writer where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty, or if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName, Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire DataSet into a CSV string.
     * <br />
     * This method is typically used when you need to export the entire data in the DataSet to a CSV format.
     * The resulting CSV string represents the entire DataSet.
     * The order of the rows in the CSV string is the same as the order of the rows in the DataSet.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @return A CSV string representing the entire DataSet.
     * @see #toCsv(int, int, Collection)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    String toCsv();

    /**
     * Converts a range of rows in the DataSet into a CSV string, including only the specified columns.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to a CSV format.
     * The resulting CSV string represents the specified range of rows and columns in the DataSet.
     * The order of the rows in the CSV string is the same as the order of the rows in the DataSet.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the CSV string.
     * @param toRowIndex The ending index of the row range to be included in the CSV string.
     * @param columnNames The names of the columns in the DataSet to be included in the CSV string.
     * @return A CSV string representing the specified range of rows and columns in the DataSet.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     * @see #toCsv(int, int, Collection)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    String toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire DataSet into a CSV string and writes it to a File.
     * <br />
     * This method is typically used when you need to export the entire data in the DataSet to a CSV format.
     * The resulting CSV string represents the entire DataSet.
     * The order of the rows in the CSV string is the same as the order of the rows in the DataSet.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param output The File where the CSV string will be written.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #toCsv(int, int, Collection, File)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    void toCsv(File output) throws UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into a CSV string, including only the specified columns, and writes it to a File.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to a CSV format.
     * The resulting CSV string represents the specified range of rows and columns in the DataSet.
     * The order of the rows in the CSV string is the same as the order of the rows in the DataSet.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the CSV string.
     * @param toRowIndex The ending index of the row range to be included in the CSV string.
     * @param columnNames The names of the columns in the DataSet to be included in the CSV string.
     * @param output The File where the CSV string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #toCsv(int, int, Collection, File)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire DataSet into a CSV string and writes it to an OutputStream.
     * <br />
     * This method is typically used when you need to export the entire data in the DataSet to a CSV format.
     * The resulting CSV string represents the entire DataSet.
     * The order of the rows in the CSV string is the same as the order of the rows in the DataSet.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param output The OutputStream where the CSV string will be written.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #toCsv(int, int, Collection, OutputStream)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    void toCsv(OutputStream output) throws UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into a CSV string, including only the specified columns, and writes it to an OutputStream.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to a CSV format.
     * The resulting CSV string represents the specified range of rows and columns in the DataSet.
     * The order of the rows in the CSV string is the same as the order of the rows in the DataSet.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the CSV string.
     * @param toRowIndex The ending index of the row range to be included in the CSV string.
     * @param columnNames The names of the columns in the DataSet to be included in the CSV string.
     * @param output The OutputStream where the CSV string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #toCsv(int, int, Collection, OutputStream)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire DataSet into a CSV string and writes it to a Writer.
     * <br />
     * This method is typically used when you need to export the entire data in the DataSet to a CSV format.
     * The resulting CSV string represents the entire DataSet.
     * The order of the rows in the CSV string is the same as the order of the rows in the DataSet.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param output The Writer where the CSV string will be written.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #toCsv(int, int, Collection, Writer)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    void toCsv(Writer output) throws UncheckedIOException;

    /**
     * Converts a range of rows in the DataSet into a CSV string, including only the specified columns, and writes it to a Writer.
     * <br />
     * This method is typically used when you need to export a subset of the data in the DataSet to a CSV format.
     * The resulting CSV string represents the specified range of rows and columns in the DataSet.
     * The order of the rows in the CSV string is the same as the order of the rows in the DataSet.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the DataSet.
     *
     * @param fromRowIndex The starting index of the row range to be included in the CSV string.
     * @param toRowIndex The ending index of the row range to be included in the CSV string.
     * @param columnNames The names of the columns in the DataSet to be included in the CSV string.
     * @param output The Writer where the CSV string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #toCsv(int, int, Collection, Writer)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Groups the rows in the DataSet by a specified key column and applies an aggregate operation on another column.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on a column's values, grouped by another column's values.
     * The resulting DataSet will have unique values of the key column, and the result of the aggregate operation on the specified column.
     *
     * @param keyColumnName The name of the column to group by.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet.
     */
    DataSet groupBy(String keyColumnName, String aggregateOnColumnName, String aggregateResultColumnName, Collector<?, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by a specified key column and applies an aggregate operation on multiple columns.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting DataSet will have unique values of the key column, and the result of the aggregate operation on the specified columns.
     *
     * @param keyColumnName The name of the column to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class type of the row in the resulting DataSet. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A new DataSet with the grouped and aggregated data - list of type {@code rowType}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    DataSet groupBy(String keyColumnName, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName, Class<?> rowType)
            throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by a specified key column and applies an aggregate operation on multiple columns.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting DataSet will have unique values of the key column, and the result of the aggregate operation on the specified columns.
     *
     * @param keyColumnName The name of the column to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code aggregateOnColumnNames} is empty.
     */
    DataSet groupBy(String keyColumnName, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by a specified key column and applies an aggregate operation on multiple columns.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting DataSet will have unique values of the key column, and the result of the aggregate operation on the specified columns.
     *
     * @param <T> The type of the elements being grouped.
     * @param keyColumnName The name of the column to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowMapper A function that transforms the aggregated rows into a specific type {@code T}.
     * @param collector The collector that defines the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code aggregateOnColumnNames} is empty.
     */
    <T> DataSet groupBy(String keyColumnName, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by a specified key column and applies an aggregate operation on a specific column.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on a column's values, grouped by another column's values.
     * The resulting DataSet will have unique values of the key column, and the result of the aggregate operation on the specified column.
     *
     * @param keyColumnName The name of the column to group by.
     * @param keyExtractor A function that transforms the key column values.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet.
     */
    DataSet groupBy(String keyColumnName, Function<?, ?> keyExtractor, String aggregateOnColumnName, String aggregateResultColumnName,
            Collector<?, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by a specified key column and applies an aggregate operation on multiple columns.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting DataSet will have unique values of the key column, and the result of the aggregate operation on the specified columns.
     *
     * @param keyColumnName The name of the column to group by.
     * @param keyExtractor A function that transforms the key column values.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class type of the row in the resulting DataSet. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A new DataSet with the grouped and aggregated data - list of type {@code rowType}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    DataSet groupBy(String keyColumnName, Function<?, ?> keyExtractor, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Class<?> rowType) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by a specified key column and applies an aggregate operation on multiple columns.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting DataSet will have unique values of the key column, and the result of the aggregate operation on the specified columns.
     *
     * @param keyColumnName The name of the column to group by.
     * @param keyExtractor A function that transforms the key column values.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code aggregateOnColumnNames} is empty.
     */
    DataSet groupBy(String keyColumnName, Function<?, ?> keyExtractor, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by a specified key column and applies an aggregate operation on multiple columns.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting DataSet will have unique values of the key column, and the result of the aggregate operation on the specified columns.
     *
     * @param <T> The type of the elements being grouped.
     * @param keyColumnName The name of the column to group by.
     * @param keyExtractor A function to transform the key column values.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowMapper A function that transforms the aggregated rows into a specific type {@code T}.
     * @param collector The collector that defines the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code aggregateOnColumnNames} is empty.
     */
    <T> DataSet groupBy(String keyColumnName, Function<?, ?> keyExtractor, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by the specified key columns.
     * <br />
     * This method is typically used when you need to categorize data based on multiple column values.
     * The resulting DataSet will have unique combinations of the key column values.
     *
     * @param keyColumnNames The names of the columns to group by.
     * @return A new DataSet with the grouped data.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty.
     */
    DataSet groupBy(Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by the specified key columns and applies an aggregate operation on a specific column.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on a column's values, grouped by other column's values.
     * The resulting DataSet will have unique combinations of the key column values, and the result of the aggregate operation on the specified column.
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty.
     */
    DataSet groupBy(Collection<String> keyColumnNames, String aggregateOnColumnName, String aggregateResultColumnName, Collector<?, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by the specified key columns and applies an aggregate operation on multiple columns.
     * The result of the aggregation is stored in a new column with a specified type.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting DataSet will have unique combinations of the key column values, and the result of the aggregate operation on the specified columns.
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class type of the new column that will store the result of the aggregate operation. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A new DataSet with the grouped and aggregated data - list of type {@code rowType}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    DataSet groupBy(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName, Class<?> rowType)
            throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by the specified key columns and applies an aggregate operation on multiple columns.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting DataSet will have unique combinations of the key column values, and the result of the aggregate operation on the specified columns.
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     */
    DataSet groupBy(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by the specified key columns and applies an aggregate operation on multiple columns.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting DataSet will have unique combinations of the key column values, and the result of the aggregate operation on the specified columns.
     *
     * @param <T> The type of the new format after applying the rowMapper function.
     * @param keyColumnNames The names of the columns to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowMapper The function to transform the rows into a new format.
     * @param collector The collector that defines the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     */
    <T> DataSet groupBy(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by the specified key columns.
     * The keys for grouping are generated by the provided keyExtractor function.
     * <br />
     * This method is typically used when you need to group data by a complex key composed of multiple columns or computed values.
     * The resulting DataSet will have unique combinations of the key values.
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param keyExtractor The function to generate the key for grouping. It takes an array of objects (the row) and returns a key object.
     * @return A new DataSet with the grouped data.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty.
     */
    DataSet groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by the specified key columns and applies an aggregate operation on a specific column.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on a column's values, grouped by other columns' values.
     * The resulting DataSet will have unique combinations of the key values, and the result of the aggregate operation on the specified column.
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param keyExtractor The function to generate the key for grouping. It takes an array of objects (the row) and returns a key object.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty.
     */
    DataSet groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, String aggregateOnColumnName,
            String aggregateResultColumnName, Collector<?, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by the specified key columns and applies an aggregate operation on specific columns.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting DataSet will have unique combinations of the key values, and the result of the aggregate operation on the specified columns.
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param keyExtractor The function to generate the key for grouping. It takes an array of objects (the row) and returns a key object.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class of the row type. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A new DataSet with the grouped and aggregated data - list of type {@code rowType}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    DataSet groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Class<?> rowType) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by the specified key columns and applies an aggregate operation on specific columns.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting DataSet will have unique combinations of the key values, and the result of the aggregate operation on the specified columns.
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param keyExtractor The function to generate the key for grouping. It takes an array of objects (the row) and returns a key object.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     */
    DataSet groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the DataSet by the specified key columns and applies an aggregate operation on specific columns.
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting DataSet will have unique combinations of the key values, and the result of the aggregate operation on the specified columns.
     *
     * @param <T> The type of the object that the row data will be mapped to.
     * @param keyColumnNames The names of the columns to group by.
     * @param keyExtractor The function to generate the key for grouping. It takes an array of objects (the row) and returns a key object.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowMapper The function to map the row data to the desired type. It takes an array of objects (the row) and returns an object of type T.
     * @param collector The collector that defines the aggregate operation.
     * @return A new DataSet with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    <T> DataSet groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the DataSet.
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the rollup operation.
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed.
     * @return A Stream of DataSets, each representing a level of the rollup operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the DataSet and applies an aggregate operation on a specific column.
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the rollup operation.
     * The result of the aggregation is stored in a new column in each of these DataSets.
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the rollup operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> keyColumnNames, String aggregateOnColumnName, String aggregateResultColumnName, Collector<?, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the DataSet and applies an aggregate operation on multiple columns.
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the rollup operation.
     * The results of the aggregation are stored in a new column in each of these DataSets.
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class of the row type. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of DataSets, each representing a level of the rollup operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName, Class<?> rowType)
            throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the DataSet and applies an aggregate operation on multiple columns.
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the rollup operation.
     * The results of the aggregation are stored in a new column in each of these DataSets.
     * The aggregation operation is defined by the provided collector.
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the rollup operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the DataSet and applies an aggregate operation on multiple columns.
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the rollup operation.
     * The results of the aggregation are stored in a new column in each of these DataSets.
     * The aggregation operation is defined by the provided collector.
     * The rowMapper function is used to transform the DisposableObjArray to a custom type T.
     *
     * @param <T> The type of the object that the row data will be mapped to.
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowMapper The function to transform the DisposableObjArray to a custom type T.
     * @param collector The collector that defines the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the rollup operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    <T> Stream<DataSet> rollup(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the DataSet using the specified columns and key mapper function.
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the rollup operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a custom key.
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a custom key.
     * @return A Stream of DataSets, each representing a level of the rollup operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the DataSet using the specified columns, key mapper function, and an aggregate operation.
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the rollup operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a custom key.
     * The results of the aggregation are stored in a new column in each of these DataSets.
     * The aggregation operation is defined by the provided collector.
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a custom key.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the rollup operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, String aggregateOnColumnName,
            String aggregateResultColumnName, Collector<?, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the DataSet using the specified columns, key mapper function, and a collection of aggregate operations.
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the rollup operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a custom key.
     * The results of the aggregation are stored in a new column in each of these DataSets.
     * The aggregation operation is defined by the provided rowType.
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a custom key.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class of the row type that defines the aggregate operation. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of DataSets, each representing a level of the rollup operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Class<?> rowType) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the DataSet using the specified columns, key mapper function, and a collector for aggregate operations.
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the rollup operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a custom key.
     * The results of the aggregation are stored in a new column in each of these DataSets.
     * The aggregation operation is defined by the provided collector.
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a custom key.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the rollup operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the DataSet using the specified columns, key mapper function, row mapper function, and a collector for aggregate operations.
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the rollup operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a custom key.
     * The rowMapper function is used to transform the DisposableObjArray to a custom row.
     * The results of the aggregation are stored in a new column in each of these DataSets.
     * The aggregation operation is defined by the provided collector.
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a custom key.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowMapper The function to transform the DisposableObjArray to a custom row.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the rollup operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    <T> Stream<DataSet> rollup(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor,
            Collection<String> aggregateOnColumnNames, String aggregateResultColumnName, Function<? super DisposableObjArray, ? extends T> rowMapper,
            Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the DataSet using the specified columns.
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the cube operation.
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @return A Stream of DataSets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty.
     */
    @Beta
    Stream<DataSet> cube(Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the DataSet using the specified columns and an aggregate operation.
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the cube operation.
     * The results of the aggregation are stored in a new column in each of these DataSets.
     * The aggregation operation is defined by the provided collector.
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> cube(Collection<String> keyColumnNames, String aggregateOnColumnName, String aggregateResultColumnName, Collector<?, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Performs a cube operation on the DataSet using the specified columns and an aggregate operation.
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the cube operation.
     * The results of the aggregation are stored in a new column in each of these DataSets.
     * The type of the new column is defined by the provided Class.
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The Class defining the type of the new column. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of DataSets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> cube(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName, Class<?> rowType)
            throws IllegalArgumentException;

    /**
     * Performs a cube operation on the DataSet using the specified columns and an aggregate operation.
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the cube operation.
     * The results of the aggregation are stored in a new column in each of these DataSets.
     * The aggregation operation is defined by the provided collector.
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> cube(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the DataSet using the specified columns and an aggregate operation.
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the cube operation.
     * The results of the aggregation are stored in a new column in each of these DataSets.
     * The aggregation operation is defined by the provided collector.
     * The rowMapper function is used to transform the DisposableObjArray to a type T before the aggregation operation.
     *
     * @param <T> The type of the object that the row data will be mapped to.
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowMapper The function to transform the DisposableObjArray to a type T before the aggregation operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    <T> Stream<DataSet> cube(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the DataSet using the specified columns and a key mapper function.
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the cube operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a key before the cube operation.
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a key before the cube operation.
     * @return A Stream of DataSets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> cube(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the DataSet using the specified columns, a key mapper function, and an aggregate operation.
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the cube operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a key before the cube operation.
     * The results of the aggregation are stored in a new column in the DataSet.
     * The aggregation operation is defined by the provided collector.
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a key before the cube operation.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> cube(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, String aggregateOnColumnName,
            String aggregateResultColumnName, Collector<?, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the DataSet using the specified columns, a key mapper function, and a row type.
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the cube operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a key before the cube operation.
     * The results of the aggregation are stored in a new column in the DataSet.
     * The row type defines the type of the rows in the resulting DataSet.
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a key before the cube operation.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class of the rows in the resulting DataSet. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of DataSets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> cube(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Class<?> rowType) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the DataSet using the specified columns, a key mapper function, and a collector.
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the cube operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a key before the cube operation.
     * The results of the aggregation are stored in a new column in the DataSet.
     * The collector defines the aggregate operation.
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a key before the cube operation.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<DataSet> cube(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the DataSet using the specified columns, a key mapper function, a row mapper function, and a collector.
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of DataSets, where each DataSet represents a level of the cube operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a key before the cube operation.
     * The rowMapper function is used to transform the DisposableObjArray to a row after the cube operation.
     * The results of the aggregation are stored in a new column in the DataSet.
     * The collector defines the aggregate operation.
     *
     * @param <T> The type of the object that the row data will be mapped to.
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a key before the cube operation.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowMapper The function to transform the DisposableObjArray to a row after the cube operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of DataSets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    <T> Stream<DataSet> cube(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Performs a pivot operation on the DataSet using the specified key column, aggregate column, pivot column, and a collector.
     * A pivot operation is a form of data summarization that rotates the data from a state of rows to a state of columns,
     * providing a multidimensional analysis.
     * <br />
     * This method returns a Sheet, where each cell represents an aggregation result.
     * The keyColumnName is used as the row identifier in the resulting Sheet.
     * The aggregateOnColumnNames is the column on whichDifference between groupBy and pivot_table for pandas dataframes the aggregate operation is to be performed.
     * The pivotColumnName is used as the column identifier in the resulting Sheet.
     * The collector defines the aggregate operation.
     *
     * @param <R> The type of the row identifier in the resulting Sheet.
     * @param <C> The type of the column identifier in the resulting Sheet.
     * @param <T> The type of the aggregation result in the resulting Sheet.
     * @param keyColumnName The name of the column to be used as the row identifier in the resulting Sheet.
     * @param aggregateOnColumnNames The name of the column on which the aggregate operation is to be performed.
     * @param pivotColumnName The name of the column to be used as the column identifier in the resulting Sheet.
     * @param collector The collector defining the aggregate operation.
     * @return A Sheet representing the result of the pivot operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/34702815">Difference between groupby and pivot_table for pandas dataframes</a>
     */
    @Beta
    <R, C, T> Sheet<R, C, T> pivot(String keyColumnName, String aggregateOnColumnNames, String pivotColumnName, Collector<?, ?, ? extends T> collector)
            throws IllegalArgumentException;

    /**
     * Performs a pivot operation on the DataSet using the specified key column, aggregate columns, pivot column, and a collector.
     * A pivot operation is a form of data summarization that rotates the data from a state of rows to a state of columns,
     * providing a multidimensional analysis.
     * <br />
     * This method returns a Sheet, where each cell represents an aggregation result.
     * The keyColumnName is used as the row identifier in the resulting Sheet.
     * The aggregateOnColumnNames are the columns on which the aggregate operation is to be performed.
     * The pivotColumnName is used as the column identifier in the resulting Sheet.
     * The collector defines the aggregate operation.
     *
     * @param <R> The type of the row identifier in the resulting Sheet.
     * @param <C> The type of the column identifier in the resulting Sheet.
     * @param <T> The type of the aggregation result in the resulting Sheet.
     * @param keyColumnName The name of the column to be used as the row identifier in the resulting Sheet.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param pivotColumnName The name of the column to be used as the column identifier in the resulting Sheet.
     * @param collector The collector defining the aggregate operation.
     * @return A Sheet representing the result of the pivot operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/34702815">Difference between groupby and pivot_table for pandas dataframes</a>
     */
    @Beta
    <R, C, T> Sheet<R, C, T> pivot(String keyColumnName, Collection<String> aggregateOnColumnNames, String pivotColumnName,
            Collector<? super Object[], ?, ? extends T> collector) throws IllegalArgumentException;

    /**
     * Performs a pivot operation on the DataSet using the specified key column, aggregate columns, pivot column, a row mapper, and a collector.
     * A pivot operation is a form of data summarization that rotates the data from a state of rows to a state of columns,
     * providing a multidimensional analysis.
     * <br />
     * This method returns a Sheet, where each cell represents an aggregation result.
     * The keyColumnName is used as the row identifier in the resulting Sheet.
     * The aggregateOnColumnNames are the columns on which the aggregate operation is to be performed.
     * The pivotColumnName is used as the column identifier in the resulting Sheet.
     * The rowMapper is a function that transforms the row data before aggregation.
     * The collector defines the aggregate operation.
     *
     * @param <R> The type of the row identifier in the resulting Sheet.
     * @param <C> The type of the column identifier in the resulting Sheet.
     * @param <U> The type of the row data after being transformed by the rowMapper.
     * @param <T> The type of the aggregation result in the resulting Sheet.
     * @param keyColumnName The name of the column to be used as the row identifier in the resulting Sheet.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param pivotColumnName The name of the column to be used as the column identifier in the resulting Sheet.
     * @param rowMapper The function to transform the row data before aggregation.
     * @param collector The collector defining the aggregate operation.
     * @return A Sheet representing the result of the pivot operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code aggregateOnColumnNames} is empty.
     * @see <a href="https://stackoverflow.com/questions/34702815">Difference between groupby and pivot_table for pandas dataframes</a>
     */
    @Beta
    <R, C, U, T> Sheet<R, C, T> pivot(String keyColumnName, Collection<String> aggregateOnColumnNames, String pivotColumnName,
            Function<? super DisposableObjArray, ? extends U> rowMapper, Collector<? super U, ?, ? extends T> collector) throws IllegalArgumentException;

    /**
     * Sorts the DataSet based on the specified column name.
     * The sorting is done in ascending order.
     *
     * @param columnName The name of the column to be used for sorting.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     */
    void sortBy(String columnName) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the DataSet based on the specified column name using the provided Comparator.
     * The Comparator determines the order of the elements.
     *
     * @param columnName The name of the column to be used for sorting.
     * @param cmp The Comparator to determine the order of the elements.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     */
    void sortBy(String columnName, Comparator<?> cmp) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the DataSet based on the specified collection of column names.
     * The sorting is done in ascending order for each column in the order they appear in the collection.
     *
     * @param columnNames The collection of column names to be used for sorting.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     */
    void sortBy(Collection<String> columnNames) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the DataSet based on the specified collection of column names using the provided Comparator.
     * The Comparator determines the order of the elements for each row, which is an Object array.
     *
     * @param columnNames The collection of column names to be used for sorting.
     * @param cmp The Comparator to determine the order of the elements. It compares Object arrays, each representing a row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     */
    void sortBy(Collection<String> columnNames, Comparator<? super Object[]> cmp) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the DataSet based on the specified column names and a key mapper function.
     * The key mapper function is applied to each row of the DataSet, and the resulting Comparable objects are used for sorting.
     * The column names determine the order of the elements in the DisposableObjArray passed to the key mapper function.
     *
     * @param columnNames The names of the columns to be used for sorting. The order of the column names determines the order of the elements in the DisposableObjArray passed to the key mapper function.
     * @param keyExtractor A function that takes a DisposableObjArray representing a row of the DataSet and returns a Comparable object that is used for sorting.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     */
    @SuppressWarnings("rawtypes")
    void sortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyExtractor)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the DataSet with multi-threads based on the specified column name.
     * The sorting is done in ascending order.
     *
     * @param columnName The name of the column to be used for sorting.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     */
    void parallelSortBy(String columnName) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the DataSet with multi-threads based on the specified column name using the provided Comparator.
     * The Comparator determines the order of the elements for each row, which is an Object array.
     *
     * @param columnName The name of the column to be used for sorting.
     * @param cmp The Comparator to determine the order of the elements. It compares Object arrays, each representing a row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     */
    void parallelSortBy(String columnName, Comparator<?> cmp) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the DataSet with multi-threads based on the specified collection of column names.
     * The sorting is done in ascending order for each column.
     *
     * @param columnNames The collection of column names to be used for sorting.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     */
    void parallelSortBy(Collection<String> columnNames) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the DataSet with multi-threads based on the specified collection of column names using the provided Comparator.
     * The Comparator determines the order of the elements for each row, which is an Object array.
     *
     * @param columnNames The collection of column names to be used for sorting.
     * @param cmp The Comparator to determine the order of the elements. It compares Object arrays, each representing a row.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     */
    void parallelSortBy(Collection<String> columnNames, Comparator<? super Object[]> cmp) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the DataSet with multi-threads based on the specified column names and a key mapper function.
     * The key mapper function is applied to each row of the DataSet, and the resulting Comparable objects are used for sorting.
     * The column names determine the order of the elements in the DisposableObjArray passed to the key mapper function.
     * This method is designed for large datasets where parallel sorting can provide a performance improvement.
     *
     * @param columnNames The names of the columns to be used for sorting. The order of the column names determines the order of the elements in the DisposableObjArray passed to the key mapper function.
     * @param keyExtractor A function that takes a DisposableObjArray representing a row of the DataSet and returns a Comparable object that is used for sorting.
     * @throws IllegalStateException if the DataSet is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     */
    @SuppressWarnings("rawtypes")
    void parallelSortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyExtractor)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Returns the top <i>n</i> rows from the DataSet based on the values in the specified column.
     * The rows are sorted in ascending order based on the values in the specified column.
     * If two rows have the same value in the specified column, their order is determined by their original order in the DataSet.
     *
     * @param columnName The name of the column to be used for sorting.
     * @param n The number of top rows to return.
     * @return A new DataSet containing the top <i>n</i> rows.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet or <i>n</i> is less than 1.
     */
    DataSet topBy(String columnName, int n) throws IllegalArgumentException;

    /**
     * Returns the top <i>n</i> rows from the DataSet based on the values in the specified column.
     * The rows are sorted based on the provided Comparator.
     * If two rows have the same value in the specified column, their order is determined by the Comparator.
     *
     * @param columnName The name of the column to be used for sorting.
     * @param n The number of top rows to return.
     * @param cmp The Comparator to determine the order of the elements. It compares Object arrays, each representing a row.
     * @return A new DataSet containing the top <i>n</i> rows.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet or <i>n</i> is less than 1.
     */
    DataSet topBy(String columnName, int n, Comparator<?> cmp) throws IllegalArgumentException;

    /**
     * Returns the top <i>n</i> rows from the DataSet based on the values in the specified columns.
     * The rows are sorted in the order they appear in the DataSet.
     * If two rows have the same values in the specified columns, their order is determined by their original order in the DataSet.
     *
     * @param columnNames The names of the columns to be used for sorting.
     * @param n The number of top rows to return.
     * @return A new DataSet containing the top <i>n</i> rows.
     * @throws IllegalArgumentException if any of the specified column names do not exist in the DataSet or {@code columnNames} is empty or <i>n</i> is less than 1.
     */
    DataSet topBy(Collection<String> columnNames, int n) throws IllegalArgumentException;

    /**
     * Returns the top <i>n</i> rows from the DataSet based on the values in the specified columns.
     * The rows are sorted based on the provided Comparator.
     * If two rows have the same values in the specified columns, their order is determined by the Comparator.
     *
     * @param columnNames The names of the columns to be used for sorting.
     * @param n The number of top rows to return.
     * @param cmp The Comparator to determine the order of the elements. It compares Object arrays, each representing a row.
     * @return A new DataSet containing the top <i>n</i> rows.
     * @throws IllegalArgumentException if any of the specified column names do not exist in the DataSet or {@code columnNames} is empty or <i>n</i> is less than 1.
     */
    DataSet topBy(Collection<String> columnNames, int n, Comparator<? super Object[]> cmp) throws IllegalArgumentException;

    /**
     * Returns the top <i>n</i> rows from the DataSet based on the values in the specified columns.
     * The rows are sorted based on the provided keyExtractor function.
     * If two rows have the same value in the specified columns, their order is determined by the keyExtractor function.
     *
     * @param columnNames The names of the columns to be used for sorting.
     * @param n The number of top rows to return.
     * @param keyExtractor The function to determine the order of the elements. It takes an array of Objects, each representing a row, and returns a Comparable.
     * @return A new DataSet containing the top <i>n</i> rows.
     * @throws IllegalArgumentException if any of the specified column names do not exist in the DataSet or {@code columnNames} is empty or <i>n</i> is less than 1.
     */
    @SuppressWarnings("rawtypes")
    DataSet topBy(Collection<String> columnNames, int n, Function<? super DisposableObjArray, ? extends Comparable> keyExtractor);

    /**
     * Returns a new DataSet containing only the distinct rows from the original DataSet.
     * The distinctness of rows is determined by the equals method of the row objects.
     *
     * @return A new DataSet containing only distinct rows.
     */
    DataSet distinct();

    /**
     * Returns a new DataSet containing only the distinct rows based on the specified column from the original DataSet.
     * The distinctness of rows is determined by the equals method of the column values.
     *
     * @param columnName The name of the column to be used for determining distinctness.
     * @return A new DataSet containing only distinct rows based on the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     */
    DataSet distinctBy(String columnName);

    /**
     * Returns a new DataSet containing only the distinct rows based on the specified column from the original DataSet.
     * The distinctness of rows is determined by the equals method of the values returned by the provided keyExtractor function.
     *
     * @param columnName The name of the column to be used for determining distinctness.
     * @param keyExtractor A function to process the column values before determining distinctness.
     * @return A new DataSet containing only distinct rows based on the specified column and keyExtractor function.
     * @throws IllegalArgumentException if the specified column name does not exist in the DataSet.
     */
    DataSet distinctBy(String columnName, Function<?, ?> keyExtractor);

    /**
     * Returns a new DataSet containing only the distinct rows based on the specified columns from the original DataSet.
     * The distinctness of rows is determined by the equals method of the values in the specified columns.
     *
     * @param columnNames The names of the columns to be used for determining distinctness.
     * @return A new DataSet containing only distinct rows based on the specified columns.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     */
    DataSet distinctBy(Collection<String> columnNames);

    /**
     * Returns a new DataSet containing only the distinct rows based on the specified columns from the original DataSet.
     * The distinctness of rows is determined by the equals method of the values returned by the provided keyExtractor function.
     *
     * @param columnNames The names of the columns to be used for determining distinctness.
     * @param keyExtractor A function to process the column values before determining distinctness.
     * @return A new DataSet containing only distinct rows based on the specified columns and keyExtractor function.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the DataSet or {@code columnNames} is empty.
     */
    DataSet distinctBy(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyExtractor);

    /**
     * Filters the rows of the DataSet based on the provided predicate.
     * The predicate is applied to each row, and only rows that satisfy the predicate (i.e., predicate returns true) are included in the returned DataSet.
     *
     * @param filter The predicate to apply to each row. It takes an instance of DisposableObjArray, which represents a row in the DataSet.
     * @return A new DataSet containing only the rows that satisfy the provided predicate.
     */
    DataSet filter(Predicate<? super DisposableObjArray> filter);

    /**
     * Filters the rows of the DataSet based on the provided predicate and limits the number of results.
     * The predicate is applied to each row, and only rows that satisfy the predicate (i.e., predicate returns true) are included in the returned DataSet.
     * The operation stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * @param filter The predicate to apply to each row. It takes an instance of DisposableObjArray, which represents a row in the DataSet.
     * @param max The maximum number of rows to include in the returned DataSet.
     * @return A new DataSet containing only the rows that satisfy the provided predicate, up to the specified maximum limit.
     * @throws IllegalArgumentException if the specified max is less than 0.
     */
    DataSet filter(Predicate<? super DisposableObjArray> filter, int max) throws IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on the provided predicate and within the specified row index range.
     * The predicate is applied to each row within the range, and only rows that satisfy the predicate (i.e., predicate returns true) are included in the returned DataSet.
     *
     * @param fromRowIndex The starting index of the row range to apply the filter. It's inclusive.
     * @param toRowIndex The ending index of the row range to apply the filter. It's exclusive.
     * @param filter The predicate to apply to each row within the specified range. It takes an instance of DisposableObjArray, which represents a row in the DataSet.
     * @return A new DataSet containing only the rows within the specified range that satisfy the provided predicate.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet.
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Predicate<? super DisposableObjArray> filter) throws IndexOutOfBoundsException;

    /**
     * Filters the rows of the DataSet based on the provided predicate, within the specified row index range, and limits the number of results.
     * The predicate is applied to each row within the range, and only rows that satisfy the predicate (i.e., predicate returns true) are included in the returned DataSet.
     * The operation stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * @param fromRowIndex The starting index of the row range to apply the filter. It's inclusive.
     * @param toRowIndex The ending index of the row range to apply the filter. It's exclusive.
     * @param filter The predicate to apply to each row within the specified range. It takes an instance of DisposableObjArray, which represents a row in the DataSet.
     * @param max The maximum number of rows to include in the returned DataSet.
     * @return A new DataSet containing only the rows within the specified range that satisfy the provided predicate, up to the specified maximum limit.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet.
     * @throws IllegalArgumentException if the specified max is less than 0.
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Predicate<? super DisposableObjArray> filter, int max)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on the provided BiPredicate and the specified column names.
     * The BiPredicate is applied to each pair of values from the specified columns, and only rows where the BiPredicate returns {@code true} are included in the returned DataSet.
     *
     * @param columnNames A Tuple2 containing the names of the two columns to be used in the BiPredicate.
     * @param filter The BiPredicate to apply to each pair of values from the specified columns. It takes two instances of Objects, which represent the values in the DataSet's row for the specified columns.
     * @return A new DataSet containing only the rows where the provided BiPredicate returns {@code true} for the pair of values from the specified columns.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet.
     */
    DataSet filter(Tuple2<String, String> columnNames, BiPredicate<?, ?> filter) throws IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on the provided BiPredicate and the specified column names, and limits the number of results.
     * The BiPredicate is applied to each pair of values from the specified columns, and only rows where the BiPredicate returns {@code true} are included in the returned DataSet.
     * The operation stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * @param columnNames A Tuple2 containing the names of the two columns to be used in the BiPredicate.
     * @param filter The BiPredicate to apply to each pair of values from the specified columns. It takes two instances of Objects, which represent the values in the DataSet's row for the specified columns.
     * @param max The maximum number of rows to include in the returned DataSet.
     * @return A new DataSet containing only the rows where the provided BiPredicate returns {@code true} for the pair of values from the specified columns, up to the specified maximum limit.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or if the specified max is less than 0.
     */
    DataSet filter(Tuple2<String, String> columnNames, BiPredicate<?, ?> filter, int max) throws IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on the provided BiPredicate and the specified column names, within the given row index range.
     * The BiPredicate is applied to each pair of values from the specified columns, and only rows where the BiPredicate returns {@code true} are included in the returned DataSet.
     * The operation is performed only on the rows within the specified index range.
     *
     * @param fromRowIndex The start index of the row range to filter.
     * @param toRowIndex The end index of the row range to filter.
     * @param columnNames A Tuple2 containing the names of the two columns to be used in the BiPredicate.
     * @param filter The BiPredicate to apply to each pair of values from the specified columns. It takes two instances of Objects, which represent the values in the DataSet's row for the specified columns.
     * @return A new DataSet containing only the rows where the provided BiPredicate returns {@code true} for the pair of values from the specified columns, within the specified row index range.
     * @throws IndexOutOfBoundsException if the specified row index range is out of the DataSet's bounds.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet.
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, BiPredicate<?, ?> filter)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on the provided BiPredicate and the specified column names, within the given row index range and limits the number of results.
     * The BiPredicate is applied to each pair of values from the specified columns, and only rows where the BiPredicate returns {@code true} are included in the returned DataSet.
     * The operation is performed only on the rows within the specified index range and stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * @param fromRowIndex The start index of the row range to filter.
     * @param toRowIndex The end index of the row range to filter.
     * @param columnNames A Tuple2 containing the names of the two columns to be used in the BiPredicate.
     * @param filter The BiPredicate to apply to each pair of values from the specified columns. It takes two instances of Objects, which represent the values in the DataSet's row for the specified columns.
     * @param max The maximum number of rows to include in the returned DataSet.
     * @return A new DataSet containing only the rows where the provided BiPredicate returns {@code true} for the pair of values from the specified columns, within the specified row index range and up to the specified maximum limit.
     * @throws IndexOutOfBoundsException if the specified row index range is out of the DataSet's bounds.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or if the specified max is less than 0.
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, BiPredicate<?, ?> filter, int max)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on a provided TriPredicate and the specified column names.
     * The TriPredicate is applied to each triplet of values from the specified columns, and only rows where the TriPredicate returns {@code true} are included in the returned DataSet.
     *
     * @param columnNames A Tuple3 containing the names of the three columns to be used in the TriPredicate.
     * @param filter The TriPredicate to apply to each triplet of values from the specified columns. It takes three instances of Objects, which represent the values in the DataSet's row for the specified columns.
     * @return A new DataSet containing only the rows where the provided TriPredicate returns {@code true} for the triplet of values from the specified columns.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet.
     */
    DataSet filter(Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter) throws IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on a provided TriPredicate and the specified column names, within a limit.
     * The TriPredicate is applied to each triplet of values from the specified columns, and only rows where the TriPredicate returns {@code true} are included in the returned DataSet.
     * The operation stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * @param columnNames A Tuple3 containing the names of the three columns to be used in the TriPredicate.
     * @param filter The TriPredicate to apply to each triplet of values from the specified columns. It takes three instances of Objects, which represent the values in the DataSet's row for the specified columns.
     * @param max The maximum number of rows to include in the returned DataSet.
     * @return A new DataSet containing only the rows where the provided TriPredicate returns {@code true} for the triplet of values from the specified columns, up to the specified maximum limit.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or if the specified max is less than 0.
     */
    DataSet filter(Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter, int max) throws IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on a provided TriPredicate and the specified column names, within a specified row index range.
     * The TriPredicate is applied to each triplet of values from the specified columns, and only rows where the TriPredicate returns {@code true} are included in the returned DataSet.
     * The operation is performed only on the rows within the specified index range.
     *
     * @param fromRowIndex The start index of the row range to apply the filter on.
     * @param toRowIndex The end index of the row range to apply the filter on.
     * @param columnNames A Tuple3 containing the names of the three columns to be used in the TriPredicate.
     * @param filter The TriPredicate to apply to each triplet of values from the specified columns. It takes three instances of Objects, which represent the values in the DataSet's row for the specified columns.
     * @return A new DataSet containing only the rows where the provided TriPredicate returns {@code true} for the triplet of values from the specified columns, within the specified row index range.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet.
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on a provided TriPredicate and the specified column names, within a specified row index range and a maximum limit.
     * The TriPredicate is applied to each triplet of values from the specified columns, and only rows where the TriPredicate returns {@code true} are included in the returned DataSet.
     * The operation is performed only on the rows within the specified index range and stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * @param fromRowIndex The start index of the row range to apply the filter on.
     * @param toRowIndex The end index of the row range to apply the filter on.
     * @param columnNames A Tuple3 containing the names of the three columns to be used in the TriPredicate.
     * @param filter The TriPredicate to apply to each triplet of values from the specified columns. It takes three instances of Objects, which represent the values in the DataSet's row for the specified columns.
     * @param max The maximum number of rows to include in the returned DataSet.
     * @return A new DataSet containing only the rows where the provided TriPredicate returns {@code true} for the triplet of values from the specified columns, within the specified row index range and up to the specified maximum limit.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or if the specified max is less than 0.
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter, int max)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on a provided Predicate and the specified column name.
     * The Predicate is applied to each value from the specified column, and only rows where the Predicate returns {@code true} are included in the returned DataSet.
     *
     * @param columnName The name of the column to be used in the Predicate.
     * @param filter The Predicate to apply to each value from the specified column. It takes an instance of Object, which represents the value in the DataSet's row for the specified column.
     * @return A new DataSet containing only the rows where the provided Predicate returns {@code true} for the value from the specified column.
     * @throws IllegalArgumentException if the specified column name is not found in the DataSet.
     */
    DataSet filter(String columnName, Predicate<?> filter) throws IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on a provided Predicate and the specified column name, with a maximum limit.
     * The Predicate is applied to each value from the specified column, and only rows where the Predicate returns {@code true} are included in the returned DataSet.
     * The operation stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * @param columnName The name of the column to be used in the Predicate.
     * @param filter The Predicate to apply to each value from the specified column. It takes an instance of Object, which represents the value in the DataSet's row for the specified column.
     * @param max The maximum number of rows to include in the returned DataSet.
     * @return A new DataSet containing only the rows where the provided Predicate returns {@code true} for the value from the specified column, up to the specified maximum limit.
     * @throws IllegalArgumentException if the specified column name is not found in the DataSet or if the specified max is less than 0.
     */
    DataSet filter(String columnName, Predicate<?> filter, int max) throws IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on a provided Predicate and the specified column name, within a given row index range.
     * The Predicate is applied to each value from the specified column within the given range, and only rows where the Predicate returns {@code true} are included in the returned DataSet.
     *
     * @param fromRowIndex The start index of the row range to apply the filter.
     * @param toRowIndex The end index of the row range to apply the filter.
     * @param columnName The name of the column to be used in the Predicate.
     * @param filter The Predicate to apply to each value from the specified column. It takes an instance of Object, which represents the value in the DataSet's row for the specified column.
     * @return A new DataSet containing only the rows within the specified range where the provided Predicate returns {@code true} for the value from the specified column.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet.
     * @throws IllegalArgumentException if the specified column name is not found in the DataSet.
     */
    DataSet filter(int fromRowIndex, int toRowIndex, String columnName, Predicate<?> filter) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on a provided predicate function.
     * The function is applied to the values of the specified column in each row.
     * Only the rows that satisfy the predicate (i.e., the function returns true) are included in the resulting DataSet.
     * The operation is performed on the rows within the specified range and until the number of rows in the resulting DataSet reaches the specified maximum limit.
     *
     * @param fromRowIndex The starting index of the row range to consider for filtering.
     * @param toRowIndex The ending index of the row range to consider for filtering.
     * @param columnName The name of the column whose values will be used as input for the predicate function.
     * @param filter The predicate function to apply to each row of the specified column. It takes an instance of the column's value type and returns a boolean.
     * @param max The maximum number of rows to include in the resulting DataSet.
     * @return A new DataSet containing only the rows that satisfy the predicate, up to the specified maximum limit.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the DataSet.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or if the specified max is less than 0.
     */
    DataSet filter(int fromRowIndex, int toRowIndex, String columnName, Predicate<?> filter, int max)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on a provided predicate function.
     * The function is applied to the values of the specified columns in each row.
     * Only the rows that satisfy the predicate (i.e., the function returns true) are included in the resulting DataSet.
     *
     * @param columnNames A collection of column names whose values will be used as input for the predicate function.
     * @param filter The predicate function to apply to each row of the specified columns. It takes an instance of DisposableObjArray (which represents the values of the specified columns in a row) and returns a boolean.
     * @return A new DataSet containing only the rows that satisfy the predicate.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or {@code columnNames} is empty.
     */
    DataSet filter(Collection<String> columnNames, Predicate<? super DisposableObjArray> filter) throws IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on a provided predicate function.
     * The function is applied to the values of the specified columns in each row.
     * Only the rows that satisfy the predicate (i.e., the function returns true) are included in the resulting DataSet.
     * The operation is performed until the number of rows in the resulting DataSet reaches the specified maximum limit.
     *
     * @param columnNames A collection of column names whose values will be used as input for the predicate function.
     * @param filter The predicate function to apply to each row of the specified columns. It takes an instance of DisposableObjArray (which represents the values of the specified columns in a row) and returns a boolean.
     * @param max The maximum number of rows to include in the resulting DataSet.
     * @return A new DataSet containing only the rows that satisfy the predicate, up to the specified maximum limit.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or {@code columnNames} is empty or if the specified max is less than 0.
     */
    DataSet filter(Collection<String> columnNames, Predicate<? super DisposableObjArray> filter, int max) throws IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on a provided predicate function.
     * The function is applied to the values of the specified columns in each row.
     * Only the rows that satisfy the predicate (i.e., the function returns true) are included in the resulting DataSet.
     * The operation is performed on a range of rows specified by the fromRowIndex and toRowIndex parameters.
     *
     * @param fromRowIndex The starting index of the row range to filter (inclusive).
     * @param toRowIndex The ending index of the row range to filter (exclusive).
     * @param columnNames A collection of column names whose values will be used as input for the predicate function.
     * @param filter The predicate function to apply to each row of the specified columns. It takes an instance of DisposableObjArray (which represents the values of the specified columns in a row) and returns a boolean.
     * @return A new DataSet containing only the rows that satisfy the predicate.
     * @throws IndexOutOfBoundsException if the specified row index range is out of the DataSet's bounds.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or {@code columnNames} is empty.
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Predicate<? super DisposableObjArray> filter)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the DataSet based on the provided Predicate and the specified column names, within the given row index range and limits the number of results.
     * The Predicate is applied to each DisposableObjArray (which represents a row in the DataSet) from the specified columns, and only rows where the Predicate returns {@code true} are included in the returned DataSet.
     * The operation is performed only on the rows within the specified index range and stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * @param fromRowIndex The start index of the row range to filter.
     * @param toRowIndex The end index of the row range to filter.
     * @param columnNames A Collection containing the names of the columns to be used in the Predicate.
     * @param filter The Predicate to apply to each DisposableObjArray from the specified columns. It takes an instance of DisposableObjArray, which represents the values in the DataSet's row for the specified columns.
     * @param max The maximum number of rows to include in the returned DataSet.
     * @return A new DataSet containing only the rows where the provided Predicate returns {@code true} for the DisposableObjArray from the specified columns, within the specified row index range and up to the specified maximum limit.
     * @throws IndexOutOfBoundsException if the specified row index range is out of the DataSet's bounds.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or {@code columnNames} is empty or if the specified max is less than 0.
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Predicate<? super DisposableObjArray> filter, int max)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Transforms the DataSet by applying a mapping function to the specified column and creating a new column with the results.
     * The original column used in the mapping function is preserved.
     * The operation also copies the specified column to the new DataSet.
     *
     * @param fromColumnName The name of the column to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnName The name of the column to be copied to the new DataSet.
     * @param mapper The mapping function to apply to each row of the specified column. It takes an instance of the column's value and returns a new value.
     * @return A new DataSet with the new column added and the specified column copied.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet.
     */
    DataSet map(String fromColumnName, String newColumnName, String copyingColumnName, Function<?, ?> mapper) throws IllegalArgumentException;

    /**
     * Transforms the DataSet by applying a mapping function to the specified column and creating a new column with the results.
     * The original column used in the mapping function is preserved.
     * The operation also copies the specified columns to the new DataSet.
     *
     * @param fromColumnName The name of the column to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new DataSet.
     * @param mapper The mapping function to apply to each row of the specified column. It takes an instance of the column's value and returns a new value.
     * @return A new DataSet with the new column added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet.
     */
    DataSet map(String fromColumnName, String newColumnName, Collection<String> copyingColumnNames, Function<?, ?> mapper) throws IllegalArgumentException;

    /**
     * Transforms the DataSet by applying a mapping function to the specified pair of columns and creating a new column with the results.
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new DataSet.
     *
     * @param fromColumnNames A Tuple2 containing the pair of column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new DataSet.
     * @param mapper The mapping function to apply to each row of the specified columns. It takes instances of the columns' values and returns a new value.
     * @return A new DataSet with the new column added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet.
     */
    DataSet map(Tuple2<String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames, BiFunction<?, ?, ?> mapper)
            throws IllegalArgumentException;

    /**
     * Transforms the DataSet by applying a mapping function to the specified columns and creating a new column with the results.
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new DataSet.
     *
     * @param fromColumnNames A Tuple3 containing the column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new DataSet.
     * @param mapper The mapping function to apply to each row of the specified columns. It takes instances of the columns' values and returns a new value.
     * @return A new DataSet with the new column added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet.
     */
    DataSet map(Tuple3<String, String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames, TriFunction<?, ?, ?, ?> mapper)
            throws IllegalArgumentException;

    /**
     * Transforms the DataSet by applying a mapping function to the specified columns and creating a new column with the results.
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new DataSet.
     *
     * @param fromColumnNames A collection of column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new DataSet.
     * @param mapper The mapping function to apply to each row of the specified columns. It takes an instance of DisposableObjArray, which represents the values in the DataSet's row for the specified columns.
     * @return A new DataSet with the new column added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or {@code fromColumnNames} is empty.
     */
    DataSet map(Collection<String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames, Function<? super DisposableObjArray, ?> mapper)
            throws IllegalArgumentException;

    /**
     * Transforms the DataSet by applying a mapping function to the specified column and creating new rows with the results.
     * The original column used in the mapping function is preserved.
     * The operation also copies the specified column to the new DataSet.
     *
     * @param fromColumnName The column name to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnName The column name to be copied to the new DataSet.
     * @param mapper The mapping function to apply to each row of the specified column. It takes an instance of the column's value and returns a Collection of new rows.
     * @return A new DataSet with the new rows added and the specified column copied.
     * @throws IllegalArgumentException if the specified column name is not found in the DataSet.
     */
    DataSet flatMap(String fromColumnName, String newColumnName, String copyingColumnName, Function<?, ? extends Collection<?>> mapper)
            throws IllegalArgumentException;

    /**
     * Transforms the DataSet by applying a mapping function to the specified column and creating new rows with the results.
     * The original column used in the mapping function is preserved.
     * The operation also copies the specified columns to the new DataSet.
     *
     * @param fromColumnName The column name to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new DataSet.
     * @param mapper The mapping function to apply to each row of the specified column. It takes an instance of the column's value and returns a Collection of new rows.
     * @return A new DataSet with the new rows added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column name is not found in the DataSet.
     */
    DataSet flatMap(String fromColumnName, String newColumnName, Collection<String> copyingColumnNames, Function<?, ? extends Collection<?>> mapper)
            throws IllegalArgumentException;

    /**
     * Transforms the DataSet by applying a mapping function to the specified pair of columns and creating new rows with the results.
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new DataSet.
     *
     * @param fromColumnNames A tuple of two column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new DataSet.
     * @return A new DataSet with the new rows added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet.
     */
    DataSet flatMap(Tuple2<String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames,
            BiFunction<?, ?, ? extends Collection<?>> mapper) throws IllegalArgumentException;

    /**
     * Transforms the DataSet by applying a mapping function to the specified columns and creating new rows with the results.
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new DataSet.
     *
     * @param fromColumnNames A tuple of three column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new DataSet.
     * @param mapper The mapping function to apply to each row of the specified columns. It takes an instance of DisposableObjArray, which represents the values in the DataSet's row for the specified columns, and returns a Collection of new rows.
     * @return A new DataSet with the new rows added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet.
     */
    DataSet flatMap(Tuple3<String, String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames,
            TriFunction<?, ?, ?, ? extends Collection<?>> mapper) throws IllegalArgumentException;

    /**
     * Transforms the DataSet by applying a mapping function to the specified columns and creating a new column with the results.
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new DataSet.
     *
     * @param fromColumnNames A collection of column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new DataSet.
     * @param mapper The mapping function to apply to each row of the specified columns. It takes an instance of DisposableObjArray, which represents the values in the DataSet's row for the specified columns.
     * @return A new DataSet with the new column added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or {@code fromColumnNames} is empty.
     */
    DataSet flatMap(Collection<String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames,
            Function<? super DisposableObjArray, ? extends Collection<?>> mapper) throws IllegalArgumentException;

    /**
     * Performs an inner join operation between this DataSet and another DataSet based on the specified column names.
     * The inner join operation combines rows from two DataSets based on a related column between them.
     * Only rows that have matching values in both DataSets will be included in the resulting DataSet.
     *
     * @param right The other DataSet to join with.
     * @param columnName The name of the column in this DataSet to use for the join.
     * @param joinColumnNameOnRight The name of the column in the other DataSet to use for the join.
     * @return A new DataSet that is the result of the inner join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet innerJoin(DataSet right, String columnName, String joinColumnNameOnRight) throws IllegalArgumentException;

    /**
     * Performs an inner join operation between this DataSet and another DataSet based on the specified column names.
     * The inner join operation combines rows from two DataSets based on related columns between them.
     * Only rows that have matching values in both DataSets will be included in the resulting DataSet.
     *
     * @param right The other DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet and the value is the corresponding column name in the other DataSet.
     * @return A new DataSet that is the result of the inner join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet innerJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Performs an inner join operation between this DataSet and another DataSet based on the specified column names.
     * The inner join operation combines rows from two DataSets based on related columns between them.
     * Only rows that have matching values in both DataSets will be included in the resulting DataSet.
     * Additionally, a new column is added to the resulting DataSet, with its type specified and is populated with values from the right DataSet.
     *
     * @param right The other DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet and the value is the corresponding column name in the other DataSet.
     * @param newColumnName The name of the new column to be added to the resulting DataSet.
     * @param newColumnType The type of the new column to be added to the resulting DataSet. It must be Object[], Collection, Map, or Bean class.
     * @return A new DataSet that is the result of the inner join operation, including the new column.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet innerJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType) throws IllegalArgumentException;

    /**
     * Performs an inner join operation between this DataSet and another DataSet based on the specified column names.
     * The inner join operation combines rows from two DataSets based on related columns between them.
     * Only rows that have matching values in both DataSets will be included in the resulting DataSet.
     * Additionally, a new column is added to the resulting DataSet, with its type specified and is populated with values from the right DataSet.
     *
     * @param right The DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet and the value is the column name in the right DataSet to join on.
     * @param newColumnName The name of the new column to be added to the resulting DataSet.
     * @param newColumnType The type of the new column. It must be Object[], Collection, Map, or Bean class.
     * @param collSupplier A function that generates a collection to hold the joined rows for the new column for one-many or many-many mapping.
     * @return A new DataSet that is the result of the inner join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    @SuppressWarnings("rawtypes")
    DataSet innerJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException;

    /**
     * Performs a left join operation between this DataSet and another DataSet based on the specified column names.
     * The left join operation combines rows from two DataSets based on related columns between them.
     * All rows from the left DataSet and the matched rows from the right DataSet will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the right side.
     *
     * @param right The other DataSet to join with.
     * @param columnName The column name in this DataSet to join on.
     * @param joinColumnNameOnRight The column name in the other DataSet to join on.
     * @return A new DataSet that is the result of the left join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet leftJoin(DataSet right, String columnName, String joinColumnNameOnRight);

    /**
     * Performs a left join operation between this DataSet and another DataSet based on the specified column names.
     * The left join operation combines rows from two DataSets based on related columns between them.
     * All rows from the left DataSet and the matched rows from the right DataSet will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the right side.
     *
     * @param right The other DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet to join on, and the value is the column name in the other DataSet to join on.
     * @return A new DataSet that is the result of the left join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet leftJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Performs a left join operation between this DataSet and another DataSet based on the specified column names.
     * The left join operation combines rows from two DataSets based on related columns between them.
     * All rows from the left DataSet and the matched rows from the right DataSet will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the right side.
     * Additionally, a new column is added to the resulting DataSet, with its type specified.
     *
     * @param right The other DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet to join on, and the value is the column name in the other DataSet to join on.
     * @param newColumnName The name of the new column to be added to the resulting DataSet.
     * @param newColumnType The type of the new column to be added to the resulting DataSet. It must be Object[], Collection, Map, or Bean class.
     * @return A new DataSet that is the result of the left join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet leftJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType) throws IllegalArgumentException;

    /**
     * Performs a left join operation between this DataSet and another DataSet based on the specified column names.
     * The left join operation combines rows from two DataSets based on related columns between them.
     * All rows from the left DataSet and the matched rows from the right DataSet will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the right side.
     * Additionally, a new column is added to the resulting DataSet, with its type specified.
     * A custom collection supplier can be provided to control the type of collection used in the join operation.
     *
     * @param right The other DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet to join on, and the value is the column name in the other DataSet to join on.
     * @param newColumnName The name of the new column to be added to the resulting DataSet.
     * @param newColumnType The type of the new column to be added to the resulting DataSet. It must be Object[], Collection, Map, or Bean class.
     * @param collSupplier A function that generates a collection to hold the joined rows for the new column for one-many or many-many mapping.
     * @return A new DataSet that is the result of the left join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    @SuppressWarnings("rawtypes")
    DataSet leftJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException;

    /**
     * Performs a right join operation between this DataSet and another DataSet based on the specified column names.
     * The right join operation combines rows from two DataSets based on related columns between them.
     * All rows from the right DataSet and the matched rows from the left DataSet will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the left side.
     *
     * @param right The other DataSet to join with.
     * @param columnName The column name in this DataSet to join on.
     * @param joinColumnNameOnRight The column name in the other DataSet to join on.
     * @return A new DataSet that is the result of the right join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet rightJoin(DataSet right, String columnName, String joinColumnNameOnRight) throws IllegalArgumentException;

    /**
     * Performs a right join operation between this DataSet and another DataSet based on the specified column names.
     * The right join operation combines rows from two DataSets based on related columns between them.
     * All rows from the right DataSet and the matched rows from the left DataSet will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the left side.
     *
     * @param right The other DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet to join on, and the value is the column name in the other DataSet to join on.
     * @return A new DataSet that is the result of the right join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet rightJoin(DataSet right, Map<String, String> onColumnNames) throws IllegalArgumentException;

    /**
     * Performs a right join operation between this DataSet and another DataSet based on the specified column names.
     * The right join operation combines rows from two DataSets based on related columns between them.
     * All rows from the right DataSet and the matched rows from the left DataSet will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the left side.
     * Additionally, a new column is added to the resulting DataSet, with its type specified by the user.
     *
     * @param right The other DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet to join on, and the value is the column name in the other DataSet to join on.
     * @param newColumnName The name of the new column to be added to the resulting DataSet.
     * @param newColumnType The type of the new column to be added to the resulting DataSet. It must be Object[], Collection, Map, or Bean class.
     * @return A new DataSet that is the result of the right join operation, with the additional column.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet rightJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType) throws IllegalArgumentException;

    /**
     * Performs a right join operation between this DataSet and another DataSet based on the specified column names.
     * The right join operation combines rows from two DataSets based on related columns between them.
     * All rows from the right DataSet and the matched rows from the left DataSet will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the left side.
     * Additionally, a new column is added to the resulting DataSet, with its type specified by the user.
     *
     * @param right The other DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet to join on, and the value is the column name in the other DataSet to join on.
     * @param newColumnName The name of the new column to be added to the resulting DataSet.
     * @param newColumnType The type of the new column to be added to the resulting DataSet. It must be Object[], Collection, Map, or Bean class.
     * @param collSupplier A function that generates a collection to hold the joined rows for the new column for one-many or many-many mapping.
     * @return A new DataSet that is the result of the right join operation, with the additional column.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    @SuppressWarnings("rawtypes")
    DataSet rightJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException;

    /**
     * Performs a full join operation between this DataSet and another DataSet based on the specified column names.
     * The full join operation combines rows from two DataSets based on related columns between them.
     * All rows from both DataSets will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the side of the DataSet that does not have a match.
     *
     * @param right The other DataSet to join with.
     * @param columnName The column name in this DataSet to join on.
     * @param joinColumnNameOnRight The column name in the other DataSet to join on.
     * @return A new DataSet that is the result of the full join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet fullJoin(DataSet right, String columnName, String joinColumnNameOnRight) throws IllegalArgumentException;

    /**
     * Performs a full join operation between this DataSet and another DataSet based on the specified column names.
     * The full join operation combines rows from two DataSets based on related columns between them.
     * All rows from both DataSets will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the side of the DataSet that does not have a match.
     *
     * @param right The other DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet to join on, and the value is the column name in the other DataSet to join on.
     * @return A new DataSet that is the result of the full join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet fullJoin(DataSet right, Map<String, String> onColumnNames) throws IllegalArgumentException;

    /**
     * Performs a full join operation between this DataSet and another DataSet based on the specified column names.
     * The full join operation combines rows from two DataSets based on related columns between them.
     * All rows from both DataSets will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the side of the DataSet that does not have a match.
     *
     * @param right The other DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet to join on, and the value is the column name in the other DataSet to join on.
     * @param newColumnName The name of the new column to be added to the resulting DataSet.
     * @param newColumnType The type of the new column to be added to the resulting DataSet. It must be Object[], Collection, Map, or Bean class.
     * @return A new DataSet that is the result of the full join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    DataSet fullJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType) throws IllegalArgumentException;

    /**
     * Performs a full join operation between this DataSet and another DataSet based on the specified column names.
     * The full join operation combines rows from two DataSets based on related columns between them.
     * All rows from both DataSets will be included in the resulting DataSet.
     * If there is no match, the result is {@code null} on the side of the DataSet that does not have a match.
     *
     * @param right The other DataSet to join with.
     * @param onColumnNames A map where the key is the column name in this DataSet to join on, and the value is the column name in the other DataSet to join on.
     * @param newColumnName The name of the new column to be added to the resulting DataSet.
     * @param newColumnType The type of the new column to be added to the resulting DataSet. It must be Object[], Collection, Map, or Bean class.
     * @param collSupplier A function that generates a collection to hold the joined rows for the new column for one-many or many-many mapping.
     * @return A new DataSet that is the result of the full join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective DataSets or the specified {@code right} DataSet is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    @SuppressWarnings("rawtypes")
    DataSet fullJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException;

    /**
     * Performs a union operation between this DataSet and another DataSet.
     * The union operation combines all rows from both DataSets into a new DataSet.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other The other DataSet to union with.
     * @return A new DataSet that is the result of the union operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}.
     */
    DataSet union(DataSet other) throws IllegalArgumentException;

    /**
     * Performs a union operation between this DataSet and another DataSet.
     * The union operation combines all rows from both DataSets into a new DataSet.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other The other DataSet to union with.
     * @param requiresSameColumns A boolean value that determines whether the union operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the union operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null} or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     */
    DataSet union(DataSet other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a union operation between this DataSet and another DataSet.
     * The union operation combines all rows from both DataSets into a new DataSet.
     * Duplicated rows detected by {@code keyColumnNames} in the returned {@code DataSet} will be eliminated.
     *
     * @param other The other DataSet to union with.
     * @param keyColumnNames The collection of column names to be used as keys for duplicate detection.
     * @return A new DataSet that is the result of the union operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null} or if the keyColumnNames is {@code null} or empty.
     */
    DataSet union(DataSet other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs a union operation between this DataSet and another DataSet.
     * The union operation combines all rows from both DataSets into a new DataSet.
     * Duplicated rows detected by {@code keyColumnNames} in the returned {@code DataSet} will be eliminated.
     *
     * @param other The other DataSet to union with.
     * @param keyColumnNames The collection of column names to be used as keys for duplicate detection.
     * @param requiresSameColumns A boolean value that determines whether the union operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the union operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns, or if the keyColumnNames is {@code null} or empty.
     */
    DataSet union(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a union all operation between this DataSet and another DataSet.
     * The union all operation combines all rows from both DataSets into a new DataSet.
     * Unlike the union operation, union all includes duplicate rows in the resulting DataSet.
     *
     * @param other The other DataSet to union with.
     * @return A new DataSet that is the result of the union all operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}.
     */
    DataSet unionAll(DataSet other) throws IllegalArgumentException;

    /**
     * Performs a union all operation between this DataSet and another DataSet.
     * The union all operation combines all rows from both DataSets into a new DataSet.
     * Unlike the union operation, union all includes duplicate rows in the resulting DataSet.
     *
     * @param other The other DataSet to union with.
     * @param requiresSameColumns A boolean value that determines whether the union all operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the union all operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     */
    DataSet unionAll(DataSet other, boolean requiresSameColumns) throws IllegalArgumentException;

    //    /**
    //     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
    //     * Duplicated rows in the returned {@code DataSet} will be eliminated.
    //     *
    //     * @param other
    //     * @param keyColumnNames this parameter won't be used. adding it here to be consistent with {@code union(DataSet, Collection)}
    //     * @return a new DataSet
    //     */
    //    @Beta
    //    DataSet unionAll(DataSet other, Collection<String> keyColumnNames);
    //
    //    /**
    //     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
    //     * Duplicated rows in the returned {@code DataSet} will be eliminated.
    //     *
    //     * @param other
    //     * @param keyColumnNames this parameter won't be used. Adding it here to be consistent with {@code union(DataSet, Collection, boolean)}
    //     * @param requiresSameColumns
    //     * @return a new DataSet
    //     */
    //    @Beta
    //    DataSet unionAll(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns);

    /**
     * Performs an intersection operation between this DataSet and another DataSet.
     * The intersection operation returns a new DataSet that contains only the rows that are common to both DataSets.
     * Duplicated rows in the returned DataSet will be eliminated.
     *
     * @param other The other DataSet to intersect with.
     * @return A new DataSet that is the result of the intersection operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}.
     */
    DataSet intersect(DataSet other) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this DataSet and another DataSet.
     * The intersection operation returns a new DataSet that contains only the rows that are common to both DataSets.
     * Duplicated rows in the returned DataSet will be eliminated.
     *
     * @param other The other DataSet to intersect with.
     * @param requiresSameColumns A boolean value that determines whether the intersection operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the intersection operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     */
    DataSet intersect(DataSet other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this DataSet and another DataSet.
     * The intersection operation returns a new DataSet that contains only the rows that are common to both DataSets based on the key columns.
     * Duplicated rows in the returned DataSet will be eliminated.
     *
     * @param other The other DataSet to intersect with.
     * @param keyColumnNames The collection of column names to be used as keys for the intersection operation.
     * @return A new DataSet that is the result of the intersection operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if the keyColumnNames is {@code null} or empty.
     */
    DataSet intersect(DataSet other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this DataSet and another DataSet.
     * The intersection operation returns a new DataSet that contains only the rows that are common to both DataSets based on the key columns.
     * Duplicated rows in the returned DataSet will be eliminated.
     *
     * @param other The other DataSet to intersect with.
     * @param keyColumnNames The collection of column names to be used as keys for the intersection operation.
     * @param requiresSameColumns A boolean value that determines whether the intersection operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the intersection operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if the keyColumnNames is {@code null} or empty, or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     */
    DataSet intersect(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this DataSet and another DataSet.
     * The intersection operation returns a new DataSet that contains only the rows that are common to both DataSets.
     * Duplicated rows in the returned DataSet will not be eliminated.
     *
     * @param other The other DataSet to intersect with.
     * @return A new DataSet that is the result of the intersection operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}.
     */
    DataSet intersectAll(DataSet other) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this DataSet and another DataSet.
     * The intersection operation returns a new DataSet that contains only the rows that are common to both DataSets.
     * Duplicated rows in the returned DataSet will not be eliminated.
     *
     * @param other The other DataSet to intersect with.
     * @param requiresSameColumns A boolean value that determines whether the intersection operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the intersection operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     */
    DataSet intersectAll(DataSet other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this DataSet and another DataSet.
     * The intersection operation returns a new DataSet that contains only the rows that are common to both DataSets based on the key columns.
     * Duplicated rows in the returned DataSet will not be eliminated.
     *
     * @param other The other DataSet to intersect with.
     * @param keyColumnNames The collection of column names to be used as keys for the intersection operation.
     * @return A new DataSet that is the result of the intersection operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if the keyColumnNames is {@code null} or empty.
     */
    DataSet intersectAll(DataSet other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this DataSet and another DataSet.
     * The intersection operation returns a new DataSet that contains only the rows that are common to both DataSets based on the key columns.
     * Duplicated rows in the returned DataSet will not be eliminated.
     *
     * @param other The other DataSet to intersect with.
     * @param keyColumnNames The collection of column names to be used as keys for the intersection operation.
     * @param requiresSameColumns A boolean value that determines whether the intersection operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the intersection operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if the keyColumnNames is {@code null} or empty, or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     */
    DataSet intersectAll(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this DataSet and another DataSet.
     * The difference operation returns a new DataSet that includes rows that are in this DataSet but not in the provided DataSet.
     * Duplicated rows in the returned DataSet will be eliminated.
     *
     * @param other The other DataSet to compare with.
     * @return A new DataSet that is the result of the difference operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}.
     */
    DataSet except(DataSet other) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this DataSet and another DataSet.
     * The difference operation returns a new DataSet that includes rows that are in this DataSet but not in the provided DataSet.
     * Duplicated rows in the returned DataSet will be eliminated.
     *
     * @param other The other DataSet to compare with.
     * @param requiresSameColumns A boolean value that determines whether the difference operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the difference operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     */
    DataSet except(DataSet other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this DataSet and another DataSet.
     * The difference operation returns a new DataSet that includes rows that are in this DataSet but not in the provided DataSet based on the key columns.
     * Duplicated rows in the returned DataSet will be eliminated.
     *
     * @param other The other DataSet to compare with.
     * @param keyColumnNames The collection of column names to be used as keys for the difference operation.
     * @return A new DataSet that is the result of the difference operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if the keyColumnNames is {@code null} or empty.
     */
    DataSet except(DataSet other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this DataSet and another DataSet.
     * The difference operation returns a new DataSet that includes rows that are in this DataSet but not in the provided DataSet based on the key columns.
     * Duplicated rows in the returned DataSet will be eliminated.
     *
     * @param other The other DataSet to compare with.
     * @param keyColumnNames The collection of column names to be used as keys for the difference operation.
     * @param requiresSameColumns A boolean value that determines whether the difference operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the difference operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if the keyColumnNames is {@code null} or empty, or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     */
    DataSet except(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this DataSet and another DataSet.
     * The difference operation returns a new DataSet that includes rows that are in this DataSet but not in the provided DataSet.
     * Duplicated rows in the returned DataSet will not be eliminated.
     *
     * @param other The other DataSet to compare with.
     * @return A new DataSet that is the result of the difference operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}.
     */
    DataSet exceptAll(DataSet other) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this DataSet and another DataSet.
     * The difference operation returns a new DataSet that includes rows that are in this DataSet but not in the provided DataSet.
     * Duplicated rows in the returned DataSet will not be eliminated.
     *
     * @param other The other DataSet to compare with.
     * @param requiresSameColumns A boolean value that determines whether the difference operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the difference operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     */
    DataSet exceptAll(DataSet other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this DataSet and another DataSet.
     * The difference operation returns a new DataSet that includes rows that are in this DataSet but not in the provided DataSet based on the key columns.
     * Duplicated rows in the returned DataSet will not be eliminated.
     *
     * @param other The other DataSet to compare with.
     * @param keyColumnNames The collection of column names to be used as keys for the difference operation.
     * @return A new DataSet that is the result of the difference operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if the keyColumnNames is {@code null} or empty.
     */
    DataSet exceptAll(DataSet other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this DataSet and another DataSet.
     * The difference operation returns a new DataSet that includes rows that are in this DataSet but not in the provided DataSet based on the key columns.
     * Duplicated rows in the returned DataSet will not be eliminated.
     *
     * @param other The other DataSet to compare with.
     * @param keyColumnNames The collection of column names to be used as keys for the difference operation.
     * @param requiresSameColumns A boolean value that determines whether the difference operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the difference operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if the keyColumnNames is {@code null} or empty, or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     */
    DataSet exceptAll(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new DataSet containing rows that appear in both this DataSet and the specified DataSet.
     * The intersection contains rows that exist in both DataSets based on matching column values.
     * For rows that appear multiple times, the result contains the minimum number of occurrences present in both DataSets.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * <p>Example:
     * <pre>
     * // DataSet 1 with columns "id", "name"
     * DataSet ds1 = DataSet.rows(
     *     Arrays.asList("id", "name"),
     *     new Object[][] {
     *         {1, "Alice"},
     *         {2, "Bob"},
     *         {3, "Charlie"},
     *         {2, "Bob"}  // duplicate row
     *     }
     * );
     *
     * // DataSet 2 with columns "id", "name"
     * DataSet ds2 = DataSet.rows(
     *     Arrays.asList("id", "name"),
     *     new Object[][] {
     *         {2, "Bob"},
     *         {3, "Charlie"},
     *         {4, "Dave"},
     *         {2, "Bob"},  // duplicate row
     *         {2, "Bob"}   // another duplicate
     *     }
     * );
     *
     * // Result will contain {2, "Bob"} twice and {3, "Charlie"} once
     * DataSet result = ds1.intersection(ds2);
     * </pre>
     *
     *
     * @param other the DataSet to find common rows with
     * @return a new DataSet containing rows present in both DataSets, with duplicates handled based on minimum occurrences
     * @throws IllegalArgumentException if the other DataSet is {@code null} or the two {@code DataSets} don't have common columns.
     * @see #intersect(DataSet)
     * @see #intersectAll(DataSet)
     * @see #intersection(DataSet, boolean)
     * @see #intersection(DataSet, Collection)
     * @see N#intersection(int[], int[])
     */
    DataSet intersection(DataSet other) throws IllegalArgumentException;

    /**
     * Returns a new DataSet containing rows that appear in both this DataSet and the specified DataSet.
     * The intersection contains rows that exist in both DataSets based on matching column values.
     * For rows that appear multiple times, the result contains the minimum number of occurrences present in both DataSets.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     * 
     * <p>Example:
     * <pre>
     * // DataSet 1 with columns "id", "name"
     * DataSet ds1 = DataSet.rows(
     *     Arrays.asList("id", "name"),
     *     new Object[][] {
     *         {1, "Alice"},
     *         {2, "Bob"},
     *         {3, "Charlie"}
     *     }
     * );
     * 
     * // DataSet 2 with columns "id", "name", "age"
     * DataSet ds2 = DataSet.rows(
     *     Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *         {2, "Bob", 30},
     *         {3, "Charlie", 25},
     *         {4, "Dave", 35}
     *     }
     * );
     * 
     * // With requiresSameColumns=true, this would throw IllegalArgumentException
     * // With requiresSameColumns=false, result will contain common rows based on common columns
     * DataSet result = ds1.intersection(ds2, false);
     * // result will contain {2, "Bob"} and {3, "Charlie"} with only the columns from ds1
     * </pre>
     *
     * @param other the DataSet to find common rows with
     * @param requiresSameColumns if true, both DataSets must have identical column structures;
     *                           if false, the intersection is based on common columns only
     * @return a new DataSet containing rows present in both DataSets, with duplicates handled based on minimum occurrences
     * @throws IllegalArgumentException if the other DataSet is {@code null} or if {@code requiresSameColumns} is true
     *                                  and the DataSets have different column structures
     * @see #intersection(DataSet)
     * @see #intersection(DataSet, Collection)
     * @see #intersection(DataSet, Collection, boolean)
     * @see N#intersection(int[], int[])
     * @see #intersect(DataSet)
     * @see #intersectAll(DataSet)
     */
    DataSet intersection(DataSet other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new DataSet containing rows that appear in both this DataSet and the specified DataSet,
     * based on matching values in the specified key columns. The intersection considers duplicates,
     * retaining the minimum number of occurrences present in both DataSets.
     * <p>The method compares only the specified key columns rather than entire rows.
     * Columns from this DataSet are preserved in the result.
     *
     * <p>Example:
     * <pre>
     * // DataSet 1 with columns "id", "name", "department"
     * DataSet ds1 = DataSet.rows(
     *     Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *         {1, "Alice", "HR"},
     *         {2, "Bob", "Engineering"},
     *         {3, "Charlie", "Marketing"},
     *         {2, "Bob", "Engineering"}  // duplicate row
     *     }
     * );
     *
     * // DataSet 2 with columns "id", "name", "salary"
     * DataSet ds2 = DataSet.rows(
     *     Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *         {2, "Bob", 75000},
     *         {3, "Charlie", 65000},
     *         {4, "Dave", 70000},
     *         {2, "Bob", 80000}  // different salary but same keys
     *     }
     * );
     *
     * // Result will contain rows matching on id and name columns
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * DataSet result = ds1.intersection(ds2, keyColumns);
     * // result will contain {2, "Bob", "Engineering"} twice and {3, "Charlie", "Marketing"} once
     * // with column structure matching ds1
     * </pre>
     *
     *
     * @param other the DataSet to find common rows with
     * @param keyColumnNames the column names to use for matching rows between DataSets
     * @return a new DataSet containing rows whose key column values appear in both DataSets,
     *         with duplicates handled based on minimum occurrences
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if keyColumnNames is
     *                                  {@code null} or empty, or if any specified key column doesn't
     *                                  exist in either DataSet
     * @see #intersection(DataSet)
     * @see #intersection(DataSet, boolean)
     * @see #intersection(DataSet, Collection, boolean)
     * @see #intersect(DataSet, Collection)
     * @see #intersectAll(DataSet, Collection)
     */
    DataSet intersection(DataSet other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Returns a new DataSet containing rows that appear in both this DataSet and the specified DataSet,
     * based on matching values in the specified key columns. The intersection considers duplicates,
     * retaining the minimum number of occurrences present in both DataSets.
     *
     * <p>Example:
     * <pre>
     * // DataSet 1 with columns "id", "name", "department"
     * DataSet ds1 = DataSet.rows(
     *     Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *         {1, "Alice", "HR"},
     *         {2, "Bob", "Engineering"},
     *         {3, "Charlie", "Marketing"},
     *         {2, "Bob", "Engineering"}  // duplicate row
     *     }
     * );
     *
     * // DataSet 2 with columns "id", "name", "salary"
     * DataSet ds2 = DataSet.rows(
     *     Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *         {2, "Bob", 75000},
     *         {3, "Charlie", 65000},
     *         {4, "Dave", 70000},
     *         {2, "Bob", 80000}  // different salary but same keys
     *     }
     * );
     *
     * // With requiresSameColumns=false, different column structures are allowed
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * DataSet result = ds1.intersection(ds2, keyColumns, false);
     * // result will contain {2, "Bob", "Engineering"} twice and {3, "Charlie", "Marketing"} once
     * // with column structure matching ds1
     * </pre>
     *
     * <p>The method compares only the specified key columns rather than entire rows.
     * When {@code requiresSameColumns} is false, the columns from this DataSet are preserved in the result.
     * When {@code requiresSameColumns} is true, both DataSets must have identical column structures.
     *
     * @param other the DataSet to find common rows with
     * @param keyColumnNames the column names to use for matching rows between DataSets
     * @param requiresSameColumns if true, both DataSets must have identical column structures;
     *                           if false, the intersection is based on common columns only
     * @return a new DataSet containing rows whose key column values appear in both DataSets,
     *         with duplicates handled based on minimum occurrences
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if keyColumnNames is
     *                                  {@code null} or empty, or if any specified key column doesn't
     *                                  exist in either DataSet, or if {@code requiresSameColumns} is true
     *                                  and the DataSets have different column structures
     * @see #intersection(DataSet)
     * @see #intersection(DataSet, boolean)
     * @see #intersection(DataSet, Collection)
     * @see #intersect(DataSet, Collection)
     * @see #intersectAll(DataSet, Collection)
     */
    DataSet intersection(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new DataSet with the rows in this DataSet but not in the specified DataSet {@code other},
     * considering the number of occurrences of each row.
     * Duplicated rows in the returned DataSet will not be eliminated.
     *
     * <p>Example:
     * <pre>
     * DataSet ds1 = DataSet.rows(Arrays.asList("id", "name"), new Object[][] {
     *     {1, "Alice"}, {2, "Bob"}, {3, "Charlie"}, {3, "Charlie"}
     * });
     * DataSet ds2 = DataSet.rows(Arrays.asList("id", "name"), new Object[][] {
     *     {1, "Alice"}, {4, "David"}, {3, "Charlie"}
     * });
     * DataSet result = ds1.difference(ds2); 
     * // result will contain: {2, "Bob"}, {3, "Charlie"}
     * // One "Charlie" row remains because ds1 has two occurrences and ds2 has one
     *
     * DataSet ds3 = DataSet.rows(Arrays.asList("id", "name"), new Object[][] {
     *     {5, "Eva"}, {6, "Frank"}
     * });
     * DataSet ds4 = DataSet.rows(Arrays.asList("id", "name"), new Object[][] {
     *     {5, "Eva"}, {5, "Eva"}, {6, "Frank"}
     * });
     * DataSet result2 = ds3.difference(ds4);
     * // result2 will be empty
     * // No rows remain because ds4 has at least as many occurrences of each row as ds3
     * </pre>
     *
     * @param other the DataSet to compare against this DataSet
     * @return a new DataSet containing the rows that are present in this DataSet but not in the specified DataSet,
     *         considering the number of occurrences
     * @throws IllegalArgumentException if the other DataSet is {@code null} or the two {@code DataSets} don't have common columns.
     * @see #difference(DataSet, boolean)
     * @see #difference(DataSet, Collection)
     * @see #symmetricDifference(DataSet)
     * @see #intersection(DataSet)
     * @see N#difference(Collection, Collection)
     * @see N#difference(int[], int[])
     */
    DataSet difference(DataSet other) throws IllegalArgumentException;

    /**
     * Returns a new DataSet with the rows in this DataSet but not in the specified DataSet {@code other},
     * considering the number of occurrences of each row.
     * Duplicated rows in the returned DataSet will not be eliminated.
     *
     * <p>Example:
     * <pre>
     * DataSet ds1 = DataSet.rows(Arrays.asList("id", "name"), new Object[][] {
     *     {1, "Alice"}, {2, "Bob"}, {3, "Charlie"}, {3, "Charlie"}
     * });
     * DataSet ds2 = DataSet.rows(Arrays.asList("id", "name"), new Object[][] {
     *     {1, "Alice"}, {4, "David"}, {3, "Charlie"}
     * });
     * 
     * // With requiresSameColumns = true
     * DataSet result1 = ds1.difference(ds2, true);
     * // result1 will contain: {2, "Bob"}, {3, "Charlie"}
     * 
     * // With requiresSameColumns = false
     * DataSet ds3 = DataSet.rows(Arrays.asList("id", "address"), new Object[][] {
     *     {1, "123 Main St"}, {3, "456 Oak Ave"}
     * });
     * DataSet result2 = ds1.difference(ds3, false);
     * // result2 will contain rows from ds1 that don't match in the common columns
     * </pre>
     *
     *
     * @param other the DataSet to compare against this DataSet
     * @param requiresSameColumns whether both DataSets must have identical column names
     * @return a new DataSet containing the rows that are present in this DataSet but not in the specified DataSet,
     *         considering the number of occurrences
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if <i>requiresSameColumns</i> is 
     *         {@code true} and the DataSets do not have the same columns
     * @see #difference(DataSet)
     * @see #difference(DataSet, Collection)
     * @see #symmetricDifference(DataSet)
     * @see #intersection(DataSet)
     * @see N#difference(Collection, Collection)
     * @see N#difference(int[], int[])
     */
    DataSet difference(DataSet other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new DataSet with the rows in this DataSet but not in the specified DataSet {@code other},
     * comparing rows based only on the values in the specified key columns and considering the number of occurrences.
     * Duplicated rows in the returned DataSet will not be eliminated.
     *
     * <p>Example:
     * <pre>
     * DataSet ds1 = DataSet.rows(Arrays.asList("id", "name", "dept"), new Object[][] {
     *     {1, "Alice", "HR"}, {2, "Bob", "IT"}, {3, "Charlie", "Finance"}, {3, "Charlie", "IT"}
     * });
     * DataSet ds2 = DataSet.rows(Arrays.asList("id", "name", "role"), new Object[][] {
     *     {1, "Alice", "Manager"}, {3, "Charlie", "Analyst"}
     * });
     * 
     * // Compare only by "id" column
     * DataSet result1 = ds1.difference(ds2, Arrays.asList("id"));
     * // result1 will contain: {2, "Bob", "IT"}, {3, "Charlie", "IT"}
     * // One "Charlie" row remains since ds1 has two occurrences and ds2 has one with id=3
     * 
     * // Compare by both "id" and "name" columns
     * DataSet result2 = ds1.difference(ds2, Arrays.asList("id", "name"));
     * // result2 will contain: {2, "Bob", "IT"}, {3, "Charlie", "IT"}
     * // The result is the same because both key fields match for Alice and Charlie
     * </pre>
     *
     * @param other the DataSet to compare against this DataSet
     * @param keyColumnNames the column names to use for comparison
     * @return a new DataSet containing the rows that are present in this DataSet but not in the specified DataSet,
     *         based on the specified key columns and considering the number of occurrences
     * @throws IllegalArgumentException if the other DataSet is {@code null} or if keyColumnNames is {@code null} or empty
     * @see #difference(DataSet)
     * @see #difference(DataSet, boolean)
     * @see #symmetricDifference(DataSet, Collection)
     * @see #intersection(DataSet, Collection)
     * @see N#difference(Collection, Collection)
     * @see N#difference(int[], int[])
     */
    DataSet difference(DataSet other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Returns a new DataSet with the rows in this DataSet but not in the specified DataSet {@code other},
     * comparing rows based only on the values in the specified key columns and considering the number of occurrences.
     * Duplicated rows in the returned DataSet will not be eliminated.
     * 
     * <p>Example:
     * <pre>
     * DataSet ds1 = DataSet.rows(Arrays.asList("id", "name", "dept"), new Object[][] {
     *     {1, "Alice", "HR"}, {2, "Bob", "IT"}, {3, "Charlie", "Finance"}, {3, "Charlie", "IT"}
     * });
     * DataSet ds2 = DataSet.rows(Arrays.asList("id", "name", "role"), new Object[][] {
     *     {1, "Alice", "Manager"}, {3, "Charlie", "Analyst"}
     * });
     * 
     * // With requiresSameColumns = true
     * try {
     *     DataSet result = ds1.difference(ds2, Arrays.asList("id", "name"), true);
     *     // Will throw IllegalArgumentException as the DataSets have different columns
     * } catch (IllegalArgumentException e) {
     *     // Handle exception
     * }
     * 
     * // With requiresSameColumns = false
     * DataSet result = ds1.difference(ds2, Arrays.asList("id", "name"), false);
     * // result will contain: {2, "Bob", "IT"}, {3, "Charlie", "IT"}
     * // One "Charlie" row remains since ds1 has two occurrences and ds2 has one
     * </pre>
     *
     * @param other the DataSet to compare against this DataSet
     * @param keyColumnNames the column names to use for comparison
     * @param requiresSameColumns whether both DataSets must have identical column names
     * @return a new DataSet containing the rows that are present in this DataSet but not in the specified DataSet,
     *         based on the specified key columns and considering the number of occurrences
     * @throws IllegalArgumentException if the other DataSet is {@code null}, if keyColumnNames is {@code null} or empty,
     *         or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns
     * @see #difference(DataSet)
     * @see #difference(DataSet, boolean)
     * @see #difference(DataSet, Collection)
     * @see #symmetricDifference(DataSet, Collection)
     * @see #intersection(DataSet, Collection)
     * @see N#difference(Collection, Collection)
     * @see N#difference(int[], int[])
     */
    DataSet difference(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new DataSet containing rows that are present in either this DataSet or the specified DataSet,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For rows that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of rows is preserved, with rows from this DataSet appearing first,
     * followed by rows from the specified DataSet that aren't in this DataSet.
     *
     * <p>Example:
     * <pre>
     * DataSet ds1 = DataSet.rows(Arrays.asList("id", "name"), new Object[][] {
     *     {1, "Alice"}, {2, "Bob"}, {2, "Bob"}, {3, "Charlie"}
     * });
     * DataSet ds2 = DataSet.rows(Arrays.asList("id", "name"), new Object[][] {
     *     {2, "Bob"}, {3, "Charlie"}, {4, "David"}, {4, "David"}
     * });
     * DataSet result = ds1.symmetricDifference(ds2);
     * // result will contain:
     * // {1, "Alice"}, {2, "Bob"}, {4, "David"}, {4, "David"}
     * // Rows explanation:
     * // - {1, "Alice"} appears only in ds1, so it remains
     * // - {2, "Bob"} appears twice in ds1 and once in ds2, so one occurrence remains
     * // - {3, "Charlie"} appears once in each DataSet, so it's removed from both
     * // - {4, "David"} appears twice in ds2 and not in ds1, so both occurrences remain
     * </pre>
     *
     * @param other the DataSet to find symmetric difference with this DataSet
     * @return a new DataSet containing rows that are present in either this DataSet or the specified DataSet,
     *         but not in both, considering the number of occurrences
     * @throws IllegalArgumentException if the other DataSet is {@code null} or the two {@code DataSets} don't have common columns.
     * @see #symmetricDifference(DataSet, boolean)
     * @see #symmetricDifference(DataSet, Collection)
     * @see #difference(DataSet)
     * @see #intersection(DataSet)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#symmetricDifference(int[], int[])
     */
    DataSet symmetricDifference(DataSet other) throws IllegalArgumentException;

    /**
     * Returns a new DataSet containing rows that are present in either this DataSet or the specified DataSet,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For rows that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of rows is preserved, with rows from this DataSet appearing first,
     * followed by rows from the specified DataSet that aren't in this DataSet.
     *
     * <p>Example:
     * <pre>
     * DataSet ds1 = DataSet.rows(Arrays.asList("id", "name", "dept"), new Object[][] {
     *     {1, "Alice", "HR"}, {2, "Bob", "IT"}, {2, "Bob", "IT"}, {3, "Charlie", "Finance"}
     * });
     * DataSet ds2 = DataSet.rows(Arrays.asList("id", "name", "role"), new Object[][] {
     *     {2, "Bob", "Manager"}, {3, "Charlie", "Analyst"}, {4, "David", "Developer"}
     * });
     * 
     * // With requiresSameColumns = true
     * try {
     *     DataSet result = ds1.symmetricDifference(ds2, true);
     *     // Will throw IllegalArgumentException as the DataSets have different columns
     * } catch (IllegalArgumentException e) {
     *     System.out.println("DataSets must have identical columns");
     * }
     * 
     * // With requiresSameColumns = false
     * DataSet result = ds1.symmetricDifference(ds2, false);
     * // result will contain:
     * // {1, "Alice", "HR"}, {2, "Bob", "IT"}, {4, "David", "Developer"}
     * // Rows explanation:
     * // - {1, "Alice", "HR"} appears only in ds1, so it remains
     * // - {2, "Bob", "IT"} appears twice in ds1 and once in ds2, so one occurrence remains
     * // - {3, "Charlie", "Finance"} and {3, "Charlie", "Analyst"} appear once in each DataSet, so they're removed
     * // - {4, "David", "Developer"} appears only in ds2, so it remains
     * </pre>
     *
     * @param other the DataSet to find symmetric difference with this DataSet
     * @param requiresSameColumns whether both DataSets must have identical column names
     * @return a new DataSet containing rows that are present in either this DataSet or the specified DataSet,
     *         but not in both, considering the number of occurrences
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if <i>requiresSameColumns</i> 
     *         is {@code true} and the DataSets do not have the same columns
     * @see #symmetricDifference(DataSet)
     * @see #symmetricDifference(DataSet, Collection)
     * @see #difference(DataSet)
     * @see #intersection(DataSet)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#symmetricDifference(int[], int[])
     */
    DataSet symmetricDifference(DataSet other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new DataSet containing rows that are present in either this DataSet or the specified DataSet,
     * but not in both, based on the specified key columns. This is the set-theoretic symmetric difference operation.
     * For rows that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of rows is preserved, with rows from this DataSet appearing first,
     * followed by rows from the specified DataSet that aren't in this DataSet.
     *
     * <p>Example:
     * <pre>
     * DataSet ds1 = DataSet.rows(Arrays.asList("id", "name", "dept"), new Object[][] {
     *     {1, "Alice", "HR"}, {2, "Bob", "IT"}, {2, "Bob", "IT"}, {3, "Charlie", "Finance"}
     * });
     * DataSet ds2 = DataSet.rows(Arrays.asList("id", "name", "role"), new Object[][] {
     *     {2, "Bob", "Manager"}, {3, "Charlie", "Analyst"}, {4, "David", "Developer"}
     * });
     * 
     * // Using only "id" and "name" as key columns for comparison
     * DataSet result = ds1.symmetricDifference(ds2, Arrays.asList("id", "name"));
     * // result will contain:
     * // {1, "Alice", "HR"}, {2, "Bob", "IT"}, {4, "David", "Developer"}
     * // Rows explanation:
     * // - {1, "Alice", "HR"} appears only in ds1, so it remains
     * // - {2, "Bob", "IT"} appears twice in ds1 and once in ds2, so one occurrence remains
     * // - {3, "Charlie", "Finance"}/{3, "Charlie", "Analyst"} appear once in each DataSet based on key columns, so they're removed
     * // - {4, "David", "Developer"} appears only in ds2, so it remains
     * </pre>
     *
     * @param other the DataSet to find symmetric difference with this DataSet
     * @param keyColumnNames the columns to use for comparison when determining differences
     * @return a new DataSet containing rows that are present in either this DataSet or the specified DataSet,
     *         but not in both, considering the number of occurrences and comparing only specified key columns
     * @throws IllegalArgumentException if the other DataSet is {@code null} or if keyColumnNames is {@code null} or empty
     * @see #symmetricDifference(DataSet)
     * @see #symmetricDifference(DataSet, boolean)
     * @see #difference(DataSet, Collection)
     * @see #intersection(DataSet, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#symmetricDifference(int[], int[])
     */
    DataSet symmetricDifference(DataSet other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Returns a new DataSet containing rows that are present in either this DataSet or the specified DataSet,
     * but not in both, based on the specified key columns. This is the set-theoretic symmetric difference operation.
     * For rows that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of rows is preserved, with rows from this DataSet appearing first,
     * followed by rows from the specified DataSet that aren't in this DataSet.
     *
     * <p>Example:
     * <pre>
     * DataSet ds1 = DataSet.rows(Arrays.asList("id", "name", "dept"), new Object[][] {
     *     {1, "Alice", "HR"}, {2, "Bob", "IT"}, {2, "Bob", "IT"}, {3, "Charlie", "Finance"}
     * });
     * DataSet ds2 = DataSet.rows(Arrays.asList("id", "name", "role"), new Object[][] {
     *     {2, "Bob", "Manager"}, {3, "Charlie", "Analyst"}, {4, "David", "Developer"}
     * });
     *
     * // With requiresSameColumns = true (comparing only "id" and "name" columns)
     * try {
     *     DataSet result = ds1.symmetricDifference(ds2, Arrays.asList("id", "name"), true);
     *     // Will throw IllegalArgumentException as the DataSets have different columns
     * } catch (IllegalArgumentException e) {
     *     System.out.println("DataSets must have identical columns");
     * }
     *
     * // With requiresSameColumns = false (comparing only "id" and "name" columns)
     * DataSet result = ds1.symmetricDifference(ds2, Arrays.asList("id", "name"), false);
     * // result will contain:
     * // {1, "Alice", "HR"}, {2, "Bob", "IT"}, {4, "David", "Developer"}
     * // Rows explanation:
     * // - {1, "Alice", "HR"} appears only in ds1, so it remains
     * // - {2, "Bob", "IT"} appears twice in ds1 and once in ds2, so one occurrence remains
     * // - {3, "Charlie", "Finance"}/{3, "Charlie", "Analyst"} appear once in each DataSet based on key columns, so they're removed
     * // - {4, "David", "Developer"} appears only in ds2, so it remains
     * </pre>
     *
     * @param other the DataSet to find symmetric difference with this DataSet
     * @param keyColumnNames the columns to use for comparison when determining differences
     * @param requiresSameColumns whether both DataSets must have identical column names
     * @return a new DataSet containing rows that are present in either this DataSet or the specified DataSet,
     *         but not in both, considering the number of occurrences and comparing only specified key columns
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if keyColumnNames is {@code null} or empty,
     *         or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns
     * @see #symmetricDifference(DataSet)
     * @see #symmetricDifference(DataSet, boolean)
     * @see #symmetricDifference(DataSet, Collection)
     * @see #difference(DataSet, Collection, boolean)
     * @see #intersection(DataSet, Collection, boolean)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#symmetricDifference(int[], int[])
     */
    DataSet symmetricDifference(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Merges this DataSet with another DataSet.
     * The merge operation combines rows from both DataSets into a new DataSet.
     * If there are columns in the other DataSet that are not present in this DataSet, they will be added to the new DataSet.
     * If there are columns in this DataSet that are not present in the other DataSet, they will also be included in the new DataSet.
     * The rows from both DataSets will be included in the new DataSet, even if they have the same values.
     *
     * @param other The DataSet to merge with.
     * @return A new DataSet that is the result of the merge operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}.
     * @see #prepend(DataSet)
     * @see #append(DataSet)
     */
    DataSet merge(DataSet other) throws IllegalArgumentException;

    /**
     * Merges this DataSet with another DataSet.
     * The merge operation combines rows from both DataSets into a new DataSet.
     * If there are columns in the other DataSet that are not present in this DataSet, they will be added to the new DataSet.
     * If there are columns in this DataSet that are not present in the other DataSet, they will also be included in the new DataSet.
     * The rows from both DataSets will be included in the new DataSet, even if they have the same values.
     *
     * @param other The DataSet to merge with.
     * @param requiresSameColumns A boolean value that determines whether the merge operation requires both DataSets to have the same columns.
     * @return A new DataSet that is the result of the merge operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null}, or if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     * @see #prepend(DataSet)
     * @see #append(DataSet)
     */
    DataSet merge(DataSet other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Merges this DataSet with another DataSet using the specified columns.
     * The merge operation combines rows from both DataSets into a new DataSet.
     * Only the columns specified in the <i>columnNames</i> parameter will be included in the new DataSet.
     * If there are columns in the <i>columnNames</i> that either not in this DataSet or the other DataSet, they will still be added to the new DataSet.
     * The rows from both DataSets will be included in the new DataSet, even if they have the same values.
     *
     * @param other The DataSet to merge with.
     * @param columnNames The collection of column names to be included in the merge operation.
     * @return A new DataSet that is the result of the merge operation.
     * @throws IllegalArgumentException if the other DataSet is {@code null} or if the <i>columnNames</i> collection is {@code null}.
     */
    DataSet merge(DataSet other, Collection<String> columnNames) throws IllegalArgumentException;

    /**
     * Merges this DataSet with another DataSet, starting from the specified row index and ending at the specified row index.
     * The merge operation combines rows from both DataSets into a new DataSet.
     * The rows from both DataSets will be included in the new DataSet, even if they have the same values.
     *
     * @param other The DataSet to merge with.
     * @param fromRowIndex The starting index of the row range to be included in the merge operation.
     * @param toRowIndex The ending index of the row range to be included in the merge operation.
     * @return A new DataSet that is the result of the merge operation.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range.
     * @throws IllegalArgumentException if the other DataSet is {@code null}.
     */
    DataSet merge(DataSet other, int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Merges this DataSet with another DataSet using the specified columns, starting from the specified row index and ending at the specified row index.
     * The merge operation combines rows from both DataSets into a new DataSet.
     * Only the columns specified in the <i>columnNames</i> parameter will be included in the new DataSet.
     * If there are columns in the <i>columnNames</i> that either not in this DataSet or the other DataSet, they will still be added to the new DataSet.
     * The rows from both DataSets will be included in the new DataSet, even if they have the same values.
     *
     * @param other The DataSet to merge with.
     * @param fromRowIndex The starting index of the row range to be included in the merge operation.
     * @param toRowIndex The ending index of the row range to be included in the merge operation.
     * @param columnNames The collection of column names to be included in the merge operation.
     * @return A new DataSet that is the result of the merge operation.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range.
     * @throws IllegalArgumentException if the other DataSet is {@code null} or if the <i>columnNames</i> collection is {@code null}.
     */
    DataSet merge(DataSet other, int fromRowIndex, int toRowIndex, Collection<String> columnNames) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Merges this DataSet with a collection of other DataSets.
     * The merge operation combines rows from all DataSets into a new DataSet.
     * All columns from all DataSets will be included in the new DataSet.
     * If there are columns that are not present in some DataSets, the corresponding values in the new DataSet will be {@code null}.
     * The rows from all DataSets will be included in the new DataSet, even if they have the same values.
     *
     * @param others The collection of DataSets to merge with.
     * @return A new DataSet that is the result of the merge operation, or a copy of this DataSet if the <i>others</i> collection is {@code null} or empty.
     */
    DataSet merge(final Collection<? extends DataSet> others);

    /**
     * Merges this DataSet with a collection of other DataSets.
     * The merge operation combines rows from all DataSets into a new DataSet.
     * All columns from all DataSets will be included in the new DataSet.
     * If there are columns that are not present in some DataSets, the corresponding values in the new DataSet will be {@code null}.
     * The rows from all DataSets will be included in the new DataSet, even if they have the same values.
     * If <i>requiresSameColumns</i> is {@code true}, all DataSets must have the same columns, otherwise an IllegalArgumentException will be thrown.
     *
     * @param others The collection of DataSets to merge with.
     * @param requiresSameColumns A boolean that indicates whether all DataSets should have the same columns.
     * @return A new DataSet that is the result of the merge operation, or a copy of this DataSet if the <i>others</i> collection is {@code null} or empty.
     * @throws IllegalArgumentException if <i>requiresSameColumns</i> is {@code true} and the DataSets do not have the same columns.
     */
    DataSet merge(final Collection<? extends DataSet> others, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a cartesian product operation with this DataSet and another DataSet.
     * The cartesian product operation combines each row from this DataSet with each row from the other DataSet.
     * The resulting DataSet will have the combined columns of both DataSets.
     * If there are columns that are not present in one of the DataSets, the corresponding values in the new DataSet will be {@code null}.
     *
     * @param other The DataSet to perform the cartesian product with.
     * @return A new DataSet that is the result of the cartesian product operation.
     * @throws IllegalArgumentException if the <i>other</i> DataSet is {@code null}.
     */
    DataSet cartesianProduct(DataSet other) throws IllegalArgumentException;

    /**
     * Splits this DataSet into multiple DataSets, each containing a maximum of <i>chunkSize</i> rows.
     * The split operation divides the DataSet into smaller DataSets, each of which contains <i>chunkSize</i> rows, except possibly for the last one, which may be smaller.
     * The resulting DataSets will have the same columns as the original DataSet.
     * The rows in the resulting DataSets will be in the same order as in the original DataSet.
     *
     * @param chunkSize The maximum number of rows each split DataSet should contain.
     * @return A Stream of DataSets, each containing <i>chunkSize</i> rows from the original DataSet, or an empty Stream if this DataSet is empty.
     * @throws IllegalArgumentException if <i>chunkSize</i> is less than or equal to 0.
     */
    Stream<DataSet> split(int chunkSize) throws IllegalArgumentException;

    /**
     * Splits this DataSet into multiple DataSets, each containing a maximum of <i>chunkSize</i> rows.
     * The split operation divides the DataSet into smaller DataSets, each of which contains <i>chunkSize</i> rows, except possibly for the last one, which may be smaller.
     * The resulting DataSets will have the same columns as specified in the <i>columnNames</i> collection.
     * The rows in the resulting DataSets will be in the same order as in the original DataSet.
     *
     * @param chunkSize The maximum number of rows each split DataSet should contain.
     * @param columnNames The collection of column names to be included in the split DataSets.
     * @return A Stream of DataSets, each containing <i>chunkSize</i> rows from the original DataSet, or an empty Stream if this DataSet is empty.
     * @throws IllegalArgumentException if <i>chunkSize</i> is less than or equal to 0.
     */
    Stream<DataSet> split(int chunkSize, Collection<String> columnNames) throws IllegalArgumentException;

    /**
     * Splits this DataSet into multiple DataSets, each containing a maximum of <i>chunkSize</i> rows.
     * The split operation divides the DataSet into smaller DataSets, each of which contains <i>chunkSize</i> rows, except possibly for the last one, which may be smaller.
     * The resulting DataSets will have the same columns as the original DataSet.
     * The rows in the resulting DataSets will be in the same order as in the original DataSet.
     *
     * @param chunkSize The maximum number of rows each split DataSet should contain.
     * @return A List of DataSets, each containing <i>chunkSize</i> rows from the original DataSet, or an empty List if this DataSet is empty.
     * @throws IllegalArgumentException if <i>chunkSize</i> is less than or equal to 0.
     */
    List<DataSet> splitToList(int chunkSize) throws IllegalArgumentException;

    /**
     * Splits this DataSet into multiple DataSets, each containing a maximum of <i>chunkSize</i> rows.
     * The split operation divides the DataSet into smaller DataSets, each of which contains <i>chunkSize</i> rows, except possibly for the last one, which may be smaller.
     * The resulting DataSets will have the same columns as specified in the <i>columnNames</i> collection.
     * The rows in the resulting DataSets will be in the same order as in the original DataSet.
     *
     * @param chunkSize The maximum number of rows each split DataSet should contain.
     * @param columnNames The collection of column names to be included in the split DataSets.
     * @return A List of DataSets, each containing <i>chunkSize</i> rows from the original DataSet, or an empty List if this DataSet is empty.
     * @throws IllegalArgumentException if <i>chunkSize</i> is less than or equal to 0.
     */
    List<DataSet> splitToList(int chunkSize, Collection<String> columnNames) throws IllegalArgumentException;

    /**
     * Returns an immutable slice of this DataSet from the specified <i>fromRowIndex</i> to <i>toRowIndex</i>.
     * The slice operation creates a new DataSet that includes rows from the original DataSet starting from <i>fromRowIndex</i> and ending at <i>toRowIndex</i>.
     * The resulting DataSet will have the same columns as the original DataSet.
     * The rows in the resulting DataSet will be in the same order as in the original DataSet.
     *
     * @param fromRowIndex The starting index of the slice, inclusive.
     * @param toRowIndex The ending index of the slice, exclusive.
     * @return A new DataSet containing the rows from <i>fromRowIndex</i> to <i>toRowIndex</i> from the original DataSet.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range.
     * @see List#subList(int, int).
     */
    DataSet slice(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Returns an immutable slice of this DataSet that includes only the columns specified in the <i>columnNames</i> collection from the original DataSet.
     * The resulting DataSet will have the same rows as the original DataSet, but only the columns specified in the <i>columnNames</i> collection.
     * The rows in the resulting DataSet will be in the same order as in the original DataSet.
     *
     * @param columnNames The collection of column names to be included in the sliced DataSet.
     * @return A new DataSet containing the same rows as the original DataSet, but only the columns specified in the <i>columnNames</i> collection.
     * @throws IllegalArgumentException if the <i>columnNames</i> collection is {@code null} or if any of the column names in the collection do not exist in the original DataSet.
     * @see List#subList(int, int).
     */
    DataSet slice(Collection<String> columnNames) throws IllegalArgumentException;

    /**
     * Returns an immutable slice of this DataSet from the specified <i>fromRowIndex</i> to <i>toRowIndex</i> and only includes the columns specified in the <i>columnNames</i> collection.
     * The resulting DataSet will have the same rows as the original DataSet, but only the columns specified in the <i>columnNames</i> collection.
     * The rows in the resulting DataSet will be in the same order as in the original DataSet.
     *
     * @param fromRowIndex The starting index of the slice, inclusive.
     * @param toRowIndex The ending index of the slice, exclusive.
     * @param columnNames The collection of column names to be included in the sliced DataSet.
     * @return A new DataSet containing the rows from <i>fromRowIndex</i> to <i>toRowIndex</i> from the original DataSet, but only the columns specified in the <i>columnNames</i> collection.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range.
     * @throws IllegalArgumentException if the <i>columnNames</i> collection is {@code null} or if any of the column names in the collection do not exist in the original DataSet.
     */
    DataSet slice(int fromRowIndex, int toRowIndex, Collection<String> columnNames) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a new DataSet that is a copy of the current DataSet.
     * The rows and columns in the resulting DataSet will be in the same order as in the original DataSet.
     * The frozen status of the copy will always be {@code false}, even the original {@code DataSet} is frozen.
     *
     * @return A new DataSet that is a copy of the current DataSet.
     */
    DataSet copy();

    /**
     * Creates a new DataSet that is a copy of the current DataSet from the specified <i>fromRowIndex</i> to <i>toRowIndex</i>.
     * The rows and columns in the resulting DataSet will be in the same order as in the original DataSet.
     * The frozen status of the copy will always be {@code false}, even the original {@code DataSet} is frozen.
     *
     * @param fromRowIndex The starting index of the copy, inclusive.
     * @param toRowIndex The ending index of the copy, exclusive.
     * @return A new DataSet that is a copy of the current DataSet from <i>fromRowIndex</i> to <i>toRowIndex</i>.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range.
     */
    DataSet copy(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Creates a new DataSet that is a copy of the current DataSet with only the columns specified in the <i>columnNames</i> collection.
     * The rows in the resulting DataSet will be in the same order as in the original DataSet.
     * The frozen status of the copy will always be {@code false}, even the original {@code DataSet} is frozen.
     *
     * @param columnNames The collection of column names to be included in the copy.
     * @return A new DataSet that is a copy of the current DataSet with only the columns specified in the <i>columnNames</i> collection.
     * @throws IllegalArgumentException if the <i>columnNames</i> collection is {@code null} or if any of the column names in the collection do not exist in the original DataSet.
     */
    DataSet copy(Collection<String> columnNames);

    /**
     * Creates a new DataSet that is a copy of the current DataSet from the specified <i>fromRowIndex</i> to <i>toRowIndex</i> with only the columns specified in the <i>columnNames</i> collection.
     * The rows in the resulting DataSet will be in the same order as in the original DataSet.
     * The frozen status of the copy will always be {@code false}, even the original {@code DataSet} is frozen.
     *
     * @param fromRowIndex The starting index of the copy, inclusive.
     * @param toRowIndex The ending index of the copy, exclusive.
     * @param columnNames The collection of column names to be included in the copy.
     * @return A new DataSet that is a copy of the current DataSet from <i>fromRowIndex</i> to <i>toRowIndex</i> with only the columns specified in the <i>columnNames</i> collection.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range.
     * @throws IllegalArgumentException if the <i>columnNames</i> collection is {@code null} or if any of the column names in the collection do not exist in the original DataSet.
     */
    DataSet copy(int fromRowIndex, int toRowIndex, Collection<String> columnNames);

    /**
     * Creates a deep copy of the current DataSet by performing Serialization/Deserialization.
     * This method ensures that the returned DataSet is a completely separate copy of the original DataSet, with no shared references.
     *
     * @return A new DataSet that is a deep copy of the current DataSet.
     */
    @Beta
    DataSet clone(); //NOSONAR

    /**
     * Creates a deep copy of the current DataSet by performing Serialization/Deserialization.
     * This method ensures that the returned DataSet is a completely separate copy of the original DataSet, with no shared references.
     *
     * @param freeze A boolean value that indicates whether the returned DataSet should be frozen.
     * @return A new DataSet that is a deep copy of the current DataSet.
     */
    @Beta
    DataSet clone(boolean freeze);

    /**
     * Creates a BiIterator over the elements in the specified columns of the DataSet.
     * The BiIterator will iterate over pairs of elements, where each pair consists of an element from columnNameA and an element from columnNameB.
     *
     * @param <A> The type of the elements in the column specified by columnNameA.
     * @param <B> The type of the elements in the column specified by columnNameB.
     * @param columnNameA The name of the first column to iterate over.
     * @param columnNameB The name of the second column to iterate over.
     * @return A BiIterator over pairs of elements from the specified columns.
     * @throws IllegalArgumentException if the specified any of column names are not found in the DataSet.
     */
    <A, B> BiIterator<A, B> iterator(String columnNameA, String columnNameB) throws IllegalArgumentException;

    /**
     * Creates a BiIterator over the elements in the specified columns of the DataSet.
     * The BiIterator will iterate over pairs of elements, where each pair consists of an element from columnNameA and an element from columnNameB.
     * The iteration starts from the row specified by fromRowIndex and ends at the row specified by toRowIndex.
     *
     * @param <A> The type of the elements in the column specified by columnNameA.
     * @param <B> The type of the elements in the column specified by columnNameB.
     * @param fromRowIndex The starting row index for the iteration.
     * @param toRowIndex The ending row index for the iteration.
     * @param columnNameA The name of the first column to iterate over.
     * @param columnNameB The name of the second column to iterate over.
     * @return A BiIterator over pairs of elements from the specified columns.
     * @throws IndexOutOfBoundsException if either fromRowIndex or toRowIndex is out of the DataSet's row bounds.
     * @throws IllegalArgumentException if the specified any of column names are not found in the DataSet.
     */
    <A, B> BiIterator<A, B> iterator(int fromRowIndex, int toRowIndex, String columnNameA, String columnNameB)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a TriIterator over the elements in the specified columns of the DataSet.
     * The TriIterator will iterate over triplets of elements, where each triplet consists of an element from columnNameA, an element from columnNameB, and an element from columnNameC.
     *
     * @param <A> The type of the elements in the column specified by columnNameA.
     * @param <B> The type of the elements in the column specified by columnNameB.
     * @param <C> The type of the elements in the column specified by columnNameC.
     * @param columnNameA The name of the first column to iterate over.
     * @param columnNameB The name of the second column to iterate over.
     * @param columnNameC The name of the third column to iterate over.
     * @return A TriIterator over triplets of elements from the specified columns.
     * @throws IllegalArgumentException if the specified any of column names are not found in the DataSet.
     */
    <A, B, C> TriIterator<A, B, C> iterator(String columnNameA, String columnNameB, String columnNameC) throws IllegalArgumentException;

    /**
     * Creates a TriIterator over the elements in the specified columns of the DataSet.
     * The TriIterator will iterate over triplets of elements, where each triplet consists of an element from columnNameA, an element from columnNameB, and an element from columnNameC.
     * The iteration starts from the row specified by fromRowIndex and ends at the row specified by toRowIndex.
     *
     * @param <A> The type of the elements in the column specified by columnNameA.
     * @param <B> The type of the elements in the column specified by columnNameB.
     * @param <C> The type of the elements in the column specified by columnNameC.
     * @param fromRowIndex The starting row index for the iteration.
     * @param toRowIndex The ending row index for the iteration.
     * @param columnNameA The name of the first column to iterate over.
     * @param columnNameB The name of the second column to iterate over.
     * @param columnNameC The name of the third column to iterate over.
     * @return A TriIterator over triplets of elements from the specified columns.
     * @throws IndexOutOfBoundsException if either fromRowIndex or toRowIndex is out of the DataSet's row bounds.
     * @throws IllegalArgumentException if the specified any of column names are not found in the DataSet.s
     */
    <A, B, C> TriIterator<A, B, C> iterator(int fromRowIndex, int toRowIndex, String columnNameA, String columnNameB, String columnNameC)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Paginated<DataSet> DataSet from the current DataSet.
     * The Paginated<DataSet> object will contain pages of DataSets, where each page has a maximum size specified by the pageSize parameter.
     *
     * @param pageSize The maximum number of rows each page can contain.
     * @return A Paginated<DataSet> object containing pages of DataSets.
     * @throws IllegalArgumentException if pageSize is less than or equal to 0.
     */
    Paginated<DataSet> paginate(int pageSize);

    /**
     * Creates a Paginated<DataSet> object from the current DataSet.
     * The Paginated<DataSet> object will contain pages of DataSets, where each page has a maximum size specified by the pageSize parameter.
     * Only the columns specified by the columnNames collection will be included in the paginated DataSet.
     *
     * @param columnNames The collection of column names to be included in the paginated DataSet.
     * @param pageSize The maximum number of rows each page can contain.
     * @return A Paginated<DataSet> object containing pages of DataSets.
     * @throws IllegalArgumentException if the specified column names are not found in the DataSet or {@code columnNames} is empty or pageSize is less than or equal to 0.
     */
    Paginated<DataSet> paginate(Collection<String> columnNames, int pageSize);

    /**
     * Returns a Stream with values from the specified column.
     * The values are read from the DataSet in the order they appear.
     *
     * @param <T> The type of the specified column.
     * @param columnName The name of the column in the DataSet to create the Stream from.
     * @return A Stream containing all values from the specified column in the DataSet.
     * @throws IllegalArgumentException if the specified column name is not found in the DataSet.
     */
    <T> Stream<T> stream(String columnName) throws IllegalArgumentException;

    /**
     * Returns a Stream with values from the specified column.
     * The Stream will contain all values from the specified column, starting from the row index specified by <i>fromRowIndex</i> and ending at the row index specified by <i>toRowIndex</i>.
     * The values are read from the DataSet in the order they appear.
     *
     * @param <T> The type of the elements in the Stream.
     * @param fromRowIndex The starting row index for the Stream.
     * @param toRowIndex The ending row index for the Stream.
     * @param columnName The name of the column in the DataSet to create the Stream from.
     * @return A Stream containing all values from the specified column in the DataSet.
     * @throws IndexOutOfBoundsException if the specified row indexes are out of the DataSet's range.
     * @throws IllegalArgumentException if the specified column name is not found in the DataSet.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, String columnName) throws IndexOutOfBoundsException, IllegalArgumentException;

    // The method stream(Collection<String>, Function<? super NoCachingNoUpdating.DisposableObjArray,Object[]>) is ambiguous for the type
    //    /**
    //     *
    //     * @param <T>
    //     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    //     * @return
    //     */
    //    <T> Stream<T> stream(Function<? super DisposableObjArray, ? extends T> rowMapper);
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param fromRowIndex
    //     * @param toRowIndex
    //     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    //     * @return
    //     */
    //    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Function<? super DisposableObjArray, ? extends T> rowMapper);
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    //     * @return
    //     */
    //    <T> Stream<T> stream(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends T> rowMapper);
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param fromRowIndex
    //     * @param toRowIndex
    //     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    //     * @return
    //     */
    //    <T> Stream<T> stream(Collection<String> columnNames, int fromRowIndex, int toRowIndex, Function<? super DisposableObjArray, ? extends T> rowMapper);

    /**
     * Creates a Stream of objects of type {@code T} converted from rows in the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowType.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of objects of type T, created from rows in the DataSet.
     * @throws IllegalArgumentException if the specified {@code beanClass} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * The subset of rows is determined by the provided fromRowIndex and toRowIndex.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row subset.
     * @param toRowIndex The ending index of the row subset.
     * @param rowType The class of the objects in the resulting Stream.
     * @return A Stream of objects of type T, created from the subset of rows in the DataSet.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range.
     * @throws IllegalArgumentException if the specified {@code beanClass} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * Only the columns specified in the {@code columnNames} collection will be included to {@code rowType}.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param columnNames The collection of column names to be included to the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of objects of type T, created from rows in the DataSet.
     * @throws IllegalArgumentException if the specified {@code beanClass} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(Collection<String> columnNames, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * Only the columns specified in the {@code columnNames} collection will be included to {@code rowType}.
     * The subset of rows is determined by the provided fromRowIndex and toRowIndex.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row subset.
     * @param toRowIndex The ending index of the row subset.
     * @param columnNames The collection of column names to be included to the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of objects of type T, created from the subset of rows in the DataSet.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range.
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Class<? extends T> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowSupplier function.
     * The rowSupplier function is responsible for creating new instances of {@code T} for each row in the DataSet.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param rowSupplier A function that creates a new instance of {@code T} for each row in the DataSet.
     * @return A Stream of objects of type T, created from the DataSet.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowSupplier function.
     * The rowSupplier function is responsible for creating new instances of {@code T} for each row in the DataSet.
     * The subset of rows is determined by the provided fromRowIndex and toRowIndex.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row subset.
     * @param toRowIndex The ending index of the row subset.
     * @param rowSupplier A function that creates a new instance of {@code T} for each row in the DataSet.
     * @return A Stream of objects of type T, created from the subset of rows in the DataSet.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowSupplier function.
     * Only the columns specified in the {@code columnNames} collection will be included to the instances created by rowSupplier.
     * The rowSupplier function is responsible for creating new instances of {@code T} for each row in the DataSet.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param columnNames The collection of column names to be included to the instances created by rowSupplier.
     * @param rowSupplier A function that creates a new instance of {@code T} for each row in the DataSet.
     * @return A Stream of objects of type T, created from the DataSet.
     * @throws IllegalArgumentException if the columnNames are not found in the DataSet or {@code columnNames} is empty, or if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowSupplier function.
     * The rowSupplier function is responsible for creating new instances of {@code T} for each row in the DataSet.
     * Only the columns specified in the {@code columnNames} collection will be included to the instances created by rowSupplier.
     * The subset of rows is determined by the provided fromRowIndex and toRowIndex.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row subset.
     * @param toRowIndex The ending index of the row subset.
     * @param columnNames The collection of column names to be included to the instances created by rowSupplier.
     * @param rowSupplier A function that creates a new instance of {@code T} for each row in the DataSet.
     * @return A Stream of objects of type T, created from the subset of rows in the DataSet.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range.
     * @throws IllegalArgumentException if the columnNames are not found in the DataSet or {@code columnNames} is empty, or if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, IntFunction<? extends T> rowSupplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * The mapping between the DataSet's columns and the fields of the {@code rowType} is determined by the provided prefixAndFieldNameMap.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param prefixAndFieldNameMap The map of prefixes and field names to be used for mapping DataSet's columns to the fields of the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Bean class.
     * @return A Stream of objects of type T, created from the DataSet.
     * @throws IllegalArgumentException if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code rowType} is not a supported type - Bean class.
     */
    <T> Stream<T> stream(Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * The mapping between the DataSet's columns and the fields of the {@code rowType} is determined by the provided prefixAndFieldNameMap.
     * The subset of rows is determined by the provided fromRowIndex and toRowIndex.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row subset.
     * @param toRowIndex The ending index of the row subset.
     * @param prefixAndFieldNameMap The map of prefixes and field names to be used for mapping DataSet's columns to the fields of the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Bean class.
     * @return A Stream of objects of type T, created from the subset of rows in the DataSet.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range.
     * @throws IllegalArgumentException if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code rowType} is not a supported type - Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} converted from rows in the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * The mapping between the DataSet's columns and the fields of the {@code rowType} is determined by the provided prefixAndFieldNameMap.
     * Only the columns specified in the {@code columnNames} collection will be included in the {@code rowType}.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param columnNames The collection of column names to be included in the {@code rowType}.
     * @param prefixAndFieldNameMap The map of prefixes and field names to be used for mapping DataSet's columns to the fields of the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Bean class.
     * @return A Stream of objects of type T
     * @throws IllegalArgumentException if the columnNames are not found in the DataSet or {@code columnNames} is empty, or if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> Stream<T> stream(Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} converted from rows in the DataSet.
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * The mapping between the DataSet's columns and the fields of the {@code rowType} is determined by the provided prefixAndFieldNameMap.
     * Only the columns specified in the {@code columnNames} collection will be included in the {@code rowType}.
     * The Stream is created for rows in the range from fromRowIndex (inclusive) to toRowIndex (exclusive).
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row, inclusive.
     * @param toRowIndex The ending index of the row, exclusive.
     * @param columnNames The collection of column names to be included in the {@code rowType}.
     * @param prefixAndFieldNameMap The map of prefixes and field names to be used for mapping DataSet's columns to the fields of the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream.
     * @return A Stream of objects of type T
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range
     * @throws IllegalArgumentException if the columnNames are not found in the DataSet or {@code columnNames} is empty, or if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} by applying the provided rowMapper function to each row in the DataSet.
     * The rowMapper function takes two arguments: the index of the row and a DisposableObjArray representing the row itself.
     * The DisposableObjArray contains the values of the row.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param rowMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the DataSet, and the DisposableObjArray represents the row itself.
     * @return A Stream of objects of type T, created by applying the rowMapper function to each row in the DataSet.
     */
    <T> Stream<T> stream(IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper);

    /**
     * Creates a Stream of objects of type {@code T} by applying the provided rowMapper function to each row in the DataSet.
     * The rowMapper function takes two arguments: the index of the row and a DisposableObjArray representing the row itself.
     * The DisposableObjArray contains the values of the row.
     * The Stream is created for rows in the range from fromRowIndex (inclusive) to toRowIndex (exclusive).
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row, inclusive.
     * @param toRowIndex The ending index of the row, exclusive.
     * @param rowMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the DataSet, and the DisposableObjArray represents the row itself.
     * @return A Stream of objects of type T, created by applying the rowMapper function to each row in the DataSet.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} by applying the provided rowMapper function to each row in the DataSet.
     * The rowMapper function takes two arguments: the index of the row and a DisposableObjArray representing the row itself.
     * The DisposableObjArray contains the values of the specified columns for that row.
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param columnNames A collection of column names to be included in the DisposableObjArray passed to the rowMapper function.
     * @param rowMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the DataSet, and the DisposableObjArray represents the row itself.
     * @return A Stream of objects of type T, created by applying the rowMapper function to each row in the DataSet.
     * @throws IllegalArgumentException if the columnNames are not found in the DataSet or {@code columnNames} is empty
     */
    <T> Stream<T> stream(Collection<String> columnNames, IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} by applying the provided rowMapper function to each row in the DataSet.
     * The rowMapper function takes two arguments: the index of the row and a DisposableObjArray representing the row itself.
     * The DisposableObjArray contains the values of the specified columns for that row.
     * The Stream is created for rows in the range from fromRowIndex (inclusive) to toRowIndex (exclusive).
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row, inclusive.
     * @param toRowIndex The ending index of the row, exclusive.
     * @param columnNames A collection of column names to be included in the DisposableObjArray passed to the rowMapper function.
     * @param rowMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the DataSet, and the DisposableObjArray represents the row itself.
     * @return A Stream of objects of type T, created by applying the rowMapper function to each row in the DataSet.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range
     * @throws IllegalArgumentException if the columnNames are not found in the DataSet or {@code columnNames} is empty
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of type T from the DataSet using the specified column names and a row mapper function.
     * The row mapper function is used to transform the values of the two columns into an instance of type T.
     *
     * @param <T> the type of the elements in the resulting Stream
     * @param columnNames a Tuple2 containing the names of the two columns to be used
     * @param rowMapper a BiFunction to transform the values of the two columns into an instance of type T
     * @return a Stream of type T
     * @throws IllegalArgumentException if the columnNames are not found in the DataSet
     */
    <T> Stream<T> stream(Tuple2<String, String> columnNames, BiFunction<?, ?, ? extends T> rowMapper) throws IllegalArgumentException;

    /**
     * Creates a Stream of type T from the DataSet using the specified column names and a row mapper function.
     * The Stream will contain all values from the specified columns, starting from the row index specified by <i>fromRowIndex</i> and ending at the row index specified by <i>toRowIndex</i>.
     * The row mapper function is used to transform the values from the specified columns into an object of type T.
     *
     * @param <T> the type of the elements in the resulting Stream
     * @param fromRowIndex the starting index of the row, inclusive
     * @param toRowIndex the ending index of the row, exclusive
     * @param columnNames a Tuple2 containing the names of the two columns to be used
     * @param rowMapper a BiFunction to transform the values of the two columns into an instance of type T
     * @return a Stream of type T
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range
     * @throws IllegalArgumentException if the columnNames are not found in the DataSet
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, BiFunction<?, ?, ? extends T> rowMapper)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of type T from the DataSet using the specified column names and a row mapper function.
     * The Stream will contain all values from the specified columns.
     * The row mapper function is used to transform the values from the specified columns into an object of type T.
     *
     * @param <T> The type of the elements in the Stream.
     * @param columnNames The names of the columns in the DataSet to create the Stream from.
     * @param rowMapper The function to transform the values from the specified columns into an object of type T.
     * @return A Stream containing all values from the specified columns in the DataSet, transformed by the row mapper function.
     * @throws IllegalArgumentException if any of the specified column names are not found in the DataSet.
     */
    <T> Stream<T> stream(Tuple3<String, String, String> columnNames, TriFunction<?, ?, ?, ? extends T> rowMapper) throws IllegalArgumentException;

    /**
     * Creates a Stream of type T from the DataSet using the specified column names and a row mapper function.
     * The Stream will contain all values from the specified columns, starting from the row index specified by <i>fromRowIndex</i> and ending at the row index specified by <i>toRowIndex</i>.
     * The row mapper function is used to transform the values from the specified columns into an object of type T.
     *
     * @param <T> The type of the elements in the Stream.
     * @param fromRowIndex The starting row index for the Stream.
     * @param toRowIndex The ending row index for the Stream.
     * @param columnNames The names of the columns in the DataSet to create the Stream from.
     * @param rowMapper The function to transform the values from the specified columns into an object of type T.
     * @return A Stream containing all values from the specified columns in the DataSet, transformed by the row mapper function.
     * @throws IndexOutOfBoundsException if the specified row indexes are out of the DataSet's range.
     * @throws IllegalArgumentException if any of the specified column names are not found in the DataSet.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, TriFunction<?, ?, ?, ? extends T> rowMapper)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Applies the provided function to this DataSet and returns the result.
     * This method is useful for performing complex operations on the DataSet that are not covered by the existing methods.
     *
     * @param <R> The type of the result.
     * @param <E> The type of the exception that the function may throw.
     * @param func The function to apply to the DataSet. This function should take a DataSet as input and return a result of type R.
     * @return The result of applying the provided function to the DataSet.
     * @throws E if the provided function throws an exception.
     */
    <R, E extends Exception> R apply(Throwables.Function<? super DataSet, ? extends R, E> func) throws E;

    /**
     * Applies the provided function to this DataSet if it is not empty and returns the result wrapped in an Optional.
     * This method is useful for performing complex operations on the DataSet that are not covered by the existing methods.
     * If the DataSet is empty, it returns an empty Optional.
     *
     * @param <R> The type of the result.
     * @param <E> The type of the exception that the function may throw.
     * @param func The function to apply to the DataSet. This function should take a DataSet as input and return a result of type R.
     * @return An Optional containing the result of applying the provided function to the DataSet, or an empty Optional if the DataSet is empty.
     * @throws E if the provided function throws an exception.
     */
    <R, E extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super DataSet, ? extends R, E> func) throws E;

    /**
     * Performs the provided action on this DataSet.
     * This method is useful for performing operations on the DataSet that do not return a result.
     *
     * @param <E> The type of the exception that the action may throw.
     * @param action The action to be performed on the DataSet. This action should take a DataSet as input.
     * @throws E if the provided action throws an exception.
     */
    <E extends Exception> void accept(Throwables.Consumer<? super DataSet, E> action) throws E;

    /**
     * Performs the provided action on this DataSet if it is not empty.
     * This method is useful for performing operations on the DataSet that do not return a result.
     * If the DataSet is empty, it does nothing and returns an instance of OrElse.
     *
     * @param <E> The type of the exception that the action may throw.
     * @param action The action to be performed on the DataSet. This action should take a DataSet as input.
     * @return An instance of OrElse, which can be used to perform an alternative action if the DataSet is empty.
     * @throws E if the provided action throws an exception.
     */
    <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super DataSet, E> action) throws E;

    /**
     * Freezes the DataSet to prevent further modification.
     * This method is useful when you want to ensure the DataSet remains constant after a certain point in your program.
     */
    void freeze();

    /**
     * Checks if the DataSet is frozen.
     *
     * @return {@code true} if the DataSet is frozen and cannot be modified, {@code false} otherwise.
     */
    boolean isFrozen();

    /**
     * Clears the DataSet.
     * This method removes all data from the DataSet, leaving it empty.
     */
    void clear();

    /**
     * Checks if the DataSet is empty.
     *
     * @return {@code true} if the DataSet is empty, {@code false} otherwise.
     */
    boolean isEmpty();

    /**
     * Trims the size of the DataSet to its current size.
     * This method can be used to minimize the memory footprint of the DataSet.
     */
    void trimToSize();

    /**
     * Returns the number of rows in the DataSet.
     *
     * @return The number of rows in the DataSet.
     */
    int size();

    /**
     * Retrieves the properties of the DataSet as a Map.
     * The keys of the Map are the property names and the values are the property values.
     *
     * @return A Map containing the properties of the DataSet.
     */
    @Beta
    Map<String, Object> properties();

    /**
     * Retrieves the names of the columns in the DataSet as a Stream.
     *
     * @return A Stream containing the names of the columns in the DataSet.
     */
    @Beta
    Stream<String> columnNames();

    /**
     * Retrieves the data of the DataSet as a Stream of ImmutableList.
     * Each ImmutableList represents a column of data in the DataSet.
     *
     * @return A Stream containing ImmutableList where each list represents a column of data in the DataSet.
     */
    Stream<ImmutableList<Object>> columns();

    /**
     * Retrieves the data of the DataSet as a Map.
     * Each entry in the Map represents a column in the DataSet, where the key is the column name and the value is an ImmutableList of objects in that column.
     *
     * @return A Map where each entry represents a column in the DataSet.
     */
    Map<String, ImmutableList<Object>> columnMap();

    // DataSetBuilder builder();

    /**
     * Prints the content of the DataSet to the standard output.
     */
    void println();

    /**
     * Prints a portion of the DataSet to the standard output.
     *
     * @param fromRowIndex the starting index of the row, inclusive
     * @param toRowIndex the ending index of the row, exclusive
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range
     */
    void println(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Prints a portion of the DataSet to the standard output.
     *
     * @param fromRowIndex the starting index of the row, inclusive
     * @param toRowIndex the ending index of the row, exclusive
     * @param columnNames the collection of column names to be printed
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range
     */
    void println(int fromRowIndex, int toRowIndex, Collection<String> columnNames) throws IndexOutOfBoundsException;

    /**
     * Prints the DataSet to the provided Writer.
     *
     * @param outputWriter the Writer where the DataSet will be printed
     * @throws UncheckedIOException if an I/O error occurs
     */
    void println(Writer outputWriter) throws UncheckedIOException;

    /**
     * Prints a portion of the DataSet to the provided Writer.
     *
     * @param fromRowIndex the starting index of the row, inclusive
     * @param toRowIndex the ending index of the row, exclusive
     * @param columnNames the collection of column names to be printed
     * @param outputWriter the Writer where the DataSet will be printed
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the DataSet's range
     * @throws UncheckedIOException if an I/O error occurs
     */
    void println(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Writer outputWriter) throws IndexOutOfBoundsException, UncheckedIOException;
}
