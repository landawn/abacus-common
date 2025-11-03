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

package com.landawn.abacus.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import org.w3c.dom.Node;

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 * XML parser implementation using JAXB (Jakarta XML Binding) for serialization and deserialization.
 * 
 * <p>This class provides XML parsing capabilities using the JAXB framework, which maps Java objects
 * to XML representations and vice versa. It extends {@link AbstractXMLParser} and implements
 * all the required serialization and deserialization methods.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Annotation-based XML binding using JAXB annotations</li>
 *   <li>Support for complex object hierarchies</li>
 *   <li>Automatic marshalling and unmarshalling</li>
 *   <li>Integration with standard JAXB marshallers and unmarshallers</li>
 * </ul>
 * 
 * <p>Note: This parser requires classes to be properly annotated with JAXB annotations
 * such as {@code @XmlRootElement}, {@code @XmlElement}, etc.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * JAXBParser parser = new JAXBParser();
 * 
 * // Serialization
 * Person person = new Person("John", 30);
 * String xml = parser.serialize(person, null);
 * 
 * // Deserialization
 * Person restored = parser.deserialize(xml, null, Person.class);
 * }</pre>
 * 
 * @see AbstractXMLParser
 * @see jakarta.xml.bind.JAXB
 */
final class JAXBParser extends AbstractXMLParser {

    /**
     * Constructs a new JAXBParser with default configuration.
     * 
     * <p>This constructor creates a parser instance with default XML serialization
     * and deserialization configurations.</p>
     */
    JAXBParser() {
    }

    /**
     * Constructs a new JAXBParser with specified configurations.
     * 
     * <p>This constructor allows customization of the parser behavior through
     * XML serialization and deserialization configuration objects.</p>
     *
     * @param xsc the XML serialization configuration
     * @param xdc the XML deserialization configuration
     */
    JAXBParser(final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        super(xsc, xdc);
    }

    /**
     * Serializes an object to a string representation.
     *
     * <p>This method converts a Java object to its XML string representation using JAXB.
     * If the object is {@code null}, an empty string is returned. The object's class must be
     * properly annotated with JAXB annotations.</p>
     *
     * <p>Note: The <i>ignoredPropNames</i> configuration option is not supported by JAXB
     * and will throw a {@link ParseException} if specified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("Alice", 25);
     * String xml = parser.serialize(person, null);
     * // Result: <?xml version="1.0"?><person><name>Alice</name><age>25</age></person>
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null} depending on implementation)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @return the XML string representation of the serialized object, or empty string if obj is null
     * @throws ParseException if ignoredPropNames is specified in config or if JAXB marshalling fails
     */
    @Override
    public String serialize(final Object obj, final XMLSerializationConfig config) {
        if (obj == null) {
            return Strings.EMPTY;
        }

        final BufferedWriter bw = Objectory.createBufferedWriter();

        try {
            write(obj, config, bw);

            return bw.toString();
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Serializes an object to a file.
     *
     * <p>This method serializes a Java object to XML and writes it to the specified file.
     * The file will be created if it doesn't exist, or overwritten if it does.
     * Parent directories must exist or an exception will be thrown.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("Bob", 35);
     * File outputFile = new File("person.xml");
     * parser.serialize(person, null, outputFile);
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null} depending on implementation)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @param output the output file to write to (must not be {@code null})
     * @throws UncheckedIOException if an I/O error occurs during file writing
     * @throws ParseException if JAXB marshalling fails
     */
    @Override
    public void serialize(final Object obj, final XMLSerializationConfig config, final File output) {
        Writer writer = null;

        try {
            createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            serialize(obj, config, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Serializes an object to an output stream.
     *
     * <p>The stream is not closed after writing, allowing the caller to manage stream
     * lifecycle. The stream will be flushed after serialization. The stream is buffered
     * internally for better performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("Carol", 28);
     * try (FileOutputStream fos = new FileOutputStream("person.xml")) {
     *     parser.serialize(person, null, fos);
     * }
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null} depending on implementation)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @param output the output stream to write to (must not be {@code null})
     * @throws UncheckedIOException if an I/O error occurs during stream writing
     * @throws ParseException if JAXB marshalling fails
     */
    @Override
    public void serialize(final Object obj, final XMLSerializationConfig config, final OutputStream output) {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            write(obj, config, bw);

            bw.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Serializes an object to a writer.
     *
     * <p>The writer is not closed after writing, allowing the caller to manage writer
     * lifecycle. The writer will be flushed after serialization. The writer is automatically
     * buffered if it's not already a BufferedWriter.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("Dave", 40);
     * StringWriter sw = new StringWriter();
     * parser.serialize(person, null, sw);
     * String xml = sw.toString();
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null} depending on implementation)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @param output the writer to write to (must not be {@code null})
     * @throws UncheckedIOException if an I/O error occurs during writing
     * @throws ParseException if JAXB marshalling fails
     */
    @Override
    public void serialize(final Object obj, final XMLSerializationConfig config, final Writer output) {
        final boolean isBufferedWriter = IOUtil.isBufferedWriter(output);
        final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output);

        try {
            write(obj, config, bw);

            bw.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle((BufferedWriter) bw);
            }
        }
    }

    /**
     * Internal method to write an object as XML to a writer.
     * 
     * <p>This method handles the actual JAXB marshalling process. It creates a
     * JAXB marshaller for the object's class and uses it to convert the object
     * to XML.</p>
     *
     * @param obj the object to serialize
     * @param config the XML serialization configuration (optional)
     * @param output the writer to write to
     * @throws ParseException if ignoredPropNames is specified in config or if JAXB marshalling fails
     * @throws UncheckedIOException if an I/O error occurs
     */
    void write(final Object obj, final XMLSerializationConfig config, final Writer output) {
        if (config != null && N.notEmpty(config.getIgnoredPropNames())) {
            throw new ParseException("'ignoredPropNames' is not supported");
        }

        if (obj == null) {
            try {
                IOUtil.write(Strings.EMPTY, output);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final Marshaller marshaller = XmlUtil.createMarshaller(obj.getClass());

        try {
            marshaller.marshal(obj, output);

            output.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final JAXBException e) {
            throw new ParseException(e);
        }
    }

    /**
     * Deserializes an object from a string representation.
     *
     * <p>This method converts an XML string representation back to a Java object
     * using JAXB unmarshalling. The target class must be properly annotated with
     * JAXB annotations.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<?xml version=\"1.0\"?><person><name>Eve</name><age>22</age></person>";
     * Person person = parser.deserialize(xml, null, Person.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the source string to deserialize from (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create (must not be {@code null})
     * @return the deserialized object instance, or the default value of targetClass if source is empty
     * @throws ParseException if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final String source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        if (Strings.isEmpty(source)) {
            return N.defaultValueOf(targetClass);
        }

        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, targetClass);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     * Deserializes an object from a file.
     *
     * <p>This method reads XML from a file and converts it to a Java object
     * using JAXB unmarshalling.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File xmlFile = new File("person.xml");
     * Person person = parser.deserialize(xmlFile, null, Person.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the source file to read from (must not be {@code null} and must exist)
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create (must not be {@code null})
     * @return the deserialized object instance
     * @throws UncheckedIOException if an I/O error occurs or the file doesn't exist
     * @throws ParseException if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final File source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return deserialize(reader, config, targetClass);
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     * Deserializes an object from an input stream.
     *
     * <p>The stream is not closed after reading, allowing the caller to manage stream lifecycle.
     * The stream is buffered internally for better performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileInputStream fis = new FileInputStream("person.xml")) {
     *     Person person = parser.deserialize(fis, null, Person.class);
     * }
     * }</pre>
     *
     * @param <T> the target type
     * @param source the input stream to read from (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create (must not be {@code null})
     * @return the deserialized object instance
     * @throws UncheckedIOException if an I/O error occurs during stream reading
     * @throws ParseException if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final InputStream source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, targetClass);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     * Deserializes an object from a reader.
     *
     * <p>The reader is not closed after reading, allowing the caller to manage reader lifecycle.
     * The reader content is read and parsed according to the parser's format.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringReader sr = new StringReader(xmlString);
     * Person person = parser.deserialize(sr, null, Person.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the reader to read from (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create (must not be {@code null})
     * @return the deserialized object instance
     * @throws UncheckedIOException if an I/O error occurs during reading
     * @throws ParseException if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final Reader source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        return read(source, config, targetClass);
    }

    /**
     * Deserializes XML from a DOM Node to an object of the specified type.
     * 
     * <p>This operation is not supported by the JAXB parser implementation.</p>
     *
     * @param <T> the type of the object to deserialize to
     * @param source the DOM Node containing XML data
     * @param config the XML deserialization configuration (optional)
     * @param targetClass the class of the object to create
     * @return never returns successfully
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     */
    @Override
    public <T> T deserialize(final Node source, final XMLDeserializationConfig config, final Class<? extends T> targetClass)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes XML from an input stream using a map of node classes.
     * 
     * <p>This operation is not supported by the JAXB parser implementation.
     * JAXB requires knowing the target class at compile time through annotations.</p>
     *
     * @param <T> the type of the object to deserialize to
     * @param source the input stream containing XML data
     * @param config the XML deserialization configuration (optional)
     * @param nodeClasses a map of XML node names to their corresponding Java classes
     * @return never returns successfully
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     */
    @Override
    public <T> T deserialize(final InputStream source, final XMLDeserializationConfig config, final Map<String, Class<?>> nodeClasses)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes XML from a reader using a map of node classes.
     * 
     * <p>This operation is not supported by the JAXB parser implementation.
     * JAXB requires knowing the target class at compile time through annotations.</p>
     *
     * @param <T> the type of the object to deserialize to
     * @param source the reader containing XML data
     * @param config the XML deserialization configuration (optional)
     * @param nodeClasses a map of XML node names to their corresponding Java classes
     * @return never returns successfully
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     */
    @Override
    public <T> T deserialize(final Reader source, final XMLDeserializationConfig config, final Map<String, Class<?>> nodeClasses)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes XML from a DOM Node using a map of node classes.
     * 
     * <p>This operation is not supported by the JAXB parser implementation.
     * JAXB requires knowing the target class at compile time through annotations.</p>
     *
     * @param <T> the type of the object to deserialize to
     * @param source the DOM Node containing XML data
     * @param config the XML deserialization configuration (optional)
     * @param nodeClasses a map of XML node names to their corresponding Java classes
     * @return never returns successfully
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     */
    @Override
    public <T> T deserialize(final Node source, final XMLDeserializationConfig config, final Map<String, Class<?>> nodeClasses)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Internal method to read XML from a reader and unmarshal it to an object.
     * 
     * <p>This method handles the actual JAXB unmarshalling process. It creates a
     * JAXB unmarshaller for the target class and uses it to convert XML to
     * a Java object.</p>
     * 
     * <p>Note: The <i>ignoredPropNames</i> configuration option is not supported by JAXB
     * and will throw a {@link ParseException} if specified.</p>
     *
     * @param source the reader containing XML data
     * @param config the XML deserialization configuration (optional)
     * @param targetClass the class of the object to create
     * @param <T> the type of the object to deserialize to
     * @return the deserialized object
     * @throws ParseException if ignoredPropNames is specified in config or if JAXB unmarshalling fails
     */
    @SuppressWarnings("unchecked")
    <T> T read(final Reader source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        if (config != null && N.notEmpty(config.getIgnoredPropNames())) {
            throw new ParseException("'ignoredPropNames' is not supported");
        }

        final Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(targetClass);

        try {
            return (T) unmarshaller.unmarshal(source);
        } catch (final JAXBException e) {
            throw new ParseException(e);
        }
    }
}
