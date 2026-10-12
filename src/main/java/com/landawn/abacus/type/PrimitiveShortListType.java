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
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link ShortList} objects, which is a primitive-backed list of {@code short} values.
 * Uses the matching primitive-array format for serialization and deserialization, while honoring
 * the {@code short[]} type handler resolved when this instance is constructed.
 * With the built-in handler, string representations use the format {@code [1, 2, 3]} with comma-separated elements
 * enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveShortListType extends AbstractPrimitiveListType<ShortList> {

    /** The type name constant for {@link ShortList} type identification, equal to {@code "ShortList"}. */
    public static final String SHORT_LIST = ShortList.class.getSimpleName();

    private final Type<short[]> arrayType = Type.of(short[].class);
    private final Type<Short> elementType = Type.of(short.class);
    private final List<Type<?>> parameterTypes = List.of(elementType);

    /**
     * Constructs a new PrimitiveShortListType instance.
     * This constructor is protected to keep instantiation controlled by the TypeFactory.
     */
    protected PrimitiveShortListType() {
        super(SHORT_LIST);
    }

    /**
     * Returns the Class object representing the ShortList type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * Class<ShortList> clazz = type.javaType();
     * System.out.println(clazz.getName());   // Output: com.landawn.abacus.util.ShortList
     * }</pre>
     *
     * @return the Class object for ShortList.class
     */
    @Override
    public Class<ShortList> javaType() {
        return ShortList.class;
    }

    /**
     * Returns the cached type handler for {@code short[]} values.
     *
     * <p>The handler is resolved through {@link Type#of(Class)} when this list type is constructed.
     * It is used for parsing and for serialization that delegates to an array handler, including a custom
     * handler registered before construction. Repeated calls return the same handler.</p>
     *
     * @return the cached {@code short[]} type handler; never {@code null}
     * @see #valueOf(String)
     */
    @Override
    public Type<short[]> arrayType() {
        return arrayType;
    }

    /**
     * Returns the Type instance for the element type of this list, which is primitive short.
     * This method provides access to the Type representation of individual list elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * Type<?> elementType = type.elementType();
     * System.out.println(elementType.name());   // Output: short
     * }</pre>
     *
     * @return the Type instance representing short type for list elements
     */
    @Override
    public Type<Short> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this list type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * List<Type<?>> paramTypes = type.parameterTypes();
     * System.out.println(paramTypes.size());         // Output: 1
     * System.out.println(paramTypes.get(0).name());  // Output: short
     * }</pre>
     *
     * @return an immutable list containing the primitive {@code short} Type that describes the elements of this list type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts the logical elements of a {@link ShortList} to their array-type string representation.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets,
     * for example {@code [1, 2, 3]}. An empty list produces {@code []}; a {@code null} list returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
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
    public String stringOf(final ShortList x) {
        if (x == null) {
            return null;
        }

        return arrayType instanceof PrimitiveShortArrayType ? stringOf(x.internalArray(), x.size()) : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a ShortList.
     * The string should contain comma-separated short values enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * ShortList list = type.valueOf("[1, 2, 3]");
     * System.out.println(list.size());  // Output: 3
     * System.out.println(list.get(0));  // Output: 1
     *
     * ShortList emptyList = type.valueOf("[]");
     * System.out.println(emptyList.isEmpty());   // Output: true
     *
     * ShortList nullList = type.valueOf(null);
     * System.out.println(nullList);   // Output: null
     * }</pre>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed ShortList, or {@code null} if the input string is {@code null}, empty or blank
     * @throws IllegalArgumentException if an unquoted element is empty or whitespace-only.
     * @throws NumberFormatException if an element is not a valid integer literal
     * @throws ArithmeticException if an element is out of the {@code short} range (e.g. {@code 32768})
     * @see #valueOf(Object)
     * @see #stringOf(ShortList)
     */
    @MayReturnNull
    @Override
    public ShortList valueOf(final String str) throws IllegalArgumentException, NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final short[] array = arrayType.valueOf(str);
        return array == null ? null : ShortList.of(array);
    }

    /**
     * Appends the logical elements of a {@link ShortList} to the destination using the array-type text format.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets.
     * An empty list produces {@code []}; a {@code null} list produces the literal {@code "null"}.
     * The built-in format matches {@link #stringOf(ShortList)} for non-null lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * ShortList list = ShortList.of((short) 10, (short) 20, (short) 30);
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
     * @see #serializeTo(CharacterWriter, ShortList, JsonXmlSerConfig)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void appendTo(final Appendable appendable, final ShortList x) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else if (arrayType instanceof PrimitiveShortArrayType && canWriteDirectly(appendable)) {
            appendTo(appendable, x.internalArray(), x.size());
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the logical elements of a {@link ShortList} using primitive-array serialization.
     * With the built-in handler, elements are written as unquoted literals separated by a comma and a space
     * and enclosed in square brackets. The configuration does not change their literal representation.
     * The built-in handler writes {@code []} for an empty list; a {@code null} list produces the literal {@code "null"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * ShortList list = ShortList.of((short) 100, (short) 200, (short) 300);
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
     * @see #appendTo(Appendable, ShortList)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void serializeTo(final CharacterWriter writer, final ShortList x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else if (arrayType instanceof PrimitiveShortArrayType && canWriteDirectly(writer, config)) {
            serializeTo(writer, x.internalArray(), x.size());
        } else {
            arrayType.serializeTo(writer, x.toArray(), config);
        }
    }
}
