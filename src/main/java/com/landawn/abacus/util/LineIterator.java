/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.stream.Stream;

/**
 * An Iterator over the lines in a {@code Reader}.
 * <p>
 * {@code LineIterator} holds a reference to an open {@code Reader}.
 * When you have finished with the iterator, you should close the reader
 * to free internal resources. This can be done by closing the reader directly,
 * by calling {@link #close()} on the iterator, by closing the {@link #stream()} built from it,
 * or by passing the iterator to {@link IOUtil#closeQuietly(AutoCloseable)}.
 * The iterator is stateful and is not safe for concurrent traversal without external synchronization.
 *
 * <p><b>Reading every line does not close anything.</b> Reaching end-of-stream only marks the iterator
 * finished; the {@code Reader} stays open until one of the actions above is taken. Draining the iterator
 * with {@code toList()}, {@code count()} or a {@code while (hasNext())} loop therefore still leaves the
 * file handle open.</p>
 *
 * <p><b>Transformations do not carry the resource.</b> The inherited operations - {@link #filter},
 * {@link #map}, {@link #skip}, {@link #limit}, {@link #skipNulls}, {@link #distinct} and friends - return
 * a plain {@link ObjIterator}, which is <i>not</i> {@link AutoCloseable}. So a chain such as
 * {@code LineIterator.of(file).filter(..).toList()} leaves no way to release the reader, because the
 * {@code LineIterator} itself is never held by the caller. Either keep the {@code LineIterator} in a
 * try-with-resources and transform inside it, or start from {@link #stream()}, which does own this
 * iterator and closes it:</p>
 * <pre>{@code
 * // leaks the file handle - nothing here is closeable
 * List<String> bad = LineIterator.of(file).filter(l -> l.contains("x")).toList();
 *
 * // either hold the iterator ...
 * try (LineIterator it = LineIterator.of(file, StandardCharsets.UTF_8)) {
 *     List<String> good = it.filter(l -> l.contains("x")).toList();
 * }
 *
 * // ... or let the stream own it
 * try (Stream<String> lines = LineIterator.of(file, StandardCharsets.UTF_8).stream()) {
 *     List<String> alsoGood = lines.filter(l -> l.contains("x")).toList();
 * }
 * }</pre>
 *
 * <p>The recommended usage pattern is:</p>
 * <pre>{@code
 * try(LineIterator it = LineIterator.of(file, StandardCharsets.UTF_8)) {
 *   while (it.hasNext()) {
 *     String line = it.next();
 *     // do something with line
 *   }
 * }
 * }</pre>
 * <p>
 * This class was copied from Apache Commons IO and may be modified.
 *
 * @see java.io.BufferedReader
 */
public final class LineIterator extends ObjIterator<String> implements AutoCloseable {
    // N.B. This class deliberately does not implement Iterable, see https://issues.apache.org/jira/browse/IO-181

    private final BufferedReader br;
    private String cachedLine;
    /** A flag indicating if the iterator has been fully read. */
    private boolean finished = false;

    private boolean isClosed = false;

    /**
     * Constructs an iterator of the lines for a {@code Reader}.
     * <p>
     * The reader will be wrapped in a {@link BufferedReader} if it's not already one.
     * This constructor does not close the reader automatically; it is the caller's
     * responsibility to close the reader or call {@link #close()} on this iterator
     * to release resources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new FileReader("data.txt");
     * try (LineIterator it = new LineIterator(reader)) {
     *     while (it.hasNext()) {
     *         System.out.println(it.next());
     *     }
     * }
     * }</pre>
     *
     * @param reader the {@code Reader} to read from, must not be null
     * @throws IllegalArgumentException if the reader is null.
     */
    public LineIterator(final Reader reader) throws IllegalArgumentException {
        N.checkArgNotNull(reader, cs.reader);

        if (reader instanceof BufferedReader) {
            br = (BufferedReader) reader;
        } else {
            br = new BufferedReader(reader);
        }
    }

    /**
     * Returns an Iterator for the lines in a {@code File} using the library default UTF-8 encoding.
     * <p>
     * This method opens an {@code InputStream} for the file and wraps it in a {@code Reader}
     * using UTF-8. When you have finished with the iterator,
     * you should close it to free internal resources. This can be done by calling the
     * {@link #close()} method or by using try-with-resources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (LineIterator it = LineIterator.of(new File("data.txt"))) {
     *     while (it.hasNext()) {
     *         String line = it.next();
     *         // do something with line
     *     }
     * }
     * }</pre>
     *
     * <p>
     * If an exception occurs during the creation of the iterator, the underlying stream
     * is automatically closed.
     *
     * @param file the file to open for input, must not be {@code null}
     * @return an Iterator of the lines in the file, never {@code null}
     * @throws IllegalArgumentException if {@code file} is {@code null} or denotes a directory.
     * @throws UncheckedIOException in case of an I/O error (e.g., file not found or cannot be read)
     * @see #of(File, Charset)
     */
    public static LineIterator of(final File file) throws IllegalArgumentException, UncheckedIOException {
        return of(file, IOUtil.DEFAULT_CHARSET);
    }

    /**
     * Returns an Iterator for the lines in a {@code File} using the specified character encoding.
     * <p>
     * This method opens an {@code InputStream} for the file and wraps it in a {@code Reader}
     * using the specified character encoding. When you have finished with the iterator,
     * you should close it to free internal resources. This can be done by calling the
     * {@link #close()} method or by using try-with-resources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (LineIterator it = LineIterator.of(new File("data.txt"), StandardCharsets.UTF_8)) {
     *     it.forEachRemaining(line -> processLine(line));
     * }
     * }</pre>
     *
     * <p>
     * If an exception occurs during the creation of the iterator, the underlying stream
     * is automatically closed to prevent resource leaks.
     *
     * @param file the file to open for input; must not be {@code null}
     * @param encoding the character encoding to use; must not be {@code null}
     * @return an Iterator of the lines in the file, never {@code null}
     * @throws IllegalArgumentException if {@code file} or {@code encoding} is {@code null}, or {@code file} denotes a directory.
     * @throws UncheckedIOException in case of an I/O error (e.g., file not found or cannot be read)
     * @see #of(File)
     */
    public static LineIterator of(final File file, final Charset encoding) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(encoding, cs.encoding);

        Reader reader = null;
        boolean noException = false;

        try {
            reader = IOUtil.newFileReader(file, encoding);

            final LineIterator iter = of(reader);

            noException = true;

            return iter;
        } finally {
            if (!noException && reader != null) {
                IOUtil.closeQuietly(reader);
            }
        }
    }

    /**
     * Returns an Iterator for the lines in an {@code InputStream}, using the library default
     * UTF-8 encoding.
     * <p>
     * {@code LineIterator} holds a reference to the open {@code InputStream} specified here.
     * When you have finished with the iterator, you should close it to free internal resources.
     * This can be done by calling {@link #close()} or by using try-with-resources.
     * Note that closing the iterator will also close the underlying input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (LineIterator lines = LineIterator.of(inputStream)) {
     *     while (lines.hasNext()) {
     *         processLine(lines.next());
     *     }
     * }
     * }</pre>
     *
     * @param input the {@code InputStream} to read from, must not be null
     * @return an Iterator of the lines in the input stream, never null
     * @throws IllegalArgumentException if {@code input} is {@code null}.
     * @see #of(InputStream, Charset)
     */
    public static LineIterator of(final InputStream input) throws IllegalArgumentException {
        return of(input, IOUtil.DEFAULT_CHARSET);
    }

    /**
     * Returns an Iterator for the lines in an {@code InputStream}, using the specified
     * character encoding.
     * <p>
     * {@code LineIterator} holds a reference to the open {@code InputStream} specified here.
     * When you have finished with the iterator, you should close it to free internal resources.
     * This can be done by calling {@link #close()} or by using try-with-resources.
     * Note that closing the iterator will also close the underlying input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (LineIterator it = LineIterator.of(inputStream, StandardCharsets.UTF_8)) {
     *     it.stream().forEach(line -> processLine(line));
     * }
     * }</pre>
     *
     * @param input the {@code InputStream} to read from; must not be {@code null}
     * @param encoding the character encoding to use; must not be {@code null}
     * @return an Iterator of the lines in the input stream, never {@code null}
     * @throws IllegalArgumentException if {@code input} or {@code encoding} is {@code null}.
     * @see #of(InputStream)
     */
    public static LineIterator of(final InputStream input, final Charset encoding) throws IllegalArgumentException {
        N.checkArgNotNull(input, cs.inputStream);
        N.checkArgNotNull(encoding, cs.encoding);

        return new LineIterator(IOUtil.createReader(input, encoding));
    }

    /**
     * Returns an Iterator for the lines in a {@code Reader}.
     * <p>
     * {@code LineIterator} holds a reference to the open {@code Reader} specified here.
     * When you have finished with the iterator, you should close it to free internal resources.
     * This can be done by calling {@link #close()} or by using try-with-resources.
     * Note that closing the iterator will also close the underlying reader.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (LineIterator lines = LineIterator.of(reader)) {
     *     lines.stream().filter(line -> line.contains("keyword"))
     *                   .forEach(System.out::println);
     * }
     * }</pre>
     *
     * @param reader the {@code Reader} to read from, must not be null
     * @return an Iterator of the lines in the reader, never null
     * @throws IllegalArgumentException if the reader is null.
     * @see #LineIterator(Reader)
     */
    public static LineIterator of(final Reader reader) throws IllegalArgumentException {
        return new LineIterator(reader);
    }

    //-----------------------------------------------------------------------

    /**
     * Indicates whether the {@code Reader} has more lines available to read.
     * <p>
     * This method checks if there are more lines in the underlying reader by attempting
     * to read the next line and caching it. If an {@code IOException} occurs during this
     * operation, {@link #close()} will be automatically called on this instance to release
     * resources, and the exception will be wrapped in an {@link UncheckedIOException}.
     * Any failure escaping cleanup is suppressed on that wrapper.
     * <p>
     * Once this method returns {@code false}, subsequent calls will continue to return
     * {@code false}, and calls to {@link #next()} will throw {@link NoSuchElementException}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LineIterator it = LineIterator.of(file);
     * while (it.hasNext()) {
     *     String line = it.next();
     *     // process line
     * }
     * }</pre>
     *
     * @return {@code true} if the reader has more lines, {@code false} if end of stream is reached
     * @throws UncheckedIOException if reading the next line from the underlying reader fails
     */
    @Override
    public boolean hasNext() throws UncheckedIOException {
        if (cachedLine != null) {
            return true;
        } else if (finished) {
            return false;
        } else {
            try {
                cachedLine = br.readLine();

                if (cachedLine == null) {
                    finished = true;
                    return false;
                } else {
                    return true;
                }
            } catch (final IOException ioe) {
                final UncheckedIOException failure = new UncheckedIOException(ioe);
                try {
                    close();
                } catch (final Throwable cleanupFailure) {
                    // Keep the read failure primary even if cleanup itself fails.
                    failure.addSuppressed(cleanupFailure);
                }
                throw failure;
            }
        }
    }

    /**
     * Returns the next line in the wrapped {@code Reader}.
     * <p>
     * This method retrieves the next line from the underlying reader. It returns the line
     * that was previously cached by {@link #hasNext()}, or attempts to read a new line if
     * {@code hasNext()} hasn't been called. The returned line does not include any
     * line-termination characters (such as {@code \n} or {@code \r\n}).
     * <p>
     * Calling {@link #hasNext()} first is not required, since this method invokes it
     * internally; however, if no more lines are available a {@link NoSuchElementException}
     * is thrown, so either guard the call with {@link #hasNext()} or be prepared to handle
     * that exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LineIterator it = LineIterator.of(file);
     * while (it.hasNext()) {
     *     String line = it.next();
     *     System.out.println(line);
     * }
     * }</pre>
     *
     * @return the next line from the input, never {@code null} (empty lines are returned as empty strings)
     * @throws UncheckedIOException if reading the next line from the underlying reader fails
     * @throws NoSuchElementException if there is no line to return (end of stream reached)
     */
    @Override
    public String next() throws UncheckedIOException, NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
        final String res = cachedLine;
        cachedLine = null;
        return res;
    }

    /**
     * Returns a {@link Stream} over the remaining lines that <b>owns this iterator</b>: closing the
     * stream (or letting a terminal operation close it) also calls {@link #close()} on this iterator,
     * releasing the underlying {@code Reader} and any file handle behind it.
     * <p>
     * This overrides {@link ObjIterator#stream()}, whose stream would leave the reader open. Either of
     * the following therefore releases the file; nesting both is harmless because {@code close()} is
     * idempotent.
     *
     * <p><b>API Note:</b> a terminal stream operation closes the stream, and therefore closes this
     * iterator as well - even a short-circuiting one such as {@code stream().limit(5).count()}. Do not
     * keep reading from this iterator directly after handing it to {@code stream()}; if you need to
     * consume only part of the file and then continue with the iterator, iterate it directly instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // the stream owns the iterator
     * try (Stream<String> lines = LineIterator.of(file, StandardCharsets.UTF_8).stream()) {
     *     lines.filter(line -> line.contains("keyword")).forEach(System.out::println);
     * }
     *
     * // or keep owning the iterator yourself
     * try (LineIterator it = LineIterator.of(file, StandardCharsets.UTF_8)) {
     *     it.stream().forEach(System.out::println);
     * }
     * }</pre>
     *
     * @return a {@code Stream} of the remaining lines, whose {@code close()} closes this iterator
     * @see #close()
     */
    @Override
    public Stream<String> stream() {
        // ObjIterator.stream() returns a stream with no close handler, which would silently leak the
        // reader for the common `try (var s = LineIterator.of(f).stream())` idiom. This iterator owns a
        // Reader, so the stream built from it must own the iterator in turn.
        return super.stream().onClose(this::close);
    }

    /**
     * Closes the underlying {@code Reader} and releases all associated resources.
     * <p>
     * This method closes the {@link BufferedReader} that wraps the underlying reader,
     * which in turn closes the original reader and any associated input streams or files.
     * After calling this method, the iterator becomes unusable - {@link #hasNext()} will
     * return {@code false} and {@link #next()} will throw {@link NoSuchElementException}.
     * <p>
     * This method is useful if you only want to process the first few lines of a larger
     * file and want to release resources early. If you do not close the iterator explicitly,
     * the {@code Reader} remains open, potentially causing resource leaks.
     * <p>
     * This method can safely be called multiple times; subsequent calls have no effect.
     * <p>
     * Although {@code close()} is {@code synchronized}, {@link #hasNext()} and {@link #next()}
     * are not, and the internal state is not {@code volatile}. This class is therefore
     * <b>not</b> thread-safe: {@code close()} is intended to be called from the same thread
     * that performs the iteration (for example in a {@code finally} block or via
     * try-with-resources), not concurrently from another thread while iteration is in progress.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LineIterator it = LineIterator.of(largeFile);
     * try {
     *     // Process only first 100 lines
     *     for (int i = 0; i < 100 && it.hasNext(); i++) {
     *         processLine(it.next());
     *     }
     * } finally {
     *     it.close();   // Important to close to release resources
     * }
     * }</pre>
     *
     */
    @Override
    public synchronized void close() {
        if (isClosed) {
            return;
        }

        isClosed = true;
        finished = true;
        cachedLine = null;
        IOUtil.closeQuietly(br);
    }
}
