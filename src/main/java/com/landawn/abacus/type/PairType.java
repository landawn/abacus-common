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

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

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

    private final Type<?>[] parameterTypes;

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
        parameterTypes = new Type[] { leftType, rightType };
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
    public Class<Pair<L, R>> clazz() {
        return typeClass;
    }

    /**
     * Returns an array containing the Type objects for the left and right elements of the Pair.
     * The first element is the type of the left value, and the second element is the type of the right value.
     *
     * @return an array of Type objects representing the parameter types
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this is a generic type. Always returns {@code true} for PairType.
     *
     * @return {@code true}, as PairType is always a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Converts a Pair object to its string representation using JSON format.
     * The pair is serialized as a JSON array with two elements: [leftValue, rightValue].
     * Returns {@code null} if the input pair is {@code null}.
     *
     * @param x the Pair object to convert to string
     * @return a JSON string representation of the pair, or {@code null} if the input is null
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
     * @param str the string to parse, expected to be a JSON array with two elements
     * @return a Pair object created from the parsed values, or {@code null} if the input is {@code null} or empty
     */
    @SuppressWarnings("unchecked")
    @Override
    public Pair<L, R> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(str, Utils.jdc, Object[].class);

        final L left = a[0] == null ? null : ((L) (leftType.clazz().isAssignableFrom(a[0].getClass()) ? a[0] : N.convert(a[0], leftType)));
        final R right = a[1] == null ? null : ((R) (rightType.clazz().isAssignableFrom(a[1].getClass()) ? a[1] : N.convert(a[1], rightType)));

        return Pair.of(left, right);
    }

    /**
     * Appends the string representation of a Pair object to an Appendable.
     * The pair is formatted as [leftValue, rightValue] with appropriate element separation.
     * If the pair is {@code null}, appends "null". Handles Writer instances with buffering optimization.
     *
     * @param appendable the Appendable to write to
     * @param x the Pair object to append
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final Pair<L, R> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);   //NOSONAR

                try {
                    bw.write(WD._BRACKET_L);

                    leftType.appendTo(bw, x.left());
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    rightType.appendTo(bw, x.right());

                    bw.write(WD._BRACKET_R);

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
                appendable.append(WD._BRACKET_L);

                leftType.appendTo(appendable, x.left());
                appendable.append(ELEMENT_SEPARATOR);
                rightType.appendTo(appendable, x.right());

                appendable.append(WD._BRACKET_R);
            }
        }
    }

    /**
     * Writes the character representation of a Pair object to a CharacterWriter.
     * The pair is formatted as [leftValue, rightValue] using the provided serialization configuration.
     * If the pair is {@code null}, writes "null".
     *
     * @param writer the CharacterWriter to write to
     * @param x the Pair object to write
     * @param config the serialization configuration to use
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Pair<L, R> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                leftType.writeCharacter(writer, x.left(), config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                rightType.writeCharacter(writer, x.right(), config);

                writer.write(WD._BRACKET_R);

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
            return ClassUtil.getSimpleClassName(Pair.class) + WD.LESS_THAN + TypeFactory.getType(leftTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(rightTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Pair.class) + WD.LESS_THAN + TypeFactory.getType(leftTypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(rightTypeName).name() + WD.GREATER_THAN;
        }
    }
}
