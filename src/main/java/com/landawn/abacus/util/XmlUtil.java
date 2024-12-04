/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

public final class XmlUtil {

    protected static final Logger logger = LoggerFactory.getLogger(XmlUtil.class);

    // ... it has to be big enougth to make it's safety to add element to
    // ArrayBlockingQueue.
    static final String NAME = "name";

    static final String TYPE = "type";

    private static final int POOL_SIZE = 1000;

    // ...
    private static final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

    private static final Queue<SAXParser> saxParserPool = new ArrayBlockingQueue<>(POOL_SIZE);

    // ...
    private static final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

    private static final Queue<DocumentBuilder> contentDocBuilderPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    static {
        try {
            if (!Class.forName("com.ctc.wstx.stax.WstxInputFactory").isAssignableFrom(xmlInputFactory.getClass()) && logger.isWarnEnabled()) {
                logger.warn("It's recommended to use woodstox: https://github.com/FasterXML/woodstox");
            }
        } catch (final Throwable e) {
            if (logger.isWarnEnabled()) {
                logger.warn("It's recommended to use woodstox: https://github.com/FasterXML/woodstox");
            }
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
     * {@link Marshaller#marshal(Object, java.io.Writer)} is called.
     *
     * @param jaxbBean The JAXB bean to be marshaled.
     * @return The XML string representation of the JAXB bean.
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
            throw N.toRuntimeException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     * Unmarshals the given XML string into an object of the specified class.
     * {@link Unmarshaller#unmarshal(Object, java.io.Writer)} is called.
     *
     * @param <T> The type of the object to be returned.
     * @param cls The class of the object to be returned.
     * @param xml The XML string to be unmarshaled.
     * @return The unmarshaled object of the specified class.
     * @see JAXBContext#newInstance(Class...)
     * @see Unmarshaller#unmarshal(Object, java.io.Reader)
     */
    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(final Class<? extends T> cls, final String xml) {
        JAXBContext jc = classJaxbContextPool.get(cls);

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(cls);
                classJaxbContextPool.put(cls, jc);
            }

            final Unmarshaller nnmarshaller = jc.createUnmarshaller();
            final StringReader reader = new StringReader(xml);

            return (T) nnmarshaller.unmarshal(reader);
        } catch (final JAXBException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates a JAXB Marshaller for the given context path.
     *
     * @param contextPath The context path for which to create the Marshaller.
     * @return The created Marshaller.
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
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates a JAXB Marshaller for the given class.
     *
     * @param cls The class for which to create the Marshaller.
     * @return The created Marshaller.
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
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates a JAXB Unmarshaller for the given context path.
     *
     * @param contextPath The context path for which to create the Unmarshaller.
     * @return The created Unmarshaller.
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
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates a JAXB Unmarshaller for the given class.
     *
     * @param cls The class for which to create the Unmarshaller.
     * @return The created Unmarshaller.
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
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates a new instance of {@code DocumentBuilder}.
     *
     * @param source The source XMLStreamReader to be filtered.
     * @param filter The StreamFilter to apply to the source.
     * @return The filtered XMLStreamReader.
     * @see DocumentBuilderFactory#newDocumentBuilder()
     */
    public static DocumentBuilder createDOMParser() {
        synchronized (docBuilderFactory) {
            try {
                return docBuilderFactory.newDocumentBuilder();
            } catch (final ParserConfigurationException e) {
                throw N.toRuntimeException(e);
            }
        }
    }

    /**
     * Creates a new instance of {@code DocumentBuilder} with the specified configuration.
     *
     * @param ignoreComments Whether to ignore comments in the XML.
     * @param ignoringElementContentWhitespace Whether to ignore whitespace in element content.
     * @return A new instance of {@code DocumentBuilder} with the specified configuration.
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
                throw N.toRuntimeException(e);
            }
        }

        return documentBuilder;
    }

    /**
     * Creates a new instance of {@code DocumentBuilder} for parsing content.
     * Call {@code recycleContentParser} to reuse the instance.
     *
     * @return A new instance of {@code DocumentBuilder}.
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
                throw N.toRuntimeException(e);
            }

            return documentBuilder;
        }
    }

    /**
     * Recycles the given DocumentBuilder instance by resetting it and adding it back to the pool.
     *
     * @param docBuilder The DocumentBuilder instance to be recycled.
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
     * Creates a new instance of {@code SAXParser}.
     * Call {@code recycleSAXParser} to reuse the instance.
     *
     * @return A new instance of {@code SAXParser}.
     * @see SAXParserFactory#newSAXParser()
     */
    public static SAXParser createSAXParser() {
        synchronized (saxParserPool) {
            SAXParser saxParser = saxParserPool.poll();

            if (saxParser == null) {
                try {
                    saxParser = saxParserFactory.newSAXParser();
                } catch (final ParserConfigurationException e) {
                    throw N.toRuntimeException(e);
                } catch (final SAXException e) {
                    throw new ParseException(e);
                }
            }

            return saxParser;
        }
    }

    /**
     * Recycles the given SAXParser instance by resetting it and adding it back to the pool.
     *
     * @param saxParser The SAXParser instance to be recycled.
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
     *
     * @param source The Reader source from which to create the XMLStreamReader.
     * @return The created XMLStreamReader.
     * @throws RuntimeException if an XMLStreamException occurs.
     * @see XMLInputFactory#createXMLStreamReader(Reader)
     */
    public static XMLStreamReader createXMLStreamReader(final Reader source) {
        try {
            return xmlInputFactory.createXMLStreamReader(source);
        } catch (final XMLStreamException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates an XMLStreamReader from the given InputStream source.
     *
     * @param source The InputStream source from which to create the XMLStreamReader.
     * @return The created XMLStreamReader.
     * @throws RuntimeException if an XMLStreamException occurs.
     * @see XMLInputFactory#createXMLStreamReader(InputStream)
     */
    public static XMLStreamReader createXMLStreamReader(final InputStream source) {
        try {
            return xmlInputFactory.createXMLStreamReader(source);
        } catch (final XMLStreamException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates an XMLStreamReader from the given InputStream source with the specified encoding.
     *
     * @param source The InputStream source from which to create the XMLStreamReader.
     * @param encoding The character encoding to be used.
     * @return The created XMLStreamReader.
     * @throws RuntimeException if an XMLStreamException occurs.
     * @see XMLInputFactory#createXMLStreamReader(InputStream, String)
     */
    public static XMLStreamReader createXMLStreamReader(final InputStream source, final String encoding) {
        try {
            return xmlInputFactory.createXMLStreamReader(source, encoding);
        } catch (final XMLStreamException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates a filtered XMLStreamReader from the given source XMLStreamReader and StreamFilter.
     *
     * @param source The source XMLStreamReader to be filtered.
     * @param filter The StreamFilter to apply to the source.
     * @return The filtered XMLStreamReader.
     * @throws RuntimeException if an XMLStreamException occurs.
     * @see XMLInputFactory#createFilteredReader(XMLStreamReader, StreamFilter)
     */
    public static XMLStreamReader createFilteredStreamReader(final XMLStreamReader source, final StreamFilter filter) {
        try {
            return xmlInputFactory.createFilteredReader(source, filter);
        } catch (final XMLStreamException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates an XMLStreamWriter from the given Writer output.
     *
     * @param output The Writer output to which the XMLStreamWriter will write.
     * @return The created XMLStreamWriter.
     * @throws RuntimeException if an XMLStreamException occurs.
     * @see XMLOutputFactory#createXMLStreamWriter(Writer)
     */
    public static XMLStreamWriter createXMLStreamWriter(final Writer output) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(output);
        } catch (final XMLStreamException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates an XMLStreamWriter from the given OutputStream.
     *
     * @param output The OutputStream to which the XMLStreamWriter will write.
     * @return The created XMLStreamWriter.
     * @throws RuntimeException if an XMLStreamException occurs.
     * @see XMLOutputFactory#createXMLStreamWriter(OutputStream)
     */
    public static XMLStreamWriter createXMLStreamWriter(final OutputStream output) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(output);
        } catch (final XMLStreamException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates an XMLStreamWriter from the given OutputStream with the specified encoding.
     *
     * @param output The OutputStream to which the XMLStreamWriter will write.
     * @param encoding The character encoding to be used.
     * @return The created XMLStreamWriter.
     * @throws RuntimeException if an XMLStreamException occurs.
     * @see XMLOutputFactory#createXMLStreamWriter(OutputStream, String)
     */
    public static XMLStreamWriter createXMLStreamWriter(final OutputStream output, final String encoding) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(output, encoding);
        } catch (final XMLStreamException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates a new instance of Transformer.
     *
     * @return A new instance of Transformer.
     * @throws RuntimeException if a TransformerConfigurationException occurs.
     * @see TransformerFactory#newTransformer()
     */
    public static Transformer createXMLTransformer() {
        try {
            return transferFactory.newTransformer();
        } catch (final TransformerConfigurationException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Transforms the given XML Document to the specified output file.
     *
     * @param source The XML Document to be transformed.
     * @param output The output file where the transformed XML will be written.
     * @throws UncheckedIOException if an IOException occurs during file operations.
     * @see Transformer#transform(Source, Result)
     */
    public static void transform(final Document source, File output) {
        output = Configuration.formatPath(output);

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
     *
     * @param source The XML Document to be transformed.
     * @param output The OutputStream where the transformed XML will be written.
     * @throws RuntimeException if a TransformerException occurs.
     * @see Transformer#transform(Source, Result)
     */
    public static void transform(final Document source, final OutputStream output) {
        // Prepare the DOM document for writing
        final Source domSource = new DOMSource(source);

        final Result result = new StreamResult(output);

        try {
            createXMLTransformer().transform(domSource, result);
        } catch (final TransformerException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Transforms the given XML Document to the specified Writer.
     *
     * @param source The XML Document to be transformed.
     * @param output The Writer where the transformed XML will be written.
     * @throws RuntimeException if a TransformerException occurs.
     * @see Transformer#transform(Source, Result)
     */
    public static void transform(final Document source, final Writer output) {
        // Prepare the DOM document for writing
        final Source domSource = new DOMSource(source);

        final Result result = new StreamResult(output);

        try {
            createXMLTransformer().transform(domSource, result);
        } catch (final TransformerException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Encodes the given bean object into an XML string.
     *
     * @param bean The object to be encoded into XML.
     * @return The XML string representation of the given object.
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
     * Decodes the given XML string into an object.
     *
     * @param <T> The type of the object to be returned.
     * @param xml The XML string to be decoded.
     * @return The decoded object.
     * @see XMLDecoder#readObject()
     */
    public static <T> T xmlDecode(final String xml) {
        final InputStream is = new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8));
        try (XMLDecoder xmlDecoder = new XMLDecoder(is)) {
            return (T) xmlDecoder.readObject();
        }
    }

    /**
     * Gets the elements by tag name.
     *
     * @param node The parent element to search within.
     * @param tagName The tag name of the elements to find.
     * @return A list of elements with the specified tag name that are direct children of the given node.
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
     * Gets the nodes by name.
     *
     * @param node The parent node to search within.
     * @param nodeName The name of the nodes to find.
     * @return A list of nodes with the specified name.
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
     * Gets the next node by name.
     *
     * @param node The parent node to search within.
     * @param nodeName The name of the node to find.
     * @return The next node with the specified name, or {@code null} if no such node is found.
     */
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
     * @param node The XML node from which to get the attribute.
     * @param attrName The name of the attribute to retrieve.
     * @return The value of the specified attribute, or {@code null} if the attribute does not exist.
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
     * Reads the attributes of the given XML node and returns them as a map.
     *
     * @param node The XML node from which to read the attributes.
     * @return A map containing the attributes of the given node, where the keys are attribute names and the values are attribute values.
     */
    public static Map<String, String> readAttributes(final Node node) {
        return readAttributes(Strings.EMPTY_STRING, node, new LinkedHashMap<>());
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
     *
     * @param element The XML element to be read.
     * @return A map containing the attributes and text content of the given element, where the keys are attribute names and the values are attribute values.
     */
    public static Map<String, String> readElement(final Element element) {
        return readElement(Strings.EMPTY_STRING, element, new LinkedHashMap<>());
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

            if (childNode instanceof final Element childElement) {
                readElement(nextParentNodeName, childElement, output);
            }
        }

        return output;
    }

    /**
     * Checks if the given node is a text element.
     * A text element is defined as an element that does not contain any child elements.
     *
     * @param node The node to be checked.
     * @return {@code true} if the node is a text element, {@code false} otherwise.
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
     *
     * @param node The XML node from which to get the text content.
     * @return The text content of the specified node, with leading and trailing whitespace removed.
     */
    public static String getTextContent(final Node node) {
        return node.getTextContent();
    }

    /**
     * Gets the text content of the given XML node, optionally ignoring whitespace characters.
     *
     * @param node The XML node from which to get the text content.
     * @param ignoreWhiteChar Whether to ignore whitespace characters in the text content. if {@code true}, all the whitespace within text content will be removed.
     * @return The text content of the specified node, with all the whitespace within text content will be removed if {@code ignoreWhiteChar} is {@code true}.
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

                        if ((sb.length() > 0) && (sb.charAt(sb.length() - 1) != ' ')) {
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
     * Writes the characters from the specified character array to the given StringBuilder.
     *
     * @param cbuf The character array containing the characters to be written.
     * @param output The StringBuilder to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(final char[] cbuf, final StringBuilder output) throws IOException {
        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     * Writes a portion of a character array to the given StringBuilder.
     *
     * @param cbuf The character array containing the characters to be written.
     * @param off The start offset in the character array.
     * @param len The number of characters to write.
     * @param output The StringBuilder to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(final char[] cbuf, final int off, final int len, final StringBuilder output) throws IOException {
        writeCharacters(cbuf, off, len, IOUtil.stringBuilder2Writer(output));
    }

    /**
     * Writes the characters from the specified string to the given StringBuilder.
     *
     * @param str The string containing the characters to be written.
     * @param output The StringBuilder to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(String str, final StringBuilder output) throws IOException {
        str = (str == null) ? Strings.NULL_STRING : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     * Writes a portion of a string to the given StringBuilder.
     *
     * @param str The string containing the characters to be written.
     * @param off The start offset in the string.
     * @param len The number of characters to write.
     * @param output The StringBuilder to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(final String str, final int off, final int len, final StringBuilder output) throws IOException {
        writeCharacters(str, off, len, IOUtil.stringBuilder2Writer(output));
    }

    /**
     * Writes the characters from the specified character array to the given OutputStream.
     *
     * @param cbuf The character array containing the characters to be written.
     * @param output The OutputStream to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(final char[] cbuf, final OutputStream output) throws IOException {
        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     * Writes a portion of a character array to the given OutputStream.
     *
     * @param cbuf The character array containing the characters to be written.
     * @param off The start offset in the character array.
     * @param len The number of characters to write.
     * @param output The OutputStream to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(final char[] cbuf, final int off, final int len, final OutputStream output) throws IOException {
        final BufferedXMLWriter bufWriter = Objectory.createBufferedXMLWriter(output); //NOSONAR

        try {
            bufWriter.writeCharacter(cbuf, off, len);
            bufWriter.flush();
        } finally {
            Objectory.recycle(bufWriter);
        }
    }

    /**
     * Writes the characters from the specified string to the given OutputStream.
     *
     * @param str The string containing the characters to be written.
     * @param output The OutputStream to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(String str, final OutputStream output) throws IOException {
        str = (str == null) ? Strings.NULL_STRING : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     * Writes a portion of a string to the given OutputStream.
     *
     * @param str The string containing the characters to be written.
     * @param off The start offset in the string.
     * @param len The number of characters to write.
     * @param output The OutputStream to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(final String str, final int off, final int len, final OutputStream output) throws IOException {
        final BufferedXMLWriter bufWriter = Objectory.createBufferedXMLWriter(output); //NOSONAR

        try {
            bufWriter.writeCharacter(str, off, len);
            bufWriter.flush();
        } finally {
            Objectory.recycle(bufWriter);
        }
    }

    /**
     * Writes the characters from the specified character array to the given Writer.
     *
     * @param cbuf The character array containing the characters to be written.
     * @param output The Writer to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(final char[] cbuf, final Writer output) throws IOException {
        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     * Writes a portion of a character array to the given Writer.
     *
     * @param cbuf The character array containing the characters to be written.
     * @param off The start offset in the character array.
     * @param len The number of characters to write.
     * @param output The Writer to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(final char[] cbuf, final int off, final int len, final Writer output) throws IOException {
        final boolean isBufferedWriter = output instanceof BufferedXMLWriter;
        final BufferedXMLWriter bw = isBufferedWriter ? (BufferedXMLWriter) output : Objectory.createBufferedXMLWriter(output); //NOSONAR

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
     * Writes the characters from the specified string to the given Writer.
     *
     * @param str The string containing the characters to be written.
     * @param output The Writer to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(String str, final Writer output) throws IOException {
        str = (str == null) ? Strings.NULL_STRING : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     * Writes a portion of a string to the given Writer.
     *
     * @param str The string containing the characters to be written.
     * @param off The start offset in the string.
     * @param len The number of characters to write.
     * @param output The Writer to which the characters will be written.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeCharacters(final String str, final int off, final int len, final Writer output) throws IOException {
        final boolean isBufferedWriter = output instanceof BufferedXMLWriter;
        final BufferedXMLWriter bw = isBufferedWriter ? (BufferedXMLWriter) output : Objectory.createBufferedXMLWriter(output); //NOSONAR

        try {
            bw.writeCharacter(str, off, len);
            bw.flush();
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    /**
     * Gets the attribute type class.
     *
     * @param node
     * @return
     */
    static Class<?> getAttributeTypeClass(final Node node) {
        final String typeAttr = XmlUtil.getAttribute(node, TYPE);

        if (typeAttr == null) {
            return null;
        }

        final Type<?> type = N.typeOf(typeAttr);

        if (type != null) {
            return type.clazz();
        }

        try {
            return ClassUtil.forClass(typeAttr);
        } catch (final RuntimeException e) {
            return null;
        }
    }

    /**
     * Gets the concrete class.
     *
     * @param targetClass
     * @param typeClass
     * @return
     */
    /*
     * static Class<?> getAttributeTypeClass(Attributes atts) { if (atts == null) { return null; }
     *
     * String typeAttr = atts.getValue(TYPE);
     *
     * if (typeAttr == null) { return null; }
     *
     * Type<?> type = N.getType(typeAttr);
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

    /**
     * Gets the concrete class.
     *
     * @param targetClass
     * @param node
     * @return
     */
    static Class<?> getConcreteClass(final Class<?> targetClass, final Node node) {
        if (node == null) {
            return targetClass;
        }

        final Class<?> typeClass = getAttributeTypeClass(node);

        return getConcreteClass(targetClass, typeClass);
    }

    /**
     * Gets the node type.
     *
     * @param nodeName
     * @param previousNodeType
     * @return
     */
    /*
     * static Class<?> getConcreteClass(Class<?> targetClass, Attributes atts) { if (atts == null) { return targetClass;
     * }
     *
     * Class<?> typeClass = getAttributeTypeClass(atts);
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
        VALUE;
    }
}
