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
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONParser;
import com.landawn.abacus.parser.JSONSerializationConfig;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.XMLConstants;
import com.landawn.abacus.parser.XMLParser;
import com.landawn.abacus.parser.XMLSerializationConfig;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.IntObjFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * It's a row DataSet from logic aspect. But each column is stored in a list.
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings({ "java:S1192", "java:S1698", "java:S1854", "java:S6539" })
public class RowDataSet implements DataSet, Cloneable {

    static final DataSet EMPTY_DATA_SET = new RowDataSet(N.emptyList(), N.emptyList());

    static {
        EMPTY_DATA_SET.freeze();
    }

    static final char PROP_NAME_SEPARATOR = '.';

    static final String NULL_STRING = "null".intern();

    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    static final String TRUE = Boolean.TRUE.toString().intern();

    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();

    static final String FALSE = Boolean.FALSE.toString().intern();

    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    static final Set<Class<?>> SUPPORTED_COUNT_COLUMN_TYPES = N.asSet((Class<?>) int.class, Integer.class, long.class, Long.class, float.class, Float.class,
            double.class, Double.class);

    static final String POSTFIX_FOR_SAME_JOINED_COLUMN_NAME = "_2";

    /**
     * Field CACHED_PROP_NAMES. (value is ""cachedPropNames"")
     */
    static final String CACHED_PROP_NAMES = "cachedPropNames";

    private static final String ROW = "row";

    private static final JSONParser jsonParser = ParserFactory.createJSONParser();

    private static final XMLParser xmlParser = ParserFactory.isXMLAvailable() ? ParserFactory.createXMLParser() : null;

    private static final KryoParser kryoParser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : null;

    private static final JSONSerializationConfig jsc = JSC.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

    private static final XMLSerializationConfig xsc = XSC.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

    private static final Type<Object> strType = N.typeOf(String.class);

    List<String> _columnNameList; //NOSONAR

    List<List<Object>> _columnList; //NOSONAR

    Map<String, Integer> _columnIndexMap; //NOSONAR

    int[] _columnIndexes; //NOSONAR

    int _currentRowNum = 0; //NOSONAR

    boolean _isFrozen = false; //NOSONAR

    Properties<String, Object> _properties; //NOSONAR

    transient int modCount = 0; //NOSONAR

    // For Kryo
    protected RowDataSet() {
    }

    /**
     *
     *
     * @param columnNameList
     * @param columnList
     */
    public RowDataSet(final List<String> columnNameList, final List<List<Object>> columnList) {
        this(columnNameList, columnList, null);
    }

    /**
     *
     *
     * @param columnNameList
     * @param columnList
     * @param properties
     */
    public RowDataSet(final List<String> columnNameList, final List<List<Object>> columnList, final Properties<String, Object> properties) {
        N.checkArgNotNull(columnNameList);
        N.checkArgNotNull(columnList);

        N.checkArgument(!N.hasDuplicates(columnNameList), "Dupliated column names: {}", columnNameList);
        N.checkArgument(columnNameList.size() == columnList.size(), "the size of column name list: {} is different from the size of column list: {}",
                columnNameList.size(), columnList.size());

        final int size = columnList.size() == 0 ? 0 : columnList.get(0).size();

        for (List<Object> column : columnList) {
            N.checkArgument(column.size() == size, "All columns in the specified 'columnList' must have same size.");
        }

        this._columnNameList = columnNameList;

        this._columnList = columnList;

        this._properties = properties;
    }

    //    @Override
    //    public String beanName() {
    //        return _beanName;
    //    }
    //
    //    @SuppressWarnings("unchecked")
    //    @Override
    //    public <T> Class<T> beanClass() {
    //        return (Class<T>) _beanClass;
    //    }

    /**
     * Column name list.
     *
     * @return
     */
    @Override
    public ImmutableList<String> columnNameList() {
        // return _columnNameList;

        return ImmutableList.wrap(_columnNameList);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int columnCount() {
        return _columnNameList.size();
    }

    /**
     * Gets the column name.
     *
     * @param columnIndex
     * @return
     */
    @Override
    public String getColumnName(final int columnIndex) {
        return _columnNameList.get(columnIndex);
    }

    /**
     * Gets the column index.
     *
     * @param columnName
     * @return
     */
    @Override
    public int getColumnIndex(final String columnName) {
        if (_columnIndexMap == null) {
            _columnIndexMap = new HashMap<>();

            int i = 0;
            for (String e : _columnNameList) {
                _columnIndexMap.put(e, i++);
            }
        }

        Integer columnIndex = _columnIndexMap.get(columnName);

        if (columnIndex == null /* && NameUtil.isCanonicalName(_beanName, columnName)*/) {
            columnIndex = _columnIndexMap.get(NameUtil.getSimpleName(columnName));
        }

        return (columnIndex == null) ? -1 : columnIndex;
    }

    /**
     * Gets the column indexes.
     *
     * @param columnNames
     * @return
     */
    @Override
    public int[] getColumnIndexes(final Collection<String> columnNames) {
        if (N.isEmpty(columnNames)) {
            return N.EMPTY_INT_ARRAY;
        }

        int[] columnIndexes = new int[columnNames.size()];
        int i = 0;

        for (String columnName : columnNames) {
            columnIndexes[i++] = getColumnIndex(columnName);
        }

        return columnIndexes;
    }

    /**
     *
     * @param columnName
     * @return true, if successful
     */
    @Override
    public boolean containsColumn(final String columnName) {
        return getColumnIndex(columnName) >= 0;
    }

    /**
     * Contains all columns.
     *
     * @param columnNames
     * @return true, if successful
     */
    @Override
    public boolean containsAllColumns(final Collection<String> columnNames) {
        for (String columnName : columnNames) {
            if (!containsColumn(columnName)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param columnName
     * @param newColumnName
     */
    @Override
    public void renameColumn(final String columnName, final String newColumnName) {
        checkFrozen();

        int idx = checkColumnName(columnName);

        if (columnName.equals(newColumnName)) {
            // ignore.
        } else {
            if (_columnNameList.contains(newColumnName)) {
                throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
            }

            if (_columnIndexMap != null) {
                _columnIndexMap.put(newColumnName, _columnIndexMap.remove(_columnNameList.get(idx)));
            }

            _columnNameList.set(idx, newColumnName);
        }

        modCount++;
    }

    /**
     *
     * @param oldNewNames
     */
    @Override
    public void renameColumns(final Map<String, String> oldNewNames) {
        checkFrozen();

        if (N.hasDuplicates(oldNewNames.values())) {
            throw new IllegalArgumentException("Duplicated new column names: " + oldNewNames.values());
        }

        for (Map.Entry<String, String> entry : oldNewNames.entrySet()) {
            checkColumnName(entry.getKey());

            if (_columnNameList.contains(entry.getValue()) && !entry.getKey().equals(entry.getValue())) {
                throw new IllegalArgumentException("The new column name: " + entry.getValue() + " is already included this DataSet: " + _columnNameList);
            }
        }

        for (Map.Entry<String, String> entry : oldNewNames.entrySet()) {
            renameColumn(entry.getKey(), entry.getValue());
        }
    }

    /**
     *
     * @param <E>
     * @param columnName
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void renameColumn(final String columnName, final Throwables.Function<String, String, E> func) throws E {
        renameColumn(columnName, func.apply(columnName));
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void renameColumns(final Collection<String> columnNames, final Throwables.Function<String, String, E> func) throws E {
        checkColumnName(columnNames);

        final Map<String, String> map = N.newHashMap(columnNames.size());

        for (String columnName : columnNames) {
            map.put(columnName, func.apply(columnName));
        }

        renameColumns(map);
    }

    /**
     *
     * @param <E>
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void renameColumns(final Throwables.Function<String, String, E> func) throws E {
        renameColumns(_columnNameList, func);
    }

    /**
     *
     * @param columnName
     * @param newPosition
     */
    @Override
    public void moveColumn(final String columnName, int newPosition) {
        checkFrozen();

        int idx = checkColumnName(columnName);

        if (newPosition < 0 || newPosition >= columnCount()) {
            throw new IllegalArgumentException("The new column index must be >= 0 and < " + columnCount());
        }

        if (idx == newPosition) {
            // ignore.
        } else {
            _columnNameList.add(newPosition, _columnNameList.remove(idx));
            _columnList.add(newPosition, _columnList.remove(idx));

            _columnIndexMap = null;
            _columnIndexes = null;
        }

        modCount++;
    }

    /**
     *
     * @param columnNameNewPositionMap
     */
    @Override
    public void moveColumns(Map<String, Integer> columnNameNewPositionMap) {
        checkFrozen();

        final List<Map.Entry<String, Integer>> entries = new ArrayList<>(columnNameNewPositionMap.size());

        for (Map.Entry<String, Integer> entry : columnNameNewPositionMap.entrySet()) {
            checkColumnName(entry.getKey());

            if (entry.getValue().intValue() < 0 || entry.getValue().intValue() >= columnCount()) {
                throw new IllegalArgumentException("The new column index must be >= 0 and < " + columnCount());
            }

            entries.add(entry);
        }

        N.sort(entries, (Comparator<Entry<String, Integer>>) (o1, o2) -> Integer.compare(o1.getValue(), o2.getValue()));

        for (Map.Entry<String, Integer> entry : entries) {
            int currentColumnIndex = checkColumnName(entry.getKey());

            if (currentColumnIndex == entry.getValue().intValue()) {
                // ignore.
            } else {
                _columnNameList.add(entry.getValue(), _columnNameList.remove(currentColumnIndex));
                _columnList.add(entry.getValue(), _columnList.remove(currentColumnIndex));

                _columnIndexMap = null;
            }
        }

        modCount++;
    }

    /**
     *
     * @param columnNameA
     * @param columnNameB
     */
    @Override
    public void swapColumns(String columnNameA, String columnNameB) {
        checkFrozen();

        int columnIndexA = checkColumnName(columnNameA);
        int columnIndexB = checkColumnName(columnNameB);

        if (columnNameA.equals(columnNameB)) {
            return;
        }

        final String tmpColumnNameA = _columnNameList.get(columnIndexA);
        _columnNameList.set(columnIndexA, _columnNameList.get(columnIndexB));
        _columnNameList.set(columnIndexB, tmpColumnNameA);

        final List<Object> tmpColumnA = _columnList.get(columnIndexA);
        _columnList.set(columnIndexA, _columnList.get(columnIndexB));
        _columnList.set(columnIndexB, tmpColumnA);

        if (N.notEmpty(_columnIndexMap)) {
            _columnIndexMap.put(columnNameA, columnIndexB);
            _columnIndexMap.put(columnNameB, columnIndexA);
        }

        modCount++;
    }

    /**
     *
     * @param rowIndex
     * @param newRowIndex
     */
    @Override
    public void moveRow(int rowIndex, int newRowIndex) {
        checkFrozen();

        this.checkRowNum(rowIndex);
        this.checkRowNum(newRowIndex);

        if (rowIndex == newRowIndex) {
            return;
        }

        for (List<Object> column : _columnList) {
            column.add(newRowIndex, column.remove(rowIndex));
        }

        modCount++;
    }

    /**
     *
     * @param rowIndexA
     * @param rowIndexB
     */
    @Override
    public void swapRows(int rowIndexA, int rowIndexB) {
        checkFrozen();

        this.checkRowNum(rowIndexA);
        this.checkRowNum(rowIndexB);

        if (rowIndexA == rowIndexB) {
            return;
        }

        Object tmp = null;

        for (List<Object> column : _columnList) {
            tmp = column.get(rowIndexA);
            column.set(rowIndexA, column.get(rowIndexB));
            column.set(rowIndexB, tmp);
        }

        modCount++;
    }

    /**
     *
     * @param <T>
     * @param rowIndex
     * @param columnIndex
     * @return
     */
    @Override
    public <T> T get(final int rowIndex, final int columnIndex) {
        return (T) _columnList.get(columnIndex).get(rowIndex);
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param targetType
    //     * @param rowIndex
    //     * @param columnIndex
    //     * @return
    //     */
    //    @Override
    //    public <T> T get(final Class<? extends T> targetType, final int rowIndex, final int columnIndex) {
    //        T rt = (T) _columnList.get(columnIndex).get(rowIndex);
    //
    //        return (rt == null) ? N.defaultValueOf(targetType) : rt;
    //    }

    /**
     *
     * @param rowIndex
     * @param columnIndex
     * @param element
     */
    @Override
    public void set(final int rowIndex, final int columnIndex, final Object element) {
        checkFrozen();

        _columnList.get(columnIndex).set(rowIndex, element);

        modCount++;
    }

    /**
     * Checks if is null.
     *
     * @param rowIndex
     * @param columnIndex
     * @return true, if is null
     */
    @Override
    public boolean isNull(final int rowIndex, final int columnIndex) {
        return get(rowIndex, columnIndex) == null;
    }

    /**
     *
     * @param <T>
     * @param columnIndex
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final int columnIndex) {
        return (T) _columnList.get(columnIndex).get(_currentRowNum);
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param targetType
    //     * @param columnIndex
    //     * @return
    //     */
    //    @Override
    //    public <T> T get(final Class<? extends T> targetType, final int columnIndex) {
    //        T rt = get(columnIndex);
    //
    //        return (rt == null) ? N.defaultValueOf(targetType) : rt;
    //    }

    /**
     *
     * @param <T>
     * @param columnName
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final String columnName) {
        return (T) get(checkColumnName(columnName));
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param targetType
    //     * @param columnName
    //     * @return
    //     */
    //    @Override
    //    public <T> T get(final Class<? extends T> targetType, final String columnName) {
    //        return get(targetType, checkColumnName(columnName));
    //    }
    //
    //    /**
    //     * Gets the or default.
    //     *
    //     * @param <T>
    //     * @param columnIndex
    //     * @param defaultValue
    //     * @return
    //     */
    //    @Override
    //    public <T> T getOrDefault(int columnIndex, T defaultValue) {
    //        return columnIndex < 0 ? defaultValue : (T) get(columnIndex);
    //    }
    //
    //    /**
    //     * Gets the or default.
    //     *
    //     * @param <T>
    //     * @param columnName
    //     * @param defaultValue
    //     * @return
    //     */
    //    @Override
    //    public <T> T getOrDefault(final String columnName, T defaultValue) {
    //        return getOrDefault(getColumnIndex(columnName), defaultValue);
    //    }

    /**
     * Gets the boolean.
     *
     * @param columnIndex
     * @return
     */
    @Override
    public boolean getBoolean(final int columnIndex) {
        Boolean rt = get(columnIndex);

        return (rt == null) ? false : rt;
    }

    /**
     * Gets the boolean.
     *
     * @param columnName
     * @return
     */
    @Override
    public boolean getBoolean(final String columnName) {
        return getBoolean(checkColumnName(columnName));
    }

    /**
     * Gets the char.
     *
     * @param columnIndex
     * @return
     */
    @Override
    public char getChar(final int columnIndex) {
        Character rt = get(columnIndex);

        return (rt == null) ? 0 : rt;
    }

    /**
     * Gets the char.
     *
     * @param columnName
     * @return
     */
    @Override
    public char getChar(final String columnName) {
        return getChar(checkColumnName(columnName));
    }

    /**
     * Gets the byte.
     *
     * @param columnIndex
     * @return
     */
    @Override
    public byte getByte(final int columnIndex) {
        Number rt = get(columnIndex);

        return (rt == null) ? 0 : rt.byteValue();
    }

    /**
     * Gets the byte.
     *
     * @param columnName
     * @return
     */
    @Override
    public byte getByte(final String columnName) {
        return getByte(checkColumnName(columnName));
    }

    /**
     * Gets the short.
     *
     * @param columnIndex
     * @return
     */
    @Override
    public short getShort(final int columnIndex) {
        Number rt = get(columnIndex);

        return (rt == null) ? 0 : rt.shortValue();
    }

    /**
     * Gets the short.
     *
     * @param columnName
     * @return
     */
    @Override
    public short getShort(final String columnName) {
        return getShort(checkColumnName(columnName));
    }

    /**
     * Gets the int.
     *
     * @param columnIndex
     * @return
     */
    @Override
    public int getInt(final int columnIndex) {
        Number rt = get(columnIndex);

        return (rt == null) ? 0 : rt.intValue();
    }

    /**
     * Gets the int.
     *
     * @param columnName
     * @return
     */
    @Override
    public int getInt(final String columnName) {
        return getInt(checkColumnName(columnName));
    }

    /**
     * Gets the long.
     *
     * @param columnIndex
     * @return
     */
    @Override
    public long getLong(final int columnIndex) {
        Number rt = get(columnIndex);

        return (rt == null) ? 0L : rt.longValue();
    }

    /**
     * Gets the long.
     *
     * @param columnName
     * @return
     */
    @Override
    public long getLong(final String columnName) {
        return getLong(checkColumnName(columnName));
    }

    /**
     * Gets the float.
     *
     * @param columnIndex
     * @return
     */
    @Override
    public float getFloat(final int columnIndex) {
        Number rt = get(columnIndex);

        return (rt == null) ? 0f : Numbers.toFloat(rt);
    }

    /**
     * Gets the float.
     *
     * @param columnName
     * @return
     */
    @Override
    public float getFloat(final String columnName) {
        return getFloat(checkColumnName(columnName));
    }

    /**
     * Gets the double.
     *
     * @param columnIndex
     * @return
     */
    @Override
    public double getDouble(final int columnIndex) {
        Number rt = get(columnIndex);

        return (rt == null) ? 0d : Numbers.toDouble(rt);
    }

    /**
     * Gets the double.
     *
     * @param columnName
     * @return
     */
    @Override
    public double getDouble(final String columnName) {
        return getDouble(checkColumnName(columnName));
    }

    /**
     * Checks if is null.
     *
     * @param columnIndex
     * @return true, if is null
     */
    @Override
    public boolean isNull(final int columnIndex) {
        return get(columnIndex) == null;
    }

    /**
     * Checks if is null.
     *
     * @param columnName
     * @return true, if is null
     */
    @Override
    public boolean isNull(final String columnName) {
        return get(columnName) == null;
    }

    /**
     *
     * @param columnIndex
     * @param value
     */
    @Override
    public void set(final int columnIndex, final Object value) {
        checkFrozen();

        _columnList.get(columnIndex).set(_currentRowNum, value);

        modCount++;
    }

    /**
     *
     * @param columnName
     * @param value
     */
    @Override
    public void set(final String columnName, final Object value) {
        set(checkColumnName(columnName), value);
    }

    /**
     * Gets the column.
     *
     * @param <T>
     * @param columnIndex
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> ImmutableList<T> getColumn(final int columnIndex) {
        // return (List<T>) _columnList.get(columnIndex);
        return ImmutableList.wrap((List) _columnList.get(columnIndex));
    }

    /**
     * Gets the column.
     *
     * @param <T>
     * @param columnName
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> ImmutableList<T> getColumn(final String columnName) {
        return getColumn(checkColumnName(columnName));
    }

    /**
     * Copy of column.
     *
     * @param <T>
     * @param columnName
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public <T> List<T> copyOfColumn(final String columnName) {
        return new ArrayList<>((List) _columnList.get(checkColumnName(columnName)));
    }

    /**
     * Adds the column.
     *
     * @param newColumnName
     * @param column
     */
    @Override
    public void addColumn(final String newColumnName, final List<?> column) {
        addColumn(_columnList.size(), newColumnName, column);
    }

    /**
     * Adds the column.
     *
     * @param newColumnPosition
     * @param newColumnName
     * @param column
     */
    @Override
    public void addColumn(final int newColumnPosition, final String newColumnName, final List<?> column) {
        checkFrozen();

        if (newColumnPosition < 0 || newColumnPosition > columnCount()) {
            throw new IllegalArgumentException("Invalid column index: " + newColumnPosition + ". It must be >= 0 and <= " + columnCount());
        }

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
        }

        if (N.notEmpty(column) && column.size() != size()) {
            throw new IllegalArgumentException("The specified column size[" + column.size() + "] must be same as the this DataSet size[" + size() + "]. ");
        }

        _columnNameList.add(newColumnPosition, newColumnName);

        if (N.isEmpty(column)) {
            _columnList.add(newColumnPosition, N.repeat(null, size()));
        } else {
            _columnList.add(newColumnPosition, new ArrayList<>(column));
        }

        updateColumnIndex(newColumnPosition, newColumnName);

        modCount++;
    }

    /**
     * Adds the column.
     *
     * @param <E>
     * @param newColumnName
     * @param fromColumnName
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void addColumn(String newColumnName, String fromColumnName, Throwables.Function<?, ?, E> func) throws E {
        addColumn(_columnList.size(), newColumnName, fromColumnName, func);
    }

    /**
     * Adds the column.
     *
     * @param <E>
     * @param newColumnPosition
     * @param newColumnName
     * @param fromColumnName
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void addColumn(int newColumnPosition, final String newColumnName, String fromColumnName, Throwables.Function<?, ?, E> func)
            throws E {
        checkFrozen();

        if (newColumnPosition < 0 || newColumnPosition > columnCount()) {
            throw new IllegalArgumentException("Invalid column index: " + newColumnPosition + ". It must be >= 0 and <= " + columnCount());
        }

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
        }

        final List<Object> newColumn = new ArrayList<>(size());
        final Throwables.Function<Object, Object, E> mapper2 = (Throwables.Function<Object, Object, E>) func;
        final List<Object> column = _columnList.get(checkColumnName(fromColumnName));

        for (Object val : column) {
            newColumn.add(mapper2.apply(val));
        }

        _columnNameList.add(newColumnPosition, newColumnName);
        _columnList.add(newColumnPosition, newColumn);

        updateColumnIndex(newColumnPosition, newColumnName);

        modCount++;
    }

    /**
     * Adds the column.
     *
     * @param <E>
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void addColumn(String newColumnName, Collection<String> fromColumnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> func) throws E {
        addColumn(_columnList.size(), newColumnName, fromColumnNames, func);
    }

    /**
     * Adds the column.
     *
     * @param <E>
     * @param newColumnPosition
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void addColumn(int newColumnPosition, final String newColumnName, Collection<String> fromColumnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> func) throws E {
        checkFrozen();

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
        }

        final int size = size();
        final int[] fromColumnIndexes = checkColumnName(fromColumnNames);
        final Throwables.Function<? super DisposableObjArray, Object, E> mapper2 = (Throwables.Function<? super DisposableObjArray, Object, E>) func;
        final List<Object> newColumn = new ArrayList<>(size);
        final Object[] row = new Object[fromColumnIndexes.length];
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(row);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0, len = fromColumnIndexes.length; i < len; i++) {
                row[i] = _columnList.get(fromColumnIndexes[i]).get(rowIndex);
            }

            newColumn.add(mapper2.apply(disposableArray));
        }

        _columnNameList.add(newColumnPosition, newColumnName);
        _columnList.add(newColumnPosition, newColumn);

        updateColumnIndex(newColumnPosition, newColumnName);

        modCount++;
    }

    /**
     * Update column index.
     *
     * @param columnIndex
     * @param newColumnName
     */
    private void updateColumnIndex(int columnIndex, final String newColumnName) {
        if (_columnIndexMap != null && columnIndex == _columnIndexMap.size()) {
            _columnIndexMap.put(newColumnName, columnIndex);
        } else {
            _columnIndexMap = null;
        }

        if (_columnIndexes != null && columnIndex == _columnIndexes.length) {
            _columnIndexes = N.copyOf(_columnIndexes, _columnIndexes.length + 1);
            _columnIndexes[columnIndex] = columnIndex;
        } else {
            _columnIndexes = null;
        }
    }

    /**
     * Adds the column.
     *
     * @param <E>
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void addColumn(String newColumnName, Tuple2<String, String> fromColumnNames, Throwables.BiFunction<?, ?, ?, E> func) throws E {
        addColumn(_columnList.size(), newColumnName, fromColumnNames, func);
    }

    /**
     * Adds the column.
     *
     * @param <E>
     * @param newColumnPosition
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void addColumn(int newColumnPosition, final String newColumnName, Tuple2<String, String> fromColumnNames,
            Throwables.BiFunction<?, ?, ?, E> func) throws E {
        checkFrozen();

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
        }

        final int size = size();
        final List<Object> column1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(fromColumnNames._2));

        @SuppressWarnings("rawtypes")
        final Throwables.BiFunction<Object, Object, Object, E> mapper2 = (Throwables.BiFunction) func;
        final List<Object> newColumn = new ArrayList<>(size());

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            newColumn.add(mapper2.apply(column1.get(rowIndex), column2.get(rowIndex)));
        }

        _columnNameList.add(newColumnPosition, newColumnName);
        _columnList.add(newColumnPosition, newColumn);

        updateColumnIndex(newColumnPosition, newColumnName);

        modCount++;
    }

    /**
     * Adds the column.
     *
     * @param <E>
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void addColumn(String newColumnName, Tuple3<String, String, String> fromColumnNames,
            Throwables.TriFunction<?, ?, ?, ?, E> func) throws E {
        addColumn(_columnList.size(), newColumnName, fromColumnNames, func);
    }

    /**
     * Adds the column.
     *
     * @param <E>
     * @param newColumnPosition
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void addColumn(int newColumnPosition, final String newColumnName, Tuple3<String, String, String> fromColumnNames,
            Throwables.TriFunction<?, ?, ?, ?, E> func) throws E {
        checkFrozen();

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
        }

        final int size = size();
        final List<Object> column1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(fromColumnNames._3));
        @SuppressWarnings("rawtypes")
        final Throwables.TriFunction<Object, Object, Object, Object, E> mapper2 = (Throwables.TriFunction) func;
        final List<Object> newColumn = new ArrayList<>(size());

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            newColumn.add(mapper2.apply(column1.get(rowIndex), column2.get(rowIndex), column3.get(rowIndex)));
        }

        _columnNameList.add(newColumnPosition, newColumnName);
        _columnList.add(newColumnPosition, newColumn);

        updateColumnIndex(newColumnPosition, newColumnName);

        modCount++;
    }

    /**
     * Removes the column.
     *
     * @param <T>
     * @param columnName
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public <T> List<T> removeColumn(final String columnName) {
        checkFrozen();

        final int columnIndex = checkColumnName(columnName);

        _columnIndexMap = null;
        _columnIndexes = null;

        _columnNameList.remove(columnIndex);
        final List<Object> removedColumn = _columnList.remove(columnIndex);

        modCount++;

        return (List) removedColumn;
    }

    /**
     * Removes the columns.
     *
     * @param columnNames
     */
    @Override
    public void removeColumns(final Collection<String> columnNames) {
        checkFrozen();

        final int[] columnIndexes = checkColumnName(columnNames);
        N.sort(columnIndexes);

        for (int i = 0, len = columnIndexes.length; i < len; i++) {
            _columnNameList.remove(columnIndexes[i] - i);
            _columnList.remove(columnIndexes[i] - i);
        }

        _columnIndexMap = null;
        _columnIndexes = null;

        modCount++;
    }

    /**
     * Removes the columns.
     *
     * @param <E>
     * @param filter
     * @throws E the e
     */
    @Override
    public <E extends Exception> void removeColumns(Throwables.Predicate<String, E> filter) throws E {
        removeColumns(N.filter(_columnNameList, filter));
    }

    //    /**
    //     * Removes the columns if.
    //     *
    //     * @param <E>
    //     * @param filter
    //     * @throws E the e
    //     */
    //    @Deprecated
    //    @Override
    //    public <E extends Exception> void removeColumnsIf(Predicate<String, E> filter) throws E {
    //        removeColumns(filter);
    //    }

    /**
     *
     * @param columnName
     * @param targetType
     */
    @Override
    public void convertColumn(final String columnName, final Class<?> targetType) {
        checkFrozen();

        convertColumnType(checkColumnName(columnName), targetType);
    }

    /**
     *
     * @param columnTargetTypes
     */
    @Override
    public void convertColumns(final Map<String, Class<?>> columnTargetTypes) {
        checkFrozen();

        checkColumnName(columnTargetTypes.keySet());

        for (Map.Entry<String, Class<?>> entry : columnTargetTypes.entrySet()) {
            convertColumnType(checkColumnName(entry.getKey()), entry.getValue());
        }
    }

    /**
     *
     *
     * @param <E>
     * @param columnName
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void updateColumn(final String columnName, final Throwables.Function<?, ?, E> func) throws E {
        checkFrozen();

        final Throwables.Function<Object, Object, E> func2 = (Throwables.Function<Object, Object, E>) func;
        final List<Object> column = _columnList.get(checkColumnName(columnName));

        for (int i = 0, len = size(); i < len; i++) {
            column.set(i, func2.apply(column.get(i)));
        }

        modCount++;
    }

    /**
     *
     *
     * @param <E>
     * @param columnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void updateColumns(final Collection<String> columnNames, final Throwables.Function<?, ?, E> func) throws E {
        checkColumnName(columnNames);

        final Throwables.Function<Object, Object, E> func2 = (Throwables.Function<Object, Object, E>) func;

        for (String columnName : columnNames) {
            final List<Object> column = _columnList.get(checkColumnName(columnName));

            for (int i = 0, len = size(); i < len; i++) {
                column.set(i, func2.apply(column.get(i)));
            }
        }

        modCount++;
    }

    /**
     * Convert column type.
     *
     * @param columnIndex
     * @param targetType
     */
    private void convertColumnType(final int columnIndex, final Class<?> targetType) {
        final List<Object> column = _columnList.get(columnIndex);

        Object newValue = null;
        for (int i = 0, len = size(); i < len; i++) {
            newValue = N.convert(column.get(i), targetType);

            column.set(i, newValue);
        }

        modCount++;
    }

    /**
     *
     * @param columnNames
     * @param newColumnName
     * @param newColumnType
     */
    @Override
    public void combineColumns(final Collection<String> columnNames, final String newColumnName, final Class<?> newColumnType) {
        checkFrozen();

        final List<Object> newColumn = toList(0, size(), columnNames, newColumnType);

        removeColumns(columnNames);

        addColumn(newColumnName, newColumn);
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param newColumnName
     * @param combineFunc
     * @throws E the e
     */
    @Override
    public <E extends Exception> void combineColumns(Collection<String> columnNames, final String newColumnName,
            Throwables.Function<? super DisposableObjArray, ?, E> combineFunc) throws E {
        addColumn(newColumnName, columnNames, combineFunc);

        removeColumns(columnNames);
    }

    /**
     *
     * @param <E>
     * @param columnNameFilter
     * @param newColumnName
     * @param newColumnType
     * @throws E the e
     */
    @Override
    public <E extends Exception> void combineColumns(Throwables.Predicate<String, E> columnNameFilter, final String newColumnName, Class<?> newColumnType)
            throws E {
        combineColumns(N.filter(_columnNameList, columnNameFilter), newColumnName, newColumnType);
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param columnNameFilter
     * @param newColumnName
     * @param combineFunc
     * @throws E the e
     * @throws E2 the e2
     */
    @Override
    public <E extends Exception, E2 extends Exception> void combineColumns(Throwables.Predicate<String, E> columnNameFilter, final String newColumnName,
            Throwables.Function<? super DisposableObjArray, ?, E2> combineFunc) throws E, E2 {
        combineColumns(N.filter(_columnNameList, columnNameFilter), newColumnName, combineFunc);
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param newColumnName
     * @param combineFunc
     * @throws E the e
     */
    @Override
    public <E extends Exception> void combineColumns(Tuple2<String, String> columnNames, final String newColumnName,
            Throwables.BiFunction<?, ?, ?, E> combineFunc) throws E {
        addColumn(newColumnName, columnNames, combineFunc);

        removeColumns(Arrays.asList(columnNames._1, columnNames._2));
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param newColumnName
     * @param combineFunc
     * @throws E the e
     */
    @Override
    public <E extends Exception> void combineColumns(Tuple3<String, String, String> columnNames, final String newColumnName,
            Throwables.TriFunction<?, ?, ?, ?, E> combineFunc) throws E {
        addColumn(newColumnName, columnNames, combineFunc);

        removeColumns(Arrays.asList(columnNames._1, columnNames._2, columnNames._3));
    }

    /**
     *
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param divideFunc
     * @throws E the e
     */
    @Override
    public <E extends Exception> void divideColumn(final String columnName, Collection<String> newColumnNames,
            Throwables.Function<?, ? extends List<?>, E> divideFunc) throws E {
        checkFrozen();

        final int columnIndex = this.checkColumnName(columnName);

        if (N.isEmpty(newColumnNames)) {
            throw new IllegalArgumentException("New column names can't be null or empty.");
        }

        if (!N.disjoint(_columnNameList, newColumnNames)) {
            throw new IllegalArgumentException("Column names: " + N.intersection(_columnNameList, newColumnNames) + " already are included in this data set.");
        }

        @SuppressWarnings("rawtypes")
        final Throwables.Function<Object, List<Object>, E> divideFunc2 = (Throwables.Function) divideFunc;
        final int newColumnsLen = newColumnNames.size();
        final List<List<Object>> newColumns = new ArrayList<>(newColumnsLen);

        for (int i = 0; i < newColumnsLen; i++) {
            newColumns.add(new ArrayList<>(size()));
        }

        final List<Object> column = _columnList.get(columnIndex);

        for (Object val : column) {
            final List<Object> newVals = divideFunc2.apply(val);

            for (int i = 0; i < newColumnsLen; i++) {
                newColumns.get(i).add(newVals.get(i));
            }
        }

        _columnNameList.remove(columnIndex);
        _columnNameList.addAll(columnIndex, newColumnNames);

        _columnList.remove(columnIndex);
        _columnList.addAll(columnIndex, newColumns);

        _columnIndexMap = null;
        _columnIndexes = null;

        modCount++;
    }

    /**
     *
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param output
     * @throws E the e
     */
    @Override
    public <E extends Exception> void divideColumn(final String columnName, Collection<String> newColumnNames, Throwables.BiConsumer<?, Object[], E> output)
            throws E {
        checkFrozen();

        final int columnIndex = this.checkColumnName(columnName);

        if (N.isEmpty(newColumnNames)) {
            throw new IllegalArgumentException("New column names can't be null or empty.");
        }

        if (!N.disjoint(_columnNameList, newColumnNames)) {
            throw new IllegalArgumentException("Column names: " + N.intersection(_columnNameList, newColumnNames) + " already are included in this data set.");
        }

        @SuppressWarnings("rawtypes")
        final Throwables.BiConsumer<Object, Object[], E> output2 = (Throwables.BiConsumer) output;
        final int newColumnsLen = newColumnNames.size();
        final List<List<Object>> newColumns = new ArrayList<>(newColumnsLen);

        for (int i = 0; i < newColumnsLen; i++) {
            newColumns.add(new ArrayList<>(size()));
        }

        final List<Object> column = _columnList.get(columnIndex);
        final Object[] tmp = new Object[newColumnsLen];

        for (Object val : column) {
            output2.accept(val, tmp);

            for (int i = 0; i < newColumnsLen; i++) {
                newColumns.get(i).add(tmp[i]);
            }
        }

        _columnNameList.remove(columnIndex);
        _columnNameList.addAll(columnIndex, newColumnNames);

        _columnList.remove(columnIndex);
        _columnList.addAll(columnIndex, newColumns);

        _columnIndexMap = null;
        _columnIndexes = null;

        modCount++;
    }

    /**
     *
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param output
     * @throws E the e
     */
    @Override
    public <E extends Exception> void divideColumn(final String columnName, final Tuple2<String, String> newColumnNames,
            final Throwables.BiConsumer<?, Pair<Object, Object>, E> output) throws E {
        checkFrozen();

        final int columnIndex = this.checkColumnName(columnName);
        this.checkNewColumnName(newColumnNames._1);
        this.checkNewColumnName(newColumnNames._2);

        @SuppressWarnings("rawtypes")
        final Throwables.BiConsumer<Object, Pair<Object, Object>, E> output2 = (Throwables.BiConsumer) output;
        final List<Object> newColumn1 = new ArrayList<>(size());
        final List<Object> newColumn2 = new ArrayList<>(size());

        final List<Object> column = _columnList.get(columnIndex);
        final Pair<Object, Object> tmp = new Pair<>();

        for (Object val : column) {
            output2.accept(val, tmp);

            newColumn1.add(tmp.left);
            newColumn2.add(tmp.right);
        }

        _columnNameList.remove(columnIndex);
        _columnNameList.addAll(columnIndex, Arrays.asList(newColumnNames._1, newColumnNames._2));

        _columnList.remove(columnIndex);
        _columnList.addAll(columnIndex, Arrays.asList(newColumn1, newColumn2));

        _columnIndexMap = null;
        _columnIndexes = null;

        modCount++;
    }

    /**
     *
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param output
     * @throws E the e
     */
    @Override
    public <E extends Exception> void divideColumn(final String columnName, final Tuple3<String, String, String> newColumnNames,
            final Throwables.BiConsumer<?, Triple<Object, Object, Object>, E> output) throws E {
        checkFrozen();

        final int columnIndex = this.checkColumnName(columnName);
        this.checkNewColumnName(newColumnNames._1);
        this.checkNewColumnName(newColumnNames._2);
        this.checkNewColumnName(newColumnNames._3);

        @SuppressWarnings("rawtypes")
        final Throwables.BiConsumer<Object, Triple<Object, Object, Object>, E> output2 = (Throwables.BiConsumer) output;
        final List<Object> newColumn1 = new ArrayList<>(size());
        final List<Object> newColumn2 = new ArrayList<>(size());
        final List<Object> newColumn3 = new ArrayList<>(size());

        final List<Object> column = _columnList.get(columnIndex);
        final Triple<Object, Object, Object> tmp = new Triple<>();

        for (Object val : column) {
            output2.accept(val, tmp);

            newColumn1.add(tmp.left);
            newColumn2.add(tmp.middle);
            newColumn3.add(tmp.right);
        }

        _columnNameList.remove(columnIndex);
        _columnNameList.addAll(columnIndex, Arrays.asList(newColumnNames._1, newColumnNames._2, newColumnNames._3));

        _columnList.remove(columnIndex);
        _columnList.addAll(columnIndex, Arrays.asList(newColumn1, newColumn2, newColumn3));

        _columnIndexMap = null;
        _columnIndexes = null;

        modCount++;
    }

    /**
     * Adds the row.
     *
     * @param row
     */
    @Override
    public void addRow(final Object row) {
        addRow(size(), row);
    }

    /**
     * Adds the row.
     *
     * @param newRowPosition
     * @param row
     */
    @Override
    public void addRow(final int newRowPosition, final Object row) {
        checkFrozen();

        if ((newRowPosition < 0) || (newRowPosition > size())) {
            throw new IllegalArgumentException("Invalid row index: " + newRowPosition + ". It must be >= 0 and <= " + size());
        }

        final Class<?> rowClass = row.getClass();
        final Type<?> rowType = N.typeOf(rowClass);

        if (rowType.isObjectArray()) {
            final Object[] a = (Object[]) row;

            if (a.length < this.columnCount()) {
                throw new IllegalArgumentException("The size of array (" + a.length + ") is less than the size of column (" + this.columnCount() + ")");
            }

            if (newRowPosition == size()) {
                for (int i = 0, len = this.columnCount(); i < len; i++) {
                    _columnList.get(i).add(a[i]);
                }
            } else {
                for (int i = 0, len = this.columnCount(); i < len; i++) {
                    _columnList.get(i).add(newRowPosition, a[i]);
                }
            }
        } else if (rowType.isCollection()) {
            final Collection<Object> c = (Collection<Object>) row;

            if (c.size() < this.columnCount()) {
                throw new IllegalArgumentException("The size of collection (" + c.size() + ") is less than the size of column (" + this.columnCount() + ")");
            }

            final Iterator<Object> it = c.iterator();

            if (newRowPosition == size()) {
                for (int i = 0, len = this.columnCount(); i < len; i++) {
                    _columnList.get(i).add(it.next());
                }
            } else {
                for (int i = 0, len = this.columnCount(); i < len; i++) {
                    _columnList.get(i).add(newRowPosition, it.next());
                }
            }
        } else if (rowType.isMap()) {
            final Map<String, Object> map = (Map<String, Object>) row;
            final Object[] a = new Object[this.columnCount()];

            int idx = 0;
            for (String columnName : this._columnNameList) {
                a[idx] = map.get(columnName);

                if (a[idx] == null && !map.containsKey(columnName)) {
                    throw new IllegalArgumentException("Column (" + columnName + ") is not found in map (" + map.keySet() + ")");
                }

                idx++;
            }

            if (newRowPosition == size()) {
                for (int i = 0, len = this.columnCount(); i < len; i++) {
                    _columnList.get(i).add(a[i]);
                }
            } else {
                for (int i = 0, len = this.columnCount(); i < len; i++) {
                    _columnList.get(i).add(newRowPosition, a[i]);
                }
            }
        } else if (rowType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowClass);
            final Object[] a = new Object[this.columnCount()];
            PropInfo propInfo = null;
            int idx = 0;

            for (String columnName : this._columnNameList) {
                propInfo = beanInfo.getPropInfo(columnName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Column (" + columnName + ") is not found in bean (" + rowClass + ")");
                }

                a[idx++] = propInfo.getPropValue(row);
            }

            if (newRowPosition == size()) {
                for (int i = 0, len = this.columnCount(); i < len; i++) {
                    _columnList.get(i).add(a[i]);
                }
            } else {
                for (int i = 0, len = this.columnCount(); i < len; i++) {
                    _columnList.get(i).add(newRowPosition, a[i]);
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and bean class are supported");
        }

        modCount++;
    }

    /**
     * Removes the row.
     *
     * @param rowIndex
     */
    @Override
    public void removeRow(final int rowIndex) {
        checkFrozen();

        this.checkRowNum(rowIndex);

        for (int i = 0, len = this._columnList.size(); i < len; i++) {
            _columnList.get(i).remove(rowIndex);
        }

        modCount++;
    }

    /**
     * Removes the rows.
     *
     * @param indices
     */
    @Override
    @SafeVarargs
    public final void removeRows(int... indices) {
        checkFrozen();

        for (int rowIndex : indices) {
            this.checkRowNum(rowIndex);
        }

        for (int i = 0, len = this._columnList.size(); i < len; i++) {
            N.deleteAllByIndices(_columnList.get(i), indices);
        }

        modCount++;
    }

    /**
     * Removes the row range.
     *
     * @param inclusiveFromRowIndex
     * @param exclusiveToRowIndex
     */
    @Override
    public void removeRowRange(int inclusiveFromRowIndex, int exclusiveToRowIndex) {
        checkFrozen();

        this.checkRowIndex(inclusiveFromRowIndex, exclusiveToRowIndex);

        for (int i = 0, len = this._columnList.size(); i < len; i++) {
            _columnList.get(i).subList(inclusiveFromRowIndex, exclusiveToRowIndex).clear();
        }

        modCount++;
    }

    /**
     *
     * @param <E>
     * @param rowIndex
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void updateRow(int rowIndex, Throwables.Function<?, ?, E> func) throws E {
        checkFrozen();

        this.checkRowNum(rowIndex);

        final Throwables.Function<Object, Object, E> func2 = (Throwables.Function<Object, Object, E>) func;

        for (List<Object> column : _columnList) {
            column.set(rowIndex, func2.apply(column.get(rowIndex)));
        }

        modCount++;
    }

    /**
     *
     * @param <E>
     * @param indices
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void updateRows(int[] indices, Throwables.Function<?, ?, E> func) throws E {
        checkFrozen();

        for (int rowIndex : indices) {
            this.checkRowNum(rowIndex);
        }

        final Throwables.Function<Object, Object, E> func2 = (Throwables.Function<Object, Object, E>) func;

        for (List<Object> column : _columnList) {
            for (int rowIndex : indices) {
                column.set(rowIndex, func2.apply(column.get(rowIndex)));
            }
        }

        modCount++;
    }

    /**
     *
     * @param <E>
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void updateAll(Throwables.Function<?, ?, E> func) throws E {
        checkFrozen();

        final Throwables.Function<Object, Object, E> func2 = (Throwables.Function<Object, Object, E>) func;
        final int size = size();

        for (List<Object> column : _columnList) {
            for (int i = 0; i < size; i++) {
                column.set(i, func2.apply(column.get(i)));
            }
        }

        modCount++;
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @param newValue
     * @throws E the e
     */
    @Override
    public <E extends Exception> void replaceIf(Throwables.Predicate<?, E> predicate, Object newValue) throws E {
        checkFrozen();

        @SuppressWarnings("rawtypes")
        final Throwables.Predicate<Object, E> Predicate2 = (Throwables.Predicate) predicate;
        final int size = size();
        Object val = null;

        for (List<Object> column : _columnList) {
            for (int i = 0; i < size; i++) {
                val = column.get(i);

                column.set(i, Predicate2.test(val) ? newValue : val);
            }
        }

        modCount++;
    }

    /**
     *
     *
     * @param other
     */
    @Override
    public void prepend(final DataSet other) {
        checkFrozen();
        checkIfColumnNamesAreSame(other, true);

        final int[] columnIndexesForOther = getColumnIndexes(other.columnNameList());

        for (int i = 0, len = columnIndexesForOther.length; i < len; i++) {
            this._columnList.get(columnIndexesForOther[i]).addAll(0, other.getColumn(i));
        }

        if (N.notEmpty(other.properties())) {
            this.properties().putAll(other.properties());
        }

        modCount++;
    }

    /**
     *
     *
     * @param other
     */
    @Override
    public void append(final DataSet other) {
        checkFrozen();
        checkIfColumnNamesAreSame(other, true);

        final int[] columnIndexesForOther = getColumnIndexes(other.columnNameList());

        for (int i = 0, len = columnIndexesForOther.length; i < len; i++) {
            this._columnList.get(columnIndexesForOther[i]).addAll(other.getColumn(i));
        }

        if (N.notEmpty(other.properties())) {
            this.properties().putAll(other.properties());
        }

        modCount++;
    }

    /**
     * Current row num.
     *
     * @return
     */
    @Override
    public int currentRowNum() {
        return _currentRowNum;
    }

    /**
     *
     * @param rowNum
     * @return
     */
    @Override
    public DataSet absolute(final int rowNum) {
        checkRowNum(rowNum);

        _currentRowNum = rowNum;

        return this;
    }

    /**
     * Gets the row.
     *
     * @param rowIndex
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object[] getRow(final int rowIndex) {
        return getRow(rowIndex, Object[].class);
    }

    /**
     * Gets the row.
     *
     * @param <T> 
     * @param rowIndex 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> T getRow(final int rowIndex, final Class<? extends T> rowType) {
        return getRow(rowIndex, _columnNameList, rowType);
    }

    /**
     * Gets the row.
     *
     * @param <T> 
     * @param rowIndex 
     * @param columnNames 
     * @param rowType 
     * @return 
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getRow(final int rowIndex, final Collection<String> columnNames, final Class<? extends T> rowType) {
        return getRow(rowIndex, columnNames, rowType, null);
    }

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowIndex
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> T getRow(int rowIndex, IntFunction<? extends T> rowSupplier) {
        return getRow(rowIndex, _columnNameList, rowSupplier);
    }

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowIndex
     * @param columnNames
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> T getRow(int rowIndex, Collection<String> columnNames, IntFunction<? extends T> rowSupplier) {
        return getRow(rowIndex, columnNames, null, rowSupplier);
    }

    private <T> T getRow(int rowIndex, Collection<String> columnNames, Class<? extends T> rowClass, IntFunction<? extends T> rowSupplier) {
        checkRowNum(rowIndex);

        columnNames = N.isEmpty(columnNames) ? this._columnNameList : columnNames;
        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;

        rowClass = rowClass == null ? (Class<T>) rowSupplier.apply(0).getClass() : rowClass;
        final Type<T> rowType = N.typeOf(rowClass);
        final BeanInfo beanInfo = rowType.isBean() ? ParserUtil.getBeanInfo(rowClass) : null;

        rowSupplier = rowSupplier == null && !rowType.isBean() ? this.createRowSupplier(rowClass, rowType) : rowSupplier;

        return getRow(beanInfo, rowIndex, columnNames, columnIndexes, columnCount, null, rowClass, rowType, rowSupplier);
    }

    private <T> T getRow(final BeanInfo beanInfo, final int rowIndex, final Collection<String> columnNames, final int[] columnIndexes, final int columnCount,
            final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowClass, final Type<T> rowType,
            final IntFunction<? extends T> rowSupplier) {

        if (rowType.isObjectArray()) {
            final Object[] result = (Object[]) rowSupplier.apply(columnCount);

            for (int i = 0; i < columnCount; i++) {
                result[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            return (T) result;
        } else if (rowType.isCollection()) {
            final Collection<Object> result = (Collection<Object>) rowSupplier.apply(columnCount);

            for (int columnIndex : columnIndexes) {
                result.add(_columnList.get(columnIndex).get(rowIndex));
            }

            return (T) result;
        } else if (rowType.isMap()) {
            final Map<String, Object> result = (Map<String, Object>) rowSupplier.apply(columnCount);

            for (int columnIndex : columnIndexes) {
                result.put(_columnNameList.get(columnIndex), _columnList.get(columnIndex).get(rowIndex));
            }

            return (T) result;
        } else if (rowType.isBean()) {
            final boolean ignoreUnmatchedProperty = columnNames == this._columnNameList;
            Object result = rowSupplier == null ? beanInfo.createBeanResult() : rowSupplier.apply(columnCount);

            Set<String> mergedPropNames = null;
            String propName = null;
            PropInfo propInfo = null;

            for (int i = 0; i < columnCount; i++) {
                propName = _columnNameList.get(columnIndexes[i]);

                if (mergedPropNames != null && mergedPropNames.contains(propName)) {
                    continue;
                }

                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo != null) {
                    propInfo.setPropValue(result, _columnList.get(columnIndexes[i]).get(rowIndex));
                } else {
                    final int idx = propName.indexOf(PROP_NAME_SEPARATOR);

                    if (idx <= 0) {
                        if (ignoreUnmatchedProperty) {
                            continue;
                        }

                        throw new IllegalArgumentException("Property " + propName + " is not found in class: " + rowClass);
                    }

                    final String realPropName = propName.substring(0, idx);
                    propInfo = getPropInfoByPrefix(beanInfo, realPropName, prefixAndFieldNameMap);

                    if (propInfo == null) {
                        if (ignoreUnmatchedProperty) {
                            continue;
                        } else {
                            throw new IllegalArgumentException("Property " + propName + " is not found in class: " + rowClass);
                        }
                    }

                    final Type<Object> propBeanType = propInfo.type.isCollection() ? (Type<Object>) propInfo.type.getElementType() : propInfo.type;

                    if (!propBeanType.isBean()) {
                        throw new UnsupportedOperationException("Property: " + propInfo.name + " in class: " + rowClass + " is not a bean type");
                    }

                    final Class<Object> propBeanClass = propBeanType.clazz();
                    final BeanInfo propBeanInfo = ParserUtil.getBeanInfo(propBeanClass);
                    final List<String> newTmpColumnNameList = new ArrayList<>();
                    final List<List<Object>> newTmpColumnList = new ArrayList<>();

                    if (mergedPropNames == null) {
                        mergedPropNames = new HashSet<>();
                    }

                    String columnName = null;
                    String newColumnName = null;

                    for (int j = i; j < columnCount; j++) {
                        columnName = this._columnNameList.get(columnIndexes[j]);

                        if (mergedPropNames.contains(columnName)) {
                            continue;
                        }

                        if (columnName.length() > idx && columnName.charAt(idx) == PROP_NAME_SEPARATOR && columnName.startsWith(realPropName)) {
                            newColumnName = columnName.substring(idx + 1);
                            newTmpColumnNameList.add(newColumnName);
                            newTmpColumnList.add(_columnList.get(columnIndexes[j]));

                            mergedPropNames.add(columnName);
                        }
                    }

                    final RowDataSet tmp = new RowDataSet(newTmpColumnNameList, newTmpColumnList);

                    final Object propValue = tmp.getRow(propBeanInfo, rowIndex, newTmpColumnNameList, tmp.checkColumnName(newTmpColumnNameList),
                            newTmpColumnNameList.size(), prefixAndFieldNameMap, propBeanClass, propBeanType, null);

                    if (propInfo.type.isCollection()) {
                        @SuppressWarnings("rawtypes")
                        Collection<Object> c = N.newCollection((Class) propInfo.clazz);
                        c.add(propValue);
                        propInfo.setPropValue(result, c);
                    } else {
                        propInfo.setPropValue(result, propValue);
                    }
                }
            }

            if (rowSupplier == null) {
                result = beanInfo.finishBeanResult(result);
            }

            return (T) result;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowType.clazz().getCanonicalName() + ". Only Array, Collection, Map and bean class are supported");
        }
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Optional<Object[]> firstRow() {
        return firstRow(Object[].class);
    }

    /**
     *
     * @param <T>
     * @param rowType
     * @return
     */
    @Override
    public <T> Optional<T> firstRow(final Class<? extends T> rowType) {
        return firstRow(_columnNameList, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param columnNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> Optional<T> firstRow(final Collection<String> columnNames, final Class<? extends T> rowType) {
        return size() == 0 ? (Optional<T>) Optional.empty() : Optional.of(getRow(0, columnNames, rowType));
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> Optional<T> firstRow(IntFunction<? extends T> rowSupplier) {
        return firstRow(_columnNameList, rowSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> Optional<T> firstRow(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) {
        if (size() == 0) {
            return Optional.empty();
        }

        final T row = getRow(0, columnNames, rowSupplier);

        return Optional.of(row);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Optional<Object[]> lastRow() {
        return lastRow(Object[].class);
    }

    /**
     *
     * @param <T>
     * @param rowType
     * @return
     */
    @Override
    public <T> Optional<T> lastRow(final Class<? extends T> rowType) {
        return lastRow(_columnNameList, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param columnNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> Optional<T> lastRow(final Collection<String> columnNames, final Class<? extends T> rowType) {
        return size() == 0 ? (Optional<T>) Optional.empty() : Optional.of(getRow(size() - 1, columnNames, rowType));
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> Optional<T> lastRow(IntFunction<? extends T> rowSupplier) {
        return lastRow(_columnNameList, rowSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> Optional<T> lastRow(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) {
        if (size() == 0) {
            return Optional.empty();
        }

        final T row = getRow(size() - 1, columnNames, rowSupplier);

        return Optional.of(row);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param columnNameA
     * @param columnNameB
     * @return
     */
    @Override
    public <A, B> BiIterator<A, B> iterator(final String columnNameA, final String columnNameB) {
        return iterator(0, size(), columnNameA, columnNameB);
    }

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
    @Override
    public <A, B> BiIterator<A, B> iterator(final int fromRowIndex, final int toRowIndex, final String columnNameA, final String columnNameB) {
        this.checkRowIndex(fromRowIndex, toRowIndex);
        final List<Object> columnA = _columnList.get(checkColumnName(columnNameA));
        final List<Object> columnB = _columnList.get(checkColumnName(columnNameB));

        final IntObjConsumer<Pair<A, B>> output = new IntObjConsumer<>() {
            private final int expectedModCount = modCount;

            @Override
            public void accept(int rowIndex, Pair<A, B> output) {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }

                output.set((A) columnA.get(rowIndex), (B) columnB.get(rowIndex));
            }
        };

        return BiIterator.generate(fromRowIndex, toRowIndex, output);
    }

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
    @Override
    public <A, B, C> TriIterator<A, B, C> iterator(final String columnNameA, final String columnNameB, final String columnNameC) {
        return iterator(0, size(), columnNameA, columnNameB, columnNameC);
    }

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
    @Override
    public <A, B, C> TriIterator<A, B, C> iterator(final int fromRowIndex, final int toRowIndex, final String columnNameA, final String columnNameB,
            final String columnNameC) {
        this.checkRowIndex(fromRowIndex, toRowIndex);
        final List<Object> columnA = _columnList.get(checkColumnName(columnNameA));
        final List<Object> columnB = _columnList.get(checkColumnName(columnNameB));
        final List<Object> columnC = _columnList.get(checkColumnName(columnNameC));

        final IntObjConsumer<Triple<A, B, C>> output = new IntObjConsumer<>() {
            private final int expectedModCount = modCount;

            @Override
            public void accept(int rowIndex, Triple<A, B, C> output) {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }

                output.set((A) columnA.get(rowIndex), (B) columnB.get(rowIndex), (C) columnC.get(rowIndex));
            }
        };

        return TriIterator.generate(fromRowIndex, toRowIndex, output);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void forEach(final Throwables.Consumer<? super DisposableObjArray, E> action) throws E {
        forEach(this._columnNameList, action);
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void forEach(final Collection<String> columnNames, final Throwables.Consumer<? super DisposableObjArray, E> action) throws E {
        forEach(0, size(), columnNames, action);
    }

    /**
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final Throwables.Consumer<? super DisposableObjArray, E> action)
            throws E {
        forEach(fromRowIndex, toRowIndex, this._columnNameList, action);
    }

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
    @Override
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Throwables.Consumer<? super DisposableObjArray, E> action) throws E {
        final int[] columnIndexes = checkColumnName(columnNames);
        checkRowIndex(fromRowIndex < toRowIndex ? fromRowIndex : (toRowIndex == -1 ? 0 : toRowIndex), fromRowIndex < toRowIndex ? toRowIndex : fromRowIndex);
        N.checkArgNotNull(action);

        if (size() == 0) {
            return;
        }

        final int columnCount = columnIndexes.length;
        final Object[] row = new Object[columnCount];
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(row);

        if (fromRowIndex <= toRowIndex) {
            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                for (int i = 0; i < columnCount; i++) {
                    row[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
                }

                action.accept(disposableArray);
            }
        } else {
            for (int rowIndex = N.min(size() - 1, fromRowIndex); rowIndex > toRowIndex; rowIndex--) {
                for (int i = 0; i < columnCount; i++) {
                    row[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
                }

                action.accept(disposableArray);
            }
        }
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void forEach(Tuple2<String, String> columnNames, Throwables.BiConsumer<?, ?, E> action) throws E {
        forEach(0, size(), columnNames, action);
    }

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
    @Override
    public <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, Throwables.BiConsumer<?, ?, E> action)
            throws E {
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));

        checkRowIndex(fromRowIndex < toRowIndex ? fromRowIndex : (toRowIndex == -1 ? 0 : toRowIndex), fromRowIndex < toRowIndex ? toRowIndex : fromRowIndex);
        N.checkArgNotNull(action);

        if (size() == 0) {
            return;
        }

        @SuppressWarnings("rawtypes")
        final Throwables.BiConsumer<Object, Object, E> action2 = (Throwables.BiConsumer) action;

        if (fromRowIndex <= toRowIndex) {
            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                action2.accept(column1.get(rowIndex), column2.get(rowIndex));
            }
        } else {
            for (int rowIndex = N.min(size() - 1, fromRowIndex); rowIndex > toRowIndex; rowIndex--) {
                action2.accept(column1.get(rowIndex), column2.get(rowIndex));
            }
        }
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void forEach(Tuple3<String, String, String> columnNames, Throwables.TriConsumer<?, ?, ?, E> action) throws E {
        forEach(0, size(), columnNames, action);
    }

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
    @Override
    public <E extends Exception> void forEach(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames,
            Throwables.TriConsumer<?, ?, ?, E> action) throws E {
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(columnNames._3));

        checkRowIndex(fromRowIndex < toRowIndex ? fromRowIndex : (toRowIndex == -1 ? 0 : toRowIndex), fromRowIndex < toRowIndex ? toRowIndex : fromRowIndex);
        N.checkArgNotNull(action);

        if (size() == 0) {
            return;
        }

        @SuppressWarnings("rawtypes")
        final Throwables.TriConsumer<Object, Object, Object, E> action2 = (Throwables.TriConsumer) action;

        if (fromRowIndex <= toRowIndex) {
            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                action2.accept(column1.get(rowIndex), column2.get(rowIndex), column3.get(rowIndex));
            }
        } else {
            for (int rowIndex = N.min(size() - 1, fromRowIndex); rowIndex > toRowIndex; rowIndex--) {
                action2.accept(column1.get(rowIndex), column2.get(rowIndex), column3.get(rowIndex));
            }
        }
    }

    /**
     *
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Object[]> toList() {
        return toList(Object[].class);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Object[]> toList(final int fromRowIndex, final int toRowIndex) {
        return toList(fromRowIndex, toRowIndex, Object[].class);
    }

    /**
     *
     * @param <T>
     * @param rowType
     * @return
     */
    @Override
    public <T> List<T> toList(final Class<? extends T> rowType) {
        return toList(0, size(), rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Class<? extends T> rowType) {
        return toList(fromRowIndex, toRowIndex, this._columnNameList, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param columnNames 
     * @param rowType 
     * @return 
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> toList(final Collection<String> columnNames, final Class<? extends T> rowType) {
        return toList(0, size(), columnNames, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, Collection<String> columnNames, final Class<? extends T> rowType) {
        return toList(fromRowIndex, toRowIndex, columnNames, null, rowType, null);
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> List<T> toList(IntFunction<? extends T> rowSupplier) {
        return toList(this._columnNameList, rowSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> List<T> toList(int fromRowIndex, int toRowIndex, IntFunction<? extends T> rowSupplier) {
        return toList(fromRowIndex, toRowIndex, this._columnNameList, rowSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> List<T> toList(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) {
        return toList(0, size(), columnNames, rowSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> List<T> toList(int fromRowIndex, int toRowIndex, Collection<String> columnNames, IntFunction<? extends T> rowSupplier) {
        return toList(fromRowIndex, toRowIndex, columnNames, null, null, rowSupplier);
    }

    private <T> List<T> toList(int fromRowIndex, int toRowIndex, Collection<String> columnNames, final Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> rowClass, IntFunction<? extends T> rowSupplier) {
        checkRowIndex(fromRowIndex, toRowIndex);
        columnNames = N.isEmpty(columnNames) ? this._columnNameList : columnNames;

        rowClass = rowClass == null ? (Class<T>) rowSupplier.apply(0).getClass() : rowClass;
        final Type<?> rowType = N.typeOf(rowClass);

        if (rowType.isBean()) {
            return toEntities(ParserUtil.getBeanInfo(rowClass), fromRowIndex, toRowIndex, null, columnNames, prefixAndFieldNameMap, false, true, rowClass,
                    rowSupplier);
        }

        rowSupplier = rowSupplier == null && !rowType.isBean() ? this.createRowSupplier(rowClass, rowType) : rowSupplier;

        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;
        final int rowCount = toRowIndex - fromRowIndex;

        final List<Object> rowList = new ArrayList<>(rowCount);

        if (rowType.isObjectArray()) {
            Object[] row = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                row = (Object[]) rowSupplier.apply(columnCount);

                for (int i = 0; i < columnCount; i++) {
                    row[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
                }

                rowList.add(row);
            }
        } else if (rowType.isList() || rowType.isSet()) {
            Collection<Object> row = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                row = (Collection<Object>) rowSupplier.apply(columnCount);

                for (int i = 0; i < columnCount; i++) {
                    row.add(_columnList.get(columnIndexes[i]).get(rowIndex));
                }

                rowList.add(row);
            }
        } else if (rowType.isMap()) {
            final String[] mapKeyNames = new String[columnCount];

            for (int i = 0; i < columnCount; i++) {
                mapKeyNames[i] = _columnNameList.get(columnIndexes[i]);
            }

            Map<String, Object> row = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                row = (Map<String, Object>) rowSupplier.apply(columnCount);

                for (int i = 0; i < columnCount; i++) {
                    row.put(mapKeyNames[i], _columnList.get(columnIndexes[i]).get(rowIndex));
                }

                rowList.add(row);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and bean class are supported");
        }

        return (List<T>) rowList;
    }

    @SuppressWarnings("rawtypes")
    private <T> IntFunction<? extends T> createRowSupplier(final Class<? extends T> rowClass, final Type<?> rowType) {
        if (rowType.isObjectArray()) {
            final Class<?> componentType = rowClass.getComponentType();
            return cc -> N.newArray(componentType, cc);
        } else if (rowType.isList() || rowType.isSet()) {
            return (IntFunction<T>) Factory.ofCollection((Class<Collection>) rowClass);

        } else if (rowType.isMap()) {
            return (IntFunction<T>) Factory.ofMap((Class<Map>) rowClass);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and bean class are supported");
        }
    }

    /**
     * 
     *
     * @param <T> 
     * @param <E> 
     * @param <E2> 
     * @param columnNameFilter 
     * @param columnNameConverter 
     * @param rowType 
     * @return 
     * @throws E 
     * @throws E2 
     */
    @Override
    public <T, E extends Exception, E2 extends Exception> List<T> toList(final Throwables.Predicate<? super String, E> columnNameFilter,
            final Throwables.Function<? super String, String, E2> columnNameConverter, final Class<? extends T> rowType) throws E, E2 {
        return toList(0, size(), columnNameFilter, columnNameConverter, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param <E> 
     * @param <E2> 
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNameFilter 
     * @param columnNameConverter 
     * @param rowType 
     * @return 
     * @throws E 
     * @throws E2 
     */
    @Override
    public <T, E extends Exception, E2 extends Exception> List<T> toList(final int fromRowIndex, final int toRowIndex,
            final Throwables.Predicate<? super String, E> columnNameFilter, final Throwables.Function<? super String, String, E2> columnNameConverter,
            final Class<? extends T> rowType) throws E, E2 {
        checkRowIndex(fromRowIndex, toRowIndex);

        if ((columnNameFilter == null || Objects.equals(columnNameFilter, Fnn.alwaysTrue()))
                && (columnNameConverter == null && Objects.equals(columnNameConverter, Fnn.identity()))) {
            return toList(fromRowIndex, toRowIndex, this._columnNameList, rowType);
        }

        @SuppressWarnings("rawtypes")
        final Throwables.Predicate<? super String, E> columnNameFilterToBeUsed = columnNameFilter == null ? (Throwables.Predicate) Fnn.alwaysTrue()
                : columnNameFilter;
        @SuppressWarnings("rawtypes")
        final Throwables.Function<? super String, String, E2> columnNameConverterToBeUsed = columnNameConverter == null ? (Throwables.Function) Fnn.identity()
                : columnNameConverter;

        final List<String> newColumnNameList = new ArrayList<>();
        final List<List<Object>> newColumnList = new ArrayList<>();
        String columnName = null;

        for (int i = 0, columnCount = this.columnCount(); i < columnCount; i++) {
            columnName = _columnNameList.get(i);

            if (columnNameFilterToBeUsed.test(columnName)) {
                newColumnNameList.add(columnNameConverterToBeUsed.apply(columnName));
                newColumnList.add(_columnList.get(i));
            }
        }

        final RowDataSet tmp = new RowDataSet(newColumnNameList, newColumnList);

        return tmp.toList(fromRowIndex, toRowIndex, rowType);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param columnNameFilter
     * @param columnNameConverter
     * @param rowSupplier
     * @return
     * @throws E
     * @throws E2
     */
    @Override
    public <T, E extends Exception, E2 extends Exception> List<T> toList(final Throwables.Predicate<? super String, E> columnNameFilter,
            final Throwables.Function<? super String, String, E2> columnNameConverter, IntFunction<? extends T> rowSupplier) throws E, E2 {
        return toList(0, size(), columnNameFilter, columnNameConverter, rowSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNameFilter
     * @param columnNameConverter
     * @param rowSupplier
     * @return
     * @throws E
     * @throws E2
     */
    @Override
    public <T, E extends Exception, E2 extends Exception> List<T> toList(final int fromRowIndex, final int toRowIndex,
            final Throwables.Predicate<? super String, E> columnNameFilter, final Throwables.Function<? super String, String, E2> columnNameConverter,
            IntFunction<? extends T> rowSupplier) throws E, E2 {
        checkRowIndex(fromRowIndex, toRowIndex);

        if ((columnNameFilter == null || Objects.equals(columnNameFilter, Fnn.alwaysTrue()))
                && (columnNameConverter == null && Objects.equals(columnNameConverter, Fnn.identity()))) {
            return toList(fromRowIndex, toRowIndex, this._columnNameList, rowSupplier);
        }

        @SuppressWarnings("rawtypes")
        final Throwables.Predicate<? super String, E> columnNameFilterToBeUsed = columnNameFilter == null ? (Throwables.Predicate) Fnn.alwaysTrue()
                : columnNameFilter;
        @SuppressWarnings("rawtypes")
        final Throwables.Function<? super String, String, E2> columnNameConverterToBeUsed = columnNameConverter == null ? (Throwables.Function) Fnn.identity()
                : columnNameConverter;

        final List<String> newColumnNameList = new ArrayList<>();
        final List<List<Object>> newColumnList = new ArrayList<>();
        String columnName = null;

        for (int i = 0, columnCount = this.columnCount(); i < columnCount; i++) {
            columnName = _columnNameList.get(i);

            if (columnNameFilterToBeUsed.test(columnName)) {
                newColumnNameList.add(columnNameConverterToBeUsed.apply(columnName));
                newColumnList.add(_columnList.get(i));
            }
        }

        final RowDataSet tmp = new RowDataSet(newColumnNameList, newColumnList);

        return tmp.toList(fromRowIndex, toRowIndex, rowSupplier);
    }

    /**
     * 
     *
     * @param <T> 
     * @param prefixAndFieldNameMap 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> List<T> toEntities(Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType) {
        return toEntities(0, size(), this._columnNameList, prefixAndFieldNameMap, rowType);
    }

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
    @Override
    public <T> List<T> toEntities(int fromRowIndex, int toRowIndex, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType) {
        return toEntities(fromRowIndex, toRowIndex, this._columnNameList, prefixAndFieldNameMap, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param columnNames 
     * @param prefixAndFieldNameMap 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> List<T> toEntities(Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType) {
        return toEntities(0, size(), columnNames, prefixAndFieldNameMap, rowType);
    }

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
    @Override
    public <T> List<T> toEntities(int fromRowIndex, int toRowIndex, Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> rowType) {
        N.checkArgument(ClassUtil.isBeanClass(rowType), "{} is not a bean class", rowType);

        return toList(fromRowIndex, toRowIndex, columnNames, prefixAndFieldNameMap, rowType, null);
    }

    /**
     *
     *
     * @param <T>
     * @param rowType
     * @return
     */
    @Override
    public <T> List<T> toMergedEntities(final Class<? extends T> rowType) {
        return toMergedEntities(this._columnNameList, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param selectPropNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> List<T> toMergedEntities(final Collection<String> selectPropNames, final Class<? extends T> rowType) {
        return toMergedEntities((Collection<String>) null, selectPropNames, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param idPropName 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> List<T> toMergedEntities(final String idPropName, final Class<? extends T> rowType) {
        return toMergedEntities(idPropName, this._columnNameList, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param idPropName 
     * @param selectPropNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> List<T> toMergedEntities(final String idPropName, final Collection<String> selectPropNames, final Class<? extends T> rowType) {
        return toMergedEntities(N.asList(idPropName), selectPropNames, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param idPropNames 
     * @param selectPropNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> List<T> toMergedEntities(final Collection<String> idPropNames, final Collection<String> selectPropNames, final Class<? extends T> rowType) {
        return toMergedEntities(idPropNames, selectPropNames, null, rowType);
    }

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
    @Override
    public <T> List<T> toMergedEntities(final Collection<String> idPropNames, Collection<String> selectPropNames,
            final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType) {
        N.checkArgument(ClassUtil.isBeanClass(rowType), "{} is not a bean class", rowType);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowType);

        Collection<String> idPropNamesToUse = idPropNames;

        if (idPropNames == null) {
            idPropNamesToUse = beanInfo.idPropNameList;

            if (N.isEmpty(idPropNamesToUse)) {
                throw new IllegalArgumentException("No id property defined in class: " + rowType);
            }
        }

        N.checkArgNotNull(idPropNamesToUse, "idPropNames");

        if (!this._columnNameList.containsAll(idPropNamesToUse)) {
            final List<String> tmp = new ArrayList<>(idPropNamesToUse.size());
            PropInfo propInfo = null;

            outer: for (String idPropName : idPropNamesToUse) { //NOSONAR
                if (this._columnNameList.contains(idPropName)) {
                    tmp.add(idPropName);
                } else {
                    propInfo = beanInfo.getPropInfo(idPropName);

                    if (propInfo != null && propInfo.columnName.isPresent() && this._columnNameList.contains(propInfo.columnName.get())) {
                        tmp.add(propInfo.columnName.get());
                    } else {
                        for (String columnName : this._columnNameList) {
                            if (columnName.equalsIgnoreCase(idPropName)) {
                                tmp.add(columnName);

                                continue outer;
                            }
                        }

                        if (propInfo != null) {
                            for (String columnName : this._columnNameList) {
                                if (propInfo.equals(beanInfo.getPropInfo(columnName))) {
                                    tmp.add(columnName);

                                    continue outer;
                                }
                            }
                        }

                        tmp.add(idPropName);
                        break;
                    }
                }
            }

            if (this._columnNameList.containsAll(tmp)) {
                idPropNamesToUse = tmp;
            }
        }

        N.checkArgument(this._columnNameList.containsAll(idPropNamesToUse), "Some id properties {} are not found in DataSet: {} for bean {}", idPropNamesToUse,
                this._columnNameList, ClassUtil.getSimpleClassName(rowType));

        selectPropNames = N.isEmpty(selectPropNames) ? this._columnNameList : selectPropNames;

        N.checkArgument(N.isEmpty(selectPropNames) || selectPropNames == this._columnNameList || this._columnNameList.containsAll(selectPropNames),
                "Some select properties {} are not found in DataSet: {} for bean {}", selectPropNames, this._columnNameList,
                ClassUtil.getSimpleClassName(rowType));

        return toEntities(beanInfo, 0, size(), idPropNamesToUse, selectPropNames, prefixAndFieldNameMap, true, false, rowType, null);
    }

    @SuppressWarnings("rawtypes")
    private <T> List<T> toEntities(final BeanInfo beanInfo, final int fromRowIndex, final int toRowIndex, final Collection<String> idPropNames,
            final Collection<String> columnNames, final Map<String, String> prefixAndFieldNameMap, boolean mergeResult, final boolean returnAllList,
            final Class<? extends T> rowType, final IntFunction<? extends T> rowSupplier) {
        N.checkArgNotNull(rowType, "rowType");
        checkRowIndex(fromRowIndex, toRowIndex);

        if (mergeResult && N.isEmpty(idPropNames)) {
            throw new IllegalArgumentException("\"idPropNames\" can't be null or empty when \"mergeResult\" is true");
        }

        final int rowCount = toRowIndex - fromRowIndex;
        final int columnCount = columnNames.size();
        final int[] idColumnIndexes = N.isEmpty(idPropNames) ? N.EMPTY_INT_ARRAY : checkColumnName(idPropNames);
        final boolean ignoreUnmatchedProperty = columnNames == this._columnNameList;

        final Object[] resultEntities = new Object[rowCount];
        final Map<Object, Object> idBeanMap = mergeResult ? N.newLinkedHashMap(N.min(64, rowCount)) : N.emptyMap();
        Object bean = null;

        if (N.isEmpty(idColumnIndexes)) {
            for (int rowIndex = fromRowIndex, i = 0; rowIndex < toRowIndex; rowIndex++, i++) {
                resultEntities[i] = rowSupplier == null ? beanInfo.createBeanResult() : rowSupplier.apply(columnCount);
            }
        } else if (idColumnIndexes.length == 1) {
            final List<Object> idColumn = _columnList.get(idColumnIndexes[0]);
            Object rowKey = null;
            Object key = null;

            for (int rowIndex = fromRowIndex, i = 0; rowIndex < toRowIndex; rowIndex++, i++) {
                key = idColumn.get(rowIndex);

                if (key == null) {
                    continue;
                }

                rowKey = hashKey(key);
                bean = idBeanMap.get(rowKey);

                if (bean == null) {
                    bean = rowSupplier == null ? beanInfo.createBeanResult() : rowSupplier.apply(columnCount);
                    idBeanMap.put(rowKey, bean);
                }

                resultEntities[i] = bean;
            }
        } else {
            final int idColumnCount = idColumnIndexes.length;

            Object[] keyRow = Objectory.createObjectArray(idColumnCount);
            Wrapper<Object[]> rowKey = null;
            boolean isAllKeyNull = true;

            for (int rowIndex = fromRowIndex, i = 0; rowIndex < toRowIndex; rowIndex++, i++) {
                isAllKeyNull = true;

                for (int j = 0; j < idColumnCount; j++) {
                    keyRow[j] = _columnList.get(idColumnIndexes[j]).get(rowIndex);

                    if (keyRow[j] != null) {
                        isAllKeyNull = false;
                    }
                }

                if (isAllKeyNull) {
                    continue;
                }

                rowKey = Wrapper.of(keyRow);
                bean = idBeanMap.get(rowKey);

                if (bean == null) {
                    bean = rowSupplier == null ? beanInfo.createBeanResult() : rowSupplier.apply(columnCount);
                    idBeanMap.put(rowKey, bean);

                    keyRow = Objectory.createObjectArray(idColumnCount);
                }

                resultEntities[i] = bean;
            }

            if (keyRow != null) {
                Objectory.recycle(keyRow);
                keyRow = null;
            }
        }

        List<Collection<Object>> listPropValuesToDeduplicate = null;

        try {
            final Set<String> mergedPropNames = new HashSet<>();
            List<Object> curColumn = null;
            int curColumnIndex = 0;
            PropInfo propInfo = null;

            for (String propName : columnNames) {
                if (mergedPropNames.contains(propName)) {
                    continue;
                }

                curColumnIndex = checkColumnName(propName);
                curColumn = _columnList.get(curColumnIndex);

                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo != null) {
                    boolean isPropValueChecked = false;
                    boolean isPropValueAssignable = false;
                    Object value = null;

                    for (int rowIndex = fromRowIndex, i = 0; rowIndex < toRowIndex; rowIndex++, i++) {
                        if (resultEntities[i] != null) {
                            if (isPropValueChecked) {
                                if (isPropValueAssignable) {
                                    propInfo.setPropValue(resultEntities[i], curColumn.get(rowIndex));
                                } else {
                                    propInfo.setPropValue(resultEntities[i], N.convert(curColumn.get(rowIndex), propInfo.jsonXmlType));
                                }
                            } else {
                                value = curColumn.get(rowIndex);

                                if (value == null) {
                                    propInfo.setPropValue(resultEntities[i], propInfo.jsonXmlType.defaultValue());
                                } else {
                                    if (propInfo.clazz.isAssignableFrom(value.getClass())) {
                                        propInfo.setPropValue(resultEntities[i], value);
                                        isPropValueAssignable = true;
                                    } else {
                                        propInfo.setPropValue(resultEntities[i], N.convert(value, propInfo.jsonXmlType));
                                        isPropValueAssignable = false;
                                    }

                                    isPropValueChecked = true;
                                }
                            }
                        }
                    }

                    mergedPropNames.add(propName);
                } else {
                    final int idx = propName.indexOf(PROP_NAME_SEPARATOR);

                    if (idx <= 0) {
                        if (ignoreUnmatchedProperty) {
                            continue;
                        }

                        throw new IllegalArgumentException("Property " + propName + " is not found in class: " + rowType);
                    }

                    final String prefix = propName.substring(0, idx);
                    propInfo = getPropInfoByPrefix(beanInfo, prefix, prefixAndFieldNameMap);

                    if (propInfo == null) {
                        if (ignoreUnmatchedProperty) {
                            continue;
                        } else {
                            throw new IllegalArgumentException("Property " + propName + " is not found in class: " + rowType);
                        }
                    }

                    final Type<?> propBeanType = propInfo.type.isCollection() ? propInfo.type.getElementType() : propInfo.type;

                    if (!propBeanType.isBean()) {
                        throw new UnsupportedOperationException("Property: " + propInfo.name + " in class: " + rowType + " is not a bean type");
                    }

                    final Class<?> propBeanClass = propBeanType.clazz();
                    final BeanInfo propBeanInfo = ParserUtil.getBeanInfo(propBeanClass);
                    final List<String> propEntityIdPropNames = mergeResult ? propBeanInfo.idPropNameList : null;
                    final List<String> newPropEntityIdNames = mergeResult && N.isEmpty(propEntityIdPropNames) ? new ArrayList<>() : propEntityIdPropNames;
                    final List<String> newTmpColumnNameList = new ArrayList<>();
                    final List<List<Object>> newTmpColumnList = new ArrayList<>();

                    String newColumnName = null;
                    int columnIndex = 0;

                    for (String columnName : columnNames) {
                        if (mergedPropNames.contains(columnName)) {
                            continue;
                        }

                        columnIndex = checkColumnName(columnName);

                        if (columnName.length() > idx && columnName.charAt(idx) == PROP_NAME_SEPARATOR && columnName.startsWith(prefix)) {
                            newColumnName = columnName.substring(idx + 1);
                            newTmpColumnNameList.add(newColumnName);
                            newTmpColumnList.add(_columnList.get(columnIndex));

                            mergedPropNames.add(columnName);

                            if (mergeResult && N.isEmpty(propEntityIdPropNames) && newColumnName.indexOf(PROP_NAME_SEPARATOR) < 0) {
                                newPropEntityIdNames.add(newColumnName);
                            }
                        }
                    }

                    final RowDataSet tmp = new RowDataSet(newTmpColumnNameList, newTmpColumnList);

                    final boolean isToMerge = mergeResult && N.notEmpty(newPropEntityIdNames) && tmp._columnNameList.containsAll(newPropEntityIdNames);

                    final List<?> propValueList = tmp.toEntities(propBeanInfo, fromRowIndex, toRowIndex, isToMerge ? newPropEntityIdNames : null,
                            tmp._columnNameList, prefixAndFieldNameMap, isToMerge, true, propBeanClass, null);

                    if (propInfo.type.isCollection()) {
                        Collection<Object> c = null;

                        for (int rowIndex = fromRowIndex, i = 0; rowIndex < toRowIndex; rowIndex++, i++) {
                            if (resultEntities[i] == null || propValueList.get(i) == null) {
                                continue;
                            }

                            c = propInfo.getPropValue(resultEntities[i]);

                            if (c == null) {
                                c = N.newCollection((Class) propInfo.clazz);
                                propInfo.setPropValue(resultEntities[i], c);

                                if (isToMerge && !(c instanceof Set)) {
                                    if (listPropValuesToDeduplicate == null) {
                                        listPropValuesToDeduplicate = new ArrayList<>();
                                    }

                                    listPropValuesToDeduplicate.add(c);
                                }
                            }

                            c.add(propValueList.get(i));
                        }
                    } else {
                        for (int rowIndex = fromRowIndex, i = 0; rowIndex < toRowIndex; rowIndex++, i++) {
                            if (resultEntities[i] == null || propValueList.get(i) == null) {
                                continue;
                            }

                            propInfo.setPropValue(resultEntities[i], propValueList.get(i));
                        }
                    }
                }
            }

            if (N.notEmpty(listPropValuesToDeduplicate)) {
                for (Collection<Object> list : listPropValuesToDeduplicate) {
                    N.removeDuplicates(list);
                }
            }

            final List<T> result = returnAllList || N.isEmpty(idBeanMap) ? (List<T>) N.asList(resultEntities)
                    : new ArrayList<>((Collection<T>) idBeanMap.values());

            if (rowSupplier == null && N.notEmpty(result)) {
                for (int i = 0, size = result.size(); i < size; i++) {
                    result.set(i, beanInfo.finishBeanResult(result.get(i)));
                }
            }

            return result;
        } finally {
            if (N.len(idColumnIndexes) > 1 && N.notEmpty(idBeanMap)) {
                for (Wrapper<Object[]> e : ((Map<Wrapper<Object[]>, Object>) ((Map) idBeanMap)).keySet()) {
                    Objectory.recycle(e.value());
                }
            }
        }
    }

    private PropInfo getPropInfoByPrefix(final BeanInfo beanInfo, final String prefix, final Map<String, String> prefixAndFieldNameMap) {
        PropInfo propInfo = beanInfo.getPropInfo(prefix);

        if (propInfo == null && N.notEmpty(prefixAndFieldNameMap) && prefixAndFieldNameMap.containsKey(prefix)) {
            propInfo = beanInfo.getPropInfo(prefixAndFieldNameMap.get(prefix));
        }

        if (propInfo == null) {
            propInfo = beanInfo.getPropInfo(prefix + "s"); // Trying to do something smart?
            final int len = prefix.length() + 1;

            if (propInfo != null && (propInfo.type.isBean() || (propInfo.type.isCollection() && propInfo.type.getElementType().isBean()))
                    && N.noneMatch(this._columnNameList, it -> it.length() > len && it.charAt(len) == '.' && Strings.startsWithIgnoreCase(it, prefix + "s."))) {
                // good
            } else {
                propInfo = beanInfo.getPropInfo(prefix + "es"); // Trying to do something smart?
                final int len2 = prefix.length() + 2;

                if (propInfo != null && (propInfo.type.isBean() || (propInfo.type.isCollection() && propInfo.type.getElementType().isBean())) && N.noneMatch(
                        this._columnNameList, it -> it.length() > len2 && it.charAt(len2) == '.' && Strings.startsWithIgnoreCase(it, prefix + "es."))) {
                    // good
                } else {
                    // Sorry, have done all I can do.
                    propInfo = null;
                }
            }
        }

        return propInfo;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyColumnName
     * @param valueColumnName
     * @return
     */
    @Override
    public <K, V> Map<K, V> toMap(final String keyColumnName, final String valueColumnName) {
        return toMap(0, size(), keyColumnName, valueColumnName);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <M>
     * @param keyColumnName
     * @param valueColumnName
     * @param supplier
     * @return
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(String keyColumnName, String valueColumnName, IntFunction<? extends M> supplier) {
        return toMap(0, size(), keyColumnName, valueColumnName, supplier);
    }

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
    @Override
    public <K, V> Map<K, V> toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final String valueColumnName) {
        return toMap(fromRowIndex, toRowIndex, keyColumnName, valueColumnName, (IntFunction<Map<K, V>>) N::newLinkedHashMap);
    }

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
    @Override
    public <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, String valueColumnName,
            IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int valueColumnIndex = checkColumnName(valueColumnName);

        final M resultMap = supplier.apply(toRowIndex - fromRowIndex);

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (V) _columnList.get(valueColumnIndex).get(rowIndex));
        }

        return resultMap;
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyColumnName 
     * @param valueColumnNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <K, V> Map<K, V> toMap(final String keyColumnName, final Collection<String> valueColumnNames, final Class<? extends V> rowType) {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowType);
    }

    /**
     * 
     *
     * @param <K> 
     * @param <V> 
     * @param <M> 
     * @param keyColumnName 
     * @param valueColumnNames 
     * @param rowType 
     * @param supplier 
     * @return 
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(String keyColumnName, Collection<String> valueColumnNames, Class<? extends V> rowType,
            IntFunction<? extends M> supplier) {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowType, supplier);
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param keyColumnName 
     * @param valueColumnNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <K, V> Map<K, V> toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final Collection<String> valueColumnNames,
            final Class<? extends V> rowType) {
        return toMap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowType, (IntFunction<Map<K, V>>) N::newLinkedHashMap);
    }

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
     * @param rowType 
     * @param supplier 
     * @return 
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            Class<? extends V> rowType, IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnName(valueColumnNames);

        final Type<?> valueType = N.typeOf(rowType);
        final int valueColumnCount = valueColumnIndexes.length;
        final Map<Object, Object> resultMap = (Map<Object, Object>) supplier.apply(toRowIndex - fromRowIndex);

        if (valueType.isObjectArray()) {
            Object[] value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = N.newArray(rowType.getComponentType(), valueColumnCount);

                for (int i = 0; i < valueColumnCount; i++) {
                    value[i] = _columnList.get(valueColumnIndexes[i]).get(rowIndex);
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isList() || valueType.isSet()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowType.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType);
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Collection<Object>) (isAbstractRowClass ? (valueType.isList() ? new ArrayList<>(valueColumnCount) : N.newHashSet(valueColumnCount))
                        : ((intConstructor == null) ? ClassUtil.invokeConstructor(constructor)
                                : ClassUtil.invokeConstructor(intConstructor, valueColumnCount)));

                for (int columIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isMap()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowType.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType);
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Map<String, Object>) (isAbstractRowClass ? N.newHashMap(valueColumnCount)
                        : (intConstructor == null ? ClassUtil.invokeConstructor(constructor) : ClassUtil.invokeConstructor(intConstructor, valueColumnCount)));

                for (int columIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columIndex), _columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowType);
            final boolean ignoreUnmatchedProperty = valueColumnNames == _columnNameList;
            Object value = null;
            String propName = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = beanInfo.createBeanResult();

                for (int columIndex : valueColumnIndexes) {
                    propName = _columnNameList.get(columIndex);
                    beanInfo.setPropValue(value, propName, _columnList.get(columIndex).get(rowIndex), ignoreUnmatchedProperty);
                }

                value = beanInfo.finishBeanResult(value);

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowType.getCanonicalName() + ". Only Array, List/Set, Map and bean class are supported");
        }

        return (M) resultMap;
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier
     * @return
     */
    @Override
    public <K, V> Map<K, V> toMap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends V> rowSupplier) {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowSupplier);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <M>
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier
     * @param supplier
     * @return
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends V> rowSupplier,
            IntFunction<? extends M> supplier) {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowSupplier, supplier);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier
     * @return
     */
    @Override
    public <K, V> Map<K, V> toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends V> rowSupplier) {
        return toMap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowSupplier, (IntFunction<Map<K, V>>) N::newLinkedHashMap);
    }

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
     * @param rowSupplier
     * @param supplier
     * @return
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends V> rowSupplier, IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnName(valueColumnNames);

        final Class<V> rowClass = (Class<V>) rowSupplier.apply(0).getClass();
        final Type<?> valueType = N.typeOf(rowClass);
        final int valueColumnCount = valueColumnIndexes.length;
        final Map<Object, Object> resultMap = (Map<Object, Object>) supplier.apply(toRowIndex - fromRowIndex);

        if (valueType.isObjectArray()) {
            Object[] value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Object[]) rowSupplier.apply(valueColumnCount);

                for (int i = 0; i < valueColumnCount; i++) {
                    value[i] = _columnList.get(valueColumnIndexes[i]).get(rowIndex);
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isList() || valueType.isSet()) {
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Collection<Object>) rowSupplier.apply(valueColumnCount);

                for (int columIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isMap()) {
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Map<String, Object>) rowSupplier.apply(valueColumnCount);

                for (int columIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columIndex), _columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isBean()) {
            final boolean ignoreUnmatchedProperty = valueColumnNames == _columnNameList;
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowClass);
            Object value = null;
            String propName = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = rowSupplier.apply(valueColumnCount);

                for (int columIndex : valueColumnIndexes) {
                    propName = _columnNameList.get(columIndex);
                    beanInfo.setPropValue(value, propName, _columnList.get(columIndex).get(rowIndex), ignoreUnmatchedProperty);
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowClass.getCanonicalName() + ". Only Array, List/Set, Map and bean class are supported");
        }

        return (M) resultMap;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param keyColumnName
     * @param valueColumnName
     * @return
     */
    @Override
    public <K, E> ListMultimap<K, E> toMultimap(final String keyColumnName, final String valueColumnName) {
        return toMultimap(0, size(), keyColumnName, valueColumnName);
    }

    /**
     *
     *
     * @param <K>
     * @param <E>
     * @param <V>
     * @param <M>
     * @param keyColumnName
     * @param valueColumnName
     * @param supplier
     * @return
     */
    @Override
    public <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(String keyColumnName, String valueColumnName,
            IntFunction<? extends M> supplier) {
        return toMultimap(0, size(), keyColumnName, valueColumnName, supplier);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnName
     * @return
     */
    @Override
    public <K, E> ListMultimap<K, E> toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final String valueColumnName) {
        return toMultimap(fromRowIndex, toRowIndex, keyColumnName, valueColumnName, (IntFunction<ListMultimap<K, E>>) len -> N.newLinkedListMultimap());
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnName
     * @param supplier
     * @return
     */
    @Override
    public <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName,
            String valueColumnName, IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final M resultMap = supplier.apply(toRowIndex - fromRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int valueColumnIndex = checkColumnName(valueColumnName);

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) _columnList.get(valueColumnIndex).get(rowIndex));
        }

        return resultMap;
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <E> 
     * @param keyColumnName 
     * @param valueColumnNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <K, E> ListMultimap<K, E> toMultimap(final String keyColumnName, final Collection<String> valueColumnNames, final Class<? extends E> rowType) {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowType);
    }

    /**
     * 
     *
     * @param <K> 
     * @param <E> 
     * @param <V> 
     * @param <M> 
     * @param keyColumnName 
     * @param valueColumnNames 
     * @param rowType 
     * @param supplier 
     * @return 
     */
    @Override
    public <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(String keyColumnName, Collection<String> valueColumnNames,
            Class<? extends E> rowType, IntFunction<? extends M> supplier) {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowType, supplier);
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <E> 
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param keyColumnName 
     * @param valueColumnNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <K, E> ListMultimap<K, E> toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final Class<? extends E> rowType) {
        return toMultimap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowType,
                (IntFunction<ListMultimap<K, E>>) len -> N.newLinkedListMultimap());
    }

    /**
     * 
     *
     * @param <K> the key type
     * @param <E> 
     * @param <V> the value type
     * @param <M> 
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param keyColumnName 
     * @param valueColumnNames 
     * @param rowType 
     * @param supplier 
     * @return 
     */
    @Override
    public <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName,
            Collection<String> valueColumnNames, Class<? extends E> rowType, IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnName(valueColumnNames);

        final Type<?> elementType = N.typeOf(rowType);
        final int valueColumnCount = valueColumnIndexes.length;

        final M resultMap = supplier.apply(toRowIndex - fromRowIndex);

        if (elementType.isObjectArray()) {
            Object[] value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = N.newArray(rowType.getComponentType(), valueColumnCount);

                for (int i = 0; i < valueColumnCount; i++) {
                    value[i] = _columnList.get(valueColumnIndexes[i]).get(rowIndex);
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else if (elementType.isList() || elementType.isSet()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowType.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType);
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Collection<Object>) (isAbstractRowClass ? (elementType.isList() ? new ArrayList<>(valueColumnCount) : N.newHashSet(valueColumnCount))
                        : ((intConstructor == null) ? ClassUtil.invokeConstructor(constructor)
                                : ClassUtil.invokeConstructor(intConstructor, valueColumnCount)));

                for (int columIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else if (elementType.isMap()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowType.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType);
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Map<String, Object>) (isAbstractRowClass ? N.newHashMap(valueColumnCount)
                        : (intConstructor == null ? ClassUtil.invokeConstructor(constructor) : ClassUtil.invokeConstructor(intConstructor, valueColumnCount)));

                for (int columIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columIndex), _columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else if (elementType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowType);
            final boolean ignoreUnmatchedProperty = valueColumnNames == _columnNameList;
            Object value = null;
            String propName = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = beanInfo.createBeanResult();

                for (int columIndex : valueColumnIndexes) {
                    propName = _columnNameList.get(columIndex);
                    beanInfo.setPropValue(value, propName, _columnList.get(columIndex).get(rowIndex), ignoreUnmatchedProperty);
                }

                value = beanInfo.finishBeanResult(value);

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowType.getCanonicalName() + ". Only Array, List/Set, Map and bean class are supported");
        }

        return resultMap;
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <E>
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier
     * @return
     */
    @Override
    public <K, E> ListMultimap<K, E> toMultimap(String keyColumnName, Collection<String> valueColumnNames, IntFunction<? extends E> rowSupplier) {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowSupplier);
    }

    /**
     *
     *
     * @param <K>
     * @param <E>
     * @param <V>
     * @param <M>
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier
     * @param supplier
     * @return
     */
    @Override
    public <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends E> rowSupplier, IntFunction<? extends M> supplier) {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowSupplier, supplier);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier
     * @return
     */
    @Override
    public <K, E> ListMultimap<K, E> toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName, Collection<String> valueColumnNames,
            IntFunction<? extends E> rowSupplier) {
        return toMultimap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowSupplier,
                (IntFunction<ListMultimap<K, E>>) len -> N.newLinkedListMultimap());
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param fromRowIndex
     * @param toRowIndex
     * @param keyColumnName
     * @param valueColumnNames
     * @param rowSupplier
     * @param supplier
     * @return
     */
    @Override
    public <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(int fromRowIndex, int toRowIndex, String keyColumnName,
            Collection<String> valueColumnNames, IntFunction<? extends E> rowSupplier, IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnName(valueColumnNames);

        final Class<?> rowClass = rowSupplier.apply(0).getClass();
        final Type<?> elementType = N.typeOf(rowClass);
        final int valueColumnCount = valueColumnIndexes.length;

        final M resultMap = supplier.apply(toRowIndex - fromRowIndex);

        if (elementType.isObjectArray()) {
            Object[] value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Object[]) rowSupplier.apply(valueColumnCount);

                for (int i = 0; i < valueColumnCount; i++) {
                    value[i] = _columnList.get(valueColumnIndexes[i]).get(rowIndex);
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else if (elementType.isList() || elementType.isSet()) {
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Collection<Object>) rowSupplier.apply(valueColumnCount);

                for (int columIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else if (elementType.isMap()) {
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Map<String, Object>) rowSupplier.apply(valueColumnCount);

                for (int columIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columIndex), _columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else if (elementType.isBean()) {
            final boolean ignoreUnmatchedProperty = valueColumnNames == _columnNameList;
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowClass);
            Object value = null;
            String propName = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = rowSupplier.apply(valueColumnCount);

                for (int columIndex : valueColumnIndexes) {
                    propName = _columnNameList.get(columIndex);
                    beanInfo.setPropValue(value, propName, _columnList.get(columIndex).get(rowIndex), ignoreUnmatchedProperty);
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowClass.getCanonicalName() + ". Only Array, List/Set, Map and bean class are supported");
        }

        return resultMap;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toJSON() {
        return toJSON(0, size());
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public String toJSON(final int fromRowIndex, final int toRowIndex) {
        return toJSON(fromRowIndex, toRowIndex, this._columnNameList);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @return
     */
    @Override
    public String toJSON(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        final BufferedJSONWriter writer = Objectory.createBufferedJSONWriter();

        try {
            toJSON(fromRowIndex, toRowIndex, columnNames, writer);

            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     *
     * @param output
     */
    @Override
    public void toJSON(final File output) {
        toJSON(0, size(), output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     */
    @Override
    public void toJSON(final int fromRowIndex, final int toRowIndex, final File output) {
        toJSON(fromRowIndex, toRowIndex, this._columnNameList, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toJSON(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final File output) throws UncheckedIOException {
        OutputStream os = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);

            toJSON(fromRowIndex, toRowIndex, columnNames, os);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param output
     */
    @Override
    public void toJSON(final OutputStream output) {
        toJSON(0, size(), output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     */
    @Override
    public void toJSON(final int fromRowIndex, final int toRowIndex, final OutputStream output) {
        toJSON(fromRowIndex, toRowIndex, this._columnNameList, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toJSON(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final OutputStream output)
            throws UncheckedIOException {
        final BufferedJSONWriter writer = Objectory.createBufferedJSONWriter(output);

        try {
            toJSON(fromRowIndex, toRowIndex, columnNames, writer);

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     *
     * @param output
     */
    @Override
    public void toJSON(final Writer output) {
        toJSON(0, size(), output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     */
    @Override
    public void toJSON(final int fromRowIndex, final int toRowIndex, final Writer output) {
        toJSON(fromRowIndex, toRowIndex, this._columnNameList, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toJSON(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Writer output) throws UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);

        if (N.isEmpty(columnNames)) {
            try {
                IOUtil.write("[]", output);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;

        final char[][] charArrayOfColumnNames = new char[columnCount][];

        for (int i = 0; i < columnCount; i++) {
            charArrayOfColumnNames[i] = ("\"" + _columnNameList.get(columnIndexes[i]) + "\"").toCharArray();
        }

        final boolean isBufferedWriter = output instanceof BufferedJSONWriter;
        final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) output : Objectory.createBufferedJSONWriter(output);

        try {
            bw.write(WD._BRACKET_L);

            Type<Object> type = null;
            Object element = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                if (rowIndex > fromRowIndex) {
                    bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                }

                bw.write(WD._BRACE_L);

                for (int i = 0; i < columnCount; i++) {
                    element = _columnList.get(columnIndexes[i]).get(rowIndex);

                    type = element == null ? null : N.typeOf(element.getClass());

                    if (i > 0) {
                        bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    bw.write(charArrayOfColumnNames[i]);
                    bw.write(WD._COLON);

                    if (type == null) {
                        bw.write(NULL_CHAR_ARRAY);
                    } else if (type.isSerializable()) {
                        type.writeCharacter(bw, element, jsc);
                    } else {
                        // jsonParser.serialize(bw, element, jsc);

                        try { //NOSONAR
                            jsonParser.serialize(element, jsc, bw);
                        } catch (Exception e) {
                            // ignore.

                            strType.writeCharacter(bw, N.toString(element), jsc);
                        }
                    }
                }

                bw.write(WD._BRACE_R);
            }

            bw.write(WD._BRACKET_R);

            bw.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toXML() {
        return toXML(ROW);
    }

    /**
     *
     * @param rowElementName
     * @return
     */
    @Override
    public String toXML(final String rowElementName) {
        return toXML(0, size(), rowElementName);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public String toXML(final int fromRowIndex, final int toRowIndex) {
        return toXML(fromRowIndex, toRowIndex, ROW);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowElementName
     * @return
     */
    @Override
    public String toXML(final int fromRowIndex, final int toRowIndex, final String rowElementName) {
        return toXML(fromRowIndex, toRowIndex, this._columnNameList, rowElementName);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @return
     */
    @Override
    public String toXML(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        return toXML(fromRowIndex, toRowIndex, columnNames, ROW);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowElementName
     * @return
     */
    @Override
    public String toXML(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName) {
        final BufferedXMLWriter writer = Objectory.createBufferedXMLWriter();

        try {
            toXML(fromRowIndex, toRowIndex, columnNames, rowElementName, writer);

            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     *
     * @param output
     */
    @Override
    public void toXML(final File output) {
        toXML(0, size(), output);
    }

    /**
     *
     * @param rowElementName
     * @param output
     */
    @Override
    public void toXML(final String rowElementName, final File output) {
        toXML(0, size(), rowElementName, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final File output) {
        toXML(fromRowIndex, toRowIndex, ROW, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowElementName
     * @param output
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final String rowElementName, final File output) {
        toXML(fromRowIndex, toRowIndex, this._columnNameList, rowElementName, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final File output) {
        toXML(fromRowIndex, toRowIndex, columnNames, ROW, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName, final File output)
            throws UncheckedIOException {
        OutputStream os = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);

            toXML(fromRowIndex, toRowIndex, columnNames, rowElementName, os);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param output
     */
    @Override
    public void toXML(final OutputStream output) {
        toXML(0, size(), output);
    }

    /**
     *
     * @param rowElementName
     * @param output
     */
    @Override
    public void toXML(final String rowElementName, final OutputStream output) {
        toXML(0, size(), rowElementName, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final OutputStream output) {
        toXML(fromRowIndex, toRowIndex, ROW, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowElementName
     * @param output
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final String rowElementName, final OutputStream output) {
        toXML(fromRowIndex, toRowIndex, this._columnNameList, rowElementName, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final OutputStream output) {
        toXML(fromRowIndex, toRowIndex, columnNames, ROW, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName,
            final OutputStream output) throws UncheckedIOException {
        final BufferedXMLWriter writer = Objectory.createBufferedXMLWriter(output);

        try {
            toXML(fromRowIndex, toRowIndex, columnNames, rowElementName, writer);

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     *
     * @param output
     */
    @Override
    public void toXML(final Writer output) {
        toXML(0, size(), output);
    }

    /**
     *
     * @param rowElementName
     * @param output
     */
    @Override
    public void toXML(final String rowElementName, final Writer output) {
        toXML(0, size(), rowElementName, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param output
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final Writer output) {
        toXML(fromRowIndex, toRowIndex, ROW, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowElementName
     * @param output
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final String rowElementName, final Writer output) {
        toXML(fromRowIndex, toRowIndex, this._columnNameList, rowElementName, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Writer output) {
        toXML(fromRowIndex, toRowIndex, columnNames, ROW, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowElementName
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toXML(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName, final Writer output)
            throws UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);

        if (N.isEmpty(columnNames)) {
            try {
                IOUtil.write(XMLConstants.DATA_SET_ELE_START, output);
                IOUtil.write(XMLConstants.DATA_SET_ELE_END, output);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final int[] columnIndexes = checkColumnName(columnNames);

        final int columnCount = columnIndexes.length;

        final char[] rowElementNameHead = ("<" + rowElementName + ">").toCharArray();
        final char[] rowElementNameTail = ("</" + rowElementName + ">").toCharArray();

        final char[][] charArrayOfColumnNames = new char[columnCount][];

        for (int i = 0; i < columnCount; i++) {
            charArrayOfColumnNames[i] = _columnNameList.get(columnIndexes[i]).toCharArray();
        }

        final boolean isBufferedWriter = output instanceof BufferedXMLWriter;
        final BufferedXMLWriter bw = isBufferedWriter ? (BufferedXMLWriter) output : Objectory.createBufferedXMLWriter(output);

        try {
            bw.write(XMLConstants.DATA_SET_ELE_START);

            Type<Object> type = null;
            Object element = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                bw.write(rowElementNameHead);

                for (int i = 0; i < columnCount; i++) {
                    element = _columnList.get(columnIndexes[i]).get(rowIndex);

                    type = element == null ? null : N.typeOf(element.getClass());

                    bw.write(WD._LESS_THAN);
                    bw.write(charArrayOfColumnNames[i]);
                    bw.write(WD._GREATER_THAN);

                    if (type == null) {
                        bw.write(NULL_CHAR_ARRAY);
                    } else if (type.isSerializable()) {
                        type.writeCharacter(bw, element, xsc);
                    } else {
                        // xmlParser.serialize(bw, element, xsc);

                        try { //NOSONAR
                            xmlParser.serialize(element, xsc, bw);
                        } catch (Exception e) {
                            // ignore.

                            strType.writeCharacter(bw, N.toString(element), xsc);
                        }
                    }

                    bw.write(WD._LESS_THAN);
                    bw.write(WD._SLASH);
                    bw.write(charArrayOfColumnNames[i]);
                    bw.write(WD._GREATER_THAN);
                }

                bw.write(rowElementNameTail);
            }

            bw.write(XMLConstants.DATA_SET_ELE_END);

            bw.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toCSV() {
        return toCSV(0, size(), this.columnNameList());
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @return
     */
    @Override
    public String toCSV(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        return toCSV(fromRowIndex, toRowIndex, columnNames, true, true);
    }

    /**
     *
     * @param writeTitle
     * @param quoted
     * @return
     */
    @Override
    public String toCSV(final boolean writeTitle, final boolean quoted) {
        return toCSV(0, size(), columnNameList(), writeTitle, quoted);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param writeTitle
     * @param quoted
     * @return
     */
    @Override
    public String toCSV(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final boolean writeTitle, final boolean quoted) {
        final BufferedWriter bw = Objectory.createBufferedWriter();

        try {
            toCSV(fromRowIndex, toRowIndex, columnNames, writeTitle, quoted, bw);

            return bw.toString();
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     *
     * @param output
     */
    @Override
    public void toCSV(final File output) {
        toCSV(0, size(), _columnNameList, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     */
    @Override
    public void toCSV(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final File output) {
        toCSV(fromRowIndex, toRowIndex, columnNames, true, true, output);
    }

    /**
     * 
     *
     * @param writeTitle 
     * @param quoted 
     * @param output 
     */
    @Override
    public void toCSV(final boolean writeTitle, final boolean quoted, final File output) {
        toCSV(0, size(), _columnNameList, writeTitle, quoted, output);
    }

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param writeTitle 
     * @param quoted 
     * @param output 
     */
    @Override
    public void toCSV(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final boolean writeTitle, final boolean quoted,
            final File output) {
        OutputStream os = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);

            toCSV(fromRowIndex, toRowIndex, columnNames, writeTitle, quoted, os);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param output
     */
    @Override
    public void toCSV(final OutputStream output) {
        toCSV(0, size(), _columnNameList, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     */
    @Override
    public void toCSV(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final OutputStream output) {
        toCSV(fromRowIndex, toRowIndex, columnNames, true, true, output);
    }

    /**
     * 
     *
     * @param writeTitle 
     * @param quoted 
     * @param output 
     */
    @Override
    public void toCSV(final boolean writeTitle, final boolean quoted, final OutputStream output) {
        toCSV(0, size(), _columnNameList, writeTitle, quoted, output);
    }

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param writeTitle 
     * @param quoted 
     * @param output 
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toCSV(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final boolean writeTitle, final boolean quoted,
            final OutputStream output) throws UncheckedIOException {
        Writer writer = null;

        try {
            writer = IOUtil.newOutputStreamWriter(output); // NOSONAR

            toCSV(fromRowIndex, toRowIndex, columnNames, writeTitle, quoted, writer);

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param output
     */
    @Override
    public void toCSV(final Writer output) {
        toCSV(0, size(), _columnNameList, output);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param output
     */
    @Override
    public void toCSV(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Writer output) {
        toCSV(fromRowIndex, toRowIndex, columnNames, true, true, output);
    }

    /**
     * 
     *
     * @param writeTitle 
     * @param quoted 
     * @param output 
     */
    @Override
    public void toCSV(final boolean writeTitle, final boolean quoted, final Writer output) {
        toCSV(0, size(), _columnNameList, writeTitle, quoted, output);
    }

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param writeTitle 
     * @param quoted 
     * @param output 
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("deprecation")
    @Override
    public void toCSV(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final boolean writeTitle, final boolean quoted,
            final Writer output) throws UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);

        if (N.isEmpty(columnNames)) {
            return;
        }

        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;

        final JSONSerializationConfig config = JSC.create();
        config.setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

        if (quoted) {
            config.quoteMapKey(true);
            config.quotePropName(true);
            config.setCharQuotation(WD._QUOTATION_D);
            config.setStringQuotation(WD._QUOTATION_D);
        } else {
            config.quoteMapKey(false);
            config.quotePropName(false);
            config.noCharQuotation();
        }

        final boolean isBufferedWriter = output instanceof BufferedJSONWriter;
        final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) output : Objectory.createBufferedJSONWriter(output);

        try {
            if (writeTitle) {
                for (int i = 0; i < columnCount; i++) {
                    if (i > 0) {
                        bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    // bw.write(getColumnName(columnIndexes[i]));
                    strType.writeCharacter(bw, getColumnName(columnIndexes[i]), config);
                }

                bw.write(IOUtil.LINE_SEPARATOR);
            }

            Type<Object> type = null;
            Object element = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                if (rowIndex > fromRowIndex) {
                    bw.write(IOUtil.LINE_SEPARATOR);
                }

                for (int i = 0; i < columnCount; i++) {
                    if (i > 0) {
                        bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    element = _columnList.get(columnIndexes[i]).get(rowIndex);

                    if (element == null) {
                        bw.write(NULL_CHAR_ARRAY);
                    } else {
                        type = N.typeOf(element.getClass());

                        if (type.isSerializable()) {
                            type.writeCharacter(bw, element, config);
                        } else {
                            strType.writeCharacter(bw, jsonParser.serialize(element, config), config);
                        }
                    }
                }
            }

            bw.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    //    /**
    //     *
    //     * @param columnName
    //     * @return
    //     */
    //    @Override
    //    public DataSet groupBy(final String columnName) {
    //        return groupBy(columnName, NULL_PARAM_INDICATOR_1);
    //    }

    /**
     *
     * @param keyColumnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public DataSet groupBy(final String keyColumnName, String aggregateResultColumnName, String aggregateOnColumnName, final Collector<?, ?, ?> collector) {
        return groupBy(keyColumnName, NULL_PARAM_INDICATOR_1, aggregateResultColumnName, aggregateOnColumnName, collector);
    }

    /**
     *
     *
     * @param keyColumnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowType
     * @return
     */
    @Override
    public DataSet groupBy(String keyColumnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowType) {
        final List<Object> keyColumn = getColumn(keyColumnName);
        final List<Object> valueColumn = toList(aggregateOnColumnNames, rowType);

        final Map<Object, List<Object>> map = N.newLinkedHashMap(N.min(9, size()));
        final List<Object> keyList = new ArrayList<>(N.min(9, size()));
        Object key = null;
        List<Object> val = null;

        for (int i = 0, size = keyColumn.size(); i < size; i++) {
            key = hashKey(keyColumn.get(i));
            val = map.get(key);

            if (val == null) {
                val = new ArrayList<>();
                map.put(key, val);

                keyList.add(keyColumn.get(i));
            }

            val.add(valueColumn.get(i));
        }

        final List<String> newColumnNameList = N.asList(keyColumnName, aggregateResultColumnName);
        final List<List<Object>> newColumnList = N.asList(keyList, new ArrayList<>(map.values()));

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param keyColumnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    @Override
    public DataSet groupBy(final String keyColumnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            final Collector<? super Object[], ?, ?> collector) {
        return groupBy(keyColumnName, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param keyColumnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> DataSet groupBy(final String keyColumnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            final Throwables.Function<? super DisposableObjArray, ? extends T, E> rowMapper, final Collector<? super T, ?, ?> collector) throws E {
        return groupBy(keyColumnName, NULL_PARAM_INDICATOR_1, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector);
    }

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
    //    @Override
    //    public <T, E extends Exception> DataSet groupBy(final String keyColumnName, String aggregateResultColumnName, String aggregateOnColumnName,
    //            final Throwables.Function<Stream<T>, ?, E> func) throws E {
    //        return groupBy(keyColumnName, NULL_PARAM_INDICATOR_1, aggregateResultColumnName, aggregateOnColumnName, func);
    //    }

    /**
     *
     * @param <E>
     * @param keyColumnName
     * @param keyMapper
     * @return
     * @throws E the e
     */
    private <E extends Exception> DataSet groupBy(final String keyColumnName, Throwables.Function<?, ?, E> keyMapper) throws E {
        final int columnIndex = checkColumnName(keyColumnName);

        final int size = size();
        final int newColumnCount = 1;
        final List<String> newColumnNameList = new ArrayList<>(newColumnCount);
        newColumnNameList.add(keyColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final Throwables.Function<Object, ?, E> keyMapper2 = (Throwables.Function<Object, ?, E>) (keyMapper == null ? Fn.identity() : keyMapper);
        final List<Object> keyColumn = newColumnList.get(0);

        final Set<Object> keySet = N.newHashSet();
        final List<Object> groupByColumn = _columnList.get(columnIndex);
        Object value = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            value = groupByColumn.get(rowIndex);

            if (keySet.add(hashKey(keyMapper2.apply(value)))) {
                keyColumn.add(value);
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param <E>
     * @param keyColumnName
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet groupBy(final String keyColumnName, Throwables.Function<?, ?, E> keyMapper, String aggregateResultColumnName,
            String aggregateOnColumnName, final Collector<?, ?, ?> collector) throws E {
        final int columnIndex = checkColumnName(keyColumnName);
        final int aggOnColumnIndex = checkColumnName(aggregateOnColumnName);

        if (N.equals(keyColumnName, aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicated Property name: " + aggregateResultColumnName);
        }

        final int size = size();
        final int newColumnCount = 2;
        final List<String> newColumnNameList = new ArrayList<>(newColumnCount);
        newColumnNameList.add(keyColumnName);
        newColumnNameList.add(aggregateResultColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final Throwables.Function<Object, ?, E> keyMapper2 = (Throwables.Function<Object, ?, E>) (keyMapper == null ? Fn.identity() : keyMapper);
        final List<Object> keyColumn = newColumnList.get(0);
        final List<Object> aggResultColumn = newColumnList.get(1);
        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, Object> accumulator = (BiConsumer<Object, Object>) collector.accumulator();
        final Function<Object, Object> finisher = (Function<Object, Object>) collector.finisher();

        final Map<Object, Integer> keyRowIndexMap = new HashMap<>();
        final List<Object> groupByColumn = _columnList.get(columnIndex);
        final List<Object> aggOnColumn = _columnList.get(aggOnColumnIndex);
        Object key = null;
        Object value = null;
        Integer collectorRowIndex = -1;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            value = groupByColumn.get(rowIndex);
            key = hashKey(keyMapper2.apply(value));

            collectorRowIndex = keyRowIndexMap.get(key);

            if (collectorRowIndex == null) {
                collectorRowIndex = aggResultColumn.size();
                keyRowIndexMap.put(key, collectorRowIndex);
                keyColumn.add(value);
                aggResultColumn.add(supplier.get());
            }

            accumulator.accept(aggResultColumn.get(collectorRowIndex), aggOnColumn.get(rowIndex));
        }

        for (int i = 0, len = aggResultColumn.size(); i < len; i++) {
            aggResultColumn.set(i, finisher.apply(aggResultColumn.get(i)));
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

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
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    @Override
    //    public <T, E extends Exception, E2 extends Exception> DataSet groupBy(final String keyColumnName, Throwables.Function<?, ?, E> keyMapper,
    //            String aggregateResultColumnName, String aggregateOnColumnName, final Throwables.Function<Stream<T>, ?, E2> func) throws E, E2 {
    //        final RowDataSet result = (RowDataSet) groupBy(keyColumnName, keyMapper, aggregateResultColumnName, aggregateOnColumnName, Collectors.toList());
    //        final List<Object> column = result._columnList.get(result.getColumnIndex(aggregateResultColumnName));
    //
    //        for (int i = 0, len = column.size(); i < len; i++) {
    //            column.set(i, func.apply(Stream.of((List<T>) column.get(i))));
    //        }
    //
    //        return result;
    //    }

    /**
     *
     *
     * @param <E>
     * @param keyColumnName
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowType
     * @return
     * @throws E
     */
    @Override
    public <E extends Exception> DataSet groupBy(String keyColumnName, Throwables.Function<?, ?, E> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Class<?> rowType) throws E {
        final Throwables.Function<Object, ?, E> keyMapper2 = (Throwables.Function<Object, ?, E>) (keyMapper == null ? Fn.identity() : keyMapper);

        final List<Object> keyColumn = getColumn(keyColumnName);
        final List<Object> valueColumn = toList(aggregateOnColumnNames, rowType);

        final Map<Object, List<Object>> map = N.newLinkedHashMap(N.min(9, size()));
        final List<Object> keyList = new ArrayList<>(N.min(9, size()));
        Object key = null;
        List<Object> val = null;

        for (int i = 0, size = keyColumn.size(); i < size; i++) {
            key = hashKey(keyMapper2.apply(keyColumn.get(i)));
            val = map.get(key);

            if (val == null) {
                val = new ArrayList<>();
                map.put(key, val);

                keyList.add(keyColumn.get(i));
            }

            val.add(valueColumn.get(i));
        }

        final List<String> newColumnNameList = N.asList(keyColumnName, aggregateResultColumnName);
        final List<List<Object>> newColumnList = N.asList(keyList, new ArrayList<>(map.values()));

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    private static final Throwables.Function<? super DisposableObjArray, Object[], RuntimeException> CLONE = DisposableObjArray::clone;
    private static final Throwables.Function<?, ?, RuntimeException> NULL_PARAM_INDICATOR_1 = null;
    private static final Throwables.Function<? super DisposableObjArray, ?, RuntimeException> NULL_PARAM_INDICATOR_2 = null;

    /**
     *
     * @param <E>
     * @param keyColumnName
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet groupBy(final String keyColumnName, Throwables.Function<?, ?, E> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, final Collector<? super Object[], ?, ?> collector) throws E {
        return groupBy(keyColumnName, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param keyColumnName
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    @Override
    public <T, E extends Exception, E2 extends Exception> DataSet groupBy(final String keyColumnName, Throwables.Function<?, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            final Throwables.Function<? super DisposableObjArray, ? extends T, E2> rowMapper, final Collector<? super T, ?, ?> collector) throws E, E2 {
        final int columnIndex = checkColumnName(keyColumnName);
        final int[] aggOnColumnIndexes = checkColumnName(aggregateOnColumnNames);

        if (N.equals(keyColumnName, aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicated Property name: " + aggregateResultColumnName);
        }

        N.checkArgNotNull(rowMapper, "rowMapper");
        N.checkArgNotNull(collector, "collector");

        final int size = size();
        final int aggOnColumnCount = aggOnColumnIndexes.length;
        final int newColumnCount = 2;
        final List<String> newColumnNameList = new ArrayList<>(newColumnCount);
        newColumnNameList.add(keyColumnName);
        newColumnNameList.add(aggregateResultColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final Throwables.Function<Object, ?, E> keyMapper2 = (Throwables.Function<Object, ?, E>) (keyMapper == null ? Fn.identity() : keyMapper);
        final List<Object> keyColumn = newColumnList.get(0);
        final List<Object> aggResultColumn = newColumnList.get(1);
        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, T> accumulator = (BiConsumer<Object, T>) collector.accumulator();
        final Function<Object, Object> finisher = (Function<Object, Object>) collector.finisher();

        final Map<Object, Integer> keyRowIndexMap = new HashMap<>();
        final List<Object> groupByColumn = _columnList.get(columnIndex);
        final Object[] aggRow = new Object[aggOnColumnCount];
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(aggRow);
        Object key = null;
        Object value = null;
        Integer collectorRowIndex = -1;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            value = groupByColumn.get(rowIndex);
            key = hashKey(keyMapper2.apply(value));

            collectorRowIndex = keyRowIndexMap.get(key);

            if (collectorRowIndex == null) {
                collectorRowIndex = aggResultColumn.size();
                keyRowIndexMap.put(key, collectorRowIndex);
                keyColumn.add(value);
                aggResultColumn.add(supplier.get());
            }

            for (int i = 0; i < aggOnColumnCount; i++) {
                aggRow[i] = _columnList.get(aggOnColumnIndexes[i]).get(rowIndex);
            }

            accumulator.accept(aggResultColumn.get(collectorRowIndex), rowMapper.apply(disposableArray));
        }

        for (int i = 0, len = aggResultColumn.size(); i < len; i++) {
            aggResultColumn.set(i, finisher.apply(aggResultColumn.get(i)));
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param keyColumnNames
     * @return
     */
    @Override
    public DataSet groupBy(final Collection<String> keyColumnNames) {
        return groupBy(keyColumnNames, NULL_PARAM_INDICATOR_2);
    }

    /**
     *
     *
     * @param keyColumnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public DataSet groupBy(Collection<String> keyColumnNames, String aggregateResultColumnName, String aggregateOnColumnName,
            final Collector<?, ?, ?> collector) {
        return groupBy(keyColumnNames, NULL_PARAM_INDICATOR_2, aggregateResultColumnName, aggregateOnColumnName, collector);
    }

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
    //    @Override
    //    public <T, E extends Exception> DataSet groupBy(Collection<String> keyColumnNames, String aggregateResultColumnName, String aggregateOnColumnName,
    //            final Throwables.Function<Stream<T>, ?, E> func) throws E {
    //        return groupBy(keyColumnNames, NULL_PARAM_INDICATOR_2, aggregateResultColumnName, aggregateOnColumnName, func);
    //    }

    /**
     *
     *
     * @param keyColumnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowType
     * @return
     */
    @Override
    public DataSet groupBy(Collection<String> keyColumnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowType) {
        N.checkArgNotEmpty(keyColumnNames, "columnNames");
        N.checkArgNotEmpty(aggregateOnColumnNames, "aggregateOnColumnNames");

        if (keyColumnNames.size() == 1) {
            return groupBy(keyColumnNames.iterator().next(), aggregateResultColumnName, aggregateOnColumnNames, rowType);
        }

        final int size = size();
        final int[] keyColumnIndexes = checkColumnName(keyColumnNames);
        final int keyColumnCount = keyColumnIndexes.length;
        final int newColumnCount = keyColumnIndexes.length + 1;
        final List<String> newColumnNameList = N.newArrayList(newColumnCount);
        newColumnNameList.addAll(keyColumnNames);
        newColumnNameList.add(aggregateResultColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < keyColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            newColumnList.add(new ArrayList<>());

            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final List<Object> valueColumnList = toList(aggregateOnColumnNames, rowType);

        final Map<Wrapper<Object[]>, List<Object>> keyRowMap = N.newLinkedHashMap(N.min(9, size()));

        Object[] keyRow = Objectory.createObjectArray(keyColumnCount);
        Wrapper<Object[]> key = null;
        List<Object> val = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            key = Wrapper.of(keyRow);
            val = keyRowMap.get(key);

            if (val == null) {
                val = new ArrayList<>();
                keyRowMap.put(key, val);

                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                keyRow = Objectory.createObjectArray(keyColumnCount);
            }

            val.add(valueColumnList.get(rowIndex));
        }

        if (keyRow != null) {
            Objectory.recycle(keyRow);
            keyRow = null;
        }

        for (Wrapper<Object[]> e : keyRowMap.keySet()) {
            Objectory.recycle(e.value());
        }

        newColumnList.add(new ArrayList<>(keyRowMap.values()));

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param keyColumnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    @Override
    public DataSet groupBy(Collection<String> keyColumnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            final Collector<? super Object[], ?, ?> collector) {
        return groupBy(keyColumnNames, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param keyColumnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> DataSet groupBy(Collection<String> keyColumnNames, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, final Throwables.Function<? super DisposableObjArray, ? extends T, E> rowMapper,
            final Collector<? super T, ?, ?> collector) throws E {
        return groupBy(keyColumnNames, NULL_PARAM_INDICATOR_2, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector);
    }

    /**
     *
     * @param <E>
     * @param keyColumnNames
     * @param keyMapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet groupBy(final Collection<String> keyColumnNames, final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper)
            throws E {
        N.checkArgNotEmpty(keyColumnNames, "columnNames");

        final boolean isNullOrIdentityKeyMapper = keyMapper == null || keyMapper == Fn.identity();

        if (keyColumnNames.size() == 1 && isNullOrIdentityKeyMapper) {
            return this.groupBy(keyColumnNames.iterator().next(), keyMapper);
        }

        final int size = size();
        final int[] keyColumnIndexes = checkColumnName(keyColumnNames);
        final int keyColumnCount = keyColumnIndexes.length;
        final int newColumnCount = keyColumnIndexes.length;
        final List<String> newColumnNameList = N.newArrayList(keyColumnNames);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final Set<Object> keyRowSet = N.newHashSet();
        Object[] keyRow = Objectory.createObjectArray(keyColumnCount);
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(keyRow);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            if (isNullOrIdentityKeyMapper) {
                if (keyRowSet.add(Wrapper.of(keyRow))) {
                    for (int i = 0; i < keyColumnCount; i++) {
                        newColumnList.get(i).add(keyRow[i]);
                    }

                    keyRow = Objectory.createObjectArray(keyColumnCount);
                }
            } else if (keyRowSet.add(hashKey(keyMapper.apply(disposableArray)))) {
                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }
            }
        }

        if (keyRow != null) {
            Objectory.recycle(keyRow);
            keyRow = null;
        }

        if (isNullOrIdentityKeyMapper) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) keyRowSet;

            for (Wrapper<Object[]> e : tmp) {
                Objectory.recycle(e.value());
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param <E>
     * @param keyColumnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet groupBy(Collection<String> keyColumnNames, final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, String aggregateOnColumnName, final Collector<?, ?, ?> collector) throws E {
        N.checkArgNotEmpty(keyColumnNames, "columnNames");

        if (N.notEmpty(keyColumnNames) && keyColumnNames.contains(aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicated Property name: " + aggregateResultColumnName);
        }

        final boolean isNullOrIdentityKeyMapper = keyMapper == null || keyMapper == Fn.identity();

        if (keyColumnNames.size() == 1 && isNullOrIdentityKeyMapper) {
            return groupBy(keyColumnNames.iterator().next(), keyMapper, aggregateResultColumnName, aggregateOnColumnName, collector);
        }

        final int size = size();
        final int[] keyColumnIndexes = checkColumnName(keyColumnNames);
        final int aggOnColumnIndex = checkColumnName(aggregateOnColumnName);
        final int keyColumnCount = keyColumnIndexes.length;
        final int newColumnCount = keyColumnIndexes.length + 1;
        final List<String> newColumnNameList = new ArrayList<>(keyColumnNames);
        newColumnNameList.add(aggregateResultColumnName);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, Object> accumulator = (BiConsumer<Object, Object>) collector.accumulator();
        final Function<Object, Object> finisher = (Function<Object, Object>) collector.finisher();

        final List<Object> aggResultColumn = newColumnList.get(newColumnList.size() - 1);
        final List<Object> aggOnColumn = _columnList.get(aggOnColumnIndex);
        final Map<Object, Integer> keyRowIndexMap = new HashMap<>();
        Object[] keyRow = Objectory.createObjectArray(keyColumnCount);
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(keyRow);
        Object key = null;
        Integer collectorRowIndex = -1;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            key = isNullOrIdentityKeyMapper ? Wrapper.of(keyRow) : hashKey(keyMapper.apply(disposableArray));
            collectorRowIndex = keyRowIndexMap.get(key);

            if (collectorRowIndex == null) {
                collectorRowIndex = aggResultColumn.size();
                keyRowIndexMap.put(key, collectorRowIndex);
                aggResultColumn.add(supplier.get());

                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                if (isNullOrIdentityKeyMapper) {
                    keyRow = Objectory.createObjectArray(keyColumnCount);
                }
            }

            accumulator.accept(aggResultColumn.get(collectorRowIndex), aggOnColumn.get(rowIndex));
        }

        for (int i = 0, len = aggResultColumn.size(); i < len; i++) {
            aggResultColumn.set(i, finisher.apply(aggResultColumn.get(i)));
        }

        if (keyRow != null) {
            Objectory.recycle(keyRow);
            keyRow = null;
        }

        if (isNullOrIdentityKeyMapper) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) keyRowIndexMap.keySet();

            for (Wrapper<Object[]> e : tmp) {
                Objectory.recycle(e.value());
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param <E>
    //     * @param <E2>
    //     * @param keyColumnNames
    //     * @param keyMapper
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    @Override
    //    public <T, E extends Exception, E2 extends Exception> DataSet groupBy(Collection<String> keyColumnNames,
    //            Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, String aggregateOnColumnName,
    //            final Throwables.Function<Stream<T>, ?, E2> func) throws E, E2 {
    //        final RowDataSet result = (RowDataSet) groupBy(keyColumnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnName, Collectors.toList());
    //        final List<Object> column = result._columnList.get(result.getColumnIndex(aggregateResultColumnName));
    //
    //        for (int i = 0, len = column.size(); i < len; i++) {
    //            column.set(i, func.apply(Stream.of((List<T>) column.get(i))));
    //        }
    //
    //        return result;
    //    }

    /**
     *
     *
     * @param <E>
     * @param keyColumnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowType
     * @return
     * @throws E
     */
    @Override
    public <E extends Exception> DataSet groupBy(Collection<String> keyColumnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowType) throws E {
        N.checkArgNotEmpty(keyColumnNames, "columnNames");
        N.checkArgNotEmpty(aggregateOnColumnNames, "aggregateOnColumnNames");

        final boolean isNullOrIdentityKeyMapper = keyMapper == null || keyMapper == Fn.identity();

        if (isNullOrIdentityKeyMapper) {
            if (keyColumnNames.size() == 1) {
                return groupBy(keyColumnNames.iterator().next(), aggregateResultColumnName, aggregateOnColumnNames, rowType);
            }

            return groupBy(keyColumnNames, aggregateResultColumnName, aggregateOnColumnNames, rowType);
        }

        final int size = size();
        final int[] keyColumnIndexes = checkColumnName(keyColumnNames);
        final int keyColumnCount = keyColumnIndexes.length;
        final int newColumnCount = keyColumnIndexes.length + 1;
        final List<String> newColumnNameList = N.newArrayList(newColumnCount);
        newColumnNameList.addAll(keyColumnNames);
        newColumnNameList.add(aggregateResultColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < keyColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            newColumnList.add(new ArrayList<>());

            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final List<Object> valueColumnList = toList(aggregateOnColumnNames, rowType);

        final Map<Object, List<Object>> keyRowMap = N.newLinkedHashMap();
        final Object[] keyRow = Objectory.createObjectArray(keyColumnCount);
        final DisposableObjArray keyDisposableArray = DisposableObjArray.wrap(keyRow);

        Object key = null;
        List<Object> val = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            key = hashKey(keyMapper.apply(keyDisposableArray));
            val = keyRowMap.get(key);

            if (val == null) {
                val = new ArrayList<>();
                keyRowMap.put(key, val);

                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }
            }

            val.add(valueColumnList.get(rowIndex));
        }

        if (keyRow != null) {
            Objectory.recycle(keyRow);
        }

        newColumnList.add(new ArrayList<>(keyRowMap.values()));

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param <E>
     * @param keyColumnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet groupBy(Collection<String> keyColumnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, final Collector<? super Object[], ?, ?> collector) throws E {
        return groupBy(aggregateOnColumnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param keyColumnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    @Override
    public <T, E extends Exception, E2 extends Exception> DataSet groupBy(Collection<String> keyColumnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            final Throwables.Function<? super DisposableObjArray, ? extends T, E2> rowMapper, final Collector<? super T, ?, ?> collector) throws E, E2 {
        N.checkArgNotEmpty(keyColumnNames, "columnNames");

        if (N.notEmpty(keyColumnNames) && keyColumnNames.contains(aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicated Property name: " + aggregateResultColumnName);
        }

        N.checkArgNotNull(rowMapper, "rowMapper");
        N.checkArgNotNull(collector, "collector");

        final boolean isNullOrIdentityKeyMapper = keyMapper == null || keyMapper == Fn.identity();

        if (keyColumnNames.size() == 1 && isNullOrIdentityKeyMapper) {
            return groupBy(keyColumnNames.iterator().next(), keyMapper, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector);
        }

        final int size = size();
        final int[] keyColumnIndexes = checkColumnName(keyColumnNames);
        final int[] aggOnColumnIndexes = checkColumnName(aggregateOnColumnNames);
        final int keyColumnCount = keyColumnIndexes.length;
        final int newColumnCount = keyColumnIndexes.length + 1;
        final List<String> newColumnNameList = new ArrayList<>(keyColumnNames);
        newColumnNameList.add(aggregateResultColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, T> accumulator = (BiConsumer<Object, T>) collector.accumulator();
        final Function<Object, Object> finisher = (Function<Object, Object>) collector.finisher();

        final int aggOnColumnCount = aggOnColumnIndexes.length;
        final List<Object> aggResultColumn = newColumnList.get(newColumnList.size() - 1);
        final Map<Object, Integer> keyRowIndexMap = new HashMap<>();
        Object[] keyRow = Objectory.createObjectArray(keyColumnCount);
        final DisposableObjArray keyDisposableArray = DisposableObjArray.wrap(keyRow);
        final Object[] aggOnRow = new Object[aggOnColumnCount];
        final DisposableObjArray aggOnRowDisposableArray = DisposableObjArray.wrap(aggOnRow);
        Object key = null;
        Integer collectorRowIndex = -1;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            key = isNullOrIdentityKeyMapper ? Wrapper.of(keyRow) : hashKey(keyMapper.apply(keyDisposableArray));
            collectorRowIndex = keyRowIndexMap.get(key);

            if (collectorRowIndex == null) {
                collectorRowIndex = aggResultColumn.size();
                keyRowIndexMap.put(key, collectorRowIndex);
                aggResultColumn.add(supplier.get());

                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                if (isNullOrIdentityKeyMapper) {
                    keyRow = Objectory.createObjectArray(keyColumnCount);
                }
            }

            for (int i = 0; i < aggOnColumnCount; i++) {
                aggOnRow[i] = _columnList.get(aggOnColumnIndexes[i]).get(rowIndex);
            }

            accumulator.accept(aggResultColumn.get(collectorRowIndex), rowMapper.apply(aggOnRowDisposableArray));
        }

        for (int i = 0, len = aggResultColumn.size(); i < len; i++) {
            aggResultColumn.set(i, finisher.apply(aggResultColumn.get(i)));
        }

        if (keyRow != null) {
            Objectory.recycle(keyRow);
            keyRow = null;
        }

        if (isNullOrIdentityKeyMapper) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) keyRowIndexMap.keySet();

            for (Wrapper<Object[]> e : tmp) {
                Objectory.recycle(e.value());
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

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
     * @throws E
     */
    @Override
    public <R, C, T, E extends Exception> Sheet<R, C, T> pivot(String groupByColumnName, String pivotColumnName, String aggColumnName,
            Collector<?, ?, ? extends T> collector) throws E {
        final DataSet groupedDataSet = groupBy(N.asList(groupByColumnName, pivotColumnName), aggColumnName, aggColumnName, collector);

        return pivot(groupedDataSet);
    }

    //    @Override
    //    public <R, C, U, T, E extends Exception> Sheet<R, C, T> pivot(String groupByColumnName, String pivotColumnName, String aggColumnName,
    //            Throwables.Function<Stream<U>, ? extends T, E> aggFunc) throws E {
    //        final DataSet groupedDataSet = groupBy(N.asList(groupByColumnName, pivotColumnName), aggColumnName, aggColumnName, aggFunc);
    //
    //        return pivot(groupedDataSet);
    //    }

    /**
     *
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
    @Override
    public <R, C, T> Sheet<R, C, T> pivot(String groupByColumnName, String pivotColumnName, Collection<String> aggColumnNames,
            Collector<? super Object[], ?, ? extends T> collector) {
        final String aggregateResultColumnName = Strings.join(aggColumnNames, "_");

        final DataSet groupedDataSet = groupBy(N.asList(groupByColumnName, pivotColumnName), aggregateResultColumnName, aggColumnNames, collector);

        return pivot(groupedDataSet);
    }

    /**
     *
     *
     * @param <R>
     * @param <C>
     * @param <U>
     * @param <T>
     * @param <E>
     * @param groupByColumnName
     * @param pivotColumnName
     * @param aggColumnNames
     * @param rowMapper
     * @param collector
     * @return
     * @throws E
     */
    @Override
    public <R, C, U, T, E extends Exception> Sheet<R, C, T> pivot(String groupByColumnName, String pivotColumnName, Collection<String> aggColumnNames,
            Throwables.Function<? super DisposableObjArray, ? extends U, E> rowMapper, Collector<? super U, ?, ? extends T> collector) throws E {
        final String aggregateResultColumnName = Strings.join(aggColumnNames, "_");

        final DataSet groupedDataSet = groupBy(N.asList(groupByColumnName, pivotColumnName), aggregateResultColumnName, aggColumnNames, rowMapper, collector);

        return pivot(groupedDataSet);
    }

    private <R, C, T> Sheet<R, C, T> pivot(final DataSet groupedDataSet) {
        final ImmutableList<R> rowkeyList = groupedDataSet.getColumn(0);
        final ImmutableList<C> colKeyList = groupedDataSet.getColumn(1);

        final Set<R> rowKeySet = new LinkedHashSet<>(rowkeyList);
        final Set<C> colKeySet = new LinkedHashSet<>(colKeyList);
        final Object[][] rows = new Object[rowKeySet.size()][colKeySet.size()];

        final Map<R, Integer> rowIndexMap = new HashMap<>(rowKeySet.size());
        final Iterator<R> rowKeyIter = rowKeySet.iterator();
        for (int i = 0, size = rowKeySet.size(); i < size; i++) {
            rowIndexMap.put(rowKeyIter.next(), i);
        }

        final Map<C, Integer> colIndexMap = new HashMap<>(colKeySet.size());
        final Iterator<C> colKeyIter = colKeySet.iterator();
        for (int i = 0, size = colKeySet.size(); i < size; i++) {
            colIndexMap.put(colKeyIter.next(), i);
        }

        final ImmutableList<R> aggColumn = groupedDataSet.getColumn(2);

        for (int i = 0, size = groupedDataSet.size(); i < size; i++) {
            rows[rowIndexMap.get(rowkeyList.get(i))][colIndexMap.get(colKeyList.get(i))] = aggColumn.get(i);
        }

        return Sheet.rows(rowKeySet, colKeySet, rows);
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @Override
    public Stream<DataSet> rollup(final Collection<String> columnNames) {
        return Stream.of(Iterables.rollup(columnNames)).reversed().filter(Fn.notEmptyC()).map(this::groupBy);
    }

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public Stream<DataSet> rollup(final Collection<String> columnNames, final String aggregateResultColumnName, final String aggregateOnColumnName,
            final Collector<?, ?, ?> collector) {
        return Stream.of(Iterables.rollup(columnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> groupBy(columnNames1, aggregateResultColumnName, aggregateOnColumnName, collector));
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     */
    //    @Override
    //    public <T> Stream<DataSet> rollup(final Collection<String> columnNames, final String aggregateResultColumnName, final String aggregateOnColumnName,
    //            final Throwables.Function<Stream<T>, ?, ? extends Exception> func) {
    //        return Stream.of(Iterables.rollup(columnNames))
    //                .reversed()
    //                .filter(Fn.notEmptyC())
    //                .map(columnNames1 -> Try.call((Callable<DataSet>) () -> groupBy(columnNames1, aggregateResultColumnName, aggregateOnColumnName, func)));
    //    }

    /**
     *
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowType
     * @return
     */
    @Override
    public Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Class<?> rowType) {
        return Stream.of(Iterables.rollup(columnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call((Callable<DataSet>) () -> groupBy(columnNames1, aggregateResultColumnName, aggregateOnColumnNames, rowType)));
    }

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    @Override
    public Stream<DataSet> rollup(final Collection<String> columnNames, final String aggregateResultColumnName, final Collection<String> aggregateOnColumnNames,
            final Collector<? super Object[], ?, ?> collector) {
        return rollup(columnNames, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <T>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     */
    @Override
    public <T> Stream<DataSet> rollup(final Collection<String> columnNames, final String aggregateResultColumnName,
            final Collection<String> aggregateOnColumnNames, final Throwables.Function<? super DisposableObjArray, ? extends T, ? extends Exception> rowMapper,
            final Collector<? super T, ?, ?> collector) {
        return Stream.of(Iterables.rollup(columnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try
                        .call((Callable<DataSet>) () -> groupBy(columnNames1, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector)));
    }

    /**
     *
     * @param columnNames
     * @param keyMapper
     * @return
     */
    @Override
    public Stream<DataSet> rollup(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper) {
        return Stream.of(Iterables.rollup(columnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call((Callable<DataSet>) () -> groupBy(columnNames1, keyMapper)));
    }

    /**
     *
     *
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public Stream<DataSet> rollup(final Collection<String> columnNames, final Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper,
            final String aggregateResultColumnName, final String aggregateOnColumnName, final Collector<?, ?, ?> collector) {
        return Stream.of(Iterables.rollup(columnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try
                        .call((Callable<DataSet>) () -> groupBy(columnNames1, keyMapper, aggregateResultColumnName, aggregateOnColumnName, collector)));
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param keyMapper
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     */
    //    @Override
    //    public <T> Stream<DataSet> rollup(final Collection<String> columnNames,
    //            final Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper, final String aggregateResultColumnName,
    //            final String aggregateOnColumnName, final Throwables.Function<Stream<T>, ?, ? extends Exception> func) {
    //        return Stream.of(Iterables.rollup(columnNames))
    //                .reversed()
    //                .filter(Fn.notEmptyC())
    //                .map(columnNames1 -> Try
    //                        .call((Callable<DataSet>) () -> groupBy(columnNames1, keyMapper, aggregateResultColumnName, aggregateOnColumnName, func)));
    //    }

    /**
     *
     *
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowType
     * @return
     */
    @Override
    public Stream<DataSet> rollup(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowType) {
        return Stream.of(Iterables.rollup(columnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try
                        .call((Callable<DataSet>) () -> groupBy(columnNames1, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, rowType)));
    }

    /**
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    @Override
    public Stream<DataSet> rollup(final Collection<String> columnNames, final Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper,
            final String aggregateResultColumnName, final Collection<String> aggregateOnColumnNames, final Collector<? super Object[], ?, ?> collector) {
        return rollup(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <T>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     */
    @Override
    public <T> Stream<DataSet> rollup(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper, final String aggregateResultColumnName,
            final Collection<String> aggregateOnColumnNames, final Throwables.Function<? super DisposableObjArray, ? extends T, ? extends Exception> rowMapper,
            final Collector<? super T, ?, ?> collector) {
        return Stream.of(Iterables.rollup(columnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call(
                        (Callable<DataSet>) () -> groupBy(columnNames1, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector)));
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @Override
    public Stream<DataSet> cube(final Collection<String> columnNames) {
        return cubeSet(columnNames).filter(Fn.notEmptyC()).map(this::groupBy);
    }

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public Stream<DataSet> cube(final Collection<String> columnNames, final String aggregateResultColumnName, final String aggregateOnColumnName,
            final Collector<?, ?, ?> collector) {
        return cubeSet(columnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> groupBy(columnNames1, aggregateResultColumnName, aggregateOnColumnName, collector));
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     */
    //    @Override
    //    public <T> Stream<DataSet> cube(final Collection<String> columnNames, final String aggregateResultColumnName, final String aggregateOnColumnName,
    //            final Throwables.Function<Stream<T>, ?, ? extends Exception> func) {
    //        return cubeSet(columnNames).filter(Fn.notEmptyC())
    //                .map(columnNames1 -> Try.call((Callable<DataSet>) () -> groupBy(columnNames1, aggregateResultColumnName, aggregateOnColumnName, func)));
    //    }

    /**
     *
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowType
     * @return
     */
    @Override
    public Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowType) {
        return cubeSet(columnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call((Callable<DataSet>) () -> groupBy(columnNames1, aggregateResultColumnName, aggregateOnColumnNames, rowType)));
    }

    /**
     *
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    @Override
    public Stream<DataSet> cube(final Collection<String> columnNames, final String aggregateResultColumnName, final Collection<String> aggregateOnColumnNames,
            final Collector<? super Object[], ?, ?> collector) {
        return cube(columnNames, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <T>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     */
    @Override
    public <T> Stream<DataSet> cube(final Collection<String> columnNames, final String aggregateResultColumnName,
            final Collection<String> aggregateOnColumnNames, final Throwables.Function<? super DisposableObjArray, ? extends T, ? extends Exception> rowMapper,
            final Collector<? super T, ?, ?> collector) {
        return cubeSet(columnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> Try
                        .call((Callable<DataSet>) () -> groupBy(columnNames1, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector)));
    }

    /**
     *
     * @param columnNames
     * @param keyMapper
     * @return
     */
    @Override
    public Stream<DataSet> cube(final Collection<String> columnNames, final Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper) {
        return cubeSet(columnNames).filter(Fn.notEmptyC()).map(columnNames1 -> Try.call((Callable<DataSet>) () -> groupBy(columnNames1, keyMapper)));
    }

    /**
     *
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public Stream<DataSet> cube(final Collection<String> columnNames, final Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper,
            final String aggregateResultColumnName, final String aggregateOnColumnName, final Collector<?, ?, ?> collector) {
        return cubeSet(columnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> Try
                        .call((Callable<DataSet>) () -> groupBy(columnNames1, keyMapper, aggregateResultColumnName, aggregateOnColumnName, collector)));
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param keyMapper
    //     * @param aggregateResultColumnName
    //     * @param aggregateOnColumnName
    //     * @param func
    //     * @return
    //     */
    //    @Override
    //    public <T> Stream<DataSet> cube(final Collection<String> columnNames,
    //            final Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper, final String aggregateResultColumnName,
    //            final String aggregateOnColumnName, final Throwables.Function<Stream<T>, ?, ? extends Exception> func) {
    //        return cubeSet(columnNames).filter(Fn.notEmptyC())
    //                .map(columnNames1 -> Try
    //                        .call((Callable<DataSet>) () -> groupBy(columnNames1, keyMapper, aggregateResultColumnName, aggregateOnColumnName, func)));
    //    }

    /**
     *
     *
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowType
     * @return
     */
    @Override
    public Stream<DataSet> cube(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowType) {
        return cubeSet(columnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> Try
                        .call((Callable<DataSet>) () -> groupBy(columnNames1, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, rowType)));
    }

    /**
     *
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    @Override
    public Stream<DataSet> cube(final Collection<String> columnNames, final Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper,
            final String aggregateResultColumnName, final Collection<String> aggregateOnColumnNames, final Collector<? super Object[], ?, ?> collector) {
        return cube(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <T>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     */
    @Override
    public <T> Stream<DataSet> cube(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, ? extends Exception> keyMapper, final String aggregateResultColumnName,
            final Collection<String> aggregateOnColumnNames, final Throwables.Function<? super DisposableObjArray, ? extends T, ? extends Exception> rowMapper,
            final Collector<? super T, ?, ?> collector) {
        return cubeSet(columnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call(
                        (Callable<DataSet>) () -> groupBy(columnNames1, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector)));
    }

    private static final com.landawn.abacus.util.function.Consumer<List<Set<String>>> REVERSE_ACTION = N::reverse;

    /**
     *
     * @param columnNames
     * @return
     */
    private Stream<Set<String>> cubeSet(final Collection<String> columnNames) {
        return Stream.of(Iterables.powerSet(N.newLinkedHashSet(columnNames)))
                .groupByToEntry(Fn.size())
                .values()
                .onEach(REVERSE_ACTION)
                .flatmap(Fn.<List<Set<String>>> identity())
                .reversed();
    }

    //    @Override
    //    public <T, R, E extends Exception> Sheet<String, String, R> pivot(String groupByColumn, String pivotColumn,
    //            Throwables.Function<List<T>, R, Throwable> aggFunc) throws E {
    //    }
    //
    //    @Override
    //    public <R, E extends Exception> Sheet<String, String, R> pivot(Collection<String> groupByColumns, Collection<String> pivotColumns,
    //            com.landawn.abacus.util.Throwables.Function<DataSet, R, Throwable> aggFunc) throws E {
    //        // TODO Auto-generated method stub
    //        return null;
    //    }

    /**
     *
     * @param columnName
     */
    @Override
    public void sortBy(final String columnName) {
        sortBy(columnName, Comparators.naturalOrder());
    }

    /**
     *
     * @param <T>
     * @param columnName
     * @param cmp
     */
    @Override
    public <T> void sortBy(final String columnName, final Comparator<T> cmp) {
        sort(columnName, cmp, false);
    }

    /**
     *
     * @param columnNames
     */
    @Override
    public void sortBy(final Collection<String> columnNames) {
        sortBy(columnNames, Comparators.OBJECT_ARRAY_COMPARATOR);
    }

    /**
     *
     * @param columnNames
     * @param cmp
     */
    @Override
    public void sortBy(final Collection<String> columnNames, final Comparator<? super Object[]> cmp) {
        sort(columnNames, cmp, false);
    }

    /**
     *
     * @param columnNames
     * @param keyMapper
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void sortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyMapper) {
        sort(columnNames, keyMapper, false);
    }

    /**
     * Parallel sort by.
     *
     * @param columnName
     */
    @Override
    public void parallelSortBy(final String columnName) {
        parallelSortBy(columnName, Comparators.naturalOrder());
    }

    /**
     * Parallel sort by.
     *
     * @param <T>
     * @param columnName
     * @param cmp
     */
    @Override
    public <T> void parallelSortBy(final String columnName, final Comparator<T> cmp) {
        sort(columnName, cmp, true);
    }

    /**
     * Parallel sort by.
     *
     * @param columnNames
     */
    @Override
    public void parallelSortBy(final Collection<String> columnNames) {
        parallelSortBy(columnNames, Comparators.OBJECT_ARRAY_COMPARATOR);
    }

    /**
     * Parallel sort by.
     *
     * @param columnNames
     * @param cmp
     */
    @Override
    public void parallelSortBy(final Collection<String> columnNames, final Comparator<? super Object[]> cmp) {
        sort(columnNames, cmp, true);
    }

    /**
     * Parallel sort by.
     *
     * @param columnNames
     * @param keyMapper
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void parallelSortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyMapper) {
        sort(columnNames, keyMapper, true);
    }

    /**
     *
     * @param <T>
     * @param columnName
     * @param cmp
     * @param isParallelSort
     */
    private <T> void sort(final String columnName, final Comparator<T> cmp, final boolean isParallelSort) {
        checkFrozen();

        final int columnIndex = checkColumnName(columnName);
        final int size = size();

        if (size == 0) {
            return;
        }

        // TODO too many array objects are created.
        final Indexed<Object>[] arrayOfPair = new Indexed[size];
        final List<Object> orderByColumn = _columnList.get(columnIndex);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            arrayOfPair[rowIndex] = Indexed.of(orderByColumn.get(rowIndex), rowIndex);
        }

        final Comparator<Indexed<Object>> pairCmp = createComparatorForIndexedObject(cmp);

        sort(arrayOfPair, pairCmp, isParallelSort);
    }

    @SuppressWarnings("rawtypes")
    private static Comparator<Indexed<Object>> createComparatorForIndexedObject(final Comparator<?> cmp) {
        Comparator<Indexed<Object>> pairCmp = null;

        if (cmp != null) {
            final Comparator<Object> cmp2 = (Comparator<Object>) cmp;
            pairCmp = (a, b) -> cmp2.compare(a.value(), b.value());
        } else {
            final Comparator<Indexed<Comparable>> tmp = (a, b) -> N.compare(a.value(), b.value());
            pairCmp = (Comparator) tmp;
        }

        return pairCmp;
    }

    private static Comparator<Indexed<Object[]>> createComparatorForIndexedObjectArray(final Comparator<? super Object[]> cmp) {
        Comparator<Indexed<Object[]>> pairCmp = null;

        if (cmp != null) {
            pairCmp = (a, b) -> cmp.compare(a.value(), b.value());
        } else {
            pairCmp = (a, b) -> Comparators.OBJECT_ARRAY_COMPARATOR.compare(a.value(), b.value());
        }

        return pairCmp;
    }

    /**
     *
     * @param columnNames
     * @param cmp
     * @param isParallelSort
     */
    private void sort(final Collection<String> columnNames, final Comparator<? super Object[]> cmp, final boolean isParallelSort) {
        checkFrozen();

        final int[] columnIndexes = checkColumnName(columnNames);
        final int size = size();

        if (size == 0) {
            return;
        }

        final int sortByColumnCount = columnIndexes.length;
        // TODO too many array objects are created.
        final Indexed<Object[]>[] arrayOfPair = new Indexed[size];

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            arrayOfPair[rowIndex] = Indexed.of(Objectory.createObjectArray(sortByColumnCount), rowIndex);
        }

        for (int i = 0; i < sortByColumnCount; i++) {
            final List<Object> orderByColumn = _columnList.get(columnIndexes[i]);

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                arrayOfPair[rowIndex].value()[i] = orderByColumn.get(rowIndex);
            }
        }

        final Comparator<Indexed<Object[]>> pairCmp = createComparatorForIndexedObjectArray(cmp);

        sort(arrayOfPair, pairCmp, isParallelSort);

        for (Indexed<Object[]> p : arrayOfPair) {
            Objectory.recycle(p.value());
        }
    }

    /**
     *
     * @param columnNames
     * @param keyMapper
     * @param isParallelSort
     */
    @SuppressWarnings("rawtypes")
    private void sort(final Collection<String> columnNames, final Function<? super DisposableObjArray, ? extends Comparable> keyMapper,
            final boolean isParallelSort) {
        checkFrozen();

        final int[] columnIndexes = checkColumnName(columnNames);
        final int size = size();

        if (size == 0) {
            return;
        }

        final int sortByColumnCount = columnIndexes.length;
        final Indexed<Comparable>[] arrayOfPair = new Indexed[size];

        final Object[] sortByRow = new Object[sortByColumnCount];
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(sortByRow);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < sortByColumnCount; i++) {
                sortByRow[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            arrayOfPair[rowIndex] = Indexed.of((Comparable) keyMapper.apply(disposableArray), rowIndex);
        }

        final Comparator<Indexed<Comparable>> pairCmp = Comparators.comparingBy(Indexed::value);

        sort(arrayOfPair, pairCmp, isParallelSort);
    }

    /**
     *
     * @param <T>
     * @param arrayOfPair
     * @param pairCmp
     * @param isParallelSort
     */
    private <T> void sort(final Indexed<T>[] arrayOfPair, final Comparator<Indexed<T>> pairCmp, final boolean isParallelSort) {
        if (isParallelSort) {
            N.parallelSort(arrayOfPair, pairCmp);
        } else {
            N.sort(arrayOfPair, pairCmp);
        }

        final int size = size();
        final int columnCount = columnCount();
        final Set<Integer> ordered = N.newHashSet(size);
        final Object[] tempRow = new Object[columnCount];

        for (int i = 0, index = 0; i < size; i++) {
            index = arrayOfPair[i].index();

            if ((index != i) && !ordered.contains(i)) {
                for (int j = 0; j < columnCount; j++) {
                    tempRow[j] = _columnList.get(j).get(i);
                }

                int previous = i;
                int next = index;

                do {
                    for (int j = 0; j < columnCount; j++) {
                        _columnList.get(j).set(previous, _columnList.get(j).get(next));
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

        modCount++;
    }

    /**
     *
     * @param columnName
     * @param n
     * @return
     */
    @Override
    public DataSet topBy(final String columnName, final int n) {
        return topBy(columnName, n, Comparators.nullsFirst());
    }

    /**
     *
     * @param <T>
     * @param columnName
     * @param n
     * @param cmp
     * @return
     */
    @Override
    public <T> DataSet topBy(final String columnName, final int n, final Comparator<T> cmp) {
        if (n < 1) {
            throw new IllegalArgumentException("'n' can not be less than 1");
        }

        final int columnIndex = checkColumnName(columnName);
        final int size = size();

        if (n >= size) {
            return this.copy();
        }

        final Comparator<Indexed<Object>> pairCmp = createComparatorForIndexedObject(cmp);

        final List<Object> orderByColumn = _columnList.get(columnIndex);

        return top(n, pairCmp, orderByColumn::get);
    }

    /**
     *
     * @param columnNames
     * @param n
     * @return
     */
    @Override
    public DataSet topBy(final Collection<String> columnNames, final int n) {
        return topBy(columnNames, n, Comparators.OBJECT_ARRAY_COMPARATOR);
    }

    /**
     *
     * @param columnNames
     * @param n
     * @param cmp
     * @return
     */
    @Override
    public DataSet topBy(final Collection<String> columnNames, final int n, final Comparator<? super Object[]> cmp) {
        if (n < 1) {
            throw new IllegalArgumentException("'n' can not be less than 1");
        }

        final int[] sortByColumnIndexes = checkColumnName(columnNames);
        final int size = size();

        if (n >= size) {
            return this.copy();
        }

        final Comparator<Indexed<Object[]>> pairCmp = createComparatorForIndexedObjectArray(cmp);

        final List<Object[]> keyRowList = new ArrayList<>(n);
        final int sortByColumnCount = sortByColumnIndexes.length;

        final DataSet result = top(n, pairCmp, rowIndex -> {
            final Object[] keyRow = Objectory.createObjectArray(sortByColumnCount);
            keyRowList.add(keyRow);

            for (int i = 0; i < sortByColumnCount; i++) {
                keyRow[i] = _columnList.get(sortByColumnIndexes[i]).get(rowIndex);
            }

            return keyRow;
        });

        for (Object[] a : keyRowList) {
            Objectory.recycle(a);
        }

        return result;
    }

    /**
     *
     * @param columnNames
     * @param n
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public DataSet topBy(final Collection<String> columnNames, final int n, final Function<? super DisposableObjArray, ? extends Comparable> keyMapper) {
        if (n < 1) {
            throw new IllegalArgumentException("'n' can not be less than 1");
        }

        final int[] columnIndexes = checkColumnName(columnNames);
        final int size = size();

        if (n >= size) {
            return this.copy();
        }

        final Comparator<Indexed<Comparable>> pairCmp = Comparators.comparingBy(Indexed::value);

        final int sortByColumnCount = columnIndexes.length;
        final Object[] keyRow = new Object[sortByColumnCount];
        final DisposableObjArray disposableObjArray = DisposableObjArray.wrap(keyRow);

        return top(n, pairCmp, rowIndex -> {

            for (int i = 0; i < sortByColumnCount; i++) {
                keyRow[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            return keyMapper.apply(disposableObjArray);
        });
    }

    /**
     *
     * @param <T>
     * @param n
     * @param pairCmp
     * @param keyFunc
     * @return
     */
    private <T> DataSet top(final int n, final Comparator<Indexed<T>> pairCmp, final IntFunction<T> keyFunc) {
        final int size = size();
        final Queue<Indexed<T>> heap = new PriorityQueue<>(n, pairCmp);
        Indexed<T> pair = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            pair = Indexed.of(keyFunc.apply(rowIndex), rowIndex);

            if (heap.size() >= n) {
                if (pairCmp.compare(heap.peek(), pair) < 0) {
                    heap.poll();
                    heap.add(pair);
                }
            } else {
                heap.offer(pair);
            }
        }

        final Indexed<Object>[] arrayOfPair = heap.toArray(new Indexed[heap.size()]);

        N.sort(arrayOfPair, (Comparator<Indexed<Object>>) (o1, o2) -> o1.index() - o2.index());

        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(arrayOfPair.length));
        }

        int rowIndex = 0;
        for (Indexed<Object> e : arrayOfPair) {
            rowIndex = e.index();

            for (int i = 0; i < columnCount; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
            }
        }

        final Properties<String, Object> newProperties = N.isEmpty(_properties) ? null : _properties.copy();

        return new RowDataSet(newColumnNameList, newColumnList, newProperties);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public DataSet distinct() {
        return distinctBy(this._columnNameList);
    }

    /**
     *
     * @param columnName
     * @return
     */
    @Override
    public DataSet distinctBy(final String columnName) {
        return distinctBy(columnName, Fn.identity());
    }

    /**
     *
     * @param <E>
     * @param columnName
     * @param keyMapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet distinctBy(final String columnName, final Throwables.Function<?, ?, E> keyMapper) throws E {
        final int columnIndex = checkColumnName(columnName);

        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final Properties<String, Object> newProperties = N.isEmpty(_properties) ? null : _properties.copy();

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, newProperties);
        }

        final Throwables.Function<Object, ?, E> keyMapper2 = (Throwables.Function<Object, ?, E>) keyMapper;
        final Set<Object> rowSet = N.newHashSet();
        Object key = null;
        Object value = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            value = _columnList.get(columnIndex).get(rowIndex);
            key = hashKey(keyMapper2 == null ? value : keyMapper2.apply(value));

            if (rowSet.add(key)) {
                for (int i = 0; i < columnCount; i++) {
                    newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, newProperties);
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @Override
    public DataSet distinctBy(final Collection<String> columnNames) {
        return distinctBy(columnNames, NULL_PARAM_INDICATOR_2);
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet distinctBy(final Collection<String> columnNames, final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper)
            throws E {
        final boolean isNullOrIdentityKeyMapper = keyMapper == null || keyMapper == Fn.identity();

        if (columnNames.size() == 1 && isNullOrIdentityKeyMapper) {
            return distinctBy(columnNames.iterator().next());
        }

        final int size = size();
        final int[] columnIndexes = checkColumnName(columnNames);

        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final Properties<String, Object> newProperties = N.isEmpty(_properties) ? null : _properties.copy();

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, newProperties);
        }

        final Set<Object> rowSet = N.newHashSet();
        Object[] row = Objectory.createObjectArray(columnCount);
        DisposableObjArray disposableArray = DisposableObjArray.wrap(row);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0, len = columnIndexes.length; i < len; i++) {
                row[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            if (isNullOrIdentityKeyMapper) {
                if (rowSet.add(Wrapper.of(row))) {
                    for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                        newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                    }

                    row = Objectory.createObjectArray(columnCount);
                }
            } else if (rowSet.add(hashKey(keyMapper.apply(disposableArray)))) {
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }
            }
        }

        if (row != null) {
            Objectory.recycle(row);
            row = null;
        }

        if (isNullOrIdentityKeyMapper) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) rowSet;

            for (Wrapper<Object[]> e : tmp) {
                Objectory.recycle(e.value());
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, newProperties);
    }

    /**
     *
     * @param <E>
     * @param filter
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(final Throwables.Predicate<? super DisposableObjArray, E> filter) throws E {
        return filter(filter, size());
    }

    /**
     *
     * @param <E>
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(Throwables.Predicate<? super DisposableObjArray, E> filter, int max) throws E {
        return filter(0, size(), filter);
    }

    /**
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(final int fromRowIndex, final int toRowIndex, final Throwables.Predicate<? super DisposableObjArray, E> filter)
            throws E {
        return filter(fromRowIndex, toRowIndex, filter, size());
    }

    /**
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(int fromRowIndex, int toRowIndex, Throwables.Predicate<? super DisposableObjArray, E> filter, int max)
            throws E {
        return filter(fromRowIndex, toRowIndex, this._columnNameList, filter, max);
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, Throwables.BiPredicate<?, ?, E> filter) throws E {
        return filter(columnNames, filter, size());
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, Throwables.BiPredicate<?, ?, E> filter, int max) throws E {
        return filter(0, size(), columnNames, filter, max);
    }

    /**
     *
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param filter
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, Throwables.BiPredicate<?, ?, E> filter)
            throws E {
        return filter(fromRowIndex, toRowIndex, columnNames, filter, size());
    }

    /**
     *
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, Throwables.BiPredicate<?, ?, E> filter,
            int max) throws E {
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter);

        @SuppressWarnings("rawtypes")
        final Throwables.BiPredicate<Object, Object, E> filter2 = (Throwables.BiPredicate) filter;
        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        final Properties<String, Object> newProperties = N.isEmpty(_properties) ? null : _properties.copy();

        if (size == 0 || max == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, newProperties);
        }

        int count = max;

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            if (filter2.test(column1.get(rowIndex), column2.get(rowIndex))) {
                if (--count < 0) {
                    break;
                }

                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, newProperties);
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(Tuple3<String, String, String> columnNames, Throwables.TriPredicate<?, ?, ?, E> filter) throws E {
        return filter(columnNames, filter, size());
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(Tuple3<String, String, String> columnNames, Throwables.TriPredicate<?, ?, ?, E> filter, int max) throws E {
        return filter(0, size(), columnNames, filter, max);
    }

    /**
     *
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param filter
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames,
            Throwables.TriPredicate<?, ?, ?, E> filter) throws E {
        return filter(fromRowIndex, toRowIndex, columnNames, filter, size());
    }

    /**
     *
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(final int fromRowIndex, final int toRowIndex, final Tuple3<String, String, String> columnNames,
            final Throwables.TriPredicate<?, ?, ?, E> filter, final int max) throws E {
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(columnNames._3));

        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter);

        @SuppressWarnings("rawtypes")
        final Throwables.TriPredicate<Object, Object, Object, E> filter2 = (Throwables.TriPredicate) filter;
        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        final Properties<String, Object> newProperties = N.isEmpty(_properties) ? null : _properties.copy();

        if (size == 0 || max == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, newProperties);
        }

        int count = max;

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            if (filter2.test(column1.get(rowIndex), column2.get(rowIndex), column3.get(rowIndex))) {
                if (--count < 0) {
                    break;
                }

                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, newProperties);
    }

    /**
     *
     * @param <E>
     * @param columnName
     * @param filter
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(final String columnName, final Throwables.Predicate<?, E> filter) throws E {
        return filter(columnName, filter, size());
    }

    /**
     *
     * @param <E>
     * @param columnName
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(final String columnName, Throwables.Predicate<?, E> filter, int max) throws E {
        return filter(0, size(), columnName, filter, max);
    }

    /**
     *
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnName
     * @param filter
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(final int fromRowIndex, final int toRowIndex, final String columnName, final Throwables.Predicate<?, E> filter)
            throws E {
        return filter(fromRowIndex, toRowIndex, columnName, filter, size());
    }

    /**
     *
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnName
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(int fromRowIndex, int toRowIndex, final String columnName, Throwables.Predicate<?, E> filter, int max)
            throws E {
        final int filterColumnIndex = checkColumnName(columnName);
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter, "filter");
        N.checkArgNotNegative(max, "max");

        final Throwables.Predicate<Object, E> filterToUse = (Throwables.Predicate<Object, E>) filter;

        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        final Properties<String, Object> newProperties = N.isEmpty(_properties) ? null : _properties.copy();

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, newProperties);
        }

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            if (filterToUse.test(_columnList.get(filterColumnIndex).get(rowIndex))) {
                if (--max < 0) {
                    break;
                }

                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, newProperties);
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(final Collection<String> columnNames, final Throwables.Predicate<? super DisposableObjArray, E> filter)
            throws E {
        return filter(columnNames, filter, size());
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(Collection<String> columnNames, Throwables.Predicate<? super DisposableObjArray, E> filter, int max) throws E {
        return filter(0, size(), columnNames, filter, max);
    }

    /**
     *
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param filter
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Throwables.Predicate<? super DisposableObjArray, E> filter) throws E {
        return filter(fromRowIndex, toRowIndex, columnNames, filter, size());
    }

    /**
     *
     *
     * @param <E>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet filter(int fromRowIndex, int toRowIndex, Collection<String> columnNames,
            Throwables.Predicate<? super DisposableObjArray, E> filter, int max) throws E {
        final int[] filterColumnIndexes = checkColumnName(columnNames);
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter, "filter");
        N.checkArgNotNegative(max, "max");

        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        final Properties<String, Object> newProperties = N.isEmpty(_properties) ? null : _properties.copy();

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, newProperties);
        }

        final int filterColumnCount = filterColumnIndexes.length;
        final Object[] values = new Object[filterColumnCount];
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(values);

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            for (int i = 0; i < filterColumnCount; i++) {
                values[i] = _columnList.get(filterColumnIndexes[i]).get(rowIndex);
            }

            if (filter.test(disposableArray)) {
                if (--max < 0) {
                    break;
                }

                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, newProperties);
    }

    /**
     *
     * @param <E>
     * @param fromColumnName
     * @param newColumnName
     * @param copyingColumnName
     * @param mapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet map(final String fromColumnName, final String newColumnName, final String copyingColumnName,
            final Throwables.Function<?, ?, E> mapper) throws E {
        return map(fromColumnName, newColumnName, Array.asList(copyingColumnName), mapper);
    }

    /**
     *
     * @param <E>
     * @param fromColumnName
     * @param newColumnName
     * @param copyingColumnNames
     * @param mapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet map(final String fromColumnName, final String newColumnName, final Collection<String> copyingColumnNames,
            final Throwables.Function<?, ?, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final int fromColumnIndex = checkColumnName(fromColumnName);
        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.Function<Object, Object, E> mapperToUse = (Throwables.Function<Object, Object, E>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        for (Object val : _columnList.get(fromColumnIndex)) {
            mappedColumn.add(mapperToUse.apply(val));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.notEmpty(copyingColumnNames)) {
            newColumnNameList.addAll(copyingColumnNames);

            for (int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param newColumnName
     * @param copyingColumnNames
     * @param mapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet map(final Tuple2<String, String> fromColumnNames, final String newColumnName,
            final Collection<String> copyingColumnNames, final Throwables.BiFunction<?, ?, ?, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.BiFunction<Object, Object, Object, E> mapperToUse = (Throwables.BiFunction<Object, Object, Object, E>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            mappedColumn.add(mapperToUse.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex)));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.notEmpty(copyingColumnNames)) {
            newColumnNameList.addAll(copyingColumnNames);

            for (int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param newColumnName
     * @param copyingColumnNames
     * @param mapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet map(final Tuple3<String, String, String> fromColumnNames, final String newColumnName,
            final Collection<String> copyingColumnNames, final Throwables.TriFunction<?, ?, ?, ?, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<Object> fromColumn3 = _columnList.get(checkColumnName(fromColumnNames._3));

        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.TriFunction<Object, Object, Object, Object, E> mapperToUse = (Throwables.TriFunction<Object, Object, Object, Object, E>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            mappedColumn.add(mapperToUse.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex), fromColumn3.get(rowIndex)));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.notEmpty(copyingColumnNames)) {
            newColumnNameList.addAll(copyingColumnNames);

            for (int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param newColumnName
     * @param copyingColumnNames
     * @param mapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet map(final Collection<String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final Throwables.Function<? super DisposableObjArray, ?, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final int[] fromColumnIndices = checkColumnName(fromColumnNames);
        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.Function<? super DisposableObjArray, Object, E> mapperToUse = (Throwables.Function<? super DisposableObjArray, Object, E>) mapper;
        final int size = size();
        final int fromColumnCount = fromColumnIndices.length;
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);
        final Object[] tmpRow = new Object[fromColumnCount];
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(tmpRow);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < fromColumnCount; i++) {
                tmpRow[i] = _columnList.get(fromColumnIndices[i]).get(rowIndex);
            }

            mappedColumn.add(mapperToUse.apply(disposableArray));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.notEmpty(copyingColumnNames)) {
            newColumnNameList.addAll(copyingColumnNames);

            for (int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param <E>
     * @param fromColumnName
     * @param newColumnName
     * @param copyingColumnName
     * @param mapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet flatMap(final String fromColumnName, final String newColumnName, final String copyingColumnName,
            final Throwables.Function<?, ? extends Collection<?>, E> mapper) throws E {
        return flatMap(fromColumnName, newColumnName, Array.asList(copyingColumnName), mapper);
    }

    /**
     *
     * @param <E>
     * @param fromColumnName
     * @param newColumnName
     * @param copyingColumnNames
     * @param mapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet flatMap(final String fromColumnName, final String newColumnName, final Collection<String> copyingColumnNames,
            final Throwables.Function<?, ? extends Collection<?>, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final int fromColumnIndex = checkColumnName(fromColumnName);
        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.Function<Object, Collection<Object>, E> mapperToUse = (Throwables.Function<Object, Collection<Object>, E>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.isEmpty(copyingColumnNames)) {
            Collection<Object> c = null;

            for (Object val : _columnList.get(fromColumnIndex)) {
                c = mapperToUse.apply(val);

                if (N.notEmpty(c)) {
                    mappedColumn.addAll(c);
                }
            }
        } else {
            newColumnNameList.addAll(copyingColumnNames);

            for (int i = 0; i < copyingColumnCount; i++) {
                newColumnList.add(new ArrayList<>(size));
            }

            final List<Object> fromColumn = _columnList.get(fromColumnIndex);
            Collection<Object> c = null;
            List<Object> copyingColumn = null;
            Object val = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                c = mapperToUse.apply(fromColumn.get(rowIndex));

                if (N.notEmpty(c)) {
                    mappedColumn.addAll(c);

                    for (int i = 0; i < copyingColumnCount; i++) {
                        val = _columnList.get(copyingColumnIndices[i]).get(rowIndex);
                        copyingColumn = newColumnList.get(i + 1);

                        for (int j = 0, len = c.size(); j < len; j++) {
                            copyingColumn.add(val);
                        }
                    }
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param newColumnName
     * @param copyingColumnNames
     * @param mapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet flatMap(final Tuple2<String, String> fromColumnNames, final String newColumnName,
            final Collection<String> copyingColumnNames, final Throwables.BiFunction<?, ?, ? extends Collection<?>, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));

        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.BiFunction<Object, Object, Collection<Object>, E> mapperToUse = (Throwables.BiFunction<Object, Object, Collection<Object>, E>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.isEmpty(copyingColumnNames)) {
            Collection<Object> c = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                c = mapperToUse.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex));

                if (N.notEmpty(c)) {
                    mappedColumn.addAll(c);
                }
            }
        } else {
            newColumnNameList.addAll(copyingColumnNames);

            for (int i = 0; i < copyingColumnCount; i++) {
                newColumnList.add(new ArrayList<>(size));
            }

            Collection<Object> c = null;
            List<Object> copyingColumn = null;
            Object val = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                c = mapperToUse.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex));

                if (N.notEmpty(c)) {
                    mappedColumn.addAll(c);

                    for (int i = 0; i < copyingColumnCount; i++) {
                        val = _columnList.get(copyingColumnIndices[i]).get(rowIndex);
                        copyingColumn = newColumnList.get(i + 1);

                        for (int j = 0, len = c.size(); j < len; j++) {
                            copyingColumn.add(val);
                        }
                    }
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param newColumnName
     * @param copyingColumnNames
     * @param mapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet flatMap(final Tuple3<String, String, String> fromColumnNames, final String newColumnName,
            final Collection<String> copyingColumnNames, final Throwables.TriFunction<?, ?, ?, ? extends Collection<?>, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<Object> fromColumn3 = _columnList.get(checkColumnName(fromColumnNames._3));

        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.TriFunction<Object, Object, Object, Collection<Object>, E> mapperToUse = (Throwables.TriFunction<Object, Object, Object, Collection<Object>, E>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.isEmpty(copyingColumnNames)) {
            Collection<Object> c = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                c = mapperToUse.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex), fromColumn3.get(rowIndex));

                if (N.notEmpty(c)) {
                    mappedColumn.addAll(c);
                }
            }
        } else {
            newColumnNameList.addAll(copyingColumnNames);

            for (int i = 0; i < copyingColumnCount; i++) {
                newColumnList.add(new ArrayList<>(size));
            }

            Collection<Object> c = null;
            List<Object> copyingColumn = null;
            Object val = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                c = mapperToUse.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex), fromColumn3.get(rowIndex));

                if (N.notEmpty(c)) {
                    mappedColumn.addAll(c);

                    for (int i = 0; i < copyingColumnCount; i++) {
                        val = _columnList.get(copyingColumnIndices[i]).get(rowIndex);
                        copyingColumn = newColumnList.get(i + 1);

                        for (int j = 0, len = c.size(); j < len; j++) {
                            copyingColumn.add(val);
                        }
                    }
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param <E>
     * @param fromColumnNames
     * @param newColumnName
     * @param copyingColumnNames
     * @param mapper
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet flatMap(final Collection<String> fromColumnNames, final String newColumnName,
            final Collection<String> copyingColumnNames, final Throwables.Function<? super DisposableObjArray, ? extends Collection<?>, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final int[] fromColumnIndices = checkColumnName(fromColumnNames);
        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.Function<? super DisposableObjArray, Collection<Object>, E> mapperToUse = (Throwables.Function<? super DisposableObjArray, Collection<Object>, E>) mapper;
        final int size = size();
        final int fromColumnCount = fromColumnIndices.length;
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        final Object[] tmpRow = new Object[fromColumnCount];
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(tmpRow);

        if (N.isEmpty(copyingColumnNames)) {
            Collection<Object> c = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                for (int j = 0; j < fromColumnCount; j++) {
                    tmpRow[j] = _columnList.get(fromColumnIndices[j]).get(rowIndex);
                }

                c = mapperToUse.apply(disposableArray);

                if (N.notEmpty(c)) {
                    mappedColumn.addAll(c);
                }
            }
        } else {
            newColumnNameList.addAll(copyingColumnNames);

            for (int i = 0; i < copyingColumnCount; i++) {
                newColumnList.add(new ArrayList<>(size));
            }

            Collection<Object> c = null;
            List<Object> copyingColumn = null;
            Object val = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                for (int j = 0; j < fromColumnCount; j++) {
                    tmpRow[j] = _columnList.get(fromColumnIndices[j]).get(rowIndex);
                }

                c = mapperToUse.apply(disposableArray);

                if (N.notEmpty(c)) {
                    mappedColumn.addAll(c);

                    for (int i = 0; i < copyingColumnCount; i++) {
                        val = _columnList.get(copyingColumnIndices[i]).get(rowIndex);
                        copyingColumn = newColumnList.get(i + 1);

                        for (int j = 0, len = c.size(); j < len; j++) {
                            copyingColumn.add(val);
                        }
                    }
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public DataSet copy() {
        return copy(0, size(), _columnNameList);
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @Override
    public DataSet copy(final Collection<String> columnNames) {
        return copy(0, size(), columnNames);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public DataSet copy(final int fromRowIndex, final int toRowIndex) {
        return copy(fromRowIndex, toRowIndex, _columnNameList);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public DataSet copy(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        return copy(fromRowIndex, toRowIndex, columnNames, this.checkColumnName(columnNames), true);
    }

    private RowDataSet copy(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final int[] columnIndexes,
            final boolean copyProperties) {

        final List<String> newColumnNameList = new ArrayList<>(columnNames);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnNameList.size());

        if (fromRowIndex == 0 && toRowIndex == size()) {
            for (int columnIndex : columnIndexes) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        } else {
            for (int columnIndex : columnIndexes) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex).subList(fromRowIndex, toRowIndex)));
            }
        }

        final Properties<String, Object> newProperties = copyProperties && N.notEmpty(_properties) ? _properties.copy() : null;

        return new RowDataSet(newColumnNameList, newColumnList, newProperties);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public DataSet clone() { //NOSONAR
        return clone(this._isFrozen);
    }

    /**
     *
     * @param freeze
     * @return
     */
    @Override
    public DataSet clone(boolean freeze) { //NOSONAR
        RowDataSet dataSet = null;

        if (kryoParser != null) {
            dataSet = kryoParser.clone(this);
        } else {
            dataSet = jsonParser.deserialize(jsonParser.serialize(this), RowDataSet.class);
        }

        dataSet._isFrozen = freeze;
        return dataSet;
    }

    /**
     *
     * @param right
     * @param columnName
     * @param joinColumnNameOnRight
     * @return
     */
    @Override
    public DataSet innerJoin(final DataSet right, final String columnName, final String joinColumnNameOnRight) {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return innerJoin(right, onColumnNames);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @return
     */
    @Override
    public DataSet innerJoin(final DataSet right, final Map<String, String> onColumnNames) {
        return join(right, onColumnNames, false);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType
     * @return
     */
    @Override
    public DataSet innerJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType) {
        return join(right, onColumnNames, newColumnName, newColumnType, false);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType
     * @param collSupplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public DataSet innerJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) {
        return join(right, onColumnNames, newColumnName, newColumnType, collSupplier, false);
    }

    /**
     *
     * @param right
     * @param columnName
     * @param joinColumnNameOnRight
     * @return
     */
    @Override
    public DataSet leftJoin(final DataSet right, final String columnName, final String joinColumnNameOnRight) {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return leftJoin(right, onColumnNames);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @return
     */
    @Override
    public DataSet leftJoin(final DataSet right, final Map<String, String> onColumnNames) {
        return join(right, onColumnNames, true);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param isLeftJoin
     * @return
     */
    private DataSet join(final DataSet right, final Map<String, String> onColumnNames, final boolean isLeftJoin) {
        checkJoinOnColumnNames(onColumnNames);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());
            final List<String> rightColumnNames = getRightColumnNames(right, onColumnEntry.getValue());
            final int newColumnSize = columnCount() + rightColumnNames.size();
            final List<String> newColumnNameList = new ArrayList<>(newColumnSize);
            final List<List<Object>> newColumnList = new ArrayList<>(newColumnSize);

            initNewColumnList(newColumnNameList, newColumnList, _columnNameList, rightColumnNames);

            if (this.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);
            List<Integer> rightRowIndexList = null;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                join(newColumnList, right, isLeftJoin, leftRowIndex, rightRowIndexList, rightColumnIndexes);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];
            final List<String> rightColumnNames = new ArrayList<>(right.columnNameList());

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames, rightColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, _columnNameList, rightColumnNames);

            if (this.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final Map<Object[], List<Integer>> joinColumnRightRowIndexMap = new ArrayHashMap<>();
            Object[] row = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                row = row == null ? Objectory.createObjectArray(rightJoinColumnIndexes.length) : row;

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);
            row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
            List<Integer> rightRowIndexList = null;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(row);

                join(newColumnList, right, isLeftJoin, leftRowIndex, rightRowIndexList, rightColumnIndexes);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (Object[] a : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param isLeftJoin
     * @param leftRowIndex
     * @param rightRowIndexList
     * @param rightColumnIndexes
     */
    private void join(final List<List<Object>> newColumnList, final DataSet right, final boolean isLeftJoin, int leftRowIndex, List<Integer> rightRowIndexList,
            final int[] rightColumnIndexes) {
        if (N.notEmpty(rightRowIndexList)) {
            final int rightRowSize = rightRowIndexList.size();
            List<Object> newColumn = null;
            Object val = null;

            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                val = _columnList.get(i).get(leftRowIndex);
                newColumn = newColumnList.get(i);

                for (int j = 0; j < rightRowSize; j++) {
                    newColumn.add(val);
                }
            }

            List<Object> column = null;

            for (int i = 0, leftColumnLength = columnCount(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                newColumn = newColumnList.get(leftColumnLength + i);
                column = right.getColumn(rightColumnIndexes[i]);

                for (int rightRowIndex : rightRowIndexList) {
                    newColumn.add(column.get(rightRowIndex));
                }
            }
        } else if (isLeftJoin) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            for (int i = 0, leftColumnLength = columnCount(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                newColumnList.get(leftColumnLength + i).add(null);
            }
        }
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType
     * @param isLeftJoin
     * @return
     */
    private DataSet join(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final boolean isLeftJoin) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());
            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            if (this.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            List<Integer> rightRowIndexList = null;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                join(newColumnList, right, isLeftJoin, newColumnType, newColumnIndex, leftRowIndex, rightRowIndexList);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            if (this.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final Map<Object[], List<Integer>> joinColumnRightRowIndexMap = new ArrayHashMap<>();
            Object[] row = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                row = row == null ? Objectory.createObjectArray(rightJoinColumnIndexes.length) : row;

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            List<Integer> rightRowIndexList = null;
            row = Objectory.createObjectArray(leftJoinColumnIndexes.length);

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(row);

                join(newColumnList, right, isLeftJoin, newColumnType, newColumnIndex, leftRowIndex, rightRowIndexList);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (Object[] a : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param isLeftJoin
     * @param newColumnType
     * @param newColumnIndex
     * @param leftRowIndex
     * @param rightRowIndexList
     */
    private void join(final List<List<Object>> newColumnList, final DataSet right, final boolean isLeftJoin, final Class<?> newColumnType,
            final int newColumnIndex, int leftRowIndex, List<Integer> rightRowIndexList) {
        if (N.notEmpty(rightRowIndexList)) {
            for (int rightRowIndex : rightRowIndexList) {
                for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                    newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
                }

                newColumnList.get(newColumnIndex).add(right.getRow(rightRowIndex, newColumnType));
            }
        } else if (isLeftJoin) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            newColumnList.get(newColumnIndex).add(null);
        }
    }

    /**
     * Check join on column names.
     *
     * @param onColumnNames
     */
    private void checkJoinOnColumnNames(final Map<String, String> onColumnNames) {
        if (N.isEmpty(onColumnNames)) {
            throw new IllegalArgumentException("The joining column names can't be null or empty");
        }
    }

    /**
     * Check ref column name.
     *
     * @param right
     * @param joinColumnNameOnRight
     * @return
     */
    private int checkRightJoinColumnName(final DataSet right, final String joinColumnNameOnRight) {
        final int rightJoinColumnIndex = right.getColumnIndex(joinColumnNameOnRight);

        if (rightJoinColumnIndex < 0) {
            throw new IllegalArgumentException(
                    "The specified column: " + joinColumnNameOnRight + " is not included in the right DataSet: " + right.columnNameList());
        }

        return rightJoinColumnIndex;
    }

    /**
     * Check new column name.
     *
     * @param newColumnName
     */
    private void checkNewColumnName(final String newColumnName) {
        if (this.containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
        }
    }

    /**
     * Gets the right column names.
     *
     * @param right
     * @param joinColumnNameOnRight
     * @return
     */
    private List<String> getRightColumnNames(final DataSet right, @SuppressWarnings("unused") final String joinColumnNameOnRight) { // NOSONAR
        final List<String> rightColumnNames = new ArrayList<>(right.columnNameList());

        // How to handle join columns with same name for full join.
        //    if (this.containsColumn(joinColumnNameOnRight)) {
        //        rightColumnNames.remove(joinColumnNameOnRight);
        //    }

        return rightColumnNames;
    }

    /**
     * Inits the column indexes.
     *
     * @param leftJoinColumnIndexes
     * @param rightJoinColumnIndexes
     * @param right
     * @param onColumnNames
     * @param rightColumnNames
     */
    private void initColumnIndexes(final int[] leftJoinColumnIndexes, final int[] rightJoinColumnIndexes, final DataSet right,
            final Map<String, String> onColumnNames, @SuppressWarnings("unused") final List<String> rightColumnNames) { // NOSONAR
        int i = 0;
        for (Map.Entry<String, String> entry : onColumnNames.entrySet()) {
            leftJoinColumnIndexes[i] = checkColumnName(entry.getKey());
            rightJoinColumnIndexes[i] = right.getColumnIndex(entry.getValue());

            if (rightJoinColumnIndexes[i] < 0) {
                throw new IllegalArgumentException(
                        "The specified column: " + entry.getValue() + " is not included in the right DataSet: " + right.columnNameList());
            }

            // How to handle join columns with same name for full join.
            //    if (entry.getKey().equals(entry.getValue())) {
            //        rightColumnNames.remove(entry.getValue());
            //    }

            i++;
        }
    }

    /**
     * Inits the column indexes.
     *
     * @param leftJoinColumnIndexes
     * @param rightJoinColumnIndexes
     * @param right
     * @param onColumnNames
     */
    private void initColumnIndexes(final int[] leftJoinColumnIndexes, final int[] rightJoinColumnIndexes, final DataSet right,
            final Map<String, String> onColumnNames) {
        int i = 0;
        for (Map.Entry<String, String> entry : onColumnNames.entrySet()) {
            leftJoinColumnIndexes[i] = checkColumnName(entry.getKey());
            rightJoinColumnIndexes[i] = right.getColumnIndex(entry.getValue());

            if (rightJoinColumnIndexes[i] < 0) {
                throw new IllegalArgumentException(
                        "The specified column: " + entry.getValue() + " is not included in the right DataSet: " + right.columnNameList());
            }

            i++;
        }
    }

    /**
     * Inits the new column list.
     *
     * @param newColumnNameList
     * @param newColumnList
     * @param rightColumnNames
     */
    private void initNewColumnList(final List<String> newColumnNameList, final List<List<Object>> newColumnList, final List<String> leftColumnNames,
            final List<String> rightColumnNames) {
        //    for (String rightColumnName : rightColumnNames) {
        //        if (this.containsColumn(rightColumnName)) {
        //            throw new IllegalArgumentException("The column name: " + rightColumnName + " is already included this DataSet: " + _columnNameList);
        //        }
        //    }

        for (String columnName : leftColumnNames) {
            newColumnNameList.add(columnName);
            newColumnList.add(new ArrayList<>());
        }

        for (String columnName : rightColumnNames) {
            if (leftColumnNames.contains(columnName)) {
                newColumnNameList.add(columnName + POSTFIX_FOR_SAME_JOINED_COLUMN_NAME);
            } else {
                newColumnNameList.add(columnName);
            }

            newColumnList.add(new ArrayList<>());
        }
    }

    /**
     * Inits the new column list.
     *
     * @param newColumnNameList
     * @param newColumnList
     * @param newColumnName
     */
    private void initNewColumnList(final List<String> newColumnNameList, final List<List<Object>> newColumnList, final String newColumnName) {
        for (int i = 0, len = columnCount(); i < len; i++) {
            newColumnNameList.add(_columnNameList.get(i));
            newColumnList.add(new ArrayList<>());
        }

        newColumnNameList.add(newColumnName);
        newColumnList.add(new ArrayList<>());
    }

    /**
     * Put row index.
     *
     * @param joinColumnRightRowIndexMap
     * @param hashKey
     * @param rightRowIndex
     */
    private void putRowIndex(final Map<Object, List<Integer>> joinColumnRightRowIndexMap, Object hashKey, int rightRowIndex) {
        List<Integer> rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

        if (rightRowIndexList == null) {
            joinColumnRightRowIndexMap.put(hashKey, N.asList(rightRowIndex));
        } else {
            rightRowIndexList.add(rightRowIndex);
        }
    }

    /**
     * Put row index.
     *
     * @param joinColumnRightRowIndexMap
     * @param row
     * @param rightRowIndex
     * @return
     */
    private Object[] putRowIndex(final Map<Object[], List<Integer>> joinColumnRightRowIndexMap, Object[] row, int rightRowIndex) {
        List<Integer> rightRowIndexList = joinColumnRightRowIndexMap.get(row);

        if (rightRowIndexList == null) {
            joinColumnRightRowIndexMap.put(row, N.asList(rightRowIndex));
            row = null;
        } else {
            rightRowIndexList.add(rightRowIndex);
        }

        return row;
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType
     * @return
     */
    @Override
    public DataSet leftJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType) {
        return join(right, onColumnNames, newColumnName, newColumnType, true);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType
     * @param collSupplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public DataSet leftJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) {
        return join(right, onColumnNames, newColumnName, newColumnType, collSupplier, true);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType
     * @param collSupplier
     * @param isLeftJoin
     * @return
     */
    @SuppressWarnings("rawtypes")
    private DataSet join(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final boolean isLeftJoin) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);
        N.checkArgNotNull(collSupplier);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            if (this.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            List<Integer> rightRowIndexList = null;
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                join(newColumnList, right, isLeftJoin, newColumnType, collSupplier, newColumnIndex, leftRowIndex, rightRowIndexList);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            if (this.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final Map<Object[], List<Integer>> joinColumnRightRowIndexMap = new ArrayHashMap<>();
            List<Integer> rightRowIndexList = null;
            Object[] row = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                row = row == null ? Objectory.createObjectArray(rightJoinColumnIndexes.length) : row;

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            row = Objectory.createObjectArray(leftJoinColumnIndexes.length);

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(row);

                join(newColumnList, right, isLeftJoin, newColumnType, collSupplier, newColumnIndex, leftRowIndex, rightRowIndexList);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (Object[] a : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param isLeftJoin
     * @param newColumnType
     * @param collSupplier
     * @param newColumnIndex
     * @param leftRowIndex
     * @param rightRowIndexList
     */
    @SuppressWarnings("rawtypes")
    private void join(final List<List<Object>> newColumnList, final DataSet right, final boolean isLeftJoin, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, int leftRowIndex, List<Integer> rightRowIndexList) {
        if (N.notEmpty(rightRowIndexList)) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

            for (int rightRowIndex : rightRowIndexList) {
                coll.add(right.getRow(rightRowIndex, newColumnType));
            }

            newColumnList.get(newColumnIndex).add(coll);
        } else if (isLeftJoin) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            newColumnList.get(newColumnIndex).add(null);
        }
    }

    /**
     *
     * @param right
     * @param columnName
     * @param joinColumnNameOnRight
     * @return
     */
    @Override
    public DataSet rightJoin(final DataSet right, final String columnName, final String joinColumnNameOnRight) {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return rightJoin(right, onColumnNames);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @return
     */
    @Override
    public DataSet rightJoin(final DataSet right, final Map<String, String> onColumnNames) {
        checkJoinOnColumnNames(onColumnNames);

        if (onColumnNames.size() == 0) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());
            final List<String> leftColumnNames = getLeftColumnNamesForRightJoin(onColumnEntry.getValue());
            final List<String> rightColumnNames = right.columnNameList();

            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, leftColumnNames, rightColumnNames);

            if (right.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            List<Integer> leftRowIndexList = null;
            Object hashKey = null;

            for (int leftRowIndex = 0, leftDataSetSize = this.size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final int[] leftColumnIndexes = this.getColumnIndexes(leftColumnNames);
            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                leftRowIndexList = joinColumnLeftRowIndexMap.get(hashKey);

                rightJoin(newColumnList, right, rightRowIndex, rightColumnIndexes, leftColumnIndexes, leftRowIndexList);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final List<String> rightColumnNames = right.columnNameList();
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexesForRightJoin(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames, leftColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, leftColumnNames, rightColumnNames);

            if (right.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final Map<Object[], List<Integer>> joinColumnLeftRowIndexMap = new ArrayHashMap<>();
            Object[] row = null;

            for (int leftRowIndex = 0, leftDataSetSize = this.size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                row = row == null ? Objectory.createObjectArray(leftJoinColumnIndexes.length) : row;

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnLeftRowIndexMap, row, leftRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int[] leftColumnIndexes = this.getColumnIndexes(leftColumnNames);
            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);
            row = Objectory.createObjectArray(rightJoinColumnIndexes.length);
            List<Integer> leftRowIndexList = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                leftRowIndexList = joinColumnLeftRowIndexMap.get(row);

                rightJoin(newColumnList, right, rightRowIndex, rightColumnIndexes, leftColumnIndexes, leftRowIndexList);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (Object[] a : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param rightRowIndex
     * @param rightColumnIndexes
     * @param leftColumnIndexes
     * @param leftRowIndexList
     */
    private void rightJoin(final List<List<Object>> newColumnList, final DataSet right, int rightRowIndex, final int[] rightColumnIndexes,
            final int[] leftColumnIndexes, List<Integer> leftRowIndexList) {
        if (N.notEmpty(leftRowIndexList)) {
            for (int leftRowIndex : leftRowIndexList) {
                for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                    newColumnList.get(i).add(this.get(leftRowIndex, leftColumnIndexes[i]));
                }

                for (int i = 0, leftColumnLength = leftColumnIndexes.length, rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                    newColumnList.get(i + leftColumnLength).add(right.get(rightRowIndex, rightColumnIndexes[i]));
                }
            }
        } else {
            for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            for (int i = 0, leftColumnLength = leftColumnIndexes.length, rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                newColumnList.get(i + leftColumnLength).add(right.get(rightRowIndex, rightColumnIndexes[i]));
            }
        }
    }

    /**
     * Inits the column indexes for right join.
     *
     * @param leftJoinColumnIndexes
     * @param rightJoinColumnIndexes
     * @param right
     * @param onColumnNames
     * @param leftColumnNames
     */
    private void initColumnIndexesForRightJoin(final int[] leftJoinColumnIndexes, final int[] rightJoinColumnIndexes, final DataSet right,
            final Map<String, String> onColumnNames, @SuppressWarnings("unused") final List<String> leftColumnNames) { // NOSONAR
        int i = 0;
        for (Map.Entry<String, String> entry : onColumnNames.entrySet()) {
            leftJoinColumnIndexes[i] = checkColumnName(entry.getKey());
            rightJoinColumnIndexes[i] = right.getColumnIndex(entry.getValue());

            if (rightJoinColumnIndexes[i] < 0) {
                throw new IllegalArgumentException(
                        "The specified column: " + entry.getValue() + " is not included in the right DataSet: " + right.columnNameList());
            }

            // How to handle join columns with same name for full join.
            //    if (entry.getKey().equals(entry.getValue())) {
            //        leftColumnNames.remove(entry.getKey());
            //    }

            i++;
        }
    }

    //    /**
    //     * Inits the new column list for right join.
    //     *
    //     * @param newColumnNameList
    //     * @param newColumnList
    //     * @param right
    //     * @param leftColumnNames
    //     * @param rightColumnNames
    //     */
    //    private void initNewColumnListForRightJoin(final List<String> newColumnNameList, final List<List<Object>> newColumnList, final DataSet right,
    //            final List<String> leftColumnNames, final List<String> rightColumnNames) {
    //        for (String leftColumnName : leftColumnNames) {
    //            if (right.containsColumn(leftColumnName)) {
    //                throw new IllegalArgumentException(
    //                        "The column in this DataSet: " + leftColumnName + " is already included in right DataSet: " + rightColumnNames);
    //            }
    //
    //            newColumnList.add(new ArrayList<>());
    //        }
    //
    //        for (String rightColumnName : rightColumnNames) {
    //            newColumnNameList.add(rightColumnName);
    //            newColumnList.add(new ArrayList<>());
    //        }
    //    }

    /**
     * Gets the left column names for right join.
     *
     * @param joinColumnNameOnRight
     * @return
     */
    private List<String> getLeftColumnNamesForRightJoin(@SuppressWarnings("unused") final String joinColumnNameOnRight) { // NOSONAR
        final List<String> leftColumnNames = new ArrayList<>(_columnNameList);

        // How to handle join columns with same name for full join.
        //    if (this.containsColumn(joinColumnNameOnRight)) {
        //        leftColumnNames.remove(joinColumnNameOnRight);
        //    }

        return leftColumnNames;
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType
     * @return
     */
    @Override
    public DataSet rightJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());

            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + 1);

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, leftColumnNames, newColumnName);

            if (right.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int leftRowIndex = 0, leftDataSetSize = this.size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final int[] leftColumnIndexes = this.getColumnIndexes(leftColumnNames);
            List<Integer> leftRowIndexList = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                leftRowIndexList = joinColumnLeftRowIndexMap.get(hashKey);

                rightJoin(newColumnList, right, newColumnType, newColumnIndex, rightRowIndex, leftRowIndexList, leftColumnIndexes);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + 1);

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, leftColumnNames, newColumnName);

            if (right.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final Map<Object[], List<Integer>> joinColumnLeftRowIndexMap = new ArrayHashMap<>();
            Object[] row = null;

            for (int leftRowIndex = 0, leftDataSetSize = this.size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                row = row == null ? Objectory.createObjectArray(leftJoinColumnIndexes.length) : row;

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnLeftRowIndexMap, row, leftRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final int[] leftColumnIndexes = this.getColumnIndexes(leftColumnNames);
            row = Objectory.createObjectArray(rightJoinColumnIndexes.length);
            List<Integer> leftRowIndexList = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                leftRowIndexList = joinColumnLeftRowIndexMap.get(row);

                rightJoin(newColumnList, right, newColumnType, newColumnIndex, rightRowIndex, leftRowIndexList, leftColumnIndexes);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (Object[] a : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param newColumnType
     * @param newColumnIndex
     * @param rightRowIndex
     * @param leftRowIndexList
     * @param leftColumnIndexes
     */
    private void rightJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType, final int newColumnIndex,
            int rightRowIndex, List<Integer> leftRowIndexList, final int[] leftColumnIndexes) {
        if (N.notEmpty(leftRowIndexList)) {
            for (int leftRowIndex : leftRowIndexList) {
                for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                    newColumnList.get(i).add(this.get(leftRowIndex, leftColumnIndexes[i]));
                }

                newColumnList.get(newColumnIndex).add(right.getRow(rightRowIndex, newColumnType));
            }
        } else {
            for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            newColumnList.get(newColumnIndex).add(right.getRow(rightRowIndex, newColumnType));
        }
    }

    /**
     * Inits the new column list for right join.
     *
     * @param newColumnNameList
     * @param newColumnList
     * @param leftColumnNames
     * @param newColumnName
     */
    private void initNewColumnListForRightJoin(final List<String> newColumnNameList, final List<List<Object>> newColumnList, final List<String> leftColumnNames,
            final String newColumnName) {
        for (String leftColumnName : leftColumnNames) {
            newColumnNameList.add(leftColumnName);
            newColumnList.add(new ArrayList<>());
        }

        newColumnNameList.add(newColumnName);
        newColumnList.add(new ArrayList<>());
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType
     * @param collSupplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public DataSet rightJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);
        N.checkArgNotNull(collSupplier);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());

            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + 1);

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, leftColumnNames, newColumnName);

            if (right.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int leftRowIndex = 0, leftDataSetSize = this.size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final int[] leftColumnIndexes = this.getColumnIndexes(leftColumnNames);
            List<Integer> leftRowIndexList = null;
            List<Integer> rightRowIndexList = null;

            for (Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                leftRowIndexList = joinColumnLeftRowIndexMap.get(rightRowIndexEntry.getKey());
                rightRowIndexList = rightRowIndexEntry.getValue();

                rightJoin(newColumnList, right, newColumnType, collSupplier, newColumnIndex, leftColumnIndexes, leftRowIndexList, rightRowIndexList);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + 1);

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, leftColumnNames, newColumnName);

            if (right.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final Map<Object[], List<Integer>> joinColumnLeftRowIndexMap = new ArrayHashMap<>();
            Object[] row = null;

            for (int leftRowIndex = 0, leftDataSetSize = this.size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                row = row == null ? Objectory.createObjectArray(leftJoinColumnIndexes.length) : row;

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnLeftRowIndexMap, row, leftRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final Map<Object[], List<Integer>> joinColumnRightRowIndexMap = new ArrayHashMap<>(LinkedHashMap.class);

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                row = row == null ? Objectory.createObjectArray(rightJoinColumnIndexes.length) : row;

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final int[] leftColumnIndexes = this.getColumnIndexes(leftColumnNames);
            List<Integer> leftRowIndexList = null;
            List<Integer> rightRowIndexList = null;

            for (Map.Entry<Object[], List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                leftRowIndexList = joinColumnLeftRowIndexMap.get(rightRowIndexEntry.getKey());
                rightRowIndexList = rightRowIndexEntry.getValue();

                rightJoin(newColumnList, right, newColumnType, collSupplier, newColumnIndex, leftColumnIndexes, leftRowIndexList, rightRowIndexList);
            }

            for (Object[] a : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            for (Object[] a : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param newColumnType
     * @param collSupplier
     * @param newColumnIndex
     * @param leftColumnIndexes
     * @param leftRowIndexList
     * @param rightRowIndexList
     */
    @SuppressWarnings("rawtypes")
    private void rightJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final int[] leftColumnIndexes, List<Integer> leftRowIndexList,
            List<Integer> rightRowIndexList) {
        if (N.notEmpty(leftRowIndexList)) {
            for (int leftRowIndex : leftRowIndexList) {
                for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                    newColumnList.get(i).add(this.get(leftRowIndex, leftColumnIndexes[i]));
                }

                final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

                for (int righRowIndex : rightRowIndexList) {
                    coll.add(right.getRow(righRowIndex, newColumnType));
                }

                newColumnList.get(newColumnIndex).add(coll);
            }
        } else {
            for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

            for (int righRowIndex : rightRowIndexList) {
                coll.add(right.getRow(righRowIndex, newColumnType));
            }

            newColumnList.get(newColumnIndex).add(coll);
        }
    }

    /**
     *
     * @param right
     * @param columnName
     * @param joinColumnNameOnRight
     * @return
     */
    @Override
    public DataSet fullJoin(final DataSet right, final String columnName, final String joinColumnNameOnRight) {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return fullJoin(right, onColumnNames);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @return
     */
    @Override
    public DataSet fullJoin(final DataSet right, final Map<String, String> onColumnNames) {
        checkJoinOnColumnNames(onColumnNames);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());
            final List<String> rightColumnNames = getRightColumnNames(right, onColumnEntry.getValue());

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, _columnNameList, rightColumnNames);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            List<Integer> rightRowIndexList = null;
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);
            final Set<Object> joinColumnLeftRowIndexSet = N.newHashSet();

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                fullJoin(newColumnList, right, leftRowIndex, rightRowIndexList, rightColumnIndexes);

                joinColumnLeftRowIndexSet.add(hashKey);
            }

            for (Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexSet.contains(rightRowIndexEntry.getKey())) {
                    fullJoin(newColumnList, right, rightRowIndexEntry.getValue(), rightColumnIndexes);
                }
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];
            final List<String> rightColumnNames = new ArrayList<>(right.columnNameList());

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames, rightColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, _columnNameList, rightColumnNames);

            final Map<Object[], List<Integer>> joinColumnRightRowIndexMap = new ArrayHashMap<>(LinkedHashMap.class);
            List<Integer> rightRowIndexList = null;
            Object[] row = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                row = row == null ? Objectory.createObjectArray(rightJoinColumnIndexes.length) : row;

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);
            final Map<Object[], Integer> joinColumnLeftRowIndexMap = new ArrayHashMap<>();

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                row = row == null ? Objectory.createObjectArray(rightJoinColumnIndexes.length) : row;

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(row);

                fullJoin(newColumnList, right, leftRowIndex, rightRowIndexList, rightColumnIndexes);

                if (!joinColumnLeftRowIndexMap.containsKey(row)) {
                    joinColumnLeftRowIndexMap.put(row, leftRowIndex);
                    row = null;
                }
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (Map.Entry<Object[], List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexMap.containsKey(rightRowIndexEntry.getKey())) {
                    fullJoin(newColumnList, right, rightRowIndexEntry.getValue(), rightColumnIndexes);
                }
            }

            for (Object[] a : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            for (Object[] a : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param rightRowIndexList
     * @param rightColumnIndexes
     */
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, List<Integer> rightRowIndexList, final int[] rightColumnIndexes) {
        for (int rightRowIndex : rightRowIndexList) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            for (int i = 0, leftColumnLength = columnCount(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                newColumnList.get(leftColumnLength + i).add(right.get(rightRowIndex, rightColumnIndexes[i]));
            }
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param leftRowIndex
     * @param rightRowIndexList
     * @param rightColumnIndexes
     */
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, int leftRowIndex, List<Integer> rightRowIndexList,
            final int[] rightColumnIndexes) {
        if (N.notEmpty(rightRowIndexList)) {
            for (int rightRowIndex : rightRowIndexList) {
                for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                    newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
                }

                for (int i = 0, leftColumnLength = columnCount(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                    newColumnList.get(leftColumnLength + i).add(right.get(rightRowIndex, rightColumnIndexes[i]));
                }
            }
        } else {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            for (int i = 0, leftColumnLength = columnCount(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                newColumnList.get(leftColumnLength + i).add(null);
            }
        }
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType
     * @return
     */
    @Override
    public DataSet fullJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            List<Integer> rightRowIndexList = null;
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final Set<Object> joinColumnLeftRowIndexSet = N.newHashSet();

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                fullJoin(newColumnList, right, newColumnType, newColumnIndex, leftRowIndex, rightRowIndexList);

                joinColumnLeftRowIndexSet.add(hashKey);
            }

            for (Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexSet.contains(rightRowIndexEntry.getKey())) {
                    fullJoin(newColumnList, right, newColumnType, newColumnIndex, rightRowIndexEntry.getValue());
                }
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            final Map<Object[], List<Integer>> joinColumnRightRowIndexMap = new ArrayHashMap<>(LinkedHashMap.class);
            List<Integer> rightRowIndexList = null;
            Object[] row = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                row = row == null ? Objectory.createObjectArray(rightJoinColumnIndexes.length) : row;

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final Map<Object[], Integer> joinColumnLeftRowIndexMap = new ArrayHashMap<>();

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                row = row == null ? Objectory.createObjectArray(rightJoinColumnIndexes.length) : row;

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(row);

                fullJoin(newColumnList, right, newColumnType, newColumnIndex, leftRowIndex, rightRowIndexList);

                if (!joinColumnLeftRowIndexMap.containsKey(row)) {
                    joinColumnLeftRowIndexMap.put(row, leftRowIndex);
                    row = null;
                }
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (Map.Entry<Object[], List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexMap.containsKey(rightRowIndexEntry.getKey())) {
                    fullJoin(newColumnList, right, newColumnType, newColumnIndex, rightRowIndexEntry.getValue());
                }
            }

            for (Object[] a : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            for (Object[] a : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param newColumnType
     * @param newColumnIndex
     * @param rightRowIndexList
     */
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType, final int newColumnIndex,
            List<Integer> rightRowIndexList) {
        for (int rightRowIndex : rightRowIndexList) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            newColumnList.get(newColumnIndex).add(right.getRow(rightRowIndex, newColumnType));
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param newColumnType
     * @param newColumnIndex
     * @param leftRowIndex
     * @param rightRowIndexList
     */
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType, final int newColumnIndex, int leftRowIndex,
            List<Integer> rightRowIndexList) {
        if (N.notEmpty(rightRowIndexList)) {
            for (int rightRowIndex : rightRowIndexList) {
                for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                    newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
                }

                newColumnList.get(newColumnIndex).add(right.getRow(rightRowIndex, newColumnType));
            }
        } else {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            newColumnList.get(newColumnIndex).add(null);
        }
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnType
     * @param collSupplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public DataSet fullJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);
        N.checkArgNotNull(collSupplier);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final Set<Object> joinColumnLeftRowIndexSet = N.newHashSet();
            List<Integer> rightRowIndexList = null;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                fullJoin(newColumnList, right, newColumnType, collSupplier, newColumnIndex, leftRowIndex, rightRowIndexList);

                joinColumnLeftRowIndexSet.add(hashKey);
            }

            for (Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexSet.contains(rightRowIndexEntry.getKey())) {
                    fullJoin(newColumnList, right, newColumnType, collSupplier, newColumnIndex, rightRowIndexEntry.getValue());
                }
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);
            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            final Map<Object[], List<Integer>> joinColumnRightRowIndexMap = new ArrayHashMap<>(LinkedHashMap.class);
            List<Integer> rightRowIndexList = null;
            Object[] row = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                row = row == null ? Objectory.createObjectArray(rightJoinColumnIndexes.length) : row;

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final Map<Object[], Integer> joinColumnLeftRowIndexMap = new ArrayHashMap<>();

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                row = row == null ? Objectory.createObjectArray(rightJoinColumnIndexes.length) : row;

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(row);

                fullJoin(newColumnList, right, newColumnType, collSupplier, newColumnIndex, leftRowIndex, rightRowIndexList);

                if (!joinColumnLeftRowIndexMap.containsKey(row)) {
                    joinColumnLeftRowIndexMap.put(row, leftRowIndex);
                    row = null;
                }
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (Map.Entry<Object[], List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexMap.containsKey(rightRowIndexEntry.getKey())) {
                    fullJoin(newColumnList, right, newColumnType, collSupplier, newColumnIndex, rightRowIndexEntry.getValue());
                }
            }

            for (Object[] a : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            for (Object[] a : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(a);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param newColumnType
     * @param collSupplier
     * @param newColumnIndex
     * @param rightRowIndexList
     */
    @SuppressWarnings("rawtypes")
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final List<Integer> rightRowIndexList) {
        for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
            newColumnList.get(i).add(null);
        }

        final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

        for (int rightRowIndex : rightRowIndexList) {
            coll.add(right.getRow(rightRowIndex, newColumnType));
        }

        newColumnList.get(newColumnIndex).add(coll);
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param newColumnType
     * @param collSupplier
     * @param newColumnIndex
     * @param leftRowIndex
     * @param rightRowIndexList
     */
    @SuppressWarnings("rawtypes")
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, int leftRowIndex, List<Integer> rightRowIndexList) {
        if (N.notEmpty(rightRowIndexList)) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

            for (int rightRowIndex : rightRowIndexList) {
                coll.add(right.getRow(rightRowIndex, newColumnType));
            }

            newColumnList.get(newColumnIndex).add(coll);
        } else {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            newColumnList.get(newColumnIndex).add(null);
        }
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    public DataSet union(final DataSet other) {
        return union(other, false);
    }

    /**
     *
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet union(final DataSet other, boolean requiresSameColumns) {
        return union(other, getKeyColumnNames(other), requiresSameColumns);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    @Override
    public DataSet union(final DataSet other, final Collection<String> keyColumnNames) {
        return union(other, keyColumnNames, false);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet union(final DataSet other, final Collection<String> keyColumnNames, boolean requiresSameColumns) {
        checkColumnNames(other, keyColumnNames, requiresSameColumns);

        final Set<String> newColumnNameSet = new LinkedHashSet<>();
        newColumnNameSet.addAll(this._columnNameList);
        newColumnNameSet.addAll(other.columnNameList());

        final List<String> newColumnNameList = new ArrayList<>(newColumnNameSet);
        final int newColumnCount = newColumnNameList.size();
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        final int totalSize = this.size() + other.size();

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>(totalSize));
        }

        final DataSet result = new RowDataSet(newColumnNameList, newColumnList);

        if (totalSize == 0) {
            return result;
        }

        final int thisColumnCount = this.columnCount();
        final int otherColumnCount = other.columnCount();
        final int keyColumnCount = keyColumnNames.size();

        if (keyColumnCount == 1) {
            final String keyColumnName = N.firstOrNullIfEmpty(keyColumnNames);
            final Set<Object> addedRowKeySet = N.newHashSet();

            if (this.size() > 0) {
                final int keyColumnIndex = this.getColumnIndex(keyColumnName);
                final List<Object> keyColumn = _columnList.get(keyColumnIndex);

                for (int rowIndex = 0, rowCount = this.size(); rowIndex < rowCount; rowIndex++) {
                    if (addedRowKeySet.add(hashKey(keyColumn.get(rowIndex)))) {
                        for (int i = 0; i < thisColumnCount; i++) {
                            newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                        }
                    }
                }

                if (newColumnCount > thisColumnCount && newColumnList.get(0).size() > 0) {
                    final List<Object> column = N.repeat(null, newColumnList.get(0).size());

                    for (int i = thisColumnCount; i < newColumnCount; i++) {
                        newColumnList.get(i).addAll(column);
                    }
                }
            }

            if (other.size() > 0) {
                final int[] otherNewColumnIndexes = result.getColumnIndexes(other.columnNameList());
                final List<Object>[] columnsInOther = new List[otherColumnCount];

                for (int i = 0; i < otherColumnCount; i++) {
                    columnsInOther[i] = other.getColumn(i);
                }

                final int keyColumnIndexInOther = other.getColumnIndex(keyColumnName);
                final List<Object> keyColumnInOther = other.getColumn(keyColumnIndexInOther);
                int cnt = 0;

                for (int rowIndex = 0, rowCount = other.size(); rowIndex < rowCount; rowIndex++) {
                    if (addedRowKeySet.add(hashKey(keyColumnInOther.get(rowIndex)))) {
                        for (int i = 0; i < otherColumnCount; i++) {
                            newColumnList.get(otherNewColumnIndexes[i]).add(columnsInOther[i].get(rowIndex));
                        }

                        cnt++;
                    }
                }

                if (newColumnCount > otherColumnCount && cnt > 0) {
                    final List<Object> column = N.repeat(null, cnt);

                    for (int i = 0; i < thisColumnCount; i++) {
                        if (!other.containsColumn(this._columnNameList.get(i))) {
                            newColumnList.get(i).addAll(column);
                        }
                    }
                }
            }
        } else {
            final Set<Object[]> addedRowKeySet = new ArrayHashSet<>();
            Object[] row = null;

            if (this.size() > 0) {
                final int[] keyColumnIndexes = this.getColumnIndexes(keyColumnNames);
                final List<Object>[] keyColumns = new List[keyColumnCount];

                for (int i = 0; i < keyColumnCount; i++) {
                    keyColumns[i] = _columnList.get(keyColumnIndexes[i]);
                }

                for (int rowIndex = 0, rowCount = this.size(); rowIndex < rowCount; rowIndex++) {
                    row = row == null ? Objectory.createObjectArray(keyColumnCount) : row;

                    for (int i = 0; i < keyColumnCount; i++) {
                        row[i] = keyColumns[i].get(rowIndex);
                    }

                    if (addedRowKeySet.add(row)) {
                        for (int i = 0; i < thisColumnCount; i++) {
                            newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                        }

                        row = null;
                    }
                }

                if (row != null) {
                    Objectory.recycle(row);
                    row = null;
                }

                if (newColumnCount > thisColumnCount && newColumnList.get(0).size() > 0) {
                    final List<Object> column = N.repeat(null, newColumnList.get(0).size());

                    for (int i = thisColumnCount; i < newColumnCount; i++) {
                        newColumnList.get(i).addAll(column);
                    }
                }
            }

            if (other.size() > 0) {
                final int[] otherNewColumnIndexes = result.getColumnIndexes(other.columnNameList());
                final List<Object>[] columnsInOther = new List[otherColumnCount];

                for (int i = 0; i < otherColumnCount; i++) {
                    columnsInOther[i] = other.getColumn(i);
                }

                final int[] keyColumnIndexesInOther = other.getColumnIndexes(keyColumnNames);
                final List<Object>[] keyColumnsInOther = new List[keyColumnCount];

                for (int i = 0; i < keyColumnCount; i++) {
                    keyColumnsInOther[i] = other.getColumn(keyColumnIndexesInOther[i]);
                }

                int cnt = 0;

                for (int rowIndex = 0, rowCount = other.size(); rowIndex < rowCount; rowIndex++) {
                    row = row == null ? Objectory.createObjectArray(keyColumnCount) : row;

                    for (int i = 0; i < keyColumnCount; i++) {
                        row[i] = keyColumnsInOther[i].get(rowIndex);
                    }

                    if (addedRowKeySet.add(row)) {
                        for (int i = 0; i < otherColumnCount; i++) {
                            newColumnList.get(otherNewColumnIndexes[i]).add(columnsInOther[i].get(rowIndex));
                        }

                        cnt++;
                        row = null;
                    }
                }

                if (row != null) {
                    Objectory.recycle(row);
                    row = null;
                }

                if (newColumnCount > otherColumnCount && cnt > 0) {
                    final List<Object> column = N.repeat(null, cnt);

                    for (int i = 0; i < thisColumnCount; i++) {
                        if (!other.containsColumn(this._columnNameList.get(i))) {
                            newColumnList.get(i).addAll(column);
                        }
                    }
                }
            }

            for (Object[] a : addedRowKeySet) {
                Objectory.recycle(a);
            }
        }

        return result;
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    public DataSet unionAll(final DataSet other) {
        return unionAll(other, false);
    }

    /**
     *
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet unionAll(final DataSet other, boolean requiresSameColumns) {
        return merge(other, requiresSameColumns);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    @Override
    public DataSet unionAll(DataSet other, Collection<String> keyColumnNames) {
        return unionAll(other);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet unionAll(DataSet other, Collection<String> keyColumnNames, boolean requiresSameColumns) {
        return unionAll(other, requiresSameColumns);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    public DataSet intersect(final DataSet other) {
        return intersect(other, false);
    }

    /**
     *
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet intersect(final DataSet other, boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, true, true);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    @Override
    public DataSet intersect(final DataSet other, final Collection<String> keyColumnNames) {
        return intersect(other, keyColumnNames, false);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet intersect(final DataSet other, final Collection<String> keyColumnNames, boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, true, true);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    public DataSet intersectAll(final DataSet other) {
        return intersectAll(other, false);
    }

    /**
     *
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet intersectAll(final DataSet other, boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, true, false);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    @Override
    public DataSet intersectAll(final DataSet other, final Collection<String> keyColumnNames) {
        return intersectAll(other, keyColumnNames, false);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet intersectAll(final DataSet other, final Collection<String> keyColumnNames, boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, true, false);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    public DataSet except(final DataSet other) {
        return except(other, false);
    }

    /**
     *
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet except(final DataSet other, boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, false, true);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    @Override
    public DataSet except(final DataSet other, final Collection<String> keyColumnNames) {
        return except(other, keyColumnNames, false);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet except(final DataSet other, final Collection<String> keyColumnNames, boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, false, true);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    public DataSet exceptAll(final DataSet other) {
        return exceptAll(other, false);
    }

    /**
     *
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet exceptAll(final DataSet other, boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, false, false);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    @Override
    public DataSet exceptAll(final DataSet other, final Collection<String> keyColumnNames) {
        return exceptAll(other, keyColumnNames, false);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet exceptAll(final DataSet other, final Collection<String> keyColumnNames, boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, false, false);
    }

    private DataSet removeAll(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns, final boolean retain,
            final boolean deduplicate) {
        checkColumnNames(other, keyColumnNames, requiresSameColumns);

        final int newColumnCount = this.columnCount();
        final List<String> newColumnNameList = new ArrayList<>(this._columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final int size = size();

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final int keyColumnCount = keyColumnNames.size();

        if (keyColumnCount == 1) {
            final String keyColumnName = N.firstOrNullIfEmpty(keyColumnNames);
            final int keyColumnIndex = this.getColumnIndex(keyColumnName);
            final int keyColumnIndexInOther = other.getColumnIndex(keyColumnName);

            final List<Object> keyColumnInOther = other.getColumn(keyColumnIndexInOther);
            final Set<Object> rowKeySet = N.newHashSet();

            for (Object e : keyColumnInOther) {
                rowKeySet.add(hashKey(e));
            }

            final List<Object> keyColumn = _columnList.get(keyColumnIndex);
            final Set<Object> addedRowKeySet = deduplicate ? N.newHashSet() : N.emptySet();
            Object hashKey = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                hashKey = hashKey(keyColumn.get(rowIndex));

                if (rowKeySet.contains(hashKey) == retain && (deduplicate == false || addedRowKeySet.add(hashKey))) {
                    for (int i = 0; i < newColumnCount; i++) {
                        newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                    }
                }
            }
        } else {
            final int[] keyColumnIndexes = this.getColumnIndexes(keyColumnNames);
            final int[] keyColumnIndexesInOther = other.getColumnIndexes(keyColumnNames);

            final List<Object>[] keyColumnsInOther = new List[keyColumnCount];

            for (int i = 0; i < keyColumnCount; i++) {
                keyColumnsInOther[i] = other.getColumn(keyColumnIndexesInOther[i]);
            }

            final Set<Object[]> rowKeySet = new ArrayHashSet<>();
            Object[] row = null;

            for (int rowIndex = 0, otherSize = other.size(); rowIndex < otherSize; rowIndex++) {
                row = row == null ? Objectory.createObjectArray(keyColumnCount) : row;

                for (int i = 0; i < keyColumnCount; i++) {
                    row[i] = keyColumnsInOther[i].get(rowIndex);
                }

                if (rowKeySet.add(row)) {
                    row = null;
                }
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final List<Object>[] keyColumns = new List[keyColumnCount];

            for (int i = 0; i < keyColumnCount; i++) {
                keyColumns[i] = _columnList.get(keyColumnIndexes[i]);
            }

            final Set<Object[]> addedRowKeySet = deduplicate ? new ArrayHashSet<>() : N.emptySet();

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                row = row == null ? Objectory.createObjectArray(keyColumnCount) : row;

                for (int i = 0; i < keyColumnCount; i++) {
                    row[i] = keyColumns[i].get(rowIndex);
                }

                if (rowKeySet.contains(row) == retain && (deduplicate == false || addedRowKeySet.add(row))) {
                    for (int i = 0; i < newColumnCount; i++) {
                        newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                    }

                    if (deduplicate) {
                        row = null;
                    }
                }
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (Object[] a : rowKeySet) {
                Objectory.recycle(a);
            }

            if (deduplicate) {
                for (Object[] a : addedRowKeySet) {
                    Objectory.recycle(a);
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    public DataSet intersection(final DataSet other) {
        return intersection(other, false);
    }

    /**
     *
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet intersection(final DataSet other, boolean requiresSameColumns) {
        return removeOccurrences(other, getKeyColumnNames(other), requiresSameColumns, true);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    @Override
    public DataSet intersection(final DataSet other, final Collection<String> keyColumnNames) {
        return intersection(other, keyColumnNames, false);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet intersection(final DataSet other, final Collection<String> keyColumnNames, boolean requiresSameColumns) {
        return removeOccurrences(other, keyColumnNames, requiresSameColumns, true);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    public DataSet difference(final DataSet other) {
        return difference(other, false);
    }

    /**
     *
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet difference(final DataSet other, boolean requiresSameColumns) {
        return removeOccurrences(other, getKeyColumnNames(other), requiresSameColumns, false);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    @Override
    public DataSet difference(final DataSet other, final Collection<String> keyColumnNames) {
        return difference(other, keyColumnNames, false);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet difference(final DataSet other, final Collection<String> keyColumnNames, boolean requiresSameColumns) {
        return removeOccurrences(other, keyColumnNames, requiresSameColumns, false);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    public DataSet symmetricDifference(final DataSet other) {
        return symmetricDifference(other, false);
    }

    /**
     *
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet symmetricDifference(final DataSet other, boolean requiresSameColumns) {
        final Collection<String> keyColumnNames = getKeyColumnNames(other);

        return this.difference(other, keyColumnNames, requiresSameColumns).merge(other.difference(this, keyColumnNames, requiresSameColumns));
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @return
     */
    @Override
    public DataSet symmetricDifference(final DataSet other, final Collection<String> keyColumnNames) {
        return symmetricDifference(other, keyColumnNames, false);
    }

    /**
     *
     *
     * @param other
     * @param keyColumnNames
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet symmetricDifference(final DataSet other, final Collection<String> keyColumnNames, boolean requiresSameColumns) {
        return this.difference(other, keyColumnNames, requiresSameColumns).merge(other.difference(this, keyColumnNames, requiresSameColumns));
    }

    private DataSet removeOccurrences(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns, final boolean retain) {
        checkColumnNames(other, keyColumnNames, requiresSameColumns);

        final int keyColumnCount = this.columnCount();
        final List<String> newColumnNameList = new ArrayList<>(this._columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(keyColumnCount);

        for (int i = 0; i < keyColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final int size = size();

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final int commonColumnCount = keyColumnNames.size();

        if (commonColumnCount == 1) {
            final String keyColumnName = N.firstOrNullIfEmpty(keyColumnNames);
            final int keyColumnIndex = this.getColumnIndex(keyColumnName);
            final int keyColumnIndexInOther = other.getColumnIndex(keyColumnName);

            final List<Object> keyColumnInOther = other.getColumn(keyColumnIndexInOther);
            final Multiset<Object> rowKeySet = new Multiset<>();

            for (Object val : keyColumnInOther) {
                rowKeySet.add(hashKey(val));
            }

            final List<Object> keyColumn = _columnList.get(keyColumnIndex);

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                if ((rowKeySet.getAndRemove(hashKey(keyColumn.get(rowIndex))) > 0) == retain) {
                    for (int i = 0; i < keyColumnCount; i++) {
                        newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                    }
                }
            }
        } else {
            final int[] keyColumnIndexes = this.getColumnIndexes(keyColumnNames);
            final int[] keyColumnIndexesInOther = other.getColumnIndexes(keyColumnNames);

            final List<Object>[] keyColumnsInOther = new List[commonColumnCount];

            for (int i = 0; i < commonColumnCount; i++) {
                keyColumnsInOther[i] = other.getColumn(keyColumnIndexesInOther[i]);
            }

            final Multiset<Object[]> rowKeySet = new Multiset<>(ArrayHashMap.class);
            Object[] row = null;

            for (int rowIndex = 0, otherSize = other.size(); rowIndex < otherSize; rowIndex++) {
                row = row == null ? Objectory.createObjectArray(commonColumnCount) : row;

                for (int i = 0; i < commonColumnCount; i++) {
                    row[i] = keyColumnsInOther[i].get(rowIndex);
                }

                if (rowKeySet.getAndAdd(row) == 0) {
                    row = null;
                }
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final List<Object>[] keyColumns = new List[commonColumnCount];

            for (int i = 0; i < commonColumnCount; i++) {
                keyColumns[i] = _columnList.get(keyColumnIndexes[i]);
            }

            row = Objectory.createObjectArray(commonColumnCount);

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                for (int i = 0; i < commonColumnCount; i++) {
                    row[i] = keyColumns[i].get(rowIndex);
                }

                if ((rowKeySet.getAndRemove(row) > 0) == retain) {
                    for (int i = 0; i < keyColumnCount; i++) {
                        newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                    }
                }
            }

            Objectory.recycle(row);
            row = null;

            for (Object[] a : rowKeySet) {
                Objectory.recycle(a);
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    public DataSet merge(final DataSet other) {
        return merge(other, false);
    }

    /**
     *
     *
     * @param other
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet merge(final DataSet other, boolean requiresSameColumns) {
        checkIfColumnNamesAreSame(other, requiresSameColumns);

        return merge(other, 0, other.size(), other.columnNameList());
    }

    /**
     *
     * @param other
     * @param columnNames
     * @return
     */
    @Override
    public DataSet merge(final DataSet other, final Collection<String> columnNames) {
        return merge(other, 0, other.size(), columnNames);
    }

    /**
     *
     * @param other
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public DataSet merge(final DataSet other, final int fromRowIndex, final int toRowIndex) {
        return merge(other, fromRowIndex, toRowIndex, other.columnNameList());
    }

    /**
     *
     * @param other
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @return
     */
    @Override
    public DataSet merge(final DataSet other, final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        checkRowIndex(fromRowIndex, toRowIndex, other.size());

        final RowDataSet result = (RowDataSet) copy();

        merge(result, other, fromRowIndex, toRowIndex, columnNames);

        return result;
    }

    private void merge(final RowDataSet result, final DataSet other, final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        List<Object> column = null;

        for (String columnName : columnNames) {
            if (!result.containsColumn(columnName)) {
                if (column == null) {
                    column = new ArrayList<>(size() + (toRowIndex - fromRowIndex));
                    N.fill(column, 0, size(), null);
                }

                result.addColumn(columnName, column);
            }
        }

        List<Object> nulls = null;

        for (String columnName : result._columnNameList) {
            column = result._columnList.get(result.getColumnIndex(columnName));

            int columnIndex = other.getColumnIndex(columnName);

            if (columnIndex >= 0) {
                if (fromRowIndex == 0 && toRowIndex == other.size()) {
                    column.addAll(other.getColumn(columnIndex));
                } else {
                    column.addAll(other.getColumn(columnIndex).subList(fromRowIndex, toRowIndex));
                }
            } else {
                if (nulls == null) {
                    nulls = new ArrayList<>(toRowIndex - fromRowIndex);
                    N.fill(nulls, 0, toRowIndex - fromRowIndex, null);
                }

                column.addAll(nulls);
            }
        }

        if (N.notEmpty(other.properties())) {
            result.properties().putAll(other.properties());
        }
    }

    //    /**
    //     *
    //     * @param a
    //     * @param b
    //     * @return
    //     */
    //    @Override
    //    public DataSet merge(final DataSet a, final DataSet b) {
    //        return N.merge(this, a, b);
    //    }

    /**
    *
    *
    * @param others
    * @return
    */
    @Override
    public DataSet merge(final Collection<? extends DataSet> others) {
        return merge(others, false);
    }

    /**
     *
     *
     * @param others
     * @param requiresSameColumns
     * @return
     */
    @Override
    public DataSet merge(final Collection<? extends DataSet> others, final boolean requiresSameColumns) {
        if (N.isEmpty(others)) {
            return this.copy();
        }

        final List<DataSet> dsList = new ArrayList<>(N.size(others) + 1);
        dsList.add(this);
        dsList.addAll(others);

        return N.merge(dsList, requiresSameColumns);
    }

    private List<String> getKeyColumnNames(final DataSet other) {
        final List<String> commonColumnNameList = new ArrayList<>(this._columnNameList);
        commonColumnNameList.retainAll(other.columnNameList());

        if (N.isEmpty(commonColumnNameList)) {
            throw new IllegalArgumentException("These two DataSets don't have common column names: " + this._columnNameList + ", " + other.columnNameList());
        }
        return commonColumnNameList;
    }

    private void checkIfColumnNamesAreSame(final DataSet other, boolean requiresSameColumns) {
        if (requiresSameColumns) {
            if (!(this.columnCount() == other.columnCount() && this._columnNameList.containsAll(other.columnNameList())
                    && other.columnNameList().containsAll(this._columnNameList))) {
                throw new IllegalArgumentException("These two DataSets don't have same column names: " + this._columnNameList + ", " + other.columnNameList());
            }
        }
    }

    private void checkColumnNames(final DataSet other, final Collection<String> keyColumnNames, boolean requiresSameColumns) {
        N.checkArgNotEmpty(keyColumnNames, "keyColumnNames");

        N.checkArgument(this.containsAllColumns(keyColumnNames), "This DataSet={} doesn't contain all keyColumnNames={}", this.columnNameList(),
                keyColumnNames);

        N.checkArgument(other.containsAllColumns(keyColumnNames), "Other DataSet={} doesn't contain all keyColumnNames={}", other.columnNameList(),
                keyColumnNames);

        checkIfColumnNamesAreSame(other, requiresSameColumns);
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    public DataSet cartesianProduct(final DataSet other) {
        final Collection<String> tmp = N.intersection(this._columnNameList, other.columnNameList());
        if (N.notEmpty(tmp)) {
            throw new IllegalArgumentException(tmp + " are included in both DataSets: " + this._columnNameList + " : " + other.columnNameList());
        }

        final int aSize = this.size();
        final int bSize = other.size();
        final int aColumnCount = this.columnCount();
        final int bColumnCount = other.columnCount();

        final int newColumnCount = aColumnCount + bColumnCount;
        final int newRowCount = aSize * bSize;

        final List<String> newColumnNameList = new ArrayList<>(newColumnCount);
        newColumnNameList.addAll(_columnNameList);
        newColumnNameList.addAll(other.columnNameList());

        final List<List<Object>> newColumnList = new ArrayList<>();

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>(newRowCount));
        }

        if (newRowCount == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final Object[] tmpArray = new Object[bSize];

        for (int rowIndex = 0; rowIndex < aSize; rowIndex++) {
            for (int columnIndex = 0; columnIndex < aColumnCount; columnIndex++) {
                N.fill(tmpArray, this.get(rowIndex, columnIndex));
                newColumnList.get(columnIndex).addAll(Arrays.asList(tmpArray));
            }

            for (int columnIndex = 0; columnIndex < bColumnCount; columnIndex++) {
                newColumnList.get(columnIndex + aColumnCount).addAll(other.getColumn(columnIndex));
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param chunkSize
     * @return
     */
    @Override
    public Stream<DataSet> split(final int chunkSize) {
        return split(chunkSize, _columnNameList);
    }

    /**
     *
     * @param chunkSize
     * @param columnNames
     * @return
     */
    @Override
    public Stream<DataSet> split(final int chunkSize, final Collection<String> columnNames) {
        final int[] columnIndexes = checkColumnName(columnNames);
        N.checkArgPositive(chunkSize, "chunkSize");

        final int expectedModCount = modCount;
        final int totalSize = this.size();

        return IntStream.range(0, totalSize, chunkSize).mapToObj(from -> {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }

            return RowDataSet.this.copy(from, from <= totalSize - chunkSize ? from + chunkSize : totalSize, columnNames, columnIndexes, true);
        });
    }

    /**
     *
     * @param chunkSize
     * @return
     */
    @Override
    public List<DataSet> splitToList(final int chunkSize) {
        return splitToList(chunkSize, _columnNameList);
    }

    /**
     *
     * @param chunkSize
     * @param columnNames
     * @return
     */
    @Override
    public List<DataSet> splitToList(final int chunkSize, final Collection<String> columnNames) {
        final int[] columnIndexes = checkColumnName(columnNames);
        N.checkArgPositive(chunkSize, "chunkSize");

        final List<DataSet> res = new ArrayList<>();

        for (int i = 0, totalSize = size(); i < totalSize; i = i <= totalSize - chunkSize ? i + chunkSize : totalSize) {
            res.add(copy(i, i <= totalSize - chunkSize ? i + chunkSize : totalSize, columnNames, columnIndexes, true));
        }

        return res;
    }

    //    /**
    //     *
    //     * @param chunkSize
    //     * @return
    //     * @deprecated replaced by {@link #splitToList(int)}
    //     */
    //    @Deprecated
    //    @Override
    //    public List<DataSet> splitt(int chunkSize) {
    //        return splitToList(chunkSize);
    //    }

    //    /**
    //     *
    //     * @param columnNames
    //     * @param chunkSize
    //     * @return
    //     * @deprecated replaced by {@link #splitToList(Collection, int)}
    //     */
    //    @Deprecated
    //    @Override
    //    public List<DataSet> splitt(Collection<String> columnNames, int chunkSize) {
    //        return splitToList(columnNames, chunkSize);
    //    }

    //    @Override
    //    public <T> List<List<T>> split(final int size, final Class<? extends T> rowType) {
    //        return split(_columnNameList, size, rowType);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(final Collection<String> columnNames, final int size, final Class<? extends T> rowType) {
    //        return split(columnNames, 0, size(), size, rowType);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(final int fromRowIndex, final int toRowIndex, final int size, final Class<? extends T> rowType) {
    //        return split(_columnNameList, fromRowIndex, toRowIndex, size, rowType);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex,
    //            final int size, final Class<? extends T> rowType) {
    //        checkRowIndex(fromRowIndex, toRowIndex);
    //
    //        final List<T> list = this.toList(columnNames, fromRowIndex, toRowIndex, rowType);
    //
    //        return N.split(list, size);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(int size, IntFunction<? extends T> rowSupplier) {
    //        return split(_columnNameList, size, rowSupplier);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(Collection<String> columnNames, int size, IntFunction<? extends T> rowSupplier) {
    //        return split(columnNames, 0, size(), size, rowSupplier);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(int fromRowIndex, int toRowIndex, int size, IntFunction<? extends T> rowSupplier) {
    //        return split(_columnNameList, fromRowIndex, toRowIndex, size, rowSupplier);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(Collection<String> columnNames, int fromRowIndex, int toRowIndex, int size, IntFunction<? extends T> rowSupplier) {
    //        checkRowIndex(fromRowIndex, toRowIndex);
    //
    //        final List<T> list = this.toList(columnNames, fromRowIndex, toRowIndex, rowSupplier);
    //
    //        return N.split(list, size);
    //    }

    /**
     *
     * @param columnNames
     * @return
     */
    @Override
    public DataSet slice(Collection<String> columnNames) {
        return slice(0, size(), columnNames);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public DataSet slice(int fromRowIndex, int toRowIndex) {
        return slice(fromRowIndex, toRowIndex, _columnNameList);
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @return
     */
    @Override
    public DataSet slice(int fromRowIndex, int toRowIndex, Collection<String> columnNames) {
        N.checkFromToIndex(fromRowIndex, toRowIndex, size());
        DataSet ds = null;

        if (N.isEmpty(columnNames)) {
            ds = N.newEmptyDataSet();
        } else {
            final int[] columnIndexes = checkColumnName(columnNames);
            final List<String> newColumnNames = new ArrayList<>(columnNames);
            final List<List<Object>> newColumnList = new ArrayList<>(newColumnNames.size());

            if (fromRowIndex > 0 || toRowIndex < size()) {
                for (int columnIndex : columnIndexes) {
                    newColumnList.add(_columnList.get(columnIndex).subList(fromRowIndex, toRowIndex));
                }
            } else {
                for (int columnIndex : columnIndexes) {
                    newColumnList.add(_columnList.get(columnIndex));
                }
            }

            ds = new RowDataSet(newColumnNames, newColumnList, _properties);
        }

        ds.freeze();

        return ds;
    }

    /**
     *
     * @param pageSize
     * @return
     */
    @Override
    public Paginated<DataSet> paginate(final int pageSize) {
        return paginate(_columnNameList, pageSize);
    }

    /**
     *
     *
     * @param columnNames
     * @param pageSize
     * @return
     */
    @Override
    public Paginated<DataSet> paginate(final Collection<String> columnNames, final int pageSize) {
        return new PaginatedDataSet(N.isEmpty(columnNames) ? _columnNameList : columnNames, pageSize);
    }

    //    @SuppressWarnings("rawtypes")
    //    @Override
    //    public <T extends Comparable<? super T>> Map<String, T> percentiles(final String columnName) {
    //        if (size() == 0) {
    //            throw new RuntimeException("The size of dataset is 0");
    //        }
    //
    //        final Object[] columns = getColumn(columnName).toArray();
    //
    //        N.sort(columns);
    //
    //        return (Map) N.percentiles(columns);
    //    }
    //
    //    @Override
    //    public <T> Map<String, T> percentiles(final String columnName, final Comparator<? super T> comparator) {
    //        if (size() == 0) {
    //            throw new RuntimeException("The size of dataset is 0");
    //        }
    //
    //        final T[] columns = (T[]) getColumn(columnName).toArray();
    //
    //        N.sort(columns, comparator);
    //
    //        return N.percentiles(columns);
    //    }

    /**
     *
     * @param <T>
     * @param columnName
     * @return
     */
    @Override
    public <T> Stream<T> stream(String columnName) {
        return stream(0, size(), columnName);
    }

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnName
     * @return
     */
    @Override
    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, final String columnName) {
        this.checkRowIndex(fromRowIndex, toRowIndex);

        return (Stream<T>) Stream.of(_columnList.get(checkColumnName(columnName)), fromRowIndex, toRowIndex);
    }

    //    @SuppressWarnings("rawtypes")
    //    @Override
    //    public <T extends Comparable<? super T>> Map<String, T> percentiles(final String columnName) {
    //        if (size() == 0) {
    //            throw new RuntimeException("The size of dataset is 0");
    //        }
    //
    //        final Object[] columns = getColumn(columnName).toArray();
    //
    //        N.sort(columns);
    //
    //        return (Map) N.percentiles(columns);
    //    }
    //
    //    @Override
    //    public <T> Map<String, T> percentiles(final String columnName, final Comparator<? super T> comparator) {
    //        if (size() == 0) {
    //            throw new RuntimeException("The size of dataset is 0");
    //        }
    //
    //        final T[] columns = (T[]) getColumn(columnName).toArray();
    //
    //        N.sort(columns, comparator);
    //
    //        return N.percentiles(columns);
    //    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param rowMapper
    //     * @return
    //     */
    //    @Override
    //    public <T> Stream<T> stream(final Function<? super DisposableObjArray, ? extends T> rowMapper) {
    //        return stream(0, size(), rowMapper);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param fromRowIndex
    //     * @param toRowIndex
    //     * @param rowMapper
    //     * @return
    //     */
    //    @Override
    //    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, final Function<? super DisposableObjArray, ? extends T> rowMapper) {
    //        return stream(_columnNameList, fromRowIndex, toRowIndex, rowMapper);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param rowMapper
    //     * @return
    //     */
    //    @Override
    //    public <T> Stream<T> stream(Collection<String> columnNames, final Function<? super DisposableObjArray, ? extends T> rowMapper) {
    //        return stream(columnNames, 0, size(), rowMapper);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param columnNames
    //     * @param fromRowIndex
    //     * @param toRowIndex
    //     * @param rowMapper
    //     * @return
    //     */
    //    @Override
    //    public <T> Stream<T> stream(final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex,
    //            final Function<? super DisposableObjArray, ? extends T> rowMapper) {
    //        final int[] columnIndexes = this.checkColumnName(columnNames);
    //        checkRowIndex(fromRowIndex, toRowIndex);
    //        N.checkArgNotNull(rowMapper, "rowMapper");
    //        final int columnCount = columnNames.size();
    //
    //        return Stream.of(new ObjIteratorEx<? super DisposableObjArray>() {
    //            private final Type<Object[]> rowType = N.typeOf(Object[].class);
    //            private Object[] row = new Object[columnCount];
    //            private DisposableObjArray disposableRow = DisposableObjArray.wrap(row);
    //            private final int expectedModCount = modCount;
    //            private int cursor = fromRowIndex;
    //
    //            @Override
    //            public boolean hasNext() {
    //                ConcurrentModification();
    //
    //                return cursor < toRowIndex;
    //            }
    //
    //            @Override
    //            public DisposableObjArray next() {
    //                ConcurrentModification();
    //
    //                if (cursor >= toRowIndex) {
    //                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                getRow(rowType, null, row, columnIndexes, columnNames, cursor);
    //
    //                cursor++;
    //
    //                return disposableRow;
    //            }
    //
    //            @Override
    //            public long count() {
    //                ConcurrentModification();
    //
    //                return toRowIndex - cursor;
    //            }
    //
    //            @Override
    //            public void advance(long n) {
    //                N.checkArgNotNegative(n, "n");
    //
    //                ConcurrentModification();
    //
    //                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
    //            }
    //
    //            @Override
    //            public <A> A[] toArray(A[] a) {
    //                ConcurrentModification();
    //
    //                final List<Object[]> rows = RowDataSet.this.toList(Object[].class, columnNames, cursor, toRowIndex);
    //
    //                a = a.length >= rows.size() ? a : (A[]) N.newArray(a.getClass().getComponentType(), rows.size());
    //
    //                rows.toArray(a);
    //
    //                return a;
    //            }
    //
    //            final void ConcurrentModification() {
    //                if (modCount != expectedModCount) {
    //                    throw new ConcurrentModificationException();
    //                }
    //            }
    //        }).map(rowMapper);
    //    }

    /**
     *
     * @param <T>
     * @param rowType
     * @return
     */
    @Override
    public <T> Stream<T> stream(Class<? extends T> rowType) {
        return stream(0, size(), rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Class<? extends T> rowType) {
        return stream(fromRowIndex, toRowIndex, _columnNameList, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param columnNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> Stream<T> stream(Collection<String> columnNames, Class<? extends T> rowType) {
        return stream(0, size(), columnNames, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Class<? extends T> rowType) {
        return stream(fromRowIndex, toRowIndex, columnNames, null, rowType, null);
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> Stream<T> stream(IntFunction<? extends T> rowSupplier) {
        return stream(0, size(), rowSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, IntFunction<? extends T> rowSupplier) {
        return stream(fromRowIndex, toRowIndex, _columnNameList, rowSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> Stream<T> stream(Collection<String> columnNames, IntFunction<? extends T> rowSupplier) {
        return stream(0, size(), columnNames, rowSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final IntFunction<? extends T> rowSupplier) {
        return stream(fromRowIndex, toRowIndex, columnNames, null, null, rowSupplier);
    }

    /**
     * 
     *
     * @param <T> 
     * @param prefixAndFieldNameMap 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> Stream<T> stream(Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType) {
        return stream(0, size(), this._columnNameList, prefixAndFieldNameMap, rowType);
    }

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
    @Override
    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType) {
        return stream(fromRowIndex, toRowIndex, this._columnNameList, prefixAndFieldNameMap, rowType);
    }

    /**
     * 
     *
     * @param <T> 
     * @param columnNames 
     * @param prefixAndFieldNameMap 
     * @param rowType 
     * @return 
     */
    @Override
    public <T> Stream<T> stream(Collection<String> columnNames, Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowType) {
        return stream(0, size(), columnNames, prefixAndFieldNameMap, rowType);
    }

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
    @Override
    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> columnNames, final Map<String, String> prefixAndFieldNameMap,
            Class<? extends T> rowType) {
        N.checkArgument(ClassUtil.isBeanClass(rowType), "{} is not a bean class", rowType);

        return stream(fromRowIndex, toRowIndex, columnNames, prefixAndFieldNameMap, rowType, null);
    }

    private <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> inputColumnNames,
            final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> inputRowClass, final IntFunction<? extends T> inputRowSupplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final Collection<String> columnNames = N.isEmpty(inputColumnNames) ? this._columnNameList : inputColumnNames;

        N.checkArgument(N.isEmpty(columnNames) || columnNames == this._columnNameList || this._columnNameList.containsAll(columnNames),
                "Some select properties {} are not found in DataSet: {}", columnNames, this._columnNameList);

        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;

        final Class<? extends T> rowClass = inputRowClass == null ? (Class<T>) inputRowSupplier.apply(0).getClass() : inputRowClass;
        final Type<T> rowType = N.typeOf(rowClass);
        final BeanInfo beanInfo = rowType.isBean() ? ParserUtil.getBeanInfo(rowClass) : null;

        final IntFunction<? extends T> rowSupplier = inputRowSupplier == null && !rowType.isBean() ? this.createRowSupplier(rowClass, rowType)
                : inputRowSupplier;

        return Stream.of(new ObjIteratorEx<T>() {
            private final int expectedModCount = modCount;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                checkConcurrentModification();

                return cursor < toRowIndex;
            }

            @Override
            public T next() {
                checkConcurrentModification();

                if (cursor >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return getRow(beanInfo, cursor++, columnNames, columnIndexes, columnCount, prefixAndFieldNameMap, rowClass, rowType, rowSupplier);
            }

            @Override
            public long count() {
                checkConcurrentModification();

                return toRowIndex - cursor; //NOSONAR
            }

            @Override
            public void advance(long n) {
                N.checkArgNotNegative(n, "n");

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                checkConcurrentModification();

                final List<T> rows = RowDataSet.this.toList(cursor, toRowIndex, columnNames, prefixAndFieldNameMap, rowClass, rowSupplier);

                a = a.length >= rows.size() ? a : (A[]) N.newArray(a.getClass().getComponentType(), rows.size());

                rows.toArray(a);

                return a;
            }

            final void checkConcurrentModification() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        });
    }

    /**
     *
     *
     * @param <T>
     * @param rowMapper
     * @return
     */
    @Override
    public <T> Stream<T> stream(IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) {
        return stream(0, size(), rowMapper);
    }

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowMapper
     * @return
     */
    @Override
    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) {
        return stream(fromRowIndex, toRowIndex, _columnNameList, rowMapper);
    }

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowMapper
     * @return
     */
    @Override
    public <T> Stream<T> stream(Collection<String> columnNames, IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) {
        return stream(0, size(), columnNames, rowMapper);
    }

    /**
     *
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param inputColumnNames
     * @param rowMapper
     * @return
     */
    @Override
    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Collection<String> inputColumnNames,
            IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final Collection<String> columnNames = N.isEmpty(inputColumnNames) ? this._columnNameList : inputColumnNames;

        N.checkArgument(N.isEmpty(columnNames) || columnNames == this._columnNameList || this._columnNameList.containsAll(columnNames),
                "Some select properties {} are not found in DataSet: {}", columnNames, this._columnNameList);

        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;

        return Stream.of(new ObjIteratorEx<T>() {
            private final int expectedModCount = modCount;
            private final Object[] row = new Object[columnCount];
            private final DisposableObjArray disposableArray = DisposableObjArray.wrap(row);
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                checkConcurrentModification();

                return cursor < toRowIndex;
            }

            @Override
            public T next() {
                checkConcurrentModification();

                if (cursor >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                for (int i = 0; i < columnCount; i++) {
                    row[i] = _columnList.get(columnIndexes[i]).get(cursor);
                }

                return rowMapper.apply(cursor, disposableArray);
            }

            @Override
            public long count() {
                checkConcurrentModification();

                return toRowIndex - cursor; //NOSONAR
            }

            @Override
            public void advance(long n) {
                N.checkArgNotNegative(n, "n");

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            final void checkConcurrentModification() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        });
    }

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowMapper
     * @return
     */
    @Override
    public <T> Stream<T> stream(Tuple2<String, String> columnNames, BiFunction<?, ?, ? extends T> rowMapper) {
        return stream(0, size(), columnNames, rowMapper);
    }

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
    @Override
    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Tuple2<String, String> columnNames, BiFunction<?, ?, ? extends T> rowMapper) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final BiFunction<Object, Object, ? extends T> rowMapperToUse = (BiFunction<Object, Object, ? extends T>) rowMapper;
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));

        return Stream.of(new ObjIteratorEx<T>() {
            private final int expectedModCount = modCount;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                checkConcurrentModification();

                return cursor < toRowIndex;
            }

            @Override
            public T next() {
                checkConcurrentModification();

                if (cursor >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T ret = rowMapperToUse.apply(column1.get(cursor), column2.get(cursor));
                cursor++;
                return ret;
            }

            @Override
            public long count() {
                checkConcurrentModification();

                return toRowIndex - cursor; //NOSONAR
            }

            @Override
            public void advance(long n) {
                N.checkArgNotNegative(n, "n");

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            final void checkConcurrentModification() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        });
    }

    /**
     *
     *
     * @param <T>
     * @param columnNames
     * @param rowMapper
     * @return
     */
    @Override
    public <T> Stream<T> stream(Tuple3<String, String, String> columnNames, TriFunction<?, ?, ?, ? extends T> rowMapper) {
        return stream(0, size(), columnNames, rowMapper);
    }

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
    @Override
    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, Tuple3<String, String, String> columnNames, TriFunction<?, ?, ?, ? extends T> rowMapper) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final TriFunction<Object, Object, Object, ? extends T> rowMapperToUse = (TriFunction<Object, Object, Object, ? extends T>) rowMapper;
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(columnNames._3));

        return Stream.of(new ObjIteratorEx<T>() {
            private final int expectedModCount = modCount;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                checkConcurrentModification();

                return cursor < toRowIndex;
            }

            @Override
            public T next() {
                checkConcurrentModification();

                if (cursor >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T ret = rowMapperToUse.apply(column1.get(cursor), column2.get(cursor), column3.get(cursor));
                cursor++;
                return ret;
            }

            @Override
            public long count() {
                checkConcurrentModification();

                return toRowIndex - cursor; //NOSONAR
            }

            @Override
            public void advance(long n) {
                N.checkArgNotNegative(n, "n");

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            final void checkConcurrentModification() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        });
    }

    //    @Override
    //    public <T> T getProperty(final String propName) {
    //        return (T) (_properties == null ? null : _properties.get(propName));
    //    }
    //
    //    @Override
    //    public <T> T setProperty(final String propName, final Object propValue) {
    //        if (_properties == null) {
    //            _properties = new Properties<String, Object>();
    //        }
    //
    //        return (T) _properties.put(propName, propValue);
    //    }
    //
    //    @Override
    //    public <T> T removeProperty(final String propName) {
    //        if (_properties == null) {
    //            return null;
    //        }
    //
    //        return (T) _properties.remove(propName);
    //    }

    /**
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    @Override
    public <R, E extends Exception> R apply(Throwables.Function<? super DataSet, ? extends R, E> func) throws E {
        return func.apply(this);
    }

    /**
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    @Override
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super DataSet, ? extends R, E> func) throws E {
        if (size() > 0) {
            return Optional.ofNullable(func.apply(this));
        } else {
            return Optional.empty();
        }
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void accept(Throwables.Consumer<? super DataSet, E> action) throws E {
        action.accept(this);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void acceptIfNotEmpty(Throwables.Consumer<? super DataSet, E> action) throws E {
        if (size() > 0) {
            action.accept(this);
        }
    }

    /**
     * Freeze.
     */
    @Override
    public void freeze() {
        _isFrozen = true;
    }

    /**
     *
     * @return true, if successful
     */
    @Override
    public boolean isFrozen() {
        return _isFrozen;
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Trim to size.
     */
    @Override
    public void trimToSize() {
        if (_columnList instanceof ArrayList) {
            ((ArrayList<?>) _columnList).trimToSize();
        }

        for (List<Object> column : _columnList) {
            if (column instanceof ArrayList) {
                ((ArrayList<?>) column).trimToSize();
            }
        }
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int size() {
        return (_columnList.size() == 0) ? 0 : _columnList.get(0).size();
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        checkFrozen();

        for (int i = 0; i < _columnList.size(); i++) {
            _columnList.get(i).clear();
        }

        // columnList.clear();
        modCount++;

        // Runtime.getRuntime().gc();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Properties<String, Object> properties() {
        if (_properties == null) {
            _properties = new Properties<>();
        }

        return _properties;
    }

    //    @Override
    //    public <T> T getProperty(final String propName) {
    //        return (T) (_properties == null ? null : _properties.get(propName));
    //    }
    //
    //    @Override
    //    public <T> T setProperty(final String propName, final Object propValue) {
    //        if (_properties == null) {
    //            _properties = new Properties<String, Object>();
    //        }
    //
    //        return (T) _properties.put(propName, propValue);
    //    }
    //
    //    @Override
    //    public <T> T removeProperty(final String propName) {
    //        if (_properties == null) {
    //            return null;
    //        }
    //
    //        return (T) _properties.remove(propName);
    //    }

    /**
     *
     *
     * @return
     */
    @Override
    public Stream<String> columnNames() {
        return Stream.of(_columnNameList);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Stream<ImmutableList<Object>> columns() {
        return IntStream.range(0, this.columnCount()).mapToObj(this::getColumn);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Map<String, ImmutableList<Object>> columnMap() {
        final Map<String, ImmutableList<Object>> result = new LinkedHashMap<>();

        for (String columnName : _columnNameList) {
            result.put(columnName, getColumn(columnName));
        }

        return result;
    }

    //    @Override
    //    public DataSetBuilder builder() {
    //        return Builder.of(this);
    //    }

    /**
     *
     */
    @Override
    public void println() {
        println(0, size());
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void println(int fromRowIndex, int toRowIndex) {
        println(fromRowIndex, toRowIndex, _columnNameList); // NOSONAR
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param columnNames
     */
    @Override
    public void println(int fromRowIndex, int toRowIndex, Collection<String> columnNames) {
        println(fromRowIndex, toRowIndex, columnNames, IOUtil.newOutputStreamWriter(System.out)); // NOSONAR
    }

    /**
     * 
     *
     * @param outputWriter 
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void println(final Writer outputWriter) throws UncheckedIOException {
        println(0, size(), _columnNameList, outputWriter);
    }

    /**
     * 
     *
     * @param fromRowIndex 
     * @param toRowIndex 
     * @param columnNames 
     * @param outputWriter 
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void println(int fromRowIndex, int toRowIndex, Collection<String> columnNames, final Writer outputWriter) throws UncheckedIOException {
        final int[] columnIndexes = N.isEmpty(columnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(columnNames);
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(outputWriter, "outputWriter");

        boolean isBufferedWriter = outputWriter instanceof BufferedWriter || outputWriter instanceof java.io.BufferedWriter;
        final Writer bw = isBufferedWriter ? outputWriter : Objectory.createBufferedWriter(outputWriter);
        final int rowLen = toRowIndex - fromRowIndex;
        final int columnLen = columnIndexes.length;

        try {
            if (columnLen == 0) {
                bw.write("+---+");
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write("|   |");
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write("+---+");
            } else {
                final List<String> columnNameList = new ArrayList<>(columnNames);
                final List<List<String>> strColumnList = new ArrayList<>(columnLen);
                final int[] maxColumnLens = new int[columnLen];

                for (int i = 0; i < columnLen; i++) {
                    final List<Object> column = _columnList.get(columnIndexes[i]);
                    final List<String> strColumn = new ArrayList<>(rowLen);
                    int maxLen = N.len(columnNameList.get(i));
                    String str = null;

                    for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                        str = N.toString(column.get(rowIndex));
                        maxLen = N.max(maxLen, N.len(str));
                        strColumn.add(str);
                    }

                    maxColumnLens[i] = maxLen;
                    strColumnList.add(strColumn);
                }

                final char hch = '-';
                final char hchDelta = 2;
                for (int i = 0; i < columnLen; i++) {
                    bw.write('+');

                    bw.write(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                }

                bw.write('+');

                bw.write(IOUtil.LINE_SEPARATOR);

                for (int i = 0; i < columnLen; i++) {
                    if (i == 0) {
                        bw.write("| ");
                    } else {
                        bw.write(" | ");
                    }

                    bw.write(Strings.padEnd(columnNameList.get(i), maxColumnLens[i]));
                }

                bw.write(" |");

                bw.write(IOUtil.LINE_SEPARATOR);

                for (int i = 0; i < columnLen; i++) {
                    bw.write('+');

                    bw.write(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
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
        } catch (IOException e) {
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
        int h = 17;
        h = (h * 31) + _columnNameList.hashCode();
        return (h * 31) + _columnList.hashCode();
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof RowDataSet other) {
            return (size() == other.size()) && N.equals(_columnNameList, other._columnNameList) && N.equals(_columnList, other._columnList);
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
        if (columnCount() == 0) {
            return "[[]]";
        }

        final BufferedWriter bw = Objectory.createBufferedWriter();

        try {
            toCSV(true, false, bw);

            return bw.toString();
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Check frozen.
     */
    void checkFrozen() {
        if (_isFrozen) {
            throw new IllegalStateException("This DataSet is frozen, can't modify it.");
        }
    }

    /**
     * Check column name.
     *
     * @param columnName
     * @return
     */
    int checkColumnName(final String columnName) {
        int columnIndex = getColumnIndex(columnName);

        if (columnIndex < 0) {
            throw new IllegalArgumentException("The specified column: " + columnName + " is not included in this DataSet: " + _columnNameList);
        }

        return columnIndex;
    }

    /**
     * Check column name.
     *
     * @param columnNames
     * @return
     */
    int[] checkColumnName(final String... columnNames) {
        if (N.isEmpty(columnNames)) {
            throw new IllegalArgumentException("The specified columnNames is null or empty");
        }

        final int length = columnNames.length;
        final int[] columnIndexes = new int[length];

        for (int i = 0; i < length; i++) {
            columnIndexes[i] = checkColumnName(columnNames[i]);
        }

        return columnIndexes;
    }

    /**
     * Check column name.
     *
     * @param columnNames
     * @return
     */
    int[] checkColumnName(final Collection<String> columnNames) {
        if (N.isEmpty(columnNames)) {
            throw new IllegalArgumentException("The specified columnNames is null or empty");
        }

        if (columnNames != this._columnNameList) {
            final int count = columnNames.size();
            final int[] columnNameIndexes = new int[count];
            final Iterator<String> it = columnNames.iterator();

            for (int i = 0; i < count; i++) {
                columnNameIndexes[i] = checkColumnName(it.next());
            }

            return columnNameIndexes;
        }

        if (this._columnIndexes == null) {
            int count = columnNames.size();
            this._columnIndexes = new int[count];

            for (int i = 0; i < count; i++) {
                _columnIndexes[i] = i;
            }
        }

        return _columnIndexes;
    }

    /**
     * Check row num.
     *
     * @param rowNum
     */
    void checkRowNum(final int rowNum) {
        if ((rowNum < 0) || (rowNum >= size())) {
            throw new IllegalArgumentException("Invalid row number: " + rowNum + ". It must be >= 0 and < " + size());
        }
    }

    /**
     * Check row index.
     *
     * @param fromRowIndex
     * @param toRowIndex
     */
    void checkRowIndex(final int fromRowIndex, final int toRowIndex) {
        checkRowIndex(fromRowIndex, toRowIndex, size());
    }

    /**
     * Check row index.
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @param size
     */
    void checkRowIndex(final int fromRowIndex, final int toRowIndex, final int size) {
        if ((fromRowIndex < 0) || (fromRowIndex > toRowIndex) || (toRowIndex > size)) {
            throw new IllegalArgumentException("Invalid fromRowIndex : " + fromRowIndex + " or toRowIndex: " + toRowIndex);
        }
    }

    /**
     * Gets the hash key.
     *
     * @param obj
     * @return
     */
    static Object hashKey(Object obj) {
        return N.hashKey(obj);
    }

    /**
     * The Class PaginatedRowDataSet.
     *
     * @author Haiyang Li
     * @version $Revision: 0.8 $ 07/01/15
     */
    private class PaginatedDataSet implements Paginated<DataSet> {
        /** The expected mod count. */
        private final int expectedModCount = modCount;

        /** The page pool. */
        private final Map<Integer, DataSet> pagePool = new HashMap<>();

        private final Collection<String> columnNames;

        /** The page size. */
        private final int pageSize;

        /** The page count. */
        private final int totalPages;

        /** The current page num. */
        private int currentPageNum;

        /**
         * Instantiates a new paginated row data set.
         *
         * @param pageSize
         */
        private PaginatedDataSet(final Collection<String> columnNames, final int pageSize) {
            // N.checkArgNotEmpty(columnNames, "columnNames"); // empty DataSet.
            N.checkArgPositive(pageSize, "pageSize");

            this.columnNames = columnNames;
            this.pageSize = pageSize;

            this.totalPages = ((RowDataSet.this.size() % pageSize) == 0) ? (RowDataSet.this.size() / pageSize) : ((RowDataSet.this.size() / pageSize) + 1);

            currentPageNum = 0;
        }

        /**
         *
         * @return
         */
        @Override
        public Iterator<DataSet> iterator() {
            return new ObjIterator<>() {
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    return cursor < totalPages;
                }

                @Override
                public DataSet next() {
                    checkConcurrentModification();

                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    DataSet ret = getPage(cursor);
                    cursor++;
                    return ret;
                }
            };
        }

        /**
         * Checks for next.
         *
         * @return true, if successful
         */
        @Override
        public boolean hasNext() {
            return currentPageNum < totalPages;
        }

        /**
         *
         * @return
         */
        @Override
        public DataSet nextPage() {
            return absolute(currentPageNum + 1).currentPage();
        }

        /**
         *
         * @return
         */
        @Override
        public DataSet currentPage() {
            return getPage(currentPageNum);
        }

        /**
         *
         * @return
         */
        @Override
        public DataSet previousPage() {
            return absolute(currentPageNum - 1).currentPage();
        }

        /**
         *
         * @return
         */
        @Override
        public Optional<DataSet> firstPage() {
            return (Optional<DataSet>) (pageCount() == 0 ? Optional.empty() : Optional.of(absolute(0).currentPage()));
        }

        /**
         *
         * @return
         */
        @Override
        public Optional<DataSet> lastPage() {
            return (Optional<DataSet>) (pageCount() == 0 ? Optional.empty() : Optional.of(absolute(pageCount() - 1).currentPage()));
        }

        /**
         * Gets the page.
         *
         * @param pageNum
         * @return
         */
        @Override
        public DataSet getPage(final int pageNum) {
            checkConcurrentModification();
            checkPageNumber(pageNum);

            synchronized (pagePool) {
                DataSet page = pagePool.get(pageNum);

                if (page == null) {
                    int offset = pageNum * pageSize;
                    page = RowDataSet.this.slice(offset, Math.min(offset + pageSize, RowDataSet.this.size()), columnNames);

                    pagePool.put(pageNum, page);
                }

                return page;
            }
        }

        /**
         *
         * @param pageNumber
         * @return
         */
        @Override
        public Paginated<DataSet> absolute(final int pageNumber) {
            checkPageNumber(pageNumber);

            currentPageNum = pageNumber;

            return this;
        }

        /**
         * Current page num.
         *
         * @return
         */
        @Override
        public int currentPageNum() {
            return currentPageNum;
        }

        /**
         *
         * @return
         */
        @Override
        public int pageSize() {
            return pageSize;
        }

        /**
         *
         * @return
         * @deprecated
         */
        @Deprecated
        @Override
        public int pageCount() {
            return totalPages;
        }

        /**
         *
         * @return
         */
        @Override
        public int totalPages() {
            return totalPages;
        }

        /**
         *
         * @return
         */
        @Override
        public Stream<DataSet> stream() {
            return Stream.of(iterator());
        }

        final void checkConcurrentModification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        /**
         * Check page number.
         *
         * @param pageNumber
         */
        private void checkPageNumber(final int pageNumber) {
            if ((pageNumber < 0) || (pageNumber >= pageCount())) {
                throw new IllegalArgumentException(pageNumber + " out of page index [0, " + pageCount() + ")");
            }
        }
    }
}
