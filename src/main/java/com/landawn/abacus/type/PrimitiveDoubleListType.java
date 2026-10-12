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
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link DoubleList} objects, which is a primitive-backed list of {@code double} values.
 * Uses the matching primitive-array format for serialization and deserialization, while honoring
 * the {@code double[]} type handler resolved when this instance is constructed.
 * With the built-in handler, string representations use the format {@code [1.5, 2.7, 3.14]} with comma-separated elements
 * enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveDoubleListType extends AbstractPrimitiveListType<DoubleList> {

    /** The type name constant for {@link DoubleList} type identification, equal to {@code "DoubleList"}. */
    public static final String DOUBLE_LIST = DoubleList.class.getSimpleName();

    private final Type<double[]> arrayType = Type.of(double[].class);
    private final Type<Double> elementType = Type.of(double.class);
    private final List<Type<?>> parameterTypes = List.of(elementType);

    /**
     * Constructs a new PrimitiveDoubleListType instance.
     * This constructor is protected to keep instantiation controlled by the TypeFactory.
     */
    protected PrimitiveDoubleListType() {
        super(DOUBLE_LIST);
    }

    /**
     * Returns the Class object representing the DoubleList type.
     *
     * @return the Class object for DoubleList
     */
    @Override
    public Class<DoubleList> javaType() {
        return DoubleList.class;
    }

    /**
     * Returns the cached type handler for {@code double[]} values.
     *
     * <p>The handler is resolved through {@link Type#of(Class)} when this list type is constructed.
     * It is used for parsing and for serialization that delegates to an array handler, including a custom
     * handler registered before construction. Repeated calls return the same handler.</p>
     *
     * @return the cached {@code double[]} type handler; never {@code null}
     * @see #valueOf(String)
     */
    @Override
    public Type<double[]> arrayType() {
        return arrayType;
    }

    /**
     * Returns the Type object for the double element type.
     *
     * @return the Type object representing Double/double elements
     */
    @Override
    public Type<Double> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this list type.
     *
     * @return an immutable list containing the Double Type that describes the elements of this list type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts the logical elements of a {@link DoubleList} to their array-type string representation.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets,
     * for example {@code [1.1, 2.2, 3.3]}. An empty list produces {@code []}; a {@code null} list returns {@code null}.
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
    public String stringOf(final DoubleList x) {
        if (x == null) {
            return null;
        }

        return arrayType instanceof PrimitiveDoubleArrayType ? stringOf(x.internalArray(), x.size()) : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a DoubleList.
     * The string is first parsed as a double array, then wrapped in a DoubleList.
     * Returns {@code null} if the input string is {@code null}, empty or blank.
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return a DoubleList created from the parsed values, or {@code null} if input is {@code null}, empty or blank
     * @throws IllegalArgumentException if an unquoted element is empty or whitespace-only.
     * @throws NumberFormatException if any element in the string cannot be parsed as a double
     * @see #valueOf(Object)
     * @see #stringOf(DoubleList)
     */
    @MayReturnNull
    @Override
    public DoubleList valueOf(final String str) throws IllegalArgumentException, NumberFormatException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final double[] array = arrayType.valueOf(str);
        return array == null ? null : DoubleList.of(array);
    }

    /**
     * Appends the logical elements of a {@link DoubleList} to the destination using the array-type text format.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets.
     * An empty list produces {@code []}; a {@code null} list produces the literal {@code "null"}.
     * The built-in format matches {@link #stringOf(DoubleList)} for non-null lists.
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
     * @see #serializeTo(CharacterWriter, DoubleList, JsonXmlSerConfig)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void appendTo(final Appendable appendable, final DoubleList x) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else if (arrayType instanceof PrimitiveDoubleArrayType && canWriteDirectly(appendable)) {
            appendTo(appendable, x.internalArray(), x.size());
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the logical elements of a {@link DoubleList} using primitive-array serialization.
     * With the built-in handler, elements are written as unquoted literals separated by a comma and a space
     * and enclosed in square brackets. The configuration does not change their literal representation.
     * The built-in handler writes {@code []} for an empty list; a {@code null} list produces the literal {@code "null"}.
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
     * @see #appendTo(Appendable, DoubleList)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void serializeTo(final CharacterWriter writer, final DoubleList x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else if (arrayType instanceof PrimitiveDoubleArrayType && canWriteDirectly(writer, config)) {
            serializeTo(writer, x.internalArray(), x.size());
        } else {
            arrayType.serializeTo(writer, x.toArray(), config);
        }
    }
}
