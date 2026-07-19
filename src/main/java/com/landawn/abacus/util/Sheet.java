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
import java.io.IOException;
import java.io.Writer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBiPredicate;
import com.landawn.abacus.util.function.IntObjFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * A two-dimensional tabular data structure that stores values in cells identified by row and column keys,
 * providing a flexible and powerful API for working with structured data. This final class combines the
 * functionality of spreadsheets and database tables, enabling efficient manipulation, transformation,
 * and analysis of tabular data with strongly-typed row keys, column keys, and cell values.
 *
 * <p>Sheet provides a rich set of operations for data manipulation including cell-level access, bulk
 * row/column operations, sorting, filtering, transposition, and export capabilities. It serves as a
 * bridge between raw data structures and higher-level data processing frameworks, making it ideal
 * for data analysis, reporting, and transformation scenarios.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Two-Dimensional Structure:</b> Organizes data in a grid with row and column identifiers</li>
 *   <li><b>Strongly Typed:</b> Separate type parameters for row keys, column keys, and cell values</li>
 *   <li><b>Flexible Access:</b> Access cells by keys or indices with efficient lookup operations</li>
 *   <li><b>Rich Operations:</b> Comprehensive API for sorting, filtering, merging, and transforming data</li>
 *   <li><b>Stream Integration:</b> Full support for functional programming with Stream operations</li>
 *   <li><b>Export Capabilities:</b> Convert to arrays, datasets, and formatted output</li>
 *   <li><b>Immutability Support:</b> In-place freeze functionality for preventing further structural or cell updates</li>
 *   <li><b>Memory Efficient:</b> Optimized internal storage with column-wise organization</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Data Analysis:</b> Statistical analysis and data exploration with tabular structures</li>
 *   <li><b>Report Generation:</b> Creating formatted reports and pivot tables</li>
 *   <li><b>Data Transformation:</b> ETL operations, data cleaning, and format conversion</li>
 *   <li><b>Configuration Management:</b> Structured configuration data with key-value mappings</li>
 *   <li><b>Matrix Operations:</b> Mathematical operations on two-dimensional data</li>
 *   <li><b>Spreadsheet-like Operations:</b> Programmatic spreadsheet functionality</li>
 *   <li><b>Database Result Processing:</b> In-memory manipulation of query results</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Creating sheets with different data types
 * Sheet<String, String, Integer> scores = Sheet.rows(
 *     Arrays.asList("Student1", "Student2", "Student3"),
 *     Arrays.asList("Math", "Science", "English"),
 *     new Integer[][] {
 *         {85, 90, 88},
 *         {92, 87, 91},
 *         {78, 85, 89}
 *     }
 * );
 *
 * // Cell operations
 * Integer mathScore = scores.get("Student1", "Math");   // returns 85
 * scores.set("Student2", "Science", 95);   // Update cell
 * boolean hasScore = scores.containsCell("Student3", "English");
 *
 * // Row and column operations
 * ImmutableList<Integer> student1Scores = scores.rowValues("Student1");
 * scores.addRow("Student4", Arrays.asList(88, 92, 87));
 * scores.removeColumn("English");
 * scores.sortRowsByColumnValues("Math", Comparator.reverseOrder());
 *
 * // Bulk operations and transformations
 * scores.updateAll(score -> score + 5);   // Add 5 to all scores
 * scores.replaceIf(score -> score < 80, 80);   // Set minimum score
 * Sheet<String, String, Integer> transposed = scores.transpose();
 *
 * // Stream operations for functional programming
 * double averageScore = scores.rowMajorStream()
 *     .filter(Objects::nonNull)
 *     .mapToInt(Integer::intValue)
 *     .average()
 *     .orElse(0.0);
 *
 * // Export and conversion
 * Dataset dataset = scores.toDataset();
 * Object[][] array = scores.toArray();
 * scores.println();   // Pretty-print to console
 *
 * // Creating immutable snapshots
 * Sheet<String, String, Integer> frozen = scores.clone(true);
 * // clone(true) already returns a frozen copy
 * }</pre>
 *
 * <p><b>Data Organization:</b>
 * <ul>
 *   <li><b>Row Keys (R):</b> Unique identifiers for rows, can be any type</li>
 *   <li><b>Column Keys (C):</b> Unique identifiers for columns, can be any type</li>
 *   <li><b>Cell Values (V):</b> Data stored in cells, can be any type including {@code null}</li>
 *   <li><b>Internal Storage:</b> Column-wise organization for memory efficiency</li>
 * </ul>
 *
 * <p><b>Factory Methods:</b>
 * <ul>
 *   <li>{@link #empty()} - Create an empty sheet</li>
 *   <li>{@link #rows(Collection, Collection, Object[][])} - Create from row-wise data</li>
 *   <li>{@link #columns(Collection, Collection, Object[][])} - Create from column-wise data</li>
 *   <li>Constructors for various initialization scenarios</li>
 * </ul>
 *
 * <p><b>Access Patterns:</b>
 * <ul>
 *   <li><b>By Keys:</b> {@code get(rowKey, columnKey)}, {@code set(rowKey, columnKey, value)}</li>
 *   <li><b>By Indices:</b> {@code get(rowIndex, columnIndex)}, {@code set(rowIndex, columnIndex, value)}</li>
 *   <li><b>By Point:</b> {@code get(point)}, {@code set(point, value)} for coordinate-based access</li>
 *   <li><b>Bulk Access:</b> {@code rowValues()}, {@code columnValues()}, {@code setRow()}, {@code setColumn()}</li>
 * </ul>
 *
 * <p><b>Stream Operations:</b>
 * <ul>
 *   <li><b>Horizontal Streaming:</b> {@code rowMajorStream()}, {@code rowMajorCells()} - Row-by-row processing</li>
 *   <li><b>Vertical Streaming:</b> {@code columnMajorStream()}, {@code columnMajorCells()} - Column-by-column processing</li>
 *   <li><b>Row Streaming:</b> {@code rowStreams()}, {@code rows()} - Stream of rows</li>
 *   <li><b>Column Streaming:</b> {@code columnStreams()}, {@code columns()} - Stream of columns</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Cell access: O(1) average time with hash-based key lookup</li>
 *   <li>Row/column operations: O(n) where n is the number of cells in row/column</li>
 *   <li>Sorting: O(n log n) where n depends on the sort dimension</li>
 *   <li>Memory usage: O(r × c) where r is rows and c is columns</li>
 *   <li>Column-wise storage provides cache-friendly access patterns</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * Sheet instances are <b>not thread-safe</b>. For concurrent access:
 * <ul>
 *   <li>Use external synchronization for write operations</li>
 *   <li>Read operations can be safely performed concurrently if no writes occur</li>
 *   <li>Use {@link #freeze()} on a safely published dedicated instance, or {@link #clone(boolean) clone(true)}, before sharing sheet structure across threads</li>
 *   <li>Consider creating thread-local copies using {@link #clone()}</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li>Use {@link #trimToSize()} to reduce memory footprint after bulk operations</li>
 *   <li>Use {@link #clear()} to release references held by cells while preserving keys and allocated storage</li>
 *   <li>Use {@link #copy()} for an independent structure or {@link #clone(boolean)} for a deep copy when Kryo is available</li>
 *   <li>Cell values are stored as object references; primitive values are boxed</li>
 * </ul>
 *
 * <p><b>Integration Points:</b>
 * <ul>
 *   <li><b>{@link Dataset}:</b> Convert a Sheet to a Dataset for advanced data processing</li>
 *   <li><b>{@link Stream}:</b> Functional programming operations on sheet data</li>
 *   <li><b>Arrays:</b> Export to 2D arrays for mathematical operations</li>
 *   <li><b>Collections Framework:</b> Maps and lists for row/column access</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use meaningful row and column key types for better code readability</li>
 *   <li>Freeze sheets when sharing between components to prevent accidental modifications</li>
 *   <li>Use appropriate value types for better memory efficiency and type safety</li>
 *   <li>Consider using streaming operations for large datasets to manage memory</li>
 *   <li>Use bulk operations instead of cell-by-cell modifications for better performance</li>
 * </ul>
 *
 * <p><b>Notation Guide:</b>
 * <ul>
 *   <li><b>R</b> = Row operations (horizontal direction)</li>
 *   <li><b>C</b> = Column operations (vertical direction)</li>
 *   <li><b>H</b> = Horizontal processing (row-by-row)</li>
 *   <li><b>V</b> = Vertical processing (column-by-column)</li>
 * </ul>
 *
 * <p><b>{@code Sheet} vs. {@code Dataset}:</b> both are tabular, but they are addressed and typed differently:</p>
 * <table border="1">
 *   <caption>Choosing between Sheet and Dataset</caption>
 *   <tr>
 *     <th>Type</th>
 *     <th>Shape</th>
 *     <th>Cell typing</th>
 *     <th>Best for</th>
 *   </tr>
 *   <tr>
 *     <td>{@code Sheet}</td>
 *     <td>values addressed by a (rowKey, columnKey) pair — a 2D grid / matrix</td>
 *     <td>a single value type {@code V} (homogeneous)</td>
 *     <td>spreadsheet-like / matrix data keyed by both a row key and a column key</td>
 *   </tr>
 *   <tr>
 *     <td>{@link Dataset}</td>
 *     <td>rows &times; <em>named</em> columns (a table / DB result set / DataFrame)</td>
 *     <td>columns may differ in type (heterogeneous); cell values are {@code Object}</td>
 *     <td>tabular data with named, differently-typed columns: filter, sort, group, join, aggregate, pivot</td>
 *   </tr>
 * </table>
 *
 * <p><b>Key-selection convention:</b> methods that take a {@code Collection} of row/column keys to act
 * on (e.g. {@link #copy(Collection, Collection)}, {@link #sortRowsByColumnValues(Collection, Comparator)},
 * {@link #sortColumnsByRowValues(Collection, Comparator)}) require explicit, existing keys: a {@code null}
 * or empty key collection throws {@link IllegalArgumentException}, and so does any key not present in this
 * Sheet. This matches {@link Dataset}'s strict column-selection contract — use the no-argument
 * {@link #copy()} to copy the whole Sheet. The sole edge case (also mirroring {@code Dataset}): on an axis
 * that is itself empty (a zero-row or zero-column Sheet), an empty non-{@code null} collection for that
 * axis is accepted as its full, empty key set; only {@code null} is always rejected. Display methods
 * ({@link #println(Collection, Collection, Appendable) println}) are exempt and tolerate null/empty.
 *
 * @param <R> the type of row keys used to identify rows in the sheet
 * @param <C> the type of column keys used to identify columns in the sheet
 * @param <V> the type of values stored in the cells of the sheet
 *
 * @see Dataset
 * @see Stream
 * @see Cloneable
 * @see Point
 * @see Cell
 */
public final class Sheet<R, C, V> implements Cloneable {

    static final KryoParser kryoParser = ParserFactory.isKryoParserAvailable() ? ParserFactory.createKryoParser() : null;

    private final Set<R> _rowKeySet; //NOSONAR

    private final Set<C> _columnKeySet; //NOSONAR

    private BiMap<R, Integer> _rowKeyIndexMap; //NOSONAR

    private BiMap<C, Integer> _columnKeyIndexMap; //NOSONAR

    private List<List<V>> _columnList; //NOSONAR

    private boolean _isInitialized = false; //NOSONAR

    private boolean _isFrozen = false; //NOSONAR

    /**
     * Creates an empty Sheet with no row keys and no column keys.
     * <p>
     * The Sheet will be initialized with empty row and column key sets.
     * Values can be added later by first adding rows and columns.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>();
     * sheet.addRow("row1", List.of());       // Add empty row
     * sheet.addColumn("col1", List.of(1));   // Add column with value
     * }</pre>
     *
     * @see #Sheet(Collection, Collection)
     * @see #Sheet(Collection, Collection, Object[][])
     * @see #empty()
     */
    public Sheet() {
        this(N.emptyList(), N.emptyList());
    }

    /**
     * Creates a new Sheet with the specified row keys and column keys.
     * <p>
     * The Sheet is initialized with the given row and column keys but contains no values initially.
     * All cells will return {@code null} until values are explicitly set. The order of keys is preserved
     * as provided in the collections.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3")
     * );
     * // All cells initially contain null
     * sheet.set("row1", "col1", 42);   // Set a value
     * }</pre>
     *
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @throws IllegalArgumentException if any of the row keys or column keys are {@code null} or duplicated
     * @see #Sheet()
     * @see #Sheet(Collection, Collection, Object[][])
     * @see #rows(Collection, Collection, Object[][])
     * @see #columns(Collection, Collection, Object[][])
     */
    public Sheet(final Collection<R> rowKeySet, final Collection<C> columnKeySet) throws IllegalArgumentException {
        N.checkArgument(!N.anyNull(rowKeySet), "Row key cannot be null");
        N.checkArgument(!N.anyNull(columnKeySet), "Column key cannot be null");
        N.checkArgument(!N.containsDuplicates(rowKeySet), "Duplicate row keys are not allowed");
        N.checkArgument(!N.containsDuplicates(columnKeySet), "Duplicate column keys are not allowed");

        _rowKeySet = N.newLinkedHashSet(rowKeySet);
        _columnKeySet = N.newLinkedHashSet(columnKeySet);
    }

    /**
     * Creates a new Sheet with the specified row keys, column keys, and initial data.
     * <p>
     * The data is provided as a two-dimensional array where each inner array represents a row.
     * A {@code null} or empty outer array creates a Sheet whose cells initially read as {@code null}.
     * Otherwise, the dimensions of the array must match the sizes of the row and column key sets exactly,
     * and no inner row may be {@code null}.
     * The array can contain {@code null} values which will be stored as {@code null} cells in the Sheet.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {1, 2, 3},
     *         {4, null, 6}
     *     }
     * );
     * Integer val = sheet.get("row2", "col2");   // returns null
     * }</pre>
     *
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @param rows the initial data as a two-dimensional array where rows[i][j] is the value at row i, column j;
     *             {@code null} or empty is treated as no initial values; otherwise it must have length equal to rowKeySet size,
     *             and every inner array must be non-{@code null} and have length equal to columnKeySet size
     * @throws IllegalArgumentException if any row/column keys are {@code null} or duplicated, an inner row is {@code null},
     *             or the array dimensions don't match the key sets
     * @see #Sheet(Collection, Collection)
     * @see #rows(Collection, Collection, Object[][])
     * @see #columns(Collection, Collection, Object[][])
     */
    public Sheet(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final Object[][] rows) throws IllegalArgumentException {
        this(rowKeySet, columnKeySet);

        final int rowLength = this.rowCount();
        final int columnLength = this.columnCount();

        if (N.notEmpty(rows)) {
            N.checkArgument(rows.length == rowLength, "The length of array is not equal to size of row/column key set"); //NOSONAR

            for (final Object[] e : rows) {
                N.checkArgument(e != null, "Row cannot be null");
                N.checkArgument(e.length == columnLength, "The length of array is not equal to size of row/column key set");
            }

            initIndexMap();

            _columnList = new ArrayList<>(columnLength);

            for (int i = 0; i < columnLength; i++) {
                final List<V> column = new ArrayList<>(rowLength);

                for (int j = 0; j < rowLength; j++) {
                    column.add((V) rows[j][i]);
                }

                _columnList.add(column);
            }

            _isInitialized = true;
        }
    }

    @SuppressWarnings("rawtypes")
    private static final Sheet EMPTY_SHEET = new Sheet<>(N.emptyList(), N.emptyList(), new Object[0][0]);

    static {
        EMPTY_SHEET.freeze();
    }

    /**
     * Returns an empty, immutable Sheet instance.
     * <p>
     * This method returns a singleton empty Sheet that is frozen (immutable).
     * Useful for initialization or when an empty Sheet is needed without creating a new instance.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> emptySheet = Sheet.empty();
     * // emptySheet.set("row", "col", 1);   // throws IllegalStateException
     * }</pre>
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @return an empty, immutable Sheet instance
     * @see #Sheet()
     * @see #freeze()
     */
    public static <R, C, V> Sheet<R, C, V> empty() {
        return EMPTY_SHEET;
    }

    /**
     * Creates a new Sheet from row-oriented data.
     * <p>
     * This is a convenience factory method equivalent to calling the constructor with the same parameters.
     * Each inner array in the rows parameter represents a complete row of data.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {1, 2, 3},
     *         {4, 5, 6}
     *     }
     * );
     * }</pre>
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @param rows the data as a two-dimensional array where each inner array represents a row; {@code null} or empty means no initial values,
     *             and a non-empty array must not contain {@code null} rows
     * @return a new Sheet with the specified keys and data
     * @throws IllegalArgumentException if any keys are {@code null} or duplicated, an inner row is {@code null}, or dimensions don't match
     * @see #Sheet(Collection, Collection, Object[][])
     * @see #rows(Collection, Collection, Collection)
     * @see #columns(Collection, Collection, Object[][])
     */
    public static <R, C, V> Sheet<R, C, V> rows(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final Object[][] rows)
            throws IllegalArgumentException {
        return new Sheet<>(rowKeySet, columnKeySet, rows);
    }

    /**
     * Creates a new Sheet from row-oriented collection data.
     * <p>
     * Each inner collection represents a complete row of data. The order of values
     * in each inner collection must correspond to the order of column keys. A {@code null} or empty
     * outer collection means no initial values; a non-empty collection must not contain {@code null} rows.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     List.of(
     *         List.of(1, 2, 3),
     *         List.of(4, 5, 6)   // row2 values
     *     )
     * );
     * }</pre>
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @param rows the data as a collection of collections where each inner collection represents a row; {@code null} or empty means no initial values
     * @return a new Sheet with the specified keys and data
     * @throws IllegalArgumentException if any keys are {@code null} or duplicated, an inner row is {@code null}, or dimensions don't match
     * @see #rows(Collection, Collection, Object[][])
     * @see #columns(Collection, Collection, Collection)
     */
    public static <R, C, V> Sheet<R, C, V> rows(final Collection<R> rowKeySet, final Collection<C> columnKeySet,
            final Collection<? extends Collection<? extends V>> rows) throws IllegalArgumentException {
        final Sheet<R, C, V> instance = new Sheet<>(rowKeySet, columnKeySet);

        final int rowLength = instance.rowCount();
        final int columnLength = instance.columnCount();

        if (N.notEmpty(rows)) {
            N.checkArgument(rows.size() == rowLength, "The size of row collection is not equal to size of row key set"); //NOSONAR

            for (final Collection<? extends V> e : rows) {
                N.checkArgument(e != null, "Row cannot be null");
                N.checkArgument(e.size() == columnLength, "The size of row is not equal to size of column key set");
            }

            instance.initIndexMap();

            instance._columnList = new ArrayList<>(columnLength);

            for (int i = 0; i < columnLength; i++) {
                instance._columnList.add(new ArrayList<>(rowLength));
            }

            for (final Collection<? extends V> row : rows) {
                final Iterator<? extends V> iter = row.iterator();

                for (int i = 0; i < columnLength; i++) {
                    instance._columnList.get(i).add(iter.next());
                }
            }

            instance._isInitialized = true;
        }

        return instance;

    }

    /**
     * Creates a new Sheet from column-oriented data.
     * <p>
     * Each inner array represents a complete column of data. This is useful when
     * your data is naturally organized by columns rather than rows. A {@code null} or empty outer
     * array means no initial values; a non-empty array must not contain {@code null} columns.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.columns(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {
     *         {1, 2, 3},
     *         {4, 5, 6}
     *     }
     * );
     * }</pre>
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columns the data as a two-dimensional array where each inner array represents a column; {@code null} or empty means no initial values
     * @return a new Sheet with the specified keys and data
     * @throws IllegalArgumentException if any keys are {@code null} or duplicated, an inner column is {@code null}, or dimensions don't match
     * @see #columns(Collection, Collection, Collection)
     * @see #rows(Collection, Collection, Object[][])
     */
    public static <R, C, V> Sheet<R, C, V> columns(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final Object[][] columns)
            throws IllegalArgumentException {
        final Sheet<R, C, V> instance = new Sheet<>(rowKeySet, columnKeySet);

        final int rowLength = instance.rowCount();
        final int columnLength = instance.columnCount();

        if (N.notEmpty(columns)) {
            N.checkArgument(columns.length == columnLength, "The length of column array is not equal to size of column key set");

            for (final Object[] e : columns) {
                N.checkArgument(e != null, "Column cannot be null");
                N.checkArgument(e.length == rowLength, "The length of column is not equal to size of row key set");
            }

            instance.initIndexMap();

            instance._columnList = new ArrayList<>(columnLength);

            for (final Object[] column : columns) {
                instance._columnList.add(new ArrayList<>((List<V>) Arrays.asList(column)));
            }

            instance._isInitialized = true;
        }

        return instance;
    }

    /**
     * Creates a new Sheet from column-oriented collection data.
     * <p>
     * Each inner collection represents a complete column of data. The order of values
     * in each inner collection must correspond to the order of row keys. A {@code null} or empty
     * outer collection means no initial values; a non-empty collection must not contain {@code null} columns.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.columns(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     List.of(
     *         List.of(1, 2, 3),
     *         List.of(4, 5, 6)   // col2 values
     *     )
     * );
     * }</pre>
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKeySet the collection of row keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columnKeySet the collection of column keys for the Sheet; must not contain {@code null} or duplicate values
     * @param columns the data as a collection of collections where each inner collection represents a column; {@code null} or empty means no initial values
     * @return a new Sheet with the specified keys and data
     * @throws IllegalArgumentException if any keys are {@code null} or duplicated, an inner column is {@code null}, or dimensions don't match
     * @see #columns(Collection, Collection, Object[][])
     * @see #rows(Collection, Collection, Collection)
     */
    public static <R, C, V> Sheet<R, C, V> columns(final Collection<R> rowKeySet, final Collection<C> columnKeySet,
            final Collection<? extends Collection<? extends V>> columns) throws IllegalArgumentException {
        final Sheet<R, C, V> instance = new Sheet<>(rowKeySet, columnKeySet);

        final int rowLength = instance.rowCount();
        final int columnLength = instance.columnCount();

        if (N.notEmpty(columns)) {
            N.checkArgument(columns.size() == columnLength, "The size of column collection is not equal to size of column key set");

            for (final Collection<? extends V> e : columns) {
                N.checkArgument(e != null, "Column cannot be null");
                N.checkArgument(e.size() == rowLength, "The size of column is not equal to size of row key set");
            }

            instance.initIndexMap();

            instance._columnList = new ArrayList<>(columnLength);

            for (final Collection<? extends V> column : columns) {
                instance._columnList.add(new ArrayList<>(column));
            }

            instance._isInitialized = true;
        }

        return instance;
    }

    /**
     * Returns an immutable set of all row keys in this Sheet.
     * <p>
     * The returned set maintains the insertion order of row keys. It is a live read-only view: changes
     * made through this Sheet are reflected in the view, while attempts to modify the view directly
     * throw {@code UnsupportedOperationException}.
     * This is useful for iterating over rows or checking row existence.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * ImmutableSet<String> rowKeySetView = sheet.rowKeySet();   // returns ["row1", "row2"]
     *
     * // Iterate over rows
     * for (String rowKey : sheet.rowKeySet()) {
     *     ImmutableList<Integer> rowValues = sheet.rowValues(rowKey);
     * }
     * }</pre>
     *
     * @return a live, immutable set view containing all row keys in insertion order
     * @see #columnKeySet()
     * @see #containsRow(Object)
     */
    public ImmutableSet<R> rowKeySet() {
        return ImmutableSet.wrap(_rowKeySet);
    }

    /**
     * Returns an immutable set of all column keys in this Sheet.
     * <p>
     * The returned set maintains the insertion order of column keys. It is a live read-only view: changes
     * made through this Sheet are reflected in the view, while attempts to modify the view directly
     * throw {@code UnsupportedOperationException}.
     * This is useful for iterating over columns or checking column existence.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * ImmutableSet<String> colKeys = sheet.columnKeySet();   // returns ["col1", "col2", "col3"]
     *
     * // Iterate over columns
     * for (String colKey : sheet.columnKeySet()) {
     *     ImmutableList<Integer> colValues = sheet.columnValues(colKey);
     * }
     * }</pre>
     *
     * @return a live, immutable set view containing all column keys in insertion order
     * @see #rowKeySet()
     * @see #containsColumn(Object)
     */
    public ImmutableSet<C> columnKeySet() {
        return ImmutableSet.wrap(_columnKeySet);
    }

    /**
     * Checks whether the cell at the specified row key and column key contains a {@code null} value.
     * <p>
     * Returns {@code true} if the cell is {@code null} or if the Sheet has not been initialized
     * with data yet. Returns {@code false} if the cell contains a non-{@code null} value.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, null}, {3, 4}}
     * );
     * boolean isNull = sheet.isNull("row1", "col2");   // returns true
     * }</pre>
     *
     * @param rowKey the key identifying the row
     * @param columnKey the key identifying the column
     * @return {@code true} if the cell contains {@code null} or the Sheet has not been initialized with data, {@code false} otherwise
     * @throws IllegalArgumentException if the row key or column key does not exist in this Sheet
     * @see #isNull(int, int)
     * @see #isNull(Point)
     * @see #get(Object, Object)
     */
    public boolean isNull(final R rowKey, final C columnKey) throws IllegalArgumentException {
        if (_isInitialized) {
            final int rowIndex = getRowIndex(rowKey);
            final int columnIndex = getColumnIndex(columnKey);

            return _columnList.get(columnIndex).get(rowIndex) == null;
        } else {
            checkRowKey(rowKey);
            checkColumnKey(columnKey);

            return true;
        }
    }

    /**
     * Checks whether the cell at the specified row and column indices contains a {@code null} value.
     * <p>
     * This method provides index-based access for checking {@code null} values. Indices are zero-based.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, null}, {3, 4}}
     * );
     * boolean isNull = sheet.isNull(0, 1);   // returns true (row1, col2)
     * }</pre>
     *
     * @param rowIndex the zero-based index of the row
     * @param columnIndex the zero-based index of the column
     * @return {@code true} if the cell contains {@code null} or the Sheet has not been initialized with data, {@code false} otherwise
     * @throws IndexOutOfBoundsException if the indices are out of bounds
     * @see #isNull(Object, Object)
     * @see #isNull(Point)
     * @see #get(int, int)
     */
    public boolean isNull(final int rowIndex, final int columnIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);
        checkColumnIndex(columnIndex);

        if (_isInitialized) {
            return _columnList.get(columnIndex).get(rowIndex) == null;
        } else {
            return true;
        }
    }

    /**
     * Checks whether the cell at the specified point contains a {@code null} value.
     * <p>
     * The Point object encapsulates both row and column indices for convenient access.
     * This is a convenience method equivalent to calling {@code isNull(point.rowIndex, point.columnIndex)}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Point point = Point.of(0, 1);
     * boolean isNull = sheet.isNull(point);   // checks if cell at row 0, column 1 is null
     * }</pre>
     *
     * @param point the Point containing row and column indices
     * @return {@code true} if the cell contains {@code null} or the Sheet has not been initialized with data, {@code false} otherwise
     * @throws IndexOutOfBoundsException if the point's indices are out of bounds
     * @see #isNull(int, int)
     * @see #isNull(Object, Object)
     * @see #get(Point)
     */
    @Beta
    public boolean isNull(final Point point) throws IndexOutOfBoundsException {
        return isNull(point.rowIndex, point.columnIndex);
    }

    /**
     * Retrieves the value stored in the cell identified by the specified row key and column key.
     * <p>
     * Returns {@code null} if the cell has not been initialized or explicitly contains {@code null}.
     * This method provides key-based access to cell values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * Integer value = sheet.get("row1", "col2");   // returns 2
     * }</pre>
     *
     * @param rowKey the key identifying the row
     * @param columnKey the key identifying the column
     * @return the value in the cell, or {@code null} if the cell is uninitialized or explicitly contains {@code null}
     * @throws IllegalArgumentException if the row key or column key does not exist in this Sheet
     * @see #get(int, int)
     * @see #get(Point)
     * @see #set(Object, Object, Object)
     * @see #isNull(Object, Object)
     */
    @MayReturnNull
    public V get(final R rowKey, final C columnKey) throws IllegalArgumentException {
        if (_isInitialized) {
            final int rowIndex = getRowIndex(rowKey);
            final int columnIndex = getColumnIndex(columnKey);

            return _columnList.get(columnIndex).get(rowIndex);
        } else {
            checkRowKey(rowKey);
            checkColumnKey(columnKey);

            return null;
        }
    }

    /**
     * Retrieves the value stored in the cell at the specified row and column indices.
     * <p>
     * This method provides index-based access to cell values. Indices are zero-based.
     * Returns {@code null} if the cell has not been initialized or contains {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * Integer value = sheet.get(0, 1);   // returns 2 (row1, col2)
     * }</pre>
     *
     * @param rowIndex the zero-based index of the row
     * @param columnIndex the zero-based index of the column
     * @return the value in the cell, or {@code null} if the cell is uninitialized or explicitly contains {@code null}
     * @throws IndexOutOfBoundsException if the indices are out of bounds
     * @see #get(Object, Object)
     * @see #get(Point)
     * @see #set(int, int, Object)
     * @see #isNull(int, int)
     */
    @MayReturnNull
    public V get(final int rowIndex, final int columnIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);
        checkColumnIndex(columnIndex);

        if (_isInitialized) {
            return _columnList.get(columnIndex).get(rowIndex);
        } else {
            return null;
        }
    }

    /**
     * Retrieves the value stored in the cell at the specified point.
     * <p>
     * The Point object encapsulates both row and column indices for convenient access.
     * This is a convenience method equivalent to calling {@code get(point.rowIndex, point.columnIndex)}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Point point = Point.of(0, 1);
     * Integer value = sheet.get(point);   // gets value at row 0, column 1
     * }</pre>
     *
     * @param point the Point containing row and column indices
     * @return the value in the cell, or {@code null} if the cell is uninitialized or explicitly contains {@code null}
     * @throws IndexOutOfBoundsException if the point's indices are out of bounds
     * @see #get(int, int)
     * @see #get(Object, Object)
     * @see #set(Point, Object)
     * @see #isNull(Point)
     */
    @MayReturnNull
    @Beta
    public V get(final Point point) throws IndexOutOfBoundsException {
        return get(point.rowIndex, point.columnIndex);
    }

    /**
     * Sets or updates the value in the cell identified by the specified row key and column key.
     * <p>
     * If the cell already contains a value, it is replaced with the new value.
     * The Sheet must not be frozen for this operation to succeed.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * Integer oldValue = sheet.set("row1", "col2", 10);   // returns 2, sets to 10
     * }</pre>
     *
     * @param rowKey the key identifying the row
     * @param columnKey the key identifying the column
     * @param value the new value to store in the cell (can be {@code null})
     * @return the previous value in the cell, or {@code null} if the cell was uninitialized or previously {@code null}
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the row key or column key does not exist in this Sheet
     * @see #set(int, int, Object)
     * @see #set(Point, Object)
     * @see #get(Object, Object)
     * @see #putAll(Sheet)
     */
    @MayReturnNull
    public V set(final R rowKey, final C columnKey, final V value) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();
        final int rowIndex = getRowIndex(rowKey);
        final int columnIndex = getColumnIndex(columnKey);

        init();

        return set(rowIndex, columnIndex, value);
    }

    /**
     * Sets or updates the value in the cell at the specified row and column indices.
     * <p>
     * This method provides index-based access for setting cell values. Indices are zero-based.
     * The Sheet must not be frozen for this operation to succeed.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * Integer oldValue = sheet.set(0, 1, 10);   // returns 2, sets cell[0][1] to 10
     * }</pre>
     *
     * @param rowIndex the zero-based index of the row
     * @param columnIndex the zero-based index of the column
     * @param value the new value to be stored in the cell
     * @return the previous value in the cell, or {@code null} if the cell was uninitialized or previously {@code null}
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IndexOutOfBoundsException if the specified indices are out of bounds
     * @see #set(Object, Object, Object)
     * @see #set(Point, Object)
     * @see #get(int, int)
     */
    @MayReturnNull
    public V set(final int rowIndex, final int columnIndex, final V value) throws IllegalStateException, IndexOutOfBoundsException {
        checkFrozen();
        checkRowIndex(rowIndex);
        checkColumnIndex(columnIndex);

        init();

        final V previousValue = _columnList.get(columnIndex).get(rowIndex);
        _columnList.get(columnIndex).set(rowIndex, value);

        return previousValue;
    }

    /**
     * Sets or updates the value in the cell at the specified point.
     * <p>
     * The Point object encapsulates both row and column indices for convenient access.
     * This is a convenience method equivalent to calling {@code set(point.rowIndex, point.columnIndex, value)}.
     * The Sheet must not be frozen for this operation to succeed.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * Point point = Point.of(0, 1);
     * Integer oldValue = sheet.set(point, 10);   // returns 2, sets cell at row 0, column 1 to 10
     * }</pre>
     *
     * @param point the Point containing row and column indices
     * @param value the new value to be stored in the cell
     * @return the previous value in the cell, or {@code null} if the cell was uninitialized or previously {@code null}
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IndexOutOfBoundsException if the point's indices are out of bounds
     * @see #set(int, int, Object)
     * @see #set(Object, Object, Object)
     * @see #get(Point)
     */
    @Beta
    @MayReturnNull
    public V set(final Point point, final V value) throws IllegalStateException, IndexOutOfBoundsException {
        return set(point.rowIndex, point.columnIndex, value);
    }

    /**
     * <p>Copies all values from the source Sheet into this Sheet.</p>
     *
     * <p>This method transfers data from the source Sheet into this Sheet. The source Sheet must have row keys
     * and column keys that are contained within this Sheet. Values from the source Sheet will replace any
     * existing values in the corresponding cells of this Sheet.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create two sheets
     * Sheet<String, String, Integer> targetSheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {1, 2, 3},
     *         {4, 5, 6}
     *     }
     * );
     *
     * Sheet<String, String, Integer> sourceSheet = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1", "col3"),
     *     new Integer[][] {
     *         {10, 30}
     *     }
     * );
     *
     * // Copy values from source to target
     * targetSheet.putAll(sourceSheet);
     *
     * // Now targetSheet contains:
     * // row1: [10, 2, 30]
     * // row2: [4, 5, 6]
     * }</pre>
     *
     * @param source the source Sheet from which to get the values
     * @throws IllegalStateException if this Sheet is frozen and cannot be modified
     * @throws IllegalArgumentException if the source Sheet contains row keys or column keys that are not present in this Sheet
     * @see #putAll(Sheet, BiFunction)
     * @see #set(Object, Object, Object)
     * @see #merge(Sheet, BiFunction)
     */
    public void putAll(final Sheet<? extends R, ? extends C, ? extends V> source) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        if (!this.rowKeySet().containsAll(source.rowKeySet())) {
            throw new IllegalArgumentException(source.rowKeySet() + " are not all included in this sheet with row key set: " + this.rowKeySet());
        }

        if (!this.columnKeySet().containsAll(source.columnKeySet())) {
            throw new IllegalArgumentException(source.columnKeySet() + " are not all included in this sheet with column key set: " + this.columnKeySet());
        }

        final Sheet<R, C, ? extends V> tmp = (Sheet<R, C, ? extends V>) source;
        int rowIndex = 0;
        int columnIndex = 0;

        for (final R r : tmp.rowKeySet()) {
            rowIndex = getRowIndex(r);
            for (final C c : tmp.columnKeySet()) {
                // this.put(r, c, tmp.get(r, c));
                columnIndex = getColumnIndex(c);

                set(rowIndex, columnIndex, tmp.get(r, c));
            }
        }
    }

    /**
     * <p>Merges all values from the source Sheet into this Sheet, combining each pair of values with the specified merge function.</p>
     *
     * <p>This method combines data from the source Sheet into this Sheet. For every cell within the source Sheet's
     * row and column key range, the provided merge function is applied with the current value from this Sheet and the
     * value from the source Sheet (either of which may be {@code null}), and the result is stored in this Sheet.
     * The source Sheet must have row keys and column keys that are contained within this Sheet.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create two sheets with overlapping data
     * Sheet<String, String, Integer> targetSheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {
     *         {1, 2},
     *         {3, 4}
     *     }
     * );
     *
     * Sheet<String, String, Integer> sourceSheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {
     *         {5, 6},
     *         {7, 8}
     *     }
     * );
     *
     * // Merge source into target, adding values together where they overlap
     * targetSheet.putAll(sourceSheet, (target, source) -> target + source);
     *
     * // Now targetSheet contains:
     * // row1: [6, 8]   (1+5, 2+6)
     * // row2: [10, 12] (3+7, 4+8)
     * }</pre>
     *
     * @param source the source Sheet from which to get the values
     * @param mergeFunction the function used to combine the values; takes the current value from this Sheet and the
     *        value from the source Sheet (either may be {@code null}) and returns the value to store
     * @throws IllegalStateException if this Sheet is frozen and cannot be modified
     * @throws IllegalArgumentException if the source Sheet contains row keys or column keys that are not present in this Sheet
     * @see #putAll(Sheet)
     * @see #set(Object, Object, Object)
     * @see #merge(Sheet, BiFunction)
     */
    public void putAll(final Sheet<? extends R, ? extends C, ? extends V> source, final BiFunction<? super V, ? super V, ? extends V> mergeFunction)
            throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        if (!this.rowKeySet().containsAll(source.rowKeySet())) {
            throw new IllegalArgumentException(source.rowKeySet() + " are not all included in this sheet with row key set: " + this.rowKeySet());
        }

        if (!this.columnKeySet().containsAll(source.columnKeySet())) {
            throw new IllegalArgumentException(source.columnKeySet() + " are not all included in this sheet with column key set: " + this.columnKeySet());
        }

        final Sheet<R, C, ? extends V> tmp = (Sheet<R, C, ? extends V>) source;
        int rowIndex = 0;
        int columnIndex = 0;

        for (final R r : tmp.rowKeySet()) {
            rowIndex = getRowIndex(r);
            for (final C c : tmp.columnKeySet()) {
                // this.put(r, c, tmp.get(r, c));
                columnIndex = getColumnIndex(c);

                set(rowIndex, columnIndex, mergeFunction.apply(get(rowIndex, columnIndex), tmp.get(r, c)));
            }
        }
    }

    /**
     * Removes the value stored in the cell identified by the specified row key and column key.
     * <p>
     * Sets the cell value to {@code null} and returns the previous value. This operation
     * requires the Sheet to be mutable (not frozen) and the cell position to exist.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * Integer removed = sheet.remove("row1", "col2");   // returns 2
     * Integer nowNull = sheet.get("row1", "col2");      // returns null
     * }</pre>
     *
     * @param rowKey the row key of the cell to clear
     * @param columnKey the column key of the cell to clear
     * @return the value that was previously stored in the cell, or {@code null} if the cell was uninitialized or already {@code null}
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the row key or column key does not exist in this Sheet
     * @see #remove(int, int)
     * @see #remove(Point)
     * @see #set(Object, Object, Object)
     */
    @MayReturnNull
    public V remove(final R rowKey, final C columnKey) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        if (_isInitialized) {
            final int rowIndex = getRowIndex(rowKey);
            final int columnIndex = getColumnIndex(columnKey);

            return remove(rowIndex, columnIndex);
        } else {
            checkRowKey(rowKey);
            checkColumnKey(columnKey);

            return null;
        }
    }

    /**
     * Removes the value stored in the cell at the specified row and column indices.
     * <p>
     * Sets the cell value to {@code null} and returns the previous value. Uses zero-based
     * indexing. This operation requires the Sheet to be mutable (not frozen).
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * Integer removed = sheet.remove(0, 1);   // removes value at row1, col2; returns 2
     * Integer nowNull = sheet.get(0, 1);      // returns null
     * }</pre>
     *
     * @param rowIndex the zero-based index of the row
     * @param columnIndex the zero-based index of the column
     * @return the value that was previously stored in the cell, or {@code null} if the cell was uninitialized or already {@code null}
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IndexOutOfBoundsException if rowIndex &lt; 0 or rowIndex &gt;= rowCount() or columnIndex &lt; 0 or columnIndex &gt;= columnCount()
     * @see #remove(Object, Object)
     * @see #remove(Point)
     * @see #set(int, int, Object)
     */
    @MayReturnNull
    public V remove(final int rowIndex, final int columnIndex) throws IllegalStateException, IndexOutOfBoundsException {
        checkFrozen();
        checkRowIndex(rowIndex);
        checkColumnIndex(columnIndex);

        if (_isInitialized) {
            return _columnList.get(columnIndex).set(rowIndex, null);
        } else {
            return null;
        }
    }

    /**
     * Removes the value stored in the cell at the specified Point.
     * <p>
     * Sets the cell value to {@code null} and returns the previous value. The Point
     * encapsulates both row and column indices for convenient access.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * Point point = Point.of(1, 0);            // row2, col1
     * Integer removed = sheet.remove(point);   // returns 3
     * }</pre>
     *
     * @param point the Point containing the row and column indices of the cell
     * @return the value that was previously stored in the cell, or {@code null} if the cell was uninitialized or already {@code null}
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IndexOutOfBoundsException if the point indices are out of bounds
     * @see #remove(int, int)
     * @see #remove(Object, Object)
     * @see #set(Point, Object)
     */
    @Beta
    @MayReturnNull
    public V remove(final Point point) throws IllegalStateException, IndexOutOfBoundsException {
        return remove(point.rowIndex, point.columnIndex);
    }

    /**
     * Checks if the Sheet contains a cell identified by the specified row key and column key.
     * <p>
     * This method verifies if the combination of row and column keys exists in the Sheet structure.
     * It returns {@code true} if both keys are present, regardless of the cell's value (including {@code null}).
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2")
     * );
     *
     * boolean exists = sheet.containsCell("row1", "col1");    // returns true
     * boolean missing = sheet.containsCell("row3", "col1");   // returns false
     * }</pre>
     *
     * @param rowKey the row key to check
     * @param columnKey the column key to check
     * @return {@code true} if the cell position exists in the Sheet structure, {@code false} otherwise
     * @see #containsValueAt(Object, Object, Object)
     * @see #containsRow(Object)
     * @see #containsColumn(Object)
     */
    public boolean containsCell(final R rowKey, final C columnKey) {
        return _rowKeySet.contains(rowKey) && _columnKeySet.contains(columnKey);
    }

    /**
     * Checks if the Sheet contains a cell with the specified row key, column key, and value.
     * <p>
     * This method verifies both the existence of the cell position and that it contains
     * the specified value. Uses {@code Objects.equals()} for value comparison, so it
     * correctly handles {@code null} values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}}
     * );
     *
     * boolean hasValue = sheet.containsValueAt("row1", "col1", 1);     // returns true
     * boolean hasNull = sheet.containsValueAt("row2", "col2", null);   // returns true
     * boolean wrong = sheet.containsValueAt("row1", "col1", 5);        // returns false
     * }</pre>
     *
     * @param rowKey the row key of the cell to check
     * @param columnKey the column key of the cell to check
     * @param value the value to check for equality in the cell
     * @return {@code true} if the cell exists and contains the specified value, {@code false} otherwise
     * @see #containsCell(Object, Object)
     * @see #containsValue(Object)
     * @see #get(Object, Object)
     */
    public boolean containsValueAt(final R rowKey, final C columnKey, final Object value) {
        if (!containsCell(rowKey, columnKey)) {
            return false;
        }

        return N.equals(get(rowKey, columnKey), value);
    }

    /**
     * Checks if the Sheet contains any cell with the specified value.
     * <p>
     * Searches through all cells in the Sheet to find one containing the specified value.
     * Uses {@code Objects.equals()} for comparison, so it correctly handles {@code null} values.
     * For uninitialized Sheets, only returns {@code true} if searching for {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}}
     * );
     *
     * boolean hasOne = sheet.containsValue(1);       // returns true
     * boolean hasNull = sheet.containsValue(null);   // returns true
     * boolean hasFive = sheet.containsValue(5);      // returns false
     * }</pre>
     *
     * @param value the value to search for in all cells
     * @return {@code true} if any cell contains the specified value, {@code false} otherwise
     * @see #containsValueAt(Object, Object, Object)
     * @see #nonNullValueCount()
     */
    public boolean containsValue(final Object value) {
        if (rowCount() == 0 || columnCount() == 0) {
            return false;
        }

        if (_isInitialized) {
            for (final List<V> column : _columnList) {
                //noinspection SuspiciousMethodCalls
                if (column.contains(value)) {
                    return true;
                }
            }

            return false;
        } else {
            return value == null;
        }
    }

    /**
     * Retrieves all the values in the row identified by the specified row key.
     * <p>
     * Returns an immutable list view containing all values in the specified row, in the order
     * corresponding to the column keys. Values are read lazily from this Sheet by row index and
     * column index when the returned list is accessed. The view's size and numeric row position are
     * fixed at creation time: later value updates at that position are visible, but moving or sorting
     * rows does not make the view follow the original row key, and structural removal may invalidate it.
     * The list may contain {@code null} values if cells in the row are empty or this Sheet has not been
     * initialized with values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {1, 2, 3},
     *         {4, null, 6}
     *     }
     * );
     * ImmutableList<Integer> row1 = sheet.rowValues("row1");   // returns [1, 2, 3]
     * ImmutableList<Integer> row2 = sheet.rowValues("row2");   // returns [4, null, 6]
     * }</pre>
     *
     * @param rowKey the row key identifying the row to retrieve
     * @return a lazy immutable list view of values at the row's current numeric position, in column order
     * @throws IllegalArgumentException if the row key does not exist in this Sheet
     * @see #columnValues(Object)
     * @see #setRow(Object, Collection)
     * @see #rowAsMap(Object)
     */
    public ImmutableList<V> rowValues(final R rowKey) throws IllegalArgumentException {
        final int columnLength = columnCount();
        // getRowIndex works on uninitialized sheets too (it lazily builds the index map from the key
        // set); capturing -1 here would poison the lazy view once a later write initializes the sheet.
        final int rowIndex = getRowIndex(rowKey);

        return new ImmutableList<>(new AbstractList<V>() {
            @Override
            public V get(final int columnIndex) {
                if (columnIndex < 0 || columnIndex >= columnLength) {
                    throw new IndexOutOfBoundsException("Index " + columnIndex + " out of bounds for length " + columnLength);
                }

                if (!_isInitialized) {
                    return null;
                }

                return _columnList.get(columnIndex).get(rowIndex);
            }

            @Override
            public int size() {
                return columnLength;
            }
        }, true);
    }

    /**
     * Sets the values for a specific row in the Sheet.
     * <p>
     * Replaces all existing values in the specified row with the values from the provided collection.
     * The values must be in the same order as the column keys. If the collection is empty, all cells
     * in the row will be set to {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * // Replace entire row
     * sheet.setRow("row1", List.of(7, 8, 9));
     * // row1 now contains: [7, 8, 9]
     *
     * // Clear row (set all to null)
     * sheet.setRow("row2", List.of());
     * }</pre>
     *
     * @param rowKey the key of the row to be set
     * @param row the collection of values to set in the row; must match the number of columns or be empty
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist in this Sheet or the collection size does not match column count (unless empty)
     * @see #rowValues(Object)
     * @see #updateRow(Object, Function)
     * @see #addRow(Object, Collection)
     */
    public void setRow(final R rowKey, final Collection<? extends V> row) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int rowIndex = getRowIndex(rowKey);
        final int columnLength = columnCount();

        if (N.notEmpty(row) && row.size() != columnLength) {
            throw new IllegalArgumentException("The size of specified row: " + row.size() + " does not match the size of column key set: " + columnLength); //NOSONAR
        }

        init();

        if (N.isEmpty(row)) {
            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).set(rowIndex, null);
            }
        } else {
            final Iterator<? extends V> iter = row.iterator();

            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).set(rowIndex, iter.next());
            }
        }
    }

    /**
     * Adds a new row to the Sheet at the end.
     * <p>
     * Creates a new row with the specified key and values. The row is appended after all existing rows.
     * The values must be provided in the same order as the column keys. If an empty collection is provided,
     * the new row will contain all {@code null} values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}}
     * );
     *
     * // Add new row with values
     * sheet.addRow("row2", List.of(4, 5, 6));
     *
     * // Add empty row (all nulls)
     * sheet.addRow("row3", List.of());
     * }</pre>
     *
     * @param rowKey the unique key for the new row; must not already exist in the Sheet
     * @param row the collection of values for the new row; must match the number of columns or be empty
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the row key already exists in this Sheet or the collection size does not match column count (unless empty)
     * @see #addRow(int, Object, Collection)
     * @see #removeRow(Object)
     * @see #setRow(Object, Collection)
     */
    public void addRow(final R rowKey, final Collection<? extends V> row) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();
        // Validate before any mutation: BiMap.put would reject the null AFTER the key set was
        // already modified, leaving the sheet permanently inconsistent.
        N.checkArgNotNull(rowKey, "rowKey");

        if (_rowKeySet.contains(rowKey)) {
            throw new IllegalArgumentException("Row '" + rowKey + "' already exists"); //NOSONAR
        }

        final int rowLength = rowCount();
        final int columnLength = columnCount();

        if (N.notEmpty(row) && row.size() != columnLength) {
            throw new IllegalArgumentException("The size of specified row: " + row.size() + " does not match the size of column key set: " + columnLength);
        }

        init();

        _rowKeySet.add(rowKey);
        _rowKeyIndexMap.put(rowKey, rowLength);

        if (N.isEmpty(row)) {
            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).add(null);
            }
        } else {
            final Iterator<? extends V> iter = row.iterator();

            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).add(iter.next());
            }
        }
    }

    /**
     * Inserts a new row at the specified index in the Sheet.
     * <p>
     * Creates a new row with the specified key and values at the given position. Existing rows
     * at and after the specified index are shifted down. The index must be between 0 (insert at beginning)
     * and rowCount() (append at end).
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {5, 6}}
     * );
     *
     * // Insert row at index 1 (between row1 and row3)
     * sheet.addRow(1, "row2", List.of(3, 4));
     * // Sheet now contains rows: ["row1", "row2", "row3"]
     * }</pre>
     *
     * @param rowIndex the zero-based index where the row should be inserted; must be &gt;= 0 and &lt;= rowCount()
     * @param rowKey the unique key for the new row; must not already exist in the Sheet
     * @param row the collection of values for the new row; must match the number of columns or be empty
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IndexOutOfBoundsException if rowIndex &lt; 0 or rowIndex &gt; rowCount()
     * @throws IllegalArgumentException if the row key already exists in this Sheet or the collection size does not match column count (unless empty)
     * @see #addRow(Object, Collection)
     * @see #moveRow(Object, int)
     * @see #removeRow(Object)
     */
    public void addRow(final int rowIndex, final R rowKey, final Collection<? extends V> row)
            throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();
        // Validate before any mutation: BiMap.put would reject the null AFTER the key set was
        // already modified, leaving the sheet permanently inconsistent.
        N.checkArgNotNull(rowKey, "rowKey");

        final int rowLength = rowCount();
        final int columnLength = columnCount();

        N.checkPositionIndex(rowIndex, rowLength);

        if (_rowKeySet.contains(rowKey)) {
            throw new IllegalArgumentException("Row '" + rowKey + "' already exists");
        }

        if (N.notEmpty(row) && row.size() != columnLength) {
            throw new IllegalArgumentException("The size of specified row: " + row.size() + " does not match the size of column key set: " + columnLength);
        }

        if (rowIndex == rowLength) {
            addRow(rowKey, row);
            return;
        }

        init();

        final List<R> tmp = new ArrayList<>(rowLength + 1);
        tmp.addAll(_rowKeySet);
        tmp.add(rowIndex, rowKey);

        _rowKeySet.clear();
        _rowKeySet.addAll(tmp);

        for (int i = _rowKeyIndexMap.size() - 1; i >= rowIndex; i--) {
            _rowKeyIndexMap.put(_rowKeyIndexMap.getByValue(i), i + 1);
        }

        _rowKeyIndexMap.put(rowKey, rowIndex);

        if (N.isEmpty(row)) {
            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).add(rowIndex, null);
            }
        } else {
            final Iterator<? extends V> iter = row.iterator();

            for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                _columnList.get(columnIndex).add(rowIndex, iter.next());
            }
        }

    }

    /**
     * Updates the values in the row identified by the specified row key using the specified function.
     * <p>
     * Applies the given function to each value in the specified row, replacing the original value
     * with the result. The function is called once for each cell in the row, including {@code null} values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * // Double all values in row1
     * sheet.updateRow("row1", v -> v == null ? null : v * 2);
     * // row1 now contains: [2, 4, 6]
     *
     * // Convert to negative values
     * sheet.updateRow("row2", v -> v == null ? 0 : -v);
     * }</pre>
     *
     * @param rowKey the key of the row to be updated
     * @param func the function to apply to each value in the row; receives current value (may be {@code null}) and returns new value
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist in this Sheet
     * @see #updateColumn(Object, Function)
     * @see #updateAll(Function)
     */
    public void updateRow(final R rowKey, final Function<? super V, ? extends V> func) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int rowIndex = this.getRowIndex(rowKey);

        if (columnCount() > 0) {
            this.init();

            for (final List<V> column : _columnList) {
                column.set(rowIndex, func.apply(column.get(rowIndex)));
            }
        }
    }

    /**
     * Removes the row identified by the specified row key from this Sheet.
     * <p>
     * Deletes the entire row and shifts all subsequent rows up. The row key is removed from
     * this Sheet and cannot be reused unless re-added. All values in the removed row are lost.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * sheet.removeRow("row2");
     * // Sheet now contains only row1 and row3
     * // sheet.containsRow("row2") returns false
     * }</pre>
     *
     * @param rowKey the key of the row to be removed
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist in this Sheet
     * @see #removeColumn(Object)
     * @see #addRow(Object, Collection)
     */
    public void removeRow(final R rowKey) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        checkRowKey(rowKey);

        final int removedRowIndex = this.getRowIndex(rowKey);

        _rowKeySet.remove(rowKey);

        {
            final int columnLength = columnCount();
            final int newRowSize = rowCount();
            _rowKeyIndexMap.remove(rowKey);

            if (removedRowIndex == newRowSize) {
                // removed the last row.
            } else {
                for (int i = removedRowIndex; i < newRowSize; i++) {
                    _rowKeyIndexMap.put(_rowKeyIndexMap.getByValue(i + 1), i);
                }
            }

            if (_isInitialized) {
                for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
                    _columnList.get(columnIndex).remove(removedRowIndex); //NOSONAR
                }
            }
        }
    }

    /**
     * Moves the row identified by the specified row key to a new position in this Sheet.
     * <p>
     * Repositions a row to a different index while maintaining all its data. Other rows are
     * shifted accordingly. The row key remains associated with the same row data.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * // Move row2 to the beginning (index 0)
     * sheet.moveRow("row2", 0);
     * // Row order is now: ["row2", "row1", "row3"]
     * }</pre>
     *
     * @param rowKey the key of the row to be moved
     * @param newRowIndex the new zero-based index where the row should be positioned
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist in this Sheet
     * @throws IndexOutOfBoundsException if newRowIndex &lt; 0 or newRowIndex &gt;= rowCount()
     * @see #swapRows(Object, Object)
     * @see #moveColumn(Object, int)
     * @see #addRow(int, Object, Collection)
     */
    public void moveRow(final R rowKey, final int newRowIndex) throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        checkFrozen();

        this.checkRowIndex(newRowIndex);

        final int rowIndex = this.getRowIndex(rowKey);
        final List<R> tmp = new ArrayList<>(rowCount());
        tmp.addAll(_rowKeySet);
        tmp.add(newRowIndex, tmp.remove(rowIndex));

        _rowKeySet.clear();
        _rowKeySet.addAll(tmp);

        _rowKeyIndexMap = null;

        if (_isInitialized && _columnList.size() > 0) {
            for (final List<V> column : _columnList) {
                column.add(newRowIndex, column.remove(rowIndex));
            }
        }
    }

    /**
     * Swaps the positions of two rows in this Sheet.
     * <p>
     * Exchanges the positions of two rows while maintaining their keys and data associations.
     * This is more efficient than using multiple move operations for a simple swap.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * // Swap row1 and row3
     * sheet.swapRows("row1", "row3");
     * // Row order is now: ["row3", "row2", "row1"]
     * // Data follows the rows: {{5, 6}, {3, 4}, {1, 2}}
     * }</pre>
     *
     * @param rowKeyA the key of the first row to swap
     * @param rowKeyB the key of the second row to swap
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if either row key does not exist in this Sheet
     * @see #moveRow(Object, int)
     * @see #swapColumns(Object, Object)
     */
    public void swapRows(final R rowKeyA, final R rowKeyB) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int rowIndexA = this.getRowIndex(rowKeyA);
        final int rowIndexB = this.getRowIndex(rowKeyB);

        final List<R> tmp = new ArrayList<>(rowCount());
        tmp.addAll(_rowKeySet);
        final R tmpRowKeyA = tmp.get(rowIndexA);
        tmp.set(rowIndexA, tmp.get(rowIndexB));
        tmp.set(rowIndexB, tmpRowKeyA);

        _rowKeySet.clear();
        _rowKeySet.addAll(tmp);

        _rowKeyIndexMap.forcePut(tmp.get(rowIndexA), rowIndexA);
        _rowKeyIndexMap.forcePut(tmp.get(rowIndexB), rowIndexB);

        if (_isInitialized && _columnList.size() > 0) {
            V tmpVal = null;

            for (final List<V> column : _columnList) {
                tmpVal = column.get(rowIndexA);
                column.set(rowIndexA, column.get(rowIndexB));
                column.set(rowIndexB, tmpVal);
            }
        }
    }

    /**
     * Renames a row in this Sheet.
     * <p>
     * Changes the key associated with a row while maintaining its position and data.
     * The new key must not already exist in this Sheet.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * // Rename row1 to rowA
     * sheet.renameRow("row1", "rowA");
     * // sheet.containsRow("row1") returns false
     * // sheet.containsRow("rowA") returns true
     * // Data remains unchanged: sheet.get("rowA", "col1") returns 1
     * }</pre>
     *
     * @param rowKey the current key of the row to rename
     * @param newRowKey the new key for the row; must not already exist
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if rowKey does not exist in this Sheet or newRowKey already exists
     * @see #renameColumn(Object, Object)
     */
    public void renameRow(final R rowKey, final R newRowKey) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();
        checkRowKey(rowKey);
        // Validate before any mutation: BiMap.put would reject the null AFTER the key set was
        // already modified, leaving the renamed row's data unreachable.
        N.checkArgNotNull(newRowKey, "newRowKey");

        if (_rowKeySet.contains(newRowKey)) {
            throw new IllegalArgumentException("Invalid new row key: " + N.toString(newRowKey) + ". It's already in the row key set.");
        }

        final int rowIndex = this.getRowIndex(rowKey);
        final List<R> tmp = new ArrayList<>(_rowKeySet);
        tmp.set(rowIndex, newRowKey);

        _rowKeySet.clear();
        _rowKeySet.addAll(tmp);

        if (N.notEmpty(_rowKeyIndexMap)) {
            _rowKeyIndexMap.put(newRowKey, _rowKeyIndexMap.remove(rowKey));
        }
    }

    /**
     * Checks if this Sheet contains a row identified by the specified row key.
     * <p>
     * Tests for the existence of a row with the given key. This method is useful
     * before performing row operations to avoid exceptions.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * boolean exists = sheet.containsRow("row1");    // returns true
     * boolean missing = sheet.containsRow("row3");   // returns false
     * }</pre>
     *
     * @param rowKey the row key to check
     * @return {@code true} if the row exists, {@code false} otherwise
     * @see #containsColumn(Object)
     * @see #containsCell(Object, Object)
     */
    public boolean containsRow(final R rowKey) {
        return _rowKeySet.contains(rowKey);
    }

    /**
     * Retrieves a map representing a row in this Sheet.
     * <p>
     * Returns a map where keys are column keys and values are the cell values for the specified row.
     * The map maintains the column order as defined in this Sheet. Values may be {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, null, 6}}
     * );
     *
     * Map<String, Integer> row1Map = sheet.rowAsMap("row1");
     * // Returns: {"col1"=1, "col2"=2, "col3"=3}
     *
     * Map<String, Integer> row2Map = sheet.rowAsMap("row2");
     * // Returns: {"col1"=4, "col2"=null, "col3"=6}
     * }</pre>
     *
     * @param rowKey the key identifying the row
     * @return a map of column keys to cell values for the specified row
     * @throws IllegalArgumentException if the row key does not exist in this Sheet
     * @see #columnAsMap(Object)
     * @see #rowsMap()
     * @see #rowValues(Object)
     */
    public Map<C, V> rowAsMap(final R rowKey) throws IllegalArgumentException {
        final int columnLength = columnCount();
        final Map<C, V> rowMap = N.newLinkedHashMap(columnLength);

        if (_isInitialized) {
            final int rowIndex = getRowIndex(rowKey);
            int columnIndex = 0;

            for (final C columnKey : this.columnKeySet()) {
                rowMap.put(columnKey, _columnList.get(columnIndex++).get(rowIndex));
            }
        } else {
            checkRowKey(rowKey);

            for (final C columnKey : this.columnKeySet()) {
                rowMap.put(columnKey, null);
            }
        }

        return rowMap;
    }

    /**
     * Retrieves a map representing all rows in this Sheet.
     * <p>
     * Returns a nested map structure where the outer map's keys are row keys and values are
     * maps representing each row. The inner maps have column keys as keys and cell values as values.
     * This provides a complete view of this Sheet's data organized by rows.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * Map<String, Map<String, Integer>> allRows = sheet.rowsMap();
     * // Returns:
     * // {
     * //   "row1" = {"col1"=1, "col2"=2},
     * //   "row2" = {"col1"=3, "col2"=4}
     * // }
     * }</pre>
     *
     * @return a map of row keys to row maps, where each row map contains column keys to cell values
     * @see #columnsMap()
     * @see #rowAsMap(Object)
     */
    public Map<R, Map<C, V>> rowsMap() {
        final Map<R, Map<C, V>> result = N.newLinkedHashMap(this.rowKeySet().size());

        for (final R rowKey : this.rowKeySet()) {
            result.put(rowKey, rowAsMap(rowKey));
        }

        return result;
    }

    /**
     * Retrieves all the values in the column identified by the specified column key.
     * <p>
     * Returns an immutable list view of the specified column's values, in the order corresponding to
     * the row keys. The view's size and numeric column position are fixed at creation time: later value
     * updates at that position are visible, but moving or sorting columns does not make the view follow
     * the original column key, and structural removal may invalidate it. The list may contain
     * {@code null} values if cells in the column are empty.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {
     *         {1, 2},
     *         {3, null},
     *         {5, 6}
     *     }
     * );
     * List<Integer> col1 = sheet.columnValues("col1");   // returns [1, 3, 5]
     * List<Integer> col2 = sheet.columnValues("col2");   // returns [2, null, 6]
     * }</pre>
     *
     * @param columnKey the key identifying the column to retrieve
     * @return a lazy immutable list view of values at the column's current numeric position, in row order
     * @throws IllegalArgumentException if the column key does not exist in this Sheet
     * @see #rowValues(Object)
     * @see #setColumn(Object, Collection)
     * @see #columnAsMap(Object)
     */
    public ImmutableList<V> columnValues(final C columnKey) throws IllegalArgumentException {
        final int rowLength = rowCount();
        // getColumnIndex works on uninitialized sheets too (it lazily builds the index map from the key
        // set). Return a lazy view rather than calling init(): init() would mutate the Sheet - flipping
        // _isInitialized and allocating every column - which a read accessor must not do, least of all on
        // a frozen (immutable, possibly shared) Sheet. Mirrors rowValues(Object).
        final int columnIndex = getColumnIndex(columnKey);

        return new ImmutableList<>(new AbstractList<V>() {
            @Override
            public V get(final int rowIndex) {
                if (rowIndex < 0 || rowIndex >= rowLength) {
                    throw new IndexOutOfBoundsException("Index " + rowIndex + " out of bounds for length " + rowLength);
                }

                if (!_isInitialized) {
                    return null;
                }

                return _columnList.get(columnIndex).get(rowIndex);
            }

            @Override
            public int size() {
                return rowLength;
            }
        }, true);
    }

    /**
     * Sets the values for a specific column in this Sheet.
     * <p>
     * Replaces all existing values in the specified column with the values from the specified collection.
     * The values must be in the same order as the row keys. If the collection is empty, all cells
     * in the column will be set to {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * // Replace entire column
     * sheet.setColumn("col1", List.of(7, 8, 9));
     * // col1 now contains: [7, 8, 9]
     *
     * // Clear column (set all to null)
     * sheet.setColumn("col2", List.of());
     * }</pre>
     *
     * @param columnKey the key of the column to be set
     * @param column the collection of values to set in the column; must match the number of rows or be empty
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist in this Sheet or collection size does not match row count (unless empty)
     * @see #columnValues(Object)
     * @see #updateColumn(Object, Function)
     * @see #addColumn(Object, Collection)
     */
    public void setColumn(final C columnKey, final Collection<? extends V> column) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndex = getColumnIndex(columnKey);

        final int rowLength = rowCount();

        if (N.notEmpty(column) && column.size() != rowLength) {
            throw new IllegalArgumentException("The size of specified column: " + column.size() + " does not match the size of row key set: " + rowLength); //NOSONAR
        }

        init();

        final List<V> existing = _columnList.get(columnIndex);
        if (N.isEmpty(column)) {
            N.fill(existing, 0, rowLength, null);
        } else {
            // Mutate in place rather than replace, so that previously returned column views remain consistent.
            int i = 0;
            for (final V v : column) {
                if (i < existing.size()) {
                    existing.set(i, v);
                } else {
                    existing.add(v);
                }
                i++;
            }
            // Trim any tail entries beyond the expected row length (defensive; should not trigger in normal use).
            while (existing.size() > rowLength) {
                existing.remove(existing.size() - 1);
            }
        }
    }

    /**
     * Adds a new column to this Sheet at the end.
     * <p>
     * Creates a new column with the specified key and values. The column is appended after all existing columns.
     * The values must be provided in the same order as the row keys. If an empty collection is provided,
     * the new column will contain all {@code null} values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1"),
     *     new Integer[][] {{1}, {2}, {3}}
     * );
     *
     * // Add new column with values
     * sheet.addColumn("col2", List.of(4, 5, 6));
     *
     * // Add empty column (all nulls)
     * sheet.addColumn("col3", List.of());
     * }</pre>
     *
     * @param columnKey the unique key for the new column; must not already exist in this Sheet
     * @param column the collection of values for the new column; must match the number of rows or be empty
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the column key already exists or collection size does not match row count (unless empty)
     * @see #addColumn(int, Object, Collection)
     * @see #removeColumn(Object)
     * @see #setColumn(Object, Collection)
     */
    public void addColumn(final C columnKey, final Collection<? extends V> column) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();
        // Validate before any mutation: BiMap.put would reject the null AFTER the key set was
        // already modified, leaving the sheet permanently inconsistent.
        N.checkArgNotNull(columnKey, "columnKey");

        if (_columnKeySet.contains(columnKey)) {
            throw new IllegalArgumentException("Column '" + columnKey + "' already exists");
        }

        final int rowLength = rowCount();
        final int columnLength = columnCount();

        if (N.notEmpty(column) && column.size() != rowLength) {
            throw new IllegalArgumentException("The size of specified column: " + column.size() + " does not match the size of row key set: " + rowLength);
        }

        init();

        _columnKeySet.add(columnKey);
        _columnKeyIndexMap.put(columnKey, columnLength);

        if (N.isEmpty(column)) {
            final List<V> newColumn = new ArrayList<>();
            N.fill(newColumn, 0, rowLength, null);
            _columnList.add(newColumn);
        } else {
            _columnList.add(new ArrayList<>(column));
        }
    }

    /**
     * Inserts a new column at the specified index in this Sheet.
     * <p>
     * Creates a new column with the specified key and values at the given position. Existing columns
     * at and after the specified index are shifted right. The index must be between 0 (insert at beginning)
     * and columnCount() (append at end).
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col3"),
     *     new Integer[][] {{1, 5}, {2, 6}, {3, 7}}
     * );
     *
     * // Insert column at index 1 (between col1 and col3)
     * sheet.addColumn(1, "col2", List.of(10, 20, 30));
     * // Sheet now contains columns: ["col1", "col2", "col3"]
     * }</pre>
     *
     * @param columnIndex the zero-based index where the column should be inserted; must be &gt;= 0 and &lt;= columnCount()
     * @param columnKey the unique key for the new column; must not already exist in this Sheet
     * @param column the collection of values for the new column; must match the number of rows or be empty
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IndexOutOfBoundsException if columnIndex &lt; 0 or columnIndex &gt; columnCount()
     * @throws IllegalArgumentException if the column key already exists or collection size does not match row count (unless empty)
     * @see #addColumn(Object, Collection)
     * @see #moveColumn(Object, int)
     */
    public void addColumn(final int columnIndex, final C columnKey, final Collection<? extends V> column)
            throws IllegalStateException, IndexOutOfBoundsException, IllegalArgumentException {
        checkFrozen();
        // Validate before any mutation: BiMap.put would reject the null AFTER the key set was
        // already modified, leaving the sheet permanently inconsistent.
        N.checkArgNotNull(columnKey, "columnKey");

        final int rowLength = rowCount();
        final int columnLength = columnCount();

        N.checkPositionIndex(columnIndex, columnLength);

        if (_columnKeySet.contains(columnKey)) {
            throw new IllegalArgumentException("Column '" + columnKey + "' already exists");
        }

        if (N.notEmpty(column) && column.size() != rowLength) {
            throw new IllegalArgumentException("The size of specified column: " + column.size() + " does not match the size of row key set: " + rowLength);
        }

        if (columnIndex == columnLength) {
            addColumn(columnKey, column);
            return;
        }

        init();

        final List<C> tmp = new ArrayList<>(columnLength + 1);
        tmp.addAll(_columnKeySet);
        tmp.add(columnIndex, columnKey);

        _columnKeySet.clear();
        _columnKeySet.addAll(tmp);

        for (int i = _columnKeyIndexMap.size() - 1; i >= columnIndex; i--) {
            _columnKeyIndexMap.put(_columnKeyIndexMap.getByValue(i), i + 1);
        }

        _columnKeyIndexMap.put(columnKey, columnIndex);

        if (N.isEmpty(column)) {
            final List<V> newColumn = new ArrayList<>();
            N.fill(newColumn, 0, rowLength, null);
            _columnList.add(columnIndex, newColumn);
        } else {
            _columnList.add(columnIndex, new ArrayList<>(column));
        }
    }

    /**
     * Updates the values in the column identified by the specified column key using the specified function.
     * <p>
     * Applies the given function to each value in the specified column, replacing the original value
     * with the result. The function is called once for each cell in the column, including {@code null} values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 10}, {2, 20}, {3, 30}}
     * );
     *
     * // Double all values in col1
     * sheet.updateColumn("col1", v -> v == null ? null : v * 2);
     * // col1 now contains: [2, 4, 6]
     *
     * // Add 5 to all values in col2
     * sheet.updateColumn("col2", v -> v == null ? 0 : v + 5);
     * }</pre>
     *
     * @param columnKey the key of the column to be updated
     * @param func the function to apply to each value in the column; receives current value (may be {@code null}) and returns new value
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist in this Sheet
     * @see #updateRow(Object, Function)
     * @see #updateAll(Function)
     */
    public void updateColumn(final C columnKey, final Function<? super V, ? extends V> func) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndex = this.getColumnIndex(columnKey);

        if (rowCount() > 0) {
            this.init();

            final int rowLength = rowCount();
            final List<V> column = _columnList.get(columnIndex);

            for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                column.set(rowIndex, func.apply(column.get(rowIndex)));
            }
        }
    }

    /**
     * Removes the column identified by the specified column key from this Sheet.
     * <p>
     * Deletes the entire column and shifts all subsequent columns left. The column key is removed from
     * this Sheet and cannot be reused unless re-added. All values in the removed column are lost.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * sheet.removeColumn("col2");
     * // Sheet now contains only col1 and col3
     * // sheet.containsColumn("col2") returns false
     * }</pre>
     *
     * @param columnKey the key of the column to be removed
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist in this Sheet
     * @see #addColumn(Object, Collection)
     * @see #removeRow(Object)
     */
    public void removeColumn(final C columnKey) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        checkColumnKey(columnKey);

        final int removedColumnIndex = this.getColumnIndex(columnKey);

        _columnKeySet.remove(columnKey);

        {
            final int newColumnLength = columnCount();
            _columnKeyIndexMap.remove(columnKey);

            if (removedColumnIndex == newColumnLength) {
                // removed the last column
            } else {
                for (int i = removedColumnIndex; i < newColumnLength; i++) {
                    _columnKeyIndexMap.put(_columnKeyIndexMap.getByValue(i + 1), i);
                }
            }

            if (_isInitialized) {
                _columnList.remove(removedColumnIndex);
            }
        }
    }

    /**
     * Moves the column identified by the specified column key to a new position in this Sheet.
     * <p>
     * Repositions a column to a different index while maintaining all its data. Other columns are
     * shifted accordingly. The column key remains associated with the same column data.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * // Move col3 to the beginning (index 0)
     * sheet.moveColumn("col3", 0);
     * // Column order is now: ["col3", "col1", "col2"]
     * }</pre>
     *
     * @param columnKey the key of the column to be moved
     * @param newColumnIndex the new zero-based index where the column should be positioned
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist in this Sheet
     * @throws IndexOutOfBoundsException if newColumnIndex &lt; 0 or newColumnIndex &gt;= columnCount()
     * @see #swapColumns(Object, Object)
     * @see #moveRow(Object, int)
     * @see #addColumn(int, Object, Collection)
     */
    public void moveColumn(final C columnKey, final int newColumnIndex) throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        checkFrozen();

        final int columnIndex = this.getColumnIndex(columnKey);
        this.checkColumnIndex(newColumnIndex);

        final List<C> tmp = new ArrayList<>(columnCount());
        tmp.addAll(_columnKeySet);
        tmp.add(newColumnIndex, tmp.remove(columnIndex));

        _columnKeySet.clear();
        _columnKeySet.addAll(tmp);

        _columnKeyIndexMap = null;

        if (_isInitialized && _columnList.size() > 0) {
            _columnList.add(newColumnIndex, _columnList.remove(columnIndex));
        }
    }

    /**
     * Swaps the positions of two columns in this Sheet.
     * <p>
     * Exchanges the positions of two columns while maintaining their keys and data associations.
     * This is more efficient than using multiple move operations for a simple swap.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}
     * );
     *
     * // Swap col1 and col3
     * sheet.swapColumns("col1", "col3");
     * // Column order is now: ["col3", "col2", "col1"]
     * // Data follows the columns: {{3, 2, 1}, {6, 5, 4}, {9, 8, 7}}
     * }</pre>
     *
     * @param columnKeyA the key of the first column to swap
     * @param columnKeyB the key of the second column to swap
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if either column key does not exist in this Sheet
     * @see #moveColumn(Object, int)
     * @see #swapRows(Object, Object)
     */
    public void swapColumns(final C columnKeyA, final C columnKeyB) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        final int columnIndexA = getColumnIndex(columnKeyA);
        final int columnIndexB = getColumnIndex(columnKeyB);

        final List<C> tmp = new ArrayList<>(columnCount());
        tmp.addAll(_columnKeySet);
        final C tmpColumnKeyA = tmp.get(columnIndexA);
        tmp.set(columnIndexA, tmp.get(columnIndexB));
        tmp.set(columnIndexB, tmpColumnKeyA);

        _columnKeySet.clear();
        _columnKeySet.addAll(tmp);

        _columnKeyIndexMap.forcePut(tmp.get(columnIndexA), columnIndexA);
        _columnKeyIndexMap.forcePut(tmp.get(columnIndexB), columnIndexB);

        if (_isInitialized && _columnList.size() > 0) {
            final List<V> tmpColumnA = _columnList.get(columnIndexA);

            _columnList.set(columnIndexA, _columnList.get(columnIndexB));
            _columnList.set(columnIndexB, tmpColumnA);
        }
    }

    /**
     * Renames a column in this Sheet.
     * <p>
     * Changes the key associated with a column while maintaining its position and data.
     * The new key must not already exist in this Sheet.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * // Rename col1 to colA
     * sheet.renameColumn("col1", "colA");
     * // sheet.containsColumn("col1") returns false
     * // sheet.containsColumn("colA") returns true
     * // Data remains unchanged: sheet.get("row1", "colA") returns 1
     * }</pre>
     *
     * @param columnKey the current key of the column to rename
     * @param newColumnKey the new key for the column; must not already exist
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if columnKey does not exist in this Sheet or newColumnKey already exists
     * @see #renameRow(Object, Object)
     */
    public void renameColumn(final C columnKey, final C newColumnKey) throws IllegalStateException, IllegalArgumentException {
        checkFrozen();

        this.checkColumnKey(columnKey);
        // Validate before any mutation: BiMap.put would reject the null AFTER the key set was
        // already modified, leaving the renamed column's data unreachable.
        N.checkArgNotNull(newColumnKey, "newColumnKey");

        if (_columnKeySet.contains(newColumnKey)) {
            throw new IllegalArgumentException("Invalid new column key: " + N.toString(newColumnKey) + ". It's already in the column key set.");
        }

        final int columnIndex = this.getColumnIndex(columnKey);
        final List<C> tmp = new ArrayList<>(_columnKeySet);
        tmp.set(columnIndex, newColumnKey);

        _columnKeySet.clear();
        _columnKeySet.addAll(tmp);

        if (N.notEmpty(_columnKeyIndexMap)) {
            _columnKeyIndexMap.put(newColumnKey, _columnKeyIndexMap.remove(columnKey));
        }
    }

    /**
     * Checks if this Sheet contains a column identified by the specified column key.
     * <p>
     * Tests for the existence of a column with the given key. This method is useful
     * before performing column operations to avoid exceptions.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * boolean exists = sheet.containsColumn("col1");    // returns true
     * boolean missing = sheet.containsColumn("col3");   // returns false
     * }</pre>
     *
     * @param columnKey the column key to check
     * @return {@code true} if the column exists, {@code false} otherwise
     * @see #containsRow(Object)
     * @see #containsCell(Object, Object)
     */
    public boolean containsColumn(final C columnKey) {
        return _columnKeySet.contains(columnKey);
    }

    /**
     * Retrieves a map representing a column in this Sheet.
     * <p>
     * Returns a map where keys are row keys and values are the cell values for the specified column.
     * The map maintains the row order as defined in this Sheet. Values may be {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, null}}
     * );
     *
     * Map<String, Integer> col1Map = sheet.columnAsMap("col1");
     * // Returns: {"row1"=1, "row2"=3, "row3"=5}
     *
     * Map<String, Integer> col2Map = sheet.columnAsMap("col2");
     * // Returns: {"row1"=2, "row2"=4, "row3"=null}
     * }</pre>
     *
     * @param columnKey the key identifying the column
     * @return a map of row keys to cell values for the specified column
     * @throws IllegalArgumentException if the column key does not exist in this Sheet
     * @see #rowAsMap(Object)
     * @see #columnsMap()
     * @see #columnValues(Object)
     */
    public Map<R, V> columnAsMap(final C columnKey) throws IllegalArgumentException {
        final int rowLength = rowCount();
        final Map<R, V> columnMap = N.newLinkedHashMap(rowLength);

        if (_isInitialized) {
            final int columnIndex = getColumnIndex(columnKey);
            final List<V> column = _columnList.get(columnIndex);
            int rowIndex = 0;

            for (final R rowKey : this.rowKeySet()) {
                columnMap.put(rowKey, column.get(rowIndex++));
            }
        } else {
            checkColumnKey(columnKey);

            for (final R rowKey : this.rowKeySet()) {
                columnMap.put(rowKey, null);
            }
        }

        return columnMap;
    }

    /**
     * Retrieves a map representing all columns in this Sheet.
     * <p>
     * Returns a nested map structure where the outer map's keys are column keys and values are
     * maps representing each column. The inner maps have row keys as keys and cell values as values.
     * This provides a complete view of this Sheet's data organized by columns.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * Map<String, Map<String, Integer>> allCols = sheet.columnsMap();
     * // Returns:
     * // {
     * //   "col1" = {"row1"=1, "row2"=3},
     * //   "col2" = {"row1"=2, "row2"=4}
     * // }
     * }</pre>
     *
     * @return a map of column keys to column maps, where each column map contains row keys to cell values
     * @see #rowsMap()
     * @see #columnAsMap(Object)
     */
    public Map<C, Map<R, V>> columnsMap() {
        final Map<C, Map<R, V>> result = N.newLinkedHashMap(this.columnKeySet().size());

        for (final C columnKey : this.columnKeySet()) {
            result.put(columnKey, columnAsMap(columnKey));
        }

        return result;
    }

    /**
     * Returns the number of rows in this Sheet.
     * <p>
     * The row length represents the total count of row keys in this Sheet,
     * regardless of whether the cells contain values or are {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     * int rows = sheet.rowCount();   // returns 3
     * }</pre>
     *
     * @return the number of rows in this Sheet
     * @see #columnCount()
     * @see #isEmpty()
     */
    public int rowCount() {
        return _rowKeySet.size();
    }

    /**
     * Returns the number of columns in this Sheet.
     * <p>
     * The column length represents the total count of column keys in this Sheet,
     * regardless of whether the cells contain values or are {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     * int cols = sheet.columnCount();   // returns 3
     * }</pre>
     *
     * @return the number of columns in this Sheet
     * @see #rowCount()
     * @see #isEmpty()
     */
    public int columnCount() {
        return _columnKeySet.size();
    }

    // TODO should the method name be "replaceAll"? If change the method name to replaceAll, what about updateColumn/updateRow?

    /**
     * Updates all values in this Sheet using the specified function.
     * <p>
     * Applies the specified function to every cell in this Sheet, replacing each value with
     * the result of the function. The function is called for each cell including those
     * containing {@code null} values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}}
     * );
     *
     * // Double all non-null values
     * sheet.updateAll(v -> v == null ? null : v * 2);
     * // Sheet now contains: {{2, 4}, {6, null}}
     * }</pre>
     *
     * @param func the function to apply to each value; receives current value (may be {@code null}) and returns new value
     * @throws IllegalStateException if this Sheet is frozen
     * @see #updateAll(IntBiFunction)
     * @see #updateAll(TriFunction)
     * @see #replaceIf(Predicate, Object)
     */
    public void updateAll(final Function<? super V, ? extends V> func) throws IllegalStateException {
        checkFrozen();

        if (rowCount() > 0 && columnCount() > 0) {
            this.init();

            final int rowLength = rowCount();

            for (final List<V> column : _columnList) {
                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    column.set(rowIndex, func.apply(column.get(rowIndex)));
                }
            }
        }
    }

    /**
     * Updates all values in this Sheet using the specified index-based function.
     * <p>
     * Applies the specified function to every cell in this Sheet, using the cell's row and column
     * indices as input. This is useful when the new value depends on the cell's position.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{0, 0, 0}, {0, 0, 0}}
     * );
     *
     * // Set each cell to row index + column index
     * sheet.updateAll((rowIdx, colIdx) -> rowIdx + colIdx);
     * // Sheet now contains: {{0, 1, 2}, {1, 2, 3}}
     * }</pre>
     *
     * @param func the function to apply; receives row and column indices (zero-based) and returns new value
     * @throws IllegalStateException if this Sheet is frozen
     * @see #updateAll(Function)
     * @see #updateAll(TriFunction)
     */
    public void updateAll(final IntBiFunction<? extends V> func) throws IllegalStateException {
        checkFrozen();

        if (rowCount() > 0 && columnCount() > 0) {
            this.init();

            final int rowLength = rowCount();
            int columnIndex = 0;

            for (final List<V> column : _columnList) {
                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    column.set(rowIndex, func.apply(rowIndex, columnIndex));
                }

                columnIndex++;
            }
        }
    }

    /**
     * Updates all values in the Sheet using the specified key-based function.
     * <p>
     * Applies the given function to every cell in the Sheet, using the cell's row key,
     * column key, and current value as input. This provides maximum flexibility for
     * value updates based on both position and current value.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, String> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new String[][] {{"A", "B"}, {"C", "D"}}
     * );
     *
     * // Combine keys and value
     * sheet.updateAll((r, c, v) -> r + "-" + c + "-" + v);
     * // Sheet now contains:
     * // {{"row1-col1-A", "row1-col2-B"},
     * //  {"row2-col1-C", "row2-col2-D"}}
     * }</pre>
     *
     * @param func the function to apply; receives row key, column key, and current value (may be {@code null}), returns new value
     * @throws IllegalStateException if this Sheet is frozen
     * @see #updateAll(Function)
     * @see #updateAll(IntBiFunction)
     */
    public void updateAll(final TriFunction<? super R, ? super C, ? super V, ? extends V> func) throws IllegalStateException {
        checkFrozen();

        if (rowCount() > 0 && columnCount() > 0) {
            this.init();

            final int rowLength = rowCount();
            int columnIndex = 0;
            C columnKey = null;

            for (final List<V> column : _columnList) {
                columnKey = _columnKeyIndexMap.getByValue(columnIndex);

                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    column.set(rowIndex, func.apply(_rowKeyIndexMap.getByValue(rowIndex), columnKey, column.get(rowIndex)));
                }

                columnIndex++;
            }
        }
    }

    /**
     * Replaces all values in the Sheet that satisfy the specified predicate with the new value.
     * <p>
     * Tests each cell value with the predicate and replaces it with the new value if
     * the predicate returns {@code true}. This is useful for bulk replacements based on conditions.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * // Replace all values greater than 3 with 0
     * sheet.replaceIf(v -> v != null && v > 3, 0);
     * // Sheet now contains: {{1, 2, 3}, {0, 0, 0}}
     *
     * // Replace all null values with -1
     * sheet.replaceIf(Objects::isNull, -1);
     * }</pre>
     *
     * @param predicate the predicate to test each value; receives current value (may be {@code null})
     * @param newValue the value to replace matching cells with
     * @throws IllegalStateException if this Sheet is frozen
     * @see #replaceIf(IntBiPredicate, Object)
     * @see #replaceIf(TriPredicate, Object)
     * @see #updateAll(Function)
     */
    public void replaceIf(final Predicate<? super V> predicate, final V newValue) throws IllegalStateException {
        checkFrozen();

        if (rowCount() > 0 && columnCount() > 0) {
            this.init();

            final int rowLength = rowCount();

            for (final List<V> column : _columnList) {
                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    if (predicate.test(column.get(rowIndex))) {
                        column.set(rowIndex, newValue);
                    }
                }
            }
        }
    }

    /**
     * Replaces all values in the Sheet that satisfy the provided index-based predicate with the new value.
     * <p>
     * Tests each cell using its row and column indices and replaces it with the new value if
     * the predicate returns {@code true}. Useful for position-based replacements.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * // Replace values in the first row with 0
     * sheet.replaceIf((r, c) -> r == 0, 0);
     * // Sheet now contains: {{0, 0}, {3, 4}, {5, 6}}
     *
     * // Replace diagonal values with -1
     * sheet.replaceIf((r, c) -> r == c, -1);
     * }</pre>
     *
     * @param predicate the predicate to test; receives row and column indices (zero-based)
     * @param newValue the value to replace matching cells with
     * @throws IllegalStateException if this Sheet is frozen
     * @see #replaceIf(Predicate, Object)
     * @see #replaceIf(TriPredicate, Object)
     */
    public void replaceIf(final IntBiPredicate predicate, final V newValue) throws IllegalStateException {
        checkFrozen();

        if (rowCount() > 0 && columnCount() > 0) {
            this.init();

            final int rowLength = rowCount();
            int columnIndex = 0;

            for (final List<V> column : _columnList) {
                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    if (predicate.test(rowIndex, columnIndex)) {
                        column.set(rowIndex, newValue);
                    }
                }

                columnIndex++;
            }
        }
    }

    /**
     * Replaces all values in the Sheet that satisfy the provided key-based predicate with the new value.
     * <p>
     * Tests each cell using its row key, column key, and current value, replacing it with the new value if
     * the predicate returns {@code true}. This provides maximum flexibility for conditional replacements.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * // Replace values in col1 that are odd
     * sheet.replaceIf((r, c, v) -> "col1".equals(c) && v != null && v % 2 == 1, 0);
     * // Sheet now contains: {{0, 2}, {0, 4}}
     * }</pre>
     *
     * @param predicate the predicate to test; receives row key, column key, and current value (may be {@code null})
     * @param newValue the value to replace matching cells with
     * @throws IllegalStateException if this Sheet is frozen
     * @see #replaceIf(Predicate, Object)
     * @see #replaceIf(IntBiPredicate, Object)
     */
    public void replaceIf(final TriPredicate<? super R, ? super C, ? super V> predicate, final V newValue) throws IllegalStateException {
        checkFrozen();

        if (rowCount() > 0 && columnCount() > 0) {
            this.init();

            final int rowLength = rowCount();
            int columnIndex = 0;
            R rowKey = null;
            C columnKey = null;
            V val = null;

            for (final List<V> column : _columnList) {
                columnKey = _columnKeyIndexMap.getByValue(columnIndex);

                for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
                    rowKey = _rowKeyIndexMap.getByValue(rowIndex);
                    val = column.get(rowIndex);

                    if (predicate.test(rowKey, columnKey, val)) {
                        column.set(rowIndex, newValue);
                    }
                }

                columnIndex++;
            }
        }
    }

    /**
     * Sorts the rows in the Sheet based on the natural ordering of the row keys.
     * <p>
     * Reorders the rows according to the natural ordering of their keys, as implemented by
     * the {@code Comparable} interface. All row data moves with their respective keys.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("charlie", "alice", "bob"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{3, 4}, {1, 2}, {5, 6}}
     * );
     *
     * sheet.sortByRowKey();
     * // Rows are now ordered: ["alice", "bob", "charlie"]
     * // With data: {{1, 2}, {5, 6}, {3, 4}}
     * }</pre>
     *
     * @throws ClassCastException if the row keys' class does not implement Comparable, or if comparing two row keys throws a ClassCastException
     * @throws IllegalStateException if this Sheet is frozen
     * @see #sortByRowKey(Comparator)
     * @see #sortByColumnKey()
     */
    public void sortByRowKey() throws IllegalStateException {
        sortByRowKey((Comparator<R>) Comparator.naturalOrder());
    }

    /**
     * Sorts the rows in the Sheet based on the row keys using the specified comparator.
     * <p>
     * Reorders the rows according to the specified comparator applied to their keys.
     * All row data moves with their respective keys to maintain data integrity.
     * If the comparator is {@code null}, natural ordering is used (row keys must implement {@code Comparable}).
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("long_name", "a", "medium"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * // Sort by string length
     * sheet.sortByRowKey(Comparator.comparing(String::length));
     * // Rows are now ordered: ["a", "medium", "long_name"]
     * }</pre>
     *
     * @param cmp the comparator to determine row key ordering; {@code null} for natural ordering
     * @throws IllegalStateException if this Sheet is frozen
     * @throws ClassCastException if {@code cmp} is {@code null} and the row keys are not mutually comparable, or if comparing two row keys throws a ClassCastException
     * @see #sortByRowKey()
     * @see #sortByColumnKey(Comparator)
     */
    public void sortByRowKey(final Comparator<? super R> cmp) throws IllegalStateException {
        checkFrozen();

        final int rowLength = rowCount();
        final Indexed<R>[] arrayOfPair = new Indexed[rowLength];
        final Iterator<R> iter = _rowKeySet.iterator();

        for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
            arrayOfPair[rowIndex] = Indexed.of(iter.next(), rowIndex);
        }

        final Comparator<Indexed<R>> pairCmp = createComparatorForIndexedObject(cmp);

        N.sort(arrayOfPair, pairCmp);

        if (_isInitialized) {
            final int columnCount = _columnKeySet.size();
            final Set<Integer> ordered = N.newHashSet(rowLength);
            final V[] tempRow = (V[]) new Object[columnCount];
            List<V> tmpColumn = null;

            for (int i = 0, index = 0; i < rowLength; i++) {
                index = arrayOfPair[i].index();

                if ((index != i) && !ordered.contains(i)) {
                    for (int j = 0; j < columnCount; j++) {
                        tempRow[j] = _columnList.get(j).get(i);
                    }

                    int previous = i;
                    int next = index;

                    do {
                        for (int j = 0; j < columnCount; j++) {
                            tmpColumn = _columnList.get(j);
                            tmpColumn.set(previous, tmpColumn.get(next));
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
        }

        final boolean indexedMapInitialized = N.notEmpty(_rowKeyIndexMap);
        _rowKeySet.clear();

        for (int i = 0; i < rowLength; i++) {
            _rowKeySet.add(arrayOfPair[i].value());

            if (indexedMapInitialized) {
                _rowKeyIndexMap.forcePut(arrayOfPair[i].value(), i);
            }
        }
    }

    /**
     * Sorts the rows in the Sheet based on the values in the specified column.
     * <p>
     * Reorders rows according to the values in the specified column using the specified comparator.
     * All data across all columns is reordered to maintain row integrity. If the comparator is {@code null},
     * natural ordering is used (values must implement {@code Comparable}).
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {3, 10, 100},
     *         {1, 20, 200},
     *         {2, 30, 300}
     *     }
     * );
     *
     * // Sort rows by values in col1 (ascending)
     * sheet.sortRowsByColumnValues("col1", Integer::compareTo);
     * // Rows are now ordered: row2, row3, row1
     * // Data becomes: {{1, 20, 200}, {2, 30, 300}, {3, 10, 100}}
     * }</pre>
     *
     * @param columnKey the key of the column whose values will determine the row ordering
     * @param cmp the comparator to determine the order of values; {@code null} for natural ordering
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the column key does not exist in this Sheet
     * @throws ClassCastException if {@code cmp} is {@code null} and the column's values are not mutually comparable
     * @see #sortColumnsByRowValues(Object, Comparator)
     * @see #sortRowsByColumnValues(Collection, Comparator)
     */
    public void sortRowsByColumnValues(final C columnKey, final Comparator<? super V> cmp) throws IllegalStateException {
        checkFrozen();

        // Resolve (and validate) the key even when uninitialized: the documented IllegalArgumentException
        // for a missing key must not depend on whether data has been written yet.
        final int columnIndex = getColumnIndex(columnKey);

        if (!_isInitialized) {
            return;
        }

        final int rowLength = rowCount();
        final Indexed<V>[] arrayOfPair = new Indexed[rowLength];
        final List<V> column = _columnList.get(columnIndex);

        for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
            arrayOfPair[rowIndex] = Indexed.of(column.get(rowIndex), rowIndex);
        }

        if (N.allMatch(arrayOfPair, it -> it.value() == null)) { // All null values in the specified column.
            return;
        }

        final Comparator<Indexed<V>> pairCmp = createComparatorForIndexedObject(cmp);

        N.sort(arrayOfPair, pairCmp);

        final int columnCount = _columnKeySet.size();
        final Set<Integer> ordered = N.newHashSet(rowLength);
        final V[] tempRow = (V[]) new Object[columnCount];
        List<V> tmpColumn = null;

        for (int i = 0, index = 0; i < rowLength; i++) {
            index = arrayOfPair[i].index();

            if ((index != i) && !ordered.contains(i)) {
                for (int j = 0; j < columnCount; j++) {
                    tempRow[j] = _columnList.get(j).get(i);
                }

                int previous = i;
                int next = index;

                do {
                    for (int j = 0; j < columnCount; j++) {
                        tmpColumn = _columnList.get(j);
                        tmpColumn.set(previous, tmpColumn.get(next));
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

        final boolean indexedMapInitialized = N.notEmpty(_rowKeyIndexMap);
        final Object[] rowKeys = _rowKeySet.toArray(new Object[rowLength]);
        R rowKey = null;
        _rowKeySet.clear();

        for (int i = 0; i < rowLength; i++) {
            rowKey = (R) rowKeys[arrayOfPair[i].index()];
            _rowKeySet.add(rowKey);

            if (indexedMapInitialized) {
                _rowKeyIndexMap.forcePut(rowKey, i);
            }
        }
    }

    /**
     * Sorts the rows in the Sheet based on the values in the specified columns.
     * <p>
     * Reorders the rows according to the combined values from multiple columns using the specified comparator.
     * Each row is represented as an array of values from the specified columns for comparison.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("priority", "secondary", "data"),
     *     new Integer[][] {
     *         {2, 5, 10},
     *         {1, 3, 20},
     *         {1, 4, 30}
     *     }
     * );
     *
     * // Sort rows by priority first, then secondary
     * sheet.sortRowsByColumnValues(List.of("priority", "secondary"),
     *     (a, b) -> {
     *         int result = Integer.compare((Integer)a[0], (Integer)b[0]);
     *         return result != 0 ? result : Integer.compare((Integer)a[1], (Integer)b[1]);
     *     });
     * }</pre>
     *
     * @param columnKeysToSort the keys of columns whose values will determine row ordering; must not be
     *            {@code null} or empty, and every key must exist in this Sheet
     * @param cmp the comparator applied to arrays of values from the specified columns; if {@code null}, arrays are compared
     *            lexicographically with {@code null} elements ordered first, as by {@link Comparators#OBJECT_ARRAY_COMPARATOR}
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if {@code columnKeysToSort} is {@code null} or empty (an empty collection
     *         is accepted only on a zero-column Sheet), or if any specified column key does not exist in this Sheet
     * @see #sortRowsByColumnValues(Object, Comparator)
     * @see #sortColumnsByRowValues(Collection, Comparator)
     */
    public void sortRowsByColumnValues(final Collection<C> columnKeysToSort, final Comparator<? super Object[]> cmp) throws IllegalStateException {
        checkFrozen();

        if (columnKeysToSort == null || (columnKeysToSort.isEmpty() && N.notEmpty(_columnKeySet))) {
            throw new IllegalArgumentException("The specified 'columnKeysToSort' can't be null or empty");
        }

        if (columnKeysToSort.isEmpty()) {
            return;
        }

        final Collection<C> columnKeys = columnKeysToSort;

        // Resolve (and validate) the keys even when uninitialized: the documented IllegalArgumentException
        // for a missing key must not depend on whether data has been written yet.
        final int sortColumnSize = columnKeys.size();
        final int[] columnIndexes = new int[sortColumnSize];
        int idx = 0;

        for (final C columnKey : columnKeys) {
            columnIndexes[idx++] = getColumnIndex(columnKey);
        }

        if (!_isInitialized) {
            return;
        }

        final int rowLength = rowCount();
        final Indexed<Object[]>[] arrayOfPair = new Indexed[rowLength];

        for (int rowIndex = 0; rowIndex < rowLength; rowIndex++) {
            final Object[] values = new Object[sortColumnSize];

            for (int i = 0; i < sortColumnSize; i++) {
                values[i] = _columnList.get(columnIndexes[i]).get(rowIndex);
            }

            arrayOfPair[rowIndex] = Indexed.of(values, rowIndex);
        }

        if (N.allMatch(arrayOfPair, it -> N.allNull(it.value()))) { // All null values in the specified columns.
            return;
        }

        final Comparator<Indexed<Object[]>> pairCmp = createComparatorForIndexedObjectArray(cmp);

        N.sort(arrayOfPair, pairCmp);

        final int columnCount = _columnKeySet.size();
        final Set<Integer> ordered = N.newHashSet(rowLength);
        final V[] tempRow = (V[]) new Object[columnCount];
        List<V> tmpColumn = null;

        for (int i = 0, index = 0; i < rowLength; i++) {
            index = arrayOfPair[i].index();

            if ((index != i) && !ordered.contains(i)) {
                for (int j = 0; j < columnCount; j++) {
                    tempRow[j] = _columnList.get(j).get(i);
                }

                int previous = i;
                int next = index;

                do {
                    for (int j = 0; j < columnCount; j++) {
                        tmpColumn = _columnList.get(j);
                        tmpColumn.set(previous, tmpColumn.get(next));
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

        final boolean indexedMapInitialized = N.notEmpty(_rowKeyIndexMap);
        final Object[] rowKeys = _rowKeySet.toArray(new Object[rowLength]);
        R rowKey = null;
        _rowKeySet.clear();

        for (int i = 0; i < rowLength; i++) {
            rowKey = (R) rowKeys[arrayOfPair[i].index()];
            _rowKeySet.add(rowKey);

            if (indexedMapInitialized) {
                _rowKeyIndexMap.forcePut(rowKey, i);
            }
        }
    }

    /**
     * Sorts the columns in the Sheet based on the natural ordering of the column keys.
     * <p>
     * Reorders columns according to the natural ordering of column keys (as defined by their {@code compareTo} method).
     * Column keys must implement {@code Comparable}. The data in all rows is reordered to match the new column order.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, Integer, String> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of(3, 1, 2),
     *     new String[][] {{"a", "b", "c"}, {"d", "e", "f"}}
     * );
     *
     * // Before sort: columns are [3, 1, 2]
     * sheet.sortByColumnKey();
     * // After sort: columns are [1, 2, 3]
     * // Data reordered: {{"b", "c", "a"}, {"e", "f", "d"}}
     * }</pre>
     *
     * @throws ClassCastException if the column keys' class does not implement Comparable, or if comparing two column keys throws a ClassCastException
     * @throws IllegalStateException if this Sheet is frozen
     * @see #sortByColumnKey(Comparator)
     * @see #sortByRowKey()
     */
    public void sortByColumnKey() throws IllegalStateException {
        sortByColumnKey((Comparator<C>) Comparator.naturalOrder());
    }

    /**
     * Sorts the columns in the Sheet based on the column keys using the specified comparator.
     * <p>
     * Reorders columns according to the specified comparator applied to column keys.
     * The data in all rows is reordered to match the new column order. If the comparator is {@code null},
     * natural ordering is used (column keys must implement {@code Comparable}).
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("apple", "cherry", "banana"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * // Sort columns by reverse alphabetical order
     * sheet.sortByColumnKey((a, b) -> b.compareTo(a));
     * // Columns are now ordered: ["cherry", "banana", "apple"]
     * // Data becomes: {{2, 3, 1}, {5, 6, 4}}
     * }</pre>
     *
     * @param cmp the comparator to determine the order of the column keys; {@code null} for natural ordering
     * @throws IllegalStateException if this Sheet is frozen
     * @throws ClassCastException if {@code cmp} is {@code null} and the column keys are not mutually comparable, or if comparing two column keys throws a ClassCastException
     * @see #sortByColumnKey()
     * @see #sortByRowKey(Comparator)
     */
    public void sortByColumnKey(final Comparator<? super C> cmp) throws IllegalStateException {
        checkFrozen();

        final int columnLength = _columnKeySet.size();
        final Indexed<C>[] arrayOfPair = new Indexed[columnLength];
        final Iterator<C> iter = _columnKeySet.iterator();

        for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
            arrayOfPair[columnIndex] = Indexed.of(iter.next(), columnIndex);
        }

        final Comparator<Indexed<C>> pairCmp = createComparatorForIndexedObject(cmp);

        N.sort(arrayOfPair, pairCmp);

        if (_isInitialized) {
            final Set<Integer> ordered = N.newHashSet(columnLength);
            List<V> tempColumn = null;

            for (int i = 0, index = 0; i < columnLength; i++) {
                index = arrayOfPair[i].index();

                if ((index != i) && !ordered.contains(i)) {
                    tempColumn = _columnList.get(i);

                    int previous = i;
                    int next = index;

                    do {
                        _columnList.set(previous, _columnList.get(next));

                        ordered.add(next);

                        previous = next;
                        next = arrayOfPair[next].index();
                    } while (next != i);

                    _columnList.set(previous, tempColumn);

                    ordered.add(i);
                }
            }
        }

        final boolean indexedMapInitialized = N.notEmpty(_columnKeyIndexMap);
        _columnKeySet.clear();

        for (int i = 0; i < columnLength; i++) {
            _columnKeySet.add(arrayOfPair[i].value());

            if (indexedMapInitialized) {
                _columnKeyIndexMap.forcePut(arrayOfPair[i].value(), i);
            }
        }
    }

    /**
     * Sorts the columns in the Sheet based on the values in the specified row.
     * <p>
     * Reorders the columns according to the values in the specified row using the specified comparator.
     * This effectively sorts the "vertical" arrangement of data based on a "horizontal" slice. If the
     * comparator is {@code null}, natural ordering is used (values must implement {@code Comparable}).
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {3, 1, 2},
     *         {6, 4, 5}
     *     }
     * );
     *
     * // Sort columns by values in row1 (ascending)
     * sheet.sortColumnsByRowValues("row1", Integer::compareTo);
     * // Columns are now ordered: ["col2", "col3", "col1"]
     * // Data becomes: {{1, 2, 3}, {4, 5, 6}}
     * }</pre>
     *
     * @param rowKey the key of the row whose values will determine the column ordering
     * @param cmp the comparator to apply to values in the specified row; {@code null} for natural ordering
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if the row key does not exist in this Sheet
     * @throws ClassCastException if {@code cmp} is {@code null} and the row's values are not mutually comparable
     * @see #sortRowsByColumnValues(Object, Comparator)
     * @see #sortColumnsByRowValues(Collection, Comparator)
     */
    public void sortColumnsByRowValues(final R rowKey, final Comparator<? super V> cmp) throws IllegalStateException {
        checkFrozen();

        // Resolve (and validate) the key even when uninitialized: the documented IllegalArgumentException
        // for a missing key must not depend on whether data has been written yet.
        final int rowIndex = getRowIndex(rowKey);

        if (!_isInitialized) {
            return;
        }

        final int columnLength = columnCount();
        final Indexed<V>[] arrayOfPair = new Indexed[columnLength];

        for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
            arrayOfPair[columnIndex] = Indexed.of(_columnList.get(columnIndex).get(rowIndex), columnIndex);
        }

        if (N.allMatch(arrayOfPair, it -> it.value() == null)) { // All null values in the specified row.
            return;
        }

        final Comparator<Indexed<V>> pairCmp = createComparatorForIndexedObject(cmp);

        N.sort(arrayOfPair, pairCmp);

        final Set<Integer> ordered = N.newHashSet(columnLength);
        List<V> tempColumn = null;

        for (int i = 0, index = 0; i < columnLength; i++) {
            index = arrayOfPair[i].index();

            if ((index != i) && !ordered.contains(i)) {
                tempColumn = _columnList.get(i);

                int previous = i;
                int next = index;

                do {
                    _columnList.set(previous, _columnList.get(next));

                    ordered.add(next);

                    previous = next;
                    next = arrayOfPair[next].index();
                } while (next != i);

                _columnList.set(previous, tempColumn);

                ordered.add(i);
            }
        }

        final boolean indexedMapInitialized = N.notEmpty(_columnKeyIndexMap);
        final Object[] columnKeys = _columnKeySet.toArray(new Object[columnLength]);
        C columnKey = null;
        _columnKeySet.clear();

        for (int i = 0; i < columnLength; i++) {
            columnKey = (C) columnKeys[arrayOfPair[i].index()];
            _columnKeySet.add(columnKey);

            if (indexedMapInitialized) {
                _columnKeyIndexMap.forcePut(columnKey, i);
            }
        }
    }

    /**
     * Sorts the columns in the Sheet based on the values in the specified rows.
     * <p>
     * Reorders the columns according to the combined values from multiple rows using the specified comparator.
     * Each column is represented as an array of values from the specified rows for comparison.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("priority", "secondary", "data"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {
     *         {2, 1, 1},
     *         {5, 3, 4},
     *         {10, 20, 30}
     *     }
     * );
     *
     * // Sort columns by priority first, then secondary
     * sheet.sortColumnsByRowValues(List.of("priority", "secondary"),
     *     (a, b) -> {
     *         int result = Integer.compare((Integer)a[0], (Integer)b[0]);
     *         return result != 0 ? result : Integer.compare((Integer)a[1], (Integer)b[1]);
     *     });
     * }</pre>
     *
     * @param rowKeysToSort the keys of rows whose values will determine column ordering; must not be
     *            {@code null} or empty, and every key must exist in this Sheet
     * @param cmp the comparator applied to arrays of values from the specified rows; if {@code null}, arrays are compared
     *            lexicographically with {@code null} elements ordered first, as by {@link Comparators#OBJECT_ARRAY_COMPARATOR}
     * @throws IllegalStateException if this Sheet is frozen
     * @throws IllegalArgumentException if {@code rowKeysToSort} is {@code null} or empty (an empty collection
     *         is accepted only on a zero-row Sheet), or if any specified row key does not exist in this Sheet
     * @see #sortColumnsByRowValues(Object, Comparator)
     * @see #sortRowsByColumnValues(Collection, Comparator)
     */
    public void sortColumnsByRowValues(final Collection<R> rowKeysToSort, final Comparator<? super Object[]> cmp) throws IllegalStateException {
        checkFrozen();

        if (rowKeysToSort == null || (rowKeysToSort.isEmpty() && N.notEmpty(_rowKeySet))) {
            throw new IllegalArgumentException("The specified 'rowKeysToSort' can't be null or empty");
        }

        if (rowKeysToSort.isEmpty()) {
            return;
        }

        final Collection<R> rowKeys = rowKeysToSort;

        // Resolve (and validate) the keys even when uninitialized: the documented IllegalArgumentException
        // for a missing key must not depend on whether data has been written yet.
        final int sortRowSize = rowKeys.size();
        final int[] rowIndexes = new int[sortRowSize];
        int idx = 0;

        for (final R rowKey : rowKeys) {
            rowIndexes[idx++] = getRowIndex(rowKey);
        }

        if (!_isInitialized) {
            return;
        }

        final int columnLength = columnCount();
        final Indexed<Object[]>[] arrayOfPair = new Indexed[columnLength];

        for (int columnIndex = 0; columnIndex < columnLength; columnIndex++) {
            final Object[] values = new Object[sortRowSize];

            for (int i = 0; i < sortRowSize; i++) {
                values[i] = _columnList.get(columnIndex).get(rowIndexes[i]);
            }

            arrayOfPair[columnIndex] = Indexed.of(values, columnIndex);
        }

        if (N.allMatch(arrayOfPair, it -> N.allNull(it.value()))) { // All null values in the specified rows.
            return;
        }

        final Comparator<Indexed<Object[]>> pairCmp = createComparatorForIndexedObjectArray(cmp);

        N.sort(arrayOfPair, pairCmp);

        final Set<Integer> ordered = N.newHashSet(columnLength);
        List<V> tempColumn = null;

        for (int i = 0, index = 0; i < columnLength; i++) {
            index = arrayOfPair[i].index();

            if ((index != i) && !ordered.contains(i)) {
                tempColumn = _columnList.get(i);

                int previous = i;
                int next = index;

                do {
                    _columnList.set(previous, _columnList.get(next));

                    ordered.add(next);

                    previous = next;
                    next = arrayOfPair[next].index();
                } while (next != i);

                _columnList.set(previous, tempColumn);

                ordered.add(i);
            }
        }

        final boolean indexedMapInitialized = N.notEmpty(_columnKeyIndexMap);
        final Object[] columnKeys = _columnKeySet.toArray(new Object[columnLength]);
        C columnKey = null;
        _columnKeySet.clear();

        for (int i = 0; i < columnLength; i++) {
            columnKey = (C) columnKeys[arrayOfPair[i].index()];
            _columnKeySet.add(columnKey);

            if (indexedMapInitialized) {
                _columnKeyIndexMap.forcePut(columnKey, i);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private <T> Comparator<Indexed<T>> createComparatorForIndexedObject(final Comparator<? super T> cmp) {
        Comparator<Indexed<T>> pairCmp = null;

        if (cmp != null) {
            pairCmp = (a, b) -> cmp.compare(a.value(), b.value());
        } else {
            final Comparator<Indexed<Comparable>> tmp = (a, b) -> N.compare(a.value(), b.value());
            pairCmp = (Comparator) tmp;
        }

        return pairCmp;
    }

    private Comparator<Indexed<Object[]>> createComparatorForIndexedObjectArray(final Comparator<? super Object[]> cmp) {
        if (cmp != null) {
            return (a, b) -> cmp.compare(a.value(), b.value());
        }

        return (a, b) -> Comparators.OBJECT_ARRAY_COMPARATOR.compare(a.value(), b.value());
    }

    /**
     * Creates a shallow copy of this Sheet.
     * <p>
     * The returned Sheet has the same row keys, column keys, and cell value references as this Sheet.
     * Its key collections and cell assignments are independent, but mutable key and value objects are shared.
     * The copy is always mutable, regardless of whether this Sheet is frozen.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *         N.asList("r1", "r2"), N.asList("c1", "c2"),
     *         new Integer[][] {{1, 2}, {3, 4}});
     * Sheet<String, String, Integer> copy = sheet.copy();
     * copy.get("r1", "c2");       // returns 2
     * copy.set("r1", "c1", 99);   // mutate the copy
     * sheet.get("r1", "c1");      // returns 1 (original unaffected)
     * }</pre>
     *
     * @return a new mutable Sheet with independent structure and the same key and cell value references as this Sheet
     * @see #copy(Collection, Collection)
     * @see #clone()
     * @see #transpose()
     */
    public Sheet<R, C, V> copy() {
        final Sheet<R, C, V> copy = new Sheet<>(_rowKeySet, _columnKeySet);

        if (_isInitialized) {
            copy.initIndexMap();

            copy._columnList = new ArrayList<>(_columnList.size());

            for (final List<V> column : _columnList) {
                copy._columnList.add(new ArrayList<>(column));
            }

            copy._isInitialized = true;
        }

        return copy;
    }

    /**
     * Creates a shallow copy of this Sheet containing only the specified row and column keys.
     * <p>
     * The returned Sheet has the same values as this Sheet for the cells at the intersection of the
     * specified row keys and column keys. Its key collections and cell assignments are independent, but
     * mutable key and value objects are shared. The copy is always mutable, regardless of whether this Sheet is frozen.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *         N.asList("r1", "r2"), N.asList("c1", "c2"),
     *         new Integer[][] {{1, 2}, {3, 4}});
     * Sheet<String, String, Integer> sub = sheet.copy(N.asList("r1"), N.asList("c2"));
     * sub.get("r1", "c2");                          // returns 2
     * sub.rowKeySet();                              // returns ["r1"]
     * sub.columnKeySet();                           // returns ["c2"]
     * sheet.copy(null, N.asList("c2"));             // throws IllegalArgumentException (use copy() to copy the whole sheet)
     * sheet.copy(N.asList("rX"), N.asList("c1"));   // throws IllegalArgumentException (unknown row key)
     * }</pre>
     *
     * @param rowKeySet the row keys to include in the copy; must not be {@code null} or empty, and every key
     *            must exist in this Sheet. To copy the entire Sheet, use {@link #copy()} instead
     * @param columnKeySet the column keys to include in the copy; must not be {@code null} or empty, and every
     *            key must exist in this Sheet
     * @return a new mutable Sheet containing the values at the intersection of the specified keys
     * @throws IllegalArgumentException if {@code rowKeySet} or {@code columnKeySet} is {@code null} or empty (an
     *         empty collection is accepted only on the corresponding empty axis of a zero-row / zero-column Sheet),
     *         or if any specified row key or column key is not present in this Sheet
     * @see #copy()
     * @see #clone()
     */
    public Sheet<R, C, V> copy(final Collection<R> rowKeySet, final Collection<C> columnKeySet) {
        if (rowKeySet == null || (rowKeySet.isEmpty() && N.notEmpty(_rowKeySet))) {
            throw new IllegalArgumentException("The specified 'rowKeySet' can't be null or empty");
        }

        if (columnKeySet == null || (columnKeySet.isEmpty() && N.notEmpty(_columnKeySet))) {
            throw new IllegalArgumentException("The specified 'columnKeySet' can't be null or empty");
        }

        if (!_rowKeySet.containsAll(rowKeySet)) {
            throw new IllegalArgumentException("Row keys: " + N.difference(rowKeySet, _rowKeySet) + " are not included in this sheet row keys: " + _rowKeySet);
        }

        if (!_columnKeySet.containsAll(columnKeySet)) {
            throw new IllegalArgumentException(
                    "Column keys: " + N.difference(columnKeySet, _columnKeySet) + " are not included in this sheet Column keys: " + _columnKeySet);
        }

        final Sheet<R, C, V> copy = new Sheet<>(rowKeySet, columnKeySet);

        if (_isInitialized) {
            copy.initIndexMap();

            copy._columnList = new ArrayList<>(copy.columnCount());

            final int[] rowKeyIndices = new int[copy.rowCount()];
            int idx = 0;

            for (final R rowKey : copy._rowKeySet) {
                rowKeyIndices[idx++] = this.getRowIndex(rowKey);
            }

            for (final C columnKey : copy._columnKeySet) {
                final List<V> column = _columnList.get(this.getColumnIndex(columnKey));
                final List<V> newColumn = new ArrayList<>(rowKeyIndices.length);

                for (final int rowIndex : rowKeyIndices) {
                    newColumn.add(column.get(rowIndex));
                }

                copy._columnList.add(newColumn);
            }

            copy._isInitialized = true;
        }

        return copy;
    }

    /**
     * Creates a deep copy of this Sheet using Kryo serialization.
     * <p>
     * All row keys, column keys, and cell values are deeply copied. Changes to the returned copy do
     * not affect this Sheet, and vice versa. The frozen state of this Sheet is preserved in the copy:
     * if this Sheet is frozen, the copy will also be frozen.
     * </p>
     *
     * @return a deep copy of this Sheet with the same frozen state
     * @throws UnsupportedOperationException if the Kryo library is not available on the classpath
     * @see #clone(boolean)
     * @see #copy()
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Beta
    @Override
    public Sheet<R, C, V> clone() { //NOSONAR
        return clone(_isFrozen);
    }

    /**
     * Creates a deep copy of this Sheet using Kryo serialization, with the specified frozen state.
     * <p>
     * All row keys, column keys, and cell values are deeply copied. Changes to the returned copy do
     * not affect this Sheet, and vice versa. The frozen state of the copy is set to the value of the
     * {@code freeze} parameter, regardless of whether this Sheet is frozen.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *         N.asList("r1", "r2"), N.asList("c1", "c2"),
     *         new Integer[][] {{1, 2}, {3, 4}});
     * Sheet<String, String, Integer> frozen = sheet.clone(true);
     * frozen.get("r2", "c2");   // returns 4
     * frozen.isFrozen();        // returns true
     *
     * Sheet<String, String, Integer> mutable = sheet.clone(false);
     * mutable.isFrozen();          // returns false
     * }</pre>
     *
     * @param freeze {@code true} to make the returned copy frozen (read-only); {@code false} for a mutable copy
     * @return a deep copy of this Sheet with the specified frozen state
     * @throws UnsupportedOperationException if the Kryo library is not available on the classpath
     * @see #clone()
     * @see #freeze()
     * @see #copy()
     */
    public Sheet<R, C, V> clone(final boolean freeze) {
        if (kryoParser == null) {
            throw new UnsupportedOperationException("Kryo library is required for deep cloning. Please add Kryo to your classpath or use copy() instead.");
        }

        final Sheet<R, C, V> copy = kryoParser.deepCopy(this);

        copy._isFrozen = freeze;

        return copy;
    }

    /**
     * Merges this Sheet with another Sheet using a merge function.
     * <p>
     * Creates a new Sheet containing the union of all row and column keys from both Sheets. The keys of
     * this Sheet come first, in their existing order, followed by any keys that appear only in the other
     * Sheet. For each cell position, applies the merge function with values from both Sheets.
     * Missing values are represented as {@code null} in the merge function.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet1 = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     * sheet1.println();
     *
     * #        +------+------+
     * #        | col1 | col2 |
     * # +------+------+------+
     * # | row1 | 1    | 2    |
     * # | row2 | 3    | 4    |
     * # +------+------+------+
     *
     * Sheet<String, String, Integer> sheet2 = Sheet.rows(
     *     List.of("row2", "row3"),
     *     List.of("col2", "col3"),
     *     new Integer[][] {{10, 20}, {30, 40}}
     * );
     * sheet2.println();
     *
     * #        +------+------+
     * #        | col2 | col3 |
     * # +------+------+------+
     * # | row2 | 10   | 20   |
     * # | row3 | 30   | 40   |
     * # +------+------+------+
     *
     * // Combine values from both sheets into a "a#b" string (null where a sheet has no value)
     * Sheet<String, String, String> merged = sheet1.merge(sheet2, (a, b) -> a + "#" + b);
     * merged.println();
     *
     * #        +-----------+---------+-----------+
     * #        | col1      | col2    | col3      |
     * # +------+-----------+---------+-----------+
     * # | row1 | 1#null    | 2#null  | null#null |
     * # | row2 | 3#null    | 4#10    | null#20   |
     * # | row3 | null#null | null#30 | null#40   |
     * # +------+-----------+---------+-----------+
     * }</pre>
     *
     * @param <U> the type of values in the other Sheet
     * @param <X> the type of values in the resulting merged Sheet
     * @param b the other Sheet to merge with this one
     * @param mergeFunction function to combine values; receives value from this Sheet and other Sheet (either may be {@code null})
     * @return a new Sheet containing the merged result
     * @see #putAll(Sheet, BiFunction)
     */
    public <U, X> Sheet<R, C, X> merge(final Sheet<? extends R, ? extends C, ? extends U> b,
            final BiFunction<? super V, ? super U, ? extends X> mergeFunction) {
        final Sheet<R, C, U> sheetB = (Sheet<R, C, U>) b;

        final Set<R> newRowKeySet = N.newLinkedHashSet(this.rowKeySet());
        newRowKeySet.addAll(sheetB.rowKeySet());

        final Set<C> newColumnKeySet = N.newLinkedHashSet(this.columnKeySet());
        newColumnKeySet.addAll(sheetB.columnKeySet());

        final Sheet<R, C, X> result = new Sheet<>(newRowKeySet, newColumnKeySet);
        final int[] rowIndexes1 = new int[newRowKeySet.size()], rowIndexes2 = new int[newRowKeySet.size()];
        final int[] columnIndexes1 = new int[newColumnKeySet.size()], columnIndexes2 = new int[newColumnKeySet.size()];

        int idx = 0;
        for (final R rowKey : newRowKeySet) {
            rowIndexes1[idx] = this.containsRow(rowKey) ? this.getRowIndex(rowKey) : -1;
            rowIndexes2[idx] = sheetB.containsRow(rowKey) ? sheetB.getRowIndex(rowKey) : -1;
            idx++;
        }

        idx = 0;

        for (final C columnKey : newColumnKeySet) {
            columnIndexes1[idx] = this.containsColumn(columnKey) ? this.getColumnIndex(columnKey) : -1;
            columnIndexes2[idx] = sheetB.containsColumn(columnKey) ? sheetB.getColumnIndex(columnKey) : -1;
            idx++;
        }

        V e1 = null;
        U e2 = null;

        for (int rowIndex = 0, rowLen = newRowKeySet.size(); rowIndex < rowLen; rowIndex++) {
            for (int columnIndex = 0, columnLen = newColumnKeySet.size(); columnIndex < columnLen; columnIndex++) {
                e1 = rowIndexes1[rowIndex] > -1 && columnIndexes1[columnIndex] > -1 ? this.get(rowIndexes1[rowIndex], columnIndexes1[columnIndex]) : null;
                e2 = rowIndexes2[rowIndex] > -1 && columnIndexes2[columnIndex] > -1 ? sheetB.get(rowIndexes2[rowIndex], columnIndexes2[columnIndex]) : null;
                result.set(rowIndex, columnIndex, mergeFunction.apply(e1, e2));
            }
        }

        return result;
    }

    /**
     * Creates a transposed copy of this Sheet.
     * <p>
     * Returns a new Sheet where rows become columns and columns become rows.
     * Row keys become column keys and vice versa. The data is reorganized accordingly.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> original = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * //        +------+------+------+
     * //        | col1 | col2 | col3 |
     * // +------+------+------+------+
     * // | row1 | 1    | 2    | 3    |
     * // | row2 | 4    | 5    | 6    |
     * // +------+------+------+------+
     *
     * Sheet<String, String, Integer> transposed = original.transpose();
     * //        +------+------+
     * //        | row1 | row2 |
     * // +------+------+------+
     * // | col1 | 1    | 4    |
     * // | col2 | 2    | 5    |
     * // | col3 | 3    | 6    |
     * // +------+------+------+
     * }</pre>
     *
     * @return a new transposed Sheet where row and column keys are swapped
     * @see #copy()
     */
    public Sheet<C, R, V> transpose() {
        final Sheet<C, R, V> copy = new Sheet<>(_columnKeySet, _rowKeySet);

        if (_isInitialized) {
            copy.initIndexMap();

            final int rowLength = copy.rowCount();
            final int columnLength = copy.columnCount();

            copy._columnList = new ArrayList<>(columnLength);

            for (int i = 0; i < columnLength; i++) {
                final List<V> column = new ArrayList<>(rowLength);

                for (int j = 0; j < rowLength; j++) {
                    column.add(_columnList.get(j).get(i));
                }

                copy._columnList.add(column);
            }

            copy._isInitialized = true;
        }

        return copy;
    }

    /**
     * Makes this Sheet immutable by freezing it.
     * <p>
     * Once frozen, all modification operations will throw {@code IllegalStateException}.
     * This includes adding/removing rows/columns, changing values, sorting, etc.
     * The frozen state cannot be reversed. Freezing is in-place and shallow: it does not create a snapshot,
     * make mutable key/value objects immutable, or by itself safely publish the instance to other threads.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1"),
     *     new Integer[][] {{1}}
     * );
     *
     * sheet.freeze();
     * // sheet.set("row1", "col1", 2);   // throws IllegalStateException
     * }</pre>
     *
     * @see #isFrozen()
     * @see #clone(boolean)
     */
    public void freeze() {
        _isFrozen = true;
    }

    /**
     * Checks if this Sheet is frozen (immutable).
     * <p>
     * A frozen Sheet cannot be modified and will throw {@code IllegalStateException}
     * for any modification attempts.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.empty();
     * boolean frozen1 = sheet.isFrozen();   // returns true (empty sheet is frozen)
     *
     * Sheet<String, String, Integer> mutable = new Sheet<>(List.of("r"), List.of("c"));
     * boolean frozen2 = mutable.isFrozen();   // returns false
     * }</pre>
     *
     * @return {@code true} if the Sheet is frozen, {@code false} otherwise
     * @see #freeze()
     */
    public boolean isFrozen() {
        return _isFrozen;
    }

    /**
     * Clears all values in the Sheet, setting them to {@code null}.
     * <p>
     * Removes all values from cells while preserving the Sheet structure (row and column keys).
     * The Sheet dimensions remain unchanged.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.clear();
     * // All values are now null, but structure is preserved
     * Integer val = sheet.get("row1", "col1");   // returns null
     * }</pre>
     *
     * @throws IllegalStateException if this Sheet is frozen
     * @see #isEmpty()
     */
    public void clear() throws IllegalStateException {
        checkFrozen();

        if (_isInitialized && _columnList.size() > 0) {
            for (final List<V> column : _columnList) {
                // column.clear();
                N.fill(column, 0, column.size(), null);
            }
        }
    }

    /**
     * Optimizes memory usage by trimming internal storage capacity.
     * <p>
     * Reduces the capacity of internal column lists to match their current size,
     * potentially freeing unused memory. This is useful after bulk removal of rows.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> rowKeys = Arrays.asList("row1", "row2", "row3");
     * List<String> columnKeys = Arrays.asList("col1", "col2", "col3");
     * Sheet<String, String, Integer> sheet = new Sheet<>(rowKeys, columnKeys);
     * // ... add and remove many rows ...
     * sheet.trimToSize();   // optimize memory usage
     * }</pre>
     *
     * @see #clear()
     */
    public void trimToSize() {
        if (_isInitialized && _columnList.size() > 0) {
            for (final List<V> column : _columnList) {
                if (column instanceof ArrayList) {
                    ((ArrayList<?>) column).trimToSize();
                }
            }
        }
    }

    /**
     * Counts the number of {@code non-null} values in the Sheet.
     * <p>
     * Iterates through all cells and counts those containing {@code non-null} values.
     * Empty or uninitialized cells are counted as {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, null, 3}, {4, 5, null}}
     * );
     *
     * long count = sheet.nonNullValueCount();   // returns 4
     * }</pre>
     *
     * @return the number of {@code non-null} values in the Sheet
     * @see #isEmpty()
     */
    public long nonNullValueCount() {
        if (_isInitialized) {
            long count = 0;

            for (final List<V> col : _columnList) {
                for (final V e : col) {
                    if (e != null) {
                        count++;
                    }
                }
            }

            return count;
        } else {
            return 0;
        }
    }

    /**
     * Checks if the Sheet has no rows or no columns.
     * <p>
     * A Sheet is considered empty if it has zero rows or zero columns.
     * An empty Sheet cannot contain any data.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> empty1 = new Sheet<>(List.of(), List.of("col1"));
     * boolean isEmpty1 = empty1.isEmpty();   // returns true (no rows)
     *
     * Sheet<String, String, Integer> empty2 = new Sheet<>(List.of("row1"), List.of());
     * boolean isEmpty2 = empty2.isEmpty();   // returns true (no columns)
     *
     * Sheet<String, String, Integer> notEmpty = new Sheet<>(List.of("row1"), List.of("col1"));
     * boolean isEmpty3 = notEmpty.isEmpty();   // returns false
     * }</pre>
     *
     * @return {@code true} if the Sheet has no rows or no columns, {@code false} otherwise
     * @see #rowCount()
     * @see #columnCount()
     */
    public boolean isEmpty() {
        return _rowKeySet.isEmpty() || _columnKeySet.isEmpty();
    }

    /**
     * Performs the given action for each cell in the Sheet in row-major order (horizontal, row by row).
     * <p>
     * Iterates through cells row by row, calling the action for each cell including {@code null} values.
     * The action receives the row key, column key, and value for each cell.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.forEachRowMajor((r, c, v) -> System.out.println(r + "," + c + "=" + v));
     * // Prints: row1,col1=1  row1,col2=2  row2,col1=3  row2,col2=4
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each cell; receives row key, column key, and value
     * @throws E if the action throws an exception
     * @see #forEachColumnMajor(Throwables.TriConsumer)
     * @see #forEachNonNullRowMajor(Throwables.TriConsumer)
     */
    public <E extends Exception> void forEachRowMajor(final Throwables.TriConsumer<? super R, ? super C, ? super V, E> action) throws E {
        if (_isInitialized) {
            for (final R rowKey : _rowKeySet) {
                for (final C columnKey : _columnKeySet) {
                    action.accept(rowKey, columnKey, get(rowKey, columnKey));
                }
            }
        } else {
            for (final R rowKey : _rowKeySet) {
                for (final C columnKey : _columnKeySet) {
                    action.accept(rowKey, columnKey, null);
                }
            }
        }
    }

    /**
     * Performs the given action for each cell in the Sheet in column-major order (vertical, column by column).
     * <p>
     * Iterates through cells column by column, calling the action for each cell including {@code null} values.
     * The action receives the row key, column key, and value for each cell.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.forEachColumnMajor((r, c, v) -> System.out.println(r + "," + c + "=" + v));
     * // Prints: row1,col1=1  row2,col1=3  row1,col2=2  row2,col2=4
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each cell; receives row key, column key, and value
     * @throws E if the action throws an exception
     * @see #forEachRowMajor(Throwables.TriConsumer)
     * @see #forEachNonNullColumnMajor(Throwables.TriConsumer)
     */
    public <E extends Exception> void forEachColumnMajor(final Throwables.TriConsumer<? super R, ? super C, ? super V, E> action) throws E {
        if (_isInitialized) {
            for (final C columnKey : _columnKeySet) {
                for (final R rowKey : _rowKeySet) {
                    action.accept(rowKey, columnKey, get(rowKey, columnKey));
                }
            }
        } else {
            for (final C columnKey : _columnKeySet) {
                for (final R rowKey : _rowKeySet) {
                    action.accept(rowKey, columnKey, null);
                }
            }
        }
    }

    /**
     * Performs the given action for each {@code non-null} cell in the Sheet in row-major order (horizontal, row by row).
     * <p>
     * Iterates through cells row by row, calling the action only for cells containing {@code non-null} values.
     * Skips {@code null} and uninitialized cells. The action is guaranteed to receive {@code non-null} values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, null}, {3, 4}}
     * );
     *
     * sheet.forEachNonNullRowMajor((r, c, v) -> System.out.println(r + "," + c + "=" + v));
     * // Prints: row1,col1=1  row2,col1=3  row2,col2=4 (skips null)
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each {@code non-null} cell; receives row key, column key, and {@code non-null} value
     * @throws E if the action throws an exception
     * @see #forEachRowMajor(Throwables.TriConsumer)
     * @see #forEachNonNullColumnMajor(Throwables.TriConsumer)
     */
    public <E extends Exception> void forEachNonNullRowMajor(final Throwables.TriConsumer<? super R, ? super C, ? super V, E> action) throws E {
        if (_isInitialized) {
            V value = null;

            for (final R rowKey : _rowKeySet) {
                for (final C columnKey : _columnKeySet) {
                    if ((value = get(rowKey, columnKey)) != null) {
                        action.accept(rowKey, columnKey, value);
                    }
                }
            }
        } else {
            // ...
        }
    }

    /**
     * Performs the given action for each {@code non-null} cell in the Sheet in column-major order (vertical, column by column).
     * <p>
     * Iterates through cells column by column, calling the action only for cells containing {@code non-null} values.
     * Skips {@code null} and uninitialized cells. The action is guaranteed to receive {@code non-null} values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, null}, {3, 4}}
     * );
     *
     * sheet.forEachNonNullColumnMajor((r, c, v) -> System.out.println(r + "," + c + "=" + v));
     * // Prints: row1,col1=1  row2,col1=3  row2,col2=4 (skips null)
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each {@code non-null} cell; receives row key, column key, and {@code non-null} value
     * @throws E if the action throws an exception
     * @see #forEachColumnMajor(Throwables.TriConsumer)
     * @see #forEachNonNullRowMajor(Throwables.TriConsumer)
     */
    public <E extends Exception> void forEachNonNullColumnMajor(final Throwables.TriConsumer<? super R, ? super C, ? super V, E> action) throws E {
        if (_isInitialized) {
            V value = null;

            for (final C columnKey : _columnKeySet) {
                for (final R rowKey : _rowKeySet) {
                    if ((value = get(rowKey, columnKey)) != null) {
                        action.accept(rowKey, columnKey, value);
                    }
                }
            }
        } else {
            // ...
        }
    }

    /**
     * Returns a stream of all cells in the Sheet in row-major order (horizontal, row by row).
     * <p>
     * Creates a stream that iterates through all cells row by row. Each cell is represented
     * as a Cell object containing the row key, column key, and value.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.rowMajorCells().forEach(cell ->
     *     System.out.println(cell.rowKey() + "," + cell.columnKey() + "=" + cell.value()));
     * }</pre>
     *
     * @return a Stream of Cell objects representing all cells, ordered by rows
     * @see #rowMajorCells(int, int)
     * @see #columnMajorCells()
     */
    public Stream<Sheet.Cell<R, C, V>> rowMajorCells() {
        return rowMajorCells(0, rowCount());
    }

    /**
     * Returns a stream of cells from a range of rows in row-major order (horizontal, row by row).
     * <p>
     * Creates a stream containing cells from the specified row range [fromRowIndex, toRowIndex),
     * ordered row by row. The toRowIndex is exclusive.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * // Get cells from rows 0 and 1 (excluding row 2)
     * sheet.rowMajorCells(0, 2).forEach(cell -> System.out.println(cell.value()));
     * // Prints: 1, 2, 3, 4
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of Cell objects from the specified row range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex &gt; toRowIndex
     * @see #rowMajorCells()
     * @see #columnMajorCells(int, int)
     */
    public Stream<Sheet.Cell<R, C, V>> rowMajorCells(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowCount());

        if (rowCount() == 0 || columnCount() == 0) {
            return Stream.empty();
        }

        final int columnLength = columnCount();

        initIndexMap();

        return Stream.of(new ObjIteratorEx<>() {
            private final long toIndex = (long) toRowIndex * columnLength;
            private long cursor = (long) fromRowIndex * columnLength;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Sheet.Cell<R, C, V> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int rowIndex = (int) (cursor / columnLength);
                final int columnIndex = (int) (cursor++ % columnLength);

                return Cell.of(_rowKeyIndexMap.getByValue(rowIndex), _columnKeyIndexMap.getByValue(columnIndex),
                        _isInitialized ? _columnList.get(columnIndex).get(rowIndex) : null);
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of all cells in the Sheet in column-major order (vertical, column by column).
     * <p>
     * Creates a stream that iterates through all cells column by column. Each cell is represented
     * as a Cell object containing the row key, column key, and value.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.columnMajorCells().forEach(cell ->
     *     System.out.println(cell.rowKey() + "," + cell.columnKey() + "=" + cell.value()));
     * // Prints: row1,col1=1  row2,col1=3  row1,col2=2  row2,col2=4
     * }</pre>
     *
     * @return a Stream of Cell objects representing all cells, ordered by columns
     * @see #columnMajorCells(int, int)
     * @see #rowMajorCells()
     */
    public Stream<Sheet.Cell<R, C, V>> columnMajorCells() {
        return columnMajorCells(0, columnCount());
    }

    /**
     * Returns a stream of cells from a range of columns in column-major order (vertical, column by column).
     * <p>
     * Creates a stream containing cells from the specified column range [fromColumnIndex, toColumnIndex),
     * ordered column by column. The toColumnIndex is exclusive.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * // Get cells from columns 0 and 1 (excluding column 2)
     * sheet.columnMajorCells(0, 2).forEach(cell -> System.out.println(cell.value()));
     * // Prints: 1, 4, 2, 5
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of Cell objects from the specified column range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex &gt; toColumnIndex
     * @see #columnMajorCells()
     * @see #rowMajorCells(int, int)
     */
    public Stream<Sheet.Cell<R, C, V>> columnMajorCells(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnCount());

        if (rowCount() == 0 || columnCount() == 0) {
            return Stream.empty();
        }

        final int rowLength = rowCount();

        initIndexMap();

        return Stream.of(new ObjIteratorEx<>() {
            private final long toIndex = (long) toColumnIndex * rowLength;
            private long cursor = (long) fromColumnIndex * rowLength;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Sheet.Cell<R, C, V> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int rowIndex = (int) (cursor % rowLength);
                final int columnIndex = (int) (cursor++ / rowLength);

                return Cell.of(_rowKeyIndexMap.getByValue(rowIndex), _columnKeyIndexMap.getByValue(columnIndex),
                        _isInitialized ? _columnList.get(columnIndex).get(rowIndex) : null);
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of row streams, where each inner stream contains the cells of one row.
     * <p>
     * Creates a nested stream structure where the outer stream yields rows and each inner stream
     * contains the cells of that row ordered by columns.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.rowCells().forEach(rowStream -> {
     *     rowStream.forEach(cell -> System.out.print(cell.value() + " "));
     *     System.out.println();
     * });
     * // Prints: 1 2 \n 3 4
     * }</pre>
     *
     * @return a Stream of Streams where each inner stream represents a row's cells
     * @see #rowCells(int, int)
     * @see #columnCells()
     */
    public Stream<Stream<Cell<R, C, V>>> rowCells() {
        return rowCells(0, rowCount());
    }

    /**
     * Returns a stream of row streams for a range of rows.
     * <p>
     * Creates a nested stream structure for the specified row range [fromRowIndex, toRowIndex).
     * Each inner stream contains the cells of one row ordered by columns.
     * If the Sheet has no columns, the outer stream still contains one empty stream per selected row.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * // Process rows 0 and 1 (excluding row 2)
     * sheet.rowCells(0, 2).forEach(rowStream -> {
     *     List<Integer> rowValues = rowStream.map(Cell::value).toList();
     *     System.out.println(rowValues);
     * });
     * // Prints: [1, 2] \n [3, 4]
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of Streams for the specified row range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex &gt; toRowIndex
     * @see #rowCells()
     * @see #columnCells(int, int)
     */
    public Stream<Stream<Cell<R, C, V>>> rowCells(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowCount());

        // A zero-column Sheet can still have rows. Return an empty cell stream for each
        // selected row instead of dropping the row dimension altogether.
        if (rowCount() == 0) {
            return Stream.empty();
        }

        initIndexMap();

        final int columnLength = columnCount();

        return Stream.of(new ObjIteratorEx<>() {
            private int rowIndex = fromRowIndex;

            @Override
            public boolean hasNext() {
                return rowIndex < toRowIndex;
            }

            @Override
            public Stream<Cell<R, C, V>> next() {
                if (rowIndex >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Stream.of(new ObjIteratorEx<>() {
                    private final int curRowIndex = rowIndex++;
                    private final R rowKey = _rowKeyIndexMap.getByValue(curRowIndex);
                    private int columnIndex = 0;

                    @Override
                    public boolean hasNext() {
                        return columnIndex < columnLength;
                    }

                    @Override
                    public Cell<R, C, V> next() {
                        if (columnIndex >= columnLength) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final int curColumnIndex = columnIndex++;

                        return Cell.of(rowKey, _columnKeyIndexMap.getByValue(curColumnIndex),
                                _isInitialized ? _columnList.get(curColumnIndex).get(curRowIndex) : null);
                    }

                    @Override
                    public void advance(final long n) {
                        if (n <= 0) {
                            return;
                        }

                        columnIndex = n < columnLength - columnIndex ? columnIndex + (int) n : columnLength;
                    }

                    @Override
                    public long count() {
                        final long ret = columnLength - columnIndex; //NOSONAR
                        columnIndex = columnLength; // count() consumes the remaining elements
                        return ret;
                    }
                });
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                rowIndex = n < toRowIndex - rowIndex ? rowIndex + (int) n : toRowIndex;
            }

            @Override
            public long count() {
                final long ret = toRowIndex - rowIndex; //NOSONAR
                rowIndex = toRowIndex; // count() consumes the remaining elements
                return ret;
            }
        });
    }

    /**
     * Returns a stream of column streams, where each inner stream contains the cells of one column.
     * <p>
     * Creates a nested stream structure where the outer stream yields columns and each inner stream
     * contains the cells of that column ordered by rows.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.columnCells().forEach(colStream -> {
     *     colStream.forEach(cell -> System.out.print(cell.value() + " "));
     *     System.out.println();
     * });
     * // Prints: 1 3 \n 2 4
     * }</pre>
     *
     * @return a Stream of Streams where each inner stream represents a column's cells
     * @see #columnCells(int, int)
     * @see #rowCells()
     */
    public Stream<Stream<Cell<R, C, V>>> columnCells() {
        return columnCells(0, columnCount());
    }

    /**
     * Returns a stream of column streams for a range of columns.
     * <p>
     * Creates a nested stream structure for the specified column range [fromColumnIndex, toColumnIndex).
     * Each inner stream contains the cells of one column ordered by rows.
     * If the Sheet has no rows, the outer stream still contains one empty stream per selected column.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * // Process columns 0 and 1 (excluding column 2)
     * sheet.columnCells(0, 2).forEach(colStream -> {
     *     List<Integer> colValues = colStream.map(Cell::value).toList();
     *     System.out.println(colValues);
     * });
     * // Prints: [1, 4] \n [2, 5]
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of Streams for the specified column range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex &gt; toColumnIndex
     * @see #columnCells()
     * @see #rowCells(int, int)
     */
    public Stream<Stream<Cell<R, C, V>>> columnCells(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnCount());

        // A zero-row Sheet can still have columns. Return an empty cell stream for each
        // selected column instead of dropping the column dimension altogether.
        if (columnCount() == 0) {
            return Stream.empty();
        }

        initIndexMap();

        final int rowLength = rowCount();

        return Stream.of(new ObjIteratorEx<>() {
            private int columnIndex = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return columnIndex < toColumnIndex;
            }

            @Override
            public Stream<Cell<R, C, V>> next() {
                if (columnIndex >= toColumnIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int curColumnIndex = columnIndex++;
                final C columnKey = _columnKeyIndexMap.getByValue(curColumnIndex);

                if (_isInitialized) {
                    final List<V> column = _columnList.get(curColumnIndex);

                    //noinspection resource
                    return IntStream.range(0, rowLength).mapToObj(rowIndex -> Cell.of(_rowKeyIndexMap.getByValue(rowIndex), columnKey, column.get(rowIndex)));
                } else {
                    //noinspection resource
                    return IntStream.range(0, rowLength).mapToObj(rowIndex -> Cell.of(_rowKeyIndexMap.getByValue(rowIndex), columnKey, null));
                }

            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                columnIndex = n < toColumnIndex - columnIndex ? columnIndex + (int) n : toColumnIndex;
            }

            @Override
            public long count() {
                final long ret = toColumnIndex - columnIndex; //NOSONAR
                columnIndex = toColumnIndex; // count() consumes the remaining elements
                return ret;
            }

        });
    }

    /**
     * Returns a stream of all coordinate points in the Sheet in row-major order (horizontal, row by row).
     * <p>
     * Creates a stream of Point objects representing cell coordinates. Each Point contains
     * zero-based row and column indices. Points are ordered row by row.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2")
     * );
     *
     * sheet.rowMajorPoints().forEach(point ->
     *     System.out.println("(" + point.rowIndex() + "," + point.columnIndex() + ")"));
     * // Prints: (0,0) (0,1) (1,0) (1,1)
     * }</pre>
     *
     * @return a Stream of Point objects representing all cell coordinates, ordered by rows
     * @see #rowMajorPoints(int, int)
     * @see #columnMajorPoints()
     */
    public Stream<Point> rowMajorPoints() {
        return rowMajorPoints(0, rowCount());
    }

    /**
     * Returns a stream of coordinate points for a range of rows in row-major order (horizontal, row by row).
     * <p>
     * Creates a stream of Point objects representing cell coordinates in the specified row range
     * [fromRowIndex, toRowIndex). Each Point contains zero-based row and column indices. Points are
     * ordered row by row.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * // Get points for rows 0 and 1 (excluding row 2)
     * sheet.rowMajorPoints(0, 2).forEach(point ->
     *     System.out.println("(" + point.rowIndex() + "," + point.columnIndex() + ")"));
     * // Prints: (0,0) (0,1) (1,0) (1,1)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of Point objects for the specified row range, ordered by rows
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex &gt; toRowIndex
     * @see #rowMajorPoints()
     * @see #columnMajorPoints(int, int)
     * @see #rowPoints(int, int)
     */
    public Stream<Point> rowMajorPoints(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowCount());

        final int columnLength = columnCount();

        //noinspection resource
        return IntStream.range(fromRowIndex, toRowIndex)
                .flatMapToObj(rowIndex -> IntStream.range(0, columnLength).mapToObj(columnIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of all coordinate points in the Sheet in column-major order (vertical, column by column).
     * <p>
     * Creates a stream of Point objects representing cell coordinates. Each Point contains
     * zero-based row and column indices. Points are ordered column by column.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = new Sheet<>(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2")
     * );
     *
     * sheet.columnMajorPoints().forEach(point ->
     *     System.out.println("(" + point.rowIndex() + "," + point.columnIndex() + ")"));
     * // Prints: (0,0) (1,0) (0,1) (1,1)
     * }</pre>
     *
     * @return a Stream of Point objects representing all cell coordinates, ordered by columns
     * @see #columnMajorPoints(int, int)
     * @see #rowMajorPoints()
     * @see #columnPoints()
     */
    public Stream<Point> columnMajorPoints() {
        return columnMajorPoints(0, columnCount());
    }

    /**
     * Returns a stream of coordinate points for a range of columns in column-major order (vertical, column by column).
     * <p>
     * Creates a stream of Point objects representing cell coordinates in the specified column range
     * [fromColumnIndex, toColumnIndex). Each Point contains zero-based row and column indices. Points are
     * ordered column by column.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * // Get points for columns 0 and 1 (excluding column 2)
     * sheet.columnMajorPoints(0, 2).forEach(point ->
     *     System.out.println("(" + point.rowIndex() + "," + point.columnIndex() + ")"));
     * // Prints: (0,0) (1,0) (0,1) (1,1)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of Point objects for the specified column range, ordered by columns
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex &gt; toColumnIndex
     * @see #columnMajorPoints()
     * @see #rowMajorPoints(int, int)
     * @see #columnPoints(int, int)
     */
    public Stream<Point> columnMajorPoints(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnCount());

        final int rowLength = rowCount();

        //noinspection resource
        return IntStream.range(fromColumnIndex, toColumnIndex)
                .flatMapToObj(columnIndex -> IntStream.range(0, rowLength).mapToObj(rowIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of point streams where each inner stream represents the points in one row.
     * <p>
     * Creates a nested stream structure where the outer stream yields rows and each inner stream
     * contains the Point coordinates of that row ordered by columns. Each Point contains
     * zero-based row and column indices.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.rowPoints().forEach(rowStream -> {
     *     rowStream.forEach(point -> System.out.print("(" + point.rowIndex() + "," + point.columnIndex() + ") "));
     *     System.out.println();
     * });
     * // Prints: (0,0) (0,1) \n (1,0) (1,1)
     * }</pre>
     *
     * @return a Stream of Streams where each inner stream represents a row's coordinate points
     * @see #rowPoints(int, int)
     * @see #columnPoints()
     * @see #rowMajorPoints()
     */
    public Stream<Stream<Point>> rowPoints() {
        return rowPoints(0, rowCount());
    }

    /**
     * Returns a stream of point streams for a range of rows.
     * <p>
     * Creates a nested stream structure for the specified row range [fromRowIndex, toRowIndex).
     * Each inner stream contains the Point coordinates of one row ordered by columns. Each Point contains
     * zero-based row and column indices.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * // Process rows 0 and 1 (excluding row 2)
     * sheet.rowPoints(0, 2).forEach(rowStream -> {
     *     List<Point> points = rowStream.toList();
     *     System.out.println(points);
     * });
     * // Prints: [(0,0), (0,1)] \n [(1,0), (1,1)]
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of Streams for the specified row range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex &gt; toRowIndex
     * @see #rowPoints()
     * @see #columnPoints(int, int)
     * @see #rowMajorPoints(int, int)
     */
    public Stream<Stream<Point>> rowPoints(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowCount());

        final int columnLength = columnCount();

        //noinspection resource
        return IntStream.range(fromRowIndex, toRowIndex)
                .mapToObj(rowIndex -> IntStream.range(0, columnLength).mapToObj(columnIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of point streams where each inner stream represents the points in one column.
     * <p>
     * Creates a nested stream structure where the outer stream yields columns and each inner stream
     * contains the Point coordinates of that column ordered by rows. Each Point contains
     * zero-based row and column indices.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.columnPoints().forEach(colStream -> {
     *     colStream.forEach(point -> System.out.print("(" + point.rowIndex() + "," + point.columnIndex() + ") "));
     *     System.out.println();
     * });
     * // Prints: (0,0) (1,0) \n (0,1) (1,1)
     * }</pre>
     *
     * @return a Stream of Streams where each inner stream represents a column's coordinate points
     * @see #columnPoints(int, int)
     * @see #rowPoints()
     * @see #columnMajorPoints()
     */
    public Stream<Stream<Point>> columnPoints() {
        return columnPoints(0, columnCount());
    }

    /**
     * Returns a stream of point streams for a range of columns.
     * <p>
     * Creates a nested stream structure for the specified column range [fromColumnIndex, toColumnIndex).
     * Each inner stream contains the Point coordinates of one column ordered by rows. Each Point contains
     * zero-based row and column indices.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * // Process only first two columns
     * sheet.columnPoints(0, 2).forEach(colStream -> {
     *     List<Point> colPoints = colStream.toList();
     *     System.out.println(colPoints);
     * });
     * // Prints: [(0,0), (1,0)] \n [(0,1), (1,1)]
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of Streams for the specified column range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex &gt; toColumnIndex
     * @see #columnPoints()
     * @see #rowPoints(int, int)
     * @see #columnMajorPoints(int, int)
     */
    public Stream<Stream<Point>> columnPoints(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnCount());

        final int rowLength = rowCount();

        //noinspection resource
        return IntStream.range(fromColumnIndex, toColumnIndex)
                .mapToObj(columnIndex -> IntStream.range(0, rowLength).mapToObj(rowIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of all values in the Sheet in row-major order (horizontal, row by row).
     * <p>
     * Creates a stream containing all cell values, ordered row by row. Includes {@code null} values
     * from empty or uninitialized cells.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}}
     * );
     *
     * sheet.rowMajorStream().forEach(System.out::println);
     * // Prints: 1, 2, 3, null
     * }</pre>
     *
     * @return a Stream of values from all cells, ordered by rows
     * @see #rowMajorStream(int, int)
     * @see #columnMajorStream()
     */
    public Stream<V> rowMajorStream() {
        return rowMajorStream(0, rowCount());
    }

    /**
     * Returns a stream of values from a range of rows in row-major order (horizontal, row by row).
     * <p>
     * Creates a stream containing values from the specified row range [fromRowIndex, toRowIndex),
     * ordered row by row. The toRowIndex is exclusive. Includes {@code null} values from empty or uninitialized cells.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}, {5, 6}}
     * );
     *
     * // Get values from rows 0 and 1 (excluding row 2)
     * sheet.rowMajorStream(0, 2).forEach(System.out::println);
     * // Prints: 1, 2, 3, null
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of values from the specified row range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex &gt; toRowIndex
     * @see #rowMajorStream()
     * @see #columnMajorStream(int, int)
     * @see #rowStreams(int, int)
     */
    public Stream<V> rowMajorStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowCount());

        if (rowCount() == 0 || columnCount() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int columnLength = columnCount();
            private final long toIndex = (long) toRowIndex * columnLength;
            private long cursor = (long) fromRowIndex * columnLength;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public V next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (_isInitialized) {
                    return _columnList.get((int) (cursor % columnLength)).get((int) (cursor++ / columnLength));
                } else {
                    cursor++;
                    return null;
                }
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of all values in the Sheet in column-major order (vertical, column by column).
     * <p>
     * Creates a stream containing all cell values, ordered column by column. Includes {@code null} values
     * from empty or uninitialized cells.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}}
     * );
     *
     * sheet.columnMajorStream().forEach(System.out::println);
     * // Prints: 1, 3, 2, null
     * }</pre>
     *
     * @return a Stream of values from all cells, ordered by columns
     * @see #columnMajorStream(int, int)
     * @see #rowMajorStream()
     * @see #columnStreams()
     */
    public Stream<V> columnMajorStream() {
        return columnMajorStream(0, columnCount());
    }

    /**
     * Returns a stream of values from a range of columns in column-major order (vertical, column by column).
     * <p>
     * Creates a stream containing values from the specified column range [fromColumnIndex, toColumnIndex),
     * ordered column by column. The toColumnIndex is exclusive. Includes {@code null} values from empty or uninitialized cells.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, null, 6}}
     * );
     *
     * // Get values from columns 0 and 1 (excluding column 2)
     * sheet.columnMajorStream(0, 2).forEach(System.out::println);
     * // Prints: 1, 4, 2, null
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of values from the specified column range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex &gt; toColumnIndex
     * @see #columnMajorStream()
     * @see #rowMajorStream(int, int)
     */
    public Stream<V> columnMajorStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnCount());

        if (rowCount() == 0 || columnCount() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int rowLength = rowCount();
            private final long toIndex = (long) toColumnIndex * rowLength;
            private long cursor = (long) fromColumnIndex * rowLength;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public V next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (_isInitialized) {
                    return _columnList.get((int) (cursor / rowLength)).get((int) (cursor++ % rowLength));
                } else {
                    cursor++;
                    return null;
                }
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of row streams, where each inner stream contains the values of one row.
     * <p>
     * Creates a nested stream structure where the outer stream yields rows and each inner stream
     * contains the values of that row ordered by columns. Includes {@code null} values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}}
     * );
     *
     * sheet.rowStreams().forEach(rowStream -> {
     *     rowStream.forEach(value -> System.out.print(value + " "));
     *     System.out.println();
     * });
     * // Prints: 1 2 \n 3 null
     * }</pre>
     *
     * @return a Stream of Streams where each inner stream represents a row's values
     * @see #rowStreams(int, int)
     * @see #columnStreams()
     */
    public Stream<Stream<V>> rowStreams() {
        return rowStreams(0, rowCount());
    }

    /**
     * Returns a stream of row streams for a range of rows.
     * <p>
     * Creates a nested stream structure for the specified row range [fromRowIndex, toRowIndex).
     * Each inner stream contains the values of one row ordered by columns. Includes {@code null} values.
     * If the Sheet has no columns, the outer stream still contains one empty stream per selected row.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, null}, {5, 6}}
     * );
     *
     * // Process rows 0 and 1 (excluding row 2)
     * sheet.rowStreams(0, 2).forEach(rowStream -> {
     *     List<Integer> rowValues = rowStream.toList();
     *     System.out.println(rowValues);
     * });
     * // Prints: [1, 2] \n [3, null]
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of Streams for the specified row range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex &gt; toRowIndex
     * @see #rowStreams()
     * @see #columnStreams(int, int)
     */
    public Stream<Stream<V>> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowCount());

        // A zero-column Sheet can still have rows. Return an empty value stream for each
        // selected row instead of dropping the row dimension altogether.
        if (rowCount() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<V> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Stream.of(new ObjIteratorEx<>() {
                    private final int rowIndex = cursor++;
                    private final int toIndex2 = columnCount();
                    private int cursor2 = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor2 < toIndex2;
                    }

                    @Override
                    public V next() {
                        if (cursor2 >= toIndex2) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        if (_isInitialized) {
                            return _columnList.get(cursor2++).get(rowIndex);
                        } else {
                            cursor2++;
                            return null;
                        }
                    }

                    @Override
                    public void advance(final long n) {
                        if (n <= 0) {
                            return;
                        }

                        cursor2 = n < toIndex2 - cursor2 ? cursor2 + (int) n : toIndex2;
                    }

                    @Override
                    public long count() {
                        final long ret = toIndex2 - cursor2; //NOSONAR
                        cursor2 = toIndex2;
                        return ret;
                    }
                });
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of column streams, where each inner stream contains the values of one column.
     * <p>
     * Creates a nested stream structure where the outer stream yields columns and each inner stream
     * contains the values of that column ordered by rows. Includes {@code null} values.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.columnStreams().forEach(colStream -> {
     *     List<Integer> colValues = colStream.toList();
     *     System.out.println(colValues);
     * });
     * // Prints: [1, 3] \n [2, 4]
     * }</pre>
     *
     * @return a Stream of Streams where each inner stream represents a column's values
     * @see #columnStreams(int, int)
     * @see #rowStreams()
     */
    public Stream<Stream<V>> columnStreams() {
        return columnStreams(0, columnCount());
    }

    /**
     * Returns a stream of column value streams for a range of columns.
     * <p>
     * Creates a nested stream structure for the specified column range [fromColumnIndex, toColumnIndex).
     * Each inner stream contains the values of one column ordered by rows. Includes {@code null} values.
     * If the Sheet has no rows, the outer stream still contains one empty stream per selected column.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * // Process only first two columns
     * sheet.columnStreams(0, 2).forEach(colStream -> {
     *     List<Integer> colValues = colStream.toList();
     *     System.out.println(colValues);
     * });
     * // Prints: [1, 4] \n [2, 5]
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of Streams for the specified column range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex &gt; toColumnIndex
     * @see #columnStreams()
     * @see #rowStreams(int, int)
     */
    public Stream<Stream<V>> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnCount());

        // A zero-row Sheet can still have columns. Return an empty value stream for each
        // selected column instead of dropping the column dimension altogether.
        if (columnCount() == 0) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toColumnIndex;
            private int cursor = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<V> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (_isInitialized) {
                    return Stream.of(_columnList.get(cursor++));
                } else {
                    cursor++;
                    return Stream.repeat(null, rowCount());
                }
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of key-value pairs representing all rows in the Sheet.
     * <p>
     * Each pair contains a row key and a stream of all values in that row (ordered by columns).
     * This provides a convenient way to process rows with their identifiers.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.rows().forEach(pair -> {
     *     String rowKey = pair.left();
     *     Stream<Integer> values = pair.right();
     *     System.out.println(rowKey + ": " + values.toList());
     * });
     * // Prints: row1: [1, 2]  row2: [3, 4]
     * }</pre>
     *
     * @return a Stream of Pair objects where each pair contains a row key and its value stream
     * @see #rows(int, int)
     * @see #columns()
     */
    public Stream<Pair<R, Stream<V>>> rows() {
        return rows(0, rowCount());
    }

    /**
     * Returns a stream of key-value pairs for a range of rows in the Sheet.
     * <p>
     * Each pair contains a row key and a stream of all values in that row (ordered by columns).
     * Only rows in the specified range [fromRowIndex, toRowIndex) are included. Includes {@code null} values.
     * A row with no columns is represented by its key and an empty value stream.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * // Process rows 0 and 1 (excluding row 2)
     * sheet.rows(0, 2).forEach(pair -> {
     *     String rowKey = pair.left();
     *     List<Integer> values = pair.right().toList();
     *     System.out.println(rowKey + ": " + values);
     * });
     * // Prints: row1: [1, 2]  row2: [3, 4]
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of Pair objects for the specified row range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex &gt; toRowIndex
     * @see #rows()
     * @see #columns(int, int)
     * @see #rowStreams(int, int)
     */
    public Stream<Pair<R, Stream<V>>> rows(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowCount());

        // Rows remain addressable even when each row contains zero values.
        if (rowCount() == 0) {
            return Stream.empty();
        }

        initIndexMap();

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Pair<R, Stream<V>> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final R rowKey = _rowKeyIndexMap.getByValue(cursor);

                final Stream<V> row = Stream.of(new ObjIteratorEx<>() {
                    private final int rowIndex = cursor++;
                    private final int toIndex2 = columnCount();
                    private int cursor2 = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor2 < toIndex2;
                    }

                    @Override
                    public V next() {
                        if (cursor2 >= toIndex2) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        if (_isInitialized) {
                            return _columnList.get(cursor2++).get(rowIndex);
                        } else {
                            cursor2++;
                            return null;
                        }
                    }

                    @Override
                    public void advance(final long n) {
                        if (n <= 0) {
                            return;
                        }

                        cursor2 = n < toIndex2 - cursor2 ? cursor2 + (int) n : toIndex2;
                    }

                    @Override
                    public long count() {
                        final long ret = toIndex2 - cursor2; //NOSONAR
                        cursor2 = toIndex2;
                        return ret;
                    }
                });

                return Pair.of(rowKey, row);
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of key-value pairs representing all rows in the Sheet, where each pair consists of a row key and a mapped value.
     * <p>
     * Each pair in the stream represents a row in the Sheet, with the row key and a value obtained by applying the provided {@code rowMapper} function to the row's values.
     * The pairs are ordered by rows.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * // Map each row to a joined string of its values
     * sheet.rows((rowIndex, row) -> row.join("-"))
     *      .forEach(pair -> System.out.println(pair.left() + ": " + pair.right()));
     * // Prints:
     * // row1: 1-2
     * // row2: 3-4
     * }</pre>
     *
     * <p>Note: the {@code DisposableObjArray} passed to {@code rowMapper} is reused across
     * invocations and must not be retained beyond the scope of the mapper call.</p>
     *
     * @param <T> the type of the mapped value for each row.
     * @param rowMapper a function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the Sheet, and the DisposableObjArray represents the row itself.
     * @return a Stream of Pair objects, where each Pair consists of a row key and a mapped value obtained by applying the {@code rowMapper} function to the row's values, ordered by rows.
     * @see #rows()
     * @see #rows(int, int, IntObjFunction)
     * @see #columns(IntObjFunction)
     */
    public <T> Stream<Pair<R, T>> rows(final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) {
        return rows(0, rowCount(), rowMapper);
    }

    /**
     * Returns a stream of key-value pairs for a range of rows in the Sheet, where each pair consists of a row key and a mapped value.
     * <p>
     * Each pair in the stream represents a row in the specified range [fromRowIndex, toRowIndex), with the row key and a value obtained by applying the provided {@code rowMapper} function to the row's values.
     * The pairs are ordered by rows.
     * The mapper is invoked with an empty array for every selected row when the Sheet has no columns.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}, {5, 6}}
     * );
     *
     * // Map rows 0 and 1 (excluding row 2) to a joined string of their values
     * sheet.rows(0, 2, (rowIndex, row) -> row.join("-"))
     *      .forEach(pair -> System.out.println(pair.left() + ": " + pair.right()));
     * // Prints:
     * // row1: 1-2
     * // row2: 3-4
     * }</pre>
     *
     * <p>Note: the {@code DisposableObjArray} passed to {@code rowMapper} is reused across
     * invocations and must not be retained beyond the scope of the mapper call.</p>
     *
     * @param <T> the type of the mapped value for each row.
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @param rowMapper a function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                  The integer represents the index of the row in the Sheet, and the DisposableObjArray represents the row itself.
     * @return a Stream of Pair objects for the specified row range, where each Pair consists of a row key and a mapped value obtained by applying the {@code rowMapper} function to the row's values, ordered by rows.
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromRowIndex &gt; toRowIndex
     * @see #rows(IntObjFunction)
     * @see #columns(int, int, IntObjFunction)
     */
    public <T> Stream<Pair<R, T>> rows(final int fromRowIndex, final int toRowIndex, final IntObjFunction<? super DisposableObjArray, ? extends T> rowMapper) {
        checkRowFromToIndex(fromRowIndex, toRowIndex, rowCount());

        // Invoke the mapper once per selected row even when the row array is empty.
        if (rowCount() == 0) {
            return Stream.empty();
        }

        initIndexMap();

        return Stream.of(new ObjIteratorEx<>() {
            private final int columnLength = columnCount();
            private final Object[] rowData = new Object[columnLength];
            private final DisposableObjArray rowArray = DisposableObjArray.wrap(rowData);
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Pair<R, T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final R rowKey = _rowKeyIndexMap.getByValue(cursor);

                if (_isInitialized) {
                    for (int i = 0; i < columnLength; i++) {
                        rowData[i] = _columnList.get(i).get(cursor);
                    }
                }

                return Pair.of(rowKey, rowMapper.apply(cursor++, rowArray));
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of key-value pairs representing all columns in the Sheet.
     * <p>
     * Each pair contains a column key and a stream of all values in that column (ordered by rows).
     * This provides a convenient way to process columns with their identifiers.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.columns().forEach(pair -> {
     *     String colKey = pair.left();
     *     Stream<Integer> values = pair.right();
     *     System.out.println(colKey + ": " + values.toList());
     * });
     * // Prints: col1: [1, 3]  col2: [2, 4]
     * }</pre>
     *
     * @return a Stream of Pair objects where each pair contains a column key and its value stream
     * @see #columns(int, int)
     * @see #rows()
     */
    public Stream<Pair<C, Stream<V>>> columns() {
        return columns(0, columnCount());
    }

    /**
     * Returns a stream of key-value pairs for a range of columns in the Sheet.
     * <p>
     * Each pair contains a column key and a stream of all values in that column (ordered by rows).
     * Only columns in the specified range [fromColumnIndex, toColumnIndex) are included. Includes {@code null} values.
     * A column with no rows is represented by its key and an empty value stream.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * // Process only first two columns
     * sheet.columns(0, 2).forEach(pair -> {
     *     String colKey = pair.left();
     *     List<Integer> values = pair.right().toList();
     *     System.out.println(colKey + ": " + values);
     * });
     * // Prints: col1: [1, 4]  col2: [2, 5]
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of Pair objects for the specified column range
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex &gt; toColumnIndex
     * @see #columns()
     * @see #rows(int, int)
     */
    public Stream<Pair<C, Stream<V>>> columns(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnCount());

        // Columns remain addressable even when each column contains zero values.
        if (columnCount() == 0) {
            return Stream.empty();
        }

        initIndexMap();

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toColumnIndex;
            private int cursor = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Pair<C, Stream<V>> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final C columnKey = _columnKeyIndexMap.getByValue(cursor);

                if (_isInitialized) {
                    return Pair.of(columnKey, Stream.of(_columnList.get(cursor++)));
                } else {
                    cursor++;
                    return Pair.of(columnKey, Stream.repeat(null, rowCount()));
                }
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Returns a stream of key-value pairs representing all columns in the Sheet, where each pair consists of a column key and a mapped value.
     * <p>
     * Each pair in the stream represents a column in the Sheet, with the column key and a value obtained by applying the provided {@code columnMapper} function to the column's values.
     * The pairs are ordered by columns.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * // Map each column to a joined string of its values
     * sheet.columns((colIndex, col) -> col.join("-"))
     *      .forEach(pair -> System.out.println(pair.left() + ": " + pair.right()));
     * // Prints:
     * // col1: 1-3
     * // col2: 2-4
     * }</pre>
     *
     * <p>Note: the {@code DisposableObjArray} passed to {@code columnMapper} is reused across
     * invocations and must not be retained beyond the scope of the mapper call.</p>
     *
     * @param <T> the type of the mapped value for each column.
     * @param columnMapper a function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                     The integer represents the index of the column in the Sheet, and the DisposableObjArray represents the column itself.
     * @return a Stream of Pair objects, where each Pair consists of a column key and a mapped value obtained by applying the {@code columnMapper} function to the column's values, ordered by columns.
     * @see #columns()
     * @see #columns(int, int, IntObjFunction)
     * @see #rows(IntObjFunction)
     */
    public <T> Stream<Pair<C, T>> columns(final IntObjFunction<? super DisposableObjArray, ? extends T> columnMapper) {
        return columns(0, columnCount(), columnMapper);
    }

    /**
     * Returns a stream of key-value pairs for a range of columns in the Sheet, where each pair consists of a column key and a mapped value.
     * <p>
     * Each pair in the stream represents a column in the specified range [fromColumnIndex, toColumnIndex), with the column key and a value obtained by applying the provided {@code columnMapper} function to the column's values.
     * The pairs are ordered by columns.
     * The mapper is invoked with an empty array for every selected column when the Sheet has no rows.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2", "row3"),
     *     List.of("col1", "col2", "col3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}
     * );
     *
     * // Map columns 0 and 1 (excluding column 2) to a joined string of their values
     * sheet.columns(0, 2, (colIndex, col) -> col.join("-"))
     *      .forEach(pair -> System.out.println(pair.left() + ": " + pair.right()));
     * // Prints:
     * // col1: 1-4-7
     * // col2: 2-5-8
     * }</pre>
     *
     * <p>Note: the {@code DisposableObjArray} passed to {@code columnMapper} is reused across
     * invocations and must not be retained beyond the scope of the mapper call.</p>
     *
     * @param <T> the type of the mapped value for each column.
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @param columnMapper a function that takes an integer and a DisposableObjArray as input and produces an object of type T.
     *                     The integer represents the index of the column in the Sheet, and the DisposableObjArray represents the column itself.
     * @return a Stream of Pair objects for the specified column range, where each Pair consists of a column key and a mapped value obtained by applying the {@code columnMapper} function to the column's values, ordered by columns.
     * @throws IndexOutOfBoundsException if indices are out of bounds or fromColumnIndex &gt; toColumnIndex
     * @see #columns(IntObjFunction)
     * @see #rows(int, int, IntObjFunction)
     */
    public <T> Stream<Pair<C, T>> columns(final int fromColumnIndex, final int toColumnIndex,
            final IntObjFunction<? super DisposableObjArray, ? extends T> columnMapper) {
        checkColumnFromToIndex(fromColumnIndex, toColumnIndex, columnCount());

        // Invoke the mapper once per selected column even when the column array is empty.
        if (columnCount() == 0) {
            return Stream.empty();
        }

        initIndexMap();

        return Stream.of(new ObjIteratorEx<>() {
            private final int rowLength = rowCount();
            private final Object[] columnData = new Object[rowLength];
            private final DisposableObjArray columnArray = DisposableObjArray.wrap(columnData);
            private final int toIndex = toColumnIndex;
            private int cursor = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Pair<C, T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final C columnKey = _columnKeyIndexMap.getByValue(cursor);

                if (_isInitialized) {
                    _columnList.get(cursor).toArray(columnData);
                }

                return Pair.of(columnKey, columnMapper.apply(cursor++, columnArray));
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }
        });
    }

    /**
     * Converts the Sheet into a Dataset with row-based organization (horizontal layout).
     * <p>
     * Each row in the Sheet becomes a row in the Dataset, with the column keys serving as the column names.
     * The data preserves its row structure, making this ideal for row-oriented data processing. The Dataset's
     * columns are named using {@link N#toString(Object)} on the Sheet's column keys. The resulting names
     * must be non-empty and unique. Because Dataset derives its row count from its columns, a Sheet with
     * rows but no columns converts to an empty Dataset and cannot preserve those row keys.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * Dataset dataset = sheet.toDataset();
     * // Dataset has 2 rows and 2 columns (col1, col2)
     * // Row 0: [1, 2]
     * // Row 1: [3, 4]
     * }</pre>
     *
     * @return a Dataset object with rows corresponding to Sheet rows and columns named by Sheet column keys
     * @throws IllegalArgumentException if converted column-key names are empty or duplicated
     * @see #toTransposedDataset()
     * @see #toArray()
     */
    public Dataset toDataset() {
        final int rowLength = rowCount();
        final int columnLength = columnCount();
        final List<String> datasetColumnNameList = new ArrayList<>(columnLength);

        for (final C columnKey : _columnKeySet) {
            datasetColumnNameList.add(N.toString(columnKey));
        }

        final List<List<Object>> datasetColumnList = new ArrayList<>(columnLength);

        if (_isInitialized) {
            for (final List<V> column : _columnList) {
                datasetColumnList.add(new ArrayList<>(column));
            }
        } else {
            for (int i = 0; i < columnLength; i++) {
                final List<Object> column = new ArrayList<>(rowLength);
                N.fill(column, 0, rowLength, null);
                datasetColumnList.add(column);
            }
        }

        return new RowDataset(datasetColumnNameList, datasetColumnList);
    }

    /**
     * Converts the Sheet into a Dataset with column-based organization (vertical/transposed layout).
     * <p>
     * Each column in the Sheet becomes a row in the Dataset, with the row keys serving as the column names.
     * This effectively transposes the data, making Sheet columns into Dataset rows. The Dataset's
     * columns are named using {@link N#toString(Object)} on the Sheet's row keys. The resulting names
     * must be non-empty and unique. Because Dataset derives its row count from its columns, a Sheet with
     * columns but no rows converts to an empty Dataset and cannot preserve those column keys as Dataset rows.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * Dataset dataset = sheet.toTransposedDataset();
     * // Dataset has 2 rows and 2 columns (row1, row2)
     * // Row 0: [1, 3] (col1 transposed)
     * // Row 1: [2, 4] (col2 transposed)
     * }</pre>
     *
     * @return a Dataset object with rows corresponding to Sheet columns and columns named by Sheet row keys (transposed)
     * @throws IllegalArgumentException if converted row-key names are empty or duplicated
     * @see #toDataset()
     * @see #toTransposedArray()
     */
    public Dataset toTransposedDataset() {
        final int rowLength = rowCount();
        final int columnLength = columnCount();
        final List<String> datasetColumnNameList = new ArrayList<>(rowLength);

        for (final R rowKey : _rowKeySet) {
            datasetColumnNameList.add(N.toString(rowKey));
        }

        final List<List<Object>> datasetColumnList = new ArrayList<>(rowLength);

        if (_isInitialized) {
            for (int i = 0; i < rowLength; i++) {
                final List<Object> column = new ArrayList<>(columnLength);

                for (int j = 0; j < columnLength; j++) {
                    column.add(_columnList.get(j).get(i));
                }

                datasetColumnList.add(column);
            }
        } else {
            for (int i = 0; i < rowLength; i++) {
                final List<Object> column = new ArrayList<>(columnLength);
                N.fill(column, 0, columnLength, null);
                datasetColumnList.add(column);
            }
        }

        return new RowDataset(datasetColumnNameList, datasetColumnList);
    }

    /**
     * Converts the Sheet into a two-dimensional array with row-major ordering.
     * <p>
     * Returns an {@code Object[][]} where {@code array[i][j]} corresponds to the value at
     * row {@code i} and column {@code j} in the Sheet. The array dimensions match the Sheet dimensions.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * Object[][] array = sheet.toArray();
     * // array[0] = [1, 2] (row1)
     * // array[1] = [3, 4] (row2)
     * }</pre>
     *
     * @return a two-dimensional Object array with row-major ordering
     * @see #toArray(Class)
     * @see #toTransposedArray()
     */
    public Object[][] toArray() {
        final int rowLength = rowCount();
        final int columnLength = columnCount();
        final Object[][] copy = new Object[rowLength][columnLength];

        if (_isInitialized) {
            for (int i = 0; i < columnLength; i++) {
                final List<V> column = _columnList.get(i);

                for (int j = 0; j < rowLength; j++) {
                    copy[j][i] = column.get(j);
                }
            }
        }

        return copy;
    }

    /**
     * Converts the Sheet into a typed two-dimensional array with row-major ordering.
     * <p>
     * Returns a {@code T[][]} where {@code array[i][j]} corresponds to the value at
     * row {@code i} and column {@code j} in the Sheet. Values are cast to the specified type.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * Integer[][] array = sheet.toArray(Integer.class);
     * // array[0] = [1, 2] (row1)
     * // array[1] = [3, 4] (row2)
     * }</pre>
     *
     * @param <T> the type of the elements in the array
     * @param componentType the Class object representing the element type
     * @return a two-dimensional typed array with row-major ordering
     * @throws ArrayStoreException if any value is not assignable to the specified component type
     * @see #toArray()
     * @see #toTransposedArray(Class)
     */
    public <T> T[][] toArray(final Class<T> componentType) {
        final int rowLength = rowCount();
        final int columnLength = columnCount();
        final T[][] copy = N.newArray(N.newArray(componentType, 0).getClass(), rowLength);

        for (int i = 0; i < rowLength; i++) {
            copy[i] = N.newArray(componentType, columnLength);
        }

        if (_isInitialized) {
            for (int i = 0; i < columnLength; i++) {
                final List<V> column = _columnList.get(i);

                for (int j = 0; j < rowLength; j++) {
                    copy[j][i] = (T) column.get(j);
                }
            }
        }

        return copy;
    }

    /**
     * Converts the Sheet into a two-dimensional array with column-major ordering.
     * <p>
     * Returns an {@code Object[][]} where {@code array[i][j]} corresponds to the value at
     * column {@code i} and row {@code j} in the Sheet. This transposes the data compared to {@link #toArray()}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * Object[][] array = sheet.toTransposedArray();
     * // array[0] = [1, 3] (col1)
     * // array[1] = [2, 4] (col2)
     * }</pre>
     *
     * @return a two-dimensional Object array with column-major ordering
     * @see #toTransposedArray(Class)
     * @see #toArray()
     */
    public Object[][] toTransposedArray() {
        final int rowLength = rowCount();
        final int columnLength = columnCount();
        final Object[][] copy = new Object[columnLength][rowLength];

        if (_isInitialized) {
            for (int i = 0; i < columnLength; i++) {
                _columnList.get(i).toArray(copy[i]);
            }
        }

        return copy;
    }

    /**
     * Converts the Sheet into a typed two-dimensional array with column-major ordering.
     * <p>
     * Returns a {@code T[][]} where {@code array[i][j]} corresponds to the value at
     * column {@code i} and row {@code j} in the Sheet. Values are cast to the specified type.
     * This transposes the data compared to {@link #toArray(Class)}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * Integer[][] array = sheet.toTransposedArray(Integer.class);
     * // array[0] = [1, 3] (col1)
     * // array[1] = [2, 4] (col2)
     * }</pre>
     *
     * @param <T> the type of the elements in the array
     * @param componentType the Class object representing the element type
     * @return a two-dimensional typed array with column-major ordering
     * @throws ArrayStoreException if any value is not assignable to the specified component type
     * @see #toTransposedArray()
     * @see #toArray(Class)
     */
    public <T> T[][] toTransposedArray(final Class<T> componentType) {
        final int rowLength = rowCount();
        final int columnLength = columnCount();
        final T[][] copy = N.newArray(N.newArray(componentType, 0).getClass(), columnLength);

        for (int i = 0; i < columnLength; i++) {
            copy[i] = N.newArray(componentType, rowLength);
        }

        if (_isInitialized) {
            for (int i = 0; i < columnLength; i++) {
                _columnList.get(i).toArray(copy[i]);
            }
        }

        return copy;
    }

    /**
     * Applies a transformation function to this Sheet and returns the result.
     * <p>
     * This method enables functional-style operations on the Sheet by applying
     * a function that takes the Sheet as input and produces any desired result.
     * Useful for custom processing, aggregations, or transformations.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * // Calculate sum of all values
     * Integer sum = sheet.apply(s ->
     *     s.rowMajorStream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum());
     * }</pre>
     *
     * @param <T> the type of the result produced by the function
     * @param <E> the type of exception the function may throw
     * @param func the function to apply to this Sheet
     * @return the result produced by applying the function to this Sheet
     * @throws E if the function throws an exception
     * @see #applyIfNotEmpty(Throwables.Function)
     */
    public <T, E extends Exception> T apply(final Throwables.Function<? super Sheet<R, C, V>, T, E> func) throws E {
        return func.apply(this);
    }

    /**
     * Applies a transformation function to this Sheet if it's not empty, returning an Optional result.
     * <p>
     * This method provides conditional functional-style operations by applying the function only when
     * the Sheet has at least one row and one column. Cell values may all still be {@code null}.
     * Returns an empty Optional if either axis is empty.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1"),
     *     new Integer[][] {{42}}
     * );
     *
     * Optional<Integer> result = sheet.applyIfNotEmpty(s ->
     *     s.rowMajorStream().mapToInt(Integer::intValue).max().orElse(0));
     * // result.isPresent() = true, result.get() = 42
     *
     * Sheet<String, String, Integer> empty = new Sheet<>();
     * Optional<Integer> emptyResult = empty.applyIfNotEmpty(s -> 100);
     * // emptyResult.isEmpty() = true
     * }</pre>
     *
     * @param <T> the type of the result produced by the function
     * @param <E> the type of exception the function may throw
     * @param func the function to apply if this Sheet has at least one row and one column
     * @return an Optional containing the result if both axes are non-empty, or an empty Optional otherwise
     * @throws E if the function throws an exception
     * @see #apply(Throwables.Function)
     * @see #isEmpty()
     */
    public <T, E extends Exception> Optional<T> applyIfNotEmpty(final Throwables.Function<? super Sheet<R, C, V>, T, E> func) throws E {
        if (!isEmpty()) {
            return Optional.ofNullable(func.apply(this));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Executes an action on this Sheet without returning a value.
     * <p>
     * This method enables functional-style side effects on the Sheet, such as logging,
     * validation, or other operations that don't produce a return value.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * // Log sheet statistics
     * sheet.accept(s -> {
     *     System.out.println("Rows: " + s.rowCount());
     *     System.out.println("Columns: " + s.columnCount());
     *     System.out.println("Non-null values: " + s.nonNullValueCount());
     * });
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on this Sheet
     * @throws E if the action throws an exception
     * @see #acceptIfNotEmpty(Throwables.Consumer)
     */
    public <E extends Exception> void accept(final Throwables.Consumer<? super Sheet<R, C, V>, E> action) throws E {
        action.accept(this);
    }

    /**
     * Executes an action on this Sheet if it's not empty, returning whether the action was performed.
     * <p>
     * This method provides conditional execution based on whether the Sheet has at least one row and one
     * column; cell values may all still be {@code null}. Returns {@link OrElse#TRUE} if the action was executed,
     * or {@link OrElse#FALSE} if either axis was empty.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1"),
     *     new Integer[][] {{42}}
     * );
     *
     * OrElse result = sheet.acceptIfNotEmpty(s -> {
     *     System.out.println("Processing non-empty sheet...");
     * });
     * // result == OrElse.TRUE
     *
     * Sheet<String, String, Integer> empty = new Sheet<>();
     * OrElse emptyResult = empty.acceptIfNotEmpty(s -> {
     *     System.out.println("This won't be printed");
     * });
     * // emptyResult == OrElse.FALSE
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform if this Sheet has at least one row and one column
     * @return OrElse.TRUE if the action was executed, or OrElse.FALSE if either axis was empty
     * @throws E if the action throws an exception
     * @see #accept(Throwables.Consumer)
     * @see #isEmpty()
     */
    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super Sheet<R, C, V>, E> action) throws E {
        if (!isEmpty()) {
            action.accept(this);

            return OrElse.TRUE;
        }

        return OrElse.FALSE;
    }

    /**
     * Prints the entire Sheet to standard output in a formatted table.
     * <p>
     * Outputs a nicely formatted ASCII table showing all rows and columns with their keys.
     * The table includes borders and proper alignment for readability.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("R1", "R2"),
     *     List.of("C1", "C2", "C3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}}
     * );
     *
     * sheet.println();
     * // Output:
     * //      +----+----+----+
     * //      | C1 | C2 | C3 |
     * // +----+----+----+----+
     * // | R1 | 1  | 2  | 3  |
     * // | R2 | 4  | 5  | 6  |
     * // +----+----+----+----+
     * }</pre>
     *
     * @throws UncheckedIOException if an I/O error occurs while printing
     * @see #println(String)
     * @see #println(Appendable)
     */
    public void println() throws UncheckedIOException {
        println(_rowKeySet, _columnKeySet);
    }

    /**
     * Prints the entire Sheet to standard output with the specified prefix on each line.
     * <p>
     * Outputs a formatted ASCII table to {@link System#out} with the specified prefix prepended
     * to every line. Useful for logging or when integrating Sheet output into larger formatted output.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("R1", "R2"),
     *     List.of("C1", "C2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * sheet.println("## ");
     * // Output:
     * // ##      +----+----+
     * // ##      | C1 | C2 |
     * // ## +----+----+----+
     * // ## | R1 | 1  | 2  |
     * // ## | R2 | 3  | 4  |
     * // ## +----+----+----+
     * }</pre>
     *
     * @param prefix the string to prepend to each line of output
     * @throws UncheckedIOException if an I/O error occurs while printing
     * @see #println()
     * @see #println(Collection, Collection, String, Appendable)
     */
    public void println(final String prefix) throws UncheckedIOException {
        println(_rowKeySet, _columnKeySet, prefix, System.out); // NOSONAR);
    }

    /**
     * Prints a subset of the Sheet to standard output showing only specified rows and columns.
     * <p>
     * Outputs a formatted ASCII table containing only the rows and columns specified by the key sets.
     * This allows for focused printing of relevant data portions.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("R1", "R2", "R3"),
     *     List.of("C1", "C2", "C3"),
     *     new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}
     * );
     *
     * // Print only specific rows and columns
     * sheet.println(List.of("R1", "R3"), List.of("C1", "C3"));
     * // Shows only intersection of R1,R3 with C1,C3
     * }</pre>
     *
     * @param rowKeySet the row keys to include in the output
     * @param columnKeySet the column keys to include in the output
     * @throws UncheckedIOException if an I/O error occurs while printing
     * @throws IllegalArgumentException if any specified row or column keys do not exist in this Sheet
     * @see #println()
     * @see #println(Collection, Collection, Appendable)
     */
    public void println(final Collection<R> rowKeySet, final Collection<C> columnKeySet) throws UncheckedIOException {
        println(rowKeySet, columnKeySet, System.out); // NOSONAR
    }

    /**
     * Prints the entire Sheet to the specified output destination.
     * <p>
     * Outputs a formatted ASCII table to any Appendable (Writer, StringBuilder, etc.).
     * Useful for capturing Sheet output to files, strings, or other destinations.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("R1", "R2"),
     *     List.of("C1", "C2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * // Print to a file
     * try (FileWriter fileWriter = new FileWriter("sheet.txt")) {
     *     sheet.println(fileWriter);
     * }
     *
     * // Print to a StringBuilder
     * StringBuilder sb = new StringBuilder();
     * sheet.println(sb);
     * String result = sb.toString();
     * }</pre>
     *
     * @param output the destination for the formatted output
     * @throws UncheckedIOException if an I/O error occurs while writing
     * @see #println()
     * @see #println(Collection, Collection, Appendable)
     */
    public void println(final Appendable output) throws UncheckedIOException {
        println(_rowKeySet, _columnKeySet, output);
    }

    /**
     * Prints a subset of the Sheet to the specified Appendable output.
     * <p>
     * Outputs a formatted ASCII table containing only the rows and columns specified by the key sets
     * to any Appendable (Writer, StringBuilder, etc.).
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *         N.asList("r1", "r2"), N.asList("c1", "c2"),
     *         new Integer[][] {{1, 2}, {3, 4}});
     * StringBuilder sb = new StringBuilder();
     * sheet.println(N.asList("r1"), N.asList("c1"), sb);   // writes only the r1/c1 cell
     * // sb now contains:
     * //      +----+
     * //      | c1 |
     * // +----+----+
     * // | r1 | 1  |
     * // +----+----+
     *
     * sheet.println(N.asList("rX"), N.asList("c1"), sb);   // throws IllegalArgumentException (unknown row key)
     * }</pre>
     *
     * @param rowKeySet the collection of row keys to include in the output
     * @param columnKeySet the collection of column keys to include in the output
     * @param output the destination for the formatted output
     * @throws IllegalArgumentException if any specified row or column keys do not exist in this Sheet
     * @throws UncheckedIOException if an I/O error occurs while printing
     * @see #println()
     * @see #println(Appendable)
     * @see #println(Collection, Collection, String, Appendable)
     */
    public void println(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final Appendable output)
            throws IllegalArgumentException, UncheckedIOException {
        println(rowKeySet, columnKeySet, null, output);
    }

    /**
     * Prints a subset of the Sheet to the specified Appendable output with a prefix on each line.
     * <p>
     * Outputs a formatted ASCII table containing only the rows and columns specified by the key sets
     * with the specified prefix prepended to every line.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *         N.asList("r1", "r2"), N.asList("c1", "c2"),
     *         new Integer[][] {{1, 2}, {3, 4}});
     * StringBuilder sb = new StringBuilder();
     * sheet.println(N.asList("r1", "r2"), N.asList("c1"), "# ", sb);   // prefixes every line with "# "
     * // sb now contains:
     * // #      +----+
     * // #      | c1 |
     * // # +----+----+
     * // # | r1 | 1  |
     * // # | r2 | 3  |
     * // # +----+----+
     *
     * sheet.println(N.asList("r1"), N.asList("cX"), "# ", sb);   // throws IllegalArgumentException (unknown column key)
     * }</pre>
     *
     * @param rowKeySet the collection of row keys to include in the output
     * @param columnKeySet the collection of column keys to include in the output
     * @param prefix the string to prepend to each line of output
     * @param output the destination for the formatted output
     * @throws IllegalArgumentException if any specified row or column keys do not exist in this Sheet
     * @throws UncheckedIOException if an I/O error occurs while printing
     * @see #println()
     * @see #println(String)
     * @see #println(Collection, Collection, Appendable)
     */
    public void println(final Collection<R> rowKeySet, final Collection<C> columnKeySet, final String prefix, final Appendable output)
            throws IllegalArgumentException, UncheckedIOException {
        // Normalize null to empty: the body's guards treat null as empty everywhere except the
        // size() calls and iteration loops, which would NPE when exactly one side is null.
        if (rowKeySet == null || columnKeySet == null) {
            println(rowKeySet == null ? N.emptyList() : rowKeySet, columnKeySet == null ? N.emptyList() : columnKeySet, prefix, output);
            return;
        }

        if (N.notEmpty(rowKeySet) && !_rowKeySet.containsAll(rowKeySet)) {
            throw new IllegalArgumentException("Row keys: " + N.difference(rowKeySet, _rowKeySet) + " are not included in this sheet row keys: " + _rowKeySet);
        }

        if (N.notEmpty(columnKeySet) && !_columnKeySet.containsAll(columnKeySet)) {
            throw new IllegalArgumentException(
                    "Column keys: " + N.difference(columnKeySet, _columnKeySet) + " are not included in this sheet Column keys: " + _columnKeySet);
        }

        N.checkArgNotNull(output, cs.output);

        final boolean isBufferedWriter = output instanceof Writer writer && IOUtil.isBufferedWriter(writer);
        final Writer bw = isBufferedWriter ? (Writer) output : (output instanceof Writer writer ? Objectory.createBufferedWriter((writer)) : null);
        final Appendable appendable = bw != null ? bw : output;
        final String lineSeparator = Strings.isEmpty(prefix) ? IOUtil.LINE_SEPARATOR_UNIX : (IOUtil.LINE_SEPARATOR_UNIX + prefix);

        try {
            if (N.notEmpty(prefix)) {
                appendable.append(prefix);
            }

            if (N.isEmpty(rowKeySet) && N.isEmpty(columnKeySet)) {
                appendable.append("+---+");
                appendable.append(lineSeparator);
                appendable.append("|   |");
                appendable.append(lineSeparator);
                appendable.append("+---+");
            } else {
                final int rowLen = rowKeySet.size();
                final int columnLen = N.max(2, columnKeySet.size() + 1);

                final int[] rowIndices = new int[rowLen];
                int idx = 0;

                for (final R rowKey : rowKeySet) {
                    rowIndices[idx++] = getRowIndex(rowKey);
                }

                final int[] columnIndices = new int[columnLen];
                idx = 0;
                columnIndices[idx++] = -1; // rowKey Column

                if (N.isEmpty(columnKeySet)) {
                    columnIndices[idx] = -1;
                } else {
                    for (final C columnKey : columnKeySet) {
                        columnIndices[idx++] = getColumnIndex(columnKey);
                    }
                }

                final List<String> columnNameList = new ArrayList<>(columnLen);
                columnNameList.add(" "); // add for row key column

                if (N.isEmpty(columnKeySet)) {
                    columnNameList.add(" "); // add for row key column
                } else {
                    for (final C ck : columnKeySet) {
                        columnNameList.add(N.toString(ck));
                    }
                }

                final List<List<String>> strColumnList = new ArrayList<>(columnLen);
                final int[] maxColumnLens = new int[columnLen];

                for (int i = 0; i < columnLen; i++) {
                    final List<String> strColumn = new ArrayList<>(rowLen);
                    int maxLen = Strings.displayWidth(columnNameList.get(i));
                    String str = null;

                    if (i == 0) {
                        for (final R rk : rowKeySet) {
                            str = N.toString(rk);
                            maxLen = N.max(maxLen, Strings.displayWidth(str));
                            strColumn.add(str);
                        }
                    } else if (columnIndices[i] < 0) {
                        maxLen = N.max(maxLen, 1);
                        N.fill(strColumn, 0, rowLen, " ");
                    } else if (!_isInitialized) {
                        maxLen = N.max(maxLen, 4);
                        N.fill(strColumn, 0, rowLen, "null");
                    } else {
                        for (final int rowIndex : rowIndices) {
                            str = N.toString(_columnList.get(columnIndices[i]).get(rowIndex));
                            maxLen = N.max(maxLen, Strings.displayWidth(str));
                            strColumn.add(str);
                        }
                    }

                    maxColumnLens[i] = maxLen;
                    strColumnList.add(strColumn);
                }

                final char hch = '-';
                final char hchDelta = 2;
                for (int i = 0; i < columnLen; i++) {
                    if (i == 0) {
                        appendable.append(Strings.repeat(' ', maxColumnLens[i] + hchDelta + 1));
                    } else {
                        appendable.append('+');

                        appendable.append(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                    }
                }

                appendable.append('+');
                appendable.append(lineSeparator);

                for (int i = 0; i < columnLen; i++) {
                    if (i == 0) {
                        appendable.append("  ");
                    } else {
                        appendable.append(" | ");
                    }

                    appendable.append(Strings.padEndByDisplayWidth(columnNameList.get(i), maxColumnLens[i]));
                }

                appendable.append(" |");
                appendable.append(lineSeparator);

                for (int i = 0; i < columnLen; i++) {
                    appendable.append('+');

                    if (i == 1 && N.isEmpty(columnKeySet)) {
                        appendable.append(Strings.repeat(' ', maxColumnLens[i] + hchDelta));
                    } else {
                        appendable.append(Strings.repeat(hch, maxColumnLens[i] + hchDelta));
                    }
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

                        appendable.append(Strings.padEndByDisplayWidth(strColumnList.get(i).get(j), maxColumnLens[i]));
                    }

                    appendable.append(" |");
                }

                if (rowLen == 0) {
                    appendable.append(lineSeparator);

                    for (int i = 0; i < columnLen; i++) {
                        if (i == 0) {
                            appendable.append("| ");
                            appendable.append(Strings.padEnd("", maxColumnLens[i]));
                        } else {
                            appendable.append(Strings.padEnd("", maxColumnLens[i] + 3));
                        }
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
     * Returns a hash code value for this Sheet.
     * <p>
     * The hash code is computed based on the row keys, column keys, and values.
     * Two Sheets that are equal according to {@link #equals(Object)} will have the same hash code.
     * </p>
     *
     * @return a hash code value for this Sheet
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + orderedKeyHashCode(_rowKeySet);
        result = prime * result + orderedKeyHashCode(_columnKeySet);
        // Note: when uninitialized, all cells are conceptually null. An initialized but all-null
        // _columnList must hash the same as an uninitialized sheet to keep equals/hashCode consistent.
        if (_isInitialized && hasAnyNonNullValue()) {
            result = prime * result + _columnList.hashCode();
        } else {
            result = prime * result;
        }
        return result;
    }

    private static int orderedKeyHashCode(final Collection<?> keys) {
        return keys == null ? 0 : new ArrayList<>(keys).hashCode();
    }

    private static boolean orderedKeysEqual(final Collection<?> keys1, final Collection<?> keys2) {
        return N.equals(keys1 == null ? null : new ArrayList<>(keys1), keys2 == null ? null : new ArrayList<>(keys2));
    }

    private boolean hasAnyNonNullValue() {
        if (!_isInitialized) {
            return false;
        }
        for (final List<V> column : _columnList) {
            for (final V e : column) {
                if (e != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Indicates whether some other object is "equal to" this Sheet.
     * <p>
     * Two Sheets are considered equal if they have the same ordered row keys, ordered column keys, and
     * identical values at corresponding positions. Values are compared with {@code equals} position by position.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet1 = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1"),
     *     new Integer[][] {{42}}
     * );
     *
     * Sheet<String, String, Integer> sheet2 = Sheet.rows(
     *     List.of("row1"),
     *     List.of("col1"),
     *     new Integer[][] {{42}}
     * );
     *
     * boolean isEqual = sheet1.equals(sheet2);   // returns true
     * }</pre>
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this Sheet is equal to the obj argument; {@code false} otherwise
     * @see #hashCode()
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof Sheet)) {
            return false;
        }

        final Sheet<R, C, V> other = (Sheet<R, C, V>) obj;

        if (!orderedKeysEqual(_rowKeySet, other._rowKeySet) || !orderedKeysEqual(_columnKeySet, other._columnKeySet)) {
            return false;
        }

        // Treat an uninitialized sheet as equivalent to an initialized one with all-null cells.
        if (_isInitialized && other._isInitialized) {
            return N.equals(other._columnList, _columnList);
        }

        if (!_isInitialized && !other._isInitialized) {
            return true;
        }

        // Exactly one is initialized: equal only if the initialized side has no non-null values.
        return _isInitialized ? !hasAnyNonNullValue() : !other.hasAnyNonNullValue();
    }

    /**
     * Returns a string representation of this Sheet.
     * <p>
     * The string contains the row keys, column keys, and all column data in a structured format.
     * This is primarily useful for debugging and logging purposes.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Sheet<String, String, Integer> sheet = Sheet.rows(
     *     List.of("row1", "row2"),
     *     List.of("col1", "col2"),
     *     new Integer[][] {{1, 2}, {3, 4}}
     * );
     *
     * String repr = sheet.toString();
     * // Returns: {rowKeySet=[row1, row2], columnKeySet=[col1, col2], columns={col1=[1, 3], col2=[2, 4]}}
     * }</pre>
     *
     * @return a string representation of this Sheet
     * @see #println()
     */
    @Override
    public String toString() {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append("{rowKeySet=");
            sb.append(_rowKeySet);
            sb.append(", columnKeySet=");
            sb.append(_columnKeySet);
            sb.append(", columns={");

            if (_isInitialized) {
                final Iterator<C> iter = _columnKeySet.iterator();

                for (int i = 0, columnLength = columnCount(); i < columnLength; i++) {
                    if (i > 0) {
                        sb.append(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    sb.append(iter.next()).append("=").append(N.toString(_columnList.get(i)));
                }
            }

            sb.append("}}");

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    private void init() {
        // Always ensure the row/column index maps exist. They may have been reset to null
        // (e.g. by moveRow/moveColumn) while the sheet remains initialized; callers such as
        // addRow/addColumn rely on these maps right after invoking init().
        initIndexMap();

        if (!_isInitialized) {
            final int rowLength = rowCount();
            final int columnLength = columnCount();
            _columnList = new ArrayList<>(columnLength);

            for (int i = 0; i < columnLength; i++) {
                final List<V> column = new ArrayList<>(rowLength);
                N.fill(column, 0, rowLength, null);
                _columnList.add(column);
            }

            _isInitialized = true;
        }
    }

    private void initIndexMap() {
        if (_rowKeyIndexMap == null) {
            final int rowLength = rowCount();
            _rowKeyIndexMap = N.newBiMap(rowLength);
            int index = 0;
            for (final R rowKey : _rowKeySet) {
                _rowKeyIndexMap.put(rowKey, index++);
            }
        }

        if (_columnKeyIndexMap == null) {
            final int columnLength = columnCount();
            _columnKeyIndexMap = N.newBiMap(columnLength);
            int index = 0;
            for (final C columnKey : _columnKeySet) {
                _columnKeyIndexMap.put(columnKey, index++);
            }
        }
    }

    /**
     * Checks if the provided rowKey is a valid key for a row in the Sheet.
     * The rowKey is valid if it exists in the Sheet's row keys.
     *
     * @param rowKey the key of the row to be checked
     * @throws IllegalArgumentException if the rowKey does not exist in the Sheet
     */
    private void checkRowKey(final R rowKey) throws IllegalArgumentException {
        if (!_rowKeySet.contains(rowKey)) {
            throw new IllegalArgumentException("No row found by key: " + rowKey);
        }
    }

    /**
     * Checks if the provided columnKey is a valid key for a column in the Sheet.
     * The columnKey is valid if it exists in the Sheet's column keys.
     *
     * @param columnKey the key of the column to be checked
     * @throws IllegalArgumentException if the columnKey does not exist in the Sheet
     */
    private void checkColumnKey(final C columnKey) throws IllegalArgumentException {
        if (!_columnKeySet.contains(columnKey)) {
            throw new IllegalArgumentException("No column found by key: " + columnKey);
        }
    }

    /**
     * Checks if the provided rowIndex is a valid index for a row in the Sheet.
     * The rowIndex is valid if it is greater than or equal to 0 and less than the number of rows in the Sheet.
     *
     * @param rowIndex the index of the row to be checked
     * @throws IndexOutOfBoundsException if the rowIndex is not a valid index for a row in the Sheet
     */
    private void checkRowIndex(final int rowIndex) throws IndexOutOfBoundsException {
        if (rowIndex < 0 || rowIndex >= rowCount()) {
            throw new IndexOutOfBoundsException("Row index " + rowIndex + " is out-of-bounds for row size " + rowCount());
        }
    }

    /**
     * Checks if the provided fromRowIndex and toRowIndex are valid indices for rows in the Sheet.
     * The fromRowIndex and toRowIndex are valid if they are greater than or equal to 0,
     * fromRowIndex is less than or equal to toRowIndex, and toRowIndex is less than or equal to the specified length.
     * The range is half-open: {@code [fromRowIndex, toRowIndex)}.
     *
     * @param fromRowIndex the starting index of the row range to be checked
     * @param toRowIndex the ending index of the row range to be checked
     * @param len the total length of the row range
     * @throws IndexOutOfBoundsException if the fromRowIndex and toRowIndex are not valid indices for rows in the Sheet
     */
    private void checkRowFromToIndex(final int fromRowIndex, final int toRowIndex, final int len) throws IndexOutOfBoundsException {
        if (fromRowIndex < 0 || fromRowIndex > toRowIndex || toRowIndex > len) {
            throw new IndexOutOfBoundsException("Row index range [" + fromRowIndex + ", " + toRowIndex + ") is out-of-bounds for row size " + len);
        }
    }

    /**
     * Checks if the provided columnIndex is a valid index for a column in the Sheet.
     * The columnIndex is valid if it is greater than or equal to 0 and less than the number of columns in the Sheet.
     *
     * @param columnIndex the index of the column to be checked
     * @throws IndexOutOfBoundsException if the columnIndex is not a valid index for a column in the Sheet
     */
    private void checkColumnIndex(final int columnIndex) throws IndexOutOfBoundsException {
        if (columnIndex < 0 || columnIndex >= columnCount()) {
            throw new IndexOutOfBoundsException("Column index " + columnIndex + " is out-of-bounds for column size " + columnCount());
        }
    }

    /**
     * Checks if the provided fromColumnIndex and toColumnIndex are valid indices for columns in the Sheet.
     * The fromColumnIndex and toColumnIndex are valid if they are greater than or equal to 0,
     * fromColumnIndex is less than or equal to toColumnIndex, and toColumnIndex is less than or equal to the specified length.
     * The range is half-open: {@code [fromColumnIndex, toColumnIndex)}.
     *
     * @param fromColumnIndex the starting index of the column range to be checked
     * @param toColumnIndex the ending index of the column range to be checked
     * @param len the total length of the column range
     * @throws IndexOutOfBoundsException if the fromColumnIndex and toColumnIndex are not valid indices for columns in the Sheet
     */
    private void checkColumnFromToIndex(final int fromColumnIndex, final int toColumnIndex, final int len) throws IndexOutOfBoundsException {
        if (fromColumnIndex < 0 || fromColumnIndex > toColumnIndex || toColumnIndex > len) {
            throw new IndexOutOfBoundsException("Column index range [" + fromColumnIndex + ", " + toColumnIndex + ") is out-of-bounds for column size " + len);
        }
    }

    /**
     * Retrieves the index of a row in the Sheet.
     * The row is identified by the provided row key.
     *
     * @param rowKey the key of the row
     * @return the index of the row
     * @throws IllegalArgumentException if the row key does not exist in this Sheet
     */
    private int getRowIndex(final R rowKey) throws IllegalArgumentException {
        if (_rowKeyIndexMap == null) {
            this.initIndexMap();
        }

        final Integer index = _rowKeyIndexMap.get(rowKey);

        if (index == null) {
            throw new IllegalArgumentException("No row found by key: " + rowKey);
        }

        return index;
    }

    /**
     * Retrieves the index of a column in the Sheet.
     * The column is identified by the provided column key.
     *
     * @param columnKey the key of the column
     * @return the index of the column
     * @throws IllegalArgumentException if the column key does not exist in this Sheet
     */
    private int getColumnIndex(final C columnKey) throws IllegalArgumentException {
        if (_columnKeyIndexMap == null) {
            this.initIndexMap();
        }

        final Integer index = _columnKeyIndexMap.get(columnKey);

        if (index == null) {
            throw new IllegalArgumentException("No column found by key: " + columnKey);
        }

        return index;
    }

    /**
     * Checks if the Sheet is frozen.
     * If the Sheet is frozen, an IllegalStateException is thrown.
     * A Sheet is considered frozen if it has been marked as unmodifiable.
     *
     * @throws IllegalStateException if this Sheet is frozen
     */
    private void checkFrozen() throws IllegalStateException {
        if (_isFrozen) {
            throw new IllegalStateException("This Sheet is frozen and cannot be modified");
        }
    }

    /**
     * A record representing a cell in the Sheet.
     * A cell is identified by a row key of type {@code R}, a column key of type {@code C}, and contains a value of type {@code V}.
     *
     * @param <R> the type of the row keys
     * @param <C> the type of the column keys
     * @param <V> the type of the values stored in the cells
     * @param rowKey the key of the row
     * @param columnKey the key of the column
     * @param value the value stored in the cell
     */
    public record Cell<R, C, V>(R rowKey, C columnKey, V value) {

        /**
         * Creates a new Cell with the specified row key, column key, and value.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Sheet.Cell<String, String, Integer> cell = Sheet.Cell.of("r1", "c1", 42);
         * cell.rowKey();      // returns "r1"
         * cell.columnKey();   // returns "c1"
         * cell.value();       // returns 42
         * }</pre>
         *
         * @param <R> the type of the row key
         * @param <C> the type of the column key
         * @param <V> the type of the value
         * @param rowKey the key of the row
         * @param columnKey the key of the column
         * @param value the value stored in the cell
         * @return a new Cell with the specified row key, column key, and value
         */
        public static <R, C, V> Cell<R, C, V> of(final R rowKey, final C columnKey, final V value) {
            return new Cell<>(rowKey, columnKey, value);
        }
    }

    /**
     * A record representing a point in a two-dimensional space, such as a cell in a Sheet.
     * A point is identified by a rowIndex and a columnIndex.
     *
     * @param rowIndex the index of the row
     * @param columnIndex the index of the column
     */
    public record Point(int rowIndex, int columnIndex) {

        private static final int MAX_CACHE_SIZE = 128;
        private static final Point[][] CACHE = new Point[MAX_CACHE_SIZE][MAX_CACHE_SIZE];

        static {
            for (int i = 0; i < MAX_CACHE_SIZE; i++) {
                for (int j = 0; j < MAX_CACHE_SIZE; j++) {
                    CACHE[i][j] = new Point(i, j);
                }
            }
        }

        /** The point at the origin, with both {@code rowIndex} and {@code columnIndex} equal to {@code 0}. */
        public static final Point ZERO = CACHE[0][0];

        /**
         * Returns a Point with the specified row index and column index.
         * <p>
         * Points whose row and column indices are both within the small-value cache range are returned
         * from a shared cache; other values produce a newly created instance. Because {@code Point} is an
         * immutable record, callers should not rely on instance identity.
         * </p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Sheet.Point p = Sheet.Point.of(2, 3);
         * p.rowIndex();                     // returns 2
         * p.columnIndex();                  // returns 3
         * p.equals(Sheet.Point.of(2, 3));   // returns true (records compare by value)
         * }</pre>
         *
         * @param rowIndex the index of the row
         * @param columnIndex the index of the column
         * @return a Point with the specified row index and column index
         */
        public static Point of(final int rowIndex, final int columnIndex) {
            if (rowIndex >= 0 && rowIndex < MAX_CACHE_SIZE && columnIndex >= 0 && columnIndex < MAX_CACHE_SIZE) {
                return CACHE[rowIndex][columnIndex];
            }

            return new Point(rowIndex, columnIndex);
        }
    }
}
