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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.TypeAttrParser;
import com.landawn.abacus.util.cs;

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
     * ({@code java.lang}, {@code java.util}, {@code java.time}, {@code com.landawn.abacus}),
     * but only when the class can be resolved by that simple name ({@code "java.util.UUID"} becomes
     * {@code "UUID"}, while {@code "java.util.BitSet"} is kept as is); every other name is kept as given.
     * Generic type parameters (the {@code <...>} suffix) are preserved. The XML-safe form
     * is derived from the normalized name with {@code <} and {@code >} escaped to
     * {@code &lt;} and {@code &gt;}.
     * </p>
     *
     * @param typeName the fully qualified or simple type name (may include generic parameters)
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    protected AbstractType(final String typeName) throws IllegalArgumentException {
        N.checkArgNotNull(typeName, cs.typeName);

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
     * @throws IllegalArgumentException if {@code typeName} is {@code null} or is not a well-formed type declaration
     *         (see {@link TypeAttrParser#parse(String)})
     */
    protected static String[] getTypeParameters(final String typeName) throws IllegalArgumentException {
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
     * @throws IllegalArgumentException if {@code typeName} is {@code null} or is not a well-formed type declaration
     *         (see {@link TypeAttrParser#parse(String)})
     */
    protected static String[] getParameters(final String typeName) throws IllegalArgumentException {
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
     * Checks if this type represents a {@link java.time.temporal.Temporal} type.
     * <p>
     * Default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} if this is a {@code Temporal} type, {@code false} otherwise
     */
    @Override
    public boolean isTemporal() {
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
     * Checks whether <em>values</em> of the represented Java type are immutable.
     * Default implementation returns {@code false}.
     * This describes the value type, not the {@code Type} handler instance itself.
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
    public int compare(final T x, final T y) throws UnsupportedOperationException {
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
     * @param length the number of characters to read
     * @return the converted value
     * @throws IndexOutOfBoundsException if {@code cbuf} is non-null and {@code offset} or {@code length} is negative or
     *         {@code offset + len > cbuf.length}
     */
    @Override
    public T valueOf(final char[] cbuf, final int offset, final int length) throws IndexOutOfBoundsException {
        return valueOf(cbuf == null ? null : String.valueOf(cbuf, offset, length));
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
     * @throws NullPointerException if {@code rs} is {@code null}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public T get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
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
     * @throws NullPointerException if {@code rs} is {@code null}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public T get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
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
     * @param statement the {@code PreparedStatement}
     * @param columnIndex the parameter index (1-based); named {@code columnIndex} for historical API
     *                    compatibility, but this is a {@link PreparedStatement} parameter index, not a
     *                    {@link ResultSet} column index
     * @param x the value to set, may be {@code null}
     * @throws NullPointerException if {@code statement} is {@code null}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final T x) throws NullPointerException, SQLException {
        statement.setString(columnIndex, stringOf(x));
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
     * @param statement the {@code CallableStatement}
     * @param parameterName the parameter name
     * @param x the value to set, may be {@code null}
     * @throws NullPointerException if {@code statement} is {@code null}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final T x) throws NullPointerException, SQLException {
        statement.setString(parameterName, stringOf(x));
    }

    /**
     * Sets a parameter value in a {@link PreparedStatement} with the given SQL type or length.
     * <p>
     * Default implementation ignores {@code sqlTypeOrLength} and delegates to
     * {@link #set(PreparedStatement, int, Object)}. Subclasses that require the SQL type
     * or column size (e.g., {@code Clob}, {@code Blob}, large strings) should override.
     * </p>
     *
     * @param statement the {@code PreparedStatement}
     * @param columnIndex the parameter index (1-based); named {@code columnIndex} for historical API
     *                    compatibility, but this is a {@link PreparedStatement} parameter index, not a
     *                    {@link ResultSet} column index
     * @param x the value to set, may be {@code null}
     * @param sqlTypeOrLength the {@code java.sql.Types} code or column length (ignored by default)
     * @throws NullPointerException if {@code statement} is {@code null}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final T x, final int sqlTypeOrLength) throws NullPointerException, SQLException {
        this.set(statement, columnIndex, x);
    }

    /**
     * Sets a parameter value in a {@link CallableStatement} with the given SQL type or length.
     * <p>
     * Default implementation ignores {@code sqlTypeOrLength} and delegates to
     * {@link #set(CallableStatement, String, Object)}. Subclasses that require the SQL type
     * or column size should override.
     * </p>
     *
     * @param statement the {@code CallableStatement}
     * @param parameterName the parameter name
     * @param x the value to set, may be {@code null}
     * @param sqlTypeOrLength the {@code java.sql.Types} code or column length (ignored by default)
     * @throws NullPointerException if {@code statement} is {@code null}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final T x, final int sqlTypeOrLength)
            throws NullPointerException, SQLException {
        this.set(statement, parameterName, x);
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
     * @throws NullPointerException if {@code appendable} is {@code null}
     * @throws IOException if appending the value to {@code appendable} fails
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
    public void appendTo(final Appendable appendable, final T x) throws NullPointerException, IOException {
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
     * @throws NullPointerException if {@code writer} is {@code null}
     * @throws IOException if writing the serialized value to {@code writer} fails
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getStringQuotation();

            if (ch == 0) {
                Utils.writeStringContent(writer, stringOf(x), ch);
            } else {
                writer.write(ch);
                Utils.writeStringContent(writer, stringOf(x), ch);
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
    public T collectionToArray(final Collection<?> c) throws UnsupportedOperationException {
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
    public <E> Collection<E> arrayToCollection(final T array, final Class<?> collClass) throws UnsupportedOperationException {
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
    public void arrayToCollection(final T array, final Collection<?> output) throws UnsupportedOperationException {
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
     * Default implementation computes the hash code via {@link N#deepHashCode(Object)}.
     *
     * @param x the value
     * @return the deep hash code
     */
    @Override
    public int deepHashCode(final T x) {
        return N.deepHashCode(x);
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
     * Default implementation compares the values via {@link N#deepEquals(Object, Object)}.
     *
     * @param x the first value
     * @param y the second value
     * @return {@code true} if the values are deeply equal
     */
    @Override
    public boolean deepEquals(final T x, final T y) {
        return N.deepEquals(x, y);
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
     * @throws NullPointerException if {@code string} or {@code separator} is {@code null}
     * @throws PatternSyntaxException if {@code separator} is a multi-character value that is not a valid regular expression
     */
    protected static String[] split(final String string, final String separator) throws NullPointerException, PatternSyntaxException {
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
     * A sequence is considered a possible millisecond value if it has more than 4 characters and consists
     * of decimal digits, optionally preceded by {@code '+'} or {@code '-'}. Hexadecimal prefixes, type
     * suffixes, and date separators are rejected. This checks the syntax only; the subsequent long parser
     * checks whether the value fits in the {@code long} range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check numeric timestamp
     * boolean result1 = isPossibleMillis("1234567890");
     * // result1: true (decimal integer longer than four characters)
     *
     * // Check ISO date format
     * boolean result2 = isPossibleMillis("2023-01-01");
     * // result2: false (contains date separators)
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

        if (len <= 4) {
            return false;
        }

        for (int i = dateTime.charAt(0) == '-' || dateTime.charAt(0) == '+' ? 1 : 0; i < len; i++) {
            if (!isDigit(dateTime.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a region of a character array possibly represents a millisecond timestamp
     * (i.e., a long integer). The region must have more than 4 characters and contain decimal digits,
     * optionally preceded by {@code '+'} or {@code '-'}. Range checking is left to the subsequent long parser.
     * <p>
     * Checking every digit keeps the {@code char[]} readers in step with their {@code String} counterparts:
     * {@link #parseLong(char[], int, int)} also accepts type suffixes and, for long tokens, hexadecimal
     * prefixes that {@link Long#parseLong(String)} rejects. Neither form is a decimal millisecond timestamp.
     * </p>
     *
     * @param cbuf the character array to inspect
     * @param offset the starting position within the array
     * @param length the number of characters in the region to inspect
     * @return {@code true} if the character region could represent a millisecond timestamp
     * @throws IndexOutOfBoundsException if {@code length > 4} and the region {@code [offset, offset + len)} extends
     *         outside {@code cbuf}
     */
    protected static boolean isPossibleMillis(final char[] cbuf, final int offset, final int length) throws IndexOutOfBoundsException {
        if (length <= 4) {
            return false;
        }

        for (int i = cbuf[offset] == '-' || cbuf[offset] == '+' ? 1 : 0; i < length; i++) {
            if (!isDigit(cbuf[offset + i])) {
                return false;
            }
        }

        return true;
    }

    private static boolean isDigit(final char ch) {
        return ch >= '0' && ch <= '9';
    }

    /**
     * Returns whether a character is one of the supported numeric literal suffixes.
     *
     * @param ch the character to check
     * @return {@code true} for {@code l}, {@code L}, {@code f}, {@code F}, {@code d}, or {@code D}
     */
    protected static boolean isNumericTypeSuffix(final char ch) {
        return ch == 'l' || ch == 'L' || ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D';
    }

    /**
     * Recognizes floating decimal text with an {@code L}/{@code l} suffix: a suffix the floating type adapters accept
     * but the JDK parser rejects. Floating text with an {@code F}/{@code f}/{@code D}/{@code d} suffix already parses
     * directly and must not pay for a suffix substring.
     *
     * @param str the trimmed text to test; must not be {@code null}
     * @return {@code true} if {@code str} is exactly an optional sign, an ASCII decimal number with an optional
     *         fraction and exponent, and one {@code L}/{@code l} suffix
     */
    static boolean hasDecimalLongSuffix(final String str) {
        final int end = str.length() - 1;

        if (end <= 0) {
            return false;
        }

        final char suffix = str.charAt(end);

        return (suffix == 'L' || suffix == 'l') && decimalEnd(str, 0, end, true) == end;
    }

    /**
     * Recognizes decimal integer text with an {@code F}/{@code f}/{@code D}/{@code d} suffix: a suffix the integral
     * type adapters accept but {@code Numbers.toInt}/{@code toLong} reject. Hexadecimal text is never recognized, so
     * the final {@code F} or {@code D} of a hexadecimal integer stays a digit.
     *
     * @param str the text to test; must not be {@code null}
     * @return {@code true} if {@code str} is exactly an optional sign, ASCII decimal digits, and one {@code F},
     *         {@code f}, {@code D} or {@code d} suffix
     */
    static boolean hasDecimalFloatingSuffix(final String str) {
        final int end = str.length() - 1;

        if (end <= 0) {
            return false;
        }

        final char suffix = str.charAt(end);

        return (suffix == 'F' || suffix == 'f' || suffix == 'D' || suffix == 'd') && decimalEnd(str, 0, end, false) == end;
    }

    /**
     * Returns the end of an ASCII decimal token, or -1 if its significand/exponent lacks digits.
     * The caller checks the following character or the end index; whitespace, suffixes, hexadecimal
     * syntax and special floating values are deliberately not consumed here.
     *
     * @param str the text containing the token
     * @param from the index of the token's first character (an optional sign)
     * @param to the exclusive index the token cannot extend past
     * @param floatingPoint {@code true} to also accept a fraction and an exponent, {@code false} for integer digits only
     * @return the exclusive end index of the token, or {@code -1} if it has no significand digits or an exponent
     *         without digits
     */
    static int decimalEnd(final String str, final int from, final int to, final boolean floatingPoint) {
        int pos = from;

        if (pos < to && (str.charAt(pos) == '+' || str.charAt(pos) == '-')) {
            pos++;
        }

        final int integerStart = pos;

        while (pos < to && isDigit(str.charAt(pos))) {
            pos++;
        }

        int digits = pos - integerStart;

        if (floatingPoint && pos < to && str.charAt(pos) == '.') {
            final int fractionStart = ++pos;

            while (pos < to && isDigit(str.charAt(pos))) {
                pos++;
            }

            digits += pos - fractionStart;
        }

        if (digits == 0) {
            return -1;
        }

        if (floatingPoint && pos < to && (str.charAt(pos) == 'e' || str.charAt(pos) == 'E')) {
            pos++;

            if (pos < to && (str.charAt(pos) == '+' || str.charAt(pos) == '-')) {
                pos++;
            }

            final int exponentStart = pos;

            while (pos < to && isDigit(str.charAt(pos))) {
                pos++;
            }

            if (pos == exponentStart) {
                return -1;
            }
        }

        return pos;
    }

    /**
     * Parses an integer from a region of a character array.
     * <p>
     * Parses an optionally signed run of ASCII decimal digits inline at any length, including leading zeros and
     * the 10-digit values up to the {@code int} bounds. A region longer than 9 characters that holds any other
     * character, or whose value overflows, falls back to {@link Numbers#toInt(String)}; a shorter malformed region is
     * rejected directly. A trailing type suffix ({@code l}, {@code L}, {@code f}, {@code F}, {@code d}, {@code D})
     * is accepted and ignored. Supports an optional leading sign ({@code '+'} or {@code '-'}).
     * Returns {@code 0} when {@code cbuf} is {@code null} or {@code length == 0}.
     * </p>
     *
     * @param cbuf the character array; may be {@code null}
     * @param offset the starting position within {@code cbuf}
     * @param length the number of characters to parse
     * @return the parsed integer value
     * @throws IllegalArgumentException if {@code offset} or {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code cbuf} is non-null, {@code length > 0}, and the selected region
     *         extends beyond the array
     * @throws NumberFormatException if the characters cannot be parsed as an integer
     * @throws ArithmeticException if the digits form a well-formed number outside the {@code int} range
     *         (e.g. {@code "2147483648"}); unlike {@link Integer#parseInt(String)}, the fallback path reports
     *         overflow this way rather than as a {@code NumberFormatException}
     * @see Integer#parseInt(String)
     * @see Numbers#toInt(String)
     */
    protected static int parseInt(final char[] cbuf, final int offset, int length)
            throws IllegalArgumentException, IndexOutOfBoundsException, NumberFormatException, ArithmeticException {
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("'offset' and 'len' cannot be negative");
        }

        if (cbuf == null || length == 0) {
            return 0;
        }

        char ch = cbuf[offset + length - 1];
        final int tokenLength = length; // error messages name the whole token, a stripped suffix included

        if (isNumericTypeSuffix(ch)) {
            length = length - 1; // ignore the suffix

            if (length == 0 || (cbuf[offset + length - 1] < '0' || cbuf[offset + length - 1] > '9')) {
                throw new NumberFormatException("Invalid numeric String: \"" + new String(cbuf, offset, tokenLength) + "\"");
            }
        }

        switch (length) {
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

                for (int i = (cbuf[offset] == '-' || cbuf[offset] == '+') ? offset + 1 : offset, to = offset + length; i < to; i++) {
                    ch = cbuf[i];

                    if (ch < '0' || ch > '9') {
                        throw new NumberFormatException("Invalid numeric String: \"" + new String(cbuf, offset, tokenLength) + "\"");
                    }

                    result = result * 10 + (ch - '0');
                }

                return isNegative ? -result : result;
            }

            default: {
                final boolean isNegative = cbuf[offset] == '-';
                final int limit = isNegative ? Integer.MIN_VALUE : -Integer.MAX_VALUE;
                final int multiplyLimit = limit / 10;
                final int to = offset + length;
                int i = (isNegative || cbuf[offset] == '+') ? offset + 1 : offset;
                int result = 0;

                // Accumulate negatively so MIN_VALUE is representable. The fallback validates the
                // whole token on overflow or other syntax and retains its original diagnostics.
                for (; i < to; i++) {
                    final int digit = cbuf[i] - '0';

                    if (digit < 0 || digit > 9 || result < multiplyLimit) {
                        break;
                    }

                    final int multiplied = result * 10;

                    if (multiplied < limit + digit) {
                        break;
                    }

                    result = multiplied - digit;
                }

                if (i == to) {
                    return isNegative ? result : -result;
                }

                try {
                    return Numbers.toInt(new String(cbuf, offset, length));
                } catch (final NumberFormatException e) {
                    if (length == tokenLength) {
                        throw e;
                    }

                    // Numbers.toInt saw the token without its stripped suffix: name the whole token, as the messages above do.
                    throw (NumberFormatException) new NumberFormatException("Invalid numeric String: \"" + new String(cbuf, offset, tokenLength) + "\"")
                            .initCause(e);
                }
            }
        }
    }

    /**
     * Parses a long from a region of a character array.
     * <p>
     * Parses an optionally signed run of ASCII decimal digits inline at any length, including leading zeros and
     * the 19-digit values up to the {@code long} bounds. A region longer than 18 characters that holds any other
     * character, or whose value overflows, falls back to {@link Numbers#toLong(String)}; a shorter malformed region is
     * rejected directly. A trailing type suffix ({@code l}, {@code L}, {@code f}, {@code F}, {@code d}, {@code D})
     * is accepted and ignored. Supports an optional leading sign ({@code '+'} or {@code '-'}).
     * Returns {@code 0L} when {@code cbuf} is {@code null} or {@code length == 0}.
     * </p>
     *
     * @param cbuf the character array; may be {@code null}
     * @param offset the starting position within {@code cbuf}
     * @param length the number of characters to parse
     * @return the parsed long value
     * @throws IllegalArgumentException if {@code offset} or {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code cbuf} is non-null, {@code length > 0}, and the selected region
     *         extends beyond the array
     * @throws NumberFormatException if the characters cannot be parsed as a long
     * @throws ArithmeticException if the digits form a well-formed number outside the {@code long} range
     *         (e.g. {@code "9223372036854775808"}); unlike {@link Long#parseLong(String)}, the fallback path
     *         reports overflow this way rather than as a {@code NumberFormatException}
     * @see Long#parseLong(String)
     * @see Numbers#toLong(String)
     */
    protected static long parseLong(final char[] cbuf, final int offset, int length)
            throws IllegalArgumentException, IndexOutOfBoundsException, NumberFormatException, ArithmeticException {
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("'offset' and 'len' cannot be negative");
        }

        if (cbuf == null || length == 0) {
            return 0;
        }

        char ch = cbuf[offset + length - 1];
        final int tokenLength = length; // error messages name the whole token, a stripped suffix included

        if (isNumericTypeSuffix(ch)) {
            length = length - 1; // ignore the suffix

            if (length == 0 || (cbuf[offset + length - 1] < '0' || cbuf[offset + length - 1] > '9')) {
                throw new NumberFormatException("Invalid numeric String: \"" + new String(cbuf, offset, tokenLength) + "\"");
            }
        }

        switch (length) {
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

                for (int i = (cbuf[offset] == '-' || cbuf[offset] == '+') ? offset + 1 : offset, to = offset + length; i < to; i++) {
                    ch = cbuf[i];

                    if (ch < '0' || ch > '9') {
                        throw new NumberFormatException("Invalid numeric String: \"" + new String(cbuf, offset, tokenLength) + "\"");
                    }

                    result = result * 10 + (ch - '0');
                }

                return isNegative ? -result : result;
            }

            default: {
                final boolean isNegative = cbuf[offset] == '-';
                final long limit = isNegative ? Long.MIN_VALUE : -Long.MAX_VALUE;
                final long multiplyLimit = limit / 10;
                final int to = offset + length;
                int i = (isNegative || cbuf[offset] == '+') ? offset + 1 : offset;
                long result = 0;

                for (; i < to; i++) {
                    final int digit = cbuf[i] - '0';

                    if (digit < 0 || digit > 9 || result < multiplyLimit) {
                        break;
                    }

                    final long multiplied = result * 10;

                    if (multiplied < limit + digit) {
                        break;
                    }

                    result = multiplied - digit;
                }

                if (i == to) {
                    return isNegative ? result : -result;
                }

                try {
                    return Numbers.toLong(new String(cbuf, offset, length));
                } catch (final NumberFormatException e) {
                    if (length == tokenLength) {
                        throw e;
                    }

                    // Numbers.toLong saw the token without its stripped suffix: name the whole token, as the messages above do.
                    throw (NumberFormatException) new NumberFormatException("Invalid numeric String: \"" + new String(cbuf, offset, tokenLength) + "\"")
                            .initCause(e);
                }
            }
        }
    }

    /**
     * Parses text into a {@code Boolean} with the library-wide rule for text:
     * <ul>
     *   <li>{@code "1"}, {@code "y"}, {@code "Y"} and {@code "true"} (in any case) &rarr; {@code true};</li>
     *   <li>{@code "0"}, {@code "-1"}, {@code "n"}, {@code "N"} and {@code "false"} (in any case) &rarr; {@code false};</li>
     *   <li>anything else is rejected. Numeric text is not parsed, so {@code "2"}, {@code "1.0"} and {@code "01"} are
     *       rejected, as are {@code "yes"}, {@code "T"} and text that is nothing but padding.</li>
     * </ul>
     * <p>
     * Leading and trailing {@linkplain #isPadding(char) padding} is removed first, so {@code " Y"} and
     * {@code " true "} are accepted. The callers test for blank text with
     * {@link Strings#isBlank(CharSequence)}, which follows {@link Character#isWhitespace(char)}: stripping here
     * with the same definition is what keeps text padded with Unicode whitespace above {@code U+0020} (U+3000
     * followed by {@code "true"}, say) - text that is not blank - from being read with its padding still attached.
     * "In any case" means ASCII case only: {@code "TRUE"} and {@code "False"} are accepted, but a non-ASCII letter
     * that {@link String#equalsIgnoreCase(String)} would match (the long s, {@code U+017F}) is not.
     * </p>
     *
     * @param str the text to parse; must not be {@code null}
     * @return {@link Boolean#TRUE} or {@link Boolean#FALSE}
     * @throws NullPointerException if {@code str} is {@code null}
     * @throws IllegalArgumentException if {@code str}, without its padding, is none of the texts above
     * @see #booleanOf(Number)
     */
    protected static Boolean parseBoolean(final String str) throws NullPointerException, IllegalArgumentException {
        final Boolean result = booleanToken(stripPadding(str));

        if (result == null) {
            throw notABooleanText(str);
        }

        return result;
    }

    /**
     * Reads {@code text} as one of the texts {@link #parseBoolean(String)} accepts, exactly: no padding is removed.
     *
     * @param text the text to read; must not be {@code null}
     * @return {@link Boolean#TRUE} or {@link Boolean#FALSE}, or {@code null} if {@code text} is none of the accepted texts
     */
    private static Boolean booleanToken(final String text) {
        switch (text.length()) {
            case 1:
                return booleanToken(text.charAt(0));

            case 2:
                return text.charAt(0) == '-' && text.charAt(1) == '1' ? Boolean.FALSE : null;

            case 4:
                return (text.charAt(0) | 0x20) == 't' && (text.charAt(1) | 0x20) == 'r' && (text.charAt(2) | 0x20) == 'u' && (text.charAt(3) | 0x20) == 'e'
                        ? Boolean.TRUE
                        : null;

            case 5:
                return (text.charAt(0) | 0x20) == 'f' && (text.charAt(1) | 0x20) == 'a' && (text.charAt(2) | 0x20) == 'l' && (text.charAt(3) | 0x20) == 's'
                        && (text.charAt(4) | 0x20) == 'e' ? Boolean.FALSE : null;

            default:
                return null;
        }
    }

    /**
     * Reads the region {@code cbuf[offset, offset + length)} as one of the texts {@link #parseBoolean(String)} accepts,
     * exactly: no padding is removed. The {@code char[]} readers use it to answer the common cases without allocating,
     * and hand anything else to their {@code String} overload, which removes padding, treats blank text as absent and
     * rejects the rest.
     *
     * @param cbuf the characters; must not be {@code null}
     * @param offset the index of the first character of the region
     * @param length the length of the region
     * @return {@link Boolean#TRUE} or {@link Boolean#FALSE}, or {@code null} if the region is none of the accepted texts
     * @throws IndexOutOfBoundsException if the region is not inside {@code cbuf}
     */
    static Boolean booleanToken(final char[] cbuf, final int offset, final int length) throws IndexOutOfBoundsException {
        switch (length) {
            case 1:
                return booleanToken(cbuf[offset]);

            case 2:
                return cbuf[offset] == '-' && cbuf[offset + 1] == '1' ? Boolean.FALSE : null;

            case 4:
                return (cbuf[offset] | 0x20) == 't' && (cbuf[offset + 1] | 0x20) == 'r' && (cbuf[offset + 2] | 0x20) == 'u' && (cbuf[offset + 3] | 0x20) == 'e'
                        ? Boolean.TRUE
                        : null;

            case 5:
                return (cbuf[offset] | 0x20) == 'f' && (cbuf[offset + 1] | 0x20) == 'a' && (cbuf[offset + 2] | 0x20) == 'l' && (cbuf[offset + 3] | 0x20) == 's'
                        && (cbuf[offset + 4] | 0x20) == 'e' ? Boolean.FALSE : null;

            default:
                return null;
        }
    }

    /**
     * Reads one character as one of the one-character texts {@link #parseBoolean(String)} accepts.
     *
     * @param ch the character to read
     * @return {@link Boolean#TRUE} for {@code '1'}, {@code 'y'} and {@code 'Y'}; {@link Boolean#FALSE} for {@code '0'},
     *         {@code 'n'} and {@code 'N'}; {@code null} for any other character
     */
    private static Boolean booleanToken(final char ch) {
        if (ch == '1' || ch == 'y' || ch == 'Y') {
            return Boolean.TRUE;
        } else if (ch == '0' || ch == 'n' || ch == 'N') {
            return Boolean.FALSE;
        }

        return null;
    }

    /**
     * The library-wide rule for reading a number as a boolean. Only the integral wrapper types {@link Integer},
     * {@link Short} and {@link Byte} are read: {@code 1} is {@code true}, {@code 0} and {@code -1} are {@code false},
     * and any other value ({@code 2}, {@code -2}) is rejected. A {@code Number} of any other class is rejected whatever
     * its value - {@code Long}, {@code Float}, {@code Double}, {@code BigInteger}, {@code BigDecimal},
     * {@code AtomicInteger}, {@code AtomicLong} and any custom subclass - so {@code 1L} and {@code 1.0} are rejected too.
     * A {@code null} number is {@code false}. Text is read by {@link #parseBoolean(String)}, which never parses numbers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * booleanOf(1);                  // returns true
     * booleanOf((short) 0);          // returns false
     * booleanOf((byte) -1);          // returns false
     * booleanOf(null);               // returns false
     * booleanOf(2);                  // throws IllegalArgumentException: "Cannot convert Integer value 2 to boolean: ..."
     * booleanOf(1L);                 // throws IllegalArgumentException: "Cannot convert java.lang.Long value 1 to boolean: ..."
     * booleanOf(1.0);                // throws IllegalArgumentException: "Cannot convert java.lang.Double value 1.0 to boolean: ..."
     * }</pre>
     *
     * <p>The exception message names the number's type and value and the values that are accepted. The value of a
     * rejected class is shown bounded and escaped: a very wide {@code BigInteger} or {@code BigDecimal} is summarized by
     * its sign and size, longer text is cut, and control characters are escaped.</p>
     *
     * @param number the number to read; may be {@code null}
     * @return {@code true} for {@code 1}; {@code false} for {@code 0}, {@code -1} or {@code null}
     * @throws IllegalArgumentException if {@code number} is an {@code Integer}, {@code Short} or {@code Byte} other than
     *         {@code -1}, {@code 0} or {@code 1}, or a {@code Number} of any other class
     * @see #parseBoolean(String)
     */
    static boolean booleanOf(final Number number) throws IllegalArgumentException {
        if (number == null) {
            return false;
        }

        if (number instanceof Integer || number instanceof Byte || number instanceof Short) {
            final int i = number.intValue();

            if (i == 1) {
                return true;
            } else if (i == 0 || i == -1) {
                return false;
            }

            // Name the type with the value: "Short value 300" says more than "the number 300".
            throw new IllegalArgumentException("Cannot convert " + number.getClass().getSimpleName() + " value " + i
                    + " to boolean: only 1 (true), 0 (false) and -1 (false) are accepted");
        } else {
            // The class is the problem here, so name it in full (a custom Number may share a simple name with a JDK class),
            // with the value it held. The value goes through describeForMessage: a BigInteger may have millions of digits,
            // and a custom Number's toString() may be long or hold a line break.
            throw new IllegalArgumentException("Cannot convert " + number.getClass().getName() + " value " + describeForMessage(number)
                    + " to boolean: only Integer, Short and Byte values 1 (true), 0 (false) and -1 (false) are accepted");
        }
    }

    /**
     * Creates the exception for text that is not a boolean value.
     *
     * @param text the rejected text, as given
     * @return the exception to throw
     */
    static IllegalArgumentException notABooleanText(final String text) {
        return new IllegalArgumentException("Cannot convert \"" + describeForMessage(text)
                + "\" to boolean: only 1, y, Y and true (any case) are true, and only 0, -1, n, N and false (any case) are false");
    }

    /** The most characters of a rejected value's text that {@link #describeForMessage(Object)} quotes. */
    private static final int MAX_TEXT_IN_MESSAGE = 64;

    /** The widest {@code BigInteger}, or unscaled {@code BigDecimal}, that {@link #describeForMessage(Object)} prints. */
    private static final int MAX_NUMBER_BITS_IN_MESSAGE = 192;

    /**
     * Renders a rejected value for an exception message, bounded as {@link Numbers} bounds its own diagnostics: a
     * {@code BigInteger} or {@code BigDecimal} too wide to print cheaply is summarized by its sign and size, longer text
     * is cut (never inside a surrogate pair) and followed by its length, and control characters, the line and paragraph
     * separators, Unicode format characters (a bidirectional control, a zero-width space) and unpaired surrogates are
     * escaped. So a hostile megabyte of text or a million-digit number costs neither a slow {@code toString()} nor a
     * megabyte message, and text or a {@code Number} subtype whose {@code toString()} holds a line break or a
     * display-order control cannot inject it into a log line.
     *
     * @param value the rejected number or text
     * @return a short, single-line description of {@code value}
     */
    private static String describeForMessage(final Object value) {
        if (value instanceof BigInteger bigInteger && bigInteger.bitLength() > MAX_NUMBER_BITS_IN_MESSAGE) {
            return "BigInteger[signum=" + bigInteger.signum() + ", bitLength=" + bigInteger.bitLength() + "]";
        } else if (value instanceof BigDecimal bigDecimal && bigDecimal.unscaledValue().bitLength() > MAX_NUMBER_BITS_IN_MESSAGE) {
            return "BigDecimal[signum=" + bigDecimal.signum() + ", unscaledBitLength=" + bigDecimal.unscaledValue().bitLength() + ", scale="
                    + bigDecimal.scale() + "]";
        }

        final String text = String.valueOf(value);
        int cut = Math.min(text.length(), MAX_TEXT_IN_MESSAGE);

        // Do not split a surrogate pair.
        if (cut < text.length() && Character.isHighSurrogate(text.charAt(cut - 1))) {
            cut--;
        }

        final StringBuilder sb = new StringBuilder(cut + 24);

        for (int i = 0; i < cut; i++) {
            final char ch = text.charAt(i);

            if (Character.isHighSurrogate(ch) && i + 1 < cut && Character.isLowSurrogate(text.charAt(i + 1))) {
                // A well-formed pair (an emoji, say) is printable, unless it encodes a format character (a tag).
                final char low = text.charAt(++i);

                if (Character.getType(Character.toCodePoint(ch, low)) == Character.FORMAT) {
                    appendUnicodeEscape(sb, ch);
                    appendUnicodeEscape(sb, low);
                } else {
                    sb.append(ch).append(low);
                }
            } else if (Character.isISOControl(ch) || ch == 0x2028 || ch == 0x2029 || Character.isSurrogate(ch) || Character.getType(ch) == Character.FORMAT) {
                appendUnicodeEscape(sb, ch);
            } else {
                sb.append(ch);
            }
        }

        if (cut < text.length()) {
            sb.append("...[").append(text.length()).append(" chars]");
        }

        return sb.toString();
    }

    /**
     * Appends {@code ch} as a six-character Unicode escape: a backslash, the letter {@code u} and four hexadecimal digits.
     *
     * @param sb the builder to append to
     * @param ch the character to escape
     */
    private static void appendUnicodeEscape(final StringBuilder sb, final char ch) {
        final String hex = Integer.toHexString(ch);
        sb.append("\\u").append("0000", hex.length(), 4).append(hex);
    }

    /**
     * Tests whether {@code ch} is padding for the purposes of {@link #parseBoolean(String)}, i.e. a character that
     * either {@link Strings#isBlank(CharSequence)} (which follows {@link Character#isWhitespace(char)}) or
     * {@link String#trim()} (which removes every character {@code <= ' '}) would discard.
     * <p>
     * The two definitions do not coincide - U+3000 is Unicode whitespace but is greater than {@code ' '}, and
     * U+0001 is {@code <= ' '} but is not Unicode whitespace - so both are treated as padding. Honouring only one
     * of the two leaves the readers that go through {@code char[]} disagreeing with those that go through
     * {@code String} on the same text.
     * </p>
     *
     * @param ch the character to test
     * @return {@code true} if a leading or trailing {@code ch} is removed before parsing
     */
    static boolean isPadding(final char ch) {
        return ch <= ' ' || Character.isWhitespace(ch);
    }

    /**
     * Removes leading and trailing {@linkplain #isPadding(char) padding} from {@code str}.
     *
     * @param str the string to strip; must not be {@code null}
     * @return {@code str} without leading and trailing padding
     */
    static String stripPadding(final String str) {
        int start = 0;
        int end = str.length();

        while (start < end && isPadding(str.charAt(start))) {
            start++;
        }

        while (end > start && isPadding(str.charAt(end - 1))) {
            end--;
        }

        return start == 0 && end == str.length() ? str : str.substring(start, end);
    }

    /**
     * Parses {@code str} as a {@code char}.
     * <p>
     * A single-character string yields that character. A longer string is parsed as a numeric UTF-16
     * code-unit value in {@code [Character.MIN_VALUE, Character.MAX_VALUE]} (for example {@code "65"}
     * yields {@code 'A'}). Callers are responsible for {@code null} or empty input.
     * </p>
     *
     * @param str the string to parse; must not be {@code null} or empty
     * @return the character represented by {@code str}
     * @throws NullPointerException if {@code str} is {@code null}
     * @throws NumberFormatException if {@code str} is empty or has more than one character and cannot be parsed as an integer
     * @throws IllegalArgumentException if the parsed integer is outside the {@code char} range
     */
    protected static char parseChar(final String str) throws NullPointerException, NumberFormatException, IllegalArgumentException {
        if (str.length() == 1) {
            return str.charAt(0);
        }

        final int intValue = Integer.parseInt(str);

        if (intValue < Character.MIN_VALUE || intValue > Character.MAX_VALUE) {
            throw new IllegalArgumentException("Integer value out of char range: " + intValue);
        }

        return (char) intValue;
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
     * @throws IllegalArgumentException if {@code targetClass} is {@code null}
     * @throws NullPointerException if {@code rs} is {@code null}
     * @throws SQLException if a database access error occurs
     */
    protected static <T> T getColumnValue(final ResultSet rs, final int columnIndex, final Class<? extends T> targetClass)
            throws IllegalArgumentException, NullPointerException, SQLException {
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
     * @throws IllegalArgumentException if {@code targetClass} is {@code null}
     * @throws NullPointerException if {@code rs} is {@code null}
     * @throws SQLException if a database access error occurs
     */
    protected static <T> T getColumnValue(final ResultSet rs, final String columnName, final Class<? extends T> targetClass)
            throws IllegalArgumentException, NullPointerException, SQLException {
        return Type.of(targetClass).get(rs, columnName);
    }

    /**
     * Calculates a buffer size for string operations, capped at {@link Integer#MAX_VALUE}.
     * <p>
     * Prevents integer overflow by clamping the result when {@code length * elementPlusDelimiterLen}
     * would exceed {@link Integer#MAX_VALUE}. Returns {@code 0} when
     * {@code elementPlusDelimiterLen} is {@code 0}.
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
     * @param length the number of elements
     * @param elementPlusDelimiterLen the length of each element plus delimiter
     * @return the calculated buffer size, capped at {@link Integer#MAX_VALUE}, or {@code 0} if
     *         {@code elementPlusDelimiterLen} is {@code 0}
     */
    protected static int calculateBufferSize(final int length, final int elementPlusDelimiterLen) {
        if (elementPlusDelimiterLen == 0) {
            return 0;
        }

        return length > Integer.MAX_VALUE / elementPlusDelimiterLen ? Integer.MAX_VALUE : length * elementPlusDelimiterLen;
    }
}
