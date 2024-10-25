/*
 * Copyright (C) 2019 HaiYang Li
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

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@SuppressWarnings("java:S2160")
public class BooleanCharType extends AbstractType<Boolean> {

    private static final String typeName = "BooleanChar";

    protected BooleanCharType() {
        super(typeName);
    }

    @Override
    public Class<Boolean> clazz() {
        return Boolean.class;
    }

    /**
     * Checks if is boolean.
     *
     * @return {@code true}, if is boolean
     */
    @Override
    public boolean isBoolean() {
        return false;
    }

    /**
     *
     * @param b
     * @return
     */
    @Override
    public String stringOf(final Boolean b) {
        return (b == null || !b.booleanValue()) ? "N" : "Y";
    }

    /**
     *
     * @param st
     * @return
     */
    @Override
    public Boolean valueOf(final String st) {
        return "Y".equalsIgnoreCase(st) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public Boolean defaultValue() {
        return Boolean.FALSE;
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public Boolean valueOf(final char[] cbuf, final int offset, final int len) {
        return (cbuf == null || len == 0) ? defaultValue() : ((len == 1 && (cbuf[offset] == 'Y' || cbuf[offset] == 'y')) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final Boolean x) throws IOException {
        appendable.append(stringOf(x));
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Boolean x, final JSONXMLSerializationConfig<?> config) throws IOException {
        final char ch = config == null ? 0 : config.getCharQuotation();

        if (ch == 0) {
            writer.write(stringOf(x));
        } else {
            writer.write(ch);
            writer.write(stringOf(x));
            writer.write(ch);
        }
    }
}
