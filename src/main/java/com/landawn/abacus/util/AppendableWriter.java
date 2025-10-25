/*
 * Copyright (C) 2024 HaiYang Li
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

import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * A Writer implementation that wraps an Appendable object.
 * This class adapts any Appendable (such as StringBuilder, StringBuffer, or Writer)
 * to the Writer interface, enabling it to be used wherever a Writer is expected.
 * 
 * <p>The class automatically handles flushing if the underlying Appendable
 * implements Flushable, and closing if it implements AutoCloseable.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * StringBuilder sb = new StringBuilder();
 * try (Writer writer = new AppendableWriter(sb)) {
 *     writer.write("Hello, ");
 *     writer.write("World!");
 * }
 * System.out.println(sb.toString()); // "Hello, World!"
 * }</pre>
 * 
 * @see StringWriter
 * @since 1.0
 */
public sealed class AppendableWriter extends Writer permits StringWriter {

    private final Appendable appendable;
    private final boolean flushable;
    private boolean closed;

    /**
     * Constructs an AppendableWriter that wraps the specified Appendable.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * AppendableWriter writer = new AppendableWriter(sb);
     * }</pre>
     *
     * @param appendable the Appendable to wrap, must not be null
     * @throws IllegalArgumentException if appendable is null
     */
    public AppendableWriter(final Appendable appendable) throws IllegalArgumentException {
        N.checkArgNotNull(appendable, cs.appendable);

        this.appendable = appendable;
        flushable = appendable instanceof Flushable;
        closed = false;
    }

    /**
     * Appends the specified character to this writer.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.append('A').append('B').append('C');
     * }</pre>
     *
     * @param c the character to append
     * @return this writer
     * @throws IOException if an I/O error occurs or if the writer has been closed
     */
    @Override
    public Writer append(final char c) throws IOException {
        checkNotClosed();

        appendable.append(c);

        return this;
    }

    /**
     * Appends the specified character sequence to this writer.
     * 
     * <p>If csq is null, then the four characters "null" are appended to this writer.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.append("Hello").append(" ").append("World");
     * }</pre>
     *
     * @param csq the character sequence to append. If csq is null, then the four characters "null" are appended
     * @return this writer
     * @throws IOException if an I/O error occurs or if the writer has been closed
     */
    @Override
    public Writer append(final CharSequence csq) throws IOException {
        checkNotClosed();

        appendable.append(csq);

        return this;
    }

    /**
     * Appends a subsequence of the specified character sequence to this writer.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.append("Hello World", 0, 5); // Appends "Hello"
     * }</pre>
     *
     * @param csq the character sequence from which a subsequence will be appended. 
     *            If csq is null, then characters will be appended as if csq contained the four characters "null"
     * @param start the index of the first character in the subsequence
     * @param end the index of the character following the last character in the subsequence
     * @return this writer
     * @throws IOException if an I/O error occurs or if the writer has been closed
     * @throws IndexOutOfBoundsException if start or end are negative, or start is greater than end, 
     *         or end is greater than csq.length()
     */
    @Override
    public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
        checkNotClosed();

        appendable.append(csq, start, end);

        return this;
    }

    /**
     * Writes a single character.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write('X');
     * writer.write(65); // Writes 'A'
     * }</pre>
     *
     * @param c the int specifying a character to be written
     * @throws IOException if an I/O error occurs or if the writer has been closed
     */
    @Override
    public void write(final int c) throws IOException {
        checkNotClosed();

        appendable.append((char) c);
    }

    /**
     * Writes an array of characters.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * writer.write(chars);
     * }</pre>
     *
     * @param cbuf the array of characters to write
     * @throws IOException if an I/O error occurs or if the writer has been closed
     */
    @Override
    public void write(final char[] cbuf) throws IOException {
        checkNotClosed();

        appendable.append(CharBuffer.wrap(cbuf));
    }

    /**
     * Writes a portion of an array of characters.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
     * writer.write(chars, 6, 5); // Writes "World"
     * }</pre>
     *
     * @param cbuf the array of characters
     * @param off the offset from which to start writing characters
     * @param len the number of characters to write
     * @throws IOException if an I/O error occurs or if the writer has been closed
     * @throws IndexOutOfBoundsException if off is negative, or len is negative, 
     *         or off+len is greater than the length of the given array
     */
    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        checkNotClosed();

        appendable.append(CharBuffer.wrap(cbuf), off, off + len);
    }

    /**
     * Writes a string.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Hello, World!");
     * }</pre>
     *
     * @param str the string to write
     * @throws IOException if an I/O error occurs or if the writer has been closed
     */
    @Override
    public void write(final String str) throws IOException {
        checkNotClosed();

        appendable.append(str);
    }

    /**
     * Writes a portion of a string.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Hello, World!", 7, 5); // Writes "World"
     * }</pre>
     *
     * @param str a string
     * @param off the offset from which to start writing characters
     * @param len the number of characters to write
     * @throws IOException if an I/O error occurs or if the writer has been closed
     * @throws IndexOutOfBoundsException if off is negative, or len is negative, 
     *         or off+len is greater than the length of the given string
     */
    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        checkNotClosed();

        appendable.append(str, off, off + len);
    }

    /**
     * Flushes the stream.
     * 
     * <p>If the underlying Appendable implements Flushable, its flush method will be called.
     * Otherwise, this method does nothing.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Important data");
     * writer.flush(); // Ensures data is flushed if supported
     * }</pre>
     *
     * @throws IOException if an I/O error occurs or if the writer has been closed
     */
    @Override
    public void flush() throws IOException {
        checkNotClosed();

        if (flushable) {
            ((Flushable) appendable).flush();
        }
    }

    /**
     * Closes the stream, flushing it first.
     * 
     * <p>Once the stream has been closed, further write() or flush() invocations
     * will cause an IOException to be thrown. Closing a previously closed stream has no effect.</p>
     * 
     * <p>If the underlying Appendable implements AutoCloseable, its close method will be called.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (AppendableWriter writer = new AppendableWriter(appendable)) {
     *     writer.write("Data");
     * } // Automatically closed
     * }</pre>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (!closed) {
            flush();

            closed = true;

            if (appendable instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) appendable).close();
                } catch (final IOException e) {
                    throw e;
                } catch (final Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
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
     * Returns the current content of the underlying Appendable as a string.
     * 
     * <p>This method calls toString() on the wrapped Appendable object.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * AppendableWriter writer = new AppendableWriter(sb);
     * writer.write("Hello");
     * System.out.println(writer.toString()); // "Hello"
     * }</pre>
     *
     * @return the string representation of the underlying Appendable
     */
    @Override
    public String toString() {
        return appendable.toString();
    }
}