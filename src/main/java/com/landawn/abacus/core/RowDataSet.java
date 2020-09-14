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

package com.landawn.abacus.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;

import com.landawn.abacus.DataSet;
import com.landawn.abacus.DirtyMarker;
import com.landawn.abacus.PaginatedDataSet;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONParser;
import com.landawn.abacus.parser.JSONSerializationConfig;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.XMLConstants;
import com.landawn.abacus.parser.XMLParser;
import com.landawn.abacus.parser.XMLSerializationConfig;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.ArrayHashMap;
import com.landawn.abacus.util.ArrayHashSet;
import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.BufferedXMLWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.Iterables;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Properties;
import com.landawn.abacus.util.StringUtil;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Throwables.BiFunction;
import com.landawn.abacus.util.Throwables.Predicate;
import com.landawn.abacus.util.Throwables.TriConsumer;
import com.landawn.abacus.util.Throwables.TriFunction;
import com.landawn.abacus.util.Throwables.TriPredicate;
import com.landawn.abacus.util.TriIterator;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.Wrapper;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IndexedConsumer;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.Collector;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * It's a row DataSet from logic aspect. But each column is stored in a list.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class RowDataSet implements DataSet, Cloneable {

    static final char PROP_NAME_SEPARATOR = '.';

    static final String NULL_STRING = "null".intern();

    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    static final String TRUE = Boolean.TRUE.toString().intern();

    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();

    static final String FALSE = Boolean.FALSE.toString().intern();

    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    static final Set<Class<?>> SUPPORTED_COUNT_COLUMN_TYPES = N.asSet((Class<?>) int.class, Integer.class, long.class, Long.class, float.class, Float.class,
            double.class, Double.class);

    /**
     * Field CACHED_PROP_NAMES. (value is ""cachedPropNames"")
     */
    public static final String CACHED_PROP_NAMES = "cachedPropNames";

    private static final String ROW = "row";

    private static final JSONParser jsonParser = ParserFactory.createJSONParser();

    private static final XMLParser xmlParser = ParserFactory.isXMLAvailable() ? ParserFactory.createXMLParser() : null;

    private static final KryoParser kryoParser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : null;

    private static final JSONSerializationConfig jsc = JSC.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

    private static final XMLSerializationConfig xsc = XSC.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

    private static final Type<Object> strType = N.typeOf(String.class);

    @SuppressWarnings("rawtypes")
    private static final Comparator<Object[]> MULTI_COLUMN_COMPARATOR = new Comparator<Object[]>() {
        private final Comparator<Comparable> naturalOrder = Comparators.naturalOrder();

        @Override
        public int compare(final Object[] o1, final Object[] o2) {
            int rt = 0;

            for (int i = 0, len = o1.length; i < len; i++) {
                rt = naturalOrder.compare((Comparable) o1[i], (Comparable) o2[i]);

                if (rt != 0) {
                    return rt;
                }
            }

            return rt;
        }
    };

    List<String> _columnNameList;

    List<List<Object>> _columnList;

    Map<String, Integer> _columnIndexMap;

    int[] _columnIndexes;

    int _currentRowNum = 0;

    boolean _isFrozen = false;

    Properties<String, Object> _properties;

    transient int modCount = 0;

    // For Kryo
    protected RowDataSet() {
    }

    public RowDataSet(final List<String> columnNameList, final List<List<Object>> columnList) {
        this(columnNameList, columnList, null);
    }

    public RowDataSet(final List<String> columnNameList, final List<List<Object>> columnList, final Properties<String, Object> properties) {
        N.checkArgNotNull(columnNameList);
        N.checkArgNotNull(columnList);
        N.checkArgument(N.hasDuplicates(columnNameList) == false, "Dupliated column names: {}", columnNameList);
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
    //    public String entityName() {
    //        return _entityName;
    //    }
    //
    //    @SuppressWarnings("unchecked")
    //    @Override
    //    public <T> Class<T> entityClass() {
    //        return (Class<T>) _entityClass;
    //    }

    /**
     * Column name list.
     *
     * @return
     */
    @Override
    public ImmutableList<String> columnNameList() {
        // return _columnNameList;

        return ImmutableList.of(_columnNameList);
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

        if (columnIndex == null /* && NameUtil.isCanonicalName(_entityName, columnName)*/) {
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
            if (containsColumn(columnName) == false) {
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
                throw new IllegalArgumentException("The new property name is already included: " + _columnNameList + ". ");
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
                throw new IllegalArgumentException("The new property name is already included: " + _columnNameList + ". ");
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

        final Map<String, String> map = N.newHashMap(N.initHashCapacity(columnNames.size()));

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

        if (newPosition < 0 || newPosition >= _columnNameList.size()) {
            throw new IllegalArgumentException("The new column index must be >= 0 and < " + _columnNameList.size());
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

            if (entry.getValue().intValue() < 0 || entry.getValue().intValue() >= _columnNameList.size()) {
                throw new IllegalArgumentException("The new column index must be >= 0 and < " + _columnNameList.size());
            }

            entries.add(entry);
        }

        N.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return Integer.compare(o1.getValue(), o2.getValue());
            }
        });

        for (Map.Entry<String, Integer> entry : entries) {
            int currentColumnIndex = checkColumnName(entry.getKey());

            if (currentColumnIndex == entry.getValue().intValue()) {
                // ignore.
            } else {
                _columnNameList.add(entry.getValue().intValue(), _columnNameList.remove(currentColumnIndex));
                _columnList.add(entry.getValue().intValue(), _columnList.remove(currentColumnIndex));

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

        if (N.notNullOrEmpty(_columnIndexMap)) {
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

    /**
     *
     * @param <T>
     * @param targetType
     * @param rowIndex
     * @param columnIndex
     * @return
     */
    @Override
    public <T> T get(final Class<T> targetType, final int rowIndex, final int columnIndex) {
        T rt = (T) _columnList.get(columnIndex).get(rowIndex);

        return (rt == null) ? N.defaultValueOf(targetType) : rt;
    }

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

    /**
     *
     * @param <T>
     * @param targetType
     * @param columnIndex
     * @return
     */
    @Override
    public <T> T get(final Class<T> targetType, final int columnIndex) {
        T rt = get(columnIndex);

        return (rt == null) ? N.defaultValueOf(targetType) : rt;
    }

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

    /**
     *
     * @param <T>
     * @param targetType
     * @param columnName
     * @return
     */
    @Override
    public <T> T get(final Class<T> targetType, final String columnName) {
        return get(targetType, checkColumnName(columnName));
    }

    /**
     * Gets the or default.
     *
     * @param <T>
     * @param columnIndex
     * @param defaultValue
     * @return
     */
    @Override
    public <T> T getOrDefault(int columnIndex, T defaultValue) {
        return columnIndex < 0 ? defaultValue : (T) get(columnIndex);
    }

    /**
     * Gets the or default.
     *
     * @param <T>
     * @param columnName
     * @param defaultValue
     * @return
     */
    @Override
    public <T> T getOrDefault(final String columnName, T defaultValue) {
        return getOrDefault(getColumnIndex(columnName), defaultValue);
    }

    /**
     * Gets the boolean.
     *
     * @param columnIndex
     * @return
     */
    @Override
    public boolean getBoolean(final int columnIndex) {
        Boolean rt = get(boolean.class, columnIndex);

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

        return (rt == null) ? 0f : rt.floatValue();
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

        return (rt == null) ? 0d : rt.doubleValue();
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
        return ImmutableList.of((List) _columnList.get(columnIndex));
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
     * @param columnName
     * @param column
     */
    @Override
    public void addColumn(final String columnName, final List<?> column) {
        addColumn(_columnList.size(), columnName, column);
    }

    /**
     * Adds the column.
     *
     * @param columnIndex
     * @param columnName
     * @param column
     */
    @Override
    public void addColumn(final int columnIndex, final String columnName, final List<?> column) {
        checkFrozen();

        if (columnIndex < 0 || columnIndex > _columnNameList.size()) {
            throw new IllegalArgumentException("Invalid column index: " + columnIndex + ". It must be >= 0 and <= " + _columnNameList.size());
        }

        if (containsColumn(columnName)) {
            throw new IllegalArgumentException("Column(" + columnName + ") is already included in this DataSet.");
        }

        if (N.notNullOrEmpty(column) && column.size() != size()) {
            throw new IllegalArgumentException("The specified column size[" + column.size() + "] must be same as the this DataSet size[" + size() + "]. ");
        }

        _columnNameList.add(columnIndex, columnName);

        if (N.isNullOrEmpty(column)) {
            _columnList.add(columnIndex, N.repeat(null, size()));
        } else {
            _columnList.add(columnIndex, new ArrayList<>(column));
        }

        updateColumnIndex(columnIndex, columnName);

        modCount++;
    }

    /**
     * Adds the column.
     *
     * @param <T>
     * @param <E>
     * @param newColumnName
     * @param fromColumnName
     * @param func
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> void addColumn(String newColumnName, String fromColumnName, Throwables.Function<T, ?, E> func) throws E {
        addColumn(_columnList.size(), newColumnName, fromColumnName, func);
    }

    /**
     * Adds the column.
     *
     * @param <T>
     * @param <E>
     * @param columnIndex
     * @param newColumnName
     * @param fromColumnName
     * @param func
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> void addColumn(int columnIndex, final String newColumnName, String fromColumnName, Throwables.Function<T, ?, E> func)
            throws E {
        checkFrozen();

        if (columnIndex < 0 || columnIndex > _columnNameList.size()) {
            throw new IllegalArgumentException("Invalid column index: " + columnIndex + ". It must be >= 0 and <= " + _columnNameList.size());
        }

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("Column(" + newColumnName + ") is already included in this DataSet.");
        }

        final List<Object> newColumn = new ArrayList<>(size());
        final Throwables.Function<Object, Object, E> mapper2 = (Throwables.Function<Object, Object, E>) func;
        final List<Object> column = _columnList.get(checkColumnName(fromColumnName));

        for (Object val : column) {
            newColumn.add(mapper2.apply(val));
        }

        _columnNameList.add(columnIndex, newColumnName);
        _columnList.add(columnIndex, newColumn);

        updateColumnIndex(columnIndex, newColumnName);

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
     * @param columnIndex
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void addColumn(int columnIndex, final String newColumnName, Collection<String> fromColumnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> func) throws E {
        checkFrozen();

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("Column(" + newColumnName + ") is already included in this DataSet.");
        }

        final int size = size();
        final int[] fromColumnIndexes = checkColumnName(fromColumnNames);
        final Throwables.Function<DisposableObjArray, Object, E> mapper2 = (Throwables.Function<DisposableObjArray, Object, E>) func;
        final List<Object> newColumn = new ArrayList<>(size);
        final Object[] row = new Object[fromColumnIndexes.length];
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(row);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0, len = fromColumnIndexes.length; i < len; i++) {
                row[i] = _columnList.get(fromColumnIndexes[i]).get(rowIndex);
            }

            newColumn.add(mapper2.apply(disposableArray));
        }

        _columnNameList.add(columnIndex, newColumnName);
        _columnList.add(columnIndex, newColumn);

        updateColumnIndex(columnIndex, newColumnName);

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
     * @param columnIndex
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void addColumn(int columnIndex, final String newColumnName, Tuple2<String, String> fromColumnNames,
            Throwables.BiFunction<?, ?, ?, E> func) throws E {
        checkFrozen();

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("Column(" + newColumnName + ") is already included in this DataSet.");
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

        _columnNameList.add(columnIndex, newColumnName);
        _columnList.add(columnIndex, newColumn);

        updateColumnIndex(columnIndex, newColumnName);

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
    public <E extends Exception> void addColumn(String newColumnName, Tuple3<String, String, String> fromColumnNames, TriFunction<?, ?, ?, ?, E> func)
            throws E {
        addColumn(_columnList.size(), newColumnName, fromColumnNames, func);
    }

    /**
     * Adds the column.
     *
     * @param <E>
     * @param columnIndex
     * @param newColumnName
     * @param fromColumnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <E extends Exception> void addColumn(int columnIndex, final String newColumnName, Tuple3<String, String, String> fromColumnNames,
            TriFunction<?, ?, ?, ?, E> func) throws E {
        checkFrozen();

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("Column(" + newColumnName + ") is already included in this DataSet.");
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

        _columnNameList.add(columnIndex, newColumnName);
        _columnList.add(columnIndex, newColumn);

        updateColumnIndex(columnIndex, newColumnName);

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
    public <E extends Exception> void removeColumns(Predicate<String, E> filter) throws E {
        removeColumns(N.filter(_columnNameList, filter));
    }

    /**
     * Removes the columns if.
     *
     * @param <E>
     * @param filter
     * @throws E the e
     */
    @Deprecated
    @Override
    public <E extends Exception> void removeColumnsIf(Predicate<String, E> filter) throws E {
        removeColumns(filter);
    }

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
     * @param <T>
     * @param <E>
     * @param columnName
     * @param func
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> void updateColumn(final String columnName, final Throwables.Function<T, ?, E> func) throws E {
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
     * @param <T>
     * @param <E>
     * @param columnNames
     * @param func
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> void updateColumns(final Collection<String> columnNames, final Throwables.Function<?, ?, E> func) throws E {
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
     * @param newColumnClass
     */
    @Override
    public void combineColumns(final Collection<String> columnNames, final String newColumnName, final Class<?> newColumnClass) {
        checkFrozen();

        final List<Object> newColumn = toList(newColumnClass, columnNames, 0, size());

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
     * @param newColumnClass
     * @throws E the e
     */
    @Override
    public <E extends Exception> void combineColumns(Throwables.Predicate<String, E> columnNameFilter, final String newColumnName, Class<?> newColumnClass)
            throws E {
        combineColumns(N.filter(_columnNameList, columnNameFilter), newColumnName, newColumnClass);
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
     * @param <T>
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param divideFunc
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> void divideColumn(final String columnName, Collection<String> newColumnNames,
            Throwables.Function<T, ? extends List<?>, E> divideFunc) throws E {
        checkFrozen();

        final int columnIndex = this.checkColumnName(columnName);

        if (N.isNullOrEmpty(newColumnNames)) {
            throw new IllegalArgumentException("New column names can't be null or empty.");
        }

        if (N.disjoint(_columnNameList, newColumnNames) == false) {
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
     * @param <T>
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param output
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> void divideColumn(final String columnName, Collection<String> newColumnNames, Throwables.BiConsumer<T, Object[], E> output)
            throws E {
        checkFrozen();

        final int columnIndex = this.checkColumnName(columnName);

        if (N.isNullOrEmpty(newColumnNames)) {
            throw new IllegalArgumentException("New column names can't be null or empty.");
        }

        if (N.disjoint(_columnNameList, newColumnNames) == false) {
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
     * @param <T>
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param output
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> void divideColumn(final String columnName, final Tuple2<String, String> newColumnNames,
            final Throwables.BiConsumer<T, Pair<Object, Object>, E> output) throws E {
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
     * @param <T>
     * @param <E>
     * @param columnName
     * @param newColumnNames
     * @param output
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> void divideColumn(final String columnName, final Tuple3<String, String, String> newColumnNames,
            final Throwables.BiConsumer<T, Triple<Object, Object, Object>, E> output) throws E {
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
     * @param rowIndex
     * @param row
     */
    @Override
    public void addRow(final int rowIndex, final Object row) {
        checkFrozen();

        if ((rowIndex < 0) || (rowIndex > size())) {
            throw new IllegalArgumentException("Invalid row index: " + rowIndex + ". It must be >= 0 and <= " + size());
        }

        final Class<?> rowClass = row.getClass();
        final Type<?> rowType = N.typeOf(rowClass);

        if (rowType.isObjectArray()) {
            final Object[] a = (Object[]) row;

            if (a.length < this._columnNameList.size()) {
                throw new IllegalArgumentException(
                        "The size of array (" + a.length + ") is less than the size of column (" + this._columnNameList.size() + ")");
            }

            if (rowIndex == size()) {
                for (int i = 0, len = this._columnNameList.size(); i < len; i++) {
                    _columnList.get(i).add(a[i]);
                }
            } else {
                for (int i = 0, len = this._columnNameList.size(); i < len; i++) {
                    _columnList.get(i).add(rowIndex, a[i]);
                }
            }
        } else if (rowType.isCollection()) {
            final Collection<Object> c = (Collection<Object>) row;

            if (c.size() < this._columnNameList.size()) {
                throw new IllegalArgumentException(
                        "The size of collection (" + c.size() + ") is less than the size of column (" + this._columnNameList.size() + ")");
            }

            final Iterator<Object> it = c.iterator();

            if (rowIndex == size()) {
                for (int i = 0, len = this._columnNameList.size(); i < len; i++) {
                    _columnList.get(i).add(it.next());
                }
            } else {
                for (int i = 0, len = this._columnNameList.size(); i < len; i++) {
                    _columnList.get(i).add(rowIndex, it.next());
                }
            }
        } else if (rowType.isMap()) {
            final Map<String, Object> map = (Map<String, Object>) row;
            final Object[] a = new Object[this._columnNameList.size()];

            int idx = 0;
            for (String columnName : this._columnNameList) {
                a[idx] = map.get(columnName);

                if (a[idx] == null && map.containsKey(columnName) == false) {
                    throw new IllegalArgumentException("Column (" + columnName + ") is not found in map (" + map.keySet() + ")");
                }

                idx++;
            }

            if (rowIndex == size()) {
                for (int i = 0, len = this._columnNameList.size(); i < len; i++) {
                    _columnList.get(i).add(a[i]);
                }
            } else {
                for (int i = 0, len = this._columnNameList.size(); i < len; i++) {
                    _columnList.get(i).add(rowIndex, a[i]);
                }
            }
        } else if (rowType.isEntity()) {
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(rowClass);
            final Object[] a = new Object[this._columnNameList.size()];
            PropInfo propInfo = null;
            int idx = 0;

            for (String columnName : this._columnNameList) {
                propInfo = entityInfo.getPropInfo(columnName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Column (" + columnName + ") is not found in entity (" + rowClass + ")");
                }

                a[idx++] = propInfo.getPropValue(row);
            }

            if (rowIndex == size()) {
                for (int i = 0, len = this._columnNameList.size(); i < len; i++) {
                    _columnList.get(i).add(a[i]);
                }
            } else {
                for (int i = 0, len = this._columnNameList.size(); i < len; i++) {
                    _columnList.get(i).add(rowIndex, a[i]);
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and entity class are supported");
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
            N.deleteAll(_columnList.get(i), indices);
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
     * @param rowNum
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object[] getRow(final int rowNum) {
        return getRow(Object[].class, rowNum);
    }

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowClass
     * @param rowNum
     * @return
     */
    @Override
    public <T> T getRow(final Class<? extends T> rowClass, final int rowNum) {
        return getRow(rowClass, _columnNameList, rowNum);
    }

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowClass
     * @param columnNames
     * @param rowNum
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getRow(final Class<? extends T> rowClass, final Collection<String> columnNames, final int rowNum) {
        checkRowNum(rowNum);

        final Type<?> rowType = N.typeOf(rowClass);
        final boolean isAbstractRowClass = Modifier.isAbstract(rowClass.getModifiers());
        final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass, int.class);
        final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass);
        final int columnCount = columnNames.size();

        Object result = null;
        if (rowType.isObjectArray()) {
            result = N.newArray(rowClass.getComponentType(), columnCount);

        } else if (rowType.isList() || rowType.isSet()) {
            if (isAbstractRowClass) {
                result = (rowType.isList() ? new ArrayList<>(columnCount) : N.newHashSet(N.initHashCapacity(columnCount)));
            } else {
                if (intConstructor == null) {
                    result = ClassUtil.invokeConstructor(constructor);
                } else {
                    result = ClassUtil.invokeConstructor(intConstructor, columnCount);
                }
            }

        } else if (rowType.isMap()) {
            if (isAbstractRowClass) {
                result = new HashMap<>(N.initHashCapacity(columnCount));
            } else {
                if (intConstructor == null) {
                    result = ClassUtil.invokeConstructor(constructor);
                } else {
                    result = ClassUtil.invokeConstructor(intConstructor, N.initHashCapacity(columnCount));
                }
            }
        } else if (rowType.isEntity()) {
            result = N.newInstance(rowClass);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and entity class are supported");
        }

        getRow(rowType, result, checkColumnName(columnNames), columnNames, rowNum);

        return (T) result;
    }

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowSupplier
     * @param rowNum
     * @return
     */
    @Override
    public <T> T getRow(IntFunction<? extends T> rowSupplier, int rowNum) {
        return getRow(rowSupplier, _columnNameList, rowNum);
    }

    /**
     * Gets the row.
     *
     * @param <T>
     * @param rowSupplier
     * @param columnNames
     * @param rowNum
     * @return
     */
    @Override
    public <T> T getRow(IntFunction<? extends T> rowSupplier, Collection<String> columnNames, int rowNum) {
        checkRowNum(rowNum);

        final T row = rowSupplier.apply(columnNames.size());

        getRow(N.typeOf(row.getClass()), row, checkColumnName(columnNames), columnNames, rowNum);

        return row;
    }

    @Override
    public Optional<Object[]> firstRow() {
        return firstRow(Object[].class);
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @return
     */
    @Override
    public <T> Optional<T> firstRow(final Class<? extends T> rowClass) {
        return firstRow(rowClass, _columnNameList);
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @param columnNames
     * @return
     */
    @Override
    public <T> Optional<T> firstRow(final Class<? extends T> rowClass, final Collection<String> columnNames) {
        return size() == 0 ? (Optional<T>) Optional.empty() : Optional.of(getRow(rowClass, columnNames, 0));
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> Optional<T> firstRow(IntFunction<? extends T> rowSupplier) {
        return firstRow(rowSupplier, _columnNameList);
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @param columnNames
     * @return
     */
    @Override
    public <T> Optional<T> firstRow(IntFunction<? extends T> rowSupplier, Collection<String> columnNames) {
        if (size() == 0) {
            return Optional.empty();
        }

        final T row = getRow(rowSupplier, columnNames, 0);

        return Optional.of(row);
    }

    @Override
    public Optional<Object[]> lastRow() {
        return lastRow(Object[].class);
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @return
     */
    @Override
    public <T> Optional<T> lastRow(final Class<? extends T> rowClass) {
        return lastRow(rowClass, _columnNameList);
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @param columnNames
     * @return
     */
    @Override
    public <T> Optional<T> lastRow(final Class<? extends T> rowClass, final Collection<String> columnNames) {
        return size() == 0 ? (Optional<T>) Optional.empty() : Optional.of(getRow(rowClass, columnNames, size() - 1));
    }

    /**
     * Gets the row.
     *
     * @param rowType
     * @param output
     * @param columnIndexes
     * @param columnNames
     * @param rowNum
     * @return
     */
    private void getRow(final Type<?> rowType, final Object output, int[] columnIndexes, final Collection<String> columnNames, final int rowNum) {
        checkRowNum(rowNum);

        if (columnIndexes == null) {
            columnIndexes = checkColumnName(columnNames);
        }

        final int columnCount = columnIndexes.length;

        if (rowType.isObjectArray()) {
            final Object[] result = (Object[]) output;

            for (int i = 0; i < columnCount; i++) {
                result[i] = _columnList.get(columnIndexes[i]).get(rowNum);
            }
        } else if (rowType.isCollection()) {
            final Collection<Object> result = (Collection<Object>) output;

            for (int i = 0; i < columnCount; i++) {
                result.add(_columnList.get(columnIndexes[i]).get(rowNum));
            }

        } else if (rowType.isMap()) {
            final Map<String, Object> result = (Map<String, Object>) output;

            for (int i = 0; i < columnCount; i++) {
                result.put(_columnNameList.get(columnIndexes[i]), _columnList.get(columnIndexes[i]).get(rowNum));
            }

        } else if (rowType.isEntity()) {
            final boolean ignoreUnknownProperty = columnNames == _columnNameList;
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(rowType.clazz());
            Object result = output;
            String propName = null;
            Object propValue = null;

            for (int i = 0; i < columnCount; i++) {
                propName = _columnNameList.get(columnIndexes[i]);
                propValue = _columnList.get(columnIndexes[i]).get(rowNum);

                entityInfo.setPropValue(result, propName, propValue, ignoreUnknownProperty);
            }

            if (result instanceof DirtyMarker) {
                DirtyMarkerUtil.markDirty((DirtyMarker) result, false);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowType.clazz().getCanonicalName() + ". Only Array, Collection, Map and entity class are supported");
        }
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> Optional<T> lastRow(IntFunction<? extends T> rowSupplier) {
        return lastRow(rowSupplier, _columnNameList);
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @param columnNames
     * @return
     */
    @Override
    public <T> Optional<T> lastRow(IntFunction<? extends T> rowSupplier, Collection<String> columnNames) {
        if (size() == 0) {
            return Optional.empty();
        }
        final T row = getRow(rowSupplier, columnNames, size() - 1);

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
        return iterator(columnNameA, columnNameB, 0, size());
    }

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
    @Override
    public <A, B> BiIterator<A, B> iterator(final String columnNameA, final String columnNameB, final int fromRowIndex, final int toRowIndex) {
        this.checkRowIndex(fromRowIndex, toRowIndex);
        final List<Object> columnA = _columnList.get(checkColumnName(columnNameA));
        final List<Object> columnB = _columnList.get(checkColumnName(columnNameB));

        final IndexedConsumer<Pair<A, B>> output = new IndexedConsumer<Pair<A, B>>() {
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
        return iterator(columnNameA, columnNameB, columnNameC, 0, size());
    }

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
    @Override
    public <A, B, C> TriIterator<A, B, C> iterator(final String columnNameA, final String columnNameB, final String columnNameC, final int fromRowIndex,
            final int toRowIndex) {
        this.checkRowIndex(fromRowIndex, toRowIndex);
        final List<Object> columnA = _columnList.get(checkColumnName(columnNameA));
        final List<Object> columnB = _columnList.get(checkColumnName(columnNameB));
        final List<Object> columnC = _columnList.get(checkColumnName(columnNameC));

        final IndexedConsumer<Triple<A, B, C>> output = new IndexedConsumer<Triple<A, B, C>>() {
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
        forEach(columnNames, 0, size(), action);
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
        forEach(this._columnNameList, fromRowIndex, toRowIndex, action);
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void forEach(final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex,
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
        forEach(columnNames, 0, size(), action);
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void forEach(Tuple2<String, String> columnNames, int fromRowIndex, int toRowIndex, Throwables.BiConsumer<?, ?, E> action)
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
    public <E extends Exception> void forEach(Tuple3<String, String, String> columnNames, TriConsumer<?, ?, ?, E> action) throws E {
        forEach(columnNames, 0, size(), action);
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void forEach(Tuple3<String, String, String> columnNames, int fromRowIndex, int toRowIndex, TriConsumer<?, ?, ?, E> action)
            throws E {
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
        return toList(Object[].class, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @return
     */
    @Override
    public <T> List<T> toList(final Class<? extends T> rowClass) {
        return toList(rowClass, 0, size());
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <T> List<T> toList(final Class<? extends T> rowClass, final int fromRowIndex, final int toRowIndex) {
        return toList(rowClass, this._columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @param columnNames
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> toList(final Class<? extends T> rowClass, final Collection<String> columnNames) {
        return toList(rowClass, columnNames, 0, size());
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    public <T> List<T> toList(final Class<? extends T> rowClass, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;

        final List<Object> rowList = new ArrayList<>(toRowIndex - fromRowIndex);

        if (fromRowIndex == toRowIndex) {
            return (List<T>) rowList;
        }

        final Type<?> rowType = N.typeOf(rowClass);

        if (rowType.isObjectArray()) {
            final Class<?> componentType = rowClass.getComponentType();
            Object[] row = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                row = N.newArray(componentType, columnCount);

                for (int i = 0; i < columnCount; i++) {
                    row[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
                }

                rowList.add(row);
            }
        } else if (rowType.isList() || rowType.isSet()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowClass.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass);

            Collection<Object> row = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                row = (Collection<Object>) (isAbstractRowClass
                        ? (rowType.isList() ? new ArrayList<>(columnCount) : N.newHashSet(N.initHashCapacity(columnCount)))
                        : ((intConstructor == null) ? ClassUtil.invokeConstructor(constructor) : ClassUtil.invokeConstructor(intConstructor, columnCount)));

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

            final boolean isAbstractRowClass = Modifier.isAbstract(rowClass.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass);

            Map<String, Object> row = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                row = (Map<String, Object>) (isAbstractRowClass ? new HashMap<>(N.initHashCapacity(columnCount))
                        : (intConstructor == null ? ClassUtil.invokeConstructor(constructor)
                                : ClassUtil.invokeConstructor(intConstructor, N.initHashCapacity(columnCount))));

                for (int i = 0; i < columnCount; i++) {
                    row.put(mapKeyNames[i], _columnList.get(columnIndexes[i]).get(rowIndex));
                }

                rowList.add(row);
            }
        } else if (rowType.isEntity()) {
            for (int rowNum = fromRowIndex; rowNum < toRowIndex; rowNum++) {
                rowList.add(N.newInstance(rowClass));
            }

            final boolean ignoreUnknownProperty = columnNames == _columnNameList;
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(rowClass);
            List<Object> column = null;
            String propName = null;
            PropInfo propInfo = null;

            for (int columnIndex : columnIndexes) {
                column = _columnList.get(columnIndex);
                propName = _columnNameList.get(columnIndex);
                propInfo = entityInfo.getPropInfo(propName);

                if (propInfo == null) {
                    for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                        if (entityInfo.setPropValue(rowList.get(rowIndex - fromRowIndex), propName, column.get(rowIndex), ignoreUnknownProperty) == false) {
                            break;
                        }
                    }
                } else {
                    for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                        propInfo.setPropValue(rowList.get(rowIndex - fromRowIndex), column.get(rowIndex));
                    }
                }
            }

            if ((rowList.size() > 0) && rowList.get(0) instanceof DirtyMarker) {
                for (Object e : rowList) {
                    DirtyMarkerUtil.markDirty((DirtyMarker) e, false);
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and entity class are supported");
        }

        return (List<T>) rowList;
    }

    @Override
    public <T, E extends Exception, E2 extends Exception> List<T> toList(final Class<? extends T> rowClass,
            final Throwables.Predicate<? super String, E> columnNameFilter, final Throwables.Function<? super String, String, E2> columnNameConverter)
            throws E, E2 {
        return toList(rowClass, columnNameFilter, columnNameConverter, 0, size());
    }

    @Override
    public <T, E extends Exception, E2 extends Exception> List<T> toList(final Class<? extends T> rowClass,
            final Throwables.Predicate<? super String, E> columnNameFilter, final Throwables.Function<? super String, String, E2> columnNameConverter,
            final int fromRowIndex, final int toRowIndex) throws E, E2 {
        checkRowIndex(fromRowIndex, toRowIndex);

        if ((columnNameFilter == null || Objects.equals(columnNameFilter, Fnn.alwaysTrue()))
                && (columnNameConverter == null && Objects.equals(columnNameConverter, Fnn.identity()))) {
            return toList(rowClass, this._columnNameList, fromRowIndex, toRowIndex);
        }

        final List<Object> rowList = new ArrayList<>(toRowIndex - fromRowIndex);

        if (fromRowIndex == toRowIndex) {
            return (List<T>) rowList;
        }

        final Type<?> rowType = N.typeOf(rowClass);

        if (rowType.isObjectArray() || rowType.isList() || rowType.isSet()
                || (columnNameConverter == null && Objects.equals(columnNameConverter, Fnn.identity()))) {
            return toList(rowClass, columnNameFilter == null ? this._columnNameList : N.filter(this._columnNameList, columnNameFilter), fromRowIndex,
                    toRowIndex);
        }

        @SuppressWarnings("rawtypes")
        final Throwables.Predicate<? super String, E> columnNameFilterToBeUsed = columnNameFilter == null ? (Throwables.Predicate) Fnn.alwaysTrue()
                : columnNameFilter;
        @SuppressWarnings("rawtypes")
        final Throwables.Function<? super String, String, E2> columnNameConverterToBeUsed = columnNameConverter == null ? (Throwables.Function) Fnn.identity()
                : columnNameConverter;

        final List<Integer> columnIndexList = new ArrayList<>();

        for (int i = 0, len = this._columnNameList.size(); i < len; i++) {
            if (columnNameFilterToBeUsed.test(_columnNameList.get(i))) {
                columnIndexList.add(i);
            }
        }

        final int columnCount = columnIndexList.size();
        final int[] columnIndexes = new int[columnCount];
        final String[] newColumnNames = new String[columnCount];

        for (int i = 0; i < columnCount; i++) {
            columnIndexes[i] = columnIndexList.get(i);
            newColumnNames[i] = columnNameConverterToBeUsed.apply(_columnNameList.get(columnIndexes[i]));
        }

        if (rowType.isMap()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowClass.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass);

            Map<String, Object> row = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                row = (Map<String, Object>) (isAbstractRowClass ? new HashMap<>(N.initHashCapacity(columnCount))
                        : (intConstructor == null ? ClassUtil.invokeConstructor(constructor)
                                : ClassUtil.invokeConstructor(intConstructor, N.initHashCapacity(columnCount))));

                for (int i = 0; i < columnCount; i++) {
                    row.put(newColumnNames[i], _columnList.get(columnIndexes[i]).get(rowIndex));
                }

                rowList.add(row);
            }
        } else if (rowType.isEntity()) {
            for (int rowNum = fromRowIndex; rowNum < toRowIndex; rowNum++) {
                rowList.add(N.newInstance(rowClass));
            }

            final boolean ignoreUnknownProperty = false;
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(rowClass);
            int columnIndex = -1;
            String propName = null;
            List<Object> column = null;
            PropInfo propInfo = null;

            for (int i = 0; i < columnCount; i++) {
                columnIndex = columnIndexes[i];
                propName = newColumnNames[i];
                column = _columnList.get(columnIndex);
                propInfo = entityInfo.getPropInfo(propName);

                if (propInfo == null) {
                    for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                        if (entityInfo.setPropValue(rowList.get(rowIndex - fromRowIndex), propName, column.get(rowIndex), ignoreUnknownProperty) == false) {
                            break;
                        }
                    }
                } else {
                    for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                        propInfo.setPropValue(rowList.get(rowIndex - fromRowIndex), column.get(rowIndex));
                    }
                }
            }

            if ((rowList.size() > 0) && rowList.get(0) instanceof DirtyMarker) {
                for (Object e : rowList) {
                    DirtyMarkerUtil.markDirty((DirtyMarker) e, false);
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and entity class are supported");
        }

        return (List<T>) rowList;
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> List<T> toList(IntFunction<? extends T> rowSupplier) {
        return toList(rowSupplier, this._columnNameList);
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <T> List<T> toList(IntFunction<? extends T> rowSupplier, int fromRowIndex, int toRowIndex) {
        return toList(rowSupplier, this._columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @param columnNames
     * @return
     */
    @Override
    public <T> List<T> toList(IntFunction<? extends T> rowSupplier, Collection<String> columnNames) {
        return toList(rowSupplier, columnNames, 0, size());
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <T> List<T> toList(IntFunction<? extends T> rowSupplier, Collection<String> columnNames, int fromRowIndex, int toRowIndex) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final List<Object> rowList = new ArrayList<>(toRowIndex - fromRowIndex);
        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;

        if (fromRowIndex == toRowIndex) {
            return (List<T>) rowList;
        }

        final Class<T> rowClass = (Class<T>) rowSupplier.apply(0).getClass();
        final Type<?> rowType = N.typeOf(rowClass);

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
        } else if (rowType.isEntity()) {
            for (int rowNum = fromRowIndex; rowNum < toRowIndex; rowNum++) {
                rowList.add(rowSupplier.apply(columnCount));
            }

            final boolean ignoreUnknownProperty = columnNames == _columnNameList;
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(rowClass);
            List<Object> column = null;
            String propName = null;
            PropInfo propInfo = null;

            for (int columnIndex : columnIndexes) {
                column = _columnList.get(columnIndex);
                propName = _columnNameList.get(columnIndex);
                propInfo = entityInfo.getPropInfo(propName);

                if (propInfo == null) {
                    for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                        if (entityInfo.setPropValue(rowList.get(rowIndex - fromRowIndex), propName, column.get(rowIndex), ignoreUnknownProperty) == false) {
                            break;
                        }
                    }
                } else {
                    for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                        propInfo.setPropValue(rowList.get(rowIndex - fromRowIndex), column.get(rowIndex));
                    }
                }
            }

            if ((rowList.size() > 0) && rowList.get(0) instanceof DirtyMarker) {
                for (Object e : rowList) {
                    DirtyMarkerUtil.markDirty((DirtyMarker) e, false);
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and entity class are supported");
        }

        return (List<T>) rowList;
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
        return toMap(keyColumnName, valueColumnName, 0, size());
    }

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
    @Override
    public <K, V> Map<K, V> toMap(final String keyColumnName, final String valueColumnName, final int fromRowIndex, final int toRowIndex) {
        return toMap(keyColumnName, valueColumnName, fromRowIndex, toRowIndex, new IntFunction<Map<K, V>>() {
            @Override
            public Map<K, V> apply(int len) {
                return new LinkedHashMap<>(N.initHashCapacity(len));
            }
        });
    }

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
    @Override
    public <K, V, M extends Map<K, V>> M toMap(String keyColumnName, String valueColumnName, int fromRowIndex, int toRowIndex,
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
     * @param <K> the key type
     * @param <V> the value type
     * @param rowClass
     * @param keyColumnName
     * @param valueColumnNames
     * @return
     */
    @Override
    public <K, V> Map<K, V> toMap(final Class<? extends V> rowClass, final String keyColumnName, final Collection<String> valueColumnNames) {
        return toMap(rowClass, keyColumnName, valueColumnNames, 0, size());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param rowClass
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <K, V> Map<K, V> toMap(final Class<? extends V> rowClass, final String keyColumnName, final Collection<String> valueColumnNames,
            final int fromRowIndex, final int toRowIndex) {
        return toMap(rowClass, keyColumnName, valueColumnNames, fromRowIndex, toRowIndex, new IntFunction<Map<K, V>>() {
            @Override
            public Map<K, V> apply(int len) {
                return new LinkedHashMap<>(N.initHashCapacity(len));
            }
        });
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param rowClass
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param supplier
     * @return
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(Class<? extends V> rowClass, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex,
            int toRowIndex, IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnName(valueColumnNames);

        final Type<?> valueType = N.typeOf(rowClass);
        final int valueColumnCount = valueColumnIndexes.length;
        final Map<Object, Object> resultMap = (Map<Object, Object>) supplier.apply(toRowIndex - fromRowIndex);

        if (valueType.isObjectArray()) {
            Object[] value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = N.newArray(rowClass.getComponentType(), valueColumnCount);

                for (int i = 0; i < valueColumnCount; i++) {
                    value[i] = _columnList.get(valueColumnIndexes[i]).get(rowIndex);
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isList() || valueType.isSet()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowClass.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass);
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Collection<Object>) (isAbstractRowClass
                        ? (valueType.isList() ? new ArrayList<>(valueColumnCount) : N.newHashSet(N.initHashCapacity(valueColumnCount)))
                        : ((intConstructor == null) ? ClassUtil.invokeConstructor(constructor)
                                : ClassUtil.invokeConstructor(intConstructor, valueColumnCount)));

                for (int columIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isMap()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowClass.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass);
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Map<String, Object>) (isAbstractRowClass ? new HashMap<>(N.initHashCapacity(valueColumnCount))
                        : (intConstructor == null ? ClassUtil.invokeConstructor(constructor)
                                : ClassUtil.invokeConstructor(intConstructor, N.initHashCapacity(valueColumnCount))));

                for (int columIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columIndex), _columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isEntity()) {
            final boolean ignoreUnknownProperty = valueColumnNames == _columnNameList;
            final boolean isDirtyMarker = DirtyMarkerUtil.isDirtyMarker(rowClass);
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(rowClass);
            Object value = null;
            String propName = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = N.newInstance(rowClass);

                for (int columIndex : valueColumnIndexes) {
                    propName = _columnNameList.get(columIndex);
                    entityInfo.setPropValue(value, propName, _columnList.get(columIndex).get(rowIndex), ignoreUnknownProperty);
                }

                if (isDirtyMarker) {
                    DirtyMarkerUtil.markDirty((DirtyMarker) value, false);
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowClass.getCanonicalName() + ". Only Array, List/Set, Map and entity class are supported");
        }

        return (M) resultMap;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param rowSupplier
     * @param keyColumnName
     * @param valueColumnNames
     * @return
     */
    @Override
    public <K, V> Map<K, V> toMap(IntFunction<? extends V> rowSupplier, String keyColumnName, Collection<String> valueColumnNames) {
        return toMap(rowSupplier, keyColumnName, valueColumnNames, 0, size());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param rowSupplier
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <K, V> Map<K, V> toMap(IntFunction<? extends V> rowSupplier, String keyColumnName, Collection<String> valueColumnNames, int fromRowIndex,
            int toRowIndex) {
        return toMap(rowSupplier, keyColumnName, valueColumnNames, fromRowIndex, toRowIndex, new IntFunction<Map<K, V>>() {
            @Override
            public Map<K, V> apply(int len) {
                return new LinkedHashMap<>(N.initHashCapacity(len));
            }
        });
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param rowSupplier
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param supplier
     * @return
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(IntFunction<? extends V> rowSupplier, String keyColumnName, Collection<String> valueColumnNames,
            int fromRowIndex, int toRowIndex, IntFunction<? extends M> supplier) {
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
        } else if (valueType.isEntity()) {
            final boolean ignoreUnknownProperty = valueColumnNames == _columnNameList;
            final boolean isDirtyMarker = DirtyMarkerUtil.isDirtyMarker(rowClass);
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(rowClass);
            Object value = null;
            String propName = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = rowSupplier.apply(valueColumnCount);

                for (int columIndex : valueColumnIndexes) {
                    propName = _columnNameList.get(columIndex);
                    entityInfo.setPropValue(value, propName, _columnList.get(columIndex).get(rowIndex), ignoreUnknownProperty);
                }

                if (isDirtyMarker) {
                    DirtyMarkerUtil.markDirty((DirtyMarker) value, false);
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowClass.getCanonicalName() + ". Only Array, List/Set, Map and entity class are supported");
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
        return toMultimap(keyColumnName, valueColumnName, 0, size());
    }

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
    @Override
    public <K, E> ListMultimap<K, E> toMultimap(final String keyColumnName, final String valueColumnName, final int fromRowIndex, final int toRowIndex) {
        return toMultimap(keyColumnName, valueColumnName, fromRowIndex, toRowIndex, new IntFunction<ListMultimap<K, E>>() {
            @Override
            public ListMultimap<K, E> apply(int len) {
                return N.newLinkedListMultimap();
            }
        });
    }

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
    @Override
    public <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(String keyColumnName, String valueColumnName, int fromRowIndex,
            int toRowIndex, IntFunction<? extends M> supplier) {
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
     * @param <K> the key type
     * @param <E>
     * @param rowClass
     * @param keyColumnName
     * @param valueColumnNames
     * @return
     */
    @Override
    public <K, E> ListMultimap<K, E> toMultimap(final Class<? extends E> rowClass, final String keyColumnName, final Collection<String> valueColumnNames) {
        return toMultimap(rowClass, keyColumnName, valueColumnNames, 0, size());
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param rowClass
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <K, E> ListMultimap<K, E> toMultimap(final Class<? extends E> rowClass, final String keyColumnName, final Collection<String> valueColumnNames,
            final int fromRowIndex, final int toRowIndex) {
        return toMultimap(rowClass, keyColumnName, valueColumnNames, fromRowIndex, toRowIndex, new IntFunction<ListMultimap<K, E>>() {
            @Override
            public ListMultimap<K, E> apply(int len) {
                return N.newLinkedListMultimap();
            }
        });
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param rowClass
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param supplier
     * @return
     */
    @Override
    public <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(Class<? extends E> rowClass, String keyColumnName,
            Collection<String> valueColumnNames, int fromRowIndex, int toRowIndex, IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnName(valueColumnNames);

        final Type<?> elementType = N.typeOf(rowClass);
        final int valueColumnCount = valueColumnIndexes.length;

        final M resultMap = supplier.apply(toRowIndex - fromRowIndex);

        if (elementType.isObjectArray()) {
            Object[] value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = N.newArray(rowClass.getComponentType(), valueColumnCount);

                for (int i = 0; i < valueColumnCount; i++) {
                    value[i] = _columnList.get(valueColumnIndexes[i]).get(rowIndex);
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else if (elementType.isList() || elementType.isSet()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowClass.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass);
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Collection<Object>) (isAbstractRowClass
                        ? (elementType.isList() ? new ArrayList<>(valueColumnCount) : N.newHashSet(N.initHashCapacity(valueColumnCount)))
                        : ((intConstructor == null) ? ClassUtil.invokeConstructor(constructor)
                                : ClassUtil.invokeConstructor(intConstructor, valueColumnCount)));

                for (int columIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else if (elementType.isMap()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowClass.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass);
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Map<String, Object>) (isAbstractRowClass ? new HashMap<>(N.initHashCapacity(valueColumnCount))
                        : (intConstructor == null ? ClassUtil.invokeConstructor(constructor)
                                : ClassUtil.invokeConstructor(intConstructor, N.initHashCapacity(valueColumnCount))));

                for (int columIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columIndex), _columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else if (elementType.isEntity()) {
            final boolean ignoreUnknownProperty = valueColumnNames == _columnNameList;
            final boolean isDirtyMarker = DirtyMarkerUtil.isDirtyMarker(rowClass);
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(rowClass);
            Object value = null;
            String propName = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = N.newInstance(rowClass);

                for (int columIndex : valueColumnIndexes) {
                    propName = _columnNameList.get(columIndex);
                    entityInfo.setPropValue(value, propName, _columnList.get(columIndex).get(rowIndex), ignoreUnknownProperty);
                }

                if (isDirtyMarker) {
                    DirtyMarkerUtil.markDirty((DirtyMarker) value, false);
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowClass.getCanonicalName() + ". Only Array, List/Set, Map and entity class are supported");
        }

        return resultMap;
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param rowSupplier
     * @param keyColumnName
     * @param valueColumnNames
     * @return
     */
    @Override
    public <K, E> ListMultimap<K, E> toMultimap(IntFunction<? extends E> rowSupplier, String keyColumnName, Collection<String> valueColumnNames) {
        return toMultimap(rowSupplier, keyColumnName, valueColumnNames, 0, size());
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param rowSupplier
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <K, E> ListMultimap<K, E> toMultimap(IntFunction<? extends E> rowSupplier, String keyColumnName, Collection<String> valueColumnNames,
            int fromRowIndex, int toRowIndex) {
        return toMultimap(rowSupplier, keyColumnName, valueColumnNames, fromRowIndex, toRowIndex, new IntFunction<ListMultimap<K, E>>() {
            @Override
            public ListMultimap<K, E> apply(int len) {
                return N.newLinkedListMultimap();
            }
        });
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param rowSupplier
     * @param keyColumnName
     * @param valueColumnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param supplier
     * @return
     */
    @Override
    public <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> M toMultimap(IntFunction<? extends E> rowSupplier, String keyColumnName,
            Collection<String> valueColumnNames, int fromRowIndex, int toRowIndex, IntFunction<? extends M> supplier) {
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
        } else if (elementType.isEntity()) {
            final boolean ignoreUnknownProperty = valueColumnNames == _columnNameList;
            final boolean isDirtyMarker = DirtyMarkerUtil.isDirtyMarker(rowClass);
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(rowClass);
            Object value = null;
            String propName = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = rowSupplier.apply(valueColumnCount);

                for (int columIndex : valueColumnIndexes) {
                    propName = _columnNameList.get(columIndex);
                    entityInfo.setPropValue(value, propName, _columnList.get(columIndex).get(rowIndex), ignoreUnknownProperty);
                }

                if (isDirtyMarker) {
                    DirtyMarkerUtil.markDirty((DirtyMarker) value, false);
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (E) value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowClass.getCanonicalName() + ". Only Array, List/Set, Map and entity class are supported");
        }

        return resultMap;
    }

    @Override
    public <T> List<T> toMergedEntities(final Class<T> entityClass) {
        return toMergedEntities(entityClass, this._columnNameList);
    }

    @Override
    public <T> List<T> toMergedEntities(final Class<T> entityClass, final Collection<String> selectPropNames) {
        N.checkArgNotNull(entityClass, "entityClass");

        @SuppressWarnings("deprecation")
        final List<String> idPropNames = ClassUtil.getIdFieldNames(entityClass);

        if (N.isNullOrEmpty(idPropNames)) {
            throw new IllegalArgumentException("No id property defined in class: " + entityClass);
        }

        return toMergedEntities(entityClass, idPropNames, selectPropNames);
    }

    @Override
    public <T> List<T> toMergedEntities(final Class<T> entityClass, final String idPropName) {
        return toMergedEntities(entityClass, idPropName, this._columnNameList);
    }

    @Override
    public <T> List<T> toMergedEntities(final Class<T> entityClass, final String idPropName, final Collection<String> selectPropNames) {
        return toMergedEntities(entityClass, N.asList(idPropName), selectPropNames);
    }

    @Override
    public <T> List<T> toMergedEntities(final Class<T> entityClass, final List<String> idPropNames, final Collection<String> selectPropNames) {
        return toMergedEntities(entityClass, idPropNames, selectPropNames, false);
    }

    @SuppressWarnings("rawtypes")
    private <T> List<T> toMergedEntities(final Class<T> entityClass, final List<String> idPropNames, Collection<String> selectPropNames,
            final boolean returnAllList) {
        N.checkArgNotNull(entityClass, "entityClass");
        N.checkArgNotNull(idPropNames, "idPropNames");
        N.checkArgument(this._columnNameList.containsAll(idPropNames), "Some id properties {} are not found in DataSet: {}", idPropNames, this._columnNameList);
        N.checkArgument(N.isNullOrEmpty(selectPropNames) || selectPropNames == this._columnNameList || this._columnNameList.containsAll(selectPropNames),
                "Some select properties {} are not found in DataSet: {}", selectPropNames, this._columnNameList);

        selectPropNames = N.isNullOrEmpty(selectPropNames) ? this._columnNameList : selectPropNames;

        final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);

        final int rowCount = size();
        final int columnCount = _columnList.size();
        final int[] idColumnIndexes = getColumnIndexes(idPropNames);
        final boolean ignoreUnknownProperty = selectPropNames == this._columnNameList;

        final Object[] resultEntities = new Object[rowCount];
        final Map<Object, Object> idEntityMap = new LinkedHashMap<>(N.min(64, rowCount));
        Object entity = null;

        if (idColumnIndexes.length == 1) {
            final List<Object> idColumn = _columnList.get(idColumnIndexes[0]);
            Object rowKey = null;
            Object key = null;

            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                key = idColumn.get(rowIndex);

                if (key == null) {
                    continue;
                }

                rowKey = getHashKey(key);
                entity = idEntityMap.get(rowKey);

                if (entity == null) {
                    entity = N.newInstance(entityClass);
                    idEntityMap.put(rowKey, entity);
                }

                resultEntities[rowIndex] = entity;
            }
        } else {
            final int idColumnCount = idColumnIndexes.length;

            Object[] keyRow = Objectory.createObjectArray(idColumnCount);
            Wrapper<Object[]> rowKey = null;
            boolean isAllKeyNull = true;

            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                isAllKeyNull = true;

                for (int i = 0; i < idColumnCount; i++) {
                    keyRow[i] = _columnList.get(idColumnIndexes[i]).get(rowIndex);

                    if (keyRow[i] != null) {
                        isAllKeyNull = false;
                    }
                }

                if (isAllKeyNull) {
                    continue;
                }

                rowKey = Wrapper.of(keyRow);
                entity = idEntityMap.get(rowKey);

                if (entity == null) {
                    entity = N.newInstance(entityClass);
                    idEntityMap.put(rowKey, entity);

                    keyRow = Objectory.createObjectArray(idColumnCount);
                }

                resultEntities[rowIndex] = entity;
            }

            if (keyRow != null) {
                Objectory.recycle(keyRow);
                keyRow = null;
            }
        }

        final List<Collection<Object>> listPropValuesToDeduplicate = new ArrayList<>();

        try {
            final Set<String> mergedPropNames = new HashSet<>();
            List<Object> curColumn = null;
            int curColumnIndex = 0;
            PropInfo propInfo = null;

            for (String propName : selectPropNames) {
                if (mergedPropNames.contains(propName)) {
                    continue;
                }

                curColumnIndex = checkColumnName(propName);
                curColumn = _columnList.get(curColumnIndex);

                propInfo = entityInfo.getPropInfo(propName);

                if (propInfo != null) {
                    for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                        if (resultEntities[rowIndex] != null) {
                            propInfo.setPropValue(resultEntities[rowIndex], curColumn.get(rowIndex));
                        }
                    }
                } else {
                    final int idx = propName.indexOf(PROP_NAME_SEPARATOR);

                    if (idx <= 0) {
                        if (ignoreUnknownProperty) {
                            continue;
                        } else {
                            throw new IllegalArgumentException("Property " + propName + " is not found in class: " + entityClass);
                        }
                    } else {
                        final String realPropName = propName.substring(0, idx);
                        propInfo = entityInfo.getPropInfo(realPropName);

                        if (propInfo == null) {
                            if (ignoreUnknownProperty) {
                                continue;
                            } else {
                                throw new IllegalArgumentException("Property " + propName + " is not found in class: " + entityClass);
                            }
                        } else {
                            final Type<?> propEntityType = propInfo.type.isCollection() ? propInfo.type.getElementType() : propInfo.type;

                            if (!propEntityType.isEntity()) {
                                throw new UnsupportedOperationException("Property: " + propInfo.name + " in class: " + entityClass + " is not an entity type");
                            }

                            final Class<?> propEntityClass = propEntityType.clazz();
                            @SuppressWarnings("deprecation")
                            final List<String> propEntityIdPropNames = ClassUtil.getIdFieldNames(propEntityClass);
                            final List<String> newPropEntityIdNames = N.isNullOrEmpty(propEntityIdPropNames) ? new ArrayList<>() : propEntityIdPropNames;
                            final List<String> newTmpColumnNameList = new ArrayList<>();
                            final List<List<Object>> newTmpColumnList = new ArrayList<>();

                            String columnName = null;
                            String newColumnName = null;

                            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                                columnName = this._columnNameList.get(columnIndex);

                                if (columnName.length() > idx && columnName.charAt(idx) == PROP_NAME_SEPARATOR && columnName.startsWith(realPropName)) {
                                    newColumnName = columnName.substring(idx + 1);
                                    newTmpColumnNameList.add(newColumnName);
                                    newTmpColumnList.add(_columnList.get(columnIndex));

                                    mergedPropNames.add(columnName);

                                    if (N.isNullOrEmpty(propEntityIdPropNames) && newColumnName.indexOf(PROP_NAME_SEPARATOR) < 0) {
                                        newPropEntityIdNames.add(newColumnName);
                                    }
                                }
                            }

                            final RowDataSet tmp = new RowDataSet(newTmpColumnNameList, newTmpColumnList);
                            final List<?> propValueList = tmp.toMergedEntities(propEntityClass, newPropEntityIdNames, tmp._columnNameList, true);

                            if (propInfo.type.isCollection()) {
                                Collection<Object> c = null;

                                for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                                    if (resultEntities[rowIndex] == null || propValueList.get(rowIndex) == null) {
                                        continue;
                                    }

                                    c = propInfo.getPropValue(resultEntities[rowIndex]);

                                    if (c == null) {
                                        c = (Collection<Object>) N.newInstance(propInfo.clazz);
                                        propInfo.setPropValue(resultEntities[rowIndex], c);

                                        if (!(c instanceof Set)) {
                                            listPropValuesToDeduplicate.add(c);
                                        }
                                    }

                                    c.add(propValueList.get(rowIndex));
                                }
                            } else {
                                for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                                    if (resultEntities[rowIndex] == null || propValueList.get(rowIndex) == null) {
                                        continue;
                                    }

                                    propInfo.setPropValue(resultEntities[rowIndex], propValueList.get(rowIndex));
                                }
                            }
                        }
                    }
                }
            }

            if (N.notNullOrEmpty(listPropValuesToDeduplicate)) {
                for (Collection<Object> list : listPropValuesToDeduplicate) {
                    N.removeDuplicates(list);
                }
            }

            return returnAllList ? (List<T>) N.asList(resultEntities) : new ArrayList<>((Collection<T>) idEntityMap.values());
        } finally {
            if (idColumnIndexes.length > 1) {
                for (Wrapper<Object[]> e : ((Map<Wrapper<Object[]>, Object>) ((Map) idEntityMap)).keySet()) {
                    Objectory.recycle(e.value());
                }
            }
        }
    }

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
        return toJSON(this._columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public String toJSON(final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        final BufferedJSONWriter writer = Objectory.createBufferedJSONWriter();

        try {
            toJSON(writer, columnNames, fromRowIndex, toRowIndex);

            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     *
     * @param out
     */
    @Override
    public void toJSON(final File out) {
        toJSON(out, 0, size());
    }

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toJSON(final File out, final int fromRowIndex, final int toRowIndex) {
        toJSON(out, this._columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toJSON(final File out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) throws UncheckedIOException {
        OutputStream os = null;

        try {
            if (out.exists() == false) {
                out.createNewFile();
            }

            os = new FileOutputStream(out);

            toJSON(os, columnNames, fromRowIndex, toRowIndex);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param out
     */
    @Override
    public void toJSON(final OutputStream out) {
        toJSON(out, 0, size());
    }

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toJSON(final OutputStream out, final int fromRowIndex, final int toRowIndex) {
        toJSON(out, this._columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toJSON(final OutputStream out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) throws UncheckedIOException {
        final BufferedJSONWriter writer = Objectory.createBufferedJSONWriter(out);

        try {
            toJSON(writer, columnNames, fromRowIndex, toRowIndex);

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     *
     * @param out
     */
    @Override
    public void toJSON(final Writer out) {
        toJSON(out, 0, size());
    }

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toJSON(final Writer out, final int fromRowIndex, final int toRowIndex) {
        toJSON(out, this._columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toJSON(final Writer out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) throws UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;

        final char[][] charArrayOfColumnNames = new char[columnCount][];

        for (int i = 0; i < columnCount; i++) {
            charArrayOfColumnNames[i] = ("\"" + _columnNameList.get(columnIndexes[i]) + "\"").toCharArray();
        }

        final boolean isBufferedWriter = out instanceof BufferedJSONWriter;
        final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) out : Objectory.createBufferedJSONWriter(out);

        try {
            bw.write(WD._BRACKET_L);

            Type<Object> type = null;
            Object element = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                if (rowIndex > fromRowIndex) {
                    bw.write(N.ELEMENT_SEPARATOR_CHAR_ARRAY);
                }

                bw.write(WD._BRACE_L);

                for (int i = 0; i < columnCount; i++) {
                    element = _columnList.get(columnIndexes[i]).get(rowIndex);

                    type = element == null ? null : N.typeOf(element.getClass());

                    if (i > 0) {
                        bw.write(N.ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    bw.write(charArrayOfColumnNames[i]);
                    bw.write(WD._COLON);

                    if (type == null) {
                        bw.write(NULL_CHAR_ARRAY);
                    } else {
                        if (type.isSerializable()) {
                            type.writeCharacter(bw, element, jsc);
                        } else {
                            // jsonParser.serialize(bw, element, jsc);

                            try {
                                jsonParser.serialize(bw, element, jsc);
                            } catch (Exception e) {
                                // ignore.

                                strType.writeCharacter(bw, N.toString(element), jsc);
                            }
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
        return toXML(rowElementName, 0, size());
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public String toXML(final int fromRowIndex, final int toRowIndex) {
        return toXML(ROW, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param rowElementName
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public String toXML(final String rowElementName, final int fromRowIndex, final int toRowIndex) {
        return toXML(rowElementName, this._columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public String toXML(final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        return toXML(ROW, columnNames, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param rowElementName
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public String toXML(final String rowElementName, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        final BufferedXMLWriter writer = Objectory.createBufferedXMLWriter();

        try {
            toXML(writer, rowElementName, columnNames, fromRowIndex, toRowIndex);

            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     *
     * @param out
     */
    @Override
    public void toXML(final File out) {
        toXML(out, 0, size());
    }

    /**
     *
     * @param out
     * @param rowElementName
     */
    @Override
    public void toXML(final File out, final String rowElementName) {
        toXML(out, rowElementName, 0, size());
    }

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toXML(final File out, final int fromRowIndex, final int toRowIndex) {
        toXML(out, ROW, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param rowElementName
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toXML(final File out, final String rowElementName, final int fromRowIndex, final int toRowIndex) {
        toXML(out, rowElementName, this._columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toXML(final File out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        toXML(out, ROW, columnNames, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param rowElementName
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toXML(final File out, final String rowElementName, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex)
            throws UncheckedIOException {
        OutputStream os = null;

        try {
            if (out.exists() == false) {
                out.createNewFile();
            }

            os = new FileOutputStream(out);

            toXML(os, rowElementName, columnNames, fromRowIndex, toRowIndex);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param out
     */
    @Override
    public void toXML(final OutputStream out) {
        toXML(out, 0, size());
    }

    /**
     *
     * @param out
     * @param rowElementName
     */
    @Override
    public void toXML(final OutputStream out, final String rowElementName) {
        toXML(out, rowElementName, 0, size());
    }

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toXML(final OutputStream out, final int fromRowIndex, final int toRowIndex) {
        toXML(out, ROW, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param rowElementName
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toXML(final OutputStream out, final String rowElementName, final int fromRowIndex, final int toRowIndex) {
        toXML(out, rowElementName, this._columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toXML(final OutputStream out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        toXML(out, ROW, columnNames, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param rowElementName
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toXML(final OutputStream out, final String rowElementName, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex)
            throws UncheckedIOException {
        final BufferedXMLWriter writer = Objectory.createBufferedXMLWriter(out);

        try {
            toXML(writer, rowElementName, columnNames, fromRowIndex, toRowIndex);

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     *
     * @param out
     */
    @Override
    public void toXML(final Writer out) {
        toXML(out, 0, size());
    }

    /**
     *
     * @param out
     * @param rowElementName
     */
    @Override
    public void toXML(final Writer out, final String rowElementName) {
        toXML(out, rowElementName, 0, size());
    }

    /**
     *
     * @param out
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toXML(final Writer out, final int fromRowIndex, final int toRowIndex) {
        toXML(out, ROW, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param rowElementName
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toXML(final Writer out, final String rowElementName, final int fromRowIndex, final int toRowIndex) {
        toXML(out, rowElementName, this._columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toXML(final Writer out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        toXML(out, ROW, columnNames, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param out
     * @param rowElementName
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toXML(final Writer out, final String rowElementName, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex)
            throws UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;

        final char[] rowElementNameHead = ("<" + rowElementName + ">").toCharArray();
        final char[] rowElementNameTail = ("</" + rowElementName + ">").toCharArray();

        final char[][] charArrayOfColumnNames = new char[columnCount][];

        for (int i = 0; i < columnCount; i++) {
            charArrayOfColumnNames[i] = _columnNameList.get(columnIndexes[i]).toCharArray();
        }

        final boolean isBufferedWriter = out instanceof BufferedXMLWriter;
        final BufferedXMLWriter bw = isBufferedWriter ? (BufferedXMLWriter) out : Objectory.createBufferedXMLWriter(out);

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
                    } else {
                        if (type.isSerializable()) {
                            type.writeCharacter(bw, element, xsc);
                        } else {
                            // xmlParser.serialize(bw, element, xsc);

                            try {
                                xmlParser.serialize(bw, element, xsc);
                            } catch (Exception e) {
                                // ignore.

                                strType.writeCharacter(bw, N.toString(element), xsc);
                            }
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

    @Override
    public String toCSV() {
        return toCSV(this.columnNameList(), 0, size());
    }

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public String toCSV(final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        return toCSV(columnNames, fromRowIndex, toRowIndex, true, true);
    }

    /**
     *
     * @param writeTitle
     * @param quoted
     * @return
     */
    @Override
    public String toCSV(final boolean writeTitle, final boolean quoted) {
        return toCSV(columnNameList(), 0, size(), writeTitle, quoted);
    }

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param writeTitle
     * @param quoted
     * @return
     */
    @Override
    public String toCSV(final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex, final boolean writeTitle, final boolean quoted) {
        final BufferedWriter bw = Objectory.createBufferedWriter();

        try {
            toCSV(bw, columnNames, fromRowIndex, toRowIndex, writeTitle, quoted);

            return bw.toString();
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     *
     * @param out
     */
    @Override
    public void toCSV(final File out) {
        toCSV(out, _columnNameList, 0, size());
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toCSV(final File out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        toCSV(out, columnNames, fromRowIndex, toRowIndex, true, true);
    }

    /**
     *
     * @param out
     * @param writeTitle
     * @param quoted
     */
    @Override
    public void toCSV(final File out, final boolean writeTitle, final boolean quoted) {
        toCSV(out, _columnNameList, 0, size(), writeTitle, quoted);
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param writeTitle
     * @param quoted
     */
    @Override
    public void toCSV(final File out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex, final boolean writeTitle,
            final boolean quoted) {
        OutputStream os = null;

        try {
            if (!out.exists()) {
                out.createNewFile();
            }

            os = new FileOutputStream(out);

            toCSV(os, columnNames, fromRowIndex, toRowIndex, writeTitle, quoted);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param out
     */
    @Override
    public void toCSV(final OutputStream out) {
        toCSV(out, _columnNameList, 0, size());
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toCSV(final OutputStream out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        toCSV(out, columnNames, fromRowIndex, toRowIndex, true, true);
    }

    /**
     *
     * @param out
     * @param writeTitle
     * @param quoted
     */
    @Override
    public void toCSV(final OutputStream out, final boolean writeTitle, final boolean quoted) {
        toCSV(out, _columnNameList, 0, size(), writeTitle, quoted);
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param writeTitle
     * @param quoted
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toCSV(final OutputStream out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex, final boolean writeTitle,
            final boolean quoted) throws UncheckedIOException {
        Writer writer = null;

        try {
            writer = new OutputStreamWriter(out);

            toCSV(writer, columnNames, fromRowIndex, toRowIndex, writeTitle, quoted);

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param out
     */
    @Override
    public void toCSV(final Writer out) {
        toCSV(out, _columnNameList, 0, size());
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     */
    @Override
    public void toCSV(final Writer out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        toCSV(out, columnNames, fromRowIndex, toRowIndex, true, true);
    }

    /**
     *
     * @param out
     * @param writeTitle
     * @param quoted
     */
    @Override
    public void toCSV(final Writer out, final boolean writeTitle, final boolean quoted) {
        toCSV(out, _columnNameList, 0, size(), writeTitle, quoted);
    }

    /**
     *
     * @param out
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param writeTitle
     * @param quoted
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void toCSV(final Writer out, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex, final boolean writeTitle,
            final boolean quoted) throws UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;

        final JSONSerializationConfig config = JSC.create();
        config.setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

        if (quoted) {
            config.setQuoteMapKey(true);
            config.setQuotePropName(true);
            config.setCharQuotation(WD._QUOTATION_D);
            config.setStringQuotation(WD._QUOTATION_D);
        } else {
            config.setQuoteMapKey(false);
            config.setQuotePropName(false);
            config.setCharQuotation((char) 0);
            config.setStringQuotation((char) 0);
        }

        final boolean isBufferedWriter = out instanceof BufferedJSONWriter;
        final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) out : Objectory.createBufferedJSONWriter(out);

        try {
            if (writeTitle) {
                for (int i = 0; i < columnCount; i++) {
                    if (i > 0) {
                        bw.write(N.ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    bw.write(getColumnName(columnIndexes[i]));
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
                        bw.write(N.ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    element = _columnList.get(columnIndexes[i]).get(rowIndex);

                    if (element == null) {
                        bw.write(NULL_CHAR_ARRAY);
                    } else {
                        type = element == null ? null : N.typeOf(element.getClass());

                        if (type.isSerializable()) {
                            type.writeCharacter(bw, element, config);
                        } else {
                            // strType.writeCharacters(bw,
                            // jsonParser.serialize(element, config), config);

                            try {
                                strType.writeCharacter(bw, jsonParser.serialize(element, config), config);
                            } catch (Exception e) {
                                // ignore.

                                strType.writeCharacter(bw, N.toString(element), config);
                            }
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

    /**
     *
     * @param columnName
     * @return
     */
    @Override
    public DataSet groupBy(final String columnName) {
        return groupBy(columnName, (Function<?, ?>) null);
    }

    /**
     *
     * @param <T>
     * @param columnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public <T> DataSet groupBy(final String columnName, String aggregateResultColumnName, String aggregateOnColumnName, final Collector<T, ?, ?> collector) {
        return groupBy(columnName, (Function<?, ?>) null, aggregateResultColumnName, aggregateOnColumnName, collector);
    }

    @Override
    public DataSet groupBy(String columnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowClass) {
        final List<Object> keyColumn = getColumn(columnName);
        final List<Object> valueColumn = toList(rowClass, aggregateOnColumnNames);

        final Map<Object, List<Object>> map = new LinkedHashMap<>(N.min(9, size()));
        final List<Object> keyList = new ArrayList<>(N.min(9, size()));
        Object key = null;
        List<Object> val = null;

        for (int i = 0, size = keyColumn.size(); i < size; i++) {
            key = getHashKey(keyColumn.get(i));
            val = map.get(key);

            if (val == null) {
                val = new ArrayList<>();
                map.put(key, val);

                keyList.add(keyColumn.get(i));
            }

            val.add(valueColumn.get(i));
        }

        final List<String> newColumnNameList = N.asList(columnName, aggregateResultColumnName);
        final List<List<Object>> newColumnList = N.asList(keyList, new ArrayList<>(map.values()));

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param columnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    @Override
    public DataSet groupBy(final String columnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            final Collector<? super Object[], ?, ?> collector) {
        return groupBy(columnName, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param columnName
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     * @throws E the e
     */
    @Override
    public <U, E extends Exception> DataSet groupBy(final String columnName, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            final Throwables.Function<? super DisposableObjArray, U, E> rowMapper, final Collector<? super U, ?, ?> collector) throws E {
        return groupBy(columnName, (Function<?, ?>) null, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector);
    }

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
    @Override
    public <T, E extends Exception> DataSet groupBy(final String columnName, String aggregateResultColumnName, String aggregateOnColumnName,
            final Throwables.Function<Stream<T>, ?, E> func) throws E {
        return groupBy(columnName, (Function<?, ?>) null, aggregateResultColumnName, aggregateOnColumnName, func);
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param columnName
     * @param keyMapper
     * @return
     * @throws E the e
     */
    @Override
    public <K, E extends Exception> DataSet groupBy(final String columnName, Throwables.Function<K, ?, E> keyMapper) throws E {
        final int columnIndex = checkColumnName(columnName);

        final int size = size();
        final int newColumnCount = 1;
        final List<String> newColumnNameList = new ArrayList<>(newColumnCount);
        newColumnNameList.add(columnName);

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

            if (keySet.add(getHashKey(keyMapper2.apply(value)))) {
                keyColumn.add(value);
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

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
    @Override
    public <K, T, E extends Exception> DataSet groupBy(final String columnName, Throwables.Function<K, ?, E> keyMapper, String aggregateResultColumnName,
            String aggregateOnColumnName, final Collector<T, ?, ?> collector) throws E {
        final int columnIndex = checkColumnName(columnName);
        final int aggOnColumnIndex = checkColumnName(aggregateOnColumnName);

        if (N.equals(columnName, aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicated Property name: " + aggregateResultColumnName);
        }

        final int size = size();
        final int newColumnCount = 2;
        final List<String> newColumnNameList = new ArrayList<>(newColumnCount);
        newColumnNameList.add(columnName);
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
            key = getHashKey(keyMapper2.apply(value));

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
    @Override
    public <K, T, E extends Exception, E2 extends Exception> DataSet groupBy(final String columnName, Throwables.Function<K, ?, E> keyMapper,
            String aggregateResultColumnName, String aggregateOnColumnName, final Throwables.Function<Stream<T>, ?, E2> func) throws E, E2 {
        final RowDataSet result = (RowDataSet) groupBy(columnName, keyMapper, aggregateResultColumnName, aggregateOnColumnName, Collectors.toList());
        final List<Object> column = result._columnList.get(result.getColumnIndex(aggregateResultColumnName));

        for (int i = 0, len = column.size(); i < len; i++) {
            column.set(i, func.apply(Stream.of((List<T>) column.get(i))));
        }

        return result;
    }

    @Override
    public <K, E extends Exception> DataSet groupBy(String columnName, com.landawn.abacus.util.Throwables.Function<K, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowClass) throws E {
        final Throwables.Function<Object, ?, E> keyMapper2 = (Throwables.Function<Object, ?, E>) (keyMapper == null ? Fn.identity() : keyMapper);

        final List<Object> keyColumn = getColumn(columnName);
        final List<Object> valueColumn = toList(rowClass, aggregateOnColumnNames);

        final Map<Object, List<Object>> map = new LinkedHashMap<>(N.min(9, size()));
        final List<Object> keyList = new ArrayList<>(N.min(9, size()));
        Object key = null;
        List<Object> val = null;

        for (int i = 0, size = keyColumn.size(); i < size; i++) {
            key = getHashKey(keyMapper2.apply(keyColumn.get(i)));
            val = map.get(key);

            if (val == null) {
                val = new ArrayList<>();
                map.put(key, val);

                keyList.add(keyColumn.get(i));
            }

            val.add(valueColumn.get(i));
        }

        final List<String> newColumnNameList = N.asList(columnName, aggregateResultColumnName);
        final List<List<Object>> newColumnList = N.asList(keyList, new ArrayList<>(map.values()));

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    private static final Function<DisposableObjArray, Object[]> CLONE = new Function<DisposableObjArray, Object[]>() {
        @Override
        public Object[] apply(DisposableObjArray t) {
            return t.clone();
        }
    };

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
    @Override
    public <K, E extends Exception> DataSet groupBy(final String columnName, Throwables.Function<K, ?, E> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, final Collector<? super Object[], ?, ?> collector) throws E {
        return groupBy(columnName, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <K> the key type
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param columnName
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
    public <K, U, E extends Exception, E2 extends Exception> DataSet groupBy(final String columnName, Throwables.Function<K, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, final Throwables.Function<? super DisposableObjArray, U, E2> rowMapper,
            final Collector<? super U, ?, ?> collector) throws E, E2 {
        final int columnIndex = checkColumnName(columnName);
        final int[] aggOnColumnIndexes = checkColumnName(aggregateOnColumnNames);

        if (N.equals(columnName, aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicated Property name: " + aggregateResultColumnName);
        }

        N.checkArgNotNull(rowMapper, "rowMapper");
        N.checkArgNotNull(collector, "collector");

        final int size = size();
        final int aggOnColumnCount = aggOnColumnIndexes.length;
        final int newColumnCount = 2;
        final List<String> newColumnNameList = new ArrayList<>(newColumnCount);
        newColumnNameList.add(columnName);
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
        final BiConsumer<Object, U> accumulator = (BiConsumer<Object, U>) collector.accumulator();
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
            key = getHashKey(keyMapper2.apply(value));

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
     * @param columnNames
     * @return
     */
    @Override
    public DataSet groupBy(final Collection<String> columnNames) {
        return groupBy(columnNames, (Function<? super DisposableObjArray, ?>) null);
    }

    /**
     *
     * @param <T>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public <T> DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName,
            final Collector<T, ?, ?> collector) {
        return groupBy(columnNames, (Function<? super DisposableObjArray, ?>) null, aggregateResultColumnName, aggregateOnColumnName, collector);
    }

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
    @Override
    public <T, E extends Exception> DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, String aggregateOnColumnName,
            final Throwables.Function<Stream<T>, ?, E> func) throws E {
        return groupBy(columnNames, (Function<? super DisposableObjArray, ?>) null, aggregateResultColumnName, aggregateOnColumnName, func);
    }

    @Override
    public DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, Class<?> rowClass) {
        N.checkArgNotNullOrEmpty(columnNames, "columnNames");
        N.checkArgNotNullOrEmpty(aggregateOnColumnNames, "aggregateOnColumnNames");

        if (columnNames.size() == 1) {
            return groupBy(columnNames.iterator().next(), aggregateResultColumnName, aggregateOnColumnNames, rowClass);
        }

        final int size = size();
        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;
        final int newColumnCount = columnIndexes.length + 1;
        final List<String> newColumnNameList = N.newArrayList(newColumnCount);
        newColumnNameList.addAll(columnNames);
        newColumnNameList.add(aggregateResultColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            newColumnList.add(new ArrayList<>());

            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final List<Object> valueColumnList = toList(rowClass, aggregateOnColumnNames);

        final Map<Wrapper<Object[]>, List<Object>> keyRowMap = new LinkedHashMap<>(N.min(9, size()));

        Object[] keyRow = Objectory.createObjectArray(columnCount);
        Wrapper<Object[]> key = null;
        List<Object> val = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < newColumnCount; i++) {
                keyRow[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            key = Wrapper.of(keyRow);
            val = keyRowMap.get(key);

            if (val == null) {
                val = new ArrayList<>();
                keyRowMap.put(key, val);

                for (int i = 0; i < newColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                keyRow = Objectory.createObjectArray(columnCount);
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
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    @Override
    public DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            final Collector<? super Object[], ?, ?> collector) {
        return groupBy(columnNames, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     * @throws E the e
     */
    @Override
    public <U, E extends Exception> DataSet groupBy(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            final Throwables.Function<? super DisposableObjArray, U, E> rowMapper, final Collector<? super U, ?, ?> collector) throws E {
        return groupBy(columnNames, (Function<? super DisposableObjArray, ?>) null, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector);
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
    public <E extends Exception> DataSet groupBy(final Collection<String> columnNames, final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper)
            throws E {
        N.checkArgNotNullOrEmpty(columnNames, "columnNames");

        final boolean isNullOrIdentityKeyMapper = keyMapper == null || keyMapper == Fn.identity();

        if (columnNames.size() == 1 && isNullOrIdentityKeyMapper) {
            return this.groupBy(columnNames.iterator().next(), keyMapper);
        }

        final int size = size();
        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;
        final int newColumnCount = columnIndexes.length;
        final List<String> newColumnNameList = N.newArrayList(columnNames);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final Set<Object> keyRowSet = N.newHashSet();
        Object[] keyRow = Objectory.createObjectArray(columnCount);
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(keyRow);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < newColumnCount; i++) {
                keyRow[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            if (isNullOrIdentityKeyMapper) {
                if (keyRowSet.add(Wrapper.of(keyRow))) {
                    for (int i = 0; i < newColumnCount; i++) {
                        newColumnList.get(i).add(keyRow[i]);
                    }

                    keyRow = Objectory.createObjectArray(columnCount);
                }
            } else {
                if (keyRowSet.add(getHashKey(keyMapper.apply(disposableArray)))) {
                    for (int i = 0; i < newColumnCount; i++) {
                        newColumnList.get(i).add(keyRow[i]);
                    }
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
     * @param <T>
     * @param <E>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> DataSet groupBy(Collection<String> columnNames, final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, String aggregateOnColumnName, final Collector<T, ?, ?> collector) throws E {
        N.checkArgNotNullOrEmpty(columnNames, "columnNames");

        if (N.notNullOrEmpty(columnNames) && columnNames.contains(aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicated Property name: " + aggregateResultColumnName);
        }

        final boolean isNullOrIdentityKeyMapper = keyMapper == null || keyMapper == Fn.identity();

        if (columnNames.size() == 1 && isNullOrIdentityKeyMapper) {
            return groupBy(columnNames.iterator().next(), keyMapper, aggregateResultColumnName, aggregateOnColumnName, collector);
        }

        final int size = size();
        final int[] columnIndexes = checkColumnName(columnNames);
        final int aggOnColumnIndex = checkColumnName(aggregateOnColumnName);
        final int columnCount = columnIndexes.length;
        final int newColumnCount = columnIndexes.length + 1;
        final List<String> newColumnNameList = new ArrayList<>(columnNames);
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
        Object[] keyRow = Objectory.createObjectArray(columnCount);
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(keyRow);
        Object key = null;
        Integer collectorRowIndex = -1;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < columnCount; i++) {
                keyRow[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            key = isNullOrIdentityKeyMapper ? Wrapper.of(keyRow) : getHashKey(keyMapper.apply(disposableArray));
            collectorRowIndex = keyRowIndexMap.get(key);

            if (collectorRowIndex == null) {
                collectorRowIndex = aggResultColumn.size();
                keyRowIndexMap.put(key, collectorRowIndex);
                aggResultColumn.add(supplier.get());

                for (int i = 0; i < columnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                keyRow = Objectory.createObjectArray(columnCount);
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

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param func
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    @Override
    public <T, E extends Exception, E2 extends Exception> DataSet groupBy(Collection<String> columnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, String aggregateOnColumnName,
            final Throwables.Function<Stream<T>, ?, E2> func) throws E, E2 {
        final RowDataSet result = (RowDataSet) groupBy(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnName, Collectors.toList());
        final List<Object> column = result._columnList.get(result.getColumnIndex(aggregateResultColumnName));

        for (int i = 0, len = column.size(); i < len; i++) {
            column.set(i, func.apply(Stream.of((List<T>) column.get(i))));
        }

        return result;
    }

    @Override
    public <E extends Exception> DataSet groupBy(Collection<String> columnNames,
            com.landawn.abacus.util.Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Class<?> rowClass) throws E {
        N.checkArgNotNullOrEmpty(columnNames, "columnNames");
        N.checkArgNotNullOrEmpty(aggregateOnColumnNames, "aggregateOnColumnNames");

        final boolean isNullOrIdentityKeyMapper = keyMapper == null || keyMapper == Fn.identity();

        if (isNullOrIdentityKeyMapper) {
            if (columnNames.size() == 1) {
                return groupBy(columnNames.iterator().next(), aggregateResultColumnName, aggregateOnColumnNames, rowClass);
            } else {
                return groupBy(columnNames, aggregateResultColumnName, aggregateOnColumnNames, rowClass);
            }
        }

        final int size = size();
        final int[] columnIndexes = checkColumnName(columnNames);
        final int columnCount = columnIndexes.length;
        final int newColumnCount = columnIndexes.length + 1;
        final List<String> newColumnNameList = N.newArrayList(newColumnCount);
        newColumnNameList.addAll(columnNames);
        newColumnNameList.add(aggregateResultColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            newColumnList.add(new ArrayList<>());

            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final List<Object> valueColumnList = toList(rowClass, aggregateOnColumnNames);

        final Map<Object, List<Object>> keyRowMap = N.newLinkedHashMap();
        final Object[] keyRow = Objectory.createObjectArray(columnCount);
        final DisposableObjArray keyDisposableArray = DisposableObjArray.wrap(keyRow);

        Object key = null;
        List<Object> val = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < newColumnCount; i++) {
                keyRow[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            key = getHashKey(keyMapper.apply(keyDisposableArray));
            val = keyRowMap.get(key);

            if (val == null) {
                val = new ArrayList<>();
                keyRowMap.put(key, val);

                for (int i = 0; i < newColumnCount; i++) {
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
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet groupBy(Collection<String> columnNames, Throwables.Function<? super DisposableObjArray, ?, E> keyMapper,
            String aggregateResultColumnName, Collection<String> aggregateOnColumnNames, final Collector<? super Object[], ?, ?> collector) throws E {
        return groupBy(aggregateOnColumnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param columnNames
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
    public <U, E extends Exception, E2 extends Exception> DataSet groupBy(Collection<String> columnNames,
            Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            final Throwables.Function<? super DisposableObjArray, U, E2> rowMapper, final Collector<? super U, ?, ?> collector) throws E, E2 {
        N.checkArgNotNullOrEmpty(columnNames, "columnNames");

        if (N.notNullOrEmpty(columnNames) && columnNames.contains(aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicated Property name: " + aggregateResultColumnName);
        }

        N.checkArgNotNull(rowMapper, "rowMapper");
        N.checkArgNotNull(collector, "collector");

        final boolean isNullOrIdentityKeyMapper = keyMapper == null || keyMapper == Fn.identity();

        if (columnNames.size() == 1 && isNullOrIdentityKeyMapper) {
            return groupBy(columnNames.iterator().next(), keyMapper, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector);
        }

        final int size = size();
        final int[] columnIndexes = checkColumnName(columnNames);
        final int[] aggOnColumnIndexes = checkColumnName(aggregateOnColumnNames);
        final int columnCount = columnIndexes.length;
        final int newColumnCount = columnIndexes.length + 1;
        final List<String> newColumnNameList = new ArrayList<>(columnNames);
        newColumnNameList.add(aggregateResultColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, U> accumulator = (BiConsumer<Object, U>) collector.accumulator();
        final Function<Object, Object> finisher = (Function<Object, Object>) collector.finisher();

        final int aggOnColumnCount = aggOnColumnIndexes.length;
        final List<Object> aggResultColumn = newColumnList.get(newColumnList.size() - 1);
        final Map<Object, Integer> keyRowIndexMap = new HashMap<>();
        Object[] keyRow = Objectory.createObjectArray(columnCount);
        final DisposableObjArray keyDisposableArray = DisposableObjArray.wrap(keyRow);
        final Object[] aggOnRow = new Object[aggOnColumnCount];
        final DisposableObjArray aggOnRowDisposableArray = DisposableObjArray.wrap(aggOnRow);
        Object key = null;
        Integer collectorRowIndex = -1;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < columnCount; i++) {
                keyRow[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            key = isNullOrIdentityKeyMapper ? Wrapper.of(keyRow) : getHashKey(keyMapper.apply(keyDisposableArray));
            collectorRowIndex = keyRowIndexMap.get(key);

            if (collectorRowIndex == null) {
                collectorRowIndex = aggResultColumn.size();
                keyRowIndexMap.put(key, collectorRowIndex);
                aggResultColumn.add(supplier.get());

                for (int i = 0; i < columnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                keyRow = Objectory.createObjectArray(columnCount);
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
     * @param columnNames
     * @return
     */
    @Override
    public Stream<DataSet> rollup(final Collection<String> columnNames) {
        return Stream.of(Iterables.rollup(columnNames)).reversed().map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return groupBy(columnNames);
            }
        });
    }

    /**
     *
     * @param <T>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public <T> Stream<DataSet> rollup(final Collection<String> columnNames, final String aggregateResultColumnName, final String aggregateOnColumnName,
            final Collector<T, ?, ?> collector) {
        return Stream.of(Iterables.rollup(columnNames)).reversed().map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return groupBy(columnNames, aggregateResultColumnName, aggregateOnColumnName, collector);
            }
        });
    }

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
    @Override
    public <T, E extends Exception> Stream<DataSet> rollup(final Collection<String> columnNames, final String aggregateResultColumnName,
            final String aggregateOnColumnName, final Throwables.Function<Stream<T>, ?, E> func) {
        return Stream.of(Iterables.rollup(columnNames)).reversed().map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, aggregateResultColumnName, aggregateOnColumnName, func);
                    }
                });
            }
        });
    }

    @Override
    public Stream<DataSet> rollup(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Class<?> rowClass) {
        return Stream.of(Iterables.rollup(columnNames)).reversed().map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, aggregateResultColumnName, aggregateOnColumnNames, rowClass);
                    }
                });
            }
        });
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
     * @param <U>
     * @param <E>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     */
    @Override
    public <U, E extends Exception> Stream<DataSet> rollup(final Collection<String> columnNames, final String aggregateResultColumnName,
            final Collection<String> aggregateOnColumnNames, final Throwables.Function<? super DisposableObjArray, U, E> rowMapper,
            final Collector<? super U, ?, ?> collector) {
        return Stream.of(Iterables.rollup(columnNames)).reversed().map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector);
                    }
                });
            }
        });
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper
     * @return
     */
    @Override
    public <E extends Exception> Stream<DataSet> rollup(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper) {
        return Stream.of(Iterables.rollup(columnNames)).reversed().map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, keyMapper);
                    }
                });
            }
        });
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public <T, E extends Exception> Stream<DataSet> rollup(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, final String aggregateResultColumnName, final String aggregateOnColumnName,
            final Collector<T, ?, ?> collector) {
        return Stream.of(Iterables.rollup(columnNames)).reversed().map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnName, collector);
                    }
                });
            }
        });
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param func
     * @return
     */
    @Override
    public <T, E extends Exception, E2 extends Exception> Stream<DataSet> rollup(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, final String aggregateResultColumnName, final String aggregateOnColumnName,
            final Throwables.Function<Stream<T>, ?, E2> func) {
        return Stream.of(Iterables.rollup(columnNames)).reversed().map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnName, func);
                    }
                });
            }
        });
    }

    @Override
    public <E extends Exception> Stream<DataSet> rollup(Collection<String> columnNames,
            com.landawn.abacus.util.Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Class<?> rowClass) {
        return Stream.of(Iterables.rollup(columnNames)).reversed().map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, rowClass);
                    }
                });
            }
        });
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    @Override
    public <E extends Exception> Stream<DataSet> rollup(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, final String aggregateResultColumnName,
            final Collection<String> aggregateOnColumnNames, final Collector<? super Object[], ?, ?> collector) {
        return rollup(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     */
    @Override
    public <U, E extends Exception, E2 extends Exception> Stream<DataSet> rollup(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, final String aggregateResultColumnName,
            final Collection<String> aggregateOnColumnNames, final Throwables.Function<? super DisposableObjArray, U, E2> rowMapper,
            final Collector<? super U, ?, ?> collector) {
        return Stream.of(Iterables.rollup(columnNames)).reversed().map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector);
                    }
                });
            }
        });
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @Override
    public Stream<DataSet> cube(final Collection<String> columnNames) {
        return cubeSet(columnNames).map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return groupBy(columnNames);
            }
        });
    }

    /**
     *
     * @param <T>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public <T> Stream<DataSet> cube(final Collection<String> columnNames, final String aggregateResultColumnName, final String aggregateOnColumnName,
            final Collector<T, ?, ?> collector) {
        return cubeSet(columnNames).map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return groupBy(columnNames, aggregateResultColumnName, aggregateOnColumnName, collector);
            }
        });
    }

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
    @Override
    public <T, E extends Exception> Stream<DataSet> cube(final Collection<String> columnNames, final String aggregateResultColumnName,
            final String aggregateOnColumnName, final Throwables.Function<Stream<T>, ?, E> func) {
        return cubeSet(columnNames).map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, aggregateResultColumnName, aggregateOnColumnName, func);
                    }
                });
            }
        });
    }

    @Override
    public Stream<DataSet> cube(Collection<String> columnNames, String aggregateResultColumnName, Collection<String> aggregateOnColumnNames,
            Class<?> rowClass) {
        return cubeSet(columnNames).map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, aggregateResultColumnName, aggregateOnColumnNames, rowClass);
                    }
                });
            }
        });
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
     * @param <U>
     * @param <E>
     * @param columnNames
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     */
    @Override
    public <U, E extends Exception> Stream<DataSet> cube(final Collection<String> columnNames, final String aggregateResultColumnName,
            final Collection<String> aggregateOnColumnNames, final Throwables.Function<? super DisposableObjArray, U, E> rowMapper,
            final Collector<? super U, ?, ?> collector) {
        return cubeSet(columnNames).map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector);
                    }
                });
            }
        });
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper
     * @return
     */
    @Override
    public <E extends Exception> Stream<DataSet> cube(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper) {
        return cubeSet(columnNames).map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, keyMapper);
                    }
                });
            }
        });
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param collector
     * @return
     */
    @Override
    public <T, E extends Exception> Stream<DataSet> cube(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, final String aggregateResultColumnName, final String aggregateOnColumnName,
            final Collector<T, ?, ?> collector) {
        return cubeSet(columnNames).map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnName, collector);
                    }
                });
            }
        });
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnName
     * @param func
     * @return
     */
    @Override
    public <T, E extends Exception, E2 extends Exception> Stream<DataSet> cube(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, final String aggregateResultColumnName, final String aggregateOnColumnName,
            final Throwables.Function<Stream<T>, ?, E2> func) {
        return cubeSet(columnNames).map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnName, func);
                    }
                });
            }
        });
    }

    @Override
    public <E extends Exception> Stream<DataSet> cube(Collection<String> columnNames,
            com.landawn.abacus.util.Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, String aggregateResultColumnName,
            Collection<String> aggregateOnColumnNames, Class<?> rowClass) {
        return cubeSet(columnNames).map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, rowClass);
                    }
                });
            }
        });
    }

    /**
     *
     * @param <E>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param collector
     * @return
     */
    @Override
    public <E extends Exception> Stream<DataSet> cube(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, final String aggregateResultColumnName,
            final Collection<String> aggregateOnColumnNames, final Collector<? super Object[], ?, ?> collector) {
        return cube(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, CLONE, collector);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param columnNames
     * @param keyMapper
     * @param aggregateResultColumnName
     * @param aggregateOnColumnNames
     * @param rowMapper
     * @param collector
     * @return
     */
    @Override
    public <U, E extends Exception, E2 extends Exception> Stream<DataSet> cube(final Collection<String> columnNames,
            final Throwables.Function<? super DisposableObjArray, ?, E> keyMapper, final String aggregateResultColumnName,
            final Collection<String> aggregateOnColumnNames, final Throwables.Function<? super DisposableObjArray, U, E2> rowMapper,
            final Collector<? super U, ?, ?> collector) {
        return cubeSet(columnNames).map(new Function<Collection<String>, DataSet>() {
            @Override
            public DataSet apply(final Collection<String> columnNames) {
                return Try.call(new Callable<DataSet>() {
                    @Override
                    public DataSet call() throws Exception {
                        return groupBy(columnNames, keyMapper, aggregateResultColumnName, aggregateOnColumnNames, rowMapper, collector);
                    }
                });
            }
        });
    }

    private static final Function<Set<String>, Integer> TO_SIZE_FUNC = new Function<Set<String>, Integer>() {
        @Override
        public Integer apply(Set<String> t) {
            return t.size();
        }
    };

    private static final Consumer<List<Set<String>>> REVERSE_ACTION = new Consumer<List<Set<String>>>() {
        @Override
        public void accept(List<Set<String>> t) {
            N.reverse(t);
        }
    };

    /**
     *
     * @param columnNames
     * @return
     */
    private Stream<Set<String>> cubeSet(final Collection<String> columnNames) {
        return Stream.of(Iterables.powerSet(N.newLinkedHashSet(columnNames)))
                .groupByToEntry(TO_SIZE_FUNC)
                .values()
                .onEach(REVERSE_ACTION)
                .flattMap(Fn.<List<Set<String>>> identity())
                .reversed();
    }

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
        sortBy(columnNames, (Comparator<? super Object[]>) null);
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
        parallelSortBy(columnNames, (Comparator<? super Object[]>) null);
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
    @SuppressWarnings("rawtypes")
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

        final Comparator<Object> cmp2 = (Comparator<Object>) cmp;

        final Comparator<Indexed<Object>> pairCmp = cmp == null ? (Comparator) new Comparator<Indexed<Comparable>>() {
            @Override
            public int compare(final Indexed<Comparable> o1, final Indexed<Comparable> o2) {
                return N.compare(o1.value(), o2.value());
            }
        } : new Comparator<Indexed<Object>>() {
            @Override
            public int compare(final Indexed<Object> o1, final Indexed<Object> o2) {
                return cmp2.compare(o1.value(), o2.value());
            }
        };

        sort(arrayOfPair, pairCmp, isParallelSort);
    }

    /**
     *
     * @param columnNames
     * @param cmp
     * @param isParallelSort
     */
    @SuppressWarnings("rawtypes")
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

        final Comparator<Indexed<Object[]>> pairCmp = cmp == null ? (Comparator) new Comparator<Indexed<Object[]>>() {
            @Override
            public int compare(final Indexed<Object[]> o1, final Indexed<Object[]> o2) {
                return MULTI_COLUMN_COMPARATOR.compare(o1.value(), o2.value());
            }
        } : new Comparator<Indexed<Object[]>>() {
            @Override
            public int compare(final Indexed<Object[]> o1, final Indexed<Object[]> o2) {
                return cmp.compare(o1.value(), o2.value());
            }
        };

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

        final Comparator<Indexed<Comparable>> pairCmp = Comparators.comparingBy(new Function<Indexed<Comparable>, Comparable>() {
            @Override
            public Comparable apply(Indexed<Comparable> t) {
                return t.value();
            }
        });

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
        final int columnCount = _columnNameList.size();
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
        return topBy(columnName, n, Comparators.naturalOrder());
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

        final Comparator<Object> cmp2 = (Comparator<Object>) cmp;
        @SuppressWarnings("rawtypes")
        final Comparator<Indexed<Object>> pairCmp = cmp == null ? (Comparator) new Comparator<Indexed<Comparable>>() {
            @Override
            public int compare(final Indexed<Comparable> o1, final Indexed<Comparable> o2) {
                return N.compare(o1.value(), o2.value());
            }
        } : new Comparator<Indexed<Object>>() {
            @Override
            public int compare(final Indexed<Object> o1, final Indexed<Object> o2) {
                return cmp2.compare(o1.value(), o2.value());
            }
        };

        final List<Object> orderByColumn = _columnList.get(columnIndex);

        return top(n, pairCmp, new IntFunction<Object>() {
            @Override
            public Object apply(int rowIndex) {
                return orderByColumn.get(rowIndex);
            }
        });
    }

    /**
     *
     * @param columnNames
     * @param n
     * @return
     */
    @Override
    public DataSet topBy(final Collection<String> columnNames, final int n) {
        return topBy(columnNames, n, (Comparator<? super Object[]>) null);
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

        @SuppressWarnings("rawtypes")
        final Comparator<Indexed<Object[]>> pairCmp = cmp == null ? (Comparator) new Comparator<Indexed<Object[]>>() {
            @Override
            public int compare(final Indexed<Object[]> o1, final Indexed<Object[]> o2) {
                return MULTI_COLUMN_COMPARATOR.compare(o1.value(), o2.value());
            }
        } : new Comparator<Indexed<Object[]>>() {
            @Override
            public int compare(final Indexed<Object[]> o1, final Indexed<Object[]> o2) {
                return cmp.compare(o1.value(), o2.value());
            }
        };

        final List<Object[]> keyRowList = new ArrayList<>(n);
        final int sortByColumnCount = sortByColumnIndexes.length;

        final DataSet result = top(n, pairCmp, new IntFunction<Object[]>() {
            @Override
            public Object[] apply(int rowIndex) {
                final Object[] keyRow = Objectory.createObjectArray(sortByColumnCount);
                keyRowList.add(keyRow);

                for (int i = 0; i < sortByColumnCount; i++) {
                    keyRow[i] = _columnList.get(sortByColumnIndexes[i]).get(rowIndex);
                }

                return keyRow;
            }
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

        final Comparator<Indexed<Comparable>> pairCmp = Comparators.comparingBy(new Function<Indexed<Comparable>, Comparable>() {
            @Override
            public Comparable apply(Indexed<Comparable> t) {
                return t.value();
            }
        });

        final int sortByColumnCount = columnIndexes.length;
        final Object[] keyRow = new Object[sortByColumnCount];
        final DisposableObjArray disposableObjArray = DisposableObjArray.wrap(keyRow);

        return top(n, pairCmp, new IntFunction<Comparable>() {
            @Override
            public Comparable apply(int rowIndex) {

                for (int i = 0; i < sortByColumnCount; i++) {
                    keyRow[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
                }

                return keyMapper.apply(disposableObjArray);
            }
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

        N.sort(arrayOfPair, new Comparator<Indexed<Object>>() {
            @Override
            public int compare(final Indexed<Object> o1, final Indexed<Object> o2) {
                return o1.index() - o2.index();
            }
        });

        final int columnCount = _columnNameList.size();
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

        final Properties<String, Object> newProperties = N.isNullOrEmpty(_properties) ? null : _properties.copy();

        return new RowDataSet(newColumnNameList, newColumnList, newProperties);
    }

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
     * @param <K> the key type
     * @param <E>
     * @param columnName
     * @param keyMapper
     * @return
     * @throws E the e
     */
    @Override
    public <K, E extends Exception> DataSet distinctBy(final String columnName, final Throwables.Function<K, ?, E> keyMapper) throws E {
        final int columnIndex = checkColumnName(columnName);

        final int size = size();
        final int columnCount = _columnNameList.size();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final Properties<String, Object> newProperties = N.isNullOrEmpty(_properties) ? null : _properties.copy();

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, newProperties);
        }

        final Throwables.Function<Object, ?, E> keyMapper2 = (Throwables.Function<Object, ?, E>) keyMapper;
        final Set<Object> rowSet = N.newHashSet();
        Object key = null;
        Object value = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            value = _columnList.get(columnIndex).get(rowIndex);
            key = getHashKey(keyMapper2 == null ? value : keyMapper2.apply(value));

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
        return distinctBy(columnNames, (Function<? super DisposableObjArray, ?>) null);
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

        final int columnCount = _columnNameList.size();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final Properties<String, Object> newProperties = N.isNullOrEmpty(_properties) ? null : _properties.copy();

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
            } else {
                if (rowSet.add(getHashKey(keyMapper.apply(disposableArray)))) {
                    for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                        newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                    }
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
        return filter(this._columnNameList, fromRowIndex, toRowIndex, filter, max);
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
        return filter(columnNames, 0, size(), filter, max);
    }

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
    @Override
    public <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, int fromRowIndex, int toRowIndex, Throwables.BiPredicate<?, ?, E> filter)
            throws E {
        return filter(columnNames, fromRowIndex, toRowIndex, filter, size());
    }

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
    @Override
    public <E extends Exception> DataSet filter(Tuple2<String, String> columnNames, int fromRowIndex, int toRowIndex, Throwables.BiPredicate<?, ?, E> filter,
            int max) throws E {
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter);

        @SuppressWarnings("rawtypes")
        final Throwables.BiPredicate<Object, Object, E> filter2 = (Throwables.BiPredicate) filter;
        final int size = size();
        final int columnCount = _columnNameList.size();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        final Properties<String, Object> newProperties = N.isNullOrEmpty(_properties) ? null : _properties.copy();

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
        return filter(columnNames, 0, size(), filter, max);
    }

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
    @Override
    public <E extends Exception> DataSet filter(Tuple3<String, String, String> columnNames, int fromRowIndex, int toRowIndex,
            Throwables.TriPredicate<?, ?, ?, E> filter) throws E {
        return filter(columnNames, fromRowIndex, toRowIndex, filter, size());
    }

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
    @Override
    public <E extends Exception> DataSet filter(final Tuple3<String, String, String> columnNames, final int fromRowIndex, final int toRowIndex,
            final Throwables.TriPredicate<?, ?, ?, E> filter, final int max) throws E {
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(columnNames._3));

        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter);

        @SuppressWarnings("rawtypes")
        final TriPredicate<Object, Object, Object, E> filter2 = (Throwables.TriPredicate) filter;
        final int size = size();
        final int columnCount = _columnNameList.size();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        final Properties<String, Object> newProperties = N.isNullOrEmpty(_properties) ? null : _properties.copy();

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
     * @param <T>
     * @param <E>
     * @param columnName
     * @param filter
     * @return
     * @throws E the e
     */
    @Override
    public <T, E extends Exception> DataSet filter(final String columnName, final Throwables.Predicate<T, E> filter) throws E {
        return filter(columnName, filter, size());
    }

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
    @Override
    public <T, E extends Exception> DataSet filter(final String columnName, Throwables.Predicate<T, E> filter, int max) throws E {
        return filter(columnName, 0, size(), filter, max);
    }

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
    @Override
    public <T, E extends Exception> DataSet filter(final String columnName, final int fromRowIndex, final int toRowIndex,
            final Throwables.Predicate<T, E> filter) throws E {
        return filter(columnName, fromRowIndex, toRowIndex, filter, size());
    }

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
    @Override
    public <T, E extends Exception> DataSet filter(final String columnName, int fromRowIndex, int toRowIndex, Throwables.Predicate<T, E> filter, int max)
            throws E {
        final int filterColumnIndex = checkColumnName(columnName);
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter, "filter");
        N.checkArgNotNegative(max, "max");

        final int size = size();
        final int columnCount = _columnNameList.size();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        final Properties<String, Object> newProperties = N.isNullOrEmpty(_properties) ? null : _properties.copy();

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, newProperties);
        }

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            if (filter.test((T) _columnList.get(filterColumnIndex).get(rowIndex))) {
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
        return filter(columnNames, 0, size(), filter, max);
    }

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
    @Override
    public <E extends Exception> DataSet filter(final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex,
            final Throwables.Predicate<? super DisposableObjArray, E> filter) throws E {
        return filter(columnNames, fromRowIndex, toRowIndex, filter, size());
    }

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
    @Override
    public <E extends Exception> DataSet filter(Collection<String> columnNames, int fromRowIndex, int toRowIndex,
            Throwables.Predicate<? super DisposableObjArray, E> filter, int max) throws E {
        final int[] filterColumnIndexes = checkColumnName(columnNames);
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter, "filter");
        N.checkArgNotNegative(max, "max");

        final int size = size();
        final int columnCount = _columnNameList.size();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        final Properties<String, Object> newProperties = N.isNullOrEmpty(_properties) ? null : _properties.copy();

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
     * @param func
     * @param newColumnName
     * @param copyingColumnName
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet map(final String fromColumnName, final Throwables.Function<?, ?, E> func, final String newColumnName,
            final String copyingColumnName) throws E {
        return map(fromColumnName, func, newColumnName, Array.asList(copyingColumnName));
    }

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
    @Override
    public <E extends Exception> DataSet map(final String fromColumnName, final Throwables.Function<?, ?, E> func, final String newColumnName,
            final Collection<String> copyingColumnNames) throws E {
        N.checkArgNotNull(func, "func");
        final int fromColumnIndex = checkColumnName(fromColumnName);
        final int[] copyingColumnIndices = N.isNullOrEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.Function<Object, Object, E> mapper = (Throwables.Function<Object, Object, E>) func;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        for (Object val : _columnList.get(fromColumnIndex)) {
            mappedColumn.add(mapper.apply(val));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.notNullOrEmpty(copyingColumnNames)) {
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
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet map(final Tuple2<String, String> fromColumnNames, final BiFunction<?, ?, ?, E> func, final String newColumnName,
            final Collection<String> copyingColumnNames) throws E {
        N.checkArgNotNull(func, "func");
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final int[] copyingColumnIndices = N.isNullOrEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.BiFunction<Object, Object, Object, E> mapper = (Throwables.BiFunction<Object, Object, Object, E>) func;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            mappedColumn.add(mapper.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex)));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.notNullOrEmpty(copyingColumnNames)) {
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
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet map(final Tuple3<String, String, String> fromColumnNames, final TriFunction<?, ?, ?, ?, E> func,
            final String newColumnName, final Collection<String> copyingColumnNames) throws E {
        N.checkArgNotNull(func, "func");
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<Object> fromColumn3 = _columnList.get(checkColumnName(fromColumnNames._3));

        final int[] copyingColumnIndices = N.isNullOrEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.TriFunction<Object, Object, Object, Object, E> mapper = (Throwables.TriFunction<Object, Object, Object, Object, E>) func;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            mappedColumn.add(mapper.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex), fromColumn3.get(rowIndex)));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.notNullOrEmpty(copyingColumnNames)) {
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
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet map(final Collection<String> fromColumnNames, final Throwables.Function<DisposableObjArray, ?, E> func,
            final String newColumnName, final Collection<String> copyingColumnNames) throws E {
        N.checkArgNotNull(func, "func");
        final int[] fromColumnIndices = checkColumnName(fromColumnNames);
        final int[] copyingColumnIndices = N.isNullOrEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.Function<DisposableObjArray, Object, E> mapper = (Throwables.Function<DisposableObjArray, Object, E>) func;
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

            mappedColumn.add(mapper.apply(disposableArray));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.notNullOrEmpty(copyingColumnNames)) {
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
     * @param func
     * @param newColumnName
     * @param copyingColumnName
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet flatMap(final String fromColumnName, final Throwables.Function<?, ? extends Collection<?>, E> func,
            final String newColumnName, final String copyingColumnName) throws E {
        return flatMap(fromColumnName, func, newColumnName, Array.asList(copyingColumnName));
    }

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
    @Override
    public <E extends Exception> DataSet flatMap(final String fromColumnName, final Throwables.Function<?, ? extends Collection<?>, E> func,
            final String newColumnName, final Collection<String> copyingColumnNames) throws E {
        N.checkArgNotNull(func, "func");
        final int fromColumnIndex = checkColumnName(fromColumnName);
        final int[] copyingColumnIndices = N.isNullOrEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.Function<Object, Collection<Object>, E> mapper = (Throwables.Function<Object, Collection<Object>, E>) func;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.isNullOrEmpty(copyingColumnNames)) {
            Collection<Object> c = null;

            for (Object val : _columnList.get(fromColumnIndex)) {
                c = mapper.apply(val);

                if (N.notNullOrEmpty(c)) {
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
                c = mapper.apply(fromColumn.get(rowIndex));

                if (N.notNullOrEmpty(c)) {
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
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet flatMap(final Tuple2<String, String> fromColumnNames, final BiFunction<?, ?, ? extends Collection<?>, E> func,
            final String newColumnName, final Collection<String> copyingColumnNames) throws E {
        N.checkArgNotNull(func, "func");
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));

        final int[] copyingColumnIndices = N.isNullOrEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.BiFunction<Object, Object, Collection<Object>, E> mapper = (Throwables.BiFunction<Object, Object, Collection<Object>, E>) func;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.isNullOrEmpty(copyingColumnNames)) {
            Collection<Object> c = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                c = mapper.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex));

                if (N.notNullOrEmpty(c)) {
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
                c = mapper.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex));

                if (N.notNullOrEmpty(c)) {
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
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet flatMap(final Tuple3<String, String, String> fromColumnNames,
            final TriFunction<?, ?, ?, ? extends Collection<?>, E> func, final String newColumnName, final Collection<String> copyingColumnNames) throws E {
        N.checkArgNotNull(func, "func");
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<Object> fromColumn3 = _columnList.get(checkColumnName(fromColumnNames._3));

        final int[] copyingColumnIndices = N.isNullOrEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.TriFunction<Object, Object, Object, Collection<Object>, E> mapper = (Throwables.TriFunction<Object, Object, Object, Collection<Object>, E>) func;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.isNullOrEmpty(copyingColumnNames)) {
            Collection<Object> c = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                c = mapper.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex), fromColumn3.get(rowIndex));

                if (N.notNullOrEmpty(c)) {
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
                c = mapper.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex), fromColumn3.get(rowIndex));

                if (N.notNullOrEmpty(c)) {
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
     * @param func
     * @param newColumnName
     * @param copyingColumnNames
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> DataSet flatMap(final Collection<String> fromColumnNames,
            final Throwables.Function<DisposableObjArray, ? extends Collection<?>, E> func, final String newColumnName,
            final Collection<String> copyingColumnNames) throws E {
        N.checkArgNotNull(func, "func");
        final int[] fromColumnIndices = checkColumnName(fromColumnNames);
        final int[] copyingColumnIndices = N.isNullOrEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(copyingColumnNames);

        final Throwables.Function<DisposableObjArray, Collection<Object>, E> mapper = (Throwables.Function<DisposableObjArray, Collection<Object>, E>) func;
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

        if (N.isNullOrEmpty(copyingColumnNames)) {
            Collection<Object> c = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                for (int j = 0; j < fromColumnCount; j++) {
                    tmpRow[j] = _columnList.get(fromColumnIndices[j]).get(rowIndex);
                }

                c = mapper.apply(disposableArray);

                if (N.notNullOrEmpty(c)) {
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

                c = mapper.apply(disposableArray);

                if (N.notNullOrEmpty(c)) {
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

    @Override
    public DataSet copy() {
        return copy(_columnNameList, 0, size());
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @Override
    public DataSet copy(final Collection<String> columnNames) {
        return copy(columnNames, 0, size());
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public DataSet copy(final int fromRowIndex, final int toRowIndex) {
        return copy(_columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public DataSet copy(final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        return copy(columnNames, fromRowIndex, toRowIndex, true);
    }

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param copyProperties
     * @return
     */
    private RowDataSet copy(final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex, final boolean copyProperties) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final List<String> newColumnNameList = new ArrayList<>(columnNames);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnNameList.size());

        if (fromRowIndex == 0 && toRowIndex == size()) {
            for (String columnName : newColumnNameList) {
                newColumnList.add(new ArrayList<>(_columnList.get(checkColumnName(columnName))));
            }
        } else {
            for (String columnName : newColumnNameList) {
                newColumnList.add(new ArrayList<>(_columnList.get(checkColumnName(columnName)).subList(fromRowIndex, toRowIndex)));
            }
        }

        final Properties<String, Object> newProperties = copyProperties && N.notNullOrEmpty(_properties) ? _properties.copy() : null;

        return new RowDataSet(newColumnNameList, newColumnList, newProperties);
    }

    @Override
    public DataSet clone() {
        return clone(this._isFrozen);
    }

    /**
     *
     * @param freeze
     * @return
     */
    @Override
    public DataSet clone(boolean freeze) {
        RowDataSet dataSet = null;

        if (kryoParser != null) {
            dataSet = kryoParser.clone(this);
        } else {
            dataSet = jsonParser.deserialize(RowDataSet.class, jsonParser.serialize(this));
        }

        dataSet._isFrozen = freeze;
        return dataSet;
    }

    /**
     *
     * @param right
     * @param columnName
     * @param refColumnName
     * @return
     */
    @Override
    public DataSet innerJoin(final DataSet right, final String columnName, final String refColumnName) {
        final Map<String, String> onColumnNames = N.asMap(columnName, refColumnName);

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
     * @param newColumnClass
     * @return
     */
    @Override
    public DataSet innerJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnClass) {
        return join(right, onColumnNames, newColumnName, newColumnClass, false);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnClass
     * @param collSupplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public DataSet innerJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnClass,
            final IntFunction<? extends Collection> collSupplier) {
        return join(right, onColumnNames, newColumnName, newColumnClass, collSupplier, false);
    }

    /**
     *
     * @param right
     * @param columnName
     * @param refColumnName
     * @return
     */
    @Override
    public DataSet leftJoin(final DataSet right, final String columnName, final String refColumnName) {
        final Map<String, String> onColumnNames = N.asMap(columnName, refColumnName);

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
            final int rightJoinColumnIndex = checkRefColumnName(right, onColumnEntry.getValue());
            final List<String> rightColumnNames = getRightColumnNames(right, onColumnEntry.getValue());
            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, rightColumnNames);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = getHashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);
            List<Integer> rightRowIndexList = null;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = getHashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                join(newColumnList, right, isLeftJoin, leftRowIndex, rightRowIndexList, rightColumnIndexes);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];
            final List<String> rightColumnNames = new ArrayList<>(right.columnNameList());

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames, rightColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, rightColumnNames);

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
        if (N.notNullOrEmpty(rightRowIndexList)) {
            for (int rightRowIndex : rightRowIndexList) {
                for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                    newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
                }

                for (int i = 0, leftColumnLength = _columnNameList.size(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                    newColumnList.get(leftColumnLength + i).add(right.get(rightRowIndex, rightColumnIndexes[i]));
                }
            }
        } else if (isLeftJoin) {
            for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            for (int i = 0, leftColumnLength = _columnNameList.size(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                newColumnList.get(leftColumnLength + i).add(null);
            }
        }
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnClass
     * @param isLeftJoin
     * @return
     */
    private DataSet join(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnClass,
            final boolean isLeftJoin) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRefColumnName(right, onColumnEntry.getValue());
            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = getHashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            List<Integer> rightRowIndexList = null;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = getHashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                join(newColumnList, right, isLeftJoin, newColumnClass, newColumnIndex, leftRowIndex, rightRowIndexList);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + 1);
            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

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

                join(newColumnList, right, isLeftJoin, newColumnClass, newColumnIndex, leftRowIndex, rightRowIndexList);
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
     * @param newColumnClass
     * @param newColumnIndex
     * @param leftRowIndex
     * @param rightRowIndexList
     */
    private void join(final List<List<Object>> newColumnList, final DataSet right, final boolean isLeftJoin, final Class<?> newColumnClass,
            final int newColumnIndex, int leftRowIndex, List<Integer> rightRowIndexList) {
        if (N.notNullOrEmpty(rightRowIndexList)) {
            for (int rightRowIndex : rightRowIndexList) {
                for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                    newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
                }

                newColumnList.get(newColumnIndex).add(right.getRow(newColumnClass, rightRowIndex));
            }
        } else if (isLeftJoin) {
            for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
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
        if (N.isNullOrEmpty(onColumnNames)) {
            throw new IllegalArgumentException("The joining column names can't be null or empty");
        }
    }

    /**
     * Check ref column name.
     *
     * @param right
     * @param refColumnName
     * @return
     */
    private int checkRefColumnName(final DataSet right, final String refColumnName) {
        final int rightJoinColumnIndex = right.getColumnIndex(refColumnName);

        if (rightJoinColumnIndex < 0) {
            throw new IllegalArgumentException("The specified column: " + refColumnName + " is not included in the right DataSet " + right.columnNameList());
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
            throw new IllegalArgumentException("The new column: " + newColumnName + " is already included in this DataSet: " + _columnNameList);
        }
    }

    /**
     * Gets the right column names.
     *
     * @param right
     * @param refColumnName
     * @return
     */
    private List<String> getRightColumnNames(final DataSet right, final String refColumnName) {
        final List<String> rightColumnNames = new ArrayList<>(right.columnNameList());

        if (this.containsColumn(refColumnName)) {
            rightColumnNames.remove(refColumnName);
        }

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
            final Map<String, String> onColumnNames, final List<String> rightColumnNames) {
        int i = 0;
        for (Map.Entry<String, String> entry : onColumnNames.entrySet()) {
            leftJoinColumnIndexes[i] = checkColumnName(entry.getKey());
            rightJoinColumnIndexes[i] = right.getColumnIndex(entry.getValue());

            if (rightJoinColumnIndexes[i] < 0) {
                throw new IllegalArgumentException(
                        "The specified column: " + entry.getValue() + " is not included in the right DataSet " + right.columnNameList());
            }

            if (entry.getKey().equals(entry.getValue())) {
                rightColumnNames.remove(entry.getValue());
            }

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
                        "The specified column: " + entry.getValue() + " is not included in the right DataSet " + right.columnNameList());
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
    private void initNewColumnList(final List<String> newColumnNameList, final List<List<Object>> newColumnList, final List<String> rightColumnNames) {
        for (int i = 0, len = _columnNameList.size(); i < len; i++) {
            newColumnNameList.add(_columnNameList.get(i));
            newColumnList.add(new ArrayList<>());
        }

        for (String rightColumnName : rightColumnNames) {
            if (this.containsColumn(rightColumnName)) {
                throw new IllegalArgumentException(
                        "The column in right DataSet: " + rightColumnName + " is already included in this DataSet: " + _columnNameList);
            }

            newColumnNameList.add(rightColumnName);
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
        for (int i = 0, len = _columnNameList.size(); i < len; i++) {
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
     * @param newColumnClass
     * @return
     */
    @Override
    public DataSet leftJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnClass) {
        return join(right, onColumnNames, newColumnName, newColumnClass, true);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnClass
     * @param collSupplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public DataSet leftJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnClass,
            final IntFunction<? extends Collection> collSupplier) {
        return join(right, onColumnNames, newColumnName, newColumnClass, collSupplier, true);
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnClass
     * @param collSupplier
     * @param isLeftJoin
     * @return
     */
    @SuppressWarnings("rawtypes")
    private DataSet join(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnClass,
            final IntFunction<? extends Collection> collSupplier, final boolean isLeftJoin) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);
        N.checkArgNotNull(collSupplier);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRefColumnName(right, onColumnEntry.getValue());

            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            List<Integer> rightRowIndexList = null;
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = getHashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = getHashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                join(newColumnList, right, isLeftJoin, newColumnClass, collSupplier, newColumnIndex, leftRowIndex, rightRowIndexList);
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + 1);
            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

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

                join(newColumnList, right, isLeftJoin, newColumnClass, collSupplier, newColumnIndex, leftRowIndex, rightRowIndexList);
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
     * @param newColumnClass
     * @param collSupplier
     * @param newColumnIndex
     * @param leftRowIndex
     * @param rightRowIndexList
     */
    @SuppressWarnings("rawtypes")
    private void join(final List<List<Object>> newColumnList, final DataSet right, final boolean isLeftJoin, final Class<?> newColumnClass,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, int leftRowIndex, List<Integer> rightRowIndexList) {
        if (N.notNullOrEmpty(rightRowIndexList)) {
            for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

            for (int rightRowIndex : rightRowIndexList) {
                coll.add(right.getRow(newColumnClass, rightRowIndex));
            }

            newColumnList.get(newColumnIndex).add(coll);
        } else if (isLeftJoin) {
            for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            newColumnList.get(newColumnIndex).add(null);
        }
    }

    /**
     *
     * @param right
     * @param columnName
     * @param refColumnName
     * @return
     */
    @Override
    public DataSet rightJoin(final DataSet right, final String columnName, final String refColumnName) {
        final Map<String, String> onColumnNames = N.asMap(columnName, refColumnName);

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
            final int rightJoinColumnIndex = checkRefColumnName(right, onColumnEntry.getValue());
            final List<String> leftColumnNames = getLeftColumnNamesForRightJoin(onColumnEntry.getValue());
            final List<String> rightColumnNames = right.columnNameList();

            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, right, leftColumnNames, rightColumnNames);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            List<Integer> leftRowIndexList = null;
            Object hashKey = null;

            for (int leftRowIndex = 0, leftDataSetSize = this.size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                hashKey = getHashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final int[] leftColumnIndexes = this.getColumnIndexes(leftColumnNames);
            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = getHashKey(rightJoinColumn.get(rightRowIndex));
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

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, right, leftColumnNames, rightColumnNames);

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
        if (N.notNullOrEmpty(leftRowIndexList)) {
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
            final Map<String, String> onColumnNames, final List<String> leftColumnNames) {
        int i = 0;
        for (Map.Entry<String, String> entry : onColumnNames.entrySet()) {
            leftJoinColumnIndexes[i] = checkColumnName(entry.getKey());
            rightJoinColumnIndexes[i] = right.getColumnIndex(entry.getValue());

            if (rightJoinColumnIndexes[i] < 0) {
                throw new IllegalArgumentException(
                        "The specified column: " + entry.getValue() + " is not included in the right DataSet " + right.columnNameList());
            }

            if (entry.getKey().equals(entry.getValue())) {
                leftColumnNames.remove(entry.getKey());
            }

            i++;
        }
    }

    /**
     * Inits the new column list for right join.
     *
     * @param newColumnNameList
     * @param newColumnList
     * @param right
     * @param leftColumnNames
     * @param rightColumnNames
     */
    private void initNewColumnListForRightJoin(final List<String> newColumnNameList, final List<List<Object>> newColumnList, final DataSet right,
            final List<String> leftColumnNames, final List<String> rightColumnNames) {
        for (String leftColumnName : leftColumnNames) {
            if (right.containsColumn(leftColumnName)) {
                throw new IllegalArgumentException(
                        "The column in this DataSet: " + leftColumnName + " is already included in right DataSet: " + rightColumnNames);
            }

            newColumnNameList.add(leftColumnName);
            newColumnList.add(new ArrayList<>());
        }

        for (String rightColumnName : rightColumnNames) {
            newColumnNameList.add(rightColumnName);
            newColumnList.add(new ArrayList<>());
        }
    }

    /**
     * Gets the left column names for right join.
     *
     * @param refColumnName
     * @return
     */
    private List<String> getLeftColumnNamesForRightJoin(final String refColumnName) {
        final List<String> leftColumnNames = new ArrayList<>(_columnNameList);

        if (this.containsColumn(refColumnName)) {
            leftColumnNames.remove(refColumnName);
        }

        return leftColumnNames;
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnClass
     * @return
     */
    @Override
    public DataSet rightJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnClass) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRefColumnName(right, onColumnEntry.getValue());

            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + 1);

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, leftColumnNames, newColumnName);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int leftRowIndex = 0, leftDataSetSize = this.size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                hashKey = getHashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final int[] leftColumnIndexes = this.getColumnIndexes(leftColumnNames);
            List<Integer> leftRowIndexList = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = getHashKey(rightJoinColumn.get(rightRowIndex));
                leftRowIndexList = joinColumnLeftRowIndexMap.get(hashKey);

                rightJoin(newColumnList, right, newColumnClass, newColumnIndex, rightRowIndex, leftRowIndexList, leftColumnIndexes);
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

                rightJoin(newColumnList, right, newColumnClass, newColumnIndex, rightRowIndex, leftRowIndexList, leftColumnIndexes);
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
     * @param newColumnClass
     * @param newColumnIndex
     * @param rightRowIndex
     * @param leftRowIndexList
     * @param leftColumnIndexes
     */
    private void rightJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnClass, final int newColumnIndex,
            int rightRowIndex, List<Integer> leftRowIndexList, final int[] leftColumnIndexes) {
        if (N.notNullOrEmpty(leftRowIndexList)) {
            for (int leftRowIndex : leftRowIndexList) {
                for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                    newColumnList.get(i).add(this.get(leftRowIndex, leftColumnIndexes[i]));
                }

                newColumnList.get(newColumnIndex).add(right.getRow(newColumnClass, rightRowIndex));
            }
        } else {
            for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            newColumnList.get(newColumnIndex).add(right.getRow(newColumnClass, rightRowIndex));
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
     * @param newColumnClass
     * @param collSupplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public DataSet rightJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnClass,
            final IntFunction<? extends Collection> collSupplier) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);
        N.checkArgNotNull(collSupplier);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRefColumnName(right, onColumnEntry.getValue());

            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + 1);

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, leftColumnNames, newColumnName);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int leftRowIndex = 0, leftDataSetSize = this.size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                hashKey = getHashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = getHashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final int[] leftColumnIndexes = this.getColumnIndexes(leftColumnNames);
            List<Integer> leftRowIndexList = null;
            List<Integer> rightRowIndexList = null;

            for (Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                leftRowIndexList = joinColumnLeftRowIndexMap.get(rightRowIndexEntry.getKey());
                rightRowIndexList = rightRowIndexEntry.getValue();

                rightJoin(newColumnList, right, newColumnClass, collSupplier, newColumnIndex, leftColumnIndexes, leftRowIndexList, rightRowIndexList);
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

                rightJoin(newColumnList, right, newColumnClass, collSupplier, newColumnIndex, leftColumnIndexes, leftRowIndexList, rightRowIndexList);
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
     * @param newColumnClass
     * @param collSupplier
     * @param newColumnIndex
     * @param leftColumnIndexes
     * @param leftRowIndexList
     * @param rightRowIndexList
     */
    @SuppressWarnings("rawtypes")
    private void rightJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnClass,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final int[] leftColumnIndexes, List<Integer> leftRowIndexList,
            List<Integer> rightRowIndexList) {
        if (N.notNullOrEmpty(leftRowIndexList)) {
            for (int leftRowIndex : leftRowIndexList) {
                for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                    newColumnList.get(i).add(this.get(leftRowIndex, leftColumnIndexes[i]));
                }

                final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

                for (int righRowIndex : rightRowIndexList) {
                    coll.add(right.getRow(newColumnClass, righRowIndex));
                }

                newColumnList.get(newColumnIndex).add(coll);
            }
        } else {
            for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

            for (int righRowIndex : rightRowIndexList) {
                coll.add(right.getRow(newColumnClass, righRowIndex));
            }

            newColumnList.get(newColumnIndex).add(coll);
        }
    }

    /**
     *
     * @param right
     * @param columnName
     * @param refColumnName
     * @return
     */
    @Override
    public DataSet fullJoin(final DataSet right, final String columnName, final String refColumnName) {
        final Map<String, String> onColumnNames = N.asMap(columnName, refColumnName);

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
            final int rightJoinColumnIndex = checkRefColumnName(right, onColumnEntry.getValue());
            final List<String> rightColumnNames = getRightColumnNames(right, onColumnEntry.getValue());

            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, rightColumnNames);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            List<Integer> rightRowIndexList = null;
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = getHashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);
            final Set<Object> joinColumnLeftRowIndexSet = N.newHashSet();

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = getHashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                fullJoin(newColumnList, right, leftRowIndex, rightRowIndexList, rightColumnIndexes);

                joinColumnLeftRowIndexSet.add(hashKey);
            }

            for (Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (joinColumnLeftRowIndexSet.contains(rightRowIndexEntry.getKey()) == false) {
                    fullJoin(newColumnList, right, rightRowIndexEntry.getValue(), rightColumnIndexes);
                }
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];
            final List<String> rightColumnNames = new ArrayList<>(right.columnNameList());

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames, rightColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + rightColumnNames.size());
            initNewColumnList(newColumnNameList, newColumnList, rightColumnNames);

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
                if (joinColumnLeftRowIndexMap.containsKey(rightRowIndexEntry.getKey()) == false) {
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
            for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            for (int i = 0, leftColumnLength = _columnNameList.size(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
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
        if (N.notNullOrEmpty(rightRowIndexList)) {
            for (int rightRowIndex : rightRowIndexList) {
                for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                    newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
                }

                for (int i = 0, leftColumnLength = _columnNameList.size(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                    newColumnList.get(leftColumnLength + i).add(right.get(rightRowIndex, rightColumnIndexes[i]));
                }
            }
        } else {
            for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            for (int i = 0, leftColumnLength = _columnNameList.size(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                newColumnList.get(leftColumnLength + i).add(null);
            }
        }
    }

    /**
     *
     * @param right
     * @param onColumnNames
     * @param newColumnName
     * @param newColumnClass
     * @return
     */
    @Override
    public DataSet fullJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnClass) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRefColumnName(right, onColumnEntry.getValue());

            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            List<Integer> rightRowIndexList = null;
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = getHashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final Set<Object> joinColumnLeftRowIndexSet = N.newHashSet();

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = getHashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                fullJoin(newColumnList, right, newColumnClass, newColumnIndex, leftRowIndex, rightRowIndexList);

                joinColumnLeftRowIndexSet.add(hashKey);
            }

            for (Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (joinColumnLeftRowIndexSet.contains(rightRowIndexEntry.getKey()) == false) {
                    fullJoin(newColumnList, right, newColumnClass, newColumnIndex, rightRowIndexEntry.getValue());
                }
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + 1);

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

                fullJoin(newColumnList, right, newColumnClass, newColumnIndex, leftRowIndex, rightRowIndexList);

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
                if (joinColumnLeftRowIndexMap.containsKey(rightRowIndexEntry.getKey()) == false) {
                    fullJoin(newColumnList, right, newColumnClass, newColumnIndex, rightRowIndexEntry.getValue());
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
     * @param newColumnClass
     * @param newColumnIndex
     * @param rightRowIndexList
     */
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnClass, final int newColumnIndex,
            List<Integer> rightRowIndexList) {
        for (int rightRowIndex : rightRowIndexList) {
            for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            newColumnList.get(newColumnIndex).add(right.getRow(newColumnClass, rightRowIndex));
        }
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param newColumnClass
     * @param newColumnIndex
     * @param leftRowIndex
     * @param rightRowIndexList
     */
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnClass, final int newColumnIndex,
            int leftRowIndex, List<Integer> rightRowIndexList) {
        if (N.notNullOrEmpty(rightRowIndexList)) {
            for (int rightRowIndex : rightRowIndexList) {
                for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                    newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
                }

                newColumnList.get(newColumnIndex).add(right.getRow(newColumnClass, rightRowIndex));
            }
        } else {
            for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
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
     * @param newColumnClass
     * @param collSupplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public DataSet fullJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnClass,
            final IntFunction<? extends Collection> collSupplier) {
        checkJoinOnColumnNames(onColumnNames);
        checkNewColumnName(newColumnName);
        N.checkArgNotNull(collSupplier);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRefColumnName(right, onColumnEntry.getValue());

            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = getHashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final Set<Object> joinColumnLeftRowIndexSet = N.newHashSet();
            List<Integer> rightRowIndexList = null;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = getHashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                fullJoin(newColumnList, right, newColumnClass, collSupplier, newColumnIndex, leftRowIndex, rightRowIndexList);

                joinColumnLeftRowIndexSet.add(hashKey);
            }

            for (Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (joinColumnLeftRowIndexSet.contains(rightRowIndexEntry.getKey()) == false) {
                    fullJoin(newColumnList, right, newColumnClass, collSupplier, newColumnIndex, rightRowIndexEntry.getValue());
                }
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(_columnNameList.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(_columnNameList.size() + 1);
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

                fullJoin(newColumnList, right, newColumnClass, collSupplier, newColumnIndex, leftRowIndex, rightRowIndexList);

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
                if (joinColumnLeftRowIndexMap.containsKey(rightRowIndexEntry.getKey()) == false) {
                    fullJoin(newColumnList, right, newColumnClass, collSupplier, newColumnIndex, rightRowIndexEntry.getValue());
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
     * @param newColumnClass
     * @param collSupplier
     * @param newColumnIndex
     * @param rightRowIndexList
     */
    @SuppressWarnings("rawtypes")
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnClass,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final List<Integer> rightRowIndexList) {
        for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
            newColumnList.get(i).add(null);
        }

        final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

        for (int rightRowIndex : rightRowIndexList) {
            coll.add(right.getRow(newColumnClass, rightRowIndex));
        }

        newColumnList.get(newColumnIndex).add(coll);
    }

    /**
     *
     * @param newColumnList
     * @param right
     * @param newColumnClass
     * @param collSupplier
     * @param newColumnIndex
     * @param leftRowIndex
     * @param rightRowIndexList
     */
    @SuppressWarnings("rawtypes")
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnClass,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, int leftRowIndex, List<Integer> rightRowIndexList) {
        if (N.notNullOrEmpty(rightRowIndexList)) {
            for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

            for (int rightRowIndex : rightRowIndexList) {
                coll.add(right.getRow(newColumnClass, rightRowIndex));
            }

            newColumnList.get(newColumnIndex).add(coll);
        } else {
            for (int i = 0, leftColumnLength = _columnNameList.size(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            newColumnList.get(newColumnIndex).add(null);
        }
    }

    /**
     *
     * @param dataSet
     * @return
     */
    @Override
    public DataSet union(final DataSet dataSet) {
        // final DataSet result = copy(_columnNameList, 0, size(), false);
        // result.merge(dataSet.difference(result));
        // return result;

        return merge(dataSet.difference(this));
    }

    /**
     *
     * @param dataSet
     * @return
     */
    @Override
    public DataSet unionAll(final DataSet dataSet) {
        // final DataSet result = copy(_columnNameList, 0, size(), false);
        // result.merge(dataSet);
        // return result;

        return merge(dataSet);
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    public DataSet intersection(final DataSet other) {
        return remove(other, true);
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    public DataSet difference(final DataSet other) {
        return remove(other, false);
    }

    /**
     *
     * @param other
     * @param retain
     * @return
     */
    private DataSet remove(final DataSet other, final boolean retain) {
        final List<String> commonColumnNameList = new ArrayList<>(this._columnNameList);
        commonColumnNameList.retainAll(other.columnNameList());

        if (N.isNullOrEmpty(commonColumnNameList)) {
            throw new IllegalArgumentException("These two DataSets don't have common column names: " + this._columnNameList + ", " + other.columnNameList());
        }

        final int newColumnCount = this.columnNameList().size();
        final List<String> newColumnNameList = new ArrayList<>(this._columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final int size = size();

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final int commonColumnCount = commonColumnNameList.size();

        if (commonColumnCount == 1) {
            final int columnIndex = this.getColumnIndex(commonColumnNameList.get(0));
            final int otherColumnIndex = other.getColumnIndex(commonColumnNameList.get(0));

            final List<Object> column = _columnList.get(columnIndex);
            final Multiset<Object> rowSet = new Multiset<>();

            for (Object val : other.getColumn(otherColumnIndex)) {
                rowSet.add(getHashKey(val));
            }

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                if ((rowSet.getAndRemove(getHashKey(column.get(rowIndex))) > 0) == retain) {
                    for (int i = 0; i < newColumnCount; i++) {
                        newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                    }
                }
            }
        } else {
            final int[] columnIndexes = this.getColumnIndexes(commonColumnNameList);
            final int[] otherColumnIndexes = other.getColumnIndexes(commonColumnNameList);

            final Multiset<Object[]> rowSet = new Multiset<>(ArrayHashMap.class);
            Object[] row = null;

            for (int otherRowIndex = 0, otherSize = other.size(); otherRowIndex < otherSize; otherRowIndex++) {
                row = row == null ? Objectory.createObjectArray(commonColumnCount) : row;

                for (int i = 0; i < commonColumnCount; i++) {
                    row[i] = other.get(otherRowIndex, otherColumnIndexes[i]);
                }

                if (rowSet.getAndAdd(row) == 0) {
                    row = null;
                }
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            row = Objectory.createObjectArray(commonColumnCount);

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                for (int i = 0; i < commonColumnCount; i++) {
                    row[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
                }

                if ((rowSet.getAndRemove(row) > 0) == retain) {
                    for (int i = 0; i < newColumnCount; i++) {
                        newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                    }
                }
            }

            Objectory.recycle(row);
            row = null;

            for (Object[] a : rowSet) {
                Objectory.recycle(a);
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param dataSet
     * @return
     */
    @Override
    public DataSet symmetricDifference(DataSet dataSet) {
        return this.difference(dataSet).merge(dataSet.difference(this));
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    public DataSet intersectAll(final DataSet other) {
        return remove2(other, true);
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    public DataSet except(final DataSet other) {
        return remove2(other, false);
    }

    /**
     * Removes the 2.
     *
     * @param other
     * @param retain
     * @return
     */
    private DataSet remove2(final DataSet other, final boolean retain) {
        final List<String> commonColumnNameList = new ArrayList<>(this._columnNameList);
        commonColumnNameList.retainAll(other.columnNameList());

        if (N.isNullOrEmpty(commonColumnNameList)) {
            throw new IllegalArgumentException("These two DataSets don't have common column names: " + this._columnNameList + ", " + other.columnNameList());
        }

        final int newColumnCount = this.columnNameList().size();
        final List<String> newColumnNameList = new ArrayList<>(this._columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final int size = size();

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList);
        }

        final int commonColumnCount = commonColumnNameList.size();

        if (commonColumnCount == 1) {
            final int columnIndex = this.getColumnIndex(commonColumnNameList.get(0));
            final int otherColumnIndex = other.getColumnIndex(commonColumnNameList.get(0));

            final List<Object> column = _columnList.get(columnIndex);
            final Set<Object> rowSet = N.newHashSet();

            for (Object e : other.getColumn(otherColumnIndex)) {
                rowSet.add(getHashKey(e));
            }

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                if (rowSet.contains(getHashKey(column.get(rowIndex))) == retain) {
                    for (int i = 0; i < newColumnCount; i++) {
                        newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                    }
                }
            }
        } else {
            final int[] columnIndexes = this.getColumnIndexes(commonColumnNameList);
            final int[] otherColumnIndexes = other.getColumnIndexes(commonColumnNameList);

            final Set<Object[]> rowSet = new ArrayHashSet<>();
            Object[] row = null;

            for (int otherRowIndex = 0, otherSize = other.size(); otherRowIndex < otherSize; otherRowIndex++) {
                row = row == null ? Objectory.createObjectArray(commonColumnCount) : row;

                for (int i = 0; i < commonColumnCount; i++) {
                    row[i] = other.get(otherRowIndex, otherColumnIndexes[i]);
                }

                if (rowSet.add(row)) {
                    row = null;
                }
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            row = Objectory.createObjectArray(commonColumnCount);

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                for (int i = 0; i < commonColumnCount; i++) {
                    row[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
                }

                if (rowSet.contains(row) == retain) {
                    for (int i = 0; i < newColumnCount; i++) {
                        newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                    }
                }
            }

            Objectory.recycle(row);
            row = null;

            for (Object[] a : rowSet) {
                Objectory.recycle(a);
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    /**
     *
     * @param from
     * @return
     */
    @Override
    public DataSet merge(final DataSet from) {
        return merge(from, from.columnNameList(), 0, from.size());
    }

    /**
     *
     * @param from
     * @param columnNames
     * @return
     */
    @Override
    public DataSet merge(final DataSet from, final Collection<String> columnNames) {
        return merge(from, columnNames, 0, from.size());
    }

    /**
     *
     * @param from
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public DataSet merge(final DataSet from, final int fromRowIndex, final int toRowIndex) {
        return merge(from, from.columnNameList(), fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param from
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public DataSet merge(final DataSet from, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        checkRowIndex(fromRowIndex, toRowIndex, from.size());

        final RowDataSet result = copy(this._columnNameList, 0, size(), true);
        List<Object> column = null;

        for (String columnName : columnNames) {
            if (result.containsColumn(columnName) == false) {
                if (column == null) {
                    column = new ArrayList<>(size() + (toRowIndex - fromRowIndex));
                    N.fill(column, 0, size(), null);
                }

                result.addColumn(columnName, column);
            }
        }

        for (String columnName : result._columnNameList) {
            column = result._columnList.get(result.getColumnIndex(columnName));
            int columnIndex = from.getColumnIndex(columnName);

            if (columnIndex >= 0) {
                if (fromRowIndex == 0 && toRowIndex == from.size()) {
                    column.addAll(from.getColumn(columnIndex));
                } else {
                    column.addAll(from.getColumn(columnIndex).subList(fromRowIndex, toRowIndex));
                }
            } else {
                for (int i = fromRowIndex; i < toRowIndex; i++) {
                    column.add(null);
                }
            }
        }

        if (N.notNullOrEmpty(from.properties())) {
            result.properties().putAll(from.properties());
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    @Override
    public DataSet merge(final DataSet a, final DataSet b) {
        return N.merge(this, a, b);
    }

    @Override
    public DataSet merge(final Collection<? extends DataSet> dss) {
        if (N.isNullOrEmpty(dss)) {
            return N.newEmptyDataSet().merge(this);
        }

        final List<DataSet> dsList = new ArrayList<>(N.size(dss) + 1);
        dsList.add(this);
        dsList.addAll(dss);

        return N.merge(dsList);
    }

    /**
     *
     * @param b
     * @return
     */
    @Override
    public DataSet cartesianProduct(DataSet b) {
        final Collection<String> tmp = N.intersection(this._columnNameList, b.columnNameList());
        if (N.notNullOrEmpty(tmp)) {
            throw new IllegalArgumentException(tmp + " are included in both DataSets: " + this._columnNameList + " : " + b.columnNameList());
        }

        final int aSize = this.size();
        final int bSize = b.size();
        final int aColumnCount = this._columnNameList.size();
        final int bColumnCount = b.columnNameList().size();

        final int newColumnCount = aColumnCount + bColumnCount;
        final int newRowCount = aSize * bSize;

        final List<String> newColumnNameList = new ArrayList<>(newColumnCount);
        newColumnNameList.addAll(_columnNameList);
        newColumnNameList.addAll(b.columnNameList());

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
                newColumnList.get(columnIndex + aColumnCount).addAll(b.getColumn(columnIndex));
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
        return split(_columnNameList, chunkSize);
    }

    /**
     *
     * @param columnNames
     * @param chunkSize
     * @return
     */
    @Override
    public Stream<DataSet> split(final Collection<String> columnNames, final int chunkSize) {
        checkColumnName(columnNames);
        N.checkArgPositive(chunkSize, "chunkSize");

        final int expectedModCount = modCount;
        final int totalSize = this.size();

        return IntStream.range(0, totalSize, chunkSize).mapToObj(new IntFunction<DataSet>() {
            @Override
            public DataSet apply(int from) {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }

                return RowDataSet.this.copy(columnNames, from, from <= totalSize - chunkSize ? from + chunkSize : totalSize);
            }
        });
    }

    /**
     *
     * @param chunkSize
     * @return
     */
    @Override
    public List<DataSet> splitToList(final int chunkSize) {
        return splitToList(_columnNameList, chunkSize);
    }

    /**
     *
     * @param columnNames
     * @param chunkSize
     * @return
     */
    @Override
    public List<DataSet> splitToList(final Collection<String> columnNames, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        final List<DataSet> res = new ArrayList<>();

        for (int i = 0, totalSize = size(); i < totalSize; i += chunkSize) {
            res.add(copy(columnNames, i, i <= totalSize - chunkSize ? i + chunkSize : totalSize));
        }

        return res;
    }

    /**
     * 
     * @param chunkSize
     * @return
     * @deprecated replaced by {@link #splitToList(int)}
     */
    @Deprecated
    @Override
    public List<DataSet> splitt(int chunkSize) {
        return splitToList(chunkSize);
    }

    /**
     * 
     * @param columnNames
     * @param chunkSize
     * @return
     * @deprecated replaced by {@link #splitToList(Collection, int)}
     */
    @Deprecated
    @Override
    public List<DataSet> splitt(Collection<String> columnNames, int chunkSize) {
        return splitToList(columnNames, chunkSize);
    }

    //    @Override
    //    public <T> List<List<T>> split(final Class<? extends T> rowClass, final int size) {
    //        return split(rowClass, _columnNameList, size);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(final Class<? extends T> rowClass, final Collection<String> columnNames, final int size) {
    //        return split(rowClass, columnNames, 0, size(), size);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(final Class<? extends T> rowClass, final int fromRowIndex, final int toRowIndex, final int size) {
    //        return split(rowClass, _columnNameList, fromRowIndex, toRowIndex, size);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(final Class<? extends T> rowClass, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex,
    //            final int size) {
    //        checkRowIndex(fromRowIndex, toRowIndex);
    //
    //        final List<T> list = this.toList(rowClass, columnNames, fromRowIndex, toRowIndex);
    //
    //        return N.split(list, size);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(IntFunction<? extends T> rowSupplier, int size) {
    //        return split(rowSupplier, _columnNameList, size);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(IntFunction<? extends T> rowSupplier, Collection<String> columnNames, int size) {
    //        return split(rowSupplier, columnNames, 0, size(), size);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(IntFunction<? extends T> rowSupplier, int fromRowIndex, int toRowIndex, int size) {
    //        return split(rowSupplier, _columnNameList, fromRowIndex, toRowIndex, size);
    //    }
    //
    //    @Override
    //    public <T> List<List<T>> split(IntFunction<? extends T> rowSupplier, Collection<String> columnNames, int fromRowIndex, int toRowIndex, int size) {
    //        checkRowIndex(fromRowIndex, toRowIndex);
    //
    //        final List<T> list = this.toList(rowSupplier, columnNames, fromRowIndex, toRowIndex);
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
        return slice(columnNames, 0, size());
    }

    /**
     *
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public DataSet slice(int fromRowIndex, int toRowIndex) {
        return slice(_columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public DataSet slice(Collection<String> columnNames, int fromRowIndex, int toRowIndex) {
        N.checkFromToIndex(fromRowIndex, toRowIndex, size());
        DataSet ds = null;

        if (N.isNullOrEmpty(columnNames)) {
            ds = N.newEmptyDataSet();
        } else {
            final int[] columnIndexes = checkColumnName(columnNames);
            final List<String> newColumnNames = new ArrayList<>(columnNames);
            final List<List<Object>> newColumnList = new ArrayList<>(newColumnNames.size());

            for (int columnIndex : columnIndexes) {
                newColumnList.add(_columnList.get(columnIndex).subList(fromRowIndex, toRowIndex));
            }

            ds = new RowDataSet(newColumnNames, newColumnList, _properties);
        }

        ds.frozen();
        return ds;
    }

    /**
     *
     * @param pageSize
     * @return
     */
    @Override
    public PaginatedDataSet paginate(final int pageSize) {
        return paginate(_columnNameList, pageSize);
    }

    @Override
    public PaginatedDataSet paginate(final Collection<String> columnNames, final int pageSize) {
        return new PaginatedRowDataSet(N.isNullOrEmpty(columnNames) ? _columnNameList : columnNames, pageSize);
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
        return stream(columnName, 0, size());
    }

    /**
     *
     * @param <T>
     * @param columnName
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <T> Stream<T> stream(final String columnName, int fromRowIndex, int toRowIndex) {
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

    /**
     *
     * @param <T>
     * @param rowMapper
     * @return
     */
    @Override
    public <T> Stream<T> stream(final Function<? super DisposableObjArray, T> rowMapper) {
        return stream(0, size(), rowMapper);
    }

    /**
     *
     * @param <T>
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowMapper
     * @return
     */
    @Override
    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, final Function<? super DisposableObjArray, T> rowMapper) {
        return stream(_columnNameList, fromRowIndex, toRowIndex, rowMapper);
    }

    /**
     *
     * @param <T>
     * @param columnNames
     * @param rowMapper
     * @return
     */
    @Override
    public <T> Stream<T> stream(Collection<String> columnNames, final Function<? super DisposableObjArray, T> rowMapper) {
        return stream(columnNames, 0, size(), rowMapper);
    }

    /**
     *
     * @param <T>
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @param rowMapper
     * @return
     */
    @Override
    public <T> Stream<T> stream(final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex,
            final Function<? super DisposableObjArray, T> rowMapper) {
        final int[] columnIndexes = this.checkColumnName(columnNames);
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(rowMapper, "rowMapper");
        final int columnCount = columnNames.size();

        return Stream.of(new ObjIteratorEx<DisposableObjArray>() {
            private final Type<Object[]> rowType = N.typeOf(Object[].class);
            private Object[] row = new Object[columnCount];
            private DisposableObjArray disposableRow = DisposableObjArray.wrap(row);
            private final int expectedModCount = modCount;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                ConcurrentModification();

                return cursor < toRowIndex;
            }

            @Override
            public DisposableObjArray next() {
                ConcurrentModification();

                if (cursor >= toRowIndex) {
                    throw new NoSuchElementException();
                }

                getRow(rowType, row, columnIndexes, columnNames, cursor);

                cursor++;

                return disposableRow;
            }

            @Override
            public long count() {
                ConcurrentModification();

                return toRowIndex - cursor;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                ConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                ConcurrentModification();

                final List<Object[]> rows = RowDataSet.this.toList(Object[].class, columnNames, cursor, toRowIndex);

                a = a.length >= rows.size() ? a : (A[]) N.newArray(a.getClass().getComponentType(), rows.size());

                rows.toArray(a);

                return a;
            }

            final void ConcurrentModification() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }).map(rowMapper);
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @return
     */
    @Override
    public <T> Stream<T> stream(Class<? extends T> rowClass) {
        return stream(rowClass, 0, size());
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <T> Stream<T> stream(Class<? extends T> rowClass, int fromRowIndex, int toRowIndex) {
        return stream(rowClass, _columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @param columnNames
     * @return
     */
    @Override
    public <T> Stream<T> stream(Class<? extends T> rowClass, Collection<String> columnNames) {
        return stream(rowClass, columnNames, 0, size());
    }

    /**
     *
     * @param <T>
     * @param rowClass
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <T> Stream<T> stream(final Class<? extends T> rowClass, final Collection<String> columnNames, final int fromRowIndex, final int toRowIndex) {
        // return Stream.of(toArray(rowClass, columnNames, fromRowIndex, toRowIndex));

        this.checkRowIndex(fromRowIndex, toRowIndex);
        final int[] columnIndexes = this.checkColumnName(columnNames);

        final Type<?> rowType = N.typeOf(rowClass);
        final boolean isAbstractRowClass = Modifier.isAbstract(rowClass.getModifiers());
        final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass, int.class);
        final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowClass);
        final int columnCount = columnNames.size();

        return Stream.of(new ObjIteratorEx<T>() {
            private final int expectedModCount = modCount;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                ConcurrentModification();

                return cursor < toRowIndex;
            }

            @Override
            public T next() {
                ConcurrentModification();

                if (cursor >= toRowIndex) {
                    throw new NoSuchElementException();
                }

                Object row = null;
                if (rowType.isObjectArray()) {
                    row = N.newArray(rowClass.getComponentType(), columnCount);
                } else if (rowType.isList() || rowType.isSet()) {
                    if (isAbstractRowClass) {
                        row = (rowType.isList() ? new ArrayList<>(columnCount) : N.newHashSet(N.initHashCapacity(columnCount)));
                    } else {
                        if (intConstructor == null) {
                            row = ClassUtil.invokeConstructor(constructor);
                        } else {
                            row = ClassUtil.invokeConstructor(intConstructor, columnCount);
                        }
                    }
                } else if (rowType.isMap()) {
                    if (isAbstractRowClass) {
                        row = new HashMap<>(N.initHashCapacity(columnCount));
                    } else {
                        if (intConstructor == null) {
                            row = ClassUtil.invokeConstructor(constructor);
                        } else {
                            row = ClassUtil.invokeConstructor(intConstructor, N.initHashCapacity(columnCount));
                        }
                    }
                } else if (rowType.isEntity()) {
                    row = N.newInstance(rowClass);
                } else {
                    throw new IllegalArgumentException("Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass)
                            + ". Only Array, List/Set, Map and entity class are supported");
                }

                getRow(rowType, row, columnIndexes, columnNames, cursor);

                cursor++;

                return (T) row;
            }

            @Override
            public long count() {
                ConcurrentModification();

                return toRowIndex - cursor;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                ConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                ConcurrentModification();

                final List<T> rows = RowDataSet.this.toList(rowClass, columnNames, cursor, toRowIndex);

                a = a.length >= rows.size() ? a : (A[]) N.newArray(a.getClass().getComponentType(), rows.size());

                rows.toArray(a);

                return a;
            }

            final void ConcurrentModification() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        });
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @return
     */
    @Override
    public <T> Stream<T> stream(IntFunction<? extends T> rowSupplier) {
        return stream(rowSupplier, 0, size());
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <T> Stream<T> stream(IntFunction<? extends T> rowSupplier, int fromRowIndex, int toRowIndex) {
        return stream(rowSupplier, _columnNameList, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @param columnNames
     * @return
     */
    @Override
    public <T> Stream<T> stream(IntFunction<? extends T> rowSupplier, Collection<String> columnNames) {
        return stream(rowSupplier, columnNames, 0, size());
    }

    /**
     *
     * @param <T>
     * @param rowSupplier
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @return
     */
    @Override
    public <T> Stream<T> stream(final IntFunction<? extends T> rowSupplier, final Collection<String> columnNames, final int fromRowIndex,
            final int toRowIndex) {
        // return Stream.of(toArray(rowClass, columnNames, fromRowIndex, toRowIndex));

        final int[] columnIndexes = this.checkColumnName(columnNames);
        this.checkRowIndex(fromRowIndex, toRowIndex);

        final Class<?> rowClass = rowSupplier.apply(0).getClass();
        final Type<?> rowType = N.typeOf(rowClass);
        final int columnCount = columnNames.size();

        return Stream.of(new ObjIteratorEx<T>() {
            private final int expectedModCount = modCount;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                ConcurrentModification();

                return cursor < toRowIndex;
            }

            @Override
            public T next() {
                ConcurrentModification();

                if (cursor >= toRowIndex) {
                    throw new NoSuchElementException();
                }

                final Object row = rowSupplier.apply(columnCount);

                getRow(rowType, row, columnIndexes, columnNames, cursor);

                cursor++;

                return (T) row;
            }

            @Override
            public long count() {
                ConcurrentModification();

                return toRowIndex - cursor;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                ConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                ConcurrentModification();

                final List<T> rows = RowDataSet.this.toList(rowSupplier, columnNames, cursor, toRowIndex);

                a = a.length >= rows.size() ? a : (A[]) N.newArray(a.getClass().getComponentType(), rows.size());

                rows.toArray(a);

                return a;
            }

            final void ConcurrentModification() {
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
    public <R, E extends Exception> R apply(Throwables.Function<? super DataSet, R, E> func) throws E {
        return func.apply(this);
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
    public boolean frozen() {
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

    @Override
    public Stream<String> columnNames() {
        return Stream.of(_columnNameList);
    }

    @Override
    public Stream<ImmutableList<Object>> columns() {
        return IntStream.range(0, this._columnNameList.size()).mapToObj(new IntFunction<ImmutableList<Object>>() {
            @Override
            public ImmutableList<Object> apply(int columnIndex) {
                return getColumn(columnIndex);
            }
        });
    }

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
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void println() throws UncheckedIOException {
        println(_columnNameList, 0, size());
    }

    /**
     *
     * @param columnNames
     * @param fromRowIndex
     * @param toRowIndex
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public void println(Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException {
        println(new OutputStreamWriter(System.out), columnNames, fromRowIndex, toRowIndex);
    }

    /**
     *
     * @param <W>
     * @param outputWriter
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
    public <W extends Writer> W println(final W outputWriter) throws UncheckedIOException {
        return println(outputWriter, _columnNameList, 0, size());
    }

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
    @Override
    public <W extends Writer> W println(final W outputWriter, Collection<String> columnNames, int fromRowIndex, int toRowIndex) throws UncheckedIOException {
        final int[] columnIndexes = N.isNullOrEmpty(columnNames) ? N.EMPTY_INT_ARRAY : checkColumnName(columnNames);
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

                    bw.write(StringUtil.repeat(hch, maxColumnLens[i] + hchDelta));
                }

                bw.write('+');

                bw.write(IOUtil.LINE_SEPARATOR);

                for (int i = 0; i < columnLen; i++) {
                    if (i == 0) {
                        bw.write("| ");
                    } else {
                        bw.write(" | ");
                    }

                    bw.write(StringUtil.padEnd(columnNameList.get(i), maxColumnLens[i]));
                }

                bw.write(" |");

                bw.write(IOUtil.LINE_SEPARATOR);

                for (int i = 0; i < columnLen; i++) {
                    bw.write('+');

                    bw.write(StringUtil.repeat(hch, maxColumnLens[i] + hchDelta));
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

                        bw.write(StringUtil.padEnd(strColumnList.get(i).get(j), maxColumnLens[i]));
                    }

                    bw.write(" |");
                }

                if (rowLen == 0) {
                    bw.write(IOUtil.LINE_SEPARATOR);

                    for (int i = 0; i < columnLen; i++) {
                        if (i == 0) {
                            bw.write("| ");
                            bw.write(StringUtil.padEnd("", maxColumnLens[i]));
                        } else {
                            bw.write(StringUtil.padEnd("", maxColumnLens[i] + 3));
                        }
                    }

                    bw.write(" |");
                }

                bw.write(IOUtil.LINE_SEPARATOR);

                for (int i = 0; i < columnLen; i++) {
                    bw.write('+');

                    bw.write(StringUtil.repeat(hch, maxColumnLens[i] + hchDelta));
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

        return outputWriter;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + _columnNameList.hashCode();
        h = (h * 31) + _columnList.hashCode();

        return h;
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof RowDataSet) {
            RowDataSet other = (RowDataSet) obj;

            return (size() == other.size()) && N.equals(_columnNameList, other._columnNameList) && N.equals(_columnList, other._columnList);
        }

        return false;
    }

    @Override
    public String toString() {
        if (_columnNameList.size() == 0) {
            return "[[]]";
        }

        final BufferedWriter bw = Objectory.createBufferedWriter();

        try {
            toCSV(bw, true, false);

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
            throw new IllegalArgumentException("The specified column(" + columnName + ") is not included in this DataSet " + _columnNameList);
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
        if (N.isNullOrEmpty(columnNames)) {
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
        if (N.isNullOrEmpty(columnNames)) {
            throw new IllegalArgumentException("The specified columnNames is null or empty");
        }

        if (columnNames == this._columnNameList) {
            if (this._columnIndexes == null) {
                int count = columnNames.size();
                this._columnIndexes = new int[count];

                for (int i = 0; i < count; i++) {
                    _columnIndexes[i] = i;
                }
            }

            return _columnIndexes;
        } else {
            final int count = columnNames.size();
            final int[] columnNameIndexes = new int[count];
            final Iterator<String> it = columnNames.iterator();

            for (int i = 0; i < count; i++) {
                columnNameIndexes[i] = checkColumnName(it.next());
            }

            return columnNameIndexes;
        }
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
    static Object getHashKey(Object obj) {
        return obj == null || obj.getClass().isArray() == false ? obj : Wrapper.of(obj);
    }

    /**
     * The Class PaginatedRowDataSet.
     *
     * @author Haiyang Li
     * @version $Revision: 0.8 $ 07/01/15
     */
    private class PaginatedRowDataSet implements PaginatedDataSet {

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
        private PaginatedRowDataSet(final Collection<String> columnNames, final int pageSize) {
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
            return new Itr();
        }

        /**
         * Checks for next.
         *
         * @return true, if successful
         */
        @Override
        public boolean hasNext() {
            return currentPageNum < pageCount();
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
        public DataSet nextPage() {
            return absolute(currentPageNum + 1).currentPage();
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
            ConcurrentModification();
            checkPageNumber(pageNum);

            synchronized (pagePool) {
                DataSet page = pagePool.get(pageNum);

                if (page == null) {
                    int offset = pageNum * pageSize;
                    page = RowDataSet.this.slice(columnNames, offset, Math.min(offset + pageSize, RowDataSet.this.size()));

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
        public PaginatedDataSet absolute(final int pageNumber) {
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
         */
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

        final void ConcurrentModification() {
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

        /**
         * The Class Itr.
         */
        private class Itr implements Iterator<DataSet> {

            /** The cursor. */
            int cursor = 0;

            /**
             * Checks for next.
             *
             * @return true, if successful
             */
            @Override
            public boolean hasNext() {
                return cursor < pageCount();
            }

            /**
             *
             * @return
             */
            @Override
            public DataSet next() {
                ConcurrentModification();

                try {
                    DataSet next = getPage(cursor);
                    cursor++;

                    return next;
                } catch (IndexOutOfBoundsException e) {
                    ConcurrentModification();
                    throw new NoSuchElementException();
                }
            }

            /**
             * Removes the.
             */
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
