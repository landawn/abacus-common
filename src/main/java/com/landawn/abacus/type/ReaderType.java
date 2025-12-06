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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
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
 */
@SuppressWarnings("java:S2160")
public class ReaderType extends AbstractType<Reader> {

    public static final String READER = Reader.class.getSimpleName();

    private final Class<Reader> typeClass;

    private final Constructor<?> stringConstructor;

    private final Constructor<?> readerConstructor;

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

        stringConstructor = null;
        readerConstructor = null;
    }

    /**
     * Constructs a new ReaderType for a specific Reader subclass.
     * Attempts to locate constructors that accept String or Reader parameters for instantiation.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     *
     * @param cls the specific Reader subclass to create a type handler for
     */
    ReaderType(final Class<Reader> cls) {
        super(ClassUtil.getSimpleClassName(cls));

        typeClass = cls;

        if (Modifier.isAbstract(cls.getModifiers())) {
            stringConstructor = null;
            readerConstructor = null;
        } else {
            stringConstructor = ClassUtil.getDeclaredConstructor(cls, String.class);
            readerConstructor = ClassUtil.getDeclaredConstructor(cls, Reader.class);
        }
    }

    /**
     * Returns the Class object representing the Reader type or its subclass.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * Class<Reader> clazz = type.clazz();
     * System.out.println(clazz.getName());   // Output: java.io.Reader
     * }</pre>
     *
     * @return the Class object for Reader.class or the specific Reader subclass
     */
    @Override
    public Class<Reader> clazz() {
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
     * The Reader is fully consumed and closed after this operation.
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
     * @param x the Reader to convert to string
     * @return the string containing all content read from the Reader, or {@code null} if the input is null
     * @throws UncheckedIOException if an I/O error occurs while reading from the Reader
     */
    @Override
    public String stringOf(final Reader x) {
        return x == null ? null : IOUtil.readAllToString(x);
    }

    /**
     * Creates a Reader instance from a string value.
     * If the concrete Reader class has a constructor accepting String or Reader,
     * it will be used. Otherwise, a StringReader is returned.
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
     * @param str the string to create a Reader from
     * @return a Reader containing the string content, or {@code null} if the input string is null
     * @throws RuntimeException if the constructor invocation fails
     */
    @Override
    public Reader valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        }

        if (stringConstructor != null) {
            return (Reader) ClassUtil.invokeConstructor(stringConstructor, str);
        } else if (readerConstructor != null) {
            return (Reader) ClassUtil.invokeConstructor(readerConstructor, new StringReader(str));
        } else {
            return new StringReader(str);
        }
    }

    /**
     * Creates a Reader from various object types.
     * If the object is a Clob, its character stream is returned.
     * Otherwise, the object is converted to string and then to a Reader.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     *
     * // From String
     * Reader reader1 = type.valueOf("Hello");
     * System.out.println(IOUtil.readAllToString(reader1));   // Output: Hello
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
     * @return a Reader representation of the object, or {@code null} if the input is null
     * @throws UncheckedSQLException if accessing the Clob's character stream fails
     */
    @SuppressFBWarnings
    @Override
    public Reader valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Clob clob) {
            try {
                return clob.getCharacterStream();
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            }
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
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final int columnIndex) throws SQLException {
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
     * @param columnLabel the label of the column to retrieve (column name or alias)
     * @return the Reader for the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Reader get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getCharacterStream(columnLabel);
    }

    /**
     * Sets a Reader parameter in a PreparedStatement.
     * The Reader will be used to provide character stream data to the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * PreparedStatement stmt = connection.prepareStatement("INSERT INTO docs (content) VALUES (?)");
     * Reader reader = new StringReader("Large text content");
     * type.set(stmt, 1, reader);
     * stmt.executeUpdate();
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the Reader to set as the parameter value
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x) throws SQLException {
        stmt.setCharacterStream(columnIndex, x);
    }

    /**
     * Sets a Reader parameter in a CallableStatement.
     * The Reader will be used to provide character stream data to the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * CallableStatement stmt = connection.prepareCall("{call update_content(?)}");
     * Reader reader = new StringReader("Updated content");
     * type.set(stmt, "content", reader);
     * stmt.execute();
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Reader to set as the parameter value
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x) throws SQLException {
        stmt.setCharacterStream(parameterName, x);
    }

    /**
     * Sets a Reader parameter in a PreparedStatement with a specified length.
     * The Reader will be used to provide character stream data to the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * PreparedStatement stmt = connection.prepareStatement("INSERT INTO docs (content) VALUES (?)");
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
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setCharacterStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a Reader parameter in a CallableStatement with a specified length.
     * The Reader will be used to provide character stream data to the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * CallableStatement stmt = connection.prepareCall("{call update_content(?)}");
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
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x, final int sqlTypeOrLength) throws SQLException {
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
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the Reader whose content should be appended
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Reader x) throws IOException {
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
     * CharacterWriter writer = new CharacterWriter();
     * JSONXMLSerializationConfig<?> config = new JSONXMLSerializationConfig<>();
     * config.setStringQuotation('"');
     * type.writeCharacter(writer, reader, config);
     * System.out.println(writer.toString());   // Output: "Sample text"
     *
     * // Without quotation
     * Reader reader2 = new StringReader("No quotes");
     * CharacterWriter writer2 = new CharacterWriter();
     * type.writeCharacter(writer2, reader2, null);
     * System.out.println(writer2.toString());   // Output: No quotes
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the Reader whose content should be written
     * @param config the serialization configuration that determines string quotation
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Reader x, final JSONXMLSerializationConfig<?> config) throws IOException {
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
                    writer.writeCharacter(buf, 0, count);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                Objectory.recycle(buf);
            }

            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }
        }
    }
}
