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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.JSONParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.stream.Stream;

/**
 * The CSVUtil class is a utility class that provides methods for handling CSV data.
 * It includes methods for loading CSV data from various sources (like File, InputStream, Reader) into a DataSet.
 * It also provides methods for streaming CSV data into objects of a specified type.
 *
 */
public final class CSVUtil {
    private CSVUtil() {
        // Utillity class
    }

    public static final JSONParser jsonParser = ParserFactory.createJSONParser();

    static final JSONDeserializationConfig jdc = JDC.create().setElementType(String.class);

    static final Splitter lineSplitter = Splitter.with(',').trimResults();

    static final CSVParser csvParser = new CSVParser();

    public static final Function<String, String[]> CSV_HEADER_PARSER = csvParser::parseLineToArray;

    public static final BiConsumer<String, String[]> CSV_LINE_PARSER = csvParser::parseLineToArray;

    public static final Function<String, String[]> CSV_HEADER_PARSER_BY_SPLITTER = it -> {
        final String[] strs = lineSplitter.splitToArray(it);
        int subStrLen = 0;

        for (int i = 0, len = strs.length; i < len; i++) {
            subStrLen = N.len(strs[i]);

            if (subStrLen > 1 && strs[i].charAt(0) == '"' && strs[i].charAt(subStrLen - 1) == '"') {
                strs[i] = strs[i].substring(0, subStrLen - 1);
            }
        }

        return strs;
    };

    public static final BiConsumer<String, String[]> CSV_LINE_PARSER_BY_SPLITTER = (it, ouput) -> {
        lineSplitter.splitToArray(it, ouput);
        int subStrLen = 0;

        for (int i = 0, len = ouput.length; i < len; i++) {
            subStrLen = N.len(ouput[i]);

            if (subStrLen > 1 && ouput[i].charAt(0) == '"' && ouput[i].charAt(subStrLen - 1) == '"') {
                ouput[i] = ouput[i].substring(0, subStrLen - 1);
            }
        }
    };

    static final Function<String, String[]> CSV_HEADER_PARSER_IN_JSON = line -> jsonParser.readString(line, jdc, String[].class);

    static final BiConsumer<String, String[]> CSV_LINE_PARSER_IN_JSON = (line, output) -> jsonParser.readString(line, jdc, output);

    static final Function<String, String[]> defaultCsvHeadereParser = CSV_HEADER_PARSER_IN_JSON;

    static final BiConsumer<String, String[]> defaultCsvLineParser = CSV_LINE_PARSER_IN_JSON;

    static final ThreadLocal<Function<String, String[]>> csvHeaderParser_TL = ThreadLocal.withInitial(() -> defaultCsvHeadereParser);
    static final ThreadLocal<BiConsumer<String, String[]>> csvLineParser_TL = ThreadLocal.withInitial(() -> defaultCsvLineParser);

    /**
     * Sets the CSV header parser for the current thread.
     *
     * @param parser the Function to set as the CSV header parser
     * @throws IllegalArgumentException if the parser is null
     */
    // TODO should share/use the same parser for line?
    public static void setCSVHeaderParser(final Function<String, String[]> parser) throws IllegalArgumentException {
        N.checkArgNotNull(parser, cs.parser);

        csvHeaderParser_TL.set(parser);
    }

    /**
     * Sets the CSV line parser for the current thread.
     *
     * @param parser the BiConsumer to set as the CSV line parser
     * @throws IllegalArgumentException if the parser is null
     */
    public static void setCSVLineParser(final BiConsumer<String, String[]> parser) throws IllegalArgumentException {
        N.checkArgNotNull(parser, cs.parser);

        csvLineParser_TL.set(parser);
    }

    /**
     * Resets the CSV header parser to the default parser for the current thread.
     */
    public static void resetCSVHeaderParser() {
        csvHeaderParser_TL.set(defaultCsvHeadereParser);
    }

    /**
     * Resets the CSV line parser to the default parser for current thread.
     */
    public static void resetCSVLineParser() {
        csvLineParser_TL.set(defaultCsvLineParser);
    }

    /**
     * Returns the CSV header parser in current thread.
     *
     * @return the current CSV header parser as a Function that takes a String and returns a String array
     */
    public static Function<String, String[]> getCurrentHeaderParser() {
        return csvHeaderParser_TL.get();
    }

    /**
     * Returns the CSV line parser in current thread.
     *
     * @return the current CSV line parser as a BiConsumer that takes a String and a String array
     */
    public static BiConsumer<String, String[]> getCurrentLineParser() {
        return csvLineParser_TL.get();
    }

    /**
     *
     * @param source
     * @return
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static DataSet loadCSV(final File source) throws UncheckedIOException {
        return loadCSV(source, (Collection<String>) null);
    }

    /**
     * Loads CSV data from a File source with specified column names.
     *
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @return a DataSet containing the loaded CSV data
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     * Loads CSV data from a File source with specified column names, offset, and count.
     *
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final long offset, final long count)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Loads CSV data from a File source with specified column names, offset, count, and row filter.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @return a DataSet containing the loaded CSV data
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
     * Loads CSV data from an InputStream source.
     *
     * @param source the InputStream source to load CSV data from
     * @return a DataSet containing the loaded CSV data
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(InputStream, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final InputStream source) throws UncheckedIOException {
        return loadCSV(source, (Collection<String>) null);
    }

    /**
     * Loads CSV data from an InputStream source with specified column names.
     *
     * @param source the InputStream source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @return a DataSet containing the loaded CSV data
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(InputStream, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     * Loads CSV data from an InputStream source with specified column names, offset, and count.
     *
     * @param source the InputStream source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(InputStream, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames, final long offset, final long count)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Loads CSV data from a InputStream source with specified column names, offset, count, and row filter.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter) throws UncheckedIOException {
        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR

        return loadCSV(reader, selectColumnNames, offset, count, rowFilter);
    }

    /**
     * Loads CSV data from a Reader source.
     *
     * @param source the Reader source to load CSV data from
     * @return a DataSet containing the loaded CSV data
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final Reader source) throws UncheckedIOException {
        return loadCSV(source, (Collection<String>) null);
    }

    /**
     * Loads CSV data from a Reader source with specified column names.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @return a DataSet containing the loaded CSV data
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     * Loads CSV data from a Reader source with specified column names, offset, and count.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate)
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Loads CSV data from a Reader source with specified column names, offset, count, and row filter.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative
     * @throws UncheckedIOException if an I/O error occurs
     */
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
            final Type<?>[] columnTypes = new Type<?>[columnCount];
            final List<String> columnNameList = new ArrayList<>(selectColumnNames == null ? columnCount : selectColumnNames.size());
            final List<List<Object>> columnList = new ArrayList<>(selectColumnNames == null ? columnCount : selectColumnNames.size());
            final Set<String> selectPropNameSet = selectColumnNames == null ? null : N.newHashSet(selectColumnNames);

            for (int i = 0; i < columnCount; i++) {
                if (selectPropNameSet == null || selectPropNameSet.remove(titles[i])) {
                    columnNameList.add(titles[i]);
                    columnList.add(new ArrayList<>());
                    columnTypes[i] = N.typeOf(String.class);
                }
            }

            if (selectPropNameSet != null && selectPropNameSet.size() > 0) {
                throw new IllegalArgumentException(selectPropNameSet + " are not included in titles: " + N.toString(titles)); //NOSONAR
            }

            final String[] output = new String[titles.length];

            while (offset-- > 0 && br.readLine() != null) { // NOSONAR
                // continue;
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(line, output);

                if (rowFilter != null && !rowFilter.test(output)) {
                    continue;
                }

                for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                    if (columnTypes[i] != null) {
                        columnList.get(columnIndex++).add(output[i]);
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
     * Loads CSV data from a File source with specified column types.
     *
     * @param source the File source to load CSV data from
     * @param beanClassForColumnTypeForColumnType the Class specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if beanClassForColumnType is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, Collection, long, long, Class)
     */
    public static DataSet loadCSV(final File source, final Class<?> beanClassForColumnTypeForColumnType) throws UncheckedIOException {
        return loadCSV(source, null, beanClassForColumnTypeForColumnType);
    }

    /**
     * Loads CSV data from a File source with specified column names and column types.
     *
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param beanClassForColumnType the Class specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if beanClassForColumnType is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, Collection, long, long, Class)
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final Class<?> beanClassForColumnType)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a File source with specified column names, offset, count, and column type.
     *
     * @param source the File source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param beanClassForColumnType the Class specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if beanClassForColumnType is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, Collection, long, long, Class)
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue(), beanClassForColumnType);
    }

    /**
     * Loads CSV data from a File source with specified column names, offset, count, row filter, and column type.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param beanClassForColumnType the Class of the bean for column type
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if beanClassForColumnType is {@code null}.
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
     * Loads CSV data from an InputStream source with specified column types.
     *
     * @param source the InputStream source to load CSV data from
     * @param beanClassForColumnType the Class specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if beanClassForColumnType is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(InputStream, Collection, long, long, Predicate, Class)
     */
    public static DataSet loadCSV(final InputStream source, final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, null, beanClassForColumnType);
    }

    /**
     * Loads CSV data from an InputStream source with specified column names and column types.
     *
     * @param source the InputStream source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param beanClassForColumnType the Class specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if beanClassForColumnType is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(InputStream, Collection, long, long, Predicate, Class)
     */
    public static DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames, final Class<?> beanClassForColumnType)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE, beanClassForColumnType);
    }

    /**
     * Loads CSV data from an InputStream source with specified column names, offset, count, and column type.
     *
     * @param source the InputStream source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param beanClassForColumnType the Class specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if beanClassForColumnType is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(InputStream, Collection, long, long, Predicate, Class)
     */
    public static DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue(), beanClassForColumnType);
    }

    /**
     * Loads CSV data from a InputStream source with specified column names, offset, count, row filter, and column type.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param beanClassForColumnType the Class of the bean for column type
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if beanClassForColumnType is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter, final Class<?> beanClassForColumnType) throws UncheckedIOException {
        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR

        return loadCSV(reader, selectColumnNames, offset, count, rowFilter, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a Reader source with specified column types.
     *
     * @param source the Reader source to load CSV data from
     * @param beanClassForColumnType the Class specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if beanClassForColumnType is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate, Class)
     */
    public static DataSet loadCSV(final Reader source, final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, null, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a Reader source with specified column names and column types.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param beanClassForColumnType the Class specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if beanClassForColumnType is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate, Class)
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, final Class<?> beanClassForColumnType)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE, beanClassForColumnType);
    }

    /**
     * Loads CSV data from a Reader source with specified column names, offset, count, and column type.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param beanClassForColumnType the Class specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if beanClassForColumnType is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, Collection, long, long, Predicate, Class)
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue(), beanClassForColumnType);
    }

    /**
     * Loads CSV data from a Reader source with specified column names, offset, count, row filter, and column type.
     *
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param beanClassForColumnType the Class of the bean for column type
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if beanClassForColumnType is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs
     */
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
            final PropInfo[] propInfos = new PropInfo[columnCount];
            final List<String> columnNameList = new ArrayList<>(selectColumnNames == null ? columnCount : selectColumnNames.size());
            final List<List<Object>> columnList = new ArrayList<>(selectColumnNames == null ? columnCount : selectColumnNames.size());
            final Set<String> selectPropNameSet = selectColumnNames == null ? null : N.newHashSet(selectColumnNames);

            for (int i = 0; i < columnCount; i++) {
                if (selectPropNameSet == null || selectPropNameSet.remove(titles[i])) {
                    propInfos[i] = beanInfo.getPropInfo(titles[i]);

                    if (propInfos[i] == null) {
                        if (selectPropNameSet != null && selectPropNameSet.remove(titles[i])) {
                            throw new IllegalArgumentException(
                                    titles[i] + " is not defined in bean class: " + ClassUtil.getCanonicalClassName(beanClassForColumnType));
                        }
                    } else {
                        if (selectPropNameSet == null || selectPropNameSet.remove(titles[i]) || selectPropNameSet.remove(propInfos[i].name)) {
                            columnNameList.add(titles[i]);
                            columnList.add(new ArrayList<>());
                        } else {
                            propInfos[i] = null;
                        }
                    }
                }
            }

            if (N.notEmpty(selectPropNameSet)) {
                throw new IllegalArgumentException(selectColumnNames + " are not included in titles: " + N.toString(titles));
            }

            final String[] output = new String[titles.length];

            while (offset-- > 0 && br.readLine() != null) { // NOSONAR
                // continue
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(line, output);

                if (rowFilter != null && !rowFilter.test(output)) {
                    continue;
                }

                for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                    if (propInfos[i] != null) {
                        columnList.get(columnIndex++).add(propInfos[i].readPropValue(output[i]));
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
     * Loads CSV data from a File source with specified column types.
     *
     * @param source the File source to load CSV data from
     * @param columnTypeMap a Map specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if columnTypeMap is {@code null} or empty
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, long, long, Predicate, Map)
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(source, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     * Loads CSV data from a File source with specified offset, count, and column type map.
     *
     * @param source the File source to load CSV data from
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param columnTypeMap a Map specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if columnTypeMap is {@code null} or empty
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(File, long, long, Predicate, Map)
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final long offset, final long count, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeMap);
    }

    /**
     * Loads CSV data from a File source with specified offset, count, filter, and column type map.
     *
     * @param source the Reader source to load CSV data from
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param columnTypeMap a Map specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if columnTypeMap is {@code null} or empty
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
     * Loads CSV data from an InputStream source with specified column types.
     *
     * @param source the InputStream source to load CSV data from
     * @param columnTypeMap a Map specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if columnTypeMap is {@code null} or empty
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(InputStream, long, long, Predicate, Map)
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream source, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(source, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     * Loads CSV data from an InputStream source with specified offset, count, and column type map.
     *
     * @param source the InputStream source to load CSV data from
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param columnTypeMap a Map specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if columnTypeMap is {@code null} or empty
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(InputStream, long, long, Predicate, Map)
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream source, final long offset, final long count, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeMap);
    }

    /**
     * Loads CSV data from a InputStream source with specified offset, count, filter, and column type map.
     *
     * @param source the Reader source to load CSV data from
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param columnTypeMap a Map specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if columnTypeMap is {@code null} or empty
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream source, final long offset, final long count, final Predicate<? super String[]> rowFilter,
            final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR

        return loadCSV(reader, offset, count, rowFilter, columnTypeMap);
    }

    /**
     * Loads CSV data from a Reader source with specified column types.
     *
     * @param source the Reader source to load CSV data from
     * @param columnTypeMap a Map specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if columnTypeMap is {@code null} or empty
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, long, long, Predicate, Map)
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(source, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     * Loads CSV data from a Reader source with specified offset, count, and column type map.
     *
     * @param source the Reader source to load CSV data from
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param columnTypeMap a Map specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if columnTypeMap is {@code null} or empty
     * @throws UncheckedIOException if an I/O error occurs
     * @see #loadCSV(Reader, long, long, Predicate, Map)
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, final long offset, final long count, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeMap);
    }

    /**
     * Loads CSV data from a Reader source with specified offset, count, filter, and column type map.
     *
     * @param source the Reader source to load CSV data from
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param columnTypeMap a Map specifying the type of each column
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if columnTypeMap is {@code null} or empty
     * @throws UncheckedIOException if an I/O error occurs
     */
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
            final Type<?>[] columnTypes = new Type<?>[columnCount];
            final List<String> columnNameList = new ArrayList<>(columnTypeMap.size());
            final List<List<Object>> columnList = new ArrayList<>(columnTypeMap.size());

            for (int i = 0; i < columnCount; i++) {
                if (columnTypeMap.containsKey(titles[i])) {
                    columnTypes[i] = columnTypeMap.get(titles[i]);
                    columnNameList.add(titles[i]);
                    columnList.add(new ArrayList<>());
                }
            }

            if (columnNameList.size() != columnTypeMap.size()) {
                final List<String> keys = new ArrayList<>(columnTypeMap.keySet());
                keys.removeAll(columnNameList);
                throw new IllegalArgumentException(keys + " are not included in titles: " + N.toString(titles));
            }

            final String[] output = new String[titles.length];

            while (offset-- > 0 && br.readLine() != null) { // NOSONAR
                // continue
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(line, output);

                if (rowFilter != null && !rowFilter.test(output)) {
                    continue;
                }

                for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                    if (columnTypes[i] != null) {
                        columnList.get(columnIndex++).add(columnTypes[i].valueOf(output[i]));
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
     * Loads CSV data from a File source with specified column types.
     * <br />
     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV.
     * <br />
     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
     *
     * @param source the File source to load CSV data from
     * @param columnTypeList a List specifying the type of each column. Set the column type to {@code null} to skip the column.
     *        The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV.
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final List<? extends Type> columnTypeList) throws UncheckedIOException {
        return loadCSV(source, 0, Long.MAX_VALUE, columnTypeList);
    }

    /**
     * Loads CSV data from a File source with specified offset, count, and column type list.
     * <br />
     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV.
     * <br />
     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
     *
     * @param source the File source to load CSV data from
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param columnTypeList a List specifying the type of each column. Set the column type to {@code null} to skip the column.
     *        The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV.
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final long offset, final long count, final List<? extends Type> columnTypeList)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeList);
    }

    /**
     * Loads CSV data from a File source with specified offset, count, row filter, and column type list.
     *
     * @param source the InputStream source to load CSV data from
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param columnTypeList a List specifying the type of each column. Set the column type to {@code null} to skip the column.
     *        The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV.
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final long offset, final long count, final Predicate<? super String[]> rowFilter,
            final List<? extends Type> columnTypeList) throws UncheckedIOException {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return loadCSV(reader, offset, count, rowFilter, columnTypeList);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     * Load the data from CSV.
     * <br />
     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV
     * <br />
     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
     *
     * @param source
     * @param columnTypeList a List specifying the type of each column. Set the column type to {@code null} to skip the column.
     *        The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV.
     * @return
     * @throws IllegalArgumentException if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream source, final List<? extends Type> columnTypeList) throws UncheckedIOException {
        return loadCSV(source, 0, Long.MAX_VALUE, columnTypeList);
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
     * @param columnTypeList a List specifying the type of each column. Set the column type to {@code null} to skip the column.
     *        The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV.
     * @return
     * @throws IllegalArgumentException if offset or count are negative, or the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream source, final long offset, final long count, final List<? extends Type> columnTypeList)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeList);
    }

    /**
     * Loads CSV data from a InputStream source with specified offset, count, row filter, and column type list.
     *
     * @param source the InputStream source to load CSV data from
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param columnTypeList a List specifying the type of each column. Set the column type to {@code null} to skip the column.
     *        The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV.
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream source, final long offset, final long count, final Predicate<? super String[]> rowFilter,
            final List<? extends Type> columnTypeList) throws UncheckedIOException {
        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR

        return loadCSV(reader, offset, count, rowFilter, columnTypeList);
    }

    /**
     * Load the data from CSV.
     * <br />
     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV
     * <br />
     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
     *
     * @param source
     * @param columnTypeList a List specifying the type of each column. Set the column type to {@code null} to skip the column.
     *        The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV.
     * @return
     * @throws IllegalArgumentException if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, final List<? extends Type> columnTypeList) throws UncheckedIOException {
        return loadCSV(source, 0, Long.MAX_VALUE, columnTypeList);
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
     * @param columnTypeList a List specifying the type of each column. Set the column type to {@code null} to skip the column.
     *        The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV.
     * @return
     * @throws IllegalArgumentException if offset or count are negative, or the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, final long offset, final long count, final List<? extends Type> columnTypeList)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeList);
    }

    /**
     * Loads CSV data from a Reader source with specified offset, count, row filter, and column type list.
     *
     * @param source the Reader source to load CSV data from
     * @param offset the number of lines to skip from the beginning
     * @param count the maximum number of lines to read
     * @param rowFilter a Predicate to filter the lines
     * @param columnTypeList a List specifying the type of each column. Set the column type to {@code null} to skip the column.
     *        The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV.
     * @return a DataSet containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the size of {@code columnTypeList} is not equal to the size of columns in CSV.
     * @throws UncheckedIOException if an I/O error occurs
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, long offset, long count, final Predicate<? super String[]> rowFilter,
            final List<? extends Type> columnTypeList) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

        if (N.isEmpty(columnTypeList)) {
            throw new IllegalArgumentException("columnTypeList can't be null or empty");
        }

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);
        final Type<?>[] columnTypes = columnTypeList.toArray(new Type[columnTypeList.size()]);

        try {
            String line = br.readLine();

            if (line == null) {
                return N.newEmptyDataSet();
            }

            final String[] titles = headerParser.apply(line);

            final int columnCount = titles.length;
            final List<String> columnNameList = new ArrayList<>(columnCount);
            final List<List<Object>> columnList = new ArrayList<>();

            for (int i = 0; i < columnCount; i++) {
                if (columnTypes[i] != null) {
                    columnNameList.add(titles[i]);
                    columnList.add(new ArrayList<>());
                }
            }

            N.checkArgument(N.size(columnTypeList) == N.len(titles),
                    "The size of specified 'columnTypeList' must be equal to the size of columns of the specified CSV");

            final String[] output = new String[titles.length];

            while (offset-- > 0 && br.readLine() != null) { // NOSONAR
                // continue
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(line, output);

                if (rowFilter != null && !rowFilter.test(output)) {
                    continue;
                }

                for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                    if (columnTypes[i] != null) {
                        columnList.get(columnIndex++).add(columnTypes[i].valueOf(output[i]));
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
     * Streams CSV data from a File source with the specified target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the File source to load CSV data from
     * @param targetType the Class of the target type
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final File source, final Class<? extends T> targetType) {
        return stream(source, (Collection<String>) null, targetType);
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

            return stream(reader, selectColumnNames, offset, count, rowFilter, true, targetType);
        } catch (final Exception e) {
            if (reader != null) {
                IOUtil.closeQuietly(reader);
            }

            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Streams CSV data from a Reader source with specified target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param closeReaderWhenStreamIsClosed a boolean indicating whether to close the Reader when the stream is closed
     * @param targetType the Class of the target type
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final Reader source, final boolean closeReaderWhenStreamIsClosed, final Class<? extends T> targetType) {
        return stream(source, (Collection<String>) null, closeReaderWhenStreamIsClosed, targetType);
    }

    /**
     * Streams CSV data from a Reader source with specified column names, offset, count, row filter, and target type.
     *
     * @param <T> the type of the elements in the stream
     * @param source the Reader source to load CSV data from
     * @param selectColumnNames a Collection of column names to select
     * @param closeReaderWhenStreamIsClosed a boolean indicating whether to close the Reader when the stream is closed
     * @param targetType the Class of the target type
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, or if the target type is {@code null} or not supported
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames, final boolean closeReaderWhenStreamIsClosed,
            final Class<? extends T> targetType) {
        return stream(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), closeReaderWhenStreamIsClosed, targetType);
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
     * @param closeReaderWhenStreamIsClosed whether to close the Reader when the stream is closed
     * @param targetType the Class of the target type
     * @return a Stream of the specified target type containing the loaded CSV data
     * @throws IllegalArgumentException if offset or count are negative, of if the target type is {@code null} or not supported.
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<? super String[]> rowFilter, final boolean closeReaderWhenStreamIsClosed, final Class<? extends T> targetType)
            throws IllegalArgumentException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);
        N.checkArgNotNull(targetType, cs.targetType);

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

                final boolean isBean = ClassUtil.isBeanClass(targetType);
                final BeanInfo beanInfo = isBean ? ParserUtil.getBeanInfo(targetType) : null;

                final int columnCount = titles.length;
                final String[] resultColumnNames = new String[columnCount];
                final Set<String> selectPropNameSet = selectColumnNames == null ? null : N.newHashSet(selectColumnNames);
                final PropInfo[] propInfos = isBean ? new PropInfo[columnCount] : null;
                int resultColumnCount = 0;

                for (int i = 0; i < columnCount; i++) {
                    if (isBean) {
                        propInfos[i] = beanInfo.getPropInfo(titles[i]);

                        if (propInfos[i] == null) {
                            if (selectPropNameSet != null && selectPropNameSet.remove(titles[i])) {
                                throw new IllegalArgumentException(titles[i] + " is not defined in bean class: " + ClassUtil.getCanonicalClassName(targetType));
                            }
                        } else {
                            if (selectPropNameSet == null || selectPropNameSet.remove(titles[i]) || selectPropNameSet.remove(propInfos[i].name)) {
                                resultColumnNames[i] = titles[i];
                                resultColumnCount++;
                            } else {
                                propInfos[i] = null;
                            }
                        }
                    } else {
                        if (selectPropNameSet == null || selectPropNameSet.remove(titles[i])) {
                            resultColumnNames[i] = titles[i];
                            resultColumnCount++;
                        }
                    }
                }

                if (N.notEmpty(selectPropNameSet)) {
                    throw new IllegalArgumentException(selectColumnNames + " are not included in titles: " + N.toString(titles));
                }

                long offsetTmp = offset;

                while (offsetTmp-- > 0 && br.readLine() != null) { // NOSONAR
                    // continue
                }

                final Type<T> type = Type.of(targetType);
                final int finalResultColumnCount = resultColumnCount;

                com.landawn.abacus.util.function.Function<String[], T> mapper = null;

                if (type.isObjectArray()) {
                    final Class<?> componentType = targetType.getComponentType();

                    mapper = values -> {
                        final Object[] result = N.newArray(componentType, finalResultColumnCount);

                        for (int i = 0, j = 0; i < columnCount; i++) {
                            if (resultColumnNames[i] != null) {
                                result[j++] = values[i];
                            }
                        }

                        return (T) result;
                    };

                } else if (type.isCollection()) {
                    mapper = values -> {
                        @SuppressWarnings("rawtypes")
                        final Collection<Object> result = N.newCollection((Class<Collection>) targetType, finalResultColumnCount);

                        for (int i = 0; i < columnCount; i++) {
                            if (resultColumnNames[i] != null) {
                                result.add(values[i]);
                            }
                        }

                        return (T) result;
                    };

                } else if (type.isMap()) {
                    mapper = values -> {
                        @SuppressWarnings("rawtypes")
                        final Map<String, Object> result = N.newMap((Class<Map>) targetType, finalResultColumnCount);

                        for (int i = 0; i < columnCount; i++) {
                            if (resultColumnNames[i] != null) {
                                result.put(resultColumnNames[i], values[i]);
                            }
                        }

                        return (T) result;
                    };

                } else if (type.isBean()) {
                    mapper = values -> {
                        final Object result = beanInfo.createBeanResult();

                        for (int i = 0; i < columnCount; i++) {
                            if (resultColumnNames[i] != null) {
                                propInfos[i].setPropValue(result, propInfos[i].readPropValue(values[i]));
                            }
                        }

                        beanInfo.finishBeanResult(result);

                        return (T) result;
                    };

                } else if (finalResultColumnCount == 1) {
                    int targetColumnIndex = 0;

                    for (int i = 0; i < columnCount; i++) {
                        if (resultColumnNames[i] != null) {
                            targetColumnIndex = i;
                            break;
                        }
                    }

                    final int finalTargetColumnIndex = targetColumnIndex;

                    mapper = values -> type.valueOf(values[finalTargetColumnIndex]);

                } else {
                    throw new IllegalArgumentException("Unsupported target type: " + targetType);
                }

                final String[] output = new String[titles.length];

                final Stream<T> ret = ((rowFilter == null || N.equals(rowFilter, Fn.alwaysTrue()) || N.equals(rowFilter, Fnn.alwaysTrue())) //
                        ? Stream.ofLines(br).map(it -> {
                            lineParser.accept(it, output);
                            return output;
                        }) //
                        : Stream.ofLines(br).map(it -> {
                            lineParser.accept(it, output);
                            return output;
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
}
