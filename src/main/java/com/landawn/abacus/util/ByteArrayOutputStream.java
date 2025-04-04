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

public final class ByteArrayOutputStream extends OutputStream {

    byte[] buf;

    int count;

    public ByteArrayOutputStream() {
        this(32);
    }

    /**
     *
     * @param initCapacity
     */
    public ByteArrayOutputStream(final int initCapacity) {
        if (initCapacity < 0) {
            throw new IllegalArgumentException("Negative initial size: " + initCapacity);
        }

        buf = new byte[initCapacity];
    }

    /**
     *
     * @param b
     */
    @Override
    public void write(final int b) {
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }

    /**
     *
     * @param b
     * @param off
     * @param len
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
     *
     * @param b
     */
    public void write(final byte b) {
        ensureCapacity(count + 1);
        buf[count] = b;
        count += 1;
    }

    /**
     *
     * @param out
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeTo(final OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    public int capacity() {
        return buf == null ? 0 : buf.length;
    }

    public byte[] array() {
        return buf;
    }

    public int size() {
        return count;
    }

    /**
     * Reset.
     */
    public void reset() {
        count = 0;
    }

    /**
     * To byte array.
     *
     * @return
     */
    public byte[] toByteArray() {
        return Arrays.copyOf(buf, count);
    }

    @Override
    public String toString() {
        return new String(buf, 0, count); // NOSONAR
    }

    /**
     *
     * @param charsetName
     * @return
     * @throws UnsupportedEncodingException If the named charset is not supported
     */
    public String toString(final String charsetName) throws UnsupportedEncodingException {
        return new String(buf, 0, count, charsetName);
    }

    /**
     *
     * @param charset
     * @return
     */
    public String toString(final Charset charset) {
        return new String(buf, 0, count, charset);
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("RedundantThrows")
    @Override
    public void close() throws IOException {
        // Do nothing.
    }

    private void ensureCapacity(final int minCapacity) {
        if (minCapacity > N.MAX_ARRAY_SIZE || minCapacity < 0) {
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
