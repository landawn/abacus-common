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

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.util.BufferedJsonWriter;
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
     * Opens a binary stream and transfers ownership of the supplied {@link Blob} locator
     * to the returned stream. Closing the stream closes the delegate and releases the locator.
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

    private static void closeAndFree(final CloseAction closeAction, final FreeAction freeAction, final String lobType) throws IOException {
        Throwable failure = null;

        try {
            closeAction.close();
        } catch (final IOException | RuntimeException | Error e) {
            failure = e;
        }

        try {
            freeAction.free();
        } catch (final SQLException | RuntimeException | Error e) {
            if (failure == null) {
                failure = new IOException("Failed to release " + lobType + " resources", e);
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
        void close() throws IOException;
    }

    @FunctionalInterface
    private interface FreeAction {
        void free() throws SQLException;
    }

    private Utils() {
        // Utility class - prevent instantiation
    }
}
