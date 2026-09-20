/*
 * Copyright (C) 2018 HaiYang Li
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

package com.landawn.abacus.type;

import java.io.BufferedWriter;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Objectory;

/**
 * Internal utility class providing shared parser instances and configurations for the type system.
 * This class provides centralized access to commonly used parsers and serialization
 * configurations throughout the type package.
 *
 * <p>This class is package-private and intended for internal use only within the type system.
 * It provides singleton instances of:</p>
 * <ul>
 *   <li>JSON parser for serialization/deserialization</li>
 *   <li>XML parser (if available in classpath)</li>
 *   <li>Default JSON serialization configuration</li>
 *   <li>Default JSON deserialization configuration</li>
 * </ul>
 */
final class Utils {

    // Parsers are created eagerly via ParserFactory. xmlParser is guarded by isXmlParserAvailable() so a
    // missing XML library yields null instead of a NoClassDefFoundError when this class initializes.
    /**
     * Shared JSON parser instance for use throughout the type system.
     */
    static final JsonParser jsonParser = ParserFactory.createJsonParser();

    /**
     * Shared XML parser instance for use throughout the type system.
     * Will be {@code null} if XML parsing libraries are not available in the classpath.
     */
    static final XmlParser xmlParser = ParserFactory.isXmlParserAvailable() ? ParserFactory.createXmlParser() : null;

    /**
     * Default JSON serialization configuration used by type converters.
     * Created with standard settings suitable for most type conversions.
     */
    static final JsonSerConfig jsc = JsonSerConfig.create();

    /**
     * Default JSON deserialization configuration used by type converters.
     * Created with standard settings suitable for most type conversions.
     */
    static final JsonDeserConfig jdc = JsonDeserConfig.create();

    /**
     * Parses each tuple slot directly from its original JSON token with the declared type.
     * An Object[] first pass would already have rounded decimal tokens before their types were known.
     */
    static Object[] parseTupleElements(final String source, final String typeName, final List<Type<?>> types) {
        int from = 0;
        int end = source.length();

        while (from < end && Character.isWhitespace(source.charAt(from))) {
            from++;
        }

        while (end > from && Character.isWhitespace(source.charAt(end - 1))) {
            end--;
        }

        final int arity = types.size();

        if (end == from) {
            throw malformedTuple(typeName, "the value is blank");
        }

        if (end - from < 2 || source.charAt(from) != '[' || source.charAt(end - 1) != ']') {
            throw malformedTuple(typeName, "not an array: the value must start with '[' and end with ']'");
        }

        final int[] starts = new int[arity];
        final int[] ends = new int[arity];
        final Deque<Character> nesting = new ArrayDeque<>();
        int count = 0;
        int start = from + 1;
        char quote = 0;
        boolean escaped = false;

        // Locate slots without interpreting their numbers, escaped strings, or nested containers.
        for (int i = start; i < end - 1; i++) {
            final char ch = source.charAt(i);

            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == quote) {
                    quote = 0;
                }
            } else if (ch == '"' || ch == '\'') {
                quote = ch;
            } else if (ch == '[' || ch == '{') {
                nesting.push(ch);
            } else if (ch == ']' || ch == '}') {
                final char opener = ch == ']' ? '[' : '{';

                if (nesting.isEmpty()) {
                    throw malformedTuple(typeName, "unbalanced brackets: '" + ch + "' at index " + i + " has no matching '" + opener + "'");
                }

                final char opened = nesting.pop();

                if (opened != opener) {
                    throw malformedTuple(typeName, "unbalanced brackets: '" + ch + "' at index " + i + " does not close the enclosing '" + opened + "'");
                }
            } else if (ch == ',' && nesting.isEmpty()) {
                // Keep counting past the arity so the element-count message below can report how many were found.
                if (count < arity) {
                    starts[count] = start;
                    ends[count] = i;
                }

                count++;
                start = i + 1;
            }
        }

        if (quote != 0) {
            throw malformedTuple(typeName, "unterminated quoted value: no closing " + quote);
        }

        if (!nesting.isEmpty()) {
            throw malformedTuple(typeName, "unbalanced brackets: unclosed '" + nesting.peek() + "'");
        }

        // "[]" and "[  ]" hold no element at all; every other input has one more slot than separators.
        final int found = count == 0 && isBlankRange(source, start, end - 1) ? 0 : count + 1;

        if (found != arity) {
            throw malformedTuple(typeName, "expected exactly " + arity + (arity == 1 ? " element" : " elements") + " but found " + found);
        }

        starts[count] = start;
        ends[count] = end - 1;
        final Object[] values = new Object[arity];

        for (int i = 0; i < values.length; i++) {
            while (starts[i] < ends[i] && Character.isWhitespace(source.charAt(starts[i]))) {
                starts[i]++;
            }

            while (ends[i] > starts[i] && Character.isWhitespace(source.charAt(ends[i] - 1))) {
                ends[i]--;
            }

            if (starts[i] == ends[i]) {
                throw malformedTuple(typeName, "empty element at index " + i);
            }

            final String token = source.substring(starts[i], ends[i]);

            // Root scalar deserialization accepts raw type text, not a JSON literal. A typed
            // singleton list invokes JSON decoding while keeping each slot's declared type.
            // Preserve literal null even for primitive/optional type descriptors.
            if (!"null".equals(token)) {
                final Type<List<Object>> slotListType = TypeFactory.getType("List<" + types.get(i).name() + ">");
                values[i] = jsonParser.deserialize("[" + token + "]", jdc, slotListType).get(0);
            }
        }

        return values;
    }

    private static boolean isBlankRange(final String source, final int from, final int to) {
        for (int i = from; i < to; i++) {
            if (!Character.isWhitespace(source.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    // Each rejection names the condition that actually failed: one shared "wrong element count" message
    // reported an unterminated quote or an unbalanced bracket as an arity problem.
    private static IllegalArgumentException malformedTuple(final String typeName, final String reason) {
        return new IllegalArgumentException("Invalid " + typeName + " format: " + reason);
    }

    // The standard JSON writer escapes double quotes. Single-quoted JSON also needs
    // apostrophes escaped; keep XML escaping and the usual double-quoted path intact.
    /**
     * @throws IOException if writing the escaped string content to {@code writer} fails
     */
    static void writeStringContent(final CharacterWriter writer, final String value, final char quotation) throws IOException {
        if (quotation != '\'' || !(writer instanceof BufferedJsonWriter) || value == null || value.indexOf('\'') < 0) {
            writer.writeCharacter(value);
            return;
        }

        int start = 0;

        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '\'') {
                writer.writeCharacter(value, start, i - start);
                writer.write("\\'");
                start = i + 1;
            }
        }

        writer.writeCharacter(value, start, value.length() - start);
    }

    /**
     * @throws IOException if writing the escaped string content to {@code writer} fails
     */
    static void writeStringContent(final CharacterWriter writer, final char[] value, final int offset, final int length, final char quotation)
            throws IOException {
        if (quotation != '\'' || !(writer instanceof BufferedJsonWriter)) {
            writer.writeCharacter(value, offset, length);
            return;
        }

        int start = offset;
        final int end = offset + length;

        for (int i = offset; i < end; i++) {
            if (value[i] == '\'') {
                writer.writeCharacter(value, start, i - start);
                writer.write("\\'");
                start = i + 1;
            }
        }

        writer.writeCharacter(value, start, end - start);
    }

    /**
     * Opens a binary stream and transfers ownership of the supplied {@link Blob} locator
     * to the returned stream. Closing the stream closes the delegate and releases the locator.
     * If opening fails, the locator is freed before the failure propagates.
     *
     * @param blob the {@link Blob} whose binary stream to open; may be {@code null}
     * @return an owning {@link InputStream} over the Blob's content, or {@code null} if {@code blob}
     *         is {@code null} or exposes no binary stream (in which case the locator is freed)
     * @throws SQLException if the binary stream cannot be obtained
     */
    static InputStream openBinaryStream(final Blob blob) throws SQLException {
        if (blob == null) {
            return null;
        }

        final InputStream stream;

        try {
            stream = blob.getBinaryStream();
        } catch (final SQLException | RuntimeException | Error e) {
            freeAfterFailure(blob, e);
            throw e;
        }

        if (stream == null) {
            blob.free();
            return null;
        }

        return new FilterInputStream(stream) {
            private boolean closed;

            /**
             * @throws IOException if closing the underlying stream or reader fails, or releasing its JDBC locator fails after a successful close
             */
            @Override
            public synchronized void close() throws IOException {
                if (!closed) {
                    closed = true;
                    closeAndFree(super::close, blob::free, "Blob");
                }
            }
        };
    }

    /**
     * Opens an ASCII stream and transfers ownership of the supplied {@link Clob} locator
     * to the returned stream. Closing the stream closes the delegate and releases the locator.
     * If opening fails, the locator is freed before the failure propagates.
     *
     * @param clob the {@link Clob} whose ASCII stream to open; may be {@code null}
     * @return an owning {@link InputStream} over the Clob's content, or {@code null} if {@code clob}
     *         is {@code null} or exposes no ASCII stream (in which case the locator is freed)
     * @throws SQLException if the ASCII stream cannot be obtained
     */
    static InputStream openAsciiStream(final Clob clob) throws SQLException {
        if (clob == null) {
            return null;
        }

        final InputStream stream;

        try {
            stream = clob.getAsciiStream();
        } catch (final SQLException | RuntimeException | Error e) {
            freeAfterFailure(clob, e);
            throw e;
        }

        if (stream == null) {
            clob.free();
            return null;
        }

        return new FilterInputStream(stream) {
            private boolean closed;

            /**
             * @throws IOException if closing the underlying stream or reader fails, or releasing its JDBC locator fails after a successful close
             */
            @Override
            public synchronized void close() throws IOException {
                if (!closed) {
                    closed = true;
                    closeAndFree(super::close, clob::free, "Clob");
                }
            }
        };
    }

    /**
     * Opens a character stream and transfers ownership of the supplied {@link Clob} locator
     * to the returned reader. Closing the reader closes the delegate and releases the locator.
     * If opening fails, the locator is freed before the failure propagates.
     *
     * @param clob the {@link Clob} whose character stream to open; may be {@code null}
     * @return an owning {@link Reader} over the Clob's content, or {@code null} if {@code clob}
     *         is {@code null} or exposes no character stream (in which case the locator is freed)
     * @throws SQLException if the character stream cannot be obtained
     */
    static Reader openCharacterStream(final Clob clob) throws SQLException {
        if (clob == null) {
            return null;
        }

        final Reader reader;

        try {
            reader = clob.getCharacterStream();
        } catch (final SQLException | RuntimeException | Error e) {
            freeAfterFailure(clob, e);
            throw e;
        }

        if (reader == null) {
            clob.free();
            return null;
        }

        return new FilterReader(reader) {
            private boolean closed;

            /**
             * @throws IOException if closing the underlying stream or reader fails, or releasing its JDBC locator fails after a successful close
             */
            @Override
            public synchronized void close() throws IOException {
                if (!closed) {
                    closed = true;
                    closeAndFree(super::close, clob::free, "Clob");
                }
            }
        };
    }

    /**
     * Recycles a temporary buffered writer without masking a failure from the write operation.
     * If recycling is the only failing operation, an underlying checked I/O cause is rethrown as
     * {@link IOException}; otherwise the recycle failure is suppressed on {@code primaryFailure}.
     *
     * @param writer the temporary buffered writer to recycle
     * @param primaryFailure the failure already raised by the write operation, or {@code null} if the
     *                       write succeeded
     * @throws IOException if recycling fails with an underlying checked I/O cause and
     *                     {@code primaryFailure} is {@code null}
     */
    static void recycle(final BufferedWriter writer, final Throwable primaryFailure) throws IOException {
        try {
            Objectory.recycle(writer);
        } catch (final UncheckedIOException e) {
            final Throwable recycleFailure = e.getCause() == null ? e : e.getCause();

            if (primaryFailure == null) {
                if (recycleFailure instanceof IOException ioException) {
                    throw ioException;
                }

                throw e;
            } else if (primaryFailure != recycleFailure) {
                primaryFailure.addSuppressed(recycleFailure);
            }
        } catch (final RuntimeException | Error e) {
            if (primaryFailure == null) {
                throw e;
            } else if (primaryFailure != e) {
                primaryFailure.addSuppressed(e);
            }
        }
    }

    /**
     * Recycles an in-memory JSON writer without masking a failure from serialization.
     * A cleanup failure is rethrown when it is the only failure, or suppressed on the
     * serialization failure otherwise.
     *
     * @param writer the in-memory JSON writer to recycle
     * @param primaryFailure the failure already raised by serialization, or {@code null} if
     *                       serialization succeeded
     */
    static void recycle(final BufferedJsonWriter writer, final Throwable primaryFailure) {
        try {
            Objectory.recycle(writer);
        } catch (final RuntimeException | Error e) {
            if (primaryFailure == null) {
                throw e;
            } else if (primaryFailure != e) {
                primaryFailure.addSuppressed(e);
            }
        }
    }

    /**
     * @throws IOException if closing the resource throws IOException, or releasing its JDBC locator throws SQLException after a successful close
     */
    private static void closeAndFree(final CloseAction closeAction, final FreeAction freeAction, final String lobType) throws IOException {
        Throwable failure = null;

        try {
            closeAction.close();
        } catch (final IOException | RuntimeException | Error e) {
            failure = e;
        }

        try {
            freeAction.free();
        } catch (final SQLException e) {
            if (failure == null) {
                failure = new IOException("Failed to release " + lobType + " resources", e);
            } else {
                failure.addSuppressed(e);
            }
        } catch (final RuntimeException | Error e) {
            if (failure == null) {
                failure = e;
            } else if (failure != e) {
                failure.addSuppressed(e);
            }
        }

        if (failure instanceof IOException e) {
            throw e;
        } else if (failure instanceof RuntimeException e) {
            throw e;
        } else if (failure instanceof Error e) {
            throw e;
        }
    }

    private static void freeAfterFailure(final Blob blob, final Throwable failure) {
        try {
            blob.free();
        } catch (final SQLException | RuntimeException | Error e) {
            if (failure != e) {
                failure.addSuppressed(e);
            }
        }
    }

    private static void freeAfterFailure(final Clob clob, final Throwable failure) {
        try {
            clob.free();
        } catch (final SQLException | RuntimeException | Error e) {
            if (failure != e) {
                failure.addSuppressed(e);
            }
        }
    }

    @FunctionalInterface
    private interface CloseAction {
        /**
         * @throws IOException if closing the stream or reader obtained from the JDBC large object fails
         */
        void close() throws IOException;
    }

    @FunctionalInterface
    private interface FreeAction {
        /**
         * @throws SQLException if the JDBC driver cannot release the large object's locator resources
         */
        void free() throws SQLException;
    }

    private Utils() {
        // Utility class - prevent instantiation
    }
}
