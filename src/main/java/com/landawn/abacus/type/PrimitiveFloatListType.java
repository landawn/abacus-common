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

import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class PrimitiveFloatListType extends AbstractPrimitiveListType<FloatList> {

    public static final String FLOAT_LIST = FloatList.class.getSimpleName();

    private final Type<float[]> arrayType = N.typeOf(float[].class);

    private final Type<?> elementType = N.typeOf(float.class);

    protected PrimitiveFloatListType() {
        super(FLOAT_LIST);
    }

    @Override
    public Class<FloatList> clazz() {
        return FloatList.class;
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
    public String stringOf(FloatList x) {
        return x == null ? null : arrayType.stringOf(x.trimToSize().array());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public FloatList valueOf(String str) {
        return N.isNullOrEmpty(str) ? null : FloatList.of(arrayType.valueOf(str));
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, FloatList x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.write(writer, x.trimToSize().array());
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
    public void writeCharacter(CharacterWriter writer, FloatList x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.trimToSize().array(), config);
        }
    }
}
