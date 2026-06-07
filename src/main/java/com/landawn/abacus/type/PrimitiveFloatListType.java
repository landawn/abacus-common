/*
 * Copyright (C) 2015 HaiYang Li
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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link FloatList} objects, which is a primitive-backed list of {@code float} values.
 * Provides serialization and deserialization of {@link FloatList} instances by delegating to the
 * underlying {@code float[]} array type handler.
 * String representations use the format {@code [1.5, 2.7, 3.14]} with comma-separated elements
 * enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveFloatListType extends AbstractPrimitiveListType<FloatList> {

    /** The type name constant for {@link FloatList} type identification, equal to {@code "FloatList"}. */
    public static final String FLOAT_LIST = FloatList.class.getSimpleName();

    private final Type<float[]> arrayType = Type.of(float[].class);

    private final Type<Float> elementType = Type.of(float.class);
    private final List<Type<?>> parameterTypes = List.of(elementType);

    /**
     * Constructs a new PrimitiveFloatListType instance.
     * This constructor is protected to allow subclassing while maintaining controlled instantiation
     * through the TypeFactory.
     */
    protected PrimitiveFloatListType() {
        super(FLOAT_LIST);
    }

    /**
     * Returns the Class object representing the FloatList type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * Class<FloatList> clazz = type.javaType();
     * // Returns: FloatList.class
     * }</pre>
     *
     * @return the Class object for FloatList
     */
    @Override
    public Class<FloatList> javaType() {
        return FloatList.class;
    }

    /**
     * Returns the Type instance for the element type of this list, which is primitive {@code float}.
     * This method provides access to the Type representation of individual list elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * Type<Float> elementType = type.elementType();
     * // Returns: Type instance for float
     * }</pre>
     *
     * @return the Type instance representing the {@code float} element type
     */
    @Override
    public Type<Float> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this list type.
     *
     * @return an immutable list containing the Float Type that describes the elements of this list type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a FloatList to its string representation.
     * The list is first converted to a float array, then serialized using the array type handler.
     * Returns {@code null} if the input list is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * FloatList list = FloatList.of(1.5f, 2.7f, 3.14f);
     * String result = type.stringOf(list);
     * // Returns: "[1.5, 2.7, 3.14]"
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the FloatList to convert
     * @return the string representation of the list, or {@code null} if input is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final FloatList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a FloatList.
     * The string is first parsed as a float array, then wrapped in a FloatList.
     * Returns {@code null} if the input string is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * FloatList result = type.valueOf("[1.5, 2.7, 3.14]");
     * // Returns: FloatList containing {1.5f, 2.7f, 3.14f}
     *
     * FloatList empty = type.valueOf("[]");
     * // Returns: empty FloatList
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return a FloatList created from the parsed values, or {@code null} if input is {@code null} or empty
     * @see #valueOf(Object)
     * @see #stringOf(FloatList)
     */
    @Override
    public FloatList valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final float[] array = arrayType.valueOf(str);
        return array == null ? null : FloatList.of(array);
    }

    /**
     * Appends the string representation of a FloatList to an Appendable.
     * Delegates to the array type handler after converting the list to an array.
     * Appends "null" if the list is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * StringBuilder sb = new StringBuilder();
     * FloatList list = FloatList.of(1.5f, 2.7f, 3.14f);
     * type.appendTo(sb, list);
     * // sb now contains: "[1.5, 2.7, 3.14]"
     * }</pre>
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the FloatList to append
     * @throws IOException if an I/O error occurs
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
    public void appendTo(final Appendable appendable, final FloatList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the character representation of a FloatList to a CharacterWriter.
     * Delegates to the array type handler after converting the list to an array.
     * Writes "null" if the list is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * CharacterWriter writer = new BufferedJsonWriter();
     * FloatList list = FloatList.of(1.5f, 2.7f, 3.14f);
     * type.serializeTo(writer, list, null);
     * // Writer contains: "[1.5, 2.7, 3.14]"
     * }</pre>
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
     * @param x the FloatList to write
     * @param config the serialization configuration (passed through to the array type writer)
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final FloatList x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.serializeTo(writer, x.toArray(), config);
        }
    }
}
