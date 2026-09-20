/*
 * Copyright (C) 2017 HaiYang Li
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Triple;

/**
 * Type handler for {@link Triple} objects. This class provides serialization and
 * deserialization support for Triple instances, which contain three values (left, middle, right).
 * The serialization format is a JSON array: [left, middle, right].
 *
 * @param <L> the type of the left element
 * @param <M> the type of the middle element
 * @param <R> the type of the right element
 */
@SuppressWarnings("java:S2160")
public class TripleType<L, M, R> extends AbstractType<Triple<L, M, R>> {

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<Triple<L, M, R>> typeClass = (Class) Triple.class; //NOSONAR

    private final Type<L> leftType;

    private final Type<M> middleType;

    private final Type<R> rightType;

    private final List<Type<?>> parameterTypes;

    /**
     * Constructs a TripleType instance with the specified element types.
     * This constructor is package-private and should only be called by TypeFactory.
     *
     * @param leftTypeName the name of the left element type
     * @param middleTypeName the name of the middle element type
     * @param rightTypeName the name of the right element type
     * @throws IllegalArgumentException if a supplied type name is {@code null}, blank, or structurally invalid.
     */
    TripleType(final String leftTypeName, final String middleTypeName, final String rightTypeName) throws IllegalArgumentException {
        super(getTypeName(leftTypeName, middleTypeName, rightTypeName, false));

        declaringName = getTypeName(leftTypeName, middleTypeName, rightTypeName, true);

        leftType = TypeFactory.getType(leftTypeName);
        middleType = TypeFactory.getType(middleTypeName);
        rightType = TypeFactory.getType(rightTypeName);
        parameterTypes = List.of(leftType, middleType, rightType);
    }

    /**
     * Returns the declaring name of this type, which uses simple class names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Triple<String, Integer, Double>> type = TypeFactory.getType("Triple<String, Integer, Double>");
     * String name = type.declaringName();   // Returns "Triple<String, Integer, Double>"
     * }</pre>
     *
     * @return the declaring name of this Triple type
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Triple type.
     *
     * @return the Class object for Triple
     */
    @Override
    public Class<Triple<L, M, R>> javaType() {
        return typeClass;
    }

    /**
     * Returns the parameter types of this generic type.
     * For Triple, this is an immutable list containing three elements: left type, middle type, and right type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Triple<String, Integer, Double>> type = TypeFactory.getType("Triple<String, Integer, Double>");
     * List<Type<?>> paramTypes = type.parameterTypes();
     * // paramTypes.get(0) is Type<String>, paramTypes.get(1) is Type<Integer>, paramTypes.get(2) is Type<Double>
     * }</pre>
     *
     * @return an immutable list containing the left, middle, and right types
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a parameterized type.
     * {@code TripleType} is always parameterized as it carries three type parameters.
     *
     * @return {@code true}, indicating this is a parameterized type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Converts a Triple object to its string representation.
     * The format is a JSON array: [left, middle, right].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Triple<String, Integer, Double>> type = TypeFactory.getType("Triple<String, Integer, Double>");
     * Triple<String, Integer, Double> triple = Triple.of("A", 1, 3.14);
     * String str = type.stringOf(triple);   // Returns ["A", 1, 3.14]
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Triple object to convert, may be {@code null}
     * @return the JSON string representation, or {@code null} if {@code x} is {@code null}
     * @throws RuntimeException if a value or bean property cannot be serialized by its selected type handler.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Triple<L, M, R> x) throws RuntimeException {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x.left(), x.middle(), x.right()), Utils.jsc);
    }

    /**
     * Creates a Triple object from its string representation.
     * Expects a JSON array format: [left, middle, right].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Triple<String, Integer, Double>> type = TypeFactory.getType("Triple<String, Integer, Double>");
     * Triple<String, Integer, Double> triple = type.valueOf("[\"A\", 1, 3.14]");
     * // triple.left() = "A", triple.middle() = 1, triple.right() = 3.14
     * }</pre>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * <p>Each slot is parsed directly from its JSON token using the declared type, preserving decimal
     * precision and scale for decimal types, including nested generic values. Numeric slots follow the
     * JSON parser's conversion rules: for an integral target, unquoted decimals such as {@code 1.5}
     * may truncate toward zero, whereas quoted fractions are rejected. Scientific notation and decimal
     * tokens that the parser cannot convert numerically are passed to the declared type's text parser.</p>
     *
     * @param str the string to parse
     * @return a Triple object containing the parsed values, or {@code null} if {@code str} is {@code null} or empty (a
     *         blank, non-empty string is not treated as empty and is rejected)
     * @throws IllegalArgumentException if the parsed value is not an array containing exactly three elements (this includes
     *         a blank string, unbalanced brackets and trailing text)
     * @throws ParsingException if an element token is not valid JSON for its declared element type
     * @throws NumberFormatException if a numeric element token cannot be converted to the declared element type
     * @throws ArithmeticException if a numeric element is outside the range accepted by its declared type.
     * @see #valueOf(Object)
     * @see #stringOf(Triple)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    @Override
    public Triple<L, M, R> valueOf(final String str) throws IllegalArgumentException, ParsingException, NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.parseTupleElements(str, name(), parameterTypes);
        final L left = (L) a[0];
        final M middle = (M) a[1];
        final R right = (R) a[2];

        return Triple.of(left, middle, right);
    }

    /**
     * Appends the string representation of a Triple object to the given Appendable.
     * Writes the format: [left, middle, right].
     * <p>
     * Each element is appended by its declared element type handler. When that declared type is {@code Object} the
     * handler of the element's runtime class is used instead, exactly as
     * {@link #serializeTo(CharacterWriter, Triple, JsonXmlSerConfig)} does, so a map, collection or bean element keeps
     * the {@code toString()}-style form rather than falling back to {@code ObjectType}'s JSON {@code stringOf}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the Triple object to append, may be {@code null}
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
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
    public void appendTo(final Appendable appendable, final Triple<L, M, R> x) throws NullPointerException, IOException, RuntimeException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR
                Throwable failure = null;

                try {
                    bw.write(SK._BRACKET_L);

                    AbstractTupleType.appendElement(bw, leftType, x.left());
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    AbstractTupleType.appendElement(bw, middleType, x.middle());
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    AbstractTupleType.appendElement(bw, rightType, x.right());

                    bw.write(SK._BRACKET_R);

                    if (!isBufferedWriter) {
                        bw.flush();
                    }
                } catch (final IOException | RuntimeException | Error e) {
                    failure = e;
                    throw e;
                } finally {
                    if (!isBufferedWriter) {
                        Utils.recycle((BufferedWriter) bw, failure);
                    }
                }
            } else {
                appendable.append(SK._BRACKET_L);

                AbstractTupleType.appendElement(appendable, leftType, x.left());
                appendable.append(ELEMENT_SEPARATOR);
                AbstractTupleType.appendElement(appendable, middleType, x.middle());
                appendable.append(ELEMENT_SEPARATOR);
                AbstractTupleType.appendElement(appendable, rightType, x.right());

                appendable.append(SK._BRACKET_R);
            }
        }
    }

    /**
     * Writes the character representation of a Triple object to the given CharacterWriter.
     * This method is used for JSON/XML serialization. Writes the format: [left, middle, right].
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * Each element is written by its declared element type handler. When the declared element type is {@code Object}
     * the handler of the element's runtime class is used instead, so an {@code Object} slot holding {@code 1} is
     * written as {@code 1} rather than {@code "1"}. An element whose (declared or runtime) handler is not
     * {@linkplain Type#isSerializable() serializable} - a bean, a map, a {@code List<Object>} - is written as embedded
     * JSON (not as a quoted JSON string) when {@code config} is a {@code JsonSerConfig}; under any other config its
     * {@code stringOf} text is written with the writer's character escaping. A {@code null} element is written by its
     * declared handler, so that handler's null-substitution flags apply. The representation depends on the supplied configuration;
     * quotation, date formats, and null substitution can differ from {@link #stringOf(Triple)}.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Triple object to write, may be {@code null}
     * @param config the serialization configuration, may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Triple<L, M, R> x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, RuntimeException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            AbstractTupleType.serializeSlot(writer, leftType, x.left(), config);
            writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
            AbstractTupleType.serializeSlot(writer, middleType, x.middle(), config);
            writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
            AbstractTupleType.serializeSlot(writer, rightType, x.right(), config);

            writer.write(SK._BRACKET_R);
        }
    }

    /**
     * Generates the type name for a Triple type with the specified element types.
     *
     * @param leftTypeName the name of the left element type
     * @param middleTypeName the name of the middle element type
     * @param rightTypeName the name of the right element type
     * @param isDeclaringName if {@code true}, uses simple class names; if {@code false}, uses canonical class names
     * @return the generated type name for Triple with the specified element types
     * @throws IllegalArgumentException if a supplied type name is {@code null}, blank, or structurally invalid.
     */
    protected static String getTypeName(final String leftTypeName, final String middleTypeName, final String rightTypeName, final boolean isDeclaringName)
            throws IllegalArgumentException {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Triple.class) + SK.LESS_THAN + TypeFactory.getType(leftTypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(middleTypeName).declaringName() + SK.COMMA_SPACE + TypeFactory.getType(rightTypeName).declaringName()
                    + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Triple.class) + SK.LESS_THAN + TypeFactory.getType(leftTypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(middleTypeName).name() + SK.COMMA_SPACE + TypeFactory.getType(rightTypeName).name() + SK.GREATER_THAN;
        }
    }
}
