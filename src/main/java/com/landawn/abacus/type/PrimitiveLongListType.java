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
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link LongList} objects, which is a primitive-backed list of {@code long} values.
 * Uses the matching primitive-array format for serialization and deserialization, while honoring
 * the {@code long[]} type handler resolved when this instance is constructed.
 * With the built-in handler, string representations use the format {@code [1, 2, 3]} with comma-separated elements
 * enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveLongListType extends AbstractPrimitiveListType<LongList> {

    /** The type name constant for {@link LongList} type identification, equal to {@code "LongList"}. */
    public static final String LONG_LIST = LongList.class.getSimpleName();

    private final Type<long[]> arrayType = Type.of(long[].class);
    private final Type<Long> elementType = Type.of(long.class);
    private final List<Type<?>> parameterTypes = List.of(elementType);

    /**
     * Constructs a new PrimitiveLongListType instance.
     * This constructor is protected to keep instantiation controlled by the TypeFactory.
     */
    protected PrimitiveLongListType() {
        super(LONG_LIST);
    }

    /**
     * Returns the Class object representing the LongList type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * Class<LongList> clazz = type.javaType();
     * System.out.println(clazz.getName());   // Output: com.landawn.abacus.util.LongList
     * }</pre>
     *
     * @return the Class object for LongList.class
     */
    @Override
    public Class<LongList> javaType() {
        return LongList.class;
    }

    /**
     * Returns the cached type handler for {@code long[]} values.
     *
     * <p>The handler is resolved through {@link Type#of(Class)} when this list type is constructed.
     * It is used for parsing and for serialization that delegates to an array handler, including a custom
     * handler registered before construction. Repeated calls return the same handler.</p>
     *
     * @return the cached {@code long[]} type handler; never {@code null}
     * @see #valueOf(String)
     */
    @Override
    public Type<long[]> arrayType() {
        return arrayType;
    }

    /**
     * Returns the Type instance for the element type of this list, which is primitive long.
     * This method provides access to the Type representation of individual list elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * Type<?> elementType = type.elementType();
     * System.out.println(elementType.name());   // Output: long
     * }</pre>
     *
     * @return the Type instance representing long type for list elements
     */
    @Override
    public Type<Long> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this list type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * List<Type<?>> paramTypes = type.parameterTypes();
     * System.out.println(paramTypes.size());         // Output: 1
     * System.out.println(paramTypes.get(0).name());  // Output: long
     * }</pre>
     *
     * @return an immutable list containing the primitive {@code long} Type that describes the elements of this list type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts the logical elements of a {@link LongList} to their array-type string representation.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets,
     * for example {@code [1, 2, 3]}. An empty list produces {@code []}; a {@code null} list returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * LongList list = LongList.of(1L, 2L, 3L);
     * String str = type.stringOf(list);
     * System.out.println(str);   // Output: [1, 2, 3]
     *
     * String nullStr = type.stringOf(null);
     * System.out.println(nullStr);   // Output: null
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
    public String stringOf(final LongList x) {
        if (x == null) {
            return null;
        }

        return arrayType instanceof PrimitiveLongArrayType ? stringOf(x.internalArray(), x.size()) : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a LongList.
     * The string should contain comma-separated long values enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * LongList list = type.valueOf("[1, 2, 3]");
     * System.out.println(list.size());  // Output: 3
     * System.out.println(list.get(0));  // Output: 1
     *
     * LongList emptyList = type.valueOf("[]");
     * System.out.println(emptyList.isEmpty());   // Output: true
     *
     * LongList nullList = type.valueOf(null);
     * System.out.println(nullList);   // Output: null
     * }</pre>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * <p>An element may also be enclosed in a matching pair of double or single quotes (for example
     * {@code ["1", "2"]}), which is the form {@link #serializeTo(CharacterWriter, LongList, JsonXmlSerConfig)} writes
     * when {@link JsonXmlSerConfig#isWriteLongAsString()} is set; the quotes are removed before the element is parsed.</p>
     *
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed LongList, or {@code null} if the input string is {@code null}, empty or blank
     * @throws IllegalArgumentException if an unquoted element is empty or whitespace-only.
     * @throws NumberFormatException if an element is not a valid integer literal
     * @throws ArithmeticException if an element is out of the {@code long} range (e.g. {@code 9223372036854775808})
     * @see #valueOf(Object)
     * @see #stringOf(LongList)
     */
    @MayReturnNull
    @Override
    public LongList valueOf(final String str) throws IllegalArgumentException, NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final long[] array = arrayType.valueOf(str);
        return array == null ? null : LongList.of(array);
    }

    /**
     * Appends the logical elements of a {@link LongList} to the destination using the array-type text format.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets.
     * An empty list produces {@code []}; a {@code null} list produces the literal {@code "null"}.
     * The built-in format matches {@link #stringOf(LongList)} for non-null lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * LongList list = LongList.of(10L, 20L, 30L);
     * StringBuilder sb = new StringBuilder("Values: ");
     * type.appendTo(sb, list);
     * System.out.println(sb.toString());   // Output: Values: [10, 20, 30]
     *
     * StringBuilder nullSb = new StringBuilder();
     * type.appendTo(nullSb, null);
     * System.out.println(nullSb.toString());   // Output: null
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
     * @see #serializeTo(CharacterWriter, LongList, JsonXmlSerConfig)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void appendTo(final Appendable appendable, final LongList x) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else if (arrayType instanceof PrimitiveLongArrayType && canWriteDirectly(appendable)) {
            appendTo(appendable, x.internalArray(), x.size());
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the logical elements of a {@link LongList} using primitive-array serialization.
     * With the built-in handler, decimal values are separated by a comma and a space and enclosed in square brackets.
     * Each value is quoted only when the configuration enables {@link JsonXmlSerConfig#isWriteLongAsString()}
     * and specifies a nonzero string quotation character. A {@code null} configuration writes unquoted values.
     * The built-in handler writes {@code []} for an empty list; a {@code null} list produces the literal {@code "null"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * LongList list = LongList.of(100L, 200L, 300L);
     * BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
     * BufferedJsonWriter nullWriter = Objectory.createBufferedJsonWriter();
     * JsonXmlSerConfig<?> config = null;
     * try {
     *     type.serializeTo(writer, list, config);
     *     System.out.println(writer.toString());   // Output: [100, 200, 300]
     *
     *     type.serializeTo(nullWriter, null, config);
     *     System.out.println(nullWriter.toString());   // Output: null
     * } finally {
     *     Objectory.recycle(writer);
     *     Objectory.recycle(nullWriter);
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
     * @see #appendTo(Appendable, LongList)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void serializeTo(final CharacterWriter writer, final LongList x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else if (arrayType instanceof PrimitiveLongArrayType && canWriteDirectly(writer, config)) {
            serializeTo(writer, x.internalArray(), x.size(), config);
        } else {
            arrayType.serializeTo(writer, x.toArray(), config);
        }
    }
}
