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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link FloatList} objects, which is a primitive-backed list of {@code float} values.
 * Uses the matching primitive-array format for serialization and deserialization, while honoring
 * the {@code float[]} type handler resolved when this instance is constructed.
 * With the built-in handler, string representations use the format {@code [1.5, 2.7, 3.14]} with comma-separated elements
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
     * This constructor is protected to keep instantiation controlled by the TypeFactory.
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
     * Returns the cached type handler for {@code float[]} values.
     *
     * <p>The handler is resolved through {@link Type#of(Class)} when this list type is constructed.
     * It is used for parsing and for serialization that delegates to an array handler, including a custom
     * handler registered before construction. Repeated calls return the same handler.</p>
     *
     * @return the cached {@code float[]} type handler; never {@code null}
     * @see #valueOf(String)
     */
    @Override
    public Type<float[]> arrayType() {
        return arrayType;
    }

    /**
     * Returns the Type instance for the element type of this list, which is primitive {@code float}.
     * This method provides access to the Type representation of individual list elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * Type<?> elementType = type.elementType();
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
     * Converts the logical elements of a {@link FloatList} to their array-type string representation.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets,
     * for example {@code [1.1, 2.2, 3.3]}. An empty list produces {@code []}; a {@code null} list returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * FloatList list = FloatList.of(1.5f, 2.7f, 3.14f);
     * String result = type.stringOf(list);
     * // Returns: "[1.5, 2.7, 3.14]"
     * }</pre>
     *
     * <p>The representation is intended to be parsed by {@link #valueOf(String)}.</p>
     *
     * @param x the list to convert; may be {@code null}
     * @return the array-type string representation, or {@code null} if {@code x} is {@code null}
     * @implNote With the built-in array handler, this method formats the backing array directly up to {@code x.size()},
     * without copying or trimming it. A custom cached array handler instead receives a snapshot containing only the
     * logical elements. Unused backing-array capacity is never included.
     * @see #arrayType()
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @SuppressWarnings("deprecation")
    @Override
    public String stringOf(final FloatList x) {
        if (x == null) {
            return null;
        }

        return arrayType instanceof PrimitiveFloatArrayType ? stringOf(x.internalArray(), x.size()) : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a FloatList.
     * The string is first parsed as a float array, then wrapped in a FloatList.
     * Returns {@code null} if the input string is {@code null}, empty or blank.
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
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return a FloatList created from the parsed values, or {@code null} if input is {@code null}, empty or blank
     * @throws IllegalArgumentException if an unquoted element is empty or whitespace-only.
     * @throws NumberFormatException if any element in the string cannot be parsed as a float
     * @see #valueOf(Object)
     * @see #stringOf(FloatList)
     */
    @MayReturnNull
    @Override
    public FloatList valueOf(final String str) throws IllegalArgumentException, NumberFormatException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final float[] array = arrayType.valueOf(str);
        return array == null ? null : FloatList.of(array);
    }

    /**
     * Appends the logical elements of a {@link FloatList} to the destination using the array-type text format.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets.
     * An empty list produces {@code []}; a {@code null} list produces the literal {@code "null"}.
     * The built-in format matches {@link #stringOf(FloatList)} for non-null lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * StringBuilder sb = new StringBuilder();
     * FloatList list = FloatList.of(1.5f, 2.7f, 3.14f);
     * type.appendTo(sb, list);
     * // sb now contains: "[1.5, 2.7, 3.14]"
     * }</pre>
     *
     * <p>This method does not flush or close the destination.</p>
     *
     * @param appendable the destination to append to; must not be {@code null}
     * @param x the list to append; may be {@code null}
     * @throws NullPointerException if {@code appendable} is {@code null}
     * @throws IOException if writing to the destination fails
     * @implNote With the built-in array handler and a destination known not to invoke application callbacks,
     * this method reads the backing array directly up to {@code x.size()}. Otherwise, it delegates to the cached
     * array handler with a snapshot of the logical elements, taken before invoking destination or handler callbacks.
     * Neither path trims the list or includes unused backing-array capacity.
     * @see #arrayType()
     * @see #serializeTo(CharacterWriter, FloatList, JsonXmlSerConfig)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void appendTo(final Appendable appendable, final FloatList x) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else if (arrayType instanceof PrimitiveFloatArrayType && canWriteDirectly(appendable)) {
            appendTo(appendable, x.internalArray(), x.size());
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the logical elements of a {@link FloatList} using primitive-array serialization.
     * With the built-in handler, elements are written as unquoted literals separated by a comma and a space
     * and enclosed in square brackets. The configuration does not change their literal representation.
     * The built-in handler writes {@code []} for an empty list; a {@code null} list produces the literal {@code "null"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
     * try {
     *     FloatList list = FloatList.of(1.5f, 2.7f, 3.14f);
     *     type.serializeTo(writer, list, null);
     *     // Writer contains: "[1.5, 2.7, 3.14]"
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * <p>This method does not flush or close the writer.</p>
     *
     * @param writer the destination writer; must not be {@code null}
     * @param x the list to serialize; may be {@code null}
     * @param config the serialization configuration; may be {@code null} and is passed unchanged when delegating to the array handler
     * @throws NullPointerException if {@code writer} is {@code null}
     * @throws IOException if writing to the destination fails
     * @implNote With the built-in array handler, a writer known not to invoke application callbacks, and either
     * a {@code null} configuration or an unextended built-in JSON/XML configuration, this method reads the backing
     * array directly up to {@code x.size()}. Otherwise, it delegates to the cached array handler with a snapshot of
     * the logical elements, taken before invoking writer, configuration, or handler callbacks. Neither path trims
     * the list or includes unused backing-array capacity.
     * @see #arrayType()
     * @see #appendTo(Appendable, FloatList)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void serializeTo(final CharacterWriter writer, final FloatList x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else if (arrayType instanceof PrimitiveFloatArrayType && canWriteDirectly(writer, config)) {
            serializeTo(writer, x.internalArray(), x.size());
        } else {
            arrayType.serializeTo(writer, x.toArray(), config);
        }
    }
}
