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
 * A compression output stream that uses the Snappy compression algorithm.
 * This class wraps the Xerial Snappy library's SnappyOutputStream to provide
 * fast compression of data written to an underlying output stream.
 * 
 * <p>Snappy is a compression/decompression library that aims for very high speeds
 * and reasonable compression ratios. It is particularly effective for data that
 * contains repeated byte sequences.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * try (FileOutputStream fos = new FileOutputStream("data.snappy");
 *      SnappyOutputStream sos = new SnappyOutputStream(fos)) {
 *     sos.write("Hello, World!".getBytes());
 * }
 * }</pre>
 * 
 * <p>Note: This class requires the Xerial Snappy library to be on the classpath.
 * 
 * @see SnappyInputStream
 * @see java.io.OutputStream
 */
public final class SnappyOutputStream extends OutputStream {

    private final org.xerial.snappy.SnappyOutputStream out;

    /**
     * Creates a new SnappyOutputStream that compresses data written to the specified output stream.
     * Uses the default buffer size for compression.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OutputStream compressed = new SnappyOutputStream(new FileOutputStream("data.snappy"));
     * }</pre>
     *
     * @param os the underlying output stream to write compressed data to
     */
    public SnappyOutputStream(final OutputStream os) {
        out = new org.xerial.snappy.SnappyOutputStream(os);
    }

    /**
     * Creates a new SnappyOutputStream with the specified buffer size.
     * A larger buffer size may improve compression performance for large data sets
     * at the cost of increased memory usage.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use a 64KB buffer for better performance with large files
     * OutputStream compressed = new SnappyOutputStream(new FileOutputStream("large.snappy"), 65536);
     * }</pre>
     *
     * @param os the underlying output stream to write compressed data to
     * @param bufferSize the size of the compression buffer in bytes
     */
    public SnappyOutputStream(final OutputStream os, final int bufferSize) {
        out = new org.xerial.snappy.SnappyOutputStream(os, bufferSize);
    }

    /**
     * Writes a single byte of compressed data to the output stream.
     * The byte is written as the low-order byte of the integer value.
     * 
     * <p>Note: Writing single bytes is generally less efficient than writing
     * arrays of bytes due to compression overhead.
     *
     * @param b the byte to write (as an integer, where only the low-order byte is used)
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final int b) throws IOException {
        out.write(b);
    }

    /**
     * Writes an array of bytes to the output stream after compression.
     * This is equivalent to calling {@code write(b, 0, b.length)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Hello, World!".getBytes();
     * snappyOut.write(data);
     * }</pre>
     *
     * @param b the byte array to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final byte[] b) throws IOException {
        out.write(b);
    }

    /**
     * Writes a portion of a byte array to the output stream after compression.
     * Writes {@code len} bytes from the array {@code b}, starting at offset {@code off}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = inputStream.read(buffer);
     * snappyOut.write(buffer, 0, bytesRead);
     * }</pre>
     *
     * @param b the byte array containing data to write
     * @param off the start offset in the array
     * @param len the number of bytes to write
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if off is negative, len is negative,
     *         or off + len is greater than b.length
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
    }

    /**
     * Flushes this output stream and forces any buffered output bytes to be written out.
     * This ensures that all data written so far is compressed and sent to the underlying stream.
     * 
     * <p>Note: Calling flush() frequently may reduce compression efficiency as it forces
     * the compressor to output data that might otherwise be further compressed with
     * subsequent writes.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Closes this output stream and releases any system resources associated with it.
     * This method will flush any remaining buffered data and write the final compressed
     * blocks before closing the underlying output stream.
     * 
     * <p>Once closed, this stream cannot be used for further write operations.
     * Calling write methods after close() will result in an IOException.
     * 
     * <p>This method is idempotent - multiple calls to close() have no additional effect.
     *
     * @throws IOException if an I/O error occurs during closing
     */
    @Override
    public void close() throws IOException {
        out.close();
    }
}