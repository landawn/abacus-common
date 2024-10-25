/*
 * Copyright (C) 2015 HaiYang Li
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
import java.lang.reflect.Array;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Range;
import com.landawn.abacus.util.Range.BoundType;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 *
 * @param <T>
 */
@SuppressWarnings("java:S2160")
public class RangeType<T extends Comparable<? super T>> extends AbstractType<Range<T>> {

    static final Type<String> strType = TypeFactory.getType(String.class);

    public static final String RANGE = Range.class.getSimpleName();

    private final String declaringName;

    private final Class<Range<T>> typeClass;

    private final Type<T>[] parameterTypes;

    private final Type<T> elementType;

    //    RangeType() {
    //        this(Object.class.getSimpleName());
    //    }

    @SuppressWarnings("rawtypes")
    RangeType(final String parameterTypeName) {
        super(RANGE + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN);

        declaringName = RANGE + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        typeClass = (Class) Range.class;
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

    @Override
    public Class<Range<T>> clazz() {
        return typeClass;
    }

    /**
     * Gets the parameter types.
     *
     * @return
     */
    @Override
    public Type<T>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<T> getElementType() {
        return elementType;
    }

    /**
     * Checks if is generic type.
     *
     * @return {@code true}, if is generic type
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
    @MayReturnNull
    @Override
    public String stringOf(final Range<T> x) {
        if (x == null) {
            return null; // NOSONAR
        }

        final BoundType boundType = x.boundType();
        final String prefix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.OPEN_CLOSED) ? "(" : "[";
        final String postfix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.CLOSED_OPEN) ? ")" : "]";
        Type<T> type = elementType;

        if (x.lowerEndpoint() != null) {
            type = TypeFactory.getType(x.lowerEndpoint().getClass());
        } else if (x.upperEndpoint() != null) {
            type = TypeFactory.getType(x.upperEndpoint().getClass());
        }

        return prefix + type.stringOf(x.lowerEndpoint()) + ELEMENT_SEPARATOR + type.stringOf(x.upperEndpoint()) + postfix;
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @Override
    public Range<T> valueOf(String str) {
        str = Strings.trim(str);

        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final String prefix = str.substring(0, 1);
        final String postfix = str.substring(str.length() - 1);

        final T[] tmp = (T[]) Utils.jsonParser.deserialize(str, 1, str.length() - 1, Utils.jdc, Array.newInstance(elementType.clazz(), 0).getClass());

        if ("(".equals(prefix)) {
            return ")".equals(postfix) ? Range.open(tmp[0], tmp[1]) : Range.openClosed(tmp[0], tmp[1]);
        } else {
            return ")".equals(postfix) ? Range.closedOpen(tmp[0], tmp[1]) : Range.closed(tmp[0], tmp[1]);
        }
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final Range<T> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            final BoundType boundType = x.boundType();
            final String prefix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.OPEN_CLOSED) ? "(" : "[";
            final String postfix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.CLOSED_OPEN) ? ")" : "]";
            Type<T> type = elementType;

            if (x.lowerEndpoint() != null) {
                type = TypeFactory.getType(x.lowerEndpoint().getClass());
            } else if (x.upperEndpoint() != null) {
                type = TypeFactory.getType(x.upperEndpoint().getClass());
            }

            if (appendable instanceof final Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR

                try {
                    bw.write(prefix);

                    type.appendTo(bw, x.lowerEndpoint());
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    type.appendTo(bw, x.upperEndpoint());

                    bw.write(postfix);

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
                appendable.append(prefix);

                type.appendTo(appendable, x.lowerEndpoint());
                appendable.append(ELEMENT_SEPARATOR);
                type.appendTo(appendable, x.upperEndpoint());

                appendable.append(postfix);
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
    public void writeCharacter(final CharacterWriter writer, final Range<T> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final BoundType boundType = x.boundType();
            final String prefix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.OPEN_CLOSED) ? "(" : "[";
            final String postfix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.CLOSED_OPEN) ? ")" : "]";
            Type<T> type = elementType;

            if (x.lowerEndpoint() != null) {
                type = TypeFactory.getType(x.lowerEndpoint().getClass());
            } else if (x.upperEndpoint() != null) {
                type = TypeFactory.getType(x.upperEndpoint().getClass());
            }

            final CharacterWriter tmpWriter = writer instanceof BufferedJSONWriter ? Objectory.createBufferedJSONWriter() : Objectory.createBufferedXMLWriter();

            try {
                tmpWriter.write(prefix);
                type.writeCharacter(tmpWriter, x.lowerEndpoint(), config);
                tmpWriter.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                type.writeCharacter(tmpWriter, x.upperEndpoint(), config);
                tmpWriter.write(postfix);

                strType.writeCharacter(writer, tmpWriter.toString(), config);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                Objectory.recycle(tmpWriter);
            }
        }
    }
}
