package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedException;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;

public class XmlUtil100Test extends TestBase {

    @Test
    public void testMarshal() {
        TestPerson person = new TestPerson("John", 30);
        
        String xml = XmlUtil.marshal(person);
        
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("John"));
        Assertions.assertTrue(xml.contains("30"));
    }

    @Test
    public void testUnmarshal() {
        TestPerson person = new TestPerson("Jane", 25);
        String xml = XmlUtil.marshal(person);
        
        TestPerson restored = XmlUtil.unmarshal(TestPerson.class, xml);
        
        Assertions.assertNotNull(restored);
        Assertions.assertEquals("Jane", restored.getName());
        Assertions.assertEquals(25, restored.getAge());
    }

    @Test
    public void testCreateMarshallerWithContextPath() {
        String contextPath = "com.landawn.abacus.util";
        
        // Marshaller marshaller = XmlUtil.createMarshaller(contextPath); 
        // Assertions.assertNotNull(marshaller);
        
        Assertions.assertThrows(UncheckedException.class, () -> XmlUtil.createMarshaller(contextPath));
    }

    @Test
    public void testCreateMarshallerWithClass() {
        Marshaller marshaller = XmlUtil.createMarshaller(TestPerson.class);
        
        Assertions.assertNotNull(marshaller);
    }

    @Test
    public void testCreateUnmarshallerWithContextPath() {
        String contextPath = "com.landawn.abacus.util";
        
        // Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(contextPath);

        // Assertions.assertNotNull(unmarshaller);

        Assertions.assertThrows(UncheckedException.class, () -> XmlUtil.createUnmarshaller(contextPath));
    }

    @Test
    public void testCreateUnmarshallerWithClass() {
        Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(TestPerson.class);
        
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
    }

    @Test
    public void testCreateContentParser() {
        DocumentBuilder parser = XmlUtil.createContentParser();
        
        Assertions.assertNotNull(parser);
        
        // Test recycling
        XmlUtil.recycleContentParser(parser);
    }

    @Test
    public void testRecycleContentParserWithNull() {
        // Should not throw exception
        XmlUtil.recycleContentParser(null);
    }

    @Test
    public void testCreateSAXParser() {
        SAXParser parser = XmlUtil.createSAXParser();
        
        Assertions.assertNotNull(parser);
        
        // Test recycling
        XmlUtil.recycleSAXParser(parser);
    }

    @Test
    public void testRecycleSAXParserWithNull() {
        // Should not throw exception
        XmlUtil.recycleSAXParser(null);
    }

    @Test
    public void testCreateXMLStreamReaderFromReader() {
        StringReader reader = new StringReader("<root>test</root>");
        
        XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(reader);
        
        Assertions.assertNotNull(xmlReader);
    }

    @Test
    public void testCreateXMLStreamReaderFromInputStream() throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream("<root>test</root>".getBytes());
        
        XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(is);
        
        Assertions.assertNotNull(xmlReader);
    }

    @Test
    public void testCreateXMLStreamReaderFromInputStreamWithEncoding() throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream("<root>test</root>".getBytes("UTF-8"));
        
        XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(is, "UTF-8");
        
        Assertions.assertNotNull(xmlReader);
    }

    @Test
    public void testCreateFilteredStreamReader() throws Exception {
        StringReader reader = new StringReader("<root><child>test</child></root>");
        XMLStreamReader source = XmlUtil.createXMLStreamReader(reader);
        
        XMLStreamReader filtered = XmlUtil.createFilteredStreamReader(source, r -> r.isStartElement());
        
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
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        XMLStreamWriter xmlWriter = XmlUtil.createXMLStreamWriter(os);
        
        Assertions.assertNotNull(xmlWriter);
    }

    @Test
    public void testCreateXMLStreamWriterFromOutputStreamWithEncoding() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        XMLStreamWriter xmlWriter = XmlUtil.createXMLStreamWriter(os, "UTF-8");
        
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
        root.setTextContent("test");
        doc.appendChild(root);
        
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        
        XmlUtil.transform(doc, tempFile);
        
        Assertions.assertTrue(tempFile.exists());
        Assertions.assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testTransformToOutputStream() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        root.setTextContent("test");
        doc.appendChild(root);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        XmlUtil.transform(doc, os);
        
        String result = os.toString();
        Assertions.assertTrue(result.contains("<root>"));
        Assertions.assertTrue(result.contains("test"));
    }

    @Test
    public void testTransformToWriter() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        root.setTextContent("test");
        doc.appendChild(root);
        
        StringWriter writer = new StringWriter();
        
        XmlUtil.transform(doc, writer);
        
        String result = writer.toString();
        Assertions.assertTrue(result.contains("<root>"));
        Assertions.assertTrue(result.contains("test"));
    }

    @Test
    public void testXmlEncode() {
        TestPerson person = new TestPerson("John", 30);
        
        String xml = XmlUtil.xmlEncode(person);
        
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("java"));
    }

    @Test
    public void testXmlDecode() {
        TestPerson person = new TestPerson("John", 30);
        String xml = XmlUtil.xmlEncode(person);
        
        TestPerson decoded = XmlUtil.xmlDecode(xml);
        
        Assertions.assertNotNull(decoded);
        Assertions.assertEquals("John", decoded.getName());
        Assertions.assertEquals(30, decoded.getAge());
    }

    @Test
    public void testGetElementsByTagName() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        Element child1 = doc.createElement("child");
        Element child2 = doc.createElement("child");
        Element grandchild = doc.createElement("child");
        child1.appendChild(grandchild);
        root.appendChild(child1);
        root.appendChild(child2);
        doc.appendChild(root);
        
        List<Element> children = XmlUtil.getElementsByTagName(root, "child");
        
        Assertions.assertEquals(2, children.size());
    }

    @Test
    public void testGetNodesByName() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        Element price1 = doc.createElement("price");
        Element price2 = doc.createElement("price");
        root.appendChild(price1);
        root.appendChild(price2);
        doc.appendChild(root);
        
        List<Node> prices = XmlUtil.getNodesByName(doc, "price");
        
        Assertions.assertEquals(2, prices.size());
    }

    @Test
    public void testGetNextNodeByName() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        Element other = doc.createElement("other");
        Element price = doc.createElement("price");
        price.setTextContent("19.99");
        root.appendChild(other);
        root.appendChild(price);
        doc.appendChild(root);
        
        Node priceNode = XmlUtil.getNextNodeByName(doc, "price");
        
        Assertions.assertNotNull(priceNode);
        Assertions.assertEquals("19.99", priceNode.getTextContent());
    }

    @Test
    public void testGetNextNodeByNameNotFound() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        doc.appendChild(root);
        
        Node notFound = XmlUtil.getNextNodeByName(doc, "nonexistent");
        
        Assertions.assertNull(notFound);
    }

    @Test
    public void testGetAttribute() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element element = doc.createElement("element");
        element.setAttribute("id", "123");
        element.setAttribute("class", "test-class");
        
        String id = XmlUtil.getAttribute(element, "id");
        String className = XmlUtil.getAttribute(element, "class");
        String nonExistent = XmlUtil.getAttribute(element, "nonexistent");
        
        Assertions.assertEquals("123", id);
        Assertions.assertEquals("test-class", className);
        Assertions.assertNull(nonExistent);
    }

    @Test
    public void testGetAttributeNoAttributes() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Node textNode = doc.createTextNode("text");
        
        String result = XmlUtil.getAttribute(textNode, "any");
        
        Assertions.assertNull(result);
    }

    @Test
    public void testReadAttributes() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element element = doc.createElement("element");
        element.setAttribute("id", "123");
        element.setAttribute("name", "test");
        element.setAttribute("active", "true");
        
        Map<String, String> attrs = XmlUtil.readAttributes(element);
        
        Assertions.assertEquals(3, attrs.size());
        Assertions.assertEquals("123", attrs.get("id"));
        Assertions.assertEquals("test", attrs.get("name"));
        Assertions.assertEquals("true", attrs.get("active"));
    }

    @Test
    public void testReadElement() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element person = doc.createElement("person");
        person.setAttribute("age", "30");
        Element name = doc.createElement("name");
        name.setTextContent("John");
        person.appendChild(name);
        
        Map<String, String> data = XmlUtil.readElement(person);
        
        Assertions.assertEquals("30", data.get("age"));
        Assertions.assertEquals("John", data.get("person.name"));
    }

    @Test
    public void testIsTextElement() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        
        Element textElem = doc.createElement("name");
        textElem.setTextContent("John");
        
        Element parentElem = doc.createElement("person");
        Element child = doc.createElement("child");
        parentElem.appendChild(child);
        
        Assertions.assertTrue(XmlUtil.isTextElement(textElem));
        Assertions.assertFalse(XmlUtil.isTextElement(parentElem));
    }

    @Test
    public void testGetTextContent() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element elem = doc.createElement("message");
        elem.setTextContent("  Hello World  ");
        
        String content = XmlUtil.getTextContent(elem);
        
        Assertions.assertEquals("  Hello World  ", content);
    }

    @Test
    public void testGetTextContentWithIgnoreWhiteChar() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element elem = doc.createElement("message");
        elem.setTextContent("  Hello\n\tWorld  ");
        
        String content = XmlUtil.getTextContent(elem, true);
        String rawContent = XmlUtil.getTextContent(elem, false);
        
        Assertions.assertEquals("Hello World", content);
        Assertions.assertEquals("  Hello\n\tWorld  ", rawContent);
    }

    @Test
    public void testWriteCharactersCharArrayToStringBuilder() throws Exception {
        char[] chars = "Hello <world> & \"friends\"".toCharArray();
        StringBuilder sb = new StringBuilder();
        
        XmlUtil.writeCharacters(chars, sb);
        
        Assertions.assertEquals("Hello &lt;world&gt; &amp; &quot;friends&quot;", sb.toString());
    }

    @Test
    public void testWriteCharactersCharArrayPartialToStringBuilder() throws Exception {
        char[] chars = "Hello <world>".toCharArray();
        StringBuilder sb = new StringBuilder();
        
        XmlUtil.writeCharacters(chars, 6, 7, sb);
        
        Assertions.assertEquals("&lt;world&gt;", sb.toString());
    }

    @Test
    public void testWriteCharactersStringToStringBuilder() throws Exception {
        String str = "Hello <world>";
        StringBuilder sb = new StringBuilder();
        
        XmlUtil.writeCharacters(str, sb);
        
        Assertions.assertEquals("Hello &lt;world&gt;", sb.toString());
    }

    @Test
    public void testWriteCharactersStringPartialToStringBuilder() throws Exception {
        String str = "Hello <world>";
        StringBuilder sb = new StringBuilder();
        
        XmlUtil.writeCharacters(str, 6, 7, sb);
        
        Assertions.assertEquals("&lt;world&gt;", sb.toString());
    }

    @Test
    public void testWriteCharactersNullStringToStringBuilder() throws Exception {
        String str = null;
        StringBuilder sb = new StringBuilder();
        
        XmlUtil.writeCharacters(str, sb);
        
        Assertions.assertEquals("null", sb.toString());
    }

    @Test
    public void testWriteCharactersCharArrayToOutputStream() throws Exception {
        char[] chars = "Hello <world>".toCharArray();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        XmlUtil.writeCharacters(chars, os);
        
        Assertions.assertEquals("Hello &lt;world&gt;", os.toString());
    }

    @Test
    public void testWriteCharactersCharArrayPartialToOutputStream() throws Exception {
        char[] chars = "Hello <world>".toCharArray();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        XmlUtil.writeCharacters(chars, 6, 7, os);
        
        Assertions.assertEquals("&lt;world&gt;", os.toString());
    }

    @Test
    public void testWriteCharactersStringToOutputStream() throws Exception {
        String str = "Hello <world>";
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        XmlUtil.writeCharacters(str, os);
        
        Assertions.assertEquals("Hello &lt;world&gt;", os.toString());
    }

    @Test
    public void testWriteCharactersStringPartialToOutputStream() throws Exception {
        String str = "Hello <world>";
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        XmlUtil.writeCharacters(str, 6, 7, os);
        
        Assertions.assertEquals("&lt;world&gt;", os.toString());
    }

    @Test
    public void testWriteCharactersCharArrayToWriter() throws Exception {
        char[] chars = "Hello <world>".toCharArray();
        StringWriter writer = new StringWriter();
        
        XmlUtil.writeCharacters(chars, writer);
        
        Assertions.assertEquals("Hello &lt;world&gt;", writer.toString());
    }

    @Test
    public void testWriteCharactersCharArrayPartialToWriter() throws Exception {
        char[] chars = "Hello <world>".toCharArray();
        StringWriter writer = new StringWriter();
        
        XmlUtil.writeCharacters(chars, 6, 7, writer);
        
        Assertions.assertEquals("&lt;world&gt;", writer.toString());
    }

    @Test
    public void testWriteCharactersStringToWriter() throws Exception {
        String str = "Hello <world>";
        StringWriter writer = new StringWriter();
        
        XmlUtil.writeCharacters(str, writer);
        
        Assertions.assertEquals("Hello &lt;world&gt;", writer.toString());
    }

    @Test
    public void testWriteCharactersStringPartialToWriter() throws Exception {
        String str = "Hello <world>";
        StringWriter writer = new StringWriter();
        
        XmlUtil.writeCharacters(str, 6, 7, writer);
        
        Assertions.assertEquals("&lt;world&gt;", writer.toString());
    }

    // Test JAXB class
    @XmlRootElement
    public static class TestPerson {
        private String name;
        private int age;
        
        public TestPerson() {}
        
        public TestPerson(String name, int age) {
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
    }
}