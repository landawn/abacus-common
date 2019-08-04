/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * It's immutable.
 * 
 * @param <T>
 * 
 * @since 0.8
 * 
 * @author haiyang li
 */
public final class HBaseColumn<T> implements Comparable<HBaseColumn<T>> {

    public static final HBaseColumn<Boolean> EMPTY_BOOLEAN_COLUMN = HBaseColumn.valueOf(false, 0);
    public static final HBaseColumn<Character> EMPTY_CHAR_COLUMN = HBaseColumn.valueOf((char) 0, 0);
    public static final HBaseColumn<Byte> EMPTY_BYTE_COLUMN = HBaseColumn.valueOf((byte) 0, 0);
    public static final HBaseColumn<Short> EMPTY_SHORT_COLUMN = HBaseColumn.valueOf((short) 0, 0);
    public static final HBaseColumn<Integer> EMPTY_INT_COLUMN = HBaseColumn.valueOf(0, 0);
    public static final HBaseColumn<Long> EMPTY_LONG_COLUMN = HBaseColumn.valueOf(0L, 0);
    public static final HBaseColumn<Float> EMPTY_FLOAT_COLUMN = HBaseColumn.valueOf(0f, 0);
    public static final HBaseColumn<Double> EMPTY_DOUBLE_COLUMN = HBaseColumn.valueOf(0d, 0);
    public static final HBaseColumn<Object> EMPTY_OBJECT_COLUMN = HBaseColumn.valueOf(null, 0);

    private static final long LATEST_TIMESTAMP = Long.MAX_VALUE;

    private static final BiMap<Class<?>, HBaseColumn<?>> emptyColumnPool = new BiMap<>();

    static {
        emptyColumnPool.put(boolean.class, EMPTY_BOOLEAN_COLUMN);
        emptyColumnPool.put(char.class, EMPTY_CHAR_COLUMN);
        emptyColumnPool.put(byte.class, EMPTY_BYTE_COLUMN);
        emptyColumnPool.put(short.class, EMPTY_SHORT_COLUMN);
        emptyColumnPool.put(int.class, EMPTY_INT_COLUMN);
        emptyColumnPool.put(long.class, EMPTY_LONG_COLUMN);
        emptyColumnPool.put(float.class, EMPTY_FLOAT_COLUMN);
        emptyColumnPool.put(double.class, EMPTY_DOUBLE_COLUMN);
        emptyColumnPool.put(String.class, EMPTY_OBJECT_COLUMN);
    }

    public static final Comparator<HBaseColumn<?>> DESC_HBASE_COLUMN_COMPARATOR = new Comparator<HBaseColumn<?>>() {
        @Override
        public int compare(HBaseColumn<?> o1, HBaseColumn<?> o2) {
            return o2.version > o1.version ? 1 : o2.version == o1.version ? 0 : -1; // Long.compare(o2.version, o1.version);
        }
    };

    public static final Comparator<Long> DESC_HBASE_VERSION_COMPARATOR = new Comparator<Long>() {
        @Override
        public int compare(Long o1, Long o2) {
            return o2.longValue() > o1.longValue() ? 1 : o2.longValue() == o1.longValue() ? 0 : -1; // Long.compare(o2, o1);
        }
    };

    private final T value;
    private final long version;

    public HBaseColumn(T value) {
        this(value, LATEST_TIMESTAMP);
    }

    public HBaseColumn(T value, long version) {
        this.value = value;
        this.version = version;
    }

    public static <T> HBaseColumn<T> emptyOf(Class<?> targetClass) {
        final HBaseColumn<?> column = emptyColumnPool.get(targetClass);

        return (HBaseColumn<T>) (column == null ? EMPTY_OBJECT_COLUMN : column);
    }

    public static <T> HBaseColumn<T> valueOf(T value) {
        return new HBaseColumn<T>(value);
    }

    public static <T> HBaseColumn<T> valueOf(T value, long version) {
        return new HBaseColumn<T>(value, version);
    }

    public static <T> List<HBaseColumn<T>> asList(T value) {
        return N.asList(new HBaseColumn<T>(value));
    }

    public static <T> List<HBaseColumn<T>> asList(T value, long version) {
        return N.asList(new HBaseColumn<T>(value, version));
    }

    public static <T> Set<HBaseColumn<T>> asSet(T value) {
        return N.asSet(new HBaseColumn<T>(value));
    }

    public static <T> Set<HBaseColumn<T>> asSet(T value, long version) {
        return N.asSet(new HBaseColumn<T>(value, version));
    }

    /**
     * Returns a sorted set descended by version
     * 
     * @param value
     * @return
     */
    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(T value) {
        return asSortedSet(value, DESC_HBASE_COLUMN_COMPARATOR);
    }

    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(T value, Comparator<HBaseColumn<?>> cmp) {
        final SortedSet<HBaseColumn<T>> set = new TreeSet<HBaseColumn<T>>(cmp == null ? DESC_HBASE_COLUMN_COMPARATOR : cmp);

        set.add(HBaseColumn.valueOf(value));

        return set;
    }

    /**
     * Returns a sorted set descended by version
     * 
     * @param value
     * @param version
     * @return
     */
    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(T value, long version) {
        return asSortedSet(value, version, DESC_HBASE_COLUMN_COMPARATOR);
    }

    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(T value, long version, Comparator<HBaseColumn<?>> cmp) {
        final SortedSet<HBaseColumn<T>> set = new TreeSet<HBaseColumn<T>>(cmp == null ? DESC_HBASE_COLUMN_COMPARATOR : cmp);

        set.add(HBaseColumn.valueOf(value, version));

        return set;
    }

    public static <T> Map<Long, HBaseColumn<T>> asMap(T value) {
        final HBaseColumn<T> hbaseColumn = HBaseColumn.valueOf(value);

        return N.asMap(hbaseColumn.version(), hbaseColumn);
    }

    public static <T> Map<Long, HBaseColumn<T>> asMap(T value, long version) {
        final HBaseColumn<T> hbaseColumn = HBaseColumn.valueOf(value, version);

        return N.asMap(hbaseColumn.version(), hbaseColumn);
    }

    /**
     * Returns a sorted map descended by version
     * 
     * @param value
     * @return
     */
    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(T value) {
        return asSortedMap(value, DESC_HBASE_VERSION_COMPARATOR);
    }

    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(T value, Comparator<Long> cmp) {
        final SortedMap<Long, HBaseColumn<T>> map = new TreeMap<Long, HBaseColumn<T>>(cmp == null ? DESC_HBASE_VERSION_COMPARATOR : cmp);
        final HBaseColumn<T> hbaseColumn = HBaseColumn.valueOf(value);

        map.put(hbaseColumn.version(), hbaseColumn);

        return map;
    }

    /**
     * Returns a sorted map descended by version
     * 
     * @param value
     * @param version
     * @return
     */
    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(T value, long version) {
        return asSortedMap(value, version, DESC_HBASE_VERSION_COMPARATOR);
    }

    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(T value, long version, Comparator<Long> cmp) {
        final SortedMap<Long, HBaseColumn<T>> map = new TreeMap<Long, HBaseColumn<T>>(cmp == null ? DESC_HBASE_VERSION_COMPARATOR : cmp);
        final HBaseColumn<T> hbaseColumn = HBaseColumn.valueOf(value, version);

        map.put(hbaseColumn.version(), hbaseColumn);

        return map;
    }

    public T value() {
        return value;
    }

    public long version() {
        return version;
    }

    public HBaseColumn<T> copy() {
        return new HBaseColumn<T>(this.value, this.version);
    }

    public boolean isNull() {
        return (value == null && version == 0) || emptyColumnPool.containsValue(this);
    }

    @Override
    public int compareTo(HBaseColumn<T> o) {
        return version > o.version ? 1 : version == o.version ? 0 : -1;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(version);
        h = 31 * h + N.hashCode(value);

        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof HBaseColumn) {
            HBaseColumn<T> other = (HBaseColumn<T>) obj;

            if (N.equals(version, other.version) && N.equals(value, other.value)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return version + ":" + N.stringOf(value);
    }
}
