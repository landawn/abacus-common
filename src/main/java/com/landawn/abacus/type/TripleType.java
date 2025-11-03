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
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.WD;

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

    private final Type<?>[] parameterTypes;

    TripleType(final String leftTypeName, final String middleTypeName, final String rightTypeName) {
        super(getTypeName(leftTypeName, middleTypeName, rightTypeName, false));

        declaringName = getTypeName(leftTypeName, middleTypeName, rightTypeName, true);

        leftType = TypeFactory.getType(leftTypeName);
        middleType = TypeFactory.getType(middleTypeName);
        rightType = TypeFactory.getType(rightTypeName);
        parameterTypes = new Type[] { leftType, middleType, rightType };
    }

    /**
     * Returns the declaring name of this type, which uses simple class names.
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
    public Class<Triple<L, M, R>> clazz() {
        return typeClass;
    }

    /**
     * Returns the parameter types of this generic type.
     * For Triple, this is an array containing three elements: left type, middle type, and right type.
     *
     * @return an array containing the left, middle, and right types
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Converts a Triple object to its string representation.
     * The format is a JSON array: [left, middle, right].
     *
     * @param x the Triple object to convert
     * @return the JSON string representation, or {@code null} if x is null
     */
    @Override
    public String stringOf(final Triple<L, M, R> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x.left(), x.middle(), x.right()), Utils.jsc);
    }

    /**
     * Creates a Triple object from its string representation.
     * Expects a JSON array format: [left, middle, right].
     *
     * @param str the string to parse
     * @return a Triple object containing the parsed values, or {@code null} if str is empty
     */
    @SuppressWarnings("unchecked")
    @Override
    public Triple<L, M, R> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(str, Utils.jdc, Object[].class);

        final L left = a[0] == null ? null : ((L) (leftType.clazz().isAssignableFrom(a[0].getClass()) ? a[0] : N.convert(a[0], leftType)));
        final M middle = a[1] == null ? null : ((M) (middleType.clazz().isAssignableFrom(a[1].getClass()) ? a[1] : N.convert(a[1], middleType)));
        final R right = a[2] == null ? null : ((R) (rightType.clazz().isAssignableFrom(a[2].getClass()) ? a[2] : N.convert(a[2], rightType)));

        return Triple.of(left, middle, right);
    }

    /**
     * Appends the string representation of a Triple object to the given Appendable.
     * Writes the format: [left, middle, right].
     *
     * @param appendable the Appendable to write to
     * @param x the Triple object to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Triple<L, M, R> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR

                try {
                    bw.write(WD._BRACKET_L);

                    leftType.appendTo(bw, x.left());
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    middleType.appendTo(bw, x.middle());
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
                middleType.appendTo(appendable, x.middle());
                appendable.append(ELEMENT_SEPARATOR);
                rightType.appendTo(appendable, x.right());

                appendable.append(WD._BRACKET_R);
            }
        }
    }

    /**
     * Writes the character representation of a Triple object to the given CharacterWriter.
     * This method is used for JSON/XML serialization. Writes the format: [left, middle, right].
     *
     * @param writer the CharacterWriter to write to
     * @param x the Triple object to write
     * @param config the serialization configuration for formatting options
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Triple<L, M, R> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                leftType.writeCharacter(writer, x.left(), config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                middleType.writeCharacter(writer, x.middle(), config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                rightType.writeCharacter(writer, x.right(), config);

                writer.write(WD._BRACKET_R);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
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
     */
    protected static String getTypeName(final String leftTypeName, final String middleTypeName, final String rightTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Triple.class) + WD.LESS_THAN + TypeFactory.getType(leftTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(middleTypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(rightTypeName).declaringName()
                    + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Triple.class) + WD.LESS_THAN + TypeFactory.getType(leftTypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(middleTypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(rightTypeName).name() + WD.GREATER_THAN;
        }
    }
}
