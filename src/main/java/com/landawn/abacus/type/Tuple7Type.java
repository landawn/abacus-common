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
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.WD;

/**
 * Type handler for {@link Tuple7} objects.
 * This class provides serialization and deserialization support for tuple instances
 * containing seven elements of potentially different types.
 *
 * @param <T1> the type of the first element in the tuple
 * @param <T2> the type of the second element in the tuple
 * @param <T3> the type of the third element in the tuple
 * @param <T4> the type of the fourth element in the tuple
 * @param <T5> the type of the fifth element in the tuple
 * @param <T6> the type of the sixth element in the tuple
 * @param <T7> the type of the seventh element in the tuple
 */
@SuppressWarnings("java:S2160")
public class Tuple7Type<T1, T2, T3, T4, T5, T6, T7> extends AbstractType<Tuple7<T1, T2, T3, T4, T5, T6, T7>> {

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<Tuple7<T1, T2, T3, T4, T5, T6, T7>> typeClass = (Class) Tuple7.class; //NOSONAR

    private final Type<T1> type1;

    private final Type<T2> type2;

    private final Type<T3> type3;

    private final Type<T4> type4;

    private final Type<T5> type5;

    private final Type<T6> type6;

    private final Type<T7> type7;

    private final Type<?>[] parameterTypes;

    /**
     * Constructs a Tuple7Type instance with the specified element types.
     * This constructor is package-private and should only be called by TypeFactory.
     *
     * @param t1TypeName the name of the first element type
     * @param t2TypeName the name of the second element type
     * @param t3TypeName the name of the third element type
     * @param t4TypeName the name of the fourth element type
     * @param t5TypeName the name of the fifth element type
     * @param t6TypeName the name of the sixth element type
     * @param t7TypeName the name of the seventh element type
     */
    Tuple7Type(final String t1TypeName, final String t2TypeName, final String t3TypeName, final String t4TypeName, final String t5TypeName,
            final String t6TypeName, final String t7TypeName) {
        super(getTypeName(t1TypeName, t2TypeName, t3TypeName, t4TypeName, t5TypeName, t6TypeName, t7TypeName, false));

        declaringName = getTypeName(t1TypeName, t2TypeName, t3TypeName, t4TypeName, t5TypeName, t6TypeName, t7TypeName, true);

        type1 = TypeFactory.getType(t1TypeName);
        type2 = TypeFactory.getType(t2TypeName);
        type3 = TypeFactory.getType(t3TypeName);
        type4 = TypeFactory.getType(t4TypeName);
        type5 = TypeFactory.getType(t5TypeName);
        type6 = TypeFactory.getType(t6TypeName);
        type7 = TypeFactory.getType(t7TypeName);
        parameterTypes = new Type[] { type1, type2, type3, type4, type5, type6, type7 };
    }

    /**
     * Returns the declaring name of this type, which includes simple class names.
     * For example: "Tuple7&lt;String, Integer, Double, Boolean, Long, Float, Byte&gt;" instead of the fully qualified name.
     *
     * @return the declaring name of this Tuple7 type
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Java class that this type handler manages.
     *
     * @return {@code Tuple7.class}
     */
    @Override
    public Class<Tuple7<T1, T2, T3, T4, T5, T6, T7>> clazz() {
        return typeClass;
    }

    /**
     * Returns the parameter types of this tuple type.
     * The returned array contains the types of all seven elements in order.
     *
     * @return an array containing the types of the tuple elements
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type.
     * Tuple7Type is always a generic type as it has type parameters.
     *
     * @return {@code true} always, as Tuple7 is a parameterized type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Converts the given Tuple7 object to its string representation.
     * The tuple is serialized as a JSON array containing its seven elements.
     *
     * @param x the Tuple7 object to convert
     * @return a JSON string representation of the tuple, or {@code null} if x is null
     */
    @Override
    public String stringOf(final Tuple7<T1, T2, T3, T4, T5, T6, T7> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x._1, x._2, x._3, x._4, x._5, x._6, x._7), Utils.jsc);
    }

    /**
     * Parses the given string into a Tuple7 object.
     * The string should be a JSON array representation with exactly seven elements.
     * Each element will be converted to the appropriate type based on the tuple's type parameters.
     *
     * @param str the JSON string to parse
     * @return a Tuple7 object parsed from the string, or {@code null} if str is empty
     */
    @SuppressWarnings("unchecked")
    @Override
    public Tuple7<T1, T2, T3, T4, T5, T6, T7> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(str, Utils.jdc, Object[].class);

        if (a == null || a.length < 7) {
            throw new IllegalArgumentException("Invalid Tuple7 format. Expected array with at least 7 elements but got: " + str);
        }

        final T1 t1 = a[0] == null ? null : ((T1) (type1.clazz().isAssignableFrom(a[0].getClass()) ? a[0] : N.convert(a[0], type1)));
        final T2 t2 = a[1] == null ? null : ((T2) (type2.clazz().isAssignableFrom(a[1].getClass()) ? a[1] : N.convert(a[1], type2)));
        final T3 t3 = a[2] == null ? null : ((T3) (type3.clazz().isAssignableFrom(a[2].getClass()) ? a[2] : N.convert(a[2], type3)));
        final T4 t4 = a[3] == null ? null : ((T4) (type4.clazz().isAssignableFrom(a[3].getClass()) ? a[3] : N.convert(a[3], type4)));
        final T5 t5 = a[4] == null ? null : ((T5) (type5.clazz().isAssignableFrom(a[4].getClass()) ? a[4] : N.convert(a[4], type5)));
        final T6 t6 = a[5] == null ? null : ((T6) (type6.clazz().isAssignableFrom(a[5].getClass()) ? a[5] : N.convert(a[5], type6)));
        final T7 t7 = a[6] == null ? null : ((T7) (type7.clazz().isAssignableFrom(a[6].getClass()) ? a[6] : N.convert(a[6], type7)));

        return Tuple.of(t1, t2, t3, t4, t5, t6, t7);
    }

    /**
     * Appends the string representation of the Tuple7 to the given Appendable.
     * The output format is: [element1, element2, element3, element4, element5, element6, element7]
     * Special handling is provided for Writer instances to improve performance.
     *
     * @param appendable the Appendable to write to
     * @param x the Tuple7 object to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Tuple7<T1, T2, T3, T4, T5, T6, T7> x) throws IOException {
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
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    type3.appendTo(bw, x._3);
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    type4.appendTo(bw, x._4);
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    type5.appendTo(bw, x._5);
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    type6.appendTo(bw, x._6);
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    type7.appendTo(bw, x._7);

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
                appendable.append(ELEMENT_SEPARATOR);
                type3.appendTo(appendable, x._3);
                appendable.append(ELEMENT_SEPARATOR);
                type4.appendTo(appendable, x._4);
                appendable.append(ELEMENT_SEPARATOR);
                type5.appendTo(appendable, x._5);
                appendable.append(ELEMENT_SEPARATOR);
                type6.appendTo(appendable, x._6);
                appendable.append(ELEMENT_SEPARATOR);
                type7.appendTo(appendable, x._7);

                appendable.append(WD._BRACKET_R);
            }
        }
    }

    /**
     * Writes the character representation of the Tuple7 to the given CharacterWriter.
     * The output format is: [element1, element2, element3, element4, element5, element6, element7]
     * This method is optimized for character-based output streams.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Tuple7 object to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Tuple7<T1, T2, T3, T4, T5, T6, T7> x, final JsonXmlSerializationConfig<?> config)
            throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                type1.writeCharacter(writer, x._1, config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                type2.writeCharacter(writer, x._2, config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                type3.writeCharacter(writer, x._3, config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                type4.writeCharacter(writer, x._4, config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                type5.writeCharacter(writer, x._5, config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                type6.writeCharacter(writer, x._6, config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                type7.writeCharacter(writer, x._7, config);

                writer.write(WD._BRACKET_R);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Generates the type name for a Tuple7 with the specified element type names.
     *
     * @param t1TypeName the type name of the first element
     * @param t2TypeName the type name of the second element
     * @param t3TypeName the type name of the third element
     * @param t4TypeName the type name of the fourth element
     * @param t5TypeName the type name of the fifth element
     * @param t6TypeName the type name of the sixth element
     * @param t7TypeName the type name of the seventh element
     * @param isDeclaringName if {@code true}, returns the declaring name (simple class names);
     *                        if {@code false}, returns the full canonical name
     * @return the formatted type name string
     */
    protected static String getTypeName(final String t1TypeName, final String t2TypeName, final String t3TypeName, final String t4TypeName,
            final String t5TypeName, final String t6TypeName, final String t7TypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Tuple7.class) + WD.LESS_THAN + TypeFactory.getType(t1TypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(t3TypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(t4TypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(t5TypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(t6TypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(t7TypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Tuple7.class) + WD.LESS_THAN + TypeFactory.getType(t1TypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(t3TypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(t4TypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(t5TypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(t6TypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(t7TypeName).name() + WD.GREATER_THAN;
        }
    }
}
