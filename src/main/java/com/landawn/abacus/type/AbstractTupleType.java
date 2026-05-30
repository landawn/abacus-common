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
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple;

/**
 * Abstract base class shared by every {@code TupleNType} (Tuple1Type ... Tuple9Type).
 * <p>
 * Concrete subclasses provide the type name, the Java {@link Class} of the wrapped tuple,
 * the per-element {@link Type} list, and a {@link #fromArray(Object[])} factory that
 * rebuilds a typed tuple from its deserialized element array. All other behavior
 * (serialization, deserialization with per-element type conversion, and JSON {@code [...]}
 * formatting) is implemented once here.
 * </p>
 *
 * @param <T> the concrete {@link Tuple} subtype handled by this type
 */
@SuppressWarnings("java:S2160")
abstract class AbstractTupleType<T extends Tuple<T>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final List<Type<?>> parameterTypes;

    /**
     * Constructs an {@code AbstractTupleType} with the given canonical/declaring names,
     * tuple class, and the per-element {@link Type} list.
     *
     * @param typeName the canonical type name (e.g. {@code "com.landawn.abacus.util.Tuple$Tuple3<...>"})
     * @param declaringName the simple-name form (e.g. {@code "Tuple3<...>"})
     * @param typeClass the {@link Class} of the concrete {@code TupleN} the subclass handles
     * @param parameterTypes the per-element {@link Type} list; arity must match {@code typeClass}
     */
    protected AbstractTupleType(final String typeName, final String declaringName, final Class<T> typeClass, final List<Type<?>> parameterTypes) {
        super(typeName);
        this.declaringName = declaringName;
        this.typeClass = typeClass;
        this.parameterTypes = parameterTypes;
    }

    /**
     * Returns the simple-name declaring form of this tuple type.
     * For example, {@code "Tuple3<String, Integer, Boolean>"}.
     *
     * @return the declaring name of this tuple type
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Java {@link Class} of the concrete tuple subtype handled by this type.
     *
     * @return the tuple class (e.g., {@code Tuple.Tuple3.class})
     */
    @Override
    public Class<T> javaType() {
        return typeClass;
    }

    /**
     * Returns the list of per-element {@link Type} instances for this tuple type.
     * The size of the list equals the arity of the tuple.
     *
     * @return an immutable list of element types in declaration order
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Returns {@code true} because tuple types are always parameterized with element types.
     *
     * @return {@code true}
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Serializes the tuple to its JSON array string representation.
     * Each element is serialized using the registered JSON parser.
     *
     * @param x the tuple value to serialize, may be {@code null}
     * @return a JSON array string (e.g., {@code "[\"foo\",42,true]"}), or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toArray(), Utils.jsc);
    }

    /**
     * Deserializes a JSON array string to a typed tuple.
     * <p>
     * The string is expected to be a JSON array whose element count matches the arity of this
     * tuple type. Each element is type-converted using the corresponding entry in
     * {@link #parameterTypes()}.
     * </p>
     *
     * @param str the JSON array string to deserialize, may be {@code null} or empty
     * @return a new tuple instance, or {@code null} if {@code str} is {@code null} or empty
     * @throws IllegalArgumentException if the parsed array is {@code null} or has fewer elements than the tuple arity
     */
    @SuppressWarnings("unchecked")
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(str, Utils.jdc, Object[].class);

        final int arity = parameterTypes.size();

        if (a == null || a.length < arity) {
            throw new IllegalArgumentException(
                    "Invalid " + typeClass.getSimpleName() + " format. Expected array with at least " + arity + " element(s) but got: " + str);
        }

        final Object[] converted = new Object[arity];

        for (int i = 0; i < arity; i++) {
            final Type<?> elementType = parameterTypes.get(i);
            final Object raw = a[i];
            converted[i] = (raw == null) ? null : (elementType.javaType().isAssignableFrom(raw.getClass()) ? raw : N.convert(raw, elementType));
        }

        return fromArray(converted);
    }

    /**
     * Rebuild a typed tuple from its deserialized element array. Implementations should
     * cast each element from {@code converted[i]} to the element's declared type and call the
     * matching {@code Tuple.of(...)} factory.
     *
     * @param converted the element array (already type-converted by {@link #valueOf(String)});
     *                  guaranteed to have exactly {@code parameterTypes().size()} elements
     * @return a new tuple instance of the appropriate arity
     */
    protected abstract T fromArray(Object[] converted);

    /**
     * Appends the JSON array representation of the tuple to the given {@code Appendable}.
     * Writes {@code "null"} if {@code x} is {@code null}; otherwise writes each element
     * separated by {@link #ELEMENT_SEPARATOR} and enclosed in {@code [...]}.
     *
     * @param appendable the target to append to
     * @param x the tuple value to append, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final T x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
            return;
        }

        final Object[] elements = x.toArray();

        if (appendable instanceof Writer writer) {
            final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
            final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR

            try {
                bw.write(SK._BRACKET_L);

                for (int i = 0; i < elements.length; i++) {
                    if (i > 0) {
                        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }
                    appendElement(bw, parameterTypes.get(i), elements[i]);
                }

                bw.write(SK._BRACKET_R);

                if (!isBufferedWriter) {
                    bw.flush();
                }
            } finally {
                if (!isBufferedWriter) {
                    Objectory.recycle((BufferedWriter) bw);
                }
            }
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0; i < elements.length; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }
                appendElement(appendable, parameterTypes.get(i), elements[i]);
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void appendElement(final Appendable appendable, final Type elementType, final Object value) throws IOException {
        elementType.appendTo(appendable, value);
    }

    /**
     * Writes the JSON array representation of the tuple to the given {@code CharacterWriter}.
     * Writes {@code "null"} if {@code x} is {@code null}; otherwise writes each element
     * separated by {@link #ELEMENT_SEPARATOR} and enclosed in {@code [...]}, applying the
     * per-element serialization configuration where applicable.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the tuple value to write, may be {@code null}
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
            return;
        }

        final Object[] elements = x.toArray();

        writer.write(SK._BRACKET_L);

        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
            }
            writeElementCharacter(writer, parameterTypes.get(i), elements[i], config);
        }

        writer.write(SK._BRACKET_R);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void writeElementCharacter(final CharacterWriter writer, final Type elementType, final Object value, final JsonXmlSerConfig<?> config)
            throws IOException {
        elementType.writeCharacter(writer, value, config);
    }
}
