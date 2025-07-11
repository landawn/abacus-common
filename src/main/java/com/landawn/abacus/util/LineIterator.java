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

/**
 * An Iterator over the lines in a {@code Reader}.
 * <p>
 * {@code LineIterator} holds a reference to an open {@code Reader}.
 * When you have finished with the iterator, you should close the reader
 * to free internal resources. This can be done by closing the reader directly,
 * or by calling the {@link #close()} or {@link IOUtil#closeQuietly(AutoCloseable)}
 * method on the iterator.
 * <p>
 * The recommended usage pattern is:
 * <pre>
 * try(LineIterator it = FileUtils.lineIterator(file, "UTF-8")) {
 *   while (it.hasNext()) {
 *     String line = it.nextLine();
 *     // do something with line
 *   }
 * }
 * </pre>
 * <p>
 * This class was copied from Apache Commons IO and may be modified.
 *
 * @see java.io.BufferedReader
 * @version $Id: LineIterator.java 1471767 2013-04-24 23:24:19Z sebb $
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
     * The reader will be wrapped in a BufferedReader if it's not already one.
     * 
     * @param reader the {@code Reader} to read from, not null
     * @throws IllegalArgumentException if the reader is null
     */
    public LineIterator(final Reader reader) throws IllegalArgumentException {
        if (reader == null) {
            throw new IllegalArgumentException("Reader must not be null");
        }

        if (reader instanceof BufferedReader) {
            br = (BufferedReader) reader;
        } else {
            br = new BufferedReader(reader);
        }
    }

    /**
     * Returns an Iterator for the lines in a {@code File} using the default encoding for the VM.
     * <p>
     * This method opens an {@code InputStream} for the file.
     * When you have finished with the iterator, you should close the stream
     * to free internal resources. This can be done by calling the
     * {@link LineIterator#close()} or
     * {@link IOUtil#closeQuietly(AutoCloseable)} method.
     * <p>
     * The recommended usage pattern is:
     * <pre>
     * LineIterator it = FileUtils.lineIterator(file, "UTF-8");
     * try {
     *   while (it.hasNext()) {
     *     String line = it.nextLine();
     *     /// do something with line
     *   }
     * } finally {
     *   closeQuietly(iterator);
     * }
     * </pre>
     * <p>
     * If an exception occurs during the creation of the iterator, the
     * underlying stream is closed.
     * <p>
     * <h3>Example:</h3>
     * <pre>
     * LineIterator lines = LineIterator.of(new File("data.txt"));
     * while (lines.hasNext()) {
     *     System.out.println(lines.next());
     * }
     * lines.close();
     * </pre>
     *
     * @param file the file to open for input, must not be {@code null}
     * @return an Iterator of the lines in the file, never {@code null}
     * @throws UncheckedIOException in case of an I/O error (file closed)
     * @see #of(File, Charset)
     */
    public static LineIterator of(final File file) {
        return of(file, Charsets.DEFAULT);
    }

    /**
     * Returns an Iterator for the lines in a {@code File}.
     * <p>
     * This method opens an {@code InputStream} for the file.
     * When you have finished with the iterator, you should close the stream
     * to free internal resources. This can be done by calling the
     * {@link LineIterator#close()} or
     * {@link IOUtil#closeQuietly(AutoCloseable)} method.
     * <p>
     * The recommended usage pattern is:
     * <pre>
     * LineIterator it = FileUtils.lineIterator(file, "UTF-8");
     * try {
     *   while (it.hasNext()) {
     *     String line = it.nextLine();
     *     /// do something with line
     *   }
     * } finally {
     *   closeQuietly(iterator);
     * }
     * </pre>
     * <p>
     * If an exception occurs during the creation of the iterator, the
     * underlying stream is closed.
     * <p>
     * <h3>Example:</h3>
     * <pre>
     * LineIterator lines = LineIterator.of(new File("data.txt"), StandardCharsets.UTF_8);
     * lines.forEach(line -> process(line));
     * lines.close();
     * </pre>
     *
     * @param file the file to open for input, must not be {@code null}
     * @param encoding the encoding to use, {@code null} means platform default
     * @return an Iterator of the lines in the file, never {@code null}
     * @throws UncheckedIOException in case of an I/O error (file closed)
     */
    public static LineIterator of(final File file, final Charset encoding) {
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
     * Returns an Iterator for the lines in an {@code InputStream}, using
     * the default character encoding.
     * <p>
     * {@code LineIterator} holds a reference to the open
     * {@code InputStream} specified here. When you have finished with
     * the iterator, you should close the stream to free internal resources.
     * This can be done by closing the stream directly, or by calling
     * {@link LineIterator#close()} or {@link IOUtil#closeQuietly(AutoCloseable)}.
     * <p>
     * <h3>Example:</h3>
     * <pre>
     * try (LineIterator lines = LineIterator.of(inputStream)) {
     *     while (lines.hasNext()) {
     *         processLine(lines.next());
     *     }
     * }
     * </pre>
     *
     * @param input the {@code InputStream} to read from, not null
     * @return an Iterator of the lines in the reader, never null
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static LineIterator of(final InputStream input) {
        return of(input, Charsets.DEFAULT);
    }

    /**
     * Returns an Iterator for the lines in an {@code InputStream}, using
     * the character encoding specified (or default encoding if null).
     * <p>
     * {@code LineIterator} holds a reference to the open
     * {@code InputStream} specified here. When you have finished with
     * the iterator, you should close the stream to free internal resources.
     * This can be done by closing the stream directly, or by calling
     * {@link LineIterator#close()} or {@link IOUtil#closeQuietly(AutoCloseable)}.
     * <p>
     * The recommended usage pattern is:
     * <pre>
     * try {
     *   LineIterator it = lineIterator(stream, charset);
     *   while (it.hasNext()) {
     *     String line = it.nextLine();
     *     /// do something with line
     *   }
     * } finally {
     *   closeQuietly(stream);
     * }
     * </pre>
     *
     * @param input the {@code InputStream} to read from, not null
     * @param encoding the encoding to use, {@code null} means platform default
     * @return an Iterator of the lines in the reader, never null
     * @throws UncheckedIOException if an I/O error occurs, such as if the encoding is invalid
     * @throws IllegalArgumentException if the input is null
     */
    public static LineIterator of(final InputStream input, final Charset encoding) throws UncheckedIOException {
        return new LineIterator(IOUtil.createReader(input, encoding));
    }

    /**
     * Returns an Iterator for the lines in a {@code Reader}.
     * <p>
     * {@code LineIterator} holds a reference to the open
     * {@code Reader} specified here. When you have finished with
     * the iterator, you should close the reader to free internal resources.
     * This can be done by closing the reader directly, or by calling
     * {@link LineIterator#close()} or {@link IOUtil#closeQuietly(AutoCloseable)}.
     * <p>
     * <h3>Example:</h3>
     * <pre>
     * try (LineIterator lines = LineIterator.of(reader)) {
     *     lines.stream().filter(line -> line.contains("keyword"))
     *                   .forEach(System.out::println);
     * }
     * </pre>
     *
     * @param reader the {@code Reader} to read from, not null
     * @return an Iterator of the lines in the reader, never null
     */
    public static LineIterator of(final Reader reader) {
        return new LineIterator(reader);
    }

    //-----------------------------------------------------------------------

    /**
     * Indicates whether the {@code Reader} has more lines.
     * If there is an {@code IOException} then {@link #close()} will
     * be called on this instance.
     * <p>
     * <h3>Example:</h3>
     * <pre>
     * LineIterator it = LineIterator.of(file);
     * while (it.hasNext()) {
     *     String line = it.next();
     *     // process line
     * }
     * </pre>
     *
     * @return {@code true} if the Reader has more lines
     * @throws IllegalStateException if an IO exception occurs
     */
    @Override
    public boolean hasNext() {
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
                close();
                throw new UncheckedIOException(ioe);
            }
        }
    }

    /**
     * Returns the next line in the wrapped {@code Reader}.
     * <p>
     * <h3>Example:</h3>
     * <pre>
     * LineIterator it = LineIterator.of(file);
     * while (it.hasNext()) {
     *     String line = it.next();
     *     System.out.println(line);
     * }
     * </pre>
     *
     * @return the next line from the input
     * @throws IllegalArgumentException if called when there are no more lines
     * @throws NoSuchElementException if there is no line to return
     */
    @Override
    public String next() throws IllegalArgumentException {
        if (!hasNext()) {
            throw new NoSuchElementException("No more lines");
        }
        final String res = cachedLine;
        cachedLine = null;
        return res;
    }

    /**
     * Closes the underlying {@code Reader} quietly.
     * This method is useful if you only want to process the first few
     * lines of a larger file. If you do not close the iterator
     * then the {@code Reader} remains open.
     * This method can safely be called multiple times.
     * <p>
     * <h3>Example:</h3>
     * <pre>
     * LineIterator it = LineIterator.of(largeFile);
     * try {
     *     // Process only first 100 lines
     *     for (int i = 0; i < 100 && it.hasNext(); i++) {
     *         processLine(it.next());
     *     }
     * } finally {
     *     it.close(); // Important to close for large files
     * }
     * </pre>
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