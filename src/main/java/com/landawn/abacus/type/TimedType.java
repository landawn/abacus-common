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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Timed;

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

    private final List<Type<?>> parameterTypes;

    /**
     * Constructs a TimedType instance with the specified value type.
     * This constructor is package-private and should only be called by TypeFactory.
     *
     * @param valueTypeName the name of the type wrapped in the Timed object
     */
    TimedType(final String valueTypeName) {
        super(getTypeName(valueTypeName, false));

        declaringName = getTypeName(valueTypeName, true);
        valueType = TypeFactory.getType(valueTypeName);
        parameterTypes = List.of(valueType);
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
    public Class<Timed<T>> javaType() {
        return typeClass;
    }

    /**
     * Returns the parameter types of this generic type.
     * For Timed, this is an immutable list containing a single element: the value type.
     *
     * @return an immutable list containing the value type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a parameterized type.
     * {@code TimedType} is always parameterized as it carries a value type parameter.
     *
     * @return {@code true}, indicating this is a parameterized type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Converts a Timed object to its string representation.
     * The format is a JSON array: [timestamp, value].
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Timed object to convert
     * @return the JSON string representation, or {@code null} if x is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Timed<T> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x.timestamp(), x.value()), Utils.jsc);
    }

    /**
     * Creates a Timed object from its string representation.
     * Expects a JSON array format: [timestamp, value].
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return a Timed object containing the parsed timestamp and value, or {@code null} if {@code str} is {@code null} or empty
     * @throws IllegalArgumentException if the parsed value is not an array containing exactly two elements
     * @see #valueOf(Object)
     * @see #stringOf(Timed)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Timed<T> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(str, Utils.jdc, Object[].class);

        if (a == null || a.length != 2) {
            throw new IllegalArgumentException("Invalid Timed format. Expected an array with exactly 2 elements [timestamp, value] but got: " + str);
        }

        final long timestamp = a[0] == null ? 0 : (a[0] instanceof Number ? ((Number) a[0]).longValue() : Numbers.toLong(a[0].toString()));
        final T value = (T) convertTupleElement(a[1], valueType);

        return Timed.of(value, timestamp);
    }

    /**
     * Appends the string representation of a Timed object to the given Appendable.
     * Writes the format: [timestamp, value].
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the Timed object to append
     * @throws IOException if an I/O error occurs during the append operation
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
    public void appendTo(final Appendable appendable, final Timed<T> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR
                Throwable failure = null;

                try {
                    bw.write(SK._BRACKET_L);

                    bw.write(String.valueOf(x.timestamp()));
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    valueType.appendTo(bw, x.value());

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

                appendable.append(String.valueOf(x.timestamp()));
                appendable.append(ELEMENT_SEPARATOR);
                valueType.appendTo(appendable, x.value());

                appendable.append(SK._BRACKET_R);
            }
        }
    }

    /**
     * Writes the character representation of a Timed object to the given CharacterWriter.
     * This method is used for JSON/XML serialization. Writes the format: [timestamp, value].
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
     * @param x the Timed object to write
     * @param config the serialization configuration for formatting options
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Timed<T> x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            writer.write(String.valueOf(x.timestamp()));
            writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
            valueType.serializeTo(writer, x.value(), config);

            writer.write(SK._BRACKET_R);
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
            return ClassUtil.getSimpleClassName(Timed.class) + SK.LESS_THAN + TypeFactory.getType(valueTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Timed.class) + SK.LESS_THAN + TypeFactory.getType(valueTypeName).name() + SK.GREATER_THAN;
        }
    }
}
