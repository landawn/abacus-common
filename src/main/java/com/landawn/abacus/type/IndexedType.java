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

import java.io.IOException;
import java.util.List;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link Indexed} objects.
 * An {@code Indexed} value pairs a payload with an index, useful for maintaining position information
 * during stream operations or when processing collections. This class provides serialization
 * and deserialization for {@code Indexed} instances.
 *
 * @param <T> the type of value stored in the {@code Indexed} container
 * @see Indexed
 */
@SuppressWarnings("java:S2160")
public class IndexedType<T> extends AbstractType<Indexed<T>> {

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<Indexed<T>> typeClass = (Class) Indexed.class; //NOSONAR

    private final Type<T> valueType;

    private final List<Type<?>> parameterTypes;

    /**
     * Package-private constructor for IndexedType.
     * This constructor is called by the TypeFactory to create {@code Indexed<T>} type instances.
     *
     * @param valueTypeName the name of the type for values stored in the Indexed container
     * @throws IllegalArgumentException if a supplied type name is {@code null}, blank, or structurally invalid.
     */
    IndexedType(final String valueTypeName) throws IllegalArgumentException {
        super(getTypeName(valueTypeName, false));

        declaringName = getTypeName(valueTypeName, true);
        valueType = TypeFactory.getType(valueTypeName);
        parameterTypes = List.of(valueType);
    }

    /**
     * Returns the declaring name of this indexed type.
     * The declaring name represents the type in a simplified format suitable for type declarations,
     * using simple class names rather than fully qualified names.
     *
     * @return the declaring name of this type (e.g., "Indexed&lt;String&gt;")
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Indexed type handled by this type handler.
     *
     * @return the Class object for Indexed
     */
    @Override
    public Class<Indexed<T>> javaType() {
        return typeClass;
    }

    /**
     * Returns an immutable list containing the parameter types of this generic indexed type.
     * For indexed types, this list contains a single element representing the value type.
     *
     * @return an immutable list containing the value type as the only parameter type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type with type parameters.
     * Indexed types are always parameterized with the value type.
     *
     * @return {@code true}, as Indexed is a generic type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Serializes an {@link Indexed} object to its JSON array representation ({@code [index, value]}).
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@link Indexed} object to serialize; may be {@code null}
     * @return the JSON array string, or {@code null} if {@code x} is {@code null}
     * @throws RuntimeException if a value or bean property cannot be serialized by its selected type handler.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Indexed<T> x) throws RuntimeException {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x.longIndex(), x.value()), Utils.jsc);
    }

    /**
     * Deserializes a JSON array string into an {@link Indexed} instance.
     * The string must be a JSON array of exactly two elements: {@code [index, value]},
     * where the first element is converted to a {@code long} index.
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * <p>Each slot is parsed directly from its JSON token using the declared type, preserving decimal
     * precision and scale, including nested generic values.</p>
     * <p>The first slot must use integer notation and fit in a {@code long} (or be null, interpreted
     * as zero); fractional, scientific-notation, and out-of-range metadata is rejected, including
     * when quoted. Quoted integer text is JSON-decoded before validation.</p>
     *
     * @param str the JSON array string to parse (e.g., {@code "[5,\"hello\"]"}); may be {@code null} or empty
     * @return the deserialized indexed value, or {@code null} if {@code str} is {@code null} or empty (a blank,
     *         non-empty string is not treated as empty and is rejected)
     * @throws IllegalArgumentException if the parsed value is not an array with exactly two elements (this includes a         blank string, unbalanced brackets and trailing text), or if the index is negative (which         {@link Indexed#of(Object, long)} rejects)
     * @throws ParsingException if the value token is not valid JSON for the declared value type
     * @throws NumberFormatException if the index slot is not an integer literal (fractional or scientific notation),         or a value token cannot be converted to the declared value type
     * @throws ArithmeticException if the index does not fit in a {@code long}
     * @see #valueOf(Object)
     * @see #stringOf(Indexed)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    @Override
    public Indexed<T> valueOf(final String str) throws IllegalArgumentException, ParsingException, NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        // Keep the decoded metadata text: ordinary numeric payload coercion may truncate a fraction.
        final Object[] a = Utils.parseTupleElements(str, name(), List.of(TypeFactory.getType(String.class), valueType));
        final Long parsedIndex = (Long) TypeFactory.getType(Long.class).valueOf((String) a[0]);
        final long index = parsedIndex == null ? 0 : parsedIndex;
        final T value = (T) a[1];

        return Indexed.of(value, index);
    }

    /**
     * Appends the {@code toString()}-style string representation of an {@link Indexed} object to an {@link Appendable}
     * in the format {@code [index, value]}.
     * <p>
     * The value is appended by its declared element type handler. When that declared type is {@code Object} the
     * handler of the value's runtime class is used instead, exactly as
     * {@link #serializeTo(CharacterWriter, Indexed, JsonXmlSerConfig)} does, so a map, collection or bean value keeps
     * the {@code toString()}-style form rather than falling back to {@code ObjectType}'s JSON {@code stringOf}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x the {@link Indexed} object to append; may be {@code null}
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
    public void appendTo(final Appendable appendable, final Indexed<T> x) throws NullPointerException, IOException, RuntimeException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            appendable.append(N.stringOf(x.longIndex()));
            appendable.append(ELEMENT_SEPARATOR);
            AbstractTupleType.appendElement(appendable, valueType, x.value());

            appendable.append(SK._BRACKET_R);
        }
    }

    /**
     * Writes the JSON array representation of an {@link Indexed} object to a {@link CharacterWriter}
     * in the format {@code [index, value]}.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * The index slot honours {@code config.isWriteLongAsString()} exactly as a {@code long} value does: when the flag is
     * set and the config has a non-zero {@code stringQuotation}, the index is wrapped in that quotation character
     * ({@code ["7", "v"]}) so indexes beyond 2<sup>53</sup> survive JavaScript consumers; {@link #valueOf(String)}
     * accepts the quoted form. The value slot is written by its declared type handler; when the declared value type is
     * {@code Object} the handler of the value's runtime class is used instead, and a value whose handler is not
     * {@linkplain Type#isSerializable() serializable} (bean, map, {@code List<Object>}) is written as embedded JSON
     * under a {@code JsonSerConfig} rather than as a quoted JSON string. A {@code null} value is written by its declared
     * handler, so that handler's null-substitution flags apply.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x the {@link Indexed} object to write; may be {@code null}
     * @param config the serialization configuration to use; may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Indexed<T> x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, RuntimeException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            final long index = x.longIndex();

            if (config != null && config.isWriteLongAsString() && config.getStringQuotation() != 0) {
                final char quotation = config.getStringQuotation();
                writer.write(quotation);
                writer.write(index);
                writer.write(quotation);
            } else {
                writer.write(index);
            }

            writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
            AbstractTupleType.serializeSlot(writer, valueType, x.value(), config);

            writer.write(SK._BRACKET_R);
        }
    }

    /**
     * Generates a type name string for an Indexed type with the specified value type.
     * The format depends on whether a declaring name (simplified) or full name is requested.
     *
     * @param valueTypeName the name of the value type
     * @param isDeclaringName {@code true} to generate a declaring name with simple class names, {@code false} for fully qualified names
     * @return the formatted type name (e.g., "Indexed&lt;String&gt;" or "com.landawn.abacus.util.Indexed&lt;java.lang.String&gt;")
     * @throws IllegalArgumentException if a supplied type name is {@code null}, blank, or structurally invalid.
     */
    protected static String getTypeName(final String valueTypeName, final boolean isDeclaringName) throws IllegalArgumentException {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Indexed.class) + SK.LESS_THAN + TypeFactory.getType(valueTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Indexed.class) + SK.LESS_THAN + TypeFactory.getType(valueTypeName).name() + SK.GREATER_THAN;
        }
    }
}
