/*
 * Copyright (C) 2022 HaiYang Li
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
 * An InputStream that decompresses data in the Brotli compression format.
 * This class wraps the org.brotli.dec.BrotliInputStream to provide seamless
 * decompression of Brotli-compressed data streams.
 *
 * <p>Brotli is a generic-purpose lossless compression algorithm that compresses
 * data using a combination of a modern variant of the LZ77 algorithm, Huffman
 * coding and 2nd order context modeling.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * try (InputStream compressed = new FileInputStream("data.br");
 *      BrotliInputStream decompressed = new BrotliInputStream(compressed)) {
 *     byte[] buffer = new byte[1024];
 *     int bytesRead;
 *     while ((bytesRead = decompressed.read(buffer)) != -1) {
 *         // Process decompressed data
 *     }
 * }
 * }</pre>
 *
 * @see java.io.InputStream
 */
public final class BrotliInputStream extends InputStream {

    private final org.brotli.dec.BrotliInputStream in;

    // Byte-at-a-time reads are buffered HERE rather than by the decoder, because only one of the two entry
    // points may buffer: the decoder's own read() fills a private buffer, and its read(byte[], int, int)
    // copies the leftover of that buffer into the caller's array and then still returns -1 whenever the
    // same call has to decode more and the stream is already finished - silently losing the bytes it just
    // copied. Owning the buffer here keeps read() cheap while guaranteeing that a partially drained buffer
    // is never reported as end-of-stream.
    private final byte[] buf;

    private int bufOff;

    private int bufLen;

    /**
     * Creates a new BrotliInputStream that decompresses data from the specified source stream.
     * Uses the default internal buffer size for reading.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream source = new FileInputStream("compressed.br");
     *      BrotliInputStream brotli = new BrotliInputStream(source)) {
     *     // Read decompressed data.
     * }
     * }</pre>
     *
     * @param source the input stream containing Brotli-compressed data
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws IOException if initializing the Brotli decoder cannot read a valid header from the source
     */
    public BrotliInputStream(final InputStream source) throws IllegalArgumentException, IOException {
        this(source, org.brotli.dec.BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE);
    }

    /**
     * Creates a new BrotliInputStream with a specified internal buffer size.
     * A larger buffer size may improve performance when reading large amounts of data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use 64KB buffer for better performance
     * try (InputStream source = new FileInputStream("large-file.br");
     *      BrotliInputStream brotli = new BrotliInputStream(source, 65536)) {
     *     // Read decompressed data.
     * }
     * }</pre>
     *
     * @param source the input stream containing Brotli-compressed data
     * @param byteReadBufferSize the size of the internal buffer for reading, in bytes
     * @throws IllegalArgumentException if {@code byteReadBufferSize} is not positive, or if {@code source} is
     *         {@code null}.
     * @throws IOException if initializing the Brotli decoder cannot read a valid header from the source
     */
    public BrotliInputStream(final InputStream source, final int byteReadBufferSize) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgPositive(byteReadBufferSize, cs.byteReadBufferSize);

        // The decoder's own buffer is only ever read by its own read(), which this class never calls, so it
        // is kept at the minimum and the buffering happens in buf instead (see the field comment above).
        in = new org.brotli.dec.BrotliInputStream(source, 1);
        buf = new byte[byteReadBufferSize];
    }

    /**
     * Reads the next byte of decompressed data from the input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int b = brotliStream.read();
     * if (b != -1) {
     *     byte data = (byte) b;
     *     // Process the byte
     * }
     * }</pre>
     *
     * @return the next byte of decompressed data, or -1 if the end of the stream is reached
     * @throws IllegalStateException if this stream has been closed and more data must be decoded
     * @throws IOException if compressed input cannot be read or contains invalid Brotli data
     */
    @Override
    public int read() throws IllegalStateException, IOException {
        if (bufOff >= bufLen) {
            bufLen = in.read(buf, 0, buf.length);
            bufOff = 0;

            if (bufLen <= 0) {
                return -1;
            }
        }

        return buf[bufOff++] & 0xFF;
    }

    /**
     * Reads decompressed data into an array of bytes.
     * This method will block until some input is available, an I/O error occurs,
     * or the end of the stream is reached.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = brotliStream.read(buffer);
     * }</pre>
     *
     * @param b the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data
     * @throws NullPointerException if {@code b} is {@code null}
     * @throws IllegalStateException if this stream has been closed and no buffered byte remains to satisfy the
     *         request; when some remain, they are transferred and the (possibly short) count is returned instead
     * @throws IOException if compressed input cannot be read or contains invalid Brotli data
     */
    @Override
    public int read(final byte[] b) throws NullPointerException, IllegalStateException, IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to len bytes of decompressed data into an array of bytes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = brotliStream.read(buffer, 0, 512);   // Read up to 512 bytes
     * }</pre>
     *
     * @param b the buffer into which the data is read
     * @param off the start offset in array b at which the data is written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data
     * @throws NullPointerException if {@code b} is {@code null}
     * @throws IndexOutOfBoundsException if off is negative, len is negative, or len is greater than b.length - off
     * @throws IllegalStateException if this stream has been closed and no buffered byte remains to satisfy the
     *         request; when some remain, they are transferred and the (possibly short) count is returned instead
     * @throws IOException if compressed input cannot be read or contains invalid Brotli data
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws NullPointerException, IndexOutOfBoundsException, IllegalStateException, IOException {
        // Enforce InputStream.read(byte[], int, int) contract: bad offset/length must throw
        // IndexOutOfBoundsException. The underlying org.brotli.dec.BrotliInputStream throws
        // IllegalArgumentException, so we validate first to surface the correct exception type.
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException("off=" + off + ", len=" + len + ", b.length=" + b.length);
        }

        if (len == 0) {
            return 0;
        }

        int copied = 0;

        if (bufOff < bufLen) {
            copied = Math.min(bufLen - bufOff, len);
            System.arraycopy(buf, bufOff, b, off, copied);
            bufOff += copied;

            if (copied == len) {
                return copied;
            }
        }

        final int n;

        if (copied > 0) {
            try {
                n = in.read(b, off + copied, len - copied);
            } catch (final IllegalStateException e) {
                // The decoder rejects decoding after close with an IllegalStateException (BrotliRuntimeException,
                // which signals corruption, is a plain RuntimeException and is deliberately NOT caught here). Bytes
                // already written into the caller's array must be reported rather than lost: propagating would leave
                // the caller unable to learn how much of `b` is valid. The state is unchanged, so the next call -
                // which finds the buffer empty and goes straight to the decoder - raises the same exception.
                return copied;
            }
        } else {
            n = in.read(b, off + copied, len - copied);
        }

        // -1 only when no byte at all was transferred; bytes already copied out of buf must be reported.
        return n <= 0 ? (copied > 0 ? copied : -1) : copied + n;
    }

    /**
     * Skips over and discards n bytes of decompressed data from this input stream.
     * The skip method may skip fewer bytes than requested.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long skipped = brotliStream.skip(1024);   // Try to skip 1KB
     * System.out.println("Actually skipped: " + skipped + " bytes");
     * }</pre>
     *
     * @param n the number of bytes to be skipped
     * @return the actual number of bytes skipped
     * @throws IllegalArgumentException if n is negative.
     * @throws IllegalStateException if this stream has been closed and no buffered byte remains to skip; when some
     *         remain, they are skipped and the (possibly short) count is returned instead
     * @throws IOException if compressed input cannot be read or decoded while skipping
     */
    @Override
    public long skip(final long n) throws IllegalArgumentException, IllegalStateException, IOException {
        N.checkArgNotNegative(n, cs.n);

        // Bytes still sitting in buf have already been consumed from the source, so they must be counted as
        // skipped here; delegating the whole request would discard them without reporting them.
        final long fromBuffer = Math.min(Math.max(bufLen - bufOff, 0), n);
        bufOff += (int) fromBuffer;

        if (fromBuffer == n) {
            return n;
        }

        if (fromBuffer > 0) {
            try {
                return fromBuffer + in.skip(n - fromBuffer);
            } catch (final IllegalStateException e) {
                // Same invariant as read(byte[], int, int): bufOff has already been advanced past these bytes, so
                // they are consumed. Unlike a read there is no caller array to inspect afterwards, so failing to
                // report them would discard them without a trace. The next call raises the same exception.
                return fromBuffer;
            }
        }

        return in.skip(n);
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or skipped over)
     * from this input stream without blocking by the next invocation of a method
     * for this input stream. The Brotli decoder does not override {@code available()}, so this method
     * always returns {@code 0}, even when decoded bytes are already buffered and could be read
     * immediately. A {@code 0} result therefore does not mean the end of the stream has been reached.
     *
     * @return an estimate of the number of bytes that can be read without blocking
     * @throws IOException retained by the {@link InputStream} contract; the current decoder implementation returns zero without performing I/O
     */
    @Override
    public int available() throws IOException {
        return in.available();
    }

    /**
     * Marks the current position in this input stream.
     * This stream does not support mark/reset: the Brotli decoder does not override
     * {@code mark}, so this call is always a no-op and {@link #markSupported()} always
     * returns {@code false}, whatever the source stream supports. {@link #reset()} always
     * throws {@link IOException}.
     *
     * @param readLimit the maximum limit of bytes that can be read before the mark position becomes invalid (ignored)
     */
    @Override
    public synchronized void mark(final int readLimit) {
        in.mark(readLimit);
    }

    /**
     * Always throws {@link IOException}: this stream does not support mark/reset (see {@link #mark(int)}).
     *
     * @throws IOException always; mark/reset is not supported by the Brotli decoder
     */
    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    /**
     * Tests if this input stream supports the mark and reset methods.
     *
     * @return always {@code false}; the Brotli decoder does not support mark/reset.
     */
    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream.
     * After close, the bytes this stream had already decoded and buffered are still served: {@link #read()}
     * returns them one at a time, and {@link #skip(long)} and an array read report however many of them they
     * could transfer, even when that is fewer than requested. Only a call that can transfer nothing at all
     * throws {@link IllegalStateException} - an unchecked exception, not an {@link IOException} - because the
     * decoder rejects any further decoding after close. No buffered byte is ever consumed without being
     * reported. A zero-length read still returns zero. Callers must not rely on reading after close; closing
     * twice is harmless, and reset() continues to throw IOException because mark/reset is unsupported.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BrotliInputStream brotliStream = new BrotliInputStream(inputStream);
     * try {
     *     // Use the stream
     * } finally {
     *     brotliStream.close();
     * }
     * }</pre>
     *
     * @throws IOException if closing the decoder or its underlying input stream fails
     */
    @Override
    public void close() throws IOException {
        in.close();
    }
}
