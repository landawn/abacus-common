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

import javax.xml.parsers.SAXParser;
import javax.xml.transform.sax.SAXSource;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;
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
 * to XML representations and vice versa. It extends {@link AbstractXmlParser} and implements
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
 * JaxbParser parser = new JaxbParser();
 *
 * // Serialization
 * Person person = new Person("John", 30);
 * String xml = parser.serialize(person, null);
 *
 * // Deserialization
 * Person restored = parser.deserialize(xml, null, Person.class);
 * }</pre>
 *
 * @see AbstractXmlParser
 * @see jakarta.xml.bind.JAXB
 */
final class JaxbParser extends AbstractXmlParser {

    /**
     * Constructs a new JaxbParser with default configuration.
     *
     * <p>This constructor creates a parser instance with default XML serialization
     * and deserialization configurations.</p>
     */
    JaxbParser() {
    }

    /**
     * Constructs a new JaxbParser with specified configurations.
     *
     * <p>This constructor allows customization of the parser behavior through
     * XML serialization and deserialization configuration objects.</p>
     *
     * @param xsc the XML serialization configuration
     * @param xdc the XML deserialization configuration
     */
    JaxbParser(final XmlSerConfig xsc, final XmlDeserConfig xdc) {
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
     * and will throw a {@link ParsingException} if specified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("Alice", 25);
     * String xml = parser.serialize(person, null);
     * // Result: <?xml version="1.0"?><person><name>Alice</name><age>25</age></person>
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null}; an empty result is written in that case)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @return the XML string representation of the serialized object, or an empty string if {@code obj} is {@code null}
     * @throws ParsingException if ignoredPropNames is specified in config or if JAXB marshalling fails
     */
    @Override
    public String serialize(final Object obj, final XmlSerConfig config) {
        checkSerializationConfig(config);

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
     * Missing parent directories are created automatically.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("Bob", 35);
     * File outputFile = new File("person.xml");
     * parser.serialize(person, null, outputFile);
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null}; an empty result is written in that case)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @param output the output file to write to (must not be {@code null})
     * @throws UncheckedIOException if an I/O error occurs during file writing
     * @throws ParsingException if {@code ignoredPropNames} is specified in {@code config}, or if JAXB marshalling fails
     */
    @Override
    public void serialize(final Object obj, final XmlSerConfig config, final File output) {
        // Validate before opening the destination: FileOutputStream truncates an existing file.
        checkSerializationConfig(config);

        OutputStream os = null;

        try {
            createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);
            serialize(obj, config, os);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     * Serializes an object to an output stream.
     *
     * <p>The stream is not closed after writing, allowing the caller to manage stream
     * lifecycle. The stream will be flushed after serialization. JAXB writes bytes directly
     * to the stream, so the byte encoding matches the encoding declared in the XML prolog.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("Carol", 28);
     * try (FileOutputStream fos = new FileOutputStream("person.xml")) {
     *     parser.serialize(person, null, fos);
     * }
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null}; an empty result is written in that case)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @param output the output stream to write to (must not be {@code null})
     * @throws UncheckedIOException if an I/O error occurs during stream writing
     * @throws ParsingException if {@code ignoredPropNames} is specified in {@code config}, or if JAXB marshalling fails
     */
    @Override
    public void serialize(final Object obj, final XmlSerConfig config, final OutputStream output) {
        write(obj, config, output);
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
     * @param obj the object to serialize (may be {@code null}; an empty result is written in that case)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @param output the writer to write to (must not be {@code null})
     * @throws UncheckedIOException if an I/O error occurs during writing
     * @throws ParsingException if {@code ignoredPropNames} is specified in {@code config}, or if JAXB marshalling fails
     */
    @Override
    public void serialize(final Object obj, final XmlSerConfig config, final Writer output) {
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
     * @throws ParsingException if ignoredPropNames is specified in config or if JAXB marshalling fails
     * @throws UncheckedIOException if an I/O error occurs
     */
    void write(final Object obj, final XmlSerConfig config, final Writer output) {
        checkSerializationConfig(config);

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
            throw new ParsingException(e);
        }
    }

    /**
     * Writes an object directly to a byte stream. Passing the stream to JAXB is important:
     * adapting it through a platform-default {@link Writer} can produce bytes in one encoding
     * while the XML declaration names another.
     *
     * @param obj the object to serialize
     * @param config the XML serialization configuration (optional)
     * @param output the stream to write to; it is flushed but not closed
     * @throws ParsingException if {@code ignoredPropNames} is specified or JAXB marshalling fails
     * @throws UncheckedIOException if flushing the stream fails
     */
    void write(final Object obj, final XmlSerConfig config, final OutputStream output) {
        checkSerializationConfig(config);

        try {
            if (obj != null) {
                final Marshaller marshaller = XmlUtil.createMarshaller(obj.getClass());
                marshaller.marshal(obj, output);
            }

            output.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final JAXBException e) {
            throw new ParsingException(e);
        }
    }

    /**
     * Deserializes an XML string to an object of the specified type using JAXB unmarshalling.
     * Delegates to {@link #deserialize(String, XmlDeserConfig, Class)} via {@code targetType.javaType()}.
     *
     * @param <T> the type of the target object
     * @param source the XML string to deserialize; if empty, returns the default value for the target type
     * @param config the deserialization configuration (may be {@code null} for default behavior);
     *        note that {@code ignoredPropNames} is not supported by JAXB
     * @param targetType the {@link Type} descriptor of the object to create
     * @return the deserialized object instance
     * @throws ParsingException if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final String source, final XmlDeserConfig config, final Type<? extends T> targetType) {
        return deserialize(source, config, targetType.javaType());
    }

    /**
     * Deserializes an XML string to an object of the specified class using JAXB unmarshalling.
     * Returns the default value for {@code targetClass} if {@code source} is empty.
     *
     * @param <T> the type of the target object
     * @param source the XML string to deserialize; if empty, returns the default value for the target class
     * @param config the deserialization configuration (may be {@code null} for default behavior);
     *        note that {@code ignoredPropNames} is not supported by JAXB
     * @param targetClass the class of the object to create; must be properly annotated with JAXB annotations
     * @return the deserialized object instance, or the default value for {@code targetClass} if {@code source} is empty
     * @throws ParsingException if {@code ignoredPropNames} is specified in {@code config},
     *         or if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final String source, final XmlDeserConfig config, final Class<? extends T> targetClass) {
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
     * Deserializes XML content from a file to an object of the specified type using JAXB unmarshalling.
     * Delegates to {@link #deserialize(File, XmlDeserConfig, Class)} via {@code targetType.javaType()}.
     *
     * @param <T> the type of the target object
     * @param source the file containing XML data to deserialize (must not be {@code null} and must exist)
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the {@link Type} descriptor of the object to create
     * @return the deserialized object instance
     * @throws UncheckedIOException if the file cannot be read
     * @throws ParsingException if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final File source, final XmlDeserConfig config, final Type<? extends T> targetType) {
        return deserialize(source, config, targetType.javaType());
    }

    /**
     * Deserializes XML content from a file to an object of the specified class using JAXB unmarshalling.
     *
     * @param <T> the type of the target object
     * @param source the file containing XML data to deserialize (must not be {@code null} and must exist)
     * @param config the deserialization configuration (may be {@code null} for default behavior);
     *        note that {@code ignoredPropNames} is not supported by JAXB
     * @param targetClass the class of the object to create; must be properly annotated with JAXB annotations
     * @return the deserialized object instance
     * @throws UncheckedIOException if the file cannot be read
     * @throws ParsingException if {@code ignoredPropNames} is specified in {@code config}, or if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final File source, final XmlDeserConfig config, final Class<? extends T> targetClass) {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return deserialize(is, config, targetClass);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     * Deserializes XML content from an input stream to an object of the specified type using JAXB unmarshalling.
     * Delegates to {@link #deserialize(InputStream, XmlDeserConfig, Class)} via {@code targetType.javaType()}.
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML data (must not be {@code null}); not closed after reading
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the {@link Type} descriptor of the object to create
     * @return the deserialized object instance
     * @throws ParsingException if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final InputStream source, final XmlDeserConfig config, final Type<? extends T> targetType) {
        return deserialize(source, config, targetType.javaType());
    }

    /**
     * Deserializes XML content from an input stream to an object of the specified class using JAXB unmarshalling.
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML data (must not be {@code null}); not closed after reading
     * @param config the deserialization configuration (may be {@code null} for default behavior);
     *        note that {@code ignoredPropNames} is not supported by JAXB
     * @param targetClass the class of the object to create; must be properly annotated with JAXB annotations
     * @return the deserialized object instance
     * @throws ParsingException if {@code ignoredPropNames} is specified in {@code config},
     *         or if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final InputStream source, final XmlDeserConfig config, final Class<? extends T> targetClass) {
        return read(source, config, targetClass);
    }

    /**
     * Deserializes XML content from a reader to an object of the specified type using JAXB unmarshalling.
     * Delegates to {@link #deserialize(Reader, XmlDeserConfig, Class)} via {@code targetType.javaType()}.
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML data (must not be {@code null}); not closed after reading
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the {@link Type} descriptor of the object to create
     * @return the deserialized object instance
     * @throws ParsingException if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final Reader source, final XmlDeserConfig config, final Type<? extends T> targetType) {
        return deserialize(source, config, targetType.javaType());
    }

    /**
     * Deserializes XML content from a reader to an object of the specified class using JAXB unmarshalling.
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML data (must not be {@code null}); not closed after reading
     * @param config the deserialization configuration (may be {@code null} for default behavior);
     *        note that {@code ignoredPropNames} is not supported by JAXB
     * @param targetClass the class of the object to create; must be properly annotated with JAXB annotations
     * @return the deserialized object instance
     * @throws ParsingException if {@code ignoredPropNames} is specified in {@code config},
     *         or if JAXB unmarshalling fails
     */
    @Override
    public <T> T deserialize(final Reader source, final XmlDeserConfig config, final Class<? extends T> targetClass) {
        return read(source, config, targetClass);
    }

    /**
     * Deserialization from a DOM {@link Node} is not supported by this JAXB-based parser.
     *
     * @param <T> the type of the target object
     * @param source the DOM node (not used)
     * @param config the deserialization configuration (not used)
     * @param targetType the target type (not used)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> T deserialize(final Node source, final XmlDeserConfig config, final Type<? extends T> targetType) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserialization from a DOM {@link Node} is not supported by this JAXB-based parser.
     *
     * @param <T> the type of the target object
     * @param source the DOM node (not used)
     * @param config the deserialization configuration (not used)
     * @param targetClass the target class (not used)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> T deserialize(final Node source, final XmlDeserConfig config, final Class<? extends T> targetClass) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Internal method to read XML from an input stream and unmarshal it to an object.
     *
     * <p>This method handles the actual JAXB unmarshalling process. It creates a
     * JAXB unmarshaller for the target class and uses it to convert XML to
     * a Java object.</p>
     *
     * <p>Note: The <i>ignoredPropNames</i> configuration option is not supported by JAXB
     * and will throw a {@link ParsingException} if specified.</p>
     *
     * @param <T> the type of the object to deserialize to
     * @param source the input stream containing XML data
     * @param config the XML deserialization configuration (optional)
     * @param targetClass the class of the object to create
     * @return the deserialized object
     * @throws ParsingException if ignoredPropNames is specified in config or if JAXB unmarshalling fails
     */
    <T> T read(final InputStream source, final XmlDeserConfig config, final Class<? extends T> targetClass) {
        if (config != null && N.notEmpty(config.getIgnoredPropNames())) {
            throw new ParsingException("'ignoredPropNames' is not supported");
        }

        final Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(targetClass);

        // Wrap in a SAXSource backed by the hardened SAXParser so JAXB doesn't fall back to
        // its own internal SAX factory. Without this wrap, attacker-controlled XML can read
        // local files, do SSRF, or trigger entity-expansion denial of service.
        return unmarshal(unmarshaller, new InputSource(source));
    }

    /**
     * Internal method to read XML from a reader and unmarshal it to an object.
     *
     * <p>This method handles the actual JAXB unmarshalling process. It creates a
     * JAXB unmarshaller for the target class and uses it to convert XML to
     * a Java object.</p>
     *
     * <p>Note: The <i>ignoredPropNames</i> configuration option is not supported by JAXB
     * and will throw a {@link ParsingException} if specified.</p>
     *
     * @param <T> the type of the object to deserialize to
     * @param source the reader containing XML data
     * @param config the XML deserialization configuration (optional)
     * @param targetClass the class of the object to create
     * @return the deserialized object
     * @throws ParsingException if ignoredPropNames is specified in config or if JAXB unmarshalling fails
     */
    <T> T read(final Reader source, final XmlDeserConfig config, final Class<? extends T> targetClass) {
        if (config != null && N.notEmpty(config.getIgnoredPropNames())) {
            throw new ParsingException("'ignoredPropNames' is not supported");
        }

        final Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(targetClass);

        // See InputStream overload above for XXE rationale.
        return unmarshal(unmarshaller, new InputSource(source));
    }

    /**
     * Unmarshals the given input with an XMLReader obtained from the
     * already-hardened {@code SAXParserFactory} held by {@link XmlUtil} (FEATURE_SECURE_PROCESSING,
     * disallow-doctype-decl, external-general/parameter-entities=false, no-load-external-dtd).
     * The parser is returned to {@link XmlUtil}'s pool after the synchronous JAXB call, including
     * when unmarshalling fails.
     *
     * @param <T> the target type
     * @param unmarshaller the JAXB unmarshaller
     * @param is the input source containing the XML payload
     * @return the unmarshalled value
     * @throws ParsingException if a hardened XMLReader cannot be obtained or JAXB unmarshalling fails
     */
    @SuppressWarnings("unchecked")
    private static <T> T unmarshal(final Unmarshaller unmarshaller, final InputSource is) {
        SAXParser sp = null;

        try {
            sp = XmlUtil.createSAXParser();
            return (T) unmarshaller.unmarshal(new SAXSource(sp.getXMLReader(), is));
        } catch (final SAXException e) {
            throw new ParsingException("Failed to obtain hardened XMLReader for JAXB unmarshalling", e);
        } catch (final JAXBException e) {
            throw new ParsingException(e);
        } finally {
            XmlUtil.recycleSAXParser(sp);
        }
    }

    /**
     * Rejects the serialization option that JAXB cannot honor. This check intentionally runs
     * before null short-circuits and before file destinations are opened.
     *
     * @param config the serialization configuration, or {@code null}
     * @throws ParsingException if ignored property names are configured
     */
    private static void checkSerializationConfig(final XmlSerConfig config) {
        if (config != null && N.notEmpty(config.getIgnoredPropNames())) {
            throw new ParsingException("'ignoredPropNames' is not supported");
        }
    }
}
