package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("2025")
public class XmlParserImpl2025Test extends TestBase {

    @TempDir
    Path tempDir;

    public static class Utf16Bean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Test
    public void test_constructor_default() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_constructor_withConfig() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlSerializationConfig xsc = new XmlSerializationConfig();
            XmlDeserializationConfig xdc = new XmlDeserializationConfig();
            XmlParser parser = new XmlParserImpl(XmlParserType.StAX, xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_serialize_string() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
            String xml = parser.serialize("test");
            assertNotNull(xml);
        }
    }

    @Test
    public void test_deserialize_file_honors_xml_encoding_utf16() throws Exception {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
            File file = tempDir.resolve("utf16.xml").toFile();
            Files.writeString(file.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-16\"?><bean><name>\u4f60\u597d</name></bean>", StandardCharsets.UTF_16);

            Utf16Bean bean = parser.deserialize(file, Utf16Bean.class);
            assertEquals("\u4f60\u597d", bean.getName());
        }
    }

    @Test
    public void test_deserialize_file_with_node_types_honors_xml_encoding_utf16() throws Exception {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
            File file = tempDir.resolve("utf16-node-types.xml").toFile();
            Files.writeString(file.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-16\"?><bean><name>\u4e16\u754c</name></bean>", StandardCharsets.UTF_16);

            Map<String, Type<?>> nodeTypes = new HashMap<>();
            nodeTypes.put("bean", Type.of(Utf16Bean.class));

            Utf16Bean bean = parser.deserialize(file, null, nodeTypes);
            assertEquals("\u4e16\u754c", bean.getName());
        }
    }
}
