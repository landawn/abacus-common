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
 * It's not multi-thread safety.
 *
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

    BufferedWriter() {
        super(DUMMY_WRITER, 1);
        value = Objectory.createCharArrayBuffer();
        lock = value;
    }

    BufferedWriter(final OutputStream os) {
        this(IOUtil.newOutputStreamWriter(os, Charsets.DEFAULT));
    }

    BufferedWriter(final Writer writer) {
        super(writer, 1);
        out = writer;
        lock = writer;
    }

    /**
     *
     * @param b
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(final boolean b) throws IOException {
        write(b ? Strings.TRUE_CHAR_ARRAY : Strings.FALSE_CHAR_ARRAY);
    }

    /**
     *
     * @param b
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(final byte b) throws IOException {
        write(N.stringOf(b));
    }

    /**
     *
     * @param s
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(final short s) throws IOException {
        write(N.stringOf(s));
    }

    /**
     *
     * @param ch
     * @throws IOException Signals that an I/O exception has occurred.
     * @deprecated replaced by write(char).
     */
    @Deprecated
    @Override
    public void write(final int ch) throws IOException {
        write((char) ch);
    }

    /**
     *
     * @param i
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeInt(final int i) throws IOException {
        write(N.stringOf(i));
    }

    /**
     *
     * @param lng
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(final long lng) throws IOException {
        write(N.stringOf(lng));
    }

    /**
     *
     * @param f
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(final float f) throws IOException {
        write(N.stringOf(f));
    }

    /**
     *
     * @param d
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(final double d) throws IOException {
        write(N.stringOf(d));
    }

    /**
     *
     * @param date
     * @throws UncheckedIOException Signals that an I/O exception has occurred.
     */
    public void write(final Date date) throws UncheckedIOException {
        Dates.formatTo(date, null, null, this);
    }

    /**
     *
     * @param c
     * @throws UncheckedIOException Signals that an I/O exception has occurred.
     */
    public void write(final Calendar c) throws UncheckedIOException {
        Dates.formatTo(c, null, null, this);
    }

    /**
     *
     * @param c
     * @throws UncheckedIOException Signals that an I/O exception has occurred.
     */
    public void write(final XMLGregorianCalendar c) throws UncheckedIOException {
        Dates.formatTo(c, null, null, this);
    }

    /**
     *
     * @param c
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param str
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param str
     * @param off
     * @param len
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param str
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Internal
    void writeNonNull(final String str) throws IOException {
        writeNonNull(str, 0, str.length());
    }

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
     *
     * @param cbuf
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param cbuf
     * @param off
     * @param len
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void newLine() throws IOException {
        write(IOUtil.LINE_SEPARATOR);
    }

    /**
     *
     * @param csq
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public Writer append(final CharSequence csq) throws IOException { //NOSONAR
        return super.append(csq);
    }

    /**
     *
     * @param csq
     * @param start
     * @param end
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public Writer append(final CharSequence csq, final int start, final int end) throws IOException { //NOSONAR
        return super.append(csq, start, end);
    }

    /**
     *
     * @param c
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public Writer append(final char c) throws IOException { //NOSONAR
        return super.append(c);
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
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
     * Reinit.
     */
    void reinit() {
        isClosed = false;
        value = Objectory.createCharArrayBuffer();
        lock = value;
    }

    /**
     *
     * @param os
     */
    void reinit(final OutputStream os) {
        reinit(IOUtil.newOutputStreamWriter(os)); // NOSONAR
    }

    /**
     *
     * @param writer
     */
    void reinit(final Writer writer) {
        isClosed = false;
        out = writer;
        lock = writer;
    }

    /**
     * Reset.
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
     *
     * @param minimumCapacity
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
