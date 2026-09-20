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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.JsonSerConfig;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.XmlConstants;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntBiObjFunction;
import com.landawn.abacus.util.function.IntBiObjPredicate;
import com.landawn.abacus.util.function.IntObjFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * The default implementation of the {@link Dataset} interface, providing a comprehensive set of
 * operations for data manipulation, transformation, and analysis similar to a data frame structure
 * in other programming languages.
 *
 * <p>Although the class is named {@code RowDataset}, the underlying data is stored column-wise
 * (one {@code List<Object>} per column), which supports direct column access. Logically the dataset
 * is addressed by row index and column name/index, exposing a row-oriented API on top of that
 * storage. This implementation is cloneable and provides methods for filtering, sorting, grouping,
 * joining, and aggregating data.</p>
 *
 * <p>This mutable class is not thread-safe. Its public constructors shallowly copy column names
 * and column storage into independent growable lists. Cell objects remain shared with the inputs.
 * {@link #freeze()} prevents mutation through the Dataset API but does not deep-freeze cell values.
 * In addition, {@link #slice(int, int)} and pages returned by {@link #paginate(int)} are frozen,
 * backed views: non-structural source updates are visible through them, while structural source
 * changes can invalidate them.</p>
 *
 * <p>The cursor used by the one-coordinate accessors is positional. Row-removal operations clamp it
 * to the last remaining row, or reset it to {@code 0} when the dataset becomes empty; they do not
 * preserve the identity of the row that previously occupied the cursor position.</p>
 *
 * @see Dataset
 */
@SuppressWarnings({ "java:S1192", "java:S1698", "java:S1854", "java:S6539" })
public final class RowDataset implements Dataset, Cloneable {

    /**
     * The immutable empty map used as the properties of a dataset that carries no metadata. It is compared by
     * identity ({@code ==}) to distinguish "no properties" from a caller-supplied map.
     *
     * <p>This MUST be declared before {@link #EMPTY_DATASET}: static initializers run in textual order, and
     * {@code EMPTY_DATASET}'s constructor resolves its properties through {@code copyProperties(null)}, which
     * returns this field. Declared after, it would still be {@code null} at that moment, leaving the shared
     * empty dataset with {@code _properties == null} and silently flipping every {@code == EMPTY_PROPERTIES}
     * identity guard (see {@code mergeProperties} and {@code getProperties}).</p>
     */
    static final Map<String, Object> EMPTY_PROPERTIES = N.emptyMap();

    /**
     * The shared, frozen, zero-column/zero-row instance returned by {@link Dataset#empty()}.
     * It is immutable, so it can safely be handed out to any number of callers.
     */
    static final Dataset EMPTY_DATASET = new RowDataset(N.emptyList(), N.emptyList(), null, true);

    static {
        EMPTY_DATASET.freeze();
    }

    /** The character separating a prefix from a property name in a nested column name, as in {@code "account.id"}. */
    static final char PROP_NAME_SEPARATOR = '.';

    /** The text written for a {@code null} value. */
    static final String NULL_STRING = "null";

    /** {@link #NULL_STRING} as a {@code char[]}, so it can be written without re-allocating. */
    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    /** The text written for {@code Boolean.TRUE}. */
    static final String TRUE = Boolean.TRUE.toString().intern();

    /** {@link #TRUE} as a {@code char[]}, so it can be written without re-allocating. */
    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();

    /** The text written for {@code Boolean.FALSE}. */
    static final String FALSE = Boolean.FALSE.toString().intern();

    /** {@link #FALSE} as a {@code char[]}, so it can be written without re-allocating. */
    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    /** The numeric column types (primitive and wrapper) accepted as a count/aggregate column. */
    static final Set<Class<?>> SUPPORTED_COUNT_COLUMN_TYPES = N.toSet(int.class, Integer.class, long.class, Long.class, float.class, Float.class, double.class,
            Double.class);

    /** The properties key under which a resolved property-name list may be cached. */
    static final String CACHED_PROP_NAMES = "cachedPropNames";

    private static final String COUNT = "count";

    private static final String ROW = "row";

    private static final JsonParser jsonParser = ParserFactory.createJsonParser();

    private static final XmlParser xmlParser = ParserFactory.isXmlParserAvailable() ? ParserFactory.createXmlParser() : null;

    private static final KryoParser kryoParser = newCloneParser();

    /** Creates a parser used only for graph copying by Dataset and Sheet clones. */
    static KryoParser newCloneParser() {
        if (!ParserFactory.isKryoParserAvailable()) {
            return null;
        }
        return CloneSupport.newParser();
    }

    // Keep optional Kryo serializer types out of the always-loaded factory method's bytecode.
    private static final class CloneSupport {
        static KryoParser newParser() {
            final KryoParser parser = ParserFactory.createKryoParser();
            parser.register(RowDataset.class, new DatasetCopySerializer());
            return parser;
        }
    }

    /** Copies logical table state, including views nested anywhere in a cell or metadata graph. */
    private static final class DatasetCopySerializer extends Serializer<RowDataset> {
        @Override
        public RowDataset copy(final Kryo kryo, final RowDataset original) {
            final int rowCount = original.size(); // Also rejects an invalidated slice.
            final RowDataset result = new RowDataset();
            // Register identity before following any cell/property reference: a view can contain itself,
            // another mutually-referencing view, or a bean that leads back to the same Dataset.
            kryo.reference(result);
            result._columnNameList = new ArrayList<>(original._columnNameList);
            result._columnList = new ArrayList<>(original._columnList.size());
            kryo.getOriginalToCopyMap().put(original._columnNameList, result._columnNameList);
            kryo.getOriginalToCopyMap().put(original._columnList, result._columnList);
            for (final List<Object> column : original._columnList) {
                final List<Object> copy = new ArrayList<>(rowCount);
                N.fill(copy, 0, rowCount, null);
                result._columnList.add(copy);
                kryo.getOriginalToCopyMap().put(column, copy);
            }
            result._currentRowIndex = original._currentRowIndex;
            result._isFrozen = original._isFrozen;
            result.missingPropertyPolicy = original.missingPropertyPolicy;

            for (int c = 0; c < original._columnList.size(); c++) {
                final List<Object> from = original._columnList.get(c);
                final List<Object> to = result._columnList.get(c);
                for (int r = 0; r < rowCount; r++) {
                    to.set(r, kryo.copy(from.get(r)));
                }
            }
            // Copy the original map in the same graph exactly once. A constructor-level map copy here
            // would break sharing between metadata and cell payloads. Parent/version/cache state is omitted.
            result._properties = kryo.copy(original._properties);
            return result;
        }

        @Override
        public void write(final Kryo kryo, final Output output, final RowDataset value) {
            throw new UnsupportedOperationException("This internal serializer supports copying only");
        }

        @Override
        public RowDataset read(final Kryo kryo, final Input input, final Class<? extends RowDataset> type) {
            throw new UnsupportedOperationException("This internal serializer supports copying only");
        }
    }

    private static final JsonSerConfig jsc = JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

    private static final XmlSerConfig xsc = XmlSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);

    private static final Type<Object> strType = Type.of(String.class);

    private static char[] toQuotedJsonNameChars(final String name) throws UncheckedIOException {
        final BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();

        try {
            bw.write(SK._DOUBLE_QUOTE);
            bw.writeCharacter(name);
            bw.write(SK._DOUBLE_QUOTE);

            return bw.toString().toCharArray();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Validates an XML element name using the document's namespace rules.
     *
     * @param document the document used for name validation
     * @param name the proposed element name
     * @param parameterName the parameter identified in a validation failure
     * @throws IllegalArgumentException if {@code name} is not a valid unqualified XML element name
     */
    private static void checkXmlElementName(final Document document, final String name, final String parameterName) throws IllegalArgumentException {
        try {
            document.createElementNS(null, name);
        } catch (final DOMException e) {
            throw new IllegalArgumentException("Invalid XML element name for " + parameterName + ": " + name, e);
        }
    }

    /**
     * Validates a row or collection supplied by a callback.
     *
     * @param <T> the supplied value's type
     * @param result the supplied value
     * @param supplierName the callback identified in a validation failure
     * @return the supplied value
     * @throws IllegalArgumentException if {@code result} is {@code null}
     */
    private static <T> T checkSupplierResult(final T result, final String supplierName) throws IllegalArgumentException {
        if (result == null) {
            throw new IllegalArgumentException(supplierName + " returned null");
        }

        return result;
    }

    /**
     * Validates the capacity of a previously checked, non-null supplied array.
     *
     * @param array the supplied array
     * @param requiredLength the number of selected columns
     * @param supplierName the callback identified in a validation failure
     * @return the supplied array
     * @throws IllegalArgumentException if {@code array.length < requiredLength}
     */
    private static Object[] checkObjectArrayCapacity(final Object[] array, final int requiredLength, final String supplierName)
            throws IllegalArgumentException {
        if (array.length < requiredLength) {
            throw new IllegalArgumentException(
                    supplierName + " returned an array of length " + array.length + ", but at least " + requiredLength + " elements are required");
        }

        return array;
    }

    /**
     * @throws IllegalArgumentException if {@code rowType} is null or is not an object-array, collection, map, or bean type
     */
    private static void checkSupportedRowType(final Class<?> rowType, final String parameterName) throws IllegalArgumentException {
        N.checkArgNotNull(rowType, parameterName);

        final Type<?> type = Type.of(rowType);

        if (!(type.isObjectArray() || type.isCollection() || type.isMap() || type.isBean())) {
            throw new IllegalArgumentException("Unsupported row type: " + ClassUtil.getCanonicalClassName(rowType)
                    + ". Only Object array, Collection, Map and bean classes are supported");
        }
    }

    private static <T> IntFunction<? extends T> reuseFirstSuppliedRow(final T firstRow, final IntFunction<? extends T> rowSupplier) {
        return new IntFunction<>() {
            private T next = firstRow;

            @Override
            public T apply(final int columnCount) {
                if (next != null) {
                    final T result = next;
                    next = null;
                    return result;
                }

                return checkSupplierResult(rowSupplier.apply(columnCount), "rowSupplier");
            }
        };
    }

    private List<String> _columnNameList; //NOSONAR

    private List<List<Object>> _columnList; //NOSONAR

    private Map<String, Integer> _columnIndexMap; //NOSONAR

    private int[] _columnIndexes; //NOSONAR

    /** An unmodifiable column-name view recognizable by the index-resolution fast path. */
    private static final class ColumnNameListView extends AbstractList<String> implements RandomAccess {
        private final List<String> target;

        ColumnNameListView(final List<String> target) {
            this.target = target;
        }

        @Override
        public String get(final int index) {
            return target.get(index);
        }

        @Override
        public int size() {
            return target.size();
        }
    }

    private int _currentRowIndex = 0; //NOSONAR

    private boolean _isFrozen = false; //NOSONAR

    private Map<String, Object> _properties; //NOSONAR

    private MissingPropertyPolicy missingPropertyPolicy = MissingPropertyPolicy.IGNORE;

    private transient int modCount = 0; //NOSONAR

    private int rowVersion;
    private RowDataset sliceParent;
    private int sliceParentVersion;
    private int sliceParentSize;

    /**
     * Verifies that the parent of this slice still has the captured row layout.
     *
     * @throws ConcurrentModificationException if the parent dataset's row version or size has changed
     */
    private void checkSliceValidity() throws ConcurrentModificationException {
        if (sliceParent != null && (sliceParent.rowVersion != sliceParentVersion || sliceParent.size() != sliceParentSize)) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Verifies the slice and the structural version captured by an iterator.
     *
     * @param expectedModCount the captured structural version
     * @throws ConcurrentModificationException if the slice's parent has changed, or {@code modCount != expectedModCount}
     */
    private void checkModification(final int expectedModCount) throws ConcurrentModificationException {
        // A parent can invalidate a slice without changing the slice's own modCount. Check even
        // iterator control paths that can return a cached count without reading a backing cell.
        checkSliceValidity();
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    private void rowsChanged() {
        modCount++;
        rowVersion++;
    }

    private final class SliceColumn extends AbstractList<Object> {
        private final List<Object> column;
        private final int offset;
        private final int length;
        private final int expectedRowVersion = rowVersion;

        SliceColumn(final List<Object> column, final int offset, final int length) {
            this.column = column;
            this.offset = offset;
            this.length = length;
        }

        /**
         * Creates another column view starting {@code from} elements into this view.
         *
         * @param from the offset from this view's start
         * @param length the number of elements in the new view
         * @return the new column view
         * @throws ConcurrentModificationException if the dataset backing this column view has changed its row layout
         */
        SliceColumn slice(final int from, final int length) throws ConcurrentModificationException {
            checkVersion();
            return new SliceColumn(column, offset + from, length);
        }

        /**
         * Verifies that this column view still has its captured row layout.
         *
         * @throws ConcurrentModificationException if the dataset's row version has changed
         */
        private void checkVersion() throws ConcurrentModificationException {
            if (rowVersion != expectedRowVersion) {
                throw new ConcurrentModificationException();
            }
        }

        /**
         * {@inheritDoc}
         * @throws ConcurrentModificationException if the dataset backing this column view has changed its row layout
         * @throws IndexOutOfBoundsException if {@code index} is negative or is not less than this column view's length
         */
        @Override
        public Object get(final int index) throws ConcurrentModificationException, IndexOutOfBoundsException {
            checkVersion();
            N.checkElementIndex(index, length);
            return column.get(offset + index);
        }

        /**
         * {@inheritDoc}
         * @throws ConcurrentModificationException if the dataset backing this column view has changed its row layout
         */
        @Override
        public int size() throws ConcurrentModificationException {
            checkVersion();
            return length;
        }
    }

    /**
     * No-argument constructor reserved for Kryo serialization/deserialization.
     * Do not call directly.
     */
    // For Kryo
    protected RowDataset() {
        _properties = EMPTY_PROPERTIES;
    }

    /**
     * Constructs a new {@code RowDataset} with the specified column names and column data.
     *
     * <p>The column names, outer column list, and each column are shallowly copied into independent,
     * growable storage. Immutable and fixed-size inputs are supported. Repeated columns, including distinct
     * views of the same backing list, become independent columns. Later changes to the supplied lists do
     * not affect this dataset, and dataset mutations do not change those lists. Cell objects themselves
     * remain shared. Construction takes O(column count + total cell count) time and space.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columnNames = new ArrayList<>(Arrays.asList("id", "name", "age"));
     * List<List<Object>> columns = new ArrayList<>(Arrays.asList(
     *     new ArrayList<>(Arrays.asList(1, 2)),
     *     new ArrayList<>(Arrays.asList("Alice", "Bob")),
     *     new ArrayList<>(Arrays.asList(25, 30))
     * ));
     * RowDataset dataset = new RowDataset(columnNames, columns);
     * System.out.println(dataset.size());            // 2
     * System.out.println(dataset.columnCount());     // 3
     * System.out.println(dataset.<String>get(0, 1)); // "Alice"
     * }</pre>
     *
     * @param columnNameList the ordered list of column names. Must not be {@code null}, must not contain
     *                       {@code null} or empty names, and must not contain duplicates. Size must equal {@code columnList.size()}.
     * @param columnList the ordered list of columns, where each element is a {@code List<Object>}
     *                   holding the values for that column. Must not be {@code null}; each element must not
     *                   be {@code null}. All columns must have the same size.
     * @throws IllegalArgumentException if {@code columnNameList} or {@code columnList} is {@code null}; if any
     *         column name is {@code null} or empty; if column names contain duplicates; if the sizes of
     *         {@code columnNameList} and {@code columnList} differ; if any column (element of {@code columnList}) is
     *         {@code null}; or if the columns do not all have the same size.
     * @see #RowDataset(List, List, Map)
     * @see Dataset#columns(Collection, Collection)
     * @see Dataset#rows(Collection, Collection)
     * @see N#newDataset(Collection, Collection)
     */
    @Internal
    public RowDataset(final List<String> columnNameList, final List<List<Object>> columnList) throws IllegalArgumentException {
        this(columnNameList, columnList, null);
    }

    /**
     * Constructs a new {@code RowDataset} with the specified column names, column data, and
     * optional metadata properties.
     *
     * <p>Table storage is shallowly copied as described by {@link #RowDataset(List, List)}. The
     * {@code properties} map is also shallowly copied into a new map, preserving its type and ordering
     * where possible. Cell objects and property values remain shared with the inputs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columnNames = new ArrayList<>(Arrays.asList("id", "name", "score"));
     * List<List<Object>> columns = new ArrayList<>(Arrays.asList(
     *     new ArrayList<>(Arrays.asList(1, 2, 3)),
     *     new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")),
     *     new ArrayList<>(Arrays.asList(95, 87, 92))
     * ));
     * Map<String, Object> props = new HashMap<>();
     * props.put("source", "test_data");
     * RowDataset dataset = new RowDataset(columnNames, columns, props);
     * System.out.println(dataset.getProperties().get("source")); // "test_data"
     * }</pre>
     *
     * @param columnNameList the ordered list of column names. Must not be {@code null}, must not contain
     *                       {@code null} or empty names, and must not contain duplicates. Size must equal {@code columnList.size()}.
     * @param columnList the ordered list of columns, where each element is a {@code List<Object>}
     *                   holding the values for that column. Must not be {@code null}; each element must not
     *                   be {@code null}. All columns must have the same size.
     * @param properties optional metadata map. May be {@code null}. A copy is made, so
     *                   later changes to the provided map are not reflected in this dataset.
     * @throws IllegalArgumentException if {@code columnNameList} or {@code columnList} is {@code null}; if any
     *         column name is {@code null} or empty; if column names contain duplicates; if the sizes of
     *         {@code columnNameList} and {@code columnList} differ; if any column (element of {@code columnList}) is
     *         {@code null}; or if the columns do not all have the same size.
     * @see #RowDataset(List, List)
     * @see Dataset#columns(Collection, Collection)
     * @see Dataset#rows(Collection, Collection)
     * @see N#newDataset(Collection, Collection)
     */
    @Internal
    public RowDataset(final List<String> columnNameList, final List<List<Object>> columnList, final Map<String, Object> properties)
            throws IllegalArgumentException {
        this(columnNameList, columnList, properties, false);
    }

    /**
     * Internal construction for already-owned storage or deliberately shared, frozen views. When
     * {@code trustedStorage} is true, callers must maintain the adopted lists' shape and ownership;
     * properties are still copied. Public construction always establishes independent table storage.
     *
     * @throws IllegalArgumentException if either list is null, column names are null, empty, or duplicated,
     *         the lists differ in size, a column is null, or the columns have different row counts
     */
    RowDataset(final List<String> columnNameList, final List<List<Object>> columnList, final Map<String, Object> properties, final boolean trustedStorage)
            throws IllegalArgumentException {
        N.checkArgNotNull(columnNameList, cs.columnNameList);

        N.checkArgument(!N.anyEmpty(columnNameList), "Empty column name found in: {}", columnNameList);
        N.checkArgument(!N.containsDuplicates(columnNameList), "Duplicated column names found in: {}", columnNameList);
        N.checkArgNotNull(columnList, cs.columnList);
        N.checkArgument(columnNameList.size() == columnList.size(), "The size of column name list: {} is different from the size of column list: {}",
                columnNameList.size(), columnList.size());

        if (columnList.size() > 0) {
            N.checkArgNotNull(columnList.get(0), "Column in columnList cannot be null");
        }

        final int size = columnList.size() == 0 ? 0 : columnList.get(0).size();

        for (final List<Object> column : columnList) {
            N.checkArgNotNull(column, "Column in columnList cannot be null");
            N.checkArgument(column.size() == size, "All columns in the specified 'columnList' must have same size.");
        }

        _columnNameList = trustedStorage ? columnNameList : new ArrayList<>(columnNameList);

        if (trustedStorage) {
            _columnList = columnList;
        } else {
            _columnList = new ArrayList<>(columnList.size());
            // Copy every occurrence, not just distinct list identities: different views can share backing
            // storage too, and column-wise row mutations require independent logical columns.
            for (final List<Object> column : columnList) {
                _columnList.add(new ArrayList<>(column));
            }
        }

        _properties = copyProperties(properties);
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     */
    @Override
    public ImmutableList<String> columnNames() throws ConcurrentModificationException {
        checkSliceValidity();
        // Not ImmutableList.wrap(_columnNameList): see ColumnNameListView. The view is already unmodifiable, so
        // ImmutableList must not wrap it again (isUnmodifiable = true), or the target would be hidden after all.
        return ImmutableList.create(new ColumnNameListView(_columnNameList), true, false);
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     */
    @Override
    public int columnCount() throws ConcurrentModificationException {
        checkSliceValidity();
        return _columnNameList.size();
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public String getColumnName(final int columnIndex) throws ConcurrentModificationException, IndexOutOfBoundsException {
        checkSliceValidity();
        return _columnNameList.get(columnIndex);
    }

    /**
     * Returns the lazily built name -&gt; index map for this dataset's columns, creating it on first use.
     *
     * <p>Every operation that changes the column set or its order keeps this map consistent with
     * {@code _columnNameList} - most discard it ({@code _columnIndexMap = null}) and let it be rebuilt here,
     * while {@code renameColumn}, {@code swapColumns} and an appending {@code addColumn} patch it in place -
     * so it is always up to date when this method returns.</p>
     *
     * @return the column-name index map
     */
    private Map<String, Integer> columnIndexMap() {
        if (_columnIndexMap == null) {
            _columnIndexMap = new HashMap<>();

            int i = 0;
            for (final String e : _columnNameList) {
                _columnIndexMap.put(e, i++);
            }
        }

        return _columnIndexMap;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public int getColumnIndex(final String columnName) throws IllegalArgumentException {
        final Integer columnIndex = columnIndexMap().get(columnName);

        //    if (columnIndex == null /* && NameUtil.isCanonicalName(_beanName, columnName)*/) {
        //        columnIndex = _columnIndexMap.get(NameUtil.getSimpleName(columnName));
        //    }

        if (columnIndex == null) {
            throw new IllegalArgumentException("The specified column: " + columnName + " is not included in this Dataset: " + _columnNameList);
        }

        return columnIndex;
    }

    /**
     * Returns the index of {@code columnName}, failing fast if this dataset has no such column.
     * A synonym of {@link #getColumnIndex(String)} used at the head of the methods that take a single
     * column name, so that the name is validated before any other work is done.
     *
     * @param columnName the column name to resolve
     * @return the zero-based index of the column
     * @throws IllegalArgumentException if {@code columnName} is not a column of this dataset.
     */
    int checkColumnName(final String columnName) throws IllegalArgumentException {
        return getColumnIndex(columnName);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
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

        final Map<String, Integer> indexByName = columnIndexMap();
        final int[] columnIndexes = new int[columnNames.size()];
        int i = 0;
        Integer columnIndex = null;

        for (final String columnName : columnNames) {
            columnIndex = indexByName.get(columnName);

            //    if (columnIndex == null /* && NameUtil.isCanonicalName(_beanName, columnName)*/) {
            //        columnIndex = _columnIndexMap.get(NameUtil.getSimpleName(columnName));
            //    }

            if (columnIndex == null) {
                throw new IllegalArgumentException("The specified column: " + columnName + " is not included in this Dataset: " + _columnNameList);
            }

            columnIndexes[i++] = columnIndex;
        }

        return columnIndexes;
    }

    /**
     * Resolves a required column selection to column indexes, failing fast on an unusable selection.
     * Unlike {@link #getColumnIndexes(Collection)}, which maps a {@code null}/empty selection to an empty
     * result, this method rejects a {@code null} selection outright and rejects an empty one unless this
     * dataset itself has no columns (where the empty list <i>is</i> the full column set).
     *
     * <p>When {@code columnNames} is this dataset's own column-name list the returned array is a cached
     * internal array rather than a fresh copy; callers must not modify it.</p>
     *
     * @param columnNames the column selection to resolve
     * @return the zero-based indexes of the selected columns, in selection order
     * @throws IllegalArgumentException if {@code columnNames} is {@code null}, if it is empty while this dataset has
     *         at least one column, or if it names a column this dataset does not have.
     */
    int[] checkColumnNames(final Collection<String> columnNames) throws IllegalArgumentException {
        // A null selection is always invalid for the strict column-selection methods. Previously the guard only
        // fired when _columnNameList was non-empty, so on a zero-column Dataset a null selection slipped through
        // and NPE'd in getColumnIndexes (e.g. copy(null)); it now fails with the documented IllegalArgumentException.
        // An EMPTY (non-null) selection is still allowed on a zero-column Dataset: there the empty list is the full
        // (empty) column set, so no-arg operations that delegate via columnNames() (e.g. toList()) keep working.
        if (columnNames == null || (columnNames.isEmpty() && N.notEmpty(_columnNameList))) {
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

        final int[] columnIndexes = getColumnIndexes(columnNames);
        checkNoDuplicateSelection(columnNames, columnIndexes);

        return columnIndexes;
    }

    /**
     * Rejects a selection that names the same column twice. Without this the same repeated name was silently
     * accepted by projections, applied twice by {@code updateColumns}, emitted twice by {@code toJson}/{@code toXml},
     * or reported late by the {@code RowDataset} constructor with an internal result-column name
     * ({@code "Duplicated column names found in: [a, a, cnt]"}) after the whole operation had already run.
     * Resolved indexes are compared instead of the names, so this is one pass over a {@code boolean[]} of the
     * column count and needs no hashing; {@code removeColumns} deliberately bypasses it (see there).
     *
     * @param columnNames the selection, for the error message
     * @param columnIndexes the indexes {@code columnNames} resolved to, in selection order
     * @throws IllegalArgumentException if two entries of {@code columnIndexes} are equal
     */
    private void checkNoDuplicateSelection(final Collection<String> columnNames, final int[] columnIndexes) throws IllegalArgumentException {
        if (columnIndexes.length < 2) {
            return;
        }

        final boolean[] seen = new boolean[_columnNameList.size()];

        for (final int columnIndex : columnIndexes) {
            if (seen[columnIndex]) {
                throw new IllegalArgumentException(
                        "Duplicated column names in the selection: " + columnNames + " (" + _columnNameList.get(columnIndex) + " is listed more than once)");
            }

            seen[columnIndex] = true;
        }
    }

    /**
     * Returns {@code true} if {@code columnNames} <i>is</i> this dataset's own column-name list - either that
     * list itself, or any {@link ImmutableList} view of it returned by {@link #columnNames()}. The check is by
     * reference identity of the underlying list, not by content: an equal-but-distinct list returns {@code false}.
     * It selects the "all columns, in declaration order" fast paths (the cached column-index array).
     * Bean conversion policy is configured independently of column-list identity.
     *
     * @param columnNames the column selection to test; may be {@code null}
     * @return {@code true} if {@code columnNames} is this dataset's own column-name list or a view of it
     */
    boolean isColumnNameList(final Collection<String> columnNames) {
        return columnNames == _columnNameList || (columnNames instanceof ImmutableList<String> immutableList // NOSONAR
                && immutableList.list instanceof ColumnNameListView view && view.target == _columnNameList);
    }

    @Override
    public boolean containsColumn(final String columnName) {
        return columnIndexMap().containsKey(columnName); // || columnIndexMap().containsKey(NameUtil.getSimpleName(columnName));
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public boolean containsAllColumns(final Collection<String> columnNames) throws IllegalArgumentException {
        N.checkArgNotNull(columnNames, cs.columnNames);

        for (final String columnName : columnNames) {
            if (!containsColumn(columnName)) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void renameColumn(final String columnName, final String newColumnName) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int idx = checkColumnName(columnName);

        if (columnName.equals(newColumnName)) {
            // ignore - no modification needed
            return;
        }

        if (Strings.isEmpty(newColumnName)) {
            throw new IllegalArgumentException("The new column name cannot be null or empty");
        }

        if (_columnNameList.contains(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included in this Dataset: " + _columnNameList);
        }

        if (_columnIndexMap != null) {
            _columnIndexMap.put(newColumnName, _columnIndexMap.remove(_columnNameList.get(idx)));
        }

        _columnNameList.set(idx, newColumnName);

        modCount++;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void renameColumns(final Map<String, String> oldNewNames) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(oldNewNames, cs.oldNewNames);

        if (N.containsDuplicates(oldNewNames.values())) {
            throw new IllegalArgumentException("Duplicated new column names: " + oldNewNames.values());
        }

        // Build the whole resulting name list first and validate THAT, instead of testing each new name
        // against the current one. Testing against the current list rejected every renaming that reuses a
        // name it is itself moving away - a swap {a->b, b->a}, a rotation, or a shift - even though the
        // result would have been perfectly well formed. Validating the outcome accepts those and still
        // rejects a genuine collision with a column that is not being renamed.
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);

        for (final Map.Entry<String, String> entry : oldNewNames.entrySet()) {
            final int idx = checkColumnName(entry.getKey());

            if (Strings.isEmpty(entry.getValue())) {
                throw new IllegalArgumentException("The new column name for '" + entry.getKey() + "' cannot be null or empty");
            }

            newColumnNameList.set(idx, entry.getValue());
        }

        if (N.containsDuplicates(newColumnNameList)) {
            throw new IllegalArgumentException("Duplicated column names in the renamed Dataset: " + newColumnNameList);
        }

        if (newColumnNameList.equals(_columnNameList)) {
            // Every entry mapped a name to itself; renaming nothing is not a structural modification.
            return;
        }

        for (int i = 0, len = newColumnNameList.size(); i < len; i++) {
            _columnNameList.set(i, newColumnNameList.get(i));
        }

        // The name -> index map cannot be patched entry by entry through a permutation, and _columnIndexes
        // is positional so it stays valid.
        _columnIndexMap = null;

        modCount++;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void renameColumns(final Collection<String> columnNames, final Function<? super String, String> func)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int[] columnIndexes = checkColumnNames(columnNames);
        N.checkArgNotNull(func, cs.func);

        // checkColumnNames, not a silent return on an empty selection: this is one of the strict
        // column-selection methods, so null/empty must be rejected exactly as copy(), toList() and the
        // set operations reject it. (checkColumnNames still allows an empty selection on a Dataset that
        // genuinely has no columns, which is what keeps renameColumns(Function) working there.)

        if (columnIndexes.length == 0) {
            return;
        }

        final Map<String, String> map = N.newHashMap(columnNames.size());

        for (final String columnName : columnNames) {
            map.put(columnName, func.apply(columnName));
        }

        renameColumns(map);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void renameColumns(final Function<? super String, String> func) throws IllegalStateException, IllegalArgumentException {
        renameColumns(_columnNameList, func);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void moveColumn(final String columnName, final int newPosition) throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        checkFrozen();

        final int currentPosition = checkColumnName(columnName);

        if (newPosition < 0 || newPosition >= columnCount()) {
            throw new IndexOutOfBoundsException("New position must be >= 0 and < " + columnCount());
        }

        if (currentPosition == newPosition) {
            // ignore - no modification needed
            return;
        }

        _columnNameList.add(newPosition, _columnNameList.remove(currentPosition));
        _columnList.add(newPosition, _columnList.remove(currentPosition));

        _columnIndexMap = null;
        _columnIndexes = null;

        modCount++;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IndexOutOfBoundsException if a nonempty selection has {@code newPosition < 0}
     *         or {@code newPosition > columnCount() - columnNames.size()}
     */
    @Override
    public void moveColumns(final List<String> columnNames, final int newPosition)
            throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        checkFrozen();

        N.checkArgNotNull(columnNames, cs.columnNames);

        // Nothing to move -> harmless no-op regardless of newPosition (the bound check below uses
        // columnCount - columnSizeToMove, which would otherwise reject an out-of-range position even
        // though there is nothing to relocate).
        if (columnNames.isEmpty()) {
            return;
        }

        // Resolve the selection first: checkColumnNames rejects an unknown or repeated name, and that must win over
        // a bound complaint computed from the (inflated) size of a selection that repeats a name.
        final int[] currentPositions = checkColumnNames(columnNames);
        final int columnCount = columnCount();
        final int columnSizeToMove = currentPositions.length;

        if (newPosition < 0 || newPosition > columnCount - columnSizeToMove) {
            throw new IndexOutOfBoundsException("The new column position must be >= 0 and <= " + (columnCount - columnSizeToMove));
        }

        if (columnSizeToMove == 1) {
            moveColumn(N.firstOrNullIfEmpty(columnNames), newPosition);
            return;
        }

        final Set<Integer> positionSet = N.toSet(currentPositions);

        if (N.isSorted(currentPositions) && currentPositions[columnSizeToMove - 1] - currentPositions[0] + 1 == columnSizeToMove) {
            if (currentPositions[0] == newPosition) {
                return;
            }

            final List<String> subColumnNameList = _columnNameList.subList(currentPositions[0], currentPositions[0] + columnSizeToMove);
            final List<String> tmpColumnNameList = new ArrayList<>(subColumnNameList);
            final List<List<Object>> subColumnList = _columnList.subList(currentPositions[0], currentPositions[0] + columnSizeToMove);
            final List<List<Object>> tmpColumnList = new ArrayList<>(subColumnList);

            subColumnNameList.clear();
            subColumnList.clear();

            _columnNameList.addAll(newPosition, tmpColumnNameList);
            _columnList.addAll(newPosition, tmpColumnList);
        } else {
            final List<String> firstHalfColumnNames = new ArrayList<>(columnCount - columnSizeToMove);
            final List<String> secondHalfColumnNames = new ArrayList<>(columnSizeToMove);
            final List<List<Object>> firstHalfColumns = new ArrayList<>(columnCount - columnSizeToMove);
            final List<List<Object>> secondHalfColumns = new ArrayList<>(columnSizeToMove);

            for (int i = 0; i < columnCount; i++) {
                if (!positionSet.contains(i)) {
                    firstHalfColumnNames.add(_columnNameList.get(i));
                    firstHalfColumns.add(_columnList.get(i));
                }
            }

            for (int columnIndex : currentPositions) {
                secondHalfColumnNames.add(_columnNameList.get(columnIndex));
                secondHalfColumns.add(_columnList.get(columnIndex));
            }

            _columnNameList.clear();
            _columnList.clear();

            _columnNameList.addAll(firstHalfColumnNames);
            _columnNameList.addAll(newPosition, secondHalfColumnNames);

            _columnList.addAll(firstHalfColumns);
            _columnList.addAll(newPosition, secondHalfColumns);
        }

        _columnIndexMap = null;
        _columnIndexes = null;

        modCount++;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void swapColumns(final String columnNameA, final String columnNameB) throws IllegalStateException, IllegalArgumentException {
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

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void moveRow(final int rowIndex, final int newPosition) throws IllegalStateException, IndexOutOfBoundsException {
        checkFrozen();

        final int size = size();

        if (rowIndex < 0 || rowIndex >= size) {
            throw new IndexOutOfBoundsException("Row index must be >= 0 and < " + size);
        }

        if (newPosition < 0 || newPosition >= size) {
            throw new IndexOutOfBoundsException("New position must be >= 0 and < " + size);
        }

        if (rowIndex == newPosition) {
            return;
        }

        for (final List<Object> column : _columnList) {
            column.add(newPosition, column.remove(rowIndex));
        }

        rowsChanged();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void moveRows(int fromRowIndex, int toRowIndex, int newPosition) throws IllegalStateException, IndexOutOfBoundsException {
        checkFrozen();

        checkRowIndex(fromRowIndex, toRowIndex);

        final int size = size();
        final int rowCountToMove = toRowIndex - fromRowIndex;

        if (newPosition < 0 || newPosition > size - rowCountToMove) {
            throw new IndexOutOfBoundsException("The new row position must be >= 0 and <= " + (size - (toRowIndex - fromRowIndex)));
        }

        // An empty range moves no row - see removeRows(int, int). Placed after the bounds check above so
        // that an out-of-range newPosition is still rejected: moveRows(5, 5, -1) must not silently succeed.
        if (fromRowIndex == toRowIndex || fromRowIndex == newPosition) {
            return;
        }

        for (final List<Object> column : _columnList) {
            final List<Object> subList = column.subList(fromRowIndex, toRowIndex);
            final List<Object> tmpList = new ArrayList<>(subList);

            subList.clear();

            column.addAll(newPosition, tmpList);
        }

        rowsChanged();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void swapRows(final int rowIndexA, final int rowIndexB) throws IllegalStateException, IndexOutOfBoundsException {
        checkFrozen();

        checkRowIndex(rowIndexA);
        checkRowIndex(rowIndexB);

        if (rowIndexA == rowIndexB) {
            return;
        }

        Object tmp = null;

        for (final List<Object> column : _columnList) {
            tmp = column.get(rowIndexA);
            column.set(rowIndexA, column.get(rowIndexB));
            column.set(rowIndexB, tmp);
        }

        rowsChanged();
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     */
    @Override
    public <T> T get(final int rowIndex, final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException {
        checkColumnIndex(columnIndex);
        checkRowIndex(rowIndex);

        return (T) _columnList.get(columnIndex).get(rowIndex);
    }

    /**
     * Reads an already-validated position without bounds checks.
     *
     * <p>Callers must pass coordinates known to be in range - a loop index over this dataset's own dimensions,
     * or an index returned by {@link #checkColumnName(String)}. The public {@link #get(int, int)} runs two
     * {@code size()} calls per access, which is wasted work in the join and cartesian-product inner loops
     * where the coordinates were established by the loop bounds themselves.</p>
     *
     * @param <T> the expected value type
     * @param rowIndex a valid zero-based row index
     * @param columnIndex a valid zero-based column index
     * @return the value at that position
     */
    private <T> T getValue(final int rowIndex, final int columnIndex) {
        return (T) _columnList.get(columnIndex).get(rowIndex);
    }

    //    @Override
    //    public <T> T get(final Class<? extends T> targetType, final int rowIndex, final int columnIndex) {
    //        T rt = (T) _columnList.get(columnIndex).get(rowIndex);
    //
    //        return (rt == null) ? N.defaultValueOf(targetType) : rt;
    //    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void set(final int rowIndex, final int columnIndex, final Object value) throws IllegalStateException, IndexOutOfBoundsException {
        checkFrozen();
        checkColumnIndex(columnIndex);
        checkRowIndex(rowIndex);

        _columnList.get(columnIndex).set(rowIndex, value);

        // Deliberately no modCount bump: writing a value changes no row, column or ordering, so every
        // live iterator and stream stays valid. modCount tracks structural modification only.
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     */
    @Override
    public boolean isNull(final int rowIndex, final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException {
        return get(rowIndex, columnIndex) == null;
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     */
    @Override
    public <T> T get(final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException {
        checkColumnIndex(columnIndex);
        checkCurrentRow();

        return (T) _columnList.get(columnIndex).get(_currentRowIndex);
    }

    //    @Override
    //    public <T> T get(final Class<? extends T> targetType, final int columnIndex) {
    //        T rt = get(columnIndex);
    //
    //        return (rt == null) ? N.defaultValueOf(targetType) : rt;
    //    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public <T> T get(final String columnName) throws IllegalArgumentException, ConcurrentModificationException, IndexOutOfBoundsException {
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

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public boolean getBoolean(final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException, ClassCastException {
        final Boolean rt = get(columnIndex);

        return rt != null && rt;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public boolean getBoolean(final String columnName)
            throws IllegalArgumentException, ConcurrentModificationException, IndexOutOfBoundsException, ClassCastException {
        return getBoolean(checkColumnName(columnName));
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public char getChar(final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException, ClassCastException {
        final Character rt = get(columnIndex);

        return (rt == null) ? 0 : rt;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public char getChar(final String columnName)
            throws IllegalArgumentException, ConcurrentModificationException, IndexOutOfBoundsException, ClassCastException {
        return getChar(checkColumnName(columnName));
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public byte getByte(final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException, ClassCastException {
        final Number rt = get(columnIndex);

        return (rt == null) ? 0 : rt.byteValue();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public byte getByte(final String columnName)
            throws IllegalArgumentException, ConcurrentModificationException, IndexOutOfBoundsException, ClassCastException {
        return getByte(checkColumnName(columnName));
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public short getShort(final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException, ClassCastException {
        final Number rt = get(columnIndex);

        return (rt == null) ? 0 : rt.shortValue();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public short getShort(final String columnName)
            throws IllegalArgumentException, ConcurrentModificationException, IndexOutOfBoundsException, ClassCastException {
        return getShort(checkColumnName(columnName));
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public int getInt(final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException, ClassCastException {
        final Number rt = get(columnIndex);

        return (rt == null) ? 0 : rt.intValue();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public int getInt(final String columnName) throws IllegalArgumentException, ConcurrentModificationException, IndexOutOfBoundsException, ClassCastException {
        return getInt(checkColumnName(columnName));
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public long getLong(final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException, ClassCastException {
        final Number rt = get(columnIndex);

        return (rt == null) ? 0L : rt.longValue();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public long getLong(final String columnName)
            throws IllegalArgumentException, ConcurrentModificationException, IndexOutOfBoundsException, ClassCastException {
        return getLong(checkColumnName(columnName));
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public float getFloat(final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException, ClassCastException {
        final Number rt = get(columnIndex);

        // Plain Number.floatValue()/doubleValue(): that is the contract the Dataset javadoc states and what the
        // Dataset.Row defaults do. Numbers.toDouble re-parses a Float through its decimal string (0.1f -> 0.1
        // instead of 0.10000000149011612), which made getDouble disagree with row(i).getDouble on the same cell.
        return (rt == null) ? 0f : rt.floatValue();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public float getFloat(final String columnName)
            throws IllegalArgumentException, ConcurrentModificationException, IndexOutOfBoundsException, ClassCastException {
        return getFloat(checkColumnName(columnName));
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public double getDouble(final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException, ClassCastException {
        final Number rt = get(columnIndex);

        return (rt == null) ? 0d : rt.doubleValue(); // see getFloat(int)
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     */
    @Override
    public double getDouble(final String columnName)
            throws IllegalArgumentException, ConcurrentModificationException, IndexOutOfBoundsException, ClassCastException {
        return getDouble(checkColumnName(columnName));
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     */
    @Override
    public boolean isNull(final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException {
        return get(columnIndex) == null;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public boolean isNull(final String columnName) throws IllegalArgumentException, ConcurrentModificationException, IndexOutOfBoundsException {
        return get(columnName) == null;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void set(final int columnIndex, final Object value) throws IllegalStateException, IndexOutOfBoundsException {
        checkFrozen();
        checkColumnIndex(columnIndex);
        checkCurrentRow();

        _columnList.get(columnIndex).set(_currentRowIndex, value);

        // No modCount bump - see set(int, int, Object).
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     */
    @Override
    public void set(final String columnName, final Object value) throws IllegalArgumentException, IllegalStateException {
        set(checkColumnName(columnName), value);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public <T> ImmutableList<T> getColumn(final int columnIndex) throws IndexOutOfBoundsException {
        checkColumnIndex(columnIndex);

        return ImmutableList.wrap((List) _columnList.get(columnIndex));
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> ImmutableList<T> getColumn(final String columnName) throws IllegalArgumentException {
        return getColumn(checkColumnName(columnName));
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public <T> List<T> copyColumn(final String columnName) throws IllegalArgumentException {
        return new ArrayList<>((List) _columnList.get(checkColumnName(columnName)));
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumn(final String newColumnName, final Collection<?> column) throws IllegalStateException, IllegalArgumentException {
        addColumn(_columnList.size(), newColumnName, column);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumn(final int newColumnPosition, final String newColumnName, final Collection<?> column)
            throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();

        final int columnCount = columnCount();
        N.checkPositionIndex(newColumnPosition, columnCount);

        if (Strings.isEmpty(newColumnName)) {
            throw new IllegalArgumentException("The new column name can not be null or empty");
        }

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included in this Dataset: " + _columnNameList);
        }

        // A Dataset derives its row count from its first column, so a zero-column Dataset always reports
        // size() == 0 and the size check below could never pass for a non-empty column. That made it
        // impossible to populate a Dataset column by column starting from N.newEmptyDataset(). The first
        // column of a column-less Dataset therefore *establishes* the row count, exactly as
        // Dataset.columns(Collection, Object[][]) does.
        if (columnCount() > 0 && N.notEmpty(column) && column.size() != size()) {
            throw new IllegalArgumentException("The specified column size[" + column.size() + "] must be the same as this Dataset size[" + size() + "]. ");
        }

        // Snapshot before changing names: the input may be a live row/name view of this Dataset,
        // or its iterator may fail. Neither case may leave a name without matching column storage.
        final List<Object> newColumn = N.isEmpty(column) ? N.repeat(null, size()) : new ArrayList<>(column);
        _columnNameList.add(newColumnPosition, newColumnName);
        _columnList.add(newColumnPosition, newColumn);

        updateColumnIndex(newColumnPosition, newColumnName);

        modCount++;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumn(final String newColumnName, final String fromColumnName, final Function<?, ?> func)
            throws IllegalStateException, IllegalArgumentException {
        addColumn(_columnList.size(), newColumnName, fromColumnName, func);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumn(final int newColumnPosition, final String newColumnName, final String fromColumnName, final Function<?, ?> func)
            throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();

        final int columnCount = columnCount();
        N.checkPositionIndex(newColumnPosition, columnCount);

        if (Strings.isEmpty(newColumnName)) {
            throw new IllegalArgumentException("The new column name can not be null or empty");
        }

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included in this Dataset: " + _columnNameList);
        }

        final List<Object> column = _columnList.get(checkColumnName(fromColumnName));
        N.checkArgNotNull(func, cs.func);
        final List<Object> newColumn = new ArrayList<>(size());
        final Function<Object, Object> mapperToUse = (Function<Object, Object>) func;

        for (final Object val : column) {
            newColumn.add(mapperToUse.apply(val));
        }

        _columnNameList.add(newColumnPosition, newColumnName);
        _columnList.add(newColumnPosition, newColumn);

        updateColumnIndex(newColumnPosition, newColumnName);

        modCount++;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumn(final String newColumnName, final Collection<String> fromColumnNames, final Function<? super DisposableObjArray, ?> func)
            throws IllegalStateException, IllegalArgumentException {
        addColumn(_columnList.size(), newColumnName, fromColumnNames, func);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumn(final int newColumnPosition, final String newColumnName, final Collection<String> fromColumnNames,
            final Function<? super DisposableObjArray, ?> func) throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();

        final int columnCount = columnCount();
        N.checkPositionIndex(newColumnPosition, columnCount);

        if (Strings.isEmpty(newColumnName)) {
            throw new IllegalArgumentException("The new column name can not be null or empty");
        }

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included in this Dataset: " + _columnNameList);
        }

        final int[] fromColumnIndexes = checkColumnNames(fromColumnNames);
        N.checkArgNotNull(func, cs.func);
        final int size = size();
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

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumn(final String newColumnName, final Tuple2<String, String> fromColumnNames, final BiFunction<?, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException {
        addColumn(_columnList.size(), newColumnName, fromColumnNames, func);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumn(final int newColumnPosition, final String newColumnName, final Tuple2<String, String> fromColumnNames, final BiFunction<?, ?, ?> func)
            throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();

        final int columnCount = columnCount();
        N.checkPositionIndex(newColumnPosition, columnCount);

        if (Strings.isEmpty(newColumnName)) {
            throw new IllegalArgumentException("The new column name can not be null or empty");
        }

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included in this Dataset: " + _columnNameList);
        }

        N.checkArgNotNull(fromColumnNames, cs.fromColumnNames);
        final List<Object> column1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(fromColumnNames._2));
        N.checkArgNotNull(func, cs.func);
        final int size = size();

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

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumn(final String newColumnName, final Tuple3<String, String, String> fromColumnNames, final TriFunction<?, ?, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException {
        addColumn(_columnList.size(), newColumnName, fromColumnNames, func);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumn(final int newColumnPosition, final String newColumnName, final Tuple3<String, String, String> fromColumnNames,
            final TriFunction<?, ?, ?, ?> func) throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();

        final int columnCount = columnCount();
        N.checkPositionIndex(newColumnPosition, columnCount);

        if (Strings.isEmpty(newColumnName)) {
            throw new IllegalArgumentException("The new column name can not be null or empty");
        }

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included in this Dataset: " + _columnNameList);
        }

        N.checkArgNotNull(fromColumnNames, cs.fromColumnNames);
        final List<Object> column1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(fromColumnNames._3));
        N.checkArgNotNull(func, cs.func);
        final int size = size();

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

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumns(final List<String> newColumnNames, final List<? extends Collection<?>> newColumns)
            throws IllegalStateException, IllegalArgumentException {
        addColumns(_columnList.size(), newColumnNames, newColumns);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException if {@code newColumnPosition} is negative or greater than {@code columnCount()}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addColumns(final int newColumnPosition, final List<String> newColumnNames, final List<? extends Collection<?>> newColumns)
            throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();
        N.checkPositionIndex(newColumnPosition, columnCount());
        N.checkArgNotNull(newColumnNames, cs.newColumnNames);

        for (final String newColumnName : newColumnNames) {
            if (N.isEmpty(newColumnName)) {
                throw new IllegalArgumentException("Empty new column name found in: " + newColumnNames);
            }

            if (containsColumn(newColumnName)) {
                throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included in this Dataset: " + _columnNameList);
            }
        }

        if (N.containsDuplicates(newColumnNames)) {
            throw new IllegalArgumentException("Duplicated new column names found in: " + newColumnNames);
        }

        N.checkArgNotNull(newColumns, cs.newColumns);
        N.checkArgument(N.size(newColumnNames) == N.size(newColumns), "The size of newColumnNames and columns must be the same.");

        if (N.isEmpty(newColumnNames)) {
            return;
        }

        // On a zero-column Dataset the incoming columns establish the row count - see
        // addColumn(int, String, Collection). They must still all agree with each other.
        int size = size();

        if (columnCount() == 0) {
            for (final Collection<?> column : newColumns) {
                if (N.notEmpty(column)) {
                    size = column.size();
                    break;
                }
            }
        }

        for (final Collection<?> column : newColumns) {
            if (N.notEmpty(column) && N.size(column) != size) {
                throw new IllegalArgumentException("The specified column size[" + column.size() + "] must be the same as this Dataset size[" + size + "]. ");
            }
        }

        final List<List<Object>> columnsToAdd = new ArrayList<>(newColumns.size());

        for (final Collection<?> column : newColumns) {
            if (N.isEmpty(column)) {
                columnsToAdd.add(N.repeat(null, size));
            } else {
                columnsToAdd.add(new ArrayList<>(column));
            }
        }

        _columnNameList.addAll(newColumnPosition, newColumnNames);
        _columnList.addAll(newColumnPosition, columnsToAdd);

        _columnIndexMap = null;
        _columnIndexes = null;

        modCount++;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public <T> List<T> removeColumn(final String columnName) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndex = checkColumnName(columnName);
        checkRemainingColumns(columnCount() - 1);

        _columnIndexMap = null;
        _columnIndexes = null;

        _columnNameList.remove(columnIndex);
        final List<Object> removedColumn = _columnList.remove(columnIndex);

        normalizeCurrentRowIndex();

        modCount++;

        return (List) removedColumn;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void removeColumns(final Collection<String> columnNames) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(columnNames, cs.columnNames);

        if (columnNames.isEmpty()) {
            return;
        }

        // getColumnIndexes, not checkColumnNames: removing a column twice is the same as removing it once, so this is
        // the one selection that tolerates a repeated name. Validate first; then build a sorted, deduplicated array
        // so duplicates in the input don't shift indices.
        final int[] tmp = getColumnIndexes(columnNames);
        final int[] columnIndexes = N.distinct(tmp);
        N.sort(columnIndexes);
        checkRemainingColumns(columnCount() - columnIndexes.length);

        for (int i = 0, len = columnIndexes.length; i < len; i++) {
            _columnNameList.remove(columnIndexes[i] - i);
            _columnList.remove(columnIndexes[i] - i);
        }

        _columnIndexMap = null;
        _columnIndexes = null;

        normalizeCurrentRowIndex();

        modCount++;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void removeColumns(final Predicate<? super String> filter) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(filter, cs.filter);

        final List<String> columnNames = filterColumnNames(_columnNameList, filter);

        if (N.notEmpty(columnNames)) {
            removeColumns(columnNames);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void convertColumn(final String columnName, final Class<?> targetType) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        convertColumnType(checkColumnName(columnName), targetType);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void convertColumns(final Map<String, Class<?>> columnTargetTypes) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(columnTargetTypes, cs.columnTargetTypes);

        if (columnTargetTypes.isEmpty()) {
            return;
        }

        checkColumnNames(columnTargetTypes.keySet());

        // Convert every selected column before writing any of them back. Converting them one at a time left
        // the Dataset partly converted - the first two columns in the target type, the rest still in the
        // source type - when N.convert threw on the third, with no way for the caller to tell how far it got.
        final int size = size();
        final int selectedColumnCount = columnTargetTypes.size();
        final List<List<Object>> columnsToUpdate = new ArrayList<>(selectedColumnCount);
        final List<Object[]> convertedColumns = new ArrayList<>(selectedColumnCount);

        for (final Map.Entry<String, Class<?>> entry : columnTargetTypes.entrySet()) {
            final List<Object> column = _columnList.get(checkColumnName(entry.getKey()));
            columnsToUpdate.add(column);
            convertedColumns.add(convertColumnValues(column, size, entry.getValue()));
        }

        for (int i = 0; i < selectedColumnCount; i++) {
            writeBackColumnValues(columnsToUpdate.get(i), convertedColumns.get(i));
        }
        // No modCount bump: see convertColumnType(int, Class).
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void updateColumn(final String columnName, final Function<?, ?> func) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final List<Object> column = _columnList.get(checkColumnName(columnName));
        N.checkArgNotNull(func, cs.func);

        final Function<Object, Object> funcToUse = (Function<Object, Object>) func;

        for (int i = 0, len = size(); i < len; i++) {
            column.set(i, funcToUse.apply(column.get(i)));
        }
        // No modCount bump: this rewrites cell values in place without changing the row count, the column
        // set or any ordering, so live iterators and streams remain valid. See set(int, int, Object).
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void updateColumns(final Collection<String> columnNames, final IntBiObjFunction<String, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(columnNames, cs.columnNames);
        if (!columnNames.isEmpty()) {
            checkColumnNames(columnNames);
        }
        N.checkArgNotNull(func, cs.func);

        if (columnNames.isEmpty()) {
            return;
        }

        final IntBiObjFunction<String, Object, Object> funcToUse = (IntBiObjFunction<String, Object, Object>) func;
        final int size = size();

        for (final String columnName : columnNames) {
            final List<Object> column = _columnList.get(checkColumnName(columnName));

            for (int i = 0; i < size; i++) {
                column.set(i, funcToUse.apply(i, columnName, column.get(i)));
            }
        }
        // No modCount bump: this rewrites cell values in place without changing the row count, the column
        // set or any ordering, so live iterators and streams remain valid. See set(int, int, Object).
    }

    private void convertColumnType(final int columnIndex, final Class<?> targetType) {
        final List<Object> column = _columnList.get(columnIndex);

        // Convert into a scratch array first and write it back only once every value has converted
        // successfully. Converting in place left the column holding a mix of source-type and target-type
        // values when N.convert threw part way through - e.g. ["1", "x", "3"] -> Integer became
        // [1, "x", "3"], a state no caller can recover from or even detect.
        writeBackColumnValues(column, convertColumnValues(column, size(), targetType));
        // No modCount bump: this rewrites cell values in place without changing the row count, the column
        // set or any ordering, so live iterators and streams remain valid. See set(int, int, Object).
    }

    /**
     * Converts the first {@code size} values of {@code column} to {@code targetType} into a new array,
     * leaving {@code column} untouched. Any conversion failure propagates before anything is written back.
     *
     * @param column the column to read
     * @param size the number of values to convert, i.e. the dataset's row count
     * @param targetType the type to convert each value to
     * @return the converted values
     */
    private static Object[] convertColumnValues(final List<Object> column, final int size, final Class<?> targetType) {
        final Object[] converted = new Object[size];

        for (int i = 0; i < size; i++) {
            converted[i] = N.convert(column.get(i), targetType);
        }

        return converted;
    }

    /**
     * Writes {@code converted} back into {@code column} positionally.
     *
     * <p>{@code set}, not a replacement list: {@link #getColumn(int)} hands out an {@code ImmutableList}
     * wrapper around this very list object, so swapping in a new list would silently detach every view a
     * caller is already holding.</p>
     *
     * @param column the column to overwrite
     * @param converted the values to write, one per row
     */
    private static void writeBackColumnValues(final List<Object> column, final Object[] converted) {
        for (int i = 0, len = converted.length; i < len; i++) {
            column.set(i, converted[i]);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void combineColumns(final Collection<String> columnNames, final String newColumnName, final Class<?> newColumnType)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        if (N.isEmpty(columnNames)) {
            throw new IllegalArgumentException("Column names to be combined cannot be null or empty");
        }

        // Snapshot: columnNames may be the live column-name view, which addColumn mutates;
        // re-reading it in removeColumns would then also remove the just-added combined column.
        final List<String> columnNamesToCombine = new ArrayList<>(columnNames);

        final int positionToAdd = checkColumnNamesForCombination(columnNamesToCombine, newColumnName);

        final List<Object> newColumn = toList(0, size(), columnNamesToCombine, newColumnType);

        addColumn(positionToAdd, newColumnName, newColumn);

        removeColumns(columnNamesToCombine);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void combineColumns(final Collection<String> columnNames, final String newColumnName, final Function<? super DisposableObjArray, ?> combineFunc)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        if (N.isEmpty(columnNames)) {
            throw new IllegalArgumentException("Column names to be combined cannot be null or empty");
        }

        // Snapshot: columnNames may be the live column-name view, which addColumn mutates;
        // re-reading it in removeColumns would then also remove the just-added combined column.
        final List<String> columnNamesToCombine = new ArrayList<>(columnNames);

        final int positionToAdd = checkColumnNamesForCombination(columnNamesToCombine, newColumnName);

        N.checkArgNotNull(combineFunc, cs.combineFunc);
        addColumn(positionToAdd, newColumnName, columnNamesToCombine, combineFunc);

        removeColumns(columnNamesToCombine);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void combineColumns(final Tuple2<String, String> columnNames, final String newColumnName, final BiFunction<?, ?, ?> combineFunc)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(columnNames, cs.columnNames);

        final List<String> columnNameList = Arrays.asList(columnNames._1, columnNames._2);
        final int positionToAdd = checkColumnNamesForCombination(columnNameList, newColumnName);

        N.checkArgNotNull(combineFunc, cs.combineFunc);
        addColumn(positionToAdd, newColumnName, columnNames, combineFunc);

        removeColumns(columnNameList);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void combineColumns(final Tuple3<String, String, String> columnNames, final String newColumnName, final TriFunction<?, ?, ?, ?> combineFunc)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(columnNames, cs.columnNames);

        final List<String> columnNameList = Arrays.asList(columnNames._1, columnNames._2, columnNames._3);
        final int positionToAdd = checkColumnNamesForCombination(columnNameList, newColumnName);

        N.checkArgNotNull(combineFunc, cs.combineFunc);
        addColumn(positionToAdd, newColumnName, columnNames, combineFunc);

        removeColumns(columnNameList);
    }

    /**
     * @throws IllegalArgumentException if {@code columnNames} is null or empty, repeats or names an absent column, or {@code newColumnName} is null, empty, or already present
     */
    private int checkColumnNamesForCombination(final Collection<String> columnNames, final String newColumnName) throws IllegalArgumentException {
        if (N.isEmpty(columnNames)) {
            throw new IllegalArgumentException("Column names to be combined cannot be null or empty");
        }

        final int[] columnIndexes = checkColumnNames(columnNames);

        if (Strings.isEmpty(newColumnName)) {
            throw new IllegalArgumentException("The new column name can not be null or empty");
        }

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included in this Dataset: " + _columnNameList);
        }

        return N.min(columnIndexes);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void divideColumn(final String columnName, final Collection<String> newColumnNames, final Function<?, ? extends List<?>> divideFunc)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndex = checkColumnName(columnName);

        // Own the schema before callbacks or publication: reading caller names can fail or change them.
        final List<String> replacementNames = newColumnNames == null ? N.emptyList() : new ArrayList<>(newColumnNames);

        if (N.isEmpty(replacementNames)) {
            throw new IllegalArgumentException("New column names cannot be null or empty");
        }

        if (N.anyEmpty(replacementNames)) {
            throw new IllegalArgumentException("New column names cannot contain null or empty names: " + replacementNames);
        }

        if (!N.disjoint(_columnNameList, replacementNames)) {
            throw new IllegalArgumentException(
                    "Column names: " + N.intersection(_columnNameList, replacementNames) + " are already included in this data set.");
        }

        if (N.containsDuplicates(replacementNames)) {
            throw new IllegalArgumentException("Duplicated new column names found in: " + replacementNames);
        }

        N.checkArgNotNull(divideFunc, cs.divideFunc);
        final Function<Object, List<Object>> divideFuncToUse = (Function<Object, List<Object>>) divideFunc;
        final int newColumnsLen = replacementNames.size();
        final List<List<Object>> newColumns = new ArrayList<>(newColumnsLen);

        for (int i = 0; i < newColumnsLen; i++) {
            newColumns.add(new ArrayList<>(size()));
        }

        final List<Object> column = _columnList.get(columnIndex);

        for (final Object val : column) {
            final List<Object> newVals = divideFuncToUse.apply(val);

            if (newVals == null || newVals.size() != newColumnsLen) {
                throw new IllegalArgumentException(
                        "divideFunc must return a list with exactly " + newColumnsLen + " elements, but got: " + (newVals == null ? "null" : newVals.size()));
            }

            for (int i = 0; i < newColumnsLen; i++) {
                newColumns.get(i).add(newVals.get(i));
            }
        }

        _columnNameList.remove(columnIndex);
        _columnNameList.addAll(columnIndex, replacementNames);

        _columnList.remove(columnIndex);
        _columnList.addAll(columnIndex, newColumns);

        _columnIndexMap = null;
        _columnIndexes = null;

        modCount++;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void divideColumn(final String columnName, final Collection<String> newColumnNames, final BiConsumer<?, Object[]> output)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndex = checkColumnName(columnName);

        // Own the schema before callbacks or publication: reading caller names can fail or change them.
        final List<String> replacementNames = newColumnNames == null ? N.emptyList() : new ArrayList<>(newColumnNames);

        if (N.isEmpty(replacementNames)) {
            throw new IllegalArgumentException("New column names cannot be null or empty");
        }

        if (N.anyEmpty(replacementNames)) {
            throw new IllegalArgumentException("New column names cannot contain null or empty names: " + replacementNames);
        }

        if (!N.disjoint(_columnNameList, replacementNames)) {
            throw new IllegalArgumentException(
                    "Column names: " + N.intersection(_columnNameList, replacementNames) + " are already included in this data set.");
        }

        if (N.containsDuplicates(replacementNames)) {
            throw new IllegalArgumentException("Duplicated new column names found in: " + replacementNames);
        }

        N.checkArgNotNull(output, cs.output);
        final BiConsumer<Object, Object[]> outputToUse = (BiConsumer<Object, Object[]>) output;
        final int newColumnsLen = replacementNames.size();
        final List<List<Object>> newColumns = new ArrayList<>(newColumnsLen);

        for (int i = 0; i < newColumnsLen; i++) {
            newColumns.add(new ArrayList<>(size()));
        }

        final List<Object> column = _columnList.get(columnIndex);
        final Object[] tmp = new Object[newColumnsLen];

        for (final Object val : column) {
            // Clear the reusable output buffer each row so slots left unwritten by the consumer
            // cannot leak values from a previous row into later rows.
            N.fill(tmp, null);

            outputToUse.accept(val, tmp);

            for (int i = 0; i < newColumnsLen; i++) {
                newColumns.get(i).add(tmp[i]);
            }
        }

        _columnNameList.remove(columnIndex);
        _columnNameList.addAll(columnIndex, replacementNames);

        _columnList.remove(columnIndex);
        _columnList.addAll(columnIndex, newColumns);

        _columnIndexMap = null;
        _columnIndexes = null;

        modCount++;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void divideColumn(final String columnName, final Tuple2<String, String> newColumnNames, final BiConsumer<?, Pair<Object, Object>> output)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndex = checkColumnName(columnName);
        N.checkArgNotNull(newColumnNames, cs.newColumnNames);

        checkNewColumnName(newColumnNames._1);
        checkNewColumnName(newColumnNames._2);

        if (N.equals(newColumnNames._1, newColumnNames._2)) {
            throw new IllegalArgumentException("Duplicated new column names found in: " + newColumnNames);
        }

        N.checkArgNotNull(output, cs.output);
        final BiConsumer<Object, Pair<Object, Object>> outputToUse = (BiConsumer<Object, Pair<Object, Object>>) output;
        final List<Object> newColumn1 = new ArrayList<>(size());
        final List<Object> newColumn2 = new ArrayList<>(size());

        final List<Object> column = _columnList.get(columnIndex);
        final Pair<Object, Object> tmp = new Pair<>();

        for (final Object val : column) {
            // Clear the reusable pair each row, exactly as the Object[] overload clears its buffer: a slot the
            // consumer leaves unset must read as null, not as whatever the previous row stored there.
            tmp.set(null, null);

            outputToUse.accept(val, tmp);

            newColumn1.add(tmp.left());
            newColumn2.add(tmp.right());
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
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void divideColumn(final String columnName, final Tuple3<String, String, String> newColumnNames,
            final BiConsumer<?, Triple<Object, Object, Object>> output) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndex = checkColumnName(columnName);
        N.checkArgNotNull(newColumnNames, cs.newColumnNames);

        checkNewColumnName(newColumnNames._1);
        checkNewColumnName(newColumnNames._2);
        checkNewColumnName(newColumnNames._3);

        if (N.equals(newColumnNames._1, newColumnNames._2) || N.equals(newColumnNames._1, newColumnNames._3)
                || N.equals(newColumnNames._2, newColumnNames._3)) {
            throw new IllegalArgumentException("Duplicated new column names found in: " + newColumnNames);
        }

        N.checkArgNotNull(output, cs.output);
        final BiConsumer<Object, Triple<Object, Object, Object>> outputToUse = (BiConsumer<Object, Triple<Object, Object, Object>>) output;
        final List<Object> newColumn1 = new ArrayList<>(size());
        final List<Object> newColumn2 = new ArrayList<>(size());
        final List<Object> newColumn3 = new ArrayList<>(size());

        final List<Object> column = _columnList.get(columnIndex);
        final Triple<Object, Object, Object> tmp = new Triple<>();

        for (final Object val : column) {
            // Clear the reusable triple each row - see divideColumn(String, Tuple2, BiConsumer).
            tmp.set(null, null, null);

            outputToUse.accept(val, tmp);

            newColumn1.add(tmp.left());
            newColumn2.add(tmp.middle());
            newColumn3.add(tmp.right());
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
    public Stream<ImmutableList<Object>> columns() {
        // Check modCount per element like every other lazy source. Without it a column removed after this call
        // surfaced as a bare IndexOutOfBoundsException from getColumn(int) instead of the documented
        // ConcurrentModificationException.
        final int expectedModCount = modCount;

        //noinspection resource
        return IntStream.range(0, columnCount()).mapToObj(columnIndex -> {
            checkModification(expectedModCount);

            return getColumn(columnIndex);
        });
    }

    @Override
    public Map<String, ImmutableList<Object>> columnMap() {
        final Map<String, ImmutableList<Object>> result = N.newLinkedHashMap(_columnNameList.size());

        for (final String columnName : _columnNameList) {
            result.put(columnName, getColumn(columnName));
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addRow(final Object row) throws IllegalStateException, IllegalArgumentException {
        addRow(size(), row);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void addRow(final int newRowPosition, final Object row) throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        checkFrozen();
        N.checkArgument(columnCount() > 0, "Cannot add a row to a Dataset without columns");

        final int size = size();
        N.checkPositionIndex(newRowPosition, size);

        final Object[] values = normalizeRow(row);

        if (newRowPosition == size) {
            for (int columnIndex = 0, len = values.length; columnIndex < len; columnIndex++) {
                _columnList.get(columnIndex).add(values[columnIndex]);
            }
        } else {
            for (int columnIndex = 0, len = values.length; columnIndex < len; columnIndex++) {
                _columnList.get(columnIndex).add(newRowPosition, values[columnIndex]);
            }
        }

        rowsChanged();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addRows(final Collection<?> rows) throws IllegalStateException, IllegalArgumentException {
        addRows(size(), rows);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addRows(final int newRowPosition, final Collection<?> rows) throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();

        final int size = size();
        N.checkPositionIndex(newRowPosition, size);

        if (N.isEmpty(rows)) {
            return;
        }

        final int columnCount = columnCount();
        final int rowCountToAdd = rows.size();
        final List<List<Object>> columnsToAdd = new ArrayList<>(columnCount);

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            columnsToAdd.add(new ArrayList<>(rowCountToAdd));
        }

        // Normalize the complete batch first. Besides allowing each row to use a different supported
        // representation, this prevents a later validation/property-access failure from leaving only
        // some columns enlarged and violating the dataset's equal-column-size invariant.
        for (final Object row : rows) {
            final Object[] values = normalizeRow(row);

            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                columnsToAdd.get(columnIndex).add(values[columnIndex]);
            }
        }

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            _columnList.get(columnIndex).addAll(newRowPosition, columnsToAdd.get(columnIndex));
        }

        rowsChanged();
    }

    /**
     * @throws IllegalArgumentException if {@code remainingColumns} is not positive while this dataset still has rows
     */
    private void checkRemainingColumns(final int remainingColumns) throws IllegalArgumentException {
        N.checkArgument(remainingColumns > 0 || size() == 0, "Cannot remove every column from a Dataset with rows; clear the rows first");
    }

    /**
     * @throws IllegalArgumentException if this dataset has no columns, {@code row} is null or has an unsupported type, its array or collection size differs from the column count, or a required column is absent from its map or bean
     */
    private Object[] normalizeRow(final Object row) throws IllegalArgumentException {
        N.checkArgument(columnCount() > 0, "Cannot add a row to a Dataset without columns");
        N.checkArgNotNull(row, ROW);

        final Class<?> rowClass = row.getClass();
        final Type<?> rowType = Type.of(rowClass);
        final int columnCount = columnCount();
        final Object[] values = new Object[columnCount];

        if (rowType.isObjectArray()) {
            final Object[] array = (Object[]) row;

            // Exact match, not just "long enough": a positional row that is too long used to be silently
            // truncated to the first columnCount elements, dropping data with no diagnostic. The row-oriented
            // factories (Dataset.rows) have always rejected a length mismatch, so this aligns the two.
            if (array.length != columnCount) {
                throw new IllegalArgumentException("The size of array (" + array.length + ") does not match the number of columns (" + columnCount + ")");
            }

            System.arraycopy(array, 0, values, 0, columnCount);
        } else if (rowType.isCollection()) {
            final Collection<?> collection = (Collection<?>) row;

            if (collection.size() != columnCount) {
                throw new IllegalArgumentException(
                        "The size of collection (" + collection.size() + ") does not match the number of columns (" + columnCount + ")");
            }

            final Iterator<?> iterator = collection.iterator();

            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                values[columnIndex] = iterator.next();
            }
        } else if (rowType.isMap()) {
            final Map<?, ?> map = (Map<?, ?>) row;

            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                final String columnName = _columnNameList.get(columnIndex);
                values[columnIndex] = map.get(columnName);

                if (values[columnIndex] == null && !map.containsKey(columnName)) {
                    throw new IllegalArgumentException("Column (" + columnName + ") is not found in map (" + map.keySet() + ")");
                }
            }
        } else if (rowType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowClass);

            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                final String columnName = _columnNameList.get(columnIndex);
                final PropInfo propInfo = beanInfo.getPropInfo(columnName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Column (" + columnName + ") is not found in bean (" + rowClass + ")");
                }

                values[columnIndex] = propInfo.getPropValue(row);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and bean class are supported");
        }

        return values;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void removeRow(final int rowIndex) throws IllegalStateException, IndexOutOfBoundsException {
        checkFrozen();

        checkRowIndex(rowIndex);

        for (final List<Object> objects : _columnList) {
            objects.remove(rowIndex);
        }

        normalizeCurrentRowIndex();

        rowsChanged();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IndexOutOfBoundsException if a row index is negative or not less than {@code size()}
     */
    @SafeVarargs
    @Override
    public final void removeRowsAt(final int... rowIndexesToRemove) throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        checkFrozen();

        N.checkArgNotNull(rowIndexesToRemove, cs.rowIndexesToRemove);

        for (final int rowIndex : rowIndexesToRemove) {
            checkRowIndex(rowIndex);
        }

        // Removing nothing is not a structural modification, so it must not bump modCount: doing so
        // invalidated every outstanding stream/split/paginate for an operation that changed no row.
        if (N.isEmpty(rowIndexesToRemove)) {
            return;
        }

        // De-duplicate and sort ONCE here. N.removeAt(List, int...) clones and sorts its index array on every
        // call, so the previous per-column loop repeated that work columnCount() times, and then rebuilt each
        // column via toArray/clear/addAll. N.distinct returns a new array, so the caller's varargs array is
        // never sorted in place.
        final int[] sortedIndexes = N.distinct(rowIndexesToRemove);
        N.sort(sortedIndexes);

        for (final List<Object> element : _columnList) {
            removeAtSortedIndexes(element, sortedIndexes);
        }

        normalizeCurrentRowIndex();

        rowsChanged();
    }

    /**
     * Removes the elements at {@code sortedIndexes} from {@code list} with a single in-place compaction pass:
     * every surviving element is shifted down over the gaps, then the now-duplicated tail is dropped in one
     * {@code subList(..).clear()}. This allocates nothing and touches each element at most once.
     *
     * @param list the column to compact. It is indexed positionally, so a column that is not random-access is
     *            quadratic here - as it already is in every other positional loop in this class
     *            ({@code updateAll}, {@code replaceIf}, {@code permuteRows}, the joins, the set operations)
     * @param sortedIndexes strictly ascending, in-range positions to remove; must not be empty
     */
    private static void removeAtSortedIndexes(final List<Object> list, final int[] sortedIndexes) {
        final int size = list.size();
        final int removeCount = sortedIndexes.length;
        int nextToRemove = 0;
        int dest = 0;

        for (int src = 0; src < size; src++) {
            if (nextToRemove < removeCount && sortedIndexes[nextToRemove] == src) {
                nextToRemove++;
                continue;
            }

            if (dest != src) {
                list.set(dest, list.get(src));
            }

            dest++;
        }

        list.subList(dest, size).clear();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void removeRows(final int inclusiveFromRowIndex, final int exclusiveToRowIndex) throws IllegalStateException, IndexOutOfBoundsException {
        checkFrozen();

        checkRowIndex(inclusiveFromRowIndex, exclusiveToRowIndex);

        // An empty range removes no row - see removeRowsAt(int...).
        if (inclusiveFromRowIndex == exclusiveToRowIndex) {
            return;
        }

        for (final List<Object> objects : _columnList) {
            objects.subList(inclusiveFromRowIndex, exclusiveToRowIndex).clear();
        }

        normalizeCurrentRowIndex();

        rowsChanged();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void removeDuplicateRowsBy(final String keyColumnName) throws IllegalStateException, IllegalArgumentException {
        removeDuplicateRowsBy(keyColumnName, Fn.identity());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void removeDuplicateRowsBy(final String keyColumnName, final Function<?, ?> keyExtractor) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();
        final int columnIndex = checkColumnName(keyColumnName);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        final int size = size();

        if (size <= 1) {
            return;
        }

        final int columnCount = columnCount();
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        // The number of surviving rows is unknown up front. Grow each column with the result
        // instead of reserving storage for every input row; additions are amortized O(1).
        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final boolean isIdentityKeyExtractor = keyExtractor == Fn.identity();
        final Function<Object, ?> keyExtractorToUse = (Function<Object, ?>) keyExtractor;
        final Set<Object> rowSet = N.newHashSet();
        final List<Object> keyColumn = _columnList.get(columnIndex);
        Object key = null;
        Object value = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            value = keyColumn.get(rowIndex);
            key = hashKey(isIdentityKeyExtractor ? value : keyExtractorToUse.apply(value));

            if (rowSet.add(key)) {
                for (int i = 0; i < columnCount; i++) {
                    newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                }
            }
        }

        // Removing nothing is not a structural modification, so it must not bump modCount: doing so
        // invalidated every outstanding stream/split/paginate for a call that dropped no row.
        if (rowSet.size() == size) {
            return;
        }

        for (int i = 0; i < columnCount; i++) {
            _columnList.get(i).clear();
            _columnList.get(i).addAll(newColumnList.get(i));
        }

        normalizeCurrentRowIndex();

        rowsChanged();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void removeDuplicateRowsBy(final Collection<String> keyColumnNames) throws IllegalStateException, IllegalArgumentException {
        removeDuplicateRowsBy(keyColumnNames, Fn.identity());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void removeDuplicateRowsBy(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);
        final int[] keyColumnIndexes = checkColumnNames(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        final boolean isIdentityKeyExtractor = keyExtractor == Fn.identity();

        if (keyColumnNames.size() == 1 && isIdentityKeyExtractor) {
            removeDuplicateRowsBy(keyColumnNames.iterator().next());

            return;
        }

        final int size = size();

        if (size <= 1) {
            return;
        }

        final int columnCount = columnCount();
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final Set<Object> rowSet = N.newHashSet();
        final int keyColumnCount = keyColumnIndexes.length;
        Object[] row = Objectory.createObjectArray(keyColumnCount);
        Wrapper<Object[]> rowWrapper = isIdentityKeyExtractor ? Wrapper.of(row) : null;
        final DisposableObjArray disposableArray = isIdentityKeyExtractor ? null : DisposableObjArray.wrap(row);
        Object key = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0, len = keyColumnIndexes.length; i < len; i++) {
                row[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            key = isIdentityKeyExtractor ? rowWrapper : hashKey(keyExtractor.apply(disposableArray));

            if (rowSet.add(key)) {
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }

                if (isIdentityKeyExtractor) {
                    row = Objectory.createObjectArray(keyColumnCount);
                    rowWrapper = Wrapper.of(row);
                }
            }
        }

        if (row != null) {
            Objectory.recycle(row);
            row = null;
        }

        final boolean anyRemoved = rowSet.size() < size;

        if (isIdentityKeyExtractor) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) rowSet;

            for (final Wrapper<Object[]> rw : tmp) {
                Objectory.recycle(rw.value());
            }
        }

        // See removeDuplicateRowsBy(String, Function): a call that drops no row is not a structural
        // modification. The pooled key arrays above are recycled either way.
        if (!anyRemoved) {
            return;
        }

        for (int i = 0; i < columnCount; i++) {
            _columnList.get(i).clear();
            _columnList.get(i).addAll(newColumnList.get(i));
        }

        normalizeCurrentRowIndex();

        rowsChanged();
    }

    private void normalizeCurrentRowIndex() {
        final int rowCount = size();

        if (rowCount == 0) {
            _currentRowIndex = 0;
        } else if (_currentRowIndex >= rowCount) {
            _currentRowIndex = rowCount - 1;
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void updateRow(final int rowIndex, final Function<?, ?> func) throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();

        checkRowIndex(rowIndex);
        N.checkArgNotNull(func, cs.func);

        final Function<Object, Object> funcToUse = (Function<Object, Object>) func;

        for (final List<Object> column : _columnList) {
            column.set(rowIndex, funcToUse.apply(column.get(rowIndex)));
        }
        // No modCount bump: this rewrites cell values in place without changing the row count, the column
        // set or any ordering, so live iterators and streams remain valid. See set(int, int, Object).
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void updateRows(final int[] rowIndexesToUpdate, final IntBiObjFunction<String, ?, ?> func)
            throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        checkFrozen();

        N.checkArgNotNull(rowIndexesToUpdate, cs.rowIndexesToUpdate);

        for (final int rowIndex : rowIndexesToUpdate) {
            checkRowIndex(rowIndex);
        }

        N.checkArgNotNull(func, cs.func);
        final IntBiObjFunction<String, Object, Object> funcToUse = (IntBiObjFunction<String, Object, Object>) func;
        final int columnCount = columnCount();

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            final String columnName = _columnNameList.get(columnIndex);
            final List<Object> column = _columnList.get(columnIndex);

            for (final int rowIndex : rowIndexesToUpdate) {
                column.set(rowIndex, funcToUse.apply(rowIndex, columnName, column.get(rowIndex)));
            }
        }
        // No modCount bump: this rewrites cell values in place without changing the row count, the column
        // set or any ordering, so live iterators and streams remain valid. See set(int, int, Object).
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void updateAll(final Function<?, ?> func) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(func, cs.func);

        final Function<Object, Object> funcToUse = (Function<Object, Object>) func;
        final int size = size();

        for (final List<Object> column : _columnList) {
            for (int i = 0; i < size; i++) {
                column.set(i, funcToUse.apply(column.get(i)));
            }
        }
        // No modCount bump: this rewrites cell values in place without changing the row count, the column
        // set or any ordering, so live iterators and streams remain valid. See set(int, int, Object).
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void updateAll(final IntBiObjFunction<String, ?, ?> func) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(func, cs.func);

        final IntBiObjFunction<String, Object, Object> funcToUse = (IntBiObjFunction<String, Object, Object>) func;
        final int columnCount = columnCount();
        final int size = size();

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            final String columnName = _columnNameList.get(columnIndex);
            final List<Object> column = _columnList.get(columnIndex);

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                column.set(rowIndex, funcToUse.apply(rowIndex, columnName, column.get(rowIndex)));
            }
        }
        // No modCount bump: this rewrites cell values in place without changing the row count, the column
        // set or any ordering, so live iterators and streams remain valid. See set(int, int, Object).
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void replaceIf(final Predicate<?> predicate, final Object newValue) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(predicate, cs.predicate);

        final Predicate<Object> predicateToUse = (Predicate<Object>) predicate;
        final int size = size();

        for (final List<Object> column : _columnList) {
            for (int i = 0; i < size; i++) {
                if (predicateToUse.test(column.get(i))) {
                    column.set(i, newValue);
                }
            }
        }
        // No modCount bump: this rewrites cell values in place without changing the row count, the column
        // set or any ordering, so live iterators and streams remain valid. See set(int, int, Object).
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void replaceIf(final IntBiObjPredicate<String, ?> predicate, final Object newValue) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(predicate, cs.predicate);

        final IntBiObjPredicate<String, Object> predicateToUse = (IntBiObjPredicate<String, Object>) predicate;
        final int columnCount = columnCount();
        final int size = size();

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            final String columnName = _columnNameList.get(columnIndex);
            final List<Object> column = _columnList.get(columnIndex);

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                if (predicateToUse.test(rowIndex, columnName, column.get(rowIndex))) {
                    column.set(rowIndex, newValue);
                }
            }
        }
        // No modCount bump: this rewrites cell values in place without changing the row count, the column
        // set or any ordering, so live iterators and streams remain valid. See set(int, int, Object).
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void prepend(final Dataset other) throws IllegalStateException, IllegalArgumentException {
        appendRows(other, 0);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void append(final Dataset other) throws IllegalStateException, IllegalArgumentException {
        appendRows(other, size());
    }

    private void appendRows(final Dataset other, final int rowPosition) {
        checkFrozen();
        N.checkArgNotNull(other, cs.other);
        checkIfColumnNamesAreSame(other, true);

        final int addedRows = other.size();
        final int[] columnIndexes = getColumnIndexes(other.columnNames());
        final List<List<Object>> incoming = new ArrayList<>(columnIndexes.length);
        final Map<String, Object> properties = copyProperties(other.getProperties());

        // A source slice can share every destination column. Snapshot all columns before the first
        // write, and never query the source after a write has invalidated that slice.
        for (int i = 0; i < columnIndexes.length; i++) {
            incoming.add(new ArrayList<>(other.getColumn(i)));
        }
        for (int i = 0; i < columnIndexes.length; i++) {
            _columnList.get(columnIndexes[i]).addAll(rowPosition, incoming.get(i));
        }
        mergeProperties(properties);
        if (addedRows > 0) {
            rowsChanged();
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void merge(final Dataset other) throws IllegalStateException, IllegalArgumentException {
        merge(other, false);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void merge(final Dataset other, final boolean requiresSameColumns) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();
        N.checkArgNotNull(other, cs.other);
        checkIfColumnNamesAreSame(other, requiresSameColumns);

        // Calls the private merge directly (instead of the public selection-based overload) because the
        // selection here is other's own column-name list, which doesn't need the selection validation
        // (and may legitimately be empty for a zero-column Dataset).
        merge(this, other, 0, other.size(), other.columnNames());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void merge(final Dataset other, final Collection<String> selectColumnNamesFromOtherToMerge) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(other, cs.other);

        merge(other, 0, other.size(), selectColumnNamesFromOtherToMerge);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void merge(final Dataset other, final int fromRowIndexFromOther, final int toRowIndexFromOther,
            final Collection<String> selectColumnNamesFromOtherToMerge) throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        checkFrozen();
        N.checkArgNotNull(other, cs.other);
        checkRowIndex(fromRowIndexFromOther, toRowIndexFromOther, other.size());
        N.checkArgNotEmpty(selectColumnNamesFromOtherToMerge, cs.selectColumnNamesFromOtherToMerge);

        if (!other.containsAllColumns(selectColumnNamesFromOtherToMerge)) {
            throw new IllegalArgumentException(
                    "Some select column names: " + selectColumnNamesFromOtherToMerge + " are not found in the other Dataset: " + other.columnNames());
        }

        // final RowDataset result = (RowDataset) copy();

        merge(this, other, fromRowIndexFromOther, toRowIndexFromOther, selectColumnNamesFromOtherToMerge);
    }

    private void merge(final RowDataset result, final Dataset other, final int fromRowIndexFromOther, final int toRowIndexFromOther,
            final Collection<String> selectColumnNamesFromOtherToMerge) {
        result.checkFrozen();
        final int addedRows = toRowIndexFromOther - fromRowIndexFromOther;
        final List<String> selectedNames = new ArrayList<>(selectColumnNamesFromOtherToMerge);
        final Map<String, List<Object>> incoming = new LinkedHashMap<>();
        final Map<String, Object> properties = copyProperties(other.getProperties());

        // Source and destination may overlap, including through a slice. Finish reading the source
        // before adding columns or rows; otherwise a failure can leave columns at different lengths.
        for (final String name : selectedNames) {
            incoming.put(name, new ArrayList<>(other.getColumn(name).subList(fromRowIndexFromOther, toRowIndexFromOther)));
        }
        for (final String name : selectedNames) {
            if (!result.containsColumn(name)) {
                result.addColumn(name, N.emptyList());
            }
        }
        final List<Object> nulls = java.util.Collections.nCopies(addedRows, null);
        for (int i = 0; i < result.columnCount(); i++) {
            result._columnList.get(i).addAll(incoming.getOrDefault(result._columnNameList.get(i), nulls));
        }
        result.mergeProperties(properties);
        if (addedRows > 0) {
            result.rowsChanged();
        }
    }

    private void mergeProperties(final Map<String, Object> properties) {
        if (N.notEmpty(properties)) {
            if (_properties == EMPTY_PROPERTIES) {
                // newTargetMap, not newOrderingMap - see copyProperties(Map).
                _properties = Maps.newTargetMap(properties);
            }

            _properties.putAll(properties);
        }
    }

    @Override
    public int currentRowIndex() {
        return _currentRowIndex;
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public Dataset moveToRow(final int rowIndex) throws ConcurrentModificationException, IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        _currentRowIndex = rowIndex;

        return this;
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= size()}
     */
    @Override
    public Dataset.Row row(final int rowIndex) throws ConcurrentModificationException, IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return new RowView(rowIndex);
    }

    /**
     * Cursor-free accessor for one row, bound to a row index. Holds no values of its own, so reads and writes
     * go straight to the enclosing dataset. Structural row/column changes invalidate the view; see {@link Dataset#row(int)}.
     */
    private final class RowView implements Dataset.Row {

        private final int rowIndex;
        private final int expectedModCount = modCount;

        private RowView(final int rowIndex) {
            this.rowIndex = rowIndex;
        }

        private void checkValidity() {
            // modCount covers column layout as well as rows; cell edits and cursor movement do not bump it.
            // Row's default accessors delegate here before resolving names or converting stale cell values.
            checkModification(expectedModCount);
        }

        @Override
        public int rowIndex() {
            checkValidity();
            return rowIndex;
        }

        /**
         * {@inheritDoc}
         * @throws ConcurrentModificationException if a structural row/column change or parent-slice invalidation made this view stale
         */
        @Override
        public int columnCount() throws ConcurrentModificationException {
            checkValidity();
            return RowDataset.this.columnCount();
        }

        /**
         * {@inheritDoc}
         * @throws IllegalArgumentException {@inheritDoc}
         */
        @Override
        public int columnIndex(final String columnName) throws IllegalArgumentException {
            checkValidity();
            return checkColumnName(columnName);
        }

        /**
         * {@inheritDoc}
         * @throws IndexOutOfBoundsException if the column index is out of bounds
         * @throws ConcurrentModificationException if a structural row/column change or parent-slice invalidation made this view stale
         */
        @Override
        public <T> T get(final int columnIndex) throws IndexOutOfBoundsException, ConcurrentModificationException {
            checkValidity();
            return RowDataset.this.get(rowIndex, columnIndex);
        }

        /**
         * {@inheritDoc}
         * @throws IllegalStateException if this dataset is frozen
         * @throws IndexOutOfBoundsException {@inheritDoc}
         */
        @Override
        public void set(final int columnIndex, final Object value) throws IllegalStateException, IndexOutOfBoundsException {
            checkValidity();
            RowDataset.this.set(rowIndex, columnIndex, value);
        }

        @Override
        public String toString() {
            try {
                checkValidity();
            } catch (final ConcurrentModificationException e) {
                // Logging an invalidated accessor must not read a different row or throw.
                return "Row[" + rowIndex + "]=<invalidated>";
            }
            return "Row[" + rowIndex + "]=" + N.toString(toArray());
        }
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public ImmutableList<Object> getRow(final int rowIndex) throws ConcurrentModificationException, IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return new ImmutableList<>(new AbstractList<>() {
            @Override
            public Object get(final int columnIndex) {
                return _columnList.get(columnIndex).get(rowIndex);
            }

            @Override
            public int size() {
                return _columnList.size();
            }
        }, true);
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> T getRow(final int rowIndex, final Class<? extends T> rowType) throws ConcurrentModificationException, IndexOutOfBoundsException,
            IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return getRow(rowIndex, _columnNameList, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> T getRow(final int rowIndex, final Collection<String> columnNames, final Class<? extends T> rowType)
            throws ConcurrentModificationException, IndexOutOfBoundsException, IllegalArgumentException, ArrayStoreException, NullPointerException,
            ClassCastException, UnsupportedOperationException, RuntimeException {
        return getRow(rowIndex, columnNames, rowType, null);
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> T getRow(final int rowIndex, final IntFunction<? extends T> rowSupplier) throws ConcurrentModificationException, IndexOutOfBoundsException,
            IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return getRow(rowIndex, _columnNameList, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> T getRow(final int rowIndex, final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier)
            throws ConcurrentModificationException, IndexOutOfBoundsException, IllegalArgumentException, ArrayStoreException, NullPointerException,
            ClassCastException, UnsupportedOperationException, RuntimeException {
        checkRowIndex(rowIndex);
        checkColumnNames(columnNames);
        N.checkArgNotNull(rowSupplier, cs.rowSupplier);

        return getRow(rowIndex, columnNames, null, rowSupplier);
    }

    private <T> T getRow(final int rowIndex, final Collection<String> columnNames, Class<? extends T> rowClass, IntFunction<? extends T> rowSupplier) {
        checkRowIndex(rowIndex);

        final int[] columnIndexes = checkColumnNames(columnNames);
        final int columnCount = columnIndexes.length;

        if (rowClass != null) {
            checkSupportedRowType(rowClass, cs.rowType);
        }

        return getRow(rowIndex, columnIndexes, columnCount, null, null, rowClass, null, rowSupplier);
    }

    /** Assigns one cell using the property's conversion type, preserving already-assignable values. */
    private static void setConvertedPropValue(final PropInfo propInfo, final Object bean, final Object value) {
        // An Object column can mix runtime types. Checking only its first value makes conversion depend
        // on row order; immutable beans/records store raw constructor arguments and cannot repair that later.
        propInfo.setPropValue(bean,
                value == null ? propInfo.jsonXmlType.defaultValue() : propInfo.clazz.isInstance(value) ? value : N.convert(value, propInfo.jsonXmlType));
    }

    private <T> T getRow(final int rowIndex, final int[] columnIndexes, final int columnCount, final Map<String, String> prefixAndFieldNameMap,
            BeanInfo beanInfo, Class<? extends T> rowClass, Type<T> rowType, IntFunction<? extends T> rowSupplier) {

        Object rowOutput = null;

        if (rowClass == null && rowSupplier != null) {
            rowOutput = checkSupplierResult(rowSupplier.apply(columnCount), "rowSupplier");
            rowClass = (Class<T>) rowOutput.getClass();
            rowType = Type.of(rowClass);
        }

        if (rowType == null && rowClass != null) {
            rowType = Type.of(rowClass);
        }

        if (rowType == null) {
            throw new IllegalArgumentException("Row type cannot be determined: either rowClass or rowSupplier must be non-null");
        }

        if (rowSupplier == null && !rowType.isBean()) {
            rowSupplier = this.createRowSupplier(rowClass, rowType);
            rowOutput = checkSupplierResult(rowSupplier.apply(columnCount), "rowSupplier");
        }

        if (beanInfo == null && rowType.isBean()) {
            beanInfo = ParserUtil.getBeanInfo(rowClass);
        }

        if (rowOutput == null && rowSupplier != null) {
            rowOutput = checkSupplierResult(rowSupplier.apply(columnCount), "rowSupplier");
        }

        if (rowType.isObjectArray()) {
            final Object[] result = checkObjectArrayCapacity((Object[]) rowOutput, columnCount, "rowSupplier");

            for (int i = 0; i < columnCount; i++) {
                result[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            return (T) result;
        } else if (rowType.isCollection()) {
            final Collection<Object> result = (Collection<Object>) rowOutput;

            for (final int columnIndex : columnIndexes) {
                result.add(_columnList.get(columnIndex).get(rowIndex));
            }

            return (T) result;
        } else if (rowType.isMap()) {
            final Map<String, Object> result = (Map<String, Object>) rowOutput;

            for (final int columnIndex : columnIndexes) {
                result.put(_columnNameList.get(columnIndex), _columnList.get(columnIndex).get(rowIndex));
            }

            return (T) result;
        } else if (rowType.isBean()) {
            final boolean ignoreUnmatchedProperty = missingPropertyPolicy == MissingPropertyPolicy.IGNORE;
            Object result = rowOutput == null ? beanInfo.createBeanResult() : rowOutput;

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
                    setConvertedPropValue(propInfo, result, _columnList.get(columnIndexes[i]).get(rowIndex));
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

                    final Type<Object> propBeanType = propInfo.type.isCollection() ? (Type<Object>) propInfo.type.elementType() : propInfo.type;

                    if (!propBeanType.isBean()) {
                        throw new UnsupportedOperationException("Property: " + propInfo.name + " in class: " + rowClass + " is not a bean type");
                    }

                    final Class<Object> propBeanClass = propBeanType.javaType();
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

                    final RowDataset tmp = new RowDataset(newTmpColumnNameList, newTmpColumnList, null, true);
                    tmp.missingPropertyPolicy = missingPropertyPolicy;

                    final Object propValue = tmp.getRow(rowIndex, tmp.checkColumnNames(newTmpColumnNameList), newTmpColumnNameList.size(),
                            prefixAndFieldNameMap, propBeanInfo, propBeanClass, propBeanType, null);

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
                    "Unsupported row type: " + rowType.javaType().getCanonicalName() + ". Only Array, Collection, Map and bean class are supported");
        }
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     */
    @Override
    public Optional<Object[]> firstRow() throws ConcurrentModificationException {
        return firstRow(Object[].class);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> Optional<T> firstRow(final Class<? extends T> rowType) throws IllegalArgumentException, ConcurrentModificationException, ArrayStoreException,
            NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return firstRow(_columnNameList, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> Optional<T> firstRow(final Collection<String> columnNames, final Class<? extends T> rowType) throws IllegalArgumentException,
            ConcurrentModificationException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        checkColumnNames(columnNames);
        checkSupportedRowType(rowType, cs.rowType);

        return size() == 0 ? (Optional<T>) Optional.empty() : Optional.of(getRow(0, columnNames, rowType));
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> Optional<T> firstRow(final IntFunction<? extends T> rowSupplier) throws IllegalArgumentException, ConcurrentModificationException,
            ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return firstRow(_columnNameList, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> Optional<T> firstRow(final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier) throws IllegalArgumentException,
            ConcurrentModificationException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        checkColumnNames(columnNames);
        N.checkArgNotNull(rowSupplier, cs.rowSupplier);

        if (size() == 0) {
            return Optional.empty();
        }

        final T row = getRow(0, columnNames, rowSupplier);

        return Optional.of(row);
    }

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     */
    @Override
    public Optional<Object[]> lastRow() throws ConcurrentModificationException {
        return lastRow(Object[].class);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> Optional<T> lastRow(final Class<? extends T> rowType) throws IllegalArgumentException, ConcurrentModificationException, ArrayStoreException,
            NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return lastRow(_columnNameList, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> Optional<T> lastRow(final Collection<String> columnNames, final Class<? extends T> rowType) throws IllegalArgumentException,
            ConcurrentModificationException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        checkColumnNames(columnNames);
        checkSupportedRowType(rowType, cs.rowType);

        return size() == 0 ? (Optional<T>) Optional.empty() : Optional.of(getRow(size() - 1, columnNames, rowType));
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> Optional<T> lastRow(final IntFunction<? extends T> rowSupplier) throws IllegalArgumentException, ConcurrentModificationException,
            ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return lastRow(_columnNameList, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> Optional<T> lastRow(final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier) throws IllegalArgumentException,
            ConcurrentModificationException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        checkColumnNames(columnNames);
        N.checkArgNotNull(rowSupplier, cs.rowSupplier);

        if (size() == 0) {
            return Optional.empty();
        }

        final T row = getRow(size() - 1, columnNames, rowSupplier);

        return Optional.of(row);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <A, B> BiIterator<A, B> iterator(final String columnNameA, final String columnNameB) throws IllegalArgumentException {
        return iterator(0, size(), columnNameA, columnNameB);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <A, B> BiIterator<A, B> iterator(final int fromRowIndex, final int toRowIndex, final String columnNameA, final String columnNameB)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);

        final List<Object> columnA = _columnList.get(checkColumnName(columnNameA));
        final List<Object> columnB = _columnList.get(checkColumnName(columnNameB));

        return new BiIterator<>() {
            private final int expectedModCount = modCount;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                checkModification(expectedModCount);
                return cursor < toRowIndex;
            }

            /**
             * {@inheritDoc}
             * @throws ConcurrentModificationException if the dataset or an ancestor slice has been structurally modified
             * @throws NoSuchElementException if this iterator has no remaining element
             */
            @Override
            public Pair<A, B> next() throws ConcurrentModificationException, NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }
                final A a = (A) columnA.get(cursor);
                final B b = (B) columnB.get(cursor);
                cursor++;
                return Pair.of(a, b);
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}
             * @throws ConcurrentModificationException if the dataset or an ancestor slice has been structurally modified
             * @throws NoSuchElementException if no row remains
             * @throws E if {@code action} throws while consuming the next row
             */
            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action)
                    throws IllegalArgumentException, ConcurrentModificationException, NoSuchElementException, E {
                N.checkArgNotNull(action, cs.action);
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }
                final A a = (A) columnA.get(cursor);
                final B b = (B) columnB.get(cursor);
                cursor++;
                action.accept(a, b);
            }
        };
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <A, B, C> TriIterator<A, B, C> iterator(final String columnNameA, final String columnNameB, final String columnNameC)
            throws IllegalArgumentException {
        return iterator(0, size(), columnNameA, columnNameB, columnNameC);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <A, B, C> TriIterator<A, B, C> iterator(final int fromRowIndex, final int toRowIndex, final String columnNameA, final String columnNameB,
            final String columnNameC) throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);

        final List<Object> columnA = _columnList.get(checkColumnName(columnNameA));
        final List<Object> columnB = _columnList.get(checkColumnName(columnNameB));
        final List<Object> columnC = _columnList.get(checkColumnName(columnNameC));

        return new TriIterator<>() {
            private final int expectedModCount = modCount;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                checkModification(expectedModCount);
                return cursor < toRowIndex;
            }

            /**
             * {@inheritDoc}
             * @throws ConcurrentModificationException if the dataset or an ancestor slice has been structurally modified
             * @throws NoSuchElementException if this iterator has no remaining element
             */
            @Override
            public Triple<A, B, C> next() throws ConcurrentModificationException, NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }
                final A a = (A) columnA.get(cursor);
                final B b = (B) columnB.get(cursor);
                final C c = (C) columnC.get(cursor);
                cursor++;
                return Triple.of(a, b, c);
            }

            /**
             * @throws IllegalArgumentException if {@code action} is {@code null}
             * @throws ConcurrentModificationException if the dataset or an ancestor slice has been structurally modified
             * @throws NoSuchElementException if no row remains
             * @throws E if {@code action} throws while consuming the next row
             */
            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, ConcurrentModificationException, NoSuchElementException, E {
                N.checkArgNotNull(action, cs.action);
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }
                final A a = (A) columnA.get(cursor);
                final B b = (B) columnB.get(cursor);
                final C c = (C) columnC.get(cursor);
                cursor++;
                action.accept(a, b, c);
            }
        };
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws E {@inheritDoc}
     */
    @Override
    public <E extends Exception> void forEach(final Throwables.Consumer<? super DisposableObjArray, E> action) throws IllegalArgumentException, E {
        forEach(_columnNameList, action);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws E {@inheritDoc}
     */
    @Override
    public <E extends Exception> void forEach(final Collection<String> columnNames, final Throwables.Consumer<? super DisposableObjArray, E> action)
            throws IllegalArgumentException, E {
        forEach(0, size(), columnNames, action);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws E {@inheritDoc}
     */
    @Override
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final Throwables.Consumer<? super DisposableObjArray, E> action)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        forEach(fromRowIndex, toRowIndex, _columnNameList, action);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws E {@inheritDoc}
     */
    @Override
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Throwables.Consumer<? super DisposableObjArray, E> action) throws IndexOutOfBoundsException, IllegalArgumentException, E {
        checkForEachRowRange(fromRowIndex, toRowIndex);
        final int[] columnIndexes = checkColumnNames(columnNames);
        N.checkArgNotNull(action, cs.action);

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
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws E {@inheritDoc}
     */
    @Override
    public <E extends Exception> void forEach(final Tuple2<String, String> columnNames, final Throwables.BiConsumer<?, ?, E> action)
            throws IllegalArgumentException, E {
        forEach(0, size(), columnNames, action);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws E {@inheritDoc}
     */
    @Override
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final Tuple2<String, String> columnNames,
            final Throwables.BiConsumer<?, ?, E> action) throws IndexOutOfBoundsException, IllegalArgumentException, E {
        checkForEachRowRange(fromRowIndex, toRowIndex);
        N.checkArgNotNull(columnNames, cs.columnNames);

        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));

        N.checkArgNotNull(action, cs.action);
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

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws E {@inheritDoc}
     */
    @Override
    public <E extends Exception> void forEach(final Tuple3<String, String, String> columnNames, final Throwables.TriConsumer<?, ?, ?, E> action)
            throws IllegalArgumentException, E {
        forEach(0, size(), columnNames, action);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws E {@inheritDoc}
     */
    @Override
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final Tuple3<String, String, String> columnNames,
            final Throwables.TriConsumer<?, ?, ?, E> action) throws IndexOutOfBoundsException, IllegalArgumentException, E {
        checkForEachRowRange(fromRowIndex, toRowIndex);
        N.checkArgNotNull(columnNames, cs.columnNames);

        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(columnNames._3));

        N.checkArgNotNull(action, cs.action);
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

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public List<Object[]> toList(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        return toList(fromRowIndex, toRowIndex, Object[].class);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final Class<? extends T> rowType)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toList(0, size(), rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Class<? extends T> rowType) throws IndexOutOfBoundsException,
            IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toList(fromRowIndex, toRowIndex, _columnNameList, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final Collection<String> columnNames, final Class<? extends T> rowType)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toList(0, size(), columnNames, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Class<? extends T> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException,
            UnsupportedOperationException, RuntimeException {
        return toList(fromRowIndex, toRowIndex, columnNames, null, rowType, null);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final IntFunction<? extends T> rowSupplier)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toList(_columnNameList, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException,
            IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toList(fromRowIndex, toRowIndex, _columnNameList, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toList(0, size(), columnNames, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier)
            throws IndexOutOfBoundsException, IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException,
            UnsupportedOperationException, RuntimeException {
        checkRowIndex(fromRowIndex, toRowIndex);
        checkColumnNames(columnNames);
        N.checkArgNotNull(rowSupplier, cs.rowSupplier);

        return toList(fromRowIndex, toRowIndex, columnNames, null, null, rowSupplier);
    }

    private <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Map<String, String> prefixAndFieldNameMap, Class<? extends T> rowClass, IntFunction<? extends T> rowSupplier) {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int[] columnIndexes = checkColumnNames(columnNames);
        final int columnCount = columnIndexes.length;

        if (rowClass != null) {
            checkSupportedRowType(rowClass, cs.rowType);
        }

        if (rowClass == null && rowSupplier != null) {
            final T temp = checkSupplierResult(rowSupplier.apply(columnCount), "rowSupplier");
            rowClass = (Class<T>) temp.getClass();
            rowSupplier = reuseFirstSuppliedRow(temp, rowSupplier);
        }
        final Type<?> rowType = Type.of(rowClass);

        if (rowType.isBean()) {
            return toEntities(ParserUtil.getBeanInfo(rowClass), fromRowIndex, toRowIndex, null, columnNames, prefixAndFieldNameMap, false, true, rowClass,
                    rowSupplier);
        }

        // rowType.isBean() is always false here: the bean case already returned above.
        rowSupplier = rowSupplier == null ? this.createRowSupplier(rowClass, rowType) : rowSupplier;

        final int rowCount = toRowIndex - fromRowIndex;

        final List<Object> rowList = new ArrayList<>(rowCount);

        if (rowType.isObjectArray()) {
            Object[] row = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                //noinspection DataFlowIssue
                row = checkObjectArrayCapacity((Object[]) checkSupplierResult(rowSupplier.apply(columnCount), "rowSupplier"), columnCount, "rowSupplier");

                for (int i = 0; i < columnCount; i++) {
                    row[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
                }

                rowList.add(row);
            }
        } else if (rowType.isCollection()) {
            Collection<Object> row = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                //noinspection DataFlowIssue
                row = (Collection<Object>) checkSupplierResult(rowSupplier.apply(columnCount), "rowSupplier");

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
                row = (Map<String, Object>) checkSupplierResult(rowSupplier.apply(columnCount), "rowSupplier");

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
            return (IntFunction<T>) IntFunctions.ofCollection((Class<Collection>) rowClass);

        } else if (rowType.isMap()) {
            return (IntFunction<T>) IntFunctions.ofMap((Class<Map>) rowClass);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + ClassUtil.getCanonicalClassName(rowClass) + ". Only Array, List/Set, Map and bean class are supported");
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final Predicate<? super String> columnNameFilter, final Function<? super String, String> columnNameConverter,
            final Class<? extends T> rowType)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        N.checkArgNotNull(columnNameFilter, cs.columnNameFilter);
        N.checkArgNotNull(columnNameConverter, cs.columnNameConverter);

        return toList(0, size(), columnNameFilter, columnNameConverter, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Predicate<? super String> columnNameFilter,
            final Function<? super String, String> columnNameConverter, final Class<? extends T> rowType) throws IndexOutOfBoundsException,
            IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(columnNameFilter, cs.columnNameFilter);
        N.checkArgNotNull(columnNameConverter, cs.columnNameConverter);

        checkSupportedRowType(rowType, cs.rowType);
        if (Objects.equals(columnNameFilter, Fn.alwaysTrue()) && Objects.equals(columnNameConverter, Fn.identity())) {
            return toList(fromRowIndex, toRowIndex, _columnNameList, rowType);
        }

        final List<String> newColumnNameList = new ArrayList<>();
        final List<List<Object>> newColumnList = new ArrayList<>();
        String columnName = null;

        for (int i = 0, columnCount = columnCount(); i < columnCount; i++) {
            columnName = _columnNameList.get(i);

            if (columnNameFilter.test(columnName)) {
                newColumnNameList.add(columnNameConverter.apply(columnName));
                newColumnList.add(_columnList.get(i));
            }
        }

        // A derived selection that came out empty is the same mistake as passing an explicit empty selection,
        // which checkColumnNames rejects. Without this the zero-column scratch Dataset below re-validated the
        // (valid) row range against its own size and reported it as out of bounds for length 0.
        if (newColumnNameList.isEmpty() && columnCount() > 0) {
            throw new IllegalArgumentException("columnNameFilter matched none of the columns: " + _columnNameList);
        }

        final RowDataset tmp = new RowDataset(newColumnNameList, newColumnList, null, true);
        tmp.missingPropertyPolicy = missingPropertyPolicy;

        return tmp.toList(fromRowIndex, toRowIndex, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final Predicate<? super String> columnNameFilter, final Function<? super String, String> columnNameConverter,
            final IntFunction<? extends T> rowSupplier)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        N.checkArgNotNull(columnNameFilter, cs.columnNameFilter);
        N.checkArgNotNull(columnNameConverter, cs.columnNameConverter);
        N.checkArgNotNull(rowSupplier, cs.rowSupplier);

        return toList(0, size(), columnNameFilter, columnNameConverter, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toList(final int fromRowIndex, final int toRowIndex, final Predicate<? super String> columnNameFilter,
            final Function<? super String, String> columnNameConverter, final IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException,
            IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(columnNameFilter, cs.columnNameFilter);
        N.checkArgNotNull(columnNameConverter, cs.columnNameConverter);
        N.checkArgNotNull(rowSupplier, cs.rowSupplier);

        if (Objects.equals(columnNameFilter, Fn.alwaysTrue()) && Objects.equals(columnNameConverter, Fn.identity())) {
            return toList(fromRowIndex, toRowIndex, _columnNameList, rowSupplier);
        }

        final List<String> newColumnNameList = new ArrayList<>();
        final List<List<Object>> newColumnList = new ArrayList<>();
        String columnName = null;

        for (int i = 0, columnCount = columnCount(); i < columnCount; i++) {
            columnName = _columnNameList.get(i);

            if (columnNameFilter.test(columnName)) {
                newColumnNameList.add(columnNameConverter.apply(columnName));
                newColumnList.add(_columnList.get(i));
            }
        }

        // A derived selection that came out empty is the same mistake as passing an explicit empty selection,
        // which checkColumnNames rejects. Without this the zero-column scratch Dataset below re-validated the
        // (valid) row range against its own size and reported it as out of bounds for length 0.
        if (newColumnNameList.isEmpty() && columnCount() > 0) {
            throw new IllegalArgumentException("columnNameFilter matched none of the columns: " + _columnNameList);
        }

        final RowDataset tmp = new RowDataset(newColumnNameList, newColumnList, null, true);
        tmp.missingPropertyPolicy = missingPropertyPolicy;

        return tmp.toList(fromRowIndex, toRowIndex, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toEntities(final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType)
            throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        return toEntities(0, size(), _columnNameList, prefixAndFieldNameMap, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toEntities(final int fromRowIndex, final int toRowIndex, final Map<String, String> prefixAndFieldNameMap,
            final Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        return toEntities(fromRowIndex, toRowIndex, _columnNameList, prefixAndFieldNameMap, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toEntities(final Collection<String> columnNames, final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType)
            throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        return toEntities(0, size(), columnNames, prefixAndFieldNameMap, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <T> List<T> toEntities(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType)
            throws IllegalArgumentException, IndexOutOfBoundsException, UnsupportedOperationException, RuntimeException {
        N.checkArgument(Beans.isBeanClass(rowType), "{} is not a bean class", rowType);

        return toList(fromRowIndex, toRowIndex, columnNames, prefixAndFieldNameMap, rowType, null);
    }

    @Override
    public <T> List<T> toMergedEntities(final Class<? extends T> rowType) throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        return toMergedEntities(_columnNameList, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final Collection<String> selectPropNames, final Class<? extends T> rowType)
            throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        N.checkArgument(Beans.isBeanClass(rowType), "{} is not a bean class", rowType);

        return toMergedEntities(ParserUtil.getBeanInfo(rowType).idPropNameList, selectPropNames, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType)
            throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        N.checkArgument(Beans.isBeanClass(rowType), "{} is not a bean class", rowType);

        return toMergedEntities(ParserUtil.getBeanInfo(rowType).idPropNameList, _columnNameList, prefixAndFieldNameMap, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final String idPropName, final Class<? extends T> rowType)
            throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        return toMergedEntities(idPropName, _columnNameList, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final String idPropName, final Collection<String> selectPropNames, final Class<? extends T> rowType)
            throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        return toMergedEntities(N.asList(idPropName), selectPropNames, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final String idPropName, final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType)
            throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        return toMergedEntities(N.asList(idPropName), _columnNameList, prefixAndFieldNameMap, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final Collection<String> idPropNames, final Collection<String> selectPropNames, final Class<? extends T> rowType)
            throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        return toMergedEntities(idPropNames, selectPropNames, null, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final Collection<String> idPropNames, final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType)
            throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        return toMergedEntities(idPropNames, _columnNameList, prefixAndFieldNameMap, rowType);
    }

    @Override
    public <T> List<T> toMergedEntities(final Collection<String> idPropNames, final Collection<String> selectPropNames,
            final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType)
            throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        N.checkArgument(Beans.isBeanClass(rowType), "{} is not a bean class", rowType);
        N.checkArgNotEmpty(idPropNames, "idPropNames cannot be null or empty. No id property defined in bean class: " + rowType);

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
        N.checkArgument(_columnNameList.containsAll(idPropNamesToUse), "Some id properties {} are not found in Dataset: {} for bean {}", idPropNamesToUse,
                _columnNameList, ClassUtil.getSimpleClassName(rowType));

        return toEntities(beanInfo, 0, size(), idPropNamesToUse, selectPropNames, prefixAndFieldNameMap, true, false, rowType, null);
    }

    /**
     * @throws ConcurrentModificationException if this dataset is an invalidated slice
     * @throws IndexOutOfBoundsException if the requested row range is outside this dataset
     * @throws IllegalArgumentException if merging is requested without ID properties, a selected column is absent or
     *         duplicated, {@code rowType} is null, or a selected property is absent from the row type
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     */
    private <T> List<T> toEntities(final BeanInfo beanInfo, final int fromRowIndex, final int toRowIndex, final Collection<String> idPropNames,
            final Collection<String> columnNames, final Map<String, String> prefixAndFieldNameMap, final boolean mergeResult, final boolean returnAllList,
            final Class<? extends T> rowType, final IntFunction<? extends T> rowSupplier)
            throws ConcurrentModificationException, IndexOutOfBoundsException, IllegalArgumentException, UnsupportedOperationException {
        return toEntities(beanInfo, fromRowIndex, toRowIndex, idPropNames, columnNames, prefixAndFieldNameMap, mergeResult, returnAllList, rowType, rowSupplier,
                null);
    }

    /**
     * @throws ConcurrentModificationException if this dataset is an invalidated slice
     * @throws IndexOutOfBoundsException if the requested row range is outside this dataset
     * @throws IllegalArgumentException if merging is requested without ID properties, a selected column is absent or
     *         duplicated, {@code rowType} is null, or a selected property is absent from the row type
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     */
    @SuppressWarnings("rawtypes")
    private <T> List<T> toEntities(final BeanInfo beanInfo, final int fromRowIndex, final int toRowIndex, final Collection<String> idPropNames,
            final Collection<String> columnNames, final Map<String, String> prefixAndFieldNameMap, final boolean mergeResult, final boolean returnAllList,
            final Class<? extends T> rowType, final IntFunction<? extends T> rowSupplier, final Object[] parentEntities)
            throws ConcurrentModificationException, IndexOutOfBoundsException, IllegalArgumentException, UnsupportedOperationException {
        checkRowIndex(fromRowIndex, toRowIndex);

        if (mergeResult && N.isEmpty(idPropNames)) {
            throw new IllegalArgumentException("'idPropNames' cannot be null or empty when 'mergeResult' is true");
        }

        final int[] idColumnIndexes = N.isEmpty(idPropNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(idPropNames);
        // An empty selection is valid only when this Dataset has no columns.
        checkColumnNames(columnNames);
        N.checkArgNotNull(rowType, cs.rowType);

        final int rowCount = toRowIndex - fromRowIndex;
        final int columnCount = columnNames.size();
        final boolean ignoreUnmatchedProperty = missingPropertyPolicy == MissingPropertyPolicy.IGNORE;

        final Object[] resultEntities = new Object[rowCount];
        final Map<Object, Object> idBeanMap = mergeResult ? N.newLinkedHashMap(N.min(64, rowCount)) : N.emptyMap();
        final Map<Object, Map<Object, Object>> beansByParent = parentEntities == null ? null : new IdentityHashMap<>();

        // Nested IDs are local to a parent. A populated child with no ID receives a unique identity
        // for that row; it must neither disappear nor disable merging for any other child/parent.
        // Composite IDs are compared as complete tuples, including null/default components.
        for (int rowIndex = fromRowIndex, i = 0; rowIndex < toRowIndex; rowIndex++, i++) {
            if (parentEntities != null && parentEntities[i] == null) {
                continue;
            }
            if (idColumnIndexes.length == 0) {
                resultEntities[i] = rowSupplier == null ? beanInfo.createBeanResult() : checkSupplierResult(rowSupplier.apply(columnCount), "rowSupplier");
                continue;
            }
            Object key;
            boolean allNull;
            if (idColumnIndexes.length == 1) {
                final Object id = _columnList.get(idColumnIndexes[0]).get(rowIndex);
                allNull = id == null;
                key = hashKey(id);
            } else {
                final Object[] idValues = new Object[idColumnIndexes.length];
                allNull = true;
                for (int j = 0; j < idColumnIndexes.length; j++) {
                    idValues[j] = _columnList.get(idColumnIndexes[j]).get(rowIndex);
                    allNull &= idValues[j] == null;
                }
                key = Wrapper.of(idValues);
            }
            if (allNull) {
                if (parentEntities == null) {
                    continue;
                }
                key = new Object();
            }
            final Map<Object, Object> identities = parentEntities == null ? idBeanMap
                    : beansByParent.computeIfAbsent(parentEntities[i], unused -> new LinkedHashMap<>());
            Object bean = identities.get(key);
            if (bean == null) {
                bean = rowSupplier == null ? beanInfo.createBeanResult() : checkSupplierResult(rowSupplier.apply(columnCount), "rowSupplier");
                identities.put(key, bean);
            }
            resultEntities[i] = bean;
        }

        Map<Collection<Object>, Set<Object>> mergedValuesByCollection = null;

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
                for (int rowIndex = fromRowIndex, i = 0; rowIndex < toRowIndex; rowIndex++, i++) {
                    if (resultEntities[i] != null) {
                        setConvertedPropValue(propInfo, resultEntities[i], curColumn.get(rowIndex));
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

                final Type<?> propBeanType = propInfo.type.isCollection() ? propInfo.type.elementType() : propInfo.type;

                if (!propBeanType.isBean()) {
                    throw new UnsupportedOperationException("Property: " + propInfo.name + " in class: " + rowType + " is not a bean type");
                }

                final Class<?> propBeanClass = propBeanType.javaType();
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

                final RowDataset tmp = new RowDataset(newTmpColumnNameList, newTmpColumnList, null, true);
                tmp.missingPropertyPolicy = missingPropertyPolicy;

                final boolean isToMerge = mergeResult && N.notEmpty(newPropEntityIdNames) && tmp._columnNameList.containsAll(newPropEntityIdNames);
                final boolean[] allNullNestedRow = mergeResult ? allNullRows(newTmpColumnList, fromRowIndex, toRowIndex) : null;
                final List<?> propValueList = tmp.toEntities(propBeanInfo, fromRowIndex, toRowIndex, isToMerge ? newPropEntityIdNames : null,
                        tmp._columnNameList, prefixAndFieldNameMap, isToMerge, true, propBeanClass, null, mergeResult ? resultEntities : null);

                if (propInfo.type.isCollection()) {
                    Collection<Object> c = null;

                    for (int rowIndex = fromRowIndex, i = 0; rowIndex < toRowIndex; rowIndex++, i++) {
                        if (resultEntities[i] == null || propValueList.get(i) == null || (allNullNestedRow != null && allNullNestedRow[i])) {
                            continue;
                        }

                        c = propInfo.getPropValue(resultEntities[i]);

                        if (c == null) {
                            c = N.newCollection((Class) propInfo.clazz);
                            propInfo.setPropValue(resultEntities[i], c);
                        }

                        if (isToMerge && !(c instanceof Set)) {
                            if (mergedValuesByCollection == null) {
                                mergedValuesByCollection = new IdentityHashMap<>();
                            }

                            final Set<Object> seen = mergedValuesByCollection.computeIfAbsent(c, unused -> Collections.newSetFromMap(new IdentityHashMap<>()));
                            if (!seen.add(propValueList.get(i))) {
                                continue;
                            }
                        }

                        c.add(propValueList.get(i));
                    }
                } else {
                    for (int rowIndex = fromRowIndex, i = 0; rowIndex < toRowIndex; rowIndex++, i++) {
                        if (resultEntities[i] == null || propValueList.get(i) == null || (allNullNestedRow != null && allNullNestedRow[i])) {
                            continue;
                        }

                        propInfo.setPropValue(resultEntities[i], propValueList.get(i));
                    }
                }
            }
        }

        // When merging, only idBeanMap holds the merged entities: rows whose id values are all
        // null are skipped, so falling back to resultEntities would return a list of nulls.
        final List<T> result = returnAllList ? (List<T>) N.toList(resultEntities) : new ArrayList<>((Collection<T>) idBeanMap.values());

        if (rowSupplier == null && N.notEmpty(result)) {
            // Builders must be finished once per identity so repeated rows still refer to the same child.
            final Map<Object, T> finished = new IdentityHashMap<>();
            for (int i = 0, size = result.size(); i < size; i++) {
                final T value = result.get(i);
                if (value != null) {
                    result.set(i, finished.computeIfAbsent(value, key -> beanInfo.finishBeanResult(key)));
                }
            }
        }

        return result;
    }

    /**
     * Marks, for each row of {@code [fromRowIndex, toRowIndex)}, whether every one of {@code columns} holds
     * {@code null} there. Index {@code i} of the result corresponds to row {@code fromRowIndex + i}.
     *
     * @param columns the columns to inspect, all of this dataset's row count
     * @param fromRowIndex the inclusive start row
     * @param toRowIndex the exclusive end row
     * @return one flag per row of the range
     */
    private static boolean[] allNullRows(final List<List<Object>> columns, final int fromRowIndex, final int toRowIndex) {
        final boolean[] result = new boolean[toRowIndex - fromRowIndex];

        for (int rowIndex = fromRowIndex, i = 0; rowIndex < toRowIndex; rowIndex++, i++) {
            boolean allNull = true;

            for (final List<Object> column : columns) {
                if (column.get(rowIndex) != null) {
                    allNull = false;
                    break;
                }
            }

            result[i] = allNull;
        }

        return result;
    }

    private PropInfo getPropInfoByPrefix(final BeanInfo beanInfo, final String prefix, final Map<String, String> prefixAndFieldNameMap) {
        PropInfo propInfo = beanInfo.getPropInfo(prefix);

        if (propInfo == null && N.notEmpty(prefixAndFieldNameMap) && prefixAndFieldNameMap.containsKey(prefix)) {
            propInfo = beanInfo.getPropInfo(prefixAndFieldNameMap.get(prefix));
        }

        if (propInfo == null) {
            propInfo = Stream.of(beanInfo.propInfoList)
                    .filter(it -> it.tablePrefix.isPresent() && it.tablePrefix.orElseThrow().equals(prefix))
                    .onlyOne()
                    .orElse(null);
        }

        if (propInfo == null) {
            propInfo = beanInfo.getPropInfo(prefix + "s"); // Trying to do something smart?
            final int len = prefix.length() + 1;

            if (propInfo != null && (propInfo.type.isBean() || (propInfo.type.isCollection() && propInfo.type.elementType().isBean()))
                    && N.noneMatch(_columnNameList, it -> it.length() > len && it.charAt(len) == '.' && Strings.startsWithIgnoreCase(it, prefix + "s."))) {
                // good
            } else {
                propInfo = beanInfo.getPropInfo(prefix + "es"); // Trying to do something smart?
                final int len2 = prefix.length() + 2;

                if (propInfo != null && (propInfo.type.isBean() || (propInfo.type.isCollection() && propInfo.type.elementType().isBean())) && N
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

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <K, V> Map<K, V> toMap(final String keyColumnName, final String valueColumnName) throws IllegalArgumentException {
        return toMap(0, size(), keyColumnName, valueColumnName);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if the supplied result map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(final String keyColumnName, final String valueColumnName, final IntFunction<? extends M> supplier)
            throws IllegalArgumentException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMap(0, size(), keyColumnName, valueColumnName, supplier);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <K, V> Map<K, V> toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final String valueColumnName)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return toMap(fromRowIndex, toRowIndex, keyColumnName, valueColumnName, N::newLinkedHashMap);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if the supplied result map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final String valueColumnName,
            final IntFunction<? extends M> supplier) throws IndexOutOfBoundsException, IllegalArgumentException, NullPointerException, ClassCastException,
            UnsupportedOperationException, RuntimeException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int valueColumnIndex = checkColumnName(valueColumnName);
        N.checkArgNotNull(supplier, cs.supplier);

        final M resultMap = checkSupplierResult(supplier.apply(toRowIndex - fromRowIndex), "supplier");

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (V) _columnList.get(valueColumnIndex).get(rowIndex));
        }

        return resultMap;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, V> Map<K, V> toMap(final String keyColumnName, final Collection<String> valueColumnNames, final Class<? extends V> rowType)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(final String keyColumnName, final Collection<String> valueColumnNames, final Class<? extends V> rowType,
            final IntFunction<? extends M> supplier)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowType, supplier);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, V> Map<K, V> toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final Collection<String> valueColumnNames,
            final Class<? extends V> rowType) throws IndexOutOfBoundsException, IllegalArgumentException, ArrayStoreException, NullPointerException,
            ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowType, N::newLinkedHashMap);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public <K, V, M extends Map<K, V>> M toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final Class<? extends V> rowType, final IntFunction<? extends M> supplier)
            throws IndexOutOfBoundsException, IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException,
            UnsupportedOperationException, RuntimeException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnNames(valueColumnNames);
        checkSupportedRowType(rowType, cs.rowType);
        N.checkArgNotNull(supplier, cs.supplier);

        final Type<?> valueType = Type.of(rowType);
        final int valueColumnCount = valueColumnIndexes.length;
        final Map<Object, Object> resultMap = (Map<Object, Object>) checkSupplierResult(supplier.apply(toRowIndex - fromRowIndex), "supplier");

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
            final IntFunction<? extends Collection<Object>> valueSupplier = (IntFunction<? extends Collection<Object>>) (IntFunction<?>) IntFunctions
                    .ofCollection((Class<? extends Collection>) rowType);
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = valueSupplier.apply(valueColumnCount);

                for (final int columnIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columnIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isMap()) {
            final IntFunction<? extends Map<String, Object>> valueSupplier = (IntFunction<? extends Map<String, Object>>) (IntFunction<?>) IntFunctions
                    .ofMap((Class<? extends Map>) rowType);
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = valueSupplier.apply(valueColumnCount);

                for (final int columnIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columnIndex), _columnList.get(columnIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowType);

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                final Object value = getRow(rowIndex, valueColumnIndexes, valueColumnCount, null, beanInfo, rowType, (Type) valueType, null);
                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowType.getCanonicalName() + ". Only Array, List/Set, Map and bean class are supported");
        }

        return (M) resultMap;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, V> Map<K, V> toMap(final String keyColumnName, final Collection<String> valueColumnNames, final IntFunction<? extends V> rowSupplier)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(final String keyColumnName, final Collection<String> valueColumnNames,
            final IntFunction<? extends V> rowSupplier, final IntFunction<? extends M> supplier)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMap(0, size(), keyColumnName, valueColumnNames, rowSupplier, supplier);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, V> Map<K, V> toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final Collection<String> valueColumnNames,
            final IntFunction<? extends V> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException, ArrayStoreException, NullPointerException,
            ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowSupplier, N::newLinkedHashMap);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, V, M extends Map<K, V>> M toMap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final IntFunction<? extends V> rowSupplier, final IntFunction<? extends M> supplier)
            throws IndexOutOfBoundsException, IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException,
            UnsupportedOperationException, RuntimeException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnNames(valueColumnNames);
        N.checkArgNotNull(rowSupplier, cs.rowSupplier);
        N.checkArgNotNull(supplier, cs.supplier);

        final int valueColumnCount = valueColumnIndexes.length;
        final V firstRow = checkSupplierResult(rowSupplier.apply(valueColumnCount), "rowSupplier");

        final Class<V> rowClass = (Class<V>) firstRow.getClass();
        final Type<?> valueType = Type.of(rowClass);
        final IntFunction<? extends V> rowSupplierToUse = reuseFirstSuppliedRow(firstRow, rowSupplier);
        final Map<Object, Object> resultMap = (Map<Object, Object>) checkSupplierResult(supplier.apply(toRowIndex - fromRowIndex), "supplier");

        if (valueType.isObjectArray()) {
            Object[] value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = checkObjectArrayCapacity((Object[]) rowSupplierToUse.apply(valueColumnCount), valueColumnCount, "rowSupplier");

                for (int i = 0; i < valueColumnCount; i++) {
                    value[i] = _columnList.get(valueColumnIndexes[i]).get(rowIndex);
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isCollection()) {
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Collection<Object>) rowSupplierToUse.apply(valueColumnCount);

                for (final int columnIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columnIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isMap()) {
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Map<String, Object>) rowSupplierToUse.apply(valueColumnCount);

                for (final int columnIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columnIndex), _columnList.get(columnIndex).get(rowIndex));
                }

                resultMap.put(_columnList.get(keyColumnIndex).get(rowIndex), value);
            }
        } else if (valueType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowClass);
            final boolean ignoreUnmatchedProperty = missingPropertyPolicy == MissingPropertyPolicy.IGNORE;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                final Object value = rowSupplierToUse.apply(valueColumnCount);
                for (final int columnIndex : valueColumnIndexes) {
                    final String propName = _columnNameList.get(columnIndex);
                    final PropInfo propInfo = beanInfo.getPropInfo(propName);
                    if (propInfo == null) {
                        beanInfo.setPropValue(value, propName, _columnList.get(columnIndex).get(rowIndex), ignoreUnmatchedProperty);
                    } else {
                        setConvertedPropValue(propInfo, value, _columnList.get(columnIndex).get(rowIndex));
                    }
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
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final String keyColumnName, final String valueColumnName) throws IllegalArgumentException {
        return toMultimap(0, size(), keyColumnName, valueColumnName);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if the supplied result map or value collection does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final String keyColumnName, final String valueColumnName,
            final IntFunction<? extends M> supplier)
            throws IllegalArgumentException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMultimap(0, size(), keyColumnName, valueColumnName, supplier);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName, final String valueColumnName)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return toMultimap(fromRowIndex, toRowIndex, keyColumnName, valueColumnName, len -> N.newLinkedListMultimap());
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if the supplied result map or value collection does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final String valueColumnName, final IntFunction<? extends M> supplier) throws IndexOutOfBoundsException, IllegalArgumentException,
            NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int valueColumnIndex = checkColumnName(valueColumnName);
        N.checkArgNotNull(supplier, cs.supplier);

        final M resultMap = checkSupplierResult(supplier.apply(toRowIndex - fromRowIndex), "supplier");

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) _columnList.get(valueColumnIndex).get(rowIndex));
        }

        return resultMap;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final String keyColumnName, final Collection<String> valueColumnNames, final Class<? extends T> rowType)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final String keyColumnName, final Collection<String> valueColumnNames,
            final Class<? extends T> rowType, final IntFunction<? extends M> supplier)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowType, supplier);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException,
            ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMultimap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowType, len -> N.newLinkedListMultimap());
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final Class<? extends T> rowType, final IntFunction<? extends M> supplier)
            throws IndexOutOfBoundsException, IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException,
            UnsupportedOperationException, RuntimeException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnNames(valueColumnNames);
        checkSupportedRowType(rowType, cs.rowType);
        N.checkArgNotNull(supplier, cs.supplier);

        final Type<?> valueType = Type.of(rowType);
        final int valueColumnCount = valueColumnIndexes.length;

        final M resultMap = checkSupplierResult(supplier.apply(toRowIndex - fromRowIndex), "supplier");

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
            final IntFunction<? extends Collection<Object>> valueSupplier = (IntFunction<? extends Collection<Object>>) (IntFunction<?>) IntFunctions
                    .ofCollection((Class<? extends Collection>) rowType);
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = valueSupplier.apply(valueColumnCount);

                for (final int columnIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columnIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else if (valueType.isMap()) {
            final IntFunction<? extends Map<String, Object>> valueSupplier = (IntFunction<? extends Map<String, Object>>) (IntFunction<?>) IntFunctions
                    .ofMap((Class<? extends Map>) rowType);
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = valueSupplier.apply(valueColumnCount);

                for (final int columnIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columnIndex), _columnList.get(columnIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else if (valueType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowType);

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                final Object value = getRow(rowIndex, valueColumnIndexes, valueColumnCount, null, beanInfo, rowType, (Type) valueType, null);
                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowType.getCanonicalName() + ". Only Array, List/Set, Map and bean class are supported");
        }

        return resultMap;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final String keyColumnName, final Collection<String> valueColumnNames,
            final IntFunction<? extends T> rowSupplier)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final String keyColumnName, final Collection<String> valueColumnNames,
            final IntFunction<? extends T> rowSupplier, final IntFunction<? extends M> supplier)
            throws IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMultimap(0, size(), keyColumnName, valueColumnNames, rowSupplier, supplier);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, T> ListMultimap<K, T> toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final IntFunction<? extends T> rowSupplier) throws IndexOutOfBoundsException, IllegalArgumentException,
            ArrayStoreException, NullPointerException, ClassCastException, UnsupportedOperationException, RuntimeException {
        return toMultimap(fromRowIndex, toRowIndex, keyColumnName, valueColumnNames, rowSupplier, len -> N.newLinkedListMultimap());
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayStoreException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws UnsupportedOperationException if a row bean has no usable builder or constructor, a selected read-only property cannot accept its value,
     *         a selected nested property has a non-bean parent, or a destination collection or map does not support insertion
     * @throws RuntimeException {@inheritDoc}
     */
    @Override
    public <K, T, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final int fromRowIndex, final int toRowIndex, final String keyColumnName,
            final Collection<String> valueColumnNames, final IntFunction<? extends T> rowSupplier, final IntFunction<? extends M> supplier)
            throws IndexOutOfBoundsException, IllegalArgumentException, ArrayStoreException, NullPointerException, ClassCastException,
            UnsupportedOperationException, RuntimeException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int keyColumnIndex = checkColumnName(keyColumnName);
        final int[] valueColumnIndexes = checkColumnNames(valueColumnNames);
        N.checkArgNotNull(rowSupplier, cs.rowSupplier);
        N.checkArgNotNull(supplier, cs.supplier);

        final int valueColumnCount = valueColumnIndexes.length;
        final T firstRow = checkSupplierResult(rowSupplier.apply(valueColumnCount), "rowSupplier");

        final Class<?> rowClass = firstRow.getClass();
        final Type<?> valueType = Type.of(rowClass);
        final IntFunction<? extends T> rowSupplierToUse = reuseFirstSuppliedRow(firstRow, rowSupplier);

        final M resultMap = checkSupplierResult(supplier.apply(toRowIndex - fromRowIndex), "supplier");

        if (valueType.isObjectArray()) {
            Object[] value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = checkObjectArrayCapacity((Object[]) rowSupplierToUse.apply(valueColumnCount), valueColumnCount, "rowSupplier");

                for (int i = 0; i < valueColumnCount; i++) {
                    value[i] = _columnList.get(valueColumnIndexes[i]).get(rowIndex);
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else if (valueType.isCollection()) {
            Collection<Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Collection<Object>) rowSupplierToUse.apply(valueColumnCount);

                for (final int columnIndex : valueColumnIndexes) {
                    value.add(_columnList.get(columnIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else if (valueType.isMap()) {
            Map<String, Object> value = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                value = (Map<String, Object>) rowSupplierToUse.apply(valueColumnCount);

                for (final int columnIndex : valueColumnIndexes) {
                    value.put(_columnNameList.get(columnIndex), _columnList.get(columnIndex).get(rowIndex));
                }

                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else if (valueType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowClass);
            final boolean ignoreUnmatchedProperty = missingPropertyPolicy == MissingPropertyPolicy.IGNORE;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                final Object value = rowSupplierToUse.apply(valueColumnCount);
                for (final int columnIndex : valueColumnIndexes) {
                    final String propName = _columnNameList.get(columnIndex);
                    final PropInfo propInfo = beanInfo.getPropInfo(propName);
                    if (propInfo == null) {
                        beanInfo.setPropValue(value, propName, _columnList.get(columnIndex).get(rowIndex), ignoreUnmatchedProperty);
                    } else {
                        setConvertedPropValue(propInfo, value, _columnList.get(columnIndex).get(rowIndex));
                    }
                }
                resultMap.put((K) _columnList.get(keyColumnIndex).get(rowIndex), (T) value);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported row type: " + rowClass.getCanonicalName() + ". Only Array, List/Set, Map and bean class are supported");
        }

        return resultMap;
    }

    /**
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws UncheckedIOException if creating or writing the temporary file fails, or it cannot atomically replace {@code output}
     */
    private static void writeExportFile(final File output, final Throwables.Consumer<Writer, IOException> action)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(output, cs.output);
        final Path destination = output.toPath().toAbsolutePath();
        Path temporary = null;
        try {
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), ".dataset-", ".tmp");
            // A sibling file keeps validation/serialization failures from truncating a previous export.
            // Require atomic replacement; an unsupported filesystem fails while preserving the destination.
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                action.accept(writer);
            }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (final IOException ignored) {
                    // Cleanup must not hide the serialization/replacement failure.
                }
            }
        }
    }

    private static boolean isSimpleExportValue(final Object value) {
        return value instanceof String || value instanceof Character || value instanceof Boolean || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double
                || value instanceof java.math.BigInteger || value instanceof java.math.BigDecimal;
    }

    private static boolean isXmlCharacter(final int cp) {
        return cp == 9 || cp == 10 || cp == 13 || cp >= 0x20 && cp <= 0xD7FF || cp >= 0xE000 && cp <= 0xFFFD || cp >= 0x10000 && cp <= 0x10FFFF;
    }

    private static IllegalArgumentException invalidExportValue(final boolean json, final int row, final String column) {
        return new IllegalArgumentException("Cannot export " + (json ? "JSON" : "XML") + ": invalid value at row " + row + ", column '" + column + "'");
    }

    private static void checkXmlCharacters(final String text, final boolean serialized, final int row, final String column) {
        boolean literal = false;
        for (int i = 0; i < text.length();) {
            final int cp = text.codePointAt(i);
            if (!isXmlCharacter(cp)) {
                throw invalidExportValue(false, row, column);
            }
            if (serialized) {
                if (text.startsWith("<![CDATA[", i) || text.startsWith("<!--", i)) {
                    literal = true;
                } else if (text.startsWith("]]>", i) || text.startsWith("-->", i)) {
                    literal = false;
                }
                if (!literal && text.startsWith("&#", i)) {
                    final int end = text.indexOf(';', i + 2);
                    final boolean hex = i + 2 < text.length() && text.charAt(i + 2) == 'x';
                    try {
                        if (end < 0 || !isXmlCharacter(Integer.parseInt(text.substring(i + (hex ? 3 : 2), end), hex ? 16 : 10))) {
                            throw invalidExportValue(false, row, column);
                        }
                    } catch (final NumberFormatException e) {
                        throw invalidExportValue(false, row, column);
                    }
                }
            }
            i += Character.charCount(cp);
        }
    }

    private static void checkJsonNumbers(final String text, final int row, final String column) {
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            final char ch = text.charAt(i);
            if (quoted && ch == '\\') {
                i++;
            } else if (ch == '"') {
                quoted = !quoted;
            } else if (!quoted && (text.startsWith("NaN", i) || text.startsWith("Infinity", i))) {
                throw invalidExportValue(true, row, column);
            }
        }
    }

    private static void writeExportValue(final CharacterWriter output, final Object value, final Type<Object> type, final boolean json, final int row,
            final String column) throws IOException {
        if (value == null) {
            output.write(NULL_CHAR_ARRAY);
            return;
        }
        if (isSimpleExportValue(value)) {
            if (json && (value instanceof Double d && !Double.isFinite(d) || value instanceof Float f && !Float.isFinite(f))) {
                throw invalidExportValue(true, row, column);
            }
            if (!json && (value instanceof String || value instanceof Character)) {
                checkXmlCharacters(value.toString(), false, row, column);
            }
            type.serializeTo(output, value, json ? jsc : xsc);
            return;
        }

        CharacterWriter scratch = json ? Objectory.createBufferedJsonWriter() : Objectory.createBufferedXmlWriter();
        try {
            if (type.isSerializable()) {
                type.serializeTo(scratch, value, json ? jsc : xsc);
            } else {
                try {
                    if (json) {
                        jsonParser.serialize(value, jsc, scratch);
                    } else {
                        xmlParser.serialize(value, xsc, scratch);
                    }
                } catch (final Exception e) { // A parser failure retains the documented string fallback.
                    recycleExportWriter(scratch);
                    scratch = json ? Objectory.createBufferedJsonWriter() : Objectory.createBufferedXmlWriter();
                    strType.serializeTo(scratch, N.toString(value), json ? jsc : xsc);
                }
            }
            // Check completed output, including nested arrays/beans and XML numeric references. Validation
            // stays outside the fallback catch: malformed values must fail, not silently become strings.
            final String text = scratch.toString();
            if (json) {
                checkJsonNumbers(text, row, column);
            } else {
                checkXmlCharacters(text, true, row, column);
            }
            output.write(text);
        } finally {
            recycleExportWriter(scratch);
        }
    }

    private static void recycleExportWriter(final CharacterWriter writer) {
        if (writer instanceof BufferedJsonWriter jsonWriter) {
            Objectory.recycle(jsonWriter);
        } else {
            Objectory.recycle((BufferedXmlWriter) writer);
        }
    }

    @Override
    public String toJson() {
        return toJson(0, size());
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public String toJson(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException, IllegalArgumentException {
        return toJson(fromRowIndex, toRowIndex, _columnNameList);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public String toJson(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        if (N.notEmpty(columnNames)) {
            checkColumnNames(columnNames);
        }

        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            toJson(fromRowIndex, toRowIndex, columnNames, writer);

            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toJson(final File output) throws IllegalArgumentException, NullPointerException, UncheckedIOException {
        toJson(0, size(), output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, NullPointerException, UncheckedIOException {
        toJson(fromRowIndex, toRowIndex, _columnNameList, output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, NullPointerException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);
        if (N.notEmpty(columnNames)) {
            checkColumnNames(columnNames);
        }
        writeExportFile(output, writer -> toJson(fromRowIndex, toRowIndex, columnNames, writer));
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toJson(final OutputStream output) throws IllegalArgumentException, UncheckedIOException {
        toJson(0, size(), output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        toJson(fromRowIndex, toRowIndex, _columnNameList, output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);
        if (N.notEmpty(columnNames)) {
            checkColumnNames(columnNames);
        }

        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter(output);

        try {
            toJson(fromRowIndex, toRowIndex, columnNames, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toJson(final Writer output) throws IllegalArgumentException, UncheckedIOException {
        toJson(0, size(), output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        toJson(fromRowIndex, toRowIndex, _columnNameList, output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toJson(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
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
            charArrayOfColumnNames[i] = toQuotedJsonNameChars(_columnNameList.get(columnIndexes[i]));
        }

        final boolean isBufferedWriter = output instanceof BufferedJsonWriter;
        final BufferedJsonWriter bw = isBufferedWriter ? (BufferedJsonWriter) output : Objectory.createBufferedJsonWriter(output);

        try {
            bw.write(SK._BRACKET_L);

            Type<Object> type = null;
            Object element = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                if (rowIndex > fromRowIndex) {
                    bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                }

                bw.write(SK._BRACE_L);

                for (int i = 0; i < columnCount; i++) {
                    element = _columnList.get(columnIndexes[i]).get(rowIndex);

                    type = element == null ? null : Type.of(element.getClass());

                    if (i > 0) {
                        bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    bw.write(charArrayOfColumnNames[i]);
                    bw.write(SK._COLON);

                    writeExportValue(bw, element, type, true, rowIndex, _columnNameList.get(columnIndexes[i]));
                }

                bw.write(SK._BRACE_R);
            }

            bw.write(SK._BRACKET_R);

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

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public String toXml(final String rowElementName) throws IllegalArgumentException {
        return toXml(0, size(), N.checkArgNotEmpty(rowElementName, cs.rowElementName));
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public String toXml(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException, IllegalArgumentException {
        return toXml(fromRowIndex, toRowIndex, ROW);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code fromRowIndex > toRowIndex}, or {@code toRowIndex > size()}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public String toXml(final int fromRowIndex, final int toRowIndex, final String rowElementName) throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);

        return toXml(fromRowIndex, toRowIndex, _columnNameList, N.checkArgNotEmpty(rowElementName, cs.rowElementName));
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public String toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return toXml(fromRowIndex, toRowIndex, columnNames, ROW);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code fromRowIndex > toRowIndex}, or {@code toRowIndex > size()}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public String toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        if (N.notEmpty(columnNames)) {
            checkColumnNames(columnNames);
        }

        N.checkArgNotEmpty(rowElementName, cs.rowElementName);

        final BufferedXmlWriter writer = Objectory.createBufferedXmlWriter();

        try {
            toXml(fromRowIndex, toRowIndex, columnNames, N.checkArgNotEmpty(rowElementName, cs.rowElementName), writer);

            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final File output) throws IllegalArgumentException, NullPointerException, UncheckedIOException {
        toXml(0, size(), output);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final String rowElementName, final File output) throws IllegalArgumentException, NullPointerException, UncheckedIOException {
        toXml(0, size(), N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, NullPointerException, UncheckedIOException {
        toXml(fromRowIndex, toRowIndex, ROW, output);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code fromRowIndex > toRowIndex}, or {@code toRowIndex > size()}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final String rowElementName, final File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, NullPointerException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);

        toXml(fromRowIndex, toRowIndex, _columnNameList, N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, NullPointerException, UncheckedIOException {
        toXml(fromRowIndex, toRowIndex, columnNames, ROW, output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName, final File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, NullPointerException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);
        if (N.notEmpty(columnNames)) {
            checkColumnNames(columnNames);
        }
        N.checkArgNotEmpty(rowElementName, cs.rowElementName);
        final Document document = XmlUtil.createDOMParser().newDocument();
        checkXmlElementName(document, rowElementName, cs.rowElementName);
        if (columnNames != null) {
            for (final String name : columnNames) {
                checkXmlElementName(document, name, cs.columnName);
            }
        }
        writeExportFile(output, writer -> toXml(fromRowIndex, toRowIndex, columnNames, rowElementName, writer));
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final OutputStream output) throws IllegalArgumentException, UncheckedIOException {
        toXml(0, size(), output);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final String rowElementName, final OutputStream output) throws IllegalArgumentException, UncheckedIOException {
        toXml(0, size(), N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        toXml(fromRowIndex, toRowIndex, ROW, output);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code fromRowIndex > toRowIndex}, or {@code toRowIndex > size()}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final String rowElementName, final OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);

        toXml(fromRowIndex, toRowIndex, _columnNameList, N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        toXml(fromRowIndex, toRowIndex, columnNames, ROW, output);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code fromRowIndex > toRowIndex}, or {@code toRowIndex > size()}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName,
            final OutputStream output) throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);
        if (N.notEmpty(columnNames)) {
            checkColumnNames(columnNames);
        }

        final BufferedXmlWriter writer = Objectory.createBufferedXmlWriter(output);

        try {
            toXml(fromRowIndex, toRowIndex, columnNames, N.checkArgNotEmpty(rowElementName, cs.rowElementName), writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final Writer output) throws IllegalArgumentException, UncheckedIOException {
        toXml(0, size(), output);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final String rowElementName, final Writer output) throws IllegalArgumentException, UncheckedIOException {
        toXml(0, size(), N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        toXml(fromRowIndex, toRowIndex, ROW, output);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code fromRowIndex > toRowIndex}, or {@code toRowIndex > size()}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final String rowElementName, final Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);

        toXml(fromRowIndex, toRowIndex, _columnNameList, N.checkArgNotEmpty(rowElementName, cs.rowElementName), output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        toXml(fromRowIndex, toRowIndex, columnNames, ROW, output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toXml(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String rowElementName, final Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int[] columnIndexes = N.isEmpty(columnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(columnNames);
        N.checkArgNotEmpty(rowElementName, cs.rowElementName);

        final Document document = XmlUtil.createDOMParser().newDocument();
        checkXmlElementName(document, rowElementName, cs.rowElementName);

        if (N.isEmpty(columnNames)) {
            try {
                IOUtil.write(XmlConstants.DATASET_ELE_START, output);
                IOUtil.write(XmlConstants.DATASET_ELE_END, output);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final int columnCount = columnIndexes.length;

        final char[] rowElementNameHead = ("<" + rowElementName + ">").toCharArray();
        final char[] rowElementNameTail = ("</" + rowElementName + ">").toCharArray();

        final char[][] charArrayOfColumnNames = new char[columnCount][];

        for (int i = 0; i < columnCount; i++) {
            final String columnName = _columnNameList.get(columnIndexes[i]);
            checkXmlElementName(document, columnName, cs.columnName);

            charArrayOfColumnNames[i] = columnName.toCharArray();
        }

        final boolean isBufferedWriter = output instanceof BufferedXmlWriter;
        final BufferedXmlWriter bw = isBufferedWriter ? (BufferedXmlWriter) output : Objectory.createBufferedXmlWriter(output);

        try {
            bw.write(XmlConstants.DATASET_ELE_START);

            Type<Object> type = null;
            Object element = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                bw.write(rowElementNameHead);

                for (int i = 0; i < columnCount; i++) {
                    element = _columnList.get(columnIndexes[i]).get(rowIndex);

                    type = element == null ? null : Type.of(element.getClass());

                    bw.write(SK._LESS_THAN);
                    bw.write(charArrayOfColumnNames[i]);
                    bw.write(SK._GREATER_THAN);

                    writeExportValue(bw, element, type, false, rowIndex, _columnNameList.get(columnIndexes[i]));

                    bw.write(SK._LESS_THAN);
                    bw.write(SK._SLASH);
                    bw.write(charArrayOfColumnNames[i]);
                    bw.write(SK._GREATER_THAN);
                }

                bw.write(rowElementNameTail);
            }

            bw.write(XmlConstants.DATASET_ELE_END);

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
        return toCsv(0, size(), _columnNameList);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public String toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        if (N.notEmpty(columnNames)) {
            checkColumnNames(columnNames);
        }

        final BufferedWriter bw = Objectory.createBufferedCsvWriter();

        try {
            toCsv(fromRowIndex, toRowIndex, columnNames, bw);

            return bw.toString();
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toCsv(final File output) throws IllegalArgumentException, NullPointerException, UncheckedIOException {
        toCsv(0, size(), _columnNameList, output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final File output)
            throws IndexOutOfBoundsException, IllegalArgumentException, NullPointerException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);
        if (N.notEmpty(columnNames)) {
            checkColumnNames(columnNames);
        }
        writeExportFile(output, writer -> toCsv(fromRowIndex, toRowIndex, columnNames, writer));
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toCsv(final OutputStream output) throws IllegalArgumentException, UncheckedIOException {
        toCsv(0, size(), _columnNameList, output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);
        if (N.notEmpty(columnNames)) {
            checkColumnNames(columnNames);
        }

        final Writer writer = IOUtil.newOutputStreamWriter(output); // NOSONAR

        try {
            toCsv(fromRowIndex, toRowIndex, columnNames, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toCsv(final Writer output) throws IllegalArgumentException, UncheckedIOException {
        toCsv(0, size(), _columnNameList, output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void toCsv(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Writer output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);

        if (N.isEmpty(columnNames)) {
            return;
        }

        final Type<Object> strType = Type.of(String.class);
        final int[] columnIndexes = checkColumnNames(columnNames);
        final int columnCount = columnIndexes.length;

        final boolean isBufferedWriter = output instanceof BufferedCsvWriter;
        final BufferedCsvWriter bw = isBufferedWriter ? (BufferedCsvWriter) output : Objectory.createBufferedCsvWriter(output);

        final char separator = SK._COMMA;

        try {
            for (int i = 0; i < columnCount; i++) {
                if (i > 0) {
                    bw.write(separator);
                }

                // bw.write(getColumnName(columnIndexes[i]));

                CsvUtil.writeField(bw, strType, getColumnName(columnIndexes[i]));
            }

            Object element = null;

            for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                for (int i = 0; i < columnCount; i++) {
                    if (i > 0) {
                        bw.write(separator);
                    }

                    element = _columnList.get(columnIndexes[i]).get(rowIndex);

                    CsvUtil.writeField(bw, null, element);
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

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final String keyColumnName, final String aggregateOnColumnName, final String aggregateResultColumnName,
            final Collector<?, ?, ?> collector) throws IllegalArgumentException {
        return groupBy(keyColumnName, Fn.identity(), aggregateOnColumnName, aggregateResultColumnName, collector);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final String keyColumnName, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Class<?> rowType) throws IllegalArgumentException {
        checkColumnName(keyColumnName);
        checkColumnNames(aggregateOnColumnNames);

        N.checkArgNotEmpty(aggregateResultColumnName, cs.aggregateResultColumnName);

        if (N.equals(keyColumnName, aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicate property name: " + aggregateResultColumnName);
        }

        checkSupportedRowType(rowType, cs.rowType);
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

        final List<String> newColumnNameList = N.toList(keyColumnName, aggregateResultColumnName);
        final List<List<Object>> newColumnList = N.toList(keyList, new ArrayList<>(map.values()));

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final String keyColumnName, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException {
        return groupBy(keyColumnName, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Dataset groupBy(final String keyColumnName, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Function<? super DisposableObjArray, ? extends T> rowMapper, final Collector<? super T, ?, ?> collector) throws IllegalArgumentException {
        return groupBy(keyColumnName, Fn.identity(), aggregateOnColumnNames, aggregateResultColumnName, rowMapper, collector);
    }

    private Dataset groupBy(final String keyColumnName, final Function<?, ?> keyExtractor) {
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
            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }

        final Function<Object, ?> keyExtractorToUse = (Function<Object, ?>) keyExtractor;
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

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final String keyColumnName, final Function<?, ?> keyExtractor, final String aggregateOnColumnName,
            final String aggregateResultColumnName, final Collector<?, ?, ?> collector) throws IllegalArgumentException {
        final int columnIndex = checkColumnName(keyColumnName);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        final int aggOnColumnIndex = checkColumnName(aggregateOnColumnName);

        N.checkArgNotEmpty(aggregateResultColumnName, cs.aggregateResultColumnName);

        if (N.equals(keyColumnName, aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicate property name: " + aggregateResultColumnName);
        }

        N.checkArgNotNull(collector, cs.collector);

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
            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }

        final boolean isIdentityKeyExtractor = keyExtractor == Fn.identity();
        final Function<Object, ?> keyExtractorToUse = (Function<Object, ?>) keyExtractor;
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
            key = hashKey(isIdentityKeyExtractor ? value : keyExtractorToUse.apply(value));

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

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final String keyColumnName, final Function<?, ?> keyExtractor, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Class<?> rowType) throws IllegalArgumentException {
        checkColumnName(keyColumnName);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        checkColumnNames(aggregateOnColumnNames);
        N.checkArgNotEmpty(aggregateResultColumnName, cs.aggregateResultColumnName);

        if (N.equals(keyColumnName, aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicate property name: " + aggregateResultColumnName);
        }

        checkSupportedRowType(rowType, cs.rowType);
        final Function<Object, ?> keyExtractorToUse = (Function<Object, ?>) keyExtractor;

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

        final List<String> newColumnNameList = N.toList(keyColumnName, aggregateResultColumnName);
        final List<List<Object>> newColumnList = N.toList(keyList, new ArrayList<>(map.values()));

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    private static final Function<? super DisposableObjArray, Object[]> CLONE = DisposableObjArray::copy;

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final String keyColumnName, final Function<?, ?> keyExtractor, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException {
        return groupBy(keyColumnName, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Dataset groupBy(final String keyColumnName, final Function<?, ?> keyExtractor, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Function<? super DisposableObjArray, ? extends T> rowMapper,
            final Collector<? super T, ?, ?> collector) throws IllegalArgumentException {
        final int columnIndex = checkColumnName(keyColumnName);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        final int[] aggOnColumnIndexes = checkColumnNames(aggregateOnColumnNames);

        N.checkArgNotEmpty(aggregateResultColumnName, cs.aggregateResultColumnName);

        if (N.equals(keyColumnName, aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicate property name: " + aggregateResultColumnName);
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
            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }

        final boolean isIdentityKeyExtractor = keyExtractor == Fn.identity();
        final Function<Object, ?> keyExtractorToUse = (Function<Object, ?>) keyExtractor;
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
            key = hashKey(isIdentityKeyExtractor ? value : keyExtractorToUse.apply(value));

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

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final Collection<String> keyColumnNames) throws IllegalArgumentException {
        return groupBy(keyColumnNames, Fn.identity());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final Collection<String> keyColumnNames, final String aggregateOnColumnName, final String aggregateResultColumnName,
            final Collector<?, ?, ?> collector) throws IllegalArgumentException {
        return groupBy(keyColumnNames, Fn.identity(), aggregateOnColumnName, aggregateResultColumnName, collector);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Class<?> rowType) throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);
        final int[] keyColumnIndexes = checkColumnNames(keyColumnNames);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);

        checkColumnNames(aggregateOnColumnNames);
        N.checkArgNotEmpty(aggregateResultColumnName, cs.aggregateResultColumnName);

        if (keyColumnNames.contains(aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicate property name: " + aggregateResultColumnName);
        }

        checkSupportedRowType(rowType, cs.rowType);
        if (keyColumnNames.size() == 1) {
            return groupBy(keyColumnNames.iterator().next(), aggregateOnColumnNames, aggregateResultColumnName, rowType);
        }

        final int size = size();

        // Eagerly, so a zero-row Dataset rejects the same arguments a populated one does: these were only
        // resolved by the toList(..) below, which the size == 0 early return skips, so a mistyped aggregate
        // column or an unsupported rowType was silently accepted whenever the Dataset happened to be empty.

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

            return new RowDataset(newColumnNameList, newColumnList, null, true);
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

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException {
        return groupBy(keyColumnNames, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Dataset groupBy(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Function<? super DisposableObjArray, ? extends T> rowMapper, final Collector<? super T, ?, ?> collector) throws IllegalArgumentException {
        return groupBy(keyColumnNames, Fn.identity(), aggregateOnColumnNames, aggregateResultColumnName, rowMapper, collector);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);
        final int[] keyColumnIndexes = checkColumnNames(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        final boolean isIdentityKeyExtractor = keyExtractor == Fn.identity();

        if (keyColumnNames.size() == 1 && isIdentityKeyExtractor) {
            return this.groupBy(keyColumnNames.iterator().next(), keyExtractor);
        }

        final int size = size();
        final int keyColumnCount = keyColumnIndexes.length;
        final int newColumnCount = keyColumnIndexes.length;
        final List<String> newColumnNameList = N.newArrayList(keyColumnNames);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }

        final Set<Object> keyRowSet = N.newHashSet();
        Object[] keyRow = Objectory.createObjectArray(keyColumnCount);
        Wrapper<Object[]> keyRowWrapper = isIdentityKeyExtractor ? Wrapper.of(keyRow) : null;
        final DisposableObjArray disposableArray = isIdentityKeyExtractor ? null : DisposableObjArray.wrap(keyRow);
        Object key = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            key = isIdentityKeyExtractor ? keyRowWrapper : hashKey(keyExtractor.apply(disposableArray));

            if (keyRowSet.add(key)) {
                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                if (isIdentityKeyExtractor) {
                    keyRow = Objectory.createObjectArray(keyColumnCount);
                    keyRowWrapper = Wrapper.of(keyRow);
                }
            }
        }

        if (keyRow != null) {
            Objectory.recycle(keyRow);
            keyRow = null;
        }

        if (isIdentityKeyExtractor) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) keyRowSet;

            for (final Wrapper<Object[]> rw : tmp) {
                Objectory.recycle(rw.value());
            }
        }

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final String aggregateOnColumnName, final String aggregateResultColumnName, final Collector<?, ?, ?> collector) throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);
        final int[] keyColumnIndexes = checkColumnNames(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        final int aggOnColumnIndex = checkColumnName(aggregateOnColumnName);
        N.checkArgNotEmpty(aggregateResultColumnName, cs.aggregateResultColumnName);

        if (keyColumnNames.contains(aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicate property name: " + aggregateResultColumnName);
        }

        N.checkArgNotNull(collector, cs.collector);

        final boolean isIdentityKeyExtractor = keyExtractor == Fn.identity();

        if (keyColumnNames.size() == 1 && isIdentityKeyExtractor) {
            return groupBy(keyColumnNames.iterator().next(), keyExtractor, aggregateOnColumnName, aggregateResultColumnName, collector);
        }

        final int size = size();
        final int keyColumnCount = keyColumnIndexes.length;
        final int newColumnCount = keyColumnIndexes.length + 1;
        final List<String> newColumnNameList = new ArrayList<>(keyColumnNames);
        newColumnNameList.add(aggregateResultColumnName);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, Object> accumulator = (BiConsumer<Object, Object>) collector.accumulator();
        final Function<Object, Object> finisher = (Function<Object, Object>) collector.finisher();

        final List<Object> aggResultColumn = newColumnList.get(newColumnList.size() - 1);
        final List<Object> aggOnColumn = _columnList.get(aggOnColumnIndex);
        final Map<Object, Integer> keyRowIndexMap = new HashMap<>();
        Object[] keyRow = Objectory.createObjectArray(keyColumnCount);
        Wrapper<Object[]> keyRowWrapper = isIdentityKeyExtractor ? Wrapper.of(keyRow) : null;
        final DisposableObjArray disposableArray = isIdentityKeyExtractor ? null : DisposableObjArray.wrap(keyRow);
        Object key = null;
        Integer collectorRowIndex = -1;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            key = isIdentityKeyExtractor ? keyRowWrapper : hashKey(keyExtractor.apply(disposableArray));
            collectorRowIndex = keyRowIndexMap.get(key);

            if (collectorRowIndex == null) {
                collectorRowIndex = aggResultColumn.size();
                keyRowIndexMap.put(key, collectorRowIndex);
                aggResultColumn.add(supplier.get());

                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                if (isIdentityKeyExtractor) {
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

        if (isIdentityKeyExtractor) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) keyRowIndexMap.keySet();

            for (final Wrapper<Object[]> rw : tmp) {
                Objectory.recycle(rw.value());
            }
        }

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Class<?> rowType) throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);
        final int[] keyColumnIndexes = checkColumnNames(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);

        checkColumnNames(aggregateOnColumnNames);
        N.checkArgNotEmpty(aggregateResultColumnName, cs.aggregateResultColumnName);

        if (keyColumnNames.contains(aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicate property name: " + aggregateResultColumnName);
        }

        checkSupportedRowType(rowType, cs.rowType);
        final boolean isIdentityKeyExtractor = keyExtractor == Fn.identity();

        if (isIdentityKeyExtractor) {
            if (keyColumnNames.size() == 1) {
                return groupBy(keyColumnNames.iterator().next(), aggregateOnColumnNames, aggregateResultColumnName, rowType);
            }

            return groupBy(keyColumnNames, aggregateOnColumnNames, aggregateResultColumnName, rowType);
        }

        final int size = size();

        // Eagerly, so a zero-row Dataset rejects the same arguments a populated one does: these were only
        // resolved by the toList(..) below, which the size == 0 early return skips, so a mistyped aggregate
        // column or an unsupported rowType was silently accepted whenever the Dataset happened to be empty.

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

            return new RowDataset(newColumnNameList, newColumnList, null, true);
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

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset groupBy(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector)
            throws IllegalArgumentException {
        return groupBy(keyColumnNames, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Dataset groupBy(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Function<? super DisposableObjArray, ? extends T> rowMapper, final Collector<? super T, ?, ?> collector) throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);
        final int[] keyColumnIndexes = checkColumnNames(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        final int[] aggOnColumnIndexes = checkColumnNames(aggregateOnColumnNames);
        N.checkArgNotEmpty(aggregateResultColumnName, cs.aggregateResultColumnName);

        if (keyColumnNames.contains(aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicate property name: " + aggregateResultColumnName);
        }

        N.checkArgNotNull(rowMapper, cs.rowMapper);
        N.checkArgNotNull(collector, cs.collector);

        final boolean isIdentityKeyExtractor = keyExtractor == Fn.identity();

        if (keyColumnNames.size() == 1 && isIdentityKeyExtractor) {
            return groupBy(keyColumnNames.iterator().next(), keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, rowMapper, collector);
        }

        final int size = size();
        final int keyColumnCount = keyColumnIndexes.length;
        final int newColumnCount = keyColumnIndexes.length + 1;
        final List<String> newColumnNameList = new ArrayList<>(keyColumnNames);
        newColumnNameList.add(aggregateResultColumnName);

        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, T> accumulator = (BiConsumer<Object, T>) collector.accumulator();
        final Function<Object, Object> finisher = (Function<Object, Object>) collector.finisher();

        final int aggOnColumnCount = aggOnColumnIndexes.length;
        final List<Object> aggResultColumn = newColumnList.get(newColumnList.size() - 1);
        final Map<Object, Integer> keyRowIndexMap = new HashMap<>();
        Object[] keyRow = Objectory.createObjectArray(keyColumnCount);
        Wrapper<Object[]> keyRowWrapper = isIdentityKeyExtractor ? Wrapper.of(keyRow) : null;
        final DisposableObjArray keyDisposableArray = isIdentityKeyExtractor ? null : DisposableObjArray.wrap(keyRow);
        final Object[] aggOnRow = new Object[aggOnColumnCount];
        final DisposableObjArray aggOnRowDisposableArray = DisposableObjArray.wrap(aggOnRow);
        Object key = null;
        Integer collectorRowIndex = -1;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0; i < keyColumnCount; i++) {
                keyRow[i] = _columnList.get(keyColumnIndexes[i]).get(rowIndex);
            }

            key = isIdentityKeyExtractor ? keyRowWrapper : hashKey(keyExtractor.apply(keyDisposableArray));
            collectorRowIndex = keyRowIndexMap.get(key);

            if (collectorRowIndex == null) {
                collectorRowIndex = aggResultColumn.size();
                keyRowIndexMap.put(key, collectorRowIndex);
                aggResultColumn.add(supplier.get());

                for (int i = 0; i < keyColumnCount; i++) {
                    newColumnList.get(i).add(keyRow[i]);
                }

                if (isIdentityKeyExtractor) {
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

        if (isIdentityKeyExtractor) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) keyRowIndexMap.keySet();

            for (final Wrapper<Object[]> rw : tmp) {
                Objectory.recycle(rw.value());
            }
        }

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /** Retains the first original key for each deep-equality pivot group, in encounter order. */
    private static <K> Set<K> pivotKeys(final Collection<K> keys) {
        // Dataset groups arrays by content, while the resulting Sheet uses standard key equality.
        // Deduplicate before constructing the Sheet so separate groups sharing an axis value do not
        // create duplicate axes or leave empty rows/columns. Expose each group's original key object.
        final Map<Object, K> representatives = new LinkedHashMap<>();
        for (final K key : keys) {
            representatives.putIfAbsent(hashKey(key), key);
        }
        return new LinkedHashSet<>(representatives.values());
    }

    private <R, C, T> Sheet<R, C, T> pivot(final Dataset groupedDataset, final String keyColumnName, final String pivotColumnName) {
        final ImmutableList<R> rowKeyList = groupedDataset.getColumn(0);
        final ImmutableList<C> colKeyList = groupedDataset.getColumn(1);

        final Set<R> rowKeySet = pivotKeys(rowKeyList);
        final Set<C> colKeySet = pivotKeys(colKeyList);

        // Sheet keys cannot be null; say which column is at fault instead of letting Sheet report a bare
        // "Row key cannot be null" that names no column.
        if (rowKeySet.contains(null)) {
            throw new IllegalArgumentException("Cannot pivot: the key column '" + keyColumnName + "' holds a null value, and a Sheet row key cannot be null");
        }

        if (colKeySet.contains(null)) {
            throw new IllegalArgumentException(
                    "Cannot pivot: the pivot column '" + pivotColumnName + "' holds a null value, and a Sheet column key cannot be null");
        }

        final List<List<T>> rows = new ArrayList<>(rowKeySet.size());
        for (int i = 0; i < rowKeySet.size(); i++) {
            rows.add(new ArrayList<>(Collections.nCopies(colKeySet.size(), null)));
        }

        // N.newHashMap takes an expected entry count and converts it to a capacity; new HashMap<>(int) does
        // not, so the raw size under-sized the map and forced a rehash while filling it (see initNewColumnList).
        final Map<Object, Integer> rowIndexMap = N.newHashMap(rowKeySet.size());
        final Iterator<R> rowKeyIter = rowKeySet.iterator();
        for (int i = 0, size = rowKeySet.size(); i < size; i++) {
            rowIndexMap.put(hashKey(rowKeyIter.next()), i);
        }

        final Map<Object, Integer> colIndexMap = N.newHashMap(colKeySet.size());
        final Iterator<C> colKeyIter = colKeySet.iterator();
        for (int i = 0, size = colKeySet.size(); i < size; i++) {
            colIndexMap.put(hashKey(colKeyIter.next()), i);
        }

        final ImmutableList<T> aggColumn = groupedDataset.getColumn(2);

        for (int i = 0, size = groupedDataset.size(); i < size; i++) {
            rows.get(rowIndexMap.get(hashKey(rowKeyList.get(i)))).set(colIndexMap.get(hashKey(colKeyList.get(i))), aggColumn.get(i));
        }

        return Sheet.rows(rowKeySet, colKeySet, rows);
    }

    /**
     * Returns a name for the aggregate column of the intermediate {@code groupBy} that {@code pivot} builds. The
     * intermediate Dataset is read back by column index and never exposed, so the name only has to differ from
     * the key and pivot column names. Reusing the aggregated column's name, as the single-column overload did,
     * failed with "Duplicate property name" whenever the aggregated column was the key or pivot column itself -
     * for example a row count via {@code pivot("region", "product", "region", counting())}.
     */
    private static String pivotResultColumnName(final String keyColumnName, final String pivotColumnName) {
        String name = "__pivot_result";

        for (int suffix = 2; name.equals(keyColumnName) || name.equals(pivotColumnName); suffix++) {
            name = "__pivot_result_" + suffix;
        }

        return name;
    }

    /**
     * Validates the grouping keys of a {@code rollup}/{@code cube} call at the call site. The returned Stream is
     * lazy, so without this an unknown or missing key column was only reported when the Stream was consumed,
     * and an empty or {@code null} selection surfaced as "The specified column: null is not included" from the
     * grand-total level's {@code groupBy}.
     *
     * @param keyColumnNames the grouping key columns
     * @throws IllegalArgumentException if {@code keyColumnNames} is {@code null} or empty, or names a column this
     *         Dataset does not have
     */
    private void checkKeyColumnNamesForRollup(final Collection<String> keyColumnNames) throws IllegalArgumentException {
        N.checkArgNotEmpty(keyColumnNames, cs.keyColumnNames);
        checkColumnNames(keyColumnNames);
    }

    /**
     * Validates the aggregate result column name of a {@code rollup}/{@code cube} call at the call site, the
     * same way {@code groupBy} validates it: it must be non-empty and must not be one of the grouping keys.
     *
     * <p>The collision was previously left to the {@code groupBy} inside the lazy {@code map(..)}, so it
     * surfaced only when the returned Stream was consumed - at an arbitrary later point, and against the
     * documented promise that "arguments are validated when this method is called, before the Stream is
     * returned". The no-aggregate overloads already avoid the collision by generating a free name; see
     * {@link #countColumnName(Collection)}.</p>
     *
     * @param keyColumnNames the grouping key columns
     * @param aggregateResultColumnName the name to give the aggregated column
     * @throws IllegalArgumentException if {@code aggregateResultColumnName} is {@code null} or empty, or is one
     *         of {@code keyColumnNames}
     */
    private static void checkAggregateResultColumnNameForRollup(final Collection<String> keyColumnNames, final String aggregateResultColumnName)
            throws IllegalArgumentException {
        N.checkArgNotEmpty(aggregateResultColumnName, cs.aggregateResultColumnName);

        if (keyColumnNames.contains(aggregateResultColumnName)) {
            throw new IllegalArgumentException("Duplicate property name: " + aggregateResultColumnName);
        }
    }

    /**
     * Returns the name to give the row-count column produced by the no-aggregate {@code rollup}/{@code cube}
     * overloads: {@code count} normally, or {@code count_2}, {@code count_3}, ... when a key column already
     * carries that name - the same suffix scheme the joins use for a colliding right-side column name.
     *
     * <p>Without this, rolling up a Dataset that happens to have a column called {@code "count"} failed
     * outright with {@code IllegalArgumentException: Duplicate property name: count}, because the generated
     * result column collided with one of the grouping keys.</p>
     *
     * @param keyColumnNames the grouping key columns, which become the result's leading columns
     * @return a result-column name that does not collide with any key column
     */
    private static String countColumnName(final Collection<String> keyColumnNames) {
        if (N.isEmpty(keyColumnNames) || !keyColumnNames.contains(COUNT)) {
            return COUNT;
        }

        int suffix = 2;
        String name = COUNT + "_" + suffix;

        while (keyColumnNames.contains(name)) {
            name = COUNT + "_" + (++suffix);
        }

        return name;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> rollup(final Collection<String> keyColumnNames) throws IllegalArgumentException {
        return rollup(keyColumnNames, N.firstOrNullIfEmpty(keyColumnNames), countColumnName(keyColumnNames), Collectors.countingToInt());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> rollup(final Collection<String> keyColumnNames, final String aggregateOnColumnName, final String aggregateResultColumnName,
            final Collector<?, ?, ?> collector) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        checkColumnName(aggregateOnColumnName);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        N.checkArgNotNull(collector, cs.collector);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();

        return Stream.of(Iterables.rollup(keyColumnNames)) //
                .reversed()
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateOnColumnName, aggregateResultColumnName, collector);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, aggregateOnColumnName, aggregateResultColumnName, collector);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> rollup(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Class<?> rowType) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);
        checkColumnNames(aggregateOnColumnNames);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        checkSupportedRowType(rowType, cs.rowType);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();
        final List<String> aggregateNames = new ArrayList<>(aggregateOnColumnNames);

        return Stream.of(Iterables.rollup(keyColumnNames)) //
                .reversed()
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateNames, aggregateResultColumnName, rowType);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, aggregateNames, aggregateResultColumnName, rowType);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> rollup(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException {
        return rollup(keyColumnNames, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<Dataset> rollup(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Function<? super DisposableObjArray, ? extends T> rowMapper,
            final Collector<? super T, ?, ?> collector) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);
        checkColumnNames(aggregateOnColumnNames);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        N.checkArgNotNull(rowMapper, cs.rowMapper);
        N.checkArgNotNull(collector, cs.collector);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();
        final List<String> aggregateNames = new ArrayList<>(aggregateOnColumnNames);

        return Stream.of(Iterables.rollup(keyColumnNames)) //
                .reversed()
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateNames, aggregateResultColumnName, rowMapper,
                                collector);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, aggregateNames, aggregateResultColumnName, rowMapper, collector);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> rollup(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor)
            throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        return rollup(keyColumnNames, keyExtractor, N.firstOrNullIfEmpty(keyColumnNames), countColumnName(keyColumnNames), Collectors.countingToInt());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> rollup(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final String aggregateOnColumnName, final String aggregateResultColumnName, final Collector<?, ?, ?> collector) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);
        checkColumnName(aggregateOnColumnName);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        N.checkArgNotNull(collector, cs.collector);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();

        return Stream.of(Iterables.rollup(keyColumnNames)) //
                .reversed()
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateOnColumnName, aggregateResultColumnName, collector);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, keyExtractor, aggregateOnColumnName, aggregateResultColumnName, collector);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> rollup(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Class<?> rowType) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);
        checkColumnNames(aggregateOnColumnNames);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        checkSupportedRowType(rowType, cs.rowType);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();
        final List<String> aggregateNames = new ArrayList<>(aggregateOnColumnNames);

        return Stream.of(Iterables.rollup(keyColumnNames)) //
                .reversed()
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateNames, aggregateResultColumnName, rowType);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, keyExtractor, aggregateNames, aggregateResultColumnName, rowType);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> rollup(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector)
            throws IllegalArgumentException {
        return rollup(keyColumnNames, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<Dataset> rollup(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Function<? super DisposableObjArray, ? extends T> rowMapper, final Collector<? super T, ?, ?> collector) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);
        checkColumnNames(aggregateOnColumnNames);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        N.checkArgNotNull(rowMapper, cs.rowMapper);
        N.checkArgNotNull(collector, cs.collector);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();
        final List<String> aggregateNames = new ArrayList<>(aggregateOnColumnNames);

        return Stream.of(Iterables.rollup(keyColumnNames)) //
                .reversed()
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateNames, aggregateResultColumnName, rowMapper,
                                collector);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, keyExtractor, aggregateNames, aggregateResultColumnName, rowMapper, collector);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> cube(final Collection<String> keyColumnNames) throws IllegalArgumentException {
        return cube(keyColumnNames, N.firstOrNullIfEmpty(keyColumnNames), countColumnName(keyColumnNames), Collectors.countingToInt());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> cube(final Collection<String> keyColumnNames, final String aggregateOnColumnName, final String aggregateResultColumnName,
            final Collector<?, ?, ?> collector) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        checkColumnName(aggregateOnColumnName);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        N.checkArgNotNull(collector, cs.collector);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();

        return cubeSet(keyColumnNames) //
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateOnColumnName, aggregateResultColumnName, collector);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, aggregateOnColumnName, aggregateResultColumnName, collector);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> cube(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Class<?> rowType) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);
        checkColumnNames(aggregateOnColumnNames);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        checkSupportedRowType(rowType, cs.rowType);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();
        final List<String> aggregateNames = new ArrayList<>(aggregateOnColumnNames);

        return cubeSet(keyColumnNames) //
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateNames, aggregateResultColumnName, rowType);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, aggregateNames, aggregateResultColumnName, rowType);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> cube(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector) throws IllegalArgumentException {
        return cube(keyColumnNames, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<Dataset> cube(final Collection<String> keyColumnNames, final Collection<String> aggregateOnColumnNames,
            final String aggregateResultColumnName, final Function<? super DisposableObjArray, ? extends T> rowMapper,
            final Collector<? super T, ?, ?> collector) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);
        checkColumnNames(aggregateOnColumnNames);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        N.checkArgNotNull(rowMapper, cs.rowMapper);
        N.checkArgNotNull(collector, cs.collector);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();
        final List<String> aggregateNames = new ArrayList<>(aggregateOnColumnNames);

        return cubeSet(keyColumnNames) //
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateNames, aggregateResultColumnName, rowMapper,
                                collector);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, aggregateNames, aggregateResultColumnName, rowMapper, collector);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> cube(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor)
            throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        return cube(keyColumnNames, keyExtractor, N.firstOrNullIfEmpty(keyColumnNames), countColumnName(keyColumnNames), Collectors.countingToInt());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> cube(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final String aggregateOnColumnName, final String aggregateResultColumnName, final Collector<?, ?, ?> collector) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);
        checkColumnName(aggregateOnColumnName);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        N.checkArgNotNull(collector, cs.collector);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();

        return cubeSet(keyColumnNames) //
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateOnColumnName, aggregateResultColumnName, collector);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, keyExtractor, aggregateOnColumnName, aggregateResultColumnName, collector);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> cube(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Class<?> rowType) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);
        checkColumnNames(aggregateOnColumnNames);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        checkSupportedRowType(rowType, cs.rowType);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();
        final List<String> aggregateNames = new ArrayList<>(aggregateOnColumnNames);

        return cubeSet(keyColumnNames) //
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateNames, aggregateResultColumnName, rowType);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, keyExtractor, aggregateNames, aggregateResultColumnName, rowType);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> cube(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName, final Collector<? super Object[], ?, ?> collector)
            throws IllegalArgumentException {
        return cube(keyColumnNames, keyExtractor, aggregateOnColumnNames, aggregateResultColumnName, CLONE, collector);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<Dataset> cube(final Collection<String> keyColumnNames, final Function<? super DisposableObjArray, ?> keyExtractor,
            final Collection<String> aggregateOnColumnNames, final String aggregateResultColumnName,
            final Function<? super DisposableObjArray, ? extends T> rowMapper, final Collector<? super T, ?, ?> collector) throws IllegalArgumentException {
        checkKeyColumnNamesForRollup(keyColumnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);
        N.checkArgNotEmpty(aggregateOnColumnNames, cs.aggregateOnColumnNames);
        checkColumnNames(aggregateOnColumnNames);
        checkAggregateResultColumnNameForRollup(keyColumnNames, aggregateResultColumnName);
        N.checkArgNotNull(rowMapper, cs.rowMapper);
        N.checkArgNotNull(collector, cs.collector);

        // Snapshot selections now; aggregation is deferred until the stream is consumed. The modCount is
        // snapshotted for the same reason every other lazy source in this class snapshots it (split(..),
        // stream(..), iterator(..), columns(), paginate(..)): each level re-reads the LIVE Dataset when it is
        // pulled, so a structural change between this call and the consumption used to produce levels computed
        // from different data - the grand total not reconciling with the detail rows, silently - or to report a
        // since-removed key column as a bad argument, contradicting the documented "arguments are validated
        // when this method is called".
        final int expectedModCount = modCount;
        final String firstKeyColumnName = keyColumnNames.iterator().next();
        final List<String> aggregateNames = new ArrayList<>(aggregateOnColumnNames);

        return cubeSet(keyColumnNames) //
                .map(columnNames -> {
                    checkModification(expectedModCount);

                    if (columnNames.isEmpty()) {
                        final Dataset ds = groupBy(firstKeyColumnName, k -> firstKeyColumnName, aggregateNames, aggregateResultColumnName, rowMapper,
                                collector);
                        ds.removeColumn(firstKeyColumnName);
                        return ds;
                    } else {
                        return groupBy(columnNames, keyExtractor, aggregateNames, aggregateResultColumnName, rowMapper, collector);
                    }
                });
    }

    /**
     * @throws IllegalArgumentException if {@code columnNames} contains more than 30 names
     */
    private Stream<Set<String>> cubeSet(final Collection<String> columnNames) throws IllegalArgumentException {
        N.checkArgument(columnNames.size() <= 30, "Cube supports at most 30 key columns");
        final List<String> names = new ArrayList<>(columnNames);
        // Enumerate fixed-cardinality bit sets directly, in the same order as the old power-set
        // grouping/reversal. Only one combination is retained, so limit(1) needs O(keyCount) space.
        return Stream.of(new ObjIteratorEx<Set<String>>() {
            private int cardinality = names.size();
            private long mask = (1L << cardinality) - 1;
            private final long bound = 1L << names.size();

            @Override
            public boolean hasNext() {
                return cardinality >= 0;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if this iterator has no remaining element
             */
            @Override
            public Set<String> next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                final Set<String> result = new LinkedHashSet<>(cardinality);
                for (int i = 0; i < names.size(); i++) {
                    if ((mask & (1L << i)) != 0) {
                        result.add(names.get(i));
                    }
                }
                if (cardinality == 0) {
                    cardinality = -1;
                } else {
                    final long lowBit = mask & -mask;
                    final long next = mask + lowBit;
                    mask = next | (((next ^ mask) >>> 2) / lowBit);
                    if (mask >= bound) {
                        mask = (1L << --cardinality) - 1;
                    }
                }
                return result;
            }
        });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <R, C, T> Sheet<R, C, T> pivot(final String keyColumnName, final String pivotColumnName, final String aggregateOnColumnName,
            final Collector<?, ?, ? extends T> collector) throws IllegalArgumentException {
        final Dataset groupedDataset = groupBy(N.asList(keyColumnName, pivotColumnName), aggregateOnColumnName,
                pivotResultColumnName(keyColumnName, pivotColumnName), collector);

        return pivot(groupedDataset, keyColumnName, pivotColumnName);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <R, C, T> Sheet<R, C, T> pivot(final String keyColumnName, final String pivotColumnName, final Collection<String> aggregateOnColumnNames,
            final Collector<? super Object[], ?, ? extends T> collector) throws IllegalArgumentException {
        final String aggregateResultColumnName = pivotResultColumnName(keyColumnName, pivotColumnName);

        final Dataset groupedDataset = groupBy(N.asList(keyColumnName, pivotColumnName), aggregateOnColumnNames, aggregateResultColumnName, collector);

        return pivot(groupedDataset, keyColumnName, pivotColumnName);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <R, C, U, T> Sheet<R, C, T> pivot(final String keyColumnName, final String pivotColumnName, final Collection<String> aggregateOnColumnNames,
            final Function<? super DisposableObjArray, ? extends U> rowMapper, final Collector<? super U, ?, ? extends T> collector)
            throws IllegalArgumentException {
        N.checkArgNotNull(rowMapper, cs.rowMapper);

        final String aggregateResultColumnName = pivotResultColumnName(keyColumnName, pivotColumnName);

        final Dataset groupedDataset = groupBy(N.asList(keyColumnName, pivotColumnName), aggregateOnColumnNames, aggregateResultColumnName, rowMapper,
                collector);

        return pivot(groupedDataset, keyColumnName, pivotColumnName);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void sortBy(final String columnName) throws IllegalStateException, IllegalArgumentException {
        sortBy(columnName, Comparators.naturalOrder());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void sortBy(final String columnName, final Comparator<?> cmp) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(cmp, cs.cmp);

        sort(columnName, cmp, false);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void sortBy(final Collection<String> columnNames) throws IllegalStateException, IllegalArgumentException {
        sortBy(columnNames, Comparators.OBJECT_ARRAY_COMPARATOR);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void sortBy(final Collection<String> columnNames, final Comparator<? super Object[]> cmp) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(cmp, cs.cmp);

        sort(columnNames, cmp, false);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void sortBy(final Collection<String> columnNames, final Function<? super DisposableObjArray, ? extends Comparable> keyExtractor)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        sort(columnNames, keyExtractor, false);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void parallelSortBy(final String columnName) throws IllegalStateException, IllegalArgumentException {
        parallelSortBy(columnName, Comparators.naturalOrder());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void parallelSortBy(final String columnName, final Comparator<?> cmp) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(cmp, cs.cmp);

        sort(columnName, cmp, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void parallelSortBy(final Collection<String> columnNames) throws IllegalStateException, IllegalArgumentException {
        parallelSortBy(columnNames, Comparators.OBJECT_ARRAY_COMPARATOR);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void parallelSortBy(final Collection<String> columnNames, final Comparator<? super Object[]> cmp)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(cmp, cs.cmp);

        sort(columnNames, cmp, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void parallelSortBy(final Collection<String> columnNames, final Function<? super DisposableObjArray, ? extends Comparable> keyExtractor)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

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

    /**
     * Lifts a value comparator to one over {@link Indexed} pairs. {@code cmp} is never {@code null} here:
     * every caller rejects a {@code null} comparator with {@code N.checkArgNotNull(cmp, cs.cmp)} first.
     */
    private static Comparator<Indexed<Object>> createComparatorForIndexedObject(final Comparator<?> cmp) {
        final Comparator<Object> cmpToUse = (Comparator<Object>) cmp;

        return (a, b) -> cmpToUse.compare(a.value(), b.value());
    }

    /**
     * Lifts an {@code Object[]} comparator to one over {@link Indexed} pairs. As above, {@code cmp} is never
     * {@code null} at this point.
     */
    private static Comparator<Indexed<Object[]>> createComparatorForIndexedObjectArray(final Comparator<? super Object[]> cmp) {
        return (a, b) -> cmp.compare(a.value(), b.value());
    }

    private void sort(final Collection<String> columnNames, final Comparator<? super Object[]> cmp, final boolean isParallelSort) {
        checkFrozen();

        final int[] columnIndexes = checkColumnNames(columnNames);
        final int size = size();

        if (columnIndexes.length == 0 || size == 0) {
            return;
        }

        final int sortByColumnCount = columnIndexes.length;
        final Indexed<Object[]>[] arrayOfPair = new Indexed[size];

        // Plain arrays, not Objectory.createObjectArray: these are handed straight to the caller's
        // Comparator<? super Object[]>, whose parameter type carries no "do not retain" contract (unlike
        // DisposableObjArray). Recycling them on the way out zero-filled arrays a memoizing or caching
        // comparator could still be holding.
        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            arrayOfPair[rowIndex] = Indexed.of(new Object[sortByColumnCount], rowIndex);
        }

        for (int i = 0; i < sortByColumnCount; i++) {
            final List<Object> orderByColumn = _columnList.get(columnIndexes[i]);

            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                arrayOfPair[rowIndex].value()[i] = orderByColumn.get(rowIndex);
            }
        }

        final Comparator<Indexed<Object[]>> pairCmp = createComparatorForIndexedObjectArray(cmp);

        sort(arrayOfPair, pairCmp, isParallelSort);
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

        // permuteRows reports whether the sort actually moved anything. Sorting already-ordered rows leaves
        // the row order untouched, so it is not a structural modification and must not bump modCount:
        // doing so invalidated every outstanding stream/split/paginate for a no-op.
        if (permuteRows(arrayOfPair, size())) {
            rowsChanged();
        }
    }

    /**
     * Reorders the rows held in {@code _columnList} in place so that the row ending up at position {@code i}
     * is the one that currently sits at {@code sortedPairs[i].index()}.
     *
     * <p>The permutation is applied one cycle at a time: a cycle is rotated using a single saved row, so each
     * cell is written at most once and no copy of the dataset is allocated. {@code placed} records the
     * positions already written, which stops the outer loop from walking a cycle a second time when it later
     * reaches one of that cycle's other members. It is a {@code boolean[]} rather than a {@code Set<Integer>}
     * so that sorting a large dataset does not box one {@code Integer} per row.</p>
     *
     * <p>{@code sortedPairs} must hold a permutation of {@code [0, rowCount)}, which a sort of {@code Indexed}
     * pairs guarantees.</p>
     *
     * @param sortedPairs the sorted index pairs describing the target row order
     * @param rowCount the number of rows, i.e. {@code sortedPairs.length}
     * @return {@code true} if any row actually moved, {@code false} if {@code sortedPairs} is the identity
     *         permutation and this dataset was left untouched
     */
    private boolean permuteRows(final Indexed<?>[] sortedPairs, final int rowCount) {
        final int columnCount = _columnList.size();

        if (columnCount == 0) {
            return false;
        }

        final boolean[] placed = new boolean[rowCount];
        final Object[] tempRow = new Object[columnCount];
        boolean moved = false;

        for (int i = 0; i < rowCount; i++) {
            if (placed[i] || sortedPairs[i].index() == i) {
                continue;
            }

            moved = true;

            for (int j = 0; j < columnCount; j++) {
                tempRow[j] = _columnList.get(j).get(i);
            }

            int previous = i;
            int next = sortedPairs[i].index();

            do {
                for (int j = 0; j < columnCount; j++) {
                    final List<Object> column = _columnList.get(j);
                    column.set(previous, column.get(next));
                }

                placed[next] = true;

                previous = next;
                next = sortedPairs[next].index();
            } while (next != i);

            for (int j = 0; j < columnCount; j++) {
                _columnList.get(j).set(previous, tempRow[j]);
            }

            placed[i] = true;
        }

        return moved;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset topBy(final String columnName, final int n) throws IllegalArgumentException {
        return topBy(columnName, n, Comparators.nullsFirst());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset topBy(final String columnName, final int n, final Comparator<?> cmp) throws IllegalArgumentException {

        final int columnIndex = checkColumnName(columnName);
        if (n < 1) {
            throw new IllegalArgumentException("'n' cannot be less than 1");
        }

        N.checkArgNotNull(cmp, cs.cmp);
        final int size = size();

        if (n >= size) {
            return copyAsTransformationResult();
        }

        final Comparator<Indexed<Object>> pairCmp = createComparatorForIndexedObject(cmp);

        final List<Object> orderByColumn = _columnList.get(columnIndex);

        return top(n, pairCmp, orderByColumn::get);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset topBy(final Collection<String> columnNames, final int n) throws IllegalArgumentException {
        return topBy(columnNames, n, Comparators.OBJECT_ARRAY_COMPARATOR);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset topBy(final Collection<String> columnNames, final int n, final Comparator<? super Object[]> cmp) throws IllegalArgumentException {

        final int[] sortByColumnIndexes = checkColumnNames(columnNames);
        if (n < 1) {
            throw new IllegalArgumentException("'n' cannot be less than 1");
        }

        N.checkArgNotNull(cmp, cs.cmp);
        final int size = size();

        if (n >= size) {
            return copyAsTransformationResult();
        }

        final Comparator<Indexed<Object[]>> pairCmp = createComparatorForIndexedObjectArray(cmp);

        final int sortByColumnCount = sortByColumnIndexes.length;

        // Plain arrays, not pooled ones - see sort(Collection, Comparator, boolean): the caller's comparator
        // receives these and may legitimately keep a reference to them.
        return top(n, pairCmp, rowIndex -> {
            final Object[] keyRow = new Object[sortByColumnCount];

            for (int i = 0; i < sortByColumnCount; i++) {
                keyRow[i] = _columnList.get(sortByColumnIndexes[i]).get(rowIndex);
            }

            return keyRow;
        });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Dataset topBy(final Collection<String> columnNames, final int n, final Function<? super DisposableObjArray, ? extends Comparable> keyExtractor)
            throws IllegalArgumentException {

        final int[] columnIndexes = checkColumnNames(columnNames);
        if (n < 1) {
            throw new IllegalArgumentException("'n' cannot be less than 1");
        }

        N.checkArgNotNull(keyExtractor, cs.keyExtractor);
        final int size = size();

        if (n >= size) {
            return copyAsTransformationResult();
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

    /**
     * Returns a full copy of this Dataset as a <i>transformation</i> result - the early-return shape of
     * {@code topBy}/{@code distinctBy} for an input the operation cannot shrink.
     *
     * <p>{@link #copy()} carries the missing-property policy over, because a copy is documented to retain it;
     * a transformation result is not (see {@link Dataset#withMissingPropertyPolicy(MissingPropertyPolicy)}:
     * "Slices, copies and clones retain the policy. Configure other transformation results explicitly").
     * Returning {@code copy()} directly made the policy of a {@code topBy} result depend on whether {@code n}
     * happened to reach the row count, so the same call answered differently on two Datasets of different
     * size.</p>
     *
     * @return a full copy carrying the default missing-property policy
     */
    private Dataset copyAsTransformationResult() {
        final RowDataset ret = (RowDataset) copy();
        ret.missingPropertyPolicy = MissingPropertyPolicy.IGNORE;

        return ret;
    }

    private <T> Dataset top(final int n, final Comparator<Indexed<T>> pairCmp, final IntFunction<T> keyFunc) {
        final int size = size();

        // Break ties by row index, later row first, so the heap root - the element evicted when a greater value
        // arrives - is always the LATEST of the tied rows and the earliest survives, as documented. Under the plain
        // value comparator the root among equal values was whichever PriorityQueue surfaced (in practice the
        // earliest inserted), so [5, 5, 5, 9] with n = 2 kept rows 1 and 3 instead of rows 0 and 3.
        final Comparator<Indexed<T>> heapCmp = (a, b) -> {
            final int c = pairCmp.compare(a, b);

            return c != 0 ? c : Integer.compare(b.index(), a.index());
        };

        final Queue<Indexed<T>> heap = new PriorityQueue<>(n, heapCmp);
        Indexed<T> pair = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            pair = Indexed.of(keyFunc.apply(rowIndex), rowIndex);

            if (heap.size() >= n) {
                if (heapCmp.compare(heap.peek(), pair) < 0) {
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

        return new RowDataset(newColumnNameList, newColumnList, _properties, true);
    }

    @Override
    public Dataset distinct() {
        return distinctBy(_columnNameList);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset distinctBy(final String columnName) throws IllegalArgumentException {
        return distinctBy(columnName, Fn.identity());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset distinctBy(final String columnName, final Function<?, ?> keyExtractor) throws IllegalArgumentException {
        final int columnIndex = checkColumnName(columnName);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0) {
            return new RowDataset(newColumnNameList, newColumnList, _properties, true);
        }

        final boolean isIdentityKeyExtractor = keyExtractor == Fn.identity();
        final Function<Object, ?> keyExtractorToUse = (Function<Object, ?>) keyExtractor;
        final Set<Object> rowSet = N.newHashSet();
        Object key = null;
        Object value = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            value = _columnList.get(columnIndex).get(rowIndex);
            key = hashKey(isIdentityKeyExtractor ? value : keyExtractorToUse.apply(value));

            if (rowSet.add(key)) {
                for (int i = 0; i < columnCount; i++) {
                    newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                }
            }
        }

        return new RowDataset(newColumnNameList, newColumnList, _properties, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset distinctBy(final Collection<String> columnNames) throws IllegalArgumentException {
        return distinctBy(columnNames, Fn.identity());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset distinctBy(final Collection<String> columnNames, final Function<? super DisposableObjArray, ?> keyExtractor)
            throws IllegalArgumentException {
        final int[] columnIndexes = checkColumnNames(columnNames);
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

        final boolean isIdentityKeyExtractor = keyExtractor == Fn.identity();

        if (N.size(columnNames) == 1 && isIdentityKeyExtractor) {
            return distinctBy(columnNames.iterator().next());
        }

        final int size = size();

        if (columnIndexes.length == 0 || size == 0) {
            return copyAsTransformationResult();
        }

        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final Set<Object> rowSet = N.newHashSet();
        Object[] row = Objectory.createObjectArray(columnIndexes.length);
        Wrapper<Object[]> rowWrapper = isIdentityKeyExtractor ? Wrapper.of(row) : null;
        final DisposableObjArray disposableArray = isIdentityKeyExtractor ? null : DisposableObjArray.wrap(row);
        Object key = null;

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int i = 0, len = columnIndexes.length; i < len; i++) {
                row[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            key = isIdentityKeyExtractor ? rowWrapper : hashKey(keyExtractor.apply(disposableArray));

            if (rowSet.add(key)) {
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }

                if (isIdentityKeyExtractor) {
                    row = Objectory.createObjectArray(columnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }
            }
        }

        if (row != null) {
            Objectory.recycle(row);
            row = null;
        }

        if (isIdentityKeyExtractor) {
            @SuppressWarnings("rawtypes")
            final Set<Wrapper<Object[]>> tmp = (Set) rowSet;

            for (final Wrapper<Object[]> rw : tmp) {
                Objectory.recycle(rw.value());
            }
        }

        return new RowDataset(newColumnNameList, newColumnList, _properties, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final Predicate<? super DisposableObjArray> filter) throws IllegalArgumentException {
        return filter(filter, size());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final Predicate<? super DisposableObjArray> filter, final int max) throws IllegalArgumentException {
        return filter(0, size(), filter, max);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final int fromRowIndex, final int toRowIndex, final Predicate<? super DisposableObjArray> filter)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return filter(fromRowIndex, toRowIndex, filter, size());
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final int fromRowIndex, final int toRowIndex, final Predicate<? super DisposableObjArray> filter, final int max)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return filter(fromRowIndex, toRowIndex, _columnNameList, filter, max);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final Tuple2<String, String> columnNames, final BiPredicate<?, ?> filter) throws IllegalArgumentException {
        return filter(columnNames, filter, size());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final Tuple2<String, String> columnNames, final BiPredicate<?, ?> filter, final int max) throws IllegalArgumentException {
        return filter(0, size(), columnNames, filter, max);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final int fromRowIndex, final int toRowIndex, final Tuple2<String, String> columnNames, final BiPredicate<?, ?> filter)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return filter(fromRowIndex, toRowIndex, columnNames, filter, size());
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final int fromRowIndex, final int toRowIndex, final Tuple2<String, String> columnNames, final BiPredicate<?, ?> filter, final int max)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(columnNames, cs.columnNames);

        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        N.checkArgNotNull(filter, cs.filter);
        N.checkArgNotNegative(max, cs.max);

        final BiPredicate<Object, Object> filterToUse = (BiPredicate<Object, Object>) filter;
        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            // A selective filter may keep very few rows; allocate storage only as matches arrive.
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0 || max == 0) {
            return new RowDataset(newColumnNameList, newColumnList, _properties, true);
        }

        int count = max;

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            if (filterToUse.test(column1.get(rowIndex), column2.get(rowIndex))) {
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }

                if (--count <= 0) {
                    break;
                }
            }
        }

        return new RowDataset(newColumnNameList, newColumnList, _properties, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final Tuple3<String, String, String> columnNames, final TriPredicate<?, ?, ?> filter) throws IllegalArgumentException {
        return filter(columnNames, filter, size());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final Tuple3<String, String, String> columnNames, final TriPredicate<?, ?, ?> filter, final int max) throws IllegalArgumentException {
        return filter(0, size(), columnNames, filter, max);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final int fromRowIndex, final int toRowIndex, final Tuple3<String, String, String> columnNames, final TriPredicate<?, ?, ?> filter)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return filter(fromRowIndex, toRowIndex, columnNames, filter, size());
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final int fromRowIndex, final int toRowIndex, final Tuple3<String, String, String> columnNames, final TriPredicate<?, ?, ?> filter,
            final int max) throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(columnNames, cs.columnNames);

        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(columnNames._3));

        N.checkArgNotNull(filter, cs.filter);
        N.checkArgNotNegative(max, cs.max);

        final TriPredicate<Object, Object, Object> filterToUse = (TriPredicate<Object, Object, Object>) filter;
        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            // A selective filter may keep very few rows; allocate storage only as matches arrive.
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0 || max == 0) {
            return new RowDataset(newColumnNameList, newColumnList, _properties, true);
        }

        int count = max;

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            if (filterToUse.test(column1.get(rowIndex), column2.get(rowIndex), column3.get(rowIndex))) {
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }

                if (--count <= 0) {
                    break;
                }
            }
        }

        return new RowDataset(newColumnNameList, newColumnList, _properties, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final String columnName, final Predicate<?> filter) throws IllegalArgumentException {
        return filter(columnName, filter, size());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final String columnName, final Predicate<?> filter, final int max) throws IllegalArgumentException {
        return filter(0, size(), columnName, filter, max);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final int fromRowIndex, final int toRowIndex, final String columnName, final Predicate<?> filter)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return filter(fromRowIndex, toRowIndex, columnName, filter, size());
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final int fromRowIndex, final int toRowIndex, final String columnName, final Predicate<?> filter, int max)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int filterColumnIndex = checkColumnName(columnName);
        N.checkArgNotNull(filter, cs.filter);

        N.checkArgNotNegative(max, cs.max);

        final Predicate<Object> filterToUse = (Predicate<Object>) filter;

        final int size = size();
        final int columnCount = columnCount();
        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            // A selective filter may keep very few rows; allocate storage only as matches arrive.
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0 || max == 0) {
            return new RowDataset(newColumnNameList, newColumnList, _properties, true);
        }

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            if (filterToUse.test(_columnList.get(filterColumnIndex).get(rowIndex))) {
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }

                if (--max <= 0) {
                    break;
                }
            }
        }

        return new RowDataset(newColumnNameList, newColumnList, _properties, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final Collection<String> columnNames, final Predicate<? super DisposableObjArray> filter) throws IllegalArgumentException {
        return filter(columnNames, filter, size());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final Collection<String> columnNames, final Predicate<? super DisposableObjArray> filter, final int max)
            throws IllegalArgumentException {
        return filter(0, size(), columnNames, filter, max);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Predicate<? super DisposableObjArray> filter) throws IndexOutOfBoundsException, IllegalArgumentException {
        return filter(fromRowIndex, toRowIndex, columnNames, filter, size());
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset filter(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Predicate<? super DisposableObjArray> filter, int max) throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int[] filterColumnIndexes = checkColumnNames(columnNames);
        N.checkArgNotNull(filter, cs.filter);

        N.checkArgNotNegative(max, cs.max);

        final int size = size();
        final int columnCount = columnCount();

        final List<String> newColumnNameList = new ArrayList<>(_columnNameList);
        final List<List<Object>> newColumnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            // A selective filter may keep very few rows; allocate storage only as matches arrive.
            newColumnList.add(new ArrayList<>());
        }

        if (size == 0 || max == 0) {
            // Empty filtered results use the transformation's default missing-property policy.
            return new RowDataset(newColumnNameList, newColumnList, _properties, true);
        }

        final int filterColumnCount = filterColumnIndexes.length;
        final Object[] values = new Object[filterColumnCount];
        final DisposableObjArray disposableArray = DisposableObjArray.wrap(values);

        for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
            for (int i = 0; i < filterColumnCount; i++) {
                values[i] = _columnList.get(filterColumnIndexes[i]).get(rowIndex);
            }

            if (filter.test(disposableArray)) {
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    newColumnList.get(columnIndex).add(_columnList.get(columnIndex).get(rowIndex));
                }

                if (--max <= 0) {
                    break;
                }
            }
        }

        return new RowDataset(newColumnNameList, newColumnList, _properties, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset mapColumn(final String fromColumnName, final String newColumnName, final String copyingColumnName, final Function<?, ?> mapper)
            throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        return mapColumn(fromColumnName, newColumnName, Array.asList(copyingColumnName), mapper);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset mapColumn(final String fromColumnName, final String newColumnName, final Collection<String> copyingColumnNames, final Function<?, ?> mapper)
            throws IllegalArgumentException {
        // Keep copied labels aligned with their resolved columns even if a mapper changes the input selection.
        final int fromColumnIndex = checkColumnName(fromColumnName);
        final List<String> copiedNames = copyingColumnNames == null ? N.emptyList() : new ArrayList<>(copyingColumnNames);
        checkMappedColumnName(newColumnName, copiedNames);

        final int[] copyingColumnIndices = N.isEmpty(copiedNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copiedNames);

        N.checkArgNotNull(mapper, cs.mapper);
        final Function<Object, Object> mapperToUse = (Function<Object, Object>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        for (final Object val : _columnList.get(fromColumnIndex)) {
            mappedColumn.add(mapperToUse.apply(val));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);

        if (N.notEmpty(copiedNames)) {
            newColumnNameList.addAll(copiedNames);

            for (final int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        newColumnNameList.add(newColumnName);
        newColumnList.add(mappedColumn);

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset mapColumns(final Tuple2<String, String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final BiFunction<?, ?, ?> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(fromColumnNames, cs.fromColumnNames);
        // Keep copied labels aligned with their resolved columns even if a mapper changes the input selection.
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<String> copiedNames = copyingColumnNames == null ? N.emptyList() : new ArrayList<>(copyingColumnNames);
        checkMappedColumnName(newColumnName, copiedNames);

        final int[] copyingColumnIndices = N.isEmpty(copiedNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copiedNames);

        N.checkArgNotNull(mapper, cs.mapper);
        final BiFunction<Object, Object, Object> mapperToUse = (BiFunction<Object, Object, Object>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            mappedColumn.add(mapperToUse.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex)));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);

        if (N.notEmpty(copiedNames)) {
            newColumnNameList.addAll(copiedNames);

            for (final int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        newColumnNameList.add(newColumnName);
        newColumnList.add(mappedColumn);

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset mapColumns(final Tuple3<String, String, String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final TriFunction<?, ?, ?, ?> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(fromColumnNames, cs.fromColumnNames);
        // Keep copied labels aligned with their resolved columns even if a mapper changes the input selection.
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<Object> fromColumn3 = _columnList.get(checkColumnName(fromColumnNames._3));
        final List<String> copiedNames = copyingColumnNames == null ? N.emptyList() : new ArrayList<>(copyingColumnNames);
        checkMappedColumnName(newColumnName, copiedNames);

        final int[] copyingColumnIndices = N.isEmpty(copiedNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copiedNames);

        N.checkArgNotNull(mapper, cs.mapper);
        final TriFunction<Object, Object, Object, Object> mapperToUse = (TriFunction<Object, Object, Object, Object>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            mappedColumn.add(mapperToUse.apply(fromColumn1.get(rowIndex), fromColumn2.get(rowIndex), fromColumn3.get(rowIndex)));
        }

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);

        if (N.notEmpty(copiedNames)) {
            newColumnNameList.addAll(copiedNames);

            for (final int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        newColumnNameList.add(newColumnName);
        newColumnList.add(mappedColumn);

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset mapColumns(final Collection<String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final Function<? super DisposableObjArray, ?> mapper) throws IllegalArgumentException {
        // Keep copied labels aligned with their resolved columns even if a mapper changes the input selection.
        final int[] fromColumnIndices = checkColumnNames(fromColumnNames);
        final List<String> copiedNames = copyingColumnNames == null ? N.emptyList() : new ArrayList<>(copyingColumnNames);
        checkMappedColumnName(newColumnName, copiedNames);

        final int[] copyingColumnIndices = N.isEmpty(copiedNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copiedNames);

        N.checkArgNotNull(mapper, cs.mapper);
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
        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);

        if (N.notEmpty(copiedNames)) {
            newColumnNameList.addAll(copiedNames);

            for (final int columnIndex : copyingColumnIndices) {
                newColumnList.add(new ArrayList<>(_columnList.get(columnIndex)));
            }
        }

        newColumnNameList.add(newColumnName);
        newColumnList.add(mappedColumn);

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset flatMapColumn(final String fromColumnName, final String newColumnName, final String copyingColumnName,
            final Function<?, ? extends Collection<?>> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        return flatMapColumn(fromColumnName, newColumnName, Array.asList(copyingColumnName), mapper);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset flatMapColumn(final String fromColumnName, final String newColumnName, final Collection<String> copyingColumnNames,
            final Function<?, ? extends Collection<?>> mapper) throws IllegalArgumentException {
        final int fromColumnIndex = checkColumnName(fromColumnName);
        checkMappedColumnName(newColumnName, copyingColumnNames);

        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        N.checkArgNotNull(mapper, cs.mapper);
        final Function<Object, Collection<Object>> mapperToUse = (Function<Object, Collection<Object>>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);

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
                        copyingColumn = newColumnList.get(i);

                        for (int j = 0, len = c.size(); j < len; j++) {
                            copyingColumn.add(val);
                        }
                    }
                }
            }
        }

        newColumnNameList.add(newColumnName);
        newColumnList.add(mappedColumn);

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset flatMapColumns(final Tuple2<String, String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final BiFunction<?, ?, ? extends Collection<?>> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(fromColumnNames, cs.fromColumnNames);
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        checkMappedColumnName(newColumnName, copyingColumnNames);

        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        N.checkArgNotNull(mapper, cs.mapper);
        final BiFunction<Object, Object, Collection<Object>> mapperToUse = (BiFunction<Object, Object, Collection<Object>>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);

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
                        copyingColumn = newColumnList.get(i);

                        for (int j = 0, len = c.size(); j < len; j++) {
                            copyingColumn.add(val);
                        }
                    }
                }
            }
        }

        newColumnNameList.add(newColumnName);
        newColumnList.add(mappedColumn);

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset flatMapColumns(final Tuple3<String, String, String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final TriFunction<?, ?, ?, ? extends Collection<?>> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(fromColumnNames, cs.fromColumnNames);
        final List<Object> fromColumn1 = _columnList.get(checkColumnName(fromColumnNames._1));
        final List<Object> fromColumn2 = _columnList.get(checkColumnName(fromColumnNames._2));
        final List<Object> fromColumn3 = _columnList.get(checkColumnName(fromColumnNames._3));
        checkMappedColumnName(newColumnName, copyingColumnNames);

        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        N.checkArgNotNull(mapper, cs.mapper);
        final TriFunction<Object, Object, Object, Collection<Object>> mapperToUse = (TriFunction<Object, Object, Object, Collection<Object>>) mapper;
        final int size = size();
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);

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
                        copyingColumn = newColumnList.get(i);

                        for (int j = 0, len = c.size(); j < len; j++) {
                            copyingColumn.add(val);
                        }
                    }
                }
            }
        }

        newColumnNameList.add(newColumnName);
        newColumnList.add(mappedColumn);

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset flatMapColumns(final Collection<String> fromColumnNames, final String newColumnName, final Collection<String> copyingColumnNames,
            final Function<? super DisposableObjArray, ? extends Collection<?>> mapper) throws IllegalArgumentException {
        final int[] fromColumnIndices = checkColumnNames(fromColumnNames);
        checkMappedColumnName(newColumnName, copyingColumnNames);

        final int[] copyingColumnIndices = N.isEmpty(copyingColumnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(copyingColumnNames);

        N.checkArgNotNull(mapper, cs.mapper);
        final Function<? super DisposableObjArray, Collection<Object>> mapperToUse = (Function<? super DisposableObjArray, Collection<Object>>) mapper;
        final int size = size();
        final int fromColumnCount = fromColumnIndices.length;
        final int copyingColumnCount = copyingColumnIndices.length;

        final List<Object> mappedColumn = new ArrayList<>(size);

        final List<String> newColumnNameList = new ArrayList<>(copyingColumnCount + 1);
        final List<List<Object>> newColumnList = new ArrayList<>(copyingColumnCount + 1);

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
                        copyingColumn = newColumnList.get(i);

                        for (int j = 0, len = c.size(); j < len; j++) {
                            copyingColumn.add(val);
                        }
                    }
                }
            }
        }

        newColumnNameList.add(newColumnName);
        newColumnList.add(mappedColumn);

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    @Override
    public Dataset copy() {
        return copy(0, size(), _columnNameList);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset copy(final Collection<String> columnNames) throws IllegalArgumentException {
        return copy(0, size(), columnNames);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public Dataset copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        return copy(fromRowIndex, toRowIndex, _columnNameList);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code fromRowIndex > toRowIndex}, or {@code toRowIndex > size()}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset copy(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);

        return copy(fromRowIndex, toRowIndex, columnNames, this.checkColumnNames(columnNames), true);
    }

    private RowDataset copy(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final int[] columnIndexes,
            final boolean copyProperties) {
        checkRowIndex(fromRowIndex, toRowIndex);

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

        // _properties directly, not copyProperties(_properties): the constructor already copies what it is
        // given, so pre-copying here built and threw away one map per copy - on the hot path of split().
        final RowDataset copy = new RowDataset(newColumnNameList, newColumnList, copyProperties ? _properties : null, true);
        copy.missingPropertyPolicy = missingPropertyPolicy;
        return copy;
    }

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException if the Kryo library required for deep cloning is unavailable
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
    @Override
    public Dataset clone() throws UnsupportedOperationException, ConcurrentModificationException { //NOSONAR
        return clone(_isFrozen);
    }

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     */
    @Override
    public Dataset clone(final boolean freeze) throws UnsupportedOperationException, ConcurrentModificationException { //NOSONAR
        if (kryoParser == null) {
            throw new UnsupportedOperationException("Kryo library is required for deep cloning. Please add Kryo to your classpath or use copy() instead.");
        }

        checkSliceValidity();
        final RowDataset dataset = kryoParser.deepCopy(this);
        dataset._isFrozen = freeze;
        return dataset;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset innerJoin(final Dataset right, final String columnName, final String joinColumnNameOnRight) throws IllegalArgumentException {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return innerJoin(right, onColumnNames);
    }

    @Override
    public Dataset innerJoin(final Dataset right, final Map<String, String> onColumnNames) {
        return join(right, onColumnNames, false);
    }

    @Override
    public Dataset innerJoin(final Dataset right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType) {
        return join(right, onColumnNames, newColumnName, newColumnType, false);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Dataset innerJoin(final Dataset right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(collSupplier, cs.collSupplier);

        return join(right, onColumnNames, newColumnName, newColumnType, collSupplier, false);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset leftJoin(final Dataset right, final String columnName, final String joinColumnNameOnRight) throws IllegalArgumentException {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return leftJoin(right, onColumnNames);
    }

    @Override
    public Dataset leftJoin(final Dataset right, final Map<String, String> onColumnNames) {
        return join(right, onColumnNames, true);
    }

    private Dataset join(final Dataset right, final Map<String, String> onColumnNames, final boolean isLeftJoin) {
        checkJoinOnColumnNames(right, onColumnNames);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());
            final List<String> rightColumnNames = new ArrayList<>(right.columnNames());
            final int newColumnSize = columnCount() + rightColumnNames.size();
            final List<String> newColumnNameList = new ArrayList<>(newColumnSize);
            final List<List<Object>> newColumnList = new ArrayList<>(newColumnSize);

            initNewColumnList(newColumnNameList, newColumnList, _columnNameList, rightColumnNames);

            if (isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);
            final List<Object>[] rightColumns = resolveColumns(right, rightColumnIndexes);
            List<Integer> rightRowIndexList = null;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                join(newColumnList, isLeftJoin, leftRowIndex, rightRowIndexList, rightColumnIndexes, rightColumns);
            }

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];
            final List<String> rightColumnNames = new ArrayList<>(right.columnNames());

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, _columnNameList, rightColumnNames);

            if (isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();

            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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
            final List<Object>[] rightColumns = resolveColumns(right, rightColumnIndexes);
            row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
            rowWrapper = Wrapper.of(row);
            List<Integer> rightRowIndexList = null;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.getValue(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                rightRowIndexList = joinColumnRightRowIndexMap.get(rowWrapper);

                join(newColumnList, isLeftJoin, leftRowIndex, rightRowIndexList, rightColumnIndexes, rightColumns);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            for (final Wrapper<Object[]> rw : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }
    }

    /**
     * Resolves each of {@code columnIndexes} to its column in {@code right}, once.
     *
     * <p>{@code Dataset.getColumn(int)} allocates a fresh {@code ImmutableList} wrapper on every call, so
     * calling it from inside a per-left-row join loop allocated one wrapper per (left row x selected right
     * column). The set operations already hoist their column views this way.</p>
     *
     * @param right the right-hand Dataset
     * @param columnIndexes the column indexes to resolve
     * @return the resolved columns, in the order of {@code columnIndexes}
     */
    @SuppressWarnings("unchecked")
    private static List<Object>[] resolveColumns(final Dataset right, final int[] columnIndexes) {
        final List<Object>[] columns = new List[columnIndexes.length];

        for (int i = 0; i < columnIndexes.length; i++) {
            columns[i] = right.getColumn(columnIndexes[i]);
        }

        return columns;
    }

    private void join(final List<List<Object>> newColumnList, final boolean isLeftJoin, final int leftRowIndex, final List<Integer> rightRowIndexList,
            final int[] rightColumnIndexes, final List<Object>[] rightColumns) {
        final int leftColumnLength = columnCount();

        if (N.notEmpty(rightRowIndexList)) {
            final int rightRowSize = rightRowIndexList.size();
            List<Object> newColumn = null;
            Object val = null;

            for (int i = 0; i < leftColumnLength; i++) {
                val = _columnList.get(i).get(leftRowIndex);
                newColumn = newColumnList.get(i);

                for (int j = 0; j < rightRowSize; j++) {
                    newColumn.add(val);
                }
            }

            for (int i = 0, rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                newColumn = newColumnList.get(leftColumnLength + i);
                final List<Object> column = rightColumns[i];

                for (final int rightRowIndex : rightRowIndexList) {
                    newColumn.add(column.get(rightRowIndex));
                }
            }
        } else if (isLeftJoin) {
            for (int i = 0; i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            for (int i = 0, rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                newColumnList.get(leftColumnLength + i).add(null);
            }
        }
    }

    private Dataset join(final Dataset right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final boolean isLeftJoin) {
        checkJoinOnColumnNames(right, onColumnNames);
        checkNewColumnName(newColumnName);
        checkNewColumnType(newColumnType);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());
            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            if (isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            if (isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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
                    row[i] = this.getValue(leftRowIndex, leftJoinColumnIndexes[i]);
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

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }
    }

    private void join(final List<List<Object>> newColumnList, final Dataset right, final boolean isLeftJoin, final Class<?> newColumnType,
            final int newColumnIndex, final int leftRowIndex, final List<Integer> rightRowIndexList) {
        if (N.notEmpty(rightRowIndexList)) {
            final int rightRowSize = rightRowIndexList.size();
            final int leftColumnLength = columnCount();
            List<Object> column = null;
            Object val = null;

            for (int i = 0; i < leftColumnLength; i++) {
                column = newColumnList.get(i);
                val = _columnList.get(i).get(leftRowIndex);

                for (int j = 0; j < rightRowSize; j++) {
                    column.add(val);
                }
            }

            for (final int rightRowIndex : rightRowIndexList) {
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
     * @throws IllegalArgumentException if {@code right} is null, or {@code onColumnNames} is null or empty
     */
    private void checkJoinOnColumnNames(final Dataset right, final Map<String, String> onColumnNames) throws IllegalArgumentException {
        N.checkArgNotNull(right, cs.right);

        if (N.isEmpty(onColumnNames)) {
            throw new IllegalArgumentException("The joining column names cannot be null or empty");
        }
    }

    /**
     * @throws IllegalArgumentException if {@code joinColumnNameOnRight} is not a column of {@code right}
     */
    private int checkRightJoinColumnName(final Dataset right, final String joinColumnNameOnRight) throws IllegalArgumentException {
        if (!right.containsColumn(joinColumnNameOnRight)) {
            throw new IllegalArgumentException(
                    "The specified column: " + joinColumnNameOnRight + " is not included in the right Dataset: " + right.columnNames());
        }

        return right.getColumnIndex(joinColumnNameOnRight);
    }

    /**
     * @throws IllegalArgumentException if {@code newColumnName} is null, empty, or already present in this dataset
     */
    private void checkNewColumnName(final String newColumnName) throws IllegalArgumentException {
        if (Strings.isEmpty(newColumnName)) {
            throw new IllegalArgumentException("The new column name cannot be null or empty");
        }

        if (containsColumn(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is already included in this Dataset: " + _columnNameList);
        }
    }

    /**
     * Validates the name of the column a {@code mapColumn(s)}/{@code flatMapColumn(s)} result adds. Unlike
     * {@link #checkNewColumnName(String)} it does not reject a name that exists in <i>this</i> Dataset - the result
     * is a new Dataset holding only the copied columns plus the mapped one - but it must not be empty and must not
     * duplicate one of the copied columns, which the {@code RowDataset} constructor would otherwise report as a
     * bare "Duplicated column names found in: [b, b]".
     * @throws IllegalArgumentException if {@code newColumnName} is null, empty, or included in {@code copyingColumnNames}
     */
    private static void checkMappedColumnName(final String newColumnName, final Collection<String> copyingColumnNames) throws IllegalArgumentException {
        if (Strings.isEmpty(newColumnName)) {
            throw new IllegalArgumentException("The new column name cannot be null or empty");
        }

        if (N.notEmpty(copyingColumnNames) && copyingColumnNames.contains(newColumnName)) {
            throw new IllegalArgumentException("The new column name: " + newColumnName + " is also one of the copied columns: " + copyingColumnNames);
        }
    }

    /**
     * @throws IllegalArgumentException if {@code newColumnType} is null or is not an object-array, collection, map, or bean type
     */
    private void checkNewColumnType(final Class<?> newColumnType) throws IllegalArgumentException {
        N.checkArgNotNull(newColumnType, cs.newColumnType);

        final Type<?> rowType = Type.of(newColumnType);

        if (!(rowType.isObjectArray() || rowType.isCollection() || rowType.isMap() || rowType.isBean())) {
            throw new IllegalArgumentException("Unsupported new column type: " + ClassUtil.getCanonicalClassName(newColumnType)
                    + ". Only Object array, Collection, Map and bean classes are supported");
        }
    }

    private void initColumnIndexes(final int[] leftJoinColumnIndexes, final int[] rightJoinColumnIndexes, final Dataset right, // NOSONAR
            final Map<String, String> onColumnNames) {
        int i = 0;
        for (final Map.Entry<String, String> entry : onColumnNames.entrySet()) {
            leftJoinColumnIndexes[i] = checkColumnName(entry.getKey());
            // checkRightJoinColumnName, not right.getColumnIndex: the latter reports an unknown name as
            // "not included in this Dataset", which reads as the *left* Dataset while listing the right
            // one's columns. The dead `< 0` guard this replaces could never fire - getColumnIndex throws.
            rightJoinColumnIndexes[i] = checkRightJoinColumnName(right, entry.getValue());

            i++;
        }
    }

    private void initNewColumnList(final List<String> newColumnNameList, final List<List<Object>> newColumnList, final List<String> leftColumnNames,
            final List<String> rightColumnNames) {
        //    for (String rightColumnName : rightColumnNames) {
        //        if (this.containsColumn(rightColumnName)) {
        //            throw new IllegalArgumentException("The column name: " + rightColumnName + " is already included in this Dataset: " + _columnNameList);
        //        }
        //    }

        // N.newHashSet takes an expected entry count and converts it to a capacity; new HashSet<>(int) does
        // not, so the raw sum under-sized the set by a factor of 0.75 and forced a rehash while filling it.
        final Set<String> usedColumnNames = N.newHashSet(leftColumnNames.size() + rightColumnNames.size());

        for (final String columnName : leftColumnNames) {
            newColumnNameList.add(columnName);
            newColumnList.add(new ArrayList<>());
            usedColumnNames.add(columnName);
        }

        for (final String columnName : rightColumnNames) {
            String resultColumnName = columnName;

            if (!usedColumnNames.add(resultColumnName)) {
                int suffix = 2;

                do {
                    resultColumnName = columnName + "_" + suffix++;
                } while (!usedColumnNames.add(resultColumnName));
            }

            newColumnNameList.add(resultColumnName);
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
            joinColumnRightRowIndexMap.put(hashKey, N.toList(rightRowIndex));
        } else {
            rightRowIndexList.add(rightRowIndex);
        }
    }

    private Object[] putRowIndex(final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap, final Wrapper<Object[]> rowWrapper, Object[] row,
            final int rightRowIndex) {
        final List<Integer> rightRowIndexList = joinColumnRightRowIndexMap.get(rowWrapper);

        if (rightRowIndexList == null) {
            joinColumnRightRowIndexMap.put(rowWrapper, N.toList(rightRowIndex));
            row = null;
        } else {
            rightRowIndexList.add(rightRowIndex);
        }

        return row;
    }

    @Override
    public Dataset leftJoin(final Dataset right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType) {
        return join(right, onColumnNames, newColumnName, newColumnType, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Dataset leftJoin(final Dataset right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(collSupplier, cs.collSupplier);

        return join(right, onColumnNames, newColumnName, newColumnType, collSupplier, true);
    }

    @SuppressWarnings("rawtypes")
    private Dataset join(final Dataset right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final boolean isLeftJoin) {
        checkJoinOnColumnNames(right, onColumnNames);
        checkNewColumnName(newColumnName);
        checkNewColumnType(newColumnType);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            if (isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            List<Integer> rightRowIndexList = null;
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                putRowIndex(joinColumnRightRowIndexMap, hashKey, rightRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;

            for (int leftRowIndex = 0, size = size(); leftRowIndex < size; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                rightRowIndexList = joinColumnRightRowIndexMap.get(hashKey);

                join(newColumnList, right, isLeftJoin, newColumnType, collSupplier, newColumnIndex, leftRowIndex, rightRowIndexList);
            }

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            if (isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new HashMap<>();
            List<Integer> rightRowIndexList = null;
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }
    }

    @SuppressWarnings("rawtypes")
    private void join(final List<List<Object>> newColumnList, final Dataset right, final boolean isLeftJoin, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final int leftRowIndex, final List<Integer> rightRowIndexList) {
        if (N.notEmpty(rightRowIndexList) || isLeftJoin) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }
        }

        if (N.notEmpty(rightRowIndexList)) {
            final Collection<Object> coll = checkSupplierResult(collSupplier.apply(rightRowIndexList.size()), "collSupplier");

            for (final int rightRowIndex : rightRowIndexList) {
                coll.add(right.getRow(rightRowIndex, newColumnType));
            }

            newColumnList.get(newColumnIndex).add(coll);
        } else if (isLeftJoin) {
            newColumnList.get(newColumnIndex).add(null);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset rightJoin(final Dataset right, final String columnName, final String joinColumnNameOnRight) throws IllegalArgumentException {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return rightJoin(right, onColumnNames);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset rightJoin(final Dataset right, final Map<String, String> onColumnNames) throws IllegalArgumentException {
        checkJoinOnColumnNames(right, onColumnNames);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());
            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final List<String> rightColumnNames = right.columnNames();

            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, leftColumnNames, rightColumnNames);

            if (right.isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            List<Integer> leftRowIndexList = null;
            Object hashKey = null;

            for (int leftRowIndex = 0, leftDatasetSize = size(); leftRowIndex < leftDatasetSize; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final int[] leftColumnIndexes = getColumnIndexes(leftColumnNames);
            final int[] rightColumnIndexes = right.getColumnIndexes(rightColumnNames);

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                leftRowIndexList = joinColumnLeftRowIndexMap.get(hashKey);

                rightJoin(newColumnList, right, rightRowIndex, rightColumnIndexes, leftColumnIndexes, leftRowIndexList);
            }

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        } else {
            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final List<String> rightColumnNames = right.columnNames();
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, leftColumnNames, rightColumnNames);

            if (right.isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int leftRowIndex = 0, leftDatasetSize = size(); leftRowIndex < leftDatasetSize; leftRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.getValue(leftRowIndex, leftJoinColumnIndexes[i]);
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

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }
    }

    private void rightJoin(final List<List<Object>> newColumnList, final Dataset right, final int rightRowIndex, final int[] rightColumnIndexes,
            final int[] leftColumnIndexes, final List<Integer> leftRowIndexList) {
        if (N.notEmpty(leftRowIndexList)) {
            for (final int leftRowIndex : leftRowIndexList) {
                for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                    newColumnList.get(i).add(this.getValue(leftRowIndex, leftColumnIndexes[i]));
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
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset rightJoin(final Dataset right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType)
            throws IllegalArgumentException {
        checkJoinOnColumnNames(right, onColumnNames);
        checkNewColumnName(newColumnName);
        checkNewColumnType(newColumnType);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());

            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + 1);

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, leftColumnNames, newColumnName);

            if (right.isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int leftRowIndex = 0, leftDatasetSize = size(); leftRowIndex < leftDatasetSize; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final int newColumnIndex = newColumnList.size() - 1;
            final int[] leftColumnIndexes = getColumnIndexes(leftColumnNames);
            List<Integer> leftRowIndexList = null;

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
                hashKey = hashKey(rightJoinColumn.get(rightRowIndex));
                leftRowIndexList = joinColumnLeftRowIndexMap.get(hashKey);

                rightJoin(newColumnList, right, newColumnType, newColumnIndex, rightRowIndex, leftRowIndexList, leftColumnIndexes);
            }

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        } else {
            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + 1);

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, leftColumnNames, newColumnName);

            if (right.isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int leftRowIndex = 0, leftDatasetSize = size(); leftRowIndex < leftDatasetSize; leftRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.getValue(leftRowIndex, leftJoinColumnIndexes[i]);
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

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }
    }

    private void rightJoin(final List<List<Object>> newColumnList, final Dataset right, final Class<?> newColumnType, final int newColumnIndex,
            final int rightRowIndex, final List<Integer> leftRowIndexList, final int[] leftColumnIndexes) {
        if (N.notEmpty(leftRowIndexList)) {
            for (final int leftRowIndex : leftRowIndexList) {
                for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                    newColumnList.get(i).add(this.getValue(leftRowIndex, leftColumnIndexes[i]));
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

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Dataset rightJoin(final Dataset right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException {
        checkJoinOnColumnNames(right, onColumnNames);
        checkNewColumnName(newColumnName);
        checkNewColumnType(newColumnType);
        N.checkArgNotNull(collSupplier, cs.collSupplier);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());

            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + 1);

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, leftColumnNames, newColumnName);

            if (right.isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object hashKey = null;

            for (int leftRowIndex = 0, leftDatasetSize = size(); leftRowIndex < leftDatasetSize; leftRowIndex++) {
                hashKey = hashKey(leftJoinColumn.get(leftRowIndex));
                putRowIndex(joinColumnLeftRowIndexMap, hashKey, leftRowIndex);
            }

            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        } else {
            final List<String> leftColumnNames = new ArrayList<>(_columnNameList);
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(leftColumnNames.size() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(leftColumnNames.size() + 1);

            initNewColumnListForRightJoin(newColumnNameList, newColumnList, leftColumnNames, newColumnName);

            if (right.isEmpty()) {
                return new RowDataset(newColumnNameList, newColumnList, null, true);
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnLeftRowIndexMap = new HashMap<>();
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int leftRowIndex = 0, leftDatasetSize = size(); leftRowIndex < leftDatasetSize; leftRowIndex++) {
                if (row == null) {
                    row = Objectory.createObjectArray(leftJoinColumnIndexes.length);
                    rowWrapper = Wrapper.of(row);
                }

                for (int i = 0, len = leftJoinColumnIndexes.length; i < len; i++) {
                    row[i] = this.getValue(leftRowIndex, leftJoinColumnIndexes[i]);
                }

                row = putRowIndex(joinColumnLeftRowIndexMap, rowWrapper, row, leftRowIndex);
            }

            if (row != null) {
                Objectory.recycle(row);
                row = null;
            }

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }
    }

    @SuppressWarnings("rawtypes")
    private void rightJoin(final List<List<Object>> newColumnList, final Dataset right, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final int[] leftColumnIndexes, final List<Integer> leftRowIndexList,
            final List<Integer> rightRowIndexList) {
        if (N.notEmpty(leftRowIndexList)) {
            for (final int leftRowIndex : leftRowIndexList) {
                for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                    newColumnList.get(i).add(this.getValue(leftRowIndex, leftColumnIndexes[i]));
                }

                final Collection<Object> coll = checkSupplierResult(collSupplier.apply(rightRowIndexList.size()), "collSupplier");

                for (final int rightRowIndex : rightRowIndexList) {
                    coll.add(right.getRow(rightRowIndex, newColumnType));
                }

                newColumnList.get(newColumnIndex).add(coll);
            }
        } else {
            for (int i = 0, leftColumnLength = leftColumnIndexes.length; i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            final Collection<Object> coll = checkSupplierResult(collSupplier.apply(rightRowIndexList.size()), "collSupplier");

            for (final int rightRowIndex : rightRowIndexList) {
                coll.add(right.getRow(rightRowIndex, newColumnType));
            }

            newColumnList.get(newColumnIndex).add(coll);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset fullJoin(final Dataset right, final String columnName, final String joinColumnNameOnRight) throws IllegalArgumentException {
        final Map<String, String> onColumnNames = N.asMap(columnName, joinColumnNameOnRight);

        return fullJoin(right, onColumnNames);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset fullJoin(final Dataset right, final Map<String, String> onColumnNames) throws IllegalArgumentException {
        checkJoinOnColumnNames(right, onColumnNames);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());
            final List<String> rightColumnNames = new ArrayList<>(right.columnNames());

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, _columnNameList, rightColumnNames);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            // LinkedHashMap only for symmetry with the sibling join branches: the unmatched tail below is
            // sorted by right row index, so nothing in this branch observes this map's iteration order.
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();
            List<Integer> rightRowIndexList = null;
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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

            // The unmatched right rows are appended in right-row order, as rightJoin emits them. The per-key
            // index lists are ascending and disjoint, so collecting and sorting them restores that order.
            // Sorting only the unmatched indexes stays free when every right row matched - the common case -
            // which an O(right.size()) boolean[] mark-and-walk would not, so it is preferred here.
            final List<Integer> unmatchedRightRowIndexes = new ArrayList<>();

            for (final Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexSet.contains(rightRowIndexEntry.getKey())) {
                    unmatchedRightRowIndexes.addAll(rightRowIndexEntry.getValue());
                }
            }

            N.sort(unmatchedRightRowIndexes);

            fullJoin(newColumnList, right, unmatchedRightRowIndexes, rightColumnIndexes);

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        } else {
            final int[] leftJoinColumnIndexes = new int[onColumnNames.size()];
            final int[] rightJoinColumnIndexes = new int[onColumnNames.size()];
            final List<String> rightColumnNames = new ArrayList<>(right.columnNames());

            initColumnIndexes(leftJoinColumnIndexes, rightJoinColumnIndexes, right, onColumnNames);

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + rightColumnNames.size());
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + rightColumnNames.size());

            initNewColumnList(newColumnNameList, newColumnList, _columnNameList, rightColumnNames);

            final Map<Wrapper<Object[]>, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();
            List<Integer> rightRowIndexList = null;
            Object[] row = null;
            Wrapper<Object[]> rowWrapper = null;

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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
                    row[i] = this.getValue(leftRowIndex, leftJoinColumnIndexes[i]);
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

            // The unmatched right rows are appended in right-row order, as rightJoin emits them. The per-key
            // index lists are ascending and disjoint, so collecting and sorting them restores that order.
            // Sorting only the unmatched indexes stays free when every right row matched - the common case -
            // which an O(right.size()) boolean[] mark-and-walk would not, so it is preferred here.
            final List<Integer> unmatchedRightRowIndexes = new ArrayList<>();

            for (final Map.Entry<Wrapper<Object[]>, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexMap.containsKey(rightRowIndexEntry.getKey())) {
                    unmatchedRightRowIndexes.addAll(rightRowIndexEntry.getValue());
                }
            }

            N.sort(unmatchedRightRowIndexes);

            fullJoin(newColumnList, right, unmatchedRightRowIndexes, rightColumnIndexes);

            for (final Wrapper<Object[]> rw : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            for (final Wrapper<Object[]> rw : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }
    }

    private void fullJoin(final List<List<Object>> newColumnList, final Dataset right, final List<Integer> rightRowIndexList, final int[] rightColumnIndexes) {
        for (final int rightRowIndex : rightRowIndexList) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            for (int i = 0, leftColumnLength = columnCount(), rightColumnLength = rightColumnIndexes.length; i < rightColumnLength; i++) {
                newColumnList.get(leftColumnLength + i).add(right.get(rightRowIndex, rightColumnIndexes[i]));
            }
        }
    }

    private void fullJoin(final List<List<Object>> newColumnList, final Dataset right, final int leftRowIndex, final List<Integer> rightRowIndexList,
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

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset fullJoin(final Dataset right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType)
            throws IllegalArgumentException {
        checkJoinOnColumnNames(right, onColumnNames);
        checkNewColumnName(newColumnName);
        checkNewColumnType(newColumnType);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            // LinkedHashMap only for symmetry with the sibling join branches: the unmatched tail below is
            // sorted by right row index, so nothing in this branch observes this map's iteration order.
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>();
            List<Integer> rightRowIndexList = null;
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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

            // The unmatched right rows are appended in right-row order, as rightJoin emits them. The per-key
            // index lists are ascending and disjoint, so collecting and sorting them restores that order.
            // Sorting only the unmatched indexes stays free when every right row matched - the common case -
            // which an O(right.size()) boolean[] mark-and-walk would not, so it is preferred here.
            final List<Integer> unmatchedRightRowIndexes = new ArrayList<>();

            for (final Map.Entry<Object, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexSet.contains(rightRowIndexEntry.getKey())) {
                    unmatchedRightRowIndexes.addAll(rightRowIndexEntry.getValue());
                }
            }

            N.sort(unmatchedRightRowIndexes);

            fullJoin(newColumnList, right, newColumnType, newColumnIndex, unmatchedRightRowIndexes);

            return new RowDataset(newColumnNameList, newColumnList, null, true);
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

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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
                    row[i] = this.getValue(leftRowIndex, leftJoinColumnIndexes[i]);
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

            // The unmatched right rows are appended in right-row order, as rightJoin emits them. The per-key
            // index lists are ascending and disjoint, so collecting and sorting them restores that order.
            // Sorting only the unmatched indexes stays free when every right row matched - the common case -
            // which an O(right.size()) boolean[] mark-and-walk would not, so it is preferred here.
            final List<Integer> unmatchedRightRowIndexes = new ArrayList<>();

            for (final Map.Entry<Wrapper<Object[]>, List<Integer>> rightRowIndexEntry : joinColumnRightRowIndexMap.entrySet()) {
                if (!joinColumnLeftRowIndexMap.containsKey(rightRowIndexEntry.getKey())) {
                    unmatchedRightRowIndexes.addAll(rightRowIndexEntry.getValue());
                }
            }

            N.sort(unmatchedRightRowIndexes);

            fullJoin(newColumnList, right, newColumnType, newColumnIndex, unmatchedRightRowIndexes);

            for (final Wrapper<Object[]> rw : joinColumnRightRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            for (final Wrapper<Object[]> rw : joinColumnLeftRowIndexMap.keySet()) {
                Objectory.recycle(rw.value());
            }

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }
    }

    private void fullJoin(final List<List<Object>> newColumnList, final Dataset right, final Class<?> newColumnType, final int newColumnIndex,
            final List<Integer> rightRowIndexList) {
        for (final int rightRowIndex : rightRowIndexList) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(null);
            }

            newColumnList.get(newColumnIndex).add(right.getRow(rightRowIndex, newColumnType));
        }
    }

    private void fullJoin(final List<List<Object>> newColumnList, final Dataset right, final Class<?> newColumnType, final int newColumnIndex,
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

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Dataset fullJoin(final Dataset right, final Map<String, String> onColumnNames, final String newColumnName, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier) throws IllegalArgumentException {
        checkJoinOnColumnNames(right, onColumnNames);
        checkNewColumnName(newColumnName);
        checkNewColumnType(newColumnType);
        N.checkArgNotNull(collSupplier, cs.collSupplier);

        if (onColumnNames.size() == 1) {
            final Map.Entry<String, String> onColumnEntry = onColumnNames.entrySet().iterator().next();
            final int leftJoinColumnIndex = checkColumnName(onColumnEntry.getKey());
            final int rightJoinColumnIndex = checkRightJoinColumnName(right, onColumnEntry.getValue());

            final List<String> newColumnNameList = new ArrayList<>(columnCount() + 1);
            final List<List<Object>> newColumnList = new ArrayList<>(columnCount() + 1);

            initNewColumnList(newColumnNameList, newColumnList, newColumnName);

            final List<Object> leftJoinColumn = this.getColumn(leftJoinColumnIndex);
            final List<Object> rightJoinColumn = right.getColumn(rightJoinColumnIndex);
            final Map<Object, List<Integer>> joinColumnRightRowIndexMap = new LinkedHashMap<>(); // insertion order: unmatched right rows are appended by iterating this map
            Object hashKey = null;

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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

            return new RowDataset(newColumnNameList, newColumnList, null, true);
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

            for (int rightRowIndex = 0, rightDatasetSize = right.size(); rightRowIndex < rightDatasetSize; rightRowIndex++) {
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
                    row[i] = this.getValue(leftRowIndex, leftJoinColumnIndexes[i]);
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

            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }
    }

    @SuppressWarnings("rawtypes")
    private void fullJoin(final List<List<Object>> newColumnList, final Dataset right, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final List<Integer> rightRowIndexList) {
        for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
            newColumnList.get(i).add(null);
        }

        final Collection<Object> coll = checkSupplierResult(collSupplier.apply(rightRowIndexList.size()), "collSupplier");

        for (final int rightRowIndex : rightRowIndexList) {
            coll.add(right.getRow(rightRowIndex, newColumnType));
        }

        newColumnList.get(newColumnIndex).add(coll);
    }

    @SuppressWarnings("rawtypes")
    private void fullJoin(final List<List<Object>> newColumnList, final Dataset right, final Class<?> newColumnType,
            final IntFunction<? extends Collection> collSupplier, final int newColumnIndex, final int leftRowIndex, final List<Integer> rightRowIndexList) {
        if (N.notEmpty(rightRowIndexList)) {
            for (int i = 0, leftColumnLength = columnCount(); i < leftColumnLength; i++) {
                newColumnList.get(i).add(_columnList.get(i).get(leftRowIndex));
            }

            final Collection<Object> coll = checkSupplierResult(collSupplier.apply(rightRowIndexList.size()), "collSupplier");

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

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset semiJoin(final Dataset other) throws IllegalArgumentException {
        return semiJoin(other, getKeyColumnNames(other));
    }

    @Override
    public Dataset semiJoin(final Dataset other, final Collection<String> keyColumnNames) {
        return removeAll(other, keyColumnNames, false, RowSetOperation.SEMI_JOIN);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset antiJoin(final Dataset other) throws IllegalArgumentException {
        return antiJoin(other, getKeyColumnNames(other));
    }

    @Override
    public Dataset antiJoin(final Dataset other, final Collection<String> keyColumnNames) {
        return removeAll(other, keyColumnNames, false, RowSetOperation.ANTI_JOIN);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset union(final Dataset other) throws IllegalArgumentException {
        return union(other, true);
    }

    @Override
    public Dataset union(final Dataset other, final boolean requiresSameColumns) {
        return unionBy(other, getKeyColumnNames(other), requiresSameColumns);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset unionBy(final Dataset other, final Collection<String> keyColumnNames) throws IllegalArgumentException {
        return unionBy(other, keyColumnNames, false);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset unionBy(final Dataset other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) throws IllegalArgumentException {
        checkColumnNames(other, keyColumnNames, requiresSameColumns);

        final Set<String> newColumnNameSet = new LinkedHashSet<>(_columnNameList);
        newColumnNameSet.addAll(other.columnNames());

        final List<String> newColumnNameList = new ArrayList<>(newColumnNameSet);
        final int newColumnCount = newColumnNameList.size();
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

        // Distinct output can be tiny even for large inputs. Grow with actual output instead of
        // reserving the summed input size, which can also overflow int.

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>());
        }

        final Dataset result = new RowDataset(newColumnNameList, newColumnList, null, true);

        if (isEmpty() && other.isEmpty()) {
            return result;
        }

        final int thisColumnCount = columnCount();
        final int otherColumnCount = other.columnCount();
        final int keyColumnCount = keyColumnNames.size();

        if (keyColumnCount == 1) {
            final String keyColumnName = N.firstOrNullIfEmpty(keyColumnNames);
            final Set<Object> addedRowKeys = new HashSet<>();

            if (size() > 0) {
                final int keyColumnIndex = getColumnIndex(keyColumnName);
                final List<Object> keyColumn = _columnList.get(keyColumnIndex);

                for (int rowIndex = 0, rowCount = size(); rowIndex < rowCount; rowIndex++) {
                    if (addedRowKeys.add(hashKey(keyColumn.get(rowIndex)))) {
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
                final int[] otherNewColumnIndexes = result.getColumnIndexes(other.columnNames());
                final List<Object>[] columnsInOther = new List[otherColumnCount];

                for (int i = 0; i < otherColumnCount; i++) {
                    columnsInOther[i] = other.getColumn(i);
                }

                final int keyColumnIndexInOther = other.getColumnIndex(keyColumnName);
                final List<Object> keyColumnInOther = other.getColumn(keyColumnIndexInOther);
                int cnt = 0;

                for (int rowIndex = 0, rowCount = other.size(); rowIndex < rowCount; rowIndex++) {
                    if (addedRowKeys.add(hashKey(keyColumnInOther.get(rowIndex)))) {

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
            final Set<Wrapper<Object[]>> addedRowKeys = new HashSet<>();
            Object[] keyRow = null;
            Wrapper<Object[]> keyRowWrapper = null;

            if (size() > 0) {
                final int[] keyColumnIndexes = getColumnIndexes(keyColumnNames);
                final List<Object>[] keyColumns = new List[keyColumnCount];

                for (int i = 0; i < keyColumnCount; i++) {
                    keyColumns[i] = _columnList.get(keyColumnIndexes[i]);
                }

                for (int rowIndex = 0, rowCount = size(); rowIndex < rowCount; rowIndex++) {
                    if (keyRow == null) {
                        keyRow = Objectory.createObjectArray(keyColumnCount);
                        keyRowWrapper = Wrapper.of(keyRow);
                    }

                    for (int i = 0; i < keyColumnCount; i++) {
                        keyRow[i] = keyColumns[i].get(rowIndex);
                    }

                    if (addedRowKeys.add(keyRowWrapper)) {
                        for (int i = 0; i < thisColumnCount; i++) {
                            newColumnList.get(i).add(_columnList.get(i).get(rowIndex));
                        }

                        keyRow = null;
                    }
                }

                if (keyRow != null) {
                    Objectory.recycle(keyRow);
                    keyRow = null;
                }

                if (newColumnCount > thisColumnCount && newColumnList.get(0).size() > 0) {
                    final List<Object> column = N.repeat(null, newColumnList.get(0).size());

                    for (int i = thisColumnCount; i < newColumnCount; i++) {
                        newColumnList.get(i).addAll(column);
                    }
                }
            }

            if (!other.isEmpty()) {
                final int[] otherNewColumnIndexes = result.getColumnIndexes(other.columnNames());
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
                    if (keyRow == null) {
                        keyRow = Objectory.createObjectArray(keyColumnCount);
                        keyRowWrapper = Wrapper.of(keyRow);
                    }

                    for (int i = 0; i < keyColumnCount; i++) {
                        keyRow[i] = keyColumnsInOther[i].get(rowIndex);
                    }

                    if (addedRowKeys.add(keyRowWrapper)) {

                        for (int i = 0; i < otherColumnCount; i++) {
                            newColumnList.get(otherNewColumnIndexes[i]).add(columnsInOther[i].get(rowIndex));
                        }

                        cnt++;
                        keyRow = null;
                    }
                }

                if (keyRow != null) {
                    Objectory.recycle(keyRow);
                    keyRow = null;
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

            for (final Wrapper<Object[]> rw : addedRowKeys) {
                Objectory.recycle(rw.value());
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset unionAll(final Dataset other) throws IllegalArgumentException {
        return unionAll(other, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset unionAll(final Dataset other, final boolean requiresSameColumns) throws IllegalArgumentException {
        // Like every other binary operation this result carries no properties, so start the copy without
        // them (copyProperties = false) rather than copying this dataset's only to discard them below. The
        // final reset is still required: merge() folds `other`'s properties into the result.
        final RowDataset result = copy(0, size(), _columnNameList, checkColumnNames(_columnNameList), false);
        // A binary operation's result is not a copy of the receiver, so it carries neither the receiver's
        // properties (reset below, as every other set operation does) nor its missing-property policy - which
        // the private copy(..) does carry, and which is the only way unionAll differed from union here.
        result.missingPropertyPolicy = MissingPropertyPolicy.IGNORE;
        result.merge(other, requiresSameColumns);
        result._properties = EMPTY_PROPERTIES;
        return result;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset intersect(final Dataset other) throws IllegalArgumentException {
        return intersect(other, true);
    }

    @Override
    public Dataset intersect(final Dataset other, final boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, RowSetOperation.INTERSECT);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset intersectBy(final Dataset other, final Collection<String> keyColumnNames) throws IllegalArgumentException {
        return intersectBy(other, keyColumnNames, false);
    }

    @Override
    public Dataset intersectBy(final Dataset other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, RowSetOperation.INTERSECT);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset intersectAll(final Dataset other) throws IllegalArgumentException {
        return intersectAll(other, true);
    }

    @Override
    public Dataset intersectAll(final Dataset other, final boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, RowSetOperation.INTERSECT_ALL);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset intersectAllBy(final Dataset other, final Collection<String> keyColumnNames) throws IllegalArgumentException {
        return intersectAllBy(other, keyColumnNames, false);
    }

    @Override
    public Dataset intersectAllBy(final Dataset other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, RowSetOperation.INTERSECT_ALL);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset except(final Dataset other) throws IllegalArgumentException {
        return except(other, true);
    }

    @Override
    public Dataset except(final Dataset other, final boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, RowSetOperation.EXCEPT);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset exceptBy(final Dataset other, final Collection<String> keyColumnNames) throws IllegalArgumentException {
        return exceptBy(other, keyColumnNames, false);
    }

    @Override
    public Dataset exceptBy(final Dataset other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, RowSetOperation.EXCEPT);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset exceptAll(final Dataset other) throws IllegalArgumentException {
        return exceptAll(other, true);
    }

    @Override
    public Dataset exceptAll(final Dataset other, final boolean requiresSameColumns) {
        return removeAll(other, getKeyColumnNames(other), requiresSameColumns, RowSetOperation.EXCEPT_ALL);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset exceptAllBy(final Dataset other, final Collection<String> keyColumnNames) throws IllegalArgumentException {
        return exceptAllBy(other, keyColumnNames, false);
    }

    @Override
    public Dataset exceptAllBy(final Dataset other, final Collection<String> keyColumnNames, final boolean requiresSameColumns) {
        return removeAll(other, keyColumnNames, requiresSameColumns, RowSetOperation.EXCEPT_ALL);
    }

    private enum RowSetOperation {
        INTERSECT, EXCEPT, INTERSECT_ALL, EXCEPT_ALL, SEMI_JOIN, ANTI_JOIN
    }

    private static Object rowSetKey(final Dataset data, final int row, final int[] indexes) {
        if (indexes.length == 1) {
            return hashKey(data.get(row, indexes[0]));
        }
        final Object[] values = new Object[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            values[i] = data.get(row, indexes[i]);
        }
        return Wrapper.of(values);
    }

    private Dataset removeAll(final Dataset other, final Collection<String> keyColumnNames, final boolean requiresSameColumns,
            final RowSetOperation operation) {
        checkColumnNames(other, keyColumnNames, requiresSameColumns);
        final int[] leftIndexes = getColumnIndexes(keyColumnNames);
        final int[] rightIndexes = other.getColumnIndexes(keyColumnNames);
        final boolean distinct = operation == RowSetOperation.INTERSECT || operation == RowSetOperation.EXCEPT;
        final boolean consume = operation == RowSetOperation.INTERSECT_ALL || operation == RowSetOperation.EXCEPT_ALL;
        final boolean retainMatches = operation == RowSetOperation.INTERSECT || operation == RowSetOperation.INTERSECT_ALL
                || operation == RowSetOperation.SEMI_JOIN;
        final Map<Object, Integer> remaining = new HashMap<>();
        for (int row = 0, count = other.size(); row < count; row++) {
            final Object key = rowSetKey(other, row, rightIndexes);
            if (consume) {
                remaining.merge(key, 1, Integer::sum);
            } else {
                remaining.put(key, 1);
            }
        }
        final List<List<Object>> columns = new ArrayList<>(columnCount());
        for (int i = 0; i < columnCount(); i++) {
            columns.add(new ArrayList<>());
        }
        final Set<Object> emitted = distinct ? new HashSet<>() : null;
        for (int row = 0, count = size(); row < count; row++) {
            final Object key = rowSetKey(this, row, leftIndexes);
            final int matches = remaining.getOrDefault(key, 0);
            // Consume occurrences even for EXCEPT ALL rows that are discarded. A membership set
            // cannot distinguish bag subtraction/intersection from anti/semi joins.
            if (consume && matches > 0) {
                remaining.put(key, matches - 1);
            }
            if ((matches > 0) == retainMatches && (!distinct || emitted.add(key))) {
                for (int i = 0; i < columns.size(); i++) {
                    columns.get(i).add(_columnList.get(i).get(row));
                }
            }
        }
        return new RowDataset(new ArrayList<>(_columnNameList), columns, null, true);
    }

    /**
     * @throws IllegalArgumentException if {@code other} is null, or the datasets share no column names
     *         and at least one dataset has columns
     */
    private List<String> getKeyColumnNames(final Dataset other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);

        final List<String> commonColumnNameList = new ArrayList<>(_columnNameList);
        commonColumnNameList.retainAll(other.columnNames());

        if (N.isEmpty(commonColumnNameList) && (columnCount() != 0 || other.columnCount() != 0)) {
            throw new IllegalArgumentException("These two Datasets do not have any common column names: " + _columnNameList + ", " + other.columnNames());
        }

        return commonColumnNameList;
    }

    /**
     * @throws IllegalArgumentException if {@code requiresSameColumns} is true and {@code other} has a different set of column names
     */
    private void checkIfColumnNamesAreSame(final Dataset other, final boolean requiresSameColumns) throws IllegalArgumentException {
        //noinspection SlowListContainsAll
        if (requiresSameColumns && !(columnCount() == other.columnCount() && _columnNameList.containsAll(other.columnNames()))) {
            throw new IllegalArgumentException("These two Datasets do not have the same column names: " + _columnNameList + ", " + other.columnNames());
        }
    }

    /**
     * @throws IllegalArgumentException if {@code other} or {@code keyColumnNames} is null, the selection is empty
     *         while either dataset has columns, a selected column is repeated or absent from either dataset,
     *         or {@code requiresSameColumns} is true and the datasets have different column names
     */
    private void checkColumnNames(final Dataset other, final Collection<String> keyColumnNames, final boolean requiresSameColumns)
            throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(keyColumnNames, cs.keyColumnNames);
        N.checkArgument(!keyColumnNames.isEmpty() || columnCount() == 0 && other.columnCount() == 0, "keyColumnNames cannot be empty");

        N.checkArgument(containsAllColumns(keyColumnNames), "This Dataset={} does not contain all keyColumnNames={}", columnNames(), keyColumnNames);

        N.checkArgument(other.containsAllColumns(keyColumnNames), "Other Dataset={} does not contain all keyColumnNames={}", other.columnNames(),
                keyColumnNames);

        // Same rule as every other column selection: a key column named twice is rejected, not silently ignored.
        checkNoDuplicateSelection(keyColumnNames, getColumnIndexes(keyColumnNames));

        checkIfColumnNamesAreSame(other, requiresSameColumns);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArithmeticException if the product of the row counts exceeds {@link Integer#MAX_VALUE}
     */
    @Override
    public Dataset cartesianProduct(final Dataset other) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(other, cs.other);

        final Collection<String> tmp = N.intersection(_columnNameList, other.columnNames());
        if (N.notEmpty(tmp)) {
            throw new IllegalArgumentException(tmp + " are included in both Datasets: " + _columnNameList + " : " + other.columnNames());
        }

        final int aSize = size();
        final int bSize = other.size();
        final int aColumnCount = columnCount();
        final int bColumnCount = other.columnCount();

        final int newColumnCount = aColumnCount + bColumnCount;

        // Check for integer overflow in multiplication
        if (aSize != 0 && bSize != 0 && bSize > Integer.MAX_VALUE / aSize) {
            throw new ArithmeticException("Cartesian product would result in too many rows (integer overflow): " + aSize + " * " + bSize);
        }

        final int newRowCount = aSize * bSize;

        final List<String> newColumnNameList = new ArrayList<>(newColumnCount);
        newColumnNameList.addAll(_columnNameList);
        newColumnNameList.addAll(other.columnNames());

        final List<List<Object>> newColumnList = new ArrayList<>();

        for (int i = 0; i < newColumnCount; i++) {
            newColumnList.add(new ArrayList<>(newRowCount));
        }

        if (newRowCount == 0) {
            return new RowDataset(newColumnNameList, newColumnList, null, true);
        }

        // Resolve the right-hand columns once: other.getColumn(i) allocates a fresh ImmutableList wrapper per
        // call, and the loop below used to call it once per (left row x right column).
        final List<Object>[] otherColumns = new List[bColumnCount];

        for (int columnIndex = 0; columnIndex < bColumnCount; columnIndex++) {
            otherColumns[columnIndex] = other.getColumn(columnIndex);
        }

        for (int rowIndex = 0; rowIndex < aSize; rowIndex++) {
            for (int columnIndex = 0; columnIndex < aColumnCount; columnIndex++) {
                // Append the left value bSize times directly. Filling a scratch array and calling
                // addAll(Arrays.asList(array)) made ArrayList.addAll clone that array on every
                // (left row x left column), doubling the copying for the left-hand side.
                final Object value = this.getValue(rowIndex, columnIndex);
                final List<Object> newColumn = newColumnList.get(columnIndex);

                for (int i = 0; i < bSize; i++) {
                    newColumn.add(value);
                }
            }

            for (int columnIndex = 0; columnIndex < bColumnCount; columnIndex++) {
                newColumnList.get(columnIndex + aColumnCount).addAll(otherColumns[columnIndex]);
            }
        }

        return new RowDataset(newColumnNameList, newColumnList, null, true);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> split(final int chunkSize) throws IllegalArgumentException {
        return split(chunkSize, _columnNameList);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Stream<Dataset> split(final int chunkSize, final Collection<String> columnNames) throws IllegalArgumentException {
        N.checkArgPositive(chunkSize, cs.chunkSize);
        final int[] columnIndexes = checkColumnNames(columnNames);

        // Snapshot columnNames now: the returned Stream is lazy and copy(...) below only re-reads its
        // columnNames argument when each chunk is actually consumed. Capturing the caller-owned collection
        // as-is would let it be mutated between this call and stream consumption, pairing it with the
        // columnIndexes already computed here and producing mismatched/incorrect chunks.
        final List<String> columnNamesSnapshot = new ArrayList<>(columnNames);

        final int expectedModCount = modCount;
        final int totalSize = size();

        return Stream.of(new ObjIteratorEx<>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                checkModification(expectedModCount);
                return cursor < totalSize;
            }

            /**
             * {@inheritDoc}
             * @throws ConcurrentModificationException if the dataset or an ancestor slice has been structurally modified
             * @throws NoSuchElementException if this iterator has no remaining element
             */
            @Override
            public Dataset next() throws ConcurrentModificationException, NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }
                final int end = cursor + Math.min(chunkSize, totalSize - cursor);
                final Dataset result = RowDataset.this.copy(cursor, end, columnNamesSnapshot, columnIndexes, true);
                cursor = end;
                return result;
            }

            @Override
            public long count() {
                checkModification(expectedModCount);
                final long remaining = (totalSize - cursor + (long) chunkSize - 1) / chunkSize;
                cursor = totalSize;
                return remaining;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }
                checkModification(expectedModCount);
                final long remaining = (totalSize - cursor + (long) chunkSize - 1) / chunkSize;
                // Only multiply when the resulting row offset is bounded by the remaining rows.
                cursor = n >= remaining ? totalSize : cursor + (int) (n * chunkSize);
            }
        });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public List<Dataset> splitToList(final int chunkSize) throws IllegalArgumentException {
        return splitToList(chunkSize, _columnNameList);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public List<Dataset> splitToList(final int chunkSize, final Collection<String> columnNames) throws IllegalArgumentException {
        N.checkArgPositive(chunkSize, cs.chunkSize);
        final int[] columnIndexes = checkColumnNames(columnNames);

        final List<Dataset> res = new ArrayList<>();
        final int totalSize = size();

        // Bound the increment before adding it so the final chunk cannot overflow an int.
        for (int start = 0; start < totalSize;) {
            final int end = start + Math.min(chunkSize, totalSize - start);
            res.add(copy(start, end, columnNames, columnIndexes, true));
            start = end;
        }

        return res;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset slice(final Collection<String> columnNames) throws IllegalArgumentException {
        return slice(0, size(), columnNames);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public Dataset slice(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        return slice(fromRowIndex, toRowIndex, _columnNameList);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset slice(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, size());

        Dataset ds = null;

        if (N.isEmpty(columnNames)) {
            N.checkArgument(fromRowIndex == toRowIndex, "Cannot project rows onto an empty column selection");
            // Keep the dataset properties, matching the non-empty selection path below.
            ds = new RowDataset(new ArrayList<>(), new ArrayList<>(), _properties, true);
        } else {
            final int[] columnIndexes = checkColumnNames(columnNames);
            final List<String> newColumnNames = new ArrayList<>(columnNames);
            final List<List<Object>> newColumnList = new ArrayList<>(newColumnNames.size());

            // A logical row version invalidates both full and partial slices on reordering, while
            // allocation-only changes (trimToSize) remain invisible. A nested slice delegates through
            // its source column, retaining all ancestor checks without depending on ArrayList.subList.
            for (final int columnIndex : columnIndexes) {
                final List<Object> column = _columnList.get(columnIndex);
                newColumnList.add(column instanceof SliceColumn view ? view.slice(fromRowIndex, toRowIndex - fromRowIndex)
                        : new SliceColumn(column, fromRowIndex, toRowIndex - fromRowIndex));
            }

            ds = new RowDataset(newColumnNames, newColumnList, _properties, true);
        }

        final RowDataset view = (RowDataset) ds;
        // Empty slices have no column through which to validate; retain the same logical parent check.
        // Flatten nested views so access does not build an increasingly expensive chain of list wrappers.
        view.sliceParent = sliceParent == null ? this : sliceParent;
        view.sliceParentVersion = view.sliceParent.rowVersion;
        view.sliceParentSize = view.sliceParent.size();
        view.missingPropertyPolicy = missingPropertyPolicy;
        ds.freeze();

        return ds;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Paginated<Dataset> paginate(final int pageSize) throws IllegalArgumentException {
        return paginate(_columnNameList, pageSize);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Paginated<Dataset> paginate(final Collection<String> columnNames, final int pageSize) throws IllegalArgumentException {
        checkColumnNames(columnNames);

        return new PaginatedDataset(columnNames, pageSize);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final String columnName) throws IllegalArgumentException {
        return stream(0, size(), columnName);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final String columnName)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);

        final int columnIndex = checkColumnName(columnName);

        // Wrap in a fail-fast iterator rather than handing back Stream.of(list, from, to): this overload was
        // the one lazy source in the class that ignored modCount, so a structural change between creating and
        // consuming the stream silently produced shifted or short results instead of a ConcurrentModificationException.
        return (Stream<T>) Stream.of(new ObjIteratorEx<>() {
            private final int expectedModCount = modCount;

            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                checkConcurrentModification();

                return cursor < toRowIndex;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if this iterator has no remaining element
             */
            @Override
            public Object next() throws NoSuchElementException {
                checkConcurrentModification();

                if (cursor >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return _columnList.get(columnIndex).get(cursor++);
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            @Override
            public long count() {
                checkConcurrentModification();

                final long ret = toRowIndex - cursor; //NOSONAR

                cursor = toRowIndex;

                return ret;
            }

            // toArray/toList are overridden so the bulk terminal operations still copy the backing column in
            // one shot. Without them, wrapping the column in an iterator would have turned what used to be a
            // single List copy (Stream.of(list, from, to)) into element-by-element accumulation.
            @Override
            public <A> A[] toArray(A[] output) {
                checkConcurrentModification();

                final List<Object> remaining = _columnList.get(columnIndex).subList(cursor, toRowIndex);

                output = output.length >= remaining.size() ? output : (A[]) N.newArray(output.getClass().getComponentType(), remaining.size());

                cursor = toRowIndex;

                return remaining.toArray(output);
            }

            @Override
            public List<Object> toList() {
                checkConcurrentModification();

                final List<Object> remaining = new ArrayList<>(_columnList.get(columnIndex).subList(cursor, toRowIndex));

                cursor = toRowIndex;

                return remaining;
            }

            private void checkConcurrentModification() {
                checkModification(expectedModCount);
            }
        });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final Class<? extends T> rowType) throws IllegalArgumentException {
        return stream(0, size(), rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Class<? extends T> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return stream(fromRowIndex, toRowIndex, _columnNameList, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final Collection<String> columnNames, final Class<? extends T> rowType) throws IllegalArgumentException {
        return stream(0, size(), columnNames, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Class<? extends T> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return stream(fromRowIndex, toRowIndex, columnNames, null, rowType, null);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final IntFunction<? extends T> rowSupplier) throws IllegalArgumentException {
        return stream(0, size(), rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final IntFunction<? extends T> rowSupplier)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return stream(fromRowIndex, toRowIndex, _columnNameList, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier) throws IllegalArgumentException {
        return stream(0, size(), columnNames, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final IntFunction<? extends T> rowSupplier)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        checkColumnNames(columnNames);
        N.checkArgNotNull(rowSupplier, cs.rowSupplier);

        return stream(fromRowIndex, toRowIndex, columnNames, null, null, rowSupplier);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType) throws IllegalArgumentException {
        return stream(0, size(), _columnNameList, prefixAndFieldNameMap, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return stream(fromRowIndex, toRowIndex, _columnNameList, prefixAndFieldNameMap, rowType);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final Collection<String> columnNames, final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType)
            throws IllegalArgumentException {
        return stream(0, size(), columnNames, prefixAndFieldNameMap, rowType);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code fromRowIndex > toRowIndex}, or {@code toRowIndex > size()}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> rowType) throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        checkColumnNames(columnNames);

        N.checkArgument(Beans.isBeanClass(rowType), "{} is not a bean class", rowType);

        return stream(fromRowIndex, toRowIndex, columnNames, prefixAndFieldNameMap, rowType, null);
    }

    private <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final Map<String, String> prefixAndFieldNameMap, final Class<? extends T> inputRowClass, final IntFunction<? extends T> inputRowSupplier) {
        checkRowIndex(fromRowIndex, toRowIndex);
        checkColumnNames(columnNames);
        // Every terminal path uses the same immutable selection and mapping, even if caller inputs change.
        final List<String> selectedColumnNames = new ArrayList<>(columnNames);
        final Map<String, String> selectedPrefixes = prefixAndFieldNameMap == null ? null : new HashMap<>(prefixAndFieldNameMap);

        final int[] columnIndexes = checkColumnNames(selectedColumnNames);

        final int columnCount = columnIndexes.length;

        if (inputRowClass == null && inputRowSupplier == null) {
            throw new IllegalArgumentException("Either inputRowClass or inputRowSupplier must be non-null");
        }

        final T firstRow = inputRowSupplier == null ? null : checkSupplierResult(inputRowSupplier.apply(columnCount), "rowSupplier");

        final Class<? extends T> rowClass = inputRowClass == null ? (Class<T>) firstRow.getClass() : inputRowClass;
        final Type<T> rowType = Type.of(rowClass);
        final BeanInfo beanInfo = rowType.isBean() ? ParserUtil.getBeanInfo(rowClass) : null;

        if (firstRow != null && rowType.isObjectArray()) {
            checkObjectArrayCapacity((Object[]) firstRow, columnCount, "rowSupplier");
        }

        final IntFunction<? extends T> rowSupplier;

        if (inputRowSupplier == null) {
            rowSupplier = rowType.isBean() ? null : this.createRowSupplier(rowClass, rowType);
        } else {
            rowSupplier = reuseFirstSuppliedRow(firstRow, inputRowSupplier);
        }

        return Stream.of(new ObjIteratorEx<T>() {
            private final int expectedModCount = modCount;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                checkConcurrentModification();

                return cursor < toRowIndex;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if this iterator has no remaining element
             */
            @Override
            public T next() throws NoSuchElementException {
                checkConcurrentModification();

                if (cursor >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return getRow(cursor++, columnIndexes, columnCount, selectedPrefixes, beanInfo, rowClass, rowType, rowSupplier);
            }

            @Override
            public long count() {
                checkConcurrentModification();

                final long result = toRowIndex - cursor; //NOSONAR
                cursor = toRowIndex;
                return result;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                checkConcurrentModification();

                final List<T> rows = RowDataset.this.toList(cursor, toRowIndex, selectedColumnNames, selectedPrefixes, rowClass, rowSupplier);

                a = a.length >= rows.size() ? a : (A[]) N.newArray(a.getClass().getComponentType(), rows.size());

                cursor = toRowIndex;

                return rows.toArray(a);
            }

            void checkConcurrentModification() {
                checkModification(expectedModCount);
            }
        });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) throws IllegalArgumentException {
        return stream(0, size(), rowMapper);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        return stream(fromRowIndex, toRowIndex, _columnNameList, rowMapper);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final Collection<String> columnNames, final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper)
            throws IllegalArgumentException {
        return stream(0, size(), columnNames, rowMapper);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames,
            final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int[] columnIndexes = checkColumnNames(columnNames);
        N.checkArgNotNull(rowMapper, cs.rowMapper);

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

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if this iterator has no remaining element
             */
            @Override
            public T next() throws NoSuchElementException {
                checkConcurrentModification();

                if (cursor >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                for (int i = 0; i < columnCount; i++) {
                    row[i] = _columnList.get(columnIndexes[i]).get(cursor);
                }

                return rowMapper.apply(cursor++, disposableArray);
            }

            @Override
            public long count() {
                checkConcurrentModification();

                final long result = toRowIndex - cursor; //NOSONAR
                cursor = toRowIndex;
                return result;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            void checkConcurrentModification() {
                checkModification(expectedModCount);
            }
        });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final Tuple2<String, String> columnNames, final BiFunction<?, ?, ? extends T> rowMapper) throws IllegalArgumentException {
        return stream(0, size(), columnNames, rowMapper);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Tuple2<String, String> columnNames,
            final BiFunction<?, ?, ? extends T> rowMapper) throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(columnNames, cs.columnNames);
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        N.checkArgNotNull(rowMapper, cs.rowMapper);

        final BiFunction<Object, Object, ? extends T> rowMapperToUse = (BiFunction<Object, Object, ? extends T>) rowMapper;

        return Stream.of(new ObjIteratorEx<T>() {
            private final int expectedModCount = modCount;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                checkConcurrentModification();

                return cursor < toRowIndex;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if this iterator has no remaining element
             */
            @Override
            public T next() throws NoSuchElementException {
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

                final long result = toRowIndex - cursor; //NOSONAR
                cursor = toRowIndex;
                return result;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            void checkConcurrentModification() {
                checkModification(expectedModCount);
            }
        });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final Tuple3<String, String, String> columnNames, final TriFunction<?, ?, ?, ? extends T> rowMapper)
            throws IllegalArgumentException {
        return stream(0, size(), columnNames, rowMapper);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public <T> Stream<T> stream(final int fromRowIndex, final int toRowIndex, final Tuple3<String, String, String> columnNames,
            final TriFunction<?, ?, ?, ? extends T> rowMapper) throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(fromRowIndex, toRowIndex);
        N.checkArgNotNull(columnNames, cs.columnNames);
        final List<Object> column1 = _columnList.get(checkColumnName(columnNames._1));
        final List<Object> column2 = _columnList.get(checkColumnName(columnNames._2));
        final List<Object> column3 = _columnList.get(checkColumnName(columnNames._3));
        N.checkArgNotNull(rowMapper, cs.rowMapper);

        final TriFunction<Object, Object, Object, ? extends T> rowMapperToUse = (TriFunction<Object, Object, Object, ? extends T>) rowMapper;

        return Stream.of(new ObjIteratorEx<T>() {
            private final int expectedModCount = modCount;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                checkConcurrentModification();

                return cursor < toRowIndex;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if this iterator has no remaining element
             */
            @Override
            public T next() throws NoSuchElementException {
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

                final long result = toRowIndex - cursor; //NOSONAR
                cursor = toRowIndex;
                return result;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                checkConcurrentModification();

                cursor = n > toRowIndex - cursor ? toRowIndex : (int) n + cursor;
            }

            void checkConcurrentModification() {
                checkModification(expectedModCount);
            }
        });
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws E if the provided function throws an exception.
     */
    @Override
    public <R, E extends Exception> R apply(final Throwables.Function<? super Dataset, ? extends R, E> func) throws IllegalArgumentException, E {
        N.checkArgNotNull(func, cs.func);

        return func.apply(this);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException if the function returns {@code null}
     * @throws E if the provided function throws an exception.
     */
    @Override
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super Dataset, ? extends R, E> func)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(func, cs.func);

        if (size() > 0) {
            return Optional.of(func.apply(this));
        } else {
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws E if the provided action throws an exception.
     */
    @Override
    public <E extends Exception> void accept(final Throwables.Consumer<? super Dataset, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action, cs.action);

        action.accept(this);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws E if the provided action throws an exception.
     */
    @Override
    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super Dataset, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action, cs.action);

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

    /**
     * {@inheritDoc}
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     */
    @Override
    public int size() throws ConcurrentModificationException {
        checkSliceValidity();
        return (_columnList.size() == 0) ? 0 : _columnList.get(0).size();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     */
    @Override
    public void clear() throws IllegalStateException {
        checkFrozen();

        // Clearing a Dataset that already has no rows changes nothing, so it is not a structural
        // modification and must not invalidate outstanding streams/iterators - see removeRowsAt(int...).
        if (size() == 0) {
            return;
        }

        for (final List<Object> column : _columnList) {
            column.clear();
        }

        normalizeCurrentRowIndex();

        rowsChanged();
    }

    @Override
    public Map<String, Object> getProperties() {
        if (_properties == EMPTY_PROPERTIES) {
            return _properties;
        } else {
            return ImmutableMap.wrap(_properties);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this dataset is frozen
     */
    @Override
    public void setProperties(final Map<String, ?> properties) throws IllegalStateException {
        checkFrozen();

        this._properties = copyProperties(properties);
    }

    private static Map<String, Object> copyProperties(final Map<String, ?> properties) {
        if (N.isEmpty(properties)) {
            return EMPTY_PROPERTIES;
        } else {
            // newTargetMap, not newOrderingMap: the copy keeps the SAME keys, so a SortedMap's comparator is
            // still valid for it. newOrderingMap exists for results that are re-keyed by the template's
            // *values* and therefore deliberately downgrades a SortedMap to a LinkedHashMap - which silently
            // turned a TreeMap of properties into an insertion-ordered map.
            final Map<String, Object> result = Maps.newTargetMap(properties);
            result.putAll(properties);
            return result;
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public Dataset withMissingPropertyPolicy(final MissingPropertyPolicy policy) throws IllegalArgumentException {
        N.checkArgNotNull(policy, cs.policy);
        final RowDataset view = (RowDataset) slice(0, size());
        view.missingPropertyPolicy = policy;
        return view;
    }

    @Override
    public void println() {
        println(0, size());
    }

    @Override
    public void println(String prefix) {
        println(0, size(), _columnNameList, prefix, System.out);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void println(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        println(fromRowIndex, toRowIndex, _columnNameList); // NOSONAR
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void println(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        println(fromRowIndex, toRowIndex, columnNames, System.out); // NOSONAR
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void println(final Appendable output) throws IllegalArgumentException, UncheckedIOException {
        println(0, size(), _columnNameList, output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void println(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final Appendable output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        println(fromRowIndex, toRowIndex, columnNames, null, output);
    }

    /**
     * {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws UncheckedIOException {@inheritDoc}
     */
    @Override
    public void println(final int fromRowIndex, final int toRowIndex, final Collection<String> columnNames, final String prefix, final Appendable output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        checkRowIndex(fromRowIndex, toRowIndex);
        final int[] columnIndexes = N.isEmpty(columnNames) ? N.EMPTY_INT_ARRAY : checkColumnNames(columnNames);
        N.checkArgNotNull(output, cs.output);

        final boolean isBufferedWriter = output instanceof Writer writer && IOUtil.isBufferedWriter(writer);
        final Writer bw = isBufferedWriter ? (Writer) output : (output instanceof Writer writer ? Objectory.createBufferedWriter((writer)) : null);
        final Appendable appendable = bw != null ? bw : output;
        final int rowLen = toRowIndex - fromRowIndex;
        final int columnLen = columnIndexes.length;
        final String lineSeparator = Strings.isEmpty(prefix) ? IOUtil.LINE_SEPARATOR_UNIX : (IOUtil.LINE_SEPARATOR_UNIX + prefix);

        try {
            if (N.notEmpty(prefix)) {
                appendable.append(prefix);
            }

            if (columnLen == 0) {
                appendable.append("+---+");
                appendable.append(lineSeparator);

                appendable.append("|   |");
                appendable.append(lineSeparator);

                appendable.append("+---+");
            } else {
                final List<String> columnNameList = new ArrayList<>(columnNames);
                final List<List<String>> strColumnList = new ArrayList<>(columnLen);
                final int[] maxColumnLens = new int[columnLen];

                for (int i = 0; i < columnLen; i++) {
                    final List<Object> column = _columnList.get(columnIndexes[i]);
                    final List<String> strColumn = new ArrayList<>(rowLen);
                    int maxLen = Strings.displayWidth(columnNameList.get(i));
                    String str = null;

                    for (int rowIndex = fromRowIndex; rowIndex < toRowIndex; rowIndex++) {
                        str = N.toString(column.get(rowIndex));
                        maxLen = N.max(maxLen, Strings.displayWidth(str));
                        strColumn.add(str);
                    }

                    maxColumnLens[i] = maxLen;
                    strColumnList.add(strColumn);
                }

                final char hch = '-';
                final char hchDelta = 2;

                for (int i = 0; i < columnLen; i++) {
                    appendable.append('+');

                    appendable.append(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                }

                appendable.append('+');
                appendable.append(lineSeparator);

                for (int i = 0; i < columnLen; i++) {
                    if (i == 0) {
                        appendable.append("| ");
                    } else {
                        appendable.append(" | ");
                    }

                    appendable.append(Strings.padEndToDisplayWidth(columnNameList.get(i), maxColumnLens[i]));
                }

                appendable.append(" |");
                appendable.append(lineSeparator);

                for (int i = 0; i < columnLen; i++) {
                    appendable.append('+');

                    appendable.append(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                }

                appendable.append('+');

                for (int j = 0; j < rowLen; j++) {
                    appendable.append(lineSeparator);

                    for (int i = 0; i < columnLen; i++) {
                        if (i == 0) {
                            appendable.append("| ");
                        } else {
                            appendable.append(" | ");
                        }

                        appendable.append(Strings.padEndToDisplayWidth(strColumnList.get(i).get(j), maxColumnLens[i]));
                    }

                    appendable.append(" |");
                }

                if (rowLen == 0) {
                    appendable.append(lineSeparator);

                    // Same cell framing as a data row. Padding the gap between columns with plain spaces
                    // instead of writing " | " rendered a zero-row table as a single cell spanning every
                    // column: "|        |" under a "| a | bb |" header.
                    for (int i = 0; i < columnLen; i++) {
                        if (i == 0) {
                            appendable.append("| ");
                        } else {
                            appendable.append(" | ");
                        }

                        appendable.append(Strings.padEndToDisplayWidth("", maxColumnLens[i]));
                    }

                    appendable.append(" |");
                }

                appendable.append(lineSeparator);

                for (int i = 0; i < columnLen; i++) {
                    appendable.append('+');

                    appendable.append(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                }

                appendable.append('+');
            }

            appendable.append(IOUtil.LINE_SEPARATOR_UNIX);

            if (bw != null) {
                bw.flush();
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (bw != null && !isBufferedWriter) {
                Objectory.recycle((BufferedWriter) bw);
            }
        }
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>The hash is derived from the column-name list and the column data only. Dataset properties and
     * frozen state are not included. Cell values are hashed <i>deeply</i>: a cell holding an array
     * contributes a content-based hash, not an identity one.</p>
     *
     * @return a hash code value for this dataset
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + _columnNameList.hashCode();

        // Deep hash of the cell values, mirroring List.hashCode()'s algorithm but calling N.deepHashCode on
        // each cell. Arrays inherit identity hashCode, so a plain _columnList.hashCode() gave two datasets
        // built from equal array cells different hashes - while distinct()/groupBy()/the set operations,
        // which route cell values through hashKey(Object), already treated those cells as equal. For a cell
        // that is not an array N.deepHashCode is exactly Objects.hashCode, so nothing else changes.
        int columnsHash = 1;

        for (final List<Object> column : _columnList) {
            int columnHash = 1;

            for (final Object value : column) {
                columnHash = (31 * columnHash) + N.deepHashCode(value);
            }

            columnsHash = (31 * columnsHash) + columnHash;
        }

        return (h * 31) + columnsHash;
    }

    /**
     * Compares this dataset to the specified object for equality.
     *
     * <p>Two {@code RowDataset} instances are equal when they have the same row count, the same ordered
     * column names, and equal column values (element-wise). Cell values are compared <i>deeply</i>, so
     * two datasets whose corresponding cells hold distinct but equal-by-content arrays are equal - which
     * is what makes {@code equals} agree with {@link #distinct()} and the set operations. Properties and
     * frozen state are not considered. {@link Sheet#equals(Object)} deliberately differs: it compares cells
     * with plain {@code equals}, so array cells there are matched by identity.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if {@code obj} is a {@code RowDataset} with the same structure
     *         and cell values; {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof final RowDataset other)) {
            return false;
        }

        if (size() != other.size() || !N.equals(_columnNameList, other._columnNameList)) {
            return false;
        }

        // Cell-wise N.deepEquals rather than _columnList.equals(other._columnList): see hashCode() - array
        // cells must compare by content so that equals() agrees with distinct()/union()/groupBy(). Iterators
        // rather than indexes because a column may be any List the caller supplied.
        final Iterator<List<Object>> columns = _columnList.iterator();
        final Iterator<List<Object>> otherColumns = other._columnList.iterator();

        while (columns.hasNext() && otherColumns.hasNext()) {
            final Iterator<Object> values = columns.next().iterator();
            final Iterator<Object> otherValues = otherColumns.next().iterator();

            while (values.hasNext() && otherValues.hasNext()) {
                if (!N.deepEquals(values.next(), otherValues.next())) {
                    return false;
                }
            }

            if (values.hasNext() || otherValues.hasNext()) {
                return false;
            }
        }

        return !columns.hasNext() && !otherColumns.hasNext();
    }

    /**
     * Returns a string representation of this dataset for debugging.
     *
     * <p>The format includes column names, optional non-empty properties, frozen flag, and
     * each column's values. It is not intended as a stable serialization format.</p>
     *
     * @return a string describing column names, properties (when present), frozen state, and column data
     */
    @Override
    public String toString() {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append("{columnNames=");
            sb.append(_columnNameList);

            if (N.notEmpty(_properties)) {
                sb.append(", properties=");
                sb.append(_properties);
            }

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

    /**
     * Asserts that this dataset is not frozen.
     *
     * @throws IllegalStateException if this dataset has been frozen via {@link #freeze()}
     */
    void checkFrozen() throws IllegalStateException {
        if (_isFrozen) {
            throw new IllegalStateException("This Dataset is frozen and cannot be modified");
        }
    }

    /**
     * Validates the row range of a {@code forEach} call, which runs forward when {@code fromRowIndex <= toRowIndex}
     * and backward otherwise.
     *
     * <p>Backward, {@code fromRowIndex} is inclusive and {@code toRowIndex} exclusive, so {@code -1} is the only way
     * to include row 0. The old check validated the range as {@code [max(0, toRowIndex), fromRowIndex)} and the
     * loop then clamped the start with {@code min(size() - 1, fromRowIndex)}, so {@code forEach(size(), -1, ...)}
     * was silently accepted and visited the same rows as {@code forEach(size() - 1, -1, ...)} - a nonexistent
     * start row was neither rejected nor distinguishable from the last one.</p>
     *
     * @param fromRowIndex the first row visited
     * @param toRowIndex the row at which iteration stops, exclusive
     * @throws IndexOutOfBoundsException if the range is invalid in the direction implied by its ends
     */
    private void checkForEachRowRange(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        if (fromRowIndex <= toRowIndex) {
            checkRowIndex(fromRowIndex, toRowIndex);
        } else if (toRowIndex < -1 || fromRowIndex >= size()) {
            throw new IndexOutOfBoundsException("Reverse row index range (" + toRowIndex + ", " + fromRowIndex + "] is out-of-bounds for length " + size()
                    + ": fromRowIndex must be < size() and toRowIndex >= -1");
        }
    }

    /**
     * Validates that {@code rowIndex} is a valid row index for this dataset.
     *
     * @param rowIndex the row index to validate
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException if {@code rowIndex} is negative or {@code >= size()}
     */
    void checkRowIndex(final int rowIndex) throws ConcurrentModificationException, IndexOutOfBoundsException {
        if ((rowIndex < 0) || (rowIndex >= size())) {
            throw new IndexOutOfBoundsException("Invalid row index: " + rowIndex + ". It must be >= 0 and < " + size());
        }
    }

    /**
     * Validates that {@code columnIndex} is a valid column index for this dataset.
     *
     * <p>Without this the two-coordinate accessors indexed straight into the backing lists, so a bad row
     * index and a bad column index produced the same anonymous {@code ArrayList} message and gave the caller
     * no way to tell which coordinate was wrong.</p>
     *
     * @param columnIndex the column index to validate
     * @throws IndexOutOfBoundsException if {@code columnIndex} is negative or {@code >= columnCount()}
     */
    void checkColumnIndex(final int columnIndex) throws IndexOutOfBoundsException {
        if ((columnIndex < 0) || (columnIndex >= _columnList.size())) {
            throw new IndexOutOfBoundsException("Invalid column index: " + columnIndex + ". It must be >= 0 and < " + _columnList.size());
        }
    }

    /**
     * Validates that the cursor set by {@link #moveToRow(int)} currently designates a real row.
     *
     * <p>The cursor is clamped to {@code 0} whenever the dataset is emptied, so on a dataset with no rows the
     * cursor-relative accessors would otherwise index row {@code 0} of an empty column and surface an
     * {@code ArrayList} {@code IndexOutOfBoundsException} that says nothing about the cursor.</p>
     *
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException if this dataset has no rows
     */
    void checkCurrentRow() throws ConcurrentModificationException, IndexOutOfBoundsException {
        if (size() == 0) {
            throw new IndexOutOfBoundsException("This Dataset has no rows, so there is no current row to read or write."
                    + " Use the two-coordinate get(int, int)/set(int, int, Object) overloads, or add rows first.");
        }
    }

    /**
     * Validates that {@code [fromRowIndex, toRowIndex)} is a valid half-open row range for this dataset.
     *
     * @param fromRowIndex the inclusive start row index
     * @param toRowIndex the exclusive end row index
     * @throws ConcurrentModificationException if this dataset is a slice invalidated by a structural row change in its parent or an ancestor
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code fromRowIndex > toRowIndex},
     *         or {@code toRowIndex > size()}
     */
    void checkRowIndex(final int fromRowIndex, final int toRowIndex) throws ConcurrentModificationException, IndexOutOfBoundsException {
        checkRowIndex(fromRowIndex, toRowIndex, size());
    }

    /**
     * Validates that {@code [fromRowIndex, toRowIndex)} is a valid half-open row range for a
     * dataset of the given {@code size}.
     *
     * @param fromRowIndex the inclusive start row index
     * @param toRowIndex the exclusive end row index
     * @param size the total row count to validate against
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code fromRowIndex > toRowIndex},
     *         or {@code toRowIndex > size}
     */
    void checkRowIndex(final int fromRowIndex, final int toRowIndex, final int size) throws IndexOutOfBoundsException {
        if ((fromRowIndex < 0) || (fromRowIndex > toRowIndex) || (toRowIndex > size)) {
            throw new IndexOutOfBoundsException("Row index range [" + fromRowIndex + ", " + toRowIndex + ") is out-of-bounds for length " + size);
        }
    }

    /**
     * Returns a hash-key representative for the given object suitable for use as a map key,
     * handling arrays (which do not override {@code equals}/{@code hashCode}) by wrapping them for value-based comparison.
     *
     * @param obj the object to wrap as a hash key; may be {@code null}
     * @return a hash-key object for {@code obj}
     */
    static Object hashKey(final Object obj) {
        return N.hashKey(obj);
    }

    /**
     * A {@link Paginated} view of this {@code RowDataset}.
     *
     * <p>Pages are lazily computed as slices of the outer dataset. Only the most recently returned page is
     * cached, so repeatedly reading the same page is cheap while a full traversal does not accumulate every
     * page in memory. The view detects concurrent structural modifications to the outer dataset, including invalidation of a slice, and throws {@link ConcurrentModificationException} if one is detected.</p>
     */
    private class PaginatedDataset implements Paginated<Dataset> {
        /** The mod-count snapshot taken when this view was created; used to detect concurrent modifications. */
        private final int expectedModCount = modCount;

        /**
         * The most recently computed page, so that the common {@code getPage(n)} / {@code pageSize()} /
         * re-read pattern and {@code iterator()}'s {@code hasNext()}+{@code next()} do not rebuild the
         * same slice twice. Deliberately one entry, not a map: the previous unbounded cache retained every
         * page ever produced for the lifetime of this view, so a full iteration of {@code paginate(1)} over
         * a 200,000-row Dataset held ~40 MB of page objects that could never be released.
         */
        private int cachedPageNum = -1;

        private Dataset cachedPage;

        /** The column names included in each page. */
        private final Collection<String> columnNames;

        /** The maximum number of rows per page. */
        private final int pageSize;

        /** The total number of pages. */
        private final int totalPages;

        //    /** The current page num. */
        //    private int currentPageNum;

        /**
         * Creates a paginated view of the enclosing {@code RowDataset}.
         *
         * @param columnNames the column names to include in each page; must not be {@code null}
         * @param pageSize the maximum number of rows per page; must be positive
         * @throws IllegalArgumentException if {@code pageSize} is not positive.
         */
        private PaginatedDataset(final Collection<String> columnNames, final int pageSize) throws IllegalArgumentException {
            // N.checkArgNotEmpty(columnNames, "columnNames");   // empty Dataset.
            N.checkArgPositive(pageSize, cs.pageSize);

            // Defensive copy: this view is long-lived and re-reads columnNames on every getPage() call,
            // so a caller mutating the collection they passed in would silently change already-created pages.
            this.columnNames = new ArrayList<>(columnNames);
            this.pageSize = pageSize;

            totalPages = ((size() % pageSize) == 0) ? (size() / pageSize) : ((size() / pageSize) + 1);

            // currentPageNum = 0;
        }

        @Override
        public Iterator<Dataset> iterator() {
            return new ObjIterator<>() {
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    checkConcurrentModification();

                    return cursor < totalPages;
                }

                /**
                 * {@inheritDoc}
                 * @throws NoSuchElementException if this iterator has no remaining element
                 */
                @Override
                public Dataset next() throws NoSuchElementException {
                    // hasNext() already runs checkConcurrentModification().
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final Dataset ret = getPage(cursor);
                    cursor++;
                    return ret;
                }
            };
        }

        /**
         * Returns the first page, or an empty {@link Optional} if there are no pages.
         *
         * @return an {@link Optional} containing the first page, or empty if the dataset is empty
         */
        @Override
        public Optional<Dataset> firstPage() {
            checkConcurrentModification();
            return totalPages == 0 ? Optional.empty() : Optional.of(getPage(0));
        }

        /**
         * Returns the last page, or an empty {@link Optional} if there are no pages.
         *
         * @return an {@link Optional} containing the last page, or empty if the dataset is empty
         */
        @Override
        public Optional<Dataset> lastPage() {
            checkConcurrentModification();
            return totalPages == 0 ? Optional.empty() : Optional.of(getPage(totalPages - 1));
        }

        /**
         * Returns the page at the given zero-based page number.
         *
         * <p>Pages are computed lazily; the most recently returned page is cached, so asking for it again
         * returns the same instance. Each page is a frozen slice of the outer dataset.</p>
         *
         * @param pageNum the zero-based page number; must be {@code >= 0} and {@code < totalPages()}
         * @return a frozen {@link Dataset} slice representing the requested page
         * @throws IllegalArgumentException if {@code pageNum} is out of range.
         * @throws ConcurrentModificationException if the outer dataset was structurally
         *         modified since this {@code PaginatedDataset} was created
         */
        @Override
        public Dataset getPage(final int pageNum) throws IllegalArgumentException, ConcurrentModificationException {
            checkConcurrentModification();
            checkPageNumber(pageNum);

            if (cachedPageNum == pageNum) {
                return cachedPage;
            }

            final int offset = pageNum * pageSize;
            final Dataset page = RowDataset.this.slice(offset, offset + Math.min(pageSize, size() - offset), columnNames);

            cachedPage = page;
            cachedPageNum = pageNum;

            return page;
        }

        /**
         * Returns the maximum number of rows per page.
         *
         * @return the page size
         */
        @Override
        public int pageSize() {
            return pageSize;
        }

        /**
         * Returns the total number of pages.
         *
         * @return total number of pages
         * @deprecated Use {@link #totalPages()} instead.
         */
        @Deprecated
        @Override
        public int pageCount() {
            return totalPages();
        }

        /**
         * Returns the total number of pages.
         *
         * @return total number of pages
         */
        @Override
        public int totalPages() {
            checkConcurrentModification();
            return totalPages;
        }

        /**
         * Returns a sequential {@link Stream} over all pages in order.
         *
         * @return a stream of {@link Dataset} pages
         */
        @Override
        public Stream<Dataset> stream() {
            return Stream.of(iterator());
        }

        /**
         * Verifies that the outer dataset has not been structurally modified since this
         * paginated view was created.
         *
         * @throws ConcurrentModificationException if the outer dataset was structurally modified
         */
        final void checkConcurrentModification() throws ConcurrentModificationException {
            checkModification(expectedModCount);
        }

        /**
         * @throws IllegalArgumentException if {@code pageNumber} is negative or is not less than the total page count
         */
        private void checkPageNumber(final int pageNumber) throws IllegalArgumentException {
            if ((pageNumber < 0) || (pageNumber >= totalPages)) {
                throw new IllegalArgumentException(pageNumber + " out of page index [0, " + totalPages + ")");
            }
        }
    }
}
