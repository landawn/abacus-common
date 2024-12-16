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
 *
 * @param <T>
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

    public static final Comparator<HBaseColumn<?>> DESC_HBASE_COLUMN_COMPARATOR = (o1, o2) -> Long.compare(o2.version, o1.version);

    public static final Comparator<Long> DESC_HBASE_VERSION_COMPARATOR = Comparator.comparing(Long::longValue).reversed();

    private final T value;

    private final long version;

    /**
     *
     * @param value
     */
    public HBaseColumn(final T value) {
        this(value, LATEST_TIMESTAMP);
    }

    /**
     *
     * @param value
     * @param version
     */
    public HBaseColumn(final T value, final long version) {
        this.value = value;
        this.version = version;
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @return
     */
    public static <T> HBaseColumn<T> emptyOf(final Class<?> targetClass) {
        final HBaseColumn<?> column = emptyColumnPool.get(targetClass);

        return (HBaseColumn<T>) (column == null ? EMPTY_OBJECT_COLUMN : column);
    }

    /**
     *
     * @param <T>
     * @param value
     * @return
     */
    public static <T> HBaseColumn<T> valueOf(final T value) {
        return new HBaseColumn<>(value);
    }

    /**
     *
     * @param <T>
     * @param value
     * @param version
     * @return
     */
    public static <T> HBaseColumn<T> valueOf(final T value, final long version) {
        return new HBaseColumn<>(value, version);
    }

    /**
     *
     * @param <T>
     * @param value
     * @return
     */
    public static <T> List<HBaseColumn<T>> asList(final T value) {
        return N.asList(new HBaseColumn<>(value));
    }

    /**
     *
     * @param <T>
     * @param value
     * @param version
     * @return
     */
    public static <T> List<HBaseColumn<T>> asList(final T value, final long version) {
        return N.asList(new HBaseColumn<>(value, version));
    }

    /**
     *
     * @param <T>
     * @param value
     * @return
     */
    public static <T> Set<HBaseColumn<T>> asSet(final T value) {
        return N.asSet(new HBaseColumn<>(value));
    }

    /**
     *
     * @param <T>
     * @param value
     * @param version
     * @return
     */
    public static <T> Set<HBaseColumn<T>> asSet(final T value, final long version) {
        return N.asSet(new HBaseColumn<>(value, version));
    }

    /**
     * Returns a sorted set descended by version.
     *
     * @param <T>
     * @param value
     * @return
     */
    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(final T value) {
        return asSortedSet(value, DESC_HBASE_COLUMN_COMPARATOR);
    }

    /**
     * As sorted set.
     *
     * @param <T>
     * @param value
     * @param cmp
     * @return
     */
    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(final T value, final Comparator<HBaseColumn<?>> cmp) {
        final SortedSet<HBaseColumn<T>> set = new TreeSet<>(cmp == null ? DESC_HBASE_COLUMN_COMPARATOR : cmp);

        set.add(HBaseColumn.valueOf(value));

        return set;
    }

    /**
     * Returns a sorted set descended by version.
     *
     * @param <T>
     * @param value
     * @param version
     * @return
     */
    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(final T value, final long version) {
        return asSortedSet(value, version, DESC_HBASE_COLUMN_COMPARATOR);
    }

    /**
     * As sorted set.
     *
     * @param <T>
     * @param value
     * @param version
     * @param cmp
     * @return
     */
    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(final T value, final long version, final Comparator<HBaseColumn<?>> cmp) {
        final SortedSet<HBaseColumn<T>> set = new TreeSet<>(cmp == null ? DESC_HBASE_COLUMN_COMPARATOR : cmp);

        set.add(HBaseColumn.valueOf(value, version));

        return set;
    }

    /**
     *
     * @param <T>
     * @param value
     * @return
     */
    public static <T> Map<Long, HBaseColumn<T>> asMap(final T value) {
        final HBaseColumn<T> hbaseColumn = HBaseColumn.valueOf(value);

        return N.asMap(hbaseColumn.version(), hbaseColumn);
    }

    /**
     *
     * @param <T>
     * @param value
     * @param version
     * @return
     */
    public static <T> Map<Long, HBaseColumn<T>> asMap(final T value, final long version) {
        final HBaseColumn<T> hbaseColumn = HBaseColumn.valueOf(value, version);

        return N.asMap(hbaseColumn.version(), hbaseColumn);
    }

    /**
     * Returns a sorted map descended by version.
     *
     * @param <T>
     * @param value
     * @return
     */
    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(final T value) {
        return asSortedMap(value, DESC_HBASE_VERSION_COMPARATOR);
    }

    /**
     * As sorted map.
     *
     * @param <T>
     * @param value
     * @param cmp
     * @return
     */
    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(final T value, final Comparator<Long> cmp) {
        final SortedMap<Long, HBaseColumn<T>> map = new TreeMap<>(cmp == null ? DESC_HBASE_VERSION_COMPARATOR : cmp);
        final HBaseColumn<T> hbaseColumn = HBaseColumn.valueOf(value);

        map.put(hbaseColumn.version(), hbaseColumn);

        return map;
    }

    /**
     * Returns a sorted map descended by version.
     *
     * @param <T>
     * @param value
     * @param version
     * @return
     */
    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(final T value, final long version) {
        return asSortedMap(value, version, DESC_HBASE_VERSION_COMPARATOR);
    }

    /**
     * As sorted map.
     *
     * @param <T>
     * @param value
     * @param version
     * @param cmp
     * @return
     */
    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(final T value, final long version, final Comparator<Long> cmp) {
        final SortedMap<Long, HBaseColumn<T>> map = new TreeMap<>(cmp == null ? DESC_HBASE_VERSION_COMPARATOR : cmp);
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
        return new HBaseColumn<>(value, version);
    }

    /**
     * Checks if is {@code null}.
     *
     * @return {@code true}, if is null
     */
    public boolean isNull() {
        return (value == null && version == 0) || emptyColumnPool.containsValue(this);
    }

    /**
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(final HBaseColumn<T> o) {
        return Long.compare(version, o.version);
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(version);
        return 31 * h + N.hashCode(value);
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

        if (obj instanceof HBaseColumn) {
            final HBaseColumn<T> other = (HBaseColumn<T>) obj;

            return N.equals(version, other.version) && N.equals(value, other.value);
        }

        return false;
    }

    @Override
    public String toString() {
        return version + ":" + N.stringOf(value);
    }
}
