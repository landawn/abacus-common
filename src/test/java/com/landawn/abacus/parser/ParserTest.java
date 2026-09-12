package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;

public class ParserTest extends TestBase {

    private TestParser parser;
    private TestObject testObject;
    private TestSerializationConfig serConfig;
    private TestDeserializationConfig deserConfig;

    private static class TestSerializationConfig extends SerializationConfig<TestSerializationConfig> {
        private boolean prettyFormat = false;

        public TestSerializationConfig setPrettyFormat(boolean prettyFormat) {
            this.prettyFormat = prettyFormat;
            return this;
        }

        public boolean isPrettyFormat() {
            return prettyFormat;
        }
    }

    private static class TestDeserializationConfig extends DeserializationConfig<TestDeserializationConfig> {
        private boolean ignoreUnknownProperty = false;

        public TestDeserializationConfig setIgnoreUnknownProperty(boolean ignoreUnknownProperty) {
            this.ignoreUnknownProperty = ignoreUnknownProperty;
            return this;
        }

        public boolean isIgnoreUnknownProperty() {
            return ignoreUnknownProperty;
        }
    }

    private static class TestParser implements Parser<TestSerializationConfig, TestDeserializationConfig> {

        @Override
        public String serialize(Object obj) {
            return obj == null ? "null" : obj.toString();
        }

        @Override
        public String serialize(Object obj, TestSerializationConfig config) {
            String result = serialize(obj);
            return config != null && config.isPrettyFormat() ? "PRETTY:" + result : result;
        }

        @Override
        public void serialize(Object obj, File output) throws UncheckedIOException {
            serialize(obj, null, output);
        }

        @Override
        public void serialize(Object obj, TestSerializationConfig config, File output) throws UncheckedIOException {
            try (FileWriter writer = new FileWriter(output)) {
                writer.write(serialize(obj, config));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void serialize(Object obj, OutputStream output) throws UncheckedIOException {
            serialize(obj, null, output);
        }

        @Override
        public void serialize(Object obj, TestSerializationConfig config, OutputStream output) throws UncheckedIOException {
            try {
                output.write(serialize(obj, config).getBytes());
                output.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void serialize(Object obj, Writer output) throws UncheckedIOException {
            serialize(obj, null, output);
        }

        @Override
        public void serialize(Object obj, TestSerializationConfig config, Writer output) throws UncheckedIOException {
            try {
                output.write(serialize(obj, config));
                output.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public <T> T deserialize(String source, Class<? extends T> targetClass) {
            return deserialize(source, null, targetClass);
        }

        @Override
        public <T> T deserialize(String source, TestDeserializationConfig config, Class<? extends T> targetClass) {
            try {
                if (targetClass == String.class) {
                    return (T) source;
                }
                T instance = targetClass.getDeclaredConstructor().newInstance();
                if (instance instanceof TestObject) {
                    ((TestObject) instance).setValue(source);
                }
                return instance;
            } catch (Exception e) {
                throw new RuntimeException("Deserialization failed", e);
            }
        }

        @Override
        public <T> T deserialize(File source, Class<? extends T> targetClass) throws UncheckedIOException {
            return deserialize(source, null, targetClass);
        }

        @Override
        public <T> T deserialize(File source, TestDeserializationConfig config, Class<? extends T> targetClass) throws UncheckedIOException {
            try {
                return deserialize(new String(java.nio.file.Files.readAllBytes(source.toPath())), config, targetClass);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public <T> T deserialize(InputStream source, Class<? extends T> targetClass) throws UncheckedIOException {
            return deserialize(source, null, targetClass);
        }

        @Override
        public <T> T deserialize(InputStream source, TestDeserializationConfig config, Class<? extends T> targetClass) throws UncheckedIOException {
            try {
                return deserialize(new String(source.readAllBytes()), config, targetClass);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public <T> T deserialize(Reader source, Class<? extends T> targetClass) throws UncheckedIOException {
            return deserialize(source, null, targetClass);
        }

        @Override
        public <T> T deserialize(Reader source, TestDeserializationConfig config, Class<? extends T> targetClass) throws UncheckedIOException {
            try {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int read;
                while ((read = source.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
                return deserialize(sb.toString(), config, targetClass);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public <T> T deserialize(String source, Type<? extends T> targetType) {
            return null;
        }

        @Override
        public <T> T deserialize(String source, TestDeserializationConfig config, Type<? extends T> targetType) {
            return deserialize(source, config, (Class<? extends T>) targetType.javaType());
        }

        @Override
        public <T> T deserialize(File source, Type<? extends T> targetType) throws UncheckedIOException {
            return deserialize(source, null, targetType);
        }

        @Override
        public <T> T deserialize(File source, TestDeserializationConfig config, Type<? extends T> targetType) throws UncheckedIOException {
            return deserialize(source, config, (Class<? extends T>) targetType.javaType());
        }

        @Override
        public <T> T deserialize(InputStream source, Type<? extends T> targetType) throws UncheckedIOException {
            return deserialize(source, null, targetType);
        }

        @Override
        public <T> T deserialize(InputStream source, TestDeserializationConfig config, Type<? extends T> targetType) throws UncheckedIOException {
            return deserialize(source, config, (Class<? extends T>) targetType.javaType());
        }

        @Override
        public <T> T deserialize(Reader source, Type<? extends T> targetType) throws UncheckedIOException {
            return deserialize(source, null, targetType);
        }

        @Override
        public <T> T deserialize(Reader source, TestDeserializationConfig config, Type<? extends T> targetType) throws UncheckedIOException {
            return deserialize(source, config, (Class<? extends T>) targetType.javaType());
        }
    }

    public static class TestObject {
        private String value = "defaultValue";

        public TestObject() {
        }

        public TestObject(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @BeforeEach
    public void setUp() {
        parser = new TestParser();
        testObject = new TestObject("testValue");
        serConfig = new TestSerializationConfig();
        deserConfig = new TestDeserializationConfig();
    }

    @Test
    public void testSerialize() {
        assertEquals("testValue", parser.serialize(testObject));
        assertEquals("null", parser.serialize(null));
        assertEquals("123", parser.serialize(123));
        assertEquals("true", parser.serialize(true));
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        assertTrue(parser.serialize(list).contains("a"));

        serConfig.setPrettyFormat(true);
        assertEquals("PRETTY:testValue", parser.serialize(testObject, serConfig));
        assertEquals("PRETTY:null", parser.serialize(null, serConfig));
        assertEquals("testValue", parser.serialize(testObject, (TestSerializationConfig) null));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(testObject, baos);
        assertEquals("testValue", baos.toString());
        parser.serialize(null, baos);
        parser.serialize(new TestObject("value1"), baos);
        parser.serialize(new TestObject("value2"), baos);
        assertEquals("testValuenullvalue1value2", baos.toString());
        baos.reset();
        parser.serialize(testObject, serConfig, baos);
        assertEquals("PRETTY:testValue", baos.toString());
        baos.reset();
        parser.serialize(testObject, null, baos);
        assertEquals("testValue", baos.toString());

        StringWriter writer = new StringWriter();
        parser.serialize(testObject, writer);
        assertEquals("testValue", writer.toString());
        writer = new StringWriter();
        parser.serialize(null, writer);
        assertEquals("null", writer.toString());
        writer = new StringWriter();
        parser.serialize(new TestObject("value1"), writer);
        parser.serialize(new TestObject("value2"), writer);
        assertEquals("value1value2", writer.toString());
        writer = new StringWriter();
        parser.serialize(testObject, serConfig, writer);
        assertEquals("PRETTY:testValue", writer.toString());
        writer = new StringWriter();
        parser.serialize(testObject, null, writer);
        assertEquals("testValue", writer.toString());
    }

    @Test
    public void testSerializeToFile() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();

        parser.serialize(testObject, tempFile);
        assertEquals("testValue", new String(java.nio.file.Files.readAllBytes(tempFile.toPath())));

        parser.serialize(null, tempFile);
        assertEquals("null", new String(java.nio.file.Files.readAllBytes(tempFile.toPath())));

        parser.serialize(new TestObject("first"), tempFile);
        parser.serialize(new TestObject("second"), tempFile);
        assertEquals("second", new String(java.nio.file.Files.readAllBytes(tempFile.toPath())));

        serConfig.setPrettyFormat(true);
        parser.serialize(testObject, serConfig, tempFile);
        assertEquals("PRETTY:testValue", new String(java.nio.file.Files.readAllBytes(tempFile.toPath())));

        parser.serialize(testObject, null, tempFile);
        assertEquals("testValue", new String(java.nio.file.Files.readAllBytes(tempFile.toPath())));
    }

    @Test
    public void testDeserialize() {
        assertEquals("testString", parser.deserialize("testString", String.class));
        deserConfig.setIgnoreUnknownProperty(true);
        assertEquals("stringValue", parser.deserialize("stringValue", deserConfig, String.class));
        assertEquals("readerString", parser.deserialize(new StringReader("readerString"), String.class));

        TestObject result = parser.deserialize("myValue", TestObject.class);
        assertEquals("myValue", result.getValue());
        assertEquals("", parser.deserialize("", TestObject.class).getValue());
        assertEquals("configValue", parser.deserialize("configValue", deserConfig, TestObject.class).getValue());
        assertEquals("nullConfigValue", parser.deserialize("nullConfigValue", null, TestObject.class).getValue());

        assertEquals("streamValue", parser.deserialize(new ByteArrayInputStream("streamValue".getBytes()), TestObject.class).getValue());
        assertEquals("", parser.deserialize(new ByteArrayInputStream("".getBytes()), TestObject.class).getValue());
        assertEquals("streamString", parser.deserialize(new ByteArrayInputStream("streamString".getBytes()), String.class));
        assertEquals("streamConfigValue",
                parser.deserialize(new ByteArrayInputStream("streamConfigValue".getBytes()), deserConfig, TestObject.class).getValue());
        assertEquals("streamNullConfig", parser.deserialize(new ByteArrayInputStream("streamNullConfig".getBytes()), null, TestObject.class).getValue());

        assertEquals("readerValue", parser.deserialize(new StringReader("readerValue"), TestObject.class).getValue());
        assertEquals("", parser.deserialize(new StringReader(""), TestObject.class).getValue());
        StringBuilder large = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            large.append("x");
        }
        assertEquals(1000, parser.deserialize(new StringReader(large.toString()), TestObject.class).getValue().length());
        assertEquals("readerConfigValue", parser.deserialize(new StringReader("readerConfigValue"), deserConfig, TestObject.class).getValue());
        assertEquals("readerNullConfig", parser.deserialize(new StringReader("readerNullConfig"), null, TestObject.class).getValue());
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();

        java.nio.file.Files.write(tempFile.toPath(), "fileValue".getBytes());
        assertEquals("fileValue", parser.deserialize(tempFile, TestObject.class).getValue());

        java.nio.file.Files.write(tempFile.toPath(), "".getBytes());
        assertEquals("", parser.deserialize(tempFile, TestObject.class).getValue());

        java.nio.file.Files.write(tempFile.toPath(), "stringFromFile".getBytes());
        assertEquals("stringFromFile", parser.deserialize(tempFile, String.class));

        deserConfig.setIgnoreUnknownProperty(true);
        java.nio.file.Files.write(tempFile.toPath(), "fileConfigValue".getBytes());
        assertEquals("fileConfigValue", parser.deserialize(tempFile, deserConfig, TestObject.class).getValue());

        java.nio.file.Files.write(tempFile.toPath(), "nullConfigFile".getBytes());
        assertEquals("nullConfigFile", parser.deserialize(tempFile, null, TestObject.class).getValue());
    }

    @Test
    public void testRoundTrip() throws IOException {
        TestObject deserialized = parser.deserialize(parser.serialize(testObject), TestObject.class);
        assertEquals(testObject.getValue(), deserialized.getValue());

        serConfig.setPrettyFormat(true);
        String serialized = parser.serialize(testObject, serConfig);
        assertTrue(serialized.startsWith("PRETTY:"));
        deserConfig.setIgnoreUnknownProperty(true);
        assertNotNull(parser.deserialize(serialized, deserConfig, TestObject.class));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(testObject, baos);
        assertEquals(testObject.getValue(), parser.deserialize(new ByteArrayInputStream(baos.toByteArray()), TestObject.class).getValue());

        StringWriter writer = new StringWriter();
        parser.serialize(testObject, writer);
        assertEquals(testObject.getValue(), parser.deserialize(new StringReader(writer.toString()), TestObject.class).getValue());

        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();
        parser.serialize(testObject, tempFile);
        assertEquals(testObject.getValue(), parser.deserialize(tempFile, TestObject.class).getValue());
    }

    // ---------------------------------------------------------------------------------------------
    // Review fixes 2026-09-06, P7-06: the Parser javadoc promised NullPointerException for a null
    // source/class; the implementations throw IllegalArgumentException (N.checkArgNotNull). Pin it.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void reviewFixes20260906_P7_06_nullSourceOrClassThrowsIllegalArgumentException() {
        final JsonParser jsonParser = ParserFactory.createJsonParser();

        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize((File) null, java.util.Map.class));
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize((InputStream) null, java.util.Map.class));
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize((Reader) null, java.util.Map.class));
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize((File) null, Type.of(java.util.Map.class)));
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize((InputStream) null, Type.of(java.util.Map.class)));
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize((Reader) null, Type.of(java.util.Map.class)));
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize((File) null, null, java.util.Map.class));
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize((InputStream) null, null, Type.of(java.util.Map.class)));

        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize("{}", (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize("{}", null, (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize(new StringReader("{}"), (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize(new ByteArrayInputStream(new byte[0]), (Class<?>) null));

        // P2-15: a null Type is rejected the same way as a null Class
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize("{}", (Type<?>) null));
        assertThrows(IllegalArgumentException.class, () -> jsonParser.deserialize(new StringReader("{}"), (Type<?>) null));

        final XmlParser xmlParser = ParserFactory.createXmlParser();

        assertThrows(IllegalArgumentException.class, () -> xmlParser.deserialize((File) null, java.util.Map.class));
        assertThrows(IllegalArgumentException.class, () -> xmlParser.deserialize((InputStream) null, java.util.Map.class));
        assertThrows(IllegalArgumentException.class, () -> xmlParser.deserialize((File) null, Type.of(java.util.Map.class)));
        assertThrows(IllegalArgumentException.class, () -> xmlParser.deserialize("<map></map>", (Class<?>) null));
    }

}
