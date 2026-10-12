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

package com.landawn.abacus.type;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Objectory;

/**
 * Type handler for {@link java.io.InputStream} and its subclasses.
 * This class provides serialization, deserialization, and database access for {@code InputStream} instances.
 * Streams are serialized by reading all of their bytes and decoding them as a UTF-8 string;
 * {@link AsciiStreamType} and {@link ClobAsciiStreamType} specialize text conversion to US-ASCII.
 * The stream content is consumed during serialization, but the stream is not closed.
 * Arbitrary binary input is not guaranteed to survive UTF-8 decoding and re-encoding; use a
 * {@code byte[]} type when a lossless binary string conversion is required.
 */
@SuppressWarnings("java:S2160")
public class InputStreamType extends AbstractType<InputStream> {

    /** The type name constant for InputStream type identification, equal to {@code "InputStream"}. */
    public static final String INPUT_STREAM = InputStream.class.getSimpleName();

    private final Class<InputStream> typeClass;
    private final Charset charset;

    /**
     * Package-private constructor for {@code InputStreamType}.
     * Instances are created by the {@code TypeFactory}.
     */
    InputStreamType() {
        this(INPUT_STREAM);
    }

    /**
     * Package-private constructor for {@code InputStreamType} with a custom type name.
     * Used by subclasses that extend this type with a specialized name.
     *
     * @param typeName the custom type name to register
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    InputStreamType(final String typeName) throws IllegalArgumentException {
        this(typeName, Charsets.UTF_8);
    }

    // Keep inherited text conversion consistent with each JDBC stream specialization's writers.
    /**
     * Package-private constructor for {@code InputStreamType} with a custom type name and text charset.
     *
     * @param typeName the custom type name to register
     * @param charset the charset used to convert the stream content to and from text
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    InputStreamType(final String typeName, final Charset charset) throws IllegalArgumentException {
        super(typeName);
        typeClass = InputStream.class;
        this.charset = charset;
    }

    /**
     * Package-private constructor for {@code InputStreamType} bound to a concrete {@link InputStream} subclass.
     * Text construction is supported only for the content-preserving classes listed in {@link #valueOf(String)}.
     *
     * @param targetClass the {@link InputStream} class (or subclass) this type handler represents
     * @throws IllegalArgumentException if {@code targetClass} is {@code null}.
     */
    InputStreamType(final Class<InputStream> targetClass) throws IllegalArgumentException {
        super(ClassUtil.getSimpleClassName(targetClass));

        typeClass = targetClass;
        charset = Charsets.UTF_8;
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code InputStream.class} or a concrete subclass thereof
     */
    @Override
    public Class<InputStream> javaType() {
        return typeClass;
    }

    /**
     * Indicates whether this type represents an InputStream.
     * For InputStreamType, this always returns {@code true}.
     *
     * @return {@code true}, indicating this is an InputStream type
     */
    @Override
    public boolean isInputStream() {
        return true;
    }

    /**
     * Reads the entire contents of an {@link InputStream} using this handler's charset and returns the result.
     * The charset is UTF-8, or US-ASCII for the ASCII stream handlers.
     * This operation consumes the stream but does not close it.
     *
     * <p>For supported stream classes and well-formed content in the handler's charset, {@link #valueOf(String)} re-encodes the same bytes. Malformed or
     * non-text binary input may be replaced during decoding and therefore does not round-trip losslessly.</p>
     *
     * @param x the {@link InputStream} to read; may be {@code null}
     * @return the stream contents as a string, or {@code null} if {@code x} is {@code null}
     * @throws UncheckedIOException if reading and decoding the remaining bytes of non-null {@code x} using this type's charset fails
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final InputStream x) throws UncheckedIOException {
        return x == null ? null : IOUtil.readAllToString(x, charset);
    }

    /**
     * Converts a string to an {@link InputStream} using UTF-8, or US-ASCII for the ASCII stream handlers.
     * Unmappable characters and malformed surrogate sequences use the charset encoder's replacement bytes.
     * Supports {@link InputStream}, {@link ByteArrayInputStream}, {@link BufferedInputStream},
     * {@link DataInputStream}, {@link PushbackInputStream}, and {@link FilterInputStream} (using a buffered stream).
     * Other subclasses require an explicitly registered handler; an InputStream or byte-array constructor
     * does not establish that the class preserves the supplied content.
     *
     * <p>This method round-trips the output of {@link #stringOf(InputStream)} only when the original stream
     * contained well-formed bytes in this handler's charset.</p>
     *
     * @param str the string to convert; may be {@code null}
     * @return a new {@link InputStream} containing the encoded bytes, or {@code null} if {@code str} is {@code null}
     * @throws UnsupportedOperationException if non-null text is supplied for an unsupported stream class
     * @see #valueOf(Object)
     * @see #stringOf(InputStream)
     */
    @MayReturnNull
    @Override
    public InputStream valueOf(final String str) throws UnsupportedOperationException {
        return str == null ? null : wrap(str.getBytes(charset)); // NOSONAR
    }

    /**
     * Whether {@link #wrap(byte[])} can create the handled class: the single list of supported classes.
     */
    private boolean isWrappable() {
        return typeClass == (Class<?>) InputStream.class || typeClass == (Class<?>) ByteArrayInputStream.class
                || typeClass == (Class<?>) BufferedInputStream.class || typeClass == (Class<?>) FilterInputStream.class
                || typeClass == (Class<?>) DataInputStream.class || typeClass == (Class<?>) PushbackInputStream.class;
    }

    /**
     * Returns {@code opened} - a JDBC stream or a stream owning a Blob locator - as the handled
     * class: as is when it already is one, wrapped when the handled class wraps a stream (closing the result closes
     * {@code opened}), or as a {@link ByteArrayInputStream} holding a copy of its content, after which {@code opened} is closed.
     * A null stream is returned unchanged. An unsupported target closes the acquired stream before failing.
     *
     * @param opened the acquired stream whose ownership is transferred to this method; may be null
     * @return the stream in the handled class, or null
     * @throws UncheckedIOException if copying or closing an acquired stream fails
     * @throws UnsupportedOperationException if the stream does not already match an unsupported handled class
     */
    private InputStream asHandledClass(final InputStream opened) throws UncheckedIOException {
        if (opened == null || typeClass.isInstance(opened)) {
            return opened;
        } else if (typeClass == (Class<?>) BufferedInputStream.class || typeClass == (Class<?>) FilterInputStream.class) {
            return new BufferedInputStream(opened);
        } else if (typeClass == (Class<?>) DataInputStream.class) {
            return new DataInputStream(opened);
        } else if (typeClass == (Class<?>) PushbackInputStream.class) {
            return new PushbackInputStream(opened);
        }

        try (InputStream source = opened) {
            // A JDBC driver can supply an arbitrary stream class. Do not copy content into an unrelated target;
            // reject it while still closing the stream acquired for this failed conversion.
            if (!isWrappable()) {
                throw unsupportedStreamClass();
            }

            return new ByteArrayInputStream(IOUtil.readAllBytes(source));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private UnsupportedOperationException unsupportedStreamClass() {
        return new UnsupportedOperationException("Construction from content is not supported for stream class: " + typeClass.getName());
    }

    /**
     * Wraps raw bytes in a stream of the handled class.
     *
     * @param bytes the content to wrap; must not be {@code null}
     * @return a new {@link InputStream} over {@code bytes}
     * @throws UnsupportedOperationException if the handled stream class cannot be constructed from content
     */
    private InputStream wrap(final byte[] bytes) throws UnsupportedOperationException {
        // In particular, decoder constructors cannot reconstruct already decoded stringOf output.
        if (!isWrappable()) {
            throw unsupportedStreamClass();
        } else if (typeClass == (Class<?>) InputStream.class || typeClass == (Class<?>) ByteArrayInputStream.class) {
            return new ByteArrayInputStream(bytes);
        } else if (typeClass == (Class<?>) BufferedInputStream.class || typeClass == (Class<?>) FilterInputStream.class) {
            return new BufferedInputStream(new ByteArrayInputStream(bytes));
        } else if (typeClass == (Class<?>) DataInputStream.class) {
            return new DataInputStream(new ByteArrayInputStream(bytes));
        } else {
            return new PushbackInputStream(new ByteArrayInputStream(bytes));
        }
    }

    /**
     * Converts an arbitrary object to an {@link InputStream}.
     * An object that already is an instance of the handled stream class is returned as is (not read, copied or closed).
     * {@link Blob} instances are converted via {@link Blob#getBinaryStream()}; ownership of a supplied
     * locator is transferred to the returned stream, whose {@link InputStream#close()} method also calls
     * {@link Blob#free()} - a {@code BufferedInputStream}, {@code DataInputStream} or {@code PushbackInputStream} wraps the
     * Blob's stream, while a {@code ByteArrayInputStream} gets a copy of the content and the locator is released at once.
     * An unsupported handled class is rejected before the Blob is opened.
     * A {@code byte[]} is wrapped as-is (the raw bytes, no charset involved) in a stream of the handled class,
     * subject to the same supported-class rule as {@link #valueOf(String)}; any other {@link InputStream} has its
     * remaining bytes read (it is consumed but not closed - the caller still owns it) and wrapped the same way. When the
     * handled class cannot be created from bytes, {@link UnsupportedOperationException} is thrown before the supplied
     * stream is read.
     * All other objects are first converted to a string and then to a stream via {@link #valueOf(String)}; a
     * {@link java.io.Reader} (consumed but not closed) or a {@link java.sql.Clob} (read and then released) is read to its end
     * that way, after the handled class has been checked.
     *
     * @param obj the object to convert; may be {@code null}
     * @return an {@link InputStream} representation of the object, or {@code null} if {@code obj} is {@code null};
     *         when {@code obj} is a {@code Blob} returned as (or wrapped around) its owning stream, closing the returned stream
     *         also releases the locator; a {@code ByteArrayInputStream} copy is returned with the locator already released
     * @throws UncheckedSQLException if a {@link java.sql.SQLException} occurs while reading from a {@link Blob}, or while reading
     *         or releasing a supplied {@link java.sql.Clob}
     * @throws UncheckedIOException if reading a supplied stream of another class, or a supplied reader, fails, or reading or
     *         releasing a {@code Blob} fails while it is copied into a {@code ByteArrayInputStream}
     * @throws UnsupportedOperationException if content is supplied for an unsupported stream class
     */
    @MayReturnNull
    @SuppressFBWarnings
    @Override
    public InputStream valueOf(final Object obj) throws UncheckedSQLException, UncheckedIOException, UnsupportedOperationException {
        if (obj == null) {
            return null; // NOSONAR
        } else if (typeClass.isInstance(obj)) {
            // Returned as is: converting would consume the caller's stream (and the string route would decode its bytes as
            // text, replacing those that are not valid in the charset: 0x80 -> EF BF BD).
            return (InputStream) obj;
        } else if (obj instanceof Blob blob) {
            // Checked before the Blob is opened: the opened stream is the class Utils makes, not the handled class.
            if (!isWrappable()) {
                throw unsupportedStreamClass();
            }

            final InputStream opened;

            try {
                opened = Utils.openBinaryStream(blob);
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            }

            return opened == null ? null : asHandledClass(opened);
        } else if (obj instanceof byte[] bytes) {
            // The string route would render the array as its list text ("[1, 2, 3]").
            return wrap(bytes);
        } else if (obj instanceof InputStream inputStream) {
            // A stream of another class: copy its remaining raw bytes. The string route would decode them as text (lossy).
            // Check the target first, so that an unsupported one does not consume the caller's stream.
            if (!isWrappable()) {
                throw unsupportedStreamClass();
            }

            return wrap(IOUtil.readAllBytes(inputStream));
        } else {
            // A Reader or a Clob is read to its end by stringOf (a Clob is also released): reject an unsupported target first,
            // as for a stream above.
            if ((obj instanceof java.io.Reader || obj instanceof java.sql.Clob) && !isWrappable()) {
                throw unsupportedStreamClass();
            }

            // A Clob is read by the Clob type handler, whatever its class looks like to the type lookup.
            return valueOf(Type.<Object> of(obj instanceof java.sql.Clob ? java.sql.Clob.class : obj.getClass()).stringOf(obj));
        }
    }

    /**
     * Retrieves an {@link InputStream} from the specified column in a {@link ResultSet}
     * via {@link ResultSet#getBinaryStream(int)}.
     * A stream already of the handled class is returned unchanged. Supported wrapper classes wrap it lazily;
     * closing the returned stream closes the JDBC stream. A {@link ByteArrayInputStream} target copies its bytes
     * and closes the JDBC stream before returning. Other mismatched targets are rejected and the acquired stream is closed.
     *
     * @param rs the {@link ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return the binary stream from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column index is invalid
     * @throws UncheckedIOException if copying or closing the acquired stream fails
     * @throws UnsupportedOperationException if the acquired stream does not match an unsupported handled class
     */
    @Override
    public InputStream get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return asHandledClass(rs.getBinaryStream(columnIndex));
    }

    /**
     * Retrieves an {@link InputStream} from the specified column in a {@link ResultSet} using the column label,
     * via {@link ResultSet#getBinaryStream(String)}.
     * A stream already of the handled class is returned unchanged. Supported wrapper classes wrap it lazily;
     * closing the returned stream closes the JDBC stream. A {@link ByteArrayInputStream} target copies its bytes
     * and closes the JDBC stream before returning. Other mismatched targets are rejected and the acquired stream is closed.
     *
     * @param rs the {@link ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return the binary stream from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column label is not found
     * @throws UncheckedIOException if copying or closing the acquired stream fails
     * @throws UnsupportedOperationException if the acquired stream does not match an unsupported handled class
     */
    @Override
    public InputStream get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return asHandledClass(rs.getBinaryStream(columnName));
    }

    /**
     * Sets an {@link InputStream} value as a binary-stream parameter in a {@link PreparedStatement}.
     *
     * @param statement the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x the {@link InputStream} to set; may be {@code null}
     * @throws NullPointerException if {@code statement} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final InputStream x) throws NullPointerException, SQLException {
        statement.setBinaryStream(columnIndex, x);
    }

    /**
     * Sets an {@link InputStream} value as a named binary-stream parameter in a {@link CallableStatement}.
     *
     * @param statement the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@link InputStream} to set; may be {@code null}
     * @throws NullPointerException if {@code statement} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final InputStream x) throws NullPointerException, SQLException {
        statement.setBinaryStream(parameterName, x);
    }

    /**
     * Sets an {@link InputStream} value as a binary-stream parameter in a {@link PreparedStatement},
     * declaring that the stream contains exactly {@code sqlTypeOrLength} bytes.
     *
     * @param statement the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x the {@link InputStream} to set; may be {@code null}
     * @param sqlTypeOrLength the declared number of bytes in the stream
     * @throws NullPointerException if {@code statement} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final InputStream x, final int sqlTypeOrLength)
            throws NullPointerException, SQLException {
        statement.setBinaryStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets an {@link InputStream} value as a named binary-stream parameter in a {@link CallableStatement},
     * declaring that the stream contains exactly {@code sqlTypeOrLength} bytes.
     *
     * @param statement the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@link InputStream} to set; may be {@code null}
     * @param sqlTypeOrLength the declared number of bytes in the stream
     * @throws NullPointerException if {@code statement} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final InputStream x, final int sqlTypeOrLength)
            throws NullPointerException, SQLException {
        statement.setBinaryStream(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Appends the content of an {@link InputStream} to an {@link Appendable}.
     * If the target is a {@link java.io.Writer}, the stream is copied directly using character encoding;
     * otherwise the stream is read to a string first.
     * Note that this operation consumes the stream.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x the {@link InputStream} to read from; may be {@code null}
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if appending to the destination fails, or reading and decoding {@code x} fails while copying directly to a
     *         {@code Writer}
     * @throws UncheckedIOException if reading and decoding non-null {@code x} fails when {@code appendable} is not a {@code Writer}
     * @implNote
     * This method appends a string representation of {@code x} to {@code appendable} (the literal {@code "null"} for a
     * {@code null} value). Conceptually this is the human-readable form produced by {@code toString()}, <i>not</i> the
     * value returned by {@code stringOf}, which is a formatted, serializable representation (typically a JSON string)
     * that {@link #valueOf(String)} can convert back into an equivalent value. For values whose nested structure makes
     * the two forms differ (collections, maps, arrays), {@code appendTo} emits the unquoted, {@code toString()}-style
     * form; it is therefore not, in the general contract, a plain
     * {@code appendable.append(x == null ? NULL_STRING : stringOf(x))}. (For value types whose human-readable and
     * serialized forms coincide, the appended text is naturally identical to {@code stringOf(x)}.)
     */
    @Override
    public void appendTo(final Appendable appendable, final InputStream x) throws NullPointerException, IOException, UncheckedIOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                IOUtil.write(IOUtil.newInputStreamReader(x, charset), writer);
            } else {
                appendable.append(IOUtil.readAllToString(x, charset));
            }
        }
    }

    /**
     * Writes the string content of an {@link InputStream} to a {@link CharacterWriter}.
     * The built-in handler fully consumes the stream using a bounded character buffer.
     * Subclasses inheriting this method retain their {@link #stringOf(InputStream)} conversion,
     * including any custom consumption behavior, before its returned text is escaped.
     * If {@code config} specifies a string quotation character, the output is quoted.
     * The built-in path writes content as it is read and does not close the stream. If reading fails part way,
     * the opening quotation and the content read before the failure have already been written, so the output is incomplete.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x the {@link InputStream} to write; may be {@code null}
     * @param config the serialization configuration to use; may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing escaped stream content, quotation marks or the null literal to {@code writer} fails
     * @throws UncheckedIOException if reading and decoding the remaining bytes of non-null {@code x} fails
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final InputStream x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, UncheckedIOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if (getClass() != InputStreamType.class) {
                // Streaming the raw bytes would bypass a subclass's stringOf conversion and its
                // consumption/failure contract. Retain config reads around that callback as well:
                // a custom conversion may change the quotation used for its returned content.
                if (config == null || config.getStringQuotation() == 0) {
                    writer.writeCharacter(stringOf(x));
                } else {
                    writer.write(config.getStringQuotation());
                    Utils.writeStringContent(writer, stringOf(x), config.getStringQuotation());
                    writer.write(config.getStringQuotation());
                }
                return;
            }

            final char quotation = config == null ? 0 : config.getStringQuotation();
            final Reader reader = IOUtil.newInputStreamReader(x, charset);
            final char[] buf = Objectory.createCharArrayBuffer();

            try {
                if (quotation != 0) {
                    writer.write(quotation);
                }

                // A high surrogate ending one chunk is held back and written with the next chunk. Writers escape a
                // surrogate they see unpaired, so splitting a pair across two writes would change its spelling
                // (two escaped halves instead of the character) depending on where the chunk boundary falls.
                int carried = 0;

                while (true) {
                    final int count;
                    try {
                        count = IOUtil.read(reader, buf, carried, buf.length - carried);
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }

                    if (count == IOUtil.EOF) {
                        if (carried > 0) {
                            // A lone trailing high surrogate is escaped exactly as it was when the whole value was written at once.
                            Utils.writeStringContent(writer, buf, 0, carried, quotation);
                        }

                        break;
                    }

                    final int available = carried + count;
                    carried = available > 0 && Character.isHighSurrogate(buf[available - 1]) ? 1 : 0;

                    Utils.writeStringContent(writer, buf, 0, available - carried, quotation);

                    if (carried > 0) {
                        buf[0] = buf[available - 1];
                    }
                }

                if (quotation != 0) {
                    writer.write(quotation);
                }
            } finally {
                Objectory.recycle(buf);
            }
        }
    }
}
