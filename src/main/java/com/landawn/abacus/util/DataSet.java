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

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
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
 *
 * @author Haiyang Li
 * @see com.landawn.abacus.util.Builder.DataSetBuilder
 * @see com.landawn.abacus.jdbc.JdbcUtil
 * @see com.landawn.abacus.util.CSVUtil
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

    /**
     * Returns an empty immutable {@code DataSet}.
     * @return
     */
    static DataSet empty() {
        return RowDataSet.EMPTY_DATA_SET;
    }

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
     * Return the column name list in this DataSet.
     *
     * @return
     */
    ImmutableList<String> columnNameList();

    /**
     * Return the count of columns in this DataSet.
     *
     * @return
     */
    int columnCount();

    /**
     *
     * @param columnIndex
     * @return
     */
    String getColumnName(int columnIndex);

    /**
     *
     * @param columnName
     * @return -1 if the specified {@code columnName} is not found
     */
    int getColumnIndex(String columnName);

    /**
     * -1 is set to the element in the returned array if the mapping column name is not included in this {@code DataSet}.
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
     * Check if this {@code DataSet} contains all the specified columns.
     *
     * @param columnNames
     * @return {@code true} if all the specified columns are included in the this {@code DataSet}
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

    //    /**
    //     *
    //     * @param columnName
    //     * @param func
    //     */
    //    void renameColumn(String columnName, Function<? super String, String > func)  ;

    /**
     *
     * @param columnNames
     * @param func
     */
    void renameColumns(Collection<String> columnNames, Function<? super String, String> func);

    /**
     *
     * @param func
     */
    void renameColumns(Function<? super String, String> func);

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
     * <br />
     * Using {@code get(int)} for better performance.
     *
     * @param <T>
     * @param columnName
     * @return
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
     * <br />
     * Using {@code getBoolean(int)} for better performance.
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
     * <br />
     * Using {@code getChar(int)} for better performance.
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
     * <br />
     * Using {@code getByte(int)} for better performance.
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
     * <br />
     * Using {@code getShort(int)} for better performance.
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
     * <br />
     * Using {@code getInt(int)} for better performance.
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
     * <br />
     * Using {@code getLong(int)} for better performance.
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
     * <br />
     * Using {@code getFloat(int)} for better performance.
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
     * <br />
     * Using {@code getDouble(int)} for better performance.
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
     * <br />
     * Using {@code isNull(int)} for better performance.
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
     * <br />
     * Using {@code set(int, Object)} for better performance.
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
    <T> List<T> copyColumn(String columnName);

    /**
     *
     * @param newColumnName
     * @param column
     */
    void addColumn(String newColumnName, List<?> column);

    /**
     *
     * @param newColumnPosition position to add.
     * @param newColumnName
     * @param column
     */
    void addColumn(int newColumnPosition, String newColumnName, List<?> column);

    /**
     * Generate the new column values from the specified column by the specified {@code Function}.
     *
     * @param newColumnName
     * @param fromColumnName
     * @param func
     */
    void addColumn(String newColumnName, String fromColumnName, Function<?, ?> func);

    /**
     * Generate the new column values from the specified column by the specified {@code Function}.
     *
     * @param newColumnPosition 
     * @param newColumnName 
     * @param fromColumnName 
     * @param func 
     */
    void addColumn(int newColumnPosition, String newColumnName, String fromColumnName, Function<?, ?> func);

    /**
     * Generate the new column values from the specified columns by the specified {@code Function}.
     *
     * @param newColumnName 
     * @param fromColumnNames 
     * @param func DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     */
    void addColumn(String newColumnName, Collection<String> fromColumnNames, Function<? super DisposableObjArray, ?> func);

    /**
     * Generate the new column values from the specified columns by the specified {@code Function}.
     *
     * @param newColumnPosition 
     * @param newColumnName 
     * @param fromColumnNames 
     * @param func DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     */
    void addColumn(int newColumnPosition, String newColumnName, Collection<String> fromColumnNames, Function<? super DisposableObjArray, ?> func);

    /**
     * Generate the new column values from the specified columns by the specified {@code Function}.
     *
     * @param newColumnName 
     * @param fromColumnNames 
     * @param func 
     */
    void addColumn(String newColumnName, Tuple2<String, String> fromColumnNames, BiFunction<?, ?, ?> func);

    /**
     * Generate the new column values from the specified columns by the specified {@code Function}.
     *
     * @param newColumnPosition 
     * @param newColumnName 
     * @param fromColumnNames 
     * @param func 
     */
    void addColumn(int newColumnPosition, String newColumnName, Tuple2<String, String> fromColumnNames, BiFunction<?, ?, ?> func);

    /**
     * Generate the new column values from the specified columns by the specified {@code Function}.
     *
     * @param newColumnName 
     * @param fromColumnNames 
     * @param func 
     */
    void addColumn(String newColumnName, Tuple3<String, String, String> fromColumnNames, TriFunction<?, ?, ?, ?> func);

    /**
     * Generate the new column values from the specified columns by the specified {@code Function}.
     *
     * @param newColumnPosition 
     * @param newColumnName 
     * @param fromColumnNames 
     * @param func 
     */
    void addColumn(int newColumnPosition, String newColumnName, Tuple3<String, String, String> fromColumnNames, TriFunction<?, ?, ?, ?> func);

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
     * @param filter column name filter
     */
    void removeColumns(Predicate<? super String> filter);

    //    /**
    //     * Remove the column(s) whose name matches the specified {@code filter}.
    //     *
    //     * @param filter column name filter
    //     * @deprecated replaced by {@code removeColumns}.
    //     */
    //    @Deprecated
    //    void removeColumnsIf(Predicate<? super String> filter);

    /**
     * Update the values of the specified column by the specified Try.Function.
     *
     * @param columnName 
     * @param func 
     */
    void updateColumn(String columnName, Function<?, ?> func);

    /**
     * Update the values of the specified columns one by one with the specified Try.Function.
     *
     * @param columnNames 
     * @param func 
     */
    void updateColumns(Collection<String> columnNames, Function<?, ?> func);

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
    //     * @param targetColumnTypes fill the element with {@code null} if don't wan to convert the target column.
    //     */
    //    void convertColumn(Class<?>[] targetColumnTypes);

    /**
     * Combines the columns specified by {@code columnNames} into a new column and then remove them from this {@code DataSet}.
     *
     * @param columnNames
     * @param newColumnName
     * @param newColumnType it can be Object[]/List/Set/Map/Bean
     */
    void combineColumns(Collection<String> columnNames, String newColumnName, Class<?> newColumnType);

    /**
     * Combines the columns specified by {@code columnNames} into a new column and then remove them from this {@code DataSet}.
     *
     * @param columnNames 
     * @param newColumnName 
     * @param combineFunc DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     */
    void combineColumns(Collection<String> columnNames, String newColumnName, Function<? super DisposableObjArray, ?> combineFunc);

    /**
     * Combines the columns specified by {@code columnNames} into a new column and then remove them from this {@code DataSet}.
     *
     * @param columnNames 
     * @param newColumnName 
     * @param combineFunc 
     */
    void combineColumns(Tuple2<String, String> columnNames, String newColumnName, BiFunction<?, ?, ?> combineFunc);

    /**
     * Combines the columns specified by {@code columnNames} into a new column and then remove them from this {@code DataSet}.
     *
     * @param columnNames 
     * @param newColumnName 
     * @param combineFunc 
     */
    void combineColumns(Tuple3<String, String, String> columnNames, String newColumnName, TriFunction<?, ?, ?, ?> combineFunc);

    /**
     * Combines the columns specified by {@code columnNames} into a new column and then remove them from this {@code DataSet}.
     *
     * @param columnNameFilter
     * @param newColumnName
     * @param newColumnType it can be Object[]/List/Set/Map/Bean
     */
    void combineColumns(Predicate<? super String> columnNameFilter, String newColumnName, Class<?> newColumnType);

    /**
     * Combines the columns specified by {@code columnNames} into a new column and then remove them from this {@code DataSet}.
     *
     * @param columnNameFilter 
     * @param newColumnName 
     * @param combineFunc DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     */
    void combineColumns(Predicate<? super String> columnNameFilter, String newColumnName, Function<? super DisposableObjArray, ?> combineFunc);

    /**
     * Divide the column specified by {@code columnName} into multiple new columns and then remove it from this {@code DataSet}.
     *
     * @param columnName 
     * @param newColumnNames 
     * @param divideFunc 
     */
    void divideColumn(String columnName, Collection<String> newColumnNames, Function<?, ? extends List<?>> divideFunc);

    /**
     * Divide the column specified by {@code columnName} into multiple new columns and then remove it from this {@code DataSet}.
     *
     * @param columnName 
     * @param newColumnNames 
     * @param output 
     */
    void divideColumn(String columnName, Collection<String> newColumnNames, BiConsumer<?, Object[]> output);

    /**
     * Divide the column specified by {@code columnName} into multiple new columns and then remove it from this {@code DataSet}.
     *
     * @param columnName 
     * @param newColumnNames 
     * @param output 
     */
    void divideColumn(String columnName, Tuple2<String, String> newColumnNames, BiConsumer<?, Pair<Object, Object>> output);

    /**
     * Divide the column specified by {@code columnName} into multiple new columns and then remove it from this {@code DataSet}.
     *
     * @param columnName 
     * @param newColumnNames 
     * @param output 
     */
    void divideColumn(String columnName, Tuple3<String, String, String> newColumnNames, BiConsumer<?, Triple<Object, Object, Object>> output);

    /**
     * Adds the row.
     *
     * @param row can be Object[]/List/Map/Bean with getter/setter methods
     */
    void addRow(Object row);

    /**
     * Adds the row.
     *
     * @param newRowPosition
     * @param row can be Object[]/List/Map/Bean with getter/setter methods
     */
    void addRow(int newRowPosition, Object row);

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
     * @param rowIndex 
     * @param func 
     */
    void updateRow(int rowIndex, Function<?, ?> func);

    /**
     * Update the values in the specified rows one by one with the specified Try.Function.
     *
     * @param indices 
     * @param func 
     */
    void updateRows(int[] indices, Function<?, ?> func);

    // TODO should the method name be "replaceAll"? If change the method name to replaceAll, what about updateColumn/updateRow?
    /**
     * Update all the values in this DataSet with the specified Try.Function.
     *
     * @param func 
     */
    void updateAll(Function<?, ?> func);

    /**
     * Replace all the values in this DataSet with the specified new value if it matches the specified condition.
     *
     * @param func 
     * @param newValue 
     */
    void replaceIf(Predicate<?> func, Object newValue);

    /**
     * Prepend the specified {@code other} into this {@code DataSet}.
     * <br />
     * The columns of two {@code DataSet} must be same.
     *
     * @param other
     * @see #merge(DataSet, boolean)
     */
    void prepend(DataSet other);

    /**
     * Append the specified {@code other} into this {@code DataSet}.
     * <br />
     * The columns of two {@code DataSet} must be same.
     *
     * @param other
     * @see #merge(DataSet, boolean)
     */
    void append(DataSet other);

    /**
     * Returns the current row number.
     *
     * @return
     */
    int currentRowNum();

    /**
     * Move the cursor to the specified row.
     *
     * @param rowIndex
     * @return this object itself.
     */
    DataSet absolute(int rowIndex);

    /**
     * Gets the row.
     *
     * @param rowIndex
     * @return
     */
    Object[] getRow(int rowIndex);

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowIndex
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> T getRow(int rowIndex, Class<? extends T> rowType);

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowIndex
     * @param columnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> T getRow(int rowIndex, Collection<String> columnNames, Class<? extends T> rowType);

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowIndex
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> T getRow(int rowIndex, IntFunction<? extends T> rowSupplier);

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowIndex
     * @param columnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> T getRow(int rowIndex, Collection<String> columnNames, IntFunction<? extends T> rowSupplier);

    /**
     *
     * @return {@code Optional<Object[]>}
     */
    Optional<Object[]> firstRow();

    /**
     *
     * @param <T>
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return {@code Optional<E>}
     */
    <T> Optional<T> firstRow(Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return {@code Optional<E>}
     */
    <T> Optional<T> firstRow(Collection<String> columnNames, Class<? extends T> rowType);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return {@code Optional<T>}
     */
    <T> Optional<T> firstRow(IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return {@code Optional<T>}
     */
    <T> Optional<T> firstRow(Collection<String> columnNames, IntFunction<? extends T> rowSupplier);

    /**
     *
     * @return {@code Optional<Object[]>}
     */
    Optional<Object[]> lastRow();

    /**
     *
     * @param <T>
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return {@code Optional<E>}
     */
    <T> Optional<T> lastRow(Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     *            which can be object array/list/set/map/bean.
     * @return {@code Optional<E>}
     */
    <T> Optional<T> lastRow(Collection<String> columnNames, Class<? extends T> rowType);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return {@code Optional<T>}
     */
    <T> Optional<T> lastRow(IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return {@code Optional<T>}
     */
    <T> Optional<T> lastRow(Collection<String> columnNames, IntFunction<? extends T> rowSupplier);

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
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param action DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @throws E the e
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Collection<String> columnNames,
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
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param action
     * @throws E the e
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, Throwables.BiConsumer<?, ?, E> action) throws E;

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
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param action
     * @throws E the e
     */
    <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, Throwables.TriConsumer<?, ?, ?, E> action)
            throws E;

    /**
     *
     *
     * @return
     */
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
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(Collection<String> columnNames, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Class<? extends T> rowType);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(Collection<String> columnNames, IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Collection<String> columnNames, IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param columnNameFilter
     * @param columnNameConverter
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNameFilter
     * @param columnNameConverter
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter,
            Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param columnNameFilter
     * @param columnNameConverter
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter, IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNameFilter
     * @param columnNameConverter
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> List<T> toList(int fromRowIndex, int toRowIndex, Predicate<? super String> columnNameFilter, Function<? super String, String> columnNameConverter,
            IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param prefixAndFieldNameMap
     * @param rowType
     * @return
     */
    <T> List<T> toEntities(Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param prefixAndFieldNameMap
     * @param rowType
     * @return
     */
    <T> List<T> toEntities(int fromRowIndex, int toRowIndex, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param prefixAndFieldNameMap
     * @param rowType
     * @return
     */
    <T> List<T> toEntities(Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param prefixAndFieldNameMap
     * @param rowType
     * @return
     */
    <T> List<T> toEntities(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> rowType);

    /**
     *
     * @param <T>
     * @param rowType
     * @return
     */
    <T> List<T> toMergedEntities(Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param selectPropNames
     * @param rowType
     * @return
     */
    <T> List<T> toMergedEntities(Collection<String> selectPropNames, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param idPropName
     * @param rowType
     * @return
     */
    <T> List<T> toMergedEntities(String idPropName, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param idPropName
     * @param selectPropNames
     * @param rowType
     * @return
     */
    <T> List<T> toMergedEntities(String idPropName, Collection<String> selectPropNames, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param idPropNames
     * @param selectPropNames
     * @param rowType
     * @return
     */
    <T> List<T> toMergedEntities(Collection<String> idPropNames, Collection<String> selectPropNames, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param idPropNames
     * @param selectPropNames
     * @param prefixAndFieldNameMap
     * @param rowType
     * @return
     */
    <T> List<T> toMergedEntities(Collection<String> idPropNames, Collection<String> selectPropNames, Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> rowType);

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
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param keyColumnName
     * @param valueColumnName
     * @param supplier
     * @return
     */
    <K, V, M extends Map<K, V>> M toMap(String keyColumnName, String valueColumnName, IntFunction<? extends M> supplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnName
     * @return
     */
    <K, V> Map<K, V> toMap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName);

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnName
     * @param supplier
     * @return
     */
    <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName, IntFunction<? extends M> supplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <K, V> Map<K, V> toMap(String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType);

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @param supplier
     * @return
     */
    <K, V, M extends Map<K, V>> M toMap(String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType,
            IntFunction<? extends M> supplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <K, V> Map<K, V> toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType);

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @param supplier
     * @return
     */
    <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType,
            IntFunction<? extends M> supplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <K, V> Map<K, V> toMap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends V> rowSupplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @param supplier
     * @return
     */
    <K, V, M extends Map<K, V>> M toMap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends V> rowSupplier,
            IntFunction<? extends M> supplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <K, V> Map<K, V> toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends V> rowSupplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @param supplier
     * @return
     */
    <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends V> rowSupplier, IntFunction<? extends M> supplier);

    /**
     *
     * @param <K> the key type
     * @param <T>
     * @param keyColumnName
     * @param valueColumnName
     * @return
     */
    <K, T> ListMultimap<K, T> toMultimap(String keyColumnName, String valueColumnName);

    /**
     *
     *
     * @param <K> the key type
     * @param <T>
     * @param <V> the value type
     * @param <M>
     * @param keyColumnName
     * @param valueColumnName
     * @param supplier
     * @return
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(String keyColumnName, String valueColumnName, IntFunction<? extends M> supplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnName
     * @return
     */
    <K, T> ListMultimap<K, T> toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName);

    /**
     *
     *
     * @param <K> the key type
     * @param <T>
     * @param <V> the value type
     * @param <M>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnName
     * @param supplier
     * @return
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName,
            IntFunction<? extends M> supplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <T>
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <K, T> ListMultimap<K, T> toMultimap(String keyColumnName, Collection<String> valueColumnNames, Class<? extends T> rowType);

    /**
     *
     *
     * @param <K> the key type
     * @param <T>
     * @param <V> the value type
     * @param <M>
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @param supplier
     * @return
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(String keyColumnName, Collection<String> valueColumnNames,
            Class<? extends T> rowType, IntFunction<? extends M> supplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <K, T> ListMultimap<K, T> toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            Class<? extends T> rowType);

    /**
     *
     *
     * @param <K> the key type
     * @param <T>
     * @param <V> the value type
     * @param <M>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @param supplier
     * @return
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName,
            Collection<String> valueColumnNames, Class<? extends T> rowType, IntFunction<? extends M> supplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <T>
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <K, T> ListMultimap<K, T> toMultimap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <T>
     * @param <V> the value type
     * @param <M>
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @param supplier
     * @return
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends T> rowSupplier, IntFunction<? extends M> supplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <K, T> ListMultimap<K, T> toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <K> the key type
     * @param <T>
     * @param <V> the value type
     * @param <M>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @param supplier
     * @return
     */
    <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName,
            Collection<String> valueColumnNames, IntFunction<? extends T> rowSupplier, IntFunction<? extends M> supplier);

    /**
     *
     *
     * @return
     */
    String toJson();

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    String toJson(int fromRowIndex, int toRowIndex);

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @return
     */
    String toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames);

    /**
     *
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJson(File output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJson(int fromRowIndex, int toRowIndex, File output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames, File output) throws UncheckedIOException;

    /**
     *
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJson(OutputStream output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJson(int fromRowIndex, int toRowIndex, OutputStream output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames, OutputStream output) throws UncheckedIOException;

    /**
     *
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJson(Writer output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJson(int fromRowIndex, int toRowIndex, Writer output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toJson(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Writer output) throws UncheckedIOException;

    /**
     *
     *
     * @return
     */
    String toXml();

    /**
     *
     * @param rowElementName
     * @return
     */
    String toXml(String rowElementName);

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    String toXml(int fromRowIndex, int toRowIndex);

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowElementName
     * @return
     */
    String toXml(int fromRowIndex, int toRowIndex, String rowElementName);

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @return
     */
    String toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames);

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowElementName
     * @return
     */
    String toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName);

    /**
     *
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(File output) throws UncheckedIOException;

    /**
     *
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(String rowElementName, File output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, File output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, String rowElementName, File output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, File output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName, File output) throws UncheckedIOException;

    /**
     *
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(OutputStream output) throws UncheckedIOException;

    /**
     *
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(String rowElementName, OutputStream output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, OutputStream output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, String rowElementName, OutputStream output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, OutputStream output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName, OutputStream output) throws UncheckedIOException;

    /**
     *
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(Writer output) throws UncheckedIOException;

    /**
     *
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(String rowElementName, Writer output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, Writer output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, String rowElementName, Writer output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Writer output) throws UncheckedIOException;

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toXml(int fromRowIndex, int toRowIndex, Collection<String> columnNames, String rowElementName, Writer output) throws UncheckedIOException;

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @return
     */
    String toCsv();

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     *
     * @return
     */
    String toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames);

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param writeTitle
     * @param quoteValue
     * @return
     */
    String toCsv(boolean writeTitle, boolean quoteValue);

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param writeTitle
     * @param quoteValue
     *
     * @return
     */
    String toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, boolean writeTitle, boolean quoteValue);

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(File output) throws UncheckedIOException;

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, File output) throws UncheckedIOException;

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param writeTitle
     * @param quoteValue
     * @param output
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(boolean writeTitle, boolean quoteValue, File output) throws UncheckedIOException;

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param writeTitle
     * @param quoteValue
     * @param output
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, boolean writeTitle, boolean quoteValue, File output)
            throws UncheckedIOException;

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(OutputStream output);

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, OutputStream output) throws UncheckedIOException;

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param writeTitle
     * @param quoteValue
     * @param output
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(boolean writeTitle, boolean quoteValue, OutputStream output) throws UncheckedIOException;

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param writeTitle
     * @param quoteValue
     * @param output
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, boolean writeTitle, boolean quoteValue, OutputStream output)
            throws UncheckedIOException;

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(Writer output);

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Writer output) throws UncheckedIOException;

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param writeTitle
     * @param quoteValue
     * @param output
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(boolean writeTitle, boolean quoteValue, Writer output) throws UncheckedIOException;

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param writeTitle
     * @param quoteValue
     * @param output
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    void toCsv(int fromRowIndex, int toRowIndex, Collection<String> columnNames, boolean writeTitle, boolean quoteValue, Writer output)
            throws UncheckedIOException;

    //    /**
    //     *
    //     * @param columnName specifying the column to group by.
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    DataSet groupBy(String columnName);

    /**
     *
     * @param keyColumnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    DataSet groupBy(String keyColumnName, String aggregateResultColumnName, String aggregateOnColumnName, Collector<?, ?, ?> collector);

    //    /**
    //     *
    //     * @param <T>
    //     * @param <E>
    //     * @param keyColumnName
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     * @throws E the e
    //     */
    //    <T> DataSet groupBy(String keyColumnName, String aggregateResultColumnName, String aggregateOnColumnName,
    //             Function<Stream<T>, ?, E> func) ;

    /**
     *
     * @param keyColumnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowType
     * @return
     */
    DataSet groupBy(String keyColumnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowType);

    /**
     *
     * @param keyColumnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    DataSet groupBy(String keyColumnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     * 
     *
     * @param <T> 
     * @param keyColumnName 
     * @param aggregateResultColumnName 
     * @param aggregateOnColumnNames 
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector 
     * @return 
     */
    <T> DataSet groupBy(String keyColumnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector);

    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <E>
    //     * @param columnName
    //     * @param keyMapper
    //     * @return
    //     * @throws E the e
    //     * @deprecated
    //     */
    //    @Deprecated
    //    <K> DataSet groupBy(String columnName, Function<K, ?> keyMapper);

    /**
     * 
     *
     * @param keyColumnName 
     * @param keyMapper 
     * @param aggregateResultColumnName 
     * @param aggregateOnColumnName 
     * @param collector 
     * @return 
     */
    DataSet groupBy(String keyColumnName, Function<?, ?> keyMapper, String aggregateResultColumnName, String aggregateOnColumnName,
            Collector<?, ?, ?> collector);

    //    /**
    //     *
    //     * @param <T>
    //     * @param <E>
    //     * @param <E2>
    //     * @param keyColumnName
    //     * @param keyMapper
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     */
    //    <T> DataSet groupBy(String keyColumnName, Function<?, ?> keyMapper,
    //            String aggregateResultColumnName, String aggregateOnColumnName,Function<Stream<T>, ?> func);

    /**
     * 
     *
     * @param keyColumnName 
     * @param keyMapper 
     * @param aggregateResultColumnName 
     * @param aggregateOnColumnNames 
     * @param rowType 
     * @return 
     */
    DataSet groupBy(String keyColumnName, Function<?, ?> keyMapper, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Class<?> rowType);

    /**
     * 
     *
     * @param keyColumnName 
     * @param keyMapper 
     * @param aggregateResultColumnName 
     * @param aggregateOnColumnNames 
     * @param collector 
     * @return 
     */
    DataSet groupBy(String keyColumnName, Function<?, ?> keyMapper, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     * 
     *
     * @param <T> 
     * @param keyColumnName 
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName 
     * @param aggregateOnColumnNames 
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector 
     * @return 
     */
    <T> DataSet groupBy(String keyColumnName, Function<?, ?> keyMapper, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector);

    /**
     *
     * @param columnNames
     * @return
     */
    DataSet groupBy(Collection<String> columnNames);

    /**
     *
     * @param keyColumnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    DataSet groupBy(Collection<String> keyColumnNames, String aggregateResultColumnName, String aggregateOnColumnName, Collector<?, ?, ?> collector);

    //    /**
    //     *
    //     * @param <T>
    //     * @param <E>
    //     * @param keyColumnNames
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     * @throws E the e
    //     */
    //    <T> DataSet groupBy(Collection<String> keyColumnNames, String aggregateResultColumnName, String aggregateOnColumnName,
    //            Function<Stream<T>, ?> func);

    /**
     *
     * @param keyColumnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowType
     * @return
     */
    DataSet groupBy(Collection<String> keyColumnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowType);

    /**
     *
     * @param keyColumnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    DataSet groupBy(Collection<String> keyColumnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     * 
     *
     * @param <T> 
     * @param keyColumnNames 
     * @param aggregateResultColumnName 
     * @param aggregateOnColumnNames 
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector 
     * @return 
     */
    <T> DataSet groupBy(Collection<String> keyColumnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector);

    /**
     * 
     *
     * @param keyColumnNames 
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return 
     */
    DataSet groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyMapper);

    /**
     * 
     *
     * @param keyColumnNames 
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName 
     * @param aggregateOnColumnName 
     * @param collector 
     * @return 
     */
    DataSet groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            String aggregateOnColumnName, Collector<?, ?, ?> collector);

    //    /**
    //     *
    //     * @param <T>
    //     * @param keyColumnNames
    //     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     */
    //    <T> DataSet groupBy(Collection<String> keyColumnNames,
    //            Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName, String aggregateOnColumnName,
    //            Function<Stream<T>, ?> func);

    /**
     * 
     *
     * @param keyColumnNames 
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName 
     * @param aggregateOnColumnNames 
     * @param rowType 
     * @return 
     */
    DataSet groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Class<?> rowType);

    /**
     * 
     *
     * @param keyColumnNames 
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName 
     * @param aggregateOnColumnNames 
     * @param collector 
     * @return 
     */
    DataSet groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Collector<? super Object[], ?, ?> collector);

    /**
     * 
     *
     * @param <T> 
     * @param keyColumnNames 
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName 
     * @param aggregateOnColumnNames 
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector 
     * @return 
     */
    <T> DataSet groupBy(Collection<String> keyColumnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector);

    /**
     *
     * @param columnNames
     * @return
     * @see Iterables#rollup(Collection)
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> columnNames);

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     * @see Iterables#rollup(Collection)
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName, Collector<?, ?, ?> collector);

    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     * @see Iterables#rollup(Collection)
    //     */
    //    @Beta
    //    <T> Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName,
    //            Function<Stream<T>, ?> func);

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowType
     * @return
     * @see Iterables#rollup(Collection)
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowType);

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     * @see Iterables#rollup(Collection)
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     *
     * @param <T>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     * @see Iterables#rollup(Collection)
     */
    @Beta
    <T> Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector);

    /**
     *
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     * @see Iterables#rollup(Collection)
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper);

    /**
     *
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     * @see Iterables#rollup(Collection)
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            String aggregateOnColumnName, Collector<?, ?, ?> collector);

    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     * @see Iterables#rollup(Collection)
    //     */
    //    @Beta
    //    <T> Stream<DataSet> rollup(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper,
    //            String aggregateResultColumnName, String aggregateOnColumnName, Function<Stream<T>, ?> func);

    /**
    *
    * @param columnNames
    * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    * @param aggregateResultColumnName
    * @param aggregateOnColumnNames
    * @param rowType
    * @return
     * @see Iterables#rollup(Collection)
    */
    @Beta
    Stream<DataSet> rollup(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Class<?> rowType);

    /**
     *
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     * @see Iterables#rollup(Collection)
     */
    @Beta
    Stream<DataSet> rollup(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Collector<? super Object[], ?, ?> collector);

    /**
     *
     * @param <T>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     * @see Iterables#rollup(Collection)
     */
    @Beta
    <T> Stream<DataSet> rollup(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector);

    /**
     *
     * @param columnNames
     * @return
     * @see Iterables#powerSet(java.util.Set)
     */
    @Beta
    Stream<DataSet> cube(Collection<String> columnNames);

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     * @see Iterables#powerSet(java.util.Set)
     */
    @Beta
    Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName, Collector<?, ?, ?> collector);

    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     * @see Iterables#powerSet(java.util.Set)
    //     */
    //    @Beta
    //    <T> Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName,
    //            Function<Stream<T>, ?> func);

    /**
    *
    * @param columnNames
    * @param aggregateResultColumnName
    * @param aggregateOnColumnNames
    * @param rowType
    * @return
    * @see Iterables#powerSet(java.util.Set)
    */
    @Beta
    Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowType);

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     * @see Iterables#powerSet(java.util.Set)
     */
    @Beta
    Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Collector<? super Object[], ?, ?> collector);

    /**
     *
     * @param <T>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     * @see Iterables#powerSet(java.util.Set)
     */
    @Beta
    <T> Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector);

    /**
     *
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return
     * @see Iterables#powerSet(java.util.Set)
     */
    @Beta
    Stream<DataSet> cube(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper);

    /**
     *
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     * @see Iterables#powerSet(java.util.Set)
     */
    @Beta
    Stream<DataSet> cube(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            String aggregateOnColumnName, Collector<?, ?, ?> collector);

    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     * @see Iterables#powerSet(java.util.Set)
    //     */
    //    @Beta
    //    <T> Stream<DataSet> cube(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper,
    //            String aggregateResultColumnName, String aggregateOnColumnName, Function<Stream<T>, ?> func);

    /**
    *
    * @param columnNames
    * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
    * @param aggregateResultColumnName
    * @param aggregateOnColumnNames
    * @param rowType
    * @return
    * @see Iterables#powerSet(java.util.Set)
    */
    @Beta
    Stream<DataSet> cube(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Class<?> rowType);

    /**
     *
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     * @see Iterables#powerSet(java.util.Set)
     */
    @Beta
    Stream<DataSet> cube(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Collector<? super Object[], ?, ?> collector);

    /**
     *
     * @param <T>
     * @param columnNames
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param collector
     * @return
     * @see Iterables#powerSet(java.util.Set)
     */
    @Beta
    <T> Stream<DataSet> cube(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Function<? super DisposableObjArray, ? extends T> rowMapper, Collector<? super T, ?, ?> collector);

    /**
     *
     *
     * @param <R>
     * @param <C>
     * @param <T>
     * @param <E>
     * @param groupByColumnName
     * @param pivotColumnName
     * @param aggColumnName
     * @param collector
     * @return
     * @throws E the e
     */
    @Beta
    <R, C, T, E extends Exception> Sheet<R, C, T> pivot(String groupByColumnName, String pivotColumnName, String aggColumnName,
            Collector<?, ?, ? extends T> collector) throws E;

    //    /**
    //     *
    //     * @param <R>
    //     * @param <C>
    //     * @param <U>
    //     * @param <T>
    //     * @param <E>
    //     * @param groupByColumnName
    //     * @param pivotColumnName
    //     * @param aggColumnName
    //     * @param aggFunc
    //     * @return
    //     * @throws E the e
    //     */
    //    @Beta
    //    <R, C, U, T, E extends Exception> Sheet<R, C, T> pivot(String groupByColumnName, String pivotColumnName, String aggColumnName,
    //           Function<Stream<U>, ? extends T> aggFunc) throws E;

    /**
     *
     * @param <R>
     * @param <C>
     * @param <T>
     * @param groupByColumnName
     * @param pivotColumnName
     * @param aggColumnNames
     * @param collector
     * @return
     */
    @Beta
    <R, C, T> Sheet<R, C, T> pivot(String groupByColumnName, String pivotColumnName, Collection<String> aggColumnNames,
            Collector<? super Object[], ?, ? extends T> collector);

    /**
     * 
     *
     * @param <R> 
     * @param <C> 
     * @param <U> 
     * @param <T> 
     * @param groupByColumnName 
     * @param pivotColumnName 
     * @param aggColumnNames 
     * @param rowMapper 
     * @param collector 
     * @return 
     */
    @Beta
    <R, C, U, T> Sheet<R, C, T> pivot(String groupByColumnName, String pivotColumnName, Collection<String> aggColumnNames,
            Function<? super DisposableObjArray, ? extends U> rowMapper, Collector<? super U, ?, ? extends T> collector);

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
     * Returns a new {@code DataSet} with the rows de-duplicated by the values in all columns.
     *
     * @return a new DataSet
     */
    DataSet distinct();

    /**
     * Returns a new {@code DataSet} with the rows de-duplicated by the value in the specified column.
     *
     * @param columnName
     * @return a new DataSet
     */
    DataSet distinctBy(String columnName);

    /**
     * Returns a new {@code DataSet} with the rows de-duplicated by the value in the specified column from the specified {@code fromRowIndex} to {@code toRowIndex}.
     *
     * @param columnName 
     * @param keyMapper don't change value of the input parameter.
     * @return 
     */
    DataSet distinctBy(String columnName, Function<?, ?> keyMapper);

    /**
     * Returns a new {@code DataSet} with the rows de-duplicated by the values in the specified columns.
     *
     * @param columnNames
     * @return a new DataSet
     */
    DataSet distinctBy(Collection<String> columnNames);

    /**
     * Returns a new {@code DataSet} with the rows de-duplicated by the values in the specified columns from the specified {@code fromRowIndex} to {@code toRowIndex}.
     *
     * @param columnNames 
     * @param keyMapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return 
     */
    DataSet distinctBy(Collection<String> columnNames, Function<? super DisposableObjArray, ?> keyMapper);

    /**
     * 
     *
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return 
     */
    DataSet filter(Predicate<? super DisposableObjArray> filter);

    /**
     * 
     *
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max 
     * @return 
     */
    DataSet filter(Predicate<? super DisposableObjArray> filter, int max);

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return 
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Predicate<? super DisposableObjArray> filter);

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max 
     * @return 
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Predicate<? super DisposableObjArray> filter, int max);

    /**
     * 
     *
     * @param columnNames 
     * @param filter 
     * @return 
     */
    DataSet filter(Tuple2<String, String> columnNames, BiPredicate<?, ?> filter);

    /**
     * 
     *
     * @param columnNames 
     * @param filter 
     * @param max 
     * @return 
     */
    DataSet filter(Tuple2<String, String> columnNames, BiPredicate<?, ?> filter, int max);

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param filter 
     * @return 
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, BiPredicate<?, ?> filter);

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param filter 
     * @param max 
     * @return 
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, BiPredicate<?, ?> filter, int max);

    /**
     * 
     *
     * @param columnNames 
     * @param filter 
     * @return 
     */
    DataSet filter(Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter);

    /**
     * 
     *
     * @param columnNames 
     * @param filter 
     * @param max 
     * @return 
     */
    DataSet filter(Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter, int max);

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param filter 
     * @return 
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter);

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param filter 
     * @param max 
     * @return 
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, TriPredicate<?, ?, ?> filter, int max);

    /**
     * 
     *
     * @param columnName 
     * @param filter 
     * @return 
     */
    DataSet filter(String columnName, Predicate<?> filter);

    /**
     * 
     *
     * @param columnName 
     * @param filter 
     * @param max 
     * @return 
     */
    DataSet filter(String columnName, Predicate<?> filter, int max);

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnName 
     * @param filter 
     * @return 
     */
    DataSet filter(int fromRowIndex, int toRowIndex, String columnName, Predicate<?> filter);

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnName 
     * @param filter 
     * @param max 
     * @return 
     */
    DataSet filter(int fromRowIndex, int toRowIndex, String columnName, Predicate<?> filter, int max);

    /**
     * 
     *
     * @param columnNames 
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return 
     */
    DataSet filter(Collection<String> columnNames, Predicate<? super DisposableObjArray> filter);

    /**
     * 
     *
     * @param columnNames 
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max 
     * @return 
     */
    DataSet filter(Collection<String> columnNames, Predicate<? super DisposableObjArray> filter, int max);

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return 
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Predicate<? super DisposableObjArray> filter);

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param filter DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @param max 
     * @return 
     */
    DataSet filter(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Predicate<? super DisposableObjArray> filter, int max);

    /**
     * 
     *
     * @param fromColumnName 
     * @param newColumnName 
     * @param copyingColumnName 
     * @param mapper 
     * @return 
     */
    DataSet map(String fromColumnName, String newColumnName, String copyingColumnName, Function<?, ?> mapper);

    /**
     * 
     *
     * @param fromColumnName 
     * @param newColumnName 
     * @param copyingColumnNames 
     * @param mapper 
     * @return 
     */
    DataSet map(String fromColumnName, String newColumnName, Collection<String> copyingColumnNames, Function<?, ?> mapper);

    /**
     * 
     *
     * @param fromColumnNames 
     * @param newColumnName 
     * @param copyingColumnNames 
     * @param mapper 
     * @return 
     */
    DataSet map(Tuple2<String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames, BiFunction<?, ?, ?> mapper);

    /**
     * 
     *
     * @param fromColumnNames 
     * @param newColumnName 
     * @param copyingColumnNames 
     * @param mapper 
     * @return 
     */
    DataSet map(Tuple3<String, String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames, TriFunction<?, ?, ?, ?> mapper);

    /**
     * 
     *
     * @param fromColumnNames 
     * @param newColumnName 
     * @param copyingColumnNames 
     * @param mapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return 
     */
    DataSet map(Collection<String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames,
            Function<? super DisposableObjArray, ?> mapper);

    /**
     * 
     *
     * @param fromColumnName 
     * @param newColumnName 
     * @param copyingColumnName 
     * @param mapper 
     * @return 
     */
    DataSet flatMap(String fromColumnName, String newColumnName, String copyingColumnName, Function<?, ? extends Collection<?>> mapper);

    /**
     * 
     *
     * @param fromColumnName 
     * @param newColumnName 
     * @param copyingColumnNames 
     * @param mapper 
     * @return 
     */
    DataSet flatMap(String fromColumnName, String newColumnName, Collection<String> copyingColumnNames, Function<?, ? extends Collection<?>> mapper);

    /**
     * 
     *
     * @param fromColumnNames 
     * @param newColumnName 
     * @param copyingColumnNames 
     * @param mapper 
     * @return 
     */
    DataSet flatMap(Tuple2<String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames,
            BiFunction<?, ?, ? extends Collection<?>> mapper);

    /**
     * 
     *
     * @param fromColumnNames 
     * @param newColumnName 
     * @param copyingColumnNames 
     * @param mapper 
     * @return 
     */
    DataSet flatMap(Tuple3<String, String, String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames,
            TriFunction<?, ?, ?, ? extends Collection<?>> mapper);

    /**
     * 
     *
     * @param fromColumnNames 
     * @param newColumnName 
     * @param copyingColumnNames 
     * @param mapper DON't cache or update the input parameter {@code DisposableObjArray} or its values(Array)
     * @return 
     */
    DataSet flatMap(Collection<String> fromColumnNames, String newColumnName, Collection<String> copyingColumnNames,
            Function<? super DisposableObjArray, ? extends Collection<?>> mapper);

    /**
     * Returns a new {@code DataSet} that is limited to the rows where there is a match in both this {@code DataSet} and right {@code DataSet}.
     *
     * @param right
     * @param columnName
     * @param joinColumnNameOnRight
     * @return a new DataSet
     */
    DataSet innerJoin(DataSet right, String columnName, String joinColumnNameOnRight);

    /**
     * Returns a new {@code DataSet} that is limited to the rows where there is a match in both this {@code DataSet} and right {@code DataSet}.
     *
     * @param right
     * @param onColumnNames
     * @return a new DataSet
     */
    DataSet innerJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new {@code DataSet} that is limited to the rows where there is a match in both this {@code DataSet} and right {@code DataSet}.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType it can be Object[]/List/Set/Map/Bean
     * @return a new DataSet
     */
    DataSet innerJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType);

    /**
     * Returns a new {@code DataSet} that is limited to the rows where there is a match in both this {@code DataSet} and right {@code DataSet}.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType it can be Object[]/List/Set/Map/Bean
     * @param collSupplier it's for one-to-many join
     * @return a new DataSet
     */
    @SuppressWarnings("rawtypes")
    DataSet innerJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier);

    /**
     * Returns a new {@code DataSet} that has all the rows from this {@code DataSet} and the rows from the specified right {@code DataSet} if they have a match with the rows from the this {@code DataSet}.
     *
     * @param right
     * @param columnName
     * @param joinColumnNameOnRight
     * @return a new DataSet
     */
    DataSet leftJoin(DataSet right, String columnName, String joinColumnNameOnRight);

    /**
     * Returns a new {@code DataSet} that has all the rows from this {@code DataSet} and the rows from the specified right {@code DataSet} if they have a match with the rows from the this {@code DataSet}.
     *
     * @param right
     * @param onColumnNames
     * @return a new DataSet
     */
    DataSet leftJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new {@code DataSet} that has all the rows from this {@code DataSet} and the rows from the specified right {@code DataSet} if they have a match with the rows from the this {@code DataSet}.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType it can be Object[]/List/Set/Map/Bean
     * @return a new DataSet
     */
    DataSet leftJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType);

    /**
     * Returns a new {@code DataSet} that has all the rows from this {@code DataSet} and the rows from the specified right {@code DataSet} if they have a match with the rows from the this {@code DataSet}.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType it can be Object[]/List/Set/Map/Bean
     * @param collSupplier it's for one-to-many join
     * @return a new DataSet
     */
    @SuppressWarnings("rawtypes")
    DataSet leftJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier);

    /**
     * Returns a new {@code DataSet} that has all the rows from the specified right {@code DataSet} and the rows from this {@code DataSet} if they have a match with the rows from the right {@code DataSet}.
     *
     * @param right
     * @param columnName
     * @param joinColumnNameOnRight
     * @return a new DataSet
     */
    DataSet rightJoin(DataSet right, String columnName, String joinColumnNameOnRight);

    /**
     * Returns a new {@code DataSet} that has all the rows from the specified right {@code DataSet} and the rows from this {@code DataSet} if they have a match with the rows from the right {@code DataSet}.
     *
     * @param right
     * @param onColumnNames
     * @return a new DataSet
     */
    DataSet rightJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new {@code DataSet} that has all the rows from the specified right {@code DataSet} and the rows from this {@code DataSet} if they have a match with the rows from the right {@code DataSet}.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType it can be Object[]/List/Set/Map/Bean
     * @return a new DataSet
     */
    DataSet rightJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType);

    /**
     * Returns a new {@code DataSet} that has all the rows from the specified right {@code DataSet} and the rows from this {@code DataSet} if they have a match with the rows from the right {@code DataSet}.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType it can be Object[]/List/Set/Map/Bean
     * @param collSupplier it's for one-to-many join
     * @return a new DataSet
     */
    @SuppressWarnings("rawtypes")
    DataSet rightJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier);

    /**
     * Returns a new {@code DataSet} that has all the rows from this {@code DataSet} and the specified right {@code DataSet}, regardless of whether there are any matches.
     *
     * @param right
     * @param columnName
     * @param joinColumnNameOnRight
     * @return a new DataSet
     */
    DataSet fullJoin(DataSet right, String columnName, String joinColumnNameOnRight);

    /**
     * Returns a new {@code DataSet} that has all the rows from this {@code DataSet} and the specified right {@code DataSet}, regardless of whether there are any matches.
     *
     * @param right
     * @param onColumnNames
     * @return a new DataSet
     */
    DataSet fullJoin(DataSet right, Map<String, String> onColumnNames);

    /**
     * Returns a new {@code DataSet} that has all the rows from this {@code DataSet} and the specified right {@code DataSet}, regardless of whether there are any matches.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType it can be Object[]/List/Set/Map/Bean
     * @return a new DataSet
     */
    DataSet fullJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType);

    /**
     * Returns a new {@code DataSet} that has all the rows from this {@code DataSet} and the specified right {@code DataSet}, regardless of whether there are any matches.
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType it can be Object[]/List/Set/Map/Bean
     * @param collSupplier it's for one-to-many join
     * @return a new DataSet
     */
    @SuppressWarnings("rawtypes")
    DataSet fullJoin(DataSet right, Map<String, String> onColumnNames, String newColumnName, Class<?> newColumnType,
            IntFunction<? extends Collection> collSupplier);

    /**
     * Returns a new {@code DataSet}. Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @return a new DataSet
     */
    DataSet union(DataSet other);

    /**
     * Returns a new {@code DataSet}. Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @param requiresSameColumns
     * @return a new DataSet
     */
    DataSet union(DataSet other, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet}. Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @return a new DataSet
     */
    DataSet union(DataSet other, Collection<String> keyColumnNames);

    /**
     * Returns a new {@code DataSet}. Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return a new DataSet
     */
    DataSet union(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet}. Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @return a new DataSet
     * @see #merge(DataSet)
     */
    DataSet unionAll(DataSet other);

    /**
     * Returns a new {@code DataSet}. Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param requiresSameColumns
     * @return a new DataSet
     * @see #merge(DataSet, boolean)
     */
    DataSet unionAll(DataSet other, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @param keyColumnNames this parameter won't be used. adding it here to be consistent with {@code union(DataSet, Collection)}
     * @return a new DataSet
     */
    @Beta
    DataSet unionAll(DataSet other, Collection<String> keyColumnNames);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @param keyColumnNames this parameter won't be used. adding it here to be consistent with {@code union(DataSet, Collection, boolean)}
     * @param requiresSameColumns
     * @return a new DataSet
     */
    @Beta
    DataSet unionAll(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @return
     */
    DataSet intersect(DataSet other);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    DataSet intersect(DataSet other, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    DataSet intersect(DataSet other, Collection<String> keyColumnNames);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    DataSet intersect(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns..
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @return
     */
    DataSet intersectAll(DataSet other);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    DataSet intersectAll(DataSet other, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns..
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    DataSet intersectAll(DataSet other, Collection<String> keyColumnNames);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    DataSet intersectAll(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @return
     */
    DataSet except(DataSet other);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    DataSet except(DataSet other, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    DataSet except(DataSet other, Collection<String> keyColumnNames);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    DataSet except(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @return
     */
    DataSet exceptAll(DataSet other);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    DataSet exceptAll(DataSet other, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    DataSet exceptAll(DataSet other, Collection<String> keyColumnNames);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    DataSet exceptAll(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns. Occurrences are considered.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#intersection(com.landawn.abacus.util.IntList)
     */
    DataSet intersection(DataSet other);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns. Occurrences are considered.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param requiresSameColumns
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#intersection(com.landawn.abacus.util.IntList)
     */
    DataSet intersection(DataSet other, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns. Occurrences are considered.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#intersection(com.landawn.abacus.util.IntList)
     */
    DataSet intersection(DataSet other, Collection<String> keyColumnNames);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns. Occurrences are considered.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#intersection(com.landawn.abacus.util.IntList)
     */
    DataSet intersection(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns. Occurrences are considered.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#difference(com.landawn.abacus.util.IntList)
     */
    DataSet difference(DataSet other);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns. Occurrences are considered.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param requiresSameColumns
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#difference(com.landawn.abacus.util.IntList)
     */
    DataSet difference(DataSet other, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns. Occurrences are considered.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#difference(com.landawn.abacus.util.IntList)
     */
    DataSet difference(DataSet other, Collection<String> keyColumnNames);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which not appear in the specified {@code other} in common columns. Occurrences are considered.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return a new DataSet
     * @see com.landawn.abacus.util.IntList#difference(com.landawn.abacus.util.IntList)
     */
    DataSet difference(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns or vice versa. Occurrences are considered.
     *
     * @param other
     * @return
     * @see com.landawn.abacus.util.IntList#symmetricDifference(com.landawn.abacus.util.IntList)
     */
    DataSet symmetricDifference(DataSet other);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns or vice versa. Occurrences are considered.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param requiresSameColumns
     * @return
     * @see com.landawn.abacus.util.IntList#symmetricDifference(com.landawn.abacus.util.IntList)
     */
    DataSet symmetricDifference(DataSet other, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns or vice versa. Occurrences are considered.
     *
     * @param other
     * @param keyColumnNames
     * @return
     * @see com.landawn.abacus.util.IntList#symmetricDifference(com.landawn.abacus.util.IntList)
     */
    DataSet symmetricDifference(DataSet other, Collection<String> keyColumnNames);

    /**
     * Returns a new {@code DataSet} with all rows from this DataSet and which also appear in the specified {@code other} in common columns or vice versa. Occurrences are considered.
     * Duplicated rows in the returned {@code DataSet} will not be eliminated.
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     * @see com.landawn.abacus.util.IntList#symmetricDifference(com.landawn.abacus.util.IntList)
     */
    DataSet symmetricDifference(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} by appending the specified {@code other} into this {@code DataSet}.
     *
     * @param other
     * @return
     */
    DataSet merge(DataSet other);

    /**
     * Returns a new {@code DataSet} by appending the specified {@code other} into this {@code DataSet}.
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    DataSet merge(DataSet other, boolean requiresSameColumns);

    /**
     * Returns a new {@code DataSet} by appending the specified {@code other} into this {@code DataSet}.
     *
     * @param other
     * @param columnNames selected column names from {@code other} {@code DataSet} to merge.
     * @return
     */
    DataSet merge(DataSet other, Collection<String> columnNames);

    /**
     * Returns a new {@code DataSet} by appending the specified {@code other} from {@code fromRowIndex} to {@code toRowIndex} into this {@code DataSet}.
     *
     * @param other
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    DataSet merge(DataSet other, int fromRowIndex, int toRowIndex);

    /**
     * Returns a new {@code DataSet} by appending the specified {@code columnNames} in {@code other} from {@code fromRowIndex} to {@code toRowIndex} into this {@code DataSet}.
     *
     * @param other
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames selected column names from {@code other} {@code DataSet} to merge.
     * @return
     */
    DataSet merge(DataSet other, int fromRowIndex, int toRowIndex, Collection<String> columnNames);

    //    /**
    //     *
    //     * @param a
    //     * @param b
    //     * @return
    //     * @deprecated replaced by {@link #merge(Collection)}
    //     */
    //    @Deprecated
    //    DataSet merge(final DataSet a, final DataSet b);

    /**
     *
     * @param others
     * @return
     */
    DataSet merge(final Collection<? extends DataSet> others);

    /**
     *
     * @param others
     * @param requiresSameColumns
     * @return
     */
    DataSet merge(final Collection<? extends DataSet> others, boolean requiresSameColumns);

    /**
     *
     * @param other
     * @return
     */
    DataSet cartesianProduct(DataSet other);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param chunkSize the desired size of each sub DataSet (the last may be smaller).
     * @return
     */
    Stream<DataSet> split(int chunkSize);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     * @param chunkSize the desired size of each sub DataSet (the last may be smaller).
     * @param columnNames
     *
     * @return
     */
    Stream<DataSet> split(int chunkSize, Collection<String> columnNames);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     *
     * @param chunkSize
     * @return
     */
    List<DataSet> splitToList(int chunkSize);

    /**
     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
     * @param chunkSize
     * @param columnNames
     *
     * @return
     */
    List<DataSet> splitToList(int chunkSize, Collection<String> columnNames);

    //    /**
    //     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
    //     *
    //     * @param chunkSize
    //     * @return
    //     * @deprecated replaced by {@link #splitToList(int)}
    //     */
    //    @Deprecated
    //    List<DataSet> splitt(int chunkSize);
    //
    //    /**
    //     * Returns consecutive sub lists of this DataSet, each of the same chunkSize (the list may be smaller), or an empty List if this DataSet is empty.
    //     *
    //     * @param columnNames
    //     * @param chunkSize
    //     * @return
    //     * @deprecated replaced by {@link #splitToList(Collection, int)}
    //     */
    //    @Deprecated
    //    List<DataSet> splitt(Collection<String> columnNames, int chunkSize);

    /**
     * Returns a frozen slice view on this {@code DataSet}.
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return a copy of this DataSet
     * @see List#subList(int, int).
     */
    DataSet slice(int fromRowIndex, int toRowIndex);

    /**
     * Returns a frozen slice view on this {@code DataSet}.
     *
     * @param columnNames
     * @return a copy of this DataSet
     * @see List#subList(int, int).
     */
    DataSet slice(Collection<String> columnNames);

    /**
     * Returns a frozen slice view on this {@code DataSet}.
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     *
     * @return a copy of this DataSet
     * @see List#subList(int, int).
     */
    DataSet slice(int fromRowIndex, int toRowIndex, Collection<String> columnNames);

    /**
     * Returns the copy of this {@code DataSet}.
     * The frozen status of the copy will always be false, even the original {@code DataSet} is frozen.
     *
     * @return a copy of this DataSet
     */
    DataSet copy();

    /**
     * Returns the copy of this {@code DataSet} from the specified {@code fromRowIndex} to {@code toRowIndex}.
     * The frozen status of the copy will always be false, even the original {@code DataSet} is frozen.
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return a copy of this DataSet
     */
    DataSet copy(int fromRowIndex, int toRowIndex);

    /**
     * Returns the copy of this {@code DataSet} with specified column name list.
     * The frozen status of the copy will always be false, even the original {@code DataSet} is frozen.
     *
     * @param columnNames
     * @return a copy of this DataSet
     */
    DataSet copy(Collection<String> columnNames);

    /**
     * Returns the copy of this {@code DataSet} with specified column name list from the specified {@code fromRowIndex} to {@code toRowIndex}.
     * The frozen status of the copy will always be false, even the original {@code DataSet} is frozen.
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     *
     * @return a copy of this DataSet
     */
    DataSet copy(int fromRowIndex, int toRowIndex, Collection<String> columnNames);

    /**
     * Deeply copy each element in this {@code DataSet} by Serialization/Deserialization.
     *
     * @return
     */
    @Beta
    DataSet clone(); //NOSONAR

    /**
     * Deeply copy each element in this {@code DataSet} by Serialization/Deserialization.
     *
     * @param freeze
     * @return
     */
    @Beta
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
     *
     * @param <A>
     * @param <B>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNameA
     * @param columnNameB
     * @return
     */
    <A, B> BiIterator<A, B> iterator(int fromRowIndex, int toRowIndex, String columnNameA, String columnNameB);

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
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNameA
     * @param columnNameB
     * @param columnNameC
     * @return
     */
    <A, B, C> TriIterator<A, B, C> iterator(int fromRowIndex, int toRowIndex, String columnNameA, String columnNameB, String columnNameC);

    /**
     *
     * @param pageSize
     * @return
     */
    Paginated<DataSet> paginate(int pageSize);

    /**
     *
     * @param columnNames
     * @param pageSize
     * @return
     */
    Paginated<DataSet> paginate(Collection<String> columnNames, int pageSize);

    /**
     *
     * @param <T>
     * @param columnName
     * @return
     */
    <T> Stream<T> stream(String columnName);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnName
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, String columnName);

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
     *
     * @param <T>
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> Stream<T> stream(Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> Stream<T> stream(Collection<String> columnNames, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowType it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Class<? extends T> rowType);

    /**
     *
     * @param <T>
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> Stream<T> stream(IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> Stream<T> stream(Collection<String> columnNames, IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowSupplier it can be Object[]/List/Set/Map/Bean
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, IntFunction<? extends T> rowSupplier);

    /**
     *
     *
     * @param <T>
     * @param prefixAndFieldNameMap
     * @param rowType
     * @return
     */
    <T> Stream<T> stream(Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param prefixAndFieldNameMap
     * @param rowType
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param prefixAndFieldNameMap
     * @param rowType
     * @return
     */
    <T> Stream<T> stream(Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param prefixAndFieldNameMap
     * @param rowType
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> rowType);

    /**
     *
     * @param <T>
     * @param rowMapper
     * @return
     */
    <T> Stream<T> stream(IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowMapper
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper);

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowMapper
     * @return
     */
    <T> Stream<T> stream(Collection<String> columnNames, IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowMapper
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper);

    /**
     *
     * @param <T>
     * @param columnNames
     * @param rowMapper
     * @return
     */
    <T> Stream<T> stream(Tuple2<String, String> columnNames, BiFunction<?, ?, ? extends T> rowMapper);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowMapper
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, BiFunction<?, ?, ? extends T> rowMapper);

    /**
     *
     * @param <T>
     * @param columnNames
     * @param rowMapper
     * @return
     */
    <T> Stream<T> stream(Tuple3<String, String, String> columnNames, TriFunction<?, ?, ?, ? extends T> rowMapper);

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowMapper
     * @return
     */
    <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, TriFunction<?, ?, ?, ? extends T> rowMapper);

    /**
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    <R, E extends Exception> R apply(Throwables.Function<? super DataSet, ? extends R, E> func) throws E;

    /**
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    <R, E extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super DataSet, ? extends R, E> func) throws E;

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    <E extends Exception> void accept(Throwables.Consumer<? super DataSet, E> action) throws E;

    /**
     *
     *
     * @param <E>
     * @param action
     * @return
     * @throws E the e
     */
    <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super DataSet, E> action) throws E;

    /**
     * Method freeze.
     */
    void freeze();

    /**
     *
     * @return true, if successful
     */
    boolean isFrozen();

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

    /**
     *
     *
     * @return
     */
    @Beta
    Properties<String, Object> properties();

    /**
     *
     *
     * @return
     */
    Stream<String> columnNames();

    /**
     *
     *
     * @return
     */
    Stream<ImmutableList<Object>> columns();

    /**
     *
     * @return key are column name, value is column - an immutable list, backed by the column in this {@code DataSet}.
     */
    Map<String, ImmutableList<Object>> columnMap();

    // DataSetBuilder builder();

    /**
     *
     */
    void println();

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     */
    void println(int fromRowIndex, int toRowIndex);

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     */
    void println(int fromRowIndex, int toRowIndex, Collection<String> columnNames);

    /**
     *
     *
     * @param outputWriter
     * @throws UncheckedIOException the unchecked IO exception
     */
    void println(Writer outputWriter) throws UncheckedIOException;

    /**
     *
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param outputWriter
     * @throws UncheckedIOException the unchecked IO exception
     */
    void println(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Writer outputWriter) throws UncheckedIOException;
}
