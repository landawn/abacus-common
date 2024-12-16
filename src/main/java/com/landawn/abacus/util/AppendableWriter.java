/*
 * Copyright (C) 2024 HaiYang Li
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

import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

public sealed class AppendableWriter extends Writer permits StringWriter {

    private final Appendable appendable;
    private final boolean flushable;
    private boolean closed;

    /**
     *
     * @param appendable
     * @throws IllegalArgumentException
     */
    public AppendableWriter(final Appendable appendable) throws IllegalArgumentException {
        N.checkArgNotNull(appendable, cs.appendable);

        this.appendable = appendable;
        flushable = appendable instanceof Flushable;
        closed = false;
    }

    /**
     *
     * @param c
     * @return
     * @throws IOException
     */
    @Override
    public Writer append(final char c) throws IOException {
        checkNotClosed();

        appendable.append(c);

        return this;
    }

    /**
     *
     * @param csq
     * @return
     * @throws IOException
     */
    @Override
    public Writer append(final CharSequence csq) throws IOException {
        checkNotClosed();

        appendable.append(csq);

        return this;
    }

    /**
     *
     * @param csq
     * @param start
     * @param end
     * @return
     * @throws IOException
     */
    @Override
    public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
        checkNotClosed();

        appendable.append(csq, start, end);

        return this;
    }

    /**
     *
     * @param c
     * @throws IOException
     */
    @Override
    public void write(final int c) throws IOException {
        checkNotClosed();

        appendable.append((char) c);
    }

    /**
     *
     * @param cbuf
     * @throws IOException
     */
    @Override
    public void write(final char[] cbuf) throws IOException {
        checkNotClosed();

        appendable.append(CharBuffer.wrap(cbuf));
    }

    /**
     *
     * @param cbuf
     * @param off
     * @param len
     * @throws IOException
     */
    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        checkNotClosed();

        appendable.append(CharBuffer.wrap(cbuf), off, off + len);
    }

    /**
     *
     * @param str
     * @throws IOException
     */
    @Override
    public void write(final String str) throws IOException {
        checkNotClosed();

        appendable.append(str);
    }

    /**
     *
     * @param str
     * @param off
     * @param len
     * @throws IOException
     */
    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        checkNotClosed();

        appendable.append(str, off, off + len);
    }

    /**
     *
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {
        checkNotClosed();

        if (flushable) {
            ((Flushable) appendable).flush();
        }
    }

    /**
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;

            flush();

            if (appendable instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) appendable).close();
                } catch (final IOException e) {
                    throw e;
                } catch (final Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        }
    }

    private void checkNotClosed() throws IOException {
        if (closed) {
            throw new IOException("This Writer has been closed");
        }
    }

    /**
     * Return the buffer's current value as a string.
     *
     * @return
     */
    @Override
    public String toString() {
        return appendable.toString();
    }
}
