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
 * An input stream that decompresses data in the block-stream format produced by
 * {@link LZ4BlockOutputStream}.
 * This class wraps the net.jpountz.lz4.LZ4BlockInputStream to provide
 * LZ4 decompression capabilities with a consistent API.
 *
 * <p>LZ4 is a fast compression algorithm that provides a good balance between
 * compression ratio and speed. This stream automatically decompresses data that
 * was compressed using {@code net.jpountz.lz4.LZ4BlockOutputStream}. This is that
 * library's custom streaming container, not the standardized LZ4 Frame format, so
 * generic {@code .lz4} tools are not necessarily interoperable with it.</p>
 *
 * <p>This class is not thread-safe and does not support mark/reset.</p>
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
     * try (FileInputStream fileIn = new FileInputStream("compressed.lz4");
     *      LZ4BlockInputStream lz4In = new LZ4BlockInputStream(fileIn)) {
     *     byte[] decompressed = lz4In.readAllBytes();
     * }
     * }</pre>
     *
     * @param is the input stream to read compressed data from; must not be {@code null}
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     */
    public LZ4BlockInputStream(final InputStream is) throws IllegalArgumentException {
        N.checkArgNotNull(is, cs.is);
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
     * @throws IOException if the compressed input is truncated or corrupt, or reading from the underlying stream fails
     */
    @Override
    public int read() throws IOException {
        return in.read();
    }

    /**
     * Reads up to b.length bytes of decompressed data from the input stream
     * into an array of bytes.
     *
     * <p>This method blocks until some input is available, unless {@code b.length} is zero. A valid
     * zero-length request returns zero without reading the underlying stream.</p>
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
     * @throws NullPointerException if {@code b} is {@code null}
     * @throws IOException if compressed input cannot be read or contains invalid LZ4 data
     */
    @Override
    public int read(final byte[] b) throws NullPointerException, IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to len bytes of decompressed data from the input stream into
     * an array of bytes, starting at the specified offset.
     *
     * <p>This method blocks until some input is available, unless {@code len} is zero. A valid
     * zero-length request returns zero without reading the underlying stream. The buffer and the whole
     * {@code (off, len)} range are validated before anything is read.</p>
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
     * @throws NullPointerException if {@code b} is {@code null}
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative,
     *         or {@code off + len} is greater than {@code b.length}
     * @throws IOException if the compressed input is truncated or corrupt, or reading from the underlying stream fails
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws NullPointerException, IndexOutOfBoundsException, IOException {
        // Validate the whole range here rather than only the len == 0 branch: the delegate reports a negative
        // length as IllegalArgumentException, which contradicts the IndexOutOfBoundsException this method
        // documents (and, for a null buffer with a negative length, that IAE fired before the NPE).
        if (b == null) {
            throw new NullPointerException("b");
        }

        // Subtraction, not off + len, so a length near Integer.MAX_VALUE cannot overflow into a passing check.
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException("off: " + off + ", len: " + len + ", length: " + b.length);
        }

        if (len == 0) {
            // The decoder may refill even for a zero-byte request; do not delegate it.
            return 0;
        }

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
     * @return the actual number of bytes skipped; 0 if the end of the stream has already been reached
     * @throws IllegalArgumentException if {@code n} is negative.
     * @throws IOException if the compressed input is truncated or corrupt, or reading from the underlying stream fails
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
     * <p>The estimate is the number of bytes left in the current decompressed block, so it is
     * {@code 0} on a fresh stream (nothing has been decompressed yet) and {@code 0} once the end
     * of the stream has been reached.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // available() is 0 until a read has decompressed a block, so read first and then size the
     * // next buffer from what is left of that block.
     * int first = lz4In.read();
     * if (first >= 0) {
     *     byte[] buffer = new byte[lz4In.available()];
     *     lz4In.read(buffer);
     * }
     * }</pre>
     *
     * @return an estimate of the number of bytes that can be read without blocking; never negative
     * @throws IOException retained by the decoder contract; the current decoder implementation only reads its buffer counters
     */
    @Override
    public int available() throws IOException {
        // Clamp: the delegate returns its unclamped originalLen - o, and the end-of-stream marker zeroes
        // originalLen while o still holds the last block's length, so it reports -(last block size) at EOF.
        // InputStream.available() specifies 0 at end of stream, and a negative value breaks callers -
        // new byte[available()] throws NegativeArraySizeException and BufferedInputStream.available()
        // overflows to Integer.MAX_VALUE.
        return Math.max(0, in.available());
    }

    /**
     * Marks the current position in this input stream.
     *
     * <p>The underlying LZ4 block stream does not support mark/reset, so this call has no useful
     * effect and a subsequent {@link #reset()} fails.</p>
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
     * <p>The underlying LZ4 block stream does not support mark/reset; this method throws an
     * {@link IOException}.</p>
     *
     * @throws IOException always, because the LZ4 decoder does not support mark/reset
     * @see #mark(int)
     * @see #markSupported()
     */
    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    /**
     * Tests if this input stream supports the {@code mark} and {@code reset} methods.
     *
     * @return {@code false}; mark/reset is not supported
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
     * @throws IOException if closing the LZ4 decoder or its underlying input stream fails
     */
    @Override
    public void close() throws IOException {
        in.close();
    }
}
