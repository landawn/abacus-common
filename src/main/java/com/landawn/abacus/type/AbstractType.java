/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public abstract class AbstractType<T> implements Type<T> {
    static final String ELEMENT_SEPARATOR = ", ".intern();

    static final char[] ELEMENT_SEPARATOR_CHAR_ARRAY = ELEMENT_SEPARATOR.toCharArray();

    static final String SYS_TIME = "sysTime";

    static final String NULL_STRING = "null".intern();

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

    protected AbstractType(String typeName) {
        String simpleName = typeName;

        if (typeName.indexOf('.') > 0) { //NOSONAR
            // generic type.

            int index = typeName.indexOf('<');
            String tmpTypeName = index > 0 ? typeName.substring(0, index) : typeName;

            try {
                Class<?> cls = ClassUtil.forClass(tmpTypeName);
                if (cls != null) {
                    cls = ClassUtil.forClass(ClassUtil.getSimpleClassName(cls));
                    if (cls != null) {
                        simpleName = ClassUtil.getSimpleClassName(cls) + (index > 0 ? typeName.substring(index) : Strings.EMPTY_STRING);
                    }
                }
            } catch (Exception e) {
                // ignore;
            }
        }

        name = simpleName;
        xmlName = name.replaceAll("<", "&lt;").replaceAll(">", "&gt;"); //NOSONAR
    }

    protected static String[] getTypeParameters(String typeName) {
        return TypeAttrParser.parse(typeName).getTypeParameters();
    }

    protected static String[] getParameters(String typeName) {
        return TypeAttrParser.parse(typeName).getParameters();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String name() {
        return name;
    }

    /**
     *
     *
     * @return
     */
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

    /**
     *
     *
     * @return
     */
    @Override
    public String xmlName() {
        return xmlName;
    }

    /**
     * Checks if is primitive type.
     *
     * @return true, if is primitive type
     */
    @Override
    public boolean isPrimitiveType() {
        return false;
    }

    /**
     * Checks if is primitive wrapper.
     *
     * @return true, if is primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return false;
    }

    /**
     * Checks if is primitive list.
     *
     * @return true, if is primitive list
     */
    @Override
    public boolean isPrimitiveList() {
        return false;
    }

    /**
     * Checks if is boolean.
     *
     * @return true, if is boolean
     */
    @Override
    public boolean isBoolean() {
        return false;
    }

    /**
     * Checks if is number.
     *
     * @return true, if is number
     */
    @Override
    public boolean isNumber() {
        return false;
    }

    /**
     * Checks if is string.
     *
     * @return true, if is string
     */
    @Override
    public boolean isString() {
        return false;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public boolean isCharSequence() {
        return false;
    }

    /**
     * Checks if is date.
     *
     * @return true, if is date
     */
    @Override
    public boolean isDate() {
        return false;
    }

    /**
     * Checks if is calendar.
     *
     * @return true, if is calendar
     */
    @Override
    public boolean isCalendar() {
        return false;
    }

    /**
     * Checks if is joda date time.
     *
     * @return true, if is joda date time
     */
    @Override
    public boolean isJodaDateTime() {
        return false;
    }

    /**
     * Checks if is primitive array.
     *
     * @return true, if is primitive array
     */
    @Override
    public boolean isPrimitiveArray() {
        return false;
    }

    /**
     * Checks if is primitive byte array.
     *
     * @return true, if is primitive byte array
     */
    @Override
    public boolean isPrimitiveByteArray() {
        return false;
    }

    /**
     * Checks if is object array.
     *
     * @return true, if is object array
     */
    @Override
    public boolean isObjectArray() {
        return false;
    }

    /**
     * Checks if is array.
     *
     * @return true, if is array
     */
    @Override
    public boolean isArray() {
        return false;
    }

    /**
     * Checks if is list.
     *
     * @return true, if is list
     */
    @Override
    public boolean isList() {
        return false;
    }

    /**
     * Checks if is sets the.
     *
     * @return true, if is sets the
     */
    @Override
    public boolean isSet() {
        return false;
    }

    /**
     * Checks if is collection.
     *
     * @return true, if is collection
     */
    @Override
    public boolean isCollection() {
        return false;
    }

    /**
     * Checks if is map.
     *
     * @return true, if is map
     */
    @Override
    public boolean isMap() {
        return false;
    }

    /**
     * Checks if is bean.
     *
     * @return true, if is bean
     */
    @Override
    public boolean isBean() {
        return false;
    }

    /**
     * Checks if is map bean.
     *
     * @return true, if is map bean
     */
    @Override
    public boolean isMapEntity() {
        return false;
    }

    /**
     * Checks if is bean id.
     *
     * @return true, if is bean id
     */
    @Override
    public boolean isEntityId() {
        return false;
    }

    /**
     * Checks if is data set.
     *
     * @return true, if is data set
     */
    @Override
    public boolean isDataSet() {
        return false;
    }

    /**
     * Checks if is input stream.
     *
     * @return true, if is input stream
     */
    @Override
    public boolean isInputStream() {
        return false;
    }

    /**
     * Checks if is reader.
     *
     * @return true, if is reader
     */
    @Override
    public boolean isReader() {
        return false;
    }

    /**
     * Checks if is byte buffer.
     *
     * @return true, if is byte buffer
     */
    @Override
    public boolean isByteBuffer() {
        return false;
    }

    /**
     * Checks if is generic type.
     *
     * @return true, if is generic type
     */
    @Override
    public boolean isGenericType() {
        return N.notEmpty(getParameterTypes());
    }

    /**
     * Checks if is immutable.
     *
     * @return true, if is immutable
     */
    @Override
    public boolean isImmutable() {
        return false;
    }

    /**
     * Checks if is comparable.
     *
     * @return true, if is comparable
     */
    @Override
    public boolean isComparable() {
        return false;
    }

    /**
     * Checks if is serializable.
     *
     * @return true, if is serializable
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
     * Checks if is optional or nullable.
     *
     * @return true, if is optional or nullable
     */
    @Override
    public boolean isOptionalOrNullable() {
        return false;
    }

    /**
     *
     *
     * @return
     */
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

    /**
     *
     *
     * @return
     */
    @Override
    public T defaultValue() {
        return null; // NOSONAR
    }

    /**
     *
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
    public int compare(T x, T y) {
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
    public T valueOf(char[] cbuf, int offset, int len) {
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
    public T get(ResultSet rs, int columnIndex) throws SQLException {
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
    public T get(ResultSet rs, String columnLabel) throws SQLException {
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
    public void set(PreparedStatement stmt, int columnIndex, T x) throws SQLException {
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
    public void set(CallableStatement stmt, String parameterName, T x) throws SQLException {
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
    public void set(PreparedStatement stmt, int columnIndex, T x, int sqlTypeOrLength) throws SQLException {
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
    public void set(CallableStatement stmt, String parameterName, T x, int sqlTypeOrLength) throws SQLException {
        this.set(stmt, parameterName, x);
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(Appendable appendable, T x) throws IOException {
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
    public void writeCharacter(CharacterWriter writer, T x, JSONXMLSerializationConfig<?> config) throws IOException {
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
    public T collection2Array(Collection<?> c) {
        throw new UnsupportedOperationException(name() + " doesn't support collection2Array Operation");
    }

    /**
     * Array 2 collection.
     *
     * @param <E>
     * @param collClass
     * @param x
     * @return
     */
    @Override
    public <E> Collection<E> array2Collection(Class<?> collClass, T x) {
        throw new UnsupportedOperationException(name() + " doesn't support array2Collection Operation");
    }

    /**
     * Array 2 collection.
     *
     * @param <E>
     * @param resultCollection
     * @param x
     * @return
     */
    @Override
    public <E> Collection<E> array2Collection(Collection<E> resultCollection, T x) {
        throw new UnsupportedOperationException(name() + " doesn't support array2Collection Operation");
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public int hashCode(T x) {
        return N.hashCode(x);
    }

    /**
     * Deep hash code.
     *
     * @param x
     * @return
     */
    @Override
    public int deepHashCode(T x) {
        return N.hashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return true, if successful
     */
    @Override
    public boolean equals(T x, T y) {
        return N.equals(x, y);
    }

    /**
     *
     * @param x
     * @param y
     * @return true, if successful
     */
    @Override
    public boolean deepEquals(T x, T y) {
        return N.equals(x, y);
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String toString(T x) {
        return N.toString(x);
    }

    /**
     * Deep to string.
     *
     * @param x
     * @return
     */
    @Override
    public String deepToString(T x) {
        return N.toString(x);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return name().hashCode();
    }

    /**
     * Returns true if the specified {@code} is a type and it's name equals with
     * this type's name.
     *
     * @param obj
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AbstractType) {
            final AbstractType<T> another = ((AbstractType<T>) obj);
            return N.equals(another.name(), this.name()) && N.equals(another.declaringName(), this.declaringName()) && another.clazz().equals(this.clazz());
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
    protected static String[] split(String st, String separator) {
        String newValue = separatorConvertor.get(separator);

        return (newValue == null) ? st.split(separator) : st.split(newValue);
    }

    /**
     *
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
                char ch = cbuf[offset];
                if (ch < '0' || ch > '9') {
                    throw new NumberFormatException("Invalid numeric String: \"" + ch + "\""); //NOSONAR
                }

                return ch - '0';
            }

            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9: {
                boolean isNagtive = cbuf[offset] == '-';

                int result = 0;
                char ch = 0;

                for (int i = (cbuf[offset] == '-' || cbuf[offset] == '+') ? offset + 1 : offset, to = offset + len; i < to; i++) {
                    ch = cbuf[i];

                    if (ch < '0' || ch > '9') {
                        throw new NumberFormatException("Invalid numeric String: \"" + new String(cbuf, offset, len) + "\"");
                    }

                    result = result * 10 + (ch - '0');
                }

                return isNagtive ? -result : result;
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
                char ch = cbuf[offset];
                if (ch < '0' || ch > '9') {
                    throw new NumberFormatException("Invalid numeric String: \"" + ch + "\"");
                }

                return ch - '0'; //NOSONAR
            }

            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18: {
                boolean isNagtive = cbuf[offset] == '-';

                long result = 0;
                char ch = 0;

                for (int i = (cbuf[offset] == '-' || cbuf[offset] == '+') ? offset + 1 : offset, to = offset + len; i < to; i++) {
                    ch = cbuf[i];

                    if (ch < '0' || ch > '9') {
                        throw new NumberFormatException("Invalid numeric String: \"" + new String(cbuf, offset, len) + "\"");
                    }

                    result = result * 10 + (ch - '0');
                }

                return isNagtive ? -result : result;
            }

            default:
                return Numbers.toLong(new String(cbuf, offset, len));
        }
    }

    /**
     * Gets the column value.
     *
     * @param <T>
     * @param targetClass
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    static <T> T getColumnValue(final Class<? extends T> targetClass, final ResultSet rs, final int columnIndex) throws SQLException {
        return N.<T> typeOf(targetClass).get(rs, columnIndex);
    }

    /**
     * Gets the column value.
     *
     * @param <T>
     * @param targetClass
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    static <T> T getColumnValue(final Class<? extends T> targetClass, final ResultSet rs, final String columnLabel) throws SQLException {
        return N.<T> typeOf(targetClass).get(rs, columnLabel);
    }
}
