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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for Indexed objects.
 * Indexed represents a value paired with an index, useful for maintaining position information
 * during stream operations or when processing collections. This class provides serialization
 * and deserialization capabilities for Indexed instances.
 *
 * @param <T> the type of value stored in the Indexed container
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
     */
    IndexedType(final String valueTypeName) {
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
     * @param x the {@link Indexed} object to serialize; may be {@code null}
     * @return the JSON array string, or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final Indexed<T> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x.index(), x.value()), Utils.jsc);
    }

    /**
     * Deserializes a JSON array string into an {@link Indexed} instance.
     * The string must be a JSON array of at least two elements: {@code [index, value]},
     * where the first element is converted to a {@code long} index.
     *
     * @param str the JSON array string to parse (e.g., {@code "[5,\"hello\"]"}); may be {@code null} or empty
     * @return a new {@link Indexed} instance with the parsed index and value,
     *         or {@code null} if {@code str} is {@code null} or empty
     * @throws IllegalArgumentException if the array has fewer than two elements
     */
    @SuppressWarnings("unchecked")
    @Override
    public Indexed<T> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(str, Utils.jdc, Object[].class);

        if (a == null || a.length < 2) {
            throw new IllegalArgumentException("Invalid Indexed format. Expected array with at least 2 elements [index, value] but got: " + str);
        }

        final long index = a[0] == null ? 0 : (a[0] instanceof Number ? ((Number) a[0]).longValue() : Numbers.toLong(a[0].toString()));
        final T value = a[1] == null ? null : ((T) (valueType.javaType().isAssignableFrom(a[1].getClass()) ? a[1] : N.convert(a[1], valueType)));

        return Indexed.of(value, index);
    }

    /**
     * Appends the JSON array representation of an {@link Indexed} object to an {@link Appendable}
     * in the format {@code [index, value]}.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x the {@link Indexed} object to append; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final Indexed<T> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            appendable.append(N.stringOf(x.longIndex()));
            appendable.append(ELEMENT_SEPARATOR);
            valueType.appendTo(appendable, x.value());

            appendable.append(SK._BRACKET_R);
        }
    }

    /**
     * Writes the JSON array representation of an {@link Indexed} object to a {@link CharacterWriter}
     * in the format {@code [index, value]}.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x the {@link Indexed} object to write; may be {@code null}
     * @param config the serialization configuration to use; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Indexed<T> x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            writer.write(N.stringOf(x.longIndex()));
            writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
            valueType.writeCharacter(writer, x.value(), config);

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
     */
    protected static String getTypeName(final String valueTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Indexed.class) + SK.LESS_THAN + TypeFactory.getType(valueTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Indexed.class) + SK.LESS_THAN + TypeFactory.getType(valueTypeName).name() + SK.GREATER_THAN;
        }
    }
}
