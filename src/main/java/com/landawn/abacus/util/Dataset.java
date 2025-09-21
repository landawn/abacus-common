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
 * The Dataset interface represents a data structure that holds a collection of data in a tabular format.
 * It provides a variety of methods for manipulating and accessing the data, such as sorting, filtering, joining, and grouping.
 * It also supports operations like union, intersection, and difference between two Datasets.
 * The data in a Dataset is organized into rows and columns, similar to a table in a relational database.
 * Each column in a Dataset has a name(case-sensitive), and the data within a column is of a specific type.
 * <br />
 * @see com.landawn.abacus.util.Builder.DatasetBuilder
 * @see com.landawn.abacus.util.Sheet
 * @see com.landawn.abacus.util.CSVUtil
 * @see com.landawn.abacus.util.IntFunctions
 * @see com.landawn.abacus.util.Clazz
 * @see com.landawn.abacus.util.N#newEmptyDataset()
 * @see com.landawn.abacus.util.N#newEmptyDataset(Collection)
 * @see com.landawn.abacus.util.N#newDataset(Map)
 * @see com.landawn.abacus.util.N#newDataset(Collection)
 * @see com.landawn.abacus.util.N#newDataset(Collection, Collection)
 * @see com.landawn.abacus.util.N#newDataset(String, String, Map)
 */
public sealed interface Dataset permits RowDataset {

    /**
     * Returns an immutable empty {@code Dataset}.
     * <br />
     * This method provides a convenient way to obtain an empty Dataset instance for initialization, 
     * comparison purposes, or as a default value. The returned Dataset contains no rows or columns 
     * and is immutable, meaning it cannot be modified.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset emptyDataset = Dataset.empty();
     * boolean isEmpty = emptyDataset.isEmpty(); // returns true
     * int size = emptyDataset.size(); // returns 0
     * 
     * // Can be used as a default value or for comparison
     * Dataset result = someCondition ? processedDataset : Dataset.empty();
     * }</pre>
     *
     * @return an immutable empty {@code Dataset} with no columns or rows
     * @see #isEmpty()
     * @see #size()
     */
    static Dataset empty() {
        return RowDataset.EMPTY_DATA_SET;
    }

    /**
     * Creates a new Dataset with the specified column names and rows.
     * <br />
     * The Dataset is a data structure that stores data in a tabular format, similar to a table in a database.
     * Each item in the <i>columnNames</i> collection represents a column in the Dataset.
     * The <i>rows</i> parameter is a 2D array where each subarray represents a row in the Dataset.
     * The order of elements in each row should correspond to the order of column names.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(
     *     Arrays.asList("id", "name", "age"),
     *     new Object[][] {{1, "Alice", 25}, {2, "Bob", 30}}
     * );
     * }</pre>
     *
     * @param columnNames A collection of strings representing the names of the columns in the Dataset.
     * @param rows A 2D array representing the data in the Dataset. Each subarray is a row.
     * @return A new Dataset with the specified column names and rows.
     * @throws IllegalArgumentException If the provided columnNames and rows do not align properly.
     * @see N#newDataset(Collection, Object[][])
     */
    static Dataset rows(final Collection<String> columnNames, final Object[][] rows) throws IllegalArgumentException {
        return N.newDataset(columnNames, rows);
    }

    /**
     * Creates a new Dataset with the specified column names and rows.
     * <br />
     * The Dataset is a data structure that stores data in a tabular format, similar to a table in a database.
     * Each item in the <i>columnNames</i> collection represents a column in the Dataset.
     * The <i>rows</i> parameter is a collection of collections where each sub-collection represents a row in the Dataset.
     * The order of elements in each row should correspond to the order of column names.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(
     *     Arrays.asList("id", "name", "age"),
     *     Arrays.asList(Arrays.asList(1, "Alice", 25), Arrays.asList(2, "Bob", 30))
     * );
     * }</pre>
     *
     * @param columnNames A collection of strings representing the names of the columns in the Dataset.
     * @param rows A collection of collections representing the data in the Dataset. Each sub-collection is a row which can be: Map/Bean/Array/List.
     * @return A new Dataset with the specified column names and rows.
     * @throws IllegalArgumentException If the provided columnNames and rows do not align properly.
     * @see N#newDataset(Collection, Collection)
     */
    static Dataset rows(final Collection<String> columnNames, final Collection<? extends Collection<?>> rows) throws IllegalArgumentException {
        return N.newDataset(columnNames, rows);
    }

    /**
     * Creates a new Dataset with the specified column names and columns.
     * <br />
     * The Dataset is a data structure that stores data in a tabular format, similar to a table in a database.
     * Each item in the <i>columnNames</i> collection represents a column in the Dataset.
     * The <i>columns</i> parameter is a 2D array where each subarray represents a column in the Dataset.
     * The order of elements in each column should correspond to the order of column names.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.columns(
     *     Arrays.asList("id", "name", "age"),
     *     new Object[][] {{1, 2}, {"Alice", "Bob"}, {25, 30}}
     * );
     * }</pre>
     *
     * @param columnNames A collection of strings representing the names of the columns in the Dataset.
     * @param columns A 2D array representing the data in the Dataset. Each subarray is a column.
     * @return A new Dataset with the specified column names and columns.
     * @throws IllegalArgumentException If the length of <i>columnNames</i> is not equal to the length of <i>columns</i> or the size of the sub-collection in <i>columns</i> is not equal.
     */
    static Dataset columns(final Collection<String> columnNames, final Object[][] columns) throws IllegalArgumentException {
        if (N.size(columnNames) != N.len(columns)) {
            throw new IllegalArgumentException("The length of 'columnNames'(" + N.size(columnNames)
                    + ") is not equal to the length of the sub-collections in 'columns'(" + N.len(columns) + ").");
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

        return new RowDataset(columnNameList, columnList);
    }

    /**
     * Creates a new Dataset with the specified column names and columns.
     * <br />
     * The Dataset is a data structure that stores data in a tabular format, similar to a table in a database.
     * Each item in the <i>columnNames</i> collection represents a column in the Dataset.
     * The <i>columns</i> parameter is a collection of collections where each sub-collection represents a column in the Dataset.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.columns(
     *     Arrays.asList("id", "name"),
     *     Arrays.asList(Arrays.asList(1, 2), Arrays.asList("Alice", "Bob"))
     * );
     * }</pre>
     *
     * @param columnNames A collection of strings representing the names of the columns in the Dataset.
     * @param columns A collection of collections representing the data in the Dataset. Each sub-collection is a column.
     * @return A new Dataset with the specified column names and columns.
     * @throws IllegalArgumentException If the length of <i>columnNames</i> is not equal to the length of <i>columns</i> or the size of the sub-collection in <i>columns</i> is not equal.
     */
    static Dataset columns(final Collection<String> columnNames, final Collection<? extends Collection<?>> columns) throws IllegalArgumentException {
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

        return new RowDataset(columnNameList, columnList);
    }

    //    /**
    //     * Creates a new Dataset with a single column.
    //     *
    //     * The Dataset is a data structure that stores data in a tabular format, similar to a table in a database.
    //     * The 'columnName' parameter represents the name of the column in the Dataset.
    //     * The 'column' parameter is a collection where each item represents a row in the Dataset.
    //     *
    //     * @param columnName A string representing the name of the column in the Dataset.
    //     * @param column A collection of objects representing the data in the Dataset. Each object is a row.
    //     * @return A new Dataset with the specified column name and column data.
    //     * @throws IllegalArgumentException If the provided columnName is empty.
    //     * @see #columns(Collection, Collection)
    //     * @see N#newDataset(String, Collection)
    //     */
    //    static Dataset singleColumn(final String columnName, final Collection<?> column) throws IllegalArgumentException {
    //        return N.newDataset(columnName, column);
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
     * Returns an immutable list of column names in this Dataset.
     * <br />
     * The order of the column names in the list reflects the order of the columns in the Dataset.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * ImmutableList<String> columns = dataset.columnNameList();
     * // columns contains ["id", "name", "age"]
     * }</pre>
     *
     * @return an ImmutableList of column names
     */
    ImmutableList<String> columnNameList();

    /**
     * Returns the number of columns in this Dataset.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * int count = dataset.columnCount(); // returns 3
     * }</pre>
     *
     * @return the count of columns
     */
    int columnCount();

    /**
     * Returns the column name at the specified index.
     * <br />
     * Column indices are zero-based.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset ds = Dataset.rows(Arrays.asList("id", "name"), data);
     * String name = ds.getColumnName(1); // returns "name"
     * }</pre>
     *
     * @param columnIndex the zero-based index of the column
     * @return the name of the column at the specified index
     * @throws IndexOutOfBoundsException if columnIndex is negative or >= columnCount()
     */
    String getColumnName(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Returns the index of the specified column in the Dataset.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * int index = dataset.getColumnIndex("name"); // returns 1
     * }</pre>
     *
     * @param columnName the name(case-sensitive) of the column for which the index is required.
     * @return the index of the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     */
    int getColumnIndex(String columnName) throws IllegalArgumentException;

    /**
     * Returns an array of column indexes corresponding to the provided column names.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * int[] indexes = dataset.getColumnIndexes(Arrays.asList("name", "age")); // returns [1, 2]
     * }</pre>
     *
     * @param columnNames the collection of column names(case-sensitive) for which indexes are required.
     * @return an array of integers representing the indexes of the specified columns.
     * @throws IllegalArgumentException if any of the provided column names does not exist in the Dataset.
     */
    int[] getColumnIndexes(Collection<String> columnNames) throws IllegalArgumentException;

    /**
     * Checks if the specified column name exists in this Dataset.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * boolean exists = dataset.containsColumn("name"); // returns true
     * }</pre>
     *
     * @param columnName the name(case-sensitive) of the column to check.
     * @return {@code true} if the column exists, {@code false} otherwise.
     */
    boolean containsColumn(String columnName);

    /**
     * Check if this {@code Dataset} contains all the specified columns.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * boolean hasAll = dataset.containsAllColumns(Arrays.asList("id", "name")); // returns true
     * }</pre>
     *
     * @param columnNames the collection of column names(case-sensitive) to check.
     * @return {@code true} if all the specified columns are included in the this {@code Dataset}
     */
    boolean containsAllColumns(Collection<String> columnNames);

    /**
     * Renames a column in the Dataset.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * dataset.renameColumn("name", "fullName");
     * }</pre>
     *
     * @param columnName the current name of the column.
     * @param newColumnName the new name for the column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset or the new column name exists in the Dataset.
     */
    void renameColumn(String columnName, String newColumnName) throws IllegalArgumentException;

    /**
     * Renames multiple columns in the Dataset.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * Map<String, String> renames = new HashMap<>();
     * renames.put("name", "fullName");
     * renames.put("age", "yearsOld");
     * dataset.renameColumns(renames);
     * }</pre>
     *
     * @param oldNewNames a map where the key is the current name of the column and the value is the new name for the column.
     * @throws IllegalArgumentException if any of the specified old column names does not exist in the Dataset or any of the new column names already exists in the Dataset.
     */
    void renameColumns(Map<String, String> oldNewNames) throws IllegalArgumentException;

    //    /**
    //     *
    //     * @param columnName
    //     * @param func
    //     */
    //    void renameColumn(String columnName, Function<? super String, String > func)  ;

    /**
     * Renames multiple columns in the Dataset using a function to determine the new names.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("first_name", "last_name"), data);
     * dataset.renameColumns(Arrays.asList("first_name", "last_name"), 
     *     name -> name.replace("_", ""));
     * }</pre>
     *
     * @param columnNames the collection of current column names to be renamed.
     * @param func a function that takes the current column name as input and returns the new column name.
     * @throws IllegalArgumentException if any of the specified old column names does not exist in the Dataset or any of the new column names already exists in the Dataset.
     */
    void renameColumns(Collection<String> columnNames, Function<? super String, String> func) throws IllegalArgumentException;

    /**
     * Renames all columns in the Dataset using a function to determine the new names.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("firstName", "lastName"), data);
     * dataset.renameColumns(name -> name.toUpperCase());
     * }</pre>
     *
     * @param func a function that takes the current column name as input and returns the new column name.
     * @throws IllegalArgumentException if any of the new column names already exists in the Dataset.
     */
    void renameColumns(Function<? super String, String> func) throws IllegalArgumentException;

    /**
     * Repositions a single column within the {@code Dataset} to a specified index.  
     * <br />
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * dataset.moveColumn("age", 1);
     * // Resulting order: ["id", "age", "name"]
     * }</pre>
     *
     * @param columnName the name of the column to move.
     * @param newPosition the zero-based index where the column should be placed.
     * @throws IllegalArgumentException if {@code columnName} does not exist in the dataset.
     * @throws IndexOutOfBoundsException if {@code newPosition} is outside the valid range of column indices.
     */
    void moveColumn(String columnName, int newPosition) throws IllegalArgumentException, IndexOutOfBoundsException;

    /**
     * Repositions multiple columns within the {@code Dataset} to a specified index.  
     * <br />
     * The relative order of the given columns is preserved in the move.  
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * dataset.moveColumns(Arrays.asList("name", "age"), 0);
     * // Resulting order: ["name", "age", "id"]
     * }</pre>
     *
     * @param columnNames the list of column names to move; their order in this list is maintained.
     * @param newPosition the zero-based index at which the first specified column will be placed.
     * @throws IllegalArgumentException if any column name in {@code columnNames} does not exist in the dataset.
     * @throws IndexOutOfBoundsException if {@code newPosition} is outside the valid range of column indices.
     */
    void moveColumns(List<String> columnNames, int newPosition) throws IllegalArgumentException, IndexOutOfBoundsException;

    //    /**
    //     * Moves multiple columns in the Dataset to new positions.
    //     *
    //     * <p>Example:</p>
    //     * <pre>{@code
    //     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
    //     * Map<String, Integer> moves = new HashMap<>();
    //     * moves.put("age", 0);
    //     * moves.put("name", 2);
    //     * dataset.moveColumns(moves);
    //     * }</pre>
    //     *
    //     * @param columnNameNewPositionMap a map where the key is the current name of the column and the value is the new position for the column.
    //     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or any of the new positions are out of bounds.
    //     */
    //    @Beta
    //    void moveColumns(Map<String, Integer> columnNameNewPositionMap) throws IllegalArgumentException;

    /**
     * Swaps the positions of two columns in the Dataset.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * dataset.swapColumnPosition("name", "age"); // swaps "name" and "age" positions
     * }</pre>
     *
     * @param columnNameA the name of the first column to be swapped.
     * @param columnNameB the name of the second column to be swapped.
     * @throws IllegalArgumentException if either of the specified column names does not exist in the Dataset.
     */
    void swapColumnPosition(String columnNameA, String columnNameB) throws IllegalArgumentException;

    /**
     * Repositions a row within the {@code Dataset} from one index to another.  
     * <br />
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * dataset.moveRow(0, 2);
     * // Row originally at index 0 is now at index 2
     * }</pre>
     *
     * @param rowIndex the zero-based index of the row to move.
     * @param newPosition the zero-based index where the row should be placed.
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code newPosition} is outside the valid range of row indices.
     */
    void moveRow(int rowIndex, int newPosition) throws IndexOutOfBoundsException;

    /**
     * Repositions a contiguous block of rows within the {@code Dataset} to a new index.  
     * <br />
     * The block is defined by the range {@code fromRowIndex} through {@code toRowIndex}, inclusive,  
     * and the relative order of rows within the block is preserved.  
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * dataset.moveRows(0, 2, 1);
     * // Rows originally at indices [0, 1, 2] are moved to start at index 1
     * }</pre>
     *
     * @param fromRowIndex the zero-based index of the first row in the block to move (inclusive).
     * @param toRowIndex the zero-based index of the last row in the block to move (inclusive).
     * @param newPosition the zero-based index where the block of rows should begin.
     * @throws IndexOutOfBoundsException if {@code fromRowIndex}, {@code toRowIndex}, or {@code newPosition} is outside the valid range of row indices.
     */
    void moveRows(int fromRowIndex, int toRowIndex, int newPosition) throws IndexOutOfBoundsException;

    /**
     * Swaps the positions of two rows in the Dataset.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * dataset.swapRowPosition(0, 1); // swaps row 0 and row 1
     * }</pre>
     *
     * @param rowIndexA the index of the first row to be swapped.
     * @param rowIndexB the index of the second row to be swapped.
     * @throws IndexOutOfBoundsException if either of the specified row indexes is out of bounds.
     */
    void swapRowPosition(int rowIndexA, int rowIndexB) throws IndexOutOfBoundsException;

    /**
     * Retrieves the value at the specified row and column index in the Dataset.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * String name = dataset.get(0, 1); // gets value at row 0, column 1
     * }</pre>
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

    //    /**
    //     * Retrieves the value at the specified row and column name in the Dataset.
    //     * <br />
    //     * There is NO underline auto-conversion from column value to target type: {@code T}.
    //     * So the column values must be the type which is assignable to target type.
    //     *
    //     * @param <T> the type of the value to be returned.
    //     * @param rowIndex the index of the row.
    //     * @param columnName the name of the column.
    //     * @return the value at the specified row and column name.
    //     * @throws IndexOutOfBoundsException if the specified row index is out of bounds.
    //     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
    //     * @deprecated Use {@link #get(int, int)} instead for better performance.
    //     */
    //    default <T> T get(int rowIndex, String columnName) throws IndexOutOfBoundsException {
    //        return get(rowIndex, getColumnIndex(columnName));
    //    }

    /**
     * Sets the value at the specified row and column index in the Dataset.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * dataset.set(0, 1, "John"); // sets value at row 0, column 1
     * }</pre>
     *
     * @param rowIndex the index of the row.
     * @param columnIndex the index of the column.
     * @param element the new value to be set at the specified row and column index.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified row or column index is out of bounds.
     */
    void set(int rowIndex, int columnIndex, Object element) throws IllegalStateException, IndexOutOfBoundsException;

    //    /**
    //     * Sets the value at the specified row and column name in the Dataset.
    //     *
    //     * @param rowIndex the index of the row.
    //     * @param columnName the name of the column.
    //     * @param element the new value to be set at the specified row and column name.
    //     * @throws IllegalStateException if the Dataset is frozen (read-only).
    //     * @throws IndexOutOfBoundsException if the specified row index is out of bounds.
    //     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
    //     * @deprecated Use {@link #set(int, int, Object)} instead for better performance.
    //     */
    //    default void set(int rowIndex, String columnName, Object element) throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
    //        set(rowIndex, getColumnIndex(columnName), element);
    //    }

    /**
     * Checks if the value at the specified row and column index in the Dataset is {@code null}.
     * <br />
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * boolean isNull = dataset.isNull(0, 1); // checks if value at row 0, column 1 is null
     * }</pre>
     *
     * @param rowIndex the index of the row.
     * @param columnIndex the index of the column.
     * @return {@code true} if the value at the specified row and column index is {@code null}, {@code false} otherwise.
     * @throws IndexOutOfBoundsException if the specified row or column index is out of bounds.
     */
    boolean isNull(int rowIndex, int columnIndex) throws IndexOutOfBoundsException;

    //    /**
    //     * Checks if the value at the specified row and column name in the Dataset is {@code null}.
    //     *
    //     * @param rowIndex the index of the row.
    //     * @param columnName the name of the column.
    //     * @return {@code true} if the value at the specified row and column name is {@code null}, {@code false} otherwise.
    //     * @throws IndexOutOfBoundsException if the specified row index is out of bounds.
    //     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
    //     * @deprecated Use {@link #isNull(int, int)} instead for better performance.
    //     */
    //    default boolean isNull(int rowIndex, String columnName) throws IndexOutOfBoundsException, IllegalArgumentException {
    //        return isNull(rowIndex, getColumnIndex(columnName));
    //    }

    /**
     * Retrieves the value at the specified column index in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * String value = dataset.get(1); // gets value at column index 1 for current row
     * }</pre>
     *
     * @param <T> the type of the value to be returned.
     * @param columnIndex the index of the column.
     * @return the value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    <T> T get(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the value at the specified column in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     * <br />
     * Using {@code get(int)} for better performance.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * String name = dataset.get("name"); // gets value for "name" column in current row
     * }</pre>
     *
     * @param <T> the type of the value to be returned.
     * @param columnName the name of the column.
     * @return the value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
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
     * Retrieves the boolean value at the specified column index in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Boolean}.
     * So the column values must be the type which is assignable to target type.
     * <br />
     * Returns default value (false) if the property is {@code null}.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "active"), data);
     * boolean isActive = dataset.getBoolean(1); // gets boolean value at column index 1
     * }</pre>
     *
     * @param columnIndex the index of the column.
     * @return the boolean value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    boolean getBoolean(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the boolean value at the specified column in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Boolean}.
     * So the column values must be the type which is assignable to target type.
     * <br />
     * Returns default value (false) if the property is {@code null}.
     * <br />
     * Using {@code getBoolean(int)} for better performance.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "active"), data);
     * boolean isActive = dataset.getBoolean("active");
     * }</pre>
     *
     * @param columnName the name of the column.
     * @return the boolean value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @see #getBoolean(int)
     */
    boolean getBoolean(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the char value at the specified column index in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Character}.
     * So the column values must be the type which is assignable to target type.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "grade"), data);
     * char grade = dataset.getChar(1); // gets char value at column index 1
     * }</pre>
     *
     * @param columnIndex the index of the column.
     * @return the char value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    char getChar(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the char value at the specified column in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Character}.
     * So the column values must be the type which is assignable to target type.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     * <br />
     * Using {@code getChar(int)} for better performance.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("grade"), new Object[][] {{'A'}});
     * char grade = dataset.getChar("grade");
     * }</pre>
     *
     * @param columnName the name of the column.
     * @return the char value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @see #getChar(int)
     */
    char getChar(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the byte value at the specified column index in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Byte}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("flag"), new Object[][] {{(byte) 1}});
     * byte flag = dataset.getByte("flag");
     * }</pre>
     *
     * @param columnIndex the index of the column.
     * @return the byte value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    byte getByte(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the byte value at the specified column in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Byte}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     * <br />
     * Using {@code getByte(int)} for better performance.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("flag"), new Object[][] {{(byte) 1}});
     * byte flag = dataset.getByte("flag");
     * }</pre>
     *
     * @param columnName the name of the column.
     * @return the byte value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @see #getByte(int)
     */
    byte getByte(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the short value at the specified column index in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Short}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("level"), new Object[][] {{(short) 3}});
     * short level = dataset.getShort("level");
     * }</pre>
     *
     * @param columnIndex the index of the column.
     * @return the short value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    short getShort(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the short value at the specified column in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Short}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     * <br />
     * Using {@code getShort(int)} for better performance.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("level"), new Object[][] {{(short) 3}});
     * short level = dataset.getShort("level");
     * }</pre>
     *
     * @param columnName the name of the column.
     * @return the short value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @see #getShort(int)
     */
    short getShort(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the integer value at the specified column index in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Integer}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), data);
     * int age = dataset.getInt(1); // gets integer value at column index 1
     * }</pre>
     *
     * @param columnIndex the index of the column.
     * @return the integer value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    int getInt(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the integer value at the specified column in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Integer}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     * <br />
     * Using {@code getInt(int)} for better performance.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), data);
     * int age = dataset.getInt("age");
     * }</pre>
     *
     * @param columnName the name of the column.
     * @return the integer value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @see #getInt(int)
     */
    int getInt(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the long value at the specified column index in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Long}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "timestamp"), data);
     * long timestamp = dataset.getLong(1); // gets long value at column index 1
     * }</pre>
     *
     * @param columnIndex the index of the column.
     * @return the long value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    long getLong(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the long value at the specified column in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Long}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0) if the property is {@code null}.
     * <br />
     * Using {@code getLong(int)} for better performance.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("timestamp"), new Object[][] {{123456789L}});
     * long timestamp = dataset.getLong("timestamp");
     * }</pre>
     *
     * @param columnName the name of the column.
     * @return the long value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @see #getLong(int)
     */
    long getLong(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the float value at the specified column index in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Float}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0f) if the property is {@code null}.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "price"), data);
     * float price = dataset.getFloat(1); // gets float value at column index 1
     * }</pre>
     *
     * @param columnIndex the index of the column.
     * @return the float value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    float getFloat(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the float value at the specified column in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Float}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0f) if the property is {@code null}.
     * <br />
     * Using {@code getFloat(int)} for better performance.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("ratio"), new Object[][] {{0.5f}});
     * float ratio = dataset.getFloat("ratio");
     * }</pre>
     *
     * @param columnName the name of the column.
     * @return the float value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @see #getFloat(int)
     */
    float getFloat(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the double value at the specified column index in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Double}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0d) if the property is {@code null}.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "score"), data);
     * double score = dataset.getDouble(1); // gets double value at column index 1
     * }</pre>
     *
     * @param columnIndex the index of the column.
     * @return the double value at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    double getDouble(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the double value at the specified column in the Dataset for the current row.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Double}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     * <br />
     * Returns default value (0d) if the property is {@code null}.
     * <br />
     * Using {@code getDouble(int)} for better performance.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("score"), new Object[][] {{98.5}});
     * double score = dataset.getDouble("score");
     * }</pre>
     *
     * @param columnName the name of the column.
     * @return the double value at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @see #getDouble(int)
     */
    double getDouble(String columnName) throws IllegalArgumentException;

    /**
     * Checks if the value at the specified column index in the Dataset for the current row is {@code null}.
     * <br />
     * This method can be used to validate the data before performing operations that do not support {@code null} values.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, null}});
     * boolean missing = dataset.isNull(0, 1);
     * }</pre>
     *
     * @param columnIndex the index of the column.
     * @return {@code true} if the value at the specified column index is {@code null}, {@code false} otherwise.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    boolean isNull(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Checks if the value at the specified column in the Dataset for the current row is {@code null}.
     * <br />
     * This method can be used to validate the data before performing operations that do not support {@code null} values.
     * <br />
     * Using {@code isNull(int)} for better performance.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, null}});
     * boolean missing = dataset.isNull(0, 1);
     * }</pre>
     *
     * @param columnName the name of the column.
     * @return {@code true} if the value at the specified column is {@code null}, {@code false} otherwise.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @see #isNull(int)
     */
    boolean isNull(String columnName) throws IllegalArgumentException;

    /**
     * Sets the value at the specified column index in the Dataset for the current row.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * dataset.set(0, 1, "Bob");
     * }</pre>
     *
     * @param columnIndex the index of the column.
     * @param value the new value to be set at the specified column index.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    void set(int columnIndex, Object value) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Sets the value at the specified column in the Dataset for the current row.
     * <br />
     * Using {@code set(int, Object)} for better performance.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * dataset.set(0, 1, "Bob");
     * }</pre>
     *
     * @param columnName the name of the column.
     * @param value the new value to be set at the specified column.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @throws IndexOutOfBoundsException if a row or column index falls outside the valid range
     * @see #set(int, Object)
     */
    void set(String columnName, Object value) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Retrieves the values of the specified column index in the Dataset as an ImmutableList.
     * <br />
     * The returned list is immutable and any attempt to modify it will result in an UnsupportedOperationException.
     * <br />
     * The type of the values in the list will be the same as the type of the column.
     * <br />
     * The order of the values in the list reflects the order of the rows in the Dataset.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * ImmutableList<String> names = dataset.getColumn(1);
     * }</pre>
     *
     * @param <T> the type of the values to be returned.
     * @param columnIndex the index of the column.
     * @return an ImmutableList of values at the specified column index.
     * @throws IndexOutOfBoundsException if the specified column index is out of bounds.
     */
    <T> ImmutableList<T> getColumn(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves the values of the specified column in the Dataset as an ImmutableList.
     * <br />
     * The returned list is immutable and any attempt to modify it will result in an UnsupportedOperationException.
     * <br />
     * The type of the values in the list will be the same as the type of the column.
     * <br />
     * The order of the values in the list reflects the order of the rows in the Dataset.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * ImmutableList<String> names = dataset.getColumn("name");
     * }</pre>
     *
     * @param <T> the type of the values to be returned.
     * @param columnName the name of the column.
     * @return an ImmutableList of values at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     */
    <T> ImmutableList<T> getColumn(String columnName) throws IllegalArgumentException;

    /**
     * Retrieves the values of the specified column in the Dataset as a List.
     * <br />
     * The returned list is a copy and modifications to it will not affect the original Dataset.
     * <br />
     * The type of the values in the list will be the same as the type of the column.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * List<String> names = dataset.copyColumn("name");
     * names.add("newName"); // This won't affect the original Dataset
     * }</pre>
     *
     * @param <T> the type of the values to be returned.
     * @param columnName the name of the column.
     * @return a List of values at the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     */
    <T> List<T> copyColumn(String columnName) throws IllegalArgumentException;

    /**
     * Adds a new column to the Dataset.
     * <br />
     * The new column is added at the end of the existing columns.
     * The size of this list should match the number of rows in the Dataset.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * dataset.addColumn("age", Arrays.asList(25, 30, 35));
     * }</pre>
     *
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the Dataset.
     * @param column The data for the new column. It should be a list where each element represents a row in the column.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the new column name already exists in the Dataset or the provided collection is not empty and its size does not match the number of rows in the Dataset.
     */
    void addColumn(String newColumnName, Collection<?> column) throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the Dataset at the specified position.
     * <br />
     * The new column is added at the position specified by newColumnPosition. Existing columns at and after this position are shifted to the right.
     * The size of the list provided should match the number of rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.addColumn("label", Arrays.asList("A", "B"));
     * }</pre>
     *
     * @param newColumnPosition The position at which the new column should be added. It should be a valid index within the current column range.
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the Dataset.
     * @param column The data for the new column. It should be a collection where each element represents a row in the column.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified {@code newColumnPosition} is less than zero or bigger than column size or the new column name already exists in the Dataset,
     * @throws IllegalArgumentException if the supplied arguments reference non-existent columns or otherwise violate dataset constraints
     *                               or if the provided collection is not empty and its size does not match the number of rows in the Dataset, or the newColumnPosition is out of bounds.
     */
    void addColumn(int newColumnPosition, String newColumnName, Collection<?> column) throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the Dataset.
     * <br />
     * The new column is generated by applying a function to an existing column. The function takes the value of the existing column for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the end of the existing columns.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("salary"), data);
     * dataset.addColumn("taxAmount", "salary", salary -> (Double) salary * 0.25);
     * }</pre>
     *
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the Dataset.
     * @param fromColumnName The name of the existing column to be used as input for the function.
     * @param func The function to generate the values for the new column. It takes the value of the existing column for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the new column name already exists in the Dataset or the existing column name does not exist in the Dataset.
     */
    void addColumn(String newColumnName, String fromColumnName, Function<?, ?> func) throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the Dataset at the specified position.
     * <br />
     * The new column is generated by applying a function to an existing column. The function takes the value of the existing column for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the position specified by newColumnPosition. Existing columns at and after this position are shifted to the right.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("name", "age"), data);
     * dataset.addColumn(1, "ageGroup", "age", age -> (Integer) age >= 18 ? "Adult" : "Minor");
     * }</pre>
     *
     * @param newColumnPosition The position at which the new column should be added. It should be a valid index within the current column range.
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the Dataset.
     * @param fromColumnName The name of the existing column to be used as input for the function.
     * @param func The function to generate the values for the new column. It takes the value of the existing column for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newColumnPosition} is less than zero or bigger than column size or the new column name already exists in the Dataset, or if the existing column name does not exist in the Dataset.
     */
    void addColumn(int newColumnPosition, String newColumnName, String fromColumnName, Function<?, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the Dataset.
     * <br />
     * The new column is generated by applying a function to multiple existing columns. The function takes the values of the existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the end of the existing columns.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("firstName", "lastName"), data);
     * dataset.addColumn("fullName", Arrays.asList("firstName", "lastName"), 
     *     row -> row.get(0) + " " + row.get(1));
     * }</pre>
     *
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the Dataset.
     * @param fromColumnNames The names of the existing columns to be used as input for the function.
     * @param func The function to generate the values for the new column. It takes the values of the existing columns for each row and returns the value for the new column for that row. The input to the function is a DisposableObjArray containing the values of the existing columns for a particular row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the new column name already exists in the Dataset or any of the existing column names does not exist in the Dataset.
     */
    void addColumn(String newColumnName, Collection<String> fromColumnNames, Function<? super DisposableObjArray, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the Dataset at the specified position.
     * <br />
     * The new column is generated by applying a function to multiple existing columns. The function takes the values of the existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the position specified by newColumnPosition. Existing columns at and after this position are shifted to the right.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.addColumn("label", Arrays.asList("A", "B"));
     * }</pre>
     *
     * @param newColumnPosition The position at which the new column should be added. It should be a valid index within the current column range.
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the Dataset.
     * @param fromColumnNames The names of the existing columns to be used as input for the function.
     * @param func The function to generate the values for the new column. It takes the values of the existing columns for each row and returns the value for the new column for that row. The input to the function is a DisposableObjArray containing the values of the existing columns for a particular row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newColumnPosition} is less than zero or bigger than column size or the new column name already exists in the Dataset, or if any of the existing column names does not exist in the Dataset.
     */
    void addColumn(int newColumnPosition, String newColumnName, Collection<String> fromColumnNames, Function<? super DisposableObjArray, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the Dataset.
     * <br />
     * The new column is generated by applying a BiFunction to two existing columns. The BiFunction takes the values of the existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the end of the existing columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.addColumn("label", Arrays.asList("A", "B"));
     * }</pre>
     *
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the Dataset.
     * @param fromColumnNames A Tuple2 containing the names of the two existing columns to be used as input for the BiFunction.
     * @param func The BiFunction to generate the values for the new column. It takes the values of the two existing columns for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the new column name already exists in the Dataset or any of the existing column names does not exist in the Dataset.
     */
    void addColumn(String newColumnName, Tuple2<String, String> fromColumnNames, BiFunction<?, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the Dataset at the specified position.
     * <br />
     * The new column is generated by applying a BiFunction to two existing columns. The BiFunction takes the values of the two existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the position specified by newColumnPosition. Existing columns at and after this position are shifted to the right.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.addColumn("label", Arrays.asList("A", "B"));
     * }</pre>
     *
     * @param newColumnPosition The position at which the new column should be added. It should be a valid index within the current column range.
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the Dataset.
     * @param fromColumnNames A Tuple2 containing the names of the two existing columns to be used as input for the BiFunction.
     * @param func The BiFunction to generate the values for the new column. It takes the values of the two existing columns for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newColumnPosition} is less than zero or bigger than column size or the new column name already exists in the Dataset, or if any of the existing column names does not exist in the Dataset.
     */
    void addColumn(int newColumnPosition, String newColumnName, Tuple2<String, String> fromColumnNames, BiFunction<?, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the Dataset.
     * <br />
     * The new column is generated by applying a TriFunction to three existing columns. The TriFunction takes the values of the three existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the end of the existing columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.addColumn("label", Arrays.asList("A", "B"));
     * }</pre>
     *
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the Dataset.
     * @param fromColumnNames A Tuple3 containing the names of the three existing columns to be used as input for the TriFunction.
     * @param func The TriFunction to generate the values for the new column. It takes the values of the three existing columns for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the new column name already exists in the Dataset or any of the existing column names does not exist in the Dataset.
     */
    void addColumn(String newColumnName, Tuple3<String, String, String> fromColumnNames, TriFunction<?, ?, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new column to the Dataset at the specified position.
     * <br />
     * The new column is generated by applying a TriFunction to three existing columns. The TriFunction takes the values of the three existing columns for each row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the position specified by newColumnPosition. Existing columns at and after this position are shifted to the right.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.addColumn("label", Arrays.asList("A", "B"));
     * }</pre>
     *
     * @param newColumnPosition The position at which the new column should be added. It should be a valid index within the current column range.
     * @param newColumnName The name of the new column to be added. It should not be a name that already exists in the Dataset.
     * @param fromColumnNames A Tuple3 containing the names of the three existing columns to be used as input for the TriFunction.
     * @param func The TriFunction to generate the values for the new column. It takes the values of the three existing columns for each row and returns the value for the new column for that row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newColumnPosition} is less than zero or bigger than column size or the new column name already exists in the Dataset, or if any of the existing column names does not exist in the Dataset.
     */
    void addColumn(int newColumnPosition, String newColumnName, Tuple3<String, String, String> fromColumnNames, TriFunction<?, ?, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds multiple columns to the Dataset.
     * <br />
     * The new columns are added at the end of the existing columns.
     * Each collection in the list represents a column, and the size of each collection should match the number of rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * dataset.addColumns(Arrays.asList("age", "salary"), Arrays.asList(
     *     Arrays.asList(25, 30, 35),
     *     Arrays.asList(50000.0, 60000.0, 70000.0)
     * ));
     * }</pre>
     *
     * @param newColumnNames A list containing the names of the new columns to be added. These should not be names that already exist in the Dataset.
     * @param newColumns A list of collections, where each collection represents a column. Each collection should have a size that matches the number of rows in the Dataset.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the new column names already exist in the Dataset, if size of {@code newColumnNames} does not match the size of {@code columns},
     *                                  or if any collection in {@code columns} is not empty and its size does not match the number of rows in the Dataset.
     */
    void addColumns(List<String> newColumnNames, List<? extends Collection<?>> newColumns) throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds multiple columns to the Dataset at the specified position.
     * <br />
     * The new columns are added at the position specified by newColumnPosition. Existing columns at and after this position are shifted to the right.
     * Each collection in the list represents a column, and the size of each collection should match the number of rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * dataset.addColumns(1, Arrays.asList("age", "salary"), Arrays.asList(
     *     Arrays.asList(25, 30, 35),
     *     Arrays.asList(50000.0, 60000.0, 70000.0)
     * ));
     * }</pre>
     *
     * @param newColumnPosition The position at which the new columns should be added. It should be a valid index within the current column range.
     * @param newColumnNames A list containing the names of the new columns to be added. These should not be names that already exist in the Dataset.
     * @param newColumns A list of collections, where each collection represents a column. Each collection should have a size that matches the number of rows in the Dataset.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newColumnPosition} is less than zero or bigger than column size or any of the new column names already exist in the Dataset,
     *                                  or if size of {@code newColumnNames} does not match the size of {@code columns},
     *                                  or if any collection in {@code columns} is not empty and its size does not match the number of rows in the Dataset,
     *                                  or the newColumnPosition is out of bounds.
     */
    void addColumns(int newColumnPosition, List<String> newColumnNames, List<? extends Collection<?>> newColumns)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Removes a column from the Dataset.
     * <br />
     * The column is identified by its name. All data in the column is removed and returned as a List.
     * <br />
     * The order of the values in the returned list reflects the order of the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * List<Integer> removedIds = dataset.removeColumn("id");
     * }</pre>
     *
     * @param <T> The type of the values in the column.
     * @param columnName The name of the column to be removed. It should be a name that exists in the Dataset.
     * @return A List containing the values of the removed column.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     */
    <T> List<T> removeColumn(String columnName) throws IllegalStateException, IllegalArgumentException;

    /**
     * Removes multiple columns from the Dataset.
     * <br />
     * The columns are identified by their names provided in the collection. All data in these columns are removed.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age", "salary"), data);
     * dataset.removeColumns(Arrays.asList("id", "salary"));
     * }</pre>
     *
     * @param columnNames A collection containing the names of the columns to be removed. These should be names that exist in the Dataset.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset.
     */
    void removeColumns(Collection<String> columnNames) throws IllegalStateException, IllegalArgumentException;

    /**
     * Removes multiple columns from the Dataset.
     * <br />
     * The columns to be removed are identified by a Predicate function. The function is applied to each column name, and if it returns {@code true}, the column is removed.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("tempId", "name", "tempAge", "salary"), data);
     * dataset.removeColumns(columnName -> columnName.startsWith("temp"));
     * }</pre>
     *
     * @param filter A Predicate function to determine which columns should be removed. It should return {@code true} for column names that should be removed, and {@code false} for those that should be kept.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
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
     * Updates the values in a specified column of the Dataset.
     * <br />
     * The update is performed by applying a function to each value in the column. The function takes the current value and returns the new value.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("name", "age"), data);
     * dataset.updateColumn("name", name -> ((String) name).toUpperCase());
     * }</pre>
     *
     * @param columnName The name of the column to be updated. It should be a name that exists in the Dataset.
     * @param func The function to be applied to each value in the column. It takes the current value and returns the new value.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     */
    void updateColumn(String columnName, Function<?, ?> func) throws IllegalStateException, IllegalArgumentException;

    /**
     * Updates the values in multiple specified columns of the Dataset.
     * <br />
     * The update is performed by applying a function to each value in the specified columns. The function takes the current value and returns the new value.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("firstName", "lastName"), data);
     * dataset.updateColumns(Arrays.asList("firstName", "lastName"), name -> ((String) name).trim());
     * }</pre>
     *
     * @param columnNames A collection containing the names of the columns to be updated. These should be names that exist in the Dataset.
     * @param func The function to be applied to each value in the columns. It takes the current value and returns the new value.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset.
     */
    void updateColumns(Collection<String> columnNames, Function<?, ?> func) throws IllegalStateException, IllegalArgumentException;

    /**
     * Converts the values in a specified column of the Dataset to a specified target type.
     * <br />
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("age"), data);
     * dataset.convertColumn("age", Integer.class); // Convert string ages to integers
     * }</pre>
     *
     * @param columnName The name of the column to be converted. It should be a name that exists in the Dataset.
     * @param targetType The Class object representing the target type to which the column values should be converted.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset or a value cannot be cast to the target type.
     * @throws NumberFormatException if string value of the column cannot be parsed to the target(Number) type.
     * @throws RuntimeException if any other error occurs during the conversion.
     * @see N#convert(Object, Class)
     */
    void convertColumn(String columnName, Class<?> targetType) throws IllegalStateException, IllegalArgumentException, NumberFormatException, RuntimeException;

    /**
     * Converts the values in multiple specified columns of the Dataset to their respective target types.
     * <br />
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("age", "salary"), data);
     * Map<String, Class<?>> types = Map.of("age", Integer.class, "salary", Double.class);
     * dataset.convertColumns(types);
     * }</pre>
     *
     * @param columnTargetTypes A map where the key is the column name and the value is the Class object representing the target type to which the column values should be converted. The column names should exist in the Dataset.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset.
     * @see N#convert(Object, Class)
     */
    void convertColumns(Map<String, Class<?>> columnTargetTypes) throws IllegalStateException, IllegalArgumentException;

    //
    //    /**
    //     * convert the specified columns to target types.
    //     *
    //     * @param targetColumnTypes fill the element with {@code null} if don't wan to convert the target column.
    //     */
    //    void convertColumn(Class<?>[] targetColumnTypes);

    /**
     * Combines multiple columns into a new column in the Dataset using a default combining strategy.
     * <br />
     * The new column is created by merging the values of the specified columns into the new column for each row.
     * The new column's type must be Object[], Collection, Map, or Bean class.
     * <br />
     * The new column is added at the position of the smallest index among the specified columns.
     * <br />
     * The original columns that were combined are removed from the Dataset after the new column is created.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("firstName", "lastName"), data);
     * dataset.combineColumns(Arrays.asList("dept_id", "dept_name"), "dept", Map.class);
     * // Creates a new "dept" column by combining "dept_id" and "dept_name" into a Map
     * // The original "dept_id" and "dept_name" columns are removed
     * }</pre>
     *
     * @param columnNames A collection containing the names of the columns to be combined. These should be names that exist in the Dataset.
     * @param newColumnName The name of the new column to be created. It should not be a name that already exists in the Dataset.
     * @param newColumnType The Class object representing the type of the new column. It must be Object[], Collection, Map, or a Bean class.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset, 
     *                                  or {@code columnNames} is empty, 
     *                                  or the new column name already exists in the Dataset,
     *                                  or the specified column type is not Object[], Collection, Map, or a Bean class.
     * @see #combineColumns(Collection, String, Function)
     * @see #addColumn(String, Collection, Function)
     * @see #addColumn(int, String, Collection, Function)
     * @see #toList(Collection, Class)
     */
    void combineColumns(Collection<String> columnNames, String newColumnName, Class<?> newColumnType) throws IllegalStateException, IllegalArgumentException;

    /**
     * Combines multiple columns into a new column in the Dataset using a custom combining function.
     * <br />
     * The new column is created by applying a function to the values of the specified columns for each row. 
     * The function takes a DisposableObjArray of the values in the existing columns for a particular row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the position of the smallest index among the specified columns.
     * <br />
     * The original columns that were combined are removed from the Dataset after the new column is created.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("x", "y", "z"), data);
     * dataset.combineColumns(Arrays.asList("x", "y", "z"), "coordinates",
     *     row -> "(" + row.get(0) + "," + row.get(1) + "," + row.get(2) + ")");
     * // Creates a new "coordinates" column by combining x, y, z values
     * // The original "x", "y", and "z" columns are removed
     * }</pre>
     *
     * @param columnNames A collection containing the names of the columns to be combined. These should be names that exist in the Dataset.
     * @param newColumnName The name of the new column to be created. It should not be a name that already exists in the Dataset.
     * @param combineFunc The function to generate the values for the new column. It takes a DisposableObjArray of the values in the existing columns for a particular row and returns the value for the new column for that row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset,
     *                                  or {@code columnNames} is empty,
     *                                  or the new column name already exists in the Dataset.
     * @see #combineColumns(Collection, String, Class)
     * @see #addColumn(String, Collection, Function)
     */
    void combineColumns(Collection<String> columnNames, String newColumnName, Function<? super DisposableObjArray, ?> combineFunc)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Combines two columns into a new column in the Dataset using a custom BiFunction.
     * <br />
     * The new column is created by applying a BiFunction to the values of the specified columns for each row.
     * The BiFunction takes the values of the two existing columns for a particular row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the position of the smallest index among the specified columns.
     * <br />
     * The original columns that were combined are removed from the Dataset after the new column is created.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("width", "height"), data);
     * dataset.combineColumns(Tuple.of("width", "height"), "area", (w, h) -> (Double) w * (Double) h);
     * // Creates a new "area" column by multiplying width and height values
     * // The original "width" and "height" columns are removed
     * }</pre>
     *
     * @param columnNames A Tuple2 containing the names of the two columns to be combined. These should be names that exist in the Dataset.
     * @param newColumnName The name of the new column to be created. It should not be a name that already exists in the Dataset.
     * @param combineFunc The BiFunction to generate the values for the new column. It takes the values of the two existing columns for a particular row and returns the value for the new column for that row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset,
     *                                  or the new column name already exists in the Dataset.
     * @see #combineColumns(Collection, String, Function)
     * @see #combineColumns(Tuple3, String, TriFunction)
     * @see #addColumn(String, Tuple2, BiFunction)
     */
    void combineColumns(Tuple2<String, String> columnNames, String newColumnName, BiFunction<?, ?, ?> combineFunc)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Combines three columns into a new column in the Dataset using a custom TriFunction.
     * <br />
     * The new column is created by applying a TriFunction to the values of the specified columns for each row.
     * The TriFunction takes the values of the three existing columns for a particular row and returns the value for the new column for that row.
     * <br />
     * The new column is added at the position of the smallest index among the specified columns.
     * <br />
     * The original columns that were combined are removed from the Dataset after the new column is created.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("x", "y", "z"), data);
     * dataset.combineColumns(Tuple.of("x", "y", "z"), "coordinates", 
     *     (x, y, z) -> "(" + x + "," + y + "," + z + ")");
     * // Creates a new "coordinates" column by combining x, y, z values
     * // The original "x", "y", and "z" columns are removed
     * }</pre>
     *
     * @param columnNames A Tuple3 containing the names of the three columns to be combined. These should be names that exist in the Dataset.
     * @param newColumnName The name of the new column to be created. It should not be a name that already exists in the Dataset.
     * @param combineFunc The TriFunction to generate the values for the new column. It takes the values of the three existing columns for a particular row and returns the value for the new column for that row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset,
     *                                  or the new column name already exists in the Dataset.
     * @see #combineColumns(Collection, String, Function)
     * @see #combineColumns(Tuple2, String, BiFunction)
     * @see #addColumn(String, Tuple3, TriFunction)
     */
    void combineColumns(Tuple3<String, String, String> columnNames, String newColumnName, TriFunction<?, ?, ?, ?> combineFunc)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Divides a column into multiple new columns in the Dataset.
     * <br />
     * The division is performed by applying a function to each value in the specified column. 
     * The function takes the current value and returns a List of new values, each of which will be a value in one of the new columns.
     * <br />
     * The new columns are added at the position of the original column, and the original column is removed.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("fullName"), data);
     * dataset.divideColumn("fullName", Arrays.asList("firstName", "lastName"), 
     *     name -> Arrays.asList(((String) name).split(" ")));
     * }</pre>
     *
     * @param columnName The name of the column to be divided. It should be a name that exists in the Dataset.
     * @param newColumnNames A collection containing the names of the new columns to be created. These should not be names that already exist in the Dataset. The size of this collection should match the size of the Lists returned by the divideFunc.
     * @param divideFunc The function to be applied to each value in the column. It takes the current value and returns a List of new values. The size of this List should match the size of the newColumnNames collection.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset, any of the new column names already exist in the Dataset, or {@code newColumnNames} is empty.
     */
    void divideColumn(String columnName, Collection<String> newColumnNames, Function<?, ? extends List<?>> divideFunc)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Divides a column into multiple new columns in the Dataset using a BiConsumer.
     * <br />
     * The division is performed by applying a BiConsumer to each value in the specified column. 
     * The BiConsumer takes the current value and an Object array, and it should populate the array with the new values for the new columns.
     * <br />
     * The new columns are added at the position of the original column, and the original column is removed.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("coordinates"), data);
     * dataset.divideColumn("coordinates", Arrays.asList("x", "y", "z"), 
     *     (coord, output) -> {
     *         String[] parts = ((String) coord).split(",");
     *         output[0] = Double.parseDouble(parts[0]);
     *         output[1] = Double.parseDouble(parts[1]);
     *         output[2] = Double.parseDouble(parts[2]);
     *     });
     * // Divides "coordinates" column into "x", "y", "z" columns
     * // The original "coordinates" column is removed
     * }</pre>
     *
     * @param columnName The name of the column to be divided. It should be a name that exists in the Dataset.
     * @param newColumnNames A collection containing the names of the new columns to be created. These should not be names that already exist in the Dataset. The size of this collection determines the size of the Object array passed to the BiConsumer.
     * @param output The BiConsumer to be applied to each value in the column. It takes the current value and an Object array, and it should populate the array with the new values for the new columns. The array size matches the size of {@code newColumnNames}.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset, 
     *                                  or any of the new column names already exist in the Dataset, 
     *                                  or {@code newColumnNames} is empty.
     * @see #divideColumn(String, Collection, Function)
     * @see #combineColumns(Collection, String, Function)
     */
    void divideColumn(String columnName, Collection<String> newColumnNames, BiConsumer<?, Object[]> output)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Divides a column into two new columns in the Dataset using a BiConsumer.
     * <br />
     * The division is performed by applying a BiConsumer to each value in the specified column.
     * The BiConsumer takes the current value and a Pair object, and it should populate the Pair with the new values for the new columns.
     * <br />
     * The new columns are added at the position of the original column, and the original column is removed.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("fullName"), data);
     * dataset.divideColumn("fullName", Tuple.of("firstName", "lastName"),
     *     (name, pair) -> {
     *         String[] parts = ((String) name).split(" ");
     *         pair.setLeft(parts[0]);
     *         pair.setRight(parts[1]);
     *     });
     * // Divides "fullName" column into "firstName" and "lastName" columns
     * // The original "fullName" column is removed
     * }</pre>
     *
     * @param columnName The name of the column to be divided. It should be a name that exists in the Dataset.
     * @param newColumnNames A Tuple2 containing the names of the two new columns to be created. These should not be names that already exist in the Dataset.
     * @param output The BiConsumer to be applied to each value in the column. It takes the current value and a Pair object, and it should populate the Pair with the new values for the new columns.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset,
     *                                  or any of the new column names already exist in the Dataset.
     * @see #divideColumn(String, Collection, Function)
     * @see #divideColumn(String, Collection, BiConsumer)
     * @see #combineColumns(Tuple2, String, BiFunction)
     */
    void divideColumn(String columnName, Tuple2<String, String> newColumnNames, BiConsumer<?, Pair<Object, Object>> output)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Divides a column into three new columns in the Dataset using a BiConsumer.
     * <br />
     * The division is performed by applying a BiConsumer to each value in the specified column.
     * The BiConsumer takes the current value and a Triple object, and it should populate the Triple with the new values for the new columns.
     * <br />
     * The new columns are added at the position of the original column, and the original column is removed.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("coordinates"), data);
     * dataset.divideColumn("coordinates", Tuple.of("x", "y", "z"),
     *     (coord, triple) -> {
     *         String[] parts = ((String) coord).split(",");
     *         triple.setLeft(Double.parseDouble(parts[0]));
     *         triple.setMiddle(Double.parseDouble(parts[1]));
     *         triple.setRight(Double.parseDouble(parts[2]));
     *     });
     * // Divides "coordinates" column into "x", "y", "z" columns
     * // The original "coordinates" column is removed
     * }</pre>
     *
     * @param columnName The name of the column to be divided. It should be a name that exists in the Dataset.
     * @param newColumnNames A Tuple3 containing the names of the three new columns to be created. These should not be names that already exist in the Dataset.
     * @param output The BiConsumer to be applied to each value in the column. It takes the current value and a Triple object, and it should populate the Triple with the new values for the new columns.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset,
     *                                  or any of the new column names already exist in the Dataset.
     * @see #divideColumn(String, Collection, Function)
     * @see #divideColumn(String, Collection, BiConsumer)
     * @see #divideColumn(String, Tuple2, BiConsumer)
     * @see #combineColumns(Tuple3, String, TriFunction)
     */
    void divideColumn(String columnName, Tuple3<String, String, String> newColumnNames, BiConsumer<?, Triple<Object, Object, Object>> output)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new row to the Dataset.
     * <br />
     * The row can be represented in various formats such as an Object array, List, Map, or a Bean with getter/setter methods.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), existingData);
     * dataset.addRow(Arrays.asList(101, "John Doe", 30));
     * }</pre>
     *
     * @param row The new row to be added to the Dataset. It can be an Object array, List, Map, or a Bean with getter/setter methods.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the structure of the row does not match the required type - Object array, List, Map, or Bean.
     */
    void addRow(Object row) throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds a new row to the Dataset at the specified position.
     * <br />
     * The row can be represented in various formats such as an Object array, List, Map, or a Bean with getter/setter methods.
     * Existing rows at and after the new row position are shifted down.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * dataset.addRow(new Object[] {2, "Bob"});
     * }</pre>
     *
     * @param newRowPosition The position at which the new row should be added. It should be a valid index within the current row range.
     * @param row The new row to be added to the Dataset. It can be an Object array, List, Map, or a Bean with getter/setter methods.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newRowPosition} is less than zero or bigger than row size, or the structure of the row does not match the required type - Object array, List, Map, or Bean.
     */
    void addRow(int newRowPosition, Object row) throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds multiple new rows to the Dataset.
     * <br />
     * Each row can be represented in various formats such as an Object array, List, Map, or a Bean with getter/setter methods.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), existingData);
     * List<Object> newRows = Arrays.asList(
     *     Arrays.asList(102, "Jane Smith", 25),
     *     Arrays.asList(103, "Mike Johnson", 40)
     * );
     * dataset.addRows(newRows);
     * }</pre>
     *
     * @param rows A collection of new rows to be added to the Dataset. Each row can be an Object array, List, Map, or a Bean with getter/setter methods.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the structure of any row does not match the required type - Object array, List, Map, or Bean.
     */
    void addRows(Collection<?> rows) throws IllegalStateException, IllegalArgumentException;

    /**
     * Adds multiple new rows to the Dataset at the specified position.
     * <br />
     * Each row can be represented in various formats such as an Object array, List, Map, or a Bean with getter/setter methods.
     * Existing rows at and after the new row position are shifted down.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}});
     * dataset.addRows(Arrays.asList(new Object[] {2}, new Object[] {3}));
     * }</pre>
     *
     * @param newRowPosition The position at which the new rows should be added. It should be a valid index within the current row range.
     * @param rows A collection of new rows to be added to the Dataset. Each row can be an Object array, List, Map, or a Bean with getter/setter methods.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified {@code newRowPosition} is less than zero or bigger than row size, or if the structure of any row does not match the required type - Object array, List, Map, or Bean.
     */
    void addRows(int newRowPosition, Collection<?> rows) throws IllegalStateException, IllegalArgumentException;

    /**
     * Removes a row from the Dataset.
     * <br />
     * The row is identified by its index. All data in the row is removed.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * dataset.removeRow(2); // Remove the third row (0-based index)
     * }</pre>
     *
     * @param rowIndex The index of the row to be removed. It should be a valid index within the current row range.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     */
    void removeRow(int rowIndex) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Removes multiple rows from the Dataset.
     * <br />
     * The rows are identified by their indices. All data in these rows are removed.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}, {3}});
     * dataset.removeRows(new int[] {0, 2});
     * }</pre>
     *
     * @param rowIndexesToRemove The indices of the rows to be removed. Each index should be a valid index within the current row range.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IndexOutOfBoundsException if any of the specified indices is out of bounds.
     */
    void removeRows(int... rowIndexesToRemove) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Removes a range of rows from the Dataset.
     * <br />
     * The range is specified by an inclusive start index and an exclusive end index. All rows within this range are removed.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}, {3}});
     * dataset.removeRowRange(0, 1);
     * }</pre>
     *
     * @param inclusiveFromRowIndex The start index of the range. It should be a valid index within the current row range. The row at this index is included in the removal.
     * @param exclusiveToRowIndex The end index of the range. It should be a valid index within the current row range. The row at this index is not included in the removal.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified {@code inclusiveFromRowIndex} is less than zero, or the specified {@code exclusiveToRowIndex} is bigger than row size, or {@code inclusiveFromRowIndex} is bigger than {@code exclusiveToRowIndex}.
     */
    void removeRowRange(int inclusiveFromRowIndex, int exclusiveToRowIndex) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Removes duplicate rows from the Dataset based on values in the specified key column.
     * <br />
     * This method identifies rows that have duplicate values in the specified column and removes all but the first occurrence.
     * The comparison is based on the natural equality of the values in the key column.
     * <br />
     * The first row encountered with a particular key value is retained, and subsequent rows with the same key value are removed.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"), 
     *     new Object[][] {
     *         {1, "John", "IT"},
     *         {2, "Jane", "HR"},
     *         {3, "John", "Finance"},  // Duplicate name
     *         {4, "Bob", "IT"}
     *     });
     * dataset.removeDuplicateRowsBy("name");
     * // Result: Only rows with id=1, id=2, and id=4 remain
     * // The row with id=3 is removed because "John" already exists
     * }</pre>
     *
     * @param keyColumnName The name of the column to use for identifying duplicates. It should be a name that exists in the Dataset.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @see #removeDuplicateRowsBy(String, Function)
     * @see #removeDuplicateRowsBy(Collection)
     * @see #distinctBy(String)
     */
    void removeDuplicateRowsBy(String keyColumnName) throws IllegalStateException, IllegalArgumentException;

    /**
     * Removes duplicate rows from the Dataset based on values in the specified key column and a custom key extractor function.
     * <br />
     * This method identifies rows that have duplicate values in the specified column, as determined by the provided key extractor function, and removes all but the first occurrence.
     * The comparison is based on the equality of the keys extracted by the key extractor function.
     * <br />
     * The first row encountered with a particular key value is retained, and subsequent rows with the same key value are removed.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"), 
     *     new Object[][] {
     *         {1, "John Doe", "IT"},
     *         {2, "Jane Smith", "HR"},
     *         {3, "Johnathan Doe", "Finance"},  // Duplicate based on last name
     *         {4, "Bob Brown", "IT"}
     *     });
     * dataset.removeDuplicateRowsBy("name", name -> ((String) name).split(" ")[1]); // Use last name as key
     * // Result: Only rows with id=1, id=2, and id=4 remain
     * // The row with id=3 is removed because "Doe" already exists
     * }</pre>
     *
     * @param keyColumnName The name of the column to use for identifying duplicates. It should be a name that exists in the Dataset.
     * @param keyExtractor The function to extract the key from the column value. It takes the column value as input and returns the key used for comparison.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     * @see #removeDuplicateRowsBy(String)
     * @see #removeDuplicateRowsBy(Collection)
     * @see #distinctBy(String, Function)
     */
    void removeDuplicateRowsBy(String keyColumnName, Function<?, ?> keyExtractor) throws IllegalStateException, IllegalArgumentException;

    /**
     * Removes duplicate rows from the Dataset based on values in the specified key columns.
     * <br />
     * This method identifies rows that have duplicate values across the specified columns and removes all but the first occurrence.
     * The comparison is based on the natural equality of the values in the key columns.
     * <br />
     * The first row encountered with a particular combination of key values is retained, and subsequent rows with the same combination are removed.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"), 
     *     new Object[][] {
     *         {1, "John", "IT"},
     *         {2, "Jane", "HR"},
     *         {3, "John", "IT"},  // Duplicate name and department
     *         {4, "Bob", "IT"}
     *     });
     * dataset.removeDuplicateRowsBy(Arrays.asList("name", "department"));
     * // Result: Only rows with id=1, id=2, and id=4 remain
     * // The row with id=3 is removed because ("John", "IT") already exists
     * }</pre>
     *
     * @param keyColumnName A collection containing the names of the columns to use for identifying duplicates. These should be names that exist in the Dataset.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or if {@code keyColumnName} is empty.
     * @see #removeDuplicateRowsBy(Collection, Function)
     * @see #removeDuplicateRowsBy(String)
     * @see #distinctBy(Collection)
     */
    void removeDuplicateRowsBy(Collection<String> keyColumnName) throws IllegalStateException, IllegalArgumentException;

    /**
     * Removes duplicate rows from the Dataset based on values in the specified key columns and a custom key extractor function.
     * <br />
     * This method identifies rows that have duplicate values across the specified columns, as determined by the provided key extractor function, and removes all but the first occurrence.
     * The comparison is based on the equality of the keys extracted by the key extractor function.
     * <br />
     * The first row encountered with a particular combination of key values is retained, and subsequent rows with the same combination are removed.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"), 
     *     new Object[][] {
     *         {1, "John Doe", "IT"},
     *         {2, "Jane Smith", "HR"},
     *         {3, "Johnathan Doe", "IT"},  // Duplicate based on last name and department
     *         {4, "Bob Brown", "IT"}
     *     });
     * dataset.removeDuplicateRowsBy(Arrays.asList("name", "department"), 
     *     row -> ((String) row.get(0)).split(" ")[1] + "|" + row.get(1)); // Use last name and department as key
     * // Result: Only rows with id=1, id=2, and id=4 remain
     * // The row with id=3 is removed because ("Doe", "IT") already exists
     * }</pre>
     *
     * @param keyColumnNames A collection containing the names of the columns to use for identifying duplicates. These should be names that exist in the Dataset.
     * @param keyExtractor The function to extract the key from a DisposableObjArray of column values. It takes a DisposableObjArray representing the values in the specified columns for a particular row and returns the key used for comparison.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset, if {@code keyColumnNames} is empty.
     * @see #removeDuplicateRowsBy(Collection)
     * @see #removeDuplicateRowsBy(String)
     * @see #distinctBy(Collection, Function)
     */
    void removeDuplicateRowsBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Updates a specific row in the Dataset.
     * <br />
     * The update is performed by applying a function to each value in the specified row. The function takes the current value and returns the new value.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("name", "status"), data);
     * dataset.updateRow(0, value -> value instanceof String ? ((String) value).toUpperCase() : value);
     * }</pre>
     *
     * @param rowIndex The index of the row to be updated. It should be a valid index within the current row range.
     * @param func The function to be applied to each value in the row. It takes the current value and returns the new value.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     */
    void updateRow(int rowIndex, Function<?, ?> func) throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Updates the values in the specified rows of the Dataset.
     * <br />
     * The update is performed by applying a function to each value in the specified rows. The function takes the current value and returns the new value.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.updateRows(new int[] {0}, value -> ((Integer) value) + 10);
     * }</pre>
     *
     * @param rowIndexesToUpdate An array of integers representing the indices of the rows to be updated. Each index should be a valid index within the current row range.
     * @param func The function to be applied to each value in the rows. It takes the current value and returns the new value.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IndexOutOfBoundsException if any of the specified indices is out of bounds.
     */
    void updateRows(int[] rowIndexesToUpdate, Function<?, ?> func) throws IllegalStateException, IndexOutOfBoundsException;

    // TODO should the method name be "replaceAll"? If change the method name to replaceAll, what about updateColumn/updateRow?

    /**
     * Updates all the values in the Dataset.
     * <br />
     * The update is performed by applying a function to each value in the Dataset. The function takes the current value and returns the new value.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.updateAll(value -> ((Integer) value) + 10);
     * }</pre>
     *
     * @param func The function to be applied to each value in the Dataset. It takes the current value and returns the new value.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     */
    void updateAll(Function<?, ?> func) throws IllegalStateException;

    /**
     * Replaces values in the Dataset that satisfy a specified condition with a new value.
     * <br />
     * The predicate takes each value in the Dataset as input and returns a boolean indicating whether the value should be replaced.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.replaceIf("id", value -> (Integer) value < 2, 99);
     * }</pre>
     *
     * @param predicate The predicate to test each value in the sheet. It takes a value from the Dataset as input and returns a boolean indicating whether the value should be replaced.
     * @param newValue The new value to replace the values that satisfy the condition.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     */
    void replaceIf(Predicate<?> predicate, Object newValue) throws IllegalStateException;

    /**
     * Prepends the provided Dataset to the current Dataset.
     * <br />
     * The operation is performed by adding all rows from the provided Dataset to the beginning of the current Dataset.
     * The structure (columns and their types) of the provided Dataset should match the structure of the current Dataset.
     * The properties from the provided Dataset will also be merged into the current Dataset, with the properties from the provided Dataset taking precedence in case of conflicts.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name"),
     *     new Object[][] {
     *          {1, "Alice"},
     *          {2, "Bob"}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name"),
     *     new Object[][] {
     *          {3, "Charlie"},
     *          {4, "Diana"}
     *     });
     *
     * dataset1.prepend(dataset2);
     * // dataset1 now contains: {3, "Charlie"}, {4, "Diana"}, {1, "Alice"}, {2, "Bob"}
     * }</pre>
     *
     * @param other The Dataset to be prepended to the current Dataset. It should have the same structure as the current Dataset.
     * @throws IllegalStateException if the current Dataset is frozen (read-only).
     * @throws IllegalArgumentException if this Dataset and the provided Dataset don't have the same column names or if the other Dataset is {@code null}.
     * @see #append(Dataset)
     * @see #merge(Dataset)
     * @see #union(Dataset)
     * @see #unionAll(Dataset)
     */
    void prepend(Dataset other) throws IllegalStateException, IllegalArgumentException;

    /**
     * Appends the provided Dataset to the current Dataset.
     * <br />
     * The operation is performed by adding all rows from the provided Dataset to the end of the current Dataset.
     * The structure (columns and their types) of the provided Dataset should match the structure of the current Dataset.
     * The properties from the provided Dataset will also be merged into the current Dataset, with the properties from the provided Dataset taking precedence in case of conflicts.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name"), 
     *     new Object[][] {
     *          {1, "Alice"},
     *          {2, "Bob"}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name"), 
     *     new Object[][] {
     *          {3, "Charlie"},
     *          {4, "Diana"}
     *     });
     * 
     * dataset1.append(dataset2); 
     * // dataset1 now contains: {1, "Alice"}, {2, "Bob"}, {3, "Charlie"}, {4, "Diana"}
     * }</pre>
     *
     * @param other The Dataset to be appended to the current Dataset. It should have the same structure as the current Dataset.
     * @throws IllegalStateException if the current Dataset is frozen (read-only).
     * @throws IllegalArgumentException if this Dataset and the provided Dataset don't have the same column names or if the other Dataset is {@code null}.
     * @see #prepend(Dataset)
     * @see #merge(Dataset)
     * @see #union(Dataset)
     * @see #unionAll(Dataset)
     */
    void append(Dataset other) throws IllegalStateException, IllegalArgumentException;

    /**
     * Merges all the rows and columns from another Dataset into this Dataset.
     * <br />
     * If there are columns in the other Dataset that are not present in this Dataset, they will be added to this Dataset with null values for rows from this Dataset.
     * If there are columns in this Dataset that are not present in the other Dataset, they will also be included with null values for rows from the other Dataset.
     * Duplicated rows in the resulting Dataset will NOT be eliminated.
     * The properties from the other Dataset will also be merged into this Dataset, with the properties from the other Dataset taking precedence in case of conflicts.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {3, "Charlie", 85}
     *     });
     *
     *  dataset1.merge(dataset2);
     * // dataset1 now contains columns: id, name, age, score
     * // dataset1 now contains rows: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {1, "Alice", null, 95}, {3, "Charlie", null, 85}
     * }</pre>
     *
     * @param other The Dataset to merge with this Dataset
     * @throws IllegalStateException if this Dataset is frozen (read-only)
     * @throws IllegalArgumentException if the other Dataset is {@code null}
     * @see #merge(Dataset, boolean)
     * @see #prepend(Dataset)
     * @see #append(Dataset)
     * @see #union(Dataset)
     * @see #unionAll(Dataset)
     */
    void merge(Dataset other) throws IllegalStateException, IllegalArgumentException;

    /**
     * Merges all the rows and columns from another Dataset into this Dataset with an option to require the same columns.
     * <br />
     * If there are columns in the other Dataset that are not present in this Dataset, they will be added to this Dataset with null values for rows from this Dataset.
     * If there are columns in this Dataset that are not present in the other Dataset, they will also be included with null values for rows from the other Dataset.
     * Duplicated rows in the resulting Dataset will NOT be eliminated.
     * The properties from the other Dataset will also be merged into this Dataset, with the properties from the other Dataset taking precedence in case of conflicts.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {3, "Charlie", 85}
     *     });
     *
     *  dataset1.merge(dataset2);
     * // dataset1 now contains columns: id, name, age, score
     * // dataset1 now contains rows: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {1, "Alice", null, 95}, {3, "Charlie", null, 85}
     * }</pre>
     *
     * @param other The Dataset to merge with this Dataset. Must not be {@code null}.
     * @param requiresSameColumns A boolean value that determines whether the merge operation requires both Datasets to have the same columns. 
     *                           If {@code true}, both Datasets must have identical column structures. If {@code false}, columns from both Datasets are combined.
     * @throws IllegalStateException if this Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the other Dataset is {@code null}, or if {@code requiresSameColumns} is {@code true} and the Datasets do not have the same columns.
     * @see #merge(Dataset)
     * @see #prepend(Dataset)
     * @see #append(Dataset)
     * @see #union(Dataset, boolean)
     * @see #unionAll(Dataset, boolean)
     */
    void merge(Dataset other, boolean requiresSameColumns) throws IllegalStateException, IllegalArgumentException;

    /**
     * Merges selected columns from another Dataset into this Dataset.
     * <br />
     * If there are selected columns in the other Dataset that are not present in this Dataset, they will be added to this Dataset with null values for rows from this Dataset.
     * If there are columns in this Dataset that are not present in the other Dataset, they will also be included with null values for rows from the other Dataset.
     * All rows from the other Dataset will be added to this Dataset, but only with values from the specified columns (other columns will have null values).
     * Duplicated rows in the resulting Dataset will NOT be eliminated.
     * The properties from the other Dataset will also be merged into this Dataset, with the properties from the other Dataset taking precedence in case of conflicts.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score", "grade"),
     *     new Object[][] {
     *          {1, "Alice", 95, "A"},
     *          {3, "Charlie", 85, "B"}
     *     });
     *
     * Collection<String> selectedColumns = Arrays.asList("id", "score");
     * dataset1.merge(dataset2, selectedColumns);
     * // dataset1 now contains columns: id, name, age, score
     * // dataset1 now contains rows: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {1, null, null, 95}, {3, null, null, 85}
     * }</pre>
     *
     * @param other The Dataset to merge selected columns from. Must not be {@code null}.
     * @param selectColumnNamesFromOtherToMerge The collection of column names to select from the other Dataset for merging. Must not be {@code null} or empty.
     * @throws IllegalStateException if this Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the other Dataset is {@code null}, or if {@code selectColumnNamesFromOtherToMerge} is {@code null} or empty, or if any of the specified column names doesn't exist in the other Dataset.
     * @see #merge(Dataset)
     * @see #merge(Dataset, boolean)
     * @see #append(Dataset)
     */
    @Beta
    void merge(Dataset other, Collection<String> selectColumnNamesFromOtherToMerge) throws IllegalStateException, IllegalArgumentException;

    //    /**
    //     * Merges this Dataset with another Dataset using rows from the specified range.
    //     * <br />
    //     * The merge operation combines all rows from this Dataset with rows from the other Dataset within the specified row range.
    //     * If there are columns in the other Dataset that are not present in this Dataset, they will be added to the new Dataset.
    //     * If there are columns in this Dataset that are not present in the other Dataset, they will also be included in the new Dataset.
    //     * Duplicated rows in the resulting Dataset will not be eliminated.
    //     * The resulting Dataset also contains the properties from both Datasets.
    //     * <br />
    //     * {@code merge(Dataset, int, int)} is similar to {@code merge(other.copy(int, int)}.
    //     *
    //     * <p><b>Example:</b></p>
    //     * <pre>{@code
    //     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
    //     *     new Object[][] {
    //     *          {1, "Alice", 25},
    //     *          {2, "Bob", 30}
    //     *     });
    //     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
    //     *     new Object[][] {
    //     *          {1, "Alice", 95},
    //     *          {2, "Bob", 90},
    //     *          {3, "Charlie", 85}
    //     *     });
    //     *
    //     * Dataset result = dataset1.merge(dataset2, 0, 2);
    //     * // Result contains columns: id, name, age, score
    //     * // Result contains: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {1, "Alice", null, 95}, {2, "Bob", null, 90}
    //     * }</pre>
    //     *
    //     * @param other The Dataset to merge with this Dataset. Must not be {@code null}.
    //     * @param fromRowIndexFromOther The starting index (inclusive) of the row range from the other Dataset to be included in the merge operation.
    //     * @param toRowIndexFromOther The ending index (exclusive) of the row range from the other Dataset to be included in the merge operation.
    //     * @return A new Dataset that is the result of the merge operation with all rows from this Dataset and the specified row range from the other Dataset.
    //     * @throws IndexOutOfBoundsException if the {@code fromRowIndex} or {@code toRowIndex} is out of bounds for the other Dataset.
    //     * @throws IllegalArgumentException if the other Dataset is {@code null}.
    //     * @see #merge(Dataset)
    //     * @see #merge(Dataset, boolean)
    //     * @see #merge(Dataset, Collection)
    //     * @see #prepend(Dataset)
    //     * @see #append(Dataset)
    //     * @see #union(Dataset)
    //     * @see #unionAll(Dataset)
    //     */
    //    @Beta
    //    Dataset merge(Dataset other, int fromRowIndexFromOther, int toRowIndexFromOther) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Merges selected columns from a specified row range of another Dataset into this Dataset.
     * <br />
     * If there are selected columns in the other Dataset that are not present in this Dataset, they will be added to this Dataset with null values for rows from this Dataset.
     * If there are columns in this Dataset that are not present in the other Dataset, they will also be included with null values for rows from the other Dataset.
     * Only rows within the specified range from the other Dataset will be added to this Dataset, and only with values from the specified columns (other columns will have null values).
     * The properties from the other Dataset will also be merged into this Dataset, with the properties from the other Dataset taking precedence in case of conflicts.
     * <br />
     *
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score", "grade"),
     *     new Object[][] {
     *          {1, "Alice", 95, "A"},
     *          {2, "Bob", 90, "B"},
     *          {3, "Charlie", 85, "C"}
     *     });
     *
     * Collection<String> selectedColumns = Arrays.asList("id", "score");
     * dataset1.merge(dataset2, 0, 2, selectedColumns);
     * // dataset1 now contains columns: id, name, age, score
     * // dataset1 now contains rows: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {1, null, null, 95}, {2, null, null, 90}
     * }</pre>
     *
     * @param other The Dataset to merge selected columns from. Must not be {@code null}.
     * @param fromRowIndexFromOther The starting index (inclusive) of the row range from the other Dataset to be included in the merge operation.
     * @param toRowIndexFromOther The ending index (exclusive) of the row range from the other Dataset to be included in the merge operation.
     * @param selectColumnNamesFromOtherToMerge The collection of column names to select from the other Dataset for merging. Must not be {@code null} or empty.
     * @throws IllegalStateException if this Dataset is frozen (read-only).
     * @throws IndexOutOfBoundsException if the {@code fromRowIndexFromOther} or {@code toRowIndexFromOther} is out of bounds for the other Dataset.
     * @throws IllegalArgumentException if the other Dataset is {@code null}, or if {@code selectColumnNamesFromOtherToMerge} is {@code null} or empty, or if any of the specified column names doesn't exist in the other Dataset.
     * @see #merge(Dataset)
     * @see #merge(Dataset, boolean)
     * @see #merge(Dataset, Collection)
     * @see #append(Dataset)
     */
    @Beta
    void merge(Dataset other, int fromRowIndexFromOther, int toRowIndexFromOther, Collection<String> selectColumnNamesFromOtherToMerge)
            throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException;

    //    /**
    //     * Merges this Dataset with a collection of other Datasets.
    //     * <br />
    //     * The merge operation combines rows from all Datasets into a new Dataset.
    //     * All columns from all Datasets will be included in the new Dataset.
    //     * If there are columns that are not present in some Datasets, the corresponding values in the new Dataset will be {@code null}.
    //     * Duplicated rows in the resulting Dataset will not be eliminated.
    //     * The resulting Dataset also contains the properties from both Datasets.
    //     * <br />
    //     * {@link #merge(Collection)} is equivalent to calling {@link #merge(Dataset)} multiple times for each Dataset in the collection.
    //     *
    //     * <p><b>Example:</b></p>
    //     * <pre>{@code
    //     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name"),
    //     *     new Object[][] {
    //     *          {1, "Alice"},
    //     *          {2, "Bob"}
    //     *     });
    //     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "age"),
    //     *     new Object[][] {
    //     *          {3, 25},
    //     *          {4, 30}
    //     *     });
    //     * Dataset dataset3 = Dataset.rows(Arrays.asList("name", "score"),
    //     *     new Object[][] {
    //     *          {"Charlie", 95},
    //     *          {"David", 90}
    //     *     });
    //     *
    //     * Dataset result = dataset1.merge(Arrays.asList(dataset2, dataset3));
    //     * // Result contains columns: id, name, age, score
    //     * // Result contains: {1, "Alice", null, null}, {2, "Bob", null, null}, 
    //     * //                  {3, null, 25, null}, {4, null, 30, null},
    //     * //                  {null, "Charlie", null, 95}, {null, "David", null, 90}
    //     * }</pre>
    //     *
    //     * @param others The collection of Datasets to merge with this Dataset. May be {@code null} or empty.
    //     * @return A new Dataset that is the result of the merge operation with all rows from this Dataset and all rows from the other Datasets, or a copy of this Dataset if the {@code others} collection is {@code null} or empty.
    //     * @see #merge(Dataset)
    //     * @see #merge(Dataset, boolean)
    //     * @see #merge(Dataset, Collection)
    //     * @see #merge(Dataset, int, int)
    //     * @see #merge(Dataset, int, int, Collection)
    //     * @see #prepend(Dataset)
    //     * @see #append(Dataset)
    //     * @see #union(Dataset)
    //     * @see #unionAll(Dataset)
    //     */
    //    @Beta
    //    Dataset merge(final Collection<? extends Dataset> others);
    //
    //    /**
    //     * Merges this Dataset with a collection of other Datasets with an option to require same columns.
    //     * <br />
    //     * The merge operation combines rows from all Datasets into a new Dataset.
    //     * All columns from all Datasets will be included in the new Dataset.
    //     * If there are columns that are not present in some Datasets, the corresponding values in the new Dataset will be {@code null}.
    //     * The rows from all Datasets will be included in the new Dataset, even if they have the same values.
    //     * The resulting Dataset also contains the properties from all Datasets.
    //     * <br />
    //     * If {@code requiresSameColumns} is {@code true}, all Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
    //     * {@link #merge(Collection, boolean)} is equivalent to calling {@link #merge(Dataset, boolean)} multiple times for each Dataset in the collection.
    //     *
    //     * <p><b>Example:</b></p>
    //     * <pre>{@code
    //     * <pre>{@code
    //     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name"),
    //     *     new Object[][] {
    //     *          {1, "Alice"},
    //     *          {2, "Bob"}
    //     *     });
    //     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "age"),
    //     *     new Object[][] {
    //     *          {3, 25},
    //     *          {4, 30}
    //     *     });
    //     * Dataset dataset3 = Dataset.rows(Arrays.asList("name", "score"),
    //     *     new Object[][] {
    //     *          {"Charlie", 95},
    //     *          {"David", 90}
    //     *     });
    //     *
    //     * // throw IllegalArgumentException if requiresSameColumns is true
    //     * dataset1.merge(Arrays.asList(dataset2, dataset3), true);
    //     * 
    //     * Dataset result = dataset1.merge(Arrays.asList(dataset2, dataset3), false);
    //     * // Result contains columns: id, name, age, score
    //     * // Result contains: {1, "Alice", null, null}, {2, "Bob", null, null}, 
    //     * //                  {3, null, 25, null}, {4, null, 30, null},
    //     * //                  {null, "Charlie", null, 95}, {null, "David", null, 90}
    //     * }</pre>
    //     *
    //     * @param others The collection of Datasets to merge with this Dataset. May be {@code null} or empty.
    //     * @param requiresSameColumns A boolean that indicates whether all Datasets should have the same columns.
    //     * @return A new Dataset that is the result of the merge operation with all rows from this Dataset and all rows from the other Datasets, or a copy of this Dataset if the {@code others} collection is {@code null} or empty.
    //     * @throws IllegalArgumentException if {@code requiresSameColumns} is {@code true} and the Datasets do not have the same columns.
    //     * @see #merge(Dataset)
    //     * @see #merge(Dataset, boolean)
    //     * @see #merge(Dataset, Collection)
    //     * @see #merge(Dataset, int, int)
    //     * @see #merge(Dataset, int, int, Collection)
    //     * @see #merge(Collection)
    //     * @see #prepend(Dataset)
    //     * @see #append(Dataset)
    //     * @see #union(Dataset)
    //     * @see #unionAll(Dataset)
    //     */
    //    @Beta
    //    Dataset merge(final Collection<? extends Dataset> others, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Retrieves the current row number in the Dataset.
     * <br />
     * This method is typically used when iterating over the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * int pointer = dataset.currentRowNum();
     * }</pre>
     *
     * @return The current row number as an integer. The first row is number 0.
     */
    int currentRowNum();

    /**
     * Moves the cursor to the row in this Dataset object specified by the given index.
     * <br />
     * This method is typically used when navigating through the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * String name = dataset.absolute(1).get("name");
     * }</pre>
     *
     * @param rowIndex The index of the row to move to. The first row is 0, the second is 1, and so on.
     * @return The Dataset object itself with the cursor moved to the specified row.
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     */
    Dataset absolute(int rowIndex);

    /**
     * Retrieves a row from the Dataset as an array of Objects.
     * <br />
     * This method is typically used when accessing the data in a specific row.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * Object[] firstRow = dataset.getRow(0); // Gets first row as Object[]
     * }</pre>
     *
     * @param rowIndex The index of the row to retrieve. The first row is 0, the second is 1, and so on.
     * @return An array of Objects representing the data in the specified row.
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     */
    Object[] getRow(int rowIndex) throws IndexOutOfBoundsException;

    /**
     * Retrieves a row from the Dataset and converts it to a specific type.
     * <br />
     * This method is typically used when accessing the data in a specific row and converting it to a specific type.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * ImmutableList<Object> row = dataset.getRow(0);
     * }</pre>
     *
     * @param rowIndex The index of the row to retrieve. The first row is 0, the second is 1, and so on.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @param <T> The type to which the row data should be converted.
     * @return An instance of the specified type representing the data in the specified row.
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> T getRow(int rowIndex, Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Retrieves a row from the Dataset as an instance of the specified type.
     * <br />
     * This method is typically used when accessing the data in a specific row and converting it to a specific type.
     * The type can be an array, List, Set, Map, or a Bean.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * ImmutableList<Object> row = dataset.getRow(0);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param rowIndex The index of the row to retrieve. The first row is 0, the second is 1, and so on.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @param columnNames the column names to include in the returned row view
     * @return An instance of the specified type representing the data in the specified row.
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> T getRow(int rowIndex, Collection<String> columnNames, Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Retrieves a row from the Dataset as an instance of the specified type.
     * <br />
     * This method is typically used when accessing the data in a specific row and converting it to a specific type.
     * The type is determined by the provided IntFunction. It must be Object[], Collection, Map, or Bean class.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * ImmutableList<Object> row = dataset.getRow(0);
     * }</pre>
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
     * Retrieves a row from the Dataset as an instance of the specified type.
     * <br />
     * This method is typically used when accessing the data in a specific row and converting it to a specific type.
     * The type is determined by the provided IntFunction. It must be Object[], Collection, Map, or Bean class.
     * Only the columns specified in the {@code columnNames} collection will be included in the returned row.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * ImmutableList<Object> row = dataset.getRow(0);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param rowIndex The index of the row to retrieve. The first row is 0, the second is 1, and so on.
     * @param columnNames The collection of column names to be included in the returned row.
     * @param rowSupplier The IntFunction that generates an instance of the target type. It takes an integer as input, which is the number of columns in the row, and returns an instance of the target type.
     * @return An instance of the specified type representing the data in the specified row.
     * @throws IndexOutOfBoundsException if the specified {@code rowIndex} is out of bounds.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class, or if any of the specified {@code columnNames} does not exist in the Dataset.
     */
    <T> T getRow(int rowIndex, Collection<String> columnNames, IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Retrieves the first row from the Dataset as an Optional array of Objects.
     * <br />
     * This method is typically used when you need to access the first row of data in the Dataset.
     * If the Dataset is empty, the returned Optional will be empty.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * ImmutableList<Object> first = dataset.firstRow();
     * }</pre>
     *
     * @return An Optional array of Objects representing the data in the first row. If the Dataset is empty, the Optional will be empty.
     */
    Optional<Object[]> firstRow();

    /**
     * Retrieves the first row from the Dataset as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the first row of data in the Dataset and convert it to a specific type.
     * The type can be an Object[], Collection, Map, or a Bean class.
     * If the Dataset is empty, the returned Optional will be empty.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * ImmutableList<Object> first = dataset.firstRow();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return An Optional instance of the specified type representing the data in the first row. If the Dataset is empty, the Optional will be empty.
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> firstRow(Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Retrieves the first row from the Dataset as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the first row of data in the Dataset and convert it to a specific type.
     * The type is determined by the provided Class object. It can be an Object[], Collection, Map, or a Bean class.
     * Only the columns specified in the {@code columnNames} collection will be included in the returned row.
     * If the Dataset is empty, the returned Optional will be empty.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * ImmutableList<Object> first = dataset.firstRow();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the returned row.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return An Optional instance of the specified type representing the data in the first row. If the Dataset is empty, the Optional will be empty.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> firstRow(Collection<String> columnNames, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Retrieves the first row from the Dataset as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the first row of data in the Dataset and convert it to a specific type.
     * The type is determined by the provided IntFunction. It can be an Object[], Collection, Map, or a Bean class.
     * If the Dataset is empty, the returned Optional will be empty.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * ImmutableList<Object> first = dataset.firstRow();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param rowSupplier The IntFunction that generates an instance of the target type. It takes an integer as input, which is the number of columns in the row, and returns an instance of the target type.
     * @return An Optional instance of the specified type representing the data in the first row. If the Dataset is empty, the Optional will be empty.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> firstRow(IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Retrieves the first row from the Dataset as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the first row of data in the Dataset and convert it to a specific type.
     * The type is determined by the provided IntFunction. It can be an Object[], Collection, Map, or a Bean class.
     * Only the columns specified in the {@code columnNames} collection will be included in the returned row.
     * If the Dataset is empty, the returned Optional will be empty.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * ImmutableList<Object> first = dataset.firstRow();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the returned row.
     * @param rowSupplier The IntFunction that generates an instance of the target type. It takes an integer as input, which is the number of columns in the row, and returns an instance of the target type.
     * @return An Optional instance of the specified type representing the data in the first row. If the Dataset is empty, the Optional will be empty.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> firstRow(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Retrieves the last row from the Dataset as an Optional array of Objects.
     * <br />
     * This method is typically used when you need to access the last row of data in the Dataset.
     * If the Dataset is empty, the returned Optional will be empty.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * ImmutableList<Object> last = dataset.lastRow();
     * }</pre>
     *
     * @return An Optional array of Objects representing the data in the last row. If the Dataset is empty, the Optional will be empty.
     */
    Optional<Object[]> lastRow();

    /**
     * Retrieves the last row from the Dataset as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the last row of data in the Dataset and convert it to a specific type.
     * The type can be an Object[], Collection, Map, or a Bean class.
     * If the Dataset is empty, the returned Optional will be empty.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * ImmutableList<Object> last = dataset.lastRow();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return An Optional instance of the specified type representing the data in the last row. If the Dataset is empty, the Optional will be empty.
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> lastRow(Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Retrieves the last row from the Dataset as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the last row of data in the Dataset and convert it to a specific type.
     * The type can be an Object[], Collection, Map, or a Bean class.
     * Only the columns specified in the {@code columnNames} collection will be included in the returned row.
     * If the Dataset is empty, the returned Optional will be empty.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * ImmutableList<Object> last = dataset.lastRow();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the returned row.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return An Optional instance of the specified type representing the data in the last row. If the Dataset is empty, the Optional will be empty.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> lastRow(Collection<String> columnNames, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Retrieves the last row from the Dataset as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the last row of data in the Dataset and convert it to a specific type.
     * The type is determined by the provided IntFunction. It can be an Object[], Collection, Map, or a Bean class.
     * If the Dataset is empty, the returned Optional will be empty.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * ImmutableList<Object> last = dataset.lastRow();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param rowSupplier The IntFunction that generates an instance of the target type. It takes an integer as input, which is the number of columns in the row, and returns an instance of the target type.
     * @return An Optional instance of the specified type representing the data in the last row. If the Dataset is empty, the Optional will be empty.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> lastRow(IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Retrieves the last row from the Dataset as an instance of the specified type wrapped in an Optional.
     * <br />
     * This method is typically used when you need to access the last row of data in the Dataset and convert it to a specific type.
     * The type is determined by the provided IntFunction. It can be an Object[], Collection, Map, or a Bean class.
     * Only the columns specified in the {@code columnNames} collection will be included in the returned row.
     * If the Dataset is empty, the returned Optional will be empty.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * ImmutableList<Object> last = dataset.lastRow();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the returned row.
     * @param rowSupplier The IntFunction that generates an instance of the target type. It takes an integer as input, which is the number of columns in the row, and returns an instance of the target type.
     * @return An Optional instance of the specified type representing the data in the last row. If the Dataset is empty, the Optional will be empty.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Optional<T> lastRow(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Performs the given action for each row of the Dataset until all rows.
     * <br />
     * This method is typically used when you need to perform an operation on each row in the Dataset.
     * The action is a Consumer function that takes a DisposableObjArray as input, which represents a row in the Dataset.
     * The action is applied to each row in the Dataset in the order they appear.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * dataset.forEach(row -> System.out.println("Row: " + Arrays.toString(row.toArray())));
     * }</pre>
     *
     * @param <E> The type of the exception that the action can throw.
     * @param action The action to be performed on each row. It takes a DisposableObjArray as input, which represents a row in the Dataset. The action should not cache or update the input DisposableObjArray or its values(Array).
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(Throwables.Consumer<? super DisposableObjArray, E> action) throws E;

    /**
     * Performs the given action for each row of the Dataset until all rows.
     * <br />
     * This method is typically used when you need to perform an operation on each row in the Dataset.
     * The action is a Consumer function that takes a DisposableObjArray as input, which represents a row in the Dataset.
     * The action is applied to each row in the Dataset in the order they appear.
     * Only the columns specified in the {@code columnNames} collection will be included in the DisposableObjArray.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * dataset.forEach(Arrays.asList("name", "age"), row -> System.out.println("Name-Age: " + Arrays.toString(row.toArray())));
     * }</pre>
     *
     * @param <E> The type of the exception that the action can throw.
     * @param columnNames The collection of column names to be included in the DisposableObjArray.
     * @param action The action to be performed on each row. It takes a DisposableObjArray as input, which represents a row in the Dataset. The action should not cache or update the input DisposableObjArray or its values(Array).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(Collection<String> columnNames, Throwables.Consumer<? super DisposableObjArray, E> action)
            throws IllegalArgumentException, E;

    /**
     * Performs the given action for each row of the Dataset within the specified range until all rows.
     * <br />
     * This method is typically used when you need to perform an operation on a specific range of rows in the Dataset.
     * The action is a Consumer function that takes a DisposableObjArray as input, which represents a row in the Dataset.
     * The action is applied to each row in the Dataset in the order they appear, starting from the row at the index specified by {@code fromRowIndex} and ending at the row before the index specified by {@code toRowIndex}.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.forEach(row -> System.out.println(row.get(0)));
     * }</pre>
     *
     * @param <E> The type of the exception that the action can throw.
     * @param fromRowIndex The starting index of the range of rows to be processed. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be processed. This index is exclusive, meaning the row at this index will not be processed.
     * @param action The action to be performed on each row. It takes a DisposableObjArray as input, which represents a row in the Dataset. The action should not cache or update the input DisposableObjArray or its values(Array).
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Throwables.Consumer<? super DisposableObjArray, E> action)
            throws IndexOutOfBoundsException, E;

    /**
     * Performs the given action for each row of the Dataset within the specified range.
     * <br />
     * This method is typically used when you need to perform an operation on a specific range of rows in the Dataset.
     * The action is a Consumer function that takes a DisposableObjArray as input, which represents a row in the Dataset.
     * The action is applied to each row in the Dataset in the order they appear, starting from the row at the index specified by {@code fromRowIndex} and ending at the row before the index specified by {@code toRowIndex}.
     * Only the columns specified in the {@code columnNames} collection will be included in the DisposableObjArray.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.forEach(row -> System.out.println(row.get(0)));
     * }</pre>
     *
     * @param <E> The type of the exception that the action can throw.
     * @param fromRowIndex The starting index of the range of rows to be processed. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be processed. This index is exclusive, meaning the row at this index will not be processed.
     * @param columnNames The collection of column names to be included in the DisposableObjArray.
     * @param action The action to be performed on each row. It takes a DisposableObjArray as input, which represents a row in the Dataset. The action should not cache or update the input DisposableObjArray or its values(Array).
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Collection<String> columnNames,
            Throwables.Consumer<? super DisposableObjArray, E> action) throws IndexOutOfBoundsException, IllegalArgumentException, E;

    /**
     * Performs the given action for each row of the Dataset.
     * <br />
     * This method is typically used when you need to perform an operation on each row in the Dataset.
     * The action is a BiConsumer function that takes two inputs, which represent the values of the two columns specified in the Tuple {@code columnNames}.
     * The action is applied to each row in the Dataset in the order they appear.
     * Only the columns specified in the Tuple {@code columnNames} will be included in the action.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("name", "age", "salary"), data);
     * dataset.forEach(Tuple.of("name", "salary"), (name, salary) -> System.out.println(name + ": $" + salary));
     * }</pre>
     *
     * @param <E> The type of the exception that the action can throw.
     * @param columnNames A Tuple2 representing the names of the two columns to be included in the action.
     * @param action The action to be performed on each row. It takes two inputs, which represent the values of the two columns specified in the Tuple {@code columnNames}. The action should not cache or update the input values.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(Tuple2<String, String> columnNames, Throwables.BiConsumer<?, ?, E> action) throws IllegalArgumentException, E;

    /**
     * Performs the given action for each row of the Dataset within the specified range.
     * <br />
     * This method is typically used when you need to perform an operation on a specific range of rows in the Dataset.
     * The action is a BiConsumer function that takes two inputs, which represent the values of the two columns specified in the Tuple {@code columnNames}.
     * The action is applied to each row in the Dataset in the order they appear, starting from the row at the index specified by {@code fromRowIndex} and ending at the row before the index specified by {@code toRowIndex}.
     * Only the columns specified in the Tuple {@code columnNames} will be included in the action.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.forEach(row -> System.out.println(row.get(0)));
     * }</pre>
     *
     * @param <E> The type of the exception that the action can throw.
     * @param fromRowIndex The starting index of the range of rows to be processed. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be processed. This index is exclusive, meaning the row at this index will not be processed.
     * @param columnNames A Tuple2 representing the names of the two columns to be included in the action.
     * @param action The action to be performed on each row. It takes two inputs, which represent the values of the two columns specified in the Tuple {@code columnNames}. The action should not cache or update the input values.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, Throwables.BiConsumer<?, ?, E> action)
            throws IndexOutOfBoundsException, IllegalArgumentException, E;

    /**
     * Performs the given action for each row of the Dataset.
     * <br />
     * This method is typically used when you need to perform an operation on each row in the Dataset.
     * The action is a TriConsumer function that takes three inputs, which represent the values of the three columns specified in the Tuple {@code columnNames}.
     * The action is applied to each row in the Dataset in the order they appear.
     * Only the columns specified in the Tuple {@code columnNames} will be included in the action.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.forEach(row -> System.out.println(row.get(0)));
     * }</pre>
     *
     * @param <E> The type of the exception that the action can throw.
     * @param columnNames A Tuple3 representing the names of the three columns to be included in the action.
     * @param action The action to be performed on each row. It takes three inputs, which represent the values of the three columns specified in the Tuple {@code columnNames}. The action should not cache or update the input values.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(Tuple3<String, String, String> columnNames, Throwables.TriConsumer<?, ?, ?, E> action)
            throws IllegalArgumentException, E;

    /**
     * Performs the given action for each row of the Dataset within the specified range.
     * <br />
     * This method is typically used when you need to perform an operation on a specific range of rows in the Dataset.
     * The action is a TriConsumer function that takes three inputs, which represent the values of the three columns specified in the Tuple {@code columnNames}.
     * The action is applied to each row in the Dataset in the order they appear, starting from the row at the index specified by {@code fromRowIndex} and ending at the row before the index specified by {@code toRowIndex}.
     * Only the columns specified in the Tuple {@code columnNames} will be included in the action.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * dataset.forEach(row -> System.out.println(row.get(0)));
     * }</pre>
     *
     * @param <E> The type of the exception that the action can throw.
     * @param fromRowIndex The starting index of the range of rows to be processed. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be processed. This index is exclusive, meaning the row at this index will not be processed.
     * @param columnNames A Tuple3 representing the names of the three columns to be included in the action.
     * @param action The action to be performed on each row. It takes three inputs, which represent the values of the three columns specified in the Tuple {@code columnNames}. The action should not cache or update the input values.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset.
     * @throws E if the action throws an exception.
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, Throwables.TriConsumer<?, ?, ?, E> action)
            throws IndexOutOfBoundsException, IllegalArgumentException, E;

    /**
     * Converts the entire Dataset into a list of Object arrays.
     * <br />
     * This method is typically used when you need to export the data in the Dataset to a different format or system.
     * Each row in the Dataset is converted into an Object array, where each element in the array corresponds to a column in the row.
     * The order of the elements in the array matches the order of the columns in the Dataset.
     * The resulting list of Object arrays is in the same order as the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * List<Object[]> rows = dataset.toList(); // Convert all rows to List<Object[]>
     * }</pre>
     *
     * @return A List of Object arrays representing the data in the Dataset. Each Object array is a row in the Dataset.
     */
    List<Object[]> toList();

    /**
     * Converts a specified range of the Dataset into a list of Object arrays.
     * <br />
     * This method is typically used when you need to export a specific range of data in the Dataset to a different format or system.
     * Each row in the specified range of the Dataset is converted into an Object array, where each element in the array corresponds to a column in the row.
     * The order of the elements in the array matches the order of the columns in the Dataset.
     * The resulting list of Object arrays is in the same order as the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * List<Object[]> firstThreeRows = dataset.toList(0, 3); // Get rows 0, 1, 2
     * }</pre>
     *
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @return A List of Object arrays representing the data in the specified range of the Dataset. Each Object array is a row in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     */
    List<Object[]> toList(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type.
     * <br />
     * This method is typically used when you need to export the data in the Dataset to a specific type of objects.
     * Each row in the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * List<Map<String, Object>> maps = dataset.toList(Map.class); // Convert to List of Maps
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a row in the Dataset.
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Converts a specified range of the Dataset into a list of instances of the specified type.
     * <br />
     * This method is typically used when you need to export a specific range of data in the Dataset to a specific type of objects.
     * Each row in the specified range of the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * List<Employee> firstFiveEmployees = dataset.toList(0, 5, Employee.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return A List of instances of the specified type representing the data in the specified range of the Dataset. Each instance is a row in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type, including only the specified columns.
     * <br />
     * This method is typically used when you need to export specific columns of data in the Dataset to a specific type of objects.
     * Each row in the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns specified in the {@code columnNames} collection will be included in the instance.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age", "salary"), data);
     * List<Employee> employees = dataset.toList(Arrays.asList("name", "salary"), Employee.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the instance.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a row in the Dataset.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty, or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(Collection<String> columnNames, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Converts a specified range of the Dataset into a list of instances of the specified type, including only the specified columns.
     * <br />
     * This method is typically used when you need to export a specific range of data and specific columns of data in the Dataset to a specific type of objects.
     * Each row in the specified range of the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns specified in the {@code columnNames} collection will be included in the instance.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age", "department"), data);
     * List<Employee> topEmployees = dataset.toList(0, 10, Arrays.asList("name", "department"), Employee.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param columnNames The collection of column names to be included in the instance.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return A List of instances of the specified type representing the data in the specified range of the Dataset. Each instance is a row in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty, or the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Class<? extends T> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export the data in the Dataset to a specific type of objects.
     * Each row in the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * List<LinkedHashMap> maps = dataset.toList(size -> new LinkedHashMap<>());
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param rowSupplier The function to create a new instance of the target type. It takes an integer as input, which represents the number of columns in the Dataset.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a row in the Dataset.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Converts a specified range of the Dataset into a list of instances of the specified type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export a specific range of data in the Dataset to a specific type of objects.
     * Each row in the specified range of the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * List<ArrayList> firstFiveRows = dataset.toList(0, 5, size -> new ArrayList<>());
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param rowSupplier The function to create a new instance of the target type. It takes an integer as input, which represents the number of columns in the Dataset.
     * @return A List of instances of the specified type representing the data in the specified range of the Dataset. Each instance is a row in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the specified columns.
     * <br />
     * This method is typically used when you need to export specific columns of data in the Dataset to a specific type of objects.
     * Each row in the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns specified in the {@code columnNames} collection will be included in the instance.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "salary"), data);
     * List<TreeMap> subsetMaps = dataset.toList(Arrays.asList("name", "salary"), size -> new TreeMap<>());
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the instance.
     * @param rowSupplier The function to create a new instance of the target type. It takes an integer as input, which represents the number of columns in the Dataset.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a row in the Dataset.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Converts a specified range of the Dataset into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the specified columns.
     * <br />
     * This method is typically used when you need to export a specific range of data and specific columns of data in the Dataset to a specific type of objects.
     * Each row in the specified range of the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns specified in the {@code columnNames} collection will be included in the instance.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * List<ImmutableList<Object>> rows = dataset.toList();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param columnNames The collection of column names to be included in the instance.
     * @param rowSupplier The function to create a new instance of the target type. It takes an integer as input, which represents the number of columns in the Dataset.
     * @return A List of instances of the specified type representing the data in the specified range of the Dataset. Each instance is a row in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Collection<String> columnNames, IntFunction<? extends T> rowSupplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the columns that pass the specified filter.
     * <br />
     * This method is typically used when you need to export specific columns of data in the Dataset to a specific type of objects.
     * Each row in the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns that pass the {@code columnNameFilter} will be included in the instance.
     * The names of the properties in the instance are determined by the {@code columnNameConverter}.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * List<ImmutableList<Object>> rows = dataset.toList();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param columnNameFilter The predicate to filter the column names. Only the columns that pass this filter will be included in the instance.
     * @param columnNameConverter The function to convert the column names into property names in the instance.
     * @param rowType The Class object representing the target type of the row. It must be Object[], Collection, Map, or Bean class.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a row in the Dataset.
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter, Class<? extends T> rowType)
            throws IllegalArgumentException;

    /**
     * Converts a specified range of the Dataset into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the columns that pass the specified filter.
     * <br />
     * This method is typically used when you need to export a specific range of data and specific columns of data in the Dataset to a specific type of objects.
     * Each row in the specified range of the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns that pass the {@code columnNameFilter} will be included in the instance.
     * The names of the properties in the instance are determined by the {@code columnNameConverter}.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * List<ImmutableList<Object>> rows = dataset.toList();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param columnNameFilter The predicate to filter the column names. Only the columns that pass this filter will be included in the instance.
     * @param columnNameConverter The function to convert the column names into property names in the instance.
     * @param rowType the bean type used to instantiate each materialized row
     * @return A List of instances of the specified type representing the data in the specified range of the Dataset. Each instance is a row in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter,
            Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the columns that pass the specified filter.
     * <br />
     * This method is typically used when you need to export specific columns of data in the Dataset to a specific type of objects.
     * Each row in the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns that pass the {@code columnNameFilter} will be included in the instance.
     * The names of the properties in the instance are determined by the {@code columnNameConverter}.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * List<ImmutableList<Object>> rows = dataset.toList();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param columnNameFilter The predicate to filter the column names. Only the columns that pass this filter will be included in the instance.
     * @param columnNameConverter The function to convert the column names into property names in the instance.
     * @param rowSupplier The function to create a new instance of the target type. It takes an integer as input, which represents the number of columns in the Dataset.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a row in the Dataset.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter, IntFunction<? extends T> rowSupplier);

    /**
     * Converts a specified range of the Dataset into a list of instances of the specified type - Object[], Collection, Map, or Bean class, including only the columns that pass the specified filter.
     * <br />
     * This method is typically used when you need to export a specific range of data and specific columns of data in the Dataset to a specific type of objects.
     * Each row in the specified range of the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns that pass the {@code columnNameFilter} will be included in the instance.
     * The names of the properties in the instance are determined by the {@code columnNameConverter}.
     * The order of the properties in the instance matches the order of the columns in the Dataset.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * List<ImmutableList<Object>> rows = dataset.toList();
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param columnNameFilter The predicate to filter the column names. Only the columns that pass this filter will be included in the instance.
     * @param columnNameConverter The function to convert the column names into property names in the instance.
     * @param rowSupplier the supplier that allocates containers for receiving column values during mapping
     * @return A List of instances of the specified type representing the data in the specified range of the Dataset. Each instance is a row in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter,
            IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type - Bean class, mapping column names to field names based on the provided map.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a specific type of objects (entities), and the column names in the Dataset do not directly match the field names in the entity class.
     * Each row in the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The mapping between column names and field names is determined by the {@code prefixAndFieldNameMap}.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * List<Person> people = dataset.toEntities(Person.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param prefixAndFieldNameMap The map that defines the mapping between column names and field names. The key is the column name prefix, and the value is the corresponding field name.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a row in the Dataset.
     * @throws IllegalArgumentException if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toEntities(Map<String, String> prefixAndFieldNameMap, Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts a specified range of the Dataset into a list of instances of the specified type - Bean class, mapping column names to field names based on the provided map.
     * <br />
     * This method is typically used when you need to export a specific range of data in the Dataset to a specific type of objects (entities), and the column names in the Dataset do not directly match the field names in the entity class.
     * Each row in the specified range of the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The mapping between column names and field names is determined by the {@code prefixAndFieldNameMap}.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * List<Person> people = dataset.toEntities(Person.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param prefixAndFieldNameMap The map that defines the mapping between column names and field names. The key is the column name prefix, and the value is the corresponding field name.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the specified range of the Dataset. Each instance is a row in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toEntities(int fromRowIndex, int toRowIndex, Map<String, String> prefixAndFieldNameMap, Class<? extends T> beanClass)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type - Bean class, mapping column names to field names based on the provided map.
     * <br />
     * This method is typically used when you need to export specific columns of data in the Dataset to a specific type of objects (entities), and the column names in the Dataset do not directly match the field names in the entity class.
     * Each row in the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * The mapping between column names and field names is determined by the {@code prefixAndFieldNameMap}.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * List<Person> people = dataset.toEntities(Person.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param columnNames The collection of column names to be included in the instance.
     * @param prefixAndFieldNameMap The map that defines the mapping between column names and field names. The key is the column name prefix, and the value is the corresponding field name.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a row in the Dataset.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty, or if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toEntities(Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap, Class<? extends T> beanClass)
            throws IllegalArgumentException;

    /**
     * Converts a specified range of the Dataset into a list of instances of the specified type - Bean class, including only the specified columns, mapping column names to field names based on the provided map.
     * <br />
     * This method is typically used when you need to export a specific range of data and specific columns of data in the Dataset to a specific type of objects (entities), and the column names in the Dataset do not directly match the field names in the entity class.
     * Each row in the specified range of the Dataset is converted into an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Only the columns specified in the {@code columnNames} collection will be included in the instance.
     * The mapping between column names and field names is determined by the {@code prefixAndFieldNameMap}.
     * The resulting list of instances is in the same order as the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * List<Person> people = dataset.toEntities(Person.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param fromRowIndex The starting index of the range of rows to be converted. The first row is 0, the second is 1, and so on.
     * @param toRowIndex The ending index of the range of rows to be converted. This index is exclusive, meaning the row at this index will not be converted.
     * @param columnNames The collection of column names to be included in the instance.
     * @param prefixAndFieldNameMap The map that defines the mapping between column names and field names. The key is the column name prefix, and the value is the corresponding field name.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the specified range of the Dataset. Each instance is a row in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty, or if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toEntities(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> beanClass) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type - Bean class, merging rows with the same ID into a single instance.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a specific type of objects (entities), and the rows in the Dataset have duplicate IDs.
     * Each unique ID in the Dataset corresponds to an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Rows with the same ID are merged into a single instance, with the properties of the instance being the union of the properties of the rows.
     * The resulting list of instances is in the same order as the unique IDs in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"), data);
     * List<Employee> employees = dataset.toMergedEntities(Employee.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a merged entity in the Dataset.
     * @throws IllegalArgumentException if the specified {@code beanClass} is not a supported type - Bean class or no id defined in {@code beanClass}.
     */
    <T> List<T> toMergedEntities(Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type - Bean class, merging rows with the same ID into a single instance.
     * <br />
     * This method is typically used when you need to export specific columns of data in the Dataset to a specific type of objects (entities), and the rows in the Dataset have duplicate IDs.
     * Each unique ID in the Dataset corresponds to an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Rows with the same ID are merged into a single instance, with the properties of the instance being the union of the properties of the rows.
     * The resulting list of instances is in the same order as the unique IDs in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {1, "Alicia"}});
     * List<Person> merged = dataset.toMergedEntities("id", Person.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param selectPropNames The collection of property names to be included in the instance.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a merged entity in the Dataset.
     * @throws IllegalArgumentException if any of the specified property names does not exist in the Dataset or {@code selectPropNames} is empty, or if the specified {@code beanClass} is not a supported type - Bean class or no id defined in {@code beanClass}.
     */
    <T> List<T> toMergedEntities(Collection<String> selectPropNames, Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type - Bean class, merging rows with the same ID into a single instance.
     * <br />
     * This method is typically used when you need to export specific columns of data in the Dataset to a specific type of objects (entities), and the rows in the Dataset have duplicate IDs.
     * Each unique ID in the Dataset corresponds to an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Rows with the same ID are merged into a single instance, with the properties of the instance being the union of the properties of the rows.
     * The resulting list of instances is in the same order as the unique IDs in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {1, "Alicia"}});
     * List<Person> merged = dataset.toMergedEntities("id", Person.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param idPropName The property name that is used as the ID for merging rows. Rows with the same ID will be merged into a single instance.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a merged entity in the Dataset.
     * @throws IllegalArgumentException if the specified {@code idPropName} does not exist in the Dataset or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toMergedEntities(String idPropName, Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type - Bean class, merging rows with the same ID into a single instance.
     * <br />
     * This method is typically used when you need to export specific columns of data in the Dataset to a specific type of objects (entities), and the rows in the Dataset have duplicate IDs.
     * Each unique ID in the Dataset corresponds to an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Rows with the same ID are merged into a single instance, with the properties of the instance being the union of the properties of the rows.
     * The resulting list of instances is in the same order as the unique IDs in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {1, "Alicia"}});
     * List<Person> merged = dataset.toMergedEntities("id", Person.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param idPropName The property name that is used as the ID for merging rows. Rows with the same ID will be merged into a single instance.
     * @param selectPropNames The collection of property names to be included in the instance.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a merged entity in the Dataset.
     * @throws IllegalArgumentException if the specified {@code idPropName} does not exist in the Dataset or {@code idPropNames} is empty, or if any of the specified property names does not exist in the Dataset or {@code selectPropNames} is empty, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toMergedEntities(String idPropName, Collection<String> selectPropNames, Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified type - Bean class, merging rows with the same IDs into a single instance.
     * <br />
     * This method is typically used when you need to export specific columns of data in the Dataset to a specific type of objects (entities), and the rows in the Dataset have duplicate IDs.
     * Each unique ID in the Dataset corresponds to an instance of the specified type, where each property in the instance corresponds to a column in the row.
     * Rows with the same IDs are merged into a single instance, with the properties of the instance being the union of the properties of the rows.
     * The resulting list of instances is in the same order as the unique IDs in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {1, "Alicia"}});
     * List<Person> merged = dataset.toMergedEntities("id", Person.class);
     * }</pre>
     *
     * @param <T> The target type of the row.
     * @param idPropNames The collection of property names that are used as the IDs for merging rows. Rows with the same IDs will be merged into a single instance.
     * @param selectPropNames The collection of property names to be included in the instance.
     * @param beanClass The Class object representing the target type of the row. It must be a Bean class.
     * @return A List of instances of the specified type representing the data in the Dataset. Each instance is a merged entity in the Dataset.
     * @throws IllegalArgumentException if any of the specified ID property names does not exist in the Dataset or {@code idPropNames} is empty, or if any of the specified property names does not exist in the Dataset or {@code selectPropNames} is empty, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> List<T> toMergedEntities(Collection<String> idPropNames, Collection<String> selectPropNames, Class<? extends T> beanClass)
            throws IllegalArgumentException;

    /**
     * Converts the entire Dataset into a list of instances of the specified bean class,
     * <br />
     * merging rows that share the same ID properties into a single entity.
     * <p>
     * This method is commonly used when exporting selected columns from a Dataset into a list of typed objects (entities),
     * especially when the Dataset contains multiple rows with the same ID values.
     * <p>
     * Each unique combination of ID property values results in one merged instance of the target type. 
     * The properties of each instance represent a union of the properties from all matching rows.
     * The order of the resulting list corresponds to the order of unique IDs in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {1, "Alicia"}});
     * List<Person> merged = dataset.toMergedEntities("id", Person.class);
     * }</pre>
     *
     * @param <T> the target bean type.
     * @param idPropNames the collection of property names used to identify and group rows. 
     *                    Rows with matching values for these properties will be merged into one instance.
     * @param selectPropNames the collection of property names to include in the resulting instances.
     * @param prefixAndFieldNameMap an optional mapping of column name prefixes to field names in the bean. 
     *                               This supports column headers that are prefixed.
     * @param beanClass the class representing the bean type. Must be a valid JavaBean.
     * @return a list of merged entities of the specified type, based on the Dataset content.
     * @throws IllegalArgumentException if {@code idPropNames} is null or empty, if any specified ID or selected property
     *                                  name does not exist in the Dataset, if the {@code prefixAndFieldNameMap} is invalid,
     *                                  or if {@code beanClass} is not a supported JavaBean class.
     */
    <T> List<T> toMergedEntities(Collection<String> idPropNames, Collection<String> selectPropNames, Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> beanClass) throws IllegalArgumentException;

    /**
     * Converts the entire Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The value of each entry is the value of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"), data);
     * Map<Integer, String> idToNameMap = dataset.toMap("id", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the Dataset that will be used as the values in the resulting map.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the Dataset.
     */
    <K, V> Map<K, V> toMap(String keyColumnName, String valueColumnName) throws IllegalArgumentException;

    /**
     * Converts the entire Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The value of each entry is the value of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * LinkedHashMap<Integer, String> idToNameMap = dataset.toMap("id", "name", LinkedHashMap::new);
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the Dataset that will be used as the values in the resulting map.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the Dataset.
     */
    <K, V, M extends Map<K, V>> M toMap(String keyColumnName, String valueColumnName, IntFunction<? extends M> supplier) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The value of each entry is the value of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * Map<Integer, String> topTenMap = dataset.toMap(0, 10, "id", "name"); // First 10 rows
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the Dataset that will be used as the values in the resulting map.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the Dataset.
     */
    <K, V> Map<K, V> toMap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The value of each entry is the value of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Map<String, Object> first = dataset.toMap(0);
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the Dataset that will be used as the values in the resulting map.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the Dataset.
     */
    <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName, IntFunction<? extends M> supplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Map<String, Object> first = dataset.toMap(0);
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the Dataset that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowType The Class object representing the type of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V> Map<K, V> toMap(String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType) throws IllegalArgumentException;

    /**
     * Converts the entire Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Map<String, Object> first = dataset.toMap(0);
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the Dataset that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowType The Class object representing the type of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V, M extends Map<K, V>> M toMap(String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType,
            IntFunction<? extends M> supplier) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Map<String, Object> first = dataset.toMap(0);
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the Dataset that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowType The Class object representing the type of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V> Map<K, V> toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Map<String, Object> first = dataset.toMap(0);
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the Dataset that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowType The Class object representing the type of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType,
            IntFunction<? extends M> supplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Map<String, Object> first = dataset.toMap(0);
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the Dataset that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V> Map<K, V> toMap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends V> rowSupplier) throws IllegalArgumentException;

    /**
     * Converts the entire Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Map<String, Object> first = dataset.toMap(0);
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the Dataset that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V, M extends Map<K, V>> M toMap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends V> rowSupplier,
            IntFunction<? extends M> supplier) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Map<String, Object> first = dataset.toMap(0);
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the Dataset that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V> Map<K, V> toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends V> rowSupplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a Map, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row, represented as an instance of the specified row type - Object[], Collection, Map, or Bean class.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a Map, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting map does not preserve the order of the rows in the Dataset.
     * The map is created by a provided supplier function, which allows the user to control the type of the map.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Map<String, Object> first = dataset.toMap(0);
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <V> The type of the values in the resulting map.
     * @param <M> The type of the map to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The collection of names of the columns in the Dataset that will be used as the values in the resulting map. Each value in the map is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity.
     * @param supplier A function that generates a new map. The function takes an integer argument, which is the initial map capacity.
     * @return A Map where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is an instance of the specified row type, where each property in the instance corresponds to a column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends V> rowSupplier, IntFunction<? extends M> supplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a ListMultimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a ListMultimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting ListMultimap does not preserve the order of the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] {{"IT", "Alice"}});
     * Multimap<String, Object> byDept = dataset.toMultimap("dept", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the Dataset that will be used as the values in the resulting map.
     * @return A ListMultimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the Dataset.
     */
    <K, T> ListMultimap<K, T> toMultimap(String keyColumnName, String valueColumnName) throws IllegalArgumentException;

    /**
     * Converts the entire Dataset into a Multimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a Multimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting Multimap does not preserve the order of the rows in the Dataset.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] {{"IT", "Alice"}});
     * Multimap<String, Object> byDept = dataset.toMultimap("dept", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the Dataset that will be used as the values in the resulting map.
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the Dataset.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(String keyColumnName, String valueColumnName, IntFunction<? extends M> supplier)
            throws IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a ListMultimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a ListMultimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting ListMultimap does not preserve the order of the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] {{"IT", "Alice"}});
     * Multimap<String, Object> byDept = dataset.toMultimap("dept", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the Dataset that will be used as the values in the resulting map.
     * @return A ListMultimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} or {@code valueColumnName} does not exist in the Dataset.
     */
    <K, T> ListMultimap<K, T> toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a Multimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value column in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a Multimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting Multimap does not preserve the order of the rows in the Dataset.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] {{"IT", "Alice"}});
     * Multimap<String, Object> byDept = dataset.toMultimap("dept", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnName The name of the column in the Dataset that will be used as the values in the resulting map.
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value column in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName,
            IntFunction<? extends M> supplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a ListMultimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a ListMultimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting ListMultimap does not preserve the order of the rows in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("department", "name", "salary"), data);
     * ListMultimap<String, Employee> deptToEmployees = dataset.toMultimap("department", Arrays.asList("name", "salary"), Employee.class);
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the Dataset that will be used as the values in the resulting map.
     * @param rowType The class of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @return A ListMultimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T> ListMultimap<K, T> toMultimap(String keyColumnName, Collection<String> valueColumnNames, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Converts the entire Dataset into a Multimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export data in the Dataset to a Multimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting Multimap does not preserve the order of the rows in the Dataset.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] {{"IT", "Alice"}});
     * Multimap<String, Object> byDept = dataset.toMultimap("dept", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the Dataset that will be used as the values in the resulting map.
     * @param rowType The class of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(String keyColumnName, Collection<String> valueColumnNames,
            Class<? extends T> rowType, IntFunction<? extends M> supplier) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a ListMultimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a ListMultimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting ListMultimap does not preserve the order of the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] {{"IT", "Alice"}});
     * Multimap<String, Object> byDept = dataset.toMultimap("dept", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the Dataset that will be used as the values in the resulting map.
     * @param rowType The class of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @return A ListMultimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T> ListMultimap<K, T> toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a Multimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a Multimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting Multimap does not preserve the order of the rows in the Dataset.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] {{"IT", "Alice"}});
     * Multimap<String, Object> byDept = dataset.toMultimap("dept", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the Dataset that will be used as the values in the resulting map.
     * @param rowType The class of the values in the resulting map. It must be Object[], Collection, Map, or Bean class.
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName,
            Collection<String> valueColumnNames, Class<? extends T> rowType, IntFunction<? extends M> supplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a ListMultimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a ListMultimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting ListMultimap does not preserve the order of the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] {{"IT", "Alice"}});
     * Multimap<String, Object> byDept = dataset.toMultimap("dept", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the Dataset that will be used as the values in the resulting map.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity. The return value created by specified {@code rowSupplier} must be an Object[], Collection, Map, or Bean class
     * @return A ListMultimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T> ListMultimap<K, T> toMultimap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends T> rowSupplier)
            throws IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a Multimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a Multimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting Multimap does not preserve the order of the rows in the Dataset.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] {{"IT", "Alice"}});
     * Multimap<String, Object> byDept = dataset.toMultimap("dept", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the Dataset that will be used as the values in the resulting map.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity. The return value created by specified {@code rowSupplier} must be an Object[], Collection, Map, or Bean class
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends T> rowSupplier, IntFunction<? extends M> supplier) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a ListMultimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a ListMultimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting ListMultimap does not preserve the order of the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] {{"IT", "Alice"}});
     * Multimap<String, Object> byDept = dataset.toMultimap("dept", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the Dataset that will be used as the values in the resulting map.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity. The return value created by specified {@code rowSupplier} must be an Object[], Collection, Map, or Bean class
     * @return A ListMultimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T> ListMultimap<K, T> toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into a Multimap, where each entry in the map corresponds to a row in the Dataset.
     * <br />
     * The key of each entry is the value of the specified key column in the row. The values of each entry are the values of the specified value columns in the row.
     * <br />
     * This method is typically used when you need to export a range of data in the Dataset to a Multimap, where each key-value pair in the map corresponds to a row in the Dataset.
     * The resulting Multimap does not preserve the order of the rows in the Dataset.
     * The Multimap is created by a provided supplier function, which allows the user to control the type of the Multimap.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] {{"IT", "Alice"}});
     * Multimap<String, Object> byDept = dataset.toMultimap("dept", "name");
     * }</pre>
     *
     * @param <K> The type of the keys in the resulting map.
     * @param <T> The type of the values in the resulting map.
     * @param <V> The type of the collection of values in the resulting map.
     * @param <M> The type of the Multimap to be returned.
     * @param fromRowIndex The starting index of the row range to be included in the map.
     * @param toRowIndex The ending index of the row range to be included in the map.
     * @param keyColumnName The name of the column in the Dataset that will be used as the keys in the resulting map.
     * @param valueColumnNames The names of the columns in the Dataset that will be used as the values in the resulting map.
     * @param rowSupplier A function that generates a new row. The function takes an integer argument, which is the initial row capacity. The return value created by specified {@code rowSupplier} must be an Object[], Collection, Map, or Bean class
     * @param supplier A function that generates a new Multimap. The function takes an integer argument, which is the initial map capacity.
     * @return A Multimap where each key-value pair corresponds to a row in the Dataset. The key of each pair is the value of the specified key column in the row. The value of each pair is the value of the specified value columns in the row.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if the specified {@code keyColumnName} does not exist in the Dataset, or if any of the specified value column names does not exist in the Dataset or {@code valueColumnNames} is empty, or the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName,
            Collection<String> valueColumnNames, IntFunction<? extends T> rowSupplier, IntFunction<? extends M> supplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a JSON string.
     * <br />
     * This method is typically used when you need to export the data in the Dataset to a JSON format.
     * The resulting JSON string represents the entire Dataset, including all rows and columns.
     * The order of the rows in the JSON string is the same as the order of the rows in the Dataset.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * String json = dataset.toJson(); // Convert Dataset to JSON array string
     * }</pre>
     *
     * @return A JSON string representing the current Dataset.
     * @see #toJson(int, int, Collection)
     */
    String toJson();

    /**
     * Converts a range of rows in the Dataset into a JSON string.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to a JSON format.
     * The resulting JSON string represents the specified range of rows in the Dataset.
     * The order of the rows in the JSON string is the same as the order of the rows in the Dataset.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String json = dataset.toJson();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the JSON string.
     * @param toRowIndex The ending index of the row range to be included in the JSON string.
     * @return A JSON string representing the specified range of rows in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     */
    String toJson(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Converts a range of rows in the Dataset into a JSON string, including only the specified columns.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to a JSON format.
     * The resulting JSON string represents the specified range of rows and columns in the Dataset.
     * The order of the rows in the JSON string is the same as the order of the rows in the Dataset.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String json = dataset.toJson();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the JSON string.
     * @param toRowIndex The ending index of the row range to be included in the JSON string.
     * @param columnNames The names of the columns in the Dataset to be included in the JSON string.
     * @return A JSON string representing the specified range of rows and columns in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     */
    String toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a JSON string and writes it to the provided File.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String json = dataset.toJson();
     * }</pre>
     *
     * @param output
     * @throws UncheckedIOException if an I/O error occurs while writing to the File.
     * @see #toJson(int, int, Collection, File)
     */
    void toJson(File output) throws UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into a JSON string and writes it to the provided File.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String json = dataset.toJson();
     * }</pre>
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws UncheckedIOException if an I/O error occurs while writing to the File.
     * @see #toJson(int, int, Collection, File)
     */
    void toJson(int fromRowIndex, int toRowIndex, File output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into a JSON string, including only the specified columns, and writes it to the provided File.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to a JSON format and write it directly to a File.
     * The resulting JSON string represents the specified range of rows and columns in the Dataset.
     * The order of the rows in the JSON string is the same as the order of the rows in the Dataset.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String json = dataset.toJson();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the JSON string.
     * @param toRowIndex The ending index of the row range to be included in the JSON string.
     * @param columnNames The names of the columns in the Dataset to be included in the JSON string.
     * @param output The File where the JSON string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs while writing to the File.
     */
    void toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames, File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire Dataset into a JSON string and writes it to the provided OutputStream.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String json = dataset.toJson();
     * }</pre>
     *
     * @param output
     * @throws UncheckedIOException if an I/O error occurs while writing to the OutputStream.
     * @see #toJson(int, int, Collection, OutputStream)
     */
    void toJson(OutputStream output) throws UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into a JSON string and writes it to the provided OutputStream.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String json = dataset.toJson();
     * }</pre>
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws UncheckedIOException if an I/O error occurs while writing to the OutputStream.
     * @see #toJson(int, int, Collection, OutputStream
     */
    void toJson(int fromRowIndex, int toRowIndex, OutputStream output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into a JSON string, including only the specified columns, and writes it to the provided OutputStream.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to a JSON format and write it directly to an OutputStream, such as a FileOutputStream for writing to a file.
     * The resulting JSON string represents the specified range of rows and columns in the Dataset.
     * The order of the rows in the JSON string is the same as the order of the rows in the Dataset.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String json = dataset.toJson();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the JSON string.
     * @param toRowIndex The ending index of the row range to be included in the JSON string.
     * @param columnNames The names of the columns in the Dataset to be included in the JSON string.
     * @param output The OutputStream where the JSON string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs while writing to the OutputStream.
     */
    void toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames, OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire Dataset into a JSON string and writes it to the provided Writer.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String json = dataset.toJson();
     * }</pre>
     *
     * @param output
     * @throws UncheckedIOException
     * @see #toJson(int, int, Collection, Writer)
     */
    void toJson(Writer output) throws UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into a JSON string and writes it to the provided Writer.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String json = dataset.toJson();
     * }</pre>
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
     * Converts a range of rows in the Dataset into a JSON string, including only the specified columns, and writes it to the provided Writer.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to a JSON format and write it directly to a Writer, such as a FileWriter for writing to a file.
     * The resulting JSON string represents the specified range of rows and columns in the Dataset.
     * The order of the rows in the JSON string is the same as the order of the rows in the Dataset.
     * The order of the keys in each JSON object (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String json = dataset.toJson();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the JSON string.
     * @param toRowIndex The ending index of the row range to be included in the JSON string.
     * @param columnNames The names of the columns in the Dataset to be included in the JSON string.
     * @param output The Writer where the JSON string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs while writing to the Writer.
     */
    void toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire Dataset into an XML string, with each row represented as an XML element with the specified name, and returns it as a String.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @return A String containing the XML representation of the Dataset.
     * @see #toXml(int, int, Collection, String)
     */
    String toXml();

    /**
     * Converts the entire Dataset into an XML string, with each row represented as an XML element with the specified name, and returns it as a String.
     * <br />
     * This method is typically used when you need to export the data in the Dataset to an XML format.
     * The resulting XML string represents the entire Dataset.
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @return A String containing the XML representation of the Dataset.
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @see #toXml(int, int, Collection, String)
     */
    String toXml(String rowElementName) throws IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into an XML string and returns it as a String.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows in the Dataset.
     * Each row in the Dataset is represented as an XML element with a default name.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     * <br />
     * Note: The method uses the default settings for XML serialization. If you need more control over the XML output (e.g., custom element names, namespaces, etc.), consider using the overloaded method with more parameters.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @return A String containing the XML representation of the specified range of rows in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @see #toXml(int, int, Collection, String)
     */
    String toXml(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Converts a range of rows in the Dataset into an XML string, with each row represented as an XML element with the specified name, and returns it as a String.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows in the Dataset.
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @return A String containing the XML representation of the specified range of rows in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @see #toXml(int, int, Collection, String)
     */
    String toXml(int fromRowIndex, int toRowIndex, String rowElementName) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into an XML string, with each row represented as an XML element with a default name, and returns it as a String.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows in the Dataset.
     * Each row in the Dataset is represented as an XML element with a default name.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The collection of column names to be included in the XML string.
     * @return A String containing the XML representation of the specified range of rows in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty
     * @see #toXml(int, int, Collection, String)
     */
    String toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts a range of rows in the Dataset into an XML string, including only the specified columns, and returns it as a String.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows and columns in the Dataset.
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The names of the columns in the Dataset to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @return A String containing the XML representation of the specified range of rows and columns in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty, or if {@code rowElementName} is empty.
     */
    String toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Writes the entire Dataset as an XML string to the specified File.
     * <br />
     * This method is typically used when you need to export the entire data in the Dataset to an XML format.
     * The resulting XML string represents all the rows in the Dataset.
     * Each row in the Dataset is represented as an XML element with a default name.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param output The File where the XML string will be written.
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     * @see #toXml(int, int, Collection, String, File)
     */
    void toXml(File output) throws UncheckedIOException;

    /**
     * Writes the entire Dataset as an XML string to the specified File, with each row represented as an XML element with the specified name.
     * <br />
     * This method is typically used when you need to export the entire data in the Dataset to an XML format.
     * The resulting XML string represents all the rows in the Dataset.
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @param output The File where the XML string will be written.
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     * @see #toXml(int, int, Collection, String, File)
     */
    void toXml(String rowElementName, File output) throws IllegalArgumentException, UncheckedIOException;

    /**
     * Writes a range of rows in the Dataset as an XML string to the specified File.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows in the Dataset.
     * Each row in the Dataset is represented as an XML element with a default name.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param output The File where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     * @see #toXml(int, int, Collection, String, File)
     */
    void toXml(int fromRowIndex, int toRowIndex, File output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Writes a range of rows in the Dataset as an XML string to the specified File, with each row represented as an XML element with the specified name.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows in the Dataset.
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @param output The File where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     * @see #toXml(int, int, Collection, String, File)
     */
    void toXml(int fromRowIndex, int toRowIndex, String rowElementName, File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into an XML string, including only the specified columns, and writes it to the specified File.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The names of the columns in the Dataset to be included in the XML string.
     * @param output The File where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     * @see #toXml(int, int, Collection, String, File)
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into an XML string, including only the specified columns, and writes it to the specified File.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows and columns in the Dataset.
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The names of the columns in the Dataset to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @param output The File where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty, or if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the File.
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName, File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire Dataset into an XML string and writes it to the specified OutputStream.
     * <br />
     * Each row in the Dataset is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export the entire data in the Dataset to an XML format.
     * The resulting XML string represents the entire Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param output The OutputStream where the XML string will be written.
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     * @see #toXml(int, int, Collection, String, OutputStream)
     */
    void toXml(OutputStream output) throws UncheckedIOException;

    /**
     * Converts the entire Dataset into an XML string, using the specified row element name, and writes it to the specified OutputStream.
     * <br />
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export the data in the Dataset to an XML format.
     * The resulting XML string represents the entire Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @param output The OutputStream where the XML string will be written.
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     * @see #toXml(int, int, Collection, String, OutputStream)
     */
    void toXml(String rowElementName, OutputStream output) throws UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into an XML string and writes it to the specified OutputStream.
     * <br />
     * Each row in the Dataset is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows in the Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param output The OutputStream where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     * @see #toXml(int, int, Collection, String, OutputStream)
     */
    void toXml(int fromRowIndex, int toRowIndex, OutputStream output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into an XML string, using the specified row element name, and writes it to the specified OutputStream.
     * <br />
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows in the Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @param output The OutputStream where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     * @see #toXml(int, int, Collection, String, OutputStream)
     */
    void toXml(int fromRowIndex, int toRowIndex, String rowElementName, OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into an XML string, including only the specified columns, and writes it to the specified OutputStream.
     * <br />
     * Each row in the Dataset is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows in the Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the specified columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The collection of column names to be included in the XML string.
     * @param output The OutputStream where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     * @see #toXml(int, int, Collection, String, OutputStream)
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into an XML string, including only the specified columns, and writes it to the specified OutputStream.
     * <br />
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows and columns in the Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The names of the columns in the Dataset to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @param output The OutputStream where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty, or if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the OutputStream.
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName, OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Writes the entire Dataset as an XML string to the specified Writer.
     * <br />
     * Each row in the Dataset is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export the entire data in the Dataset to an XML format.
     * The resulting XML string represents all the rows in the Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param output The Writer where the XML string will be written.
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     * @see #toXml(int, int, Collection, String, Writer)
     */
    void toXml(Writer output) throws UncheckedIOException;

    /**
     * Writes the entire Dataset as an XML string to the specified Writer.
     * <br />
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export the entire data in the Dataset to an XML format.
     * The resulting XML string represents all the rows in the Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @param output The Writer where the XML string will be written.
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     * @see #toXml(int, int, Collection, String, Writer)
     */
    void toXml(String rowElementName, Writer output) throws IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into an XML string and writes it to the specified Writer.
     * <br />
     * Each row in the Dataset is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows in the Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param output The Writer where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     * @see #toXml(int, int, Collection, String, Writer)
     */
    void toXml(int fromRowIndex, int toRowIndex, Writer output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into an XML string and writes it to the specified Writer.
     * <br />
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows in the Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @param output The Writer where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     * @see #toXml(int, int, Collection, String, Writer)
     */
    void toXml(int fromRowIndex, int toRowIndex, String rowElementName, Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into an XML string, including only the specified columns, and writes it to the specified Writer.
     * <br />
     * Each row in the Dataset is represented as an XML element named "row".
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows in the Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the specified columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The collection of column names to be included in the XML string.
     * @param output The Writer where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     * @see #toXml(int, int, Collection, String, Writer)
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts a range of rows in the Dataset into an XML string, including only the specified columns, and writes it to the specified Writer.
     * <br />
     * Each row in the Dataset is represented as an XML element with the name specified by the {@code rowElementName} parameter.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to an XML format.
     * The resulting XML string represents the specified range of rows and columns in the Dataset.
     * The order of the rows in the XML string is the same as the order of the rows in the Dataset.
     * The order of the elements in each XML row element (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String xml = dataset.toXml();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the XML string.
     * @param toRowIndex The ending index of the row range to be included in the XML string.
     * @param columnNames The names of the columns in the Dataset to be included in the XML string.
     * @param rowElementName The name of the XML element that represents a row in the Dataset.
     * @param output The Writer where the XML string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty, or if {@code rowElementName} is empty.
     * @throws UncheckedIOException if an I/O error occurs writing to the Writer.
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName, Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire Dataset into a CSV string.
     * <br />
     * This method is typically used when you need to export the entire data in the Dataset to a CSV format.
     * The resulting CSV string represents the entire Dataset.
     * The order of the rows in the CSV string is the same as the order of the rows in the Dataset.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String csv = dataset.toCsv();
     * }</pre>
     *
     * @return A CSV string representing the entire Dataset.
     * @see #toCsv(int, int, Collection)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    String toCsv();

    /**
     * Converts a range of rows in the Dataset into a CSV string, including only the specified columns.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to a CSV format.
     * The resulting CSV string represents the specified range of rows and columns in the Dataset.
     * The order of the rows in the CSV string is the same as the order of the rows in the Dataset.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String csv = dataset.toCsv();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the CSV string.
     * @param toRowIndex The ending index of the row range to be included in the CSV string.
     * @param columnNames The names of the columns in the Dataset to be included in the CSV string.
     * @return A CSV string representing the specified range of rows and columns in the Dataset.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     * @see #toCsv(int, int, Collection)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    String toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Converts the entire Dataset into a CSV string and writes it to a File.
     * <br />
     * This method is typically used when you need to export the entire data in the Dataset to a CSV format.
     * The resulting CSV string represents the entire Dataset.
     * The order of the rows in the CSV string is the same as the order of the rows in the Dataset.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String csv = dataset.toCsv();
     * }</pre>
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
     * Converts a range of rows in the Dataset into a CSV string, including only the specified columns, and writes it to a File.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to a CSV format.
     * The resulting CSV string represents the specified range of rows and columns in the Dataset.
     * The order of the rows in the CSV string is the same as the order of the rows in the Dataset.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String csv = dataset.toCsv();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the CSV string.
     * @param toRowIndex The ending index of the row range to be included in the CSV string.
     * @param columnNames The names of the columns in the Dataset to be included in the CSV string.
     * @param output The File where the CSV string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #toCsv(int, int, Collection, File)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire Dataset into a CSV string and writes it to an OutputStream.
     * <br />
     * This method is typically used when you need to export the entire data in the Dataset to a CSV format.
     * The resulting CSV string represents the entire Dataset.
     * The order of the rows in the CSV string is the same as the order of the rows in the Dataset.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String csv = dataset.toCsv();
     * }</pre>
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
     * Converts a range of rows in the Dataset into a CSV string, including only the specified columns, and writes it to an OutputStream.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to a CSV format.
     * The resulting CSV string represents the specified range of rows and columns in the Dataset.
     * The order of the rows in the CSV string is the same as the order of the rows in the Dataset.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String csv = dataset.toCsv();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the CSV string.
     * @param toRowIndex The ending index of the row range to be included in the CSV string.
     * @param columnNames The names of the columns in the Dataset to be included in the CSV string.
     * @param output The OutputStream where the CSV string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #toCsv(int, int, Collection, OutputStream)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Converts the entire Dataset into a CSV string and writes it to a Writer.
     * <br />
     * This method is typically used when you need to export the entire data in the Dataset to a CSV format.
     * The resulting CSV string represents the entire Dataset.
     * The order of the rows in the CSV string is the same as the order of the rows in the Dataset.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String csv = dataset.toCsv();
     * }</pre>
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
     * Converts a range of rows in the Dataset into a CSV string, including only the specified columns, and writes it to a Writer.
     * <br />
     * This method is typically used when you need to export a subset of the data in the Dataset to a CSV format.
     * The resulting CSV string represents the specified range of rows and columns in the Dataset.
     * The order of the rows in the CSV string is the same as the order of the rows in the Dataset.
     * The order of the elements in each CSV row (representing a row) is the same as the order of the columns in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * String csv = dataset.toCsv();
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to be included in the CSV string.
     * @param toRowIndex The ending index of the row range to be included in the CSV string.
     * @param columnNames The names of the columns in the Dataset to be included in the CSV string.
     * @param output The Writer where the CSV string will be written.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #toCsv(int, int, Collection, Writer)
     * @see CSVUtil#setEscapeCharToBackSlashForWrite()
     * @see CSVUtil#resetEscapeCharForWrite()
     * @see CSVUtil#writeField(BufferedCSVWriter, com.landawn.abacus.type.Type, Object)
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException;

    /**
     * Groups the rows in the Dataset by a specified key column and applies an aggregate operation on another column.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on a column's values, grouped by another column's values.
     * The resulting Dataset will have unique values of the key column, and the result of the aggregate operation on the specified column.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("department", "employee", "salary"),
     *     new Object[][] {
     *          {"Sales", "Alice", 50000},
     *          {"Sales", "Bob", 55000},
     *          {"IT", "Charlie", 70000},
     *          {"IT", "David", 75000},
     *          {"Sales", "Eve", 52000}
     *     });
     *
     * // +------------+----------+--------+
     * // | department | employee | salary |
     * // +------------+----------+--------+
     * // | Sales      | Alice    | 50000  |
     * // | Sales      | Bob      | 55000  |
     * // | IT         | Charlie  | 70000  |
     * // | IT         | David    | 75000  |
     * // | Sales      | Eve      | 52000  |
     * // +------------+----------+--------+
     *
     * Dataset result = dataset.groupBy(
     *     "department",                           // group by department
     *     "salary",                              // aggregate on salary column
     *     "total_salary",                        // result column name
     *     Collectors.summingInt(Integer.class::cast)); // sum salaries
     *
     * // Result Dataset:
     * // +------------+--------------+
     * // | department | total_salary |
     * // +------------+--------------+
     * // | Sales      | 157000       |
     * // | IT         | 145000       |
     * // +------------+--------------+
     * }</pre>
     *
     * @param keyColumnName The name of the column to group by. Must not be {@code null}.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed. Must not be {@code null}.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param collector The collector that defines the aggregate operation. Must not be {@code null}.
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or if {@code keyColumnName}, {@code aggregateOnColumnName}, {@code aggregateResultColumnName}, or {@code collector} is {@code null}.
     * @see #groupBy(Collection)
     * @see #groupBy(String, Collection, String, Collector)
     * @see #rollup(Collection)
     * @see #cube(Collection)
     * @see #pivot(String, String, String, Collector)
     * @see java.util.stream.Collectors
     */
    Dataset groupBy(String keyColumnName, String aggregateOnColumnName, String aggregateResultColumnName, Collector<?, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by a specified key column and applies an aggregate operation on multiple columns.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting Dataset will have unique values of the key column, and the result of the aggregate operation on the specified columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnName The name of the column to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class type of the row in the resulting Dataset. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A new Dataset with the grouped and aggregated data - list of type {@code rowType}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see #groupBy(String, String, String, Collector)
     * @see #groupBy(String, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(String keyColumnName, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName, Class<?> rowType)
            throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by a specified key column and applies an aggregate operation on multiple columns.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting Dataset will have unique values of the key column, and the result of the aggregate operation on the specified columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnName The name of the column to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector the downstream collector used to aggregate grouped rows
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code aggregateOnColumnNames} is empty.
     * @see #groupBy(String, String, String, Collector)
     * @see #groupBy(String, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(String keyColumnName, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by a specified key column and applies an aggregate operation on multiple columns.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting Dataset will have unique values of the key column, and the result of the aggregate operation on the specified columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param <T> The type of the elements being grouped.
     * @param keyColumnName The name of the column to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowMapper A function that transforms the aggregated rows into a specific type {@code T}.
     * @param collector The collector that defines the aggregate operation.
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code aggregateOnColumnNames} is empty.
     * @see #groupBy(String, String, String, Collector)
     * @see #groupBy(String, Function, Collection, String, Function, Collector)
     */
    <T> Dataset groupBy(String keyColumnName, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by a specified key column and applies an aggregate operation on a specific column.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on a column's values, grouped by another column's values.
     * The resulting Dataset will have unique values of the key column, and the result of the aggregate operation on the specified column.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnName The name of the column to group by.
     * @param keyExtractor A function that transforms the key column values.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset.
     * @see #groupBy(String, String, String, Collector)
     * @see #groupBy(String, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(String keyColumnName, Function<?, ?> keyExtractor, String aggregateOnColumnName, String aggregateResultColumnName,
            Collector<?, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by a specified key column and applies an aggregate operation on multiple columns.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting Dataset will have unique values of the key column, and the result of the aggregate operation on the specified columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnName The name of the column to group by.
     * @param keyExtractor A function that transforms the key column values.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class type of the row in the resulting Dataset. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A new Dataset with the grouped and aggregated data - list of type {@code rowType}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see #groupBy(String, String, String, Collector)
     * @see #groupBy(String, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(String keyColumnName, Function<?, ?> keyExtractor, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Class<?> rowType) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by a specified key column and applies an aggregate operation on multiple columns.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting Dataset will have unique values of the key column, and the result of the aggregate operation on the specified columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnName The name of the column to group by.
     * @param keyExtractor A function that transforms the key column values.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code aggregateOnColumnNames} is empty.
     * @see #groupBy(String, String, String, Collector)
     * @see #groupBy(String, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(String keyColumnName, Function<?, ?> keyExtractor, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by a specified key column and applies an aggregate operation on multiple columns.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by another column's values.
     * The resulting Dataset will have unique values of the key column (after transformation by the keyExtractor), and the result of the aggregate operation on the specified columns.
     * <br />
     * The keyExtractor function allows you to transform the key column values before grouping, enabling operations like case-insensitive grouping, date truncation, or other key transformations.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("department", "employee", "salary", "bonus"),
     *     new Object[][] {
     *          {"sales", "Alice", 50000, 5000},
     *          {"SALES", "Bob", 55000, 6000},
     *          {"it", "Charlie", 70000, 7000},
     *          {"IT", "David", 75000, 8000}
     *     });
     *
     * // +------------+----------+--------+-------+
     * // | department | employee | salary | bonus |
     * // +------------+----------+--------+-------+
     * // | sales      | Alice    | 50000  | 5000  |
     * // | SALES      | Bob      | 55000  | 6000  |
     * // | it         | Charlie  | 70000  | 7000  |
     * // | IT         | David    | 75000  | 8000  |
     * // +------------+----------+--------+-------+
     * 
     * Dataset result = dataset.groupBy(
     *     "department",                           // group by department
     *     String::toLowerCase,                    // normalize to lowercase
     *     Arrays.asList("salary", "bonus"),      // aggregate on salary and bonus
     *     "total_compensation",                   // result column name
     *     row -> ((Integer) row.get(0)) + ((Integer) row.get(1)), // sum salary + bonus
     *     Collectors.summingInt(Integer.class::cast)); // sum the results
     *
     * // Result Dataset:
     * // +------------+--------------------+
     * // | department | total_compensation |
     * // +------------+--------------------+
     * // | sales      | 116000             |
     * // | it         | 160000             |
     * // +------------+--------------------+
     * }</pre>
     *
     * @param <T> The type of the elements being grouped after row mapping.
     * @param keyColumnName The name of the column to group by. Must not be {@code null}.
     * @param keyExtractor A function that transforms the key column values before grouping. Must not be {@code null}.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param rowMapper A function that transforms the aggregated rows into a specific type {@code T}. Must not be {@code null}.
     * @param collector The collector that defines the aggregate operation. Must not be {@code null}.
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or if {@code keyColumnName}, {@code keyExtractor}, {@code aggregateOnColumnNames}, {@code aggregateResultColumnName}, {@code rowMapper}, or {@code collector} is {@code null}, or if {@code aggregateOnColumnNames} is empty.
     * @see #groupBy(Collection)
     * @see #groupBy(String, String, String, Collector)
     * @see #groupBy(String, Collection, String, Collector)
     * @see #rollup(Collection)
     * @see #cube(Collection)
     * @see #pivot(String, String, String, Collector)
     * @see java.util.stream.Collectors
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    <T> Dataset groupBy(String keyColumnName, Function<?, ?> keyExtractor, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by the specified key columns.
     * <br />
     * This method is typically used when you need to categorize data based on multiple column values.
     * The resulting Dataset will have unique combinations of the key column values, with duplicates removed.
     * Each row in the result represents a distinct combination of values from the specified key columns.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("department", "level", "employee", "salary"),
     *     new Object[][] {
     *          {"Sales", "Junior", "Alice", 50000},
     *          {"Sales", "Senior", "Bob", 75000},
     *          {"Sales", "Junior", "Charlie", 52000},
     *          {"IT", "Senior", "David", 80000},
     *          {"IT", "Junior", "Eve", 55000},
     *          {"Sales", "Senior", "Frank", 78000}
     *     });
     *
     * // +------------+--------+----------+--------+
     * // | department | level  | employee | salary |
     * // +------------+--------+----------+--------+
     * // | Sales      | Junior | Alice    | 50000  |
     * // | Sales      | Senior | Bob      | 75000  |
     * // | Sales      | Junior | Charlie  | 52000  |
     * // | IT         | Senior | David    | 80000  |
     * // | IT         | Junior | Eve      | 55000  |
     * // | Sales      | Senior | Frank    | 78000  |
     * // +------------+--------+----------+--------+
     * 
     * Dataset result = dataset.groupBy(Arrays.asList("department", "level"));
     *
     * // Result Dataset:
     * // +------------+--------+
     * // | department | level  |
     * // +------------+--------+
     * // | Sales      | Junior |
     * // | Sales      | Senior |
     * // | IT         | Senior |
     * // | IT         | Junior |
     * // +------------+--------+
     * }</pre>
     *
     * @param keyColumnNames The names of the columns to group by. Must not be {@code null} or empty.
     * @return A new Dataset containing only the specified key columns with unique combinations of their values.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset, or if {@code keyColumnNames} is {@code null} or empty.
     * @see #groupBy(String, String, String, Collector)
     * @see #groupBy(String, Collection, String, Collector)
     * @see #groupBy(Collection, String, String, Collector)
     * @see #rollup(Collection)
     * @see #cube(Collection)
     * @see #pivot(String, String, String, Collector)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    Dataset groupBy(Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by the specified key columns and applies an aggregate operation on a specific column.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on a column's values, grouped by other column's values.
     * The resulting Dataset will have unique combinations of the key column values, and the result of the aggregate operation on the specified column.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty.
     * @see #groupBy(Collection)
     * @see #groupBy(Collection, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(Collection<String> keyColumnNames, String aggregateOnColumnName, String aggregateResultColumnName, Collector<?, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by the specified key columns and applies an aggregate operation on multiple columns.
     * <br />
     * The result of the aggregation is stored in a new column with a specified type.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting Dataset will have unique combinations of the key column values, and the result of the aggregate operation on the specified columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class type of the new column that will store the result of the aggregate operation. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A new Dataset with the grouped and aggregated data - list of type {@code rowType}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see #groupBy(Collection)
     * @see #groupBy(Collection, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName, Class<?> rowType)
            throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by the specified key columns and applies an aggregate operation on multiple columns.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting Dataset will have unique combinations of the key column values, and the result of the aggregate operation on the specified columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see #groupBy(Collection)
     * @see #groupBy(Collection, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by the specified key columns and applies an aggregate operation on multiple columns.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting Dataset will have unique combinations of the key column values, and the result of the aggregate operation on the specified columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param <T> The type of the new format after applying the rowMapper function.
     * @param keyColumnNames The names of the columns to group by.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowMapper The function to transform the rows into a new format.
     * @param collector The collector that defines the aggregate operation.
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see #groupBy(Collection)
     * @see #groupBy(Collection, Function, Collection, String, Function, Collector)
     */
    <T> Dataset groupBy(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by the specified key columns.
     * <br />
     * The keys for grouping are generated by the provided keyExtractor function.
     * <br />
     * This method is typically used when you need to group data by a complex key composed of multiple columns or computed values.
     * The resulting Dataset will have unique combinations of the key values.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param keyExtractor The function to generate the key for grouping. It takes an array of objects (the row) and returns a key object.
     * @return A new Dataset with the grouped data.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty.
     * @see #groupBy(Collection)
     * @see #groupBy(Collection, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by the specified key columns and applies an aggregate operation on a specific column.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on a column's values, grouped by other columns' values.
     * The resulting Dataset will have unique combinations of the key values, and the result of the aggregate operation on the specified column.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param keyExtractor The function to generate the key for grouping. It takes an array of objects (the row) and returns a key object.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty.
     * @see #groupBy(Collection)
     * @see #groupBy(Collection, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, String aggregateOnColumnName,
            String aggregateResultColumnName, Collector<?, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by the specified key columns and applies an aggregate operation on specific columns.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting Dataset will have unique combinations of the key values, and the result of the aggregate operation on the specified columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param keyExtractor The function to generate the key for grouping. It takes an array of objects (the row) and returns a key object.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class of the row type. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A new Dataset with the grouped and aggregated data - list of type {@code rowType}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see #groupBy(Collection)
     * @see #groupBy(Collection, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Class<?> rowType) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by the specified key columns and applies an aggregate operation on specific columns.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting Dataset will have unique combinations of the key values, and the result of the aggregate operation on the specified columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] {{"IT", 100}});
     * Dataset grouped = dataset.groupBy(Arrays.asList("dept"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns to group by.
     * @param keyExtractor The function to generate the key for grouping. It takes an array of objects (the row) and returns a key object.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector that defines the aggregate operation.
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see #groupBy(Collection)
     * @see #groupBy(Collection, Function, Collection, String, Function, Collector)
     */
    Dataset groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Groups the rows in the Dataset by the specified key columns and applies an aggregate operation on multiple columns.
     * <br />
     * The result of the aggregation is stored in a new column.
     * <br />
     * This method is typically used when you need to perform operations such as sum, average, count, etc., on multiple columns' values, grouped by other columns' values.
     * The resulting Dataset will have unique combinations of the key values (after transformation by the keyExtractor), and the result of the aggregate operation on the specified columns.
     * <br />
     * The keyExtractor function allows you to transform the key column values before grouping, enabling operations like creating composite keys, applying transformations, or other key processing.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("department", "level", "employee", "salary", "bonus"),
     *     new Object[][] {
     *          {"Sales", "Junior", "Alice", 50000, 5000},
     *          {"Sales", "Senior", "Bob", 75000, 8000},
     *          {"IT", "Junior", "Charlie", 55000, 6000},
     *          {"IT", "Senior", "David", 80000, 9000},
     *          {"Sales", "Junior", "Eve", 52000, 5500}
     *     });
     *
     * // +------------+--------+----------+--------+-------+
     * // | department | level  | employee | salary | bonus |
     * // +------------+--------+----------+--------+-------+
     * // | Sales      | Junior | Alice    | 50000  | 5000  |
     * // | Sales      | Senior | Bob      | 75000  | 8000  |
     * // | IT         | Junior | Charlie  | 55000  | 6000  |
     * // | IT         | Senior | David    | 80000  | 9000  |
     * // | Sales      | Junior | Eve      | 52000  | 5500  |
     * // +------------+--------+----------+--------+-------+
     * 
     * Dataset result = dataset.groupBy(
     *     Arrays.asList("department", "level"),    // group by department and level
     *     row -> row.get(0) + "_" + row.get(1),   // create composite key: "Sales_Junior"
     *     Arrays.asList("salary", "bonus"),       // aggregate on salary and bonus
     *     "total_compensation",                    // result column name
     *     row -> ((Integer) row.get(0)) + ((Integer) row.get(1)), // sum salary + bonus
     *     Collectors.summingInt(Integer.class::cast)); // sum the results
     *
     * // Result Dataset:
     * // +------------+--------+--------------------+
     * // | department | level  | total_compensation |
     * // +------------+--------+--------------------+
     * // | Sales      | Junior | 112500             |
     * // | Sales      | Senior | 83000              |
     * // | IT         | Junior | 61000              |
     * // | IT         | Senior | 89000              |
     * // +------------+--------+--------------------+
     * }</pre>
     *
     * @param <T> The type of the elements being grouped after row mapping.
     * @param keyColumnNames The names of the columns to group by. Must not be {@code null} or empty.
     * @param keyExtractor A function that transforms the key column values before grouping. Must not be {@code null}.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param rowMapper A function that transforms the aggregated rows into a specific type {@code T}. Must not be {@code null}.
     * @param collector The collector that defines the aggregate operation. Must not be {@code null}.
     * @return A new Dataset with the grouped and aggregated data - collected by the specified {@code collector}.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset, or if {@code keyColumnNames}, {@code keyExtractor}, {@code aggregateOnColumnNames}, {@code aggregateResultColumnName}, {@code rowMapper}, or {@code collector} is {@code null}, or if {@code keyColumnNames} or {@code aggregateOnColumnNames} is empty.
     * @see #groupBy(Collection)
     * @see #groupBy(String, String, String, Collector)
     * @see #groupBy(Collection, String, String, Collector)
     * @see #groupBy(Collection, Function, String, String, Collector)
     * @see #rollup(Collection)
     * @see #cube(Collection)
     * @see #pivot(String, String, String, Collector)
     * @see java.util.stream.Collectors
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    <T> Dataset groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the Dataset.
     * <br />
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the rollup operation.
     * The rollup starts with the most detailed level (all specified columns) and progressively removes
     * the rightmost column in each subsequent level, ending with the grand total (no grouping columns).
     * Each level in the rollup contains grouped data with subtotals for that level of granularity.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "USA", "New York", 1000, 50},
     *          {"North", "USA", "Boston", 800, 40},
     *          {"North", "Canada", "Toronto", 600, 30},
     *          {"South", "Mexico", "Mexico City", 400, 20}
     *     });
     *
     * // +--------+---------+-------------+-------+----------+
     * // | region | country | city        | sales | quantity |
     * // +--------+---------+-------------+-------+----------+
     * // | North  | USA     | New York    | 1000  | 50       |
     * // | North  | USA     | Boston      | 800   | 40       |
     * // | North  | Canada  | Toronto     | 600   | 30       |
     * // | South  | Mexico  | Mexico City | 400   | 20       |
     * // +--------+---------+-------------+-------+----------+
     *
     * Stream<Dataset> rollupResult = dataset.rollup(Arrays.asList("region", "country", "city"));
     *
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country, city (most detailed)
     * // +--------+---------+-------------+-------+
     * // | region | country | city        | count |
     * // +--------+---------+-------------+-------+
     * // | North  | USA     | New York    | 1     |
     * // | North  | USA     | Boston      | 1     |
     * // | North  | Canada  | Toronto     | 1     |
     * // | South  | Mexico  | Mexico City | 1     |
     * // +--------+---------+-------------+-------+
     * // Level 2: Grouped by region, country
     * // +--------+---------+-------+
     * // | region | country | count |
     * // +--------+---------+-------+
     * // | North  | USA     | 2     |
     * // | North  | Canada  | 1     |
     * // | South  | Mexico  | 1     |
     * // +--------+---------+-------+
     * // Level 3: Grouped by region
     * // +--------+-------+
     * // | region | count |
     * // +--------+-------+
     * // | North  | 3     |
     * // | South  | 1     |
     * // +--------+-------+
     * // Level 4: Grand total (no grouping)
     * // +-------+
     * // | count |
     * // +-------+
     * // | 4     |
     * // +-------+
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed. Must not be {@code null} or empty.
     * @return A Stream of Datasets, each representing a level of the rollup operation, from most detailed to grand total.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty.
     * @see #groupBy(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> rollup(Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the Dataset using the specified columns with aggregation.
     * <br />
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns, and an aggregation operation is applied
     * to the specified aggregate column using the provided collector.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the rollup operation.
     * The rollup starts with the most detailed level (all specified columns) and progressively removes
     * the rightmost column in each subsequent level, ending with the grand total (no grouping columns).
     * Each level in the rollup contains grouped data with subtotals for that level of granularity.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "USA", "New York", 1000, 50},
     *          {"North", "USA", "Boston", 800, 40},
     *          {"North", "Canada", "Toronto", 600, 30},
     *          {"South", "Mexico", "Mexico City", 400, 20}
     *     });
     *
     * // +--------+---------+-------------+-------+----------+
     * // | region | country | city        | sales | quantity |
     * // +--------+---------+-------------+-------+----------+
     * // | North  | USA     | New York    | 1000  | 50       |
     * // | North  | USA     | Boston      | 800   | 40       |
     * // | North  | Canada  | Toronto     | 600   | 30       |
     * // | South  | Mexico  | Mexico City | 400   | 20       |
     * // +--------+---------+-------------+-------+----------+
     *
     * Stream<Dataset> rollupResult = dataset.rollup(
     *     Arrays.asList("region", "country", "city"),
     *     "sales",
     *     "total_sales",
     *     Collectors.summingInt(Numbers::toInt));
     *
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country, city (most detailed)
     * // +--------+---------+-------------+-------------+
     * // | region | country | city        | total_sales |
     * // +--------+---------+-------------+-------------+
     * // | North  | USA     | New York    | 1000        |
     * // | North  | USA     | Boston      | 800         |
     * // | North  | Canada  | Toronto     | 600         |
     * // | South  | Mexico  | Mexico City | 400         |
     * // +--------+---------+-------------+-------------+
     * // Level 2: Grouped by region, country
     * // +--------+---------+-------------+
     * // | region | country | total_sales |
     * // +--------+---------+-------------+
     * // | North  | USA     | 1800        |
     * // | North  | Canada  | 600         |
     * // | South  | Mexico  | 400         |
     * // +--------+---------+-------------+
     * // Level 3: Grouped by region
     * // +--------+-------------+
     * // | region | total_sales |
     * // +--------+-------------+
     * // | North  | 2400        |
     * // | South  | 400         |
     * // +--------+-------------+
     * // Level 4: Grand total (no grouping)
     * // +-------------+
     * // | total_sales |
     * // +-------------+
     * // | 2800        |
     * // +-------------+
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed. Must not be {@code null}.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param collector The collector defining the aggregate operation. Must not be {@code null}.
     * @return A Stream of Datasets, each representing a level of the rollup operation, from most detailed to grand total.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty, or if {@code aggregateOnColumnName}, {@code aggregateResultColumnName}, or {@code collector} is {@code null}.
     * @see #rollup(Collection)
     * @see #groupBy(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> rollup(Collection<String> keyColumnNames, String aggregateOnColumnName, String aggregateResultColumnName, Collector<?, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the Dataset using the specified columns with aggregation on multiple columns.
     * <br />
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns, and an aggregation operation is applied
     * to the specified aggregate columns. The rows are converted to the specified row type before aggregation.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the rollup operation.
     * The rollup starts with the most detailed level (all specified columns) and progressively removes
     * the rightmost column in each subsequent level, ending with the grand total (no grouping columns).
     * Each level in the rollup contains grouped data with subtotals for that level of granularity.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "USA", "New York", 1000, 50},
     *          {"North", "USA", "Boston", 800, 40},
     *          {"North", "Canada", "Toronto", 600, 30},
     *          {"South", "Mexico", "Mexico City", 400, 20}
     *     });
     *
     * // +--------+---------+-------------+-------+----------+
     * // | region | country | city        | sales | quantity |
     * // +--------+---------+-------------+-------+----------+
     * // | North  | USA     | New York    | 1000  | 50       |
     * // | North  | USA     | Boston      | 800   | 40       |
     * // | North  | Canada  | Toronto     | 600   | 30       |
     * // | South  | Mexico  | Mexico City | 400   | 20       |
     * // +--------+---------+-------------+-------+----------+
     *
     * Stream<Dataset> rollupResult = dataset.rollup(
     *     Arrays.asList("region", "country", "city"),
     *     Arrays.asList("sales", "quantity"),
     *     "aggregated_data",
     *     Object[].class);
     *
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country, city (most detailed)
     * // +--------+---------+-------------+-----------------+
     * // | region | country | city        | aggregated_data |
     * // +--------+---------+-------------+-----------------+
     * // | North  | USA     | New York    | [[1000, 50]]    |
     * // | North  | USA     | Boston      | [[800, 40]]     |
     * // | North  | Canada  | Toronto     | [[600, 30]]     |
     * // | South  | Mexico  | Mexico City | [[400, 20]]     |
     * // +--------+---------+-------------+-----------------+
     * // Level 2: Grouped by region, country
     * // +--------+---------+-------------------------+
     * // | region | country | aggregated_data         |
     * // +--------+---------+-------------------------+
     * // | North  | USA     | [[1000, 50], [800, 40]] |
     * // | North  | Canada  | [[600, 30]]             |
     * // | South  | Mexico  | [[400, 20]]             |
     * // +--------+---------+-------------------------+
     * // Level 3: Grouped by region
     * // +--------+------------------------------------+
     * // | region | aggregated_data                    |
     * // +--------+------------------------------------+
     * // | North  | [[1000, 50], [800, 40], [600, 30]] |
     * // | South  | [[400, 20]]                        |
     * // +--------+------------------------------------+
     * // Level 4: Grand total (no grouping)
     * // +-----------------------------------------------+
     * // | aggregated_data                               |
     * // +-----------------------------------------------+
     * // | [[1000, 50], [800, 40], [600, 30], [400, 20]] |
     * // +-----------------------------------------------+
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param rowType The class of the row type. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of Datasets, each representing a level of the rollup operation, from most detailed to grand total.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty, or {@code aggregateOnColumnNames} is {@code null} or empty, or if {@code aggregateResultColumnName} or {@code rowType} is {@code null}, or if the specified {@code rowType} is not a supported type.
     * @see #rollup(Collection)
     * @see #rollup(Collection, String, String, Collector)
     * @see #groupBy(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> rollup(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName, Class<?> rowType)
            throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the Dataset using the specified columns with aggregation on multiple columns.
     * <br />
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns, and an aggregation operation is applied
     * to the specified aggregate columns using the provided collector.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the rollup operation.
     * The rollup starts with the most detailed level (all specified columns) and progressively removes
     * the rightmost column in each subsequent level, ending with the grand total (no grouping columns).
     * Each level in the rollup contains grouped data with subtotals for that level of granularity.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "USA", "New York", 1000, 50},
     *          {"North", "USA", "Boston", 800, 40},
     *          {"North", "Canada", "Toronto", 600, 30},
     *          {"South", "Mexico", "Mexico City", 400, 20}
     *     });
     *
     * // +--------+---------+-------------+-------+----------+
     * // | region | country | city        | sales | quantity |
     * // +--------+---------+-------------+-------+----------+
     * // | North  | USA     | New York    | 1000  | 50       |
     * // | North  | USA     | Boston      | 800   | 40       |
     * // | North  | Canada  | Toronto     | 600   | 30       |
     * // | South  | Mexico  | Mexico City | 400   | 20       |
     * // +--------+---------+-------------+-------+----------+
     *
     * Stream<Dataset> rollupResult = dataset.rollup(
     *     Arrays.asList("region", "country", "city"),
     *     Arrays.asList("sales", "quantity"),
     *     "aggregated_totals",
     *     MoreCollectors.summingInt(a -> (Integer) a[0], a -> (Integer) a[1]));
     *
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country, city (most detailed)
     * // +--------+---------+-------------+-----------------+
     * // | region | country | city        | aggregated_data |
     * // +--------+---------+-------------+-----------------+
     * // | North  | USA     | New York    | (1000, 50)      |
     * // | North  | USA     | Boston      | (800, 40)       |
     * // | North  | Canada  | Toronto     | (600, 30)       |
     * // | South  | Mexico  | Mexico City | (400, 20)       |
     * // +--------+---------+-------------+-----------------+
     * // Level 2: Grouped by region, country
     * // +--------+---------+-----------------+
     * // | region | country | aggregated_data |
     * // +--------+---------+-----------------+
     * // | North  | USA     | (1800, 90)      |
     * // | North  | Canada  | (600, 30)       |
     * // | South  | Mexico  | (400, 20)       |
     * // +--------+---------+-----------------+
     * // Level 3: Grouped by region
     * // +--------+-----------------+
     * // | region | aggregated_data |
     * // +--------+-----------------+
     * // | North  | (2400, 120)     |
     * // | South  | (400, 20)       |
     * // +--------+-----------------+
     * // Level 4: Grand total (no grouping)
     * // +-----------------+
     * // | aggregated_data |
     * // +-----------------+
     * // | (2800, 140)     |
     * // +-----------------+
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param collector The collector defining the aggregate operation. Must not be {@code null}.
     * @return A Stream of Datasets, each representing a level of the rollup operation, from most detailed to grand total.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty, or {@code aggregateOnColumnNames} is {@code null} or empty, or if {@code aggregateResultColumnName} or {@code collector} is {@code null}.
     * @see #rollup(Collection)
     * @see #rollup(Collection, String, String, Collector)
     * @see #rollup(Collection, Collection, String, Class)
     * @see #groupBy(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> rollup(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the Dataset using the specified columns with aggregation on multiple columns using a custom row mapper.
     * <br />
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns, and an aggregation operation is applied
     * to the specified aggregate columns using the provided row mapper and collector.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the rollup operation.
     * The rollup starts with the most detailed level (all specified columns) and progressively removes
     * the rightmost column in each subsequent level, ending with the grand total (no grouping columns).
     * Each level in the rollup contains grouped data with subtotals for that level of granularity.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "USA", "New York", 1000, 50},
     *          {"North", "USA", "Boston", 800, 40},
     *          {"North", "Canada", "Toronto", 600, 30},
     *          {"South", "Mexico", "Mexico City", 400, 20}
     *     });
     *
     * // +--------+---------+-------------+-------+----------+
     * // | region | country | city        | sales | quantity |
     * // +--------+---------+-------------+-------+----------+
     * // | North  | USA     | New York    | 1000  | 50       |
     * // | North  | USA     | Boston      | 800   | 40       |
     * // | North  | Canada  | Toronto     | 600   | 30       |
     * // | South  | Mexico  | Mexico City | 400   | 20       |
     * // +--------+---------+-------------+-------+----------+
     *
     * Stream<Dataset> rollupResult = dataset.rollup(
     *     Arrays.asList("region", "country", "city"),
     *     Arrays.asList("sales", "quantity"),
     *     "aggregated_totals",
     *     row -> Tuple.of((Integer) row.get(0), (Integer) row.get(1)),
     *     MoreCollectors.summingInt(tp -> tp._1, tp -> tp._2));
     *
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country, city (most detailed)
     * // +--------+---------+-------------+-----------------+
     * // | region | country | city        | aggregated_data |
     * // +--------+---------+-------------+-----------------+
     * // | North  | USA     | New York    | (1000, 50)      |
     * // | North  | USA     | Boston      | (800, 40)       |
     * // | North  | Canada  | Toronto     | (600, 30)       |
     * // | South  | Mexico  | Mexico City | (400, 20)       |
     * // +--------+---------+-------------+-----------------+
     * // Level 2: Grouped by region, country
     * // +--------+---------+-----------------+
     * // | region | country | aggregated_data |
     * // +--------+---------+-----------------+
     * // | North  | USA     | (1800, 90)      |
     * // | North  | Canada  | (600, 30)       |
     * // | South  | Mexico  | (400, 20)       |
     * // +--------+---------+-----------------+
     * // Level 3: Grouped by region
     * // +--------+-----------------+
     * // | region | aggregated_data |
     * // +--------+-----------------+
     * // | North  | (2400, 120)     |
     * // | South  | (400, 20)       |
     * // +--------+-----------------+
     * // Level 4: Grand total (no grouping)
     * // +-----------------+
     * // | aggregated_data |
     * // +-----------------+
     * // | (2800, 140)     |
     * // +-----------------+
     * }</pre>
     *
     * @param <T> The type of the object that the row data will be mapped to.
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param rowMapper The function to transform the DisposableObjArray to a custom type T. Must not be {@code null}.
     * @param collector The collector that defines the aggregate operation. Must not be {@code null}.
     * @return A Stream of Datasets, each representing a level of the rollup operation, from most detailed to grand total.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty, or {@code aggregateOnColumnNames} is {@code null} or empty, or if {@code aggregateResultColumnName}, {@code rowMapper}, or {@code collector} is {@code null}.
     * @see #rollup(Collection)
     * @see #rollup(Collection, String, String, Collector)
     * @see #rollup(Collection, Collection, String, Class)
     * @see #rollup(Collection, Collection, String, Collector)
     * @see #groupBy(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    <T> Stream<Dataset> rollup(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the Dataset using the specified columns with a custom key extractor function.
     * <br />
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns, and the key extractor function is used
     * to transform the DisposableObjArray to a custom key for grouping purposes.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the rollup operation.
     * The rollup starts with the most detailed level (all specified columns) and progressively removes
     * the rightmost column in each subsequent level, ending with the grand total (no grouping columns).
     * Each level in the rollup contains grouped data with subtotals for that level of granularity.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "USA", "New York", 1000, 50},
     *          {"North", "USA", "Boston", 800, 40},
     *          {"North", "Canada", "Toronto", 600, 30},
     *          {"South", "Mexico", "Mexico City", 400, 20}
     *     });
     *
     * // +--------+---------+-------------+-------+----------+
     * // | region | country | city        | sales | quantity |
     * // +--------+---------+-------------+-------+----------+
     * // | North  | USA     | New York    | 1000  | 50       |
     * // | North  | USA     | Boston      | 800   | 40       |
     * // | North  | Canada  | Toronto     | 600   | 30       |
     * // | South  | Mexico  | Mexico City | 400   | 20       |
     * // +--------+---------+-------------+-------+----------+
     *
     * Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
     *
     * Stream<Dataset> rollupResult = dataset.rollup(
     *     Arrays.asList("region", "country", "city"),
     *     keyExtractor);
     *
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country, city (most detailed)
     * // +--------+---------+-------------+-------+
     * // | region | country | city        | count |
     * // +--------+---------+-------------+-------+
     * // | North  | USA     | New York    | 1     |
     * // | North  | USA     | Boston      | 1     |
     * // | North  | Canada  | Toronto     | 1     |
     * // | South  | Mexico  | Mexico City | 1     |
     * // +--------+---------+-------------+-------+
     * // Level 2: Grouped by region, country
     * // +--------+---------+-------+
     * // | region | country | count |
     * // +--------+---------+-------+
     * // | North  | USA     | 2     |
     * // | North  | Canada  | 1     |
     * // | South  | Mexico  | 1     |
     * // +--------+---------+-------+
     * // Level 3: Grouped by region
     * // +--------+-------+
     * // | region | count |
     * // +--------+-------+
     * // | North  | 3     |
     * // | South  | 1     |
     * // +--------+-------+
     * // Level 4: Grand total (no grouping)
     * // +-------+
     * // | count |
     * // +-------+
     * // | 4     |
     * // +-------+
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed. Must not be {@code null} or empty.
     * @param keyExtractor The function to transform the DisposableObjArray to a custom key for grouping purposes. Must not be {@code null}.
     * @return A Stream of Datasets, each representing a level of the rollup operation, from most detailed to grand total.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty, or if {@code keyExtractor} is {@code null}.
     * @see #rollup(Collection)
     * @see #rollup(Collection, String, String, Collector)
     * @see #rollup(Collection, Collection, String, Class)
     * @see #rollup(Collection, Collection, String, Collector)
     * @see #rollup(Collection, Collection, String, Function, Collector)
     * @see #groupBy(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> rollup(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the Dataset using the specified columns with a key extractor function and aggregation on a single column.
     * <br />
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns, and the key extractor function is used
     * to transform the DisposableObjArray to a custom key for grouping purposes.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the rollup operation.
     * The rollup starts with the most detailed level (all specified columns) and progressively removes
     * the rightmost column in each subsequent level, ending with the grand total (no grouping columns).
     * Each level in the rollup contains grouped data with subtotals for that level of granularity.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "USA", "New York", 1000, 50},
     *          {"North", "USA", "Boston", 800, 40},
     *          {"North", "Canada", "Toronto", 600, 30},
     *          {"South", "Mexico", "Mexico City", 400, 20}
     *     });
     *
     * // +--------+---------+-------------+-------+----------+
     * // | region | country | city        | sales | quantity |
     * // +--------+---------+-------------+-------+----------+
     * // | North  | USA     | New York    | 1000  | 50       |
     * // | North  | USA     | Boston      | 800   | 40       |
     * // | North  | Canada  | Toronto     | 600   | 30       |
     * // | South  | Mexico  | Mexico City | 400   | 20       |
     * // +--------+---------+-------------+-------+----------+
     *
     * Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
     *
     * Stream<Dataset> rollupResult = dataset.rollup(
     *     Arrays.asList("region", "country", "city"),
     *     keyExtractor,
     *     "sales",
     *     "total_sales",
     *     Collectors.summingInt(Numbers::toInt));
     *
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country, city (most detailed)
     * // +--------+---------+-------------+-------------+
     * // | region | country | city        | total_sales |
     * // +--------+---------+-------------+-------------+
     * // | North  | USA     | New York    | 1000        |
     * // | North  | USA     | Boston      | 800         |
     * // | North  | Canada  | Toronto     | 600         |
     * // | South  | Mexico  | Mexico City | 400         |
     * // +--------+---------+-------------+-------------+
     * // Level 2: Grouped by region, country
     * // +--------+---------+-------------+
     * // | region | country | total_sales |
     * // +--------+---------+-------------+
     * // | North  | USA     | 1800        |
     * // | North  | Canada  | 600         |
     * // | South  | Mexico  | 400         |
     * // +--------+---------+-------------+
     * // Level 3: Grouped by region
     * // +--------+-------------+
     * // | region | total_sales |
     * // +--------+-------------+
     * // | North  | 2400        |
     * // | South  | 400         |
     * // +--------+-------------+
     * // Level 4: Grand total (no grouping)
     * // +-------------+
     * // | total_sales |
     * // +-------------+
     * // | 2800        |
     * // +-------------+
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed. Must not be {@code null} or empty.
     * @param keyExtractor The function to transform the DisposableObjArray to a custom key for grouping purposes. Must not be {@code null}.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed. Must not be {@code null}.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param collector The collector that defines the aggregate operation. Must not be {@code null}.
     * @return A Stream of Datasets, each representing a level of the rollup operation, from most detailed to grand total.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty, or if {@code keyExtractor}, {@code aggregateOnColumnName}, {@code aggregateResultColumnName}, or {@code collector} is {@code null}.
     * @see #rollup(Collection)
     * @see #rollup(Collection, String, String, Collector)
     * @see #rollup(Collection, Collection, String, Class)
     * @see #rollup(Collection, Collection, String, Collector)
     * @see #rollup(Collection, Collection, String, Function, Collector)
     * @see #rollup(Collection, Function)
     * @see #groupBy(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> rollup(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, String aggregateOnColumnName,
            String aggregateResultColumnName, Collector<?, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the Dataset using the specified columns with a key extractor function and aggregation on multiple columns.
     * <br />
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns, and the key extractor function is used
     * to transform the DisposableObjArray to a custom key for grouping purposes.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the rollup operation.
     * The rollup starts with the most detailed level (all specified columns) and progressively removes
     * the rightmost column in each subsequent level, ending with the grand total (no grouping columns).
     * Each level in the rollup contains grouped data with subtotals for that level of granularity.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     * The aggregation operation is defined by the provided rowType.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "USA", "New York", 1000, 50},
     *          {"North", "USA", "Boston", 800, 40},
     *          {"North", "Canada", "Toronto", 600, 30},
     *          {"South", "Mexico", "Mexico City", 400, 20}
     *     });
     *
     * // +--------+---------+-------------+-------+----------+
     * // | region | country | city        | sales | quantity |
     * // +--------+---------+-------------+-------+----------+
     * // | North  | USA     | New York    | 1000  | 50       |
     * // | North  | USA     | Boston      | 800   | 40       |
     * // | North  | Canada  | Toronto     | 600   | 30       |
     * // | South  | Mexico  | Mexico City | 400   | 20       |
     * // +--------+---------+-------------+-------+----------+
     *
     * Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
     *
     * Stream<Dataset> rollupResult = dataset.rollup(
     *     Arrays.asList("region", "country", "city"),
     *     keyExtractor,
     *     Arrays.asList("sales", "quantity"),
     *     "aggregated_data",
     *     Object[].class);
     *
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country, city (most detailed)
     * // +--------+---------+-------------+-----------------+
     * // | region | country | city        | aggregated_data |
     * // +--------+---------+-------------+-----------------+
     * // | North  | USA     | New York    | [[1000, 50]]    |
     * // | North  | USA     | Boston      | [[800, 40]]     |
     * // | North  | Canada  | Toronto     | [[600, 30]]     |
     * // | South  | Mexico  | Mexico City | [[400, 20]]     |
     * // +--------+---------+-------------+-----------------+
     * // Level 2: Grouped by region, country
     * // +--------+---------+-------------------------+
     * // | region | country | aggregated_data         |
     * // +--------+---------+-------------------------+
     * // | North  | USA     | [[1000, 50], [800, 40]] |
     * // | North  | Canada  | [[600, 30]]             |
     * // | South  | Mexico  | [[400, 20]]             |
     * // +--------+---------+-------------------------+
     * // Level 3: Grouped by region
     * // +--------+------------------------------------+
     * // | region | aggregated_data                    |
     * // +--------+------------------------------------+
     * // | North  | [[1000, 50], [800, 40], [600, 30]] |
     * // | South  | [[400, 20]]                        |
     * // +--------+------------------------------------+
     * // Level 4: Grand total (no grouping)
     * // +-----------------------------------------------+
     * // | aggregated_data                               |
     * // +-----------------------------------------------+
     * // | [[1000, 50], [800, 40], [600, 30], [400, 20]] |
     * // +-----------------------------------------------+
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed. Must not be {@code null} or empty.
     * @param keyExtractor The function to transform the DisposableObjArray to a custom key for grouping purposes. Must not be {@code null}.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param rowType The class of the row type that defines the aggregate operation. It must be one of the supported types - Object[], Collection, Map, or Bean class. Must not be {@code null}.
     * @return A Stream of Datasets, each representing a level of the rollup operation, from most detailed to grand total.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty, or {@code aggregateOnColumnNames} is {@code null} or empty, or if {@code keyExtractor}, {@code aggregateResultColumnName}, or {@code rowType} is {@code null}, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see #rollup(Collection)
     * @see #rollup(Collection, String, String, Collector)
     * @see #rollup(Collection, Collection, String, Class)
     * @see #rollup(Collection, Collection, String, Collector)
     * @see #rollup(Collection, Collection, String, Function, Collector)
     * @see #rollup(Collection, Function)
     * @see #rollup(Collection, Function, String, String, Collector)
     * @see #groupBy(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> rollup(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Class<?> rowType) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the Dataset using the specified columns with a key extractor function and aggregation on multiple columns with a custom collector.
     * <br />
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns, and the key extractor function is used
     * to transform the DisposableObjArray to a custom key for grouping purposes.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the rollup operation.
     * The rollup starts with the most detailed level (all specified columns) and progressively removes
     * the rightmost column in each subsequent level, ending with the grand total (no grouping columns).
     * Each level in the rollup contains grouped data with subtotals for that level of granularity.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     * The aggregation operation is defined by the provided collector.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "USA", "New York", 1000, 50},
     *          {"North", "USA", "Boston", 800, 40},
     *          {"North", "Canada", "Toronto", 600, 30},
     *          {"South", "Mexico", "Mexico City", 400, 20}
     *     });
     *
     * // +--------+---------+-------------+-------+----------+
     * // | region | country | city        | sales | quantity |
     * // +--------+---------+-------------+-------+----------+
     * // | North  | USA     | New York    | 1000  | 50       |
     * // | North  | USA     | Boston      | 800   | 40       |
     * // | North  | Canada  | Toronto     | 600   | 30       |
     * // | South  | Mexico  | Mexico City | 400   | 20       |
     * // +--------+---------+-------------+-------+----------+
     *
     * Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
     *
     * Stream<Dataset> rollupResult = dataset.rollup(
     *     Arrays.asList("region", "country", "city"),
     *     keyExtractor,
     *     Arrays.asList("sales", "quantity"),
     *     "aggregated_data",
     *     MoreCollectors.summingInt(a -> (Integer) a[0], a -> (Integer) a[1]));
     *
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country, city (most detailed)
     * // +--------+---------+-------------+-----------------+
     * // | region | country | city        | aggregated_data |
     * // +--------+---------+-------------+-----------------+
     * // | North  | USA     | New York    | (1000, 50)      |
     * // | North  | USA     | Boston      | (800, 40)       |
     * // | North  | Canada  | Toronto     | (600, 30)       |
     * // | South  | Mexico  | Mexico City | (400, 20)       |
     * // +--------+---------+-------------+-----------------+
     * // Level 2: Grouped by region, country
     * // +--------+---------+-----------------+
     * // | region | country | aggregated_data |
     * // +--------+---------+-----------------+
     * // | North  | USA     | (1800, 90)      |
     * // | North  | Canada  | (600, 30)       |
     * // | South  | Mexico  | (400, 20)       |
     * // +--------+---------+-----------------+
     * // Level 3: Grouped by region
     * // +--------+-----------------+
     * // | region | aggregated_data |
     * // +--------+-----------------+
     * // | North  | (2400, 120)     |
     * // | South  | (400, 20)       |
     * // +--------+-----------------+
     * // Level 4: Grand total (no grouping)
     * // +-----------------+
     * // | aggregated_data |
     * // +-----------------+
     * // | (2800, 140)     |
     * // +-----------------+
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed. Must not be {@code null} or empty.
     * @param keyExtractor The function to transform the DisposableObjArray to a custom key for grouping purposes. Must not be {@code null}.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param collector The collector that defines the aggregate operation. Must not be {@code null}.
     * @return A Stream of Datasets, each representing a level of the rollup operation, from most detailed to grand total.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty, or {@code aggregateOnColumnNames} is {@code null} or empty, or if {@code keyExtractor}, {@code aggregateResultColumnName}, or {@code collector} is {@code null}.
     * @see #rollup(Collection)
     * @see #rollup(Collection, String, String, Collector)
     * @see #rollup(Collection, Collection, String, Class)
     * @see #rollup(Collection, Collection, String, Collector)
     * @see #rollup(Collection, Collection, String, Function, Collector)
     * @see #rollup(Collection, Function)
     * @see #rollup(Collection, Function, String, String, Collector)
     * @see #rollup(Collection, Function, Collection, String, Class)
     * @see #groupBy(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> rollup(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a rollup operation on the Dataset using the specified columns, custom key extractor, row mapper function, and a collector for aggregate operations.
     * <br />
     * A rollup is a form of data summarization that aggregates data by ascending levels of granularity.
     * The rollup operation is performed on the specified columns, and the key extractor function is used to transform
     * the DisposableObjArray to a custom key for grouping.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the rollup operation.
     * The rollup starts with the most detailed level (all specified columns) and progressively removes
     * the rightmost column in each subsequent level, ending with the grand total (no grouping columns).
     * The keyExtractor function allows for custom transformation of row data before grouping.
     * The rowMapper function transforms the aggregate data before applying the collector.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "USA", "New York", 1000, 50},
     *          {"North", "USA", "Boston", 800, 40},
     *          {"North", "Canada", "Toronto", 600, 30},
     *          {"South", "Mexico", "Mexico City", 400, 20}
     *     });
     *
     * // +--------+---------+-------------+-------+----------+
     * // | region | country | city        | sales | quantity |
     * // +--------+---------+-------------+-------+----------+
     * // | North  | USA     | New York    | 1000  | 50       |
     * // | North  | USA     | Boston      | 800   | 40       |
     * // | North  | Canada  | Toronto     | 600   | 30       |
     * // | South  | Mexico  | Mexico City | 400   | 20       |
     * // +--------+---------+-------------+-------+----------+
     *
     * Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
     * Stream<Dataset> rollupResult = dataset.rollup(
     *     Arrays.asList("region", "country", "city"),
     *     keyExtractor,
     *     Arrays.asList("sales", "quantity"),
     *     "aggregated_totals",
     *     row -> Tuple.of((Integer) row.get(0), (Integer) row.get(1)),
     *     MoreCollectors.summingInt(tp -> tp._1, tp -> tp._2));
     *
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country, city (most detailed)
     * // +--------+---------+-------------+-----------------+
     * // | region | country | city        | aggregated_data |
     * // +--------+---------+-------------+-----------------+
     * // | North  | USA     | New York    | (1000, 50)      |
     * // | North  | USA     | Boston      | (800, 40)       |
     * // | North  | Canada  | Toronto     | (600, 30)       |
     * // | South  | Mexico  | Mexico City | (400, 20)       |
     * // +--------+---------+-------------+-----------------+
     * // Level 2: Grouped by region, country
     * // +--------+---------+-----------------+
     * // | region | country | aggregated_data |
     * // +--------+---------+-----------------+
     * // | North  | USA     | (1800, 90)      |
     * // | North  | Canada  | (600, 30)       |
     * // | South  | Mexico  | (400, 20)       |
     * // +--------+---------+-----------------+
     * // Level 3: Grouped by region
     * // +--------+-----------------+
     * // | region | aggregated_data |
     * // +--------+-----------------+
     * // | North  | (2400, 120)     |
     * // | South  | (400, 20)       |
     * // +--------+-----------------+
     * // Level 4: Grand total (no grouping)
     * // +-----------------+
     * // | aggregated_data |
     * // +-----------------+
     * // | (2800, 140)     |
     * // +-----------------+
     * }</pre>
     *
     * @param <T> The type of elements produced by the row mapper function.
     * @param keyColumnNames The names of the columns on which the rollup operation is to be performed. Must not be {@code null} or empty.
     * @param keyExtractor The function to transform the DisposableObjArray to a custom key for grouping. Must not be {@code null}.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param rowMapper The function to transform the DisposableObjArray to a custom row before aggregation. Must not be {@code null}.
     * @param collector The collector defining the aggregate operation. Must not be {@code null}.
     * @return A Stream of Datasets, each representing a level of the rollup operation, from most detailed to grand total.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty, or {@code aggregateOnColumnNames} is {@code null} or empty, or if {@code keyExtractor}, {@code rowMapper}, {@code collector}, or {@code aggregateResultColumnName} is {@code null}.
     * @see #rollup(Collection)
     * @see #rollup(Collection, Function)
     * @see #groupBy(Collection, Function)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    <T> Stream<Dataset> rollup(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor,
            Collection<String> aggregateOnColumnNames, String aggregateResultColumnName, Function<? super DisposableObjArray, ? extends T> rowMapper,
            Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the Dataset using the specified columns.
     * <br />
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * Unlike rollup which creates hierarchical subtotals, cube creates subtotals for every possible combination of the grouping columns.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the cube operation.
     * The cube operation generates 2^n datasets where n is the number of key columns, representing all possible
     * combinations of grouping columns including the grand total (no grouping columns).
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "sales"),
     *     new Object[][] {
     *          {"North", "USA", 1000},
     *          {"North", "Canada", 600},
     *          {"South", "Mexico", 400}
     *     });
     *
     * // +--------+---------+-------+
     * // | region | country | sales |
     * // +--------+---------+-------+
     * // | North  | USA     | 1000  |
     * // | North  | Canada  | 600   |
     * // | South  | Mexico  | 400   |
     * // +--------+---------+-------+
     * 
     * Stream<Dataset> cubeResult = dataset.cube(Arrays.asList("region", "country"));
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country (most detailed)
     * // +--------+---------+-------+
     * // | region | country | count |
     * // +--------+---------+-------+
     * // | North  | USA     | 1     |
     * // | North  | Canada  | 1     |
     * // | South  | Mexico  | 1     |
     * // +--------+---------+-------+
     * // Level 2: Grouped by region only
     * // +--------+-------+
     * // | region | count |
     * // +--------+-------+
     * // | North  | 2     |
     * // | South  | 1     |
     * // +--------+-------+
     * // Level 3: Grouped by country only  
     * // +---------+-------+
     * // | country | count |
     * // +---------+-------+
     * // | USA     | 1     |
     * // | Canada  | 1     |
     * // | Mexico  | 1     |
     * // +---------+-------+
     * // Level 4: Grand total (no grouping)
     * // +-------+
     * // | count |
     * // +-------+
     * // | 3     |
     * // +-------+
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed. Must not be {@code null} or empty.
     * @return A Stream of Datasets, each representing a level of the cube operation, covering all possible combinations of the specified columns.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty.
     * @see #rollup(Collection)
     * @see #cube(Collection, Function, Collection, String, Function, Collector)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> cube(Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the Dataset using the specified columns and an aggregate operation.
     * <br />
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the cube operation.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     * The aggregation operation is defined by the provided collector.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "amount"), new Object[][] {{"APAC", "A", 42}});
     * Stream<Dataset> levels = dataset.cube(Arrays.asList("region", "product"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of Datasets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty.
     * @see #cube(Collection)
     * @see #cube(Collection, Function, Collection, String, Function, Collector)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> cube(Collection<String> keyColumnNames, String aggregateOnColumnName, String aggregateResultColumnName, Collector<?, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Performs a cube operation on the Dataset using the specified columns and an aggregate operation.
     * <br />
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the cube operation.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     * The type of the new column is defined by the provided Class.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "amount"), new Object[][] {{"APAC", "A", 42}});
     * Stream<Dataset> levels = dataset.cube(Arrays.asList("region", "product"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The Class defining the type of the new column. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of Datasets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see #cube(Collection)
     * @see #cube(Collection, Function, Collection, String, Function, Collector)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> cube(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName, Class<?> rowType)
            throws IllegalArgumentException;

    /**
     * Performs a cube operation on the Dataset using the specified columns and an aggregate operation.
     * <br />
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the cube operation.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     * The aggregation operation is defined by the provided collector.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "amount"), new Object[][] {{"APAC", "A", 42}});
     * Stream<Dataset> levels = dataset.cube(Arrays.asList("region", "product"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of Datasets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see #cube(Collection)
     * @see #cube(Collection, Function, Collection, String, Function, Collector)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> cube(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the Dataset using the specified columns and an aggregate operation.
     * <br />
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the cube operation.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     * The aggregation operation is defined by the provided collector.
     * The rowMapper function is used to transform the DisposableObjArray to a type T before the aggregation operation.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "amount"), new Object[][] {{"APAC", "A", 42}});
     * Stream<Dataset> levels = dataset.cube(Arrays.asList("region", "product"));
     * }</pre>
     *
     * @param <T> The type of the object that the row data will be mapped to.
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowMapper The function to transform the DisposableObjArray to a type T before the aggregation operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of Datasets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see #cube(Collection)
     * @see #cube(Collection, Function, Collection, String, Function, Collector)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    <T> Stream<Dataset> cube(Collection<String> keyColumnNames, Collection<String> aggregateOnColumnNames, String aggregateResultColumnName,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the Dataset using the specified columns and a key mapper function.
     * <br />
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the cube operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a key before the cube operation.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "amount"), new Object[][] {{"APAC", "A", 42}});
     * Stream<Dataset> levels = dataset.cube(Arrays.asList("region", "product"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a key before the cube operation.
     * @return A Stream of Datasets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty
     * @see #cube(Collection)
     * @see #cube(Collection, Function, Collection, String, Function, Collector)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> cube(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the Dataset using the specified columns, a key mapper function, and an aggregate operation.
     * <br />
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the cube operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a key before the cube operation.
     * The results of the aggregation are stored in a new column in the Dataset.
     * The aggregation operation is defined by the provided collector.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "amount"), new Object[][] {{"APAC", "A", 42}});
     * Stream<Dataset> levels = dataset.cube(Arrays.asList("region", "product"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a key before the cube operation.
     * @param aggregateOnColumnName The name of the column on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of Datasets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty.
     * @see #cube(Collection)
     * @see #cube(Collection, Function, Collection, String, Function, Collector)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> cube(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, String aggregateOnColumnName,
            String aggregateResultColumnName, Collector<?, ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the Dataset using the specified columns, a key mapper function, and a row type.
     * <br />
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the cube operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a key before the cube operation.
     * The results of the aggregation are stored in a new column in the Dataset.
     * The row type defines the type of the rows in the resulting Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "amount"), new Object[][] {{"APAC", "A", 42}});
     * Stream<Dataset> levels = dataset.cube(Arrays.asList("region", "product"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a key before the cube operation.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param rowType The class of the rows in the resulting Dataset. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of Datasets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty, or if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see #cube(Collection)
     * @see #cube(Collection, Function, Collection, String, Function, Collector)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> cube(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Class<?> rowType) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the Dataset using the specified columns, a key mapper function, and a collector.
     * <br />
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * The cube operation is performed on the specified columns.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the cube operation.
     * The keyExtractor function is used to transform the DisposableObjArray to a key before the cube operation.
     * The results of the aggregation are stored in a new column in the Dataset.
     * The collector defines the aggregate operation.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "amount"), new Object[][] {{"APAC", "A", 42}});
     * Stream<Dataset> levels = dataset.cube(Arrays.asList("region", "product"));
     * }</pre>
     *
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed.
     * @param keyExtractor The function to transform the DisposableObjArray to a key before the cube operation.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation.
     * @param collector The collector defining the aggregate operation.
     * @return A Stream of Datasets, each representing a level of the cube operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is empty or {@code aggregateOnColumnNames} is empty.
     * @see #cube(Collection)
     * @see #cube(Collection, Function, Collection, String, Function, Collector)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    Stream<Dataset> cube(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException;

    /**
     * Performs a cube operation on the Dataset using the specified columns with a key extractor function, row mapper function, and aggregation on multiple columns with a custom collector.
     * <br />
     * A cube operation is a form of data summarization that aggregates data by all possible combinations of the specified columns.
     * Unlike rollup which creates hierarchical subtotals, cube creates subtotals for every possible combination of the grouping columns.
     * <br />
     * This method returns a Stream of Datasets, where each Dataset represents a level of the cube operation.
     * The cube operation generates 2^n datasets where n is the number of key columns, representing all possible
     * combinations of grouping columns including the grand total (no grouping columns).
     * The key extractor function is used to transform the DisposableObjArray to a custom key for grouping purposes.
     * The row mapper function is used to transform the aggregate input data before applying the collector.
     * The results of the aggregation are stored in a new column in each of these Datasets.
     * The aggregation operation is defined by the provided collector.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "sales"),
     *     new Object[][] {
     *          {"North", "USA", 1000},
     *          {"North", "Canada", 600},
     *          {"South", "Mexico", 400}
     *     });
     *
     * // +--------+---------+-------+
     * // | region | country | sales |
     * // +--------+---------+-------+
     * // | North  | USA     | 1000  |
     * // | North  | Canada  | 600   |
     * // | South  | Mexico  | 400   |
     * // +--------+---------+-------+ 
     *
     * Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
     * Function<DisposableObjArray, Double> rowMapper = row -> (Integer) row.get(0) * 1.1;
     * Stream<Dataset> cubeResult = dataset.cube(
     *     Arrays.asList("region", "country"),
     *     keyExtractor,
     *     Arrays.asList("sales"),
     *     "total_sales_with_markup",
     *     rowMapper,
     *     Collectors.collectingAndThen(Collectors.summingDouble(Double::doubleValue), r -> Numbers.round(r, 2)));
     * // Returns a stream of 4 datasets:
     * // Level 1: Grouped by region, country (most detailed)
     * // +--------+---------+-------------------------+
     * // | region | country | total_sales_with_markup |
     * // +--------+---------+-------------------------+
     * // | North  | USA     | 1100.0                  |
     * // | North  | Canada  | 660.0                   |
     * // | South  | Mexico  | 440.0                   |
     * // +--------+---------+-------------------------+
     * // Level 2: Grouped by region only
     * // +--------+-------------------------+
     * // | region | total_sales_with_markup |
     * // +--------+-------------------------+
     * // | North  | 1760.0                  |
     * // | South  | 440.0                   |
     * // +--------+-------------------------+
     * // Level 3: Grouped by country only
     * // +---------+-------------------------+
     * // | country | total_sales_with_markup |
     * // +---------+-------------------------+
     * // | USA     | 1100.0                  |
     * // | Canada  | 660.0                   |
     * // | Mexico  | 440.0                   |
     * // +---------+-------------------------+
     * // Level 4: Grand total (no grouping)
     * // +-------------------------+
     * // | total_sales_with_markup |
     * // +-------------------------+
     * // | 2200.0                  |
     * // +-------------------------+
     * }</pre>
     *
     * @param <T> The type of the object that the row data will be mapped to by the row mapper function.
     * @param keyColumnNames The names of the columns on which the cube operation is to be performed. Must not be {@code null} or empty.
     * @param keyExtractor The function to transform the DisposableObjArray to a custom key for grouping purposes. Must not be {@code null}.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed. Must not be {@code null} or empty.
     * @param aggregateResultColumnName The name of the new column that will store the result of the aggregate operation. Must not be {@code null}.
     * @param rowMapper The function to transform the DisposableObjArray to a mapped object before applying the collector. Must not be {@code null}.
     * @param collector The collector that defines the aggregate operation. Must not be {@code null}.
     * @return A Stream of Datasets, each representing a level of the cube operation, covering all possible combinations of the specified columns.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code keyColumnNames} is {@code null} or empty, or {@code aggregateOnColumnNames} is {@code null} or empty, or if {@code keyExtractor}, {@code aggregateResultColumnName}, {@code rowMapper}, or {@code collector} is {@code null}.
     * @see #rollup(Collection, Function, Collection, String, Function, Collector)
     * @see #cube(Collection, Function, Collection, String, Function, Collector)
     * @see <a href="https://stackoverflow.com/questions/37975227">What is the difference between cube, rollup and groupBy operators?</a>
     */
    @Beta
    <T> Stream<Dataset> cube(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyExtractor, Collection<String> aggregateOnColumnNames,
            String aggregateResultColumnName, Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector)
            throws IllegalArgumentException;

    /**
     * Performs a pivot operation on the Dataset using the specified key column, aggregate column, pivot column, and a collector.
     * <br />
     * A pivot operation is a form of data summarization that rotates the data from a state of rows to a state of columns,
     * providing a multidimensional analysis.
     * <br />
     * This method returns a Sheet, where each cell represents an aggregation result.
     * The keyColumnName is used as the row identifier in the resulting Sheet.
     * The aggregateOnColumnNames is the column on which the aggregate operation is to be performed.
     * The pivotColumnName is used as the column identifier in the resulting Sheet.
     * The collector defines the aggregate operation.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "sales"),
     *     new Object[][] {
     *          {"North", "A", 100},
     *          {"North", "B", 200},
     *          {"South", "A", 150},
     *          {"South", "B", 250}
     *     });
     *
     * // +--------+---------+-------+
     * // | region | product | sales |
     * // +--------+---------+-------+
     * // | North  | A       | 100   |
     * // | North  | B       | 200   |
     * // | South  | A       | 150   |
     * // | South  | B       | 250   |
     * // +--------+---------+-------+
     *
     * Sheet<String, String, Integer> pivotResult = dataset.pivot(
     *     "region",      // row identifier
     *     "product",     // column identifier
     *     "sales",       // aggregate column
     *     Collectors.summingInt(Integer::intValue));
     *
     * // Result Sheet:
     * //         +-----+-----+
     * //         | A   | B   |
     * // +-------+-----+-----+
     * // | North | 100 | 200 |
     * // | South | 150 | 250 |
     * // +-------+-----+-----+
     * }</pre>
     *
     * @param <R> The type of the row identifier in the resulting Sheet.
     * @param <C> The type of the column identifier in the resulting Sheet.
     * @param <T> The type of the aggregation result in the resulting Sheet.
     * @param keyColumnName The name of the column to be used as the row identifier in the resulting Sheet. Must not be {@code null}.
     * @param pivotColumnName The name of the column to be used as the column identifier in the resulting Sheet. Must not be {@code null}.
     * @param aggregateOnColumnNames The name of the column on which the aggregate operation is to be performed. Must not be {@code null}.
     * @param collector The collector defining the aggregate operation. Must not be {@code null}.
     * @return A Sheet representing the result of the pivot operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or if {@code keyColumnName}, {@code aggregateOnColumnNames}, {@code pivotColumnName}, or {@code collector} is {@code null}.
     * @see #groupBy(Collection)
     * @see #rollup(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/34702815">Difference between groupby and pivot_table for pandas dataframes</a>
     */
    @Beta
    <R, C, T> Sheet<R, C, T> pivot(String keyColumnName, String pivotColumnName, String aggregateOnColumnNames, Collector<?, ?, ? extends T> collector)
            throws IllegalArgumentException;

    /**
     * Performs a pivot operation on the Dataset using the specified key column, aggregate columns, pivot column, and a collector.
     * <br />
     * A pivot operation is a form of data summarization that rotates the data from a state of rows to a state of columns,
     * providing a multidimensional analysis.
     * <br />
     * This method returns a Sheet, where each cell represents an aggregation result.
     * The keyColumnName is used as the row identifier in the resulting Sheet.
     * The aggregateOnColumnNames are the columns on which the aggregate operation is to be performed.
     * The pivotColumnName is used as the column identifier in the resulting Sheet.
     * The collector defines the aggregate operation.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "A", 100, 10},
     *          {"North", "B", 200, 20},
     *          {"South", "A", 150, 15},
     *          {"South", "B", 250, 25}
     *     });
     *
     * // +--------+---------+-------+----------+
     * // | region | product | sales | quantity |
     * // +--------+---------+-------+----------+
     * // | North  | A       | 100   | 10       |
     * // | North  | B       | 200   | 20       |
     * // | South  | A       | 150   | 15       |
     * // | South  | B       | 250   | 25       |
     * // +--------+---------+-------+----------+
     *
     * Sheet<String, String, Integer> pivotResult = dataset.pivot(
     *     "region",                           // row identifier
     *     "product",                          // column identifier
     *     Arrays.asList("sales", "quantity"), // aggregate columns
     *     Collectors.summingInt(arr -> (Integer) arr[0] + (Integer) arr[1])); // sum sales + quantity
     *
     * // Result Sheet:
     * //         +-----+-----+
     * //         | A   | B   |
     * // +-------+-----+-----+
     * // | North | 110 | 220 |
     * // | South | 165 | 275 |
     * // +-------+-----+-----+
     * }</pre>
     *
     * @param <R> The type of the row identifier in the resulting Sheet.
     * @param <C> The type of the column identifier in the resulting Sheet.
     * @param <T> The type of the aggregation result in the resulting Sheet.
     * @param keyColumnName The name of the column to be used as the row identifier in the resulting Sheet. Must not be {@code null}.
     * @param pivotColumnName The name of the column to be used as the column identifier in the resulting Sheet. Must not be {@code null}.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed. Must not be {@code null} or empty.
     * @param collector The collector defining the aggregate operation. Must not be {@code null}.
     * @return A Sheet representing the result of the pivot operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or if {@code keyColumnName}, {@code aggregateOnColumnNames}, {@code pivotColumnName}, or {@code collector} is {@code null}, or if {@code aggregateOnColumnNames} is empty.
     * @see #pivot(String, String, String, Collector)
     * @see #groupBy(Collection)
     * @see #rollup(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/34702815">Difference between groupby and pivot_table for pandas dataframes</a>
     */
    @Beta
    <R, C, T> Sheet<R, C, T> pivot(String keyColumnName, String pivotColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ? extends T> collector) throws IllegalArgumentException;

    /**
     * Performs a pivot operation on the Dataset using the specified key column, aggregate columns, pivot column, a row mapper, and a collector.
     * <br />
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
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "sales", "quantity"),
     *     new Object[][] {
     *          {"North", "A", 100, 10},
     *          {"North", "B", 200, 20},
     *          {"South", "A", 150, 15},
     *          {"South", "B", 250, 25}
     *     });
     *
     * // +--------+---------+-------+----------+
     * // | region | product | sales | quantity |
     * // +--------+---------+-------+----------+
     * // | North  | A       | 100   | 10       |
     * // | North  | B       | 200   | 20       |
     * // | South  | A       | 150   | 15       |
     * // | South  | B       | 250   | 25       |
     * // +--------+---------+-------+----------+
     *
     * Function<DisposableObjArray, Double> rowMapper = row -> (Integer) row.get(0) * 1.1; // Apply 10% markup to sales
     *
     * Sheet<String, String, Double> pivotResult = dataset.pivot(
     *     "region",                           // row identifier
     *     "product",                          // column identifier
     *     Arrays.asList("sales", "quantity"), // aggregate columns
     *     rowMapper,                          // transform function
     *     Collectors.collectingAndThen(Collectors.summingDouble(Double::doubleValue), r -> Numbers.round(r, 2))); // aggregation
     * //         +-------+-------+
     * //         | A     | B     |
     * // +-------+-------+-------+
     * // | North | 110.0 | 220.0 |
     * // | South | 165.0 | 275.0 |
     * // +-------+-------+-------+
     * }</pre>
     *
     * @param <R> The type of the row identifier in the resulting Sheet.
     * @param <C> The type of the column identifier in the resulting Sheet.
     * @param <U> The type of the row data after being transformed by the rowMapper.
     * @param <T> The type of the aggregation result in the resulting Sheet.
     * @param keyColumnName The name of the column to be used as the row identifier in the resulting Sheet. Must not be {@code null}.
     * @param pivotColumnName The name of the column to be used as the column identifier in the resulting Sheet. Must not be {@code null}.
     * @param aggregateOnColumnNames The names of the columns on which the aggregate operation is to be performed. Must not be {@code null} or empty.
     * @param rowMapper The function to transform the row data before aggregation. Must not be {@code null}.
     * @param collector The collector defining the aggregate operation. Must not be {@code null}.
     * @return A Sheet representing the result of the pivot operation.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or if {@code keyColumnName}, {@code aggregateOnColumnNames}, {@code pivotColumnName}, {@code rowMapper}, or {@code collector} is {@code null}, or if {@code aggregateOnColumnNames} is empty.
     * @see #pivot(String, String, String, Collector)
     * @see #pivot(String, String, Collection, Collector)
     * @see #groupBy(Collection)
     * @see #rollup(Collection)
     * @see #cube(Collection)
     * @see <a href="https://stackoverflow.com/questions/34702815">Difference between groupby and pivot_table for pandas dataframes</a>
     */
    @Beta
    <R, C, U, T> Sheet<R, C, T> pivot(String keyColumnName, String pivotColumnName, Collection<String> aggregateOnColumnNames,
            Function<? super DisposableObjArray, ? extends U> rowMapper, Collector<? super U, ?, ? extends T> collector) throws IllegalArgumentException;

    /**
     * Sorts the Dataset based on the specified column name.
     * <br />
     * The sorting is done in ascending order.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset employees = Dataset.rows(Arrays.asList("name", "age", "salary"), data);
     * employees.sortBy("age"); // sorts by age in ascending order
     * }</pre>
     *
     * @param columnName The name of the column to be used for sorting.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     */
    void sortBy(String columnName) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the Dataset based on the specified column name using the provided Comparator.
     * <br />
     * The Comparator determines the order of the elements.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{2, "Bob"}, {1, "Alice"}});
     * Dataset sorted = dataset.sortBy("id");
     * }</pre>
     *
     * @param columnName The name of the column to be used for sorting.
     * @param cmp The Comparator to determine the order of the elements.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     */
    void sortBy(String columnName, Comparator<?> cmp) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the Dataset based on the specified collection of column names.
     * <br />
     * The sorting is done in ascending order for each column in the order they appear in the collection.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{2, "Bob"}, {1, "Alice"}});
     * Dataset sorted = dataset.sortBy("id");
     * }</pre>
     *
     * @param columnNames The collection of column names to be used for sorting.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     */
    void sortBy(Collection<String> columnNames) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the Dataset based on the specified collection of column names using the provided Comparator.
     * <br />
     * The Comparator determines the order of the elements for each row, which is an Object array.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{2, "Bob"}, {1, "Alice"}});
     * Dataset sorted = dataset.sortBy("id");
     * }</pre>
     *
     * @param columnNames The collection of column names to be used for sorting.
     * @param cmp The Comparator to determine the order of the elements. It compares Object arrays, each representing a row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     */
    void sortBy(Collection<String> columnNames, Comparator<? super Object[]> cmp) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the Dataset based on the specified column names and a key mapper function.
     * The key mapper function is applied to each row of the Dataset, and the resulting Comparable objects are used for sorting.
     * The column names determine the order of the elements in the DisposableObjArray passed to the key mapper function.
     *
     * @param columnNames The names of the columns to be used for sorting. The order of the column names determines the order of the elements in the DisposableObjArray passed to the key mapper function.
     * @param keyExtractor A function that takes a DisposableObjArray representing a row of the Dataset and returns a Comparable object that is used for sorting.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     */
    @SuppressWarnings("rawtypes")
    void sortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyExtractor)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the Dataset with multi-threads based on the specified column name.
     * <br />
     * The sorting is done in ascending order.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{2, "Bob"}, {1, "Alice"}});
     * Dataset sorted = dataset.parallelSortBy("id");
     * }</pre>
     *
     * @param columnName The name of the column to be used for sorting.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     */
    void parallelSortBy(String columnName) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the Dataset with multi-threads based on the specified column name using the provided Comparator.
     * <br />
     * The Comparator determines the order of the elements for each row, which is an Object array.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{2, "Bob"}, {1, "Alice"}});
     * Dataset sorted = dataset.parallelSortBy("id");
     * }</pre>
     *
     * @param columnName The name of the column to be used for sorting.
     * @param cmp The Comparator to determine the order of the elements. It compares Object arrays, each representing a row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     */
    void parallelSortBy(String columnName, Comparator<?> cmp) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the Dataset with multi-threads based on the specified collection of column names.
     * <br />
     * The sorting is done in ascending order for each column.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{2, "Bob"}, {1, "Alice"}});
     * Dataset sorted = dataset.parallelSortBy("id");
     * }</pre>
     *
     * @param columnNames The collection of column names to be used for sorting.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     */
    void parallelSortBy(Collection<String> columnNames) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the Dataset with multi-threads based on the specified collection of column names using the provided Comparator.
     * <br />
     * The Comparator determines the order of the elements for each row, which is an Object array.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{2, "Bob"}, {1, "Alice"}});
     * Dataset sorted = dataset.parallelSortBy("id");
     * }</pre>
     *
     * @param columnNames The collection of column names to be used for sorting.
     * @param cmp The Comparator to determine the order of the elements. It compares Object arrays, each representing a row.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     */
    void parallelSortBy(Collection<String> columnNames, Comparator<? super Object[]> cmp) throws IllegalStateException, IllegalArgumentException;

    /**
     * Sorts the Dataset with multi-threads based on the specified column names and a key mapper function.
     * The key mapper function is applied to each row of the Dataset, and the resulting Comparable objects are used for sorting.
     * The column names determine the order of the elements in the DisposableObjArray passed to the key mapper function.
     * This method is designed for large datasets where parallel sorting can provide a performance improvement.
     *
     * @param columnNames The names of the columns to be used for sorting. The order of the column names determines the order of the elements in the DisposableObjArray passed to the key mapper function.
     * @param keyExtractor A function that takes a DisposableObjArray representing a row of the Dataset and returns a Comparable object that is used for sorting.
     * @throws IllegalStateException if the Dataset is frozen (read-only).
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     */
    @SuppressWarnings("rawtypes")
    void parallelSortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyExtractor)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Returns the top <i>n</i> rows from the Dataset based on the values in the specified column.
     * <br />
     * The rows are sorted in ascending order based on the values in the specified column.
     * If two rows have the same value in the specified column, their order is determined by their original order in the Dataset.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset employees = Dataset.rows(Arrays.asList("name", "salary", "age"), data);
     * Dataset topPaid = employees.topBy("salary", 5); // top 5 highest salaries
     * }</pre>
     *
     * @param columnName The name of the column to be used for sorting.
     * @param n The number of top rows to return.
     * @return A new Dataset containing the top <i>n</i> rows.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset or <i>n</i> is less than 1.
     */
    Dataset topBy(String columnName, int n) throws IllegalArgumentException;

    /**
     * Returns the top <i>n</i> rows from the Dataset based on the values in the specified column.
     * <br />
     * The rows are sorted based on the provided Comparator.
     * If two rows have the same value in the specified column, their order is determined by the Comparator.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 85}, {2, 95}});
     * Dataset top = dataset.topBy("score", 1);
     * }</pre>
     *
     * @param columnName The name of the column to be used for sorting.
     * @param n The number of top rows to return.
     * @param cmp The Comparator to determine the order of the elements. It compares Object arrays, each representing a row.
     * @return A new Dataset containing the top <i>n</i> rows.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset or <i>n</i> is less than 1.
     */
    Dataset topBy(String columnName, int n, Comparator<?> cmp) throws IllegalArgumentException;

    /**
     * Returns the top <i>n</i> rows from the Dataset based on the values in the specified columns.
     * <br />
     * The rows are sorted in the order they appear in the Dataset.
     * If two rows have the same values in the specified columns, their order is determined by their original order in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 85}, {2, 95}});
     * Dataset top = dataset.topBy("score", 1);
     * }</pre>
     *
     * @param columnNames The names of the columns to be used for sorting.
     * @param n The number of top rows to return.
     * @return A new Dataset containing the top <i>n</i> rows.
     * @throws IllegalArgumentException if any of the specified column names do not exist in the Dataset or {@code columnNames} is empty or <i>n</i> is less than 1.
     */
    Dataset topBy(Collection<String> columnNames, int n) throws IllegalArgumentException;

    /**
     * Returns the top <i>n</i> rows from the Dataset based on the values in the specified columns.
     * <br />
     * The rows are sorted based on the provided Comparator.
     * If two rows have the same values in the specified columns, their order is determined by the Comparator.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 85}, {2, 95}});
     * Dataset top = dataset.topBy("score", 1);
     * }</pre>
     *
     * @param columnNames The names of the columns to be used for sorting.
     * @param n The number of top rows to return.
     * @param cmp The Comparator to determine the order of the elements. It compares Object arrays, each representing a row.
     * @return A new Dataset containing the top <i>n</i> rows.
     * @throws IllegalArgumentException if any of the specified column names do not exist in the Dataset or {@code columnNames} is empty or <i>n</i> is less than 1.
     */
    Dataset topBy(Collection<String> columnNames, int n, Comparator<? super Object[]> cmp) throws IllegalArgumentException;

    /**
     * Returns the top <i>n</i> rows from the Dataset based on the values in the specified columns.
     * The rows are sorted based on the provided keyExtractor function.
     * If two rows have the same value in the specified columns, their order is determined by the keyExtractor function.
     *
     * @param columnNames The names of the columns to be used for sorting.
     * @param n The number of top rows to return.
     * @param keyExtractor The function to determine the order of the elements. It takes an array of Objects, each representing a row, and returns a Comparable.
     * @return A new Dataset containing the top <i>n</i> rows.
     * @throws IllegalArgumentException if any of the specified column names do not exist in the Dataset or {@code columnNames} is empty or <i>n</i> is less than 1.
     */
    @SuppressWarnings("rawtypes")
    Dataset topBy(Collection<String> columnNames, int n, Function<? super DisposableObjArray, ? extends Comparable> keyExtractor);

    /**
     * Returns a new Dataset containing only the distinct rows from the original Dataset.
     * <br />
     * The distinctness of rows is determined by the equals method of the row objects.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset orders = Dataset.rows(Arrays.asList("product", "customer"), data);
     * Dataset uniqueOrders = orders.distinct(); // removes duplicate rows
     * }</pre>
     *
     * @return A new Dataset containing only distinct rows.
     */
    Dataset distinct();

    /**
     * Returns a new Dataset containing only the distinct rows based on the specified column from the original Dataset.
     * <br />
     * The distinctness of rows is determined by the equals method of the column values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset employees = Dataset.rows(Arrays.asList("name", "department", "salary"), data);
     * Dataset uniqueDepts = employees.distinctBy("department"); // one row per department
     * }</pre>
     *
     * @param columnName The name of the column to be used for determining distinctness.
     * @return A new Dataset containing only distinct rows based on the specified column.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     */
    Dataset distinctBy(String columnName);

    /**
     * Returns a new Dataset containing only the distinct rows based on the specified column from the original Dataset.
     * <br />
     * The distinctness of rows is determined by the equals method of the values returned by the provided keyExtractor function.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {1, "Alice"}});
     * Dataset distinct = dataset.distinctBy(Arrays.asList("id"));
     * }</pre>
     *
     * @param columnName The name of the column to be used for determining distinctness.
     * @param keyExtractor A function to process the column values before determining distinctness.
     * @return A new Dataset containing only distinct rows based on the specified column and keyExtractor function.
     * @throws IllegalArgumentException if the specified column name does not exist in the Dataset.
     */
    Dataset distinctBy(String columnName, Function<?, ?> keyExtractor);

    /**
     * Returns a new Dataset containing only the distinct rows based on the specified columns from the original Dataset.
     * <br />
     * The distinctness of rows is determined by the equals method of the values in the specified columns.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {1, "Alice"}});
     * Dataset distinct = dataset.distinctBy(Arrays.asList("id"));
     * }</pre>
     *
     * @param columnNames The names of the columns to be used for determining distinctness.
     * @return A new Dataset containing only distinct rows based on the specified columns.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     */
    Dataset distinctBy(Collection<String> columnNames);

    /**
     * Returns a new Dataset containing only the distinct rows based on the specified columns from the original Dataset.
     * <br />
     * The distinctness of rows is determined by the equals method of the values returned by the provided keyExtractor function.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {1, "Alice"}});
     * Dataset distinct = dataset.distinctBy(Arrays.asList("id"));
     * }</pre>
     *
     * @param columnNames The names of the columns to be used for determining distinctness.
     * @param keyExtractor A function to process the column values before determining distinctness.
     * @return A new Dataset containing only distinct rows based on the specified columns and keyExtractor function.
     * @throws IllegalArgumentException if any of the specified column names does not exist in the Dataset or {@code columnNames} is empty.
     */
    Dataset distinctBy(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyExtractor);

    /**
     * Filters the rows of the Dataset based on the provided predicate.
     * <br />
     * The predicate is applied to each row, and only rows that satisfy the predicate (i.e., predicate returns true) are included in the returned Dataset.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset employees = Dataset.rows(Arrays.asList("name", "age", "salary"), data);
     * Dataset highEarners = employees.filter(row -> (Integer) row.get(2) > 50000);
     * }</pre>
     *
     * @param filter The predicate to apply to each row. It takes an instance of DisposableObjArray, which represents a row in the Dataset.
     * @return A new Dataset containing only the rows that satisfy the provided predicate.
     */
    Dataset filter(Predicate<? super DisposableObjArray> filter);

    /**
     * Filters the rows of the Dataset based on the provided predicate and limits the number of results.
     * <br />
     * The predicate is applied to each row, and only rows that satisfy the predicate (i.e., predicate returns true) are included in the returned Dataset.
     * The operation stops once the number of satisfied rows reaches the specified maximum limit.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset products = Dataset.rows(Arrays.asList("name", "price", "category"), data);
     * Dataset cheapProducts = products.filter(row -> (Double) row.get(1) < 100.0, 5);
     * }</pre>
     *
     * @param filter The predicate to apply to each row. It takes an instance of DisposableObjArray, which represents a row in the Dataset.
     * @param max The maximum number of rows to include in the returned Dataset.
     * @return A new Dataset containing only the rows that satisfy the provided predicate, up to the specified maximum limit.
     * @throws IllegalArgumentException if the specified max is less than 0.
     */
    Dataset filter(Predicate<? super DisposableObjArray> filter, int max) throws IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on the provided predicate and within the specified row index range.
     * <br />
     * The predicate is applied to each row within the range, and only rows that satisfy the predicate (i.e., predicate returns true) are included in the returned Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to apply the filter. It's inclusive.
     * @param toRowIndex The ending index of the row range to apply the filter. It's exclusive.
     * @param filter The predicate to apply to each row within the specified range. It takes an instance of DisposableObjArray, which represents a row in the Dataset.
     * @return A new Dataset containing only the rows within the specified range that satisfy the provided predicate.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset.
     */
    Dataset filter(int fromRowIndex, int toRowIndex, Predicate<? super DisposableObjArray> filter) throws IndexOutOfBoundsException;

    /**
     * Filters the rows of the Dataset based on the provided predicate, within the specified row index range, and limits the number of results.
     * <br />
     * The predicate is applied to each row within the range, and only rows that satisfy the predicate (i.e., predicate returns true) are included in the returned Dataset.
     * The operation stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to apply the filter. It's inclusive.
     * @param toRowIndex The ending index of the row range to apply the filter. It's exclusive.
     * @param filter The predicate to apply to each row within the specified range. It takes an instance of DisposableObjArray, which represents a row in the Dataset.
     * @param max The maximum number of rows to include in the returned Dataset.
     * @return A new Dataset containing only the rows within the specified range that satisfy the provided predicate, up to the specified maximum limit.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset.
     * @throws IllegalArgumentException if the specified max is less than 0.
     */
    Dataset filter(int fromRowIndex, int toRowIndex, Predicate<? super DisposableObjArray> filter, int max)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on the provided BiPredicate and the specified column names.
     * <br />
     * The BiPredicate is applied to each pair of values from the specified columns, and only rows where the BiPredicate returns {@code true} are included in the returned Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param columnNames A Tuple2 containing the names of the two columns to be used in the BiPredicate.
     * @param filter The BiPredicate to apply to each pair of values from the specified columns. It takes two instances of Objects, which represent the values in the Dataset's row for the specified columns.
     * @return A new Dataset containing only the rows where the provided BiPredicate returns {@code true} for the pair of values from the specified columns.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset.
     */
    Dataset filter(Tuple2<String, String> columnNames, BiPredicate<?, ?> filter) throws IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on the provided BiPredicate and the specified column names, and limits the number of results.
     * <br />
     * The BiPredicate is applied to each pair of values from the specified columns, and only rows where the BiPredicate returns {@code true} are included in the returned Dataset.
     * The operation stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param columnNames A Tuple2 containing the names of the two columns to be used in the BiPredicate.
     * @param filter The BiPredicate to apply to each pair of values from the specified columns. It takes two instances of Objects, which represent the values in the Dataset's row for the specified columns.
     * @param max The maximum number of rows to include in the returned Dataset.
     * @return A new Dataset containing only the rows where the provided BiPredicate returns {@code true} for the pair of values from the specified columns, up to the specified maximum limit.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or if the specified max is less than 0.
     */
    Dataset filter(Tuple2<String, String> columnNames, BiPredicate<?, ?> filter, int max) throws IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on the provided BiPredicate and the specified column names, within the given row index range.
     * <br />
     * The BiPredicate is applied to each pair of values from the specified columns, and only rows where the BiPredicate returns {@code true} are included in the returned Dataset.
     * The operation is performed only on the rows within the specified index range.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param fromRowIndex The start index of the row range to filter.
     * @param toRowIndex The end index of the row range to filter.
     * @param columnNames A Tuple2 containing the names of the two columns to be used in the BiPredicate.
     * @param filter The BiPredicate to apply to each pair of values from the specified columns. It takes two instances of Objects, which represent the values in the Dataset's row for the specified columns.
     * @return A new Dataset containing only the rows where the provided BiPredicate returns {@code true} for the pair of values from the specified columns, within the specified row index range.
     * @throws IndexOutOfBoundsException if the specified row index range is out of the Dataset's bounds.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset.
     */
    Dataset filter(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, BiPredicate<?, ?> filter)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on the provided BiPredicate and the specified column names, within the given row index range and limits the number of results.
     * <br />
     * The BiPredicate is applied to each pair of values from the specified columns, and only rows where the BiPredicate returns {@code true} are included in the returned Dataset.
     * The operation is performed only on the rows within the specified index range and stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param fromRowIndex The start index of the row range to filter.
     * @param toRowIndex The end index of the row range to filter.
     * @param columnNames A Tuple2 containing the names of the two columns to be used in the BiPredicate.
     * @param filter The BiPredicate to apply to each pair of values from the specified columns. It takes two instances of Objects, which represent the values in the Dataset's row for the specified columns.
     * @param max The maximum number of rows to include in the returned Dataset.
     * @return A new Dataset containing only the rows where the provided BiPredicate returns {@code true} for the pair of values from the specified columns, within the specified row index range and up to the specified maximum limit.
     * @throws IndexOutOfBoundsException if the specified row index range is out of the Dataset's bounds.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or if the specified max is less than 0.
     */
    Dataset filter(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, BiPredicate<?, ?> filter, int max)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on a provided TriPredicate and the specified column names.
     * <br />
     * The TriPredicate is applied to each triplet of values from the specified columns, and only rows where the TriPredicate returns {@code true} are included in the returned Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param columnNames A Tuple3 containing the names of the three columns to be used in the TriPredicate.
     * @param filter The TriPredicate to apply to each triplet of values from the specified columns. It takes three instances of Objects, which represent the values in the Dataset's row for the specified columns.
     * @return A new Dataset containing only the rows where the provided TriPredicate returns {@code true} for the triplet of values from the specified columns.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset.
     */
    Dataset filter(Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter) throws IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on a provided TriPredicate and the specified column names, within a limit.
     * <br />
     * The TriPredicate is applied to each triplet of values from the specified columns, and only rows where the TriPredicate returns {@code true} are included in the returned Dataset.
     * The operation stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param columnNames A Tuple3 containing the names of the three columns to be used in the TriPredicate.
     * @param filter The TriPredicate to apply to each triplet of values from the specified columns. It takes three instances of Objects, which represent the values in the Dataset's row for the specified columns.
     * @param max The maximum number of rows to include in the returned Dataset.
     * @return A new Dataset containing only the rows where the provided TriPredicate returns {@code true} for the triplet of values from the specified columns, up to the specified maximum limit.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or if the specified max is less than 0.
     */
    Dataset filter(Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter, int max) throws IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on a provided TriPredicate and the specified column names, within a specified row index range.
     * <br />
     * The TriPredicate is applied to each triplet of values from the specified columns, and only rows where the TriPredicate returns {@code true} are included in the returned Dataset.
     * The operation is performed only on the rows within the specified index range.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param fromRowIndex The start index of the row range to apply the filter on.
     * @param toRowIndex The end index of the row range to apply the filter on.
     * @param columnNames A Tuple3 containing the names of the three columns to be used in the TriPredicate.
     * @param filter The TriPredicate to apply to each triplet of values from the specified columns. It takes three instances of Objects, which represent the values in the Dataset's row for the specified columns.
     * @return A new Dataset containing only the rows where the provided TriPredicate returns {@code true} for the triplet of values from the specified columns, within the specified row index range.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset.
     */
    Dataset filter(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on a provided TriPredicate and the specified column names, within a specified row index range and a maximum limit.
     * <br />
     * The TriPredicate is applied to each triplet of values from the specified columns, and only rows where the TriPredicate returns {@code true} are included in the returned Dataset.
     * The operation is performed only on the rows within the specified index range and stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param fromRowIndex The start index of the row range to apply the filter on.
     * @param toRowIndex The end index of the row range to apply the filter on.
     * @param columnNames A Tuple3 containing the names of the three columns to be used in the TriPredicate.
     * @param filter The TriPredicate to apply to each triplet of values from the specified columns. It takes three instances of Objects, which represent the values in the Dataset's row for the specified columns.
     * @param max The maximum number of rows to include in the returned Dataset.
     * @return A new Dataset containing only the rows where the provided TriPredicate returns {@code true} for the triplet of values from the specified columns, within the specified row index range and up to the specified maximum limit.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or if the specified max is less than 0.
     */
    Dataset filter(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter, int max)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on a provided Predicate and the specified column name.
     * <br />
     * The Predicate is applied to each value from the specified column, and only rows where the Predicate returns {@code true} are included in the returned Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param columnName The name of the column to be used in the Predicate.
     * @param filter The Predicate to apply to each value from the specified column. It takes an instance of Object, which represents the value in the Dataset's row for the specified column.
     * @return A new Dataset containing only the rows where the provided Predicate returns {@code true} for the value from the specified column.
     * @throws IllegalArgumentException if the specified column name is not found in the Dataset.
     */
    Dataset filter(String columnName, Predicate<?> filter) throws IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on a provided Predicate and the specified column name, with a maximum limit.
     * <br />
     * The Predicate is applied to each value from the specified column, and only rows where the Predicate returns {@code true} are included in the returned Dataset.
     * The operation stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param columnName The name of the column to be used in the Predicate.
     * @param filter The Predicate to apply to each value from the specified column. It takes an instance of Object, which represents the value in the Dataset's row for the specified column.
     * @param max The maximum number of rows to include in the returned Dataset.
     * @return A new Dataset containing only the rows where the provided Predicate returns {@code true} for the value from the specified column, up to the specified maximum limit.
     * @throws IllegalArgumentException if the specified column name is not found in the Dataset or if the specified max is less than 0.
     */
    Dataset filter(String columnName, Predicate<?> filter, int max) throws IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on a provided Predicate and the specified column name, within a given row index range.
     * <br />
     * The Predicate is applied to each value from the specified column within the given range, and only rows where the Predicate returns {@code true} are included in the returned Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param fromRowIndex The start index of the row range to apply the filter.
     * @param toRowIndex The end index of the row range to apply the filter.
     * @param columnName The name of the column to be used in the Predicate.
     * @param filter The Predicate to apply to each value from the specified column. It takes an instance of Object, which represents the value in the Dataset's row for the specified column.
     * @return A new Dataset containing only the rows within the specified range where the provided Predicate returns {@code true} for the value from the specified column.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset.
     * @throws IllegalArgumentException if the specified column name is not found in the Dataset.
     */
    Dataset filter(int fromRowIndex, int toRowIndex, String columnName, Predicate<?> filter) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on a provided predicate function.
     * <br />
     * The function is applied to the values of the specified column in each row.
     * Only the rows that satisfy the predicate (i.e., the function returns true) are included in the resulting Dataset.
     * The operation is performed on the rows within the specified range and until the number of rows in the resulting Dataset reaches the specified maximum limit.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to consider for filtering.
     * @param toRowIndex The ending index of the row range to consider for filtering.
     * @param columnName The name of the column whose values will be used as input for the predicate function.
     * @param filter The predicate function to apply to each row of the specified column. It takes an instance of the column's value type and returns a boolean.
     * @param max The maximum number of rows to include in the resulting Dataset.
     * @return A new Dataset containing only the rows that satisfy the predicate, up to the specified maximum limit.
     * @throws IndexOutOfBoundsException if the specified {@code fromRowIndex} or {@code toRowIndex} is out of the range of the Dataset.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or if the specified max is less than 0.
     */
    Dataset filter(int fromRowIndex, int toRowIndex, String columnName, Predicate<?> filter, int max)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on a provided predicate function.
     * <br />
     * The function is applied to the values of the specified columns in each row.
     * Only the rows that satisfy the predicate (i.e., the function returns true) are included in the resulting Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param columnNames A collection of column names whose values will be used as input for the predicate function.
     * @param filter The predicate function to apply to each row of the specified columns. It takes an instance of DisposableObjArray (which represents the values of the specified columns in a row) and returns a boolean.
     * @return A new Dataset containing only the rows that satisfy the predicate.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or {@code columnNames} is empty.
     */
    Dataset filter(Collection<String> columnNames, Predicate<? super DisposableObjArray> filter) throws IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on a provided predicate function.
     * <br />
     * The function is applied to the values of the specified columns in each row.
     * Only the rows that satisfy the predicate (i.e., the function returns true) are included in the resulting Dataset.
     * The operation is performed until the number of rows in the resulting Dataset reaches the specified maximum limit.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param columnNames A collection of column names whose values will be used as input for the predicate function.
     * @param filter The predicate function to apply to each row of the specified columns. It takes an instance of DisposableObjArray (which represents the values of the specified columns in a row) and returns a boolean.
     * @param max The maximum number of rows to include in the resulting Dataset.
     * @return A new Dataset containing only the rows that satisfy the predicate, up to the specified maximum limit.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or {@code columnNames} is empty or if the specified max is less than 0.
     */
    Dataset filter(Collection<String> columnNames, Predicate<? super DisposableObjArray> filter, int max) throws IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on a provided predicate function.
     * <br />
     * The function is applied to the values of the specified columns in each row.
     * Only the rows that satisfy the predicate (i.e., the function returns true) are included in the resulting Dataset.
     * The operation is performed on a range of rows specified by the fromRowIndex and toRowIndex parameters.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param fromRowIndex The starting index of the row range to filter (inclusive).
     * @param toRowIndex The ending index of the row range to filter (exclusive).
     * @param columnNames A collection of column names whose values will be used as input for the predicate function.
     * @param filter The predicate function to apply to each row of the specified columns. It takes an instance of DisposableObjArray (which represents the values of the specified columns in a row) and returns a boolean.
     * @return A new Dataset containing only the rows that satisfy the predicate.
     * @throws IndexOutOfBoundsException if the specified row index range is out of the Dataset's bounds.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or {@code columnNames} is empty.
     */
    Dataset filter(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Predicate<? super DisposableObjArray> filter)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Filters the rows of the Dataset based on the provided Predicate and the specified column names, within the given row index range and limits the number of results.
     * <br />
     * The Predicate is applied to each DisposableObjArray (which represents a row in the Dataset) from the specified columns, and only rows where the Predicate returns {@code true} are included in the returned Dataset.
     * The operation is performed only on the rows within the specified index range and stops once the number of satisfied rows reaches the specified maximum limit.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "age"), new Object[][] {{1, 18}, {2, 25}});
     * Dataset adults = dataset.filter("age", age -> (Integer) age >= 21);
     * }</pre>
     *
     * @param fromRowIndex The start index of the row range to filter.
     * @param toRowIndex The end index of the row range to filter.
     * @param columnNames A Collection containing the names of the columns to be used in the Predicate.
     * @param filter The Predicate to apply to each DisposableObjArray from the specified columns. It takes an instance of DisposableObjArray, which represents the values in the Dataset's row for the specified columns.
     * @param max The maximum number of rows to include in the returned Dataset.
     * @return A new Dataset containing only the rows where the provided Predicate returns {@code true} for the DisposableObjArray from the specified columns, within the specified row index range and up to the specified maximum limit.
     * @throws IndexOutOfBoundsException if the specified row index range is out of the Dataset's bounds.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or {@code columnNames} is empty or if the specified max is less than 0.
     */
    Dataset filter(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Predicate<? super DisposableObjArray> filter, int max)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Transforms the Dataset by applying a mapping function to the specified column and creating a new column with the results.
     * <br />
     * The original column used in the mapping function is preserved.
     * The operation also copies the specified column to the new Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * Dataset doubled = dataset.map("id", id -> (Integer) id * 2, "doubleId");
     * }</pre>
     *
     * @param fromColumnName The name of the column to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnName The name of the column to be copied to the new Dataset.
     * @param mapper The mapping function to apply to each row of the specified column. It takes an instance of the column's value and returns a new value.
     * @return A new Dataset with the new column added and the specified column copied.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset.
     */
    Dataset map(String fromColumnName, String newColumnName, String copyingColumnName, Function<?, ?> mapper) throws IllegalArgumentException;

    /**
     * Transforms the Dataset by applying a mapping function to the specified column and creating a new column with the results.
     * <br />
     * The original column used in the mapping function is preserved.
     * The operation also copies the specified columns to the new Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * Dataset doubled = dataset.map("id", id -> (Integer) id * 2, "doubleId");
     * }</pre>
     *
     * @param fromColumnName The name of the column to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new Dataset.
     * @param mapper The mapping function to apply to each row of the specified column. It takes an instance of the column's value and returns a new value.
     * @return A new Dataset with the new column added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset.
     */
    Dataset map(String fromColumnName, String newColumnName, Collection<String> copyingColumnNames, Function<?, ?> mapper) throws IllegalArgumentException;

    /**
     * Transforms the Dataset by applying a mapping function to the specified pair of columns and creating a new column with the results.
     * <br />
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * Dataset doubled = dataset.map("id", id -> (Integer) id * 2, "doubleId");
     * }</pre>
     *
     * @param fromColumnNames A Tuple2 containing the pair of column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new Dataset.
     * @param mapper The mapping function to apply to each row of the specified columns. It takes instances of the columns' values and returns a new value.
     * @return A new Dataset with the new column added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset.
     */
    Dataset map(Tuple2<String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames, BiFunction<?, ?, ?> mapper)
            throws IllegalArgumentException;

    /**
     * Transforms the Dataset by applying a mapping function to the specified columns and creating a new column with the results.
     * <br />
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * Dataset doubled = dataset.map("id", id -> (Integer) id * 2, "doubleId");
     * }</pre>
     *
     * @param fromColumnNames A Tuple3 containing the column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new Dataset.
     * @param mapper The mapping function to apply to each row of the specified columns. It takes instances of the columns' values and returns a new value.
     * @return A new Dataset with the new column added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset.
     */
    Dataset map(Tuple3<String, String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames, TriFunction<?, ?, ?, ?> mapper)
            throws IllegalArgumentException;

    /**
     * Transforms the Dataset by applying a mapping function to the specified columns and creating a new column with the results.
     * <br />
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}});
     * Dataset doubled = dataset.map("id", id -> (Integer) id * 2, "doubleId");
     * }</pre>
     *
     * @param fromColumnNames A collection of column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new Dataset.
     * @param mapper The mapping function to apply to each row of the specified columns. It takes an instance of DisposableObjArray, which represents the values in the Dataset's row for the specified columns.
     * @return A new Dataset with the new column added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or {@code fromColumnNames} is empty.
     */
    Dataset map(Collection<String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames, Function<? super DisposableObjArray, ?> mapper)
            throws IllegalArgumentException;

    /**
     * Transforms the Dataset by applying a mapping function to the specified column and creating new rows with the results.
     * <br />
     * The original column used in the mapping function is preserved.
     * The operation also copies the specified column to the new Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("tags"), new Object[][] {{Arrays.asList("a", "b")}});
     * Dataset exploded = dataset.flatMap("tags", tag -> Dataset.rows(Arrays.asList("tag"), new Object[][] {{tag}}));
     * }</pre>
     *
     * @param fromColumnName The column name to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnName The column name to be copied to the new Dataset.
     * @param mapper The mapping function to apply to each row of the specified column. It takes an instance of the column's value and returns a Collection of new rows.
     * @return A new Dataset with the new rows added and the specified column copied.
     * @throws IllegalArgumentException if the specified column name is not found in the Dataset.
     */
    Dataset flatMap(String fromColumnName, String newColumnName, String copyingColumnName, Function<?, ? extends Collection<?>> mapper)
            throws IllegalArgumentException;

    /**
     * Transforms the Dataset by applying a mapping function to the specified column and creating new rows with the results.
     * <br />
     * The original column used in the mapping function is preserved.
     * The operation also copies the specified columns to the new Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("tags"), new Object[][] {{Arrays.asList("a", "b")}});
     * Dataset exploded = dataset.flatMap("tags", tag -> Dataset.rows(Arrays.asList("tag"), new Object[][] {{tag}}));
     * }</pre>
     *
     * @param fromColumnName The column name to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new Dataset.
     * @param mapper The mapping function to apply to each row of the specified column. It takes an instance of the column's value and returns a Collection of new rows.
     * @return A new Dataset with the new rows added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column name is not found in the Dataset.
     */
    Dataset flatMap(String fromColumnName, String newColumnName, Collection<String> copyingColumnNames, Function<?, ? extends Collection<?>> mapper)
            throws IllegalArgumentException;

    /**
     * Transforms the Dataset by applying a mapping function to the specified pair of columns and creating new rows with the results.
     * <br />
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("tags"), new Object[][] {{Arrays.asList("a", "b")}});
     * Dataset exploded = dataset.flatMap("tags", tag -> Dataset.rows(Arrays.asList("tag"), new Object[][] {{tag}}));
     * }</pre>
     *
     * @param fromColumnNames A tuple of two column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new Dataset.
     * @param mapper the mapper that turns each input row into a dataset whose rows are concatenated
     * @return A new Dataset with the new rows added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset.
     */
    Dataset flatMap(Tuple2<String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames,
            BiFunction<?, ?, ? extends Collection<?>> mapper) throws IllegalArgumentException;

    /**
     * Transforms the Dataset by applying a mapping function to the specified columns and creating new rows with the results.
     * <br />
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("tags"), new Object[][] {{Arrays.asList("a", "b")}});
     * Dataset exploded = dataset.flatMap("tags", tag -> Dataset.rows(Arrays.asList("tag"), new Object[][] {{tag}}));
     * }</pre>
     *
     * @param fromColumnNames A tuple of three column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new Dataset.
     * @param mapper The mapping function to apply to each row of the specified columns. It takes an instance of DisposableObjArray, which represents the values in the Dataset's row for the specified columns, and returns a Collection of new rows.
     * @return A new Dataset with the new rows added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset.
     */
    Dataset flatMap(Tuple3<String, String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames,
            TriFunction<?, ?, ?, ? extends Collection<?>> mapper) throws IllegalArgumentException;

    /**
     * Transforms the Dataset by applying a mapping function to the specified columns and creating a new column with the results.
     * <br />
     * The original columns used in the mapping function are preserved.
     * The operation also copies the specified columns to the new Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("tags"), new Object[][] {{Arrays.asList("a", "b")}});
     * Dataset exploded = dataset.flatMap("tags", tag -> Dataset.rows(Arrays.asList("tag"), new Object[][] {{tag}}));
     * }</pre>
     *
     * @param fromColumnNames A collection of column names to be used as input for the mapping function.
     * @param newColumnName The name of the new column that will store the results of the mapping function.
     * @param copyingColumnNames A collection of column names to be copied to the new Dataset.
     * @param mapper The mapping function to apply to each row of the specified columns. It takes an instance of DisposableObjArray, which represents the values in the Dataset's row for the specified columns.
     * @return A new Dataset with the new column added and the specified columns copied.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or {@code fromColumnNames} is empty.
     */
    Dataset flatMap(Collection<String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames,
            Function<? super DisposableObjArray, ? extends Collection<?>> mapper) throws IllegalArgumentException;

    /**
     * Performs an inner join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The inner join operation combines rows from two Datasets based on a related column between them.
     * Only rows that have matching values in both Datasets will be included in the resulting Dataset.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset employees = Dataset.rows(Arrays.asList("id", "name"), empData);
     * Dataset departments = Dataset.rows(Arrays.asList("dept_id", "dept_name"), deptData);
     * Dataset joined = employees.innerJoin(departments, "id", "dept_id");
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param columnName The name of the column in this Dataset to use for the join.
     * @param joinColumnNameOnRight The name of the column in the other Dataset to use for the join.
     * @return A new Dataset that is the result of the inner join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset innerJoin(Dataset right, String columnName, String joinColumnNameOnRight) throws IllegalArgumentException;

    /**
     * Performs an inner join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The inner join operation combines rows from two Datasets based on related columns between them.
     * Only rows that have matching values in both Datasets will be included in the resulting Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Dataset joined = left.innerJoin(Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 95}}), Arrays.asList("id"));
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset and the value is the corresponding column name in the other Dataset.
     * @return A new Dataset that is the result of the inner join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset innerJoin(Dataset right, Map<String, String> onColumnNames);

    /**
     * Performs an inner join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The inner join operation combines rows from two Datasets based on related columns between them.
     * Only rows that have matching values in both Datasets will be included in the resulting Dataset.
     * Additionally, a new column is added to the resulting Dataset, with its type specified and is populated with values from the right Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Dataset joined = left.innerJoin(Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 95}}), Arrays.asList("id"));
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset and the value is the corresponding column name in the other Dataset.
     * @param newColumnName The name of the new column to be added to the resulting Dataset.
     * @param newColumnType The type of the new column to be added to the resulting Dataset. It must be Object[], Collection, Map, or Bean class.
     * @return A new Dataset that is the result of the inner join operation, including the new column.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset innerJoin(Dataset right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType) throws IllegalArgumentException;

    /**
     * Performs an inner join operation between this Dataset and another Dataset based on the specified column names.
     * The inner join operation combines rows from two Datasets based on related columns between them.
     * Only rows that have matching values in both Datasets will be included in the resulting Dataset.
     * Additionally, a new column is added to the resulting Dataset, with its type specified and is populated with values from the right Dataset.
     *
     * @param right The Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset and the value is the column name in the right Dataset to join on.
     * @param newColumnName The name of the new column to be added to the resulting Dataset.
     * @param newColumnType The type of the new column. It must be Object[], Collection, Map, or Bean class.
     * @param collSupplier A function that generates a collection to hold the joined rows for the new column for one-many or many-many mapping.
     * @return A new Dataset that is the result of the inner join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    @SuppressWarnings("rawtypes")
    Dataset innerJoin(Dataset right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException;

    /**
     * Performs a left join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The left join operation combines rows from two Datasets based on related columns between them.
     * All rows from the left Dataset and the matched rows from the right Dataset will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the right side.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset employees = Dataset.rows(Arrays.asList("id", "name"), empData);
     * Dataset salaries = Dataset.rows(Arrays.asList("emp_id", "salary"), salData);
     * Dataset result = employees.leftJoin(salaries, "id", "emp_id"); // includes all employees
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param columnName The column name in this Dataset to join on.
     * @param joinColumnNameOnRight The column name in the other Dataset to join on.
     * @return A new Dataset that is the result of the left join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset leftJoin(Dataset right, String columnName, String joinColumnNameOnRight);

    /**
     * Performs a left join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The left join operation combines rows from two Datasets based on related columns between them.
     * All rows from the left Dataset and the matched rows from the right Dataset will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the right side.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Dataset joined = left.leftJoin(Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 95}}), Arrays.asList("id"));
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset to join on, and the value is the column name in the other Dataset to join on.
     * @return A new Dataset that is the result of the left join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset leftJoin(Dataset right, Map<String, String> onColumnNames);

    /**
     * Performs a left join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The left join operation combines rows from two Datasets based on related columns between them.
     * All rows from the left Dataset and the matched rows from the right Dataset will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the right side.
     * Additionally, a new column is added to the resulting Dataset, with its type specified.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Dataset joined = left.leftJoin(Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 95}}), Arrays.asList("id"));
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset to join on, and the value is the column name in the other Dataset to join on.
     * @param newColumnName The name of the new column to be added to the resulting Dataset.
     * @param newColumnType The type of the new column to be added to the resulting Dataset. It must be Object[], Collection, Map, or Bean class.
     * @return A new Dataset that is the result of the left join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset leftJoin(Dataset right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType) throws IllegalArgumentException;

    /**
     * Performs a left join operation between this Dataset and another Dataset based on the specified column names.
     * The left join operation combines rows from two Datasets based on related columns between them.
     * All rows from the left Dataset and the matched rows from the right Dataset will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the right side.
     * Additionally, a new column is added to the resulting Dataset, with its type specified.
     * A custom collection supplier can be provided to control the type of collection used in the join operation.
     *
     * @param right The other Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset to join on, and the value is the column name in the other Dataset to join on.
     * @param newColumnName The name of the new column to be added to the resulting Dataset.
     * @param newColumnType The type of the new column to be added to the resulting Dataset. It must be Object[], Collection, Map, or Bean class.
     * @param collSupplier A function that generates a collection to hold the joined rows for the new column for one-many or many-many mapping.
     * @return A new Dataset that is the result of the left join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    @SuppressWarnings("rawtypes")
    Dataset leftJoin(Dataset right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException;

    /**
     * Performs a right join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The right join operation combines rows from two Datasets based on related columns between them.
     * All rows from the right Dataset and the matched rows from the left Dataset will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the left side.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset right = Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 95}});
     * Dataset joined = right.rightJoin(Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}}), Arrays.asList("id"));
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param columnName The column name in this Dataset to join on.
     * @param joinColumnNameOnRight The column name in the other Dataset to join on.
     * @return A new Dataset that is the result of the right join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset rightJoin(Dataset right, String columnName, String joinColumnNameOnRight) throws IllegalArgumentException;

    /**
     * Performs a right join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The right join operation combines rows from two Datasets based on related columns between them.
     * All rows from the right Dataset and the matched rows from the left Dataset will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the left side.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset right = Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 95}});
     * Dataset joined = right.rightJoin(Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}}), Arrays.asList("id"));
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset to join on, and the value is the column name in the other Dataset to join on.
     * @return A new Dataset that is the result of the right join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset rightJoin(Dataset right, Map<String, String> onColumnNames) throws IllegalArgumentException;

    /**
     * Performs a right join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The right join operation combines rows from two Datasets based on related columns between them.
     * All rows from the right Dataset and the matched rows from the left Dataset will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the left side.
     * Additionally, a new column is added to the resulting Dataset, with its type specified by the user.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset right = Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 95}});
     * Dataset joined = right.rightJoin(Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}}), Arrays.asList("id"));
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset to join on, and the value is the column name in the other Dataset to join on.
     * @param newColumnName The name of the new column to be added to the resulting Dataset.
     * @param newColumnType The type of the new column to be added to the resulting Dataset. It must be Object[], Collection, Map, or Bean class.
     * @return A new Dataset that is the result of the right join operation, with the additional column.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset rightJoin(Dataset right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType) throws IllegalArgumentException;

    /**
     * Performs a right join operation between this Dataset and another Dataset based on the specified column names.
     * The right join operation combines rows from two Datasets based on related columns between them.
     * All rows from the right Dataset and the matched rows from the left Dataset will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the left side.
     * Additionally, a new column is added to the resulting Dataset, with its type specified by the user.
     *
     * @param right The other Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset to join on, and the value is the column name in the other Dataset to join on.
     * @param newColumnName The name of the new column to be added to the resulting Dataset.
     * @param newColumnType The type of the new column to be added to the resulting Dataset. It must be Object[], Collection, Map, or Bean class.
     * @param collSupplier A function that generates a collection to hold the joined rows for the new column for one-many or many-many mapping.
     * @return A new Dataset that is the result of the right join operation, with the additional column.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    @SuppressWarnings("rawtypes")
    Dataset rightJoin(Dataset right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException;

    /**
     * Performs a full join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The full join operation combines rows from two Datasets based on related columns between them.
     * All rows from both Datasets will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the side of the Dataset that does not have a match.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Dataset joined = left.fullJoin(Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 95}}), Arrays.asList("id"));
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param columnName The column name in this Dataset to join on.
     * @param joinColumnNameOnRight The column name in the other Dataset to join on.
     * @return A new Dataset that is the result of the full join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset fullJoin(Dataset right, String columnName, String joinColumnNameOnRight) throws IllegalArgumentException;

    /**
     * Performs a full join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The full join operation combines rows from two Datasets based on related columns between them.
     * All rows from both Datasets will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the side of the Dataset that does not have a match.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Dataset joined = left.fullJoin(Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 95}}), Arrays.asList("id"));
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset to join on, and the value is the column name in the other Dataset to join on.
     * @return A new Dataset that is the result of the full join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset fullJoin(Dataset right, Map<String, String> onColumnNames) throws IllegalArgumentException;

    /**
     * Performs a full join operation between this Dataset and another Dataset based on the specified column names.
     * <br />
     * The full join operation combines rows from two Datasets based on related columns between them.
     * All rows from both Datasets will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the side of the Dataset that does not have a match.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Dataset joined = left.fullJoin(Dataset.rows(Arrays.asList("id", "score"), new Object[][] {{1, 95}}), Arrays.asList("id"));
     * }</pre>
     *
     * @param right The other Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset to join on, and the value is the column name in the other Dataset to join on.
     * @param newColumnName The name of the new column to be added to the resulting Dataset.
     * @param newColumnType The type of the new column to be added to the resulting Dataset. It must be Object[], Collection, Map, or Bean class.
     * @return A new Dataset that is the result of the full join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    Dataset fullJoin(Dataset right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType) throws IllegalArgumentException;

    /**
     * Performs a full join operation between this Dataset and another Dataset based on the specified column names.
     * The full join operation combines rows from two Datasets based on related columns between them.
     * All rows from both Datasets will be included in the resulting Dataset.
     * If there is no match, the result is {@code null} on the side of the Dataset that does not have a match.
     *
     * @param right The other Dataset to join with.
     * @param onColumnNames A map where the key is the column name in this Dataset to join on, and the value is the column name in the other Dataset to join on.
     * @param newColumnName The name of the new column to be added to the resulting Dataset.
     * @param newColumnType The type of the new column to be added to the resulting Dataset. It must be Object[], Collection, Map, or Bean class.
     * @param collSupplier A function that generates a collection to hold the joined rows for the new column for one-many or many-many mapping.
     * @return A new Dataset that is the result of the full join operation.
     * @throws IllegalArgumentException if the specified column names are not found in the respective Datasets or the specified {@code right} Dataset is {@code null}, or if the specified {@code newColumnType} is not a supported type - Object[], Collection, Map, or Bean class.
     * @see <a href="https://stackoverflow.com/questions/38549">What is the difference between "INNER JOIN" and "OUTER JOIN"</a>
     */
    @SuppressWarnings("rawtypes")
    Dataset fullJoin(Dataset right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException;

    /**
     * Performs a union operation between this Dataset and another Dataset.
     * <br />
     * The union operation combines all rows from both Datasets into a new Dataset.
     * Duplicated rows detected by common columns in the returned Dataset will be eliminated.
     * The resulting Dataset will contain the union of columns from both Datasets, with {@code null} values for columns that don't exist in one of the source Datasets.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {3, "Charlie", 85}
     *     });
     *
     * Dataset result = dataset1.union(dataset2);
     * // The resulting Dataset will have columns: id, name, age, score
     * // Result contains: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {3, "Charlie", null, 85}
     * // Note: Duplicate rows are eliminated
     * }</pre>
     *
     * @param other The other Dataset to union with
     * @return A new Dataset that is the result of the union operation with duplicates eliminated
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if the Datasets do not have the same column structure
     * @see #unionAll(Dataset)
     * @see #unionAll(Dataset, boolean)
     * @see #intersect(Dataset)
     * @see #except(Dataset)
     */
    Dataset union(Dataset other) throws IllegalArgumentException;

    /**
     * Performs a union operation between this Dataset and another Dataset.
     * <br />
     * The union operation combines all rows from both Datasets into a new Dataset.
     * Duplicated rows detected by common columns in the returned Dataset will be eliminated.
     * The resulting Dataset will contain the union of columns from both Datasets, with {@code null} values for columns that don't exist in one of the source Datasets.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as they share at least one common column.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {3, "Charlie", 85}
     *     });
     *
     * // Require same columns - will throw IllegalArgumentException due to different columns
     * // Dataset result = dataset1.union(dataset2, true);
     *
     * // Allow different columns - combines all columns from both datasets
     * Dataset result = dataset1.union(dataset2, false);
     * // The resulting Dataset will have columns: id, name, age, score
     * // Result contains: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {3, "Charlie", null, 85}
     * // Note: Duplicate rows are eliminated
     * }</pre>
     *
     * @param other The other Dataset to union with
     * @param requiresSameColumns Whether both Datasets must have identical column structures
     * @return A new Dataset that is the result of the union operation with duplicates eliminated
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if requiresSameColumns is {@code true} and the Datasets do not have identical column structures
     * @see #union(Dataset)
     * @see #unionAll(Dataset)
     * @see #unionAll(Dataset, boolean)
     * @see #intersect(Dataset)
     * @see #except(Dataset)
     */
    Dataset union(Dataset other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a union operation between this Dataset and another Dataset.
     * <br />
     * The union operation combines all rows from both Datasets into a new Dataset.
     * Duplicated rows detected by the specified key columns in the returned Dataset will be eliminated.
     * The resulting Dataset will contain the union of columns from both Datasets, with {@code null} values for columns that don't exist in one of the source Datasets.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {3, "Charlie", 85}
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     *
     * Dataset result = dataset1.union(dataset2, keyColumns);
     * // Result contains columns: id, name, age, score
     * // Result contains: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {3, "Charlie", null, 85}
     * // Note: Duplicates based on key columns (id, name) are eliminated
     * }</pre>
     *
     * @param other The other Dataset to union with
     * @param keyColumnNames The collection of column names to be used as keys for duplicate detection
     * @return A new Dataset that is the result of the union operation with duplicates eliminated based on key columns
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if the keyColumnNames is {@code null} or empty,
     *                                  or if any of the specified key column names do not exist in either Dataset
     * @see #union(Dataset)
     * @see #union(Dataset, boolean)
     * @see #unionAll(Dataset)
     * @see #intersect(Dataset, Collection)
     * @see #except(Dataset, Collection)
     */
    Dataset union(Dataset other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs a union operation between this Dataset and another Dataset.
     * <br />
     * The union operation combines all rows from both Datasets into a new Dataset.
     * Duplicated rows detected by the specified key columns in the returned Dataset will be eliminated.
     * The resulting Dataset will contain the union of columns from both Datasets, with {@code null} values for columns that don't exist in one of the source Datasets.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as the specified key columns exist in both.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {3, "Charlie", 85}
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     *
     * // Require same columns - will throw IllegalArgumentException due to different columns
     * // Dataset result2 = dataset1.union(dataset2, keyColumns, true);
     *
     * // Allow different columns with key-based duplicate elimination
     * Dataset result = dataset1.union(dataset2, keyColumns, false);
     * // Result contains columns: id, name, age, score
     * // Result contains: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {3, "Charlie", null, 85}
     * // Note: Duplicates based on key columns (id, name) are eliminated
     * }</pre>
     *
     * @param other The other Dataset to union with
     * @param keyColumnNames The collection of column names to be used as keys for duplicate detection
     * @param requiresSameColumns Whether both Datasets must have identical column structures
     * @return A new Dataset that is the result of the union operation with duplicates eliminated based on key columns
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if the keyColumnNames is {@code null} or empty,
     *                                  or if requiresSameColumns is {@code true} and the Datasets do not have identical column structures,
     *                                  or if any of the specified key column names do not exist in either Dataset
     * @see #union(Dataset)
     * @see #union(Dataset, boolean)
     * @see #union(Dataset, Collection)
     * @see #unionAll(Dataset)
     * @see #intersect(Dataset, Collection)
     * @see #except(Dataset, Collection)
     */
    Dataset union(Dataset other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a union all operation between this Dataset and another Dataset.
     * <br />
     * The union all operation combines all rows from both Datasets into a new Dataset.
     * Unlike the union operation, union all includes duplicate rows detected by common columns in the resulting Dataset.
     * The resulting Dataset will contain the union of columns from both Datasets, with {@code null} values for columns that don't exist in one of the source Datasets.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {3, "Charlie", 85}
     *     });
     *
     * Dataset result = dataset1.unionAll(dataset2);
     * // Result contains columns: id, name, age, score
     * // Result contains: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {1, "Alice", 35, null}, {1, "Alice", null, 95}, {3, "Charlie", null, 85}
     * // Note: All rows are included, including duplicates
     * }</pre>
     *
     * @param other The other Dataset to union with
     * @return A new Dataset that is the result of the union all operation with all rows included
     * @throws IllegalArgumentException if the other Dataset is {@code null}
     * @see #union(Dataset)
     * @see #union(Dataset, boolean)
     * @see #union(Dataset, Collection)
     * @see #unionAll(Dataset, boolean)
     * @see #intersect(Dataset)
     * @see #except(Dataset)
     * @see #merge(Dataset)
     */
    Dataset unionAll(Dataset other) throws IllegalArgumentException;

    /**
     * Performs a union all operation between this Dataset and another Dataset with an option to require same columns.
     * <br />
     * The union all operation combines all rows from both Datasets into a new Dataset.
     * Unlike the union operation, union all includes duplicate rows detected by common columns in the resulting Dataset.
     * The resulting Dataset will contain the union of columns from both Datasets, with {@code null} values for columns that don't exist in one of the source Datasets.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {3, "Charlie", 85}
     *     });
     *
     * // Require same columns - will throw IllegalArgumentException due to different columns
     * // Dataset result2 = dataset1.unionAll(dataset2, true);
     *
     * // Allow different columns
     * Dataset result = dataset1.unionAll(dataset2, false);
     * // Result contains columns: id, name, age, score
     * // Result contains: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {1, "Alice", 35, null}, {1, "Alice", null, 95}, {3, "Charlie", null, 85}
     * // Note: All rows are included, including duplicates
     * }</pre>
     *
     * @param other The other Dataset to union with
     * @param requiresSameColumns Whether both Datasets must have identical column structures
     * @return A new Dataset that is the result of the union all operation with all rows included
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if requiresSameColumns is {@code true} and the Datasets do not have identical column structures
     * @see #union(Dataset)
     * @see #union(Dataset, boolean)
     * @see #union(Dataset, Collection)
     * @see #unionAll(Dataset)
     * @see #intersect(Dataset)
     * @see #except(Dataset)
     * @see #merge(Dataset, boolean)
     */
    Dataset unionAll(Dataset other, boolean requiresSameColumns) throws IllegalArgumentException;

    //    /**
    //     * Returns a new {@code Dataset} with all rows from this Dataset and which also appear in the specified {@code other} in common columns.
    //     * Duplicated rows in the returned {@code Dataset} will be eliminated.
    //     *
    //     * @param other
    //     * @param keyColumnNames this parameter won't be used. adding it here to be consistent with {@code union(Dataset, Collection)}
    //     * @return a new Dataset
    //     */
    //    @Beta
    //    Dataset unionAll(Dataset other, Collection<String> keyColumnNames);
    //
    //    /**
    //     * Returns a new {@code Dataset} with all rows from this Dataset and which also appear in the specified {@code other} in common columns.
    //     * Duplicated rows in the returned {@code Dataset} will be eliminated.
    //     *
    //     * @param other
    //     * @param keyColumnNames this parameter won't be used. Adding it here to be consistent with {@code union(Dataset, Collection, boolean)}
    //     * @param requiresSameColumns
    //     * @return a new Dataset
    //     */
    //    @Beta
    //    Dataset unionAll(Dataset other, Collection<String> keyColumnNames, boolean requiresSameColumns);

    /**
     * Performs an intersection operation between this Dataset and another Dataset.
     * <br />
     * Only the rows that have same values in all common columns between the two Datasets will be included in the result.
     * Duplicated rows detected by common columns in the returned Dataset will be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name"), 
     *     new Object[][] {
     *          {1, "Alice"}, 
     *          {2, "Bob"},
     *          {1, "Alice"}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name"), 
     *     new Object[][] {
     *          {1, "Alice"}, 
     *          {3, "Charlie"}
     *     });
     * 
     * Dataset result = dataset1.intersect(dataset2);
     * // Result contains only: {1, "Alice"}
     * }</pre>
     *
     * @param other The other Dataset to intersect with
     * @return A new Dataset that is the result of the intersection operation
     * @throws IllegalArgumentException if the other Dataset is {@code null}
     * @see #union(Dataset)
     * @see #except(Dataset)
     */
    Dataset intersect(Dataset other) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this Dataset and another Dataset.
     * <br />
     * Only the rows that have same values in all common columns between the two Datasets will be included in the result.
     * Duplicated rows detected by common columns in the returned Dataset will be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as they share at least one common column.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {3, "Charlie", 85}
     *     });
     *
     * // Require same columns - will throw IllegalArgumentException
     * // Dataset result = dataset1.intersect(dataset2, true);
     *
     * // Allow different columns - works fine
     * Dataset result = dataset1.intersect(dataset2, false);
     * // Result contains: {1, "Alice", 25} based on matching common columns id and name
     * }</pre>
     *
     * @param other The other Dataset to intersect with
     * @param requiresSameColumns Whether both Datasets must have the same columns
     * @return A new Dataset that is the result of the intersection operation
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if requiresSameColumns is {@code true} and the Datasets do not have the same columns
     * @see #intersect(Dataset)
     * @see #union(Dataset, boolean)
     * @see #except(Dataset, boolean)
     */
    Dataset intersect(Dataset other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this Dataset and another Dataset using specified key columns.
     * <br />
     * Only the rows that have same values in the specified key columns between the two Datasets will be included in the result.
     * Duplicated rows detected by the specified key columns in the returned Dataset will be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {3, "Charlie", 85}
     *     });
     *
     * Dataset result = dataset1.intersect(dataset2, Arrays.asList("id", "name"));
     * // Result contains: {1, "Alice", 25} based on matching id and name
     * }</pre>
     *
     * @param other The other Dataset to intersect with
     * @param keyColumnNames The collection of column names to be used as keys for intersection
     * @return A new Dataset that is the result of the intersection operation
     * @throws IllegalArgumentException if the other Dataset is {@code null} or if keyColumnNames is {@code null} or empty
     * @see #intersect(Dataset)
     * @see #union(Dataset, Collection)
     * @see #except(Dataset, Collection)
     */
    Dataset intersect(Dataset other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this Dataset and another Dataset using specified key columns.
     * <br />
     * Only the rows that have same values in the specified key columns between the two Datasets will be included in the result.
     * Duplicated rows detected by the specified key columns in the returned Dataset will be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as the specified key columns exist in both.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {3, "Charlie", 85}
     *     });
     *
     * // Require same columns - will throw IllegalArgumentException
     * // Dataset result = dataset1.intersect(dataset2, Arrays.asList("id", "name"), true);
     *
     * // Allow different columns - works fine
     * Dataset result = dataset1.intersect(dataset2, Arrays.asList("id", "name"), false);
     * // Result contains: {1, "Alice", 25} based on matching id and name
     * }</pre>
     *
     * @param other The other Dataset to intersect with
     * @param keyColumnNames The collection of column names to be used as keys for intersection
     * @param requiresSameColumns Whether both Datasets must have the same columns
     * @return A new Dataset that is the result of the intersection operation
     * @throws IllegalArgumentException if the other Dataset is {@code null}, if keyColumnNames is {@code null} or empty, 
     *                                  or if requiresSameColumns is {@code true} and the Datasets do not have the same columns
     * @see #intersect(Dataset)
     * @see #intersect(Dataset, Collection)
     * @see #union(Dataset, Collection, boolean)
     * @see #except(Dataset, Collection, boolean)
     */
    Dataset intersect(Dataset other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this Dataset and another Dataset.
     * <br />
     * Only the rows that have same values in all common columns between the two Datasets will be included in the result.
     * Duplicated rows detected by common columns in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 25}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {2, "Bob", 95},
     *          {3, "Charlie", 85},
     *     });
     *
     * Dataset result = dataset1.intersectAll(dataset2);
     * // Result contains: {1, "Alice", 25}, {2, "Bob", 30}, {1, "Alice", 35} based on matching common columns id and name
     * // Note: Duplicates are preserved
     * }</pre>
     *
     * @param other The other Dataset to intersect with
     * @return A new Dataset that is the result of the intersection operation with duplicates preserved
     * @throws IllegalArgumentException if the other Dataset is {@code null}
     * @see #intersect(Dataset)
     * @see #unionAll(Dataset)
     * @see #exceptAll(Dataset)
     */
    Dataset intersectAll(Dataset other) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this Dataset and another Dataset.
     * <br />
     * Only the rows that have same values in all common columns between the two Datasets will be included in the result.
     * Duplicated rows detected by common columns in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as they share at least one common column.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 25}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {2, "Bob", 95},
     *          {3, "Charlie", 85},
     *     });
     *
     * // Require same columns - will throw IllegalArgumentException
     * // Dataset result = dataset1.intersectAll(dataset2, true);
     *
     * // Allow different columns - works fine
     * Dataset result = dataset1.intersectAll(dataset2, false);
     * // Result contains: {1, "Alice", 25}, {2, "Bob", 30}, {1, "Alice", 25} based on matching common columns id and name
     * // Note: Duplicates are preserved
     * }</pre>
     *
     * @param other The other Dataset to intersect with
     * @param requiresSameColumns Whether both Datasets must have the same columns
     * @return A new Dataset that is the result of the intersection operation with duplicates preserved
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if requiresSameColumns is {@code true} and the Datasets do not have the same columns
     * @see #intersectAll(Dataset)
     * @see #intersect(Dataset, boolean)
     * @see #unionAll(Dataset, boolean)
     * @see #exceptAll(Dataset, boolean)
     */
    Dataset intersectAll(Dataset other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this Dataset and another Dataset using specified key columns.
     * <br />
     * Only the rows that have same values in the specified key columns between the two Datasets will be included in the result.
     * Duplicated rows detected by the specified key columns in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 25}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {2, "Bob", 95},
     *          {3, "Charlie", 85},
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * Dataset result = dataset1.intersectAll(dataset2, keyColumns);
     * // Result contains: {1, "Alice", 25}, {2, "Bob", 30}, {1, "Alice", 25} based on matching key columns id and name
     * // Note: Duplicates are preserved
     * }</pre>
     *
     * @param other The other Dataset to intersect with
     * @param keyColumnNames The collection of column names to be used as keys for the intersection operation
     * @return A new Dataset that is the result of the intersection operation with duplicates preserved
     * @throws IllegalArgumentException if the other Dataset is {@code null}, 
     *                                  or if the keyColumnNames is {@code null} or empty,
     *                                  or if any of the specified key column names do not exist in either Dataset
     * @see #intersectAll(Dataset)
     * @see #intersectAll(Dataset, boolean)
     * @see #intersect(Dataset, Collection)
     * @see #exceptAll(Dataset, Collection)
     */
    Dataset intersectAll(Dataset other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs an intersection operation between this Dataset and another Dataset using specified key columns.
     * <br />
     * Only the rows that have same values in the specified key columns between the two Datasets will be included in the result.
     * Duplicated rows detected by the specified key columns in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as the specified key columns exist in both.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {1, "Alice", 25}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {1, "Alice", 95},
     *          {2, "Bob", 95},
     *          {3, "Charlie", 85},
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * 
     * // Require same columns - will throw IllegalArgumentException
     * // Dataset result = dataset1.intersectAll(dataset2, keyColumns, true);
     *
     * // Allow different columns - works fine
     * Dataset result = dataset1.intersectAll(dataset2, keyColumns, false);
     * // Result contains: {1, "Alice", 25}, {2, "Bob", 30}, {1, "Alice", 25} based on matching key columns id and name
     * // Note: Duplicates are preserved
     * }</pre>
     *
     * @param other The other Dataset to intersect with
     * @param keyColumnNames The collection of column names to be used as keys for the intersection operation
     * @param requiresSameColumns Whether both Datasets must have the same columns
     * @return A new Dataset that is the result of the intersection operation with duplicates preserved
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if the keyColumnNames is {@code null} or empty,
     *                                  or if any of the specified key column names do not exist in either Dataset,
     *                                  or if requiresSameColumns is {@code true} and the Datasets do not have the same columns
     * @see #intersectAll(Dataset)
     * @see #intersectAll(Dataset, boolean)
     * @see #intersectAll(Dataset, Collection)
     * @see #intersect(Dataset, Collection, boolean)
     * @see #exceptAll(Dataset, Collection, boolean)
     */
    Dataset intersectAll(Dataset other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this Dataset and another Dataset.
     * <br />
     * The difference operation returns a new Dataset that includes rows that are in this Dataset but not in the provided Dataset.
     * This operation compares all common columns between the two Datasets. If the Datasets have different column structures,
     * only the common columns will be used for the comparison.
     * Duplicated rows detected by common columns in the returned Dataset will be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {3, "Charlie", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {3, "Charlie", 85}
     *     });
     *
     * Dataset result = dataset1.except(dataset2);
     * // Result contains: {1, "Alice", 25}, {2, "Bob", 30} based on matching common columns id and name
     * // Note: Duplicates are eliminated
     * }</pre>
     *
     * @param other The other Dataset to compare with
     * @return A new Dataset that is the result of the difference operation with duplicates eliminated
     * @throws IllegalArgumentException if the other Dataset is {@code null}
     * @see #except(Dataset, boolean)
     * @see #exceptAll(Dataset)
     * @see #intersect(Dataset)
     * @see #union(Dataset)
     */
    Dataset except(Dataset other) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this Dataset and another Dataset.
     * <br />
     * The difference operation returns a new Dataset that includes rows that are in this Dataset but not in the provided Dataset.
     * This operation compares all common columns between the two Datasets. If the Datasets have different column structures,
     * only the common columns will be used for the comparison.
     * Duplicated rows detected by common columns in the returned Dataset will be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as they share at least one common column.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {3, "Charlie", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {3, "Charlie", 85}
     *     });
     *
     * // Require same columns - will throw IllegalArgumentException
     * // Dataset result = dataset1.except(dataset2, true);
     *
     * // Allow different columns - works fine
     * Dataset result = dataset1.except(dataset2, false);
     * // Result contains: {1, "Alice", 25}, {2, "Bob", 30} based on matching common columns id and name
     * // Note: Duplicates are eliminated
     * }</pre>
     *
     * @param other The other Dataset to compare with
     * @param requiresSameColumns Whether both Datasets must have the same columns
     * @return A new Dataset that is the result of the difference operation with duplicates eliminated
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if requiresSameColumns is {@code true} and the Datasets do not have the same columns
     * @see #except(Dataset)
     * @see #except(Dataset, Collection)
     * @see #exceptAll(Dataset, boolean)
     * @see #intersect(Dataset, boolean)
     * @see #union(Dataset, boolean)
     */
    Dataset except(Dataset other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this Dataset and another Dataset based on specified key columns.
     * <br />
     * The difference operation returns a new Dataset that includes rows that are in this Dataset but not in the provided Dataset.
     * Duplicated rows detected by common columns in the returned Dataset will be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {3, "Charlie", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {3, "Charlie", 85}
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * Dataset result = dataset1.except(dataset2, keyColumns);
     * // Result contains: {1, "Alice", 25}, {2, "Bob", 30} based on matching key columns id and name
     * // Note: Duplicates are eliminated
     * }</pre>
     *
     * @param other The other Dataset to compare with
     * @param keyColumnNames The collection of column names to be used as keys for the difference operation
     * @return A new Dataset that is the result of the difference operation with duplicates eliminated
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if the keyColumnNames is {@code null} or empty,
     *                                  or if any of the specified key column names do not exist in either Dataset
     * @see #except(Dataset)
     * @see #except(Dataset, boolean)
     * @see #exceptAll(Dataset, Collection)
     * @see #intersect(Dataset, Collection)
     * @see #union(Dataset, Collection)
     */
    Dataset except(Dataset other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this Dataset and another Dataset based on specified key columns.
     * <br />
     * The difference operation returns a new Dataset that includes rows that are in this Dataset but not in the provided Dataset.
     * Duplicated rows detected by common columns in the returned Dataset will be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as the specified key columns exist in both.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {3, "Charlie", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {3, "Charlie", 85}
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * 
     * // Require same columns - will throw IllegalArgumentException
     * // Dataset result = dataset1.except(dataset2, keyColumns, true);
     *
     * // Allow different columns - works fine
     * Dataset result = dataset1.except(dataset2, keyColumns, false);
     * // Result contains: {1, "Alice", 25}, {2, "Bob", 30} based on matching key columns id and name
     * // Note: Duplicates are eliminated
     * }</pre>
     *
     * @param other The other Dataset to compare with
     * @param keyColumnNames The collection of column names to be used as keys for the difference operation
     * @param requiresSameColumns Whether both Datasets must have the same columns
     * @return A new Dataset that is the result of the difference operation with duplicates eliminated
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if the keyColumnNames is {@code null} or empty,
     *                                  or if any of the specified key column names do not exist in either Dataset,
     *                                  or if requiresSameColumns is {@code true} and the Datasets do not have the same columns
     * @see #except(Dataset)
     * @see #except(Dataset, boolean)
     * @see #except(Dataset, Collection)
     * @see #exceptAll(Dataset, Collection, boolean)
     * @see #intersect(Dataset, Collection, boolean)
     * @see #union(Dataset, Collection, boolean)
     */
    Dataset except(Dataset other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this Dataset and another Dataset.
     * <br />
     * The difference operation returns a new Dataset that includes rows that are in this Dataset but not in the provided Dataset.
     * This operation compares all common columns between the two Datasets. If the Datasets have different column structures,
     * only the common columns will be used for the comparison.
     * Duplicated rows detected by common columns in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {3, "Charlie", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {3, "Charlie", 85}
     *     });
     *
     * Dataset result = dataset1.exceptAll(dataset2);
     * // Result contains: {1, "Alice", 25}, {1, "Alice", 25}, {2, "Bob", 30}
     * // Note: Duplicates are NOT eliminated
     * }</pre>
     *
     * @param other The other Dataset to compare with
     * @return A new Dataset that is the result of the difference operation with duplicates preserved
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if the Datasets do not have the same column structure
     * @see #except(Dataset)
     * @see #exceptAll(Dataset, boolean)
     * @see #exceptAll(Dataset, Collection)
     * @see #intersectAll(Dataset)
     * @see #unionAll(Dataset)
     */
    Dataset exceptAll(Dataset other) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this Dataset and another Dataset.
     * <br />
     * The difference operation returns a new Dataset that includes rows that are in this Dataset but not in the provided Dataset.
     * This operation compares all common columns between the two Datasets. If the Datasets have different column structures,
     * only the common columns will be used for the comparison.
     * Duplicated rows detected by common columns in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as they share at least one common column.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {3, "Charlie", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {3, "Charlie", 85}
     *     });
     *
     * // Require same columns - will throw IllegalArgumentException
     * // Dataset result = dataset1.exceptAll(dataset2, true);
     *
     * // Allow different columns - works fine, compares common columns (id, name)
     * Dataset result = dataset1.exceptAll(dataset2, false);
     * // Result contains: {1, "Alice", 25}, {1, "Alice", 25}, {2, "Bob", 30}
     * // Note: Duplicates are NOT eliminated
     * }</pre>
     *
     * @param other The other Dataset to compare with
     * @param requiresSameColumns Whether both Datasets must have the same columns
     * @return A new Dataset that is the result of the difference operation with duplicates preserved
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if requiresSameColumns is {@code true} and the Datasets do not have the same columns
     * @see #except(Dataset, boolean)
     * @see #exceptAll(Dataset)
     * @see #exceptAll(Dataset, Collection)
     * @see #intersectAll(Dataset, boolean)
     * @see #unionAll(Dataset, boolean)
     */
    Dataset exceptAll(Dataset other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this Dataset and another Dataset based on specified key columns.
     * <br />
     * The difference operation returns a new Dataset that includes rows that are in this Dataset but not in the provided Dataset.
     * Duplicated rows detected by common columns in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {3, "Charlie", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {3, "Charlie", 35}
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     *
     * Dataset result = dataset1.exceptAll(dataset2, keyColumns);
     * // Result contains: {1, "Alice", 25}, {1, "Alice", 25}, {2, "Bob", 30} based on matching key columns id and name
     * // Note: Duplicates are NOT eliminated
     * }</pre>
     *
     * @param other The other Dataset to compare with
     * @param keyColumnNames The collection of column names to be used as keys for the difference operation
     * @return A new Dataset that is the result of the difference operation with duplicates preserved
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if the keyColumnNames is {@code null} or empty,
     *                                  or if any of the specified key column names do not exist in either Dataset,
     *                                  or if the Datasets do not have the same column structure
     * @see #except(Dataset, Collection)
     * @see #exceptAll(Dataset)
     * @see #exceptAll(Dataset, boolean)
     * @see #exceptAll(Dataset, Collection, boolean)
     * @see #intersectAll(Dataset, Collection)
     * @see #unionAll(Dataset)
     */
    Dataset exceptAll(Dataset other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Performs a difference operation between this Dataset and another Dataset based on specified key columns.
     * <br />
     * The difference operation returns a new Dataset that includes rows that are in this Dataset but not in the provided Dataset.
     * Duplicated rows detected by common columns in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as the specified key columns exist in both.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
     *     new Object[][] {
     *          {1, "Alice", 25},
     *          {1, "Alice", 25},
     *          {2, "Bob", 30},
     *          {3, "Charlie", 35}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"),
     *     new Object[][] {
     *          {3, "Charlie", 85}
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     *
     * // Require same columns - will throw IllegalArgumentException due to different columns
     * // Dataset result = dataset1.exceptAll(dataset2, keyColumns, true);
     *
     * // Allow different columns - works fine, compares only key columns (id, name)
     * Dataset result = dataset1.exceptAll(dataset2, keyColumns, false);
     * // Result contains: {1, "Alice", 25}, {1, "Alice", 25}, {2, "Bob", 30} based on matching key columns
     * // Note: Duplicates are NOT eliminated
     * }</pre>
     *
     * @param other The other Dataset to compare with
     * @param keyColumnNames The collection of column names to be used as keys for the difference operation
     * @param requiresSameColumns Whether both Datasets must have the same columns
     * @return A new Dataset that is the result of the difference operation with duplicates preserved
     * @throws IllegalArgumentException if the other Dataset is {@code null},
     *                                  or if the keyColumnNames is {@code null} or empty,
     *                                  or if any of the specified key column names do not exist in either Dataset,
     *                                  or if requiresSameColumns is {@code true} and the Datasets do not have the same columns
     * @see #except(Dataset, Collection, boolean)
     * @see #exceptAll(Dataset)
     * @see #exceptAll(Dataset, boolean)
     * @see #exceptAll(Dataset, Collection)
     * @see #intersectAll(Dataset, Collection, boolean)
     * @see #unionAll(Dataset, boolean)
     */
    Dataset exceptAll(Dataset other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new Dataset containing rows that appear in both this Dataset and the specified Dataset.
     * <br />
     * The intersection contains rows that exist in both Datasets based on matching column values.
     * For rows that appear multiple times, the result contains the minimum number of occurrences present in both Datasets.
     * Duplicated rows in the returned {@code Dataset} will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name"),
     *     new Object[][] {
     *          {1, "Alice"},
     *          {2, "Bob"},
     *          {2, "Bob"}  // duplicate row
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name"),
     *     new Object[][] {
     *          {1, "Alice"},
     *          {2, "Bob"},
     *          {3, "Charlie"},
     *          {2, "Bob"},  // duplicate row
     *          {2, "Bob"}   // another duplicate
     *     });
     *
     * Dataset result = dataset1.intersection(dataset2);
     * // Returns contains {1, "Alice"} once and {2, "Bob"} twice (minimum of 2 and 3 occurrences)
     * }</pre>
     *
     * @param other The Dataset to find common rows with. Must not be {@code null}.
     * @return A new Dataset containing rows present in both Datasets, with duplicates handled based on minimum occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}, or if the two Datasets don't have the same columns.
     * @see #intersection(Dataset, boolean)
     * @see #intersection(Dataset, Collection)
     * @see #intersection(Dataset, Collection, boolean)
     * @see #intersect(Dataset)
     * @see #intersectAll(Dataset)
     * @see #union(Dataset)
     * @see #except(Dataset)
     */
    Dataset intersection(Dataset other) throws IllegalArgumentException;

    /**
     * Returns a new Dataset containing rows that appear in both this Dataset and the specified Dataset.
     * <br />
     * The intersection contains rows that exist in both Datasets based on matching column values.
     * For rows that appear multiple times, the result contains the minimum number of occurrences present in both Datasets.
     * Duplicated rows in the returned {@code Dataset} will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as they share at least one common column.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name"),
     *     new Object[][] {
     *          {1, "Alice"},
     *          {2, "Bob"},
     *          {2, "Bob"}  // duplicate row
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name"),
     *     new Object[][] {
     *          {1, "Alice"},
     *          {2, "Bob"},
     *          {3, "Charlie"},
     *          {2, "Bob"},  // duplicate row
     *          {2, "Bob"}   // another duplicate
     *     });
     *
     * Dataset result = dataset1.intersection(dataset2, true);
     * // Returns contains {1, "Alice"} once and {2, "Bob"} twice (minimum of 2 and 3 occurrences)
     * }</pre>
     *
     * @param other The Dataset to find common rows with. Must not be {@code null}.
     * @param requiresSameColumns A boolean that indicates whether both Datasets should have the same columns.
     * @return A new Dataset containing rows present in both Datasets, with duplicates handled based on minimum occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}, or if {@code requiresSameColumns} is {@code true} and the Datasets do not have the same columns, or if the two Datasets don't have common columns when {@code requiresSameColumns} is {@code false}.
     * @see #intersection(Dataset)
     * @see #intersection(Dataset, Collection)
     * @see #intersection(Dataset, Collection, boolean)
     * @see #intersect(Dataset)
     * @see #intersectAll(Dataset)
     * @see #union(Dataset)
     * @see #except(Dataset)
     */
    Dataset intersection(Dataset other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new Dataset containing rows that appear in both this Dataset and the specified Dataset.
     * <br />
     * The intersection contains rows that exist in both Datasets based on the values in the specified key columns.
     * For rows that appear multiple times, the result contains the minimum number of occurrences present in both Datasets.
     * Duplicated rows in the returned {@code Dataset} will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *          {1, "Alice", "HR"},
     *          {2, "Bob", "Engineering"},
     *          {2, "Bob", "Engineering"}  // duplicate row
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *          {1, "Alice", 75000},
     *          {2, "Bob", 75000},
     *          {3, "Charlie", 65000},
     *          {4, "Dave", 70000},
     *          {2, "Bob", 80000}  // different salary but same keys
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * Dataset result = dataset1.intersection(dataset2, keyColumns);
     * // Result contains {1, "Alice", "HR"} once and {2, "Bob", "Engineering"} twice (minimum of 2 and 3 occurrences)
     * // with column structure matching dataset1
     * }</pre>
     *
     * @param other The Dataset to find common rows with. Must not be {@code null}.
     * @param keyColumnNames The column names to use for matching rows between Datasets. Must not be {@code null} or empty.
     * @return A new Dataset containing rows whose key column values appear in both Datasets, with duplicates handled based on minimum occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}, or if {@code keyColumnNames} is {@code null} or empty, or if any specified key column doesn't exist in either Dataset.
     * @see #intersection(Dataset)
     * @see #intersection(Dataset, boolean)
     * @see #intersection(Dataset, Collection, boolean)
     * @see #intersect(Dataset, Collection)
     * @see #intersectAll(Dataset, Collection)
     */
    Dataset intersection(Dataset other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Returns a new Dataset containing rows that appear in both this Dataset and the specified Dataset.
     * <br />
     * The intersection contains rows that exist in both Datasets based on the values in the specified key columns.
     * For rows that appear multiple times, the result contains the minimum number of occurrences present in both Datasets.
     * Duplicated rows in the returned {@code Dataset} will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as the specified key columns exist in both.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *          {1, "Alice", "HR"},
     *          {2, "Bob", "Engineering"},
     *          {2, "Bob", "Engineering"}  // duplicate row
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *          {1, "Alice", 75000},
     *          {2, "Bob", 75000},
     *          {3, "Charlie", 65000},
     *          {4, "Dave", 70000},
     *          {2, "Bob", 80000}  // different salary but same keys
     *     });
     * 
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * Dataset result = dataset1.intersection(dataset2, keyColumns, false);
     * // Result contains {1, "Alice", "HR"} once and {2, "Bob", "Engineering"} twice (minimum of 2 and 3 occurrences)
     * // with column structure matching dataset1
     * }</pre>
     *
     * @param other The Dataset to find common rows with. Must not be {@code null}.
     * @param keyColumnNames The column names to use for matching rows between Datasets. Must not be {@code null} or empty.
     * @param requiresSameColumns A boolean that indicates whether both Datasets should have the same columns.
     * @return A new Dataset containing rows whose key column values appear in both Datasets, with duplicates handled based on minimum occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}, or if {@code keyColumnNames} is {@code null} or empty, or if any specified key column doesn't exist in either Dataset, or if {@code requiresSameColumns} is {@code true} and the Datasets do not have the same columns.
     * @see #intersection(Dataset)
     * @see #intersection(Dataset, boolean)
     * @see #intersection(Dataset, Collection)
     * @see #intersect(Dataset, Collection)
     * @see #intersectAll(Dataset, Collection)
     */
    Dataset intersection(Dataset other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new Dataset with the rows in this Dataset but not in the specified Dataset {@code other}, considering the number of occurrences of each row.
     * <br />
     * The comparison is performed on common columns only between the two Datasets.
     * Duplicated rows in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *          {1, "Alice", "HR"},
     *          {2, "Bob", "Engineering"},
     *          {3, "Charlie", "Marketing"},
     *          {3, "Charlie", "Marketing"}  // duplicate row
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *          {1, "Alice", 50000},
     *          {4, "Dave", 60000},
     *          {3, "Charlie", 55000}
     *     });
     *
     * Dataset result = dataset1.difference(dataset2);
     * // Result contains {2, "Bob", "Engineering"} and {3, "Charlie", "Marketing"} once
     * // One Charlie row remains because dataset1 has two occurrences and dataset2 has one
     * }</pre>
     *
     * @param other The Dataset to compare against this Dataset. Must not be {@code null}.
     * @return A new Dataset containing the rows that are present in this Dataset but not in the specified Dataset, considering the number of occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}, or if the two Datasets don't have common columns.
     * @see #difference(Dataset, boolean)
     * @see #difference(Dataset, Collection)
     * @see #difference(Dataset, Collection, boolean)
     * @see #symmetricDifference(Dataset)
     * @see #intersection(Dataset)
     */
    Dataset difference(Dataset other) throws IllegalArgumentException;

    /**
     * Returns a new Dataset with the rows in this Dataset but not in the specified Dataset {@code other}, considering the number of occurrences of each row.
     * <br />
     * The comparison is performed on common columns only between the two Datasets.
     * Duplicated rows in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as they share at least one common column.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *          {1, "Alice", "HR"},
     *          {2, "Bob", "Engineering"},
     *          {3, "Charlie", "Marketing"},
     *          {3, "Charlie", "Marketing"}  // duplicate row
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *          {1, "Alice", 50000},
     *          {4, "Dave", 60000},
     *          {3, "Charlie", 55000}
     *     });
     *
     * Dataset result = dataset1.difference(dataset2, false);
     * // Result contains {2, "Bob", "Engineering"} and {3, "Charlie", "Marketing"} once
     * // One Charlie row remains because dataset1 has two occurrences and dataset2 has one
     * }</pre>
     *
     * @param other The Dataset to compare against this Dataset. Must not be {@code null}.
     * @param requiresSameColumns A boolean that indicates whether both Datasets should have the same columns.
     * @return A new Dataset containing the rows that are present in this Dataset but not in the specified Dataset, considering the number of occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}, or if {@code requiresSameColumns} is {@code true} and the two Datasets don't have the same columns, or if {@code requiresSameColumns} is {@code false} and the two Datasets don't have common columns.
     * @see #difference(Dataset)
     * @see #difference(Dataset, Collection)
     * @see #difference(Dataset, Collection, boolean)
     * @see #symmetricDifference(Dataset)
     * @see #intersection(Dataset)
     */
    Dataset difference(Dataset other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new Dataset with the rows in this Dataset but not in the specified Dataset {@code other}, considering the number of occurrences of each row.
     * <br />
     * The comparison is performed on the specified key columns only between the two Datasets.
     * Duplicated rows in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *          {1, "Alice", "HR"},
     *          {2, "Bob", "Engineering"},
     *          {3, "Charlie", "Marketing"},
     *          {3, "Charlie", "Finance"}  // duplicate key values
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *          {1, "Alice", 50000},
     *          {3, "Charlie", 60000}
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * Dataset result = dataset1.difference(dataset2, keyColumns);
     * // Result contains {2, "Bob", "Engineering"} and {3, "Charlie", "Finance"}
     * // One Charlie row remains because dataset1 has two occurrences and dataset2 has one
     * }</pre>
     *
     * @param other The Dataset to compare against this Dataset. Must not be {@code null}.
     * @param keyColumnNames The column names to use for matching rows between Datasets. Must not be {@code null} or empty.
     * @return A new Dataset containing the rows that are present in this Dataset but not in the specified Dataset, based on the specified key columns and considering the number of occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}, or if {@code keyColumnNames} is {@code null} or empty, or if any specified key column doesn't exist in either Dataset.
     * @see #difference(Dataset)
     * @see #difference(Dataset, boolean)
     * @see #difference(Dataset, Collection, boolean)
     * @see #symmetricDifference(Dataset)
     * @see #intersection(Dataset, Collection)
     */
    Dataset difference(Dataset other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Returns a new Dataset with the rows in this Dataset but not in the specified Dataset {@code other}, considering the number of occurrences of each row.
     * <br />
     * The comparison is performed on the specified key columns only between the two Datasets.
     * Duplicated rows in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will have the same column structure as this Dataset.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as the specified key columns exist in both.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *          {1, "Alice", "HR"},
     *          {2, "Bob", "Engineering"},
     *          {3, "Charlie", "Marketing"},
     *          {3, "Charlie", "Finance"}  // duplicate key values
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *          {1, "Alice", 50000},
     *          {3, "Charlie", 60000}
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * Dataset result = dataset1.difference(dataset2, keyColumns, false);
     * // Result contains {2, "Bob", "Engineering"} and {3, "Charlie", "Finance"}
     * // One Charlie row remains because dataset1 has two occurrences and dataset2 has one
     * }</pre>
     *
     * @param other The Dataset to compare against this Dataset. Must not be {@code null}.
     * @param keyColumnNames The column names to use for matching rows between Datasets. Must not be {@code null} or empty.
     * @param requiresSameColumns A boolean that indicates whether both Datasets should have the same columns.
     * @return A new Dataset containing the rows that are present in this Dataset but not in the specified Dataset, based on the specified key columns and considering the number of occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}, or if {@code keyColumnNames} is {@code null} or empty, or if any specified key column doesn't exist in either Dataset, or if {@code requiresSameColumns} is {@code true} and the two Datasets don't have the same columns, or if {@code requiresSameColumns} is {@code false} and the two Datasets don't have common columns.
     * @see #difference(Dataset)
     * @see #difference(Dataset, boolean)
     * @see #difference(Dataset, Collection)
     * @see #symmetricDifference(Dataset)
     * @see #intersection(Dataset, Collection, boolean)
     */
    Dataset difference(Dataset other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new Dataset containing rows that are present in either this Dataset or the specified Dataset,
     * <br />
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For rows that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     * The comparison is performed on the common columns between the two Datasets.
     * Duplicated rows in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will contain the union of columns from both Datasets, with {@code null} values for columns that don't exist in one of the source Datasets.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *          {1, "Alice", "HR"},
     *          {2, "Bob", "Engineering"},
     *          {2, "Bob", "Engineering"},  // duplicate row
     *          {3, "Charlie", "Marketing"}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *          {2, "Bob", 50000},
     *          {3, "Charlie", 55000},
     *          {4, "Dave", 60000}
     *     });
     *
     * Dataset result = dataset1.symmetricDifference(dataset2);
     * // Result contains {1, "Alice", "HR", null}, one occurrence of {2, "Bob", "Engineering", null} and {4, "Dave", null, 60000}
     * // One Bob row remains because dataset1 has two occurrences and dataset2 has one
     * }</pre>
     *
     * @param other The Dataset to find symmetric difference with this Dataset. Must not be {@code null}.
     * @return A new Dataset containing rows that are present in either this Dataset or the specified Dataset, but not in both, considering the number of occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null} or if the two Datasets don't have the same columns.
     * @see #symmetricDifference(Dataset, boolean)
     * @see #symmetricDifference(Dataset, Collection)
     * @see #symmetricDifference(Dataset, Collection, boolean)
     * @see #difference(Dataset)
     * @see #intersection(Dataset)
     */
    Dataset symmetricDifference(Dataset other) throws IllegalArgumentException;

    /**
     * Returns a new Dataset containing rows that are present in either this Dataset or the specified Dataset,
     * <br />
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For rows that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     * The comparison is performed on the common columns between the two Datasets.
     * Duplicated rows in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will contain the union of columns from both Datasets, with {@code null} values for columns that don't exist in one of the source Datasets.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as they share at least one common column.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *          {1, "Alice", "HR"},
     *          {2, "Bob", "Engineering"},
     *          {2, "Bob", "Engineering"},  // duplicate row
     *          {3, "Charlie", "Marketing"}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *          {2, "Bob", 50000},
     *          {3, "Charlie", 55000},
     *          {4, "Dave", 60000}
     *     });
     *
     * Dataset result = dataset1.symmetricDifference(dataset2, false);
     * // Result contains {1, "Alice", "HR", null}, one occurrence of {2, "Bob", "Engineering", null} and {4, "Dave", null, 60000}
     * // One Bob row remains because dataset1 has two occurrences and dataset2 has one
     * }</pre>
     *
     * @param other The Dataset to find symmetric difference with this Dataset. Must not be {@code null}.
     * @param requiresSameColumns A boolean that indicates whether both Datasets should have the same columns.
     * @return A new Dataset containing rows that are present in either this Dataset or the specified Dataset, but not in both, considering the number of occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}, or if {@code requiresSameColumns} is {@code true} and the two Datasets don't have the same columns, or if {@code requiresSameColumns} is {@code false} and the two Datasets don't have common columns.
     * @see #symmetricDifference(Dataset)
     * @see #symmetricDifference(Dataset, Collection)
     * @see #symmetricDifference(Dataset, Collection, boolean)
     * @see #difference(Dataset, boolean)
     * @see #intersection(Dataset, boolean)
     */
    Dataset symmetricDifference(Dataset other, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Returns a new Dataset containing rows that are present in either this Dataset or the specified Dataset,
     * <br />
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For rows that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     * The comparison is performed on the specified key columns only between the two Datasets.
     * Duplicated rows in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will contain the union of columns from both Datasets, with {@code null} values for columns that don't exist in one of the source Datasets.
     * <br />
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *          {1, "Alice", "HR"},
     *          {2, "Bob", "Engineering"},
     *          {2, "Bob", "Engineering"},  // duplicate row
     *          {3, "Charlie", "Marketing"}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *          {2, "Bob", 50000},
     *          {3, "Charlie", 55000},
     *          {4, "Dave", 60000}
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * Dataset result = dataset1.symmetricDifference(dataset2, keyColumns);
     * // Result contains {1, "Alice", "HR", null}, one occurrence of {2, "Bob", "Engineering", null} and {4, "Dave", null, 60000}
     * // One Bob row remains because dataset1 has two occurrences and dataset2 has one
     * }</pre>
     *
     * @param other The Dataset to find symmetric difference with this Dataset. Must not be {@code null}.
     * @param keyColumnNames The column names to use for matching rows between Datasets. Must not be {@code null} or empty.
     * @return A new Dataset containing rows that are present in either this Dataset or the specified Dataset, but not in both, based on the specified key columns and considering the number of occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}, or if {@code keyColumnNames} is {@code null} or empty, or if any specified key column doesn't exist in both Datasets.
     * @see #symmetricDifference(Dataset)
     * @see #symmetricDifference(Dataset, boolean)
     * @see #symmetricDifference(Dataset, Collection, boolean)
     * @see #difference(Dataset, Collection)
     * @see #intersection(Dataset, Collection)
     */
    Dataset symmetricDifference(Dataset other, Collection<String> keyColumnNames) throws IllegalArgumentException;

    /**
     * Returns a new Dataset containing rows that are present in either this Dataset or the specified Dataset,
     * <br />
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For rows that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     * The comparison is performed on the specified key columns only between the two Datasets.
     * Duplicated rows in the returned Dataset will NOT be eliminated.
     * The resulting Dataset will contain the union of columns from both Datasets, with {@code null} values for columns that don't exist in one of the source Datasets.
     * <br />
     * If {@code requiresSameColumns} is {@code true}, both Datasets must have the same columns, otherwise an {@code IllegalArgumentException} will be thrown.
     * If {@code requiresSameColumns} is {@code false}, the Datasets can have different columns as long as the specified key columns exist in both.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
     *     new Object[][] {
     *          {1, "Alice", "HR"},
     *          {2, "Bob", "Engineering"},
     *          {2, "Bob", "Engineering"},  // duplicate row
     *          {3, "Charlie", "Marketing"}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
     *     new Object[][] {
     *          {2, "Bob", 50000},
     *          {3, "Charlie", 55000},
     *          {4, "Dave", 60000}
     *     });
     *
     * Collection<String> keyColumns = Arrays.asList("id", "name");
     * Dataset result = dataset1.symmetricDifference(dataset2, keyColumns, false);
     * // Result contains {1, "Alice", "HR", null}, one occurrence of {2, "Bob", "Engineering", null}
     * // and {4, "Dave", null, 60000}
     * // One Bob row remains because dataset1 has two occurrences and dataset2 has one
     * }</pre>
     *
     * @param other The Dataset to find symmetric difference with this Dataset. Must not be {@code null}.
     * @param keyColumnNames The column names to use for matching rows between Datasets. Must not be {@code null} or empty.
     * @param requiresSameColumns A boolean that indicates whether both Datasets should have the same columns.
     * @return A new Dataset containing rows that are present in either this Dataset or the specified Dataset, but not in both, based on the specified key columns and considering the number of occurrences.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}, or if {@code keyColumnNames} is {@code null} or empty, or if any specified key column doesn't exist in both Datasets, or if {@code requiresSameColumns} is {@code true} and the two Datasets don't have the same columns.
     * @see #symmetricDifference(Dataset)
     * @see #symmetricDifference(Dataset, boolean)
     * @see #symmetricDifference(Dataset, Collection)
     * @see #difference(Dataset, Collection, boolean)
     * @see #intersection(Dataset, Collection, boolean)
     */
    Dataset symmetricDifference(Dataset other, Collection<String> keyColumnNames, boolean requiresSameColumns) throws IllegalArgumentException;

    /**
     * Performs a cartesian product operation with this Dataset and another Dataset.
     * <br />
     * The cartesian product operation combines each row from this Dataset with each row from the other Dataset.
     * The resulting Dataset will have the combined columns of both Datasets.
     * If there are columns with the same name in both Datasets, the columns from the other Dataset will be suffixed with a unique identifier to avoid conflicts.
     * The total number of rows in the resulting Dataset will be the product of the number of rows in both Datasets.
     * <br />
     * For example, if this Dataset has 3 rows and the other Dataset has 2 rows, the resulting Dataset will have 6 rows (3 × 2).
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name"),
     *     new Object[][] {
     *          {1, "Alice"},
     *          {2, "Bob"}
     *     });
     * Dataset dataset2 = Dataset.rows(Arrays.asList("category", "score"),
     *     new Object[][] {
     *          {"A", 95},
     *          {"B", 90}
     *     });
     *
     * Dataset result = dataset1.cartesianProduct(dataset2);
     * // Result contains columns: id, name, category, score
     * // Result contains: {1, "Alice", "A", 95}, {1, "Alice", "B", 90},
     * //                  {2, "Bob", "A", 95}, {2, "Bob", "B", 90}
     * }</pre>
     *
     * @param other The Dataset to perform the cartesian product with. Must not be {@code null}.
     * @return A new Dataset that is the result of the cartesian product operation.
     * @throws IllegalArgumentException if the {@code other} Dataset is {@code null}.
     * @see #merge(Dataset)
     * @see #innerJoin(Dataset, String, String)
     * @see #leftJoin(Dataset, String, String)
     * @see #union(Dataset)
     */
    Dataset cartesianProduct(Dataset other) throws IllegalArgumentException;

    /**
     * Splits this Dataset into multiple Datasets, each containing a maximum of <i>chunkSize</i> rows.
     * <br />
     * The split operation divides the Dataset into smaller Datasets, each of which contains <i>chunkSize</i> rows, except possibly for the last one, which may be smaller.
     * The resulting Datasets will have the same columns as the original Dataset.
     * The rows in the resulting Datasets will be in the same order as in the original Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}, {3}});
     * Dataset[] parts = dataset.split(2);
     * }</pre>
     *
     * @param chunkSize The maximum number of rows each split Dataset should contain.
     * @return A Stream of Datasets, each containing <i>chunkSize</i> rows from the original Dataset, or an empty Stream if this Dataset is empty.
     * @throws IllegalArgumentException if <i>chunkSize</i> is less than or equal to 0.
     */
    Stream<Dataset> split(int chunkSize) throws IllegalArgumentException;

    /**
     * Splits this Dataset into multiple Datasets, each containing a maximum of <i>chunkSize</i> rows.
     * <br />
     * The split operation divides the Dataset into smaller Datasets, each of which contains <i>chunkSize</i> rows, except possibly for the last one, which may be smaller.
     * The resulting Datasets will have the same columns as specified in the <i>columnNames</i> collection.
     * The rows in the resulting Datasets will be in the same order as in the original Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}, {3}});
     * Dataset[] parts = dataset.split(2);
     * }</pre>
     *
     * @param chunkSize The maximum number of rows each split Dataset should contain.
     * @param columnNames The collection of column names to be included in the split Datasets.
     * @return A Stream of Datasets, each containing <i>chunkSize</i> rows from the original Dataset, or an empty Stream if this Dataset is empty.
     * @throws IllegalArgumentException if <i>chunkSize</i> is less than or equal to 0.
     */
    Stream<Dataset> split(int chunkSize, Collection<String> columnNames) throws IllegalArgumentException;

    /**
     * Splits this Dataset into multiple Datasets, each containing a maximum of <i>chunkSize</i> rows.
     * <br />
     * The split operation divides the Dataset into smaller Datasets, each of which contains <i>chunkSize</i> rows, except possibly for the last one, which may be smaller.
     * The resulting Datasets will have the same columns as the original Dataset.
     * The rows in the resulting Datasets will be in the same order as in the original Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}, {3}});
     * List<Dataset> parts = dataset.splitToList(2);
     * }</pre>
     *
     * @param chunkSize The maximum number of rows each split Dataset should contain.
     * @return A List of Datasets, each containing <i>chunkSize</i> rows from the original Dataset, or an empty List if this Dataset is empty.
     * @throws IllegalArgumentException if <i>chunkSize</i> is less than or equal to 0.
     */
    List<Dataset> splitToList(int chunkSize) throws IllegalArgumentException;

    /**
     * Splits this Dataset into multiple Datasets, each containing a maximum of <i>chunkSize</i> rows.
     * <br />
     * The split operation divides the Dataset into smaller Datasets, each of which contains <i>chunkSize</i> rows, except possibly for the last one, which may be smaller.
     * The resulting Datasets will have the same columns as specified in the <i>columnNames</i> collection.
     * The rows in the resulting Datasets will be in the same order as in the original Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}, {3}});
     * List<Dataset> parts = dataset.splitToList(2);
     * }</pre>
     *
     * @param chunkSize The maximum number of rows each split Dataset should contain.
     * @param columnNames The collection of column names to be included in the split Datasets.
     * @return A List of Datasets, each containing <i>chunkSize</i> rows from the original Dataset, or an empty List if this Dataset is empty.
     * @throws IllegalArgumentException if <i>chunkSize</i> is less than or equal to 0.
     */
    List<Dataset> splitToList(int chunkSize, Collection<String> columnNames) throws IllegalArgumentException;

    /**
     * Returns an immutable slice of this Dataset from the specified <i>fromRowIndex</i> to <i>toRowIndex</i>.
     * <br />
     * The slice operation creates a new Dataset that includes rows from the original Dataset starting from <i>fromRowIndex</i> and ending at <i>toRowIndex</i>.
     * The resulting Dataset will have the same columns as the original Dataset.
     * The rows in the resulting Dataset will be in the same order as in the original Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * Dataset slice = dataset.slice(0, 1);
     * }</pre>
     *
     * @param fromRowIndex The starting index of the slice, inclusive.
     * @param toRowIndex The ending index of the slice, exclusive.
     * @return A new Dataset containing the rows from <i>fromRowIndex</i> to <i>toRowIndex</i> from the original Dataset.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range.
     * @see List#subList(int, int).
     */
    Dataset slice(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Returns an immutable slice of this Dataset that includes only the columns specified in the <i>columnNames</i> collection from the original Dataset.
     * <br />
     * The resulting Dataset will have the same rows as the original Dataset, but only the columns specified in the <i>columnNames</i> collection.
     * The rows in the resulting Dataset will be in the same order as in the original Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * Dataset slice = dataset.slice(0, 1);
     * }</pre>
     *
     * @param columnNames The collection of column names to be included in the sliced Dataset.
     * @return A new Dataset containing the same rows as the original Dataset, but only the columns specified in the <i>columnNames</i> collection.
     * @throws IllegalArgumentException if the <i>columnNames</i> collection is {@code null} or if any of the column names in the collection do not exist in the original Dataset.
     * @see List#subList(int, int).
     */
    Dataset slice(Collection<String> columnNames) throws IllegalArgumentException;

    /**
     * Returns an immutable slice of this Dataset from the specified <i>fromRowIndex</i> to <i>toRowIndex</i> and only includes the columns specified in the <i>columnNames</i> collection.
     * <br />
     * The resulting Dataset will have the same rows as the original Dataset, but only the columns specified in the <i>columnNames</i> collection.
     * The rows in the resulting Dataset will be in the same order as in the original Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}, {2, "Bob"}});
     * Dataset slice = dataset.slice(0, 1);
     * }</pre>
     *
     * @param fromRowIndex The starting index of the slice, inclusive.
     * @param toRowIndex The ending index of the slice, exclusive.
     * @param columnNames The collection of column names to be included in the sliced Dataset.
     * @return A new Dataset containing the rows from <i>fromRowIndex</i> to <i>toRowIndex</i> from the original Dataset, but only the columns specified in the <i>columnNames</i> collection.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range.
     * @throws IllegalArgumentException if the <i>columnNames</i> collection is {@code null} or if any of the column names in the collection do not exist in the original Dataset.
     */
    Dataset slice(int fromRowIndex, int toRowIndex, Collection<String> columnNames) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a new Dataset that is a copy of the current Dataset.
     * <br />
     * The rows and columns in the resulting Dataset will be in the same order as in the original Dataset.
     * The frozen status of the copy will always be {@code false}, even the original {@code Dataset} is frozen.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset original = Dataset.rows(Arrays.asList("name", "age"), data);
     * Dataset backup = original.copy(); // creates an independent copy
     * }</pre>
     *
     * @return A new Dataset that is a copy of the current Dataset.
     */
    Dataset copy();

    /**
     * Creates a new Dataset that is a copy of the current Dataset from the specified <i>fromRowIndex</i> to <i>toRowIndex</i>.
     * <br />
     * The rows and columns in the resulting Dataset will be in the same order as in the original Dataset.
     * The frozen status of the copy will always be {@code false}, even the original {@code Dataset} is frozen.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}, {3}});
     * Dataset slice = dataset.copy(0, 2);
     * }</pre>
     *
     * @param fromRowIndex The starting index of the copy, inclusive.
     * @param toRowIndex The ending index of the copy, exclusive.
     * @return A new Dataset that is a copy of the current Dataset from <i>fromRowIndex</i> to <i>toRowIndex</i>.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range.
     */
    Dataset copy(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Creates a new Dataset that is a copy of the current Dataset with only the columns specified in the <i>columnNames</i> collection.
     * <br />
     * The rows in the resulting Dataset will be in the same order as in the original Dataset.
     * The frozen status of the copy will always be {@code false}, even the original {@code Dataset} is frozen.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}, {3}});
     * Dataset slice = dataset.copy(0, 2);
     * }</pre>
     *
     * @param columnNames The collection of column names to be included in the copy.
     * @return A new Dataset that is a copy of the current Dataset with only the columns specified in the <i>columnNames</i> collection.
     * @throws IllegalArgumentException if the <i>columnNames</i> collection is {@code null} or if any of the column names in the collection do not exist in the original Dataset.
     */
    Dataset copy(Collection<String> columnNames);

    /**
     * Creates a new Dataset that is a copy of the current Dataset from the specified <i>fromRowIndex</i> to <i>toRowIndex</i> with only the columns specified in the <i>columnNames</i> collection.
     * <br />
     * The rows in the resulting Dataset will be in the same order as in the original Dataset.
     * The frozen status of the copy will always be {@code false}, even the original {@code Dataset} is frozen.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}, {3}});
     * Dataset slice = dataset.copy(0, 2);
     * }</pre>
     *
     * @param fromRowIndex The starting index of the copy, inclusive.
     * @param toRowIndex The ending index of the copy, exclusive.
     * @param columnNames The collection of column names to be included in the copy.
     * @return A new Dataset that is a copy of the current Dataset from <i>fromRowIndex</i> to <i>toRowIndex</i> with only the columns specified in the <i>columnNames</i> collection.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range.
     * @throws IllegalArgumentException if the <i>columnNames</i> collection is {@code null} or if any of the column names in the collection do not exist in the original Dataset.
     */
    Dataset copy(int fromRowIndex, int toRowIndex, Collection<String> columnNames);

    /**
     * Creates a deep copy of the current Dataset by performing Serialization/Deserialization.
     * <br />
     * This method ensures that the returned Dataset is a completely separate copy of the original Dataset, with no shared references.
     * The frozen status of the copy will always be same as the frozen status in the original Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}});
     * Dataset copy = dataset.clone();
     * }</pre>
     *
     * @return A new Dataset that is a deep copy of the current Dataset.
     */
    @Beta
    Dataset clone(); //NOSONAR

    /**
     * Creates a deep copy of the current Dataset by performing Serialization/Deserialization.
     * <br />
     * This method ensures that the returned Dataset is a completely separate copy of the original Dataset, with no shared references.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}});
     * Dataset copy = dataset.clone();
     * }</pre>
     *
     * @param freeze A boolean value that indicates whether the returned Dataset should be frozen.
     * @return A new Dataset that is a deep copy of the current Dataset.
     */
    @Beta
    Dataset clone(boolean freeze);

    /**
     * Creates a BiIterator over the elements in the specified columns of the Dataset.
     * <br />
     * The BiIterator will iterate over pairs of elements, where each pair consists of an element from columnNameA and an element from columnNameB.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Iterator<ImmutableList<Object>> rows = dataset.iterator();
     * }</pre>
     *
     * @param <A> The type of the elements in the column specified by columnNameA.
     * @param <B> The type of the elements in the column specified by columnNameB.
     * @param columnNameA The name of the first column to iterate over.
     * @param columnNameB The name of the second column to iterate over.
     * @return A BiIterator over pairs of elements from the specified columns.
     * @throws IllegalArgumentException if the specified any of column names are not found in the Dataset.
     */
    <A, B> BiIterator<A, B> iterator(String columnNameA, String columnNameB) throws IllegalArgumentException;

    /**
     * Creates a BiIterator over the elements in the specified columns of the Dataset.
     * <br />
     * The BiIterator will iterate over pairs of elements, where each pair consists of an element from columnNameA and an element from columnNameB.
     * The iteration starts from the row specified by fromRowIndex and ends at the row specified by toRowIndex.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Iterator<ImmutableList<Object>> rows = dataset.iterator();
     * }</pre>
     *
     * @param <A> The type of the elements in the column specified by columnNameA.
     * @param <B> The type of the elements in the column specified by columnNameB.
     * @param fromRowIndex The starting row index for the iteration.
     * @param toRowIndex The ending row index for the iteration.
     * @param columnNameA The name of the first column to iterate over.
     * @param columnNameB The name of the second column to iterate over.
     * @return A BiIterator over pairs of elements from the specified columns.
     * @throws IndexOutOfBoundsException if either fromRowIndex or toRowIndex is out of the Dataset's row bounds.
     * @throws IllegalArgumentException if the specified any of column names are not found in the Dataset.
     */
    <A, B> BiIterator<A, B> iterator(int fromRowIndex, int toRowIndex, String columnNameA, String columnNameB)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a TriIterator over the elements in the specified columns of the Dataset.
     * <br />
     * The TriIterator will iterate over triplets of elements, where each triplet consists of an element from columnNameA, an element from columnNameB, and an element from columnNameC.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Iterator<ImmutableList<Object>> rows = dataset.iterator();
     * }</pre>
     *
     * @param <A> The type of the elements in the column specified by columnNameA.
     * @param <B> The type of the elements in the column specified by columnNameB.
     * @param <C> The type of the elements in the column specified by columnNameC.
     * @param columnNameA The name of the first column to iterate over.
     * @param columnNameB The name of the second column to iterate over.
     * @param columnNameC The name of the third column to iterate over.
     * @return A TriIterator over triplets of elements from the specified columns.
     * @throws IllegalArgumentException if the specified any of column names are not found in the Dataset.
     */
    <A, B, C> TriIterator<A, B, C> iterator(String columnNameA, String columnNameB, String columnNameC) throws IllegalArgumentException;

    /**
     * Creates a TriIterator over the elements in the specified columns of the Dataset.
     * <br />
     * The TriIterator will iterate over triplets of elements, where each triplet consists of an element from columnNameA, an element from columnNameB, and an element from columnNameC.
     * The iteration starts from the row specified by fromRowIndex and ends at the row specified by toRowIndex.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Iterator<ImmutableList<Object>> rows = dataset.iterator();
     * }</pre>
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
     * @throws IndexOutOfBoundsException if either fromRowIndex or toRowIndex is out of the Dataset's row bounds.
     * @throws IllegalArgumentException if the specified any of column names are not found in the Dataset.s
     */
    <A, B, C> TriIterator<A, B, C> iterator(int fromRowIndex, int toRowIndex, String columnNameA, String columnNameB, String columnNameC)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Paginated<Dataset> Dataset from the current Dataset.
     * <br />
     * The Paginated<Dataset> object will contain pages of Datasets, where each page has a maximum size specified by the pageSize parameter.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}, {3}});
     * Paginated<Dataset> pages = dataset.paginate(2);
     * }</pre>
     *
     * @param pageSize The maximum number of rows each page can contain.
     * @return A Paginated<Dataset> object containing pages of Datasets.
     * @throws IllegalArgumentException if pageSize is less than or equal to 0.
     */
    Paginated<Dataset> paginate(int pageSize);

    /**
     * Creates a Paginated<Dataset> object from the current Dataset.
     * <br />
     * The Paginated<Dataset> object will contain pages of Datasets, where each page has a maximum size specified by the pageSize parameter.
     * Only the columns specified by the columnNames collection will be included in the paginated Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}, {2}, {3}});
     * Paginated<Dataset> pages = dataset.paginate(2);
     * }</pre>
     *
     * @param columnNames The collection of column names to be included in the paginated Dataset.
     * @param pageSize The maximum number of rows each page can contain.
     * @return A Paginated<Dataset> object containing pages of Datasets.
     * @throws IllegalArgumentException if the specified column names are not found in the Dataset or {@code columnNames} is empty or pageSize is less than or equal to 0.
     */
    Paginated<Dataset> paginate(Collection<String> columnNames, int pageSize);

    /**
     * Returns a Stream with values from the specified column.
     * <br />
     * The values are read from the Dataset in the order they appear.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of the specified column.
     * @param columnName The name of the column in the Dataset to create the Stream from.
     * @return A Stream containing all values from the specified column in the Dataset.
     * @throws IllegalArgumentException if the specified column name is not found in the Dataset.
     */
    <T> Stream<T> stream(String columnName) throws IllegalArgumentException;

    /**
     * Returns a Stream with values from the specified column.
     * <br />
     * The Stream will contain all values from the specified column, starting from the row index specified by <i>fromRowIndex</i> and ending at the row index specified by <i>toRowIndex</i>.
     * The values are read from the Dataset in the order they appear.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of the elements in the Stream.
     * @param fromRowIndex The starting row index for the Stream.
     * @param toRowIndex The ending row index for the Stream.
     * @param columnName The name of the column in the Dataset to create the Stream from.
     * @return A Stream containing all values from the specified column in the Dataset.
     * @throws IndexOutOfBoundsException if the specified row indexes are out of the Dataset's range.
     * @throws IllegalArgumentException if the specified column name is not found in the Dataset.
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
     * Creates a Stream of objects of type {@code T} converted from rows in the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset employees = Dataset.rows(Arrays.asList("name", "age", "salary"), data);
     * Stream<Employee> empStream = employees.stream(Employee.class);
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of objects of type T, created from rows in the Dataset.
     * @throws IllegalArgumentException if the specified {@code beanClass} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * The subset of rows is determined by the provided fromRowIndex and toRowIndex.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row subset.
     * @param toRowIndex The ending index of the row subset.
     * @param rowType The class of the objects in the resulting Stream.
     * @return A Stream of objects of type T, created from the subset of rows in the Dataset.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range.
     * @throws IllegalArgumentException if the specified {@code beanClass} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * Only the columns specified in the {@code columnNames} collection will be included to {@code rowType}.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param columnNames The collection of column names to be included to the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of objects of type T, created from rows in the Dataset.
     * @throws IllegalArgumentException if the specified {@code beanClass} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(Collection<String> columnNames, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * Only the columns specified in the {@code columnNames} collection will be included to {@code rowType}.
     * The subset of rows is determined by the provided fromRowIndex and toRowIndex.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row subset.
     * @param toRowIndex The ending index of the row subset.
     * @param columnNames The collection of column names to be included to the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Object[], Collection, Map, or Bean class.
     * @return A Stream of objects of type T, created from the subset of rows in the Dataset.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range.
     * @throws IllegalArgumentException if the specified {@code rowType} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Class<? extends T> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowSupplier function.
     * The rowSupplier function is responsible for creating new instances of {@code T} for each row in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param rowSupplier A function that creates a new instance of {@code T} for each row in the Dataset.
     * @return A Stream of objects of type T, created from the Dataset.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowSupplier function.
     * The rowSupplier function is responsible for creating new instances of {@code T} for each row in the Dataset.
     * The subset of rows is determined by the provided fromRowIndex and toRowIndex.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row subset.
     * @param toRowIndex The ending index of the row subset.
     * @param rowSupplier A function that creates a new instance of {@code T} for each row in the Dataset.
     * @return A Stream of objects of type T, created from the subset of rows in the Dataset.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range.
     * @throws IllegalArgumentException if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowSupplier function.
     * Only the columns specified in the {@code columnNames} collection will be included to the instances created by rowSupplier.
     * The rowSupplier function is responsible for creating new instances of {@code T} for each row in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param columnNames The collection of column names to be included to the instances created by rowSupplier.
     * @param rowSupplier A function that creates a new instance of {@code T} for each row in the Dataset.
     * @return A Stream of objects of type T, created from the Dataset.
     * @throws IllegalArgumentException if the columnNames are not found in the Dataset or {@code columnNames} is empty, or if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowSupplier function.
     * The rowSupplier function is responsible for creating new instances of {@code T} for each row in the Dataset.
     * Only the columns specified in the {@code columnNames} collection will be included to the instances created by rowSupplier.
     * The subset of rows is determined by the provided fromRowIndex and toRowIndex.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row subset.
     * @param toRowIndex The ending index of the row subset.
     * @param columnNames The collection of column names to be included to the instances created by rowSupplier.
     * @param rowSupplier A function that creates a new instance of {@code T} for each row in the Dataset.
     * @return A Stream of objects of type T, created from the subset of rows in the Dataset.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range.
     * @throws IllegalArgumentException if the columnNames are not found in the Dataset or {@code columnNames} is empty, or if the return value created by specified {@code rowSupplier} is not a supported type - Object[], Collection, Map, or Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, IntFunction<? extends T> rowSupplier)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * The mapping between the Dataset's columns and the fields of the {@code rowType} is determined by the provided prefixAndFieldNameMap.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param prefixAndFieldNameMap The map of prefixes and field names to be used for mapping Dataset's columns to the fields of the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Bean class.
     * @return A Stream of objects of type T, created from the Dataset.
     * @throws IllegalArgumentException if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code rowType} is not a supported type - Bean class.
     */
    <T> Stream<T> stream(Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} from a subset of rows in the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * The mapping between the Dataset's columns and the fields of the {@code rowType} is determined by the provided prefixAndFieldNameMap.
     * The subset of rows is determined by the provided fromRowIndex and toRowIndex.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row subset.
     * @param toRowIndex The ending index of the row subset.
     * @param prefixAndFieldNameMap The map of prefixes and field names to be used for mapping Dataset's columns to the fields of the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Bean class.
     * @return A Stream of objects of type T, created from the subset of rows in the Dataset.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range.
     * @throws IllegalArgumentException if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code rowType} is not a supported type - Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} converted from rows in the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * The mapping between the Dataset's columns and the fields of the {@code rowType} is determined by the provided prefixAndFieldNameMap.
     * Only the columns specified in the {@code columnNames} collection will be included in the {@code rowType}.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param columnNames The collection of column names to be included in the {@code rowType}.
     * @param prefixAndFieldNameMap The map of prefixes and field names to be used for mapping Dataset's columns to the fields of the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream. It must be one of the supported types - Bean class.
     * @return A Stream of objects of type T
     * @throws IllegalArgumentException if the columnNames are not found in the Dataset or {@code columnNames} is empty, or if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> Stream<T> stream(Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} converted from rows in the Dataset.
     * <br />
     * The type of objects in the resulting Stream is determined by the provided rowType.
     * The mapping between the Dataset's columns and the fields of the {@code rowType} is determined by the provided prefixAndFieldNameMap.
     * Only the columns specified in the {@code columnNames} collection will be included in the {@code rowType}.
     * The Stream is created for rows in the range from fromRowIndex (inclusive) to toRowIndex (exclusive).
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row, inclusive.
     * @param toRowIndex The ending index of the row, exclusive.
     * @param columnNames The collection of column names to be included in the {@code rowType}.
     * @param prefixAndFieldNameMap The map of prefixes and field names to be used for mapping Dataset's columns to the fields of the {@code rowType}.
     * @param rowType The class of the objects in the resulting Stream.
     * @return A Stream of objects of type T
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range
     * @throws IllegalArgumentException if the columnNames are not found in the Dataset or {@code columnNames} is empty, or if the mapping defined by {@code prefixAndFieldNameMap} is invalid, or if the specified {@code beanClass} is not a supported type - Bean class.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} by applying the provided rowMapper function to each row in the Dataset.
     * <br />
     * The rowMapper function takes two arguments: the index of the row and a DisposableObjArray representing the row itself.
     * The DisposableObjArray contains the values of the row.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param rowMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the Dataset, and the DisposableObjArray represents the row itself.
     * @return A Stream of objects of type T, created by applying the rowMapper function to each row in the Dataset.
     */
    <T> Stream<T> stream(IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper);

    /**
     * Creates a Stream of objects of type {@code T} by applying the provided rowMapper function to each row in the Dataset.
     * <br />
     * The rowMapper function takes two arguments: the index of the row and a DisposableObjArray representing the row itself.
     * The DisposableObjArray contains the values of the row.
     * The Stream is created for rows in the range from fromRowIndex (inclusive) to toRowIndex (exclusive).
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row, inclusive.
     * @param toRowIndex The ending index of the row, exclusive.
     * @param rowMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the Dataset, and the DisposableObjArray represents the row itself.
     * @return A Stream of objects of type T, created by applying the rowMapper function to each row in the Dataset.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range
     * @throws IllegalArgumentException if the supplied arguments reference non-existent columns or otherwise violate dataset constraints
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} by applying the provided rowMapper function to each row in the Dataset.
     * <br />
     * The rowMapper function takes two arguments: the index of the row and a DisposableObjArray representing the row itself.
     * The DisposableObjArray contains the values of the specified columns for that row.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param columnNames A collection of column names to be included in the DisposableObjArray passed to the rowMapper function.
     * @param rowMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the Dataset, and the DisposableObjArray represents the row itself.
     * @return A Stream of objects of type T, created by applying the rowMapper function to each row in the Dataset.
     * @throws IllegalArgumentException if the columnNames are not found in the Dataset or {@code columnNames} is empty
     */
    <T> Stream<T> stream(Collection<String> columnNames, IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) throws IllegalArgumentException;

    /**
     * Creates a Stream of objects of type {@code T} by applying the provided rowMapper function to each row in the Dataset.
     * <br />
     * The rowMapper function takes two arguments: the index of the row and a DisposableObjArray representing the row itself.
     * The DisposableObjArray contains the values of the specified columns for that row.
     * The Stream is created for rows in the range from fromRowIndex (inclusive) to toRowIndex (exclusive).
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of objects in the resulting Stream.
     * @param fromRowIndex The starting index of the row, inclusive.
     * @param toRowIndex The ending index of the row, exclusive.
     * @param columnNames A collection of column names to be included in the DisposableObjArray passed to the rowMapper function.
     * @param rowMapper A function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the Dataset, and the DisposableObjArray represents the row itself.
     * @return A Stream of objects of type T, created by applying the rowMapper function to each row in the Dataset.
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range
     * @throws IllegalArgumentException if the columnNames are not found in the Dataset or {@code columnNames} is empty
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of type T from the Dataset using the specified column names and a row mapper function.
     * <br />
     * The row mapper function is used to transform the values of the two columns into an instance of type T.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> the type of the elements in the resulting Stream
     * @param columnNames a Tuple2 containing the names of the two columns to be used
     * @param rowMapper a BiFunction to transform the values of the two columns into an instance of type T
     * @return a Stream of type T
     * @throws IllegalArgumentException if the columnNames are not found in the Dataset
     */
    <T> Stream<T> stream(Tuple2<String, String> columnNames, BiFunction<?, ?, ? extends T> rowMapper) throws IllegalArgumentException;

    /**
     * Creates a Stream of type T from the Dataset using the specified column names and a row mapper function.
     * <br />
     * The Stream will contain all values from the specified columns, starting from the row index specified by <i>fromRowIndex</i> and ending at the row index specified by <i>toRowIndex</i>.
     * The row mapper function is used to transform the values from the specified columns into an object of type T.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> the type of the elements in the resulting Stream
     * @param fromRowIndex the starting index of the row, inclusive
     * @param toRowIndex the ending index of the row, exclusive
     * @param columnNames a Tuple2 containing the names of the two columns to be used
     * @param rowMapper a BiFunction to transform the values of the two columns into an instance of type T
     * @return a Stream of type T
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range
     * @throws IllegalArgumentException if the columnNames are not found in the Dataset
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, BiFunction<?, ?, ? extends T> rowMapper)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Creates a Stream of type T from the Dataset using the specified column names and a row mapper function.
     * <br />
     * The Stream will contain all values from the specified columns.
     * The row mapper function is used to transform the values from the specified columns into an object of type T.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of the elements in the Stream.
     * @param columnNames The names of the columns in the Dataset to create the Stream from.
     * @param rowMapper The function to transform the values from the specified columns into an object of type T.
     * @return A Stream containing all values from the specified columns in the Dataset, transformed by the row mapper function.
     * @throws IllegalArgumentException if any of the specified column names are not found in the Dataset.
     */
    <T> Stream<T> stream(Tuple3<String, String, String> columnNames, TriFunction<?, ?, ?, ? extends T> rowMapper) throws IllegalArgumentException;

    /**
     * Creates a Stream of type T from the Dataset using the specified column names and a row mapper function.
     * <br />
     * The Stream will contain all values from the specified columns, starting from the row index specified by <i>fromRowIndex</i> and ending at the row index specified by <i>toRowIndex</i>.
     * The row mapper function is used to transform the values from the specified columns into an object of type T.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.stream("name");
     * }</pre>
     *
     * @param <T> The type of the elements in the Stream.
     * @param fromRowIndex The starting row index for the Stream.
     * @param toRowIndex The ending row index for the Stream.
     * @param columnNames The names of the columns in the Dataset to create the Stream from.
     * @param rowMapper The function to transform the values from the specified columns into an object of type T.
     * @return A Stream containing all values from the specified columns in the Dataset, transformed by the row mapper function.
     * @throws IndexOutOfBoundsException if the specified row indexes are out of the Dataset's range.
     * @throws IllegalArgumentException if any of the specified column names are not found in the Dataset.
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, TriFunction<?, ?, ?, ? extends T> rowMapper)
            throws IndexOutOfBoundsException, IllegalArgumentException;

    /**
     * Applies the provided function to this Dataset and returns the result.
     * <br />
     * This method is useful for performing complex operations on the Dataset that are not covered by the existing methods.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}});
     * int count = dataset.apply(Dataset::columnCount);
     * }</pre>
     *
     * @param <R> The type of the result.
     * @param <E> The type of the exception that the function may throw.
     * @param func The function to apply to the Dataset. This function should take a Dataset as input and return a result of type R.
     * @return The result of applying the provided function to the Dataset.
     * @throws E if the provided function throws an exception.
     */
    <R, E extends Exception> R apply(Throwables.Function<? super Dataset, ? extends R, E> func) throws E;

    /**
     * Applies the provided function to this Dataset if it is not empty and returns the result wrapped in an Optional.
     * <br />
     * This method is useful for performing complex operations on the Dataset that are not covered by the existing methods.
     * If the Dataset is empty, it returns an empty Optional.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}});
     * Optional<Integer> maybeCount = dataset.applyIfNotEmpty(Dataset::size);
     * }</pre>
     *
     * @param <R> The type of the result.
     * @param <E> The type of the exception that the function may throw.
     * @param func The function to apply to the Dataset. This function should take a Dataset as input and return a result of type R.
     * @return An Optional containing the result of applying the provided function to the Dataset, or an empty Optional if the Dataset is empty.
     * @throws E if the provided function throws an exception.
     */
    <R, E extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super Dataset, ? extends R, E> func) throws E;

    /**
     * Performs the provided action on this Dataset.
     * <br />
     * This method is useful for performing operations on the Dataset that do not return a result.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}});
     * dataset.accept(ds -> System.out.println(ds.size()));
     * }</pre>
     *
     * @param <E> The type of the exception that the action may throw.
     * @param action The action to be performed on the Dataset. This action should take a Dataset as input.
     * @throws E if the provided action throws an exception.
     */
    <E extends Exception> void accept(Throwables.Consumer<? super Dataset, E> action) throws E;

    /**
     * Performs the provided action on this Dataset if it is not empty.
     * <br />
     * This method is useful for performing operations on the Dataset that do not return a result.
     * If the Dataset is empty, it does nothing and returns an instance of OrElse.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}});
     * dataset.acceptIfNotEmpty(Dataset::freeze).orElse(() -> System.out.println("empty"));
     * }</pre>
     *
     * @param <E> The type of the exception that the action may throw.
     * @param action The action to be performed on the Dataset. This action should take a Dataset as input.
     * @return An instance of OrElse, which can be used to perform an alternative action if the Dataset is empty.
     * @throws E if the provided action throws an exception.
     */
    <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super Dataset, E> action) throws E;

    /**
     * Freezes the Dataset to prevent further modification.
     * <br />
     * This method is useful when you want to ensure the Dataset remains constant after a certain point in your program.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset config = Dataset.rows(Arrays.asList("key", "value"), data);
     * config.freeze(); // prevents further modifications
     * }</pre>
     */
    void freeze();

    /**
     * Checks if the Dataset is frozen.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}});
     * boolean locked = dataset.isFrozen();
     * }</pre>
     *
     * @return {@code true} if the Dataset is frozen and cannot be modified, {@code false} otherwise.
     */
    boolean isFrozen();

    /**
     * Clears the Dataset.
     * <br />
     * This method removes all data from the Dataset, leaving it empty.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Dataset tempData = Dataset.rows(Arrays.asList("id", "name"), data);
     * tempData.clear(); // removes all rows
     * }</pre>
     * @throws IllegalStateException if the Dataset is frozen and cannot be modified.
     */
    void clear() throws IllegalStateException;

    /**
     * Checks if the Dataset is empty.
     * 
     * <p>This method is part of the {@link Dataset} API; the default {@link RowDataset} implementation enforces these semantics.</p>
     * <p>Example:</p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * if (dataset.isEmpty()) { 
     *    // handle empty Dataset
     * }</pre>
     *
     * @return {@code true} if the Dataset is empty, {@code false} otherwise.
     */
    boolean isEmpty();

    /**
     * Trims the size of the Dataset to its current size.
     * <br />
     * A frozen Dataset can be trimmed too because trimming doesn't change elements held in the Dataset.
     * This method can be used to minimize the memory footprint of the Dataset by releasing any unused
     * capacity in the underlying data structures.
     * <br />
     * This operation does not modify the content or structure of the Dataset, only optimizes its
     * internal storage allocation. It is safe to call on both mutable and frozen Datasets.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "age"), data);
     * dataset.removeRows(100, 200); // Remove some rows
     * dataset.trimToSize(); // Reclaim unused memory
     *
     * // Can also be called on frozen datasets
     * dataset.freeze();
     * dataset.trimToSize(); // Still valid - doesn't modify content
     * }</pre>
     *
     * @see #freeze()
     * @see #isFrozen()
     */
    void trimToSize();

    /**
     * Returns the number of rows in the Dataset.
     * 
     * <p>This method is part of the {@link Dataset} API; the default {@link RowDataset} implementation enforces these semantics.</p>
     * <p>Example:</p>
     * <pre>{@code
     * Dataset employees = Dataset.rows(Arrays.asList("name", "age"), data);
     * int totalEmployees = employees.size(); // returns row count
     * }</pre>
     *
     * @return The number of rows in the Dataset.
     */
    int size();

    /**
     * Retrieves the properties of the Dataset as a Map.
     * <br />
     * The keys of the Map are the property names and the values are the property values.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}});
     * Map<String, Object> props = dataset.properties();
     * }</pre>
     *
     * @return A Map containing the properties of the Dataset.
     */
    @Beta
    Map<String, Object> properties();

    /**
     * Retrieves the names of the columns in the Dataset as a Stream.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * Stream<String> names = dataset.columnNames();
     * }</pre>
     *
     * @return A Stream containing the names of the columns in the Dataset.
     */
    @Beta
    Stream<String> columnNames();

    /**
     * Retrieves the data of the Dataset as a Stream of ImmutableList.
     * <br />
     * Each ImmutableList represents a column of data in the Dataset.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.columns(Arrays.asList("id", "name"), new Object[][] {{1, 2}, {"Alice", "Bob"}});
     * int count = dataset.columnCount();
     * }</pre>
     *
     * @return A Stream containing ImmutableList where each list represents a column of data in the Dataset.
     */
    Stream<ImmutableList<Object>> columns();

    /**
     * Retrieves the data of the Dataset as a Map.
     * <br />
     * Each entry in the Map represents a column in the Dataset, where the key is the column name and the value is an ImmutableList of objects in that column.
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id"), new Object[][] {{1}});
     * Map<String, ImmutableList<Object>> map = dataset.columnMap();
     * }</pre>
     *
     * @return A Map where each entry represents a column in the Dataset.
     */
    Map<String, ImmutableList<Object>> columnMap();

    // DatasetBuilder builder();

    /**
     * Prints the content of the Dataset to the standard output.
     * <br />
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     *
     * +----+---------+-----+---------+
     * | id | name    | age | salary  |
     * +----+---------+-----+---------+
     * | 1  | John    | 25  | 50000.0 |
     * | 2  | Jane    | 30  | 60000.0 |
     * | 3  | Bob     | 35  | 70000.0 |
     * | 4  | Alice   | 28  | 55000.0 |
     * | 5  | Charlie | 40  | 80000.0 |
     * +----+---------+-----+---------+
     * }</pre>
     * @see #println(String)
     */
    void println();

    /**
     * Prints the content of the Dataset to the standard output.
     * <br />
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * dataset.println("## "); // Print entire Dataset to console with prefix "## "
     * 
     * ## +----+---------+-----+---------+
     * ## | id | name    | age | salary  |
     * ## +----+---------+-----+---------+
     * ## | 1  | John    | 25  | 50000.0 |
     * ## | 2  | Jane    | 30  | 60000.0 |
     * ## | 3  | Bob     | 35  | 70000.0 |
     * ## | 4  | Alice   | 28  | 55000.0 |
     * ## | 5  | Charlie | 40  | 80000.0 |
     * ## +----+---------+-----+---------+
     * }</pre>
     * 
     * @param prefix the prefix string to be printed before each line of the Dataset output
     * @see #println()
     */
    void println(String prefix);

    /**
     * Prints a portion of the Dataset to the standard output.
     * <br />
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * dataset.println(0, 10); // Print first 10 rows to console
     * }</pre>
     *
     * @param fromRowIndex the starting index of the row, inclusive
     * @param toRowIndex the ending index of the row, exclusive
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range
     * @see #println()
     * @see #println(String)
     */
    void println(int fromRowIndex, int toRowIndex) throws IndexOutOfBoundsException;

    /**
     * Prints a portion of the Dataset to the standard output.
     * <br />
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * }</pre>
     *
     * @param fromRowIndex the starting index of the row, inclusive
     * @param toRowIndex the ending index of the row, exclusive
     * @param columnNames the collection of column names to be printed
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range
     * @see #println()
     * @see #println(String)
     */
    void println(int fromRowIndex, int toRowIndex, Collection<String> columnNames) throws IndexOutOfBoundsException;

    /**
     * Prints the Dataset to the provided Writer.
     * <br />
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), data);
     * StringWriter writer = new StringWriter();
     * dataset.println(writer); // Print Dataset to StringWriter
     * }</pre>
     *
     * @param output the appendable where the Dataset will be printed
     * @throws UncheckedIOException if an I/O error occurs
     * @see #println()
     * @see #println(String)
     */
    void println(Appendable output) throws UncheckedIOException;

    /**
     * Prints a portion of the Dataset to the provided Writer.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * dataset.println();
     * }</pre>
     *
     * @param fromRowIndex the starting index of the row, inclusive
     * @param toRowIndex the ending index of the row, exclusive
     * @param columnNames the collection of column names to be printed
     * @param output the appendable where the Dataset will be printed
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range
     * @throws UncheckedIOException if an I/O error occurs
     * @see #println()
     * @see #println(String)
     */
    void println(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Appendable output) throws IndexOutOfBoundsException, UncheckedIOException;

    /**
     * Prints a portion of the Dataset to the provided Writer.
     * <br />
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>{@code
     * Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] {{1, "Alice"}});
     * dataset.println();
     * }</pre>
     *
     * @param fromRowIndex the starting index of the row, inclusive
     * @param toRowIndex the ending index of the row, exclusive
     * @param columnNames the collection of column names to be printed
     * @param prefix the prefix string to be printed before each line of the Dataset output
     * @param output the appendable where the Dataset will be printed
     * @throws IndexOutOfBoundsException if the fromRowIndex or toRowIndex is out of the Dataset's range
     * @throws UncheckedIOException if an I/O error occurs
     * @see #println()
     * @see #println(String)
     */
    void println(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String prefix, Appendable output)
            throws IndexOutOfBoundsException, UncheckedIOException;
}
