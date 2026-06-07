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
 * A high-performance string writer implementation built on StringBuilder.
 * Unlike {@link java.io.StringWriter}, this implementation is NOT thread-safe,
 * trading thread safety for better performance in single-threaded scenarios.
 *
 * <p>This class extends {@link AppendableWriter} and uses a StringBuilder as its
 * internal buffer. It provides all standard Writer operations plus additional
 * methods for efficient string building.
 *
 * <p>Key differences from java.io.StringWriter:
 * <ul>
 *   <li>Not thread-safe (no synchronization overhead)</li>
 *   <li>Built on StringBuilder for better performance</li>
 *   <li>Provides direct access to the underlying StringBuilder</li>
 *   <li>Returns <i>this</i> from append methods for method chaining</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * StringWriter writer = new StringWriter();
 * writer.append("Hello")
 *       .append(' ')
 *       .append("World!");
 * String result = writer.toString();   // returns "Hello World!"
 * }</pre>
 *
 * @see AppendableWriter
 * @see java.io.StringWriter
 */
public final class StringWriter extends AppendableWriter {

    private final StringBuilder buf;

    /**
     * Creates a new StringWriter with a default initial capacity.
     * The initial capacity is determined by the StringBuilder's default constructor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringWriter writer = new StringWriter();
     * writer.write("Hello, World!");
     * }</pre>
     *
     */
    public StringWriter() {
        this(new StringBuilder());
    }

    /**
     * Creates a new StringWriter with the specified initial capacity.
     * This can improve performance when the approximate size of the content is known.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // If expecting approximately 1000 characters
     * StringWriter writer = new StringWriter(1000);
     * }</pre>
     *
     * @param initialSize the initial capacity of the internal StringBuilder
     * @throws NegativeArraySizeException if {@code initialSize} is negative
     * @see #StringWriter()
     * @see #StringWriter(StringBuilder)
     */
    public StringWriter(final int initialSize) {
        this(new StringBuilder(initialSize));
    }

    /**
     * Creates a new StringWriter that wraps the provided StringBuilder.
     * Any content already in the StringBuilder will be preserved, and new
     * content will be appended to it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder("Existing content. ");
     * StringWriter writer = new StringWriter(sb);
     * writer.write("New content.");
     * // sb now contains "Existing content. New content."
     * }</pre>
     *
     * @param sb the StringBuilder to use as the internal buffer; must not be {@code null}
     * @throws IllegalArgumentException if {@code sb} is {@code null}
     * @see #stringBuilder()
     */
    public StringWriter(final StringBuilder sb) {
        super(sb);
        buf = sb;
        lock = buf;
    }

    /**
     * Returns the underlying StringBuilder used by this writer.
     * This allows direct manipulation of the buffer when needed.
     *
     * <p>Note: Modifying the returned StringBuilder will affect the
     * content of this StringWriter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringWriter writer = new StringWriter();
     * writer.write("Hello");
     * StringBuilder sb = writer.stringBuilder();
     * sb.reverse();   // Writer now contains "olleH"
     * }</pre>
     *
     * @return the internal StringBuilder buffer
     */
    public StringBuilder stringBuilder() {
        return buf;
    }

    /**
     * Appends a single character to this writer.
     * This method returns the writer itself to allow method chaining.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.append('H').append('i').append('!');
     * }</pre>
     *
     * @param c the character to append
     * @return this StringWriter instance for method chaining
     */
    @Override
    public StringWriter append(final char c) {
        buf.append(c);

        return this;
    }

    /**
     * Appends a character sequence to this writer.
     * If the sequence is {@code null}, the string "null" is appended.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.append("Hello").append(" ").append("World");
     * }</pre>
     *
     * @param csq the character sequence to append, may be null
     * @return this StringWriter instance for method chaining
     */
    @Override
    public StringWriter append(final CharSequence csq) {
        buf.append(csq);

        return this;
    }

    /**
     * Appends a subsequence of the specified character sequence to this writer.
     * Characters from index {@code start} (inclusive) to index {@code end} (exclusive)
     * are appended. If {@code csq} is {@code null}, characters are appended as if
     * {@code csq} contained the four characters {@code "null"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.append("Hello World", 0, 5);   // Appends "Hello"
     * }</pre>
     *
     * @param csq the character sequence from which a subsequence is appended, may be {@code null}
     * @param start the index of the first character in the subsequence (inclusive)
     * @param end the index of the character following the last character in the subsequence (exclusive)
     * @return this StringWriter instance for method chaining
     * @throws IndexOutOfBoundsException if {@code start} or {@code end} are negative,
     *         {@code start} is greater than {@code end}, or {@code end} is greater than
     *         {@code csq.length()}
     */
    @Override
    public StringWriter append(final CharSequence csq, final int start, final int end) {
        buf.append(csq, start, end);

        return this;
    }

    /**
     * Writes a single character to this writer.
     * The character is written as the low-order 16 bits of the integer value;
     * the high-order 16 bits are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write(65);    // Writes 'A'
     * writer.write('B');   // Also valid
     * }</pre>
     *
     * @param c the character to write (as an integer)
     */
    @Override
    public void write(final int c) {
        buf.append((char) c);
    }

    /**
     * Writes an array of characters to this writer.
     * The entire array is written to the internal buffer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * writer.write(chars);
     * }</pre>
     *
     * @param cbuf the character array to write; must not be {@code null}
     * @throws NullPointerException if {@code cbuf} is {@code null}
     */
    @Override
    public void write(final char[] cbuf) {
        buf.append(cbuf);
    }

    /**
     * Writes a portion of a character array to this writer.
     * Characters are written starting at offset {@code off} and
     * writing {@code len} characters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
     * writer.write(chars, 6, 5);   // Writes "World"
     * }</pre>
     *
     * @param cbuf the character array containing data to write
     * @param off the index of the first character in {@code cbuf} to write
     * @param len the number of characters to write
     * @throws IndexOutOfBoundsException if {@code off} is negative, {@code len} is negative,
     *         or {@code off + len} is greater than {@code cbuf.length}
     */
    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        buf.append(cbuf, off, len);
    }

    /**
     * Writes a string to this writer.
     * If the string is {@code null}, the four characters {@code "null"} are written
     * (matching {@link StringBuilder#append(String)} semantics).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Hello, World!");
     * }</pre>
     *
     * @param str the string to write
     */
    @Override
    public void write(final String str) {
        buf.append(str);
    }

    /**
     * Writes a portion of a string to this writer.
     * Characters are written starting at offset {@code off} and
     * writing {@code len} characters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.write("Hello, World!", 7, 5);   // Writes "World"
     * }</pre>
     *
     * @param str the string containing data to write
     * @param off the index of the first character to write
     * @param len the number of characters to write
     * @throws IndexOutOfBoundsException if {@code off} is negative, {@code len} is negative,
     *         or {@code off + len} is greater than {@code str.length()}
     */
    @Override
    public void write(final String str, final int off, final int len) {
        buf.append(str, off, off + len);
    }

    /**
     * Flushes the writer.
     * Since StringWriter writes to an in-memory buffer, this method
     * has no effect and is provided only for compatibility with the
     * Writer interface.
     */
    @Override
    public void flush() { //NOSONAR
        // Do nothing.
    }

    /**
     * Closes the writer.
     * Since StringWriter writes to an in-memory buffer and uses no
     * system resources, this method has no effect. The writer can
     * continue to be used after calling close().
     *
     * <p>This method is provided only for compatibility with the
     * Writer interface.
     */
    @Override
    public void close() { //NOSONAR
        // Do nothing
    }

    /**
     * Returns the current content of the buffer as a string.
     * This method creates a new String from the current content
     * of the internal StringBuilder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringWriter writer = new StringWriter();
     * writer.write("Hello");
     * writer.append(" World!");
     * String result = writer.toString();   // returns "Hello World!"
     * }</pre>
     *
     * @return a string containing the current buffer content
     */
    @Override
    public String toString() {
        return buf.toString();
    }
}
