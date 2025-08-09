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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.JSONParser;
import com.landawn.abacus.parser.JSONSerializationConfig;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

/**
 * The CSVUtil class provides comprehensive utilities for handling CSV (Comma-Separated Values) data.
 * It offers methods for loading CSV data from various sources (File, Reader) into DataSet objects,
 * as well as streaming CSV data into objects of specified types. The class supports custom parsing,
 * column selection, row filtering, and type conversion.
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li>Loading CSV data into DataSet objects with column selection and filtering</li>
 *   <li>Streaming CSV data with lazy evaluation for memory efficiency</li>
 *   <li>Custom CSV parsers for headers and lines</li>
 *   <li>Type conversion support through bean classes and type mappings</li>
 *   <li>Row extraction with custom logic</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Load CSV file into DataSet
 * DataSet ds = CSVUtil.loadCSV(new File("data.csv"));
 * 
 * // Stream CSV data as beans
 * Stream<Person> persons = CSVUtil.stream(new File("persons.csv"), Person.class);
 * }</pre>
 * 
 * @see com.landawn.abacus.util.CSVParser
 * @see com.landawn.abacus.util.DataSet
 */
public final class CSVUtil {
    private CSVUtil() {
        // Utility class
    }

    public static final JSONParser jsonParser = ParserFactory.createJSONParser();

    static final JSONDeserializationConfig jdc = JDC.create().setElementType(String.class);

    static final Splitter lineSplitter = Splitter.with(',').trimResults();

    static final CSVParser csvParser = new CSVParser();

    static final int BATCH_SIZE_FOR_FLUSH = 1000;

    /**
     * Default CSV header parser function that uses the internal CSVParser.
     * This parser handles quoted fields and escape characters according to CSV standards.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * String[] headers = CSV_HEADER_PARSER.apply("Name,Age,\"Address, City\"");
     * // Result: ["Name", "Age", "Address, City"]
     * }</pre>
     */
    public static final Function<String, String[]> CSV_HEADER_PARSER = csvParser::parseLineToArray;

    /**
     * Default CSV line parser that parses a line into an existing array.
     * This parser handles quoted fields and escape characters according to CSV standards.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * String[] row = new String[3];
     * CSV_LINE_PARSER.accept("John,30,\"New York, NY\"", row);
     * // row contains: ["John", "30", "New York, NY"]
     * }</pre>
     */
    public static final BiConsumer<String, String[]> CSV_LINE_PARSER = csvParser::parseLineToArray;

    /**
     * CSV header parser that uses a simple splitter approach.
     * This parser splits by comma and removes surrounding quotes from fields.
     * It's faster but less robust than the default parser for standard CSV files.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * String[] headers = CSV_HEADER_PARSER_BY_SPLITTER.apply("\"Name\",\"Age\",\"City\"");
     * // Result: ["Name", "Age", "City"]
     * }</pre>
     */
    public static final Function<String, String[]> CSV_HEADER_PARSER_BY_SPLITTER = it -> {
        final String[] strs = lineSplitter.splitToArray(it);
        int subStrLen = 0;

        for (int i = 0, len = strs.length; i < len; i++) {
            subStrLen = N.len(strs[i]);

            if (subStrLen > 1 && strs[i].charAt(0) == '"' && strs[i].charAt(subStrLen - 1) == '"') {
                strs[i] = strs[i].substring(1, subStrLen - 1);
            }
        }

        return strs;
    };

    /**
     * CSV line parser that uses a simple splitter approach.
     * This parser splits by comma and removes surrounding quotes from fields.
     * It's faster but less robust than the default parser for standard CSV files.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * String[] row = new String[3];
     * CSV_LINE_PARSER_BY_SPLITTER.accept("\"John\",\"30\",\"NYC\"", row);
     * // row contains: ["John", "30", "NYC"]
     * }</pre>
     */
    public static final BiConsumer<String, String[]> CSV_LINE_PARSER_BY_SPLITTER = (it, output) -> {
        lineSplitter.splitToArray(it, output);
        int subStrLen = 0;

        for (int i = 0, len = output.length; i < len; i++) {
            subStrLen = N.len(output[i]);

            if (subStrLen > 1 && output[i].charAt(0) == '"' && output[i].charAt(subStrLen - 1) == '"') {
                output[i] = output[i].substring(1, subStrLen - 1);
            }
        }
    };

    /**
     * CSV header parser that expects JSON array format.
     * This parser treats each line as a JSON array of strings.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * String[] headers = CSV_HEADER_PARSER_IN_JSON.apply("[\"Name\",\"Age\",\"City\"]");
     * // Result: ["Name", "Age", "City"]
     * }</pre>
     */
    public static final Function<String, String[]> CSV_HEADER_PARSER_IN_JSON = line -> jsonParser.readString(line, jdc, String[].class);

    /**
     * CSV line parser that expects JSON array format.
     * This parser treats each line as a JSON array of strings.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * String[] row = new String[3];
     * CSV_LINE_PARSER_IN_JSON.accept("[\"John\",\"30\",\"NYC\"]", row);
     * // row contains: ["John", "30", "NYC"]
     * }</pre>
     */
    public static final BiConsumer<String, String[]> CSV_LINE_PARSER_IN_JSON = (line, output) -> jsonParser.readString(line, jdc, output);

    private static final Function<String, String[]> defaultCsvHeaderParser = CSV_HEADER_PARSER;

    private static final BiConsumer<String, String[]> defaultCsvLineParser = CSV_LINE_PARSER;

    private static final ThreadLocal<Function<String, String[]>> csvHeaderParser_TL = ThreadLocal.withInitial(() -> defaultCsvHeaderParser);
    private static final ThreadLocal<BiConsumer<String, String[]>> csvLineParser_TL = ThreadLocal.withInitial(() -> defaultCsvLineParser);
    private static final ThreadLocal<Boolean> isBackSlashEscapeCharForWrite_TL = ThreadLocal.withInitial(() -> false);

    /**
     * Sets the CSV header parser for the current thread.
     * This allows customization of how CSV headers are parsed on a per-thread basis.
     * The parser will remain active for the current thread until reset.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Use JSON format for headers in current thread
     * CSVUtil.setHeaderParser(CSVUtil.CSV_HEADER_PARSER_IN_JSON);
     * DataSet ds = CSVUtil.loadCSV(file); // Will use JSON parser
     * CSVUtil.resetHeaderParser(); // Reset to default
     * }</pre>
     *
     * @param parser the Function to set as the CSV header parser, must not be null
     * @throws IllegalArgumentException if the parser is null
     * @see #resetHeaderParser()
     */
    public static void setHeaderParser(final Function<String, String[]> parser) throws IllegalArgumentException {
        N.checkArgNotNull(parser, cs.parser);

        csvHeaderParser_TL.set(parser);
    }

    /**
     * Sets the CSV line parser for the current thread.
     * This allows customization of how CSV data lines are parsed on a per-thread basis.
     * The parser will remain active for the current thread until reset.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Use splitter for simple CSV files in current thread
     * CSVUtil.setLineParser(CSVUtil.CSV_LINE_PARSER_BY_SPLITTER);
     * DataSet ds = CSVUtil.loadCSV(file); // Will use splitter parser
     * CSVUtil.resetLineParser(); // Reset to default
     * }</pre>
     *
     * @param parser the BiConsumer to set as the CSV line parser, must not be null
     * @throws IllegalArgumentException if the parser is null
     * @see #resetLineParser()
     */
    public static void setLineParser(final BiConsumer<String, String[]> parser) throws IllegalArgumentException {
        N.checkArgNotNull(parser, cs.parser);

        csvLineParser_TL.set(parser);
    }

    /**
     * Resets the CSV header parser to the default parser for the current thread.
     * After calling this method, the thread will use the standard CSV header parser.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * CSVUtil.setHeaderParser(customParser);
     * // ... use custom parser
     * CSVUtil.resetHeaderParser(); // Back to default
     * }</pre>
     */
    public static void resetHeaderParser() {
        csvHeaderParser_TL.set(defaultCsvHeaderParser);
    }

    /**
     * Resets the CSV line parser to the default parser for the current thread.
     * After calling this method, the thread will use the standard CSV line parser.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * CSVUtil.setLineParser(customParser);
     * // ... use custom parser
     * CSVUtil.resetLineParser(); // Back to default
     * }</pre>
     */
    public static void resetLineParser() {
        csvLineParser_TL.set(defaultCsvLineParser);
    }

    /**
     * Returns the CSV header parser currently active in the current thread.
     * This will be either a custom parser set via setHeaderParser() or the default parser.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Function<String, String[]> currentParser = CSVUtil.getCurrentHeaderParser();
     * String[] headers = currentParser.apply("Name,Age,City");
     * }</pre>
     *
     * @return the current CSV header parser as a Function that takes a String and returns a String array
     * @see #setHeaderParser(Function)
     * @see #resetHeaderParser()
     */
    public static Function<String, String[]> getCurrentHeaderParser() {
        return csvHeaderParser_TL.get();
    }

    /**
     * Returns the CSV line parser currently active in the current thread.
     * This will be either a custom parser set via setLineParser() or the default parser.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BiConsumer<String, String[]> currentParser = CSVUtil.getCurrentLineParser();
     * String[] row = new String[3];
     * currentParser.accept("John,30,NYC", row);
     * }</pre>
     *
     * @return the current CSV line parser as a BiConsumer that takes a String and a String array
     * @see #setLineParser(BiConsumer)
     * @see #resetLineParser()
     */
    public static BiConsumer<String, String[]> getCurrentLineParser() {
        return csvLineParser_TL.get();
    }

    /**
     * Sets the escape character to backslash for CSV writing operations in the current thread.
     * This affects how special characters are escaped when writing CSV data.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * CSVUtil.setEscapeCharToBackSlashForWrite();
     * // Write CSV with backslash escaping
     * CSVUtil.resetEscapeCharForWrite(); // Reset to default
     * }</pre>
     * 
     * @see #resetEscapeCharForWrite()
     */
    public static void setEscapeCharToBackSlashForWrite() {
        isBackSlashEscapeCharForWrite_TL.set(true);
    }

    /**
     * Resets the escape character setting for CSV writing to the default in the current thread.
     * After calling this method, the thread will use the default escape character behavior.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * CSVUtil.setEscapeCharToBackSlashForWrite();
     * // ... write with backslash escaping
     * CSVUtil.resetEscapeCharForWrite(); // Back to default
     * }</pre>
     */
    public static void resetEscapeCharForWrite() {
        isBackSlashEscapeCharForWrite_TL.set(false);
    }

    static boolean isBackSlashEscapeCharForWrite() {
        return isBackSlashEscapeCharForWrite_TL.get();
    }

    @SuppressWarnings("deprecation")
    static final JSONSerializationConfig config = JSC.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP).setStringQuotation(WD._QUOTATION_D);
    static final Type<Object> strType = N.typeOf(String.class);

    /**
     * Writes a single field value to a CSV writer with appropriate formatting and escaping.
     * This method handles null values, quotable types, and proper character escaping.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BufferedCSVWriter writer = new BufferedCSVWriter(outputWriter);
     * CSVUtil.writeField(writer, Type.of(String.class), "Hello, World");
     * CSVUtil.writeField(writer, Type.of(Integer.class), 42);
     * CSVUtil.writeField(writer, null, null); // Writes NULL
     * }</pre>
     * 
     * @param writer the BufferedCSVWriter to write to
     * @param type the Type of the value, can be null (defaults to String type for null values)
     * @param value the value to write, can be null
     * @throws IOException if an I/O error occurs during writing
     */
    public static void writeField(final BufferedCSVWriter writer, final Type<?> type, final Object value) throws IOException {
        final Type<Object> valType = type != null ? (Type<Object>) type : (value == null ? strType : N.typeOf(value.getClass()));

        if (value == null) {
            writer.write(Strings.NULL_CHAR_ARRAY);
        } else if (valType.isNonQuotableCsvType()) {
            // writer.write(valType.stringOf(value));
            valType.writeCharacter(writer, value, config);
        } else {
            strType.writeCharacter(writer, valType.stringOf(value), config);
        }
    }

    /**
     * Loads CSV data from a file into a DataSet with all columns included.
     * The first line of the file is treated as column headers.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * DataSet ds = CSVUtil.loadCSV(new File("data.csv"));
     * System.out.println("Columns: " + ds.columnNameList());
     * System.out.println("Row count: " + ds.size());
     * }</pre>
     *
     * @param source the File containing CSV data
     * @return a DataSet containing the loaded CSV data
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     */
    public static DataSet loadCSV(final File source) throws UncheckedIOException {
        return loadCSV(source, (Collection<String>) null);
    }

    /**
     * Loads CSV data from a file with specified column selection.
     * Only the specified columns will be included in the resulting DataSet.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("Name", "Age", "City");
     * DataSet ds = CSVUtil.loadCSV(new File("data.csv"), columns);
     * // Only Name, Age, and City columns will be loaded
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames a Collection of column names to include, null to include all columns
     * @return a DataSet containing the loaded CSV data with selected columns
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @see #loadCSV(File, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     * Loads CSV data from a file with column selection, offset, and row limit.
     * This method allows for pagination and partial loading of large CSV files.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Skip first 100 rows, load next 50 rows
     * DataSet ds = CSVUtil.loadCSV(new File("large.csv"), null, 100, 50);
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @param offset number of data rows to skip (after header)
     * @param count maximum number of rows to read
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final long offset, final long count)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.alwaysTrue());
    }

    /**
     * Loads CSV data from a file with full control over column selection, pagination, and row filtering.
     * This is the most flexible method for loading CSV data from files.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Load only adults (age > 18) from specific columns
     * Predicate<String[]> adultFilter = row -> Integer.parseInt(row[1]) > 18;
     * DataSet ds = CSVUtil.loadCSV(
     *     new File("people.csv"), 
     *     Arrays.asList("Name", "Age"), 
     *     0, 1000, 
     *     adultFilter
     * );
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @param offset number of data rows to skip (after header)
     * @param count maximum number of rows to read
     * @param rowFilter predicate to filter rows, only matching rows are included
     * @return a DataSet containing the filtered CSV data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter) throws UncheckedIOException {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return loadCSV(reader, selectColumnNames, offset, count, rowFilter);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     * Loads CSV data from a Reader with all columns included.
     * The first line is treated as column headers.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (Reader reader = new FileReader("data.csv")) {
     *     DataSet ds = CSVUtil.loadCSV(reader);
     *     ds.println(); // Print the dataset
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @return a DataSet containing the loaded CSV data
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final Reader source) throws UncheckedIOException {
        return loadCSV(source, (Collection<String>) null);
    }

    /**
     * Loads CSV data from a Reader with specified column selection.
     * Only the specified columns will be included in the resulting DataSet.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (Reader reader = new StringReader("Name,Age,City\nJohn,30,NYC\nJane,25,LA")) {
     *     DataSet ds = CSVUtil.loadCSV(reader, Arrays.asList("Name", "City"));
     *     // Only Name and City columns will be loaded
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @return a DataSet containing the selected columns
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     * Loads CSV data from a Reader with column selection, offset, and row limit.
     * This method allows for pagination and partial loading of CSV data.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (Reader reader = new FileReader("large.csv")) {
     *     // Skip first 1000 rows, load next 100 rows
     *     DataSet ds = CSVUtil.loadCSV(reader, null, 1000, 100);
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @param offset number of data rows to skip (after header)
     * @param count maximum number of rows to read
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.alwaysTrue());
    }

    /**
     * Loads CSV data from a Reader with full control over column selection, pagination, and row filtering.
     * This is the most flexible method for loading CSV data from readers.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (Reader reader = new FileReader("sales.csv")) {
     *     // Load only high-value sales (amount > 1000)
     *     Predicate<String[]> highValueFilter = row -> Double.parseDouble(row[2]) > 1000;
     *     DataSet ds = CSVUtil.loadCSV(
     *         reader,
     *         Arrays.asList("Date", "Product", "Amount"),
     *         0, Long.MAX_VALUE,
     *         highValueFilter
     *     );
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @param offset number of data rows to skip (after header)
     * @param count maximum number of rows to read
     * @param rowFilter predicate to filter rows, only matching rows are included
     * @return a DataSet containing the filtered CSV data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, long offset, long count,
            final Predicate<? super String[]> rowFilter) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count); //NOSONAR

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);

        try {
            String line = br.readLine();

            if (line == null) {
                return N.newEmptyDataSet();
            }

            final String[] titles = headerParser.apply(line);
            final int columnCount = titles.length;
            final boolean noSelectColumnNamesSpecified = N.isEmpty(selectColumnNames)
                    || (selectColumnNames.size() == columnCount && selectColumnNames.containsAll(Arrays.asList(titles)));
            final Set<String> selectPropNameSet = noSelectColumnNamesSpecified ? null : N.newHashSet(selectColumnNames);
            final int selectColumnCount = noSelectColumnNamesSpecified ? columnCount : selectPropNameSet.size();
            final List<String> columnNameList = new ArrayList<>(selectColumnCount);
            final boolean[] isColumnSelected = noSelectColumnNamesSpecified ? null : new boolean[columnCount];

            if (noSelectColumnNamesSpecified) {
                columnNameList.addAll(Arrays.asList(titles));
            } else {
                for (int i = 0; i < columnCount; i++) {
                    if (selectPropNameSet.remove(titles[i])) {
                        columnNameList.add(titles[i]);
                        isColumnSelected[i] = true;
                    }
                }

                if (N.notEmpty(selectPropNameSet)) {
                    throw new IllegalArgumentException(selectColumnNames + " are not included in titles: " + N.toString(titles));
                }
            }

            while (offset-- > 0 && br.readLine() != null) { // NOSONAR
                // continue
            }

            final List<List<Object>> columnList = new ArrayList<>(selectColumnCount);

            for (int i = 0; i < selectColumnCount; i++) {
                columnList.add(new ArrayList<>());
            }

            final String[] row = new String[columnCount];

            while (count > 0 && (line = br.readLine()) != null) {
                N.fill(row, null);
                lineParser.accept(line, row);

                if (rowFilter != null && !rowFilter.test(row)) {
                    continue;
                }

                if (noSelectColumnNamesSpecified) {
                    for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                        columnList.get(columnIndex++).add(row[i]);
                    }
                } else {
                    for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                        if (isColumnSelected[i]) {
                            columnList.get(columnIndex++).add(row[i]);
                        }
                    }
                }

                count--;
            }

            return new RowDataSet(columnNameList, columnList);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedReader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     * Loads CSV data from a file with automatic type conversion based on a bean class.
     * Column values are converted to the appropriate types defined in the bean class properties.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * public class Person {
     *     private String name;
     *     private int age;
     *     private Date birthDate;
     *     // getters and setters...
     * }
     * 
     * DataSet ds = CSVUtil.loadCSV(new File("people.csv"), Person.class);
     * // Age will be parsed as int, birthDate as Date
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param beanClassForColumnType the bean class defining column types
     * @return a DataSet with typed columns
     * @throws IllegalArgumentException if beanClassForColumnType is null
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, Collection, long, long, Class)
     */
    public static DataSet loadCSV(final File source, final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, null, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a file with column selection and type conversion based on a bean class.
     * Only selected columns are loaded and converted to their corresponding types.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * DataSet ds = CSVUtil.loadCSV(
     *     new File("people.csv"), 
     *     Arrays.asList("name", "age"), 
     *     Person.class
     * );
     * // Only name and age columns will be loaded with proper types
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @param beanClassForColumnType the bean class defining column types
     * @return a DataSet with typed columns
     * @throws IllegalArgumentException if beanClassForColumnType is null
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, Collection, long, long, Class)
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final Class<?> beanClassForColumnType)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a file with column selection, pagination, and type conversion.
     * This method combines type conversion with offset and limit capabilities.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Load page 2 of data (items 100-199) with type conversion
     * DataSet ds = CSVUtil.loadCSV(
     *     new File("products.csv"),
     *     Arrays.asList("id", "name", "price"),
     *     100, 100,
     *     Product.class
     * );
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @param offset number of data rows to skip
     * @param count maximum number of rows to read
     * @param beanClassForColumnType the bean class defining column types
     * @return a DataSet with typed columns
     * @throws IllegalArgumentException if offset/count are negative or beanClassForColumnType is null
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, Collection, long, long, Class)
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.alwaysTrue(), beanClassForColumnType);
    }

    /**
     * Loads CSV data from a file with full control including type conversion and row filtering.
     * This method provides complete flexibility for loading typed CSV data.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Load expensive products (price > 100) with type conversion
     * Predicate<String[]> expensiveFilter = row -> Double.parseDouble(row[2]) > 100;
     * DataSet ds = CSVUtil.loadCSV(
     *     new File("products.csv"),
     *     Arrays.asList("id", "name", "price"),
     *     0, 1000,
     *     expensiveFilter,
     *     Product.class
     * );
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @param offset number of data rows to skip
     * @param count maximum number of rows to read
     * @param rowFilter predicate to filter rows
     * @param beanClassForColumnType the bean class defining column types
     * @return a DataSet with typed and filtered data
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter, final Class<?> beanClassForColumnType) throws UncheckedIOException {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return loadCSV(reader, selectColumnNames, offset, count, rowFilter, beanClassForColumnType);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     * Loads CSV data from a Reader with automatic type conversion based on a bean class.
     * Column values are converted to the appropriate types defined in the bean class properties.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (Reader reader = new FileReader("data.csv")) {
     *     DataSet ds = CSVUtil.loadCSV(reader, Person.class);
     *     // Columns will be typed according to Person properties
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param beanClassForColumnType the bean class defining column types
     * @return a DataSet with typed columns
     * @throws IllegalArgumentException if beanClassForColumnType is null
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate, Class)
     */
    public static DataSet loadCSV(final Reader source, final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, null, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a Reader with column selection and type conversion based on a bean class.
     * Only selected columns are loaded and converted to their corresponding types.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (Reader reader = new StringReader(csvData)) {
     *     DataSet ds = CSVUtil.loadCSV(
     *         reader,
     *         Arrays.asList("name", "age"),
     *         Person.class
     *     );
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @param beanClassForColumnType the bean class defining column types
     * @return a DataSet with typed columns
     * @throws IllegalArgumentException if beanClassForColumnType is null
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate, Class)
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, final Class<?> beanClassForColumnType)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a Reader with column selection, pagination, and type conversion.
     * This method combines type conversion with offset and limit capabilities.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (Reader reader = new FileReader("large.csv")) {
     *     // Load rows 500-599 with type conversion
     *     DataSet ds = CSVUtil.loadCSV(reader, null, 500, 100, Product.class);
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @param offset number of data rows to skip
     * @param count maximum number of rows to read
     * @param beanClassForColumnType the bean class defining column types
     * @return a DataSet with typed columns
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate, Class)
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.alwaysTrue(), beanClassForColumnType);
    }

    /**
     * Loads CSV data from a Reader with full control including type conversion and row filtering.
     * This is the most comprehensive method for loading typed CSV data from readers.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (Reader reader = new FileReader("employees.csv")) {
     *     // Load senior employees (age > 40) with type conversion
     *     Predicate<String[]> seniorFilter = row -> Integer.parseInt(row[2]) > 40;
     *     DataSet ds = CSVUtil.loadCSV(
     *         reader,
     *         Arrays.asList("id", "name", "age", "department"),
     *         0, Long.MAX_VALUE,
     *         seniorFilter,
     *         Employee.class
     *     );
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @param offset number of data rows to skip
     * @param count maximum number of rows to read
     * @param rowFilter predicate to filter rows
     * @param beanClassForColumnType the bean class defining column types
     * @return a DataSet with typed and filtered data
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, long offset, long count,
            final Predicate<? super String[]> rowFilter, final Class<?> beanClassForColumnType) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);
        N.checkArgNotNull(beanClassForColumnType, cs.beanClassForColumnType);

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClassForColumnType);

        try {
            String line = br.readLine();

            if (line == null) {
                return N.newEmptyDataSet();
            }

            final String[] titles = headerParser.apply(line);
            final int columnCount = titles.length;
            final boolean noSelectColumnNamesSpecified = N.isEmpty(selectColumnNames)
                    || (selectColumnNames.size() == columnCount && selectColumnNames.containsAll(Arrays.asList(titles)));
            final Set<String> selectPropNameSet = noSelectColumnNamesSpecified ? null : N.newHashSet(selectColumnNames);
            final int selectColumnCount = noSelectColumnNamesSpecified ? columnCount : selectPropNameSet.size();
            final List<String> columnNameList = new ArrayList<>(selectColumnCount);
            final boolean[] isColumnSelected = noSelectColumnNamesSpecified ? null : new boolean[columnCount];
            final PropInfo[] propInfos = new PropInfo[columnCount];

            if (noSelectColumnNamesSpecified) {
                columnNameList.addAll(Arrays.asList(titles));

                for (int i = 0; i < columnCount; i++) {
                    propInfos[i] = beanInfo.getPropInfo(titles[i]);
                }
            } else {
                for (int i = 0; i < columnCount; i++) {
                    if (selectPropNameSet.remove(titles[i])) {
                        columnNameList.add(titles[i]);
                        propInfos[i] = beanInfo.getPropInfo(titles[i]);
                        isColumnSelected[i] = true;
                    }
                }

                if (N.notEmpty(selectPropNameSet)) {
                    throw new IllegalArgumentException(selectColumnNames + " are not included in titles: " + N.toString(titles));
                }
            }

            while (offset-- > 0 && br.readLine() != null) { // NOSONAR
                // continue
            }

            final List<List<Object>> columnList = new ArrayList<>(selectColumnCount);

            for (int i = 0; i < selectColumnCount; i++) {
                columnList.add(new ArrayList<>());
            }

            final String[] row = new String[columnCount];

            while (count > 0 && (line = br.readLine()) != null) {
                N.fill(row, null);
                lineParser.accept(line, row);

                if (rowFilter != null && !rowFilter.test(row)) {
                    continue;
                }

                if (noSelectColumnNamesSpecified) {
                    for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                        if (propInfos[i] == null) {
                            columnList.get(columnIndex++).add(row[i]);
                        } else {
                            columnList.get(columnIndex++).add(propInfos[i].readPropValue(row[i]));
                        }
                    }
                } else {
                    for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                        if (isColumnSelected[i]) {
                            if (propInfos[i] == null) {
                                columnList.get(columnIndex++).add(row[i]);
                            } else {
                                columnList.get(columnIndex++).add(propInfos[i].readPropValue(row[i]));
                            }
                        }
                    }
                }

                count--;
            }

            return new RowDataSet(columnNameList, columnList);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedReader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     * Loads CSV data from a file with explicit type mapping for each column.
     * Each column name is mapped to its specific Type for proper conversion.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Type<?>> typeMap = new HashMap<>();
     * typeMap.put("id", Type.of(Long.class));
     * typeMap.put("name", Type.of(String.class));
     * typeMap.put("price", Type.of(Double.class));
     * typeMap.put("active", Type.of(Boolean.class));
     * 
     * DataSet ds = CSVUtil.loadCSV(new File("products.csv"), typeMap);
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param columnTypeMap mapping of column names to their Types
     * @return a DataSet with explicitly typed columns
     * @throws IllegalArgumentException if columnTypeMap is null or empty
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, long, long, Predicate, Map)
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(source, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     * Loads CSV data from a file with type mapping and pagination support.
     * Allows loading a subset of rows with proper type conversion.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Type<?>> typeMap = new HashMap<>();
     * typeMap.put("date", Type.of(Date.class));
     * typeMap.put("amount", Type.of(BigDecimal.class));
     * 
     * // Load rows 1000-1999
     * DataSet ds = CSVUtil.loadCSV(new File("transactions.csv"), 1000, 1000, typeMap);
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param offset number of data rows to skip
     * @param count maximum number of rows to read
     * @param columnTypeMap mapping of column names to their Types
     * @return a DataSet with typed columns
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, long, long, Predicate, Map)
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final long offset, final long count, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.alwaysTrue(), columnTypeMap);
    }

    /**
     * Loads CSV data from a file with type mapping, pagination, and row filtering.
     * Provides complete control over which rows to load and how to type them.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Type<?>> typeMap = new HashMap<>();
     * typeMap.put("timestamp", Type.of(Timestamp.class));
     * typeMap.put("level", Type.of(String.class));
     * typeMap.put("message", Type.of(String.class));
     * 
     * // Load only ERROR level logs
     * Predicate<String[]> errorFilter = row -> "ERROR".equals(row[1]);
     * DataSet ds = CSVUtil.loadCSV(
     *     new File("app.log.csv"),
     *     0, 10000,
     *     errorFilter,
     *     typeMap
     * );
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param offset number of data rows to skip
     * @param count maximum number of rows to read
     * @param rowFilter predicate to filter rows
     * @param columnTypeMap mapping of column names to their Types
     * @return a DataSet with typed and filtered data
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final long offset, final long count, final Predicate<? super String[]> rowFilter,
            final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return loadCSV(reader, offset, count, rowFilter, columnTypeMap);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     * Loads CSV data from a Reader with explicit type mapping for each column.
     * Each column name is mapped to its specific Type for proper conversion.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Type<?>> typeMap = new HashMap<>();
     * typeMap.put("userId", Type.of(UUID.class));
     * typeMap.put("score", Type.of(Float.class));
     * 
     * try (Reader reader = new FileReader("scores.csv")) {
     *     DataSet ds = CSVUtil.loadCSV(reader, typeMap);
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param columnTypeMap mapping of column names to their Types
     * @return a DataSet with explicitly typed columns
     * @throws IllegalArgumentException if columnTypeMap is null or empty
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, long, long, Predicate, Map)
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(source, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     * Loads CSV data from a Reader with type mapping and pagination support.
     * Allows loading a subset of rows with proper type conversion.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Type<?>> typeMap = new HashMap<>();
     * typeMap.put("created", Type.of(LocalDateTime.class));
     * typeMap.put("status", Type.of(String.class));
     * 
     * try (Reader reader = new FileReader("events.csv")) {
     *     // Load first 1000 events
     *     DataSet ds = CSVUtil.loadCSV(reader, 0, 1000, typeMap);
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param offset number of data rows to skip
     * @param count maximum number of rows to read
     * @param columnTypeMap mapping of column names to their Types
     * @return a DataSet with typed columns
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, long, long, Predicate, Map)
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, final long offset, final long count, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.alwaysTrue(), columnTypeMap);
    }

    /**
     * Loads CSV data from a Reader with type mapping, pagination, and row filtering.
     * This is the most comprehensive method for loading CSV with explicit type mapping.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Type<?>> typeMap = new HashMap<>();
     * typeMap.put("orderId", Type.of(Long.class));
     * typeMap.put("orderDate", Type.of(Date.class));
     * typeMap.put("total", Type.of(BigDecimal.class));
     * typeMap.put("status", Type.of(String.class));
     * 
     * try (Reader reader = new FileReader("orders.csv")) {
     *     // Load completed orders only
     *     Predicate<String[]> completedFilter = row -> "COMPLETED".equals(row[3]);
     *     DataSet ds = CSVUtil.loadCSV(reader, 0, 5000, completedFilter, typeMap);
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param offset number of data rows to skip
     * @param count maximum number of rows to read
     * @param rowFilter predicate to filter rows
     * @param columnTypeMap mapping of column names to their Types
     * @return a DataSet with typed and filtered data
     * @throws IllegalArgumentException if parameters are invalid or columnTypeMap is empty
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, long offset, long count, final Predicate<? super String[]> rowFilter,
            final Map<String, ? extends Type> columnTypeMap) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

        if (N.isEmpty(columnTypeMap)) {
            throw new IllegalArgumentException("columnTypeMap can't be null or empty");
        }

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);

        try {
            String line = br.readLine();

            if (line == null) {
                return N.newEmptyDataSet();
            }

            final String[] titles = headerParser.apply(line);

            final int columnCount = titles.length;
            final int selectColumnCount = columnTypeMap.size();
            final List<String> columnNameList = new ArrayList<>(selectColumnCount);
            final List<List<Object>> columnList = new ArrayList<>(selectColumnCount);
            final Type<?>[] columnTypes = new Type<?>[columnCount];

            for (int i = 0; i < columnCount; i++) {
                if (columnTypeMap.containsKey(titles[i])) {
                    columnTypes[i] = columnTypeMap.get(titles[i]);
                    columnNameList.add(titles[i]);
                    columnList.add(new ArrayList<>());
                }
            }

            if (columnNameList.size() != selectColumnCount) {
                final List<String> keys = new ArrayList<>(columnTypeMap.keySet());
                keys.removeAll(columnNameList);
                throw new IllegalArgumentException(keys + " are not included in titles: " + N.toString(titles));
            }

            while (offset-- > 0 && br.readLine() != null) { // NOSONAR
                // continue
            }

            final String[] row = new String[columnCount];

            while (count > 0 && (line = br.readLine()) != null) {
                N.fill(row, null);
                lineParser.accept(line, row);

                if (rowFilter != null && !rowFilter.test(row)) {
                    continue;
                }

                for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                    if (columnTypes[i] != null) {
                        columnList.get(columnIndex++).add(columnTypes[i].valueOf(row[i]));
                    }
                }

                count--;
            }

            return new RowDataSet(columnNameList, columnList);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedReader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     * Loads CSV data from a file with custom row extraction logic.
     * The row extractor receives column names and row data to produce typed output.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = 
     *     (columns, row, output) -> {
     *         output[0] = row.get(0); // name as String
     *         output[1] = Integer.parseInt(row.get(1)); // age as int
     *         output[2] = LocalDate.parse(row.get(2)); // date as LocalDate
     *     };
     * 
     * DataSet ds = CSVUtil.loadCSV(new File("data.csv"), extractor);
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param rowExtractor custom logic to extract and convert row data
     * @return a DataSet with custom extracted data
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static DataSet loadCSV(final File source,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return loadCSV(source, null, rowExtractor);
    }

    /**
     * Loads CSV data from a file with column selection and custom row extraction.
     * Only selected columns are passed to the row extractor.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = 
     *     (columns, row, output) -> {
     *         // Process only selected columns
     *         output[0] = row.get(0).toUpperCase();
     *         output[1] = Double.parseDouble(row.get(1)) * 1.1; // Add 10%
     *     };
     * 
     * DataSet ds = CSVUtil.loadCSV(
     *     new File("prices.csv"),
     *     Arrays.asList("product", "price"),
     *     extractor
     * );
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames columns to include, null for all columns
     * @param rowExtractor custom logic to extract and convert row data
     * @return a DataSet with custom extracted data
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), rowExtractor);
    }

    /**
     * Loads CSV data from a file with pagination and custom row extraction.
     * Allows processing a subset of rows with custom logic.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = 
     *     (columns, row, output) -> {
     *         // Custom parsing logic
     *         output[0] = parseCustomFormat(row.get(0));
     *         output[1] = validateAndConvert(row.get(1));
     *     };
     * 
     * // Process rows 100-199
     * DataSet ds = CSVUtil.loadCSV(new File("large.csv"), 100, 100, extractor);
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param offset number of data rows to skip
     * @param count maximum number of rows to read
     * @param rowExtractor custom logic to extract and convert row data
     * @return a DataSet with custom extracted data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static DataSet loadCSV(final File source, final long offset, final long count,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return loadCSV(source, null, offset, count, Fn.alwaysTrue(), rowExtractor);
    }

    /**
     * Loads CSV data from a File source with specified offset, count, row filter, and column type list.
     *
     * @param source the InputStream source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param rowExtractor a TriConsumer to extract the row data to the output array.
     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array. 
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return loadCSV(reader, selectColumnNames, offset, count, rowFilter, rowExtractor);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    //    /**
    //     * Loads the data from CSV.
    //     * <br />
    //     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV
    //     * <br />
    //     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
    //     *
    //     * @param source
    //     * @param rowExtractor a TriConsumer to extract the row data to the output array.
    //     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array. 
    //     * @return
    //     * @throws IllegalArgumentException if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
    //     * @throws UncheckedIOException if an I/O error occurs.
    //     */
    //    public static DataSet loadCSV(final InputStream source, final TriConsumer<? super String[], ? super String[], Object[]> rowExtractor)
    //            throws UncheckedIOException {
    //        return loadCSV(source, 0, Long.MAX_VALUE, rowExtractor);
    //    }
    //
    //    /**
    //     * Loads the data from CSV.
    //     * <br />
    //     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV
    //     * <br />
    //     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
    //     *
    //     * @param source
    //     * @param offset
    //     * @param count
    //     * @param rowExtractor a TriConsumer to extract the row data to the output array.
    //     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array.
    //     * @return
    //     * @throws IllegalArgumentException if offset or count are negative, or the size of {@code columnTypeList} is not equal to the size of columns in CSV.
    //     * @throws UncheckedIOException if an I/O error occurs.
    //     */
    //    public static DataSet loadCSV(final InputStream source, final long offset, final long count,
    //            final TriConsumer<? super String[], ? super String[], Object[]> rowExtractor) throws UncheckedIOException {
    //        return loadCSV(source, offset, count, Fn.alwaysTrue(), rowExtractor);
    //    }
    //
    //    /**
    //     * Loads CSV data from InputStream source with specified offset, count, row filter, and column type list.
    //     *
    //     * @param source the InputStream source to load CSV data from
    //     * @param offset the number of lines to skip from the beginning
    //     * @param count the maximum number of lines to read
    //     * @param rowFilter a Predicate to filter the lines
    //     * @param rowExtractor a TriConsumer to extract the row data to the output array.
    //     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array.
    //     * @return a DataSet containing the loaded CSV data
    //     * @throws IllegalArgumentException if offset or count are negative, or if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
    //     * @throws UncheckedIOException if an I/O error occurs
    //     */
    //    public static DataSet loadCSV(final InputStream source, final long offset, final long count, final Predicate<? super String[]> rowFilter,
    //            final TriConsumer<? super String[], ? super String[], Object[]> rowExtractor) throws UncheckedIOException {
    //        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR
    //
    //        return loadCSV(reader, offset, count, rowFilter, rowExtractor);
    //    }

    /**
     * Load the data from CSV.
     * <br />
     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV
     * <br />
     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
     *
     * @param source
     * @param rowExtractor a TriConsumer to extract the row data to the output array.
     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array. 
     * @return
     * @throws IllegalArgumentException if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static DataSet loadCSV(final Reader source,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return loadCSV(source, null, rowExtractor);
    }

    /**
     * Load the data from CSV.
     * <br />
     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV
     * <br />
     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
     *
     * @param source
     * @param selectColumnNames a Collection of column names to select
     * @param rowExtractor a TriConsumer to extract the row data to the output array.
     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array. 
     * @return
     * @throws IllegalArgumentException if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), rowExtractor);
    }

    /**
     * Load the data from CSV.
     * <br />
     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV
     * <br />
     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
     *
     * @param source
     * @param offset
     * @param count
     * @param rowExtractor a TriConsumer to extract the row data to the output array.
     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array. 
     * @return
     * @throws IllegalArgumentException if offset or count are negative, or the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static DataSet loadCSV(final Reader source, final long offset, final long count,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return loadCSV(source, null, offset, count, Fn.alwaysTrue(), rowExtractor);
    }

    /**
     * Loads CSV data from a Reader source with specified offset, count, row filter, and column type list.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param rowExtractor a TriConsumer to extract the row data to the output array.
     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array. 
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, long offset, long count,
            final Predicate<? super String[]> rowFilter,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);

        try {
            String line = br.readLine();

            if (line == null) {
                return N.newEmptyDataSet();
            }

            final String[] titles = headerParser.apply(line);
            final int columnCount = titles.length;
            final boolean noSelectColumnNamesSpecified = N.isEmpty(selectColumnNames)
                    || (selectColumnNames.size() == columnCount && selectColumnNames.containsAll(Arrays.asList(titles)));
            final Set<String> selectPropNameSet = noSelectColumnNamesSpecified ? null : N.newHashSet(selectColumnNames);
            final int selectColumnCount = noSelectColumnNamesSpecified ? columnCount : selectPropNameSet.size();
            final List<String> columnNameList = new ArrayList<>(selectColumnCount);
            final boolean[] isColumnSelected = noSelectColumnNamesSpecified ? null : new boolean[columnCount];

            if (noSelectColumnNamesSpecified) {
                columnNameList.addAll(Arrays.asList(titles));
            } else {
                for (int i = 0; i < columnCount; i++) {
                    if (selectPropNameSet.remove(titles[i])) {
                        isColumnSelected[i] = true;
                        columnNameList.add(titles[i]);
                    }
                }

                if (N.notEmpty(selectPropNameSet)) {
                    throw new IllegalArgumentException(selectColumnNames + " are not included in titles: " + N.toString(titles));
                }
            }

            while (offset-- > 0 && br.readLine() != null) { // NOSONAR
                // continue
            }

            final List<List<Object>> columnList = new ArrayList<>(selectColumnCount);

            for (int i = 0; i < selectColumnCount; i++) {
                columnList.add(new ArrayList<>());
            }

            final List<String> immutableColumnNameList = ImmutableList.wrap(columnNameList);
            final String[] rowData = new String[columnCount];
            final String[] selectRowData = noSelectColumnNamesSpecified ? rowData : new String[selectColumnCount];
            final NoCachingNoUpdating.DisposableArray<String> disposableRowData = NoCachingNoUpdating.DisposableArray.wrap(selectRowData);
            final Object[] output = new Object[selectColumnCount];

            while (count > 0 && (line = br.readLine()) != null) {
                N.fill(rowData, null);
                lineParser.accept(line, rowData);

                if (rowFilter != null && !rowFilter.test(rowData)) {
                    continue;
                }

                if (!noSelectColumnNamesSpecified) {
                    for (int i = 0, j = 0; i < columnCount; i++) {
                        if (isColumnSelected[i]) {
                            selectRowData[j++] = rowData[i];
                        }
                    }
                }

                N.fill(output, null);

                rowExtractor.accept(immutableColumnNameList, disposableRowData, output);

                for (int i = 0; i < selectColumnCount; i++) {
                    columnList.get(i).add(output[i]);
                }

                count--;
            }

            return new RowDataSet(columnNameList, columnList);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedReader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     * Streams CSV data from a File source with the specified target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param targetType the Class of the target type
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final File source, final Class<? extends T> targetType) {
        return stream(source, null, targetType);
    }

    /**
     * Streams CSV data from a File source with specified column names and target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param targetType the Class of the target type
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final File source, final Collection<String> selectColumnNames, final Class<? extends T> targetType) {
        return stream(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), targetType);
    }

    /**
     * Streams CSV data from a File source with specified column names, offset, count, row filter, and target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param targetType the Class of the target type
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter, final Class<? extends T> targetType) {
        FileReader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return stream(reader, selectColumnNames, offset, count, rowFilter, targetType, true);
        } catch (final Exception e) {
            if (reader != null) {
                IOUtil.closeQuietly(reader);
            }

            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Streams CSV data from a Reader source with the specified target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param targetType the Class of the target type
     * @param closeReaderWhenStreamIsClosed a boolean indicating whether to close the Reader when the stream is closed
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final Reader source, final Class<? extends T> targetType, final boolean closeReaderWhenStreamIsClosed) {
        return stream(source, null, targetType, closeReaderWhenStreamIsClosed);
    }

    /**
     * Streams CSV data from a Reader source with specified column names, offset, count, row filter, and target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param targetType the Class of the target type
     * @param closeReaderWhenStreamIsClosed a boolean indicating whether to close the Reader when the stream is closed
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames, final Class<? extends T> targetType,
            final boolean closeReaderWhenStreamIsClosed) {
        return stream(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), targetType, closeReaderWhenStreamIsClosed);
    }

    /**
     * Streams CSV data from a Reader source with specified offset, count, row filter, and target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param targetType the Class of the target type
     * @param closeReaderWhenStreamIsClosed whether to close the Reader when the stream is closed
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, of if the target type is {@code null} or not supported.
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter, final Class<? extends T> targetType, final boolean closeReaderWhenStreamIsClosed)
            throws IllegalArgumentException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);
        N.checkArgNotNull(targetType, cs.targetType);

        //noinspection resource
        return Stream.defer(() -> {

            final boolean isBufferedReader = IOUtil.isBufferedReader(source);
            final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);
            boolean noException = false;

            try {
                final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
                final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();

                final String line = br.readLine();

                if (line == null) {
                    noException = true;
                    return Stream.empty();
                }

                final boolean isBean = Beans.isBeanClass(targetType);
                final BeanInfo beanInfo = isBean ? ParserUtil.getBeanInfo(targetType) : null;

                final String[] titles = headerParser.apply(line);
                final int columnCount = titles.length;
                final boolean noSelectColumnNamesSpecified = N.isEmpty(selectColumnNames)
                        || (selectColumnNames.size() == columnCount && selectColumnNames.containsAll(Arrays.asList(titles)));
                final Set<String> selectPropNameSet = noSelectColumnNamesSpecified ? null : N.newHashSet(selectColumnNames);
                final int selectColumnCount = noSelectColumnNamesSpecified ? columnCount : selectPropNameSet.size();
                final boolean[] isColumnSelected = new boolean[columnCount];
                final PropInfo[] propInfos = isBean ? new PropInfo[columnCount] : null;

                if (noSelectColumnNamesSpecified) {
                    if (isBean) {
                        for (int i = 0; i < columnCount; i++) {
                            propInfos[i] = beanInfo.getPropInfo(titles[i]);
                            isColumnSelected[i] = propInfos[i] != null;
                        }
                    } else {
                        N.fill(isColumnSelected, true);
                    }
                } else {
                    for (int i = 0; i < columnCount; i++) {
                        if (selectPropNameSet.remove(titles[i])) {
                            if (isBean) {
                                propInfos[i] = beanInfo.getPropInfo(titles[i]);

                                if (propInfos[i] == null) {
                                    throw new IllegalArgumentException("Property '" + titles[i] + "' is not found in " + targetType);
                                }
                            }

                            isColumnSelected[i] = true;
                        }
                    }

                    if (N.notEmpty(selectPropNameSet)) {
                        throw new IllegalArgumentException(selectColumnNames + " are not included in titles: " + N.toString(titles));
                    }
                }

                final Type<T> type = Type.of(targetType);
                Function<String[], T> mapper = null;

                if (type.isObjectArray()) {
                    final Class<?> componentType = targetType.getComponentType();

                    mapper = values -> {
                        final Object[] result = N.newArray(componentType, selectColumnCount);

                        for (int i = 0, j = 0; i < columnCount; i++) {
                            if (isColumnSelected[i]) {
                                result[j++] = values[i];
                            }
                        }

                        return (T) result;
                    };

                } else if (type.isCollection()) {
                    mapper = values -> {
                        @SuppressWarnings("rawtypes")
                        final Collection<Object> result = N.newCollection((Class<Collection>) targetType, selectColumnCount);

                        for (int i = 0; i < columnCount; i++) {
                            if (isColumnSelected[i]) {
                                result.add(values[i]);
                            }
                        }

                        return (T) result;
                    };

                } else if (type.isMap()) {
                    mapper = values -> {
                        @SuppressWarnings("rawtypes")
                        final Map<String, Object> result = N.newMap((Class<Map>) targetType, selectColumnCount);

                        for (int i = 0; i < columnCount; i++) {
                            if (isColumnSelected[i]) {
                                result.put(titles[i], values[i]);
                            }
                        }

                        return (T) result;
                    };

                } else if (type.isBean()) {
                    mapper = values -> {
                        @SuppressWarnings("DataFlowIssue")
                        final Object result = beanInfo.createBeanResult();

                        for (int i = 0; i < columnCount; i++) {
                            if (isColumnSelected[i]) {
                                //noinspection DataFlowIssue
                                propInfos[i].setPropValue(result, propInfos[i].readPropValue(values[i]));
                            }
                        }

                        return beanInfo.finishBeanResult(result);
                    };

                } else if (selectColumnCount == 1) {
                    int targetColumnIndex = 0;

                    for (int i = 0; i < columnCount; i++) {
                        if (isColumnSelected[i]) {
                            targetColumnIndex = i;
                            break;
                        }
                    }

                    final int finalTargetColumnIndex = targetColumnIndex;

                    mapper = values -> type.valueOf(values[finalTargetColumnIndex]);
                } else {
                    throw new IllegalArgumentException("Unsupported target type: " + targetType);
                }

                final String[] rowData = new String[columnCount];

                final Stream<T> ret = ((rowFilter == null || N.equals(rowFilter, Fn.alwaysTrue()) || N.equals(rowFilter, Fnn.alwaysTrue())) //
                        ? Stream.ofLines(br).skip(offset).map(it -> {
                            N.fill(rowData, null);
                            lineParser.accept(it, rowData);
                            return rowData;
                        }) //
                        : Stream.ofLines(br).skip(offset).map(it -> {
                            N.fill(rowData, null);
                            lineParser.accept(it, rowData);
                            return rowData;
                        }).filter(Fn.from(rowFilter))) //
                                .limit(count)
                                .map(mapper)
                                .onClose(() -> {
                                    if (br != source) {
                                        Objectory.recycle(br);
                                    }
                                });

                noException = true;

                return ret;
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (!noException && !isBufferedReader) {
                    Objectory.recycle(br);
                }
            }
        }).onClose(() -> {
            if (closeReaderWhenStreamIsClosed) {
                IOUtil.close(source);
            }
        });
    }

    /**
     * Streams CSV data from a File source with the specified target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param rowMapper converts the row data to the target type
     *      The first parameter is the column names, the second parameter is the row data. 
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final File source,
            final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper) {
        return stream(source, null, rowMapper);
    }

    /**
     * Streams CSV data from a File source with specified column names and target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param rowMapper converts the row data to the target type
     *      The first parameter is the column names, the second parameter is the row data. 
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final File source, final Collection<String> selectColumnNames,
            final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper) {
        return stream(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), rowMapper);
    }

    /**
     * Streams CSV data from a File source with specified column names, offset, count, row filter, and target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param rowMapper converts the row data to the target type
     *      The first parameter is the column names, the second parameter is the row data. 
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter,
            final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper) {
        FileReader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return stream(reader, selectColumnNames, offset, count, rowFilter, rowMapper, true);
        } catch (final Exception e) {
            if (reader != null) {
                IOUtil.closeQuietly(reader);
            }

            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Streams CSV data from a Reader source with the specified target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param rowMapper converts the row data to the target type
     *      The first parameter is the column names, the second parameter is the row data. 
     * @param closeReaderWhenStreamIsClosed a boolean indicating whether to close the Reader when the stream is closed
     * @throws IllegalArgumentException if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final Reader source,
            final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper,
            final boolean closeReaderWhenStreamIsClosed) {
        return stream(source, null, rowMapper, closeReaderWhenStreamIsClosed);
    }

    /**
     * Streams CSV data from a Reader source with specified column names, offset, count, row filter, and target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param rowMapper converts the row data to the target type
     *      The first parameter is the column names, the second parameter is the row data. 
     * @param closeReaderWhenStreamIsClosed a boolean indicating whether to close the Reader when the stream is closed
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames,
            final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper,
            final boolean closeReaderWhenStreamIsClosed) {
        return stream(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), rowMapper, closeReaderWhenStreamIsClosed);
    }

    /**
     * Streams CSV data from a Reader source with specified offset, count, row filter, and target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param rowMapper converts the row data to the target type
     *      The first parameter is the column names, the second parameter is the row data. 
     * @param closeReaderWhenStreamIsClosed whether to close the Reader when the stream is closed
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, of if the target type is {@code null} or not supported.
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter,
            final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper,
            final boolean closeReaderWhenStreamIsClosed) throws IllegalArgumentException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

        //noinspection resource
        return Stream.defer(() -> {
            final boolean isBufferedReader = IOUtil.isBufferedReader(source);
            final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);
            boolean noException = false;

            try {
                final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
                final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();

                final String line = br.readLine();

                if (line == null) {
                    noException = true;
                    return Stream.empty();
                }

                final String[] titles = headerParser.apply(line);
                final int columnCount = titles.length;
                final boolean noSelectColumnNamesSpecified = N.isEmpty(selectColumnNames)
                        || (selectColumnNames.size() == columnCount && selectColumnNames.containsAll(Arrays.asList(titles)));
                final Set<String> selectPropNameSet = noSelectColumnNamesSpecified ? null : N.newHashSet(selectColumnNames);
                final int selectColumnCount = noSelectColumnNamesSpecified ? columnCount : selectPropNameSet.size();
                final String[] selectColumnNameArray = noSelectColumnNamesSpecified ? titles : new String[selectColumnCount];
                final boolean[] isColumnSelected = noSelectColumnNamesSpecified ? null : new boolean[columnCount];

                if (!noSelectColumnNamesSpecified) {
                    for (int i = 0, j = 0; i < columnCount; i++) {
                        if (selectPropNameSet.remove(titles[i])) {
                            isColumnSelected[i] = true;
                            selectColumnNameArray[j++] = titles[i];
                        }
                    }

                    if (N.notEmpty(selectPropNameSet)) {
                        throw new IllegalArgumentException(selectColumnNames + " are not included in titles: " + N.toString(titles));
                    }
                }

                final List<String> selectColumnNameList = List.of(selectColumnNameArray);

                final String[] rowData = new String[columnCount];
                final String[] selectRowData = noSelectColumnNamesSpecified ? rowData : new String[selectColumnCount];
                final NoCachingNoUpdating.DisposableArray<String> disposableRowData = NoCachingNoUpdating.DisposableArray.wrap(selectRowData);

                final Function<String[], T> mapper = row -> {
                    if (!noSelectColumnNamesSpecified) {
                        for (int i = 0, j = 0; i < columnCount; i++) {
                            if (isColumnSelected[i]) {
                                selectRowData[j++] = row[i];
                            }
                        }
                    }

                    return rowMapper.apply(selectColumnNameList, disposableRowData);
                };

                final Stream<T> ret = ((rowFilter == null || N.equals(rowFilter, Fn.alwaysTrue()) || N.equals(rowFilter, Fnn.alwaysTrue())) //
                        ? Stream.ofLines(br).skip(offset).map(it -> {
                            N.fill(rowData, null);
                            lineParser.accept(it, rowData);
                            return rowData;
                        }) //
                        : Stream.ofLines(br).skip(offset).map(it -> {
                            N.fill(rowData, null);
                            lineParser.accept(it, rowData);
                            return rowData;
                        }).filter(Fn.from(rowFilter))) //
                                .limit(count)
                                .map(mapper)
                                .onClose(() -> {
                                    if (br != source) {
                                        Objectory.recycle(br);
                                    }
                                });

                noException = true;

                return ret;
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (!noException && !isBufferedReader) {
                    Objectory.recycle(br);
                }
            }
        }).onClose(() -> {
            if (closeReaderWhenStreamIsClosed) {
                IOUtil.close(source);
            }
        });
    }

    /**
     * Converts a CSV file to JSON format with all columns included.
     * This is a convenience method that converts the entire CSV file to JSON format
     * without column selection or type specification.
     *
     * <p>The CSV file's first line is treated as column headers, and all subsequent
     * lines are converted to JSON objects with string values. The output JSON will
     * be an array of objects where each object represents a CSV row.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Convert employees.csv to employees.json
     * CSVUtil.csv2json(
     *     new File("data/employees.csv"),
     *     new File("output/employees.json")
     * );
     * }</pre>
     *
     * <p>Example CSV input:</p>
     * <pre>
     * Name,Age,Department
     * John,30,Engineering
     * Jane,25,Marketing
     * </pre>
     *
     * <p>Example JSON output:</p>
     * <pre>{@code
     * [
     *   {"Name":"John","Age":"30","Department":"Engineering"},
     *   {"Name":"Jane","Age":"25","Department":"Marketing"}
     * ]
     * }</pre>
     *
     * @param csvFile the source CSV file to convert
     * @param jsonFile the destination JSON file to create
     * @return the number of rows written to the JSON file
     * @throws IllegalArgumentException if csvFile or jsonFile is null
     * @throws UncheckedIOException if an I/O error occurs during file operations
     * @see #csv2json(File, Collection, File)
     * @see #csv2json(File, Collection, File, Class)
     */
    public static long csv2json(final File csvFile, final File jsonFile) throws UncheckedIOException {
        return csv2json(csvFile, null, jsonFile);
    }

    /**
     * Converts a CSV file to JSON format with selected columns.
     * This method allows you to specify which columns from the CSV file should be
     * included in the JSON output. All values are treated as strings in the output.
     *
     * <p>The CSV file's first line is treated as column headers. Only the columns
     * specified in {@code selectColumnNames} will be included in the JSON output.
     * If {@code selectColumnNames} is null, all columns will be included.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Convert only specific columns from employees.csv to JSON
     * CSVUtil.csv2json(
     *     new File("data/employees.csv"),
     *     Arrays.asList("Name", "Department"),
     *     new File("output/employees_filtered.json")
     * );
     * }</pre>
     *
     * <p>Example CSV input:</p>
     * <pre>
     * Name,Age,Department,Salary
     * John,30,Engineering,75000
     * Jane,25,Marketing,65000
     * </pre>
     *
     * <p>Example JSON output (with selected columns "Name", "Department"):</p>
     * <pre>{@code
     * [
     *   {"Name":"John","Department":"Engineering"},
     *   {"Name":"Jane","Department":"Marketing"}
     * ]
     * }</pre>
     *
     * @param csvFile the source CSV file to convert
     * @param selectColumnNames the collection of column names to include in JSON output, 
     *                         null to include all columns
     * @param jsonFile the destination JSON file to create
     * @return the number of rows written to the JSON file
     * @throws IllegalArgumentException if csvFile or jsonFile is null
     * @throws UncheckedIOException if an I/O error occurs during file operations
     * @see #csv2json(File, File)
     * @see #csv2json(File, Collection, File, Class)
     */
    public static long csv2json(final File csvFile, final Collection<String> selectColumnNames, final File jsonFile) throws UncheckedIOException {
        return csv2json(csvFile, selectColumnNames, jsonFile, null, true);
    }

    /**
     * Converts a CSV file to JSON format with selected columns and type conversion.
     * This method allows you to specify which columns to include and provides type conversion
     * based on a bean class definition. Values are converted from strings to their appropriate
     * types as defined by the bean class properties.
     *
     * <p>The CSV file's first line is treated as column headers. Only the columns
     * specified in {@code selectColumnNames} will be included in the JSON output.
     * The {@code beanClassForTypeWriting} parameter defines how each column should be
     * typed in the JSON output - properties of the bean class determine the target types.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * public class Employee {
     *     private String name;
     *     private Integer age;
     *     private String department;
     *     // getters and setters...
     * }
     *
     * // Convert with type conversion - age will be numeric in JSON
     * CSVUtil.csv2json(
     *     new File("data/employees.csv"),
     *     Arrays.asList("name", "age", "department"),
     *     new File("output/employees_typed.json"),
     *     Employee.class
     * );
     * }</pre>
     *
     * <p>Example CSV input:</p>
     * <pre>
     * name,age,department,salary
     * John,30,Engineering,75000
     * Jane,25,Marketing,65000
     * </pre>
     *
     * <p>Example JSON output (with selected columns and type conversion):</p>
     * <pre>{@code
     * [
     *   {"name":"John","age":30,"department":"Engineering"},
     *   {"name":"Jane","age":25,"department":"Marketing"}
     * ]
     * }</pre>
     *
     * @param csvFile the source CSV file to convert
     * @param selectColumnNames the collection of column names to include in JSON output,
     *                         null to include all columns
     * @param jsonFile the destination JSON file to create
     * @param beanClassForTypeWriting the bean class defining property types for conversion,
     *                               null to treat all values as strings
     * @return the number of rows written to the JSON file                              
     * @throws IllegalArgumentException if csvFile or jsonFile is null
     * @throws UncheckedIOException if an I/O error occurs during file operations
     * @see #csv2json(File, File)
     * @see #csv2json(File, Collection, File)
     */
    public static long csv2json(final File csvFile, final Collection<String> selectColumnNames, final File jsonFile, final Class<?> beanClassForTypeWriting)
            throws UncheckedIOException {
        return csv2json(csvFile, selectColumnNames, jsonFile, beanClassForTypeWriting, false);
    }

    private static long csv2json(final File csvFile, final Collection<String> selectColumnNames, final File jsonFile, final Class<?> beanClassForTypeWriting,
            final boolean canBeanClassForTypeWritingBeNull) throws UncheckedIOException {
        if (beanClassForTypeWriting == null && !canBeanClassForTypeWritingBeNull) {
            throw new IllegalArgumentException("'beanClassForTypeWriting' can't be null.");
        }

        try (final BufferedReader reader = IOUtil.newBufferedReader(csvFile); //
                final BufferedJSONWriter bw = Objectory.createBufferedJSONWriter(IOUtil.newFileWriter(jsonFile))) {
            final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
            final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();

            String line = reader.readLine();

            if (line == null) {
                return 0;
            }

            final Type<Object> strType = Type.of(String.class);

            final String[] titles = headerParser.apply(line);
            final int columnCount = titles.length;
            final boolean noSelectColumnNamesSpecified = N.isEmpty(selectColumnNames)
                    || (selectColumnNames.size() == columnCount && selectColumnNames.containsAll(Arrays.asList(titles)));
            final Set<String> selectPropNameSet = noSelectColumnNamesSpecified ? null : N.newHashSet(selectColumnNames);
            final boolean[] isColumnSelected = new boolean[columnCount];

            if (noSelectColumnNamesSpecified) {
                N.fill(isColumnSelected, true);
            } else {
                for (int i = 0; i < columnCount; i++) {
                    if (selectPropNameSet.remove(titles[i])) {
                        isColumnSelected[i] = true;
                    }
                }

                if (N.notEmpty(selectPropNameSet)) {
                    throw new IllegalArgumentException(selectColumnNames + " are not included in titles: " + N.toString(titles));
                }
            }

            final Type<Object>[] columnType = new Type[columnCount];

            if (beanClassForTypeWriting == null) {
                Arrays.fill(columnType, strType);
            } else {
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClassForTypeWriting);

                for (int i = 0; i < columnCount; i++) {
                    final PropInfo propInfo = beanInfo.getPropInfo(titles[i]);

                    if (propInfo == null || propInfo.type.equals(strType)) {
                        columnType[i] = strType;
                    } else {
                        columnType[i] = propInfo.type;
                    }
                }
            }

            long cnt = 0;
            final String[] rowData = new String[columnCount];

            boolean firstRow = true;
            bw.write("[\n");

            while ((line = reader.readLine()) != null) {
                if (!firstRow) {
                    bw.write(",\n");
                } else {
                    firstRow = false;
                }

                lineParser.accept(line, rowData);

                bw.write("{");
                boolean firstColumn = true;

                for (int i = 0; i < columnCount; i++) {
                    if (isColumnSelected[i]) {
                        if (!firstColumn) {
                            bw.write(",");
                        } else {
                            firstColumn = false;
                        }

                        bw.write("\"");
                        bw.write(titles[i]);
                        bw.write("\":");
                        if (columnType[i] == strType) {
                            columnType[i].writeCharacter(bw, rowData[i], config);
                        } else {
                            columnType[i].writeCharacter(bw, columnType[i].valueOf(rowData[i]), config);
                        }
                    }
                }

                bw.write("}");

                if (++cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                    bw.flush();
                }
            }

            bw.write("\n]");

            return cnt;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Converts a JSON file to CSV format with all columns included.
     * This is a convenience method that converts the entire JSON file to CSV format
     * without header selection.
     *
     * <p>The JSON file must contain an array of objects where each object represents a row.
     * The first object's properties will be used as CSV headers.</p> 
     *
     * @param jsonFile the source JSON file to convert
     * @param csvFile the destination CSV file to create
     * @return the number of rows written to the CSV file (including header row)
     * @throws IllegalArgumentException if jsonFile or csvFile is null
     * @throws UncheckedIOException if an I/O error occurs during file operations or if the JSON format is invalid
     * @see #json2csv(File, Collection, File)
     */
    public static long json2csv(final File jsonFile, final File csvFile) throws UncheckedIOException {
        return json2csv(jsonFile, null, csvFile);
    }

    /**
     * Converts a JSON file to CSV format with optional header selection.
     * This method reads a JSON file containing an array of objects and converts it to CSV format.
     * Each JSON object becomes a row in the CSV file, with object properties becoming columns.
     *
     * <p>The JSON file must contain an array of objects where each object represents a row.
     * If {@code selectCsvHeaders} is provided, only those properties will be included as columns
     * in the CSV output. If {@code selectCsvHeaders} is null or empty, all properties from the
     * first JSON object will be used as headers.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Convert with specific headers
     * CSVUtil.json2csv(
     *     new File("data/employees.json"),
     *     Arrays.asList("name", "age", "department"),
     *     new File("output/employees.csv")
     * );
     * }</pre>
     *
     * <p>Example JSON input:</p>
     * <pre>{@code
     * [
     *   {"name":"John","age":30,"department":"Engineering","salary":75000},
     *   {"name":"Jane","age":25,"department":"Marketing","salary":65000}
     * ]
     * }</pre>
     *
     * <p>Example CSV output (with selected headers "name", "age", "department"):</p>
     * <pre>
     * "name","age","department"
     * "John",30,"Engineering"
     * "Jane",25,"Marketing"
     * </pre>
     *
     * @param jsonFile the source JSON file to convert
     * @param selectCsvHeaders the collection of property names to include as CSV headers,
     *                        null or empty to include all properties from the first object
     * @param csvFile the destination CSV file to create
     * @return the number of rows written to the CSV file (including header row)
     * @throws IllegalArgumentException if jsonFile or csvFile is null
     * @throws UncheckedIOException if an I/O error occurs during file operations or if the JSON format is invalid
     * @see #json2csv(File, File)
     */
    public static long json2csv(final File jsonFile, final Collection<String> selectCsvHeaders, final File csvFile) throws UncheckedIOException {
        try (final Stream<Map<String, Object>> strem = jsonParser.stream(jsonFile, Type.ofMap(String.class, Object.class)); //
                final BufferedCSVWriter bw = Objectory.createBufferedCSVWriter(IOUtil.newFileWriter(csvFile))) {
            final List<Object> headers = N.newArrayList(selectCsvHeaders);

            final char separator = WD._COMMA;
            Map<String, Object> row = null;
            long cnt = 0;

            @SuppressWarnings({ "resource", "deprecation" })
            final Iterator<Map<String, Object>> iter = strem.iterator();

            if (iter.hasNext()) {
                cnt++;
                row = iter.next();

                if (N.isEmpty(headers)) {
                    headers.addAll(row.keySet());
                }

                final int headSize = headers.size();

                for (int i = 0; i < headSize; i++) {
                    if (i > 0) {
                        bw.write(separator);
                    }

                    writeField(bw, null, headers.get(i));
                }

                bw.write(IOUtil.LINE_SEPARATOR);

                for (int i = 0; i < headSize; i++) {
                    if (i > 0) {
                        bw.write(separator);
                    }

                    writeField(bw, null, row.get(headers.get(i)));
                }

                while (iter.hasNext()) {
                    row = iter.next();

                    bw.write(IOUtil.LINE_SEPARATOR);

                    for (int i = 0; i < headSize; i++) {
                        if (i > 0) {
                            bw.write(separator);
                        }

                        writeField(bw, null, row.get(headers.get(i)));
                    }

                    if (++cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                }
            }

            return cnt;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
