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

/**
 * Built on {@code StringBuilder}. Not like <code/>java.io.StringWriter</code>. it's not multi-thread safety.
 *
 */
public final class StringWriter extends AppendableWriter {

    private final StringBuilder buf;

    /**
     *
     */
    public StringWriter() {
        this(new StringBuilder());
    }

    /**
     *
     *
     * @param initialSize
     */
    public StringWriter(final int initialSize) {
        this(new StringBuilder(initialSize));
    }

    /**
     *
     *
     * @param sb
     */
    public StringWriter(final StringBuilder sb) {
        super(sb);
        buf = sb;
        lock = buf;
    }

    /**
     *
     *
     * @return
     */
    public StringBuilder stringBuilder() {
        return buf;
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public StringWriter append(final char c) {
        buf.append(c);

        return this;
    }

    /**
     *
     * @param csq
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public StringWriter append(final CharSequence csq) throws IOException {
        buf.append(csq);

        return this;
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
    public StringWriter append(final CharSequence csq, final int start, final int end) throws IOException {
        buf.append(csq, start, end);

        return this;
    }

    /**
     *
     * @param c
     */
    @Override
    public void write(final int c) {
        buf.append((char) c);
    }

    /**
     *
     *
     * @param cbuf
     * @throws IOException
     */
    @Override
    public void write(final char[] cbuf) throws IOException {
        buf.append(cbuf);
    }

    /**
     *
     * @param cbuf
     * @param off
     * @param len
     */
    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        buf.append(cbuf, off, len);
    }

    /**
     *
     * @param str
     */
    @Override
    public void write(final String str) {
        buf.append(str);
    }

    /**
     *
     * @param str
     * @param off
     * @param len
     */
    @Override
    public void write(final String str, final int off, final int len) {
        buf.append(str, off, off + len);
    }

    /**
     * Flush.
     */
    @Override
    public void flush() { //NOSONAR
        // Do nothing.
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void close() throws IOException { //NOSONAR
        // Do nothing
    }

    /**
     * Return the buffer's current value as a string.
     *
     * @return
     */
    @Override
    public String toString() {
        return buf.toString();
    }
}
