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
 * An input stream that decompresses data in the LZ4 block format.
 * This class wraps the net.jpountz.lz4.LZ4BlockInputStream to provide
 * LZ4 decompression capabilities with a consistent API.
 *
 * <p>LZ4 is a fast compression algorithm that provides a good balance between
 * compression ratio and speed. This stream automatically decompresses data that
 * was compressed using LZ4BlockOutputStream or compatible LZ4 block format.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * try (InputStream compressed = new FileInputStream("data.lz4");
 *      LZ4BlockInputStream lz4In = new LZ4BlockInputStream(compressed)) {
 *     byte[] buffer = new byte[1024];
 *     int bytesRead;
 *     while ((bytesRead = lz4In.read(buffer)) != -1) {
 *         // Process decompressed data
 *     }
 * }
 * }</pre>
 *
 * @see LZ4BlockOutputStream
 */
public final class LZ4BlockInputStream extends InputStream {

    private final net.jpountz.lz4.LZ4BlockInputStream in;

    /**
     * Creates a new LZ4BlockInputStream that will decompress data from the specified input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FileInputStream fileIn = new FileInputStream("compressed.lz4");
     * LZ4BlockInputStream lz4In = new LZ4BlockInputStream(fileIn);
     * }</pre>
     *
     * @param is the input stream to read compressed data from
     */
    public LZ4BlockInputStream(final InputStream is) {
        in = new net.jpountz.lz4.LZ4BlockInputStream(is);
    }

    /**
     * Reads the next byte of decompressed data from the input stream.
     * The value byte is returned as an int in the range 0 to 255.
     * If no byte is available because the end of the stream has been reached,
     * the value -1 is returned.
     *
     * <p>This method blocks until input data is available, the end of the stream
     * is detected, or an exception is thrown.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int b = lz4In.read();
     * if (b != -1) {
     *     byte data = (byte) b;
     *     // Process the byte
     * }
     * }</pre>
     *
     * @return the next byte of data, or -1 if the end of the stream is reached
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        return in.read();
    }

    /**
     * Reads up to b.length bytes of decompressed data from the input stream
     * into an array of bytes.
     *
     * <p>This method blocks until some input is available.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = lz4In.read(buffer);
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
     * Reads up to len bytes of decompressed data from the input stream into
     * an array of bytes, starting at the specified offset.
     *
     * <p>This method blocks until some input is available.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = lz4In.read(buffer, 0, 512);   // Read up to 512 bytes
     * }</pre>
     *
     * @param b the buffer into which the data is read
     * @param off the start offset in the buffer at which the data is written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data
     *         because the end of the stream has been reached
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return in.read(b, off, len);
    }

    /**
     * Skips over and discards up to {@code n} bytes of decompressed data from this input stream.
     * Fewer than {@code n} bytes may be skipped, for example because fewer bytes remain in the
     * current decompressed block. The actual number of bytes skipped is returned.
     *
     * <p>This method never skips more than {@code n} bytes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long skipped = lz4In.skip(1024);   // Try to skip 1024 bytes
     * }</pre>
     *
     * @param n the number of bytes to skip
     * @return the actual number of bytes skipped, or -1 if the end of the stream has already been reached
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int n = lz4In.available();
     * if (n > 0) {
     *     byte[] buffer = new byte[n];
     *     lz4In.read(buffer);
     * }
     * }</pre>
     *
     * @return an estimate of the number of bytes that can be read without blocking
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        return in.available();
    }

    /**
     * Marks the current position in this input stream.
     *
     * <p>The underlying LZ4 block input stream does not support mark/reset,
     * so this method has no effect.</p>
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
     * <p>The underlying LZ4 block input stream does not support mark/reset,
     * so this method always throws an {@link IOException}.</p>
     *
     * @throws IOException always, because the underlying LZ4 block input stream does not support mark/reset
     * @see #mark(int)
     * @see #markSupported()
     */
    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    /**
     * Tests if this input stream supports the {@code mark} and {@code reset} methods.
     * The underlying LZ4 block input stream does not support mark/reset, so this
     * method returns {@code false}.
     *
     * @return {@code false}, because the underlying LZ4 block input stream does not support mark/reset
     * @see #mark(int)
     * @see #reset()
     */
    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream,
     * including the underlying input stream. Read operations attempted after the stream has
     * been closed typically fail with an {@link IOException}.
     *
     * <p>Closing a previously closed stream has no effect.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LZ4BlockInputStream lz4In = new LZ4BlockInputStream(compressedStream);
     * try {
     *     // Read decompressed data
     * } finally {
     *     lz4In.close();
     * }
     * }</pre>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        in.close();
    }
}
