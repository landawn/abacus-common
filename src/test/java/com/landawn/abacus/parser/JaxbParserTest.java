package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.SAXParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.XmlUtil;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

public class JaxbParserTest extends TestBase {

    private JaxbParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        parser = new JaxbParser();
    }

    @XmlRootElement
    public static class Person {
        private String name;
        private int age;

        public Person() {
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @XmlElement
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlElement
        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }
    }

    @XmlRootElement
    public static class Book {
        private String title;
        private String isbn;

        @XmlAttribute
        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        @XmlElement
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    @Test
    public void test_constructor_default() {
        XmlParser parser = new JaxbParser();
        assertNotNull(parser);
    }

    @Test
    public void test_constructor_withConfig() {
        XmlSerConfig xsc = new XmlSerConfig();
        XmlDeserConfig xdc = new XmlDeserConfig();
        XmlParser parser = new JaxbParser(xsc, xdc);
        assertNotNull(parser);
    }

    @Test
    public void testConstructorWithConfigs() {
        XmlSerConfig xsc = new XmlSerConfig();
        XmlDeserConfig xdc = new XmlDeserConfig();

        JaxbParser parserWithConfig = new JaxbParser(xsc, xdc);
        assertNotNull(parserWithConfig);

        Person person = new Person("Test", 30);
        String xml = parserWithConfig.serialize(person, (XmlSerConfig) null);
        Person deserialized = parserWithConfig.deserialize(xml, null, Person.class);
        assertEquals(person, deserialized);
    }

    @Test
    public void testConstructorSerializationConfigIsAppliedWhenCallConfigIsNull() {
        XmlSerConfig xsc = new XmlSerConfig().setIgnoredPropNames(Map.of(Person.class, Set.of("age")));
        JaxbParser parserWithConfig = new JaxbParser(xsc, null);

        assertThrows(ParsingException.class, () -> parserWithConfig.serialize(new Person("Test", 30), (XmlSerConfig) null));
    }

    @Test
    public void testConstructorDeserializationConfigIsAppliedWhenCallConfigIsNull() {
        XmlDeserConfig xdc = new XmlDeserConfig().setIgnoredPropNames(Map.of(Person.class, Set.of("age")));
        JaxbParser parserWithConfig = new JaxbParser(null, xdc);

        assertThrows(ParsingException.class, () -> parserWithConfig.deserialize("<person><name>Test</name><age>30</age></person>", null, Person.class));
        assertThrows(ParsingException.class, () -> parserWithConfig.deserialize("", null, Person.class));

        XmlDeserConfig perCallConfig = new XmlDeserConfig().setIgnoredPropNames(Map.of(Person.class, Set.of("age")));
        assertThrows(ParsingException.class, () -> parser.deserialize("", perCallConfig, Person.class));
    }

    @Test
    public void testSerializeToString() {
        Person person = new Person("John", 30);
        String xml = parser.serialize(person, (XmlSerConfig) null);

        assertNotNull(xml);
        assertTrue(xml.contains("<name>John</name>"));
        assertTrue(xml.contains("<age>30</age>"));
    }

    @Test
    public void testSerializeNull() {
        String result = parser.serialize(null, (XmlSerConfig) null);
        assertEquals("", result);
    }

    @Test
    public void testRoundTrip() {
        Person original = new Person("Henry", 33);
        String xml = parser.serialize(original, (XmlSerConfig) null);
        Person deserialized = parser.deserialize(xml, null, Person.class);

        assertEquals(original, deserialized);
    }

    @Test
    public void testSerializeWithAttributes() {
        Book book = new Book();
        book.setTitle("Java Programming");
        book.setIsbn("123-456-789");

        String xml = parser.serialize(book, (XmlSerConfig) null);

        assertTrue(xml.contains("isbn=\"123-456-789\""));
        assertTrue(xml.contains("<title>Java Programming</title>"));
    }

    @Test
    public void testSerializeToFile() throws IOException {
        Person person = new Person("Alice", 25);
        File file = tempDir.resolve("person.xml").toFile();

        parser.serialize(person, null, file);

        assertTrue(file.exists());
        String content = IOUtil.readAllToString(file);
        assertTrue(content.contains("<name>Alice</name>"));
        assertTrue(content.contains("<age>25</age>"));
    }

    @Test
    public void testSerializeToOutputStream() throws IOException {
        Person person = new Person("Bob", 35);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        parser.serialize(person, null, baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<name>Bob</name>"));
        assertTrue(xml.contains("<age>35</age>"));
    }

    @Test
    public void testSerializeToOutputStreamUsesTheDeclaredEncoding() {
        Person person = new Person("Zoë雪", 35);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        parser.serialize(person, null, baos);

        String xml = baos.toString(StandardCharsets.UTF_8);
        assertTrue(xml.contains("encoding=\"UTF-8\""));
        assertTrue(xml.contains("<name>Zoë雪</name>"));
        assertEquals(person, parser.deserialize(new ByteArrayInputStream(baos.toByteArray()), null, Person.class));
    }

    @Test
    public void testSerializeToWriter() throws IOException {
        Person person = new Person("Charlie", 40);
        StringWriter writer = new StringWriter();

        parser.serialize(person, null, writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<name>Charlie</name>"));
        assertTrue(xml.contains("<age>40</age>"));
    }

    @Test
    public void testSerializeWithIgnoredPropNames() {
        Person person = new Person("Test", 100);
        XmlSerConfig config = new XmlSerConfig();
        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        ignoredProps.put(Person.class, new HashSet<>(Arrays.asList("age")));
        config.setIgnoredPropNames(ignoredProps);

        assertThrows(ParsingException.class, () -> parser.serialize(person, config));
    }

    @Test
    public void testSerializeNullStillValidatesUnsupportedConfig() {
        XmlSerConfig config = new XmlSerConfig();
        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        ignoredProps.put(Person.class, Set.of("age"));
        config.setIgnoredPropNames(ignoredProps);

        assertThrows(ParsingException.class, () -> parser.serialize(null, config));
    }

    @Test
    public void testInvalidConfigDoesNotTruncateExistingFile() throws IOException {
        File file = tempDir.resolve("existing.xml").toFile();
        Files.writeString(file.toPath(), "preserve-me", StandardCharsets.UTF_8);
        XmlSerConfig config = new XmlSerConfig();
        config.setIgnoredPropNames(Map.of(Person.class, Set.of("age")));

        assertThrows(ParsingException.class, () -> parser.serialize(new Person("Test", 1), config, file));
        assertEquals("preserve-me", Files.readString(file.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    public void testSerializeNullToFile() throws IOException {
        File file = tempDir.resolve("null.xml").toFile();
        parser.serialize(null, null, file);

        assertTrue(file.exists());
        assertEquals("", IOUtil.readAllToString(file));
    }

    @Test
    public void testSerializeNullToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(null, null, baos);

        assertEquals("", baos.toString());
    }

    @Test
    public void testSerializeNullToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        parser.serialize(null, null, writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testDeserializeFromString() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<person><name>David</name><age>45</age></person>";

        Person person = parser.deserialize(xml, null, Person.class);

        assertEquals("David", person.getName());
        assertEquals(45, person.getAge());
    }

    @Test
    public void testDeserializeEmptyString() {
        Person person = parser.deserialize("", null, Person.class);
        assertNull(person);
    }

    @Test
    public void testDeserializeFromInputStreamUtf16() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-16\" standalone=\"yes\"?>" + "<person><name>\u4f60\u597d</name><age>50</age></person>";
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_16));

        Person person = parser.deserialize(bais, null, Person.class);

        assertEquals("\u4f60\u597d", person.getName());
        assertEquals(50, person.getAge());
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<person><name>Eve</name><age>22</age></person>";
        File file = tempDir.resolve("test.xml").toFile();
        IOUtil.write(xml, file);

        Person person = parser.deserialize(file, null, Person.class);

        assertEquals("Eve", person.getName());
        assertEquals(22, person.getAge());
    }

    @Test
    public void testDeserializeFromInputStream() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<person><name>Frank</name><age>50</age></person>";
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());

        Person person = parser.deserialize(bais, null, Person.class);

        assertEquals("Frank", person.getName());
        assertEquals(50, person.getAge());
    }

    @Test
    public void testDeserializeFromReader() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<person><name>Grace</name><age>28</age></person>";
        StringReader reader = new StringReader(xml);

        Person person = parser.deserialize(reader, null, Person.class);

        assertEquals("Grace", person.getName());
        assertEquals(28, person.getAge());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeserializeRecyclesHardenedSaxParser() throws Exception {
        Field field = XmlUtil.class.getDeclaredField("saxParserPool");
        field.setAccessible(true);
        Queue<SAXParser> pool = (Queue<SAXParser>) field.get(null);
        List<SAXParser> saved;

        synchronized (pool) {
            saved = new ArrayList<>(pool);
            pool.clear();
        }

        try {
            String xml = "<person><name>Pool</name><age>2</age></person>";
            assertEquals("Pool", parser.deserialize(xml, null, Person.class).getName());

            synchronized (pool) {
                assertEquals(1, pool.size());
            }

            assertEquals("Pool", parser.deserialize(new StringReader(xml), null, Person.class).getName());

            synchronized (pool) {
                assertEquals(1, pool.size());
            }
        } finally {
            synchronized (pool) {
                pool.clear();
                pool.addAll(saved);
            }
        }
    }

    @Test
    public void testDeserializeFromFileUtf16() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-16\" standalone=\"yes\"?>" + "<person><name>\u4e16\u754c</name><age>22</age></person>";
        File file = tempDir.resolve("test-utf16.xml").toFile();
        Files.writeString(file.toPath(), xml, StandardCharsets.UTF_16);

        Person person = parser.deserialize(file, null, Person.class);

        assertEquals("\u4e16\u754c", person.getName());
        assertEquals(22, person.getAge());
    }

    @Test
    public void testDeserializeWithIgnoredPropNames() {
        String xml = "<person><name>Test</name><age>100</age></person>";
        XmlDeserConfig config = new XmlDeserConfig();
        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        ignoredProps.put(Person.class, new HashSet<>(Arrays.asList("age")));
        config.setIgnoredPropNames(ignoredProps);

        assertThrows(ParsingException.class, () -> parser.deserialize(xml, config, Person.class));
    }

    @Test
    public void testDeserializeFromNode() {
        assertThrows(UnsupportedOperationException.class, () -> parser.deserialize((Node) null, null, Person.class));
    }

    @Test
    public void testDeserializeFromInputStreamWithNodeClasses() {
        ByteArrayInputStream bais = new ByteArrayInputStream("test".getBytes());
        Map<String, Type<?>> nodeClasses = new HashMap<>();

        assertThrows(UnsupportedOperationException.class, () -> parser.deserialize(bais, null, nodeClasses));
    }

    @Test
    public void testDeserializeFromReaderWithNodeClasses() {
        StringReader reader = new StringReader("test");
        Map<String, Type<?>> nodeClasses = new HashMap<>();

        assertThrows(UnsupportedOperationException.class, () -> parser.deserialize(reader, null, nodeClasses));
    }

    @Test
    public void testDeserializeFromNodeWithNodeClasses() {
        Map<String, Type<?>> nodeClasses = new HashMap<>();

        assertThrows(UnsupportedOperationException.class, () -> parser.deserialize((Node) null, null, nodeClasses));
    }

    // ---------------------------------------------------------------------------------------------
    // reviewFixes20260906: P6-04 (caller's stream/reader not closed), P6-08 (I/O failure while
    // marshalling -> UncheckedIOException).
    // ---------------------------------------------------------------------------------------------

    private static final String PERSON_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><name>Ines</name><age>31</age></person>";

    private static final String MALFORMED_XML = "<person><name>Ines</name><age>31</person>";

    private static final String DOCTYPE_XML = "<!DOCTYPE person [<!ENTITY x \"boom\">]><person><name>&x;</name><age>1</age></person>";

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {
        int closeCalls;

        CloseTrackingInputStream(final String xml) {
            super(xml.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() throws IOException {
            closeCalls++;
            super.close();
        }
    }

    private static final class CloseTrackingReader extends StringReader {
        int closeCalls;

        CloseTrackingReader(final String xml) {
            super(xml);
        }

        @Override
        public void close() {
            closeCalls++;
            super.close();
        }
    }

    @Test
    public void reviewFixes20260906_deserializeFromInputStreamDoesNotCloseCallerStream() {
        CloseTrackingInputStream in = new CloseTrackingInputStream(PERSON_XML);
        Person person = parser.deserialize(in, null, Person.class);
        assertEquals("Ines", person.getName());
        assertEquals(31, person.getAge());
        assertEquals(0, in.closeCalls);
        // Consumed to the end of the document, but still usable by the caller.
        assertEquals(-1, in.read());
        in.reset();
        assertEquals("Ines", parser.deserialize(in, null, Person.class).getName());
        assertEquals(0, in.closeCalls);

        in = new CloseTrackingInputStream(PERSON_XML);
        assertEquals(31, parser.deserialize(in, null, Type.of(Person.class)).getAge());
        assertEquals(0, in.closeCalls);

        final CloseTrackingInputStream malformed = new CloseTrackingInputStream(MALFORMED_XML);
        assertThrows(ParsingException.class, () -> parser.deserialize(malformed, null, Person.class));
        assertEquals(0, malformed.closeCalls);

        final CloseTrackingInputStream doctype = new CloseTrackingInputStream(DOCTYPE_XML);
        assertThrows(ParsingException.class, () -> parser.deserialize(doctype, null, Person.class));
        assertEquals(0, doctype.closeCalls);

        // Unicode through the wrapper, UTF-8 and UTF-16 alike.
        final String unicode = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><person><name>é中😀</name><age>2</age></person>";
        assertEquals("é中😀", parser.deserialize(new CloseTrackingInputStream(unicode), null, Person.class).getName());
        final String utf16 = "<?xml version=\"1.0\" encoding=\"UTF-16\"?><person><name>你好</name><age>3</age></person>";
        assertEquals("你好", parser.deserialize(new ByteArrayInputStream(utf16.getBytes(StandardCharsets.UTF_16)), null, Person.class).getName());
    }

    @Test
    public void reviewFixes20260906_deserializeFromReaderDoesNotCloseCallerReader() throws IOException {
        CloseTrackingReader reader = new CloseTrackingReader(PERSON_XML);
        Person person = parser.deserialize(reader, null, Person.class);
        assertEquals("Ines", person.getName());
        assertEquals(0, reader.closeCalls);
        assertEquals(-1, reader.read());
        reader.reset();
        assertEquals(31, parser.deserialize(reader, null, Person.class).getAge());
        assertEquals(0, reader.closeCalls);

        reader = new CloseTrackingReader(PERSON_XML);
        assertEquals("Ines", parser.deserialize(reader, null, Type.of(Person.class)).getName());
        assertEquals(0, reader.closeCalls);

        final CloseTrackingReader malformed = new CloseTrackingReader(MALFORMED_XML);
        assertThrows(ParsingException.class, () -> parser.deserialize(malformed, null, Person.class));
        assertEquals(0, malformed.closeCalls);

        final CloseTrackingReader doctype = new CloseTrackingReader(DOCTYPE_XML);
        assertThrows(ParsingException.class, () -> parser.deserialize(doctype, null, Person.class));
        assertEquals(0, doctype.closeCalls);

        final CloseTrackingReader unicode = new CloseTrackingReader("<person><name>é中😀</name><age>2</age></person>");
        assertEquals("é中😀", parser.deserialize(unicode, null, Person.class).getName());
        assertEquals(0, unicode.closeCalls);
    }

    @Test
    public void reviewFixes20260906_deserializeFromFileStillClosesItsOwnStream() throws IOException {
        final File file = tempDir.resolve("own-stream.xml").toFile();
        IOUtil.write(PERSON_XML, file);

        assertEquals("Ines", parser.deserialize(file, null, Person.class).getName());

        // On Windows an open handle would make the delete fail.
        Files.delete(file.toPath());
        assertFalse(file.exists());
    }

    private static final class FailingOutputStream extends OutputStream {
        private final IOException failure;
        private final boolean failOnWrite;

        FailingOutputStream(final IOException failure, final boolean failOnWrite) {
            this.failure = failure;
            this.failOnWrite = failOnWrite;
        }

        @Override
        public void write(final int b) throws IOException {
            if (failOnWrite) {
                throw failure;
            }
        }

        @Override
        public void flush() throws IOException {
            throw failure;
        }
    }

    public static class NotAnXmlRoot {
        private String value = "v";

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    public void reviewFixes20260906_serializeToFailingOutputStreamThrowsUncheckedIOException() throws IOException {
        final IOException disk = new IOException("disk full");
        final Person person = new Person("Io", 5);

        UncheckedIOException thrown = assertThrows(UncheckedIOException.class, () -> parser.serialize(person, null, new FailingOutputStream(disk, true)));
        assertSame(disk, thrown.getCause());

        thrown = assertThrows(UncheckedIOException.class, () -> parser.serialize(person, null, new FailingOutputStream(disk, false)));
        assertSame(disk, thrown.getCause());

        // A null object writes nothing but still flushes the stream.
        thrown = assertThrows(UncheckedIOException.class, () -> parser.serialize(null, null, new FailingOutputStream(disk, true)));
        assertSame(disk, thrown.getCause());

        // Writer overload: a failure inside JAXB's own write loop (large payload) is an I/O failure too.
        final Writer failingWriter = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                throw disk;
            }

            @Override
            public void flush() throws IOException {
                throw disk;
            }

            @Override
            public void close() {
            }
        };
        final Person big = new Person("x".repeat(100_000), 1);
        thrown = assertThrows(UncheckedIOException.class, () -> parser.serialize(big, null, failingWriter));
        assertSame(disk, thrown.getCause());

        // A marshalling failure without an I/O cause is still a ParsingException.
        final ParsingException parsing = assertThrows(ParsingException.class, () -> parser.serialize(new NotAnXmlRoot(), null, new ByteArrayOutputStream()));
        assertFalse(UncheckedIOException.class.isInstance(parsing));
        assertThrows(ParsingException.class, () -> parser.serialize(new NotAnXmlRoot(), null, new StringWriter()));

        // And the parser still works afterwards.
        final ByteArrayOutputStream ok = new ByteArrayOutputStream();
        parser.serialize(person, null, ok);
        assertTrue(ok.toString(StandardCharsets.UTF_8).contains("<name>Io</name>"));
    }

    @Test
    public void reviewFixes20260907_jaxbParserDoesNotDependOnTheOptionalAvroClasses() throws IOException {
        // Avro and the JAXB runtime are independent `provided`-scope dependencies, so a deployment may
        // carry one without the other. Borrowing AvroParser's non-closing wrapper (the first shape of the
        // P6-04 fix) made deserialize(InputStream, ..) fail on a JAXB-but-no-Avro classpath with
        // NoClassDefFoundError: org/apache/avro/io/DatumReader. The class file is the only place this can
        // be pinned without rebuilding the classpath, so assert JaxbParser's constant pool names no Avro type.
        final byte[] classFile;

        try (java.io.InputStream in = JaxbParser.class.getResourceAsStream("JaxbParser.class")) {
            assertNotNull(in, "JaxbParser.class must be readable as a resource");
            classFile = in.readAllBytes();
        }

        final String constantPool = new String(classFile, StandardCharsets.ISO_8859_1);
        assertFalse(constantPool.contains("org/apache/avro"), "JaxbParser must not reference Apache Avro");
        assertFalse(constantPool.contains("parser/AvroParser"), "JaxbParser must not reference AvroParser");

        // ... while the stream overload keeps the behaviour that motivated the shared wrapper.
        final CloseTrackingInputStream in = new CloseTrackingInputStream(PERSON_XML);
        assertEquals("Ines", parser.deserialize(in, null, Person.class).getName());
        assertEquals(0, in.closeCalls);
    }

    @Test
    public void reviewFixes20260908_deserializeFromFailingSourceThrowsUncheckedIOException() {
        // JAXB wraps a failing source's IOException in an UnmarshalException. Parser documents
        // UncheckedIOException for the stream/reader deserialize overloads, so the unmarshal path must
        // unwrap it exactly as the marshal path already does - not report it as a ParsingException.
        final IOException boom = new IOException("boom-read");

        final InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw boom;
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                throw boom;
            }
        };

        UncheckedIOException thrown = assertThrows(UncheckedIOException.class, () -> parser.deserialize(failingStream, null, Person.class));
        assertSame(boom, thrown.getCause());

        final Reader failingReader = new Reader() {
            @Override
            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                throw boom;
            }

            @Override
            public void close() {
            }
        };

        thrown = assertThrows(UncheckedIOException.class, () -> parser.deserialize(failingReader, null, Person.class));
        assertSame(boom, thrown.getCause());

        // A malformed document has no I/O cause, so it stays a ParsingException.
        final ParsingException parsing = assertThrows(ParsingException.class,
                () -> parser.deserialize(new ByteArrayInputStream("<person><name>".getBytes(StandardCharsets.UTF_8)), null, Person.class));
        assertFalse(UncheckedIOException.class.isInstance(parsing));
        assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader("<person><name>"), null, Person.class));

        // And the parser still works afterwards.
        assertEquals("Ines", parser.deserialize(new StringReader(PERSON_XML), null, Person.class).getName());
    }


    // R03: mapping every IOException in the JAXB cause chain to UncheckedIOException also caught the one that
    // is not an I/O failure at all - Xerces reports undecodable bytes with MalformedByteSequenceException, a
    // CharConversionException. Nothing failed to arrive; the document cannot be decoded, which is what r9503
    // and every other XML backend in this package report as a ParsingException.
    @Test
    public void reviewFixes20260908_undecodableBytesAreAParsingFailureNotAnIoFailure() {
        // <person><name>?</name></person> with an invalid 2-byte UTF-8 sequence inside the text
        final byte[] undecodable = { 60, 112, 101, 114, 115, 111, 110, 62, 60, 110, 97, 109, 101, 62, (byte) 0xC3, (byte) 0x28, 60, 47, 110, 97, 109, 101, 62,
                60, 47, 112, 101, 114, 115, 111, 110, 62 };

        final ParsingException fromStream = assertThrows(ParsingException.class,
                () -> parser.deserialize(new ByteArrayInputStream(undecodable), null, Person.class));
        assertFalse(UncheckedIOException.class.isInstance(fromStream));

        // a source that genuinely fails is still an UncheckedIOException, so the carve-out is not a blanket one
        final IOException boom = new IOException("boom-read");
        final InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw boom;
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                throw boom;
            }
        };

        assertSame(boom, assertThrows(UncheckedIOException.class, () -> parser.deserialize(failingStream, null, Person.class)).getCause());

        // and the parser still works afterwards
        assertEquals("Ines", parser.deserialize(new StringReader(PERSON_XML), null, Person.class).getName());
    }
}
