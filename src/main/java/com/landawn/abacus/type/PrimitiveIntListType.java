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
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link IntList} objects, which is a primitive-backed list of {@code int} values.
 * Uses the matching primitive-array format for serialization and deserialization, while honoring
 * the {@code int[]} type handler resolved when this instance is constructed.
 * With the built-in handler, string representations use the format {@code [1, 2, 3]} with comma-separated elements
 * enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveIntListType extends AbstractPrimitiveListType<IntList> {

    /** The type name constant for {@link IntList} type identification, equal to {@code "IntList"}. */
    public static final String INT_LIST = IntList.class.getSimpleName();

    private final Type<int[]> arrayType = Type.of(int[].class);
    private final Type<Integer> elementType = Type.of(int.class);
    private final List<Type<?>> parameterTypes = List.of(elementType);

    /**
     * Constructs a new PrimitiveIntListType instance.
     * This constructor is protected to keep instantiation controlled by the TypeFactory.
     */
    protected PrimitiveIntListType() {
        super(INT_LIST);
    }

    /**
     * Returns the Class object representing the IntList type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * Class<IntList> clazz = type.javaType();
     * // clazz equals IntList.class
     * }</pre>
     *
     * @return the Class object for IntList.class
     */
    @Override
    public Class<IntList> javaType() {
        return IntList.class;
    }

    /**
     * Returns the cached type handler for {@code int[]} values.
     *
     * <p>The handler is resolved through {@link Type#of(Class)} when this list type is constructed.
     * It is used for parsing and for serialization that delegates to an array handler, including a custom
     * handler registered before construction. Repeated calls return the same handler.</p>
     *
     * @return the cached {@code int[]} type handler; never {@code null}
     * @see #valueOf(String)
     */
    @Override
    public Type<int[]> arrayType() {
        return arrayType;
    }

    /**
     * Returns the Type instance for the element type of this list, which is primitive int.
     * This method provides access to the Type representation of individual list elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * Type<?> elemType = type.elementType();
     * // elemType can be used for element-level operations
     * }</pre>
     *
     * @return the Type instance representing int type for list elements
     */
    @Override
    public Type<Integer> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this list type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * List<Type<?>> paramTypes = type.parameterTypes();
     * // paramTypes.get(0) represents the element type
     * }</pre>
     *
     * @return an immutable list containing the primitive {@code int} Type that describes the elements of this list type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts the logical elements of a {@link IntList} to their array-type string representation.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets,
     * for example {@code [1, 2, 3]}. An empty list produces {@code []}; a {@code null} list returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * IntList list = IntList.of(1, 2, 3);
     * String str = type.stringOf(list);
     * // str equals "[1, 2, 3]"
     *
     * String nullStr = type.stringOf(null);
     * // nullStr equals null
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
    public String stringOf(final IntList x) {
        if (x == null) {
            return null;
        }

        return arrayType instanceof PrimitiveIntArrayType ? stringOf(x.internalArray(), x.size()) : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates an IntList.
     * The string should contain comma-separated integer values enclosed in square brackets.
     * For example, "[1, 2, 3]" will be parsed to an IntList containing {1, 2, 3}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * IntList list = type.valueOf("[1, 2, 3]");
     * // list contains {1, 2, 3}
     *
     * IntList empty = type.valueOf("[]");
     * // empty.size() equals 0
     *
     * IntList nullList = type.valueOf(null);
     * // nullList equals null
     * }</pre>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed IntList, or {@code null} if the input string is {@code null}, empty or blank
     * @throws IllegalArgumentException if an unquoted element is empty or whitespace-only.
     * @throws NumberFormatException if an element is not a valid integer literal
     * @throws ArithmeticException if an element is out of the {@code int} range (e.g. {@code 2147483648})
     * @see #valueOf(Object)
     * @see #stringOf(IntList)
     */
    @MayReturnNull
    @Override
    public IntList valueOf(final String str) throws IllegalArgumentException, NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final int[] array = arrayType.valueOf(str);
        return array == null ? null : IntList.of(array);
    }

    /**
     * Appends the logical elements of a {@link IntList} to the destination using the array-type text format.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets.
     * An empty list produces {@code []}; a {@code null} list produces the literal {@code "null"}.
     * The built-in format matches {@link #stringOf(IntList)} for non-null lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * StringBuilder sb = new StringBuilder();
     * IntList list = IntList.of(10, 20, 30);
     * type.appendTo(sb, list);
     * // sb.toString() equals "[10, 20, 30]"
     *
     * StringBuilder sb2 = new StringBuilder();
     * type.appendTo(sb2, null);
     * // sb2.toString() equals "null"
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
     * @see #serializeTo(CharacterWriter, IntList, JsonXmlSerConfig)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void appendTo(final Appendable appendable, final IntList x) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else if (arrayType instanceof PrimitiveIntArrayType && canWriteDirectly(appendable)) {
            appendTo(appendable, x.internalArray(), x.size());
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the logical elements of a {@link IntList} using primitive-array serialization.
     * With the built-in handler, elements are written as unquoted literals separated by a comma and a space
     * and enclosed in square brackets. The configuration does not change their literal representation.
     * The built-in handler writes {@code []} for an empty list; a {@code null} list produces the literal {@code "null"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
     * BufferedJsonWriter nullWriter = Objectory.createBufferedJsonWriter();
     * try {
     *     IntList list = IntList.of(5, 10, 15);
     *     type.serializeTo(writer, list, null);
     *     // Writes: [5, 10, 15]
     *
     *     type.serializeTo(nullWriter, null, null);
     *     // Writes: null
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
     * @see #appendTo(Appendable, IntList)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void serializeTo(final CharacterWriter writer, final IntList x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else if (arrayType instanceof PrimitiveIntArrayType && canWriteDirectly(writer, config)) {
            serializeTo(writer, x.internalArray(), x.size());
        } else {
            arrayType.serializeTo(writer, x.toArray(), config);
        }
    }
}
