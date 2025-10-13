package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

@Tag("2025")
public class Parser2025Test extends TestBase {

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
            if (obj == null) {
                return "null";
            }
            return obj.toString();
        }

        @Override
        public String serialize(Object obj, TestSerializationConfig config) {
            String result = serialize(obj);
            if (config != null && config.isPrettyFormat()) {
                result = "PRETTY:" + result;
            }
            return result;
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
                String serialized = serialize(obj, config);
                output.write(serialized.getBytes());
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
                String serialized = serialize(obj, config);
                output.write(serialized);
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
                String content = new String(java.nio.file.Files.readAllBytes(source.toPath()));
                return deserialize(content, config, targetClass);
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
                byte[] bytes = source.readAllBytes();
                String content = new String(bytes);
                return deserialize(content, config, targetClass);
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
    public void testSerializeToString() {
        String result = parser.serialize(testObject);
        assertEquals("testValue", result);
    }

    @Test
    public void testSerializeToStringWithNull() {
        String result = parser.serialize(null);
        assertEquals("null", result);
    }

    @Test
    public void testSerializeToStringWithDifferentTypes() {
        assertEquals("123", parser.serialize(123));
        assertEquals("true", parser.serialize(true));

        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        String listResult = parser.serialize(list);
        assertNotNull(listResult);
        assertTrue(listResult.contains("a"));
    }

    @Test
    public void testSerializeToStringWithConfig() {
        serConfig.setPrettyFormat(true);
        String result = parser.serialize(testObject, serConfig);
        assertEquals("PRETTY:testValue", result);
    }

    @Test
    public void testSerializeToStringWithConfigNull() {
        String result = parser.serialize(testObject, (TestSerializationConfig) null);
        assertEquals("testValue", result);
    }

    @Test
    public void testSerializeToStringWithConfigAndNullObject() {
        serConfig.setPrettyFormat(true);
        String result = parser.serialize(null, serConfig);
        assertEquals("PRETTY:null", result);
    }

    @Test
    public void testSerializeToFile() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();

        parser.serialize(testObject, tempFile);

        String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
        assertEquals("testValue", content);
    }

    @Test
    public void testSerializeToFileWithNull() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();

        parser.serialize(null, tempFile);

        String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
        assertEquals("null", content);
    }

    @Test
    public void testSerializeToFileOverwritesExisting() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();

        parser.serialize(new TestObject("first"), tempFile);
        parser.serialize(new TestObject("second"), tempFile);

        String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
        assertEquals("second", content);
    }

    @Test
    public void testSerializeToFileWithConfig() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();

        serConfig.setPrettyFormat(true);
        parser.serialize(testObject, serConfig, tempFile);

        String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
        assertEquals("PRETTY:testValue", content);
    }

    @Test
    public void testSerializeToFileWithConfigNull() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();

        parser.serialize(testObject, null, tempFile);

        String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
        assertEquals("testValue", content);
    }

    @Test
    public void testSerializeToOutputStream() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(testObject, baos);

        assertEquals("testValue", baos.toString());
    }

    @Test
    public void testSerializeToOutputStreamWithNull() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(null, baos);

        assertEquals("null", baos.toString());
    }

    @Test
    public void testSerializeToOutputStreamMultipleTimes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(new TestObject("value1"), baos);
        parser.serialize(new TestObject("value2"), baos);

        assertEquals("value1value2", baos.toString());
    }

    @Test
    public void testSerializeToOutputStreamWithConfig() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serConfig.setPrettyFormat(true);
        parser.serialize(testObject, serConfig, baos);

        assertEquals("PRETTY:testValue", baos.toString());
    }

    @Test
    public void testSerializeToOutputStreamWithConfigNull() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(testObject, null, baos);

        assertEquals("testValue", baos.toString());
    }

    @Test
    public void testSerializeToWriter() {
        StringWriter writer = new StringWriter();
        parser.serialize(testObject, writer);

        assertEquals("testValue", writer.toString());
    }

    @Test
    public void testSerializeToWriterWithNull() {
        StringWriter writer = new StringWriter();
        parser.serialize(null, writer);

        assertEquals("null", writer.toString());
    }

    @Test
    public void testSerializeToWriterMultipleTimes() {
        StringWriter writer = new StringWriter();
        parser.serialize(new TestObject("value1"), writer);
        parser.serialize(new TestObject("value2"), writer);

        assertEquals("value1value2", writer.toString());
    }

    @Test
    public void testSerializeToWriterWithConfig() {
        StringWriter writer = new StringWriter();
        serConfig.setPrettyFormat(true);
        parser.serialize(testObject, serConfig, writer);

        assertEquals("PRETTY:testValue", writer.toString());
    }

    @Test
    public void testSerializeToWriterWithConfigNull() {
        StringWriter writer = new StringWriter();
        parser.serialize(testObject, null, writer);

        assertEquals("testValue", writer.toString());
    }

    @Test
    public void testDeserializeFromString() {
        TestObject result = parser.deserialize("myValue", TestObject.class);
        assertNotNull(result);
        assertEquals("myValue", result.getValue());
    }

    @Test
    public void testDeserializeFromStringToString() {
        String result = parser.deserialize("testString", String.class);
        assertEquals("testString", result);
    }

    @Test
    public void testDeserializeFromEmptyString() {
        TestObject result = parser.deserialize("", TestObject.class);
        assertNotNull(result);
        assertEquals("", result.getValue());
    }

    @Test
    public void testDeserializeFromStringWithConfig() {
        deserConfig.setIgnoreUnknownProperty(true);
        TestObject result = parser.deserialize("configValue", deserConfig, TestObject.class);
        assertNotNull(result);
        assertEquals("configValue", result.getValue());
    }

    @Test
    public void testDeserializeFromStringWithConfigNull() {
        TestObject result = parser.deserialize("nullConfigValue", null, TestObject.class);
        assertNotNull(result);
        assertEquals("nullConfigValue", result.getValue());
    }

    @Test
    public void testDeserializeFromStringWithConfigToString() {
        deserConfig.setIgnoreUnknownProperty(true);
        String result = parser.deserialize("stringValue", deserConfig, String.class);
        assertEquals("stringValue", result);
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();
        java.nio.file.Files.write(tempFile.toPath(), "fileValue".getBytes());

        TestObject result = parser.deserialize(tempFile, TestObject.class);
        assertNotNull(result);
        assertEquals("fileValue", result.getValue());
    }

    @Test
    public void testDeserializeFromEmptyFile() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();
        java.nio.file.Files.write(tempFile.toPath(), "".getBytes());

        TestObject result = parser.deserialize(tempFile, TestObject.class);
        assertNotNull(result);
        assertEquals("", result.getValue());
    }

    @Test
    public void testDeserializeFromFileToString() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();
        java.nio.file.Files.write(tempFile.toPath(), "stringFromFile".getBytes());

        String result = parser.deserialize(tempFile, String.class);
        assertEquals("stringFromFile", result);
    }

    @Test
    public void testDeserializeFromFileWithConfig() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();
        java.nio.file.Files.write(tempFile.toPath(), "fileConfigValue".getBytes());

        deserConfig.setIgnoreUnknownProperty(true);
        TestObject result = parser.deserialize(tempFile, deserConfig, TestObject.class);
        assertNotNull(result);
        assertEquals("fileConfigValue", result.getValue());
    }

    @Test
    public void testDeserializeFromFileWithConfigNull() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();
        java.nio.file.Files.write(tempFile.toPath(), "nullConfigFile".getBytes());

        TestObject result = parser.deserialize(tempFile, null, TestObject.class);
        assertNotNull(result);
        assertEquals("nullConfigFile", result.getValue());
    }

    @Test
    public void testDeserializeFromInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream("streamValue".getBytes());
        TestObject result = parser.deserialize(bais, TestObject.class);
        assertNotNull(result);
        assertEquals("streamValue", result.getValue());
    }

    @Test
    public void testDeserializeFromEmptyInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream("".getBytes());
        TestObject result = parser.deserialize(bais, TestObject.class);
        assertNotNull(result);
        assertEquals("", result.getValue());
    }

    @Test
    public void testDeserializeFromInputStreamToString() {
        ByteArrayInputStream bais = new ByteArrayInputStream("streamString".getBytes());
        String result = parser.deserialize(bais, String.class);
        assertEquals("streamString", result);
    }

    @Test
    public void testDeserializeFromInputStreamWithConfig() {
        ByteArrayInputStream bais = new ByteArrayInputStream("streamConfigValue".getBytes());
        deserConfig.setIgnoreUnknownProperty(true);
        TestObject result = parser.deserialize(bais, deserConfig, TestObject.class);
        assertNotNull(result);
        assertEquals("streamConfigValue", result.getValue());
    }

    @Test
    public void testDeserializeFromInputStreamWithConfigNull() {
        ByteArrayInputStream bais = new ByteArrayInputStream("streamNullConfig".getBytes());
        TestObject result = parser.deserialize(bais, null, TestObject.class);
        assertNotNull(result);
        assertEquals("streamNullConfig", result.getValue());
    }

    @Test
    public void testDeserializeFromReader() {
        StringReader reader = new StringReader("readerValue");
        TestObject result = parser.deserialize(reader, TestObject.class);
        assertNotNull(result);
        assertEquals("readerValue", result.getValue());
    }

    @Test
    public void testDeserializeFromEmptyReader() {
        StringReader reader = new StringReader("");
        TestObject result = parser.deserialize(reader, TestObject.class);
        assertNotNull(result);
        assertEquals("", result.getValue());
    }

    @Test
    public void testDeserializeFromReaderToString() {
        StringReader reader = new StringReader("readerString");
        String result = parser.deserialize(reader, String.class);
        assertEquals("readerString", result);
    }

    @Test
    public void testDeserializeFromReaderWithLargeContent() {
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("x");
        }
        StringReader reader = new StringReader(largeContent.toString());
        TestObject result = parser.deserialize(reader, TestObject.class);
        assertNotNull(result);
        assertEquals(1000, result.getValue().length());
    }

    @Test
    public void testDeserializeFromReaderWithConfig() {
        StringReader reader = new StringReader("readerConfigValue");
        deserConfig.setIgnoreUnknownProperty(true);
        TestObject result = parser.deserialize(reader, deserConfig, TestObject.class);
        assertNotNull(result);
        assertEquals("readerConfigValue", result.getValue());
    }

    @Test
    public void testDeserializeFromReaderWithConfigNull() {
        StringReader reader = new StringReader("readerNullConfig");
        TestObject result = parser.deserialize(reader, null, TestObject.class);
        assertNotNull(result);
        assertEquals("readerNullConfig", result.getValue());
    }

    @Test
    public void testRoundTripSerializationDeserialization() {
        String serialized = parser.serialize(testObject);
        TestObject deserialized = parser.deserialize(serialized, TestObject.class);
        assertNotNull(deserialized);
        assertEquals(testObject.getValue(), deserialized.getValue());
    }

    @Test
    public void testRoundTripWithConfig() {
        serConfig.setPrettyFormat(true);
        String serialized = parser.serialize(testObject, serConfig);
        assertTrue(serialized.startsWith("PRETTY:"));

        deserConfig.setIgnoreUnknownProperty(true);
        TestObject deserialized = parser.deserialize(serialized, deserConfig, TestObject.class);
        assertNotNull(deserialized);
    }

    @Test
    public void testRoundTripViaFile() throws IOException {
        File tempFile = File.createTempFile("parser124test", ".txt");
        tempFile.deleteOnExit();

        parser.serialize(testObject, tempFile);
        TestObject deserialized = parser.deserialize(tempFile, TestObject.class);

        assertNotNull(deserialized);
        assertEquals(testObject.getValue(), deserialized.getValue());
    }

    @Test
    public void testRoundTripViaStream() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(testObject, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        TestObject deserialized = parser.deserialize(bais, TestObject.class);

        assertNotNull(deserialized);
        assertEquals(testObject.getValue(), deserialized.getValue());
    }

    @Test
    public void testRoundTripViaWriterReader() {
        StringWriter writer = new StringWriter();
        parser.serialize(testObject, writer);

        StringReader reader = new StringReader(writer.toString());
        TestObject deserialized = parser.deserialize(reader, TestObject.class);

        assertNotNull(deserialized);
        assertEquals(testObject.getValue(), deserialized.getValue());
    }
}
