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
import com.landawn.abacus.util.cs;

/**
 * Type handler for {@link java.nio.ByteBuffer} values.
 * Converts between {@code ByteBuffer} instances and their Base64-encoded string representations,
 * enabling storage and transmission of binary buffer data in text-based formats.
 *
 * <p>The buffer's content is defined as the bytes from index {@code 0} up to (but not including)
 * the current {@link java.nio.ByteBuffer#position() position}. The position, limit, and mark are
 * preserved across calls to {@link #stringOf(ByteBuffer)} and {@link #byteArrayOf(ByteBuffer)}.</p>
 *
 * <p>There is no direct JDBC mapping; this type is intended for JSON/XML serialization contexts.
 * For database binary data, prefer {@link BytesType} or {@link BlobType}.</p>
 *
 * @see java.nio.ByteBuffer
 */
@SuppressWarnings("java:S2160")
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
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    ByteBufferType(final Class<? extends ByteBuffer> cls) throws IllegalArgumentException {
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
     * {@link java.nio.ByteBuffer#position() position}; the buffer's position, limit, and mark are
     * not modified.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
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
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * <p>The returned buffer is an instance of the class this handler was created for: a heap buffer for
     * {@link java.nio.ByteBuffer} itself, a direct buffer for a handler whose class only direct buffers
     * satisfy (a direct buffer is also a {@link java.nio.MappedByteBuffer} on this JDK). A handler created for
     * a buffer class that neither can satisfy cannot be constructed from text at all.</p>
     *
     * @param str the Base64-encoded string to decode; may be {@code null} or empty
     * @return a buffer wrapping the decoded bytes with its position at the end of the data,
     *         an empty buffer if {@code str} is empty, or {@code null} if {@code str} is {@code null}
     * @throws IllegalArgumentException if {@code str} is not valid Base64 (a character outside the
     *         Base64 alphabet, or malformed padding)
     * @throws UnsupportedOperationException if non-null text is supplied for a buffer class that neither a
     *         heap nor a direct buffer is an instance of
     * @see #valueOf(Object)
     * @see #stringOf(ByteBuffer)
     */
    @MayReturnNull
    @Override
    public ByteBuffer valueOf(final String str) throws IllegalArgumentException, UnsupportedOperationException {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.isEmpty()) {
            return wrap(N.EMPTY_BYTE_ARRAY);
        } else {
            return wrap(Strings.base64Decode(str));
        }
    }

    /**
     * Wraps raw bytes in a buffer of the handled class, with the position at the end of the data
     * (the convention shared by {@link #valueOf(byte[])} and {@link #byteArrayOf(ByteBuffer)}).
     *
     * <p>{@link #valueOf(byte[])} is {@code static} and can only ever build a heap buffer, so this instance
     * helper is what keeps a handler created for a {@code ByteBuffer} subclass from advertising a
     * {@link #javaType()} it never returns. The produced buffer is tested for assignability rather than the
     * target class being classified: on this JDK {@code DirectByteBuffer} extends {@code MappedByteBuffer},
     * so the direct allocation satisfies both.</p>
     *
     * @param bytes the content to wrap; must not be {@code null}
     * @return a buffer of the handled class holding {@code bytes}, positioned at {@code bytes.length}
     * @throws UnsupportedOperationException if the handled buffer class is neither a heap nor a direct buffer class
     */
    private ByteBuffer wrap(final byte[] bytes) throws UnsupportedOperationException {
        final ByteBuffer heapBuffer = ByteBufferType.valueOf(bytes);

        if (typeClass.isInstance(heapBuffer)) {
            return heapBuffer;
        }

        final ByteBuffer directBuffer = ByteBuffer.allocateDirect(bytes.length);
        directBuffer.put(bytes);

        if (typeClass.isInstance(directBuffer)) {
            return directBuffer;
        }

        throw new UnsupportedOperationException("Content construction is not supported for byte buffer class: " + typeClass.getName());
    }

    /**
     * Converts an arbitrary object to a {@link java.nio.ByteBuffer}.
     * A {@code byte[]} is wrapped directly in a buffer of the handled class (sharing the array when that
     * class is satisfied by a heap buffer, with the position at {@code bytes.length}); every other object is
     * converted through its own type's string form and then {@link #valueOf(String)}, exactly as the
     * inherited default does. Both routes therefore produce an instance of {@link #javaType()}.
     *
     * @param obj the object to convert; may be {@code null}
     * @return a buffer holding the byte array, the result of {@link #valueOf(String)} for any other
     *         object, or {@code null} if {@code obj} is {@code null}
     * @throws IllegalArgumentException if the string form of {@code obj} is not valid Base64
     * @throws UnsupportedOperationException if content is supplied for a buffer class that neither a heap
     *         nor a direct buffer is an instance of
     * @see #valueOf(byte[])
     * @see #valueOf(String)
     */
    @MayReturnNull
    @Override
    public ByteBuffer valueOf(final Object obj) throws IllegalArgumentException, UnsupportedOperationException {
        // The inherited default would render a byte[] as its list text ("[1, 2, 3]") and then try to
        // Base64-decode that text.
        if (obj instanceof byte[] bytes) {
            return wrap(bytes);
        }

        return super.valueOf(obj);
    }

    /**
     * Extracts the written content of a {@link java.nio.ByteBuffer} as a byte array.
     * Copies bytes from index {@code 0} up to (but not including) the buffer's current
     * {@link java.nio.ByteBuffer#position() position}. The copy is read through a duplicate, so
     * the original buffer's position, limit, and mark are not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteBuffer buf = ByteBuffer.allocate(10);
     * buf.put((byte) 10).put((byte) 20);                // position is now 2
     * byte[] bytes = ByteBufferType.byteArrayOf(buf);   // returns [10, 20]; buf.position() still 2
     * }</pre>
     *
     * @param x the {@code ByteBuffer} to extract bytes from; must not be {@code null}
     * @return a new byte array containing the buffer's written bytes (indices {@code 0..position-1})
     * @throws IllegalArgumentException if {@code x} is {@code null}
     */
    public static byte[] byteArrayOf(final ByteBuffer x) throws IllegalArgumentException {
        N.checkArgNotNull(x, cs.x);
        final ByteBuffer duplicate = x.duplicate();
        final byte[] bytes = new byte[duplicate.position()];

        duplicate.position(0);
        duplicate.get(bytes);

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
     * <p>This is a {@code static} utility and therefore always produces a plain heap buffer. A handler bound
     * to a {@code ByteBuffer} subclass builds an instance of that subclass instead; use its
     * {@link #valueOf(Object)} for that.</p>
     *
     * @param bytes the byte array to wrap; must not be {@code null}
     * @return a {@code ByteBuffer} wrapping {@code bytes} with position at {@code bytes.length}
     * @throws IllegalArgumentException if {@code bytes} is {@code null}
     */
    public static ByteBuffer valueOf(final byte[] bytes) throws IllegalArgumentException {
        N.checkArgNotNull(bytes, cs.bytes);
        return ByteBuffer.wrap(bytes, bytes.length, 0);
    }
}
