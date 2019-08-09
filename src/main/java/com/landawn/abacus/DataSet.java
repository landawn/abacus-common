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

package com.landawn.abacus;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Properties;
import com.landawn.abacus.util.TriIterator;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.Function;
// import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.stream.Collector;
import com.landawn.abacus.util.stream.Stream;

// TODO: Auto-generated Javadoc
/**
 * The Interface DataSet.
 *
 * @author Haiyang Li
 * @see com.landawn.abacus.util.DataSetUtil
 * @see com.landawn.abacus.util.Build.DataSetBuilder
 * @see com.landawn.abacus.util.JdbcUtil
 * @see com.landawn.abacus.util.CSVUtil
 * @see com.landawn.abacus.util.function.IntFunction
 * @see com.landawn.abacus.util.Fn.Factory
 * @see com.landawn.abacus.util.Clazz
 * @see com.landawn.abacus.util.N#newEmptyDataSet()
 * @see com.landawn.abacus.util.N#newEmptyDataSet(Collection)
 * @see com.landawn.abacus.util.N#newDataSet(Map)
 * @see com.landawn.abacus.util.N#newDataSet(Collection)
 * @see com.landawn.abacus.util.N#newDataSet(Collection, Collection)
 * @see com.landawn.abacus.util.N#newDataSet(String, String, Map)
 * @since 0.8
 */
public interface DataSet {

    //    /**
    //     * Returns the entity name associated with the query.
    //     *
    //     * @return
    //     */
    //    String entityName();
    //
    //    /**
    //     * Returns the target entity class associated with the query.
    //     *
    //     * @return
    //     */
    //    <T> Class<T> entityClass();

    /**
     * Return the column name list in this DataSet.
     *
     * @return the immutable list
     */
    ImmutableList<String> columnNameList();

    /**
     * Method getColumnName.
     *
     * @param columnIndex the column index
     * @return the column name
     */
    String getColumnName(int columnIndex);

    /**
     * Method getColumnIndex.
     *
     * @param columnName the column name
     * @return -1 if the specified <code>columnName</code> is not found
     */
    int getColumnIndex(String columnName);

    /**
     * -1 is set to the element in the returned array if the mapping column name is not included in this <code>DataSet</code>.
     *
     * @param columnNames the column names
     * @return the column indexes
     */
    int[] getColumnIndexes(Collection<String> columnNames);

    /**
     * Contains column.
     *
     * @param columnName the column name
     * @return true, if successful
     */
    boolean containsColumn(String columnName);

    /**
     * Check if this <code>DataSet</code> contains all the specified columns.
     *
     * @param columnNames the column names
     * @return <code>true</code> if all the specified columns are included in the this <code>DataSet</code>
     */
    boolean containsAllColumns(Collection<String> columnNames);

    /**
     * Rename column.
     *
     * @param columnName the column name
     * @param newColumnName the new column name
     */
    void renameColumn(String columnName, String newColumnName);

    /**
     * Rename columns.
     *
     * @param oldNewNames the old new names
     */
    void renameColumns(Map<String, String> oldNewNames);

    /**
     * Rename column.
     *
     * @param <E> the element type
     * @param columnName the column name
     * @param func the func
     * @throws E the e
     */
    <E extends Exception> void renameColumn(String columnName, Try.Function<String, String, E> func) throws E;

    /**
     * Rename columns.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param func the func
     * @throws E the e
     */
    <E extends Exception> void renameColumns(Collection<String> columnNames, Try.Function<String, String, E> func) throws E;

    /**
     * Rename columns.
     *
     * @param <E> the element type
     * @param func the func
     * @throws E the e
     */
    <E extends Exception> void renameColumns(Try.Function<String, String, E> func) throws E;

    /**
     * Move column.
     *
     * @param columnName the column name
     * @param newPosition the new position
     */
    void moveColumn(String columnName, int newPosition);

    /**
     * Move columns.
     *
     * @param columnNameNewPositionMap the column name new position map
     */
    void moveColumns(Map<String, Integer> columnNameNewPositionMap);

    /**
     * Swap the positions of the two specified columns.
     *
     * @param columnNameA the column name A
     * @param columnNameB the column name B
     */
    void swapColumns(String columnNameA, String columnNameB);

    /**
     * Move the specified row to the new position.
     *
     * @param rowIndex the row index
     * @param newRowIndex the new row index
     */
    void moveRow(int rowIndex, int newRowIndex);

    /**
     * Swap the positions of the two specified rows.
     *
     * @param rowIndexA the row index A
     * @param rowIndexB the row index B
     */
    void swapRows(int rowIndexA, int rowIndexB);

    /**
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T> the generic type
     * @param rowIndex the row index
     * @param columnIndex the column index
     * @return the t
     */
    <T> T get(int rowIndex, int columnIndex);

    /**
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T> the generic type
     * @param targetType the target type
     * @param rowIndex the row index
     * @param columnIndex the column index
     * @return the t
     * @deprecated may be misused because it implies there is an underline auto-conversion from column values to target return type but actually there is not.
     */
    @Deprecated
    <T> T get(Class<T> targetType, int rowIndex, int columnIndex);

    /**
     * Sets the.
     *
     * @param rowIndex the row index
     * @param columnIndex the column index
     * @param element the element
     */
    void set(int rowIndex, int columnIndex, Object element);

    /**
     * Checks if is null.
     *
     * @param rowIndex the row index
     * @param columnIndex the column index
     * @return true, if is null
     */
    boolean isNull(int rowIndex, int columnIndex);

    /**
     *  
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T> the generic type
     * @param columnIndex the column index
     * @return the t
     */
    <T> T get(int columnIndex);

    /**
     *  
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T> the generic type
     * @param columnName the column name
     * @return the t
     */
    <T> T get(String columnName);

    /**
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T> the generic type
     * @param targetType the target type
     * @param columnIndex the column index
     * @return the t
     * @deprecated may be misused because it implies there is an underline auto-conversion from column values to target return type but actually there is not.
     */
    @Deprecated
    <T> T get(Class<T> targetType, int columnIndex);

    /**
     *  
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T> the generic type
     * @param targetType the target type
     * @param columnName the column name
     * @return the t
     * @deprecated may be misused because it implies there is an underline auto-conversion from column values to target return type but actually there is not.
     */
    @Deprecated
    <T> T get(Class<T> targetType, String columnName);

    /**
     * Returns the value from the current row and specified column if the specified {@code columnIndex} is equal or bigger than zero, 
     * or the specified {@code defaultValue} otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T> the generic type
     * @param columnIndex the column index
     * @param defaultValue the default value
     * @return the or default
     * @deprecated 
     */
    @Deprecated
    <T> T getOrDefault(int columnIndex, T defaultValue);

    /**
     * Returns the value from the current row and specified column if the specified {@code columnName} exists, 
     * or the specified {@code defaultValue} otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T> the generic type
     * @param columnName the column name
     * @param defaultValue the default value
     * @return the or default
     * @deprecated 
     */
    @Deprecated
    <T> T getOrDefault(String columnName, T defaultValue);

    /**
     * Return default value (false) if the property is null.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Boolean}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param columnIndex the column index
     * @return the boolean
     */
    boolean getBoolean(int columnIndex);

    /**
     * Return default value (false) if the property is null.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Boolean}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param columnName the column name
     * @return the boolean
     */
    boolean getBoolean(String columnName);

    /**
     * Return default value (0) if the property is null.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Character}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param columnIndex the column index
     * @return the char
     */
    char getChar(int columnIndex);

    /**
     * Return default value (0) if the property is null.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Character}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param columnName the column name
     * @return the char
     */
    char getChar(String columnName);

    /**
     * Return default value (0) if the property is null. Return Number.byteValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Byte}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex the column index
     * @return the byte
     */
    byte getByte(int columnIndex);

    /**
     * Return default value (0) if the property is null. Return Number.byteValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Byte}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName the column name
     * @return the byte
     */
    byte getByte(String columnName);

    /**
     * Return default value (0) if the property is null. Return Number.shortValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Short}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex the column index
     * @return the short
     */
    short getShort(int columnIndex);

    /**
     * Return default value (0) if the property is null. Return Number.shortValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Short}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName the column name
     * @return the short
     */
    short getShort(String columnName);

    /**
     * Return default value (0) if the property is null. Return Number.intValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Integer}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex the column index
     * @return the int
     */
    int getInt(int columnIndex);

    /**
     * Return default value (0) if the property is null. Return Number.intValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Integer}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName the column name
     * @return the int
     */
    int getInt(String columnName);

    /**
     * Return default value (0) if the property is null. Return Number.longValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Long}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex the column index
     * @return the long
     */
    long getLong(int columnIndex);

    /**
     * Return default value (0) if the property is null. Return Number.longValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Long}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName the column name
     * @return the long
     */
    long getLong(String columnName);

    /**
     * Return default value (0f) if the property is null. Return Number.floatValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Float}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex the column index
     * @return the float
     */
    float getFloat(int columnIndex);

    /**
     * Return default value (0f) if the property is null. Return Number.floatValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Float}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName the column name
     * @return the float
     */
    float getFloat(String columnName);

    /**
     * Return default value (0d) if the property is null. Return Number.doubleValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Double}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex the column index
     * @return the double
     */
    double getDouble(int columnIndex);

    /**
     * Return default value (0d) if the property is null. Return Number.doubleValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Double}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName the column name
     * @return the double
     */
    double getDouble(String columnName);

    /**
     * Checks if is null.
     *
     * @param columnIndex the column index
     * @return true, if is null
     */
    boolean isNull(int columnIndex);

    /**
     * Checks if is null.
     *
     * @param columnName the column name
     * @return true, if is null
     */
    boolean isNull(String columnName);

    /**
     * Method set.
     *
     * @param columnIndex the column index
     * @param value the value
     */
    void set(int columnIndex, Object value);

    /**
     * Method set.
     *
     * @param columnName the column name
     * @param value the value
     */
    void set(String columnName, Object value);

    /**
     * Must NOT modify the returned list.
     *
     * @param <T> the generic type
     * @param columnIndex the column index
     * @return the column
     */
    <T> ImmutableList<T> getColumn(int columnIndex);

    /**
     * Must NOT modify the returned list.
     *
     * @param <T> the generic type
     * @param columnName the column name
     * @return the column
     */
    <T> ImmutableList<T> getColumn(String columnName);

    /**
     * Copy of column.
     *
     * @param <T> the generic type
     * @param columnName the column name
     * @return the list
     */
    <T> List<T> copyOfColumn(String columnName);

    /**
     * Method addColumn.
     *
     * @param columnName the column name
     * @param column the column
     */
    void addColumn(String columnName, List<?> column);

    /**
     * Method addColumn.
     *
     * @param columnIndex position to add.
     * @param columnName the column name
     * @param column the column
     */
    void addColumn(int columnIndex, String columnName, List<?> column);

    /**
     * Generate the new column values from the specified column by the specified <code>Function</code>.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param newColumnName the new column name
     * @param fromColumnName the from column name
     * @param func the func
     * @throws E the e
     */
    <T, E extends Exception> void addColumn(String newColumnName, String fromColumnName, Try.Function<T, ?, E> func) throws E;

    /**
     * Generate the new column values from the specified column by the specified <code>Function</code>.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnIndex the column index
     * @param newColumnName the new column name
     * @param fromColumnName the from column name
     * @param func the func
     * @throws E the e
     */
    <T, E extends Exception> void addColumn(int columnIndex, String newColumnName, String fromColumnName, Try.Function<T, ?, E> func) throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E> the element type
     * @param newColumnName the new column name
     * @param fromColumnNames the from column names
     * @param func DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void addColumn(String newColumnName, Collection<String> fromColumnNames, Try.Function<? super DisposableObjArray, ?, E> func)
            throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E> the element type
     * @param columnIndex the column index
     * @param newColumnName the new column name
     * @param fromColumnNames the from column names
     * @param func DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void addColumn(int columnIndex, String newColumnName, Collection<String> fromColumnNames,
            Try.Function<? super DisposableObjArray, ?, E> func) throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E> the element type
     * @param newColumnName the new column name
     * @param fromColumnNames the from column names
     * @param func the func
     * @throws E the e
     */
    <E extends Exception> void addColumn(String newColumnName, Tuple2<String, String> fromColumnNames, Try.BiFunction<?, ?, ?, E> func) throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E> the element type
     * @param columnIndex the column index
     * @param newColumnName the new column name
     * @param fromColumnNames the from column names
     * @param func the func
     * @throws E the e
     */
    <E extends Exception> void addColumn(int columnIndex, String newColumnName, Tuple2<String, String> fromColumnNames, Try.BiFunction<?, ?, ?, E> func)
            throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E> the element type
     * @param newColumnName the new column name
     * @param fromColumnNames the from column names
     * @param func the func
     * @throws E the e
     */
    <E extends Exception> void addColumn(String newColumnName, Tuple3<String, String, String> fromColumnNames, Try.TriFunction<?, ?, ?, ?, E> func) throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E> the element type
     * @param columnIndex the column index
     * @param newColumnName the new column name
     * @param fromColumnNames the from column names
     * @param func the func
     * @throws E the e
     */
    <E extends Exception> void addColumn(int columnIndex, String newColumnName, Tuple3<String, String, String> fromColumnNames,
            Try.TriFunction<?, ?, ?, ?, E> func) throws E;

    /**
     * Remove the column with the specified columnName from this DataSet.
     *
     * @param <T> the generic type
     * @param columnName the column name
     * @return the list
     */
    <T> List<T> removeColumn(String columnName);

    /**
     * Remove the column(s) with the specified columnNames from this DataSet.
     *
     * @param columnNames the column names
     */
    void removeColumns(Collection<String> columnNames);

    /**
     * Remove the column(s) whose name matches the specified {@code filter}.
     *
     * @param <E> the element type
     * @param filter column name filter
     * @throws E the e
     */
    <E extends Exception> void removeColumns(Try.Predicate<String, E> filter) throws E;

    /**
     * Remove the column(s) whose name matches the specified {@code filter}.
     *
     * @param <E> the element type
     * @param filter column name filter
     * @throws E the e
     * @deprecated replaced by {@code removeColumns}.
     */
    @Deprecated
    <E extends Exception> void removeColumnsIf(Try.Predicate<String, E> filter) throws E;

    /**
     * Update the values of the specified column by the specified Try.Function.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param func the func
     * @throws E the e
     */
    <T, E extends Exception> void updateColumn(String columnName, Try.Function<T, ?, E> func) throws E;

    /**
     * Update the values of the specified columns one by one with the specified Try.Function.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnNames the column names
     * @param func the func
     * @throws E the e
     */
    <T, E extends Exception> void updateColumns(Collection<String> columnNames, Try.Function<?, ?, E> func) throws E;

    /**
     * Convert the specified column to target type.
     *
     * @param columnName the column name
     * @param targetType the target type
     */
    void convertColumn(String columnName, Class<?> targetType);

    /**
     * Convert the specified columns to target types.
     *
     * @param columnTargetTypes the column target types
     */
    void convertColumns(Map<String, Class<?>> columnTargetTypes);

    //
    //    /**
    //     * convert the specified columns to target types.
    //     *
    //     * @param targetColumnTypes fill the element with <code>null</code> if don't wan to convert the target column.
    //     */
    //    void convertColumn(Class<?>[] targetColumnTypes);
    //
    /**
     * Combine columns.
     *
     * @param columnNames the column names
     * @param newColumnName the new column name
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     */
    void combineColumns(Collection<String> columnNames, String newColumnName, Class<?> newColumnClass);

    /**
     * Combine columns.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param newColumnName the new column name
     * @param combineFunc DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void combineColumns(Collection<String> columnNames, String newColumnName, Try.Function<? super DisposableObjArray, ?, E> combineFunc)
            throws E;

    /**
     * Combine columns.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param newColumnName the new column name
     * @param combineFunc the combine func
     * @throws E the e
     */
    <E extends Exception> void combineColumns(Tuple2<String, String> columnNames, String newColumnName, Try.BiFunction<?, ?, ?, E> combineFunc) throws E;

    /**
     * Combine columns.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param newColumnName the new column name
     * @param combineFunc the combine func
     * @throws E the e
     */
    <E extends Exception> void combineColumns(Tuple3<String, String, String> columnNames, String newColumnName, Try.TriFunction<?, ?, ?, ?, E> combineFunc)
            throws E;

    /**
     * Combine columns.
     *
     * @param <E> the element type
     * @param columnNameFilter the column name filter
     * @param newColumnName the new column name
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @throws E the e
     */
    <E extends Exception> void combineColumns(Try.Predicate<String, E> columnNameFilter, String newColumnName, Class<?> newColumnClass) throws E;

    /**
     * Combine columns.
     *
     * @param <E> the element type
     * @param <E2> the generic type
     * @param columnNameFilter the column name filter
     * @param newColumnName the new column name
     * @param combineFunc DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     * @throws E2 the e2
     */
    <E extends Exception, E2 extends Exception> void combineColumns(Try.Predicate<String, E> columnNameFilter, String newColumnName,
            Try.Function<? super DisposableObjArray, ?, E2> combineFunc) throws E, E2;

    /**
     * Divide column.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param newColumnNames the new column names
     * @param divideFunc the divide func
     * @throws E the e
     */
    <T, E extends Exception> void divideColumn(String columnName, Collection<String> newColumnNames, Try.Function<T, ? extends List<?>, E> divideFunc) throws E;

    /**
     * Divide column.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param newColumnNames the new column names
     * @param output the output
     * @throws E the e
     */
    <T, E extends Exception> void divideColumn(String columnName, Collection<String> newColumnNames, Try.BiConsumer<T, Object[], E> output) throws E;

    /**
     * Divide column.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param newColumnNames the new column names
     * @param output the output
     * @throws E the e
     */
    <T, E extends Exception> void divideColumn(String columnName, Tuple2<String, String> newColumnNames, Try.BiConsumer<T, Pair<Object, Object>, E> output)
            throws E;

    /**
     * Divide column.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param newColumnNames the new column names
     * @param output the output
     * @throws E the e
     */
    <T, E extends Exception> void divideColumn(String columnName, Tuple3<String, String, String> newColumnNames,
            Try.BiConsumer<T, Triple<Object, Object, Object>, E> output) throws E;

    /**
     * Adds the row.
     *
     * @param row can be Object[]/List/Map/Entity with getter/setter methods
     */
    void addRow(Object row);

    /**
     * Adds the row.
     *
     * @param rowIndex the row index
     * @param row can be Object[]/List/Map/Entity with getter/setter methods
     */
    void addRow(int rowIndex, Object row);

    /**
     * Removes the row.
     *
     * @param rowIndex the row index
     */
    void removeRow(int rowIndex);

    /**
     * Removes the rows.
     *
     * @param indices the indices
     */
    void removeRows(int... indices);

    /**
     * Removes the row range.
     *
     * @param inclusiveFromRowIndex the inclusive from row index
     * @param exclusiveToRowIndex the exclusive to row index
     */
    void removeRowRange(int inclusiveFromRowIndex, int exclusiveToRowIndex);

    /**
     * Update the values in the specified row with the specified Try.Function.
     *
     * @param <E> the element type
     * @param rowIndex the row index
     * @param func the func
     * @throws E the e
     */
    <E extends Exception> void updateRow(int rowIndex, Try.Function<?, ?, E> func) throws E;

    /**
     * Update the values in the specified rows one by one with the specified Try.Function.
     *
     * @param <E> the element type
     * @param indices the indices
     * @param func the func
     * @throws E the e
     */
    <E extends Exception> void updateRows(int[] indices, Try.Function<?, ?, E> func) throws E;

    /**
     * Update all the values in this DataSet with the specified Try.Function.
     *
     * @param <E> the element type
     * @param func the func
     * @throws E the e
     */
    <E extends Exception> void updateAll(Try.Function<?, ?, E> func) throws E;

    /**
     * Replace all the values in this DataSet with the specified new value if it matches the specified condition.
     *
     * @param <E> the element type
     * @param func the func
     * @param newValue the new value
     * @throws E the e
     */
    <E extends Exception> void replaceIf(Try.Predicate<?, E> func, Object newValue) throws E;

    /**
     * Returns the current row number.
     *
     * @return the int
     */
    int currentRowNum();

    /**
     * Move the cursor to the specified row.
     *
     * @param rowNum the row num
     * @return this object itself.
     */
    DataSet absolute(int rowNum);

    /**
     * Gets the row.
     *
     * @param rowNum the row num
     * @return the row
     */
    Object[] getRow(int rowNum);

    /**
     * Gets the row.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param rowNum the row num
     * @return the row
     */
    <T> T getRow(Class<? extends T> rowClass, int rowNum);

    /**
     * Gets the row.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @param rowNum the row num
     * @return the row
     */
    <T> T getRow(Class<? extends T> rowClass, Collection<String> columnNames, int rowNum);

    /**
     * Gets the row.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param rowNum the row num
     * @return the row
     */
    <T> T getRow(IntFunction<? extends T> rowSupplier, int rowNum);

    /**
     * Gets the row.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @param rowNum the row num
     * @return the row
     */
    <T> T getRow(IntFunction<? extends T> rowSupplier, Collection<String> columnNames, int rowNum);

    /**
     * First row.
     *
     * @return {@code Optional<Object[]>}
     */
    Optional<Object[]> firstRow();

    /**
     * First row.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @return {@code Optional<E>}
     */
    <T> Optional<T> firstRow(Class<? extends T> rowClass);

    /**
     * First row.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @return {@code Optional<E>}
     */
    <T> Optional<T> firstRow(Class<? extends T> rowClass, Collection<String> columnNames);

    /**
     * First row.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @return {@code Optional<T>}
     */
    <T> Optional<T> firstRow(IntFunction<? extends T> rowSupplier);

    /**
     * First row.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @return {@code Optional<T>}
     */
    <T> Optional<T> firstRow(IntFunction<? extends T> rowSupplier, Collection<String> columnNames);

    /**
     * Last row.
     *
     * @return {@code Optional<Object[]>}
     */
    Optional<Object[]> lastRow();

    /**
     * Last row.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @return {@code Optional<E>}
     */
    <T> Optional<T> lastRow(Class<? extends T> rowClass);

    /**
     * Last row.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     *            which can be object array/list/set/map/entity.
     * @param columnNames the column names
     * @return {@code Optional<E>}
     */
    <T> Optional<T> lastRow(Class<? extends T> rowClass, Collection<String> columnNames);

    /**
     * Last row.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @return {@code Optional<T>}
     */
    <T> Optional<T> lastRow(IntFunction<? extends T> rowSupplier);

    /**
     * Last row.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @return {@code Optional<T>}
     */
    <T> Optional<T> lastRow(IntFunction<? extends T> rowSupplier, Collection<String> columnNames);

    /**
     * Performs the given action for each row of the {@code DataSet}
     * until all rows have been processed or the action throws an
     * exception.
     *
     * @param <E> the element type
     * @param action DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void forEach(Try.Consumer<? super DisposableObjArray, E> action) throws E;

    /**
     * Performs the given action for each row of the {@code DataSet}
     * until all rows have been processed or the action throws an
     * exception.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param action DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void forEach(Collection<String> columnNames, Try.Consumer<? super DisposableObjArray, E> action) throws E;

    /**
     * Performs the given action for each row of the {@code DataSet}
     * until all rows have been processed or the action throws an
     * exception.
     *
     * @param <E> the element type
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param action DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Try.Consumer<? super DisposableObjArray, E> action) throws E;

    /**
     * Performs the given action for each row of the {@code DataSet}
     * until all rows have been processed or the action throws an
     * exception.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param action DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void forEach(Collection<String> columnNames, int fromRowIndex, int toRowIndex, Try.Consumer<? super DisposableObjArray, E> action)
            throws E;

    /**
     * For each.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param action the action
     * @throws E the e
     */
    <E extends Exception> void forEach(Tuple2<String, String> columnNames, Try.BiConsumer<?, ?, E> action) throws E;

    /**
     * For each.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param action the action
     * @throws E the e
     */
    <E extends Exception> void forEach(Tuple2<String, String> columnNames, int fromRowIndex, int toRowIndex, Try.BiConsumer<?, ?, E> action) throws E;

    /**
     * For each.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param action the action
     * @throws E the e
     */
    <E extends Exception> void forEach(Tuple3<String, String, String> columnNames, Try.TriConsumer<?, ?, ?, E> action) throws E;

    /**
     * For each.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param action the action
     * @throws E the e
     */
    <E extends Exception> void forEach(Tuple3<String, String, String> columnNames, int fromRowIndex, int toRowIndex, Try.TriConsumer<?, ?, ?, E> action)
            throws E;

    /**
     * To list.
     *
     * @return the list
     */
    List<Object[]> toList();

    /**
     * To list.
     *
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the list
     */
    List<Object[]> toList(int fromRowIndex, int toRowIndex);

    /**
     * To list.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @return the list
     */
    <T> List<T> toList(Class<? extends T> rowClass);

    /**
     * To list.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the list
     */
    <T> List<T> toList(Class<? extends T> rowClass, int fromRowIndex, int toRowIndex);

    /**
     * To list.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @return the list
     */
    <T> List<T> toList(Class<? extends T> rowClass, Collection<String> columnNames);

    /**
     * To list.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the list
     */
    <T> List<T> toList(Class<? extends T> rowClass, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * To list.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @return the list
     */
    <T> List<T> toList(IntFunction<? extends T> rowSupplier);

    /**
     * To list.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the list
     */
    <T> List<T> toList(IntFunction<? extends T> rowSupplier, int fromRowIndex, int toRowIndex);

    /**
     * To list.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @return the list
     */
    <T> List<T> toList(IntFunction<? extends T> rowSupplier, Collection<String> columnNames);

    /**
     * To list.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the list
     */
    <T> List<T> toList(IntFunction<? extends T> rowSupplier, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * To map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyColumnName the key column name
     * @param valueColumnName the value column name
     * @return the map
     */
    <K, V> Map<K, V> toMap(String keyColumnName, String valueColumnName);

    /**
     * To map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyColumnName the key column name
     * @param valueColumnName the value column name
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the map
     */
    <K, V> Map<K, V> toMap(String keyColumnName, String valueColumnName, int fromRowIndex, int toRowIndex);

    /**
     * To map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M> the generic type
     * @param keyColumnName the key column name
     * @param valueColumnName the value column name
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param supplier the supplier
     * @return the m
     */
    <K, V, M extends Map<K, V>> M toMap(String keyColumnName, String valueColumnName, int fromRowIndex, int toRowIndex, IntFunction<? extends M> supplier);

    /**
     * To map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @return the map
     */
    <K, V> Map<K, V> toMap(Class<? extends V> rowClass, String keyColumnName, Collection<String> valueColumnNames);

    /**
     * To map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the map
     */
    <K, V> Map<K, V> toMap(Class<? extends V> rowClass, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex, int toRowIndex);

    /**
     * To map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param supplier the supplier
     * @return the m
     */
    <K, V, M extends Map<K, V>> M toMap(Class<? extends V> rowClass, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex,
            int toRowIndex, IntFunction<? extends M> supplier);

    /**
     * To map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @return the map
     */
    <K, V> Map<K, V> toMap(IntFunction<? extends V> rowSupplier, String keyColumnName, Collection<String> valueColumnNames);

    /**
     * To map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the map
     */
    <K, V> Map<K, V> toMap(IntFunction<? extends V> rowSupplier, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex, int toRowIndex);

    /**
     * To map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param supplier the supplier
     * @return the m
     */
    <K, V, M extends Map<K, V>> M toMap(IntFunction<? extends V> rowSupplier, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex,
            int toRowIndex, IntFunction<? extends M> supplier);

    /**
     * To multimap.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param keyColumnName the key column name
     * @param valueColumnName the value column name
     * @return the list multimap
     */
    <K, E> ListMultimap<K, E> toMultimap(String keyColumnName, String valueColumnName);

    /**
     * To multimap.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param keyColumnName the key column name
     * @param valueColumnName the value column name
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the list multimap
     */
    <K, E> ListMultimap<K, E> toMultimap(String keyColumnName, String valueColumnName, int fromRowIndex, int toRowIndex);

    /**
     * To multimap.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param <V> the value type
     * @param <M> the generic type
     * @param keyColumnName the key column name
     * @param valueColumnName the value column name
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param supplier the supplier
     * @return the m
     */
    <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(String keyColumnName, String valueColumnName, int fromRowIndex, int toRowIndex,
            IntFunction<? extends M> supplier);

    /**
     * To multimap.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @return the list multimap
     */
    <K, E> ListMultimap<K, E> toMultimap(Class<? extends E> rowClass, String keyColumnName, Collection<String> valueColumnNames);

    /**
     * To multimap.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the list multimap
     */
    <K, E> ListMultimap<K, E> toMultimap(Class<? extends E> rowClass, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex,
            int toRowIndex);

    /**
     * To multimap.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param <V> the value type
     * @param <M> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param supplier the supplier
     * @return the m
     */
    <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(Class<? extends E> rowClass, String keyColumnName,
            Collection<String> valueColumnNames, int fromRowIndex, int toRowIndex, IntFunction<? extends M> supplier);

    /**
     * To multimap.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @return the list multimap
     */
    <K, E> ListMultimap<K, E> toMultimap(IntFunction<? extends E> rowSupplier, String keyColumnName, Collection<String> valueColumnNames);

    /**
     * To multimap.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the list multimap
     */
    <K, E> ListMultimap<K, E> toMultimap(IntFunction<? extends E> rowSupplier, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex,
            int toRowIndex);

    /**
     * To multimap.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param <V> the value type
     * @param <M> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName the key column name
     * @param valueColumnNames the value column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param supplier the supplier
     * @return the m
     */
    <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(IntFunction<? extends E> rowSupplier, String keyColumnName,
            Collection<String> valueColumnNames, int fromRowIndex, int toRowIndex, IntFunction<? extends M> supplier);

    /**
     * To JSON.
     *
     * @return the string
     */
    String toJSON();

    /**
     * To JSON.
     *
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the string
     */
    String toJSON(int fromRowIndex, int toRowIndex);

    /**
     * To JSON.
     *
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the string
     */
    String toJSON(Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * To JSON.
     *
     * @param out the out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(File out) throws UncheckedIOException;

    /**
     * To JSON.
     *
     * @param out the out
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(File out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To JSON.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(File out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To JSON.
     *
     * @param out the out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(OutputStream out) throws UncheckedIOException;

    /**
     * To JSON.
     *
     * @param out the out
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(OutputStream out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To JSON.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(OutputStream out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To JSON.
     *
     * @param out the out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(Writer out) throws UncheckedIOException;

    /**
     * To JSON.
     *
     * @param out the out
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(Writer out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To JSON.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(Writer out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @return the string
     */
    String toXML();

    /**
     * To XML.
     *
     * @param rowElementName the row element name
     * @return the string
     */
    String toXML(String rowElementName);

    /**
     * To XML.
     *
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the string
     */
    String toXML(int fromRowIndex, int toRowIndex);

    /**
     * To XML.
     *
     * @param rowElementName the row element name
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the string
     */
    String toXML(String rowElementName, int fromRowIndex, int toRowIndex);

    /**
     * To XML.
     *
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the string
     */
    String toXML(Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * To XML.
     *
     * @param rowElementName the row element name
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the string
     */
    String toXML(String rowElementName, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * To XML.
     *
     * @param out the out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param rowElementName the row element name
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out, String rowElementName) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param rowElementName the row element name
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out, String rowElementName, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param rowElementName the row element name
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out, String rowElementName, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param rowElementName the row element name
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out, String rowElementName) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param rowElementName the row element name
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out, String rowElementName, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param rowElementName the row element name
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out, String rowElementName, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param rowElementName the row element name
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out, String rowElementName) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param rowElementName the row element name
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out, String rowElementName, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To XML.
     *
     * @param out the out
     * @param rowElementName the row element name
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out, String rowElementName, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To CSV.
     *
     * @return the string
     */
    String toCSV();

    /**
     * To CSV.
     *
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the string
     */
    String toCSV(Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * To CSV.
     *
     * @param writeTitle the write title
     * @param quoteValue the quote value
     * @return the string
     */
    String toCSV(boolean writeTitle, boolean quoteValue);

    /**
     * To CSV.
     *
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param writeTitle the write title
     * @param quoteValue the quote value
     * @return the string
     */
    String toCSV(Collection<String> columnNames, int fromRowIndex, int toRowIndex, boolean writeTitle, boolean quoteValue);

    /**
     * To CSV.
     *
     * @param out the out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(File out) throws UncheckedIOException;

    /**
     * To CSV.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(File out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To CSV.
     *
     * @param out the out
     * @param writeTitle the write title
     * @param quoteValue the quote value
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(File out, boolean writeTitle, boolean quoteValue) throws UncheckedIOException;

    /**
     * To CSV.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param writeTitle the write title
     * @param quoteValue the quote value
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(File out, Collection<String> columnNames, int fromRowIndex, int toRowIndex, boolean writeTitle, boolean quoteValue) throws UncheckedIOException;

    /**
     * To CSV.
     *
     * @param out the out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(OutputStream out);

    /**
     * To CSV.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(OutputStream out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To CSV.
     *
     * @param out the out
     * @param writeTitle the write title
     * @param quoteValue the quote value
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(OutputStream out, boolean writeTitle, boolean quoteValue) throws UncheckedIOException;

    /**
     * To CSV.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param writeTitle the write title
     * @param quoteValue the quote value
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(OutputStream out, Collection<String> columnNames, int fromRowIndex, int toRowIndex, boolean writeTitle, boolean quoteValue)
            throws UncheckedIOException;

    /**
     * To CSV.
     *
     * @param out the out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(Writer out);

    /**
     * To CSV.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(Writer out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * To CSV.
     *
     * @param out the out
     * @param writeTitle the write title
     * @param quoteValue the quote value
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(Writer out, boolean writeTitle, boolean quoteValue) throws UncheckedIOException;

    /**
     * To CSV.
     *
     * @param out the out
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param writeTitle the write title
     * @param quoteValue the quote value
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(Writer out, Collection<String> columnNames, int fromRowIndex, int toRowIndex, boolean writeTitle, boolean quoteValue)
            throws UncheckedIOException;

    /**
     * Group by.
     *
     * @param columnName specifying the column to group by.
     * @return the data set
     */
    DataSet groupBy(String columnName);

    /**
     * Group by.
     *
     * @param <T> the generic type
     * @param columnName the column name
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param collector the collector
     * @return the data set
     */
    <T> DataSet groupBy(String columnName, String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     * Group by.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param func the func
     * @return the data set
     * @throws E the e
     */
    <T, E extends Exception> DataSet groupBy(String columnName, String aggregateResultColumnName, String aggregateOnColumnName,
            Try.Function<Stream<T>, ?, E> func) throws E;

    /**
     * Group by.
     *
     * @param columnName the column name
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param collector the collector
     * @return the data set
     */
    DataSet groupBy(String columnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     * Group by.
     *
     * @param <U> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector the collector
     * @return the data set
     * @throws E the e
     */
    <U, E extends Exception> DataSet groupBy(String columnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Try.Function<? super DisposableObjArray, U, E> rowMapper, Collector<? super U, ?, ?> collector) throws E;

    /**
     * Group by.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param columnName the column name
     * @param keyMapper the key mapper
     * @return the data set
     * @throws E the e
     */
    <K, E extends Exception> DataSet groupBy(String columnName, Try.Function<K, ?, E> keyMapper) throws E;

    /**
     * Group by.
     *
     * @param <K> the key type
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param keyMapper the key mapper
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param collector the collector
     * @return the data set
     * @throws E the e
     */
    <K, T, E extends Exception> DataSet groupBy(String columnName, Try.Function<K, ?, E> keyMapper, String aggregateResultColumnName,
            String aggregateOnColumnName, Collector<T, ?, ?> collector) throws E;

    /**
     * Group by.
     *
     * @param <K> the key type
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param columnName the column name
     * @param keyMapper the key mapper
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param func the func
     * @return the data set
     * @throws E the e
     * @throws E2 the e2
     */
    <K, T, E extends Exception, E2 extends Exception> DataSet groupBy(String columnName, Try.Function<K, ?, E> keyMapper, String aggregateResultColumnName,
            String aggregateOnColumnName, Try.Function<Stream<T>, ?, E2> func) throws E, E2;

    /**
     * Group by.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param columnName the column name
     * @param keyMapper the key mapper
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param collector the collector
     * @return the data set
     * @throws E the e
     */
    <K, E extends Exception> DataSet groupBy(String columnName, Try.Function<K, ?, E> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Collector<? super Object[], ?, ?> collector) throws E;

    /**
     * Group by.
     *
     * @param <K> the key type
     * @param <U> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param columnName the column name
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector the collector
     * @return the data set
     * @throws E the e
     * @throws E2 the e2
     */
    <K, U, E extends Exception, E2 extends Exception> DataSet groupBy(String columnName, Try.Function<K, ?, E> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Try.Function<? super DisposableObjArray, U, E2> rowMapper, Collector<? super U, ?, ?> collector)
            throws E, E2;

    /**
     * Group by.
     *
     * @param columnNames the column names
     * @return the data set
     */
    DataSet groupBy(Collection<String> columnNames);

    /**
     * Group by.
     *
     * @param <T> the generic type
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param collector the collector
     * @return the data set
     */
    <T> DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     * Group by.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param func the func
     * @return the data set
     * @throws E the e
     */
    <T, E extends Exception> DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName,
            Try.Function<Stream<T>, ?, E> func) throws E;

    /**
     * Group by.
     *
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param collector the collector
     * @return the data set
     */
    DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     * Group by.
     *
     * @param <U> the generic type
     * @param <E> the element type
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector the collector
     * @return the data set
     * @throws E the e
     */
    <U, E extends Exception> DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Try.Function<? super DisposableObjArray, U, E> rowMapper, Collector<? super U, ?, ?> collector) throws E;

    /**
     * Group by.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet groupBy(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper) throws E;

    /**
     * Group by.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param collector the collector
     * @return the data set
     * @throws E the e
     */
    <T, E extends Exception> DataSet groupBy(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector) throws E;

    /**
     * Group by.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param func the func
     * @return the data set
     * @throws E the e
     * @throws E2 the e2
     */
    <T, E extends Exception, E2 extends Exception> DataSet groupBy(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, String aggregateOnColumnName, Try.Function<Stream<T>, ?, E2> func) throws E, E2;

    /**
     * Group by.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param collector the collector
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet groupBy(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Collector<? super Object[], ?, ?> collector) throws E;

    /**
     * Group by.
     *
     * @param <U> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector the collector
     * @return the data set
     * @throws E the e
     * @throws E2 the e2
     */
    <U, E extends Exception, E2 extends Exception> DataSet groupBy(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Try.Function<? super DisposableObjArray, U, E2> rowMapper,
            Collector<? super U, ?, ?> collector) throws E, E2;

    /**
     * Rollup.
     *
     * @param columnNames the column names
     * @return the stream
     */
    Stream<DataSet> rollup(Collection<String> columnNames);

    /**
     * Rollup.
     *
     * @param <T> the generic type
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param collector the collector
     * @return the stream
     */
    <T> Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     * Rollup.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param func the func
     * @return the stream
     */
    <T, E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName,
            Try.Function<Stream<T>, ?, E> func);

    /**
     * Rollup.
     *
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param collector the collector
     * @return the stream
     */
    Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     * Rollup.
     *
     * @param <U> the generic type
     * @param <E> the element type
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector the collector
     * @return the stream
     */
    <U, E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Try.Function<? super DisposableObjArray, U, E> rowMapper, Collector<? super U, ?, ?> collector);

    /**
     * Rollup.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the stream
     */
    <E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper);

    /**
     * Rollup.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param collector the collector
     * @return the stream
     */
    <T, E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     * Rollup.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param func the func
     * @return the stream
     */
    <T, E extends Exception, E2 extends Exception> Stream<DataSet> rollup(Collection<String> columnNames,
            Try.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, String aggregateOnColumnName,
            Try.Function<Stream<T>, ?, E2> func);

    /**
     * Rollup.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param collector the collector
     * @return the stream
     */
    <E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Collector<? super Object[], ?, ?> collector);

    /**
     * Rollup.
     *
     * @param <U> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector the collector
     * @return the stream
     */
    <U, E extends Exception, E2 extends Exception> Stream<DataSet> rollup(Collection<String> columnNames,
            Try.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Try.Function<? super DisposableObjArray, U, E2> rowMapper, Collector<? super U, ?, ?> collector);

    /**
     * Cube.
     *
     * @param columnNames the column names
     * @return the stream
     */
    Stream<DataSet> cube(Collection<String> columnNames);

    /**
     * Cube.
     *
     * @param <T> the generic type
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param collector the collector
     * @return the stream
     */
    <T> Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     * Cube.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param func the func
     * @return the stream
     */
    <T, E extends Exception> Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName,
            Try.Function<Stream<T>, ?, E> func);

    /**
     * Cube.
     *
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param collector the collector
     * @return the stream
     */
    Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     * Cube.
     *
     * @param <U> the generic type
     * @param <E> the element type
     * @param columnNames the column names
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector the collector
     * @return the stream
     */
    <U, E extends Exception> Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Try.Function<? super DisposableObjArray, U, E> rowMapper, Collector<? super U, ?, ?> collector);

    /**
     * Cube.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the stream
     */
    <E extends Exception> Stream<DataSet> cube(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper);

    /**
     * Cube.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param collector the collector
     * @return the stream
     */
    <T, E extends Exception> Stream<DataSet> cube(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     * Cube.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnName the aggregate on column name
     * @param func the func
     * @return the stream
     */
    <T, E extends Exception, E2 extends Exception> Stream<DataSet> cube(Collection<String> columnNames,
            Try.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, String aggregateOnColumnName,
            Try.Function<Stream<T>, ?, E2> func);

    /**
     * Cube.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param collector the collector
     * @return the stream
     */
    <E extends Exception> Stream<DataSet> cube(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Collector<? super Object[], ?, ?> collector);

    /**
     * Cube.
     *
     * @param <U> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName the aggregate result column name
     * @param aggregateOnColumnNames the aggregate on column names
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector the collector
     * @return the stream
     */
    <U, E extends Exception, E2 extends Exception> Stream<DataSet> cube(Collection<String> columnNames,
            Try.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Try.Function<? super DisposableObjArray, U, E2> rowMapper, Collector<? super U, ?, ?> collector);

    /**
     * Sort by.
     *
     * @param columnName the column name
     */
    void sortBy(String columnName);

    /**
     * Sort by.
     *
     * @param <T> the generic type
     * @param columnName the column name
     * @param cmp the cmp
     */
    <T> void sortBy(String columnName, Comparator<T> cmp);

    /**
     * Sort by.
     *
     * @param columnNames the column names
     */
    void sortBy(Collection<String> columnNames);

    /**
     * Sort by.
     *
     * @param columnNames the column names
     * @param cmp the cmp
     */
    void sortBy(Collection<String> columnNames, Comparator<? super Object[]> cmp);

    /**
     * Sort by.
     *
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     */
    @SuppressWarnings("rawtypes")
    void sortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyMapper);

    /**
     * Parallel sort by.
     *
     * @param columnName the column name
     */
    void parallelSortBy(String columnName);

    /**
     * Parallel sort by.
     *
     * @param <T> the generic type
     * @param columnName the column name
     * @param cmp the cmp
     */
    <T> void parallelSortBy(String columnName, Comparator<T> cmp);

    /**
     * Parallel sort by.
     *
     * @param columnNames the column names
     */
    void parallelSortBy(Collection<String> columnNames);

    /**
     * Parallel sort by.
     *
     * @param columnNames the column names
     * @param cmp the cmp
     */
    void parallelSortBy(Collection<String> columnNames, Comparator<? super Object[]> cmp);

    /**
     * Parallel sort by.
     *
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     */
    @SuppressWarnings("rawtypes")
    void parallelSortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyMapper);

    /**
     * Top by.
     *
     * @param columnName the column name
     * @param n the n
     * @return the data set
     */
    DataSet topBy(String columnName, int n);

    /**
     * Top by.
     *
     * @param <T> the generic type
     * @param columnName the column name
     * @param n the n
     * @param cmp the cmp
     * @return the data set
     */
    <T> DataSet topBy(String columnName, int n, Comparator<T> cmp);

    /**
     * Top by.
     *
     * @param columnNames the column names
     * @param n the n
     * @return the data set
     */
    DataSet topBy(Collection<String> columnNames, int n);

    /**
     * Top by.
     *
     * @param columnNames the column names
     * @param n the n
     * @param cmp the cmp
     * @return the data set
     */
    DataSet topBy(Collection<String> columnNames, int n, Comparator<? super Object[]> cmp);

    /**
     * Top by.
     *
     * @param columnNames the column names
     * @param n the n
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the data set
     */
    @SuppressWarnings("rawtypes")
    DataSet topBy(Collection<String> columnNames, int n, Function<? super DisposableObjArray, ? extends Comparable> keyMapper);

    /**
     * Returns a new <code>DataSet</code> with the rows de-duplicated by the values in all columns.
     *
     * @return a new DataSet
     */
    DataSet distinct();

    /**
     * Returns a new <code>DataSet</code> with the rows de-duplicated by the value in the specified column.
     *
     * @param columnName the column name
     * @return a new DataSet
     */
    DataSet distinctBy(String columnName);

    /**
     * Returns a new <code>DataSet</code> with the rows de-duplicated by the value in the specified column from the specified <code>fromRowIndex</code> to <code>toRowIndex</code>.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param columnName the column name
     * @param keyMapper don't change value of the input parameter.
     * @return the data set
     * @throws E the e
     */
    <K, E extends Exception> DataSet distinctBy(String columnName, Try.Function<K, ?, E> keyMapper) throws E;

    /**
     * Returns a new <code>DataSet</code> with the rows de-duplicated by the values in the specified columns.
     *
     * @param columnNames the column names
     * @return a new DataSet
     */
    DataSet distinctBy(Collection<String> columnNames);

    /**
     * Returns a new <code>DataSet</code> with the rows de-duplicated by the values in the specified columns from the specified <code>fromRowIndex</code> to <code>toRowIndex</code>.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet distinctBy(Collection<String> columnNames, Try.Function<? super DisposableObjArray, ?, E> keyMapper) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Try.Predicate<? super DisposableObjArray, E> filter) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max the max
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Try.Predicate<? super DisposableObjArray, E> filter, int max) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(int fromRowIndex, int toRowIndex, Try.Predicate<? super DisposableObjArray, E> filter) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max the max
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(int fromRowIndex, int toRowIndex, Try.Predicate<? super DisposableObjArray, E> filter, int max) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param filter the filter
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, Try.BiPredicate<?, ?, E> filter) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param filter the filter
     * @param max the max
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, Try.BiPredicate<?, ?, E> filter, int max) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param filter the filter
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, int fromRowIndex, int toRowIndex, Try.BiPredicate<?, ?, E> filter) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param filter the filter
     * @param max the max
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, int fromRowIndex, int toRowIndex, Try.BiPredicate<?, ?, E> filter, int max)
            throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param filter the filter
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple3<String, String, String> columnNames, Try.TriPredicate<?, ?, ?, E> filter) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param filter the filter
     * @param max the max
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple3<String, String, String> columnNames, Try.TriPredicate<?, ?, ?, E> filter, int max) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param filter the filter
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple3<String, String, String> columnNames, int fromRowIndex, int toRowIndex, Try.TriPredicate<?, ?, ?, E> filter)
            throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param filter the filter
     * @param max the max
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple3<String, String, String> columnNames, int fromRowIndex, int toRowIndex, Try.TriPredicate<?, ?, ?, E> filter,
            int max) throws E;

    /**
     * Filter.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param filter the filter
     * @return the data set
     * @throws E the e
     */
    <T, E extends Exception> DataSet filter(String columnName, Try.Predicate<T, E> filter) throws E;

    /**
     * Filter.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param filter the filter
     * @param max the max
     * @return the data set
     * @throws E the e
     */
    <T, E extends Exception> DataSet filter(String columnName, Try.Predicate<T, E> filter, int max) throws E;

    /**
     * Filter.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param filter the filter
     * @return the data set
     * @throws E the e
     */
    <T, E extends Exception> DataSet filter(String columnName, int fromRowIndex, int toRowIndex, Try.Predicate<T, E> filter) throws E;

    /**
     * Filter.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param columnName the column name
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param filter the filter
     * @param max the max
     * @return the data set
     * @throws E the e
     */
    <T, E extends Exception> DataSet filter(String columnName, int fromRowIndex, int toRowIndex, Try.Predicate<T, E> filter, int max) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Collection<String> columnNames, Try.Predicate<? super DisposableObjArray, E> filter) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max the max
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Collection<String> columnNames, Try.Predicate<? super DisposableObjArray, E> filter, int max) throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Collection<String> columnNames, int fromRowIndex, int toRowIndex, Try.Predicate<? super DisposableObjArray, E> filter)
            throws E;

    /**
     * Filter.
     *
     * @param <E> the element type
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max the max
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Collection<String> columnNames, int fromRowIndex, int toRowIndex, Try.Predicate<? super DisposableObjArray, E> filter,
            int max) throws E;

    /**
     * Map.
     *
     * @param <E> the element type
     * @param fromColumnName the from column name
     * @param func the func
     * @param newColumnName the new column name
     * @param copyingColumnName the copying column name
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet map(String fromColumnName, Try.Function<?, ?, E> func, String newColumnName, String copyingColumnName) throws E;

    /**
     * Map.
     *
     * @param <E> the element type
     * @param fromColumnName the from column name
     * @param func the func
     * @param newColumnName the new column name
     * @param copyingColumnNames the copying column names
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet map(String fromColumnName, Try.Function<?, ?, E> func, String newColumnName, Collection<String> copyingColumnNames) throws E;

    /**
     * Map.
     *
     * @param <E> the element type
     * @param fromColumnNames the from column names
     * @param func the func
     * @param newColumnName the new column name
     * @param copyingColumnNames the copying column names
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet map(Tuple2<String, String> fromColumnNames, Try.BiFunction<?, ?, ?, E> func, String newColumnName,
            Collection<String> copyingColumnNames) throws E;

    /**
     * Map.
     *
     * @param <E> the element type
     * @param fromColumnNames the from column names
     * @param func the func
     * @param newColumnName the new column name
     * @param copyingColumnNames the copying column names
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet map(Tuple3<String, String, String> fromColumnNames, Try.TriFunction<?, ?, ?, ?, E> func, String newColumnName,
            Collection<String> copyingColumnNames) throws E;

    /**
     * Map.
     *
     * @param <E> the element type
     * @param fromColumnNames the from column names
     * @param func DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param newColumnName the new column name
     * @param copyingColumnNames the copying column names
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet map(Collection<String> fromColumnNames, Try.Function<DisposableObjArray, ?, E> func, String newColumnName,
            Collection<String> copyingColumnNames) throws E;

    /**
     * Flat map.
     *
     * @param <E> the element type
     * @param fromColumnName the from column name
     * @param func the func
     * @param newColumnName the new column name
     * @param copyingColumnName the copying column name
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet flatMap(String fromColumnName, Try.Function<?, ? extends Collection<?>, E> func, String newColumnName,
            String copyingColumnName) throws E;

    /**
     * Flat map.
     *
     * @param <E> the element type
     * @param fromColumnName the from column name
     * @param func the func
     * @param newColumnName the new column name
     * @param copyingColumnNames the copying column names
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet flatMap(String fromColumnName, Try.Function<?, ? extends Collection<?>, E> func, String newColumnName,
            Collection<String> copyingColumnNames) throws E;

    /**
     * Flat map.
     *
     * @param <E> the element type
     * @param fromColumnNames the from column names
     * @param func the func
     * @param newColumnName the new column name
     * @param copyingColumnNames the copying column names
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet flatMap(Tuple2<String, String> fromColumnNames, Try.BiFunction<?, ?, ? extends Collection<?>, E> func, String newColumnName,
            Collection<String> copyingColumnNames) throws E;

    /**
     * Flat map.
     *
     * @param <E> the element type
     * @param fromColumnNames the from column names
     * @param func the func
     * @param newColumnName the new column name
     * @param copyingColumnNames the copying column names
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet flatMap(Tuple3<String, String, String> fromColumnNames, Try.TriFunction<?, ?, ?, ? extends Collection<?>, E> func,
            String newColumnName, Collection<String> copyingColumnNames) throws E;

    /**
     * Flat map.
     *
     * @param <E> the element type
     * @param fromColumnNames the from column names
     * @param func DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param newColumnName the new column name
     * @param copyingColumnNames the copying column names
     * @return the data set
     * @throws E the e
     */
    <E extends Exception> DataSet flatMap(Collection<String> fromColumnNames, Try.Function<DisposableObjArray, ? extends Collection<?>, E> func,
            String newColumnName, Collection<String> copyingColumnNames) throws E;

    /**
     * Returns a new <code>DataSet</code> that is limited to the rows where there is a match in both <code>this DataSet</code> and <code>right DataSet</code>.
     *
     * @param right the right
     * @param columnName the column name
     * @param refColumnName the ref column name
     * @return a new DataSet
     */
    DataSet innerJoin(DataSet right, String columnName, String refColumnName);

    /**
     * Returns a new <code>DataSet</code> that is limited to the rows where there is a match in both <code>this DataSet</code> and <code>right DataSet</code>.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @return a new DataSet
     */
    DataSet innerJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new <code>DataSet</code> that is limited to the rows where there is a match in both <code>this DataSet</code> and <code>right DataSet</code>.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @param newColumnName the new column name
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @return a new DataSet
     */
    DataSet innerJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass);

    /**
     * Returns a new <code>DataSet</code> that is limited to the rows where there is a match in both <code>this DataSet</code> and <code>right DataSet</code>.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @param newColumnName the new column name
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @param collSupplier it's for one-to-many join
     * @return a new DataSet
     */
    @SuppressWarnings("rawtypes")
    DataSet innerJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass,
            IntFunction<? extends Collection> collSupplier);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the rows from the specified <code>right DataSet</code> if they have a match with the rows from the this <code>DataSet</code>.
     *
     * @param right the right
     * @param columnName the column name
     * @param refColumnName the ref column name
     * @return a new DataSet
     */
    DataSet leftJoin(DataSet right, String columnName, String refColumnName);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the rows from the specified <code>right DataSet</code> if they have a match with the rows from the this <code>DataSet</code>.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @return a new DataSet
     */
    DataSet leftJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the rows from the specified <code>right DataSet</code> if they have a match with the rows from the this <code>DataSet</code>.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @param newColumnName the new column name
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @return a new DataSet
     */
    DataSet leftJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the rows from the specified <code>right DataSet</code> if they have a match with the rows from the this <code>DataSet</code>.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @param newColumnName the new column name
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @param collSupplier it's for one-to-many join
     * @return a new DataSet
     */
    @SuppressWarnings("rawtypes")
    DataSet leftJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass,
            IntFunction<? extends Collection> collSupplier);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from the specified right <code>DataSet</code> and the rows from <code>this DataSet</code> if they have a match with the rows from the right <code>DataSet</code>.
     *
     * @param right the right
     * @param columnName the column name
     * @param refColumnName the ref column name
     * @return a new DataSet
     */
    DataSet rightJoin(DataSet right, String columnName, String refColumnName);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from the specified right <code>DataSet</code> and the rows from <code>this DataSet</code> if they have a match with the rows from the right <code>DataSet</code>.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @return a new DataSet
     */
    DataSet rightJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from the specified right <code>DataSet</code> and the rows from <code>this DataSet</code> if they have a match with the rows from the right <code>DataSet</code>.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @param newColumnName the new column name
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @return a new DataSet
     */
    DataSet rightJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from the specified right <code>DataSet</code> and the rows from <code>this DataSet</code> if they have a match with the rows from the right <code>DataSet</code>.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @param newColumnName the new column name
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @param collSupplier it's for one-to-many join
     * @return a new DataSet
     */
    @SuppressWarnings("rawtypes")
    DataSet rightJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass,
            IntFunction<? extends Collection> collSupplier);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the specified <code>right DataSet</code>, regardless of whether there are any matches.
     *
     * @param right the right
     * @param columnName the column name
     * @param refColumnName the ref column name
     * @return a new DataSet
     */
    DataSet fullJoin(DataSet right, String columnName, String refColumnName);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the specified <code>right DataSet</code>, regardless of whether there are any matches.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @return a new DataSet
     */
    DataSet fullJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the specified <code>right DataSet</code>, regardless of whether there are any matches.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @param newColumnName the new column name
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @return a new DataSet
     */
    DataSet fullJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the specified <code>right DataSet</code>, regardless of whether there are any matches.
     *
     * @param right the right
     * @param onColumnNames the on column names
     * @param newColumnName the new column name
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @param collSupplier it's for one-to-many join
     * @return a new DataSet
     */
    @SuppressWarnings("rawtypes")
    DataSet fullJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass,
            IntFunction<? extends Collection> collSupplier);

    /**
     * Returns a new <code>DataSet</code>. Duplicated row in the specified {@code DataSet} will be eliminated.
     *
     * @param dataSet the data set
     * @return a new DataSet
     */
    DataSet union(DataSet dataSet);

    /**
     * Returns a new <code>DataSet</code>. Duplicated row in the specified {@code DataSet} will not be eliminated.
     *
     * @param dataSet the data set
     * @return a new DataSet
     */
    DataSet unionAll(DataSet dataSet);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
     * This operation doesn't remove duplicate rows from the final result set.
     *
     * @param other the other
     * @return the data set
     * @see java.util.Collection#retainAll(Collection)
     */
    DataSet intersectAll(DataSet other);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns.
     *
     * @param other the other
     * @return the data set
     * @see java.util.Collection#removeAll(Collection)
     */
    DataSet except(DataSet other);

    /**
     * Returns a new <code>DataSet</code>.
     *
     * @param dataSet the data set
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#difference(com.landawn.abacus.util.IntList)
     */
    DataSet difference(DataSet dataSet);

    /**
     * Symmetric difference.
     *
     * @param dataSet the data set
     * @return the data set
     * @see com.landawn.abacus.util.IntList#symmetricDifference(com.landawn.abacus.util.IntList)
     */
    DataSet symmetricDifference(DataSet dataSet);

    /**
     * Returns a new <code>DataSet</code>.
     *
     * @param dataSet the data set
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#intersection(com.landawn.abacus.util.IntList)
     */
    DataSet intersection(DataSet dataSet);

    /**
     * Returns a new <code>DataSet</code> by appending the specified <code>from</code> <code>DataSet</code> into this <code>DataSet</code>.
     *
     * @param from the from
     * @return the data set
     */
    DataSet merge(DataSet from);

    /**
     * Returns a new <code>DataSet</code> by appending the specified <code>from</code> <code>DataSet</code> into this <code>DataSet</code>.
     *
     * @param from the from
     * @param columnNames the column names
     * @return the data set
     */
    DataSet merge(DataSet from, Collection<String> columnNames);

    /**
     * Returns a new <code>DataSet</code> by appending the specified <code>from</code> <code>DataSet</code> from <code>fromRowIndex</code> to <code>toRowIndex</code> into this <code>DataSet</code>.
     *
     * @param from the from
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the data set
     */
    DataSet merge(DataSet from, int fromRowIndex, int toRowIndex);

    /**
     * Returns a new <code>DataSet</code> by appending the specified <code>columnNames</code> in <code>from</code> <code>DataSet</code> from <code>fromRowIndex</code> to <code>toRowIndex</code> into this <code>DataSet</code>.
     *
     * @param from the from
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the data set
     */
    DataSet merge(DataSet from, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * Cartesian product.
     *
     * @param b the b
     * @return the data set
     */
    DataSet cartesianProduct(DataSet b);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param chunkSize the desired size of each sub DataSet (the last may be smaller).
     * @return the stream
     */
    Stream<DataSet> split(int chunkSize);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param columnNames the column names
     * @param chunkSize the desired size of each sub DataSet (the last may be smaller).
     * @return the stream
     */
    Stream<DataSet> split(Collection<String> columnNames, int chunkSize);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param chunkSize the chunk size
     * @return the list
     */
    List<DataSet> splitt(int chunkSize);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param columnNames the column names
     * @param chunkSize the chunk size
     * @return the list
     */
    List<DataSet> splitt(Collection<String> columnNames, int chunkSize);

    /**
     * Returns a frozen {@code DataSet}.
     *
     * @param columnNames the column names
     * @return a copy of this DataSet
     * @see List#subList(int, int).
     */
    DataSet slice(Collection<String> columnNames);

    /**
     * Returns a frozen {@code DataSet}.
     *
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return a copy of this DataSet
     * @see List#subList(int, int).
     */
    DataSet slice(int fromRowIndex, int toRowIndex);

    /**
     * Returns a frozen {@code DataSet}.
     *
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return a copy of this DataSet
     * @see List#subList(int, int).
     */
    DataSet slice(Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * Returns the copy of this <code>DataSet</code>.
     * The frozen status of the copy will always be false, even the original <code>DataSet</code> is frozen.
     *
     * @return a copy of this DataSet
     */
    DataSet copy();

    /**
     * Returns the copy of this <code>DataSet</code> with specified column name list.
     * The frozen status of the copy will always be false, even the original <code>DataSet</code> is frozen.
     *
     * @param columnNames the column names
     * @return a copy of this DataSet
     */
    DataSet copy(Collection<String> columnNames);

    /**
     * Returns the copy of this <code>DataSet</code> from the specified <code>fromRowIndex</code> to <code>toRowIndex</code>.
     * The frozen status of the copy will always be false, even the original <code>DataSet</code> is frozen.
     *
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return a copy of this DataSet
     */
    DataSet copy(int fromRowIndex, int toRowIndex);

    /**
     * Returns the copy of this <code>DataSet</code> with specified column name list from the specified <code>fromRowIndex</code> to <code>toRowIndex</code>.
     * The frozen status of the copy will always be false, even the original <code>DataSet</code> is frozen.
     *
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return a copy of this DataSet
     */
    DataSet copy(Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * Deeply copy each element in this <code>DataSet</code> by Serialization/Deserialization.
     *
     * @return the data set
     */
    DataSet clone();

    /**
     * Deeply copy each element in this <code>DataSet</code> by Serialization/Deserialization.
     *
     * @param freeze the freeze
     * @return the data set
     */
    DataSet clone(boolean freeze);

    /**
     * Iterator.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param columnNameA the column name A
     * @param columnNameB the column name B
     * @return the bi iterator
     */
    <A, B> BiIterator<A, B> iterator(String columnNameA, String columnNameB);

    /**
     * Iterator.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param columnNameA the column name A
     * @param columnNameB the column name B
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the bi iterator
     */
    <A, B> BiIterator<A, B> iterator(String columnNameA, String columnNameB, int fromRowIndex, int toRowIndex);

    /**
     * Iterator.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param columnNameA the column name A
     * @param columnNameB the column name B
     * @param columnNameC the column name C
     * @return the tri iterator
     */
    <A, B, C> TriIterator<A, B, C> iterator(String columnNameA, String columnNameB, String columnNameC);

    /**
     * Iterator.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param columnNameA the column name A
     * @param columnNameB the column name B
     * @param columnNameC the column name C
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the tri iterator
     */
    <A, B, C> TriIterator<A, B, C> iterator(String columnNameA, String columnNameB, String columnNameC, int fromRowIndex, int toRowIndex);

    /**
     * Method paginate.
     *
     * @param pageSize the page size
     * @return the paginated data set
     */
    PaginatedDataSet paginate(int pageSize);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param columnName the column name
     * @return the stream
     */
    <T> Stream<T> stream(String columnName);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param columnName the column name
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the stream
     */
    <T> Stream<T> stream(String columnName, int fromRowIndex, int toRowIndex);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the stream
     */
    <T> Stream<T> stream(Function<? super DisposableObjArray, T> rowMapper);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the stream
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Function<? super DisposableObjArray, T> rowMapper);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param columnNames the column names
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the stream
     */
    <T> Stream<T> stream(Collection<String> columnNames, Function<? super DisposableObjArray, T> rowMapper);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return the stream
     */
    <T> Stream<T> stream(Collection<String> columnNames, int fromRowIndex, int toRowIndex, Function<? super DisposableObjArray, T> rowMapper);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @return the stream
     */
    <T> Stream<T> stream(Class<? extends T> rowClass);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the stream
     */
    <T> Stream<T> stream(Class<? extends T> rowClass, int fromRowIndex, int toRowIndex);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @return the stream
     */
    <T> Stream<T> stream(Class<? extends T> rowClass, Collection<String> columnNames);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the stream
     */
    <T> Stream<T> stream(Class<? extends T> rowClass, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @return the stream
     */
    <T> Stream<T> stream(IntFunction<? extends T> rowSupplier);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the stream
     */
    <T> Stream<T> stream(IntFunction<? extends T> rowSupplier, int fromRowIndex, int toRowIndex);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @return the stream
     */
    <T> Stream<T> stream(IntFunction<? extends T> rowSupplier, Collection<String> columnNames);

    /**
     * Stream.
     *
     * @param <T> the generic type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the stream
     */
    <T> Stream<T> stream(IntFunction<? extends T> rowSupplier, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * Apply.
     *
     * @param <R> the generic type
     * @param <E> the element type
     * @param func the func
     * @return the r
     * @throws E the e
     */
    <R, E extends Exception> R apply(Try.Function<? super DataSet, R, E> func) throws E;

    /**
     * Accept.
     *
     * @param <E> the element type
     * @param action the action
     * @throws E the e
     */
    <E extends Exception> void accept(Try.Consumer<? super DataSet, E> action) throws E;

    /**
     * Method freeze.
     */
    void freeze();

    /**
     * Method frozen.
     *
     * @return true, if successful
     */
    boolean frozen();

    /**
     * Method clear.
     */
    void clear();

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    boolean isEmpty();

    /**
     * Trim to size.
     */
    void trimToSize();

    /**
     * Returns the size of this {@code DataSet}.
     *
     * @return the int
     */
    int size();

    /**
     * Properties.
     *
     * @return the properties
     */
    Properties<String, Object> properties();

    /**
     * Column names.
     *
     * @return the stream
     */
    Stream<String> columnNames();

    /**
     * Columns.
     *
     * @return the stream
     */
    Stream<ImmutableList<Object>> columns();

    /**
     * Column map.
     *
     * @return key are column name, value is column - an immutable list, backed by the column in this {@code DataSet}.
     */
    Map<String, ImmutableList<Object>> columnMap();

    // DataSetBuilder builder();

    /**
     * Println.
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    void println() throws UncheckedIOException;

    /**
     * Println.
     *
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @throws UncheckedIOException the unchecked IO exception
     */
    void println(Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     * Println.
     *
     * @param <W> the generic type
     * @param outputWriter the output writer
     * @return the specified {@code outputWriter}
     * @throws UncheckedIOException the unchecked IO exception
     */
    <W extends Writer> W println(W outputWriter) throws UncheckedIOException;

    /**
     * Println.
     *
     * @param <W> the generic type
     * @param outputWriter the output writer
     * @param columnNames the column names
     * @param fromRowIndex the from row index
     * @param toRowIndex the to row index
     * @return the specified {@code outputWriter}
     * @throws UncheckedIOException the unchecked IO exception
     */
    <W extends Writer> W println(W outputWriter, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;
}
