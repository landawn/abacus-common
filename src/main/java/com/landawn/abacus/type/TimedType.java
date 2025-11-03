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
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Timed;
import com.landawn.abacus.util.WD;

/**
 * Type handler for {@link Timed} objects. This class provides serialization and
 * deserialization support for Timed instances, which wrap a value with a timestamp.
 * The serialization format is a JSON array: [timestamp, value].
 *
 * @param <T> the type of value wrapped in the Timed object
 */
@SuppressWarnings("java:S2160")
public class TimedType<T> extends AbstractType<Timed<T>> { //NOSONAR

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<Timed<T>> typeClass = (Class) Timed.class; //NOSONAR

    private final Type<T> valueType;

    private final Type<?>[] parameterTypes;

    TimedType(final String valueTypeName) {
        super(getTypeName(valueTypeName, false));

        declaringName = getTypeName(valueTypeName, true);
        valueType = TypeFactory.getType(valueTypeName);
        parameterTypes = new Type[] { valueType };
    }

    /**
     * Returns the declaring name of this type, which uses simple class names.
     *
     * @return the declaring name of this Timed type
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Timed type.
     *
     * @return the Class object for Timed
     */
    @Override
    public Class<Timed<T>> clazz() {
        return typeClass;
    }

    /**
     * Returns the parameter types of this generic type.
     * For Timed, this is an array containing a single element: the value type.
     *
     * @return an array containing the value type
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
     * Converts a Timed object to its string representation.
     * The format is a JSON array: [timestamp, value].
     *
     * @param x the Timed object to convert
     * @return the JSON string representation, or {@code null} if x is null
     */
    @Override
    public String stringOf(final Timed<T> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x.timestamp(), x.value()), Utils.jsc);
    }

    /**
     * Creates a Timed object from its string representation.
     * Expects a JSON array format: [timestamp, value].
     *
     * @param str the string to parse
     * @return a Timed object containing the parsed timestamp and value, or {@code null} if str is empty
     */
    @SuppressWarnings("unchecked")
    @Override
    public Timed<T> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(str, Utils.jdc, Object[].class);

        final long timestamp = a[0] == null ? 0 : (a[0] instanceof Number ? ((Number) a[0]).longValue() : Numbers.toLong(a[0].toString()));
        final T value = a[1] == null ? null : ((T) (valueType.clazz().isAssignableFrom(a[1].getClass()) ? a[1] : N.convert(a[1], valueType)));

        return Timed.of(value, timestamp);
    }

    /**
     * Appends the string representation of a Timed object to the given Appendable.
     * Writes the format: [timestamp, value].
     *
     * @param appendable the Appendable to write to
     * @param x the Timed object to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Timed<T> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR

                try {
                    bw.write(WD._BRACKET_L);

                    bw.write(String.valueOf(x.timestamp()));
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    valueType.appendTo(bw, x.value());

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

                appendable.append(String.valueOf(x.timestamp()));
                appendable.append(ELEMENT_SEPARATOR);
                valueType.appendTo(appendable, x.value());

                appendable.append(WD._BRACKET_R);
            }
        }
    }

    /**
     * Writes the character representation of a Timed object to the given CharacterWriter.
     * This method is used for JSON/XML serialization. Writes the format: [timestamp, value].
     *
     * @param writer the CharacterWriter to write to
     * @param x the Timed object to write
     * @param config the serialization configuration for formatting options
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Timed<T> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                writer.write(String.valueOf(x.timestamp()));
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                valueType.writeCharacter(writer, x.value(), config);

                writer.write(WD._BRACKET_R);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Generates the type name for a Timed type with the specified value type.
     *
     * @param valueTypeName the name of the value type
     * @param isDeclaringName if {@code true}, uses simple class names; if {@code false}, uses canonical class names
     * @return the generated type name for Timed with the specified value type
     */
    protected static String getTypeName(final String valueTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Timed.class) + WD.LESS_THAN + TypeFactory.getType(valueTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Timed.class) + WD.LESS_THAN + TypeFactory.getType(valueTypeName).name() + WD.GREATER_THAN;
        }
    }
}
