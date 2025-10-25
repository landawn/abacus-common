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

import java.nio.ByteBuffer;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for ByteBuffer operations.
 * This class provides conversion between java.nio.ByteBuffer instances
 * and their Base64-encoded string representations, enabling storage and
 * transmission of binary buffer data in text formats.
 */
public class ByteBufferType extends AbstractType<ByteBuffer> {

    /**
     * The type name constant for ByteBuffer type identification.
     */
    public static final String BYTE_BUFFER = "ByteBuffer";

    ByteBufferType() {
        super(BYTE_BUFFER);
    }

    ByteBufferType(final Class<? extends ByteBuffer> cls) {
        super(ClassUtil.getSimpleClassName(cls));
    }

    /**
     * Returns the Class object representing the ByteBuffer class.
     *
     * @return the Class object for java.nio.ByteBuffer
     */
    @Override
    public Class<ByteBuffer> clazz() {
        return ByteBuffer.class;
    }

    /**
     * Determines whether this type represents a ByteBuffer.
     * Always returns {@code true} for ByteBufferType instances.
     *
     * @return {@code true} indicating this is a byte buffer type
     */
    @Override
    public boolean isByteBuffer() {
        return true;
    }

    /**
     * Converts a ByteBuffer to its Base64-encoded string representation.
     * The buffer's content from position 0 to the current position is encoded.
     *
     * @param x the ByteBuffer to encode
     * @return the Base64-encoded string representation of the buffer's content,
     *         or null if the input is null
     */
    @Override
    public String stringOf(final ByteBuffer x) {
        return x == null ? null : Strings.base64Encode(byteArrayOf(x));
    }

    /**
     * Converts a Base64-encoded string back to a ByteBuffer.
     * Creates a ByteBuffer wrapping the decoded byte array with position
     * set to the array length and limit/capacity set to array length.
     *
     * @param str the Base64-encoded string to decode
     * @return a ByteBuffer containing the decoded data, or null if str is null,
     *         or an empty buffer if str is empty
     * @throws IllegalArgumentException if the input string is not valid Base64
     */
    @MayReturnNull
    @Override
    public ByteBuffer valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.isEmpty()) {
            return valueOf(N.EMPTY_BYTE_ARRAY);
        } else {
            return valueOf(Strings.base64Decode(str));
        }
    }

    /**
     * Extracts the content of a ByteBuffer as a byte array.
     * Reads from position 0 to the current position of the buffer,
     * then resets the position back to its original value.
     *
     * @param x the ByteBuffer to extract bytes from
     * @return a byte array containing the buffer's content from 0 to current position
     */
    public static byte[] byteArrayOf(final ByteBuffer x) {
        final byte[] bytes = new byte[x.position()];

        x.position(0);
        x.get(bytes);
        x.position(bytes.length);

        return bytes;
    }

    /**
     * Creates a ByteBuffer from a byte array.
     * The resulting buffer wraps the input array with position set to the array length,
     * limit set to array length, and capacity equal to array length.
     *
     * @param bytes the byte array to wrap in a ByteBuffer
     * @return a ByteBuffer wrapping the input array with position at the end
     */
    public static ByteBuffer valueOf(final byte[] bytes) {
        return ByteBuffer.wrap(bytes, bytes.length, 0);
    }
}