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

import com.landawn.abacus.exception.UncheckedIOException;
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
     */
    PairType(final String leftTypeName, final String rightTypeName) {
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
     * Indicates whether this is a generic type. Always returns {@code true} for PairType.
     *
     * @return {@code true}, as PairType is always a generic type
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
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Pair object to convert to string
     * @return a JSON string representation of the pair, or {@code null} if the input is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Pair<L, R> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x.left(), x.right()), Utils.jsc);
    }

    /**
     * Parses a string representation and creates a Pair object.
     * The string should be in JSON array format: [leftValue, rightValue].
     * Returns {@code null} if the input string is {@code null} or empty.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse, expected to be a JSON array with two elements
     * @return a Pair object created from the parsed values, or {@code null} if the input is {@code null} or empty
     * @throws IllegalArgumentException if the parsed array is {@code null} or has fewer than 2 elements
     * @see #valueOf(Object)
     * @see #stringOf(Pair)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Pair<L, R> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(str, Utils.jdc, Object[].class);

        if (a == null || a.length < 2) {
            throw new IllegalArgumentException("Invalid Pair format. Expected array with at least 2 elements but got: " + str);
        }

        final L left = (L) convertTupleElement(a[0], leftType);
        final R right = (R) convertTupleElement(a[1], rightType);

        return Pair.of(left, right);
    }

    /**
     * Appends the string representation of a Pair object to an Appendable.
     * The pair is formatted as [leftValue, rightValue] with appropriate element separation.
     * If the pair is {@code null}, appends "null". Handles Writer instances with buffering optimization.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the Pair object to append
     * @throws IOException if an I/O error occurs during writing
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
    public void appendTo(final Appendable appendable, final Pair<L, R> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR

                try {
                    bw.write(SK._BRACKET_L);

                    leftType.appendTo(bw, x.left());
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    rightType.appendTo(bw, x.right());

                    bw.write(SK._BRACKET_R);

                    if (!isBufferedWriter) {
                        bw.flush();
                    }
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            } else {
                appendable.append(SK._BRACKET_L);

                leftType.appendTo(appendable, x.left());
                appendable.append(ELEMENT_SEPARATOR);
                rightType.appendTo(appendable, x.right());

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
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Pair object to write
     * @param config the serialization configuration to use
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Pair<L, R> x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(SK._BRACKET_L);

                leftType.serializeTo(writer, x.left(), config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                rightType.serializeTo(writer, x.right(), config);

                writer.write(SK._BRACKET_R);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Generates a type name for a Pair type with the specified left and right type names.
     *
     * @param leftTypeName the name of the left type
     * @param rightTypeName the name of the right type
     * @param isDeclaringName if {@code true}, uses simple class names; if {@code false}, uses canonical class names
     * @return the generated type name string
     */
    protected static String getTypeName(final String leftTypeName, final String rightTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Pair.class) + SK.LESS_THAN + TypeFactory.getType(leftTypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(rightTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Pair.class) + SK.LESS_THAN + TypeFactory.getType(leftTypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(rightTypeName).name() + SK.GREATER_THAN;
        }
    }
}
