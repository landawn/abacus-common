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

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Objectory;

/**
 * Type handler for java.io.Reader and its subclasses.
 * Provides functionality for converting between Reader instances and their string representations,
 * as well as handling database operations with character streams (CLOB, LONGVARCHAR, etc.).
 *
 * <p>This type handler supports:
 * <ul>
 *   <li>Reading content from Reader instances and converting to strings</li>
 *   <li>Creating Reader instances from strings</li>
 *   <li>Database operations with character streams (getCharacterStream/setCharacterStream)</li>
 *   <li>Handling both abstract Reader class and concrete subclasses</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<Reader> type = TypeFactory.getType(Reader.class);
 *
 * // Convert Reader to string
 * Reader reader = new StringReader("Sample content");
 * String content = type.stringOf(reader);   // Returns "Sample content"
 *
 * // Convert string to Reader
 * Reader newReader = type.valueOf("New content");
 *
 * // Database operations
 * try (ResultSet rs = stmt.executeQuery("SELECT document FROM docs WHERE id = 1")) {
 *     if (rs.next()) {
 *         Reader docReader = type.get(rs, 1);
 *         String document = IOUtil.readAllToString(docReader);
 *     }
 * }
 *
 * // Storing large text in database
 * PreparedStatement stmt = conn.prepareStatement("INSERT INTO docs (content) VALUES (?)");
 * Reader largeText = new StringReader(veryLargeString);
 * type.set(stmt, 1, largeText);
 * stmt.executeUpdate();
 * }</pre>
 *
 */
@SuppressWarnings("java:S2160")
public class ReaderType extends AbstractType<Reader> {

    /**
     * The type name identifier for Reader type, equal to the simple class name {@code "Reader"}.
     */
    public static final String READER = Reader.class.getSimpleName();

    private final Class<Reader> typeClass;

    /**
     * Constructs a new ReaderType instance for the base Reader class.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    ReaderType() {
        this(READER);
    }

    /**
     * Constructs a new ReaderType with the specified type name.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     *
     * @param typeName the name of the Reader type
     */
    ReaderType(final String typeName) {
        super(typeName);

        typeClass = Reader.class;

    }

    /**
     * Constructs a new ReaderType for a specific Reader subclass.
     * Text construction is supported only for the content-preserving classes listed in {@link #valueOf(String)}.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     *
     * @param cls the specific Reader subclass to create a type handler for
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     */
    ReaderType(final Class<Reader> cls) throws IllegalArgumentException {
        super(ClassUtil.getSimpleClassName(cls));

        typeClass = cls;

    }

    /**
     * Returns the Class object representing the Reader type or its subclass.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * Class<Reader> clazz = type.javaType();
     * System.out.println(clazz.getName());   // Output: java.io.Reader
     * }</pre>
     *
     * @return the Class object for Reader.class or the specific Reader subclass
     */
    @Override
    public Class<Reader> javaType() {
        return typeClass;
    }

    /**
     * Indicates whether this type represents a Reader.
     * For ReaderType, this always returns {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * boolean isReader = type.isReader();
     * System.out.println(isReader);   // Output: true
     * }</pre>
     *
     * @return {@code true}, indicating this is a Reader type
     */
    @Override
    public boolean isReader() {
        return true;
    }

    /**
     * Converts a Reader to its string representation by reading all content from the Reader.
     * The Reader is fully consumed (but not closed) by this operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * Reader reader = new StringReader("Hello World");
     * String content = type.stringOf(reader);
     * System.out.println(content);   // Output: Hello World
     *
     * String nullStr = type.stringOf(null);
     * System.out.println(nullStr);   // Output: null
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)} for supported reader classes. Other subclasses can still be read, but require
     * an explicitly registered type handler to reconstruct them from text. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Reader to convert to string
     * @return the string containing all content read from the Reader, or {@code null} if the input is null
     * @throws UncheckedIOException if reading the remaining characters of non-null {@code x} fails
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Reader x) throws UncheckedIOException {
        return x == null ? null : IOUtil.readAllToString(x);
    }

    /**
     * Creates a Reader instance from a string value.
     * Supports {@link Reader}, {@link StringReader}, {@link CharArrayReader}, {@link BufferedReader},
     * {@link PushbackReader}, and {@link FilterReader} (using a pushback reader).
     * Other subclasses are rejected: a String constructor may interpret its argument as a pathname,
     * and a Reader constructor may transform rather than preserve content.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * Reader reader = type.valueOf("Sample text");
     * String content = IOUtil.readAllToString(reader);
     * System.out.println(content);   // Output: Sample text
     *
     * Reader nullReader = type.valueOf(null);
     * System.out.println(nullReader);   // Output: null
     * }</pre>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to create a Reader from
     * @return a Reader containing the string content, or {@code null} if the input string is null
     * @throws UnsupportedOperationException if non-null text is supplied for an unsupported reader class
     * @see #valueOf(Object)
     * @see #stringOf(Reader)
     */
    @MayReturnNull
    @Override
    public Reader valueOf(final String str) throws UnsupportedOperationException {
        if (str == null) {
            return null; // NOSONAR
        }

        // Constructor signatures do not establish content semantics (notably FileReader(String)).
        if (typeClass == (Class<?>) Reader.class || typeClass == (Class<?>) StringReader.class) {
            return new StringReader(str);
        } else if (typeClass == (Class<?>) CharArrayReader.class) {
            return new CharArrayReader(str.toCharArray());
        } else if (typeClass == (Class<?>) BufferedReader.class) {
            return new BufferedReader(new StringReader(str));
        } else if (typeClass == (Class<?>) PushbackReader.class || typeClass == (Class<?>) FilterReader.class) {
            return new PushbackReader(new StringReader(str));
        }

        throw new UnsupportedOperationException("Text construction is not supported for reader class: " + typeClass.getName());
    }

    /**
     * Creates a Reader from various object types.
     * If the object is a Clob, its character stream is returned in an owning wrapper.
     * Closing the returned reader closes the character stream and releases the Clob locator.
     * A {@code char[]} is read as its raw characters, in a reader of the handled class chosen by the
     * same rule as {@link #valueOf(String)}.
     * Otherwise, the object is converted to string and then to a Reader.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     *
     * // From an arbitrary object (converted to its string representation first)
     * Reader reader1 = type.valueOf((Object) "Hello");
     * System.out.println(IOUtil.readAllToString(reader1));   // Output: Hello
     *
     * // From a char[] (the characters themselves, not the array's list text)
     * Reader reader3 = type.valueOf((Object) new char[] { 'H', 'i' });
     * System.out.println(IOUtil.readAllToString(reader3));   // Output: Hi
     *
     * // From Clob (assuming clob is a valid SQL Clob object)
     * Reader reader2 = type.valueOf(clob);
     *
     * // From null
     * Reader nullReader = type.valueOf((Object) null);
     * System.out.println(nullReader);   // Output: null
     * }</pre>
     *
     * @param obj the object to convert to a Reader
     * @return a Reader representation of the object, or {@code null} if the input is null;
     *         for a Clob, closing the returned reader also releases the locator
     * @throws UncheckedSQLException if accessing the Clob's character stream fails
     * @throws UnsupportedOperationException if content is supplied for an unsupported reader class
     */
    @MayReturnNull
    @SuppressFBWarnings
    @Override
    public Reader valueOf(final Object obj) throws UncheckedSQLException, UnsupportedOperationException {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Clob clob) {
            try {
                return Utils.openCharacterStream(clob);
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            }
        } else if (obj instanceof char[] chars) {
            // The string route would render the array as its list text ("['a', 'b']").
            return valueOf(new String(chars));
        } else {
            return valueOf(Type.<Object> of(obj.getClass()).stringOf(obj));
        }
    }

    /**
     * Retrieves a character stream (Reader) from the specified column in the ResultSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * // Assuming rs is a ResultSet with a character stream in column 1
     * Reader reader = type.get(rs, 1);
     * String content = IOUtil.readAllToString(reader);
     * System.out.println(content);   // Output: content from the database
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return the Reader for the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getCharacterStream(columnIndex);
    }

    /**
     * Retrieves a character stream (Reader) from the specified column in the ResultSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * // Assuming rs is a ResultSet with a character stream in column "content"
     * Reader reader = type.get(rs, "content");
     * String text = IOUtil.readAllToString(reader);
     * System.out.println(text);   // Output: content from the database
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnName the label of the column to retrieve (column name or alias)
     * @return the Reader for the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Reader get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getCharacterStream(columnName);
    }

    /**
     * Sets a Reader parameter in a PreparedStatement.
     * The Reader will be used to provide character stream data to the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * PreparedStatement stmt = Mockito.mock(PreparedStatement.class);
     * Reader reader = new StringReader("Large text content");
     * type.set(stmt, 1, reader);
     * stmt.executeUpdate();
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the Reader to set as the parameter value
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x) throws NullPointerException, SQLException {
        stmt.setCharacterStream(columnIndex, x);
    }

    /**
     * Sets a Reader parameter in a CallableStatement.
     * The Reader will be used to provide character stream data to the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * CallableStatement stmt = Mockito.mock(CallableStatement.class);
     * Reader reader = new StringReader("Updated content");
     * type.set(stmt, "content", reader);
     * stmt.execute();
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Reader to set as the parameter value
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x) throws NullPointerException, SQLException {
        stmt.setCharacterStream(parameterName, x);
    }

    /**
     * Sets a Reader parameter in a PreparedStatement with a specified length.
     * The Reader will be used to provide character stream data to the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * PreparedStatement stmt = Mockito.mock(PreparedStatement.class);
     * String text = "Fixed length content";
     * Reader reader = new StringReader(text);
     * type.set(stmt, 1, reader, text.length());
     * stmt.executeUpdate();
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the Reader to set as the parameter value
     * @param sqlTypeOrLength the length of the stream in characters
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x, final int sqlTypeOrLength) throws NullPointerException, SQLException {
        stmt.setCharacterStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a Reader parameter in a CallableStatement with a specified length.
     * The Reader will be used to provide character stream data to the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * CallableStatement stmt = Mockito.mock(CallableStatement.class);
     * String text = "Content with known length";
     * Reader reader = new StringReader(text);
     * type.set(stmt, "content", reader, text.length());
     * stmt.execute();
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Reader to set as the parameter value
     * @param sqlTypeOrLength the length of the stream in characters
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x, final int sqlTypeOrLength)
            throws NullPointerException, SQLException {
        stmt.setCharacterStream(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Appends the content of a Reader to the given Appendable.
     * If the Appendable is a Writer, the content is copied directly.
     * Otherwise, the Reader content is read as a string and appended.
     * The Reader is fully consumed after this operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * Reader reader = new StringReader("Hello World");
     * StringBuilder sb = new StringBuilder("Message: ");
     * type.appendTo(sb, reader);
     * System.out.println(sb.toString());   // Output: Message: Hello World
     *
     * // With Writer
     * StringWriter writer = new StringWriter();
     * Reader reader2 = new StringReader("Content");
     * type.appendTo(writer, reader2);
     * System.out.println(writer.toString());   // Output: Content
     * }</pre>
     *
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the Reader whose content should be appended; may be {@code null}, in which case
     *          the literal {@code "null"} is appended
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if appending to the destination fails, or reading {@code x} fails while copying directly to a {@code Writer}
     * @throws UncheckedIOException if reading non-null {@code x} fails when {@code appendable} is not a {@code Writer}
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
    public void appendTo(final Appendable appendable, final Reader x) throws NullPointerException, IOException, UncheckedIOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                IOUtil.write(x, writer);
            } else {
                appendable.append(IOUtil.readAllToString(x));
            }
        }
    }

    /**
     * Writes the content of a Reader to the given CharacterWriter.
     * The content is optionally quoted based on the serialization configuration.
     * The Reader is fully consumed after this operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * Reader reader = new StringReader("Sample text");
     * BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
     * BufferedJsonWriter writer2 = Objectory.createBufferedJsonWriter();
     * JsonSerConfig config = JsonSerConfig.create();
     * config.setStringQuotation('"');
     * try {
     *     type.serializeTo(writer, reader, config);
     *     System.out.println(writer.toString());   // Output: "Sample text"
     *
     *     // Without quotation
     *     Reader reader2 = new StringReader("No quotes");
     *     type.serializeTo(writer2, reader2, null);
     *     System.out.println(writer2.toString());   // Output: No quotes
     * } finally {
     *     Objectory.recycle(writer);
     *     Objectory.recycle(writer2);
     * }
     * }</pre>
     *
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
     * @param writer the CharacterWriter to write to
     * @param x the Reader whose content should be written; may be {@code null}, in which case
     *          the literal {@code "null"} is written
     * @param config the serialization configuration that determines string quotation; may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if reading characters from {@code x}, writing their escaped representation, or writing quotation marks or the
     *         null literal to {@code writer} fails
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Reader x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }

            final char[] buf = Objectory.createCharArrayBuffer();

            try {
                int count = 0;

                while (IOUtil.EOF != (count = IOUtil.read(x, buf, 0, buf.length))) {
                    Utils.writeStringContent(writer, buf, 0, count, config == null ? 0 : config.getStringQuotation());
                }
            } finally {
                Objectory.recycle(buf);
            }

            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }
        }
    }
}
