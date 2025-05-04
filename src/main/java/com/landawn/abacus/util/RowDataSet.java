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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
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
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.IntObjFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

@SuppressWarnings({ "java:S1192", "java:S1698", "java:S1854", "java:S6539" })
public class RowDataSet implements DataSet, Cloneable {

    static final DataSet EMPTY_DATA_SET = new RowDataSet(N.emptyList(), N.emptyList());

    static {
        EMPTY_DATA_SET.freeze();
    }

    static final char PROP_NAME_SEPARATOR = '.';

    static final String NULL_STRING = "null";

    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    static final String TRUE = Boolean.TRUE.toString().intern();

    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();

    static final String FALSE = Boolean.FALSE.toString().intern();

    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    static final Set<Class<?>> SUPPORTED_COUNT_COLUMN_TYPES = N.asSet(int.class, Integer.class, long.class, Long.class, float.class, Float.class, double.class,
            Double.class);

    static final String POSTFIX_FOR_SAME_JOINED_COLUMN_NAME = "_2";

    static final String CACHED_PROP_NAMES = "cachedPropNames";

    private static final String ROW = "row";

    private static final JSONParser jsonParser = ParserFactory.createJSONParser();

    private static final XMLParser xmlParser = ParserFactory.isXMLAvailable() ? ParserFactory.createXMLParser() : null;

    private static final KryoParser kryoParser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : null;

    private static final JSONSerializationConfig jsc = JSC.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

    private static final XMLSerializationConfig xsc = XSC.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

    private static final Type<Object> strType = N.typeOf(String.class);

    private List<String> _columnNameList; //NOSONAR

    private List<List<Object>> _columnList; //NOSONAR

    private Map<String, Integer> _columnIndexMap; //NOSONAR

    private int[] _columnIndexes; //NOSONAR

    private int _currentRowNum = 0; //NOSONAR

    private boolean _isFrozen = false; //NOSONAR

    private Map<String, Object> _properties; //NOSONAR

    private transient int modCount = 0; //NOSONAR

    // For Kryo
    protected RowDataSet() {
        _properties = N.emptyMap();
    }

    public RowDataSet(final List<String> columnNameList, final List<List<Object>> columnList) {
        this(columnNameList, columnList, null);
    }

    public RowDataSet(final List<String> columnNameList, final List<List<Object>> columnList, final Map<String, Object> properties)
            throws IllegalArgumentException {
        N.checkArgNotNull(columnNameList);
        N.checkArgNotNull(columnList);

        N.checkArgument(!N.anyEmpty(columnNameList), "Empty column name found in: {}", columnNameList);
        N.checkArgument(!N.hasDuplicates(columnNameList), "Duplicated column names found in: {}", columnNameList);
        N.checkArgument(columnNameList.size() == columnList.size(), "The size of column name list: {} is different from the size of column list: {}",
                columnNameList.size(), columnList.size());

        final int size = columnList.size() == 0 ? 0 : columnList.get(0).size();

        for (final List<Object> column : columnList) {
            N.checkArgument(column.size() == size, "All columns in the specified 'columnList' must have same size.");
        }

        _columnNameList = columnNameList;

        _columnList = columnList;

        if (N.isEmpty(properties)) {
            _properties = N.emptyMap();
        } else {
            _properties = Maps.newOrderingMap(properties);
            _properties.putAll(properties);
        }
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

    @Override
    public ImmutableList<String> columnNameList() {
        // return _columnNameList;

        return ImmutableList.wrap(_columnNameList);
    }

    @Override
    public int columnCount() {
        return _columnNameList.size();
    }

    @Override
    public String getColumnName(final int columnIndex) {
        return _columnNameList.get(columnIndex);
    }

    @Override
    public int getColumnIndex(final String columnName) throws IllegalArgumentException {
        if (_columnIndexMap == null) {
            _columnIndexMap = new HashMap<>();

            int i = 0;
            for (final String e : _columnNameList) {
                _columnIndexMap.put(e, i++);
            }
        }

        final Integer columnIndex = _columnIndexMap.get(columnName);

        //    if (columnIndex == null /* && NameUtil.isCanonicalName(_beanName, columnName)*/) {
        //        columnIndex = _columnIndexMap.get(NameUtil.getSimpleName(columnName));
        //    }

        if (columnIndex == null) {
            throw new IllegalArgumentException("The specified column: " + columnName + " is not included in this DataSet: " + _columnNameList);
        }

        return columnIndex;
    }

    int checkColumnName(final String columnName) throws IllegalArgumentException {
        return getColumnIndex(columnName);
    }

    @Override
    public int[] getColumnIndexes(final Collection<String> columnNames) throws IllegalArgumentException {
        if (N.isEmpty(columnNames)) {
            return N.EMPTY_INT_ARRAY;
        }

        if (isColumnNameList(columnNames)) {
            if (_columnIndexes == null) {
                final int count = columnNames.size();
                _columnIndexes = new int[count];

                for (int i = 0; i < count; i++) {
                    _columnIndexes[i] = i;
                }
            }

            return _columnIndexes.clone();
        }

        if (_columnIndexMap == null) {
            _columnIndexMap = new HashMap<>();

            int i = 0;
            for (final String e : _columnNameList) {
                _columnIndexMap.put(e, i++);
            }
        }

        final int[] columnIndexes = new int[columnNames.size()];
        int i = 0;
        Integer columnIndex = null;

        for (final String columnName : columnNames) {
            columnIndex = _columnIndexMap.get(columnName);

            //    if (columnIndex == null /* && NameUtil.isCanonicalName(_beanName, columnName)*/) {
            //        columnIndex = _columnIndexMap.get(NameUtil.getSimpleName(columnName));
            //    }

            if (columnIndex == null) {
                throw new IllegalArgumentException("The specified column: " + columnName + " is not included in this DataSet: " + _columnNameList);
            }

            columnIndexes[i++] = columnIndex;
        }

        return columnIndexes;
    }

    int[] checkColumnNames(final Collection<String> columnNames) throws IllegalArgumentException {
        if (N.isEmpty(columnNames)) {
            throw new IllegalArgumentException("The specified columnNames is null or empty");
        }

        if (isColumnNameList(columnNames)) {
            if (_columnIndexes == null) {
                final int count = columnNames.size();
                _columnIndexes = new int[count];

                for (int i = 0; i < count; i++) {
                    _columnIndexes[i] = i;
                }
            }

            return _columnIndexes;
        }

        return getColumnIndexes(columnNames);
    }

    boolean isColumnNameList(final Collection<String> columnNames) {
        if (columnNames == _columnNameList) {
            return true;
        }

        if (columnNames instanceof ImmutableList<String> immutableList) { // NOSONAR
            return immutableList.list == _columnNameList;
        }

        return false;
    }

    @Override
    public boolean containsColumn(final String columnName) {
        if (_columnIndexMap == null) {
            _columnIndexMap = new HashMap<>();

            int i = 0;
            for (final String e : _columnNameList) {
                _columnIndexMap.put(e, i++);
            }
        }

        return _columnIndexMap.containsKey(columnName); // || _columnIndexMap.containsKey(NameUtil.getSimpleName(columnName));
    }

    @Override
    public boolean containsAllColumns(final Collection<String> columnNames) {
        for (final String columnName : columnNames) {
            if (!containsColumn(columnName)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void renameColumn(final String columnName, final String newColumnName) throws IllegalArgumentException {
        checkFrozen();

        final int idx = checkColumnName(columnName);

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

    @Override
    public void renameColumns(final Map<String, String> oldNewNames) throws IllegalArgumentException {
        checkFrozen();

        if (N.hasDuplicates(oldNewNames.values())) {
            throw new IllegalArgumentException("Duplicated new column names: " + oldNewNames.values());
        }

        for (final Map.Entry<String, String> entry : oldNewNames.entrySet()) {
            checkColumnName(entry.getKey());

            if (_columnNameList.contains(entry.getValue()) && !entry.getKey().equals(entry.getValue())) {
                throw new IllegalArgumentException("The new column name: " + entry.getValue() + " is already included this DataSet: " + _columnNameList);
            }
        }

        for (final Map.Entry<String, String> entry : oldNewNames.entrySet()) {
            renameColumn(entry.getKey(), entry.getValue());
        }
    }

    //    @Override
    //    public  void renameColumn(final String columnName, final Function<? super String, String> func) {
    //        renameColumn(columnName, func.apply(columnName));
    //    }

    @Override
    public void renameColumns(final Collection<String> columnNames, final Function<? super String, String> func) throws IllegalArgumentException {
        checkColumnNames(columnNames);

        final Map<String, String> map = N.newHashMap(columnNames.size());

        for (final String columnName : columnNames) {
            map.put(columnName, func.apply(columnName));
        }

        renameColumns(map);
    }

    @Override
    public void renameColumns(final Function<? super String, String> func) throws IllegalArgumentException {
        renameColumns(_columnNameList, func);
    }

    @Override
    public void moveColumn(final String columnName, final int newPosition) throws IllegalArgumentException {
        checkFrozen();

        final int idx = checkColumnName(columnName);

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

    @Override
    public void moveColumns(final Map<String, Integer> columnNameNewPositionMap) {
        checkFrozen();

        final List<Map.Entry<String, Integer>> entries = new ArrayList<>(columnNameNewPositionMap.size());

        for (final Map.Entry<String, Integer> entry : columnNameNewPositionMap.entrySet()) {
            checkColumnName(entry.getKey());

            if (entry.getValue() < 0 || entry.getValue() >= columnCount()) {
                throw new IllegalArgumentException("The new column index must be >= 0 and < " + columnCount());
            }

            entries.add(entry);
        }

        N.sort(entries, Comparators.comparingByValue());

        for (final Map.Entry<String, Integer> entry : entries) {
            final int currentColumnIndex = checkColumnName(entry.getKey());

            if (currentColumnIndex == entry.getValue()) {
                // ignore.
            } else {
                _columnNameList.add(entry.getValue(), _columnNameList.remove(currentColumnIndex));
                _columnList.add(entry.getValue(), _columnList.remove(currentColumnIndex));

                _columnIndexMap = null;
            }
        }

        modCount++;
    }

    @Override
    public void swapColumns(final String columnNameA, final String columnNameB) {
        checkFrozen();

        final int columnIndexA = checkColumnName(columnNameA);
        final int columnIndexB = checkColumnName(columnNameB);

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

    @Override
    public void moveRow(final int rowIndex, final int newRowIndex) {
        checkFrozen();

        checkRowNum(rowIndex);
        checkRowNum(newRowIndex);

        if (rowIndex == newRowIndex) {
            return;
        }

        for (final List<Object> column : _columnList) {
            column.add(newRowIndex, column.remove(rowIndex));
        }

        modCount++;
    }

    @Override
    public void swapRows(final int rowIndexA, final int rowIndexB) {
        checkFrozen();

        checkRowNum(rowIndexA);
        checkRowNum(rowIndexB);

        if (rowIndexA == rowIndexB) {
            return;
        }

        Object tmp = null;

        for (final List<Object> column : _columnList) {
            tmp = column.get(rowIndexA);
            column.set(rowIndexA, column.get(rowIndexB));
            column.set(rowIndexB, tmp);
        }

        modCount++;
    }

    @Override
    public <T> T get(final int rowIndex, final int columnIndex) {
        return (T) _columnList.get(columnIndex).get(rowIndex);
    }

    //    @Override
    //    public <T> T get(final Class<? extends T> targetType, final int rowIndex, final int columnIndex) {
    //        T rt = (T) _columnList.get(columnIndex).get(rowIndex);
    //
    //        return (rt == null) ? N.defaultValueOf(targetType) : rt;
    //    }

    @Override
    public void set(final int rowIndex, final int columnIndex, final Object element) {
        checkFrozen();

        _columnList.get(columnIndex).set(rowIndex, element);

        modCount++;
    }

    @Override
    public boolean isNull(final int rowIndex, final int columnIndex) {
        return get(rowIndex, columnIndex) == null;
    }

    @Override
    public <T> T get(final int columnIndex) {
        return (T) _columnList.get(columnIndex).get(_currentRowNum);
    }

    //    @Override
    //    public <T> T get(final Class<? extends T> targetType, final int columnIndex) {
    //        T rt = get(columnIndex);
    //
    //        return (rt == null) ? N.defaultValueOf(targetType) : rt;
    //    }

    @Override
    public <T> T get(final String columnName) {
        return get(checkColumnName(columnName));
    }

    //    @Override
    //    public <T> T get(final Class<? extends T> targetType, final String columnName) {
    //        return get(targetType, checkColumnName(columnName));
    //    }
    //

    //    @Override
    //    public <T> T getOrDefault(int columnIndex, T defaultValue) {
    //        return columnIndex < 0 ? defaultValue : (T) get(columnIndex);
    //    }
    //

    //    @Override
    //    public <T> T getOrDefault(final String columnName, T defaultValue) {
    //        return getOrDefault(getColumnIndex(columnName), defaultValue);
    //    }

    @Override
    public boolean getBoolean(final int columnIndex) {
        final Boolean rt = get(columnIndex);

        return rt != null && rt;
    }

    @Override
    public boolean getBoolean(final String columnName) {
        return getBoolean(checkColumnName(columnName));
    }

    @Override
    public char getChar(final int columnIndex) {
        final Character rt = get(columnIndex);

        return (rt == null) ? 0 : rt;
    }

    @Override
    public char getChar(final String columnName) {
        return getChar(checkColumnName(columnName));
    }

    @Override
    public byte getByte(final int columnIndex) {
        final Number rt = get(columnIndex);

        return (rt == null) ? 0 : rt.byteValue();
    }

    @Override
    public byte getByte(final String columnName) {
        return getByte(checkColumnName(columnName));
    }

    @Override
    public short getShort(final int columnIndex) {
        final Number rt = get(columnIndex);

        return (rt == null) ? 0 : rt.shortValue();
    }

    @Override
    public short getShort(final String columnName) {
        return getShort(checkColumnName(columnName));
    }

    @Override
    public int getInt(final int columnIndex) {
        final Number rt = get(columnIndex);

        return (rt == null) ? 0 : rt.intValue();
    }

    @Override
    public int getInt(final String columnName) {
        return getInt(checkColumnName(columnName));
    }

    @Override
    public long getLong(final int columnIndex) {
        final Number rt = get(columnIndex);

        return (rt == null) ? 0L : rt.longValue();
    }

    @Override
    public long getLong(final String columnName) {
        return getLong(checkColumnName(columnName));
    }

    @Override
    public float getFloat(final int columnIndex) {
        final Number rt = get(columnIndex);

        return (rt == null) ? 0f : Numbers.toFloat(rt);
    }

    @Override
    public float getFloat(final String columnName) {
        return getFloat(checkColumnName(columnName));
    }

    @Override
    public double getDouble(final int columnIndex) {
        final Number rt = get(columnIndex);

        return (rt == null) ? 0d : Numbers.toDouble(rt);
    }

    @Override
    public double getDouble(final String columnName) {
        return getDouble(checkColumnName(columnName));
    }

    @Override
    public boolean isNull(final int columnIndex) {
        return get(columnIndex) == null;
    }

    @Override
    public boolean isNull(final String columnName) {
        return get(columnName) == null;
    }

    @Override
    public void set(final int columnIndex, final Object value) {
        checkFrozen();

        _columnList.get(columnIndex).set(_currentRowNum, value);

        modCount++;
    }

    @Override
    public void set(final String columnName, final Object value) {
        set(checkColumnName(columnName), value);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <T> ImmutableList<T> getColumn(final int columnIndex) {
        // return (List<T>) _columnList.get(columnIndex);
        return ImmutableList.wrap((List) _columnList.get(columnIndex));
    }

    @Override
    public <T> ImmutableList<T> getColumn(final String columnName) {
        return getColumn(checkColumnName(columnName));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <T> List<T> copyColumn(final String columnName) {
        return new ArrayList<>((List) _columnList.get(checkColumnName(columnName)));
    }

    @Override
    public void addColumn(final String newColumnName, final Collection<?> column) {
        addColumn(_columnList.size(), newColumnName, column);
    }

    @Override
    public void addColumn(final int newColumnPosition, final String newColumnName, final Collection<?> column) {
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

    @Override
    public void addColumn(final String newColumnName, final String fromColumnName, final Function<?, ?> func) {
        addColumn(_columnList.size(), newColumnName, fromColumnName, func);
    }

    @Override
    public void addColumn(final int newColumnPosition, final String newColumnName, final String fromColumnName, final Function<?, ?> func) {
        checkFrozen();

        if (newColumnPosition < 0 || newColumnPosition > columnCount()) {
            throw new IllegalArgumentException("Invalid column index: " + newColumnPosition + ". It must be >= 0 and <= " + columnCount());
        }

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
        }

        final List<Object> newColumn = new ArrayList<>(size());
        final Function<Object, Object> mapperToUse = (Function<Object, Object>) func;
        final List<Object> column = _columnList.get(checkColumnName(fromColumnName));

        for (final Object val : column) {
            newColumn.add(mapperToUse.apply(val));
        }

        _columnNameList.add(newColumnPosition, newColumnName);
        _columnList.add(newColumnPosition, newColumn);

        updateColumnIndex(newColumnPosition, newColumnName);

        modCount++;
    }

    @Override
    public void addColumn(final String newColumnName, final Collection<String> fromColumnNames, final Function<? super DisposableObjArray, ?> func) {
        addColumn(_columnList.size(), newColumnName, fromColumnNames, func);
    }

    @Override
    public void addColumn(final int newColumnPosition, final String newColumnName, final Collection<String> fromColumnNames,
            final Function<? super DisposableObjArray, ?> func) {
        checkFrozen();

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
        }

        final int size = size();
        final int[] fromColumnIndexes = checkColumnNames(fromColumnNames);
        final Function<? super DisposableObjArray, Object> mapperToUse = (Function<? super DisposableObjArray, Object>) func;
        final List<Object> newColumn = new ArrayList<>(size);
        final Object[] row = new Object[fromColumnIndexes.length];
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(row);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0, len = fromColumnIndexes.length; i < len; i++) {
                row[i] = _columnList.get(fromColumnIndexes[i]).get(rowIndex);
            }

            newColumn.add(mapperToUse.apply(disposableArray));
        }

        _columnNameList.add(newColumnPosition, newColumnName);
        _columnList.add(newColumnPosition, newColumn);

        updateColumnIndex(newColumnPosition, newColumnName);

        modCount++;
    }

    private void updateColumnIndex(final int columnIndex, final String newColumnName) {
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

    @Override
    public void addColumn(final String newColumnName, final Tuple2<String, String> fromColumnNames, final BiFunction<?, ?, ?> func) {
        addColumn(_columnList.size(), newColumnName, fromColumnNames, func);
    }

    @Override
    public void addColumn(final int newColumnPosition, final String newColumnName, final Tuple2<String, String> fromColumnNames,
            final BiFunction<?, ?, ?> func) {
        checkFrozen();

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
        }

        final int size = size();
        final List<Object> column1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(fromColumnNames._2));

        final BiFunction<Object, Object, Object> mapperToUse = (BiFunction<Object, Object, Object>) func;
        final List<Object> newColumn = new ArrayList<>(size());

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            newColumn.add(mapperToUse.apply(column1.get(rowIndex), column2.get(rowIndex)));
        }

        _columnNameList.add(newColumnPosition, newColumnName);
        _columnList.add(newColumnPosition, newColumn);

        updateColumnIndex(newColumnPosition, newColumnName);

        modCount++;
    }

    @Override
    public void addColumn(final String newColumnName, final Tuple3<String, String, String> fromColumnNames, final TriFunction<?, ?, ?, ?> func) {
        addColumn(_columnList.size(), newColumnName, fromColumnNames, func);
    }

    @Override
    public void addColumn(final int newColumnPosition, final String newColumnName, final Tuple3<String, String, String> fromColumnNames,
            final TriFunction<?, ?, ?, ?> func) {
        checkFrozen();

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
        }

        final int size = size();
        final List<Object> column1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(fromColumnNames._3));

        final TriFunction<Object, Object, Object, Object> mapperToUse = (TriFunction<Object, Object, Object, Object>) func;
        final List<Object> newColumn = new ArrayList<>(size());

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            newColumn.add(mapperToUse.apply(column1.get(rowIndex), column2.get(rowIndex), column3.get(rowIndex)));
        }

        _columnNameList.add(newColumnPosition, newColumnName);
        _columnList.add(newColumnPosition, newColumn);

        updateColumnIndex(newColumnPosition, newColumnName);

        modCount++;
    }

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

    @Override
    public void removeColumns(final Collection<String> columnNames) {
        checkFrozen();

        if (N.isEmpty(columnNames)) {
            return;
        }

        final int[] columnIndexes = checkColumnNames(columnNames);
        N.sort(columnIndexes);

        for (int i = 0, len = columnIndexes.length; i < len; i++) {
            _columnNameList.remove(columnIndexes[i] - i);
            _columnList.remove(columnIndexes[i] - i);
        }

        _columnIndexMap = null;
        _columnIndexes = null;

        modCount++;
    }

    @Override
    public void removeColumns(final Predicate<? super String> filter) {
        checkFrozen();

        final List<String> columnNames = filterColumnNames(_columnNameList, filter);

        if (N.notEmpty(columnNames)) {
            removeColumns(columnNames);
        }
    }

    //    @Deprecated
    //    @Override
    //    public void removeColumnsIf(Predicate<? super String, E> filter) {
    //        removeColumns(filter);
    //    }

    @Override
    public void convertColumn(final String columnName, final Class<?> targetType) {
        checkFrozen();

        convertColumnType(checkColumnName(columnName), targetType);
    }

    @Override
    public void convertColumns(final Map<String, Class<?>> columnTargetTypes) {
        checkFrozen();

        if (N.isEmpty(columnTargetTypes)) {
            return;
        }

        checkColumnNames(columnTargetTypes.keySet());

        for (final Map.Entry<String, Class<?>> entry : columnTargetTypes.entrySet()) {
            convertColumnType(checkColumnName(entry.getKey()), entry.getValue());
        }
    }

    @Override
    public void updateColumn(final String columnName, final Function<?, ?> func) {
        checkFrozen();

        final Function<Object, Object> funcToUse = (Function<Object, Object>) func;
        final List<Object> column = _columnList.get(checkColumnName(columnName));

        for (int i = 0, len = size(); i < len; i++) {
            column.set(i, funcToUse.apply(column.get(i)));
        }

        modCount++;
    }

    @Override
    public void updateColumns(final Collection<String> columnNames, final Function<?, ?> func) {
        checkFrozen();

        if (N.isEmpty(columnNames)) {
            return;
        }

        checkColumnNames(columnNames);

        final Function<Object, Object> funcToUse = (Function<Object, Object>) func;

        for (final String columnName : columnNames) {
            final List<Object> column = _columnList.get(checkColumnName(columnName));

            for (int i = 0, len = size(); i < len; i++) {
                column.set(i, funcToUse.apply(column.get(i)));
            }
        }

        modCount++;
    }

    private void convertColumnType(final int columnIndex, final Class<?> targetType) {
        final List<Object> column = _columnList.get(columnIndex);

        Object newValue = null;
        for (int i = 0, len = size(); i < len; i++) {
            newValue = N.convert(column.get(i), targetType);

            column.set(i, newValue);
        }

        modCount++;
    }

    @Override
    public void combineColumns(final Collection<String> columnNames, final String newColumnName, final Class<?> newColumnType) {
        checkFrozen();

        final List<Object> newColumn = toList(0, size(), columnNames, newColumnType);

        removeColumns(columnNames);

        addColumn(newColumnName, newColumn);
    }

    @Override
    public void combineColumns(final Collection<String> columnNames, final String newColumnName, final Function<? super DisposableObjArray, ?> combineFunc) {
        addColumn(newColumnName, columnNames, combineFunc);

        removeColumns(columnNames);
    }

    @Override
    public void combineColumns(final Predicate<? super String> columnNameFilter, final String newColumnName, final Class<?> newColumnType) {
        combineColumns(filterColumnNames(_columnNameList, columnNameFilter), newColumnName, newColumnType);
    }

    @Override
    public void combineColumns(final Predicate<? super String> columnNameFilter, final String newColumnName,
            final Function<? super DisposableObjArray, ?> combineFunc) {
        combineColumns(filterColumnNames(_columnNameList, columnNameFilter), newColumnName, combineFunc);
    }

    @Override
    public void combineColumns(final Tuple2<String, String> columnNames, final String newColumnName, final BiFunction<?, ?, ?> combineFunc) {
        addColumn(newColumnName, columnNames, combineFunc);

        removeColumns(Arrays.asList(columnNames._1, columnNames._2));
    }

    @Override
    public void combineColumns(final Tuple3<String, String, String> columnNames, final String newColumnName, final TriFunction<?, ?, ?, ?> combineFunc) {
        addColumn(newColumnName, columnNames, combineFunc);

        removeColumns(Arrays.asList(columnNames._1, columnNames._2, columnNames._3));
    }

    @Override
    public void divideColumn(final String columnName, final Collection<String> newColumnNames, final Function<?, ? extends List<?>> divideFunc) {
        checkFrozen();

        final int columnIndex = checkColumnName(columnName);

        if (N.isEmpty(newColumnNames)) {
            throw new IllegalArgumentException("New column names can't be null or empty.");
        }

        if (!N.disjoint(_columnNameList, newColumnNames)) {
            throw new IllegalArgumentException("Column names: " + N.intersection(_columnNameList, newColumnNames) + " already are included in this data set.");
        }

        final Function<Object, List<Object>> divideFuncToUse = (Function<Object, List<Object>>) divideFunc;
        final int newColumnsLen = newColumnNames.size();
        final List<List<Object>> newColumns = new ArrayList<>(newColumnsLen);

        for (int i = 0; i < newColumnsLen; i++) {
            newColumns.add(new ArrayList<>(size()));
        }

        final List<Object> column = _columnList.get(columnIndex);

        for (final Object val : column) {
            final List<Object> newVals = divideFuncToUse.apply(val);

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

    @Override
    public void divideColumn(final String columnName, final Collection<String> newColumnNames, final BiConsumer<?, Object[]> output) {
        checkFrozen();

        final int columnIndex = checkColumnName(columnName);

        if (N.isEmpty(newColumnNames)) {
            throw new IllegalArgumentException("New column names can't be null or empty.");
        }

        if (!N.disjoint(_columnNameList, newColumnNames)) {
            throw new IllegalArgumentException("Column names: " + N.intersection(_columnNameList, newColumnNames) + " already are included in this data set.");
        }

        final BiConsumer<Object, Object[]> outputToUse = (BiConsumer<Object, Object[]>) output;
        final int newColumnsLen = newColumnNames.size();
        final List<List<Object>> newColumns = new ArrayList<>(newColumnsLen);

        for (int i = 0; i < newColumnsLen; i++) {
            newColumns.add(new ArrayList<>(size()));
        }

        final List<Object> column = _columnList.get(columnIndex);
        final Object[] tmp = new Object[newColumnsLen];

        for (final Object val : column) {
            outputToUse.accept(val, tmp);

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

    @Override
    public void divideColumn(final String columnName, final Tuple2<String, String> newColumnNames, final BiConsumer<?, Pair<Object, Object>> output) {
        checkFrozen();

        final int columnIndex = checkColumnName(columnName);
        checkNewColumnName(newColumnNames._1);
        checkNewColumnName(newColumnNames._2);

        final BiConsumer<Object, Pair<Object, Object>> outputToUse = (BiConsumer<Object, Pair<Object, Object>>) output;
        final List<Object> newColumn1 = new ArrayList<>(size());
        final List<Object> newColumn2 = new ArrayList<>(size());

        final List<Object> column = _columnList.get(columnIndex);
        final Pair<Object, Object> tmp = new Pair<>();

        for (final Object val : column) {
            outputToUse.accept(val, tmp);

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

    @Override
    public void divideColumn(final String columnName, final Tuple3<String, String, String> newColumnNames,
            final BiConsumer<?, Triple<Object, Object, Object>> output) {
        checkFrozen();

        final int columnIndex = checkColumnName(columnName);
        checkNewColumnName(newColumnNames._1);
        checkNewColumnName(newColumnNames._2);
        checkNewColumnName(newColumnNames._3);

        final BiConsumer<Object, Triple<Object, Object, Object>> outputToUse = (BiConsumer<Object, Triple<Object, Object, Object>>) output;
        final List<Object> newColumn1 = new ArrayList<>(size());
        final List<Object> newColumn2 = new ArrayList<>(size());
        final List<Object> newColumn3 = new ArrayList<>(size());

        final List<Object> column = _columnList.get(columnIndex);
        final Triple<Object, Object, Object> tmp = new Triple<>();

        for (final Object val : column) {
            outputToUse.accept(val, tmp);

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

    @Override
    public void addRow(final Object row) {
        addRow(size(), row);
    }

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

            if (a.length < columnCount()) {
                throw new IllegalArgumentException("The size of array (" + a.length + ") is less than the size of column (" + columnCount() + ")");
            }

            if (newRowPosition == size()) {
                for (int i = 0, len = columnCount(); i < len; i++) {
                    _columnList.get(i).add(a[i]);
                }
            } else {
                for (int i = 0, len = columnCount(); i < len; i++) {
                    _columnList.get(i).add(newRowPosition, a[i]);
                }
            }
        } else if (rowType.isCollection()) {
            final Collection<Object> c = (Collection<Object>) row;

            if (c.size() < columnCount()) {
                throw new IllegalArgumentException("The size of collection (" + c.size() + ") is less than the size of column (" + columnCount() + ")");
            }

            final Iterator<Object> it = c.iterator();

            if (newRowPosition == size()) {
                for (int i = 0, len = columnCount(); i < len; i++) {
                    _columnList.get(i).add(it.next());
                }
            } else {
                for (int i = 0, len = columnCount(); i < len; i++) {
                    _columnList.get(i).add(newRowPosition, it.next());
                }
            }
        } else if (rowType.isMap()) {
            final Map<String, Object> map = (Map<String, Object>) row;
            final Object[] a = new Object[columnCount()];

            int idx = 0;
            for (final String columnName : _columnNameList) {
                a[idx] = map.get(columnName);

                if (a[idx] == null && !map.containsKey(columnName)) {
                    throw new IllegalArgumentException("Column (" + columnName + ") is not found in map (" + map.keySet() + ")");
                }

                idx++;
            }

            if (newRowPosition == size()) {
                for (int i = 0, len = columnCount(); i < len; i++) {
                    _columnList.get(i).add(a[i]);
                }
            } else {
                for (int i = 0, len = columnCount(); i < len; i++) {
                    _columnList.get(i).add(newRowPosition, a[i]);
                }
            }
        } else if (rowType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowClass);
            final Object[] a = new Object[columnCount()];
            PropInfo propInfo = null;
            int idx = 0;

            for (final String columnName : _columnNameList) {
                propInfo = beanInfo.getPropInfo(columnName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Column (" + columnName + ") is not found in bean (" + rowClass + ")");
                }

                a[idx++] = propInfo.getPropValue(row);
            }

            if (newRowPosition == size()) {
                for (int i = 0, len = columnCount(); i < len; i++) {
                    _columnList.get(i).add(a[i]);
                }
            } else {
                for (int i = 0, len = columnCount(); i < len; i++) {
                    _columnList.get(i).add(newRowPosition, a[i]);
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and bean class are supported");
        }

        modCount++;
    }

    @Override
    public void removeRow(final int rowIndex) {
        checkFrozen();

        checkRowNum(rowIndex);

        for (final List<Object> objects : _columnList) {
            objects.remove(rowIndex);
        }

        modCount++;
    }

    @Override
    public final void removeRows(final int... indices) {
        checkFrozen();

        for (final int rowIndex : indices) {
            checkRowNum(rowIndex);
        }

        for (final List<Object> element : _columnList) {
            N.deleteAllByIndices(element, indices);
        }

        modCount++;
    }

    @Override
    public void removeRowRange(final int inclusiveFromRowIndex, final int exclusiveToRowIndex) {
        checkFrozen();

        this.checkRowIndex(inclusiveFromRowIndex, exclusiveToRowIndex);

        for (final List<Object> objects : _columnList) {
            objects.subList(inclusiveFromRowIndex, exclusiveToRowIndex).clear();
        }

        modCount++;
    }

    @Override
    public void updateRow(final int rowIndex, final Function<?, ?> func) {
        checkFrozen();

        checkRowNum(rowIndex);

        final Function<Object, Object> funcToUse = (Function<Object, Object>) func;

        for (final List<Object> column : _columnList) {
            column.set(rowIndex, funcToUse.apply(column.get(rowIndex)));
        }

        modCount++;
    }

    @Override
    public void updateRows(final int[] indices, final Function<?, ?> func) {
        checkFrozen();

        for (final int rowIndex : indices) {
            checkRowNum(rowIndex);
        }

        final Function<Object, Object> funcToUse = (Function<Object, Object>) func;

        for (final List<Object> column : _columnList) {
            for (final int rowIndex : indices) {
                column.set(rowIndex, funcToUse.apply(column.get(rowIndex)));
            }
        }

        modCount++;
    }

    @Override
    public void updateAll(final Function<?, ?> func) {
        checkFrozen();

        final Function<Object, Object> funcToUse = (Function<Object, Object>) func;
        final int size = size();

        for (final List<Object> column : _columnList) {
            for (int i = 0; i < size; i++) {
                column.set(i, funcToUse.apply(column.get(i)));
            }
        }

        modCount++;
    }

    @Override
    public void replaceIf(final Predicate<?> predicate, final Object newValue) {
        checkFrozen();

        final Predicate<Object> predicateToUse = (Predicate<Object>) predicate;
        final int size = size();
        Object val = null;

        for (final List<Object> column : _columnList) {
            for (int i = 0; i < size; i++) {
                val = column.get(i);

                column.set(i, predicateToUse.test(val) ? newValue : val);
            }
        }

        modCount++;
    }

    @Override
    public void prepend(final DataSet other) {
        checkFrozen();
        checkIfColumnNamesAreSame(other, true);

        final int[] columnIndexesForOther = getColumnIndexes(other.columnNameList());

        for (int i = 0, len = columnIndexesForOther.length; i < len; i++) {
            _columnList.get(columnIndexesForOther[i]).addAll(0, other.getColumn(i));
        }

        mergeProperties(other.properties());

        modCount++;
    }

    @Override
    public void append(final DataSet other) {
        checkFrozen();
        checkIfColumnNamesAreSame(other, true);

        final int[] columnIndexesForOther = getColumnIndexes(other.columnNameList());

        for (int i = 0, len = columnIndexesForOther.length; i < len; i++) {
            _columnList.get(columnIndexesForOther[i]).addAll(other.getColumn(i));
        }

        mergeProperties(other.properties());

        modCount++;
    }

    private void mergeProperties(final Map<String, Object> properties) {
        if (N.notEmpty(properties)) {
            if (_properties == N.<String, Object> emptyMap()) {
                _properties = Maps.newOrderingMap(properties);
            }

            _properties.putAll(properties);
        }
    }

    @Override
    public int currentRowNum() {
        return _currentRowNum;
    }

    @Override
    public DataSet absolute(final int rowNum) {
        checkRowNum(rowNum);

        _currentRowNum = rowNum;

        return this;
    }

    @Override
    public Object[] getRow(final int rowIndex) {
        return getRow(rowIndex, Object[].class);
    }

    @Override
    public <T> T getRow(final int rowIndex, final Class<? extends T> rowType) {
        return getRow(rowIndex, _columnNameList, rowType);
    }

    @Override
    public <T> T getRow(final int rowIndex, final Collection<String> columnNames, final Class<? extends T> rowType) {
        return getRow(rowIndex, columnNames, rowType, null);
    }

    @Override
    public <T> T getRow(final int rowIndex, final IntFunction<? extends T> rowSupplier) {
        return getRow(rowIndex, _columnNameList, rowSupplier);
    }

    @Override
    public <T> T getRow(final int rowIndex, final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier) {
        return getRow(rowIndex, columnNames, null, rowSupplier);
    }

    private <T> T getRow(final int rowIndex, final Collection<String> columnNames, Class<? extends T> rowClass, IntFunction<? extends T> rowSupplier) {
        checkRowNum(rowIndex);

        final int[] columnIndexes = checkColumnNames(columnNames);
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

            for (final int columnIndex : columnIndexes) {
                result.add(_columnList.get(columnIndex).get(rowIndex));
            }

            return (T) result;
        } else if (rowType.isMap()) {
            final Map<String, Object> result = (Map<String, Object>) rowSupplier.apply(columnCount);

            for (final int columnIndex : columnIndexes) {
                result.put(_columnNameList.get(columnIndex), _columnList.get(columnIndex).get(rowIndex));
            }

            return (T) result;
        } else if (rowType.isBean()) {
            final boolean ignoreUnmatchedProperty = isColumnNameList(columnNames);
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
                        columnName = _columnNameList.get(columnIndexes[j]);

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

                    final Object propValue = tmp.getRow(propBeanInfo, rowIndex, newTmpColumnNameList, tmp.checkColumnNames(newTmpColumnNameList),
                            newTmpColumnNameList.size(), prefixAndFieldNameMap, propBeanClass, propBeanType, null);

                    if (propInfo.type.isCollection()) {
                        @SuppressWarnings("rawtypes")
                        final Collection<Object> c = N.newCollection((Class) propInfo.clazz);
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

    @Override
    public Optional<Object[]> firstRow() {
        return firstRow(Object[].class);
    }

    @Override
    public <T> Optional<T> firstRow(final Class<? extends T> rowType) {
        return firstRow(_columnNameList, rowType);
    }

    @Override
    public <T> Optional<T> firstRow(final Collection<String> columnNames, final Class<? extends T> rowType) {
        return size() == 0 ? (Optional<T>) Optional.empty() : Optional.of(getRow(0, columnNames, rowType));
    }

    @Override
    public <T> Optional<T> firstRow(final IntFunction<? extends T> rowSupplier) {
        return firstRow(_columnNameList, rowSupplier);
    }

    @Override
    public <T> Optional<T> firstRow(final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier) {
        if (size() == 0) {
            return Optional.empty();
        }

        final T row = getRow(0, columnNames, rowSupplier);

        return Optional.of(row);
    }

    @Override
    public Optional<Object[]> lastRow() {
        return lastRow(Object[].class);
    }

    @Override
    public <T> Optional<T> lastRow(final Class<? extends T> rowType) {
        return lastRow(_columnNameList, rowType);
    }

    @Override
    public <T> Optional<T> lastRow(final Collection<String> columnNames, final Class<? extends T> rowType) {
        return size() == 0 ? (Optional<T>) Optional.empty() : Optional.of(getRow(size() - 1, columnNames, rowType));
    }

    @Override
    public <T> Optional<T> lastRow(final IntFunction<? extends T> rowSupplier) {
        return lastRow(_columnNameList, rowSupplier);
    }

    @Override
    public <T> Optional<T> lastRow(final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier) {
        if (size() == 0) {
            return Optional.empty();
        }

        final T row = getRow(size() - 1, columnNames, rowSupplier);

        return Optional.of(row);
    }

    @Override
    public <A, B> BiIterator<A, B> iterator(final String columnNameA, final String columnNameB) {
        return iterator(0, size(), columnNameA, columnNameB);
    }

    @Override
    public <A, B> BiIterator<A, B> iterator(final int fromRowIndex, final int toRowIndex, final String columnNameA, final String columnNameB) {
        this.checkRowIndex(fromRowIndex, toRowIndex);
        final List<Object> columnA = _columnList.get(checkColumnName(columnNameA));
        final List<Object> columnB = _columnList.get(checkColumnName(columnNameB));

        final IntObjConsumer<Pair<A, B>> output = new IntObjConsumer<>() {
            private final int expectedModCount = modCount;

            @Override
            public void accept(final int rowIndex, final Pair<A, B> output) {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }

                output.set((A) columnA.get(rowIndex), (B) columnB.get(rowIndex));
            }
        };

        return BiIterator.generate(fromRowIndex, toRowIndex, output);
    }

    @Override
    public <A, B, C> TriIterator<A, B, C> iterator(final String columnNameA, final String columnNameB, final String columnNameC) {
        return iterator(0, size(), columnNameA, columnNameB, columnNameC);
    }

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
            public void accept(final int rowIndex, final Triple<A, B, C> output) {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }

                output.set((A) columnA.get(rowIndex), (B) columnB.get(rowIndex), (C) columnC.get(rowIndex));
            }
        };

        return TriIterator.generate(fromRowIndex, toRowIndex, output);
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.Consumer<? super DisposableObjArray, E> action) throws E {
        forEach(_columnNameList, action);
    }

    @Override
    public <E extends Exception> void forEach(final Collection<String> columnNames, final Throwables.Consumer<? super DisposableObjArray, E> action) throws E {
        forEach(0, size(), columnNames, action);
    }

    @Override
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final Throwables.Consumer<? super DisposableObjArray, E> action)
            throws E {
        forEach(fromRowIndex, toRowIndex, _columnNameList, action);
    }

    @Override
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Throwables.Consumer<? super DisposableObjArray, E> action) throws IllegalArgumentException, E {
        final int[] columnIndexes = checkColumnNames(columnNames);
        checkRowIndex(fromRowIndex < toRowIndex ? fromRowIndex : (toRowIndex == -1 ? 0 : toRowIndex), Math.max(fromRowIndex, toRowIndex));
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

    @Override
    public <E extends Exception> void forEach(final Tuple2<String, String> columnNames, final Throwables.BiConsumer<?, ?, E> action)
            throws IllegalArgumentException, E {
        forEach(0, size(), columnNames, action);
    }

    @Override
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final Tuple2<String, String> columnNames,
            final Throwables.BiConsumer<?, ?, E> action) throws E {
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));

        checkRowIndex(fromRowIndex < toRowIndex ? fromRowIndex : (toRowIndex == -1 ? 0 : toRowIndex), Math.max(fromRowIndex, toRowIndex));
        N.checkArgNotNull(action);

        if (size() == 0) {
            return;
        }

        final Throwables.BiConsumer<Object, Object, E> actionToUse = (Throwables.BiConsumer<Object, Object, E>) action;

        if (fromRowIndex <= toRowIndex) {
            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                actionToUse.accept(column1.get(rowIndex), column2.get(rowIndex));
            }
        } else {
            for (int rowIndex = N.min(size() - 1, fromRowIndex); rowIndex > toRowIndex; rowIndex--) {
                actionToUse.accept(column1.get(rowIndex), column2.get(rowIndex));
            }
        }
    }

    @Override
    public <E extends Exception> void forEach(final Tuple3<String, String, String> columnNames, final Throwables.TriConsumer<?, ?, ?, E> action) throws E {
        forEach(0, size(), columnNames, action);
    }

    @Override
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final Tuple3<String, String, String> columnNames,
            final Throwables.TriConsumer<?, ?, ?, E> action) throws IllegalArgumentException, E {
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(columnNames._3));

        checkRowIndex(fromRowIndex < toRowIndex ? fromRowIndex : (toRowIndex == -1 ? 0 : toRowIndex), Math.max(fromRowIndex, toRowIndex));
        N.checkArgNotNull(action);

        if (size() == 0) {
            return;
        }

        final Throwables.TriConsumer<Object, Object, Object, E> actionToUse = (Throwables.TriConsumer<Object, Object, Object, E>) action;

        if (fromRowIndex <= toRowIndex) {
            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                actionToUse.accept(column1.get(rowIndex), column2.get(rowIndex), column3.get(rowIndex));
            }
        } else {
            for (int rowIndex = N.min(size() - 1, fromRowIndex); rowIndex > toRowIndex; rowIndex--) {
                actionToUse.accept(column1.get(rowIndex), column2.get(rowIndex), column3.get(rowIndex));
            }
        }
    }

    @Override
    public List<Object[]> toList() {
        return toList(Object[].class);
    }

    @Override
    public List<Object[]> toList(final int fromRowIndex, final int toRowIndex) {
        return toList(fromRowIndex, toRowIndex, Object[].class);
    }

    @Override
    public <T> List<T> toList(final Class<? extends T> rowType) {
        return toList(0, size(), rowType);
    }

    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Class<? extends T> rowType) {
        return toList(fromRowIndex, toRowIndex, _columnNameList, rowType);
    }

    @Override
    public <T> List<T> toList(final Collection<String> columnNames, final Class<? extends T> rowType) {
        return toList(0, size(), columnNames, rowType);
    }

    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Class<? extends T> rowType) {
        return toList(fromRowIndex, toRowIndex, columnNames, null, rowType, null);
    }

    @Override
    public <T> List<T> toList(final IntFunction<? extends T> rowSupplier) {
        return toList(_columnNameList, rowSupplier);
    }

    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final IntFunction<? extends T> rowSupplier) {
        return toList(fromRowIndex, toRowIndex, _columnNameList, rowSupplier);
    }

    @Override
    public <T> List<T> toList(final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier) {
        return toList(0, size(), columnNames, rowSupplier);
    }

    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier) {
        return toList(fromRowIndex, toRowIndex, columnNames, null, null, rowSupplier);
    }

    private <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowClass, IntFunction<? extends T> rowSupplier) {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int[] columnIndexes = checkColumnNames(columnNames);

        rowClass = rowClass == null ? (Class<T>) rowSupplier.apply(0).getClass() : rowClass;
        final Type<?> rowType = N.typeOf(rowClass);

        if (rowType.isBean()) {
            return toEntities(ParserUtil.getBeanInfo(rowClass), fromRowIndex, toRowIndex, null, columnNames, prefixAndFieldNameMap, false, true, rowClass,
                    rowSupplier);
        }

        rowSupplier = rowSupplier == null && !rowType.isBean() ? this.createRowSupplier(rowClass, rowType) : rowSupplier;

        final int columnCount = columnIndexes.length;
        final int rowCount = toRowIndex - fromRowIndex;

        final List<Object> rowList = new ArrayList<>(rowCount);

        if (rowType.isObjectArray()) {
            Object[] row = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                //noinspection DataFlowIssue
                row = (Object[]) rowSupplier.apply(columnCount);

                for (int i = 0; i < columnCount; i++) {
                    row[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
                }

                rowList.add(row);
            }
        } else if (rowType.isCollection()) {
            Collection<Object> row = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                //noinspection DataFlowIssue
                row = (Collection<Object>) rowSupplier.apply(columnCount);

                for (final int columnIndex : columnIndexes) {
                    row.add(_columnList.get(columnIndex).get(rowIndex));
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
                //noinspection DataFlowIssue
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
        } else if (rowType.isCollection()) {
            return (IntFunction<T>) Factory.ofCollection((Class<Collection>) rowClass);

        } else if (rowType.isMap()) {
            return (IntFunction<T>) Factory.ofMap((Class<Map>) rowClass);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and bean class are supported");
        }
    }

    @Override
    public <T> List<T> toList(final Predicate<? super String> columnNameFilter, final Function<? super String, String> columnNameConverter,
            final Class<? extends T> rowType) {
        return toList(0, size(), columnNameFilter, columnNameConverter, rowType);
    }

    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Predicate<? super String> columnNameFilter,
            final Function<? super String, String> columnNameConverter, final Class<? extends T> rowType) {
        checkRowIndex(fromRowIndex, toRowIndex);

        if ((columnNameFilter == null || Objects.equals(columnNameFilter, Fn.alwaysTrue()))
                && (columnNameConverter == null || Objects.equals(columnNameConverter, Fn.identity()))) {
            return toList(fromRowIndex, toRowIndex, _columnNameList, rowType);
        }

        final Predicate<? super String> columnNameFilterToBeUsed = columnNameFilter == null ? Fn.alwaysTrue() : columnNameFilter;
        final Function<? super String, String> columnNameConverterToBeUsed = columnNameConverter == null ? Fn.identity() : columnNameConverter;

        final List<String> newColumnNameList = new ArrayList<>();
        final List<List<Object>> newColumnList = new ArrayList<>();
        String columnName = null;

        for (int i = 0, columnCount = columnCount(); i < columnCount; i++) {
            columnName = _columnNameList.get(i);

            if (columnNameFilterToBeUsed.test(columnName)) {
                newColumnNameList.add(columnNameConverterToBeUsed.apply(columnName));
                newColumnList.add(_columnList.get(i));
            }
        }

        final RowDataSet tmp = new RowDataSet(newColumnNameList, newColumnList);

        return tmp.toList(fromRowIndex, toRowIndex, rowType);
    }

    @Override
    public <T> List<T> toList(final Predicate<? super String> columnNameFilter, final Function<? super String, String> columnNameConverter,
            final IntFunction<? extends T> rowSupplier) {
        return toList(0, size(), columnNameFilter, columnNameConverter, rowSupplier);
    }

    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Predicate<? super String> columnNameFilter,
            final Function<? super String, String> columnNameConverter, final IntFunction<? extends T> rowSupplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        if ((columnNameFilter == null || Objects.equals(columnNameFilter, Fn.alwaysTrue()))
                && (columnNameConverter == null || Objects.equals(columnNameConverter, Fn.identity()))) {
            return toList(fromRowIndex, toRowIndex, _columnNameList, rowSupplier);
        }

        final Predicate<? super String> columnNameFilterToBeUsed = columnNameFilter == null ? Fn.alwaysTrue() : columnNameFilter;
        final Function<? super String, String> columnNameConverterToBeUsed = columnNameConverter == null ? Fn.identity() : columnNameConverter;

        final List<String> newColumnNameList = new ArrayList<>();
        final List<List<Object>> newColumnList = new ArrayList<>();
        String columnName = null;

        for (int i = 0, columnCount = columnCount(); i < columnCount; i++) {
            columnName = _columnNameList.get(i);

            if (columnNameFilterToBeUsed.test(columnName)) {
                newColumnNameList.add(columnNameConverterToBeUsed.apply(columnName));
                newColumnList.add(_columnList.get(i));
            }
        }

        final RowDataSet tmp = new RowDataSet(newColumnNameList, newColumnList);

        return tmp.toList(fromRowIndex, toRowIndex, rowSupplier);
    }

    @Override
    public <T> List<T> toEntities(final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType) {
        return toEntities(0, size(), _columnNameList, prefixAndFieldNameMap, rowType);
    }

    @Override
    public <T> List<T> toEntities(final int fromRowIndex, final int toRowIndex, final Map<String, String> prefixAndFieldNameMap,
            final Class<? extends T> rowType) {
        return toEntities(fromRowIndex, toRowIndex, _columnNameList, prefixAndFieldNameMap, rowType);
    }

    @Override
    public <T> List<T> toEntities(final Collection<String> columnNames, final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType) {
        return toEntities(0, size(), columnNames, prefixAndFieldNameMap, rowType);
    }

    @Override
    public <T> List<T> toEntities(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType) throws IllegalArgumentException {
        N.checkArgument(ClassUtil.isBeanClass(rowType), "{} is not a bean class", rowType);

        return toList(fromRowIndex, toRowIndex, columnNames, prefixAndFieldNameMap, rowType, null);
    }

    @Override
    public <T> List<T> toMergedEntities(final Class<? extends T> rowType) {
        return toMergedEntities(_columnNameList, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final Collection<String> selectPropNames, final Class<? extends T> rowType) {
        return toMergedEntities(ParserUtil.getBeanInfo(rowType).idPropNameList, selectPropNames, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final String idPropName, final Class<? extends T> rowType) {
        return toMergedEntities(idPropName, _columnNameList, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final String idPropName, final Collection<String> selectPropNames, final Class<? extends T> rowType) {
        return toMergedEntities(N.asList(idPropName), selectPropNames, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final Collection<String> idPropNames, final Collection<String> selectPropNames, final Class<? extends T> rowType) {
        return toMergedEntities(idPropNames, selectPropNames, null, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final Collection<String> idPropNames, final Collection<String> selectPropNames,
            final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType) throws IllegalArgumentException {
        N.checkArgument(ClassUtil.isBeanClass(rowType), "{} is not a bean class", rowType);
        N.checkArgNotEmpty(idPropNames, "idPropNames can't be null or empty or No id property defined in bean class: " + rowType);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowType);
        Collection<String> idPropNamesToUse = idPropNames;

        //noinspection SlowListContainsAll
        if (!_columnNameList.containsAll(idPropNamesToUse)) {
            final List<String> tmp = new ArrayList<>(idPropNamesToUse.size());
            PropInfo propInfo = null;

            outer: for (final String idPropName : idPropNamesToUse) { //NOSONAR
                if (_columnNameList.contains(idPropName)) {
                    tmp.add(idPropName);
                } else {
                    propInfo = beanInfo.getPropInfo(idPropName);

                    if (propInfo != null && propInfo.columnName.isPresent() && _columnNameList.contains(propInfo.columnName.get())) {
                        tmp.add(propInfo.columnName.get());
                    } else {
                        for (final String columnName : _columnNameList) {
                            if (columnName.equalsIgnoreCase(idPropName)) {
                                tmp.add(columnName);

                                continue outer;
                            }
                        }

                        if (propInfo != null) {
                            for (final String columnName : _columnNameList) {
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

            //noinspection SlowListContainsAll
            if (_columnNameList.containsAll(tmp)) {
                idPropNamesToUse = tmp;
            }
        }

        //noinspection SlowListContainsAll
        N.checkArgument(_columnNameList.containsAll(idPropNamesToUse), "Some id properties {} are not found in DataSet: {} for bean {}", idPropNamesToUse,
                _columnNameList, ClassUtil.getSimpleClassName(rowType));

        return toEntities(beanInfo, 0, size(), idPropNamesToUse, selectPropNames, prefixAndFieldNameMap, true, false, rowType, null);
    }

    @SuppressWarnings("rawtypes")
    private <T> List<T> toEntities(final BeanInfo beanInfo, final int fromRowIndex, final int toRowIndex, final Collection<String> idPropNames,
            final Collection<String> columnNames, final Map<String, String> prefixAndFieldNameMap, final boolean mergeResult, final boolean returnAllList,
            final Class<? extends T> rowType, final IntFunction<? extends T> rowSupplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        N.checkArgNotNull(rowType, cs.rowType);
        N.checkArgNotEmpty(columnNames, cs.columnNames);

        if (mergeResult && N.isEmpty(idPropNames)) {
            throw new IllegalArgumentException("\"idPropNames\" can't be null or empty when \"mergeResult\" is true");
        }

        final int rowCount = toRowIndex - fromRowIndex;
        final int columnCount = columnNames.size();
        final int[] idColumnIndexes = N.isEmpty(idPropNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(idPropNames);
        final boolean ignoreUnmatchedProperty = isColumnNameList(columnNames);

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
            Wrapper<Object[]> keyRowWrapper = Wrapper.of(keyRow);
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

                bean = idBeanMap.get(keyRowWrapper);

                if (bean == null) {
                    bean = rowSupplier == null ? beanInfo.createBeanResult() : rowSupplier.apply(columnCount);
                    idBeanMap.put(keyRowWrapper, bean);

                    keyRow = Objectory.createObjectArray(idColumnCount);
                    keyRowWrapper = Wrapper.of(keyRow);
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

            for (final String propName : columnNames) {
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

                    for (final String columnName : columnNames) {
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

                    @SuppressWarnings("SlowListContainsAll")
                    boolean isToMerge = mergeResult && N.notEmpty(newPropEntityIdNames) && tmp._columnNameList.containsAll(newPropEntityIdNames);

                    if (isToMerge) {
                        for (int i = 0, idPropCount = propBeanInfo.idPropInfoList.size(); i < idPropCount; i++) {
                            final PropInfo idPropInfo = propBeanInfo.idPropInfoList.get(i);
                            final Object defaultIdPropValue = idPropInfo.type.defaultValue();
                            final List<Object> idColumn = tmp._columnList.get(tmp.getColumnIndex(newPropEntityIdNames.get(i)));

                            //noinspection resource
                            if (!Stream.of(idColumn).nMatch(0, 1, it -> N.equals(it, defaultIdPropValue))) { // two or more rows have the same id value.
                                isToMerge = false;
                                break;
                            }
                        }
                    }

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
                for (final Collection<Object> list : listPropValuesToDeduplicate) {
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
                for (final Wrapper<Object[]> rw : ((Map<Wrapper<Object[]>, Object>) ((Map) idBeanMap)).keySet()) {
                    Objectory.recycle(rw.value());
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
            //noinspection resource
            propInfo = Stream.of(beanInfo.propInfoList)
                    .filter(it -> it.tablePrefix.isPresent() && it.tablePrefix.orElseThrow().equals(prefix))
                    .onlyOne()
                    .orElse(null);
        }

        if (propInfo == null) {
            propInfo = beanInfo.getPropInfo(prefix + "s"); // Trying to do something smart?
            final int len = prefix.length() + 1;

            if (propInfo != null && (propInfo.type.isBean() || (propInfo.type.isCollection() && propInfo.type.getElementType().isBean()))
                    && N.noneMatch(_columnNameList, it -> it.length() > len && it.charAt(len) == '.' && Strings.startsWithIgnoreCase(it, prefix + "s."))) {
                // good
            } else {
                propInfo = beanInfo.getPropInfo(prefix + "es"); // Trying to do something smart?
                final int len2 = prefix.length() + 2;

                if (propInfo != null && (propInfo.type.isBean() || (propInfo.type.isCollection() && propInfo.type.getElementType().isBean())) && N
                        .noneMatch(_columnNameList, it -> it.length() > len2 && it.charAt(len2) == '.' && Strings.startsWithIgnoreCase(it, prefix + "es."))) {
                    // good
                } else {
                    // Sorry, have done all I can do.
                    propInfo = null;
                }
            }
        }

        return propInfo;
    }

    @Override
    public <K, V> Map<K, V> toMap(final String keyColumnName, final String valueColumnName) {
        return toMap(0, size(), keyColumnName, valueColumnName);
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(final String keyColumnName, final String valueColumnName, final IntFunction<? extends M> supplier) {
        return toMap(0, size(), keyColumnName, valueColumnName, supplier);
    }

    @Override
    public <K, V> Map<K, V> toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final String valueColumnName) {
        return toMap(fromRowIndex, toRowIndex, keyColumnName, valueColumnName, N::newLinkedHashMap);
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final String valueColumnName,
            final IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int valueColumnIndex = checkColumnName(valueColumnName);

        final M resultMap = supplier.apply(toRowIndex - fromRowIndex);

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (V) _columnList.get(valueColumnIndex).get(rowIndex));
        }

        return resultMap;
    }

    @Override
    public <K, V> Map<K, V> toMap(final String keyColumnName, final Collection<String> valueColumnNames, final Class<? extends V> rowType) {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowType);
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(final String keyColumnName, final Collection<String> valueColumnNames, final Class<? extends V> rowType,
            final IntFunction<? extends M> supplier) {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowType, supplier);
    }

    @Override
    public <K, V> Map<K, V> toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final Collection<String> valueColumnNames,
            final Class<? extends V> rowType) {
        return toMap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowType, N::newLinkedHashMap);
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final Class<? extends V> rowType, final IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnNames(valueColumnNames);

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
        } else if (valueType.isCollection()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowType.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType);
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = isAbstractRowClass ? (valueType.isList() ? new ArrayList<>(valueColumnCount) : N.newHashSet(valueColumnCount))
                        : ((intConstructor == null) ? (Collection<Object>) ClassUtil.invokeConstructor(constructor)
                                : (Collection<Object>) ClassUtil.invokeConstructor(intConstructor, valueColumnCount));

                for (final int columIndex : valueColumnIndexes) {
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
                value = isAbstractRowClass ? N.newHashMap(valueColumnCount)
                        : (intConstructor == null ? (Map<String, Object>) ClassUtil.invokeConstructor(constructor)
                                : (Map<String, Object>) ClassUtil.invokeConstructor(intConstructor, valueColumnCount));

                for (final int columIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columIndex), _columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowType);
            final boolean ignoreUnmatchedProperty = isColumnNameList(valueColumnNames);
            Object value = null;
            String propName = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = beanInfo.createBeanResult();

                for (final int columIndex : valueColumnIndexes) {
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

    @Override
    public <K, V> Map<K, V> toMap(final String keyColumnName, final Collection<String> valueColumnNames, final IntFunction<? extends V> rowSupplier) {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowSupplier);
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(final String keyColumnName, final Collection<String> valueColumnNames,
            final IntFunction<? extends V> rowSupplier, final IntFunction<? extends M> supplier) {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowSupplier, supplier);
    }

    @Override
    public <K, V> Map<K, V> toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final Collection<String> valueColumnNames,
            final IntFunction<? extends V> rowSupplier) {
        return toMap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowSupplier, N::newLinkedHashMap);
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final IntFunction<? extends V> rowSupplier, final IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnNames(valueColumnNames);

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
        } else if (valueType.isCollection()) {
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Collection<Object>) rowSupplier.apply(valueColumnCount);

                for (final int columIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isMap()) {
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Map<String, Object>) rowSupplier.apply(valueColumnCount);

                for (final int columIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columIndex), _columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isBean()) {
            final boolean ignoreUnmatchedProperty = isColumnNameList(valueColumnNames);
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowClass);
            Object value = null;
            String propName = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = rowSupplier.apply(valueColumnCount);

                for (final int columIndex : valueColumnIndexes) {
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

    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final String keyColumnName, final String valueColumnName) {
        return toMultimap(0, size(), keyColumnName, valueColumnName);
    }

    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final String keyColumnName, final String valueColumnName,
            final IntFunction<? extends M> supplier) {
        return toMultimap(0, size(), keyColumnName, valueColumnName, supplier);
    }

    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final String valueColumnName) {
        return toMultimap(fromRowIndex, toRowIndex, keyColumnName, valueColumnName, len -> N.newLinkedListMultimap());
    }

    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final String valueColumnName, final IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final M resultMap = supplier.apply(toRowIndex - fromRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int valueColumnIndex = checkColumnName(valueColumnName);

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) _columnList.get(valueColumnIndex).get(rowIndex));
        }

        return resultMap;
    }

    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final String keyColumnName, final Collection<String> valueColumnNames, final Class<? extends T> rowType) {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowType);
    }

    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final String keyColumnName, final Collection<String> valueColumnNames,
            final Class<? extends T> rowType, final IntFunction<? extends M> supplier) {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowType, supplier);
    }

    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final Class<? extends T> rowType) {
        return toMultimap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowType, len -> N.newLinkedListMultimap());
    }

    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final Class<? extends T> rowType, final IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnNames(valueColumnNames);

        final Type<?> valueType = N.typeOf(rowType);
        final int valueColumnCount = valueColumnIndexes.length;

        final M resultMap = supplier.apply(toRowIndex - fromRowIndex);

        if (valueType.isObjectArray()) {
            Object[] value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = N.newArray(rowType.getComponentType(), valueColumnCount);

                for (int i = 0; i < valueColumnCount; i++) {
                    value[i] = _columnList.get(valueColumnIndexes[i]).get(rowIndex);
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else if (valueType.isCollection()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowType.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType);
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = isAbstractRowClass ? (valueType.isList() ? new ArrayList<>(valueColumnCount) : N.newHashSet(valueColumnCount))
                        : ((intConstructor == null) ? (Collection<Object>) ClassUtil.invokeConstructor(constructor)
                                : (Collection<Object>) ClassUtil.invokeConstructor(intConstructor, valueColumnCount));

                for (final int columIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else if (valueType.isMap()) {
            final boolean isAbstractRowClass = Modifier.isAbstract(rowType.getModifiers());
            final Constructor<?> intConstructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType, int.class);
            final Constructor<?> constructor = isAbstractRowClass ? null : ClassUtil.getDeclaredConstructor(rowType);
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = isAbstractRowClass ? N.newHashMap(valueColumnCount)
                        : (intConstructor == null ? (Map<String, Object>) ClassUtil.invokeConstructor(constructor)
                                : (Map<String, Object>) ClassUtil.invokeConstructor(intConstructor, valueColumnCount));

                for (final int columIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columIndex), _columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else if (valueType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowType);
            final boolean ignoreUnmatchedProperty = isColumnNameList(valueColumnNames);
            Object value = null;
            String propName = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = beanInfo.createBeanResult();

                for (final int columIndex : valueColumnIndexes) {
                    propName = _columnNameList.get(columIndex);
                    beanInfo.setPropValue(value, propName, _columnList.get(columIndex).get(rowIndex), ignoreUnmatchedProperty);
                }

                value = beanInfo.finishBeanResult(value);

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowType.getCanonicalName() + ". Only Array, List/Set, Map and bean class are supported");
        }

        return resultMap;
    }

    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final String keyColumnName, final Collection<String> valueColumnNames,
            final IntFunction<? extends T> rowSupplier) {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowSupplier);
    }

    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final String keyColumnName, final Collection<String> valueColumnNames,
            final IntFunction<? extends T> rowSupplier, final IntFunction<? extends M> supplier) {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowSupplier, supplier);
    }

    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final IntFunction<? extends T> rowSupplier) {
        return toMultimap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowSupplier, len -> N.newLinkedListMultimap());
    }

    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final IntFunction<? extends T> rowSupplier, final IntFunction<? extends M> supplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnNames(valueColumnNames);

        final Class<?> rowClass = rowSupplier.apply(0).getClass();
        final Type<?> valueType = N.typeOf(rowClass);
        final int valueColumnCount = valueColumnIndexes.length;

        final M resultMap = supplier.apply(toRowIndex - fromRowIndex);

        if (valueType.isObjectArray()) {
            Object[] value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Object[]) rowSupplier.apply(valueColumnCount);

                for (int i = 0; i < valueColumnCount; i++) {
                    value[i] = _columnList.get(valueColumnIndexes[i]).get(rowIndex);
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else if (valueType.isCollection()) {
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Collection<Object>) rowSupplier.apply(valueColumnCount);

                for (final int columIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else if (valueType.isMap()) {
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Map<String, Object>) rowSupplier.apply(valueColumnCount);

                for (final int columIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columIndex), _columnList.get(columIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else if (valueType.isBean()) {
            final boolean ignoreUnmatchedProperty = isColumnNameList(valueColumnNames);
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowClass);
            String propName = null;
            T value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = rowSupplier.apply(valueColumnCount);

                for (final int columIndex : valueColumnIndexes) {
                    propName = _columnNameList.get(columIndex);
                    beanInfo.setPropValue(value, propName, _columnList.get(columIndex).get(rowIndex), ignoreUnmatchedProperty);
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowClass.getCanonicalName() + ". Only Array, List/Set, Map and bean class are supported");
        }

        return resultMap;
    }

    @Override
    public String toJson() {
        return toJson(0, size());
    }

    @Override
    public String toJson(final int fromRowIndex, final int toRowIndex) {
        return toJson(fromRowIndex, toRowIndex, _columnNameList);
    }

    @Override
    public String toJson(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        final BufferedJSONWriter writer = Objectory.createBufferedJSONWriter();

        try {
            toJson(fromRowIndex, toRowIndex, columnNames, writer);

            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Override
    public void toJson(final File output) {
        toJson(0, size(), output);
    }

    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final File output) {
        toJson(fromRowIndex, toRowIndex, _columnNameList, output);
    }

    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final File output) throws UncheckedIOException {
        Writer writer = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            toJson(fromRowIndex, toRowIndex, columnNames, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    @Override
    public void toJson(final OutputStream output) {
        toJson(0, size(), output);
    }

    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final OutputStream output) {
        toJson(fromRowIndex, toRowIndex, _columnNameList, output);
    }

    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final OutputStream output)
            throws UncheckedIOException {
        final BufferedJSONWriter writer = Objectory.createBufferedJSONWriter(output);

        try {
            toJson(fromRowIndex, toRowIndex, columnNames, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Override
    public void toJson(final Writer output) {
        toJson(0, size(), output);
    }

    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final Writer output) {
        toJson(fromRowIndex, toRowIndex, _columnNameList, output);
    }

    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Writer output) throws UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);

        if (N.isEmpty(columnNames)) {
            try {
                IOUtil.write("[]", output);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final int[] columnIndexes = checkColumnNames(columnNames);
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
                        } catch (final Exception e) {
                            // ignore.

                            strType.writeCharacter(bw, N.toString(element), jsc);
                        }
                    }
                }

                bw.write(WD._BRACE_R);
            }

            bw.write(WD._BRACKET_R);

            bw.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    @Override
    public String toXml() {
        return toXml(ROW);
    }

    @Override
    public String toXml(final String rowElementName) {
        return toXml(0, size(), N.checkArgNotEmpty(rowElementName, cs.rowElementName));
    }

    @Override
    public String toXml(final int fromRowIndex, final int toRowIndex) {
        return toXml(fromRowIndex, toRowIndex, ROW);
    }

    @Override
    public String toXml(final int fromRowIndex, final int toRowIndex, final String rowElementName) {
        return toXml(fromRowIndex, toRowIndex, _columnNameList, N.checkArgNotEmpty(rowElementName, cs.rowElementName));
    }

    @Override
    public String toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        return toXml(fromRowIndex, toRowIndex, columnNames, ROW);
    }

    @Override
    public String toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName) {
        final BufferedXMLWriter writer = Objectory.createBufferedXMLWriter();

        try {
            toXml(fromRowIndex, toRowIndex, columnNames, N.checkArgNotEmpty(rowElementName, cs.rowElementName), writer);

            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Override
    public void toXml(final File output) {
        toXml(0, size(), output);
    }

    @Override
    public void toXml(final String rowElementName, final File output) {
        toXml(0, size(), N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final File output) {
        toXml(fromRowIndex, toRowIndex, ROW, output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final String rowElementName, final File output) {
        toXml(fromRowIndex, toRowIndex, _columnNameList, N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final File output) {
        toXml(fromRowIndex, toRowIndex, columnNames, ROW, output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName, final File output)
            throws UncheckedIOException {
        Writer writer = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            toXml(fromRowIndex, toRowIndex, columnNames, N.checkArgNotEmpty(rowElementName, cs.rowElementName), writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    @Override
    public void toXml(final OutputStream output) {
        toXml(0, size(), output);
    }

    @Override
    public void toXml(final String rowElementName, final OutputStream output) {
        toXml(0, size(), N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final OutputStream output) {
        toXml(fromRowIndex, toRowIndex, ROW, output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final String rowElementName, final OutputStream output) {
        toXml(fromRowIndex, toRowIndex, _columnNameList, N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final OutputStream output) {
        toXml(fromRowIndex, toRowIndex, columnNames, ROW, output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName,
            final OutputStream output) throws UncheckedIOException {
        final BufferedXMLWriter writer = Objectory.createBufferedXMLWriter(output);

        try {
            toXml(fromRowIndex, toRowIndex, columnNames, N.checkArgNotEmpty(rowElementName, cs.rowElementName), writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Override
    public void toXml(final Writer output) {
        toXml(0, size(), output);
    }

    @Override
    public void toXml(final String rowElementName, final Writer output) {
        toXml(0, size(), N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Writer output) {
        toXml(fromRowIndex, toRowIndex, ROW, output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final String rowElementName, final Writer output) {
        toXml(fromRowIndex, toRowIndex, _columnNameList, N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Writer output) {
        toXml(fromRowIndex, toRowIndex, columnNames, ROW, output);
    }

    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName, final Writer output)
            throws UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotEmpty(rowElementName, cs.rowElementName);

        if (N.isEmpty(columnNames)) {
            try {
                IOUtil.write(XMLConstants.DATA_SET_ELE_START, output);
                IOUtil.write(XMLConstants.DATA_SET_ELE_END, output);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final int[] columnIndexes = checkColumnNames(columnNames);

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
                        } catch (final Exception e) {
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
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    @Override
    public String toCsv() {
        return toCsv(0, size(), columnNameList());
    }

    @Override
    public String toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        return toCsv(fromRowIndex, toRowIndex, columnNames, true, true);
    }

    @Override
    public String toCsv(final boolean writeTitle, final boolean quoted) {
        return toCsv(0, size(), columnNameList(), writeTitle, quoted);
    }

    @Override
    public String toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final boolean writeTitle, final boolean quoted) {
        final BufferedWriter bw = Objectory.createBufferedCSVWriter();

        try {
            toCsv(fromRowIndex, toRowIndex, columnNames, writeTitle, quoted, bw);

            return bw.toString();
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Override
    public void toCsv(final File output) {
        toCsv(0, size(), _columnNameList, output);
    }

    @Override
    public void toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final File output) {
        toCsv(fromRowIndex, toRowIndex, columnNames, true, true, output);
    }

    @Override
    public void toCsv(final boolean writeTitle, final boolean quoted, final File output) {
        toCsv(0, size(), _columnNameList, writeTitle, quoted, output);
    }

    @Override
    public void toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final boolean writeTitle, final boolean quoted,
            final File output) {
        Writer writer = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            toCsv(fromRowIndex, toRowIndex, columnNames, writeTitle, quoted, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    @Override
    public void toCsv(final OutputStream output) {
        toCsv(0, size(), _columnNameList, output);
    }

    @Override
    public void toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final OutputStream output) {
        toCsv(fromRowIndex, toRowIndex, columnNames, true, true, output);
    }

    @Override
    public void toCsv(final boolean writeTitle, final boolean quoted, final OutputStream output) {
        toCsv(0, size(), _columnNameList, writeTitle, quoted, output);
    }

    @Override
    public void toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final boolean writeTitle, final boolean quoted,
            final OutputStream output) throws UncheckedIOException {
        Writer writer = null;

        try {
            writer = IOUtil.newOutputStreamWriter(output); // NOSONAR

            toCsv(fromRowIndex, toRowIndex, columnNames, writeTitle, quoted, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void toCsv(final Writer output) {
        toCsv(0, size(), _columnNameList, output);
    }

    @Override
    public void toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Writer output) {
        toCsv(fromRowIndex, toRowIndex, columnNames, true, true, output);
    }

    @Override
    public void toCsv(final boolean writeTitle, final boolean quoted, final Writer output) {
        toCsv(0, size(), _columnNameList, writeTitle, quoted, output);
    }

    @Override
    public void toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final boolean writeTitle, final boolean quoted,
            final Writer output) throws UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);

        if (N.isEmpty(columnNames)) {
            return;
        }

        final int[] columnIndexes = checkColumnNames(columnNames);
        final int columnCount = columnIndexes.length;

        final JSONSerializationConfig config = JSC.create();
        config.setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

        final boolean isBufferedWriter = output instanceof BufferedCSVWriter;
        final BufferedCSVWriter bw = isBufferedWriter ? (BufferedCSVWriter) output : Objectory.createBufferedCSVWriter(output);

        final char separator = WD._COMMA;

        try {
            if (writeTitle) {
                for (int i = 0; i < columnCount; i++) {
                    if (i > 0) {
                        bw.write(separator);
                    }

                    // bw.write(getColumnName(columnIndexes[i]));
                    strType.writeCharacter(bw, getColumnName(columnIndexes[i]), config);
                }

                bw.write(IOUtil.LINE_SEPARATOR);
            }

            Object element = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                if (rowIndex > fromRowIndex) {
                    bw.write(IOUtil.LINE_SEPARATOR);
                }

                for (int i = 0; i < columnCount; i++) {
                    if (i > 0) {
                        bw.write(separator);
                    }

                    element = _columnList.get(columnIndexes[i]).get(rowIndex);

                    CSVUtil.writeField(bw, null, element);
                }
            }

            bw.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    @Override
    public DataSet groupBy(final String keyColumnName, final String aggregateOnColumnName, final String aggregateResultColumnName,
            final Collector<?, ?, ?> collector) {
        return groupBy(keyColumnName, NULL_PARAM_INDICATOR_1, aggregateOnColumnName, aggregateResultColumnName, collector);
    }

    @Override
    public DataSet groupBy(final String keyColumnName, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Class<?> rowType) {
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

    @Override
    public DataSet groupBy(final String keyColumnName, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Collector<? super Object[], ?, ?> collector) {
        return groupBy(keyColumnName, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    @Override
    public <T> DataSet groupBy(final String keyColumnName, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Function<? super DisposableObjArray, ? extends T> rowMapper, final Collector<? super T, ?, ?> collector) {
        return groupBy(keyColumnName, NULL_PARAM_INDICATOR_1, aggregateOnColumnNames, aggregateResultColumnName, rowMapper, collector);
    }

    private DataSet groupBy(final String keyColumnName, final Function<?, ?> keyExtractor) {
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

        final Function<Object, ?> keyExtractorToUse = (Function<Object, ?>) (keyExtractor == null ? Fn.identity() : keyExtractor);
        final List<Object> keyColumn = newColumnList.get(0);

        final Set<Object> keySet = N.newHashSet();
        final List<Object> groupByColumn = _columnList.get(columnIndex);
        Object value = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            value = groupByColumn.get(rowIndex);

            if (keySet.add(hashKey(keyExtractorToUse.apply(value)))) {
                keyColumn.add(value);
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    @Override
    public DataSet groupBy(final String keyColumnName, final Function<?, ?> keyExtractor, final String aggregateOnColumnName,
            final String aggregateResultColumnName, final Collector<?, ?, ?> collector) {
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

        final Function<Object, ?> keyExtractorToUse = (Function<Object, ?>) (keyExtractor == null ? Fn.identity() : keyExtractor);
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
            key = hashKey(keyExtractorToUse.apply(value));

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

    @Override
    public DataSet groupBy(final String keyColumnName, final Function<?, ?> keyExtractor, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Class<?> rowType) {
        final Function<Object, ?> keyExtractorToUse = (Function<Object, ?>) (keyExtractor == null ? Fn.identity() : keyExtractor);

        final List<Object> keyColumn = getColumn(keyColumnName);
        final List<Object> valueColumn = toList(aggregateOnColumnNames, rowType);

        final Map<Object, List<Object>> map = N.newLinkedHashMap(N.min(9, size()));
        final List<Object> keyList = new ArrayList<>(N.min(9, size()));
        Object key = null;
        List<Object> val = null;

        for (int i = 0, size = keyColumn.size(); i < size; i++) {
            key = hashKey(keyExtractorToUse.apply(keyColumn.get(i)));
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

    private static final Function<? super DisposableObjArray, Object[]> CLONE = DisposableObjArray::copy;
    private static final Function<?, ?> NULL_PARAM_INDICATOR_1 = null;
    private static final Function<? super DisposableObjArray, ?> NULL_PARAM_INDICATOR_2 = null;

    @Override
    public DataSet groupBy(final String keyColumnName, final Function<?, ?> keyExtractor, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector) {
        return groupBy(keyColumnName, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    @Override
    public <T> DataSet groupBy(final String keyColumnName, final Function<?, ?> keyExtractor, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Function<? super DisposableObjArray, ? extends T> rowMapper,
            final Collector<? super T, ?, ?> collector) throws IllegalArgumentException {
        final int columnIndex = checkColumnName(keyColumnName);
        final int[] aggOnColumnIndexes = checkColumnNames(aggregateOnColumnNames);

        if (N.equals(keyColumnName, aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicated Property name: " + aggregateResultColumnName);
        }

        N.checkArgNotNull(rowMapper, cs.rowMapper);
        N.checkArgNotNull(collector, cs.collector);

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

        final Function<Object, ?> keyExtractorToUse = (Function<Object, ?>) (keyExtractor == null ? Fn.identity() : keyExtractor);
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
            key = hashKey(keyExtractorToUse.apply(value));

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

    @Override
    public DataSet groupBy(final Collection<String> keyColumnNames) {
        return groupBy(keyColumnNames, NULL_PARAM_INDICATOR_2);
    }

    @Override
    public DataSet groupBy(final Collection<String> keyColumnNames, final String aggregateOnColumnName, final String aggregateResultColumnName,
            final Collector<?, ?, ?> collector) {
        return groupBy(keyColumnNames, NULL_PARAM_INDICATOR_2, aggregateOnColumnName, aggregateResultColumnName, collector);
    }

    @Override
    public DataSet groupBy(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Class<?> rowType) throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);

        if (keyColumnNames.size() == 1) {
            return groupBy(keyColumnNames.iterator().next(), aggregateOnColumnNames, aggregateResultColumnName, rowType);
        }

        final int size = size();
        final int[] keyColumnIndexes = checkColumnNames(keyColumnNames);
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
        Wrapper<Object[]> keyRowWrapper = Wrapper.of(keyRow);
        List<Object> val = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            val = keyRowMap.get(keyRowWrapper);

            if (val == null) {
                val = new ArrayList<>();
                keyRowMap.put(keyRowWrapper, val);

                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                keyRow = Objectory.createObjectArray(keyColumnCount);
                keyRowWrapper = Wrapper.of(keyRow);
            }

            val.add(valueColumnList.get(rowIndex));
        }

        if (keyRow != null) {
            Objectory.recycle(keyRow);
            keyRow = null;
        }

        for (final Wrapper<Object[]> rw : keyRowMap.keySet()) {
            Objectory.recycle(rw.value());
        }

        newColumnList.add(new ArrayList<>(keyRowMap.values()));

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    @Override
    public DataSet groupBy(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Collector<? super Object[], ?, ?> collector) {
        return groupBy(keyColumnNames, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    @Override
    public <T> DataSet groupBy(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Function<? super DisposableObjArray, ? extends T> rowMapper, final Collector<? super T, ?, ?> collector) {
        return groupBy(keyColumnNames, NULL_PARAM_INDICATOR_2, aggregateOnColumnNames, aggregateResultColumnName, rowMapper, collector);
    }

    @Override
    public DataSet groupBy(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);

        final boolean isNullOrIdentityKeyExtractor = keyExtractor == null || keyExtractor == Fn.identity();

        if (keyColumnNames.size() == 1 && isNullOrIdentityKeyExtractor) {
            return this.groupBy(keyColumnNames.iterator().next(), keyExtractor);
        }

        final int size = size();
        final int[] keyColumnIndexes = checkColumnNames(keyColumnNames);
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
        Wrapper<Object[]> keyRowWrapper = isNullOrIdentityKeyExtractor ? Wrapper.of(keyRow) : null;
        final DisposableObjArray disposableArray = isNullOrIdentityKeyExtractor ? null : DisposableObjArray.wrap(keyRow);
        Object key = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            key = isNullOrIdentityKeyExtractor ? keyRowWrapper : hashKey(keyExtractor.apply(disposableArray));

            if (keyRowSet.add(key)) {
                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                if (isNullOrIdentityKeyExtractor) {
                    keyRow = Objectory.createObjectArray(keyColumnCount);
                    keyRowWrapper = Wrapper.of(keyRow);
                }
            }
        }

        if (keyRow != null) {
            Objectory.recycle(keyRow);
            keyRow = null;
        }

        if (isNullOrIdentityKeyExtractor) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) keyRowSet;

            for (final Wrapper<Object[]> rw : tmp) {
                Objectory.recycle(rw.value());
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    @Override
    public DataSet groupBy(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final String aggregateOnColumnName, final String aggregateResultColumnName, final Collector<?, ?, ?> collector) throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);

        if (N.notEmpty(keyColumnNames) && keyColumnNames.contains(aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicated Property name: " + aggregateResultColumnName);
        }

        final boolean isNullOrIdentityKeyExtractor = keyExtractor == null || keyExtractor == Fn.identity();

        if (keyColumnNames.size() == 1 && isNullOrIdentityKeyExtractor) {
            return groupBy(keyColumnNames.iterator().next(), keyExtractor, aggregateOnColumnName, aggregateResultColumnName, collector);
        }

        final int size = size();
        final int[] keyColumnIndexes = checkColumnNames(keyColumnNames);
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
        Wrapper<Object[]> keyRowWrapper = isNullOrIdentityKeyExtractor ? Wrapper.of(keyRow) : null;
        final DisposableObjArray disposableArray = isNullOrIdentityKeyExtractor ? null : DisposableObjArray.wrap(keyRow);
        Object key = null;
        Integer collectorRowIndex = -1;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            key = isNullOrIdentityKeyExtractor ? keyRowWrapper : hashKey(keyExtractor.apply(disposableArray));
            collectorRowIndex = keyRowIndexMap.get(key);

            if (collectorRowIndex == null) {
                collectorRowIndex = aggResultColumn.size();
                keyRowIndexMap.put(key, collectorRowIndex);
                aggResultColumn.add(supplier.get());

                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                if (isNullOrIdentityKeyExtractor) {
                    keyRow = Objectory.createObjectArray(keyColumnCount);
                    keyRowWrapper = Wrapper.of(keyRow);
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

        if (isNullOrIdentityKeyExtractor) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) keyRowIndexMap.keySet();

            for (final Wrapper<Object[]> rw : tmp) {
                Objectory.recycle(rw.value());
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    @Override
    public DataSet groupBy(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Class<?> rowType) throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);

        final boolean isNullOrIdentityKeyExtractor = keyExtractor == null || keyExtractor == Fn.identity();

        if (isNullOrIdentityKeyExtractor) {
            if (keyColumnNames.size() == 1) {
                return groupBy(keyColumnNames.iterator().next(), aggregateOnColumnNames, aggregateResultColumnName, rowType);
            }

            return groupBy(keyColumnNames, aggregateOnColumnNames, aggregateResultColumnName, rowType);
        }

        final int size = size();
        final int[] keyColumnIndexes = checkColumnNames(keyColumnNames);
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

            key = hashKey(keyExtractor.apply(keyDisposableArray));
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

    @Override
    public DataSet groupBy(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector) {
        return groupBy(aggregateOnColumnNames, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    @Override
    public <T> DataSet groupBy(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Function<? super DisposableObjArray, ? extends T> rowMapper, final Collector<? super T, ?, ?> collector) throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);

        if (N.notEmpty(keyColumnNames) && keyColumnNames.contains(aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicated Property name: " + aggregateResultColumnName);
        }

        N.checkArgNotNull(rowMapper, cs.rowMapper);
        N.checkArgNotNull(collector, cs.collector);

        final boolean isNullOrIdentityKeyExtractor = keyExtractor == null || keyExtractor == Fn.identity();

        if (keyColumnNames.size() == 1 && isNullOrIdentityKeyExtractor) {
            return groupBy(keyColumnNames.iterator().next(), keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, rowMapper, collector);
        }

        final int size = size();
        final int[] keyColumnIndexes = checkColumnNames(keyColumnNames);
        final int[] aggOnColumnIndexes = checkColumnNames(aggregateOnColumnNames);
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
        Wrapper<Object[]> keyRowWrapper = isNullOrIdentityKeyExtractor ? Wrapper.of(keyRow) : null;
        final DisposableObjArray keyDisposableArray = isNullOrIdentityKeyExtractor ? null : DisposableObjArray.wrap(keyRow);
        final Object[] aggOnRow = new Object[aggOnColumnCount];
        final DisposableObjArray aggOnRowDisposableArray = DisposableObjArray.wrap(aggOnRow);
        Object key = null;
        Integer collectorRowIndex = -1;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            key = isNullOrIdentityKeyExtractor ? keyRowWrapper : hashKey(keyExtractor.apply(keyDisposableArray));
            collectorRowIndex = keyRowIndexMap.get(key);

            if (collectorRowIndex == null) {
                collectorRowIndex = aggResultColumn.size();
                keyRowIndexMap.put(key, collectorRowIndex);
                aggResultColumn.add(supplier.get());

                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                if (isNullOrIdentityKeyExtractor) {
                    keyRow = Objectory.createObjectArray(keyColumnCount);
                    keyRowWrapper = Wrapper.of(keyRow);
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

        if (isNullOrIdentityKeyExtractor) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) keyRowIndexMap.keySet();

            for (final Wrapper<Object[]> rw : tmp) {
                Objectory.recycle(rw.value());
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    private <R, C, T> Sheet<R, C, T> pivot(final DataSet groupedDataSet) {
        final ImmutableList<R> rowKeyList = groupedDataSet.getColumn(0);
        final ImmutableList<C> colKeyList = groupedDataSet.getColumn(1);

        final Set<R> rowKeySet = new LinkedHashSet<>(rowKeyList);
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
            rows[rowIndexMap.get(rowKeyList.get(i))][colIndexMap.get(colKeyList.get(i))] = aggColumn.get(i);
        }

        return Sheet.rows(rowKeySet, colKeySet, rows);
    }

    @Override
    public Stream<DataSet> rollup(final Collection<String> keyColumnNames) {
        //noinspection resource
        return Stream.of(Iterables.rollup(keyColumnNames)).reversed().filter(Fn.notEmptyC()).map(this::groupBy);
    }

    @Override
    public Stream<DataSet> rollup(final Collection<String> keyColumnNames, final String aggregateOnColumnName, final String aggregateResultColumnName,
            final Collector<?, ?, ?> collector) {
        //noinspection resource
        return Stream.of(Iterables.rollup(keyColumnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> groupBy(columnNames1, aggregateOnColumnName, aggregateResultColumnName, collector));
    }

    @Override
    public Stream<DataSet> rollup(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Class<?> rowType) {
        //noinspection resource
        return Stream.of(Iterables.rollup(keyColumnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call(() -> groupBy(columnNames1, aggregateOnColumnNames, aggregateResultColumnName, rowType)));
    }

    @Override
    public Stream<DataSet> rollup(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector) {
        return rollup(keyColumnNames, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    @Override
    public <T> Stream<DataSet> rollup(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Function<? super DisposableObjArray, ? extends T> rowMapper,
            final Collector<? super T, ?, ?> collector) {
        //noinspection resource
        return Stream.of(Iterables.rollup(keyColumnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call(() -> groupBy(columnNames1, aggregateOnColumnNames, aggregateResultColumnName, rowMapper, collector)));
    }

    @Override
    public Stream<DataSet> rollup(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor) {
        //noinspection resource
        return Stream.of(Iterables.rollup(keyColumnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call(() -> groupBy(columnNames1, keyExtractor)));
    }

    @Override
    public Stream<DataSet> rollup(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final String aggregateOnColumnName, final String aggregateResultColumnName, final Collector<?, ?, ?> collector) {
        //noinspection resource
        return Stream.of(Iterables.rollup(keyColumnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call(() -> groupBy(columnNames1, keyExtractor, aggregateOnColumnName, aggregateResultColumnName, collector)));
    }

    @Override
    public Stream<DataSet> rollup(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Class<?> rowType) {
        //noinspection resource
        return Stream.of(Iterables.rollup(keyColumnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call(() -> groupBy(columnNames1, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, rowType)));
    }

    @Override
    public Stream<DataSet> rollup(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector) {
        return rollup(keyColumnNames, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    @Override
    public <T> Stream<DataSet> rollup(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Function<? super DisposableObjArray, ? extends T> rowMapper, final Collector<? super T, ?, ?> collector) {
        //noinspection resource
        return Stream.of(Iterables.rollup(keyColumnNames))
                .reversed()
                .filter(Fn.notEmptyC())
                .map(columnNames1 -> Try
                        .call(() -> groupBy(columnNames1, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, rowMapper, collector)));
    }

    @Override
    public Stream<DataSet> cube(final Collection<String> keyColumnNames) {
        //noinspection resource
        return cubeSet(keyColumnNames).filter(Fn.notEmptyC()).map(this::groupBy);
    }

    @Override
    public Stream<DataSet> cube(final Collection<String> keyColumnNames, final String aggregateOnColumnName, final String aggregateResultColumnName,
            final Collector<?, ?, ?> collector) {
        //noinspection resource
        return cubeSet(keyColumnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> groupBy(columnNames1, aggregateOnColumnName, aggregateResultColumnName, collector));
    }

    @Override
    public Stream<DataSet> cube(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Class<?> rowType) {
        //noinspection resource
        return cubeSet(keyColumnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call(() -> groupBy(columnNames1, aggregateOnColumnNames, aggregateResultColumnName, rowType)));
    }

    @Override
    public Stream<DataSet> cube(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector) {
        return cube(keyColumnNames, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    @Override
    public <T> Stream<DataSet> cube(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Function<? super DisposableObjArray, ? extends T> rowMapper,
            final Collector<? super T, ?, ?> collector) {
        //noinspection resource
        return cubeSet(keyColumnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call(() -> groupBy(columnNames1, aggregateOnColumnNames, aggregateResultColumnName, rowMapper, collector)));
    }

    @Override
    public Stream<DataSet> cube(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor) {
        //noinspection resource
        return cubeSet(keyColumnNames).filter(Fn.notEmptyC()).map(columnNames1 -> Try.call(() -> groupBy(columnNames1, keyExtractor)));
    }

    @Override
    public Stream<DataSet> cube(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final String aggregateOnColumnName, final String aggregateResultColumnName, final Collector<?, ?, ?> collector) {
        //noinspection resource
        return cubeSet(keyColumnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call(() -> groupBy(columnNames1, keyExtractor, aggregateOnColumnName, aggregateResultColumnName, collector)));
    }

    @Override
    public Stream<DataSet> cube(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Class<?> rowType) {
        //noinspection resource
        return cubeSet(keyColumnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> Try.call(() -> groupBy(columnNames1, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, rowType)));
    }

    @Override
    public Stream<DataSet> cube(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector) {
        return cube(keyColumnNames, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    @Override
    public <T> Stream<DataSet> cube(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Function<? super DisposableObjArray, ? extends T> rowMapper, final Collector<? super T, ?, ?> collector) {
        //noinspection resource
        return cubeSet(keyColumnNames).filter(Fn.notEmptyC())
                .map(columnNames1 -> Try
                        .call(() -> groupBy(columnNames1, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, rowMapper, collector)));
    }

    private static final com.landawn.abacus.util.function.Consumer<List<Set<String>>> REVERSE_ACTION = N::reverse;

    private Stream<Set<String>> cubeSet(final Collection<String> columnNames) {
        //noinspection resource
        return Stream.of(Iterables.powerSet(N.newLinkedHashSet(columnNames)))
                .groupByToEntry(Fn.size())
                .values()
                .onEach(REVERSE_ACTION)
                .flatmap(Fn.identity())
                .reversed();
    }

    @Override
    public <R, C, T> Sheet<R, C, T> pivot(final String keyColumnName, final String aggregateOnColumnNames, final String pivotColumnName,
            final Collector<?, ?, ? extends T> collector) {
        final DataSet groupedDataSet = groupBy(N.asList(keyColumnName, pivotColumnName), aggregateOnColumnNames, aggregateOnColumnNames, collector);

        return pivot(groupedDataSet);
    }

    @Override
    public <R, C, T> Sheet<R, C, T> pivot(final String keyColumnName, final Collection<String> aggregateOnColumnNames, final String pivotColumnName,
            final Collector<? super Object[], ?, ? extends T> collector) {
        final String aggregateResultColumnName = Strings.join(aggregateOnColumnNames, "_");

        final DataSet groupedDataSet = groupBy(N.asList(keyColumnName, pivotColumnName), aggregateOnColumnNames, aggregateResultColumnName, collector);

        return pivot(groupedDataSet);
    }

    @Override
    public <R, C, U, T> Sheet<R, C, T> pivot(final String keyColumnName, final Collection<String> aggregateOnColumnNames, final String pivotColumnName,
            final Function<? super DisposableObjArray, ? extends U> rowMapper, final Collector<? super U, ?, ? extends T> collector) {
        final String aggregateResultColumnName = Strings.join(aggregateOnColumnNames, "_");

        final DataSet groupedDataSet = groupBy(N.asList(keyColumnName, pivotColumnName), aggregateOnColumnNames, aggregateResultColumnName, rowMapper,
                collector);

        return pivot(groupedDataSet);
    }

    @Override
    public void sortBy(final String columnName) {
        sortBy(columnName, Comparators.naturalOrder());
    }

    @Override
    public void sortBy(final String columnName, final Comparator<?> cmp) {
        sort(columnName, cmp, false);
    }

    @Override
    public void sortBy(final Collection<String> columnNames) {
        sortBy(columnNames, Comparators.OBJECT_ARRAY_COMPARATOR);
    }

    @Override
    public void sortBy(final Collection<String> columnNames, final Comparator<? super Object[]> cmp) {
        sort(columnNames, cmp, false);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void sortBy(final Collection<String> columnNames, final Function<? super DisposableObjArray, ? extends Comparable> keyExtractor) {
        sort(columnNames, keyExtractor, false);
    }

    @Override
    public void parallelSortBy(final String columnName) {
        parallelSortBy(columnName, Comparators.naturalOrder());
    }

    @Override
    public void parallelSortBy(final String columnName, final Comparator<?> cmp) {
        sort(columnName, cmp, true);
    }

    @Override
    public void parallelSortBy(final Collection<String> columnNames) {
        parallelSortBy(columnNames, Comparators.OBJECT_ARRAY_COMPARATOR);
    }

    @Override
    public void parallelSortBy(final Collection<String> columnNames, final Comparator<? super Object[]> cmp) {
        sort(columnNames, cmp, true);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void parallelSortBy(final Collection<String> columnNames, final Function<? super DisposableObjArray, ? extends Comparable> keyExtractor) {
        sort(columnNames, keyExtractor, true);
    }

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
            final Comparator<Object> cmpToUse = (Comparator<Object>) cmp;
            pairCmp = (a, b) -> cmpToUse.compare(a.value(), b.value());
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

    private void sort(final Collection<String> columnNames, final Comparator<? super Object[]> cmp, final boolean isParallelSort) {
        checkFrozen();

        final int[] columnIndexes = checkColumnNames(columnNames);
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

        for (final Indexed<Object[]> p : arrayOfPair) {
            Objectory.recycle(p.value());
        }
    }

    @SuppressWarnings("rawtypes")
    private void sort(final Collection<String> columnNames, final Function<? super DisposableObjArray, ? extends Comparable> keyExtractor,
            final boolean isParallelSort) {
        checkFrozen();

        final int[] columnIndexes = checkColumnNames(columnNames);
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

            arrayOfPair[rowIndex] = Indexed.of(keyExtractor.apply(disposableArray), rowIndex);
        }

        final Comparator<Indexed<Comparable>> pairCmp = Comparators.comparingBy(Indexed::value);

        sort(arrayOfPair, pairCmp, isParallelSort);
    }

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

    @Override
    public DataSet topBy(final String columnName, final int n) {
        return topBy(columnName, n, Comparators.nullsFirst());
    }

    @Override
    public DataSet topBy(final String columnName, final int n, final Comparator<?> cmp) {
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

    @Override
    public DataSet topBy(final Collection<String> columnNames, final int n) {
        return topBy(columnNames, n, Comparators.OBJECT_ARRAY_COMPARATOR);
    }

    @Override
    public DataSet topBy(final Collection<String> columnNames, final int n, final Comparator<? super Object[]> cmp) {
        if (n < 1) {
            throw new IllegalArgumentException("'n' can not be less than 1");
        }

        final int[] sortByColumnIndexes = checkColumnNames(columnNames);
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

        for (final Object[] a : keyRowList) {
            Objectory.recycle(a);
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public DataSet topBy(final Collection<String> columnNames, final int n, final Function<? super DisposableObjArray, ? extends Comparable> keyExtractor) {
        if (n < 1) {
            throw new IllegalArgumentException("'n' can not be less than 1");
        }

        final int[] columnIndexes = checkColumnNames(columnNames);
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

            return keyExtractor.apply(disposableObjArray);
        });
    }

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

        final Indexed<Object>[] arrayOfPair = heap.toArray(new Indexed[0]);

        N.sort(arrayOfPair, Comparator.comparingInt(Indexed::index));

        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(arrayOfPair.length));
        }

        int rowIndex = 0;
        for (final Indexed<Object> e : arrayOfPair) {
            rowIndex = e.index();

            for (int i = 0; i < columnCount; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, _properties);
    }

    @Override
    public DataSet distinct() {
        return distinctBy(_columnNameList);
    }

    @Override
    public DataSet distinctBy(final String columnName) {
        return distinctBy(columnName, Fn.identity());
    }

    @Override
    public DataSet distinctBy(final String columnName, final Function<?, ?> keyExtractor) {
        final int columnIndex = checkColumnName(columnName);

        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, _properties);
        }

        final Function<Object, ?> keyExtractorToUse = (Function<Object, ?>) keyExtractor;
        final Set<Object> rowSet = N.newHashSet();
        Object key = null;
        Object value = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            value = _columnList.get(columnIndex).get(rowIndex);
            key = hashKey(keyExtractorToUse == null ? value : keyExtractorToUse.apply(value));

            if (rowSet.add(key)) {
                for (int i = 0; i < columnCount; i++) {
                    newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, _properties);
    }

    @Override
    public DataSet distinctBy(final Collection<String> columnNames) {
        return distinctBy(columnNames, NULL_PARAM_INDICATOR_2);
    }

    @Override
    public DataSet distinctBy(final Collection<String> columnNames, final Function<? super DisposableObjArray, ?> keyExtractor) {
        final boolean isNullOrIdentityKeyExtractor = keyExtractor == null || keyExtractor == Fn.identity();

        if (columnNames.size() == 1 && isNullOrIdentityKeyExtractor) {
            return distinctBy(columnNames.iterator().next());
        }

        final int size = size();
        final int[] columnIndexes = checkColumnNames(columnNames);

        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, _properties);
        }

        final Set<Object> rowSet = N.newHashSet();
        Object[] row = Objectory.createObjectArray(columnCount);
        Wrapper<Object[]> rowWrapper = isNullOrIdentityKeyExtractor ? Wrapper.of(row) : null;
        final DisposableObjArray disposableArray = isNullOrIdentityKeyExtractor ? null : DisposableObjArray.wrap(row);
        Object key = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0, len = columnIndexes.length; i < len; i++) {
                row[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            key = isNullOrIdentityKeyExtractor ? rowWrapper : hashKey(keyExtractor.apply(disposableArray));

            if (rowSet.add(key)) {
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }

                if (isNullOrIdentityKeyExtractor) {
                    row = Objectory.createObjectArray(columnCount);
                    rowWrapper = Wrapper.of(row);
                }
            }
        }

        if (row != null) {
            Objectory.recycle(row);
            row = null;
        }

        if (isNullOrIdentityKeyExtractor) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) rowSet;

            for (final Wrapper<Object[]> rw : tmp) {
                Objectory.recycle(rw.value());
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, _properties);
    }

    @Override
    public DataSet filter(final Predicate<? super DisposableObjArray> filter) {
        return filter(filter, size());
    }

    @Override
    public DataSet filter(final Predicate<? super DisposableObjArray> filter, final int max) {
        return filter(0, size(), filter, max);
    }

    @Override
    public DataSet filter(final int fromRowIndex, final int toRowIndex, final Predicate<? super DisposableObjArray> filter) {
        return filter(fromRowIndex, toRowIndex, filter, size());
    }

    @Override
    public DataSet filter(final int fromRowIndex, final int toRowIndex, final Predicate<? super DisposableObjArray> filter, final int max) {
        return filter(fromRowIndex, toRowIndex, _columnNameList, filter, max);
    }

    @Override
    public DataSet filter(final Tuple2<String, String> columnNames, final BiPredicate<?, ?> filter) {
        return filter(columnNames, filter, size());
    }

    @Override
    public DataSet filter(final Tuple2<String, String> columnNames, final BiPredicate<?, ?> filter, final int max) {
        return filter(0, size(), columnNames, filter, max);
    }

    @Override
    public DataSet filter(final int fromRowIndex, final int toRowIndex, final Tuple2<String, String> columnNames, final BiPredicate<?, ?> filter) {
        return filter(fromRowIndex, toRowIndex, columnNames, filter, size());
    }

    @Override
    public DataSet filter(final int fromRowIndex, final int toRowIndex, final Tuple2<String, String> columnNames, final BiPredicate<?, ?> filter, final int max)
            throws IllegalArgumentException {
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter);

        final BiPredicate<Object, Object> filterToUse = (BiPredicate<Object, Object>) filter;
        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        if (size == 0 || max == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, _properties);
        }

        int count = max;

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            if (filterToUse.test(column1.get(rowIndex), column2.get(rowIndex))) {
                if (--count < 0) {
                    break;
                }

                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, _properties);
    }

    @Override
    public DataSet filter(final Tuple3<String, String, String> columnNames, final TriPredicate<?, ?, ?> filter) {
        return filter(columnNames, filter, size());
    }

    @Override
    public DataSet filter(final Tuple3<String, String, String> columnNames, final TriPredicate<?, ?, ?> filter, final int max) {
        return filter(0, size(), columnNames, filter, max);
    }

    @Override
    public DataSet filter(final int fromRowIndex, final int toRowIndex, final Tuple3<String, String, String> columnNames, final TriPredicate<?, ?, ?> filter) {
        return filter(fromRowIndex, toRowIndex, columnNames, filter, size());
    }

    @Override
    public DataSet filter(final int fromRowIndex, final int toRowIndex, final Tuple3<String, String, String> columnNames, final TriPredicate<?, ?, ?> filter,
            final int max) throws IllegalArgumentException {
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(columnNames._3));

        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter);

        final TriPredicate<Object, Object, Object> filterToUse = (TriPredicate<Object, Object, Object>) filter;
        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        if (size == 0 || max == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, _properties);
        }

        int count = max;

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            if (filterToUse.test(column1.get(rowIndex), column2.get(rowIndex), column3.get(rowIndex))) {
                if (--count < 0) {
                    break;
                }

                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, _properties);
    }

    @Override
    public DataSet filter(final String columnName, final Predicate<?> filter) {
        return filter(columnName, filter, size());
    }

    @Override
    public DataSet filter(final String columnName, final Predicate<?> filter, final int max) throws IllegalArgumentException {
        return filter(0, size(), columnName, filter, max);
    }

    @Override
    public DataSet filter(final int fromRowIndex, final int toRowIndex, final String columnName, final Predicate<?> filter) {
        return filter(fromRowIndex, toRowIndex, columnName, filter, size());
    }

    @Override
    public DataSet filter(final int fromRowIndex, final int toRowIndex, final String columnName, final Predicate<?> filter, int max)
            throws IllegalArgumentException {
        final int filterColumnIndex = checkColumnName(columnName);
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter, cs.filter);
        N.checkArgNotNegative(max, cs.max);

        final Predicate<Object> filterToUse = (Predicate<Object>) filter;

        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, _properties);
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

        return new RowDataSet(newColumnNameList, newColumnList, _properties);
    }

    @Override
    public DataSet filter(final Collection<String> columnNames, final Predicate<? super DisposableObjArray> filter) {
        return filter(columnNames, filter, size());
    }

    @Override
    public DataSet filter(final Collection<String> columnNames, final Predicate<? super DisposableObjArray> filter, final int max) {
        return filter(0, size(), columnNames, filter, max);
    }

    @Override
    public DataSet filter(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Predicate<? super DisposableObjArray> filter) {
        return filter(fromRowIndex, toRowIndex, columnNames, filter, size());
    }

    @Override
    public DataSet filter(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Predicate<? super DisposableObjArray> filter, int max) throws IllegalArgumentException {
        final int[] filterColumnIndexes = checkColumnNames(columnNames);
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(filter, cs.filter);
        N.checkArgNotNegative(max, cs.max);

        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>(N.min(max, (size == 0) ? 0 : ((int) (size * 0.8) + 1))));
        }

        if (size == 0) {
            return new RowDataSet(newColumnNameList, newColumnList, _properties);
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

        return new RowDataSet(newColumnNameList, newColumnList, _properties);
    }

    @Override
    public DataSet map(final String fromColumnName, final String newColumnName, final String copyingColumnName, final Function<?, ?> mapper) {
        return map(fromColumnName, newColumnName, Array.asList(copyingColumnName), mapper);
    }

    @Override
    public DataSet map(final String fromColumnName, final String newColumnName, final Collection<String> copyingColumnNames, final Function<?, ?> mapper)
            throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);
        final int fromColumnIndex = checkColumnName(fromColumnName);
        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        final Function<Object, Object> mapperToUse = (Function<Object, Object>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        for (final Object val : _columnList.get(fromColumnIndex)) {
            mappedColumn.add(mapperToUse.apply(val));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.notEmpty(copyingColumnNames)) {
            newColumnNameList.addAll(copyingColumnNames);

            for (final int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    @Override
    public DataSet map(final Tuple2<String, String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final BiFunction<?, ?, ?> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        final BiFunction<Object, Object, Object> mapperToUse = (BiFunction<Object, Object, Object>) mapper;
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

            for (final int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    @Override
    public DataSet map(final Tuple3<String, String, String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final TriFunction<?, ?, ?, ?> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<Object> fromColumn3 = _columnList.get(checkColumnName(fromColumnNames._3));

        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        final TriFunction<Object, Object, Object, Object> mapperToUse = (TriFunction<Object, Object, Object, Object>) mapper;
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

            for (final int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    @Override
    public DataSet map(final Collection<String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final Function<? super DisposableObjArray, ?> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);
        final int[] fromColumnIndices = checkColumnNames(fromColumnNames);
        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        final Function<? super DisposableObjArray, Object> mapperToUse = (Function<? super DisposableObjArray, Object>) mapper;
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

            for (final int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    @Override
    public DataSet flatMap(final String fromColumnName, final String newColumnName, final String copyingColumnName,
            final Function<?, ? extends Collection<?>> mapper) {
        return flatMap(fromColumnName, newColumnName, Array.asList(copyingColumnName), mapper);
    }

    @Override
    public DataSet flatMap(final String fromColumnName, final String newColumnName, final Collection<String> copyingColumnNames,
            final Function<?, ? extends Collection<?>> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);
        final int fromColumnIndex = checkColumnName(fromColumnName);
        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        final Function<Object, Collection<Object>> mapperToUse = (Function<Object, Collection<Object>>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        newColumnNameList.add(newColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);
        newColumnList.add(mappedColumn);

        if (N.isEmpty(copyingColumnNames)) {
            Collection<Object> c = null;

            for (final Object val : _columnList.get(fromColumnIndex)) {
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

    @Override
    public DataSet flatMap(final Tuple2<String, String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final BiFunction<?, ?, ? extends Collection<?>> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));

        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        final BiFunction<Object, Object, Collection<Object>> mapperToUse = (BiFunction<Object, Object, Collection<Object>>) mapper;
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

    @Override
    public DataSet flatMap(final Tuple3<String, String, String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final TriFunction<?, ?, ?, ? extends Collection<?>> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<Object> fromColumn3 = _columnList.get(checkColumnName(fromColumnNames._3));

        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        final TriFunction<Object, Object, Object, Collection<Object>> mapperToUse = (TriFunction<Object, Object, Object, Collection<Object>>) mapper;
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

    @Override
    public DataSet flatMap(final Collection<String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final Function<? super DisposableObjArray, ? extends Collection<?>> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);
        final int[] fromColumnIndices = checkColumnNames(fromColumnNames);
        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        final Function<? super DisposableObjArray, Collection<Object>> mapperToUse = (Function<? super DisposableObjArray, Collection<Object>>) mapper;
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

    @Override
    public DataSet copy() {
        return copy(0, size(), _columnNameList);
    }

    @Override
    public DataSet copy(final Collection<String> columnNames) {
        return copy(0, size(), columnNames);
    }

    @Override
    public DataSet copy(final int fromRowIndex, final int toRowIndex) {
        return copy(fromRowIndex, toRowIndex, _columnNameList);
    }

    @Override
    public DataSet copy(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        return copy(fromRowIndex, toRowIndex, columnNames, this.checkColumnNames(columnNames), true);
    }

    private RowDataSet copy(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final int[] columnIndexes,
            final boolean copyProperties) {

        final List<String> newColumnNameList = new ArrayList<>(columnNames);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnNameList.size());

        if (fromRowIndex == 0 && toRowIndex == size()) {
            for (final int columnIndex : columnIndexes) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        } else {
            for (final int columnIndex : columnIndexes) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex).subList(fromRowIndex, toRowIndex)));
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList, copyProperties ? _properties : null);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
    @Override
    public DataSet clone() { //NOSONAR
        return clone(_isFrozen);
    }

    @Override
    public DataSet clone(final boolean freeze) { //NOSONAR
        if (kryoParser == null) {
            throw new RuntimeException("Kryo is required");
        }

        //    if (kryoParser != null) {
        //        dataSet = kryoParser.clone(this);
        //    } else {
        //        dataSet = jsonParser.deserialize(jsonParser.serialize(this), RowDataSet.class); // column type could be different by json Serialization/Deserialization
        //    }

        final RowDataSet dataSet = kryoParser.clone(this);

        dataSet._isFrozen = freeze;

        return dataSet;
    }

    @Override
    public DataSet innerJoin(final DataSet right, final String columnName, final String joinColumnNameOnRight) {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return innerJoin(right, onColumnNames);
    }

    @Override
    public DataSet innerJoin(final DataSet right, final Map<String, String> onColumnNames) {
        return join(right, onColumnNames, false);
    }

    @Override
    public DataSet innerJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType) {
        return join(right, onColumnNames, newColumnName, newColumnType, false);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public DataSet innerJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) {
        return join(right, onColumnNames, newColumnName, newColumnType, collSupplier, false);
    }

    @Override
    public DataSet leftJoin(final DataSet right, final String columnName, final String joinColumnNameOnRight) {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return leftJoin(right, onColumnNames);
    }

    @Override
    public DataSet leftJoin(final DataSet right, final Map<String, String> onColumnNames) {
        return join(right, onColumnNames, true);
    }

    private DataSet join(final DataSet right, final Map<String, String> onColumnNames, final boolean isLeftJoin) {
        checkJoinOnColumnNames(onColumnNames);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());
            final List<String> rightColumnNames = getRightColumnNames(right);
            final int newColumnSize = columnCount() + rightColumnNames.size();
            final List<String> newColumnNameList = new ArrayList<>(newColumnSize);
            final List<List<Object>> newColumnList = new ArrayList<>(newColumnSize);

            initNewColumnList(newColumnNameList, newColumnList, _columnNameList, rightColumnNames);

            if (isEmpty()) {
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

            initColumnIndexes1(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, _columnNameList, rightColumnNames);

            if (isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();

            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(rightJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, rowWrapper, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);
            row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
            rowWrapper = Wrapper.of(row);
            List<Integer> rightRowIndexList = null;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(rowWrapper);

                join(newColumnList, right, isLeftJoin, leftRowIndex, rightRowIndexList, rightColumnIndexes);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (final Wrapper<Object[]> rw : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    private void join(final List<List<Object>> newColumnList, final DataSet right, final boolean isLeftJoin, final int leftRowIndex,
            final List<Integer> rightRowIndexList, final int[] rightColumnIndexes) {
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

                for (final int rightRowIndex : rightRowIndexList) {
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

            if (isEmpty()) {
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

            if (isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(rightJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, rowWrapper, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            List<Integer> rightRowIndexList = null;
            row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
            rowWrapper = Wrapper.of(row);

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(rowWrapper);

                join(newColumnList, right, isLeftJoin, newColumnType, newColumnIndex, leftRowIndex, rightRowIndexList);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (final Wrapper<Object[]> rw : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    private void join(final List<List<Object>> newColumnList, final DataSet right, final boolean isLeftJoin, final Class<?> newColumnType,
            final int newColumnIndex, final int leftRowIndex, final List<Integer> rightRowIndexList) {
        if (N.notEmpty(rightRowIndexList)) {
            for (final int rightRowIndex : rightRowIndexList) {
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

    private void checkJoinOnColumnNames(final Map<String, String> onColumnNames) {
        if (N.isEmpty(onColumnNames)) {
            throw new IllegalArgumentException("The joining column names can't be null or empty");
        }
    }

    private int checkRightJoinColumnName(final DataSet right, final String joinColumnNameOnRight) {
        if (!right.containsColumn(joinColumnNameOnRight)) {
            throw new IllegalArgumentException(
                    "The specified column: " + joinColumnNameOnRight + " is not included in the right DataSet: " + right.columnNameList());
        }

        return right.getColumnIndex(joinColumnNameOnRight);
    }

    private void checkNewColumnName(final String newColumnName) {
        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included this DataSet: " + _columnNameList);
        }
    }

    private List<String> getRightColumnNames(final DataSet right) { // NOSONAR
        // final List<String> rightColumnNames = new ArrayList<>(right.columnNameList());

        // How to handle join columns with same name for full join.
        //    if (onColumnEntry.getKey().equals(onColumnEntry.getValue())) {
        //        rightColumnNames.remove(onColumnEntry.getValue());
        //    }

        return new ArrayList<>(right.columnNameList());
    }

    private void initColumnIndexes1(final int[] leftJoinColumnIndexes, final int[] rightJoinColumnIndexes, final DataSet right,
            final Map<String, String> onColumnNames) { // NOSONAR
        int i = 0;
        for (final Map.Entry<String, String> entry : onColumnNames.entrySet()) {
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

    private void initColumnIndexes(final int[] leftJoinColumnIndexes, final int[] rightJoinColumnIndexes, final DataSet right, // NOSONAR
            final Map<String, String> onColumnNames) {
        int i = 0;
        for (final Map.Entry<String, String> entry : onColumnNames.entrySet()) {
            leftJoinColumnIndexes[i] = checkColumnName(entry.getKey());
            rightJoinColumnIndexes[i] = right.getColumnIndex(entry.getValue());

            if (rightJoinColumnIndexes[i] < 0) {
                throw new IllegalArgumentException(
                        "The specified column: " + entry.getValue() + " is not included in the right DataSet: " + right.columnNameList());
            }

            i++;
        }
    }

    private void initNewColumnList(final List<String> newColumnNameList, final List<List<Object>> newColumnList, final List<String> leftColumnNames,
            final List<String> rightColumnNames) {
        //    for (String rightColumnName : rightColumnNames) {
        //        if (this.containsColumn(rightColumnName)) {
        //            throw new IllegalArgumentException("The column name: " + rightColumnName + " is already included this DataSet: " + _columnNameList);
        //        }
        //    }

        for (final String columnName : leftColumnNames) {
            newColumnNameList.add(columnName);
            newColumnList.add(new ArrayList<>());
        }

        for (final String columnName : rightColumnNames) {
            if (leftColumnNames.contains(columnName)) {
                newColumnNameList.add(columnName + POSTFIX_FOR_SAME_JOINED_COLUMN_NAME);
            } else {
                newColumnNameList.add(columnName);
            }

            newColumnList.add(new ArrayList<>());
        }
    }

    private void initNewColumnList(final List<String> newColumnNameList, final List<List<Object>> newColumnList, final String newColumnName) {
        newColumnNameList.addAll(_columnNameList);
        newColumnNameList.add(newColumnName);

        for (int i = 0, len = columnCount() + 1; i < len; i++) {
            newColumnList.add(new ArrayList<>());
        }
    }

    private void putRowIndex(final Map<Object, List<Integer>> joinColumnRightRowIndexMap, final Object hashKey, final int rightRowIndex) {
        final List<Integer> rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

        if (rightRowIndexList == null) {
            joinColumnRightRowIndexMap.put(hashKey, N.asList(rightRowIndex));
        } else {
            rightRowIndexList.add(rightRowIndex);
        }
    }

    private Object[] putRowIndex(final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap, final Wrapper<Object[]> rowWrapper, Object[] row,
            final int rightRowIndex) {
        final List<Integer> rightRowIndexList = joinColumnRightRowIndexMap.get(rowWrapper);

        if (rightRowIndexList == null) {
            joinColumnRightRowIndexMap.put(rowWrapper, N.asList(rightRowIndex));
            row = null;
        } else {
            rightRowIndexList.add(rightRowIndex);
        }

        return row;
    }

    @Override
    public DataSet leftJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType) {
        return join(right, onColumnNames, newColumnName, newColumnType, true);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public DataSet leftJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) {
        return join(right, onColumnNames, newColumnName, newColumnType, collSupplier, true);
    }

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

            if (isEmpty()) {
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

            if (isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            List<Integer> rightRowIndexList = null;
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(rightJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, rowWrapper, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
            rowWrapper = Wrapper.of(row);

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(rowWrapper);

                join(newColumnList, right, isLeftJoin, newColumnType, collSupplier, newColumnIndex, leftRowIndex, rightRowIndexList);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (final Wrapper<Object[]> rw : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    @SuppressWarnings("rawtypes")
    private void join(final List<List<Object>> newColumnList, final DataSet right, final boolean isLeftJoin, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final int leftRowIndex, final List<Integer> rightRowIndexList) {
        if (N.notEmpty(rightRowIndexList) || isLeftJoin) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }
        }

        if (N.notEmpty(rightRowIndexList)) {
            final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

            for (final int rightRowIndex : rightRowIndexList) {
                coll.add(right.getRow(rightRowIndex, newColumnType));
            }

            newColumnList.get(newColumnIndex).add(coll);
        } else if (isLeftJoin) {
            newColumnList.get(newColumnIndex).add(null);
        }
    }

    @Override
    public DataSet rightJoin(final DataSet right, final String columnName, final String joinColumnNameOnRight) {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return rightJoin(right, onColumnNames);
    }

    @Override
    public DataSet rightJoin(final DataSet right, final Map<String, String> onColumnNames) {
        checkJoinOnColumnNames(onColumnNames);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());
            final List<String> leftColumnNames = getLeftColumnNamesForRightJoin();
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

            for (int leftRowIndex = 0, leftDataSetSize = size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final int[] leftColumnIndexes = getColumnIndexes(leftColumnNames);
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

            initColumnIndexesForRightJoin(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, leftColumnNames, rightColumnNames);

            if (right.isEmpty()) {
                return new RowDataSet(newColumnNameList, newColumnList);
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int leftRowIndex = 0, leftDataSetSize = size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnLeftRowIndexMap, rowWrapper, row, leftRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int[] leftColumnIndexes = getColumnIndexes(leftColumnNames);
            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);
            row = Objectory.createObjectArray(rightJoinColumnIndexes.length);
            rowWrapper = Wrapper.of(row);
            List<Integer> leftRowIndexList = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                leftRowIndexList = joinColumnLeftRowIndexMap.get(rowWrapper);

                rightJoin(newColumnList, right, rightRowIndex, rightColumnIndexes, leftColumnIndexes, leftRowIndexList);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (final Wrapper<Object[]> rw : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    private void rightJoin(final List<List<Object>> newColumnList, final DataSet right, final int rightRowIndex, final int[] rightColumnIndexes,
            final int[] leftColumnIndexes, final List<Integer> leftRowIndexList) {
        if (N.notEmpty(leftRowIndexList)) {
            for (final int leftRowIndex : leftRowIndexList) {
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

    private void initColumnIndexesForRightJoin(final int[] leftJoinColumnIndexes, final int[] rightJoinColumnIndexes, final DataSet right, // NOSONAR
            final Map<String, String> onColumnNames) { // NOSONAR
        int i = 0;
        for (final Map.Entry<String, String> entry : onColumnNames.entrySet()) {
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

    private List<String> getLeftColumnNamesForRightJoin() { // NOSONAR
        // final List<String> leftColumnNames = new ArrayList<>(_columnNameList);

        // How to handle join columns with same name for full join.
        //    if (this.containsColumn(joinColumnNameOnRight)) {
        //        leftColumnNames.remove(joinColumnNameOnRight);
        //    }

        return new ArrayList<>(_columnNameList);
    }

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

            for (int leftRowIndex = 0, leftDataSetSize = size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final int[] leftColumnIndexes = getColumnIndexes(leftColumnNames);
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

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int leftRowIndex = 0, leftDataSetSize = size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnLeftRowIndexMap, rowWrapper, row, leftRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final int[] leftColumnIndexes = getColumnIndexes(leftColumnNames);
            row = Objectory.createObjectArray(rightJoinColumnIndexes.length);
            rowWrapper = Wrapper.of(row);
            List<Integer> leftRowIndexList = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                leftRowIndexList = joinColumnLeftRowIndexMap.get(rowWrapper);

                rightJoin(newColumnList, right, newColumnType, newColumnIndex, rightRowIndex, leftRowIndexList, leftColumnIndexes);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (final Wrapper<Object[]> rw : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    private void rightJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType, final int newColumnIndex,
            final int rightRowIndex, final List<Integer> leftRowIndexList, final int[] leftColumnIndexes) {
        if (N.notEmpty(leftRowIndexList)) {
            for (final int leftRowIndex : leftRowIndexList) {
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

    private void initNewColumnListForRightJoin(final List<String> newColumnNameList, final List<List<Object>> newColumnList, final List<String> leftColumnNames,
            final String newColumnName) {
        for (final String leftColumnName : leftColumnNames) {
            newColumnNameList.add(leftColumnName);
            newColumnList.add(new ArrayList<>());
        }

        newColumnNameList.add(newColumnName);
        newColumnList.add(new ArrayList<>());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public DataSet rightJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException {
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

            for (int leftRowIndex = 0, leftDataSetSize = size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final int[] leftColumnIndexes = getColumnIndexes(leftColumnNames);
            List<Integer> leftRowIndexList = null;
            List<Integer> rightRowIndexList = null;

            for (final Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
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

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int leftRowIndex = 0, leftDataSetSize = size(); leftRowIndex < leftDataSetSize; leftRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnLeftRowIndexMap, rowWrapper, row, leftRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(rightJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, rowWrapper, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final int[] leftColumnIndexes = getColumnIndexes(leftColumnNames);
            List<Integer> leftRowIndexList = null;
            List<Integer> rightRowIndexList = null;

            for (final Map.Entry<Wrapper<Object[]>, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                leftRowIndexList = joinColumnLeftRowIndexMap.get(rightRowIndexEntry.getKey());
                rightRowIndexList = rightRowIndexEntry.getValue();

                rightJoin(newColumnList, right, newColumnType, collSupplier, newColumnIndex, leftColumnIndexes, leftRowIndexList, rightRowIndexList);
            }

            for (final Wrapper<Object[]> rw : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            for (final Wrapper<Object[]> rw : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    @SuppressWarnings("rawtypes")
    private void rightJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final int[] leftColumnIndexes, final List<Integer> leftRowIndexList,
            final List<Integer> rightRowIndexList) {
        if (N.notEmpty(leftRowIndexList)) {
            for (final int leftRowIndex : leftRowIndexList) {
                for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                    newColumnList.get(i).add(this.get(leftRowIndex, leftColumnIndexes[i]));
                }

                final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

                for (final int rightRowIndex : rightRowIndexList) {
                    coll.add(right.getRow(rightRowIndex, newColumnType));
                }

                newColumnList.get(newColumnIndex).add(coll);
            }
        } else {
            for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

            for (final int rightRowIndex : rightRowIndexList) {
                coll.add(right.getRow(rightRowIndex, newColumnType));
            }

            newColumnList.get(newColumnIndex).add(coll);
        }
    }

    @Override
    public DataSet fullJoin(final DataSet right, final String columnName, final String joinColumnNameOnRight) {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return fullJoin(right, onColumnNames);
    }

    @Override
    public DataSet fullJoin(final DataSet right, final Map<String, String> onColumnNames) {
        checkJoinOnColumnNames(onColumnNames);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());
            final List<String> rightColumnNames = getRightColumnNames(right);

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

            for (final Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexSet.contains(rightRowIndexEntry.getKey())) {
                    fullJoin(newColumnList, right, rightRowIndexEntry.getValue(), rightColumnIndexes);
                }
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];
            final List<String> rightColumnNames = new ArrayList<>(right.columnNameList());

            initColumnIndexes1(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, _columnNameList, rightColumnNames);

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();
            List<Integer> rightRowIndexList = null;
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(rightJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, rowWrapper, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);
            final Map<Wrapper<Object[]>, Integer> joinColumnLeftRowIndexMap = new HashMap<>();

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(rowWrapper);

                fullJoin(newColumnList, right, leftRowIndex, rightRowIndexList, rightColumnIndexes);

                if (!joinColumnLeftRowIndexMap.containsKey(rowWrapper)) {
                    joinColumnLeftRowIndexMap.put(rowWrapper, leftRowIndex);
                    row = null;
                }
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (final Map.Entry<Wrapper<Object[]>, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexMap.containsKey(rightRowIndexEntry.getKey())) {
                    fullJoin(newColumnList, right, rightRowIndexEntry.getValue(), rightColumnIndexes);
                }
            }

            for (final Wrapper<Object[]> rw : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            for (final Wrapper<Object[]> rw : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final List<Integer> rightRowIndexList, final int[] rightColumnIndexes) {
        for (final int rightRowIndex : rightRowIndexList) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            for (int i = 0, leftColumnLength = columnCount(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                newColumnList.get(leftColumnLength + i).add(right.get(rightRowIndex, rightColumnIndexes[i]));
            }
        }
    }

    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final int leftRowIndex, final List<Integer> rightRowIndexList,
            final int[] rightColumnIndexes) {
        if (N.notEmpty(rightRowIndexList)) {
            for (final int rightRowIndex : rightRowIndexList) {
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

            for (final Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
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

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();
            List<Integer> rightRowIndexList = null;
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(rightJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, rowWrapper, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final Map<Wrapper<Object[]>, Integer> joinColumnLeftRowIndexMap = new HashMap<>();

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(rowWrapper);

                fullJoin(newColumnList, right, newColumnType, newColumnIndex, leftRowIndex, rightRowIndexList);

                if (!joinColumnLeftRowIndexMap.containsKey(rowWrapper)) {
                    joinColumnLeftRowIndexMap.put(rowWrapper, leftRowIndex);
                    row = null;
                }
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (final Map.Entry<Wrapper<Object[]>, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexMap.containsKey(rightRowIndexEntry.getKey())) {
                    fullJoin(newColumnList, right, newColumnType, newColumnIndex, rightRowIndexEntry.getValue());
                }
            }

            for (final Wrapper<Object[]> rw : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            for (final Wrapper<Object[]> rw : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType, final int newColumnIndex,
            final List<Integer> rightRowIndexList) {
        for (final int rightRowIndex : rightRowIndexList) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            newColumnList.get(newColumnIndex).add(right.getRow(rightRowIndex, newColumnType));
        }
    }

    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType, final int newColumnIndex,
            final int leftRowIndex, final List<Integer> rightRowIndexList) {
        if (N.notEmpty(rightRowIndexList)) {
            for (final int rightRowIndex : rightRowIndexList) {
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

    @SuppressWarnings("rawtypes")
    @Override
    public DataSet fullJoin(final DataSet right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException {
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

            for (final Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
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

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();
            List<Integer> rightRowIndexList = null;
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rightRowIndex = 0, rightDataSetSize = right.size(); rightRowIndex < rightDataSetSize; rightRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(rightJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = rightJoinColumnIndexes.length; i < len; i++) {
                    row[i] = right.get(rightRowIndex, rightJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnRightRowIndexMap, rowWrapper, row, rightRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final Map<Wrapper<Object[]>, Integer> joinColumnLeftRowIndexMap = new HashMap<>();

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.get(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(rowWrapper);

                fullJoin(newColumnList, right, newColumnType, collSupplier, newColumnIndex, leftRowIndex, rightRowIndexList);

                if (!joinColumnLeftRowIndexMap.containsKey(rowWrapper)) {
                    joinColumnLeftRowIndexMap.put(rowWrapper, leftRowIndex);
                    row = null;
                }
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (final Map.Entry<Wrapper<Object[]>, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexMap.containsKey(rightRowIndexEntry.getKey())) {
                    fullJoin(newColumnList, right, newColumnType, collSupplier, newColumnIndex, rightRowIndexEntry.getValue());
                }
            }

            for (final Wrapper<Object[]> rw : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            for (final Wrapper<Object[]> rw : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataSet(newColumnNameList, newColumnList);
        }
    }

    @SuppressWarnings("rawtypes")
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final List<Integer> rightRowIndexList) {
        for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
            newColumnList.get(i).add(null);
        }

        final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

        for (final int rightRowIndex : rightRowIndexList) {
            coll.add(right.getRow(rightRowIndex, newColumnType));
        }

        newColumnList.get(newColumnIndex).add(coll);
    }

    @SuppressWarnings("rawtypes")
    private void fullJoin(final List<List<Object>> newColumnList, final DataSet right, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final int leftRowIndex, final List<Integer> rightRowIndexList) {
        if (N.notEmpty(rightRowIndexList)) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            final Collection<Object> coll = collSupplier.apply(rightRowIndexList.size());

            for (final int rightRowIndex : rightRowIndexList) {
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

    @Override
    public DataSet union(final DataSet other) {
        return union(other, false);
    }

    @Override
    public DataSet union(final DataSet other, final boolean requiresSameColumns) {
        return union(other, getKeyColumnNames(other), requiresSameColumns);
    }

    @Override
    public DataSet union(final DataSet other, final Collection<String> keyColumnNames) {
        return union(other, keyColumnNames, false);
    }

    @Override
    public DataSet union(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        checkColumnNames(other, keyColumnNames, requiresSameColumns);

        final Set<String> newColumnNameSet = new LinkedHashSet<>(_columnNameList);
        newColumnNameSet.addAll(other.columnNameList());

        final List<String> newColumnNameList = new ArrayList<>(newColumnNameSet);
        final int newColumnCount = newColumnNameList.size();
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        final int totalSize = size() + other.size();

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>(totalSize));
        }

        final DataSet result = new RowDataSet(newColumnNameList, newColumnList);

        if (totalSize == 0) {
            return result;
        }

        final int thisColumnCount = columnCount();
        final int otherColumnCount = other.columnCount();
        final int keyColumnCount = keyColumnNames.size();

        if (keyColumnCount == 1) {
            final String keyColumnName = N.firstOrNullIfEmpty(keyColumnNames);
            final Set<Object> addedRowKeySet = N.newHashSet();

            if (size() > 0) {
                final int keyColumnIndex = getColumnIndex(keyColumnName);
                final List<Object> keyColumn = _columnList.get(keyColumnIndex);

                for (int rowIndex = 0, rowCount = size(); rowIndex < rowCount; rowIndex++) {
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

            if (!other.isEmpty()) {
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
                        if (!other.containsColumn(_columnNameList.get(i))) {
                            newColumnList.get(i).addAll(column);
                        }
                    }
                }
            }
        } else {
            final Set<Wrapper<Object[]>> addedRowKeySet = new HashSet<>();
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            if (size() > 0) {
                final int[] keyColumnIndexes = getColumnIndexes(keyColumnNames);
                final List<Object>[] keyColumns = new List[keyColumnCount];

                for (int i = 0; i < keyColumnCount; i++) {
                    keyColumns[i] = _columnList.get(keyColumnIndexes[i]);
                }

                for (int rowIndex = 0, rowCount = size(); rowIndex < rowCount; rowIndex++) {
                    if (row == null) {
                        row = Objectory.createObjectArray(keyColumnCount);
                        rowWrapper = Wrapper.of(row);
                    }

                    for (int i = 0; i < keyColumnCount; i++) {
                        row[i] = keyColumns[i].get(rowIndex);
                    }

                    if (addedRowKeySet.add(rowWrapper)) {
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

            if (!other.isEmpty()) {
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
                    if (row == null) {
                        row = Objectory.createObjectArray(keyColumnCount);
                        rowWrapper = Wrapper.of(row);
                    }

                    for (int i = 0; i < keyColumnCount; i++) {
                        row[i] = keyColumnsInOther[i].get(rowIndex);
                    }

                    if (addedRowKeySet.add(rowWrapper)) {
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
                        if (!other.containsColumn(_columnNameList.get(i))) {
                            newColumnList.get(i).addAll(column);
                        }
                    }
                }
            }

            for (final Wrapper<Object[]> rw : addedRowKeySet) {
                Objectory.recycle(rw.value());
            }
        }

        return result;
    }

    @Override
    public DataSet unionAll(final DataSet other) {
        return unionAll(other, false);
    }

    @Override
    public DataSet unionAll(final DataSet other, final boolean requiresSameColumns) {
        return merge(other, requiresSameColumns);
    }

    //    @Override
    //    public DataSet unionAll(final DataSet other, final Collection<String> keyColumnNames) {
    //        return unionAll(other);
    //    }
    //
    //    @Override
    //    public DataSet unionAll(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
    //        return unionAll(other, requiresSameColumns);
    //    }

    @Override
    public DataSet intersect(final DataSet other) {
        return intersect(other, false);
    }

    @Override
    public DataSet intersect(final DataSet other, final boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, true, true);
    }

    @Override
    public DataSet intersect(final DataSet other, final Collection<String> keyColumnNames) {
        return intersect(other, keyColumnNames, false);
    }

    @Override
    public DataSet intersect(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, true, true);
    }

    @Override
    public DataSet intersectAll(final DataSet other) {
        return intersectAll(other, false);
    }

    @Override
    public DataSet intersectAll(final DataSet other, final boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, true, false);
    }

    @Override
    public DataSet intersectAll(final DataSet other, final Collection<String> keyColumnNames) {
        return intersectAll(other, keyColumnNames, false);
    }

    @Override
    public DataSet intersectAll(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, true, false);
    }

    @Override
    public DataSet except(final DataSet other) {
        return except(other, false);
    }

    @Override
    public DataSet except(final DataSet other, final boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, false, true);
    }

    @Override
    public DataSet except(final DataSet other, final Collection<String> keyColumnNames) {
        return except(other, keyColumnNames, false);
    }

    @Override
    public DataSet except(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, false, true);
    }

    @Override
    public DataSet exceptAll(final DataSet other) {
        return exceptAll(other, false);
    }

    @Override
    public DataSet exceptAll(final DataSet other, final boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, false, false);
    }

    @Override
    public DataSet exceptAll(final DataSet other, final Collection<String> keyColumnNames) {
        return exceptAll(other, keyColumnNames, false);
    }

    @Override
    public DataSet exceptAll(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, false, false);
    }

    private DataSet removeAll(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns, final boolean retain,
            final boolean deduplicate) {
        checkColumnNames(other, keyColumnNames, requiresSameColumns);

        final int newColumnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
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
            final int keyColumnIndex = getColumnIndex(keyColumnName);
            final int keyColumnIndexInOther = other.getColumnIndex(keyColumnName);

            final List<Object> keyColumnInOther = other.getColumn(keyColumnIndexInOther);
            final Set<Object> rowKeySet = N.newHashSet();

            for (final Object e : keyColumnInOther) {
                rowKeySet.add(hashKey(e));
            }

            final List<Object> keyColumn = _columnList.get(keyColumnIndex);
            final Set<Object> addedRowKeySet = deduplicate ? N.newHashSet() : N.emptySet();
            Object hashKey = null;

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                hashKey = hashKey(keyColumn.get(rowIndex));

                if (rowKeySet.contains(hashKey) == retain && (!deduplicate || addedRowKeySet.add(hashKey))) {
                    for (int i = 0; i < newColumnCount; i++) {
                        newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                    }
                }
            }
        } else {
            final int[] keyColumnIndexes = getColumnIndexes(keyColumnNames);
            final int[] keyColumnIndexesInOther = other.getColumnIndexes(keyColumnNames);

            final List<Object>[] keyColumnsInOther = new List[keyColumnCount];

            for (int i = 0; i < keyColumnCount; i++) {
                keyColumnsInOther[i] = other.getColumn(keyColumnIndexesInOther[i]);
            }

            final Set<Wrapper<Object[]>> rowKeySet = new HashSet<>();
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rowIndex = 0, otherSize = other.size(); rowIndex < otherSize; rowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(keyColumnCount);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0; i < keyColumnCount; i++) {
                    row[i] = keyColumnsInOther[i].get(rowIndex);
                }

                if (rowKeySet.add(rowWrapper)) {
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

            final Set<Wrapper<Object[]>> addedRowKeySet = deduplicate ? new HashSet<>() : N.emptySet();

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(keyColumnCount);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0; i < keyColumnCount; i++) {
                    row[i] = keyColumns[i].get(rowIndex);
                }

                if (rowKeySet.contains(rowWrapper) == retain && (!deduplicate || addedRowKeySet.add(rowWrapper))) {
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

            for (final Wrapper<Object[]> rw : rowKeySet) {
                Objectory.recycle(rw.value());
            }

            if (deduplicate) {
                for (final Wrapper<Object[]> rw : addedRowKeySet) {
                    Objectory.recycle(rw.value());
                }
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    @Override
    public DataSet intersection(final DataSet other) {
        return intersection(other, false);
    }

    @Override
    public DataSet intersection(final DataSet other, final boolean requiresSameColumns) {
        return removeOccurrences(other, getKeyColumnNames(other), requiresSameColumns, true);
    }

    @Override
    public DataSet intersection(final DataSet other, final Collection<String> keyColumnNames) {
        return intersection(other, keyColumnNames, false);
    }

    @Override
    public DataSet intersection(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        return removeOccurrences(other, keyColumnNames, requiresSameColumns, true);
    }

    @Override
    public DataSet difference(final DataSet other) {
        return difference(other, false);
    }

    @Override
    public DataSet difference(final DataSet other, final boolean requiresSameColumns) {
        return removeOccurrences(other, getKeyColumnNames(other), requiresSameColumns, false);
    }

    @Override
    public DataSet difference(final DataSet other, final Collection<String> keyColumnNames) {
        return difference(other, keyColumnNames, false);
    }

    @Override
    public DataSet difference(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        return removeOccurrences(other, keyColumnNames, requiresSameColumns, false);
    }

    @Override
    public DataSet symmetricDifference(final DataSet other) {
        return symmetricDifference(other, false);
    }

    @Override
    public DataSet symmetricDifference(final DataSet other, final boolean requiresSameColumns) {
        final Collection<String> keyColumnNames = getKeyColumnNames(other);

        return this.difference(other, keyColumnNames, requiresSameColumns).merge(other.difference(this, keyColumnNames, requiresSameColumns));
    }

    @Override
    public DataSet symmetricDifference(final DataSet other, final Collection<String> keyColumnNames) {
        return symmetricDifference(other, keyColumnNames, false);
    }

    @Override
    public DataSet symmetricDifference(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        return this.difference(other, keyColumnNames, requiresSameColumns).merge(other.difference(this, keyColumnNames, requiresSameColumns));
    }

    private DataSet removeOccurrences(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns, final boolean retain) {
        checkColumnNames(other, keyColumnNames, requiresSameColumns);

        final int keyColumnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
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
            final int keyColumnIndex = getColumnIndex(keyColumnName);
            final int keyColumnIndexInOther = other.getColumnIndex(keyColumnName);

            final List<Object> keyColumnInOther = other.getColumn(keyColumnIndexInOther);
            final Multiset<Object> rowKeySet = new Multiset<>();

            for (final Object val : keyColumnInOther) {
                rowKeySet.add(hashKey(val));
            }

            final List<Object> keyColumn = _columnList.get(keyColumnIndex);

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                if ((rowKeySet.remove(hashKey(keyColumn.get(rowIndex)), 1) > 0) == retain) {
                    for (int i = 0; i < keyColumnCount; i++) {
                        newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                    }
                }
            }
        } else {
            final int[] keyColumnIndexes = getColumnIndexes(keyColumnNames);
            final int[] keyColumnIndexesInOther = other.getColumnIndexes(keyColumnNames);

            final List<Object>[] keyColumnsInOther = new List[commonColumnCount];

            for (int i = 0; i < commonColumnCount; i++) {
                keyColumnsInOther[i] = other.getColumn(keyColumnIndexesInOther[i]);
            }

            final Multiset<Wrapper<Object[]>> rowKeySet = new Multiset<>();
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rowIndex = 0, otherSize = other.size(); rowIndex < otherSize; rowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(commonColumnCount);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0; i < commonColumnCount; i++) {
                    row[i] = keyColumnsInOther[i].get(rowIndex);
                }

                if (rowKeySet.add(rowWrapper, 1) == 0) {
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

            final List<Wrapper<Object[]>> rowKeys = new ArrayList<>(rowKeySet.elementSet());

            row = Objectory.createObjectArray(commonColumnCount);
            rowWrapper = Wrapper.of(row);

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                for (int i = 0; i < commonColumnCount; i++) {
                    row[i] = keyColumns[i].get(rowIndex);
                }

                if ((rowKeySet.remove(rowWrapper, 1) > 0) == retain) {
                    for (int i = 0; i < keyColumnCount; i++) {
                        newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                    }
                }
            }

            Objectory.recycle(row);
            row = null;

            for (final Wrapper<Object[]> rw : rowKeys) {
                Objectory.recycle(rw.value());
            }
        }

        return new RowDataSet(newColumnNameList, newColumnList);
    }

    @Override
    public DataSet merge(final DataSet other) {
        return merge(other, false);
    }

    @Override
    public DataSet merge(final DataSet other, final boolean requiresSameColumns) {
        checkIfColumnNamesAreSame(other, requiresSameColumns);

        return merge(other, 0, other.size(), other.columnNameList());
    }

    @Override
    public DataSet merge(final DataSet other, final Collection<String> columnNames) {
        return merge(other, 0, other.size(), columnNames);
    }

    @Override
    public DataSet merge(final DataSet other, final int fromRowIndex, final int toRowIndex) {
        return merge(other, fromRowIndex, toRowIndex, other.columnNameList());
    }

    @Override
    public DataSet merge(final DataSet other, final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        checkRowIndex(fromRowIndex, toRowIndex, other.size());

        final RowDataSet result = (RowDataSet) copy();

        merge(result, other, fromRowIndex, toRowIndex, columnNames);

        return result;
    }

    private void merge(final RowDataSet result, final DataSet other, final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        List<Object> column = null;

        for (final String columnName : columnNames) {
            if (!result.containsColumn(columnName)) {
                if (column == null) {
                    column = new ArrayList<>(size() + (toRowIndex - fromRowIndex));
                    N.fill(column, 0, size(), null);
                }

                result.addColumn(columnName, column);
            }
        }

        List<Object> nulls = null;

        for (final String columnName : result._columnNameList) {
            column = result._columnList.get(result.getColumnIndex(columnName));

            if (other.containsColumn(columnName)) {
                final int columnIndex = other.getColumnIndex(columnName);

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

        result.mergeProperties(other.properties());
    }

    //    @Override
    //    public DataSet merge(final DataSet a, final DataSet b) {
    //        return N.merge(this, a, b);
    //    }

    @Override
    public DataSet merge(final Collection<? extends DataSet> others) {
        return merge(others, false);
    }

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
        final List<String> commonColumnNameList = new ArrayList<>(_columnNameList);
        commonColumnNameList.retainAll(other.columnNameList());

        if (N.isEmpty(commonColumnNameList)) {
            throw new IllegalArgumentException("These two DataSets don't have common column names: " + _columnNameList + ", " + other.columnNameList());
        }
        return commonColumnNameList;
    }

    private void checkIfColumnNamesAreSame(final DataSet other, final boolean requiresSameColumns) throws IllegalArgumentException {
        //noinspection SlowListContainsAll
        if (requiresSameColumns && !(columnCount() == other.columnCount() && _columnNameList.containsAll(other.columnNameList()))) {
            throw new IllegalArgumentException("These two DataSets don't have same column names: " + _columnNameList + ", " + other.columnNameList());
        }
    }

    private void checkColumnNames(final DataSet other, final Collection<String> keyColumnNames, final boolean requiresSameColumns)
            throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);

        N.checkArgument(containsAllColumns(keyColumnNames), "This DataSet={} doesn't contain all keyColumnNames={}", columnNameList(), keyColumnNames);

        N.checkArgument(other.containsAllColumns(keyColumnNames), "Other DataSet={} doesn't contain all keyColumnNames={}", other.columnNameList(),
                keyColumnNames);

        checkIfColumnNamesAreSame(other, requiresSameColumns);
    }

    @Override
    public DataSet cartesianProduct(final DataSet other) {
        final Collection<String> tmp = N.intersection(_columnNameList, other.columnNameList());
        if (N.notEmpty(tmp)) {
            throw new IllegalArgumentException(tmp + " are included in both DataSets: " + _columnNameList + " : " + other.columnNameList());
        }

        final int aSize = size();
        final int bSize = other.size();
        final int aColumnCount = columnCount();
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

    @Override
    public Stream<DataSet> split(final int chunkSize) {
        return split(chunkSize, _columnNameList);
    }

    @Override
    public Stream<DataSet> split(final int chunkSize, final Collection<String> columnNames) throws IllegalArgumentException {
        final int[] columnIndexes = checkColumnNames(columnNames);
        N.checkArgPositive(chunkSize, cs.chunkSize);

        final int expectedModCount = modCount;
        final int totalSize = size();

        //noinspection resource
        return IntStream.range(0, totalSize, chunkSize).mapToObj(from -> {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }

            return RowDataSet.this.copy(from, from <= totalSize - chunkSize ? from + chunkSize : totalSize, columnNames, columnIndexes, true);
        });
    }

    @Override
    public List<DataSet> splitToList(final int chunkSize) {
        return splitToList(chunkSize, _columnNameList);
    }

    @Override
    public List<DataSet> splitToList(final int chunkSize, final Collection<String> columnNames) throws IllegalArgumentException {
        final int[] columnIndexes = checkColumnNames(columnNames);
        N.checkArgPositive(chunkSize, cs.chunkSize);

        final List<DataSet> res = new ArrayList<>();

        for (int i = 0, totalSize = size(); i < totalSize; i = i <= totalSize - chunkSize ? i + chunkSize : totalSize) {
            res.add(copy(i, i <= totalSize - chunkSize ? i + chunkSize : totalSize, columnNames, columnIndexes, true));
        }

        return res;
    }

    //    @Deprecated
    //    @Override
    //    public List<DataSet> splitt(int chunkSize) {
    //        return splitToList(chunkSize);
    //    }

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

    @Override
    public DataSet slice(final Collection<String> columnNames) {
        return slice(0, size(), columnNames);
    }

    @Override
    public DataSet slice(final int fromRowIndex, final int toRowIndex) {
        return slice(fromRowIndex, toRowIndex, _columnNameList);
    }

    @Override
    public DataSet slice(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, size());
        DataSet ds = null;

        if (N.isEmpty(columnNames)) {
            ds = N.newEmptyDataSet();
        } else {
            final int[] columnIndexes = checkColumnNames(columnNames);
            final List<String> newColumnNames = new ArrayList<>(columnNames);
            final List<List<Object>> newColumnList = new ArrayList<>(newColumnNames.size());

            if (fromRowIndex > 0 || toRowIndex < size()) {
                for (final int columnIndex : columnIndexes) {
                    newColumnList.add(_columnList.get(columnIndex).subList(fromRowIndex, toRowIndex));
                }
            } else {
                for (final int columnIndex : columnIndexes) {
                    newColumnList.add(_columnList.get(columnIndex));
                }
            }

            ds = new RowDataSet(newColumnNames, newColumnList, _properties);
        }

        ds.freeze();

        return ds;
    }

    @Override
    public Paginated<DataSet> paginate(final int pageSize) {
        return paginate(_columnNameList, pageSize);
    }

    @Override
    public Paginated<DataSet> paginate(final Collection<String> columnNames, final int pageSize) {
        checkColumnNames(columnNames);

        return new PaginatedDataSet(columnNames, pageSize);
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

    @Override
    public <T> Stream<T> stream(final String columnName) {
        return stream(0, size(), columnName);
    }

    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final String columnName) throws IllegalArgumentException {
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

    //    @Override
    //    public <T> Stream<T> stream(final Function<? super DisposableObjArray, ? extends T> rowMapper) {
    //        return stream(0, size(), rowMapper);
    //    }
    //
    //    @Override
    //    public <T> Stream<T> stream(int fromRowIndex, int toRowIndex, final Function<? super DisposableObjArray, ? extends T> rowMapper) {
    //        return stream(_columnNameList, fromRowIndex, toRowIndex, rowMapper);
    //    }
    //
    //    @Override
    //    public <T> Stream<T> stream(Collection<String> columnNames, final Function<? super DisposableObjArray, ? extends T> rowMapper) {
    //        return stream(columnNames, 0, size(), rowMapper);
    //    }
    //
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
    //                N.checkArgNotNegative(n, s.n);
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

    @Override
    public <T> Stream<T> stream(final Class<? extends T> rowType) {
        return stream(0, size(), rowType);
    }

    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Class<? extends T> rowType) {
        return stream(fromRowIndex, toRowIndex, _columnNameList, rowType);
    }

    @Override
    public <T> Stream<T> stream(final Collection<String> columnNames, final Class<? extends T> rowType) {
        return stream(0, size(), columnNames, rowType);
    }

    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Class<? extends T> rowType) {
        return stream(fromRowIndex, toRowIndex, columnNames, null, rowType, null);
    }

    @Override
    public <T> Stream<T> stream(final IntFunction<? extends T> rowSupplier) {
        return stream(0, size(), rowSupplier);
    }

    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final IntFunction<? extends T> rowSupplier) {
        return stream(fromRowIndex, toRowIndex, _columnNameList, rowSupplier);
    }

    @Override
    public <T> Stream<T> stream(final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier) {
        return stream(0, size(), columnNames, rowSupplier);
    }

    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final IntFunction<? extends T> rowSupplier) {
        return stream(fromRowIndex, toRowIndex, columnNames, null, null, rowSupplier);
    }

    @Override
    public <T> Stream<T> stream(final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType) {
        return stream(0, size(), _columnNameList, prefixAndFieldNameMap, rowType);
    }

    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Map<String, String> prefixAndFieldNameMap,
            final Class<? extends T> rowType) {
        return stream(fromRowIndex, toRowIndex, _columnNameList, prefixAndFieldNameMap, rowType);
    }

    @Override
    public <T> Stream<T> stream(final Collection<String> columnNames, final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType) {
        return stream(0, size(), columnNames, prefixAndFieldNameMap, rowType);
    }

    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType) throws IllegalArgumentException {
        N.checkArgument(ClassUtil.isBeanClass(rowType), "{} is not a bean class", rowType);

        return stream(fromRowIndex, toRowIndex, columnNames, prefixAndFieldNameMap, rowType, null);
    }

    private <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> inputRowClass, final IntFunction<? extends T> inputRowSupplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int[] columnIndexes = checkColumnNames(columnNames);

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
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

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

            void checkConcurrentModification() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        });
    }

    @Override
    public <T> Stream<T> stream(final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) {
        return stream(0, size(), rowMapper);
    }

    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) {
        return stream(fromRowIndex, toRowIndex, _columnNameList, rowMapper);
    }

    @Override
    public <T> Stream<T> stream(final Collection<String> columnNames, final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) {
        return stream(0, size(), columnNames, rowMapper);
    }

    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) throws IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int[] columnIndexes = checkColumnNames(columnNames);

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
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            void checkConcurrentModification() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        });
    }

    @Override
    public <T> Stream<T> stream(final Tuple2<String, String> columnNames, final BiFunction<?, ?, ? extends T> rowMapper) {
        return stream(0, size(), columnNames, rowMapper);
    }

    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Tuple2<String, String> columnNames,
            final BiFunction<?, ?, ? extends T> rowMapper) {
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
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            void checkConcurrentModification() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        });
    }

    @Override
    public <T> Stream<T> stream(final Tuple3<String, String, String> columnNames, final TriFunction<?, ?, ?, ? extends T> rowMapper) {
        return stream(0, size(), columnNames, rowMapper);
    }

    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Tuple3<String, String, String> columnNames,
            final TriFunction<?, ?, ?, ? extends T> rowMapper) {
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
            public void advance(final long n) throws IllegalArgumentException {
                N.checkArgNotNegative(n, cs.n);

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            void checkConcurrentModification() {
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

    @Override
    public <R, E extends Exception> R apply(final Throwables.Function<? super DataSet, ? extends R, E> func) throws E {
        return func.apply(this);
    }

    @Override
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super DataSet, ? extends R, E> func) throws E {
        if (size() > 0) {
            return Optional.ofNullable(func.apply(this));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public <E extends Exception> void accept(final Throwables.Consumer<? super DataSet, E> action) throws E {
        action.accept(this);
    }

    @Override
    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super DataSet, E> action) throws E {
        if (size() > 0) {
            action.accept(this);

            return OrElse.TRUE;
        }

        return OrElse.FALSE;
    }

    @Override
    public void freeze() {
        _isFrozen = true;
    }

    @Override
    public boolean isFrozen() {
        return _isFrozen;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void trimToSize() {
        if (_columnList instanceof ArrayList) {
            ((ArrayList<?>) _columnList).trimToSize();
        }

        for (final List<Object> column : _columnList) {
            if (column instanceof ArrayList) {
                ((ArrayList<?>) column).trimToSize();
            }
        }
    }

    @Override
    public int size() {
        return (_columnList.size() == 0) ? 0 : _columnList.get(0).size();
    }

    @Override
    public void clear() {
        checkFrozen();

        for (final List<Object> objects : _columnList) {
            objects.clear();
        }

        // columnList.clear();
        modCount++;

        // Runtime.getRuntime().gc();
    }

    @Override
    public Map<String, Object> properties() {
        if (_properties == N.<String, Object> emptyMap()) {
            return _properties;
        } else {
            return ImmutableMap.wrap(_properties);
        }
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
        //noinspection resource
        return IntStream.range(0, columnCount()).mapToObj(this::getColumn);
    }

    @Override
    public Map<String, ImmutableList<Object>> columnMap() {
        final Map<String, ImmutableList<Object>> result = new LinkedHashMap<>();

        for (final String columnName : _columnNameList) {
            result.put(columnName, getColumn(columnName));
        }

        return result;
    }

    //    @Override
    //    public DataSetBuilder builder() {
    //        return Builder.of(this);
    //    }

    @Override
    public void println() {
        println(0, size());
    }

    @Override
    public void println(final int fromRowIndex, final int toRowIndex) {
        println(fromRowIndex, toRowIndex, _columnNameList); // NOSONAR
    }

    @Override
    public void println(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames) {
        println(fromRowIndex, toRowIndex, columnNames, IOUtil.newOutputStreamWriter(System.out)); // NOSONAR
    }

    @Override
    public void println(final Writer outputWriter) throws UncheckedIOException {
        println(0, size(), _columnNameList, outputWriter);
    }

    @Override
    public void println(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Writer outputWriter)
            throws IllegalArgumentException, UncheckedIOException {
        final int[] columnIndexes = N.isEmpty(columnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(columnNames);
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(outputWriter, cs.outputWriter);

        final boolean isBufferedWriter = IOUtil.isBufferedWriter(outputWriter);
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
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle((BufferedWriter) bw);
            }
        }
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + _columnNameList.hashCode();
        return (h * 31) + _columnList.hashCode();
    }

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

    @Override
    public String toString() {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append("{columnNames=");
            sb.append(_columnNameList);

            sb.append(", properties=");
            sb.append(_properties);

            sb.append(", isFrozen=");
            sb.append(_isFrozen);

            sb.append(", columns={");

            for (int i = 0, columnCount = _columnNameList.size(); i < columnCount; i++) {
                if (i > 0) {
                    sb.append(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                }

                sb.append(_columnNameList.get(i)).append("=").append(N.toString(_columnList.get(i)));
            }

            sb.append("}}");

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    private List<String> filterColumnNames(final Collection<String> columnNames, final Predicate<? super String> columnNameFilter) {
        if (N.isEmpty(columnNames)) {
            return new ArrayList<>();
        }

        final List<String> ret = new ArrayList<>(columnNames.size() / 2);

        for (final String columnName : columnNames) {
            if (columnNameFilter.test(columnName)) {
                ret.add(columnName);
            }
        }

        return ret;
    }

    void checkFrozen() {
        if (_isFrozen) {
            throw new IllegalStateException("This DataSet is frozen, can't modify it.");
        }
    }

    void checkRowNum(final int rowNum) {
        if ((rowNum < 0) || (rowNum >= size())) {
            throw new IllegalArgumentException("Invalid row number: " + rowNum + ". It must be >= 0 and < " + size());
        }
    }

    void checkRowIndex(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(fromRowIndex, toRowIndex, size());
    }

    void checkRowIndex(final int fromRowIndex, final int toRowIndex, final int size) throws IndexOutOfBoundsException {
        if ((fromRowIndex < 0) || (fromRowIndex > toRowIndex) || (toRowIndex > size)) {
            throw new IndexOutOfBoundsException("Row index range [" + fromRowIndex + ", " + toRowIndex + "] is out-of-bounds for length " + size);
        }
    }

    static Object hashKey(final Object obj) {
        return N.hashKey(obj);
    }

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

        private PaginatedDataSet(final Collection<String> columnNames, final int pageSize) {
            // N.checkArgNotEmpty(columnNames, "columnNames"); // empty DataSet.
            N.checkArgPositive(pageSize, cs.pageSize);

            this.columnNames = columnNames;
            this.pageSize = pageSize;

            totalPages = ((size() % pageSize) == 0) ? (size() / pageSize) : ((size() / pageSize) + 1);

            currentPageNum = 0;
        }

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

                    final DataSet ret = getPage(cursor);
                    cursor++;
                    return ret;
                }
            };
        }

        @Override
        public boolean hasNext() {
            return currentPageNum < totalPages;
        }

        @Override
        public DataSet nextPage() {
            return absolute(currentPageNum + 1).currentPage();
        }

        @Override
        public DataSet currentPage() {
            return getPage(currentPageNum);
        }

        @Override
        public DataSet previousPage() {
            return absolute(currentPageNum - 1).currentPage();
        }

        @Override
        public Optional<DataSet> firstPage() {
            return pageCount() == 0 ? Optional.empty() : Optional.of(absolute(0).currentPage());
        }

        @Override
        public Optional<DataSet> lastPage() {
            return pageCount() == 0 ? Optional.empty() : Optional.of(absolute(pageCount() - 1).currentPage());
        }

        @Override
        public DataSet getPage(final int pageNum) {
            checkConcurrentModification();
            checkPageNumber(pageNum);

            synchronized (pagePool) {
                DataSet page = pagePool.get(pageNum);

                if (page == null) {
                    final int offset = pageNum * pageSize;
                    page = RowDataSet.this.slice(offset, Math.min(offset + pageSize, size()), columnNames);

                    pagePool.put(pageNum, page);
                }

                return page;
            }
        }

        @Override
        public Paginated<DataSet> absolute(final int pageNumber) {
            checkPageNumber(pageNumber);

            currentPageNum = pageNumber;

            return this;
        }

        @Override
        public int currentPageNum() {
            return currentPageNum;
        }

        @Override
        public int pageSize() {
            return pageSize;
        }

        /**
         * @deprecated replaced by {@link #totalPages()}
         */
        @Deprecated
        @Override
        public int pageCount() {
            return totalPages;
        }

        @Override
        public int totalPages() {
            return totalPages;
        }

        @Override
        public Stream<DataSet> stream() {
            return Stream.of(iterator());
        }

        final void checkConcurrentModification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        private void checkPageNumber(final int pageNumber) {
            if ((pageNumber < 0) || (pageNumber >= pageCount())) {
                throw new IllegalArgumentException(pageNumber + " out of page index [0, " + pageCount() + ")");
            }
        }
    }
}
