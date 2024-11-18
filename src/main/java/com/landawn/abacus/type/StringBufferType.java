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

public class StringBufferType extends AbstractCharSequenceType<StringBuilder> {

    public static final String STRING_BUFFER = StringBuffer.class.getSimpleName();

    StringBufferType() {
        super(STRING_BUFFER);
    }

    @Override
    public Class<StringBuilder> clazz() {
        return StringBuilder.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final StringBuilder x) {
        return x == null ? null : x.toString();
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public StringBuilder valueOf(final String str) {
        return str == null ? null : new StringBuilder(str);
    }

    @Override
    public boolean isImmutable() {
        return false;
    }
}
