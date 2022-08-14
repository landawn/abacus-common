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
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class XMLUtil {

    protected static final Logger logger = LoggerFactory.getLogger(XMLUtil.class);

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
        } catch (Throwable e) {
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

    private XMLUtil() {
        // singleton.
    }

    /**
     * {@link Marshaller#marshal(Object, java.io.Writer)} is called
     *
     * @param jaxbEntity
     * @return
     */
    public static String marshal(Object jaxbEntity) {
        Class<?> cls = jaxbEntity.getClass();
        JAXBContext jc = classJaxbContextPool.get(cls);
        final ByteArrayOutputStream writer = Objectory.createByteArrayOutputStream();

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(cls);
                classJaxbContextPool.put(cls, jc);
            }

            Marshaller marshaller = jc.createMarshaller();
            marshaller.marshal(jaxbEntity, writer);
            writer.flush();

            return writer.toString();
        } catch (JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     * {@link Unmarshaller#unmarshal(Object, java.io.Writer)} is called
     *
     * @param <T>
     * @param cls
     * @param xml
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(Class<? extends T> cls, String xml) {
        JAXBContext jc = classJaxbContextPool.get(cls);

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(cls);
                classJaxbContextPool.put(cls, jc);
            }

            Unmarshaller nnmarshaller = jc.createUnmarshaller();
            StringReader reader = new StringReader(xml);

            return (T) nnmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Creates the marshaller.
     *
     * @param contextPath
     * @return
     */
    public static Marshaller createMarshaller(String contextPath) {
        JAXBContext jc = pathJaxbContextPool.get(contextPath);

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(contextPath);
                pathJaxbContextPool.put(contextPath, jc);
            }

            return jc.createMarshaller();
        } catch (JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Creates the marshaller.
     *
     * @param cls
     * @return
     */
    public static Marshaller createMarshaller(Class<?> cls) {
        JAXBContext jc = classJaxbContextPool.get(cls);

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(cls);
                classJaxbContextPool.put(cls, jc);
            }

            return jc.createMarshaller();
        } catch (JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Creates the unmarshaller.
     *
     * @param contextPath
     * @return
     */
    public static Unmarshaller createUnmarshaller(String contextPath) {
        JAXBContext jc = pathJaxbContextPool.get(contextPath);

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(contextPath);
                pathJaxbContextPool.put(contextPath, jc);
            }

            return jc.createUnmarshaller();
        } catch (JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Creates the unmarshaller.
     *
     * @param cls
     * @return
     */
    public static Unmarshaller createUnmarshaller(Class<?> cls) {
        JAXBContext jc = classJaxbContextPool.get(cls);

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(cls);
                classJaxbContextPool.put(cls, jc);
            }

            return jc.createUnmarshaller();
        } catch (JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link DocumentBuilderFactory#newDocumentBuilder()} is called. client should Cache the result for performance
     * improvement if this method is called frequently.
     *
     * @return
     */
    public static DocumentBuilder createDOMParser() {
        synchronized (docBuilderFactory) {
            try {
                return docBuilderFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw ExceptionUtil.toRuntimeException(e);
            }
        }
    }

    /**
     * Creates the DOM parser.
     *
     * @param ignoreComments
     * @param ignoringElementContentWhitespace
     * @return
     */
    public static DocumentBuilder createDOMParser(boolean ignoreComments, boolean ignoringElementContentWhitespace) {
        DocumentBuilder documentBuilder = null;

        synchronized (docBuilderFactory) {
            try {
                boolean orgIgnoreComments = docBuilderFactory.isIgnoringComments();
                boolean orgIgnoringElementContentWhitespace = docBuilderFactory.isIgnoringElementContentWhitespace();

                docBuilderFactory.setIgnoringComments(ignoreComments);
                docBuilderFactory.setIgnoringElementContentWhitespace(ignoringElementContentWhitespace);

                documentBuilder = docBuilderFactory.newDocumentBuilder();

                docBuilderFactory.setIgnoringComments(orgIgnoreComments);
                docBuilderFactory.setIgnoringElementContentWhitespace(orgIgnoringElementContentWhitespace);
            } catch (ParserConfigurationException e) {
                throw ExceptionUtil.toRuntimeException(e);
            }
        }

        return documentBuilder;
    }

    /**
     * Call <code>recycleContentParser</code> to reuse the instance.
     *
     * @return
     */
    public static DocumentBuilder createContentParser() {
        synchronized (contentDocBuilderPool) {
            DocumentBuilder documentBuilder = contentDocBuilderPool.poll();

            try {
                if (documentBuilder == null) {
                    boolean orgIgnoreComments = docBuilderFactory.isIgnoringComments();
                    boolean orgIgnoringElementContentWhitespace = docBuilderFactory.isIgnoringElementContentWhitespace();

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
            } catch (ParserConfigurationException e) {
                throw ExceptionUtil.toRuntimeException(e);
            }

            return documentBuilder;
        }
    }

    /**
     * Recycle content parser.
     *
     * @param docBuilder
     */
    public static void recycleContentParser(DocumentBuilder docBuilder) {
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
     * Call <code>recycleSAXParser</code> to reuse the instance.
     *
     * @return
     */
    public static SAXParser createSAXParser() {
        synchronized (saxParserPool) {
            SAXParser saxParser = saxParserPool.poll();

            if (saxParser == null) {
                try {
                    saxParser = saxParserFactory.newSAXParser();
                } catch (ParserConfigurationException e) {
                    throw ExceptionUtil.toRuntimeException(e);
                } catch (SAXException e) {
                    throw new ParseException(e);
                }
            }

            return saxParser;
        }
    }

    /**
     * Recycle SAX parser.
     *
     * @param saxParser
     */
    public static void recycleSAXParser(SAXParser saxParser) {
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
     * {@link XMLInputFactory#createXMLStreamReader(Reader)} is called.
     *
     * @param reader
     * @return
     */
    public static XMLStreamReader createXMLStreamReader(Reader reader) {
        try {
            return xmlInputFactory.createXMLStreamReader(reader);
        } catch (XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLInputFactory#createXMLStreamReader(InputStream)} is called.
     *
     * @param stream
     * @return
     */
    public static XMLStreamReader createXMLStreamReader(InputStream stream) {
        try {
            return xmlInputFactory.createXMLStreamReader(stream);
        } catch (XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLInputFactory#createXMLStreamReader(InputStream, String)} is called.
     *
     * @param stream
     * @param encoding
     * @return
     */
    public static XMLStreamReader createXMLStreamReader(InputStream stream, String encoding) {
        try {
            return xmlInputFactory.createXMLStreamReader(stream, encoding);
        } catch (XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Creates the filtered stream reader.
     *
     * @param reader
     * @param filter
     * @return
     */
    public static XMLStreamReader createFilteredStreamReader(XMLStreamReader reader, StreamFilter filter) {
        try {
            return xmlInputFactory.createFilteredReader(reader, filter);
        } catch (XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLOutputFactory#createXMLStreamWriter(Writer)} is called.
     *
     * @param writer
     * @return
     */
    public static XMLStreamWriter createXMLStreamWriter(Writer writer) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(writer);
        } catch (XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLOutputFactory#createXMLStreamWriter(OutputStream)} is called.
     *
     * @param stream
     * @return
     */
    public static XMLStreamWriter createXMLStreamWriter(OutputStream stream) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(stream);
        } catch (XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLOutputFactory#createXMLStreamWriter(OutputStream, String)} is called.
     *
     * @param stream
     * @param encoding
     * @return
     */
    public static XMLStreamWriter createXMLStreamWriter(OutputStream stream, String encoding) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(stream, encoding);
        } catch (XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link TransformerFactory#newTransformer()} is called.
     *
     * @return
     */
    public static Transformer createXMLTransformer() {
        try {
            return transferFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     *
     * @param doc
     * @param xmlFile
     */
    public static void transform(Document doc, File xmlFile) {
        xmlFile = Configuration.formatPath(xmlFile);

        OutputStream os = null;

        try {
            IOUtil.createNewFileIfNotExists(xmlFile);

            os = IOUtil.newFileOutputStream(xmlFile);

            transform(doc, os);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param doc
     * @param ou
     */
    public static void transform(Document doc, OutputStream ou) {
        // Prepare the DOM document for writing
        Source source = new DOMSource(doc);

        Result result = new StreamResult(ou);

        try {
            createXMLTransformer().transform(source, result);
        } catch (TransformerException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLEncoder#writeObject(Object)} is called.
     *
     * @param entity
     * @return
     */
    public static String xmlEncode(Object entity) {
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

        try (XMLEncoder xmlEncoder = new XMLEncoder(os)) {
            xmlEncoder.writeObject(entity);
            xmlEncoder.flush();
        }

        String result = os.toString();
        Objectory.recycle(os);

        return result;
    }

    /**
     * {@link XMLDecoder#readObject()} is called.
     *
     * @param <T>
     * @param xml
     * @return
     */
    public static <T> T xmlDecode(String xml) {
        InputStream is = new ByteArrayInputStream(xml.getBytes());
        try (XMLDecoder xmlDecoder = new XMLDecoder(is)) {
            return (T) xmlDecoder.readObject();
        }
    }

    /**
     * Gets the elements by tag name.
     *
     * @param node
     * @param tagName
     * @return
     */
    public static List<Element> getElementsByTagName(Element node, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList nodeList = node.getElementsByTagName(tagName);

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
     * @param node
     * @param nodeName
     * @return
     */
    public static List<Node> getNodesByName(Node node, String nodeName) {
        List<Node> nodes = new ArrayList<>();

        if (node.getNodeName().equals(nodeName)) {
            nodes.add(node);
        }

        NodeList nodeList = node.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            List<Node> temp = getNodesByName(nodeList.item(i), nodeName);

            if (temp.size() > 0) {
                nodes.addAll(temp);
            }
        }

        return nodes;
    }

    /**
     * Gets the next node by name.
     *
     * @param node
     * @param nodeName
     * @return
     */
    public static Node getNextNodeByName(Node node, String nodeName) {
        if (node.getNodeName().equals(nodeName)) {
            return node;
        } else {
            NodeList nodeList = node.getChildNodes();

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
     *
     * @param node
     * @return
     */
    public static Map<String, String> readAttributes(Node node) {
        final Map<String, String> attrs = new LinkedHashMap<>();
        final NamedNodeMap attrNodes = node.getAttributes();

        if (attrNodes == null || attrNodes.getLength() == 0) {
            return attrs;
        }

        for (int i = 0; i < attrNodes.getLength(); i++) {
            String attrName = attrNodes.item(i).getNodeName();
            String attrValue = attrNodes.item(i).getNodeValue();
            attrs.put(attrName, attrValue);
        }

        return attrs;
    }

    /**
     * Gets the attribute.
     *
     * @param node
     * @param attrName
     * @return
     */
    public static String getAttribute(Node node, String attrName) {
        NamedNodeMap attrsNode = node.getAttributes();

        if (attrsNode == null) {
            return null;
        }

        Node attrNode = attrsNode.getNamedItem(attrName);

        return (attrNode == null) ? null : attrNode.getNodeValue();
    }

    /**
     *
     * @param node
     * @return boolean
     */
    public static boolean isTextElement(Node node) {
        NodeList childNodeList = node.getChildNodes();

        for (int i = 0; i < childNodeList.getLength(); i++) {
            if (childNodeList.item(i).getNodeType() == Document.ELEMENT_NODE) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the text content.
     *
     * @param node
     * @return
     */
    public static String getTextContent(Node node) {
        return node.getTextContent();
    }

    /**
     * Gets the text content.
     *
     * @param node
     * @param ignoreWhiteChar
     * @return
     */
    public static String getTextContent(Node node, boolean ignoreWhiteChar) {
        String textContent = node.getTextContent();

        if (ignoreWhiteChar && N.notNullOrEmpty(textContent)) {
            final StringBuilder sb = Objectory.createStringBuilder();

            for (char c : textContent.toCharArray()) {
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

            int length = sb.length();

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
     *
     * @param sb
     * @param cbuf
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(StringBuilder sb, char[] cbuf) throws IOException {
        writeCharacters(sb, cbuf, 0, cbuf.length);
    }

    /**
     *
     * @param sb
     * @param cbuf
     * @param off
     * @param len
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(StringBuilder sb, char[] cbuf, int off, int len) throws IOException {
        writeCharacters(IOUtil.stringBuilder2Writer(sb), cbuf, off, len);
    }

    /**
     *
     * @param sb
     * @param str
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(StringBuilder sb, String str) throws IOException {
        str = (str == null) ? Strings.NULL_STRING : str;
        writeCharacters(sb, str, 0, str.length());
    }

    /**
     *
     * @param sb
     * @param str
     * @param off
     * @param len
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(StringBuilder sb, String str, int off, int len) throws IOException {
        writeCharacters(IOUtil.stringBuilder2Writer(sb), str, off, len);
    }

    /**
     *
     * @param os
     * @param cbuf
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(OutputStream os, char[] cbuf) throws IOException {
        writeCharacters(os, cbuf, 0, cbuf.length);
    }

    /**
     *
     * @param os
     * @param cbuf
     * @param off
     * @param len
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(OutputStream os, char[] cbuf, int off, int len) throws IOException {
        final BufferedXMLWriter bufWriter = Objectory.createBufferedXMLWriter(os);
        bufWriter.writeCharacter(cbuf, off, len);
        bufWriter.flush();
        Objectory.recycle(bufWriter);
    }

    /**
     *
     * @param os
     * @param str
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(OutputStream os, String str) throws IOException {
        str = (str == null) ? Strings.NULL_STRING : str;
        writeCharacters(os, str, 0, str.length());
    }

    /**
     *
     * @param os
     * @param str
     * @param off
     * @param len
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(OutputStream os, String str, int off, int len) throws IOException {
        final BufferedXMLWriter bufWriter = Objectory.createBufferedXMLWriter(os);
        bufWriter.writeCharacter(str, off, len);
        bufWriter.flush();
        Objectory.recycle(bufWriter);
    }

    /**
     *
     * @param writer
     * @param cbuf
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(Writer writer, char[] cbuf) throws IOException {
        writeCharacters(writer, cbuf, 0, cbuf.length);
    }

    /**
     *
     * @param writer
     * @param cbuf
     * @param off
     * @param len
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(Writer writer, char[] cbuf, int off, int len) throws IOException {
        boolean isBufferedWriter = writer instanceof BufferedXMLWriter;
        final BufferedXMLWriter bw = isBufferedWriter ? (BufferedXMLWriter) writer : Objectory.createBufferedXMLWriter(writer);

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
     *
     * @param writer
     * @param str
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(Writer writer, String str) throws IOException {
        str = (str == null) ? Strings.NULL_STRING : str;
        writeCharacters(writer, str, 0, str.length());
    }

    /**
     *
     * @param writer
     * @param str
     * @param off
     * @param len
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(Writer writer, String str, int off, int len) throws IOException {
        boolean isBufferedWriter = writer instanceof BufferedXMLWriter;
        final BufferedXMLWriter bw = isBufferedWriter ? (BufferedXMLWriter) writer : Objectory.createBufferedXMLWriter(writer);

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
    static Class<?> getAttributeTypeClass(Node node) {
        String typeAttr = XMLUtil.getAttribute(node, TYPE);

        if (typeAttr == null) {
            return null;
        }

        Type<?> type = N.typeOf(typeAttr);

        if (type != null) {
            return type.clazz();
        }

        try {
            return ClassUtil.forClass(typeAttr);
        } catch (RuntimeException e) {
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
    static Class<?> getConcreteClass(Class<?> targetClass, Class<?> typeClass) {
        if (typeClass == null) {
            return targetClass;
        } else if (targetClass == null) {
            return typeClass;
        } else if (targetClass.isAssignableFrom(typeClass)) {
            return typeClass;
        } else {
            return targetClass;
        }
    }

    /**
     * Gets the concrete class.
     *
     * @param targetClass
     * @param node
     * @return
     */
    static Class<?> getConcreteClass(Class<?> targetClass, Node node) {
        if (node == null) {
            return targetClass;
        }

        Class<?> typeClass = getAttributeTypeClass(node);

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
    static NodeType getNodeType(String nodeName, NodeType previousNodeType) {
        if (previousNodeType == NodeType.ENTITY) {
            return NodeType.PROPERTY;
        }

        NodeType nodeType = nodeTypePool.get(nodeName);

        if (nodeType == null) {
            return NodeType.ENTITY;
        }

        return nodeType;
    }

    /**
     * The Enum NodeType.
     */
    enum NodeType {

        /** The entity. */
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
