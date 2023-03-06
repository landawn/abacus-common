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

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @param <L>
 * @param <M>
 * @param <R>
 * @since 0.9
 */
@SuppressWarnings("java:S2160")
public class TripleType<L, M, R> extends AbstractType<Triple<L, M, R>> {

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<Triple<L, M, R>> typeClass = (Class) Triple.class; //NOSONAR

    private final Type<L> leftType;

    private final Type<M> middleType;

    private final Type<R> rightType;

    private final Type<?>[] parameterTypes;

    TripleType(String leftTypeName, String middleTypeName, String rightTypeName) {
        super(getTypeName(leftTypeName, middleTypeName, rightTypeName, false));

        this.declaringName = getTypeName(leftTypeName, middleTypeName, rightTypeName, true);

        this.leftType = TypeFactory.getType(leftTypeName);
        this.middleType = TypeFactory.getType(middleTypeName);
        this.rightType = TypeFactory.getType(rightTypeName);
        this.parameterTypes = new Type[] { leftType, middleType, rightType };
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

    @Override
    public Class<Triple<L, M, R>> clazz() {
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
    public String stringOf(Triple<L, M, R> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asArray(x.left, x.middle, x.right), Utils.jsc);
    }

    /**
     *
     * @param str
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public Triple<L, M, R> valueOf(String str) {
        if (N.isNullOrEmpty(str)) {
            return null;
        }

        final Object[] a = Utils.jsonParser.deserialize(Object[].class, str, Utils.jdc);

        final L left = a[0] == null ? null : ((L) (leftType.clazz().isAssignableFrom(a[0].getClass()) ? a[0] : N.convert(a[0], leftType)));
        final M middle = a[1] == null ? null : ((M) (middleType.clazz().isAssignableFrom(a[1].getClass()) ? a[1] : N.convert(a[1], middleType)));
        final R right = a[2] == null ? null : ((R) (rightType.clazz().isAssignableFrom(a[2].getClass()) ? a[2] : N.convert(a[2], rightType)));

        return Triple.of(left, middle, right);
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, Triple<L, M, R> x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            boolean isBufferedWriter = writer instanceof BufferedWriter || writer instanceof java.io.BufferedWriter;
            final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);

            try {
                bw.write(WD._BRACKET_L);

                leftType.write(bw, x.left);
                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                middleType.write(bw, x.middle);
                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                rightType.write(bw, x.right);

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
    public void writeCharacter(CharacterWriter writer, Triple<L, M, R> x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                leftType.writeCharacter(writer, x.left, config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                middleType.writeCharacter(writer, x.middle, config);
                writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                rightType.writeCharacter(writer, x.right, config);

                writer.write(WD._BRACKET_R);

            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Gets the type name.
     *
     * @param leftTypeName
     * @param middleTypeName
     * @param rightTypeName
     * @param isDeclaringName
     * @return
     */
    protected static String getTypeName(String leftTypeName, String middleTypeName, String rightTypeName, boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Triple.class) + WD.LESS_THAN + TypeFactory.getType(leftTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(middleTypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(rightTypeName).declaringName()
                    + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Triple.class) + WD.LESS_THAN + TypeFactory.getType(leftTypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(middleTypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(rightTypeName).name() + WD.GREATER_THAN;
        }
    }
}
