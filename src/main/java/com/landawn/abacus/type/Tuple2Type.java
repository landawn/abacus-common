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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.WD;

/**
 * Type handler for {@link Tuple2} objects.
 * This class provides serialization and deserialization support for tuple instances
 * containing two elements of potentially different types.
 *
 * @param <T1> the type of the first element in the tuple
 * @param <T2> the type of the second element in the tuple
 */
@SuppressWarnings("java:S2160")
public class Tuple2Type<T1, T2> extends AbstractType<Tuple2<T1, T2>> {

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<Tuple2<T1, T2>> typeClass = (Class) Tuple2.class; //NOSONAR

    private final Type<T1> type1;

    private final Type<T2> type2;

    private final Type<?>[] parameterTypes;

    Tuple2Type(final String t1TypeName, final String t2TypeName) {
        super(getTypeName(t1TypeName, t2TypeName, false));

        declaringName = getTypeName(t1TypeName, t2TypeName, true);
        type1 = TypeFactory.getType(t1TypeName);
        type2 = TypeFactory.getType(t2TypeName);
        parameterTypes = new Type[] { type1, type2 };
    }

    /**
     * Returns the declaring name of this type, which includes simple class names.
     * For example: "Tuple2<String, Integer>" instead of the fully qualified name.
     *
     * @return the declaring name of this Tuple2 type
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Java class that this type handler manages.
     *
     * @return {@code Tuple2.class}
     */
    @Override
    public Class<Tuple2<T1, T2>> clazz() {
        return typeClass;
    }

    /**
     * Returns the parameter types of this tuple type.
     * The returned array contains the types of the first and second elements in order.
     *
     * @return an array containing the types of the tuple elements
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this is a generic type.
     * Tuple2Type is always a generic type as it has type parameters.
     *
     * @return {@code true} always, as Tuple2 is a parameterized type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Converts the given Tuple2 object to its string representation.
     * The tuple is serialized as a JSON array containing its two elements.
     *
     * @param x the Tuple2 object to convert
     * @return a JSON string representation of the tuple, or null if x is null
     */
    @Override
    public String stringOf(final Tuple2<T1, T2> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x._1, x._2), Utils.jsc);
    }

    /**
     * Parses the given string into a Tuple2 object.
     * The string should be a JSON array representation with exactly two elements.
     * Each element will be converted to the appropriate type based on the tuple's type parameters.
     *
     * @param str the JSON string to parse
     * @return a Tuple2 object parsed from the string, or null if str is empty
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    @Override
    public Tuple2<T1, T2> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(str, Utils.jdc, Object[].class);

        final T1 t1 = a[0] == null ? null : ((T1) (type1.clazz().isAssignableFrom(a[0].getClass()) ? a[0] : N.convert(a[0], type1)));
        final T2 t2 = a[1] == null ? null : ((T2) (type2.clazz().isAssignableFrom(a[1].getClass()) ? a[1] : N.convert(a[1], type2)));

        return Tuple.of(t1, t2);
    }

    /**
     * Appends the string representation of the Tuple2 to the given Appendable.
     * The output format is: [element1, element2]
     * Special handling is provided for Writer instances to improve performance.
     *
     * @param appendable the Appendable to write to
     * @param x the Tuple2 object to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Tuple2<T1, T2> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR

                try {
                    bw.write(WD._BRACKET_L);

                    type1.appendTo(bw, x._1);
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    type2.appendTo(bw, x._2);

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
                appendable.append(ELEMENT_SEPARATOR);
                type2.appendTo(appendable, x._2);

                appendable.append(WD._BRACKET_R);
            }
        }
    }

    /**
     * Writes the character representation of the Tuple2 to the given CharacterWriter.
     * The output format is: [element1, element2]
     * This method is optimized for character-based output streams.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Tuple2 object to write
     * @param config the serialization configuration (may be null)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Tuple2<T1, T2> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                type1.writeCharacter(writer, x._1, config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                type2.writeCharacter(writer, x._2, config);

                writer.write(WD._BRACKET_R);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Generates the type name for a Tuple2 with the specified element type names.
     * 
     * @param t1TypeName the type name of the first element
     * @param t2TypeName the type name of the second element
     * @param isDeclaringName if true, returns the declaring name (simple class names); 
     *                        if false, returns the full canonical name
     * @return the formatted type name string
     */
    protected static String getTypeName(final String t1TypeName, final String t2TypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Tuple2.class) + WD.LESS_THAN + TypeFactory.getType(t1TypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Tuple2.class) + WD.LESS_THAN + TypeFactory.getType(t1TypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).name() + WD.GREATER_THAN;
        }
    }
}