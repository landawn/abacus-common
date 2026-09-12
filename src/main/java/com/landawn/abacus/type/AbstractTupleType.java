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
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple;

/**
 * The abstract base class shared by every {@code TupleNType} ({@code Tuple1Type} ... {@code Tuple9Type}).
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
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    protected AbstractTupleType(final String typeName, final String declaringName, final Class<T> typeClass, final List<Type<?>> parameterTypes)
            throws IllegalArgumentException {
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
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the tuple value to serialize, may be {@code null}
     * @return a JSON array string (e.g., {@code "[\"foo\", 42, true]"}), or {@code null} if {@code x} is {@code null}
     * @throws RuntimeException if a value or bean property cannot be serialized by its selected type handler.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final T x) throws RuntimeException {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toArray(), Utils.jsc);
    }

    /**
     * Deserializes a JSON array string to a typed tuple.
     * <p>
     * The string is expected to be a JSON array whose length is exactly the arity of this tuple type.
     * Each tuple element is parsed directly using the corresponding entry in {@link #parameterTypes()},
     * preserving decimal precision and scale even in nested generic values.
     * </p>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON array string to deserialize, may be {@code null} or empty
     * @return a new tuple instance, or {@code null} if {@code str} is {@code null} or empty (a blank, non-empty
     *         string is not treated as empty and is rejected)
     * @throws IllegalArgumentException if the parsed value is not an array whose length exactly matches the tuple         arity (this includes a blank string, unbalanced brackets and trailing text)
     * @throws ParsingException if an element token is not valid JSON for its declared element type
     * @throws NumberFormatException if a numeric element token cannot be converted to its declared element type         (for example {@code 1.0} into an {@code Integer} slot)
     * @throws ArithmeticException if a numeric element is outside the range accepted by its declared type.
     * @see #valueOf(Object)
     * @see #stringOf(Tuple)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    @Override
    public T valueOf(final String str) throws IllegalArgumentException, ParsingException, NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        return fromArray(Utils.parseTupleElements(str, name(), parameterTypes));
    }

    /**
     * Rebuilds a typed tuple from its deserialized element array. Implementations should
     * cast each element from {@code converted[i]} to the element's declared type and call the
     * matching {@code Tuple.of(...)} factory.
     *
     * @param converted the element array (already type-converted by {@link #valueOf(String)});
     *                  guaranteed to have exactly {@code parameterTypes().size()} elements
     * @return a new tuple instance of the appropriate arity
     */
    protected abstract T fromArray(Object[] converted);

    /**
     * Appends the {@code toString()}-style string representation of the tuple to the given {@code Appendable}.
     * Writes {@code "null"} if {@code x} is {@code null}; otherwise writes each element
     * separated by {@link #ELEMENT_SEPARATOR} and enclosed in {@code [...]}.
     * <p>
     * Each element is appended by its declared element type handler. When the declared element type is
     * {@code Object} the handler of the element's runtime class is used instead, exactly as
     * {@link #serializeTo(CharacterWriter, Tuple, JsonXmlSerConfig)} does, so a map, collection or bean element keeps
     * the {@code toString()}-style form rather than falling back to {@code ObjectType}'s JSON {@code stringOf}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the target to append to
     * @param x the tuple value to append, may be {@code null}
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
    public void appendTo(final Appendable appendable, final T x) throws NullPointerException, IOException, RuntimeException {
        if (x == null) {
            appendable.append(NULL_STRING);
            return;
        }

        final Object[] elements = x.toArray();

        if (appendable instanceof Writer writer) {
            final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
            final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR
            Throwable failure = null;

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

            for (int i = 0; i < elements.length; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }
                appendElement(appendable, parameterTypes.get(i), elements[i]);
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    /**
     * Appends one slot of a wrapper or tuple-like value ({@code Optional}, {@code Nullable}, {@code Pair},
     * {@code Triple}, {@code TupleN}, {@code Indexed}, {@code Timed}) to an {@code Appendable}, resolving the handler
     * the same way {@link #serializeSlot(CharacterWriter, Type, Object, JsonXmlSerConfig)} does:
     * <ul>
     *   <li>a {@code null} value is written by the declared handler, so its {@code null} rendering applies;</li>
     *   <li>a declared {@code Object} slot carries no type information - and {@code ObjectType} has no {@code appendTo}
     *       of its own, so it falls back to {@code stringOf}, i.e. the JSON form - so the handler of the value's
     *       runtime class is used instead. A map, collection or bean slot value therefore keeps the
     *       {@code toString()}-style form ({@code {k:1}}, not {@code {"k": 1}}), matching what the same value appends
     *       as when it is not wrapped.</li>
     * </ul>
     *
     * @param appendable the target to append to
     * @param declaredType the slot's declared element type handler
     * @param value the slot value, may be {@code null}
     * @throws NullPointerException if the destination or declared type is {@code null}.
     * @throws IOException if the selected type handler or destination writer reports a checked I/O failure.
     * @throws RuntimeException if a value is incompatible with its declared type or its selected type handler cannot serialize it.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void appendElement(final Appendable appendable, final Type declaredType, final Object value)
            throws NullPointerException, IOException, RuntimeException {
        final Type type = (value != null && declaredType.isObject()) ? TypeFactory.getType(value.getClass()) : declaredType;

        type.appendTo(appendable, value);
    }

    /**
     * Writes the JSON array representation of the tuple to the given {@code CharacterWriter}.
     * Writes {@code "null"} if {@code x} is {@code null}; otherwise writes each element
     * separated by {@link #ELEMENT_SEPARATOR} and enclosed in {@code [...]}, applying the
     * per-element serialization configuration where applicable.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * Each element is written by its declared element type handler. When the declared element type is {@code Object}
     * the handler of the element's runtime class is used instead, so a {@code Tuple2<Object, Object>} holding
     * {@code 1} and {@code "a"} is written as {@code [1, "a"]} rather than {@code ["1", "a"]}. An element whose
     * (declared or runtime) handler is not {@linkplain Type#isSerializable() serializable} - a bean, a map, a
     * {@code List<Object>} - is written as embedded JSON ({@code {"k": 1}}, not the quoted string
     * {@code "{\"k\": 1}"}) when {@code config} is a {@link JsonSerConfig}; under any other config its
     * {@code stringOf} text is written with the writer's character escaping. A {@code null} element is written by
     * its declared handler, so that handler's null-substitution flags ({@code writeNullNumberAsZero} and friends)
     * apply. The result matches {@link #stringOf(Tuple)} for every element shape.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the tuple value to write, may be {@code null}
     * @param config the serialization configuration, may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, RuntimeException {
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
            serializeSlot(writer, parameterTypes.get(i), elements[i], config);
        }

        writer.write(SK._BRACKET_R);
    }

    /**
     * Writes one slot of a wrapper or tuple-like value ({@code Optional}, {@code Nullable}, {@code Pair}, {@code Triple},
     * {@code TupleN}, {@code Indexed}, {@code Timed}) to a {@code CharacterWriter}, resolving the handler the same way
     * the JSON serializer does for a collection element or map value:
     * <ul>
     *   <li>a {@code null} value is written by the declared handler, so its null-substitution flags apply;</li>
     *   <li>a declared {@code Object} slot carries no type information, so the handler of the value's runtime class is used
     *       (a registered subtype declared as its base type keeps the declared handler);</li>
     *   <li>a serializable handler (scalars, tuples, optionals, registered single-value types, {@code List<Integer>} ...)
     *       writes the value itself;</li>
     *   <li>a structured handler that is not serializable (bean, map, {@code List<Object>}, {@code Object[]}) would
     *       otherwise emit a quoted JSON <i>string</i>, so the value is written as embedded JSON when {@code config} is a
     *       {@link JsonSerConfig}, and as escaped {@code stringOf} text under any other config. The embedded JSON is
     *       written straight to the writer only when that writer is a JSON writer; on an XML or CSV writer - which a
     *       {@code JsonSerConfig} can still reach, {@code Type.serializeTo} being public API - the same text goes
     *       through {@code writeCharacter} so it is escaped for the target format instead of landing there as raw
     *       JSON. The embedded JSON is always written compactly: {@code prettyFormat} is deliberately not propagated
     *       to it, because this helper is not told the caller's current indentation and a pretty embedded structure
     *       would restart at the left margin. An unregistered plain object (serialization type {@code UNKNOWN}) keeps
     *       the handler's own quoted {@code toString()} form.</li>
     * </ul>
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param declaredType the slot's declared element type handler
     * @param value the slot value, may be {@code null}
     * @param config the serialization configuration, may be {@code null}
     * @throws NullPointerException if the destination or declared type is {@code null}.
     * @throws IOException if the selected type handler or destination writer reports a checked I/O failure.
     * @throws RuntimeException if a value is incompatible with its declared type or its selected type handler cannot serialize it.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void serializeSlot(final CharacterWriter writer, final Type declaredType, final Object value, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, RuntimeException {
        if (value == null) {
            declaredType.serializeTo(writer, null, config);
            return;
        }

        // An Object slot has no usable declared handler (ObjectType quotes everything); dispatch on the runtime class
        // exactly as JsonParserImpl does for an Object-typed collection element or map value.
        final Type type = declaredType.isObject() ? TypeFactory.getType(value.getClass()) : declaredType;

        if (type.isSerializable() || type.serializationType() == SerializationType.UNKNOWN) {
            type.serializeTo(writer, value, config);
        } else if (config instanceof JsonSerConfig jsonConfig) {
            // Bean/map/List<Object>/Object[]: the handler's serializeTo would write a quoted JSON string; write the
            // structure itself, exactly as stringOf does. Pretty format is switched off for the embedded write: this
            // helper is not told the caller's current indentation, so a pretty embedded structure would restart at the
            // left margin and mis-align every one of its lines.
            final JsonSerConfig embeddedConfig = jsonConfig.isPrettyFormat() ? jsonConfig.copy().setPrettyFormat(false) : jsonConfig;

            if (writer instanceof BufferedJsonWriter) {
                Utils.jsonParser.serialize(value, embeddedConfig, writer);
            } else {
                // A JSON config can still arrive on an XML or CSV writer (Type.serializeTo is public API): the
                // parser would write raw JSON - unescaped ", < and & - into that format. Emit the text through
                // writeCharacter so it is escaped for the target format, as CollectionType and ObjectArrayType do.
                writer.writeCharacter(Utils.jsonParser.serialize(value, embeddedConfig));
            }
        } else {
            writer.writeCharacter(type.stringOf(value));
        }
    }
}
