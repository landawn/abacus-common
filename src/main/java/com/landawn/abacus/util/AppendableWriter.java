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
 * A {@link java.io.Writer} implementation that wraps any {@link Appendable} object.
 * This class adapts any {@code Appendable} (such as {@link StringBuilder},
 * {@link StringBuffer}, or another {@code Writer}) to the {@code Writer} interface,
 * enabling it to be used wherever a {@code Writer} is expected.
 *
 * <p>Flushing is supported if the underlying {@code Appendable} also implements
 * {@link java.io.Flushable}; otherwise {@link #flush()} is a no-op. Closing
 * delegates to the underlying {@code Appendable} if it also implements
 * {@link AutoCloseable}.</p>
 *
 * <p>Once closed, any further write or flush call on an {@code AppendableWriter} throws
 * {@link java.io.IOException}. Its one permitted subclass relaxes this: {@link StringWriter} holds no
 * resource to release, so its {@code close()} is a no-op and the writer stays usable.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * StringBuilder sb = new StringBuilder();
 * try (Writer writer = new AppendableWriter(sb)) {
 *     writer.write("Hello, ");
 *     writer.write("World!");
 * }
 * System.out.println(sb.toString());   // prints "Hello, World!"
 * }</pre>
 *
 * @see StringWriter
 * @see java.io.Writer
 */
public sealed class AppendableWriter extends Writer permits StringWriter {

    private final Appendable appendable;
    private final boolean flushable;
    private boolean closed;

    /**
     * Constructs an {@code AppendableWriter} that wraps the specified {@link Appendable}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * AppendableWriter writer = new AppendableWriter(sb);
     * }</pre>
     *
     * @param appendable the {@code Appendable} to wrap; must not be {@code null}
     * @throws IllegalArgumentException if {@code appendable} is {@code null}.
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
     * @throws IOException if this writer is closed, or appending the characters to the wrapped appendable fails
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
     * <p>If {@code csq} is {@code null}, then the four characters {@code "null"} are appended to this writer.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.append("Hello").append(" ").append("World");
     * }</pre>
     *
     * @param csq the character sequence to append. If {@code csq} is {@code null}, then the four characters {@code "null"} are appended
     * @return this writer
     * @throws IOException if this writer is closed, or appending the characters to the wrapped appendable fails
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
     * writer.append("Hello World", 0, 5);   // Appends "Hello"
     * }</pre>
     *
     * @param csq the character sequence from which a subsequence will be appended.
     *            If {@code csq} is {@code null}, then characters will be appended as if {@code csq} contained the four characters {@code "null"}
     * @param start the index of the first character in the subsequence
     * @param end the index of the character following the last character in the subsequence
     * @return this writer
     * @throws IOException if this writer is closed, or appending the characters to the wrapped appendable fails
     * @throws IndexOutOfBoundsException if start or end are negative, or start is greater than end,
     *         or end is greater than the effective sequence length (four when {@code csq} is {@code null})
     */
    @Override
    public Writer append(final CharSequence csq, final int start, final int end) throws IOException, IndexOutOfBoundsException {
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
     * writer.write(65);   // Writes 'A'
     * }</pre>
     *
     * @param c the int specifying a character to be written
     * @throws IOException if this writer is closed, or appending the characters to the wrapped appendable fails
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
     * @param cbuf the array of characters to write; must not be {@code null}
     * @throws IOException if this writer is closed, or appending the characters to the wrapped appendable fails
     * @throws NullPointerException if {@code cbuf} is {@code null}
     */
    @Override
    public void write(final char[] cbuf) throws IOException, NullPointerException {
        checkNotClosed();

        appendable.append(CharBuffer.wrap(cbuf));
    }

    /**
     * Writes a portion of an array of characters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
     * writer.write(chars, 6, 5);   // Writes "World"
     * }</pre>
     *
     * @param cbuf the array of characters; must not be {@code null}
     * @param off the offset from which to start writing characters
     * @param len the number of characters to write
     * @throws IOException if this writer is closed, or appending the characters to the wrapped appendable fails
     * @throws NullPointerException if {@code cbuf} is {@code null}
     * @throws IndexOutOfBoundsException if off is negative, or len is negative,
     *         or off+len is greater than the length of the given array
     */
    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException, NullPointerException, IndexOutOfBoundsException {
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
     * @param str the string to write; unlike {@code java.io.Writer}, a {@code null} string is
     *            accepted and written as the four characters {@code "null"} (Appendable semantics)
     * @throws IOException if this writer is closed, or appending the characters to the wrapped appendable fails
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
     * writer.write("Hello, World!", 7, 5);   // Writes "World"
     * }</pre>
     *
     * @param str a string; unlike {@code java.io.Writer}, a {@code null} string is accepted and
     *            treated as the four characters {@code "null"} (Appendable semantics)
     * @param off the offset from which to start writing characters
     * @param len the number of characters to write
     * @throws IOException if this writer is closed, or appending the characters to the wrapped appendable fails
     * @throws IndexOutOfBoundsException if off is negative, or len is negative,
     *         or off+len is greater than the effective sequence length (four when {@code str} is {@code null})
     */
    @Override
    public void write(final String str, final int off, final int len) throws IOException, IndexOutOfBoundsException {
        checkNotClosed();

        appendable.append(str, off, off + len);
    }

    /**
     * Flushes the stream.
     *
     * <p>If the underlying {@code Appendable} implements {@link java.io.Flushable},
     * its {@code flush()} method is called. Otherwise, this method does nothing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Important data");
     * writer.flush();   // Ensures data is flushed if supported
     * }</pre>
     *
     * @throws IOException if this writer is closed, or flushing the wrapped appendable fails when it implements {@link Flushable}
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
     * <p>Once the stream has been closed, further {@code write()} or {@code flush()} invocations on an
     * {@code AppendableWriter} cause an {@link java.io.IOException}; see {@link StringWriter#close()},
     * which overrides this method and leaves the writer usable. Closing a previously closed stream
     * has no effect.</p>
     *
     * <p>If the underlying {@code Appendable} implements {@link AutoCloseable},
     * its {@code close()} method will be called. Non-{@link java.io.IOException} exceptions
     * thrown by the underlying {@code close()} are rethrown as unchecked exceptions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (AppendableWriter writer = new AppendableWriter(appendable)) {
     *     writer.write("Data");
     * }
     * }</pre>
     *
     * @throws IOException if the first failure while flushing or closing is an {@code IOException}
     * @throws RuntimeException if the first failure while flushing or closing is a runtime exception,
     *         or a non-I/O checked exception that is converted to a runtime exception
     */
    @Override
    public void close() throws IOException, RuntimeException {
        if (!closed) {
            // Mark closed first so a flush failure doesn't leave the stream
            // in a state where subsequent close() retries can leak the
            // underlying Appendable. The underlying close is still attempted
            // in a finally block so resources are released even if flush throws.
            closed = true;

            Throwable exception = null;

            try {
                if (flushable) {
                    ((Flushable) appendable).flush();
                }
            } catch (final Throwable e) {
                exception = e;
            }

            try {
                if (appendable instanceof AutoCloseable) {
                    ((AutoCloseable) appendable).close();
                }
            } catch (final Throwable e) {
                if (exception == null) {
                    exception = e;
                } else if (exception != e) {
                    // Throwable rejects self-suppression; an underlying implementation may
                    // reuse the same failure instance for flush() and close().
                    exception.addSuppressed(e);
                }
            }

            if (exception instanceof IOException) {
                throw (IOException) exception;
            } else if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else if (exception instanceof Error) {
                throw (Error) exception;
            } else if (exception instanceof Exception) {
                throw ExceptionUtil.toRuntimeException((Exception) exception, true);
            } else if (exception != null) {
                throw new RuntimeException(exception);
            }
        }
    }

    /**
     * @throws IOException if this writer has been closed.
     */
    private void checkNotClosed() throws IOException {
        if (closed) {
            throw new IOException("This Writer has been closed");
        }
    }

    /**
     * Returns the current content of the underlying {@code Appendable} as a string.
     *
     * <p>This method delegates to {@code toString()} on the wrapped {@code Appendable} object.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * AppendableWriter writer = new AppendableWriter(sb);
     * writer.write("Hello");
     * System.out.println(writer.toString());   // prints "Hello"
     * }</pre>
     *
     * @return the string representation of the underlying {@code Appendable}
     */
    @Override
    public String toString() {
        return appendable.toString();
    }
}
