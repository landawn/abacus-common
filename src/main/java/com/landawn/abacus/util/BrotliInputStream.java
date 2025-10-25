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
 * @since 1.0
 */
public final class BrotliInputStream extends InputStream {

    private final org.brotli.dec.BrotliInputStream in;

    /**
     * Creates a new BrotliInputStream that decompresses data from the specified source stream.
     * Uses the default internal buffer size for reading.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream compressed = new FileInputStream("compressed.br");
     * BrotliInputStream brotli = new BrotliInputStream(compressed);
     * }</pre>
     *
     * @param source the input stream containing Brotli-compressed data
     * @throws IOException if an I/O error occurs while initializing the decompressor
     */
    public BrotliInputStream(final InputStream source) throws IOException {
        in = new org.brotli.dec.BrotliInputStream(source);
    }

    /**
     * Creates a new BrotliInputStream with a specified internal buffer size.
     * A larger buffer size may improve performance when reading large amounts of data.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream compressed = new FileInputStream("large-file.br");
     * // Use 64KB buffer for better performance
     * BrotliInputStream brotli = new BrotliInputStream(compressed, 65536);
     * }</pre>
     *
     * @param source the input stream containing Brotli-compressed data
     * @param byteReadBufferSize the size of the internal buffer for reading, in bytes
     * @throws IOException if an I/O error occurs while initializing the decompressor
     */
    public BrotliInputStream(final InputStream source, final int byteReadBufferSize) throws IOException {
        in = new org.brotli.dec.BrotliInputStream(source, byteReadBufferSize);
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
     * @throws IOException if an I/O error occurs during decompression
     */
    @Override
    public int read() throws IOException {
        return in.read();
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
     * @throws IOException if an I/O error occurs during decompression
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return in.read(b);
    }

    /**
     * Reads up to len bytes of decompressed data into an array of bytes.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = brotliStream.read(buffer, 0, 512); // Read up to 512 bytes
     * }</pre>
     *
     * @param b the buffer into which the data is read
     * @param off the start offset in array b at which the data is written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data
     * @throws IOException if an I/O error occurs during decompression
     * @throws IndexOutOfBoundsException if off is negative, len is negative, or len is greater than b.length - off
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return in.read(b, off, len);
    }

    /**
     * Skips over and discards n bytes of decompressed data from this input stream.
     * The skip method may skip fewer bytes than requested.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long skipped = brotliStream.skip(1024); // Try to skip 1KB
     * System.out.println("Actually skipped: " + skipped + " bytes");
     * }</pre>
     *
     * @param n the number of bytes to be skipped
     * @return the actual number of bytes skipped
     * @throws IllegalArgumentException if n is negative
     * @throws IOException if an I/O error occurs during the skip operation
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (brotliStream.available() > 0) {
     *     // Data is available to read without blocking
     *     byte[] data = new byte[brotliStream.available()];
     *     brotliStream.read(data);
     * }
     * }</pre>
     *
     * @return an estimate of the number of bytes that can be read without blocking, or 0
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        return in.available();
    }

    /**
     * Marks the current position in this input stream.
     * A subsequent call to the reset method repositions this stream at the last marked position.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (brotliStream.markSupported()) {
     *     brotliStream.mark(1024); // Mark with 1KB read limit
     *     // Read some data
     *     brotliStream.reset(); // Return to marked position
     * }
     * }</pre>
     *
     * @param readLimit the maximum limit of bytes that can be read before the mark position becomes invalid
     */
    @Override
    public synchronized void mark(final int readLimit) {
        in.mark(readLimit);
    }

    /**
     * Repositions this stream to the position at the time the mark method was last called.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * brotliStream.mark(100);
     * byte[] preview = new byte[50];
     * brotliStream.read(preview);
     * brotliStream.reset(); // Go back to marked position
     * }</pre>
     *
     * @throws IOException if the stream has not been marked or if the mark has been invalidated
     */
    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    /**
     * Tests if this input stream supports the mark and reset methods.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (brotliStream.markSupported()) {
     *     System.out.println("Mark/reset operations are supported");
     * }
     * }</pre>
     *
     * @return {@code true} if this stream instance supports the mark and reset methods; false otherwise
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
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        in.close();
    }
}