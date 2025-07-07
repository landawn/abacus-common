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
import java.io.InputStream;

/**
 * A wrapper class for Snappy-compressed input streams that provides transparent decompression.
 * This class extends {@link InputStream} and delegates all operations to the underlying
 * Xerial Snappy implementation while providing a consistent API.
 * 
 * <p>Snappy is a fast compression/decompression algorithm developed by Google, optimized for
 * speed rather than compression ratio. This input stream automatically decompresses data
 * that was compressed using Snappy compression.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * try (FileInputStream fis = new FileInputStream("data.snappy");
 *      SnappyInputStream sis = new SnappyInputStream(fis)) {
 *     byte[] buffer = new byte[1024];
 *     int bytesRead;
 *     while ((bytesRead = sis.read(buffer)) != -1) {
 *         // Process decompressed data
 *     }
 * }
 * }</pre>
 * 
 * @see SnappyOutputStream
 * @see org.xerial.snappy.SnappyInputStream
 */
public final class SnappyInputStream extends InputStream {

    private final org.xerial.snappy.SnappyInputStream in;

    /**
     * Creates a new SnappyInputStream that decompresses data from the specified input stream.
     * The input stream should contain data that was compressed using Snappy compression.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * InputStream compressedStream = new FileInputStream("compressed.snappy");
     * SnappyInputStream snappyStream = new SnappyInputStream(compressedStream);
     * }</pre>
     *
     * @param is the input stream containing Snappy-compressed data
     * @throws IOException if an I/O error occurs during initialization
     */
    public SnappyInputStream(final InputStream is) throws IOException {
        in = new org.xerial.snappy.SnappyInputStream(is);
    }

    /**
     * Reads the next byte of decompressed data from this input stream.
     * The value byte is returned as an {@code int} in the range 0 to 255.
     * If no byte is available because the end of the stream has been reached,
     * the value -1 is returned.
     * 
     * <p>This method blocks until input data is available, the end of the stream
     * is detected, or an exception is thrown.</p>
     *
     * @return the next byte of data, or -1 if the end of the stream is reached
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        return in.read();
    }

    /**
     * Reads up to {@code b.length} bytes of decompressed data from this input stream
     * into an array of bytes. This method blocks until some input is available.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = snappyStream.read(buffer);
     * }</pre>
     *
     * @param b the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data
     *         because the end of the stream has been reached
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return in.read(b);
    }

    /**
     * Reads up to {@code len} bytes of decompressed data from this input stream
     * into an array of bytes, starting at the specified offset.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = snappyStream.read(buffer, 10, 100); // Read up to 100 bytes starting at index 10
     * }</pre>
     *
     * @param b the buffer into which the data is read
     * @param off the start offset in the destination array {@code b}
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data
     *         because the end of the stream has been reached
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if {@code off} is negative, {@code len} is negative,
     *         or {@code len} is greater than {@code b.length - off}
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return in.read(b, off, len);
    }

    /**
     * Skips over and discards {@code n} bytes of decompressed data from this input stream.
     * The {@code skip} method may, for a variety of reasons, end up skipping over some
     * smaller number of bytes, possibly 0. The actual number of bytes skipped is returned.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * long skipped = snappyStream.skip(1024); // Try to skip 1024 bytes
     * }</pre>
     *
     * @param n the number of bytes to be skipped
     * @return the actual number of bytes skipped
     * @throws IllegalArgumentException if {@code n} is negative
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long skip(final long n) throws IllegalArgumentException, IOException {
        N.checkArgNotNegative(n, cs.n);

        return in.skip(n);
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or skipped over)
     * from this input stream without blocking by the next invocation of a method
     * for this input stream.
     * 
     * <p>Note that this method provides only an estimate; the actual number of bytes
     * that can be read without blocking may be more or less than the returned value.</p>
     *
     * @return an estimate of the number of bytes that can be read without blocking
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        return in.available();
    }

    /**
     * Marks the current position in this input stream. A subsequent call to
     * the {@code reset} method repositions this stream at the last marked position
     * so that subsequent reads re-read the same bytes.
     * 
     * <p>The {@code readLimit} argument tells this input stream to allow that many
     * bytes to be read before the mark position gets invalidated.</p>
     *
     * @param readLimit the maximum limit of bytes that can be read before the mark position becomes invalid
     * @see #reset()
     * @see #markSupported()
     */
    @Override
    public synchronized void mark(final int readLimit) {
        in.mark(readLimit);
    }

    /**
     * Repositions this stream to the position at the time the {@code mark} method
     * was last called on this input stream.
     * 
     * <p>Stream marks are intended to be used in situations where you need to read
     * ahead a little to see what's in the stream.</p>
     *
     * @throws IOException if this stream has not been marked or if the mark has been invalidated
     * @see #mark(int)
     * @see #markSupported()
     */
    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    /**
     * Tests if this input stream supports the {@code mark} and {@code reset} methods.
     * The {@code markSupported} method of {@code SnappyInputStream} returns the result
     * of calling {@code markSupported} on the underlying Snappy input stream.
     *
     * @return {@code true} if this stream instance supports the mark and reset methods;
     *         {@code false} otherwise
     * @see #mark(int)
     * @see #reset()
     */
    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream.
     * Once the stream has been closed, further read(), available(), reset(), or skip()
     * invocations will throw an IOException.
     * 
     * <p>Closing a previously closed stream has no effect.</p>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        in.close();
    }
}