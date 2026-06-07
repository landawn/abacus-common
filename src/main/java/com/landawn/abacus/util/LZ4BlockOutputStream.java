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

/**
 * An output stream that compresses data using the LZ4 block format.
 * This class wraps the net.jpountz.lz4.LZ4BlockOutputStream to provide
 * LZ4 compression capabilities with a consistent API.
 *
 * <p>LZ4 is a fast compression algorithm that provides a good balance between
 * compression ratio and speed. Data written to this stream is automatically
 * compressed and can be decompressed using LZ4BlockInputStream.</p>
 *
 * <p>The stream buffers data internally and compresses it in fixed-size blocks.
 * Calling {@link #close()} (for example, via try-with-resources) automatically
 * flushes and finishes the compressed stream; call {@link #finish()} explicitly
 * only when you want to finish the LZ4 stream without closing the underlying
 * output stream.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * try (FileOutputStream fileOut = new FileOutputStream("data.lz4");
 *      LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(fileOut)) {
 *     lz4Out.write(data);
 *     lz4Out.finish();   // Ensure all data is compressed and written
 * }
 * }</pre>
 *
 * @see LZ4BlockInputStream
 */
public final class LZ4BlockOutputStream extends OutputStream {

    private final net.jpountz.lz4.LZ4BlockOutputStream out;

    /**
     * Creates a new LZ4BlockOutputStream that will compress data and write it to
     * the specified output stream using the default block size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FileOutputStream fileOut = new FileOutputStream("compressed.lz4");
     * LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(fileOut);
     * }</pre>
     *
     * @param os the output stream to write compressed data to
     */
    public LZ4BlockOutputStream(final OutputStream os) {
        out = new net.jpountz.lz4.LZ4BlockOutputStream(os);
    }

    /**
     * Creates a new LZ4BlockOutputStream with a custom block size.
     *
     * <p>The block size is the maximum number of bytes buffered and compressed at
     * once. Larger blocks may achieve better compression ratios but use more
     * memory. It must be in the range 64 bytes to 32 MB (inclusive); values
     * outside this range cause an {@link IllegalArgumentException}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FileOutputStream fileOut = new FileOutputStream("compressed.lz4");
     * // Use 1MB blocks for a better compression ratio
     * LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(fileOut, 1024 * 1024);
     * }</pre>
     *
     * @param os the output stream to write compressed data to
     * @param blockSize the maximum number of bytes to compress at once, must be
     *        between 64 and 32 MB (inclusive)
     * @throws IllegalArgumentException if {@code blockSize} is less than 64 or
     *         greater than 32 MB
     */
    public LZ4BlockOutputStream(final OutputStream os, final int blockSize) {
        out = new net.jpountz.lz4.LZ4BlockOutputStream(os, blockSize);
    }

    /**
     * Writes the specified byte to this output stream.
     * The byte is buffered internally and will be compressed once a full block
     * has accumulated or when the stream is finished or closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * lz4Out.write(65);   // Write the byte 'A'
     * }</pre>
     *
     * @param b the byte to write (the 8 low-order bits are written)
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if this stream has already been finished or closed
     */
    @Override
    public void write(final int b) throws IOException {
        out.write(b);
    }

    /**
     * Writes all bytes from the specified byte array to this output stream.
     * The bytes are buffered internally and will be compressed as needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Hello, World!".getBytes();
     * lz4Out.write(data);
     * }</pre>
     *
     * @param b the byte array to write
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if {@code b} is {@code null}
     * @throws IllegalStateException if this stream has already been finished or closed
     */
    @Override
    public void write(final byte[] b) throws IOException {
        out.write(b);
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off
     * to this output stream. The bytes are buffered internally and will be
     * compressed as needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = inputStream.read(buffer);
     * lz4Out.write(buffer, 0, bytesRead);
     * }</pre>
     *
     * @param b the byte array containing the data to write
     * @param off the start offset in the data
     * @param len the number of bytes to write
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if {@code b} is {@code null}
     * @throws IllegalArgumentException if {@code len} is negative
     * @throws IndexOutOfBoundsException if {@code off} is negative or
     *         {@code off + len} is greater than {@code b.length}
     * @throws IllegalStateException if this stream has already been finished or closed
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
    }

    /**
     * Flushes the underlying output stream.
     *
     * <p>This implementation does not compress or write out a partially filled
     * block. Data that has been written but does not yet fill a complete block
     * remains buffered and is not compressed or made available to a reader until
     * {@link #finish()} or {@link #close()} is called. Calling this method only
     * forwards the flush to the underlying output stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * lz4Out.write(data);
     * lz4Out.flush();   // Flush underlying stream (data still buffered until finish())
     * }</pre>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Finishes writing compressed data to the output stream without closing the underlying stream.
     * Any buffered data is compressed and written, and the LZ4 end marker is emitted.
     *
     * <p>Use this method when the underlying stream must stay open after compression is complete.
     * It is not required before {@link #close()}, which finishes the stream automatically if it
     * has not already been finished.</p>
     *
     * <p>After calling finish(), no more data can be written to this stream, but the
     * underlying output stream remains open and can be used for other purposes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * lz4Out.write(data);
     * lz4Out.finish();   // Ensure all data is compressed
     * // The underlying stream is still open for other uses
     * }</pre>
     *
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if this stream has already been finished or closed
     */
    public void finish() throws IOException {
        out.finish();
    }

    /**
     * Closes this output stream and releases any system resources associated with it.
     * This method automatically calls {@link #finish()} if it hasn't been called already,
     * then closes the underlying output stream.
     *
     * <p>Once the stream has been closed, further {@link #write(int) write}
     * invocations throw an {@link IllegalStateException}. Calling
     * {@link #flush()} after the stream is closed has no effect.</p>
     *
     * <p>Closing a previously closed stream has no effect.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(new FileOutputStream("data.lz4"));
     * try {
     *     lz4Out.write(data);
     * } finally {
     *     lz4Out.close();   // Finishes and releases resources
     * }
     * }</pre>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        out.close();
    }
}
