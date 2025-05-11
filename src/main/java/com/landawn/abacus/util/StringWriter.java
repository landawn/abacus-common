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

/**
 * Built on {@code StringBuilder} for better performance. But not like <code/>java.io.StringWriter</code>. It's not multi-thread safety.
 *
 */
public final class StringWriter extends AppendableWriter {

    private final StringBuilder buf;

    public StringWriter() {
        this(new StringBuilder());
    }

    /**
     *
     * @param initialSize
     */
    public StringWriter(final int initialSize) {
        this(new StringBuilder(initialSize));
    }

    /**
     *
     * @param sb
     */
    public StringWriter(final StringBuilder sb) {
        super(sb);
        buf = sb;
        lock = buf;
    }

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
     */
    @Override
    public StringWriter append(final CharSequence csq) {
        buf.append(csq);

        return this;
    }

    /**
     *
     * @param csq
     * @param start
     * @param end
     * @return
     */
    @Override
    public StringWriter append(final CharSequence csq, final int start, final int end) {
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
     * @param cbuf
     */
    @Override
    public void write(final char[] cbuf) {
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
     */
    @Override
    public void close() { //NOSONAR
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
