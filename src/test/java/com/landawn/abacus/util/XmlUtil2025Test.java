package com.landawn.abacus.util;

import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedException;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;

@Tag("2025")
public class XmlUtil2025Test extends TestBase {

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
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Person person = (Person) obj;
            return age == person.age && CommonUtil.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    @Test
    public void testMarshal() {
        Person person = new Person("John", 30);
        String xml = XmlUtil.marshal(person);

        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("<person>"));
        Assertions.assertTrue(xml.contains("<name>John</name>"));
        Assertions.assertTrue(xml.contains("<age>30</age>"));
        Assertions.assertTrue(xml.contains("</person>"));
    }

    @Test
    public void testUnmarshal() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><age>30</age><name>John</name></person>";
        Person person = XmlUtil.unmarshal(Person.class, xml);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
        Assertions.assertEquals(30, person.getAge());
    }

    @Test
    public void testMarshalUnmarshalRoundTrip() {
        Person original = new Person("Alice", 25);
        String xml = XmlUtil.marshal(original);
        Person restored = XmlUtil.unmarshal(Person.class, xml);

        Assertions.assertEquals(original, restored);
    }

    @Test
    public void testCreateMarshallerWithContextPath() {
        assertThrows(UncheckedException.class, () -> XmlUtil.createMarshaller("com.landawn.abacus.util"));
    }

    @Test
    public void testCreateMarshallerWithClass() {
        Marshaller marshaller = XmlUtil.createMarshaller(Person.class);
        Assertions.assertNotNull(marshaller);
    }

    @Test
    public void testCreateUnmarshallerWithContextPath() {
        assertThrows(UncheckedException.class, () -> XmlUtil.createUnmarshaller("com.landawn.abacus.util"));
    }

    @Test
    public void testCreateUnmarshallerWithClass() {
        Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(Person.class);
        Assertions.assertNotNull(unmarshaller);
    }

    @Test
    public void testCreateDOMParser() {
        DocumentBuilder parser = XmlUtil.createDOMParser();
        Assertions.assertNotNull(parser);
    }

    @Test
    public void testCreateDOMParserWithOptions() {
        DocumentBuilder parser = XmlUtil.createDOMParser(true, true);
        Assertions.assertNotNull(parser);

        DocumentBuilder parser2 = XmlUtil.createDOMParser(false, false);
        Assertions.assertNotNull(parser2);
    }

    @Test
    public void testCreateContentParser() {
        DocumentBuilder parser = XmlUtil.createContentParser();
        Assertions.assertNotNull(parser);
    }

    @Test
    public void testRecycleContentParser() {
        DocumentBuilder parser = XmlUtil.createContentParser();
        Assertions.assertNotNull(parser);

        XmlUtil.recycleContentParser(parser);

        XmlUtil.recycleContentParser(null);
    }

    @Test
    public void testCreateSAXParser() {
        SAXParser parser = XmlUtil.createSAXParser();
        Assertions.assertNotNull(parser);
    }

    @Test
    public void testRecycleSAXParser() {
        SAXParser parser = XmlUtil.createSAXParser();
        Assertions.assertNotNull(parser);

        XmlUtil.recycleSAXParser(parser);

        XmlUtil.recycleSAXParser(null);
    }

    @Test
    public void testCreateXMLStreamReaderFromReader() {
        String xml = "<?xml version=\"1.0\"?><root><item>test</item></root>";
        StringReader reader = new StringReader(xml);
        XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(reader);

        Assertions.assertNotNull(xmlReader);
    }

    @Test
    public void testCreateXMLStreamReaderFromInputStream() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root><item>test</item></root>";
        InputStream is = new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8));
        XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(is);

        Assertions.assertNotNull(xmlReader);
        is.close();
    }

    @Test
    public void testCreateXMLStreamReaderWithEncoding() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root><item>test</item></root>";
        InputStream is = new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8));
        XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(is, "UTF-8");

        Assertions.assertNotNull(xmlReader);
        is.close();
    }

    @Test
    public void testCreateFilteredStreamReader() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root><item>test</item></root>";
        StringReader reader = new StringReader(xml);
        XMLStreamReader source = XmlUtil.createXMLStreamReader(reader);

        XMLStreamReader filtered = XmlUtil.createFilteredStreamReader(source, r -> r.isStartElement() || r.isEndElement());

        Assertions.assertNotNull(filtered);
    }

    @Test
    public void testCreateXMLStreamWriterFromWriter() {
        StringWriter writer = new StringWriter();
        XMLStreamWriter xmlWriter = XmlUtil.createXMLStreamWriter(writer);

        Assertions.assertNotNull(xmlWriter);
    }

    @Test
    public void testCreateXMLStreamWriterFromOutputStream() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter xmlWriter = XmlUtil.createXMLStreamWriter(baos);

        Assertions.assertNotNull(xmlWriter);
    }

    @Test
    public void testCreateXMLStreamWriterWithEncoding() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter xmlWriter = XmlUtil.createXMLStreamWriter(baos, "UTF-8");

        Assertions.assertNotNull(xmlWriter);
    }

    @Test
    public void testCreateXMLTransformer() {
        Transformer transformer = XmlUtil.createXMLTransformer();
        Assertions.assertNotNull(transformer);
    }

    @Test
    public void testTransformToFile() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        doc.appendChild(root);

        File tempFile = File.createTempFile("xmlutil-test", ".xml");
        tempFile.deleteOnExit();

        XmlUtil.transform(doc, tempFile);

        Assertions.assertTrue(tempFile.exists());
        Assertions.assertTrue(tempFile.length() > 0);

        String content = new String(Files.readAllBytes(tempFile.toPath()));
        Assertions.assertTrue(content.contains("<root"));

        tempFile.delete();
    }

    @Test
    public void testTransformToOutputStream() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        doc.appendChild(root);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.transform(doc, baos);

        String result = baos.toString();
        Assertions.assertTrue(result.contains("<root"));
    }

    @Test
    public void testTransformToWriter() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        doc.appendChild(root);

        StringWriter writer = new StringWriter();
        XmlUtil.transform(doc, writer);

        String result = writer.toString();
        Assertions.assertTrue(result.contains("<root"));
    }

    @Test
    public void testXmlEncode() {
        Person person = new Person("Bob", 35);
        String xml = XmlUtil.xmlEncode(person);

        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("<?xml"));
        Assertions.assertTrue(xml.contains("java"));
    }

    @Test
    public void testXmlDecode() {
        Person original = new Person("Charlie", 40);
        String xml = XmlUtil.xmlEncode(original);
        Person decoded = XmlUtil.xmlDecode(xml);

        Assertions.assertNotNull(decoded);
        Assertions.assertEquals(original.getName(), decoded.getName());
        Assertions.assertEquals(original.getAge(), decoded.getAge());
    }

    @Test
    public void testGetElementsByTagName() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root><child>1</child><child>2</child><other><child>3</child></other></root>";
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8)));
        Element root = doc.getDocumentElement();

        List<Element> children = XmlUtil.getElementsByTagName(root, "child");

        Assertions.assertNotNull(children);
        Assertions.assertEquals(2, children.size());
    }

    @Test
    public void testGetNodesByName() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root><item>1</item><container><item>2</item></container></root>";
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8)));

        List<Node> nodes = XmlUtil.getNodesByName(doc, "item");

        Assertions.assertNotNull(nodes);
        Assertions.assertEquals(2, nodes.size());
    }

    @Test
    public void testGetNextNodeByName() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root><item>1</item><other>2</other><item>3</item></root>";
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8)));

        Node node = XmlUtil.getNextNodeByName(doc, "item");

        Assertions.assertNotNull(node);
        Assertions.assertEquals("item", node.getNodeName());
    }

    @Test
    public void testGetNextNodeByNameNotFound() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root><item>1</item></root>";
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8)));

        Node node = XmlUtil.getNextNodeByName(doc, "nonexistent");

        Assertions.assertNull(node);
    }

    @Test
    public void testGetAttribute() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root id=\"123\" name=\"test\"/>";
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8)));
        Element root = doc.getDocumentElement();

        String id = XmlUtil.getAttribute(root, "id");
        String name = XmlUtil.getAttribute(root, "name");
        String missing = XmlUtil.getAttribute(root, "missing");

        Assertions.assertEquals("123", id);
        Assertions.assertEquals("test", name);
        Assertions.assertNull(missing);
    }

    @Test
    public void testReadAttributes() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root id=\"123\" name=\"test\" value=\"abc\"/>";
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8)));
        Element root = doc.getDocumentElement();

        Map<String, String> attrs = XmlUtil.readAttributes(root);

        Assertions.assertNotNull(attrs);
        Assertions.assertEquals("123", attrs.get("id"));
        Assertions.assertEquals("test", attrs.get("name"));
        Assertions.assertEquals("abc", attrs.get("value"));
    }

    @Test
    public void testReadElement() throws Exception {
        String xml = "<?xml version=\"1.0\"?><person age=\"30\"><name>John</name><city>NYC</city></person>";
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8)));
        Element root = doc.getDocumentElement();

        Map<String, String> data = XmlUtil.readElement(root);

        Assertions.assertNotNull(data);
        Assertions.assertEquals("30", data.get("age"));
        Assertions.assertTrue(data.containsKey("person.name"));
        Assertions.assertTrue(data.containsKey("person.city"));
    }

    @Test
    public void testIsTextElement() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root><text>value</text><parent><child>nested</child></parent></root>";
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8)));
        Element root = doc.getDocumentElement();

        NodeList children = root.getChildNodes();
        Element textElem = null;
        Element parentElem = null;

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if ("text".equals(node.getNodeName())) {
                    textElem = (Element) node;
                } else if ("parent".equals(node.getNodeName())) {
                    parentElem = (Element) node;
                }
            }
        }

        Assertions.assertNotNull(textElem);
        Assertions.assertNotNull(parentElem);
        Assertions.assertTrue(XmlUtil.isTextElement(textElem));
        Assertions.assertFalse(XmlUtil.isTextElement(parentElem));
    }

    @Test
    public void testGetTextContent() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root>  Hello World  </root>";
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8)));
        Element root = doc.getDocumentElement();

        String content = XmlUtil.getTextContent(root);

        Assertions.assertNotNull(content);
        Assertions.assertEquals("  Hello World  ", content);
    }

    @Test
    public void testGetTextContentWithWhitespaceHandling() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root>  Hello\n\tWorld  </root>";
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8)));
        Element root = doc.getDocumentElement();

        String content = XmlUtil.getTextContent(root, true);

        Assertions.assertNotNull(content);
        Assertions.assertEquals("Hello World", content);

        String contentRaw = XmlUtil.getTextContent(root, false);
        Assertions.assertTrue(contentRaw.contains("\n") || contentRaw.contains("\t"));
    }

    @Test
    public void testWriteCharactersCharArrayToStringBuilder() throws Exception {
        char[] chars = "<hello> & \"world\"".toCharArray();
        StringBuilder sb = new StringBuilder();

        XmlUtil.writeCharacters(chars, sb);

        String result = sb.toString();
        Assertions.assertTrue(result.contains("&lt;"));
        Assertions.assertTrue(result.contains("&gt;"));
        Assertions.assertTrue(result.contains("&amp;"));
        Assertions.assertTrue(result.contains("&quot;"));
    }

    @Test
    public void testWriteCharactersCharArrayWithOffsetToStringBuilder() throws Exception {
        char[] chars = "prefix <tag> suffix".toCharArray();
        StringBuilder sb = new StringBuilder();

        XmlUtil.writeCharacters(chars, 7, 5, sb);

        String result = sb.toString();
        Assertions.assertTrue(result.contains("&lt;tag&gt;"));
    }

    @Test
    public void testWriteCharactersStringToStringBuilder() throws Exception {
        String str = "<data> & 'value'";
        StringBuilder sb = new StringBuilder();

        XmlUtil.writeCharacters(str, sb);

        String result = sb.toString();
        Assertions.assertTrue(result.contains("&lt;"));
        Assertions.assertTrue(result.contains("&amp;"));
        Assertions.assertTrue(result.contains("&apos;"));
    }

    @Test
    public void testWriteCharactersStringWithOffsetToStringBuilder() throws Exception {
        String str = "prefix <tag> suffix";
        StringBuilder sb = new StringBuilder();

        XmlUtil.writeCharacters(str, 7, 5, sb);

        String result = sb.toString();
        Assertions.assertTrue(result.contains("&lt;tag&gt;"));
    }

    @Test
    public void testWriteCharactersCharArrayToOutputStream() throws Exception {
        char[] chars = "<test>".toCharArray();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        XmlUtil.writeCharacters(chars, baos);

        String result = baos.toString();
        Assertions.assertTrue(result.contains("&lt;test&gt;"));
    }

    @Test
    public void testWriteCharactersCharArrayWithOffsetToOutputStream() throws Exception {
        char[] chars = "prefix <tag> suffix".toCharArray();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        XmlUtil.writeCharacters(chars, 7, 5, baos);

        String result = baos.toString();
        Assertions.assertTrue(result.contains("&lt;tag&gt;"));
    }

    @Test
    public void testWriteCharactersStringToOutputStream() throws Exception {
        String str = "<data> & value";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        XmlUtil.writeCharacters(str, baos);

        String result = baos.toString();
        Assertions.assertTrue(result.contains("&lt;"));
        Assertions.assertTrue(result.contains("&amp;"));
    }

    @Test
    public void testWriteCharactersStringWithOffsetToOutputStream() throws Exception {
        String str = "prefix <tag> suffix";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        XmlUtil.writeCharacters(str, 7, 5, baos);

        String result = baos.toString();
        Assertions.assertTrue(result.contains("&lt;tag&gt;"));
    }

    @Test
    public void testWriteCharactersCharArrayToWriter() throws Exception {
        char[] chars = "<element>".toCharArray();
        StringWriter writer = new StringWriter();

        XmlUtil.writeCharacters(chars, writer);

        String result = writer.toString();
        Assertions.assertTrue(result.contains("&lt;element&gt;"));
    }

    @Test
    public void testWriteCharactersCharArrayWithOffsetToWriter() throws Exception {
        char[] chars = "prefix <tag> suffix".toCharArray();
        StringWriter writer = new StringWriter();

        XmlUtil.writeCharacters(chars, 7, 5, writer);

        String result = writer.toString();
        Assertions.assertTrue(result.contains("&lt;tag&gt;"));
    }

    @Test
    public void testWriteCharactersStringToWriter() throws Exception {
        String str = "<root> & \"data\"";
        StringWriter writer = new StringWriter();

        XmlUtil.writeCharacters(str, writer);

        String result = writer.toString();
        Assertions.assertTrue(result.contains("&lt;"));
        Assertions.assertTrue(result.contains("&amp;"));
        Assertions.assertTrue(result.contains("&quot;"));
    }

    @Test
    public void testWriteCharactersStringWithOffsetToWriter() throws Exception {
        String str = "prefix <tag> suffix";
        StringWriter writer = new StringWriter();

        XmlUtil.writeCharacters(str, 7, 5, writer);

        String result = writer.toString();
        Assertions.assertTrue(result.contains("&lt;tag&gt;"));
    }

    @Test
    public void testWriteCharactersNullString() throws Exception {
        StringBuilder sb = new StringBuilder();

        XmlUtil.writeCharacters((String) null, sb);

        String result = sb.toString();
        Assertions.assertEquals("null", result);
    }

    @Test
    public void testSAXParserPooling() {
        SAXParser parser1 = XmlUtil.createSAXParser();
        SAXParser parser2 = XmlUtil.createSAXParser();

        Assertions.assertNotNull(parser1);
        Assertions.assertNotNull(parser2);

        XmlUtil.recycleSAXParser(parser1);
        XmlUtil.recycleSAXParser(parser2);

        SAXParser parser3 = XmlUtil.createSAXParser();
        Assertions.assertNotNull(parser3);
    }

    @Test
    public void testContentParserPooling() {
        DocumentBuilder parser1 = XmlUtil.createContentParser();
        DocumentBuilder parser2 = XmlUtil.createContentParser();

        Assertions.assertNotNull(parser1);
        Assertions.assertNotNull(parser2);

        XmlUtil.recycleContentParser(parser1);
        XmlUtil.recycleContentParser(parser2);

        DocumentBuilder parser3 = XmlUtil.createContentParser();
        Assertions.assertNotNull(parser3);
    }

    @Test
    public void testMarshallerCaching() {
        Marshaller m1 = XmlUtil.createMarshaller(Person.class);
        Marshaller m2 = XmlUtil.createMarshaller(Person.class);

        Assertions.assertNotNull(m1);
        Assertions.assertNotNull(m2);
    }

    @Test
    public void testUnmarshallerCaching() {
        Unmarshaller u1 = XmlUtil.createUnmarshaller(Person.class);
        Unmarshaller u2 = XmlUtil.createUnmarshaller(Person.class);

        Assertions.assertNotNull(u1);
        Assertions.assertNotNull(u2);
    }
}
