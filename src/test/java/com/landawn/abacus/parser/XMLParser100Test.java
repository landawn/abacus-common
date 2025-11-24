package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("new-test")
public class XMLParser100Test extends TestBase {

    private TestXMLParser parser;

    private static class TestXMLParser implements XMLParser {
        @Override
        public String serialize(Object obj) {
            return "<test/>";
        }

        @Override
        public String serialize(Object obj, XMLSerializationConfig config) {
            return serialize(obj);
        }

        @Override
        public void serialize(Object obj, File output) {
            try (FileWriter writer = new FileWriter(output)) {
                writer.write(serialize(obj));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Object obj, XMLSerializationConfig config, File output) {
            serialize(obj, output);
        }

        @Override
        public void serialize(Object obj, OutputStream output) {
            try {
                output.write(serialize(obj).getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Object obj, XMLSerializationConfig config, OutputStream output) {
            serialize(obj, output);
        }

        @Override
        public void serialize(Object obj, Writer output) {
            try {
                output.write(serialize(obj));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Object obj, XMLSerializationConfig config, Writer output) {
            serialize(obj, output);
        }

        @Override
        public <T> T deserialize(String source, Class<? extends T> targetClass) {
            try {
                return targetClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public <T> T deserialize(String source, XMLDeserializationConfig config, Class<? extends T> targetClass) {
            return deserialize(source, targetClass);
        }

        @Override
        public <T> T deserialize(File source, Class<? extends T> targetClass) {
            return deserialize("", targetClass);
        }

        @Override
        public <T> T deserialize(File source, XMLDeserializationConfig config, Class<? extends T> targetClass) {
            return deserialize(source, targetClass);
        }

        @Override
        public <T> T deserialize(InputStream source, Class<? extends T> targetClass) {
            return deserialize("", targetClass);
        }

        @Override
        public <T> T deserialize(InputStream source, XMLDeserializationConfig config, Class<? extends T> targetClass) {
            return deserialize(source, targetClass);
        }

        @Override
        public <T> T deserialize(Reader source, Class<? extends T> targetClass) {
            return deserialize("", targetClass);
        }

        @Override
        public <T> T deserialize(Reader source, XMLDeserializationConfig config, Class<? extends T> targetClass) {
            return deserialize(source, targetClass);
        }

        @Override
        public <T> T deserialize(Node source, Class<? extends T> targetClass) {
            return deserialize("", targetClass);
        }

        @Override
        public <T> T deserialize(Node source, XMLDeserializationConfig config, Class<? extends T> targetClass) {
            return deserialize(source, targetClass);
        }

        @Override
        public <T> T deserialize(InputStream source, XMLDeserializationConfig config, Map<String, Type<?>> nodeClasses) {
            if (nodeClasses.containsKey("test")) {
                return (T) deserialize("", nodeClasses.get("test"));
            }
            return null;
        }

        @Override
        public <T> T deserialize(Reader source, XMLDeserializationConfig config, Map<String, Type<?>> nodeClasses) {
            if (nodeClasses.containsKey("test")) {
                return (T) deserialize("", nodeClasses.get("test"));
            }
            return null;
        }

        @Override
        public <T> T deserialize(Node source, XMLDeserializationConfig config, Map<String, Type<?>> nodeClasses) {
            if (nodeClasses.containsKey("test")) {
                return (T) deserialize("", nodeClasses.get("test"));
            }
            return null;
        }

        @Override
        public <T> T deserialize(String source, Type<? extends T> targetType) {
            return deserialize(source, targetType.clazz());
        }

        @Override
        public <T> T deserialize(String source, XMLDeserializationConfig config, Type<? extends T> targetType) {
            return deserialize(source, config, targetType.clazz());
        }

        @Override
        public <T> T deserialize(File source, Type<? extends T> targetType) {
            return deserialize(source, targetType.clazz());
        }

        @Override
        public <T> T deserialize(File source, XMLDeserializationConfig config, Type<? extends T> targetType) {
            return deserialize(source, config, targetType.clazz());
        }

        @Override
        public <T> T deserialize(InputStream source, Type<? extends T> targetType) {
            return deserialize(source, targetType.clazz());
        }

        @Override
        public <T> T deserialize(InputStream source, XMLDeserializationConfig config, Type<? extends T> targetType) {
            return deserialize(source, config, targetType.clazz());
        }

        @Override
        public <T> T deserialize(Reader source, Type<? extends T> targetType) {
            return deserialize(source, targetType.clazz());
        }

        @Override
        public <T> T deserialize(Reader source, XMLDeserializationConfig config, Type<? extends T> targetType) {
            return deserialize(source, config, targetType.clazz());
        }

        @Override
        public <T> T deserialize(Node source, Type<? extends T> targetType) {
            return deserialize(source, targetType.clazz());
        }

        @Override
        public <T> T deserialize(Node source, XMLDeserializationConfig config, Type<? extends T> targetType) {
            return deserialize(source, config, targetType.clazz());
        }

        @Override
        public <T> T deserialize(File source, XMLDeserializationConfig config, Map<String, Type<?>> nodeTypes) {
            return deserialize(source, config, nodeTypes);
        }
    }

    private static class TestObject {
        private String value;
    }

    @BeforeEach
    public void setUp() {
        parser = new TestXMLParser();
    }

    @Test
    public void testDeserializeFromNode() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream("<test/>".getBytes()));
        Node node = doc.getFirstChild();

        TestObject result = parser.deserialize(node, TestObject.class);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeFromNodeWithConfig() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream("<test/>".getBytes()));
        Node node = doc.getFirstChild();

        XMLDeserializationConfig config = new XMLDeserializationConfig();
        TestObject result = parser.deserialize(node, config, TestObject.class);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeWithNodeClassesFromInputStream() {
        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("test", Type.of(TestObject.class));

        ByteArrayInputStream bais = new ByteArrayInputStream("<test/>".getBytes());
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        TestObject result = parser.deserialize(bais, config, nodeClasses);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeWithNodeClassesFromReader() {
        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("test", Type.of(TestObject.class));

        StringReader reader = new StringReader("<test/>");
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        TestObject result = parser.deserialize(reader, config, nodeClasses);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeWithNodeClassesFromNode() throws Exception {
        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("test", Type.of(TestObject.class));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream("<test/>".getBytes()));
        Node node = doc.getFirstChild();

        XMLDeserializationConfig config = new XMLDeserializationConfig();
        TestObject result = parser.deserialize(node, config, nodeClasses);
        assertNotNull(result);
    }
}
