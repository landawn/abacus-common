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
import java.io.Writer;
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
import com.landawn.abacus.parser.JsonDeserializationConfig;
import com.landawn.abacus.parser.JsonDeserializationConfig.JDC;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.JsonSerializationConfig;
import com.landawn.abacus.parser.JsonSerializationConfig.JSC;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.function.Callable;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

/**
 * This utility class providing advanced CSV (Comma-Separated Values) data processing
 * capabilities including high-performance parsing, streaming operations, type-safe conversions, and seamless
 * integration with Dataset objects for efficient data manipulation and analysis. This class serves as a powerful
 * toolkit for ETL (Extract, Transform, Load) operations, data import/export scenarios, and bulk data processing
 * commonly encountered in enterprise data management, analytics, and reporting applications.
 *
 * <p>The {@code CsvUtil} class addresses critical challenges in enterprise CSV data processing by providing
 * optimized, scalable solutions for handling large CSV files efficiently while maintaining data integrity
 * and type safety. It supports various data sources, custom parsing configurations, and flexible output
 * formats, offering configurable options for memory management, custom transformations, and filtering
 * operations suitable for production environments with strict performance and reliability requirements.</p>
 *
 * @see Dataset
 * @see CsvParser
 * @see com.landawn.abacus.util.stream.Stream
 * @see com.landawn.abacus.parser.JsonParser
 * @see com.landawn.abacus.annotation.Column
 * @see <a href="https://tools.ietf.org/html/rfc4180">RFC 4180: CSV Format Specification</a>
 */
public final class CsvUtil {
    private CsvUtil() {
        // Utility class
    }

    /** JSON parser used for parsing JSON data within CSV operations. */
    public static final JsonParser jsonParser = ParserFactory.createJsonParser();

    /** JSON deserialization configuration for deserializing String element types. */
    static final JsonDeserializationConfig jdc = JDC.create().setElementType(String.class);

    static final Splitter lineSplitter = Splitter.with(',').trimResults();

    static final CsvParser csvParser = new CsvParser();

    static final int BATCH_SIZE_FOR_FLUSH = 1000;

    /**
     * Default CSV header parser function that uses the internal CsvParser.
     * This parser handles quoted fields and escape characters according to CSV standards.
     * 
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use JSON format for headers in current thread
     * CsvUtil.setHeaderParser(CsvUtil.CSV_HEADER_PARSER_IN_JSON);
     * try {
     *     Dataset ds = CsvUtil.load(file);   // Will use JSON parser
     * } finally {
     *     CsvUtil.resetHeaderParser();          // Reset to default
     * }
     * }</pre>
     *
     * @param parser the Function to set as the CSV header parser, must not be null.
     * @throws IllegalArgumentException if the parser is null.
     * @see #resetHeaderParser()
     * @see #getCurrentHeaderParser()
     * @see #setLineParser(BiConsumer)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use splitter for simple CSV files in current thread
     * CsvUtil.setLineParser(CsvUtil.CSV_LINE_PARSER_BY_SPLITTER);
     * try {
     *     Dataset ds = CsvUtil.load(file);   // Will use splitter parser
     * } finally {
     *     CsvUtil.resetLineParser();            // Reset to default
     * }
     * }</pre>
     *
     * @param parser the BiConsumer to set as the CSV line parser, must not be null.
     * @throws IllegalArgumentException if the parser is null.
     * @see #resetLineParser()
     * @see #getCurrentLineParser()
     * @see #setHeaderParser(Function)
     */
    public static void setLineParser(final BiConsumer<String, String[]> parser) throws IllegalArgumentException {
        N.checkArgNotNull(parser, cs.parser);

        csvLineParser_TL.set(parser);
    }

    /**
     * Resets the CSV header parser to the default parser for the current thread.
     * After calling this method, the thread will use the standard CSV header parser.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use JSON format for headers in current thread
     * CsvUtil.setHeaderParser(CsvUtil.CSV_HEADER_PARSER_IN_JSON);
     * try {
     *     Dataset ds = CsvUtil.load(file);   // Will use JSON parser
     * } finally {
     *     CsvUtil.resetHeaderParser();          // Reset to default
     * }
     * }</pre>
     *
     * @see #setHeaderParser(Function)
     * @see #getCurrentHeaderParser()
     * @see #resetLineParser()
     */
    public static void resetHeaderParser() {
        csvHeaderParser_TL.set(defaultCsvHeaderParser);
    }

    /**
     * Resets the CSV line parser to the default parser for the current thread.
     * After calling this method, the thread will use the standard CSV line parser.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use splitter for simple CSV files in current thread
     * CsvUtil.setLineParser(CsvUtil.CSV_LINE_PARSER_BY_SPLITTER);
     * try {
     *     Dataset ds = CsvUtil.load(file);   // Will use splitter parser
     * } finally {
     *     CsvUtil.resetLineParser();            // Reset to default
     * }
     * }</pre>
     *
     * @see #setLineParser(BiConsumer)
     * @see #getCurrentLineParser()
     * @see #resetHeaderParser()
     */
    public static void resetLineParser() {
        csvLineParser_TL.set(defaultCsvLineParser);
    }

    /**
     * Returns the CSV header parser currently active in the current thread.
     * This will be either a custom parser set via setHeaderParser() or the default parser.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Function<String, String[]> currentParser = CsvUtil.getCurrentHeaderParser();
     * String[] headers = currentParser.apply("Name,Age,City");
     * }</pre>
     *
     * @return the current CSV header parser as a Function that takes a String and returns a String array.
     * @see #setHeaderParser(Function)
     * @see #resetHeaderParser()
     * @see #getCurrentLineParser()
     */
    public static Function<String, String[]> getCurrentHeaderParser() {
        return csvHeaderParser_TL.get();
    }

    /**
     * Returns the CSV line parser currently active in the current thread.
     * This will be either a custom parser set via setLineParser() or the default parser.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumer<String, String[]> currentParser = CsvUtil.getCurrentLineParser();
     * String[] row = new String[3];
     * currentParser.accept("John,30,NYC", row);
     * }</pre>
     *
     * @return the current CSV line parser as a BiConsumer that takes a String and a String array.
     * @see #setLineParser(BiConsumer)
     * @see #resetLineParser()
     * @see #getCurrentHeaderParser()
     */
    public static BiConsumer<String, String[]> getCurrentLineParser() {
        return csvLineParser_TL.get();
    }

    /**
     * Sets the escape character to backslash for CSV writing operations in the current thread.
     * This affects how special characters are escaped when writing CSV data.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvUtil.setEscapeCharToBackSlashForWrite();
     * try {
     *     // Write CSV with backslash escaping
     * } finally {
     *     CsvUtil.resetEscapeCharForWrite();   // Reset to default
     * }
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvUtil.setEscapeCharToBackSlashForWrite();
     * try {
     *     // ... write with backslash escaping
     * } finally {
     *     CsvUtil.resetEscapeCharForWrite();   // Back to default
     * }
     * }</pre>
     */
    public static void resetEscapeCharForWrite() {
        isBackSlashEscapeCharForWrite_TL.set(false);
    }

    static boolean isBackSlashEscapeCharForWrite() {
        return isBackSlashEscapeCharForWrite_TL.get();
    }

    @SuppressWarnings("deprecation")
    static final JsonSerializationConfig config = JSC.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP).setStringQuotation(WD._QUOTATION_D);
    static final Type<String> strType = Type.of(String.class);

    /**
     * Writes a single field value to a CSV writer with appropriate formatting and escaping.
     * This method handles {@code null} values, quotable types, and proper character escaping.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedCsvWriter writer = new BufferedCsvWriter(outputWriter);
     * CsvUtil.writeField(writer, Type.of(String.class), "Hello, World");
     * CsvUtil.writeField(writer, Type.of(Integer.class), 42);
     * CsvUtil.writeField(writer, null, null);   // Writes NULL
     * }</pre>
     * 
     * @param writer the BufferedCsvWriter to write to.
     * @param type the Type of the value, can be {@code null} (defaults to String type for {@code null} values).
     * @param value the value to write, can be null.
     * @throws IOException if an I/O error occurs during writing.
     */
    public static void writeField(final BufferedCsvWriter writer, final Type<?> type, final Object value) throws IOException {
        @SuppressWarnings("rawtypes")
        final Type<Object> valType = type != null ? (Type<Object>) type : (value == null ? (Type) strType : Type.of(value.getClass()));

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
     * Loads CSV data from a file into a Dataset with all columns included.
     * The first line of the file is treated as column headers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dataset ds = CsvUtil.load(new File("data.csv"));
     * System.out.println("Columns: " + ds.columnNameList());
     * System.out.println("Row count: " + ds.size());
     * }</pre>
     *
     * @param source the File containing CSV data.
     * @return a Dataset containing the loaded CSV data.
     * @throws UncheckedIOException if an I/O error occurs while reading the file.
     * @see #load(File, Collection)
     * @see #load(File, Collection, long, long)
     * @see #load(File, Collection, long, long, Predicate)
     * @see #load(File, Class)
     */
    public static Dataset load(final File source) throws UncheckedIOException {
        return load(source, (Collection<String>) null);
    }

    /**
     * Loads CSV data from a file with specified column selection.
     * Only the specified columns will be included in the resulting Dataset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("Name", "Age", "City");
     * Dataset ds = CsvUtil.load(new File("data.csv"), columns);
     * // Only Name, Age, and City columns will be loaded
     * }</pre>
     *
     * @param source the File containing CSV data.
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns.
     * @return a Dataset containing the loaded CSV data with selected columns.
     * @throws UncheckedIOException if an I/O error occurs while reading the file.
     * @see #load(File)
     * @see #load(File, Collection, long, long)
     * @see #load(File, Collection, long, long, Predicate)
     * @see #load(File, Collection, Class)
     */
    public static Dataset load(final File source, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return load(source, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     * Loads CSV data from a file with specified column selection, offset, and row limit.
     * This method allows for pagination and partial loading of large CSV files.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Skip first 100 rows, load next 50 rows
     * Dataset ds = CsvUtil.load(new File("large.csv"), null, 100, 50);
     * }</pre>
     *
     * @param source the File containing CSV data.
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns.
     * @param offset the number of data rows to skip from the beginning (after header).
     * @param count the maximum number of rows to process.
     * @return a Dataset containing the loaded CSV data.
     * @throws IllegalArgumentException if offset or count are negative.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #load(File)
     * @see #load(File, Collection)
     * @see #load(File, Collection, long, long, Predicate)
     * @see #load(File, Collection, long, long, Class)
     */
    public static Dataset load(final File source, final Collection<String> selectColumnNames, final long offset, final long count) throws UncheckedIOException {
        return load(source, selectColumnNames, offset, count, Fn.alwaysTrue());
    }

    /**
     * Loads CSV data from a file with full control over column selection, pagination, and row filtering.
     * This is the most flexible method for loading CSV data from files.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Load only adults (age > 18) from specific columns
     * Predicate<String[]> adultFilter = row -> Integer.parseInt(row[1]) > 18;
     * Dataset ds = CsvUtil.load(
     *     new File("people.csv"),
     *     Arrays.asList("Name", "Age"),
     *     0, 1000,
     *     adultFilter
     * );
     * }</pre>
     *
     * @param source the File containing CSV data.
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns.
     * @param offset the number of data rows to skip from the beginning (after header).
     * @param count the maximum number of rows to process.
     * @param rowFilter a Predicate to filter rows, only matching rows are included.
     * @return a Dataset containing the filtered CSV data.
     * @throws IllegalArgumentException if offset or count are negative.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #load(File)
     * @see #load(File, Collection)
     * @see #load(File, Collection, long, long)
     * @see #load(Reader, Collection, long, long, Predicate)
     * @see #load(File, Collection, long, long, Predicate, Class)
     */
    public static Dataset load(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter) throws UncheckedIOException {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return load(reader, selectColumnNames, offset, count, rowFilter);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     * Loads CSV data from a Reader with all columns included.
     * The first line is treated as column headers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("data.csv")) {
     *     Dataset ds = CsvUtil.load(reader);
     *     ds.println();   // Print the dataset
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data.
     * @return a Dataset containing the loaded CSV data.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #load(Reader, Collection)
     * @see #load(Reader, Collection, long, long)
     * @see #load(Reader, Collection, long, long, Predicate)
     * @see #load(Reader, Class)
     */
    public static Dataset load(final Reader source) throws UncheckedIOException {
        return load(source, (Collection<String>) null);
    }

    /**
     * Loads CSV data from a Reader with specified column selection.
     * Only the specified columns will be included in the resulting Dataset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new StringReader("Name,Age,City\nJohn,30,NYC\nJane,25,LA")) {
     *     Dataset ds = CsvUtil.load(reader, Arrays.asList("Name", "City"));
     *     // Only Name and City columns will be loaded
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data.
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns.
     * @return a Dataset containing the selected columns.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #load(Reader)
     * @see #load(Reader, Collection, long, long)
     * @see #load(Reader, Collection, long, long, Predicate)
     * @see #load(Reader, Collection, Class)
     */
    public static Dataset load(final Reader source, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return load(source, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     * Loads CSV data from a Reader with specified column selection, offset, and row limit.
     * This method allows for pagination and partial loading of CSV data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("large.csv")) {
     *     // Skip first 1000 rows, load next 100 rows
     *     Dataset ds = CsvUtil.load(reader, null, 1000, 100);
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data.
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns.
     * @param offset the number of data rows to skip from the beginning (after header).
     * @param count the maximum number of rows to process.
     * @return a Dataset containing the loaded CSV data.
     * @throws IllegalArgumentException if offset or count are negative.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #load(Reader)
     * @see #load(Reader, Collection)
     * @see #load(Reader, Collection, long, long, Predicate)
     * @see #load(Reader, Collection, long, long, Class)
     */
    public static Dataset load(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count)
            throws UncheckedIOException {
        return load(source, selectColumnNames, offset, count, Fn.alwaysTrue());
    }

    /**
     * Loads CSV data from a Reader with full control over column selection, pagination, and row filtering.
     * This is the most flexible method for loading CSV data from readers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("sales.csv")) {
     *     // Load only high-value sales (amount > 1000)
     *     Predicate<String[]> highValueFilter = row -> Double.parseDouble(row[2]) > 1000;
     *     Dataset ds = CsvUtil.load(
     *         reader,
     *         Arrays.asList("Date", "Product", "Amount"),
     *         0, Long.MAX_VALUE,
     *         highValueFilter
     *     );
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowFilter a Predicate to filter rows, only matching rows are included
     * @return a Dataset containing the filtered CSV data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(Reader)
     * @see #load(Reader, Collection)
     * @see #load(Reader, Collection, long, long)
     * @see #load(File, Collection, long, long, Predicate)
     * @see #load(Reader, Collection, long, long, Predicate, Class)
     */
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    public static Dataset load(final Reader source, final Collection<String> selectColumnNames, long offset, long count,
            final Predicate<? super String[]> rowFilter) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s cannot be negative", offset, count); //NOSONAR

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);

        try {
            String line = br.readLine();

            if (line == null) {
                return N.newEmptyDataset();
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

            while (count-- > 0 && (line = br.readLine()) != null) {
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
            }

            return new RowDataset(columnNameList, columnList);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class Person {
     *     private String name;
     *     private int age;
     *     private Date birthDate;
     *     // getters and setters...
     * }
     *
     * Dataset ds = CsvUtil.load(new File("people.csv"), Person.class);
     * // Age will be parsed as int, birthDate as Date
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param beanClassForColumnType the bean class defining column types
     * @return a Dataset with typed columns
     * @throws IllegalArgumentException if beanClassForColumnType is null
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(File)
     * @see #load(File, Collection, Class)
     * @see #load(File, Collection, long, long, Class)
     * @see #load(File, Collection, long, long, Predicate, Class)
     */
    public static Dataset load(final File source, final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return load(source, null, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a file with column selection and type conversion based on a bean class.
     * Only selected columns are loaded and converted to their corresponding types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dataset ds = CsvUtil.load(
     *     new File("people.csv"),
     *     Arrays.asList("name", "age"),
     *     Person.class
     * );
     * // Only name and age columns will be loaded with proper types
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param beanClassForColumnType the bean class defining column types
     * @return a Dataset with typed columns
     * @throws IllegalArgumentException if beanClassForColumnType is null
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(File, Class)
     * @see #load(File, Collection, long, long, Class)
     * @see #load(File, Collection, long, long, Predicate, Class)
     */
    public static Dataset load(final File source, final Collection<String> selectColumnNames, final Class<?> beanClassForColumnType)
            throws UncheckedIOException {
        return load(source, selectColumnNames, 0, Long.MAX_VALUE, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a file with column selection, pagination, and type conversion.
     * This method combines type conversion with offset and limit capabilities.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Load page 2 of data (items 100-199) with type conversion
     * Dataset ds = CsvUtil.load(
     *     new File("products.csv"),
     *     Arrays.asList("id", "name", "price"),
     *     100, 100,
     *     Product.class
     * );
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param beanClassForColumnType the bean class defining column types
     * @return a Dataset with typed columns
     * @throws IllegalArgumentException if offset/count are negative or beanClassForColumnType is null
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(File, Class)
     * @see #load(File, Collection, Class)
     * @see #load(File, Collection, long, long, Predicate, Class)
     */
    public static Dataset load(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return load(source, selectColumnNames, offset, count, Fn.alwaysTrue(), beanClassForColumnType);
    }

    /**
     * Loads CSV data from a file with full control including type conversion and row filtering.
     * This method provides complete flexibility for loading typed CSV data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Load expensive products (price > 100) with type conversion
     * Predicate<String[]> expensiveFilter = row -> Double.parseDouble(row[2]) > 100;
     * Dataset ds = CsvUtil.load(
     *     new File("products.csv"),
     *     Arrays.asList("id", "name", "price"),
     *     0, 1000,
     *     expensiveFilter,
     *     Product.class
     * );
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowFilter a Predicate to filter rows, only matching rows are included
     * @param beanClassForColumnType the bean class defining column types
     * @return a Dataset with typed and filtered data
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(File, Class)
     * @see #load(File, Collection, Class)
     * @see #load(File, Collection, long, long, Class)
     * @see #load(Reader, Collection, long, long, Predicate, Class)
     */
    public static Dataset load(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter, final Class<?> beanClassForColumnType) throws UncheckedIOException {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return load(reader, selectColumnNames, offset, count, rowFilter, beanClassForColumnType);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     * Loads CSV data from a Reader with automatic type conversion based on a bean class.
     * Column values are converted to the appropriate types defined in the bean class properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("data.csv")) {
     *     Dataset ds = CsvUtil.load(reader, Person.class);
     *     // Columns will be typed according to Person properties
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param beanClassForColumnType the bean class defining column types
     * @return a Dataset with typed columns
     * @throws IllegalArgumentException if beanClassForColumnType is null
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(Reader)
     * @see #load(Reader, Collection, Class)
     * @see #load(Reader, Collection, long, long, Class)
     * @see #load(Reader, Collection, long, long, Predicate, Class)
     */
    public static Dataset load(final Reader source, final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return load(source, null, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a Reader with column selection and type conversion based on a bean class.
     * Only selected columns are loaded and converted to their corresponding types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new StringReader(csvData)) {
     *     Dataset ds = CsvUtil.load(
     *         reader,
     *         Arrays.asList("name", "age"),
     *         Person.class
     *     );
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param beanClassForColumnType the bean class defining column types
     * @return a Dataset with typed columns
     * @throws IllegalArgumentException if beanClassForColumnType is null
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(Reader, Class)
     * @see #load(Reader, Collection, long, long, Class)
     * @see #load(Reader, Collection, long, long, Predicate, Class)
     */
    public static Dataset load(final Reader source, final Collection<String> selectColumnNames, final Class<?> beanClassForColumnType)
            throws UncheckedIOException {
        return load(source, selectColumnNames, 0, Long.MAX_VALUE, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a Reader with column selection, pagination, and type conversion.
     * This method combines type conversion with offset and limit capabilities.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("large.csv")) {
     *     // Load rows 500-599 with type conversion
     *     Dataset ds = CsvUtil.load(reader, null, 500, 100, Product.class);
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param beanClassForColumnType the bean class defining column types
     * @return a Dataset with typed columns
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(Reader, Class)
     * @see #load(Reader, Collection, Class)
     * @see #load(Reader, Collection, long, long, Predicate, Class)
     */
    public static Dataset load(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return load(source, selectColumnNames, offset, count, Fn.alwaysTrue(), beanClassForColumnType);
    }

    /**
     * Loads CSV data from a Reader with full control including type conversion and row filtering.
     * This is the most comprehensive method for loading typed CSV data from readers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("employees.csv")) {
     *     // Load senior employees (age > 40) with type conversion
     *     Predicate<String[]> seniorFilter = row -> Integer.parseInt(row[2]) > 40;
     *     Dataset ds = CsvUtil.load(
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
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowFilter a Predicate to filter rows, only matching rows are included
     * @param beanClassForColumnType the bean class defining column types
     * @return a Dataset with typed and filtered data
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(Reader, Class)
     * @see #load(Reader, Collection, Class)
     * @see #load(Reader, Collection, long, long, Class)
     * @see #load(File, Collection, long, long, Predicate, Class)
     */
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    public static Dataset load(final Reader source, final Collection<String> selectColumnNames, long offset, long count,
            final Predicate<? super String[]> rowFilter, final Class<?> beanClassForColumnType) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s cannot be negative", offset, count);
        N.checkArgNotNull(beanClassForColumnType, cs.beanClassForColumnType);

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClassForColumnType);

        try {
            String line = br.readLine();

            if (line == null) {
                return N.newEmptyDataset();
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

            while (count-- > 0 && (line = br.readLine()) != null) {
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
            }

            return new RowDataset(columnNameList, columnList);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedReader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     * Loads CSV data from a file with all columns included and provided column-to-type mapping.
     * Columns present in {@code columnTypeMap} will be parsed into the specified {@link Type};
     * all other columns default to {@link String}. 
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String,  ? extends Type<?>> typeMap = new HashMap<>();
     * typeMap.put("id", Type.of(Long.class));
     * // typeMap.put("name", Type.of(String.class)); // String is default
     * typeMap.put("price", Type.of(Double.class));
     * typeMap.put("active", Type.of(Boolean.class));
     * 
     * Dataset ds = CsvUtil.load(new File("products.csv"), typeMap);
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param columnTypeMap mapping of column names to their Types
     * @return a Dataset with explicitly typed columns
     * @throws IllegalArgumentException if columnTypeMap is {@code null} or empty
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(File, Collection, long, long, Predicate, Map)
     */
    public static Dataset load(final File source, final Map<String, ? extends Type<?>> columnTypeMap) throws UncheckedIOException {
        return load(source, null, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     * Loads CSV data from a file with specified column selection and provided column-to-type mapping.
     * Columns present in {@code columnTypeMap} will be parsed into the specified {@link Type};
     * Other columns default to {@link String}. 
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String,  ? extends Type<?>> typeMap = new HashMap<>();
     * typeMap.put("id", Type.of(Long.class));
     * // typeMap.put("name", Type.of(String.class)); // String is default
     * typeMap.put("price", Type.of(Double.class));
     * typeMap.put("active", Type.of(Boolean.class));
     * 
     * Dataset ds = CsvUtil.load(new File("products.csv"), Arrays.asList("id", "name", "price", "active"), 1000, 1000, typeMap);
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param columnTypeMap mapping of column names to their Types
     * @return a Dataset with typed columns
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(File, Collection, long, long, Predicate, Map)
     */
    public static Dataset load(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Map<String, ? extends Type<?>> columnTypeMap) throws UncheckedIOException {
        return load(source, selectColumnNames, offset, count, Fn.alwaysTrue(), columnTypeMap);
    }

    /**
     * Loads CSV data from a file with specified column selection, pagination, row filtering and provided column-to-type mapping.
     * Columns present in {@code columnTypeMap} will be parsed into the specified {@link Type};
     * Other columns default to {@link String}. 
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String,  ? extends Type<?>> typeMap = new HashMap<>();
     * typeMap.put("id", Type.of(Long.class));
     * // typeMap.put("name", Type.of(String.class)); // String is default
     * typeMap.put("price", Type.of(Double.class));
     * typeMap.put("active", Type.of(Boolean.class));
     * 
     * // Load only products with active status
     * Predicate<String[]> activeFilter = row -> Boolean.parseBoolean(row[3]);
     * Dataset ds = CsvUtil.load(new File("products.csv"), Arrays.asList("id", "name", "price", "active"), 1000, 1000, activeFilter, typeMap);
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowFilter a Predicate to filter rows, only matching rows are included
     * @param columnTypeMap mapping of column names to their Types
     * @return a Dataset with typed and filtered data
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static Dataset load(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter, final Map<String, ? extends Type<?>> columnTypeMap) throws UncheckedIOException {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return load(reader, selectColumnNames, offset, count, rowFilter, columnTypeMap);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     * Loads CSV data from a Reader with all columns included and provided column-to-type mapping.
     * Columns present in {@code columnTypeMap} will be parsed into the specified {@link Type};
     * all other columns default to {@link String}. 
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String,  ? extends Type<?>> typeMap = new HashMap<>();
     * typeMap.put("id", Type.of(Long.class));
     * // typeMap.put("name", Type.of(String.class)); // String is default
     * typeMap.put("price", Type.of(Double.class));
     * typeMap.put("active", Type.of(Boolean.class));
     * 
     * try (Reader reader = new FileReader("products.csv")) {
     *     // Load first 1000 products
     *     Dataset ds = CsvUtil.load(reader, typeMap);
     * }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param columnTypeMap mapping of column names to their Types
     * @return a Dataset with explicitly typed columns
     * @throws IllegalArgumentException if columnTypeMap is {@code null} or empty
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(Reader, Collection, long, long, Predicate, Map)
     */
    public static Dataset load(final Reader source, final Map<String, ? extends Type<?>> columnTypeMap) throws UncheckedIOException {
        return load(source, null, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     * Loads CSV data from a Reader with specified column selection and provided column-to-type mapping.
     * Columns present in {@code columnTypeMap} will be parsed into the specified {@link Type};
     * Other columns default to {@link String}. 
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String,  ? extends Type<?>> typeMap = new HashMap<>();
     * typeMap.put("id", Type.of(Long.class));
     * // typeMap.put("name", Type.of(String.class)); // String is default
     * typeMap.put("price", Type.of(Double.class));
     * typeMap.put("active", Type.of(Boolean.class));
     * 
     * try (Reader reader = new FileReader("products.csv")) {
     *     // Load first 1000 products
     *     Dataset ds = CsvUtil.load(reader, Arrays.asList("id", "name", "price", "active"), 1000, 1000, typeMap);
     *  }
     * }</pre>
     *
     * @param source the Reader providing CSV data
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param columnTypeMap mapping of column names to their Types
     * @return a Dataset with typed columns
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(Reader, Collection, long, long, Predicate, Map)
     */
    public static Dataset load(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Map<String, ? extends Type<?>> columnTypeMap) throws UncheckedIOException {
        return load(source, selectColumnNames, offset, count, Fn.alwaysTrue(), columnTypeMap);
    }

    /**
     * Loads CSV data from a Reader with specified column selection, pagination, row filtering and provided column-to-type mapping.
     * Columns present in {@code columnTypeMap} will be parsed into the specified {@link Type};
     * Other columns default to {@link String}. 
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String,  ? extends Type<?>> typeMap = new HashMap<>();
     * typeMap.put("id", Type.of(Long.class));
     * // typeMap.put("name", Type.of(String.class)); // String is default
     * typeMap.put("price", Type.of(Double.class));
     * typeMap.put("active", Type.of(Boolean.class));
     * 
     * // Load only products with active status
     * Predicate<String[]> activeFilter = row -> Boolean.parseBoolean(row[3]);
     *
     * try (Reader reader = new FileReader("products.csv")) {
     *      Dataset ds = CsvUtil.load(reader, Arrays.asList("id", "name", "price", "active"), 1000, 1000, activeFilter, typeMap);
     * }
     * }</pre> 
     *
     * @param source the Reader providing CSV data
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowFilter a Predicate to filter rows, only matching rows are included
     * @param columnTypeMap mapping of column names to their Types
     * @return a Dataset with typed and filtered data
     * @throws IllegalArgumentException if parameters are invalid or columnTypeMap is empty
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    public static Dataset load(final Reader source, final Collection<String> selectColumnNames, long offset, long count,
            final Predicate<? super String[]> rowFilter, final Map<String, ? extends Type<?>> columnTypeMap)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s cannot be negative", offset, count);

        if (N.isEmpty(columnTypeMap)) {
            throw new IllegalArgumentException("columnTypeMap cannot be null or empty");
        }

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);

        try {
            String line = br.readLine();

            if (line == null) {
                return N.newEmptyDataset();
            }

            final String[] titles = headerParser.apply(line);
            final int columnCount = titles.length;

            final boolean noSelectColumnNamesSpecified = N.isEmpty(selectColumnNames)
                    || (selectColumnNames.size() == columnCount && selectColumnNames.containsAll(Arrays.asList(titles)));
            final Set<String> selectPropNameSet = noSelectColumnNamesSpecified ? null : N.newHashSet(selectColumnNames);
            final int selectColumnCount = noSelectColumnNamesSpecified ? columnCount : selectPropNameSet.size();

            final List<String> columnNameList = new ArrayList<>(selectColumnCount);
            final List<List<Object>> columnList = new ArrayList<>(selectColumnCount);
            // final boolean[] isColumnSelected = noSelectColumnNamesSpecified ? null : new boolean[columnCount];
            final Type<?>[] columnTypes = new Type<?>[columnCount];
            final Map<String, Type<?>> columnTypeMapToUse = columnTypeMap == null ? N.emptyMap() : (Map<String, Type<?>>) columnTypeMap;

            if (noSelectColumnNamesSpecified) {
                columnNameList.addAll(Arrays.asList(titles));

                for (int i = 0; i < columnCount; i++) {
                    columnTypes[i] = columnTypeMapToUse.getOrDefault(titles[i], strType);
                    columnList.add(new ArrayList<>());
                }
            } else {
                for (int i = 0; i < columnCount; i++) {
                    if (selectPropNameSet.remove(titles[i])) {
                        columnNameList.add(titles[i]);
                        columnTypes[i] = columnTypeMapToUse.getOrDefault(titles[i], strType);
                        // isColumnSelected[i] = true;
                        columnList.add(new ArrayList<>());
                    }
                }

                if (N.notEmpty(selectPropNameSet)) {
                    throw new IllegalArgumentException(selectColumnNames + " are not included in titles: " + N.toString(titles));
                }
            }

            while (offset-- > 0 && br.readLine() != null) { // NOSONAR
                // continue
            }

            final String[] row = new String[columnCount];

            while (count-- > 0 && (line = br.readLine()) != null) {
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
            }

            return new RowDataset(columnNameList, columnList);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = 
     *     (columns, row, output) -> {
     *         output[0] = row.get(0);   // name as String
     *         output[1] = Integer.parseInt(row.get(1));   // age as int
     *         output[2] = LocalDate.parse(row.get(2));   // date as LocalDate
     *     };
     * 
     * Dataset ds = CsvUtil.load(new File("data.csv"), extractor);
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param rowExtractor custom logic to extract and convert row data
     * @return a Dataset with custom extracted data
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static Dataset load(final File source,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return load(source, null, rowExtractor);
    }

    /**
     * Loads CSV data from a file with column selection and custom row extraction.
     * Only selected columns are passed to the row extractor.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = 
     *     (columns, row, output) -> {
     *         // Process only selected columns
     *         output[0] = row.get(0).toUpperCase();
     *         output[1] = Double.parseDouble(row.get(1)) * 1.1;  // Add 10%
     *     };
     * 
     * Dataset ds = CsvUtil.load(
     *     new File("prices.csv"),
     *     Arrays.asList("product", "price"),
     *     extractor
     * );
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param rowExtractor custom logic to extract and convert row data
     * @return a Dataset with custom extracted data
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static Dataset load(final File source, final Collection<String> selectColumnNames,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return load(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), rowExtractor);
    }

    /**
     * Loads CSV data from a file with pagination and custom row extraction.
     * Allows processing a subset of rows with custom logic.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = 
     *     (columns, row, output) -> {
     *         // Custom parsing logic
     *         output[0] = parseCustomFormat(row.get(0));
     *         output[1] = validateAndConvert(row.get(1));
     *     };
     * 
     * // Process rows 100-199
     * Dataset ds = CsvUtil.load(new File("large.csv"), 100, 100, extractor);
     * }</pre>
     *
     * @param source the File containing CSV data
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowExtractor custom logic to extract and convert row data
     * @return a Dataset with custom extracted data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static Dataset load(final File source, final long offset, final long count,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return load(source, null, offset, count, Fn.alwaysTrue(), rowExtractor);
    }

    /**
     * Loads CSV data from a File source with specified offset, count, row filter, and column type list.
     *
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowFilter a Predicate to filter rows, only matching rows are included
     * @param rowExtractor a TriConsumer to extract the row data to the output array.
     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array.
     * @return a Dataset containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static Dataset load(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return load(reader, selectColumnNames, offset, count, rowFilter, rowExtractor);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     * Loads CSV data from a Reader source using a custom row extractor function.
     *
     * <p>This method provides maximum flexibility for custom data extraction and transformation.
     * The row extractor function receives the column names, row data, and an output array,
     * allowing you to implement custom type conversion, data validation, or transformation logic.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new FileReader("data.csv");
     * Dataset result = CsvUtil.load(reader, (columnNames, rowData, output) -> {
     *     // Custom extraction logic
     *     output[0] = Integer.parseInt(rowData.get(0));
     *     output[1] = rowData.get(1).toUpperCase();
     * });
     * }</pre>
     *
     * @param source the Reader source to read CSV data from
     * @param rowExtractor a TriConsumer to extract the row data to the output array.
     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array.
     * @return a Dataset containing the loaded CSV data
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(Reader, Collection, TriConsumer)
     */
    public static Dataset load(final Reader source,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return load(source, null, rowExtractor);
    }

    /**
     * Loads CSV data from a Reader source with column selection and a custom row extractor function.
     *
     * <p>This method allows you to select specific columns from the CSV file while using a custom
     * row extractor for flexible data processing. Only the selected columns will be included in
     * the resulting Dataset.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new FileReader("employees.csv");
     * List<String> columns = Arrays.asList("name", "department", "salary");
     * Dataset result = CsvUtil.load(reader, columns, (columnNames, rowData, output) -> {
     *     output[0] = rowData.get(0);   // name
     *     output[1] = rowData.get(1);   // department
     *     output[2] = Double.parseDouble(rowData.get(2));   // salary as double
     * });
     * }</pre>
     *
     * @param source the Reader source to read CSV data from
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param rowExtractor a TriConsumer to extract the row data to the output array.
     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array.
     * @return a Dataset containing the loaded CSV data with selected columns
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(Reader, TriConsumer)
     * @see #load(Reader, Collection, long, long, Predicate, TriConsumer)
     */
    public static Dataset load(final Reader source, final Collection<String> selectColumnNames,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return load(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), rowExtractor);
    }

    /**
     * Loads CSV data from a Reader source with pagination and a custom row extractor function.
     *
     * <p>This method supports pagination through the {@code offset} and {@code count} parameters,
     * allowing you to skip initial rows and limit the number of rows loaded. This is useful for
     * processing large CSV files in chunks or implementing pagination in data viewing applications.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new FileReader("large-dataset.csv");
     * // Load rows 101-200 (skip first 100, load next 100)
     * Dataset result = CsvUtil.load(reader, 100, 100, (columnNames, rowData, output) -> {
     *     // Custom extraction for each row
     *     for (int i = 0; i < rowData.size(); i++) {
     *         output[i] = rowData.get(i);
     *     }
     * });
     * }</pre>
     *
     * @param source the Reader source to read CSV data from
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowExtractor a TriConsumer to extract the row data to the output array.
     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array.
     * @return a Dataset containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     * @see #load(Reader, TriConsumer)
     * @see #load(Reader, Collection, long, long, Predicate, TriConsumer)
     */
    public static Dataset load(final Reader source, final long offset, final long count,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor) throws UncheckedIOException {
        return load(source, null, offset, count, Fn.alwaysTrue(), rowExtractor);
    }

    /**
     * Loads CSV data from a Reader source with specified offset, count, row filter, and column type list.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowFilter a Predicate to filter rows, only matching rows are included
     * @param rowExtractor a TriConsumer to extract the row data to the output array.
     *      The first parameter is the column names, the second parameter is the row data, and the third parameter is the output array. 
     * @return a Dataset containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    public static Dataset load(final Reader source, final Collection<String> selectColumnNames, long offset, long count,
            final Predicate<? super String[]> rowFilter,
            final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s cannot be negative", offset, count);

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);

        try {
            String line = br.readLine();

            if (line == null) {
                return N.newEmptyDataset();
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

            while (count-- > 0 && (line = br.readLine()) != null) {
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
            }

            return new RowDataset(columnNameList, columnList);
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
     * This method provides lazy evaluation of CSV data, reading rows on-demand rather than
     * loading the entire file into memory. The stream will automatically close the underlying
     * file reader when the stream is closed.
     *
     * <p>The target type can be:
     * <ul>
     *   <li>A bean class - rows are converted to bean instances</li>
     *   <li>Map - rows are converted to Map&lt;String, Object&gt;</li>
     *   <li>Collection - rows are converted to collections of column values</li>
     *   <li>Object[] - rows are converted to object arrays</li>
     *   <li>A single-column primitive type - when only one column is selected</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Stream as bean objects
     * try (Stream<Person> stream = CsvUtil.stream(new File("people.csv"), Person.class)) {
     *     stream.filter(p -> p.getAge() > 18)
     *           .forEach(System.out::println);
     * }
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param targetType the Class of the target type
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the target type is {@code null} or not supported
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @see #stream(File, Collection, Class)
     * @see #stream(File, Collection, long, long, Predicate, Class)
     * @see #stream(Reader, Class, boolean)
     */
    public static <T> Stream<T> stream(final File source, final Class<? extends T> targetType) {
        return stream(source, null, targetType);
    }

    /**
     * Streams CSV data from a File source with specified column names and target type.
     * This method provides lazy evaluation with column selection - only specified columns
     * are included in the streamed objects.
     *
     * <p>The stream automatically closes the underlying file reader when closed.
     * For bean types, only properties matching the selected column names are populated.
     * Non-selected columns are ignored during processing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Stream only name and age columns as Person beans
     * try (Stream<Person> stream = CsvUtil.stream(
     *         new File("people.csv"),
     *         Arrays.asList("name", "age"),
     *         Person.class)) {
     *     List<Person> adults = stream.filter(p -> p.getAge() >= 18)
     *                                 .collect(Collectors.toList());
     * }
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param targetType the Class of the target type
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the target type is {@code null} or not supported, or if selected columns are not found in CSV
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @see #stream(File, Class)
     * @see #stream(File, Collection, long, long, Predicate, Class)
     * @see #stream(Reader, Collection, Class, boolean)
     */
    public static <T> Stream<T> stream(final File source, final Collection<String> selectColumnNames, final Class<? extends T> targetType) {
        return stream(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), targetType);
    }

    /**
     * Streams CSV data from a File source with full control over column selection, pagination, and filtering.
     * This is the most comprehensive streaming method providing lazy evaluation with all configuration options.
     *
     * <p>The stream automatically closes the underlying file reader when closed. This method supports:
     * <ul>
     *   <li>Column selection - include only specified columns</li>
     *   <li>Pagination - skip initial rows and limit total rows processed</li>
     *   <li>Row filtering - include only rows matching the predicate</li>
     *   <li>Type conversion - convert to beans, maps, collections, or arrays</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Stream active employees over 30, skip first 100, limit to 500
     * Predicate<String[]> activeFilter = row -> "ACTIVE".equals(row[3]);
     * try (Stream<Employee> stream = CsvUtil.stream(
     *         new File("employees.csv"),
     *         Arrays.asList("id", "name", "age", "status"),
     *         100, 500,
     *         activeFilter,
     *         Employee.class)) {
     *     stream.filter(e -> e.getAge() > 30)
     *           .forEach(System.out::println);
     * }
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowFilter a Predicate to filter rows, only matching rows are included
     * @param targetType the Class of the target type
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the target type is {@code null} or not supported
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @see #stream(File, Class)
     * @see #stream(File, Collection, Class)
     * @see #stream(Reader, Collection, long, long, Predicate, Class, boolean)
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
     * This method provides lazy evaluation of CSV data from a Reader, with control over
     * whether the reader should be closed when the stream terminates.
     *
     * <p>This method is useful when you have an existing Reader (e.g., from a network stream,
     * compressed file, or other source) and want to stream its CSV content. Set
     * {@code closeReaderWhenStreamIsClosed} to {@code true} if the stream should own the reader
     * lifecycle, or {@code false} if you'll manage the reader externally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Stream from a StringReader
     * String csvData = "name,age\nJohn,30\nJane,25";
     * Reader reader = new StringReader(csvData);
     * try (Stream<Person> stream = CsvUtil.stream(reader, Person.class, true)) {
     *     stream.forEach(System.out::println);
     * } // reader is closed automatically
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param targetType the Class of the target type
     * @param closeReaderWhenStreamIsClosed {@code true} to close the reader when the stream is closed, {@code false} otherwise
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the target type is {@code null} or not supported
     * @throws UncheckedIOException if an I/O error occurs while reading
     * @see #stream(Reader, Collection, Class, boolean)
     * @see #stream(Reader, Collection, long, long, Predicate, Class, boolean)
     * @see #stream(File, Class)
     */
    public static <T> Stream<T> stream(final Reader source, final Class<? extends T> targetType, final boolean closeReaderWhenStreamIsClosed) {
        return stream(source, null, targetType, closeReaderWhenStreamIsClosed);
    }

    /**
     * Streams CSV data from a Reader source with specified column names and target type.
     * This method provides lazy evaluation with column selection from a Reader source.
     *
     * <p>Only the specified columns are included in the streamed objects. For bean types,
     * only properties matching the selected column names are populated. The reader lifecycle
     * can be controlled via the {@code closeReaderWhenStreamIsClosed} parameter.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Stream selected columns from a Reader
     * try (Reader reader = new FileReader("employees.csv");
     *      Stream<Map<String, Object>> stream = CsvUtil.stream(
     *          reader,
     *          Arrays.asList("name", "department"),
     *          Map.class,
     *          true)) {
     *     stream.limit(10).forEach(System.out::println);
     * }
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param targetType the Class of the target type
     * @param closeReaderWhenStreamIsClosed {@code true} to close the reader when the stream is closed, {@code false} otherwise
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the target type is {@code null} or not supported, or if selected columns are not found
     * @throws UncheckedIOException if an I/O error occurs while reading
     * @see #stream(Reader, Class, boolean)
     * @see #stream(Reader, Collection, long, long, Predicate, Class, boolean)
     * @see #stream(File, Collection, Class)
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames, final Class<? extends T> targetType,
            final boolean closeReaderWhenStreamIsClosed) {
        return stream(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), targetType, closeReaderWhenStreamIsClosed);
    }

    /**
     * Streams CSV data from a Reader source with complete control over all streaming parameters.
     * This is the most comprehensive Reader-based streaming method providing lazy evaluation with
     * full configuration options including pagination, filtering, and column selection.
     *
     * <p>This method supports:
     * <ul>
     *   <li>Column selection - include only specified columns</li>
     *   <li>Pagination - skip initial rows and limit total rows processed</li>
     *   <li>Row filtering - include only rows matching the predicate</li>
     *   <li>Type conversion - convert to beans, maps, collections, or arrays</li>
     *   <li>Reader lifecycle management - optionally close reader with stream</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Stream with all options
     * try (Reader reader = new FileReader("sales.csv")) {
     *     Predicate<String[]> highValueFilter = row -> Double.parseDouble(row[2]) > 1000;
     *     try (Stream<Sale> stream = CsvUtil.stream(
     *             reader,
     *             Arrays.asList("date", "product", "amount"),
     *             100, 500,  // skip 100, take 500
     *             highValueFilter,
     *             Sale.class,
     *             true)) {
     *         stream.forEach(System.out::println);
     *     }
     * }
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowFilter a Predicate to filter rows, only matching rows are included
     * @param targetType the Class of the target type
     * @param closeReaderWhenStreamIsClosed whether to close the Reader when the stream is closed
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the target type is {@code null} or not supported.
     * @throws UncheckedIOException if an I/O error occurs while reading
     * @see #stream(Reader, Class, boolean)
     * @see #stream(Reader, Collection, Class, boolean)
     * @see #stream(File, Collection, long, long, Predicate, Class)
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter, final Class<? extends T> targetType, final boolean closeReaderWhenStreamIsClosed)
            throws IllegalArgumentException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s cannot be negative", offset, count);
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
     * Streams CSV data from a File source with custom row mapping logic.
     * This method provides lazy evaluation with a custom row mapper function that receives
     * column names and row data, allowing complete control over how rows are converted.
     *
     * <p>The row mapper receives:
     * <ul>
     *   <li>Column names as an immutable List&lt;String&gt;</li>
     *   <li>Row data as a DisposableArray&lt;String&gt; (reused for efficiency)</li>
     * </ul>
     *
     * <p>The stream automatically closes the underlying file reader when closed.
     * The DisposableArray is reused across rows for performance, so extract all needed
     * data within the mapper function.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Custom row mapping to create DTOs
     * try (Stream<PersonDTO> stream = CsvUtil.stream(
     *         new File("people.csv"),
     *         (columns, row) -> {
     *             PersonDTO dto = new PersonDTO();
     *             dto.setFullName(row.get(0) + " " + row.get(1));   // Combine first and last
     *             dto.setAge(Integer.parseInt(row.get(2)));
     *             return dto;
     *         })) {
     *     stream.forEach(System.out::println);
     * }
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param rowMapper converts the row data to the target type.
     *                  First parameter is the column names, second parameter is the row data.
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the rowMapper is null
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @see #stream(File, Collection, BiFunction)
     * @see #stream(File, Collection, long, long, Predicate, BiFunction)
     * @see #stream(Reader, BiFunction, boolean)
     */
    public static <T> Stream<T> stream(final File source,
            final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper) {
        return stream(source, null, rowMapper);
    }

    /**
     * Streams CSV data from a File source with column selection and custom row mapping.
     * This method provides lazy evaluation with both column filtering and custom conversion logic.
     *
     * <p>Only the specified columns are passed to the row mapper. The mapper receives:
     * <ul>
     *   <li>Selected column names as an immutable List&lt;String&gt;</li>
     *   <li>Selected column values as a DisposableArray&lt;String&gt; (reused for efficiency)</li>
     * </ul>
     *
     * <p>The stream automatically closes the underlying file reader when closed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map selected columns with custom logic
     * try (Stream<Summary> stream = CsvUtil.stream(
     *         new File("sales.csv"),
     *         Arrays.asList("product", "revenue", "quantity"),
     *         (columns, row) -> {
     *             Summary s = new Summary();
     *             s.setProduct(row.get(0));
     *             s.setRevenue(Double.parseDouble(row.get(1)));
     *             s.setQuantity(Integer.parseInt(row.get(2)));
     *             return s;
     *         })) {
     *     stream.forEach(System.out::println);
     * }
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param rowMapper converts the row data to the target type.
     *                  First parameter is the column names, second parameter is the row data.
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the rowMapper is {@code null} or selected columns are not found
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @see #stream(File, BiFunction)
     * @see #stream(File, Collection, long, long, Predicate, BiFunction)
     * @see #stream(Reader, Collection, BiFunction, boolean)
     */
    public static <T> Stream<T> stream(final File source, final Collection<String> selectColumnNames,
            final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper) {
        return stream(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), rowMapper);
    }

    /**
     * Streams CSV data from a File source with full control including custom row mapping.
     * This is the most comprehensive File-based streaming method with custom mapping, providing
     * lazy evaluation with pagination, filtering, column selection, and custom conversion logic.
     *
     * <p>This method supports:
     * <ul>
     *   <li>Column selection - include only specified columns</li>
     *   <li>Pagination - skip initial rows and limit total rows processed</li>
     *   <li>Row filtering - include only rows matching the predicate</li>
     *   <li>Custom mapping - complete control over row-to-object conversion</li>
     * </ul>
     *
     * <p>The stream automatically closes the underlying file reader when closed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Stream with custom mapper, filtering, and pagination
     * Predicate<String[]> validFilter = row -> !row[0].isEmpty();
     * try (Stream<CustomDTO> stream = CsvUtil.stream(
     *         new File("data.csv"),
     *         Arrays.asList("id", "name", "value"),
     *         100, 500,  // skip 100, take 500
     *         validFilter,
     *         (columns, row) -> new CustomDTO(row.get(0), row.get(1), row.get(2)))) {
     *     stream.forEach(System.out::println);
     * }
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowFilter a Predicate to filter rows, only matching rows are included
     * @param rowMapper converts the row data to the target type.
     *                  First parameter is the column names, second parameter is the row data.
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the rowMapper is null
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @see #stream(File, BiFunction)
     * @see #stream(File, Collection, BiFunction)
     * @see #stream(Reader, Collection, long, long, Predicate, BiFunction, boolean)
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
     * Streams CSV data from a Reader source with custom row mapping logic.
     * This method provides lazy evaluation with a custom row mapper and control over
     * the reader lifecycle.
     *
     * <p>The row mapper receives:
     * <ul>
     *   <li>Column names as an immutable List&lt;String&gt;</li>
     *   <li>Row data as a DisposableArray&lt;String&gt; (reused for efficiency)</li>
     * </ul>
     *
     * <p>The DisposableArray is reused across rows for performance, so extract all needed
     * data within the mapper function.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String csvData = "name,age,city\nJohn,30,NYC\nJane,25,LA";
     * try (Reader reader = new StringReader(csvData);
     *      Stream<Person> stream = CsvUtil.stream(
     *          reader,
     *          (columns, row) -> new Person(row.get(0), Integer.parseInt(row.get(1)), row.get(2)),
     *          true)) {
     *     stream.forEach(System.out::println);
     * }
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param rowMapper converts the row data to the target type.
     *                  First parameter is the column names, second parameter is the row data.
     * @param closeReaderWhenStreamIsClosed {@code true} to close the reader when the stream is closed, {@code false} otherwise
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the rowMapper is null
     * @throws UncheckedIOException if an I/O error occurs while reading
     * @see #stream(Reader, Collection, BiFunction, boolean)
     * @see #stream(Reader, Collection, long, long, Predicate, BiFunction, boolean)
     * @see #stream(File, BiFunction)
     */
    public static <T> Stream<T> stream(final Reader source,
            final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper,
            final boolean closeReaderWhenStreamIsClosed) {
        return stream(source, null, rowMapper, closeReaderWhenStreamIsClosed);
    }

    /**
     * Streams CSV data from a Reader source with column selection and custom row mapping.
     * This method provides lazy evaluation with both column filtering and custom conversion logic
     * from a Reader source.
     *
     * <p>Only the specified columns are passed to the row mapper. The mapper receives:
     * <ul>
     *   <li>Selected column names as an immutable List&lt;String&gt;</li>
     *   <li>Selected column values as a DisposableArray&lt;String&gt; (reused for efficiency)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("products.csv");
     *      Stream<Product> stream = CsvUtil.stream(
     *          reader,
     *          Arrays.asList("id", "name", "price"),
     *          (columns, row) -> new Product(
     *              Long.parseLong(row.get(0)),
     *              row.get(1),
     *              new BigDecimal(row.get(2))),
     *          true)) {
     *     stream.forEach(System.out::println);
     * }
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param rowMapper converts the row data to the target type.
     *                  First parameter is the column names, second parameter is the row data.
     * @param closeReaderWhenStreamIsClosed {@code true} to close the reader when the stream is closed, {@code false} otherwise
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the rowMapper is {@code null} or selected columns are not found
     * @throws UncheckedIOException if an I/O error occurs while reading
     * @see #stream(Reader, BiFunction, boolean)
     * @see #stream(Reader, Collection, long, long, Predicate, BiFunction, boolean)
     * @see #stream(File, Collection, BiFunction)
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames,
            final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper,
            final boolean closeReaderWhenStreamIsClosed) {
        return stream(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), rowMapper, closeReaderWhenStreamIsClosed);
    }

    /**
     * Streams CSV data from a Reader source with complete control including custom row mapping.
     * This is the most comprehensive Reader-based streaming method with custom mapping, providing
     * lazy evaluation with all configuration options: pagination, filtering, column selection,
     * custom conversion, and reader lifecycle management.
     *
     * <p>This method supports:
     * <ul>
     *   <li>Column selection - include only specified columns</li>
     *   <li>Pagination - skip initial rows and limit total rows processed</li>
     *   <li>Row filtering - include only rows matching the predicate</li>
     *   <li>Custom mapping - complete control over row-to-object conversion</li>
     *   <li>Reader lifecycle management - optionally close reader with stream</li>
     * </ul>
     *
     * <p>The DisposableArray is reused across rows for performance, so extract all needed
     * data within the mapper function.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("transactions.csv")) {
     *     Predicate<String[]> validFilter = row -> row[0] != null && !row[0].isEmpty();
     *     try (Stream<Transaction> stream = CsvUtil.stream(
     *             reader,
     *             Arrays.asList("id", "amount", "date"),
     *             100, 1000,  // skip 100, take 1000
     *             validFilter,
     *             (columns, row) -> new Transaction(
     *                 UUID.fromString(row.get(0)),
     *                 new BigDecimal(row.get(1)),
     *                 LocalDate.parse(row.get(2))),
     *             true)) {
     *         stream.forEach(System.out::println);
     *     }
     * }
     * }</pre>
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select, {@code null} to include all columns
     * @param offset the number of data rows to skip from the beginning (after header)
     * @param count the maximum number of rows to process
     * @param rowFilter a Predicate to filter rows, only matching rows are included
     * @param rowMapper converts the row data to the target type.
     *                  First parameter is the column names, second parameter is the row data.
     * @param closeReaderWhenStreamIsClosed whether to close the Reader when the stream is closed
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the rowMapper is null
     * @throws UncheckedIOException if an I/O error occurs while reading
     * @see #stream(Reader, BiFunction, boolean)
     * @see #stream(Reader, Collection, BiFunction, boolean)
     * @see #stream(File, Collection, long, long, Predicate, BiFunction)
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter,
            final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper,
            final boolean closeReaderWhenStreamIsClosed) throws IllegalArgumentException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s cannot be negative", offset, count);

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert employees.csv to employees.json
     * CsvUtil.csvToJson(
     *     new File("data/employees.csv"),
     *     new File("output/employees.json")
     * );
     * }</pre>
     *
     * <p>Example CSV input:</p>
     * <pre>{@code
     * Name,Age,Department
     * John,30,Engineering
     * Jane,25,Marketing
     * }</pre>
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
     * @see #csvToJson(File, Collection, File)
     * @see #csvToJson(File, Collection, File, Class)
     */
    public static long csvToJson(final File csvFile, final File jsonFile) throws UncheckedIOException {
        return csvToJson(csvFile, null, jsonFile);
    }

    /**
     * Converts a CSV file to JSON format with selected columns.
     * This method allows you to specify which columns from the CSV file should be
     * included in the JSON output. All values are treated as strings in the output.
     *
     * <p>The CSV file's first line is treated as column headers. Only the columns
     * specified in {@code selectColumnNames} will be included in the JSON output.
     * If {@code selectColumnNames} is {@code null}, all columns will be included.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert only specific columns from employees.csv to JSON
     * CsvUtil.csvToJson(
     *     new File("data/employees.csv"),
     *     Arrays.asList("Name", "Department"),
     *     new File("output/employees_filtered.json")
     * );
     * }</pre>
     *
     * <p>Example CSV input:</p>
     * <pre>{@code
     * Name,Age,Department,Salary
     * John,30,Engineering,75000
     * Jane,25,Marketing,65000
     * }</pre>
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
     *                         {@code null} to include all columns
     * @param jsonFile the destination JSON file to create
     * @return the number of rows written to the JSON file
     * @throws IllegalArgumentException if csvFile or jsonFile is null
     * @throws UncheckedIOException if an I/O error occurs during file operations
     * @see #csvToJson(File, File)
     * @see #csvToJson(File, Collection, File, Class)
     */
    public static long csvToJson(final File csvFile, final Collection<String> selectColumnNames, final File jsonFile) throws UncheckedIOException {
        return csvToJson(csvFile, selectColumnNames, jsonFile, null);
    }

    /**
     * Converts a CSV file to JSON format with selected columns and type conversion.
     * This method allows you to specify which columns to include and provides type conversion
     * based on a bean class definition. Values are converted from strings to their appropriate
     * types as defined by the bean class properties.
     *
     * <p>The CSV file's first line is treated as column headers. Only the columns
     * specified in {@code selectColumnNames} will be included in the JSON output.
     * The {@code beanClassForColumnTypeInference} parameter defines how each column should be
     * typed in the JSON output - properties of the bean class determine the target types.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class Employee {
     *     private String name;
     *     private Integer age;
     *     private String department;
     *     // getters and setters...
     * }
     *
     * // Convert with type conversion - age will be numeric in JSON
     * CsvUtil.csvToJson(
     *     new File("data/employees.csv"),
     *     Arrays.asList("name", "age", "department"),
     *     new File("output/employees_typed.json"),
     *     Employee.class
     * );
     * }</pre>
     *
     * <p>Example CSV input:</p>
     * <pre>{@code
     * name,age,department,salary
     * John,30,Engineering,75000
     * Jane,25,Marketing,65000
     * }</pre>
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
     *                         {@code null} to include all columns
     * @param jsonFile the destination JSON file to create
     * @param beanClassForColumnTypeInference the bean class defining property types for conversion,
     *                               {@code null} to treat all values as strings
     * @return the number of rows written to the JSON file                              
     * @throws IllegalArgumentException if csvFile or jsonFile is null
     * @throws UncheckedIOException if an I/O error occurs during file operations
     * @see #csvToJson(File, File)
     * @see #csvToJson(File, Collection, File)
     */
    public static long csvToJson(final File csvFile, final Collection<String> selectColumnNames, final File jsonFile,
            final Class<?> beanClassForColumnTypeInference) throws UncheckedIOException {
        N.checkArgNotNull(csvFile, "csvFile");
        N.checkArgNotNull(jsonFile, "jsonFile");

        try (Reader csvReader = IOUtil.newFileReader(csvFile);
             Writer jsonWriter = IOUtil.newFileWriter(jsonFile)) {
            return csvToJson(csvReader, selectColumnNames, jsonWriter, beanClassForColumnTypeInference);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Converts CSV data from a Reader to JSON format and writes it to a Writer.
     * This method provides streaming conversion of CSV data to JSON with optional
     * column selection and type conversion based on a bean class.
     *
     * <p>The CSV reader's first line is treated as column headers. If {@code selectColumnNames}
     * is provided, only those columns will be included in the JSON output. If
     * {@code beanClassForColumnTypeInference} is provided, values are converted to their
     * appropriate types as defined by the bean class properties.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader csvReader = new FileReader("data.csv");
     *      Writer jsonWriter = new FileWriter("data.json")) {
     *     long rowCount = CsvUtil.csvToJson(
     *         csvReader,
     *         Arrays.asList("name", "age"),
     *         jsonWriter,
     *         Person.class
     *     );
     *     System.out.println("Converted " + rowCount + " rows");
     * }
     * }</pre>
     *
     * @param csvReader the Reader providing CSV data
     * @param selectColumnNames the collection of column names to include in JSON output,
     *                         {@code null} to include all columns
     * @param jsonWriter the Writer to write JSON output to
     * @param beanClassForColumnTypeInference the bean class defining property types for conversion,
     *                               {@code null} to treat all values as strings
     * @return the number of rows written to the JSON output
     * @throws UncheckedIOException if an I/O error occurs during reading or writing
     * @see #csvToJson(File, File)
     * @see #csvToJson(File, Collection, File)
     * @see #csvToJson(File, Collection, File, Class)
     */
    public static long csvToJson(final Reader csvReader, final Collection<String> selectColumnNames, final Writer jsonWriter,
            final Class<?> beanClassForColumnTypeInference) throws UncheckedIOException {
        return csvToJson(csvReader, selectColumnNames, 0, Long.MAX_VALUE, jsonWriter, beanClassForColumnTypeInference, true);
    }

    private static long csvToJson(final Reader csvReader, final Collection<String> selectColumnNames, long offset, long count, final Writer jsonWriter,
            final Class<?> beanClassForColumnTypeInference, final boolean canBeanClassForTypeWritingBeNull) throws UncheckedIOException {
        if (beanClassForColumnTypeInference == null && !canBeanClassForTypeWritingBeNull) {
            throw new IllegalArgumentException("'beanClassForColumnTypeInference' cannot be null");
        }

        try (final BufferedReader reader = IOUtil.newBufferedReader(csvReader); //
             final BufferedJsonWriter bw = Objectory.createBufferedJsonWriter(jsonWriter)) {
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

            if (beanClassForColumnTypeInference == null) {
                Arrays.fill(columnType, strType);
            } else {
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClassForColumnTypeInference);

                for (int i = 0; i < columnCount; i++) {
                    final PropInfo propInfo = beanInfo.getPropInfo(titles[i]);

                    if (propInfo == null || propInfo.type.equals(strType)) {
                        columnType[i] = strType;
                    } else {
                        columnType[i] = propInfo.type;
                    }
                }
            }

            while (offset-- > 0 && reader.readLine() != null) { // NOSONAR
                // continue
            }

            long cnt = 0;
            final String[] rowData = new String[columnCount];

            boolean firstRow = true;
            bw.write("[\n");

            while (count-- > 0 && (line = reader.readLine()) != null) {
                if (!firstRow) {
                    bw.write(",\n");
                } else {
                    firstRow = false;
                }

                N.fill(rowData, null);
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

            bw.flush();

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
     * @see #jsonToCsv(File, Collection, File)
     */
    public static long jsonToCsv(final File jsonFile, final File csvFile) throws UncheckedIOException {
        return jsonToCsv(jsonFile, null, csvFile);
    }

    /**
     * Converts a JSON file to CSV format with optional header selection.
     * This method reads a JSON file containing an array of objects and converts it to CSV format.
     * Each JSON object becomes a row in the CSV file, with object properties becoming columns.
     *
     * <p>The JSON file must contain an array of objects where each object represents a row.
     * If {@code selectCsvHeaders} is provided, only those properties will be included as columns
     * in the CSV output. If {@code selectCsvHeaders} is {@code null} or empty, all properties from the
     * first JSON object will be used as headers.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert with specific headers
     * CsvUtil.jsonToCsv(
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
     * <pre>{@code
     * "name","age","department"
     * "John",30,"Engineering"
     * "Jane",25,"Marketing"
     * }</pre>
     *
     * @param jsonFile the source JSON file to convert
     * @param selectCsvHeaders the collection of property names to include as CSV headers,
     *                        {@code null} or empty to include all properties from the first object
     * @param csvFile the destination CSV file to create
     * @return the number of rows written to the CSV file (including header row)
     * @throws IllegalArgumentException if jsonFile or csvFile is null
     * @throws UncheckedIOException if an I/O error occurs during file operations or if the JSON format is invalid
     * @see #jsonToCsv(File, File)
     */
    public static long jsonToCsv(final File jsonFile, final Collection<String> selectCsvHeaders, final File csvFile) throws UncheckedIOException {
        N.checkArgNotNull(csvFile, "csvFile");
        N.checkArgNotNull(jsonFile, "jsonFile");

        try (Reader jsonReader = IOUtil.newFileReader(jsonFile);
             Writer csvWriter = IOUtil.newFileWriter(csvFile)) {
            return jsonToCsv(jsonReader, selectCsvHeaders, csvWriter);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Converts JSON data from a Reader to CSV format and writes it to a Writer.
     * This method provides streaming conversion of JSON data to CSV with optional
     * header selection.
     *
     * <p>The JSON reader must provide an array of objects where each object represents a row.
     * If {@code selectCsvHeaders} is provided, only those properties will be included as columns
     * in the CSV output. If {@code selectCsvHeaders} is {@code null} or empty, all properties from
     * the first JSON object will be used as headers.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader jsonReader = new FileReader("data.json");
     *      Writer csvWriter = new FileWriter("data.csv")) {
     *     long rowCount = CsvUtil.jsonToCsv(
     *         jsonReader,
     *         Arrays.asList("name", "age", "department"),
     *         csvWriter
     *     );
     *     System.out.println("Converted " + rowCount + " rows");
     * }
     * }</pre>
     *
     * @param jsonReader the Reader providing JSON data (must be an array of objects)
     * @param selectCsvHeaders the collection of property names to include as CSV headers,
     *                        {@code null} or empty to include all properties from the first object
     * @param csvWriter the Writer to write CSV output to
     * @return the number of rows written to the CSV output (excluding header row)
     * @throws UncheckedIOException if an I/O error occurs during reading or writing,
     *         or if the JSON format is invalid
     * @see #jsonToCsv(File, File)
     * @see #jsonToCsv(File, Collection, File)
     */
    public static long jsonToCsv(final Reader jsonReader, final Collection<String> selectCsvHeaders, final Writer csvWriter) throws UncheckedIOException {
        return jsonToCsv(jsonReader, selectCsvHeaders, 0, Long.MAX_VALUE, csvWriter);
    }

    private static long jsonToCsv(final Reader jsonReader, final Collection<String> selectCsvHeaders, long offset, long count, final Writer csvWriter)
            throws UncheckedIOException {
        try (final Stream<Map<String, Object>> stream = jsonParser.stream(jsonReader, false, Type.ofMap(String.class, Object.class))
                .skip(offset < 0 ? 0 : offset)
                .limit(count <= 0 ? 0 : count); //
             final BufferedCsvWriter bw = Objectory.createBufferedCsvWriter(csvWriter)) {
            final List<String> headers = N.newArrayList(selectCsvHeaders);

            final char separator = WD._COMMA;
            Map<String, Object> row = null;
            long cnt = 0;

            @SuppressWarnings({ "deprecation" })
            final Iterator<Map<String, Object>> iter = stream.iterator();

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

                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                for (int i = 0; i < headSize; i++) {
                    if (i > 0) {
                        bw.write(separator);
                    }

                    writeField(bw, null, row.get(headers.get(i)));
                }

                while (iter.hasNext()) {
                    row = iter.next();

                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);

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

                bw.flush();
            }

            return cnt;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new {@link CSVLoader} instance for fluent-style CSV loading operations.
     * The loader provides a builder pattern for configuring and executing CSV loading operations
     * with customizable parsing options, column selection, filtering, and type conversion.
     *
     * <p>The CSVLoader supports configuration of:
     * <ul>
     *   <li>Custom header and line parsers</li>
     *   <li>Source file or reader</li>
     *   <li>Column selection</li>
     *   <li>Offset and count for pagination</li>
     *   <li>Row filtering</li>
     *   <li>Type conversion via bean class or column type map</li>
     *   <li>Custom row extraction logic</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Load CSV with type conversion
     * Dataset ds = CsvUtil.loader()
     *     .source(new File("data.csv"))
     *     .selectColumns(Arrays.asList("name", "age", "salary"))
     *     .beanClassForColumnTypeInference(Employee.class)
     *     .rowFilter(row -> row[2] != null)
     *     .offset(100)
     *     .count(500)
     *     .load();
     * }</pre>
     *
     * @return a new CSVLoader instance for configuring and executing CSV load operations
     * @see CSVLoader
     * @see #converter()
     */
    public static CSVLoader loader() {
        return new CSVLoader();
    }

    /**
     * Creates a new {@link CSVConverter} instance for fluent-style CSV conversion operations.
     * The converter provides a builder pattern for configuring and executing conversions
     * between CSV and JSON formats with customizable options.
     *
     * <p>The CSVConverter supports configuration of:
     * <ul>
     *   <li>Custom header and line parsers</li>
     *   <li>Source file or reader</li>
     *   <li>Column selection</li>
     *   <li>Offset and count for pagination</li>
     *   <li>Type conversion via bean class for JSON output</li>
     *   <li>Escape character configuration for writing</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert CSV to JSON with type conversion
     * long rowCount = CsvUtil.converter()
     *     .source(new File("data.csv"))
     *     .selectColumns(Arrays.asList("name", "age"))
     *     .beanClassForColumnTypeInference(Person.class)
     *     .csvToJson(new File("output.json"));
     *
     * // Convert JSON to CSV
     * long rowCount = CsvUtil.converter()
     *     .source(new File("data.json"))
     *     .selectColumns(Arrays.asList("name", "department"))
     *     .jsonToCsv(new File("output.csv"));
     * }</pre>
     *
     * @return a new CSVConverter instance for configuring and executing format conversions
     * @see CSVConverter
     * @see #loader()
     */
    public static CSVConverter converter() {
        return new CSVConverter();
    }

    static abstract class CSVCommon<This extends CSVCommon<This>> {
        protected Function<String, String[]> headerParser;
        protected BiConsumer<String, String[]> lineParser;
        protected Boolean escapeCharToBackSlashForWrite;
        protected File sourceFile;
        protected Reader sourceReader;
        protected Collection<String> selectColumnNames;
        protected long offset = 0;
        protected long count = Long.MAX_VALUE;
        protected Class<?> beanClassForColumnTypeInference;

        /**
         * Sets a custom header parser function for parsing the CSV header line.
         *
         * @param headerParser function that parses the header line into column names
         * @return this instance for method chaining
         * @throws IllegalArgumentException if headerParser is {@code null}
         */
        public This setHeaderParser(final Function<String, String[]> headerParser) {
            N.checkArgNotNull(headerParser, "headerParser");

            this.headerParser = headerParser;

            return (This) this;
        }

        /**
         * Sets a custom line parser for parsing CSV data lines.
         *
         * @param lineParser consumer that parses a line and populates the output array
         * @return this instance for method chaining
         * @throws IllegalArgumentException if lineParser is {@code null}
         */
        public This setLineParser(final BiConsumer<String, String[]> lineParser) {
            N.checkArgNotNull(lineParser, "lineParser");

            this.lineParser = lineParser;

            return (This) this;
        }

        /**
         * Configures the writer to use backslash as the escape character when writing CSV.
         *
         * @return this instance for method chaining
         */
        public This setEscapeCharToBackSlashForWrite() {
            this.escapeCharToBackSlashForWrite = true;

            return (This) this;
        }

        /**
         * Sets the source file for CSV/JSON operations.
         *
         * @param source the File to read CSV/JSON data from
         * @return this instance for method chaining
         * @throws IllegalArgumentException if source is {@code null} or if a Reader source is already set
         */
        public This source(final File source) {
            N.checkArgNotNull(source, "sourceFile");

            if (sourceReader != null) {
                throw new IllegalArgumentException("Can't set both 'sourceFile' and 'sourceReader'");
            }

            this.sourceFile = source;

            return (This) this;
        }

        /**
         * Sets the source reader for CSV/JSON operations.
         *
         * @param source the Reader to read CSV/JSON data from
         * @return this instance for method chaining
         * @throws IllegalArgumentException if source is {@code null} or if a File source is already set
         */
        public This source(final Reader source) {
            N.checkArgNotNull(source, "sourceReader");

            if (sourceFile != null) {
                throw new IllegalArgumentException("Can't set both 'sourceReader' and 'sourceFile'");
            }

            this.sourceReader = source;

            return (This) this;
        }

        /**
         * Specifies which columns to select from the CSV/JSON data.
         *
         * @param selectColumnNames collection of column names to include, {@code null} for all columns
         * @return this instance for method chaining
         */
        public This selectColumns(final Collection<String> selectColumnNames) {
            this.selectColumnNames = selectColumnNames;

            return (This) this;
        }

        /**
         * Sets the number of data rows to skip from the beginning (after header for CSV).
         *
         * @param offset the number of rows to skip
         * @return this instance for method chaining
         */
        public This offset(final long offset) {
            this.offset = offset;

            return (This) this;
        }

        /**
         * Sets the maximum number of rows to process.
         *
         * @param count the maximum number of rows to process
         * @return this instance for method chaining
         */
        public This count(final long count) {
            this.count = count;

            return (This) this;
        }

        /**
         * Sets the bean class for automatic type conversion during CSV reading.
         * Column types are inferred from the bean class properties.
         *
         * @param beanClassForColumnTypeInference the bean class defining property types
         * @return this instance for method chaining
         * @throws IllegalArgumentException if beanClassForColumnTypeInference is {@code null}
         */
        public This beanClassForColumnTypeInference(final Class<?> beanClassForColumnTypeInference) {
            N.checkArgNotNull(beanClassForColumnTypeInference, "beanClassForColumnTypeInference");

            this.beanClassForColumnTypeInference = beanClassForColumnTypeInference;

            return (This) this;
        }

        <T> T apply(Callable<T> action) {
            final Function<String, String[]> currentHeaderParser = headerParser != null ? CsvUtil.getCurrentHeaderParser() : null;
            final BiConsumer<String, String[]> currentLineParser = lineParser != null ? CsvUtil.getCurrentLineParser() : null;
            final Boolean currentBackSlashEscapeCharForWrite = escapeCharToBackSlashForWrite != null ? CsvUtil.isBackSlashEscapeCharForWrite() : null;

            try {
                if (headerParser != null) {
                    CsvUtil.setHeaderParser(headerParser);
                }

                if (lineParser != null) {
                    CsvUtil.setLineParser(lineParser);
                }

                if (escapeCharToBackSlashForWrite != null) {
                    if (escapeCharToBackSlashForWrite) {
                        CsvUtil.setEscapeCharToBackSlashForWrite();
                    } else {
                        CsvUtil.resetEscapeCharForWrite();
                    }
                }

                return action.call();
            } finally {
                if (headerParser != null) {
                    CsvUtil.setHeaderParser(currentHeaderParser);
                }

                if (lineParser != null) {
                    CsvUtil.setLineParser(currentLineParser);
                }

                if (escapeCharToBackSlashForWrite != null) {
                    if (Boolean.TRUE.equals(currentBackSlashEscapeCharForWrite)) {
                        CsvUtil.setEscapeCharToBackSlashForWrite();
                    } else {
                        CsvUtil.resetEscapeCharForWrite();
                    }
                }
            }
        }
    }

    /**
     * A fluent builder for loading CSV data with customizable parsing options.
     * Provides a convenient way to configure column selection, filtering, type conversion,
     * and custom row extraction logic.
     *
     * @see CsvUtil#loader()
     */
    public static final class CSVLoader extends CSVCommon<CSVLoader> {
        private Predicate<? super String[]> rowFilter;
        private Map<String, ? extends Type<?>> columnTypeMap;

        /**
         * Creates a new CSVLoader instance.
         * Use {@link CsvUtil#loader()} to obtain an instance.
         */
        CSVLoader() {
            // package-private constructor
        }

        /**
         * Sets the bean class for automatic type conversion during CSV reading.
         * Column types are inferred from the bean class properties.
         * Cannot be used together with {@code columnTypeMap} or {@code rowExtractor}.
         *
         * @param beanClassForColumnTypeInference the bean class defining property types
         * @return this instance for method chaining
         * @throws IllegalArgumentException if beanClassForColumnTypeInference is {@code null} or if columnTypeMap is already set
         */
        @Override
        public CSVLoader beanClassForColumnTypeInference(final Class<?> beanClassForColumnTypeInference) {
            N.checkArgNotNull(beanClassForColumnTypeInference, "beanClassForColumnTypeInference");

            if (columnTypeMap != null) {
                throw new IllegalArgumentException("Can't set both 'columnTypeMap' and 'beanClassForTypeReading'");
            }

            this.beanClassForColumnTypeInference = beanClassForColumnTypeInference;

            return this;
        }

        /**
         * Sets the column type mapping for type conversion during CSV loading.
         * Cannot be used together with {@code beanClassForColumnTypeInference} or {@code rowExtractor}.
         *
         * @param columnTypeMap mapping of column names to their Types
         * @return this instance for method chaining
         * @throws IllegalArgumentException if columnTypeMap is {@code null} or if beanClassForColumnTypeInference is already set
         */
        public CSVLoader columnTypeMap(final Map<String, ? extends Type<?>> columnTypeMap) {
            N.checkArgNotNull(columnTypeMap, "columnTypeMap");

            if (beanClassForColumnTypeInference != null) {
                throw new IllegalArgumentException("Can't set both 'beanClassForColumnTypeInference' and 'columnTypeMap'");
            }

            this.columnTypeMap = columnTypeMap;
            return this;
        }

        /**
         * Sets a row filter predicate to include only matching rows.
         *
         * @param rowFilter predicate to filter rows
         * @return this instance for method chaining
         * @throws IllegalArgumentException if rowFilter is {@code null}
         */
        public CSVLoader rowFilter(final Predicate<? super String[]> rowFilter) {
            N.checkArgNotNull(rowFilter, "rowFilter");

            this.rowFilter = rowFilter;
            return this;
        }

        /**
         * Loads the CSV data into a Dataset using the configured options.
         * Either a source (file or reader) and a type configuration (columnTypeMap, beanClassForColumnTypeInference)
         * must be set before calling this method.
         *
         * @return a Dataset containing the loaded CSV data
         * @throws IllegalArgumentException if source or type configuration is not set
         * @throws UncheckedIOException if an I/O error occurs
         * @see #load(TriConsumer)
         */
        public Dataset load() throws UncheckedIOException {
            final Callable<Dataset> action = () -> {
                if (sourceFile != null) {
                    if (columnTypeMap != null) {
                        return CsvUtil.load(sourceFile, selectColumnNames, offset, count, rowFilter, columnTypeMap);
                    } else if (beanClassForColumnTypeInference != null) {
                        return CsvUtil.load(sourceFile, selectColumnNames, offset, count, rowFilter, beanClassForColumnTypeInference);
                    } else {
                        throw new IllegalArgumentException(
                                "Either 'columnTypeMap', 'beanClassForColumnTypeInference' or 'rowExtractor' must be set before calling load().");
                    }
                } else if (sourceReader != null) {
                    if (columnTypeMap != null) {
                        return CsvUtil.load(sourceReader, selectColumnNames, offset, count, rowFilter, columnTypeMap);
                    } else if (beanClassForColumnTypeInference != null) {
                        return CsvUtil.load(sourceReader, selectColumnNames, offset, count, rowFilter, beanClassForColumnTypeInference);
                    } else {
                        throw new IllegalArgumentException(
                                "Either 'columnTypeMap', 'beanClassForColumnTypeInference' or 'rowExtractor' must be set before calling load().");
                    }
                } else {
                    throw new IllegalArgumentException("Either 'sourceFile' or 'sourceReader' must be set before calling load().");
                }
            };

            return apply(action);
        }

        /**
         * Loads the CSV data into a Dataset using a custom row extractor function.
         * Either a source (file or reader) must be set before calling this method.
         *
         * @param rowExtractor function to extract data from each row
         * @return a Dataset containing the loaded CSV data
         * @throws IllegalArgumentException if rowExtractor is {@code null} or source is not set
         * @throws UncheckedIOException if an I/O error occurs
         * @see #load()
         */
        public Dataset load(final TriConsumer<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor)
                throws UncheckedIOException {
            N.checkArgNotNull(rowExtractor, "rowExtractor");

            //    if (columnTypeMap != null || beanClassForColumnTypeInference != null) {
            //        throw new IllegalArgumentException("Can't set both 'columnTypeMap/beanClassForColumnTypeInference' and 'rowExtractor'");
            //    }

            final Callable<Dataset> action = () -> {
                if (sourceFile != null) {
                    return CsvUtil.load(sourceFile, selectColumnNames, offset, count, rowFilter, rowExtractor);
                } else if (sourceReader != null) {
                    return CsvUtil.load(sourceReader, selectColumnNames, offset, count, rowFilter, rowExtractor);
                } else {
                    throw new IllegalArgumentException("Either 'sourceFile' or 'sourceReader' must be set before calling load().");
                }
            };

            return apply(action);
        }

        /**
         * Creates a Stream of elements using a custom row mapper function.
         * The reader is not closed automatically when the stream is closed.
         *
         * @param <T> the type of elements in the stream
         * @param rowMapper function to convert each row to the target type
         * @return a Stream of mapped elements
         * @throws IllegalArgumentException if rowMapper is {@code null} or source is not set
         */
        public <T> Stream<T> stream(final BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper) {
            return stream(rowMapper, false);
        }

        /**
         * Creates a Stream of elements using a custom row mapper function with optional reader closing.
         *
         * @param <T> the type of elements in the stream
         * @param rowMapper function to convert each row to the target type
         * @param closeReaderWhenStreamIsClosed {@code true} to close the reader when the stream is closed
         * @return a Stream of mapped elements
         * @throws IllegalArgumentException if rowMapper is {@code null} or source is not set
         */
        public <T> Stream<T> stream(BiFunction<? super List<String>, ? super NoCachingNoUpdating.DisposableArray<String>, ? extends T> rowMapper,
                final boolean closeReaderWhenStreamIsClosed) {
            N.checkArgNotNull(rowMapper, "rowMapper");

            final Callable<Stream<T>> action = () -> {
                if (sourceFile != null) {
                    return CsvUtil.stream(sourceFile, selectColumnNames, offset, count, rowFilter, rowMapper);
                } else if (sourceReader != null) {
                    return CsvUtil.stream(sourceReader, selectColumnNames, offset, count, rowFilter, rowMapper, closeReaderWhenStreamIsClosed);
                } else {
                    throw new IllegalArgumentException("Either 'sourceFile' or 'sourceReader' must be set before calling load().");
                }
            };

            return apply(action);
        }
    }

    /**
     * A fluent builder for converting between CSV and JSON formats.
     * Provides a convenient way to configure column selection, pagination, and type conversion
     * for format conversion operations.
     *
     * @see CsvUtil#converter()
     */
    public static final class CSVConverter extends CSVCommon<CSVConverter> {

        /**
         * Creates a new CSVConverter instance.
         * Use {@link CsvUtil#converter()} to obtain an instance.
         */
        CSVConverter() {
            // package-private constructor
        }

        /**
         * Converts CSV data to JSON format and writes to the specified output file.
         *
         * @param outputJsonFile the file to write JSON output to
         * @return the number of rows converted
         * @throws IllegalArgumentException if outputJsonFile is {@code null} or source is not set
         * @throws UncheckedIOException if an I/O error occurs
         */
        public long csvToJson(File outputJsonFile) throws UncheckedIOException {
            N.checkArgNotNull(outputJsonFile, "outputJsonFile");

            final Callable<Long> action = () -> {
                if (sourceFile != null) {
                    try (Reader csvReader = IOUtil.newFileReader(sourceFile);
                         Writer outputJsonWriter = IOUtil.newFileWriter(outputJsonFile)) {
                        return CsvUtil.csvToJson(csvReader, selectColumnNames, offset, count, outputJsonWriter, beanClassForColumnTypeInference, true);
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                } else if (sourceReader != null) {
                    try (Writer outputJsonWriter = IOUtil.newFileWriter(outputJsonFile)) {
                        return CsvUtil.csvToJson(sourceReader, selectColumnNames, offset, count, outputJsonWriter, beanClassForColumnTypeInference, true);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                } else {
                    throw new IllegalArgumentException("Either 'sourceFile' or 'sourceReader' must be set before calling csvToJson().");
                }
            };

            return apply(action);
        }

        /**
         * Converts CSV data to JSON format and writes to the specified Writer.
         *
         * @param outputJsonWriter the Writer to write JSON output to
         * @return the number of rows converted
         * @throws IllegalArgumentException if outputJsonWriter is {@code null} or source is not set
         * @throws UncheckedIOException if an I/O error occurs
         */
        public long csvToJson(Writer outputJsonWriter) throws UncheckedIOException {
            N.checkArgNotNull(outputJsonWriter, "outputJsonWriter");

            final Callable<Long> action = () -> {
                if (sourceFile != null) {
                    try (Reader csvReader = IOUtil.newFileReader(sourceFile)) {
                        return CsvUtil.csvToJson(csvReader, selectColumnNames, offset, count, outputJsonWriter, beanClassForColumnTypeInference, true);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                } else if (sourceReader != null) {
                    return CsvUtil.csvToJson(sourceReader, selectColumnNames, offset, count, outputJsonWriter, beanClassForColumnTypeInference, true);
                } else {
                    throw new IllegalArgumentException("Either 'sourceFile' or 'sourceReader' must be set before calling csvToJson().");
                }
            };

            return apply(action);
        }

        /**
         * Converts JSON data to CSV format and writes to the specified output file.
         *
         * @param outputCsvFile the file to write CSV output to
         * @return the number of rows converted
         * @throws IllegalArgumentException if outputCsvFile is {@code null} or source is not set
         * @throws UncheckedIOException if an I/O error occurs
         */
        public long jsonToCsv(File outputCsvFile) throws UncheckedIOException {
            N.checkArgNotNull(outputCsvFile, "outputCsvFile");

            final Callable<Long> action = () -> {
                if (sourceFile != null) {
                    try (Reader jsonReader = IOUtil.newFileReader(sourceFile);
                         Writer outputCsvWriter = IOUtil.newFileWriter(outputCsvFile)) {
                        return CsvUtil.jsonToCsv(jsonReader, selectColumnNames, offset, count, outputCsvWriter);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                } else if (sourceReader != null) {
                    try (Writer outputCsvWriter = IOUtil.newFileWriter(outputCsvFile)) {
                        return CsvUtil.jsonToCsv(sourceReader, selectColumnNames, offset, count, outputCsvWriter);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                } else {
                    throw new IllegalArgumentException("Either 'sourceFile' or 'sourceReader' must be set before calling jsonToCsv().");
                }
            };

            return apply(action);
        }

        /**
         * Converts JSON data to CSV format and writes to the specified Writer.
         *
         * @param outputCsvWriter the Writer to write CSV output to
         * @return the number of rows converted
         * @throws IllegalArgumentException if outputCsvWriter is {@code null} or source is not set
         * @throws UncheckedIOException if an I/O error occurs
         */
        public long jsonToCsv(Writer outputCsvWriter) throws UncheckedIOException {
            N.checkArgNotNull(outputCsvWriter, "outputCsvWriter");

            final Callable<Long> action = () -> {
                if (sourceFile != null) {
                    try (Reader jsonReader = IOUtil.newFileReader(sourceFile)) {
                        return CsvUtil.jsonToCsv(jsonReader, selectColumnNames, offset, count, outputCsvWriter);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                } else if (sourceReader != null) {
                    return CsvUtil.jsonToCsv(sourceReader, selectColumnNames, offset, count, outputCsvWriter);
                } else {
                    throw new IllegalArgumentException("Either 'sourceFile' or 'sourceReader' must be set before calling jsonToCsv().");
                }
            };

            return apply(action);
        }
    }

}
