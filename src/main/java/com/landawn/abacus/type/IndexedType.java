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

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

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

    private final Type<?>[] parameterTypes;

    /**
     * Package-private constructor for IndexedType.
     * Creates a type handler for Indexed objects containing values of the specified type.
     * This constructor is called by the TypeFactory to create Indexed&lt;T&gt; type instances.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Obtained via TypeFactory
     * Type<Indexed<String>> type = TypeFactory.getType("Indexed<String>");
     * Indexed<String> indexed = Indexed.of("hello", 5);
     * String serialized = type.stringOf(indexed);  // "[5,\"hello\"]"
     * Indexed<String> deserialized = type.valueOf("[5,\"hello\"]");
     * }</pre>
     *
     * @param valueTypeName the name of the type for values stored in the Indexed container
     */
    IndexedType(final String valueTypeName) {
        super(getTypeName(valueTypeName, false));

        declaringName = getTypeName(valueTypeName, true);
        valueType = TypeFactory.getType(valueTypeName);
        parameterTypes = new Type[] { valueType };
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
    public Class<Indexed<T>> clazz() {
        return typeClass;
    }

    /**
     * Returns an array containing the parameter types of this generic indexed type.
     * For indexed types, this array contains a single element representing the value type.
     *
     * @return an array containing the value type as the only parameter type
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Converts an Indexed object to its string representation.
     * The indexed value is serialized as a JSON array with two elements: [index, value].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Indexed<String>> type = TypeFactory.getType("Indexed<String>");
     * Indexed<String> indexed = Indexed.of("hello", 5);
     * String result = type.stringOf(indexed);
     * // Returns: [5,"hello"]
     *
     * Type<Indexed<Integer>> intType = TypeFactory.getType("Indexed<Integer>");
     * Indexed<Integer> intIndexed = Indexed.of(42, 0);
     * result = intType.stringOf(intIndexed);
     * // Returns: [0,42]
     * }</pre>
     *
     * @param x the Indexed object to convert to string
     * @return the JSON array representation "[index,value]", or {@code null} if the input is null
     */
    @Override
    public String stringOf(final Indexed<T> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x.index(), x.value()), Utils.jsc);
    }

    /**
     * Parses a string representation into an Indexed instance.
     * The string should be in JSON array format with exactly two elements: [index, value].
     * The first element is converted to a long index, and the second element is parsed
     * according to the value type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Indexed<String>> type = TypeFactory.getType("Indexed<String>");
     * Indexed<String> indexed = type.valueOf("[5,\"hello\"]");
     * // indexed.index() returns 5, indexed.value() returns "hello"
     *
     * Type<Indexed<Integer>> intType = TypeFactory.getType("Indexed<Integer>");
     * Indexed<Integer> intIndexed = intType.valueOf("[0,42]");
     * // intIndexed.index() returns 0, intIndexed.value() returns 42
     * }</pre>
     *
     * @param str the JSON array string to parse (e.g., "[0,\"hello\"]")
     * @return a new Indexed instance with the parsed index and value, or {@code null} if the input is {@code null} or empty
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
        final T value = a[1] == null ? null : ((T) (valueType.clazz().isAssignableFrom(a[1].getClass()) ? a[1] : N.convert(a[1], valueType)));

        return Indexed.of(value, index);
    }

    /**
     * Appends the string representation of an Indexed object to an Appendable.
     * The output format is a JSON array: [index,value].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Indexed<String>> type = TypeFactory.getType("Indexed<String>");
     * StringBuilder sb = new StringBuilder();
     * Indexed<String> indexed = Indexed.of("hello", 5);
     * type.appendTo(sb, indexed);
     * // sb contains: [5,"hello"]
     * }</pre>
     *
     * @param appendable the Appendable to write to
     * @param x the Indexed object to append
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final Indexed<T> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(WD._BRACKET_L);

            appendable.append(N.stringOf(x.longIndex()));
            appendable.append(ELEMENT_SEPARATOR);
            valueType.appendTo(appendable, x.value());

            appendable.append(WD._BRACKET_R);
        }
    }

    /**
     * Writes the character representation of an Indexed object to a CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The indexed value is serialized as a JSON array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Indexed<String>> type = TypeFactory.getType("Indexed<String>");
     * CharacterWriter writer = new CharacterWriter();
     * JSONXMLSerializationConfig config = JSONXMLSerializationConfig.of();
     * Indexed<String> indexed = Indexed.of("hello", 5);
     * type.writeCharacter(writer, indexed, config);
     * String result = writer.toString();
     * // result: [5,"hello"]
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the Indexed object to write
     * @param config the serialization configuration to use
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Indexed<T> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                writer.write(N.stringOf(x.longIndex()));
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                valueType.writeCharacter(writer, x.value(), config);

                writer.write(WD._BRACKET_R);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
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
            return ClassUtil.getSimpleClassName(Indexed.class) + WD.LESS_THAN + TypeFactory.getType(valueTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Indexed.class) + WD.LESS_THAN + TypeFactory.getType(valueTypeName).name() + WD.GREATER_THAN;
        }
    }
}
