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

import java.nio.ByteBuffer;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class ByteBufferType extends AbstractType<ByteBuffer> {

    public static final String BYTE_BUFFER = "ByteBuffer";

    ByteBufferType() {
        super(BYTE_BUFFER);
    }

    ByteBufferType(Class<? extends ByteBuffer> cls) {
        super(ClassUtil.getSimpleClassName(cls));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<ByteBuffer> clazz() {
        return ByteBuffer.class;
    }

    /**
     * Checks if is byte buffer.
     *
     * @return true, if is byte buffer
     */
    @Override
    public boolean isByteBuffer() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(ByteBuffer x) {
        return x == null ? null : Strings.base64Encode(byteArrayOf(x));
    }

    /**
     *
     * @param str
     * @return {@code null} if {@code (str == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public ByteBuffer valueOf(String str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.length() == 0) {
            return valueOf(N.EMPTY_BYTE_ARRAY);
        } else {
            return valueOf(Strings.base64Decode(str));
        }
    }

    /**
     * Byte array of.
     *
     * @param x
     * @return
     */
    public static byte[] byteArrayOf(ByteBuffer x) {
        final byte[] bytes = new byte[x.position()];

        x.position(0);
        x.get(bytes);
        x.position(bytes.length);

        return bytes;
    }

    /**
     *
     * @param bytes
     * @return
     */
    public static ByteBuffer valueOf(byte[] bytes) {
        return ByteBuffer.wrap(bytes, bytes.length, 0);
    }
}
