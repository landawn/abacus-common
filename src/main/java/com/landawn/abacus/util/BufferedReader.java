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
import java.io.Reader;

import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * It's not multi-thread safety.
 *
 */
@SuppressFBWarnings
final class BufferedReader extends java.io.BufferedReader { // NOSONAR

    static final Reader DUMMY_READER = new DummyReader();

    char[] _cbuf; //NOSONAR

    int nChars = 0;

    int nextChar = 0;

    boolean skipLF = false;

    String str;

    char[] strValue;

    int strLength;

    Reader in;

    boolean isClosed;

    BufferedReader(final String st) {
        super(DUMMY_READER, 1);
        reinit(st);
    }

    BufferedReader(final InputStream is) {
        this(IOUtil.newInputStreamReader(is, Charsets.DEFAULT));
    }

    BufferedReader(final Reader reader) {
        super(reader, 1);
        reinit(reader);
    }

    /**
     *
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public int read() throws IOException {
        if (str == null) {
            do {
                if (nextChar >= nChars) {
                    fill();

                    if (nextChar >= nChars) {
                        return -1;
                    }
                }

                if (skipLF) {
                    skipLF = false;

                    if (_cbuf[nextChar] == '\n') {
                        nextChar++;

                        continue;
                    }
                }

                return _cbuf[nextChar++];
            } while (true);
        } else {
            return (nextChar < strLength) ? strValue[nextChar++] : (-1);
        }
    }

    /**
     *
     * @param cbuf
     * @param off
     * @param len
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private int read1(final char[] cbuf, final int off, final int len) throws IOException { // NOSONAR
        if (nextChar >= nChars) {
            /*
             * If the requested length is at least as large as the buffer, and if there is no mark/reset activity, and
             * if line feeds are not being skipped, do not bother to copy the characters into the local buffer. In this
             * way buffered streams will cascade harmlessly.
             */
            if ((len >= Objectory.BUFFER_SIZE) && !skipLF) {
                return IOUtil.read(in, cbuf, off, len);
            }

            fill();
        }

        if (nextChar >= nChars) {
            return -1;
        }

        if (skipLF) {
            skipLF = false;

            if (_cbuf[nextChar] == '\n') {
                nextChar++;

                if (nextChar >= nChars) {
                    fill();
                }

                if (nextChar >= nChars) {
                    return -1;
                }
            }
        }

        final int n = Math.min(len, nChars - nextChar);
        N.copy(_cbuf, nextChar, cbuf, off, n);
        nextChar += n;

        return n;
    }

    /**
     *
     * @param cbuf
     * @param off
     * @param len
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        if ((off < 0) || (len < 0) || (off > cbuf.length) || (len > cbuf.length - off)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (str == null) {
            int n = read1(cbuf, off, len);

            if (n <= 0) {
                return n;
            }

            while ((n < len) && in.ready()) {
                final int n1 = read1(cbuf, off + n, len - n);

                if (n1 <= 0) {
                    break;
                }

                n += n1;
            }

            return n;
        } else {
            if (nextChar >= strLength) {
                return -1;
            }

            final int n = Math.min(strLength - nextChar, len);
            str.getChars(nextChar, nextChar + n, cbuf, off);
            nextChar += n;

            return n;
        }
    }

    /**
     *
     * @param ignoreLF
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    String readLine(final boolean ignoreLF) throws IOException {
        StringBuilder sb = null;
        int startChar;

        boolean omitLF = ignoreLF || skipLF;

        do {
            if (nextChar >= nChars) {
                fill();
            }

            if (nextChar >= nChars) { /* EOF */
                String str = null; //NOSONAR

                if ((sb != null) && (!sb.isEmpty())) {
                    str = sb.toString();
                }

                Objectory.recycle(sb);

                return str;
            }

            boolean eol = false;
            char c = 0;
            int i;

            /* Skip a leftover '\n', if necessary */
            if (omitLF && (_cbuf[nextChar] == '\n')) {
                nextChar++;
            }

            skipLF = false;
            omitLF = false;

            for (i = nextChar; i < nChars; i++) {
                c = _cbuf[i];

                if ((c == '\n') || (c == '\r')) {
                    eol = true;

                    break;
                }
            }

            startChar = nextChar;
            nextChar = i;

            if (eol) {
                String str = null; //NOSONAR

                if (sb == null) {
                    str = new String(_cbuf, startChar, i - startChar);
                } else {
                    sb.append(_cbuf, startChar, i - startChar);
                    str = sb.toString();
                }

                nextChar++;

                if (c == '\r') {
                    skipLF = true;
                }

                Objectory.recycle(sb);

                return str;
            }

            if (sb == null) {
                sb = Objectory.createStringBuilder();
            }

            sb.append(_cbuf, startChar, i - startChar);
        } while (true);
    }

    /**
     *
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public String readLine() throws IOException {
        if (str == null) {
            return readLine(false);
        } else {
            if (nextChar >= strLength) {
                return null;
            } else {
                final String line = str.substring(nextChar);
                nextChar = strLength;

                return line;
            }
        }
    }

    /**
     *
     * @param n
     * @return
     * @throws IllegalArgumentException
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public long skip(final long n) throws IllegalArgumentException, IOException {
        N.checkArgNotNegative(n, cs.n);

        if (str == null) {
            long r = n;

            while (r > 0) {
                if (nextChar >= nChars) {
                    fill();
                }

                if (nextChar >= nChars) { /* EOF */

                    break;
                }

                if (skipLF) {
                    skipLF = false;

                    if (_cbuf[nextChar] == '\n') {
                        nextChar++;
                    }
                }

                final long d = nChars - nextChar; //NOSONAR

                if (r <= d) {
                    nextChar += (int) r;
                    r = 0;

                    break;
                } else {
                    r -= d;
                    nextChar = nChars;
                }
            }

            return n - r;
        } else {
            if (nextChar >= strLength) {
                return 0;
            }

            // Bound skip by beginning and end of the source
            final long ns = Math.min(strLength - nextChar, n); //NOSONAR
            nextChar += (int) ns;

            return ns;
        }
    }

    /**
     *
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public boolean ready() throws IOException {
        if (str == null) {
            /*
             * If newline needs to be skipped and the next char to be read is a newline character, then just skip it
             * right away.
             */
            if (skipLF) {
                /*
                 * Note that in.ready() will return {@code true} if and only if the next read on the stream will not block.
                 */
                if ((nextChar >= nChars) && in.ready()) {
                    fill();
                }

                if (nextChar < nChars) {
                    if (_cbuf[nextChar] == '\n') {
                        nextChar++;
                    }

                    skipLF = false;
                }
            }

            return (nextChar < nChars) || in.ready();
        } else {
            return true;
        }
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
            if (in != null) {
                in.close();
            }
        } finally {
            _reset();
            isClosed = true;
        }
    }

    /**
     *
     * @param st
     */
    @SuppressWarnings("deprecation")
    void reinit(final String st) {
        isClosed = false;
        str = st;
        strValue = InternalUtil.getCharsForReadOnly(str);
        strLength = st.length();
        lock = str;
    }

    /**
     *
     * @param is
     */
    void reinit(final InputStream is) {
        reinit(IOUtil.newInputStreamReader(is));
    }

    /**
     *
     * @param reader
     */
    void reinit(final Reader reader) {
        isClosed = false;
        in = reader;
        lock = reader;
    }

    /**
     * Reset.
     */
    void _reset() { //NOSONAR
        //noinspection SynchronizeOnNonFinalField
        synchronized (lock) {
            Objectory.recycle(_cbuf);
            _cbuf = null;
            nextChar = 0;
            nChars = 0;
            skipLF = false;

            str = null;
            strValue = null;
            strLength = 0;
            in = null;
        }
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void fill() throws IOException { // NOSONAR
        if (_cbuf == null) {
            _cbuf = Objectory.createCharArrayBuffer();
        }

        final int len = nChars - nextChar;

        if (len > 0) {
            N.copy(_cbuf, nextChar, _cbuf, 0, len);
        }

        nextChar = 0;
        nChars = len;

        final int n = IOUtil.read(in, _cbuf, len, _cbuf.length - len);

        if (n > 0) {
            nChars += n;
        }
    }

    static final class DummyReader extends Reader {

        @Override
        public int read(final char[] cbuf, final int off, final int len) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }
}
