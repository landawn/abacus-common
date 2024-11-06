/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.landawn.abacus.util.Tuple.Tuple9;
import com.landawn.abacus.util.WD;

/**
 *
 * @param <T1>
 * @param <T2>
 * @param <T3>
 * @param <T4>
 * @param <T5>
 * @param <T6>
 * @param <T7>
 * @param <T8>
 * @param <T9>
 */
@SuppressWarnings("java:S2160")
public class Tuple9Type<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends AbstractType<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> {

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> typeClass = (Class) Tuple9.class; //NOSONAR

    /** The type 1. */
    private final Type<T1> type1;

    /** The type 2. */
    private final Type<T2> type2;

    /** The type 3. */
    private final Type<T3> type3;

    /** The type 4. */
    private final Type<T4> type4;

    /** The type 5. */
    private final Type<T5> type5;

    /** The type 6. */
    private final Type<T6> type6;

    /** The type 7. */
    private final Type<T7> type7;

    /** The type 8. */
    private final Type<T8> type8;

    /** The type 9. */
    private final Type<T9> type9;

    private final Type<?>[] parameterTypes;

    Tuple9Type(final String t1TypeName, final String t2TypeName, final String t3TypeName, final String t4TypeName, final String t5TypeName,
            final String t6TypeName, final String t7TypeName, final String t8TypeName, final String t9TypeName) {
        super(getTypeName(t1TypeName, t2TypeName, t3TypeName, t4TypeName, t5TypeName, t6TypeName, t7TypeName, t8TypeName, t9TypeName, false));

        declaringName = getTypeName(t1TypeName, t2TypeName, t3TypeName, t4TypeName, t5TypeName, t6TypeName, t7TypeName, t8TypeName, t9TypeName, true);

        type1 = TypeFactory.getType(t1TypeName);
        type2 = TypeFactory.getType(t2TypeName);
        type3 = TypeFactory.getType(t3TypeName);
        type4 = TypeFactory.getType(t4TypeName);
        type5 = TypeFactory.getType(t5TypeName);
        type6 = TypeFactory.getType(t6TypeName);
        type7 = TypeFactory.getType(t7TypeName);
        type8 = TypeFactory.getType(t8TypeName);
        type9 = TypeFactory.getType(t9TypeName);
        parameterTypes = new Type[] { type1, type2, type3, type4, type5, type6, type7, type8, type9 };
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

    @Override
    public Class<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> clazz() {
        return typeClass;
    }

    /**
     * Gets the parameter types.
     *
     * @return
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x._1, x._2, x._3, x._4, x._5, x._6, x._7, x._8, x._9), Utils.jsc);
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(str, Utils.jdc, Object[].class);

        final T1 t1 = a[0] == null ? null : ((T1) (type1.clazz().isAssignableFrom(a[0].getClass()) ? a[0] : N.convert(a[0], type1)));
        final T2 t2 = a[1] == null ? null : ((T2) (type2.clazz().isAssignableFrom(a[1].getClass()) ? a[1] : N.convert(a[1], type2)));
        final T3 t3 = a[2] == null ? null : ((T3) (type3.clazz().isAssignableFrom(a[2].getClass()) ? a[2] : N.convert(a[2], type3)));
        final T4 t4 = a[3] == null ? null : ((T4) (type4.clazz().isAssignableFrom(a[3].getClass()) ? a[3] : N.convert(a[3], type4)));
        final T5 t5 = a[4] == null ? null : ((T5) (type5.clazz().isAssignableFrom(a[4].getClass()) ? a[4] : N.convert(a[4], type5)));
        final T6 t6 = a[5] == null ? null : ((T6) (type6.clazz().isAssignableFrom(a[5].getClass()) ? a[5] : N.convert(a[5], type6)));
        final T7 t7 = a[6] == null ? null : ((T7) (type7.clazz().isAssignableFrom(a[6].getClass()) ? a[6] : N.convert(a[6], type7)));
        final T8 t8 = a[7] == null ? null : ((T8) (type8.clazz().isAssignableFrom(a[7].getClass()) ? a[7] : N.convert(a[7], type8)));
        final T9 t9 = a[8] == null ? null : ((T9) (type9.clazz().isAssignableFrom(a[8].getClass()) ? a[8] : N.convert(a[8], type9)));

        return Tuple.of(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof final Writer writer) {
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
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    type8.appendTo(bw, x._8);
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    type9.appendTo(bw, x._9);

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
                appendable.append(ELEMENT_SEPARATOR);
                type8.appendTo(appendable, x._8);
                appendable.append(ELEMENT_SEPARATOR);
                type9.appendTo(appendable, x._9);

                appendable.append(WD._BRACKET_R);
            }
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> x, final JSONXMLSerializationConfig<?> config)
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
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                type8.writeCharacter(writer, x._8, config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                type9.writeCharacter(writer, x._9, config);

                writer.write(WD._BRACKET_R);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Gets the type name.
     *
     * @param t1TypeName
     * @param t2TypeName
     * @param t3TypeName
     * @param t4TypeName
     * @param t5TypeName
     * @param t6TypeName
     * @param t7TypeName
     * @param t8TypeName
     * @param t9TypeName
     * @param isDeclaringName
     * @return
     */
    protected static String getTypeName(final String t1TypeName, final String t2TypeName, final String t3TypeName, final String t4TypeName,
            final String t5TypeName, final String t6TypeName, final String t7TypeName, final String t8TypeName, final String t9TypeName,
            final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Tuple9.class) + WD.LESS_THAN + TypeFactory.getType(t1TypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(t3TypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(t4TypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(t5TypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(t6TypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(t7TypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(t8TypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(t9TypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Tuple9.class) + WD.LESS_THAN + TypeFactory.getType(t1TypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(t3TypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(t4TypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(t5TypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(t6TypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(t7TypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(t8TypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(t9TypeName).name() + WD.GREATER_THAN;
        }
    }
}
