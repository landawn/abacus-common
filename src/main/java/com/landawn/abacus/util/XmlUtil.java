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

package com.landawn.abacus.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.XMLConstants;
import com.landawn.abacus.type.Type;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 * A comprehensive utility class providing various XML processing capabilities including JAXB marshalling/unmarshalling,
 * DOM manipulation, SAX parsing, StAX processing, and XML transformation operations.
 * 
 * <p>This class offers a wide range of static methods for:</p>
 * <ul>
 *   <li>JAXB operations (marshal/unmarshal with caching)</li>
 *   <li>DOM parsing and manipulation</li>
 *   <li>SAX parser creation with pooling</li>
 *   <li>StAX reader/writer creation</li>
 *   <li>XML transformation</li>
 *   <li>XML encoding/decoding using Java beans</li>
 *   <li>Character encoding for XML content</li>
 *   <li>Node and attribute manipulation</li>
 * </ul>
 * 
 * <p>The class employs object pooling for parsers and contexts to improve performance in high-throughput scenarios.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // JAXB marshalling
 * Person person = new Person("John", 30);
 * String xml = XmlUtil.marshal(person);
 * 
 * // JAXB unmarshalling
 * Person p = XmlUtil.unmarshal(Person.class, xml);
 * 
 * // DOM parsing
 * DocumentBuilder parser = XmlUtil.createDOMParser();
 * Document doc = parser.parse(new File("data.xml"));
 * 
 * // Get element attributes
 * Element elem = doc.getDocumentElement();
 * String attr = XmlUtil.getAttribute(elem, "id");
 * }</pre>
 *
 */
public final class XmlUtil {

    protected static final Logger logger = LoggerFactory.getLogger(XmlUtil.class);

    static final String NAME = "name";

    static final String TYPE = "type";

    private static final int POOL_SIZE = 1000;

    // ...
    private static final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

    static {
        try {
            saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            saxParserFactory.setXIncludeAware(false);
        } catch (Exception e) { // NOSONAR
            // ignore - these security features may not be supported on all platforms
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to set SAX parser security features: " + e.getMessage());
            }
        }
    }

    private static final Queue<SAXParser> saxParserPool = new ArrayBlockingQueue<>(POOL_SIZE);

    // ...
    private static final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

    static {
        try {
            docBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            docBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            docBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            docBuilderFactory.setXIncludeAware(false);
            docBuilderFactory.setExpandEntityReferences(false);
        } catch (Exception e) { // NOSONAR
            // ignore - these security features may not be supported on all platforms
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to set DocumentBuilder security features: " + e.getMessage());
            }
        }
    }

    private static final Queue<DocumentBuilder> contentDocBuilderPool = new ArrayBlockingQueue<>(POOL_SIZE);

    // private static final int BUFFER_SIZE = 1024 * 16; // 16KB
    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    static {
        try {
            if (Class.forName("com.ctc.wstx.stax.WstxInputFactory").isAssignableFrom(xmlInputFactory.getClass())) {
                // xmlInputFactory.setProperty("com.ctc.wstx.inputBufferLength", BUFFER_SIZE);
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("It's recommended to use woodstox: https://github.com/FasterXML/woodstox");
                }

                // xmlInputFactory.setProperty("javax.xml.stream.bufferSize", BUFFER_SIZE);   // 8KB buffer
            }
        } catch (final Throwable e) {
            if (logger.isWarnEnabled()) {
                logger.warn("It's recommended to use woodstox: https://github.com/FasterXML/woodstox");
            }

            // xmlInputFactory.setProperty("javax.xml.stream.bufferSize", BUFFER_SIZE);   // 8KB buffer
        }
    }

    // private static final Queue<DocumentBuilder> xmlInputPool = new ArrayBlockingQueue<>(POOL_SIZE);
    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    // private static final Queue<DocumentBuilder> xmlOutputPool = new ArrayBlockingQueue<>(POOL_SIZE);

    // ...
    private static final TransformerFactory transferFactory = TransformerFactory.newInstance();
    // private static final Queue<DocumentBuilder> xmlTransferPool = new ArrayBlockingQueue<>(POOL_SIZE);

    // ...
    private static final Map<String, JAXBContext> pathJaxbContextPool = new ConcurrentHashMap<>(POOL_SIZE);

    private static final Map<Class<?>, JAXBContext> classJaxbContextPool = new ConcurrentHashMap<>(POOL_SIZE);

    private static final Map<String, NodeType> nodeTypePool = new HashMap<>();

    static {
        nodeTypePool.put(XMLConstants.ARRAY, NodeType.ARRAY);
        nodeTypePool.put(XMLConstants.LIST, NodeType.COLLECTION);
        nodeTypePool.put(XMLConstants.E, NodeType.ELEMENT);
        nodeTypePool.put(XMLConstants.MAP, NodeType.MAP);
        nodeTypePool.put(XMLConstants.ENTRY, NodeType.ENTRY);
        nodeTypePool.put(XMLConstants.KEY, NodeType.KEY);
        nodeTypePool.put(XMLConstants.VALUE, NodeType.VALUE);
    }

    private XmlUtil() {
        // singleton.
    }

    /**
     * Marshals the given JAXB bean into an XML string.
     * The JAXBContext is cached for the bean's class to improve performance on repeated operations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @XmlRootElement
     * public class Person {
     *     private String name;
     *     private int age;
     *     // getters and setters
     * }
     * 
     * Person person = new Person("John", 30);
     * String xml = XmlUtil.marshal(person);
     * // Result: <?xml version="1.0" encoding="UTF-8" standalone="yes"?><person><age>30</age><name>John</name></person>
     * }</pre>
     *
     * @param jaxbBean The JAXB-annotated bean to be marshaled
     * @return The XML string representation of the JAXB bean
     * @throws RuntimeException if marshalling fails
     * @see JAXBContext#newInstance(Class...)
     * @see Marshaller#marshal(Object, java.io.OutputStream)
     */
    public static String marshal(final Object jaxbBean) {
        final Class<?> cls = jaxbBean.getClass();
        JAXBContext jc = classJaxbContextPool.get(cls);
        final ByteArrayOutputStream writer = Objectory.createByteArrayOutputStream();

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(cls);
                classJaxbContextPool.put(cls, jc);
            }

            final Marshaller marshaller = jc.createMarshaller();
            marshaller.marshal(jaxbBean, writer);
            writer.flush();

            return writer.toString();
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     * Unmarshal the given XML string into an object of the specified class.
     * The JAXBContext is cached for the target class to improve performance on repeated operations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><age>30</age><name>John</name></person>";
     * Person person = XmlUtil.unmarshal(Person.class, xml);
     * System.out.println(person.getName());   // Prints: John
     * }</pre>
     *
     * @param <T> The type of the object to be returned
     * @param cls The class of the object to be returned (must be JAXB-annotated)
     * @param xml The XML string to be unmarshalled
     * @return The unmarshalled object of the specified class
     * @throws RuntimeException if unmarshalling fails
     * @see JAXBContext#newInstance(Class...)
     * @see Unmarshaller#unmarshal(Reader)
     */
    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(final Class<? extends T> cls, final String xml) {
        JAXBContext jc = classJaxbContextPool.get(cls);

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(cls);
                classJaxbContextPool.put(cls, jc);
            }

            final Unmarshaller unmarshaller = jc.createUnmarshaller();
            final StringReader reader = new StringReader(xml);

            return (T) unmarshaller.unmarshal(reader);
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates a JAXB Marshaller for the given context path.
     * The JAXBContext is cached to improve performance on repeated operations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Marshaller marshaller = XmlUtil.createMarshaller("com.example.model");
     * marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
     * marshaller.marshal(jaxbObject, System.out);
     * }</pre>
     *
     * @param contextPath The context path for which to create the Marshaller (package names separated by ':')
     * @return The created Marshaller
     * @throws RuntimeException if the Marshaller cannot be created
     * @see JAXBContext#newInstance(String)
     * @see JAXBContext#createMarshaller()
     */
    public static Marshaller createMarshaller(final String contextPath) {
        JAXBContext jc = pathJaxbContextPool.get(contextPath);

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(contextPath);
                pathJaxbContextPool.put(contextPath, jc);
            }

            return jc.createMarshaller();
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates a JAXB Marshaller for the given class.
     * The JAXBContext is cached to improve performance on repeated operations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Marshaller marshaller = XmlUtil.createMarshaller(Person.class);
     * marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
     * marshaller.marshal(person, new File("person.xml"));
     * }</pre>
     *
     * @param cls The class for which to create the Marshaller (must be JAXB-annotated)
     * @return The created Marshaller
     * @throws RuntimeException if the Marshaller cannot be created
     * @see JAXBContext#newInstance(Class...)
     * @see JAXBContext#createMarshaller()
     */
    public static Marshaller createMarshaller(final Class<?> cls) {
        JAXBContext jc = classJaxbContextPool.get(cls);

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(cls);
                classJaxbContextPool.put(cls, jc);
            }

            return jc.createMarshaller();
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates a JAXB Unmarshaller for the given context path.
     * The JAXBContext is cached to improve performance on repeated operations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Unmarshaller unmarshaller = XmlUtil.createUnmarshaller("com.example.model");
     * Object result = unmarshaller.unmarshal(new File("data.xml"));
     * }</pre>
     *
     * @param contextPath The context path for which to create the Unmarshaller (package names separated by ':')
     * @return The created Unmarshaller
     * @throws RuntimeException if the Unmarshaller cannot be created
     * @see JAXBContext#newInstance(String)
     * @see JAXBContext#createUnmarshaller()
     */
    public static Unmarshaller createUnmarshaller(final String contextPath) {
        JAXBContext jc = pathJaxbContextPool.get(contextPath);

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(contextPath);
                pathJaxbContextPool.put(contextPath, jc);
            }

            return jc.createUnmarshaller();
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates a JAXB Unmarshaller for the given class.
     * The JAXBContext is cached to improve performance on repeated operations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(Person.class);
     * Person person = (Person) unmarshaller.unmarshal(new StringReader(xmlString));
     * }</pre>
     *
     * @param cls The class for which to create the Unmarshaller (must be JAXB-annotated)
     * @return The created Unmarshaller
     * @throws RuntimeException if the Unmarshaller cannot be created
     * @see JAXBContext#newInstance(Class...)
     * @see JAXBContext#createUnmarshaller()
     */
    public static Unmarshaller createUnmarshaller(final Class<?> cls) {
        JAXBContext jc = classJaxbContextPool.get(cls);

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(cls);
                classJaxbContextPool.put(cls, jc);
            }

            return jc.createUnmarshaller();
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates a new instance of {@code DocumentBuilder} with default configuration.
     * The parser is configured with the factory's default settings.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DocumentBuilder parser = XmlUtil.createDOMParser();
     * Document doc = parser.parse(new File("data.xml"));
     * Element root = doc.getDocumentElement();
     * }</pre>
     *
     * @return A new instance of {@code DocumentBuilder}
     * @throws RuntimeException if the parser cannot be created
     * @see DocumentBuilderFactory#newDocumentBuilder()
     */
    public static DocumentBuilder createDOMParser() {
        synchronized (docBuilderFactory) {
            try {
                return docBuilderFactory.newDocumentBuilder();
            } catch (final ParserConfigurationException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Creates a new instance of {@code DocumentBuilder} with the specified configuration.
     * This method allows control over comment and whitespace handling.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create parser that ignores comments and whitespace
     * DocumentBuilder parser = XmlUtil.createDOMParser(true, true);
     * Document doc = parser.parse(xmlFile);
     * }</pre>
     *
     * @param ignoreComments Whether to ignore comments in the XML
     * @param ignoringElementContentWhitespace Whether to ignore whitespace in element content
     * @return A new instance of {@code DocumentBuilder} with the specified configuration
     * @throws RuntimeException if the parser cannot be created
     * @see DocumentBuilderFactory#newDocumentBuilder()
     */
    public static DocumentBuilder createDOMParser(final boolean ignoreComments, final boolean ignoringElementContentWhitespace) {
        DocumentBuilder documentBuilder = null;

        synchronized (docBuilderFactory) {
            try {
                final boolean orgIgnoreComments = docBuilderFactory.isIgnoringComments();
                final boolean orgIgnoringElementContentWhitespace = docBuilderFactory.isIgnoringElementContentWhitespace();

                docBuilderFactory.setIgnoringComments(ignoreComments);
                docBuilderFactory.setIgnoringElementContentWhitespace(ignoringElementContentWhitespace);

                documentBuilder = docBuilderFactory.newDocumentBuilder();

                docBuilderFactory.setIgnoringComments(orgIgnoreComments);
                docBuilderFactory.setIgnoringElementContentWhitespace(orgIgnoringElementContentWhitespace);
            } catch (final ParserConfigurationException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        return documentBuilder;
    }

    /**
     * Creates a new instance of {@code DocumentBuilder} optimized for parsing content.
     * The parser is pre-configured to ignore comments and element content whitespace.
     * This method uses object pooling for better performance.
     * 
     * <p>Important: Call {@link #recycleContentParser(DocumentBuilder)} when done to return the parser to the pool.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DocumentBuilder parser = XmlUtil.createContentParser();
     * try {
     *     Document doc = parser.parse(xmlInputStream);
     *     // Process document
     * } finally {
     *     XmlUtil.recycleContentParser(parser);
     * }
     * }</pre>
     *
     * @return A {@code DocumentBuilder} instance from the pool or newly created
     * @throws RuntimeException if the parser cannot be created
     */
    public static DocumentBuilder createContentParser() {
        synchronized (contentDocBuilderPool) {
            DocumentBuilder documentBuilder = contentDocBuilderPool.poll();

            try {
                if (documentBuilder == null) {
                    final boolean orgIgnoreComments = docBuilderFactory.isIgnoringComments();
                    final boolean orgIgnoringElementContentWhitespace = docBuilderFactory.isIgnoringElementContentWhitespace();

                    if (!orgIgnoreComments) {
                        docBuilderFactory.setIgnoringComments(true);
                    }

                    if (!orgIgnoringElementContentWhitespace) {
                        docBuilderFactory.setIgnoringElementContentWhitespace(true);
                    }

                    documentBuilder = docBuilderFactory.newDocumentBuilder();

                    docBuilderFactory.setIgnoringComments(orgIgnoreComments);
                    docBuilderFactory.setIgnoringElementContentWhitespace(orgIgnoringElementContentWhitespace);
                }
            } catch (final ParserConfigurationException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }

            return documentBuilder;
        }
    }

    /**
     * Recycles the given DocumentBuilder instance by resetting it and adding it back to the pool.
     * This method should be called after using a DocumentBuilder obtained from {@link #createContentParser()}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DocumentBuilder parser = XmlUtil.createContentParser();
     * try {
     *     // Use parser
     * } finally {
     *     XmlUtil.recycleContentParser(parser);
     * }
     * }</pre>
     *
     * @param docBuilder The DocumentBuilder instance to be recycled (can be null)
     */
    public static void recycleContentParser(final DocumentBuilder docBuilder) {
        if (docBuilder == null) {
            return;
        }

        synchronized (contentDocBuilderPool) {
            if (contentDocBuilderPool.size() < POOL_SIZE) {
                docBuilder.reset();
                contentDocBuilderPool.add(docBuilder);
            }
        }
    }

    /**
     * Creates a new instance of {@code SAXParser} from a pool or creates a new one if the pool is empty.
     * This method uses object pooling for better performance.
     * 
     * <p>Important: Call {@link #recycleSAXParser(SAXParser)} when done to return the parser to the pool.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SAXParser parser = XmlUtil.createSAXParser();
     * try {
     *     parser.parse(xmlFile, myHandler);
     * } finally {
     *     XmlUtil.recycleSAXParser(parser);
     * }
     * }</pre>
     *
     * @return A new instance of {@code SAXParser}
     * @throws RuntimeException if the parser cannot be created
     * @throws ParseException if SAX parsing configuration fails
     * @see SAXParserFactory#newSAXParser()
     */
    public static SAXParser createSAXParser() {
        synchronized (saxParserPool) {
            SAXParser saxParser = saxParserPool.poll();

            if (saxParser == null) {
                try {
                    saxParser = saxParserFactory.newSAXParser();
                } catch (final ParserConfigurationException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                } catch (final SAXException e) {
                    throw new ParseException(e);
                }
            }

            return saxParser;
        }
    }

    /**
     * Recycles the given SAXParser instance by resetting it and adding it back to the pool.
     * This method should be called after using a SAXParser obtained from {@link #createSAXParser()}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SAXParser parser = XmlUtil.createSAXParser();
     * try {
     *     // Use parser
     * } finally {
     *     XmlUtil.recycleSAXParser(parser);
     * }
     * }</pre>
     *
     * @param saxParser The SAXParser instance to be recycled (can be null)
     */
    public static void recycleSAXParser(final SAXParser saxParser) {
        if (saxParser == null) {
            return;
        }

        synchronized (saxParserPool) {
            if (saxParserPool.size() < POOL_SIZE) {
                saxParser.reset();
                saxParserPool.add(saxParser);
            }
        }
    }

    /**
     * Creates an XMLStreamReader from the given Reader source.
     * This is used for StAX (Streaming API for XML) parsing.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringReader reader = new StringReader(xmlString);
     * XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(reader);
     * while (xmlReader.hasNext()) {
     *     int event = xmlReader.next();
     *     // Process events
     * }
     * }</pre>
     *
     * @param source The Reader source from which to create the XMLStreamReader
     * @return The created XMLStreamReader
     * @throws RuntimeException if an XMLStreamException occurs
     * @see XMLInputFactory#createXMLStreamReader(Reader)
     */
    public static XMLStreamReader createXMLStreamReader(final Reader source) {
        try {
            return xmlInputFactory.createXMLStreamReader(source);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates an XMLStreamReader from the given InputStream source.
     * This is used for StAX (Streaming API for XML) parsing.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FileInputStream fis = new FileInputStream("data.xml");
     * XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(fis);
     * // Process XML stream
     * }</pre>
     *
     * @param source The InputStream source from which to create the XMLStreamReader
     * @return The created XMLStreamReader
     * @throws RuntimeException if an XMLStreamException occurs
     * @see XMLInputFactory#createXMLStreamReader(InputStream)
     */
    public static XMLStreamReader createXMLStreamReader(final InputStream source) {
        try {
            return xmlInputFactory.createXMLStreamReader(source);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates an XMLStreamReader from the given InputStream source with the specified encoding.
     * This is used for StAX (Streaming API for XML) parsing with explicit character encoding.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FileInputStream fis = new FileInputStream("data.xml");
     * XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(fis, "UTF-8");
     * }</pre>
     *
     * @param source The InputStream source from which to create the XMLStreamReader
     * @param encoding The character encoding to be used (e.g., "UTF-8", "ISO-8859-1")
     * @return The created XMLStreamReader
     * @throws RuntimeException if an XMLStreamException occurs
     * @see XMLInputFactory#createXMLStreamReader(InputStream, String)
     */
    public static XMLStreamReader createXMLStreamReader(final InputStream source, final String encoding) {
        try {
            return xmlInputFactory.createXMLStreamReader(source, encoding);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates a filtered XMLStreamReader from the given source XMLStreamReader and StreamFilter.
     * The filter allows selective processing of XML events.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLStreamReader reader = XmlUtil.createXMLStreamReader(inputStream);
     * StreamFilter filter = new StreamFilter() {
     *     public boolean accept(XMLStreamReader reader) {
     *         return reader.isStartElement() || reader.isEndElement();
     *     }
     * };
     * XMLStreamReader filteredReader = XmlUtil.createFilteredStreamReader(reader, filter);
     * }</pre>
     *
     * @param source The source XMLStreamReader to be filtered
     * @param filter The StreamFilter to apply to the source
     * @return The filtered XMLStreamReader
     * @throws RuntimeException if an XMLStreamException occurs
     * @see XMLInputFactory#createFilteredReader(XMLStreamReader, StreamFilter)
     */
    public static XMLStreamReader createFilteredStreamReader(final XMLStreamReader source, final StreamFilter filter) {
        try {
            return xmlInputFactory.createFilteredReader(source, filter);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates an XMLStreamWriter from the given Writer output.
     * This is used for StAX (Streaming API for XML) writing.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringWriter writer = new StringWriter();
     * XMLStreamWriter xmlWriter = XmlUtil.createXMLStreamWriter(writer);
     * xmlWriter.writeStartDocument();
     * xmlWriter.writeStartElement("root");
     * xmlWriter.writeCharacters("Hello XML");
     * xmlWriter.writeEndElement();
     * xmlWriter.writeEndDocument();
     * }</pre>
     *
     * @param output The Writer output to which the XMLStreamWriter will write
     * @return The created XMLStreamWriter
     * @throws RuntimeException if an XMLStreamException occurs
     * @see XMLOutputFactory#createXMLStreamWriter(Writer)
     */
    public static XMLStreamWriter createXMLStreamWriter(final Writer output) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(output);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates an XMLStreamWriter from the given OutputStream.
     * This is used for StAX (Streaming API for XML) writing with default encoding.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FileOutputStream fos = new FileOutputStream("output.xml");
     * XMLStreamWriter xmlWriter = XmlUtil.createXMLStreamWriter(fos);
     * // Write XML content
     * xmlWriter.close();
     * }</pre>
     *
     * @param output The OutputStream to which the XMLStreamWriter will write
     * @return The created XMLStreamWriter
     * @throws RuntimeException if an XMLStreamException occurs
     * @see XMLOutputFactory#createXMLStreamWriter(OutputStream)
     */
    public static XMLStreamWriter createXMLStreamWriter(final OutputStream output) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(output);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates an XMLStreamWriter from the given OutputStream with the specified encoding.
     * This is used for StAX (Streaming API for XML) writing with explicit character encoding.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FileOutputStream fos = new FileOutputStream("output.xml");
     * XMLStreamWriter xmlWriter = XmlUtil.createXMLStreamWriter(fos, "UTF-8");
     * xmlWriter.writeStartDocument("UTF-8", "1.0");
     * // Write XML content
     * }</pre>
     *
     * @param output The OutputStream to which the XMLStreamWriter will write
     * @param encoding The character encoding to be used (e.g., "UTF-8", "ISO-8859-1")
     * @return The created XMLStreamWriter
     * @throws RuntimeException if an XMLStreamException occurs
     * @see XMLOutputFactory#createXMLStreamWriter(OutputStream, String)
     */
    public static XMLStreamWriter createXMLStreamWriter(final OutputStream output, final String encoding) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(output, encoding);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates a new instance of Transformer for XML transformation operations.
     * The Transformer can be used to transform XML documents using XSLT or for serialization.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Transformer transformer = XmlUtil.createXMLTransformer();
     * transformer.setOutputProperty(OutputKeys.INDENT, "yes");
     * transformer.transform(new DOMSource(document), new StreamResult(outputFile));
     * }</pre>
     *
     * @return A new instance of Transformer
     * @throws RuntimeException if a TransformerConfigurationException occurs
     * @see TransformerFactory#newTransformer()
     */
    public static Transformer createXMLTransformer() {
        try {
            return transferFactory.newTransformer();
        } catch (final TransformerConfigurationException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Transforms the given XML Document to the specified output file.
     * The file will be created if it doesn't exist, or overwritten if it does.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = parser.parse(inputFile);
     * // Modify document
     * XmlUtil.transform(doc, new File("output.xml"));
     * }</pre>
     *
     * @param source The XML Document to be transformed
     * @param output The output file where the transformed XML will be written
     * @throws UncheckedIOException if an IOException occurs during file operations
     * @see Transformer#transform(Source, Result)
     */
    public static void transform(final Document source, File output) {
        output = PropertiesUtil.formatPath(output);

        Writer writer = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            transform(source, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Transforms the given XML Document to the specified OutputStream.
     * The stream is not closed by this method.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = createDocument();
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * XmlUtil.transform(doc, baos);
     * String xmlString = baos.toString("UTF-8");
     * }</pre>
     *
     * @param source The XML Document to be transformed
     * @param output The OutputStream where the transformed XML will be written
     * @throws RuntimeException if a TransformerException occurs
     * @see Transformer#transform(Source, Result)
     */
    public static void transform(final Document source, final OutputStream output) {
        // Prepare the DOM document for writing
        final Source domSource = new DOMSource(source);

        final Result result = new StreamResult(output);

        try {
            createXMLTransformer().transform(domSource, result);
        } catch (final TransformerException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Transforms the given XML Document to the specified Writer.
     * The writer is not closed by this method.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = createDocument();
     * StringWriter writer = new StringWriter();
     * XmlUtil.transform(doc, writer);
     * String xmlString = writer.toString();
     * }</pre>
     *
     * @param source The XML Document to be transformed
     * @param output The Writer where the transformed XML will be written
     * @throws RuntimeException if a TransformerException occurs
     * @see Transformer#transform(Source, Result)
     */
    public static void transform(final Document source, final Writer output) {
        // Prepare the DOM document for writing
        final Source domSource = new DOMSource(source);

        final Result result = new StreamResult(output);

        try {
            createXMLTransformer().transform(domSource, result);
        } catch (final TransformerException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Encodes the given bean object into an XML string using Java's XMLEncoder.
     * This method is suitable for serializing JavaBeans with standard getter/setter patterns.
     * 
     * <p>Note: This uses Java's built-in XMLEncoder, not JAXB. The output format is specific
     * to Java serialization and may not be suitable for interoperability with non-Java systems.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String xml = XmlUtil.xmlEncode(person);
     * // Result contains Java-specific XML encoding
     * }</pre>
     *
     * @param bean The object to be encoded into XML
     * @return The XML string representation of the given object
     * @see XMLEncoder#writeObject(Object)
     */
    public static String xmlEncode(final Object bean) {
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

        try (XMLEncoder xmlEncoder = new XMLEncoder(os)) {
            xmlEncoder.writeObject(bean);
            xmlEncoder.flush();
        }

        final String result = os.toString();
        Objectory.recycle(os);

        return result;
    }

    /**
     * Decodes the given XML string into an object using Java's XMLDecoder.
     * This method is the counterpart to {@link #xmlEncode(Object)} and should be used
     * to decode XML created by XMLEncoder.
     * 
     * <p>Note: This uses Java's built-in XMLDecoder, not JAXB. The XML format must be
     * compatible with Java's XMLEncoder output.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = XmlUtil.xmlEncode(originalPerson);
     * Person decodedPerson = XmlUtil.xmlDecode(xml);
     * }</pre>
     *
     * @param <T> The type of the object to be returned
     * @param xml The XML string to be decoded
     * @return The decoded object
     * @see XMLDecoder#readObject()
     */
    public static <T> T xmlDecode(final String xml) {
        InputStream is = null;
        XMLDecoder xmlDecoder = null;
        try {
            is = new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8));
            xmlDecoder = new XMLDecoder(is);
            return (T) xmlDecoder.readObject();
        } finally {
            if (xmlDecoder != null) {
                xmlDecoder.close();
            }
        }
    }

    /**
     * Gets all direct child elements with the specified tag name from the given parent element.
     * This method only returns elements that are immediate children of the parent node,
     * not descendants at deeper levels.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element parent = doc.getDocumentElement();
     * List<Element> childElements = XmlUtil.getElementsByTagName(parent, "child");
     * for (Element child : childElements) {
     *     System.out.println(child.getTextContent());
     * }
     * }</pre>
     *
     * @param node The parent element to search within
     * @param tagName The tag name of the elements to find
     * @return A list of elements with the specified tag name that are direct children of the given node
     */
    public static List<Element> getElementsByTagName(final Element node, final String tagName) {
        final List<Element> result = new ArrayList<>();
        final NodeList nodeList = node.getElementsByTagName(tagName);

        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getParentNode().isSameNode(node)) {
                result.add((Element) nodeList.item(i));
            }
        }

        return result;
    }

    /**
     * Gets all nodes with the specified name from the given node and its descendants.
     * This method performs a recursive search through the entire node tree.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = parser.parse(xmlFile);
     * List<Node> allPriceNodes = XmlUtil.getNodesByName(doc, "price");
     * }</pre>
     *
     * @param node The parent node to search within
     * @param nodeName The name of the nodes to find
     * @return A list of all nodes with the specified name
     */
    public static List<Node> getNodesByName(final Node node, final String nodeName) {
        final List<Node> nodes = new ArrayList<>();

        getNodesByName(node, nodeName, nodes);

        return nodes;
    }

    private static void getNodesByName(final Node node, final String nodeName, final List<Node> output) {
        if (node.getNodeName().equals(nodeName)) {
            output.add(node);
        }

        final NodeList nodeList = node.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            getNodesByName(nodeList.item(i), nodeName, output);
        }
    }

    /**
     * Gets the first node with the specified name from the given node or its descendants.
     * This method performs a depth-first search and returns the first matching node found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = parser.parse(xmlFile);
     * Node firstPriceNode = XmlUtil.getNextNodeByName(doc, "price");
     * if (firstPriceNode != null) {
     *     System.out.println("Price: " + firstPriceNode.getTextContent());
     * }
     * }</pre>
     *
     * @param node The parent node to search within
     * @param nodeName The name of the node to find
     * @return The first node with the specified name, or {@code null} if no such node is found
     */
    @MayReturnNull
    public static Node getNextNodeByName(final Node node, final String nodeName) {
        if (node.getNodeName().equals(nodeName)) {
            return node;
        } else {
            final NodeList nodeList = node.getChildNodes();

            Node subNode = null;

            for (int i = 0; i < nodeList.getLength(); i++) {
                subNode = nodeList.item(i);

                if (subNode.getNodeName().equals(nodeName)) {
                    return subNode;
                }
            }

            Node nextNode = null;

            for (int i = 0; i < nodeList.getLength(); i++) {
                nextNode = getNextNodeByName(nodeList.item(i), nodeName);

                if (nextNode != null) {
                    return nextNode;
                }
            }
        }

        return null;
    }

    /**
     * Gets the attribute value of the specified attribute name from the given XML node.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element element = doc.getElementById("myElement");
     * String id = XmlUtil.getAttribute(element, "id");
     * String className = XmlUtil.getAttribute(element, "class");
     * }</pre>
     *
     * @param node The XML node from which to get the attribute
     * @param attrName The name of the attribute to retrieve
     * @return The value of the specified attribute, or {@code null} if the attribute does not exist
     */
    @MayReturnNull
    public static String getAttribute(final Node node, final String attrName) {
        final NamedNodeMap attrsNode = node.getAttributes();

        if (attrsNode == null) {
            return null;
        }

        final Node attrNode = attrsNode.getNamedItem(attrName);

        return (attrNode == null) ? null : attrNode.getNodeValue();
    }

    /**
     * Reads all attributes of the given XML node and returns them as a map.
     * The map keys are attribute names and values are attribute values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element element = doc.getElementById("myElement");
     * Map<String, String> attrs = XmlUtil.readAttributes(element);
     * for (Map.Entry<String, String> entry : attrs.entrySet()) {
     *     System.out.println(entry.getKey() + "=" + entry.getValue());
     * }
     * }</pre>
     *
     * @param node The XML node from which to read the attributes
     * @return A map containing the attributes of the given node, where keys are attribute names and values are attribute values
     */
    public static Map<String, String> readAttributes(final Node node) {
        return readAttributes(Strings.EMPTY, node, new LinkedHashMap<>());
    }

    private static Map<String, String> readAttributes(final String parentNodeName, final Node node, final Map<String, String> output) {
        final NamedNodeMap attrNodes = node.getAttributes();

        if (attrNodes == null || attrNodes.getLength() == 0) {
            return output;
        }

        final boolean isEmptyParentNodeName = Strings.isEmpty(parentNodeName);

        for (int i = 0; i < attrNodes.getLength(); i++) {
            final String attrName = attrNodes.item(i).getNodeName();
            final String attrValue = attrNodes.item(i).getNodeValue();

            if (isEmptyParentNodeName) {
                output.put(attrName, attrValue);
            } else {
                output.put(parentNodeName + "." + attrName, attrValue);
            }
        }

        return output;
    }

    /**
     * Reads the given XML element and returns its attributes and text content as a map.
     * This method recursively processes the element and all its child elements,
     * creating a flattened map with dot-notation keys for nested elements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element root = doc.getDocumentElement();
     * Map<String, String> data = XmlUtil.readElement(root);
     * // For XML: <person age="30"><name>John</name></person>
     * // Result: {"age"="30", "person.name"="John"}
     * }</pre>
     *
     * @param element The XML element to be read
     * @return A map containing the attributes and text content of the given element and its descendants
     */
    public static Map<String, String> readElement(final Element element) {
        return readElement(Strings.EMPTY, element, new LinkedHashMap<>());
    }

    private static Map<String, String> readElement(final String parentNodeName, final Element element, final Map<String, String> output) {
        readAttributes(parentNodeName, element, output);

        final boolean isEmptyParentNodeName = Strings.isEmpty(parentNodeName);

        if (isTextElement(element)) {
            final String nodeName = element.getNodeName();
            final String nodeText = Strings.strip(getTextContent(element));

            if (isEmptyParentNodeName) {
                output.put(nodeName, nodeText);
            } else {
                output.put(parentNodeName + "." + nodeName, nodeText);
            }
        }

        final String nextParentNodeName = isEmptyParentNodeName ? element.getNodeName() : parentNodeName + "." + element.getNodeName();
        final NodeList childNodeList = element.getChildNodes();

        for (int childNodeIndex = 0; childNodeIndex < childNodeList.getLength(); childNodeIndex++) {
            final Node childNode = childNodeList.item(childNodeIndex);

            if (childNode instanceof Element childElement) {
                readElement(nextParentNodeName, childElement, output);
            }
        }

        return output;
    }

    /**
     * Checks if the given node is a text element.
     * A text element is defined as an element that does not contain any child elements,
     * only text content or other non-element nodes (like comments or text nodes).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element elem = doc.createElement("name");
     * elem.setTextContent("John");
     * boolean isText = XmlUtil.isTextElement(elem);   // Returns true
     * 
     * Element parent = doc.createElement("person");
     * parent.appendChild(elem);
     * boolean isParentText = XmlUtil.isTextElement(parent);   // Returns false
     * }</pre>
     *
     * @param node The node to be checked
     * @return {@code true} if the node is a text element, {@code false} otherwise
     */
    public static boolean isTextElement(final Node node) {
        final NodeList childNodeList = node.getChildNodes();

        for (int i = 0; i < childNodeList.getLength(); i++) {
            if (childNodeList.item(i).getNodeType() == Document.ELEMENT_NODE) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the text content of the given XML node.
     * This method returns the text content with leading and trailing whitespace preserved.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element elem = doc.createElement("message");
     * elem.setTextContent("  Hello World  ");
     * String content = XmlUtil.getTextContent(elem);   // Returns "  Hello World  "
     * }</pre>
     *
     * @param node The XML node from which to get the text content
     * @return The text content of the specified node
     */
    public static String getTextContent(final Node node) {
        return node.getTextContent();
    }

    /**
     * Gets the text content of the given XML node, optionally processing whitespace characters.
     * When ignoreWhiteChar is {@code true}, all whitespace characters (tabs, newlines, etc.) within
     * the text content are normalized to single spaces, and leading/trailing spaces are removed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element elem = doc.createElement("message");
     * elem.setTextContent("  Hello\n\tWorld  ");
     * String content = XmlUtil.getTextContent(elem, true);       // Returns "Hello World"
     * String rawContent = XmlUtil.getTextContent(elem, false);   // Returns "  Hello\n\tWorld  "
     * }</pre>
     *
     * @param node The XML node from which to get the text content
     * @param ignoreWhiteChar Whether to normalize whitespace characters in the text content
     * @return The text content of the specified node, with whitespace processed if requested
     */
    public static String getTextContent(final Node node, final boolean ignoreWhiteChar) {
        String textContent = node.getTextContent();

        if (ignoreWhiteChar && Strings.isNotEmpty(textContent)) {
            final StringBuilder sb = Objectory.createStringBuilder();

            for (final char c : textContent.toCharArray()) {
                switch (c) {
                    case '\t':
                    case '\b':
                    case '\n':
                    case '\r':
                    case '\f':

                        if ((!sb.isEmpty()) && (sb.charAt(sb.length() - 1) != ' ')) {
                            sb.append(' ');
                        }

                        break;

                    default:
                        sb.append(c);
                }
            }

            final int length = sb.length();

            if ((length > 0) && ((sb.charAt(0) == ' ') || (sb.charAt(length - 1) == ' '))) {
                int from = 0;

                do {
                    if (sb.charAt(from) != ' ') {
                        break;
                    }

                    from++;
                } while (from < length);

                int to = length - 1;

                do {
                    if (sb.charAt(to) != ' ') {
                        break;
                    }

                    to--;
                } while (to >= 0);

                if (from <= to) {
                    textContent = sb.substring(from, to + 1);
                } else {
                    textContent = "";
                }
            } else {
                textContent = sb.toString();
            }

            Objectory.recycle(sb);
        }

        return textContent;
    }

    /**
     * Writes XML-escaped characters from the specified character array to the given StringBuilder.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Hello <world> & \"friends\"".toCharArray();
     * StringBuilder sb = new StringBuilder();
     * XmlUtil.writeCharacters(chars, sb);
     * // Result: "Hello <world> &amp; &quot;friends&quot;"
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param output The StringBuilder to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(final char[] cbuf, final StringBuilder output) throws IOException {
        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     * Writes XML-escaped characters from a portion of a character array to the given StringBuilder.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Data: <value> & 'text'".toCharArray();
     * StringBuilder sb = new StringBuilder();
     * XmlUtil.writeCharacters(chars, 6, 14, sb);
     * // Result: "<value> &amp; "
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param off The start offset in the character array
     * @param len The number of characters to write
     * @param output The StringBuilder to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(final char[] cbuf, final int off, final int len, final StringBuilder output) throws IOException {
        writeCharacters(cbuf, off, len, IOUtil.stringBuilder2Writer(output));
    }

    /**
     * Writes XML-escaped characters from the specified string to the given StringBuilder.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * If the string is {@code null}, the text "null" is written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * XmlUtil.writeCharacters("<tag attr='value'>text & more</tag>", sb);
     * // Result: "<tag attr=&apos;value&apos;>text &amp; more</tag>"
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param output The StringBuilder to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(String str, final StringBuilder output) throws IOException {
        str = (str == null) ? Strings.NULL : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     * Writes XML-escaped characters from a portion of a string to the given StringBuilder.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String text = "Hello <world> & friends";
     * StringBuilder sb = new StringBuilder();
     * XmlUtil.writeCharacters(text, 6, 8, sb);
     * // Result: "<world>"
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param off The start offset in the string
     * @param len The number of characters to write
     * @param output The StringBuilder to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(final String str, final int off, final int len, final StringBuilder output) throws IOException {
        writeCharacters(str, off, len, IOUtil.stringBuilder2Writer(output));
    }

    /**
     * Writes XML-escaped characters from the specified character array to the given OutputStream.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "<data>value & more</data>".toCharArray();
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * XmlUtil.writeCharacters(chars, baos);
     * // Result: "<data>value &amp; more</data>"
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param output The OutputStream to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(final char[] cbuf, final OutputStream output) throws IOException {
        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     * Writes XML-escaped characters from a portion of a character array to the given OutputStream.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * Uses a BufferedXMLWriter internally for efficient writing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Data: <value> & 'text'".toCharArray();
     * FileOutputStream fos = new FileOutputStream("output.xml");
     * XmlUtil.writeCharacters(chars, 7, 14, fos);
     * // Writes: "<value> &amp; "
     * fos.close();
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param off The start offset in the character array
     * @param len The number of characters to write
     * @param output The OutputStream to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(final char[] cbuf, final int off, final int len, final OutputStream output) throws IOException {
        final BufferedXMLWriter bufWriter = Objectory.createBufferedXMLWriter(output);   //NOSONAR

        try {
            bufWriter.writeCharacter(cbuf, off, len);
            bufWriter.flush();
        } finally {
            Objectory.recycle(bufWriter);
        }
    }

    /**
     * Writes XML-escaped characters from the specified string to the given OutputStream.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * If the string is {@code null}, the text "null" is written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String data = "<message>Hello & goodbye</message>";
     * FileOutputStream fos = new FileOutputStream("output.xml");
     * XmlUtil.writeCharacters(data, fos);
     * // Writes: "<message>Hello &amp; goodbye</message>"
     * fos.close();
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param output The OutputStream to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(String str, final OutputStream output) throws IOException {
        str = (str == null) ? Strings.NULL : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     * Writes XML-escaped characters from a portion of a string to the given OutputStream.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * Uses a BufferedXMLWriter internally for efficient writing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String text = "Prefix <tag>content</tag> suffix";
     * FileOutputStream fos = new FileOutputStream("output.xml");
     * XmlUtil.writeCharacters(text, 7, 17, fos);
     * // Writes: "<tag>content</tag>"
     * fos.close();
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param off The start offset in the string
     * @param len The number of characters to write
     * @param output The OutputStream to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(final String str, final int off, final int len, final OutputStream output) throws IOException {
        final BufferedXMLWriter bufWriter = Objectory.createBufferedXMLWriter(output);   //NOSONAR

        try {
            bufWriter.writeCharacter(str, off, len);
            bufWriter.flush();
        } finally {
            Objectory.recycle(bufWriter);
        }
    }

    /**
     * Writes XML-escaped characters from the specified character array to the given Writer.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Test: <value> & 'more'".toCharArray();
     * StringWriter writer = new StringWriter();
     * XmlUtil.writeCharacters(chars, writer);
     * String result = writer.toString();
     * // Result: "Test: <value> &amp; &apos;more&apos;"
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param output The Writer to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(final char[] cbuf, final Writer output) throws IOException {
        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     * Writes XML-escaped characters from a portion of a character array to the given Writer.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * Uses a BufferedXMLWriter for efficient writing if the output is not already a BufferedXMLWriter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Data: <tag>value</tag> end".toCharArray();
     * StringWriter writer = new StringWriter();
     * XmlUtil.writeCharacters(chars, 6, 16, writer);
     * // Result: "<tag>value</tag>"
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param off The start offset in the character array
     * @param len The number of characters to write
     * @param output The Writer to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(final char[] cbuf, final int off, final int len, final Writer output) throws IOException {
        final boolean isBufferedWriter = output instanceof BufferedXMLWriter;
        final BufferedXMLWriter bw = isBufferedWriter ? (BufferedXMLWriter) output : Objectory.createBufferedXMLWriter(output);   //NOSONAR

        try {
            bw.writeCharacter(cbuf, off, len);
            bw.flush();
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    /**
     * Writes XML-escaped characters from the specified string to the given Writer.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * If the string is {@code null}, the text "null" is written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<book title='Java & XML'>Content</book>";
     * StringWriter writer = new StringWriter();
     * XmlUtil.writeCharacters(xml, writer);
     * String result = writer.toString();
     * // Result: "<book title=&apos;Java &amp; XML&apos;>Content</book>"
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param output The Writer to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(String str, final Writer output) throws IOException {
        str = (str == null) ? Strings.NULL : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     * Writes XML-escaped characters from a portion of a string to the given Writer.
     * Uses a BufferedXMLWriter for efficient writing if the output is not already a BufferedXMLWriter.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringWriter writer = new StringWriter();
     * XmlUtil.writeCharacters("Hello <world>", 0, 13, writer);
     * String result = writer.toString();   // "Hello <world>"
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param off The start offset in the string
     * @param len The number of characters to write
     * @param output The Writer to which the escaped characters will be written
     * @throws IOException If an I/O error occurs
     */
    public static void writeCharacters(final String str, final int off, final int len, final Writer output) throws IOException {
        final boolean isBufferedWriter = output instanceof BufferedXMLWriter;
        final BufferedXMLWriter bw = isBufferedWriter ? (BufferedXMLWriter) output : Objectory.createBufferedXMLWriter(output);   //NOSONAR

        try {
            bw.writeCharacter(str, off, len);
            bw.flush();
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    static Class<?> getAttributeTypeClass(final Node node) {
        final String typeAttr = XmlUtil.getAttribute(node, TYPE);

        if (typeAttr == null) {
            return null;
        }

        final Type<?> type = Type.of(typeAttr);

        if (type != null) {
            return type.clazz();
        }

        try {
            return ClassUtil.forClass(typeAttr);
        } catch (final RuntimeException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to load type attribute class: " + typeAttr, e);
            }

            return null;
        }
    }

    /*
     * static Class&lt;?&gt; getAttributeTypeClass(Attributes attrs) { if (attrs == null) { return null; }
     *
     * String typeAttr = attrs.getValue(TYPE);
     *
     * if (typeAttr == null) { return null; }
     *
     * Type&lt;?&gt; type = N.getType(typeAttr);
     *
     * if (type != null) { return type.getTypeClass(); }
     *
     * try { return N.forClass(typeAttr); } catch (RuntimeException e) { return null; } }
     */
    static Class<?> getConcreteClass(final Class<?> targetClass, final Class<?> typeClass) {
        if ((typeClass == null) || ((targetClass != null) && !targetClass.isAssignableFrom(typeClass))) {
            return targetClass;
        } else {
            return typeClass;
        }
    }

    static Class<?> getConcreteClass(final Class<?> targetClass, final Node node) {
        if (node == null) {
            return targetClass;
        }

        final Class<?> typeClass = getAttributeTypeClass(node);

        return getConcreteClass(targetClass, typeClass);
    }

    /*
     * static Class&lt;?&gt; getConcreteClass(Class&lt;?&gt; targetClass, Attributes attrs) { if (attrs == null) { return targetClass;
     * }
     *
     * Class&lt;?&gt; typeClass = getAttributeTypeClass(attrs);
     *
     * return getConcreteClass(targetClass, typeClass); }
     */
    static NodeType getNodeType(final String nodeName, final NodeType previousNodeType) {
        if (previousNodeType == NodeType.ENTITY) {
            return NodeType.PROPERTY;
        }

        final NodeType nodeType = nodeTypePool.get(nodeName);

        if (nodeType == null) {
            return NodeType.ENTITY;
        }

        return nodeType;
    }

    /**
     * The Enum NodeType.
     */
    enum NodeType {

        /** The bean. */
        ENTITY,
        /** The property. */
        PROPERTY,
        /** The array. */
        ARRAY,
        /** The element. */
        ELEMENT,
        /** The collection. */
        COLLECTION,
        /** The map. */
        MAP,
        /** The entry. */
        ENTRY,
        /** The key. */
        KEY,
        /** The value. */
        VALUE
    }

}
