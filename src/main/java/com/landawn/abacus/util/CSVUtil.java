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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class CSVUtil {

    public static final JSONParser jsonParser = ParserFactory.createJSONParser();

    static final JSONDeserializationConfig jdc = JDC.create().setElementType(String.class);

    static final Splitter lineSplitter = Splitter.with(',').trimResults();

    static final CSVParser csvParser = new CSVParser();

    public static final Function<String, String[]> CSV_HEADER_PARSER = csvParser::parseLineToArray;

    public static final BiConsumer<String[], String> CSV_LINE_PARSER = csvParser::parseLineToArray;

    public static final Function<String, String[]> CSV_HEADER_PARSER_BY_SPLITTER = lineSplitter::splitToArray;

    public static final BiConsumer<String[], String> CSV_LINE_PARSER_BY_SPLITTER = lineSplitter::splitToArray;

    static final Function<String, String[]> CSV_HEADER_PARSER_IN_JSON = line -> jsonParser.readString(String[].class, line, jdc);

    static final BiConsumer<String[], String> CSV_LINE_PARSER_IN_JSON = (output, line) -> jsonParser.readString(output, line, jdc);

    static final Function<String, String[]> defaultCsvHeadereParser = CSV_HEADER_PARSER_IN_JSON;

    static final BiConsumer<String[], String> defaultCsvLineParser = CSV_LINE_PARSER_IN_JSON;

    static final ThreadLocal<Function<String, String[]>> csvHeaderParser_TL = ThreadLocal.withInitial(() -> defaultCsvHeadereParser);
    static final ThreadLocal<BiConsumer<String[], String>> csvLineParser_TL = ThreadLocal.withInitial(() -> defaultCsvLineParser);

    // TODO should share/use the same parser for line?
    public static void setCSVHeaderParser(final Function<String, String[]> parser) {
        N.checkArgNotNull(parser, "parser");

        csvHeaderParser_TL.set(parser);
    }

    public static void setCSVLineParser(final BiConsumer<String[], String> parser) {
        N.checkArgNotNull(parser, "parser");

        csvLineParser_TL.set(parser);
    }

    public static void resetCSVHeaderParser() {
        csvHeaderParser_TL.set(defaultCsvHeadereParser);
    }

    public static void resetCSVLineParser() {
        csvLineParser_TL.set(defaultCsvLineParser);
    }

    public static Function<String, String[]> getCurrentHeaderParser() {
        return csvHeaderParser_TL.get();
    }

    public static BiConsumer<String[], String> getCurrentLineParser() {
        return csvLineParser_TL.get();
    }

    /**
     *
     * @param csvFile
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final File csvFile) throws UncheckedIOException {
        return loadCSV(csvFile, (Collection<String>) null);
    }

    /**
     *
     * @param csvFile
     * @param selectColumnNames
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final File csvFile, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(csvFile, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param csvFile
     * @param selectColumnNames
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final File csvFile, final Collection<String> selectColumnNames, final long offset, final long count)
            throws UncheckedIOException {
        return loadCSV(csvFile, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param csvFile
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final File csvFile, final Collection<String> selectColumnNames, final long offset, final long count,
            final Throwables.Predicate<String[], E> filter) throws UncheckedIOException, E {
        InputStream csvInputStream = null;

        try {
            csvInputStream = IOUtil.newFileInputStream(csvFile);

            return loadCSV(csvInputStream, selectColumnNames, offset, count, filter);
        } finally {
            IOUtil.closeQuietly(csvInputStream);
        }
    }

    /**
     *
     * @param csvInputStream
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final InputStream csvInputStream) throws UncheckedIOException {
        return loadCSV(csvInputStream, (Collection<String>) null);
    }

    /**
     *
     * @param csvInputStream
     * @param selectColumnNames
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final InputStream csvInputStream, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(csvInputStream, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param csvInputStream
     * @param selectColumnNames
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final InputStream csvInputStream, final Collection<String> selectColumnNames, final long offset, final long count)
            throws UncheckedIOException {
        return loadCSV(csvInputStream, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param csvInputStream
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final InputStream csvInputStream, final Collection<String> selectColumnNames, final long offset,
            final long count, final Throwables.Predicate<String[], E> filter) throws UncheckedIOException, E {
        final Reader csvReader = new InputStreamReader(csvInputStream);

        return loadCSV(csvReader, selectColumnNames, offset, count, filter);
    }

    /**
     *
     * @param csvReader
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Reader csvReader) throws UncheckedIOException {
        return loadCSV(csvReader, (Collection<String>) null);
    }

    /**
     *
     * @param csvReader
     * @param selectColumnNames
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Reader csvReader, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(csvReader, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param csvReader
     * @param selectColumnNames
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Reader csvReader, final Collection<String> selectColumnNames, long offset, long count) throws UncheckedIOException {
        return loadCSV(csvReader, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param csvReader
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final Reader csvReader, final Collection<String> selectColumnNames, long offset, long count,
            final Throwables.Predicate<String[], E> filter) throws UncheckedIOException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String[], String> lineParser = csvLineParser_TL.get();
        final BufferedReader br = csvReader instanceof BufferedReader ? (BufferedReader) csvReader : Objectory.createBufferedReader(csvReader);

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
                throw new IllegalArgumentException(selectPropNameSet + " are not included in titles: " + N.toString(titles));
            }

            final String[] strs = new String[titles.length];

            while (offset-- > 0 && br.readLine() != null) {
                // continue;
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(strs, line);

                if (filter != null && !filter.test(strs)) {
                    continue;
                }

                for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                    if (columnTypes[i] != null) {
                        columnList.get(columnIndex++).add(strs[i]);
                    }
                }

                count--;
            }

            return new RowDataSet(columnNameList, columnList);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (br != csvReader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     *
     * @param entityClass
     * @param csvFile
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Class<?> entityClass, final File csvFile) throws UncheckedIOException {
        return loadCSV(entityClass, csvFile, null);
    }

    /**
     *
     * @param entityClass
     * @param csvFile
     * @param selectColumnNames
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Class<?> entityClass, final File csvFile, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(entityClass, csvFile, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param entityClass
     * @param csvFile
     * @param selectColumnNames
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Class<?> entityClass, final File csvFile, final Collection<String> selectColumnNames, final long offset,
            final long count) throws UncheckedIOException {
        return loadCSV(entityClass, csvFile, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param entityClass
     * @param csvFile
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final Class<?> entityClass, final File csvFile, final Collection<String> selectColumnNames,
            final long offset, final long count, final Throwables.Predicate<String[], E> filter) throws UncheckedIOException, E {
        InputStream csvInputStream = null;

        try {
            csvInputStream = IOUtil.newFileInputStream(csvFile);

            return loadCSV(entityClass, csvInputStream, selectColumnNames, offset, count, filter);
        } finally {
            IOUtil.closeQuietly(csvInputStream);
        }
    }

    /**
     *
     * @param entityClass
     * @param csvInputStream
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Class<?> entityClass, final InputStream csvInputStream) throws UncheckedIOException {
        return loadCSV(entityClass, csvInputStream, null);
    }

    /**
     *
     * @param entityClass
     * @param csvInputStream
     * @param selectColumnNames
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Class<?> entityClass, final InputStream csvInputStream, final Collection<String> selectColumnNames)
            throws UncheckedIOException {
        return loadCSV(entityClass, csvInputStream, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param entityClass
     * @param csvInputStream
     * @param selectColumnNames
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Class<?> entityClass, final InputStream csvInputStream, final Collection<String> selectColumnNames, final long offset,
            final long count) throws UncheckedIOException {
        return loadCSV(entityClass, csvInputStream, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param entityClass
     * @param csvInputStream
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final Class<?> entityClass, final InputStream csvInputStream,
            final Collection<String> selectColumnNames, final long offset, final long count, final Throwables.Predicate<String[], E> filter)
            throws UncheckedIOException, E {
        final Reader csvReader = new InputStreamReader(csvInputStream);
        return loadCSV(entityClass, csvReader, selectColumnNames, offset, count, filter);
    }

    /**
     *
     * @param entityClass
     * @param csvReader
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Class<?> entityClass, final Reader csvReader) throws UncheckedIOException {
        return loadCSV(entityClass, csvReader, null);
    }

    /**
     *
     * @param entityClass
     * @param csvReader
     * @param selectColumnNames
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Class<?> entityClass, final Reader csvReader, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(entityClass, csvReader, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param entityClass
     * @param csvReader
     * @param selectColumnNames
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Class<?> entityClass, final Reader csvReader, final Collection<String> selectColumnNames, long offset, long count)
            throws UncheckedIOException {
        return loadCSV(entityClass, csvReader, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param entityClass
     * @param csvReader
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final Class<?> entityClass, final Reader csvReader, final Collection<String> selectColumnNames,
            long offset, long count, final Throwables.Predicate<String[], E> filter) throws UncheckedIOException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String[], String> lineParser = csvLineParser_TL.get();
        final BufferedReader br = csvReader instanceof BufferedReader ? (BufferedReader) csvReader : Objectory.createBufferedReader(csvReader);
        final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);

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
                    propInfos[i] = entityInfo.getPropInfo(titles[i]);

                    if (propInfos[i] == null) {
                        if (selectPropNameSet != null && selectPropNameSet.remove(titles[i])) {
                            throw new IllegalArgumentException(titles[i] + " is not defined in entity class: " + ClassUtil.getCanonicalClassName(entityClass));
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

            if (N.notNullOrEmpty(selectPropNameSet)) {
                throw new IllegalArgumentException(selectColumnNames + " are not included in titles: " + N.toString(titles));
            }

            final String[] strs = new String[titles.length];

            while (offset-- > 0 && br.readLine() != null) {
                // continue
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(strs, line);

                if (filter != null && !filter.test(strs)) {
                    continue;
                }

                for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                    if (propInfos[i] != null) {
                        columnList.get(columnIndex++).add(propInfos[i].readPropValue(strs[i]));
                    }
                }

                count--;
            }

            return new RowDataSet(columnNameList, columnList);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (br != csvReader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     *
     * @param csvFile
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File csvFile, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(csvFile, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     *
     * @param csvFile
     * @param offset
     * @param count
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File csvFile, final long offset, final long count, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedIOException {
        return loadCSV(csvFile, offset, count, Fn.<String[]> alwaysTrue(), columnTypeMap);
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param csvFile
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final File csvFile, final long offset, final long count, final Throwables.Predicate<String[], E> filter,
            final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException, E {
        InputStream csvInputStream = null;

        try {
            csvInputStream = IOUtil.newFileInputStream(csvFile);

            return loadCSV(csvInputStream, offset, count, filter, columnTypeMap);
        } finally {
            IOUtil.closeQuietly(csvInputStream);
        }
    }

    /**
     *
     * @param csvInputStream
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream csvInputStream, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(csvInputStream, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     *
     * @param csvInputStream
     * @param offset
     * @param count
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream csvInputStream, final long offset, final long count, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedIOException {
        return loadCSV(csvInputStream, offset, count, Fn.<String[]> alwaysTrue(), columnTypeMap);
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param csvInputStream
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final InputStream csvInputStream, final long offset, final long count,
            final Throwables.Predicate<String[], E> filter, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException, E {
        final Reader csvReader = new InputStreamReader(csvInputStream);

        return loadCSV(csvReader, offset, count, filter, columnTypeMap);
    }

    /**
     *
     * @param csvReader
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader csvReader, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(csvReader, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     *
     * @param csvReader
     * @param offset
     * @param count
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader csvReader, long offset, long count, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedIOException {
        return loadCSV(csvReader, offset, count, Fn.<String[]> alwaysTrue(), columnTypeMap);
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param csvReader
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final Reader csvReader, long offset, long count, final Throwables.Predicate<String[], E> filter,
            final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

        if (N.isNullOrEmpty(columnTypeMap)) {
            throw new IllegalArgumentException("columnTypeMap can't be null or empty");
        }

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String[], String> lineParser = csvLineParser_TL.get();
        final BufferedReader br = csvReader instanceof BufferedReader ? (BufferedReader) csvReader : Objectory.createBufferedReader(csvReader);

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

            final String[] strs = new String[titles.length];

            while (offset-- > 0 && br.readLine() != null) {
                // continue
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(strs, line);

                if (filter != null && !filter.test(strs)) {
                    continue;
                }

                for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                    if (columnTypes[i] != null) {
                        columnList.get(columnIndex++).add(columnTypes[i].valueOf(strs[i]));
                    }
                }

                count--;
            }

            return new RowDataSet(columnNameList, columnList);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (br != csvReader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     *
     * @param csvFile
     * @param columnTypeList
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File csvFile, final List<? extends Type> columnTypeList) throws UncheckedIOException {
        return loadCSV(csvFile, 0, Long.MAX_VALUE, columnTypeList);
    }

    /**
     *
     * @param csvFile
     * @param offset
     * @param count
     * @param columnTypeList
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File csvFile, final long offset, final long count, final List<? extends Type> columnTypeList)
            throws UncheckedIOException {
        return loadCSV(csvFile, offset, count, Fn.<String[]> alwaysTrue(), columnTypeList);
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param csvFile
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeList set the column type to null to skip the column in CSV.
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final File csvFile, final long offset, final long count, final Throwables.Predicate<String[], E> filter,
            final List<? extends Type> columnTypeList) throws UncheckedIOException, E {
        InputStream csvInputStream = null;

        try {
            csvInputStream = IOUtil.newFileInputStream(csvFile);

            return loadCSV(csvInputStream, offset, count, filter, columnTypeList);
        } finally {
            IOUtil.closeQuietly(csvInputStream);
        }
    }

    /**
     *
     * @param csvInputStream
     * @param columnTypeList
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream csvInputStream, final List<? extends Type> columnTypeList) throws UncheckedIOException {
        return loadCSV(csvInputStream, 0, Long.MAX_VALUE, columnTypeList);
    }

    /**
     *
     * @param csvInputStream
     * @param offset
     * @param count
     * @param columnTypeList
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream csvInputStream, final long offset, final long count, final List<? extends Type> columnTypeList)
            throws UncheckedIOException {
        return loadCSV(csvInputStream, offset, count, Fn.<String[]> alwaysTrue(), columnTypeList);
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param csvInputStream
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeList set the column type to null to skip the column in CSV.
     * @return
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final InputStream csvInputStream, final long offset, final long count,
            final Throwables.Predicate<String[], E> filter, final List<? extends Type> columnTypeList) throws E {
        final Reader csvReader = new InputStreamReader(csvInputStream);

        return loadCSV(csvReader, offset, count, filter, columnTypeList);
    }

    /**
     *
     * @param csvReader
     * @param columnTypeList
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader csvReader, final List<? extends Type> columnTypeList) {
        return loadCSV(csvReader, 0, Long.MAX_VALUE, columnTypeList);
    }

    /**
     *
     * @param csvReader
     * @param offset
     * @param count
     * @param columnTypeList
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader csvReader, long offset, long count, final List<? extends Type> columnTypeList) {
        return loadCSV(csvReader, offset, count, Fn.<String[]> alwaysTrue(), columnTypeList);
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param csvReader
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeList set the column type to null to skip the column in CSV.
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final Reader csvReader, long offset, long count, final Throwables.Predicate<String[], E> filter,
            final List<? extends Type> columnTypeList) throws UncheckedIOException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

        if (N.isNullOrEmpty(columnTypeList)) {
            throw new IllegalArgumentException("columnTypeList can't be null or empty");
        }

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String[], String> lineParser = csvLineParser_TL.get();
        final BufferedReader br = csvReader instanceof BufferedReader ? (BufferedReader) csvReader : Objectory.createBufferedReader(csvReader);
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

            final String[] strs = new String[titles.length];

            while (offset-- > 0 && br.readLine() != null) {
                // continue
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(strs, line);

                if (filter != null && !filter.test(strs)) {
                    continue;
                }

                for (int i = 0, columnIndex = 0; i < columnCount; i++) {
                    if (columnTypes[i] != null) {
                        columnList.get(columnIndex++).add(columnTypes[i].valueOf(strs[i]));
                    }
                }

                count--;
            }

            return new RowDataSet(columnNameList, columnList);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (br != csvReader) {
                Objectory.recycle(br);
            }
        }
    }

    public static <T> Stream<T> stream(final Class<T> targetType, final File csvFile) {
        return stream(targetType, csvFile, (Collection<String>) null);
    }

    public static <T> Stream<T> stream(final Class<T> targetType, final File csvFile, final Collection<String> selectColumnNames) {
        return stream(targetType, csvFile, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue());
    }

    public static <T> Stream<T> stream(final Class<T> targetType, final File csvFile, final Collection<String> selectColumnNames, final long offset,
            final long count, final Predicate<String[]> filter) {
        FileReader csvReader = null;

        try {
            csvReader = IOUtil.newFileReader(csvFile);

            return stream(targetType, csvReader, selectColumnNames, offset, count, true, filter);
        } catch (Exception e) {
            if (csvReader != null) {
                IOUtil.closeQuietly(csvReader);
            }

            throw N.toRuntimeException(e);
        }
    }

    public static <T> Stream<T> stream(final Class<T> targetType, final Reader csvReader, final boolean closeReaderWhenStreamIsClosed) {
        return stream(targetType, csvReader, (Collection<String>) null, closeReaderWhenStreamIsClosed);
    }

    public static <T> Stream<T> stream(final Class<T> targetType, final Reader csvReader, final Collection<String> selectColumnNames,
            final boolean closeReaderWhenStreamIsClosed) {
        return stream(targetType, csvReader, selectColumnNames, 0, Long.MAX_VALUE, closeReaderWhenStreamIsClosed, Fn.alwaysTrue());
    }

    public static <T> Stream<T> stream(final Class<T> targetType, final Reader csvReader, final Collection<String> selectColumnNames, final long offset,
            final long count, final boolean closeReaderWhenStreamIsClosed, final Predicate<String[]> filter) {

        return Stream.defer(() -> {
            N.checkArgNotNull(targetType, "targetType");
            N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

            final BufferedReader br = csvReader instanceof BufferedReader ? (BufferedReader) csvReader : Objectory.createBufferedReader(csvReader);
            boolean noException = false;

            try {
                final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
                final BiConsumer<String[], String> lineParser = csvLineParser_TL.get();

                String line = br.readLine();

                if (line == null) {
                    noException = true;
                    return Stream.empty();
                }

                final String[] titles = headerParser.apply(line);

                final boolean isEntity = ClassUtil.isEntity(targetType);
                final EntityInfo entityInfo = isEntity ? ParserUtil.getEntityInfo(targetType) : null;

                final int columnCount = titles.length;
                final String[] resultColumnNames = new String[columnCount];
                final Set<String> selectPropNameSet = selectColumnNames == null ? null : N.newHashSet(selectColumnNames);
                final PropInfo[] propInfos = isEntity ? new PropInfo[columnCount] : null;
                int resultColumnCount = 0;

                for (int i = 0; i < columnCount; i++) {
                    if (isEntity) {
                        propInfos[i] = entityInfo.getPropInfo(titles[i]);

                        if (propInfos[i] == null) {
                            if (selectPropNameSet != null && selectPropNameSet.remove(titles[i])) {
                                throw new IllegalArgumentException(
                                        titles[i] + " is not defined in entity class: " + ClassUtil.getCanonicalClassName(targetType));
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

                if (N.notNullOrEmpty(selectPropNameSet)) {
                    throw new IllegalArgumentException(selectColumnNames + " are not included in titles: " + N.toString(titles));
                }

                long offsetTmp = offset;

                while (offsetTmp-- > 0 && br.readLine() != null) {
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
                        final Collection<Object> result = N.newCollection(targetType, finalResultColumnCount);

                        for (int i = 0; i < columnCount; i++) {
                            if (resultColumnNames[i] != null) {
                                result.add(values[i]);
                            }
                        }

                        return (T) result;
                    };

                } else if (type.isMap()) {
                    mapper = values -> {
                        final Map<String, Object> result = N.newMap(targetType, finalResultColumnCount);

                        for (int i = 0; i < columnCount; i++) {
                            if (resultColumnNames[i] != null) {
                                result.put(resultColumnNames[i], values[i]);
                            }
                        }

                        return (T) result;
                    };

                } else if (type.isEntity()) {
                    mapper = values -> {
                        final Object result = entityInfo.createEntityResult();

                        for (int i = 0; i < columnCount; i++) {
                            if (resultColumnNames[i] != null) {
                                propInfos[i].setPropValue(result, propInfos[i].readPropValue(values[i]));
                            }
                        }

                        entityInfo.finishEntityResult(result);

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

                final String[] strs = new String[titles.length];

                Stream<T> ret = ((filter == null || N.equals(filter, Fn.alwaysTrue()) || N.equals(filter, Fnn.alwaysTrue())) //
                        ? Stream.lines(br).map(it -> {
                            lineParser.accept(strs, it);
                            return strs;
                        }) //
                        : Stream.lines(br).map(it -> {
                            lineParser.accept(strs, it);
                            return strs;
                        }).filter(Fn.from(filter))) //
                                .limit(count)
                                .map(mapper)
                                .onClose(() -> {
                                    if (br != csvReader) {
                                        Objectory.recycle(br);
                                    }
                                });

                noException = true;

                return ret;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (!noException && br != csvReader) {
                    Objectory.recycle(br);
                }
            }
        }).onClose(() -> {
            if (closeReaderWhenStreamIsClosed) {
                IOUtil.close(csvReader);
            }
        });
    }
}
