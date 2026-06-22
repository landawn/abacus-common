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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;

/**
 * Type handler for InputStream and its subclasses.
 * This class provides serialization, deserialization, and database access capabilities for InputStream instances.
 * InputStreams are serialized by reading all their bytes and decoding them as a string using the platform default charset.
 * Note that the InputStream content is consumed during serialization.
 */
@SuppressWarnings("java:S2160")
public class InputStreamType extends AbstractType<InputStream> {

    /** The type name constant for InputStream type identification. */
    public static final String INPUT_STREAM = InputStream.class.getSimpleName();

    private final Class<InputStream> typeClass;

    private final Constructor<?> bytesConstructor;

    private final Constructor<?> streamConstructor;

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
        super(typeName);

        typeClass = InputStream.class;

        bytesConstructor = null;
        streamConstructor = null;
    }

    /**
     * Package-private constructor for {@code InputStreamType} bound to a concrete {@link InputStream} subclass.
     * For non-abstract classes, the {@code byte[]} and {@link InputStream} constructors are looked up
     * for later use by {@link #valueOf(String)}.
     *
     * @param cls the {@link InputStream} class (or subclass) this type handler represents
     */
    InputStreamType(final Class<InputStream> cls) {
        super(ClassUtil.getSimpleClassName(cls));

        typeClass = cls;

        if (Modifier.isAbstract(cls.getModifiers())) {
            bytesConstructor = null;
            streamConstructor = null;
        } else {
            bytesConstructor = ClassUtil.getDeclaredConstructor(cls, byte[].class);
            streamConstructor = ClassUtil.getDeclaredConstructor(cls, InputStream.class);
        }
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
     * Reads the entire contents of an {@link InputStream} and returns them as a string.
     * Note that this operation consumes the stream.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@link InputStream} to read; may be {@code null}
     * @return the stream contents as a string, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final InputStream x) {
        // return x == null ? null : Strings.base64Encode(IOUtil.readAllBytes(x));

        return x == null ? null : IOUtil.readAllToString(x);
    }

    /**
     * Converts a string to an {@link InputStream} by encoding it with the default charset.
     * Creates the appropriate subclass based on the constructors discovered at construction time;
     * falls back to {@link ByteArrayInputStream} if no suitable constructor is available.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert; may be {@code null}
     * @return a new {@link InputStream} containing the encoded bytes, or {@code null} if {@code str} is {@code null}
     * @see #valueOf(Object)
     * @see #stringOf(InputStream)
     */
    @Override
    public InputStream valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        }

        final byte[] bytes = str.getBytes(Charsets.UTF_8);

        if (bytesConstructor != null) {
            //noinspection PrimitiveArrayArgumentToVarargsMethod
            return (InputStream) ClassUtil.invokeConstructor(bytesConstructor, bytes); // NOSONAR
        } else if (streamConstructor != null) {
            return (InputStream) ClassUtil.invokeConstructor(streamConstructor, new ByteArrayInputStream(bytes));
        } else {
            return new ByteArrayInputStream(bytes);
        }
    }

    /**
     * Converts an arbitrary object to an {@link InputStream}.
     * {@link Blob} instances are converted via {@link Blob#getBinaryStream()};
     * all other objects are first converted to a string and then to a stream via {@link #valueOf(String)}.
     *
     * @param obj the object to convert; may be {@code null}
     * @return an {@link InputStream} representation of the object, or {@code null} if {@code obj} is {@code null}
     * @throws com.landawn.abacus.exception.UncheckedSQLException if a {@link java.sql.SQLException} occurs while reading from a {@link Blob}
     */
    @SuppressFBWarnings
    @Override
    public InputStream valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Blob blob) {
            try {
                return blob.getBinaryStream();
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            }
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
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public InputStream get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getBinaryStream(columnIndex);
    }

    /**
     * Retrieves an {@link InputStream} from the specified column in a {@link ResultSet} using the column label,
     * via {@link ResultSet#getBinaryStream(String)}.
     *
     * @param rs the {@link ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return the binary stream from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public InputStream get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getBinaryStream(columnName);
    }

    /**
     * Sets an {@link InputStream} value as a binary-stream parameter in a {@link PreparedStatement}.
     *
     * @param stmt the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x the {@link InputStream} to set; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x) throws SQLException {
        stmt.setBinaryStream(columnIndex, x);
    }

    /**
     * Sets an {@link InputStream} value as a named binary-stream parameter in a {@link CallableStatement}.
     *
     * @param stmt the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@link InputStream} to set; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x) throws SQLException {
        stmt.setBinaryStream(parameterName, x);
    }

    /**
     * Sets an {@link InputStream} value as a binary-stream parameter in a {@link PreparedStatement},
     * reading at most {@code sqlTypeOrLength} bytes from the stream.
     *
     * @param stmt the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x the {@link InputStream} to set; may be {@code null}
     * @param sqlTypeOrLength the maximum number of bytes to read from the stream
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBinaryStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets an {@link InputStream} value as a named binary-stream parameter in a {@link CallableStatement},
     * reading at most {@code sqlTypeOrLength} bytes from the stream.
     *
     * @param stmt the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@link InputStream} to set; may be {@code null}
     * @param sqlTypeOrLength the maximum number of bytes to read from the stream
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x, final int sqlTypeOrLength) throws SQLException {
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
     * @throws IOException if an I/O error occurs during reading or writing
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
    public void appendTo(final Appendable appendable, final InputStream x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                IOUtil.write(IOUtil.newInputStreamReader(x), writer);
            } else {
                appendable.append(IOUtil.readAllToString(x));
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
     * @throws IOException if an I/O error occurs during reading or writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final InputStream x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if ((config == null) || (config.getStringQuotation() == 0)) {
                writer.writeCharacter(stringOf(x));
            } else {
                writer.write(config.getStringQuotation());
                writer.writeCharacter(stringOf(x));
                writer.write(config.getStringQuotation());
            }
        }
    }
}
