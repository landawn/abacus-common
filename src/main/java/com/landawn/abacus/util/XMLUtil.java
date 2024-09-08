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

    private XMLUtil() {
        // singleton.
    }

    /**
     * {@link Marshaller#marshal(Object, java.io.Writer)} is called
     *
     * @param jaxbBean
     * @return
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
            throw ExceptionUtil.toRuntimeException(e);
        } catch (final IOException e) {
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
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Creates the marshaller.
     *
     * @param contextPath
     * @return
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
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Creates the marshaller.
     *
     * @param cls
     * @return
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
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Creates the unmarshaller.
     *
     * @param contextPath
     * @return
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
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Creates the unmarshaller.
     *
     * @param cls
     * @return
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
            } catch (final ParserConfigurationException e) {
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
                } catch (final ParserConfigurationException e) {
                    throw ExceptionUtil.toRuntimeException(e);
                } catch (final SAXException e) {
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
     * {@link XMLInputFactory#createXMLStreamReader(Reader)} is called.
     *
     * @param source
     * @return
     */
    public static XMLStreamReader createXMLStreamReader(final Reader source) {
        try {
            return xmlInputFactory.createXMLStreamReader(source);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLInputFactory#createXMLStreamReader(InputStream)} is called.
     *
     * @param source
     * @return
     */
    public static XMLStreamReader createXMLStreamReader(final InputStream source) {
        try {
            return xmlInputFactory.createXMLStreamReader(source);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLInputFactory#createXMLStreamReader(InputStream, String)} is called.
     *
     * @param source
     * @param encoding
     * @return
     */
    public static XMLStreamReader createXMLStreamReader(final InputStream source, final String encoding) {
        try {
            return xmlInputFactory.createXMLStreamReader(source, encoding);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Creates the filtered stream reader.
     *
     * @param source
     * @param filter
     * @return
     */
    public static XMLStreamReader createFilteredStreamReader(final XMLStreamReader source, final StreamFilter filter) {
        try {
            return xmlInputFactory.createFilteredReader(source, filter);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLOutputFactory#createXMLStreamWriter(Writer)} is called.
     *
     * @param output
     * @return
     */
    public static XMLStreamWriter createXMLStreamWriter(final Writer output) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(output);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLOutputFactory#createXMLStreamWriter(OutputStream)} is called.
     *
     * @param output
     * @return
     */
    public static XMLStreamWriter createXMLStreamWriter(final OutputStream output) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(output);
        } catch (final XMLStreamException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLOutputFactory#createXMLStreamWriter(OutputStream, String)} is called.
     *
     * @param output
     * @param encoding
     * @return
     */
    public static XMLStreamWriter createXMLStreamWriter(final OutputStream output, final String encoding) {
        try {
            return xmlOutputFactory.createXMLStreamWriter(output, encoding);
        } catch (final XMLStreamException e) {
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
        } catch (final TransformerConfigurationException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     *
     * @param source
     * @param output
     */
    public static void transform(final Document source, File output) {
        output = Configuration.formatPath(output);

        OutputStream os = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);

            transform(source, os);

            os.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param source
     * @param output
     */
    public static void transform(final Document source, final OutputStream output) {
        // Prepare the DOM document for writing
        final Source domSource = new DOMSource(source);

        final Result result = new StreamResult(output);

        try {
            createXMLTransformer().transform(domSource, result);
        } catch (final TransformerException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * {@link XMLEncoder#writeObject(Object)} is called.
     *
     * @param bean
     * @return
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
     * {@link XMLDecoder#readObject()} is called.
     *
     * @param <T>
     * @param xml
     * @return
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
     * @param node
     * @param tagName
     * @return
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
     * @param node
     * @param nodeName
     * @return
     */
    public static List<Node> getNodesByName(final Node node, final String nodeName) {
        final List<Node> nodes = new ArrayList<>();

        if (node.getNodeName().equals(nodeName)) {
            nodes.add(node);
        }

        final NodeList nodeList = node.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            final List<Node> temp = getNodesByName(nodeList.item(i), nodeName);

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
     *
     * @param node
     * @return
     */
    public static Map<String, String> readAttributes(final Node node) {
        final Map<String, String> attrs = new LinkedHashMap<>();
        final NamedNodeMap attrNodes = node.getAttributes();

        if (attrNodes == null || attrNodes.getLength() == 0) {
            return attrs;
        }

        for (int i = 0; i < attrNodes.getLength(); i++) {
            final String attrName = attrNodes.item(i).getNodeName();
            final String attrValue = attrNodes.item(i).getNodeValue();
            attrs.put(attrName, attrValue);
        }

        return attrs;
    }

    /**
     * Gets the attribute.
     *
     * @param node
     * @param attrName
     * @return {@code null} if {@code (attrsNode == null)}. (auto-generated java doc for return)
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
     *
     * @param node
     * @return boolean
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
     * Gets the text content.
     *
     * @param node
     * @return
     */
    public static String getTextContent(final Node node) {
        return node.getTextContent();
    }

    /**
     * Gets the text content.
     *
     * @param node
     * @param ignoreWhiteChar
     * @return
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
     *
     * @param cbuf
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(final char[] cbuf, final StringBuilder output) throws IOException {
        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     *
     * @param cbuf
     * @param off
     * @param len
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(final char[] cbuf, final int off, final int len, final StringBuilder output) throws IOException {
        writeCharacters(cbuf, off, len, IOUtil.stringBuilder2Writer(output));
    }

    /**
     *
     * @param str
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(String str, final StringBuilder output) throws IOException {
        str = (str == null) ? Strings.NULL_STRING : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     *
     * @param str
     * @param off
     * @param len
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(final String str, final int off, final int len, final StringBuilder output) throws IOException {
        writeCharacters(str, off, len, IOUtil.stringBuilder2Writer(output));
    }

    /**
     *
     * @param cbuf
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(final char[] cbuf, final OutputStream output) throws IOException {
        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     *
     * @param cbuf
     * @param off
     * @param len
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param str
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(String str, final OutputStream output) throws IOException {
        str = (str == null) ? Strings.NULL_STRING : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     *
     * @param str
     * @param off
     * @param len
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param cbuf
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(final char[] cbuf, final Writer output) throws IOException {
        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     *
     * @param cbuf
     * @param off
     * @param len
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param str
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeCharacters(String str, final Writer output) throws IOException {
        str = (str == null) ? Strings.NULL_STRING : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     *
     * @param str
     * @param off
     * @param len
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
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
        final String typeAttr = XMLUtil.getAttribute(node, TYPE);

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
        if (typeClass == null) {
            return targetClass;
        } else if ((targetClass == null) || targetClass.isAssignableFrom(typeClass)) {
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
