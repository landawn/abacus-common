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

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.nio.ByteBuffer} values.
 * Converts between {@code ByteBuffer} instances and their Base64-encoded string representations,
 * enabling storage and transmission of binary buffer data in text-based formats.
 *
 * <p>The buffer's content is defined as the bytes from index {@code 0} up to (but not including)
 * the current {@link java.nio.ByteBuffer#position() position}. The position is preserved across
 * calls to {@link #stringOf(ByteBuffer)} and {@link #byteArrayOf(ByteBuffer)}.</p>
 *
 * <p>There is no direct JDBC mapping; this type is intended for JSON/XML serialization contexts.
 * For database binary data, prefer {@link BytesType} or {@link BlobType}.</p>
 *
 * @see java.nio.ByteBuffer
 */
public class ByteBufferType extends AbstractType<ByteBuffer> {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "ByteBuffer"}).
     */
    public static final String BYTE_BUFFER = "ByteBuffer";

    private final Class<ByteBuffer> typeClass;

    /**
     * Package-private constructor for {@code ByteBufferType} using the standard {@link java.nio.ByteBuffer} class.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    ByteBufferType() {
        super(BYTE_BUFFER);

        typeClass = ByteBuffer.class;
    }

    /**
     * Package-private constructor for {@code ByteBufferType} with a specific {@link java.nio.ByteBuffer} subclass.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     *
     * @param cls the specific {@code ByteBuffer} subclass represented by this type handler; also used as the type name source
     */
    @SuppressWarnings("unchecked")
    ByteBufferType(final Class<? extends ByteBuffer> cls) {
        super(ClassUtil.getSimpleClassName(cls));

        typeClass = (Class<ByteBuffer>) cls;
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return the {@code ByteBuffer} class (or the specific subclass) this handler was created for
     */
    @Override
    public Class<ByteBuffer> javaType() {
        return typeClass;
    }

    /**
     * Indicates that this type handler manages {@link java.nio.ByteBuffer} values.
     *
     * @return {@code true} always
     */
    @Override
    public boolean isByteBuffer() {
        return true;
    }

    /**
     * Converts a {@link java.nio.ByteBuffer} to its Base64-encoded string representation.
     * The encoded bytes are taken from position {@code 0} to the buffer's current
     * {@link java.nio.ByteBuffer#position() position}; the position is restored after reading.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code ByteBuffer} to encode; may be {@code null}
     * @return the Base64-encoded string of the buffer's written content,
     *         or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final ByteBuffer x) {
        return x == null ? null : Strings.base64Encode(byteArrayOf(x));
    }

    /**
     * Decodes a Base64-encoded string and returns a {@link java.nio.ByteBuffer} wrapping the decoded bytes.
     * The resulting buffer has its position set to the length of the decoded array
     * (i.e. positioned at the end of the written data, consistent with the convention used by
     * {@link #byteArrayOf(ByteBuffer)} and {@link #valueOf(byte[])}).
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the Base64-encoded string to decode; may be {@code null} or empty
     * @return a {@code ByteBuffer} wrapping the decoded bytes with position at {@code bytes.length},
     *         or {@code null} if {@code str} is {@code null},
     *         or an empty buffer if {@code str} is empty
     * @throws IllegalArgumentException if {@code str} contains characters outside the Base64 alphabet
     * @see #valueOf(Object)
     * @see #stringOf(ByteBuffer)
     */
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
     * Extracts the written content of a {@link java.nio.ByteBuffer} as a byte array.
     * Copies bytes from index {@code 0} up to (but not including) the buffer's current
     * {@link java.nio.ByteBuffer#position() position}, then restores the position to its
     * original value before returning.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteBuffer buf = ByteBuffer.allocate(10);
     * buf.put((byte) 10).put((byte) 20);              // position is now 2
     * byte[] bytes = ByteBufferType.byteArrayOf(buf); // returns [10, 20]; buf.position() still 2
     * }</pre>
     *
     * @param x the {@code ByteBuffer} to extract bytes from; must not be {@code null}
     * @return a new byte array containing the buffer's written bytes (indices {@code 0..position-1})
     */
    public static byte[] byteArrayOf(final ByteBuffer x) {
        final byte[] bytes = new byte[x.position()];

        x.position(0);
        x.get(bytes);
        x.position(bytes.length);

        return bytes;
    }

    /**
     * Wraps a byte array in a {@link java.nio.ByteBuffer} with position set to the end
     * of the data (i.e. {@code bytes.length}).
     * The resulting buffer has capacity equal to {@code bytes.length} and limit equal to
     * {@code bytes.length}, with position at {@code bytes.length} — ready for reading
     * via {@link #byteArrayOf(ByteBuffer)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteBuffer buf = ByteBufferType.valueOf(new byte[] { 1, 2, 3 });
     * buf.position();   // returns 3
     * buf.limit();      // returns 3
     * buf.capacity();   // returns 3
     * }</pre>
     *
     * @param bytes the byte array to wrap; must not be {@code null}
     * @return a {@code ByteBuffer} wrapping {@code bytes} with position at {@code bytes.length}
     */
    public static ByteBuffer valueOf(final byte[] bytes) {
        return ByteBuffer.wrap(bytes, bytes.length, 0);
    }
}
