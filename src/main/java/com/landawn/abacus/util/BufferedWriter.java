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
import java.io.Writer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * A high-performance buffered writer implementation that provides efficient writing
 * capabilities with support for primitive types and common objects.
 *
 * <p>This class extends {@link java.io.BufferedWriter} and adds methods for writing
 * primitive values, dates, and other common types directly without manual conversion
 * to strings. It can operate in two modes:</p>
 * <ul>
 *   <li>Internal buffer mode: Content is stored in memory and can be retrieved via {@link #toString()}</li>
 *   <li>External writer mode: Content is written to an underlying Writer or OutputStream</li>
 * </ul>
 *
 * <p>The writer provides automatic buffer management with dynamic capacity expansion
 * and efficient memory usage through object pooling.</p>
 *
 * <p><b>Important:</b> This class is not thread-safe. If multiple threads access
 * an instance concurrently, external synchronization is required.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Writing to internal buffer
 * BufferedWriter writer = new BufferedWriter();
 * writer.write("Name: ");
 * writer.write(true);
 * writer.write(", Age: ");
 * writer.writeInt(25);
 * String result = writer.toString();   // "Name: true, Age: 25"
 *
 * // Writing to a file
 * try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
 *     writer.write("Temperature: ");
 *     writer.write(23.5);
 *     writer.write("°C");
 *     writer.newLine();
 * }
 * }</pre>
 *
 * @see java.io.BufferedWriter
 * @see CharacterWriter
 */
@SuppressFBWarnings
sealed class BufferedWriter extends java.io.BufferedWriter permits CharacterWriter { // NOSONAR

    static final Writer DUMMY_WRITER = new DummyWriter();

    /**
     * The underlying writer to which buffered content is flushed, or {@code null}
     * when this writer operates in internal buffer mode.
     */
    protected Writer out;

    /**
     * The internal character buffer used in internal buffer mode, or {@code null}
     * when writing to an underlying {@code out} writer.
     */
    protected char[] value;

    /**
     * The number of valid characters currently stored in {@link #value}.
     */
    protected int count = 0;

    /**
     * The temporary flush buffer used when writing to an underlying {@code out} writer.
     */
    protected char[] _cbuf; //NOSONAR

    /**
     * The next position to write into {@link #_cbuf}.
     */
    protected int nextChar = 0;

    /**
     * Indicates whether this writer has been closed.
     */
    protected boolean isClosed = false;

    /**
     * Creates a BufferedWriter with an internal buffer.
     * Content is stored in memory and can be retrieved using {@link #toString()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedWriter writer = new BufferedWriter();
     * writer.write("Hello World");
     * String content = writer.toString();   // "Hello World"
     * }</pre>
     *
     */
    BufferedWriter() {
        super(DUMMY_WRITER, 1);
        value = Objectory.createCharArrayBuffer();
        lock = value;
    }

    /**
     * Creates a BufferedWriter that writes to the specified OutputStream.
     * Characters are encoded as UTF-8 ({@link IOUtil#DEFAULT_CHARSET}), independent of the
     * JVM's platform-default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (BufferedWriter writer = new BufferedWriter(new FileOutputStream("file.txt"))) {
     *     writer.write("Hello World");
     * }
     * }</pre>
     *
     * @param os the output stream to write to
     */
    BufferedWriter(final OutputStream os) {
        this(IOUtil.newOutputStreamWriter(os, IOUtil.DEFAULT_CHARSET));
    }

    /**
     * Creates a BufferedWriter that writes to the specified Writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (BufferedWriter writer = new BufferedWriter(new FileWriter("file.txt"))) {
     *     writer.write("Hello World");
     * }
     * }</pre>
     *
     * @param writer the underlying writer to write to
     */
    BufferedWriter(final Writer writer) {
        super(writer, 1);
        out = writer;
        lock = writer;
    }

    /**
     * Writes a boolean value as "true" or "false".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write(true);    // "true" is written
     * writer.write(false);   // "false" is written
     * }</pre>
     *
     * @param b the boolean value to write
     * @throws IOException if an I/O error occurs
     */
    public void write(final boolean b) throws IOException {
        write(b ? Strings.TRUE_CHAR_ARRAY : Strings.FALSE_CHAR_ARRAY);
    }

    /**
     * Writes a byte value as a decimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write((byte) 127);   // "127" is written
     * writer.write((byte) -1);    // "-1" is written
     * }</pre>
     *
     * @param b the byte value to write
     * @throws IOException if an I/O error occurs
     */
    public void write(final byte b) throws IOException {
        write(N.stringOf(b));
    }

    /**
     * Writes a short value as a decimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write((short) 32767);   // "32767" is written
     * }</pre>
     *
     * @param s the short value to write
     * @throws IOException if an I/O error occurs
     */
    public void write(final short s) throws IOException {
        write(N.stringOf(s));
    }

    /**
     * Writes a single character. The integer is cast to a {@code char}, so only the
     * low-order 16 bits are written and any higher-order bits are discarded.
     *
     * @param ch the character to write; the value is cast to {@code char} (0-65535)
     * @throws IOException if an I/O error occurs
     * @deprecated replaced by {@link #write(char)}
     */
    @Deprecated
    @Override
    public void write(final int ch) throws IOException {
        write((char) ch);
    }

    /**
     * Writes an integer value as a decimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.writeInt(12345);   // "12345" is written
     * writer.writeInt(-999);    // "-999" is written
     * }</pre>
     *
     * @param i the integer value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeInt(final int i) throws IOException {
        write(N.stringOf(i));
    }

    /**
     * Writes a long value as a decimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write(1234567890L);   // "1234567890" is written
     * }</pre>
     *
     * @param lng the long value to write
     * @throws IOException if an I/O error occurs
     */
    public void write(final long lng) throws IOException {
        write(N.stringOf(lng));
    }

    /**
     * Writes a float value as a decimal string.
     *
     * <p>The output format follows the same rules as {@link Float#toString(float)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write(3.14f);       // "3.14" is written
     * writer.write(1.0f/3.0f);   // "0.33333334" is written
     * }</pre>
     *
     * @param f the float value to write
     * @throws IOException if an I/O error occurs
     */
    public void write(final float f) throws IOException {
        write(N.stringOf(f));
    }

    /**
     * Writes a double value as a decimal string.
     *
     * <p>The output format follows the same rules as {@link Double#toString(double)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write(3.14159);   // "3.14159" is written
     * writer.write(1.0/3.0);   // "0.3333333333333333" is written
     * }</pre>
     *
     * @param d the double value to write
     * @throws IOException if an I/O error occurs
     */
    public void write(final double d) throws IOException {
        write(N.stringOf(d));
    }

    /**
     * Writes a {@link Date} using the default ISO-8601 date format
     * ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}). If {@code date} is {@code null},
     * the string {@code "null"} is written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write(new Date());    // "2024-06-15T10:30:00Z" is written (example value)
     * writer.write((Date) null);   // "null" is written
     * }</pre>
     *
     * @param date the date to write; if {@code null}, {@code "null"} is written
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void write(final Date date) throws UncheckedIOException {
        Dates.formatTo(date, null, null, this);
    }

    /**
     * Writes a {@link Calendar} using the default ISO-8601 date format
     * ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}). If {@code c} is {@code null},
     * the string {@code "null"} is written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * writer.write(cal);               // "2024-06-15T10:30:00Z" is written (example value)
     * writer.write((Calendar) null);   // "null" is written
     * }</pre>
     *
     * @param c the calendar to write; if {@code null}, {@code "null"} is written
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void write(final Calendar c) throws UncheckedIOException {
        Dates.formatTo(c, null, null, this);
    }

    /**
     * Writes an {@link XMLGregorianCalendar} using the default ISO-8601 date format
     * ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}). If {@code c} is {@code null},
     * the string {@code "null"} is written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar();
     * writer.write(xmlCal);                        // "2024-06-15T10:30:00Z" is written (example value)
     * writer.write((XMLGregorianCalendar) null);   // "null" is written
     * }</pre>
     *
     * @param c the XMLGregorianCalendar to write; if {@code null}, {@code "null"} is written
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void write(final XMLGregorianCalendar c) throws UncheckedIOException {
        Dates.formatTo(c, null, null, this);
    }

    /**
     * Writes a single character.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write('A');    // "A" is written
     * writer.write('\n');   // newline is written
     * }</pre>
     *
     * @param c the character to write
     * @throws IOException if an I/O error occurs
     */
    public void write(final char c) throws IOException {
        ensureOpen();

        if (value == null) {
            if (nextChar >= Objectory.BUFFER_SIZE) {
                flushBufferToWriter();
            }

            if (_cbuf == null) {
                _cbuf = Objectory.createCharArrayBuffer();
            }

            _cbuf[nextChar++] = c;
        } else {
            if (count == value.length) {
                expandCapacity(count + 64);
            }

            value[count++] = c;
        }
    }

    /**
     * Writes a string. If the string is {@code null}, "null" is written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Hello");   // "Hello" is written
     * writer.write(null);      // "null" is written
     * }</pre>
     *
     * @param str the string to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final String str) throws IOException {
        if (str == null) {
            write(Strings.NULL_CHAR_ARRAY);
        } else {
            write(str, 0, str.length());
        }
    }

    /**
     * Writes a portion of a string. If {@code str} is {@code null}, the four characters
     * {@code "null"} are written and the range is applied to the resulting {@code "null"} string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Hello World", 6, 5);   // "World" is written
     * }</pre>
     *
     * @param str the string to write; if {@code null}, {@code "null"} is written
     * @param off the offset from which to start writing; must be {@code >= 0} and {@code <= str.length()}
     * @param len the number of characters to write; must be {@code >= 0} and {@code <= str.length() - off}
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if {@code off < 0}, {@code len < 0},
     *         {@code off > str.length()}, or {@code len > str.length() - off}
     */
    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        if (str == null) {
            write(Strings.NULL_CHAR_ARRAY, off, len);
        } else {
            writeNonNull(str, off, len);
        }
    }

    /**
     * Internal method to write a {@code non-null} string.
     *
     * @param str the string to write (must not be null)
     * @throws IOException if an I/O error occurs
     */
    @Internal
    void writeNonNull(final String str) throws IOException {
        writeNonNull(str, 0, str.length());
    }

    /**
     * Internal method to write a portion of a {@code non-null} string.
     *
     * @param str the string to write; must not be {@code null}
     * @param off the offset from which to start writing; must be {@code >= 0} and {@code <= str.length()}
     * @param len the number of characters to write; must be {@code >= 0} and {@code <= str.length() - off}
     * @throws IndexOutOfBoundsException if {@code off < 0}, {@code len < 0},
     *         {@code off > str.length()}, or {@code len > str.length() - off}
     * @throws IOException if an I/O error occurs
     */
    @Internal
    void writeNonNull(final String str, final int off, int len) throws IOException {
        ensureOpen();

        if ((off < 0) || (len < 0) || (off > str.length()) || (len > str.length() - off)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        // write(InternalUtil.getCharsForReadOnly(str), off, len);

        if (value == null) {
            if (len > (Objectory.BUFFER_SIZE - nextChar)) {
                if (nextChar > 0) {
                    flushBufferToWriter();
                }

                out.write(str, off, len);
            } else {
                if (_cbuf == null) {
                    _cbuf = Objectory.createCharArrayBuffer();
                }

                str.getChars(off, off + len, _cbuf, nextChar);
                nextChar += len;
            }
        } else {
            if (len > (value.length - count)) {
                expandCapacity(count + len);
            }

            str.getChars(off, off + len, value, count);
            count += len;
        }
    }

    /**
     * Writes a character array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * writer.write(chars);   // "Hello" is written
     * }</pre>
     *
     * @param cbuf the character array to write; must not be {@code null}
     * @throws NullPointerException if {@code cbuf} is {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final char[] cbuf) throws IOException {
        ensureOpen();

        final int len = cbuf.length;

        if (value == null) {
            if (len > (Objectory.BUFFER_SIZE - nextChar)) {
                if (nextChar > 0) {
                    flushBufferToWriter();
                }

                out.write(cbuf, 0, len);
            } else {
                if (_cbuf == null) {
                    _cbuf = Objectory.createCharArrayBuffer();
                }

                N.copy(cbuf, 0, _cbuf, nextChar, len);
                nextChar += len;
            }
        } else {
            if (len > (value.length - count)) {
                expandCapacity(count + len);
            }

            N.copy(cbuf, 0, value, count, len);
            count += len;
        }
    }

    /**
     * Writes a portion of a character array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
     * writer.write(chars, 6, 5);   // "World" is written
     * }</pre>
     *
     * @param cbuf the character array; must not be {@code null}
     * @param off the offset from which to start writing; must be {@code >= 0} and {@code <= cbuf.length}
     * @param len the number of characters to write; must be {@code >= 0} and {@code <= cbuf.length - off}
     * @throws NullPointerException if {@code cbuf} is {@code null}
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if {@code off < 0}, {@code len < 0},
     *         {@code off > cbuf.length}, or {@code len > cbuf.length - off}
     */
    @Override
    public void write(final char[] cbuf, final int off, int len) throws IOException {
        ensureOpen();

        if ((off < 0) || (len < 0) || (off > cbuf.length) || (len > cbuf.length - off)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        if (value == null) {
            if (len > (Objectory.BUFFER_SIZE - nextChar)) {
                if (nextChar > 0) {
                    flushBufferToWriter();
                }

                out.write(cbuf, off, len);
            } else {
                if (_cbuf == null) {
                    _cbuf = Objectory.createCharArrayBuffer();
                }

                N.copy(cbuf, off, _cbuf, nextChar, len);
                nextChar += len;
            }
        } else {
            if (len > (value.length - count)) {
                expandCapacity(count + len);
            }

            N.copy(cbuf, off, value, count, len);
            count += len;
        }
    }

    /**
     * Writes a line separator.
     *
     * <p>This implementation always writes the Unix line separator ({@code "\n"}),
     * regardless of the underlying platform. This differs from
     * {@link java.io.BufferedWriter#newLine()}, which uses the platform's
     * {@code line.separator} system property.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Line 1");
     * writer.newLine();
     * writer.write("Line 2");
     * // Output on every platform: "Line 1\nLine 2"
     * }</pre>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void newLine() throws IOException {
        write(IOUtil.LINE_SEPARATOR_UNIX);
    }

    /**
     * Appends the specified character sequence to this writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.append("Hello").append(" ").append("World");
     * }</pre>
     *
     * @param csq the character sequence to append. If {@code null}, then
     *        the four characters "null" are appended
     * @return this writer
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Writer append(final CharSequence csq) throws IOException { //NOSONAR
        return super.append(csq);
    }

    /**
     * Appends a subsequence of the specified character sequence to this writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.append("Hello World", 0, 5);   // "Hello" is appended
     * }</pre>
     *
     * @param csq the character sequence from which a subsequence will be appended.
     *        If {@code null}, then characters will be appended as if {@code csq}
     *        contained the four characters "null"
     * @param start the index of the first character in the subsequence
     * @param end the index of the character following the last character in the subsequence
     * @return this writer
     * @throws IndexOutOfBoundsException if start or end are negative, start is greater
     *         than end, or end is greater than csq.length()
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Writer append(final CharSequence csq, final int start, final int end) throws IOException { //NOSONAR
        return super.append(csq, start, end);
    }

    /**
     * Appends the specified character to this writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.append('H').append('i').append('!');
     * }</pre>
     *
     * @param c the character to append
     * @return this writer
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Writer append(final char c) throws IOException { //NOSONAR
        return super.append(c);
    }

    /**
     * Writes the contents of the transient flush buffer ({@link #_cbuf}) to the
     * underlying {@code out} writer and resets the buffer position. This is a no-op
     * in internal buffer mode (when {@link #value} is non-{@code null}) and when the
     * flush buffer is empty. It is invoked automatically when the flush buffer becomes
     * full or cannot accommodate the next write.
     *
     * @throws IOException if an I/O error occurs
     */
    void flushBufferToWriter() throws IOException {
        if (value == null) {
            if (nextChar == 0) {
                return;
            }

            out.write(_cbuf, 0, nextChar);

            nextChar = 0;
        }
    }

    /**
     * Flushes the stream.
     *
     * <p>In external writer mode, any characters saved in the flush buffer are written
     * immediately to the underlying {@code out} writer, and that writer is then flushed.
     * In internal buffer mode (created via {@code BufferedWriter()}) there is no
     * underlying destination, so the accumulated content is retained and this method only
     * releases the transient flush buffer.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Important data");
     * writer.flush();   // data is flushed to the underlying stream
     * }</pre>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        ensureOpen();

        flushBufferToWriter();

        if (value == null) {
            out.flush();
        }

        Objectory.recycle(_cbuf);
        _cbuf = null;
        nextChar = 0;
    }

    /**
     * Ensures the writer is open before an I/O operation.
     *
     * @throws IOException if the writer has been closed
     */
    void ensureOpen() throws IOException {
        if (isClosed) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * Closes the stream, flushing it first.
     *
     * <p>Once the stream has been closed, further write() or flush() invocations
     * will cause an IOException to be thrown. Closing a previously closed stream
     * has no effect.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedWriter writer = new BufferedWriter(new FileWriter("file.txt"));
     * try {
     *     writer.write("Hello World");
     * } finally {
     *     writer.close();   // closes in finally block
     * }
     * }</pre>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (isClosed) {
            return;
        }

        try {
            // Always attempt to close the underlying writer even if flush() throws,
            // while preserving the original failure and suppressing close failures.
            Throwable exception = null;

            try {
                flush();
            } catch (final Throwable e) {
                exception = e;
            }

            try {
                if (out != null) {
                    out.close();
                }
            } catch (final Throwable e) {
                if (exception == null) {
                    exception = e;
                } else if (exception != e) {
                    // Avoid masking the original failure if the delegate rethrows the same
                    // Throwable instance from flush() and close().
                    exception.addSuppressed(e);
                }
            }

            if (exception instanceof IOException) {
                throw (IOException) exception;
            } else if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else if (exception instanceof Error) {
                throw (Error) exception;
            } else if (exception != null) {
                throw new IOException(exception);
            }
        } finally {
            _reset();
            isClosed = true;
        }
    }

    /**
     * Returns the contents of the internal buffer as a string.
     *
     * <p>For writers in internal buffer mode (created via {@code BufferedWriter()}),
     * this returns all content written so far without flushing.
     * For writers backed by an underlying output stream or writer, the flush buffer
     * is first flushed to the underlying writer and then {@code out.toString()} is
     * returned; the usefulness of that result depends on the underlying writer's own
     * {@code toString()} implementation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedWriter writer = new BufferedWriter();
     * writer.write("Hello ");
     * writer.write("World");
     * String result = writer.toString();   // "Hello World"
     * }</pre>
     *
     * @return the string representation of the written content
     * @throws UncheckedIOException if an I/O error occurs during flush (external writer mode only)
     */
    @Override
    public String toString() throws UncheckedIOException {
        if (value == null) {
            try {
                flush();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return out.toString();
        } else {
            return String.valueOf(value, 0, count);
        }
    }

    /**
     * Reinitializes this writer for internal buffer mode, replacing any previous
     * state. Any previously held internal or flush buffers are recycled, and a
     * fresh internal character buffer is allocated. This allows reusing the same
     * writer instance without creating a new object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedWriter writer = new BufferedWriter();
     * writer.write("First use");
     * System.out.println(writer.toString());   // "First use"
     *
     * writer.reinit();
     * writer.write("Second use");
     * System.out.println(writer.toString());   // "Second use"
     * }</pre>
     *
     */
    void reinit() {
        isClosed = false;
        Objectory.recycle(_cbuf);
        _cbuf = null;
        nextChar = 0;

        Objectory.recycle(value);
        value = Objectory.createCharArrayBuffer();
        count = 0;
        out = null;
        lock = value;
    }

    /**
     * Reinitializes this writer to write to the specified {@link OutputStream},
     * replacing any previous state. The stream is wrapped in a UTF-8
     * {@link java.io.OutputStreamWriter}. Any previously held buffers are recycled.
     * This allows reusing the same writer instance with a different output stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedWriter writer = new BufferedWriter();
     * writer.write("First file");
     * writer.reinit(new FileOutputStream("second.txt"));
     * writer.write("Second file");
     * }</pre>
     *
     * @param os the new OutputStream to write to; must not be {@code null}
     */
    void reinit(final OutputStream os) {
        reinit(IOUtil.newOutputStreamWriter(os)); // NOSONAR
    }

    /**
     * Reinitializes this writer to write to the specified {@link Writer},
     * replacing any previous state. Any previously held internal buffers are
     * recycled. This allows reusing the same writer instance with a different
     * underlying writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedWriter writer = new BufferedWriter(new FileWriter("first.txt"));
     * writer.write("First content");
     *
     * writer.reinit(new FileWriter("second.txt"));
     * writer.write("Second content");
     * }</pre>
     *
     * @param writer the new Writer to write to; must not be {@code null}
     */
    void reinit(final Writer writer) {
        isClosed = false;
        Objectory.recycle(value);
        value = null;
        count = 0;
        nextChar = 0;
        out = writer;
        lock = writer;
    }

    void _reset() { //NOSONAR
        Objectory.recycle(_cbuf);
        _cbuf = null;
        nextChar = 0;

        Objectory.recycle(value);
        value = null;
        count = 0;

        out = null;
        lock = null;
    }

    /**
     * Expands the capacity of the internal buffer to ensure it can hold
     * at least the specified minimum capacity.
     *
     * @param minimumCapacity the desired minimum capacity
     * @throws OutOfMemoryError if the required capacity exceeds limits
     */
    void expandCapacity(final int minimumCapacity) {
        int newCapacity = (value.length * 2) + 2;

        if ((newCapacity - minimumCapacity) < 0) {
            newCapacity = minimumCapacity;
        }

        if (newCapacity < 0) {
            if (minimumCapacity < 0) { // overflow
                throw new OutOfMemoryError();
            }

            newCapacity = Integer.MAX_VALUE;
        }

        final char[] tmp = Arrays.copyOf(value, newCapacity);

        Objectory.recycle(value);

        value = tmp;
    }

    /**
     * A dummy Writer implementation used as a placeholder for internal buffer mode.
     */
    static final class DummyWriter extends Writer {
        DummyWriter() {
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void flush() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }
}
