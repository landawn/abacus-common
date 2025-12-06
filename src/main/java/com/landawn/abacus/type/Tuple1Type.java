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

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.WD;

/**
 * Type handler for {@link Tuple1} objects. This class provides serialization and
 * deserialization support for Tuple1 instances, which contain a single value.
 * The serialization format is a JSON array: [value].
 *
 * @param <T1> the type of the single element in the tuple
 */
@SuppressWarnings("java:S2160")
public class Tuple1Type<T1> extends AbstractType<Tuple1<T1>> {

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<Tuple1<T1>> typeClass = (Class) Tuple1.class; //NOSONAR

    private final Type<T1> type1;

    private final Type<?>[] parameterTypes;

    /**
     * Constructs a Tuple1Type instance with the specified element type.
     * This constructor is package-private and should only be called by TypeFactory.
     *
     * @param t1TypeName the name of the element type
     */
    Tuple1Type(final String t1TypeName) {
        super(getTypeName(t1TypeName, false));

        declaringName = getTypeName(t1TypeName, true);

        type1 = TypeFactory.getType(t1TypeName);
        parameterTypes = new Type[] { type1 };
    }

    /**
     * Returns the declaring name of this type, which uses simple class names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Tuple1<String>> type = TypeFactory.getType("Tuple1<String>");
     * String name = type.declaringName();   // Returns "Tuple1<String>"
     * }</pre>
     *
     * @return the declaring name of this Tuple1 type
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Tuple1 type.
     *
     * @return the Class object for Tuple1
     */
    @Override
    public Class<Tuple1<T1>> clazz() {
        return typeClass;
    }

    /**
     * Returns the parameter types of this generic type.
     * For Tuple1, this is an array containing a single element type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Tuple1<String>> type = TypeFactory.getType("Tuple1<String>");
     * Type<?>[] paramTypes = type.getParameterTypes();
     * // paramTypes[0] is Type<String>
     * }</pre>
     *
     * @return an array containing the element type
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
     * Converts a Tuple1 object to its string representation.
     * The format is a JSON array: [value].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Tuple1<String>> type = TypeFactory.getType("Tuple1<String>");
     * Tuple1<String> tuple = Tuple.of("Hello");
     * String str = type.stringOf(tuple);   // Returns ["Hello"]
     * }</pre>
     *
     * @param x the Tuple1 object to convert
     * @return the JSON string representation, or {@code null} if x is null
     */
    @Override
    public String stringOf(final Tuple1<T1> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x._1), Utils.jsc);
    }

    /**
     * Creates a Tuple1 object from its string representation.
     * Expects a JSON array format: [value].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Tuple1<String>> type = TypeFactory.getType("Tuple1<String>");
     * Tuple1<String> tuple = type.valueOf("[\"Hello\"]");
     * // tuple._1 = "Hello"
     * }</pre>
     *
     * @param str the string to parse
     * @return a Tuple1 object containing the parsed value, or {@code null} if str is empty
     */
    @SuppressWarnings("unchecked")
    @Override
    public Tuple1<T1> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(str, Utils.jdc, Object[].class);

        final T1 t1 = a[0] == null ? null : ((T1) (type1.clazz().isAssignableFrom(a[0].getClass()) ? a[0] : N.convert(a[0], type1)));

        return Tuple.of(t1);
    }

    /**
     * Appends the string representation of a Tuple1 object to the given Appendable.
     * Writes the format: [value].
     *
     * @param appendable the Appendable to write to
     * @param x the Tuple1 object to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Tuple1<T1> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);   //NOSONAR

                try {
                    bw.write(WD._BRACKET_L);

                    type1.appendTo(bw, x._1);

                    bw.write(WD._BRACKET_R);

                    if (!isBufferedWriter) {
                        bw.flush();
                    }
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            } else {
                appendable.append(WD._BRACKET_L);

                type1.appendTo(appendable, x._1);

                appendable.append(WD._BRACKET_R);
            }
        }
    }

    /**
     * Writes the character representation of a Tuple1 object to the given CharacterWriter.
     * This method is used for JSON/XML serialization. Writes the format: [value].
     *
     * @param writer the CharacterWriter to write to
     * @param x the Tuple1 object to write
     * @param config the serialization configuration for formatting options
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Tuple1<T1> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                type1.writeCharacter(writer, x._1, config);

                writer.write(WD._BRACKET_R);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Generates the type name for a Tuple1 type with the specified element type.
     *
     * @param t1TypeName the name of the element type
     * @param isDeclaringName if {@code true}, uses simple class names; if {@code false}, uses canonical class names
     * @return the generated type name for Tuple1 with the specified element type
     */
    protected static String getTypeName(final String t1TypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Tuple1.class) + WD.LESS_THAN + TypeFactory.getType(t1TypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Tuple1.class) + WD.LESS_THAN + TypeFactory.getType(t1TypeName).name() + WD.GREATER_THAN;
        }
    }
}
