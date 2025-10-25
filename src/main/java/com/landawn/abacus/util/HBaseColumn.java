/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
 * Represents a column value in HBase with its associated version (timestamp).
 * This class provides a type-safe wrapper for HBase column values along with their version information.
 * 
 * <p>The class is immutable and implements {@link Comparable} based on version timestamps.
 * It provides various factory methods for creating instances and converting them to different collection types.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a column with current timestamp
 * HBaseColumn<String> col1 = HBaseColumn.valueOf("value");
 * 
 * // Create a column with specific version
 * HBaseColumn<String> col2 = HBaseColumn.valueOf("value", 12345L);
 * 
 * // Get value and version
 * String value = col2.value();
 * long version = col2.version();
 * }</pre>
 *
 * @param <T> the type of the column value
 */
public final class HBaseColumn<T> implements Comparable<HBaseColumn<T>> {

    /** Empty boolean column instance with value false and version 0 */
    public static final HBaseColumn<Boolean> EMPTY_BOOLEAN_COLUMN = HBaseColumn.valueOf(false, 0);

    /** Empty character column instance with value '\0' and version 0 */
    public static final HBaseColumn<Character> EMPTY_CHAR_COLUMN = HBaseColumn.valueOf((char) 0, 0);

    /** Empty byte column instance with value 0 and version 0 */
    public static final HBaseColumn<Byte> EMPTY_BYTE_COLUMN = HBaseColumn.valueOf((byte) 0, 0);

    /** Empty short column instance with value 0 and version 0 */
    public static final HBaseColumn<Short> EMPTY_SHORT_COLUMN = HBaseColumn.valueOf((short) 0, 0);

    /** Empty integer column instance with value 0 and version 0 */
    public static final HBaseColumn<Integer> EMPTY_INT_COLUMN = HBaseColumn.valueOf(0, 0);

    /** Empty long column instance with value 0L and version 0 */
    public static final HBaseColumn<Long> EMPTY_LONG_COLUMN = HBaseColumn.valueOf(0L, 0);

    /** Empty float column instance with value 0f and version 0 */
    public static final HBaseColumn<Float> EMPTY_FLOAT_COLUMN = HBaseColumn.valueOf(0f, 0);

    /** Empty double column instance with value 0d and version 0 */
    public static final HBaseColumn<Double> EMPTY_DOUBLE_COLUMN = HBaseColumn.valueOf(0d, 0);

    /** Empty object column instance with null value and version 0 */
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

    /** Comparator that sorts HBaseColumn instances by version in descending order */
    public static final Comparator<HBaseColumn<?>> DESC_HBASE_COLUMN_COMPARATOR = (o1, o2) -> Long.compare(o2.version, o1.version);

    /** Comparator that sorts version numbers (Long) in descending order */
    public static final Comparator<Long> DESC_HBASE_VERSION_COMPARATOR = Comparator.comparing(Long::longValue).reversed();

    private final T value;

    private final long version;

    /**
     * Constructs an HBaseColumn with the specified value and the latest timestamp.
     * The version will be set to {@code Long.MAX_VALUE}.
     *
     * @param value the column value
     */
    public HBaseColumn(final T value) {
        this(value, LATEST_TIMESTAMP);
    }

    /**
     * Constructs an HBaseColumn with the specified value and version.
     *
     * @param value the column value
     * @param version the version timestamp
     */
    public HBaseColumn(final T value, final long version) {
        this.value = value;
        this.version = version;
    }

    /**
     * Returns an empty HBaseColumn instance for the specified target class.
     * Primitive types return their corresponding empty constant instances,
     * while other types return {@code EMPTY_OBJECT_COLUMN}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HBaseColumn<Integer> empty = HBaseColumn.emptyOf(int.class);
     * // Returns EMPTY_INT_COLUMN
     * }</pre>
     *
     * @param <T> the type of the column value
     * @param targetClass the class type for which to get an empty column
     * @return an empty HBaseColumn instance
     */
    public static <T> HBaseColumn<T> emptyOf(final Class<?> targetClass) {
        final HBaseColumn<?> column = emptyColumnPool.get(targetClass);

        return (HBaseColumn<T>) (column == null ? EMPTY_OBJECT_COLUMN : column);
    }

    /**
     * Creates an HBaseColumn instance with the specified value and the latest timestamp.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @return a new HBaseColumn instance
     */
    public static <T> HBaseColumn<T> valueOf(final T value) {
        return new HBaseColumn<>(value);
    }

    /**
     * Creates an HBaseColumn instance with the specified value and version.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @param version the version timestamp
     * @return a new HBaseColumn instance
     */
    public static <T> HBaseColumn<T> valueOf(final T value, final long version) {
        return new HBaseColumn<>(value, version);
    }

    /**
     * Creates a List containing a single HBaseColumn with the specified value and latest timestamp.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @return a List containing the HBaseColumn
     */
    public static <T> List<HBaseColumn<T>> asList(final T value) {
        return N.asList(new HBaseColumn<>(value));
    }

    /**
     * Creates a List containing a single HBaseColumn with the specified value and version.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @param version the version timestamp
     * @return a List containing the HBaseColumn
     */
    public static <T> List<HBaseColumn<T>> asList(final T value, final long version) {
        return N.asList(new HBaseColumn<>(value, version));
    }

    /**
     * Creates a Set containing a single HBaseColumn with the specified value and latest timestamp.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @return a Set containing the HBaseColumn
     */
    public static <T> Set<HBaseColumn<T>> asSet(final T value) {
        return N.asSet(new HBaseColumn<>(value));
    }

    /**
     * Creates a Set containing a single HBaseColumn with the specified value and version.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @param version the version timestamp
     * @return a Set containing the HBaseColumn
     */
    public static <T> Set<HBaseColumn<T>> asSet(final T value, final long version) {
        return N.asSet(new HBaseColumn<>(value, version));
    }

    /**
     * Creates a SortedSet containing a single HBaseColumn with the specified value,
     * sorted by version in descending order.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @return a SortedSet containing the HBaseColumn
     */
    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(final T value) {
        return asSortedSet(value, DESC_HBASE_COLUMN_COMPARATOR);
    }

    /**
     * Creates a SortedSet containing a single HBaseColumn with the specified value,
     * using the provided comparator for sorting.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @param cmp the comparator to use for sorting, or null to use DESC_HBASE_COLUMN_COMPARATOR
     * @return a SortedSet containing the HBaseColumn
     */
    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(final T value, final Comparator<HBaseColumn<?>> cmp) {
        final SortedSet<HBaseColumn<T>> set = new TreeSet<>(cmp == null ? DESC_HBASE_COLUMN_COMPARATOR : cmp);

        set.add(HBaseColumn.valueOf(value));

        return set;
    }

    /**
     * Creates a SortedSet containing a single HBaseColumn with the specified value and version,
     * sorted by version in descending order.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @param version the version timestamp
     * @return a SortedSet containing the HBaseColumn
     */
    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(final T value, final long version) {
        return asSortedSet(value, version, DESC_HBASE_COLUMN_COMPARATOR);
    }

    /**
     * Creates a SortedSet containing a single HBaseColumn with the specified value and version,
     * using the provided comparator for sorting.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @param version the version timestamp
     * @param cmp the comparator to use for sorting, or null to use DESC_HBASE_COLUMN_COMPARATOR
     * @return a SortedSet containing the HBaseColumn
     */
    public static <T> SortedSet<HBaseColumn<T>> asSortedSet(final T value, final long version, final Comparator<HBaseColumn<?>> cmp) {
        final SortedSet<HBaseColumn<T>> set = new TreeSet<>(cmp == null ? DESC_HBASE_COLUMN_COMPARATOR : cmp);

        set.add(HBaseColumn.valueOf(value, version));

        return set;
    }

    /**
     * Creates a Map with a single entry mapping the version to an HBaseColumn
     * containing the specified value with latest timestamp.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @return a Map with version as key and HBaseColumn as value
     */
    public static <T> Map<Long, HBaseColumn<T>> asMap(final T value) {
        final HBaseColumn<T> hbaseColumn = HBaseColumn.valueOf(value);

        return N.asMap(hbaseColumn.version(), hbaseColumn);
    }

    /**
     * Creates a Map with a single entry mapping the version to an HBaseColumn
     * containing the specified value and version.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @param version the version timestamp
     * @return a Map with version as key and HBaseColumn as value
     */
    public static <T> Map<Long, HBaseColumn<T>> asMap(final T value, final long version) {
        final HBaseColumn<T> hbaseColumn = HBaseColumn.valueOf(value, version);

        return N.asMap(hbaseColumn.version(), hbaseColumn);
    }

    /**
     * Creates a SortedMap with a single entry mapping the version to an HBaseColumn
     * containing the specified value, sorted by version in descending order.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @return a SortedMap with version as key and HBaseColumn as value
     */
    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(final T value) {
        return asSortedMap(value, DESC_HBASE_VERSION_COMPARATOR);
    }

    /**
     * Creates a SortedMap with a single entry mapping the version to an HBaseColumn
     * containing the specified value, using the provided comparator for sorting versions.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @param cmp the comparator for sorting versions, or null to use DESC_HBASE_VERSION_COMPARATOR
     * @return a SortedMap with version as key and HBaseColumn as value
     */
    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(final T value, final Comparator<Long> cmp) {
        final SortedMap<Long, HBaseColumn<T>> map = new TreeMap<>(cmp == null ? DESC_HBASE_VERSION_COMPARATOR : cmp);
        final HBaseColumn<T> hbaseColumn = HBaseColumn.valueOf(value);

        map.put(hbaseColumn.version(), hbaseColumn);

        return map;
    }

    /**
     * Creates a SortedMap with a single entry mapping the version to an HBaseColumn
     * containing the specified value and version, sorted by version in descending order.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @param version the version timestamp
     * @return a SortedMap with version as key and HBaseColumn as value
     */
    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(final T value, final long version) {
        return asSortedMap(value, version, DESC_HBASE_VERSION_COMPARATOR);
    }

    /**
     * Creates a SortedMap with a single entry mapping the version to an HBaseColumn
     * containing the specified value and version, using the provided comparator for sorting versions.
     *
     * @param <T> the type of the column value
     * @param value the column value
     * @param version the version timestamp
     * @param cmp the comparator for sorting versions, or null to use DESC_HBASE_VERSION_COMPARATOR
     * @return a SortedMap with version as key and HBaseColumn as value
     */
    public static <T> SortedMap<Long, HBaseColumn<T>> asSortedMap(final T value, final long version, final Comparator<Long> cmp) {
        final SortedMap<Long, HBaseColumn<T>> map = new TreeMap<>(cmp == null ? DESC_HBASE_VERSION_COMPARATOR : cmp);
        final HBaseColumn<T> hbaseColumn = HBaseColumn.valueOf(value, version);

        map.put(hbaseColumn.version(), hbaseColumn);

        return map;
    }

    /**
     * Returns the value of this HBase column.
     *
     * @return the column value
     */
    public T value() {
        return value;
    }

    /**
     * Returns the version (timestamp) of this HBase column.
     *
     * @return the version timestamp
     */
    public long version() {
        return version;
    }

    /**
     * Creates a copy of this HBaseColumn with the same value and version.
     *
     * @return a new HBaseColumn instance with the same value and version
     */
    public HBaseColumn<T> copy() {
        return new HBaseColumn<>(value, version);
    }

    /**
     * Checks if this HBaseColumn is null (empty).
     * An HBaseColumn is considered null if:
     * <ul>
     * <li>Both value is null and version is 0, or</li>
     * <li>It is one of the predefined empty column constants</li>
     * </ul>
     *
     * @return {@code true} if this column is null/empty, {@code false} otherwise
     */
    public boolean isNull() {
        return (value == null && version == 0) || emptyColumnPool.containsValue(this);
    }

    /**
     * Compares this HBaseColumn with another based on their version timestamps.
     * The comparison is done in ascending order of versions.
     *
     * @param o the HBaseColumn to compare with
     * @return a negative integer, zero, or a positive integer as this column's version
     *         is less than, equal to, or greater than the specified column's version
     */
    @Override
    public int compareTo(final HBaseColumn<T> o) {
        return Long.compare(version, o.version);
    }

    /**
     * Returns the hash code value for this HBaseColumn.
     * The hash code is computed based on both the version and value.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(version);
        return 31 * h + N.hashCode(value);
    }

    /**
     * Indicates whether some other object is "equal to" this HBaseColumn.
     * Two HBaseColumns are considered equal if they have the same version and value.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is equal to the obj argument; {@code false} otherwise
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

    /**
     * Returns a string representation of this HBaseColumn.
     * The format is "version:value".
     *
     * @return a string representation of this column
     */
    @Override
    public String toString() {
        return version + ":" + N.stringOf(value);
    }
}