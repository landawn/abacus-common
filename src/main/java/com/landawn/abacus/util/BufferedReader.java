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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * A high-performance buffered reader implementation that can read from strings,
 * input streams, or other readers.
 * 
 * <p>This class provides optimized reading capabilities and can be reused by
 * reinitializing with different sources. Unlike the standard BufferedReader,
 * this implementation allows switching between string-based and stream-based
 * reading modes.</p>
 * 
 * <p><b>Important:</b> This class is not thread-safe. If multiple threads access
 * an instance concurrently, external synchronization is required.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Reading from a string
 * BufferedReader reader = new BufferedReader("Hello\nWorld");
 * String line1 = reader.readLine();   // "Hello"
 * String line2 = reader.readLine();   // "World"
 * 
 * // Reading from an InputStream
 * BufferedReader reader2 = new BufferedReader(new FileInputStream("file.txt"));
 * String firstLine = reader2.readLine();
 * reader2.close();
 * }</pre>
 * 
 * @see java.io.BufferedReader
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

    /**
     * Creates a BufferedReader that reads from the specified string.
     * 
     * <p>This constructor initializes the reader to read characters directly
     * from the provided string without any underlying stream.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedReader reader = new BufferedReader("Line 1\nLine 2\nLine 3");
     * while (true) {
     *     String line = reader.readLine();
     *     if (line == null) break;
     *     System.out.println(line);
     * }
     * }</pre>
     * 
     * @param st the string to read from
     */
    BufferedReader(final String st) {
        super(DUMMY_READER, 1);
        reinit(st);
    }

    /**
     * Creates a BufferedReader that reads from the specified InputStream.
     * 
     * <p>The stream is wrapped with an InputStreamReader using the default
     * character encoding.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedReader reader = new BufferedReader(System.in);
     * System.out.print("Enter text: ");
     * String input = reader.readLine();
     * }</pre>
     * 
     * @param is the input stream to read from
     */
    BufferedReader(final InputStream is) {
        this(IOUtil.newInputStreamReader(is, Charsets.DEFAULT));
    }

    /**
     * Creates a BufferedReader that reads from the specified Reader.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FileReader fileReader = new FileReader("data.txt");
     * BufferedReader reader = new BufferedReader(fileReader);
     * String content = reader.readLine();
     * reader.close();
     * }</pre>
     * 
     * @param reader the reader to read from
     */
    BufferedReader(final Reader reader) {
        super(reader, 1);
        reinit(reader);
    }

    /**
     * Reads a single character from the input.
     * 
     * <p>This method returns the character as an integer value, or -1 if
     * the end of the stream has been reached. Line terminators are processed
     * according to the reader's configuration.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedReader reader = new BufferedReader("ABC");
     * int ch1 = reader.read();   // 65 ('A')
     * int ch2 = reader.read();   // 66 ('B')
     * int ch3 = reader.read();   // 67 ('C')
     * int ch4 = reader.read();   // -1 (end of stream)
     * }</pre>
     *
     * @return the character read, as an integer in the range 0 to 65535,
     *         or -1 if the end of the stream has been reached
     * @throws IOException if an I/O error occurs
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
     * Reads characters into a portion of an array.
     * 
     * <p>This method implements efficient bulk reading of characters. It attempts
     * to read as many characters as possible up to the specified length.</p>
     *
     * @param cbuf destination buffer
     * @param off offset at which to start storing characters
     * @param len maximum number of characters to read
     * @return the number of characters read, or -1 if the end of the stream
     *         has been reached
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if off or len are invalid
     */
    private int read1(final char[] cbuf, final int off, final int len) throws IOException { // NOSONAR
        if (nextChar >= nChars) {
            /*
             * If the requested length is at least as large as the buffer, and if there is no mark/reset activity, and
             * if line feeds are not being skipped, do not bother to copy the characters into the local buffer. In this
             *  way, buffered streams will cascade harmlessly.
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
     * Reads characters into a portion of an array.
     * 
     * <p>This method attempts to read up to {@code len} characters from the input,
     * storing them into the array {@code cbuf} starting at offset {@code off}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedReader reader = new BufferedReader("Hello World");
     * char[] buffer = new char[5];
     * int count = reader.read(buffer, 0, 5);              // Reads "Hello"
     * System.out.println(new String(buffer, 0, count));   // "Hello"
     * }</pre>
     *
     * @param cbuf destination buffer
     * @param off offset at which to start storing characters
     * @param len maximum number of characters to read
     * @return the number of characters read, or -1 if the end of the stream
     *         has been reached
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if the parameters are invalid
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
     * Reads a line of text with control over line terminator handling.
     * 
     * @param ignoreLF if {@code true}, skip a leading line feed character
     * @return a String containing the contents of the line, not including
     *         any line-termination characters, or {@code null} if the end of the
     *         stream has been reached
     * @throws IOException if an I/O error occurs
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
     * Reads a line of text.
     * 
     * <p>A line is considered to be terminated by any one of a line feed ('\n'),
     * a carriage return ('\r'), or a carriage return followed immediately by a
     * line feed. The line terminator is not included in the returned string.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedReader reader = new BufferedReader("Line 1\nLine 2\nLine 3");
     * System.out.println(reader.readLine());   // "Line 1"
     * System.out.println(reader.readLine());   // "Line 2"
     * System.out.println(reader.readLine());   // "Line 3"
     * System.out.println(reader.readLine());   // null
     * }</pre>
     *
     * @return a String containing the contents of the line, not including
     *         any line-termination characters, or {@code null} if the end of the
     *         stream has been reached
     * @throws IOException if an I/O error occurs
     */
    @MayReturnNull
    @Override
    public String readLine() throws IOException {
        if (str == null) {
            return readLine(false);
        } else {
            if (nextChar >= strLength) {
                return null;
            }

            final int start = nextChar;
            int i = nextChar;
            char c = 0;

            while (i < strLength) {
                c = strValue[i];

                if (c == '\n' || c == '\r') {
                    break;
                }

                i++;
            }

            final String line = str.substring(start, i);
            nextChar = i;

            if (nextChar < strLength) {
                nextChar++;

                if (c == '\r' && nextChar < strLength && strValue[nextChar] == '\n') {
                    nextChar++;
                }
            }

            return line;
        }
    }

    /**
     * Skips characters.
     * 
     * <p>This method will skip as many characters as possible up to the
     * specified number, and returns the actual number of characters skipped.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedReader reader = new BufferedReader("1234567890");
     * long skipped = reader.skip(5);
     * System.out.println(skipped);   // 5
     * int ch = reader.read();        // '6' (the 6th character)
     * }</pre>
     *
     * @param n the number of characters to skip
     * @return the number of characters actually skipped
     * @throws IllegalArgumentException if n is negative
     * @throws IOException if an I/O error occurs
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
     * Tells whether this stream is ready to be read.
     * 
     * <p>A stream is ready if the buffer contains data or if the underlying
     * stream is ready.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedReader reader = new BufferedReader("Hello");
     * if (reader.ready()) {
     *     String data = reader.readLine();
     * }
     * }</pre>
     *
     * @return {@code true} if the next read() is guaranteed not to block for input,
     *         {@code false} otherwise
     * @throws IOException if an I/O error occurs
     */
    @Override
    public boolean ready() throws IOException {
        if (str == null) {
            /*
             * If newline needs to be skipped and the next char to be read is a newline character, then skip it
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
     * Closes the stream and releases any system resources associated with it.
     * 
     * <p>Once the stream has been closed, further read(), ready(), or skip()
     * invocations will throw an IOException. Closing a previously closed
     * stream has no effect.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedReader reader = new BufferedReader(new FileInputStream("file.txt"));
     * try {
     *     String line = reader.readLine();
     *     // Process line
     * } finally {
     *     reader.close();   // Always close in finally block
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
            if (in != null) {
                in.close();
            }
        } finally {
            _reset();
            isClosed = true;
        }
    }

    /**
     * Reinitializes this reader to read from the specified string.
     * 
     * <p>This method allows reusing the same BufferedReader instance for
     * reading different strings, which can be more efficient than creating
     * new instances.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedReader reader = new BufferedReader("First");
     * System.out.println(reader.readLine());   // "First"
     * 
     * reader.reinit("Second");
     * System.out.println(reader.readLine());   // "Second"
     * }</pre>
     *
     * @param st the new string to read from
     */
    @SuppressWarnings("deprecation")
    void reinit(final String st) {
        isClosed = false;
        str = st;
        strValue = InternalUtil.getCharsForReadOnly(str);
        strLength = st.length();
        nextChar = 0;
        nChars = 0;
        skipLF = false;
        lock = str;
    }

    /**
     * Reinitializes this reader to read from the specified InputStream.
     * 
     * <p>The stream is wrapped with an InputStreamReader using the default
     * character encoding.</p>
     *
     * @param is the new input stream to read from
     */
    void reinit(final InputStream is) {
        reinit(IOUtil.newInputStreamReader(is));
    }

    /**
     * Reinitializes this reader to read from the specified Reader.
     * 
     * <p>This method allows reusing the same BufferedReader instance for
     * reading from different sources.</p>
     *
     * @param reader the new reader to read from
     */
    void reinit(final Reader reader) {
        isClosed = false;
        str = null;
        strValue = null;
        strLength = 0;
        in = reader;
        nextChar = 0;
        nChars = 0;
        skipLF = false;
        lock = reader;
    }

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
     * Fills the internal buffer with data from the underlying reader.
     *
     * @throws IOException if an I/O error occurs
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

    /**
     * A dummy Reader implementation used as a placeholder.
     */
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
