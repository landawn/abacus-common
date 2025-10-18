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

/**
 * Abstract base class for all types in the type system.
 * This class provides default implementations for most Type interface methods
 * and defines common constants and utility methods used across all type implementations.
 * Concrete type classes should extend this class and override methods as needed
 * for their specific type handling.
 *
 * @param <T> the Java type that this Type represents
 */
public abstract class AbstractType<T> implements Type<T> {

    /** List of common factory method names used for creating instances from strings */
    static final List<String> factoryMethodNames = List.of("valueOf", "of", "create", "parse");

    /** List of common getter method names for extracting values from wrapper objects */
    static final List<String> getValueMethodNames = List.of("value", "getValue", "get");

    /** List of common field names for value fields in wrapper objects */
    static final List<String> valueFieldNames = List.of("value", "val");

    /** Default element separator used in string representations of collections/arrays */
    static final String ELEMENT_SEPARATOR = Strings.ELEMENT_SEPARATOR;

    /** Character array version of element separator for efficient writing */
    static final char[] ELEMENT_SEPARATOR_CHAR_ARRAY = ELEMENT_SEPARATOR.toCharArray();

    /** Special string constant for system time */
    static final String SYS_TIME = "sysTime";

    /** String representation of null value */
    static final String NULL_STRING = "null";

    /** String representation of empty array */
    static final String STR_FOR_EMPTY_ARRAY = "[]";

    /** Character array representation of null for efficient writing */
    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    /** String representation of boolean true */
    static final String TRUE_STRING = Boolean.TRUE.toString().intern();

    /** Character array representation of boolean true for efficient writing */
    static final char[] TRUE_CHAR_ARRAY = TRUE_STRING.toCharArray();

    /** String representation of boolean false */
    static final String FALSE_STRING = Boolean.FALSE.toString().intern();

    /** Character array representation of boolean false for efficient writing */
    static final char[] FALSE_CHAR_ARRAY = FALSE_STRING.toCharArray();

    /** Empty type array constant */
    @SuppressWarnings("rawtypes")
    protected static final Type[] EMPTY_TYPE_ARRAY = {};

    /** Map for converting separator strings to regex patterns */
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

    /** The name of this type */
    private final String name;

    /** The XML-safe name of this type (with angle brackets escaped) */
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

    /**
     * Extracts type parameters from a generic type name.
     * For example, "Map<String, Integer>" returns ["String", "Integer"].
     *
     * @param typeName the type name to parse
     * @return array of type parameter names
     */
    protected static String[] getTypeParameters(final String typeName) {
        return TypeAttrParser.parse(typeName).getTypeParameters();
    }

    /**
     * Extracts all parameters from a type name.
     * This includes both type parameters and other attributes.
     *
     * @param typeName the type name to parse
     * @return array of parameter names
     */
    protected static String[] getParameters(final String typeName) {
        return TypeAttrParser.parse(typeName).getParameters();
    }

    /**
     * Returns the name of this type.
     * This is the simplified name without package qualifiers for common types.
     *
     * @return the type name
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the declaring name of this type.
     * For most types, this is the same as {@link #name()}.
     *
     * @return the declaring name
     */
    @Override
    public String declaringName() {

        return name;
    }

    /**
     * Returns the XML-safe name of this type.
     * This escapes angle brackets to make the name safe for use in XML.
     *
     * @return the XML-safe type name with escaped angle brackets
     */
    @Override
    public String xmlName() {
        return xmlName;
    }

    /**
     * Checks if this is a primitive type.
     * Default implementation returns {@code false}.
     * Subclasses for primitive types should override this method.
     *
     * @return {@code true} if this is a primitive type, {@code false} otherwise
     */
    @Override
    public boolean isPrimitiveType() {
        return false;
    }

    /**
     * Checks if this is a primitive wrapper type.
     * Default implementation returns {@code false}.
     * Subclasses for primitive wrapper types should override this method.
     *
     * @return {@code true} if this is a primitive wrapper type, {@code false} otherwise
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return false;
    }

    /**
     * Checks if this is a primitive list type.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a primitive list type, {@code false} otherwise
     */
    @Override
    public boolean isPrimitiveList() {
        return false;
    }

    /**
     * Checks if this is a boolean type.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a boolean type, {@code false} otherwise
     */
    @Override
    public boolean isBoolean() {
        return false;
    }

    /**
     * Checks if this is a number type.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a number type, {@code false} otherwise
     */
    @Override
    public boolean isNumber() {
        return false;
    }

    /**
     * Checks if this is a string type.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a string type, {@code false} otherwise
     */
    @Override
    public boolean isString() {
        return false;
    }

    /**
     * Checks if this is a CharSequence type.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a CharSequence type, {@code false} otherwise
     */
    @Override
    public boolean isCharSequence() {
        return false;
    }

    /**
     * Checks if this type represents a Date.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a Date type, {@code false} otherwise
     */
    @Override
    public boolean isDate() {
        return false;
    }

    /**
     * Checks if this type represents a Calendar.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a Calendar type, {@code false} otherwise
     */
    @Override
    public boolean isCalendar() {
        return false;
    }

    /**
     * Checks if this type represents a Joda DateTime.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a Joda DateTime type, {@code false} otherwise
     */
    @Override
    public boolean isJodaDateTime() {
        return false;
    }

    /**
     * Checks if this type represents a primitive array.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a primitive array type, {@code false} otherwise
     */
    @Override
    public boolean isPrimitiveArray() {
        return false;
    }

    /**
     * Checks if this type represents a byte array.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a byte array type, {@code false} otherwise
     */
    @Override
    public boolean isPrimitiveByteArray() {
        return false;
    }

    /**
     * Checks if this type represents an object array.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is an object array type, {@code false} otherwise
     */
    @Override
    public boolean isObjectArray() {
        return false;
    }

    /**
     * Checks if this type represents any kind of array.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is an array type, {@code false} otherwise
     */
    @Override
    public boolean isArray() {
        return false;
    }

    /**
     * Checks if this type represents a List.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a List type, {@code false} otherwise
     */
    @Override
    public boolean isList() {
        return false;
    }

    /**
     * Checks if this type represents a Set.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a Set type, {@code false} otherwise
     */
    @Override
    public boolean isSet() {
        return false;
    }

    /**
     * Checks if this type represents a Collection.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a Collection type, {@code false} otherwise
     */
    @Override
    public boolean isCollection() {
        return false;
    }

    /**
     * Checks if this type represents a Map.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a Map type, {@code false} otherwise
     */
    @Override
    public boolean isMap() {
        return false;
    }

    /**
     * Checks if this type represents a Bean.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a Bean type, {@code false} otherwise
     */
    @Override
    public boolean isBean() {
        return false;
    }

    /**
     * Checks if this type represents a MapEntity.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a MapEntity type, {@code false} otherwise
     */
    @Override
    public boolean isMapEntity() {
        return false;
    }

    /**
     * Checks if this type represents an EntityId.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is an EntityId type, {@code false} otherwise
     */
    @Override
    public boolean isEntityId() {
        return false;
    }

    /**
     * Checks if this type represents a Dataset.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a Dataset type, {@code false} otherwise
     */
    @Override
    public boolean isDataset() {
        return false;
    }

    /**
     * Checks if this type represents an InputStream.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is an InputStream type, {@code false} otherwise
     */
    @Override
    public boolean isInputStream() {
        return false;
    }

    /**
     * Checks if this type represents a Reader.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a Reader type, {@code false} otherwise
     */
    @Override
    public boolean isReader() {
        return false;
    }

    /**
     * Checks if this type represents a ByteBuffer.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is a ByteBuffer type, {@code false} otherwise
     */
    @Override
    public boolean isByteBuffer() {
        return false;
    }

    /**
     * Checks if this is a generic type.
     * A type is considered generic if it has type parameters.
     *
     * @return {@code true} if this type has type parameters, {@code false} otherwise
     */
    @Override
    public boolean isGenericType() {
        return N.notEmpty(getParameterTypes());
    }

    /**
     * Checks if this type is immutable.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if values of this type are immutable, {@code false} otherwise
     */
    @Override
    public boolean isImmutable() {
        return false;
    }

    /**
     * Checks if this type is comparable.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if values of this type can be compared, {@code false} otherwise
     */
    @Override
    public boolean isComparable() {
        return false;
    }

    /**
     * Checks if this type is serializable.
     * Default implementation returns {@code true}.
     *
     * @return {@code true} if values of this type can be serialized, {@code false} otherwise
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Gets the serialization type for this type.
     * Returns SERIALIZABLE if the type is serializable, otherwise UNKNOWN.
     *
     * @return the serialization type
     */
    @Override
    public SerializationType getSerializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.UNKNOWN;
    }

    /**
     * Checks if this type is optional or nullable.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this type represents optional or nullable values, {@code false} otherwise
     */
    @Override
    public boolean isOptionalOrNullable() {
        return false;
    }

    /**
     * Checks if this is the Object type.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this is the Object type, {@code false} otherwise
     */
    @Override
    public boolean isObjectType() {
        return false;
    }

    /**
     * Gets the element type for collection/array types.
     * Default implementation returns {@code null}.
     *
     * @return the element type, or {@code null} if not applicable
     */
    @Override
    public Type<?> getElementType() {
        return null; // NOSONAR
    }

    /**
     * Gets the parameter types for generic types.
     * Default implementation returns an empty array.
     *
     * @return array of parameter types
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return AbstractType.EMPTY_TYPE_ARRAY;
    }

    /**
     * Returns the default value for this type.
     * Default implementation returns {@code null}.
     *
     * @return the default value, typically {@code null}
     */
    @Override
    public T defaultValue() {
        return null; // NOSONAR
    }

    /**
     * Checks if the given value is the default value for this type.
     *
     * @param value the value to check
     * @return {@code true} if the value equals the default value
     */
    @Override
    public boolean isDefaultValue(final T value) {
        return N.equals(defaultValue(), value);
    }

    /**
     * Compares two values of this type.
     * Only supported for comparable types.
     *
     * @param x the first value
     * @param y the second value
     * @return negative if x < y, zero if x equals y, positive if x > y
     * @throws UnsupportedOperationException if this type is not comparable
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
     * Converts an object to this type.
     * Default implementation converts the object to string first,
     * then parses it using {@link #valueOf(String)}.
     *
     * @param obj the object to convert
     * @return the converted value
     */
    @Override
    public T valueOf(final Object obj) {
        return valueOf(obj == null ? null : N.typeOf(obj.getClass()).stringOf(obj));
    }

    /**
     * Converts a character array to this type.
     * Default implementation creates a string from the character array
     * and delegates to {@link #valueOf(String)}.
     *
     * @param cbuf the character array
     * @param offset the starting position
     * @param len the number of characters
     * @return the converted value
     */
    @Override
    public T valueOf(final char[] cbuf, final int offset, final int len) {
        return valueOf(cbuf == null ? null : String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a value of this type from a ResultSet.
     * Default implementation gets the value as a string and converts it.
     *
     * @param rs the ResultSet
     * @param columnIndex the column index (1-based)
     * @return the retrieved value
     * @throws SQLException if a database access error occurs
     */
    @Override
    public T get(final ResultSet rs, final int columnIndex) throws SQLException {
        return valueOf(rs.getString(columnIndex));
    }

    /**
     * Retrieves a value of this type from a ResultSet by column label.
     * Default implementation gets the value as a string and converts it.
     *
     * @param rs the ResultSet
     * @param columnLabel the column label
     * @return the retrieved value
     * @throws SQLException if a database access error occurs
     */
    @Override
    public T get(final ResultSet rs, final String columnLabel) throws SQLException {
        return valueOf(rs.getString(columnLabel));
    }

    /**
     * Sets a parameter value in a PreparedStatement.
     * Default implementation converts the value to string.
     *
     * @param stmt the PreparedStatement
     * @param columnIndex the parameter index (1-based)
     * @param x the value to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x) throws SQLException {
        stmt.setString(columnIndex, stringOf(x));
    }

    /**
     * Sets a parameter value in a CallableStatement.
     * Default implementation converts the value to string.
     *
     * @param stmt the CallableStatement
     * @param parameterName the parameter name
     * @param x the value to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x) throws SQLException {
        stmt.setString(parameterName, stringOf(x));
    }

    /**
     * Sets a parameter value in a PreparedStatement with SQL type.
     * Default implementation ignores the SQL type and delegates to {@link #set(PreparedStatement, int, Object)}.
     *
     * @param stmt the PreparedStatement
     * @param columnIndex the parameter index (1-based)
     * @param x the value to set
     * @param sqlTypeOrLength the SQL type or length (ignored in default implementation)
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x, final int sqlTypeOrLength) throws SQLException {
        this.set(stmt, columnIndex, x);
    }

    /**
     * Sets a parameter value in a CallableStatement with SQL type.
     * Default implementation ignores the SQL type and delegates to {@link #set(CallableStatement, String, Object)}.
     *
     * @param stmt the CallableStatement
     * @param parameterName the parameter name
     * @param x the value to set
     * @param sqlTypeOrLength the SQL type or length (ignored in default implementation)
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x, final int sqlTypeOrLength) throws SQLException {
        this.set(stmt, parameterName, x);
    }

    /**
     * Appends the string representation of a value to an Appendable.
     * Default implementation writes "null" for null values,
     * otherwise uses {@link #stringOf(Object)}.
     *
     * @param appendable the target to append to
     * @param x the value to append
     * @throws IOException if an I/O error occurs
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
     * Writes a value to a CharacterWriter with optional configuration.
     * Default implementation handles string quotation based on configuration.
     *
     * @param writer the CharacterWriter to write to
     * @param x the value to write
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
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
     * Converts a collection to an array of this type.
     * Default implementation throws UnsupportedOperationException.
     *
     * @param c the collection to convert
     * @return the array representation
     * @throws UnsupportedOperationException if not supported by this type
     */
    @Override
    public T collection2Array(final Collection<?> c) {
        throw new UnsupportedOperationException(name() + " doesn't support collection2Array Operation");
    }

    /**
     * Converts an array to a collection.
     * Default implementation throws UnsupportedOperationException.
     *
     * @param <E> the element type
     * @param array the array to convert
     * @param collClass the collection class to create
     * @return the collection
     * @throws UnsupportedOperationException if not supported by this type
     */
    @Override
    public <E> Collection<E> array2Collection(final T array, final Class<?> collClass) {
        throw new UnsupportedOperationException(name() + " doesn't support array2Collection Operation");
    }

    /**
     * Converts an array to a collection by adding elements to the output collection.
     * Default implementation throws UnsupportedOperationException.
     *
     * @param <E> the element type
     * @param array the array to convert
     * @param output the collection to add elements to
     * @throws UnsupportedOperationException if not supported by this type
     */
    @Override
    public <E> void array2Collection(final T array, final Collection<E> output) {
        throw new UnsupportedOperationException(name() + " doesn't support array2Collection Operation");
    }

    /**
     * Calculates the hash code for a value of this type.
     * Default implementation uses {@link N#hashCode(Object)}.
     *
     * @param x the value
     * @return the hash code
     */
    @Override
    public int hashCode(final T x) {
        return N.hashCode(x);
    }

    /**
     * Calculates the deep hash code for a value of this type.
     * Default implementation delegates to {@link #hashCode(Object)}.
     *
     * @param x the value
     * @return the deep hash code
     */
    @Override
    public int deepHashCode(final T x) {
        return N.hashCode(x);
    }

    /**
     * Checks equality between two values of this type.
     * Default implementation uses {@link N#equals(Object, Object)}.
     *
     * @param x the first value
     * @param y the second value
     * @return {@code true} if the values are equal
     */
    @Override
    public boolean equals(final T x, final T y) {
        return N.equals(x, y);
    }

    /**
     * Checks deep equality between two values of this type.
     * Default implementation delegates to {@link #equals(Object, Object)}.
     *
     * @param x the first value
     * @param y the second value
     * @return {@code true} if the values are deeply equal
     */
    @Override
    public boolean deepEquals(final T x, final T y) {
        return N.equals(x, y);
    }

    /**
     * Converts a value to its string representation.
     * Default implementation uses {@link N#toString(Object)}.
     *
     * @param x the value
     * @return the string representation
     */
    @Override
    public String toString(final T x) {
        return N.toString(x);
    }

    /**
     * Converts a value to its deep string representation.
     * Default implementation delegates to {@link #toString(Object)}.
     *
     * @param x the value
     * @return the deep string representation
     */
    @Override
    public String deepToString(final T x) {
        return toString(x);
    }

    /**
     * Returns the hash code of this type based on its name.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return name().hashCode();
    }

    /**
     * Checks if this type equals another object.
     * Two types are equal if they have the same name, declaring name, and class.
     *
     * @param obj the object to compare
     * @return {@code true} if the objects are equal types
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
     * Returns the string representation of this type (its name).
     *
     * @return the type name
     */
    @Override
    public String toString() {
        return name();
    }

    /**
     * Splits a string using the specified separator.
     * Handles regex special characters by escaping them.
     *
     * @param st the string to split
     * @param separator the separator
     * @return array of split strings
     */
    protected static String[] split(final String st, final String separator) {
        final String newValue = separatorConvertor.get(separator);

        return (newValue == null) ? st.split(separator) : st.split(newValue);
    }

    /**
     * Checks if a date string represents a null date/time.
     * Returns true for empty strings or the literal "null".
     *
     * @param date the date string to check
     * @return {@code true} if the date represents null
     */
    protected static boolean isNullDateTime(final String date) {
        return Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date));
    }

    /**
     * Checks if a character sequence possibly represents a long timestamp.
     * A string is considered a possible long if positions 2 and 4 contain digits.
     *
     * @param dateTime the character sequence to check
     * @return {@code true} if it could be a long timestamp
     */
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

    /**
     * Checks if a character array possibly represents a long timestamp.
     * A character array is considered a possible long if positions 2 and 4 contain digits.
     *
     * @param cbuf the character array
     * @param offset the starting position
     * @param len the length to check
     * @return {@code true} if it could be a long timestamp
     */
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
     * Parses an integer from a character array.
     * Optimized for common cases with direct parsing for lengths 1-9.
     * Supports negative numbers and optional sign characters.
     *
     * @param cbuf the character array
     * @param offset the starting position
     * @param len the number of characters to parse
     * @return the parsed integer value
     * @throws NumberFormatException if the characters cannot be parsed as an integer
     * @throws IllegalArgumentException if offset or len is negative
     * @see Integer#parseInt(String)
     */
    protected static int parseInt(final char[] cbuf, final int offset, int len) throws NumberFormatException {
        if (offset < 0 || len < 0) {
            throw new IllegalArgumentException("'offset' and 'len' can't be negative");
        }

        char ch = cbuf[offset + len - 1];

        if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
            len = len - 1; // ignore the suffix

            if (len == 0 || (cbuf[offset + len - 1] < '0' || cbuf[offset + len - 1] > '9')) {
                throw new NumberFormatException("Invalid numeric String: \"" + ch + "\""); //NOSONAR
            }
        }

        if (len == 0 || (N.isEmpty(cbuf) && offset == 0)) {
            return 0;
        }

        switch (len) {
            case 1: {
                ch = cbuf[offset];
                if (ch < '0' || ch > '9') {
                    throw new NumberFormatException("Invalid numeric String: \"" + ch + "\""); //NOSONAR
                }

                return ch - '0';
            }

            case 2, 3, 4, 5, 6, 7, 8, 9: {
                final boolean isNegative = cbuf[offset] == '-';

                int result = 0;

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
     * Parses a long from a character array.
     * Optimized for common cases with direct parsing for lengths 1-18.
     * Supports negative numbers and optional sign characters.
     *
     * @param cbuf the character array
     * @param offset the starting position
     * @param len the number of characters to parse
     * @return the parsed long value
     * @throws NumberFormatException if the characters cannot be parsed as a long
     * @throws IllegalArgumentException if offset or len is negative
     * @see Long#parseLong(String)
     */
    protected static long parseLong(final char[] cbuf, final int offset, int len) throws NumberFormatException {
        if (offset < 0 || len < 0) {
            throw new IllegalArgumentException("'offset' and 'len' can't be negative");
        }

        char ch = cbuf[offset + len - 1];

        if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
            len = len - 1; // ignore the suffix

            if (len == 0 || (cbuf[offset + len - 1] < '0' || cbuf[offset + len - 1] > '9')) {
                throw new NumberFormatException("Invalid numeric String: \"" + ch + "\""); //NOSONAR
            }
        }

        if (len == 0 || (N.isEmpty(cbuf) && offset == 0)) {
            return 0;
        }

        switch (len) {
            case 1: {
                ch = cbuf[offset];

                if (ch < '0' || ch > '9') {
                    throw new NumberFormatException("Invalid numeric String: \"" + ch + "\"");
                }

                return ch - '0'; //NOSONAR
            }

            case 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18: {
                final boolean isNegative = cbuf[offset] == '-';

                long result = 0;

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
     * Parses a string into a Boolean value with enhanced recognition for common true/false indicators.
     * <p>
     * This method extends the standard Boolean parsing functionality by adding special handling
     * for single-character strings, recognizing "Y", "y", and "1" as true values,
     * in addition to the standard "true" string.
     * <p>
     * For single-character inputs:
     * <ul>
     *   <li>"Y", "y", "1" → true</li>
     *   <li>Any other single character → false</li>
     * </ul>
     * <p>
     * For multi-character inputs, delegates to {@link Boolean#valueOf(String)}, which
     * returns {@code true} only if the string equals "true" (case-insensitive).
     *
     * @param str the string to parse, must not be null
     * @return a Boolean representing the parsed value
     * @see Boolean#valueOf(String)
     */
    protected Boolean parseBoolean(final String str) {
        if (str.length() == 1) {
            final char ch = str.charAt(0);
            return ch == 'Y' || ch == 'y' || ch == '1';
        }

        return Boolean.valueOf(str);
    }

    /**
     * Gets a column value from a ResultSet with type conversion.
     * Uses the type system to retrieve and convert the value.
     *
     * @param <T> the target type
     * @param rs the ResultSet
     * @param columnIndex the column index (1-based)
     * @param targetClass the target class
     * @return the column value converted to the target type
     * @throws SQLException if a database access error occurs
     */
    protected static <T> T getColumnValue(final ResultSet rs, final int columnIndex, final Class<? extends T> targetClass) throws SQLException {
        return N.<T> typeOf(targetClass).get(rs, columnIndex);
    }

    /**
     * Gets a column value from a ResultSet by label with type conversion.
     * Uses the type system to retrieve and convert the value.
     *
     * @param <T> the target type
     * @param rs the ResultSet
     * @param columnLabel the column label
     * @param targetClass the target class
     * @return the column value converted to the target type
     * @throws SQLException if a database access error occurs
     */
    protected static <T> T getColumnValue(final ResultSet rs, final String columnLabel, final Class<? extends T> targetClass) throws SQLException {
        return N.<T> typeOf(targetClass).get(rs, columnLabel);
    }

    /**
     * Calculates buffer size for string operations.
     * Prevents integer overflow by checking against MAX_VALUE.
     *
     * @param len the number of elements
     * @param elementPlusDelimiterLen the length of each element plus delimiter
     * @return the calculated buffer size, capped at Integer.MAX_VALUE
     */
    protected static int calculateBufferSize(final int len, final int elementPlusDelimiterLen) {
        return len > Integer.MAX_VALUE / elementPlusDelimiterLen ? Integer.MAX_VALUE : len * elementPlusDelimiterLen;
    }
}
