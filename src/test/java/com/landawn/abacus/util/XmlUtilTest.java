package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.transform.Transformer;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedException;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;

public class XmlUtilTest extends TestBase {

    @XmlRootElement(name = "person")
    public static class Person {
        private String name;
        private int age;

        public Person() {
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Person person = (Person) obj;
            return age == person.age && CommonUtil.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    private static Element parse(String xml) throws Exception {
        return XmlUtil.createDOMParser().parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8))).getDocumentElement();
    }

    private static XMLStreamReader trackingReader(String xml, AtomicBoolean closed) {
        return new StreamReaderDelegate(XmlUtil.createXMLStreamReader(new StringReader(xml))) {
            @Override
            public void close() throws XMLStreamException {
                closed.set(true);
                super.close();
            }
        };
    }

    @Test
    public void testMarshalUnmarshal() {
        Person original = new Person("John", 30);
        String xml = XmlUtil.marshal(original);
        assertNotNull(xml);
        assertTrue(xml.contains("<person>") && xml.contains("<name>John</name>") && xml.contains("<age>30</age>") && xml.contains("</person>"));
        assertEquals(original, XmlUtil.unmarshal(Person.class, xml));

        Person alice = new Person("Alice", 25);
        assertEquals(alice, XmlUtil.unmarshal(Person.class, XmlUtil.marshal(alice)));

        Person nonAscii = new Person("Café 中文", 42);
        String nonAsciiXml = XmlUtil.marshal(nonAscii);
        assertTrue(nonAsciiXml.contains("Café 中文"));
        assertEquals(nonAscii, XmlUtil.unmarshal(Person.class, nonAsciiXml));

        Person fromLiteral = XmlUtil.unmarshal(Person.class,
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><age>30</age><name>John</name></person>");
        assertEquals("John", fromLiteral.getName());
        assertEquals(30, fromLiteral.getAge());
    }

    @Test
    public void testUnmarshal_EdgeCase() throws Exception {
        File externalEntityFile = File.createTempFile("xmlutil-unmarshal-xxe-", ".txt");
        try {
            Files.write(externalEntityFile.toPath(), "XXE_PUBLIC_UNMARSHAL_MARKER_2026".getBytes(Charsets.UTF_8));
            String maliciousXml = "<?xml version=\"1.0\"?><!DOCTYPE person [<!ENTITY xxe SYSTEM \"" + externalEntityFile.toURI()
                    + "\">]><person><name>&xxe;</name><age>25</age></person>";
            assertThrows(RuntimeException.class, () -> XmlUtil.unmarshal(Person.class, maliciousXml));
        } finally {
            externalEntityFile.delete();
        }

        AtomicBoolean closedOnFailure = new AtomicBoolean();
        assertThrows(RuntimeException.class, () -> XmlUtil.unmarshalAndClose(Person.class, trackingReader("<person><name>Alice</person>", closedOnFailure)));
        assertTrue(closedOnFailure.get());

        AtomicBoolean closedOnSuccess = new AtomicBoolean();
        assertEquals(new Person("Alice", 25),
                XmlUtil.unmarshalAndClose(Person.class, trackingReader("<person><name>Alice</name><age>25</age></person>", closedOnSuccess)));
        assertTrue(closedOnSuccess.get());
    }

    @Test
    public void testCreateMarshallerAndUnmarshaller() {
        Marshaller m1 = XmlUtil.createMarshaller(Person.class);
        Marshaller m2 = XmlUtil.createMarshaller(Person.class);
        assertNotNull(m1);
        assertNotNull(m2);
        assertThrows(UncheckedException.class, () -> XmlUtil.createMarshaller("com.landawn.abacus.util"));

        Unmarshaller u1 = XmlUtil.createUnmarshaller(Person.class);
        Unmarshaller u2 = XmlUtil.createUnmarshaller(Person.class);
        assertNotNull(u1);
        assertNotNull(u2);
        assertThrows(UncheckedException.class, () -> XmlUtil.createUnmarshaller("com.landawn.abacus.util"));
    }

    @Test
    public void testCreateDOMParser() {
        assertNotNull(XmlUtil.createDOMParser());
        assertNotNull(XmlUtil.createDOMParser(true, true));
        assertNotNull(XmlUtil.createDOMParser(false, false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testContentParserPool() throws Exception {
        DocumentBuilder p1 = XmlUtil.createContentParser();
        DocumentBuilder p2 = XmlUtil.createContentParser();
        assertNotNull(p1);
        assertNotNull(p2);
        XmlUtil.recycleContentParser(p1);
        XmlUtil.recycleContentParser(p2);
        XmlUtil.recycleContentParser(null);
        assertNotNull(XmlUtil.createContentParser());

        java.lang.reflect.Field poolField = XmlUtil.class.getDeclaredField("contentDocBuilderPool");
        poolField.setAccessible(true);
        Queue<DocumentBuilder> pool = (Queue<DocumentBuilder>) poolField.get(null);

        java.lang.reflect.Field pooledField = XmlUtil.class.getDeclaredField("pooledContentParsers");
        pooledField.setAccessible(true);
        Map<DocumentBuilder, Boolean> pooled = (Map<DocumentBuilder, Boolean>) pooledField.get(null);

        synchronized (pool) {
            // pooledContentParsers mirrors the queue, so both have to be cleared together. Clearing only the
            // queue would strand a strong entry for a builder that is no longer pooled, and
            // recycleContentParser would then refuse that live builder as a duplicate for the rest of the JVM.
            pool.clear();
            pooled.clear();
        }
        XmlUtil.recycleContentParser(DocumentBuilderFactory.newInstance().newDocumentBuilder());
        synchronized (pool) {
            assertTrue(pool.isEmpty());
            assertEquals(pool.size(), pooled.size(), "the membership map must still mirror the queue");
        }
        DocumentBuilder owned = XmlUtil.createContentParser();
        XmlUtil.recycleContentParser(owned);
        XmlUtil.recycleContentParser(owned);
        synchronized (pool) {
            assertEquals(1, pool.size());
            assertEquals(pool.size(), pooled.size(), "the membership map must still mirror the queue");
            pool.clear();
            pooled.clear();
            assertEquals(pool.size(), pooled.size(), "the membership map must still mirror the queue");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSAXParserPool() throws Exception {
        SAXParser s1 = XmlUtil.createSAXParser();
        SAXParser s2 = XmlUtil.createSAXParser();
        assertNotNull(s1);
        assertNotNull(s2);
        XmlUtil.recycleSAXParser(s1);
        XmlUtil.recycleSAXParser(s2);
        XmlUtil.recycleSAXParser(null);
        assertNotNull(XmlUtil.createSAXParser());

        java.lang.reflect.Field poolField = XmlUtil.class.getDeclaredField("saxParserPool");
        poolField.setAccessible(true);
        Queue<SAXParser> pool = (Queue<SAXParser>) poolField.get(null);

        java.lang.reflect.Field pooledField = XmlUtil.class.getDeclaredField("pooledSaxParsers");
        pooledField.setAccessible(true);
        Map<SAXParser, Boolean> pooled = (Map<SAXParser, Boolean>) pooledField.get(null);

        synchronized (pool) {
            // See the DocumentBuilder twin: the queue and its membership map must be cleared together, or a
            // live owned parser stays in the map and recycleSAXParser refuses it as a duplicate forever.
            pool.clear();
            pooled.clear();
        }
        XmlUtil.recycleSAXParser(SAXParserFactory.newInstance().newSAXParser());
        synchronized (pool) {
            assertTrue(pool.isEmpty());
            assertEquals(pool.size(), pooled.size(), "the membership map must still mirror the queue");
        }
        SAXParser owned = XmlUtil.createSAXParser();
        XmlUtil.recycleSAXParser(owned);
        XmlUtil.recycleSAXParser(owned);
        synchronized (pool) {
            assertEquals(1, pool.size());
            assertEquals(pool.size(), pooled.size(), "the membership map must still mirror the queue");
            pool.clear();
            pooled.clear();
            assertEquals(pool.size(), pooled.size(), "the membership map must still mirror the queue");
        }
    }

    @Test
    public void testDomAndSaxParsersRejectDoctype() throws Exception {
        byte[] hostileXml = "<!DOCTYPE root [<!ENTITY xxe SYSTEM \"file:///definitely-not-readable-abacus-xxe\">]><root>&xxe;</root>".getBytes(Charsets.UTF_8);
        DocumentBuilder documentBuilder = XmlUtil.createContentParser();
        try {
            assertThrows(Exception.class, () -> documentBuilder.parse(new ByteArrayInputStream(hostileXml)));
        } finally {
            XmlUtil.recycleContentParser(documentBuilder);
        }
        SAXParser saxParser = XmlUtil.createSAXParser();
        try {
            assertThrows(Exception.class, () -> saxParser.parse(new ByteArrayInputStream(hostileXml), new DefaultHandler()));
        } finally {
            XmlUtil.recycleSAXParser(saxParser);
        }
    }

    @Test
    public void testCreateXMLStreamReader() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root><item>test</item></root>";
        assertNotNull(XmlUtil.createXMLStreamReader(new StringReader(xml)));
        try (InputStream is = new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8))) {
            assertNotNull(XmlUtil.createXMLStreamReader(is));
        }
        try (InputStream is = new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8))) {
            assertNotNull(XmlUtil.createXMLStreamReader(is, "UTF-8"));
        }

        File externalEntityFile = File.createTempFile("xmlutil-xxe-", ".txt");
        try {
            Files.write(externalEntityFile.toPath(), "XXE_MARKER_2026".getBytes(Charsets.UTF_8));
            String maliciousXml = "<?xml version=\"1.0\"?><!DOCTYPE root [<!ENTITY xxe SYSTEM \"" + externalEntityFile.toURI() + "\">]><root>&xxe;</root>";
            assertThrows(Exception.class, () -> {
                XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(new StringReader(maliciousXml));
                try {
                    while (xmlReader.hasNext()) {
                        xmlReader.next();
                    }
                } finally {
                    xmlReader.close();
                }
            });
        } finally {
            externalEntityFile.delete();
        }
    }

    @Test
    public void testCreateFilteredStreamReader() throws Exception {
        XMLStreamReader source = XmlUtil.createXMLStreamReader(new StringReader("<?xml version=\"1.0\"?><root><item>test</item></root>"));
        assertNotNull(XmlUtil.createFilteredStreamReader(source, r -> r.isStartElement() || r.isEndElement()));

        XMLStreamReader nullFilterSource = XmlUtil.createXMLStreamReader(new StringReader("<root/>"));
        try {
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.createFilteredStreamReader(nullFilterSource, null));
        } finally {
            nullFilterSource.close();
        }
    }

    @Test
    public void testCreateXMLStreamWriter() {
        assertNotNull(XmlUtil.createXMLStreamWriter(new StringWriter()));
        assertNotNull(XmlUtil.createXMLStreamWriter(new ByteArrayOutputStream()));
        assertNotNull(XmlUtil.createXMLStreamWriter(new ByteArrayOutputStream(), "UTF-8"));
    }

    @Test
    public void testConcurrentStaxReaderAndWriterCreation() {
        assertDoesNotThrow(() -> java.util.stream.IntStream.range(0, 200).parallel().forEach(i -> {
            XMLStreamReader reader = null;
            XMLStreamWriter writer = null;
            try {
                reader = XmlUtil.createXMLStreamReader(new StringReader("<root><value>" + i + "</value></root>"));
                writer = XmlUtil.createXMLStreamWriter(new StringWriter());
                assertEquals(XMLStreamConstants.START_DOCUMENT, reader.getEventType());
                writer.writeStartDocument();
                writer.writeEmptyElement("root");
                writer.writeEndDocument();
            } catch (Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (writer != null) {
                        writer.close();
                    }
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        }));
    }

    @Test
    public void testCreateXMLTransformer() {
        Transformer transformer = XmlUtil.createXMLTransformer();
        assertNotNull(transformer);
    }

    @Test
    public void testTransform() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        doc.appendChild(root);

        File tempFile = File.createTempFile("xmlutil-test", ".xml");
        tempFile.deleteOnExit();
        try {
            XmlUtil.transform(doc, tempFile);
            assertTrue(tempFile.exists() && tempFile.length() > 0);
            assertTrue(new String(Files.readAllBytes(tempFile.toPath())).contains("<root"));
        } finally {
            tempFile.delete();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.transform(doc, baos);
        assertTrue(baos.toString().contains("<root"));

        StringWriter writer = new StringWriter();
        XmlUtil.transform(doc, writer);
        assertTrue(writer.toString().contains("<root"));
    }

    @Test
    public void testTransform_EdgeCase() throws Exception {
        Document doc = XmlUtil.createDOMParser().newDocument();
        Element root = doc.createElement("root");
        root.setTextContent("Café 中文");
        doc.appendChild(root);

        File tempFile = File.createTempFile("xmlutil-utf8", ".xml");
        tempFile.deleteOnExit();
        try {
            XmlUtil.transform(doc, tempFile);
            String content = new String(Files.readAllBytes(tempFile.toPath()), Charsets.UTF_8);
            assertTrue(content.contains("Café 中文"));
            assertEquals("Café 中文", XmlUtil.createDOMParser().parse(tempFile).getDocumentElement().getTextContent());
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void testTransformRejectsNullBeforeWriting() throws Exception {
        File existingFile = File.createTempFile("xmlutil-null-source-", ".xml");
        java.nio.file.Path directory = Files.createTempDirectory("xmlutil-null-source-");
        File missingFile = directory.resolve("missing.xml").toFile();
        try {
            Files.writeString(existingFile.toPath(), "original content", Charsets.UTF_8);
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(null, existingFile));
            assertEquals("original content", Files.readString(existingFile.toPath(), Charsets.UTF_8));

            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(null, missingFile));
            assertFalse(missingFile.exists());

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write("original content".getBytes(Charsets.UTF_8));
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(null, output));
            assertEquals("original content", output.toString(Charsets.UTF_8));

            StringWriter writer = new StringWriter();
            writer.write("original content");
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(null, writer));
            assertEquals("original content", writer.toString());

            Document document = parse("<root/>").getOwnerDocument();
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(document, (File) null));
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(document, (java.io.OutputStream) null));
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(document, (java.io.Writer) null));
        } finally {
            Files.deleteIfExists(existingFile.toPath());
            Files.deleteIfExists(missingFile.toPath());
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void testXmlEncodeDecode_disabledByDefault() {
        assertThrows(UnsupportedOperationException.class, () -> XmlUtil.xmlEncode(new Person("Bob", 35)));
        assertThrows(UnsupportedOperationException.class, () -> XmlUtil.xmlDecode("<?xml ?><void/>"));
    }

    @Test
    public void testGetElementsAndNodesByName() throws Exception {
        Element root = parse("<?xml version=\"1.0\"?><root><child>1</child><child>2</child><other><child>3</child></other></root>");
        List<Element> children = XmlUtil.getElementsByTagName(root, "child");
        assertEquals(2, children.size());
        assertEquals("1", children.get(0).getTextContent());
        assertEquals("2", children.get(1).getTextContent());
        List<Element> allDirect = XmlUtil.getElementsByTagName(root, "*");
        assertEquals(3, allDirect.size());
        assertEquals(List.of("child", "child", "other"), allDirect.stream().map(Element::getTagName).toList());

        Document doc = root.getOwnerDocument();
        List<Node> items = XmlUtil
                .getNodesByName(parse("<?xml version=\"1.0\"?><root><item>1</item><container><item>2</item></container></root>").getOwnerDocument(), "item");
        assertEquals(2, items.size());

        Node found = XmlUtil.getNextNodeByName(parse("<?xml version=\"1.0\"?><root><item>1</item><other>2</other><item>3</item></root>").getOwnerDocument(),
                "item");
        assertEquals("item", found.getNodeName());
        assertNull(XmlUtil.getNextNodeByName(doc, "nonexistent"));

        Element itemRoot = parse("<?xml version=\"1.0\"?><item>value</item>");
        assertEquals("item", XmlUtil.getNextNodeByName(itemRoot, "item").getNodeName());
    }

    @Test
    public void testGetAttributeAndReadAttributes() throws Exception {
        Element root = parse("<?xml version=\"1.0\"?><root id=\"123\" name=\"test\" value=\"abc\"/>");
        assertEquals("123", XmlUtil.getAttribute(root, "id"));
        assertEquals("test", XmlUtil.getAttribute(root, "name"));
        assertNull(XmlUtil.getAttribute(root, "missing"));

        Map<String, String> attrs = XmlUtil.readAttributes(root);
        assertEquals("123", attrs.get("id"));
        assertEquals("test", attrs.get("name"));
        assertEquals("abc", attrs.get("value"));

        Element withClass = parse("<?xml version=\"1.0\"?><root id=\"42\" class=\"test\"/>");
        Map<String, String> classAttrs = XmlUtil.readAttributes(withClass);
        assertEquals("42", classAttrs.get("id"));
        assertEquals("test", classAttrs.get("class"));

        assertTrue(XmlUtil.readAttributes(parse("<?xml version=\"1.0\"?><root/>")).isEmpty());
    }

    @Test
    public void testReadElement() throws Exception {
        Map<String, String> textRoot = XmlUtil.readElement(parse("<?xml version=\"1.0\"?><name>John</name>"));
        assertTrue(textRoot.containsKey("name") || textRoot.containsKey("name.name"));

        Map<String, String> person = XmlUtil.readElement(parse("<?xml version=\"1.0\"?><person age=\"30\"><name>John</name><city>NYC</city></person>"));
        assertEquals("30", person.get("age"));
        assertTrue(person.containsKey("person.name"));
        assertTrue(person.containsKey("person.city"));

        Map<String, String> nested = XmlUtil.readElement(parse("<?xml version=\"1.0\"?><person id=\"7\"><home zip=\"10001\"/><work zip=\"94105\"/></person>"));
        assertEquals("7", nested.get("id"));
        assertEquals("10001", nested.get("person.home.zip"));
        assertEquals("94105", nested.get("person.work.zip"));
        assertFalse(nested.containsKey("person.zip"));
    }

    @Test
    public void testIsTextElement() throws Exception {
        Element root = parse("<?xml version=\"1.0\"?><root><text>value</text><parent><child>nested</child></parent><empty/></root>");
        NodeList children = root.getChildNodes();
        Element textElem = null;
        Element parentElem = null;
        Element empty = null;
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if ("text".equals(node.getNodeName())) {
                    textElem = (Element) node;
                } else if ("parent".equals(node.getNodeName())) {
                    parentElem = (Element) node;
                } else if ("empty".equals(node.getNodeName())) {
                    empty = (Element) node;
                }
            }
        }
        assertTrue(XmlUtil.isTextElement(textElem));
        assertFalse(XmlUtil.isTextElement(parentElem));
        assertTrue(XmlUtil.isTextElement(empty));
    }

    @Test
    public void testGetTextContent() throws Exception {
        assertEquals("  Hello World  ", XmlUtil.getTextContent(parse("<?xml version=\"1.0\"?><root>  Hello World  </root>")));
        assertEquals("", XmlUtil.getTextContent(parse("<?xml version=\"1.0\"?><root></root>")));

        Element ws = parse("<?xml version=\"1.0\"?><root>  Hello\n\tWorld  </root>");
        assertEquals("Hello World", XmlUtil.getTextContent(ws, true));
        assertTrue(XmlUtil.getTextContent(ws, false).contains("\n") || XmlUtil.getTextContent(ws, false).contains("\t"));
        assertEquals("", XmlUtil.getTextContent(parse("<?xml version=\"1.0\"?><root></root>"), true));

        DocumentBuilder db = XmlUtil.createContentParser();
        try {
            Node contentRoot = db.parse(new ByteArrayInputStream("<root>  Hello\n\tWorld  </root>".getBytes("UTF-8"))).getDocumentElement();
            String normalized = XmlUtil.getTextContent(contentRoot, true);
            assertFalse(normalized.startsWith(" "));
            assertFalse(normalized.endsWith(" "));
            assertEquals("  Hello World  ",
                    XmlUtil.getTextContent(db.parse(new ByteArrayInputStream("<root>  Hello World  </root>".getBytes("UTF-8"))).getDocumentElement(), false));
        } finally {
            XmlUtil.recycleContentParser(db);
        }

        String trailing = XmlUtil.getTextContent(parse("<root>hello\t</root>"), true);
        assertTrue(trailing.endsWith("hello") || trailing.equals("hello"));
    }

    @Test
    public void testWriteCharacters() throws Exception {
        StringBuilder sb = new StringBuilder();
        XmlUtil.writeCharacters("<hello> & \"world\"".toCharArray(), sb);
        assertTrue(sb.toString().contains("&lt;") && sb.toString().contains("&gt;") && sb.toString().contains("&amp;") && sb.toString().contains("&quot;"));

        sb = new StringBuilder();
        XmlUtil.writeCharacters("prefix <tag> suffix".toCharArray(), 7, 5, sb);
        assertTrue(sb.toString().contains("&lt;tag&gt;"));

        sb = new StringBuilder();
        XmlUtil.writeCharacters("<data> & 'value'", sb);
        assertTrue(sb.toString().contains("&lt;") && sb.toString().contains("&amp;") && sb.toString().contains("&apos;"));

        sb = new StringBuilder();
        XmlUtil.writeCharacters("prefix <tag> suffix", 7, 5, sb);
        assertTrue(sb.toString().contains("&lt;tag&gt;"));

        sb = new StringBuilder();
        XmlUtil.writeCharacters((String) null, sb);
        assertEquals("null", sb.toString());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.writeCharacters("<test>".toCharArray(), baos);
        assertTrue(baos.toString().contains("&lt;test&gt;"));
        baos.reset();
        XmlUtil.writeCharacters("prefix <tag> suffix".toCharArray(), 7, 5, baos);
        assertTrue(baos.toString().contains("&lt;tag&gt;"));
        baos.reset();
        XmlUtil.writeCharacters("<data> & value", baos);
        assertTrue(baos.toString().contains("&lt;") && baos.toString().contains("&amp;"));
        baos.reset();
        XmlUtil.writeCharacters("prefix <tag> suffix", 7, 5, baos);
        assertTrue(baos.toString().contains("&lt;tag&gt;"));

        StringWriter writer = new StringWriter();
        XmlUtil.writeCharacters("<element>".toCharArray(), writer);
        assertTrue(writer.toString().contains("&lt;element&gt;"));
        writer = new StringWriter();
        XmlUtil.writeCharacters("prefix <tag> suffix".toCharArray(), 7, 5, writer);
        assertTrue(writer.toString().contains("&lt;tag&gt;"));
        writer = new StringWriter();
        XmlUtil.writeCharacters("<root> & \"data\"", writer);
        assertTrue(writer.toString().contains("&lt;") && writer.toString().contains("&amp;") && writer.toString().contains("&quot;"));
        writer = new StringWriter();
        XmlUtil.writeCharacters("prefix <tag> suffix", 7, 5, writer);
        assertTrue(writer.toString().contains("&lt;tag&gt;"));
    }

    @Test
    public void testWriteCharactersPreservesWriterFailuresWithoutRetrying() {
        for (boolean arrayInput : new boolean[] { false, true }) {
            for (boolean failOnFlush : new boolean[] { false, true }) {
                IOException failure = new IOException("original writer failure");
                int[] calls = new int[3];
                Writer output = new Writer() {
                    @Override
                    public void write(char[] buffer, int offset, int length) throws IOException {
                        calls[0]++;
                        if (!failOnFlush) {
                            throw failure;
                        }
                    }

                    @Override
                    public void flush() throws IOException {
                        calls[1]++;
                        if (failOnFlush) {
                            throw failure;
                        }
                    }

                    @Override
                    public void close() {
                        calls[2]++;
                    }
                };
                IOException actual = assertThrows(IOException.class, () -> {
                    if (arrayInput) {
                        XmlUtil.writeCharacters("x<&>y".toCharArray(), 1, 3, output);
                    } else {
                        XmlUtil.writeCharacters("x<&>y", 1, 3, output);
                    }
                });
                assertSame(failure, actual);
                assertEquals(1, calls[0]);
                assertEquals(failOnFlush ? 1 : 0, calls[1]);
                assertEquals(0, calls[2]);
            }
        }
    }

    @Test
    public void testWriteCharactersPreservesOutputStreamFailuresWithoutRetrying() {
        for (boolean arrayInput : new boolean[] { false, true }) {
            for (boolean failOnFlush : new boolean[] { false, true }) {
                IOException failure = new IOException("original stream failure");
                int[] calls = new int[3];
                OutputStream output = new OutputStream() {
                    @Override
                    public void write(int value) throws IOException {
                        write(new byte[] { (byte) value }, 0, 1);
                    }

                    @Override
                    public void write(byte[] buffer, int offset, int length) throws IOException {
                        calls[0]++;
                        if (!failOnFlush) {
                            throw failure;
                        }
                    }

                    @Override
                    public void flush() throws IOException {
                        calls[1]++;
                        if (failOnFlush) {
                            throw failure;
                        }
                    }

                    @Override
                    public void close() {
                        calls[2]++;
                    }
                };
                IOException actual = assertThrows(IOException.class, () -> {
                    if (arrayInput) {
                        XmlUtil.writeCharacters("x<&>y".toCharArray(), 1, 3, output);
                    } else {
                        XmlUtil.writeCharacters("x<&>y", 1, 3, output);
                    }
                });
                assertSame(failure, actual);
                assertEquals(1, calls[0]);
                assertEquals(failOnFlush ? 1 : 0, calls[1]);
                assertEquals(0, calls[2]);
            }
        }
    }

    @Test
    public void testGetAttributeTypeClass() throws Exception {
        assertEquals(int.class, XmlUtil.getAttributeTypeClass(parse("<?xml version=\"1.0\"?><root type=\"int\"/>")));
        assertNull(XmlUtil.getAttributeTypeClass(parse("<?xml version=\"1.0\"?><root/>")));
        assertEquals(String.class, XmlUtil.getAttributeTypeClass(parse("<?xml version=\"1.0\"?><root type=\"String\"/>")));
        assertNull(XmlUtil.getAttributeTypeClass(parse("<?xml version=\"1.0\"?><root type=\"com.unknown.NonExistentClass12345XYZ\"/>")));
        assertEquals(java.util.LinkedHashMap.class, XmlUtil.getAttributeTypeClass(parse("<root type=\"java.util.LinkedHashMap\"/>")));
        assertNull(XmlUtil.getAttributeTypeClass(parse("<root type=\"java.lang.Runtime\"/>")));
        assertNull(XmlUtil.getAttributeTypeClass(parse("<root type=\"java.util.List&lt;java.lang.Runtime&gt;\"/>")));
        assertEquals(String[][].class, XmlUtil.getAttributeTypeClass(parse("<root type=\"java.lang.String[][]\"/>")));
    }

    @Test
    public void testGetConcreteClass() throws Exception {
        assertEquals(String.class, XmlUtil.getConcreteClass(Object.class, String.class));
        assertEquals(Integer.class, XmlUtil.getConcreteClass(Integer.class, String.class));
        assertEquals(String.class, XmlUtil.getConcreteClass(String.class, (Class<?>) null));
        assertEquals(String.class, XmlUtil.getConcreteClass(null, String.class));
        assertEquals(String.class, XmlUtil.getConcreteClass(String.class, (Node) null));
        assertNotNull(XmlUtil.getConcreteClass(Object.class, parse("<?xml version=\"1.0\"?><root type=\"int\"/>")));
    }

    @Test
    public void testGetNodeType() {
        assertEquals(XmlUtil.NodeType.PROPERTY, XmlUtil.getNodeType("anything", XmlUtil.NodeType.ENTITY));
        assertEquals(XmlUtil.NodeType.ARRAY, XmlUtil.getNodeType("array", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.ENTITY, XmlUtil.getNodeType("unknownTag", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.COLLECTION, XmlUtil.getNodeType("list", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.MAP, XmlUtil.getNodeType("map", XmlUtil.NodeType.PROPERTY));
    }

    /**
     * An exception whose cause chain loops back on itself, without touching any JDK internals: {@code getCause()}
     * is overridden to return a target set after construction.
     */
    private static final class LoopingCauseException extends Exception {
        private static final long serialVersionUID = 1L;

        private transient Throwable target;

        LoopingCauseException(final String message) {
            super(message);
        }

        void pointAt(final Throwable t) {
            target = t;
        }

        @Override
        public synchronized Throwable getCause() {
            return target;
        }
    }

    /**
     * A cyclic cause chain must not make the {@code IOException} search loop forever.
     *
     * <p>Guarded by a preemptive timeout on purpose: the defect was an unbounded walk, and it ran inside
     * {@code synchronized (xmlInputFactory)}, so without the timeout a regression would wedge the whole suite.</p>
     */
    @Test
    public void testToRuntimeException_CyclicCauseChainTerminates() throws Exception {
        final LoopingCauseException loop = new LoopingCauseException("loop");
        final XMLStreamException top = new XMLStreamException("top", loop);
        loop.pointAt(top);

        // Reflection, not a widened modifier: the same test then exercises the pre-fix build too, so its
        // RED-on-base result is the unbounded walk itself rather than an access error.
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> assertNotNull(invokeToRuntimeException(top)));
    }

    /** An {@code IOException} anywhere in an acyclic chain is still unwrapped. */
    @Test
    public void testToRuntimeException_UnwrapsIOExceptionFromNestedCause() throws Exception {
        final java.io.IOException io = new java.io.IOException("disk");
        final XMLStreamException top = new XMLStreamException("top", new IllegalStateException("mid", io));

        assertTrue(invokeToRuntimeException(top) instanceof com.landawn.abacus.exception.UncheckedIOException);
    }

    private static RuntimeException invokeToRuntimeException(final XMLStreamException e) throws Exception {
        final java.lang.reflect.Method m = XmlUtil.class.getDeclaredMethod("toRuntimeException", XMLStreamException.class);
        m.setAccessible(true);

        try {
            return (RuntimeException) m.invoke(null, e);
        } catch (final java.lang.reflect.InvocationTargetException ite) {
            final Throwable cause = ite.getCause();

            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }

            throw new AssertionError("unexpected checked exception from toRuntimeException", cause);
        }
    }

    /**
     * The container element names the abacus XML writers really emit must map to {@code COLLECTION}, matching the
     * authoritative table in {@code AbacusXmlParserImpl}.
     */
    @Test
    public void testGetNodeType_SetAndCollectionAreContainerNames() {
        assertEquals(XmlUtil.NodeType.COLLECTION, XmlUtil.getNodeType("set", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.COLLECTION, XmlUtil.getNodeType("collection", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.COLLECTION, XmlUtil.getNodeType("list", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.ARRAY, XmlUtil.getNodeType("array", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.ENTITY, XmlUtil.getNodeType("unknownTag", XmlUtil.NodeType.PROPERTY));
    }

    /** Contract pin for the documented {@code NullPointerException} of the four JAXB factory methods. */
    @Test
    public void testCreateMarshallerAndUnmarshaller_NullArgument() {
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.createMarshaller((String) null));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.createMarshaller((Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.createUnmarshaller((String) null));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.createUnmarshaller((Class<?>) null));
    }

    /**
     * Contract pin: a non-null node can still have no DOM text content. {@code Document} and
     * {@code DocumentType} nodes report {@code null}, which both {@code getTextContent} overloads pass through.
     */
    @Test
    public void testGetTextContent_NodeTypesWithoutTextContent() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream("<root>hello</root>".getBytes(Charsets.UTF_8)));

        assertEquals(Node.DOCUMENT_NODE, doc.getNodeType());
        assertNull(XmlUtil.getTextContent(doc));
        assertNull(XmlUtil.getTextContent(doc, true));
        assertNull(XmlUtil.getTextContent(doc, false));

        // The factory disallows DOCTYPE, so build the document-type node through the DOM implementation.
        org.w3c.dom.DocumentType docType = builder.getDOMImplementation().createDocumentType("qname", "publicId", "systemId");

        assertEquals(Node.DOCUMENT_TYPE_NODE, docType.getNodeType());
        assertNull(XmlUtil.getTextContent(docType));
        assertNull(XmlUtil.getTextContent(docType, true));
        assertNull(XmlUtil.getTextContent(docType, false));

        assertEquals("hello", XmlUtil.getTextContent(doc.getDocumentElement()));
        assertEquals("hello", XmlUtil.getTextContent(doc.getDocumentElement(), true));
    }

    /**
     * The writer-side predicate must answer exactly what the reader does, including for values with surrounding
     * whitespace: without the reader's {@code trim()} a trailing space defeated the {@code "[]"} suffix stripping.
     */
    @Test
    public void testIsResolvableXmlTypeAttributeName_AgreesWithReaderOnPaddedNames() throws Exception {
        assertTrue(XmlUtil.isResolvableXmlTypeAttributeName(" java.util.List[] "));
        assertNotNull(XmlUtil.getAttributeType(parse("<root type=\" java.util.List[] \"/>")));

        assertTrue(XmlUtil.isResolvableXmlTypeAttributeName(" int[] "));
        assertNotNull(XmlUtil.getAttributeType(parse("<root type=\" int[] \"/>")));

        assertTrue(XmlUtil.isResolvableXmlTypeAttributeName(" java.util.List "));
        assertNotNull(XmlUtil.getAttributeType(parse("<root type=\" java.util.List \"/>")));

        assertFalse(XmlUtil.isResolvableXmlTypeAttributeName(" "));
        assertNull(XmlUtil.getAttributeType(parse("<root type=\" \"/>")));

        assertFalse(XmlUtil.isResolvableXmlTypeAttributeName("\t"));
        assertFalse(XmlUtil.isResolvableXmlTypeAttributeName(""));
        assertFalse(XmlUtil.isResolvableXmlTypeAttributeName(null));

        assertFalse(XmlUtil.isResolvableXmlTypeAttributeName(" java.lang.Runtime "));
        assertNull(XmlUtil.getAttributeType(parse("<root type=\" java.lang.Runtime \"/>")));

        assertTrue(XmlUtil.isResolvableXmlTypeAttributeName("java.util.List[]"));
        assertNotNull(XmlUtil.getAttributeType(parse("<root type=\"java.util.List[]\"/>")));
    }

    /**
     * Contract pin for the documented {@code IndexOutOfBoundsException} of the six bounded
     * {@code writeCharacters} overloads. The exception is not an {@code IOException}, so the StringBuilder
     * overloads do not convert it either.
     */
    @Test
    public void testWriteCharacters_OutOfRangeOffsetOrLength() {
        char[] cbuf = "abc".toCharArray();

        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters(cbuf, 2, 5, new StringBuilder()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters(cbuf, -1, 1, new StringBuilder()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", 2, 5, new StringBuilder()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", 0, -1, new StringBuilder()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters((String) null, 0, 9, new StringBuilder()));

        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters(cbuf, 2, 5, new ByteArrayOutputStream()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", 2, 5, new ByteArrayOutputStream()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters(cbuf, 2, 5, new StringWriter()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", 2, 5, new StringWriter()));

        // The documented null-String behaviour: off/len select a slice of the literal "null".
        StringBuilder sb = new StringBuilder();
        XmlUtil.writeCharacters((String) null, 1, 3, sb);
        assertEquals("ull", sb.toString());
    }

    /** A {@code DocumentBuilder} whose {@code reset()} is unsupported, which JAXP explicitly permits. */
    private static final class ResetUnsupportedDocumentBuilder extends DocumentBuilder {
        @Override
        public Document parse(final org.xml.sax.InputSource is) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNamespaceAware() {
            return true;
        }

        @Override
        public boolean isValidating() {
            return false;
        }

        @Override
        public void setEntityResolver(final org.xml.sax.EntityResolver er) {
            // no-op
        }

        @Override
        public void setErrorHandler(final org.xml.sax.ErrorHandler eh) {
            // no-op
        }

        @Override
        public Document newDocument() {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.w3c.dom.DOMImplementation getDOMImplementation() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Contract pin for the two documented silent drops: an owned, not-yet-pooled parser is discarded (without
     * an exception) when the pool is at capacity, and likewise when the provider's {@code reset()} throws.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testRecycleContentParserDropsWhenPoolIsFullOrResetFails() throws Exception {
        java.lang.reflect.Field poolField = XmlUtil.class.getDeclaredField("contentDocBuilderPool");
        poolField.setAccessible(true);
        Queue<DocumentBuilder> pool = (Queue<DocumentBuilder>) poolField.get(null);

        java.lang.reflect.Field pooledField = XmlUtil.class.getDeclaredField("pooledContentParsers");
        pooledField.setAccessible(true);
        Map<DocumentBuilder, Boolean> pooled = (Map<DocumentBuilder, Boolean>) pooledField.get(null);

        java.lang.reflect.Field poolSizeField = XmlUtil.class.getDeclaredField("POOL_SIZE");
        poolSizeField.setAccessible(true);
        int poolSize = (Integer) poolSizeField.get(null);

        DocumentBuilder victim = XmlUtil.createContentParser();

        synchronized (pool) {
            // pooledContentParsers mirrors the queue, so both have to be rewritten together. Clearing only
            // the queue would strand a strong entry for a builder that is no longer pooled, and
            // recycleContentParser would then refuse that live builder as a duplicate for the rest of the JVM.
            pool.clear();
            pooled.clear();

            for (int i = 0; i < poolSize; i++) {
                DocumentBuilder filler = new ResetUnsupportedDocumentBuilder();
                pool.add(filler);
                pooled.put(filler, Boolean.TRUE);
            }
        }

        assertDoesNotThrow(() -> XmlUtil.recycleContentParser(victim));

        synchronized (pool) {
            assertEquals(poolSize, pool.size());
            assertFalse(pooled.containsKey(victim));
            pool.clear();
            pooled.clear();
        }

        java.lang.reflect.Field ownedField = XmlUtil.class.getDeclaredField("ownedContentParsers");
        ownedField.setAccessible(true);
        Object owned = ownedField.get(null);
        java.lang.reflect.Method add = owned.getClass().getDeclaredMethod("add", Object.class);
        add.setAccessible(true);

        DocumentBuilder resetFails = new ResetUnsupportedDocumentBuilder();
        assertThrows(UnsupportedOperationException.class, resetFails::reset);
        add.invoke(owned, resetFails);

        assertDoesNotThrow(() -> XmlUtil.recycleContentParser(resetFails));

        synchronized (pool) {
            assertEquals(0, pool.size());
            assertFalse(pooled.containsKey(resetFails));
            assertEquals(pool.size(), pooled.size(), "the membership map must still mirror the queue");
        }
    }

    /** A {@code SAXParser} whose {@code reset()} is unsupported, which JAXP explicitly permits. */
    private static final class ResetUnsupportedSaxParser extends SAXParser {
        @Override
        @SuppressWarnings("deprecation")
        public org.xml.sax.Parser getParser() {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.xml.sax.XMLReader getXMLReader() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNamespaceAware() {
            return true;
        }

        @Override
        public boolean isValidating() {
            return false;
        }

        @Override
        public void setProperty(final String name, final Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getProperty(final String name) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * The SAX sibling of {@link #testRecycleContentParserDropsWhenPoolIsFullOrResetFails()}: the same two
     * silent drops are documented on {@code recycleSAXParser}, so they are pinned the same way.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testRecycleSaxParserDropsWhenPoolIsFullOrResetFails() throws Exception {
        java.lang.reflect.Field poolField = XmlUtil.class.getDeclaredField("saxParserPool");
        poolField.setAccessible(true);
        Queue<SAXParser> pool = (Queue<SAXParser>) poolField.get(null);

        java.lang.reflect.Field pooledField = XmlUtil.class.getDeclaredField("pooledSaxParsers");
        pooledField.setAccessible(true);
        Map<SAXParser, Boolean> pooled = (Map<SAXParser, Boolean>) pooledField.get(null);

        java.lang.reflect.Field poolSizeField = XmlUtil.class.getDeclaredField("POOL_SIZE");
        poolSizeField.setAccessible(true);
        int poolSize = (Integer) poolSizeField.get(null);

        SAXParser victim = XmlUtil.createSAXParser();

        synchronized (pool) {
            // See the DocumentBuilder twin: the queue and its membership map must be rewritten together.
            pool.clear();
            pooled.clear();

            for (int i = 0; i < poolSize; i++) {
                SAXParser filler = new ResetUnsupportedSaxParser();
                pool.add(filler);
                pooled.put(filler, Boolean.TRUE);
            }
        }

        assertDoesNotThrow(() -> XmlUtil.recycleSAXParser(victim));

        synchronized (pool) {
            assertEquals(poolSize, pool.size());
            assertFalse(pooled.containsKey(victim));
            pool.clear();
            pooled.clear();
        }

        java.lang.reflect.Field ownedField = XmlUtil.class.getDeclaredField("ownedSaxParsers");
        ownedField.setAccessible(true);
        Object owned = ownedField.get(null);
        java.lang.reflect.Method add = owned.getClass().getDeclaredMethod("add", Object.class);
        add.setAccessible(true);

        SAXParser resetFails = new ResetUnsupportedSaxParser();
        assertThrows(UnsupportedOperationException.class, resetFails::reset);
        add.invoke(owned, resetFails);

        assertDoesNotThrow(() -> XmlUtil.recycleSAXParser(resetFails));

        synchronized (pool) {
            assertEquals(0, pool.size());
            assertFalse(pooled.containsKey(resetFails));
            assertEquals(pool.size(), pooled.size(), "the membership map must still mirror the queue");
        }
    }

    /**
     * The writer factories must unwrap an {@code IOException} cause the same way their
     * {@code createXMLStreamReader} siblings do. An unsupported encoding name is the reachable case: the
     * provider reports it as an {@code XMLStreamException} wrapping an {@code UnsupportedEncodingException}.
     */
    @Test
    public void testCreateXMLStreamWriterUnwrapsIOExceptionCause() {
        com.landawn.abacus.exception.UncheckedIOException thrown = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> XmlUtil.createXMLStreamWriter(new ByteArrayOutputStream(), "no-such-charset-xyz"));

        assertTrue(thrown.getCause() instanceof java.io.UnsupportedEncodingException);
    }
}
