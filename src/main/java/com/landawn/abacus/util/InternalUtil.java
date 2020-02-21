/*
 * Copyright (c) 2019, Haiyang Li.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.DirtyMarker;
import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.core.DirtyMarkerUtil;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;

/**  
 * 
 * @deprecated DO NOT call the methods defined in this class. it's for internal use only.
 */
@Deprecated
@Internal
@Beta
public final class InternalUtil {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(InternalUtil.class);

    private InternalUtil() {
        // singleton for utility class
    }

    /**
     * Unconditionally close an <code>ResultSet</code>.
     * <p>
     * Equivalent to {@link ResultSet#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param rs
     */
    public static void closeQuietly(final ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                logger.error("Failed to close ResultSet", e);
            }
        }
    }

    /**
     * Unconditionally close an <code>Statement</code>.
     * <p>
     * Equivalent to {@link Statement#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param stmt
     */
    public static void closeQuietly(final Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                logger.error("Failed to close Statement", e);
            }
        }
    }

    /**
     * Unconditionally close an <code>Connection</code>.
     * <p>
     * Equivalent to {@link Connection#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param conn
     */
    public static void closeQuietly(final Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                logger.error("Failed to close Connection", e);
            }
        }
    }

    /**
     *
     * @param rs
     * @param n the count of row to move ahead.
     * @return
     * @throws SQLException the SQL exception
     */
    public static int skip(final ResultSet rs, int n) throws SQLException {
        return skip(rs, (long) n);
    }

    /**
     *
     * @param rs
     * @param n the count of row to move ahead.
     * @return
     * @throws SQLException the SQL exception
     * @see {@link ResultSet#absolute(int)}
     */
    public static int skip(final ResultSet rs, long n) throws SQLException {
        if (n <= 0) {
            return 0;
        } else if (n == 1) {
            return rs.next() == true ? 1 : 0;
        } else {
            final int currentRow = rs.getRow();

            if (n <= Integer.MAX_VALUE) {
                try {
                    if (n > Integer.MAX_VALUE - rs.getRow()) {
                        while (n-- > 0L && rs.next()) {
                        }
                    } else {
                        rs.absolute((int) n + rs.getRow());
                    }
                } catch (SQLException e) {
                    while (n-- > 0L && rs.next()) {
                    }
                }
            } else {
                while (n-- > 0L && rs.next()) {
                }
            }

            return rs.getRow() - currentRow;
        }
    }

    public static List<String> getColumnLabelList(ResultSet rs) throws SQLException {
        final ResultSetMetaData metaData = rs.getMetaData();
        final int columnCount = metaData.getColumnCount();
        final List<String> labelList = new ArrayList<>(columnCount);

        for (int i = 1, n = columnCount + 1; i < n; i++) {
            labelList.add(getColumnLabel(metaData, i));
        }

        return labelList;
    }

    /**
     * Gets the column label.
     *
     * @param rsmd
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    public static String getColumnLabel(final ResultSetMetaData rsmd, final int columnIndex) throws SQLException {
        final String result = rsmd.getColumnLabel(columnIndex);

        return N.isNullOrEmpty(result) ? rsmd.getColumnName(columnIndex) : result;
    }

    /**
     * Gets the column index.
     *
     * @param resultSet
     * @param columnName
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int getColumnIndex(final ResultSet resultSet, final String columnName) throws UncheckedSQLException {
        int columnIndex = -1;

        try {
            final ResultSetMetaData rsmd = resultSet.getMetaData();
            final int columnCount = rsmd.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                if (getColumnLabel(rsmd, i).equals(columnName)) {
                    columnIndex = i - 1;
                    break;
                }
            }
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }

        N.checkArgument(columnIndex >= 0, "No column found by name %s", columnName);

        return columnIndex;
    }

    /**
     * Gets the column value.
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    public static Object getColumnValue(final ResultSet rs, final int columnIndex) throws SQLException {
        // Copied from JdbcUtils#getResultSetValue(ResultSet, int) in SpringJdbc under Apache License, Version 2.0.
        //    final Object obj = rs.getObject(columnIndex);
        //
        //    if (obj == null) {
        //        return obj;
        //    }
        //
        //    final String className = obj.getClass().getName();
        //
        //    if (obj instanceof Blob) {
        //        final Blob blob = (Blob) obj;
        //        return blob.getBytes(1, (int) blob.length());
        //    } else if (obj instanceof Clob) {
        //        final Clob clob = (Clob) obj;
        //        return clob.getSubString(1, (int) clob.length());
        //    } else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
        //        return rs.getTimestamp(columnIndex);
        //    } else if (className.startsWith("oracle.sql.DATE")) {
        //        final String columnClassName = rs.getMetaData().getColumnClassName(columnIndex);
        //
        //        if ("java.sql.Timestamp".equals(columnClassName) || "oracle.sql.TIMESTAMP".equals(columnClassName)) {
        //            return rs.getTimestamp(columnIndex);
        //        } else {
        //            return rs.getDate(columnIndex);
        //        }
        //    } else if (obj instanceof java.sql.Date) {
        //        if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(columnIndex))) {
        //            return rs.getTimestamp(columnIndex);
        //        }
        //    }
        //
        //    return obj;

        return rs.getObject(columnIndex);
    }

    /**
     * Gets the column value.
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    public static Object getColumnValue(final ResultSet rs, final String columnLabel) throws SQLException {
        // Copied from JdbcUtils#getResultSetValue(ResultSet, int) in SpringJdbc under Apache License, Version 2.0.
        //    final Object obj = rs.getObject(columnLabel);
        //
        //    if (obj == null) {
        //        return obj;
        //    }
        //
        //    final String className = obj.getClass().getName();
        //
        //    if (obj instanceof Blob) {
        //        final Blob blob = (Blob) obj;
        //        return blob.getBytes(1, (int) blob.length());
        //    } else if (obj instanceof Clob) {
        //        final Clob clob = (Clob) obj;
        //        return clob.getSubString(1, (int) clob.length());
        //    } else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
        //        return rs.getTimestamp(columnLabel);
        //    } else if (className.startsWith("oracle.sql.DATE")) {
        //        final int columnIndex = JdbcUtil.getColumnLabelList(rs).indexOf(columnLabel);
        //
        //        if (columnIndex >= 0) {
        //            final String columnClassName = rs.getMetaData().getColumnClassName(columnIndex + 1);
        //
        //            if ("java.sql.Timestamp".equals(columnClassName) || "oracle.sql.TIMESTAMP".equals(columnClassName)) {
        //                return rs.getTimestamp(columnLabel);
        //            } else {
        //                return rs.getDate(columnLabel);
        //            }
        //        }
        //    } else if (obj instanceof java.sql.Date) {
        //        final int columnIndex = JdbcUtil.getColumnLabelList(rs).indexOf(columnLabel);
        //
        //        if (columnIndex >= 0) {
        //            if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(columnIndex + 1))) {
        //                return rs.getTimestamp(columnLabel);
        //            }
        //        }
        //    }
        //
        //    return obj;

        return rs.getObject(columnLabel);
    }

    /**
     * Don't cache or reuse the returned {@code BiRowMapper} instance. It's stateful.
     *
     * @param <T>
     * @param targetClass
     * @return
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Throwables.BiFunction<ResultSet, List<String>, T, SQLException> to(Class<? extends T> targetClass) {
        return to(targetClass, false);
    }

    /**
     * Don't cache or reuse the returned {@code BiRowMapper} instance. It's stateful.
     *
     * @param <T>
     * @param targetClass
     * @param ignoreNonMatchedColumns
     * @return
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Throwables.BiFunction<ResultSet, List<String>, T, SQLException> to(Class<? extends T> targetClass,
            final boolean ignoreNonMatchedColumns) {
        if (Object[].class.isAssignableFrom(targetClass)) {
            return new Throwables.BiFunction<ResultSet, List<String>, T, SQLException>() {
                @Override
                public T apply(ResultSet rs, List<String> columnLabelList) throws SQLException {
                    final int columnCount = columnLabelList.size();
                    final Object[] a = Array.newInstance(targetClass.getComponentType(), columnCount);

                    for (int i = 0; i < columnCount; i++) {
                        a[i] = InternalUtil.getColumnValue(rs, i + 1);
                    }

                    return (T) a;
                }
            };
        } else if (List.class.isAssignableFrom(targetClass)) {
            return new Throwables.BiFunction<ResultSet, List<String>, T, SQLException>() {
                private final boolean isListOrArrayList = targetClass.equals(List.class) || targetClass.equals(ArrayList.class);

                @Override
                public T apply(ResultSet rs, List<String> columnLabelList) throws SQLException {
                    final int columnCount = columnLabelList.size();
                    final List<Object> c = isListOrArrayList ? new ArrayList<>(columnCount) : (List<Object>) N.newInstance(targetClass);

                    for (int i = 0; i < columnCount; i++) {
                        c.add(InternalUtil.getColumnValue(rs, i + 1));
                    }

                    return (T) c;
                }
            };
        } else if (Map.class.isAssignableFrom(targetClass)) {
            return new Throwables.BiFunction<ResultSet, List<String>, T, SQLException>() {
                private final boolean isMapOrHashMap = targetClass.equals(Map.class) || targetClass.equals(HashMap.class);
                private final boolean isLinkedHashMap = targetClass.equals(LinkedHashMap.class);
                private volatile String[] columnLabels = null;

                @Override
                public T apply(ResultSet rs, List<String> columnLabelList) throws SQLException {
                    final int columnCount = columnLabelList.size();
                    String[] columnLabels = this.columnLabels;

                    if (columnLabels == null) {
                        columnLabels = columnLabelList.toArray(new String[columnCount]);
                        this.columnLabels = columnLabels;
                    }

                    final Map<String, Object> m = isMapOrHashMap ? new HashMap<>(columnCount)
                            : (isLinkedHashMap ? new LinkedHashMap<>(columnCount) : (Map<String, Object>) N.newInstance(targetClass));

                    for (int i = 0; i < columnCount; i++) {
                        m.put(columnLabels[i], InternalUtil.getColumnValue(rs, i + 1));
                    }

                    return (T) m;
                }
            };
        } else if (ClassUtil.isEntity(targetClass)) {
            return new Throwables.BiFunction<ResultSet, List<String>, T, SQLException>() {
                private final boolean isDirtyMarker = DirtyMarkerUtil.isDirtyMarker(targetClass);
                private final EntityInfo entityInfo = ParserUtil.getEntityInfo(targetClass);
                private volatile String[] columnLabels = null;
                private volatile PropInfo[] propInfos;
                private volatile Type<?>[] columnTypes = null;

                @Override
                public T apply(ResultSet rs, List<String> columnLabelList) throws SQLException {
                    final int columnCount = columnLabelList.size();

                    String[] columnLabels = this.columnLabels;
                    PropInfo[] propInfos = this.propInfos;
                    Type<?>[] columnTypes = this.columnTypes;

                    if (columnLabels == null) {
                        columnLabels = columnLabelList.toArray(new String[columnCount]);
                        this.columnLabels = columnLabels;
                    }

                    if (columnTypes == null || propInfos == null) {
                        final Map<String, String> column2FieldNameMap = getColumn2FieldNameMap(targetClass);

                        propInfos = new PropInfo[columnCount];
                        columnTypes = new Type[columnCount];

                        for (int i = 0; i < columnCount; i++) {
                            propInfos[i] = entityInfo.getPropInfo(columnLabels[i]);

                            if (propInfos[i] == null) {
                                String fieldName = column2FieldNameMap.get(columnLabels[i]);

                                if (N.isNullOrEmpty(fieldName)) {
                                    fieldName = column2FieldNameMap.get(columnLabels[i].toLowerCase());
                                }

                                if (N.notNullOrEmpty(fieldName)) {
                                    propInfos[i] = entityInfo.getPropInfo(fieldName);
                                }
                            }

                            if (propInfos[i] == null) {
                                if (ignoreNonMatchedColumns) {
                                    columnLabels[i] = null;
                                } else {
                                    throw new IllegalArgumentException(
                                            "No property in class: " + ClassUtil.getCanonicalClassName(targetClass) + " mapping to column: " + columnLabels[i]);
                                }
                            } else {
                                columnTypes[i] = entityInfo.getPropInfo(columnLabels[i]).dbType;
                            }
                        }

                        this.propInfos = propInfos;
                        this.columnTypes = columnTypes;
                    }

                    final Object entity = N.newInstance(targetClass);

                    for (int i = 0; i < columnCount; i++) {
                        if (columnLabels[i] == null) {
                            continue;
                        }

                        propInfos[i].setPropValue(entity, columnTypes[i].get(rs, i + 1));
                    }

                    if (isDirtyMarker) {
                        DirtyMarkerUtil.markDirty((DirtyMarker) entity, false);
                    }

                    return (T) entity;
                }
            };
        } else {
            return new Throwables.BiFunction<ResultSet, List<String>, T, SQLException>() {
                private final Type<? extends T> targetType = N.typeOf(targetClass);
                private int columnCount = 0;

                @Override
                public T apply(ResultSet rs, List<String> columnLabelList) throws SQLException {
                    if (columnCount != 1 && (columnCount = columnLabelList.size()) != 1) {
                        throw new IllegalArgumentException(
                                "It's not supported to retrieve value from multiple columns: " + columnLabelList + " for type: " + targetClass);
                    }

                    return targetType.get(rs, 1);
                }
            };
        }
    }

    /** The Constant column2FieldNameMapPool. */
    private static final Map<Class<?>, ImmutableMap<String, String>> column2FieldNameMapPool = new ConcurrentHashMap<>();

    /**
     * Gets the column 2 field name map.
     *
     * @param entityClass
     * @return
     */
    public static ImmutableMap<String, String> getColumn2FieldNameMap(Class<?> entityClass) {
        ImmutableMap<String, String> result = column2FieldNameMapPool.get(entityClass);

        if (result == null) {
            final Map<String, String> biMap = N.newBiMap(LinkedHashMap.class, LinkedHashMap.class);
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);

            for (PropInfo propInfo : entityInfo.propInfoList) {
                if (propInfo.columnName.isPresent()) {
                    biMap.put(propInfo.columnName.get(), propInfo.name);
                    biMap.put(propInfo.columnName.get().toLowerCase(), propInfo.name);
                    biMap.put(propInfo.columnName.get().toUpperCase(), propInfo.name);
                }
            }

            result = ImmutableMap.of(biMap);

            column2FieldNameMapPool.put(entityClass, result);
        }

        return result;
    }

    /** The Constant listElementDataField. */
    // ...
    static final Field listElementDataField;

    /** The Constant listSizeField. */
    static final Field listSizeField;

    /** The is list element data field gettable. */
    static volatile boolean isListElementDataFieldGettable = true;

    /** The is list element data field settable. */
    static volatile boolean isListElementDataFieldSettable = true;

    static {
        Field tmp = null;

        try {
            tmp = ArrayList.class.getDeclaredField("elementData");
        } catch (Throwable e) {
            // ignore.
        }

        listElementDataField = tmp != null && tmp.getType().equals(Object[].class) ? tmp : null;

        if (listElementDataField != null) {
            ClassUtil.setAccessibleQuietly(listElementDataField, true);
        }

        tmp = null;

        try {
            tmp = ArrayList.class.getDeclaredField("size");
        } catch (Throwable e) {
            // ignore.
        }

        listSizeField = tmp != null && tmp.getType().equals(int.class) ? tmp : null;

        if (listSizeField != null) {
            ClassUtil.setAccessibleQuietly(listSizeField, true);
        }
    }

    /**
     * 
     * @param c
     * @return
     * @deprecated internal use only
     */
    @Deprecated
    static Object[] getInternalArray(final Collection<?> c) {
        if (c == null) {
            return null;
        }

        if (isListElementDataFieldGettable && listElementDataField != null && c.getClass().equals(ArrayList.class)) {
            try {
                return (Object[]) listElementDataField.get(c);
            } catch (Throwable e) {
                // ignore;
                isListElementDataFieldGettable = false;
            }

        }

        return null;
    }

    /**
     * Create an array list by initializing its elements data with the specified array <code>a</code>.
     * The returned list may share the same elements with the specified array <code>a</code>.
     * That's to say any change on the List/Array will affect the Array/List.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    static <T> List<T> createList(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        if (isListElementDataFieldSettable && listElementDataField != null && listSizeField != null) {
            final List<T> list = new ArrayList<>();

            try {
                listElementDataField.set(list, a);
                listSizeField.set(list, a.length);

                return list;
            } catch (Throwable e) {
                // ignore;
                isListElementDataFieldSettable = false;
            }
        }

        return CommonUtil.asList(a);
    }

    /** The Constant strValueField. */
    static final Field strValueField;

    /** The is string chars gettable. */
    static volatile boolean isStringCharsGettable = true;

    /** The Constant sharedStringConstructor. */
    static final Constructor<String> sharedStringConstructor;

    static {
        Field tmp = null;

        strValueField = ((tmp != null) && tmp.getName().equals("value") && tmp.getType().equals(char[].class)) ? tmp : null;

        if (strValueField != null) {
            ClassUtil.setAccessibleQuietly(strValueField, true);
        }

        Constructor<String> tmpConstructor = null;

        try {
            tmpConstructor = String.class.getDeclaredConstructor(char[].class, boolean.class);
            ClassUtil.setAccessibleQuietly(tmpConstructor, true);
        } catch (Exception e) {
            // ignore.
        }

        sharedStringConstructor = tmpConstructor;
    }

    /**
     * Gets the chars for read only.
     *
     * @param str
     * @return
     */
    public static char[] getCharsForReadOnly(final String str) {
        if (isStringCharsGettable && strValueField != null && str.length() > 3) {
            try {
                final char[] chars = (char[]) strValueField.get(str);

                if (chars.length == str.length()) {
                    return chars;
                } else {
                    isStringCharsGettable = false;
                }

            } catch (Exception e) {
                // ignore.
                isStringCharsGettable = false;
            }
        }

        return str.toCharArray();
    }

    /**
     *
     * @param a the specified array should not be modified after it's used to
     *            create the new String.
     * @param share the same array will be shared with the new created ArrayList
     *            if it's true.
     * @return
     */ 
    static String newString(final char[] a, final boolean share) {
        if (share && sharedStringConstructor != null) {
            try {
                return sharedStringConstructor.newInstance(a, true);
            } catch (Exception e) {
                throw N.toRuntimeException(e);
            }
        } else {
            return String.valueOf(a);
        }
    }
}