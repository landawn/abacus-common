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
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link Pair} objects, providing serialization and deserialization capabilities.
 * This type handler manages the conversion between Pair objects and their string representations,
 * supporting both JSON serialization and custom formatted output.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a PairType for String and Integer
 * Type<Pair<String, Integer>> type = TypeFactory.getType("Pair<String, Integer>");
 *
 * // Serialize a Pair to string
 * Pair<String, Integer> pair = Pair.of("age", 25);
 * String json = type.stringOf(pair);   // Returns ["age", 25]
 *
 * // Deserialize a string to Pair
 * Pair<String, Integer> restored = type.valueOf("[\"age\", 25]");
 * }</pre>
 *
 * @param <L> the type of the left element in the pair
 * @param <R> the type of the right element in the pair
 */
@SuppressWarnings("java:S2160")
public class PairType<L, R> extends AbstractType<Pair<L, R>> {

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<Pair<L, R>> typeClass = (Class) Pair.class; //NOSONAR

    private final Type<L> leftType;

    private final Type<R> rightType;

    private final List<Type<?>> parameterTypes;

    /**
     * Constructs a new PairType with the specified left and right type names.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     *
     * @param leftTypeName the type name for the left element of the pair
     * @param rightTypeName the type name for the right element of the pair
     * @throws IllegalArgumentException if a supplied type name is {@code null}, blank, or structurally invalid.
     */
    PairType(final String leftTypeName, final String rightTypeName) throws IllegalArgumentException {
        super(getTypeName(leftTypeName, rightTypeName, false));

        declaringName = getTypeName(leftTypeName, rightTypeName, true);
        leftType = TypeFactory.getType(leftTypeName);
        rightType = TypeFactory.getType(rightTypeName);
        parameterTypes = List.of(leftType, rightType);
    }

    /**
     * Returns the declaring name of this type, which includes the simple class name and parameter types.
     * For example: "Pair&lt;String, Integer&gt;" for a Pair with String left type and Integer right type.
     *
     * @return the declaring name of this type
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Pair type.
     *
     * @return the Class object for Pair
     */
    @Override
    public Class<Pair<L, R>> javaType() {
        return typeClass;
    }

    /**
     * Returns an immutable list containing the Type objects for the left and right elements of the Pair.
     * The first element is the type of the left value, and the second element is the type of the right value.
     *
     * @return an immutable list of Type objects representing the parameter types
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type with type parameters.
     * Pair types are always parameterized with the left and right value types.
     *
     * @return {@code true}, as Pair is a generic type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Converts a Pair object to its string representation using JSON format.
     * The pair is serialized as a JSON array with two elements: [leftValue, rightValue].
     * Returns {@code null} if the input pair is {@code null}.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Pair object to convert to string
     * @return a JSON string representation of the pair, or {@code null} if the input is null
     * @throws RuntimeException if a value or bean property cannot be serialized by its selected type handler.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Pair<L, R> x) throws RuntimeException {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x.left(), x.right()), Utils.jsc);
    }

    /**
     * Parses a string representation and creates a Pair object.
     * The string should be in JSON array format: [leftValue, rightValue].
     * Returns {@code null} if the input string is {@code null} or empty.
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
     * @param str the string to parse, expected to be a JSON array with exactly two elements
     * @return a Pair object created from the parsed values, or {@code null} if the input is {@code null} or empty (a
     *         blank, non-empty string is not treated as empty and is rejected)
     * @throws IllegalArgumentException if the parsed value is not an array with exactly 2 elements (this includes a         blank string, unbalanced brackets and trailing text)
     * @throws ParsingException if an element token is not valid JSON for its declared element type
     * @throws NumberFormatException if a numeric element token cannot be converted to the declared element type
     * @throws ArithmeticException if a numeric element is outside the range accepted by its declared type.
     * @see #valueOf(Object)
     * @see #stringOf(Pair)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    @Override
    public Pair<L, R> valueOf(final String str) throws IllegalArgumentException, ParsingException, NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.parseTupleElements(str, name(), parameterTypes);
        final L left = (L) a[0];
        final R right = (R) a[1];

        return Pair.of(left, right);
    }

    /**
     * Appends the string representation of a Pair object to an Appendable.
     * The pair is formatted as [leftValue, rightValue] with appropriate element separation.
     * If the pair is {@code null}, appends "null". Handles Writer instances with buffering optimization.
     * <p>
     * Each element is appended by its declared element type handler. When that declared type is {@code Object} the
     * handler of the element's runtime class is used instead, exactly as
     * {@link #serializeTo(CharacterWriter, Pair, JsonXmlSerConfig)} does, so a map, collection or bean element keeps
     * the {@code toString()}-style form rather than falling back to {@code ObjectType}'s JSON {@code stringOf}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the Pair object to append
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
    public void appendTo(final Appendable appendable, final Pair<L, R> x) throws NullPointerException, IOException, RuntimeException {
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
                AbstractTupleType.appendElement(appendable, rightType, x.right());

                appendable.append(SK._BRACKET_R);
            }
        }
    }

    /**
     * Writes the character representation of a Pair object to a CharacterWriter.
     * The pair is formatted as [leftValue, rightValue] using the provided serialization configuration.
     * If the pair is {@code null}, writes "null".
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * Each element is written by its declared element type handler. When the declared element type is {@code Object}
     * the handler of the element's runtime class is used instead, so a {@code Pair<Object, Object>} holding {@code 1}
     * and {@code "a"} is written as {@code [1, "a"]} rather than {@code ["1", "a"]}. An element whose (declared or
     * runtime) handler is not {@linkplain Type#isSerializable() serializable} - a bean, a map, a {@code List<Object>} -
     * is written as embedded JSON (not as a quoted JSON string) when {@code config} is a {@code JsonSerConfig}; under any
     * other config its {@code stringOf} text is written with the writer's character escaping. A {@code null} element is
     * written by its declared handler, so that handler's null-substitution flags apply. The result matches
     * {@link #stringOf(Pair)} for every element shape.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Pair object to write
     * @param config the serialization configuration to use, may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Pair<L, R> x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, RuntimeException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            AbstractTupleType.serializeSlot(writer, leftType, x.left(), config);
            writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
            AbstractTupleType.serializeSlot(writer, rightType, x.right(), config);

            writer.write(SK._BRACKET_R);
        }
    }

    /**
     * Generates a type name for a Pair type with the specified left and right type names.
     *
     * @param leftTypeName the name of the left type
     * @param rightTypeName the name of the right type
     * @param isDeclaringName if {@code true}, uses simple class names; if {@code false}, uses canonical class names
     * @return the generated type name string
     * @throws IllegalArgumentException if a supplied type name is {@code null}, blank, or structurally invalid.
     */
    protected static String getTypeName(final String leftTypeName, final String rightTypeName, final boolean isDeclaringName) throws IllegalArgumentException {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Pair.class) + SK.LESS_THAN + TypeFactory.getType(leftTypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(rightTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Pair.class) + SK.LESS_THAN + TypeFactory.getType(leftTypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(rightTypeName).name() + SK.GREATER_THAN;
        }
    }
}
