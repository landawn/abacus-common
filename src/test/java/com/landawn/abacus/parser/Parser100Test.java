package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

@Tag("new-test")
public class Parser100Test extends TestBase {

    private TestParser parser;
    private TestObject testObject;

    private static class TestSerializationConfig extends SerializationConfig<TestSerializationConfig> {
    }

    private static class TestDeserializationConfig extends DeserializationConfig<TestDeserializationConfig> {
    }

    private static class TestParser implements Parser<TestSerializationConfig, TestDeserializationConfig> {
        @Override
        public String serialize(Object obj) {
            return obj != null ? obj.toString() : "";
        }

        @Override
        public String serialize(Object obj, TestSerializationConfig config) {
            return serialize(obj);
        }

        @Override
        public void serialize(Object obj, TestSerializationConfig config, File output) {
            try (FileWriter writer = new FileWriter(output)) {
                writer.write(serialize(obj, config));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void serialize(Object obj, File output) {
            serialize(obj, null, output);
        }

        @Override
        public void serialize(Object obj, TestSerializationConfig config, OutputStream output) {
            try {
                output.write(serialize(obj, config).getBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void serialize(Object obj, OutputStream output) {
            serialize(obj, null, output);
        }

        @Override
        public void serialize(Object obj, TestSerializationConfig config, Writer output) {
            try {
                output.write(serialize(obj, config));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void serialize(Object obj, Writer output) {
            serialize(obj, null, output);
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
        public <T> T deserialize(String source, TestDeserializationConfig config, Class<? extends T> targetClass) {
            return deserialize(source, targetClass);
        }

        @Override
        public <T> T deserialize(File source, Class<? extends T> targetClass) {
            return deserialize("", targetClass);
        }

        @Override
        public <T> T deserialize(File source, TestDeserializationConfig config, Class<? extends T> targetClass) {
            return deserialize(source, targetClass);
        }

        @Override
        public <T> T deserialize(InputStream source, Class<? extends T> targetClass) {
            return deserialize("", targetClass);
        }

        @Override
        public <T> T deserialize(InputStream source, TestDeserializationConfig config, Class<? extends T> targetClass) {
            return deserialize(source, targetClass);
        }

        @Override
        public <T> T deserialize(Reader source, Class<? extends T> targetClass) {
            return deserialize("", targetClass);
        }

        @Override
        public <T> T deserialize(Reader source, TestDeserializationConfig config, Class<? extends T> targetClass) {
            return deserialize(source, targetClass);
        }
    }

    private static class TestObject {
        private String value = "test";

        @Override
        public String toString() {
            return value;
        }
    }

    @BeforeEach
    public void setUp() {
        parser = new TestParser();
        testObject = new TestObject();
    }

    @Test
    public void testSerializeToString() {
        String result = parser.serialize(testObject);
        assertEquals("test", result);

        String nullResult = parser.serialize(null);
        assertEquals("", nullResult);
    }

    @Test
    public void testSerializeToStringWithConfig() {
        TestSerializationConfig config = new TestSerializationConfig();
        String result = parser.serialize(testObject, config);
        assertEquals("test", result);
    }

    @Test
    public void testSerializeToFile() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        parser.serialize(testObject, tempFile);

        String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
        assertEquals("test", content);
    }

    @Test
    public void testSerializeToFileWithConfig() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        TestSerializationConfig config = new TestSerializationConfig();
        parser.serialize(testObject, config, tempFile);

        String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
        assertEquals("test", content);
    }

    @Test
    public void testSerializeToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(testObject, baos);

        assertEquals("test", baos.toString());
    }

    @Test
    public void testSerializeToOutputStreamWithConfig() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TestSerializationConfig config = new TestSerializationConfig();
        parser.serialize(testObject, config, baos);

        assertEquals("test", baos.toString());
    }

    @Test
    public void testSerializeToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        parser.serialize(testObject, writer);

        assertEquals("test", writer.toString());
    }

    @Test
    public void testSerializeToWriterWithConfig() throws IOException {
        StringWriter writer = new StringWriter();
        TestSerializationConfig config = new TestSerializationConfig();
        parser.serialize(testObject, config, writer);

        assertEquals("test", writer.toString());
    }

    @Test
    public void testDeserializeFromString() {
        TestObject result = parser.deserialize("test", TestObject.class);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeFromStringWithConfig() {
        TestDeserializationConfig config = new TestDeserializationConfig();
        TestObject result = parser.deserialize("test", config, TestObject.class);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        TestObject result = parser.deserialize(tempFile, TestObject.class);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeFromFileWithConfig() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        TestDeserializationConfig config = new TestDeserializationConfig();
        TestObject result = parser.deserialize(tempFile, config, TestObject.class);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeFromInputStream() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream("test".getBytes());
        TestObject result = parser.deserialize(bais, TestObject.class);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeFromInputStreamWithConfig() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream("test".getBytes());
        TestDeserializationConfig config = new TestDeserializationConfig();
        TestObject result = parser.deserialize(bais, config, TestObject.class);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeFromReader() throws IOException {
        StringReader reader = new StringReader("test");
        TestObject result = parser.deserialize(reader, TestObject.class);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeFromReaderWithConfig() throws IOException {
        StringReader reader = new StringReader("test");
        TestDeserializationConfig config = new TestDeserializationConfig();
        TestObject result = parser.deserialize(reader, config, TestObject.class);
        assertNotNull(result);
    }
}
