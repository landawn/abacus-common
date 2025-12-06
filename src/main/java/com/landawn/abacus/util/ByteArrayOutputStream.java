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

package com.landawn.abacus.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * A high-performance implementation of ByteArrayOutputStream that provides direct access
 * to the internal byte array buffer for efficiency.
 * 
 * <p>This class is similar to {@link java.io.ByteArrayOutputStream} but provides
 * additional methods for performance optimization, such as direct access to the
 * internal array via {@link #array()} and capacity management.</p>
 * 
 * <p>Unlike the standard implementation, this class is not thread-safe. If multiple
 * threads access an instance concurrently, external synchronization is required.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ByteArrayOutputStream baos = new ByteArrayOutputStream();
 * baos.write("Hello".getBytes());
 * baos.write((byte) ' ');
 * baos.write("World".getBytes());
 * String result = baos.toString();   // "Hello World"
 * }</pre>
 * 
 * @see java.io.ByteArrayOutputStream
 */
public final class ByteArrayOutputStream extends OutputStream {

    byte[] buf;

    int count;

    /**
     * Creates a new ByteArrayOutputStream with a default initial capacity of 32 bytes.
     * 
     * <p>The buffer will grow automatically as needed when data is written.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * }</pre>
     */
    public ByteArrayOutputStream() {
        this(32);
    }

    /**
     * Creates a new ByteArrayOutputStream with the specified initial capacity.
     * 
     * <p>The buffer will grow automatically as needed when data is written
     * beyond the initial capacity.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
     * // Creates a stream with 1KB initial buffer
     * }</pre>
     *
     * @param initCapacity the initial capacity of the buffer
     * @throws IllegalArgumentException if initCapacity is negative
     */
    public ByteArrayOutputStream(final int initCapacity) {
        if (initCapacity < 0) {
            throw new IllegalArgumentException("Negative initial size: " + initCapacity);
        }

        buf = new byte[initCapacity];
    }

    /**
     * Writes the specified byte to this output stream.
     * 
     * <p>The byte is written as the low-order byte of the argument b.
     * The 24 high-order bits of b are ignored.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * baos.write(65);     // Writes 'A'
     * baos.write(0x42);   // Writes 'B'
     * }</pre>
     *
     * @param b the byte to write (as an int)
     */
    @Override
    public void write(final int b) {
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }

    /**
     * Writes a portion of the specified byte array to this output stream.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * byte[] data = "Hello World".getBytes();
     * baos.write(data, 6, 5);   // Writes "World"
     * }</pre>
     *
     * @param b the byte array containing data to write
     * @param off the start offset in the data
     * @param len the number of bytes to write
     * @throws IndexOutOfBoundsException if off is negative, len is negative,
     *         or off+len is greater than the length of the array b
     */
    @Override
    public void write(final byte[] b, final int off, final int len) {
        if ((off < 0) || (off > b.length) || (len < 0) || (((off + len) - b.length) > 0)) {
            throw new IndexOutOfBoundsException();
        }

        ensureCapacity(count + len);
        N.copy(b, off, buf, count, len);
        count += len;
    }

    /**
     * Writes a single byte to this output stream.
     * 
     * <p>This method provides a more type-safe alternative to {@link #write(int)}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * baos.write((byte) 0xFF);
     * }</pre>
     *
     * @param b the byte to write
     */
    public void write(final byte b) {
        ensureCapacity(count + 1);
        buf[count] = b;
        count += 1;
    }

    /**
     * Writes the complete contents of this ByteArrayOutputStream to another output stream.
     * 
     * <p>This method writes all bytes from the internal buffer up to the current
     * position to the specified output stream.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * baos.write("Hello".getBytes());
     * 
     * FileOutputStream fos = new FileOutputStream("output.txt");
     * baos.writeTo(fos);   // Writes "Hello" to the file
     * fos.close();
     * }</pre>
     *
     * @param out the output stream to write to
     * @throws IOException if an I/O error occurs
     */
    public void writeTo(final OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    /**
     * Returns the current capacity of the internal buffer.
     * 
     * <p>The capacity is the size of the internal array and may be larger
     * than the current size of valid data.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream(100);
     * System.out.println(baos.capacity());   // 100
     * baos.write(new byte[50]);
     * System.out.println(baos.capacity());   // Still 100
     * }</pre>
     * 
     * @return the current capacity, or 0 if the buffer is null
     */
    public int capacity() {
        return buf == null ? 0 : buf.length;
    }

    /**
     * Returns a reference to the internal byte array buffer.
     * 
     * <p>This method provides direct access to the internal buffer for performance
     * reasons. The returned array should not be modified unless you understand
     * the implications. Only bytes from index 0 to {@link #size()} - 1 contain
     * valid data.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * baos.write("Hello".getBytes());
     * byte[] buffer = baos.array();
     * int validBytes = baos.size();   // Use only buffer[0] to buffer[validBytes-1]
     * }</pre>
     * 
     * @return the internal byte array buffer
     */
    public byte[] array() {
        return buf;
    }

    /**
     * Returns the current size (number of valid bytes) in the buffer.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * System.out.println(baos.size());   // 0
     * baos.write("Hello".getBytes());
     * System.out.println(baos.size());   // 5
     * }</pre>
     * 
     * @return the number of valid bytes in the buffer
     */
    public int size() {
        return count;
    }

    /**
     * Resets this stream to the beginning.
     * 
     * <p>After reset, the size becomes 0, but the internal buffer capacity
     * remains unchanged. This allows reuse of the buffer without reallocation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * baos.write("Hello".getBytes());
     * System.out.println(baos.size());   // 5
     * baos.reset();
     * System.out.println(baos.size());   // 0
     * }</pre>
     */
    public void reset() {
        count = 0;
    }

    /**
     * Creates a new byte array containing a copy of the valid data.
     * 
     * <p>This method creates a new array of the exact size needed and copies
     * the valid data into it. The internal buffer remains unchanged.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * baos.write("Hello".getBytes());
     * byte[] data = baos.toByteArray();   // Returns new array with "Hello"
     * }</pre>
     * 
     * @return a new byte array containing the valid data
     */
    public byte[] toByteArray() {
        return Arrays.copyOf(buf, count);
    }

    /**
     * Converts the buffer contents to a string using the platform's default charset.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * baos.write("Hello World".getBytes());
     * String result = baos.toString();   // "Hello World"
     * }</pre>
     * 
     * @return a String decoded from the buffer contents
     */
    @Override
    public String toString() {
        return new String(buf, 0, count);   // NOSONAR
    }

    /**
     * Converts the buffer contents to a string using the specified charset.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * baos.write("Hello".getBytes("UTF-8"));
     * String result = baos.toString("UTF-8");   // "Hello"
     * }</pre>
     *
     * @param charsetName the name of the charset to use for decoding
     * @return a String decoded from the buffer contents
     * @throws UnsupportedEncodingException if the named charset is not supported
     */
    public String toString(final String charsetName) throws UnsupportedEncodingException {
        return new String(buf, 0, count, charsetName);
    }

    /**
     * Converts the buffer contents to a string using the specified charset.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * baos.write("Hello".getBytes(StandardCharsets.UTF_8));
     * String result = baos.toString(StandardCharsets.UTF_8);   // "Hello"
     * }</pre>
     *
     * @param charset the charset to use for decoding
     * @return a String decoded from the buffer contents
     */
    public String toString(final Charset charset) {
        return new String(buf, 0, count, charset);
    }

    /**
     * Closing a ByteArrayOutputStream has no effect.
     * 
     * <p>The methods in this class can be called after the stream has been closed
     * without generating an IOException.</p>
     *
     * @throws IOException never thrown, but declared for OutputStream compatibility
     */
    @SuppressWarnings("RedundantThrows")
    @Override
    public void close() throws IOException {
        // Do nothing.
    }

    private void ensureCapacity(final int minCapacity) {
        if (minCapacity < 0 || minCapacity > N.MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError();
        }

        if ((minCapacity - buf.length) > 0) {
            int newCapacity = (int) (buf.length * 1.75);

            if (newCapacity < 0 || newCapacity > N.MAX_ARRAY_SIZE) {
                newCapacity = N.MAX_ARRAY_SIZE;
            }

            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }

            buf = Arrays.copyOf(buf, newCapacity);
        }
    }
}
