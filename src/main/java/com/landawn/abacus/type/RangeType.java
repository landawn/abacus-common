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
import java.lang.reflect.Array;
import java.util.List;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Range;
import com.landawn.abacus.util.Range.BoundType;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link Range} objects containing comparable values.
 * This class handles serialization and deserialization of range bounds in bracket notation
 * (for example {@code [1, 10]} for a closed range and {@code (1, 10)} for an open range).
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a RangeType for Integer
 * Type<Range<Integer>> type = TypeFactory.getType("Range<Integer>");
 *
 * // Serialize a Range to string
 * Range<Integer> closedRange = Range.closed(1, 10);   // [1, 10]
 * String str1 = type.stringOf(closedRange);           // Returns "[1, 10]"
 *
 * Range<Integer> openRange = Range.open(1, 10);   // (1, 10)
 * String str2 = type.stringOf(openRange);         // Returns "(1, 10)"
 *
 * // Deserialize string to Range
 * Range<Integer> restored = type.valueOf("[1, 10]");
 * boolean contains5 = restored.contains(5);   // true
 * }</pre>
 *
 * @param <T> the type of values in the range, must be Comparable
 */
@SuppressWarnings("java:S2160")
public class RangeType<T extends Comparable<? super T>> extends AbstractType<Range<T>> {

    /**
     * Shared {@code String} type handler used by {@link #serializeTo(CharacterWriter, Range, JsonXmlSerConfig)}
     * to emit the bracket notation produced by {@link #stringOf(Range)} as a JSON/XML string value.
     */
    static final Type<String> strType = TypeFactory.getType(String.class);

    /**
     * The type name identifier for Range type, equal to the simple class name {@code "Range"}.
     */
    public static final String RANGE = Range.class.getSimpleName();

    private final String declaringName;

    private final Class<Range<T>> typeClass;

    private final List<Type<?>> parameterTypes;

    private final Type<T> elementType;

    /**
     * The two-endpoint array type {@link #valueOf(String)} deserializes into. It depends only on
     * {@link #elementType}, so it is resolved once here rather than rebuilt on every call.
     */
    private final Type<?> endpointArrayType;

    /**
     * Constructs a new RangeType with the specified parameter type.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     *
     * @param parameterTypeName the type name for the range's element type, which must be Comparable
     */
    @SuppressWarnings("rawtypes")
    RangeType(final String parameterTypeName) {
        super(RANGE + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + SK.GREATER_THAN);

        declaringName = RANGE + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + SK.GREATER_THAN;
        typeClass = (Class) Range.class;
        elementType = TypeFactory.getType(parameterTypeName);
        parameterTypes = List.of(elementType);

        // Keep the descriptor, not just its raw class, so generic endpoints retain their value types.
        final Type<?> endpointType = elementType.isPrimitive() ? TypeFactory.getType(ClassUtil.wrap(elementType.javaType())) : elementType;
        endpointArrayType = TypeFactory.getType(endpointType.name() + "[]");
    }

    /**
     * Returns the declaring name of this type, which includes the Range class name
     * and its parameterized type in angle brackets.
     *
     * @return the declaring name in the format "Range&lt;ElementType&gt;"
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Range type.
     *
     * @return the Class object for Range.class
     */
    @Override
    public Class<Range<T>> javaType() {
        return typeClass;
    }

    /**
     * Returns the Type instance for the element type of this Range.
     * This represents the type of values that the Range can contain.
     *
     * @return the Type instance for the element type
     */
    @Override
    public Type<T> elementType() {
        return elementType;
    }

    /**
     * Returns an immutable list containing the Type instances for the parameter types of this Range.
     * For Range, this returns a single-element list containing the element type.
     *
     * @return an immutable list with one Type instance representing the element type of the Range
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a parameterized type.
     * {@code RangeType} is always parameterized as it carries a comparable element type parameter.
     *
     * @return {@code true}, indicating this is a parameterized type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Converts a Range object to its string representation.
     * The format depends on the bound type:
     * <ul>
     *   <li>{@code OPEN_OPEN}: {@code "(lower, upper)"}</li>
     *   <li>{@code OPEN_CLOSED}: {@code "(lower, upper]"}</li>
     *   <li>{@code CLOSED_OPEN}: {@code "[lower, upper)"}</li>
     *   <li>{@code CLOSED_CLOSED}: {@code "[lower, upper]"}</li>
     * </ul>
     * Endpoints are written using the declared element type's serialization, retaining generic arguments,
     * separated by a comma and space. Untyped ranges select the runtime endpoint handler.
     * Endpoints are written with the type system's default serialization config, so {@code java.util.Date},
     * {@code Timestamp}, {@code Instant}, {@code OffsetDateTime} and {@code ZonedDateTime} endpoints are emitted as
     * epoch milliseconds: sub-millisecond precision, the UTC offset and the region zone are not round-tripped (the
     * value is read back, to millisecond precision, in the system default zone). {@code LocalDate}/{@code LocalTime}/
     * {@code LocalDateTime} endpoints are written as ISO text and round-trip exactly.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip, subject to the endpoint
     * caveat above; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Range to convert to string
     * @return the string representation of the Range, or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @MayReturnNull
    @Override
    public String stringOf(final Range<T> x) {
        if (x == null) {
            return null; // NOSONAR
        }

        final BoundType boundType = x.boundType();
        final String prefix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.OPEN_CLOSED) ? "(" : "[";
        final String postfix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.CLOSED_OPEN) ? ")" : "]";
        Type<T> type = elementType;

        // Only an untyped range needs runtime dispatch; a raw class would erase declared endpoint generics.
        if (elementType.isObject()) {
            if (x.lowerEndpoint() != null) {
                type = TypeFactory.getType(x.lowerEndpoint().getClass());
            } else if (x.upperEndpoint() != null) {
                type = TypeFactory.getType(x.upperEndpoint().getClass());
            }
        }

        final BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();
        Throwable failure = null;

        try {
            bw.write(prefix);
            type.serializeTo(bw, x.lowerEndpoint(), Utils.jsc);
            bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
            type.serializeTo(bw, x.upperEndpoint(), Utils.jsc);
            bw.write(postfix);

            return bw.toString();
        } catch (final IOException e) {
            final UncheckedIOException uncheckedException = new UncheckedIOException(e);
            failure = uncheckedException;
            throw uncheckedException;
        } catch (final RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            Utils.recycle(bw, failure);
        }
    }

    /**
     * Parses a string representation of a Range and returns the corresponding Range object.
     * The string should be in one of the following formats:
     * <ul>
     *   <li>{@code "(lower, upper)"} for open-open range</li>
     *   <li>{@code "(lower, upper]"} for open-closed range</li>
     *   <li>{@code "[lower, upper)"} for closed-open range</li>
     *   <li>{@code "[lower, upper]"} for closed-closed range</li>
     * </ul>
     * Endpoints are deserialized as a JSON array using the complete declared element type, including
     * nested generic arguments. Primitive element descriptors are boxed; null endpoints remain invalid.
     * For integral endpoint types, unquoted decimals such as {@code 1.5} may truncate toward zero under
     * the JSON parser's numeric conversion rules. Quoted fractions are rejected; tokens that cannot be
     * converted numerically are passed to the declared element type's text parser.
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return the parsed Range object, or {@code null} if the input string is {@code null} or empty
     * @throws IllegalArgumentException if the string format is invalid or does not contain exactly two endpoints,
     *         or if the endpoints are out of order or {@code null} (rejected by {@code Range} itself)
     * @throws RuntimeException if an endpoint cannot be parsed as the element type; the exception is the element
     *         handler's own, for example {@code NumberFormatException} for {@code ["1.5", 2]} with an
     *         {@code Integer} element type, {@code ArithmeticException} for {@code "[2147483648, 2147483649]"}
     *         (out of the {@code int} range), or {@code com.landawn.abacus.exception.ParsingException} for a
     *         malformed endpoint list such as {@code "[1 5]"}. A nested endpoint list ({@code "[[1, 5]]"}) is
     *         accepted and unwrapped, not rejected.
     * @see #valueOf(Object)
     * @see #stringOf(Range)
     */
    @MayReturnNull
    @Override
    public Range<T> valueOf(String str) throws IllegalArgumentException, RuntimeException {
        str = Strings.trim(str);

        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (str.length() < 3) {
            throw new IllegalArgumentException("Invalid Range format. Expected format like '[lower, upper]' but got: " + str);
        }

        final String prefix = str.substring(0, 1);
        final String postfix = str.substring(str.length() - 1);

        if (!("(".equals(prefix) || "[".equals(prefix)) || !(")".equals(postfix) || "]".equals(postfix))) {
            throw new IllegalArgumentException("Invalid Range format. Expected format like '[lower, upper]' but got: " + str);
        }

        final Object endpoints = Utils.jsonParser.deserialize(str, 1, str.length() - 1, Utils.jdc, endpointArrayType);

        if ((endpoints == null) || (Array.getLength(endpoints) != 2)) {
            throw new IllegalArgumentException("Invalid Range format. Expected exactly 2 endpoints but got: " + str);
        }

        final T lowerEndpoint = (T) Array.get(endpoints, 0);
        final T upperEndpoint = (T) Array.get(endpoints, 1);

        if ("(".equals(prefix)) {
            return ")".equals(postfix) ? Range.open(lowerEndpoint, upperEndpoint) : Range.openClosed(lowerEndpoint, upperEndpoint);
        } else {
            return ")".equals(postfix) ? Range.closedOpen(lowerEndpoint, upperEndpoint) : Range.closed(lowerEndpoint, upperEndpoint);
        }
    }

    /**
     * Appends the string representation of a Range to the given Appendable.
     * The format depends on the bound type and includes the appropriate brackets/parentheses.
     * If the Range is {@code null}, appends "null".
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the Range to append
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if appending the range text or null literal to {@code appendable} fails
     * @implNote
     * This method appends a string representation of {@code x} to {@code appendable} (the literal {@code "null"} for a
     * {@code null} value). Conceptually this is the human-readable form produced by {@code toString()}, <i>not</i> the
     * value returned by {@code stringOf}, which is a formatted, serializable representation (typically a JSON string)
     * that {@link #valueOf(String)} can convert back into an equivalent value. For values whose nested structure makes
     * the two forms differ (collections, maps, arrays), {@code appendTo} emits the unquoted, {@code toString()}-style
     * form; it is therefore not, in the general contract, a plain
     * {@code appendable.append(x == null ? NULL_STRING : stringOf(x))}. (For value types whose human-readable and
     * serialized forms coincide, the appended text is naturally identical to {@code stringOf(x)}.)
     */
    @Override
    public void appendTo(final Appendable appendable, final Range<T> x) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(stringOf(x));
        }
    }

    /**
     * Writes the character representation of a Range to the given CharacterWriter.
     * The Range is first converted to a string representation, then written as a quoted string
     * according to the serialization configuration.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Range to write
     * @param config the serialization configuration that determines string quotation
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the range text, configured string quotation or null literal to {@code writer} fails
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Range<T> x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            strType.serializeTo(writer, stringOf(x), config);
        }
    }
}
