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
 * String result = writer.toString(); // "Name: true, Age: 25"
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
 * @since 1.0
 */
@SuppressFBWarnings
sealed class BufferedWriter extends java.io.BufferedWriter permits CharacterWriter { // NOSONAR

    static final Writer DUMMY_WRITER = new DummyWriter();

    protected Writer out;

    protected char[] value;

    protected int count = 0;

    protected char[] _cbuf; //NOSONAR

    protected int nextChar = 0;

    protected boolean isClosed = false;

    /**
     * Creates a BufferedWriter with an internal buffer.
     * Content is stored in memory and can be retrieved using {@link #toString()}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedWriter writer = new BufferedWriter();
     * writer.write("Hello World");
     * String content = writer.toString(); // "Hello World"
     * }</pre>
     */
    BufferedWriter() {
        super(DUMMY_WRITER, 1);
        value = Objectory.createCharArrayBuffer();
        lock = value;
    }

    /**
     * Creates a BufferedWriter that writes to the specified OutputStream.
     * Characters are encoded using the default character encoding.
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
        this(IOUtil.newOutputStreamWriter(os, Charsets.DEFAULT));
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
     * writer.write(true);  // Writes "true"
     * writer.write(false); // Writes "false"
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
     * writer.write((byte) 127); // Writes "127"
     * writer.write((byte) -1);  // Writes "-1"
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
     * writer.write((short) 32767); // Writes "32767"
     * }</pre>
     *
     * @param s the short value to write
     * @throws IOException if an I/O error occurs
     */
    public void write(final short s) throws IOException {
        write(N.stringOf(s));
    }

    /**
     * Writes a single character.
     * 
     * @param ch the character to write as an int (0-65535)
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
     * writer.writeInt(12345);    // Writes "12345"
     * writer.writeInt(-999);     // Writes "-999"
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
     * writer.write(1234567890L); // Writes "1234567890"
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
     * writer.write(3.14f);     // Writes "3.14"
     * writer.write(1.0f/3.0f); // Writes "0.33333334"
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
     * writer.write(3.14159);   // Writes "3.14159"
     * writer.write(1.0/3.0);   // Writes "0.3333333333333333"
     * }</pre>
     *
     * @param d the double value to write
     * @throws IOException if an I/O error occurs
     */
    public void write(final double d) throws IOException {
        write(N.stringOf(d));
    }

    /**
     * Writes a Date using the default date format.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write(new Date()); // Writes current date/time
     * }</pre>
     *
     * @param date the date to write
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void write(final Date date) throws UncheckedIOException {
        Dates.formatTo(date, null, null, this);
    }

    /**
     * Writes a Calendar using the default date format.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * writer.write(cal); // Writes calendar date/time
     * }</pre>
     *
     * @param c the calendar to write
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void write(final Calendar c) throws UncheckedIOException {
        Dates.formatTo(c, null, null, this);
    }

    /**
     * Writes an XMLGregorianCalendar using the default date format.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar();
     * writer.write(xmlCal);
     * }</pre>
     *
     * @param c the XMLGregorianCalendar to write
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
     * writer.write('A'); // Writes "A"
     * writer.write('\n'); // Writes newline
     * }</pre>
     *
     * @param c the character to write
     * @throws IOException if an I/O error occurs
     */
    public void write(final char c) throws IOException {
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
     * Writes a string. If the string is null, "null" is written.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Hello");  // Writes "Hello"
     * writer.write(null);     // Writes "null"
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
     * Writes a portion of a string.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Hello World", 6, 5); // Writes "World"
     * }</pre>
     *
     * @param str the string to write
     * @param off the offset from which to start writing
     * @param len the number of characters to write
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if off or len are invalid
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
     * Internal method to write a non-null string.
     *
     * @param str the string to write (must not be null)
     * @throws IOException if an I/O error occurs
     */
    @Internal
    void writeNonNull(final String str) throws IOException {
        writeNonNull(str, 0, str.length());
    }

    /**
     * Internal method to write a portion of a non-null string.
     *
     * @param str the string to write (must not be null)
     * @param off the offset from which to start writing
     * @param len the number of characters to write
     * @throws IOException if an I/O error occurs
     */
    @Internal
    void writeNonNull(final String str, final int off, int len) throws IOException {
        // write(InternalUtil.getCharsForReadOnly(str), off, len);

        len = Math.min(str.length() - off, len);

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
     * writer.write(chars); // Writes "Hello"
     * }</pre>
     *
     * @param cbuf the character array to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final char[] cbuf) throws IOException {
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
     * writer.write(chars, 6, 5); // Writes "World"
     * }</pre>
     *
     * @param cbuf the character array
     * @param off the offset from which to start writing
     * @param len the number of characters to write
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if off or len are invalid
     */
    @Override
    public void write(final char[] cbuf, final int off, int len) throws IOException {
        len = Math.min(cbuf.length - off, len);

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
     * Writes a platform-specific line separator.
     * 
     * <p>The line separator string is defined by the system property
     * {@code line.separator}, and is not necessarily a single newline character.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Line 1");
     * writer.newLine();
     * writer.write("Line 2");
     * // Output on Unix: "Line 1\nLine 2"
     * // Output on Windows: "Line 1\r\nLine 2"
     * }</pre>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void newLine() throws IOException {
        write(IOUtil.LINE_SEPARATOR);
    }

    /**
     * Appends the specified character sequence to this writer.
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
     * @param c the character to append
     * @return this writer
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Writer append(final char c) throws IOException { //NOSONAR
        return super.append(c);
    }

    /**
     * Flushes the internal buffer to the underlying writer.
     * This method is called automatically when the buffer is full.
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
     * <p>If the stream has saved any characters from the write() methods in a buffer,
     * write them immediately to their destination. Then, if that destination is another
     * stream, flush it.</p>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        flushBufferToWriter();

        if (value == null) {
            out.flush();
        }

        Objectory.recycle(_cbuf);
        _cbuf = null;
        nextChar = 0;
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
     *     writer.close(); // Always close in finally block
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
            flush();

            IOUtil.close(out);
        } finally {
            _reset();
            isClosed = true;
        }
    }

    /**
     * Returns the contents of the internal buffer as a string.
     * 
     * <p>For writers in internal buffer mode, this returns all written content.
     * For writers with an underlying output stream or writer, this flushes
     * the buffer and returns the string representation of the underlying writer.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedWriter writer = new BufferedWriter();
     * writer.write("Hello ");
     * writer.write("World");
     * String result = writer.toString(); // "Hello World"
     * }</pre>
     *
     * @return the string representation of the written content
     * @throws UncheckedIOException if an I/O error occurs during flush
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
     * Reinitializes this writer for internal buffer mode.
     * This allows reusing the same writer instance.
     */
    void reinit() {
        isClosed = false;
        value = Objectory.createCharArrayBuffer();
        lock = value;
    }

    /**
     * Reinitializes this writer to write to the specified OutputStream.
     *
     * @param os the output stream to write to
     */
    void reinit(final OutputStream os) {
        reinit(IOUtil.newOutputStreamWriter(os)); // NOSONAR
    }

    /**
     * Reinitializes this writer to write to the specified Writer.
     *
     * @param writer the writer to write to
     */
    void reinit(final Writer writer) {
        isClosed = false;
        out = writer;
        lock = writer;
    }

    /**
     * Resets the internal state of this writer.
     */
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