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

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.TypeAttrParser;

/**
 *
 * @param <T>
 */
public abstract class AbstractType<T> implements Type<T> {

    static final List<String> factoryMethodNames = List.of("valueOf", "of", "create", "parse");

    static final List<String> getValueMethodNames = List.of("value", "getValue", "get");

    static final List<String> valueFieldNames = List.of("value", "val");

    static final String ELEMENT_SEPARATOR = Strings.ELEMENT_SEPARATOR;

    static final char[] ELEMENT_SEPARATOR_CHAR_ARRAY = ELEMENT_SEPARATOR.toCharArray();

    static final String SYS_TIME = "sysTime";

    static final String NULL_STRING = "null";

    static final String STR_FOR_EMPTY_ARRAY = "[]";

    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    static final String TRUE_STRING = Boolean.TRUE.toString().intern();

    static final char[] TRUE_CHAR_ARRAY = TRUE_STRING.toCharArray();

    static final String FALSE_STRING = Boolean.FALSE.toString().intern();

    static final char[] FALSE_CHAR_ARRAY = FALSE_STRING.toCharArray();

    @SuppressWarnings("rawtypes")
    protected static final Type[] EMPTY_TYPE_ARRAY = {};

    private static final Map<String, String> separatorConvertor = new HashMap<>();

    static {
        separatorConvertor.put(".", "\\.");
        separatorConvertor.put("|", "\\|");
        separatorConvertor.put("-", "\\-");
        separatorConvertor.put("*", "\\*");
        separatorConvertor.put("?", "\\?");
        separatorConvertor.put("+", "\\+");
        separatorConvertor.put("$", "\\$");
        separatorConvertor.put("^", "\\^");
        separatorConvertor.put("\\", "\\\\");
    }

    private final String name;

    private final String xmlName;

    protected AbstractType(final String typeName) {
        String simpleName = typeName;

        if (typeName.indexOf('.') > 0 && Strings.startsWithAny(typeName, "java.lang.", "java.util.", "java.time.", "com.landawn.abacus.")) { //NOSONAR
            // generic type.

            final int index = typeName.indexOf('<');
            final String tmpTypeName = index > 0 ? typeName.substring(0, index) : typeName;

            try {
                Class<?> cls = ClassUtil.forClass(tmpTypeName);

                //noinspection ConstantValue
                if (cls != null) {
                    cls = ClassUtil.forClass(ClassUtil.getSimpleClassName(cls));

                    //noinspection ConstantValue
                    if (cls != null) {
                        simpleName = ClassUtil.getSimpleClassName(cls) + (index > 0 ? typeName.substring(index) : Strings.EMPTY);
                    }
                }
            } catch (final Exception e) {
                // ignore;
            }
        }

        name = simpleName;
        xmlName = name.replace("<", "&lt;").replace(">", "&gt;"); //NOSONAR
    }

    protected static String[] getTypeParameters(final String typeName) {
        return TypeAttrParser.parse(typeName).getTypeParameters();
    }

    protected static String[] getParameters(final String typeName) {
        return TypeAttrParser.parse(typeName).getParameters();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String declaringName() {
        //        final String name = getName();
        //        final int index = name.lastIndexOf('.');
        //
        //        if (index >= 0) {
        //            final int index2 = name.lastIndexOf('<');
        //
        //            if (index2 < 0 || index2 > index) {
        //                return name.substring(index + 1);
        //            }
        //        }
        //
        //        return name;

        return name;
    }

    @Override
    public String xmlName() {
        return xmlName;
    }

    /**
     * Checks if is a primitive type.
     *
     * @return {@code true}, if it is a primitive type
     */
    @Override
    public boolean isPrimitiveType() {
        return false;
    }

    /**
     * Checks if is a primitive wrapper.
     *
     * @return {@code true}, if it is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return false;
    }

    /**
     * Checks if is a primitive list.
     *
     * @return {@code true}, if it is a primitive list
     */
    @Override
    public boolean isPrimitiveList() {
        return false;
    }

    /**
     * Checks if is boolean.
     *
     * @return {@code true}, if it is boolean
     */
    @Override
    public boolean isBoolean() {
        return false;
    }

    /**
     * Checks if is number.
     *
     * @return {@code true}, if it is number
     */
    @Override
    public boolean isNumber() {
        return false;
    }

    /**
     * Checks if is string.
     *
     * @return {@code true}, if it is string
     */
    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean isCharSequence() {
        return false;
    }

    /**
     * Checks if this is type of {@code Date}.
     *
     * @return {@code true}, if it is {@code Date} type
     */
    @Override
    public boolean isDate() {
        return false;
    }

    /**
     * Checks if this is type of {@code Calendar}.
     *
     * @return {@code true}, if it is {@code Calendar} type
     */
    @Override
    public boolean isCalendar() {
        return false;
    }

    /**
     * Checks if this is a type of Joda {@code DateTime}.
     *
     * @return {@code true}, if it is Joda {@code DateTime} type
     */
    @Override
    public boolean isJodaDateTime() {
        return false;
    }

    /**
     * Checks if this is a type of primitive {@code array}.
     *
     * @return {@code true}, if it is a primitive {@code array} type
     */
    @Override
    public boolean isPrimitiveArray() {
        return false;
    }

    /**
     * Checks if this is type of {@code byte[]}.
     *
     * @return {@code true}, if it is {@code byte[]} type
     */
    @Override
    public boolean isPrimitiveByteArray() {
        return false;
    }

    /**
     * Checks if this is a type of object {@code array}.
     *
     * @return {@code true}, if it is object {@code array} type
     */
    @Override
    public boolean isObjectArray() {
        return false;
    }

    /**
     * Checks if this is type of {@code array}.
     *
     * @return {@code true}, if it is {@code array} type
     */
    @Override
    public boolean isArray() {
        return false;
    }

    /**
     * Checks if this is type of {@code List}.
     *
     * @return {@code true}, if it is {@code List} type
     */
    @Override
    public boolean isList() {
        return false;
    }

    /**
     * Checks if this is type of {@code Set}.
     *
     * @return {@code true}, if it is {@code Set} type
     */
    @Override
    public boolean isSet() {
        return false;
    }

    /**
     * Checks if this is type of {@code Collection}.
     *
     * @return {@code true}, if it is {@code Collection} type
     */
    @Override
    public boolean isCollection() {
        return false;
    }

    /**
     * Checks if this is type of {@code Map}.
     *
     * @return {@code true}, if it is {@code Map} type
     */
    @Override
    public boolean isMap() {
        return false;
    }

    /**
     * Checks if this is type of {@code Bean}.
     *
     * @return {@code true}, if it is {@code Bean} type
     */
    @Override
    public boolean isBean() {
        return false;
    }

    /**
     * Checks if this is type of {@code MapEntity}.
     *
     * @return {@code true}, if it is {@code MapEntity} type
     */
    @Override
    public boolean isMapEntity() {
        return false;
    }

    /**
     * Checks if this is type of {@code EntityId}.
     *
     * @return {@code true}, if it is {@code EntityId} type
     */
    @Override
    public boolean isEntityId() {
        return false;
    }

    /**
     * Checks if this is type of {@code DataSet}.
     *
     * @return {@code true}, if it is {@code DataSet} type
     */
    @Override
    public boolean isDataSet() {
        return false;
    }

    /**
     * Checks if this is type of {@code InputStream}.
     *
     * @return {@code true}, if it is {@code InputStream} type
     */
    @Override
    public boolean isInputStream() {
        return false;
    }

    /**
     * Checks if this is type of {@code Reader}.
     *
     * @return {@code true}, if it is {@code Reader} type
     */
    @Override
    public boolean isReader() {
        return false;
    }

    /**
     * Checks if this is type of {@code ByteBuffer}.
     *
     * @return {@code true}, if it is {@code ByteBuffer} type
     */
    @Override
    public boolean isByteBuffer() {
        return false;
    }

    /**
     * Checks if this is a generic type.
     *
     * @return {@code true}, if it is a generic type
     */
    @Override
    public boolean isGenericType() {
        return N.notEmpty(getParameterTypes());
    }

    /**
     * Checks if this is an immutable type.
     *
     * @return {@code true}, if it is immutable
     */
    @Override
    public boolean isImmutable() {
        return false;
    }

    /**
     * Checks if this is a comparable type.
     *
     * @return {@code true}, if it is comparable
     */
    @Override
    public boolean isComparable() {
        return false;
    }

    /**
     * Checks if this is a serializable type.
     *
     * @return {@code true}, if it is serializable
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Gets the serialization type.
     *
     * @return
     */
    @Override
    public SerializationType getSerializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.UNKNOWN;
    }

    /**
     * Checks if is optional or {@code nullable}.
     *
     * @return {@code true}, if it is optional or nullable
     */
    @Override
    public boolean isOptionalOrNullable() {
        return false;
    }

    @Override
    public boolean isObjectType() {
        return false;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<?> getElementType() {
        return null; // NOSONAR
    }

    /**
     * Gets the parameter types.
     *
     * @return
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return AbstractType.EMPTY_TYPE_ARRAY;
    }

    @Override
    public T defaultValue() {
        return null; // NOSONAR
    }

    /**
     *
     * @param value
     * @return
     */
    @Override
    public boolean isDefaultValue(final T value) {
        return N.equals(defaultValue(), value);
    }

    /**
     *
     * @param x
     * @param y
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public int compare(final T x, final T y) {
        if (isComparable()) {
            return (x == null) ? ((y == null) ? 0 : (-1)) : ((y == null) ? 1 : ((Comparable<? super T>) x).compareTo(y));
        } else {
            throw new UnsupportedOperationException(name() + " doesn't support compare Operation");
        }
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public T valueOf(final Object obj) {
        return valueOf(obj == null ? null : N.typeOf(obj.getClass()).stringOf(obj));
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public T valueOf(final char[] cbuf, final int offset, final int len) {
        return valueOf(cbuf == null ? null : String.valueOf(cbuf, offset, len));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public T get(final ResultSet rs, final int columnIndex) throws SQLException {
        return valueOf(rs.getString(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public T get(final ResultSet rs, final String columnLabel) throws SQLException {
        return valueOf(rs.getString(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x) throws SQLException {
        stmt.setString(columnIndex, stringOf(x));
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x) throws SQLException {
        stmt.setString(parameterName, stringOf(x));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x, final int sqlTypeOrLength) throws SQLException {
        this.set(stmt, columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x, final int sqlTypeOrLength) throws SQLException {
        this.set(stmt, parameterName, x);
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final T x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(stringOf(x));
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getStringQuotation();

            if (ch == 0) {
                writer.writeCharacter(stringOf(x));
            } else {
                writer.write(ch);
                writer.writeCharacter(stringOf(x));
                writer.write(ch);
            }
        }
    }

    /**
     * Collection 2 array.
     *
     * @param c
     * @return
     */
    @Override
    public T collection2Array(final Collection<?> c) {
        throw new UnsupportedOperationException(name() + " doesn't support collection2Array Operation");
    }

    /**
     *
     * @param <E>
     * @param array
     * @param collClass
     * @return
     */
    @Override
    public <E> Collection<E> array2Collection(final T array, final Class<?> collClass) {
        throw new UnsupportedOperationException(name() + " doesn't support array2Collection Operation");
    }

    /**
     *
     * @param <E>
     * @param array
     * @param output
     */
    @Override
    public <E> void array2Collection(final T array, final Collection<E> output) {
        throw new UnsupportedOperationException(name() + " doesn't support array2Collection Operation");
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public int hashCode(final T x) {
        return N.hashCode(x);
    }

    /**
     * Deep hash code.
     *
     * @param x
     * @return
     */
    @Override
    public int deepHashCode(final T x) {
        return N.hashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return {@code true}, if successful
     */
    @Override
    public boolean equals(final T x, final T y) {
        return N.equals(x, y);
    }

    /**
     *
     * @param x
     * @param y
     * @return {@code true}, if successful
     */
    @Override
    public boolean deepEquals(final T x, final T y) {
        return N.equals(x, y);
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String toString(final T x) {
        return N.toString(x);
    }

    /**
     * Deep to string.
     *
     * @param x
     * @return
     */
    @Override
    public String deepToString(final T x) {
        return N.toString(x);
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }

    /**
     * Returns {@code true} if the specified {@code Object} is a type and its name equals with this type's name.
     *
     * @param obj
     * @return boolean
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AbstractType) {
            final AbstractType<T> another = ((AbstractType<T>) obj);
            return N.equals(another.name(), this.name()) && N.equals(another.declaringName(), this.declaringName()) && another.clazz().equals(clazz());
        }

        return false;
    }

    /**
     * Returns the type name.
     *
     * @return String
     */
    @Override
    public String toString() {
        return name();
    }

    /**
     *
     * @param st
     * @param separator
     * @return
     */
    protected static String[] split(final String st, final String separator) {
        final String newValue = separatorConvertor.get(separator);

        return (newValue == null) ? st.split(separator) : st.split(newValue);
    }

    protected static boolean isNullDateTime(final String date) {
        return Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date));
    }

    protected static boolean isPossibleLong(final CharSequence dateTime) {
        final int len = dateTime.length();

        if (len > 4) {
            char ch = dateTime.charAt(2);

            if (ch >= '0' && ch <= '9') {
                ch = dateTime.charAt(4);

                return ch >= '0' && ch <= '9';
            }
        }

        return false;
    }

    protected static boolean isPossibleLong(final char[] cbuf, final int offset, final int len) {
        if (len > 4) {
            char ch = cbuf[offset + 2];

            if (ch >= '0' && ch <= '9') {
                ch = cbuf[offset + 4];

                return ch >= '0' && ch <= '9';
            }
        }

        return false;
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     * @throws NumberFormatException the number format exception
     * @see {@link Integer#parseInt(String)}
     */
    protected static int parseInt(final char[] cbuf, final int offset, final int len) throws NumberFormatException {
        if (offset < 0 || len < 0) {
            throw new IllegalArgumentException("'offset' and 'len' can't be negative");
        }

        if (len == 0 || (N.isEmpty(cbuf) && offset == 0)) {
            return 0;
        }

        switch (len) {
            case 1: {
                final char ch = cbuf[offset];
                if (ch < '0' || ch > '9') {
                    throw new NumberFormatException("Invalid numeric String: \"" + ch + "\""); //NOSONAR
                }

                return ch - '0';
            }

            case 2, 3, 4, 5, 6, 7, 8, 9: {
                final boolean isNegative = cbuf[offset] == '-';

                int result = 0;
                char ch = 0;

                for (int i = (cbuf[offset] == '-' || cbuf[offset] == '+') ? offset + 1 : offset, to = offset + len; i < to; i++) {
                    ch = cbuf[i];

                    if (ch < '0' || ch > '9') {
                        throw new NumberFormatException("Invalid numeric String: \"" + new String(cbuf, offset, len) + "\"");
                    }

                    result = result * 10 + (ch - '0');
                }

                return isNegative ? -result : result;
            }

            default:
                return Numbers.toInt(new String(cbuf, offset, len));
        }
    }

    /**
     * Parses the long.
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     * @throws NumberFormatException the number format exception
     * @see {@link Long#parseLong(String)}
     */
    protected static long parseLong(final char[] cbuf, final int offset, final int len) throws NumberFormatException {
        if (offset < 0 || len < 0) {
            throw new IllegalArgumentException("'offset' and 'len' can't be negative");
        }

        if (len == 0 || (N.isEmpty(cbuf) && offset == 0)) {
            return 0;
        }

        switch (len) {
            case 1: {
                final char ch = cbuf[offset];
                if (ch < '0' || ch > '9') {
                    throw new NumberFormatException("Invalid numeric String: \"" + ch + "\"");
                }

                return ch - '0'; //NOSONAR
            }

            case 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18: {
                final boolean isNegative = cbuf[offset] == '-';

                long result = 0;
                char ch = 0;

                for (int i = (cbuf[offset] == '-' || cbuf[offset] == '+') ? offset + 1 : offset, to = offset + len; i < to; i++) {
                    ch = cbuf[i];

                    if (ch < '0' || ch > '9') {
                        throw new NumberFormatException("Invalid numeric String: \"" + new String(cbuf, offset, len) + "\"");
                    }

                    result = result * 10 + (ch - '0');
                }

                return isNegative ? -result : result;
            }

            default:
                return Numbers.toLong(new String(cbuf, offset, len));
        }
    }

    /**
     * Gets the column value.
     * @param rs
     * @param columnIndex
     * @param targetClass
     *
     * @param <T>
     * @return
     * @throws SQLException the SQL exception
     */
    protected static <T> T getColumnValue(final ResultSet rs, final int columnIndex, final Class<? extends T> targetClass) throws SQLException {
        return N.<T> typeOf(targetClass).get(rs, columnIndex);
    }

    /**
     * Gets the column value.
     * @param rs
     * @param columnLabel
     * @param targetClass
     *
     * @param <T>
     * @return
     * @throws SQLException the SQL exception
     */
    protected static <T> T getColumnValue(final ResultSet rs, final String columnLabel, final Class<? extends T> targetClass) throws SQLException {
        return N.<T> typeOf(targetClass).get(rs, columnLabel);
    }

    protected static int calculateBufferSize(final int len, final int elementPlusDelimiterLen) {
        return len > Integer.MAX_VALUE / elementPlusDelimiterLen ? Integer.MAX_VALUE : len * elementPlusDelimiterLen;
    }
}
