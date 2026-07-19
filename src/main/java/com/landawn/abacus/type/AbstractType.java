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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.TypeAttrParser;

/**
 * The abstract base class for all types in the type system.
 * <p>
 * This class provides default implementations for most {@link Type} interface methods
 * and defines common constants and utility methods used across all type implementations.
 * Concrete type classes should extend this class and override methods as needed
 * for their specific type handling.
 * </p>
 *
 * <p>The default implementations exposed here include:</p>
 * <ul>
 *   <li>String serialization via {@link #stringOf(Object)} / {@link #valueOf(String)} (subclasses
 *       must implement these); {@link #valueOf(Object)} and {@link #valueOf(char[], int, int)}
 *       have generic default implementations that delegate through string form.</li>
 *   <li>JDBC integration via {@link #get(ResultSet, int)}, {@link #get(ResultSet, String)},
 *       and {@link #set(PreparedStatement, int, Object)}; the default JDBC mapping treats values
 *       as {@code VARCHAR}, so subclasses with native JDBC support (numbers, dates, blobs) should
 *       override these.</li>
 *   <li>Append/write operations via {@link #appendTo(Appendable, Object)} and
 *       {@link #serializeTo(CharacterWriter, Object, JsonXmlSerConfig)}, both honoring the
 *       textual {@code "null"} marker for {@code null} values.</li>
 *   <li>Type classification predicates ({@link #isPrimitive()}, {@link #isNumber()},
 *       {@link #isDate()}, etc.) which all return {@code false} by default; subclasses override
 *       only the predicates relevant to their category.</li>
 * </ul>
 *
 * <p>Specialised abstract subclasses anchor distinct families of concrete types, including:</p>
 * <ul>
 *   <li>{@link AbstractPrimaryType} &mdash; primitive wrappers, {@link AbstractCharSequenceType}
 *       and {@code Character}/{@code Boolean} types.</li>
 *   <li>{@link AbstractDateType}, {@link AbstractTemporalType}, {@link AbstractJodaDateTimeType}
 *       &mdash; date/time families.</li>
 *   <li>{@link AbstractArrayType}, {@link AbstractPrimitiveArrayType} &mdash; array types.</li>
 *   <li>{@link AbstractPrimitiveListType} &mdash; specialized primitive {@code List}-like types.</li>
 *   <li>{@link AbstractAtomicType} &mdash; {@code java.util.concurrent.atomic} wrappers.</li>
 *   <li>{@link AbstractOptionalType} &mdash; {@code Optional}/{@code Nullable} containers.</li>
 *   <li>{@link AbstractTupleType} &mdash; {@code TupleN} types.</li>
 * </ul>
 *
 * @param <T> the Java type that this {@code Type} represents
 * @see Type
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

    /** String representation of {@code null} value */
    static final String NULL_STRING = "null";

    /** String representation of empty array */
    static final String STR_FOR_EMPTY_ARRAY = "[]";

    /** Character array representation of {@code null} for efficient writing */
    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    /** String representation of boolean {@code true} */
    static final String TRUE_STRING = Boolean.TRUE.toString().intern();

    /** Character array representation of boolean {@code true} for efficient writing */
    static final char[] TRUE_CHAR_ARRAY = TRUE_STRING.toCharArray();

    /** String representation of boolean {@code false} */
    static final String FALSE_STRING = Boolean.FALSE.toString().intern();

    /** Character array representation of boolean {@code false} for efficient writing */
    static final char[] FALSE_CHAR_ARRAY = FALSE_STRING.toCharArray();

    /** Empty parameter-types list constant */
    protected static final List<Type<?>> EMPTY_TYPE_LIST = List.of();

    /** Map for converting separator strings to regex patterns */
    private static final Map<String, String> separatorConverter = new HashMap<>();

    static {
        separatorConverter.put(".", "\\.");
        separatorConverter.put("|", "\\|");
        separatorConverter.put("-", "\\-");
        separatorConverter.put("*", "\\*");
        separatorConverter.put("?", "\\?");
        separatorConverter.put("+", "\\+");
        separatorConverter.put("$", "\\$");
        separatorConverter.put("^", "\\^");
        separatorConverter.put("\\", "\\\\");
        separatorConverter.put("(", "\\(");
        separatorConverter.put(")", "\\)");
        separatorConverter.put("[", "\\[");
        separatorConverter.put("]", "\\]");
        separatorConverter.put("{", "\\{");
        separatorConverter.put("}", "\\}");
    }

    /** The name of this type */
    private final String name;

    /** The XML-safe name of this type (with angle brackets escaped) */
    private final String xmlName;

    /**
     * Constructs an {@code AbstractType} with the specified type name.
     * <p>
     * The constructor normalizes the type name by stripping the package qualifier and
     * keeping only the simple class name for types that live in well-known packages
     * ({@code java.lang}, {@code java.util}, {@code java.time}, {@code com.landawn.abacus}).
     * Generic type parameters (the {@code <...>} suffix) are preserved. The XML-safe form
     * is derived from the normalized name with {@code <} and {@code >} escaped to
     * {@code &lt;} and {@code &gt;}.
     * </p>
     *
     * @param typeName the fully qualified or simple type name (may include generic parameters)
     */
    protected AbstractType(final String typeName) {
        String simpleName = typeName;

        if (typeName.indexOf('.') > 0 && Strings.startsWithAny(typeName, "java.lang.", "java.util.", "java.time.", "com.landawn.abacus.")) { //NOSONAR
            // generic type.

            final int index = typeName.indexOf('<');
            final String tmpTypeName = index > 0 ? typeName.substring(0, index) : typeName;

            try {
                Class<?> cls = ClassUtil.forName(tmpTypeName);

                //noinspection ConstantValue
                if (cls != null) {
                    cls = ClassUtil.forName(ClassUtil.getSimpleClassName(cls));

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
     * <p>
     * For example, {@code "Map<String, Integer>"} returns {@code ["String", "Integer"]}.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * // Extract type parameters from Map
     * String[] params1 = getTypeParameters("Map<String, Integer>");
     * // params1: ["String", "Integer"]
     *
     * // Extract type parameter from List
     * String[] params2 = getTypeParameters("List<Long>");
     * // params2: ["Long"]
     *
     * // No type parameters
     * String[] params3 = getTypeParameters("String");
     * // params3: [] (empty array)
     * }</pre>
     *
     * @param typeName the type name to parse
     * @return array of type parameter names
     */
    protected static String[] getTypeParameters(final String typeName) {
        return TypeAttrParser.parse(typeName).getTypeParameters();
    }

    /**
     * Extracts the constructor parameters (the values enclosed in parentheses) from a type name.
     * <p>
     * These are the parameters in the trailing {@code (...)} of a type name, not the generic
     * type parameters in {@code <...>} (use {@link #getTypeParameters(String)} for those).
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * // Constructor parameters in parentheses
     * String[] params = getParameters("StringBuilder(100)");
     * // params: ["100"]
     *
     * // Multiple constructor parameters
     * String[] params2 = getParameters("HashMap(16, 0.75f)");
     * // params2: ["16", "0.75f"]
     *
     * // No constructor parameters (generic type parameters are NOT returned here)
     * String[] params3 = getParameters("Map<String, Integer>");
     * // params3: [] (empty array)
     * }</pre>
     *
     * @param typeName the type name to parse
     * @return array of constructor parameter strings
     */
    protected static String[] getParameters(final String typeName) {
        return TypeAttrParser.parse(typeName).getParameters();
    }

    /**
     * Returns the name of this type.
     * <p>
     * This is the simplified name without package qualifiers for common types.
     * </p>
     *
     * @return the type name
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the Java reflection type for this type.
     *
     * @return the Java reflection type
     */
    @Override
    public java.lang.reflect.Type reflectType() {
        return javaType();
    }

    /**
     * Returns the declaring name of this type.
     * <p>
     * For most types, this is the same as {@link #name()}.
     * </p>
     *
     * @return the declaring name
     */
    @Override
    public String declaringName() {
        return name;
    }

    /**
     * Returns the XML-safe name of this type.
     * <p>
     * This escapes angle brackets to make the name safe for use in XML.
     * </p>
     *
     * @return the XML-safe type name with escaped angle brackets
     */
    @Override
    public String xmlName() {
        return xmlName;
    }

    /**
     * Checks if this is a primitive type.
     * <p>
     * Default implementation returns {@code false}.
     * Subclasses for primitive types should override this method.
     * </p>
     *
     * @return {@code true} if this is a primitive type, {@code false} otherwise
     */
    @Override
    public boolean isPrimitive() {
        return false;
    }

    /**
     * Checks if this is a primitive wrapper type.
     * <p>
     * Default implementation returns {@code false}.
     * Subclasses for primitive wrapper types should override this method.
     * </p>
     *
     * @return {@code true} if this is a primitive wrapper type, {@code false} otherwise
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return false;
    }

    /**
     * Checks if this is a primitive list type.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a primitive list type, {@code false} otherwise
     */
    @Override
    public boolean isPrimitiveList() {
        return false;
    }

    /**
     * Checks if this is a boolean type.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a boolean type, {@code false} otherwise
     */
    @Override
    public boolean isBoolean() {
        return false;
    }

    /**
     * Checks if this is a number type.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a number type, {@code false} otherwise
     */
    @Override
    public boolean isNumber() {
        return false;
    }

    /**
     * Checks if this is a string type.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a string type, {@code false} otherwise
     */
    @Override
    public boolean isString() {
        return false;
    }

    /**
     * Checks if this is a {@code CharSequence} type.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code CharSequence} type, {@code false} otherwise
     */
    @Override
    public boolean isCharSequence() {
        return false;
    }

    /**
     * Checks if this type represents a {@code Date}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code Date} type, {@code false} otherwise
     */
    @Override
    public boolean isDate() {
        return false;
    }

    /**
     * Checks if this type represents a {@code Calendar}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code Calendar} type, {@code false} otherwise
     */
    @Override
    public boolean isCalendar() {
        return false;
    }

    /**
     * Checks if this type represents a Joda {@code DateTime}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a Joda {@code DateTime} type, {@code false} otherwise
     */
    @Override
    public boolean isJodaDateTime() {
        return false;
    }

    /**
     * Checks if this type represents a primitive array.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a primitive array type, {@code false} otherwise
     */
    @Override
    public boolean isPrimitiveArray() {
        return false;
    }

    /**
     * Checks if this type represents a byte array.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a byte array type, {@code false} otherwise
     */
    @Override
    public boolean isPrimitiveByteArray() {
        return false;
    }

    /**
     * Checks if this type represents an object array.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is an object array type, {@code false} otherwise
     */
    @Override
    public boolean isObjectArray() {
        return false;
    }

    // isArray() intentionally NOT overridden here: it inherits Type's default, which derives the
    // answer from isPrimitiveArray() || isObjectArray(). Hardcoding false here would shadow that
    // formula for any direct AbstractType subclass that overrides isPrimitiveArray()/isObjectArray()
    // without also going through AbstractArrayType (which does override isArray() explicitly).
    // See the analogous isComparable() note below and Type#isArray().

    /**
     * Checks if this type represents a {@code List}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code List} type, {@code false} otherwise
     */
    @Override
    public boolean isList() {
        return false;
    }

    /**
     * Checks if this type represents a {@code Set}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code Set} type, {@code false} otherwise
     */
    @Override
    public boolean isSet() {
        return false;
    }

    /**
     * Checks if this type represents a {@code Collection}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code Collection} type, {@code false} otherwise
     */
    @Override
    public boolean isCollection() {
        return false;
    }

    /**
     * Checks if this type represents a {@code Map}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code Map} type, {@code false} otherwise
     */
    @Override
    public boolean isMap() {
        return false;
    }

    /**
     * Checks if this type represents a Bean.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a Bean type, {@code false} otherwise
     */
    @Override
    public boolean isBean() {
        return false;
    }

    /**
     * Checks if this type represents a {@code MapEntity}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code MapEntity} type, {@code false} otherwise
     */
    @Override
    public boolean isMapEntity() {
        return false;
    }

    /**
     * Checks if this type represents an {@code EntityId}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is an {@code EntityId} type, {@code false} otherwise
     */
    @Override
    public boolean isEntityId() {
        return false;
    }

    /**
     * Checks if this type represents a {@code Dataset}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code Dataset} type, {@code false} otherwise
     */
    @Override
    public boolean isDataset() {
        return false;
    }

    /**
     * Checks if this type represents an {@code InputStream}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is an {@code InputStream} type, {@code false} otherwise
     */
    @Override
    public boolean isInputStream() {
        return false;
    }

    /**
     * Checks if this type represents a {@code Reader}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code Reader} type, {@code false} otherwise
     */
    @Override
    public boolean isReader() {
        return false;
    }

    /**
     * Checks if this type represents a {@code ByteBuffer}.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code ByteBuffer} type, {@code false} otherwise
     */
    @Override
    public boolean isByteBuffer() {
        return false;
    }

    /**
     * Checks if this type is a parameterized type (a generic type with actual type arguments,
     * e.g. {@code List<String>}).
     * A type is considered parameterized if it has type parameters.
     *
     * @return {@code true} if this type has type parameters, {@code false} otherwise
     */
    @Override
    public boolean isParameterizedType() {
        return N.notEmpty(parameterTypes());
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

    // isComparable() intentionally NOT overridden here: it inherits Type's default, which derives
    // the answer from javaType() implementing Comparable (so a comparable scalar type is reported
    // comparable without an explicit override). See Type#isComparable().

    /**
     * Returns the serialization type for this type.
     * Returns {@link SerializationType#SERIALIZABLE} if {@link #isSerializable()} returns
     * {@code true}; otherwise returns {@link SerializationType#UNKNOWN}.
     *
     * @return the {@link SerializationType} for this type
     */
    @Override
    public SerializationType serializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.UNKNOWN;
    }

    /**
     * Checks if this type is optional or {@code nullable}.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if this type represents optional or {@code nullable} values, {@code false} otherwise
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
    public boolean isObject() {
        return false;
    }

    /**
     * Gets the element type for collection/array types.
     * Default implementation returns {@code null}.
     *
     * @return the element type, or {@code null} if not applicable
     */
    @Override
    public Type<?> elementType() {
        return null; // NOSONAR
    }

    /**
     * Gets the parameter types for generic types.
     * Default implementation returns an empty list.
     *
     * @return immutable list of parameter types
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return EMPTY_TYPE_LIST;
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
     * Compares two values of this type using their natural ordering.
     * <p>
     * {@code null} values are treated as less than non-{@code null} values; two {@code null}
     * arguments compare equal. This default implementation is only supported when
     * {@link #isComparable()} returns {@code true}.
     * </p>
     *
     * @param x the first value, may be {@code null}
     * @param y the second value, may be {@code null}
     * @return a negative integer if {@code x} is less than {@code y}, zero if they are equal,
     *         or a positive integer if {@code x} is greater than {@code y}
     * @throws UnsupportedOperationException if this type is not comparable
     */
    @SuppressWarnings("unchecked")
    @Override
    public int compare(final T x, final T y) {
        if (!isComparable()) {
            throw new UnsupportedOperationException(name() + " does not support compare operation");
        }

        return (x == null) ? ((y == null) ? 0 : (-1)) : ((y == null) ? 1 : ((Comparable<? super T>) x).compareTo(y));
    }

    /**
     * Converts an arbitrary object to a value of this type.
     * <p>
     * Default implementation serializes the object to a string using the type-specific
     * {@link #stringOf(Object)} method of the object's actual runtime type, then parses
     * that string using {@link #valueOf(String)}. Subclasses that can perform a more
     * efficient or more accurate conversion should override this method.
     * </p>
     *
     * @param obj the object to convert, may be {@code null}
     * @return the converted value, or the result of {@code valueOf((String) null)} if {@code obj}
     *         is {@code null} (typically {@code null} or this type's default)
     */
    @Override
    public T valueOf(final Object obj) {
        return valueOf(obj == null ? null : Type.<Object> of(obj.getClass()).stringOf(obj));
    }

    /**
     * Converts a region of a character array to a value of this type.
     * <p>
     * Default implementation builds a {@link String} from the {@code [offset, offset+len)}
     * range of {@code cbuf} and delegates to {@link #valueOf(String)}.
     * If {@code cbuf} is {@code null}, this method delegates to {@code valueOf((String) null)}.
     * </p>
     *
     * @param cbuf the character array, may be {@code null}
     * @param offset the starting position within {@code cbuf}
     * @param len the number of characters to read
     * @return the converted value
     */
    @Override
    public T valueOf(final char[] cbuf, final int offset, final int len) {
        return valueOf(cbuf == null ? null : String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a value of this type from a {@link ResultSet} by column index.
     * <p>
     * Default implementation reads the column via {@link ResultSet#getString(int)} and
     * converts the result through {@link #valueOf(String)}. SQL {@code NULL} columns yield
     * a {@code null} string, which {@code valueOf} typically converts to this type's
     * default value (often {@code null}).
     * </p>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the column index (1-based)
     * @return the retrieved value, possibly {@code null}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public T get(final ResultSet rs, final int columnIndex) throws SQLException {
        return valueOf(rs.getString(columnIndex));
    }

    /**
     * Retrieves a value of this type from a {@link ResultSet} by column label.
     * <p>
     * Default implementation reads the column via {@link ResultSet#getString(String)} and
     * converts the result through {@link #valueOf(String)}. SQL {@code NULL} columns yield
     * a {@code null} string, which {@code valueOf} typically converts to this type's
     * default value (often {@code null}).
     * </p>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label
     * @return the retrieved value, possibly {@code null}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public T get(final ResultSet rs, final String columnName) throws SQLException {
        return valueOf(rs.getString(columnName));
    }

    /**
     * Sets a parameter value in a {@link PreparedStatement} by index.
     * <p>
     * Default implementation converts the value to its string representation via
     * {@link #stringOf(Object)} and binds it as a {@code VARCHAR} using
     * {@link PreparedStatement#setString(int, String)}. A {@code null} {@code x} is
     * therefore bound as SQL {@code NULL}. Subclasses with a more specific JDBC mapping
     * (numbers, dates, blobs, etc.) should override this method.
     * </p>
     *
     * @param stmt the {@code PreparedStatement}
     * @param columnIndex the parameter index (1-based)
     * @param x the value to set, may be {@code null}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x) throws SQLException {
        stmt.setString(columnIndex, stringOf(x));
    }

    /**
     * Sets a parameter value in a {@link CallableStatement} by name.
     * <p>
     * Default implementation converts the value to its string representation via
     * {@link #stringOf(Object)} and binds it as a {@code VARCHAR} using
     * {@link CallableStatement#setString(String, String)}. A {@code null} {@code x} is
     * therefore bound as SQL {@code NULL}. Subclasses with a more specific JDBC mapping
     * should override this method.
     * </p>
     *
     * @param stmt the {@code CallableStatement}
     * @param parameterName the parameter name
     * @param x the value to set, may be {@code null}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x) throws SQLException {
        stmt.setString(parameterName, stringOf(x));
    }

    /**
     * Sets a parameter value in a {@link PreparedStatement} with the given SQL type or length.
     * <p>
     * Default implementation ignores {@code sqlTypeOrLength} and delegates to
     * {@link #set(PreparedStatement, int, Object)}. Subclasses that require the SQL type
     * or column size (e.g., {@code Clob}, {@code Blob}, large strings) should override.
     * </p>
     *
     * @param stmt the {@code PreparedStatement}
     * @param columnIndex the parameter index (1-based)
     * @param x the value to set, may be {@code null}
     * @param sqlTypeOrLength the {@code java.sql.Types} code or column length (ignored by default)
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x, final int sqlTypeOrLength) throws SQLException {
        this.set(stmt, columnIndex, x);
    }

    /**
     * Sets a parameter value in a {@link CallableStatement} with the given SQL type or length.
     * <p>
     * Default implementation ignores {@code sqlTypeOrLength} and delegates to
     * {@link #set(CallableStatement, String, Object)}. Subclasses that require the SQL type
     * or column size should override.
     * </p>
     *
     * @param stmt the {@code CallableStatement}
     * @param parameterName the parameter name
     * @param x the value to set, may be {@code null}
     * @param sqlTypeOrLength the {@code java.sql.Types} code or column length (ignored by default)
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x, final int sqlTypeOrLength) throws SQLException {
        this.set(stmt, parameterName, x);
    }

    /**
     * Appends the string representation of a value to an {@code Appendable}.
     * Appends the literal {@code "null"} string for {@code null} values;
     * otherwise delegates to {@link #stringOf(Object)}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas
     * {@link #serializeTo(CharacterWriter, Object, JsonXmlSerConfig)} produces the JSON/XML-serialized form (with
     * string quotation and character escaping applied per the supplied {@link JsonXmlSerConfig}) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the target to append to
     * @param x the value to append
     * @throws IOException if an I/O error occurs
     * @implNote
     * {@code appendTo} writes a string representation of {@code x} for general text output. Conceptually this is the
     * human-readable form produced by {@code toString()}, as opposed to {@link #stringOf(Object)}, which returns a
     * formatted, serializable representation (typically a JSON string) that {@link #valueOf(String)} can convert back
     * into an equivalent value. This default implementation delegates to {@link #stringOf(Object)}, which is adequate
     * for the simple, leaf value types where the human-readable and serialized forms coincide; subclasses that
     * represent structured values (collections, maps, arrays) override it to emit the unquoted,
     * {@code toString()}-style form, so that in the general contract {@code appendTo} is not a plain
     * {@code appendable.append(x == null ? NULL_STRING : stringOf(x))}.
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
     * Writes a value to a {@code CharacterWriter} with optional configuration.
     * Writes {@code "null"} for a {@code null} value; otherwise serializes via
     * {@link #stringOf(Object)} and applies string quotation from {@code config} if specified.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to
     * the {@code CharacterWriter}, applying string quotation and character escaping according to the supplied
     * {@link JsonXmlSerConfig} (a {@code null} config means no surrounding quotation). It is the streaming counterpart
     * of {@link #stringOf(Object)} and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and
     * escaped), whereas {@link #appendTo(Appendable, Object)} produces a plain, human-readable {@code toString()}-style
     * rendering without JSON/XML quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the value to write
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config) throws IOException {
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
    public T collectionToArray(final Collection<?> c) {
        throw new UnsupportedOperationException(name() + " does not support collectionToArray operation");
    }

    /**
     * Converts an array to a collection.
     * Default implementation throws UnsupportedOperationException.
     *
     * @param <E> the element type
     * @param array the array to convert
     * @param collClass the collection class to create
     * @return a collection containing the converted array elements
     * @throws UnsupportedOperationException if not supported by this type
     */
    @Override
    public <E> Collection<E> arrayToCollection(final T array, final Class<?> collClass) {
        throw new UnsupportedOperationException(name() + " does not support arrayToCollection operation");
    }

    /**
     * Converts an array to a collection by adding elements to the output collection.
     * Default implementation throws UnsupportedOperationException.
     *
     * @param array the array to convert
     * @param output the collection to add elements to
     * @throws UnsupportedOperationException if not supported by this type
     */
    @Override
    public void arrayToCollection(final T array, final Collection<?> output) {
        throw new UnsupportedOperationException(name() + " does not support arrayToCollection operation");
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
     * Default implementation computes the hash code via {@link N#hashCode(Object)},
     * which for most non-array types matches {@link #hashCode(Object)}.
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
     * Default implementation compares the values via {@link N#equals(Object, Object)},
     * which for most non-array types matches {@link #equals(Object, Object)}.
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
     * <p>
     * Two {@code AbstractType} instances are considered equal if they have the same
     * {@link #name()}, {@link #declaringName()}, and {@link #javaType()}. Returns
     * {@code false} for any object that is not an {@code AbstractType}.
     * </p>
     *
     * @param obj the object to compare
     * @return {@code true} if {@code obj} is an equivalent type, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AbstractType) {
            final AbstractType<T> another = ((AbstractType<T>) obj);
            return N.equals(another.name(), this.name()) && N.equals(another.declaringName(), this.declaringName()) && another.javaType().equals(javaType());
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
     * Handles single-character regex metacharacters by escaping them. Multi-character
     * separators retain the historical regular-expression behavior.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Split with simple separator
     * String[] result1 = split("a,b,c", ",");
     * // result1: ["a", "b", "c"]
     *
     * // Split with regex special character (automatically escaped)
     * String[] result2 = split("a.b.c", ".");
     * // result2: ["a", "b", "c"]
     *
     * // Split with pipe (automatically escaped)
     * String[] result3 = split("a|b|c", "|");
     * // result3: ["a", "b", "c"]
     * }</pre>
     *
     * @param string the string to split
     * @param separator the delimiter used to split the input string; a single regex
     *        metacharacter is interpreted literally, while a multi-character value is
     *        treated as a regular expression for backward compatibility
     * @return array of split strings
     */
    protected static String[] split(final String string, final String separator) {
        final String newValue = separatorConverter.get(separator);

        return (newValue == null) ? string.split(separator) : string.split(newValue);
    }

    /**
     * Checks if a date string represents a {@code null} date/time.
     * Returns {@code true} for {@code null} or empty strings, or the literal {@code "null"} (case-insensitive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check null string
     * boolean result1 = isNullDateTime(null);
     * // result1: true
     *
     * // Check empty string
     * boolean result2 = isNullDateTime("");
     * // result2: true
     *
     * // Check "null" literal (case-insensitive)
     * boolean result3 = isNullDateTime("null");
     * // result3: true
     *
     * // Check valid date
     * boolean result4 = isNullDateTime("2023-01-01");
     * // result4: false
     * }</pre>
     *
     * @param date the date string to check
     * @return {@code true} if the date represents null
     */
    protected static boolean isNullDateTime(final String date) {
        return Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date));
    }

    /**
     * Checks if a string represents the system time keyword.
     * Accepts both "sysTime" and "SYS_TIME" (case-insensitive).
     *
     * @param date the string to check
     * @return {@code true} if the string matches a system time keyword
     */
    protected static boolean isSysTime(final String date) {
        return Strings.equalsIgnoreCase(date, SYS_TIME) || "SYS_TIME".equalsIgnoreCase(date);
    }

    /**
     * Checks if a character sequence possibly represents a millisecond timestamp (i.e., a long integer).
     * A string is considered a possible millisecond value if it has more than 4 characters and
     * the characters at positions 2 and 4 are both digits, which distinguishes pure numeric
     * values from date strings like {@code "2023-01-01"} where position 4 is {@code '-'}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check numeric timestamp
     * boolean result1 = isPossibleMillis("1234567890");
     * // result1: true (positions 2 and 4 are digits)
     *
     * // Check ISO date format
     * boolean result2 = isPossibleMillis("2023-01-01");
     * // result2: false (position 4 is '-', not a digit)
     *
     * // Check short string
     * boolean result3 = isPossibleMillis("123");
     * // result3: false (length <= 4)
     * }</pre>
     *
     * @param dateTime the character sequence to check
     * @return {@code true} if the sequence could represent a millisecond timestamp
     */
    protected static boolean isPossibleMillis(final CharSequence dateTime) {
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
     * Checks if a region of a character array possibly represents a millisecond timestamp
     * (i.e., a long integer). The region is considered a possible millisecond value if it has
     * more than 4 characters and the characters at relative positions 2 and 4 are both digits,
     * which distinguishes pure numeric values from date strings where position 4 is a separator.
     *
     * @param cbuf the character array to inspect
     * @param offset the starting position within the array
     * @param len the number of characters in the region to inspect
     * @return {@code true} if the character region could represent a millisecond timestamp
     */
    protected static boolean isPossibleMillis(final char[] cbuf, final int offset, final int len) {
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
     * Parses an integer from a region of a character array.
     * <p>
     * Optimized fast path for lengths 1&ndash;9 (parses digits inline); longer regions
     * fall back to {@link Numbers#toInt(String)}. A trailing type suffix
     * ({@code l}, {@code L}, {@code f}, {@code F}, {@code d}, {@code D}) is accepted and
     * ignored. Supports an optional leading sign ({@code '+'} or {@code '-'}).
     * Returns {@code 0} when {@code cbuf} is {@code null} or {@code len == 0}.
     * </p>
     *
     * @param cbuf the character array; may be {@code null}
     * @param offset the starting position within {@code cbuf}
     * @param len the number of characters to parse
     * @return the parsed integer value
     * @throws NumberFormatException if the characters cannot be parsed as an integer
     * @throws IllegalArgumentException if {@code offset} or {@code len} is negative
     * @see Integer#parseInt(String)
     */
    protected static int parseInt(final char[] cbuf, final int offset, int len) throws NumberFormatException {
        if (offset < 0 || len < 0) {
            throw new IllegalArgumentException("'offset' and 'len' cannot be negative");
        }

        if (cbuf == null || len == 0) {
            return 0;
        }

        char ch = cbuf[offset + len - 1];

        if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
            len = len - 1; // ignore the suffix

            if (len == 0 || (cbuf[offset + len - 1] < '0' || cbuf[offset + len - 1] > '9')) {
                throw new NumberFormatException("Invalid numeric String: \"" + ch + "\""); //NOSONAR
            }
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
     * Parses a long from a region of a character array.
     * <p>
     * Optimized fast path for lengths 1&ndash;18 (parses digits inline); longer regions
     * fall back to {@link Numbers#toLong(String)}. A trailing type suffix
     * ({@code l}, {@code L}, {@code f}, {@code F}, {@code d}, {@code D}) is accepted and
     * ignored. Supports an optional leading sign ({@code '+'} or {@code '-'}).
     * Returns {@code 0L} when {@code cbuf} is {@code null} or {@code len == 0}.
     * </p>
     *
     * @param cbuf the character array; may be {@code null}
     * @param offset the starting position within {@code cbuf}
     * @param len the number of characters to parse
     * @return the parsed long value
     * @throws NumberFormatException if the characters cannot be parsed as a long
     * @throws IllegalArgumentException if {@code offset} or {@code len} is negative
     * @see Long#parseLong(String)
     */
    protected static long parseLong(final char[] cbuf, final int offset, int len) throws NumberFormatException {
        if (offset < 0 || len < 0) {
            throw new IllegalArgumentException("'offset' and 'len' cannot be negative");
        }

        if (cbuf == null || len == 0) {
            return 0;
        }

        char ch = cbuf[offset + len - 1];

        if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
            len = len - 1; // ignore the suffix

            if (len == 0 || (cbuf[offset + len - 1] < '0' || cbuf[offset + len - 1] > '9')) {
                throw new NumberFormatException("Invalid numeric String: \"" + ch + "\""); //NOSONAR
            }
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
     * for single-character strings, recognizing "Y", "y", and "1" as {@code true} values,
     * in addition to the standard "true" string.
     * </p>
     * <p>
     * For single-character inputs:
     * </p>
     * <ul>
     *   <li>"Y", "y", "1" &rarr; {@code true}</li>
     *   <li>Any other single character &rarr; {@code false}</li>
     * </ul>
     * <p>
     * For multi-character inputs, delegates to {@link Boolean#valueOf(String)}, which
     * returns {@code true} only if the string equals "true" (case-insensitive).
     * </p>
     *
     * @param str the string to parse; must not be {@code null}
     * @return a non-{@code null} {@code Boolean} representing the parsed value
     * @throws NullPointerException if {@code str} is {@code null}
     * @see Boolean#valueOf(String)
     */
    protected static Boolean parseBoolean(final String str) {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get String value from column 1
     * String name = getColumnValue(resultSet, 1, String.class);
     *
     * // Get Integer value from column 2
     * Integer age = getColumnValue(resultSet, 2, Integer.class);
     *
     * // Get Date value from column 3
     * Date createdDate = getColumnValue(resultSet, 3, Date.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param rs the ResultSet
     * @param columnIndex the column index (1-based)
     * @param targetClass the target class
     * @return the column value converted to the target type
     * @throws SQLException if a database access error occurs
     */
    protected static <T> T getColumnValue(final ResultSet rs, final int columnIndex, final Class<? extends T> targetClass) throws SQLException {
        return Type.of(targetClass).get(rs, columnIndex);
    }

    /**
     * Gets a column value from a ResultSet by label with type conversion.
     * Uses the type system to retrieve and convert the value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get String value by column name
     * String name = getColumnValue(resultSet, "user_name", String.class);
     *
     * // Get Integer value by column name
     * Integer age = getColumnValue(resultSet, "age", Integer.class);
     *
     * // Get Date value by column name
     * Date createdDate = getColumnValue(resultSet, "created_at", Date.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param rs the ResultSet
     * @param columnName the column label
     * @param targetClass the target class
     * @return the column value converted to the target type
     * @throws SQLException if a database access error occurs
     */
    protected static <T> T getColumnValue(final ResultSet rs, final String columnName, final Class<? extends T> targetClass) throws SQLException {
        return Type.of(targetClass).get(rs, columnName);
    }

    /**
     * Calculates a buffer size for string operations, capped at {@link Integer#MAX_VALUE}.
     * <p>
     * Prevents integer overflow by clamping the result when {@code len * elementPlusDelimiterLen}
     * would exceed {@link Integer#MAX_VALUE}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate buffer size for 100 elements, each 20 chars
     * int bufferSize1 = calculateBufferSize(100, 20);
     * // bufferSize1: 2000
     *
     * // Calculate with potential overflow
     * int bufferSize2 = calculateBufferSize(Integer.MAX_VALUE / 10, 20);
     * // bufferSize2: Integer.MAX_VALUE (overflow prevented)
     *
     * // Small calculation
     * int bufferSize3 = calculateBufferSize(5, 10);
     * // bufferSize3: 50
     * }</pre>
     *
     * @param len the number of elements
     * @param elementPlusDelimiterLen the length of each element plus delimiter
     * @return the calculated buffer size, capped at Integer.MAX_VALUE
     */
    protected static int calculateBufferSize(final int len, final int elementPlusDelimiterLen) {
        if (elementPlusDelimiterLen == 0) {
            return 0;
        }

        return len > Integer.MAX_VALUE / elementPlusDelimiterLen ? Integer.MAX_VALUE : len * elementPlusDelimiterLen;
    }

    /**
     * Converts a raw element produced by an untyped first-pass JSON parse into the declared element type
     * of a tuple slot ({@code Pair}, {@code Triple}, {@code Tuple1..9}).
     *
     * @param raw the raw element value from the untyped parse, may be {@code null}
     * @param type the declared type of the tuple slot
     * @return the element converted to the declared type, or {@code null} if {@code raw} is {@code null}
     */
    protected static Object convertTupleElement(final Object raw, final Type<?> type) {
        if (raw == null) {
            return null;
        }

        // A parameterized slot must be re-deserialized with its declared element types: the
        // untyped first-pass parse produced parser defaults (Integer, LinkedHashMap, ...), and
        // both the raw-assignability shortcut and N.convert would keep them unconverted.
        if (type.isParameterizedType() && !(raw instanceof CharSequence)) {
            return type.valueOf(Utils.jsonParser.serialize(raw));
        }

        return type.javaType().isAssignableFrom(raw.getClass()) ? raw : N.convert(raw, type);
    }
}
