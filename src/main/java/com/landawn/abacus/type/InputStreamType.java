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
     */
    InputStreamType(final String typeName) {
        this(typeName, Charsets.UTF_8);
    }

    // Keep inherited text conversion consistent with each JDBC stream specialization's writers.
    InputStreamType(final String typeName, final Charset charset) {
        super(typeName);
        typeClass = InputStream.class;
        this.charset = charset;
    }

    /**
     * Package-private constructor for {@code InputStreamType} bound to a concrete {@link InputStream} subclass.
     * Text construction is supported only for the content-preserving classes listed in {@link #valueOf(String)}.
     *
     * @param cls the {@link InputStream} class (or subclass) this type handler represents
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     */
    InputStreamType(final Class<InputStream> cls) throws IllegalArgumentException {
        super(ClassUtil.getSimpleClassName(cls));

        typeClass = cls;
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
     * Wraps raw bytes in a stream of the handled class.
     *
     * @param bytes the content to wrap; must not be {@code null}
     * @return a new {@link InputStream} over {@code bytes}
     * @throws UnsupportedOperationException if the handled stream class cannot be constructed from content
     */
    private InputStream wrap(final byte[] bytes) throws UnsupportedOperationException {
        // In particular, decoder constructors cannot reconstruct already decoded stringOf output.
        if (typeClass == (Class<?>) InputStream.class || typeClass == (Class<?>) ByteArrayInputStream.class) {
            return new ByteArrayInputStream(bytes);
        } else if (typeClass == (Class<?>) BufferedInputStream.class || typeClass == (Class<?>) FilterInputStream.class) {
            return new BufferedInputStream(new ByteArrayInputStream(bytes));
        } else if (typeClass == (Class<?>) DataInputStream.class) {
            return new DataInputStream(new ByteArrayInputStream(bytes));
        } else if (typeClass == (Class<?>) PushbackInputStream.class) {
            return new PushbackInputStream(new ByteArrayInputStream(bytes));
        }

        throw new UnsupportedOperationException("Text construction is not supported for stream class: " + typeClass.getName());
    }

    /**
     * Converts an arbitrary object to an {@link InputStream}.
     * {@link Blob} instances are converted via {@link Blob#getBinaryStream()}; ownership of a supplied
     * locator is transferred to the returned stream, whose {@link InputStream#close()} method also calls
     * {@link Blob#free()}.
     * A {@code byte[]} is wrapped as-is (the raw bytes, no charset involved) in a stream of the handled class,
     * subject to the same supported-class rule as {@link #valueOf(String)}.
     * All other objects are first converted to a string and then to a stream via {@link #valueOf(String)}.
     *
     * @param obj the object to convert; may be {@code null}
     * @return an {@link InputStream} representation of the object, or {@code null} if {@code obj} is {@code null};
     *         when {@code obj} is a {@code Blob}, closing the returned stream also releases the locator
     * @throws UncheckedSQLException if a {@link java.sql.SQLException} occurs while reading from a {@link Blob}
     * @throws UnsupportedOperationException if content is supplied for an unsupported stream class
     */
    @MayReturnNull
    @SuppressFBWarnings
    @Override
    public InputStream valueOf(final Object obj) throws UncheckedSQLException, UnsupportedOperationException {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Blob blob) {
            try {
                return Utils.openBinaryStream(blob);
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            }
        } else if (obj instanceof byte[] bytes) {
            // The string route would render the array as its list text ("[1, 2, 3]").
            return wrap(bytes);
        } else {
            return valueOf(Type.<Object> of(obj.getClass()).stringOf(obj));
        }
    }

    /**
     * Retrieves an {@link InputStream} from the specified column in a {@link ResultSet}
     * via {@link ResultSet#getBinaryStream(int)}.
     *
     * @param rs the {@link ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return the binary stream from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public InputStream get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getBinaryStream(columnIndex);
    }

    /**
     * Retrieves an {@link InputStream} from the specified column in a {@link ResultSet} using the column label,
     * via {@link ResultSet#getBinaryStream(String)}.
     *
     * @param rs the {@link ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return the binary stream from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public InputStream get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getBinaryStream(columnName);
    }

    /**
     * Sets an {@link InputStream} value as a binary-stream parameter in a {@link PreparedStatement}.
     *
     * @param stmt the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x the {@link InputStream} to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x) throws NullPointerException, SQLException {
        stmt.setBinaryStream(columnIndex, x);
    }

    /**
     * Sets an {@link InputStream} value as a named binary-stream parameter in a {@link CallableStatement}.
     *
     * @param stmt the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@link InputStream} to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x) throws NullPointerException, SQLException {
        stmt.setBinaryStream(parameterName, x);
    }

    /**
     * Sets an {@link InputStream} value as a binary-stream parameter in a {@link PreparedStatement},
     * declaring that the stream contains exactly {@code sqlTypeOrLength} bytes.
     *
     * @param stmt the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x the {@link InputStream} to set; may be {@code null}
     * @param sqlTypeOrLength the declared number of bytes in the stream
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x, final int sqlTypeOrLength)
            throws NullPointerException, SQLException {
        stmt.setBinaryStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets an {@link InputStream} value as a named binary-stream parameter in a {@link CallableStatement},
     * declaring that the stream contains exactly {@code sqlTypeOrLength} bytes.
     *
     * @param stmt the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@link InputStream} to set; may be {@code null}
     * @param sqlTypeOrLength the declared number of bytes in the stream
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x, final int sqlTypeOrLength)
            throws NullPointerException, SQLException {
        stmt.setBinaryStream(parameterName, x, sqlTypeOrLength);
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
     * The stream is fully consumed and converted to a string.
     * If {@code config} specifies a string quotation character, the output is quoted.
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
            if ((config == null) || (config.getStringQuotation() == 0)) {
                writer.writeCharacter(stringOf(x));
            } else {
                writer.write(config.getStringQuotation());
                Utils.writeStringContent(writer, stringOf(x), config.getStringQuotation());
                writer.write(config.getStringQuotation());
            }
        }
    }
}
