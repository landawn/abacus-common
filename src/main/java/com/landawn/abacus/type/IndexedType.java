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

import java.io.IOException;
import java.io.Writer;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.9
 */
@SuppressWarnings("java:S2160")
public class IndexedType<T> extends AbstractType<Indexed<T>> {

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<Indexed<T>> typeClass = (Class) Indexed.class; //NOSONAR

    private final Type<T> valueType;

    private final Type<?>[] parameterTypes;

    IndexedType(String valueTypeName) {
        super(getTypeName(valueTypeName, false));

        this.declaringName = getTypeName(valueTypeName, true);
        this.valueType = TypeFactory.getType(valueTypeName);
        this.parameterTypes = new Type[] { valueType };
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<Indexed<T>> clazz() {
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
     * Checks if is generic type.
     *
     * @return true, if is generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(Indexed<T> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x.index(), x.value()), Utils.jsc);
    }

    /**
     *
     * @param str
     * @return {@code null} if {@code (Strings.isEmpty(str))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    @Override
    public Indexed<T> valueOf(String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Object[] a = Utils.jsonParser.deserialize(Object[].class, str, Utils.jdc);

        final long index = a[0] == null ? 0 : (a[0] instanceof Number ? ((Number) a[0]).longValue() : Numbers.toLong(a[0].toString()));
        final T value = a[1] == null ? null : ((T) (valueType.clazz().isAssignableFrom(a[1].getClass()) ? a[1] : N.convert(a[1], valueType)));

        return Indexed.of(value, index);
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, Indexed<T> x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            boolean isBufferedWriter = writer instanceof BufferedWriter || writer instanceof java.io.BufferedWriter;
            final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);

            try {
                bw.write(WD._BRACKET_L);

                bw.write(String.valueOf(x.longIndex()));
                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                valueType.write(bw, x.value());

                bw.write(WD._BRACKET_R);

                if (!isBufferedWriter) {
                    bw.flush();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (!isBufferedWriter) {
                    Objectory.recycle((BufferedWriter) bw);
                }
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
    public void writeCharacter(CharacterWriter writer, Indexed<T> x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                writer.write(String.valueOf(x.longIndex()));
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                valueType.writeCharacter(writer, x.value(), config);

                writer.write(WD._BRACKET_R);

            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Gets the type name.
     *
     * @param valueTypeName
     * @param isDeclaringName
     * @return
     */
    protected static String getTypeName(String valueTypeName, boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Indexed.class) + WD.LESS_THAN + TypeFactory.getType(valueTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Indexed.class) + WD.LESS_THAN + TypeFactory.getType(valueTypeName).name() + WD.GREATER_THAN;
        }
    }
}
