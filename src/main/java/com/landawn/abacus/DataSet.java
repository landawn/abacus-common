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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Properties;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.TriIterator;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.Function;
// import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.stream.Collector;
import com.landawn.abacus.util.stream.Stream;

/**
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
     * @return
     */
    ImmutableList<String> columnNameList();

    /**
     *
     * @param columnIndex
     * @return
     */
    String getColumnName(int columnIndex);

    /**
     *
     * @param columnName
     * @return -1 if the specified <code>columnName</code> is not found
     */
    int getColumnIndex(String columnName);

    /**
     * -1 is set to the element in the returned array if the mapping column name is not included in this <code>DataSet</code>.
     *
     * @param columnNames
     * @return
     */
    int[] getColumnIndexes(Collection<String> columnNames);

    /**
     *
     * @param columnName
     * @return true, if successful
     */
    boolean containsColumn(String columnName);

    /**
     * Check if this <code>DataSet</code> contains all the specified columns.
     *
     * @param columnNames
     * @return <code>true</code> if all the specified columns are included in the this <code>DataSet</code>
     */
    boolean containsAllColumns(Collection<String> columnNames);

    /**
     *
     * @param columnName
     * @param newColumnName
     */
    void renameColumn(String columnName, String newColumnName);

    /**
     *
     * @param oldNewNames
     */
    void renameColumns(Map<String, String> oldNewNames);

    /**
     *
     * @param <E>
     * @param columnName
     * @param func
     * @throws E the e
     */
    <E extends Exception> void renameColumn(String columnName, Throwables.Function<String, String, E> func) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param func
     * @throws E the e
     */
    <E extends Exception> void renameColumns(Collection<String> columnNames, Throwables.Function<String, String, E> func) throws E;

    /**
     *
     * @param <E>
     * @param func
     * @throws E the e
     */
    <E extends Exception> void renameColumns(Throwables.Function<String, String, E> func) throws E;

    /**
     *
     * @param columnName
     * @param newPosition
     */
    void moveColumn(String columnName, int newPosition);

    /**
     *
     * @param columnNameNewPositionMap
     */
    void moveColumns(Map<String, Integer> columnNameNewPositionMap);

    /**
     * Swap the positions of the two specified columns.
     *
     * @param columnNameA
     * @param columnNameB
     */
    void swapColumns(String columnNameA, String columnNameB);

    /**
     * Move the specified row to the new position.
     *
     * @param rowIndex
     * @param newRowIndex
     */
    void moveRow(int rowIndex, int newRowIndex);

    /**
     * Swap the positions of the two specified rows.
     *
     * @param rowIndexA
     * @param rowIndexB
     */
    void swapRows(int rowIndexA, int rowIndexB);

    /**
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T>
     * @param rowIndex
     * @param columnIndex
     * @return
     */
    <T> T get(int rowIndex, int columnIndex);

    /**
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T>
     * @param targetType
     * @param rowIndex
     * @param columnIndex
     * @return
     * @deprecated may be misused because it implies there is an underline auto-conversion from column values to target return type but actually there is not.
     */
    @Deprecated
    <T> T get(Class<T> targetType, int rowIndex, int columnIndex);

    /**
     *
     * @param rowIndex
     * @param columnIndex
     * @param element
     */
    void set(int rowIndex, int columnIndex, Object element);

    /**
     * Checks if is null.
     *
     * @param rowIndex
     * @param columnIndex
     * @return true, if is null
     */
    boolean isNull(int rowIndex, int columnIndex);

    /**
     *  
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T>
     * @param columnIndex
     * @return
     */
    <T> T get(int columnIndex);

    /**
     *  
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T>
     * @param columnName
     * @return
     */
    <T> T get(String columnName);

    /**
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T>
     * @param targetType
     * @param columnIndex
     * @return
     * @deprecated may be misused because it implies there is an underline auto-conversion from column values to target return type but actually there is not.
     */
    @Deprecated
    <T> T get(Class<T> targetType, int columnIndex);

    /**
     *  
     * There is NO underline auto-conversion from column value to target type: {@code T}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param <T>
     * @param targetType
     * @param columnName
     * @return
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
     * @param <T>
     * @param columnIndex
     * @param defaultValue
     * @return
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
     * @param <T>
     * @param columnName
     * @param defaultValue
     * @return
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
     * @param columnIndex
     * @return
     */
    boolean getBoolean(int columnIndex);

    /**
     * Return default value (false) if the property is null.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Boolean}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param columnName
     * @return
     */
    boolean getBoolean(String columnName);

    /**
     * Return default value (0) if the property is null.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Character}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param columnIndex
     * @return
     */
    char getChar(int columnIndex);

    /**
     * Return default value (0) if the property is null.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Character}.
     * So the column values must be the type which is assignable to target type.
     *
     * @param columnName
     * @return
     */
    char getChar(String columnName);

    /**
     * Return default value (0) if the property is null. Return Number.byteValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Byte}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex
     * @return
     */
    byte getByte(int columnIndex);

    /**
     * Return default value (0) if the property is null. Return Number.byteValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Byte}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName
     * @return
     */
    byte getByte(String columnName);

    /**
     * Return default value (0) if the property is null. Return Number.shortValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Short}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex
     * @return
     */
    short getShort(int columnIndex);

    /**
     * Return default value (0) if the property is null. Return Number.shortValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Short}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName
     * @return
     */
    short getShort(String columnName);

    /**
     * Return default value (0) if the property is null. Return Number.intValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Integer}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex
     * @return
     */
    int getInt(int columnIndex);

    /**
     * Return default value (0) if the property is null. Return Number.intValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Integer}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName
     * @return
     */
    int getInt(String columnName);

    /**
     * Return default value (0) if the property is null. Return Number.longValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Long}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex
     * @return
     */
    long getLong(int columnIndex);

    /**
     * Return default value (0) if the property is null. Return Number.longValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Long}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName
     * @return
     */
    long getLong(String columnName);

    /**
     * Return default value (0f) if the property is null. Return Number.floatValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Float}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex
     * @return
     */
    float getFloat(int columnIndex);

    /**
     * Return default value (0f) if the property is null. Return Number.floatValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Float}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName
     * @return
     */
    float getFloat(String columnName);

    /**
     * Return default value (0d) if the property is null. Return Number.doubleValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Double}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnIndex
     * @return
     */
    double getDouble(int columnIndex);

    /**
     * Return default value (0d) if the property is null. Return Number.doubleValue() otherwise.
     * <br />
     * There is NO underline auto-conversion from column value to target type: {@code Double}.
     * So the column values must be the type which is assignable to target type, or {@code Number}.
     *
     * @param columnName
     * @return
     */
    double getDouble(String columnName);

    /**
     * Checks if is null.
     *
     * @param columnIndex
     * @return true, if is null
     */
    boolean isNull(int columnIndex);

    /**
     * Checks if is null.
     *
     * @param columnName
     * @return true, if is null
     */
    boolean isNull(String columnName);

    /**
     *
     * @param columnIndex
     * @param value
     */
    void set(int columnIndex, Object value);

    /**
     *
     * @param columnName
     * @param value
     */
    void set(String columnName, Object value);

    /**
     * Must NOT modify the returned list.
     *
     * @param <T>
     * @param columnIndex
     * @return
     */
    <T> ImmutableList<T> getColumn(int columnIndex);

    /**
     * Must NOT modify the returned list.
     *
     * @param <T>
     * @param columnName
     * @return
     */
    <T> ImmutableList<T> getColumn(String columnName);

    /**
     * Copy of column.
     *
     * @param <T>
     * @param columnName
     * @return
     */
    <T> List<T> copyOfColumn(String columnName);

    /**
     *
     * @param columnName
     * @param column
     */
    void addColumn(String columnName, List<?> column);

    /**
     *
     * @param columnIndex position to add.
     * @param columnName
     * @param column
     */
    void addColumn(int columnIndex, String columnName, List<?> column);

    /**
     * Generate the new column values from the specified column by the specified <code>Function</code>.
     *
     * @param <T>
     * @param <E>
     * @param newColumnName
     * @param fromColumnName
     * @param func
     * @throws E the e
     */
    <T, E extends Exception> void addColumn(String newColumnName, String fromColumnName, Throwables.Function<T, ?, E> func) throws E;

    /**
     * Generate the new column values from the specified column by the specified <code>Function</code>.
     *
     * @param <T>
     * @param <E>
     * @param columnIndex
     * @param newColumnName
     * @param fromColumnName
     * @param func
     * @throws E the e
     */
    <T, E extends Exception> void addColumn(int columnIndex, String newColumnName, String fromColumnName, Throwables.Function<T, ?, E> func) throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E>
     * @param newColumnName
     * @param fromColumnNames
     * @param func DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void addColumn(String newColumnName, Collection<String> fromColumnNames, Throwables.Function<? super DisposableObjArray, ?, E> func)
            throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E>
     * @param columnIndex
     * @param newColumnName
     * @param fromColumnNames
     * @param func DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void addColumn(int columnIndex, String newColumnName, Collection<String> fromColumnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> func) throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E>
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    <E extends Exception> void addColumn(String newColumnName, Tuple2<String, String> fromColumnNames, Throwables.BiFunction<?, ?, ?, E> func) throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E>
     * @param columnIndex
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    <E extends Exception> void addColumn(int columnIndex, String newColumnName, Tuple2<String, String> fromColumnNames, Throwables.BiFunction<?, ?, ?, E> func)
            throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E>
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    <E extends Exception> void addColumn(String newColumnName, Tuple3<String, String, String> fromColumnNames, Throwables.TriFunction<?, ?, ?, ?, E> func)
            throws E;

    /**
     * Generate the new column values from the specified columns by the specified <code>Function</code>.
     *
     * @param <E>
     * @param columnIndex
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    <E extends Exception> void addColumn(int columnIndex, String newColumnName, Tuple3<String, String, String> fromColumnNames,
            Throwables.TriFunction<?, ?, ?, ?, E> func) throws E;

    /**
     * Remove the column with the specified columnName from this DataSet.
     *
     * @param <T>
     * @param columnName
     * @return
     */
    <T> List<T> removeColumn(String columnName);

    /**
     * Remove the column(s) with the specified columnNames from this DataSet.
     *
     * @param columnNames
     */
    void removeColumns(Collection<String> columnNames);

    /**
     * Remove the column(s) whose name matches the specified {@code filter}.
     *
     * @param <E>
     * @param filter column name filter
     * @throws E the e
     */
    <E extends Exception> void removeColumns(Throwables.Predicate<String, E> filter) throws E;

    /**
     * Remove the column(s) whose name matches the specified {@code filter}.
     *
     * @param <E>
     * @param filter column name filter
     * @throws E the e
     * @deprecated replaced by {@code removeColumns}.
     */
    @Deprecated
    <E extends Exception> void removeColumnsIf(Throwables.Predicate<String, E> filter) throws E;

    /**
     * Update the values of the specified column by the specified Try.Function.
     *
     * @param <T>
     * @param <E>
     * @param columnName
     * @param func
     * @throws E the e
     */
    <T, E extends Exception> void updateColumn(String columnName, Throwables.Function<T, ?, E> func) throws E;

    /**
     * Update the values of the specified columns one by one with the specified Try.Function.
     *
     * @param <T>
     * @param <E>
     * @param columnNames
     * @param func
     * @throws E the e
     */
    <T, E extends Exception> void updateColumns(Collection<String> columnNames, Throwables.Function<?, ?, E> func) throws E;

    /**
     * Convert the specified column to target type.
     *
     * @param columnName
     * @param targetType
     */
    void convertColumn(String columnName, Class<?> targetType);

    /**
     * Convert the specified columns to target types.
     *
     * @param columnTargetTypes
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
     *
     * @param columnNames
     * @param newColumnName
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     */
    void combineColumns(Collection<String> columnNames, String newColumnName, Class<?> newColumnClass);

    /**
     *
     * @param <E>
     * @param columnNames
     * @param newColumnName
     * @param combineFunc DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void combineColumns(Collection<String> columnNames, String newColumnName,
            Throwables.Function<? super DisposableObjArray, ?, E> combineFunc) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param newColumnName
     * @param combineFunc
     * @throws E the e
     */
    <E extends Exception> void combineColumns(Tuple2<String, String> columnNames, String newColumnName, Throwables.BiFunction<?, ?, ?, E> combineFunc) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param newColumnName
     * @param combineFunc
     * @throws E the e
     */
    <E extends Exception> void combineColumns(Tuple3<String, String, String> columnNames, String newColumnName,
            Throwables.TriFunction<?, ?, ?, ?, E> combineFunc) throws E;

    /**
     *
     * @param <E>
     * @param columnNameFilter
     * @param newColumnName
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @throws E the e
     */
    <E extends Exception> void combineColumns(Throwables.Predicate<String, E> columnNameFilter, String newColumnName, Class<?> newColumnClass) throws E;

    /**
     *
     * @param <E>
     * @param <E2>
     * @param columnNameFilter
     * @param newColumnName
     * @param combineFunc DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     * @throws E2 the e2
     */
    <E extends Exception, E2 extends Exception> void combineColumns(Throwables.Predicate<String, E> columnNameFilter, String newColumnName,
            Throwables.Function<? super DisposableObjArray, ?, E2> combineFunc) throws E, E2;

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param divideFunc
     * @throws E the e
     */
    <T, E extends Exception> void divideColumn(String columnName, Collection<String> newColumnNames, Throwables.Function<T, ? extends List<?>, E> divideFunc)
            throws E;

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param output
     * @throws E the e
     */
    <T, E extends Exception> void divideColumn(String columnName, Collection<String> newColumnNames, Throwables.BiConsumer<T, Object[], E> output) throws E;

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param output
     * @throws E the e
     */
    <T, E extends Exception> void divideColumn(String columnName, Tuple2<String, String> newColumnNames,
            Throwables.BiConsumer<T, Pair<Object, Object>, E> output) throws E;

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param output
     * @throws E the e
     */
    <T, E extends Exception> void divideColumn(String columnName, Tuple3<String, String, String> newColumnNames,
            Throwables.BiConsumer<T, Triple<Object, Object, Object>, E> output) throws E;

    /**
     * Adds the row.
     *
     * @param row can be Object[]/List/Map/Entity with getter/setter methods
     */
    void addRow(Object row);

    /**
     * Adds the row.
     *
     * @param rowIndex
     * @param row can be Object[]/List/Map/Entity with getter/setter methods
     */
    void addRow(int rowIndex, Object row);

    /**
     * Removes the row.
     *
     * @param rowIndex
     */
    void removeRow(int rowIndex);

    /**
     * Removes the rows.
     *
     * @param indices
     */
    void removeRows(int... indices);

    /**
     * Removes the row range.
     *
     * @param inclusiveFromRowIndex
     * @param exclusiveToRowIndex
     */
    void removeRowRange(int inclusiveFromRowIndex, int exclusiveToRowIndex);

    /**
     * Update the values in the specified row with the specified Try.Function.
     *
     * @param <E>
     * @param rowIndex
     * @param func
     * @throws E the e
     */
    <E extends Exception> void updateRow(int rowIndex, Throwables.Function<?, ?, E> func) throws E;

    /**
     * Update the values in the specified rows one by one with the specified Try.Function.
     *
     * @param <E>
     * @param indices
     * @param func
     * @throws E the e
     */
    <E extends Exception> void updateRows(int[] indices, Throwables.Function<?, ?, E> func) throws E;

    /**
     * Update all the values in this DataSet with the specified Try.Function.
     *
     * @param <E>
     * @param func
     * @throws E the e
     */
    <E extends Exception> void updateAll(Throwables.Function<?, ?, E> func) throws E;

    /**
     * Replace all the values in this DataSet with the specified new value if it matches the specified condition.
     *
     * @param <E>
     * @param func
     * @param newValue
     * @throws E the e
     */
    <E extends Exception> void replaceIf(Throwables.Predicate<?, E> func, Object newValue) throws E;

    /**
     * Returns the current row number.
     *
     * @return
     */
    int currentRowNum();

    /**
     * Move the cursor to the specified row.
     *
     * @param rowNum
     * @return this object itself.
     */
    DataSet absolute(int rowNum);

    /**
     * Gets the row.
     *
     * @param rowNum
     * @return
     */
    Object[] getRow(int rowNum);

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param rowNum
     * @return
     */
    <T> T getRow(Class<? extends T> rowClass, int rowNum);

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @param rowNum
     * @return
     */
    <T> T getRow(Class<? extends T> rowClass, Collection<String> columnNames, int rowNum);

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param rowNum
     * @return
     */
    <T> T getRow(IntFunction<? extends T> rowSupplier, int rowNum);

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @param rowNum
     * @return
     */
    <T> T getRow(IntFunction<? extends T> rowSupplier, Collection<String> columnNames, int rowNum);

    /**
     *
     * @return {@code Optional<Object[]>}
     */
    Optional<Object[]> firstRow();

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @return {@code Optional<E>}
     */
    <T> Optional<T> firstRow(Class<? extends T> rowClass);

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @return {@code Optional<E>}
     */
    <T> Optional<T> firstRow(Class<? extends T> rowClass, Collection<String> columnNames);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @return {@code Optional<T>}
     */
    <T> Optional<T> firstRow(IntFunction<? extends T> rowSupplier);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @return {@code Optional<T>}
     */
    <T> Optional<T> firstRow(IntFunction<? extends T> rowSupplier, Collection<String> columnNames);

    /**
     *
     * @return {@code Optional<Object[]>}
     */
    Optional<Object[]> lastRow();

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @return {@code Optional<E>}
     */
    <T> Optional<T> lastRow(Class<? extends T> rowClass);

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     *            which can be object array/list/set/map/entity.
     * @param columnNames
     * @return {@code Optional<E>}
     */
    <T> Optional<T> lastRow(Class<? extends T> rowClass, Collection<String> columnNames);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @return {@code Optional<T>}
     */
    <T> Optional<T> lastRow(IntFunction<? extends T> rowSupplier);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @return {@code Optional<T>}
     */
    <T> Optional<T> lastRow(IntFunction<? extends T> rowSupplier, Collection<String> columnNames);

    /**
     * Performs the given action for each row of the {@code DataSet}
     * until all rows have been processed or the action throws an
     * exception.
     *
     * @param <E>
     * @param action DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void forEach(Throwables.Consumer<? super DisposableObjArray, E> action) throws E;

    /**
     * Performs the given action for each row of the {@code DataSet}
     * until all rows have been processed or the action throws an
     * exception.
     *
     * @param <E>
     * @param columnNames
     * @param action DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void forEach(Collection<String> columnNames, Throwables.Consumer<? super DisposableObjArray, E> action) throws E;

    /**
     * Performs the given action for each row of the {@code DataSet}
     * until all rows have been processed or the action throws an
     * exception.
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param action DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Throwables.Consumer<? super DisposableObjArray, E> action) throws E;

    /**
     * Performs the given action for each row of the {@code DataSet}
     * until all rows have been processed or the action throws an
     * exception.
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param action DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void forEach(Collection<String> columnNames, int fromRowIndex, int toRowIndex,
            Throwables.Consumer<? super DisposableObjArray, E> action) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param action
     * @throws E the e
     */
    <E extends Exception> void forEach(Tuple2<String, String> columnNames, Throwables.BiConsumer<?, ?, E> action) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param action
     * @throws E the e
     */
    <E extends Exception> void forEach(Tuple2<String, String> columnNames, int fromRowIndex, int toRowIndex, Throwables.BiConsumer<?, ?, E> action) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param action
     * @throws E the e
     */
    <E extends Exception> void forEach(Tuple3<String, String, String> columnNames, Throwables.TriConsumer<?, ?, ?, E> action) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param action
     * @throws E the e
     */
    <E extends Exception> void forEach(Tuple3<String, String, String> columnNames, int fromRowIndex, int toRowIndex, Throwables.TriConsumer<?, ?, ?, E> action)
            throws E;

    List<Object[]> toList();

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    List<Object[]> toList(int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @return
     */
    <T> List<T> toList(Class<? extends T> rowClass);

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <T> List<T> toList(Class<? extends T> rowClass, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @return
     */
    <T> List<T> toList(Class<? extends T> rowClass, Collection<String> columnNames);

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <T> List<T> toList(Class<? extends T> rowClass, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnFilter
     * @param columnConverter
     * @return
     */
    <T, E extends Exception, E2 extends Exception> List<T> toList(Class<? extends T> rowClass, Throwables.Predicate<? super String, E> columnFilter,
            Throwables.Function<? super String, String, E2> columnConverter) throws E, E2;

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnFilter
     * @param columnConverter
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <T, E extends Exception, E2 extends Exception> List<T> toList(Class<? extends T> rowClass, Throwables.Predicate<? super String, E> columnFilter,
            Throwables.Function<? super String, String, E2> columnConverter, int fromRowIndex, int toRowIndex) throws E, E2;

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @return
     */
    <T> List<T> toList(IntFunction<? extends T> rowSupplier);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <T> List<T> toList(IntFunction<? extends T> rowSupplier, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @return
     */
    <T> List<T> toList(IntFunction<? extends T> rowSupplier, Collection<String> columnNames);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <T> List<T> toList(IntFunction<? extends T> rowSupplier, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyColumnName
     * @param valueColumnName
     * @return
     */
    <K, V> Map<K, V> toMap(String keyColumnName, String valueColumnName);

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyColumnName
     * @param valueColumnName
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <K, V> Map<K, V> toMap(String keyColumnName, String valueColumnName, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param keyColumnName
     * @param valueColumnName
     * @param fromRowIndex
     * @param toRowIndex
     * @param supplier
     * @return
     */
    <K, V, M extends Map<K, V>> M toMap(String keyColumnName, String valueColumnName, int fromRowIndex, int toRowIndex, IntFunction<? extends M> supplier);

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @return
     */
    <K, V> Map<K, V> toMap(Class<? extends V> rowClass, String keyColumnName, Collection<String> valueColumnNames);

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <K, V> Map<K, V> toMap(Class<? extends V> rowClass, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param supplier
     * @return
     */
    <K, V, M extends Map<K, V>> M toMap(Class<? extends V> rowClass, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex,
            int toRowIndex, IntFunction<? extends M> supplier);

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @return
     */
    <K, V> Map<K, V> toMap(IntFunction<? extends V> rowSupplier, String keyColumnName, Collection<String> valueColumnNames);

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <K, V> Map<K, V> toMap(IntFunction<? extends V> rowSupplier, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param supplier
     * @return
     */
    <K, V, M extends Map<K, V>> M toMap(IntFunction<? extends V> rowSupplier, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex,
            int toRowIndex, IntFunction<? extends M> supplier);

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param keyColumnName
     * @param valueColumnName
     * @return
     */
    <K, E> ListMultimap<K, E> toMultimap(String keyColumnName, String valueColumnName);

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param keyColumnName
     * @param valueColumnName
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <K, E> ListMultimap<K, E> toMultimap(String keyColumnName, String valueColumnName, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param keyColumnName
     * @param valueColumnName
     * @param fromRowIndex
     * @param toRowIndex
     * @param supplier
     * @return
     */
    <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(String keyColumnName, String valueColumnName, int fromRowIndex, int toRowIndex,
            IntFunction<? extends M> supplier);

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @return
     */
    <K, E> ListMultimap<K, E> toMultimap(Class<? extends E> rowClass, String keyColumnName, Collection<String> valueColumnNames);

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <K, E> ListMultimap<K, E> toMultimap(Class<? extends E> rowClass, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex,
            int toRowIndex);

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param supplier
     * @return
     */
    <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(Class<? extends E> rowClass, String keyColumnName,
            Collection<String> valueColumnNames, int fromRowIndex, int toRowIndex, IntFunction<? extends M> supplier);

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @return
     */
    <K, E> ListMultimap<K, E> toMultimap(IntFunction<? extends E> rowSupplier, String keyColumnName, Collection<String> valueColumnNames);

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <K, E> ListMultimap<K, E> toMultimap(IntFunction<? extends E> rowSupplier, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex,
            int toRowIndex);

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param supplier
     * @return
     */
    <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(IntFunction<? extends E> rowSupplier, String keyColumnName,
            Collection<String> valueColumnNames, int fromRowIndex, int toRowIndex, IntFunction<? extends M> supplier);

    /**
     * 
     * @param <T>
     * @param entityClass
     * @return
     */
    <T> List<T> toMergedEntities(Class<T> entityClass);

    /**
     * 
     * @param <T>
     * @param entityClass
     * @param selectPropNames
     * @return
     */
    <T> List<T> toMergedEntities(Class<T> entityClass, Collection<String> selectPropNames);

    /**
     * 
     * @param <T>
     * @param entityClass
     * @param idPropName
     * @return
     */
    <T> List<T> toMergedEntities(Class<T> entityClass, String idPropName);

    /**
     * 
     * @param <T>
     * @param entityClass
     * @param idPropName
     * @param selectPropNames
     * @return
     */
    <T> List<T> toMergedEntities(Class<T> entityClass, String idPropName, Collection<String> selectPropNames);

    /**
     * 
     * @param <T>
     * @param entityClass
     * @param idPropNames
     * @param selectPropNames
     * @return
     */
    <T> List<T> toMergedEntities(Class<T> entityClass, List<String> idPropNames, Collection<String> selectPropNames);

    String toJSON();

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    String toJSON(int fromRowIndex, int toRowIndex);

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    String toJSON(Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(File out) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(File out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(File out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(OutputStream out) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(OutputStream out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(OutputStream out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(Writer out) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(Writer out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJSON(Writer out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    String toXML();

    /**
     *
     * @param rowElementName
     * @return
     */
    String toXML(String rowElementName);

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    String toXML(int fromRowIndex, int toRowIndex);

    /**
     *
     * @param rowElementName
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    String toXML(String rowElementName, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    String toXML(Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param rowElementName
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    String toXML(String rowElementName, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param rowElementName
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out, String rowElementName) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param rowElementName
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out, String rowElementName, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param rowElementName
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(File out, String rowElementName, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param rowElementName
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out, String rowElementName) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param rowElementName
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out, String rowElementName, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param rowElementName
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(OutputStream out, String rowElementName, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param rowElementName
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out, String rowElementName) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param rowElementName
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out, String rowElementName, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param rowElementName
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXML(Writer out, String rowElementName, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    String toCSV();

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    String toCSV(Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param writeTitle
     * @param quoteValue
     * @return
     */
    String toCSV(boolean writeTitle, boolean quoteValue);

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param writeTitle
     * @param quoteValue
     * @return
     */
    String toCSV(Collection<String> columnNames, int fromRowIndex, int toRowIndex, boolean writeTitle, boolean quoteValue);

    /**
     *
     * @param out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(File out) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(File out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param writeTitle
     * @param quoteValue
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(File out, boolean writeTitle, boolean quoteValue) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param writeTitle
     * @param quoteValue
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(File out, Collection<String> columnNames, int fromRowIndex, int toRowIndex, boolean writeTitle, boolean quoteValue) throws UncheckedIOException;

    /**
     *
     * @param out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(OutputStream out);

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(OutputStream out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param writeTitle
     * @param quoteValue
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(OutputStream out, boolean writeTitle, boolean quoteValue) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param writeTitle
     * @param quoteValue
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(OutputStream out, Collection<String> columnNames, int fromRowIndex, int toRowIndex, boolean writeTitle, boolean quoteValue)
            throws UncheckedIOException;

    /**
     *
     * @param out
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(Writer out);

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(Writer out, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param writeTitle
     * @param quoteValue
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(Writer out, boolean writeTitle, boolean quoteValue) throws UncheckedIOException;

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param writeTitle
     * @param quoteValue
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCSV(Writer out, Collection<String> columnNames, int fromRowIndex, int toRowIndex, boolean writeTitle, boolean quoteValue)
            throws UncheckedIOException;

    /**
     *
     * @param columnName specifying the column to group by.
     * @return
     * @deprecated
     */
    @Deprecated
    DataSet groupBy(String columnName);

    /**
     *
     * @param <T>
     * @param columnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    <T> DataSet groupBy(String columnName, String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param func
     * @return
     * @throws E the e
     */
    <T, E extends Exception> DataSet groupBy(String columnName, String aggregateResultColumnName, String aggregateOnColumnName,
            Throwables.Function<Stream<T>, ?, E> func) throws E;

    /**
     *
     * @param columnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowClass
     * @return
     */
    DataSet groupBy(String columnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowClass);

    /**
     *
     * @param columnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    DataSet groupBy(String columnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     *
     * @param <U>
     * @param <E>
     * @param columnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     * @throws E the e
     */
    <U, E extends Exception> DataSet groupBy(String columnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Throwables.Function<? super DisposableObjArray, U, E> rowMapper, Collector<? super U, ?, ?> collector) throws E;

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param columnName
     * @param keyMapper
     * @return
     * @throws E the e
     * @deprecated
     */
    @Deprecated
    <K, E extends Exception> DataSet groupBy(String columnName, Throwables.Function<K, ?, E> keyMapper) throws E;

    /**
     *
     * @param <K> the key type
     * @param <T>
     * @param <E>
     * @param columnName
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     * @throws E the e
     */
    <K, T, E extends Exception> DataSet groupBy(String columnName, Throwables.Function<K, ?, E> keyMapper, String aggregateResultColumnName,
            String aggregateOnColumnName, Collector<T, ?, ?> collector) throws E;

    /**
     *
     * @param <K> the key type
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param columnName
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param func
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    <K, T, E extends Exception, E2 extends Exception> DataSet groupBy(String columnName, Throwables.Function<K, ?, E> keyMapper,
            String aggregateResultColumnName, String aggregateOnColumnName, Throwables.Function<Stream<T>, ?, E2> func) throws E, E2;

    /**
    *
    * @param <K> the key type
    * @param <E>
    * @param columnName
    * @param keyMapper
    * @param aggregateResultColumnName
    * @param aggregateOnColumnNames
    * @param rowClass
    * @return
    * @throws E the e
    */
    <K, E extends Exception> DataSet groupBy(String columnName, Throwables.Function<K, ?, E> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Class<?> rowClass) throws E;

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param columnName
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     * @throws E the e
     */
    <K, E extends Exception> DataSet groupBy(String columnName, Throwables.Function<K, ?, E> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Collector<? super Object[], ?, ?> collector) throws E;

    /**
     *
     * @param <K> the key type
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param columnName
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    <K, U, E extends Exception, E2 extends Exception> DataSet groupBy(String columnName, Throwables.Function<K, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Throwables.Function<? super DisposableObjArray, U, E2> rowMapper,
            Collector<? super U, ?, ?> collector) throws E, E2;

    /**
     *
     * @param columnNames
     * @return 
     */
    DataSet groupBy(Collection<String> columnNames);

    /**
     *
     * @param <T>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    <T> DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param func
     * @return
     * @throws E the e
     */
    <T, E extends Exception> DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName,
            Throwables.Function<Stream<T>, ?, E> func) throws E;

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowClass
     * @return
     */
    DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowClass);

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     *
     * @param <U>
     * @param <E>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     * @throws E the e
     */
    <U, E extends Exception> DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Throwables.Function<? super DisposableObjArray, U, E> rowMapper, Collector<? super U, ?, ?> collector) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet groupBy(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper) throws E;

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     * @throws E the e
     */
    <T, E extends Exception> DataSet groupBy(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector) throws E;

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param func
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    <T, E extends Exception, E2 extends Exception> DataSet groupBy(Collection<String> columnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, String aggregateOnColumnName,
            Throwables.Function<Stream<T>, ?, E2> func) throws E, E2;

    /**
    *
    * @param <E>
    * @param columnNames
    * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    * @param aggregateResultColumnName
    * @param aggregateOnColumnNames
    * @param rowClass
    * @return
    * @throws E the e
    */
    <E extends Exception> DataSet groupBy(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowClass) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet groupBy(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Collector<? super Object[], ?, ?> collector) throws E;

    /**
     *
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    <U, E extends Exception, E2 extends Exception> DataSet groupBy(Collection<String> columnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Throwables.Function<? super DisposableObjArray, U, E2> rowMapper, Collector<? super U, ?, ?> collector) throws E, E2;

    /**
     *
     * @param columnNames
     * @return
     */
    Stream<DataSet> rollup(Collection<String> columnNames);

    /**
     *
     * @param <T>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    <T> Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param func
     * @return
     */
    <T, E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName,
            Throwables.Function<Stream<T>, ?, E> func);

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowClass
     * @return
     */
    Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowClass);

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     *
     * @param <U>
     * @param <E>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     */
    <U, E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Throwables.Function<? super DisposableObjArray, U, E> rowMapper, Collector<? super U, ?, ?> collector);

    /**
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     */
    <E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper);

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    <T, E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param func
     * @return
     */
    <T, E extends Exception, E2 extends Exception> Stream<DataSet> rollup(Collection<String> columnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, String aggregateOnColumnName,
            Throwables.Function<Stream<T>, ?, E2> func);

    /**
    *
    * @param <E>
    * @param columnNames
    * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    * @param aggregateResultColumnName
    * @param aggregateOnColumnNames
    * @param rowClass
    * @return
    */
    <E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowClass);

    /**
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    <E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Collector<? super Object[], ?, ?> collector);

    /**
     *
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     */
    <U, E extends Exception, E2 extends Exception> Stream<DataSet> rollup(Collection<String> columnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Throwables.Function<? super DisposableObjArray, U, E2> rowMapper, Collector<? super U, ?, ?> collector);

    /**
     *
     * @param columnNames
     * @return
     */
    Stream<DataSet> cube(Collection<String> columnNames);

    /**
     *
     * @param <T>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    <T> Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param func
     * @return
     */
    <T, E extends Exception> Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName,
            Throwables.Function<Stream<T>, ?, E> func);

    /**
    *
    * @param columnNames
    * @param aggregateResultColumnName
    * @param aggregateOnColumnNames
    * @param rowClass
    * @return
    */
    Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowClass);

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     *
     * @param <U>
     * @param <E>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     */
    <U, E extends Exception> Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Throwables.Function<? super DisposableObjArray, U, E> rowMapper, Collector<? super U, ?, ?> collector);

    /**
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     */
    <E extends Exception> Stream<DataSet> cube(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper);

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    <T, E extends Exception> Stream<DataSet> cube(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, String aggregateOnColumnName, Collector<T, ?, ?> collector);

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param func
     * @return
     */
    <T, E extends Exception, E2 extends Exception> Stream<DataSet> cube(Collection<String> columnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, String aggregateOnColumnName,
            Throwables.Function<Stream<T>, ?, E2> func);

    /**
    *
    * @param <E>
    * @param columnNames
    * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    * @param aggregateResultColumnName
    * @param aggregateOnColumnNames
    * @param rowClass
    * @return
    */
    <E extends Exception> Stream<DataSet> cube(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowClass);

    /**
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    <E extends Exception> Stream<DataSet> cube(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Collector<? super Object[], ?, ?> collector);

    /**
     *
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     */
    <U, E extends Exception, E2 extends Exception> Stream<DataSet> cube(Collection<String> columnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Throwables.Function<? super DisposableObjArray, U, E2> rowMapper, Collector<? super U, ?, ?> collector);

    /**
     *
     * @param columnName
     */
    void sortBy(String columnName);

    /**
     *
     * @param <T>
     * @param columnName
     * @param cmp
     */
    <T> void sortBy(String columnName, Comparator<T> cmp);

    /**
     *
     * @param columnNames
     */
    void sortBy(Collection<String> columnNames);

    /**
     *
     * @param columnNames
     * @param cmp
     */
    void sortBy(Collection<String> columnNames, Comparator<? super Object[]> cmp);

    /**
     *
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     */
    @SuppressWarnings("rawtypes")
    void sortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyMapper);

    /**
     * Parallel sort by.
     *
     * @param columnName
     */
    void parallelSortBy(String columnName);

    /**
     * Parallel sort by.
     *
     * @param <T>
     * @param columnName
     * @param cmp
     */
    <T> void parallelSortBy(String columnName, Comparator<T> cmp);

    /**
     * Parallel sort by.
     *
     * @param columnNames
     */
    void parallelSortBy(Collection<String> columnNames);

    /**
     * Parallel sort by.
     *
     * @param columnNames
     * @param cmp
     */
    void parallelSortBy(Collection<String> columnNames, Comparator<? super Object[]> cmp);

    /**
     * Parallel sort by.
     *
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     */
    @SuppressWarnings("rawtypes")
    void parallelSortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyMapper);

    /**
     *
     * @param columnName
     * @param n
     * @return
     */
    DataSet topBy(String columnName, int n);

    /**
     *
     * @param <T>
     * @param columnName
     * @param n
     * @param cmp
     * @return
     */
    <T> DataSet topBy(String columnName, int n, Comparator<T> cmp);

    /**
     *
     * @param columnNames
     * @param n
     * @return
     */
    DataSet topBy(Collection<String> columnNames, int n);

    /**
     *
     * @param columnNames
     * @param n
     * @param cmp
     * @return
     */
    DataSet topBy(Collection<String> columnNames, int n, Comparator<? super Object[]> cmp);

    /**
     *
     * @param columnNames
     * @param n
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
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
     * @param columnName
     * @return a new DataSet
     */
    DataSet distinctBy(String columnName);

    /**
     * Returns a new <code>DataSet</code> with the rows de-duplicated by the value in the specified column from the specified <code>fromRowIndex</code> to <code>toRowIndex</code>.
     *
     * @param <K> the key type
     * @param <E>
     * @param columnName
     * @param keyMapper don't change value of the input parameter.
     * @return
     * @throws E the e
     */
    <K, E extends Exception> DataSet distinctBy(String columnName, Throwables.Function<K, ?, E> keyMapper) throws E;

    /**
     * Returns a new <code>DataSet</code> with the rows de-duplicated by the values in the specified columns.
     *
     * @param columnNames
     * @return a new DataSet
     */
    DataSet distinctBy(Collection<String> columnNames);

    /**
     * Returns a new <code>DataSet</code> with the rows de-duplicated by the values in the specified columns from the specified <code>fromRowIndex</code> to <code>toRowIndex</code>.
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet distinctBy(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper) throws E;

    /**
     *
     * @param <E>
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Throwables.Predicate<? super DisposableObjArray, E> filter) throws E;

    /**
     *
     * @param <E>
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Throwables.Predicate<? super DisposableObjArray, E> filter, int max) throws E;

    /**
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(int fromRowIndex, int toRowIndex, Throwables.Predicate<? super DisposableObjArray, E> filter) throws E;

    /**
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(int fromRowIndex, int toRowIndex, Throwables.Predicate<? super DisposableObjArray, E> filter, int max) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, Throwables.BiPredicate<?, ?, E> filter) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, Throwables.BiPredicate<?, ?, E> filter, int max) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, int fromRowIndex, int toRowIndex, Throwables.BiPredicate<?, ?, E> filter) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, int fromRowIndex, int toRowIndex, Throwables.BiPredicate<?, ?, E> filter, int max)
            throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple3<String, String, String> columnNames, Throwables.TriPredicate<?, ?, ?, E> filter) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple3<String, String, String> columnNames, Throwables.TriPredicate<?, ?, ?, E> filter, int max) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple3<String, String, String> columnNames, int fromRowIndex, int toRowIndex,
            Throwables.TriPredicate<?, ?, ?, E> filter) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Tuple3<String, String, String> columnNames, int fromRowIndex, int toRowIndex,
            Throwables.TriPredicate<?, ?, ?, E> filter, int max) throws E;

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnName
     * @param filter
     * @return
     * @throws E the e
     */
    <T, E extends Exception> DataSet filter(String columnName, Throwables.Predicate<T, E> filter) throws E;

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnName
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    <T, E extends Exception> DataSet filter(String columnName, Throwables.Predicate<T, E> filter, int max) throws E;

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnName
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter
     * @return
     * @throws E the e
     */
    <T, E extends Exception> DataSet filter(String columnName, int fromRowIndex, int toRowIndex, Throwables.Predicate<T, E> filter) throws E;

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnName
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    <T, E extends Exception> DataSet filter(String columnName, int fromRowIndex, int toRowIndex, Throwables.Predicate<T, E> filter, int max) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Collection<String> columnNames, Throwables.Predicate<? super DisposableObjArray, E> filter) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Collection<String> columnNames, Throwables.Predicate<? super DisposableObjArray, E> filter, int max) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Collection<String> columnNames, int fromRowIndex, int toRowIndex,
            Throwables.Predicate<? super DisposableObjArray, E> filter) throws E;

    /**
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet filter(Collection<String> columnNames, int fromRowIndex, int toRowIndex,
            Throwables.Predicate<? super DisposableObjArray, E> filter, int max) throws E;

    /**
     *
     * @param <E>
     * @param fromColumnName
     * @param func
     * @param newColumnName
     * @param copyingColumnName
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet map(String fromColumnName, Throwables.Function<?, ?, E> func, String newColumnName, String copyingColumnName) throws E;

    /**
     *
     * @param <E>
     * @param fromColumnName
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet map(String fromColumnName, Throwables.Function<?, ?, E> func, String newColumnName, Collection<String> copyingColumnNames)
            throws E;

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet map(Tuple2<String, String> fromColumnNames, Throwables.BiFunction<?, ?, ?, E> func, String newColumnName,
            Collection<String> copyingColumnNames) throws E;

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet map(Tuple3<String, String, String> fromColumnNames, Throwables.TriFunction<?, ?, ?, ?, E> func, String newColumnName,
            Collection<String> copyingColumnNames) throws E;

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param func DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet map(Collection<String> fromColumnNames, Throwables.Function<DisposableObjArray, ?, E> func, String newColumnName,
            Collection<String> copyingColumnNames) throws E;

    /**
     *
     * @param <E>
     * @param fromColumnName
     * @param func
     * @param newColumnName
     * @param copyingColumnName
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet flatMap(String fromColumnName, Throwables.Function<?, ? extends Collection<?>, E> func, String newColumnName,
            String copyingColumnName) throws E;

    /**
     *
     * @param <E>
     * @param fromColumnName
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet flatMap(String fromColumnName, Throwables.Function<?, ? extends Collection<?>, E> func, String newColumnName,
            Collection<String> copyingColumnNames) throws E;

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet flatMap(Tuple2<String, String> fromColumnNames, Throwables.BiFunction<?, ?, ? extends Collection<?>, E> func,
            String newColumnName, Collection<String> copyingColumnNames) throws E;

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet flatMap(Tuple3<String, String, String> fromColumnNames, Throwables.TriFunction<?, ?, ?, ? extends Collection<?>, E> func,
            String newColumnName, Collection<String> copyingColumnNames) throws E;

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param func DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    <E extends Exception> DataSet flatMap(Collection<String> fromColumnNames, Throwables.Function<DisposableObjArray, ? extends Collection<?>, E> func,
            String newColumnName, Collection<String> copyingColumnNames) throws E;

    /**
     * Returns a new <code>DataSet</code> that is limited to the rows where there is a match in both <code>this DataSet</code> and <code>right DataSet</code>.
     *
     * @param right
     * @param columnName
     * @param refColumnName
     * @return a new DataSet
     */
    DataSet innerJoin(DataSet right, String columnName, String refColumnName);

    /**
     * Returns a new <code>DataSet</code> that is limited to the rows where there is a match in both <code>this DataSet</code> and <code>right DataSet</code>.
     *
     * @param right
     * @param onColumnNames
     * @return a new DataSet
     */
    DataSet innerJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new <code>DataSet</code> that is limited to the rows where there is a match in both <code>this DataSet</code> and <code>right DataSet</code>.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @return a new DataSet
     */
    DataSet innerJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass);

    /**
     * Returns a new <code>DataSet</code> that is limited to the rows where there is a match in both <code>this DataSet</code> and <code>right DataSet</code>.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
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
     * @param right
     * @param columnName
     * @param refColumnName
     * @return a new DataSet
     */
    DataSet leftJoin(DataSet right, String columnName, String refColumnName);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the rows from the specified <code>right DataSet</code> if they have a match with the rows from the this <code>DataSet</code>.
     *
     * @param right
     * @param onColumnNames
     * @return a new DataSet
     */
    DataSet leftJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the rows from the specified <code>right DataSet</code> if they have a match with the rows from the this <code>DataSet</code>.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @return a new DataSet
     */
    DataSet leftJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the rows from the specified <code>right DataSet</code> if they have a match with the rows from the this <code>DataSet</code>.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
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
     * @param right
     * @param columnName
     * @param refColumnName
     * @return a new DataSet
     */
    DataSet rightJoin(DataSet right, String columnName, String refColumnName);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from the specified right <code>DataSet</code> and the rows from <code>this DataSet</code> if they have a match with the rows from the right <code>DataSet</code>.
     *
     * @param right
     * @param onColumnNames
     * @return a new DataSet
     */
    DataSet rightJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from the specified right <code>DataSet</code> and the rows from <code>this DataSet</code> if they have a match with the rows from the right <code>DataSet</code>.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @return a new DataSet
     */
    DataSet rightJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from the specified right <code>DataSet</code> and the rows from <code>this DataSet</code> if they have a match with the rows from the right <code>DataSet</code>.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
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
     * @param right
     * @param columnName
     * @param refColumnName
     * @return a new DataSet
     */
    DataSet fullJoin(DataSet right, String columnName, String refColumnName);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the specified <code>right DataSet</code>, regardless of whether there are any matches.
     *
     * @param right
     * @param onColumnNames
     * @return a new DataSet
     */
    DataSet fullJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the specified <code>right DataSet</code>, regardless of whether there are any matches.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnClass it can be Object[]/List/Set/Map/Entity
     * @return a new DataSet
     */
    DataSet fullJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnClass);

    /**
     * Returns a new <code>DataSet</code> that has all the rows from this <code>DataSet</code> and the specified <code>right DataSet</code>, regardless of whether there are any matches.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
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
     * @param dataSet
     * @return a new DataSet
     */
    DataSet union(DataSet dataSet);

    /**
     * Returns a new <code>DataSet</code>. Duplicated row in the specified {@code DataSet} will not be eliminated.
     *
     * @param dataSet
     * @return a new DataSet
     */
    DataSet unionAll(DataSet dataSet);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
     * This operation removes duplicate rows from the final result set.
     *
     * @param other
     * @return 
     */
    DataSet intersect(DataSet other);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
     * This operation doesn't remove duplicate rows from the final result set.
     *
     * @param other
     * @return
     * @see java.util.Collection#retainAll(Collection)
     */
    DataSet intersectAll(DataSet other);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns by occurrences.
     * This operation only cares the occurrences of rows in both {@code DataSet}. it doesn't remove duplicate.
     *
     * @param dataSet
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#intersection(com.landawn.abacus.util.IntList)
     */
    DataSet intersection(DataSet dataSet);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns.
     *
     * @param other
     * @return
     * @see java.util.Collection#removeAll(Collection)
     */
    DataSet except(DataSet other);

    /**
     * Returns a new <code>DataSet</code>.
     *
     * @param dataSet
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#difference(com.landawn.abacus.util.IntList)
     */
    DataSet difference(DataSet dataSet);

    /**
     *
     * @param dataSet
     * @return
     * @see com.landawn.abacus.util.IntList#symmetricDifference(com.landawn.abacus.util.IntList)
     */
    DataSet symmetricDifference(DataSet dataSet);

    /**
     * Returns a new <code>DataSet</code> by appending the specified <code>from</code> <code>DataSet</code> into this <code>DataSet</code>.
     *
     * @param from
     * @return
     */
    DataSet merge(DataSet from);

    /**
     * Returns a new <code>DataSet</code> by appending the specified <code>from</code> <code>DataSet</code> into this <code>DataSet</code>.
     *
     * @param from
     * @param columnNames
     * @return
     */
    DataSet merge(DataSet from, Collection<String> columnNames);

    /**
     * Returns a new <code>DataSet</code> by appending the specified <code>from</code> <code>DataSet</code> from <code>fromRowIndex</code> to <code>toRowIndex</code> into this <code>DataSet</code>.
     *
     * @param from
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    DataSet merge(DataSet from, int fromRowIndex, int toRowIndex);

    /**
     * Returns a new <code>DataSet</code> by appending the specified <code>columnNames</code> in <code>from</code> <code>DataSet</code> from <code>fromRowIndex</code> to <code>toRowIndex</code> into this <code>DataSet</code>.
     *
     * @param from
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    DataSet merge(DataSet from, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param a
     * @param b
     * @return
     */
    DataSet merge(final DataSet a, final DataSet b);

    /**
     *
     * @param dss
     * @return
     */
    DataSet merge(final Collection<? extends DataSet> dss);

    /**
     *
     * @param b
     * @return
     */
    DataSet cartesianProduct(DataSet b);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param chunkSize the desired size of each sub DataSet (the last may be smaller).
     * @return
     */
    Stream<DataSet> split(int chunkSize);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param columnNames
     * @param chunkSize the desired size of each sub DataSet (the last may be smaller).
     * @return
     */
    Stream<DataSet> split(Collection<String> columnNames, int chunkSize);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param chunkSize
     * @return
     */
    List<DataSet> splitToList(int chunkSize);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param columnNames
     * @param chunkSize
     * @return
     */
    List<DataSet> splitToList(Collection<String> columnNames, int chunkSize);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param chunkSize
     * @return
     * @deprecated replaced by {@link #splitToList(int)}
     */
    @Deprecated
    List<DataSet> splitt(int chunkSize);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param columnNames
     * @param chunkSize
     * @return
     * @deprecated replaced by {@link #splitToList(Collection, int)}
     */
    @Deprecated
    List<DataSet> splitt(Collection<String> columnNames, int chunkSize);

    /**
     * Returns a frozen {@code DataSet}.
     *
     * @param columnNames
     * @return a copy of this DataSet
     * @see List#subList(int, int).
     */
    DataSet slice(Collection<String> columnNames);

    /**
     * Returns a frozen {@code DataSet}.
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return a copy of this DataSet
     * @see List#subList(int, int).
     */
    DataSet slice(int fromRowIndex, int toRowIndex);

    /**
     * Returns a frozen {@code DataSet}.
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
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
     * @param columnNames
     * @return a copy of this DataSet
     */
    DataSet copy(Collection<String> columnNames);

    /**
     * Returns the copy of this <code>DataSet</code> from the specified <code>fromRowIndex</code> to <code>toRowIndex</code>.
     * The frozen status of the copy will always be false, even the original <code>DataSet</code> is frozen.
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return a copy of this DataSet
     */
    DataSet copy(int fromRowIndex, int toRowIndex);

    /**
     * Returns the copy of this <code>DataSet</code> with specified column name list from the specified <code>fromRowIndex</code> to <code>toRowIndex</code>.
     * The frozen status of the copy will always be false, even the original <code>DataSet</code> is frozen.
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return a copy of this DataSet
     */
    DataSet copy(Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     * Deeply copy each element in this <code>DataSet</code> by Serialization/Deserialization.
     *
     * @return
     */
    DataSet clone();

    /**
     * Deeply copy each element in this <code>DataSet</code> by Serialization/Deserialization.
     *
     * @param freeze
     * @return
     */
    DataSet clone(boolean freeze);

    /**
     *
     * @param <A>
     * @param <B>
     * @param columnNameA
     * @param columnNameB
     * @return
     */
    <A, B> BiIterator<A, B> iterator(String columnNameA, String columnNameB);

    /**
     *
     * @param <A>
     * @param <B>
     * @param columnNameA
     * @param columnNameB
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <A, B> BiIterator<A, B> iterator(String columnNameA, String columnNameB, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param columnNameA
     * @param columnNameB
     * @param columnNameC
     * @return
     */
    <A, B, C> TriIterator<A, B, C> iterator(String columnNameA, String columnNameB, String columnNameC);

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param columnNameA
     * @param columnNameB
     * @param columnNameC
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <A, B, C> TriIterator<A, B, C> iterator(String columnNameA, String columnNameB, String columnNameC, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param pageSize
     * @return
     */
    PaginatedDataSet paginate(int pageSize);

    /**
     * 
     * @param columnNames
     * @param pageSize
     * @return
     */
    PaginatedDataSet paginate(Collection<String> columnNames, int pageSize);

    /**
     *
     * @param <T>
     * @param columnName
     * @return
     */
    <T> Stream<T> stream(String columnName);

    /**
     *
     * @param <T>
     * @param columnName
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <T> Stream<T> stream(String columnName, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <T>
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     */
    <T> Stream<T> stream(Function<? super DisposableObjArray, T> rowMapper);

    /**
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Function<? super DisposableObjArray, T> rowMapper);

    /**
     *
     * @param <T>
     * @param columnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     */
    <T> Stream<T> stream(Collection<String> columnNames, Function<? super DisposableObjArray, T> rowMapper);

    /**
     *
     * @param <T>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     */
    <T> Stream<T> stream(Collection<String> columnNames, int fromRowIndex, int toRowIndex, Function<? super DisposableObjArray, T> rowMapper);

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @return
     */
    <T> Stream<T> stream(Class<? extends T> rowClass);

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <T> Stream<T> stream(Class<? extends T> rowClass, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @return
     */
    <T> Stream<T> stream(Class<? extends T> rowClass, Collection<String> columnNames);

    /**
     *
     * @param <T>
     * @param rowClass it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <T> Stream<T> stream(Class<? extends T> rowClass, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @return
     */
    <T> Stream<T> stream(IntFunction<? extends T> rowSupplier);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <T> Stream<T> stream(IntFunction<? extends T> rowSupplier, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @return
     */
    <T> Stream<T> stream(IntFunction<? extends T> rowSupplier, Collection<String> columnNames);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Entity
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    <T> Stream<T> stream(IntFunction<? extends T> rowSupplier, Collection<String> columnNames, int fromRowIndex, int toRowIndex);

    /**
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    <R, E extends Exception> R apply(Throwables.Function<? super DataSet, R, E> func) throws E;

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    <E extends Exception> void accept(Throwables.Consumer<? super DataSet, E> action) throws E;

    /**
     * Method freeze.
     */
    void freeze();

    /**
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
     * @return
     */
    int size();

    @Beta
    Properties<String, Object> properties();

    Stream<String> columnNames();

    Stream<ImmutableList<Object>> columns();

    /**
     *
     * @return key are column name, value is column - an immutable list, backed by the column in this {@code DataSet}.
     */
    Map<String, ImmutableList<Object>> columnMap();

    // DataSetBuilder builder();

    /**
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    void println() throws UncheckedIOException;

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    void println(Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;

    /**
     *
     * @param <W>
     * @param outputWriter
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <W extends Writer> W println(W outputWriter) throws UncheckedIOException;

    /**
     *
     * @param <W>
     * @param outputWriter
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <W extends Writer> W println(W outputWriter, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException;
}
