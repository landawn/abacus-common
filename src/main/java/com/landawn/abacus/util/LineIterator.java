/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * Note: it's copied from Apache Commons IO developed at The Apache Software Foundation (http://www.apache.org/), or under the Apache License 2.0.
 *
 * An Iterator over the lines in a <code>Reader</code>.
 * <p>
 * <code>LineIterator</code> holds a reference to an open <code>Reader</code>.
 * When you have finished with the iterator you should close the reader
 * to free internal resources. This can be done by closing the reader directly,
 * or by calling the {@link #close()} or {@link #closeAllQuietly(LineIterator)}
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
 *
 * @version $Id: LineIterator.java 1471767 2013-04-24 23:24:19Z sebb $
 * @since 1.2
 */
public final class LineIterator extends ObjIterator<String> implements AutoCloseable {
    // N.B. This class deliberately does not implement Iterable, see https://issues.apache.org/jira/browse/IO-181

    private final BufferedReader br;
    private String cachedLine;
    /** A flag indicating if the iterator has been fully read. */
    private boolean finished = false;

    private boolean isClosed = false;

    /**
     * Constructs an iterator of the lines for a <code>Reader</code>.
     *
     * @param reader the <code>Reader</code> to read from, not null
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
     * Returns an Iterator for the lines in a <code>File</code> using the default encoding for the VM.
     * <p>
     * This method opens an <code>InputStream</code> for the file.
     * When you have finished with the iterator you should close the stream
     * to free internal resources. This can be done by calling the
     * {@link LineIterator#close()} or
     * {@link IOUtil#closeQuietly(LineIterator)} method.
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
     *
     * @param file the file to open for input, must not be {@code null}
     * @return an Iterator of the lines in the file, never {@code null}
     * @throws UncheckedIOException in case of an I/O error (file closed)
     * @see #lineIterator(File, Charset)
     */
    public static LineIterator of(final File file) {
        return of(file, IOUtil.DEFAULT_CHARSET);
    }

    /**
     * Returns an Iterator for the lines in a <code>File</code>.
     * <p>
     * This method opens an <code>InputStream</code> for the file.
     * When you have finished with the iterator you should close the stream
     * to free internal resources. This can be done by calling the
     * {@link LineIterator#close()} or
     * {@link IOUtil#closeQuietly(LineIterator)} method.
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
     *
     * @param file the file to open for input, must not be {@code null}
     * @param encoding the encoding to use, {@code null} means platform default
     * @return an Iterator of the lines in the file, never {@code null}
     * @throws UncheckedIOException in case of an I/O error (file closed)
     */
    public static LineIterator of(final File file, final Charset encoding) {
        InputStream in = null;
        boolean noException = false;

        try {
            in = IOUtil.newFileInputStream(file);

            LineIterator iter = of(in, encoding);

            noException = true;

            return iter;
        } finally {
            if (noException == false && in != null) {
                IOUtil.closeQuietly(in);
            }
        }
    }

    /**
     *
     * @param input
     * @return
     */
    public static LineIterator of(final InputStream input) {
        return of(input, IOUtil.DEFAULT_CHARSET);
    }

    /**
     * Returns an Iterator for the lines in an <code>InputStream</code>, using
     * the character encoding specified (or default encoding if null).
     * <p>
     * <code>LineIterator</code> holds a reference to the open
     * <code>InputStream</code> specified here. When you have finished with
     * the iterator you should close the stream to free internal resources.
     * This can be done by closing the stream directly, or by calling
     * {@link LineIterator#close()} or {@link IOUtil#closeQuietly(LineIterator)}.
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
     * @param input the <code>InputStream</code> to read from, not null
     * @param encoding the encoding to use, null means platform default
     * @return an Iterator of the lines in the reader, never null
     * @throws UncheckedIOException if an I/O error occurs, such as if the encoding is invalid
     * @throws IllegalArgumentException if the input is null
     */
    public static LineIterator of(final InputStream input, final Charset encoding) throws UncheckedIOException {
        return new LineIterator(IOUtil.createReader(input, encoding));
    }

    /**
     *
     * @param reader
     * @return
     */
    public static LineIterator of(final Reader reader) {
        return new LineIterator(reader);
    }

    //    @SafeVarargs
    //    public static LineIterator[] of(final Reader... readers) {
    //        if (N.isEmpty(readers)) {
    //            return new LineIterator[0];
    //        }
    //
    //        final LineIterator[] iterators = new LineIterator[readers.length];
    //
    //        for (int i = 0, len = readers.length; i < len; i++) {
    //            iterators[i] = new LineIterator(readers[i]);
    //        }
    //
    //        return iterators;
    //    }

    //    public static List<LineIterator> of(final Collection<? extends Reader> readers) {
    //        if (N.isEmpty(readers)) {
    //            return new ArrayList<>();
    //        }
    //
    //        final List<LineIterator> iterators = new ArrayList<>(readers.size());
    //
    //        for (Reader reader : readers) {
    //            iterators.add(new LineIterator(reader));
    //        }
    //
    //        return iterators;
    //    }

    //-----------------------------------------------------------------------
    /**
     * Indicates whether the <code>Reader</code> has more lines.
     * If there is an <code>IOException</code> then {@link #close()} will
     * be called on this instance.
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
     * Returns the next line in the wrapped <code>Reader</code>.
     *
     * @return
     * @throws IllegalArgumentException
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
     * Closes the underlying <code>Reader</code> quietly.
     * This method is useful if you only want to process the first few
     * lines of a larger file. If you do not close the iterator
     * then the <code>Reader</code> remains open.
     * This method can safely be called multiple times.
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
