/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.exception.UncheckedIOException;

/**
 * It's not multi-thread safety.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class BufferedWriter extends Writer {

    protected Writer out;

    protected char[] value;

    protected int count = 0;

    protected char[] _cbuf;

    protected int nextChar = 0;

    protected boolean isClosed = false;

    BufferedWriter() {
        this.value = Objectory.createCharArrayBuffer();
        this.lock = value;
    }

    BufferedWriter(OutputStream os) {
        this(new OutputStreamWriter(os, Charsets.UTF_8));
    }

    BufferedWriter(Writer writer) {
        this.out = writer;
        this.lock = writer;
    }

    /**
     *
     * @param b
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(boolean b) throws IOException {
        write(b ? N.TRUE_CHAR_ARRAY : N.FALSE_CHAR_ARRAY);
    }

    /**
     *
     * @param b
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(byte b) throws IOException {
        write(N.stringOf(b));
    }

    /**
     *
     * @param s
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(short s) throws IOException {
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
    public void write(int ch) throws IOException {
        write((char) ch);
    }

    /**
     *
     * @param i
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeInt(int i) throws IOException {
        write(N.stringOf(i));
    }

    /**
     *
     * @param lng
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(long lng) throws IOException {
        write(N.stringOf(lng));
    }

    /**
     *
     * @param f
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(float f) throws IOException {
        write(String.valueOf(f));
    }

    /**
     *
     * @param d
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(double d) throws IOException {
        write(String.valueOf(d));
    }

    /**
     *
     * @param date
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(Date date) throws IOException {
        DateUtil.format(this, date, null, null);
    }

    /**
     *
     * @param c
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(Calendar c) throws IOException {
        DateUtil.format(this, c, null, null);
    }

    /**
     *
     * @param c
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(XMLGregorianCalendar c) throws IOException {
        DateUtil.format(this, c, null, null);
    }

    /**
     *
     * @param c
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(char c) throws IOException {
        if (value == null) {
            if (nextChar >= Objectory.BUFFER_SIZE) {
                flushBuffer();
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
    public void write(String str) throws IOException {
        if (str == null) {
            write(N.NULL_CHAR_ARRAY);
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
    public void write(String str, int off, int len) throws IOException {
        if (str == null) {
            write(N.NULL_CHAR_ARRAY, off, len);
        } else {
            // write(InternalUtil.getCharsForReadOnly(str), off, len);

            len = Math.min(str.length() - off, len);

            if (value == null) {
                if (len > (Objectory.BUFFER_SIZE - nextChar)) {
                    if (nextChar > 0) {
                        flushBuffer();
                    }

                    out.write(str, off, len);
                } else {
                    if (this._cbuf == null) {
                        this._cbuf = Objectory.createCharArrayBuffer();
                    }

                    str.getChars(off, off + len, this._cbuf, nextChar);
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
    }

    /**
     *
     * @param cbuf
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(char[] cbuf) throws IOException {
        final int len = cbuf.length;

        if (value == null) {
            if (len > (Objectory.BUFFER_SIZE - nextChar)) {
                if (nextChar > 0) {
                    flushBuffer();
                }

                out.write(cbuf, 0, len);
            } else {
                if (this._cbuf == null) {
                    this._cbuf = Objectory.createCharArrayBuffer();
                }

                N.copy(cbuf, 0, this._cbuf, nextChar, len);
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
    public void write(char[] cbuf, int off, int len) throws IOException {
        len = Math.min(cbuf.length - off, len);

        if (value == null) {
            if (len > (Objectory.BUFFER_SIZE - nextChar)) {
                if (nextChar > 0) {
                    flushBuffer();
                }

                out.write(cbuf, off, len);
            } else {
                if (this._cbuf == null) {
                    this._cbuf = Objectory.createCharArrayBuffer();
                }

                N.copy(cbuf, off, this._cbuf, nextChar, len);
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
    public Writer append(CharSequence csq) throws IOException {
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
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        return super.append(csq, start, end);
    }

    /**
     *
     * @param c
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public Writer append(char c) throws IOException {
        return super.append(c);
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void flushBuffer() throws IOException {
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
        flushBuffer();

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
            } catch (IOException e) {
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
        this.isClosed = false;
        this.value = Objectory.createCharArrayBuffer();
        this.lock = value;
    }

    /**
     *
     * @param os
     */
    void reinit(OutputStream os) {
        reinit(new OutputStreamWriter(os));
    }

    /**
     *
     * @param writer
     */
    void reinit(Writer writer) {
        this.isClosed = false;
        this.out = writer;
        this.lock = writer;
    }

    /**
     * Reset.
     */
    void _reset() {
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
    void expandCapacity(int minimumCapacity) {
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

        char[] tmp = Arrays.copyOf(value, newCapacity);

        Objectory.recycle(value);

        value = tmp;
    }
}
