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
 *
 * @author Haiyang Li
 * @since 0.8
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
     *
     *
     * @param parser
     */
    // TODO should share/use the same parser for line?
    public static void setCSVHeaderParser(final Function<String, String[]> parser) {
        N.checkArgNotNull(parser, "parser");

        csvHeaderParser_TL.set(parser);
    }

    /**
     *
     *
     * @param parser
     */
    public static void setCSVLineParser(final BiConsumer<String, String[]> parser) {
        N.checkArgNotNull(parser, "parser");

        csvLineParser_TL.set(parser);
    }

    /**
     *
     */
    public static void resetCSVHeaderParser() {
        csvHeaderParser_TL.set(defaultCsvHeadereParser);
    }

    /**
     *
     */
    public static void resetCSVLineParser() {
        csvLineParser_TL.set(defaultCsvLineParser);
    }

    /**
     *
     *
     * @return
     */
    public static Function<String, String[]> getCurrentHeaderParser() {
        return csvHeaderParser_TL.get();
    }

    /**
     *
     *
     * @return
     */
    public static BiConsumer<String, String[]> getCurrentLineParser() {
        return csvLineParser_TL.get();
    }

    /**
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final File source) throws UncheckedIOException {
        return loadCSV(source, (Collection<String>) null);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final long offset, final long count)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Throwables.Predicate<String[], E> filter) throws UncheckedIOException, E {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return loadCSV(is, selectColumnNames, offset, count, filter);
        } finally {
            IOUtil.closeQuietly(is);
        }
    }

    /**
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final InputStream source) throws UncheckedIOException {
        return loadCSV(source, (Collection<String>) null);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames, final long offset, final long count)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames, final long offset,
            final long count, final Throwables.Predicate<String[], E> filter) throws UncheckedIOException, E {
        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR

        return loadCSV(reader, selectColumnNames, offset, count, filter);
    }

    /**
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Reader source) throws UncheckedIOException {
        return loadCSV(source, (Collection<String>) null);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, long offset, long count) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue());
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, long offset, long count,
            final Throwables.Predicate<String[], E> filter) throws UncheckedIOException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count); //NOSONAR

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final BufferedReader br = source instanceof BufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);

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

            while (offset-- > 0 && br.readLine() != null) {
                // continue;
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(line, output);

                if (filter != null && !filter.test(output)) {
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (br != source) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     *
     * @param source
     * @param beanClassForColumnTypeForColumnType
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final File source, final Class<?> beanClassForColumnTypeForColumnType) throws UncheckedIOException {
        return loadCSV(source, null, beanClassForColumnTypeForColumnType);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @param beanClassForColumnType
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final Class<?> beanClassForColumnType)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE, beanClassForColumnType);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param beanClassForColumnType
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue(), beanClassForColumnType);
    }

    /**
     * Load the data from CSV.
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @param beanClassForColumnType
     *
     * @param <E>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Throwables.Predicate<String[], E> filter, final Class<?> beanClassForColumnType) throws UncheckedIOException, E {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return loadCSV(is, selectColumnNames, offset, count, filter, beanClassForColumnType);
        } finally {
            IOUtil.closeQuietly(is);
        }
    }

    /**
     *
     * @param source
     * @param beanClassForColumnType
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final InputStream source, final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, null, beanClassForColumnType);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @param beanClassForColumnType
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames, final Class<?> beanClassForColumnType)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE, beanClassForColumnType);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param beanClassForColumnType
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue(), beanClassForColumnType);
    }

    /**
     * Load the data from CSV.
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @param beanClassForColumnType
     *
     * @param <E>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final InputStream source, final Collection<String> selectColumnNames, final long offset,
            final long count, final Throwables.Predicate<String[], E> filter, final Class<?> beanClassForColumnType) throws UncheckedIOException, E {
        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR

        return loadCSV(reader, selectColumnNames, offset, count, filter, beanClassForColumnType);
    }

    /**
     *
     * @param source
     * @param beanClassForColumnType
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Reader source, final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, null, beanClassForColumnType);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @param beanClassForColumnType
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, final Class<?> beanClassForColumnType)
            throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, 0, Long.MAX_VALUE, beanClassForColumnType);
    }

    /**
     *
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param beanClassForColumnType
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, long offset, long count,
            final Class<?> beanClassForColumnType) throws UncheckedIOException {
        return loadCSV(source, selectColumnNames, offset, count, Fn.<String[]> alwaysTrue(), beanClassForColumnType);
    }

    /**
     * Load the data from CSV.
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @param beanClassForColumnType
     *
     * @param <E>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> DataSet loadCSV(final Reader source, final Collection<String> selectColumnNames, long offset, long count,
            final Throwables.Predicate<String[], E> filter, final Class<?> beanClassForColumnType) throws UncheckedIOException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final BufferedReader br = source instanceof BufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);
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

            while (offset-- > 0 && br.readLine() != null) {
                // continue
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(line, output);

                if (filter != null && !filter.test(output)) {
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (br != source) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     *
     * @param source
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(source, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     *
     * @param source
     * @param offset
     * @param count
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final long offset, final long count, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeMap);
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param source
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final File source, final long offset, final long count, final Throwables.Predicate<String[], E> filter,
            final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException, E {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return loadCSV(is, offset, count, filter, columnTypeMap);
        } finally {
            IOUtil.closeQuietly(is);
        }
    }

    /**
     *
     * @param source
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream source, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(source, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     *
     * @param source
     * @param offset
     * @param count
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream source, final long offset, final long count, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeMap);
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param source
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final InputStream source, final long offset, final long count,
            final Throwables.Predicate<String[], E> filter, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException, E {
        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR

        return loadCSV(reader, offset, count, filter, columnTypeMap);
    }

    /**
     *
     * @param source
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(source, 0, Long.MAX_VALUE, columnTypeMap);
    }

    /**
     *
     * @param source
     * @param offset
     * @param count
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, long offset, long count, final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeMap);
    }

    /**
     * Load the data from CSV.
     *
     * @param <E>
     * @param source
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeMap
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final Reader source, long offset, long count, final Throwables.Predicate<String[], E> filter,
            final Map<String, ? extends Type> columnTypeMap) throws UncheckedIOException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

        if (N.isEmpty(columnTypeMap)) {
            throw new IllegalArgumentException("columnTypeMap can't be null or empty");
        }

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final BufferedReader br = source instanceof BufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);

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

            while (offset-- > 0 && br.readLine() != null) {
                // continue
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(line, output);

                if (filter != null && !filter.test(output)) {
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (br != source) {
                Objectory.recycle(br);
            }
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
     * @param columnTypeList
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final List<? extends Type> columnTypeList) throws UncheckedIOException {
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
     * @param columnTypeList
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final File source, final long offset, final long count, final List<? extends Type> columnTypeList)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeList);
    }

    /**
     * Load the data from CSV.
     * <br />
     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV
     * <br />
     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
     *
     * @param <E>
     * @param source
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeList set the column type to null to skip the column in CSV.
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final File source, final long offset, final long count, final Throwables.Predicate<String[], E> filter,
            final List<? extends Type> columnTypeList) throws UncheckedIOException, E {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return loadCSV(is, offset, count, filter, columnTypeList);
        } finally {
            IOUtil.closeQuietly(is);
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
     * @param columnTypeList
     * @return
     * @throws UncheckedIOException the unchecked IO exception
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
     * @param columnTypeList
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final InputStream source, final long offset, final long count, final List<? extends Type> columnTypeList)
            throws UncheckedIOException {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeList);
    }

    /**
     * Load the data from CSV.
     * <br />
     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV
     * <br />
     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
     *
     * @param <E>
     * @param source
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeList set the column type to null to skip the column in CSV.
     * @return
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final InputStream source, final long offset, final long count,
            final Throwables.Predicate<String[], E> filter, final List<? extends Type> columnTypeList) throws E {
        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR

        return loadCSV(reader, offset, count, filter, columnTypeList);
    }

    /**
     * Load the data from CSV.
     * <br />
     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV
     * <br />
     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
     *
     * @param source
     * @param columnTypeList
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, final List<? extends Type> columnTypeList) {
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
     * @param columnTypeList
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static DataSet loadCSV(final Reader source, long offset, long count, final List<? extends Type> columnTypeList) {
        return loadCSV(source, offset, count, Fn.<String[]> alwaysTrue(), columnTypeList);
    }

    /**
     * Load the data from CSV.
     * <br />
     * The size of specified {@code columnTypeList} must be equal to the size of columns of the specified CSV
     * <br />
     * To skip a column in CSV, set {@code null} to position in {@code columnTypeList}.
     *
     * @param <E>
     * @param source
     * @param offset
     * @param count
     * @param filter
     * @param columnTypeList
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> DataSet loadCSV(final Reader source, long offset, long count, final Throwables.Predicate<String[], E> filter,
            final List<? extends Type> columnTypeList) throws UncheckedIOException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

        if (N.isEmpty(columnTypeList)) {
            throw new IllegalArgumentException("columnTypeList can't be null or empty");
        }

        final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
        final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();
        final BufferedReader br = source instanceof BufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);
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

            while (offset-- > 0 && br.readLine() != null) {
                // continue
            }

            while (count > 0 && (line = br.readLine()) != null) {
                lineParser.accept(line, output);

                if (filter != null && !filter.test(output)) {
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (br != source) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     *
     *
     * @param source
     * @param targetType
     * @param <T>
     * @return
     */
    public static <T> Stream<T> stream(final File source, final Class<? extends T> targetType) {
        return stream(source, (Collection<String>) null, targetType);
    }

    /**
     *
     *
     * @param source
     * @param selectColumnNames
     * @param targetType
     * @param <T>
     * @return
     */
    public static <T> Stream<T> stream(final File source, final Collection<String> selectColumnNames, final Class<? extends T> targetType) {
        return stream(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), targetType);
    }

    /**
     *
     *
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @param targetType
     * @param <T>
     * @return
     */
    public static <T> Stream<T> stream(final File source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<String[]> filter, final Class<? extends T> targetType) {
        FileReader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return stream(reader, selectColumnNames, offset, count, filter, true, targetType);
        } catch (Exception e) {
            if (reader != null) {
                IOUtil.closeQuietly(reader);
            }

            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param source
     * @param closeReaderWhenStreamIsClosed
     * @param targetType
     * @param <T>
     * @return
     */
    public static <T> Stream<T> stream(final Reader source, final boolean closeReaderWhenStreamIsClosed, final Class<? extends T> targetType) {
        return stream(source, (Collection<String>) null, closeReaderWhenStreamIsClosed, targetType);
    }

    /**
     *
     *
     * @param source
     * @param selectColumnNames
     * @param closeReaderWhenStreamIsClosed
     * @param targetType
     * @param <T>
     * @return
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames, final boolean closeReaderWhenStreamIsClosed,
            final Class<? extends T> targetType) {
        return stream(source, selectColumnNames, 0, Long.MAX_VALUE, Fn.alwaysTrue(), closeReaderWhenStreamIsClosed, targetType);
    }

    /**
     *
     *
     * @param source
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @param closeReaderWhenStreamIsClosed
     * @param targetType
     * @param <T>
     * @return
     */
    public static <T> Stream<T> stream(final Reader source, final Collection<String> selectColumnNames, final long offset, final long count,
            final Predicate<String[]> filter, final boolean closeReaderWhenStreamIsClosed, final Class<? extends T> targetType) {

        return Stream.defer(() -> {
            N.checkArgNotNull(targetType, "targetType");
            N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);

            final BufferedReader br = source instanceof BufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source);
            boolean noException = false;

            try {
                final Function<String, String[]> headerParser = csvHeaderParser_TL.get();
                final BiConsumer<String, String[]> lineParser = csvLineParser_TL.get();

                String line = br.readLine();

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

                Stream<T> ret = ((filter == null || N.equals(filter, Fn.alwaysTrue()) || N.equals(filter, Fnn.alwaysTrue())) //
                        ? Stream.lines(br).map(it -> {
                            lineParser.accept(it, output);
                            return output;
                        }) //
                        : Stream.lines(br).map(it -> {
                            lineParser.accept(it, output);
                            return output;
                        }).filter(Fn.from(filter))) //
                                .limit(count)
                                .map(mapper)
                                .onClose(() -> {
                                    if (br != source) {
                                        Objectory.recycle(br);
                                    }
                                });

                noException = true;

                return ret;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (!noException && br != source) {
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
