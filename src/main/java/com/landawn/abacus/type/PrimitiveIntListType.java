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

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

@SuppressWarnings("java:S2160")
public final class PrimitiveIntListType extends AbstractPrimitiveListType<IntList> {

    public static final String INT_LIST = IntList.class.getSimpleName();

    private final Type<int[]> arrayType = N.typeOf(int[].class);

    private final Type<?> elementType = N.typeOf(int.class);

    protected PrimitiveIntListType() {
        super(INT_LIST);
    }

    @Override
    public Class<IntList> clazz() {
        return IntList.class;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<?> getElementType() {
        return elementType;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final IntList x) {
        return x == null ? null : arrayType.stringOf(x.trimToSize().array());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public IntList valueOf(final String str) {
        return Strings.isEmpty(str) ? null : IntList.of(arrayType.valueOf(str));
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final IntList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.trimToSize().array());
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
    public void writeCharacter(final CharacterWriter writer, final IntList x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.trimToSize().array(), config);
        }
    }
}
